package org.toubassi.femtozip.models.huffmann;


import org.toubassi.femtozip.DocumentList;
import org.toubassi.femtozip.coding.huffman.FrequencyHuffmanModel;
import org.toubassi.femtozip.models.femtozip.FemtoZipCompressionModelBuilder;
import org.toubassi.femtozip.models.femtozip.FemtoZipHuffmanModel;

import java.io.IOException;
import java.nio.ByteBuffer;

public class FrequencyHuffmanModelBuilder {

    private final DocumentList documents;

    public FrequencyHuffmanModelBuilder(DocumentList documents) {
        this.documents = documents;
    }

    public static FrequencyHuffmanModel buildModel(DocumentList documents) throws IOException {
        FrequencyHuffmanModelBuilder fhmb = new FrequencyHuffmanModelBuilder(documents);
        return fhmb.build();
    }


    public FrequencyHuffmanModel build() throws IOException {
        int[] histogram = new int[256 + 1]; // +1 for EOF

        for (int i = 0, count = documents.size(); i < count; i++) {
            ByteBuffer bytes = documents.getBB(i);
            for (int j = 0, jcount = bytes.remaining(); j < jcount; j++) { //TODO: just read through
                histogram[bytes.get(j) & 0xff]++;
            }
            histogram[histogram.length - 1]++;
        }

        return new FrequencyHuffmanModel(histogram, false);
    }
}
