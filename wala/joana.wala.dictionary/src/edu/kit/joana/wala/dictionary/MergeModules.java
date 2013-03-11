/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.wala.dictionary;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileFilter;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.jar.JarFile;

import com.ibm.wala.classLoader.IBytecodeMethod;
import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.classLoader.Language;
import com.ibm.wala.ipa.callgraph.AnalysisCache;
import com.ibm.wala.ipa.callgraph.AnalysisOptions;
import com.ibm.wala.ipa.callgraph.AnalysisScope;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.CallGraph;
import com.ibm.wala.ipa.callgraph.Entrypoint;
import com.ibm.wala.ipa.callgraph.impl.SetOfClasses;
import com.ibm.wala.ipa.callgraph.impl.SubtypesEntrypoint;
import com.ibm.wala.ipa.callgraph.propagation.PointerAnalysis;
import com.ibm.wala.ipa.callgraph.propagation.SSAPropagationCallGraphBuilder;
import com.ibm.wala.ipa.cha.ClassHierarchy;
import com.ibm.wala.ipa.cha.ClassHierarchyException;
import com.ibm.wala.shrikeCT.InvalidClassFileException;
import com.ibm.wala.ssa.IR;
import com.ibm.wala.ssa.SSAInstruction;
import com.ibm.wala.ssa.SSAInvokeInstruction;
import com.ibm.wala.types.ClassLoaderReference;
import com.ibm.wala.types.MethodReference;
import com.ibm.wala.util.CancelException;
import com.ibm.wala.util.config.AnalysisScopeReader;
import com.ibm.wala.util.config.FileOfClasses;
import com.ibm.wala.util.strings.StringStuff;

import edu.kit.joana.ifc.sdg.graph.SDG;
import edu.kit.joana.ifc.sdg.graph.SDGEdge;
import edu.kit.joana.ifc.sdg.graph.SDGNode;
import edu.kit.joana.ifc.sdg.graph.SDGSerializer;
import edu.kit.joana.ifc.sdg.graph.chopper.SummaryMergedChopper;
import edu.kit.joana.ifc.sdg.util.BytecodeLocation;
import edu.kit.joana.util.Log;
import edu.kit.joana.util.Logger;
import edu.kit.joana.wala.core.ExternalCallCheck;
import edu.kit.joana.wala.core.Main;
import edu.kit.joana.wala.dictionary.Dictionary.SDGSummaryReference;
import edu.kit.joana.wala.flowless.pointsto.AliasGraph.MayAliasGraph;
import edu.kit.joana.wala.flowless.pointsto.Pts2AliasGraph;
import edu.kit.joana.wala.flowless.util.AliasGraphIO;
import gnu.trove.map.hash.TIntObjectHashMap;

public class MergeModules {

	private final Logger debug = Log.getLogger(Log.L_MOJO_DEBUG);
	
	public static void main(final String[] argv) throws ClassHierarchyException, IllegalArgumentException, IOException, CancelException {
		// merge precomputed dependency graphs
		final ModuleCFG mainModule = new ModuleCFG("program.Program.main([Ljava/lang/String;)V",
				"../joana.wala.modular.testdata/dist/mojo-test-program.jar");
		final ModuleCFG[] otherModules1 = new ModuleCFG[] {
				new ModuleCFG("module1", "../joana.wala.modular.testdata/dist/mojo-test-modules1.jar")
		};
		MergeModules.runMergeModules("merge_modules1", "./out/", mainModule, otherModules1, null);

		final ModuleCFG[] otherModules2 = new ModuleCFG[] {
				new ModuleCFG("module2", "../joana.wala.modular.testdata/dist/mojo-test-modules2.jar")
		};
		MergeModules.runMergeModules("merge_modules2", "./out/", mainModule, otherModules2, null);
	}

	public static class ModuleCFG {
		public final String name;
		public final String classpath;

		public ModuleCFG(final String name, final String classpath) {
			this.name = name;
			this.classpath = classpath;
		}
	}

	private final String name;
	private final String baseDir;
	private final ModuleCFG mainModule;
	private final ModuleCFG[] otherModules;
	@SuppressWarnings("unused")
	private final ExternalCallCheck exc;
	private SDG sdg;
	private Set<SDGNode> extCallNodes;
	private Dictionary dict;
	private CallGraph cg;
	private PointerAnalysis pts;


