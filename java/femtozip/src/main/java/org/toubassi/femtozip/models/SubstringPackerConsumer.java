package org.toubassi.femtozip.models;

import org.toubassi.femtozip.substring.SubstringPacker;

import java.io.PrintWriter;

/**
 * Created by chris on 15.12.16.
 */
public class SubstringPackerConsumer implements SubstringPacker.Consumer {

    @Override
    public void encodeLiteral(int aByte, Object context) {
        PrintWriter writer = (PrintWriter)context;
        writer.print((char)aByte);
    }

    @Override
    public void encodeSubstring(int offset, int length, Object context) {
        PrintWriter writer = (PrintWriter)context;
        writer.print('<');
        writer.print(offset);
        writer.print(',');
        writer.print(length);
        writer.print('>');
    }

    @Override
    public void endEncoding(Object context) {
        PrintWriter writer = (PrintWriter)context;
        writer.close();
    }
}