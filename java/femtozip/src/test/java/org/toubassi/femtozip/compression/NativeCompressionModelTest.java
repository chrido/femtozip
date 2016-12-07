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

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import java.nio.ByteBuffer;

import org.junit.Ignore;
import org.junit.Test;
import org.toubassi.femtozip.ArrayDocumentList;
import org.toubassi.femtozip.models.FemtoZipCompressionModel;
import org.toubassi.femtozip.models.NativeCompressionModel;


@Ignore
public class NativeCompressionModelTest {
    
    /**
     * A Simple API example, packaged as a unit test
     */
    @Test
    public void testNativeModel() throws IOException {
        NativeCompressionModel model = new NativeCompressionModel();
        FemtoZipCompressionModel fModel = new FemtoZipCompressionModel();

        ByteBuffer sourceBytes = ByteBuffer.wrap(CompressionTest.PreambleString.getBytes());
        ByteBuffer dictionaryBytes = ByteBuffer.wrap(CompressionTest.PreambleDictionary.getBytes());

        model.setDictionary(dictionaryBytes);
        model.build(new ArrayDocumentList(sourceBytes));

        RegressionTests.testBuiltModel(model, sourceBytes, 187);

        File modelFile = File.createTempFile("native", ".fzm");
        
        model.save(modelFile.getPath());
        model = new NativeCompressionModel();
        model.load(modelFile.getPath());

        modelFile.delete();
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
            RegressionTests.testBuiltModel(model, doc, -1);
        }
    }
    
    private ByteBuffer generateSampleDoc(int length) {
        ByteBuffer out = ByteBuffer.allocate(length);
        
        for (int i = 0; i < length; i++) {
            if (Math.random() < .1) {
                out.put((byte) 0);
                out.put((byte) 1);
                out.put((byte) 2);
                out.put((byte) 3);
                out.put((byte) 4);
                out.put((byte) 5);
            }
            else {
                out.put((byte) (Math.random() * 0xff));
            }
        }
        
        return out;
    }

}
