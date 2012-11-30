/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.deprecated.jsdg.exceptions.nullpointer;

import java.io.File;


import com.ibm.wala.cfg.ControlFlowGraph;
import com.ibm.wala.cfg.IBasicBlock;
import com.ibm.wala.dataflow.graph.DataflowSolver;
import com.ibm.wala.ssa.ISSABasicBlock;
import com.ibm.wala.ssa.SSAInstruction;
import com.ibm.wala.util.CancelException;
import com.ibm.wala.util.WalaException;

import edu.kit.joana.deprecated.jsdg.Analyzer;
import edu.kit.joana.deprecated.jsdg.util.ExtendedNodeDecorator;
import edu.kit.joana.deprecated.jsdg.wala.viz.DotUtil;

/**
 * @author Juergen Graf <graf@kit.edu>
 *
 */
public final class DebugUtil {

	private DebugUtil() {
	}

	public static class CFGNodeDec extends ExtendedNodeDecorator.DefaultImpl {
		private final DataflowSolver<? extends ISSABasicBlock, EdgeState> solver;

		public CFGNodeDec(DataflowSolver<? extends ISSABasicBlock, EdgeState> solver) {
			this.solver = solver;
		}

		public String getLabel(Object o) {
			if (o instanceof ISSABasicBlock) {
				ISSABasicBlock bb = (ISSABasicBlock) o;

				EdgeState in = solver.getIn(bb);

				if (bb.isEntryBlock()) {
					return in + " ENTRY";
				} else if (bb.isExitBlock()) {
					return in + "EXIT";
				} else if (bb.isCatchBlock() || bb.getLastInstructionIndex() > 0) {
					SSAInstruction instr = NullPointerTransferFunctionProvider.getRelevantInstruction(bb);
					if (instr != null) {
//						return in + edu.kit.joana.wala.util.Util.prettyShortInstruction(instr) + " [" + bb.getNumber() + "]";
						return in + instr.toString();
					}
				} else {
					return in + o.toString();
				}
			}

			return o.toString();
		}
	}

	public static <I, T extends IBasicBlock<I>> void dumpCFGorigExpl(ControlFlowGraph<I, T> cfg, String prefix,
			CFGNodeDec nodeDec) {
		String outputDir = Analyzer.cfg.outputDir;
		File cfgOut = new File(outputDir + "/cfg-orig-expl/");
		if (!cfgOut.exists()) {
			cfgOut.mkdir();
		}

		String fileName = edu.kit.joana.deprecated.jsdg.util.Util.methodName(cfg.getMethod());
//		if (fileName.length() > edu.kit.joana.wala.util.Util.MAX_FILENAME_LENGHT) {
//			fileName = fileName.substring(edu.kit.joana.wala.util.Util.MAX_FILENAME_LENGHT) + edu.kit.joana.wala.util.Util.fileNameId++;
//		}
		String dotFile = outputDir + "/cfg-orig-expl/" + prefix + fileName + ".cfg-orig-expl.dot";
		try {
			DotUtil.dotify(cfg, cfg, nodeDec, dotFile, null, edu.kit.joana.deprecated.jsdg.util.Util.DOT_EXEC, null);
		} catch (WalaException e) {
			e.printStackTrace();
		} catch (CancelException e) {
			e.printStackTrace();
		}
	}
}
