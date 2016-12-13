/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.deprecated.jsdg.sdg;

import java.io.IOException;
import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.ipa.callgraph.AnalysisCache;
import com.ibm.wala.ipa.callgraph.AnalysisScope;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.CallGraph;
import com.ibm.wala.ipa.callgraph.impl.FakeRootClass;
import com.ibm.wala.ipa.callgraph.propagation.InstanceKey;
import com.ibm.wala.ipa.callgraph.propagation.PointerAnalysis;
import com.ibm.wala.ssa.SSAInstruction;
import com.ibm.wala.ssa.analysis.IExplodedBasicBlock;
import com.ibm.wala.types.TypeReference;
import com.ibm.wala.util.CancelException;
import com.ibm.wala.util.MonitorUtil.IProgressMonitor;
import com.ibm.wala.util.WalaException;
import com.ibm.wala.util.collections.HashMapFactory;
import com.ibm.wala.util.collections.HashSetFactory;
import com.ibm.wala.util.graph.traverse.BFSIterator;

import edu.kit.joana.deprecated.jsdg.Analyzer;
import edu.kit.joana.deprecated.jsdg.Messages;
import edu.kit.joana.deprecated.jsdg.SDGFactory;
import edu.kit.joana.deprecated.jsdg.SDGFactory.Config.ObjTreeType;
import edu.kit.joana.deprecated.jsdg.exceptions.ExceptionPrunedCFGAnalysis;
import edu.kit.joana.deprecated.jsdg.exceptions.nullpointer.NullPointerAnalysis;
import edu.kit.joana.deprecated.jsdg.sdg.controlflow.ControlFlowAnalysis;
import edu.kit.joana.deprecated.jsdg.sdg.dataflow.DataFlowAnalysis;
import edu.kit.joana.deprecated.jsdg.sdg.dataflow.FixStubInitializerDependencies;
import edu.kit.joana.deprecated.jsdg.sdg.dataflow.noparampassing.DirectDataFlowComputation;
import edu.kit.joana.deprecated.jsdg.sdg.interference.InterferenceComputation;
import edu.kit.joana.deprecated.jsdg.sdg.nodes.AbstractPDGNode;
import edu.kit.joana.deprecated.jsdg.sdg.nodes.AbstractParameterNode;
import edu.kit.joana.deprecated.jsdg.sdg.nodes.CallNode;
import edu.kit.joana.deprecated.jsdg.sdg.nodes.EntryNode;
import edu.kit.joana.deprecated.jsdg.sdg.nodes.JDependencyGraph;
import edu.kit.joana.deprecated.jsdg.sdg.parammodel.IParamComputation;
import edu.kit.joana.deprecated.jsdg.sdg.parammodel.IParamSet;
import edu.kit.joana.deprecated.jsdg.sdg.parammodel.IParameter;
import edu.kit.joana.deprecated.jsdg.sdg.parammodel.objgraph.params.ActOutLocalNode;
import edu.kit.joana.deprecated.jsdg.sdg.pointsto.IPointerAnalysis;
import edu.kit.joana.deprecated.jsdg.util.Debug;
import edu.kit.joana.deprecated.jsdg.util.Log;
import edu.kit.joana.deprecated.jsdg.util.Util;
import edu.kit.joana.deprecated.jsdg.wala.BytecodeLocation;
import edu.kit.joana.deprecated.jsdg.wala.SourceLocation;
import edu.kit.joana.deprecated.jsdg.wala.objecttree.IKey2Origin;
import edu.kit.joana.util.maps.MultiHashMap;
import edu.kit.joana.util.maps.MultiMap;


/**
 *
 * @author Juergen Graf <graf@kit.edu>
 *
 */
public class SDG extends JDependencyGraph {
	private final HashMap<CGNode, PDG> method2pdg;
	private final HashMap<Integer, PDG> id2pdg;
	private final MultiMap<IMethod, CGNode> method2cgnode;
	private final IKey2Origin k2o;
	private final IPointerAnalysis pta;
	private final IParamComputation pComp;

