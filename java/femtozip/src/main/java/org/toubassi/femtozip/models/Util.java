package org.toubassi.femtozip.models;

import org.toubassi.femtozip.CompressionModel;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.Arrays;

/**
 * Created by chris on 15.12.16.
 */
public class Util {

    /**
     * Duplicates the dictionary ByteBuffer
     * In case the dictionary is to long,
     * @param dictionary
     * @param maxDictionaryLength
     * @return
     */
    public static ByteBuffer trimDictionary(ByteBuffer dictionary, int maxDictionaryLength) {
        if(dictionary.remaining() > maxDictionaryLength) {
            int oldPosition = dictionary.position();

            dictionary.position(dictionary.remaining() - maxDictionaryLength);
            ByteBuffer trimmed = dictionary.slice();
            dictionary.position(oldPosition);

            return trimmed;
        }
        return dictionary.slice();
    }

}
