package org.nineml.coffeepot;

import com.beust.jcommander.*;
import net.sf.saxon.s9api.*;
import org.nineml.coffeefilter.InvisibleXml;
import org.nineml.coffeefilter.InvisibleXmlDocument;
import org.nineml.coffeefilter.InvisibleXmlParser;
import org.nineml.coffeefilter.ParserOptions;
import org.nineml.coffeefilter.exceptions.IxmlException;
import org.nineml.coffeefilter.trees.*;
import org.nineml.coffeefilter.utils.URIUtils;
import org.nineml.coffeegrinder.parser.*;
import org.nineml.coffeepot.utils.ParserOptionsLoader;
import org.xml.sax.InputSource;

import javax.xml.transform.sax.SAXSource;
import java.io.*;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * A command-line Invisible XML parser.
 */
class Main {
    public static final String logcategory = "CoffeePot";
    enum OutputFormat { XML, JSON_DATA, JSON_TREE, CSV };
    ParserOptions options;

    public static void main(String[] args) throws IOException {
        Main driver = new Main();
        try {
            int rc = driver.run(args);
            System.exit(rc);
        } catch (Exception ex) {
            System.err.println(ex.getMessage());
            System.exit(2);
        }
    }

    private int run(String[] args) throws IOException {
        CommandMain cmain = new CommandMain();
        JCommander jc = JCommander.newBuilder().addObject(cmain).build();

        jc.setProgramName("coffeepot");

        // If '--' appears in the args, it marks the beginning of input explicitly
        String[] explicitInput = null;
        int argpos = 0;
        while (argpos < args.length) {
            if ("--".equals(args[argpos])) {
                int rest = (args.length - argpos) - 1;
                explicitInput = new String[rest];
                System.arraycopy(args, argpos+1, explicitInput, 0, rest);
                String[] newargs = new String[argpos];
                System.arraycopy(args, 0, newargs, 0, argpos);
                args = newargs;
                break;
            }
            argpos++;
        }

        try {
            jc.parse(args);
            if (cmain.help) {
                usage(jc, true);
            }
        } catch (ParameterException pe) {
            System.err.println(pe.getMessage());
            usage(pe.getJCommander(), false);
        }

        int parseCount = 0;
        boolean allparses = false;

        if (cmain.defaultLogLevel == null) {
            cmain.defaultLogLevel = "warning";
        }

        ParserOptionsLoader loader = new ParserOptionsLoader(cmain.defaultLogLevel);
        options = loader.loadOptions();

        InvisibleXml.setOptions(options);

        options.prettyPrint = options.prettyPrint || cmain.prettyPrint;
        options.showChart = cmain.showChart;

        OutputFormat outputFormat = OutputFormat.XML;
        if (cmain.outputFormat != null) {
            switch (cmain.outputFormat) {
                case "xml":
                    break;
                case "json":
                case "json-data":
                    outputFormat = OutputFormat.JSON_DATA;
                    break;
                case "json-tree":
                case "json-text":
                    outputFormat = OutputFormat.JSON_TREE;
                    break;
                case "csv":
                    outputFormat = OutputFormat.CSV;
                    break;
                default:
                    System.err.println("Unrecognized output format: " + cmain.outputFormat);
                    return 2;
            }
        }

        if (cmain.graphSvg != null) {
            if (options.graphviz == null) {
                options.logger.error(logcategory, "Cannot output SVG; GraphViz is not configured.");
                cmain.graphSvg = null;
            } else {
                try {
                    new Processor(false);
                } catch (Exception ex) {
                    options.logger.error(logcategory, "Cannot output SVG; failed to find Saxon on the classpath.");
                    cmain.graphSvg = null;
                }
            }
        }

        if (cmain.parse != null) {
            if (cmain.parse.equals("all")) {
                allparses = true;
            } else {
                parseCount = Integer.parseInt(cmain.parse);
                if (parseCount < 1) {
                    options.logger.warn(logcategory, "Ignoring absurd parse number: %d", parseCount);
                    parseCount = 0;
                }
            }
        }

        String[] actualInput = null;
        if (cmain.inputText.isEmpty()) {
            if (explicitInput != null) {
                actualInput = explicitInput;
            }
        } else {
            if (explicitInput != null) {
                options.logger.error(logcategory, "Unexpected input: %s", cmain.inputText.get(0));
                System.exit(1);
            } else {
                actualInput = cmain.inputText.toArray(new String[]{});
                for (String input : actualInput) {
                    if (input.startsWith("-")) {
                        options.logger.error(logcategory, "Unexpected option: %s", input);
                        System.exit(1);
                    }
                }
            }
        }

        String input = null;
        if (actualInput != null) {
            StringBuilder sb = new StringBuilder();
            for (String token : actualInput) {
                sb.append(token).append(" ");
            }
            input = sb.toString().trim();
        }

        if (cmain.inputFile == null && input == null && !cmain.showGrammar && cmain.compiledGrammar == null) {
            usage(jc, true);
        }

        if (cmain.inputFile != null && input != null) {
            usage(jc, false, "Input cannot come from both a file and the command line.");
        }

        if (cmain.outputFile != null && cmain.suppressOutput) {
            usage(jc, false, "You cannot simultaneously specify an output file and suppress output.");
        }

        InvisibleXmlParser parser;
        try {
            if (cmain.grammar == null) {
                options.logger.trace(logcategory, "Parsing input with the ixml specification grammar.");
                parser = InvisibleXml.getParser();
            } else  {
                URI grammarURI = URIUtils.resolve(URIUtils.cwd(), cmain.grammar);
                options.logger.trace(logcategory, "Loading grammar: " + grammarURI);
                parser = InvisibleXml.getParser(grammarURI);
                if (cmain.timing) {
                    showTime(parser.getParseTime(), cmain.grammar);
                }
            }
        } catch (IOException ex) {
            System.err.println("Cannot read " + cmain.grammar);
            return 1;
        } catch (IxmlException ex) {
            System.err.println(ex.getMessage());
            return 2;
        }

        parser.setOptions(options);

        if (parser.constructed()) {
            HygieneReport report = parser.getHygieneReport();
        } else {
            InvisibleXmlDocument doc = parser.getFailedParse();
            System.err.printf("Failed to parse grammar: could not match %s at line %d, column %d%n",
                    doc.getEarleyResult().getLastToken(), doc.getLineNumber(), doc.getColumnNumber());

            if (cmain.showChart) {
                System.out.println(doc.getTree());
            }

            return 2;
        }

        if (cmain.showGrammar) {
            System.out.println("The Earley grammar:");
            for (Rule rule : parser.getGrammar().getRules()) {
                System.out.println(rule);
            }
        }

        if (cmain.compiledGrammar != null) {
            if ("-".equals(cmain.compiledGrammar)) {
                System.out.println(parser.getCompiledParser());
            } else {
                PrintStream ps = new PrintStream(new FileOutputStream(cmain.compiledGrammar));
                ps.println(parser.getCompiledParser());
                ps.close();
            }
        }

        if (cmain.inputFile == null && input == null) {
            return 0;
        }

        InvisibleXmlDocument doc;
        if (cmain.inputFile != null) {
            URI inputURI = URIUtils.resolve(URIUtils.cwd(), cmain.inputFile);
            options.logger.trace(logcategory, "Loading input from %s", inputURI);
            doc = parser.parse(inputURI);
        } else {
            options.logger.trace(logcategory, "Input: %s", input);
            doc = parser.parse(input);
        }

        if (cmain.timing) {
            showTime(doc.parseTime());
        }

        boolean suppressAmbiguous = false;
        boolean suppressPrefix = false;
        if (cmain.suppress != null) {
            String[] states = cmain.suppress.split("[\\s,:]+");
            for (String state : states) {
                switch (state) {
                    case "prefix":
                        doc.getOptions().suppressIxmlPrefix = true;
                        suppressPrefix = true;
                        break;
                    case "ambiguous":
                        doc.getOptions().suppressIxmlAmbiguous = true;
                        suppressAmbiguous = true;
                        break;
                    default:
                        System.err.println("Ignoring request to suppress unknown state: "+ state);
                }
            }
        }

        if (cmain.graphXml != null) {
            doc.getEarleyResult().getForest().serialize(cmain.graphXml);
        }

        if (cmain.graphSvg != null) {
            graphForest(doc.getEarleyResult(), options, cmain.graphSvg);
        }

        if (parseCount == 0 && !allparses) {
            if (doc.getNumberOfParses() > 1) {
                if (!suppressAmbiguous) {
                    System.out.println("There are " + doc.getExactNumberOfParses() + " possible parses.");
                }
                if (cmain.describeAmbiguity) {
                    Ambiguity ambiguity = doc.getEarleyResult().getForest().getAmbiguity();
                    if (ambiguity.getInfinitelyAmbiguous()) {
                        System.out.println("Infinite ambiguity:");
                    } else {
                        System.out.println("Ambiguity:");
                    }
                    if (ambiguity.getRoots().size() > 1) {
                        System.out.printf("Graph has %d roots.%n", ambiguity.getRoots().size());
                    }
                    for (ForestNode node : ambiguity.getChoices().keySet()) {
                        System.out.println(node);
                        for (Family family : ambiguity.getChoices().get(node)) {
                            System.out.println("\t" + family);
                        }
                    }
                }
            }
        }

        if (doc.getNumberOfParses() > 0 && parseCount > doc.getNumberOfParses()) {
            System.out.println("There are only " + doc.getExactNumberOfParses() + " possible parses.");
            return 1;
        }

        if (!cmain.suppressOutput) {
            PrintStream output = System.out;
            if (cmain.outputFile != null) {
                output = new PrintStream(new FileOutputStream(cmain.outputFile));
            }

            if (parseCount > 0 || allparses) {
                if (outputFormat == OutputFormat.CSV) {
                    System.err.println("Cannot output multiple parses as CSV");
                    return 1;
                }

                long max = parseCount;
                if (allparses) {
                    max = doc.getNumberOfParses();
                }

                String state = "";
                if (suppressAmbiguous) {
                    state = "ambiguous";
                }
                if (doc.getEarleyResult().prefixSucceeded() && !suppressPrefix) {
                    if ("".equals(state)) {
                        state = "prefix";
                    } else {
                        state += " prefix";
                    }
                }

                doc.getOptions().suppressIxmlPrefix = true;
                doc.getOptions().suppressIxmlAmbiguous = true;

                if (outputFormat == OutputFormat.JSON_DATA || outputFormat == OutputFormat.JSON_TREE) {
                    output.printf("{\"ixml\":{\"parses\":%d, \"totalParses\":%d,%n", max, doc.getExactNumberOfParses());
                    if (!"".equals(state)) {
                        output.printf("\"ixml:state\": \"%s\",%n", state);
                    }
                    output.println("\"trees\":[");
                } else {
                    output.print("<ixml ");
                    if (!"".equals(state)) {
                        output.printf(" xmlns:ixml='http://invisiblexml.org/NS' ixml:state='%s' ", state);
                    }
                    output.printf("parses='%d' totalParses='%s'>%n", max, doc.getExactNumberOfParses());
                }

                for (int pos = 0; pos < max; pos++) {
                    if ((outputFormat == OutputFormat.JSON_DATA || outputFormat == OutputFormat.JSON_TREE)
                         && pos > 0) {
                        output.println(",");
                    }
                    serialize(output, doc, outputFormat);
                    doc.nextTree();
                }

                if (outputFormat == OutputFormat.JSON_DATA || outputFormat == OutputFormat.JSON_TREE) {
                    output.println("]}}");
                } else {
                    output.println("</ixml>");
                }
            } else {
                ParseTree tree = doc.getParseTree();
                if (false && tree == null) {
                    options.logger.error(logcategory, "Parse produced no output.");
                } else {
                    serialize(output, doc, outputFormat);

                    if (cmain.treeXml != null) {
                        doc.getParseTree().serialize(cmain.treeXml);
                    }

                    if (cmain.treeSvg != null) {
                        graphTree(doc.getParseTree(), options, cmain.treeSvg);
                    }
                }
            }
        }

        return 0;
    }

