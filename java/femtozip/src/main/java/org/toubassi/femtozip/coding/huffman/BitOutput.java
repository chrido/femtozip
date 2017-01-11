package org.toubassi.femtozip.coding.huffman;

import java.io.IOException;

public interface BitOutput {
    void writeBit(int bit) throws IOException;

    void flush() throws IOException;

    int getWrittenBytes();
}