	public MergeModules(final String name, final String baseDir, final ModuleCFG mainModule,
			final ModuleCFG[] otherModules, final ExternalCallCheck exc) {
		this.name = name;
		this.baseDir = baseDir;
		this.mainModule = mainModule;
		this.otherModules = otherModules;
		this.exc = exc;
	}

	public static SDG runMergeModules(final String name, final String baseDir, final ModuleCFG mainModule,
			final ModuleCFG[] otherModules, final ExternalCallCheck exc) throws IOException, ClassHierarchyException, IllegalArgumentException, CancelException {
		MergeModules mm = new MergeModules(name, baseDir, mainModule, otherModules, exc);
		mm.run();

		return mm.sdg;
	}

	private void run() throws IOException, ClassHierarchyException, IllegalArgumentException, CancelException {
		//TODO implement me!
		// 1. compute points-to
		// 2. find and load main sdg
		// (3.) recompute alias configurations on marked call sites
		// 4. populate dictionary with method-alias configurations
		// 5. decide at each call site which configuration to use
		// 6. load summary for selected configuration
		// 7. add to main sdg
		// 8. propagate field nodes
		// 9. adjust data flow
		computeMergedPTS("../../contrib/lib/stubs/natives_empty.xml", "../../contrib/lib/stubs/jSDG-stubs-jre1.4.jar",
				Main.STD_EXCLUSION_REG_EXP);
		final String mainPdgName = findAndLoadMainSDG();
		populateDictionary();
		insertSummariesAtCallsites();
		writeOutAdjustedSDG(mainPdgName);
	}

	private void writeOutAdjustedSDG(final String mainPdgName) throws FileNotFoundException {
		String filename = (mainPdgName.contains(File.separator)
				? mainPdgName.substring(mainPdgName.lastIndexOf(File.separator))
				: mainPdgName);

		filename = filename.substring(0, filename.length() - ".pdg".length());

		filename += "-" + name + ".pdg";

		String dirname = baseDir + (baseDir.endsWith(File.separator) ? "" : File.separator) + "merge";
		File dir = new File(dirname);
		if (dir.exists() && (!dir.isDirectory() || !dir.canWrite())) {
			System.err.println("Could not write sdg to dir '" + dirname + "' because it is not a writeable directory.");
			return;
		} else if (!dir.exists()) {
			if (!dir.mkdir()) {
				System.err.println("Could not create dir '" + dirname + "'.");
				return;
			}
		}

		SDGSerializer.toPDGFormat(sdg, new FileOutputStream(dirname + File.separator + filename));
	}

	private void insertSummariesAtCallsites() throws IOException {
		print("Inserting summaries at callsites... ");

		for (final SDGNode call : extCallNodes) {
			final List<CGNode> callers = findMatchingCallsites(call);
			final List<SSAInvokeInstruction> calls = findMatchingCalls(call, callers);
			final MayAliasGraph callContext = computeAliasCallContext(callers, calls);
			AliasGraphIO.dumpToDot(callContext, this.baseDir + File.separator + "ext_calls" + File.separator
					+ "c_" + call.getId() + File.separator + "alias_context.dot");
			final Set<IMethod> tgts = findPossibleTargets(callers, calls);

			for (final IMethod im : tgts) {
				final SDGSummaryReference sumref = dict.getSDGfor(im, callContext);
				insertSummaryAt(call, sumref, im);
			}
		}

		println("done.");
	}