    private void serialize(PrintStream output, InvisibleXmlDocument doc, OutputFormat outputFormat) {
        DataTreeBuilder dataBuilder;
        SimpleTreeBuilder simpleBuilder;
        DataTree dataTree;
        SimpleTree simpleTree;

        ParserOptions nonXmlOptions = new ParserOptions(options);
        nonXmlOptions.assertValidXmlNames = false;

        switch (outputFormat) {
            case CSV:
                dataBuilder = new DataTreeBuilder(nonXmlOptions);
                doc.getTree(dataBuilder);
                dataTree = dataBuilder.getTree();
                List<CsvColumn> columns = dataTree.prepareCsv();
                if (columns == null) {
                    System.err.println("Result cannot be serialized as CSV");
                    return;
                }
                output.print(dataTree.asCSV(columns));
                break;
            case JSON_DATA:
                dataBuilder = new DataTreeBuilder(nonXmlOptions);
                doc.getTree(dataBuilder);
                dataTree = dataBuilder.getTree();
                output.print(dataTree.asJSON());
                break;
            case JSON_TREE:
                simpleBuilder = new SimpleTreeBuilder(nonXmlOptions);
                doc.getTree(simpleBuilder);
                simpleTree = simpleBuilder.getTree();
                output.print(simpleTree.asJSON());
                break;
            default:
                doc.getTree(output);
        }
    }

