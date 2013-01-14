/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.wala.core;

import java.io.PrintStream;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import org.jgrapht.graph.DirectedPseudograph;

import com.ibm.wala.cfg.ControlFlowGraph;
import com.ibm.wala.classLoader.IBytecodeMethod;
import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.classLoader.IField;
import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.classLoader.IMethod.SourcePosition;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.propagation.PointerAnalysis;
import com.ibm.wala.shrikeCT.InvalidClassFileException;
import com.ibm.wala.ssa.IR;
import com.ibm.wala.ssa.SSAAbstractInvokeInstruction;
import com.ibm.wala.ssa.SSAArrayLoadInstruction;
import com.ibm.wala.ssa.SSAArrayStoreInstruction;
import com.ibm.wala.ssa.SSAGetCaughtExceptionInstruction;
import com.ibm.wala.ssa.SSAGetInstruction;
import com.ibm.wala.ssa.SSAInstruction;
import com.ibm.wala.ssa.SSAInvokeInstruction;
import com.ibm.wala.ssa.SSAPhiInstruction;
import com.ibm.wala.ssa.SSAPiInstruction;
import com.ibm.wala.ssa.SSAPutInstruction;
import com.ibm.wala.ssa.SSAReturnInstruction;
import com.ibm.wala.ssa.analysis.IExplodedBasicBlock;
import com.ibm.wala.types.MethodReference;
import com.ibm.wala.types.TypeReference;
import com.ibm.wala.util.CancelException;
import com.ibm.wala.util.MonitorUtil.IProgressMonitor;
import com.ibm.wala.util.WalaException;
import com.ibm.wala.util.collections.Pair;
import com.ibm.wala.util.graph.GraphIntegrity.UnsoundGraphException;
import com.ibm.wala.util.graph.INodeWithNumber;
import com.ibm.wala.viz.DotUtil;
import com.ibm.wala.viz.NodeDecorator;

import edu.kit.joana.ifc.sdg.util.BytecodeLocation;
import edu.kit.joana.ifc.sdg.util.SDGConstants;
import edu.kit.joana.util.Log;
import edu.kit.joana.util.LogUtil;
import edu.kit.joana.util.Logger;
import edu.kit.joana.wala.core.PDGNode.Kind;
import edu.kit.joana.wala.core.SDGBuilder.ExceptionAnalysis;
import edu.kit.joana.wala.core.graphs.CDG;
import edu.kit.joana.wala.flowless.pointsto.AliasGraph;
import edu.kit.joana.wala.flowless.pointsto.AliasGraph.MayAliasGraph;
import edu.kit.joana.wala.flowless.pointsto.Pts2AliasGraph;
import edu.kit.joana.wala.flowless.pointsto.PtsParameter;
import edu.kit.joana.wala.util.PrettyWalaNames;
import edu.kit.joana.wala.util.WriteGraphToDot;
import gnu.trove.map.TIntObjectMap;
import gnu.trove.map.hash.TIntObjectHashMap;

public final class PDG extends DependenceGraph implements INodeWithNumber {

	private static final long serialVersionUID = -6154468467229624897L;

	public static PDG build(SDGBuilder builder, String name, CGNode node, int id,
			ExternalCallCheck ext, PrintStream out, IProgressMonitor progress)
	throws UnsoundGraphException, CancelException {
		PDG pdg = new PDG(builder, name, node, id);

		pdg.run(node.getIR(), ext, out, progress);

		return pdg;
	}

	public static PDG buildDummy(SDGBuilder builder, String name, CGNode node, int id,
			ExternalCallCheck ext, PrintStream out, IProgressMonitor progress)
	throws UnsoundGraphException, CancelException {
		PDG pdg = new PDG(builder, name, node, id);

		// calling run with an empty IR triggers dummy creation.
		pdg.run(null, ext, out, progress);

		return pdg;
	}
	// equals is bogus on ssa instructions, but comparing iindex works
	private final Map<PDGNode, SSAInstruction> node2instr = new HashMap<PDGNode, SSAInstruction>();
	private final Map<SSAInstruction, PDGNode> instr2node = new HashMap<SSAInstruction, PDGNode>();
	private final List<PDGNode> calls = new LinkedList<PDGNode>();
	private final Map<PDGNode, PDGNode[]> call2in = new HashMap<PDGNode, PDGNode[]>();
	private final Map<PDGNode, List<PDGField>> call2staticIn = new HashMap<PDGNode, List<PDGField>>();
	private final Map<PDGNode, List<PDGField>> call2staticOut = new HashMap<PDGNode, List<PDGField>>();


	public static final String NOP_LABEL = "nop";
	/** maps each call nodes to its actual out nodes. Does not include accessed static or object fields.
	 * for java this is either a single entry containing the exception act-out for void methods,
	 * or two entries: a return act-out and the exception act-out */
	private final Map<PDGNode, PDGCallReturn> call2out = new HashMap<PDGNode, PDGCallReturn>();
	/** a list of nodes that correspond to field-get instructions */
	private final List<PDGField> hread = new LinkedList<PDGField>();
	/** a list of nodes that correspond to field-set instructions */
	private final List<PDGField> hwrite = new LinkedList<PDGField>();
	private final Set<ParameterField> fread = new HashSet<ParameterField>();
	private final Set<ParameterField> fwrite = new HashSet<ParameterField>();

	/** a list of all return statements */
	private final List<PDGNode> returns = new LinkedList<PDGNode>();

	private final String sourceFile;
	/** the method this pdg corresponds to */
	private final IMethod method;
	/** the cg node this pdg corresponds to */
	public final CGNode cgNode;
	/** id of this pdg, unique for each pdg. Each node belonging to this pdg has its pdgId set to this value. */
	private final Integer id;
	private final SDGBuilder builder;

	/** entry node of the pdg */
	public final PDGNode entry;
	/** formal-out parameter for the return value of the method. Also present for void methods,
	 *  but then there is no connection through param structure. */
	public final PDGNode exit;
	/** formal-out parameter for potentially thrown exception that leave this methods scope */
	public final PDGNode exception;
	/** formal-in parameters for method parameters. includes this pointer for non-static methods. */
	public final PDGNode[] params;
	/** formal-in parameters for method local static field reads */
	public PDGField[] staticReads = new PDGField[0];
	/** formal-out parameters for method local static field writes */
	public PDGField[] staticWrites = new PDGField[0];
	public List<PDGField> staticInterprocReads = new LinkedList<PDGField>();
	public List<PDGField> staticInterprocWrites = new LinkedList<PDGField>();

	private final boolean ignoreStaticFields;
	private final boolean keepPhiNodes;
	private final boolean noBasePointerDependency;

	private PDG(SDGBuilder builder, String name, CGNode node, int pdgId) {
		this.id = pdgId;
		this.nodeID = pdgId;
		this.cgNode = node;
		this.method = node.getMethod();
		IClass cls = method.getDeclaringClass();
		this.sourceFile = PrettyWalaNames.sourceFileName(cls.getName());;
		this.builder = builder;
		this.entry = createNode(name, PDGNode.Kind.ENTRY, PDGNode.DEFAULT_NO_TYPE);
		this.exit = createNode(name, PDGNode.Kind.EXIT, method.getReturnType());
		this.exception = createNode("_exception_", PDGNode.Kind.FORMAL_OUT, TypeReference.JavaLangException);
		this.params = new PDGNode[method.getNumberOfParameters()];
		for (int i = 0; i < params.length; i++) {
			if (method.isStatic()) {
				// parameter index 0 is reserved for the this pointer. static method params start at 1.
				PDGNode p = createNode("param " + (i + 1), PDGNode.Kind.FORMAL_IN, method.getParameterType(i));
				this.params[i] = p;
			} else {
				PDGNode p = createNode((i == 0 ? "this" : "param " + i), PDGNode.Kind.FORMAL_IN,
						method.getParameterType(i));
				this.params[i] = p;
			}
		}

		this.ignoreStaticFields = builder.isIgnoreStaticFields(method.getDeclaringClass().getReference());
		this.keepPhiNodes = builder.isKeepPhiNodes();
		this.noBasePointerDependency = builder.isNoBasePointerDependency();
	}

	private void run(final IR ir, ExternalCallCheck ext, PrintStream out, IProgressMonitor progress) throws UnsoundGraphException, CancelException {
		if (ir == null || ir.isEmptyIR() || builder.isImmutableStub(method.getDeclaringClass().getReference())) {
			addDummyConnections();
			addSourcecodeInfoToNodes(null);
		} else {
			final Logger dumpSSA = Log.getLogger(Log.L_WALA_IR_DUMP);
			if (dumpSSA.isEnabled()) {
				dumpSSA.outln(PrettyWalaNames.ir2string(ir));
			}
			addNodesForInstructions(ir);
			addSourcecodeInfoToNodes(ir);
			addControlFlow(progress, ir);
			addControlDependence();
			addDataFlowSSA(ir);
			removeNopAndPhiNodes();
			addRootParameterStructure();
			checkForExternalCalls(ext, out);
		}
	}

	// only for evaluation purposes, adds random edges
	@SuppressWarnings("unused")
	private static void addRandomEdges(final AliasGraph g, final int numEdges) {
		final int numOfNodes = g.getNumberOfNodes();
		Random rand = new Random();

		for (int i = 0; i < numEdges; i++) {
			final int from = rand.nextInt(numOfNodes);
			final PtsParameter fromNode = g.getNode(from);
			int to = rand.nextInt(numOfNodes);
			PtsParameter toNode = g.getNode(to);

			int maxTries = 20;
			while (maxTries > 0 && (to == from || g.hasEdge(fromNode, toNode))) {
				maxTries--;
				to = rand.nextInt(numOfNodes);
				toNode = g.getNode(to);
			}

			g.addEdge(fromNode, toNode);
			g.addEdge(toNode, fromNode);
		}
	}

	private void checkForExternalCalls(final ExternalCallCheck ext, final PrintStream out) {
		for (final PDGNode call : calls) {
			final SSAInvokeInstruction invk = (SSAInvokeInstruction) node2instr.get(call);

			if (ext.isCallToModule(invk)) {
				out.print("E" + getId());
				//out.println("Call to external module: " + invk.getDeclaredTarget().getSignature());
				final CGNode cgnode = cgNode;
				final PointerAnalysis pts = builder.getPointerAnalysis();
				final MayAliasGraph mayAlias = Pts2AliasGraph.computeCurrentAliasing(pts, cgnode, invk);

				ext.registerAliasContext(invk, call.getId(), mayAlias);
			}

		}
	}

	private void addRootParameterStructure() {
		if (method.getReturnType() != TypeReference.Void) {
			addEdge(entry, exit, PDGEdge.Kind.PARAM_STRUCT);
		}
		addEdge(entry, exception, PDGEdge.Kind.PARAM_STRUCT);

		for (PDGNode p : params) {
			addEdge(entry, p, PDGEdge.Kind.PARAM_STRUCT);
		}

		for (PDGField s : staticReads) {
			addEdge(entry, s.node, PDGEdge.Kind.PARAM_STRUCT);
		}

		for (PDGField s : staticWrites) {
			addEdge(entry, s.node, PDGEdge.Kind.PARAM_STRUCT);
		}

		for (PDGNode c : calls) {
			for (PDGNode in : call2in.get(c)) {
				addEdge(c, in, PDGEdge.Kind.PARAM_STRUCT);
			}

			for (final PDGNode out : call2out.get(c)) {
				addEdge(c, out, PDGEdge.Kind.PARAM_STRUCT);
			}
		}
	}

