/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */

package edu.kit.joana.ifc.sdg.qifc.nildumu;

import static edu.kit.joana.ifc.sdg.qifc.nildumu.BasicLogger.*;
import static edu.kit.joana.ifc.sdg.qifc.nildumu.Lattices.bl;
import static edu.kit.joana.ifc.sdg.qifc.nildumu.Lattices.vl;

import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.PriorityQueue;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import com.google.common.collect.BiMap;
import com.google.common.collect.ComparisonChain;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.Iterators;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ssa.IR;
import com.ibm.wala.ssa.ISSABasicBlock;
import com.ibm.wala.ssa.SSACFG;
import com.ibm.wala.ssa.SSAInstruction;
import com.ibm.wala.ssa.SSAInvokeInstruction;
import com.ibm.wala.ssa.SymbolTable;

import edu.kit.joana.api.IFCAnalysis;
import edu.kit.joana.api.annotations.AnnotationType;
import edu.kit.joana.api.sdg.SDGFormalParameter;
import edu.kit.joana.api.sdg.SDGMethod;
import edu.kit.joana.api.sdg.SDGProgram;
import edu.kit.joana.api.sdg.SDGProgramPartVisitor;
import edu.kit.joana.ifc.sdg.graph.SDG;
import edu.kit.joana.ifc.sdg.graph.SDGEdge;
import edu.kit.joana.ifc.sdg.graph.SDGNode;
import edu.kit.joana.ifc.sdg.graph.SDGNode.Kind;
import edu.kit.joana.ifc.sdg.lattice.IStaticLattice;
import edu.kit.joana.ifc.sdg.qifc.nildumu.Lattices.B;
import edu.kit.joana.ifc.sdg.qifc.nildumu.Lattices.Sec;
import edu.kit.joana.ifc.sdg.qifc.nildumu.Lattices.Value;
import edu.kit.joana.ifc.sdg.qifc.nildumu.interproc.MethodInvocationHandler;
import edu.kit.joana.ifc.sdg.qifc.nildumu.ui.CodeUI;
import edu.kit.joana.ifc.sdg.qifc.nildumu.ui.Config;
import edu.kit.joana.ifc.sdg.qifc.nildumu.ui.EntryPoint;
import edu.kit.joana.ifc.sdg.qifc.nildumu.ui.OutputMethod;
import edu.kit.joana.ifc.sdg.qifc.nildumu.util.DefaultMap;
import edu.kit.joana.ifc.sdg.qifc.nildumu.util.NildumuException;
import edu.kit.joana.ifc.sdg.qifc.nildumu.util.StablePriorityQueue;
import edu.kit.joana.ifc.sdg.qifc.nildumu.util.Util;
import edu.kit.joana.ifc.sdg.qifc.nildumu.util.Util.Box;
import edu.kit.joana.ifc.sdg.util.JavaMethodSignature;
import edu.kit.joana.ifc.sdg.util.JavaType;
import edu.kit.joana.ui.annotations.Source;
import edu.kit.joana.util.Pair;
import edu.kit.joana.wala.core.PDG;
import edu.kit.joana.wala.core.PDGNode;
import edu.kit.joana.wala.core.SDGBuilder;

/**
 * Contains all static information on the program and helper methods
 */
public class Program {
	
	public static class UnsupportedType extends NildumuException {
		public UnsupportedType(JavaType type) {
			super(String.format("Type %s is not supported", type.toHRString()));
		}
	}
	
	private static class Tmp {
		@Config
		void func() {}
	}
	
	public static final String DEFAULT_MAIN_METHOD_NAME = "program";

	public final IFCAnalysis ana;
	
	public final SDGBuilder builder;
	
	public final SDG sdg;
	
	public final IStaticLattice<String> lattice;
	
	public final BiMap<SDGNode, Method> entryToMethod;
	
	public final BiMap<String, Method> bcNameToMethod;
	
	public final int intWidth;
	
	public final Method main;
	
	public final Context context;
	
	public Program(BuildResult build) {
		this(build, null);
	}
	
