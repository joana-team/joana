/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.deprecated.jsdg.sdg.dataflow.noparampassing;

import java.util.List;
import java.util.Map;
import java.util.Set;

import com.ibm.wala.analysis.pointers.HeapGraph;
import com.ibm.wala.classLoader.IField;
import com.ibm.wala.dataflow.graph.AbstractMeetOperator;
import com.ibm.wala.dataflow.graph.BitVectorFramework;
import com.ibm.wala.dataflow.graph.BitVectorIdentity;
import com.ibm.wala.dataflow.graph.BitVectorKillGen;
import com.ibm.wala.dataflow.graph.BitVectorMinusVector;
import com.ibm.wala.dataflow.graph.BitVectorSolver;
import com.ibm.wala.dataflow.graph.BitVectorUnion;
import com.ibm.wala.dataflow.graph.BitVectorUnionVector;
import com.ibm.wala.dataflow.graph.ITransferFunctionProvider;
import com.ibm.wala.fixpoint.BitVectorVariable;
import com.ibm.wala.fixpoint.UnaryOperator;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.CallGraph;
import com.ibm.wala.ipa.callgraph.propagation.InstanceKey;
import com.ibm.wala.ipa.callgraph.propagation.PointerKey;
import com.ibm.wala.ssa.SSAArrayLengthInstruction;
import com.ibm.wala.ssa.SSAArrayLoadInstruction;
import com.ibm.wala.ssa.SSAArrayStoreInstruction;
import com.ibm.wala.ssa.SSAGetInstruction;
import com.ibm.wala.ssa.SSAInstruction;
import com.ibm.wala.ssa.SSAPutInstruction;
import com.ibm.wala.util.CancelException;
import com.ibm.wala.util.MonitorUtil.IProgressMonitor;
import com.ibm.wala.util.WalaException;
import com.ibm.wala.util.collections.HashMapFactory;
import com.ibm.wala.util.collections.HashSetFactory;
import com.ibm.wala.util.collections.ObjectArrayMapping;
import com.ibm.wala.util.intset.BitVector;
import com.ibm.wala.util.intset.IntSet;
import com.ibm.wala.util.intset.IntSetAction;
import com.ibm.wala.util.intset.OrdinalSet;
import com.ibm.wala.util.intset.OrdinalSetMapping;

import edu.kit.joana.deprecated.jsdg.sdg.PDG;
import edu.kit.joana.deprecated.jsdg.sdg.SDG;
import edu.kit.joana.deprecated.jsdg.sdg.nodes.AbstractPDGNode;
import edu.kit.joana.deprecated.jsdg.sdg.nodes.ExpressionNode;
import edu.kit.joana.deprecated.jsdg.sdg.nodes.ObjectField;
import edu.kit.joana.deprecated.jsdg.util.Log;

/**
 * Computes dataflow without parameter nodes using direct connects between statements of
 * different PDGs. Much faster but context-sensitivity is lost.
 *
 * @author Juergen Graf <graf@kit.edu>
 *
 */
public class DirectDataFlowComputation {

	private final SDG sdg;
	private final CallGraph cg;
	private final HeapGraph hg;
	private static final boolean doArrays = true;

	/**
	 * Look for each heap accessing node which other heap accessing node may
	 * happen afterwards and may be referring to the same location.
	 * @throws CancelException
	 * @throws WalaException
	 */
	public static void compute(SDG sdg, CallGraph cg, HeapGraph hg, IProgressMonitor progress)
	throws CancelException, WalaException {
		DirectDataFlowComputation ddfc = new DirectDataFlowComputation(sdg, cg, hg);

		ddfc.compute(progress);
	}

	private DirectDataFlowComputation(SDG sdg, CallGraph cg, HeapGraph hg) {
		this.sdg = sdg;
		this.cg = cg;
		this.hg = hg;
	}

	/**
	 * Look for each heap accessing node which other heap accessing node may
	 * happen afterwards and may be referring to the same location.
	 * @throws CancelException
	 * @throws WalaException
	 */
	private final void compute(IProgressMonitor progress) throws CancelException, WalaException {
		Log.info("Computing direct data dependencies - please stand by");

		final Map<CGNode, Set<HeapRead>> cg2read = HashMapFactory.make();
		final Map<CGNode, Set<HeapWrite>> cg2write = HashMapFactory.make();

		Set<HeapAccess> acc = HashSetFactory.make();

		for (PDG pdg : sdg.getAllContainedPDGs()) {
			if (pdg == null) {
				continue;
			}

			Set<HeapWrite> writes = getHeapWrites(pdg);
			cg2write.put(pdg.getCallGraphNode(), writes);
			acc.addAll(writes);

			Set<HeapRead> reads = getHeapReads(pdg);
			cg2read.put(pdg.getCallGraphNode(), reads);
			acc.addAll(reads);

			if (progress.isCanceled()) {
				throw CancelException.make("Computing direct data dependencies canceled.");
			}

			progress.worked(1);
		}

		OrdinalSetMapping<HeapAccess> map = createMapping(acc);

		DataFlow df = new DataFlow(cg2read, cg2write, acc, map);
		df.compute(progress);
	}

