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
import org.toubassi.femtozip.coding.huffman.ByteBufferOutputStream;
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

    public ByteBuffer compressDeprecated(ByteBuffer data) {
        ByteBuffer compressed = ByteBuffer.allocate((int) (data.remaining() * 0.5));
        ByteBufferOutputStream bbos = new ByteBufferOutputStream(compressed, true);
        try {
            compressDeprecated(data, bbos);
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException("IOException", e);
        }
        ByteBuffer bb = bbos.toByteBuffer();

        return bb;
    }

    public void compressDeprecated(ByteBuffer data, OutputStream out) throws IOException {
        this.subStringPacker.pack(data, substringPackerConsumer, new PrintWriter(out));
    }

    public ByteBuffer decompressDeprecated(ByteBuffer compressedData) {
        SubstringUnpacker unpacker = new SubstringUnpacker(dictionary);

        String source = getString(compressedData);
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
        return unpacker.getUnpackedBytes();
    }

    @Override
    public int compress(ByteBuffer decompressedIn, ByteBuffer compressedOut) {
        return 0;
    }

    @Override
    public int compress(ByteBuffer decompressedIn, OutputStream compressedOut) throws IOException {
        return 0;
    }

    @Override
    public int decompress(ByteBuffer compressedIn, ByteBuffer decompressedOut) {
        return 0;
    }

    @Override
    public int decompress(InputStream compressedIn, ByteBuffer decompressedOut) throws IOException {
        return 0;
    }

    @Override
    public int setDictionary(ByteBuffer dictionary) {
        return 0;
    }

    @Override
    public void load(DataInputStream in) throws IOException {

    }

    @Override
    public void save(DataOutputStream out) throws IOException {

    }
}