    private void usage(JCommander jc, boolean help) {
        usage(jc, help, null);
    }

    private void usage(JCommander jc, boolean help, String message) {
        if (message != null) {
            System.err.println("\n" + message);
        }

        if (message == null && jc != null) {
            DefaultUsageFormatter formatter = new DefaultUsageFormatter(jc);
            StringBuilder sb = new StringBuilder();
            formatter.usage(sb);
            System.err.println(sb);
        }

        if (help) {
            System.exit(0);
        } else {
            System.exit(1);
        }
    }

    private void graphForest(EarleyResult result, ParserOptions options, String output) {
        String stylesheet = "/org/nineml/coffeegrinder/forest2dot.xsl";
        try {
            // Get the graph as XML
            Processor processor = new Processor(false);
            ByteArrayInputStream bais = new ByteArrayInputStream(result.getForest().serialize().getBytes(StandardCharsets.UTF_8));
            DocumentBuilder builder = processor.newDocumentBuilder();
            graphXdm(builder.build(new SAXSource(new InputSource(bais))), options, stylesheet, output);
        } catch (Exception ex) {
            System.err.println("Failed to create SVG: " + ex.getMessage());
        }
    }

    private void graphTree(ParseTree result, ParserOptions options, String output) {
        String stylesheet = "/org/nineml/coffeegrinder/tree2dot.xsl";
        try {
            // Get the graph as XML
            Processor processor = new Processor(false);
            ByteArrayInputStream bais = new ByteArrayInputStream(result.serialize().getBytes(StandardCharsets.UTF_8));
            DocumentBuilder builder = processor.newDocumentBuilder();
            graphXdm(builder.build(new SAXSource(new InputSource(bais))), options, stylesheet, output);
        } catch (Exception ex) {
            System.err.println("Failed to create SVG: " + ex.getMessage());
        }
    }

