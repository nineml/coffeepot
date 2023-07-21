package org.nineml.coffeepot.utils;

import org.nineml.logging.Logger;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;

/**
 * Load parser options from a properties file.
 * <p>The loader looks for the property file in two or three places. First, it looks for
 * <code>.nineml.properties</code> in the users home directory. If it doesn't find one there,
 * it looks for the properties file <code>nineml.properties</code> (no leading ".") on the class path.</p>
 * <p>(In the special case where the program is being run with <code>java -jar coffeepot...</code>,
 * the loader will also look for <code>nineml.properties</code> (no leading "."), in the current
 * directory.)</p>
 * <p>To see which file was loaded, enable "debug" level logging for "CoffeePot" messages.</p>
 */
public class ParserOptionsLoader {
    public static final Set<String> PROPERTY_NAMES = new HashSet<>();
    static {
        PROPERTY_NAMES.add("allow-multiple-definitions");
        PROPERTY_NAMES.add("allow-undefined-symbols");
        PROPERTY_NAMES.add("allow-unproductive-symbols");
        PROPERTY_NAMES.add("allow-unreachable-symbols");
        PROPERTY_NAMES.add("ascii-only");
        PROPERTY_NAMES.add("assert-valid-xml-names");
        PROPERTY_NAMES.add("assert-valid-xml-characters");
        PROPERTY_NAMES.add("default-log-level");
        PROPERTY_NAMES.add("disable-pragmas");
        PROPERTY_NAMES.add("enable-pragmas");
        PROPERTY_NAMES.add("graph-options");
        PROPERTY_NAMES.add("graphviz");
        PROPERTY_NAMES.add("ignore-bom");
        PROPERTY_NAMES.add("ignore-trailing-whitespace");
        PROPERTY_NAMES.add("log-levels");
        PROPERTY_NAMES.add("parser");
        PROPERTY_NAMES.add("pedantic");
        PROPERTY_NAMES.add("prefix-parsing");
        PROPERTY_NAMES.add("pretty-print");
        PROPERTY_NAMES.add("progress-bar");
        PROPERTY_NAMES.add("strict-ambiguity");
        PROPERTY_NAMES.add("suppress-states");
        PROPERTY_NAMES.add("trailing-newline-on-output");
    }

    private static final String propfn = "nineml.properties";
    private final ParserOptions options = new ParserOptions();
    private Properties prop = null;

    /**
     * Load the options, setting the initial logging levels.
     * @param initOptions the initial default options.
     */
    public ParserOptionsLoader(ParserOptions initOptions) {
        Logger ilogger = initOptions.getLogger();
        options.getLogger().setDefaultLogLevel(ilogger.getDefaultLogLevel());
        for (String category : ilogger.getLogCategories()) {
            options.getLogger().setLogLevel(category, ilogger.getLogLevel(category));
        }
    }

    /**
     * Load the options.
     * @param configFile the name of the configuration file.
     * @return the options initialized from the properties file, if one was found.
     */
    public ParserOptions loadOptions(String configFile) {
        try {
            File propfile;
            if (configFile != null) {
                propfile = new File(configFile);
                if (propfile.exists() && propfile.canRead()) {
                    options.getLogger().debug("CoffeePot", "Loading properties: %s", configFile);
                    return loadFromFile(propfile);
                } else {
                    options.getLogger().error("CoffeePot", "Cannot read configuration file: %s", configFile);
                }
            }

            String fs = System.getProperty("file.separator");
            String fn = System.getProperty("user.dir");
            if (fn.endsWith(fs)) {
                fn += "." + propfn;
            } else {
                fn += fs + "." + propfn;
            }

            propfile = new File(fn);
            if (propfile.exists() && propfile.canRead()) {
                options.getLogger().debug("CoffeePot", "Loading properties: %s", fn);
                return loadFromFile(propfile);
            }

            ClassLoader loader = ClassLoader.getSystemClassLoader();
            InputStream stream = loader.getResourceAsStream(propfn);
            if (stream != null) {
                URL resource = loader.getResource(propfn);
                options.getLogger().debug("CoffeePot", "Loading properties: %s", resource);
                return loadFromStream(stream);
            }

            options.getLogger().debug("CoffeePot", "Failed to find nineml.properties");
        } catch (IOException ex) {
            options.getLogger().debug("CoffeePot", "Failed to load nineml.properties: %s", ex.getMessage());
        }

        return options;
    }

    private ParserOptions loadFromFile(File pfile) throws IOException {
        FileInputStream fis = new FileInputStream(pfile);
        return loadFromStream(fis);
    }

