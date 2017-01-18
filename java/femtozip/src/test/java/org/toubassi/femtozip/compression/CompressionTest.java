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

import java.io.IOException;
import java.util.ArrayList;

import java.nio.ByteBuffer;

import org.junit.Assert;
import org.junit.Test;
import org.toubassi.femtozip.ArrayDocumentList;
import org.toubassi.femtozip.CompressionModel;
import org.toubassi.femtozip.dictionary.DictionaryOptimizer;
import org.toubassi.femtozip.models.CompressionModelBase;
import org.toubassi.femtozip.models.FemtoZipCompressionModel;
import org.toubassi.femtozip.models.VariableIntCompressionModel;

import static org.toubassi.femtozip.util.FileUtil.getString;


public class CompressionTest {
    public static String PreambleString = "We the People of the United States, in Order to form a more perfect Union, establish Justice, insure domestic Tranquility, provide for the common defence, promote the general Welfare, and secure the Blessings of Liberty to ourselves and our Posterity, do ordain and establish this Constitution for the United States of America.";
    public static String PreambleDictionary = " of and for the a United States ";

    public static String PanamaString = "a man a plan a canal panama";
    

    private static void testDictionary(String document, String expectedDictionary) throws IOException {
        DictionaryOptimizer dictOpt = new DictionaryOptimizer(new ArrayDocumentList(document));
        ByteBuffer dc = dictOpt.optimize(8 * 1024);

        String actualDictionary= dictionaryToString(dc);
        Assert.assertEquals(expectedDictionary, actualDictionary);
    }

    @Test
    public void testDictionaryPanama() throws IOException {
        testDictionary(PanamaString, "an a ");
    }

    @Test
    public void testDictionaryPreamble() throws IOException {
        testDictionary(PreambleString, " our to , ince, sticure and , proity, s of e the for the establish the United States");
    }

    private static String dictionaryToString(ByteBuffer dictionary) {
        return getString(dictionary);
    }

    @Test
    public void testDocumentUniquenessScoring() throws IOException {
        ArrayList<ByteBuffer> documents = new ArrayList<>();
        documents.add(ByteBuffer.wrap(new String("garrick1garrick2garrick3garrick4garrick").getBytes("UTF-8")));
        documents.add(ByteBuffer.wrap(new String("xtoubassigarrick").getBytes("UTF-8")));
        documents.add(ByteBuffer.wrap(new String("ytoubassi").getBytes("UTF-8")));
        documents.add(ByteBuffer.wrap(new String("ztoubassi").getBytes("UTF-8")));

        ByteBuffer optimizedDictionary = DictionaryOptimizer.getOptimizedDictionary(new ArrayDocumentList(documents), 64 * 1024);

        String dictionary = dictionaryToString(optimizedDictionary);
        Assert.assertEquals("garricktoubassi", dictionary);
    }

    @Test
    public void testNonexistantStrings() throws IOException {
        ArrayList<ByteBuffer> documents = new ArrayList<>();
        documents.add(ByteBuffer.wrap(new String("http://espn.de").getBytes("UTF-8")));
        documents.add(ByteBuffer.wrap(new String("http://popsugar.de").getBytes("UTF-8")));
        documents.add(ByteBuffer.wrap(new String("http://google.de").getBytes("UTF-8")));
        documents.add(ByteBuffer.wrap(new String("http://yahoo.de").getBytes("UTF-8")));
        documents.add(ByteBuffer.wrap(new String("gtoubassi").getBytes("UTF-8")));
        documents.add(ByteBuffer.wrap(new String("gtoubassi").getBytes("UTF-8")));

        ByteBuffer optimizedDictionary = DictionaryOptimizer.getOptimizedDictionary(new ArrayDocumentList(documents), 64 * 1024);
        String dictionary = dictionaryToString(optimizedDictionary);
        // Make sure it doesn't think .dehttp:// is a good one
        Assert.assertEquals("gtoubassihttp://", dictionary);
    }
    
    @Test
    public void testVariableIntCompressionModel() throws IOException {
        String source = "12345";
        ByteBuffer sourceBytes = ByteBuffer.wrap(source.getBytes("UTF-8"));
        ByteBuffer compressed = ByteBuffer.allocate(sourceBytes.remaining());
        ByteBuffer decompressed = ByteBuffer.allocate(sourceBytes.remaining());

        VariableIntCompressionModel model = new VariableIntCompressionModel();
        //model.build(new ArrayDocumentList(sourceBytes));

        int length = model.compress(sourceBytes, compressed);

        Assert.assertEquals(2, compressed.remaining());
        Assert.assertEquals(2, length);

        model.decompress(compressed, decompressed);

        String decompressedString = getString(decompressed);
        Assert.assertEquals(source, decompressedString);
    }
}