	private void insertSummaryAt(final SDGNode call, final SDGSummaryReference sumref, final IMethod im) throws IOException {
		//print("[sum " + sumref + " at " + call + "] ");

		// remove mapping from previous sdg summary
		oldid2node.clear();
		final SDG sum = sumref.load();

		// find matching entry nodes
		final SDGNode entry = findEntryNode(sum, im);

		final SDGNode sdgEntry = findOrCreateNode(entry);
		sdg.addEdge(call, sdgEntry, new SDGEdge(call, sdgEntry, SDGEdge.Kind.CALL));

		for (final SDGNode fIn : sum.getFormalInsOfProcedure(entry)) {
			switch (fIn.getBytecodeIndex()) {
			case BytecodeLocation.ROOT_PARAMETER:
				connectParamAtCallsite(sum, fIn, call);
				break;
			case BytecodeLocation.STATIC_FIELD:
				debug.outln("-sf-" + fIn.getLabel());
				break;
			case BytecodeLocation.ARRAY_FIELD:
			case BytecodeLocation.ARRAY_INDEX:
			case BytecodeLocation.OBJECT_FIELD:
			case BytecodeLocation.BASE_FIELD:
				break;
			default:
				throw new IllegalStateException("formal-in with unknown type: " + fIn + ":"
						+ fIn.getBytecodeIndex() + " " + fIn.getBytecodeName());
			}
		}

		// connect form-outs to act-outs
		for (final SDGNode fOut : sum.getFormalOutsOfProcedure(entry)) {
			switch (fOut.getBytecodeIndex()) {
			case BytecodeLocation.ROOT_PARAMETER:
				connectParamAtCallsite(sum, fOut, call);
				break;
			case BytecodeLocation.STATIC_FIELD:
				debug.outln("-sf-" + fOut.getLabel());
				break;
			case BytecodeLocation.ARRAY_FIELD:
			case BytecodeLocation.ARRAY_INDEX:
			case BytecodeLocation.OBJECT_FIELD:
			case BytecodeLocation.BASE_FIELD:
				break;
			default:
				throw new IllegalStateException("formal-out with unknown type: " + fOut + ":"
						+ fOut.getBytecodeIndex() + " " + fOut.getBytecodeName());
			}
		}

		// add whole summary
		addSummaryBodyToCall(call, sum, entry);

		//TODO adjust control flow for newly created act-in/-out nodes

		// remove mapping from previous sdg summary
		oldid2node.clear();
	}

	private void addSummaryBodyToCall(final SDGNode call, final SDG sum, final SDGNode entry) {
		SummaryMergedChopper chopper = new SummaryMergedChopper(sum);
		final Set<SDGNode> sourceSet = sum.getFormalInsOfProcedure(entry);
		sourceSet.add(entry);
		final Set<SDGNode> sinkSet = sum.getFormalOutsOfProcedure(entry);

		Collection<SDGNode> chop = chopper.chop(sourceSet, sinkSet);

		debug.out("-" + chop.size() + " nodes in chop-");

		for (final SDGNode node : chop) {
			findOrCreateNode(node);
		}

		for (final SDGNode node : chop) {
			final SDGNode sdgNode = oldid2node.get(node.getId());

			for (final SDGEdge edge : sum.outgoingEdgesOf(node)) {
				final int sumToId = edge.getTarget().getId();
				if (oldid2node.contains(sumToId)) {
					final SDGNode sdgTo = oldid2node.get(sumToId);
					sdg.addEdge(sdgNode, sdgTo, new SDGEdge(sdgNode, sdgTo, edge.getKind()));
				}
			}
		}

	}


//	private final static String THIS_PREFIX = "this";
//	private final static String PARAM_PREFIX = "param ";

	private int nextNodeID = -1;

	private int getNextNodeID() {
		if (nextNodeID < 0) {
			nextNodeID = sdg.getMaxNodeID() + 1;
		}

		return nextNodeID++;
	}

	private void connectParamAtCallsite(final SDG callee, final SDGNode fIn, final SDGNode call) {
		final String prefix = getPrefixFromParamNode(fIn);

		final boolean isIn = fIn.getKind() == SDGNode.Kind.FORMAL_IN;

		if (isIn) {
			for (final SDGNode actIn : findActualInsOfCall(sdg, call)) {
				if (actIn.getBytecodeIndex() == BytecodeLocation.ROOT_PARAMETER) {
					final String actPrefix = getPrefixFromParamNode(actIn);
					if (prefix.equals(actPrefix)) {
						debug.outln("found match: <ACT: " + actIn.getLabel() + " > - <FORM: " + fIn.getLabel() + " >");
						connectNodeAndChildren(call, callee, actIn, fIn, new HashSet<SDGNode>());
					}
				}
			}
		} else {
			for (final SDGNode actOut : findActualOutsOfCall(sdg, call)) {
				if (actOut.getBytecodeIndex() == BytecodeLocation.ROOT_PARAMETER) {
					final String actPrefix = getPrefixFromParamNode(actOut);
					if (prefix.equals(actPrefix)) {
						debug.outln("found match: <ACT: " + actOut.getLabel() + " > - <FORM: " + fIn.getLabel() + " >");
						connectNodeAndChildren(call, callee, actOut, fIn, new HashSet<SDGNode>());
					}
				}
			}
		}
	}

