/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.deprecated.jsdg.sdg.dataflow;

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.ibm.wala.cfg.ControlFlowGraph;
import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.ssa.SSAInstruction;
import com.ibm.wala.ssa.SSAInvokeInstruction;
import com.ibm.wala.ssa.analysis.IExplodedBasicBlock;
import com.ibm.wala.util.collections.HashMapFactory;
import com.ibm.wala.util.collections.HashSetFactory;
import com.ibm.wala.util.graph.Graph;
import com.ibm.wala.util.graph.impl.SlowSparseNumberedGraph;

import edu.kit.joana.deprecated.jsdg.sdg.IntermediatePDG;
import edu.kit.joana.deprecated.jsdg.sdg.nodes.AbstractPDGNode;
import edu.kit.joana.deprecated.jsdg.sdg.nodes.AbstractParameterNode;
import edu.kit.joana.deprecated.jsdg.sdg.nodes.CallNode;
import edu.kit.joana.deprecated.jsdg.sdg.nodes.ExpressionNode;
import edu.kit.joana.deprecated.jsdg.sdg.nodes.HeapAccessCompound;
import edu.kit.joana.deprecated.jsdg.sdg.parammodel.IParamSet;
import edu.kit.joana.deprecated.jsdg.util.Log;
import edu.kit.joana.deprecated.jsdg.util.Util;

/**
 * Takes an ExplodedControlFlowGraph from wala and a pdg as input and adds the
 * parameter nodes of the pdg to the matching positions in the cfg.
 *
 *  In short a new cfg is created that includes parameter nodes.
 *
 *  Formal-in nodes are inserted right after the cfg entry.
 *  Formal-out nodes are inserted right before the cfg exit
 *  Actual-in nodes are inserted right before the matching method call
 *  Actual-out nodes are inserted right after the matching method call
 *
 *  If the option nodesInRow is not set:
 *
 *  All parameter nodes are directly connected to the basic block they belong to.
 *  No parameter node chains are built: e.g. {a,b,c} belong to call m then the
 *  new cfg looks like this:
 *
 *    m
 *   /|\        and not    m - a - b - c
 *  a b c
 *
 *  Else if the option nodesInRow is set:
 *
 *  All parameter nodes are connected in a row before or after the basic block
 *  that has been selected.
 *
 *  Not all parameter nodes of the pdg are inserted into this pdg. Only those
 *  who belong to this pdg and who are involved in heapbased dataflow (represent
 *  a value stored on the heap). These are all parameter nodes except non-static
 *  root nodes which represent a stack value (mostly a method parameter reference).
 *
 * @author Juergen Graf <graf@kit.edu>
 *
 */
public class CFGWithParameterNodes implements Graph<CFGWithParameterNodes.CFGNode> {

	private final ControlFlowGraph<SSAInstruction, IExplodedBasicBlock> ecfg;
	private final SlowSparseNumberedGraph<CFGNode> cfg;
	private final IntermediatePDG pdg;
	private final Map<CFGNode, IExplodedBasicBlock> node2bb;
//	private final HashMap<AbstractParameterNode, CFGNode> param2node;
	private final HashMap<AbstractPDGNode, CFGNode> sdg2cfg;

	private final Map<IExplodedBasicBlock, Set<CFGNode>> firstNodes;
	private final Map<IExplodedBasicBlock, Set<CFGNode>> lastNodes;
	private CFGNode entry;
	private CFGNode exit;

	private int numberOfParameterNodes;

	public CFGWithParameterNodes(ControlFlowGraph<SSAInstruction, IExplodedBasicBlock> ecfg, IntermediatePDG pdg) {
		this.ecfg = ecfg;
		this.pdg = pdg;
		this.cfg = SlowSparseNumberedGraph.make();
		this.node2bb = HashMapFactory.make();
//		this.param2node = HashMapFactory.make();
		this.sdg2cfg = HashMapFactory.make();
		this.firstNodes = HashMapFactory.make();
		this.lastNodes = HashMapFactory.make();
		createCFG();
	}

	public CFGNode getNodeForParameter(AbstractParameterNode param) {
		return sdg2cfg.get(param);
	}

