package org.toubassi.femtozip.dictionary;


import org.toubassi.femtozip.ArrayDocumentList;
import org.toubassi.femtozip.substring.SubstringPacker;

import java.nio.ByteBuffer;

public class DictionaryCleaner implements SubstringPacker.Consumer{

    int[] dictUsage;
    int currentIndex = 0;
    int internalUsage;
    int dictLength;
    private final ByteBuffer dictionary;


    public DictionaryCleaner(ByteBuffer dictionary) {
        dictLength = dictionary.remaining();
        this.dictionary = dictionary;

        dictUsage = new int[dictLength];
        for(int i = 0; i < dictLength; i++)
            dictUsage[0] = 0;
        internalUsage = 0;
    }

    public String usageAsString() {
        StringBuilder sb = new StringBuilder();
        for(int entry: dictUsage) {
            sb.append(entry);
            sb.append("&");
        }
        return sb.toString();
    }

    public DictUsageStats getUsageStats() {
        int notUsed = 0;
        int used = 0;

        for (int entry : dictUsage) {
            if (entry == 0)
                notUsed++;
            else
                used++;
        }

        double notUsedPercent = 100.0 / dictUsage.length * notUsed;
        double usedPercent = 100.0 / dictUsage.length * used;

        return new DictUsageStats(notUsedPercent, usedPercent, internalUsage);
    }

    public ByteBuffer getScrubbedDictionary() {
        return getScrubbedDictionary(1, 5);
    }

    public ByteBuffer getScrubbedDictionary(int minUsage, int mingap) {
        int entryCount = 0;
        for(int entry : dictUsage) {
            if(entry >= minUsage) {
                entryCount++;
            }
        }

        ByteBuffer bb = ByteBuffer.allocate(entryCount);
        int gapcount = 0;
        for(int i = 0; i < dictLength; i++) {
            if(dictUsage[i] > minUsage) {
                gapcount++;
            }
            else {
                if (gapcount > mingap) {
                    gapcount = 0;
                }
                else {
                    for (; gapcount >= 0; gapcount--) {
                        bb.put(dictionary.get(i - gapcount));
                    }
                    gapcount = 0;
                }
            }
        }
        bb.flip();

        return bb;
    }

    public ByteBuffer simpleScrubber(int minUsage) {
        int entryCount = 0;
        for(int entry : dictUsage) {
            if(entry >= minUsage) {
                entryCount++;
            }
        }

        ByteBuffer bb = ByteBuffer.allocate(entryCount);
        for(int i = 0; i < dictLength; i++) {
            if(dictUsage[i] >= minUsage) {
                bb.put(dictionary.get(i));
            }
        }
        bb.flip();

        return bb;
    }

    @Override
    public void encodeLiteral(int aByte, Object context) {
        currentIndex += 1;
    }

    @Override
    public void encodeSubstring(int offset, int length, Object context) {
        if (currentIndex + offset < 0) { //substring starts in dictionary
            int startDict = currentIndex + offset + dictLength;
            int endDict = startDict + length;
            int end = 0;

            if (endDict > dictLength) {
                end = endDict - dictLength;
                endDict = dictLength;
            }

            for (int i = startDict; i < endDict; i++) {
                dictUsage[i]++;
            }

            if (end > 0) { //the dictionary entry and content overlaps
                for (int i = 0; i < end; i++) {
                    internalUsage++;
                }
            }

        } else { //internal entry
            internalUsage += length;
        }
        currentIndex += length;
    }

    @Override
    public void endEncoding(Object context) {
        //do nothing
    }

    public void recordUsage(ByteBuffer bb) {
        SubstringPacker sp = new SubstringPacker(this.dictionary);
        sp.pack(bb, this, null);
    }

    public void recordUsage(ArrayDocumentList arrayDocumentList) {
        SubstringPacker sp = new SubstringPacker(this.dictionary);

        for(int i = 0; i < arrayDocumentList.size(); i++) {
            ByteBuffer document = arrayDocumentList.getBB(i);
            sp.pack(document, this, null);
            currentIndex = 0;
        }
    }
}
