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
package org.toubassi.femtozip.substring;

import org.toubassi.femtozip.coding.huffman.ByteBufferOutputStream;

import java.nio.Buffer;
import java.nio.BufferOverflowException;
import java.nio.ByteBuffer;


import java.io.ByteArrayOutputStream;

public class SubstringUnpacker implements SubstringPacker.Consumer {
    private ByteBuffer dictionary;
    private ByteOutput bytesOut = new ByteOutput();
    private byte[] unpackedBytes;

    public SubstringUnpacker(ByteBuffer dictionary) {
        this.dictionary = dictionary == null ? ByteBuffer.allocate(0) : dictionary;
    }
    
    public void encodeLiteral(int aByte, Object context) {
        bytesOut.write(aByte);
    }

    public ByteBuffer getUnpackedBytes() {

        if (unpackedBytes == null) {
            unpackedBytes = bytesOut.toByteArray();
            bytesOut = new ByteOutput();
        }
        return ByteBuffer.wrap(unpackedBytes);
    }

    public void encodeSubstring(int offset, int length, Object context) {
        int dictLength = dictionary.remaining();
        int currentIndex = bytesOut.size();

        if (currentIndex + offset < 0) {
            int startDict = currentIndex + offset + dictLength;
            int endDict = startDict + length;
            int end = 0;
            
            if (endDict > dictLength) {
                end = endDict - dictLength;
                endDict = dictLength;
            }
            for (int i = startDict; i < endDict; i++) {
                bytesOut.write(dictionary.get(i));
            }
            
            if (end > 0) {
                for (int i = 0; i < end; i++) {
                    bytesOut.write(bytesOut.get(i));
                }
            }
        }
        else {
            for (int i = currentIndex + offset, count = currentIndex + offset + length; i < count; i++) {
                bytesOut.write(bytesOut.get(i));
            }
        }
    }
    
    public void endEncoding(Object context) {
    }

    public int writeOut(ByteBuffer decompressedOut) {
        byte[] unpacked = bytesOut.toByteArray();
        decompressedOut.put(unpacked);

        return unpacked.length;
    }

    private static class ByteOutput extends ByteArrayOutputStream {
        public byte get(int i) {
            return buf[i];
        }
    }
}
