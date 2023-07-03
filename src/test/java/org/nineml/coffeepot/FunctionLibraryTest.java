package org.nineml.coffeepot;

import org.junit.Test;
import org.junit.jupiter.api.Assertions;
import org.nineml.coffeepot.managers.OutputManager;

import static org.junit.Assert.fail;

public class FunctionLibraryTest extends CoffeePotTest {
    @Test
    public void ambig1SelectAB() {
        WrappedPrintStream stdout = new WrappedPrintStream();
        WrappedPrintStream stderr = new WrappedPrintStream();
        Main main = new Main(stdout.stream, stderr.stream);
        try {
            OutputManager manager = main.commandLine(new String[] {"-g:src/test/resources/ambig1.ixml", "--function-library:src/test/resources/ambig1-a-b.xsl", "x" });
            Assertions.assertEquals(1, manager.stringRecords.size());
            Assertions.assertEquals("<S><A><B><C>x</C></B></A></S>", manager.stringRecords.get(0));
            Assertions.assertTrue(stderr.contains("Found 4 possible parses"));
        } catch (Exception ex) {
            fail();
        }
    }

    @Test
    public void ambig1SelectAC() {
        WrappedPrintStream stdout = new WrappedPrintStream();
        WrappedPrintStream stderr = new WrappedPrintStream();
        Main main = new Main(stdout.stream, stderr.stream);
        try {
            OutputManager manager = main.commandLine(new String[] {"-g:src/test/resources/ambig1.ixml", "--function-library:src/test/resources/ambig1-a-c.xsl", "x" });
            Assertions.assertEquals(1, manager.stringRecords.size());
            Assertions.assertEquals("<S><A><C>x</C></A></S>", manager.stringRecords.get(0));
            Assertions.assertTrue(stderr.contains("Found 4 possible parses"));
        } catch (Exception ex) {
            fail();
        }
    }

    @Test
    public void ambig1SelectBC() {
        WrappedPrintStream stdout = new WrappedPrintStream();
        WrappedPrintStream stderr = new WrappedPrintStream();
        Main main = new Main(stdout.stream, stderr.stream);
        try {
            OutputManager manager = main.commandLine(new String[] {"-g:src/test/resources/ambig1.ixml", "--function-library:src/test/resources/ambig1-b-c.xsl", "x" });
            Assertions.assertEquals(1, manager.stringRecords.size());
            Assertions.assertEquals("<S><B><C>x</C></B></S>", manager.stringRecords.get(0));
            Assertions.assertTrue(stderr.contains("Found 4 possible parses"));
        } catch (Exception ex) {
            fail();
        }
    }

    @Test
    public void ambig1SelectD() {
        WrappedPrintStream stdout = new WrappedPrintStream();
        WrappedPrintStream stderr = new WrappedPrintStream();
        Main main = new Main(stdout.stream, stderr.stream);
        try {
            OutputManager manager = main.commandLine(new String[] {"-g:src/test/resources/ambig1.ixml", "--function-library:src/test/resources/ambig1-d.xsl", "x" });
            Assertions.assertEquals(1, manager.stringRecords.size());
            Assertions.assertTrue(manager.stringRecords.get(0).contains("ambiguous"));
            Assertions.assertTrue(stderr.contains("Found 4 possible parses"));
        } catch (Exception ex) {
            fail();
        }
    }

    @Test
    public void ambig3loop10() {
        WrappedPrintStream stdout = new WrappedPrintStream();
        WrappedPrintStream stderr = new WrappedPrintStream();
        Main main = new Main(stdout.stream, stderr.stream);
        try {
            OutputManager manager = main.commandLine(new String[] {"-g:src/test/resources/loop.ixml", "--function-library:src/test/resources/loop10.xsl", "xyz" });
            Assertions.assertEquals(1, manager.stringRecords.size());
            Assertions.assertEquals("<S><X>x</X><A><A><A><A><A><A><A><A><A><A>y</A></A></A></A></A></A></A></A></A></A><Z>z</Z></S>", manager.stringRecords.get(0));
            Assertions.assertTrue(stderr.contains("Found 2 possible parses (of infinitely many)"));
        } catch (Exception ex) {
            fail();
        }
    }

}
