package org.nineml.coffeepot;

import org.junit.Test;
import org.junit.jupiter.api.Assertions;
import org.nineml.coffeepot.managers.OutputManager;

import static org.junit.Assert.fail;

public class CsvTest {
    @Test
    public void csv1_csv1() {
        Main main = new Main();
        try {
            OutputManager manager = main.commandLine(new String[] {"-g:src/test/resources/csv.ixml", "-i:src/test/resources/csv.txt",
                    "--format:csv"
            });
            Assertions.assertEquals(1, manager.stringRecords.size());
            Assertions.assertEquals("\"year\",\"month\",\"day\"\n" +
                    "\"Year\",\"Month\",\"Day\"\n" +
                    "\"1970\",\"1\",\"1\"\n" +
                    "\"2023\",\"7\",\"2\"\n", manager.stringRecords.get(0));
        } catch (Exception ex) {
            fail();
        }
    }

    @Test
    public void csv1_csv2() {
        Main main = new Main();
        try {
            OutputManager manager = main.commandLine(new String[] {"-g:src/test/resources/csv.ixml", "-i:src/test/resources/csv.txt",
                    "--format:csv", "--omit-csv-headers"
            });
            Assertions.assertEquals(1, manager.stringRecords.size());
            Assertions.assertEquals("\"Year\",\"Month\",\"Day\"\n" +
                    "\"1970\",\"1\",\"1\"\n" +
                    "\"2023\",\"7\",\"2\"\n", manager.stringRecords.get(0));
        } catch (Exception ex) {
            fail();
        }
    }

}