	public CFGNode getNodeForHeapAccess(HeapAccessCompound param) {
		return sdg2cfg.get(param);
	}

	public CFGNode getNodeForExpression(ExpressionNode expr) {
		return sdg2cfg.get(expr);
	}

	public AbstractPDGNode getMainPDGNode(IExplodedBasicBlock bb) {
		AbstractPDGNode pdgnode = null;

		SSAInstruction instr = bb.getInstruction();
		if (instr == null) {
			return null;
		}

		List<AbstractPDGNode> nodes = pdg.getNodesForInstruction(instr);
		if (nodes == null || nodes.isEmpty()) {
			return null;
		}

		// the first element of the node list is per convention always the
		// main node concerning the instruction
		pdgnode = nodes.get(0);

		return pdgnode;
	}

	/**
	 * Is there a path between src and dst using only non-exception controlflow
	 * @param src
	 * @param dst
	 * @return
	 */
	public boolean isInNormalSuccessors(CFGNode src, CFGNode dst) {
		if (pdg.isIgnoreExceptions()) {
			return true;
		} else {
			IExplodedBasicBlock bbSrc = node2bb.get(src);
			IExplodedBasicBlock bbDst = node2bb.get(dst);
			if (bbDst == bbSrc) {
				return true;
			} else {
				Collection<IExplodedBasicBlock> bbNormSucc = ecfg.getNormalSuccessors(bbSrc);
				return bbNormSucc.contains(bbDst);
			}
		}
	}

	private Set<CFGNode> firstNodesOfBB(IExplodedBasicBlock bb) {
		Set<CFGNode> first = firstNodes.get(bb);
		if (first == null) {
			createFirstAndLastNodes(bb);
			first = firstNodes.get(bb);
		}

		return first;
	}

	private Set<CFGNode> lastNodesOfBB(IExplodedBasicBlock bb) {
		Set<CFGNode> last = lastNodes.get(bb);
		if (last == null) {
			createFirstAndLastNodes(bb);
			last = lastNodes.get(bb);
		}

		return last;
	}

