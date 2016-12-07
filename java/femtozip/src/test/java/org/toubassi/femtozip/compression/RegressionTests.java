package org.toubassi.femtozip.compression;


import java.nio.ByteBuffer;

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
public class RegressionTests {
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

    public RegressionTests(String source, String dictionary, CompressionModel model, int expectedSize) {
        this.source = source;
        this.dictionary = dictionary;
        this.model = model;
        this.expectedSize = expectedSize;
    }

    @Test
    public void testModel() throws IOException {
        ByteBuffer sourceBytes = ByteBuffer.wrap(source.getBytes());
        if(dictionary != null) {
            ByteBuffer dictionaryBytes = ByteBuffer.wrap(dictionary.getBytes());
            model.setDictionary(dictionaryBytes);
        }
        model.build(new ArrayDocumentList(sourceBytes));

        testBuiltModel(model, sourceBytes, expectedSize);
    }

    public static void testBuiltModel(CompressionModel model, ByteBuffer sourceBytes, int expectedSize) throws IOException {
        ByteBuffer compressedBytes = model.compress(sourceBytes);

        if (expectedSize >= 0) {
            Assert.assertEquals(expectedSize, compressedBytes.remaining());
        }

        ByteBuffer decompressedBytes = model.decompress(compressedBytes);

        sourceBytes.reset();
        Assert.assertTrue(sourceBytes.equals(decompressedBytes));
    }
}
