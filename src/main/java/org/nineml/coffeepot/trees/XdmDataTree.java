package org.nineml.coffeepot.trees;

import net.sf.saxon.s9api.XdmAtomicValue;
import net.sf.saxon.s9api.XdmEmptySequence;
import net.sf.saxon.s9api.XdmMap;
import net.sf.saxon.s9api.XdmValue;
import org.nineml.coffeefilter.trees.CsvColumn;
import org.nineml.coffeefilter.trees.DataText;
import org.nineml.coffeefilter.trees.DataTree;
import org.nineml.coffeepot.managers.Configuration;

import java.util.List;

public class XdmDataTree {
    private final Configuration config;
    private final DataTree dataTree;

    public XdmDataTree(Configuration config, DataTree tree) {
        this.config = config;
        this.dataTree = tree;
    }

    public XdmValue json() {
        return jsonTreeXdm(dataTree);
    }

    private XdmValue jsonTreeXdm(DataTree tree) {
        if (tree.getAll().isEmpty()) {
            return XdmEmptySequence.getInstance();
        }

        if (tree.getAll().get(0) instanceof DataText) {
            return new XdmAtomicValue(tree.getAll().get(0).getValue());
        }

        XdmMap map = new XdmMap();
        for (DataTree child : tree.getAll()) {
            map = map.put(new XdmAtomicValue(child.getName()), jsonTreeXdm(child));
        }
        return map;
    }

    public XdmValue csv() {
        List<CsvColumn> headers = dataTree.prepareCsv();
        if (headers == null) {
            config.stderr.println("Result cannot be serialized as CSV");
            return null;
        }
        String csv = dataTree.asCSV(headers);
        return new XdmAtomicValue(csv);
    }
}