	private void createFirstAndLastNodes(IExplodedBasicBlock block) {
		final SSAInstruction instr = block.getInstruction();

		if (block == ecfg.entry()) {
			assert (entry == null);

			CFGNode cfgNode = createNode(block);

			Set<CFGNode> first = HashSetFactory.make(1);
			first.add(cfgNode);
			firstNodes.put(block, first);

			entry = cfgNode;

			Set<? extends AbstractParameterNode> after = getAllHeapLocatedFormIns();

			CFGNode current = cfgNode;
			for (AbstractParameterNode p : after) {
				CFGNode param = findOrCreateNode(block, p);
				cfg.addEdge(current, param);
				current = param;
				numberOfParameterNodes++;
			}

			if (instr != null) {
				AbstractPDGNode node = pdg.getMainNodeForInstruction(instr);
				if (node != null) {
					CFGNode artificialNode = createArtificialNode(block, node);
					cfg.addEdge(current, artificialNode);
					current = artificialNode;
				}
			}

			Set<CFGNode> last = HashSetFactory.make(1);
			last.add(current);
			lastNodes.put(block, last);
		} else if (block == ecfg.exit()) {
			assert (exit == null);

			CFGNode cfgNode = createNode(block);

			Set<AbstractParameterNode> before = getAllHeapLocatedFormOuts();
			if (!pdg.isIgnoreExceptions() && pdg.getExceptionalExit() != null) {
				before.add(pdg.getExceptionalExit());
			}

			if (before.isEmpty()) {
				Set<CFGNode> first = HashSetFactory.make(1);
				first.add(cfgNode);
				firstNodes.put(block, first);
			} else {
				CFGNode current = null;
				for (AbstractParameterNode p : before) {
					CFGNode param = findOrCreateNode(block, p);
					if (current == null) {
						Set<CFGNode> first = HashSetFactory.make(1);
						first.add(param);
						firstNodes.put(block, first);
					} else {
						cfg.addEdge(current, param);
					}
					current = param;
					numberOfParameterNodes++;
				}

				cfg.addEdge(current, cfgNode);
			}

			exit = cfgNode;

			Set<CFGNode> last = HashSetFactory.make(1);
			last.add(cfgNode);
			lastNodes.put(block, last);
		} else if (block.isCatchBlock() && instr != null) {
			// special case where a single cfg node corresponds to 2 statements...

			CFGNode cfgNode = createNode(block);
			Set<CFGNode> first = HashSetFactory.make(1);
			first.add(cfgNode);
			firstNodes.put(block, first);
			AbstractPDGNode pdgNode = pdg.getMainNodeForInstruction(instr);
			CFGNode lastNode = createArtificialNode(block, pdgNode);
			Set<CFGNode> last = HashSetFactory.make(1);
			last.add(lastNode);
			lastNodes.put(block, last);
			cfg.addEdge(cfgNode, lastNode);

			List<AbstractPDGNode> nodes = pdg.getNodesForInstruction(instr);
			for (AbstractPDGNode node : nodes) {
				if (node instanceof ExpressionNode) {
					sdg2cfg.put((ExpressionNode) node, lastNode);
				}
			}
		} else if (instr instanceof SSAInvokeInstruction) {
			// method call found - add actual in nodes before and actual-out
			// nodes after the call
			Set<CFGNode> first = HashSetFactory.make();
			Set<CFGNode> last = HashSetFactory.make();

			Set<CallNode> calls = getCallsForInstruction((SSAInvokeInstruction) instr);

			CFGNode compoundCallNode = null;
			if (calls.size() > 1) {
				compoundCallNode = createNode(block);
				first.add(compoundCallNode);
			}

			for (CallNode call : calls) {
				CFGNode cfgNode = createNode(block, call);

				Set<? extends AbstractParameterNode> actIns = getAllHeapLocatedActualIns(call);
				numberOfParameterNodes += actIns.size();

				if (actIns.size() == 0) {
					if (compoundCallNode == null) {
						first.add(cfgNode);
					} else {
						cfg.addEdge(compoundCallNode, cfgNode);
					}
				} else {
					CFGNode current = null;
					for (AbstractParameterNode p : actIns) {
						CFGNode param = findOrCreateNode(block, p);
						if (current == null) {
							if (compoundCallNode == null) {
								first.add(param);
							} else {
								cfg.addEdge(compoundCallNode, param);
							}
						} else {
							cfg.addEdge(current, param);
						}

						current = param;
					}

					cfg.addEdge(current, cfgNode);
				}


				Set<? extends AbstractParameterNode> actOuts = getAllHeapLocatedActualOuts(call);
				numberOfParameterNodes += actOuts.size();

				CFGNode current = cfgNode;
				for (AbstractParameterNode p : actOuts) {
					CFGNode param = findOrCreateNode(block, p);
					cfg.addEdge(current, param);
					current = param;
				}
				last.add(current);
			}

			firstNodes.put(block, first);
			lastNodes.put(block, last);
		} else {
			CFGNode cfgNode = createNode(block);

			AbstractPDGNode node = null;
			if (instr != null) {
				node = pdg.getMainNodeForInstruction(instr);
			}

			if (node instanceof ExpressionNode) {
				ExpressionNode expr = (ExpressionNode) node;

				sdg2cfg.put((ExpressionNode) node, cfgNode);

				if (expr.isFieldAccess() || expr.isArrayAccess()) {
					Set<CFGNode> first = HashSetFactory.make(1);
					Set<CFGNode> last = HashSetFactory.make(1);
					last.add(cfgNode);

					CFGNode pred = null;

					if (!expr.isStaticFieldAccess()) {
						CFGNode baseCfg = createArtificialNode(block, expr.getBaseValue());
						first.add(baseCfg);
						pred = baseCfg;
					}

					if (expr.isArrayAccess()) {
						CFGNode indexCfg = createArtificialNode(block, expr.getIndexValue());
						if (pred != null) {
							cfg.addEdge(pred, indexCfg);
						} else {
							first.add(indexCfg);
						}
						pred = indexCfg;
					}

					if (expr.isGet()) {
						CFGNode fieldCfg = createArtificialNode(block, expr.getFieldValue());
						if (pred != null) {
							cfg.addEdge(pred, fieldCfg);
						} else {
							first.add(fieldCfg);
						}
						pred = fieldCfg;
					}

					if (expr.isSet()) {
						CFGNode valCfg = createArtificialNode(block, expr.getSetValue());
						if (pred != null) {
							cfg.addEdge(pred, valCfg);
						} else {
							first.add(valCfg);
						}
						pred = valCfg;
					}

					if (pred != null) {
						cfg.addEdge(pred, cfgNode);
					} else {
						first.add(cfgNode);
					}

					firstNodes.put(block, first);
					lastNodes.put(block, last);
				} else {
					Set<CFGNode> firstAndLast = HashSetFactory.make(1);
					firstAndLast.add(cfgNode);
					firstNodes.put(block, firstAndLast);
					lastNodes.put(block, firstAndLast);
				}
			} else {
				Set<CFGNode> firstAndLast = HashSetFactory.make(1);
				firstAndLast.add(cfgNode);
				firstNodes.put(block, firstAndLast);
				lastNodes.put(block, firstAndLast);
			}
		}

	}

