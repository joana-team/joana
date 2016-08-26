/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.api;

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;

import com.ibm.wala.shrikeCT.AnnotationsReader.ElementValue;
import com.ibm.wala.shrikeCT.AnnotationsReader.EnumElementValue;
import com.ibm.wala.types.ClassLoaderReference;
import com.ibm.wala.types.TypeName;
import com.ibm.wala.types.TypeReference;
import com.ibm.wala.types.annotations.Annotation;

import edu.kit.joana.api.annotations.AnnotationType;
import edu.kit.joana.api.annotations.IFCAnnotation;
import edu.kit.joana.api.annotations.IFCAnnotationManager;
import edu.kit.joana.api.annotations.NodeAnnotationInfo;
import edu.kit.joana.api.lattice.BuiltinLattices;
import edu.kit.joana.api.sdg.SDGActualParameter;
import edu.kit.joana.api.sdg.SDGAttribute;
import edu.kit.joana.api.sdg.SDGCall;
import edu.kit.joana.api.sdg.SDGCallReturnNode;
import edu.kit.joana.api.sdg.SDGFieldOfParameter;
import edu.kit.joana.api.sdg.SDGMethod;
import edu.kit.joana.api.sdg.SDGProgram;
import edu.kit.joana.api.sdg.SDGProgramPart;
import edu.kit.joana.api.sdg.SDGProgramPartWriter;
import edu.kit.joana.api.sdg.ThrowingSDGProgramPartVisitor;
import edu.kit.joana.ifc.sdg.core.IFC;
import edu.kit.joana.ifc.sdg.core.ReduceRedundantFlows;
import edu.kit.joana.ifc.sdg.core.SecurityNode;
import edu.kit.joana.ifc.sdg.core.SlicingBasedIFC;
import edu.kit.joana.ifc.sdg.core.conc.ConflictScanner;
import edu.kit.joana.ifc.sdg.core.conc.DataConflict;
import edu.kit.joana.ifc.sdg.core.conc.LSODNISlicer;
import edu.kit.joana.ifc.sdg.core.conc.OrderConflict;
import edu.kit.joana.ifc.sdg.core.conc.PossibilisticNIChecker;
import edu.kit.joana.ifc.sdg.core.conc.ProbabilisticNIChecker;
import edu.kit.joana.ifc.sdg.core.conc.TimeSensitiveIFCDecorator;
import edu.kit.joana.ifc.sdg.core.violations.ConflictEdge;
import edu.kit.joana.ifc.sdg.core.violations.IIllegalFlow;
import edu.kit.joana.ifc.sdg.core.violations.IViolation;
import edu.kit.joana.ifc.sdg.core.violations.IllegalFlow;
import edu.kit.joana.ifc.sdg.core.violations.ViolationMapper;
import edu.kit.joana.ifc.sdg.graph.SDG;
import edu.kit.joana.ifc.sdg.graph.slicer.conc.I2PBackward;
import edu.kit.joana.ifc.sdg.graph.slicer.conc.I2PForward;
import edu.kit.joana.ifc.sdg.graph.slicer.graph.threads.MHPAnalysis;
import edu.kit.joana.ifc.sdg.graph.slicer.graph.threads.PreciseMHPAnalysis;
import edu.kit.joana.ifc.sdg.graph.slicer.graph.threads.SimpleMHPAnalysis;
import edu.kit.joana.ifc.sdg.irlsod.OptORLSODChecker;
import edu.kit.joana.ifc.sdg.irlsod.ProbInfComputer;
import edu.kit.joana.ifc.sdg.irlsod.ThreadModularCDomOracle;
import edu.kit.joana.ifc.sdg.irlsod.TimingClassificationChecker;
import edu.kit.joana.ifc.sdg.lattice.IStaticLattice;
import edu.kit.joana.ifc.sdg.mhpoptimization.CSDGPreprocessor;
import edu.kit.joana.ifc.sdg.mhpoptimization.MHPType;
import edu.kit.joana.ifc.sdg.util.JavaMethodSignature;
import edu.kit.joana.ifc.sdg.util.JavaType;
import edu.kit.joana.ui.annotations.AnnotationPolicy;
import edu.kit.joana.ui.annotations.Level;
import edu.kit.joana.ui.annotations.Sink;
import edu.kit.joana.ui.annotations.Source;
import edu.kit.joana.util.Log;
import edu.kit.joana.util.Logger;
import edu.kit.joana.util.Maybe;
import gnu.trove.map.TObjectIntMap;
import gnu.trove.map.hash.TObjectIntHashMap;

