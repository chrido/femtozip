/**
 *   Copyright 2011 Garrick Toubassi
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */
package org.toubassi.femtozip;

import java.nio.Buffer;
import java.nio.ByteBuffer;


import org.toubassi.femtozip.coding.huffman.ByteBufferOutputStream;
import org.toubassi.femtozip.dictionary.DictionaryOptimizer;
import org.toubassi.femtozip.models.*;
import org.toubassi.femtozip.substring.SubstringPacker;

import java.io.*;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * The primary class used by external consumers of the Java FemtoZip API.
 * It provides compression/decompression as well as model building functionality.
 * The basic recipe for using FemtoZip is to:
 *
 * 1. Collect sample "documents" (document is simply a byte[]) which
 *    can be used to build a model.
 * 2. Call the static CompressionModel.buildOptimalModel with a DocumentList
 *    which can be used to iterate the documents.  There are several built in
 *    DocumentLists if the data can be stored in memory, or you can implement
 *    your own.  A newly created CompressionModel will be returned.
 * 3. Call the CompressionModel.save(String) to save the model to a file.
 * 4. Later (perhaps in a different process), load the model via the static
 *    CompressionModel.loadModel(String);
 * 5. Use CompressionModel.compress/decompress as needed.
 * 
 * For a simple pure Java example, see the org.toubassi.femtozip.ExampleTest JUnit test
 * case in the source distribution of FemtoZip at http://github.com/gtoubassi/femtozip
 * 
 * To use the JNI interface to FemtoZip, you will follow largely the same recipe, but you
 * will use the NativeCompressionModel.
 * 
 * @see org.toubassi.femtozip.models.NativeCompressionModel
 */
public abstract class CompressionModel implements SubstringPacker.Consumer {
    
    protected ByteBuffer dictionary;
    protected SubstringPacker packer;
    private int maxDictionaryLength;

    public CompressionModel() {
        dictionary = ByteBuffer.allocate(0);
    }


    public static CompressionModel instantiateCompressionModel(String modelName) {
        if (modelName.indexOf('.') == -1) {
            modelName = FemtoZipCompressionModel.class.getPackage().getName() + "." + modelName;
            if (!modelName.endsWith("CompressionModel")) {
                modelName += "CompressionModel";
            }
        }

        CompressionModel model = null;

        try {
            Class<?> cls = Class.forName(modelName);
            model = (CompressionModel)cls.newInstance();
        }
        catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        } catch (InstantiationException e) {
            throw new RuntimeException(e);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
        return model;
    }
    
    public static class ModelOptimizationResult implements Comparable<ModelOptimizationResult>{
        public CompressionModel model;
        public int totalCompressedSize;
        public int totalDataSize;

        public ModelOptimizationResult(CompressionModel model) {
            this.model = model;
        }

        public int compareTo(ModelOptimizationResult other) {
            return totalCompressedSize - other.totalCompressedSize;
        }
        
        public void accumulate(ModelOptimizationResult result) {
            totalCompressedSize += result.totalCompressedSize < result.totalDataSize ? result.totalCompressedSize:  result.totalDataSize;
            totalDataSize += result.totalDataSize;
        }
        
        public String toString() {
            DecimalFormat format = new DecimalFormat("#.##");
            String prefix = "";
            if (model != null) {
                prefix = model.getClass().getSimpleName() + " ";
            }
            return prefix + format.format((100f * totalCompressedSize) / totalDataSize) + "% (" + totalCompressedSize + " from " + totalDataSize + " bytes)";
        }
    }
    
    /**
     * Builds a new model trained on the specified documents.  This is where it all begins.
     * @return The newly created CompressionModel
     * @throws IOException
     */
    public static CompressionModel buildOptimalModel(DocumentList documents) throws IOException {
        return buildOptimalModel(documents, null, null, false);
    }
    