    private void graphXdm(XdmNode document, ParserOptions options, String resource, String output) {
        try {
            Processor processor = document.getProcessor();

            // Transform the graph into dot
            InputStream stylesheet = getClass().getResourceAsStream(resource);
            if (stylesheet == null) {
                System.err.println("Failed to load stylesheet: " + resource);
            } else {
                XsltCompiler compiler = processor.newXsltCompiler();
                compiler.setSchemaAware(false);
                XsltExecutable exec = compiler.compile(new SAXSource(new InputSource(stylesheet)));
                XsltTransformer transformer = exec.load();
                transformer.setInitialContextNode(document);
                XdmDestination destination = new XdmDestination();
                transformer.setDestination(destination);
                transformer.transform();

                // Store the dot file somewhere
                File temp = File.createTempFile("jixp", ".dot");
                temp.deleteOnExit();
                PrintWriter dot = new PrintWriter(new FileOutputStream(temp));
                dot.println(destination.getXdmNode().getStringValue());
                dot.close();

                String[] args = new String[] { options.graphviz, "-Tsvg", temp.getAbsolutePath(), "-o", output};
                Process proc = Runtime.getRuntime().exec(args);
                proc.waitFor();
                if (!temp.delete()) {
                    options.logger.warn(logcategory, "Failed to delete temporary file: %s", temp.getAbsolutePath());
                };

                options.logger.trace(logcategory, "Wrote SVG: %s", output);
            }
        } catch (Exception ex) {
            System.err.println("Failed to write SVG: " + ex.getMessage());
        }
    }

