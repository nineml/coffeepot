package org.nineml.coffeepot.managers;

import net.sf.saxon.s9api.*;
import org.nineml.coffeefilter.InvisibleXmlDocument;
import org.nineml.coffeegrinder.trees.Arborist;
import org.xml.sax.InputSource;

import javax.xml.transform.Source;
import javax.xml.transform.sax.SAXSource;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class GraphOutputManager {
    public static final String logcategory = "CoffeePot";
    private final Configuration config;
    private final Processor processor;

    public GraphOutputManager(Configuration config) {
        this.config = config;
        this.processor = config.processor;
    }

    public void publish(InvisibleXmlDocument doc, Set<Integer> selectedNodes) {
        if (processor == null) {
            return;
        }

        if (config.forest != null) {
            doc.getResult().getForest().serialize(config.forest);
        }

        if (config.graph != null) {
            graphForest(config, doc, selectedNodes);
        }
    }

    private void graphForest(Configuration config, InvisibleXmlDocument doc, Set<Integer> selectedNodes) {
        String stylesheet = "/org/nineml/coffeegrinder/forest2dot.xsl";
        try {
            // Get the graph as XML
            ByteArrayInputStream bais = new ByteArrayInputStream(doc.getResult().getForest().serialize().getBytes(StandardCharsets.UTF_8));
            HashMap<String,String> styleopts = new HashMap<>(config.options.getGraphOptions());

            String comma = "";
            StringBuilder sb = new StringBuilder();
            for (int id : selectedNodes) {
                sb.append(comma);
                sb.append("id").append(id);
                comma = ",";
            }
            styleopts.put("selected-nodes", sb.toString());

            for (String opt : config.graphOptions) {
                final int pos;
                if (opt.contains("=")) {
                    pos = opt.indexOf("=");
                } else if (opt.contains(":")) {
                    pos = opt.indexOf(":");
                } else {
                    pos = -1;
                }

                final String name;
                final String value;
                if (pos >= 0) {
                    name = opt.substring(0, pos).trim();
                    value = opt.substring(pos+1).trim();
                } else {
                    name = null;
                    value = null;
                }

                if (name == null) {
                    config.stderr.println("Unparsable graph option: " + opt);
                } else {
                    styleopts.put(name, value);
                }
            }

            graphXdm(config, new SAXSource(new InputSource(bais)), stylesheet, styleopts);
        } catch (Exception ex) {
            config.stderr.println("Failed to create SVG: " + ex.getMessage());
        }
    }

    private void graphXdm(Configuration config, Source document, String resource, Map<String,String> styleopts) {
        try {
            // Transform the graph into dot
            InputStream stylesheet = getClass().getResourceAsStream(resource);
            if (stylesheet == null) {
                config.stderr.println("Failed to load stylesheet: " + resource);
            } else {
                XsltCompiler compiler = processor.newXsltCompiler();
                compiler.setSchemaAware(false);
                XsltExecutable exec = compiler.compile(new SAXSource(new InputSource(stylesheet)));
                Xslt30Transformer transformer = exec.load30();
                XdmDestination destination = new XdmDestination();

                Map<QName, XsltExecutable.ParameterDetails> paramMap = exec.getGlobalParameters();
                HashMap<QName,XdmAtomicValue> xformOpts = new HashMap<>();
                for (String key : styleopts.keySet()) {
                    XdmAtomicValue value = new XdmAtomicValue(styleopts.get(key));
                    QName name = new QName(key);
                    if (!paramMap.containsKey(name)) {
                        config.stderr.printf("Unrecognized graph option: %s%n", key);
                    }
                    xformOpts.put(name, value);
                }
                transformer.setStylesheetParameters(xformOpts);

                transformer.transform(document, destination);

                // Store the dot file somewhere
                File temp = File.createTempFile("jixp", ".dot");
                PrintWriter dot = new PrintWriter(Files.newOutputStream(temp.toPath()));
                dot.println(destination.getXdmNode().getStringValue());
                dot.close();

                String[] args = new String[] {
                        config.options.getGraphviz(),
                        "-T" + config.graphFormat,
                        temp.getAbsolutePath(),
                        "-o", config.graph};
                Process proc = Runtime.getRuntime().exec(args);
                proc.waitFor();

                if (proc.exitValue() != 0) {
                    StringBuilder sb = new StringBuilder();
                    int ch;
                    while ((ch = proc.getErrorStream().read()) >= 0) {
                        sb.appendCodePoint(ch);
                    }
                    config.options.getLogger().error(logcategory, "Failed to write %s: %s", config.graphFormat.toUpperCase(), sb);
                } else {
                    config.options.getLogger().trace(logcategory, "Wrote %s: %s", config.graphFormat.toUpperCase(), config.graph);
                    if (!temp.delete()) {
                        config.options.getLogger().warn(logcategory, "Failed to delete temporary file: %s", temp.getAbsolutePath());
                        temp.deleteOnExit();
                    }
                }
            }
        } catch (Exception ex) {
            config.stderr.printf("Failed to write %s: %s%n", config.graphFormat.toUpperCase(), ex.getMessage());
        }
    }
}
