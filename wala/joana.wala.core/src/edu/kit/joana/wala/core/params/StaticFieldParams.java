/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.wala.core.params;

import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import com.ibm.wala.dataflow.graph.BitVectorFramework;
import com.ibm.wala.dataflow.graph.BitVectorSolver;
import com.ibm.wala.dataflow.graph.ITransferFunctionProvider;
import com.ibm.wala.fixpoint.BitVectorVariable;
import com.ibm.wala.util.CancelException;
import com.ibm.wala.util.MonitorUtil;
import com.ibm.wala.util.MonitorUtil.IProgressMonitor;
import com.ibm.wala.util.collections.ObjectArrayMapping;
import com.ibm.wala.util.graph.Graph;
import com.ibm.wala.util.graph.impl.GraphInverter;
import com.ibm.wala.util.graph.impl.SparseNumberedGraph;
import com.ibm.wala.util.intset.IntIterator;
import com.ibm.wala.util.intset.IntSet;
import com.ibm.wala.util.intset.OrdinalSet;
import com.ibm.wala.util.intset.OrdinalSetMapping;

import edu.kit.joana.util.Log;
import edu.kit.joana.util.Logger;
import edu.kit.joana.wala.core.PDG;
import edu.kit.joana.wala.core.PDGEdge;
import edu.kit.joana.wala.core.PDGField;
import edu.kit.joana.wala.core.PDGNode;
import edu.kit.joana.wala.core.ParameterField;
import edu.kit.joana.wala.core.SDGBuilder;
import edu.kit.joana.wala.core.dataflow.GenReach;
import edu.kit.joana.wala.core.dataflow.IModRef;
import edu.kit.joana.wala.core.dataflow.ModRefStaticField;
import edu.kit.joana.wala.core.dataflow.ReachingDefsTransferFP;

public class StaticFieldParams {
	private final static Logger debug = Log.getLogger(Log.L_WALA_CORE_DEBUG);
	private final static boolean IS_DEBUG = debug.isEnabled();
	private final SDGBuilder sdg;

	public static void compute(SDGBuilder sdg, IProgressMonitor progress) throws CancelException {
		StaticFieldParams sfp = new StaticFieldParams(sdg);
		sfp.run(progress);
	}

	private StaticFieldParams(SDGBuilder sdg) {
		this.sdg = sdg;
	}

	private void run(IProgressMonitor progress) throws CancelException {
		// propagate static root nodes
		propagateStaticFields(progress);
		// add dataflow for static fields
		addDataFlowForStaticFields(progress);
	}

	private void addDataFlowForStaticFields(final IProgressMonitor progress) throws CancelException {
		for (final PDG pdg : sdg.getAllPDGs()) {
			final Graph<PDGNode> cfg = extractControlFlow(pdg);

			final Map<PDGNode, OrdinalSet<ParameterField>> access = pdg.getStaticAccessMap();
			final Set<PDGNode> relevant = access.keySet();
			final PDGNode[] arr = new PDGNode[relevant.size()];
			relevant.toArray(arr);
			final OrdinalSetMapping<PDGNode> mapping = new ObjectArrayMapping<PDGNode>(arr);
			final IModRef modRef = new ModRefStaticField(mapping, access);
			modRef.compute(progress);
			final ITransferFunctionProvider<PDGNode, BitVectorVariable> transfer = new ReachingDefsTransferFP(mapping, modRef);
			final BitVectorFramework<PDGNode, PDGNode> reachDef = new BitVectorFramework<PDGNode, PDGNode>(cfg, transfer, mapping);
			final BitVectorSolver<PDGNode> solver = new BitVectorSolver<PDGNode>(reachDef);
			solver.solve(progress);

			for (PDGNode node : relevant) {
				final BitVectorVariable in = solver.getIn(node);
				final BitVectorVariable ref = modRef.getRef(node);

				final IntSet inSet = in.getValue();
				final IntSet refSet = ref.getValue();
				if (inSet == null || refSet == null) {
					continue;
				}

				if (node.getKind() == PDGNode.Kind.HREAD || node.getKind() == PDGNode.Kind.HWRITE) {
					// we connect data dependencies through the fine grained field nodes, not the instruction node
					final PDGField f = pdg.getField(node);
					node = f.accfield;
				}

				final IntSet intersect = refSet.intersection(inSet);

				for (final IntIterator it = intersect.intIterator(); it.hasNext();) {
					final int fromId = it.next();
					PDGNode from = mapping.getMappedObject(fromId);

					if (SDGBuilder.DATA_FLOW_FOR_GET_FROM_FIELD_NODE && from.getKind() == PDGNode.Kind.HREAD) {
						// we connect data dependencies through the fine grained field nodes, not the instruction node
						final PDGField f = pdg.getField(from);
						from = f.accfield;
					} else if (from.getKind() == PDGNode.Kind.HWRITE) {
						// we connect data dependencies through the fine grained field nodes, not the instruction node
						final PDGField f = pdg.getField(from);
						from = f.accfield;
					}

					if (from != node) {
						pdg.addEdge(from, node, PDGEdge.Kind.DATA_DEP);
					}
				}
			}
		}
	}

	private static Graph<PDGNode> extractControlFlow(PDG pdg) {
		Graph<PDGNode> cfg = new SparseNumberedGraph<PDGNode>();
		for (PDGNode node : pdg.vertexSet()) {
			if (node.getPdgId() == pdg.getId()) {
				cfg.addNode(node);
			}
		}

		for (PDGNode node : pdg.vertexSet()) {
			if (node.getPdgId() == pdg.getId()) {
				for (PDGEdge edge : pdg.outgoingEdgesOf(node)) {
					if (edge.kind.isFlow() && edge.to.getPdgId() == pdg.getId()) {
						cfg.addEdge(node, edge.to);
					}
				}
			}
		}

		return cfg;
	}

