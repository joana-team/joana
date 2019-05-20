/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */

package edu.kit.joana.ifc.sdg.qifc.nildumu;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import com.google.common.collect.Iterators;
import com.ibm.wala.ssa.ISSABasicBlock;
import com.ibm.wala.ssa.SSACFG;
import com.ibm.wala.ssa.SSAConditionalBranchInstruction;
import com.ibm.wala.ssa.SSAGotoInstruction;
import com.ibm.wala.ssa.SSAInstruction;
import com.ibm.wala.ssa.SSAPhiInstruction;
import com.ibm.wala.ssa.SymbolTable;

import edu.kit.joana.ifc.sdg.graph.SDGEdge;
import edu.kit.joana.ifc.sdg.graph.SDGNode;
import edu.kit.joana.ifc.sdg.qifc.nildumu.util.Util;
import edu.kit.joana.wala.core.PDGNode;

/**
 * A control flow graph based on the dependencies between basic blocks.
 * 
 * It extends the normal Dominators class by providing methods to get the currently
 * affecting {@code if}-conditions for a SDGNode and to get the conditions that affect
 * the arguments of a phi-node.
 * 
 * Main idea: 
 * The demo application works by using explicit end-nodes to signify that the reach of
 * a specific conditional-statement ended. It also uses the fact that the body of
 * a conditional statement is executed as a whole.
 * This is infeasible for a proper PDG. Therefore a new approach has to be found
 * to enable a proper handling of condition introduced bit modificators.
 * This solution has to provide two key functions:
 * <ul>
 * <li>
 * It has to provide the conditional SDGNode that directly affects whether an
 * given SDGNode would be executed if the program would be evaluated and
 * for which value of the conditional expression.
 * <li>
 * <li>
 * It also has to provide the same information for every argument of a phi-node.
 * </li>
 * </ul>
 */
public class BasicBlockGraph extends Dominators<ISSABasicBlock> {

	private final Program program;
	private final SSACFG cfg;
	
	public BasicBlockGraph(Program program, SSACFG cfg) {
		super(cfg.entry(), b -> {
			// getNormalSuccessors seems to discard any exception related
			// successors
			return cfg.getNormalSuccessors(b);
		});
		this.program = program;
		this.cfg = cfg;
	}
	
	/**
	 * Returns the conditional SDGNode that directly affects whether an
	 * given SDGNode would be executed if the program would be evaluated and
	 * for which value of the conditional expression.
	 */
	public Optional<AffectingConditional> getAffectingConditional(SDGNode node) {
		return program.sdg.getIncomingEdgesOfKind(node, SDGEdge.Kind.CONTROL_DEP_COND).stream()
				.map(SDGEdge::getSource)
				.findFirst().map(cd -> {
					if (program.getInstruction(cd) == null || 
							!(program.getInstruction(cd) instanceof SSAConditionalBranchInstruction)) {
						return null;
					}
					SSAConditionalBranchInstruction instr = (SSAConditionalBranchInstruction)program.getInstruction(cd);
					// same block as branch instruction: `false` branch, else `true` branch
					// i.e. `if (…) goto …`
					ISSABasicBlock cdBlock = program.getBlock(cd);
					ISSABasicBlock nodeBlock = program.getBlock(node);
					if (cfg.getBlockForInstruction(instr.getTarget()).getNumber() == nodeBlock.getNumber()){
						return new AffectingConditional(cd, true);
					}
					return new AffectingConditional(cd, false);
				});
	}
	
