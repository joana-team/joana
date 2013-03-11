/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.wala.core.killdef;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.ibm.wala.cfg.ControlFlowGraph;
import com.ibm.wala.cfg.exc.ExceptionPruningAnalysis;
import com.ibm.wala.cfg.exc.NullPointerAnalysis;
import com.ibm.wala.cfg.exc.intra.MutableCFG;
import com.ibm.wala.classLoader.IField;
import com.ibm.wala.dataflow.graph.AbstractMeetOperator;
import com.ibm.wala.dataflow.graph.BitVectorFramework;
import com.ibm.wala.dataflow.graph.BitVectorIdentity;
import com.ibm.wala.dataflow.graph.BitVectorKillGen;
import com.ibm.wala.dataflow.graph.BitVectorSolver;
import com.ibm.wala.dataflow.graph.BitVectorUnion;
import com.ibm.wala.dataflow.graph.ITransferFunctionProvider;
import com.ibm.wala.fixpoint.BitVectorVariable;
import com.ibm.wala.fixpoint.UnaryOperator;
import com.ibm.wala.ipa.cha.IClassHierarchy;
import com.ibm.wala.ssa.IR;
import com.ibm.wala.ssa.SSAArrayLengthInstruction;
import com.ibm.wala.ssa.SSAArrayLoadInstruction;
import com.ibm.wala.ssa.SSAArrayStoreInstruction;
import com.ibm.wala.ssa.SSABinaryOpInstruction;
import com.ibm.wala.ssa.SSACheckCastInstruction;
import com.ibm.wala.ssa.SSAComparisonInstruction;
import com.ibm.wala.ssa.SSAConditionalBranchInstruction;
import com.ibm.wala.ssa.SSAConversionInstruction;
import com.ibm.wala.ssa.SSAGetCaughtExceptionInstruction;
import com.ibm.wala.ssa.SSAGetInstruction;
import com.ibm.wala.ssa.SSAGotoInstruction;
import com.ibm.wala.ssa.SSAInstanceofInstruction;
import com.ibm.wala.ssa.SSAInstruction;
import com.ibm.wala.ssa.SSAInvokeInstruction;
import com.ibm.wala.ssa.SSALoadMetadataInstruction;
import com.ibm.wala.ssa.SSAMonitorInstruction;
import com.ibm.wala.ssa.SSANewInstruction;
import com.ibm.wala.ssa.SSAPhiInstruction;
import com.ibm.wala.ssa.SSAPiInstruction;
import com.ibm.wala.ssa.SSAPutInstruction;
import com.ibm.wala.ssa.SSAReturnInstruction;
import com.ibm.wala.ssa.SSASwitchInstruction;
import com.ibm.wala.ssa.SSAThrowInstruction;
import com.ibm.wala.ssa.SSAUnaryOpInstruction;
import com.ibm.wala.ssa.analysis.ExplodedControlFlowGraph;
import com.ibm.wala.ssa.analysis.IExplodedBasicBlock;
import com.ibm.wala.util.CancelException;
import com.ibm.wala.util.MonitorUtil.IProgressMonitor;
import com.ibm.wala.util.WalaException;
import com.ibm.wala.util.graph.Acyclic;
import com.ibm.wala.util.graph.GraphIntegrity.UnsoundGraphException;
import com.ibm.wala.util.graph.dominators.Dominators;
import com.ibm.wala.util.intset.BitVector;
import com.ibm.wala.util.intset.IBinaryNaturalRelation;
import com.ibm.wala.util.intset.IntPair;
import com.ibm.wala.util.intset.MutableMapping;
import com.ibm.wala.util.intset.OrdinalSet;
import com.ibm.wala.util.intset.OrdinalSetMapping;

import edu.kit.joana.wala.core.PDG;
import edu.kit.joana.wala.core.PDGEdge;
import edu.kit.joana.wala.core.PDGField;
import edu.kit.joana.wala.core.PDGNode;
import edu.kit.joana.wala.core.ParameterField;
import edu.kit.joana.wala.core.ParameterFieldFactory;
import edu.kit.joana.wala.core.SDGBuilder;
import edu.kit.joana.wala.core.killdef.Access.Kind;
import edu.kit.joana.wala.flowless.util.DotUtil;
import edu.kit.joana.wala.flowless.util.ExtendedNodeDecorator;

