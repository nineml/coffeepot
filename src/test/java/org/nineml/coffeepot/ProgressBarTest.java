package org.nineml.coffeepot;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.nineml.coffeepot.utils.ParserOptions;
import org.nineml.coffeepot.utils.ProgressBar;

public class ProgressBarTest {
    @Test
    public void emptyBar() {
        ParserOptions options = new ParserOptions();
        options.setProgressBarCharacters("xy");
        ProgressBar progress = new ProgressBar(options);
        String bar = progress.bar(0.0);
        Assertions.assertEquals("xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx", bar);
    }

    @Test
    public void fullBar() {
        ParserOptions options = new ParserOptions();
        options.setProgressBarCharacters("xy");
        ProgressBar progress = new ProgressBar(options);
        String bar = progress.bar(1.0);
        Assertions.assertEquals("yyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyy", bar);
    }

    @Test
    public void halfBar() {
        ParserOptions options = new ParserOptions();
        options.setProgressBarCharacters(".#");
        ProgressBar progress = new ProgressBar(options);
        String bar = progress.bar(0.5);
        Assertions.assertEquals("####################....................", bar);
    }

    @Test
    public void fractions_1_plus_eighths() {
        ParserOptions options = new ParserOptions();
        options.setProgressBarCharacters(".12345678");
        ProgressBar progress = new ProgressBar(options);
        for (int pos = 1; pos < 8; pos++) {
            double frac = (1.0 * pos) / 8.0;
            frac = frac * ProgressBar.barDelta;
            frac = frac + ProgressBar.barDelta;
            String bar = progress.bar(frac);
            String expected = "8" + pos + "......................................";
            Assertions.assertEquals(expected, bar);
        }
    }

    @Test
    public void fractions_20_plus_eighths() {
        ParserOptions options = new ParserOptions();
        options.setProgressBarCharacters(".12345678");
        ProgressBar progress = new ProgressBar(options);
        for (int pos = 1; pos < 8; pos++) {
            double frac = (1.0 * pos) / 8.0;
            frac = frac * ProgressBar.barDelta;
            frac = frac + (20 * ProgressBar.barDelta);
            String bar = progress.bar(frac);
            String expected = "88888888888888888888" + pos + "...................";
            Assertions.assertEquals(expected, bar);
        }
    }

    @Test
    public void fractions_1_plus_quarters() {
        ParserOptions options = new ParserOptions();
        options.setProgressBarCharacters(".1234");
        ProgressBar progress = new ProgressBar(options);
        for (int pos = 1; pos < 4; pos++) {
            double frac = (1.0 * pos) / 4.0;
            frac = frac * ProgressBar.barDelta;
            frac = frac + ProgressBar.barDelta;
            String bar = progress.bar(frac);
            String expected = "4" + pos + "......................................";
            Assertions.assertEquals(expected, bar);
        }
    }

    @Test
    public void fractions_20_plus_quarters() {
        ParserOptions options = new ParserOptions();
        options.setProgressBarCharacters(".1234");
        ProgressBar progress = new ProgressBar(options);
        for (int pos = 1; pos < 4; pos++) {
            double frac = (1.0 * pos) / 4.0;
            frac = frac * ProgressBar.barDelta;
            frac = frac + (20 * ProgressBar.barDelta);
            String bar = progress.bar(frac);
            String expected = "44444444444444444444" + pos + "...................";
            Assertions.assertEquals(expected, bar);
        }
    }

}
