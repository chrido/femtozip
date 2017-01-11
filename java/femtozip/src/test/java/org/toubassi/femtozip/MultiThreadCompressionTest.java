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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Random;

import java.nio.ByteBuffer;


import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.toubassi.femtozip.models.*;

import static junit.framework.TestCase.assertNull;
import static org.toubassi.femtozip.util.FileUtil.getString;


@RunWith(Parameterized.class)
public class MultiThreadCompressionTest {

    private final CompressionModels model;

    public MultiThreadCompressionTest(CompressionModels model) {
        this.model = model;
    }

    @Parameterized.Parameters()
    public static Iterable<Object[]> data() {
        return Arrays.asList(new Object[][] {
                /*{ new VerboseStringCompressionModel() },
                { new FemtoZipCompressionModel() },
                { new GZipDictionaryCompressionModel() },
                { new GZipCompressionModel() },*/
                { CompressionModels.PureHuffmann },
                /*{ new VariableIntCompressionModel() }*/
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
        ByteBuffer dictionary;
        Exception e;
        
        public CompressionThread(long runTimeMillis, CompressionModel model, ByteBuffer dictionary) {
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
            ByteBuffer sourceBytes = ByteBuffer.wrap(source.getBytes());
            ByteBuffer compressedBytes = ByteBuffer.allocate(sourceBytes.remaining());
            model.compress(sourceBytes, compressedBytes);

            ByteBuffer decompressedBytes = ByteBuffer.allocate(sourceBytes.remaining());
            model.decompress(compressedBytes, decompressedBytes);

            String decompressedString = getString(decompressedBytes);
            Assert.assertEquals(source, decompressedString);
        }

        public void run() {
            while (true) {
                if (start == 0) {
                    start = System.currentTimeMillis();
                } else if (System.currentTimeMillis() - start > runTime) {
                    return;
                }

                testModel(model, source);
            }
        }
    }
    
    void testThreadedCompressionModel(CompressionModels modelType) throws IOException, InterruptedException {
        System.out.println(model.getClass().getName());

        Random random = new Random();
        StringBuilder dict = new StringBuilder();
        for (int i = 0, count = 256 + random.nextInt(64); i < count; i++) {
            dict.append('a' + random.nextInt(26));
        }
        ByteBuffer dictionary = ByteBuffer.wrap(dict.toString().getBytes());

        CompressionModel model = CompressionModelBase.buildModel(new ArrayDocumentList(dictionary), dictionary, modelType);

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
            assertNull("Exception in thread " + thread.getId() + " : " + model.getClass() + " " + thread.e, thread.e);
        }
    }
}