	private void removeNopAndPhiNodes() {
		List<PDGNode> toRemove = new LinkedList<PDGNode>();
		for (PDGNode node : vertexSet()) {
			if (isNopNode(node)) {
				toRemove.add(node);
			} else if (!keepPhiNodes && node.getKind() == PDGNode.Kind.PHI) {
				toRemove.add(node);
			}
		}

		for (PDGNode nop : toRemove) {
			removeNode(nop);
		}
	}

	private void addDataFlowSSA(IR ir) {
		TIntObjectMap<PDGNode> var2node = new TIntObjectHashMap<PDGNode>();

		// add var defs for formal in nodes
		for (int i = 0; i < params.length; i++) {
			if (i < ir.getNumberOfParameters()) {
				// no value number for unused params?
				PDGNode p = params[i];
				final int ssaDef = ir.getParameter(i);
				var2node.put(ssaDef, p);
			}
		}

		// add var defs for act-out nodes of calls and normal expression nodes
		for (Iterator<SSAInstruction> it = ir.iterateAllInstructions(); it.hasNext();) {
			SSAInstruction instr = it.next();
			if (instr.hasDef()) {
				PDGNode node = instr2node.get(instr);
				if (node.getKind() == PDGNode.Kind.CALL) {
					SSAInvokeInstruction invk = (SSAInvokeInstruction) instr;
					final PDGCallReturn out = call2out.get(node);
					if (out.excVal != null) {
						var2node.put(invk.getException(), out.excVal);
					}
					if (out.retVal != null) {
						var2node.put(invk.getDef(), out.retVal);
					}
				} else if (SDGBuilder.DATA_FLOW_FOR_GET_FROM_FIELD_NODE && node.getKind() == PDGNode.Kind.HREAD) {
					PDGField fread = findFieldForNode(hread, node);
					for (int i = 0; i < instr.getNumberOfDefs(); i++) {
						var2node.put(instr.getDef(i), fread.accfield);
					}
				} else {
					for (int i = 0; i < instr.getNumberOfDefs(); i++) {
						var2node.put(instr.getDef(i), node);
					}
				}
			}
		}

		// add flow from defs to uses
		for (Iterator<SSAInstruction> it = ir.iterateAllInstructions(); it.hasNext();) {
			SSAInstruction instr = it.next();
			if (instr.getNumberOfUses() > 0) {
				PDGNode node = instr2node.get(instr);

				if (node.getKind() == PDGNode.Kind.CALL) {
					PDGNode[] in = call2in.get(node);
					for (int i = 0; i < instr.getNumberOfUses(); i++) {
						PDGNode defNode = var2node.get(instr.getUse(i));
						if (defNode != null) {
							addEdge(defNode, in[i], PDGEdge.Kind.DATA_DEP);
						}
					}
				} else if (node.getKind() == PDGNode.Kind.HREAD) {
					PDGField fread = findFieldForNode(hread, node);
					if (fread.field.isArray()) {
						SSAArrayLoadInstruction aload = (SSAArrayLoadInstruction) instr;
						PDGNode defIndex = var2node.get(aload.getIndex());
						if (defIndex != null) {
							addEdge(defIndex, fread.index, PDGEdge.Kind.DATA_DEP);
						}
						PDGNode defBase = var2node.get(aload.getArrayRef());
						if (defBase != null) {
							addEdge(defBase, fread.base, PDGEdge.Kind.DATA_DEP);
						}
					} else {
						SSAGetInstruction fget = (SSAGetInstruction) instr;
						if (!fget.isStatic()) {
							PDGNode defBase = var2node.get(fget.getRef());
							if (defBase != null) {
								addEdge(defBase, fread.base, PDGEdge.Kind.DATA_DEP);
							}
						}
					}
				} else if (node.getKind() == PDGNode.Kind.HWRITE) {
					PDGField fwrite = findFieldForNode(hwrite, node);
					if (fwrite.field.isArray()) {
						SSAArrayStoreInstruction astore = (SSAArrayStoreInstruction) instr;
						PDGNode defIndex = var2node.get(astore.getIndex());
						if (defIndex != null) {
							addEdge(defIndex, fwrite.index, PDGEdge.Kind.DATA_DEP);
						}
						PDGNode defBase = var2node.get(astore.getArrayRef());
						if (defBase != null) {
							addEdge(defBase, fwrite.base, PDGEdge.Kind.DATA_DEP);
						}
						PDGNode defVal = var2node.get(astore.getValue());
						if (defVal != null) {
							addEdge(defVal, fwrite.node, PDGEdge.Kind.DATA_DEP);
						}
					} else {
						SSAPutInstruction fput = (SSAPutInstruction) instr;
						if (!fput.isStatic()) {
							PDGNode defBase = var2node.get(fput.getRef());
							if (defBase != null) {
								addEdge(defBase, fwrite.base, PDGEdge.Kind.DATA_DEP);
							}
						}
						PDGNode defVal = var2node.get(fput.getVal());
						if (defVal != null) {
							addEdge(defVal, fwrite.node, PDGEdge.Kind.DATA_DEP);
						}
					}
				} else {
					for (int i = 0; i < instr.getNumberOfUses(); i++) {
						PDGNode defNode = var2node.get(instr.getUse(i));
						if (defNode != null) {
							addEdge(defNode, node, PDGEdge.Kind.DATA_DEP);
						}
					}
				}
			}
		}

		// add flow from exception of method calls to formal-out exception node
		for (PDGNode c : calls) {
			final PDGCallReturn out = call2out.get(c);

			if (out.excVal == null) {
				continue;
			}

			final PDGNode exc = out.excVal;

			boolean hasDirectControlFlow = false;
			for (PDGEdge edge : getAllEdges(exc, exception)) {
				if (edge.kind.isFlow()) {
					hasDirectControlFlow = true;
					break;
				}
			}

			if (hasDirectControlFlow) {
				// if there is direct control flow, there is data flow
				addEdge(exc, exception, PDGEdge.Kind.DATA_DEP);
			}
		}

		if (method.getReturnType() != TypeReference.Void) {
			// add data deps from return instructions to exit node.
			for (Iterator<SSAInstruction> it = ir.iterateAllInstructions(); it.hasNext();) {
				SSAInstruction instr = it.next();
				if (instr instanceof SSAReturnInstruction) {
					PDGNode ret = instr2node.get(instr);
					addEdge(ret, exit, PDGEdge.Kind.DATA_DEP);
				}
			}
		}
	}

	private static PDGField findFieldForNode(Collection<PDGField> fields, PDGNode n) {
		for (PDGField f : fields) {
			if (f.node.equals(n)) {
				return f;
			}
		}

		return null;
	}

	private static PDGField findFieldForNode(PDGField[] fields, PDGNode n) {
		for (PDGField f : fields) {
			if (f.node.equals(n)) {
				return f;
			}
		}

		return null;
	}

	private void addControlDependence() {
		final DependenceGraph cfg = createCfgWithoutParams();
		final CDG cdg = CDG.build(cfg, entry, exit);

		for (final PDGNode from : cdg.vertexSet()) {
			for (final PDGEdge edge : cdg.outgoingEdgesOf(from)) {
				final PDGNode to = cdg.getEdgeTarget(edge);
				// do not add control dep to exception formal out. there is already a
				// connection through data dependence from ret _exception_ field
				if (!(from.getKind() == PDGNode.Kind.CALL && to == exception)) {
					addEdge(from, to, (from == entry && to == exit ? PDGEdge.Kind.CONTROL_DEP_EXPR : PDGEdge.Kind.CONTROL_DEP));
				}
			}
		}

		// add additional deps for parameter nodes
		addEdge(entry, exception, PDGEdge.Kind.CONTROL_DEP_EXPR);

		for (final PDGNode p : params) {
			addEdge(entry, p, PDGEdge.Kind.CONTROL_DEP_EXPR);
		}

		for (final PDGField s : staticReads) {
			addEdge(entry, s.node, PDGEdge.Kind.CONTROL_DEP_EXPR);
		}

		for (final PDGField s : staticWrites) {
			addEdge(entry, s.node, PDGEdge.Kind.CONTROL_DEP_EXPR);
		}

		for (final PDGNode call : calls) {
			final PDGNode[] in = call2in.get(call);
			// add dependency from this-pointer to call for virtual calls
			if (in.length > 0) {
				final SSAInvokeInstruction invk = (SSAInvokeInstruction) getInstruction(call);
				if (!invk.isStatic()) {
					addEdge(in[0], call, PDGEdge.Kind.CONTROL_DEP /* this may be a virtual control dependency */);
				}
			}

			for (final PDGNode actIn : in) {
				addEdge(call, actIn, PDGEdge.Kind.CONTROL_DEP_EXPR);
			}

			for (final PDGNode out : call2out.get(call)) {
				addEdge(call, out, PDGEdge.Kind.CONTROL_DEP_EXPR);
			}
		}

		// fix constant propagation through phi nodes: the value of a constant in v3 = phi(#3, #4); depends on the
		// path through which the instruction is reached in the control flow.
		// boolean foo = ...;
		// if (foo) { bar = true } else { bar = false }
		// print(bar);
		// results in a phi(#true, #false) that has no control of data dependence to foo. which is wrong.
		for (final PDGNode n : cdg.vertexSet()) {
			if (n.getKind() == PDGNode.Kind.PHI) {
				Set<PDGNode> cdPreds = new HashSet<PDGNode>();

				for (final PDGEdge e : incomingEdgesOf(n)) {
					if (e.kind == PDGEdge.Kind.CONTROL_FLOW) {
						for (final PDGEdge ePred : incomingEdgesOf(e.from)) {
							if (ePred.kind == PDGEdge.Kind.CONTROL_DEP) {
								cdPreds.add(ePred.from);
							}
						}
					}
				}

				for (final PDGNode cd : cdPreds) {
					addEdge(cd, n, PDGEdge.Kind.CONTROL_DEP);
				}
			}
		}
	}

	private Set<PDGNode> findUnreachableFrom(final DirectedPseudograph<PDGNode, PDGEdge> cfg, final PDGNode entry) {
		final Set<PDGNode> unreachable = new HashSet<PDGNode>(cfg.vertexSet());

		final LinkedList<PDGNode> todo = new LinkedList<PDGNode>();
		todo.add(entry);

		while (!todo.isEmpty()) {
			final PDGNode cur = todo.removeFirst();
			unreachable.remove(cur);

			for (final PDGEdge out : cfg.outgoingEdgesOf(cur)) {
				if (out.kind.isFlow() && unreachable.contains(out.to)) {
					todo.add(out.to);
				}
			}
		}

		return unreachable;
	}

	private Set<PDGNode> findNotReachingTo(final DirectedPseudograph<PDGNode, PDGEdge> cfg, final PDGNode exit) {
		final Set<PDGNode> unreachable = new HashSet<PDGNode>(cfg.vertexSet());

		final LinkedList<PDGNode> todo = new LinkedList<PDGNode>();
		todo.add(exit);

		while (!todo.isEmpty()) {
			final PDGNode cur = todo.removeFirst();
			unreachable.remove(cur);

			for (final PDGEdge in : cfg.incomingEdgesOf(cur)) {
				if (in.kind.isFlow() && unreachable.contains(in.from)) {
					todo.add(in.from);
				}
			}
		}

		return unreachable;
	}

