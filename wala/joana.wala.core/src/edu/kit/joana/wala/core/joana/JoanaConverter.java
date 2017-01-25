/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.

 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.wala.core.joana;

import java.util.Arrays;
import java.util.Comparator;
import java.util.Stack;

import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.util.CancelException;
import com.ibm.wala.util.MonitorUtil;
import com.ibm.wala.util.MonitorUtil.IProgressMonitor;

import edu.kit.joana.ifc.sdg.core.SecurityNode;
import edu.kit.joana.ifc.sdg.graph.SDG;
import edu.kit.joana.ifc.sdg.graph.SDGEdge;
import edu.kit.joana.ifc.sdg.graph.SDGNode;
import edu.kit.joana.ifc.sdg.graph.SDGNode.Kind;
import edu.kit.joana.ifc.sdg.graph.SDGNode.Operation;
import edu.kit.joana.wala.core.DependenceGraph;
import edu.kit.joana.wala.core.PDG;
import edu.kit.joana.wala.core.PDGEdge;
import edu.kit.joana.wala.core.PDGNode;
import edu.kit.joana.wala.core.SDGBuilder;
import edu.kit.joana.wala.core.SourceLocation;
import edu.kit.joana.wala.core.graphs.GraphWalker;
import edu.kit.joana.wala.util.PrettyWalaNames;
import gnu.trove.set.TIntSet;



public class JoanaConverter {

	private JoanaConverter() {}

	public static SDG convert(final SDGBuilder b, IProgressMonitor progress) throws CancelException {
		final SDG sdg = (b.getEntry() == null
			? new SDG("multiple-entrypoints.SDG()")
			: new SDG(PrettyWalaNames.methodName(b.getEntry())));

        progress.beginTask("Building utility edges", IProgressMonitor.UNKNOWN);
        addUtilityEdges(b);
        progress.worked(1);
		MonitorUtil.throwExceptionIfCanceled(progress);
        progress.done();

        progress.beginTask("Sorting nodes", IProgressMonitor.UNKNOWN);  // XXX: can't we get the node count here?
        //progress.subTask("Sorting all nodes by their id");
        PDGNode allNodes[] = getAllNodesSorted(b, progress);
        progress.done();

        progress.beginTask("Inserting nodes into SDG", allNodes.length);
        progress.subTask("processing " + allNodes.length + " nodes");
        convertNodes(sdg, allNodes, b, progress);
        progress.done();

        progress.beginTask("Inserting edges into SDG", allNodes.length);
        progress.subTask("processing " + allNodes.length + " nodes");
        for (int i = 0; i < allNodes.length; i++) {
        	addEdgesForNode(sdg, allNodes[i], b);

        	if (i % 107 == 0) {
                progress.worked(i);
    			MonitorUtil.throwExceptionIfCanceled(progress);
            }
        }
        sdg.setNode2Instr(b.getPDGNode2IIndex());
        sdg.setEntryToCGNode(b.getEntryNode2CGNode());
        progress.done();

		return sdg;
	}

	private static void addEdgesForNode(SDG sdg, PDGNode node, SDGBuilder b) {
		PDG pdg = b.getPDGforId(node.getPdgId());
		SDGNode from = sdg.getNode(node.getId());

		if (!pdg.containsVertex(node)) {
			throw new IllegalStateException();
		}

		for (PDGEdge edge : pdg.outgoingEdgesOf(node)) {
			SDGNode to = sdg.getNode(edge.to.getId());
			
			SDGEdge sdgEdge = createEdge(from, to, edge.kind, edge.getLabel());
			sdg.addEdge(from, to, sdgEdge);
		}
	}

