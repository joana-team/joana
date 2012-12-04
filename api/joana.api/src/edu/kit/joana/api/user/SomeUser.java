/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.api.user;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import com.ibm.wala.ipa.cha.ClassHierarchyException;
import com.ibm.wala.util.CancelException;
import com.ibm.wala.util.graph.GraphIntegrity.UnsoundGraphException;

import edu.kit.joana.api.IFCAnalysis;
import edu.kit.joana.api.IllicitFlow;
import edu.kit.joana.api.annotations.IFCAnnotation;
import edu.kit.joana.api.annotations.IFCAnnotation.Type;
import edu.kit.joana.api.sdg.SDGConfig;
import edu.kit.joana.api.sdg.SDGProgram;
import edu.kit.joana.api.sdg.SDGProgramPart;
import edu.kit.joana.util.Stubs;
import edu.kit.joana.wala.core.SDGBuilder.FieldPropagation;
import edu.kit.joana.wala.core.SDGBuilder.PointsToPrecision;

class Annotation {
	private String ppSpec;
	private SimpleSecurityLevel level;
	private String[] contexts;

	Annotation(String ppSpec, SimpleSecurityLevel level, String... contexts) {
		this.ppSpec = ppSpec;
		this.level = level;
		this.contexts = contexts.clone();
	}

	Set<IFCAnnotation> toIFCAnnotations(String entryMethod, SDGProgram program, AnalysisType analysisType) {
		Set<IFCAnnotation> ret = new HashSet<IFCAnnotation>();
		SDGProgramPart pp;
		if (ppSpec.startsWith("->")) {
			ppSpec = entryMethod + ppSpec;
		}
		pp = program.getPart(ppSpec);


		if (contexts.length == 0) {
			ret.add(new IFCAnnotation(analysisType.getType(level), level.toString(), pp));
		} else {
			for (String ctx : contexts) {
				ret.add(new IFCAnnotation(analysisType.getType(level), level.toString(), pp,
						program.getMethod(ctx)));
			}
		}
		return ret;
	}
}

enum AnalysisType {

	CONFIDENTIALITY,
	INTEGRITY;

	IFCAnnotation.Type getType(SimpleSecurityLevel level) {
		switch (this) {
		case CONFIDENTIALITY:
			switch (level) {
			case LOW:
				return Type.SINK;
				/** low sink == public output/effects */
			case HIGH:
				return Type.SOURCE;
				/** high source == secret input */
			default:
				throw new IllegalStateException();
			}
		case INTEGRITY:
			switch (level) {
			case LOW:
				return Type.SOURCE;
				/** low source == public input */
			case HIGH:
				return Type.SINK;
				/** high sink == secret output/effects */
			default:
				throw new IllegalStateException();
			}
		default:
			throw new IllegalStateException();
		}
	}
}

enum SimpleSecurityLevel {
	LOW,
	HIGH;

	public String toString() {
		switch (this) {
		case LOW:
			return "low";
		case HIGH:
			return "high";
		default:
			throw new IllegalStateException();
		}
	}
}

class IFCConfig {
	private SDGConfig sdgConfig;
	private AnalysisType analysisType;
	private List<Annotation> anns;

	IFCConfig(String classPath, String entryMethod, Stubs stubsPath, AnalysisType analysisType) {
		this(new SDGConfig(classPath, entryMethod, stubsPath), analysisType);
	}

	IFCConfig(SDGConfig sdgConfig, AnalysisType analysisType) {
		this(sdgConfig, analysisType, new ArrayList<Annotation>());
	}

	IFCConfig(SDGConfig sdgConfig, AnalysisType analysisType, List<Annotation> anns) {
		this.sdgConfig = sdgConfig;
		this.analysisType = analysisType;
		this.anns = anns;
	}

	void addAnnotation(Annotation a) {
		anns.add(a);
	}

	IFCAnalysis toIFCAnalysis() throws ClassHierarchyException, IOException, UnsoundGraphException, CancelException {
		SDGProgram program = SDGProgram.createSDGProgram(sdgConfig);
		IFCAnalysis ifcAnalysis = new IFCAnalysis(program);

		for (Annotation a : anns) {
			for (IFCAnnotation ann : a.toIFCAnnotations(sdgConfig.getEntryMethod(), program, analysisType)) {
				ifcAnalysis.addAnnotation(ann);
			}
		}

		return ifcAnalysis;
	}

	Collection<IllicitFlow> run() throws ClassHierarchyException, IOException, UnsoundGraphException, CancelException {
		IFCAnalysis ifcAnalysis = toIFCAnalysis();
		return ifcAnalysis.doIFC();
	}

}

public class SomeUser {

	public static void main(String[] args) throws ClassHierarchyException, IOException, UnsoundGraphException,
			CancelException {

		// setup sdg building configuration
		SDGConfig config = new SDGConfig("bin", "example.Example.foo(I)I", Stubs.NO_STUBS);
		config.setFieldPropagation(FieldPropagation.FLAT);
		config.setPointsToPrecision(PointsToPrecision.OBJECT_SENSITIVE);

		// specify annotations
		List<Annotation> anns = new LinkedList<Annotation>();
		anns.add(new Annotation("->p1", SimpleSecurityLevel.HIGH));
		anns.add(new Annotation("->exit", SimpleSecurityLevel.LOW));

		IFCConfig ifcConfig = new IFCConfig(config, AnalysisType.CONFIDENTIALITY, anns);

		Collection<IllicitFlow> illFlows = ifcConfig.run();
		if (illFlows.isEmpty()) {
			System.out.println("No illegal flows detected.");
		} else {
			System.out.println("The following illegal flows were detected: ");
			for (IllicitFlow iFlow : illFlows) {
				System.out.println(iFlow);
			}
		}
	}
}
