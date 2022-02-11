package org.nineml.coffeepot.utils;

import org.nineml.coffeefilter.ParserOptions;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Properties;

public class ParserOptionsLoader {
    private Properties prop = null;

    public ParserOptions loadOptions() {
        ParserOptions options = new ParserOptions();
        String name = "nineml.properties";
        try {
            InputStream stream = getClass().getResourceAsStream(name);
            if (stream != null) {
                prop = new Properties();
                prop.load(stream);

                options.verbose = "true".equals(getProperty("verbose", "false"));
                options.prettyPrint = "true".equals(getProperty("pretty-print", "false"));
                options.ignoreTrailingWhitespace = "true".equals(getProperty("ignore-trailing-whitespace", "false"));
                options.graphviz = getProperty("graphviz", null);
/*
            } else {
                System.err.println("Failed to find nineml.properties on classpath");
                ClassLoader loader = ClassLoader.getSystemClassLoader();
                URL[] urls = ((URLClassLoader) loader).getURLs();
                for (URL url : urls) {
                    System.err.println("\t" + url);
                }
                options.graphviz = "/usr/local/bin/dot";
 */
            }
        } catch (IOException ex) {
            // nevermind
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
