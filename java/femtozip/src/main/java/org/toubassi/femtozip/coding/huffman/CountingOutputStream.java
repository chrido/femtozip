package org.toubassi.femtozip.coding.huffman;

import java.io.IOException;
import java.io.OutputStream;

public class CountingOutputStream extends OutputStream {

    private final OutputStream out;
    private int writtenBytes;
    private int maxMark;

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

    @Override
    public void write(byte[] b, int off, int len) throws IOException {
        out.write(b, off, len);
        maxMark = Math.max(maxMark, off + len);
    }

    public int getWrittenBytes() {
        return Math.max(writtenBytes, maxMark);
    }
}
