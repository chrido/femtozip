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
import org.toubassi.femtozip.DocumentList;

/**
 * NativeCompressionModel provides an interface to the native implementation
 * of FemtoZip, within the CompressionModelBase abstraction.  Some things to note.
 * To use this implementation, you must have built the native shared library.
 * See https://github.com/gtoubassi/femtozip/wiki/How-to-build.
 * 
 * The major difference between native and pure java implementations is that
 * with the native implementation, you call build, and load(String) directly
 * on an instance of this class, vs the buildOptimalModel and loadModel
 * statics on CompressionModelBase.
 * 
 * For a simple JNI example, see the org.toubassi.femtozip.models.NativeCompressionModelTest
 * JUnit test case in the source distribution of FemtoZip at
 * http://github.com/gtoubassi/femtozip
 */
@Deprecated
public class NativeCompressionModel implements CompressionModel {
    
    private static boolean nativeLibraryLoaded;

    protected long nativeModel;
    
    public NativeCompressionModel() {
        ensureNativeLibraryLoaded();
    }

    private void ensureNativeLibraryLoaded() {
        if (!nativeLibraryLoaded) {
            System.loadLibrary("jnifzip");
            nativeLibraryLoaded = true;
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

    public void load(DataInputStream in) throws IOException {
        throw new UnsupportedOperationException();
    }

    public void save(DataOutputStream out) throws IOException {
        throw new UnsupportedOperationException();
    }    

    public void compressDeprecated(ByteBuffer data, OutputStream out) throws IOException {
        // XXX Performance.  Lots of allocations.  Lots of copying.  Use a thread local?  Change this api?
        byte[] buf = new byte[data.remaining() * 2];
        byte[] dataBuf = new byte[data.remaining()];
        data.get(dataBuf);
        int length = compressba(dataBuf, buf);
        
        if (length < 0) {
            buf = new byte[length];
            length = compressba(data.array(), buf);
            if (length < 0) {
                throw new IllegalStateException();
            }
        }
        
        out.write(buf, 0, length);
    }

    public ByteBuffer decompressDeprecated(ByteBuffer compressedData) {
        // XXX Performance.  Lots of allocations.  Lots of copying.  Use a thread local?  Change this api?
        //TODO: Use Java Nio ByteBuffer instead of copying
        byte[] buf = new byte[compressedData.remaining() * 20];
        byte[] compressedDataB = new byte[compressedData.remaining()];
        compressedData.get(compressedDataB);
        int length = decompressba(compressedDataB, buf);
        
        if (length < 0) {
            buf = new byte[length];
            length = decompressba(compressedData.array(), buf);
            if (length < 0) {
                throw new IllegalStateException();
            }
        }
        if (buf.length != length) {
            byte[] newbuf = new byte[length];
            System.arraycopy(buf, 0, newbuf, 0, length);
            buf = newbuf;
        }
        return ByteBuffer.wrap(buf);
    }

    public void build(DocumentList documents) throws IOException {
        buildba(documents);
    }

    
    public native void load(String path) throws IOException;
    
    public native void save(String path) throws IOException;
    
    public native void buildba(DocumentList documents) throws IOException;

    public native int compressba(byte[] data, byte[] output);
    
    public native int decompressba(byte[] compressedData, byte[] decompressedData);

    @Override
    protected void finalize() {
        free();
    }

    protected native synchronized void free();
}
