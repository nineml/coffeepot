package org.nineml.coffeepot.utils;

import org.nineml.coffeefilter.InvisibleXml;
import org.nineml.logging.DefaultLogger;
import org.nineml.logging.Logger;

import java.io.PrintStream;
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

    public void showOptions(PrintStream stderr) {
        Logger logger = getLogger();

        show(stderr, logger, "Parser type: %s", getParserType());
        show(stderr, logger, "Return chart: %s", getReturnChart());
        show(stderr, logger, "Prefix parsing: %s", getPrefixParsing());
        show(stderr, logger, "Progress monitor characters: %s", getProgressBarCharacters());
        show(stderr, logger, "Ignore trailing whitespace: %s", getIgnoreTrailingWhitespace());
        show(stderr, logger, "Allow undefined symbols: %s", getAllowUndefinedSymbols());
        show(stderr, logger, "Allow unreachable symbols: %s", getAllowUnreachableSymbols());
        show(stderr, logger, "Allow unproductive symbols: %s", getAllowUnproductiveSymbols());
        show(stderr, logger, "Allow multiple definitions: %s", getAllowMultipleDefinitions());
        show(stderr, logger, "Pretty print: %s", getPrettyPrint());
        show(stderr, logger, "Show chart: %s", getShowChart());
        show(stderr, logger, "Assert valid XML names: %s", getAssertValidXmlNames());
        show(stderr, logger, "Assert Valid XML characters: %s", getAssertValidXmlCharacters());
        show(stderr, logger, "Pedantic: %s", getPedantic());
        show(stderr, logger, "Show marks: %s", getShowMarks());
        show(stderr, logger, "Show BNF nonterminals: %s", getShowBnfNonterminals());
        show(stderr, logger, "Ignore BOM: %s", getIgnoreBOM());
        show(stderr, logger, "Graphviz: %s", getGraphviz());
        show(stderr, logger, "Rule rewriter: %s", getRuleRewriter());
        show(stderr, logger, "ASCII only: %s", getAsciiOnly());
        show(stderr, logger, "Strict ambiguity: %s", getStrictAmbiguity());
        show(stderr, logger, "Trailing newline on output: %s", getTrailingNewlineOnOutput());
        show(stderr, logger, "Priority style: %s", getPriorityStyle());

        if (getGraphOptions().isEmpty()) {
            show(stderr, logger, "Graph options: null");
        } else {
            StringBuilder sb = new StringBuilder();
            boolean first = true;
            for (String key : getGraphOptions().keySet()) {
                if (!first) {
                    sb.append(", ");
                }
                sb.append(key).append("=").append(getGraphOptions().get(key));
                first = false;
            }
            show(stderr, logger, "Graph options: %s", sb.toString());
        }

        for (String state : InvisibleXml.knownStates()) {
            show(stderr, logger, "Suppressed state: %s: %s", state, isSuppressedState(state));
        }

        for (String pname : InvisibleXml.knownPragmas()) {
            show(stderr, logger, "Pragma disabled: %s: %s", pname, pragmaDisabled(pname));
        }


    }
    
    private void show(PrintStream stderr, Logger logger, String format, Object... value) {
        stderr.printf(format, value);
        stderr.println();
        logger.info("CoffeePotOptions", format, value);
    }
    
}
