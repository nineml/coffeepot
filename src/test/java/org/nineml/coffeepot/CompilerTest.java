package org.nineml.coffeepot;

import org.junit.Ignore;
import org.junit.Test;
import org.nineml.coffeefilter.InvisibleXml;
import org.nineml.coffeefilter.InvisibleXmlParser;

import java.io.File;
import java.io.IOException;

import static org.junit.Assert.fail;

public class CompilerTest {
    @Ignore
    public void compileGrammar() {
        InvisibleXml ixml = new InvisibleXml();
        try {
            InvisibleXmlParser parser = ixml.getParser(new File("/Volumes/Projects/nineml/grinder/src/test/resources/ixml.ixml"));
            String s = parser.getCompiledParser();
            //System.err.println(s);
        } catch (IOException e) {
            fail();
        }
    }

    @Ignore
    public void compileAmbigousGrammar() {
        InvisibleXml ixml = new InvisibleXml();
        try {
            InvisibleXmlParser parser = ixml.getParser(new File("/Volumes/Projects/nineml/grinder/src/test/resources/property-file.ixml"));
            String s = parser.getCompiledParser();
            //System.err.println(s);
        } catch (IOException e) {
            fail();
        }
    }
}
