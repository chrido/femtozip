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
package org.toubassi.femtozip;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;

import static org.toubassi.femtozip.util.FileUtil.toArrayResetReader;

public class ArrayDocumentList implements DocumentList {
    
    private ArrayList<ByteBuf> docs;
    
    public ArrayDocumentList(String... documents) {
        try {
            this.docs = new ArrayList<>(documents.length);
            for (String string : documents) {
                    docs.add(Unpooled.wrappedBuffer(string.getBytes("UTF-8")));
            }
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }

    public ArrayDocumentList(ArrayList<ByteBuf> documents) {
        this.docs = documents;
    }

    public ArrayDocumentList(ByteBuf singleDocument) {
        docs = new ArrayList<ByteBuf>(1);
        docs.add(singleDocument);
    }

    public int size() {
        return docs.size();
    }

    public ByteBuf getBB(int i) {
        return docs.get(i);
    }

    public byte[] get(int i) throws IOException {
        return toArrayResetReader(getBB(i));
    }
}