	/**
	 * Returns the affecting conditions for the operands of a phi-node.
	 * <p>
	 * Assumption: the first block following a conditional block is the block for the cond == false branch
	 * 
	 * @see SSAPhiInstruction
	 */
	public List<AffectingConditional> getPhiOperandAffectingConditionals(SDGNode phi){
		assert program.getPDGNode(phi).getKind() == PDGNode.Kind.PHI;
		List<AffectingConditional> conds = new ArrayList<>();
		SymbolTable st = program.getProcSymbolTable(phi);
		SSAPhiInstruction instr = (SSAPhiInstruction)program.getInstruction(phi);
		ISSABasicBlock phiBlock = program.getBlock(phi);
		List<ISSABasicBlock> predBlocks = Util.toList(cfg.getPredNodes(phiBlock));
		for (int useNum = 0; useNum < instr.getNumberOfUses(); useNum++) {
    		int use = instr.getUse(useNum);
    		ISSABasicBlock blockForOperand = predBlocks.get(useNum);
    		ISSABasicBlock condBlock = null;
    		ISSABasicBlock condBlockChild;
    		if (loopDepth(phiBlock) > loopDepth(blockForOperand)) {
    			// the operand is defined outside of the loop that the phi depends on
    			condBlockChild = blockForOperand;
    			condBlock = cfg.getSuccNodes(blockForOperand).next();
    			while (!doesBlockEndWithCondBranch(condBlock)) {
    				condBlockChild = condBlock;
    				condBlock = cfg.getSuccNodes(condBlock).next();
    			}
    		} else if (doesBlockEndWithCondBranch(blockForOperand)) {
    			// we found our block
    			condBlock = blockForOperand;
    			condBlockChild = phiBlock;
    		} else {
    			// one indirection is allowed
    			ISSABasicBlock curBlockForOperand = blockForOperand;
    			int ignoredConds = 0;
    			do {
    			  if (ignoredConds > 0 && cfg.getSuccNodeCount(condBlock) > 1) {
    			    ignoredConds--;
    			  }
    			  if (cfg.getPredNodeCount(curBlockForOperand) != 1 &&
    			      !Optional.ofNullable(getLastInstruction(curBlockForOperand)).map(SSAInstruction::isPEI).orElse(false)) {
    			    // if this is not a standard block, but a block, 
    			    // that joins the branches of an condition
    			    // ignore this condition
    			    ignoredConds++; 
    			  }
    			if (cfg.getPredNodeCount(curBlockForOperand) == 1) {
    				condBlock = cfg.getPredNodes(curBlockForOperand).next();
    			} else {
    				List<ISSABasicBlock> preds = Arrays.asList(Iterators.toArray(cfg.getPredNodes(curBlockForOperand), ISSABasicBlock.class));
    				for (ISSABasicBlock block : preds) {
    					if (preds.stream().allMatch(sub -> !dominators(sub).contains(block) || sub == block)) {
							condBlock = block;
							break;
						}
					}
    			}
      			condBlockChild = curBlockForOperand;
      			curBlockForOperand = condBlock;
    			} while (!doesBlockEndWithCondBranch(condBlock) || ignoredConds > 0);
    		}
    		// now we can find the cond branch instruction
    		SSAConditionalBranchInstruction condInstr = 
    				(SSAConditionalBranchInstruction)getLastInstruction(condBlock);
    		// and the branch: assumption: first successor of the condBlock is the
    		// block for which the conditional jump is not executed
    		boolean branch = cfg.getSuccNodes(condBlock).next().getNumber() == condBlockChild.getNumber();
    		//System.err.println("this is phi " + phi.getLabel());
    		SDGNode condNode = program.getSDGNodeForInstr(phi, condInstr);
    		conds.add(new AffectingConditional(condNode, branch));
    	}
		return conds;
	}
	
	private SSAInstruction getInstruction(ISSABasicBlock block, int id) {
		return Iterators.filter(block.iterator(), s -> s.iindex == id).next();
	}
	
	private boolean doesBlockEndWithCondBranch(ISSABasicBlock block) {
		return block.getLastInstructionIndex() >= 0 && 
				block.getLastInstruction() instanceof SSAConditionalBranchInstruction;
		//int first = block.getFirstInstructionIndex();
		//return first >= 0 && Iterators.any(block.iterator(), s -> s.iindex == first && 
		//		s instanceof SSAConditionalBranchInstruction);
	}
	
	private boolean doesBlockEndWithGoto(ISSABasicBlock block) {
		return block.getLastInstructionIndex() >= 0 && 
				block.getLastInstruction() instanceof SSAGotoInstruction;
		//int first = block.getFirstInstructionIndex();
		//return first >= 0 && Iterators.any(block.iterator(), s -> s.iindex == first && 
		//		s instanceof SSAConditionalBranchInstruction);
	}
	
	private SSAInstruction getLastInstruction(ISSABasicBlock block) {
		return block.getLastInstructionIndex() >= 0 ? block.getLastInstruction() : null;//getInstruction(block, block.getFirstInstructionIndex());
	}
}