	private static SDGEdge createEdge(SDGNode from, SDGNode to, PDGEdge.Kind kind, String label) {
		SDGEdge.Kind sdgKind = null;

		switch (kind) {
		case CALL_STATIC:
			sdgKind = SDGEdge.Kind.CALL;
			assert label == null;
			label = "static";
			break;
		case CALL_VIRTUAL:
			sdgKind = SDGEdge.Kind.CALL;
			assert label == null;
			label = "virtual";
			break;
		case CONTROL_DEP_EXPR:
			sdgKind = SDGEdge.Kind.CONTROL_DEP_EXPR;
			assert label == null;
			break;
		case CONTROL_DEP:
			sdgKind = SDGEdge.Kind.CONTROL_DEP_COND;
			assert label == null;
			break;
		case CONTROL_FLOW:
			sdgKind = SDGEdge.Kind.CONTROL_FLOW;
			assert label == null;
			break;
		case CONTROL_FLOW_EXC:
			sdgKind = SDGEdge.Kind.CONTROL_FLOW;
			assert label == null;
			label = "exc";
			break;
		case DATA_DEP:
			sdgKind = SDGEdge.Kind.DATA_DEP;
			assert label == null;
			break;
		case DATA_HEAP:
			sdgKind = SDGEdge.Kind.DATA_HEAP;
			assert label == null;
			break;
		case DATA_ALIAS:
			sdgKind = SDGEdge.Kind.DATA_ALIAS;
			break;
		case SUMMARY_DATA:
			sdgKind = SDGEdge.Kind.SUMMARY_DATA;
			break;
		case SUMMARY_NO_ALIAS:
			sdgKind = SDGEdge.Kind.SUMMARY_NO_ALIAS;
			break;
		case PARAM_STRUCT:
			sdgKind = SDGEdge.Kind.PARAMETER_STRUCTURE;
			assert label == null;
			break;
		case UTILITY:
			sdgKind = SDGEdge.Kind.HELP;
			assert label == null;
			break;
		case PARAMETER_IN:
			sdgKind = SDGEdge.Kind.PARAMETER_IN;
			assert label == null;
			break;
		case PARAMETER_OUT:
			sdgKind = SDGEdge.Kind.PARAMETER_OUT;
			assert label == null;
			break;
		case INTERFERENCE:
			sdgKind = SDGEdge.Kind.INTERFERENCE;
			break;
		case INTERFERENCE_WRITE:
			sdgKind = SDGEdge.Kind.INTERFERENCE_WRITE;
			break;
		case FORK:
			sdgKind = SDGEdge.Kind.FORK;
			break;
		case FORK_IN:
			sdgKind = SDGEdge.Kind.FORK_IN;
			break;
		case RETURN:
			sdgKind = SDGEdge.Kind.RETURN;
			break;
		default:
			throw new IllegalStateException("Unknown edge type: " + kind.name());
		}

		assert sdgKind != null;

		SDGEdge edge = (label == null ? new SDGEdge(from, to, sdgKind) : new SDGEdge(from, to, sdgKind, label));

		return edge;
	}

	private static void convertNodes(SDG sdg, PDGNode[] nodes, SDGBuilder b,
			IProgressMonitor progress) throws CancelException {
		int i = 0;
		for (PDGNode node : nodes) {
			SDGNode snode = convertNode(b, node);
			sdg.addVertex(snode);

			if (i++ % 107 == 0) {
				progress.worked(i);
				MonitorUtil.throwExceptionIfCanceled(progress);
			}
		}
	}