/**
 *
 * @author Juergen Graf <juergen.graf@gmail.com>
 *
 */
public final class LocalKillingDefs {

	private static final boolean DEBUG_PRINT = false;
	private static final boolean INFO_PRINT = false;

	private final SDGBuilder sdg;
	private final PDG pdg;
	private final IR ir;
	private final IClassHierarchy cha;
	private final ParameterFieldFactory pfact;
	private final AccessManager<IExplodedBasicBlock> accesses;
	private int totalRemoved = 0;

	public static int run(final SDGBuilder sdg, final IProgressMonitor progress) throws CancelException {
		if (DEBUG_PRINT) System.out.println("\n>>>> local killing definitions");

		int removed = 0;

		for (final PDG pdg : sdg.getAllPDGs()) {
			final IR ir = pdg.cgNode.getIR();
			if (ir != null) {
				removed += run(sdg, pdg, progress);
			}
		}

		if (INFO_PRINT) {
			System.out.println(sdg.getMainMethodName() + " - killed " + removed + " edges in total.");
		}
		if (DEBUG_PRINT) {
			System.out.println("<<<< local killing definitions");
		}

		return removed;
	}

	public static int run(final SDGBuilder sdg, final PDG pdg, final IProgressMonitor progress)
			throws CancelException {
		final LocalKillingDefs lkd = new LocalKillingDefs(sdg, pdg);
		lkd.run(progress);

		return lkd.totalRemoved;
	}

	private LocalKillingDefs(final SDGBuilder sdg, final PDG pdg) {
		this.sdg = sdg;
		this.pdg = pdg;
		this.ir = pdg.cgNode.getIR();
		this.cha = sdg.getClassHierarchy();
		this.pfact = sdg.getParameterFieldFactory();
		this.accesses = new AccessManager<IExplodedBasicBlock>(pdg.cgNode);
	}

	public static ControlFlowGraph<SSAInstruction,IExplodedBasicBlock> stripBackEdges(
			final ExplodedControlFlowGraph cfg) {
		final IBinaryNaturalRelation backEdges = Acyclic.computeBackEdges(cfg, cfg.entry());
	    boolean hasBackEdge = backEdges.iterator().hasNext();
	    if (hasBackEdge) {
	    	final MutableCFG<SSAInstruction, IExplodedBasicBlock> cfg2 = MutableCFG.copyFrom(cfg);

	    	for (final IntPair edge : backEdges) {
	    		final IExplodedBasicBlock from = cfg2.getNode(edge.getX());
	    		final IExplodedBasicBlock to = cfg2.getNode(edge.getY());
		        cfg2.removeEdge(from, to);
		        cfg2.addEdge(from, cfg.exit());
	    	}

	    	return cfg2;
	    } else {
	    	return cfg;
	    }

	}

