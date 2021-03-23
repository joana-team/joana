package edu.kit.joana.ifc.sdg.qifc.qif_interpreter.ir;

import com.ibm.wala.ssa.SSAInstruction;
import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.SimpleLoopHandler;
import org.logicng.formulas.Formula;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class LoopBody implements ISATAnalysisFragment {

	private final Map<Integer, Formula[]> deps;
	private final Method owner;
	private final List<ISATAnalysisFragment> childFragments;
	private final SimpleLoopHandler handler;

	public LoopBody(Method owner, SimpleLoopHandler handler) {
		this.owner = owner;
		this.childFragments = new ArrayList<>();
		deps = new HashMap<>();
		this.handler = handler;
	}

	@Override public void setDepsForValnum(int valNum, Formula[] deps) {
		this.deps.put(valNum, deps);
	}

	@Override public Formula[] getDepsForValnum(int valNum) {
		return deps.get(valNum);
	}

	@Override public Map<Integer, Formula[]> getFragmentDeps() {
		return deps;
	}

	@Override public boolean hasValnum(int valNum) {
		return deps.containsKey(valNum);
	}

	@Override public void createValnum(int valNum, SSAInstruction i) {
		assert (i.getNumberOfUses() > 0);
		int op = i.getUse(0);
		int width;

		if (deps.containsKey(op)) {
			width = deps.get(op).length;
		} else {
			assert (owner.hasValnum(op));
			width = owner.getDepsForValnum(op).length;
		}
		deps.put(valNum, new Formula[width]);
	}

	@Override public Method getOwner() {
		return owner;
	}

	@Override public List<ISATAnalysisFragment> getChildFragments() {
		return this.childFragments;
	}

	@Override public FragmentType getFragmentType() {
		return FragmentType.LOOP;
	}
}
