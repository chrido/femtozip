package org.toubassi.femtozip;

import java.io.IOException;

import java.nio.ByteBuffer;

import org.junit.Assert;
import org.junit.Test;
import org.toubassi.femtozip.models.CompressionModelBase;
import org.toubassi.femtozip.models.CompressionModelVariant;

/**
 * A Simple API example, packaged as a unit test
 */
public class ExampleTest {

    @Test
    public void example() throws IOException {
        ArrayDocumentList trainingDocs = new ArrayDocumentList("http://espn.de", "http://popsugar.de",
                "http://google.de", "http://yahoo.de", "http://www.linkedin.com", "http://www.facebook.com",
                "http:www.stanford.edu");
        
        //CompressionModel model = CompressionModelBase.buildOptimalModel(trainingDocs);
        CompressionModel model = CompressionModelBase.buildOptimalModel(trainingDocs);
        ByteBuffer originalData = ByteBuffer.wrap("check out http://www.facebook.com/someone".getBytes("UTF-8"));
        int orignalUncompressedLength = originalData.remaining();

        ByteBuffer compressedResult = ByteBuffer.allocate(originalData.remaining());

        model.compress(originalData, compressedResult);
        originalData.rewind();

        ByteBuffer decompressedResult = ByteBuffer.allocate(originalData.remaining());
        int decompressedLength = model.decompress(compressedResult, decompressedResult);

        Assert.assertEquals(orignalUncompressedLength, decompressedLength);
        Assert.assertTrue(originalData.equals(decompressedResult));
    }
}