	@SuppressWarnings("unused")
	private void run(final IProgressMonitor progress) throws CancelException {
		final AccessCreationVisitor acv = new AccessCreationVisitor();
		ir.visitAllInstructions(acv);

		if (!acv.hasWrite) {
			// skip methods without write statements. There are no killing definitions here ;)
			if (DEBUG_PRINT) System.out.println(pdg.getMethod().getName() + ": no writes - skipping.");
			return;
		}

		if (DEBUG_PRINT) System.out.println(pdg.getMethod().getName() + ":");

		final ExceptionPruningAnalysis<SSAInstruction, IExplodedBasicBlock> npe =
				NullPointerAnalysis.createIntraproceduralExplodedCFGAnalysis(ir);
		try {
			npe.compute(progress);
		} catch (UnsoundGraphException e1) {
			throw new CancelException(e1);
		}
		
		final ControlFlowGraph<SSAInstruction, IExplodedBasicBlock> ecfg = npe.getCFG();

		for (final IExplodedBasicBlock bb : ecfg) {
			final SSAInstruction instr = bb.getInstruction();
			if (instr != null) {
				final Access<IExplodedBasicBlock> a = accesses.getAccForInstr(instr);
				accesses.mapNodeToAccess(bb, a);
			}
		}

		if (DEBUG_PRINT) {
			try {
				DotUtil.dotify(ecfg, ecfg, new ExtendedNodeDecorator() {
					
					@Override
					public String getLabel(Object o) throws WalaException {
						if (o instanceof IExplodedBasicBlock) {
							final IExplodedBasicBlock eb = (IExplodedBasicBlock) o;
							final SSAInstruction sa = eb.getInstruction();
							if (sa != null) {
								final Access<IExplodedBasicBlock> acc = accesses.getAccForInstr(sa);
								if (acc != null) {
									return acc.toString();
								} else {
									return edu.kit.joana.wala.flowless.util.Util.prettyShortInstruction(sa);
								}
							} else {
								return (eb.isEntryBlock() ? "entry" 
										: (eb.isExitBlock() ? "exit" :
											(eb.isCatchBlock() ? "catch" : "nop")));
							}
						}
						return ExtendedNodeDecorator.DEFAULT.getLabel(o);
					}
					
					@Override
					public String getShape(Object o) throws WalaException {
						return ExtendedNodeDecorator.DEFAULT.getShape(o);
					}
					
					@Override
					public String getColor(Object o) throws WalaException {
						return ExtendedNodeDecorator.DEFAULT.getColor(o);
					}
				}, pdg.getMethod().getName() + "-cfg.dot", progress);
			} catch (WalaException e) {
				e.printStackTrace();
			}
		}
		
		final List<FieldAccess<IExplodedBasicBlock>> initialWrites = accesses.createAdditionalInitialWrites();

		for (final FieldAccess<IExplodedBasicBlock> fa : initialWrites) {
			accesses.mapNodeToAccess(ecfg.entry(), fa);
		}

		// build flow graph without back edges
		final ControlFlowGraph<SSAInstruction, IExplodedBasicBlock> flow = ecfg; // = stripBackEdges(ecfg);
		final Dominators<IExplodedBasicBlock> dom = Dominators.make(flow, flow.entry());
		final Map<IExplodedBasicBlock, OrdinalSet<Access<IExplodedBasicBlock>>> mayReach =
				accesses.computeMayReachableAccesses(flow, progress);
		final Map<IExplodedBasicBlock, OrdinalSet<Access<IExplodedBasicBlock>>> mustReach =
				accesses.computeMustReachableAccesses(flow, progress);
		
		if (DEBUG_PRINT) {
			for (IExplodedBasicBlock bb : ecfg) {
				System.out.print("bb" + bb.getGraphNodeId() + " must-reach: ");
				OrdinalSet<Access<IExplodedBasicBlock>> must = mustReach.get(bb);
				if (must != null) {
					for (Access<IExplodedBasicBlock> a : must) {
						System.out.print("bb" + a.getNode().getGraphNodeId() + " ");
					}
				} else {
					System.out.print("<null>");
				}
				System.out.println();
			}
		}
		
		final IFieldsMayMod fMayMod = sdg.getFieldsMayMod();
		final Reachability<IExplodedBasicBlock> reach =
				new Reachability<IExplodedBasicBlock>(pdg.cgNode, fMayMod, mayReach, mustReach, dom);
		accesses.computeEquivalenceClasses(reach, progress);

		// then: do standard kill/gen analysis
		final Map<FieldAccess<IExplodedBasicBlock>, Set<FieldAccess<IExplodedBasicBlock>>> mustKill =
				accesses.buildMustKillMap();

		if (DEBUG_PRINT) {
			for (FieldAccess<IExplodedBasicBlock> fa : mustKill.keySet()) {
				System.out.print("bb" + fa.getNode().getGraphNodeId() + " kills: ");
				Set<FieldAccess<IExplodedBasicBlock>> must = mustKill.get(fa);
				if (must != null) {
					for (FieldAccess<IExplodedBasicBlock> a : must) {
						System.out.print("bb" + a.getNode().getGraphNodeId() + " ");
					}
				} else {
					System.out.print("<null>");
				}
				System.out.println();
			}
		}
		
		final OrdinalSetMapping<FieldAccess<IExplodedBasicBlock>> map =
				new MutableMapping<FieldAccess<IExplodedBasicBlock>>(new FieldAccess[1]);
		for (final FieldAccess<IExplodedBasicBlock> write : accesses.getWrites()) {
			map.add(write);
		}

		if (DEBUG_PRINT) {
			System.out.println(map);
		}
		
		final LocalKillTransferFunctions lktf = LocalKillTransferFunctions.build(ecfg, accesses, mustKill, map, fMayMod);

		// debug out
		if (DEBUG_PRINT) {
			for (final FieldAccess<IExplodedBasicBlock> n1 : accesses.getReads()) {
				System.out.println(n1.id + ":" + n1);
			}
			for (final FieldAccess<IExplodedBasicBlock> n1 : accesses.getWrites()) {
				System.out.println(n1.id + ":" + n1);
			}
		}

		if (DEBUG_PRINT) System.out.print("Solving data flow problem");
		final BitVectorFramework<IExplodedBasicBlock, FieldAccess<IExplodedBasicBlock>> lkf =
				new BitVectorFramework<IExplodedBasicBlock, FieldAccess<IExplodedBasicBlock>>(ecfg, lktf, map);
		if (DEBUG_PRINT) System.out.print(".");
		final BitVectorSolver<IExplodedBasicBlock> solver = new BitVectorSolver<IExplodedBasicBlock>(lkf);
		if (DEBUG_PRINT) System.out.print(".");
		solver.solve(progress);
		if (DEBUG_PRINT) System.out.println("done.");

		final Map<FieldAccess<IExplodedBasicBlock>, Set<FieldAccess<IExplodedBasicBlock>>> mustRead =
				accesses.buildMustReadMap();

		if (DEBUG_PRINT) {
			for (FieldAccess<IExplodedBasicBlock> fa : mustRead.keySet()) {
				Set<FieldAccess<IExplodedBasicBlock>> mreads = mustRead.get(fa);
				System.out.print("bb" + fa.getNode().getGraphNodeId() + " must-read: ");
				for (FieldAccess<IExplodedBasicBlock> r : mreads) {
					System.out.print("bb" + r.getNode().getGraphNodeId() + " ");
				}
				System.out.println();
			}
		}
		
		for (final FieldAccess<IExplodedBasicBlock> r : accesses.getReads()) {
			final IExplodedBasicBlock node = r.getNode();
			if (node.isEntryBlock()) continue;

			final BitVectorVariable bvIn = solver.getIn(node);
			final OrdinalSet<FieldAccess<IExplodedBasicBlock>> inSet =
					new OrdinalSet<FieldAccess<IExplodedBasicBlock>>(bvIn.getValue(), map);
			
			if (DEBUG_PRINT) {
				System.out.print("bb" + r.getNode().getGraphNodeId() + " in: ");
				for (FieldAccess<IExplodedBasicBlock> is : inSet) {
					System.out.print("bb" + is.getNode().getGraphNodeId() + " ");
				}
				System.out.println();
			}
			
			final Set<FieldAccess<IExplodedBasicBlock>> nodeReads = mustRead.get(r);
			if (nodeReads != null) {
				for (final FieldAccess<IExplodedBasicBlock> mayRead : nodeReads) {
					if (!inSet.contains(mayRead)) {
						// killed read - remove data deps iff there are any
						final int removed = removePotentialDataDep(mayRead, r);
						if (DEBUG_PRINT && removed > 0) System.out.println(r + ": killed read from " + mayRead + ": "
								+ removed + " edges removed");

						totalRemoved += removed;
					}
				}
			}
		}

		if (DEBUG_PRINT) System.out.println();
	}

