/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.wala.flowless.pointsto;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;

import com.ibm.wala.classLoader.IField;
import com.ibm.wala.types.ClassLoaderReference;
import com.ibm.wala.types.TypeReference;
import com.ibm.wala.util.graph.Graph;

import edu.kit.joana.wala.flowless.pointsto.AliasGraph.MayAliasGraph;
import edu.kit.joana.wala.flowless.pointsto.AliasGraph.NoMayAliasGraph;
import edu.kit.joana.wala.flowless.pointsto.PtsParameter.RootParameter;
import edu.kit.joana.wala.flowless.util.GraphWriter;
import edu.kit.joana.wala.util.ParamNum;

/**
 * This class is used to build points-to sets from a given May-Alias or Not-May-Alias
 * graph. Each graph node is mapped to a set of artificial points-to elements.
 * The subset relations of these sets fulfill the aliasing situation defined by the
 * given graph.
 *
 * @author Juergen Graf <graf@kit.edu>
 *
 */
public class PointsToSetBuilder {

	private PointsToSetBuilder() {
	}

	/**
	 * This represents a single element of a points-to set.
	 *
	 * @author Juergen Graf <graf@kit.edu>
	 *
	 */
	public static class PtsElement {

		private static int idCounter = 0;

		private final int id = idCounter++;

		public String toString() {
			return "P<" + id + ">";
		}

	}

	/**
	 * A points-to set maps method parameters and thies object fields to a set
	 * of points-to elements. PtsParameter -> { PtsElement }
	 *
	 * @author Juergen Graf <graf@kit.edu>
	 *
	 */
	public static class PointsTo {

		private final Map<PtsParameter, Set<PtsElement>> param2pts = new HashMap<PtsParameter, Set<PtsElement>>();

		private final Set<PtsElement> allPtsElems = new HashSet<PtsElement>();

		public Set<PtsParameter> getAllParameters() {
			return param2pts.keySet();
		}

		public Set<PtsElement> getAllPointsToElements() {
			return Collections.unmodifiableSet(allPtsElems);
		}

		/**
		 * Returns the root parameter with the given parameter number. BEWARE: this is tricky!
		 * There are 3 variants for numbering parameters of non-static methods. <br>
		 * - MethodReference numbering: represents an unresolved method. Does not know any this-pointer.
		 * Therefore there is no parameter number for the this pointer and the numbering of the remaining
		 * params starts at 0.<br>
		 * - MethodInstance numbering: represents a resolved method. The this-pointer is included in the
		 * numbering as well as in the parameter count. this-pointer number == 0, first normal parameter starts at 1.<br>
		 * - PtsParameter numbering: represents a parameter in the sdg/points-to graph. The this-pointer
		 * has a separate number (SimpleParameter.THIS_VAL == -1) while the "normal" parameters always start at 0.<br>
		 *
		 * All static methods
		 * are safe, as their count always starts at 0 and goes up to num_of_params - 1.
		 *
		 * @param paramNum number of the parameter in PtsParameter format. Use RootParameter.mRef2ptsParamNum methods to convert.
		 * @return The root parameter that matches the given parameter number.
		 */
		public RootParameter getRootParameter(final ParamNum paramNum) {
			for (RootParameter root : getRootParameters()) {
				if (root.getParamNum().equals(paramNum)) {
					return root;
				}
			}

			throw new IllegalStateException("Could not find the root of parameter no. " + paramNum);
		}

		public Iterable<RootParameter> getRootParameters() {
			return new Iterable<RootParameter>() {

				@Override
				public Iterator<RootParameter> iterator() {
					return new Iterator<RootParameter>() {

						private Iterator<PtsParameter> it = param2pts.keySet().iterator();
						private RootParameter next = null;

						@Override
						public boolean hasNext() {
							if (next != null) {
								return true;
							}

							while (it.hasNext() && next == null) {
								PtsParameter n = it.next();
								if (n instanceof RootParameter) {
									next = (RootParameter) n;
								}
							}

							return next != null;
						}

						@Override
						public RootParameter next() {
							if (next != null) {
								RootParameter ret = next;
								next = null;
								return ret;
							}

							while (it.hasNext() && next == null) {
								PtsParameter n = it.next();
								if (n instanceof RootParameter) {
									next = (RootParameter) n;
								}
							}

							if (next != null) {
								RootParameter ret = next;
								next = null;
								return ret;
							}

							return null;
						}

						@Override
						public void remove() {
							throw new IllegalStateException("remove not implemented.");
						}
					};
				}
			};
		}

		public Set<PtsElement> getPointsTo(PtsParameter param) {
			return param2pts.get(param);
		}

