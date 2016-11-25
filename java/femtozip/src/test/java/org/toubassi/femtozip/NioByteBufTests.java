package org.toubassi.femtozip;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufOutputStream;
import org.junit.Test;
import org.toubassi.femtozip.models.FemtoZipCompressionModel;

import static io.netty.buffer.Unpooled.buffer;

/**
 * Created by chris on 22.11.16.
 */
public class NioByteBufTests {

    @Test
    public void testFemtoZipWithByteBuf() throws Exception {

        byte[] asByteArray = CompressionTest.PreambleString.getBytes();
        ByteBuf buf = buffer(asByteArray.length);
        buf.writeBytes(asByteArray);

        //ByteBufOutputStream bbos = new ByteBufOutputStream(buf);

        FemtoZipCompressionModel model = new FemtoZipCompressionModel();
        model.compress(buf);




    }
}
