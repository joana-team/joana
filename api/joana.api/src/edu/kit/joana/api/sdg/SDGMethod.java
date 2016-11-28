/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.api.sdg;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import edu.kit.joana.ifc.sdg.util.BytecodeLocation;
import edu.kit.joana.ifc.sdg.util.JavaMethodSignature;
import edu.kit.joana.ifc.sdg.util.JavaType;

public class SDGMethod implements SDGProgramPart {

	private SDGMethodExitNode exit;
	private SDGMethodExceptionNode exception;
	private JavaMethodSignature sig;
	private SortedMap<Integer, SDGFormalParameter> params = new TreeMap<Integer, SDGFormalParameter>();
	private List<SDGInstruction> instructions = new ArrayList<SDGInstruction>();
	private List<SDGCall> calls = new ArrayList<SDGCall>();
	private List<SDGPhi> phis = new ArrayList<SDGPhi>();
	private Map<String, SDGLocalVariable> localVariables = new HashMap<>();
	private final String classLoader;
	public SDGMethod(JavaMethodSignature sig, String classLoader, boolean isStatic) {

		this.sig = sig;
		if (!isStatic) {
			this.params.put(0, new SDGFormalParameter(this, 0, "this", sig.getDeclaringType()));
		}
		int paramIndex = 1;
		for (JavaType argType : this.sig.getArgumentTypes()) {
			this.params.put(paramIndex, new SDGFormalParameter(this, paramIndex, BytecodeLocation.getRootParamName(paramIndex), argType));
			paramIndex++;
		}
		this.exit = new SDGMethodExitNode(this, this.sig.getReturnType());
		this.exception = new SDGMethodExceptionNode(this);
		this.instructions = new ArrayList<SDGInstruction>();
		this.calls = new ArrayList<SDGCall>();
		this.phis = new ArrayList<SDGPhi>();
		this.classLoader = classLoader;
	}
	
	public String getClassLoader() {
		return classLoader;
	}
	
	void addLocalVariable(SDGLocalVariable var) {
		if (this.localVariables.containsKey(var.getName())) {
			throw new IllegalStateException("Local variable " + var.getName() + " already present.");
		}
		this.localVariables.put(var.getName(), var);
	}
	void addInstruction(SDGInstruction i) {
		this.instructions.add(i);
	}

	void addPhi(SDGPhi phi) {
		this.phis.add(phi);
	}

	public void addCall(SDGCall newCall) {
		this.calls.add(newCall);
		addInstruction(newCall);
	}

	public SDGFormalParameter getParameter(int i) {
		if (!params.containsKey(i)) {
			return null;
		} else {
			return params.get(i);
		}
	}

	public SDGInstruction getInstruction(int i) {
		if (i < 0 || i >= instructions.size()) {
			return null;
		} else {
			return instructions.get(i);
		}
	}

	public List<SDGCall> getAllCalls(JavaMethodSignature target) {
		List<SDGCall> ret = new LinkedList<SDGCall>();

		for (SDGCall call : calls) {
			if (call.possiblyCalls(target)) {
				ret.add(call);
			}
		}

		return ret;
	}

	public List<SDGCall> getAllCalls() {
		return Collections.unmodifiableList(calls);
	}

	public int getInstructionIndex(SDGInstruction instr) {
		for (int i = 0; i < instructions.size(); i++) {
			if (instr.equals(instructions.get(i))) {
				return i;
			}
		}

		return -1;
	}

	public List<SDGInstruction> getInstructions() {
		return instructions;
	}

	public List<SDGPhi> getPhis() {
		return phis;
	}
	
	public Collection<SDGLocalVariable> getLocalVariables() {
		return localVariables.values();
	}
	
	public SDGLocalVariable getLocalVariable(String name) {
		return localVariables.get(name);
	}

	public int getNumberOfInstructions() {
		return instructions.size();
	}

	public SDGInstruction getInstructionWithBCIndex(int index) {
		for (SDGInstruction i : instructions)
			if (i.getBytecodeIndex() == index)
				return i;
		return null;
	}

	public Collection<SDGInstruction> getInstructionsWithLabelMatching(String labelRegEx) {
		List<SDGInstruction> ret = new LinkedList<SDGInstruction>();
		for (SDGInstruction i : instructions)
			if (i.getLabel().matches(labelRegEx))
				ret.add(i);
		return ret;
	}

	public SDGMethodExitNode getExit() {
		return exit;
	}

	public SDGMethodExceptionNode getException() {
		return exception;
	}
	
	public Collection<SDGFormalParameter> getParameters() {
		return params.values();
	}

	public JavaMethodSignature getSignature() {
		return sig;
	}

	public boolean parameterIndexValid(int i) {
		return params.containsKey(i);
	}

	public boolean instructionIndexValid(int i) {
		return (i >= 0 && i < instructions.size());
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((sig == null) ? 0 : sig.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (!(obj instanceof SDGMethod)) {
			return false;
		}
		SDGMethod other = (SDGMethod) obj;
		if (sig == null) {
			if (other.sig != null) {
				return false;
			}
		} else if (!sig.equals(other.sig)) {
			return false;
		}
		return true;
	}

	@Override
	public String toString() {
		return sig.toBCString();
	}

	@Override
	public <R, D> R acceptVisitor(SDGProgramPartVisitor<R, D> v, D data) {
		return v.visitMethod(this, data);
	}

	@Override
	public SDGMethod getOwningMethod() {
		return this;
	}
}
