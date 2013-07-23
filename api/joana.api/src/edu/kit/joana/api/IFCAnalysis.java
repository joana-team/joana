/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.api;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import com.ibm.wala.shrikeCT.AnnotationsReader.ElementValue;
import com.ibm.wala.shrikeCT.AnnotationsReader.EnumElementValue;
import com.ibm.wala.types.ClassLoaderReference;
import com.ibm.wala.types.TypeName;
import com.ibm.wala.types.TypeReference;
import com.ibm.wala.types.annotations.Annotation;

import edu.kit.joana.api.annotations.IFCAnnotation;
import edu.kit.joana.api.annotations.IFCAnnotation.Type;
import edu.kit.joana.api.annotations.IFCAnnotationManager;
import edu.kit.joana.api.annotations.Level;
import edu.kit.joana.api.annotations.Sink;
import edu.kit.joana.api.annotations.Source;
import edu.kit.joana.api.lattice.BuiltinLattices;
import edu.kit.joana.api.sdg.SDGMethod;
import edu.kit.joana.api.sdg.SDGProgram;
import edu.kit.joana.api.sdg.SDGProgramPart;
import edu.kit.joana.api.sdg.SDGProgramPartWriter;
import edu.kit.joana.ifc.sdg.core.IFC;
import edu.kit.joana.ifc.sdg.core.ReduceRedundantFlows;
import edu.kit.joana.ifc.sdg.core.conc.ConflictScanner;
import edu.kit.joana.ifc.sdg.core.conc.PossibilisticNIChecker;
import edu.kit.joana.ifc.sdg.core.conc.ProbabilisticNIChecker;
import edu.kit.joana.ifc.sdg.core.conc.TimeSensitiveIFCDecorator;
import edu.kit.joana.ifc.sdg.core.conc.ViolationTranslator;
import edu.kit.joana.ifc.sdg.core.violations.ClassifiedViolation;
import edu.kit.joana.ifc.sdg.core.violations.IConflictLeak;
import edu.kit.joana.ifc.sdg.core.violations.IIllegalFlow;
import edu.kit.joana.ifc.sdg.core.violations.IViolation;
import edu.kit.joana.ifc.sdg.graph.SDG;
import edu.kit.joana.ifc.sdg.graph.SDGNode;
import edu.kit.joana.ifc.sdg.graph.chopper.RepsRosayChopper;
import edu.kit.joana.ifc.sdg.graph.slicer.graph.threads.MHPAnalysis;
import edu.kit.joana.ifc.sdg.graph.slicer.graph.threads.PreciseMHPAnalysis;
import edu.kit.joana.ifc.sdg.graph.slicer.graph.threads.SimpleMHPAnalysis;
import edu.kit.joana.ifc.sdg.lattice.IEditableLattice;
import edu.kit.joana.ifc.sdg.lattice.IStaticLattice;
import edu.kit.joana.ifc.sdg.mhpoptimization.CSDGPreprocessor;
import edu.kit.joana.ifc.sdg.util.JavaType;
import edu.kit.joana.util.Log;
import edu.kit.joana.util.Logger;

public class IFCAnalysis {

	private SDGProgram program;
	private IFCAnnotationManager annManager;
	private IStaticLattice<String> secLattice;
	private IFCType ifcType = IFCType.POSSIBILISTIC;
	private IFC ifc;
	private boolean timeSensitiveAnalysis = false;
	private boolean removeRedundantFlows = false;

	public static final IStaticLattice<String> stdLattice = BuiltinLattices.getBinaryLattice();

	private static Logger debug = Log.getLogger("api.debug");

	public IFCAnalysis(SDGProgram program, IStaticLattice<String> secLattice) {
		if (program == null || secLattice == null) {
			throw new IllegalArgumentException("Neither program nor security lattice may be null!");
		}
		setProgram(program);
		setLattice(secLattice);
		setIFCType(IFCType.POSSIBILISTIC);
	}

	public IFCAnalysis(SDGProgram program) {
		this(program, stdLattice);
	}

	public void setProgram(SDGProgram program) {
		if (program == null) {
			throw new IllegalArgumentException("program must not be null!");
		}
		this.program = program;
		this.annManager = new IFCAnnotationManager(program);
		if (this.ifc != null) {
			this.ifc.setSDG(this.program.getSDG());
		} else {
			this.ifc = new PossibilisticNIChecker(this.program.getSDG(), secLattice);
		}
	}

