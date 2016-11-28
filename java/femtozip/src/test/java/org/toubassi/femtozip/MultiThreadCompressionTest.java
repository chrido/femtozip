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

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Random;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import junit.framework.Assert;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.toubassi.femtozip.models.GZipCompressionModel;
import org.toubassi.femtozip.models.GZipDictionaryCompressionModel;
import org.toubassi.femtozip.models.FemtoZipCompressionModel;
import org.toubassi.femtozip.models.PureHuffmanCompressionModel;
import org.toubassi.femtozip.models.VariableIntCompressionModel;
import org.toubassi.femtozip.models.VerboseStringCompressionModel;


@RunWith(Parameterized.class)
public class MultiThreadCompressionTest {

    private final CompressionModel model;

    public MultiThreadCompressionTest(CompressionModel model) {
        this.model = model;
    }

    @Parameterized.Parameters()
    public static Iterable<Object[]> data() {
        return Arrays.asList(new Object[][] {
                { new VerboseStringCompressionModel()}, { new FemtoZipCompressionModel()}, {new GZipDictionaryCompressionModel()}, { new GZipCompressionModel()}, {new PureHuffmanCompressionModel()}, {new VariableIntCompressionModel()}
        });
    }

    @Test
    public void testThreading() throws IOException, InterruptedException {
        testThreadedCompressionModel(model);
    }
    
    public static class CompressionThread extends Thread {
        
        long start;
        long runTime;
        CompressionModel model;
        String source;
        ByteBuf dictionary;
        Exception e;
        
        public CompressionThread(long runTimeMillis, CompressionModel model, ByteBuf dictionary) {
            runTime = runTimeMillis;
            this.model = model;
            Random random = new Random();
            this.dictionary = dictionary;
            
            StringBuilder s = new StringBuilder();
            for (int i = 0, count = 256 + random.nextInt(64); i < count; i++) {
                s.append('a' + random.nextInt(26));
            }
            source = s.toString();
        }
        
        private void testModel(CompressionModel model, String source) {
            ByteBuf sourceBytes = Unpooled.wrappedBuffer(source.getBytes());
            ByteBuf compressedBytes = model.compress(sourceBytes);

            ByteBuf decompressedBytes = model.decompress(compressedBytes);
            String decompressedString = decompressedBytes.toString(Charset.forName("UTF-8"));
            
            Assert.assertEquals(source, decompressedString);
        }
        
        public void run() {
            try {
                while (true) {
                    if (start == 0) {
                        start = System.currentTimeMillis();
                    }
                    else if (System.currentTimeMillis() - start > runTime) {
                        return;
                    }
                    
                    testModel(model, source);
                }
            }
            catch (Exception e){
                this.e = e;
            }
        }
        
    }
    
    void testThreadedCompressionModel(CompressionModel model) throws IOException, InterruptedException {
        Random random = new Random();
        StringBuilder dict = new StringBuilder();
        for (int i = 0, count = 256 + random.nextInt(64); i < count; i++) {
            dict.append('a' + random.nextInt(26));
        }
        ByteBuf dictionary = Unpooled.wrappedBuffer(dict.toString().getBytes());
        
        model.setDictionary(dictionary);
        model.build(new ArrayDocumentList(dictionary));
        
        ArrayList<CompressionThread> threads = new ArrayList<CompressionThread>();
        threads.add(new CompressionThread(500, model, dictionary));
        threads.add(new CompressionThread(500, model, dictionary));
        threads.add(new CompressionThread(500, model, dictionary));
        threads.add(new CompressionThread(500, model, dictionary));
        threads.add(new CompressionThread(500, model, dictionary));
        threads.add(new CompressionThread(500, model, dictionary));
        threads.add(new CompressionThread(500, model, dictionary));
        
        for (CompressionThread thread : threads) {
            thread.start();
        }
        
        for (CompressionThread thread : threads) {
            thread.join();
            if (thread.e != null) {
                thread.e.printStackTrace();
            }
            Assert.assertNull("Exception in thread " + thread.getId() + " : " + model.getClass() + " " + thread.e, thread.e);
        }
    }
}
