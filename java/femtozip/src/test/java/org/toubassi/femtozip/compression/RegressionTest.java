package org.toubassi.femtozip.compression;


import java.nio.ByteBuffer;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.toubassi.femtozip.ArrayDocumentList;
import org.toubassi.femtozip.CompressionModel;
import org.toubassi.femtozip.coding.huffman.ByteBufferInputStream;
import org.toubassi.femtozip.coding.huffman.ByteBufferOutputStream;
import org.toubassi.femtozip.models.CompressionModelBase;
import org.toubassi.femtozip.models.*;

import java.io.IOException;
import java.util.ArrayList;

@RunWith(Parameterized.class)
public class RegressionTest {
    public static String PreambleString = "We the People of the United States, in Order to form a more perfect Union, establish Justice, insure domestic Tranquility, provide for the common defence, promote the general Welfare, and secure the Blessings of Liberty to ourselves and our Posterity, do ordain and establish this Constitution for the United States of America.";
    public static String PreambleDictionary = " of and for the a United States ";
    private final String source;
    private final ByteBuffer dictionary;
    private final CompressionModelVariant model;
    private final int expectedSize;

    @Parameterized.Parameters()
    public static Iterable<Object[]> data() {
        String[][] testPairs = {{PreambleString, PreambleDictionary}, {"",""}};
        ArrayList<Object[]> allCombinations = new ArrayList<>();
        for (String[] testPair : testPairs) {
            allCombinations.add(new Object[]{testPair[0], testPair[1], CompressionModelVariant.VerboseString, testPair[0].length() == 0 ? -1 : 363});
            allCombinations.add(new Object[]{testPair[0], testPair[1], CompressionModelVariant.FemtoZip, testPair[0].length() == 0 ? -1 : 205});
            allCombinations.add(new Object[]{testPair[0], testPair[1], CompressionModelVariant.GZipDictionary, testPair[0].length() == 0 ? -1 : 204});
            allCombinations.add(new Object[]{testPair[0], testPair[1], CompressionModelVariant.GZip, testPair[0].length() == 0 ? -1 : 210});
            allCombinations.add(new Object[]{testPair[0], testPair[1], CompressionModelVariant.PureHuffmann, testPair[0].length() == 0 ? -1 : 211});
            allCombinations.add(new Object[]{testPair[0], testPair[1], CompressionModelVariant.VariableInt, testPair[0].length() == 0 ? -1 : 333});
        }
        return allCombinations;
    }

    public RegressionTest(String source, String dictionary, CompressionModelVariant model, int expectedSize) {
        this.source = source;
        this.dictionary = ByteBuffer.wrap(dictionary.getBytes());
        this.model = model;
        this.expectedSize = expectedSize;
    }

    @Test
    public void testUsingByteBuffers() throws IOException {
        ByteBuffer sourceBytes = ByteBuffer.wrap(source.getBytes());

        CompressionModel compressionModel = CompressionModelBase.buildModel(this.model, new ArrayDocumentList(sourceBytes), dictionary);
        sourceBytes.rewind();

        testBuiltModelBytebuffers(compressionModel, sourceBytes, expectedSize);
    }

    public static void testBuiltModelBytebuffers(CompressionModel model, ByteBuffer sourceBytes, int expectedSize) throws IOException {
        //System.out.println(model.getClass().getName());

        ByteBuffer compressedBytes = ByteBuffer.allocate(sourceBytes.remaining() * 2); //just to be on the safe side
        int writtenSize = model.compress(sourceBytes, compressedBytes);

        if (expectedSize >= 0) {
            Assert.assertEquals("Error: " + model.getClass().getName(), expectedSize, writtenSize);
            Assert.assertEquals("Error: " + model.getClass().getName(), expectedSize, compressedBytes.remaining());
        }

        sourceBytes.rewind();
        ByteBuffer decompressedBytes = ByteBuffer.allocate(sourceBytes.remaining()*2);
        model.decompress(compressedBytes, decompressedBytes);
        decompressedBytes.rewind();

        sourceBytes.rewind();
        Assert.assertTrue("Error: " + model.getClass().getName(), sourceBytes.equals(decompressedBytes));
    }

    @Test
    public void testUsingInputOutputStreams() throws IOException {
        ByteBuffer sourceBytes = ByteBuffer.wrap(source.getBytes());


        CompressionModel compressionModel = CompressionModelBase.buildModel(this.model, new ArrayDocumentList(sourceBytes), dictionary);
        sourceBytes.rewind();

        testBuiltModelStreams(compressionModel, sourceBytes, expectedSize);
    }

    public static void testBuiltModelStreams(CompressionModel model, ByteBuffer sourceBytes, int expectedSize) throws IOException {
        //System.out.println(model.getClass().getName());

        ByteBuffer backingCompressed = ByteBuffer.allocate(sourceBytes.remaining() * 3); //just to be on the safe side
        ByteBufferOutputStream bbos = new ByteBufferOutputStream(backingCompressed);
        int writtenSize = model.compress(sourceBytes, bbos);
        backingCompressed.flip();

        if (expectedSize >= 0) {
            Assert.assertEquals("Error: " + model.getClass().getName(), expectedSize, writtenSize);
            Assert.assertEquals("Error: " + model.getClass().getName(), expectedSize, backingCompressed.remaining());
        }

        sourceBytes.rewind();


        ByteBufferInputStream bbis = new ByteBufferInputStream(backingCompressed);
        ByteBuffer decompressedBytes = ByteBuffer.allocate(sourceBytes.remaining()*3);
        model.decompress(bbis, decompressedBytes);

        sourceBytes.rewind();
        Assert.assertTrue("Error: " + model.getClass().getName(), sourceBytes.equals(decompressedBytes));
    }

}
