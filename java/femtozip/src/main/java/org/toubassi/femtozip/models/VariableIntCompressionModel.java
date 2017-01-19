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
        int initial = compressedOut.position();

        try(ByteBufferOutputStream bbos = new ByteBufferOutputStream(compressedOut)){
            int length = compress(decompressedIn, bbos);
            compressedOut.flip();
            compressedOut.position(initial);
            return length;
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException("IOException", e);
        }
    }

    @Override
    public int compress(ByteBuffer decompressedIn, OutputStream compressedOut) throws IOException {
        if (decompressedIn.remaining() == 0) {
            return 0;
        }

        // If its too big to hold an int, or it has a leading 0, we can't do it (leading zeros will get lost in the encoding).
        if (decompressedIn.remaining() > 10 || decompressedIn.get(0) == '0') {
            return compressAsNonInt(decompressedIn, compressedOut);
        }

        for (int i = 0, count = decompressedIn.remaining(); i < count; i++) {
            if (decompressedIn.get(i) < '0' || decompressedIn.get(i) > '9') {
                return compressAsNonInt(decompressedIn, compressedOut);
            }
        }

        long l = Long.parseLong(getString(decompressedIn));
        int i = (int)l;
        if (i != l) {
            return compressAsNonInt(decompressedIn, compressedOut);
        }else {
            int size = 0;
            while ((i & ~0x7F) != 0) {
                compressedOut.write((byte)((i & 0x7f) | 0x80));
                size ++;
                i >>>= 7;
            }
            compressedOut.write((byte) i);
            size++;

            return size;
        }
    }

    @Override
    public int decompress(ByteBuffer compressedIn, ByteBuffer decompressedOut) {

        if (compressedIn.remaining() == 0) {
            return 0;
        }
        if (compressedIn.remaining() > 5) {
            return decompressAsNonInt(compressedIn, decompressedOut);
        }

        byte b = compressedIn.get();
        int i = b & 0x7F;
        for (int shift = 7; (b & 0x80) != 0; shift += 7) {
            b = compressedIn.get();
            i |= (b & 0x7F) << shift;
        }

        byte[] bytes = Integer.toString(i).getBytes(Charset.forName("UTF-8"));
        decompressedOut.put(bytes);
        decompressedOut.limit(bytes.length);
        decompressedOut.rewind();

        return bytes.length;
    }

    @Override
    public int decompress(InputStream compressedIn, ByteBuffer decompressedOut) throws IOException {

        byte[] header = new byte[6];
        int alreadRead = compressedIn.read(header);
        if(alreadRead == 0 || alreadRead == -1)
            return 0;

        if (alreadRead > 5) {
            return decompressAsNonIntStream(header, compressedIn, decompressedOut);
        }
        else {
            int j = 0;
            byte b = header[j]; j++;

            int i = b & 0x7F;
            for (int shift = 7; (b & 0x80) != 0; shift += 7) {
                b = (byte) header[j];
                j++;
                i |= (b & 0x7F) << shift;
            }

            byte[] bytes = Integer.toString(i).getBytes(Charset.forName("UTF-8"));
            int initialPosition = decompressedOut.position();
            decompressedOut.put(bytes);
            decompressedOut.flip();
            decompressedOut.position(initialPosition);

            return decompressedOut.remaining();
        }
    }

    public void save(DataOutputStream out) throws IOException {
        out.writeUTF(getClass().getName());
    }
    
    private static byte[] padding = new byte[6];
    
    private int compressAsNonInt(ByteBuffer data, OutputStream out) throws IOException {
        int size = 0;
        out.write(padding);
        size += padding.length;
        while (data.hasRemaining()){ //TODO: remove loop
            out.write(data.get());
            size++;
        }
        return size;
    }

    private int decompressAsNonIntStream(byte[] header, InputStream compressedIn, ByteBuffer compressed) throws IOException {

        int initialPosition = compressed.position();
        byte b;
        while ((b = (byte) compressedIn.read()) != -1) {
            compressed.put(b);
        }
        compressed.flip();
        compressed.position(initialPosition);
        return compressed.remaining();
    }
    
    private int decompressAsNonInt(ByteBuffer compressedData, ByteBuffer compressed) {
        //ByteBuffer slice = compressedData.slice();
        byte[] toreturn = new byte[compressedData.remaining() - 6];
        compressedData.position(6);
        compressedData.get(toreturn);

        compressed.put(toreturn);
        compressed.limit(toreturn.length);
        compressed.rewind();
        return toreturn.length;
    }
}
