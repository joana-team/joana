/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.ifc.wala.attestation;

import java.io.BufferedOutputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jgrapht.DirectedGraph;
import org.jgrapht.graph.EdgeReversedGraph;

import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.classLoader.ShrikeCTMethod;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.CallGraph;
import com.ibm.wala.ipa.callgraph.propagation.InstanceKey;
import com.ibm.wala.ipa.callgraph.propagation.PointerAnalysis;
import com.ibm.wala.ipa.cha.ClassHierarchyException;
import com.ibm.wala.shrikeCT.InvalidClassFileException;
import com.ibm.wala.ssa.IR;
import com.ibm.wala.ssa.SSACFG;
import com.ibm.wala.ssa.SSACFG.BasicBlock;
import com.ibm.wala.util.CancelException;
import com.ibm.wala.util.graph.GraphIntegrity.UnsoundGraphException;
import com.ibm.wala.util.intset.IntSet;
import com.ibm.wala.util.intset.MutableSparseIntSet;

import edu.kit.joana.api.IFCAnalysis;
import edu.kit.joana.api.lattice.BuiltinLattices;
import edu.kit.joana.api.sdg.SDGConfig;
import edu.kit.joana.api.sdg.SDGProgram;
import edu.kit.joana.graph.dominators.slca.DFSIntervalOrder;
import edu.kit.joana.ifc.sdg.core.SecurityNode;
import edu.kit.joana.ifc.sdg.core.violations.IIllegalFlow;
import edu.kit.joana.ifc.sdg.core.violations.IViolation;
import edu.kit.joana.ifc.sdg.core.violations.ViolationSeparator;
import edu.kit.joana.ifc.sdg.graph.SDG;
import edu.kit.joana.ifc.sdg.graph.SDGEdge;
import edu.kit.joana.ifc.sdg.graph.SDGNode;
import edu.kit.joana.ifc.sdg.graph.SDGSerializer;
import edu.kit.joana.ifc.sdg.graph.chopper.Chopper;
import edu.kit.joana.ifc.sdg.graph.chopper.RepsRosayChopper;
import edu.kit.joana.ifc.sdg.graph.slicer.graph.CFG;
import edu.kit.joana.ifc.sdg.graph.slicer.graph.building.ICFGBuilder;
import edu.kit.joana.ifc.sdg.util.sdg.GraphModifier;
import edu.kit.joana.util.Pair;
import edu.kit.joana.util.Stubs;
import edu.kit.joana.wala.core.CGConsumer;
import edu.kit.joana.wala.core.SDGBuilder.ExceptionAnalysis;
import edu.kit.joana.wala.core.graphs.DominanceFrontiers;
import edu.kit.joana.wala.core.graphs.Dominators;
import edu.kit.joana.wala.core.graphs.Dominators.DomEdge;

public class Main {
	private static class CGKeeper implements CGConsumer {
		public CallGraph cg;
		/* (non-Javadoc)
		 * @see edu.kit.joana.wala.core.CGConsumer#consume(com.ibm.wala.ipa.callgraph.CallGraph, com.ibm.wala.ipa.callgraph.propagation.PointerAnalysis)
		 */
		@Override
		public void consume(CallGraph cg, PointerAnalysis<? extends InstanceKey> pts) {
			// TODO Auto-generated method stub
			this.cg = cg;
		}
		
	}