	private TIntObjectHashMap<SDGNode> oldid2node = new TIntObjectHashMap<SDGNode>();

	private SDGNode findOrCreateNode(final SDGNode node) {
		if (oldid2node.contains(node.getId())) {
			return oldid2node.get(node.getId());
		}


		final SDGNode copy = node.clone();
		copy.tmp = node.getId();
		copy.setId(getNextNodeID());
		oldid2node.put(node.getId(), copy);
		sdg.addVertex(copy);

		return copy;
	}

	private void connectNodeAndChildren(final SDGNode callNode, final SDG callee, final SDGNode act, final SDGNode form, final Set<SDGNode> visited) {
		if (visited.contains(act)) {
			return;
		}

		visited.add(act);

		final boolean isIn = form.getKind() == SDGNode.Kind.FORMAL_IN;
		final SDGNode sdgForm = findOrCreateNode(form);
		if (isIn) {
			sdg.addEdge(act, sdgForm, new SDGEdge(act, sdgForm, SDGEdge.Kind.PARAMETER_IN));
		} else {
			sdg.addEdge(sdgForm, act, new SDGEdge(sdgForm, act, SDGEdge.Kind.PARAMETER_OUT));
		}

		for (final SDGEdge outEdge : callee.outgoingEdgesOf(form)) {
			if (outEdge.getKind() == SDGEdge.Kind.PARAMETER_STRUCTURE) {
				final SDGNode formChild = outEdge.getTarget();
//				if (formInChild.getKind() == SDGNode.Kind.FORMAL_IN) {
					// search or create matching actual-in
					final SDGNode actInChild = findOrCreateMatchingActualNode(callNode, act, formChild);
					connectNodeAndChildren(callNode, callee, actInChild, formChild, visited);
//				}
			}
		}
	}

	private SDGNode findOrCreateMatchingActualNode(final SDGNode callNode, final SDGNode actParent, final SDGNode formChild) {
		final boolean isIn = formChild.getKind() == SDGNode.Kind.FORMAL_IN;

		SDGNode actChild = null;

		for (final SDGEdge out : sdg.outgoingEdgesOf(actParent)) {
			if (out.getKind() == SDGEdge.Kind.PARAMETER_STRUCTURE) {
				final SDGNode child = out.getTarget();
				if ((isIn && child.getKind() == SDGNode.Kind.ACTUAL_IN)
						|| (!isIn && child.getKind() == SDGNode.Kind.ACTUAL_OUT)) {
					// bytecode index and method contain parameter type and name
					if (child.getBytecodeIndex() == formChild.getBytecodeIndex()
							&& child.getBytecodeName().equals(formChild.getBytecodeName())) {
						// found
						actChild = child;
						break;
					}
				}
			}
		}

		if (actChild == null) {
			// create child
			actChild = formChild.clone();
			actChild.kind = (isIn ? SDGNode.Kind.ACTUAL_IN : SDGNode.Kind.ACTUAL_OUT);
			actChild.operation = (isIn ? SDGNode.Operation.ACTUAL_IN : SDGNode.Operation.ACTUAL_OUT);
			actChild.tmp = formChild.getId();
			actChild.setId(getNextNodeID());
			sdg.addVertex(actChild);
			sdg.addEdge(actParent, actChild, new SDGEdge(actParent, actChild, SDGEdge.Kind.PARAMETER_STRUCTURE));
			sdg.addEdge(callNode, actChild, new SDGEdge(callNode, actChild, SDGEdge.Kind.CONTROL_DEP_EXPR));
		}

		return actChild;
	}

	private static String getPrefixFromParamNode(final SDGNode node) {
		String label = node.getBytecodeName();
		if (label.startsWith(BytecodeLocation.ROOT_PARAM_PREFIX)) {
			for (int index = BytecodeLocation.ROOT_PARAM_PREFIX.length(); index < label.length(); index++) {
				final char ch = label.charAt(index);
				if (ch < '0' || ch > '9') {
					label = label.substring(0, index);
					break;
				}
			}
		} else if (label.startsWith(BytecodeLocation.RETURN_PARAM)) {
			label = BytecodeLocation.RETURN_PARAM;
		} else if (label.startsWith(BytecodeLocation.EXCEPTION_PARAM)) {
			label = BytecodeLocation.EXCEPTION_PARAM;
		} else {
			System.err.println("Unknown prefix '" + label + "'");
		}

//		System.out.println("prefix '" + label + "'");

		return label;
	}

