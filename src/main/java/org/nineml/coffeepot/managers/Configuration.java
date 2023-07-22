package org.nineml.coffeepot.managers;

import com.beust.jcommander.*;
import net.sf.saxon.s9api.Processor;
import org.nineml.coffeepot.BuildConfig;
import org.nineml.coffeepot.exceptions.ConfigurationException;
import org.nineml.coffeepot.utils.ParserOptions;
import org.nineml.coffeepot.utils.ParserOptionsLoader;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;

public class Configuration {
    public static final String logcategory = "CoffeePot";
    public enum OutputFormat { XML, JSON_DATA, JSON_TREE, CSV }
    public final PrintStream stdout;
    public final PrintStream stderr;
    public final Processor processor;

    public final ParserOptions options;
    public final OutputFormat outputFormat;
    public final String outputFile;
    public final String input;
    public final String inputFile;
    public final String grammar;
    public final boolean timing;
    public final boolean timeRecords;
    public final boolean bnf;
    public final boolean analyzeAmbiguity;
    public final boolean showChart;
    public final boolean showGrammar;
    public final String encoding;
    public final String grammarEncoding;
    public final boolean records;
    public final String recordStart;
    public final String recordEnd;
    public final boolean suppressOutput;
    public final boolean unbuffered;
    public final String forest;
    public final String graph;
    public final String graphFormat;
    public final List<String> graphOptions;
    public final int parse;
    public final int parseCount;
    public final boolean allParses;
    public final List<String> choose;
    public final String functionLibrary;
    public final String describeAmbiguityWith;
    public final boolean omitCsvHeaders;
    public final int repeat;

    public Configuration(String[] args) {
        this(System.out, System.err, args);
    }

    public Configuration(PrintStream stdout, PrintStream stderr, String[] args) {
        this.stdout = stdout;
        this.stderr = stderr;

        Processor localProcessor = null;
        try {
            localProcessor = new Processor(true);
        } catch (Exception ex) {
            // nevermind
        }
        processor = localProcessor;

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
            stderr.println(pe.getMessage());
            usage(pe.getJCommander(), false);
        }