	private CFGNode findOrCreateNode(IExplodedBasicBlock bb, AbstractParameterNode param) {
		CFGNode node = sdg2cfg.get(param);

		if (node == null) {
			node = new CFGNode(param);
			sdg2cfg.put(param, node);
			node2bb.put(node, bb);
			cfg.addNode(node);
		}

		return node;
	}

	private CFGNode createNode(IExplodedBasicBlock bb) {
		CFGNode node = new CFGNode(bb);

		node2bb.put(node, bb);
		cfg.addNode(node);

		return node;
	}

	private CFGNode createArtificialNode(IExplodedBasicBlock bb, AbstractPDGNode node) {
		CFGNode cfgNode = new CFGNode(node);
		cfg.addNode(cfgNode);
		node2bb.put(cfgNode, bb);
		sdg2cfg.put(node, cfgNode);

		return cfgNode;
	}

	private CFGNode createNode(IExplodedBasicBlock bb, CallNode call) {
		CFGNode node = new CFGNode(call);

		node2bb.put(node, bb);
		cfg.addNode(node);

		return node;
	}

	public final class CFGNode {

		private final IExplodedBasicBlock bb;
		private final AbstractParameterNode param;
		private final CallNode call;
		private final AbstractPDGNode artificial;

		private CFGNode(IExplodedBasicBlock bb) {
			this.bb = bb;
			this.param = null;
			this.call = null;
			this.artificial = null;
		}

		private CFGNode(AbstractParameterNode param) {
			this.bb = null;
			this.param = param;
			this.call = null;
			this.artificial = null;
		}

		private CFGNode(CallNode call) {
			this.bb = null;
			this.param = null;
			this.call = call;
			this.artificial = null;
		}

		private CFGNode(AbstractPDGNode artificial) {
			this.bb = null;
			this.param = null;
			this.call = null;
			this.artificial = artificial;
		}

		public final boolean isBasicBlock() {
			return bb != null;
		}

		public final boolean isParameter() {
			return param != null;
		}

		public final boolean isCall() {
			return call != null;
		}

		public final boolean isArtificial() {
			return artificial != null;
		}

		public final IExplodedBasicBlock getBasicBlock() {
			return bb;
		}

		public final AbstractParameterNode getParameterNode() {
			return param;
		}

		public final CallNode getCall() {
			return call;
		}

		public final AbstractPDGNode getArtificialNode() {
			return artificial;
		}

		public String toString() {
			if (param != null) {
				return "cfg: " + param.toString();
			} else if (bb != null) {
				return "cfg: " + Util.prettyBasicBlock(bb);
			} else if (call != null) {
				return "cfg: " + call;
			} else if (artificial != null) {
				return "cfg: " + artificial;
			} else {
				return "cfg: empty";
			}
		}
	}

	private final Iterator<IExplodedBasicBlock> getSuccs(IExplodedBasicBlock bb) {
		if (pdg.isIgnoreExceptions()) {
			return ecfg.getNormalSuccessors(bb).iterator();
		} else {
			return ecfg.getSuccNodes(bb);
		}
	}

