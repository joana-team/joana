/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.deprecated.jsdg.exceptions.nullpointer;

import com.ibm.wala.cfg.ControlFlowGraph;
import com.ibm.wala.dataflow.graph.IKilldallFramework;
import com.ibm.wala.ssa.IR;
import com.ibm.wala.ssa.ISSABasicBlock;
import com.ibm.wala.ssa.SSAInstruction;
import com.ibm.wala.util.graph.Graph;

/**
 * @author Juergen Graf <graf@kit.edu>
 *
 */
class NullPointerFrameWork<T extends ISSABasicBlock>  implements IKilldallFramework<T, EdgeState> {

	private final ControlFlowGraph<SSAInstruction, T> cfg;
	private final NullPointerTransferFunctionProvider<T> transferFunct;

	NullPointerFrameWork(ControlFlowGraph<SSAInstruction, T> cfg, IR ir) {
		this.cfg = cfg;
		this.transferFunct = new NullPointerTransferFunctionProvider<T>(cfg, ir);
	}

	/* (non-Javadoc)
	 * @see com.ibm.wala.dataflow.graph.IKilldallFramework#getFlowGraph()
	 */
	@Override
	public Graph<T> getFlowGraph() {
		return cfg;
	}

	/* (non-Javadoc)
	 * @see com.ibm.wala.dataflow.graph.IKilldallFramework#getTransferFunctionProvider()
	 */
	@Override
	public NullPointerTransferFunctionProvider<T> getTransferFunctionProvider() {
		return transferFunct;
	}

}
