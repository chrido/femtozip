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

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import java.nio.ByteBuffer;


import org.toubassi.femtozip.CompressionModel;
import org.toubassi.femtozip.DocumentList;
import org.toubassi.femtozip.coding.huffman.*;

public class PureHuffmanCompressionModel extends CompressionModel {

    private FrequencyHuffmanModel codeModel;

    public PureHuffmanCompressionModel() {
        super();
    }

    public void load(DataInputStream in) throws IOException {
        codeModel = new FrequencyHuffmanModel(in);
    }

    public void save(DataOutputStream out) throws IOException {
        codeModel.save(out);
    }
    
    public void build(DocumentList documents) {
        try {
            int[] histogram = new int[256 + 1]; // +1 for EOF
            
            for (int i = 0, count = documents.size(); i < count; i++) {
                ByteBuffer bytes = documents.getBB(i);
                for (int j = 0, jcount = bytes.remaining(); j < jcount; j++) { //TODO: just read through
                    histogram[bytes.get(j) & 0xff]++;
                }
                histogram[histogram.length - 1]++;
            }

            codeModel = new FrequencyHuffmanModel(histogram, false);
        }
        catch (IOException e) {
            throw new RuntimeException(e);
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
    
    public void compress(ByteBuffer data, OutputStream out) throws IOException {
        HuffmanEncoder encoder = new HuffmanEncoder(codeModel, new BitOutputOutputStreamImpl(out));
        for (int i = 0, count = data.remaining(); i < count; i++) {
            encoder.encodeSymbol(((int)data.get(i)) & 0xff);
        }
        encoder.close();
        out.close();
    }
    
    public ByteBuffer decompress(ByteBuffer compressedData) {
        try {
            ByteBufferInputStream bytesIn = new ByteBufferInputStream(compressedData);
            HuffmanDecoder decoder = new HuffmanDecoder(codeModel, bytesIn);
            ByteBuffer bytesOut = ByteBuffer.allocate(compressedData.remaining() * 4); //TODO: Pooling
            
            int nextSymbol;
            while ((nextSymbol = decoder.decodeSymbol()) != -1) {
                bytesOut.put((byte)nextSymbol);
            }
            return bytesOut;
        }
        catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

}
