package org.toubassi.femtozip;

import java.io.IOException;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.Unpooled;
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
        ByteBuf data = Unpooled.wrappedBuffer("check out http://www.facebook.com/someone".getBytes("UTF-8"));
        ByteBuf compressed = model.compress(data);
        
        ByteBuf decompressed = model.decompress(compressed);

        data.resetReaderIndex();
        Assert.assertTrue(ByteBufUtil.equals(data, decompressed));
    }
}