		public void addPtsElement(PtsParameter from, PtsElement ptsElem) {
			if (ptsElem == null) {
				throw new IllegalArgumentException("ptsElem may not be null.");
			}

			allPtsElems.add(ptsElem);

			if (from.getType() == MergedPtsParameter.MERGE_ARTIFICAL_TYPE) {
				MergedPtsParameter merged = (MergedPtsParameter) from;

				for (PtsParameter member : merged.getMembers()) {
					addPtsElement(member, ptsElem);
				}
			} else {
				Set<PtsElement> pts = param2pts.get(from);
				if (pts == null) {
					pts = new HashSet<PtsElement>();
					param2pts.put(from, pts);
				}

				pts.add(ptsElem);
			}
		}

		public void createUniqueElement(PtsParameter from) {
			if (from.getType() == MergedPtsParameter.MERGE_ARTIFICAL_TYPE) {
				MergedPtsParameter merged = (MergedPtsParameter) from;

				// create an additional element that is shared amongst all participants of the shared node.
				PtsElement shared = new PtsElement();
				allPtsElems.add(shared);

				for (PtsParameter member : merged.getMembers()) {
					createUniqueElement(member);
					param2pts.get(member).add(shared);
				}
			} else {
				Set<PtsElement> pts = param2pts.get(from);
				if (pts == null) {
					pts = new HashSet<PtsElement>();
					param2pts.put(from, pts);
				}

				PtsElement newElem = new PtsElement();
				allPtsElems.add(newElem);
				pts.add(newElem);
			}
		}

		public Set<PtsParameter> getAllParamsPointingTo(PtsElement elem) {
			Set<PtsParameter> params = new HashSet<PtsParameter>();

			for (PtsParameter p : getAllParameters()) {
				if (getPointsTo(p).contains(elem)) {
					params.add(p);
				}
			}

			return params;
		}

		public String toString() {
			StringBuffer sb = new StringBuffer();

			for (RootParameter root : getRootParameters()) {
				sb.append("(" + root + "): { ");
				writeSet(sb, getPointsTo(root));
				sb.append("}\n");

				if (root.hasChildren()) {
					for (PtsParameter child : root.getChildren()) {
						writeParam(sb, child);
					}
				}
			}

			return sb.toString();
		}

		private void writeParam(StringBuffer sb, PtsParameter param) {
			sb.append("(" + param + "): { ");
			writeSet(sb, getPointsTo(param));
			sb.append("}\n");

			if (param.hasChildren()) {
				for (PtsParameter child : param.getChildren()) {
					writeParam(sb, child);
				}
			}
		}

		private static void writeSet(StringBuffer sb, Set<PtsElement> pts) {
			for (PtsElement p : pts) {
				sb.append(p + " ");
			}
		}
	}

	/**
	 * During creation of the points-to sets, we merge similar nodes of the alias
	 * graph that provides the aliasing specification into a single nodes.
	 * This class is used to represent a set of merged nodes from the original
	 * alias graph.
	 *
	 * @author Juergen Graf <graf@kit.edu>
	 *
	 */
	public static class MergedPtsParameter extends PtsParameter {

		private static final long serialVersionUID = -5093215185957083616L;

		private static final TypeReference MERGE_ARTIFICAL_TYPE = TypeReference.findOrCreate(ClassLoaderReference.Primordial, "Merge#Type");

		private final Set<PtsParameter> members = new HashSet<PtsParameter>();

		public MergedPtsParameter() {
			super(nextID(), MERGE_ARTIFICAL_TYPE);
		}

		public void accept(PtsParameterVisitor visitor) {
			visitor.visit(this);
		}

		public void add(PtsParameter param) {
			if (param == null || param == this || param instanceof MergedPtsParameter) {
				throw new IllegalArgumentException();
			}

			members.add(param);
		}

		public boolean contains(PtsParameter param) {
			return members.contains(param);
		}

		public Set<PtsParameter> getMembers() {
			return Collections.unmodifiableSet(members);
		}

		@Override
		public String getName() {
			StringBuilder sb = new StringBuilder();
			sb.append("Merge[ ");
			for (PtsParameter member : members) {
				sb.append("#" + member.id + " ");
			}
			sb.append("]");

			return sb.toString();
		}

		@Override
		public boolean hasParent(IField field) {
			return false;
		}

		@Override
		public boolean isRoot() {
			return false;
		}

	}

	/**
	 * Computes a points-to configuration that conforms to the aliasing specification
	 * of the given may-alias graph. The points-to configuration maps each node of the
	 * may-alias graph to a set of arbitrary points-to elements. Iff two nodes are
	 * connected in the may-alias graph they also share a common element in their
	 * points-to sets. Iff they are not connected in the graph their points-to sets
	 * are disjunct.
	 * @param alias A may-alias graph specifying the aliasing.
	 * @return A points-to configuration that conforms to the specified alias configuration.
	 */
	public static PointsTo compute(MayAliasGraph alias) {
		return compute(alias, GraphWriter.NO_OUTPUT);
	}