    public static CompressionModel buildOptimalModel(DocumentList documents, List<ModelOptimizationResult> results, CompressionModel[] competingModels, boolean verify) throws IOException {

        if (competingModels == null || competingModels.length == 0) {
            competingModels = new CompressionModel[5];
            competingModels[0] = new FemtoZipCompressionModel();
            competingModels[1] = new PureHuffmanCompressionModel();
            competingModels[2] = new GZipCompressionModel();
            competingModels[3] = new GZipDictionaryCompressionModel();
            competingModels[4] = new VariableIntCompressionModel();
        }
        
        if (results == null) {
            results = new ArrayList<ModelOptimizationResult>();
        }

        for (CompressionModel model : competingModels) {
            if(model != null)
                results.add(new ModelOptimizationResult(model));
        }
        
        // Split the documents into two groups.  One for building each model out
        // and one for testing which model is best.  Shouldn't build and test
        // with the same set as a model may over optimize for the training set.
        SamplingDocumentList trainingDocuments = new SamplingDocumentList(documents, 2, 0);
        SamplingDocumentList testingDocuments = new SamplingDocumentList(documents, 2, 1);
        
        // Build the dictionary once to avoid rebuilding for each model.
        ByteBuffer dictionary = buildDictionary(trainingDocuments, 64*1024);

        // Build each model out
        for (ModelOptimizationResult result : results) {
            result.model.setDictionary(dictionary);
            result.model.build(trainingDocuments);
        }

        // Pick the best model

        for (int i = 0, count = testingDocuments.size(); i < count; i++) {
            ByteBuffer data = testingDocuments.getBB(i);
            
            for (ModelOptimizationResult result : results) {
                ByteBuffer backingBB = ByteBuffer.allocate(1000);
                try(ByteBufferOutputStream bbos = new ByteBufferOutputStream(backingBB, true)) {
                    result.model.compress(data, bbos);
                }
                
                if (verify) {
                    ByteBuffer decompressed = result.model.decompress(backingBB);
                    if (!decompressed.equals(data)) {
                        throw new RuntimeException("Compress/Decompress round trip failed for " + result.model.getClass().getSimpleName());
                    }
                }
                
                result.totalCompressedSize += backingBB.remaining();
                result.totalDataSize += data.remaining();
            }
        }
        
        Collections.sort(results);
        
        ModelOptimizationResult bestResult = results.get(0);
        return bestResult.model;
    }
    
    public void setDictionary(ByteBuffer newDictionary) {
        if (maxDictionaryLength > 0 && newDictionary.remaining()> maxDictionaryLength) {
            // We chop off the front as important strings are packed towards the end for shorter lengths/offsets
            this.dictionary = ByteBuffer.allocate(maxDictionaryLength);
            newDictionary.position(newDictionary.remaining() - maxDictionaryLength);

            for(int i = 0; i < maxDictionaryLength; i++) {
                this.dictionary.put(newDictionary.get());
            }
            //newDictionary.getBytes(newDictionary.remaining() - maxDictionaryLength, this.dictionary, maxDictionaryLength);
        }
        this.dictionary = newDictionary;
        packer = null;
    }
    
    public ByteBuffer getDictionary() {
        return dictionary;
    }

    public int getMaxDictionaryLength() {
        return maxDictionaryLength;
    }
    
    public void setMaxDictionaryLength(int length) {
        maxDictionaryLength = length;
    }
    
    protected SubstringPacker getSubstringPacker() {
        if (packer == null) {
            packer = new SubstringPacker(dictionary);
        }
        return packer;
    }
    
    public void load(DataInputStream in) throws IOException {
        in.readInt(); // file format version, currently unused.
        
        int dictionaryLength = in.readInt();
        
        if (dictionaryLength == -1) {
            setDictionary(null);
        }
        else {
            ByteBuffer dictionary = ByteBuffer.allocate(dictionaryLength);
            //ByteBuffer dictionary = arena.buffer(dictionaryLength);
            //int totalRead = dictionary.writeBytes(in, dictionaryLength);
            int i; //TODO: error handling
            for(i = 0; i < dictionaryLength; i++){
                dictionary.put(in.readByte());
            }
            if (i != dictionaryLength) {
                throw new IOException("Bad model in stream.  Could not read dictionary of length " + dictionaryLength);
            }

            setDictionary(dictionary);
        }
    }

