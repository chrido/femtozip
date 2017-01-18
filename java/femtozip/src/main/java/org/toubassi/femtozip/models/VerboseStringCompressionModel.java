/**
 *   Copyright 2011 Garrick Toubassi
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */
package org.toubassi.femtozip.models;

import java.io.*;

import java.nio.ByteBuffer;


import org.toubassi.femtozip.CompressionModel;
import org.toubassi.femtozip.DocumentList;
import org.toubassi.femtozip.coding.huffman.BitOutput;
import org.toubassi.femtozip.coding.huffman.BitOutputByteBufferImpl;
import org.toubassi.femtozip.coding.huffman.ByteBufferOutputStream;
import org.toubassi.femtozip.coding.huffman.CountingOutputStream;
import org.toubassi.femtozip.substring.SubstringPacker;
import org.toubassi.femtozip.substring.SubstringUnpacker;

import static org.toubassi.femtozip.util.FileUtil.getString;

public class VerboseStringCompressionModel implements CompressionModel {

    private final ByteBuffer dictionary;
    private SubstringPacker subStringPacker;
    private SubstringPackerConsumer substringPackerConsumer;

    public VerboseStringCompressionModel(ByteBuffer dictionary) {
        this.dictionary = dictionary;
        this.subStringPacker = new SubstringPacker(dictionary);
        substringPackerConsumer = new SubstringPackerConsumer();
    }

    public VerboseStringCompressionModel() {
        this.dictionary = ByteBuffer.allocate(0);
        this.subStringPacker = new SubstringPacker(this.dictionary);
        substringPackerConsumer = new SubstringPackerConsumer();
    }

    @Override
    public int compress(ByteBuffer decompressedIn, ByteBuffer compressedOut) {
        int initialPosition = compressedOut.position();

        try {
            try (ByteBufferOutputStream bbos = new ByteBufferOutputStream(compressedOut)) {
                compress(decompressedIn, bbos);
                int writtenBytes = bbos.getWrittenBytes();
                compressedOut.position(initialPosition);
                compressedOut.limit(initialPosition + writtenBytes);
                return writtenBytes;
            }
        } catch (IOException e) {
            e.printStackTrace();
            //This should never happen since we only wrap ByteBuffers
            throw new RuntimeException(e);
        }
    }

        @Override
    public int compress(ByteBuffer decompressedIn, OutputStream compressedOut) throws IOException {
        //CountingOutputStream cout = new CountingOutputStream(compressedOut);
        this.subStringPacker.pack(decompressedIn, substringPackerConsumer, new PrintWriter(compressedOut));

        return 0;//cout.getWrittenBytes();
    }

    @Override
    public int decompress(ByteBuffer compressedIn, ByteBuffer decompressedOut) {
        int beginPosition = decompressedOut.position();
        SubstringUnpacker unpacker = new SubstringUnpacker(dictionary, decompressedOut);

        String source = getString(compressedIn);
        for (int i = 0, count = source.length(); i < count; i++) {
            char ch = source.charAt(i);
            if (ch == '<') {
                int rightAngleIndex = source.indexOf('>', i);
                String substring = source.substring(i + 1, rightAngleIndex);
                String[] parts = substring.split(",");
                int offset = Integer.parseInt(parts[0]);
                int length = Integer.parseInt(parts[1]);

                unpacker.encodeSubstring(offset, length, null);
                // Skip past this in the outer loop
                i = rightAngleIndex;
            } else {
                unpacker.encodeLiteral((int) ch, null);
            }
        }
        unpacker.endEncoding(null);

        int written = decompressedOut.position() - beginPosition;
        decompressedOut.position(beginPosition);
        decompressedOut.limit(beginPosition + written);

        return written;
    }

    @Override
    public int decompress(InputStream compressedIn, ByteBuffer decompressedOut) throws IOException {
        return 0;
    }

    @Override
    public void load(DataInputStream in) throws IOException {

    }

    @Override
    public void save(DataOutputStream out) throws IOException {

    }
}