	private int removePotentialDataDep(final FieldAccess<IExplodedBasicBlock> from,
			final FieldAccess<IExplodedBasicBlock> to) {
		int removedEdges = 0;
		final PDGField fieldTo = findFieldAccess(to);

		if (fieldTo == null) {
			return removedEdges;
		}

		if (from.getNode().isEntryBlock()) {
			// remove all deps from formal-in nodes
			final List<PDGEdge> toRemove = new LinkedList<PDGEdge>();
			for (final PDGEdge e : pdg.incomingEdgesOf(fieldTo.accfield)) {
				if ((e.kind == PDGEdge.Kind.DATA_HEAP || e.kind == PDGEdge.Kind.DATA_ALIAS)
						&& e.from.getKind() == PDGNode.Kind.FORMAL_IN) {
					if (INFO_PRINT) {
						System.out.println("remove " + e.from.getId() + "(" + e.from.getLabel() + ")->"
								+ e.to.getId() + "(" + e.to.getLabel() + ")");
					}
					toRemove.add(e);
				}
			}

			removedEdges += toRemove.size();

			pdg.removeAllEdges(toRemove);
		} else {
			// search matching write statement and remove potential edges
			final PDGField fieldFrom = findFieldAccess(from);

			if (fieldFrom != null) {
				final List<PDGEdge> toRemove = new LinkedList<PDGEdge>();
				for (final PDGEdge e : pdg.incomingEdgesOf(fieldTo.accfield)) {
					if ((e.kind == PDGEdge.Kind.DATA_HEAP || e.kind == PDGEdge.Kind.DATA_ALIAS)
							&& e.from == fieldFrom.accfield) {
						if (INFO_PRINT) {
							System.out.println("remove " + e.from.getId() + "(" + e.from.getLabel() + ")->"
									+ e.to.getId() + "(" + e.to.getLabel() + ")");
						}
						toRemove.add(e);
					}
				}

				removedEdges += toRemove.size();

				pdg.removeAllEdges(toRemove);
			}
		}

		return removedEdges;
	}