	private DependenceGraph createCfgWithoutParams() {
		final DependenceGraph cfg = new DependenceGraph();
		for (final PDGNode node : vertexSet()) {
			cfg.addVertex(node);
		}

		for (final PDGNode from : vertexSet()) {
			for (final PDGEdge edge : outgoingEdgesOf(from)) {
				if (edge.kind == PDGEdge.Kind.CONTROL_FLOW || edge.kind == PDGEdge.Kind.CONTROL_FLOW_EXC) {
					cfg.addEdge(from, edge.to, edge);
				}
			}
		}

		final List<PDGNode> toRemove = new LinkedList<PDGNode>();
		for (final PDGNode node : cfg.vertexSet()) {
			switch (node.getKind()) {
			case ACTUAL_IN:
			case ACTUAL_OUT:
			case FORMAL_IN:
			case FORMAL_OUT:
				if (node != exception) {
					toRemove.add(node);
				}
				break;
			case NORMAL:
				// remove fine grained field access nodes (base, index, field) from cfg
				if (node.getBytecodeIndex() < 0
						&& node.getBytecodeIndex() != BytecodeLocation.UNDEFINED_POS_IN_BYTECODE) {
					if (!noBasePointerDependency || node.getBytecodeIndex() != BytecodeLocation.BASE_FIELD) {
						// keep base pointer nodes in case of noBasePointerDependency is set, because they are used to
						// carry controldependencies rather then the instruciton nodes to prevent a depdency from
						// a field value to the control dep. NullPointerException is only influenced by the
						// value of the base pointer not the field.
						toRemove.add(node);
					}
				}
				break;
			default: break;
			}
		}

		if (noBasePointerDependency) {
			/* in case of noBasePointerDependency, remove all nodes except the base-pointer node from the cfg, for
			 * all field accesses that use a base pointer (object-field get/set and array-field get/set). Leave
			 * static-field accesses untouched.
			 */

			for (final PDGField f : getFieldReads()) {
				if (f.base != null) {
					toRemove.add(f.node);
				}
			}

			for (final PDGField f : getFieldWrites()) {
				if (f.base != null) {
					toRemove.add(f.node);
				}
			}
		}

		for (final PDGNode node : toRemove) {
			cfg.removeNode(node);
		}

		return cfg;
	}

	private int getNextBytecodeInstructionIndex(final PDGNode start) {
		final Set<PDGNode> visited = new HashSet<PDGNode>();
		visited.add(start);
		PDGNode tgt = start;

		while (tgt.getBytecodeIndex() <= 0) {
			final PDGNode old = tgt;

			for (PDGEdge tgtOut : outgoingEdgesOf(tgt)) {
				if (tgtOut.kind == PDGEdge.Kind.CONTROL_FLOW && !visited.contains(tgtOut.to)) {
					tgt = tgtOut.to;
					visited.add(tgt);
					break;
				}
			}

			if (tgt == old) {
				break;
			}
		}

		return tgt.getBytecodeIndex();
	}

	/**
	 * Add controlflow from cfg to pdg. Contruct nop nodes for empty blocks.
	 * @throws CancelException
	 * @throws UnsoundGraphException
	 */
	private void addControlFlow(final IProgressMonitor progress, final IR ir)
			throws UnsoundGraphException, CancelException {
		final ControlFlowGraph<SSAInstruction, IExplodedBasicBlock> ecfg =
				builder.createExceptionAnalyzedCFG(cgNode, progress);
		
		final Logger log = Log.getLogger(Log.L_WALA_CFG_DUMP);
		if (log.isEnabled()) {
			final String fileName = WriteGraphToDot.sanitizeFileName(method.getSignature() + "-cfg.dot");
			WriteGraphToDot.writeCfgToDot(ecfg, ir, "CFG of " + fileName, fileName);
		}

		final TIntObjectMap<PDGNode[]> bbnum2node = mapBasicBlockToNode(ecfg);

		for (final IExplodedBasicBlock current : ecfg) {
			final PDGNode[] fromNodes = bbnum2node.get(current.getNumber());

			for (int index = 1; index < fromNodes.length; index++) {
				addEdge(fromNodes[index - 1], fromNodes[index], PDGEdge.Kind.CONTROL_FLOW);
			}

			final PDGNode from = fromNodes[fromNodes.length - 1];

			for (final IExplodedBasicBlock next : ecfg.getNormalSuccessors(current)) {
				final PDGNode[] nextNodes = bbnum2node.get(next.getNumber());
				final PDGNode nextNode = nextNodes[0];

				if (from == nextNode) {
					continue;
				}

				addEdge(from, nextNode, PDGEdge.Kind.CONTROL_FLOW);
			}

			for (final IExplodedBasicBlock next : ecfg.getExceptionalSuccessors(current)) {
				final PDGNode[] nextNodes = bbnum2node.get(next.getNumber());
				final PDGNode nextNode = nextNodes[0];

				if (from == nextNode) {
					continue;
				}

				if (nextNode == exit) {
					addEdge(from, exception, PDGEdge.Kind.CONTROL_FLOW_EXC);
				} else {
					addEdge(from, nextNode, PDGEdge.Kind.CONTROL_FLOW_EXC);
				}
			}
		}

		// adjust labels of goto and if-instructions
		for (final PDGNode node : vertexSet()) {
			if (node.getLabel().equals("goto")) {
				for (final PDGEdge edge : outgoingEdgesOf(node)) {
					if (edge.kind == PDGEdge.Kind.CONTROL_FLOW) {
						final int nextBCindex = getNextBytecodeInstructionIndex(edge.to);
						node.setLabel(node.getLabel() + " " + nextBCindex);
						break;
					}
				}
			} else if (node.getLabel().startsWith("if ")) {
				for (final PDGEdge edge : outgoingEdgesOf(node)) {
					if (edge.kind == PDGEdge.Kind.CONTROL_FLOW) {
						final int nextBCindex = getNextBytecodeInstructionIndex(edge.to);
						node.setLabel(node.getLabel() + " goto " + nextBCindex);
						break;
					}
				}
			}

			if (builder.cfg.exceptions != ExceptionAnalysis.IGNORE_ALL) {
				for (final PDGEdge edge : outgoingEdgesOf(node)) {
					if (edge.kind == PDGEdge.Kind.CONTROL_FLOW_EXC) {
						final int nextBCindex = getNextBytecodeInstructionIndex(edge.to);
						if (nextBCindex >= 0) {
							node.setLabel(node.getLabel() + " exc " + nextBCindex);
						}
					}
				}
			}
		}

		// move normal control flow from ret _exception_ node to its direct pred
		for (final PDGNode node : vertexSet()) {
			if (node.getKind() == PDGNode.Kind.ACTUAL_OUT
					&& node.getLabel().equals("ret _exception_")) {
				final List<PDGEdge> norm = new LinkedList<PDGEdge>();
				boolean hasExcFlow = false;
				for (final PDGEdge edge : outgoingEdgesOf(node)) {
					if (edge.kind == PDGEdge.Kind.CONTROL_FLOW) {
						norm.add(edge);
					} else if (edge.kind == PDGEdge.Kind.CONTROL_FLOW_EXC) {
						hasExcFlow = true;
					}
				}

				if (!hasExcFlow) {
					// method does not throw an exception. No further adjustments needed.
					continue;
				}

				final Set<PDGEdge> dPreds = new HashSet<PDGEdge>();
				for (PDGEdge edge : incomingEdgesOf(node)) {
					if (edge.kind == PDGEdge.Kind.CONTROL_FLOW) {
						dPreds.add(edge);
					}
				}

				// make control flow to exception return a exception control flow
				for (final PDGEdge edge : dPreds) {
					removeEdge(edge);
					addEdge(edge.from, edge.to, PDGEdge.Kind.CONTROL_FLOW_EXC);
				}

				// add normal control flow from exception return as normal control
				// flow of its direct preds
				for (final PDGEdge edge : norm) {
					removeEdge(edge);
					for (PDGEdge pred : dPreds) {
						addEdge(pred.from, edge.to, PDGEdge.Kind.CONTROL_FLOW);
					}
				}
			}
		}

		// add control flow for fine grained field access nodes
		for (final PDGField hr : hread) {
			final List<PDGEdge> flowTo = findControlFlowReaching(hr.node);

			if (hr.field.isArray()) {
				if (noBasePointerDependency) {
					final List<PDGEdge> flowFrom = findControlFlowLeaving(hr.node);
					changeEdgeTargetTo(flowTo, hr.base);
					addEdge(hr.base, hr.index, PDGEdge.Kind.CONTROL_FLOW);
					addEdge(hr.index, hr.accfield, PDGEdge.Kind.CONTROL_FLOW);
					addEdge(hr.accfield, hr.node, PDGEdge.Kind.CONTROL_FLOW);
					addEdge(hr.node, hr.base, PDGEdge.Kind.CONTROL_FLOW);
					changeEdgeSourceTo(flowFrom, hr.base);
				} else {
					changeEdgeTargetTo(flowTo, hr.index);
					addEdge(hr.index, hr.base, PDGEdge.Kind.CONTROL_FLOW);
					addEdge(hr.base, hr.accfield, PDGEdge.Kind.CONTROL_FLOW);
					addEdge(hr.accfield, hr.node, PDGEdge.Kind.CONTROL_FLOW);
				}
			} else if (hr.field.isStatic()) {
				changeEdgeTargetTo(flowTo, hr.accfield);
				addEdge(hr.accfield, hr.node, PDGEdge.Kind.CONTROL_FLOW);
			} else {
				changeEdgeTargetTo(flowTo, hr.base);
				if (noBasePointerDependency) {
					final List<PDGEdge> flowFrom = findControlFlowLeaving(hr.node);
					addEdge(hr.base, hr.accfield, PDGEdge.Kind.CONTROL_FLOW);
					addEdge(hr.accfield, hr.node, PDGEdge.Kind.CONTROL_FLOW);
					addEdge(hr.node, hr.base, PDGEdge.Kind.CONTROL_FLOW);
					changeEdgeSourceTo(flowFrom, hr.base);
				} else {
					addEdge(hr.base, hr.accfield, PDGEdge.Kind.CONTROL_FLOW);
					addEdge(hr.accfield, hr.node, PDGEdge.Kind.CONTROL_FLOW);
				}
			}
		}

		for (final PDGField hw : hwrite) {
			final List<PDGEdge> flowFrom = findControlFlowLeaving(hw.node);

			if (hw.field.isArray()) {
				final List<PDGEdge> flowTo = findControlFlowReaching(hw.node);
				if (noBasePointerDependency) {
					changeEdgeTargetTo(flowTo, hw.base);
					addEdge(hw.base, hw.index, PDGEdge.Kind.CONTROL_FLOW);
					addEdge(hw.index, hw.node, PDGEdge.Kind.CONTROL_FLOW);
					addEdge(hw.node, hw.accfield, PDGEdge.Kind.CONTROL_FLOW);
					addEdge(hw.accfield, hw.base, PDGEdge.Kind.CONTROL_FLOW);
					changeEdgeSourceTo(flowFrom, hw.base);
				} else {
					changeEdgeTargetTo(flowTo, hw.index);
					addEdge(hw.index, hw.base, PDGEdge.Kind.CONTROL_FLOW);
					addEdge(hw.base, hw.node, PDGEdge.Kind.CONTROL_FLOW);
					addEdge(hw.node, hw.accfield, PDGEdge.Kind.CONTROL_FLOW);
					changeEdgeSourceTo(flowFrom, hw.accfield);
				}
			} else if (hw.field.isStatic()) {
				addEdge(hw.node, hw.accfield, PDGEdge.Kind.CONTROL_FLOW);
				changeEdgeSourceTo(flowFrom, hw.accfield);
			} else {
				final List<PDGEdge> flowTo = findControlFlowReaching(hw.node);
				changeEdgeTargetTo(flowTo, hw.base);
				if (noBasePointerDependency) {
					addEdge(hw.base, hw.node, PDGEdge.Kind.CONTROL_FLOW);
					addEdge(hw.node, hw.accfield, PDGEdge.Kind.CONTROL_FLOW);
					addEdge(hw.accfield, hw.base, PDGEdge.Kind.CONTROL_FLOW);
					changeEdgeSourceTo(flowFrom, hw.base);
				} else {
					addEdge(hw.base, hw.node, PDGEdge.Kind.CONTROL_FLOW);
					addEdge(hw.node, hw.accfield, PDGEdge.Kind.CONTROL_FLOW);
					changeEdgeSourceTo(flowFrom, hw.accfield);
				}
			}
		}

		addEdge(exception, exit, PDGEdge.Kind.CONTROL_FLOW_EXC);
		addEdge(entry, exit, PDGEdge.Kind.CONTROL_FLOW);

		if (inDegreeOf(exception) == 0) {
			addEdge(entry, exception, PDGEdge.Kind.CONTROL_FLOW);
		}

		// fix control flow for unreachable or non-terminating code
		{
			// add an edge from entry to unreachable code
			final Set<PDGNode> unreachEntry = findUnreachableFrom(this, entry);

			if (!unreachEntry.isEmpty()) {
				final LinkedList<PDGNode> toRemove = new LinkedList<PDGNode>();
				for (PDGNode n : unreachEntry) {
					if (n.getPdgId() != getId()) {
						toRemove.add(n);
					}
				}

				unreachEntry.removeAll(toRemove);

				for (final PDGNode unreach : unreachEntry) {
					addEdge(entry, unreach, PDGEdge.Kind.CONTROL_FLOW);
				}
			}
		}

		{
			// add an edge from non-terminating code to exit
			final Set<PDGNode> unreachExit = findNotReachingTo(this, exit);

			if (!unreachExit.isEmpty()) {
				final LinkedList<PDGNode> toRemove = new LinkedList<PDGNode>();
				for (final PDGNode n : unreachExit) {
					if (n.getPdgId() != getId()) {
						toRemove.add(n);
					}
				}

				unreachExit.removeAll(toRemove);

				for (final PDGNode unreach : unreachExit) {
					addEdge(unreach, exit, PDGEdge.Kind.CONTROL_FLOW);
				}
			}
		}
	}

