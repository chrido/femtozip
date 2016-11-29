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

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.junit.Test;
import org.toubassi.femtozip.ArrayDocumentList;
import org.toubassi.femtozip.compression.CompressionTest;
import org.toubassi.femtozip.dictionary.DictionaryOptimizer;
import org.toubassi.femtozip.models.FemtoZipCompressionModel;
import org.toubassi.femtozip.models.NativeCompressionModel;


public class NativeCompressionModelTest {
    
    /**
     * A Simple API example, packaged as a unit test
     */
    @Test
    public void testNativeModel() throws IOException {
        NativeCompressionModel model = new NativeCompressionModel();
        FemtoZipCompressionModel fModel = new FemtoZipCompressionModel();

        ByteBuf sourceBytes = Unpooled.wrappedBuffer(CompressionTest.PreambleString.getBytes());
        ByteBuf dictionaryBytes = Unpooled.wrappedBuffer(CompressionTest.PreambleDictionary.getBytes());

        model.setDictionary(dictionaryBytes);
        model.build(new ArrayDocumentList(sourceBytes));

        DictionaryOptimizerTest.testBuiltModel(model, sourceBytes, 187);
        //DictionaryOptimizerTest.testModel(CompressionTest.PreambleString, CompressionTest.PreambleDictionary, fModel, 187);
        
        File modelFile = File.createTempFile("native", ".fzm");
        
        model.save(modelFile.getPath());
        model = new NativeCompressionModel();
        model.load(modelFile.getPath());

        modelFile.delete();
    }
    
    
    @Test
    public void testNativeModel2() throws IOException {
        
        // Generate sample documents to train
        ArrayList<ByteBuf> trainingDocs = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            trainingDocs.add(generateSampleDoc((int)(Math.random() * 100) + 100));
        }

        NativeCompressionModel model = new NativeCompressionModel();
        model.build(new ArrayDocumentList(trainingDocs));
        
        for (int i = 0; i < 100; i++) {
            ByteBuf doc = generateSampleDoc((int)(Math.random() * 100) + 100);
            DictionaryOptimizerTest.testBuiltModel(model, doc, -1);
        }
    }
    
    private ByteBuf generateSampleDoc(int length) {
        ByteBuf out = Unpooled.buffer();
        
        for (int i = 0; i < length; i++) {
            if (Math.random() < .1) {
                out.writeByte(0);
                out.writeByte(1);
                out.writeByte(2);
                out.writeByte(3);
                out.writeByte(4);
                out.writeByte(5);
            }
            else {
                out.writeByte((int)(Math.random() * 0xff));
            }
        }
        
        return out;
    }

}