	private static SDGNode convertNode(SDGBuilder sdg, PDGNode node) {
		Operation op = null;
		Kind kind = null;
		int[] allocNodes = null;
		switch (node.getKind()) {
		case ACTUAL_IN:
			op = Operation.ACTUAL_IN;
			kind = Kind.ACTUAL_IN;
			break;
		case ACTUAL_OUT:
			op = Operation.ACTUAL_OUT;
			kind = Kind.ACTUAL_OUT;
			break;
		case CALL:
			op = Operation.CALL;
			kind = Kind.CALL;
			TIntSet allocNodesAsSet = sdg.getAllocationNodes(node);
			if (allocNodesAsSet != null) {
				allocNodes = allocNodesAsSet.toArray();
			}
			break;
		case ENTRY:
			op = Operation.ENTRY;
			kind = Kind.ENTRY;
			break;
		case EXIT:
			op = Operation.EXIT;
			kind = Kind.EXIT;
			break;
		case EXPRESSION:
			op = Operation.ASSIGN;
			kind = Kind.EXPRESSION;
			break;
		case FOLDED:
			op = Operation.COMPOUND;
			kind = Kind.FOLDED;
			break;
		case FORMAL_IN:
			op = Operation.FORMAL_IN;
			kind = Kind.FORMAL_IN;
			break;
		case FORMAL_OUT:
			op = Operation.FORMAL_OUT;
			kind = Kind.FORMAL_OUT;
			break;
		case HREAD:
			op = Operation.REFERENCE;
			kind = Kind.EXPRESSION;
			break;
		case HWRITE:
			op = Operation.MODIFY;
			kind = Kind.EXPRESSION;
			break;
		case JOIN:
			op = Operation.COMPOUND;
			kind = Kind.JOIN;
			break;
		case NEW:
			op = Operation.DECLARATION;
			kind = Kind.NORMAL;
			break;
		case NORMAL:
			op = Operation.COMPOUND;
			kind = Kind.NORMAL;
			break;
		case PHI:
			op = Operation.ASSIGN;
			kind = Kind.EXPRESSION;
			break;
		case PREDICATE:
			op = Operation.IF;
			kind = Kind.PREDICATE;
			break;
		case SYNCHRONIZATION:
			op = Operation.MONITOR;
			kind = Kind.SYNCHRONIZATION;
			break;
		default:
			throw new IllegalStateException("Unknown node kind: " + node.getKind().name());
		}
		SourceLocation sloc = node.getSourceLocation();

		SDGNode sn = new SecurityNode(node.getId(), op, node.getLabel(), node.getPdgId(), node.getType(),
				sloc.getSourceFile(), sloc.getStartRow(), sloc.getStartColumn(), sloc.getEndRow(), sloc.getEndColumn(),
				node.getBytecodeName(), node.getBytecodeIndex());

		if (node.getKind() == PDGNode.Kind.ENTRY) {
			PDG pdg = sdg.getPDGforId(node.getPdgId());
			IMethod im = pdg.getMethod();

			if (im != null) {
				IClass cls = im.getDeclaringClass();

				if (cls != null) {
					String clsLoader = cls.getClassLoader().toString();
					sn.setClassLoader(clsLoader);
				}
			}
		}

		if (allocNodes != null) {
			sn.setAllocationSites(allocNodes);
		}

		if (node.getAliasDataSources() != null) {
			sn.setAliasDataSources(node.getAliasDataSources());
		}

		if (node.getUnresolvedCallTarget() != null) {
			sn.setUnresolvedCallTarget(node.getUnresolvedCallTarget());
		}
		
		if (node.getLocalUseNames() != null) {
			sn.setLocalUseNames(node.getLocalUseNames());
		}
			
		if (node.getLocalDefNames() != null) {
			sn.setLocalDefNames(node.getLocalDefNames());
		}
		
		assert sn.getKind() == kind;

		return sn;
	}

	private static class UtilityEdgeWalker extends GraphWalker<PDGNode, PDGEdge> {

		private final Stack<PDGNode> parent = new Stack<PDGNode>();
		private final DependenceGraph g;

		public UtilityEdgeWalker(DependenceGraph graph) {
			super(graph);
			this.g = graph;
		}

		@Override
		public boolean traverse(PDGEdge edge) {
			return edge.kind.isControl();
		}

		@Override
		public void discover(PDGNode node) {
			if (!parent.isEmpty()) {
				PDGNode pred = parent.peek();
				g.addEdge(pred, node, PDGEdge.Kind.UTILITY);
			}

			parent.push(node);
		}

		@Override
		public void finish(PDGNode node) {
			parent.pop();
		}

	}

	private static void addUtilityEdges(SDGBuilder sdg) {
		for (PDG pdg : sdg.getAllPDGs()) {
			UtilityEdgeWalker walker = new UtilityEdgeWalker(pdg);
			walker.traverseDFS(pdg.entry);
		}
	}

	private static PDGNode[] getAllNodesSorted(SDGBuilder builder, IProgressMonitor progress) throws CancelException {
		final long nrOfNodesInPdg = builder.countNodesInPdgs();
		if (nrOfNodesInPdg > ((long) Integer.MAX_VALUE)) {
			throw new IllegalStateException();
		}
		PDGNode[] copy = new PDGNode[(int)nrOfNodesInPdg];
		
        int progr = 0;
		
        for (PDG pdg : builder.getAllPDGs()) {
			for (PDGNode node : pdg.vertexSet()) {
				if (node.getPdgId() == pdg.getId()) {
					copy[progr] = node;
                    if (++progr % 107 == 0) {
                        progress.worked(progr);
				        MonitorUtil.throwExceptionIfCanceled(progress);
                    }
				}
			}
		}

        //progress.worked(1);
		MonitorUtil.throwExceptionIfCanceled(progress);

        Arrays.parallelSort(copy, new Comparator<PDGNode>() {
            public int compare(PDGNode o1, PDGNode o2) {
                return o1.getId() - o2.getId();
            }});
        //progress.worked(1);
		MonitorUtil.throwExceptionIfCanceled(progress);

		return copy;
	}



}
