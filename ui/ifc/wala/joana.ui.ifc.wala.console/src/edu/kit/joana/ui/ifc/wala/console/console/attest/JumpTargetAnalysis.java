/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.ui.ifc.wala.console.console.attest;

import com.ibm.wala.classLoader.ShrikeCTMethod;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.CallGraph;
import com.ibm.wala.shrikeCT.InvalidClassFileException;
import com.ibm.wala.ssa.IR;
import com.ibm.wala.ssa.SSACFG;
import com.ibm.wala.ssa.SSACFG.BasicBlock;
import edu.kit.joana.api.IFCAnalysis;
import edu.kit.joana.graph.dominators.slca.DFSIntervalOrder;
import edu.kit.joana.ifc.sdg.core.SecurityNode;
import edu.kit.joana.ifc.sdg.core.violations.IIllegalFlow;
import edu.kit.joana.ifc.sdg.core.violations.IViolation;
import edu.kit.joana.ifc.sdg.core.violations.ViolationSeparator;
import edu.kit.joana.ifc.sdg.graph.SDG;
import edu.kit.joana.ifc.sdg.graph.SDGEdge;
import edu.kit.joana.ifc.sdg.graph.SDGNode;
import edu.kit.joana.ifc.sdg.graph.chopper.Chopper;
import edu.kit.joana.ifc.sdg.graph.chopper.RepsRosayChopper;
import edu.kit.joana.ifc.sdg.graph.slicer.graph.CFG;
import edu.kit.joana.ifc.sdg.graph.slicer.graph.building.ICFGBuilder;
import edu.kit.joana.ifc.sdg.util.sdg.GraphModifier;
import edu.kit.joana.util.Pair;
import edu.kit.joana.wala.core.graphs.DominanceFrontiers;
import edu.kit.joana.wala.core.graphs.Dominators;
import edu.kit.joana.wala.core.graphs.Dominators.DomEdge;
import org.jgrapht.DirectedGraph;
import org.jgrapht.graph.EdgeReversedGraph;

import java.io.PrintStream;
import java.util.*;

public class JumpTargetAnalysis {
	private final PrintStream debugOut;
	private final IFCAnalysis ana;
	private SDG sdg;
	private DominanceFrontiers<SDGNode, SDGEdge> frontiers;
	private DFSIntervalOrder<SDGNode, DomEdge> dio;
	private final CallGraph cg;
	private final Map<SDGNode, Map<SDGNode, List<SDGNode>>> nodeJumps = new HashMap<>();
	private final Map<SDGNode, Map<SDGNode, List<SDGNode>>> jumps = new HashMap<>();

	private JumpTargetAnalysis(PrintStream debugOut, IFCAnalysis ana, CallGraph cg) {
		this.debugOut = debugOut;
		this.ana = ana;
		this.cg = cg;
	}

	public static JumpTargetAnalysis analyse(PrintStream debugOut, IFCAnalysis ana, CallGraph cg) {
		JumpTargetAnalysis m = new JumpTargetAnalysis(debugOut, ana, cg);
		m.analyse();
		return m;
	}

	void analyse() {
		sdg = ana.getProgram().getSDG();
		calculateJumpMap();
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
			debugOut.println(vio);
			Collection<SDGNode> chop = c.chop(vio.getSource(), vio.getSink());
			debugOut.println(chop);
			for (SDGNode n : chop) {
				nodeJumps.put(n, getJumpsForNode(n));
			}
		}
		nodeJumpDebugOutput(debugOut);
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

	private Collection<IIllegalFlow<SecurityNode>> getIllegalFlows(Collection<? extends IViolation<SecurityNode>> vios) {
		ViolationSeparator<SecurityNode> separator = new ViolationSeparator<>();
		for (IViolation<SecurityNode> vio : vios) {
			vio.accept(separator);
		}
		return separator.getIllegalFlows();
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
	
	private void printSDGNodeDebugInfo(PrintStream debugOut, SDGNode n, String prefix, int adjust) {
		debugOut.println(prefix + "Node " + n.getId() + " " + getBCMethodAndIndexString(n, adjust));
	}

	private void nodeJumpDebugOutput(PrintStream debugOut) {
		for (Map.Entry<SDGNode, Map<SDGNode, List<SDGNode>>> entry1 : nodeJumps.entrySet()) {
			SDGNode n = entry1.getKey();
			printSDGNodeDebugInfo(debugOut, n, "", 0);
			for (Map.Entry<SDGNode, List<SDGNode>> entry2 : entry1.getValue().entrySet()) {
				SDGNode d = entry2.getKey();
				printSDGNodeDebugInfo(debugOut, d, "\t", 1);
				for (SDGNode t : entry2.getValue()) {
					printSDGNodeDebugInfo(debugOut, t, "\tvia: ", -1);
				}
			}
		}
	}

	public void jumpsOutput(PrintStream out) {
		for (Map.Entry<SDGNode, Map<SDGNode, List<SDGNode>>> entry1 : jumps.entrySet()) {
			SDGNode n = entry1.getKey();
			out.println(n.getId() + " " + getBCMethodAndIndexString(n, 1));
		}
	}

	public void jumpsAndTargetsOutput(PrintStream out) {
		for (Map.Entry<SDGNode, Map<SDGNode, List<SDGNode>>> entry1 : jumps.entrySet()) {
			try {
				SDGNode n = entry1.getKey();
				out.println(n.getId() + " " + getBCMethodAndIndexString(n, 1));
				for (SDGNode d : entry1.getValue().keySet()) {
					out.print(" " + getBCIndex(d, -1));
				}
				out.println();
			} catch (Exception e) {}
		}
	}

	public void jumpsAndTargetsAndNodesOutput(PrintStream out) {
		for (Map.Entry<SDGNode, Map<SDGNode, List<SDGNode>>> entry1 : jumps.entrySet()) {
			SDGNode n = entry1.getKey();
			out.println(n.getId() + " " + getBCMethodAndIndexString(n, 1));
			for (Map.Entry<SDGNode, List<SDGNode>> entry2 : entry1.getValue().entrySet()) {
				SDGNode d = entry2.getKey();
				out.println(" " + getBCMethodAndIndexString(d, -1) + " " + entry2.getValue());
			}
		}
	}

	private Map<SDGNode, List<SDGNode>> getJumpsForNode(SDGNode n) {
		Set<SDGNode> doms = frontiers.getDominanceFrontier(n);
		Map<SDGNode, List<SDGNode>> domMap = new HashMap<>();
		for (SDGNode d : doms) {
			List<SDGNode> targets = new ArrayList<>();
			Map<SDGNode, List<SDGNode>> succsMap = jumps.get(d);
			if (succsMap == null) {
				continue;
			}
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
}