	/**
	 * Computes a points-to configuration that conforms to the aliasing specification
	 * of the given may-alias graph. The points-to configuration maps each node of the
	 * may-alias graph to a set of arbitrary points-to elements. Iff two nodes are
	 * connected in the may-alias graph they also share a common element in their
	 * points-to sets. Iff they are not connected in the graph their points-to sets
	 * are disjunct.
	 * @param alias A may-alias graph specifying the aliasing.
	 * @param graphOut A graph writer that can be used to output the created merged alias graphs.
	 * @return A points-to configuration that conforms to the specified alias configuration.
	 */
	public static PointsTo compute(MayAliasGraph alias, GraphWriter graphOut) {
		PointsTo result = new PointsTo();

		NoMayAliasGraph mergedNoAlias = createMergedNoAlias(alias);
		graphOut.writeGraph(mergedNoAlias, "-merged-no_alias");
		MayAliasGraph mergedAlias = mergedNoAlias.constructNegated();
		graphOut.writeGraph(mergedAlias, "-merged-alias");

		// work around as long as there is no undirected graph
		// remove edges in one direction
		for (PtsParameter from : mergedAlias) {
			for (Iterator<PtsParameter> it = mergedAlias.getSuccNodes(from); it.hasNext();) {
				PtsParameter to = it.next();
				if (mergedAlias.hasEdge(to, from)) {
					mergedAlias.removeEdge(to, from);
				}
			}
		}

		for (PtsParameter from : mergedAlias) {
			// every node gets its unique element
			result.createUniqueElement(from);

			for (Iterator<PtsParameter> it = mergedAlias.getSuccNodes(from); it.hasNext();) {
				// additionally a element for each edge is created, as it represents a specific
				// alias relation.
				PtsParameter to = it.next();
				PtsElement ptsElem = new PtsElement();
				result.addPtsElement(from, ptsElem);
				result.addPtsElement(to, ptsElem);
			}
		}

		return result;
	}

	/**
	 * Creates a no-alias graph from an may-alias graph. A no-alias graph contains edges
	 * between nodes that are definitely not aliased. In addition this method merges nodes
	 * that are connected to the same nodes to a single MergedPtsParameter nodes. This
	 * reduces the number of nodes and edges in the no-alias graph. Since this graph is
	 * used to create points-to sets, a smaller graph results in points-to sets with less
	 * elements.
	 * @param alias A graph containing may-alias relations between PtsParameters
	 * @return A merged graph containing no-alias relation between merged PtsParameter
	 */
	public static NoMayAliasGraph createMergedNoAlias(MayAliasGraph alias) {
		NoMayAliasGraph noAlias = alias.constructNegated();

		// merge similar nodes in the graph
		LinkedList<PtsParameter> worklist = new LinkedList<PtsParameter>();
		for (PtsParameter p : noAlias) {
			worklist.add(p);
		}
		Set<PtsParameter> obsolete = new HashSet<PtsParameter>();

		while (!worklist.isEmpty()) {
			PtsParameter p = worklist.removeFirst();
			if (obsolete.contains(p)) {
				continue;
			}

			MergedPtsParameter merge = null;

			// check for similar nodes, those are nodes that may-alias and are not alias to the exact same nodes.
			for (Iterator<PtsParameter> it = alias.getSuccNodes(p); it.hasNext();) {
				PtsParameter succ = it.next();
				if (p != succ && nodesAreSimilar(noAlias, p, succ)) {
					if (merge == null) {
						merge = new MergedPtsParameter();
						merge.add(p);
					}

					merge.add(succ);
				}
			}

			if (merge != null) {
				// replace similar nodes with a single merge node in the graph
				Set<PtsParameter> members = merge.getMembers();
				obsolete.addAll(members);

				noAlias.addNode(merge);

				for (Iterator<PtsParameter> it = noAlias.getSuccNodes(p); it.hasNext();) {
					PtsParameter succ = it.next();
					if (!members.contains(succ)) {
						noAlias.addEdge(merge, succ);
					}
				}

				for (Iterator<PtsParameter> it = noAlias.getPredNodes(p); it.hasNext();) {
					PtsParameter pred = it.next();
					if (!members.contains(pred)) {
						noAlias.addEdge(pred, merge);
					}
				}

				for (PtsParameter member : members) {
					noAlias.removeNodeAndEdges(member);
				}

				// add merged node to worklist? I dont think so...
			}
		}

		return noAlias;
	}

	private static <T> boolean nodesAreSimilar(Graph<T> graph, T n1, T n2) {
		for (Iterator<T> it = graph.getSuccNodes(n1); it.hasNext();) {
			T succ1 = it.next();
			if (succ1 != n2) {
				if (!graph.hasEdge(n2, succ1)) {
					return false;
				}
			}
		}

		for (Iterator<T> it = graph.getSuccNodes(n2); it.hasNext();) {
			T succ2 = it.next();
			if (succ2 != n1) {
				if (!graph.hasEdge(n1, succ2)) {
					return false;
				}
			}
		}

		// we assume that alias graphs are undirected. and a->b => b->a
		// So we do not need to check the predecessors

		return true;
	}

}