	private static List<SDGNode> findActualInsOfCall(final SDG sdg, final SDGNode call) {
		final List<SDGNode> actIns = new LinkedList<SDGNode>();

		for (final SDGEdge edge : sdg.outgoingEdgesOf(call)) {
			if (edge.getKind() == SDGEdge.Kind.CONTROL_DEP_EXPR) {
				final SDGNode to = edge.getTarget();
				if (to.getKind() == SDGNode.Kind.ACTUAL_IN) {
					actIns.add(to);
				}
			}
		}

		return actIns;
	}

	private static List<SDGNode> findActualOutsOfCall(final SDG sdg, final SDGNode call) {
		final List<SDGNode> actOuts = new LinkedList<SDGNode>();

		for (final SDGEdge edge : sdg.outgoingEdgesOf(call)) {
			if (edge.getKind() == SDGEdge.Kind.CONTROL_DEP_EXPR) {
				final SDGNode to = edge.getTarget();
				if (to.getKind() == SDGNode.Kind.ACTUAL_OUT) {
					actOuts.add(to);
				}
			}
		}

		return actOuts;
	}

	private final SDGNode findEntryNode(final SDG sum, final IMethod im) {
		final String bcMethod = im.getSignature();
		final SDGNode root = sum.getRoot();

		for (final SDGEdge out : sum.outgoingEdgesOf(root)) {
			final SDGNode to = out.getTarget();

			if (to.kind == SDGNode.Kind.CALL) {
				for (final SDGEdge cl : sum.outgoingEdgesOf(to)) {
					if (cl.getKind() == SDGEdge.Kind.CALL) {
						final SDGNode tgt = cl.getTarget();

						if (bcMethod.equals(tgt.getBytecodeName())) {
							return tgt;
						}
					}
				}
			}
		}

		return null;
	}

	private Set<IMethod> findPossibleTargets(final List<CGNode> callers, final List<SSAInvokeInstruction> calls) {
		final Set<IMethod> tgts = new HashSet<IMethod>();

		for (int i = 0; i < callers.size(); i++) {
			final CGNode node = callers.get(i);
			final SSAInvokeInstruction invk = calls.get(i);

			final Set<CGNode> cgTgts = cg.getPossibleTargets(node, invk.getCallSite());
			//XXX this is empty atm, as reflection loading seems to be broken in wala
			if (cgTgts.isEmpty()) {
				// shortcut
				Collection<IMethod> chaTgts = cg.getClassHierarchy().getPossibleTargets(invk.getDeclaredTarget());
				for (final IMethod im : chaTgts) {
					if (im != null && !im.isAbstract()) {
						tgts.add(im);
					}
				}
			}

			for (final CGNode tgt : cgTgts) {
				tgts.add(tgt.getMethod());
			}
		}

		return tgts;
	}

	private List<CGNode> findMatchingCallsites(final SDGNode call) {
		final String bcMethod = call.getBytecodeName();

		final List<CGNode> matches = new LinkedList<CGNode>();

		for (final CGNode node : cg) {
			final String sig = node.getMethod().getSignature();
			if (sig.equals(bcMethod)) {
				matches.add(node);
			}
		}

		return matches;
	}