	private final String mainClass;
	private final EntryNode root;
	private final ExitParam exit;
	private PDG main;
	private final IMethod imain;
	private final CallGraph cg;
	private final Set<PDG> clinits;

	private final boolean ignoreExceptions;
	private final boolean optimizeExceptions;
	private final boolean exceptionStubs;
	private final boolean computeInterference;
	private final boolean interferenceOptimizeThisAccess;
	private final boolean interferenceNoClinits;
	private final boolean interferenceUseEscape;
	private final boolean simpleDataDependency;
	private final boolean addControlFlow;
	private final boolean directDataDependency;

	private int currentPdgId = 3;
	private final int id = 0;

//	protected static final int threadStartID = 1;
//	protected static final int threadRunID = 2;

	private final String[] immutables;

	private PointerAnalysis<InstanceKey> pTo;
    private AnalysisScope scope;

	private SDG(IMethod main, CallGraph cg, IKey2Origin k2o, IPointerAnalysis pta,
			SDGFactory.Config cfg, IParamComputation pComp) {
		this.imain = main;
		this.mainClass = Util.typeName(main.getDeclaringClass().getName());
		this.cg = cg;
		this.pComp = pComp;
		this.k2o = k2o;
		this.pta = pta;
		this.method2pdg = HashMapFactory.make();
		this.id2pdg = HashMapFactory.make();
		this.method2cgnode = new MultiHashMap<IMethod, CGNode>();
		this.clinits = HashSetFactory.make();
		this.exceptionStubs = cfg.exceptionStubs;
		this.ignoreExceptions = cfg.ignoreExceptions;
		this.optimizeExceptions = cfg.optimizeExceptions;
		this.simpleDataDependency = cfg.simpleDataDependency;
		this.computeInterference = cfg.computeInterference;
		this.interferenceOptimizeThisAccess = cfg.interferenceOptimizeThisAccess;
		this.interferenceNoClinits = cfg.interferenceNoClinits;
		this.interferenceUseEscape = cfg.interferenceUseEscape;
		this.addControlFlow = cfg.addControlFlow;
		this.directDataDependency = (cfg.objTree == ObjTreeType.DIRECT_CONNECTIONS);
		if (cfg.immutables != null) {
			this.immutables = new String[cfg.immutables.length];
			for (int i = 0; i< cfg.immutables.length; i++) {
				this.immutables[i] = cfg.immutables[i];
			}
		} else {
			this.immutables = new String[0];
		}
		this.root = makeEntry("*Start*"); //$NON-NLS-1$
		this.exit = new ExitParam(getId());
		addNode(exit);
		exit.setLabel("*Start*");
		addParameterStructureDependency(root, exit);
		addUnconditionalControlDependency(root, exit);
	}

	public static final class ExitParam extends AbstractParameterNode {

		private ExitParam(int id) {
			super(id);
		}

		public String getBytecodeName() {
			return BytecodeLocation.RETURN_PARAM;
		}

		public boolean isExit() {
			return true;
		}

		public boolean isVoid() {
			return true;
		}

		public boolean isActual() {
			return false;
		}

		public boolean isFormal() {
			return true;
		}

		public boolean isIn() {
			return false;
		}

		public boolean isMayAliasing(IParameter p) {
			return false;
		}

		public boolean isMustAliasing(IParameter p) {
			return false;
		}

		public boolean isOnHeap() {
			return false;
		}

		public boolean isOut() {
			return true;
		}

		public boolean isPrimitive() {
			return true;
		}

		@Override
		public boolean isArray() {
			return false;
		}

		@Override
		public boolean isObjectField() {
			return false;
		}

		@Override
		public boolean isRoot() {
			return true;
		}

		@Override
		public boolean isStaticField() {
			return false;
		}

	}

	public static final String[] stdImmutables = new String[] {
		"java.lang.String", "java.lang.Throwable", "java.lang.Integer", "java.lang.FloatingDecimal",
		"java.lang.FDBigInt", "java.lang.CharacterData", "java.lang.CharacterDataLatin1"};