    private ParserOptions loadFromStream(InputStream stream) throws IOException {
        prop = new Properties();
        prop.load(stream);

        options.setGraphviz(getProperty("graphviz", null));

        options.setPrettyPrint(getBooleanProperty("pretty-print"));
        options.setIgnoreTrailingWhitespace(getBooleanProperty("ignore-trailing-whitespace"));
        options.setIgnoreBOM(getBooleanProperty("ignore-bom"));
        options.setTrailingNewlineOnOutput(getBooleanProperty("trailing-newline-on-output", true));
        options.setAsciiOnly(getBooleanProperty("ascii-only", false));
        options.setStrictAmbiguity(getBooleanProperty("strict-ambiguity", false));
        options.setPedantic(getBooleanProperty("pedantic"));
        options.setAssertValidXmlNames(getBooleanProperty("assert-valid-xml-names", true));
        options.setAssertValidXmlCharacters(getBooleanProperty("assert-valid-xml-characters", true));
        options.setPrefixParsing(getBooleanProperty("prefix-parsing"));

        options.setAllowMultipleDefinitions(getBooleanProperty("allow-multiple-definitions"));
        options.setAllowUnproductiveSymbols(getBooleanProperty("allow-unproductive-symbols"));
        options.setAllowUnreachableSymbols(getBooleanProperty("allow-unreachable-symbols"));
        options.setAllowUndefinedSymbols(getBooleanProperty("allow-undefined-symbols"));

        String value = getProperty("default-log-level", null);
        if (value != null) {
            options.getLogger().setDefaultLogLevel(value);
        }

        value = getProperty("log-levels", null);
        if (value != null) {
            options.getLogger().setLogLevels(value);
        }

        value = getProperty("parser", "Earley");
        if ("Earley".equalsIgnoreCase(value) || "GLL".equalsIgnoreCase(value)) {
            if ("Earley".equalsIgnoreCase(value)) {
                options.setParserType("Earley");
            } else {
                options.setParserType("GLL");
            }
        } else {
            options.getLogger().warn("CoffeePot", "Unrecognized parser option: %s", value);
        }


        value = getProperty("suppress-states", null);
        if (value != null) {
            for (String state : value.split(",")) {
                if (!"".equals(state.trim())) {
                    options.suppressState(state.trim());
                }
            }
        }

        value = getProperty("disable-pragmas", null);
        if (value != null) {
            for (String pragma : value.split(",")) {
                if (!"".equals(pragma.trim())) {
                    options.disablePragma(pragma.trim());
                }
            }
        }

        value = getProperty("enable-pragmas", null);
        if (value != null) {
            for (String pragma : value.split(",")) {
                if (!"".equals(pragma.trim())) {
                    options.enablePragma(pragma.trim());
                }
            }
        }

        value = getProperty("progress-bar", "false");
        if ("true".equals(value) || "false".equals(value) || "tty".equals(value)) {
            options.setProgressBar(value);
        } else {
            options.getLogger().warn("CoffeePot", "Unrecognized progress-bar option: %s", value);
        }

        value = getProperty("progress-bar-characters", null);
        if (value != null && getProperty("progress-bar-style", null) != null) {
            options.getLogger().warn("CoffeePot", "The progress-bar-characters option supercedes progress-bar-style");
        }

        if (value != null) {
            options.setProgressBarCharacters(value);
        } else {
            value = getProperty("progress-bar-style", "plain");

            if (value != null) {
                switch (value) {
                    case "ascii":
                    case "plain":
                        options.setProgressBarCharacters(".#");
                        break;
                    case "lines":
                        options.setProgressBarCharacters("-=");
                        break;
                    case "blocks":
                        options.setProgressBarCharacters(" ▏▎▍▌▋▊▉█");
                        break;
                    case "shades":
                        options.setProgressBarCharacters(" ░▒▓█");
                        break;
                    default:
                        options.getLogger().warn("CoffeePot", "Unrecognized progress-bar-style: %s", value);
                }
            }
        }

        value = getProperty("graph-options", null);
        if (value != null) {
            for (String pair : value.split(",\\s*")) {
                int pos = pair.indexOf(":");
                if (pos < 0) {
                    pos = pair.indexOf("=");
                }
                if (pos > 0) {
                    String pname = pair.substring(0, pos);
                    String pvalue = pair.substring(pos+1);
                    options.addGraphOption(pname, pvalue);
                } else {
                    options.getLogger().warn("CoffeePot", "Cannot parse graph-option: %s", pair);
                }
            }
        }

        for (String name : prop.stringPropertyNames()) {
            if (!PROPERTY_NAMES.contains(name)) {
                options.getLogger().debug("CoffeePot", "Unknown property name: %s", name);
            }
        }

        return options;
    }

    private boolean getBooleanProperty(String name) {
        return getBooleanProperty(name, false);
    }

    private boolean getBooleanProperty(String name, boolean defaultValue) {
        if (prop == null || name == null) {
            return defaultValue;
        }

        String qualifiedName = "coffeepot." + name;
        String value;
        if (prop.containsKey(qualifiedName)) {
            value = prop.getProperty(qualifiedName);
        } else {
            value = prop.getProperty(name, null);
        }

        if (value == null) {
            return defaultValue;
        }

        if ("true".equals(value) || "1".equals(value) || "yes".equals(value)) {
            return true;
        }

        if ("false".equals(value) || "0".equals(value) || "no".equals(value)) {
            return false;
        }

        options.getLogger().warn("CoffeePot",
                "Unrecognized boolean value '%s', assuming %s", value, defaultValue);
        return defaultValue;
    }

    private String getProperty(String name, String defaultValue) {
        if (prop == null || name == null) {
            return defaultValue;
        }

        String qualifiedName = "coffeepot." + name;
        if (prop.containsKey(qualifiedName)) {
            return prop.getProperty(qualifiedName);
        } else {
            return prop.getProperty(name, defaultValue);
        }
    }
}
