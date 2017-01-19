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
package org.toubassi.femtozip.compression;

import java.io.*;
import java.util.ArrayList;

import java.nio.ByteBuffer;

import org.junit.Ignore;
import org.junit.Test;
import org.toubassi.femtozip.ArrayDocumentList;
import org.toubassi.femtozip.CompressionModel;
import org.toubassi.femtozip.models.CompressionModelBase;
import org.toubassi.femtozip.models.CompressionModelVariant;
import org.toubassi.femtozip.models.NativeCompressionModel;

import static org.toubassi.femtozip.TestUtil.generateSampleDoc;


@Ignore
public class NativeCompressionModelTest {
    
    /**
     * A Simple API example, packaged as a unit test
     */
    @Test
    public void testNativeModel() throws IOException {

        ByteBuffer sourceBytes = ByteBuffer.wrap(CompressionTest.PreambleString.getBytes());
        ByteBuffer dictionaryBytes = ByteBuffer.wrap(CompressionTest.PreambleDictionary.getBytes());

        CompressionModel nativeCompressionModel = CompressionModelBase.buildModel(CompressionModelVariant.Native, new ArrayDocumentList(sourceBytes), dictionaryBytes);

        //CompressionModel femtoZipCompressionModel = CompressionModelBase.buildModel(new ArrayDocumentList(sourceBytes), dictionaryBytes, CompressionModelVariant.FemtoZip);

        RegressionTest.testBuiltModelBytebuffers(nativeCompressionModel, sourceBytes, 187);

        File modelFile = File.createTempFile("native", ".fzm");

        try(FileOutputStream fileOutputStream = new FileOutputStream(modelFile.getPath());
            DataOutputStream dataOutputStream = new DataOutputStream(fileOutputStream))
        {
            nativeCompressionModel.save(dataOutputStream);
        }

        nativeCompressionModel = new NativeCompressionModel();

        try(FileInputStream fileInputStream = new FileInputStream(modelFile.getPath());
            DataInputStream dataInputStream = new DataInputStream(fileInputStream))
        {
            throw new RuntimeException("TODO");
            //nativeCompressionModel.load(dataInputStream);
        }

        //modelFile.delete();
    }
    
    
    @Test
    public void testNativeModel2() throws IOException {
        
        // Generate sample documents to train
        ArrayList<ByteBuffer> trainingDocs = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            trainingDocs.add(generateSampleDoc((int)(Math.random() * 100) + 100));
        }

        NativeCompressionModel model = new NativeCompressionModel();
        model.build(new ArrayDocumentList(trainingDocs));
        
        for (int i = 0; i < 100; i++) {
            ByteBuffer doc = generateSampleDoc((int)(Math.random() * 100) + 100);
            int i1 = doc.remaining();
            RegressionTest.testBuiltModelBytebuffers(model, doc, -1);
        }
    }
}