	private PDGField findFieldAccess(final FieldAccess<IExplodedBasicBlock> fa) {
		final SSAInstruction instr = fa.getNode().getInstruction();
		final PDGNode n = pdg.getNode(instr);
		final List<PDGField> fields = (fa.isRead() ? pdg.getFieldReads() : pdg.getFieldWrites());

		for (final PDGField fRead : fields) {
			if (fRead.node == n) {
				return fRead;
			}
		}

		return null;
	}

	private static class LocalKillTransferFunctions implements ITransferFunctionProvider<IExplodedBasicBlock, BitVectorVariable> {

		private final Map<IExplodedBasicBlock, BitVector> bb2kill;
		private final Map<IExplodedBasicBlock, BitVector> bb2gen;

		private static final BitVector EMPTY = new BitVector();

		public static LocalKillTransferFunctions build(
				final ControlFlowGraph<SSAInstruction, IExplodedBasicBlock> cfg,
				final AccessManager<IExplodedBasicBlock> access,
				final Map<FieldAccess<IExplodedBasicBlock>, Set<FieldAccess<IExplodedBasicBlock>>> mustKill,
				final OrdinalSetMapping<FieldAccess<IExplodedBasicBlock>> map, final IFieldsMayMod fMayMod) {

			final Map<IExplodedBasicBlock, BitVector> bb2kill = new HashMap<IExplodedBasicBlock, BitVector>();
			final Map<IExplodedBasicBlock, BitVector> bb2gen = new HashMap<IExplodedBasicBlock, BitVector>();

			for (final IExplodedBasicBlock bb : cfg) {
				final Set<Access<IExplodedBasicBlock>> accs = access.getAccess(bb);
				final BitVector kill = createKill(accs, mustKill, map);
				bb2kill.put(bb, kill);
				final BitVector gen = createGen(accs, map, access, fMayMod);
				bb2gen.put(bb, gen);
			}
			
			if (DEBUG_PRINT) {
				for (final IExplodedBasicBlock bb : cfg) {
					final BitVector kill = bb2kill.get(bb);
					System.out.println("bb" + bb.getGraphNodeId() + " kill: " + kill);
					final BitVector gen = bb2gen.get(bb);
					System.out.println("bb" + bb.getGraphNodeId() + " gen:  " + gen);
				}
			}

			return new LocalKillTransferFunctions(bb2kill, bb2gen);
		}

