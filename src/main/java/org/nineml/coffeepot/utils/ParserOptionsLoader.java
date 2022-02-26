package org.nineml.coffeepot.utils;

import org.nineml.coffeefilter.ParserOptions;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Properties;

public class ParserOptionsLoader {
    private static final String propfn = "nineml.properties";
    private final ParserOptions options = new ParserOptions();
    private Properties prop = null;

    public ParserOptionsLoader(String level) {
        options.logger.setDefaultLogLevel(level);
    }

    public ParserOptions loadOptions() {
        try {
            String fs = System.getProperty("file.separator");
            String fn = System.getProperty("user.dir");
            if (fn.endsWith(fs)) {
                fn += "." + propfn;
            } else {
                fn += fs + "." + propfn;
            }

            File propfile = new File(fn);
            //System.err.println("FN1:" + fn);
            if (propfile.exists() && propfile.canRead()) {
                options.logger.debug("CoffeePot", "Loading properties: %s", fn);
                return loadFromFile(propfile);
            }

            ClassLoader loader = ClassLoader.getSystemClassLoader();
            InputStream stream = loader.getResourceAsStream(propfn);
            if (stream != null) {
                URL resource = loader.getResource(propfn);
                options.logger.debug("CoffeePot", "Loading properties: %s", resource);
                return loadFromStream(stream);
            }

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
                    options.logger.debug("CoffeePot", "Loading properties: %s", fn);
                    return loadFromFile(propfile);
                }
            }

            options.logger.debug("CofeePot", "Failed to find nineml.properties");
        } catch (IOException ex) {
            options.logger.debug("CofeePot", "Failed to load nineml.properties: %s", ex.getMessage());
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

        options.prettyPrint = "true".equals(getProperty("pretty-print", "false"));
        options.ignoreTrailingWhitespace = "true".equals(getProperty("ignore-trailing-whitespace", "false"));
        options.graphviz = getProperty("graphviz", null);

        String value = getProperty("default-log-level", null);
        if (value != null) {
            options.logger.setDefaultLogLevel(value);
        }

        value = getProperty("log-levels", null);
        if (value != null) {
            options.logger.setLogLevels(value);
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
