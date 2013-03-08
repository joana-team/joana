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
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

import edu.kit.joana.ifc.sdg.graph.SDG;
import edu.kit.joana.ifc.sdg.graph.SDGEdge;
import edu.kit.joana.ifc.sdg.graph.SDGNode;
import edu.kit.joana.ifc.sdg.graph.SDGNode.Kind;
import edu.kit.joana.ifc.sdg.util.BytecodeLocation;
import edu.kit.joana.ifc.sdg.util.JavaMethodSignature;
import edu.kit.joana.ifc.sdg.util.JavaType;
import edu.kit.joana.ifc.sdg.util.JavaType.Format;

public class SDGMethod extends SDGProgramPart {

	private final SDG sdg;
	private SDGNode entry;
	private SDGMethodExitNode exit;
	private JavaMethodSignature sig;
	private SortedMap<Integer, SDGParameter> params = new TreeMap<Integer, SDGParameter>();
	private List<SDGInstruction> instructions = new ArrayList<SDGInstruction>();
	private List<SDGPhi> phis = new ArrayList<SDGPhi>();
	private boolean initialized = false;

	public SDGMethod(SDG sdg, SDGNode entry) {
		this.sdg = sdg;
		this.entry = entry;
		this.sig = JavaMethodSignature.fromString(entry.getBytecodeName());
		initialize(sdg);
	}

	private void setExit(SDG sdg) {
		for (SDGNode node : sdg.getFormalOutsOfProcedure(entry)) {
			if (node.getKind() == SDGNode.Kind.EXIT)
				this.exit = new SDGMethodExitNode(node, this);
		}
	}
	
	SDG getSDG() {
		return sdg;
	}

	public void initialize(SDG sdg) {
		if (!initialized) {
			setExit(sdg);
			int instructionIndex = 0;
			for (SDGNode n : sdg.getNodesOfProcedure(entry)) {
				if (n.getBytecodeIndex() >= 0) {
					Set<SDGNode> attSourceNodes = new HashSet<SDGNode>();
					Set<SDGNode> attSinkNodes = new HashSet<SDGNode>();
					if (n.getKind() == SDGNode.Kind.CALL) {
						for (SDGEdge e : sdg.getOutgoingEdgesOfKind(n,
								SDGEdge.Kind.CONTROL_DEP_EXPR)) {
							SDGNode node = e.getTarget();
							if (node.getKind() == Kind.ACTUAL_IN) {
								attSinkNodes.add(node);
							} else if (node.getKind() == Kind.ACTUAL_OUT) {
								attSourceNodes.add(node);
							}
						}
					}
					instructions.add(new SDGInstruction(this, n, attSourceNodes, attSinkNodes,
							instructionIndex));
					instructionIndex++;
				} else if (BytecodeLocation.isNormalFormalParameter(n)) {
					int paramIndex = BytecodeLocation.getRootParamIndex(n
							.getBytecodeName());
					if (paramIndex >= 0) {
						SDGParameter p;
						if (!params.containsKey(paramIndex)) {
							p = new SDGParameter(this, paramIndex);
							params.put(paramIndex, p);
						} else {
							p = params.get(paramIndex);
						}

						switch (n.getKind()) {
						case FORMAL_IN:
							p.setInRoot(n);
							break;
						case FORMAL_OUT:
							p.setOutRoot(n);
							break;
						default:
							break;
						}
						p.setType(JavaType.parseSingleTypeFromString(n.getType(), Format.BC));
					} else {
						throw new IllegalStateException();
					}
				} else if (BytecodeLocation.isPhiNode(n)) {
					phis.add(new SDGPhi(this, n));
				}
			}
			Collections.sort(instructions);
			initialized = true;
		}
	}

	public SDGNode getEntry() {
		return entry;
	}

