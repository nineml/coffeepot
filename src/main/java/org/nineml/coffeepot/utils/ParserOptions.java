package org.nineml.coffeepot.utils;

import org.nineml.logging.DefaultLogger;
import org.nineml.logging.Logger;

/**
 * Parser options.
 */
public class ParserOptions extends org.nineml.coffeefilter.ParserOptions {
    private String cacheDir = null;
    private String progressBar = "false";
    private String barCharacters = ".#";
    private boolean trailingNewlineOnOutput = true;

    /**
     * Create the parser options.
     * <p>The initial logger will be a {@link DefaultLogger} initialized with
     * {@link DefaultLogger#readSystemProperties readSystemProperties()}.</p>
     */
    public ParserOptions() {
        super();
    }

    /**
     * Create the parser options with an explicit logger.
     * @param logger the logger.
     */
    public ParserOptions(Logger logger) {
        super(logger);
    }

    /**
     * The cache directory.
     * <p>If this is not null, it should be the name of a directory on the
     * local filesystem that can be used as a cache location.</p>
     */
    public String getCacheDir() {
        return cacheDir;
    };

    /**
     * Set the {@link #getCacheDir()} property.
     * @param dir the cache directory
     */
    public void setCacheDir(String dir) {
        cacheDir = dir;
    }

    /**
     * Display a progress bar?
     * <p>Three values are valid: "false" disables the display of progress indicators,
     * "true" enables the display unconditionally, and "tty" limits the display to
     * outputs that are believed to be TTYs (i.e., consoles).</p>
     * <p>The difference between "true" and "tty" is most readily apparent if
     * {@link System#out} is redirected. If the value is "tty", no progress messages
     * will be written to the output in this case. If the value is "true", the progress
     * messages will be written to the redirected location.</p>
     */
    public String getProgressBar() {
        return progressBar;
    }

    /**
     * Sets the {@link #getProgressBar()} property.
     * <p>The value must be "true", "false", or "tty".</p>
     * @param bar the bar setting
     * @throws IllegalArgumentException if the value is not supported.
     */
    public void setProgressBar(String bar) {
        if ("true".equals(bar) || "false".equals(bar) || "tty".equals(bar)) {
            progressBar = bar;
        } else {
            throw new IllegalArgumentException("Invalid progress bar: " + bar);
        }
    }

    /**
     * Get progress bar characters.
     * <p>The first character is used for an "empty" space in the progress bar.
     * The last character is used for a "full" space. Any characters in between
     * the first and the last are used for even fractions between 0 and 1.</p>
     */
    public String getProgressBarCharacters() {
        return barCharacters;
    }

    /**
     * Sets the {@link #getProgressBarCharacters()} property.
     * @param chars the bar characters
     * @throws IllegalArgumentException if fewer than two characters are provided
     * @throws NullPointerException if chars is null
     */
    public void setProgressBarCharacters(String chars) {
        if (chars == null) {
            throw new NullPointerException("The chars must not be null");
        }
        if (chars.length() < 2) {
            throw new IllegalArgumentException("At least two characters must be provided.");
        }
        barCharacters = chars;
    }

    /**
     * Make sure the output ends with a newline?
     * <p>If this option is true, a newline will be added to the end of the output.
     * </p>
     */
    public boolean getTrailingNewlineOnOutput() {
        return trailingNewlineOnOutput;
    }

    /**
     * Set the {@link #getTrailingNewlineOnOutput()} property.
     * @param newline add a trailing newline?
     */
    public void setTrailingNewlineOnOutput(boolean newline) {
        trailingNewlineOnOutput = newline;
    }
}
