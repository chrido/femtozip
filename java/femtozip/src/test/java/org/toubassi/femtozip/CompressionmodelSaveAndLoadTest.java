package org.toubassi.femtozip;


import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.toubassi.femtozip.models.CompressionModelBase;
import org.toubassi.femtozip.models.CompressionModelVariant;

import java.io.*;
import java.nio.ByteBuffer;
import java.util.ArrayList;

import static org.toubassi.femtozip.TestUtil.generateSampleDoc;
import static org.toubassi.femtozip.TestUtil.getTrainingDocs;

@RunWith(Parameterized.class)
public class CompressionmodelSaveAndLoadTest {

    private final CompressionModelVariant variant;

    public CompressionmodelSaveAndLoadTest(CompressionModelVariant variant) {
        this.variant = variant;
    }

    @Parameterized.Parameters()
    public static Iterable<Object[]> data() {
        return TestUtil.getActiveCompressionModels();
    }


    @Test
    public void testSaveAndLoad() throws IOException {
        ArrayList<ByteBuffer> trainingDocs = getTrainingDocs();
        CompressionModel compressionModel = CompressionModelBase.buildModel(variant, new ArrayDocumentList(trainingDocs));

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(baos);
        compressionModel.save(dos);
        dos.close();
        baos.close();

        byte[] serializedModel = baos.toByteArray();

        ByteArrayInputStream bais = new ByteArrayInputStream(serializedModel);
        DataInputStream dis = new DataInputStream(bais);
        CompressionModel restoredModel = CompressionModelBase.loadModel(dis);

        Assert.assertEquals(compressionModel.getClass().getName(), restoredModel.getClass().getName());

        compressionComparison(compressionModel, restoredModel);
    }

    private void compressionComparison(CompressionModel org, CompressionModel restored) {
        ByteBuffer doc = generateSampleDoc(200);
        ByteBuffer orgCompressed = ByteBuffer.allocate(doc.remaining() * 3);
        ByteBuffer restoredCompressed = ByteBuffer.allocate(doc.remaining() * 3);

        org.compress(doc, orgCompressed);
        doc.rewind();
        restored.compress(doc, restoredCompressed);

        Assert.assertTrue("Error: " + variant.name(), orgCompressed.equals(restoredCompressed));
    }

}
