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
package org.toubassi.femtozip.models;

import java.nio.ByteBuffer;


import org.toubassi.femtozip.CompressionModel;
import org.toubassi.femtozip.DocumentList;
import org.toubassi.femtozip.SamplingDocumentList;
import org.toubassi.femtozip.coding.huffman.FrequencyHuffmanModel;
import org.toubassi.femtozip.dictionary.DictionaryOptimizer;
import org.toubassi.femtozip.models.femtozip.FemtoZipCompressionModelBuilder;
import org.toubassi.femtozip.models.femtozip.FemtoZipHuffmanModel;
import org.toubassi.femtozip.models.huffmann.FrequencyHuffmanModelBuilder;
import org.toubassi.femtozip.substring.SubstringPacker;
import org.toubassi.femtozip.util.StreamUtil;

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
 * 2. Call the static CompressionModelBase.buildOptimalModel with a DocumentList
 *    which can be used to iterate the documents.  There are several built in
 *    DocumentLists if the data can be stored in memory, or you can implement
 *    your own.  A newly created CompressionModelBase will be returned.
 * 3. Call the CompressionModelBase.save(String) to save the model to a file.
 * 4. Later (perhaps in a different process), load the model via the static
 *    CompressionModelBase.loadModel(String);
 * 5. Use CompressionModelBase.compressDeprecated/decompressDeprecated as needed.
 * 
 * For a simple pure Java example, see the org.toubassi.femtozip.ExampleTest JUnit test
 * case in the source distribution of FemtoZip at http://github.com/gtoubassi/femtozip
 * 
 * To use the JNI interface to FemtoZip, you will follow largely the same recipe, but you
 * will use the NativeCompressionModel.
 * 
 * @see org.toubassi.femtozip.models.NativeCompressionModel
 */
public class CompressionModelBase {

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
     * @return The newly created CompressionModelBase
     * @throws IOException
     */
    public static CompressionModel buildOptimalModel(DocumentList documents) throws IOException {
        return buildOptimalModel(documents,  new ArrayList<ModelOptimizationResult>(), null, false);
    }

    public static CompressionModel buildModel(CompressionModelVariant model, DocumentList documents) throws IOException {
        return buildModel(model, documents, 64*1024);
    }

    public static CompressionModel buildModel(CompressionModelVariant model, DocumentList documents, int maxDictionaryLength) throws IOException {
        return buildModel(model, documents, DictionaryOptimizer.getOptimizedDictionary(documents, maxDictionaryLength));
    }

    public static CompressionModel buildModel(CompressionModelVariant variant, DocumentList documents, ByteBuffer dictionary) throws IOException {
        switch (variant) {
            case VerboseString:
                return new VerboseStringCompressionModel(dictionary);
            case VariableInt:
                return new VariableIntCompressionModel();
            case PureHuffmann:
                return new PureHuffmanCompressionModel(FrequencyHuffmanModelBuilder.buildModel(documents));
            case GZipDictionary:
                return new GZipDictionaryCompressionModel(dictionary);
            case FemtoZip:
                return new FemtoZipCompressionModel(FemtoZipCompressionModelBuilder.buildModel(dictionary, documents), dictionary);
            case GZip:
                return new GZipCompressionModel();
            case Native:
                return new NativeCompressionModel();
        }
        throw new RuntimeException("Unable to match CompressionModelVariant");
    }

    public static CompressionModel buildOptimalModel(DocumentList documents, List<CompressionModelBase.ModelOptimizationResult> results, CompressionModelVariant[] competingModels, boolean verify) throws IOException {

        CompressionModelVariant[] models;
        if(competingModels == null || competingModels.length == 0)
            models = CompressionModelVariant.values();
        else
            models = competingModels.clone();

        // Split the documents into two groups.  One for building each model out
        // and one for testing which model is best.  Shouldn't build and test
        // with the same set as a model may over optimize for the training set.
        SamplingDocumentList trainingDocuments = new SamplingDocumentList(documents, 2, 0);
        SamplingDocumentList testingDocuments = new SamplingDocumentList(documents, 2, 1);

        // Build the dictionary once to avoid rebuilding for each model.
        ByteBuffer dictionary = DictionaryOptimizer.getOptimizedDictionary(trainingDocuments, 64 * 1024);

        for(CompressionModelVariant model: models) {
            CompressionModel compressionModel = buildModel(model, trainingDocuments, dictionary.slice());
            results.add(new ModelOptimizationResult(compressionModel));
        }

        // Pick the best model
        for (int i = 0, count = testingDocuments.size(); i < count; i++) {
            ByteBuffer data = testingDocuments.getBB(i);

            for (ModelOptimizationResult result : results) {
                data.rewind();

                ByteBuffer compressed = ByteBuffer.allocate(data.remaining() * 2);
                result.model.compress(data, compressed);

                if (verify) {
                    data.rewind();
                    ByteBuffer verifyResult = ByteBuffer.allocate(data.remaining());
                    result.model.decompress(compressed, verifyResult);

                    if (!verifyResult.equals(data)) {
                        throw new RuntimeException("Compress/Decompress round trip failed for " + result.model.getClass().getSimpleName());
                    }
                }
                
                result.totalCompressedSize += compressed.remaining();
                result.totalDataSize += data.remaining();
            }
        }
        
        Collections.sort(results);
        
        ModelOptimizationResult bestResult = results.get(0);
        return bestResult.model;
    }
    