	private final void changeEdgeTargetTo(final List<PDGEdge> list, final PDGNode to) {
		removeAllEdges(list);

		for (final PDGEdge edge : list) {
			addEdge(edge.from, to, edge.kind);
		}
	}

	private final void changeEdgeSourceTo(final List<PDGEdge> list, final PDGNode from) {
		removeAllEdges(list);

		for (final PDGEdge edge : list) {
			addEdge(from, edge.to, edge.kind);
		}
	}

	private List<PDGEdge> findControlFlowReaching(final PDGNode n) {
		final List<PDGEdge> flowTo = new LinkedList<PDGEdge>();
		for (final PDGEdge edge : incomingEdgesOf(n)) {
			if (edge.kind.isFlow()) {
				flowTo.add(edge);
			}
		}

		return flowTo;
	}

	private List<PDGEdge> findControlFlowLeaving(final PDGNode n) {
		final List<PDGEdge> flowFrom = new LinkedList<PDGEdge>();
		for (final PDGEdge edge : outgoingEdgesOf(n)) {
			if (edge.kind.isFlow()) {
				flowFrom.add(edge);
			}
		}

		return flowFrom;
	}

	private TIntObjectMap<PDGNode[]> mapBasicBlockToNode(ControlFlowGraph<SSAInstruction, IExplodedBasicBlock> ecfg) {
		TIntObjectMap<PDGNode[]> map = new TIntObjectHashMap<PDGNode[]>();

		for (IExplodedBasicBlock bb : ecfg) {
			List<PDGNode> nodes = new LinkedList<PDGNode>();

			if (bb.isEntryBlock()) {
				nodes.add(entry);
				for (PDGNode p : params) {
					nodes.add(p);
				}
				for (PDGField s : staticReads) {
					nodes.add(s.node);
				}
			}

			for (Iterator<? extends SSAInstruction> it = bb.iteratePhis(); it.hasNext();) {
				SSAPhiInstruction phi = (SSAPhiInstruction) it.next();
				PDGNode node = instr2node.get(phi);
				nodes.add(node);
			}

			if (bb.isCatchBlock()) {
				SSAInstruction catchInstr = bb.getCatchInstruction();
				if (catchInstr != null) {
					PDGNode catchNode = instr2node.get(catchInstr);
					nodes.add(catchNode);
				} else {
					PDGNode nop = createNopNode();
					nodes.add(nop);
				}
			}

			if (bb.getInstruction() != null) {
				SSAInstruction instr = bb.getInstruction();
				PDGNode node = instr2node.get(instr);

				if (node.getKind() == PDGNode.Kind.CALL) {
					PDGNode[] in = call2in.get(node);
					for (PDGNode actIn : in) {
						nodes.add(actIn);
					}
					nodes.add(node);
					for (final PDGNode out : call2out.get(node)) {
						nodes.add(out);
					}
				} else {
					nodes.add(node);
				}
			} else if (!bb.isExitBlock() && nodes.isEmpty()) {
				PDGNode nop = createNopNode();
				nodes.add(nop);
			}

			if (bb.isExitBlock()) {
				for (PDGField s : staticWrites) {
					nodes.add(s.node);
				}
				nodes.add(exit);
			}

			map.put(bb.getNumber(), nodes.toArray(new PDGNode[nodes.size()]));
		}

		return map;
	}

	private SourceLocation defSrcLoc = null;
	private String defBcName = null;
	private int defBcIndex = BytecodeLocation.UNDEFINED_POS_IN_BYTECODE;

	private void addSourcecodeInfoToNodes(IR ir) {
		defSrcLoc = SourceLocation.getLocation(sourceFile, 0, 0, 0, 0);
		defBcName = method.getSignature();

		if (ir != null && method instanceof IBytecodeMethod) {
			IBytecodeMethod bcMethod = (IBytecodeMethod) method;

			for (Iterator<SSAInstruction> it = ir.iterateAllInstructions(); it.hasNext();) {
				SSAInstruction instr = it.next();
				if (instr != null) {
					PDGNode node = instr2node.get(instr);
					if (node == null) {
						throw new IllegalStateException("No node for instruction: " + instr + " in " + method.getName().toString());
					}

					final int index = instr.iindex;
					if (index == SSAInstruction.NO_INDEX) {
						if (instr instanceof SSAPiInstruction || instr instanceof SSAPhiInstruction || instr instanceof SSAGetCaughtExceptionInstruction) {
							node.setSourceLocation(defSrcLoc);
							continue;
						} else {
							throw new IllegalStateException("No instruction index for " + instr.getClass().getSimpleName() + ": " + instr);
						}
					}

					try {
						final int bcIndex = bcMethod.getBytecodeIndex(index);
						SourcePosition pos = method.getSourcePosition(bcIndex);
						if (pos != null) {
							SourceLocation loc = SourceLocation.getLocation(sourceFile, pos.getFirstLine(),
									(pos.getFirstCol() >= 0 ? pos.getFirstCol() : 0), pos.getLastLine(),
									(pos.getLastCol() >= 0 ? pos.getLastCol() : 0));
							node.setSourceLocation(loc);
						} else {
							node.setSourceLocation(defSrcLoc);
						}
						node.setBytecodeIndex((bcIndex >= 0 ? bcIndex : defBcIndex));
						node.setBytecodeName(defBcName);
					} catch (ArrayIndexOutOfBoundsException e) {
						node.setSourceLocation(defSrcLoc);
					} catch (InvalidClassFileException e) {
						node.setSourceLocation(defSrcLoc);
					}

					if (node.getKind() == Kind.CALL) {
						// set sourcelocation of paramnodes
						for (PDGNode actIn : call2in.get(node)) {
							actIn.setSourceLocation(node.getSourceLocation());
						}

						for (final PDGNode out : call2out.get(node)) {
							out.setSourceLocation(node.getSourceLocation());
						}
					}
				}
			}

			for (int i = 0; i < params.length; i++) {
				PDGNode formIn = params[i];
				try {
					SourcePosition pos = method.getParameterSourcePosition(i);
					if (pos != null) {
						SourceLocation loc = SourceLocation.getLocation(sourceFile, pos.getFirstLine(),
								(pos.getFirstCol() >= 0 ? pos.getFirstCol() : 0), pos.getLastLine(),
								(pos.getLastCol() >= 0 ? pos.getLastCol() : 0));
						formIn.setSourceLocation(loc);
					} else {
						formIn.setSourceLocation(defSrcLoc);
					}
				} catch (InvalidClassFileException e) {
					formIn.setSourceLocation(defSrcLoc);
				}
			}

			for (PDGField sIn : staticReads) {
				sIn.node.setSourceLocation(defSrcLoc);
				assert sIn.field.isStatic();
				IField f = sIn.field.getField();
				sIn.node.setBytecodeName(PrettyWalaNames.bcFieldName(f));
				sIn.node.setBytecodeIndex(BytecodeLocation.STATIC_FIELD);
			}

			for (PDGField sOut : staticWrites) {
				sOut.node.setSourceLocation(defSrcLoc);
				assert sOut.field.isStatic();
				IField f = sOut.field.getField();
				sOut.node.setBytecodeName(PrettyWalaNames.bcFieldName(f));
				sOut.node.setBytecodeIndex(BytecodeLocation.STATIC_FIELD);
			}

			entry.setSourceLocation(defSrcLoc);
			exception.setSourceLocation(defSrcLoc);
			exit.setSourceLocation(defSrcLoc);
		} else {
			// set default sourcecode position
			for (PDGNode node : vertexSet()) {
				node.setSourceLocation(defSrcLoc);
				node.setBytecodeIndex(defBcIndex);
				node.setBytecodeName(defBcName);
			}
		}

		// set bytecode positions for special nodes, like form-in/-out, act-in/-out, ...
		for (int i = 0; i < params.length; i++) {
			PDGNode formIn = params[i];
			// parameter index 0 is reserved for the this pointer. static method params start at 1.
			formIn.setBytecodeName(BytecodeLocation.getRootParamName(method.isStatic() ? i + 1 : i));
			formIn.setBytecodeIndex(BytecodeLocation.ROOT_PARAMETER);
		}

		for (PDGNode node : vertexSet()) {
			if (node.getKind() == Kind.CALL) {
				// set sourcelocation of paramnodes
				// parameter index 0 is reserved for the this pointer. static method params start at 1.
				final SSAInvokeInstruction invk = (SSAInvokeInstruction) node2instr.get(node);
				int pindex = (invk.isStatic() ? 1 : 0);
				for (PDGNode actIn : call2in.get(node)) {
					actIn.setBytecodeName(BytecodeLocation.getRootParamName(pindex));
					actIn.setBytecodeIndex(BytecodeLocation.ROOT_PARAMETER);
					pindex++;
				}

				final PDGCallReturn outs = call2out.get(node);
				if (outs.retVal != null) {
					outs.retVal.setBytecodeName(BytecodeLocation.RETURN_PARAM);
					outs.retVal.setBytecodeIndex(BytecodeLocation.ROOT_PARAMETER);
				}
				if (outs.excVal != null) {
					outs.excVal.setBytecodeName(BytecodeLocation.EXCEPTION_PARAM);
					outs.excVal.setBytecodeIndex(BytecodeLocation.ROOT_PARAMETER);
				}
			} else if (node.getKind() == PDGNode.Kind.PHI) {
				node.setBytecodeIndex(BytecodeLocation.PHI);
				node.setBytecodeName(BytecodeLocation.PHI_NODE);
			}
		}

		for (PDGField hr : hread) {
			if (hr.field.isArray()) {
				hr.base.setSourceLocation(hr.node.getSourceLocation());
				hr.accfield.setSourceLocation(hr.node.getSourceLocation());
				hr.index.setSourceLocation(hr.node.getSourceLocation());
			} else if (hr.field.isStatic()) {
				hr.accfield.setSourceLocation(hr.node.getSourceLocation());
			} else {
				hr.base.setSourceLocation(hr.node.getSourceLocation());
				hr.accfield.setSourceLocation(hr.node.getSourceLocation());
			}
		}

		for (PDGField hw : hwrite) {
			if (hw.field.isArray()) {
				hw.base.setSourceLocation(hw.node.getSourceLocation());
				hw.accfield.setSourceLocation(hw.node.getSourceLocation());
				hw.index.setSourceLocation(hw.node.getSourceLocation());
			} else if (hw.field.isStatic()) {
				hw.accfield.setSourceLocation(hw.node.getSourceLocation());
			} else {
				hw.base.setSourceLocation(hw.node.getSourceLocation());
				hw.accfield.setSourceLocation(hw.node.getSourceLocation());
			}
		}

		entry.setBytecodeIndex(defBcIndex);
		entry.setBytecodeName(defBcName);
		exception.setBytecodeIndex(BytecodeLocation.ROOT_PARAMETER);
		exception.setBytecodeName(BytecodeLocation.EXCEPTION_PARAM);
		exit.setBytecodeIndex(BytecodeLocation.ROOT_PARAMETER);
		exit.setBytecodeName(BytecodeLocation.RETURN_PARAM);
	}

