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

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.nio.charset.Charset;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufOutputStream;
import io.netty.buffer.PooledByteBufAllocator;
import org.toubassi.femtozip.CompressionModel;
import org.toubassi.femtozip.DocumentList;
import org.toubassi.femtozip.substring.SubstringUnpacker;

public class VerboseStringCompressionModel extends CompressionModel {

    public VerboseStringCompressionModel() {
        super();
    }

    public VerboseStringCompressionModel(PooledByteBufAllocator arena) {
        super(arena);
    }

    public void build(DocumentList documents) throws IOException {
        buildDictionaryIfUnspecified(documents);
    }

    @Override
    public ByteBuf compress(ByteBuf data) {
        ByteBuf compressed = arena.buffer();
        OutputStream bbos = new ByteBufOutputStream(compressed);
        try {
            compress(data, bbos);
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException("IOException", e);
        }

        return compressed;
    }

    public void compress(ByteBuf data, OutputStream out) throws IOException {
        getSubstringPacker().pack(data, this, new PrintWriter(out));
    }

    public ByteBuf decompress(ByteBuf compressedData) {
        SubstringUnpacker unpacker = new SubstringUnpacker(dictionary, arena);

        String source = compressedData.toString(Charset.forName("UTF-8"));
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

    public void encodeLiteral(int aByte, Object context) {
        PrintWriter writer = (PrintWriter)context;
        writer.print((char)aByte);
    }

    public void encodeSubstring(int offset, int length, Object context) {
        PrintWriter writer = (PrintWriter)context;
        writer.print('<');
        writer.print(offset);
        writer.print(',');
        writer.print(length);
        writer.print('>');
    }
    
    public void endEncoding(Object context) {
        PrintWriter writer = (PrintWriter)context;
        writer.close();
    }
}
