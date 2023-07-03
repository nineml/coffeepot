package org.nineml.coffeepot.trees;

import net.sf.saxon.PreparedStylesheet;
import net.sf.saxon.expr.StaticContext;
import net.sf.saxon.functions.FunctionLibrary;
import net.sf.saxon.ma.map.MapType;
import net.sf.saxon.om.FunctionItem;
import net.sf.saxon.om.StructuredQName;
import net.sf.saxon.pattern.NodeKindTest;
import net.sf.saxon.query.QueryModule;
import net.sf.saxon.query.StaticQueryContext;
import net.sf.saxon.query.XQueryExpression;
import net.sf.saxon.s9api.*;
import net.sf.saxon.trans.SymbolicName;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.type.FunctionItemType;
import org.nineml.coffeefilter.InvisibleXmlDocument;
import org.nineml.coffeefilter.InvisibleXmlParser;
import org.nineml.coffeegrinder.parser.Family;
import org.nineml.coffeegrinder.parser.NonterminalSymbol;
import org.nineml.coffeegrinder.trees.PriorityTreeSelector;
import org.nineml.coffeepot.managers.Configuration;
import org.nineml.coffeepot.utils.ParserOptions;
import org.nineml.coffeesacks.XmlForest;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.xml.transform.Source;
import javax.xml.transform.sax.SAXSource;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.util.*;

public class VerboseTreeSelector extends PriorityTreeSelector {
    public static final String logcategory = "CoffeePot";

    private static final QName _name = new QName("name");
    private static final QName _value = new QName("value");
    private static final QName _symbol = new QName("symbol");
    private static final QName _id = new QName("id");
    private static final QName _children = new QName("children");
    private static final QName _start = new QName("start");
    private static final QName _length = new QName("length");
    private static final QName _priority = new QName("priority");

    public static final XdmAtomicValue _input = new XdmAtomicValue("input");
    public static final XdmAtomicValue _forest = new XdmAtomicValue("forest");
    public static final XdmAtomicValue _available_choices = new XdmAtomicValue("available-choices");
    public static final XdmAtomicValue _other_choices = new XdmAtomicValue("other-choices");
    public static final XdmAtomicValue _selection = new XdmAtomicValue("selection");
    public static final XdmAtomicValue _ambiguous_choice = new XdmAtomicValue("ambiguous-choice");
    private static final QName _context = new QName("context");
    private static final QName _options = new QName("options");
    public static final String CPNS = "https://coffeepot.nineml.org/ns/functions";
    public static final StructuredQName CP_CHOOSE = new StructuredQName("cp", CPNS, "choose-alternative");
    private final Stack<String> symbolStack = new Stack<>();
    private final Stack<Integer> countStack = new Stack<>();
    private final Configuration config;
    private final Processor processor;
    private final XmlForest forest;
    private final ParserOptions options;
    private final XdmAtomicValue input;
    private final List<String> expressions = new ArrayList<>();
    private String functionLibrary = null;
    private XdmMap accumulator = new XdmMap();

    public VerboseTreeSelector(Configuration config, InvisibleXmlParser parser, InvisibleXmlDocument document, String input) {
        this.config = config;
        this.processor = config.processor;
        this.options = config.options;
        this.input = new XdmAtomicValue(input);
        try {
            forest = new XmlForest(processor, document);
        } catch (SaxonApiException | SAXException ex) {
            throw new RuntimeException(ex);
        }
    }

    public void addExpression(String expr) {
        expressions.add(expr);
    }