	private void addNodesForInstructions(final IR ir) {
		PDGNodeCreationVisitor visitor = new PDGNodeCreationVisitor(this, builder.getClassHierarchy(),
				builder.getParameterFieldFactory(), ir.getSymbolTable(), ignoreStaticFields);

		for (Iterator<SSAInstruction> it = ir.iterateAllInstructions(); it.hasNext();) {
			SSAInstruction instr = it.next();
			instr.visit(visitor);
			PDGNode node = visitor.lastNode;
			node2instr.put(node, instr);
			instr2node.put(instr, node);
		}

		// add root nodes for static field accesses
		{
			List<PDGField> sReads = new LinkedList<PDGField>();
			for (ParameterField field : fread) {
				if (field.isStatic()) {
					PDGNode sread = createNode(field.getName(), PDGNode.Kind.FORMAL_IN, field.getField().getFieldTypeReference());
					PDGField sfield = PDGField.formIn(sread, field);
					sReads.add(sfield);
				}
			}
			staticReads = new PDGField[sReads.size()];
			sReads.toArray(staticReads);
		}

		{
			List<PDGField> sWrites = new LinkedList<PDGField>();
			for (ParameterField field : fwrite) {
				if (field.isStatic()) {
					PDGNode swrite = createNode(field.getName(), PDGNode.Kind.FORMAL_OUT, field.getField().getFieldTypeReference());
					PDGField sfield = PDGField.formOut(swrite, field);
					sWrites.add(sfield);
				}
			}
			staticWrites = new PDGField[sWrites.size()];
			sWrites.toArray(staticWrites);
		}

		for (final PDGField fr : hread) {
			final SSAInstruction instr = getInstruction(fr.node);

			if (fr.base != null) {
				node2instr.put(fr.base, instr);
			}

			if (fr.accfield != null) {
				node2instr.put(fr.accfield, instr);
			}

			if (fr.index != null) {
				node2instr.put(fr.index, instr);
			}
		}

		for (final PDGField fw : hwrite) {
			final SSAInstruction instr = getInstruction(fw.node);

			if (fw.base != null) {
				node2instr.put(fw.base, instr);
			}

			if (fw.accfield != null) {
				node2instr.put(fw.accfield, instr);
			}

			if (fw.index != null) {
				node2instr.put(fw.index, instr);
			}
		}
	}

	private void addDummyConnections() {
		final boolean isVoid = method.getReturnType() == TypeReference.Void;

		PDGNode prev = entry;
		for (PDGNode p : params) {
			addEdge(prev, p, PDGEdge.Kind.CONTROL_FLOW);
			prev = p;
		}
		addEdge(prev, exception, PDGEdge.Kind.CONTROL_FLOW);
		addEdge(exception, exit, PDGEdge.Kind.CONTROL_FLOW);
		addEdge(entry, exit, PDGEdge.Kind.CONTROL_FLOW);

		addEdge(entry, exit, PDGEdge.Kind.CONTROL_DEP_EXPR);
		addEdge(entry, exception, PDGEdge.Kind.CONTROL_DEP_EXPR);
		for (PDGNode p : params) {
			addEdge(entry, p, PDGEdge.Kind.CONTROL_DEP_EXPR);
			addEdge(entry, p, PDGEdge.Kind.PARAM_STRUCT);
			addEdge(p, exception, PDGEdge.Kind.DATA_DEP);
			if (!isVoid) {
				addEdge(p, exit, PDGEdge.Kind.DATA_DEP);
			}
		}

		if (!isVoid) {
			addEdge(entry, exit, PDGEdge.Kind.PARAM_STRUCT);
		}
		addEdge(entry, exception, PDGEdge.Kind.PARAM_STRUCT);
	}

	@Override
	public boolean removeNode(PDGNode node) {
		SSAInstruction instr = node2instr.get(node);
		if (instr != null) {
			node2instr.remove(node);
			instr2node.remove(instr);
		}

		return super.removeNode(node);
	}

	private boolean isNopNode(PDGNode n) {
		return n.getKind() == PDGNode.Kind.NORMAL
				&& n.getLabel().equals(NOP_LABEL)
				&& PDGNode.DEFAULT_TYPE.equals(n.getTypeRef());
	}

	private PDGNode createNopNode() {
		PDGNode nop = createNode(NOP_LABEL, PDGNode.Kind.NORMAL, PDGNode.DEFAULT_TYPE);

		nop.setSourceLocation(defSrcLoc);
		nop.setBytecodeIndex(defBcIndex);
		nop.setBytecodeName(defBcName);

		return nop;
	}

	public PDGNode createNode(final String label, final PDGNode.Kind kind, final TypeReference type) {
		final int nodeId = builder.getNextNodeId();
		PDGNode node = new PDGNode(nodeId, id, label, kind, type);
		addVertex(node);

		return node;
	}

	public PDGNode createCallReturnNode(PDGNode call) {
		PDGNode callRetNode = createNode(SDGConstants.CALLRET_LABEL, Kind.NORMAL, PDGNode.DEFAULT_NO_TYPE);
		callRetNode.setBytecodeName(call.getBytecodeName());
		callRetNode.setSourceLocation(call.getSourceLocation());
		callRetNode.setBytecodeIndex(BytecodeLocation.CALL_RET);
		return callRetNode;
	}

	public PDGNode getNode(SSAInstruction instr) {
		return instr2node.get(instr);
	}

	public SSAInstruction getInstruction(PDGNode node) {
		return node2instr.get(node);
	}

	public String getSourceFile() {
		return sourceFile;
	}

	public List<PDGField> getStaticIn(PDGNode call) {
		if (call.getKind() != PDGNode.Kind.CALL) {
			throw new IllegalArgumentException("Argument not a call node: " + call);
		} else if (call.getPdgId() != id) {
			throw new IllegalArgumentException("Call node (" + call + ") not part of this pdg: " + toString());
		}

		return call2staticIn.get(call);
	}

	public List<PDGField> getStaticOut(PDGNode call) {
		if (call.getKind() != PDGNode.Kind.CALL) {
			throw new IllegalArgumentException("Argument not a call node: " + call);
		} else if (call.getPdgId() != id) {
			throw new IllegalArgumentException("Call node (" + call + ") not part of this pdg: " + toString());
		}

		return call2staticOut.get(call);
	}

	public PDGNode[] getParamIn(PDGNode call) {
		if (call.getKind() != PDGNode.Kind.CALL) {
			throw new IllegalArgumentException("Argument not a call node: " + call);
		} else if (call.getPdgId() != id) {
			throw new IllegalArgumentException("Call node (" + call + ") not part of this pdg: " + toString());
		}

		return call2in.get(call);
	}

	public PDGNode getReturnOut(PDGNode call) {
		if (call.getKind() != PDGNode.Kind.CALL) {
			throw new IllegalArgumentException("Argument not a call node: " + call);
		} else if (call.getPdgId() != id) {
			throw new IllegalArgumentException("Call node (" + call + ") not part of this pdg: " + toString());
		}

		final PDGCallReturn out = call2out.get(call);

		return out.retVal;
	}

	public PDGNode getExceptionOut(PDGNode call) {
		if (call.getKind() != PDGNode.Kind.CALL) {
			throw new IllegalArgumentException("Argument not a call node: " + call);
		} else if (call.getPdgId() != id) {
			throw new IllegalArgumentException("Call node (" + call + ") not part of this pdg: " + toString());
		}

		final PDGCallReturn out = call2out.get(call);

		return out.excVal;
	}

	public void addCall(PDGNode call, PDGNode[] in, PDGCallReturn out) {
		if (call.getKind() != PDGNode.Kind.CALL) {
			throw new IllegalArgumentException("Argument not a call node: " + call);
		} else if (call.getPdgId() != id) {
			throw new IllegalArgumentException("Call node (" + call + ") not part of this pdg: " + toString());
		} else if (out == null) {
			throw new IllegalArgumentException();
		}

		calls.add(call);
		call2in.put(call, in);
		call2out.put(call, out);
	}

	public void addReturn(final PDGNode ret) {
		assert ret.getPdgId() == id;

		returns.add(ret);
	}

	public List<PDGNode> getReturns() {
		return returns;
	}

