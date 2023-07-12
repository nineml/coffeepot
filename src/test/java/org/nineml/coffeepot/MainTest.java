package org.nineml.coffeepot;

import com.saxonica.ee.schema.Assertion;
import org.junit.Test;
import org.junit.jupiter.api.Assertions;
import org.nineml.coffeepot.managers.OutputManager;

import static org.junit.Assert.fail;

public class MainTest extends CoffeePotTest {

    @Test
    public void help() {
        WrappedPrintStream stdout = new WrappedPrintStream();
        WrappedPrintStream stderr = new WrappedPrintStream();
        Main main = new Main(stdout.stream, stderr.stream);
        try {
            OutputManager manager = main.commandLine(new String[] {"--help" });
            Assertions.assertEquals(0, manager.stringRecords.size());
            Assertions.assertTrue(stderr.contains("Usage:"));
            Assertions.assertEquals(0, manager.getReturnCode());
        } catch (Exception ex) {
            fail();
        }
    }

    @Test
    public void smokeTestXml() {
        Main main = new Main();
        try {
            OutputManager manager = main.commandLine(new String[] {"-g:src/test/resources/smoke.ixml", "a" });
            Assertions.assertEquals(1, manager.stringRecords.size());
            Assertions.assertEquals("<S><A>a</A></S>", manager.stringRecords.get(0));
        } catch (Exception ex) {
            fail();
        }
    }

    @Test
    public void smokeTestJson() {
        Main main = new Main();
        try {
            OutputManager manager = main.commandLine(new String[] {"-g:src/test/resources/smoke.ixml", "--format:json", "a" });
            Assertions.assertEquals(1, manager.stringRecords.size());
            Assertions.assertEquals("{\"S\":{\"A\":\"a\"}}", manager.stringRecords.get(0));
        } catch (Exception ex) {
            fail();
        }
    }

    @Test
    public void showEarleyGrammar() {
        WrappedPrintStream stdout = new WrappedPrintStream();
        WrappedPrintStream stderr = new WrappedPrintStream();
        Main main = new Main(stdout.stream, stderr.stream);
        try {
            OutputManager manager = main.commandLine(new String[] {"-g:src/test/resources/smoke.ixml", "--show-grammar", "a" });
            Assertions.assertEquals(1, manager.stringRecords.size());
            Assertions.assertEquals("<S><A>a</A></S>", manager.stringRecords.get(0));
            Assertions.assertTrue(stderr.contains("The Earley grammar"));
            Assertions.assertTrue(stderr.contains("2.  S ::= A"));
        } catch (Exception ex) {
            fail();
        }
    }

    @Test
    public void showGllGrammar() {
        WrappedPrintStream stdout = new WrappedPrintStream();
        WrappedPrintStream stderr = new WrappedPrintStream();
        Main main = new Main(stdout.stream, stderr.stream);
        try {
            OutputManager manager = main.commandLine(new String[] {"-g:src/test/resources/smoke.ixml", "--show-grammar", "--gll", "a" });
            Assertions.assertEquals(1, manager.stringRecords.size());
            Assertions.assertEquals("<S><A>a</A></S>", manager.stringRecords.get(0));
            Assertions.assertTrue(stderr.contains("The GLL grammar"));
            Assertions.assertTrue(stderr.contains("2.  S ::= A"));
        } catch (Exception ex) {
            fail();
        }
    }

    @Test
    public void noOutput() {
        Main main = new Main();
        try {
            OutputManager manager = main.commandLine(new String[] {"-g:src/test/resources/smoke.ixml", "a", "--no-output"});
            Assertions.assertEquals(1, manager.stringRecords.size());
            Assertions.assertEquals("", manager.publication());
        } catch (Exception ex) {
            fail();
        }
    }

    @Test
    public void timing() {
        WrappedPrintStream stdout = new WrappedPrintStream();
        WrappedPrintStream stderr = new WrappedPrintStream();
        Main main = new Main(stdout.stream, stderr.stream);
        try {
            OutputManager manager = main.commandLine(new String[] {"-g:src/test/resources/smoke.ixml", "a", "--time"});
            Assertions.assertEquals(1, manager.stringRecords.size());
            Assertions.assertTrue(stderr.toString().contains("Parsed src/"));
            Assertions.assertTrue(stderr.toString().contains("Parsed input in"));
        } catch (Exception ex) {
            fail();
        }
    }

    @Test
    public void ambig2() {
        WrappedPrintStream stdout = new WrappedPrintStream();
        WrappedPrintStream stderr = new WrappedPrintStream();
        Main main = new Main(stdout.stream, stderr.stream);
        try {
            OutputManager manager = main.commandLine(new String[] {"-g:src/test/resources/ambig2.ixml", "x" });
            Assertions.assertEquals(1, manager.stringRecords.size());
            Assertions.assertTrue(manager.stringRecords.get(0).contains("ambiguous"));
            Assertions.assertTrue(stderr.contains("Found 3 possible"));
        } catch (Exception ex) {
            fail();
        }
    }

    @Test
    public void ambig2_suppressed() {
        WrappedPrintStream stdout = new WrappedPrintStream();
        WrappedPrintStream stderr = new WrappedPrintStream();
        Main main = new Main(stdout.stream, stderr.stream);
        try {
            OutputManager manager = main.commandLine(new String[] {"-g:src/test/resources/ambig2.ixml", "--suppress:ambiguous", "x" });
            Assertions.assertEquals(1, manager.stringRecords.size());
            Assertions.assertFalse(manager.stringRecords.get(0).contains("ambiguous"));
        } catch (Exception ex) {
            fail();
        }
    }

