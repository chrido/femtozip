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

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.util.Arrays;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.buffer.Unpooled;
import org.toubassi.femtozip.CompressionModel;
import org.toubassi.femtozip.DocumentList;

public class VariableIntCompressionModel extends CompressionModel {

    public VariableIntCompressionModel(PooledByteBufAllocator pbba) {
        super(pbba);
    }

    public VariableIntCompressionModel() {
        super();
    }

    public void load(DataInputStream in) throws IOException {
    }

    public void save(DataOutputStream out) throws IOException {
    }
    
    public void build(DocumentList documents) {
    }
    
    
    public void encodeLiteral(int aByte, Object context) {
        throw new UnsupportedOperationException();
    }

    public void encodeSubstring(int offset, int length, Object context) {
        throw new UnsupportedOperationException();
    }

    public void endEncoding(Object context) {
        throw new UnsupportedOperationException();
    }
    
    public void compress(ByteBuf data, OutputStream out) throws IOException {
        if (data.readableBytes() == 0) {
            return;
        }
        // If its too big to hold an int, or it has a leading 0, we can't do it (leading zeros will get lost in the encoding).
        if (data.readableBytes() > 10 || data.getByte(0) == '0') {
            compressAsNonInt(data, out);
            return;
        }
        
        for (int i = 0, count = data.readableBytes(); i < count; i++) {
            if (data.getByte(i) < '0' || data.getByte(i) > '9') {
                compressAsNonInt(data, out);
                return;
            }
        }
        
        long l = Long.parseLong(data.toString(Charset.forName("UTF-8")));
        int i = (int)l;
        if (i != l) {
            compressAsNonInt(data, out);
            return;
        }
        while ((i & ~0x7F) != 0) {
            out.write((byte)((i & 0x7f) | 0x80));
            i >>>= 7;
       }
        out.write(i);
    }
    
    private static byte[] padding = new byte[6];
    
    private void compressAsNonInt(ByteBuf data, OutputStream out) throws IOException {
        out.write(padding);
        data.readBytes(out, data.readableBytes());
        data.resetReaderIndex();
    }
    
    private ByteBuf decompressAsNonInt(ByteBuf compressedData) {
        return compressedData.slice(6, compressedData.readableBytes()-6);
        //return Arrays.copyOfRange(compressedData, 6, compressedData.length);
    }
    
    public ByteBuf decompress(ByteBuf compressedData) {
        if (compressedData.readableBytes() == 0) {
            return compressedData;
        }
        if (compressedData.readableBytes() > 5) {
            return decompressAsNonInt(compressedData);
        }
        
        int index = 0;
        byte b = compressedData.getByte(index++);
        int i = b & 0x7F;
        for (int shift = 7; (b & 0x80) != 0; shift += 7) {
          b = compressedData.getByte(index++);
          i |= (b & 0x7F) << shift;
        }

        String s = Integer.toString(i);
        ByteBuf buf = arena.buffer();
        buf.writeBytes(s.getBytes(Charset.forName("UTF-8")));
        return buf;
    }
}
