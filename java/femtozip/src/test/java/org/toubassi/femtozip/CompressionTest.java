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

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.junit.Assert;
import org.junit.Test;
import org.toubassi.femtozip.models.GZipCompressionModel;
import org.toubassi.femtozip.models.GZipDictionaryCompressionModel;
import org.toubassi.femtozip.models.FemtoZipCompressionModel;
import org.toubassi.femtozip.models.PureHuffmanCompressionModel;
import org.toubassi.femtozip.models.VariableIntCompressionModel;
import org.toubassi.femtozip.models.VerboseStringCompressionModel;


public class CompressionTest {
    public static String PreambleString = "We the People of the United States, in Order to form a more perfect Union, establish Justice, insure domestic Tranquility, provide for the common defence, promote the general Welfare, and secure the Blessings of Liberty to ourselves and our Posterity, do ordain and establish this Constitution for the United States of America.";
    public static String PreambleDictionary = " of and for the a United States ";

    public static String PanamaString = "a man a plan a canal panama";
    
    
    @Test
    public void testDictionaryOptimizer() throws IOException {
        
        CompressionModel compressionModel = new FemtoZipCompressionModel();
        compressionModel.build(new ArrayDocumentList(PreambleString));
        
        String dictionary = dictionaryToString(compressionModel.getDictionary());
        Assert.assertEquals(" our to , ince, sticure and , proity, s of e the for the establish the United States", dictionary);

        compressionModel = new FemtoZipCompressionModel();
        compressionModel.build(new ArrayDocumentList(PanamaString));
        
        dictionary = dictionaryToString(compressionModel.getDictionary());
        Assert.assertEquals("an a ", dictionary);
    }
    
    
    @Test
    public void testCompressionModels() throws IOException {
        String[][] testPairs = {{PreambleString, PreambleDictionary}, {"",""}};
        for (String[] testPair : testPairs) {
            testModel(testPair[0], testPair[1], new VerboseStringCompressionModel(), testPair[0].length() == 0 ? -1 : 363);
            testModel(testPair[0], testPair[1], new FemtoZipCompressionModel(), testPair[0].length() == 0 ? -1 : 205);
            testModel(testPair[0], testPair[1], new GZipDictionaryCompressionModel(), testPair[0].length() == 0 ? -1 : 204);
            testModel(testPair[0], testPair[1], new GZipCompressionModel(), testPair[0].length() == 0 ? -1 : 210);
            testModel(testPair[0], testPair[1], new PureHuffmanCompressionModel(), testPair[0].length() == 0 ? -1 : 211);
            testModel(testPair[0], testPair[1], new VariableIntCompressionModel(), testPair[0].length() == 0 ? -1 : 333);
        }
    }
    
    private static String dictionaryToString(ByteBuf dictionary) {
        return dictionary.toString(Charset.forName("UTF-8"));
    }
    
    public static void testModel(String source, String dictionary, CompressionModel model, int expectedSize) throws IOException {
        ByteBuf sourceBytes = Unpooled.wrappedBuffer(source.getBytes());
        ByteBuf dictionaryBytes = dictionary == null ? null : Unpooled.wrappedBuffer(dictionary.getBytes());
        
        model.setDictionary(dictionaryBytes);
        model.build(new ArrayDocumentList(sourceBytes));
        
        testBuiltModel(model, sourceBytes, expectedSize);
    }
    
    public static void testBuiltModel(CompressionModel model, ByteBuf sourceBytes, int expectedSize) throws IOException {
        ByteBuf compressedBytes = model.compress(sourceBytes);

        if (expectedSize >= 0) {
            Assert.assertEquals(expectedSize, compressedBytes.readableBytes());
        }
        
        ByteBuf decompressedBytes = model.decompress(compressedBytes);

        Assert.assertTrue(sourceBytes.equals(decompressedBytes));

        sourceBytes.release();
        compressedBytes.release();
    }

    @Test
    public void testDocumentUniquenessScoring() throws IOException {
        CompressionModel model = new FemtoZipCompressionModel();
        ArrayList<ByteBuf> documents = new ArrayList<>();
        documents.add(Unpooled.wrappedBuffer(new String("garrick1garrick2garrick3garrick4garrick").getBytes("UTF-8")));
        documents.add(Unpooled.wrappedBuffer(new String("xtoubassigarrick").getBytes("UTF-8")));
        documents.add(Unpooled.wrappedBuffer(new String("ytoubassi").getBytes("UTF-8")));
        documents.add(Unpooled.wrappedBuffer(new String("ztoubassi").getBytes("UTF-8")));
        
        model.build(new ArrayDocumentList(documents));
        
        String dictionary = dictionaryToString(model.getDictionary());
        Assert.assertEquals("garricktoubassi", dictionary);
    }

    @Test
    public void testNonexistantStrings() throws IOException {
        CompressionModel model = new FemtoZipCompressionModel();
        ArrayList<ByteBuf> documents = new ArrayList<>();
        documents.add(Unpooled.wrappedBuffer(new String("http://espn.de").getBytes("UTF-8")));
        documents.add(Unpooled.wrappedBuffer(new String("http://popsugar.de").getBytes("UTF-8")));
        documents.add(Unpooled.wrappedBuffer(new String("http://google.de").getBytes("UTF-8")));
        documents.add(Unpooled.wrappedBuffer(new String("http://yahoo.de").getBytes("UTF-8")));
        documents.add(Unpooled.wrappedBuffer(new String("gtoubassi").getBytes("UTF-8")));
        documents.add(Unpooled.wrappedBuffer(new String("gtoubassi").getBytes("UTF-8")));
        
        model.build(new ArrayDocumentList(documents));
        
        String dictionary = dictionaryToString(model.getDictionary());
        // Make sure it doesn't think .dehttp:// is a good one
        Assert.assertEquals("gtoubassihttp://", dictionary);
    }
    
    @Test
    public void testVariableIntCompressionModel() throws IOException {
        String source = "12345";
        ByteBuf sourceBytes = Unpooled.wrappedBuffer(source.getBytes("UTF-8"));
        VariableIntCompressionModel model = new VariableIntCompressionModel();
        model.build(new ArrayDocumentList(sourceBytes));
        
        ByteBuf compressedBytes = model.compress(sourceBytes);

        Assert.assertEquals(2, compressedBytes.readableBytes());

        ByteBuf decompressedBytes = model.decompress(compressedBytes);
        String decompressedString = new String(decompressedBytes.array());
        
        Assert.assertEquals(source, decompressedString);
        
    }
}