	public static SDG create(IMethod main, CallGraph cg, AnalysisCache cache, IKey2Origin k2o,
			IPointerAnalysis pta, SDGFactory.Config cfg, IProgressMonitor progress)
	throws CancelException, PDGFormatException, WalaException {
		IParamComputation pComp = Util.getParamComputation(cfg);

		if (AbstractPDGNode.unique_count != 1) {
			Log.warn("AbstractPDGNode.unique_count != 1 on calling SDG.create - fix: set to 1");
			AbstractPDGNode.unique_count = 1;
		}

		SDG sdg = new SDG(main, cg, k2o, pta, cfg, pComp);
		sdg.build(cfg, progress);

		AbstractPDGNode.unique_count = 1;

		return sdg;
	}

	public SourceLocation getLocation(AbstractPDGNode node) {
		PDG pdg = getPdgForId(node.getPdgId());

		return (pdg != null ? pdg.getLocation(node) : null);
	}

	public BytecodeLocation getBytecodeLocation(AbstractPDGNode node) {
		PDG pdg = getPdgForId(node.getPdgId());

		return (pdg != null ? pdg.getBytecodeLocation(node) : null);
	}

	public int getId() {
		return id;
	}

	public Set<PDG> getStaticInitializers() {
		return clinits;
	}

	public boolean isIgnoreExceptions() {
		return ignoreExceptions;
	}

	public boolean isAddControlFlow() {
		return addControlFlow;
	}

	public boolean isSimpleDataDependency() {
		return simpleDataDependency;
	}

	public boolean isComputeInterference() {
		return computeInterference;
	}

	public EntryNode getRoot() {
		return root;
	}

	public ExitParam getExit() {
		return exit;
	}

	public CallGraph getCallGraph() {
		return cg;
	}

	private final boolean isFakeRoot(CGNode node) {
		TypeReference tRef = node.getMethod().getDeclaringClass().getReference();

		return tRef == FakeRootClass.FAKE_ROOT_CLASS;
	}

	public final Set<String> getImmutables() {
		HashSet<String> set = new HashSet<String>();
		for (int i = 0; i < immutables.length; i++) {
			set.add(immutables[i]);
		}

		return set;
	}

	public final boolean isImmutableClass(IMethod method) {
		String className = Util.typeName(method.getDeclaringClass().getName());

		if (exceptionStubs && className.endsWith("Exception")) {
			return true;
		}

		for (String im : immutables) {
			if (im.equals(className) || (className != null && className.startsWith(im + "."))) {
				Log.info("Immutable " + className);

				return true;
			}
		}

		return false;
	}

	private PDG createPDG(CGNode method, ExceptionPrunedCFGAnalysis<SSAInstruction, IExplodedBasicBlock> epa,
			IProgressMonitor progress)
	throws PDGFormatException, CancelException {
		if (method == null) {
			throw new IllegalArgumentException("At least a single call graph node is needed to compute an pdg");
		}

        if (progress.isCanceled()) {
            throw CancelException.make("Operation aborted.");
        }

		PDG pdg = method2pdg.get(method);

		IMethod imethod = method.getMethod();
		method2cgnode.add(imethod, method);

		if (pdg == null) {
			String methodName = Util.methodName(imethod);
			progress.worked(1);
			Log.info("Creating PDG " + methodName); //$NON-NLS-1$

//			if (InterferenceComputation.isThreadStart(imethod)) {
//				pdg = PDG.create(progress, method, threadStartID, pta, k2o, cg, ignoreExceptions, epa, pComp);
//			} else if (InterferenceComputation.isThreadRun(imethod)){
//				pdg = PDG.create(progress, method, threadRunID, pta, k2o, cg, ignoreExceptions, epa, pComp);
//			} else
			if (isImmutableClass(imethod)){
				pdg = PDG.createStub(progress, method, currentPdgId++, pta, k2o, cg, ignoreExceptions, epa, pComp);
			} else {
				pdg = PDG.create(progress, method, currentPdgId++, pta, k2o, cg, ignoreExceptions, epa, pComp);
			}
			method2pdg.put(method, pdg);
			id2pdg.put(pdg.getId(), pdg);

			if (imethod.isClinit()) {
				CallNode node = addStaticCallNodes(method, imethod);

				if (!containsNode(pdg.getRoot())) {
					addNode(pdg.getRoot());
				}

				addCallDependency(node, pdg.getRoot());
				clinits.add(pdg);
			}

			Log.info("PDG done."); //$NON-NLS-1$
		}

		return pdg;
	}

