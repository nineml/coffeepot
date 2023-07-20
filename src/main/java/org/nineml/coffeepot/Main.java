package org.nineml.coffeepot;

import net.sf.saxon.s9api.*;
import org.nineml.coffeefilter.InvisibleXml;
import org.nineml.coffeefilter.InvisibleXmlDocument;
import org.nineml.coffeefilter.InvisibleXmlParser;
import org.nineml.coffeefilter.util.URIUtils;
import org.nineml.coffeegrinder.parser.*;
import org.nineml.coffeegrinder.trees.Arborist;
import org.nineml.coffeepot.exceptions.ConfigurationException;
import org.nineml.coffeepot.managers.Configuration;
import org.nineml.coffeepot.managers.GraphOutputManager;
import org.nineml.coffeepot.managers.InputManager;
import org.nineml.coffeepot.managers.OutputManager;
import org.nineml.coffeepot.utils.ParserOptions;
import org.nineml.coffeepot.utils.ProgressBar;
import org.xml.sax.InputSource;

import javax.xml.transform.sax.SAXSource;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.Calendar;

/**
 * A command-line Invisible XML parser.
 */
class Main {
    public static final String logcategory = "CoffeePot";

    private final PrintStream stdout;
    private final PrintStream stderr;

    ProgressBar progress = null;

    InvisibleXmlParser parser;
    private Configuration config = null;
    private ParserOptions options = null;
    private int iteration = 0;
    private long accumulatedGrammarParseTime = 0;
    private long accumulatedInputParseTime = 0;

    public static void main(String[] args) {
        Main main = new Main();
        main.run(args);
    }

    public Main() {
        stdout = System.out;
        stderr = System.err;
    }

    public Main(PrintStream stdout, PrintStream stderr) {
        this.stdout = stdout;
        this.stderr = stderr;
    }

    public void run(String[] args) {
        try {
            OutputManager manager = commandLine(args);
            if (manager.isConfigured()) {
                manager.publish();
            }
            System.exit(manager.getReturnCode());
        } catch (Exception ex) {
            if (progress != null) {
                stderr.println();
            }
            stderr.println(ex.getMessage());
            System.exit(1);
        }
    }

    public OutputManager commandLine(String[] args) {
        OutputManager manager = new OutputManager();

        try {
            config = new Configuration(stdout, stderr, args);
        } catch (ConfigurationException ex) {
            if (ex.errorCode == ConfigurationException.HELP) {
                return manager;
            }
            if (ex.getMessage() != null) {
                stderr.println(ex.getMessage());
            }
            manager.setReturnCode(2);
            return manager;
        } catch (Exception ex) {
            if (ex.getMessage() != null) {
                stderr.println(ex.getMessage());
            }
            manager.setReturnCode(2);
            return manager;
        }

        manager.configure(config);

        try {
            if (config.repeat > 1) {
                stderr.printf("Repeating the parse %d times%n", config.repeat);
            }
            for (iteration = 1; iteration <= config.repeat; iteration++) {
                process(manager);
            }
            if (config.timing && config.repeat > 1) {
                showTime(accumulatedGrammarParseTime, "grammar repeatedly");
                showTime(accumulatedInputParseTime, "input repeatedly");
            }
        } catch (Exception ex) {
            if (progress != null) {
                stderr.println();
            }
            stderr.println(ex.getMessage());
            manager.setReturnCode(1);
            manager.setException(ex);
        }

        return manager;
    }

    private void process(OutputManager outputManager) throws IOException {
        options = config.options;

        final InvisibleXml invisibleXml = new InvisibleXml(options);
        final URI grammarURI;
        if (config.grammar == null) {
            options.getLogger().trace(logcategory, "Parsing input with the ixml specification grammar.");
            parser = invisibleXml.getParser();
            grammarURI = null;
        } else  {
            grammarURI = URIUtils.resolve(URIUtils.cwd(), config.grammar);
            options.getLogger().trace(logcategory, "Loading grammar: " + grammarURI);
            parser = invisibleXml.getParser(grammarURI, config.grammarEncoding);
            accumulatedGrammarParseTime += parser.getParseTime();
            if (config.timing) {
                showTime(parser.getParseTime(), config.grammar);
            }
        }

        if (parser.constructed()) {
            if (config.bnf && grammarURI != null) {
                checkBnf(grammarURI);
            }
            hygeineReport();
        } else {
            if (parser.getException() != null) {
                stderr.printf("Failed to parse grammar: %s%n", parser.getException().getMessage());
            } else {
                InvisibleXmlDocument doc = parser.getFailedParse();
                stderr.printf("Failed to parse grammar: could not match %s at line %d, column %d%n",
                        doc.getResult().getLastToken(), doc.getLineNumber(), doc.getColumnNumber());
                if (config.showChart) {
                    stderr.println(doc.getTree());
                }
            }
            return;
        }

        showGrammar();

        if (config.inputFile == null && config.input == null) {
            return;
        }

        InputManager inputManager = new InputManager(config, parser);
        GraphOutputManager graphOutputManager = new GraphOutputManager(config);

        long parseStart = Calendar.getInstance().getTimeInMillis();

        progress = new ProgressBar(options);
        if (inputManager.records.size() == 1) {
            parser.getOptions().setProgressMonitor(progress);
        } else {
            progress.startingRecords(inputManager.records.size());
        }

        for (int pos = 0; pos < inputManager.records.size(); pos++) {
            String record = inputManager.records.get(pos);
            if (inputManager.records.size() > 1) {
                options.getLogger().debug(logcategory, "Parsing record %d of %d", pos, inputManager.records.size());
            }

            if (inputManager.records.size() > 1 && (pos % 10 == 0)) {
                progress.progressRecord(pos);
            }

            options.getLogger().trace(logcategory, "Input: %s", record);
            InvisibleXmlDocument doc = parser.parse(record);
            accumulatedInputParseTime += doc.parseTime();

            if (inputManager.records.size() == 1) {
                if (config.timing) {
                    showTime(doc.parseTime());
                }
            } else {
                if (config.timeRecords) {
                    showTime(doc.parseTime(), String.format("record %d", pos+1));
                }
            }

            outputManager.addOutput(parser, doc, record);

            if (!doc.succeeded()) {
                break;
            }

            graphOutputManager.publish(doc, outputManager.selectedNodes);
        }

        progress.finishedRecords();

        long parseEnd = Calendar.getInstance().getTimeInMillis();

        if (config.timing && inputManager.records.size() > 1) {
            showTime(parseEnd - parseStart, "all input");
        }
    }

