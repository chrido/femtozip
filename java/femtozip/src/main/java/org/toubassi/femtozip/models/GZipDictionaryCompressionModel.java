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
import java.util.zip.DataFormatException;
import java.util.zip.Deflater;
import java.util.zip.Inflater;

import java.nio.ByteBuffer;


import org.toubassi.femtozip.CompressionModel;
import org.toubassi.femtozip.DocumentList;
import org.toubassi.femtozip.util.StreamUtil;

public class GZipDictionaryCompressionModel implements CompressionModel {
    private byte[] dictionary;
    private static int GZIPMAXSIZE = (1 << 15) - 1;

    public GZipDictionaryCompressionModel(ByteBuffer dictionary) {
        this.dictionary = new byte[dictionary.remaining()];
        dictionary.get(this.dictionary);
    }

    public GZipDictionaryCompressionModel() {
        dictionary = new byte[0];
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

    public void compressDeprecated(ByteBuffer data, OutputStream out) throws IOException {
        compress(out, data);
    }

    protected void compress(OutputStream out, ByteBuffer input) throws IOException {
        Deflater compressor = new Deflater();

        try {
            compressor.setLevel(Deflater.BEST_COMPRESSION);
            if (dictionary != null) {
                compressor.setDictionary(this.dictionary);
            }

            // Give the compressor the data to compressDeprecated
            byte[] inputB = new byte[input.remaining()];
            input.get(inputB); //TODO

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

    public ByteBuffer decompressDeprecated(ByteBuffer compressedData) {
        try {
            byte[] asArray = new byte[compressedData.remaining()];
            compressedData.get(asArray);

            Inflater decompresser = new Inflater();
            decompresser.setInput(asArray, 0, asArray.length);
            byte[] result = new byte[1024];
            ByteArrayOutputStream bytesOut = new ByteArrayOutputStream(2 * asArray.length);
            while (!decompresser.finished()) {
                int resultLength = decompresser.inflate(result);
                if (resultLength == 0 && decompresser.needsDictionary()) {
                    decompresser.setDictionary(dictionary);
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

    @Override
    public int setDictionary(ByteBuffer dictionary) {
        int i = 0;
        if (dictionary.remaining() > GZIPMAXSIZE) {
            this.dictionary = new byte[GZIPMAXSIZE];
            dictionary.position(dictionary.remaining() - GZIPMAXSIZE);
            while (dictionary.hasRemaining()) {
                this.dictionary[i] = dictionary.get();
                i++;
            }
        }
        else {
            this.dictionary = new byte[dictionary.remaining()];
            while (dictionary.hasRemaining()) {
                this.dictionary[i] = dictionary.get();
                i++;
            }
        }

        return i;
    }

    @Override
    public void load(DataInputStream in) throws IOException {
        if(in.readInt() == 0) { // file format version, currently 0.

            int dictionaryLength = in.readInt();
            if (dictionaryLength == -1) {
                setDictionary(null);
            } else {
                dictionary = new byte[dictionaryLength];
                int totalRead = StreamUtil.readBytes(in, dictionary, dictionaryLength);
                if (totalRead != dictionaryLength) {
                    throw new IOException("Bad model in stream.  Could not read dictionary of length " + dictionaryLength);
                }
            }
        }
    }

    @Override
    public void save(DataOutputStream out) throws IOException {
        out.writeInt(0); // Poor mans file format version
        if (dictionary == null) {
            out.writeInt(-1);
        }
        else {
            out.writeInt(dictionary.length);
            out.write(dictionary);
        }
    }
}