	private void setIFCType(IFCType ifcType) {
		this.ifcType = ifcType;
		switch (this.ifcType) {
		case POSSIBILISTIC:
			this.ifc = new PossibilisticNIChecker(this.program.getSDG(), secLattice);
			if (timeSensitiveAnalysis) {
				if (this.program.getSDG().getThreadsInfo() == null) {
					CSDGPreprocessor.preprocessSDG(this.program.getSDG());
				}
				this.ifc = new TimeSensitiveIFCDecorator(this.ifc);
				if (removeRedundantFlows) {
					this.ifc = ReduceRedundantFlows.makeReducingConcurrentIFC(this.ifc);
				}
			}
			break;
		case PROBABILISTIC_WITH_SIMPLE_MHP:
			if (this.program.getSDG().getThreadsInfo() == null) {
				CSDGPreprocessor.preprocessSDG(this.program.getSDG());
			}
			MHPAnalysis mhpSimple = SimpleMHPAnalysis.analyze(this.program.getSDG());
			this.ifc = new ProbabilisticNIChecker(this.program.getSDG(), secLattice, mhpSimple,
					this.timeSensitiveAnalysis);
			break;
		case PROBABILISTIC_WITH_PRECISE_MHP:
			if (this.program.getSDG().getThreadsInfo() == null) {
				CSDGPreprocessor.preprocessSDG(this.program.getSDG());
			}
			MHPAnalysis mhpPrecise = PreciseMHPAnalysis.analyze(this.program.getSDG());
			this.ifc = new ProbabilisticNIChecker(this.program.getSDG(), secLattice, mhpPrecise,
					this.timeSensitiveAnalysis);
			break;
		default:
			throw new IllegalStateException("unhandled ifc type: " + ifcType + "!");
		}
	}

	public IFC getIFC() {
		return ifc;
	}

	public void setTimesensitivity(boolean newTimeSens) {
		this.timeSensitiveAnalysis = newTimeSens;
	}

	public void setLattice(IStaticLattice<String> secLattice) {
		if (secLattice == null) {
			throw new IllegalArgumentException();
		}
		this.secLattice = secLattice;
		if (this.ifc != null) {
			this.ifc.setLattice(secLattice);
		}
		clearAllAnnotations();
	}

	public void clearAllAnnotations() {
		annManager.removeAllAnnotations();
	}

	public Collection<IFCAnnotation> getSources() {
		return annManager.getSources();
	}

	public Collection<IFCAnnotation> getSinks() {
		return annManager.getSinks();
	}

	public Collection<IFCAnnotation> getDeclassifications() {
		return annManager.getDeclassifications();
	}

	public void addAnnotation(IFCAnnotation annotation) {
		String ppDesc = SDGProgramPartWriter.getStandardVersion().writeSDGProgramPart(annotation.getProgramPart());
		Collection<? extends SDGProgramPart> equivPParts = program.getParts(ppDesc);
		for (SDGProgramPart part : equivPParts) {
			annManager.addAnnotation(annotation.transferTo(part));
		}
	}

	public Collection<IFCAnnotation> getAnnotations() {
		return annManager.getAnnotations();
	}

	public Collection<IllicitFlow> doIFC(IFCType ifcType) {
		setIFCType(ifcType);
		assert ifc != null && ifc.getSDG() != null && ifc.getLattice() != null;

		annManager.applyAllAnnotations();
		long time = 0L;
		time = System.currentTimeMillis();
		Collection<ClassifiedViolation> vios = new ViolationTranslator().translate(ifc.checkIFlow());
		time = System.currentTimeMillis() - time;
		debug.outln(String.format("IFC Analysis took %d ms.", time));
		List<IllicitFlow> ret = new LinkedList<IllicitFlow>();

		Collection<SDGProgramPart> allParts = getProgram().getAllProgramParts();
		for (SDGProgramPart ppart : allParts) {
			debug.outln("Program part " + ppart + " with node(s): " + ppart.getAttachedNodes());
		}

		for (ClassifiedViolation vio : vios) {
			IllicitFlow ill = new IllicitFlow(vio, allParts);
			ret.add(ill);
			RepsRosayChopper c = new RepsRosayChopper(program.getSDG());
			SDGProgramPart illSrc = ill.getSource();
			SDGProgramPart illSnk = ill.getSink();
			if (illSrc != null && illSnk != null) {
				Collection<SDGNode> chop = c.chop(illSrc.getAttachedNodes(), illSnk.getAttachedNodes());
				debug.outln("Illicit flow with the following nodes involved: ");
				Map<SDGNode, Collection<SDGNode>> groupedByProc = groupByProc(program.getSDG(), chop);
				for (Map.Entry<SDGNode, Collection<SDGNode>> nProc : groupedByProc.entrySet()) {
					debug.outln("In method " + nProc.getKey().getBytecodeMethod() + ": " + nProc.getValue());
				}
			} else {
				IIllegalFlow v = ill.getViolation();
				debug.outln("unidentifiable flow from " + v.getSource() + " to " + v.getSink());
			}

		}
		
		Collection<? extends IConflictLeak> joanaConflicts = analyzeConflicts();
		if (!joanaConflicts.isEmpty()) {
			debug.outln("conflicts:");
			for (IConflictLeak jc : joanaConflicts) {
				debug.outln(jc);
			}
		}
		
		annManager.unapplyAllAnnotations();
		return ret;
	}
	
	public Set<IConflictLeak> getConflicts() {
		Collection<? extends IConflictLeak> confs = analyzeConflicts();
		Set<IConflictLeak> ret = new HashSet<IConflictLeak>();
		ret.addAll(confs);
		return ret;
	}