	public void addFieldRead(ParameterField field, PDGNode node) {
		final PDGField f;

		if (field.isArray()) {
			final TypeReference elemType = field.getElementType();
			final TypeReference baseType = TypeReference.findOrCreateArrayOf(elemType);
			final PDGNode base = createNode("base", PDGNode.Kind.NORMAL, baseType);
			base.setBytecodeIndex(BytecodeLocation.BASE_FIELD);
			base.setBytecodeName(BytecodeLocation.BASE_PARAM);
			final PDGNode accfield = createNode("field [" + PrettyWalaNames.simpleTypeName(elemType) + "]",
					PDGNode.Kind.NORMAL, elemType);
			accfield.setBytecodeIndex(BytecodeLocation.ARRAY_FIELD);
			accfield.setBytecodeName(BytecodeLocation.ARRAY_PARAM);
			final PDGNode index = createNode("index", PDGNode.Kind.NORMAL, TypeReference.Int);
			index.setBytecodeIndex(BytecodeLocation.ARRAY_INDEX);
			index.setBytecodeName(BytecodeLocation.INDEX_PARAM);

			addEdge(base, node, PDGEdge.Kind.DATA_DEP);
			if (noBasePointerDependency) {
				addEdge(base, index, PDGEdge.Kind.CONTROL_DEP_EXPR);
				// the index value may trigger an out-of-bounds or negative-array-size exception,
				// therefore it also depends on the index value which cfg path is taken
				// => a forward slice from index node must contain nodes control dependent on the
				// array access. As currently these control dependencies are connected through the
				// base pointer node, we create a dummy connection to it.
				// A more sophisticated way would be to create an additional node for "end of array access" that
				// captures the possible control dependencies.
				addEdge(index, base, PDGEdge.Kind.CONTROL_DEP_EXPR);
				addEdge(index, node, PDGEdge.Kind.CONTROL_DEP_EXPR);
			} else {
				addEdge(node, base, PDGEdge.Kind.CONTROL_DEP_EXPR);
				addEdge(node, index, PDGEdge.Kind.CONTROL_DEP_EXPR);
			}
			addEdge(accfield, node, PDGEdge.Kind.DATA_DEP);
			addEdge(node, accfield, PDGEdge.Kind.CONTROL_DEP_EXPR);
			addEdge(index, node, PDGEdge.Kind.DATA_DEP);
			addEdge(base, accfield, PDGEdge.Kind.PARAM_STRUCT);

			f = PDGField.arrayGet(node, base, accfield, index, field);
		} else if (field.isStatic()) {
			final IField ifield = field.getField();
			final PDGNode accfield = createNode("field " + PrettyWalaNames.simpleFieldName(ifield), PDGNode.Kind.NORMAL,
					ifield.getFieldTypeReference());
			accfield.setBytecodeIndex(BytecodeLocation.STATIC_FIELD);
			accfield.setBytecodeName(PrettyWalaNames.bcFieldName(ifield));

			addEdge(accfield, node, PDGEdge.Kind.DATA_DEP);
			addEdge(node, accfield, PDGEdge.Kind.CONTROL_DEP_EXPR);

			f = PDGField.fieldGetStatic(node, accfield, field);
		} else {
			final IField ifield = field.getField();
			final PDGNode base = createNode("base", PDGNode.Kind.NORMAL, ifield.getDeclaringClass().getReference());
			base.setBytecodeIndex(BytecodeLocation.BASE_FIELD);
			base.setBytecodeName(BytecodeLocation.BASE_PARAM);
			final PDGNode accfield = createNode("field " + PrettyWalaNames.simpleFieldName(ifield), PDGNode.Kind.NORMAL,
					ifield.getFieldTypeReference());
			accfield.setBytecodeIndex(BytecodeLocation.OBJECT_FIELD);
			accfield.setBytecodeName(PrettyWalaNames.bcFieldName(ifield));

			addEdge(base, node, PDGEdge.Kind.DATA_DEP);
			if (noBasePointerDependency) {
				addEdge(base, node, PDGEdge.Kind.CONTROL_DEP_EXPR);
			} else {
				addEdge(node, base, PDGEdge.Kind.CONTROL_DEP_EXPR);
			}
			addEdge(accfield, node, PDGEdge.Kind.DATA_DEP);
			addEdge(node, accfield, PDGEdge.Kind.CONTROL_DEP_EXPR);
			addEdge(base, accfield, PDGEdge.Kind.PARAM_STRUCT);

			f = PDGField.fieldGet(node, base, accfield, field);
		}

		hread.add(f);
		fread.add(field);
	}

	public void addFieldWrite(ParameterField field, PDGNode node) {
		final PDGField f;

		if (field.isArray()) {
			final TypeReference elemType = field.getElementType();
			final TypeReference baseType = TypeReference.findOrCreateArrayOf(elemType);
			final PDGNode base = createNode("base", PDGNode.Kind.NORMAL, baseType);
			base.setBytecodeIndex(BytecodeLocation.BASE_FIELD);
			base.setBytecodeName(BytecodeLocation.BASE_PARAM);
			final PDGNode accfield = createNode("field [" + PrettyWalaNames.simpleTypeName(elemType) + "]",
					PDGNode.Kind.NORMAL, elemType);
			accfield.setBytecodeIndex(BytecodeLocation.ARRAY_FIELD);
			accfield.setBytecodeName(BytecodeLocation.ARRAY_PARAM);
			final PDGNode index = createNode("index", PDGNode.Kind.NORMAL, TypeReference.Int);
			index.setBytecodeIndex(BytecodeLocation.ARRAY_INDEX);
			index.setBytecodeName(BytecodeLocation.INDEX_PARAM);

			addEdge(base, node, PDGEdge.Kind.DATA_DEP);
			if (noBasePointerDependency) {
				addEdge(base, index, PDGEdge.Kind.CONTROL_DEP_EXPR);
				// the index value may trigger an out-of-bounds or negative-array-size exception,
				// therefore it also depends on the index value which cfg path is taken
				// => a forward slice from index node must contain nodes control dependent on the
				// array access. As currently these control dependencies are connected through the
				// base pointer node, we create a dummy connection to it.
				// A more sophisticated way would be to create an additional node for "end of array access" that
				// captures the possible control dependencies.
				addEdge(index, base, PDGEdge.Kind.CONTROL_DEP_EXPR);
				addEdge(index, node, PDGEdge.Kind.CONTROL_DEP_EXPR);
			} else {
				addEdge(node, base, PDGEdge.Kind.CONTROL_DEP_EXPR);
				addEdge(node, index, PDGEdge.Kind.CONTROL_DEP_EXPR);
			}
			addEdge(node, accfield, PDGEdge.Kind.DATA_DEP);
			addEdge(node, accfield, PDGEdge.Kind.CONTROL_DEP_EXPR);
			addEdge(index, node, PDGEdge.Kind.DATA_DEP);
			addEdge(base, accfield, PDGEdge.Kind.PARAM_STRUCT);

			f = PDGField.arraySet(node, base, accfield, index, field);
		} else if (field.isStatic()) {
			final IField ifield = field.getField();
			final PDGNode accfield = createNode("field " + PrettyWalaNames.simpleFieldName(ifield), PDGNode.Kind.NORMAL,
					ifield.getFieldTypeReference());
			accfield.setBytecodeIndex(BytecodeLocation.STATIC_FIELD);
			accfield.setBytecodeName(PrettyWalaNames.bcFieldName(ifield));

			addEdge(node, accfield, PDGEdge.Kind.DATA_DEP);
			addEdge(node, accfield, PDGEdge.Kind.CONTROL_DEP_EXPR);

			f = PDGField.fieldSetStatic(node, accfield, field);
		} else {
			final IField ifield = field.getField();
			final PDGNode base = createNode("base", PDGNode.Kind.NORMAL, ifield.getDeclaringClass().getReference());
			base.setBytecodeIndex(BytecodeLocation.BASE_FIELD);
			base.setBytecodeName(BytecodeLocation.BASE_PARAM);
			final PDGNode accfield = createNode("field " + PrettyWalaNames.simpleFieldName(ifield), PDGNode.Kind.NORMAL,
					ifield.getFieldTypeReference());
			accfield.setBytecodeIndex(BytecodeLocation.OBJECT_FIELD);
			accfield.setBytecodeName(PrettyWalaNames.bcFieldName(ifield));

			addEdge(base, node, PDGEdge.Kind.DATA_DEP);
			if (noBasePointerDependency) {
				addEdge(base, node, PDGEdge.Kind.CONTROL_DEP_EXPR);
			} else {
				addEdge(node, base, PDGEdge.Kind.CONTROL_DEP_EXPR);
			}
			addEdge(node, accfield, PDGEdge.Kind.DATA_DEP);
			addEdge(node, accfield, PDGEdge.Kind.CONTROL_DEP_EXPR);
			addEdge(base, accfield, PDGEdge.Kind.PARAM_STRUCT);

			f = PDGField.fieldSet(node, base, accfield, field);
		}

		hwrite.add(f);
		fwrite.add(field);
	}

	public List<PDGField> getFieldReads() {
		return hread;
	}

	public List<PDGField> getFieldWrites() {
		return hwrite;
	}

	public PDGNode addOutputFieldChildTo(final PDGNode parent, final String name, final String bcFieldName,
			final int bcIndex, final TypeReference bcType) {
		if (parent.getPdgId() != id || !containsVertex(parent)) {
			throw new IllegalArgumentException("Not part of this pdg: " + parent + " - " + toString());
		} else if (!isParamNode(parent) && parent.getKind() != PDGNode.Kind.CALL && parent != entry) {
			throw new IllegalArgumentException("Not a parameter node: " + parent + " - " + toString());
		}

		final PDGNode nf;
		if (parent.getKind() == PDGNode.Kind.FORMAL_IN || parent.getKind() == PDGNode.Kind.FORMAL_OUT
				|| parent.getKind() == PDGNode.Kind.EXIT || parent.getKind() == PDGNode.Kind.ENTRY) {
			nf = createNode(name, PDGNode.Kind.FORMAL_OUT, bcType);
		} else if (parent.getKind() == PDGNode.Kind.ACTUAL_IN || parent.getKind() == PDGNode.Kind.ACTUAL_OUT
				|| parent.getKind() == PDGNode.Kind.CALL) {
			nf = createNode(name, PDGNode.Kind.ACTUAL_OUT, bcType);
		} else {
			throw new IllegalStateException();
		}

		nf.setBytecodeIndex(bcIndex);
		nf.setBytecodeName(bcFieldName);
		nf.setSourceLocation(parent.getSourceLocation());

		// add parameter structure
		addEdge(parent, nf, PDGEdge.Kind.PARAM_STRUCT);

		if (parent.getKind() == PDGNode.Kind.CALL) {
			addEdge(parent, nf, PDGEdge.Kind.CONTROL_DEP_EXPR);
			addToNormalControlFlowAfter(parent, nf);
		} else if (parent.getKind() == PDGNode.Kind.ENTRY) {
			addEdge(parent, nf, PDGEdge.Kind.CONTROL_DEP_EXPR);
			addToNormalControlFlowBefore(this.exit, nf);
		} else {
			// add node to control dependence
			for (PDGEdge in : incomingEdgesOf(parent)) {
				if (in.kind.isControl()) {
					addEdge(in.from, nf, in.kind);
				}
			}

			// add node to control flow
			if (parent.getKind() == PDGNode.Kind.ACTUAL_IN || parent.getKind() == PDGNode.Kind.FORMAL_IN
					|| parent.getKind() == PDGNode.Kind.EXIT || parent.getKind() == PDGNode.Kind.ENTRY) {
				addToNormalControlFlowBefore(this.exit, nf);
			} else {
				addToNormalControlFlowAfter(parent, nf);
			}
		}

		return nf;
	}

