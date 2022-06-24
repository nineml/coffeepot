package org.nineml.coffeepot.utils;

import org.nineml.coffeegrinder.parser.GearleyParser;
import org.nineml.coffeegrinder.parser.ParserType;
import org.nineml.coffeegrinder.parser.ProgressMonitor;
import org.nineml.coffeegrinder.util.StopWatch;

import java.util.Calendar;

import static org.fusesource.jansi.internal.CLibrary.STDOUT_FILENO;
import static org.fusesource.jansi.internal.CLibrary.isatty;

/**
 * A progress monitor for input parsing.
 * <p>This implementation of the CoffeeGrinder {@link ProgressMonitor} provides
 * feedback about the progress of the parse against the input. It can operate in three modes, controlled
 * by the {@link ParserOptions#getProgressBar()} setting. If set to "tty", it will only attempt to
 * provide progress feedback when the output is a terminal.</p>
 */
public class ProgressBar implements ProgressMonitor {
    public static final String logcategory = "ProgressBar";
    /** Output frequency for large parses. */
    public static final int gllFrequency = 10000;
    /** Output frequency for large parses. */
    public static final int slowFrequency = 500;
    /** Output frequency for small parses. */
    public static final int fastFrequency = 100;
    /** Threshold for producing any output at all.
     * <p>If the total size of the input is smaller than this threshold,
     * no progress will be displayed.</p>
     */
    public static final int minSize = fastFrequency;
    /** The threshold between small and large parses. */
    public static final int threshold = 8192;

    public static final int barWidth = 40;
    public static final double barDelta = 1.0 / barWidth;
    public static final boolean istty = isatty(STDOUT_FILENO) == 1;

    private final String emptyCell;
    private final String fullCell;
    private final String[] shades;

    private final ParserOptions options;
    private boolean showProgressBar;
    private boolean showProgress;
    private int totalSize;
    private int frequency;
    public int logUpdateSeconds = 10; // seconds
    public double logUpdatePercent = 0.1; // 10%
    private long logUpdateInterval = logUpdateSeconds * 1000;
    private StopWatch timer = null;
    private long lastUpdateTime = 0;
    private double lastUpdatePercent = 0.0;
    private int highwater = 0;
    private long highwaterTime = 0;

    /**
     * Create a progress bar.
     * @param options The parser options
     */
    public ProgressBar(ParserOptions options) {
        this.options = options;

        if (options.getProgressBarCharacters().length() <= 2) {
            emptyCell = options.getProgressBarCharacters().substring(0, 1);
            fullCell = options.getProgressBarCharacters().substring(1, 2);
            shades = null;
        } else {
            String bar = options.getProgressBarCharacters();
            emptyCell = bar.substring(0,1);
            fullCell = bar.substring(bar.length()-1);
            shades = new String[bar.length() - 1];
            shades[0] = emptyCell;
            for (int pos = 1; pos < bar.length() - 1; pos++) {
                shades[pos] = bar.substring(pos, pos+1);
            }
        }
    }

    /**
     * Progress tracking begins.
     * @param parser the parser
     * @return the update frequency
     */
    @Override
    public int starting(GearleyParser parser, int tokens) {
        lastUpdateTime = Calendar.getInstance().getTimeInMillis();
        lastUpdatePercent = 0.0;
        totalSize = tokens;
        frequency = (tokens > threshold) ? slowFrequency : fastFrequency;
        showProgress = !"false".equals(options.getProgressBar()) && totalSize >= 0;
        showProgressBar = "true".equals(options.getProgressBar()) || ("tty".equals(options.getProgressBar()) && istty);

        logUpdateInterval = 1000L * logUpdateSeconds;

        if (parser.getParserType() == ParserType.GLL) {
            frequency = gllFrequency;
        }

        if (showProgressBar) {
            options.getLogger().debug(logcategory, "Progress bar from 0 to %,d every %,d tokens %s",
                    tokens, frequency, (istty ? "on a TTY" : "(not a TTY)"));
        } else {
            if (parser.getParserType() == ParserType.Earley) {
                options.getLogger().debug(logcategory, "Progress logging every %4.1f%%", logUpdatePercent*100.0);
            } else {
                options.getLogger().debug(logcategory, "Progress logging every %ds", logUpdateSeconds);
            }
        }

        timer = new StopWatch();
        return frequency;
    }