public class IFCAnalysis {

	private SDGProgram program;
	private IFCAnnotationManager annManager;
	private IStaticLattice<String> secLattice;
	private IFCType ifcType = IFCType.CLASSICAL_NI;
	private IFC<String> ifc;
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
		setIFCType(IFCType.CLASSICAL_NI, null);
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

	private void setIFCType(IFCType ifcType, MHPType mhpType) {
		this.ifcType = ifcType;
		MHPAnalysis mhp;
		switch (this.ifcType) {
		case CLASSICAL_NI:
			this.ifc = new SlicingBasedIFC(this.program.getSDG(), secLattice, new I2PForward(this.program.getSDG()), new I2PBackward(this.program.getSDG()));
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
		case LSOD:
			if (this.program.getSDG().getThreadsInfo() == null) {
				CSDGPreprocessor.preprocessSDG(this.program.getSDG());
			}
			mhp = performMHPAnalysis(mhpType);
			ConflictScanner lsodScanner = LSODNISlicer.simpleCheck(this.program.getSDG(), secLattice, mhp,
			this.timeSensitiveAnalysis);
			this.ifc = new ProbabilisticNIChecker(this.program.getSDG(), secLattice, lsodScanner,
					mhp, this.timeSensitiveAnalysis);
			break;
		case RLSOD:
			if (this.program.getSDG().getThreadsInfo() == null) {
				CSDGPreprocessor.preprocessSDG(this.program.getSDG());
			}
			mhp = performMHPAnalysis(mhpType);
			this.ifc = new ProbabilisticNIChecker(this.program.getSDG(), secLattice, mhp,
					this.timeSensitiveAnalysis);
			break;
		case iRLSOD: {
			if (this.program.getSDG().getThreadsInfo() == null) {
				CSDGPreprocessor.preprocessSDG(this.program.getSDG());
			}
			mhp = performMHPAnalysis(mhpType);
			final SDG sdg = this.program.getSDG();
			final ThreadModularCDomOracle tmdo = new ThreadModularCDomOracle(sdg);
			final ProbInfComputer probInf = new ProbInfComputer(sdg, tmdo);
//			this.ifc = new ORLSODChecker<String>(sdg, secLattice, probInf, null);
			this.ifc = new OptORLSODChecker<String>(sdg, secLattice, probInf);
//			this.ifc = new PathBasedORLSODChecker<String>(sdg, secLattice, probInf);
//			this.ifc = new BetterORLSODChecker<String>(sdg, secLattice, probInf);
			break;
		}
		case timingiRLSOD: {
			if (this.program.getSDG().getThreadsInfo() == null) {
				CSDGPreprocessor.preprocessSDG(this.program.getSDG());
			}
			mhp = performMHPAnalysis(mhpType);
			final SDG sdg = this.program.getSDG();
			final ThreadModularCDomOracle tmdo = new ThreadModularCDomOracle(sdg);
			this.ifc = new TimingClassificationChecker<String>(sdg, secLattice, mhp, tmdo);
			break;
		}
		default:
			throw new IllegalStateException("unhandled ifc type: " + ifcType + "!");
		}
	}

	private MHPAnalysis performMHPAnalysis(MHPType mhpType) {
		if (mhpType == MHPType.SIMPLE) {
			return SimpleMHPAnalysis.analyze(this.program.getSDG());
		} else {
			return PreciseMHPAnalysis.analyze(this.program.getSDG());
		}
	}

