/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.api.sdg;

import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.classLoader.IField;
import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.classLoader.ShrikeCTMethod;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.CallGraph;
import com.ibm.wala.ipa.callgraph.impl.FakeRootClass;
import com.ibm.wala.ipa.cha.ClassHierarchyException;
import com.ibm.wala.ipa.cha.IClassHierarchy;
import com.ibm.wala.shrikeCT.InvalidClassFileException;
import com.ibm.wala.shrikeCT.TypeAnnotationsReader.TargetType;
import com.ibm.wala.ssa.IR;
import com.ibm.wala.ssa.SSAGetInstruction;
import com.ibm.wala.ssa.SSAInstruction;
import com.ibm.wala.ssa.SSAPutInstruction;
import com.ibm.wala.types.annotations.Annotation;
import com.ibm.wala.types.annotations.TypeAnnotation;
import com.ibm.wala.types.annotations.TypeAnnotation.LocalVarTarget;
import com.ibm.wala.util.CancelException;
import com.ibm.wala.util.MonitorUtil.IProgressMonitor;
import com.ibm.wala.util.graph.GraphIntegrity.UnsoundGraphException;
import edu.kit.joana.api.annotations.AnnotationType;
import edu.kit.joana.api.annotations.AnnotationTypeBasedNodeCollector;
import edu.kit.joana.api.annotations.IdManager;
import edu.kit.joana.ifc.sdg.core.SecurityNode;
import edu.kit.joana.ifc.sdg.graph.SDG;
import edu.kit.joana.ifc.sdg.graph.SDGEdge;
import edu.kit.joana.ifc.sdg.graph.SDGNode;
import edu.kit.joana.ifc.sdg.graph.SDGSerializer;
import edu.kit.joana.ifc.sdg.graph.chopper.Chopper;
import edu.kit.joana.ifc.sdg.graph.chopper.NonSameLevelChopper;
import edu.kit.joana.ifc.sdg.graph.slicer.graph.threads.MHPAnalysis;
import edu.kit.joana.ifc.sdg.mhpoptimization.CSDGPreprocessor;
import edu.kit.joana.ifc.sdg.mhpoptimization.MHPType;
import edu.kit.joana.ifc.sdg.mhpoptimization.PruneInterferences;
import edu.kit.joana.ifc.sdg.util.BytecodeLocation;
import edu.kit.joana.ifc.sdg.util.JavaMethodSignature;
import edu.kit.joana.ifc.sdg.util.JavaType;
import edu.kit.joana.ifc.sdg.util.JavaType.Format;
import edu.kit.joana.ui.annotations.*;
import edu.kit.joana.util.Log;
import edu.kit.joana.util.Logger;
import edu.kit.joana.util.Pair;
import edu.kit.joana.util.Stubs;
import edu.kit.joana.util.io.IOFactory;
import edu.kit.joana.wala.core.NullProgressMonitor;
import edu.kit.joana.wala.core.SDGBuildArtifacts;
import edu.kit.joana.wala.core.SDGBuilder;
import edu.kit.joana.wala.summary.SummaryComputation;
import edu.kit.joana.wala.summary.WorkPackage;
import edu.kit.joana.wala.summary.WorkPackage.EntryPoint;
import edu.kit.joana.wala.util.PrettyWalaNames;
import gnu.trove.set.TIntSet;
import gnu.trove.set.hash.TIntHashSet;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.stream.Collectors;

public class SDGProgram {

	/**
	 * Special object which provides information about where a given code piece stems from
	 * @author Martin Mohr
	 */
	public static class ClassLoader {

		/** for code which stems from the actual application */
		public static final ClassLoader APPLICATION = new ClassLoader("Application");

		/** for java standard library code */
		public static final ClassLoader PRIMORDIAL = new ClassLoader("Primordial");

		private String name;

		private ClassLoader(String name) {
			this.name = name;
		}

		public String getName() {
			return name;
		}

		/**
		 * Extracts the class loader from the given entry node. Does not work if the given node is not an entry node.
		 * @param entry entry node to extract class loader from
		 * @return class loader value from the given entry node
		 */
		public static ClassLoader fromSDGNode(SDGNode entry) {
			String clsLoader = entry.getClassLoader();
			if (clsLoader.equals("Application")) {
				return ClassLoader.APPLICATION;
			} else if (clsLoader.equals("Primordial")) {
				return ClassLoader.PRIMORDIAL;
			} else {
				return new ClassLoader(clsLoader);
			}
		}
		
		public static ClassLoader fromString(String clsLoader) {
			if (clsLoader.equals("Application")) {
				return APPLICATION;
			} else if (clsLoader.equals("Primordial")){
				return PRIMORDIAL;
			} else {
				return new ClassLoader(clsLoader);
			}
		}

		/* (non-Javadoc)
		 * @see java.lang.Object#hashCode()
		 */
		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((name == null) ? 0 : name.hashCode());
			return result;
		}

