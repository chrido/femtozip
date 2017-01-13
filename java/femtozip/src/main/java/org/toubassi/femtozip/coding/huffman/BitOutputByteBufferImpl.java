package org.toubassi.femtozip.coding.huffman;

import java.nio.ByteBuffer;

import java.io.IOException;

public class BitOutputByteBufferImpl implements BitOutput {
    private final ByteBuffer out;
    private int buffer;
    private int count;
    private int writtenBytes;

    public BitOutputByteBufferImpl(ByteBuffer out) {
        this.out = out;
        writtenBytes = 0;
    }

    @Override
    public void writeBit(int bit) throws IOException {
        if (bit > 0) {
            buffer |= (1 << count);
        }
        count++;
        if (count == 8) {
            out.put((byte) buffer);
            writtenBytes++;
            buffer = 0;
            count = 0;
        }
    }

    @Override
    public void flush() throws IOException {
        if (count > 0) {
            out.put((byte)buffer);
            writtenBytes++;
            buffer = 0;
            count = 0;
        }
    }

    @Override
    public int getWrittenBytes() {
        return writtenBytes;
    }
}
