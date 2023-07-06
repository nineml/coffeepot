package org.nineml.coffeepot.utils;

import org.nineml.logging.DefaultLogger;
import org.nineml.logging.Logger;

import java.util.HashMap;
import java.util.Map;

/**
 * Parser options.
 */
public class ParserOptions extends org.nineml.coffeefilter.ParserOptions {
    private String progressBar = "false";
    private String barCharacters = ".#";
    private boolean trailingNewlineOnOutput = true;
    private boolean asciiOnly = false;
    private final HashMap<String,String> graphOptions;

    /**
     * Create the parser options.
     * <p>The initial logger will be a {@link DefaultLogger} initialized with
     * {@link DefaultLogger#readSystemProperties readSystemProperties()}.</p>
     */
    public ParserOptions() {
        super();
        graphOptions = new HashMap<>();
    }

    /**
     * Create the parser options with an explicit logger.
     * @param logger the logger.
     */
    public ParserOptions(Logger logger) {
        super(logger);
        graphOptions = new HashMap<>();
    }

    public ParserOptions(ParserOptions copy) {
        super(copy);
        progressBar = copy.progressBar;
        barCharacters = copy.barCharacters;
        trailingNewlineOnOutput = copy.trailingNewlineOnOutput;
        asciiOnly = copy.asciiOnly;
        graphOptions = new HashMap<>(copy.graphOptions);
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
     * @return the progress bar display setting.
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
     * @return the characters used to create the status bar.
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
     * @return the trailing newline setting.
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

    /**
     * Use only ASCII characters in output?
     * <p>If this option is true, punctuation in output like the ambiguity description
     * will include only ASCII characters. (Note: this has no effect on the output of the
     * parse.)
     * </p>
     * @return the ASCII only setting.
     */
    public boolean getAsciiOnly() {
        return asciiOnly;
    }

    /**
     * Set the {@link #getAsciiOnly()} ()} property.
     * @param ascii only ASCII output?
     */
    public void setAsciiOnly(boolean ascii) {
        asciiOnly = ascii;
    }

    /**
     * Get the default graph options.
     *
     * @return the options.
     */
    public Map<String,String> getGraphOptions() {
        return new HashMap<>(graphOptions);
    }

    /**
     * Add a graph option.
     * @param name the option (parameter) name
     * @param value the option value
     * @throws NullPointerException if either name or value is null.
     */
    public void addGraphOption(String name, String value) {
        if (name == null) {
            throw new NullPointerException("Name must not be null.");
        }
        if (value == null) {
            throw new NullPointerException("Value must not be null.");
        }
        graphOptions.put(name, value);
    }

    /**
     * Remove a graph option.
     * @param name the option (parameter) name
     */
    public void removeGraphOption(String name) {
        if (name != null) {
            graphOptions.remove(name);
        }
    }

    public void logOptions() {
        String cat = "CoffeePotOptions";
        Logger logger = getLogger();

        logger.error(cat, "Parser type: %s", getParserType());
        logger.error(cat, "Return chart: %s", getReturnChart());
        logger.error(cat, "Prefix parsing: %s", getPrefixParsing());
        logger.error(cat, "Progress monitor: %s", getProgressMonitor());
        logger.error(cat, "Ignore trailing whitespace: %s", getIgnoreTrailingWhitespace());
        logger.error(cat, "Allow undefined symbols: %s", getAllowUndefinedSymbols());
        logger.error(cat, "Allow unreachable symbols: %s", getAllowUnreachableSymbols());
        logger.error(cat, "Allow unproductive symbols: %s", getAllowUnproductiveSymbols());
        logger.error(cat, "Allow multiple definitions: %s", getAllowMultipleDefinitions());
        logger.error(cat, "Pretty print: %s", getPrettyPrint());
        logger.error(cat, "Show chart: %s", getShowChart());
        logger.error(cat, "Assert valid XML names: %s", getAssertValidXmlNames());
        logger.error(cat, "Assert Valid XML characters: %s", getAssertValidXmlCharacters());
        logger.error(cat, "Pedantic: %s", getPedantic());
        logger.error(cat, "Show marks: %s", getShowMarks());
        logger.error(cat, "Show BNF nonterminals: %s", getShowBnfNonterminals());
        logger.error(cat, "Ignore BOM: %s", getIgnoreBOM());
        logger.error(cat, "Graphviz: %s", getGraphviz());
        logger.error(cat, "Rule rewriter: %s", getRuleRewriter());
        logger.error(cat, "ASCII only: %s", getAsciiOnly());
        logger.error(cat, "Strict ambiguity: %s", getStrictAmbiguity());
    }
}
