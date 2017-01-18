package org.toubassi.femtozip.models;

import org.toubassi.femtozip.substring.SubstringPacker;

import java.io.PrintWriter;

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