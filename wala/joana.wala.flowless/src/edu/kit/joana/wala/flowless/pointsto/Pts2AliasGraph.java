/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.wala.flowless.pointsto;

import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import com.ibm.wala.analysis.pointers.HeapGraph;
import com.ibm.wala.classLoader.IField;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.propagation.ArrayContentsKey;
import com.ibm.wala.ipa.callgraph.propagation.InstanceFieldKey;
import com.ibm.wala.ipa.callgraph.propagation.InstanceFieldPointerKey;
import com.ibm.wala.ipa.callgraph.propagation.InstanceKey;
import com.ibm.wala.ipa.callgraph.propagation.PointerAnalysis;
import com.ibm.wala.ipa.callgraph.propagation.PointerKey;
import com.ibm.wala.ssa.SSAInvokeInstruction;
import com.ibm.wala.types.TypeReference;
import com.ibm.wala.util.intset.OrdinalSet;

import edu.kit.joana.wala.flowless.pointsto.AliasGraph.MayAliasGraph;
import edu.kit.joana.wala.flowless.pointsto.PtsParameter.FieldParameter;
import edu.kit.joana.wala.flowless.pointsto.PtsParameter.NormalFieldParameter;
import edu.kit.joana.wala.flowless.pointsto.PtsParameter.RootParameter;
import edu.kit.joana.wala.util.ParamNum;

public final class Pts2AliasGraph {

	private Pts2AliasGraph() {}

	public static MayAliasGraph computeCurrentAliasing(final PointerAnalysis<InstanceKey> pts, final CGNode node,
			final SSAInvokeInstruction invk) {

		// System.out.println("Creating alias graph for callsite: " + node + " - " + invk);

		final List<Pts2AliasParam> roots = new LinkedList<Pts2AliasParam>();

		for (int i = 0; i < invk.getNumberOfParameters(); i++) {
			final int use = invk.getUse(i);

			final PointerKey pkParam = pts.getHeapModel().getPointerKeyForLocal(node, use);
			final OrdinalSet<InstanceKey> ikParam = pts.getPointsToSet(pkParam);

			final ParamNum paramNum = ParamNum.fromIMethod(invk.isStatic(), i);
			final PtsParameter paramRoot = new PtsParameter.RootParameter(invk.getDeclaredTarget(), paramNum);
			final Pts2AliasParam p2aParam = new Pts2AliasParam(paramRoot, pkParam, ikParam);
			roots.add(p2aParam);
		}

		final HeapGraph hg = pts.getHeapGraph();

		final Set<Pts2AliasParam> created = new HashSet<Pts2AliasParam>();
		created.addAll(roots);

		for (final Pts2AliasParam root : roots) {
			createParamFields(root, hg, pts, created);
		}

		MayAliasGraph mayAlias = new MayAliasGraph(invk.isStatic());
		for (Pts2AliasParam p2a : created) {
			mayAlias.addNode(p2a.param);
		}

		for (Pts2AliasParam one : created) {
			for (Pts2AliasParam two : created) {
				if (one != two && one.mayAlias(two)) {
					mayAlias.addEdge(one.param, two.param);
				}
			}
		}

		return mayAlias;
	}