		/* (non-Javadoc)
		 * @see java.lang.Object#equals(java.lang.Object)
		 */
		@Override
		public boolean equals(Object obj) {
			if (this == obj) {
				return true;
			}
			if (obj == null) {
				return false;
			}
			if (!(obj instanceof ClassLoader)) {
				return false;
			}
			ClassLoader other = (ClassLoader) obj;
			if (name == null) {
				if (other.name != null) {
					return false;
				}
			} else if (!name.equals(other.name)) {
				return false;
			}
			return true;
		}
	}

	private boolean isBuilt = false;
	private final SDGClassResolver classRes = new SDGClassResolver();
	private final SDGClassComputation classComp;
	private final Set<SDGClass> classes = new LinkedHashSet<SDGClass>();
	private final SDG sdg;
	private final MHPAnalysis mhpAnalysis;
	private SDGProgramPartParserBC ppartParser;
	private final Map<SDGProgramPart, Collection<Pair<Annotation, String>>> annotations = new LinkedHashMap<>();
	/**
	 * Additional annotations, like EntryPoint and other annotations used in the code,
	 * excludes sink, source and declassification related annotations (stored in the annotations field)
	 */
	private final Map<SDGProgramPart, Set<Annotation>> miscAnnotations = new LinkedHashMap<>();

	private final IdManager idManager = new IdManager();

	private final AnnotationTypeBasedNodeCollector coll;
	private IClassHierarchy ch;
	private final Optional<String> entryMethod;

	private static Logger debug = Log.getLogger(Log.L_API_DEBUG);

	public SDGProgram(SDG sdg, MHPAnalysis mhpAnalysis, String entryMethod) {
		this(sdg, mhpAnalysis, Optional.of(entryMethod));
	}

	public SDGProgram(SDG sdg, MHPAnalysis mhpAnalysis) {
		this(sdg, mhpAnalysis, Optional.empty());
	}

	public SDGProgram(SDG sdg, MHPAnalysis mhpAnalysis, Optional<String> entryMethod) {
		this.sdg = sdg;
		this.mhpAnalysis = mhpAnalysis;
		this.ppartParser = new SDGProgramPartParserBC(this);
		this.classComp = new SDGClassComputation(sdg);
		this.coll = new AnnotationTypeBasedNodeCollector(sdg, this.classComp);
		this.coll.init(this);
		this.entryMethod = entryMethod;
	}

	public AnnotationTypeBasedNodeCollector getNodeCollector() {
		return coll;
	}

	public static SDGProgram loadSDG(String path, MHPType mhpType) throws IOException {
		final SDG sdg = SDG.readFromAndUseLessHeap(path, new SecurityNode.SecurityNodeFactory());
		final MHPAnalysis mhpAnalysis = mhpType.getMhpAnalysisConstructor().apply(sdg);
		PruneInterferences.pruneInterferences(sdg, mhpAnalysis);
		return new SDGProgram(sdg, mhpAnalysis);
	}

	public static SDGProgram createSDGProgram(String classPath, String entryMethod) {
		return createSDGProgram(classPath, entryMethod, false);
	}

	public static SDGProgram createSDGProgram(String classPath, String entryMethod, boolean computeInterference) {
		return createSDGProgram(classPath, entryMethod, computeInterference, MHPType.SIMPLE);
	}

	public static SDGProgram createSDGProgram(String classPath, String entryMethod, boolean computeInterference,
			MHPType mhpType) {
		try {
			// TODO: do not ever create with stubs == null!
			return createSDGProgram(classPath, entryMethod, null, computeInterference, mhpType,
					IOFactory.createUTF8PrintStream(new ByteArrayOutputStream()), NullProgressMonitor.INSTANCE);
		} catch (ClassHierarchyException e) {
			return null;
		} catch (IOException e) {
			return null;
		} catch (UnsoundGraphException e) {
			return null;
		} catch (CancelException e) {
			return null;
		}
	}

	public static SDGProgram createSDGProgram(String classPath, String entryMethod, Stubs stubsPath,
			boolean computeInterference, MHPType mhpType, PrintStream out, IProgressMonitor monitor)
			throws ClassHierarchyException, IOException, UnsoundGraphException, CancelException {
		SDGConfig config = new SDGConfig(classPath, entryMethod, stubsPath);
		config.setComputeInterferences(computeInterference);
		config.setMhpType(mhpType);
		return createSDGProgram(config, out, monitor);
	}

	public static SDGProgram createSDGProgram(SDGConfig config) throws ClassHierarchyException, IOException,
			UnsoundGraphException, CancelException {
		return createSDGProgram(config, IOFactory.createUTF8PrintStream(new ByteArrayOutputStream()),
				NullProgressMonitor.INSTANCE);
	}
	
	public static SDGProgram createSDGProgram(SDGConfig config, PrintStream out, IProgressMonitor monitor)
			throws ClassHierarchyException, IOException, UnsoundGraphException, CancelException {
		return createSDGProgram(config, out, monitor, null);
	}

	public static SDGProgram createSDGProgram(SDGConfig config, PrintStream out, IProgressMonitor monitor, OutputStream sdgFileOut)
			throws ClassHierarchyException, IOException, UnsoundGraphException, CancelException {
		monitor.beginTask("build SDG", 20);
		ConstructionNotifier notifier = config.getNotifier();
		if (notifier != null) {
			notifier.sdgStarted();
		}
		final com.ibm.wala.util.collections.Pair<SDG, SDGBuildArtifacts> p =
				SDGBuildPreparation.computeAndKeepBuildArtifacts(out, makeBuildPreparationConfig(config), monitor);
		final SDG sdg = p.fst;
		final SDGBuildArtifacts buildArtifacts = p.snd;

		if (config.computeInterferences()) {
			CSDGPreprocessor.preprocessSDG(sdg);
		}
		
		final MHPAnalysis mhpAnalysis = config.getMhpType().getMhpAnalysisConstructor().apply(sdg);
		assert (mhpAnalysis == null) == (config.getMhpType() == MHPType.NONE);
		
		if (config.computeInterferences()) {
			PruneInterferences.pruneInterferences(sdg, mhpAnalysis);
		}
		
		if (notifier != null) {
			notifier.sdgFinished();
			notifier.numberOfCGNodes(buildArtifacts.getNonPrunedWalaCallGraph().getNumberOfNodes(), buildArtifacts.getWalaCallGraph().getNumberOfNodes());
		}
		if (config.getIgnoreIndirectFlows()) {
			if (notifier != null) {
				notifier.stripControlDepsStarted();
			}
			throwAwayControlDeps(sdg);
			if (notifier != null) {
				notifier.stripControlDepsFinished();
			}
			
		}
		if (sdgFileOut != null) {
			SDGSerializer.toPDGFormat(sdg, sdgFileOut);
			sdgFileOut.flush();
		}
		final SDGProgram ret = new SDGProgram(sdg, mhpAnalysis, config.getEntryMethod());
		ret.setClassHierarchy(buildArtifacts.getClassHierarchy());
		if (config.isSkipSDGProgramPart()) {
			return ret;
		}
		
		
		final IClassHierarchy ch  = buildArtifacts.getClassHierarchy();
		final CallGraph callGraph = buildArtifacts.getWalaCallGraph(); 
		ret.fillWithAnnotations(ch, findClassesRelevantForAnnotation(ch, callGraph));
		return ret;
	}
	
	private void setClassHierarchy(IClassHierarchy ch) {
		this.ch = ch;
	}
	
	public IClassHierarchy getClassHierarchy(){
		return ch;
	}

	public static Set<IClass> findClassesRelevantForAnnotation(IClassHierarchy ch, CallGraph callGraph) {
		final Set<IClass> classes = new HashSet<>();
		SSAInstruction.Visitor collectReferencedClasses = new SSAInstruction.Visitor() {
			@Override
			public void visitGet(SSAGetInstruction instruction) {
				final IClass cl = ch.lookupClass(instruction.getDeclaredFieldType());
				if (cl != null && !cl.isArrayClass() ) {
					classes.add(cl);
				}
			}
			
			@Override
			public void visitPut(SSAPutInstruction instruction) {
				final IClass cl = ch.lookupClass(instruction.getDeclaredFieldType());
				if (cl != null && !cl.isArrayClass()) {
					classes.add(cl);
				}
			}
		};
		
		// TODO: is this enough in general?!?!?
		for (CGNode cgnode : callGraph) {
			final IClass cl = cgnode.getMethod().getDeclaringClass();
			assert cl != null;
			
			if (!(cl instanceof FakeRootClass) && !cl.isArrayClass()) {
				classes.add(cl);
			}
			
			final IR ir = cgnode.getIR();
			if (ir != null) {
				ir.visitNormalInstructions(collectReferencedClasses);
			}
		}
		return classes;
		
	}

	public void fillWithAnnotations(IClassHierarchy cha, Iterable<IClass> classes) {
		collectedAllNodesForMiscAnnotations = false;
		final Collection<String> sourceOrSinkAnnotationName = 
				Arrays.asList(new Class<?>[] { 
					Source.class, Sink.class, Declassification.class,
					Sources.class, Sinks.class, Declassifications.class, ReturnValue.class
				})
				.stream().map( cl -> "L" + cl.getName().replace(".", "/")).collect(Collectors.toList());
		for (IClass c : classes) {
			final String walaClassName = c.getName().toString();
			final JavaType jt = JavaType.parseSingleTypeFromString(walaClassName, Format.BC);
			final String sourcefile = PrettyWalaNames.sourceFileName(c.getName());


			Function<Collection<Annotation>, Optional<Pair<Collection<Pair<Annotation, String>>, Collection<Annotation>>>> splitAnnotations = anns -> {
				if (anns == null || anns.isEmpty()) {
					return Optional.empty();
				}
				return Optional.of(Pair.nonNullPairs(anns.stream()
						.filter( a -> sourceOrSinkAnnotationName.contains(a.getType().getName().toString()))
						.map( a -> Pair.pair(a, sourcefile))
						.collect(Collectors.toList()), anns.stream()
						.filter( a -> !sourceOrSinkAnnotationName.contains(a.getType().getName().toString()))
						.collect(Collectors.toList())));
			};

			BiConsumer<Pair<Collection<Pair<Annotation, String>>, Collection<Annotation>>, Collection<? extends SDGProgramPart>> storeAnnotations = (pair, parts) -> {
				for (SDGProgramPart part : parts) {
					if (pair.getFirst().size() > 0) {
						this.annotations.computeIfAbsent(part, p -> new HashSet<>()).addAll(pair.getFirst());
					}
					if (pair.getSecond().size() > 0) {
						this.miscAnnotations.computeIfAbsent(part, p -> new HashSet<>()).addAll(pair.getSecond());
					}
				}
			};

			for (IField f : c.getAllFields()) {
				final Collection<SDGAttribute> attributes = this.getAttribute(jt, f.getName().toString());
				// attributes.isEmpty() if c isn't Part of the CallGraph
				Optional<Pair<Collection<Pair<Annotation, String>>, Collection<Annotation>>> relAndNonRel = splitAnnotations
						.apply(f.getAnnotations());
				if (relAndNonRel.isPresent()) {
					storeAnnotations.accept(relAndNonRel.get(), attributes);
					for (SDGAttribute attribute : attributes) {
						f.getAnnotations().forEach(a -> idManager.put(attribute, a));
					}
					debug.outln("Annotated: " + jt + ":::" + f.getName() + " with " + f.getAnnotations());
				}

			}

			for (IMethod m : c.getAllMethods()) {
				
				if (m.getAnnotations() != null) {
					Optional<Pair<Collection<Pair<Annotation, String>>, Collection<Annotation>>> relAndNonRel = splitAnnotations
							.apply(m.getAnnotations());
					if (relAndNonRel.isPresent()) {
						final Set<IMethod> implementors = cha.getPossibleTargets(m.getReference());
						final Collection<SDGMethod> methods = implementors.stream().flatMap(
								mImpl -> this.getMethods(JavaMethodSignature.fromString(mImpl.getSignature())).stream()
						).collect(Collectors.toList());
						storeAnnotations.accept(relAndNonRel.get(), methods);
						if (relAndNonRel.get().getFirst().size() > 0) {
							for (SDGMethod sdgm : methods) {
								m.getAnnotations().forEach(a -> idManager.put(sdgm, a));
							}
						}
						debug.outln("Annotated: " + jt + ":::" + m.getName() + " with " + m.getAnnotations());
					}
				}
				
				if (m instanceof ShrikeCTMethod) {
					ShrikeCTMethod method = (ShrikeCTMethod) m;
					Collection<SDGMethod> methods = Collections.emptyList();

					int parameterNumber = m.isStatic() ? 1 : 1;
					for(Collection<Annotation> parameter : method.getParameterAnnotations() ) {
						Optional<Pair<Collection<Pair<Annotation, String>>, Collection<Annotation>>> relAndNonRel = splitAnnotations.apply(parameter);
						if (relAndNonRel.isPresent()) {
							if (methods.isEmpty()) { 
								final Set<IMethod> implementors = cha.getPossibleTargets(m.getReference());
								methods = implementors.stream().flatMap(
										mImpl -> this.getMethods(JavaMethodSignature.fromString(mImpl.getSignature())).stream()
								).collect(Collectors.toList());
							}
							if (relAndNonRel.get().getSecond().size() > 0) {
								for (SDGMethod sdgMethod : methods) {
									SDGFormalParameter param = sdgMethod.getParameter(parameterNumber);
									this.miscAnnotations.computeIfAbsent(param, p -> new HashSet<>()).addAll(relAndNonRel.get().getSecond());
								}
							}
							for (SDGMethod sdgm : methods) {
								this.annotations.put(sdgm.getParameter(parameterNumber), relAndNonRel.get().getFirst());
								m.getAnnotations().forEach(a -> idManager.put(sdgm, a));
							}
						}
						parameterNumber++;
					}
					
					try {
						final Collection<TypeAnnotation> localVarAnnotations = 
						method.getTypeAnnotationsAtCode(true)
							.stream()
							.filter(a -> a.getTargetType() == TargetType.LOCAL_VARIABLE)
							.collect(Collectors.toList());
						
						if (!localVarAnnotations.isEmpty()) {
							if (methods.isEmpty()) { 
								methods = this.getMethods(JavaMethodSignature.fromString(m.getSignature()));
							}
							for (TypeAnnotation ta : localVarAnnotations) {
								final LocalVarTarget localVarTarget = (LocalVarTarget) ta.getTypeAnnotationTarget();
								final String varName = localVarTarget.getName();
								if (varName == null) {
									debug.outln("Warning: variable Name of local Variable annotation could not be determined");
								} else {
									for (SDGMethod sdgm : methods) {
										final SDGLocalVariable localVar = sdgm.getLocalVariable(varName);
										if (localVar != null) {
											c.getSourceFileName();
											c.getSource();
											this.annotations.computeIfAbsent(localVar, lv -> new LinkedList<Pair<Annotation, String>>());
											this.annotations.computeIfPresent(localVar, (lv, anns) -> {
												anns.add(Pair.pair(ta.getAnnotation(),sourcefile));
												return anns;
											});
											idManager.put(sdgm, ta.getAnnotation());
										} else {
											debug.outln("Warning: Variable "
											   + localVarTarget + " in "
											   + JavaMethodSignature.fromString(m.getSignature()) 
											   + "not found. Did you try to annotate an 'ephemeral' Variable such as 'int x = p' where 'x' is never used in the method?"
											);
										}
									}
								}
							}
							parameterNumber++;
						}
					} catch (InvalidClassFileException e) {
						// TODO: handle this?!?!
					}
				} else {
					debug.outln("Warning: Parameter Annotation Processing not supported for Methods representet by " + m.getClass());
				}
			}
		}
	}

	/**
	 * Returns an immutable id manager
	 */
	public IdManager getIdManager(){
		return idManager.immutable();
	}

	public static SDGBuilder createSDGBuilder(SDGConfig config) throws ClassHierarchyException, UnsoundGraphException, CancelException, IOException {
		return SDGBuildPreparation.createBuilder(IOFactory.createUTF8PrintStream(new ByteArrayOutputStream()), makeBuildPreparationConfig(config), NullProgressMonitor.INSTANCE);
	}
	public static SDGBuildPreparation.Config makeBuildPreparationConfig(SDGConfig config) {
		JavaMethodSignature mainMethod = JavaMethodSignature.fromString(config.getEntryMethod());// JavaMethodSignature.mainMethodOfClass(config.getMainClass());
		SDGBuildPreparation.Config cfg = new SDGBuildPreparation.Config(mainMethod.toBCString(), mainMethod.toBCString(), config.getClassPath(), config.getClasspathAddEntriesFromMANIFEST(),
				config.getFieldPropagation());
		cfg.thirdPartyLibPath = config.getThirdPartyLibsPath();
		cfg.exceptions = config.getExceptionAnalysis();
		cfg.defaultExceptionMethodState = config.getDefaultExceptionMethodState();
		cfg.pts = config.getPointsToPrecision();
		cfg.accessPath = config.computeAccessPaths();
		cfg.sideEffects = config.getSideEffectDetectorConfig();
		cfg.stubs = config.getStubs();
		cfg.pruningPolicy = config.getPruningPolicy();
		cfg.exclusions = config.getExclusions();
		cfg.computeAllocationSites = config.computeAllocationSites();
		cfg.cgConsumer = config.getCGConsumer();
		cfg.ctxSelector = config.getContextSelector();
		cfg.ddisp = config.getDynamicDispatchHandling();
		cfg.computeSummaryEdges = config.isComputeSummaryEdges();
		cfg.summaryComputationType = config.getSummaryComputationType();
		cfg.computeInterference = config.computeInterferences();
		cfg.localKillingDefs = config.localKillingDefs();
		cfg.isParallel = config.isParallel();
		cfg.controlDependenceVariant = config.getControlDependenceVariant();
		cfg.fieldHelperOptions = config.getFieldHelperOptions();
		debug.outln(cfg.stubs);
		return cfg;
	}
	public static void throwAwayControlDeps(SDG sdg) throws CancelException {
		final List<SDGEdge> toRemove = new LinkedList<SDGEdge>();
		for (final SDGEdge e : sdg.edgeSet()) {
			switch (e.getKind()) {
			case CONTROL_DEP_COND:
			case CONTROL_DEP_UNCOND:
			case SUMMARY:
			case SUMMARY_DATA:
			case SUMMARY_NO_ALIAS:
				toRemove.add(e);
				break;
			default: // nothing to do
			}
		}
		sdg.removeAllEdges(toRemove);
		// rerun summary computation
		final WorkPackage<SDG> wp = createSummaryWorkpackage(sdg);
		SummaryComputation.computeHeapDataDep(wp, new com.ibm.wala.util.NullProgressMonitor());
	}

	private static WorkPackage<SDG> createSummaryWorkpackage(final SDG sdg) {
		final Set<EntryPoint> entries = new TreeSet<EntryPoint>();
		final SDGNode root = sdg.getRoot();

		final TIntSet formalIns = new TIntHashSet();
		for (final SDGNode fIn : sdg.getFormalIns(root)) {
			formalIns.add(fIn.getId());
		}
		final TIntSet formalOuts = new TIntHashSet();
		for (final SDGNode fOut : sdg.getFormalOuts(root)) {
			formalOuts.add(fOut.getId());
		}
		final EntryPoint ep = new EntryPoint(root.getId(), formalIns, formalOuts);
		entries.add(ep);
		return WorkPackage.create(sdg, entries, "no_control_deps");
	}

	private void build() {
		if (!isBuilt) {
			this.classes.addAll(classComp.compute());
			this.classRes.setClasses(classes);
			isBuilt = true;
		}
	}

	public SDG getSDG() {
		return sdg;
	}

	public MHPAnalysis getMhpAnalysis() {
		return mhpAnalysis;
	}
	

	public Map<SDGProgramPart, Collection<Pair<Annotation,String>>> getJavaSourceAnnotations() {
		return annotations;
	}

	public Map<SDGProgramPart, Set<Annotation>> getMiscJavaSourceAnnotations() {
		return Collections.unmodifiableMap(miscAnnotations);
	}

	public Set<Annotation> getMiscJavaSourceAnnotations(SDGProgramPart part) {
		return Collections.unmodifiableSet(miscAnnotations.getOrDefault(part, Collections.emptySet()));
	}

	/** Exclude annotations from the edu.kit.joana package */
	public Set<Annotation> getMiscJavaSourceAnnotationsWOJoana(SDGProgramPart part) {
		return miscAnnotations.getOrDefault(part, Collections.emptySet()).stream()
				.filter(a -> a.getType().getName().getPackage().toString().startsWith("edu.kit.joana.")).collect(Collectors.toSet());
	}

	private boolean collectedAllNodesForMiscAnnotations = false;

	private void initAnnotationCollectorWithAllMiscAnnotatedNodes() {
		if (!collectedAllNodesForMiscAnnotations) {
			miscAnnotations.keySet().forEach(p -> coll.collectNodes(p, AnnotationType.MISC));
			collectedAllNodesForMiscAnnotations = true;
		}
	}

	public Set<Annotation> getMiscAnnotations(SDGNode node) {
		initAnnotationCollectorWithAllMiscAnnotatedNodes();
		return coll.getCoveringCandidates(node).stream().flatMap(p -> miscAnnotations.getOrDefault(p, Collections.emptySet()).stream()).collect(
				Collectors.toSet());
	}

	public Set<Annotation> getMiscAnnotationsWOJoana(SDGNode node) {
		return getMiscAnnotations(node).stream().filter(a -> a.getType().getName().getPackage().toString().startsWith("edu.kit.joana.")).collect(
				Collectors.toSet());
	}

	public Collection<SDGClass> getClasses() {
		build();
		return classes;
	}

	public Collection<SDGClass> getClass(JavaType typeName) {
		build();
		return classRes.getClass(typeName);
	}

	public Collection<SDGAttribute> getAttribute(JavaType typeName, String attrName) {
		build();
		return classRes.getAttribute(typeName, attrName);
	}

	public Collection<SDGMethod> getMethods(JavaMethodSignature methodSig) {
		build();
		return classRes.getMethod(methodSig.getDeclaringType(), methodSig);
	}

	public Collection<SDGInstruction> getInstruction(JavaMethodSignature methodSig, int bcIndex) {
		build();
		return classRes.getInstruction(methodSig.getDeclaringType(), methodSig, bcIndex);
	}
	
	/**
	 * Get instructions by label, i.e. the label of the corresponding sdg node. Precisely, all
	 *
	 * @param methodSig method in which to search
	 * @param labelRegEx label to look for
	 * @return instructions for which the label matches the given regex
	 */
	public Collection<SDGInstruction> getInstruction(JavaMethodSignature methodSig, String labelRegEx) {
		build();
		return classRes.getInstruction(methodSig.getDeclaringType(), methodSig, labelRegEx);
	}

	
	public Collection<SDGLocalVariable> getLocalVariables(JavaMethodSignature methodSig, String varName) {
		build();
		return classRes.getLocalVariable(methodSig.getDeclaringType(), methodSig, varName);
	}
	
	public Collection<SDGCall> getCallsToMethod(JavaMethodSignature tgt) {
		Collection<SDGCall> ret = new LinkedList<SDGCall>();
		build();
		for (SDGClass cl : getClasses()) {
			for (SDGMethod m : cl.getMethods()) {
				ret.addAll(m.getAllCalls(tgt));
			}
		}
		return ret;
	}

	public ClassLoader getClassLoader(SDGInstruction i) {
		return ClassLoader.fromString(i.getOwningMethod().getClassLoader());
	}

	public ClassLoader getClassLoader(SDGMethod m) {
		return ClassLoader.fromString(m.getClassLoader());
	}

	/**
	 * Returns whether the given instruction is contained in the application's code
	 * @param i an instruction from this program
	 * @return {@code true}, if the given instruction is contained in the application's code,
	 * {@code false} otherwise
	 */
	public boolean isInApplicationCode(SDGInstruction i) {
		return getClassLoader(i) == ClassLoader.APPLICATION;
	}

	/**
	 * Returns whether the given method is contained in the application's code
	 * @param m a method from this program
	 * @return {@code true}, if the given method is contained in the application's code,
	 * {@code false} otherwise
	 */
	public boolean isInApplicationCode(SDGMethod m) {
		return getClassLoader(m) == ClassLoader.APPLICATION;
	}

	/**
	 * Returns whether the given instruction is contained in standard library code
	 * @param i an instruction from this program
	 * @return {@code true}, if the given instruction is contained in standard library code,
	 * {@code false} otherwise
	 */
	public boolean isInPrimordialCode(SDGInstruction i) {
		return getClassLoader(i) == ClassLoader.PRIMORDIAL;
	}

	/**
	 * Returns whether the given method is contained in standard library code
	 * @param m a method from this program
	 * @return {@code true}, if the given method is contained in standard library code,
	 * {@code false} otherwise
	 */
	public boolean isInPrimordialCode(SDGMethod m) {
		return getClassLoader(m) == ClassLoader.PRIMORDIAL;
	}

	public Collection<SDGMethodExitNode> getMethodExitNode(JavaMethodSignature methodSig) {
		build();
		return classRes.getMethodExitNode(methodSig.getDeclaringType(), methodSig);
	}

	public Collection<SDGFormalParameter> getMethodParameter(JavaMethodSignature methodSig, int paramIndex) {
		build();
		return classRes.getMethodParameter(methodSig.getDeclaringType(), methodSig, paramIndex);
	}

	public Collection<? extends SDGProgramPart> getParts(String partDesc) {
		build();
		return ppartParser.getProgramParts(partDesc);
	}

	public Collection<SDGProgramPart> getAllProgramParts() {
		List<SDGProgramPart> ret = new LinkedList<SDGProgramPart>();
		SDGProgramPartCollector coll = new SDGProgramPartCollector();
		for (SDGClass cl : getClasses()) {
			cl.acceptVisitor(coll, ret);
		}

		return ret;
	}

	/**
	 * Tries to find a covering program part for the given SDG node. Currently, the following types of nodes are supported:
	 * <ul>
	 * <li>nodes of kind ENTRY, FORMAL_IN, FORMAL_OUT, EXIT</li>
	 * <li>nodes of kind ACTUAL_IN, ACTUAL_OUT</li>
	 * <li>other nodes with non-negative bytecode index</li>
	 * </ul>
	 * For all other types of nodes, {@code null} is returned.
	 * @param node node for which a covering program part shall be found
	 * @return covering program part for the given node, or {@code null} if the node is not supported (see above)
	 */
	public SDGProgramPart findCoveringProgramPart(SDGNode node) {
		Set<SDGProgramPart> candidates = collectCoveringCandidates(node);
		if (candidates.isEmpty()) {
			return null;
		}
		SDGProgramPart mostConcreteCandidate = Collections.max(candidates, SDGPPConcretenessEvaluator.getComparator());
		if (mostConcreteCandidate != null) {
			return mostConcreteCandidate;
		}
		debug.outln("node " + node + " has no program part!");
		return null;
	}

	public Set<SDGProgramPart> collectCoveringCandidates(SDGNode node) {
		LinkedHashSet<SDGProgramPart> ret = new LinkedHashSet<SDGProgramPart>();
		SDGNode entry = sdg.getEntry(node);
		JavaMethodSignature sig = JavaMethodSignature.fromString(entry.getBytecodeMethod());
		Collection<SDGMethod> methods = getMethods(sig);
		int bcIndex = node.getBytecodeIndex();
		switch (node.getKind()) {
		case ENTRY:
			ret.addAll(methods);
			break;
		case FORMAL_IN:
		case FORMAL_OUT:
		case EXIT:
			ret.addAll(methods);
			List<SDGNode> rootParams = SDGParameterUtils.collectPsBWReachableRootParameters(node, sdg);
			for (SDGNode rootParam : rootParams) {
				if (rootParam.getBytecodeName().equals(BytecodeLocation.RETURN_PARAM)) {
					for (SDGMethod m : methods) {
						if (m.getExit() != null) {
							ret.add(m.getExit());
						}
					}
				} else if (rootParam.getBytecodeName().equals(BytecodeLocation.EXCEPTION_PARAM)) {
					continue;
				} else {
					int paramIndex = BytecodeLocation.getRootParamIndex(rootParam.getBytecodeName());
					for (SDGMethod m : methods) {
						if (m.getParameter(paramIndex) != null) {
							ret.add(m.getParameter(paramIndex));
						}
					}
				}
			}
			break;
		case ACTUAL_IN:
		case ACTUAL_OUT:
			List<SDGNode> actRootParams = SDGParameterUtils.collectPsBWReachableRootParameters(node, sdg);
			for (SDGNode rootParam : actRootParams) {
				SDGNode callNode = SDGParameterUtils.locateCall(rootParam, sdg);
				int callBCIndex = callNode.getBytecodeIndex();
				for (SDGMethod m : methods) {
					SDGInstruction i = m.getInstructionWithBCIndex(callBCIndex);
					if (!(i instanceof SDGCall)) {
						throw new IllegalStateException(String.format("instruction should be a call instruction: (%s):%d", m.getSignature(), callBCIndex));
					}
					SDGCall call = (SDGCall) m.getInstructionWithBCIndex(callBCIndex);
					if (rootParam.getBytecodeName().equals(BytecodeLocation.RETURN_PARAM)) {
						if (call.getReturn() != null) {
							ret.add(call.getReturn());
						}
					} else if (rootParam.getBytecodeName().equals(BytecodeLocation.EXCEPTION_PARAM)) {
						if (call.getExceptionNode() != null) {
							ret.add(call.getExceptionNode());
						}
					} else {
						int paramIndex = BytecodeLocation.getRootParamIndex(rootParam.getBytecodeName());
						if (call.getActualParameter(paramIndex) != null) {
							ret.add(call.getActualParameter(paramIndex));
						}
					}
				}
			}
			break;
		default:
			if (bcIndex >= 0) {
				for (SDGMethod m : methods) {
					if (m.getInstructionWithBCIndex(bcIndex) != null) {
						ret.add(m.getInstructionWithBCIndex(bcIndex));
					}
				}
			}
			break;
		}
		return ret;
	}

	public boolean covers(SDGProgramPart ppart, SDGNode node) {
		return coll.collectNodes(ppart, AnnotationType.SOURCE).contains(node) || coll.collectNodes(ppart, AnnotationType.SINK).contains(node);
	}

	/**
	 * Given a source and a sink instructions, computes a chop of these two program parts and collects all instructions which are on the way.
	 * This works only for sequential programs
	 * @param source source instruction
	 * @param sink sink instruction
	 * @return instructions through which information may flow from source to sink
	 */
	public Set<SDGInstruction> computeInstructionChop(SDGProgramPart source, SDGProgramPart sink) {
		Chopper chopper = new NonSameLevelChopper(this.sdg);
		AnnotationTypeBasedNodeCollector c = new AnnotationTypeBasedNodeCollector(this.sdg);
		Collection<SDGNode> chop = chopper.chop(c.collectNodes(source, AnnotationType.SOURCE), c.collectNodes(sink, AnnotationType.SINK));
		Set<SDGInstruction> ret = new HashSet<SDGInstruction>();
		for (SDGNode n : chop) {
			SDGMethod m = getMethod(this.sdg.getEntry(n).getBytecodeMethod());
			SDGInstruction i = m.getInstructionWithBCIndex(n.getBytecodeIndex());
			if (i != null) {
				ret.add(i);
			}
		}

		return ret;
	}

	public Collection<SDGMethod> getMethods(String methodDesc) {
		build();
		return ppartParser.getMethods(methodDesc);
	}

	public Collection<SDGMethod> getAllMethods() {
		build();
		Set<SDGMethod> ret = new HashSet<SDGMethod>();
		for (SDGClass cl : getClasses()) {
			ret.addAll(cl.getMethods());
		}
		return ret;
	}

	public SDGProgramPart getPart(String partDesc) {
		Collection<? extends SDGProgramPart> pparts = getParts(partDesc);
		if (pparts.isEmpty()) {
			return null;
		} else {
			return pparts.iterator().next();
		}
	}

	public SDGMethod getMethod(String methodDesc) {
		Collection<SDGMethod> pparts = getMethods(methodDesc);
		if (pparts.isEmpty()) {
			return null;
		} else {
			return pparts.iterator().next();
		}
	}

	/**
	 * Parser for the different parts of the program represented by an sdg. The
	 * strings accepted by this parser have to use a bytecode notation of types
	 * and a point-separated notation of attributes and methods. So, the type
	 * java.lang.System is represented by the string "Ljava/lang/System;". If a
	 * type is part of the name of an attribute or a method, then a
	 * point-notation has to be used, so the attribute out of the type
	 * java.lang.System is denoted by java.lang.System.out. Methods are
	 * represented by their signatures, which are written (with the exception of
	 * the class part of the name) in bytecode notation. So the println() method
	 * of the type java.io.PrintStream is written as
	 * java.io.PrintStream.println(Ljava/lang/String;)V. Parameters in methods
	 * are represented by an arrow and subsequently p<number> (starting at 0),
	 * so for example java.lang.System.out.println(Ljava/lang/String;)V->p1 gets
	 * the first real parameter, ->p0 gets the this-pointer-parameter. The exit
	 * of a method is represented by the method signature followed by "->exit".
	 * Individual instructions inside the method are represented by the method
	 * signature followed by ":<number>" where <number> denotes the bytecode
	 * index of the instruction. This index has to be taken from the sdg
	 * representing the program from which the instruction is to be taken.
	 *
	 * @author Martin Mohr
	 *
	 */
	private static final class SDGProgramPartParserBC {

		private final SDGProgram program;

		public SDGProgramPartParserBC(SDGProgram program) {
			this.program = program;
		}

		private Collection<SDGClass> getClasss(String className) {
			JavaType typeName = JavaType.parseSingleTypeFromString(className, Format.BC);
			if (typeName == null) {
				return null;
			} else {
				return program.getClass(typeName);
			}
		}

		private Collection<SDGAttribute> getAttributes(String fullAttributeName) {
			if (!fullAttributeName.contains(".")) {
				return null;
			} else {
				int dotIndex = fullAttributeName.lastIndexOf(".");
				String declaringTypeSrc = fullAttributeName.substring(0, dotIndex);
				String attrName = fullAttributeName.substring(dotIndex + 1);
				JavaType declaringType = JavaType.parseSingleTypeFromString(declaringTypeSrc, Format.HR);
				if (declaringType == null) {
					return null;
				} else {
					return program.getAttribute(declaringType, attrName);
				}
			}
		}

		private Collection<SDGMethod> getMethods(String fullMethodName) {
			JavaMethodSignature mSig = JavaMethodSignature.fromString(fullMethodName);
			if (mSig == null) {
				return null;
			} else {
				return program.getMethods(mSig);
			}
		}

		private Collection<SDGMethodExitNode> getMethodExitNodes(String str) {
			str = str.replaceAll("\\s+", "");
			if (!str.endsWith("->exit")) {
				return null;
			} else {
				String methodSigSrc = str.substring(0, str.lastIndexOf("->"));
				JavaMethodSignature m = JavaMethodSignature.fromString(methodSigSrc);
				return program.getMethodExitNode(m);
			}
		}

		private Collection<SDGLocalVariable> getLocalVariables(String instr) {
			if (!instr.contains("#")) {
				return null;
			} else {
				int hashtagIndex = instr.lastIndexOf('#');
				String methodSrc = instr.substring(0, hashtagIndex);
				JavaMethodSignature m = JavaMethodSignature.fromString(methodSrc);
				if (m == null) {
					return null;
				} else {
					String name = instr.substring(hashtagIndex + 1);
					return program.getLocalVariables(m, name);
				}
			}
		}
		
		private Collection<SDGInstruction> getInstructions(String instr) {
			if (!instr.contains(":")) {
				return null;
			} else {
				int colonIndex = instr.lastIndexOf(':');
				String methodSrc = instr.substring(0, colonIndex);
				JavaMethodSignature m = JavaMethodSignature.fromString(methodSrc);
				if (m == null) {
					return null;
				} else {
					String bcIndexSrc = instr.substring(colonIndex + 1);
					Integer bcIndex;
					try {
						bcIndex = Integer.parseInt(bcIndexSrc);
					} catch (NumberFormatException e) {
						return null;
					}

					return program.getInstruction(m, bcIndex);
				}
			}
		}

		private Collection<SDGFormalParameter> getMethodParameters(String str) {
			if (!str.contains("->")) {
				return null;
			} else {
				int arrowLoc = str.lastIndexOf("->");
				String methodSrc = str.substring(0, arrowLoc);
				JavaMethodSignature m = JavaMethodSignature.fromString(methodSrc);
				if (m == null) {
					return null;
				} else {
					String rest = str.substring(arrowLoc + 2);
					String paramIndexSrc;
					if (!rest.startsWith("p")) {
						return null;
					} else {
						paramIndexSrc = rest.substring(1);
					}
					Integer paramIndex;
					try {
						paramIndex = Integer.parseInt(paramIndexSrc);
					} catch (NumberFormatException e) {
						return null;
					}

					return program.getMethodParameter(m, paramIndex);
				}
			}
		}

		private Collection<? extends SDGProgramPart> getProgramParts(String str) {
			str = str.replaceAll("\\s+", "");
			if (str.contains(":")) {
				return getInstructions(str);
			} else if (str.endsWith("->exit")) {
				return getMethodExitNodes(str);
			} else if (str.contains("->")) {
				return getMethodParameters(str);
			} else if (str.contains(".")) {
				if (str.contains("#")) {
					return getLocalVariables(str);
				} else if (str.contains("(")) {
					return getMethods(str);
				} else {
					return getAttributes(str);
				}
			} else {
				return getClasss(str);
			}
		}

	}

	public boolean hasDefinedEntryMethod(){
		return entryMethod.isPresent();
	}

	public String getEntryMethod(){
		return entryMethod.get();
	}
}

