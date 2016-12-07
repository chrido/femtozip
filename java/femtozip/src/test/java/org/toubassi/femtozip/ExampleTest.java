package org.toubassi.femtozip;

import java.io.IOException;

import java.nio.ByteBuffer;

import org.junit.Assert;
import org.junit.Test;

/**
 * A Simple API example, packaged as a unit test
 */
public class ExampleTest {

    @Test
    public void example() throws IOException {
        ArrayDocumentList trainingDocs = new ArrayDocumentList("http://espn.de", "http://popsugar.de",
                "http://google.de", "http://yahoo.de", "http://www.linkedin.com", "http://www.facebook.com",
                "http:www.stanford.edu");
        
        CompressionModel model = CompressionModel.buildOptimalModel(trainingDocs);
        ByteBuffer data = ByteBuffer.wrap("check out http://www.facebook.com/someone".getBytes("UTF-8"));
        ByteBuffer compressed = model.compress(data);
        data.rewind();
        
        ByteBuffer decompressed = model.decompress(compressed);

        Assert.assertTrue(data.equals(decompressed));
    }
}
