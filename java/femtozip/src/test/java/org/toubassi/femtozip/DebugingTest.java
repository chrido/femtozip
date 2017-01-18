package org.toubassi.femtozip;

import org.junit.Test;
import org.toubassi.femtozip.dictionary.SuffixArray;

import java.nio.charset.Charset;

public class DebugingTest {

    public void test() {
        byte[] orgString = "hallo hallo".getBytes(Charset.forName("UTF-8"));
        int[] ints = SuffixArray.computeSuffixArray(orgString);
        int[] lcp = SuffixArray.computeLCP(orgString, ints);

        SuffixArray.dump(System.out, orgString, ints, lcp);
    }
}
