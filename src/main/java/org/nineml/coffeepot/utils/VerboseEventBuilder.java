package org.nineml.coffeepot.utils;

import net.sf.saxon.PreparedStylesheet;
import net.sf.saxon.expr.StaticContext;
import net.sf.saxon.functions.FunctionLibrary;
import net.sf.saxon.om.Function;
import net.sf.saxon.om.StructuredQName;
import net.sf.saxon.query.QueryModule;
import net.sf.saxon.query.StaticQueryContext;
import net.sf.saxon.query.XQueryExpression;
import net.sf.saxon.s9api.*;
import net.sf.saxon.trans.SymbolicName;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.type.BuiltInAtomicType;
import net.sf.saxon.type.FunctionItemType;
import net.sf.saxon.value.Int64Value;
import org.nineml.coffeefilter.ParserOptions;
import org.nineml.coffeegrinder.parser.ForestNode;
import org.nineml.coffeegrinder.parser.RuleChoice;
import org.nineml.coffeegrinder.parser.State;
import org.nineml.coffeesacks.AlternativeEventBuilder;
import org.nineml.coffeesacks.CoffeeSacksException;
import org.xml.sax.InputSource;

import javax.xml.transform.Source;
import javax.xml.transform.sax.SAXSource;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;

public class VerboseEventBuilder extends AlternativeEventBuilder {
    public static final String logcategory = "CoffeePot";
    public static final String CPNS = "https://coffeepot.nineml.org/ns/functions";
    public static final StructuredQName CP_CHOOSE = new StructuredQName("cp", CPNS, "choose-alternative");
    public boolean verbose = false;
    public boolean showXmlAmbiguity = false;
    public boolean showApiXmlAmbiguity = false;
    public boolean shownHeader = false;
    public boolean infiniteAmbiguity = false;
    private final List<String> expressions = new ArrayList<>();
    private String functionLibrary = null;
    private XsltExecutable simplify = null;

    public VerboseEventBuilder(String ixmlVersion, ParserOptions options, Processor processor) {
        super(processor, ixmlVersion, options);
    }

