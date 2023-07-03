package org.nineml.coffeepot;

import org.junit.Test;
import org.junit.jupiter.api.Assertions;
import org.nineml.coffeepot.managers.OutputManager;

import static org.junit.Assert.fail;

public class PriorityTest extends CoffeePotTest {
    @Test
    public void defaultPriority() {
        WrappedPrintStream stdout = new WrappedPrintStream();
        WrappedPrintStream stderr = new WrappedPrintStream();
        Main main = new Main(stdout.stream, stderr.stream);
        try {
            OutputManager manager = main.commandLine(new String[] {"-g:src/test/resources/priority.ixml", "due 02/07/2023" });
            Assertions.assertEquals(1, manager.stringRecords.size());
            String result = manager.stringRecords.get(0);
            Assertions.assertTrue(result.contains("<deadline"));
            Assertions.assertFalse(result.contains("ixml:state")); // priority defeats ambiguity
            Assertions.assertTrue(result.contains("<month>02</"));
        } catch (Exception ex) {
            fail();
        }
    }

    @Test
    public void defaultPriorityPedantic() {
        WrappedPrintStream stdout = new WrappedPrintStream();
        WrappedPrintStream stderr = new WrappedPrintStream();
        Main main = new Main(stdout.stream, stderr.stream);
        try {
            OutputManager manager = main.commandLine(new String[] {"-g:src/test/resources/priority.ixml", "--strict-ambiguity", "due 02/07/2023" });
            Assertions.assertEquals(1, manager.stringRecords.size());
            String result = manager.stringRecords.get(0);
            Assertions.assertTrue(result.contains("<deadline"));
            Assertions.assertTrue(result.contains("ixml:state"));
            Assertions.assertTrue(result.contains("ambiguous"));
            Assertions.assertTrue(result.contains("<month>02</"));
        } catch (Exception ex) {
            fail();
        }
    }

    @Test
    public void maxPriority() {
        WrappedPrintStream stdout = new WrappedPrintStream();
        WrappedPrintStream stderr = new WrappedPrintStream();
        Main main = new Main(stdout.stream, stderr.stream);
        try {
            OutputManager manager = main.commandLine(new String[] {"-g:src/test/resources/priority.ixml", "--priority-style:max", "due 02/07/2023" });
            Assertions.assertEquals(1, manager.stringRecords.size());
            String result = manager.stringRecords.get(0);
            Assertions.assertTrue(result.contains("<deadline"));
            Assertions.assertFalse(result.contains("ixml:state")); // priority defeats ambiguity
            Assertions.assertTrue(result.contains("<month>02</"));
        } catch (Exception ex) {
            fail();
        }
    }

    @Test
    public void sumPriority() {
        WrappedPrintStream stdout = new WrappedPrintStream();
        WrappedPrintStream stderr = new WrappedPrintStream();
        Main main = new Main(stdout.stream, stderr.stream);
        try {
            OutputManager manager = main.commandLine(new String[] {"-g:src/test/resources/priority.ixml", "--priority-style:sum", "due 02/07/2023" });
            Assertions.assertEquals(1, manager.stringRecords.size());
            String result = manager.stringRecords.get(0);
            Assertions.assertTrue(result.contains("<deadline"));
            Assertions.assertFalse(result.contains("ixml:state")); // priority defeats ambiguity
            Assertions.assertTrue(result.contains("<month>07</"));
        } catch (Exception ex) {
            fail();
        }
    }

    @Test
    public void ignorePriority() {
        WrappedPrintStream stdout = new WrappedPrintStream();
        WrappedPrintStream stderr = new WrappedPrintStream();
        Main main = new Main(stdout.stream, stderr.stream);
        try {
            OutputManager manager = main.commandLine(new String[] {"-g:src/test/resources/priority.ixml", "--disable-pragma:priority", "due 02/07/2023" });
            Assertions.assertEquals(1, manager.stringRecords.size());
            String result = manager.stringRecords.get(0);
            Assertions.assertTrue(result.contains("<deadline"));
            Assertions.assertTrue(result.contains("ixml:state"));
            Assertions.assertTrue(result.contains("ambiguous"));
            Assertions.assertTrue(result.contains("<month>02</"));
        } catch (Exception ex) {
            fail();
        }
    }

    @Test
    public void pedanticPriority() {
        WrappedPrintStream stdout = new WrappedPrintStream();
        WrappedPrintStream stderr = new WrappedPrintStream();
        Main main = new Main(stdout.stream, stderr.stream);
        try {
            OutputManager manager = main.commandLine(new String[] {"-g:src/test/resources/priority.ixml", "--pedantic", "due 02/07/2023" });
            Assertions.assertEquals(1, manager.stringRecords.size());
            String result = manager.stringRecords.get(0);
            Assertions.assertTrue(result.contains("<deadline"));
            Assertions.assertTrue(result.contains("ixml:state"));
            Assertions.assertTrue(result.contains("ambiguous"));
            Assertions.assertTrue(result.contains("<month>02</"));
        } catch (Exception ex) {
            fail();
        }
    }

}
