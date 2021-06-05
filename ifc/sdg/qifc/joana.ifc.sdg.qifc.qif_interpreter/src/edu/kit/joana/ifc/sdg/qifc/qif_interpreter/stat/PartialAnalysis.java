package edu.kit.joana.ifc.sdg.qifc.qif_interpreter.stat;

import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.ir.LoopBody;
import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.pipeline.Environment;
import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.stat.nildumu.Converter;
import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.stat.nildumu.NildumuProgram;
import nildumu.Parser;

import java.util.List;

public class PartialAnalysis {

	Environment env;

	public int analyze(LoopBody l, NildumuProgram.ConvertedLoopMethod loopMethod) {
		Converter c = new Converter(env.nProgram.context);
		List<Parser.StatementNode> inputs = c.convertToSecretInput(loopMethod.params, loopMethod.iMethod);
		List<Parser.StatementNode> outputs = c.convertToPublicOutput(loopMethod.returnVars, loopMethod.iMethod);
		return 0;
	}
}