package edu.kit.joana.ifc.sdg.qifc.qif_interpreter.ir;

import com.ibm.wala.ssa.SSAInstruction;
import org.logicng.formulas.Formula;

import java.util.List;
import java.util.Map;

public interface ISATAnalysisFragment {

	void setDepsForValnum(int valNum, Formula[] deps);

	Formula[] getDepsForValnum(int valNum);

	Map<Integer, Formula[]> getFragmentDeps();

	boolean hasValnum(int valNum);

	void createValnum(int valNum, SSAInstruction i);

	Method getOwner();

	List<ISATAnalysisFragment> getChildFragments();

	FragmentType getFragmentType();

	/**
	 * @param valNum a value number that whose def is part of this fragment or one of its child fragments
	 * @return ragment that contains the value for the provided valNum. If no matching fragment is found the method returns null.
	 * The method will only look for the value number in the child fragments, not in any owner fragments.
	 */
	default ISATAnalysisFragment getFragmentForValNum(int valNum) {
		if (this.hasValnum(valNum)) {
			return this;
		} else {
			for (ISATAnalysisFragment frag: this.getChildFragments()) {
				if (getFragmentForValNum(valNum) != null) {
					return frag;
				}
			}
		}
		return null;
	}
}
