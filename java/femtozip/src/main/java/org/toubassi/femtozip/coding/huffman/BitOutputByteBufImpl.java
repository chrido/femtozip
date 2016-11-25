package org.toubassi.femtozip.coding.huffman;

import io.netty.buffer.ByteBuf;

import java.io.IOException;

public class BitOutputByteBufImpl implements BitOutput {
    private final ByteBuf out;
    private int buffer;
    private int count;

    public BitOutputByteBufImpl(ByteBuf out) {
        this.out = out;
    }

    @Override
    public void writeBit(int bit) throws IOException {
        if (bit > 0) {
            buffer |= (1 << count);
        }
        count++;
        if (count == 8) {
            out.writeByte(buffer);
            buffer = 0;
            count = 0;
        }
    }

    @Override
    public void flush() throws IOException {
        if (count > 0) {
            out.writeByte(buffer);
            buffer = 0;
            count = 0;
        }

    }
}