	public SDGParameter getParameter(int i) {
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
	
	public List<SDGInstruction> getAllCalls(JavaMethodSignature target) {
		List<SDGInstruction> ret = new LinkedList<SDGInstruction>();
		
		for (SDGInstruction i : getInstructions()) {
			if (i.possiblyCalls(target)) {
				ret.add(i);
			}
		}
		
		return ret;
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

	public int getNumberOfInstructions() {
		return instructions.size();
	}

	public SDGInstruction getInstructionWithBCIndex(int index) {
		for (SDGInstruction i : instructions)
			if (i.getBytecodeIndex() == index)
				return i;
		return null;
	}

	public SDGMethodExitNode getExit() {
		return exit;
	}

	public Collection<SDGParameter> getParameters() {
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
	public String toString() {
		return sig.toBCString();
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((entry == null) ? 0 : entry.hashCode());
		result = prime * result + ((exit == null) ? 0 : exit.hashCode());
		result = prime * result + (initialized ? 1231 : 1237);
		result = prime * result
		+ ((instructions == null) ? 0 : instructions.hashCode());
		result = prime * result + ((params == null) ? 0 : params.hashCode());
		result = prime * result + ((sig == null) ? 0 : sig.hashCode());
		return result;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
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
		if (entry == null) {
			if (other.entry != null) {
				return false;
			}
		} else if (!entry.equals(other.entry)) {
			return false;
		}
		if (exit == null) {
			if (other.exit != null) {
				return false;
			}
		} else if (!exit.equals(other.exit)) {
			return false;
		}
		if (initialized != other.initialized) {
			return false;
		}
		if (instructions == null) {
			if (other.instructions != null) {
				return false;
			}
		} else if (!instructions.equals(other.instructions)) {
			return false;
		}
		if (params == null) {
			if (other.params != null) {
				return false;
			}
		} else if (!params.equals(other.params)) {
			return false;
		}
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
	public <R, D> R acceptVisitor(SDGProgramPartVisitor<R, D> v, D data) {
		return v.visitMethod(this, data);
	}

	@Override
	public SDGMethod getOwningMethod() {
		return this;
	}

	@Override
	public boolean covers(SDGNode node) {
		if (node.equals(entry)) {
			return true;
		}

		for (SDGParameter p : getParameters()) {
			if (p.covers(node)) {
				return true;
			}
		}

		if (getExit().covers(node))
			return true;

		for (SDGInstruction i : getInstructions()) {
			if (i.covers(node)) {
				return true;
			}
		}

		for (SDGPhi phi : getPhis()) {
			if (phi.covers(node))
				return true;
		}

		return false;
	}

	@Override
	public Collection<SDGNode> getAttachedNodes() {
		Set<SDGNode> ret = new HashSet<SDGNode>();

		ret.add(entry);

		for (SDGParameter p : getParameters()) {
			ret.addAll(p.getAttachedNodes());
		}

		ret.addAll(getExit().getAttachedNodes());

		for (SDGInstruction i : getInstructions()) {
			ret.addAll(i.getAttachedNodes());
		}

		for (SDGPhi phi : getPhis()) {
			ret.addAll(phi.getAttachedNodes());
		}

		return ret;
	}

	@Override
	public Collection<SDGNode> getAttachedSourceNodes() {
		Set<SDGNode> ret = new HashSet<SDGNode>();

		ret.add(entry);

		for (SDGParameter p : getParameters()) {
			ret.addAll(p.getAttachedSourceNodes());
		}

		ret.addAll(getExit().getAttachedSourceNodes());

		for (SDGInstruction i : getInstructions()) {
			ret.addAll(i.getAttachedSourceNodes());
		}

		for (SDGPhi phi : getPhis()) {
			ret.addAll(phi.getAttachedSourceNodes());
		}

		return ret;
	}

	@Override
	public Collection<SDGNode> getAttachedSinkNodes() {
		Set<SDGNode> ret = new HashSet<SDGNode>();

		ret.add(entry);

		for (SDGParameter p : getParameters()) {
			ret.addAll(p.getAttachedSinkNodes());
		}

		ret.addAll(getExit().getAttachedSinkNodes());

		for (SDGInstruction i : getInstructions()) {
			ret.addAll(i.getAttachedSinkNodes());
		}

		for (SDGPhi phi : getPhis()) {
			ret.addAll(phi.getAttachedSinkNodes());
		}

		return ret;
	}

	@Override
	public SDGProgramPart getCoveringComponent(SDGNode node) {
		if (entry.equals(node)) {
			return this;
		} else if (exit.covers(node)) {
			return exit;
		} else {
			for (SDGParameter p : getParameters()) {
				if (p.covers(node)) {
					return p;
				}
			}

			for (SDGPhi phi : getPhis()) {
				if (phi.covers(node)) {
					return phi;
				}
			}

			for (SDGInstruction i : getInstructions()) {
				if (i.covers(node)) {
					return i;
				}
			}

			return null;
		}
	}

}
