/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.api.sdg;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.classLoader.IField;
import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.ipa.cha.ClassHierarchyException;
import com.ibm.wala.ipa.cha.IClassHierarchy;
import com.ibm.wala.types.TypeReference;
import com.ibm.wala.types.annotations.Annotation;
import com.ibm.wala.util.CancelException;
import com.ibm.wala.util.MonitorUtil.IProgressMonitor;
import com.ibm.wala.util.graph.GraphIntegrity.UnsoundGraphException;

import edu.kit.joana.api.annotations.AnnotationType;
import edu.kit.joana.api.annotations.AnnotationTypeBasedNodeCollector;
import edu.kit.joana.ifc.sdg.core.SecurityNode;
import edu.kit.joana.ifc.sdg.graph.SDG;
import edu.kit.joana.ifc.sdg.graph.SDGNode;
import edu.kit.joana.ifc.sdg.graph.SDGSerializer;
import edu.kit.joana.ifc.sdg.graph.chopper.Chopper;
import edu.kit.joana.ifc.sdg.graph.chopper.NonSameLevelChopper;
import edu.kit.joana.ifc.sdg.mhpoptimization.MHPType;
import edu.kit.joana.ifc.sdg.mhpoptimization.PruneInterferences;
import edu.kit.joana.ifc.sdg.util.JavaMethodSignature;
import edu.kit.joana.ifc.sdg.util.JavaType;
import edu.kit.joana.ifc.sdg.util.JavaType.Format;
import edu.kit.joana.util.Log;
import edu.kit.joana.util.Logger;
import edu.kit.joana.util.Stubs;
import edu.kit.joana.util.io.IOFactory;
import edu.kit.joana.wala.core.NullProgressMonitor;
import edu.kit.joana.wala.core.SDGBuilder;
import edu.kit.joana.wala.core.SDGBuilder.PointsToPrecision;
import edu.kit.joana.wala.util.pointsto.ObjSensZeroXCFABuilder;

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
	private final Set<SDGClass> classes = new HashSet<SDGClass>();
	private final SDG sdg;
	private SDGProgramPartParserBC ppartParser;
	private final Map<SDGProgramPart, Collection<Annotation>> annotations = new HashMap<SDGProgramPart, Collection<Annotation>>();
	private final AnnotationTypeBasedNodeCollector coll;

	private static Logger debug = Log.getLogger(Log.L_API_DEBUG);

	public SDGProgram(SDG sdg) {
		this.sdg = sdg;
		this.ppartParser = new SDGProgramPartParserBC(this);
		this.classComp = new SDGClassComputation(sdg);
		this.coll = new AnnotationTypeBasedNodeCollector(sdg, this.classComp);
		this.coll.init(this);
	}

	public static SDGProgram loadSDG(String path) throws IOException {
		return new SDGProgram(SDG.readFrom(path, new SecurityNode.SecurityNodeFactory()));
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
		JavaMethodSignature mainMethod = JavaMethodSignature.fromString(config.getEntryMethod());// JavaMethodSignature.mainMethodOfClass(config.getMainClass());
		SDGBuildPreparation.Config cfg = new SDGBuildPreparation.Config(mainMethod.toBCString(), mainMethod.toBCString(), config.getClassPath(),
				config.getFieldPropagation());
		cfg.thirdPartyLibPath = config.getThirdPartyLibsPath();
		cfg.exceptions = config.getExceptionAnalysis();
		cfg.defaultExceptionMethodState = config.getDefaultExceptionMethodState();
		cfg.pts = config.getPointsToPrecision();
		cfg.accessPath = config.computeAccessPaths();
		cfg.sideEffects = config.getSideEffectDetectorConfig();
		cfg.stubs = config.getStubsPath().getPath();
		cfg.nativesXML = config.getNativesXML();
		cfg.pruningPolicy = config.getPruningPolicy();
		cfg.exclusions = config.getExclusions();
		cfg.computeAllocationSites = config.computeAllocationSites();
		debug.outln(cfg.stubs);

		if (config.computeInterferences()) {
			cfg.pts = PointsToPrecision.OBJECT_SENSITIVE;
			if (config.getPointsToPrecision() == PointsToPrecision.OBJECT_SENSITIVE && config.getMethodFilter() != null) {
				cfg.objSensFilter = new MethodFilterChain(new ThreadSensitiveMethodFilterWithCaching(),	config.getMethodFilter());
			} else {
				cfg.objSensFilter = new ThreadSensitiveMethodFilterWithCaching();
			}
		} else {
			cfg.objSensFilter = config.getMethodFilter();
		}
		monitor.beginTask("build SDG", 20);
		final com.ibm.wala.util.collections.Pair<SDG, SDGBuilder> p =
				SDGBuildPreparation.computeAndKeepBuilder(out, cfg,	config.computeInterferences(), monitor);
		final SDG sdg = p.fst;
		final SDGBuilder builder = p.snd;

		if (config.computeInterferences()) {
			PruneInterferences.preprocessAndPruneCSDG(sdg, config.getMhpType());
		}
		if (sdgFileOut != null) {
			SDGSerializer.toPDGFormat(sdg, sdgFileOut);
			sdgFileOut.flush();
		}
		SDGProgram ret = new SDGProgram(sdg);

		// TODO: Iterate only over classes present in the call graph
		for (IClass c : builder.getClassHierarchy()) {
			final String walaClassName = c.getName().toString();
			final JavaType jt = JavaType.parseSingleTypeFromString(walaClassName, Format.BC);

			for (IField f : c.getAllFields()) {
				final Collection<SDGAttribute> attributes = ret.getAttribute(jt, f.getName().toString());
				// attributes.isEmpty() if c isn't Part of the CallGraph
				if (f.getAnnotations() != null && !f.getAnnotations().isEmpty()) {
					for (SDGAttribute a : attributes)
						ret.annotations.put(a, f.getAnnotations());
					debug.outln("Annotated: " + jt + ":::" + f.getName() + " with " + f.getAnnotations());
				}

			}

			for (IMethod m : c.getAllMethods()) {
				if (m.getAnnotations() != null && !m.getAnnotations().isEmpty()) {
					final Collection<SDGMethod> methods = ret.getMethods(JavaMethodSignature.fromString(m
							.getSignature()));
					for (SDGMethod sdgm : methods)
						ret.annotations.put(sdgm, m.getAnnotations());
					debug.outln("Annotated: " + jt + ":::" + m.getName() + " with " + m.getAnnotations());
				}

			}

		}

		return ret;
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

	public Map<SDGProgramPart, Collection<Annotation>> getJavaSourceAnnotations() {
		return annotations;
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
		if (classComp.getNodes(i).isEmpty()) {
			return null;
		} else {
			SDGNode n = classComp.getNodes(i).iterator().next();
			if (n.getKind() == SDGNode.Kind.ENTRY) {
				return ClassLoader.fromSDGNode(n);
			} else {
				return ClassLoader.fromSDGNode(sdg.getEntry(n));
			}
		}
	}

	public ClassLoader getClassLoader(SDGMethod m) {
		if (classComp.getEntries(m).isEmpty()) {
			return null;
		} else {
			return ClassLoader.fromSDGNode(classComp.getEntries(m).iterator().next());
		}
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

	public SDGProgramPart findCoveringProgramPart(SDGNode node) {
		Set<SDGProgramPart> candidates = coll.getCoveringCandidates(node);
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
				if (str.contains("(")) {
					return getMethods(str);
				} else {
					return getAttributes(str);
				}
			} else {
				return getClasss(str);
			}
		}

	}

}

