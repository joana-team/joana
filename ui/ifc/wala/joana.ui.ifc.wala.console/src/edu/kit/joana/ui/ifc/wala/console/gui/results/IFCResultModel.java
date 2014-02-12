/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.ui.ifc.wala.console.gui.results;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Iterator;
import java.util.TreeMap;
import java.util.HashMap;
import java.util.TreeSet;

import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;

import edu.kit.joana.api.sdg.SDGProgramPart;
import edu.kit.joana.ifc.sdg.core.violations.IllegalFlow;
import edu.kit.joana.ifc.sdg.core.violations.IViolation;
import edu.kit.joana.api.sdg.SDGInstruction;
import edu.kit.joana.api.sdg.SDGMethod;
import edu.kit.joana.ifc.sdg.util.JavaPackage;
import edu.kit.joana.ui.ifc.wala.console.gui.tree.IFCTreeNode.Kind;

import edu.kit.joana.ifc.sdg.core.violations.IIllegalFlow;

public class IFCResultModel extends DefaultTreeModel {

	private static final long serialVersionUID = -187700438315111297L;

	private static final String EMPTY_STR = "<No anaysis run yet>";
	private final IFCRootNode root;

	public IFCResultModel() {
		super(new IFCRootNode(EMPTY_STR));
		root = getRoot();
	}

	public void clear() {
		root.removeAllChildren();
		root.setUserObject(EMPTY_STR);
		nodeStructureChanged(root);
	}

    public void update(final Collection<? extends IViolation<SDGProgramPart>> vios) {
        root.removeAllChildren();
        root.setUserObject("Violations by Source");

        final Map<SDGProgramPart, IFCResultNode> sourceNodes = new HashMap<SDGProgramPart, IFCResultNode>();

        for (Iterator it = vios.iterator(); it.hasNext();) {
            final IViolation<SDGProgramPart> vio = (IViolation<SDGProgramPart>) it.next();
        
            final SDGProgramPart source;
            final SDGProgramPart sink;
            {
                if (vio instanceof IIllegalFlow) {
                    final IIllegalFlow<SDGProgramPart> flow = (IIllegalFlow<SDGProgramPart>) vio;
                    source = flow.getSource();
                    sink = flow.getSink();
                } else {
                    source = null; // TODO
                    sink = null; 
                }
            }

            final IFCResultNode sourceNode;
            {
                if (sourceNodes.containsKey(source)) {
                    sourceNode = sourceNodes.get(source);
                } else {
                    sourceNode = new IFCResultNode(source, true); 
                }
            }

            final IFCResultNode sinkNode;
            {
                sinkNode = new IFCResultNode(sink, true); 
            }

            final ChopNode chopNode;
            {
                // Will be filled on double-click
                chopNode = new ChopNode(source, sink);
                sinkNode.add(chopNode);
            }


            { // Insert Sink into Source
                sourceNode.add(sinkNode);
            }

            { // Insert Source into Root
                if (sourceNodes.containsKey(source)) {
                    nodeStructureChanged(sourceNode);
                } else {
                    sourceNodes.put(source, sourceNode);
                    root.add(sourceNode);
                }
            }
        }

        nodeStructureChanged(root);
    }

    public void insertChop(final ChopNode chopNode, final Set<SDGInstruction> chop) {
        chopNode.setUserObject("Chop of " + chop.size() + " instructions");

        final List<SDGInstruction> sortedChop;
        {
            final Comparator<SDGInstruction> compa = new Comparator<SDGInstruction>() {
                @Override
                public int compare(final SDGInstruction a, final SDGInstruction b) {
                    int res = 0;
                    // Compare Method
                    res = a.getOwningMethod().getSignature().toBCString().compareTo(
                            b.getOwningMethod().getSignature().toBCString());
                    if (res == 0) {
                        // Compare BCI
                        res = Double.compare(a.getNode().getBytecodeIndex(), b.getNode().getBytecodeIndex());
                    }
                    return res;
                }
            };

            sortedChop = new ArrayList<SDGInstruction>(chop);
            java.util.Collections.sort(sortedChop, compa);
        }


        final Map<SDGMethod, IFCResultNode> methods = new HashMap<SDGMethod, IFCResultNode>();

        for (final SDGInstruction instr : sortedChop) {
            final IFCResultNode methodNode;
            {
                if (methods.containsKey(instr.getOwningMethod())) {
                    methodNode = methods.get(instr.getOwningMethod());
                } else {
                    methodNode = new IFCResultNode(instr.getOwningMethod(), true);
                    methods.put(instr.getOwningMethod(), methodNode);
                    chopNode.add(methodNode);
                }
            }

            final String line = String.format("%04d:  %s", instr.getNode().getBytecodeIndex(), instr.getNode().getLabel());
            final IFCResultNode cNode  = new IFCResultNode(line, false);
            methodNode.add(cNode);
        }

        for (final IFCResultNode methodNode : methods.values()) {
            nodeStructureChanged(methodNode);
        }
        nodeStructureChanged(chopNode);
    }

	@Override
	public IFCRootNode getRoot() {
		return (IFCRootNode) super.getRoot();
	}

	public void setRoot(TreeNode root) {
		throw new UnsupportedOperationException();
	}
}


