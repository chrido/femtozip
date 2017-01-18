package org.toubassi.femtozip;

import org.toubassi.femtozip.models.CompressionModelVariant;

import java.nio.ByteBuffer;
import java.util.ArrayList;

public class TestUtil {

    public static void generateSampleDoc(ByteBuffer out, int length) {
        for (int i = 0; i < length; i++) {
            if (Math.random() < .1) {
                for(int j = 0; i < length; i++, j++) {
                    out.put((byte) j);
                }
            }
            else {
                out.put((byte) (Math.random() * 0xff));
            }
        }
    }
    public static ByteBuffer generateSampleDoc(int length) {
        ByteBuffer out = ByteBuffer.allocate(length);
        generateSampleDoc(out, length);
        out.rewind();
        return out;
    }

    public static ArrayList<Object[]> getActiveCompressionModels() {
        ArrayList<Object[]> allCombinations = new ArrayList<>();
        for (CompressionModelVariant variant : CompressionModelVariant.values()) {
            if(variant == CompressionModelVariant.Native)
                continue;
            allCombinations.add(new Object[]{variant});
        }
        return allCombinations;
    }

    public static ArrayList<ByteBuffer> getTrainingDocs() {
        ArrayList<ByteBuffer> trainingDocs = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            trainingDocs.add(generateSampleDoc((int)(Math.random() * 100) + 100));
        }
        return trainingDocs;
    }

}