    /**
     * Loads a model previously saved with save.  You must use this
     * static because it dynamically instantiates the correct
     * model based on the type that was saved.
     * @param path
     * @throws IOException
     * 
     * @see CompressionModelBase.save(String path) throws IOException
     */
    public static CompressionModel loadModel(String path) throws IOException {
        try(FileInputStream fileIn = new FileInputStream(path);
            BufferedInputStream bufferedIn = new BufferedInputStream(fileIn);
            DataInputStream in = new DataInputStream(bufferedIn))
        {
            return loadModel(in);
        }
    }

    public static CompressionModel loadModel(DataInputStream in) throws IOException {

        String compressionModel = in.readUTF();

        if (compressionModel.indexOf('.') == -1) {
            compressionModel = FemtoZipCompressionModel.class.getPackage().getName() + "." + compressionModel;
            if (!compressionModel.endsWith("CompressionModel")) {
                compressionModel += "CompressionModel";
            }
        }

        if(compressionModel.equals(FemtoZipCompressionModel.class.getName())) {
            return loadFemtoZipCompressionModel(in);
        }else if (compressionModel.equals(GZipCompressionModel.class.getName())){
            return new GZipCompressionModel();
        }else if (compressionModel.equals(GZipDictionaryCompressionModel.class.getName())) {
            return loadGZipDictionaryCompressionModel(in);
        }else if (compressionModel.equals(PureHuffmanCompressionModel.class.getName())) {
            return loadPureHuffmanCompressionModel(in);
        } else if(compressionModel.equals(VariableIntCompressionModel.class.getName())) {
            return new VariableIntCompressionModel();
        } else if(compressionModel.equals(VerboseStringCompressionModel.class.getName())) {
            return loadVerboseStringcompressionModel(in);
        }
        throw new IOException("Could not initialize Compression Model");
    }

    private static CompressionModel loadVerboseStringcompressionModel(DataInputStream in) throws IOException {
        if(in.readInt() == 0) { // file format version, currently 0.
            ByteBuffer dictionary = readDictionary(in);
            return new VerboseStringCompressionModel(dictionary);
        }
        throw new IOException("Unknown version number");
    }

    private static ByteBuffer readDictionary(DataInputStream in) throws IOException {
        int dictionaryLength = in.readInt();
        if (dictionaryLength == -1) {
            throw new IOException("No dictionary set, maybe you wanted to restore a GZipCompressionModel?");
        } else {
            byte[] dictionary = new byte[dictionaryLength];
            int totalRead = StreamUtil.readBytes(in, dictionary, dictionaryLength);
            if (totalRead != dictionaryLength) {
                throw new IOException("Bad model in stream.  Could not read dictionary of length " + dictionaryLength);
            }

            return  ByteBuffer.wrap(dictionary);
        }
    }

    private static CompressionModel loadPureHuffmanCompressionModel(DataInputStream in) throws IOException {
        if(in.readInt() == 0) { // file format version, currently 0.
            FrequencyHuffmanModel frequencyHuffmanModel = new FrequencyHuffmanModel(in);
            return new PureHuffmanCompressionModel(frequencyHuffmanModel);
        }
        throw new IOException("Unknown version number");
    }

    private static CompressionModel loadGZipDictionaryCompressionModel(DataInputStream in) throws IOException {
        if(in.readInt() == 0) { // file format version, currently 0.
            ByteBuffer dictionary = readDictionary(in);
            return new GZipDictionaryCompressionModel(dictionary);
        }
        throw new IOException("Unknown version number");
    }

    private static CompressionModel loadFemtoZipCompressionModel(DataInputStream in) throws IOException {
        int version = in.readInt();//Version

        if(version == 0) {
            ByteBuffer dictionary = DictionaryOptimizer.readDictionary(in);
            FemtoZipHuffmanModel femtoZipHuffmanModel = new FemtoZipHuffmanModel(in);
            return new FemtoZipCompressionModel(femtoZipHuffmanModel, dictionary);
        }
        throw new IOException("Unknown version number");
    }

    /**
     * Saves the specified model to the specified file path.
     * @param path
     * @throws IOException
     * 
     * @see CompressionModelBase.loadModel(String path) throws IOException
     */
    public void save(CompressionModel model, String path) throws IOException {
        try(FileOutputStream fileOut = new FileOutputStream(path);
            BufferedOutputStream bufferedOut = new BufferedOutputStream(fileOut);
            DataOutputStream out = new DataOutputStream(bufferedOut))
        {
            out.writeUTF(model.getClass().getName());
            model.save(out);
        }
    }
    
    public static ByteBuffer buildDictionary(DocumentList documents, int maxDictionaryLength) throws IOException {
        DictionaryOptimizer optimizer = new DictionaryOptimizer(documents);
        return optimizer.optimize(maxDictionaryLength);
    }

    public static ByteBuffer buildDictionary(DocumentList documents) throws IOException {
        return buildDictionary(documents, 64*1024);
    }
 }
