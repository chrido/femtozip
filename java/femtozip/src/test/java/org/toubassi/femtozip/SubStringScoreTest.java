package org.toubassi.femtozip;

import org.junit.Assert;
import org.junit.Test;
import org.toubassi.femtozip.dictionary.DictionaryOptimizer;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;


public class SubStringScoreTest {
    @Test
    public void example() throws IOException {

        int maxDictionaryLength = 2000;

        String commonOccured = "arash";
        ByteBuffer commonOccuredAsByteBuffer = ByteBuffer.wrap(commonOccured.getBytes(Charset.forName("UTF-8")));
        ArrayDocumentList trainingDocs = new ArrayDocumentList("http://espn.de", "http://popsugar.de",
                "http://google.de", "http://yahoo.de", "http://www.linkedin.com", "http://www.facebook.com",
                "http:www.stanford.edu", commonOccured + "!",
                commonOccured + ">", commonOccured + "_",
                commonOccured + ")");

        DictionaryOptimizer optimizer = new DictionaryOptimizer(trainingDocs);
        optimizer.optimize(maxDictionaryLength);

        Map<ByteBuffer, Integer> subscores = optimizer.calcSubstringScores(maxDictionaryLength);

        Assert.assertFalse(subscores.isEmpty());
        Assert.assertTrue(subscores.containsKey(commonOccuredAsByteBuffer));

        //Score from SubstringArrays: (100 * count * (length - 3)) / length
        //arash = 5 letters; 100*4 * (5 - 3) / 5
        Integer scoreArash = 100*4 * (5 - 3) / 5;
        Assert.assertEquals(scoreArash, subscores.get(commonOccuredAsByteBuffer));
    }
}
