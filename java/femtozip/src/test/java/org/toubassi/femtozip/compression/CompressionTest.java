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
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.junit.Assert;
import org.junit.Test;
import org.toubassi.femtozip.ArrayDocumentList;
import org.toubassi.femtozip.CompressionModel;
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

    private static String dictionaryToString(ByteBuf dictionary) {
        return dictionary.toString(Charset.forName("UTF-8"));
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
