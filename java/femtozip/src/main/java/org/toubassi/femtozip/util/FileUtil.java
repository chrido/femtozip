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
package org.toubassi.femtozip.util;

import java.nio.ByteBuffer;


import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Arrays;

public class FileUtil {
    
    public static long computeSize(File root) {
        File[] files = root.listFiles();
        
        long size = 0;
        for (File subFile : files) {
            if (!subFile.getPath().endsWith(".fzmodel")) {
                size += subFile.length();
                if (subFile.isDirectory()) {
                    size += computeSize(subFile);
                }
            }
        }
        
        return size;
    }
    
    
    public static boolean recursiveDelete(File file) {
        boolean status = true;
        
        if (file.isDirectory()) {
            File[] files = file.listFiles();
            for (File f : files) {
                if (!f.getName().equals(".") && !f.getName().equals("..")) {
                    status = status && recursiveDelete(f);
                }
            }
        }
        return status && file.delete();
    }
    
    public static ByteBuffer readFile(File file) throws IOException {
        FileInputStream in = new FileInputStream(file);
        // No need to buffer as StreamUtil.readAll will read in big chunks
        return ByteBuffer.wrap(StreamUtil.readAll(in));
    }

    public static byte[] readFile(String path) throws IOException {
        FileInputStream in = new FileInputStream(path);
        // No need to buffer as StreamUtil.readAll will read in big chunks
        return StreamUtil.readAll(in);
    }

    public static byte[] toArrayResetReader(ByteBuffer buf) {

        byte[] arr = new byte[buf.remaining()];
        int i = 0;
        while (buf.hasRemaining()) {
            arr[i] = buf.get();
            i++;
        }

        return arr;
    }

    public static String getString(ByteBuffer buffer) {
        return getString(buffer, false);
    }

    public static String getString(ByteBuffer buffer, Boolean setPosition) {
        byte[] bytes;
        if(buffer.hasArray()) {
            bytes = Arrays.copyOfRange(buffer.array(), buffer.position(), buffer.limit());
        } else {
            bytes = new byte[buffer.limit() - buffer.position()];
            buffer.get(bytes);
        }
        if(setPosition)
            buffer.position(buffer.limit());
        return new String(bytes, Charset.forName("UTF-8"));
    }

    public static byte[] getBytes(ByteBuffer buffer) {
        byte[] bytes;
        if(buffer.hasArray()) {
            bytes = buffer.array();
        } else {
            bytes = new byte[buffer.remaining()];
            buffer.get(bytes);
        }
        return bytes;
    }

}