    @Test
    public void describeAmbiguity() {
        WrappedPrintStream stdout = new WrappedPrintStream();
        WrappedPrintStream stderr = new WrappedPrintStream();
        Main main = new Main(stdout.stream, stderr.stream);
        try {
            OutputManager manager = main.commandLine(new String[] {"-g:src/test/resources/ambig2.ixml", "--describe-ambiguity", "x" });
            Assertions.assertEquals(1, manager.stringRecords.size());
            Assertions.assertTrue(stdout.contains("✔ S «1,1» ⇒ 'x'"));
        } catch (Exception ex) {
            fail();
        }
    }

    @Test
    public void analyzeAmbiguity() {
        WrappedPrintStream stdout = new WrappedPrintStream();
        WrappedPrintStream stderr = new WrappedPrintStream();
        Main main = new Main(stdout.stream, stderr.stream);
        try {
            OutputManager manager = main.commandLine(new String[] {"-g:src/test/resources/ambig2.ixml", "--analyze-ambiguity", "x" });
            Assertions.assertEquals(1, manager.stringRecords.size());
            Assertions.assertTrue(stdout.contains("The grammar is ambiguous"));
            Assertions.assertTrue(stdout.contains("vertical ambiguity:"));
            Assertions.assertTrue(stdout.contains("horizontal ambiguity:"));
        } catch (Exception ex) {
            fail();
        }
    }

    @Test
    public void showMarks() {
        WrappedPrintStream stdout = new WrappedPrintStream();
        WrappedPrintStream stderr = new WrappedPrintStream();
        Main main = new Main(stdout.stream, stderr.stream);
        try {
            OutputManager manager = main.commandLine(new String[] {"-g:src/test/resources/priority.ixml", "--show-marks", "due 02/07/2023" });
            Assertions.assertEquals(1, manager.stringRecords.size());
            String result = manager.stringRecords.get(0);
            Assertions.assertTrue(result.contains(" ixml:mark"));
        } catch (Exception ex) {
            fail();
        }
    }

    @Test
    public void prettyPrint() {
        WrappedPrintStream stdout = new WrappedPrintStream();
        WrappedPrintStream stderr = new WrappedPrintStream();
        Main main = new Main(stdout.stream, stderr.stream);
        try {
            OutputManager manager = main.commandLine(new String[] {"-g:src/test/resources/priority.ixml", "-pp", "due 02/07/2023" });
            Assertions.assertEquals(1, manager.stringRecords.size());
            String result = manager.stringRecords.get(0);
            Assertions.assertTrue(result.contains("<deadline>\n   <date>\n"));
        } catch (Exception ex) {
            fail();
        }
    }

    @Test
    public void showHiddenNonterminals() {
        WrappedPrintStream stdout = new WrappedPrintStream();
        WrappedPrintStream stderr = new WrappedPrintStream();
        Main main = new Main(stdout.stream, stderr.stream);
        try {
            OutputManager manager = main.commandLine(new String[] {"-g:src/test/resources/priority.ixml", "--show-hidden-nonterminals", "due 02/07/2023" });
            Assertions.assertEquals(1, manager.stringRecords.size());
            String result = manager.stringRecords.get(0);
            Assertions.assertTrue(result.contains("<n:symbol"));
            Assertions.assertTrue(result.contains("name='$$'"));
        } catch (Exception ex) {
            fail();
        }
    }

    @Test
    public void encoding() {
        WrappedPrintStream stdout = new WrappedPrintStream();
        WrappedPrintStream stderr = new WrappedPrintStream();
        Main main = new Main(stdout.stream, stderr.stream);
        try {
            OutputManager manager = main.commandLine(new String[] {"-g:src/test/resources/iso-latin-1.ixml",
                    "--grammar-encoding:iso-8859-1", "--encoding:iso-8859-1", "-i:src/test/resources/iso-latin-1.txt" });
            Assertions.assertEquals(1, manager.stringRecords.size());
            Assertions.assertEquals("<S>© (C)</S>", manager.stringRecords.get(0));
        } catch (Exception ex) {
            fail();
        }
    }

    @Test
    public void bnf() {
        WrappedPrintStream stdout = new WrappedPrintStream();
        WrappedPrintStream stderr = new WrappedPrintStream();
        Main main = new Main(stdout.stream, stderr.stream);
        try {
            OutputManager manager = main.commandLine(new String[] {"-g:src/test/resources/simple-bnf.ixml",
                    "--bnf", "bc" });
            Assertions.assertEquals(1, manager.stringRecords.size());
            Assertions.assertEquals("<S><B>b<C>c</C></B></S>", manager.stringRecords.get(0));
            Assertions.assertEquals("", stderr.toString());
        } catch (Exception ex) {
            fail();
        }
    }

    @Test
    public void notBnf() {
        WrappedPrintStream stdout = new WrappedPrintStream();
        WrappedPrintStream stderr = new WrappedPrintStream();
        Main main = new Main(stdout.stream, stderr.stream);
        try {
            OutputManager manager = main.commandLine(new String[] {"-g:src/test/resources/not-bnf.ixml",
                    "--bnf", "abcbc" });
            Assertions.assertEquals(1, manager.stringRecords.size());
            Assertions.assertEquals("<S><A>a</A><B>b<C>c</C></B><B>b<C>c</C></B></S>", manager.stringRecords.get(0));
            Assertions.assertEquals("Grammar does not conform to plain BNF: S\n", stderr.toString());
        } catch (Exception ex) {
            fail();
        }
    }


}
