package org.nineml.coffeepot.trees;

import net.sf.saxon.s9api.XdmAtomicValue;
import net.sf.saxon.s9api.XdmEmptySequence;
import net.sf.saxon.s9api.XdmMap;
import net.sf.saxon.s9api.XdmValue;
import org.nineml.coffeefilter.trees.SimpleText;
import org.nineml.coffeefilter.trees.SimpleTree;
import org.nineml.coffeepot.managers.Configuration;

public class XdmSimpleTree {
    private final SimpleTree simpleTree;

    public XdmSimpleTree(Configuration config, SimpleTree tree) {
        this.simpleTree = tree;
    }

    public XdmValue json() {
        return simpleTreeXdm(simpleTree);
    }

    private XdmValue simpleTreeXdm(SimpleTree tree) {
        if (tree instanceof SimpleText) {
            return new XdmAtomicValue(tree.getText());
        }

        XdmMap map = new XdmMap();
        if (tree.getName() != null) {
            map = map.put(new XdmAtomicValue("name"), new XdmAtomicValue(tree.getName()));
        }

        if (!tree.getAttributes().isEmpty()) {
            XdmMap attr = new XdmMap();
            for (String name : tree.getAttributes().keySet()) {
                attr.put(new XdmAtomicValue(name), new XdmAtomicValue(tree.getAttribute(name)));
            }
            map = map.put(new XdmAtomicValue("attributes"), attr);
        }

        if (!tree.getChildren().isEmpty()) {
            if (tree.getChildren().size() == 1) {
                map = map.put(new XdmAtomicValue("content"), simpleTreeXdm(tree.getChildren().get(0)));
            } else {
                XdmValue children = XdmEmptySequence.getInstance();
                for (SimpleTree child : tree.getChildren()) {
                    children.append(simpleTreeXdm(child));
                }
                map = map.put(new XdmAtomicValue("content"), children);
            }
        }

        return map;
    }
}