    private void showTime(Long time) {
        showTime(time, "input");
    }

    private void showTime(Long time, String filename) {
        String prefix = "Parsed in ";
        if (filename != null) {
            prefix = "Parsed " + filename + " in ";
        }
        if (time > 1000) {
            System.out.println(prefix + time / 1000 + "s");
        } else {
            System.out.println(prefix + time  + "ms");
        }
    }

    @Parameters(separators = ":", commandDescription = "IxmlParser options")
    private static class CommandMain {
        @Parameter(names = {"-help", "-h", "--help"}, help = true, description = "Display help")
        public boolean help = false;

        @Parameter(names = {"-g", "--grammar"}, description = "The input grammar")
        public String grammar = null;

        @Parameter(names = {"--graph-xml"}, description = "Output an XML description of the forest")
        public String graphXml = null;

        @Parameter(names = {"-G", "--graph-svg"}, description = "Output an SVG graph of the forest")
        public String graphSvg = null;

        @Parameter(names = {"--tree-xml"}, description = "Output an XML description of the parse tree")
        public String treeXml = null;

        @Parameter(names = {"-T", "--tree-svg"}, description = "Output an SVG graph of the parse tree")
        public String treeSvg = null;

        @Parameter(names = {"-i", "--input"}, description = "The input")
        public String inputFile = null;

        @Parameter(names = {"-o", "--output"}, description = "The output, or stdout")
        public String outputFile = null;

        @Parameter(names = {"--no-output"}, description = "Don't print the output")
        public boolean suppressOutput = false;

        @Parameter(names = {"-c", "--compiled-grammar"}, description = "Save the compiled grammar")
        public String compiledGrammar = null;

        @Parameter(names = {"-t", "--time"}, description = "Display timing information")
        public boolean timing = false;

        @Parameter(names = {"-p", "--parse"}, description = "Select a parse, or all parses")
        public String parse = null;

        @Parameter(names = {"-pp", "--pretty-print"}, description = "Pretty-print (indent) the output")
        public boolean prettyPrint = false;

        @Parameter(names = {"--log"}, description = "Default log level (silent, error, warning, info, debug, trace)")
        public String defaultLogLevel = null;

        @Parameter(names = {"-D", "--describe-ambiguity"}, description = "Describe why a parse is ambiguous")
        public boolean describeAmbiguity = false;

        @Parameter(names = {"--show-grammar"}, description = "Show the underlying Earley grammar")
        public boolean showGrammar = false;

        @Parameter(names = {"--show-chart"}, description = "Show the underlying Earley chart")
        public boolean showChart = false;

        @Parameter(names = {"--format"}, description = "Output format (xml, json-data, json-tree, or csv")
        public String outputFormat = null;

        @Parameter(names = {"--suppress"}, description = "States to ignore in the output")
        public String suppress = null;

        @Parameter(description = "The input")
        public List<String> inputText = new ArrayList<>();
    }
}