    public void save(DataOutputStream out) throws IOException {
        out.writeInt(0); // Poor mans file format version
        if (dictionary == null) {
            out.writeInt(-1);
        }
        else {
            out.writeInt(dictionary.remaining());
            while(dictionary.hasRemaining()) {
                out.write(dictionary.get());
            }
            dictionary.position(0);
            //dictionary.readBytes(out, dictionary.remaining());
            //dictionary.resetReaderIndex();
        }
    }
    
    /**
     * Loads a model previously saved with save.  You must use this
     * static because it dynamically instantiates the correct
     * model based on the type that was saved.
     * @param path
     * @throws IOException
     * 
     * @see org.toubassi.femtozip.CompressionModel.save(String path) throws IOException
     */
    public static CompressionModel loadModel(String path) throws IOException {
        FileInputStream fileIn = new FileInputStream(path);
        BufferedInputStream bufferedIn = new BufferedInputStream(fileIn);
        DataInputStream in = new DataInputStream(bufferedIn);
        
        CompressionModel model = instantiateCompressionModel(in.readUTF());
        model.load(in);
        
        in.close();
        return model;
    }
    
    /**
     * Saves the specified model to the specified file path.
     * @param path
     * @throws IOException
     * 
     * @see org.toubassi.femtozip.CompressionModel.loadModel(String path) throws IOException
     */
    public void save(String path) throws IOException {
        FileOutputStream fileOut = new FileOutputStream(path);
        BufferedOutputStream bufferedOut = new BufferedOutputStream(fileOut);
        DataOutputStream out = new DataOutputStream(bufferedOut);
        
        out.writeUTF(getClass().getName());
        
        save(out);
        
        out.close();
    }
    
    public abstract void build(DocumentList documents) throws IOException;

    public void compress(ByteBuffer data, OutputStream out) throws IOException {
        getSubstringPacker().pack(data, this, null);
    }
    
    /**
     * Decompresses the specified data.
     * @param data The data to decompress.
     * @return The decompressed data
     */
    public abstract ByteBuffer decompress(ByteBuffer compressedData);

    /**
     * Compresses the specified data.
     * @param data The data to compress.
     * @return The compressed data
     */
    public ByteBuffer compress(ByteBuffer data) {
        ByteBuffer compressed = ByteBuffer.allocate((int) (data.remaining() * 0.5));
        ByteBufferOutputStream bbos = new ByteBufferOutputStream(compressed, true);
        try {
            compress(data, bbos);
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException("IOException", e);
        }
        return bbos.toByteBuffer();
    }


    protected void buildDictionaryIfUnspecified(DocumentList documents) throws IOException {
        if(dictionary.remaining() == 0) {
            dictionary = (this.maxDictionaryLength != 0) ? buildDictionary(documents, this.maxDictionaryLength) : buildDictionary(documents);
        }
    }

    protected static ByteBuffer buildDictionary(DocumentList documents, int maxDictionaryLength) throws IOException {
        DictionaryOptimizer optimizer = new DictionaryOptimizer(documents);
        return optimizer.optimize(maxDictionaryLength);
    }

    protected static ByteBuffer buildDictionary(DocumentList documents) throws IOException {
        return buildDictionary(documents, 64*1024);
    }

    protected SubstringPacker.Consumer createModelBuilder() {
        return null;
    }
    
    protected SubstringPacker.Consumer buildEncodingModel(DocumentList documents) {
        try {
            SubstringPacker modelBuildingPacker = new SubstringPacker(dictionary);
            SubstringPacker.Consumer modelBuilder = createModelBuilder();
            for (int i = 0, count = documents.size(); i < count; i++) {
                modelBuildingPacker.pack(documents.getBB(i), modelBuilder, null);
            }
            
            return modelBuilder;
        }
        catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
