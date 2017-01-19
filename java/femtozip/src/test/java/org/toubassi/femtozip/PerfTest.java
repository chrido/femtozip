package org.toubassi.femtozip;

import java.nio.ByteBuffer;

import org.junit.Ignore;
import org.junit.Test;
import org.toubassi.femtozip.models.CompressionModelBase;
import org.toubassi.femtozip.models.CompressionModelVariant;

import java.io.*;
import java.nio.charset.Charset;
import java.util.ArrayList;

@Ignore
public class PerfTest {
    @Test
    public void testPerformance() throws IOException {

        ArrayList<ByteBuffer> documents = new ArrayList<>();


        String line;
        int i = 0;
        try (
                InputStream fis = new FileInputStream("/home/chris/datasets/sdcdata/meetup/rsvps.json");
                InputStreamReader isr = new InputStreamReader(fis, Charset.forName("UTF-8"));
                BufferedReader br = new BufferedReader(isr);
        ) {
            while (((line = br.readLine()) != null) && (i < 200)) {
                documents.add(ByteBuffer.wrap(line.getBytes(Charset.forName("UTF-8"))));
                i ++;
            }
        }
        System.out.println("#begin:building-dict");
        ArrayDocumentList arrayDocumentList = new ArrayDocumentList(documents);
        long sum = 0;

        for(int j = 1; j < 20; j ++) {

            for (ByteBuffer document : documents) {
                document.rewind();
            }

            long begin = System.currentTimeMillis();
            CompressionModel compressionModel = CompressionModelBase.buildModel(CompressionModelVariant.FemtoZip, arrayDocumentList);
            long duration = System.currentTimeMillis() - begin;

            if(j > 3) {
                sum += duration;
                System.out.println("#run:" + j + "#duration:" + duration + "#avg:" + sum / (j-3) + "#sum:" + sum);
            }
            else {
                System.out.println("Warming up");
            }
        }
    }
}
