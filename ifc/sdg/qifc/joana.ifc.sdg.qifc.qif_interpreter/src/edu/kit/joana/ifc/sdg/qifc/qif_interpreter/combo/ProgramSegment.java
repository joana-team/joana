package edu.kit.joana.ifc.sdg.qifc.qif_interpreter.combo;

import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.ir.Program;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Map;
import java.util.stream.Collectors;

public class ProgramSegment extends Segment<Program> {

	public ProgramSegment(Program p) {
		this.level = 0;
		this.parent = null;
		this.children = new ArrayList<>();

		this.inputs = p.getEntryMethod().getProgramValues().entrySet().stream().filter(e -> e.getValue().isParameter())
				.collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().getVars()));
		this.outputs = new ArrayList<>();
		this.arrayOutputs = new ArrayList<>();
		this.arrayInputs = Collections.emptyMap();
	}

	@Override public void finalize() {

	}
}