	public Program(BuildResult build, java.lang.reflect.Method mainMethod) {
		super();
		this.ana = build.analysis;
		this.builder = build.builder;
		this.sdg = ana.getProgram().getSDG();
		this.entryToMethod = HashBiMap.create(ana.getProgram().getSDG().sortByProcedures().keySet().stream().collect(Collectors.toMap(n -> n, 
				n -> new Method(this,
						ana.getProgram().getAllMethods().stream()
							.filter(m -> 
								n.getBytecodeMethod().equals(m.getSignature().toBCString())
								).findFirst().get(), n, () -> calculateCFGDoms(n))
				)));
		this.bcNameToMethod = HashBiMap.create(entryToMethod.values().stream().collect(Collectors.toMap(m -> m.toBCString(), m -> m)));
		this.main = entryToMethod.values().stream().filter(m -> {
			if (mainMethod == null) {
				return m.method.getSignature().getMethodName().equals(Program.DEFAULT_MAIN_METHOD_NAME);
			} else {
				return getJavaMethodForSignatureIfPossible(m.method.getSignature()).map(mainMethod::equals).orElse(false);
			}
		}).findFirst().get();
		java.lang.reflect.Method mMethod = getJavaMethodForSignature(main.method.getSignature());
		lattice = ana.getLattice();
		Config defaultConfig = null;
		try {
			defaultConfig = Tmp.class.getDeclaredMethods()[0].getAnnotation(Config.class);
		} catch (SecurityException e) {
			e.printStackTrace();
		}
		Config config = mMethod.getAnnotationsByType(Config.class).length > 0 ? 
				mMethod.getAnnotationsByType(Config.class)[0] : 
			    defaultConfig;
		this.intWidth = config.intWidth();
		this.context = new Context(this);
		check();
		initContext();
	}

	private void initContext(){
		java.lang.reflect.Method mainMethod = getJavaMethodForSignature(main.method.getSignature());
		main.getParameters().forEach(p -> {
				Pair<Source, String> ap = Util.get(ana.getJavaSourceAnnotations().getFirst().get(p));
				Parameter param = mainMethod.getParameters()[p.getIndex() - 1];
				int bitWidth = bitWidthForType(p.getType());
				Value val;
				if (param.isAnnotationPresent(edu.kit.joana.ifc.sdg.qifc.nildumu.ui.Value.class)) {
					val = vl.parse(param.getAnnotation(edu.kit.joana.ifc.sdg.qifc.nildumu.ui.Value.class).value());
				} else {
					val = createUnknownValue(bitWidth);
				}
				val.description(param.getName());
				Sec<?> sec = ap.getFirst().level() == null ? context.sl.top() : context.sl.parse(ap.getFirst().level());
				context.setParamValue(p.getIndex(), val);
				context.addInputValue(sec, val);
			});
	}
	
	public Value createUnknownValue(JavaType type) {
		return createUnknownValue(bitWidthForType(type));
	}
	
	public int bitWidthForType(JavaType type) {
		int bitWidth = intWidth;
		switch (type.toHRString()) {
		case "boolean":
			bitWidth = 1;
			break;
		case "byte":
		case "char":
			bitWidth = 8;
			break;
		case "short":
			bitWidth = 16;
		}
		return bitWidth;
	}
	
	private BasicBlockGraph calculateCFGDoms(SDGNode entry){
		SSACFG cfg = getProcIR(entry).getControlFlowGraph();
		return new BasicBlockGraph(this, cfg);
	}
	