	private List<SSAInvokeInstruction> findMatchingCalls(final SDGNode call, final List<CGNode> matches) {
		final List<SSAInvokeInstruction> result = new LinkedList<SSAInvokeInstruction>();

		final String bcMethod = call.getBytecodeName();
		final int bcIndex = call.getBytecodeIndex();

		if (matches.size() == 0) {
			fail("found no matches for '" + bcMethod + "' in callgraph.");
		}

		for (final CGNode node : matches) {
			final IR ir = node.getIR();
			final SSAInstruction[] instructions = ir.getInstructions();
			final IBytecodeMethod method = (IBytecodeMethod) node.getMethod();

			SSAInvokeInstruction invk = null;

			for (final SSAInstruction instr : instructions) {
				if (instr != null) {
					try {
						final int bci = method.getBytecodeIndex(instr.iindex);

						if (bci == bcIndex) {
							// found call instruction
							if (instr instanceof SSAInvokeInstruction) {
								invk = (SSAInvokeInstruction) instr;
								break;
							} else {
								fail("not a invoke instruction: (" + bcMethod + ":" + bcIndex + ")->" + instr);
							}
						}
					} catch (InvalidClassFileException e) {}
				}
			}

			if (invk == null) {
				fail("no a invoke instruction found for: (" + bcMethod + ":" + bcIndex + ")");
			}

			result.add(invk);
		}

		return result;
	}

	private MayAliasGraph computeAliasCallContext(final List<CGNode> callsites, final List<SSAInvokeInstruction> calls) {
		if (callsites.size() != calls.size()) {
			throw new IllegalStateException("Call and callsites should have the same size.");
		} else if (callsites.size() == 0) {
			fail("found no matches for in callgraph.");
		} else if (callsites.size() > 1) {
			print("[found " + callsites.size() + " matches in callgraph. TODO implement context selector] ");
			// we default to the first callsite for now, but in order to be safe we have to
			// - consider all instances and compute the lowest upper bound
			// or
			// - select exactly the instance that matches the callsite of the callnode
		}

///		print("[context " + invk + "] ");

		MayAliasGraph result = null;

		for (int i = 0; i < callsites.size(); i++) {
			final CGNode node = callsites.get(i);
			final SSAInvokeInstruction invk = calls.get(i);
			final MayAliasGraph alias = Pts2AliasGraph.computeCurrentAliasing(pts, node, invk);

			if (result != null) {
				result.merge(alias);
			} else {
				result = alias;
			}
		}


		return result;
	}

	private void populateDictionary() throws FileNotFoundException {
		final Dictionary dict = new Dictionary();

		for (final ModuleCFG mod : otherModules) {
			print("Loading module '" + mod.name + "' into dictionary... ");

			String modDirName = baseDir;
			if (!baseDir.endsWith(File.separator)) {
				modDirName += File.separator;
			}

			modDirName += "lib_" + mod.name;

			final File modDir = new File(modDirName);

			if (!modDir.exists() || !modDir.isDirectory()) {
				fail("directory of module " + mod.name + " does not exist: " + modDir.getAbsolutePath());
			}

			final File[] mmethoddirs = modDir.listFiles(new FileFilter() {

				@Override
				public boolean accept(File pathname) {
					return pathname.isDirectory() && pathname.getName().startsWith("m_");
				}
			});

			for (final File mdir : mmethoddirs) {
				//print("'" + mdir.getAbsolutePath() + "' ");
				final String signature = mdir.getName().substring("m_".length());

				final String[] aliasCfgs = mdir.list(new FilenameFilter() {

					@Override
					public boolean accept(File dir, String name) {
						return name.endsWith(".alias");
					}
				});

				for (final String aliasCfg : aliasCfgs) {
					final String prefix = aliasCfg.substring(0, aliasCfg.length() - ".alias".length());

					String filenamePrefix = mdir.getAbsolutePath();
					if (!filenamePrefix.endsWith(File.separator)) {
						filenamePrefix += File.separator;
					}
					filenamePrefix += prefix;

					final MayAliasGraph alias = AliasGraphIO.readIn(filenamePrefix + ".alias");

					final SDGSummaryReference sumref = new SDGSummaryReference(filenamePrefix + ".pdg");

					dict.putSDGFor(signature, alias, sumref);
				}
			}

			println("done.");
		}

		this.dict = dict;
	}

