/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.api.sdg;

import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.ipa.cha.ClassHierarchyException;
import com.ibm.wala.ipa.cha.IClassHierarchy;
import com.ibm.wala.types.TypeReference;
import com.ibm.wala.util.CancelException;
import com.ibm.wala.util.MonitorUtil.IProgressMonitor;
import com.ibm.wala.util.graph.GraphIntegrity.UnsoundGraphException;

import edu.kit.joana.api.annotations.IFCAnnotation;
import edu.kit.joana.api.annotations.IFCAnnotation.Type;
import edu.kit.joana.ifc.sdg.core.SecurityNode;
import edu.kit.joana.ifc.sdg.graph.SDG;
import edu.kit.joana.ifc.sdg.graph.SDGEdge;
import edu.kit.joana.ifc.sdg.graph.SDGNode;
import edu.kit.joana.ifc.sdg.graph.SDGNode.NodeFactory;
import edu.kit.joana.ifc.sdg.graph.SDGNode.Operation;
import edu.kit.joana.ifc.sdg.mhpoptimization.CSDGPreprocessor;
import edu.kit.joana.ifc.sdg.util.BytecodeLocation;
import edu.kit.joana.ifc.sdg.util.JavaMethodSignature;
import edu.kit.joana.ifc.sdg.util.JavaType;
import edu.kit.joana.ifc.sdg.util.JavaType.Format;
import edu.kit.joana.ifc.sdg.util.Pair;
import edu.kit.joana.ifc.sdg.util.io.Print2Nirvana;
import edu.kit.joana.util.Log;
import edu.kit.joana.util.Logger;
import edu.kit.joana.util.Stubs;
import edu.kit.joana.wala.core.Main;
import edu.kit.joana.wala.core.NullProgressMonitor;
import edu.kit.joana.wala.core.SDGBuilder.PointsToPrecision;
import edu.kit.joana.wala.flowless.wala.ObjSensContextSelector.MethodFilter;

public class SDGProgram {

	private boolean isBuilt = false;
	private final SDGClassResolver classRes = new SDGClassResolver();
	private final Set<SDGClass> classes = new HashSet<SDGClass>();
	private final SDG sdg;
	private SDGProgramPartParserBC ppartParser;
	private static Logger debug = Log.getLogger(Log.L_API_DEBUG);

	public SDGProgram(SDG sdg) {
		this.sdg = sdg;
		this.ppartParser = new SDGProgramPartParserBC(this);
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
			return createSDGProgram(classPath, entryMethod, null, computeInterference, mhpType, new Print2Nirvana(),
					NullProgressMonitor.INSTANCE);
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
		return createSDGProgram(config, new Print2Nirvana(), NullProgressMonitor.INSTANCE);
	}

	public static SDGProgram createSDGProgram(SDGConfig config, PrintStream out, IProgressMonitor monitor)
			throws ClassHierarchyException, IOException, UnsoundGraphException, CancelException {
		JavaMethodSignature mainMethod = JavaMethodSignature.fromString(config.getEntryMethod());// JavaMethodSignature.mainMethodOfClass(config.getMainClass());
		Main.Config cfg = new Main.Config(mainMethod.toBCString(), mainMethod.toBCString(), config.getClassPath(),
				config.getFieldPropagation());
		cfg.exceptions = config.getExceptionAnalysis();
		cfg.pts = config.getPointsToPrecision();
		cfg.accessPath = config.computeAccessPaths();
		cfg.stubs = config.getStubsPath().getPath();

		debug.outln(cfg.stubs);

		if (config.computeInterferences()) {
			cfg.pts = PointsToPrecision.OBJECT_SENSITIVE;
			cfg.objSensFilter = new ThreadSensitiveMethodFilterWithCaching();
		}
		monitor.beginTask("build SDG", 20);
		SDG sdg = Main.compute(out, cfg, config.computeInterferences(), monitor);
		if (config.computeInterferences()) {
			// CSDGPreprocessor p = new CSDGPreprocessor(sdg);
			switch (config.getMhpType()) {
			case NONE:
				CSDGPreprocessor.justPrecprocess(sdg);
				break;
			case SIMPLE:
				CSDGPreprocessor.runMHP(sdg, CSDGPreprocessor.MHPPrecision.SIMPLE);
				break;
			case PRECISE:
				CSDGPreprocessor.runMHP(sdg, CSDGPreprocessor.MHPPrecision.PRECISE);
				break;
			default:
				throw new IllegalStateException();
			}
		}
		SDGProgram ret = new SDGProgram(sdg);
		return ret;
	}

