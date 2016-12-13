/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.deprecated.jsdg.sdg;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ssa.IR;
import com.ibm.wala.ssa.SSAArrayLengthInstruction;
import com.ibm.wala.ssa.SSAArrayLoadInstruction;
import com.ibm.wala.ssa.SSAArrayStoreInstruction;
import com.ibm.wala.ssa.SSAGetInstruction;
import com.ibm.wala.ssa.SSAInstruction;
import com.ibm.wala.ssa.SSAInvokeInstruction;
import com.ibm.wala.ssa.SSAPutInstruction;
import com.ibm.wala.ssa.SSAReturnInstruction;
import com.ibm.wala.ssa.SSAThrowInstruction;
import com.ibm.wala.util.collections.HashSetFactory;
import com.ibm.wala.util.intset.BitVectorIntSet;
import com.ibm.wala.util.intset.IntSet;
import com.ibm.wala.util.intset.MutableIntSet;

import edu.kit.joana.deprecated.jsdg.sdg.nodes.AbstractPDGNode;
import edu.kit.joana.deprecated.jsdg.sdg.nodes.AbstractParameterNode;
import edu.kit.joana.deprecated.jsdg.sdg.nodes.CallNode;
import edu.kit.joana.deprecated.jsdg.sdg.nodes.ConstantPhiValueNode;
import edu.kit.joana.deprecated.jsdg.sdg.nodes.JDependencyGraph;
import edu.kit.joana.deprecated.jsdg.sdg.nodes.NormalNode;
import edu.kit.joana.deprecated.jsdg.sdg.parammodel.IParamModel;
import edu.kit.joana.deprecated.jsdg.util.Log;
import edu.kit.joana.util.maps.MultiHashMap;
import edu.kit.joana.util.maps.MultiMap;
import gnu.trove.map.hash.TIntObjectHashMap;

/**
 * This class is the simple data container part of the PDG.
 *
 * Extending the JDependencyGraph to hold basic Wala IR related information
 * for the PDG creation. E.g. set of get and set instructions, references of
 * phi variables, call instructions, set of form-in/out nodes etc.
 *
 * @author Juergen Graf <graf@kit.edu>
 *
 */
public abstract class AbstractPDG extends JDependencyGraph {

	public static final int VALUE_NUMBER_OF_THIS_POINTER = 1;

	/**
	 * Contains the id of all threads that may be calling this pdg
	 */
	private final MutableIntSet threadIds;

	/**
	 * Maps an ssa variable to the pdg node where its value has been defined.
	 * Ssa vars in wala are numbers from 0-i.
	 */
	private final TIntObjectHashMap<Set<AbstractPDGNode>> definesVar;

	/**
	 * kind of reverse map for forwardRefs
	 * dependency of ssa var i cannot be resolved in nodes
	 * unresolved ssa var -> list of using nodes
	 */
	private final TIntObjectHashMap<List<AbstractPDGNode>> unresolvedDDs;

//	/**
//	 * stores the forward references of phi nodes. The key is the variable name
//	 * holding the phi value and the int array stores the ssa var it may point
//	 * to. The referenced ssa vars may be phi values themselves.
//	 */
//	private final TIntObjectHashMap<int[]> phiReferences;
//
//	/**
//	 * Lazy cache for resolved forward refs. These forward refs do not
//	 * contain any phi references.
//	 */
//	private final TIntObjectHashMap<IntSet> resolvedForwardRefs;

	/**
	 * get instructions
	 */
	private final Set<SSAGetInstruction> gets;
	private final Set<SSAArrayLoadInstruction> agets;
	private final Set<SSAArrayLengthInstruction> alengths;

	/**
	 * set instructions
	 */
	private final Set<SSAPutInstruction> sets;
	private final Set<SSAArrayStoreInstruction> asets;

	/**
	 * Maps the ssa variable the return value is stored to the return
	 * statement
	 */
	private final TIntObjectHashMap<SSAReturnInstruction> tmp2return;
	private final TIntObjectHashMap<SSAThrowInstruction> tmp2throw;

	private final MultiMap<CGNode, CallNode> method2call;
	private final Set<CallNode> calls;

	private final Set<NormalNode> returns;
	private final Set<NormalNode> throwz;

	private final int id;

	public AbstractPDG(int id) {
		this.id = id;
		this.definesVar = new TIntObjectHashMap<Set<AbstractPDGNode>>();
		this.unresolvedDDs = new TIntObjectHashMap<List<AbstractPDGNode>>();
//		this.phiReferences = new TIntObjectHashMap<int[]>();
//		this.resolvedForwardRefs = new TIntObjectHashMap<IntSet>();
		this.gets = HashSetFactory.make();
		this.agets = HashSetFactory.make();
		this.sets = HashSetFactory.make();
		this.asets = HashSetFactory.make();
		this.alengths = HashSetFactory.make();

		this.tmp2return = new TIntObjectHashMap<SSAReturnInstruction>();
		this.tmp2throw = new TIntObjectHashMap<SSAThrowInstruction>();
		this.method2call = new MultiHashMap<CGNode, CallNode>();
		this.calls = HashSetFactory.make();
		this.returns = HashSetFactory.make();
		this.throwz = HashSetFactory.make();
		this.threadIds = new BitVectorIntSet();
	}

	public final void addThreadId(int id) {
		threadIds.add(id);
	}

	public final IntSet getThreadIds() {
		return threadIds;
	}