    /**
     * Track progress.
     * <p>On a TTY, this updates an in-place status bar with information about the percentage
     * complete and estimated time to finishing. If {@link System#out} is not a TTY, it simply
     * prints a progress message.</p>
     * @param parser the parser
     * @param tokens the number of tokens (characters) processed so far
     */
    @Override
    public void progress(GearleyParser parser, int tokens) {
        if (!showProgress) {
            return;
        }

        long now = Calendar.getInstance().getTimeInMillis();
        double percent = (1.0*tokens) / totalSize;

        if ((!showProgressBar && now - lastUpdateTime < logUpdateInterval)
                && (percent - lastUpdatePercent < logUpdatePercent)) {
            return;
        }

        double tpms = (1.0*tokens) / timer.duration();
        long remaining = (long) ((totalSize - tokens) / tpms);

        if (showProgressBar) {
            System.out.printf("%5.1f%% (%d t/s) %s %s     \r", percent * 100.0, (long) (tpms * 1000.0), bar(percent), timer.elapsed(remaining));
        } else {
            options.getLogger().info(logcategory, "Parsed %,d tokens (%4.1f%% at %,d t/s)",
                    tokens, percent * 100.0, (long) (tpms * 1000.0));
            lastUpdateTime = now;
            lastUpdatePercent = percent;
        }
    }

    private long lastHighwaterTime = 0;
    private long totalproc = 0;

    /**
     * Track progress.
     * <p>On a TTY, this updates an in-place status bar with information about the percentage
     * complete and estimated time to finishing. If {@link System#out} is not a TTY, it simply
     * prints a progress message.</p>
     * @param parser the parser
     * @param size the number of tokens (characters) processed so far
     */
    @Override
    public void workingSet(GearleyParser parser, int size) {
        if (!showProgress) {
            return;
        }

        long remaining = -1;
        final double percent;
        if (size > highwater) {
            percent = 1.0;
            highwater = size;
            highwaterTime = timer.duration();
        } else {
            percent = (1.0 * size) / highwater;
            double spms = (1.0*(highwater - size)) / (timer.duration() - highwaterTime);
            remaining = (long) (size / spms);
        }

        if (showProgressBar) {
            if (remaining >= 0) {
                System.out.printf("%s %,d/%,d %s                  \r", bar(percent), size, highwater, timer.elapsed(remaining));
            } else {
                System.out.printf("%s %,d/%,d                 \r", bar(percent), size, highwater);
            }
        } else {
            long now = Calendar.getInstance().getTimeInMillis();
            if (now - lastUpdateTime > logUpdateInterval) {
                lastUpdateTime = now;
                if (remaining >= 0) {
                    options.getLogger().info(logcategory, "Remaining %,d/%,d states; %s", size, highwater, timer.elapsed(remaining));
                } else {
                    options.getLogger().info(logcategory, "Remaining %,d/%,d states", size, highwater);
                }
            }
        }
    }

    /**
     * Return a progress bar.
     * <p>This method is really only public for testing.</p>
     * @param percent the percentage
     * @return a progress bar
     * @throws IllegalArgumentException if percent &lt; 0 or percent &gt; 1
     */
    public String bar(double percent) {
        if (percent < 0.0 || percent > 1.0) {
            throw new IllegalArgumentException("Percentage out of range: " + percent);
        }
        StringBuilder sb = new StringBuilder();
        for (int pos = 0; pos < barWidth; pos++) {
            double segment = barDelta * pos;
            if (segment >= percent) {
                sb.append(emptyCell);
            } else if (segment + barDelta > percent) {
                if (shades == null) {
                    sb.append(emptyCell);
                } else {
                    int idx = (int) Math.round(((percent - segment) * shades.length) / barDelta);
                    sb.append(shades[Math.min(idx, shades.length - 1)]);
                }
            } else {
                sb.append(fullCell);
            }
        }
        return sb.toString();
    }

    /**
     * Progress tracking ends.
     * <p>On a TTY, this removes the progress bar and progress details from the screen.</p>
     * @param parser the parser
     */
    @Override
    public void finished(GearleyParser parser) {
        if ((!showProgressBar || (totalSize >= 0 && totalSize < minSize))) {
            return;
        }
        if (istty) {
            System.out.print("                                                                                      \r");
        }
    }
}