	private void addToNormalControlFlowBefore(final PDGNode reference, final PDGNode toAdd) {
		List<PDGEdge> toRemove = new LinkedList<PDGEdge>();
		for (PDGEdge in : incomingEdgesOf(reference)) {
			if (in.kind.isFlow()) {
				addEdge(in.from, toAdd, in.kind);
				toRemove.add(in);
			}
		}
		removeAllEdges(toRemove);
		addEdge(toAdd, reference, PDGEdge.Kind.CONTROL_FLOW);
	}

	private void addToNormalControlFlowAfter(final PDGNode reference, final PDGNode toAdd) {
		List<PDGEdge> toRemove = new LinkedList<PDGEdge>();
		for (PDGEdge out : outgoingEdgesOf(reference)) {
			if (out.kind.isFlow()) {
				addEdge(toAdd, out.to, out.kind);
				toRemove.add(out);
			}
		}
		removeAllEdges(toRemove);
		addEdge(reference, toAdd, PDGEdge.Kind.CONTROL_FLOW);
	}

	public PDGNode addInputFieldChildTo(final PDGNode parent, final String name, final String bcFieldName,
			final int bcIndex, final TypeReference bcType) {
		if (parent.getPdgId() != id || !containsVertex(parent)) {
			throw new IllegalArgumentException("Not part of this pdg: " + parent + " - " + toString());
		} else if (!isParamNode(parent) && parent.getKind() != PDGNode.Kind.CALL) {
			throw new IllegalArgumentException("Not a parameter node: " + parent + " - " + toString());
		}

		final PDGNode nf;
		if (parent.getKind() == PDGNode.Kind.FORMAL_IN) {
			nf = createNode(name, PDGNode.Kind.FORMAL_IN, bcType);
		} else if (parent.getKind() == PDGNode.Kind.ACTUAL_IN || parent.getKind() == PDGNode.Kind.CALL) {
			nf = createNode(name, PDGNode.Kind.ACTUAL_IN, bcType);
		} else {
			throw new IllegalStateException();
		}

		nf.setBytecodeIndex(bcIndex);
		nf.setBytecodeName(bcFieldName);
		nf.setSourceLocation(parent.getSourceLocation());

		// add parameter structure
		addEdge(parent, nf, PDGEdge.Kind.PARAM_STRUCT);

		if (parent.getKind() == PDGNode.Kind.CALL) {
			addEdge(parent, nf, PDGEdge.Kind.CONTROL_DEP_EXPR);
			addToNormalControlFlowBefore(parent, nf);
		} else {
			// add node to control dependence
			for (PDGEdge in : incomingEdgesOf(parent)) {
				if (in.kind.isControl()) {
					addEdge(in.from, nf, in.kind);
				}
			}

			// add node to control flow
			addToNormalControlFlowAfter(parent, nf);
		}

		return nf;
	}

	private static final boolean isParamNode(PDGNode p) {
		switch (p.getKind()) {
		case ACTUAL_IN:
		case ACTUAL_OUT:
		case EXIT:
		case FORMAL_IN:
		case FORMAL_OUT:
			return true;
		default:
			return false;
		}
	}

	public int getId() {
		return id;
	}

	public IMethod getMethod() {
		return method;
	}

	public List<PDGNode> getCalls() {
		List<PDGNode> calls = new LinkedList<PDGNode>();

		for (PDGNode n : vertexSet()) {
			if (n.getPdgId() == id && n.getKind() == PDGNode.Kind.CALL) {
				calls.add(n);
			}
		}

		return calls;
	}

	public String toString() {
		return "PDG of " + method.getSignature();
	}

	public void connectCall(PDGNode call, Set<PDG> tgts) {
		if (call.getPdgId() != id || call.getKind() != PDGNode.Kind.CALL) {
			throw new IllegalArgumentException("Not a call node of this pdg: " + call);
		}

		final PDGNode[] actIn = call2in.get(call);
		final PDGCallReturn actOut = call2out.get(call);

		for (PDG tgt : tgts) {
			addVertex(tgt.entry);
			if (call.getType().equals("static")) {
				addEdge(call, tgt.entry, PDGEdge.Kind.CALL_STATIC);
			} else {
				addEdge(call, tgt.entry, PDGEdge.Kind.CALL_VIRTUAL);
			}

			for (int i = 0; i < actIn.length; i++) {
				PDGNode formIn = tgt.params[i];
				addVertex(formIn);
				addEdge(actIn[i], formIn, PDGEdge.Kind.PARAMETER_IN);
			}

			if (actOut.retVal != null) {
				tgt.addVertex(actOut.retVal);
				tgt.addEdge(tgt.exit, actOut.retVal, PDGEdge.Kind.PARAMETER_OUT);
			}

			if (actOut.excVal != null) {
				tgt.addVertex(actOut.excVal);
				tgt.addEdge(tgt.exception, actOut.excVal, PDGEdge.Kind.PARAMETER_OUT);
			}
		}
	}


	private int nodeID; // set to pdgId at constructor

	@Override
	public int getGraphNodeId() {
		return nodeID;
	}

	@Override
	public void setGraphNodeId(int number) {
		this.nodeID = number;
	}

	public void addStaticRead(ParameterField field) {
		if (!fread.contains(field) ) {
			fread.add(field);
			PDGNode sread = createNode(field.getName(), PDGNode.Kind.FORMAL_IN, field.getField().getFieldTypeReference());
			IField f = field.getField();
			sread.setBytecodeName(PrettyWalaNames.bcFieldName(f));
			sread.setBytecodeIndex(BytecodeLocation.STATIC_FIELD);
			sread.setSourceLocation(entry.getSourceLocation());
			sread.setParameterField(field);

			PDGField pdgField = PDGField.formIn(sread, field);
			staticInterprocReads.add(pdgField);

			addEdge(entry, sread, PDGEdge.Kind.CONTROL_DEP_EXPR);
			addEdge(entry, sread, PDGEdge.Kind.PARAM_STRUCT);
			// append in controlflow after entry node.
			addToControlFlowAfter(sread, entry);
		}
	}

	public void addStaticWrite(ParameterField field) {
		if (!fwrite.contains(field) ) {
			fwrite.add(field);
			PDGNode swrite = createNode(field.getName(), PDGNode.Kind.FORMAL_OUT, field.getField().getFieldTypeReference());
			IField f = field.getField();
			swrite.setBytecodeName(PrettyWalaNames.bcFieldName(f));
			swrite.setBytecodeIndex(BytecodeLocation.STATIC_FIELD);
			swrite.setSourceLocation(entry.getSourceLocation());
			swrite.setParameterField(field);

			PDGField pdgField = PDGField.formOut(swrite, field);
			staticInterprocWrites.add(pdgField);

			addEdge(entry, swrite, PDGEdge.Kind.CONTROL_DEP_EXPR);
			addEdge(entry, swrite, PDGEdge.Kind.PARAM_STRUCT);
			// append in controlflow before exit node.
			addToControlFlowBefore(swrite, exit);
		}
	}

	public PDGNode addStaticReadToCall(PDGNode call, ParameterField field) {
		List<PDGField> sIns = call2staticIn.get(call);
		if (sIns == null) {
			sIns = new LinkedList<PDGField>();
			call2staticIn.put(call, sIns);
		}

		PDGField pdgField = null;
		for (PDGField f : sIns) {
			if (f.field.equals(field)) {
				pdgField = f;
				break;
			}
		}

		if (pdgField == null) {
			// create new node
			PDGNode actIn = createNode(field.getName(), PDGNode.Kind.ACTUAL_IN, field.getField().getFieldTypeReference());
			IField f = field.getField();
			actIn.setBytecodeName(PrettyWalaNames.bcFieldName(f));
			actIn.setBytecodeIndex(BytecodeLocation.STATIC_FIELD);
			actIn.setSourceLocation(call.getSourceLocation());
			actIn.setParameterField(field);

			addEdge(call, actIn, PDGEdge.Kind.CONTROL_DEP_EXPR);
			addEdge(call, actIn, PDGEdge.Kind.PARAM_STRUCT);

			// add to control flow
			addToControlFlowBefore(actIn, call);

			pdgField = PDGField.actIn(actIn, field);
			sIns.add(pdgField);
		}

		return pdgField.node;
	}

	private void addToControlFlowAfter(final PDGNode toAdd, final PDGNode refPoint) {
		List<PDGNode> cfSuccs = new LinkedList<PDGNode>();
		List<PDGNode> cfeSuccs = new LinkedList<PDGNode>();
		List<PDGEdge> toRemove = new LinkedList<PDGEdge>();
		for (PDGEdge edge : outgoingEdgesOf(refPoint)) {
			if (refPoint == entry && (edge.to == exception || edge.to == exit)) {
				continue;
			}

			if (edge.kind == PDGEdge.Kind.CONTROL_FLOW) {
				cfSuccs.add(edge.to);
				toRemove.add(edge);
			} else if (edge.kind == PDGEdge.Kind.CONTROL_FLOW_EXC) {
				cfeSuccs.add(edge.to);
				toRemove.add(edge);
			}
		}
		removeAllEdges(toRemove);
		addEdge(refPoint, toAdd, PDGEdge.Kind.CONTROL_FLOW);
		for (PDGNode cfs : cfSuccs) {
			addEdge(toAdd, cfs, PDGEdge.Kind.CONTROL_FLOW);
		}
		for (PDGNode cfes : cfeSuccs) {
			addEdge(toAdd, cfes, PDGEdge.Kind.CONTROL_FLOW_EXC);
		}
	}

	private void addToControlFlowBefore(final PDGNode toAdd, final PDGNode refPoint) {
		List<PDGNode> cfPreds = new LinkedList<PDGNode>();
		List<PDGNode> cfePreds = new LinkedList<PDGNode>();
		List<PDGEdge> toRemove = new LinkedList<PDGEdge>();
		for (PDGEdge edge : incomingEdgesOf(refPoint)) {
			if (edge.kind == PDGEdge.Kind.CONTROL_FLOW) {
				cfPreds.add(edge.from);
				toRemove.add(edge);
			} else if (edge.kind == PDGEdge.Kind.CONTROL_FLOW_EXC) {
				cfePreds.add(edge.from);
				toRemove.add(edge);
			}
		}
		removeAllEdges(toRemove);
		addEdge(toAdd, refPoint, PDGEdge.Kind.CONTROL_FLOW);
		for (PDGNode cfp : cfPreds) {
			addEdge(cfp, toAdd, PDGEdge.Kind.CONTROL_FLOW);
		}
		for (PDGNode cfep : cfePreds) {
			addEdge(cfep, toAdd, PDGEdge.Kind.CONTROL_FLOW_EXC);
		}
	}