	private final class GlobalRDTransfer implements ITransferFunctionProvider<CGNode, BitVectorVariable> {

		private final Map<CGNode, BitVector> cg2kill;
		private final Map<CGNode, BitVector> cg2gen;

		private final BitVector empty = new BitVector();

		private GlobalRDTransfer(Map<CGNode, BitVector> cg2kill, Map<CGNode, BitVector> cg2gen) {
			this.cg2gen = cg2gen;
			this.cg2kill = cg2kill;
		}

		private BitVector kill(CGNode node) {
			BitVector bv = cg2kill.get(node);

			return (bv != null ? bv : empty);
		}

		private BitVector gen(CGNode node) {
			BitVector bv = cg2gen.get(node);

			return (bv != null ? bv : empty);
		}

		public UnaryOperator<BitVectorVariable> getEdgeTransferFunction(
				CGNode src, CGNode dst) {
	        BitVector kill = kill(src);
	        BitVector gen = gen(src);

	        if (kill == null && gen == null) {
	            return BitVectorIdentity.instance();
	        } else if (kill == null && gen != null) {
	            return new BitVectorUnionVector(gen);
	        } else if (kill != null && gen == null) {
	            return new BitVectorMinusVector(kill);
	        } else if (kill != null && gen != null) {
	            return new BitVectorKillGen(kill, gen);
	        } else {
	        	// this else part should never be excecuted as the if clauses
	        	// should be complete
	        	throw new IllegalStateException();
	        }
		}

		public AbstractMeetOperator<BitVectorVariable> getMeetOperator() {
			return BitVectorUnion.instance();
		}

		public UnaryOperator<BitVectorVariable> getNodeTransferFunction(
				CGNode node) {
			// UNREACHABLE
			assert false;

			return null;
		}

		public boolean hasEdgeTransferFunctions() {
			return true;
		}

		public boolean hasNodeTransferFunctions() {
			return false;
		}

	}

	private final class DataFlow {

		private final Map<CGNode, Set<HeapRead>> cg2read;
		private final Map<CGNode, Set<HeapWrite>> cg2write;
		private final Set<HeapAccess> all;
		private final OrdinalSetMapping<HeapAccess> mapping;

		private final Map<AbstractPDGNode, BitVector> kill;
		private final Map<AbstractPDGNode, BitVector> gen;

		private final Map<CGNode, BitVector> cg2kill;
		private final Map<CGNode, BitVector> cg2gen;

		private DataFlow(final Map<CGNode, Set<HeapRead>> cg2read,
				final Map<CGNode, Set<HeapWrite>> cg2write,
				final Set<HeapAccess> all,
				OrdinalSetMapping<HeapAccess> mapping) {
			this.cg2read = cg2read;
			this.cg2write = cg2write;
			this.all = all;
			this.mapping = mapping;
			this.kill = HashMapFactory.make();
			this.gen = HashMapFactory.make();
			this.cg2kill = HashMapFactory.make();
			this.cg2gen = HashMapFactory.make();
		}

