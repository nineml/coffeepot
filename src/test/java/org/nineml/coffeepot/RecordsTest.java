package org.nineml.coffeepot;

import org.junit.Test;
import org.junit.jupiter.api.Assertions;
import org.nineml.coffeepot.managers.OutputManager;

import static org.junit.Assert.fail;

public class RecordsTest extends CoffeePotTest {
    @Test
    public void recordsTest1_xml_with_timing() {
        WrappedPrintStream stdout = new WrappedPrintStream();
        WrappedPrintStream stderr = new WrappedPrintStream();
        Main main = new Main(stdout.stream, stderr.stream);
        try {
            OutputManager manager = main.commandLine(new String[] {"-g:src/test/resources/records.ixml", "-i:src/test/resources/records.txt",
                    "--records", "--time"
            });
            Assertions.assertEquals(3, manager.stringRecords.size());
            Assertions.assertEquals("<records>\n" +
                    "<line><d>1</d><text>First line</text></line>\n" +
                    "<line><d>2</d><text>Second line</text></line>\n" +
                    "<line><d>3</d><text>Third line</text></line>\n" +
                    "</records>\n", manager.publication());

            Assertions.assertEquals("", stdout.toString().trim());
            Assertions.assertTrue(stderr.toString().contains("Parsed src/"));
            Assertions.assertFalse(stderr.toString().contains("Parsed record 1"));
        } catch (Exception ex) {
            fail();
        }
    }

    @Test
    public void recordsTest1_xml_with_record_timing() {
        WrappedPrintStream stdout = new WrappedPrintStream();
        WrappedPrintStream stderr = new WrappedPrintStream();
        Main main = new Main(stdout.stream, stderr.stream);
        try {
            OutputManager manager = main.commandLine(new String[] {"-g:src/test/resources/records.ixml", "-i:src/test/resources/records.txt",
                    "--records", "--time-records", "-t"
            });
            Assertions.assertEquals(3, manager.stringRecords.size());
            Assertions.assertEquals("<records>\n" +
                    "<line><d>1</d><text>First line</text></line>\n" +
                    "<line><d>2</d><text>Second line</text></line>\n" +
                    "<line><d>3</d><text>Third line</text></line>\n" +
                    "</records>\n", manager.publication());

            Assertions.assertEquals("", stdout.toString().trim());
            Assertions.assertTrue(stderr.toString().contains("Parsed src/"));
            Assertions.assertTrue(stderr.toString().contains("Parsed record 1"));
            Assertions.assertTrue(stderr.toString().contains("Parsed record 2"));
            Assertions.assertTrue(stderr.toString().contains("Parsed record 3"));
            Assertions.assertFalse(stderr.toString().contains("Parsed record 0"));
            Assertions.assertFalse(stderr.toString().contains("Parsed record 4"));
            Assertions.assertTrue(stderr.toString().contains("Parsed all input"));
        } catch (Exception ex) {
            fail();
        }
    }

    @Test
    public void recordsTest1_xml_without_timing() {
        WrappedPrintStream stdout = new WrappedPrintStream();
        WrappedPrintStream stderr = new WrappedPrintStream();
        Main main = new Main(stdout.stream, stderr.stream);
        try {
            OutputManager manager = main.commandLine(new String[] {"-g:src/test/resources/records.ixml", "-i:src/test/resources/records.txt",
                    "--records"
            });
            Assertions.assertEquals(3, manager.stringRecords.size());
            Assertions.assertEquals("<records>\n" +
                    "<line><d>1</d><text>First line</text></line>\n" +
                    "<line><d>2</d><text>Second line</text></line>\n" +
                    "<line><d>3</d><text>Third line</text></line>\n" +
                    "</records>\n", manager.publication());

            Assertions.assertEquals("", stdout.toString().trim());
            Assertions.assertFalse(stderr.toString().contains("Parsed src/"));
            Assertions.assertFalse(stderr.toString().contains("Parsed record 1"));
        } catch (Exception ex) {
            fail();
        }
    }

    @Test
    public void recordsTest1_json_data() {
        Main main = new Main();
        try {
            OutputManager manager = main.commandLine(new String[] {"-g:src/test/resources/records.ixml", "-i:src/test/resources/records.txt",
                    "--records", "--format:json-data"
            });
            Assertions.assertEquals(3, manager.stringRecords.size());
            Assertions.assertEquals("{\n" +
                    "  \"records\": [\n" +
                    "{\"line\":{\"d\":1,\"text\":\"First line\"}},\n" +
                    "{\"line\":{\"d\":2,\"text\":\"Second line\"}},\n" +
                    "{\"line\":{\"d\":3,\"text\":\"Third line\"}}]\n" +
                    "}\n", manager.publication());
        } catch (Exception ex) {
            fail();
        }
    }

    @Test
    public void recordsTest1_json_tree() {
        Main main = new Main();
        try {
            OutputManager manager = main.commandLine(new String[] {"-g:src/test/resources/records.ixml", "-i:src/test/resources/records.txt",
                    "--records", "--format:json-tree"
            });
            Assertions.assertEquals(3, manager.stringRecords.size());
            Assertions.assertEquals("{\n" +
                    "  \"records\": [\n" +
                    "{\"content\":{\"name\":\"line\",\"content\":[{\"name\":\"d\",\"content\":1},{\"name\":\"text\",\"content\":\"First line\"}]}},\n" +
                    "{\"content\":{\"name\":\"line\",\"content\":[{\"name\":\"d\",\"content\":2},{\"name\":\"text\",\"content\":\"Second line\"}]}},\n" +
                    "{\"content\":{\"name\":\"line\",\"content\":[{\"name\":\"d\",\"content\":3},{\"name\":\"text\",\"content\":\"Third line\"}]}}]\n" +
                    "}\n", manager.publication());
        } catch (Exception ex) {
            fail();
        }
    }

    @Test
    public void recordsTest1_csv() {
        WrappedPrintStream stdout = new WrappedPrintStream();
        WrappedPrintStream stderr = new WrappedPrintStream();
        Main main = new Main(stdout.stream, stderr.stream);
        try {
            OutputManager manager = main.commandLine(new String[] {"-g:src/test/resources/records.ixml", "-i:src/test/resources/records.txt",
                    "--records", "--format:csv"
            });
            Assertions.assertEquals(0, manager.stringRecords.size());
            Assertions.assertTrue(stderr.toString().contains("Result cannot be serialized as CSV:"));
        } catch (Exception ex) {
            fail();
        }
    }


}