	public PDGNode addStaticWriteToCall(PDGNode call, ParameterField field) {
		List<PDGField> sOuts = call2staticOut.get(call);
		if (sOuts == null) {
			sOuts = new LinkedList<PDGField>();
			call2staticOut.put(call, sOuts);
		}

		PDGField pdgField = null;
		for (PDGField f : sOuts) {
			if (f.field.equals(field)) {
				pdgField = f;
				break;
			}
		}

		if (pdgField == null) {
			// create new node
			PDGNode actOut = createNode(field.getName(), PDGNode.Kind.ACTUAL_OUT, field.getField().getFieldTypeReference());
			IField f = field.getField();
			actOut.setBytecodeName(PrettyWalaNames.bcFieldName(f));
			actOut.setBytecodeIndex(BytecodeLocation.STATIC_FIELD);
			actOut.setSourceLocation(call.getSourceLocation());
			actOut.setParameterField(field);

			addEdge(call, actOut, PDGEdge.Kind.CONTROL_DEP_EXPR);
			addEdge(call, actOut, PDGEdge.Kind.PARAM_STRUCT);

			// add to control flow
			addToControlFlowAfter(actOut, call);

			pdgField = PDGField.actOut(actOut, field);
			sOuts.add(pdgField);
		}

		return pdgField.node;
	}

	public Map<PDGNode, ParameterField> getStaticAccessMap() {
		Map<PDGNode, ParameterField> access = new HashMap<PDGNode, ParameterField>();

		for (PDGField f : staticReads) {
			access.put(f.node, f.field);
		}

		for (PDGField f : staticWrites) {
			access.put(f.node, f.field);
		}

		for (PDGField f : staticInterprocReads) {
			access.put(f.node, f.field);
		}

		for (PDGField f : staticInterprocWrites) {
			access.put(f.node, f.field);
		}

		for (PDGNode call : calls) {
			List<PDGField> actIns = call2staticIn.get(call);
			if (actIns != null) {
				for (PDGField f : actIns) {
					access.put(f.node, f.field);
				}
			}

			List<PDGField> actOuts = call2staticOut.get(call);
			if (actOuts != null) {
				for (PDGField f : actOuts) {
					access.put(f.node, f.field);
				}
			}
		}

		for (PDGField f : hread) {
			if (f.field.isStatic()) {
				access.put(f.node, f.field);
			}
		}

		for (PDGField f : hwrite) {
			if (f.field.isStatic()) {
				access.put(f.node, f.field);
			}
		}

		return access;
	}

	public PDGField getField(PDGNode node) {
		if (node.getPdgId() != id) {
			throw new IllegalArgumentException("Node not part of this pdg: " + node + " not in " + toString());
		}

		PDGField result = null;

		switch (node.getKind()) {
		case FORMAL_IN: {
			result = findFieldForNode(staticReads, node);
			if (result == null) {
				result = findFieldForNode(staticInterprocReads, node);
			}
		} break;
		case FORMAL_OUT: {
			result = findFieldForNode(staticWrites, node);
			if (result == null) {
				result = findFieldForNode(staticInterprocWrites, node);
			}
		} break;
		case HREAD: {
			result = findFieldForNode(hread, node);
		} break;
		case HWRITE: {
			result = findFieldForNode(hwrite, node);
		} break;
		default:
			throw new IllegalArgumentException("Don't know how to search a field for node: " + node);
		}

		assert result != null;

		return result;
	}

	public boolean hasReturnValue() {
		return !isVoid();
	}

	public boolean isVoid() {
		return method.getReturnType() == TypeReference.Void;
	}

	public PDGNode searchInputField(final PDGNode parent, final String bcField) {
		for (PDGEdge out : outgoingEdgesOf(parent)) {
			if (out.kind == PDGEdge.Kind.PARAM_STRUCT && bcField.equals(out.to.getBytecodeName())) {
				final PDGNode field = out.to;
				if (field.getKind() == Kind.FORMAL_IN || field.getKind() == Kind.ACTUAL_IN) {
					return field;
				}
			}
		}

		return null;
	}

	public PDGNode searchOutputField(final PDGNode parent, final String bcField) {
		for (PDGEdge out : outgoingEdgesOf(parent)) {
			if (out.kind == PDGEdge.Kind.PARAM_STRUCT && bcField.equals(out.to.getBytecodeName())) {
				final PDGNode field = out.to;
				if (field.getKind() == Kind.FORMAL_OUT || field.getKind() == Kind.ACTUAL_OUT || field.getKind() == Kind.EXIT) {
					return field;
				}
			}
		}

		return null;
	}

	public TypeReference getParamType(int i) {
		return method.getParameterType(i);
	}

	public TypeReference getParamType(PDGNode call, int i) {
		if (call.getPdgId() != id || call.getKind() != PDGNode.Kind.CALL) {
			throw new IllegalArgumentException(call + " is not a call nodes contained in this pdg: " + this);
		}

		final SSAAbstractInvokeInstruction invk = (SSAAbstractInvokeInstruction) getInstruction(call);
		final MethodReference mRef = invk.getDeclaredTarget();
		TypeReference type = null;

		if (invk.isStatic()) {
			type = mRef.getParameterType(i);
		} else {
			if (i == 0) {
				type = mRef.getDeclaringClass();
			} else {
				type = mRef.getParameterType(i - 1);
			}
		}

		return type;
	}

	public PDGNode getNodeWithId(final int id) {
		for (PDGNode n : vertexSet()) {
			if (n.getId() == id) {
				return n;
			}
		}

		return null;
	}

	/**
	 * Search the corresponding formal-in node for the given formal-out node.
	 * It may not always exists. E.g. static fields that are only written but never read or the exception exit node.
	 * @param n A formal-out node that is contained in this pdg.
	 * @return The matching formal-in node or null.
	 */
	public PDGNode getMatchingFormalIn(final PDGNode n) {
		if (n.getPdgId() != id || !containsVertex(n)) {
			throw new IllegalArgumentException("not part of this pdg: " + n);
		} else if (n.getKind() != PDGNode.Kind.FORMAL_OUT) {
			throw new IllegalArgumentException("not a formal-out node: " + n);
		}

		LinkedList<PDGNode> pathToRoot = new LinkedList<PDGNode>();
		getPathToRoot(n, pathToRoot, new HashSet<PDGNode>());

		PDGNode cur = null;

		for (PDGNode p : pathToRoot) {
			if (cur == null) {
				cur = p;
				assert p == entry;
			} else {
				boolean found = false;

				for (final PDGEdge out : outgoingEdgesOf(cur)) {
					if (out.kind == PDGEdge.Kind.PARAM_STRUCT) {
						final PDGNode succ = out.to;
						if (succ.getKind() == PDGNode.Kind.FORMAL_IN && succ.getBytecodeName().equals(p.getBytecodeName())) {
							cur = succ;
							found = true;
							break;
						}
					}
				}

				if (!found) {
					return null;
				}
			}
		}

		return cur;
	}

	public List<Pair<PDGNode, PDGNode>> getFormInOutToActualNodeForCall(final PDGNode call, final PDG callee) {
		if (call.getPdgId() != id || call.getKind() != PDGNode.Kind.CALL) {
			throw new IllegalArgumentException();
		}

		final List<Pair<PDGNode, PDGNode>> match = new LinkedList<Pair<PDGNode,PDGNode>>();

		for (PDGEdge e : outgoingEdgesOf(call)) {
			if (e.kind == PDGEdge.Kind.CONTROL_DEP_EXPR && e.to.getKind() == PDGNode.Kind.ACTUAL_IN) {
				for (PDGEdge ae : outgoingEdgesOf(e.to)) {
					if (ae.kind == PDGEdge.Kind.PARAMETER_IN && ae.to.getPdgId() == callee.getId()) {
						// add match formal-in <-> actual-in
						match.add(Pair.make(ae.to, e.to));
					}
				}
			}
		}

		for (PDGEdge e : callee.outgoingEdgesOf(callee.entry)) {
			if (e.kind == PDGEdge.Kind.CONTROL_DEP_EXPR && (e.to.getKind() == PDGNode.Kind.FORMAL_OUT || e.to.getKind() == PDGNode.Kind.EXIT)) {
				for (PDGEdge fe : callee.outgoingEdgesOf(e.to)) {
					if (fe.kind == PDGEdge.Kind.PARAMETER_OUT && fe.to.getPdgId() == id) {
						// add match formal-out <-> actual-out
						match.add(Pair.make(e.to, fe.to));
					}
				}
			}
		}

		return match;
	}

	private void getPathToRoot(final PDGNode n, LinkedList<PDGNode> path, Set<PDGNode> visited) {
		if (visited.contains(n)) {
			return;
		}

		path.addFirst(n);
		visited.add(n);

		if (n == entry) {
			return;
		}

		for (final PDGEdge in : incomingEdgesOf(n)) {
			if (in.kind == PDGEdge.Kind.PARAM_STRUCT) {
				getPathToRoot(in.from, path, visited);
			}
		}
	}

	public List<Pair<PDGNode, PDGNode>> getActInOutPairs(PDGNode call) {
		if (call.getPdgId() != id) {
			throw new IllegalArgumentException();
		} else if (call.getKind() != PDGNode.Kind.CALL) {
			throw new IllegalArgumentException();
		}

		final List<PDGNode> actIns = new LinkedList<PDGNode>();
		final List<PDGNode> actOuts = new LinkedList<PDGNode>();

		for (final PDGEdge e : outgoingEdgesOf(call)) {
			if (e.kind == PDGEdge.Kind.CONTROL_DEP_EXPR) {
				if (e.to.getKind() == PDGNode.Kind.ACTUAL_IN) {
					actIns.add(e.to);
				} else if (e.to.getKind() == PDGNode.Kind.ACTUAL_OUT) {
					actOuts.add(e.to);
				}
			}
		}

		final List<Pair<PDGNode, PDGNode>> aio = new LinkedList<Pair<PDGNode,PDGNode>>();
		for (final PDGNode ai : actIns) {
			for (final PDGNode ao : actOuts) {
				if (((ai.isRootParam() && ao.isRootParam()) || (ai.isStaticField() && ao.isStaticField()))
						&& ai.getBytecodeName().equals(ao.getBytecodeName())) {
					aio.add(Pair.make(ai, ao));
				}
			}
		}

		return aio;
	}

	public List<Pair<PDGNode, PDGNode>> getFormInOutPairs() {
		final List<PDGNode> formIns = new LinkedList<PDGNode>();
		final List<PDGNode> formOuts = new LinkedList<PDGNode>();

		for (final PDGEdge e : outgoingEdgesOf(entry)) {
			if (e.kind == PDGEdge.Kind.CONTROL_DEP_EXPR) {
				if (e.to.getKind() == PDGNode.Kind.FORMAL_IN) {
					formIns.add(e.to);
				} else if (e.to.getKind() == PDGNode.Kind.FORMAL_OUT) {
					formOuts.add(e.to);
				}
			}
		}

		final List<Pair<PDGNode, PDGNode>> fio = new LinkedList<Pair<PDGNode,PDGNode>>();
		for (final PDGNode fi : formIns) {
			for (final PDGNode fo : formOuts) {
				if (((fi.isRootParam() && fo.isRootParam()) || (fi.isStaticField() && fo.isStaticField()))
						&& fi.getBytecodeName().equals(fo.getBytecodeName())) {
					fio.add(Pair.make(fi, fo));
				}
			}
		}

		return fio;
	}

	public boolean isImmutable(final TypeReference tref) {
		return builder.isImmutableStub(tref);
	}

}