	private String findAndLoadMainSDG() throws IOException {
		final File dir = new File(baseDir);
		final String[] pdgFiles = dir.list(new FilenameFilter() {

			@Override
			public boolean accept(File dir, String name) {
				return name.endsWith(".pdg");
			}
		});

		if (pdgFiles.length == 0) {
			fail("No .pdg file found in " + dir.getAbsolutePath());
		} else if (pdgFiles.length > 1) {
				println("Found >1 .pdg files in " + dir.getAbsolutePath() + " -> using first match.");
		}

		String fileName = baseDir;
		if (!fileName.endsWith(File.separator)) {
			fileName += File.separator;
		}
		fileName += pdgFiles[0];

		print("Reading SDG from '" + pdgFiles[0] + "' ... ");
		final SDG sdg = SDG.readFrom(fileName);
		println("done.");

		print("Searching calls to modules ... ");

		// save call node and alias cfg dir
		final Set<SDGNode> calls = new HashSet<SDGNode>();

		String extCallDirName = baseDir;
		if (!extCallDirName.endsWith(File.separator)) {
			extCallDirName += File.separator;
		}
		extCallDirName += "ext_calls";
		final File extCallDir = new File(extCallDirName);

		if (!extCallDir.exists() || !extCallDir.isDirectory()) {
			print("'" + extCallDir.getAbsolutePath() + "' does not exist - no external calls ");
		} else {
			final String[] extCalls = extCallDir.list(new FilenameFilter() {

				@Override
				public boolean accept(File dir, String name) {
					return name.startsWith("c_");
				}
			});

			for (final String exCl : extCalls) {
				try {
					final int callNodeId = Integer.parseInt(exCl.substring("c_".length()));
					final SDGNode cl = sdg.getNode(callNodeId);
					if (cl != null && cl.kind == SDGNode.Kind.CALL) {
						calls.add(cl);
						print("[call: '" + cl + "'] ");
					} else {
						print("no call node: '" + cl + "' ");
					}
				} catch (NumberFormatException exc) {
					print("no node id: '" + exCl + "' ");
				}
			}
		}

		println("done.");

		this.sdg = sdg;
		this.extCallNodes = calls;

		return fileName;
	}

	private void computeMergedPTS(final String nativesXML, final String stubs, final String exclude)
	throws IOException, ClassHierarchyException, IllegalArgumentException, CancelException {
		print("Setting up analysis scope... ");

		// Fuegt die normale Java Bibliothek zum Scope hinzu
		final AnalysisScope scope = AnalysisScopeReader.makePrimordialScope(null);

		if (nativesXML != null) {
			com.ibm.wala.ipa.callgraph.impl.Util.setNativeSpec(nativesXML);
		}

		// if use stubs
		if (stubs != null) {
			scope.addToScope(ClassLoaderReference.Primordial, new JarFile(stubs));
		}

		// Nimmt unnoetige Klassen raus
		final SetOfClasses exclusions = new FileOfClasses(new ByteArrayInputStream(exclude.getBytes()));
		scope.setExclusions(exclusions);

	    final ClassLoaderReference loader = scope.getLoader(AnalysisScope.APPLICATION);
	    AnalysisScopeReader.addClassPathToScope(mainModule.classpath, scope, loader);

    	final ClassLoaderReference loaderExt = scope.getLoader(AnalysisScope.EXTENSION);
	    for (final ModuleCFG mcfg : otherModules) {
		    AnalysisScopeReader.addClassPathToScope(mcfg.classpath, scope, loaderExt);
	    }

	    println("done.");

	    print("Building class hierarchy... ");

	    final ClassHierarchy cha = ClassHierarchy.make(scope);

		println("done.");

	    // Methode in der Klassenhierarchie suchen
		final MethodReference mr = StringStuff.makeMethodReference(Language.JAVA, mainModule.name);
		IMethod entry = cha.resolveMethod(mr);
		if (entry == null) {
			fail("could not resolve " + mr);
		}


		final List<Entrypoint> entries = new LinkedList<Entrypoint>();
		final Entrypoint ep = new SubtypesEntrypoint(entry, cha);
		entries.add(ep);

		final AnalysisOptions options = new AnalysisOptions(scope, entries);
		final AnalysisCache cache = new AnalysisCache();
		final SSAPropagationCallGraphBuilder builder =
			com.ibm.wala.ipa.callgraph.impl.Util.makeVanillaZeroOneCFABuilder(options, cache, cha, cha.getScope());

		print("Computing merged points-to information... ");

		final CallGraph cg = builder.makeCallGraph(options);

		this.cg = cg;
		this.pts = builder.getPointerAnalysis();

		println("done.");
	}

	private void fail(final String str) {
		System.err.print(str);
		throw new IllegalStateException();
	}

	private void print(final String str) {
		System.out.print(str);
	}

	private void println(final String str) {
		System.out.println(str);
	}
}
