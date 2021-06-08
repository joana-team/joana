package edu.kit.joana.ifc.sdg.qifc.qif_interpreter.combo;

import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.ProgramPart;
import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.State;
import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.dyn.SATVisitor;
import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.ir.BasicBlock;
import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.ir.Method;
import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.util.CFGUtil;
import org.logicng.formulas.Formula;

import java.util.HashSet;
import java.util.Set;

public class ConditionalSegment extends Segment<ProgramPart.Container> {

	public Formula branchCondition;

	public ConditionalSegment(Segment<? extends ProgramPart> parent, Formula branchCondition, BasicBlock split) {
		super(new ProgramPart.Container(new HashSet<>()), parent);
		this.branchCondition = branchCondition;

		int ifTarget = (split.succs().get(0).idx() == split.getTrueTarget()) ? 0 : 1;
		BasicBlock ifBranch = split.succs().get(ifTarget);
		BasicBlock elseBranch = split.succs().get(1 - ifTarget);
		this.createBranch(split, ifBranch, 0);
		this.createBranch(split, elseBranch, 1);
		this.children.forEach(c -> this.programPart.blocks.addAll((((ProgramPart.Container) c.programPart).blocks)));
		split.setCondSegment(this);
	}

	@Override public boolean owns(BasicBlock block) {
		return this.children.stream().anyMatch(c -> c.owns(block));
	}

	@Override public void finalize() {
	}

	@Override public Set<BasicBlock> getBlocks() {
		HashSet<BasicBlock> allBLocks = new HashSet<>(this.getIfBranch().getBlocks());
		allBLocks.addAll(this.getElseBranch().getBlocks());
		return allBLocks;
	}

	public ContainerSegment getIfBranch() {
		return (ContainerSegment) this.children.get(0);
	}

	public ContainerSegment getElseBranch() {
		return (ContainerSegment) this.children.get(1);
	}

	private void createBranch(BasicBlock split, BasicBlock branchStart, int rank) {
		Set<BasicBlock> blocks = new HashSet<>(CFGUtil.computeConditionalBranch(split, branchStart));
		ContainerSegment container = new ContainerSegment(new ProgramPart.Container(blocks), this);
		container.rank = rank;
		this.children.add(container);
	}

	@Override public String getLabel() {
		return this.rank + "\n" + "Conditional";
	}

	@Override public State computeSATDeps(State state, Method m, SATVisitor sv) {
		assert (this.owns(state.current));
		state = this.getIfBranch().computeSATDeps(state, m, sv);
		state = this.getElseBranch().computeSATDeps(state, m, sv);
		return state;
	}
}