        cmain.records |= cmain.recordstart != null || cmain.recordend != null;
        if (cmain.records) {
            if (cmain.recordstart != null && cmain.recordend != null) {
                throw ConfigurationException.configError("You may only specify one of --record-start and --record-end");
            }
            if (cmain.recordstart == null && cmain.recordend == null) {
                cmain.recordend = "\n";
            }
            if (cmain.parse != 1 || !"1".equals(cmain.parseCount)) {
                throw ConfigurationException.configError("You may not specify --parse or --parse-count with --records");
            }
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

        // Make sure we apply the command line log levels to the "real" options
        if (cmain.logLevels != null) {
            if (cmain.logLevels.contains(":")) {
                options.getLogger().setLogLevels(cmain.logLevels);
            } else {
                options.getLogger().setLogLevels("*:" + cmain.logLevels);
            }
        }

        options.setPrettyPrint(options.getPrettyPrint() || cmain.prettyPrint);

        if (options.getPedantic() || cmain.pedantic) {
            options.setPedantic(true);
            options.setAllowMultipleDefinitions(false);
            options.setAllowUndefinedSymbols(false);
            options.setAllowUnproductiveSymbols(false);
            options.setAllowUnreachableSymbols(false);
        } else {
            options.setPedantic(false);
            options.setAllowMultipleDefinitions(true);
            options.setAllowUndefinedSymbols(false);   // this is just too problematic
            options.setAllowUnproductiveSymbols(true);
            options.setAllowUnreachableSymbols(true);
        }

        options.setShowChart(cmain.showChart);
        options.setShowMarks(cmain.showMarks);
        options.setShowBnfNonterminals(cmain.showHiddenNonterminals);
        options.setPriorityStyle(cmain.priorityStyle);
        options.setStrictAmbiguity(cmain.strictAmbiguity);

        if (cmain.gllParser) {
            options.setParserType("GLL");
        }

        if (cmain.earleyParser) {
            if (cmain.gllParser) {
                options.getLogger().error(logcategory, "Only one parser is allowed, using Earley.");
            }
            options.setParserType("Earley");
        }

        if (cmain.suppress != null) {
            String[] states = cmain.suppress.split("[\\s,:]+");
            for (String state : states) {
                options.suppressState(state);
            }
        }

        if (cmain.describeAmbiguityWith == null) {
            if (cmain.describeAmbiguity) {
                describeAmbiguityWith = "text";
            } else {
                describeAmbiguityWith = "none";
            }
        } else {
            if ("apixml".equals(cmain.describeAmbiguityWith)) {
                cmain.describeAmbiguityWith = "api-xml";
            }
            if ("xml".equals(cmain.describeAmbiguityWith) || "api-xml".equals(cmain.describeAmbiguityWith)) {
                if (processor == null) {
                    options.getLogger().error(logcategory, "Cannot describe ambiguity with XML, no Saxon processor available");
                    describeAmbiguityWith = "none";
                } else {
                    describeAmbiguityWith = cmain.describeAmbiguityWith;
                }
            } else {
                if (!"text".equals(cmain.describeAmbiguityWith)) {
                    options.getLogger().error(logcategory, "Ignoring unrecognized ambiguity description type: " + cmain.describeAmbiguityWith);
                    describeAmbiguityWith = "none";
                } else {
                    describeAmbiguityWith = "text";
                }
            }
        }

        if (cmain.functionLibrary != null && processor == null) {
            options.getLogger().error(logcategory, "Cannot resolve ambiguity with a function library, no Saxon processor available");
            cmain.functionLibrary = null;
        }

        if (cmain.choose.size() > 0 && processor == null) {
            options.getLogger().error(logcategory, "Cannot resolve ambiguity with a XPath expressions, no Saxon processor available");
            cmain.choose.clear();
        }

        if (cmain.version) {
            if (options.getPedantic()) {
                stderr.printf("%s version %s (pedantic).%n", BuildConfig.TITLE, BuildConfig.VERSION);
            } else {
                stderr.printf("%s version %s.%n", BuildConfig.TITLE, BuildConfig.VERSION);
            }
        }

        options.getLogger().trace(logcategory, "%s version %s (published %s, hash: %s%s)",
                BuildConfig.TITLE, BuildConfig.VERSION, BuildConfig.PUB_DATE, BuildConfig.PUB_HASH,
                options.getPedantic() ? "; pedantic" : "");

        if (cmain.outputFormat == null) {
            outputFormat = OutputFormat.XML;
        } else {
            switch (cmain.outputFormat) {
                case "xml":
                    outputFormat = OutputFormat.XML;
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
                    throw ConfigurationException.configError("Unrecognized output format: " + cmain.outputFormat);
            }
        }

        if (outputFormat == OutputFormat.CSV) {
            omitCsvHeaders = cmain.omitCsvHeaders;
        } else {
            omitCsvHeaders = false;
        }

        if (cmain.graphXml != null && cmain.forest != null) {
            options.getLogger().error(logcategory, "The --graph-xml option is deprecated, using --forest");
            cmain.graphXml = null;
        } else if (cmain.graphXml != null) {
            options.getLogger().error(logcategory, "The --graph-xml option is deprecated, use --forest");
            cmain.forest = cmain.graphXml;
            cmain.graphXml = null;
        }

        if (cmain.graphSvg != null && cmain.graph != null) {
            options.getLogger().error(logcategory, "The --graph-svg option is deprecated, using --graph");
            cmain.graphSvg = null;
        } else if (cmain.graphSvg != null) {
            options.getLogger().error(logcategory, "The --graph-svg option is deprecated, use --graph");
            cmain.graph = cmain.graphSvg;
            cmain.graphSvg = null;
        }

        if (cmain.graph != null) {
            if (options.getGraphviz() == null) {
                options.getLogger().error(logcategory, "Cannot output graph; GraphViz is not configured.");
                cmain.graph = null;
            }
        }

        if (!cmain.graphSvgOptions.isEmpty() && !cmain.graphOptions.isEmpty()) {
            options.getLogger().error(logcategory, "The --graph-svg-option option is deprecated, using --graph-option");
        } else if (!cmain.graphSvgOptions.isEmpty()) {
            options.getLogger().error(logcategory, "The --graph-svg-option option is deprecated, use --graph-option");
            cmain.graphOptions.addAll(cmain.graphSvgOptions);
        }

        if (cmain.graph == null && !cmain.graphOptions.isEmpty()) {
            options.getLogger().error(logcategory, "Ignoring--graph-option, --graph not selected");
        }

        String[] actualInput = null;
        if (cmain.inputText.isEmpty()) {
            if (explicitInput != null) {
                actualInput = explicitInput;
            }
        } else {
            if (explicitInput != null) {
                throw ConfigurationException.configError("Unexpected input: " + cmain.inputText.get(0));
            } else {
                actualInput = cmain.inputText.toArray(new String[]{});
                for (String input : actualInput) {
                    if (input.startsWith("-")) {
                        throw ConfigurationException.configError("Unexpected option: " + input);
                     }
                }
            }
        }

        if (actualInput != null) {
            if (cmain.inputFile != null) {
                usage(jc, false, "Input cannot come from both a file and the command line.");
            }

            StringBuilder sb = new StringBuilder();
            for (String token : actualInput) {
                sb.append(token).append(" ");
            }
            input = sb.toString().trim();
            inputFile = null;
        } else {
            if (cmain.inputFile == null && !cmain.analyzeAmbiguity) {
                 if (cmain.showGrammar) {
                     input = null;
                     inputFile = null;
                 } else {
                     if (!cmain.version && !cmain.showOptions) {
                         System.err.println("Usage: ... -g:input.ixml -o:output.xml (--help for more details)");
                         throw ConfigurationException.noInput();
                     }
                     input = null;
                     inputFile = null;
                 }
            } else {
                input = null;
                inputFile = cmain.inputFile;
            }
        }

        if (cmain.outputFile != null && cmain.suppressOutput) {
            usage(jc, false, "You cannot simultaneously specify an output file and suppress output.");
        }

        for (String pragma : cmain.disabledPragmas) {
            options.disablePragma(pragma);
        }

        for (String pragma : cmain.enabledPragmas) {
            options.enablePragma(pragma);
        }

        grammar = cmain.grammar;
        timing = cmain.timing;
        timeRecords = cmain.timeRecords;
        bnf = cmain.bnf;
        analyzeAmbiguity = cmain.analyzeAmbiguity;
        showChart = cmain.showChart;
        showGrammar = cmain.showGrammar;
        encoding = cmain.encoding;
        grammarEncoding = cmain.grammarEncoding;
        records = cmain.records;
        recordStart = cmain.recordstart;
        recordEnd = cmain.recordend;
        suppressOutput = cmain.suppressOutput;
        unbuffered = cmain.unbuffered;
        outputFile = cmain.outputFile;
        forest = cmain.forest;
        graph = cmain.graph;
        graphOptions = cmain.graphOptions;
        if (cmain.graphFormat != null) {
            graphFormat = cmain.graphFormat;
        } else {
            if (graph != null && graph.contains(".")) {
                int pos = graph.lastIndexOf(".");
                graphFormat = graph.substring(pos+1);
            } else {
                graphFormat = "svg";
            }
        }
        parse = cmain.parse;

        if (cmain.parseCount == null) {
            parseCount = 1;
            allParses = false;
        } else {
            if (cmain.parseCount.equals("all")) {
                allParses = true;
                parseCount = 1;
            } else {
                allParses = false;
                int count = Integer.parseInt(cmain.parseCount);
                if (count < 1) {
                    options.getLogger().warn(logcategory, "Ignoring absurd parse count: %d", count);
                    parseCount = 1;
                } else {
                    parseCount = count;
                }
            }
        }

        choose = cmain.choose;
        functionLibrary = cmain.functionLibrary;

        if (cmain.repeat < 1) {
            throw ConfigurationException.configError("The --repeat count must be larger than 0");
        }
        repeat = cmain.repeat;

        if ("on".equals(cmain.progressBar) || "off".equals(cmain.progressBar) || "tty".equals(cmain.progressBar)) {
            // Sigh. I don't want to use 'true' and 'false' as the values because that's
            // potentially confusing with other, actually boolean parameters.
            final String value;
            if ("tty".equals(cmain.progressBar)) {
                value = cmain.progressBar;
            } else {
                value = String.valueOf("on".equals(cmain.progressBar));
            }
            options.setProgressBar(value);
        } else {
            if (cmain.progressBar != null) {
                throw ConfigurationException.configError("Unexpected value for --progress-bar: " + cmain.progressBar);
            }
        }

        if (cmain.showOptions) {
            options.showOptions(stderr);
        }
    }

