package edu.kit.joana.ifc.sdg.qifc.qif_interpreter.combo;

import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.ProgramPart;
import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.ir.Method;

import java.util.Map;
import java.util.stream.Collectors;

public class MethodSegment extends Segment<Method> {

	public MethodSegment(Method m, Segment<? extends ProgramPart> parent) {
		super(m, parent);

		this.inputs = m.getProgramValues().entrySet().stream().filter(e -> e.getValue().isParameter())
				.collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().getVars()));
	}

	@Override public void finalize() {

	}
}