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
import java.nio.charset.Charset;

import java.nio.ByteBuffer;


import org.toubassi.femtozip.CompressionModel;
import org.toubassi.femtozip.DocumentList;
import org.toubassi.femtozip.coding.huffman.ByteBufferOutputStream;

import static org.toubassi.femtozip.util.FileUtil.getString;

public class VariableIntCompressionModel implements CompressionModel {

    public VariableIntCompressionModel() {
        super();
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

    public void load(DataInputStream in) throws IOException {
    }

    public void save(DataOutputStream out) throws IOException {
    }
    
    public void build(DocumentList documents) {
    }
    

    public ByteBuffer compressDeprecated(ByteBuffer data) {
        ByteBuffer compressed = ByteBuffer.allocate((int) (data.remaining() *0.8)); //Estimate
        ByteBufferOutputStream bbos = new ByteBufferOutputStream(compressed, true);
        try {
            compressDeprecated(data, bbos);
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException("IOException", e);
        }

        return bbos.toByteBuffer();
    }

    public void compressDeprecated(ByteBuffer data, OutputStream out) throws IOException {
        if (data.remaining() == 0) {
            return;
        }
        // If its too big to hold an int, or it has a leading 0, we can't do it (leading zeros will get lost in the encoding).
        if (data.remaining() > 10 || data.get(0) == '0') {
            compressAsNonInt(data, out);
            return;
        }
        
        for (int i = 0, count = data.remaining(); i < count; i++) {
            if (data.get(i) < '0' || data.get(i) > '9') {
                compressAsNonInt(data, out);
                return;
            }
        }
        
        long l = Long.parseLong(getString(data));
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
    
    private void compressAsNonInt(ByteBuffer data, OutputStream out) throws IOException {
        out.write(padding);
        while (data.hasRemaining()){
            out.write(data.get());
        }
    }
    
    private ByteBuffer decompressAsNonInt(ByteBuffer compressedData) {
        //ByteBuffer slice = compressedData.slice();
        byte[] toreturn = new byte[compressedData.remaining() - 6];
        compressedData.position(6);
        compressedData.get(toreturn);
        return ByteBuffer.wrap(toreturn);
    }
    
    public ByteBuffer decompressDeprecated(ByteBuffer compressedData) {
        if (compressedData.remaining() == 0) {
            return compressedData;
        }
        if (compressedData.remaining() > 5) {
            return decompressAsNonInt(compressedData);
        }
        
        byte b = compressedData.get();
        int i = b & 0x7F;
        for (int shift = 7; (b & 0x80) != 0; shift += 7) {
          b = compressedData.get();
          i |= (b & 0x7F) << shift;
        }

        byte[] bytes = Integer.toString(i).getBytes(Charset.forName("UTF-8"));
        return ByteBuffer.wrap(bytes);
    }
}