class SDGClassResolver {

	private static final Logger debug = Log.getLogger(Log.L_API_DEBUG);

	private Set<SDGClass> classes = new HashSet<SDGClass>();

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

class ThreadSensitiveMethodFilter implements ObjSensZeroXCFABuilder.MethodFilter {

	@Override
	public boolean engageObjectSensitivity(IMethod m) {
		IClassHierarchy cha = m.getClassHierarchy();
		return !m.isStatic()
				&& (cha.isAssignableFrom(cha.lookupClass(TypeReference.JavaLangThread), m.getDeclaringClass()));
	}

	@Override
	public int getFallbackCallsiteSensitivity() {
		return 1;
	}

	@Override
	public boolean restrictToOneLevelObjectSensitivity(IMethod m) {
		return true;
	}

}

/**
 * Method filter which chains together two sub-filters F1 and F2.
 * <p/>
 * The intended use case is to have a method filter, which is at least as
 * object-sensitive as both F1 and F2, with a slight preference of F1 if neither
 * F1 nor F2 find a given method interesting enough to distinguish its object
 * contexts.
 * <p/>
 *
 * It works as follows:
 * <ul>
 * <li>For engageObjectSensitivity(), it first asks F1 - if F1 says
 * {@code false}, then F2 is asked.</li>
 * <li>For fallback callsite sensitivity, F1's return value is taken.</li>
 * </ul>
 *
 * @author Martin Mohr
 */
class MethodFilterChain implements ObjSensZeroXCFABuilder.MethodFilter {