	private void build(SDGFactory.Config cfg, IProgressMonitor progress)
	throws CancelException, PDGFormatException, WalaException {
		progress.beginTask(Messages.getString("SDG.Task_Create_SDG"), -1); //$NON-NLS-1$

		ExceptionPrunedCFGAnalysis<SSAInstruction, IExplodedBasicBlock> epa = null;
		if (optimizeExceptions && !ignoreExceptions) {
//			epa = new ExceptionPruneAnalysis(cg, pTo, cache);
//			System.err.println("Exception analysis removed for distribution.");
			epa = NullPointerAnalysis.createIntraproceduralExplodedCFGAnalysis();
		}

		createMainEntryPDGs(epa, progress);

		Log.logTime();
		progress.subTask(Messages.getString("SDG.SubTask_Call_Dep")); //$NON-NLS-1$
		computeCallDependencies(epa, progress);
		if (progress.isCanceled()) {
			throw CancelException.make("Operation aborted.");
		}

		Log.logTime();
		if (!directDataDependency) {
			pComp.computeTransitiveModRef(this, progress);
		} else {
			DirectDataFlowComputation.compute(this,	cg, pta.getHeapGraph(), progress);
		}

		if (isComputeInterference()) {
			Log.logTime();
			InterferenceComputation.computeInterference(this, cg, clinits, method2cgnode, interferenceOptimizeThisAccess,
					interferenceUseEscape, interferenceNoClinits, pta.getHeapGraph(), k2o, progress);
			if (progress.isCanceled()) {
				throw CancelException.make("Operation aborted.");
			}
		}

		if (!isSimpleDataDependency() && pComp != null) {
			Log.logTime();
			DataFlowAnalysis.computeDataDependence(getAllContainedPDGs(), progress);
			if (progress.isCanceled()) {
				throw CancelException.make("Operation aborted.");
			}
		}

		if (exceptionStubs || (immutables != null && immutables.length > 0)) {
			FixStubInitializerDependencies.apply(this, progress);
		}

		if (isAddControlFlow()) {
			Log.logTime();
			ControlFlowAnalysis.compute(this, epa, progress);
		}

		Log.logTime();

		debugDumps(progress);
	}


	private void createMainEntryPDGs(ExceptionPrunedCFGAnalysis<SSAInstruction, IExplodedBasicBlock> epa, IProgressMonitor progress)
	throws PDGFormatException, CancelException {
		Set<CGNode> entries = cg.getNodes(imain.getReference());

		if (entries == null || entries.size() == 0) {
			System.out.println("\nNo entries for " + Util.methodName(imain));
		}

		assert (entries != null);
		assert (entries.size() == 1);

		CGNode maincg = entries.iterator().next();

		CallNode mainCall = makeStaticRootCall(imain, maincg);
		addUnconditionalControlDependency(root, mainCall);

		PDG pdgCur = createPDG(maincg, epa, progress);
		addNode(pdgCur.getRoot());
		main = pdgCur;

		addCallDependency(mainCall, pdgCur.getRoot());
	}

	private final boolean onlyCalledByStubs(CGNode n) {
		Iterator<? extends CGNode> it = cg.getPredNodes(n);
		boolean allImmutable = true;
		boolean atLeastOne = false;

		while (it.hasNext()) {
			atLeastOne = true;
			CGNode caller = it.next();
			allImmutable &= isImmutableClass(caller.getMethod());
		}

		return atLeastOne && allImmutable;
	}