    public void addFunctionLibrary(String uri) {
        String sed = processor.getUnderlyingConfiguration().getEditionCode();
        if ("HE".equals(sed)) {
            throw new IllegalStateException("Function library support requires Saxon PE or Saxon EE");
        }
        functionLibrary = uri;

        try {
            XPathCompiler compiler = processor.newXPathCompiler();
            StaticContext staticContext = compiler.getUnderlyingStaticContext();

            String cwd = System.getProperty("user.dir").replace('\\', '/');
            cwd += cwd.endsWith("/") ? "" : "/";
            URI baseURI;
            if (cwd.startsWith("/")) {
                baseURI = new URI("file:" + cwd);
            } else {
                baseURI = new URI("file:///" + cwd);
            }
            String staticBaseURI = staticContext.getStaticBaseURI();
            if (staticBaseURI != null && !"".equals(staticBaseURI)) {
                baseURI = baseURI.resolve(staticBaseURI);
            }

            URL url = baseURI.resolve(functionLibrary).toURL();

            options.getLogger().debug(logcategory, "Loading function library: %s", url);
            URLConnection connection = url.openConnection();
            final FunctionLibrary fl;
            if (functionLibrary.contains("xsl")) {
                Source xslt = new SAXSource(new InputSource(connection.getInputStream()));
                XsltCompiler xsltCompiler = processor.newXsltCompiler();
                XsltExecutable xsltExec = xsltCompiler.compile(xslt);
                PreparedStylesheet ps = xsltExec.getUnderlyingCompiledStylesheet();
                fl = ps.getFunctionLibrary();
            } else {
                XQueryCompiler xqcomp = processor.newXQueryCompiler();
                StaticQueryContext sqc = xqcomp.getUnderlyingStaticContext();
                sqc.compileLibrary(connection.getInputStream(), "utf-8");
                XQueryExpression xqe = sqc.compileQuery("import module namespace cp='" + CPNS + "'; .");
                QueryModule qm = xqe.getMainModule();
                fl = qm.getGlobalFunctionLibrary();
            }

            // Set the function library with reflection so that this code will compile with HE
            Class<?> pc = Class.forName("com.saxonica.config.ProfessionalConfiguration");
            Class<?> fc = Class.forName("net.sf.saxon.functions.FunctionLibrary");
            Method setBinder = pc.getMethod("setExtensionBinder", String.class, fc);
            setBinder.invoke(processor.getUnderlyingConfiguration(), "coffeepot", fl);

            // Did you actually provide the function we need?
            SymbolicName.F fname = new SymbolicName.F(CP_CHOOSE, 2);
            FunctionItem chooseAlternative = fl.getFunctionItem(fname, staticContext);

            if (chooseAlternative == null) {
                throw new IllegalArgumentException("Function library does not provide suitable function: " + functionLibrary);
            }

            // Make this code work for either Saxon 11 or Saxon 12
            Class<?> klass = null;
            try {
                // Saxon 11
                klass = Class.forName("net.sf.saxon.om.Function");
            } catch (ClassNotFoundException ex11) {
                try {
                    // Saxon 12
                    klass = Class.forName("net.sf.saxon.om.FunctionItem");
                } catch (ClassNotFoundException ex12) {
                    throw new RuntimeException(ex12);
                }
            }

            final FunctionItemType ctype;
            try {
                Method getFit = klass.getMethod("getFunctionItemType");
                ctype = (FunctionItemType) getFit.invoke(chooseAlternative);
            } catch (IllegalAccessException | IllegalArgumentException ex) {
                throw new RuntimeException(ex);
            }

            if (ctype.getResultType().getPrimaryType() != MapType.ANY_MAP_TYPE) {
                throw new IllegalArgumentException("The choose-alternative function must return a map");
            }
            if (ctype.getArgumentTypes()[0].getPrimaryType() != NodeKindTest.ELEMENT) {
                throw new IllegalArgumentException("The first argument to the choose-alternative function must be an element");
            }
            if (ctype.getArgumentTypes()[1].getPrimaryType() != MapType.ANY_MAP_TYPE) {
                throw new IllegalArgumentException("The second argument to the choose-alternative function must be a map");
            }
        } catch (SaxonApiException | ClassNotFoundException | NoSuchMethodException | IllegalAccessException |
                 InvocationTargetException | XPathException | URISyntaxException | IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void startNonterminal(NonterminalSymbol symbol, Map<String,String> attributes, int leftExtent, int rightExtent) {
        if (symbolStack.isEmpty() || !symbol.getName().equals(symbolStack.peek())) {
            symbolStack.push(symbol.getName());
            countStack.push(1);
        } else {
            int count = countStack.pop() + 1;
            countStack.push(count);
        }
    }

    @Override
    public void endNonterminal(NonterminalSymbol symbol, Map<String,String> attributes, int leftExtent, int rightExtent) {
        if (countStack.peek() == 1) {
            symbolStack.pop();
            countStack.pop();
        } else {
            int count = countStack.pop() - 1;
            countStack.push(count);
        }
    }

    @Override
    public Family select(List<Family> choices, List<Family> otherChoices) {
        Family selected = null;

        HashMap<String,Family> choiceMap = new HashMap<>();
        for (Family choice : choices) {
            choiceMap.put("C" + choice.id, choice);
        }
        for (Family choice : otherChoices) {
            choiceMap.put("C" + choice.id, choice);
        }

        final XdmNode node;
        try {
            XPathCompiler compiler = processor.newXPathCompiler();
            XPathExecutable exec = compiler.compile(String.format("//children[@id='C%d']/parent::symbol", choices.get(0).id));
            XPathSelector selector = exec.load();
            selector.setContextItem(forest.getXml());
            node = (XdmNode) selector.evaluateSingle();

            XdmMap map = new XdmMap();
            map = map.put(_forest, forest.getXml());
            map = map.put(_input, input);

            XdmValue seq = XdmEmptySequence.getInstance();
            for (Family choice : choices) {
                seq = seq.append(new XdmAtomicValue("C" + choice.id));
            }
            map = map.put(_available_choices, seq);

            seq = XdmEmptySequence.getInstance();
            for (Family choice : otherChoices) {
                seq = seq.append(new XdmAtomicValue("C" + choice.id));
            }
            map = map.put(_other_choices, seq);

            for (XdmAtomicValue key : accumulator.keySet()) {
                map = map.put(key, accumulator.get(key));
            }

            if (functionLibrary == null) {
                for (String expr : expressions) {
                    exec = compiler.compile(expr);
                    selector = exec.load();

                    XdmSequenceIterator<XdmNode> iter = node.axisIterator(Axis.CHILD, _children);
                    while (iter.hasNext()) {
                        XdmNode children = iter.next();
                        selector.setContextItem(children);
                        boolean match = selector.effectiveBooleanValue();
                        if (match) {
                            String selection = children.getAttributeValue(_id);
                            if (choiceMap.containsKey(selection)) { // it has to, right?
                                options.getLogger().debug("Expression %s selected %s", expr, selection);
                                selected = choiceMap.get(selection);
                                break;
                            }
                        }
                    }

                    if (selected != null) {
                        break;
                    }

                    options.getLogger().debug("Expression %s did not make a selection", expr);
                }
            } else  {
                compiler.declareNamespace("f", "https://coffeepot.nineml.org/ns/functions");
                compiler.declareVariable(_context);
                compiler.declareVariable(_options);
                exec = compiler.compile("f:choose-alternative($context, $options)");
                selector = exec.load();
                selector.setVariable(_context, node);
                selector.setVariable(_options, map);
                Map<XdmAtomicValue,XdmValue> newMap = selector.evaluateSingle().asMap();

                String selection = null;
                map = new XdmMap();
                for (XdmAtomicValue key : newMap.keySet()) {
                    if (!_forest.equals(key) && !_selection.equals(key)
                            && !_available_choices.equals(key) && !_other_choices.equals(key)
                            && !_ambiguous_choice.equals(key)) {
                        map = map.put(key, newMap.get(key));
                    }
                    if (_selection.equals(key)) {
                        selection = newMap.get(key).getUnderlyingValue().getStringValue();
                    }
                    if (!madeAmbiguousChoice && _ambiguous_choice.equals(key)) {
                        madeAmbiguousChoice = newMap.get(key).getUnderlyingValue().effectiveBooleanValue();
                    }
                }
                accumulator = map;

                if (selection == null) {
                    throw new IllegalArgumentException("choose-alternatives function must return a selection");
                }

                if (choiceMap.containsKey(selection)) {
                    selected = choiceMap.get(selection);
                } else {
                    throw new IllegalArgumentException("choose-alternatives function returned an invalid selection: " + selection);
                }
            }
        } catch (SaxonApiException | XPathException e) {
            throw new RuntimeException(e);
        }

        if (selected == null) {
            selected = super.select(choices, otherChoices);
        }

        if (!"none".equals(config.describeAmbiguityWith)) {
            ArrayList<Family> allChoices = new ArrayList<>(choices);
            allChoices.addAll(otherChoices);
            switch (config.describeAmbiguityWith) {
                case "text":
                    textAmbiguity(node, selected, allChoices);
                    break;
                case "api-xml":
                case "xml":
                    xmlAmbiguity(node, selected, allChoices);
                    break;
                default:
                    break;
            }

        }

        return selected;
    }

    @Override
    public void reset() {
        // nop
    }

    private void textAmbiguity(XdmNode node, Family selection, List<Family> choices) {
        config.stdout.println(choiceContext());

        String lhs = node.getAttributeValue(_name);

        final String nocheck = "        ";
        final String check;
        final String lquo;
        final String rquo;
        final String arrow;
        if (config.options.getAsciiOnly()) {
            check = "      X ";
            lquo = " (";
            rquo = ")";
            arrow = " => ";
        } else {
            check = "      ✔ ";
            lquo = " «";
            rquo = "»";
            arrow = " ⇒ ";
        }

        String id = "C" + selection.id;
        XdmSequenceIterator<XdmNode> childrenIterator = node.axisIterator(Axis.CHILD, _children);
        while (childrenIterator.hasNext()) {
            XdmNode children = childrenIterator.next();

            StringBuilder sb = new StringBuilder();
            if (id.equals(children.getAttributeValue(_id))) {
                sb.append(check);
            } else {
                sb.append(nocheck);
            }
            sb.append(lhs).append(lquo).append(node.getAttributeValue(_start));
            sb.append(",").append(node.getAttributeValue(_length));
            sb.append(rquo);
            if (!"0".equals(children.getAttributeValue(_priority))) {
                sb.append("/").append(children.getAttributeValue(_priority));
            }
            sb.append(arrow);

            boolean first = true;
            XdmSequenceIterator<XdmNode> childIterator = children.axisIterator(Axis.CHILD);
            while (childIterator.hasNext()) {
                XdmNode child = childIterator.next();
                if (!first){
                    sb.append(", ");
                }
                if (_symbol.equals(child.getNodeName())) {
                    sb.append(child.getAttributeValue(_name));
                } else {
                    sb.append(child.getAttributeValue(_value));
                }
                first = false;
            }

            config.stdout.println(sb);
        }
    }

    private void xmlAmbiguity(XdmNode node, Family selection,  List<Family> choices) {
        config.stdout.printf("%s (selected C%s)%n", choiceContext(), selection.id);
        config.stdout.println(node);
    }

    private String choiceContext() {
        StringBuilder sb = new StringBuilder();
        sb.append("At ");
        for (int pos = 0; pos < symbolStack.size(); pos++) {
            String name = symbolStack.get(pos);
            sb.append("/").append(name);
            sb.append("[");
            sb.append(countStack.get(pos));
            sb.append("]");
        }
        return sb.toString();
    }
}
