/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.api;

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import edu.kit.joana.api.annotations.IFCAnnotation;
import edu.kit.joana.api.annotations.IFCAnnotation.Type;
import edu.kit.joana.api.annotations.IFCAnnotationManager;
import edu.kit.joana.api.lattice.BuiltinLattices;
import edu.kit.joana.api.sdg.SDGMethod;
import edu.kit.joana.api.sdg.SDGProgram;
import edu.kit.joana.api.sdg.SDGProgramPart;
import edu.kit.joana.api.sdg.SDGProgramPartWriter;
import edu.kit.joana.ifc.sdg.core.IFC;
import edu.kit.joana.ifc.sdg.core.conc.PossibilisticNIChecker;
import edu.kit.joana.ifc.sdg.core.conc.ProbabilisticNIChecker;
import edu.kit.joana.ifc.sdg.core.conc.TimeSensitiveIFCDecorator;
import edu.kit.joana.ifc.sdg.core.violations.Violation;
import edu.kit.joana.ifc.sdg.graph.SDG;
import edu.kit.joana.ifc.sdg.graph.SDGNode;
import edu.kit.joana.ifc.sdg.graph.chopper.RepsRosayChopper;
import edu.kit.joana.ifc.sdg.graph.slicer.graph.threads.MHPAnalysis;
import edu.kit.joana.ifc.sdg.graph.slicer.graph.threads.PreciseMHPAnalysis;
import edu.kit.joana.ifc.sdg.graph.slicer.graph.threads.SimpleMHPAnalysis;
import edu.kit.joana.ifc.sdg.lattice.IEditableLattice;
import edu.kit.joana.ifc.sdg.lattice.IStaticLattice;
import edu.kit.joana.ifc.sdg.lattice.LatticeUtil;
import edu.kit.joana.ifc.sdg.lattice.WrongLatticeDefinitionException;
import edu.kit.joana.util.Log;
import edu.kit.joana.util.Logger;

public class IFCAnalysis {
	
	private SDGProgram program;
	private IFCAnnotationManager annManager;
	private IStaticLattice<String> secLattice;
	private IFCType ifcType = IFCType.POSSIBILISTIC;
	private IFC ifc;
	private boolean timeSensitiveAnalysis = false;
	
	public static final IStaticLattice<String> stdLattice;
	
	private static Logger debug = Log.getLogger("api.debug");

	static {
		try {
			stdLattice = LatticeUtil.loadLattice(BuiltinLattices.STD_SECLEVEL_LOW + "<=" + BuiltinLattices.STD_SECLEVEL_HIGH);
		} catch (WrongLatticeDefinitionException e) {
			throw new IllegalStateException();
		}
	}

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
		switch(this.ifcType) {
		case POSSIBILISTIC:
			this.ifc = new PossibilisticNIChecker(this.program.getSDG(), secLattice);
			if (timeSensitiveAnalysis) {
				this.ifc = new TimeSensitiveIFCDecorator(this.ifc);
			}
			break;
		case PROBABILISTIC_WITH_SIMPLE_MHP:
			MHPAnalysis mhpSimple = SimpleMHPAnalysis.analyze(this.program.getSDG());
			this.ifc = new ProbabilisticNIChecker(this.program.getSDG(), secLattice, mhpSimple, this.timeSensitiveAnalysis);
			break;
		case PROBABILISTIC_WITH_PRECISE_MHP:
			MHPAnalysis mhpPrecise = PreciseMHPAnalysis.analyze(this.program.getSDG());
			this.ifc = new ProbabilisticNIChecker(this.program.getSDG(), secLattice, mhpPrecise, this.timeSensitiveAnalysis);
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
		// out.log("Running sequential IFC...");
		time = System.currentTimeMillis();
		Collection<Violation> vios = ifc.checkIFlow();
		time = System.currentTimeMillis() - time;


		List<IllicitFlow> ret = new LinkedList<IllicitFlow>();
		
		Collection<SDGProgramPart> allParts = getProgram().getAllProgramParts();
		for (SDGProgramPart ppart : allParts) {
			debug.outln("Program part " + ppart + " with node(s): " + ppart.getAttachedNodes());
		}
		
		
		for (Violation vio : vios) {
			IllicitFlow ill = new IllicitFlow(vio, allParts);
			ret.add(ill);
			RepsRosayChopper c = new RepsRosayChopper(program.getSDG());
			SDGProgramPart illSrc = ill.getSource();
			SDGProgramPart illSnk = ill.getSink();
			if (illSrc != null && illSnk != null) {
				Collection<SDGNode> chop = c.chop(illSrc.getAttachedNodes(), illSnk.getAttachedNodes());
				debug.outln("Illicit flow with the following nodes involved: ");
				Map<SDGNode, Collection<SDGNode>> groupedByProc = groupByProc(program.getSDG(), chop);
				for (SDGNode nProc : groupedByProc.keySet()) {
					debug.outln("In method " + nProc.getBytecodeMethod() + ": " + groupedByProc.get(nProc));
				}
			} else {
				Violation v = ill.getViolation();
				debug.outln("unidentifiable flow from " + v.getSource() + " to " + v.getSink());
			}
			
		}
		
		
		
		annManager.unapplyAllAnnotations();
		return ret;
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
}
