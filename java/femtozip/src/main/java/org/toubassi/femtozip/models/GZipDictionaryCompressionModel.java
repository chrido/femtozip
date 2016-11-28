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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.zip.DataFormatException;
import java.util.zip.Deflater;
import java.util.zip.Inflater;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.buffer.Unpooled;
import org.toubassi.femtozip.CompressionModel;
import org.toubassi.femtozip.DocumentList;

public class GZipDictionaryCompressionModel extends CompressionModel {
    private byte[] gzipSizedDictionary;

    public GZipDictionaryCompressionModel(PooledByteBufAllocator pbba) {
        super(pbba);
    }

    public GZipDictionaryCompressionModel() {
        super();
    }

    public void setDictionary(ByteBuf dictionary) {
        super.setDictionary(dictionary);
        //byte[] dictArr = dictionary.array(); //TODO: migrate to bytebuf
        if (dictionary.readableBytes() > (1 << 15) - 1) {
            gzipSizedDictionary = new byte[dictionary.readableBytes() - ((1 << 15) - 1)];
            dictionary.getBytes(dictionary.readableBytes() - ((1 << 15) - 1), gzipSizedDictionary);
            //gzipSizedDictionary = Arrays.copyOfRange(dictArr, dictArr.length - ((1 << 15) - 1), dictArr.length);
        }
        else {
            gzipSizedDictionary = new byte[dictionary.readableBytes()];
            dictionary.getBytes(0, gzipSizedDictionary);
        }
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
    
    public void build(DocumentList documents) {
    }

    public void compress(ByteBuf data, OutputStream out) throws IOException {
        compress(out, dictionary, data); 
    }

    protected void compress(OutputStream out, ByteBuf dictionary, ByteBuf input) throws IOException {
        Deflater compressor = new Deflater();

        try {
            compressor.setLevel(Deflater.BEST_COMPRESSION);
            if (dictionary != null) {
                compressor.setDictionary(gzipSizedDictionary);
            }

            // Give the compressor the data to compress
            compressor.setInput(input.array());
            compressor.finish();

            // Compress the data
            byte[] buf = new byte[1024];
            while (!compressor.finished()) {
                int count = compressor.deflate(buf);
                out.write(buf, 0, count);
            }

        } finally {
            compressor.end();
        }
    }
    
    public ByteBuf decompress(ByteBuf compressedData) {
        try {
            byte[] asArray = compressedData.array(); //TODO: migrate to ByteBuf
            Inflater decompresser = new Inflater();
            decompresser.setInput(asArray, 0, compressedData.readableBytes());
            byte[] result = new byte[1024];
            ByteArrayOutputStream bytesOut = new ByteArrayOutputStream(2 * asArray.length);
            while (!decompresser.finished()) {
                int resultLength = decompresser.inflate(result);
                if (resultLength == 0 && decompresser.needsDictionary()) {
                    decompresser.setDictionary(gzipSizedDictionary);
                }
                if (resultLength > 0) {
                    bytesOut.write(result, 0, resultLength);
                }
            }
            decompresser.end();
            return Unpooled.wrappedBuffer(bytesOut.toByteArray());
        }
        catch (DataFormatException e) {
            throw new RuntimeException(e);
        }
    }
}
