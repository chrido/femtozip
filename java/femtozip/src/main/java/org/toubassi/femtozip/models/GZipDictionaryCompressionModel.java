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
import java.util.zip.DataFormatException;
import java.util.zip.Deflater;
import java.util.zip.Inflater;

import java.nio.ByteBuffer;


import org.toubassi.femtozip.CompressionModel;
import org.toubassi.femtozip.DocumentList;

public class GZipDictionaryCompressionModel extends CompressionModel {
    private byte[] gzipSizedDictionary;
    private static int GZIPMAXSIZE = (1 << 15) - 1;

    public GZipDictionaryCompressionModel() {
        super();
    }

    public void setDictionary(ByteBuffer dictionary) {
        super.setDictionary(dictionary);
        if (dictionary.remaining() > GZIPMAXSIZE) {
            gzipSizedDictionary = new byte[GZIPMAXSIZE];
            dictionary.position(dictionary.remaining() - GZIPMAXSIZE);
            int i = 0;
            while (dictionary.hasRemaining()) {
                gzipSizedDictionary[i] = dictionary.get();
            }
        }
        else {
            gzipSizedDictionary = new byte[dictionary.remaining()];
            int i = 0;
            while (dictionary.hasRemaining()) {
                gzipSizedDictionary[i] = dictionary.get();
            }
        }
        dictionary.position(0);
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

    public void compress(ByteBuffer data, OutputStream out) throws IOException {
        compress(out, dictionary, data); 
    }

    protected void compress(OutputStream out, ByteBuffer dictionary, ByteBuffer input) throws IOException {
        Deflater compressor = new Deflater();

        try {
            compressor.setLevel(Deflater.BEST_COMPRESSION);
            if (dictionary != null) {
                compressor.setDictionary(gzipSizedDictionary);
            }

            // Give the compressor the data to compress
            byte[] inputB = new byte[input.remaining()];
            //input.readBytes(inputB); //TODO

            compressor.setInput(inputB);
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

    public ByteBuffer decompress(ByteBuffer compressedData) {
        try {
            byte[] asArray = new byte[compressedData.remaining()];
            //compressedData.readBytes(asArray); //TODO

            Inflater decompresser = new Inflater();
            decompresser.setInput(asArray, 0, asArray.length);
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
            return ByteBuffer.wrap(bytesOut.toByteArray());
        }
        catch (DataFormatException e) {
            throw new RuntimeException(e);
        }
    }
}