		public void compute(IProgressMonitor monitor) throws CancelException {
			for (PDG pdg : sdg.getAllContainedPDGs()) {
				if (pdg == null) {
					continue;
				}

				BitVector pdgKill = new BitVector();
				BitVector pdgGen = new BitVector();
				CGNode node = pdg.getCallGraphNode();

				Set<HeapWrite> writes = cg2write.get(node);
				if (writes != null) {
					for (HeapWrite write : writes) {
						BitVector bvKill = getKill(write);
						kill.put(write.getExpr(), bvKill);
						pdgKill.or(bvKill);

						BitVector bvGen = getGen(write);
						gen.put(write.getExpr(), bvGen);
						pdgGen.or(bvGen);
					}
				}

				cg2kill.put(node, pdgKill);
				cg2gen.put(node, pdgGen);
			}

			// propagate global reaching defs on a per method basis
			GlobalRDTransfer transfer = new GlobalRDTransfer(cg2kill, cg2gen);

			BitVectorFramework<CGNode, HeapAccess> reachDef =
				new BitVectorFramework<CGNode, HeapAccess>(cg, transfer, mapping);

			BitVectorSolver<CGNode> solver = new BitVectorSolver<CGNode>(reachDef);

			solver.solve(monitor);

			// Build interproc data dependencies
			for (PDG pdg : sdg.getAllContainedPDGs()) {
				if (pdg == null) {
					continue;
				}

				CGNode node = pdg.getCallGraphNode();

				BitVectorVariable in = solver.getIn(node);
				final Set<HeapRead> reads = cg2read.get(node);
				IntSet iset = in.getValue();
				iset.foreach(new IntSetAction(){
					public void act(int x) {
						HeapAccess ha = mapping.getMappedObject(x);
						if (ha.isWrite()) {
							for (HeapRead read : reads) {
								if (read.isAliasing(ha)) {
									addDataDependency((HeapWrite) ha, read);
								}
							}
						} else {
							Log.warn("There should be no reads in the last reaching definitions: %1s", ha.getExpr());
						}
					}
				});
			}

			// TODO we are missing intraprocedural reaching defs - we are just assuming everything is reachable
			// and do not look at the controlflow
			Log.warn("TODO Compute reaching defs for the intraprocedural case you lazy bastard!");

			for (PDG pdg : sdg.getAllContainedPDGs()) {
				if (pdg == null) {
					continue;
				}

				CGNode node = pdg.getCallGraphNode();
				final Set<HeapRead> reads = cg2read.get(node);
				final Set<HeapWrite> writes = cg2write.get(node);

				for (HeapWrite write : writes) {
					for (HeapRead read : reads) {
						if (read.isAliasing(write)) {
							addDataDependency(write, read);
						}
					}
				}

			}
		}

		private final BitVector empty = new BitVector();

		/**
		 * Return a bitvector set of all heap accesses that may modify a location
		 * that read reads.
		 * @param read
		 * @return
		 */
		private BitVector getKill(HeapWrite read) {
			BitVector bv = new BitVector();

			boolean changed = false;
			for (HeapAccess ha : all) {
				if (ha.isWrite() && ha.isMustAliasing(read)) {
					bv.set(mapping.getMappedIndex(ha));
					changed = true;
				}
			}

			return (changed ? bv : empty);
		}

		/**
		 * Return a bitvector set of all heap accesses that may read a location
		 * that write modifies.
		 * @param write
		 * @return
		 */
		private BitVector getGen(HeapWrite write) {
			BitVector bv = new BitVector();

			bv.set(mapping.getMappedIndex(write));

			return bv;
		}

	}

	private final OrdinalSetMapping<HeapAccess> createMapping(Set<HeapAccess> acc) {
		HeapAccess arr[] = new HeapAccess[acc.size()];
		arr = acc.toArray(arr);

		OrdinalSetMapping<HeapAccess> domain =
			new ObjectArrayMapping<HeapAccess>(arr);

		return domain;
	}

	private final void addDataDependency(HeapWrite write, HeapRead read) {
		ExpressionNode ewrite = write.getExpr();
		PDG pdgWrite = sdg.getPdgForId(ewrite.getPdgId());
		ExpressionNode eread = read.getExpr();

		if (!pdgWrite.containsNode(eread)) {
			pdgWrite.addNode(eread);
		}

//		System.err.println(write + " -> " + read);

		pdgWrite.addHeapDataDependency(ewrite, eread);
	}

	private final ExpressionNode retrieveExpression(PDG pdg, SSAInstruction get) {
		List<AbstractPDGNode> nodes = pdg.getNodesForInstruction(get);

		assert (nodes != null);
		assert (nodes.size() == 1) : "More then one node found for a field access operation";
		assert (nodes.get(0) instanceof ExpressionNode) : "Field access node is not an expression: " + nodes.get(0);

		ExpressionNode expr = (ExpressionNode) nodes.get(0);

		return expr;
	}

	private final void addHeapReads(PDG pdg, SSAInstruction get, Set<HeapRead> hreads) {
		ExpressionNode expr = retrieveExpression(pdg, get);

		for (int i = 0; i < get.getNumberOfDefs(); i++) {
			OrdinalSet<InstanceKey> pt = pdg.getPointsToSet(get.getDef(i));

			HeapRead read = HeapAccess.createRead(expr, pt);
			hreads.add(read);
		}
	}

