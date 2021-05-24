package edu.kit.joana.ifc.sdg.qifc.qif_interpreter.pipeline;

import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.App;
import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.ir.Program;
import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.stat.nildumu.NildumuProgram;

import java.util.HashMap;
import java.util.Map;

public class Environment {

	public IStage.IResult lastStage;
	public App.Args args;
	public Program iProgram;
	public NildumuProgram nProgram;
	public Map<IStage.Stage, Boolean> completedSuccessfully;

	public Environment(App.Args args) {
		this.args = args;
		this.completedSuccessfully = new HashMap<>();
	}

	public boolean completedStage(IStage.Stage stage) {
		return completedSuccessfully.getOrDefault(stage, false);
	}
}