	private void check() {
		List<String> errors = new ArrayList<>();
		ana.getAnnotations().stream().forEach(a -> {
			boolean hasError = false;
			if (a.getProgramPart().getClass() == SDGFormalParameter.class) {
				SDGMethod method = ((SDGFormalParameter)a.getProgramPart()).getOwningMethod();
				hasError = !getJavaMethodForSignature(method.getSignature()).isAnnotationPresent(EntryPoint.class);
			} else {
				hasError = true;
			}
			if (hasError) {
		//		errors.add(String.format("Annotations are only allowed for parameters"
	//					+ " of an EntryPoint annotated method, annotation: %s", a));
			}
			if (a.getType() != AnnotationType.SOURCE) {
				errors.add(String.format("Only SOURCE annotations are allowed, annotation: %s", a));
			}
		});
		main.method.getParameters().stream()
		.filter(p -> !ana.getAnnotations().stream().anyMatch(a -> a.getProgramPart().equals(p)))
		.forEach(p -> errors.add(String.format("Parameter %s of method %s is not annotated",
				p, main.method.getSignature().toHRString())));
		if (errors.size() > 0) {
			throw new NildumuException(String.join("\n", errors));
		}
	}
	
	private Class<?> getMainClass() {
		return classForType(main.method.getSignature().getDeclaringType()).get();
	}
	
	public static Optional<Class<?>> classForType(JavaType type){
		switch (type.toHRString()) {
		case "int":
			return Optional.of(int.class);
		case "short":
			return Optional.of(short.class);
		case "char":
			return Optional.of(char.class);
		case "byte":
			return Optional.of(byte.class);
		case "boolean":
			return Optional.of(boolean.class);
		}
		try {
			return Optional.of(Class.forName(type.toHRString()));
		} catch (ClassNotFoundException e) {
		}
		return Optional.empty();
	}

	public SDGProgram getProgram() {
		return ana.getProgram();
	}
	
	public <R, D> R accept(SDGProgramPartVisitor<R, D> visitor, D data) {
		return main.method.acceptVisitor(visitor, data);
	}
	
	public SDG getSDG() {
		return getProgram().getSDG();
	}
	
	public Stream<SDGNode> getDataDependencies(SDGNode node){
		return sdg.getIncomingEdgesOfKind(node, SDGEdge.Kind.DATA_DEP)
				.stream().map(SDGEdge::getSource);
	}
	
	public Stream<SDGNode> getDataDependentOn(SDGNode node){
		return sdg.getOutgoingEdgesOfKind(node, SDGEdge.Kind.DATA_DEP)
				.stream().map(SDGEdge::getTarget);
	}
	
	/**
	 * Handles actual nodes of method calls: returns the method call site for it
	 */
	public Stream<SDGNode> getDataDependenciesWC(SDGNode node){
		return sdg.getIncomingEdgesOfKind(node, SDGEdge.Kind.DATA_DEP)
				.stream().map(SDGEdge::getSource).map(n -> {
					if (n.kind == Kind.ACTUAL_OUT && !n.getLabel().equals("ret _exception_")) {
						return sdg.getIncomingEdgesOfKind(n, SDGEdge.Kind.CONTROL_DEP_EXPR)
								.iterator().next().getSource();
					}
					return n;
				});
	}
	
	private boolean filterUninterestingNodes(SDGNode n) {
		return 	Arrays.asList(Kind.NORMAL, Kind.EXPRESSION, Kind.PREDICATE, Kind.CALL).contains(n.kind)
				&& !n.getLabel().endsWith("_exception_")
				&& !Arrays.asList("CALL_RET").contains(n.getLabel())
				&& !n.getLabel().equals("many2many");
	}
	
	public static interface NextBlockFilter extends Predicate<ISSABasicBlock> {
		public void clear();
	}
	
