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

import org.toubassi.femtozip.CompressionModel;

import java.io.*;
import java.nio.ByteBuffer;

public class GZipCompressionModel implements CompressionModel {

    GZipDictionaryCompressionModel gZipDictionaryCompressionModel;

    public GZipCompressionModel() {
         gZipDictionaryCompressionModel = new GZipDictionaryCompressionModel(ByteBuffer.allocate(0));
    }

    @Override
    public int compress(ByteBuffer decompressedIn, ByteBuffer compressedOut) {
        return this.gZipDictionaryCompressionModel.compress(decompressedIn, compressedOut);
    }

    @Override
    public int compress(ByteBuffer decompressedIn, OutputStream compressedOut) throws IOException {
        return this.gZipDictionaryCompressionModel.compress(decompressedIn, compressedOut);
    }

    @Override
    public int decompress(ByteBuffer compressedIn, ByteBuffer decompressedOut) {
        return this.gZipDictionaryCompressionModel.decompress(compressedIn, decompressedOut);
    }

    @Override
    public int decompress(InputStream compressedIn, ByteBuffer decompressedOut) throws IOException {
        return this.gZipDictionaryCompressionModel.decompress(compressedIn, decompressedOut);
    }

    @Override
    public void save(DataOutputStream out) throws IOException {
        // Nothing to save, we put the classname to know which CompressionModel to restore
        out.writeUTF(getClass().getName());
    }
}
