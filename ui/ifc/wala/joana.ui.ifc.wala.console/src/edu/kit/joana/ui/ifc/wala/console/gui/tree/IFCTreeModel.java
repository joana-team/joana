/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.ui.ifc.wala.console.gui.tree;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;

import edu.kit.joana.api.annotations.IFCAnnotation;
import edu.kit.joana.api.sdg.SDGAttribute;
import edu.kit.joana.api.sdg.SDGClass;
import edu.kit.joana.api.sdg.SDGInstruction;
import edu.kit.joana.api.sdg.SDGMethod;
import edu.kit.joana.api.sdg.SDGParameter;
import edu.kit.joana.api.sdg.SDGPhi;
import edu.kit.joana.ifc.sdg.util.JavaPackage;
import edu.kit.joana.ui.ifc.wala.console.gui.tree.IFCTreeNode.Kind;



public class IFCTreeModel extends DefaultTreeModel {

	private static final long serialVersionUID = -187700438315652297L;

	private static final String EMPTY_STR = "<No SDG loaded>";
	private final IFCRootNode root;

	private final Set<SDGClass> classes = new HashSet<SDGClass>();

	public IFCTreeModel() {
		super(new IFCRootNode(EMPTY_STR));
		root = getRoot();
	}

	public void clearMethods() {
		root.removeAllChildren();
		root.setUserObject(EMPTY_STR);
		nodeStructureChanged(root);
	}

	private Map<JavaPackage, Set<SDGClass>> groupByPackage(Set<SDGClass> classes) {
		Map<JavaPackage, Set<SDGClass>> ret = new HashMap<JavaPackage, Set<SDGClass>>();
		for (SDGClass c : classes) {
			JavaPackage p = c.getTypeName().getPackage();
			Set<SDGClass> packClasses;
			if (ret.containsKey(p)) {
				packClasses = ret.get(p);
			} else {
				packClasses = new HashSet<SDGClass>();
				ret.put(p, packClasses);
			}
			packClasses.add(c);
		}

		return ret;
	}

