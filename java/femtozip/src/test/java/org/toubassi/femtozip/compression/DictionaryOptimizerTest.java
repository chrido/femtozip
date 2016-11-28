package org.toubassi.femtozip.compression;


import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.toubassi.femtozip.ArrayDocumentList;
import org.toubassi.femtozip.CompressionModel;
import org.toubassi.femtozip.models.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Objects;

@RunWith(Parameterized.class)
public class DictionaryOptimizerTest {
    public static String PreambleString = "We the People of the United States, in Order to form a more perfect Union, establish Justice, insure domestic Tranquility, provide for the common defence, promote the general Welfare, and secure the Blessings of Liberty to ourselves and our Posterity, do ordain and establish this Constitution for the United States of America.";
    public static String PreambleDictionary = " of and for the a United States ";
    private final String source;
    private final String dictionary;
    private final CompressionModel model;
    private final int expectedSize;

    @Parameterized.Parameters()
    public static Iterable<Object[]> data() {
        String[][] testPairs = {{PreambleString, PreambleDictionary}, {"",""}};
        ArrayList<Object[]> allCombinations = new ArrayList<>();
        for (String[] testPair : testPairs) {
            allCombinations.add(new Object[]{testPair[0], testPair[1], new VerboseStringCompressionModel(), testPair[0].length() == 0 ? -1 : 363});
            allCombinations.add(new Object[]{testPair[0], testPair[1], new FemtoZipCompressionModel(), testPair[0].length() == 0 ? -1 : 205});
            allCombinations.add(new Object[]{testPair[0], testPair[1], new GZipDictionaryCompressionModel(), testPair[0].length() == 0 ? -1 : 204});
            allCombinations.add(new Object[]{testPair[0], testPair[1], new GZipCompressionModel(), testPair[0].length() == 0 ? -1 : 210});
            allCombinations.add(new Object[]{testPair[0], testPair[1], new PureHuffmanCompressionModel(), testPair[0].length() == 0 ? -1 : 211});
            allCombinations.add(new Object[]{testPair[0], testPair[1], new VariableIntCompressionModel(), testPair[0].length() == 0 ? -1 : 333});
        }
        return allCombinations;
    }

    public DictionaryOptimizerTest(String source, String dictionary, CompressionModel model, int expectedSize) {
        this.source = source;
        this.dictionary = dictionary;
        this.model = model;
        this.expectedSize = expectedSize;
    }

    @Test
    public void testModel() throws IOException {
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
}