	private void build() {
		if (!isBuilt) {
			this.classes.addAll(new SDGClassComputation(sdg).compute());
			this.classRes.setClasses(classes);
			isBuilt = true;
		}
	}

	public SDG getSDG() {
		return sdg;
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
	
	public Collection<SDGInstruction> getCallsToMethod(JavaMethodSignature tgt) {
		Collection<SDGInstruction> ret = new LinkedList<SDGInstruction>();
		build();
		for (SDGClass cl : getClasses()) {
			for (SDGMethod m : cl.getMethods()) {
				ret.addAll(m.getAllCalls(tgt));
			}
		}
		return ret;
	}

	public Collection<SDGMethodExitNode> getMethodExitNode(JavaMethodSignature methodSig) {
		build();
		return classRes.getMethodExitNode(methodSig.getDeclaringType(), methodSig);
	}

	public Collection<SDGParameter> getMethodParameter(JavaMethodSignature methodSig, int paramIndex) {
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
		for (SDGProgramPart ppart : getAllProgramParts()) {
			if (ppart.covers(node)) {
				return ppart.getCoveringComponent(node);
			}
		}
		
		debug.outln("node " + node + " has no program part!");
		return null;
	}

	public Collection<SDGMethod> getMethods(String methodDesc) {
		build();
		return ppartParser.getMethods(methodDesc);
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

		private Collection<SDGParameter> getMethodParameters(String str) {
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

final class SDGClassComputation {

	private static final Logger debug = Log.getLogger(Log.L_API_DEBUG);

	private final SDG sdg;
	private final Map<JavaType, Set<SDGNode>> seenClasses = new HashMap<JavaType, Set<SDGNode>>();
	private final Map<JavaType, Map<String, Pair<Set<SDGNode>, Set<SDGNode>>>> seenAttributes = new HashMap<JavaType, Map<String, Pair<Set<SDGNode>, Set<SDGNode>>>>();
	private final Map<JavaType, Set<SDGNode>> seenMethods = new HashMap<JavaType, Set<SDGNode>>();

	SDGClassComputation(SDG sdg) {
		this.sdg = sdg;
	}

	private void seenDeclaration(SDGNode declNode) {
		if (debug.isEnabled()) {
			debug.outln("seen declaration node " + declNode + " of type " + declNode.getType());
		}
		JavaType type = JavaType.parseSingleTypeFromString(declNode.getType(), Format.BC);
		addDeclarationNode(type, declNode);

	}

	private void addDeclarationNode(JavaType type, SDGNode declNode) {
		seenClass(type);
		seenClasses.get(type).add(declNode);
	}

	private void seenMethod(SDGNode entry) {
		if (entry.getBytecodeName() != null) {
			int offset = entry.getBytecodeName().lastIndexOf('.');
			if (offset >= 0) {
				JavaType typeName = JavaType.parseSingleTypeFromString(entry.getBytecodeName().substring(0, offset),
						Format.HR);
				assert typeName != null;
				seenClass(typeName);

				Set<SDGNode> declaredMethods;
				if (!seenMethods.containsKey(typeName)) {
					declaredMethods = new HashSet<SDGNode>();
					seenMethods.put(typeName, declaredMethods);
				} else {
					declaredMethods = seenMethods.get(typeName);
				}
				declaredMethods.add(entry);
			}
		}
	}

	private void seenAttribute(SDGNode node, Operation op) {

		Set<SDGNode> fieldNodes = new HashSet<SDGNode>();
		// Set<SDGNode> fieldNodes = new HashSet<SDGNode>();

		for (SDGEdge e : sdg.outgoingEdgesOf(node)) {
			if (e.getKind() == SDGEdge.Kind.CONTROL_DEP_EXPR) {
				int bcIndex = e.getTarget().getBytecodeIndex();
				if (bcIndex == BytecodeLocation.STATIC_FIELD || bcIndex == BytecodeLocation.OBJECT_FIELD
						|| bcIndex == BytecodeLocation.ARRAY_FIELD) {
					fieldNodes.add(e.getTarget());
				}
			}
		}

		for (SDGNode fNode : fieldNodes) {
			String bcMethod = fNode.getBytecodeName();
			int offset = bcMethod.lastIndexOf('.');
			if (offset >= 0) {
				JavaType typeName = JavaType.parseSingleTypeFromString(bcMethod.substring(0, offset), Format.BC);
				String attrName = bcMethod.substring(offset + 1);
				addAttributeNode(typeName, attrName, fNode, Type.SOURCE);
				addAttributeNode(typeName, attrName, fNode, Type.SINK);
				addDDReachableSinkNodes(typeName, attrName, fNode);
			}
		}
	}

	// TODO: Why is this kind of semi-interprocedural data-forward-slice
	// sufficient???
	private void addDDReachableSinkNodes(JavaType declaringClass, String attrName, SDGNode start) {
		if (start.getBytecodeIndex() == BytecodeLocation.ARRAY_FIELD
				|| start.getBytecodeIndex() == BytecodeLocation.OBJECT_FIELD
				|| start.getBytecodeIndex() == BytecodeLocation.STATIC_FIELD) {

			LinkedList<SDGNode> worklist = new LinkedList<SDGNode>();
			Set<SDGNode> done = new HashSet<SDGNode>();
			worklist.add(start);
			while (!worklist.isEmpty()) {
				SDGNode next = worklist.poll();

				for (SDGEdge e : sdg.outgoingEdgesOf(next)) {
					SDGNode n = e.getTarget();
					switch (e.getKind()) {
					case DATA_ALIAS:
					case DATA_DEP:
					case DATA_DEP_EXPR_REFERENCE:
					case DATA_DEP_EXPR_VALUE:
					case DATA_HEAP:
					case DATA_LOOP:
					case SUMMARY:
						if (!done.contains(n)) {
							worklist.add(n);
						}
						if (n.getKind() == SDGNode.Kind.ACTUAL_OUT || n.getOperation() == Operation.MODIFY) {
							addAttributeNode(declaringClass, attrName, n, Type.SINK);
						}
						done.add(n);
						break;
					default:
						break;
					}
				}
			}
		}
	}

	private void seenClass(JavaType type) {
		if (!seenClasses.containsKey(type)) {
			seenClasses.put(type, new HashSet<SDGNode>());
		}
	}

	private void addAttributeNode(JavaType declaringClass, String attrName, SDGNode node, IFCAnnotation.Type type) {
		seenClass(declaringClass);
		Map<String, Pair<Set<SDGNode>, Set<SDGNode>>> attrMap;
		Set<SDGNode> attrSrcs;
		Set<SDGNode> attrSnks;
		if (!seenAttributes.containsKey(declaringClass)) {
			attrMap = new HashMap<String, Pair<Set<SDGNode>, Set<SDGNode>>>();
			seenAttributes.put(declaringClass, attrMap);
		} else {
			attrMap = seenAttributes.get(declaringClass);
		}

		if (!attrMap.containsKey(attrName)) {
			attrSrcs = new HashSet<SDGNode>();
			attrSnks = new HashSet<SDGNode>();
			attrMap.put(attrName, Pair.pair(attrSrcs, attrSnks));
		} else {
			Pair<Set<SDGNode>, Set<SDGNode>> p = attrMap.get(attrName);
			attrSrcs = p.getFirst();
			attrSnks = p.getSecond();
		}

		if (type == Type.SOURCE)
			attrSrcs.add(node);
		else {
			attrSnks.add(node);
		}
	}

	public List<SDGClass> compute() {
		List<SDGClass> result = new ArrayList<SDGClass>();

		for (SDGNode node : sdg.vertexSet()) {
			switch (node.getKind()) {
			case ENTRY:
				seenMethod(node);
				break;
			default:
				switch (node.getOperation()) {
				case ASSIGN:
				case MODIFY:
				case REFERENCE:
					if (node.getBytecodeIndex() >= 0) {
						seenAttribute(node, node.getOperation());
					}
					break;
				case DECLARATION:
					seenDeclaration(node);
					break;
				default:
					break;
				}
			}
		}

		for (JavaType typeName : seenClasses.keySet()) {
			result.add(new SDGClass(typeName, seenClasses.get(typeName), seenAttributes.get(typeName), seenMethods
					.get(typeName), sdg));
		}

		return result;
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

	public Collection<SDGParameter> getMethodParameter(JavaType typeName, JavaMethodSignature methodSig, int paramNo) {
		Collection<SDGMethod> ms = getMethod(typeName, methodSig);
		Collection<SDGParameter> ret = new LinkedList<SDGParameter>();
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

class ThreadSensitiveMethodFilter implements MethodFilter {

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

}

class ThreadSensitiveMethodFilterWithCaching implements MethodFilter {
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
}
