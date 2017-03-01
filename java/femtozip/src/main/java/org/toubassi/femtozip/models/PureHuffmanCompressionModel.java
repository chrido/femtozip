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
import org.toubassi.femtozip.coding.huffman.*;

public class PureHuffmanCompressionModel implements CompressionModel {

    private FrequencyHuffmanModel codeModel;

    public PureHuffmanCompressionModel(FrequencyHuffmanModel codeModel) {
        this.codeModel = codeModel;
    }

    @Override
    public int compress(ByteBuffer decompressedIn, ByteBuffer compressedOut) {
        if(decompressedIn.remaining() <= 0)
        {
            compressedOut.limit(compressedOut.position());
            return 0;
        }

        try {
            int initalPosition = compressedOut.position();

            BitOutputByteBufferImpl bobbi = new BitOutputByteBufferImpl(compressedOut);
            HuffmanEncoder encoder = new HuffmanEncoder(codeModel, bobbi);
            for (int i = 0, count = decompressedIn.remaining(); i < count; i++) {
                encoder.encodeSymbol(((int) decompressedIn.get()) & 0xff);
            }
            encoder.close();

            int written = compressedOut.position() - initalPosition;
            compressedOut.flip();
            compressedOut.position(initalPosition);
            return written;
        } catch (IOException e) {
            throw new RuntimeException("should never occure", e);
        }
    }

    @Override
    public int compress(ByteBuffer decompressedIn, OutputStream compressedOut) throws IOException {
        if(decompressedIn.remaining() <= 0) {
            return 0;
        }

        BitOutputOutputStreamImpl bitOutputOutputStream = new BitOutputOutputStreamImpl(compressedOut);
        HuffmanEncoder encoder = new HuffmanEncoder(codeModel, bitOutputOutputStream);
        for (int i = 0, count = decompressedIn.remaining(); i < count; i++) {
            encoder.encodeSymbol(((int) decompressedIn.get(i)) & 0xff);
        }
        encoder.close();

        return bitOutputOutputStream.getWrittenBytes();
    }

    @Override
    public int decompress(ByteBuffer compressedIn, ByteBuffer decompressedOut) {
        try {

            ByteBufferInputStream bytesIn = new ByteBufferInputStream(compressedIn);
            return decompress(bytesIn, decompressedOut);
        }
        catch (IOException e) {
            throw new RuntimeException("should never occure", e);
        }
    }

    @Override
    public int decompress(InputStream compressedIn, ByteBuffer decompressedOut) throws IOException {

        int initalPosition = decompressedOut.position();
        HuffmanDecoder decoder = new HuffmanDecoder(codeModel, compressedIn);

        int nextSymbol;
        while ((nextSymbol = decoder.decodeSymbol()) != -1) {
            decompressedOut.put((byte)nextSymbol);
        }

        int written = decompressedOut.position() - initalPosition;
        decompressedOut.limit(decompressedOut.position());
        decompressedOut.position(initalPosition);

        return written;
    }

    public void save(DataOutputStream out) throws IOException {
        out.writeUTF(getClass().getName());
        out.writeInt(0); //Version

        codeModel.save(out);
    }
}