	private static void createParamFields(final Pts2AliasParam parent, final HeapGraph hg,
			final PointerAnalysis<InstanceKey> pts, final Set<Pts2AliasParam> created) {
		for (final InstanceKey ik : parent.pts) {
			for (Iterator<Object> it = hg.getSuccNodes(ik); it.hasNext(); ) {
				final PointerKey succ = (PointerKey) it.next();
				if (succ instanceof InstanceFieldPointerKey) {
					// create node for it
					if (succ instanceof InstanceFieldKey && !sameFieldInPathToRoot(parent, (InstanceFieldKey) succ)) {
						final InstanceFieldKey ifk = (InstanceFieldKey) succ;
						final IField field = ifk.getField();
						if (parent.param.hasChild(field)) {
							PtsParameter child = parent.param.getChild(field);
							Pts2AliasParam p2aField = null;
							for (final Pts2AliasParam p2a : created) {
								if (p2a.param == child) {
									p2aField = p2a;
									break;
								}
							}

							final OrdinalSet<InstanceKey> ptsfield = pts.getPointsToSet(ifk);
							p2aField.merge(ifk, ptsfield);
							createParamFields(p2aField, hg, pts, created);
						} else {
							PtsParameter ptsField = new PtsParameter.NormalFieldParameter(parent.param, field);
							final OrdinalSet<InstanceKey> ptsfield = pts.getPointsToSet(ifk);
							Pts2AliasParam p2aField = new Pts2AliasParam(ptsField, ifk, ptsfield);
							created.add(p2aField);
							createParamFields(p2aField, hg, pts, created);
						}
					} else if (succ instanceof ArrayContentsKey) {
						final ArrayContentsKey ack = (ArrayContentsKey) succ;
						final TypeReference rootTref = parent.param.getType();
						final TypeReference arrayType = rootTref.getArrayElementType();
						final PtsParameter ptsField;

						if (parent.param.hasArrayFieldChild()) {
							final PtsParameter child = parent.param.getArrayFieldChild();
							final OrdinalSet<InstanceKey> ptsfield = pts.getPointsToSet(ack);

							Pts2AliasParam p2aField = null;
							for (final Pts2AliasParam p2a : created) {
								if (p2a.param == child) {
									p2aField = p2a;
									break;
								}
							}

							p2aField.merge(ack, ptsfield);
							createParamFields(p2aField, hg, pts, created);
						} else {

							if (parent.param instanceof RootParameter) {
								ptsField = new PtsParameter.ArrayFieldParameter((RootParameter) parent.param, arrayType);
							} else if (parent.param instanceof FieldParameter) {
								ptsField = new PtsParameter.ArrayFieldParameter((FieldParameter) parent.param, arrayType);
							} else {
								throw new IllegalStateException();
							}

							final OrdinalSet<InstanceKey> ptsfield = pts.getPointsToSet(ack);
							Pts2AliasParam p2aField = new Pts2AliasParam(ptsField, ack, ptsfield);
							created.add(p2aField);

							createParamFields(p2aField, hg, pts, created);
						}
					} else if (!(succ instanceof InstanceFieldKey && sameFieldInPathToRoot(parent, (InstanceFieldKey) succ))) {
						System.out.println("Pts2AliasGraph: " + succ.getClass().getCanonicalName());
					}
				}
			}
		}
	}

	private static boolean sameFieldInPathToRoot(Pts2AliasParam parent, InstanceFieldKey key) {
		final IField field = key.getField();

		PtsParameter cur = parent.param;
		while (cur != null) {
			if (cur instanceof NormalFieldParameter) {
				final NormalFieldParameter fp = ((NormalFieldParameter) cur);
				if (fp.getFieldRef().equals(field.getReference())) {
					return true;
				}
			}

			cur = cur.getParent();
		}

		return false;
	}

	private static class Pts2AliasParam {

		public final PtsParameter param;

		public final Set<PointerKey> pk;
		public OrdinalSet<InstanceKey> pts;

		public Pts2AliasParam(PtsParameter param, PointerKey pk, OrdinalSet<InstanceKey> pts) {
			this.param = param;
			this.pk = new HashSet<PointerKey>();
			this.pk.add(pk);
			this.pts = pts;
		}

		public boolean mayAlias(Pts2AliasParam two) {
			if (pts != null && two.pts != null) {
				return pts.containsAny(two.pts);
			}

			return false;
		}

		public void merge(final PointerKey otherPk, final OrdinalSet<InstanceKey> otherPts) {
			this.pts = OrdinalSet.unify(this.pts, otherPts);
			this.pk.add(otherPk);
		}

		public int hashCode() {
			return param.hashCode();
		}

		public boolean equals(Object obj) {
			if (obj == this) {
				return true;
			}

			if (obj instanceof Pts2AliasParam) {
				Pts2AliasParam other = (Pts2AliasParam) obj;

				return param.equals(other.param) && pk.equals(other.pk) && pts.equals(other.pts);
			}

			return false;
		}
	}

}
