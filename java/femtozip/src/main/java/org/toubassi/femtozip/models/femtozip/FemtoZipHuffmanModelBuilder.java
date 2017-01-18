package org.toubassi.femtozip.models.femtozip;

import org.toubassi.femtozip.coding.huffman.FrequencyHuffmanModel;
import org.toubassi.femtozip.substring.SubstringPacker;

public class FemtoZipHuffmanModelBuilder implements SubstringPacker.Consumer {
    private int[] literalLengthHistogram = new int[256 + 256 + 1]; // 256 for each unique literal byte, 256 for all possible length, plus 1 for EOF
    private int[] offsetHistogramNibble0 = new int[16];
    private int[] offsetHistogramNibble1 = new int[16];
    private int[] offsetHistogramNibble2 = new int[16];
    private int[] offsetHistogramNibble3 = new int[16];

    @Override
    public void encodeLiteral(int aByte, Object context) {
        literalLengthHistogram[aByte]++;
    }

    @Override
    public void endEncoding(Object context) {
        literalLengthHistogram[literalLengthHistogram.length - 1]++;
    }

    @Override
    public void encodeSubstring(int offset, int length, Object context) {

        if (length < 1 || length > 255) {
            throw new IllegalArgumentException("Length " + length + " out of range [1,255]");
        }
        literalLengthHistogram[256 + length]++;

        offset = -offset;
        if (length < 1 || offset > (2<<15)-1) {
            throw new IllegalArgumentException("Length " + length + " out of range [1, 65535]");
        }
        offsetHistogramNibble0[offset & 0xf]++;
        offsetHistogramNibble1[(offset >> 4) & 0xf]++;
        offsetHistogramNibble2[(offset >> 8) & 0xf]++;
        offsetHistogramNibble3[(offset >> 12) & 0xf]++;
    }

    public FemtoZipHuffmanModel createModel() {
        return new FemtoZipHuffmanModel(
                new FrequencyHuffmanModel(literalLengthHistogram, false),
                new FrequencyHuffmanModel(offsetHistogramNibble0, false),
                new FrequencyHuffmanModel(offsetHistogramNibble1, false),
                new FrequencyHuffmanModel(offsetHistogramNibble2, false),
                new FrequencyHuffmanModel(offsetHistogramNibble3, false));
    }
}
