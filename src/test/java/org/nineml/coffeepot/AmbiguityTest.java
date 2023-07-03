package org.nineml.coffeepot;

import org.junit.Test;
import org.junit.jupiter.api.Assertions;
import org.nineml.coffeepot.managers.OutputManager;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.fail;

public class AmbiguityTest {
    private static List<String> fourAnswers = null;

    @Test
    public void parseCountAll() {
        CoffeePotTest.WrappedPrintStream stdout = new CoffeePotTest.WrappedPrintStream();
        CoffeePotTest.WrappedPrintStream stderr = new CoffeePotTest.WrappedPrintStream();
        Main main = new Main(stdout.stream, stderr.stream);
        try {
            OutputManager manager = main.commandLine(new String[] {"-g:src/website/xml/examples/ambig01.ixml",
                    "--parse-count:all", "Shimmer" });
            Assertions.assertEquals(2, manager.stringRecords.size());

            String wax = manager.stringRecords.get(0);
            String top = manager.stringRecords.get(1);

            Assertions.assertTrue((wax.contains("<floor-wax>") && top.contains("dessert-topping"))
                    || (wax.contains("<dessert-topping>") && top.contains("floor-wax")));
        } catch (Exception ex) {
            fail();
        }
    }

    @Test
    public void fourParses() {
        CoffeePotTest.WrappedPrintStream stdout = new CoffeePotTest.WrappedPrintStream();
        CoffeePotTest.WrappedPrintStream stderr = new CoffeePotTest.WrappedPrintStream();
        Main main = new Main(stdout.stream, stderr.stream);
        try {
            OutputManager manager = main.commandLine(new String[] {"-g:src/test/resources/four.ixml", "a" });
            Assertions.assertEquals(1, manager.stringRecords.size());
            Assertions.assertTrue(stderr.contains("Found 4 possible parses"));
        } catch (Exception ex) {
            fail();
        }
    }

    private synchronized void getFourAnswers() {
        if (AmbiguityTest.fourAnswers != null) {
            return;
        }

        // I assume they'll be in the same order every time for any given runtime
        CoffeePotTest.WrappedPrintStream stdout = new CoffeePotTest.WrappedPrintStream();
        CoffeePotTest.WrappedPrintStream stderr = new CoffeePotTest.WrappedPrintStream();
        Main main = new Main(stdout.stream, stderr.stream);
        try {
            OutputManager manager = main.commandLine(new String[]{"-g:src/test/resources/four.ixml", "--parse-count:all", "a"});
            Assertions.assertEquals(4, manager.stringRecords.size());
            AmbiguityTest.fourAnswers = new ArrayList<>(manager.stringRecords);
        } catch (Exception ex) {
            fail();
        }
    }

    @Test
    public void fourParses4() {
        getFourAnswers();

        CoffeePotTest.WrappedPrintStream stdout = new CoffeePotTest.WrappedPrintStream();
        CoffeePotTest.WrappedPrintStream stderr = new CoffeePotTest.WrappedPrintStream();
        Main main = new Main(stdout.stream, stderr.stream);
        try {
            OutputManager manager = main.commandLine(new String[] {"-g:src/test/resources/four.ixml", "--parse-count:4", "a" });
            Assertions.assertEquals(4, manager.stringRecords.size());
            for (int pos = 0; pos < manager.stringRecords.size(); pos++) {
                Assertions.assertEquals(AmbiguityTest.fourAnswers.get(pos), manager.stringRecords.get(pos));
            }
        } catch (Exception ex) {
            fail();
        }
    }

    @Test
    public void fourParsesAll() {
        getFourAnswers();

        CoffeePotTest.WrappedPrintStream stdout = new CoffeePotTest.WrappedPrintStream();
        CoffeePotTest.WrappedPrintStream stderr = new CoffeePotTest.WrappedPrintStream();
        Main main = new Main(stdout.stream, stderr.stream);
        try {
            OutputManager manager = main.commandLine(new String[] {"-g:src/test/resources/four.ixml", "--parse-count:all", "a" });
            Assertions.assertEquals(4, manager.stringRecords.size());
            for (int pos = 0; pos < manager.stringRecords.size(); pos++) {
                Assertions.assertEquals(AmbiguityTest.fourAnswers.get(pos), manager.stringRecords.get(pos));
            }
        } catch (Exception ex) {
            fail();
        }
    }

