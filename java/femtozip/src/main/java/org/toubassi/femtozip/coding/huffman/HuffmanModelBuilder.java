package org.toubassi.femtozip.coding.huffman;

import org.toubassi.femtozip.DocumentList;
import org.toubassi.femtozip.models.femtozip.FemtoZipHuffmanModelBuilder;
import org.toubassi.femtozip.substring.SubstringPacker;

import java.io.IOException;
import java.nio.ByteBuffer;

public class HuffmanModelBuilder {
    private final DocumentList documents;
    private final ByteBuffer dictionary;

    public HuffmanModelBuilder(DocumentList documents, ByteBuffer dictionary) {
        this.documents = documents;
        this.dictionary = dictionary;
    }

    protected SubstringPacker.Consumer buildEncodingModel() {
        try {
            SubstringPacker modelBuildingPacker = new SubstringPacker(this.dictionary);
            SubstringPacker.Consumer modelBuilder = new FemtoZipHuffmanModelBuilder();
            for (int i = 0, count = documents.size(); i < count; i++) {
                modelBuildingPacker.pack(documents.getBB(i), modelBuilder, null);
            }

            return modelBuilder;
        }
        catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