	/**
	 * 2-Phase Algorithm for pdg creation and call dependencies between
	 * pdgs. Split up to resolve recursive calls.
	 * 1. iterate through all called methods, create pdgs and add root
	 * 	nodes to sdg
	 * 1.a) create summary edges from act-ins to act-outs for dummy calls
	 * 2. iterate again through call graph and add call edges
	 * @throws PDGFormatException
	 * @throws CancelException
	 */
	private void computeCallDependencies(ExceptionPrunedCFGAnalysis<SSAInstruction, IExplodedBasicBlock> epa,
			IProgressMonitor progress) throws PDGFormatException, CancelException {
		Set<CGNode> toIgnore;
		if (ignoreExceptions) {
//			toIgnore = ExceptionFilter.findExceptionOnlyNodes(cg);
			toIgnore = new HashSet<CGNode>(); 
			// for evaluation purposes this is deactivated to make callgraphs comparable with obj-graph appreoach
			// in the future this optimization needs to come to the new obj-graph branch.
		} else {
			toIgnore = new HashSet<CGNode>();
		}

		// create and add nodes
		BFSIterator<CGNode> bfsIt = new BFSIterator<CGNode>(cg);
		while (bfsIt.hasNext()) {
			CGNode cur = bfsIt.next();
			if (!isFakeRoot(cur) && !toIgnore.contains(cur) && !onlyCalledByStubs(cur)) {
				// when we ignore the effects of exceptions we do not consider methods that are only called
				// from within a catch block
				PDG pdgCur = createPDG(cur, epa, progress);
				addNode(pdgCur.getRoot());
			}
		}

		// connect call dependencies
		bfsIt = new BFSIterator<CGNode>(cg);
		while (bfsIt.hasNext()) {
			CGNode cur = bfsIt.next();

			final boolean isThreadStart = InterferenceComputation.isThreadStart(cur.getMethod());

			if (!isFakeRoot(cur) && !onlyCalledByStubs(cur) && !toIgnore.contains(cur)) {
				PDG pdgCur = createPDG(cur, epa, progress);

				if (pdgCur.isStub()) {
					// ignore calls from methods that are treated as stubs
					Log.info("Ignoring calls from method treated as stub: " + pdgCur);
					continue;
				}

				Iterator<? extends CGNode> itCalls = cg.getSuccNodes(cur);
				while (itCalls.hasNext()) {
					CGNode call = itCalls.next();

					if (toIgnore.contains(call)) {
						continue;
					}

					PDG pdgCalled = createPDG(call, epa, progress);

					for (CallNode callNode : pdgCur.getCallsTo(pdgCalled)) {
						if (!pdgCur.containsNode(pdgCalled.getRoot())) {
							pdgCur.addNode(pdgCalled.getRoot());
						}

						if (isThreadStart && InterferenceComputation.maybeThreadRun(call.getMethod())) {
							pdgCur.addForkDependency(callNode, pdgCalled.getRoot());
						} else {
							pdgCur.addCallDependency(callNode, pdgCalled.getRoot());
						}

						pComp.connectCallParamNodes(pdgCur, callNode, pdgCalled);

						// add call dependency to sdg
						if (!containsNode(callNode)) {
							addNode(callNode);
						}
						addUnconditionalControlDependency(pdgCur.getRoot(), callNode);
						if (isThreadStart && InterferenceComputation.maybeThreadRun(call.getMethod())) {
							addForkDependency(callNode, pdgCalled.getRoot());
						} else {
							addCallDependency(callNode, pdgCalled.getRoot());
						}
					}
				}
			}
		}

		// add stub methods to callgraph root if they dont have a callsite yet
		// TODO better not create those methods
		for (PDG pdg : getAllContainedPDGs()) {
			int preds = getPredNodeCount(pdg.getRoot(), EdgeType.CL);
			preds += getPredNodeCount(pdg.getRoot(), EdgeType.FORK);

			if (preds == 0) {
				// remove method that is never called
				Log.warn("Adding missing call for " + pdg
				    + ". This method may be missing because our optimized control flow detected an impossible flow.");
				CallNode stubCall = makeStaticRootCall(pdg.getMethod(), pdg.getCallGraphNode());
				addUnconditionalControlDependency(root, stubCall);
				addCallDependency(stubCall, pdg.getRoot());
				ActOutLocalNode stubOut = new ActOutLocalNode(id, true, pdg.getMethod().getReturnType(), null, null,
						stubCall.getUniqueId(), BytecodeLocation.UNDEFINED_POS_IN_BYTECODE,
						BytecodeLocation.UNDEFINED_POS_IN_BYTECODE);
				stubOut.setLabel("return [stub]");
				addNode(stubOut);
				addParameterChildDependency(stubCall, stubOut);
                pdg.addNode(stubOut);
				SDG.addParameterOutDependency(pdg, pdg.getExit(), stubOut);

                if (isAddControlFlow()) {
                  addControlFlowDependency(root, stubCall);
                  addControlFlowDependency(stubCall, stubOut);
                  addControlFlowDependency(stubOut, exit);
                }
			}
		}

		progress.done();
	}



