package org.nineml.coffeepot;

import com.beust.jcommander.*;
import net.sf.saxon.s9api.*;
import org.nineml.coffeefilter.InvisibleXml;
import org.nineml.coffeefilter.InvisibleXmlDocument;
import org.nineml.coffeefilter.InvisibleXmlParser;
import org.nineml.coffeefilter.exceptions.IxmlException;
import org.nineml.coffeefilter.trees.*;
import org.nineml.coffeefilter.utils.URIUtils;
import org.nineml.coffeegrinder.parser.*;
import org.nineml.coffeepot.utils.Cache;
import org.nineml.coffeepot.utils.ParserOptions;
import org.nineml.coffeepot.utils.ParserOptionsLoader;
import org.nineml.coffeepot.utils.ProgressBar;
import org.xml.sax.InputSource;

import javax.xml.transform.sax.SAXSource;
import java.io.*;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * A command-line Invisible XML parser.
 */
class Main {
    public static final String logcategory = "CoffeePot";
    enum OutputFormat { XML, JSON_DATA, JSON_TREE, CSV }
    ProgressBar progress = null;
    ParserOptions options;

    public static void main(String[] args) {
        Main driver = new Main();
        try {
            int rc = driver.run(args);
            System.exit(rc);
        } catch (Exception ex) {
            if (driver.progress != null) {
                System.err.println();
            }
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

        // We need temp options to parse the command line log levels parameter
        ParserOptions tempOptions = new ParserOptions();
        if (cmain.logLevels != null) {
            if (cmain.logLevels.contains(":")) {
                tempOptions.getLogger().setLogLevels(cmain.logLevels);
            } else {
                tempOptions.getLogger().setLogLevels("*:" + cmain.logLevels);
            }
        }

        ParserOptionsLoader loader = new ParserOptionsLoader(tempOptions);
        options = loader.loadOptions(cmain.configFile);

        options.setPrettyPrint(options.getPrettyPrint() || cmain.prettyPrint);
        options.setPedantic(options.getPedantic() || cmain.pedantic);
        options.setShowChart(cmain.showChart);
        if (cmain.suppressCache) {
            options.setCacheDir(null);
        }

        if (cmain.version) {
            if (options.getPedantic()) {
                System.out.printf("%s version %s (pedantic).%n", BuildConfig.TITLE, BuildConfig.VERSION);
            } else {
                System.out.printf("%s version %s.%n", BuildConfig.TITLE, BuildConfig.VERSION);
            }
        }

        options.getLogger().trace(logcategory, "%s version %s (published %s, hash: %s%s)",
                BuildConfig.TITLE, BuildConfig.VERSION, BuildConfig.PUB_DATE, BuildConfig.PUB_HASH,
                options.getPedantic() ? "; pedantic" : "");

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
            if (options.getGraphviz() == null) {
                options.getLogger().error(logcategory, "Cannot output SVG; GraphViz is not configured.");
                cmain.graphSvg = null;
            } else {
                try {
                    new Processor(false);
                } catch (Exception ex) {
                    options.getLogger().error(logcategory, "Cannot output SVG; failed to find Saxon on the classpath.");
                    cmain.graphSvg = null;
                }
            }
        }

        int startingParse;
        if (cmain.parse <= 0) {
            options.getLogger().warn(logcategory, "Ignoring absurd parse number: %d", cmain.parse);
            startingParse = 1;
        } else {
            startingParse = cmain.parse;
        }

        long parseCount = 0;
        boolean allparses = false;
        if (cmain.parseCount != null) {
            if (cmain.parseCount.equals("all")) {
                allparses = true;
            } else {
                parseCount = Integer.parseInt(cmain.parseCount);
                if (parseCount < 1) {
                    options.getLogger().warn(logcategory, "Ignoring absurd parse count: %d", parseCount);
                    parseCount = 1;
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
                options.getLogger().error(logcategory, "Unexpected input: %s", cmain.inputText.get(0));
                System.exit(1);
            } else {
                actualInput = cmain.inputText.toArray(new String[]{});
                for (String input : actualInput) {
                    if (input.startsWith("-")) {
                        options.getLogger().error(logcategory, "Unexpected option: %s", input);
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
            if (!cmain.version) {
                usage(jc, true);
            }
            return 0;
        }

        if (cmain.inputFile != null && input != null) {
            usage(jc, false, "Input cannot come from both a file and the command line.");
        }

        if (cmain.outputFile != null && cmain.suppressOutput) {
            usage(jc, false, "You cannot simultaneously specify an output file and suppress output.");
        }

        Cache cache = new Cache(options);

        URI grammarURI;
        URI cachedURI;

        InvisibleXml invisibleXml = new InvisibleXml(options);
        InvisibleXmlParser parser;
        try {
            if (cmain.grammar == null) {
                options.getLogger().trace(logcategory, "Parsing input with the ixml specification grammar.");
                parser = invisibleXml.getParser();
                grammarURI = null;
                cachedURI = null;
            } else  {
                grammarURI = URIUtils.resolve(URIUtils.cwd(), cmain.grammar);
                options.getLogger().trace(logcategory, "Loading grammar: " + grammarURI);

                cachedURI = cache.getCached(grammarURI);
                if (cachedURI != grammarURI) {
                    parser = invisibleXml.getParser(cachedURI);
                    String ver = parser.getGrammar().getMetadataProperty("coffeepot-version");
                    if (!BuildConfig.VERSION.equals(ver)) {
                        // Ignore the cached grammar...
                        parser = invisibleXml.getParser(grammarURI);
                    } else {
                        options.getLogger().trace(logcategory, "Cached grammar: " + cachedURI);
                    }
                } else {
                    parser = invisibleXml.getParser(grammarURI);
                }

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

        if (parser.constructed()) {
            parser.getHygieneReport();
            if (grammarURI != null && grammarURI == cachedURI) { // it *didn't* get read from the cache...
                Grammar grammar = parser.getGrammar();
                grammar.setMetadataProperty("uri", grammarURI.toString());
                grammar.setMetadataProperty("coffeepot-version", BuildConfig.VERSION);

                TimeZone tz = TimeZone.getTimeZone("UTC");
                DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
                df.setTimeZone(tz);
                grammar.setMetadataProperty("date",df.format(new Date()));

                cache.storeCached(grammarURI, parser.getCompiledParser());
            }
        } else {
            if (parser.getException() != null) {
                System.err.printf("Failed to parse grammar: %s", parser.getException().getMessage());
            } else {
                InvisibleXmlDocument doc = parser.getFailedParse();
                System.err.printf("Failed to parse grammar: could not match %s at line %d, column %d%n",
                        doc.getEarleyResult().getLastToken(), doc.getLineNumber(), doc.getColumnNumber());
                if (cmain.showChart) {
                    System.out.println(doc.getTree());
                }
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
            File ifile = new File(cmain.inputFile);
            if (ifile.exists()) {
                progress = new ProgressBar(options, ifile.length());
            } else {
                progress = new ProgressBar(options ,-1);
            }
            parser.getOptions().setProgressMonitor(progress);

            URI inputURI = URIUtils.resolve(URIUtils.cwd(), cmain.inputFile);
            options.getLogger().trace(logcategory, "Loading input: %s", inputURI);
            doc = parser.parse(inputURI);
        } else {
            progress = new ProgressBar(options, input.length());
            parser.getOptions().setProgressMonitor(progress);
            options.getLogger().trace(logcategory, "Input: %s", input);
            doc = parser.parse(input);
        }

        if (cmain.timing) {
            showTime(doc.parseTime());
        }

        if (cmain.suppress != null) {
            String[] states = cmain.suppress.split("[\\s,:]+");
            for (String state : states) {
                doc.getOptions().suppressState(state);
            }
        }

        if (cmain.graphXml != null) {
            doc.getEarleyResult().getForest().serialize(cmain.graphXml);
        }

        if (cmain.graphSvg != null) {
            graphForest(doc.getEarleyResult(), options, cmain.graphSvg);
        }

        Ambiguity ambiguity;
        boolean infambig = false;
        if (doc.succeeded()) {
            ambiguity = doc.getEarleyResult().getForest().getAmbiguity();
            infambig = ambiguity.getInfinitelyAmbiguous();
        } else {
            ambiguity = null;
        }

        if (doc.getNumberOfParses() > 1 || infambig) {
            assert ambiguity != null;

            if (!doc.getOptions().isSuppressedState("ambiguous")) {
                if (doc.getNumberOfParses() == 1) {
                    System.out.println("There is 1 parse, but the grammar is infinitely ambiguous");
                } else {
                    if (infambig) {
                        System.out.printf("There are %s possible parses (of infinitely many).%n", doc.getExactNumberOfParses().toString());
                    } else {
                        System.out.printf("There are %s possible parses.%n", doc.getExactNumberOfParses().toString());
                    }
                }
            }
            if (cmain.describeAmbiguity) {
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

        if (doc.getNumberOfParses() > 0 && startingParse > doc.getNumberOfParses()) {
            System.out.println("There are only " + doc.getExactNumberOfParses() + " possible parses.");
            return 1;
        }

        if (!cmain.suppressOutput) {
            boolean more = true;
            for (int pos = 1; more && pos < startingParse; pos++) {
                more = doc.nextTree();
            }

            PrintStream output = System.out;
            if (cmain.outputFile != null) {
                output = new PrintStream(new FileOutputStream(cmain.outputFile));
            }

            if (parseCount > 1 || allparses) {
                if (outputFormat == OutputFormat.CSV) {
                    System.err.println("Cannot output multiple parses as CSV");
                    return 1;
                }

                long max = parseCount;
                if (allparses) {
                    max = doc.getNumberOfParses();
                }

                String state = "";
                if (!doc.getOptions().isSuppressedState("ambiguous")) {
                    state = "ambiguous";
                }
                if (doc.getEarleyResult().prefixSucceeded() && !doc.getOptions().isSuppressedState("prefix")) {
                    if ("".equals(state)) {
                        state = "prefix";
                    } else {
                        state += " prefix";
                    }
                }

                doc.getOptions().suppressState("prefix");
                doc.getOptions().suppressState("ambiguous");

                if (outputFormat == OutputFormat.JSON_DATA || outputFormat == OutputFormat.JSON_TREE) {
                    output.printf("{\"ixml\":{\"parses\":%d, \"totalParses\":%d,%n", max, doc.getExactNumberOfParses());
                    if (!"".equals(state)) {
                        output.printf("\"ixml:state\": \"%s\",%n", state);
                    }
                    output.println("\"trees\":[");
                } else {
                    output.print("<ixml");
                    if (!"".equals(state)) {
                        output.printf(" xmlns:ixml='http://invisiblexml.org/NS' ixml:state='%s'", state);
                    }
                    output.printf(" parses='%d' totalParses='%s'>%n", max, doc.getExactNumberOfParses());
                }

                for (int pos = 0; more && pos < max; pos++) {
                    if ((outputFormat == OutputFormat.JSON_DATA || outputFormat == OutputFormat.JSON_TREE)
                         && pos > 0) {
                        output.println(",");
                    }
                    serialize(output, doc, outputFormat);
                    more = doc.nextTree();
                }

                if (outputFormat == OutputFormat.JSON_DATA || outputFormat == OutputFormat.JSON_TREE) {
                    output.println("]}}");
                } else {
                    output.println("</ixml>");
                }
            } else {
                serialize(output, doc, outputFormat);
                if (options.getTrailingNewlineOnOutput()) {
                    output.println();
                }

                if (cmain.treeXml != null) {
                    doc.getParseTree().serialize(cmain.treeXml);
                }

                if (cmain.treeSvg != null) {
                    graphTree(doc.getParseTree(), options, cmain.treeSvg);
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

        switch (outputFormat) {
            case CSV:
                options.setAssertValidXmlNames(false);
                dataBuilder = new DataTreeBuilder(options);
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
                options.setAssertValidXmlNames(false);
                dataBuilder = new DataTreeBuilder(options);
                doc.getTree(dataBuilder);
                dataTree = dataBuilder.getTree();
                output.print(dataTree.asJSON());
                break;
            case JSON_TREE:
                options.setAssertValidXmlNames(false);
                simpleBuilder = new SimpleTreeBuilder(options);
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
                PrintWriter dot = new PrintWriter(new FileOutputStream(temp));
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

        @Parameter(names = {"-c", "--config"}, description = "Load a specific configuration file")
        public String configFile = null;

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

        @Parameter(names = {"--compiled-grammar"}, description = "Save the compiled grammar")
        public String compiledGrammar = null;

        @Parameter(names = {"-t", "--time"}, description = "Display timing information")
        public boolean timing = false;

        @Parameter(names = {"-p", "--parse"}, description = "Select a (starting) parse")
        public Integer parse = 1;

        @Parameter(names = {"--parse-count"}, description = "The number of parses to print")
        public String parseCount = "1";

        @Parameter(names = {"-pp", "--pretty-print"}, description = "Pretty-print (indent) the output")
        public boolean prettyPrint = false;

        @Parameter(names = {"--log"}, description = "Specify log levels (silent, error, warning, info, debug, trace)")
        public String logLevels = null;

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

        @Parameter(names = {"--version"}, description = "Show the version")
        public boolean version = false;

        @Parameter(names = {"--pedantic"}, description = "Run in pedantic mode")
        public boolean pedantic = false;

        @Parameter(names = {"--no-cache"}, description = "Ignore the cache")
        public boolean suppressCache = false;

        @Parameter(description = "The input")
        public List<String> inputText = new ArrayList<>();
    }
}