	public Set<AbstractPDGNode> getDefinesVar(int ssaVar) {
		return definesVar.get(ssaVar);
	}

	protected void addThrowTmp(SSAThrowInstruction instr, int tmp) {
		tmp2throw.put(tmp, instr);
	}

	public int[] getAllThrowTmps() {
		return tmp2throw.keys();
	}

	public Set<NormalNode> getThrows() {
		return throwz;
	}

	protected void addThrow(NormalNode node) {
		throwz.add(node);
	}

	protected void addCall(CallNode call) {
		method2call.add(call.getTarget(), call);
		calls.add(call);
	}

	public Set<CallNode> getCallsTo(PDG pdg) {
		return method2call.get(pdg.getCallGraphNode());
	}

	public Set<CallNode> getCallsForInstruction(SSAInvokeInstruction instr) {
		Set<CallNode> callsForInstr = HashSetFactory.make();
		for (CallNode call : calls) {
			if (call.getInstruction() == instr) {
				callsForInstr.add(call);
			}
		}

		return callsForInstr;
	}

	public Set<CallNode> getAllCalls() {
		return calls;
	}

	public CallNode getCall(AbstractParameterNode param) {
		if (param.getPdgId() != this.id) {
			throw new IllegalArgumentException("Does not belong to this PDG");
		} else if (!param.isActual()) {
			throw new IllegalArgumentException("Is no actual node.");
		}

		final IParamModel model = getParamModel();

		for (CallNode call : calls) {
			if (param.isIn() && model.getRefParams(call).contains(param)) {
				return call;
			} else if (param.isOut() && model.getModParams(call).contains(param)) {
				return call;
			}
		}

		System.err.println("No call node for " + param + " in " + this);

		return null;
	}

	public Set<NormalNode> getReturns() {
		return returns;
	}

	protected void addReturn(NormalNode ret) {
		returns.add(ret);
	}

	protected void addReturnTmp(SSAReturnInstruction ret, int tmp) {
		tmp2return.put(tmp, ret);
	}

	/**
	 * Searches the node that defines the specified ssa variable and adds a data
	 * dependency to the specified pdg node.
	 * @param node The pdg node that uses the ssa variable
	 * @param ssaVar The value number of the ssa variable
	 */
	public void addDataFlow(AbstractPDGNode node, int ssaVar) {
		if (ssaVar < 0) {
			Log.warn("Couldn't resolve ssa var no. " + ssaVar + " for " + node);
			return;
		}

		Set<AbstractPDGNode> defs = definesVar.get(ssaVar);

		if (defs != null) {
			// not a forward reference
			for (AbstractPDGNode def : defs) {
				if (def != node) {
					addDataDependency(def, node, ssaVar);
				}
			}
		} else if (getIR().getSymbolTable().isConstant(ssaVar)) {
			// when we find a reference to a constant value we create a node for it
			ConstantPhiValueNode cphi = makeConstantPhiValue(ssaVar, getIR());
			addDefinesVar(cphi, ssaVar);
			addDataDependency(cphi, node);
		} else {
			// references are saved for later resolution
			// a forward reference -> store info and process later
			// this is normally not possible in ssa form, but
			// it could be defined on a phi statement
			List<AbstractPDGNode> fNodes = unresolvedDDs.get(ssaVar);

			if (fNodes == null) {
				fNodes = new ArrayList<AbstractPDGNode>();
				unresolvedDDs.put(ssaVar, fNodes);
			}

			fNodes.add(node);
		}
	}

	protected void addDataFlow(AbstractPDGNode node, SSAInstruction instr) {
		for (int i = 0; i < instr.getNumberOfUses(); i++) {
			int use = instr.getUse(i);
			addDataFlow(node, use);
		}
	}

	public void addDefinesVar(AbstractPDGNode node, Integer ssaVar) {
		Set<AbstractPDGNode> defs = definesVar.get(ssaVar);
		if (defs == null) {
			defs = HashSetFactory.make();
			definesVar.put(ssaVar, defs);
		}
		defs.add(node);
	}

	protected void addGet(SSAArrayLoadInstruction instr) {
		agets.add(instr);
	}

	public Set<SSAArrayLoadInstruction> getArrayGets() {
		return agets;
	}

	protected void addArrayLength(SSAArrayLengthInstruction instr) {
		alengths.add(instr);
	}

	public Set<SSAArrayLengthInstruction> getArrayLengths() {
		return alengths;
	}

	protected void addGet(SSAGetInstruction instr) {
		gets.add(instr);
	}

	public Set<SSAGetInstruction> getGets() {
		return gets;
	}

	protected void addSet(SSAArrayStoreInstruction instr) {
		asets.add(instr);
	}

	public Set<SSAArrayStoreInstruction> getArraySets() {
		return asets;
	}

	protected void addSet(SSAPutInstruction instr) {
		sets.add(instr);
	}

	public Set<SSAPutInstruction> getSets() {
		return sets;
	}

	/**
	 * Gets all unresolved data dependencies to a ssa variable (value number)
	 * @param valueNr value number of the ssa variable
	 * @return Set of nodes of statements referenceing the ssa variable (valueNr)
	 */
	public List<AbstractPDGNode> getUnresolvedDDs(int valueNr) {
		return unresolvedDDs.get(valueNr);
	}

	public int[] getUnresolvedSSAVars() {
		return unresolvedDDs.keys();
	}

	public int getId() {
		return id;
	}

	public abstract IR getIR();

	public abstract IParamModel getParamModel();
}
