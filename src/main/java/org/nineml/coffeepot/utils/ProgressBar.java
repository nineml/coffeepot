package org.nineml.coffeepot.utils;

import org.nineml.coffeegrinder.parser.EarleyParser;
import org.nineml.coffeegrinder.parser.ProgressMonitor;
import org.nineml.coffeegrinder.util.StopWatch;

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

    private static final int barWidth = 40;
    private static final boolean istty = isatty(STDOUT_FILENO) == 1;
    private final boolean showProgress;
    private final int frequency;
    private final long totalSize;
    private StopWatch timer = null;

    /**
     * Create a progress bar.
     * @param options The parser options
     * @param total The total size of the input
     */
    public ProgressBar(ParserOptions options, long total) {
        totalSize = total;
        frequency = (total > threshold) ? slowFrequency : fastFrequency;

        showProgress = "true".equals(options.getProgressBar()) || ("tty".equals(options.getProgressBar()) && istty);
    }

    /**
     * Progress tracking begins.
     * @param parser the parser
     * @return the update frequency
     */
    @Override
    public int starting(EarleyParser parser) {
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
    public void progress(EarleyParser parser, long tokens) {
        if (!showProgress || (totalSize >= 0 && totalSize < minSize)) {
            return;
        }

        double percent = (1.0*tokens) / totalSize;
        int completed = (int) Math.floor(barWidth * percent);
        double tpms = (1.0*tokens) / timer.duration();
        long remaining = (long) ((totalSize - tokens) / tpms);

        if (istty) {
            StringBuilder sb = new StringBuilder();
            for (int p = 0; p < barWidth; p++) {
                if (p > completed) {
                    sb.append(".");
                } else {
                    sb.append("#");
                }
            }

            System.out.printf("%5.1f%% (%d t/s) %s %s     \r", percent * 100.0, (long) (tpms * 1000.0), sb, timer.elapsed(remaining));
        } else {
            System.out.printf("%5.1f%% (%d t/s) %s%n", percent * 100.0, (long) (tpms * 1000.0), timer.elapsed(remaining));
        }
    }

    /**
     * Progress tracking ends.
     * <p>On a TTY, this removes the progress bar and progress details from the screen.</p>
     * @param parser the parser
     */
    @Override
    public void finished(EarleyParser parser) {
        if ((!showProgress || (totalSize >= 0 && totalSize < minSize))) {
            return;
        }
        if (istty) {
            System.out.print("                                                                      \r");
        }
    }
}
