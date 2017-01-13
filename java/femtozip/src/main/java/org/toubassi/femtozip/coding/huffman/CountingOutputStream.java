package org.toubassi.femtozip.coding.huffman;

import java.io.IOException;
import java.io.OutputStream;

/**
 * Created by chris on 13.01.17.
 */
public class CountingOutputStream extends OutputStream {

    private final OutputStream out;
    private int writtenBytes;

    public CountingOutputStream(OutputStream out) {
        this.out = out;
        this.writtenBytes = 0;
    }

    @Override
    public void write(int b) throws IOException {
        out.write(b);
        this.writtenBytes++;
    }

    @Override
    public void write(byte[] b) throws IOException {
        out.write(b);
        this.writtenBytes += b.length;
    }

    public int getWrittenBytes() {
        return writtenBytes;
    }
}
