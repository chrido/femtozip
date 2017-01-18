package org.toubassi.femtozip;


import java.io.*;
import java.nio.ByteBuffer;

public interface CompressionModel {

    /**
     * The compressed result is written to @param compressedOut
     * In case the compressedOut ByteBuffer is to small, an exception is thrown
     * @param decompressedIn A <code>ByteBuffer</code> which is read from the current position to the limit
     * @param compressedOut A <code>ByteBuffer</code> which is written to from the current position onwards. The provided <code>ByteBuffer</code> has to big enough to hold the data
     * @return the total bytes written to the @param compressedOut <code>Bytebuffer</code>
     */
    int compress(ByteBuffer decompressedIn, ByteBuffer compressedOut);

    /**
     * The result is written to the OutPutStream. The OutPutStream is not flushed or closed, this is the caller responsibility
     *
     * @param decompressedIn A <code>ByteBuffer</code> which is read from the current position to the limit
     * @param compressedOut A <code>OutputStream</code> where the compressed data is written to
     * @return the total bytes written
     */
    int compress(ByteBuffer decompressedIn, OutputStream compressedOut) throws IOException;

    /**
     * The inverse of compressDeprecated
     * @param compressedIn Data is read from the current position until the limit
     * @param decompressedOut Data is written to the output bytebuffer from the current position on
     * @return
     */
    int decompress(ByteBuffer compressedIn, ByteBuffer decompressedOut);


    /**
     * The inverse of compressDeprecated
     * @param compressedIn Data is read from the current position until the limit
     * @param decompressedOut Data is written from the current position on.
     * @return
     */
    int decompress(InputStream compressedIn, ByteBuffer decompressedOut) throws IOException;


    /**
     * Saves the model
     * @param out
     * @throws IOException
     */
    void save(DataOutputStream out) throws IOException;
}
