package org.nineml.coffeepot.utils;

import org.nineml.logging.Logger;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLClassLoader;
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
        PROPERTY_NAMES.add("assert-valid-xml-names");
        PROPERTY_NAMES.add("cache");
        PROPERTY_NAMES.add("default-log-level");
        PROPERTY_NAMES.add("graphviz");
        PROPERTY_NAMES.add("ignore-trailing-whitespace");
        PROPERTY_NAMES.add("log-levels");
        PROPERTY_NAMES.add("pedantic");
        PROPERTY_NAMES.add("pretty-print");
        PROPERTY_NAMES.add("progress-bar");
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
            //System.err.println("FN1:" + fn);
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

            // The 'java -jar ...' case...
            URL[] urls = ((URLClassLoader) loader).getURLs();
            if (urls.length == 1) {
                fn = urls[0].getPath();
                int pos = fn.lastIndexOf("/");
                if (pos >= 0) {
                    fn = fn.substring(0, pos);
                }
                fn = fn + "/" + propfn;

                propfile = new File(fn);
                //System.err.println("FN2:" + fn);
                if (propfile.exists() && propfile.canRead()) {
                    options.getLogger().debug("CoffeePot", "Loading properties: %s", fn);
                    return loadFromFile(propfile);
                }
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

        options.setPrettyPrint("true".equals(getProperty("pretty-print", "false")));
        options.setIgnoreTrailingWhitespace("true".equals(getProperty("ignore-trailing-whitespace", "false")));
        options.setTrailingNewlineOnOutput("true".equals(getProperty("trailing-newline-on-output", "true")));
        options.setPedantic("true".equals(getProperty("pedantic", "false")));
        options.setGraphviz(getProperty("graphviz", null));
        options.setCacheDir(getProperty("cache", null));
        options.setAssertValidXmlNames("true".equals(getProperty("assert-valid-xml-names", "true")));

        options.setAllowMultipleDefinitions("true".equals(getProperty("allow-multiple-definitions", "false")));
        options.setAllowUnproductiveSymbols("true".equals(getProperty("allow-unproductive-symbols", "false")));
        options.setAllowUnreachableSymbols("true".equals(getProperty("allow-unreachable-symbols", "false")));
        options.setAllowUndefinedSymbols("true".equals(getProperty("allow-undefined-symbols", "false")));

        String value = getProperty("default-log-level", null);
        if (value != null) {
            options.getLogger().setDefaultLogLevel(value);
        }

        value = getProperty("log-levels", null);
        if (value != null) {
            options.getLogger().setLogLevels(value);
        }

        value = getProperty("suppress-states", null);
        if (value != null) {
            for (String state : value.split(",")) {
                if (!"".equals(state.trim())) {
                    options.suppressState(state.trim());
                }
            }
        }

        value = getProperty("progress-bar", "false");
        if ("true".equals(value) || "false".equals(value) || "tty".equals(value)) {
            options.setProgressBar(value);
        } else {
            options.getLogger().warn("CoffeePot", "Unrecognized progress-bar option: %s", value);
        }

        for (String name : prop.stringPropertyNames()) {
            if (!PROPERTY_NAMES.contains(name)) {
                options.getLogger().debug("CoffeePot", "Unknown property name: %s", name);
            }
        }

        return options;
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
