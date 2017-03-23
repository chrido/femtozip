package org.toubassi.femtozip;

import org.junit.Assert;
import org.junit.Test;
import org.toubassi.femtozip.dictionary.DictionaryCleaner;
import org.toubassi.femtozip.dictionary.DictionaryOptimizer;
import org.toubassi.femtozip.models.VerboseStringCompressionModel;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;

import static org.toubassi.femtozip.models.CompressionModelBase.rewindReaderIndexDocumentList;
import static org.toubassi.femtozip.util.FileUtil.getString;


public class InternalRepetitionRemoverTest {

    @Test
    public void testRemovingInternalRepetitions() throws IOException {
        ByteBuffer doc1 = ByteBuffer.wrap("6aaaaaabcdeX6aaaaaabcde".getBytes("UTF-8"));
        //                                 | ext     |--|   int  |

        ByteBuffer doc2 = ByteBuffer.wrap("6aaaaaajkloY6aaaaaajklo".getBytes("UTF-8"));
        //                                 | ext |-----| int     |

        ByteBuffer doc3 = ByteBuffer.wrap("6aaaaaaerqwX6aaaaaaerqw".getBytes("UTF-8"));
        //                                 | ext |-----| int     |

        ArrayList<ByteBuffer> docs = new ArrayList<>();
        docs.add(doc1);
        docs.add(doc2);
        docs.add(doc3);

        //Dictionary old
        ByteBuffer oldDict = ByteBuffer.wrap("6aaaaaabcde6aaaaaaerqw6aaaaaajkloX6aaaaaa".getBytes("UTF-8"));
        rewindReaderIndexDocumentList(new ArrayDocumentList(docs));
        checkContents(oldDict, doc1, "<-41,11>X<-12,11>");
        checkContents(oldDict, doc2, "<-19,11>Y<-12,11>");
        checkContents(oldDict, doc3, "<-30,11>X<-12,11>");


        //Dictionary scrubbed - less compression but smaller dictionary
        rewindReaderIndexDocumentList(new ArrayDocumentList(docs));
        ByteBuffer dict = testDictScrubbing("6aaaaaabcde6aaaaaaerqw6aaaaaajklo", "6aaaaaabcde6aaaaaaerqw6aaaaaajkloX6aaaaaa", new ArrayDocumentList(docs));
        checkContents(dict, doc1, "<-33,11>X<-12,11>");
        checkContents(dict, doc2, "<-11,11>Y<-12,11>");
        checkContents(dict, doc3, "<-22,11>X<-12,11>");
    }

    private void checkContents(ByteBuffer dict, ByteBuffer doc1, String s) {
        VerboseStringCompressionModel vscm = new VerboseStringCompressionModel(dict);
        ByteBuffer allocate = ByteBuffer.allocate(500);
        vscm.compress(doc1, allocate);
        //System.out.println(getString(allocate));

        Assert.assertEquals(s, getString(allocate));
    }

    private ByteBuffer testDictScrubbing(String shouldbeDictionary, String dictbefore, ArrayDocumentList documents) throws IOException {
        DictionaryOptimizer optimizer = new DictionaryOptimizer(documents);
        ByteBuffer dictionary = optimizer.optimize(1000);
        rewindReaderIndexDocumentList(documents);

        Assert.assertEquals(dictbefore, getString(dictionary));

        DictionaryCleaner dc = new DictionaryCleaner(dictionary);
        dc.recordUsage(documents);

        rewindReaderIndexDocumentList(documents);

        ByteBuffer scrubbedDictionary = dc.simpleScrubber(1);
        String scrubbedDictionaryStr = getString(scrubbedDictionary);
        //System.out.println(dc.usageAsString());

        Assert.assertEquals("Compressed: ", shouldbeDictionary, scrubbedDictionaryStr);
        return scrubbedDictionary;
    }
}