	private Collection<? extends IConflictLeak> analyzeConflicts() {
		if (getIFC() instanceof ProbabilisticNIChecker) {
			ProbabilisticNIChecker probIFC = (ProbabilisticNIChecker) getIFC();
			ConflictScanner probSlicer = probIFC.getProbSlicer();
			return probSlicer.getAllConflicts();
		} else {
			return Collections.emptySet();
		}
	}

	private static Map<SDGNode, Collection<SDGNode>> groupByProc(SDG sdg, Collection<SDGNode> nodes) {
		Map<SDGNode, Collection<SDGNode>> ret = new HashMap<SDGNode, Collection<SDGNode>>();
		for (SDGNode n : nodes) {
			SDGNode nProc = sdg.getEntry(n);
			Collection<SDGNode> procColl;
			if (ret.containsKey(nProc)) {
				procColl = ret.get(nProc);
			} else {
				procColl = new LinkedList<SDGNode>();
				ret.put(nProc, procColl);
			}
			procColl.add(n);
		}
		return ret;
	}

	public Collection<IllicitFlow> doIFC() {
		return doIFC(IFCType.POSSIBILISTIC);
	}

	public boolean isAnnotated(SDGProgramPart part) {
		return annManager.isAnnotated(part);
	}

	public void clearAllAnnotationsOfMethodPart(SDGProgramPart toClear) {
		annManager.removeAnnotation(toClear);
	}

	public SDGProgram getProgram() {
		return program;
	}

	public void setSecurityLattice(IEditableLattice<String> l0) {
		this.secLattice = l0;
	}

	public IStaticLattice<String> getSecurityLattice() {
		return secLattice;
	}

	private void addSourceAnnotation(SDGProgramPart toMark, String level, SDGMethod context) {
		addAnnotation(new IFCAnnotation(Type.SOURCE, level, toMark, context));
	}

	private void addSinkAnnotation(SDGProgramPart toMark, String level, SDGMethod context) {
		addAnnotation(new IFCAnnotation(Type.SINK, level, toMark, context));
	}

	public void addDeclassification(SDGProgramPart toMark, String level1, String level2) {
		addAnnotation(new IFCAnnotation(level1, level2, toMark));
	}

	public void addSourceAnnotation(SDGProgramPart toMark, String level) {
		addSourceAnnotation(toMark, level, null);
	}

	public void addSinkAnnotation(SDGProgramPart toMark, String level) {
		addSinkAnnotation(toMark, level, null);
	}

	public boolean isAnnotationLegal(IFCAnnotation ann) {
		return annManager.isAnnotationLegal(ann);
	}

	public SDGProgramPart getProgramPart(String ppartDesc) {
		return program.getPart(ppartDesc);
	}
	
	/**
	 * If Java Source annotations are available, add corresponding IFC Annotations to the Analysis.
	 */
	public void addAllJavaSourceAnnotations() {
		final TypeReference source = TypeReference.findOrCreate(
		      ClassLoaderReference.Application,
		      TypeName.findOrCreate(JavaType.parseSingleTypeFromString(Source.class.getCanonicalName()).toBCString(false)));
		final TypeReference sink = TypeReference.findOrCreate(
			      ClassLoaderReference.Application,
			      TypeName.findOrCreate(JavaType.parseSingleTypeFromString(Sink.class.getCanonicalName()).toBCString(false)));

		final TypeName level = TypeName.findOrCreate(JavaType.parseSingleTypeFromString(Level.class.getCanonicalName()).toBCString());
		final Map<SDGProgramPart,Collection<Annotation>> annotations = program.getJavaSourceAnnotations();
		
		
		
		for (Entry<SDGProgramPart,Collection<Annotation>> e : annotations.entrySet()) {
			for(Annotation a : e.getValue()) {
				debug.outln("Processing::: " + a);
				if(source.equals(a.getType()) || sink.equals(a.getType())) {
					final ElementValue elemvalue = a.getNamedArguments().get("value");

					// As per @Sink / @Source Definition: "value" is an Enum .. 
					assert (elemvalue != null && elemvalue instanceof EnumElementValue);  
					final EnumElementValue enumvalue = (EnumElementValue) elemvalue;
					// .. of Type Level 
					assert (level.equals(TypeName.findOrCreate(enumvalue.enumType)));
					final Level l = Level.valueOf(enumvalue.enumVal);
					assert l!=null;
					
					// TODO: instead of two "Typeswitches" (over latticelevel and a.getType()), do something nicer.
					final String latticeLevel;
					switch (l) {
						case HIGH: latticeLevel = BuiltinLattices.STD_SECLEVEL_HIGH; break;
						case LOW:  latticeLevel = BuiltinLattices.STD_SECLEVEL_LOW; break;
						default: latticeLevel = null; throw new IllegalArgumentException("Unknown Security-Level:" + l);
					}
					if (source.equals(a.getType())) addSourceAnnotation(e.getKey(), latticeLevel); 
					if (sink.equals(a.getType()))   addSinkAnnotation(e.getKey(), latticeLevel);
					
					debug.outln("Added " + a.getType().getName().getClassName() + " Annotation: " + e.getKey() + ":::" + latticeLevel);
				}
			}
		}
	}
}
