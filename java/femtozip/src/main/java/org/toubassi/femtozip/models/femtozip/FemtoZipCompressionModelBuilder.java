package org.toubassi.femtozip.models.femtozip;

import org.toubassi.femtozip.DocumentList;
import org.toubassi.femtozip.substring.SubstringPacker;

import java.io.IOException;
import java.nio.ByteBuffer;

public class FemtoZipCompressionModelBuilder {

    private final ByteBuffer dictionary;
    private final DocumentList documents;

    protected FemtoZipCompressionModelBuilder(ByteBuffer dictionary, DocumentList documents) {
        this.dictionary = dictionary;
        this.documents = documents;
    }

    public static FemtoZipHuffmanModel buildModel(ByteBuffer dictionary, DocumentList documents) throws IOException {
        FemtoZipCompressionModelBuilder fzcmb = new FemtoZipCompressionModelBuilder(dictionary, documents);
        return fzcmb.buildModel();
    }


    public FemtoZipHuffmanModel buildModel() throws IOException {

        SubstringPacker modelBuildingPacker = new SubstringPacker(dictionary);
        FemtoZipHuffmanModelBuilder modelBuilder = new FemtoZipHuffmanModelBuilder();
        for (int i = 0, count = documents.size(); i < count; i++) {
            modelBuildingPacker.pack(documents.getBB(i), modelBuilder, null);
        }

        return modelBuilder.createModel();
    }
}
