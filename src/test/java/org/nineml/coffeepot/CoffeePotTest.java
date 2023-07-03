package org.nineml.coffeepot;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;

public class CoffeePotTest {
    protected static class WrappedPrintStream {
        public final ByteArrayOutputStream baos;
        public final PrintStream stream;
        public WrappedPrintStream() {
            baos = new ByteArrayOutputStream();
            stream = new PrintStream(baos);
        }
        public String toString() {
            try {
                return baos.toString("UTF-8");
            } catch (UnsupportedEncodingException ex) {
                throw new RuntimeException(ex); // this can't happen...
            }
        }
        public boolean contains(String target) {
            return toString().contains(target);
        }
    }
}
