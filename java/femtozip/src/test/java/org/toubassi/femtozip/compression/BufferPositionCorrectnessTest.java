package org.toubassi.femtozip.compression;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.toubassi.femtozip.ArrayDocumentList;
import org.toubassi.femtozip.CompressionModel;
import org.toubassi.femtozip.DocumentList;
import org.toubassi.femtozip.TestUtil;
import org.toubassi.femtozip.models.CompressionModelBase;
import org.toubassi.femtozip.models.CompressionModelVariant;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.ArrayList;

import static org.toubassi.femtozip.TestUtil.generateSampleDoc;
import static org.toubassi.femtozip.TestUtil.getTrainingDocs;


@RunWith(Parameterized.class)
public class BufferPositionCorrectnessTest {

    private final CompressionModelVariant variant;

    public BufferPositionCorrectnessTest(CompressionModelVariant variant) {
        this.variant = variant;
    }

    @Parameterized.Parameters()
    public static Iterable<Object[]> data() {
        return TestUtil.getActiveCompressionModels();
    }

    @Test
    public void testCompressingShouldSetCorrectLimitsAndPositions() throws IOException {
        ArrayList<ByteBuffer> trainingDocs = getTrainingDocs();

        CompressionModel compressionModel = CompressionModelBase.buildModel(variant, new ArrayDocumentList(trainingDocs));

        ByteBuffer toBeCompressed = ByteBuffer.allocate(500);
        toBeCompressed.put("compressed".getBytes(Charset.forName("UTF-8")));
        TestUtil.generateSampleDoc(toBeCompressed, toBeCompressed.remaining());

        ByteBuffer compressedResult = ByteBuffer.allocate(1500);
        compressedResult.put("compressed".getBytes(Charset.forName("UTF-8")));
        compressedResult.put((byte)1);

        int initialToBeCompressedPosition = 5;
        toBeCompressed.position(initialToBeCompressedPosition);

        int initialCompressedResultPosition = compressedResult.position();

        //
        int compressedWritten = compressionModel.compress(toBeCompressed, compressedResult);

        //Position should be the same as before so we can read until the limit
        Assert.assertEquals("Error: " + variant.name(), initialCompressedResultPosition, compressedResult.position());

        //The limit of the buffer should be set correctly
        Assert.assertEquals("Error: " + variant.name(), initialCompressedResultPosition + compressedWritten, compressedResult.limit());

        //The limit of the buffer to be read should be reached
        Assert.assertEquals("Error: " + variant.name(), toBeCompressed.limit(), toBeCompressed.position());
    }
}
