package org.nineml.coffeepot;

import org.junit.Test;
import org.junit.jupiter.api.Assertions;
import org.nineml.coffeepot.managers.OutputManager;

import static org.junit.Assert.fail;

public class InvalidGrammarTest {
    @Test
    public void invalid_xml() {
        CoffeePotTest.WrappedPrintStream stdout = new CoffeePotTest.WrappedPrintStream();
        CoffeePotTest.WrappedPrintStream stderr = new CoffeePotTest.WrappedPrintStream();
        Main main = new Main(stdout.stream, stderr.stream);
        try {
            OutputManager manager = main.commandLine(new String[] {"-g:src/test/resources/invalid.ixml", "abx" });
            Assertions.assertEquals(0, manager.stringRecords.size());
            Assertions.assertTrue(stderr.toString().contains("Failed to parse grammar:"));
        } catch (Exception ex) {
            fail();
        }
    }

    @Test
    public void invalid_json_data() {
        CoffeePotTest.WrappedPrintStream stdout = new CoffeePotTest.WrappedPrintStream();
        CoffeePotTest.WrappedPrintStream stderr = new CoffeePotTest.WrappedPrintStream();
        Main main = new Main(stdout.stream, stderr.stream);
        try {
            OutputManager manager = main.commandLine(new String[] {"-g:src/test/resources/invalid.ixml", "--format:json-data", "abx" });
            Assertions.assertEquals(0, manager.stringRecords.size());
            Assertions.assertTrue(stderr.toString().contains("Failed to parse grammar:"));
        } catch (Exception ex) {
            fail();
        }
    }

    @Test
    public void invalid_json_tree() {
        CoffeePotTest.WrappedPrintStream stdout = new CoffeePotTest.WrappedPrintStream();
        CoffeePotTest.WrappedPrintStream stderr = new CoffeePotTest.WrappedPrintStream();
        Main main = new Main(stdout.stream, stderr.stream);
        try {
            OutputManager manager = main.commandLine(new String[] {"-g:src/test/resources/invalid.ixml", "--format:json-tree", "abx" });
            Assertions.assertEquals(0, manager.stringRecords.size());
            Assertions.assertTrue(stderr.toString().contains("Failed to parse grammar:"));
        } catch (Exception ex) {
            fail();
        }
    }

    @Test
    public void invalid_csv() {
        CoffeePotTest.WrappedPrintStream stdout = new CoffeePotTest.WrappedPrintStream();
        CoffeePotTest.WrappedPrintStream stderr = new CoffeePotTest.WrappedPrintStream();
        Main main = new Main(stdout.stream, stderr.stream);
        try {
            OutputManager manager = main.commandLine(new String[] {"-g:src/test/resources/invalid.ixml", "--format:csv", "abx" });
            Assertions.assertEquals(0, manager.stringRecords.size());
            Assertions.assertTrue(stderr.toString().contains("Failed to parse grammar:"));
        } catch (Exception ex) {
            fail();
        }
    }


}
