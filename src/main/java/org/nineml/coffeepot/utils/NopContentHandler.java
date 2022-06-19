package org.nineml.coffeepot.utils;

import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;

public class NopContentHandler implements ContentHandler {
    @Override
    public void setDocumentLocator(Locator locator) {
        // nop
    }

    @Override
    public void startDocument() throws SAXException {
        // nop
    }

    @Override
    public void endDocument() throws SAXException {
        // nop
    }

    @Override
    public void startPrefixMapping(String prefix, String uri) throws SAXException {
        // nop
    }

    @Override
    public void endPrefixMapping(String prefix) throws SAXException {
        // nop
    }

    @Override
    public void startElement(String uri, String localName, String qName, Attributes atts) throws SAXException {
        // nop
    }

    @Override
    public void endElement(String uri, String localName, String qName) throws SAXException {
        // nop
    }

    @Override
    public void characters(char[] ch, int start, int length) throws SAXException {
        // nop
    }

    @Override
    public void ignorableWhitespace(char[] ch, int start, int length) throws SAXException {
        // nop
    }

    @Override
    public void processingInstruction(String target, String data) throws SAXException {
        // nop
    }

    @Override
    public void skippedEntity(String name) throws SAXException {
        // nop
    }
}
