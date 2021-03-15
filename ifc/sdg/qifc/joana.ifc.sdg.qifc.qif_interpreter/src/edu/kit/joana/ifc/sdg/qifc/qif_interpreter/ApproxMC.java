package edu.kit.joana.ifc.sdg.qifc.qif_interpreter;

import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.oopsies.ErrorHandler;
import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.util.LogicUtil;
import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.util.Util;
import org.logicng.formulas.Formula;
import org.logicng.formulas.Variable;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ApproxMC {

	private static final Pattern resultLine = Pattern.compile("s mc ([0-9]*)");
	private static final String approcMCPath = "../../../../contrib/lib/approxmc ";

	public int estimateModelCount(Formula f, List<Variable> samplingSet) throws IOException, InterruptedException {
		Formula cnf = f.cnf();
		System.out.println(cnf);
		LogicUtil.writeDimacsFile("leaked.cnf", cnf, samplingSet, true);
		return invokeApproxMC("leaked.cnf", null);
	}

	public int invokeApproxMC(String in, String out) throws IOException, InterruptedException {

		String cmd = approcMCPath + in;
		Runtime run = Runtime.getRuntime();
		Process pr = run.exec(cmd);

		BufferedReader buf = new BufferedReader(new InputStreamReader(pr.getInputStream()));
		String line = "";
		StringBuilder sb = new StringBuilder();
		Matcher m;
		int res = -1;
		while ((line = buf.readLine()) != null) {
			sb.append(line).append(System.lineSeparator());
			m = resultLine.matcher(line);
			if (m.matches()) {
				res = Integer.parseInt(m.group(1));
			}
		}
		if (res == -1) {
			ErrorHandler.fatal(new IOException("Estimation of Indistinguishability relation failed. Aborting."));
		}

		if (out != null) {
			Util.dumpToFile(out, sb);
		}
		return res;
	}

	public boolean isResult(String line) {
		Matcher m = resultLine.matcher(line);
		return m.matches();
	}
}
