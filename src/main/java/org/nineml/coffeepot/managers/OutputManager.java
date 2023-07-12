package org.nineml.coffeepot.managers;

import net.sf.saxon.s9api.BuildingContentHandler;
import net.sf.saxon.s9api.DocumentBuilder;
import net.sf.saxon.s9api.SaxonApiException;
import net.sf.saxon.s9api.XdmValue;
import org.nineml.coffeefilter.InvisibleXmlDocument;
import org.nineml.coffeefilter.InvisibleXmlParser;
import org.nineml.coffeefilter.trees.*;
import org.nineml.coffeegrinder.trees.NopTreeBuilder;
import org.nineml.coffeepot.trees.XdmDataTree;
import org.nineml.coffeepot.trees.XdmSimpleTree;
import org.nineml.coffeepot.utils.ParserOptions;
import org.nineml.coffeepot.trees.VerboseTreeSelector;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class OutputManager {
    public static final String logcategory = "CoffeePot";
    private boolean parseError = false;
    public final List<XdmValue> records = new ArrayList<>();
    public final List<String> stringRecords = new ArrayList<>();
    private Configuration config = null;
    public boolean xdmResults = false;
    private int returnCode = 0;
    private Exception thrown = null;
    private int firstParse = -1;
    private int parseCount = -1;
    private long totalParses = -1;
    private boolean infiniteParses = false;

    public void configure(Configuration config) {
        if (config == null) {
            throw new NullPointerException("OutputManager config must not be null");
        }
        if (this.config != null) {
            throw new IllegalStateException("Cannot configure OutputManager twice");
        }
        this.config = config;
    }

    private void checkConfig() {
        if (config == null) {
            throw new IllegalStateException("Cannot use an unconfigured OutputManager");
        }
    }

    public boolean isConfigured() {
        return config != null;
    }

    public void setReturnCode(int code) {
        returnCode = code;
    }

    public int getReturnCode() {
        return returnCode;
    }

    public void setException(Exception ex) {
        thrown = ex;
    }

    public Exception getException() {
        return thrown;
    }

    public boolean hadParseError() {
        checkConfig();
        return parseError;
    }

    public void addOutput(InvisibleXmlParser parser, InvisibleXmlDocument doc, String input) {
        checkConfig();

        parseError = parseError || !doc.succeeded();

        if (doc.getNumberOfParses() > 1 || doc.isInfinitelyAmbiguous()) {
            if (!doc.getOptions().isSuppressedState("ambiguous")) {
                if (doc.getNumberOfParses() == 1) {
                    config.stderr.println("Found 1 parse, but the grammar is infinitely ambiguous");
                } else {
                    if (doc.isInfinitelyAmbiguous()) {
                        config.stderr.printf("Found %,d possible parses (of infinitely many).%n", doc.getNumberOfParses());
                    } else {
                        config.stderr.printf("Found %,d possible parses.%n", doc.getNumberOfParses());
                    }
                }
            }
        }

        if (config.parse <= 0) {
            config.options.getLogger().warn(logcategory, "Ignoring absurd parse number: %d", config.parse);
            firstParse = 1;
        } else {
            firstParse = config.parse;
        }

        VerboseTreeSelector treeSelector = new VerboseTreeSelector(config, parser, doc, input);
        for (String expr : config.choose) {
            treeSelector.addExpression(expr);
        }
        if (config.functionLibrary != null) {
            treeSelector.addFunctionLibrary(config.functionLibrary);
        }

        doc.setTreeSelector(treeSelector);
        if (doc.succeeded()) {
            NopTreeBuilder nopBuilder = new NopTreeBuilder();
            for (int pos = 1; pos < firstParse; pos++) {
                if (!doc.getResult().hasMoreTrees()) {
                    config.stderr.printf("There are only %d parses.%n", pos - 1);
                    return;
                }
                doc.getTree(nopBuilder);
            }
        }

        parseCount = 0;
        totalParses = doc.getNumberOfParses();
        infiniteParses = doc.isInfinitelyAmbiguous();

        boolean done = false;
        while (!done) {
            if (xdmResults) {
                getXdmResults(parser, doc);
            } else {
                getStringResults(parser, doc);
            }

            parseCount++;
            if (config.allParses) {
                done = !doc.getResult().hasMoreTrees();
            } else {
                done = parseCount == config.parseCount;
            }
        }

        if (parseCount > totalParses) {
            totalParses = parseCount;
        }
    }

    public void getXdmResults(InvisibleXmlParser parser, InvisibleXmlDocument doc) {
        checkConfig();

        try {
            ParserOptions opts = new ParserOptions(config.options);
            XmlTreeBuilder treeBuilder = null;
            SimpleTreeBuilder simpleBuilder = null;

            switch (config.outputFormat) {
                case XML:
                    DocumentBuilder builder = config.processor.newDocumentBuilder();
                    BuildingContentHandler handler = builder.newBuildingContentHandler();
                    treeBuilder = new XmlTreeBuilder(parser.getIxmlVersion(), config.options, handler);
                    doc.getTree(treeBuilder);
                    records.add(handler.getDocumentNode());
                    break;
                case JSON_DATA:
                    opts.setAssertValidXmlNames(false);
                    opts.setAssertValidXmlCharacters(false);
                    DataTreeBuilder dataBuilder = new DataTreeBuilder(opts);
                    treeBuilder = new XmlTreeBuilder(parser.getIxmlVersion(), opts, dataBuilder);
                    doc.getTree(treeBuilder);
                    XdmDataTree dtree = new XdmDataTree(config, dataBuilder.getTree());
                    records.add(dtree.json());
                    break;
                case JSON_TREE:
                    opts.setAssertValidXmlNames(false);
                    opts.setAssertValidXmlCharacters(false);
                    simpleBuilder = new SimpleTreeBuilder(opts);
                    treeBuilder = new XmlTreeBuilder(parser.getIxmlVersion(), opts, simpleBuilder);
                    doc.getTree(treeBuilder);
                    XdmSimpleTree stree = new XdmSimpleTree(config, simpleBuilder.getTree());
                    records.add(stree.json());
                    break;
                case CSV:
                    opts.setAssertValidXmlNames(false);
                    opts.setAssertValidXmlCharacters(false);
                    DataTreeBuilder csvBuilder = new DataTreeBuilder(opts);
                    treeBuilder = new XmlTreeBuilder(parser.getIxmlVersion(), opts, csvBuilder);
                    doc.getTree(treeBuilder);
                    XdmDataTree csvtree = new XdmDataTree(config, csvBuilder.getTree());
                    records.add(csvtree.csv());
                    break;
                default:
                    throw new RuntimeException("Unexpected output format!?");
            }
        } catch (SaxonApiException ex) {
            throw new RuntimeException(ex);
        }
    }

    public void getStringResults(InvisibleXmlParser parser, InvisibleXmlDocument doc) {
        checkConfig();

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PrintStream output = new PrintStream(baos);

        XmlTreeBuilder treeBuilder;
        DataTreeBuilder dataBuilder;
        SimpleTreeBuilder simpleBuilder;
        DataTree dataTree;
        SimpleTree simpleTree;

        ParserOptions opts = new ParserOptions(config.options);

        switch (config.outputFormat) {
            case XML:
                StringTreeBuilder handler = new StringTreeBuilder(opts, output);
                treeBuilder = new XmlTreeBuilder(parser.getIxmlVersion(), opts, handler);
                doc.getTree(treeBuilder);
                break;
            case JSON_DATA:
                opts.setAssertValidXmlNames(false);
                opts.setAssertValidXmlCharacters(false);
                dataBuilder = new DataTreeBuilder(opts);
                treeBuilder = new XmlTreeBuilder(parser.getIxmlVersion(), opts, dataBuilder);
                doc.getTree(treeBuilder);
                dataTree = dataBuilder.getTree();
                output.print(dataTree.asJSON());
                break;
            case JSON_TREE:
                opts.setAssertValidXmlNames(false);
                opts.setAssertValidXmlCharacters(false);
                simpleBuilder = new SimpleTreeBuilder(opts);
                treeBuilder = new XmlTreeBuilder(parser.getIxmlVersion(), opts, simpleBuilder);
                doc.getTree(treeBuilder);
                simpleTree = simpleBuilder.getTree();
                output.print(simpleTree.asJSON());
                break;
            case CSV:
                opts.setAssertValidXmlNames(false);
                opts.setAssertValidXmlCharacters(false);
                dataBuilder = new DataTreeBuilder(opts);
                treeBuilder = new XmlTreeBuilder(parser.getIxmlVersion(), opts, dataBuilder);
                doc.getTree(treeBuilder);
                dataTree = dataBuilder.getTree();
                List<CsvColumn> columns = dataTree.prepareCsv();
                if (columns == null) {
                    StringTreeBuilder shandler = new StringTreeBuilder(opts, output);
                    treeBuilder = new XmlTreeBuilder(parser.getIxmlVersion(), opts, shandler);
                    doc.getTree(treeBuilder);
                    try {
                        config.stderr.println("Result cannot be serialized as CSV: " + baos.toString("UTF-8"));
                        returnCode = 1;
                    } catch (UnsupportedEncodingException ex) {
                        // This can't happen.
                    }
                    return;
                }
                output.print(dataTree.asCSV(columns, config.omitCsvHeaders));
                break;
            default:
                throw new RuntimeException("Unexpected output format!?");
        }

        try {
            stringRecords.add(baos.toString("UTF-8"));
        } catch (UnsupportedEncodingException ex) {
            // This can't happen.
        }
    }

    public void publish() throws IOException {
        checkConfig();

        if (config.suppressOutput) {
            return;
        }

        PrintStream output = config.stdout;
        if (config.outputFile != null) {
            output = new PrintStream(Files.newOutputStream(Paths.get(config.outputFile)));
        }
        output.print(publication());
    }

    public String publication() {
        checkConfig();

        if (config.suppressOutput) {
            return "";
        }

        if (xdmResults) {
            return publishXdm();
        } else {
            return publishStrings();
        }
    }

    private String publishXdm() {
        throw new IllegalStateException("XDM results output not implemented");
    }

    private String publishStrings() {
        StringBuilder sb = new StringBuilder();

        boolean outputJSON = config.outputFormat == Configuration.OutputFormat.JSON_DATA
                || config.outputFormat == Configuration.OutputFormat.JSON_TREE;

        if (stringRecords.size() > 1) {
            if (outputJSON) {
                sb.append("{\n");
                if (parseError) {
                    sb.append("  \"ixml:state\": \"failed\",\n");
                }
                if (firstParse != 1) {
                    sb.append("  \"firstParse\": ").append(firstParse).append(",\n");
                }
                if (parseCount > 1 || totalParses > 1) {
                    sb.append("  \"parses\": ").append(parseCount).append(",\n");
                    sb.append("  \"totalParses\": ").append(totalParses).append(",\n");
                }
                if (infiniteParses) {
                    sb.append("  \"infinitelyAmbiguous\": true,");
                }
                sb.append("  \"records\": [\n");
            } else {
                sb.append("<records");
                if (firstParse != 1) {
                    sb.append(" firstParse=\"").append(firstParse).append("\"");
                }
                if (parseCount > 1 || totalParses > 1) {
                    sb.append(" parses=\"").append(parseCount).append("\"");
                    sb.append(" totalParses=\"").append(totalParses).append("\"");
                }
                if (infiniteParses) {
                    sb.append(" infinitelyAmbiguous=\"true\"");
                }
                if (parseError) {
                    sb.append(" xmlns:ixml=\"http://invisiblexml.org/NS\" ixml:state=\"failed\"");
                }
                sb.append(">\n");
            }
        }

        for (int pos = 0; pos < stringRecords.size(); pos++) {
            sb.append(stringRecords.get(pos));
            if (pos+1 < stringRecords.size()) {
                if (outputJSON) {
                    sb.append(",");
                }
                sb.append("\n");
            }
        }

        if (stringRecords.size() > 1) {
            if (outputJSON) {
                sb.append("]\n}\n");
            } else {
                sb.append("\n</records>\n");
            }
        }

        return sb.toString();
    }
}