class SDGClassResolver {

	private static final Logger debug = Log.getLogger(Log.L_API_DEBUG);

	private Set<SDGClass> classes = new LinkedHashSet<SDGClass>();

	public SDGClassResolver() {

	}

	public SDGClassResolver(Set<SDGClass> classes) {
		this.classes = classes;
	}

	public void setClasses(Collection<SDGClass> newClasses) {
		this.classes.clear();
		this.classes.addAll(newClasses);
	}

	public Collection<SDGClass> getClass(JavaType typeName) {
		Collection<SDGClass> ret = new LinkedList<SDGClass>();
		for (SDGClass cl : classes) {
			if (debug.isEnabled()) {
				debug.outln(cl.getTypeName().toBCString() + " " + typeName.toBCString());
			}

			if (cl.getTypeName().equals(typeName)) {
				ret.add(cl);
			}
		}

		return ret;
	}

	public Collection<SDGAttribute> getAttribute(JavaType typeName, String attrName) {
		Collection<SDGClass> cl = getClass(typeName);
		if (cl.isEmpty()) {
			if (debug.isEnabled()) {
				debug.outln("class " + typeName + " not found!");
			}
		}
		Collection<SDGAttribute> ret = new LinkedList<SDGAttribute>();
		for (SDGClass c : cl) {
			for (SDGAttribute a : c.getAttributes()) {
				if (attrName.equals(a.getName())) {
					ret.add(a);
				}
			}
		}

		return ret;
	}

