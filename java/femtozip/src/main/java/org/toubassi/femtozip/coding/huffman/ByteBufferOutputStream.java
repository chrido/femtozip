package org.toubassi.femtozip.coding.huffman;

import java.io.OutputStream;
import java.nio.BufferOverflowException;
import java.nio.ByteBuffer;


public class ByteBufferOutputStream extends OutputStream {

    private ByteBuffer wrappedBuffer;
    private int writtenBytes;

    public ByteBufferOutputStream(final ByteBuffer wrappedBuffer) {

        this.wrappedBuffer = wrappedBuffer;
        writtenBytes = 0;
    }

    @Override
    public void write(final int bty) {
        wrappedBuffer.put((byte) bty);
        writtenBytes++;
    }

    @Override
    public void write(final byte[] bytes) {
        wrappedBuffer.put(bytes);
        writtenBytes += bytes.length;
    }

    public int getWrittenBytes() {
        return writtenBytes;
    }
}