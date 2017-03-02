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

import java.nio.ByteBuffer;

import java.util.Arrays;

public class PrefixHash {
    
    public static final int PrefixLength = 4;
    
    final private ByteBuffer buffer;
    final int[] hash;
    final int[] heap;
    
    public PrefixHash(ByteBuffer buf, boolean addToHash) {
        buffer = buf;
        hash = new int[(int)(1.75 * buf.remaining())];
        Arrays.fill(hash, -1);
        heap = new int[buf.remaining()];
        Arrays.fill(heap, -1);
        if (addToHash) {
            for (int i = 0, count = buf.remaining() - PrefixLength; i < count; i++) {
                put(i);
            }
        }
    }
    
    private int hashIndex(ByteBuffer buf, int i) {
        int code = (buf.get(i) & 0xff) | ((buf.get(i + 1) & 0xff) << 8) | ((buf.get(i + 2) & 0xff) << 16) | ((buf.get(i + 3) & 0xff) << 24);
        return (code & 0x7fffff) % hash.length;
    }

    
    public void put(int index) {
        int hashIndex = hashIndex(buffer, index);
        heap[index] = hash[hashIndex];
        hash[hashIndex] = index;
    }

    public final long getBestMatch(final int index, final ByteBuffer targetBuf) {
        int bestMatchIndex = 0;
        int bestMatchLength = 0;

        final int bufLen = this.buffer.remaining();

        if (bufLen == 0) {
            return (((long) 0) << 32) | (0 & 0xffffffffL);
        }

        final int targetBufLen = targetBuf.remaining();

        final int maxLimit = Math.min(255, targetBufLen - index);

        int targetHashIndex = hashIndex(targetBuf, index);
        int candidateIndex = hash[targetHashIndex];
        while (candidateIndex >= 0) {
            int distance;
            if (targetBuf != this.buffer) {
                distance = index + bufLen - candidateIndex;
            } else {
                distance = index - candidateIndex;
            }
            if (distance > (2 << 15) - 1) {
                // Since we are iterating over nearest offsets first, once we pass 64k
                // we know the rest are over 64k too.
                break;
            }

            final int maxMatchJ = index + Math.min(maxLimit, bufLen - candidateIndex);
            int j, k;
            for (j = index, k = candidateIndex; j < maxMatchJ; j++, k++) {
                if (this.buffer.get(k) != targetBuf.get(j)) {
                    break;
                }
            }

            final int matchLength = j - index;
            if (matchLength > bestMatchLength) {
                bestMatchIndex = candidateIndex;
                bestMatchLength = matchLength;
            }
            candidateIndex = heap[candidateIndex];
        }

        return (((long) bestMatchIndex) << 32) | (bestMatchLength & 0xffffffffL);
    }
}