	private void propagateStaticFields(final IProgressMonitor progress) throws CancelException {
		final Map<PDG, Collection<FieldAccess>> accessMap = createStaticFieldAccessMap();
		final Graph<PDG> cgPDG = sdg.createCallGraph();
		final Graph<PDG> invertedCG = GraphInverter.invert(cgPDG);
		final GenReach<PDG, FieldAccess> genReach = new GenReach<PDG, FieldAccess>(invertedCG, accessMap);
		final BitVectorSolver<PDG> solver = new BitVectorSolver<PDG>(genReach);
		solver.solve(progress);

		for (final PDG pdg : sdg.getAllPDGs()) {
			final BitVectorVariable bv = solver.getOut(pdg);
			final Iterator<FieldAccess> faccs;
			// We assume that for non-parallel SDGBuilder, the user cares about determinism of sdg creation.
			// TODO: this is somewhat of an abuse of sdg.isParallel(). Maybe rename that?
			if (sdg.isParallel()) {
				faccs = new OrdinalSet<FieldAccess>(bv.getValue(), genReach.getLatticeValues()).iterator();
			} else {
			  faccs = new OrdinalSet<FieldAccess>(bv.getValue(), genReach.getLatticeValues()).iteratorOrdinalSorted();
			}
			
			while (faccs.hasNext()) {
				FieldAccess facc = faccs.next();
				if (facc.isWrite) {
					pdg.addStaticWrite(facc.field);
				} else {
					pdg.addStaticRead(facc.field);
				}
			}
			MonitorUtil.throwExceptionIfCanceled(progress);
		}

		// propagate new formal-in/-out nodes to callsites
		for (final PDG pdg : sdg.getAllPDGs()) {
			for (final PDGNode call : pdg.getCalls()) {
				final Set<PDG> tgts = sdg.getPossibleTargets(call);
				for (final PDG target : tgts) {
					// get new interproc reads
					for (final PDGField read : target.staticReads) {
						final PDGNode actIn = pdg.addStaticReadToCall(call, read.field);
						pdg.addVertex(read.node);
						pdg.addEdge(actIn, read.node, PDGEdge.Kind.PARAMETER_IN);
					}

					for (final PDGField read : target.staticInterprocReads) {
						final PDGNode actIn = pdg.addStaticReadToCall(call, read.field);
						pdg.addVertex(read.node);
						pdg.addEdge(actIn, read.node, PDGEdge.Kind.PARAMETER_IN);
					}

					for (final PDGField write : target.staticWrites) {
						final PDGNode actOut = pdg.addStaticWriteToCall(call, write.field);
						target.addVertex(actOut);
						target.addEdge(write.node, actOut, PDGEdge.Kind.PARAMETER_OUT);
					}

					for (final PDGField write : target.staticInterprocWrites) {
						final PDGNode actOut = pdg.addStaticWriteToCall(call, write.field);
						target.addVertex(actOut);
						target.addEdge(write.node, actOut, PDGEdge.Kind.PARAMETER_OUT);
					}
				}
			}
		}
	}

	private static final class FieldAccess {
		public final ParameterField field;
		public final boolean isWrite;

		private FieldAccess(ParameterField field, boolean isWrite) {
			this.field = field;
			this.isWrite = isWrite;
		}

		public int hashCode() {
			return field.hashCode() * (isWrite ? 2 : 1);
		}

		public boolean equals(Object obj) {
			if (this == obj) {
				return true;
			}

			if (obj instanceof FieldAccess) {
				FieldAccess facc = (FieldAccess) obj;
				return facc.isWrite == isWrite && facc.field.equals(field);
			}

			return false;
		}
		
		public String toString() {
			return field.toString() + (isWrite ? "[w]" : "[r]");
		}
	}

	private Map<PDG, Collection<FieldAccess>> createStaticFieldAccessMap() {
		final List<PDG> allPdgs = sdg.getAllPDGs();
		// We want to avoid calls to org.jgrapht.graph.AbstractGraph.hashCode(),
		// but an IdentityHashMap creates indeterminate PDGNode numbers :(
		final Map<PDG, Collection<FieldAccess>> access = new TreeMap<>(new Comparator<PDG>() {
			@Override
			public int compare(PDG o1, PDG o2) {
				return Integer.compare(o1.getId(), o2.getId()); 
			}
		});

		
		for (PDG pdg : allPdgs) {
			Set<FieldAccess> statics = new HashSet<FieldAccess>();
			access.put(pdg, statics);

			for (PDGField read : pdg.staticReads) {
				FieldAccess facc = new FieldAccess(read.field, false);
				statics.add(facc);
			}

			for (PDGField write : pdg.staticWrites) {
				FieldAccess facc = new FieldAccess(write.field, true);
				statics.add(facc);
			}
			
			if (IS_DEBUG && statics.size() > 10) {
				System.out.println(pdg.toString() + ": " + statics.size());
				System.out.println("\t" + statics);
			}
		}

		return access;
	}

//	private OrdinalSetMapping<ParameterField> createStaticFieldsMapping() {
//		Set<ParameterField> sFields = new HashSet<ParameterField>();
//		for (PDG pdg : sdg.getAllPDGs()) {
//			for (PDGField read : pdg.staticReads) {
//				sFields.add(read.field);
//			}
//
//			for (PDGField write : pdg.staticWrites) {
//				sFields.add(write.field);
//			}
//		}
//		ParameterField[] arr = new ParameterField[sFields.size()];
//		sFields.toArray(arr);
//		OrdinalSetMapping<ParameterField> mapping = new ObjectArrayMapping<ParameterField>(arr);
//
//		return mapping;
//	}

}