	private SDGProgram buildSDG(String cp, String mainMethod, CGConsumer c) {
		SDGProgram program = null;
		SDGConfig config = new SDGConfig(cp, mainMethod, Stubs.JRE_14_INCOMPLETE);
		config.setCGConsumer(c);
		config.setExceptionAnalysis(ExceptionAnalysis.IGNORE_ALL);
		System.out.println(config.getExceptionAnalysis());
		try {
			program = SDGProgram.createSDGProgram(config);
		} catch (ClassHierarchyException | IOException | UnsoundGraphException | CancelException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return program;
	}

	private Collection<IIllegalFlow<SecurityNode>> getIllegalFlows(Collection<? extends IViolation<SecurityNode>> vios) {
		ViolationSeparator<SecurityNode> separator = new ViolationSeparator<>();
		for (IViolation<SecurityNode> vio : vios) {
			vio.accept(separator);
		}
		return separator.getIllegalFlows();
	}

	public void dumpSDG(SDG sdg) {
		try {
			BufferedOutputStream bOut = new BufferedOutputStream(new FileOutputStream("/ben/bischof/test/" + className + ".pdg"));
			SDGSerializer.toPDGFormat(sdg, bOut);
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	/*
	 * Get the bytecode method plus index of the given node n.
	 * If adjust = 0, return the bytecode index unchanged.
	 * If adjust < 0, return the first bytecode index of the relevant basic block (for jump successors).
	 * If adjust > 0, return the last bytecode index of the relevant basic block (for jumps).
	 */
	private Pair<String, Integer> getBCMethodAndIndexPair(SDGNode n, int adjust) {
		if (adjust == 0) {
			return Pair.pair(n.getBytecodeMethod(), n.getBytecodeIndex());
		}
		SDGNode cur = n;
		// find the first successor that is represented in the bytecode
		// (as opposed to synthetic nodes like actual-ins)
		while (cur.getBytecodeIndex() < 0) {
			List<SDGEdge> succs = sdg.getOutgoingEdgesOfKindUnsafe(cur, SDGEdge.Kind.CONTROL_FLOW);
			/*if (succs.size() != 1) {
				throw new AssertionError("Node " + cur + " has no unique successor!");
			}*/
			cur = succs.get(0).getTarget();
		}
		IR ir = null;
		for (CGNode cgn : cg) {
			// find the correct IR
			if (cur.getBytecodeName().equals(cgn.getMethod().getSignature())) {
				ir = cgn.getIR();
				break;
			}
		}
		ShrikeCTMethod m = (ShrikeCTMethod) ir.getMethod();
		try {
			// find the basic block the current instruction is in
			int bcIndex = cur.getBytecodeIndex();
			int instrIndex = m.getInstructionIndex(bcIndex);
			SSACFG ssacfg = ir.getControlFlowGraph();
			BasicBlock bb = ssacfg.getBlockForInstruction(instrIndex);
			// find the bytecode index of the first/last instruction of the basic block
			int firstInstrIndex = adjust < 0 ? bb.getFirstInstructionIndex() : bb.getLastInstructionIndex();
			return Pair.pair(cur.getBytecodeMethod(), m.getBytecodeIndex(firstInstrIndex));
		} catch (InvalidClassFileException e) {
			throw new AssertionError("Bytecode index for node" + n + "could not be found!");
		}
	}
	
	private String getBCMethodAndIndexString(SDGNode n, int adjust) {
		Pair<String, Integer> p = getBCMethodAndIndexPair(n, adjust);
		return p.getFirst() + " " + p.getSecond();
	}
	
	private int getBCIndex(SDGNode n, int adjust) {
		return getBCMethodAndIndexPair(n, adjust).getSecond();
	}
	
	private void printSDGNodeDebugInfo(SDGNode n, String prefix, int adjust) {
		System.out.println(prefix + "Node " + n.getId() + " " + getBCMethodAndIndexString(n, adjust));
	}

	private void nodeJumpDebugOutput() {
		for (Map.Entry<SDGNode, Map<SDGNode, List<SDGNode>>> entry1 : nodeJumps.entrySet()) {
			SDGNode n = entry1.getKey();
			printSDGNodeDebugInfo(n, "", 0);
			for (Map.Entry<SDGNode, List<SDGNode>> entry2 : entry1.getValue().entrySet()) {
				SDGNode d = entry2.getKey();
				printSDGNodeDebugInfo(d, "\t", 1);
				for (SDGNode t : entry2.getValue()) {
					printSDGNodeDebugInfo(t, "\tvia: ", -1);
				}
			}
		}
	}

	private void jumpsOutput() {
		for (Map.Entry<SDGNode, Map<SDGNode, List<SDGNode>>> entry1 : jumps.entrySet()) {
			SDGNode n = entry1.getKey();
			System.out.println(n.getId() + " " + getBCMethodAndIndexString(n, 1));
		}
	}

	private void jumpsAndTargetsOutput() {
		for (Map.Entry<SDGNode, Map<SDGNode, List<SDGNode>>> entry1 : jumps.entrySet()) {
			SDGNode n = entry1.getKey();
			System.out.println(n.getId() + " " + getBCMethodAndIndexString(n, 1));
			for (SDGNode d : entry1.getValue().keySet()) {
				System.out.print(" " + getBCIndex(d, -1));
			}
			System.out.println();
		}
	}

	private void jumpsAndTargetsAndNodesOutput() {
		for (Map.Entry<SDGNode, Map<SDGNode, List<SDGNode>>> entry1 : jumps.entrySet()) {
			SDGNode n = entry1.getKey();
			System.out.println(n.getId() + " " + getBCMethodAndIndexString(n, 1));
			for (Map.Entry<SDGNode, List<SDGNode>> entry2 : entry1.getValue().entrySet()) {
				SDGNode d = entry2.getKey();
				System.out.println(" " + getBCMethodAndIndexString(d, -1) + " " + entry2.getValue());
			}
		}
	}

	private Map<SDGNode, List<SDGNode>> getJumpsForNode(SDGNode n) {
		Set<SDGNode> doms = frontiers.getDominanceFrontier(n);
		Map<SDGNode, List<SDGNode>> domMap = new HashMap<>();
		for (SDGNode d : doms) {
			List<SDGNode> targets = new ArrayList<>();
			Map<SDGNode, List<SDGNode>> succsMap = jumps.get(d);
			for (SDGEdge e : sdg.outgoingEdgesOfUnsafe(d)) {
				SDGNode t = e.getTarget();
				if (e.getKind().isControlFlowEdge() && dio.isLeq(t, n)) {
					targets.add(t);
					succsMap.get(t).add(n);
				}
			}
			domMap.put(d, targets);
		}
		return domMap;
	}
	
	private void calculateJumpMap() {
		for (SDGNode n : sdg.vertexSet()) {
			int cgId = sdg.getCGNodeId(sdg.getEntry(n));
			if (cg.getNode(cgId).getMethod().isSynthetic()) {
				continue;
			}
			List<SDGEdge> succs = sdg.getOutgoingEdgesOfKindUnsafe(n, SDGEdge.Kind.CONTROL_FLOW);
			if (succs.size() >= 2) {
				Map<SDGNode, List<SDGNode>> succsMap = new HashMap<>();
				for (SDGEdge e : succs) {
					succsMap.put(e.getTarget(), new ArrayList<>());
				}
				jumps.put(n, succsMap);
			}
		}
	}

	void analyse() {
		CGKeeper cgk = new CGKeeper();
		SDGProgram program = buildSDG("/ben/bischof/test", className + ".main([Ljava/lang/String;)V", cgk);
		sdg = program.getSDG();
		dumpSDG(sdg);
		cg = cgk.cg;
		calculateJumpMap();
		IFCAnalysis ana = new IFCAnalysis(program);
		ana.addSourceAnnotation(program.getPart(className + ".f(II)I->p2"), BuiltinLattices.STD_SECLEVEL_HIGH);
		ana.addSinkAnnotation(program.getPart(className + ".print(I)V->p1"), BuiltinLattices.STD_SECLEVEL_LOW);
		Collection<IIllegalFlow<SecurityNode>> vios = getIllegalFlows(ana.doIFC());
		final CFG icfg = ICFGBuilder.extractICFG(sdg);
		GraphModifier.removeCallCallRetEdges(icfg);
        final DirectedGraph<SDGNode, SDGEdge> reversedCfg = new EdgeReversedGraph<>(icfg);
        SDGNode exit = null;
        for (SDGEdge e : sdg.getOutgoingEdgesOfKindUnsafe(sdg.getRoot(), SDGEdge.Kind.CONTROL_DEP_EXPR)) {
        	if (e.getTarget().getKind() == SDGNode.Kind.EXIT) {
        		exit = e.getTarget();
        	}
        }
        frontiers = DominanceFrontiers.compute(reversedCfg, exit);
		Dominators<SDGNode, SDGEdge> dom = Dominators.compute(reversedCfg, exit);
		this.dio = new DFSIntervalOrder<SDGNode, DomEdge>(dom.getDominationTree());
		Chopper c = new RepsRosayChopper(sdg);
		for (IIllegalFlow<SecurityNode> vio : vios) {
			System.out.println(vio);
			Collection<SDGNode> chop = c.chop(vio.getSource(), vio.getSink());
			System.out.println(chop);
			for (SDGNode n : chop) {
				nodeJumps.put(n, getJumpsForNode(n));
			}
		}
		nodeJumpDebugOutput();
		System.out.println();
		jumpsOutput();
		System.out.println();
		jumpsAndTargetsOutput();
		System.out.println();
		jumpsAndTargetsAndNodesOutput();
	}

	public static void analyse(String className) {
		Main m = new Main(className);
		m.analyse();
	}
	
	private String className;
	private SDG sdg;
	private DominanceFrontiers<SDGNode, SDGEdge> frontiers;
	private DFSIntervalOrder<SDGNode, DomEdge> dio;
	private CallGraph cg;
	private Map<SDGNode, Map<SDGNode, List<SDGNode>>> nodeJumps = new HashMap<>();
	private Map<SDGNode, Map<SDGNode, List<SDGNode>>> jumps = new HashMap<>();

	private Main(String className) {
		this.className = className;
	}

	public static void main(String[] args) {
		analyse("A");
	}
}