	public PDG getPdgForId(int id) {
		return id2pdg.get(id);
	}

	public PDG getPdgForMethodSignature(CGNode method) {
		return method2pdg.get(method);
	}


	/**
	 * Adds all nodes and dependences modeling a static method call of the root
	 * node. calledMethod may only be a static initializer or the main method.
	 *
	 * @param calledMethod The called IMethod
	 * @return The source Node of call (CL) dependences
	 */
	private CallNode addStaticCallNodes(CGNode cgMethod, IMethod method) {
		CallNode call = makeStaticRootCall(method, cgMethod);
		addUnconditionalControlDependency(root, call);

		return call;
	}

	public String toString() {
		return "SDG of " + mainClass; //$NON-NLS-1$
	}

	public PDG getMainPDG() {
		return main;
	}

	public IMethod getMain() {
		return imain;
	}

	/**
	 * Returns a set of all PDGs contained in this SDG
	 * @return
	 */
	public Set<PDG> getAllContainedPDGs() {
		Set<PDG> contained = HashSetFactory.make();
		contained.addAll(id2pdg.values());

		return contained;
	}

	public Iterator<? extends AbstractPDGNode> getCallers(AbstractPDGNode callee) {
		return getPredNodes(callee, EdgeType.CL);
	}

	public Iterator<? extends AbstractPDGNode> getCallees(AbstractPDGNode caller) {
		return getSuccNodes(caller, EdgeType.CL);
	}


	public static class Call {
		public final PDG caller;
		public final CallNode node;
		public final PDG callee;

		public Call(PDG caller, CallNode node, PDG callee) {
			this.caller = caller;
			this.node = node;
			this.callee = callee;
		}

		public String toString() {
			return node.toString();
		}
	}

	private Set<Call> allCalls = null;

	public Set<Call> getAllCalls() {
		if (allCalls == null) {
			allCalls = new HashSet<Call>();
			for (CGNode caller : method2pdg.keySet()) {
				PDG callerPdg = method2pdg.get(caller);
				for (CallNode callNode : callerPdg.getAllCalls()) {
					CGNode callee = callNode.getTarget();
					PDG calleePdg = method2pdg.get(callee);

					if (calleePdg == null) {
						continue;
					}

					Call call = new Call(callerPdg, callNode, calleePdg);
					allCalls.add(call);
				}
			}

		}

		return allCalls;
	}

	public static void addParameterInDependency(PDG pdg, AbstractParameterNode from, AbstractParameterNode to) {
		/**
		 * This is for the special trick of static initializers. The form-out nodes
		 * of static initializers are connected directly to the corresponding form-in nodes.
		 * Because no callsites exists for the static initializers, there exist no actual-nodes.
		 */
		assert ((from.isFormal() && from.isOut() && pdg.getMethod().isClinit()) || (from.isActual() && from.isIn()));
		assert (to.isFormal() && to.isIn());

		if (!pdg.containsNode(from)) {
			pdg.addNode(from);
		}
		if (!pdg.containsNode(to)) {
			pdg.addNode(to);
		}

		if (InterferenceComputation.isThreadStart(pdg.getMethod())) {
			pdg.addForkInDependency(from, to);
		} else {
			pdg.addParamInDependency(from, to);
		}
	}

