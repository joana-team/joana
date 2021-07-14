package edu.kit.joana.ifc.sdg.qifc.qif_interpreter.dyn;

import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.ProgramPart;
import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.combo.Segment;
import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.ir.LoopBody;
import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.ir.Method;
import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.ir.Value;
import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.util.LogicUtil;
import org.logicng.formulas.Formula;

import java.util.stream.IntStream;

public class TempValue {

	public int valNum;
	public Segment<? extends ProgramPart> owningSeg; // temp values either represent the output of a loop or a function
	public Formula[] tempVars;
	public Formula[] real;
	public Value.BitLatticeValue[] afterLoopConstantBits;
	public Formula valueRestriction;

	public TempValue(int valNum, Method m, LoopBody loop, Value.BitLatticeValue[] alcb) {
		this.valNum = valNum;
		this.tempVars = m.getDepsForValue(valNum);
		this.real = LogicUtil.createVars(valNum, m.getValue(valNum).getWidth(), "bot");
		this.owningSeg = loop.getSegment();
		this.afterLoopConstantBits = alcb;
	}

	public void setReal(Formula[] deps) {
		this.real = deps;
	}

	public Formula asValueRestrictedFormula() {
		return IntStream.range(0, tempVars.length).mapToObj(i -> LogicUtil.ff.equivalence(tempVars[i], real[i]))
				.reduce(valueRestriction, LogicUtil.ff::and);
	}

	public Formula asOpenFormula() {
		Formula[] afterLoop = new Formula[this.afterLoopConstantBits.length];
		for (int i = 0; i < afterLoop.length; i++) {
			if (this.afterLoopConstantBits[i] == Value.BitLatticeValue.UNKNOWN) {
				afterLoop[i] = real[i];
			} else {
				afterLoop[i] = this.afterLoopConstantBits[i].asPropFormula();
			}
		}
		return IntStream.range(0, tempVars.length).mapToObj(i -> LogicUtil.ff.equivalence(tempVars[i], afterLoop[i]))
				.reduce(LogicUtil.ff.constant(true), LogicUtil.ff::and);
	}
}