	/** the first filter to be used */
	private ObjSensZeroXCFABuilder.MethodFilter filter1;

	/** the second filter to be used */
	private ObjSensZeroXCFABuilder.MethodFilter filter2;

	/**
	 * Chains together this method filter from the two given method filters
	 *
	 * @param filter1
	 *            first filter to be used
	 * @param filter2
	 *            'fall back filter' to be used if the first filter says that
	 *            object sensitivity shall not be engaged
	 */
	MethodFilterChain(ObjSensZeroXCFABuilder.MethodFilter filter1, ObjSensZeroXCFABuilder.MethodFilter filter2) {
		this.filter1 = filter1;
		this.filter2 = filter2;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see
	 * edu.kit.joana.wala.flowless.wala.ObjSensContextSelector.MethodFilter#
	 * engageObjectSensitivity(com.ibm.wala.classLoader.IMethod)
	 */
	@Override
	public boolean engageObjectSensitivity(IMethod m) {
		if (filter1.engageObjectSensitivity(m)) {
			return true;
		} else {
			return filter2.engageObjectSensitivity(m);
		}
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see
	 * edu.kit.joana.wala.flowless.wala.ObjSensContextSelector.MethodFilter#
	 * getFallbackCallsiteSensitivity()
	 */
	@Override
	public int getFallbackCallsiteSensitivity() {
		return filter1.getFallbackCallsiteSensitivity();
	}

	/* (non-Javadoc)
	 * @see edu.kit.joana.wala.flowless.wala.ObjSensContextSelector.MethodFilter#restrictToOneLevelObjectSensitivity(com.ibm.wala.classLoader.IMethod)
	 */
	@Override
	public boolean restrictToOneLevelObjectSensitivity(IMethod m) {
		return filter1.restrictToOneLevelObjectSensitivity(m) && filter2.restrictToOneLevelObjectSensitivity(m);
	}

}

class ThreadSensitiveMethodFilterWithCaching implements ObjSensZeroXCFABuilder.MethodFilter {
	private final Set<IClass> threadClasses = new HashSet<IClass>();
	private final Set<IClass> nonThreadClasses = new HashSet<IClass>();
	private final ThreadSensitiveMethodFilter normalFilter = new ThreadSensitiveMethodFilter();
	private IClass javaLangThread = null;

	@Override
	public int getFallbackCallsiteSensitivity() {
		return 1;
	}

	private void initJavaLangThread(IClassHierarchy cha) {
		if (javaLangThread == null) {
			javaLangThread = cha.lookupClass(TypeReference.JavaLangThread);
		}
	}

	@Override
	public boolean engageObjectSensitivity(IMethod m) {
		if (nonThreadClasses.contains(m.getDeclaringClass())) {
			return false;
		} else if (threadClasses.contains(m.getDeclaringClass())) {
			return !m.isStatic();
		} else {
			IClassHierarchy cha = m.getClassHierarchy();
			initJavaLangThread(cha);
			boolean ret = normalFilter.engageObjectSensitivity(m);
			if (ret) {
				threadClasses.add(m.getDeclaringClass());
			} else {
				nonThreadClasses.add(m.getDeclaringClass());
			}
			return ret;
		}
	}

	@Override
	public boolean restrictToOneLevelObjectSensitivity(IMethod m) {
		return normalFilter.restrictToOneLevelObjectSensitivity(m);
	}
}