		private static BitVector createKill(final Set<Access<IExplodedBasicBlock>> accs,
				final Map<FieldAccess<IExplodedBasicBlock>, Set<FieldAccess<IExplodedBasicBlock>>> mustKill,
				final OrdinalSetMapping<FieldAccess<IExplodedBasicBlock>> map) {
			if (accs == null || accs.isEmpty()) {
				return null;
			}

			final BitVector bv = new BitVector();

			for (final Access<IExplodedBasicBlock> a : accs) {
				if (!a.isWrite()) continue;

				final FieldAccess<IExplodedBasicBlock> fa = (FieldAccess<IExplodedBasicBlock>) a;
				final Set<FieldAccess<IExplodedBasicBlock>> kill = mustKill.get(fa);

				if (kill != null) {
					for (final FieldAccess<IExplodedBasicBlock> k : kill) {
						final int id = map.getMappedIndex(k);
						bv.set(id);
					}
				}
			}

			return (bv.isZero() ? null : bv);
		}

		private static BitVector createGen(final Set<Access<IExplodedBasicBlock>> accs,
				final OrdinalSetMapping<FieldAccess<IExplodedBasicBlock>> map,
				final AccessManager<IExplodedBasicBlock> accManager, final IFieldsMayMod fMayMod) {
			if (accs == null || accs.isEmpty()) {
				return null;
			}

			final BitVector bv = new BitVector();

			for (final Access<IExplodedBasicBlock> a : accs) {
				if (a.isWrite()) {
					final FieldAccess<IExplodedBasicBlock> fa = (FieldAccess<IExplodedBasicBlock>) a;
					final int id = map.getMappedIndex(fa);
					bv.set(id);
				} else if (a.getKind() == Kind.CALL) {
					// a call does not "reactivate" previously killed writes, it may kill active ones. But
					// as we are only interested in must-kills, it is save to assume that is does nothing.
//					final CallSiteReference csr = a.getCallSite();
//					final CGNode method = accManager.getMethod();
//					for (final FieldAccess<IExplodedBasicBlock> w : accManager.getWrites()) {
//						final ParameterField field = w.getField();
//						if (fMayMod.mayCallModField(method, csr, field)) {
//							final int id = map.getMappedIndex(w);
//							bv.set(id);
//						}
//					}
				}
			}

			return (bv.isZero() ? null : bv);
		}

		private LocalKillTransferFunctions(final Map<IExplodedBasicBlock, BitVector> bb2kill,
				final Map<IExplodedBasicBlock, BitVector> bb2gen) {
			this.bb2gen = bb2gen;
			this.bb2kill = bb2kill;
		}

		@Override
		public UnaryOperator<BitVectorVariable> getNodeTransferFunction(final IExplodedBasicBlock node) {
			final BitVector kill = bb2kill.get(node);
			final BitVector gen = bb2gen.get(node);

			if (kill == null && gen == null) {
				return BitVectorIdentity.instance();
			} else if (kill != null && gen == null) {
				return new BitVectorKillGen(kill, EMPTY);
			} else if (kill == null && gen != null) {
				return new BitVectorKillGen(EMPTY, gen);
			} else {
				return new BitVectorKillGen(kill, gen);
			}
		}

		@Override
		public boolean hasNodeTransferFunctions() {
			return true;
		}

		@Override
		public UnaryOperator<BitVectorVariable> getEdgeTransferFunction(IExplodedBasicBlock src, IExplodedBasicBlock dst) {
			return BitVectorIdentity.instance();
		}

		@Override
		public boolean hasEdgeTransferFunctions() {
			return false;
		}

		@Override
		public AbstractMeetOperator<BitVectorVariable> getMeetOperator() {
			return BitVectorUnion.instance();
		}

	}

	private class AccessCreationVisitor extends SSAInstruction.Visitor {

		private boolean hasWrite = false;

	    public void visitInvoke(final SSAInvokeInstruction ii) {
	    	accesses.addCall(ii.iindex, ii.getCallSite());
	    }

