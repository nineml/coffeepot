package org.nineml.coffeepot.utils;

import org.nineml.coffeefilter.ParserOptions;
import org.nineml.coffeefilter.utils.EventBuilder;
import org.nineml.coffeegrinder.parser.NonterminalSymbol;
import org.nineml.coffeegrinder.parser.RuleChoice;
import org.nineml.coffeegrinder.parser.Symbol;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;

public class VerboseEventBuilder extends EventBuilder {
    public boolean verbose = false;
    private final Stack<StackFrame> symbolStack = new Stack<>();

    public VerboseEventBuilder(String ixmlVersion, ParserOptions options) {
        super(ixmlVersion, options);
    }

    public void startTree() {
        super.startTree();
        symbolStack.clear();
    }

    @Override
    public int startAlternative(List<RuleChoice> alternatives) {
        int selected = super.startAlternative(alternatives);

        if (verbose && alternatives.size() > 1) {
            StringBuilder sb = new StringBuilder();
            sb.append("From ").append(alternatives.get(0).getLeftExtent());
            sb.append("-").append(alternatives.get(0).getRightExtent());
            sb.append(" at ");

            for (int index = 0; index < symbolStack.size(); index++) {
                StackFrame frame = symbolStack.get(index);
                String mark =  frame.attributes.getOrDefault("mark", "-");
                if ("^".equals(mark)) {
                    sb.append("/");
                    sb.append(frame.symbol);
                    sb.append("[").append(symbolStack.get(index-1).childCounts.get(frame.symbol)).append("]");
                }
            }

            System.out.println(sb);

            for (int index = 0; index < alternatives.size(); index++) {
                RuleChoice choice = alternatives.get(index);
                sb = new StringBuilder();
                sb.append(choice.getSymbol());
                String priority = choice.getSymbol().getAttributesMap().getOrDefault("priority", null);
                if (priority != null) {
                    sb.append("(").append(priority).append(")");
                }

                if (choice.getRightHandSide() != null) {
                    sb.append(" = ");
                    int pos = 0;
                    for (Symbol symbol : choice.getRightHandSide()) {
                        if (pos > 0) {
                            sb.append(", ");
                        }
                        sb.append(symbol);
                        priority = symbol.getAttributesMap().getOrDefault("priority", null);
                        if (priority != null) {
                            sb.append("(").append(priority).append(")");
                        }
                        pos++;
                    }
                }

                System.out.printf("\t%s %s%n", (index == selected ? "X" : " "), sb);
            }
        }

        return selected;
    }

    @Override
    public void startNonterminal(NonterminalSymbol symbol, Map<String,String> attributes, int leftExtent, int rightExtent) {
        super.startNonterminal(symbol, attributes, leftExtent, rightExtent);
        if (!symbolStack.isEmpty()) {
            StackFrame top = symbolStack.peek();
            if (!top.childCounts.containsKey(symbol)) {
                top.childCounts.put(symbol, 0);
            }
            top.childCounts.put(symbol, top.childCounts.get(symbol)+1);
        }
        symbolStack.push(new StackFrame(symbol, attributes, leftExtent, rightExtent));
    }

    @Override
    public void endNonterminal(NonterminalSymbol symbol, Map<String,String> attributes, int leftExtent, int rightExtent) {
        super.endNonterminal(symbol, attributes, leftExtent, rightExtent);
        symbolStack.pop();
    }

    private static class StackFrame {
        public final HashMap<Symbol,Integer> childCounts;
        public final Symbol symbol;
        public final Map<String,String> attributes;
        public final int leftExtent;
        public final int rightExtent;
        public StackFrame(Symbol symbol, Map<String,String> attributes, int left, int right) {
            childCounts = new HashMap<>();
            this.symbol = symbol;
            this.attributes = attributes;
            this.leftExtent = left;
            this.rightExtent = right;
        }
    }
}