	/**
	 * Adaptive workList algorithm
	 */
	public void workList(SDGNode entryNode, 
			NodeEvaluator nodeEvaluator,
			NextBlockFilter nextBlockFilter) {
		Set<SDGNode> procNodes = getSDG().getNodesOfProcedure(entryNode);
		Set<SDGNode> filteredProcNodes = procNodes.stream().filter(this::filterUninterestingNodes).collect(Collectors.toSet());

		// order inside of blocks
		List<SDGNode> topOrder = topOrder(entryNode);
		Map<SDGNode, Integer> topOrderIndex = 
				IntStream.range(0, topOrder.size()).boxed().collect(Collectors.toMap(topOrder::get, Function.identity()));
		
		//Queue<SDGNode> q = new ArrayDeque<>();
		Method method = method(entryNode);
		BasicBlockGraph bbg = method.getDoms();
		
		List<ISSABasicBlock> blockTopOrder = bbg.getElementsInTopologicalOrder();
		Map<ISSABasicBlock, Integer> blockTopOrderIndex = 
				IntStream.range(0, blockTopOrder.size()).boxed().collect(Collectors.toMap(blockTopOrder::get, Function.identity()));

		//System.err.println("Blocks in top order: " + blockTopOrder.stream().map(ISSABasicBlock::getNumber).map(Object::toString).collect(Collectors.joining(" → ")));
		
		// gather the nodes per block
		Map<ISSABasicBlock, Set<SDGNode>> nodesPerBlock = 
				procNodes.stream().filter(this::filterUninterestingNodes).collect(Collectors.groupingBy(this::getBlock,
				Collectors.toSet()));
		
		// gather the nodes per block that only data depend on out-of block nodes
		Map<ISSABasicBlock, Set<SDGNode>> outOfBlockNodesPerBlock =
				nodesPerBlock.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey,
						e -> {
							return e.getValue().stream()
									.filter(n -> getDataDependencies(n)
											.allMatch(dn -> {
												ISSABasicBlock dnBlock = getBlock(dn);
												return dnBlock == null || dnBlock.getNumber() != e.getKey().getNumber() || dn == n;
											}))
								
									.collect(Collectors.toSet());
				}));
		
		// for each node n: nodes that n data depends on and which have an altered value
		//    compared to the time of the last evaluation of n
		// the entry for each node is cleared between checking and evaluating a node
		// the map contains at the beginning for each node all its data dependencies
		Map<SDGNode, Set<SDGNode>> nodesWithNewEvaluationPerNode = 
				topOrder.stream().collect(Collectors.<SDGNode, SDGNode, Set<SDGNode>>toMap(n -> n, 
						n -> new HashSet<SDGNode>(getDataDependenciesWC(n).collect(Collectors.toSet()))));
		
		Set<SDGNode> nodesEvaluatedOnce = new HashSet<>();
		
		// blocks ordered by their loop depth
		// → higher priority to inner loop nodes
		StablePriorityQueue<ISSABasicBlock> blockQueue = 
				new StablePriorityQueue<>((a, b) -> {
					//System.err.println(a);
					assert blockTopOrderIndex.containsKey(a);
					return ComparisonChain.start().compare(-method.getLoopDepth(a), -method.getLoopDepth(b))
					.compare(blockTopOrderIndex.get(a), blockTopOrderIndex.get(b)).result();
					});
		
		// we start at the root block
		blockQueue.add(bbg.getRootElem());
		
		Map<SDGNode, Set<SDGNode>> nodesThatANodeDependsOn = new DefaultMap<>(() -> new HashSet<>());
		Map<SDGNode, Set<SDGNode>> nodesThatDependOnTheNode = new DefaultMap<>(() -> new HashSet<>());
		// the target depends on the source
		BiConsumer<SDGNode, SDGNode> addDepFromTo = (source, target) -> {
			nodesThatDependOnTheNode.get(source).add(target);
			nodesThatANodeDependsOn.get(target).add(source);
		};
		for (SDGNode node : filteredProcNodes) {
				sdg.getOutgoingEdgesOfKind(node, SDGEdge.Kind.DATA_DEP).stream()
				.map(SDGEdge::getTarget).forEach(target -> addDepFromTo.accept(node, target));
			if (getPDGNode(node).getKind() == PDGNode.Kind.PHI) {
				bbg.getPhiOperandAffectingConditionals(node).forEach(a -> addDepFromTo.accept(a.conditional, node));
			}
		}
		
		while (!blockQueue.isEmpty()) {
			
			log(() -> "Block queue: " + blockQueue.stream().map(b -> b.getNumber() + "").collect(Collectors.joining(" → ")));
			
			// we get a new block
			ISSABasicBlock curBlock = blockQueue.poll();
			
			// did something change during the evaluation of the block
			boolean somethingChanged = false;
			
			Set<SDGNode> nodes = nodesPerBlock.getOrDefault(curBlock, Collections.emptySet());
			
			// now we gather all nodes that belong to this block and do not depend data depend on
			// nodes in this block and put them into a queue			
			PriorityQueue<SDGNode> nodeQueue = new PriorityQueue<>(Comparator.comparingInt(topOrderIndex::get));
			nodeQueue.addAll(outOfBlockNodesPerBlock.getOrDefault(curBlock, Collections.emptySet()));
			
			// the inner block graph could be cyclic (loops)
			Set<SDGNode> alreadyVisited = new HashSet<>();
			
			if (isLoggingEnabled()) {
				log("Started with block " + curBlock.getNumber());
				log("----------------------------");
				logNodes("", nodeQueue.stream().collect(Collectors.toList()));
			}
			
			// now a walk through these nodes in topological order
			
			while (!nodeQueue.isEmpty()) {
				SDGNode curNode = nodeQueue.poll();
				if (alreadyVisited.contains(curNode)) {
					continue;
				}
				alreadyVisited.add(curNode);
				// a node is evaluated if either
				//   the node was not evaluated any time before in this method
				//   or the node is data dependent on a node that changed its value since
				//     the last evaluation
				if (!nodesEvaluatedOnce.contains(curNode) ||
						nodesWithNewEvaluationPerNode.get(curNode).size() > 0) {
					boolean evalChanged = nodeEvaluator.evaluate(curNode) || !nodesEvaluatedOnce.contains(curNode);
					// no node changed its value, besides possibly the node itself
					nodesWithNewEvaluationPerNode.get(curNode).clear();
					if (evalChanged) {
						// tell the nodes that data depend on it, that its value changed
						nodesThatDependOnTheNode.get(curNode).stream()
							.map(nodesWithNewEvaluationPerNode::get)
							.forEach(s -> s.add(curNode));
						// add all nodes to the queue that depend on this node and belong to the
						// current block
						nodesThatDependOnTheNode.get(curNode).stream()
							.filter(nodes::contains)
							//.filter(filteredProcNodes::contains)
							.forEach(nodeQueue::offer);
						// add all the blocks that these nodes are part of to the block queue
						// this ensures that the nodes are actually reevaluated
						nodesThatDependOnTheNode.get(curNode).stream()
							.filter(filteredProcNodes::contains)
							.map(this::getBlock).forEach(b -> {
								if (b.getNumber() != curBlock.getNumber() && !blockQueue.contains(b) && !bbg.dominators(curBlock).contains(b)) {
									blockQueue.offer(b);
								}
							});
					}
					nodesEvaluatedOnce.add(curNode);
					somethingChanged = somethingChanged || evalChanged;
				}
			}
			
			// the current block is now evaluated fully
			// we now go back to the fix point iteration 
			if (somethingChanged || nodes.isEmpty()) {
				bbg.getNextElems(curBlock).stream().filter(nextBlockFilter).filter(b -> !blockQueue.contains(b)).forEach(blockQueue::offer);
				nextBlockFilter.clear();
			}
		}
	}
	
	/**
	 * Based on the depth-first algorithm (ignores cycles): 
	 * https://en.wikipedia.org/wiki/Topological_sorting
	 */
	public List<SDGNode> topOrder(SDGNode entryNode){
		Set<SDGNode> procNodes = getSDG().getNodesOfProcedure(entryNode);
		Set<SDGNode> unmarked = new HashSet<>(getSDG().getNodesOfProcedure(entryNode));
		List<SDGNode> l = new ArrayList<>();
		Box<Consumer<SDGNode>> visit = new Box<>(null);
		visit.val = n -> {
			if (!unmarked.contains(n)) {
				return;
			}
			unmarked.remove(n);
			getSDG().outgoingEdgesOf(n).stream()
				.map(SDGEdge::getTarget)
				.filter(procNodes::contains)
				.forEach(visit.val::accept);
			l.add(n);
		};
		while (!unmarked.isEmpty()) {
			visit.val.accept(Util.get(unmarked));
		}
		Collections.reverse(l);
		return l;
	}
	
	private void logNodes(String header, List<SDGNode> nodes) {
		log(header + "\n------\n");
		nodes.stream().filter(this::filterUninterestingNodes).forEach(n -> {
			if (getBlock(n) != null) {
				log("\t%2d|%s", getBlock(n).getNumber(), toString(n));
			}
		});
	}
	
	public Method method(SDGNode entry) {
		assert entry.getKind() == Kind.ENTRY;
		return entryToMethod.get(entry);
	}
	
	public Method method(String bcString) {
		return bcNameToMethod.get(bcString);
	}
	
	public void checkType(JavaType type) {
		if (!Arrays.asList("int", "boolean").contains(type.toHRString())) {
			throw new UnsupportedType(type);
		}
	}
	
	public Method getMethodForCallSite(SDGNode callSite) {
		assert callSite.kind == Kind.CALL;
		if (callSite.getUnresolvedCallTarget() != null) {
			return method(callSite.getUnresolvedCallTarget()); 
		}
		return method(((SSAInvokeInstruction)getInstruction(callSite)).getDeclaredTarget().getSignature());
	}
	
	public boolean isOutputMethodCall(SDGNode callSite) {
		java.lang.reflect.Method method = getJavaMethodCallTarget(callSite);
		return method.getDeclaringClass().equals(CodeUI.class) && method.isAnnotationPresent(OutputMethod.class);
	}
	
	public JavaMethodSignature parseSignature(String signature) {
		return JavaMethodSignature.fromString(signature);
	}
	
	public java.lang.reflect.Method getJavaMethodForSignature(JavaMethodSignature signature){
		return getJavaMethodForSignatureIfPossible(signature).orElseGet(() -> {
			throw new NildumuException(String.format("Method %s not found", signature.getFullyQualifiedMethodName()));
		});
	}
	
	public Optional<java.lang.reflect.Method> getJavaMethodForSignatureIfPossible(JavaMethodSignature signature){
		try {
			return classForType(signature.getDeclaringType()).map(
					c -> {
						try {
							return c.getMethod(signature.getMethodName(), 
									signature.getArgumentTypes().stream().map(Program::classForType).map(Optional::get).toArray(i -> new Class[i]));
						} catch (NoSuchMethodException | SecurityException | NoSuchElementException e) {
							return null;
						}
					});
		} catch (SecurityException e) {}
		return Optional.empty();
	}
	
	public java.lang.reflect.Method getJavaMethodCallTarget(SDGNode callSite){
		if (callSite.getUnresolvedCallTarget() != null) {
			return getJavaMethodForSignature(parseSignature(callSite.getUnresolvedCallTarget()));
		}
		return getJavaMethodForSignature(parseSignature(((SSAInvokeInstruction)getInstruction(callSite)).getDeclaredTarget().getSignature()));
	}
	
	public void trialWorkListRun(SDGNode entryNode) {
		workList(entryNode, n -> {
			System.out.printf("%s:Block %d\n", toString(n), getBlock(n).getNumber());
			return false;
		}, new NextBlockFilter() {
			
			@Override
			public boolean test(ISSABasicBlock t) {
				return false;
			}
			
			@Override
			public void clear() {
			}
		}); 
	}
	
	public static String toString(SDGNode node) {
		return String.format("%s:%s", node.toString(), node.getLabel());
	}
	
	/**
	 * Prints the node and returns {@code false}
	 */
	public static boolean print(SDGNode node) {
		System.out.println(toString(node));
		return false;
	}
	
	List<SDGNode> getParamNodes(SDGNode callSite){
		assert callSite.kind == Kind.CALL;
		Method method = getMethodForCallSite(callSite);
		if (method != null) {
			
		}
		// HACK, parse label (is okay)
		return sdg.getAllActualInsForCallSiteOf(callSite).stream()
				.sorted(Comparator.comparing(n -> Integer.parseInt(n.getLabel().split(" ")[1])))
				.collect(Collectors.toList());
	}
	
	Value createUnknownValue(int width){
		assert width <= intWidth;
        return IntStream.range(0, width).mapToObj(i -> bl.create(B.U)).collect(Value.collector());
	}
	
	public void fixPointIteration() {
		context.fixPointIteration(main.entry);
	}
	
	public PDG getPDG(SDGNode node) {
		return builder.getPDGforMethod(getCGNode(node));
	}
	
	private final DefaultMap<SDGNode, SSAInstruction> nodeToInstr = 
			new DefaultMap<>((map, node) -> getPDG(node).getInstruction(getPDGNode(node)));
	
	public SSAInstruction getInstruction(SDGNode node) {
		return nodeToInstr.get(node);
	}
	
	public PDGNode getPDGNode(SDGNode node) {
		return getPDG(node).getNodeWithId(node.getId());
	}
	
	public SSAInstruction getNextInstruction(SDGNode node) {
		SSAInstruction instr = getInstruction(node);
		return  Iterators.filter(getProcIR(node).iterateAllInstructions(), i -> i.iindex > instr.iindex).next();
	}
	
	public ISSABasicBlock getNextBlock(SDGNode node) {
		return getProcIR(node).getBasicBlockForInstruction(getNextInstruction(node));
	}
	
	/**
	 * 
	 * @param node used to get the method
	 * @return
	 */
	public IR getProcIR(SDGNode node) {
		return getCGNode(node).getIR();
	}
	
	public CGNode getCGNode(SDGNode node) {
		return builder.getAllPDGs().stream().filter(n -> n.getId() == node.getProc()).map(n -> n.cgNode).findFirst().get();
	}
	
	public ISSABasicBlock getBlock(SDGNode node) {
		return getPDG(node).cgNode.getIR().getBasicBlockForInstruction(getInstruction(node));
	}
	
	/**
	 * Returns {@code null} if there is no block for this id
	 */
	public ISSABasicBlock blockForId(SDGNode base, int firstInstructionId) {
		return Iterators.getOnlyElement(Iterators.filter(getProcIR(base).getBlocks(), b -> b.getFirstInstructionIndex() == firstInstructionId), null);
	}
	
	/**
	 * 
	 * @param node used to get the method
	 * @return
	 */
	public SymbolTable getProcSymbolTable(SDGNode node) {
		return getProcIR(node).getSymbolTable();
	}
	
	public List<SDGNode> getControlDeps(SDGNode node) {
		return sdg.getIncomingEdgesOfKind(node, SDGEdge.Kind.CONTROL_DEP_COND).stream().map(SDGEdge::getSource).filter(n -> n.kind == Kind.PREDICATE).collect(Collectors.toList());
	}
	
	public Dominators<Method> getMethodDominators(){
		return new Dominators<>(main, m -> {
			Set<Method> called = new HashSet<>();
			builder.getNonPrunedWalaCallGraph().getSuccNodes(getCGNode(m.entry)).forEachRemaining(c -> called.add(entryToMethod.get(sdg.getNode(builder.getPDGforMethod(c).entry.getId()))));
			return called;
		});
	}
	
	public Program setMethodInvocationHandler(String props) {
		if (props != null) {
			context.forceMethodInvocationHandler(MethodInvocationHandler.parseAndSetup(this, props));
		}
		return this;
	}
	
	public Context analyze() {
		//System.out.println(" -- " + context.getInputBits(context.sl.top()));
		context.fixPointIteration(main.entry);
		//System.out.println(" -- " + context.getOutputBits(context.sl.bot()));
		//System.out.println(MinCut.compute(context, context.sl.bot()));
		return context;
	}
	
	public SDGNode getSDGNodeForInstr(SDGNode base, SSAInstruction instr) {
		return sdg.getNode(getPDG(base).getNode(instr).getId());
	}
}