	public Collection<SDGMethod> getMethod(JavaType typeName, JavaMethodSignature methodSig) {
		final Collection<SDGClass> cl = getClass(typeName);
		Collection<SDGMethod> ret = new LinkedList<SDGMethod>();
		for (SDGClass c : cl) {
			for (SDGMethod m : c.getMethods()) {
				if (methodSig.equals(m.getSignature())) {
					ret.add(m);
				}
			}
		}

		return ret;
	}

	public Collection<SDGFormalParameter> getMethodParameter(JavaType typeName, JavaMethodSignature methodSig, int paramNo) {
		Collection<SDGMethod> ms = getMethod(typeName, methodSig);
		Collection<SDGFormalParameter> ret = new LinkedList<SDGFormalParameter>();
		for (SDGMethod m : ms) {
			if (m.getParameter(paramNo) != null) {
				ret.add(m.getParameter(paramNo));
			}
		}
		return ret;
	}

	public Collection<SDGInstruction> getInstruction(JavaType typeName, JavaMethodSignature methodSig, int bcIndex) {
		Collection<SDGMethod> ms = getMethod(typeName, methodSig);
		Collection<SDGInstruction> ret = new LinkedList<SDGInstruction>();
		for (SDGMethod m : ms) {
			ret.add(m.getInstructionWithBCIndex(bcIndex));
		}

		return ret;
	}

