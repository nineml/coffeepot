package org.nineml.coffeepot.utils;

/**
 * Parser options.
 */
public class ParserOptions extends org.nineml.coffeefilter.ParserOptions {
    /** Create the options.*/
    public ParserOptions() {
        super();
    }

    /**
     * The cache directory.
     * <p>If this is not null, it should be the name of a directory on the
     * local filesystem that can be used as a cache location.</p>
     */
    public String cacheDir = null;

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
    public String progressBar = "false";
}
