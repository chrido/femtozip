package org.toubassi.femtozip;

import java.nio.ByteBuffer;

import org.junit.Ignore;
import org.junit.Test;
import org.toubassi.femtozip.models.CompressionModelBase;
import org.toubassi.femtozip.models.CompressionModelVariant;

import java.io.*;
import java.nio.charset.Charset;
import java.util.ArrayList;

import static org.toubassi.femtozip.util.FileUtil.getString;

@Ignore
public class PerfTest {
    @Test
    public void testPerformance() throws IOException {

        ArrayList<ByteBuffer> training = new ArrayList<>();
        ArrayList<ByteBuffer> test = new ArrayList<>();

        int trainingdocs = 1000;
        int testdocs = 20000;

        String line;
        int i = 0;
        try (
                InputStream fis = new FileInputStream("/home/chris/datasets/sdcdata/meetup/rsvps.json");
                InputStreamReader isr = new InputStreamReader(fis, Charset.forName("UTF-8"));
                BufferedReader br = new BufferedReader(isr);
        ) {
            while (((line = br.readLine()) != null) && (i < trainingdocs)) {
                training.add(ByteBuffer.wrap(line.getBytes(Charset.forName("UTF-8"))));
                i ++;
            }
            i = 0;
            while (((line = br.readLine()) != null) && (i < testdocs)) {
                test.add(ByteBuffer.wrap(line.getBytes(Charset.forName("UTF-8"))));
                i ++;
            }
        }
        System.out.println("#begin:building-dict");
        ArrayDocumentList trainingDocs = new ArrayDocumentList(training);
        ByteBuffer compressionOutput = ByteBuffer.allocate(5000);
        long sumBuilding = 0;

        for(int j = 1; j < 20; j ++) {

            for (ByteBuffer document : training) {
                document.rewind();
            }

            long beginBuilding = System.currentTimeMillis();
            CompressionModel compressionModel = CompressionModelBase.buildModel(CompressionModelVariant.FemtoZip, trainingDocs);
            long durationBuildingModel = System.currentTimeMillis() - beginBuilding;

            //Compression Performance

            int originalLength = 0;
            int compressedLength = 0;

            long beginCompressing = System.currentTimeMillis();

            for(ByteBuffer doc: test) {
                doc.rewind();
                int docSize = doc.remaining();
                int resCompressedLength = compressionModel.compress(doc, compressionOutput);

                //ByteBuffer decompressed = ByteBuffer.allocate(docSize);
                //int decompress = compressionModel.decompress(compressionOutput, decompressed);

                //System.out.println("#compressed:" + resCompressedLength + "#uncompressed:" + docSize);

                originalLength += docSize;
                compressedLength += resCompressedLength;

                compressionOutput.clear();

            }
            long durationCompression = System.currentTimeMillis() - beginCompressing;


            if(j > 3) {
                sumBuilding += durationBuildingModel;
                System.out.println("#run:" + j + "#duration:" + durationBuildingModel + "#avg:" + sumBuilding / (j-3) + "#sum:" + sumBuilding + "#durationcompression:" + durationCompression +"#ratio:" + (100-(100.0/originalLength * compressedLength)));
            }
            else {
                System.out.println("warm-up: #run:" + j + "#duration:" + durationBuildingModel + "#avg:--" + "#sum:" + sumBuilding + "#durationcompression:" + durationCompression +"#ratio:" + (100-(100.0/originalLength * compressedLength)));
            }
        }
    }
}
