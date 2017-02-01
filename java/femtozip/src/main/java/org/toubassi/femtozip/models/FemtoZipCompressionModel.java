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
import java.nio.channels.Channels;
import java.nio.channels.WritableByteChannel;

import org.toubassi.femtozip.CompressionModel;
import org.toubassi.femtozip.coding.huffman.*;
import org.toubassi.femtozip.dictionary.DictionaryOptimizer;
import org.toubassi.femtozip.models.femtozip.FemtoZipHuffmanModel;
import org.toubassi.femtozip.substring.SubstringPacker;
import org.toubassi.femtozip.substring.SubstringUnpacker;

public class FemtoZipCompressionModel implements CompressionModel, SubstringPacker.Consumer {
    private SubstringPacker subStringPacker;
    private FemtoZipHuffmanModel codeModel;
    private ByteBuffer dictionary;

    public FemtoZipCompressionModel(FemtoZipHuffmanModel codeModel, ByteBuffer dictionary) {
        this.codeModel = codeModel;
        this.dictionary = dictionary;
        this.subStringPacker = new SubstringPacker(dictionary);
    }

    @Override
    public void save(DataOutputStream out) throws IOException {
        out.writeUTF(getClass().getName());
        out.writeInt(0); //Version

        out.writeInt(dictionary.remaining());

        WritableByteChannel channel = Channels.newChannel(out);
        channel.write(dictionary);

        dictionary.rewind();

        codeModel.save(out);
    }

    @Override
    public int compress(ByteBuffer decompressedIn, ByteBuffer compressedOut) {
        if(decompressedIn.remaining() <= 0)
            return 0;

        int initialPosition = compressedOut.position();
        BitOutput bitOutputByteBuffer = new BitOutputByteBufferImpl(compressedOut);
        try {
            int written = compress(decompressedIn, bitOutputByteBuffer);
            compressedOut.flip();
            compressedOut.position(initialPosition);
            return written;

        } catch (IOException e) {
            e.printStackTrace();
            //This should never happen since we only wrap ByteBuffers
            throw new RuntimeException(e);
        }
    }

    @Override
    public int compress(ByteBuffer decompressedIn, OutputStream compressedOut) throws IOException{
        BitOutputOutputStreamImpl bitOutputOutputStream = new BitOutputOutputStreamImpl(compressedOut);
        return compress(decompressedIn, bitOutputOutputStream);
    }

    private int compress(ByteBuffer decompressedIn, BitOutput compressedOut) throws IOException {
        if(decompressedIn.remaining() == 0)
            return 0;

        HuffmanEncoder huffmanEncoder = new HuffmanEncoder(codeModel.createModel(), compressedOut);
        this.subStringPacker.pack(decompressedIn, this, huffmanEncoder);
        compressedOut.flush();

        return compressedOut.getWrittenBytes();
    }

    @Override
    public int decompress(ByteBuffer compressedIn, ByteBuffer decompressedOut) {
        if(compressedIn.remaining() <= 0)
            return 0;

        try {
            ByteBufferInputStream bytesIn = new ByteBufferInputStream(compressedIn);
            decompress(bytesIn, decompressedOut);

            return decompressedOut.remaining();
        } catch (IOException e) {
            //with Bytebuffers this should never occure, this is why we throw a RuntimeException
            throw new RuntimeException(e);
        }
    }

    @Override
    public int decompress(InputStream compressedIn, ByteBuffer decompressedOut) throws IOException{

        int startPosition = decompressedOut.position();

        HuffmanDecoder decoder = new HuffmanDecoder(codeModel.createModel(), compressedIn);
        SubstringUnpacker unpacker = new SubstringUnpacker(dictionary, decompressedOut);

        int nextSymbol;
        while ((nextSymbol = decoder.decodeSymbol()) != -1) {
            if (nextSymbol > 255) {
                int length = nextSymbol - 256;
                int offset = decoder.decodeSymbol() | (decoder.decodeSymbol() << 4) | (decoder.decodeSymbol() << 8) | (decoder.decodeSymbol() << 12);
                offset = -offset;
                unpacker.encodeSubstring(offset, length, null);
            } else {
                unpacker.encodeLiteral(nextSymbol, null);
            }
        }
        unpacker.endEncoding(null);

        decompressedOut.flip();
        decompressedOut.position(startPosition);
        return decompressedOut.remaining();
    }

    @Deprecated
    public ByteBuffer compressDeprecated(ByteBuffer buf) {
        ByteBuffer compressed = ByteBuffer.allocate((int) (buf.remaining() * 2)); //Estimation is that the data is roughly half

        HuffmanEncoder huffmanEncoder = new HuffmanEncoder(codeModel.createModel(), new BitOutputByteBufferImpl(compressed));
        this.subStringPacker.pack(buf, this, huffmanEncoder);

        int numOfBytes = compressed.position();
        compressed.position(0);
        compressed.limit(numOfBytes);

        return compressed;
    }


    @Override
    public void encodeLiteral(int aByte, Object context) {
        try {
            HuffmanEncoder encoder = (HuffmanEncoder)context;
            encoder.encodeSymbol(aByte);
        }
        catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void encodeSubstring(int offset, int length, Object context) {
        try {
            HuffmanEncoder encoder = (HuffmanEncoder)context;
            if (length < 1 || length > 255) {
                throw new IllegalArgumentException("Length " + length + " out of range [1,255]");
            }
            encoder.encodeSymbol(256 + length);
            
            offset = -offset;
            if (offset < 1 || offset > (2<<15)-1) {
                throw new IllegalArgumentException("Offset " + offset + " out of range [1, 65535]");
            }
            encoder.encodeSymbol(offset & 0xf);
            encoder.encodeSymbol((offset >> 4) & 0xf);
            encoder.encodeSymbol((offset >> 8) & 0xf);
            encoder.encodeSymbol((offset >> 12) & 0xf);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void endEncoding(Object context) {
        try {
            HuffmanEncoder encoder = (HuffmanEncoder)context;
            encoder.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