    private void usage(JCommander jc, boolean help) {
        usage(jc, help, null);
    }

    private void usage(JCommander jc, boolean help, String message) {
        if (message != null) {
            stderr.println("\n" + message);
        }

        if (message == null && jc != null) {
            DefaultUsageFormatter formatter = new DefaultUsageFormatter(jc);
            StringBuilder sb = new StringBuilder();
            formatter.usage(sb);
            stderr.println(sb);
        }

        if (help) {
            throw ConfigurationException.help();
        } else {
            throw ConfigurationException.argsError();
        }
    }

    @Parameters(separators = ":", commandDescription = "CoffeePot options")
    private static class CommandMain {
        @Parameter(names = {"-help", "-h", "--help"}, help = true, description = "Display help")
        public boolean help = false;

        @Parameter(names = {"-c", "--config"}, description = "Load a specific configuration file")
        public String configFile = null;

        @Parameter(names = {"-g", "--grammar"}, description = "The input grammar")
        public String grammar = null;

        @Parameter(names = {"--forest"}, description = "Output an XML description of the forest")
        public String forest = null;

        @Parameter(names = {"--graph-xml"}, description = "Output an XML description of the forest")
        public String graphXml = null;

        @Parameter(names = {"--graph-svg"}, description = "Output an SVG graph of the forest")
        public String graphSvg = null;

        @Parameter(names = {"-G", "--graph"}, description = "Output a graph of the forest")
        public String graph = null;

