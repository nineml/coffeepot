package org.nineml.coffeepot;

import com.beust.jcommander.*;
import net.sf.saxon.s9api.*;
import org.nineml.coffeefilter.InvisibleXml;
import org.nineml.coffeefilter.InvisibleXmlDocument;
import org.nineml.coffeefilter.InvisibleXmlParser;
import org.nineml.coffeefilter.exceptions.IxmlException;
import org.nineml.coffeefilter.trees.*;
import org.nineml.coffeefilter.util.URIUtils;
import org.nineml.coffeegrinder.parser.*;
import org.nineml.coffeepot.utils.ParserOptions;
import org.nineml.coffeepot.utils.*;
import org.xml.sax.InputSource;

import javax.xml.transform.sax.SAXSource;
import java.io.*;
import java.net.URI;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
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
    VerboseEventBuilder eventBuilder;
    CommandMain cmain;
    List<String> inputRecords;
    ArrayList<String> outputRecords = new ArrayList<>();
    boolean parseError = false;
    InvisibleXmlParser parser;
    OutputFormat outputFormat;
    private final Processor processor;
    private static final int UnicodeBOM = 0xFEFF;

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

    public Main() {
        Processor localProcessor = null;
        try {
            localProcessor = new Processor(true);
        } catch (Exception ex) {
            // nevermind
        }
        processor = localProcessor;
    }

    private int run(String[] args) throws IOException {
        cmain = new CommandMain();
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

        cmain.records |= cmain.recordstart != null || cmain.recordend != null;
        if (cmain.records) {
            if (cmain.recordstart != null && cmain.recordend != null) {
                System.err.println("You can only specify one of --record-start and --record-end");
                return 1;
            }
            if (cmain.recordstart == null && cmain.recordend == null) {
                cmain.recordend = "\n";
            }
            if (cmain.parse != 1 || !"1".equals(cmain.parseCount)) {
                System.err.println("You cannot specify --parse or --parse-count with --records");
                return 1;
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
        options.setPedantic(options.getPedantic() || cmain.pedantic);
        options.setShowChart(cmain.showChart);
        options.setShowMarks(cmain.showMarks);
        options.setShowBnfNonterminals(cmain.showHiddenNonterminals);
        if (cmain.suppressCache) {
            options.setCacheDir(null);
        }

        if (cmain.gllParser) {
            options.setParserType("GLL");
        }
        if (cmain.earleyParser) {
            if (cmain.gllParser) {
                options.getLogger().error(logcategory, "Only one parser is allowed, using Earley.");
            }
            options.setParserType("Earley");
        }

        if (cmain.describeAmbiguityWith != null) {
            if ("apixml".equals(cmain.describeAmbiguityWith)) {
                cmain.describeAmbiguityWith = "api-xml";
            }

            if ("xml".equals(cmain.describeAmbiguityWith) || "api-xml".equals(cmain.describeAmbiguityWith)) {
                if (processor == null) {
                    options.getLogger().error(logcategory, "Cannot describe ambiguity with XML, no Saxon processor available");
                    cmain.describeAmbiguityWith = null;
                }
                cmain.describeAmbiguity = true;
            } else {
                if (!"text".equals(cmain.describeAmbiguityWith)) {
                    options.getLogger().error(logcategory, "Ignoring unrecognized ambiguity description type: " + cmain.describeAmbiguityWith);
                }
                cmain.describeAmbiguityWith = null;
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
                System.err.printf("%s version %s (pedantic).%n", BuildConfig.TITLE, BuildConfig.VERSION);
            } else {
                System.err.printf("%s version %s.%n", BuildConfig.TITLE, BuildConfig.VERSION);
            }
        }

        options.getLogger().trace(logcategory, "%s version %s (published %s, hash: %s%s)",
                BuildConfig.TITLE, BuildConfig.VERSION, BuildConfig.PUB_DATE, BuildConfig.PUB_HASH,
                options.getPedantic() ? "; pedantic" : "");

        outputFormat = OutputFormat.XML;
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
                if (processor == null) {
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
                    try {
                        parser = invisibleXml.getParser(cachedURI);
                    } catch (IxmlException ex) {
                        if ("P004".equals(ex.getCode())) {
                            options.getLogger().warn(Cache.logcategory, "%s", ex.getMessage());
                            // Delete the cached URI; try again
                            if (cachedURI.getScheme().equals("file")) {
                                File cached = new File(cachedURI.getPath());
                                boolean ok = cached.delete();
                                if (ok) {
                                    options.getLogger().info(Cache.logcategory, "Deleted cached grammar: %s", cachedURI.getPath());
                                } else {
                                    options.getLogger().info(Cache.logcategory, "Failed to delete cached grammar: %s", cachedURI.getPath());
                                }
                            }
                            parser = invisibleXml.getParser(grammarURI);
                        } else {
                            throw ex;
                        }
                    }
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
                SourceGrammar grammar = parser.getGrammar();
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
                System.err.printf("Failed to parse grammar: %s%n", parser.getException().getMessage());
            } else {
                InvisibleXmlDocument doc = parser.getFailedParse();
                System.err.printf("Failed to parse grammar: could not match %s at line %d, column %d%n",
                        doc.getResult().getLastToken(), doc.getLineNumber(), doc.getColumnNumber());
                if (cmain.showChart) {
                    System.err.println(doc.getTree());
                }
            }

            return 2;
        }

        if (cmain.showGrammar) {
            SourceGrammar resolved = parser.getGrammar().resolveDuplicates();

            // Let's align the ::= signs for pretty...
            ArrayList<String> rules = new ArrayList<>();
            int indent = 0;
            for (Rule rule : resolved.getRules()) {
                // We implement our own version of rule.toString() so that we can include
                // marks in the display; those attributes aren't displayed by CoffeeGrinder
                StringBuilder sb = new StringBuilder();
                sb.append(rule.getSymbol());
                sb.append(" ::= ");
                int count = 0;
                for (Symbol symbol : rule.getRhs().symbols) {
                    if (count > 0) {
                        sb.append(", ");
                    }
                    if (symbol instanceof NonterminalSymbol) {
                        String mark = ((NonterminalSymbol) symbol).getAttributeValue("mark", "^");
                        if (!"^".equals(mark)) {
                            sb.append(mark);
                        }
                    }
                    sb.append(symbol.toString());
                    count += 1;
                }

                String rulestr = sb.toString();
                if (rule.rhs.isEmpty()) {
                    rulestr += "ε";
                }
                if (rulestr.indexOf("::=") > indent) {
                    indent = rulestr.indexOf("::=");
                }
                rules.add(rulestr);
            }

            // Get me some blanks.
            StringBuilder sb = new StringBuilder();
            for (int pos = 0; pos < indent; pos++) {
                sb.append(" ");
            }
            String maxIndent = sb.toString();

            String format = "%d";
            if (rules.size() >= 10) {
                format = "%2d";
            }
            if (rules.size() >= 100) {
                format = "%3d";
            }
            if (rules.size() >= 1000) {
                format = "%4,d";
            }
            format += ". %s%s%n";
            System.err.printf("The %s grammar (%d rules):%n", options.getParserType(), rules.size());
            for (int index = 0; index < rules.size(); index++) {
                String rule = rules.get(index);
                String indentStr = maxIndent.substring(0, indent - rule.indexOf("::="));
                System.err.printf(format, index+1, indentStr, rule);
            }
        }

        if (cmain.compiledGrammar != null) {
            if ("-".equals(cmain.compiledGrammar)) {
                System.err.println(parser.getCompiledParser());
            } else {
                PrintStream ps = new PrintStream(Files.newOutputStream(Paths.get(cmain.compiledGrammar)), true, "UTF-8");
                ps.println(parser.getCompiledParser());
                ps.close();
            }
        }

        if (cmain.inputFile == null && input == null) {
            return 0;
        }

        // Collect the input so we can break it into records
        if (cmain.inputFile != null) {
            try {
                final InputStreamReader isr;
                if ("-".equals(cmain.inputFile)) {
                    options.getLogger().debug(logcategory, "Reading standard input");
                    isr = new InputStreamReader(System.in, cmain.encoding);
                } else {
                    URI inputURI = URIUtils.resolve(URIUtils.cwd(), cmain.inputFile);
                    options.getLogger().debug(logcategory, "Loading input: %s", inputURI);
                    URLConnection conn = inputURI.toURL().openConnection();
                    isr = new InputStreamReader(conn.getInputStream(), cmain.encoding);
                }
                BufferedReader reader = new BufferedReader(isr);

                // I'm not confident this is the most efficient way to do this, but...
                StringBuilder sb = new StringBuilder(1024);
                boolean ignoreBOM = options.getIgnoreBOM() && "utf-8".equalsIgnoreCase(cmain.encoding);
                int inputchar = reader.read();
                if (inputchar != -1) {
                    if (!ignoreBOM || inputchar != UnicodeBOM) {
                        sb.append((char) inputchar);
                    }
                    inputchar = reader.read();
                }
                while (inputchar != -1) {
                    sb.append((char) inputchar);
                    inputchar = reader.read();
                }
                input = sb.toString();
            } catch (IOException ex) {
                System.err.println("Failed to read input: " + cmain.inputFile);
                System.err.println(ex.getMessage());
                return 3;
            }
        }

        // What if the grammar specifies a record separator...
        if (!cmain.records) {
            final String rsuri = "https://nineml.org/ns/pragma/options/record-start";
            final String reuri = "https://nineml.org/ns/pragma/options/record-end";

            Map<String,List<String>> metadata = parser.getMetadata();
            if (metadata.containsKey(rsuri) && metadata.containsKey(reuri)) {
                options.getLogger().error(logcategory, "Grammar must not specify both record-start and record-end options.");
            } else if (metadata.containsKey(rsuri) || metadata.containsKey(reuri)) {
                String key = metadata.containsKey(rsuri) ? rsuri : reuri;
                if (metadata.get(key).size() != 1) {
                    options.getLogger().error(logcategory, "Grammar must not specify more than one record-start or record-end option.");
                } else {
                    String value = metadata.get(key).get(0).trim();
                    if ("".equals(value)) {
                        options.getLogger().error(logcategory, "Grammar must not specify empty record separator.");
                    } else {
                        String quote = value.substring(0, 1);
                        if (quote.equals("\"") || quote.equals("'")) {
                            if (!value.endsWith(quote)) {
                                options.getLogger().error(logcategory, "Grammar specified record separator with mismatched quotes.");
                            } else {
                                value = value.substring(1, value.length()-1);
                                if ("'".equals(quote)) {
                                    value = value.replaceAll("\\\\'", quote);
                                } else {
                                    value = value.replaceAll("\\\\\"", quote);
                                }
                                options.getLogger().info(logcategory, "Grammar selects record-based processing.");
                                cmain.records = true;
                                if (rsuri.equals(key)) {
                                    cmain.recordstart = value;
                                } else {
                                    cmain.recordend = value;
                                }
                            }
                        } else {
                            options.getLogger().info(logcategory, "Grammar selects record-based processing.");
                            cmain.records = true;
                            if (rsuri.equals(key)) {
                                cmain.recordstart = value;
                            } else {
                                cmain.recordend = value;
                            }
                        }
                    }
                }
            }
        }

        if (cmain.records) {
            if (cmain.recordend != null) {
                inputRecords = RecordSplitter.splitOnEnd(input, cmain.recordend);
            } else {
                inputRecords = RecordSplitter.splitOnStart(input, cmain.recordstart);
            }
        } else {
            inputRecords = new ArrayList<>();
            inputRecords.add(input);
        }

        long parseStart = Calendar.getInstance().getTimeInMillis();

        progress = new ProgressBar(options);
        if (inputRecords.size() == 1) {
            parser.getOptions().setProgressMonitor(progress);
        } else {
            progress.startingRecords(inputRecords.size());
        }

        for (int pos = 0; pos < inputRecords.size(); pos++) {
            String record = inputRecords.get(pos);
            options.getLogger().debug(logcategory, "Parsing record %d of %d", pos, inputRecords.size());
            int rc = parse(record, pos+1);
            if (rc != 0) {
                progress.finishedRecords();
                return rc;
            }
        }

        progress.finishedRecords();

        long parseEnd = Calendar.getInstance().getTimeInMillis();
        if (cmain.timing && inputRecords.size() > 1) {
            showTime(parseEnd - parseStart);
        }

        if (!cmain.suppressOutput) {
            PrintStream output = System.out;
            if (cmain.outputFile != null) {
                output = new PrintStream(Files.newOutputStream(Paths.get(cmain.outputFile)));
            }

            if (inputRecords.size() > 1) {
                if (outputFormat == OutputFormat.JSON_DATA || outputFormat == OutputFormat.JSON_TREE) {
                    output.println("{");
                    if (parseError) {
                        output.println("  \"ixml:state\": \"failed\",");
                    }
                    output.println("  \"records\": [");
                } else {
                    output.print("<records");
                    if (parseError) {
                        output.print(" xmlns:ixml=\"http://invisiblexml.org/NS\" ixml:state=\"failed\"");
                    }
                    output.println(">");
                }
            }

            for (int pos = 0; pos < outputRecords.size(); pos++) {
                String result = outputRecords.get(pos);
                output.print(result);
                if ((outputFormat == OutputFormat.JSON_DATA || outputFormat == OutputFormat.JSON_TREE)
                        && (pos+1 < outputRecords.size())) {
                    output.println(",");
                }
            }

            if (inputRecords.size() > 1) {
                if (outputFormat == OutputFormat.JSON_DATA || outputFormat == OutputFormat.JSON_TREE) {
                    output.println("]\n}\n");
                } else {
                    output.println("</records>");
                }
            }

            output.close();
        }

        return 0;
    }

    private int parse(String input, int recordNumber) {
        eventBuilder = new VerboseEventBuilder(parser.getIxmlVersion(), options, processor);
        for (String expr : cmain.choose) {
            eventBuilder.addExpression(expr);
        }
        if (cmain.functionLibrary != null) {
            eventBuilder.addFunctionLibrary(cmain.functionLibrary);
        }
        eventBuilder.setAmbiguityDescription(cmain.describeAmbiguityWith);

        InvisibleXmlDocument doc;

        if (inputRecords.size() > 1 && (recordNumber % 10 == 0)) {
            progress.progressRecord(recordNumber);
        }

        options.getLogger().trace(logcategory, "Input: %s", input);
        doc = parser.parse(input);

        if (cmain.timing && inputRecords.size() == 1) {
            showTime(doc.parseTime());
        }

        if (cmain.suppress != null) {
            String[] states = cmain.suppress.split("[\\s,:]+");
            for (String state : states) {
                doc.getOptions().suppressState(state);
            }
        }

        if (cmain.graphXml != null) {
            doc.getResult().getForest().serialize(cmain.graphXml);
        }

        if (cmain.graphSvg != null) {
            graphForest(doc.getResult(), options, cmain.graphSvg);
        }

        boolean infambig = false;
        if (doc.succeeded()) {
            infambig = doc.getResult().isInfinitelyAmbiguous();
        } else {
            parseError = true;
        }

        if (doc.getNumberOfParses() > 1 || infambig) {
            if (!doc.getOptions().isSuppressedState("ambiguous")) {
                if (doc.getNumberOfParses() == 1) {
                    System.err.println("Found 1 parse, but the grammar is infinitely ambiguous");
                } else {
                    if (infambig) {
                        System.err.printf("Found %,d possible parses (of infinitely many).%n", doc.getNumberOfParses());
                    } else {
                        System.err.printf("Found %,d possible parses.%n", doc.getNumberOfParses());
                    }
                }
            }

            eventBuilder.infiniteAmbiguity(infambig);
        }

        NopContentHandler handler = new NopContentHandler();
        eventBuilder.setHandler(handler);

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

        for (int pos = 1; pos < startingParse; pos++) {
            doc.getTree(eventBuilder);
            if (!doc.moreParses()) {
                System.err.printf("Ran out of parses after %d.%n", pos);
                return 1;
            }
        }

        eventBuilder.verbose = cmain.describeAmbiguity;

        StringBuilder xoutput = new StringBuilder();

        if (parseCount > 1 || allparses) {
            if (outputFormat == OutputFormat.CSV) {
                System.err.println("Cannot output multiple parses as CSV");
                return 1;
            }

            String state = "";
            if (!doc.getOptions().isSuppressedState("ambiguous")) {
                state = "ambiguous";
            }
            if (doc.getResult().prefixSucceeded() && !doc.getOptions().isSuppressedState("prefix")) {
                if ("".equals(state)) {
                    state = "prefix";
                } else {
                    state += " prefix";
                }
            }

            doc.getOptions().suppressState("prefix");
            doc.getOptions().suppressState("ambiguous");

            if (outputFormat == OutputFormat.JSON_DATA || outputFormat == OutputFormat.JSON_TREE) {
                if (allparses) {
                    xoutput.append("{\"ixml\":{\"parses\":\"all\",\n");
                } else {
                    xoutput.append("{\"ixml\":{\"parses\":").append(parseCount).append(",\n");
                }
                if (!"".equals(state)) {
                    xoutput.append("\"ixml:state\": \"").append(state).append("\",\n");
                }
                xoutput.append("\"trees\":[");
            } else {
                xoutput.append("<ixml-parses");
                if (!"".equals(state)) {
                    xoutput.append(" xmlns:ixml='http://invisiblexml.org/NS' ixml:state='").append(state).append("'");

                }
                xoutput.append(" requested-parses='").append(allparses ? "all" : parseCount).append("'>\n");
            }

            boolean more = true;
            int pos = 1;
            while (more) {
                if ((outputFormat == OutputFormat.JSON_DATA || outputFormat == OutputFormat.JSON_TREE)
                        && pos > 1) {
                    xoutput.append(",");
                }
                if (outputFormat == OutputFormat.XML) {
                    xoutput.append("<ixml parse='").append(pos).append("'>");
                }

                String result = serialize(doc, outputFormat);
                if (result == null) {
                    return 3;
                }

                xoutput.append(result);
                if (outputFormat == OutputFormat.XML) {
                    xoutput.append("</ixml>\n");
                }
                pos++;
                more = doc.succeeded() && eventBuilder.moreParses();
                more = more && (allparses || pos <= parseCount);
            }

            if (outputFormat == OutputFormat.JSON_DATA || outputFormat == OutputFormat.JSON_TREE) {
                xoutput.append("]}}");
            } else {
                xoutput.append("</ixml-parses>");
            }
        } else {
            String result = serialize(doc, outputFormat);
            if (result == null) {
                return 3;
            }
            xoutput.append(result);
            if (options.getTrailingNewlineOnOutput()) {
                xoutput.append("\n");
            }
        }

        outputRecords.add(xoutput.toString());

        return 0;
    }

    private String serialize(InvisibleXmlDocument doc, OutputFormat outputFormat) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PrintStream output = new PrintStream(baos);

        DataTreeBuilder dataBuilder;
        SimpleTreeBuilder simpleBuilder;
        DataTree dataTree;
        SimpleTree simpleTree;

        switch (outputFormat) {
            case CSV:
                options.setAssertValidXmlNames(false);
                dataBuilder = new DataTreeBuilder(options);
                eventBuilder.setHandler(dataBuilder);
                doc.getTree(eventBuilder);
                dataTree = dataBuilder.getTree();
                List<CsvColumn> columns = dataTree.prepareCsv();
                if (columns == null) {
                    System.err.println("Result cannot be serialized as CSV");
                    return null;
                }
                output.print(dataTree.asCSV(columns));
                break;
            case JSON_DATA:
                options.setAssertValidXmlNames(false);
                dataBuilder = new DataTreeBuilder(options);
                eventBuilder.setHandler(dataBuilder);
                doc.getTree(eventBuilder);
                dataTree = dataBuilder.getTree();
                output.print(dataTree.asJSON());
                break;
            case JSON_TREE:
                options.setAssertValidXmlNames(false);
                simpleBuilder = new SimpleTreeBuilder(options);
                eventBuilder.setHandler(simpleBuilder);
                doc.getTree(eventBuilder);
                simpleTree = simpleBuilder.getTree();
                output.print(simpleTree.asJSON());
                break;
            default:
                StringTreeBuilder handler = new StringTreeBuilder(options, output);
                eventBuilder.setHandler(handler);
                doc.getTree(eventBuilder);
        }

        try {
            return baos.toString("UTF-8");
        } catch (UnsupportedEncodingException ex) {
            // This can't happen.
            return null;
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

    private void graphForest(GearleyResult result, ParserOptions options, String output) {
        assert processor != null;

        String stylesheet = "/org/nineml/coffeegrinder/forest2dot.xsl";
        try {
            // Get the graph as XML
            ByteArrayInputStream bais = new ByteArrayInputStream(result.getForest().serialize().getBytes(StandardCharsets.UTF_8));
            DocumentBuilder builder = processor.newDocumentBuilder();
            graphXdm(builder.build(new SAXSource(new InputSource(bais))), options, stylesheet, output);
        } catch (Exception ex) {
            System.err.println("Failed to create SVG: " + ex.getMessage());
        }
    }

    private void graphTree(ParseTree result, ParserOptions options, String output) {
        assert processor != null;

        String stylesheet = "/org/nineml/coffeegrinder/tree2dot.xsl";
        try {
            // Get the graph as XML
            ByteArrayInputStream bais = new ByteArrayInputStream(result.serialize().getBytes(StandardCharsets.UTF_8));
            DocumentBuilder builder = processor.newDocumentBuilder();
            graphXdm(builder.build(new SAXSource(new InputSource(bais))), options, stylesheet, output);
        } catch (Exception ex) {
            System.err.println("Failed to create SVG: " + ex.getMessage());
        }
    }

    private void graphXdm(XdmNode document, ParserOptions options, String resource, String output) {
        try {
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
            System.err.println(prefix + time / 1000 + "s");
        } else {
            System.err.println(prefix + time  + "ms");
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

        @Parameter(names = {"--graph-xml"}, description = "Output an XML description of the forest")
        public String graphXml = null;

        @Parameter(names = {"-G", "--graph-svg"}, description = "Output an SVG graph of the forest")
        public String graphSvg = null;

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

        @Parameter(names = {"--describe-ambiguity-with"}, description = "Describe why a parse is ambiguous")
        public String describeAmbiguityWith = null;

        @Parameter(names = {"--show-grammar"}, description = "Show the underlying Earley grammar")
        public boolean showGrammar = false;

        @Parameter(names = {"--show-chart"}, description = "Show the underlying Earley chart")
        public boolean showChart = false;

        @Parameter(names = {"--format"}, description = "Output format (xml, json-data, json-tree, or csv)")
        public String outputFormat = null;

        @Parameter(names = {"--suppress"}, description = "States to ignore in the output")
        public String suppress = null;

        @Parameter(names = {"--version"}, description = "Show the version")
        public boolean version = false;

        @Parameter(names = {"--pedantic"}, description = "Run in pedantic mode")
        public boolean pedantic = false;

        @Parameter(names = {"--no-cache"}, description = "Ignore the cache")
        public boolean suppressCache = false;

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

        @Parameter(names = {"--choose"}, description = "XPath expressions to choose between ambiguous alternatives")
        public List<String> choose = new ArrayList<>();

        @Parameter(names = {"--function-library"}, description = "Function library to choose between ambiguous alternatives")
        public String functionLibrary = null;

        @Parameter(description = "The input")
        public List<String> inputText = new ArrayList<>();
    }
}