    private void hygeineReport() {
        HygieneReport report = parser.getHygieneReport();

        if (config.analyzeAmbiguity) {
            report.checkAmbiguity();
            if (report.ambiguityChecked()) {
                if (report.provablyUnambiguous()) {
                    stdout.println("The grammar is unambiguous.");
                } else {
                    String summary = report.getAmbiguityReport();
                    if (summary == null) {
                        summary = "";
                    }
                    if (summary.contains("***")) {
                        stdout.println("The grammar is ambiguous:");
                        stdout.println(summary);
                    } else {
                        stdout.println("Analysis cannot prove the grammar is unambiguous.");
                        if (!"".equals(summary)) {
                            stdout.println(summary);
                        }
                    }
                }
                if (!report.reliablyUnambiguous()) {
                    stdout.println("(Analysis may be unreliable if the grammar or input uses Unicode characters outside the BMP.)");
                }
            } else {
                stdout.println("Grammar analysis unavailable or analysis failed.");
            }
        }
    }

    private void showGrammar() {
        if (!config.showGrammar) {
            return;
        }

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
        stderr.printf("The %s grammar (%d rules):%n", options.getParserType(), rules.size());
        for (int index = 0; index < rules.size(); index++) {
            String rule = rules.get(index);
            String indentStr = maxIndent.substring(0, indent - rule.indexOf("::="));
            stderr.printf(format, index+1, indentStr, rule);
        }
    }

    private void checkBnf(URI grammarURI) {
        try {
            InvisibleXml invisibleXml = new InvisibleXml(options);
            InvisibleXmlParser bnfparser = invisibleXml.getParser();
            InvisibleXmlDocument bnfgrammar = bnfparser.parse(grammarURI);

            Arborist walker = bnfgrammar.getResult().getArborist();
            DocumentBuilder builder = config.processor.newDocumentBuilder();
            BuildingContentHandler handler = builder.newBuildingContentHandler();
            walker.getTree(bnfgrammar.getAdapter(handler));
            XdmNode tree = handler.getDocumentNode();

            try (InputStream stylestream = getClass().getResourceAsStream("/org/nineml/coffeepot/bnf.xsl")) {
                if (stylestream == null) {
                    stderr.println("Cannot test grammar for BNF formatting; failed to load resource");
                } else {
                    XsltCompiler compiler = config.processor.newXsltCompiler();
                    compiler.setSchemaAware(false);
                    XsltExecutable exec = compiler.compile(new SAXSource(new InputSource(stylestream)));
                    XsltTransformer transformer = exec.load();
                    transformer.setInitialContextNode(tree);
                    XdmDestination destination = new XdmDestination();
                    transformer.setDestination(destination);
                    transformer.transform();
                    String result = destination.getXdmNode().getStringValue();
                    if (!"".equals(result)) {
                        stderr.println("Grammar does not conform to plain BNF: " + result);
                    }
                }
            }
        } catch (SaxonApiException | IOException ex) {
            // I don't believe this is possible...
            throw new RuntimeException(ex);
        }
    }

    private void showTime(Long time) {
        showTime(time, "input");
    }

    private void showTime(Long time, String filename) {
        String prefix = "Parsed in ";
        if (filename != null) {
            prefix = "Parsed " + filename + " in";
        }
        String suffix = "";
        if (config.repeat > 1 && iteration <= config.repeat) {
            // iteration > config.repeat after the last iteration...
            suffix = String.format(" (iteration %d of %d)", iteration, config.repeat);
        }
        if (time > 1000) {
            stderr.printf("%s %ds%s%n", prefix, time / 1000, suffix);
        } else {
            stderr.printf("%s %ds%s%n", prefix, time, suffix);
        }
    }

}