	public IFC<String> getIFC() {
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

	public Map<SecurityNode, NodeAnnotationInfo> getAnnotatedNodes() {
		annManager.applyAllAnnotations();
		Map<SecurityNode, NodeAnnotationInfo> ret = annManager.getAnnotatedNodes();
		annManager.unapplyAllAnnotations();
		return ret;
	}
	public Collection<IFCAnnotation> getDeclassifications() {
		return annManager.getDeclassifications();
	}

	public void addAnnotation(IFCAnnotation annotation) {
		String ppDesc = SDGProgramPartWriter.getStandardVersion().writeSDGProgramPart(annotation.getProgramPart());
		Collection<SDGProgramPart> parts = new HashSet<SDGProgramPart>();
		Collection<? extends SDGProgramPart> allParts = program.getParts(ppDesc);
		parts.add(annotation.getProgramPart());
		if (allParts != null) {
			parts.addAll(allParts);
		}
		for (SDGProgramPart part : parts) {
			annManager.addAnnotation(annotation.transferTo(part));
		}
	}

	public Collection<IFCAnnotation> getAnnotations() {
		return annManager.getAnnotations();
	}

	/**
	 * Do IFC analysis of the specified type and uses precise mhp analysis.
	 * @param ifcType type of IFC analysis to perform
	 * @return collection of security violations reported by the specified ifc analysis
	 */
	public Collection<? extends IViolation<SecurityNode>> doIFC(IFCType ifcType) {
		return doIFC(ifcType, MHPType.PRECISE);
	}

	/**
	 * Do IFC analysis of the specified type and use the specified MHP analysis precision, if relevant.
	 * Note that mhpType is assumed to be not {@code null}, if ifcType is {@link IFCType#LSOD} or {@link IFCType#RLSOD}. If ifcType is
	 * {@link IFCType#CLASSICAL_NI}, then mhpType is ignored and thus also allowed to be {@code null}.
	 * @param ifcType type of IFC analysis to perform
	 * @param mhpType precision of the MHP analysis to use - must be non-null if ifcType is {@link IFCType#LSOD} or {@link IFCType#RLSOD}; may be {@code null} otherwise
	 * @return collection of security violations reported by the specified ifc analysis
	 */
	public Collection<? extends IViolation<SecurityNode>> doIFC(IFCType ifcType, MHPType mhpType) {
		assert ifc != null && ifc.getSDG() != null && ifc.getLattice() != null;
		annManager.applyAllAnnotations();
		setIFCType(ifcType, mhpType);
		long time = 0L;
		time = System.currentTimeMillis();
		Collection<? extends IViolation<SecurityNode>> vios = ifc.checkIFlow();
		time = System.currentTimeMillis() - time;
		debug.outln(String.format("IFC Analysis took %d ms.", time));
		annManager.unapplyAllAnnotations();
		return vios;
	}

	public TObjectIntMap<? extends IViolation<SDGProgramPart>> doIFCAndGroupByPPPart(IFCType ifcType) {
		return groupByPPPart(doIFC(ifcType));
	}
	
	public TObjectIntMap<IViolation<SDGProgramPart>> groupByPPPart(Collection<? extends IViolation<SecurityNode>> vios) {
		annManager.applyAllAnnotations();
		ViolationMapper<SecurityNode, IViolation<SDGProgramPart>> transl = new ViolationMapper<SecurityNode, IViolation<SDGProgramPart>>() {

			@Override
			protected IIllegalFlow<SDGProgramPart> mapIllegalFlow(IIllegalFlow<SecurityNode> iFlow) {
				return new IllegalFlow<SDGProgramPart>(annManager.resolve(iFlow.getSource()), annManager.resolve(iFlow.getSink()), iFlow.getAttackerLevel());
			}

			@Override
			protected DataConflict<SDGProgramPart> mapDataConflict(DataConflict<SecurityNode> dc) {
				SDGProgramPart ppInfluenced = annManager.resolve(dc.getInfluenced());
				return new DataConflict<SDGProgramPart>(resolveConflictEdge(dc.getConflictEdge()), ppInfluenced, dc.getAttackerLevel(), resolveTrigger(dc.getTrigger()));
			}
			
			private Maybe<SDGProgramPart> resolveTrigger(Maybe<SecurityNode> trigger) {
				if (trigger.isNothing()) {
					return Maybe.<SDGProgramPart>nothing();
				} else {
					return Maybe.just(annManager.resolve(trigger.extract()));
				}
			}
			
			private ConflictEdge<SDGProgramPart> resolveConflictEdge(ConflictEdge<SecurityNode> confEdge) {
				SDGProgramPart src = annManager.resolve(confEdge.getSource());
				SDGProgramPart tgt = annManager.resolve(confEdge.getTarget());
				return new ConflictEdge<SDGProgramPart>(src, tgt);
			}

			@Override
			protected OrderConflict<SDGProgramPart> mapOrderConflict(OrderConflict<SecurityNode> oc) {
				return new OrderConflict<SDGProgramPart>(resolveConflictEdge(oc.getConflictEdge()), oc.getAttackerLevel(), resolveTrigger(oc.getTrigger()));
			}
			
		};
		final TObjectIntMap<IViolation<SDGProgramPart>> ret = new TObjectIntHashMap<IViolation<SDGProgramPart>>();
		for (IViolation<SecurityNode> vio : vios) {
			IViolation<SDGProgramPart> key = transl.mapSingle(vio);
			if (!ret.containsKey(key)) {
				ret.put(key, 1);
			} else {
				ret.put(key, ret.get(key) + 1);
			}
		}
		annManager.unapplyAllAnnotations();
		return ret;
	}
	

	public Collection<? extends IViolation<SecurityNode>> doIFC() {
		return doIFC(IFCType.CLASSICAL_NI);
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

	public IStaticLattice<String> getLattice() {
		return secLattice;
	}

	private void addSourceAnnotation(SDGProgramPart toMark, String level, SDGMethod context) {
		addAnnotation(new IFCAnnotation(AnnotationType.SOURCE, level, toMark, context));
	}

	private void addSinkAnnotation(SDGProgramPart toMark, String level, SDGMethod context) {
		addAnnotation(new IFCAnnotation(AnnotationType.SINK, level, toMark, context));
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

	@Deprecated
	public void addSourceAnnotationsToCallers(JavaMethodSignature signature, String level) {
		final Collection<SDGCall> calls = program.getCallsToMethod(signature);
		for (final SDGCall call : calls) {
			final SDGCallReturnNode ret = call.getReturn();
			addSourceAnnotation(ret, level);
		}
	}
	
	public void addSourceAnnotation(SDGMethod methodToMark, String level, AnnotationPolicy annotationPolicy) {
		switch (annotationPolicy) {
			// TODO: AFAICT, relying on the methods Signature to obtain all calls to this method,
			// different SDGMethod instances of the same Method (with the same signature),
			// created e.g. due to call-string sensitive points-to analysis,
			// will get lumped together here again.
			case ANNOTATE_USAGES: addSourceAnnotationsToCallers(methodToMark.getSignature(), level);	break;
			case ANNOTATE_CALLEE:  addSourceAnnotation(methodToMark, level); break;
			default: throw new IllegalArgumentException("Unknown AnnotationPolicy: " + annotationPolicy);
		}
	}
	
	@Deprecated
	public void addSinkAnnotationsToActualsAtCallsites(JavaMethodSignature signature, String level) {
		final Collection<SDGCall> calls = program.getCallsToMethod(signature);
		for (final SDGCall call : calls) {
			final Collection<SDGActualParameter> params = call.getActualParameters();
			for (final SDGActualParameter aIn : params) {
				//if (aIn.getIndex() != 1) throw new IllegalArgumentException("rofl");
				addSinkAnnotation(aIn, level);
			}
		}
	}
	
	public void addSinkAnnotation(SDGMethod methodToMark, String level, AnnotationPolicy annotationPolicy) {
		switch (annotationPolicy) {
			case ANNOTATE_USAGES: addSinkAnnotationsToActualsAtCallsites(methodToMark.getSignature(), level); break;
			case ANNOTATE_CALLEE:  addSinkAnnotation(methodToMark, level); break;
			default: throw new IllegalArgumentException("Unknown AnnotationPolicy: " + annotationPolicy);
		}
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
		final TypeName annotationPolicy = TypeName.findOrCreate(JavaType.parseSingleTypeFromString(AnnotationPolicy.class.getCanonicalName()).toBCString());
		final Map<SDGProgramPart,Collection<Annotation>> annotations = program.getJavaSourceAnnotations();
		
		for (final Entry<SDGProgramPart,Collection<Annotation>> e : annotations.entrySet()) {
			for(final Annotation a : e.getValue()) {
				debug.outln("Processing::: " + a);
				if(source.equals(a.getType()) || sink.equals(a.getType())) {
					final ElementValue elemvalue = a.getNamedArguments().get("level");
					final Level l;
					if (elemvalue == null) {
						// try to get default value
						Level temp = null;
						try {
							if (source.equals(a.getType())) {
								temp = (Level) Source.class.getMethod("level").getDefaultValue();
							} else {
								temp = (Level) Sink.class.getMethod("level").getDefaultValue();
							}
						} catch (Exception e1) {
							assert false;
						} finally {
							l = temp;
						}
					} else {
						// As per @Sink / @Source Definition: "value" is an Enum .. 
						assert (elemvalue != null && elemvalue instanceof EnumElementValue); 
						final EnumElementValue enumvalue = (EnumElementValue) elemvalue;
						// .. of Type Level / AnnotationPolicy
						assert (level.equals(TypeName.findOrCreate(enumvalue.enumType)));
						l = Level.valueOf(enumvalue.enumVal);
					}
					assert (l != null);

					// If "annotate" is missing, use the default value 
					final ElementValue elemannotate = a.getNamedArguments().get("annotate");
					final AnnotationPolicy ap; 
					if (elemannotate == null) {
						AnnotationPolicy temp = null;
						try {
							temp = (AnnotationPolicy) Source.class.getMethod("annotate").getDefaultValue();
						} catch (Exception e1) {
							assert false;
						} finally {
							ap = temp;
						}
					} else {
						assert (elemannotate instanceof EnumElementValue);
						final EnumElementValue enumannotate = (EnumElementValue) elemannotate;
						assert (annotationPolicy.equals(TypeName.findOrCreate(enumannotate.enumType)));
						ap = AnnotationPolicy.valueOf(enumannotate.enumVal);						
					}
					assert ap!=null;
					
					// TODO: instead of two "Typeswitches" (over latticelevel and a.getType()), do something nicer.
					final String latticeLevel;
					switch (l) {
						case HIGH: latticeLevel = BuiltinLattices.STD_SECLEVEL_HIGH; break;
						case LOW:  latticeLevel = BuiltinLattices.STD_SECLEVEL_LOW; break;
						default: latticeLevel = null; throw new IllegalArgumentException("Unknown Security-Level:" + l);
					}
					
					
					e.getKey().acceptVisitor(new ThrowingSDGProgramPartVisitor<Void, Void>() {
						@Override
						protected Void visitMethod(SDGMethod m, Void data) {
							if (source.equals(a.getType())) addSourceAnnotation(m, latticeLevel,ap); 
							if (sink.equals(a.getType()))   addSinkAnnotation(m, latticeLevel,ap);
							return null;
						}
						
						@Override
						protected Void visitAttribute(SDGAttribute attribute, Void data) {
							if (ap != AnnotationPolicy.ANNOTATE_USAGES) {
								throw new IllegalArgumentException("Fields may onlye be annotated with annotate == " + 
							                                        AnnotationPolicy.ANNOTATE_USAGES);
							}
							if (source.equals(a.getType())) addSourceAnnotation(attribute, latticeLevel); 
							if (sink.equals(a.getType()))   addSinkAnnotation(attribute, latticeLevel);
							return null;
						}
					}, null);
					
					debug.outln("Added " + a.getType().getName().getClassName() + " Annotation: " + e.getKey() + ":::" + latticeLevel);
				}
			}
		}
	}
}
