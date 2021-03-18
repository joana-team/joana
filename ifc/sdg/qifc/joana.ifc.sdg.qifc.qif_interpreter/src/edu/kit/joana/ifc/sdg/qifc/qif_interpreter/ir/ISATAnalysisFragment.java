package edu.kit.joana.ifc.sdg.qifc.qif_interpreter.ir;

import com.ibm.wala.ssa.SSAInstruction;
import org.logicng.formulas.Formula;

import java.util.Map;

public interface ISATAnalysisFragment {

	void setDepsForValnum(int valNum, Formula[] deps);

	Formula[] getDepsForValnum(int valNum);

	Map<Integer, Formula[]> getFragmentDeps();

	boolean hasValnum(int valNum);

	void createValnum(int valNum, SSAInstruction i);

	Method getOwner();
}
