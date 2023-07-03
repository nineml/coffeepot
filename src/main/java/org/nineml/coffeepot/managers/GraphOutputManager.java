package org.nineml.coffeepot.managers;

import net.sf.saxon.s9api.*;
import org.nineml.coffeefilter.InvisibleXmlDocument;
import org.nineml.coffeegrinder.parser.GearleyResult;
import org.nineml.coffeepot.utils.ParserOptions;
import org.xml.sax.InputSource;

import javax.xml.transform.sax.SAXSource;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;

public class GraphOutputManager {
    public static final String logcategory = "CoffeePot";
    private final Configuration config;
    private final Processor processor;

    public GraphOutputManager(Configuration config) {
        this.config = config;
        this.processor = config.processor;
    }

    public void publish(InvisibleXmlDocument doc) {
        if (processor == null) {
            return;
        }

        if (config.graphXml != null) {
            doc.getResult().getForest().serialize(config.graphXml);
        }

        if (config.graphSvg != null) {
            graphForest(doc.getResult(), config.options, config.graphSvg, doc.getResult().lastSelectedNodes());
        }
    }

    private void graphForest(GearleyResult result, ParserOptions options, String output, Set<Integer> selectedNodes) {
        String stylesheet = "/org/nineml/coffeegrinder/forest2dot.xsl";
        try {
            // Get the graph as XML
            ByteArrayInputStream bais = new ByteArrayInputStream(result.getForest().serialize().getBytes(StandardCharsets.UTF_8));
            DocumentBuilder builder = processor.newDocumentBuilder();
            HashSet<String> knownopts = new HashSet<>();
            HashMap<String,String> styleopts = new HashMap<>();

            String comma = "";
            StringBuilder sb = new StringBuilder();
            for (int id : selectedNodes) {
                sb.append(comma);
                sb.append("id").append(id);
                comma = ",";
            }
            styleopts.put("selected-nodes", sb.toString());

            knownopts.add("alt-edge-color");
            knownopts.add("alt-edge-style");
            knownopts.add("ambiguity-font-color");
            knownopts.add("edge-color");
            knownopts.add("edge-pen-width");
            knownopts.add("edge-style");
            knownopts.add("elide-root");
            knownopts.add("label-color");
            knownopts.add("node-color");
            knownopts.add("node-fill-color");
            knownopts.add("node-font-color");
            knownopts.add("node-font-name");
            knownopts.add("node-pen-width");
            knownopts.add("nonterminal-shape");
            knownopts.add("priority-color");
            knownopts.add("priority-size");
            knownopts.add("rankdir");
            knownopts.add("selected-depth");
            knownopts.add("selected-node-color");
            knownopts.add("selected-node-fill-color");
            knownopts.add("selected-node-font-color");
            knownopts.add("selected-node-font-name");
            knownopts.add("selected-node-pen-width");
            knownopts.add("selected-nodes");
            knownopts.add("selected-root");
            knownopts.add("show-priority");
            knownopts.add("show-states");
            knownopts.add("state-shape");
            knownopts.add("subgraph-color");
            knownopts.add("terminal-shape");

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
                    if (!knownopts.contains(name)) {
                        config.stderr.println("Unrecognized graph option: " + name);
                    }
                    styleopts.put(name, value);
                }
            }

            graphXdm(builder.build(new SAXSource(new InputSource(bais))), options, stylesheet, styleopts, output);
        } catch (Exception ex) {
            config.stderr.println("Failed to create SVG: " + ex.getMessage());
        }
    }

    private void graphXdm(XdmNode document, ParserOptions options, String resource, Map<String,String> styleopts, String output) {
        try {
            // Transform the graph into dot
            InputStream stylesheet = getClass().getResourceAsStream(resource);
            if (stylesheet == null) {
                config.stderr.println("Failed to load stylesheet: " + resource);
            } else {
                XsltCompiler compiler = processor.newXsltCompiler();
                compiler.setSchemaAware(false);
                XsltExecutable exec = compiler.compile(new SAXSource(new InputSource(stylesheet)));
                XsltTransformer transformer = exec.load();
                transformer.setInitialContextNode(document);
                XdmDestination destination = new XdmDestination();
                transformer.setDestination(destination);

                for (String key : styleopts.keySet()) {
                    XdmValue value = new XdmAtomicValue(styleopts.get(key));
                    QName name = new QName(key);
                    transformer.setParameter(name, value);
                }

                transformer.transform();

                // Store the dot file somewhere
                File temp = File.createTempFile("jixp", ".dot");
                PrintWriter dot = new PrintWriter(Files.newOutputStream(temp.toPath()));
                dot.println(destination.getXdmNode().getStringValue());
                dot.close();

                String[] args = new String[] { options.getGraphviz(), "-Tsvg", temp.getAbsolutePath(), "-o", output};
                Process proc = Runtime.getRuntime().exec(args);
                proc.waitFor();

                if (proc.exitValue() != 0) {
                    StringBuilder sb = new StringBuilder();
                    int ch;
                    while ((ch = proc.getErrorStream().read()) >= 0) {
                        sb.appendCodePoint(ch);
                    }
                    options.getLogger().error(logcategory, "Failed to write SVG: %s", sb.toString());
                } else {
                    options.getLogger().trace(logcategory, "Wrote SVG: %s", output);
                    if (!temp.delete()) {
                        options.getLogger().warn(logcategory, "Failed to delete temporary file: %s", temp.getAbsolutePath());
                        temp.deleteOnExit();
                    }
                }
            }
        } catch (Exception ex) {
            config.stderr.println("Failed to write SVG: " + ex.getMessage());
        }
    }
}