	    public void visitArrayLoad(final SSAArrayLoadInstruction ii) {
	    	final int base = ii.getArrayRef();
	    	final int index = ii.getIndex();
	    	final ParameterField field = pfact.getArrayField(ii.getElementType());
	    	final int value = ii.getDef();
	    	accesses.addArrayRead(ii.iindex, field, base, index, value);
	    }

	    public void visitArrayStore(final SSAArrayStoreInstruction ii) {
	    	hasWrite = true;
	    	final int base = ii.getArrayRef();
	    	final int index = ii.getIndex();
	    	final ParameterField field = pfact.getArrayField(ii.getElementType());
	    	final int value = ii.getValue();
	    	accesses.addArrayWrite(ii.iindex, field, base, index, value);
	    }

	    public void visitGet(final SSAGetInstruction ii) {
	    	final IField f = cha.resolveField(ii.getDeclaredField());
	    	if (f == null) {
	    		defaultVisit(ii);
	    		return;
	    	}
	    	final ParameterField field = pfact.getObjectField(f);

	    	if (ii.isStatic()) {
		    	final int value = ii.getDef();
		    	accesses.addStaticRead(ii.iindex, field, value);
	    	} else {
		    	final int base = ii.getRef();
		    	final int value = ii.getDef();
		    	accesses.addFieldRead(ii.iindex, field, base, value);
	    	}
	    }

	    public void visitPut(final SSAPutInstruction ii) {
	    	final IField f = cha.resolveField(ii.getDeclaredField());
	    	if (f == null) {
	    		defaultVisit(ii);
	    		return;
	    	}
	    	hasWrite = true;
	    	final ParameterField field = pfact.getObjectField(f);

	    	if (ii.isStatic()) {
		    	final int value = ii.getVal();
		    	accesses.addStaticWrite(ii.iindex, field, value);
	    	} else {
		    	final int base = ii.getRef();
		    	final int value = ii.getVal();
		    	accesses.addFieldWrite(ii.iindex, field, base, value);
	    	}
	    }

	    private void defaultVisit(final SSAInstruction ii) {
	    	if (ii.iindex >= 0) {
	    		accesses.addDummy(ii.iindex);
	    	}
	    }

		@Override public void visitGoto(final SSAGotoInstruction ii) 								{defaultVisit(ii);}
		@Override public void visitBinaryOp(final SSABinaryOpInstruction ii) 						{defaultVisit(ii);}
		@Override public void visitUnaryOp(final SSAUnaryOpInstruction ii) 							{defaultVisit(ii);}
		@Override public void visitConversion(final SSAConversionInstruction ii) 					{defaultVisit(ii);}
		@Override public void visitComparison(final SSAComparisonInstruction ii) 					{defaultVisit(ii);}
		@Override public void visitConditionalBranch(final SSAConditionalBranchInstruction ii) 		{defaultVisit(ii);}
		@Override public void visitSwitch(final SSASwitchInstruction ii) 							{defaultVisit(ii);}
		@Override public void visitReturn(final SSAReturnInstruction ii) 							{defaultVisit(ii);}
		@Override public void visitNew(final SSANewInstruction ii) 									{defaultVisit(ii);}
		@Override public void visitArrayLength(final SSAArrayLengthInstruction ii) 					{defaultVisit(ii);}
		@Override public void visitThrow(final SSAThrowInstruction ii) 								{defaultVisit(ii);}
		@Override public void visitMonitor(final SSAMonitorInstruction ii) 							{defaultVisit(ii);}
		@Override public void visitCheckCast(final SSACheckCastInstruction ii) 						{defaultVisit(ii);}
		@Override public void visitInstanceof(final SSAInstanceofInstruction ii) 					{defaultVisit(ii);}
		@Override public void visitPhi(final SSAPhiInstruction ii) 									{defaultVisit(ii);}
		@Override public void visitPi(final SSAPiInstruction ii) 									{defaultVisit(ii);}
		@Override public void visitGetCaughtException(final SSAGetCaughtExceptionInstruction ii) 	{defaultVisit(ii);}
		@Override public void visitLoadMetadata(final SSALoadMetadataInstruction ii) 				{defaultVisit(ii);}

	}

}
