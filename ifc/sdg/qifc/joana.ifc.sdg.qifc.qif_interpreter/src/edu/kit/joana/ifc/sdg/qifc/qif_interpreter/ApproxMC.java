package edu.kit.joana.ifc.sdg.qifc.qif_interpreter;

import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.util.LogicUtil;
import org.logicng.formulas.Formula;
import org.logicng.formulas.Variable;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.List;

public class ApproxMC {


	public int estimateModelCount(Formula f, List<Variable> samplingSet) throws IOException, InterruptedException {
		Formula cnf = f.cnf();
		System.out.println(cnf);
		LogicUtil.writeDimacsFile("leaked.cnf", cnf, samplingSet, true);
		invokeApproxMC(new File("leaked.cnf"));

		return -1;
	}

	private void invokeApproxMC(File f) throws IOException, InterruptedException {
		// String cmd = "approxMC/approxmc/build/approxmc " + f.getPath();
		String cmd = "../../../../contrib/lib/approxmc " + f.getPath();
		Runtime run = Runtime.getRuntime();
		Process pr = run.exec(cmd);
		pr.waitFor();
		BufferedReader buf = new BufferedReader(new InputStreamReader(pr.getInputStream()));
		String line = "";
		while ((line=buf.readLine())!= null) {
			System.out.println(line);
		}
	}
}
