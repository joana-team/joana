package edu.kit.joana.ifc.sdg.qifc.qif_interpreter.stat;

import com.ibm.wala.ssa.SSAArrayLoadInstruction;
import com.ibm.wala.ssa.SSAArrayStoreInstruction;
import com.ibm.wala.util.collections.Pair;
import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.ir.Array;
import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.ir.BBlock;
import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.ir.LoopBody;
import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.ir.Value;
import org.logicng.formulas.Formula;

import java.util.List;
import java.util.stream.Collectors;

/**
 * when analysing loops we use special placeholder array values
 *
 * this class re-defines all array-specific visit methods to make sure we use the right reference objects
 */
public class LoopSATVisitor extends SATVisitor {

	private LoopBody l;

	public void setLoop(LoopBody l) {
		this.l = l;
	}

	public LoopSATVisitor(StaticAnalysis staticAnalysis, LoopBody l) {
		super(staticAnalysis);
		this.l = l;
	}

	public LoopSATVisitor(StaticAnalysis staticAnalysis) {
		super(staticAnalysis);
	}

	@Override public void visitArrayLoad(SSAArrayLoadInstruction instruction) {
		Array<? extends Value> placeHolder = l.getPlaceholderArray(instruction.getArrayRef());
		this.visitArrayLoad(instruction, placeHolder);
	}


	@Override public void visitArrayStore(SSAArrayStoreInstruction instruction) {
		Array<? extends Value> placeHolder = l.getPlaceholderArray(instruction.getArrayRef());
		this.visitArrayStore(instruction, placeHolder, this.getCurrentBlock().generateImplicitFlowFormula());
	}
}