        @Parameter(names = {"--graph-svg-option"}, description = "Options (parameters) for the graph SVG stylesheet")
        public List<String> graphSvgOptions = new ArrayList<>();

        @Parameter(names = {"--graph-option"}, description = "Options (parameters) for the graph stylesheet")
        public List<String> graphOptions = new ArrayList<>();

        @Parameter(names = {"--graph-format"}, description = "The graph output format (svg, png, etc.)")
        public String graphFormat = null;

        @Parameter(names = {"-i", "--input"}, description = "The input")
        public String inputFile = null;

        @Parameter(names = {"-o", "--output"}, description = "The output, or stdout")
        public String outputFile = null;

        @Parameter(names = {"--no-output"}, description = "Don't print the output")
        public boolean suppressOutput = false;

        @Parameter(names = {"--unbuffered"}, description = "Don't buffer the output")
        public boolean unbuffered = false;

        @Parameter(names = {"--omit-csv-headers"}, description = "Don't generate a CSV header row")
        public boolean omitCsvHeaders = false;

        @Parameter(names = {"-t", "--time"}, description = "Display timing information")
        public boolean timing = false;

        @Parameter(names = {"--time-records"}, description = "Display timing information for each record")
        public boolean timeRecords = false;

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

        @Parameter(names = {"--describe-ambiguity-with"}, description = "Describe why a parse is ambiguous")
        public String describeAmbiguityWith = null;

        @Parameter(names = {"-A", "--analyze-ambiguity"}, description = "Attempt to analyze the grammar for potential ambiguity")
        public boolean analyzeAmbiguity = false;

        @Parameter(names = {"--strict-ambiguity"}, description = "Report ambiguity even when explicit choices are made")
        public boolean strictAmbiguity = false;

        @Parameter(names = {"--show-grammar"}, description = "Show the underlying Earley grammar")
        public boolean showGrammar = false;

        @Parameter(names = {"--show-chart"}, description = "Show the underlying Earley chart")
        public boolean showChart = false;

        @Parameter(names = {"--format"}, description = "Output format (xml, json-data, json-tree, or csv)")
        public String outputFormat = null;

        @Parameter(names = {"--suppress"}, description = "States to ignore in the output")
        public String suppress = null;

        @Parameter(names = {"--disable-pragma"}, description = "Pragmas to disable")
        public List<String> disabledPragmas = new ArrayList<>();

        @Parameter(names = {"--enable-pragma"}, description = "Pragmas to enable")
        public List<String> enabledPragmas = new ArrayList<>();

        @Parameter(names = {"--version"}, description = "Show the version")
        public boolean version = false;

        @Parameter(names = {"--pedantic"}, description = "Run in pedantic mode")
        public boolean pedantic = false;

        @Parameter(names = {"--show-marks"}, description = "Ignore marks in the grammar, output everything")
        public boolean showMarks = false;

        @Parameter(names = {"--show-hidden-nonterminals"}, description = "Show nonterminals generated by the BNF conversion")
        public boolean showHiddenNonterminals = false;

        @Parameter(names = {"--gll"}, description = "Use the GLL parser")
        public boolean gllParser = false;

        @Parameter(names = {"--earley"}, description = "Use the Earley parser")
        public boolean earleyParser = false;

        @Parameter(names = {"--records"}, description = "Process the input as a set of records")
        public boolean records = false;

        @Parameter(names = {"--record-end", "-re"}, description = "Specify the end record separator (regex)")
        public String recordend = null;

        @Parameter(names = {"--record-start", "-rs"}, description = "Specify the start record separator (regex)")
        public String recordstart = null;

        @Parameter(names = {"--encoding"}, description = "Input encoding")
        public String encoding = "UTF-8";

        @Parameter(names = {"--grammar-encoding"}, description = "Grammar encoding")
        public String grammarEncoding = "UTF-8";

        @Parameter(names = {"--choose"}, description = "XPath expressions to choose between ambiguous alternatives")
        public List<String> choose = new ArrayList<>();

        @Parameter(names = {"--function-library"}, description = "Function library to choose between ambiguous alternatives")
        public String functionLibrary = null;

        @Parameter(names = {"--priority-style"}, description = "The style used to compute priorities")
        public String priorityStyle = "max";

        @Parameter(names = {"--bnf"}, description = "Check if the grammar is a simple BNF grammar")
        public boolean bnf = false;

        @Parameter(names = {"--repeat"}, description = "Run the parse several times (for performance testing)")
        public Integer repeat = 1;

        @Parameter(names = {"--progress-bar"}, description = "Specify the type of progress bar")
        public String progressBar = null;

        @Parameter(names = {"--show-options"}, description = "Show the configured options for the parse")
        public boolean showOptions = false;

        @Parameter(description = "The input")
        public List<String> inputText = new ArrayList<>();
    }
}