	public Collection<SDGInstruction> getInstruction(JavaType typeName, JavaMethodSignature methodSig, String labelRegEx) {
		Collection<SDGMethod> ms = getMethod(typeName, methodSig);
		Collection<SDGInstruction> ret = new LinkedList<SDGInstruction>();
		for (SDGMethod m : ms) {
			ret.addAll(m.getInstructionsWithLabelMatching(labelRegEx));
		}

		return ret;
	}
	
	public Collection<SDGLocalVariable> getLocalVariable(JavaType typeName, JavaMethodSignature methodSig, String varName) {
		Collection<SDGMethod> ms = getMethod(typeName, methodSig);
		Collection<SDGLocalVariable> ret = new LinkedList<SDGLocalVariable>();
		for (SDGMethod m : ms) {
			ret.add(m.getLocalVariable(varName));
		}
		return ret;
	}

	public Collection<SDGMethodExitNode> getMethodExitNode(JavaType typeName, JavaMethodSignature methodSig) {
		Collection<SDGMethod> ms = getMethod(typeName, methodSig);
		Collection<SDGMethodExitNode> ret = new LinkedList<SDGMethodExitNode>();
		for (SDGMethod m : ms) {
			ret.add(m.getExit());
		}
		return ret;
	}

	// public SDGProgramPart findProgramPart(SDGNode node) {
	// JavaMethodSignature sig;
	// JavaType type;
	// SDGNode entry;
	// SDGMethod m;
	// switch (node.getKind()) {
	// case ENTRY:
	// // method
	// sig = JavaMethodSignature.fromString(node.getBytecodeName());
	// type = sig.getDeclaringType();
	// return getMethod(type, sig);
	// case FORMAL_IN:
	// case FORMAL_OUT:
	// // parameter
	// entry = console.getSDG().getEntry(node);
	// sig = JavaMethodSignature.fromString(entry.getBytecodeName());
	// type = sig.getDeclaringType();
	// m = getMethod(type, sig);
	// for (SDGParameter p : m.getParameters())
	// if (node.equals(p.getInRoot()) || node.equals(p.getOutRoot())) {
	// return p;
	// }
	// return null;
	// case EXIT:
	// // exit node
	// entry = console.getSDG().getEntry(node);
	// sig = JavaMethodSignature.fromString(entry.getBytecodeName());
	// type = sig.getDeclaringType();
	// m = getMethod(type, sig);
	// if (node.equals(m.getExit().getExitNode())) {
	// return m.getExit();
	// } else {
	// return null;
	// }
	// case ACTUAL_IN:
	// case ACTUAL_OUT:
	// // SDGNode callNode = console.getSDG().getCallSiteFor(node);
	// // sig = JavaMethodSignature.fromString(callNode.getBytecodeName());
	// // type = sig.getDeclaringType();
	// // m = getMethod(type, sig);
	// // for (SDGInstruction i : m.getInstructions()) {
	// // if (i.getSourceNodes().contains(node) ||
	// i.getSinkNodes().contains(node)) {
	// // return i;
	// // }
	// // }
	// for (SDGClass cl : classes) {
	// for (SDGAttribute a : cl.getAttributes()) {
	// if (a.covers(node)) {
	// return a;
	// }
	// }
	// }
	// return null;
	// default:
	// // instruction or attribute
	// if (node.getBytecodeIndex() >= 0) {
	// sig = JavaMethodSignature.fromString(node.getBytecodeName());
	// type = sig.getDeclaringType();
	// m = getMethod(type, sig);
	// for (SDGInstruction i : m.getInstructions()) {
	// if (i.covers(node)) {
	// return i;
	// }
	// }
	//
	// for (SDGClass cl : classes) {
	// for (SDGAttribute a : cl.getAttributes()) {
	// if (a.covers(node)) {
	// return a;
	// }
	// }
	// }
	//
	// return null;
	// } else {
	// int offset = node.getBytecodeName().lastIndexOf('.');
	// String typeSrc = node.getBytecodeName().substring(0, offset);
	// String attrName = node.getBytecodeName().substring(offset + 1);
	// type = JavaType.fromString(typeSrc);
	// return getAttribute(type, attrName);
	// }
	// }
	// }

	public Set<SDGClass> getClasses() {
		return classes;
	}
}
