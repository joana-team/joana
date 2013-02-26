package edu.kit.joana.ui.ifc.wala.rifl;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.bind.JAXB;

import edu.kit.joana.ui.ifc.wala.console.console.JoanaBatch;
import edu.kit.joana.ui.ifc.wala.rifl.xml.Assign;
import edu.kit.joana.ui.ifc.wala.rifl.xml.Attribute;
import edu.kit.joana.ui.ifc.wala.rifl.xml.Category;
import edu.kit.joana.ui.ifc.wala.rifl.xml.Domainassignment;
import edu.kit.joana.ui.ifc.wala.rifl.xml.Parameter;
import edu.kit.joana.ui.ifc.wala.rifl.xml.Returnvalue;
import edu.kit.joana.ui.ifc.wala.rifl.xml.Sourcesandsinks;

public final class JoanaRIFL {
	
	private JoanaRIFL() {}
	
	
	private static final JoanaSpec extractJoanaSpecification(Sourcesandsinks sas, Domainassignment dass) {
		final Map<String, String> srcRet = new HashMap<String, String>();
		final Map<String, String> snkRet = new HashMap<String, String>();
	
		final Map<String, String> cat2level = new HashMap<String, String>();
		for (Assign ass : dass.getAssign()) {
			cat2level.put(ass.getCategory(), ass.getSecuritydomain());
		}
	
		for (Category c : sas.getSources().getCategory()) {
			for (Attribute a : c.getAttributes().getAttribute()) {
				srcRet.put(a.getName(), cat2level.get(c.getName()));
			}
			
			for (Parameter p : c.getParameters().getParameter()) {
				srcRet.put(p.getMethodname()+ "->p"+p.getPosition(), cat2level.get(c.getName()));
			}
			
			for (Returnvalue r : c.getReturnvalues().getReturnvalue()) {
				srcRet.put(r.getMethodname()+"->exit", cat2level.get(c.getName()));
			}
		}
		
		for (Category c : sas.getSinks().getCategory()) {
			for (Attribute a : c.getAttributes().getAttribute()) {
				snkRet.put(a.getName(), cat2level.get(c.getName()));
			}
			
			for (Parameter p : c.getParameters().getParameter()) {
				snkRet.put(p.getMethodname()+ "->p"+p.getPosition(), cat2level.get(c.getName()));
			}
			
			for (Returnvalue r : c.getReturnvalues().getReturnvalue()) {
				snkRet.put(r.getMethodname()+"->exit", cat2level.get(c.getName()));
			}
		}
		
		return new JoanaSpec(srcRet, snkRet);
	}
	
	public static void main(String[] args) throws IOException {
		// classpath, entry method, sources-and-sinks-spec, domain assignment
		Sourcesandsinks s = JAXB.unmarshal(new File(args[2]), Sourcesandsinks.class);
		Domainassignment dass = JAXB.unmarshal(new File(args[3]), Domainassignment.class);
		JoanaSpec joanaSpec = extractJoanaSpecification(s, dass);
		List<String> script = generateJoanaInstructions(args[0], args[1], joanaSpec);
		dumpScript(script, System.out);
		JoanaBatch.executeScriptWithStandardOutput(script);
	}
	
	private static List<String> generateJoanaInstructions(String classPath, String entryMethod, JoanaSpec jSpec) {
		List<String> ret = new ArrayList<String>();
		ret.add("setClasspath " + classPath);
		ret.add("searchEntries");
		ret.add("selectEntry " + entryMethod);
		ret.add("buildSDG false NONE INTRAPROC");
		ret.addAll(generateJoanaSpecInstructions(jSpec));
		ret.add("run poss");
		return ret;
	}
	
	private static void dumpScript(List<String> script, PrintStream out) {
		for (String line : script) {
			out.println(line);
		}
	}
	
	private static List<String> generateJoanaSpecInstructions(JoanaSpec jSpec) {
		final List<String> ret = new ArrayList<String>();
		
		for (Map.Entry<String, String> spart : jSpec.getSourcesSpec().entrySet()) {
			ret.add(String.format("source %s %s", spart.getKey(), spart.getValue()));
		}
		
		for (Map.Entry<String, String> spart : jSpec.getSinksSpec().entrySet()) {
			ret.add(String.format("sink %s %s", spart.getKey(), spart.getValue()));
		}
		
		return ret;
	}
}

class JoanaSpec {
	
	private final Map<String, String> sourcesSpec;
	private final Map<String, String> sinksSpec;
	
	
	public JoanaSpec(Map<String, String> sourcesSpec,
			Map<String, String> sinksSpec) {
		this.sourcesSpec = sourcesSpec;
		this.sinksSpec = sinksSpec;
	}


	public Map<String, String> getSourcesSpec() {
		return sourcesSpec;
	}


	public Map<String, String> getSinksSpec() {
		return sinksSpec;
	}
	
	
}