    @Test
    public void fourParsesSecond() {
        getFourAnswers();

        CoffeePotTest.WrappedPrintStream stdout = new CoffeePotTest.WrappedPrintStream();
        CoffeePotTest.WrappedPrintStream stderr = new CoffeePotTest.WrappedPrintStream();
        Main main = new Main(stdout.stream, stderr.stream);
        try {
            OutputManager manager = main.commandLine(new String[] {"-g:src/test/resources/four.ixml", "--parse:2", "a" });
            Assertions.assertEquals(1, manager.stringRecords.size());
            for (int pos = 0; pos < manager.stringRecords.size(); pos++) {
                Assertions.assertEquals(AmbiguityTest.fourAnswers.get(pos+1), manager.stringRecords.get(pos));
            }
        } catch (Exception ex) {
            fail();
        }
    }

    @Test
    public void fourParsesThird() {
        getFourAnswers();

        CoffeePotTest.WrappedPrintStream stdout = new CoffeePotTest.WrappedPrintStream();
        CoffeePotTest.WrappedPrintStream stderr = new CoffeePotTest.WrappedPrintStream();
        Main main = new Main(stdout.stream, stderr.stream);
        try {
            OutputManager manager = main.commandLine(new String[] {"-g:src/test/resources/four.ixml", "--parse:3", "a" });
            Assertions.assertEquals(1, manager.stringRecords.size());
            for (int pos = 0; pos < manager.stringRecords.size(); pos++) {
                Assertions.assertEquals(AmbiguityTest.fourAnswers.get(pos+2), manager.stringRecords.get(pos));
            }
        } catch (Exception ex) {
            fail();
        }
    }

    @Test
    public void fourParsesFourth() {
        getFourAnswers();

        CoffeePotTest.WrappedPrintStream stdout = new CoffeePotTest.WrappedPrintStream();
        CoffeePotTest.WrappedPrintStream stderr = new CoffeePotTest.WrappedPrintStream();
        Main main = new Main(stdout.stream, stderr.stream);
        try {
            OutputManager manager = main.commandLine(new String[] {"-g:src/test/resources/four.ixml", "--parse:4", "a" });
            Assertions.assertEquals(1, manager.stringRecords.size());
            for (int pos = 0; pos < manager.stringRecords.size(); pos++) {
                Assertions.assertEquals(AmbiguityTest.fourAnswers.get(pos+3), manager.stringRecords.get(pos));
            }
        } catch (Exception ex) {
            fail();
        }
    }

    @Test
    public void fourParses2to3() {
        getFourAnswers();

        CoffeePotTest.WrappedPrintStream stdout = new CoffeePotTest.WrappedPrintStream();
        CoffeePotTest.WrappedPrintStream stderr = new CoffeePotTest.WrappedPrintStream();
        Main main = new Main(stdout.stream, stderr.stream);
        try {
            OutputManager manager = main.commandLine(new String[] {"-g:src/test/resources/four.ixml", "--parse-count:2", "--parse:2", "a" });
            Assertions.assertEquals(2, manager.stringRecords.size());
            for (int pos = 0; pos < manager.stringRecords.size(); pos++) {
                Assertions.assertEquals(AmbiguityTest.fourAnswers.get(pos+1), manager.stringRecords.get(pos));
            }
        } catch (Exception ex) {
            fail();
        }
    }

    @Test
    public void fourParsesPublish() {
        getFourAnswers();

        CoffeePotTest.WrappedPrintStream stdout = new CoffeePotTest.WrappedPrintStream();
        CoffeePotTest.WrappedPrintStream stderr = new CoffeePotTest.WrappedPrintStream();
        Main main = new Main(stdout.stream, stderr.stream);
        try {
            OutputManager manager = main.commandLine(new String[] {"-g:src/test/resources/four.ixml", "--parse-count:2", "--parse:2", "a" });
            Assertions.assertEquals(2, manager.stringRecords.size());
            for (int pos = 0; pos < manager.stringRecords.size(); pos++) {
                Assertions.assertEquals(AmbiguityTest.fourAnswers.get(pos+1), manager.stringRecords.get(pos));
            }
            String xml = manager.publication();
            Assertions.assertTrue(xml.contains("firstParse=\"2\""));
            Assertions.assertTrue(xml.contains("totalParses=\"4\""));
            Assertions.assertFalse(xml.contains("infinite"));
        } catch (Exception ex) {
            fail();
        }
    }


}
