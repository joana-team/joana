/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.deprecated.jsdg.sdg.nodes;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ssa.ConstantValue;
import com.ibm.wala.ssa.IR;
import com.ibm.wala.ssa.SSAArrayLoadInstruction;
import com.ibm.wala.ssa.SSAArrayStoreInstruction;
import com.ibm.wala.ssa.SSAGetInstruction;
import com.ibm.wala.ssa.SSAInstruction;
import com.ibm.wala.ssa.SSAInvokeInstruction;
import com.ibm.wala.ssa.SSAMonitorInstruction;
import com.ibm.wala.ssa.SSAPhiInstruction;
import com.ibm.wala.ssa.SSAPutInstruction;
import com.ibm.wala.util.graph.labeled.SlowSparseNumberedLabeledGraph;

import edu.kit.joana.deprecated.jsdg.util.Util;
import gnu.trove.map.hash.TIntObjectHashMap;

/**
 * Contains all dependency and node creation methods needed for pdg and sdg
 * creation later on. Is implemented as a subclass of the Wala internal graph
 * representation.
 *
 * @author Juergen Graf <graf@kit.edu>
 *
 */
public abstract class JDependencyGraph
extends SlowSparseNumberedLabeledGraph<AbstractPDGNode, JDependencyGraph.EdgeType>
implements IPDGNodeFactory {

	public static enum EdgeType {
		PS,		// parameter structure edge
		CL,		// call edge
		SU,		// summary edge
		DD,		// data dependency
		DH,		// data dependency through heap values
		CE,		// control dependency for expressions
		CD_TRUE,		// control dependency true
		CD_FALSE,		// control dependency false
		CD_EX,  // control dependency for exceptions
		NTSCD,	// nontermination sensitive control dependency (see ranganath et al.)
		HE,		// help/utility edge
		UN,		// unconditional control dependency
		CF,		// control flow
		VD,		// virtual dependency
		PI,		// parameter-in edge
		PO,		// parameter-out edge
		RD,		// reference dependency
		CC,		// call control dependency
		ID,		// read-write interference for threads
		IW,		// write-write interference for threads
		FORK,	// call from thread.start to thread.run methods are not simple
				// call edges they are fork edges
		FORK_IN,// parameter in edges for new threads
		FORK_OUT// parameter out edges for new thread
	}

	public static class PDGFormatException extends Exception {

		/**
		 * generated
		 */
		private static final long serialVersionUID = -1241238642317190894L;

		public PDGFormatException() {
			super();
		}

		public PDGFormatException(Exception exc) {
			super(exc);
		}

		public PDGFormatException(String msg) {
			super(msg);
		}
	}

	private final Map<SSAInstruction, List<AbstractPDGNode>> ssa2node;
	private final Map<AbstractPDGNode, SSAInstruction> node2ssa;
	private final Map<SSAInstruction, Integer> ssa2index;

	protected int numControlDep = 0;
	protected int numCallDep = 0;
	protected int numDataDep = 0;
	protected int numParamInDep = 0;
	protected int numParamOutDep = 0;
	protected int numSumEdge = 0;

	@Override
	public final void addEdge(AbstractPDGNode src, AbstractPDGNode dst) throws IllegalArgumentException {
		throw new UnsupportedOperationException("please use addEdge(node, node, kind) to specify the type of edge instead.");
	}

	public JDependencyGraph() {
		super(JDependencyGraph.EdgeType.DD);
		this.ssa2node = new HashMap<SSAInstruction, List<AbstractPDGNode>>();
		this.node2ssa = new HashMap<AbstractPDGNode, SSAInstruction>();
		this.ssa2index = new HashMap<SSAInstruction, Integer>();
	}

	public abstract int getId();

	public abstract AbstractPDGNode getRoot();

	/**
	 * Gets all pdg nodes belonging to a specified ssa instrutions. Most of the
	 * time an ssa instruction maps only to a single node. But method calls
	 * e.g. map to the call node and the corresponding actual in/out nodes.
	 *
	 * By Convention the first element of the list that is returned is the call
	 * node and never an actual-in/out or form-in/out node.
	 * @param instr ssa instruction
	 * @return nodes belonging to this instruction. First element of this
	 * list is special (see above description).
	 */
	public List<AbstractPDGNode> getNodesForInstruction(SSAInstruction instr) {
		return ssa2node.get(instr);
	}

	/**
	 * Gets the ssa instruction mapped to the pdg node. At most a single
	 * instruction is mapped to a node but it may be none.
	 * @param node pdg node
	 * @return ssa instruction correspondig to the pdg node (may be null).
	 */
	public SSAInstruction getInstructionForNode(AbstractPDGNode node) {
		return node2ssa.get(node);
	}

	/**
	 * Returns the index of the specified ssa instrucion. The specified ssa
	 * instruction has to be part of the intermediate reprenstantion (ir) of
	 * the method this pdg is built for.
	 * @param instr ssa instruction
	 * @return index of the ssa instruction
	 */
	public Integer getSSAIndex(SSAInstruction instr) {
		return ssa2index.get(instr);
	}

	/**
	 * Saves the mapping of an ssa instruction to its index number.
	 * @param instr ssa instrucion
	 * @param index index of the ssa instruction
	 */
	protected void addSSAIndex(SSAInstruction instr, int index) {
		ssa2index.put(instr, index);
	}

	protected void addToMap(AbstractPDGNode node, SSAInstruction instr) {
		assert (instr != null);

		List<AbstractPDGNode> list = ssa2node.get(instr);
		if (list == null) {
			list = new ArrayList<AbstractPDGNode>();
			ssa2node.put(instr, list);
			node2ssa.put(node, instr);
		}

		list.add(node);
	}

	/**
	 * Dependency creating stuff
	 */

	public void addForkDependency(CallNode from, EntryNode to) {
		numCallDep++;

		addEdge(from, to, EdgeType.FORK);
	}

	public void addCallDependency(CallNode from, EntryNode to) {
		numCallDep++;
		addEdge(from, to, EdgeType.CL);
	}

	public void addDataDependency(AbstractPDGNode from, AbstractPDGNode to, final int ssaVar) {
		addDataDependency(from, to);
	}

	public void addDataDependency(AbstractPDGNode from, AbstractPDGNode to) {
		numDataDep++;

		addEdge(from, to, EdgeType.DD);
	}

	public void addHeapDataDependency(AbstractPDGNode from, AbstractPDGNode to) {
		numDataDep++;

		addEdge(from, to, EdgeType.DH);
	}

	public void addForkInDependency(AbstractParameterNode from, AbstractParameterNode to) {
		assert (from != null);
		assert (to != null && to.isIn());

		numParamInDep++;
		addEdge(from, to, EdgeType.FORK_IN);
	}

	public void addParamInDependency(AbstractParameterNode from, AbstractParameterNode to) {
		assert (from != null);
		assert (to != null && to.isIn());

		numParamInDep++;
		addEdge(from, to, EdgeType.PI);
	}

	public void addParamOutDependency(AbstractParameterNode from, AbstractParameterNode to) {
//		if (Assertions.verifyAssertions) {
//			Assertions._assert(getId() == from.getPdgId(), "From node is not part of the pdg.");
//		}
		numParamOutDep++;
		addEdge(from, to, EdgeType.PO);
	}

	public void addSummaryEdge(AbstractPDGNode from, AbstractPDGNode to) {
		numSumEdge++;
		addEdge(from, to, EdgeType.SU);
	}

	public void addParameterStructureDependency(AbstractParameterNode from, AbstractParameterNode to) {
		assert from.isActual() == to.isActual();
		addEdge(from, to, EdgeType.PS);
	}

	public void addParameterStructureDependency(EntryNode from, AbstractParameterNode to) {
		assert to.isFormal();
		addEdge(from, to, EdgeType.PS);
	}

	public void addParameterStructureDependency(CallNode from, AbstractParameterNode to) {
		assert to.isActual();
		addEdge(from, to, EdgeType.PS);
	}

	public void addUnconditionalControlDependency(AbstractPDGNode from,	AbstractPDGNode to) {
		numControlDep++;
		addEdge(from, to, EdgeType.UN);
	}

	public void addExceptionControlDependency(AbstractPDGNode from, AbstractParameterNode to) {
		//TODO add CD_EX to graph grammar - someday
		addEdge(from, to, EdgeType.UN);
//		addEdge(from, to, EdgeType.CD_EX);
	}

	public void addExceptionControlDependency(AbstractPDGNode from, ConstantPhiValueNode to) {
		addEdge(from, to, EdgeType.UN);
//		addEdge(from, to, EdgeType.CD_EX);
	}

	public void addExceptionControlDependency(AbstractPDGNode from, CatchNode to) {
		addEdge(from, to, EdgeType.UN);
//		addEdge(from, to, EdgeType.CD_EX);
	}

	public void addExpressionControlDependency(AbstractPDGNode from, AbstractPDGNode to) {
		numControlDep++;
		addEdge(from, to, EdgeType.CE);
	}

	public void addCallControlDependency(AbstractPDGNode from, AbstractPDGNode to) {
		numControlDep++;
		addEdge(from, to, EdgeType.CC);
	}

	public void addControlFlowDependency(AbstractPDGNode from, AbstractPDGNode to) {
		addEdge(from, to, EdgeType.CF);
	}

	public void addParameterChildDependency(AbstractPDGNode from, AbstractPDGNode to) {
//		addEdge(from, to, EdgeType.CD_TRUE);
		addEdge(from, to, EdgeType.CE);
	}

	public boolean hasControlDependency(AbstractPDGNode from, AbstractPDGNode to,
			boolean isTrue) {
		return hasEdge(from, to, (isTrue ? EdgeType.CD_TRUE : EdgeType.CD_FALSE));
	}

	public void addControlDependency(AbstractPDGNode from, AbstractPDGNode to,
			boolean isTrue) {
		numControlDep++;
		addEdge(from, to, (isTrue ? EdgeType.CD_TRUE : EdgeType.CD_FALSE));
	}

	public void addNonterminationSensitiveControlDependency(AbstractPDGNode from, AbstractPDGNode to) {
		addEdge(from, to, EdgeType.NTSCD);
	}

	public void addVirtualDependency(AbstractPDGNode from, AbstractPDGNode to) {
		addEdge(from, to, EdgeType.VD);
	}

	public void addUtilityEdge(AbstractPDGNode from, AbstractPDGNode to) {
		addEdge(from, to, EdgeType.HE);
	}

	public void addReadWriteInterference(AbstractPDGNode write, AbstractPDGNode read) {
		assert (write.getPdgId() == getId());
		assert (read instanceof EntryNode || read instanceof AbstractParameterNode
				|| (read instanceof ExpressionNode && ((ExpressionNode) read).isGet()))
			: "Read operation is no field get: " + read;
		assert (write instanceof EntryNode || write instanceof AbstractParameterNode
				|| (write instanceof ExpressionNode && ((ExpressionNode) write).isSet()))
			: "Write operation is no field set: " + write;

		addEdge(write, read, EdgeType.ID);
	}

	public void addWriteWriteInterference(AbstractPDGNode write1, AbstractPDGNode write2) {
		assert (write1.getPdgId() == getId());
		assert (write1 instanceof EntryNode || write1 instanceof AbstractParameterNode
				|| (write1 instanceof ExpressionNode && ((ExpressionNode) write1).isSet()))
			: "Write operation no. 1 is no field set: " + write1;
		assert (write2 instanceof EntryNode || write2 instanceof AbstractParameterNode
				|| (write2 instanceof ExpressionNode && ((ExpressionNode) write2).isSet()))
			: "Write operation no. 2 is no field set: " + write2;

		addEdge(write1, write2, EdgeType.IW);
	}

	/**
	 * Node creating stuff
	 */

	/**
	 * This is only used to model the call to method main
	 * @return a new CallNode
	 */
	public CallNode makeStaticRootCall(IMethod method, CGNode target) {
		CallNode call = new CallNode(getId(), target);

		addNode(call);

		call.setLabel(Util.methodName(method));

		return call;
	}

	public NormalNode makeCompound(SSAInvokeInstruction instr) {
		NormalNode node = new NormalNode(getId());
		node.setLabel("compound call");

		addToMap(node, instr);

		addNode(node);

		return node;
	}

	public CallNode makeCall(SSAInvokeInstruction instr, CGNode target) {
		CallNode node = null;

		assert (instr != null);
		assert (target != null);

		node = new CallNode(getId(), instr, target);

		node.setLabel(Util.methodName(target.getMethod()));

		addToMap(node, instr);

		addNode(node);

		return node;
	}

	public CallNode makeCallDummy(SSAInvokeInstruction instr) {
		CallNode node = null;

		assert (instr != null);

		node = new CallNode(getId(), instr);

		node.setLabel(Util.methodName(instr.getDeclaredTarget()) + "[dummy]");

		addToMap(node, instr);

		addNode(node);

		return node;
	}

	public EntryNode makeEntry(String methodSig) {
		EntryNode node = new EntryNode(getId());

		node.setLabel(methodSig);
		addNode(node);

		return node;
	}

	public ExpressionNode makeExpression(SSAInstruction instr) {
		ExpressionNode node = new ExpressionNode(getId());

		if (instr != null) {
			addToMap(node, instr);
		}

		addNode(node);

		return node;
	}

	private HeapAccessCompound makeBaseRef() {
		HeapAccessCompound base = new HeapAccessCompound(getId(), HeapAccessCompound.Type.BASE);

		addNode(base);

		return base;
	}

	private HeapAccessCompound makeFieldRef() {
		HeapAccessCompound base = new HeapAccessCompound(getId(), HeapAccessCompound.Type.FIELD);

		addNode(base);

		return base;
	}

	private HeapAccessCompound makeIndexRef() {
		HeapAccessCompound base = new HeapAccessCompound(getId(), HeapAccessCompound.Type.INDEX);

		addNode(base);

		return base;
	}

	private HeapAccessCompound makeValueRef() {
		HeapAccessCompound base = new HeapAccessCompound(getId(), HeapAccessCompound.Type.VALUE);

		addNode(base);

		return base;
	}

	public FieldGetArrayNode makeFieldGetArray(SSAArrayLoadInstruction instr, ParameterField field) {
		HeapAccessCompound base = makeBaseRef();
		HeapAccessCompound index = makeIndexRef();
		HeapAccessCompound fieldVal = makeFieldRef();
		FieldGetArrayNode node = new FieldGetArrayNode(getId(), field, base, index, fieldVal);

		if (instr != null) {
			addToMap(node, instr);
		}

		addNode(node);

		addUnconditionalControlDependency(node, base);
		addDataDependency(base, node);
		addUnconditionalControlDependency(node, index);
		addDataDependency(index, node);
		addUnconditionalControlDependency(node, fieldVal);
		addDataDependency(fieldVal, node);

		return node;
	}

	public FieldSetArrayNode makeFieldSetArray(SSAArrayStoreInstruction instr, ParameterField field) {
		HeapAccessCompound base = makeBaseRef();
		HeapAccessCompound index = makeIndexRef();
		HeapAccessCompound value = makeValueRef();
		FieldSetArrayNode node = new FieldSetArrayNode(getId(), field, base, index, value);

		if (instr != null) {
			addToMap(node, instr);
		}

		addNode(node);

		addUnconditionalControlDependency(node, base);
		addDataDependency(base, node);
		addUnconditionalControlDependency(node, index);
		addDataDependency(index, node);
		addUnconditionalControlDependency(node, value);
		addDataDependency(value, node);

		return node;
	}

	public FieldGetNode makeFieldGet(SSAGetInstruction instr, ParameterField field) {
		HeapAccessCompound base = makeBaseRef();
		HeapAccessCompound fieldVal = makeFieldRef();

		FieldGetNode node = new FieldGetNode(getId(), field, base, fieldVal);

		if (instr != null) {
			addToMap(node, instr);
		}

		addNode(node);

		addUnconditionalControlDependency(node, base);
		addDataDependency(base, node);
		addUnconditionalControlDependency(node, fieldVal);
		addDataDependency(fieldVal, node);

		return node;
	}

	public FieldGetStaticNode makeFieldGetStatic(SSAGetInstruction instr, ParameterField field) {
		HeapAccessCompound fieldVal = makeFieldRef();

		FieldGetStaticNode node = new FieldGetStaticNode(getId(), field, fieldVal);

		if (instr != null) {
			addToMap(node, instr);
		}

		addNode(node);

		addUnconditionalControlDependency(node, fieldVal);
		addDataDependency(fieldVal, node);

		return node;
	}

	public FieldSetNode makeFieldSet(SSAPutInstruction instr, ParameterField field) {
		HeapAccessCompound base = makeBaseRef();
		HeapAccessCompound value = makeValueRef();

		FieldSetNode node = new FieldSetNode(getId(), field, base, value);

		if (instr != null) {
			addToMap(node, instr);
		}

		addNode(node);

		addUnconditionalControlDependency(node, base);
		addDataDependency(base, node);
		addUnconditionalControlDependency(node, value);
		addDataDependency(value, node);

		return node;
	}

	public FieldSetStaticNode makeFieldSetStatic(SSAPutInstruction instr, ParameterField field) {
		HeapAccessCompound value = makeValueRef();

		FieldSetStaticNode node = new FieldSetStaticNode(getId(), field, value);

		if (instr != null) {
			addToMap(node, instr);
		}

		addNode(node);

		addUnconditionalControlDependency(node, value);
		addDataDependency(value, node);

		return node;
	}

	public PhiValueNode makePhiValue(SSAPhiInstruction instr) {
		int valueNum = instr.getDef();
		StringBuffer sb = new StringBuffer();
		sb.append("PHI v" + valueNum + " = ");
		int[] uses = new int[instr.getNumberOfUses()];
		for (int i = 0; i < uses.length; i++) {
			uses[i] = instr.getUse(i);
			sb.append("v" + uses[i]);
			if (i < uses.length - 1) {
				sb.append(", ");
			}
		}

		PhiValueNode node = new PhiValueNode(getId(), valueNum, uses);

		node.setLabel(sb.toString());

		if (instr != null) {
			addToMap(node, instr);
		}

		addNode(node);
		addUnconditionalControlDependency(getRoot(), node);

		return node;
	}

	public ConstantPhiValueNode makeConstantPhiValue(int valueNum, IR ir) {
		ConstantPhiValueNode node = new ConstantPhiValueNode(getId(), valueNum);

		ConstantValue val = node.getConstant(ir);
		String label = Util.sanitizeLabel(val.toString());
		node.setLabel("CONST " + label);

		addNode(node);
		addUnconditionalControlDependency(getRoot(), node);

		return node;
	}

	/*

	*/
	public NormalNode makeNormal(SSAInstruction instr) {
		NormalNode node = new NormalNode(getId());

		if (instr != null) {
			addToMap(node, instr);
		}

		addNode(node);

		return node;
	}

	public PredicateNode makePredicate(SSAInstruction instr) {
		PredicateNode node = new PredicateNode(getId());

		if (instr != null) {
			addToMap(node, instr);
		}

		addNode(node);

		return node;
	}

	private TIntObjectHashMap<CatchNode> bb2catch = new TIntObjectHashMap<CatchNode>();

	public CatchNode makeCatch(SSAInstruction instr, int basicBlock, int val) {
		CatchNode catchn = new CatchNode(getId(), basicBlock, val);

		if (instr != null) {
			addToMap(catchn, instr);
		}

		addNode(catchn);
		bb2catch.put(basicBlock, catchn);

		return catchn;
	}

	public CatchNode getCatchForBB(int basicBlockNr) {
		return bb2catch.get(basicBlockNr);
	}

	public boolean containsCatch() {
		return !bb2catch.isEmpty();
	}

	public SyncNode makeSync(SSAMonitorInstruction instr) {
		if (instr == null) {
			throw new IllegalArgumentException();
		}

		SyncNode node = new SyncNode(getId());

		addToMap(node, instr);

		addNode(node);

		return node;
	}

}
