package org.nineml.coffeepot;

import org.junit.Test;
import org.junit.jupiter.api.Assertions;
import org.nineml.coffeepot.managers.OutputManager;

import static org.junit.Assert.fail;

public class XPathChoiceTest extends CoffeePotTest {
    @Test
    public void ambig2B() {
        WrappedPrintStream stdout = new WrappedPrintStream();
        WrappedPrintStream stderr = new WrappedPrintStream();
        Main main = new Main(stdout.stream, stderr.stream);
        try {
            OutputManager manager = main.commandLine(new String[] {"-g:src/test/resources/ambig2.ixml", "--describe-ambiguity-with:xml",
                    "--choose:children[symbol[@name='B']]/@id", "x" });
            Assertions.assertEquals(1, manager.stringRecords.size());
            Assertions.assertEquals("<S><B>x</B></S>", manager.stringRecords.get(0));
            Assertions.assertTrue(stderr.contains("Found 3 possible parses"));

            String output = stdout.toString();
            int pos = output.indexOf("C");
            String selected = output.substring(pos);
            pos = selected.indexOf(")");
            selected = selected.substring(0, pos);

            pos = output.indexOf(String.format("<children id=\"%s\"", selected));
            output = output.substring(pos);

            pos = output.indexOf("<symbol");
            output = output.substring(pos);

            Assertions.assertTrue(output.startsWith("<symbol name=\"B\""));
        } catch (Exception ex) {
            fail();
        }
    }
}