	private void createCFG() {
		for (IExplodedBasicBlock block : ecfg) {
			Set<CFGNode> last = lastNodesOfBB(block);

			Iterator<IExplodedBasicBlock> it = getSuccs(block);
			while (it.hasNext()) {
				IExplodedBasicBlock succ = it.next();
				if (succ == null) {
					Log.warn("Basic block is null in " + Util.methodName(ecfg.getMethod()));
					continue;
				}

				Set<CFGNode> firstOfSucc = firstNodesOfBB(succ);
				for (CFGNode from : last) {
					for (CFGNode to : firstOfSucc) {
						cfg.addEdge(from, to);
					}
				}
			}
		}

		cfg.addEdge(entry, exit);
	}

	private Set<? extends AbstractParameterNode> getAllHeapLocatedActualOuts(CallNode call) {
		IParamSet<?> params = pdg.getParamModel().getModParams(call);

		return filterExitNode(params);
	}

	/**
	 * This does not filter non-heap nodes anymore, because they should also be included in the controlflow
	 * Therefore we have to ignore them later on during the dataflow analysis.
	 * @param params
	 * @return
	 */
	private static final Set<AbstractParameterNode> filterExitNode(IParamSet<?> params) {
		Set<AbstractParameterNode> result = HashSetFactory.make();

		if (params == null) {
			return result;
		}

		for (AbstractParameterNode node : params) {
			if (!node.isExit()) {
				result.add(node);
			}
		}

		return result;
	}

	private Set<? extends AbstractParameterNode> getAllHeapLocatedActualIns(CallNode call) {
		IParamSet<?> params = pdg.getParamModel().getRefParams(call);

		return filterExitNode(params);
	}

	private Set<CallNode> getCallsForInstruction(SSAInvokeInstruction invk) {
		Set<CallNode> calls = HashSetFactory.make();

		for (CallNode c: pdg.getAllCalls()) {
			if (c.getInstruction() == invk) {
				calls.add(c);
			}
		}

		return calls;
	}

	private Set<? extends AbstractParameterNode> getAllHeapLocatedFormIns() {
		IParamSet<?> params = pdg.getParamModel().getRefParams();

		return filterExitNode(params);
	}

	private Set<AbstractParameterNode> getAllHeapLocatedFormOuts() {
		IParamSet<?> params = pdg.getParamModel().getModParams();

		return filterExitNode(params);
	}

	public void removeNodeAndEdges(CFGNode N)
			throws UnsupportedOperationException {
		throw new UnsupportedOperationException();
	}

	public void addNode(CFGNode n) {
		throw new UnsupportedOperationException();
	}

	public boolean containsNode(CFGNode N) {
		return cfg.containsNode(N);
	}

	public int getNumberOfNodes() {
		return cfg.getNumberOfNodes();
	}

	public Iterator<CFGNode> iterator() {
		return cfg.iterator();
	}

	public void removeNode(CFGNode n) {
		throw new UnsupportedOperationException();
	}

	public void addEdge(CFGNode src, CFGNode dst) {
		throw new UnsupportedOperationException();
	}

	public int getPredNodeCount(CFGNode N) {
		return cfg.getPredNodeCount(N);
	}

	public Iterator<CFGNode> getPredNodes(CFGNode N) {
		return cfg.getPredNodes(N);
	}

	public int getSuccNodeCount(CFGNode N) {
		return cfg.getSuccNodeCount(N);
	}

	public Iterator<CFGNode> getSuccNodes(CFGNode N) {
		return cfg.getSuccNodes(N);
	}

	public boolean hasEdge(CFGNode src, CFGNode dst) {
		return cfg.hasEdge(src, dst);
	}

	public void removeAllIncidentEdges(CFGNode node)
			throws UnsupportedOperationException {
		throw new UnsupportedOperationException();
	}

	public void removeEdge(CFGNode src, CFGNode dst)
			throws UnsupportedOperationException {
		throw new UnsupportedOperationException();
	}

	public void removeIncomingEdges(CFGNode node)
			throws UnsupportedOperationException {
		throw new UnsupportedOperationException();
	}

	public void removeOutgoingEdges(CFGNode node)
			throws UnsupportedOperationException {
		throw new UnsupportedOperationException();
	}

	public IMethod getMethod() {
		return ecfg.getMethod();
	}

	public CFGNode getEntry() {
		return entry;
	}

	public CFGNode getExit() {
		return exit;
	}
}