	public void updateClasses(final Collection<SDGClass> newClasses, final String sdg) {
		if (!newClasses.equals(classes)) {
			classes.clear();
			classes.addAll(newClasses);
			root.removeAllChildren();
			root.setUserObject(sdg);

			Map<JavaPackage, Set<SDGClass>> byPackage = groupByPackage(classes);
			// update model

			List<IFCTreeNode> pkgNodes = new ArrayList<IFCTreeNode>();

			for (JavaPackage pkg : byPackage.keySet()) {

				ListTreeNode<SDGClass> pkgNode = new ListTreeNode<SDGClass>(byPackage.get(pkg), true, pkg==null?"(default package)":pkg.getName(), Kind.PACKAGE);

				for (SDGClass cl : byPackage.get(pkg)) {

					SingleElementTreeNode clNode = new SingleElementTreeNode(cl, true, true, Kind.CLASS);

					ListTreeNode<SDGAttribute> attrsNode = new ListTreeNode<SDGAttribute>(cl.getAttributes(), false, "Attributes", Kind.NONE);
					ListTreeNode<SDGMethod> methsNode = new ListTreeNode<SDGMethod>(cl.getMethods(), false, "Methods", Kind.NONE);
					clNode.add(attrsNode);
					clNode.add(methsNode);

					for (SDGAttribute a : cl.getAttributes()) {
						SingleElementTreeNode aNode = new SingleElementTreeNode(a, false, true, Kind.ATTRIBUTE);
						attrsNode.add(aNode);
					}

					for (SDGMethod m : cl.getMethods()) {

						// if
						// (!ClassLoaderReference.Application.getName().toString().equals(m.getEntry().getClassLoader()))
						// {
						// continue;
						// }
						SingleElementTreeNode methNode = new SingleElementTreeNode(m, true, true, Kind.METHOD);
						List<SDGParameter> params = new LinkedList<SDGParameter>();
						params.addAll(m.getParameters());
						ListTreeNode<SDGParameter> paramsNode = new ListTreeNode<SDGParameter>(params, false, "Parameters", Kind.NONE);

						for (SDGParameter p : params) {
							SingleElementTreeNode pNode = new SingleElementTreeNode(
									p, false, true, Kind.PARAMETER);
							paramsNode.add(pNode);
						}

						methNode.add(paramsNode);
						methNode.add(new SingleElementTreeNode(m.getExit(), false, true, Kind.EXIT));

						List<SDGPhi> phis = new LinkedList<SDGPhi>();
						phis.addAll(m.getPhis());
						ListTreeNode<SDGPhi> phisNode = new ListTreeNode<SDGPhi>(phis, false, "PHI Nodes", Kind.NONE);
						for (SDGPhi phi : phis) {
							SingleElementTreeNode phiTreeNode = new SingleElementTreeNode(phi, false, false, Kind.NONE);
							phisNode.add(phiTreeNode);
						}

						methNode.add(phisNode);

						List<SDGInstruction> instructions = new LinkedList<SDGInstruction>();
						instructions.addAll(m.getInstructions());
						ListTreeNode<SDGInstruction> instrsNode = new ListTreeNode<SDGInstruction>(instructions, false, "Instructions", Kind.NONE);

						for (SDGInstruction instr : instructions) {
							SingleElementTreeNode iNode = new SingleElementTreeNode(instr, false, true, Kind.NONE);
							instrsNode.add(iNode);
						}

						methNode.add(instrsNode);

						methsNode.add(methNode);
					}
					pkgNode.add(clNode);
				}

				pkgNodes.add(pkgNode);
			}

			Collections.sort(pkgNodes);

			for (IFCTreeNode pkgNode : pkgNodes) {
				root.add(pkgNode);
			}

		}

		nodeStructureChanged(root);
	}

	public void updateAnnotations(Collection<IFCAnnotation> sources,
			Collection<IFCAnnotation> sinks, Collection<IFCAnnotation> declasses) {
		final Enumeration<?> bfs = root.breadthFirstEnumeration();

		while (bfs.hasMoreElements()) {
			final Object obj = bfs.nextElement();

			final IFCTreeNode node = (IFCTreeNode) obj;
			if (node.isAnnotateable()) {
				if (node.annotate(sources, sinks, declasses)) {
					for (TreeNode anc : node.getPath()) {
						nodeChanged(anc);
					}
				}

			}
		}
	}

	@Override
	public IFCRootNode getRoot() {
		return (IFCRootNode) super.getRoot();
	}

	public void setRoot(TreeNode root) {
		throw new UnsupportedOperationException();
	}

	@Override
	public IFCTreeNode getChild(Object parent, int index) {
		if (!(parent instanceof IFCTreeNode)) {
			throw new IllegalArgumentException();
		}

		return (IFCTreeNode) super.getChild(parent, index);
	}

	@Override
	public int getChildCount(Object parent) {
		if (!(parent instanceof IFCTreeNode)) {
			throw new IllegalArgumentException();
		}

		return super.getChildCount(parent);
	}

	@Override
	public boolean isLeaf(Object node) {
		if (!(node instanceof IFCTreeNode)) {
			throw new IllegalArgumentException();
		}

		return super.isLeaf(node);
	}

	@Override
	public void valueForPathChanged(TreePath path, Object newValue) {
		if (!(newValue instanceof IFCTreeNode)) {
			throw new IllegalArgumentException();
		}

		super.valueForPathChanged(path, newValue);
	}

	@Override
	public int getIndexOfChild(Object parent, Object child) {
		if (!(parent instanceof IFCTreeNode) || !(child instanceof IFCTreeNode)) {
			throw new IllegalArgumentException();
		}

		return super.getIndexOfChild(parent, child);
	}

}


