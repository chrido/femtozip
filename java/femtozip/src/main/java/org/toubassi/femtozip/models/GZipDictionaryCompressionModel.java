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
import java.util.zip.InflaterInputStream;


import org.toubassi.femtozip.CompressionModel;
import org.toubassi.femtozip.DocumentList;
import org.toubassi.femtozip.coding.huffman.ByteBufferOutputStream;
import org.toubassi.femtozip.util.StreamUtil;

public class GZipDictionaryCompressionModel implements CompressionModel {
    private byte[] dictionary;
    private static int GZIPMAXSIZE = (1 << 15) - 1;
    private static int INTERMEDIATE_BUFFER_SIZE = 1024;

    public GZipDictionaryCompressionModel(ByteBuffer dictionary) {
        initDictionary(dictionary);
    }

    private int initDictionary(ByteBuffer dictionary) {
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
    public int compress(ByteBuffer decompressedIn, ByteBuffer compressedOut) {
        int initialPosition = compressedOut.position();
        try (ByteBufferOutputStream byteBufferOutputStream = new ByteBufferOutputStream(compressedOut);)
        {
            int length = compress(decompressedIn, byteBufferOutputStream);
            compressedOut.flip();
            compressedOut.position(initialPosition);
            return length;
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException("IOException with ByteBufferOutputStream should never happen");
        }
    }

    @Override
    public int compress(ByteBuffer decompressedIn, OutputStream compressedOut) throws IOException {
        if(decompressedIn.remaining() <= 0)
            return 0;

        int size = 0;

        Deflater compressor = new Deflater(); //TODO: make field and wrap with locks

        try {
            compressor.setLevel(Deflater.BEST_COMPRESSION);
            if (dictionary != null) {
                compressor.setDictionary(this.dictionary);
            }

            // Give the compressor the data to compressDeprecated
            byte[] inputB = new byte[decompressedIn.remaining()];
            decompressedIn.get(inputB); //TODO, make loop giving junks

            compressor.setInput(inputB);
            compressor.finish();

            // Compress the data
            byte[] buf = new byte[INTERMEDIATE_BUFFER_SIZE];
            while (!compressor.finished()) {
                int count = compressor.deflate(buf);
                size += count;
                compressedOut.write(buf, 0, count);
            }
        } finally {
            compressor.end();
        }

        return size;
    }

    @Override
    public int decompress(ByteBuffer compressedIn, ByteBuffer decompressedOut) {
        if(compressedIn.remaining() <= 0)
            return 0;

        int initialPosition = decompressedOut.position();
        Inflater decompresser = new Inflater();

        try {
            int decompressedLength = 0;

            byte[] asArray = new byte[compressedIn.remaining()];
            compressedIn.get(asArray);

            decompresser.setInput(asArray, 0, asArray.length);
            decompressedLength = decompressInteral(decompressedOut, decompresser, decompressedLength);

            decompressedOut.limit(initialPosition + decompressedLength);
            decompressedOut.position(initialPosition);

            return decompressedLength;
        }
        catch (DataFormatException e) {
            throw new RuntimeException(e);
        }
        finally {
            decompresser.end();
        }
    }

    public static long copy(final InputStream input, final OutputStream output) throws IOException {
        byte[] buffer = new byte[INTERMEDIATE_BUFFER_SIZE];
        long count = 0;
        int n;
        while (-1 != (n = input.read(buffer))) {
            output.write(buffer, 0, n);
            count += n;
        }
        return count;
    }

    @Override
    public int decompress(InputStream compressedIn, ByteBuffer decompressedOut) throws IOException {
        int initialPosition = decompressedOut.position();

        Inflater decompresser = new Inflater(); //TODO: create field
        try {
            int decompressedLength = 0;

            ByteArrayOutputStream inputData = new ByteArrayOutputStream(); //TODO: set a maxbuffersize or something to not run out of memory
            copy(compressedIn, inputData);

            byte[] asArray = inputData.toByteArray();
            if(asArray.length == 0)
                return 0;

            decompresser.setInput(asArray, 0, asArray.length);
            decompressedLength = decompressInteral(decompressedOut, decompresser, decompressedLength);

            decompressedOut.limit(initialPosition + decompressedLength);
            decompressedOut.position(initialPosition);

            return decompressedLength;

        }
        catch (DataFormatException e) {
            throw new RuntimeException(e);
        }
        finally {
            decompresser.end();
        }
    }

    private int decompressInteral(ByteBuffer decompressedOut, Inflater decompresser, int decompressedLength) throws DataFormatException {
        byte[] result = new byte[INTERMEDIATE_BUFFER_SIZE];
        while (!decompresser.finished()) {
            int resultLength = decompresser.inflate(result);
            decompressedLength += resultLength;

            if (resultLength == 0 && decompresser.needsDictionary()) {
                decompresser.setDictionary(dictionary);
            }
            if (resultLength > 0) {
                decompressedOut.put(result, 0, resultLength);
            }
        }
        return decompressedLength;
    }


    @Override
    public void save(DataOutputStream out) throws IOException {
        out.writeUTF(getClass().getName());
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