	public static void addParameterOutDependency(PDG pdg, AbstractParameterNode from, AbstractParameterNode to) {
		assert (from.isFormal() && from.isOut()) : "Node is not formal and out: " + from;
		assert (to.isActual() && to.isOut());

		if (!pdg.containsNode(to)) {
			pdg.addNode(to);
		}
		if (!pdg.containsNode(from)) {
			pdg.addNode(from);
		}

		pdg.addParamOutDependency(from, to);
	}

	public void setPointerAnalysis(PointerAnalysis<InstanceKey> pTo) {
	    this.pTo = pTo;
	}

    public PointerAnalysis<InstanceKey> getPointerAnalysis() {
        return pTo;
    }

    public void setAnalysisScope(AnalysisScope scope) {
        this.scope = scope;
    }

    public AnalysisScope getAnalysisScope() {
        return scope;
    }

    private void debugDumps(IProgressMonitor progress) throws CancelException {
    	// Exception statistics
    	for (TypeReference exc : ExceptionPrunedCFGAnalysis.COUNT_EXCEPTIONS.keySet()) {
    		Integer count = ExceptionPrunedCFGAnalysis.COUNT_EXCEPTIONS.get(exc);
    		Log.info("EXC-COUNT: " + count + " " + exc.toString());
    	}

    	{
	    	long count = 0;
	    	double sum = 0.0;
	    	for (CGNode node : ExceptionPrunedCFGAnalysis.PERCENT.keySet()) {
	    		Double percent = ExceptionPrunedCFGAnalysis.PERCENT.get(node);
	    		if (!Double.isNaN(percent)) {
		    		sum += percent;
		    		count++;
	    		}
	    	}
			DecimalFormat df = new DecimalFormat("00.00");
	    	Log.info("EXC-AVG: " + (sum == 0 || count == 0 ? 0 : df.format(sum / (double) count)) + "% cfg edges removed.");
    	}

    	{
    		long count = 0;
	    	double sum = 0.0;
	    	for (CGNode node : ExceptionPrunedCFGAnalysis.PERCENT_PIE.keySet()) {
	    		Double percent = ExceptionPrunedCFGAnalysis.PERCENT_PIE.get(node);
	    		if (!Double.isNaN(percent)) {
		    		sum += percent;
		    		count++;
	    		}
	    	}
			DecimalFormat df = new DecimalFormat("00.00");
	    	Log.info("EXC-AVG: " + (sum == 0 || count == 0 ? 0 : df.format(sum / (double) count)) + "% exceptions removed.");
    	}

		if (Debug.Var.PRINT_INTERFACE.isSet()) {
			for (PDG pdg : getAllContainedPDGs()) {
		        if (progress.isCanceled()) {
		            throw CancelException.make("Operation aborted.");
		        }

				Log.info("Interface of " + pdg);

				IParamSet<? extends AbstractParameterNode> refs = pdg.getParamModel().getRefParams();
				for (AbstractParameterNode node : refs) {
					if (node.isOnHeap()) {
						Log.info("REF: " + node);
					}
				}

				IParamSet<? extends AbstractParameterNode> mods = pdg.getParamModel().getModParams();
				for (AbstractParameterNode node : mods) {
					if (node.isOnHeap()) {
						Log.info("MOD: " + node);
					}
				}
			}
		}

		if (Debug.Var.DUMP_SSA.isSet()) {
			for (PDG pdg : getAllContainedPDGs()) {
		        if (progress.isCanceled()) {
		            throw CancelException.make("Operation aborted.");
		        }

				if (pdg.getIR() != null) {
					try {
						Util.dumpSSA(pdg.getIR(), Analyzer.cfg.outputDir);
					} catch (IOException e) {
						System.err.println("Debug output of SSA Form for method " +
							Util.methodName(pdg.getMethod()) + " went wrong.");
						e.printStackTrace();
					}
				}
			}
		}


		if (Debug.Var.DUMP_CDG.isSet()) {
			for (PDG pdg : getAllContainedPDGs()) {
				Util.dumpCDG(pdg, null);
			}
		}

		if (Debug.Var.DUMP_PDG_CFG.isSet()) {
			for (PDG pdg : getAllContainedPDGs()) {
				Util.dumpCFG(pdg, null);
			}
		}
    }
}
