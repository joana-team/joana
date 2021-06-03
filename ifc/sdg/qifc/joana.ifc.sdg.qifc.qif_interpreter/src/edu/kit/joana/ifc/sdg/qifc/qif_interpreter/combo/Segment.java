package edu.kit.joana.ifc.sdg.qifc.qif_interpreter.combo;

import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.ProgramPart;
import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.dyn.State;
import org.logicng.formulas.Formula;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public abstract class Segment<T extends ProgramPart> {

	T programPart;
	int level;
	Segment<? extends ProgramPart> parent;
	List<Segment<? extends ProgramPart>> children;

	Map<Integer, Formula[]> inputs;
	Map<Integer, Formula[][]> arrayInputs;

	List<Integer> outputs;
	List<Integer> arrayOutputs;

	int channelCapacity;
	Formula executionCondition;

	public Segment(T t, Segment<? extends ProgramPart> parent) {
		this.programPart = t;
		this.level = parent.level + 1;
		this.parent = parent;
		parent.registerChild(this);
		this.children = new ArrayList<>();

		this.inputs = new HashMap<>();
		this.arrayInputs = new HashMap<>();
		this.outputs = new ArrayList<>();
		this.arrayOutputs = new ArrayList<>();
	}

	public Segment() {
	}

	public void registerChild(Segment<? extends ProgramPart> newChild) {
		this.children.add(newChild);
	}

	public State analyse() {
		return null;
	}

	public abstract void finalize();
}