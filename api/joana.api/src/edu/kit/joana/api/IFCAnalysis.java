/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.api;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;
import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.ipa.cha.ClassHierarchy;
import com.ibm.wala.ipa.cha.IClassHierarchy;
import com.ibm.wala.shrikeCT.AnnotationsReader.AnnotationAttribute;
import com.ibm.wala.shrikeCT.AnnotationsReader.ArrayElementValue;
import com.ibm.wala.shrikeCT.AnnotationsReader.ConstantElementValue;
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
import edu.kit.joana.api.annotations.cause.AnnotationCause;
import edu.kit.joana.api.annotations.cause.JavaSinkAnnotation;
import edu.kit.joana.api.annotations.cause.JavaSourceAnnotation;
import edu.kit.joana.api.annotations.cause.UnknownCause;
import edu.kit.joana.api.lattice.BuiltinLattices;
import edu.kit.joana.api.sdg.*;
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
import edu.kit.joana.ifc.sdg.graph.SDGNode;
import edu.kit.joana.ifc.sdg.graph.slicer.conc.I2PBackward;
import edu.kit.joana.ifc.sdg.graph.slicer.conc.I2PForward;
import edu.kit.joana.ifc.sdg.graph.slicer.graph.threads.MHPAnalysis;
import edu.kit.joana.ifc.sdg.irlsod.PredProbInfComputer;
import edu.kit.joana.ifc.sdg.irlsod.OptORLSODChecker;
import edu.kit.joana.ifc.sdg.irlsod.ProbInfComputer;
import edu.kit.joana.ifc.sdg.irlsod.ThreadModularCDomOracle;
import edu.kit.joana.ifc.sdg.irlsod.TimingClassificationChecker;
import edu.kit.joana.ifc.sdg.lattice.IStaticLattice;
import edu.kit.joana.ifc.sdg.mhpoptimization.CSDGPreprocessor;
import edu.kit.joana.ifc.sdg.util.JavaMethodSignature;
import edu.kit.joana.ifc.sdg.util.JavaType;
import edu.kit.joana.ui.annotations.AnnotationPolicy;
import edu.kit.joana.ui.annotations.Declassification;
import edu.kit.joana.ui.annotations.Declassifications;
import edu.kit.joana.ui.annotations.Level;
import edu.kit.joana.ui.annotations.PositionDefinition;
import edu.kit.joana.ui.annotations.Sink;
import edu.kit.joana.ui.annotations.Sinks;
import edu.kit.joana.ui.annotations.Source;
import edu.kit.joana.ui.annotations.Sources;
import edu.kit.joana.util.Log;
import edu.kit.joana.util.Logger;
import edu.kit.joana.util.Maybe;
import edu.kit.joana.util.Pair;
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
	/**
	 * "" -> match all annotations that have no tags
	 */
	private String sourceSinkAnnotationTag = "";
	private String lastSourceSinkAnnotationTag = "";
	
	private Multimap<SDGProgramPart, Pair<Source,String>> sources = null;
	private Multimap<SDGProgramPart, Pair<Sink,String>> sinks = null;
	private Multimap<SDGProgramPart, Pair<Declassification,String>> declasss = null;

	public static final IStaticLattice<String> stdLattice = BuiltinLattices.getBinaryLattice();

	private static Logger debug = Log.getLogger("api.debug");

	public IFCAnalysis(SDGProgram program, IStaticLattice<String> secLattice) {
		if (program == null || secLattice == null) {
			throw new IllegalArgumentException("Neither program nor security lattice may be null!");
		}
		setProgram(program);
		setLattice(secLattice);
		setIFCType(IFCType.CLASSICAL_NI);
	}

	public IFCAnalysis(SDGProgram program) {
		this(program, stdLattice);
	}

	public void setProgram(SDGProgram program) {
		if (program == null) {
			throw new IllegalArgumentException("program must not be null!");
		}
		this.program = program;
		this.annManager = new IFCAnnotationManager(program, this);
		if (this.ifc != null) {
			this.ifc.setSDG(this.program.getSDG());
		} else {
			this.ifc = new PossibilisticNIChecker(this.program.getSDG(), secLattice);
		}
	}

	private void setIFCType(IFCType ifcType) {
		this.ifcType = ifcType;
		final MHPAnalysis mhp = program.getMhpAnalysis();
		switch (this.ifcType) {
		case CLASSICAL_NI: break;
		case LSOD:
		case RLSOD:
		case iRLSOD:
		case timingiRLSOD:
			if (mhp == null) {
				throw new IllegalStateException("Cannot run " + ifcType + "for SDGProram without available MHPAnalysis");
			}
			if (this.program.getSDG().getThreadsInfo() == null) {
				CSDGPreprocessor.preprocessSDG(this.program.getSDG());
			}
			break;
		default:
			throw new IllegalStateException("unhandled ifc type: " + ifcType + "!");
		}

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
			ConflictScanner lsodScanner = LSODNISlicer.simpleCheck(this.program.getSDG(), secLattice, mhp,
			this.timeSensitiveAnalysis);
			this.ifc = new ProbabilisticNIChecker(this.program.getSDG(), secLattice, lsodScanner,
					mhp, this.timeSensitiveAnalysis);
			break;
		case RLSOD:
			this.ifc = new ProbabilisticNIChecker(this.program.getSDG(), secLattice, mhp,
					this.timeSensitiveAnalysis);
			break;
		case iRLSOD: {
			final SDG sdg = this.program.getSDG();
//			final ThreadModularCDomOracle tmdo = new ThreadModularCDomOracle(sdg);
//			final ProbInfComputer probInf = new CDomProbInfComputer(sdg, tmdo);
			final ProbInfComputer probInf = new PredProbInfComputer(sdg, mhp);
//			this.ifc = new ORLSODChecker<String>(sdg, secLattice, probInf, null);
			this.ifc = new OptORLSODChecker<String>(sdg, secLattice, probInf);
//			this.ifc = new PathBasedORLSODChecker<String>(sdg, secLattice, probInf);
//			this.ifc = new BetterORLSODChecker<String>(sdg, secLattice, probInf);
			break;
		}
		case timingiRLSOD: {
			final SDG sdg = this.program.getSDG();
			final ThreadModularCDomOracle tmdo = new ThreadModularCDomOracle(sdg);
			this.ifc = new TimingClassificationChecker<String>(sdg, secLattice, mhp, tmdo);
			break;
		}
		default:
			throw new IllegalStateException("unhandled ifc type: " + ifcType + "!");
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

	public Map<SecurityNode, Set<NodeAnnotationInfo>> getAnnotatedNodes() {
		annManager.applyAllAnnotations();
		Map<SecurityNode, Set<NodeAnnotationInfo>> ret = annManager.getAnnotatedNodes();
		annManager.unapplyAllAnnotations();
		return ret;
	}

	public Map<SDGNode, String> getNodeLevels() {
		Map<SDGNode, String> result = new HashMap<SDGNode, String>();
		annManager.applyAllAnnotations();
		for (SDGNode n : this.program.getSDG().vertexSet()) {
			SecurityNode sn = (SecurityNode) n;
			if (sn.getProvided() != null && sn.getRequired() == null) {
				result.put(sn, sn.getProvided());
			} else if (sn.getProvided() == null && sn.getRequired() != null) {
				result.put(sn,  sn.getRequired());
			}
		}
		annManager.unapplyAllAnnotations();
		return result;
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
	 * Do IFC analysis of the specified type and use the specified MHP analysis precision, if relevant.
	 * Note that mhpType is assumed to be not {@code null}, if ifcType is {@link IFCType#LSOD} or {@link IFCType#RLSOD}. If ifcType is
	 * {@link IFCType#CLASSICAL_NI}, then mhpType is ignored and thus also allowed to be {@code null}.
	 * @param ifcType type of IFC analysis to perform
	 * @param mhpType precision of the MHP analysis to use - must be non-null if ifcType is {@link IFCType#LSOD} or {@link IFCType#RLSOD}; may be {@code null} otherwise
	 * @return collection of security violations reported by the specified ifc analysis
	 */
	public Collection<? extends IViolation<SecurityNode>> doIFC(IFCType ifcType) {
		assert ifc != null && ifc.getSDG() != null && ifc.getLattice() != null;
		annManager.applyAllAnnotations();
		setIFCType(ifcType);
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
		ViolationMapper<SecurityNode, Set<? extends IViolation<SDGProgramPart>>> transl = new ViolationMapper<SecurityNode, Set<? extends IViolation<SDGProgramPart>>>() {

			@Override
			protected Set<IIllegalFlow<SDGProgramPart>> mapIllegalFlow(IIllegalFlow<SecurityNode> iFlow) {
				LinkedHashSet<IIllegalFlow<SDGProgramPart>> ret = new LinkedHashSet<>();
				for (SDGProgramPart ppSource : resolveNode(iFlow.getSource())) {
					for (SDGProgramPart ppSink : resolveNode(iFlow.getSink())) {
						ret.add(new IllegalFlow<SDGProgramPart>(ppSource, ppSink, iFlow.getAttackerLevel()));
					}
				}
				return ret;
			}

			@Override
			protected Set<DataConflict<SDGProgramPart>> mapDataConflict(DataConflict<SecurityNode> dc) {
				LinkedHashSet<DataConflict<SDGProgramPart>> ret = new LinkedHashSet<>();
				Maybe<Set<SDGProgramPart>> possibleTriggerSet = resolveTrigger(dc.getTrigger());
				for (SDGProgramPart ppInfluenced: resolveNode(dc.getInfluenced())) {
					for (SDGProgramPart src: resolveNode(dc.getConflictEdge().getSource())) {
						for (SDGProgramPart tgt : resolveNode(dc.getConflictEdge().getTarget())) {
							if (possibleTriggerSet.isNothing()) {
								ret.add(new DataConflict<SDGProgramPart>(new ConflictEdge<SDGProgramPart>(src, tgt), ppInfluenced, dc.getAttackerLevel(), Maybe.nothing()));
							} else {
								for (SDGProgramPart ppTrigger : possibleTriggerSet.extract()) {
									ret.add(new DataConflict<SDGProgramPart>(new ConflictEdge<SDGProgramPart>(src, tgt), ppInfluenced, dc.getAttackerLevel(), Maybe.just(ppTrigger)));
								}
							}
						}
					}

				}
				return ret;
			}
			//TODO: should this yield Set<Maybe<...>> instead of Maybe<Set<...>>?
			private Maybe<Set<SDGProgramPart>> resolveTrigger(Maybe<SecurityNode> trigger) {
				if (trigger.isNothing()) {
					return Maybe.<Set<SDGProgramPart>>nothing();
				} else {
					return Maybe.just(resolveNode(trigger.extract()));
				}
			}

			@Override
			protected Set<OrderConflict<SDGProgramPart>> mapOrderConflict(OrderConflict<SecurityNode> oc) {
				LinkedHashSet<OrderConflict<SDGProgramPart>> ret = new LinkedHashSet<>();
				Maybe<Set<SDGProgramPart>> possibleTriggerSet = resolveTrigger(oc.getTrigger());
				for (SDGProgramPart ppSrc : resolveNode(oc.getConflictEdge().getSource())) {
					for (SDGProgramPart ppTgt : resolveNode(oc.getConflictEdge().getTarget())) {
						if (possibleTriggerSet.isNothing()) {
							ret.add(new OrderConflict<SDGProgramPart>(new ConflictEdge<SDGProgramPart>(ppSrc, ppTgt), oc.getAttackerLevel(), Maybe.nothing()));
						} else {
							for (SDGProgramPart triggerPP : possibleTriggerSet.extract()) {
								ret.add(new OrderConflict<SDGProgramPart>(new ConflictEdge<SDGProgramPart>(ppSrc, ppTgt), oc.getAttackerLevel(), Maybe.just(triggerPP)));
							}
						}
					}
				}
				return ret;
			}

			private Set<SDGProgramPart> resolveNode(SecurityNode node) {
				if (annManager.getAnnotatedNodes().get(node) == null) {
					return Collections.emptySet();
				} else {
					LinkedHashSet<SDGProgramPart> result = new LinkedHashSet<SDGProgramPart>();
					for (NodeAnnotationInfo nai : annManager.getAnnotatedNodes().get(node)) {
						result.add(nai.getAnnotation().getProgramPart());
					}
					return result;
				}
			}
		};
		final TObjectIntMap<IViolation<SDGProgramPart>> ret = new TObjectIntHashMap<IViolation<SDGProgramPart>>();
		for (IViolation<SecurityNode> vio : vios) {
			Set<? extends IViolation<SDGProgramPart>> keys = transl.mapSingle(vio);
			for (IViolation<SDGProgramPart> key : keys) {
				if (!ret.containsKey(key)) {
					ret.put(key, 1);
				} else {
					ret.put(key, ret.get(key) + 1);
				}
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
	
	/**
	 * @return the sourceSinkAnnotationTag
	 */
	public String getSourceSinkAnnotationTag() {
		return sourceSinkAnnotationTag;
	}

	/**
	 * @param sourceSinkAnnotationTag the sourceSinkAnnotationTag to set
	 */
	public void setSourceSinkAnnotationTag(String sourceSinkAnnotationTag) {
		this.sourceSinkAnnotationTag = sourceSinkAnnotationTag;
	}

	private void addSourceAnnotation(SDGProgramPart toMark, String level, SDGMethod context, AnnotationCause cause) {
		addAnnotation(new IFCAnnotation(AnnotationType.SOURCE, level, toMark, context, cause));
	}

	private void addSinkAnnotation(SDGProgramPart toMark, String level, SDGMethod context, AnnotationCause cause) {
		addAnnotation(new IFCAnnotation(AnnotationType.SINK, level, toMark, context, cause));
	}

	public void addDeclassification(SDGProgramPart toMark, String level1, String level2) {
		addAnnotation(new IFCAnnotation(level1, level2, toMark));
	}

	public void addSourceAnnotation(SDGProgramPart toMark, String level) {
		addSourceAnnotation(toMark, level, null, UnknownCause.INSTANCE);
	}

	public void addSourceAnnotation(SDGProgramPart toMark, String level, AnnotationCause cause) {
		addSourceAnnotation(toMark, level, null, cause);
	}

	public void addSinkAnnotation(SDGProgramPart toMark, String level) {
		addSinkAnnotation(toMark, level, null, UnknownCause.INSTANCE);
	}
	
	public void addSinkAnnotation(SDGProgramPart toMark, String level, AnnotationCause cause) {
		addSinkAnnotation(toMark, level, null, cause);
	}

	@Deprecated
	public void addSourceAnnotationsToCallers(JavaMethodSignature signature, String level, AnnotationCause cause) {
		final Collection<SDGCall> calls = program.getCallsToMethod(signature);
		for (final SDGCall call : calls) {
			final SDGCallReturnNode ret = call.getReturn();
			addSourceAnnotation(ret, level, null, cause);
		}
	}
	
	@Deprecated
	public void addSourceAnnotation(SDGMethod methodToMark, String level, AnnotationPolicy annotationPolicy) {
		addSourceAnnotation(methodToMark, level, annotationPolicy, UnknownCause.INSTANCE);
	}
	
	public void addSourceAnnotation(SDGMethod methodToMark, String level, AnnotationPolicy annotationPolicy, AnnotationCause cause) {
	
		switch (annotationPolicy) {
			// TODO: AFAICT, relying on the methods Signature to obtain all calls to this method,
			// different SDGMethod instances of the same Method (with the same signature),
			// created e.g. due to call-string sensitive points-to analysis,
			// will get lumped together here again.
			case ANNOTATE_USAGES: addSourceAnnotationsToCallers(methodToMark.getSignature(), level, cause);	break;
			case ANNOTATE_CALLEE:  addSourceAnnotation((SDGProgramPart)methodToMark, level, (SDGMethod) null, cause); break;
			default: throw new IllegalArgumentException("Unknown AnnotationPolicy: " + annotationPolicy);
		}
	}
	
	@Deprecated
	public void addSinkAnnotationsToActualsAtCallsites(JavaMethodSignature signature, String level, AnnotationCause cause) {
		final Collection<SDGCall> calls = program.getCallsToMethod(signature);
		for (final SDGCall call : calls) {
			final Collection<SDGActualParameter> params = call.getActualParameters();
			for (final SDGActualParameter aIn : params) {
				//if (aIn.getIndex() != 1) throw new IllegalArgumentException("rofl");
				addSinkAnnotation(aIn, level, cause);
			}
		}
	}
	
	@Deprecated
	public void addSinkAnnotation(SDGMethod methodToMark, String level, AnnotationPolicy annotationPolicy) {
		addSinkAnnotation(methodToMark, level, annotationPolicy, UnknownCause.INSTANCE);
	}
	
	public void addSinkAnnotation(SDGMethod methodToMark, String level, AnnotationPolicy annotationPolicy, AnnotationCause cause) {
		switch (annotationPolicy) {
			case ANNOTATE_USAGES: addSinkAnnotationsToActualsAtCallsites(methodToMark.getSignature(), level, cause); break;
			case ANNOTATE_CALLEE:  addSinkAnnotation((SDGProgramPart)methodToMark, level, (SDGMethod) null, cause); break;
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
	public Pair<Multimap<SDGProgramPart, Pair<Source, String>>, Multimap<SDGProgramPart, Pair<Sink, String>>> addAllJavaSourceAnnotations() {
		return addAllJavaSourceAnnotations(BuiltinLattices.getBinaryLattice());
	}
	
 	/**
	 * If Java Source includes/mayInclude annotations are available, add corresponding IFC Annotations to the Analysis.
	 */
	public Pair<Multimap<SDGProgramPart, Pair<Source, String>>, Multimap<SDGProgramPart, Pair<Sink, String>>> addAllJavaSourceIncludesAnnotations(Map<Set<String>,String> fromSet, IStaticLattice<String> stringEncodedLattice) {
		final Pair<Multimap<SDGProgramPart, Pair<Source, String>>, Multimap<SDGProgramPart, Pair<Sink, String>>> annotations = getJavaSourceAnnotations();
		final Multimap<SDGProgramPart, Pair<Source, String>> sources = annotations.getFirst();
		final Multimap<SDGProgramPart, Pair<Sink, String>> sinks  = annotations.getSecond();

		for (final Entry<SDGProgramPart, Pair<Source, String>> e : sources.entries()) {
			final Source source       = e.getValue().getFirst();
			final String sourceFile   = e.getValue().getSecond();
			final SDGProgramPart part = e.getKey();
			
			if (source.level() != null) {
				debug.out("Source Annotation with level == " + source.level() + " != null found while processing Includes-EntryPoint, ignoring");
			} else if (source.mayKnow() != null) {
				debug.out("Source Annotation with mayKnow == " + source.mayKnow() + " != null found while processing Includes-EntryPoint, ignoring");
			} else if (source.includes() == null) {
				debug.out("Source Annotation with includes == null found while processing Includes-EntryPoint, ignoring");
			} else try {
				final String[] includes = source.includes();
				final AnnotationPolicy annotate =
					(source.annotate() == null) ?
						(AnnotationPolicy) Source.class.getMethod("annotate").getDefaultValue() : source.annotate();

				final String level = fromSet.get(Sets.newHashSet(includes));
				if (!stringEncodedLattice.getElements().contains(level)) {
					throw new IllegalArgumentException("Unknown dataset " + level + " in includes == " + Arrays.toString(includes));
				}
				annotatePart(Source.class, source, part, level, annotate, sourceFile);
			} catch (NoSuchMethodException nsme) {
				throw new AssertionError("Default value for invalid annotation attribute requested");
			}
		}
		
		for (final Entry<SDGProgramPart, Pair<Sink, String>> e : sinks.entries()) {
			final Sink sink           = e.getValue().getFirst();
			final String sourceFile   = e.getValue().getSecond();
			final SDGProgramPart part = e.getKey();
			
			if (sink.level() != null) {
				debug.out("Sink Annotation with level == " + sink.level() + " != null found while processing Includes-EntryPoint, ignoring");
			} else if (sink.seenBy() != null) {
				debug.out("Sink Annotation with seenBy == " + sink.seenBy() + " != null found while processing Includes-EntryPoint, ignoring");
			} else if (sink.mayInclude() == null) {
				debug.out("Sink Annotation with mayInclude == null found while processing mayKnow-Includes, ignoring");
			} else try {
				final String[] mayInclude = sink.mayInclude();
				final AnnotationPolicy annotate =
					(sink.annotate() == null) ?
						(AnnotationPolicy) Source.class.getMethod("annotate").getDefaultValue() : sink.annotate();

				final String level = fromSet.get(Sets.newHashSet(mayInclude));
				if (!stringEncodedLattice.getElements().contains(level)) {
					throw new IllegalArgumentException("Unknown dataset " + level + "in mayInclude == " + Arrays.toString(mayInclude));
				}
				annotatePart(Sink.class, sink, part, level, annotate, sourceFile);
			} catch (NoSuchMethodException nsme) {
				throw new AssertionError("Default value for invalid annotation attribute requested");
			}
		}
		return annotations;
	}

	
	/**
	 * If Java Source mayKnow/seenBy annotations are available, add corresponding IFC Annotations to the Analysis.
	 */
	public Pair<Multimap<SDGProgramPart, Pair<Source, String>>, Multimap<SDGProgramPart, Pair<Sink, String>>> addAllJavaSourceMayKnowAnnotations(Map<Set<String>,String> fromSet, IStaticLattice<String> stringEncodedLattice) {
		final Pair<Multimap<SDGProgramPart, Pair<Source, String>>, Multimap<SDGProgramPart, Pair<Sink, String>>> annotations = getJavaSourceAnnotations();
		final Multimap<SDGProgramPart, Pair<Source, String>> sources = annotations.getFirst();
		final Multimap<SDGProgramPart, Pair<Sink, String>> sinks  = annotations.getSecond();

		for (final Entry<SDGProgramPart, Pair<Source, String>> e : sources.entries()) {
			final Source source       = e.getValue().getFirst();
			final String sourceFile   = e.getValue().getSecond();
			final SDGProgramPart part = e.getKey();
			
			if (source.level() != null) {
				debug.out("Source Annotation with level == " + source.level() + " != null found while processing mayKnow-EntryPoint, ignoring");
			} else if (source.includes() != null) {
				debug.out("Source Annotation with includes == " + source.includes() + " != null found while processing mayKnow-EntryPoint, ignoring");
			} else if (source.mayKnow() == null) {
				debug.out("Source Annotation with mayKnow == null found while processing mayKnow-EntryPoint, ignoring");
			} else try {
				final String[] mayKnow = source.mayKnow();
				final AnnotationPolicy annotate =
					(source.annotate() == null) ?
						(AnnotationPolicy) Source.class.getMethod("annotate").getDefaultValue() : source.annotate();

				final String level = fromSet.get(Sets.newHashSet(mayKnow));
				if (!stringEncodedLattice.getElements().contains(level)) {
					throw new IllegalArgumentException("Unknown dataset in mayKnow == " + Arrays.toString(mayKnow));
				}
				annotatePart(Source.class, source, part, level, annotate, sourceFile);
			} catch (NoSuchMethodException nsme) {
				throw new AssertionError("Default value for invalid annotation attribute requested");
			}
		}
		
		for (final Entry<SDGProgramPart, Pair<Sink, String>> e : sinks.entries()) {
			final Sink sink           = e.getValue().getFirst();
			final String sourceFile   = e.getValue().getSecond();
			final SDGProgramPart part = e.getKey();
			
			if (sink.level() != null) {
				debug.out("Sink Annotation with level == " + sink.level() + " != null found while processing mayKnow-EntryPoint, ignoring");
			} else if (sink.mayInclude() != null) {
				debug.out("Sink Annotation with mayInclude == " + sink.mayInclude() + " != null found while processing mayKnow-EntryPoint, ignoring");
			} else if (sink.seenBy() == null) {
				debug.out("Sink Annotation with seenBy == null found while processing mayKnow-EntryPoint, ignoring");
			} else try {
				final String[] seenBy = sink.seenBy();
				final AnnotationPolicy annotate =
					(sink.annotate() == null) ?
						(AnnotationPolicy) Source.class.getMethod("annotate").getDefaultValue() : sink.annotate();

				final String level = fromSet.get(Sets.newHashSet(seenBy));
				if (!stringEncodedLattice.getElements().contains(level)) {
					throw new IllegalArgumentException("Unknown dataset in mayKnow == " + Arrays.toString(seenBy));
				}
				annotatePart(Sink.class, sink, part, level, annotate, sourceFile);
			} catch (NoSuchMethodException nsme) {
				throw new AssertionError("Default value for invalid annotation attribute requested");
			}
		}
		return annotations;
	}
	
	/**
	 * Return available Source/Sink annotations
	 */
	public Pair<Multimap<SDGProgramPart, Pair<Source,String>>,
                Multimap<SDGProgramPart, Pair<Sink,String>>> getJavaSourceAnnotations() {
		return getJavaSourceAnnotations(true).getFirst();
	}
	
	/**
	 * Return available Source/Sink annotations
	 */
	public Pair<Pair<Multimap<SDGProgramPart, Pair<Source,String>>,
                Multimap<SDGProgramPart, Pair<Sink,String>>>, 
                Multimap<SDGProgramPart, Pair<Declassification,String>>> getJavaSourceAnnotations(boolean ignoreDeclass) {
		if (sources == null || sinks == null || ignoreDeclass != (declasss == null) || !sourceSinkAnnotationTag.equals(lastSourceSinkAnnotationTag)) {
			updateJavaSourceAnnotations(ignoreDeclass);
			lastSourceSinkAnnotationTag = sourceSinkAnnotationTag;
		}
		assert sources != null;
		assert sinks != null;
		assert declasss != null;
		return Pair.pair(Pair.pair(sources, sinks), declasss);
	}

	private void updateJavaSourceAnnotations(boolean ignoreDeclass) {
		this.sources = ArrayListMultimap.create();
		this.sinks  = ArrayListMultimap.create();
		this.declasss = ArrayListMultimap.create();
		final TypeReference source = TypeReference.findOrCreate(
		      ClassLoaderReference.Application,
		      TypeName.findOrCreate(JavaType.parseSingleTypeFromString(Source.class.getCanonicalName()).toBCString(false)));
		final TypeReference sink = TypeReference.findOrCreate(
			      ClassLoaderReference.Application,
			      TypeName.findOrCreate(JavaType.parseSingleTypeFromString(Sink.class.getCanonicalName()).toBCString(false)));
		final TypeReference declass = TypeReference.findOrCreate(
			      ClassLoaderReference.Application,
			      TypeName.findOrCreate(JavaType.parseSingleTypeFromString(Declassification.class.getCanonicalName()).toBCString(false)));
		final TypeReference sourcesType = TypeReference.findOrCreate(
			      ClassLoaderReference.Application,
			      TypeName.findOrCreate(JavaType.parseSingleTypeFromString(Sources.class.getCanonicalName()).toBCString(false)));
		final TypeReference sinksType = TypeReference.findOrCreate(
			      ClassLoaderReference.Application,
			      TypeName.findOrCreate(JavaType.parseSingleTypeFromString(Sinks.class.getCanonicalName()).toBCString(false)));
		final TypeReference declasssType = TypeReference.findOrCreate(
			      ClassLoaderReference.Application,
			      TypeName.findOrCreate(JavaType.parseSingleTypeFromString(Declassifications.class.getCanonicalName()).toBCString(false)));
		final Map<SDGProgramPart,Collection<Pair<Annotation,String>>> annotations = program.getJavaSourceAnnotations();
		final TypeName annotationPolicy = TypeName.findOrCreate(JavaType.parseSingleTypeFromString(AnnotationPolicy.class.getCanonicalName()).toBCString());
		final TypeName positionDefinition = TypeName.findOrCreate(JavaType.parseSingleTypeFromString(AnnotationPolicy.class.getCanonicalName()).toBCString());
		
		for (final Entry<SDGProgramPart,Collection<Pair<Annotation,String>>> e : annotations.entrySet()) {
			for(final Pair<Annotation,String> p : e.getValue()) {
				final Annotation a = p.getFirst();
				final String sourceFile = p.getSecond();
				List<Annotation> subs = new ArrayList<>();
				if (a.getType().equals(sourcesType) || a.getType().equals(sinksType) || a.getType().equals(declasssType)) {
					Object[] subAnnotations = ((ArrayElementValue)a.getNamedArguments().get("value")).vals;
					Arrays.stream(subAnnotations).forEach(ann -> {
						AnnotationAttribute elem = (AnnotationAttribute)ann;
						subs.add(Annotation.makeWithNamed( TypeReference.findOrCreate(
							      ClassLoaderReference.Application, TypeName.findOrCreate(elem.type.toString().replace(";", ""))),
								elem.elementValues));
					});
				} else {
					subs.add(a);
				}
				for (Annotation subA : subs) {
					debug.outln("Processing::: " + subA);
					processSourceSinkDeclassification(subA, annotationPolicy, 
							positionDefinition, source, sink, 
							declass, e.getKey(), sourceFile, ignoreDeclass);
				}
			}
		}
	}
	
	private void processSourceSinkDeclassification(Annotation a, TypeName annotationPolicy, TypeName positionDefinition, 
			TypeReference source, TypeReference sink, TypeReference declass, SDGProgramPart part, String sourceFile,
			boolean ignoreDeclass) {
		if (!checkTags(a)) {
			return;
		}
		if (source.equals(a.getType())) {
			sources.put(part, Pair.pair(processSource(a, positionDefinition, annotationPolicy), sourceFile));
		} else if (sink.equals(a.getType())) {
			sinks.put(part, Pair.pair(processSink(a, annotationPolicy, positionDefinition), sourceFile));
		} else if (declass.equals(a.getType()) && !ignoreDeclass) {
			declasss.put(part, Pair.pair(processDeclassification(a, annotationPolicy, positionDefinition), sourceFile));
		}
	}
	
	private boolean checkTags(Annotation a) {
		if (a.getNamedArguments().containsKey("tags")) {
			if (sourceSinkAnnotationTag.isEmpty()) {
				return ((ArrayElementValue)a.getNamedArguments().get("tags")).vals.length == 0 ||
						Arrays.stream(((ArrayElementValue)a.getNamedArguments().get("tags")).vals)
						.map(e -> (ConstantElementValue)e)
						.anyMatch(e -> e.val.equals(""));
			}
			return Arrays.stream(((ArrayElementValue)a.getNamedArguments().get("tags")).vals)
					.map(e -> (ConstantElementValue)e)
					.anyMatch(e ->  e.val.equals(sourceSinkAnnotationTag));
		}
		return sourceSinkAnnotationTag.isEmpty();
	}

	private Source processSource(Annotation a, TypeName annotationPolicy, TypeName positionDefinition){
		final ElementValue levelValue = a.getNamedArguments().get("level");
		final String level;
		if (levelValue == null) {
			level = null;
		} else {
			// As per @Sink / @Source Definition: "level" is a constant String (such as "LOW" or "HIGH") 
			final ConstantElementValue constantvalue = (ConstantElementValue) levelValue;
			// .. of Type String
			level = (String) constantvalue.val;
		}
		
		final ElementValue annotateValue = a.getNamedArguments().get("annotate");
		final AnnotationPolicy annotate; 
		if (annotateValue == null) {
			annotate = null;
		} else {
			final EnumElementValue enumannotate = (EnumElementValue) annotateValue;
			assert (annotationPolicy.equals(TypeName.findOrCreate(enumannotate.enumType)));
			annotate = AnnotationPolicy.valueOf(enumannotate.enumVal);						
		}
		
		final ElementValue positionDefinitionValue = a.getNamedArguments().get("positionDefinition");
		final PositionDefinition positionDef;
		if (positionDefinitionValue == null) {
			positionDef = null;
		} else {
			final EnumElementValue enumpositiondefinition = (EnumElementValue) positionDefinitionValue;
			assert (positionDefinition.equals(TypeName.findOrCreate(enumpositiondefinition.enumType)));
			positionDef = PositionDefinition.valueOf(enumpositiondefinition.enumVal);
		}
		
		final ElementValue mayKnowValue = a.getNamedArguments().get("mayKnow");
		final String[] mayKnow;
		if (mayKnowValue == null) {
			mayKnow = null;
		} else {
			final ArrayElementValue arrayMayKnow = (ArrayElementValue) mayKnowValue;
			mayKnow = new String[arrayMayKnow.vals.length];
			for (int i = 0; i < arrayMayKnow.vals.length; i++) {
				mayKnow[i] = arrayMayKnow.vals[i].toString();
			}
		}

		final ElementValue includesValue = a.getNamedArguments().get("includes");
		final String[] includes;
		if (includesValue == null) {
			includes = null;
		} else {
			final ArrayElementValue arrayIncludes = (ArrayElementValue) includesValue;
			includes = new String[arrayIncludes.vals.length];
			for (int i = 0; i < arrayIncludes.vals.length; i++) {
				includes[i] = arrayIncludes.vals[i].toString();
			}
		}
		
		final ElementValue lineNumberValue = a.getNamedArguments().get("lineNumber");
		final Integer lineNumber;
		if (lineNumberValue == null) {
			lineNumber = -1;
		} else {
			final ConstantElementValue constantvalue = (ConstantElementValue) lineNumberValue;
			lineNumber = (Integer) constantvalue.val;
		}
		
		final ElementValue columnNumberValue = a.getNamedArguments().get("columnNumber");
		final Integer columnNumber;
		if (columnNumberValue == null) {
			columnNumber = -1;
		} else {
			final ConstantElementValue constantvalue = (ConstantElementValue) columnNumberValue;
			columnNumber = (Integer) constantvalue.val;
		}
		
		final ElementValue idValue = a.getNamedArguments().get("id");
		final String id;
		if (idValue == null) {
			id = null;
		} else {
			// As per @Sink / @Source Definition: "id" is a constant String 
			final ConstantElementValue constantvalue = (ConstantElementValue) idValue;
			// .. of Type String
			id = (String) constantvalue.val;
		}
		final ElementValue tagsValue = a.getNamedArguments().get("tags");
		final String[] tags;
		if (tagsValue == null) {
			tags = new String[0];
		} else {
			tags = Arrays.stream(((ArrayElementValue)a.getNamedArguments().get("tags")).vals)
					.map(es -> (ConstantElementValue)es).map(es -> (String)es.val).toArray(String[]::new);
		}
		return new Source() {
			@Override
			public Class<? extends java.lang.annotation.Annotation> annotationType() {
				return Source.class;
			}
			
			@Override
			public String[] mayKnow() {
				return mayKnow;
			}
			
			@Override
			public String level() {
				return level;
			}
			
			@Override
			public String[] includes() {
				return includes;
			}
			
			@Override
			public AnnotationPolicy annotate() {
				return annotate;
			}
			
			@Override
			public PositionDefinition positionDefinition() {
				return positionDef;
			}
			
			@Override
			public int lineNumber() {
				return lineNumber;
			}
			
			@Override
			public int columnNumber() {
				return columnNumber;
			}
			
			@Override
			public String id() {
				return id;
			}
			
			@Override
			public String[] tags() {
				return tags;
			}
			
			@Override
			public String toString() {
				return "@Source(level = " + level + ", includes = " + Arrays.toString(includes) + ", mayKnow = " + Arrays.toString(mayKnow) + ")";
			};
		};
	}
	
	private Sink processSink(Annotation a, TypeName annotationPolicy, TypeName positionDefinition){
		final ElementValue levelValue = a.getNamedArguments().get("level");
		final String level;
		if (levelValue == null) {
			level = null;
		} else {
			// As per @Sink / @Source Definition: "level" is a constant String (such as "LOW" or "HIGH") 
			final ConstantElementValue constantvalue = (ConstantElementValue) levelValue;
			// .. of Type String
			level = (String) constantvalue.val;
		}
		
		final ElementValue annotateValue = a.getNamedArguments().get("annotate");
		final AnnotationPolicy annotate; 
		if (annotateValue == null) {
			annotate = null;
		} else {
			final EnumElementValue enumannotate = (EnumElementValue) annotateValue;
			assert (annotationPolicy.equals(TypeName.findOrCreate(enumannotate.enumType)));
			annotate = AnnotationPolicy.valueOf(enumannotate.enumVal);						
		}
		
		final ElementValue positionDefinitionValue = a.getNamedArguments().get("positionDefinition");
		final PositionDefinition positionDef;
		if (positionDefinitionValue == null) {
			positionDef = null;
		} else {
			final EnumElementValue enumpositiondefinition = (EnumElementValue) positionDefinitionValue;
			assert (positionDefinition.equals(TypeName.findOrCreate(enumpositiondefinition.enumType)));
			positionDef = PositionDefinition.valueOf(enumpositiondefinition.enumVal);
		}

		final ElementValue seenByValue = a.getNamedArguments().get("seenBy");
		final String[] seenBy;
		if (seenByValue == null) {
			seenBy = null;
		} else {
			final ArrayElementValue arraySeenBy = (ArrayElementValue) seenByValue;
			seenBy = new String[arraySeenBy.vals.length];
			for (int i = 0; i < arraySeenBy.vals.length; i++) {
				seenBy[i] = arraySeenBy.vals[i].toString();
			}
		}

		final ElementValue mayIncludeValue = a.getNamedArguments().get("mayInclude");
		final String[] mayInclude;
		if (mayIncludeValue == null) {
			mayInclude = null;
		} else {
			final ArrayElementValue arrayMayInclude = (ArrayElementValue) mayIncludeValue;
			mayInclude = new String[arrayMayInclude.vals.length];
			for (int i = 0; i < arrayMayInclude.vals.length; i++) {
				mayInclude[i] = arrayMayInclude.vals[i].toString();
			}
		}
		
		final ElementValue lineNumberValue = a.getNamedArguments().get("lineNumber");
		final Integer lineNumber;
		if (lineNumberValue == null) {
			lineNumber = -1;
		} else {
			final ConstantElementValue constantvalue = (ConstantElementValue) lineNumberValue;
			lineNumber = (Integer) constantvalue.val;
		}
		
		final ElementValue columnNumberValue = a.getNamedArguments().get("columnNumber");
		final Integer columnNumber;
		if (columnNumberValue == null) {
			columnNumber = -1;
		} else {
			final ConstantElementValue constantvalue = (ConstantElementValue) columnNumberValue;
			columnNumber = (Integer) constantvalue.val;
		}
		
		final ElementValue idValue = a.getNamedArguments().get("id");
		final String id;
		if (idValue == null) {
			id = null;
		} else {
			// As per @Sink / @Source Definition: "id" is a constant String 
			final ConstantElementValue constantvalue = (ConstantElementValue) idValue;
			// .. of Type String
			id = (String) constantvalue.val;
		}
		
		final ElementValue tagsValue = a.getNamedArguments().get("tags");
		final String[] tags;
		if (tagsValue == null) {
			tags = new String[0];
		} else {
			tags = Arrays.stream(((ArrayElementValue)a.getNamedArguments().get("tags")).vals)
					.map(es -> (ConstantElementValue)es).map(es -> (String)es.val).toArray(String[]::new);
		}
		
		return new Sink() {
			@Override
			public Class<? extends java.lang.annotation.Annotation> annotationType() {
				return Sink.class;
			}
			
			@Override
			public String[] seenBy() {
				return seenBy;
			}
			
			@Override
			public String[] mayInclude() {
				return mayInclude;
			}
			
			@Override
			public String level() {
				return level;
			}
			
			@Override
			public AnnotationPolicy annotate() {
				return annotate;
			}
			
			@Override
			public PositionDefinition positionDefinition() {
				return positionDef;
			}
			
			@Override
			public int lineNumber() {
				return lineNumber;
			}
			
			@Override
			public int columnNumber() {
				return columnNumber;
			}
			
			@Override
			public String id() {
				return id;
			}
			
			@Override
			public String[] tags() {
				return tags;
			}
			
			@Override
			public String toString() {
				return "@Sink(level = " + level + ", mayInclude = " + Arrays.toString(mayInclude) + ", seenBy = " + Arrays.toString(seenBy) + ")";
			}
		};
	}
	
	private Declassification processDeclassification(Annotation a, TypeName annotationPolicy, TypeName positionDefinition){
		final ElementValue fromValue = a.getNamedArguments().get("from");
		final String from;
		if (fromValue == null) {
			from = null;
		} else {
			from = (String) ((ConstantElementValue) fromValue).val;
		}
		
		final ElementValue toValue = a.getNamedArguments().get("to");
		final String to;
		if (toValue == null) {
			to = null;
		} else {
			to = (String) ((ConstantElementValue) toValue).val;
		}
		
		final ElementValue tagsValue = a.getNamedArguments().get("tags");
		final String[] tags;
		if (tagsValue == null) {
			tags = new String[0];
		} else {
			tags = Arrays.stream(((ArrayElementValue)a.getNamedArguments().get("tags")).vals)
					.map(es -> (ConstantElementValue)es).map(es -> (String)es.val).toArray(String[]::new);
		}
		
		return new Declassification() {
			
			@Override
			public Class<? extends java.lang.annotation.Annotation> annotationType() {
				return Declassification.class;
			}
			
			@Override
			public String to() {
				return to;
			}
			
			@Override
			public String[] tags() {
				return tags;
			}
			
			@Override
			public String from() {
				return from;
			}
		};
	}
	
	/**
	 * If Java Source annotations are available, add corresponding IFC Annotations to the Analysis.
	 */
	public Pair<Multimap<SDGProgramPart, Pair<Source, String>>, Multimap<SDGProgramPart, Pair<Sink, String>>> addAllJavaSourceAnnotations(IStaticLattice<String> lattice) {
		final Pair<Multimap<SDGProgramPart, Pair<Source, String>>, Multimap<SDGProgramPart, Pair<Sink, String>>> annotations = getJavaSourceAnnotations();
		final Multimap<SDGProgramPart, Pair<Source, String>> sources = annotations.getFirst();
		final Multimap<SDGProgramPart, Pair<Sink, String>> sinks  = annotations.getSecond();

		for (final Entry<SDGProgramPart, Pair<Source, String>> e : sources.entries()) {
			final Source source       = e.getValue().getFirst();
			final String sourceFile   = e.getValue().getSecond();
			final SDGProgramPart part = e.getKey();
			
			if (source.mayKnow() != null) {
				debug.out("Source Annotation with mayKnow == " + source.mayKnow() + " != null found while processing lattice-EntryPoint, ignoring");
			} else if (source.includes() != null) {
				debug.out("Source Annotation with includes == " + source.includes() + " != null found while processing lattice-EntryPoint, ignoring");
			} else try {
				final String level = 
					(source.level() == null) ?
						(String) Source.class.getMethod("level").getDefaultValue() : source.level();
				final AnnotationPolicy annotate =
					(source.annotate() == null) ?
						(AnnotationPolicy) Source.class.getMethod("annotate").getDefaultValue() : source.annotate();

				annotatePart(Source.class, source, part, level, annotate, sourceFile);
			} catch (NoSuchMethodException nsme) {
				throw new AssertionError("Default value for invalid annotation attribute requested");
			}
		}

		for (final Entry<SDGProgramPart, Pair<Sink, String>> e : sinks.entries()) {
			final Sink sink           = e.getValue().getFirst();
			final String sourceFile   = e.getValue().getSecond();
			final SDGProgramPart part = e.getKey();
			
			if (sink.seenBy() != null) {
				debug.out("Source Annotation with seenBy == " + sink.seenBy() + " != null found while processing lattice-EntryPoint, ignoring");
			} else if (sink.mayInclude() != null) {
				debug.out("Source Annotation with includes == " + sink.mayInclude() + " != null found while processing lattice-EntryPoint, ignoring");
			} else try {
				final String level = 
					(sink.level() == null) ?
						(String) Sink.class.getMethod("level").getDefaultValue() : sink.level();
				final AnnotationPolicy annotate =
					(sink.annotate() == null) ?
						(AnnotationPolicy) Sink.class.getMethod("annotate").getDefaultValue() : sink.annotate();
				
				annotatePart(Sink.class, sink, part, level, annotate, sourceFile);
			} catch (NoSuchMethodException nsme) {
				throw new AssertionError("Default value for invalid annotation attribute requested");
			}
		}
		return annotations;
	}
	
	private <S extends java.lang.annotation.Annotation> void annotatePart(Class<S> ann, S s, SDGProgramPart part, String level, AnnotationPolicy annotate, String sourceFile) {
		if (ann != Source.class && ann != Sink.class) throw new IllegalArgumentException();
		
		final AnnotationCause cause;
		if (ann == Source.class) {
			cause = new JavaSourceAnnotation((Source) s, sourceFile);
		} else {
			assert (ann == Sink.class);
			cause = new JavaSinkAnnotation((Sink) s, sourceFile);
		}
		part.acceptVisitor(new ThrowingSDGProgramPartVisitor<Void, Void>() {
			@Override
			protected Void visitMethod(SDGMethod m, Void data) {
				if (ann == Source.class) addSourceAnnotation(m, level, annotate, cause);
				if (ann == Sink.class)   addSinkAnnotation(m, level, annotate, cause);
				return null;
			}
			
			@Override
			protected Void visitAttribute(SDGAttribute attribute, Void data) {
				if (annotate != AnnotationPolicy.ANNOTATE_USAGES) {
					throw new IllegalArgumentException("Fields may onlye be annotated with annotate == " + 
				                                        AnnotationPolicy.ANNOTATE_USAGES);
				}
				if (ann == Source.class) addSourceAnnotation(attribute, level, null, cause);
				if (ann == Sink.class)   addSinkAnnotation(attribute, level, null, cause);
				return null;
			}
			
			@Override
			protected Void visitParameter(SDGFormalParameter p, Void data) {
				if (annotate != AnnotationPolicy.ANNOTATE_USAGES) {
					throw new IllegalArgumentException("Fields may onlye be annotated with annotate == " + 
					                                        AnnotationPolicy.ANNOTATE_USAGES);
				}
				if (ann == Source.class) addSourceAnnotation(p, level, null, cause); 
				if (ann == Sink.class)   addSinkAnnotation(p, level, null, cause);
				return null;
			}
			
			@Override
			protected Void visitLocalVariable(SDGLocalVariable local, Void data) {
				if (annotate != AnnotationPolicy.ANNOTATE_USAGES) {
					throw new IllegalArgumentException("Local Variables may onlye be annotated with annotate == " + 
					                                        AnnotationPolicy.ANNOTATE_USAGES);
				}
				if (ann == Source.class) addSourceAnnotation(local, level, null, cause); 
				if (ann == Sink.class)   addSinkAnnotation(local, level, null, cause);
				return null;
			}
		}, null);
		debug.outln("Added " + ann + " Annotation: " + part + ":::" + level);
	}
	
	public void addSinkClasses(String[] sinkClasses){
		if (sinkClasses.length == 0){
			return;
		}
		IClassHierarchy ch = program.getClassHierarchy();
		for (SDGMethod method : program.getAllMethods()){
			JavaMethodSignature sig = method.getSignature();
			TypeReference ref = TypeReference.find(ClassLoaderReference.Application, sig.getDeclaringType().toBCString().replace(";", ""));
			if (ref == null){
				continue;
			}
			IClass declaring = ch.lookupClass(ref);
			for (String sinkClass : sinkClasses){
				IClass sink = ch.lookupClass(TypeReference.find(ClassLoaderReference.Application, sinkClass));
				if (declaring != null && (ch.isSubclassOf(declaring, sink) || ch.implementsInterface(declaring, sink))){
					for (SDGFormalParameter param : method.getParameters()){
						if (!param.getName().equals("this")){
							addSinkAnnotation(param, Level.LOW);
						}
					}
				}
			}			
		}
	}

	public boolean addSinkClass(SDGClass klass){
		IClassHierarchy ch = program.getClassHierarchy();
		boolean added = false;
		for (SDGMethod method : program.getAllMethods()){
			JavaMethodSignature sig = method.getSignature();
			TypeReference ref = TypeReference.find(ClassLoaderReference.Application, sig.getDeclaringType().toBCString().replace(";", ""));
			if (ref == null){
				continue;
			}
			IClass declaring = ch.lookupClass(ref);
			IClass sink = ch.lookupClass(klass.getTypeName().toBCString().replace(";", ""));

			if (declaring != null && (ch.isSubclassOf(declaring, sink) || ch.implementsInterface(declaring, sink) || declaring.equals(sink))){
				for (SDGFormalParameter param : method.getParameters()){
					if (!param.getName().equals("this")){
						addSinkAnnotation(param, Level.LOW);
						added = true;
					}
				}
			}
		}
		return added;
	}
}
