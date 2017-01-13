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

import java.io.OutputStream;
import java.nio.ByteBuffer;


import java.io.ByteArrayOutputStream;

public class SubstringUnpacker implements SubstringPacker.Consumer {
    private ByteBuffer dictionary;
    private final ByteBuffer bytesOut;

    public SubstringUnpacker(ByteBuffer dictionary, ByteBuffer bytesOut) {
        this.dictionary = dictionary == null ? ByteBuffer.allocate(0) : dictionary;
        this.bytesOut = bytesOut;
    }
    
    public void encodeLiteral(int aByte, Object context) {
        bytesOut.put((byte)aByte);
    }

    public void encodeSubstring(int offset, int length, Object context) {
        int dictLength = dictionary.remaining();
        int currentIndex = bytesOut.position();

        if (currentIndex + offset < 0) {
            int startDict = currentIndex + offset + dictLength;
            int endDict = startDict + length;
            int end = 0;
            
            if (endDict > dictLength) {
                end = endDict - dictLength;
                endDict = dictLength;
            }
            for (int i = startDict; i < endDict; i++) {
                bytesOut.put(dictionary.get(i));
            }
            
            if (end > 0) {
                for (int i = 0; i < end; i++) {
                    bytesOut.put(bytesOut.get(i));
                }
            }
        }
        else {
            for (int i = currentIndex + offset, count = currentIndex + offset + length; i < count; i++) {
                bytesOut.put(bytesOut.get(i));
            }
        }
    }
    
    public void endEncoding(Object context) {
        //Nothing
    }
}