	private final Set<HeapRead> getHeapReads(PDG pdg) {
		Set<HeapRead> hreads = HashSetFactory.make();

		for (SSAGetInstruction get : pdg.getGets()) {
			addHeapReads(pdg, get, hreads);
		}

		if (doArrays) {
			for (SSAArrayLengthInstruction get : pdg.getArrayLengths()) {
				addHeapReads(pdg, get, hreads);
			}

			for (SSAArrayLoadInstruction get : pdg.getArrayGets()) {
				addHeapReads(pdg, get, hreads);
			}
		}

		return hreads;
	}

	private final boolean mayBeEscaping(PDG pdg, SSAPutInstruction set) {
		return true;
	}

	private final Set<HeapWrite> getHeapWrites(PDG pdg) throws WalaException {
		Set<HeapWrite> hwrites = HashSetFactory.make();

		for (SSAPutInstruction set : pdg.getSets()) {
			if (!mayBeEscaping(pdg, set)) {
				// skip writes to non-escaping objects
				continue;
			}

			ExpressionNode expr = retrieveExpression(pdg, set);

			if (expr.getField() == null) {
				Log.info("No Field found for '%1s'", set);
				continue;
			}

			OrdinalSet<InstanceKey> pt;
			if (set.isStatic()) {
				pt = pdg.getPointsToSet(expr.getField());
			} else {
				OrdinalSet<InstanceKey> roots = pdg.getPointsToSet(set.getRef());
				IField field = ((ObjectField) expr.getField()).getField();

				pt = OrdinalSet.empty();
				for (InstanceKey root : roots) {
					PointerKey pk = hg.getHeapModel().getPointerKeyForInstanceField(root, field);
					OrdinalSet<InstanceKey> ptsPk = hg.getPointerAnalysis().getPointsToSet(pk);
					pt = OrdinalSet.unify(pt, ptsPk);
				}
			}

			HeapWrite write = HeapAccess.createWrite(expr, pt);
			hwrites.add(write);
		}

		if (doArrays) {
			for (SSAArrayStoreInstruction set : pdg.getArraySets()) {
				ExpressionNode expr = retrieveExpression(pdg, set);

				OrdinalSet<InstanceKey> roots = pdg.getPointsToSet(set.getArrayRef());

				OrdinalSet<InstanceKey> pt = OrdinalSet.empty();
				for (InstanceKey root : roots) {
					PointerKey pk = hg.getHeapModel().getPointerKeyForArrayContents(root);
					OrdinalSet<InstanceKey> ptsPk = hg.getPointerAnalysis().getPointsToSet(pk);
					pt = OrdinalSet.unify(pt, ptsPk);
				}

				HeapWrite write = HeapAccess.createWrite(expr, pt);
				hwrites.add(write);
			}
		}
		return hwrites;
	}

	private static abstract class HeapAccess {
		private final ExpressionNode expr;
		private final OrdinalSet<InstanceKey> pts;

		private HeapAccess(ExpressionNode expr, OrdinalSet<InstanceKey> pts) {
			this.expr = expr;
			this.pts = pts;
		}

		public static HeapWrite createWrite(ExpressionNode expr,
				OrdinalSet<InstanceKey> pts) {
			HeapWrite hw = new HeapWrite(expr, pts);
			return hw;
		}

		public static HeapRead createRead(ExpressionNode expr,
				OrdinalSet<InstanceKey> pts) {
			HeapRead hr = new HeapRead(expr, pts);
			return hr;
		}

		public abstract boolean isWrite();

		public OrdinalSet<InstanceKey> getPts() {
			return pts;
		}

		public ExpressionNode getExpr() {
			return expr;
		}

		public String toString() {
			return expr.getLabel();
		}

		public boolean isAliasing(HeapAccess acc) {
			boolean emtpyBases = (pts == null && acc.pts == null);
			//|| (base != null && acc.base != null && base.isEmpty() && acc.base.isEmpty());

			return 	(emtpyBases || (pts != null && pts.containsAny(acc.pts)));
		}

		public boolean isMustAliasing(HeapAccess acc) {
			boolean emtpyBases = (pts == null && acc.pts == null);
			//|| (base != null && acc.base != null && base.isEmpty() && acc.base.isEmpty());

			return 	(!emtpyBases && (pts.size() == 1 && pts.containsAny(acc.pts)));
		}

	}

	private static final class HeapRead extends HeapAccess {

		private HeapRead(ExpressionNode expr, OrdinalSet<InstanceKey> pts) {
			super(expr, pts);
		}

		public boolean isWrite() {
			return false;
		}
	}

	private static final class HeapWrite extends HeapAccess {

		private HeapWrite(ExpressionNode expr, OrdinalSet<InstanceKey> pts) {
			super(expr, pts);
		}

		public boolean isWrite() {
			return true;
		}
	}

}
