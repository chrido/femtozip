package org.toubassi.femtozip.models;

import org.toubassi.femtozip.CompressionModel;

import java.nio.ByteBuffer;

/**
 * Created by chris on 15.12.16.
 */
public class Util {

    /**
     * Instantiates a new Compressionmodel based on the Enum
     * @param model
     * @return
     */
    public static CompressionModel fromEnum(CompressionModelVariant model) {
        switch (model) {
            case GZip:
                return new GZipCompressionModel();
            case FemtoZip:
                return new FemtoZipCompressionModel();
            case GZipDictionary:
                return new GZipDictionaryCompressionModel();
            case PureHuffmann:
                return new PureHuffmanCompressionModel();
            case VariableInt:
                return new VariableIntCompressionModel();
            case VerboseString:
                return new VerboseStringCompressionModel();
        }

        throw new RuntimeException("Unknown CompressionModel");
    }

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