    public void setAmbiguityDescription(String desc) {
        showXmlAmbiguity = "xml".equals(desc);
        showApiXmlAmbiguity = "api-xml".equals(desc);

        if (showXmlAmbiguity) {
            try {
                ClassLoader loader = ClassLoader.getSystemClassLoader();
                InputStream stream = loader.getResourceAsStream("org/nineml/coffeepot/simplify.xsl");
                Source xslt = new SAXSource(new InputSource(stream));
                XsltCompiler xsltCompiler = processor.newXsltCompiler();
                simplify = xsltCompiler.compile(xslt);
            } catch (SaxonApiException err) {
                throw new RuntimeException("Failed to load simplify.xsl resource", err);
            }
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

            getOptions().getLogger().debug(logcategory, "Loading function library: %s", url);
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
            SymbolicName.F fname = new SymbolicName.F(CP_CHOOSE, 1);
            Function chooseAlternative = fl.getFunctionItem(fname, staticContext);

            if (chooseAlternative == null) {
                throw new IllegalArgumentException("Function library does not provide suitable function: " + functionLibrary);
            }

            FunctionItemType ftype = chooseAlternative.getFunctionItemType();
            if (ftype.getResultType().getPrimaryType() != BuiltInAtomicType.INTEGER) {
                throw new IllegalArgumentException("The choose-alternative() function must return an xs:integer");
            }

            // I can't find a convenient way to test if the argument type is element()+ so I'm just going to
            // assume it is and let the whole thing crash and burn if it isn't.
        } catch (SaxonApiException | ClassNotFoundException | NoSuchMethodException | IllegalAccessException |
                 InvocationTargetException | XPathException | URISyntaxException | IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void infiniteAmbiguity(boolean infinite) {
        infiniteAmbiguity = infinite;
    }

    @Override
    public int startAlternative(ForestNode tree, List<RuleChoice> alternatives) {
        int selected = super.startAlternative(tree, alternatives);

        if (alternatives.size() <= 1) {
            return selected;
        }

        if (processor != null && (functionLibrary != null || expressions.size() > 0)) {
            int xmlselected = startXmlAlternative(tree, alternatives);
            if (xmlselected >= 0) {
                return xmlselected;
            }
        }

        if (!verbose) {
            return selected;
        }

        if (!shownHeader) {
            shownHeader = true;
            if (infiniteAmbiguity) {
                System.out.println("Infinite ambiguity:");
            } else {
                System.out.println("Ambiguity:");
            }
        }

        StringBuilder sb = new StringBuilder();
        sb.append("At ");

        for (int index = 1; index < symbolStack.size(); index++) {
            AlternativeEventBuilder.StackFrame frame = symbolStack.get(index);
            sb.append("/");
            sb.append(frame.symbol);
            sb.append("[").append(symbolStack.get(index-1).childCounts.get(frame.symbol)).append("]");
        }

        if (tree.symbol != null) {
            sb.append("/");
            sb.append(tree.symbol);
            sb.append("[").append(symbolStack.get(symbolStack.size()-1).childCounts.get(tree.symbol)+1).append("]");
        }

        System.out.println(sb);

        if (showApiXmlAmbiguity) {
            showApiXmlAmbiguity(tree, alternatives, selected);
            return selected;
        }

        if (showXmlAmbiguity) {
            showXmlAmbiguity(tree, alternatives, selected);
            return selected;
        }

        for (int index = 0; index < alternatives.size(); index++) {
            RuleChoice choice = alternatives.get(index);

            ForestNode left = choice.getLeftNode();
            ForestNode right = choice.getRightNode();

            sb = new StringBuilder();

            if (right == null) {
                // ε
                sb.append("ε");
            } else {
                if (left != null) {
                    // Left and right side
                    if (left.state == null) {
                        sb.append(left);
                    } else {
                        showState(sb, left);
                    }
                    sb.append(" / ");
                }
                if (right.state == null) {
                    sb.append(right);
                } else {
                    showState(sb, right);
                }
            }
            System.out.printf("\t%s %s%n", (index == selected ? "X" : " "), sb);
        }

        return selected;
    }

    private void showState(StringBuilder sb, ForestNode node) {
        State state = node.getState();
        sb.append(state.symbol);
        sb.append(" «");
        sb.append(node.leftExtent);
        sb.append("-");
        sb.append(node.rightExtent);
        sb.append("» => ");

        if (state.position == 0) {
            sb.append("ε");
        } else {
            sb.append(state.rhs.get(0));
        }

        for (int pos = 1; pos < state.position; pos++) {
            sb.append(", ");
            sb.append(state.rhs.get(pos));
        }
    }

    private void showApiXmlAmbiguity(ForestNode tree, List<RuleChoice> alternatives, int selected) {
        final List<XdmNode> contexts;
        try {
            contexts = xmlAlternatives(tree, alternatives);
        } catch (CoffeeSacksException err) {
            throw new RuntimeException(err);
        }

        int count = 0;
        for (XdmNode alt : contexts) {
            XdmNode root = alt;
            while (root.getParent() != null) {
                root = root.getParent();
            }
            if (count == selected) {
                System.out.printf("Alternative %d of %d (selected):%n", count+1, alternatives.size());
            } else {
                System.out.printf("Alternative %d of %d:%n", count+1, alternatives.size());
            }
            System.out.println(root);
            count++;
        }
    }

    private void showXmlAmbiguity(ForestNode tree, List<RuleChoice> alternatives, int selected) {
        final List<XdmNode> contexts;
        try {
            contexts = xmlAlternatives(tree, alternatives);
        } catch (CoffeeSacksException err) {
            throw new RuntimeException(err);
        }

        int count = 0;
        for (XdmNode alt : contexts) {
            if (count == selected) {
                System.out.printf("Alternative %d of %d (selected):%n", count+1, alternatives.size());
            } else {
                System.out.printf("Alternative %d of %d:%n", count+1, alternatives.size());
            }

            try {
                Xslt30Transformer transformer = simplify.load30();
                XdmDestination result = new XdmDestination();
                transformer.transform(alt.asSource(), result);
                System.out.println(result.getXdmNode());
            } catch (SaxonApiException err) {
                throw new RuntimeException("Running the simplify transform failed", err);
            }

            count++;
        }
    }

    private int startXmlAlternative(ForestNode tree, List<RuleChoice> alternatives) {
        final List<XdmNode> contexts;
        try {
            contexts = xmlAlternatives(tree, alternatives);
        } catch (CoffeeSacksException err) {
            throw new RuntimeException(err);
        }

        XdmValue altseq = contexts.get(0);
        for (int count = 1; count < contexts.size(); count++) {
            altseq = altseq.append(contexts.get(count));
        }

        try {
            XPathCompiler compiler = processor.newXPathCompiler();

            if (functionLibrary != null) {
                compiler.declareNamespace("f", "https://coffeepot.nineml.org/ns/functions");
                compiler.declareVariable(new QName("alts"));
                XPathExecutable exec = compiler.compile("f:choose-alternative($alts)");
                XPathSelector selector = exec.load();
                selector.setVariable(new QName("alts"), altseq);
                XdmItem item = selector.evaluateSingle();

                // I checked the return type when I loaded the function, so I think this is safe
                long value = ((Int64Value) item.getUnderlyingValue()).longValue();
                if (value != 0) {
                    if (value < 0 || value > alternatives.size()) {
                        throw new IllegalArgumentException("Value out of range from choose-alternatives function");
                    }
                    return ((int) value) - 1;
                }
            }

            int choice = 0;
            for (XdmNode context : contexts) {
                for (String expr : expressions) {
                    XPathExecutable exec = compiler.compile(expr);
                    XPathSelector selector = exec.load();
                    selector.setContextItem(context);
                    //XdmItem item = selector.evaluateSingle();
                    //selector = exec.load();
                    //selector.setContextItem(context);
                    boolean ebv = selector.effectiveBooleanValue();
                    if (ebv) {
                        return choice;
                    }
                }
                choice++;
            }
        } catch (SaxonApiException e) {
            throw new RuntimeException(e);
        }

        return -1;
    }
}
