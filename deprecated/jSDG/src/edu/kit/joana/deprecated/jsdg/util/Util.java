/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.deprecated.jsdg.util;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.TreeSet;
import java.util.jar.JarFile;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.ibm.wala.analysis.pointers.HeapGraph;
import com.ibm.wala.cfg.ControlFlowGraph;
import com.ibm.wala.cfg.IBasicBlock;
import com.ibm.wala.classLoader.BinaryDirectoryTreeModule;
import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.classLoader.IField;
import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.classLoader.Module;
import com.ibm.wala.classLoader.NewSiteReference;
import com.ibm.wala.classLoader.SourceDirectoryTreeModule;
import com.ibm.wala.dataflow.graph.BitVectorSolver;
import com.ibm.wala.dataflow.graph.ITransferFunctionProvider;
import com.ibm.wala.fixpoint.BitVectorVariable;
import com.ibm.wala.ipa.callgraph.AnalysisScope;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.CallGraph;
import com.ibm.wala.ipa.callgraph.Entrypoint;
import com.ibm.wala.ipa.callgraph.propagation.AllocationSite;
import com.ibm.wala.ipa.callgraph.propagation.AllocationSiteInNode;
import com.ibm.wala.ipa.callgraph.propagation.ArrayContentsKey;
import com.ibm.wala.ipa.callgraph.propagation.ConcreteTypeKey;
import com.ibm.wala.ipa.callgraph.propagation.InstanceFieldKey;
import com.ibm.wala.ipa.callgraph.propagation.InstanceFieldPointerKey;
import com.ibm.wala.ipa.callgraph.propagation.InstanceKey;
import com.ibm.wala.ipa.callgraph.propagation.LocalPointerKey;
import com.ibm.wala.ipa.callgraph.propagation.PointerAnalysis;
import com.ibm.wala.ipa.callgraph.propagation.PointerKey;
import com.ibm.wala.ipa.callgraph.propagation.StaticFieldKey;
import com.ibm.wala.ipa.cha.IClassHierarchy;
import com.ibm.wala.ipa.modref.ArrayLengthKey;
import com.ibm.wala.properties.WalaProperties;
import com.ibm.wala.shrikeBT.ConditionalBranchInstruction;
import com.ibm.wala.shrikeBT.IComparisonInstruction;
import com.ibm.wala.shrikeBT.UnaryOpInstruction;
import com.ibm.wala.shrikeCT.InvalidClassFileException;
import com.ibm.wala.ssa.IR;
import com.ibm.wala.ssa.ISSABasicBlock;
import com.ibm.wala.ssa.SSAArrayLengthInstruction;
import com.ibm.wala.ssa.SSAArrayLoadInstruction;
import com.ibm.wala.ssa.SSAArrayStoreInstruction;
import com.ibm.wala.ssa.SSAFieldAccessInstruction;
import com.ibm.wala.ssa.SSAGetCaughtExceptionInstruction;
import com.ibm.wala.ssa.SSAGetInstruction;
import com.ibm.wala.ssa.SSAInstruction;
import com.ibm.wala.ssa.SSAInvokeInstruction;
import com.ibm.wala.ssa.SSANewInstruction;
import com.ibm.wala.ssa.SSAPhiInstruction;
import com.ibm.wala.ssa.SSAPutInstruction;
import com.ibm.wala.ssa.SymbolTable;
import com.ibm.wala.ssa.analysis.IExplodedBasicBlock;
import com.ibm.wala.types.ClassLoaderReference;
import com.ibm.wala.types.FieldReference;
import com.ibm.wala.types.MethodReference;
import com.ibm.wala.types.TypeName;
import com.ibm.wala.types.TypeReference;
import com.ibm.wala.util.CancelException;
import com.ibm.wala.util.MonitorUtil.IProgressMonitor;
import com.ibm.wala.util.NullProgressMonitor;
import com.ibm.wala.util.WalaException;
import com.ibm.wala.util.collections.HashSetFactory;
import com.ibm.wala.util.config.SetOfClasses;
import com.ibm.wala.util.debug.Assertions;
import com.ibm.wala.util.graph.Graph;
import com.ibm.wala.util.intset.BitVector;
import com.ibm.wala.util.intset.BitVectorIntSet;
import com.ibm.wala.util.intset.IntIterator;
import com.ibm.wala.util.intset.IntSet;
import com.ibm.wala.util.intset.IntSetAction;
import com.ibm.wala.util.intset.OrdinalSet;
import com.ibm.wala.util.intset.OrdinalSetMapping;
import com.ibm.wala.util.io.FileProvider;
import com.ibm.wala.util.strings.Atom;

import edu.kit.joana.deprecated.jsdg.Analyzer;
import edu.kit.joana.deprecated.jsdg.Messages;
import edu.kit.joana.deprecated.jsdg.SDGFactory;
import edu.kit.joana.deprecated.jsdg.sdg.IntermediatePDG;
import edu.kit.joana.deprecated.jsdg.sdg.PDG;
import edu.kit.joana.deprecated.jsdg.sdg.SDG;
import edu.kit.joana.deprecated.jsdg.sdg.controlflow.ControlDependenceGraph;
import edu.kit.joana.deprecated.jsdg.sdg.dataflow.CFGWithParameterNodes;
import edu.kit.joana.deprecated.jsdg.sdg.dataflow.CFGWithParameterNodes.CFGNode;
import edu.kit.joana.deprecated.jsdg.sdg.dataflow.ReachingDefsTransferFP;
import edu.kit.joana.deprecated.jsdg.sdg.nodes.AbstractPDGNode;
import edu.kit.joana.deprecated.jsdg.sdg.nodes.AbstractParameterNode;
import edu.kit.joana.deprecated.jsdg.sdg.nodes.CallNode;
import edu.kit.joana.deprecated.jsdg.sdg.nodes.ExpressionNode;
import edu.kit.joana.deprecated.jsdg.sdg.nodes.JDependencyGraph.EdgeType;
import edu.kit.joana.deprecated.jsdg.sdg.nodes.ParameterField;
import edu.kit.joana.deprecated.jsdg.sdg.parammodel.IModRef;
import edu.kit.joana.deprecated.jsdg.sdg.parammodel.IParamComputation;
import edu.kit.joana.deprecated.jsdg.sdg.parammodel.IParamSet;
import edu.kit.joana.deprecated.jsdg.sdg.parammodel.objgraph.ObjGraphParamComputation;
import edu.kit.joana.deprecated.jsdg.sdg.parammodel.objtree.ActualInOutNode;
import edu.kit.joana.deprecated.jsdg.sdg.parammodel.objtree.FormInOutNode;
import edu.kit.joana.deprecated.jsdg.sdg.parammodel.objtree.ObjTreeInterprocParamComputation;
import edu.kit.joana.deprecated.jsdg.sdg.parammodel.objtree.ObjTreeParamModel;
import edu.kit.joana.deprecated.jsdg.sdg.parammodel.objtree.ParameterNode;
import edu.kit.joana.deprecated.jsdg.sdg.parammodel.objtree.interfacecomp.KLimitUnfoldingCriterion;
import edu.kit.joana.deprecated.jsdg.sdg.parammodel.objtree.interfacecomp.ObjTreeUnfoldingCriterion;
import edu.kit.joana.deprecated.jsdg.sdg.parammodel.objtree.interfacecomp.PtsLimitUnfoldingCriterion;
import edu.kit.joana.deprecated.jsdg.sdg.parammodel.objtree.interfacecomp.ZeroUnfoldingCriterion;
import edu.kit.joana.deprecated.jsdg.wala.objecttree.IKey2Origin;
import edu.kit.joana.deprecated.jsdg.wala.objecttree.IKey2Origin.InstanceKeyOrigin;
import edu.kit.joana.deprecated.jsdg.wala.objecttree.ObjectTreeBuilder;
import edu.kit.joana.deprecated.jsdg.wala.objecttree.ObjectTreeBuilder.FieldRef;
import edu.kit.joana.deprecated.jsdg.wala.viz.DotUtil;
import edu.kit.joana.deprecated.jsdg.wala.viz.EdgeDecorator;
import edu.kit.joana.deprecated.jsdg.wala.viz.SubLabeledGraph;
import edu.kit.joana.wala.util.MultiTree;
import edu.kit.joana.wala.util.NotImplementedException;
import gnu.trove.map.hash.TIntObjectHashMap;

/**
 * Contains some utility methods
 *
 * @author Juergen Graf <graf@kit.edu>
 *
 */
public final class Util {

	private Util() {}

	private final static Pattern p =
		Pattern.compile("[a-zA-Z0-9.\\-,:! \\t?'\\\\/\\(\\)\\{\\}\\[\\]].*");

	public static String sanitizeLabel(Object obj) {
		if (obj != null) {
			String label = obj.toString().replace('"', '\'');
			if (label.length() > 20) {
				label = label.substring(0, 17) + "...";
			}
			Matcher m = p.matcher(label);
			if (m.matches()) {
				return label;
			} else {
				return "...";
			}
		} else {
			return "?";
		}
	}

	public static ObjTreeUnfoldingCriterion getUnfoldCrit(SDGFactory.Config cfg) {
		ObjTreeUnfoldingCriterion unfold = null;
		switch (cfg.objTree) {
		case K1_LIMIT:
			unfold = new KLimitUnfoldingCriterion(1);
			break;
		case K2_LIMIT:
			unfold = new KLimitUnfoldingCriterion(2);
			break;
		case K3_LIMIT:
			unfold = new KLimitUnfoldingCriterion(3);
			break;
		case PTS_LIMIT:
			unfold = new PtsLimitUnfoldingCriterion();
			break;
		case ZERO:
			unfold = new ZeroUnfoldingCriterion();
			break;
		default:
			throw new IllegalStateException("Unknown objtree unfolding criterion: " + cfg.objTree);
		}

		return unfold;
	}

	public static IParamComputation getParamComputation(SDGFactory.Config cfg) {
		IParamComputation pComp;

		switch (cfg.objTree) {
		case PTS_GRAPH:
			pComp = new ObjGraphParamComputation(true, true);
			break;
		case PTS_GRAPH_NO_FIELD:
			pComp = new ObjGraphParamComputation(false, true);
			break;
		case PTS_GRAPH_NO_FIELD_NO_REFINE:
			pComp = new ObjGraphParamComputation(false, false);
			break;
		case PTS_GRAPH_NO_REFINE:
			pComp = new ObjGraphParamComputation(true, false);
			break;
		case K1_LIMIT:
		case K2_LIMIT:
		case K3_LIMIT:
		case PTS_LIMIT:
		case ZERO:
			ObjTreeUnfoldingCriterion unfold = getUnfoldCrit(cfg);
			pComp = new ObjTreeInterprocParamComputation(unfold);
			break;
		case DIRECT_CONNECTIONS:
			// do only parameter passing for root nodes
			pComp = new ObjTreeInterprocParamComputation(new ZeroUnfoldingCriterion());
			break;
		default:
			Log.error("Don't know what to do with objtree type: " + cfg.objTree);
			throw new NotImplementedException();
		}

		return pComp;
	}

	public static AnalysisScope makeAnalysisScope(SDGFactory.Config conf, ClassLoader javaLoader) throws IOException, IllegalArgumentException, InvalidClassFileException {
		AnalysisScope scope = AnalysisScope.createJavaAnalysisScope();

		for (String line : conf.scopeData) {
			StringTokenizer toks = new StringTokenizer(line, "\n,");

			Atom loaderName = Atom.findOrCreateUnicodeAtom(toks.nextToken());
			// skip language token -> we only support Java
			toks.nextToken();
			ClassLoaderReference walaLoader = scope.getLoader(loaderName);
			if (walaLoader == null) {
				Log.warn("Unkown loader '" + loaderName + "' -> using default APPLICATION loader.");
				walaLoader = scope.getApplicationLoader();
			}

			String entryType = toks.nextToken();
			String entryPathname = toks.nextToken();
			if ("classFile".equals(entryType)) {
				File cf = (new FileProvider()).getFile(entryPathname, javaLoader);
				scope.addClassFileToScope(walaLoader, cf);
			} else if ("sourceFile".equals(entryType)) {
				File sf = (new FileProvider()).getFile(entryPathname, javaLoader);
				scope.addSourceFileToScope(walaLoader, sf, entryPathname);
			} else if ("binaryDir".equals(entryType)) {
				File bd = (new FileProvider()).getFile(entryPathname, javaLoader);
				assert bd.isDirectory();
				scope.addToScope(walaLoader, new BinaryDirectoryTreeModule(bd));
			} else if ("sourceDir".equals(entryType)) {
				File sd = (new FileProvider()).getFile(entryPathname, javaLoader);
				assert sd.isDirectory();
				scope.addToScope(walaLoader, new SourceDirectoryTreeModule(sd));
			} else if ("jarFile".equals(entryType)) {
				Module M = (new FileProvider()).getJarFileModule(entryPathname,
						javaLoader);
				scope.addToScope(walaLoader, M);
			} else if ("loaderImpl".equals(entryType)) {
				scope.setLoaderImpl(walaLoader, entryPathname);
			} else if ("stdlib".equals(entryType)) {
				String[] stdlibs = WalaProperties.getJ2SEJarFiles();
				for (int i = 0; i < stdlibs.length; i++) {
					scope.addToScope(walaLoader, new JarFile(stdlibs[i]));
				}
			} else {
				System.err.println("Unknown entryType: " + entryType);
				Assertions.UNREACHABLE();
			}
		}

		StringTokenizer paths = new StringTokenizer(conf.classpath, File.pathSeparator);
	    ClassLoaderReference loader = scope.getLoader(AnalysisScope.APPLICATION);

		while (paths.hasMoreTokens()) {
			String path = paths.nextToken();
		    try {
				if (path.endsWith(".jar")) {
					scope.addToScope(loader, new JarFile(path));
				} else {
					File f = new File(path);
					if (f.isDirectory()) {
						scope.addToScope(loader, new BinaryDirectoryTreeModule(f));
					} else {
						scope.addClassFileToScope(loader, f);
					}
				}
			} catch (IOException e) {
				Assertions.UNREACHABLE(path + ": "+ e.toString());
			}
		}

		if (conf.exclusions != null && !conf.exclusions.isEmpty()) {
			SetOfClasses soc = new RegexOfClasses(conf.exclusions);
			if (conf.invertExclusion) {
				soc = SetComplement.complement((RegexOfClasses) soc);
			}

			scope.setExclusions(soc);
		}

		return scope;
	}

	public static <A> boolean setsEqual(OrdinalSet<A> set1, OrdinalSet<A> set2) {
		if (set1 != null && set2 != null) {
			if (set1.getMapping() == set2.getMapping()) {
				IntSet i1 =set1.getBackingSet();
				IntSet i2 =set2.getBackingSet();
				return (i1 == null && i2 == null) || (i1 != null && i2 !=null && i1.sameValue(i2));
			}

			return false;
		} else {
			return set1 == set2;
		}
	}

	public final static String DOT_EXEC =
		Messages.getString("Analyzer.Dot_Executable"); //$NON-NLS-1$

	public static <T> void dumpGraph(Graph<T> graph, String name) {
		if (Analyzer.cfg != null) {
			String outputDir = Analyzer.cfg.outputDir;
			File cfgOut = new File(outputDir + "/dump-graphs/");
			if (!cfgOut.exists()) {
				cfgOut.mkdir();
			}

			String dotFile = outputDir + "/dump-graphs/" + name + ".dot";
			try {
				DotUtil.dotify(graph, graph, BBNodeDec, dotFile, null, DOT_EXEC, new NullProgressMonitor());
			} catch (WalaException e) {
				e.printStackTrace();
			} catch (CancelException e) {
				e.printStackTrace();
			}
		}
	}

	public static <T> void dumpGraphToFile(final String filename, final Graph<T> graph) {
		try {
			DotUtil.dotify(graph, graph, BBNodeDec, filename, null, DOT_EXEC, new NullProgressMonitor());
		} catch (WalaException e) {
			e.printStackTrace();
		} catch (CancelException e) {
			e.printStackTrace();
		}
	}

	public static void dumpCFGorig(ControlFlowGraph<SSAInstruction, ISSABasicBlock> cfg, IProgressMonitor monitor) {
		String outputDir = Analyzer.cfg.outputDir;
		File cfgOut = new File(outputDir + "/cfg-orig/");
		if (!cfgOut.exists()) {
			cfgOut.mkdir();
		}

		String fileName = methodName(cfg.getMethod());
		if (fileName.length() > MAX_FILENAME_LENGHT) {
			fileName = fileName.substring(MAX_FILENAME_LENGHT) + fileNameId++;
		}
		String dotFile = outputDir + "/cfg-orig/" + fileName + ".cfg-orig.dot";
		try {
			DotUtil.dotify(cfg, cfg, BBNodeDec, dotFile, null, DOT_EXEC, monitor);
		} catch (WalaException e) {
			e.printStackTrace();
		} catch (CancelException e) {
			e.printStackTrace();
		}
	}

	public static void dumpCFGorigExpl(ControlFlowGraph<SSAInstruction, IExplodedBasicBlock> cfg, IProgressMonitor monitor) {
		dumpCFGorigExpl(cfg, "", monitor);
	}

	public static <I, T extends IBasicBlock<I>> void dumpCFGorigExpl(ControlFlowGraph<I, T> cfg, String prefix,
			IProgressMonitor monitor) {
		String outputDir = Analyzer.cfg.outputDir;
		File cfgOut = new File(outputDir + "/cfg-orig-expl/");
		if (!cfgOut.exists()) {
			cfgOut.mkdir();
		}

		String fileName = methodName(cfg.getMethod());
		if (fileName.length() > MAX_FILENAME_LENGHT) {
			fileName = fileName.substring(MAX_FILENAME_LENGHT) + fileNameId++;
		}
		String dotFile = outputDir + "/cfg-orig-expl/" + prefix + fileName + ".cfg-orig-expl.dot";
		try {
			DotUtil.dotify(cfg, cfg, BBNodeDec, dotFile, null, DOT_EXEC, monitor);
		} catch (WalaException e) {
			e.printStackTrace();
		} catch (CancelException e) {
			e.printStackTrace();
		}
	}

	public static void dumpCFG(PDG pdg, IProgressMonitor monitor) {
		String outputDir = Analyzer.cfg.outputDir;
		File cfgOut = new File(outputDir + "/pdg-cfg/");
		if (!cfgOut.exists()) {
			cfgOut.mkdir();
		}

		String fileName = methodName(pdg.getMethod());
		if (fileName.length() > MAX_FILENAME_LENGHT) {
			fileName = fileName.substring(MAX_FILENAME_LENGHT) + fileNameId++;
		}
		String dotFile = outputDir + "/pdg-cfg/" + fileName + ".cfg.dot";
		Set<EdgeType> labels = HashSetFactory.make();
		labels.add(EdgeType.CF);
//		labels.add(EdgeType.CE);
		SubLabeledGraph<AbstractPDGNode, EdgeType> pdgCfg =
			new SubLabeledGraph<AbstractPDGNode, EdgeType>(pdg, labels);

		BitVectorIntSet allowed = new BitVectorIntSet();
		for (AbstractPDGNode node : pdg) {
			if (node.getPdgId() == pdg.getId()) {
				allowed.add(pdg.getNumber(node));
			}
		}
		//SubGraph<AbstractPDGNode> pdgOnlyOwnNodes = new SubGraph<AbstractPDGNode>(pdg, allowed);

		try {
			DotUtil.dotify(pdgCfg, pdgCfg, ExtendedNodeDecorator.DEFAULT, dotFile, null, DOT_EXEC, monitor);
		} catch (WalaException e) {
			e.printStackTrace();
		} catch (CancelException e) {
			e.printStackTrace();
		}
	}


	public static void dumpCFG(CFGWithParameterNodes cfg, IProgressMonitor monitor) {
		String outputDir = Analyzer.cfg.outputDir;
		File cfgOut = new File(outputDir + "/cfg/");
		if (!cfgOut.exists()) {
			cfgOut.mkdir();
		}

		String fileName = methodName(cfg.getMethod());
		if (fileName.length() > MAX_FILENAME_LENGHT) {
			fileName = fileName.substring(MAX_FILENAME_LENGHT) + fileNameId++;
		}
		String dotFile = outputDir + "/cfg/" + fileName + ".cfg.dot";
		try {
			DotUtil.dotify(cfg, cfg, ExtendedNodeDecorator.DEFAULT, dotFile, null, DOT_EXEC, monitor);
		} catch (WalaException e) {
			e.printStackTrace();
		} catch (CancelException e) {
			e.printStackTrace();
		}
	}

	private final static BasicBlockDecorator BBNodeDec = new BasicBlockDecorator();
	private static class BasicBlockDecorator extends ExtendedNodeDecorator.DefaultImpl {
		public String getLabel(Object o) {
			if (o instanceof ISSABasicBlock) {
				ISSABasicBlock bb = (ISSABasicBlock) o;
				if (bb.isEntryBlock()) {
					return "ENTRY";
				} else if (bb.isExitBlock()) {
					return "EXIT";
				} else if (bb.getLastInstructionIndex() > 0) {
					SSAInstruction instr = bb.getLastInstruction();
					if (instr != null) {
						return (bb.isCatchBlock() ?  "CATCH " : "") + prettyShortInstruction(instr) + " [" + bb.getNumber() + "]";
					} else {
						return "skip 1 [" + bb.getNumber() + "]";
					}
				} else {
					return "skip 2 [" + bb.getNumber() + "]";
				}
			}

			return o.toString();
		}
	}

	private static class HGNodeDec extends ExtendedNodeDecorator.DefaultImpl {

		public String getLabel(Object o) throws WalaException {
			if (o instanceof PointerKey) {
				PointerKey pk = (PointerKey) o;
				if (pk instanceof InstanceFieldKey) {
					InstanceFieldKey pki = (InstanceFieldKey) pk;
					return fieldName(pki.getField());
				} else if (pk instanceof LocalPointerKey) {
					LocalPointerKey pkl = (LocalPointerKey) pk;
					CGNode cg = pkl.getNode();
					String methodName = methodName(cg.getMethod());
					if (pkl.isParameter()) {
						final IR ir = cg.getIR();
						if (ir == null) {
							return methodName + " v" + pkl.getValueNumber();
						}
						int []params = ir.getParameterValueNumbers();
						int paramNum = 0;
						for (int i = 0; i < params.length; i++) {
							if (params[i] == pkl.getValueNumber()) {
								paramNum = i;
								break;
							}
						}

						if (cg.getMethod().isStatic()) {
							return methodName + " p" + (paramNum + 1);
						} else {
							return methodName + (paramNum == 0 ? " this" : " p" + paramNum);
						}
					} else {
						return methodName + " v" + pkl.getValueNumber();
					}
				} else if (pk instanceof StaticFieldKey) {
					StaticFieldKey pks = (StaticFieldKey) pk;
					return fieldName(pks.getField());
				} else if (pk instanceof ArrayLengthKey) {
					ArrayLengthKey pka = (ArrayLengthKey) pk;
					return getLabel(pka.getInstanceKey()) + ".length";
				} else if (pk instanceof ArrayContentsKey) {
					ArrayContentsKey pka = (ArrayContentsKey) pk;
					return getLabel(pka.getInstanceKey()) + "[]";
				} else {
					return "Pk: " + pk.toString();
				}
			} else if (o instanceof InstanceKey) {
				InstanceKey ik = (InstanceKey) o;
				if (ik instanceof AllocationSiteInNode) {
					AllocationSiteInNode ika = (AllocationSiteInNode) ik;
					NewSiteReference nsr = ika.getSite();
					CGNode cg = ika.getNode();
					return typeName(ika.getConcreteType().getName()) + "@" +
						methodName(cg.getMethod()) + ":" + nsr.getProgramCounter();
				} else if (ik instanceof AllocationSite) {
					AllocationSite ika = (AllocationSite) ik;
					NewSiteReference nsr = ika.getSite();
					return typeName(ika.getConcreteType().getName()) + "@" +
					methodName(ika.getMethod()) + ":" + nsr.getProgramCounter();
				} else if (ik instanceof ConcreteTypeKey) {
					ConcreteTypeKey ikc = (ConcreteTypeKey) ik;
					return typeName(ikc.getConcreteType().getName());
				} else {
					return "Ik: " + ik.toString();
				}
			} else {
		        return DEFAULT.getLabel(o);
			}
	      }


		public String getShape(Object o) throws WalaException {
			if (o instanceof InstanceKey) {
				return "ellipse";
			} else {
				return DEFAULT.getShape(o);
			}
		}

		public String getColor(Object o) throws WalaException {
			if (o instanceof InstanceKey) {
				return "red";
			} else if (o instanceof InstanceFieldPointerKey || o instanceof StaticFieldKey) {
				return "blue";
			} else {
				return "black";
			}
		}

	}

	public static void dumpHeapGraph(String name, HeapGraph hg, Object node, IProgressMonitor monitor) {
		String outputDir = Analyzer.cfg.outputDir;
		File heapOut = new File(outputDir + "/heap/");
		if (!heapOut.exists()) {
			heapOut.mkdir();
		}

		if (!hg.containsNode(node)) {
			return;
		}

		Graph<Object> g = GraphUtil.createReachableFromSubGraph(hg, node);

		String dotFile = outputDir + "/heap/" + name + ".heap.dot";
		try {
			DotUtil.dotify(g, g, new HGNodeDec(), dotFile, null, DOT_EXEC, monitor);
		} catch (WalaException e) {
			e.printStackTrace();
		} catch (CancelException e) {
			e.printStackTrace();
		}
	}

	public static void dumpHeapGraph(String name, HeapGraph hg, IProgressMonitor monitor) {
		String outputDir = Analyzer.cfg.outputDir;
		File heapOut = new File(outputDir + "/heap/");
		if (!heapOut.exists()) {
			heapOut.mkdir();
		}

		BitVectorIntSet nodesOk = new BitVectorIntSet();
		for (Object obj : hg) {
			if (obj instanceof InstanceKey) {
				InstanceKey ik = (InstanceKey) obj;
				if (ik instanceof AllocationSiteInNode) {
					nodesOk.add(hg.getNumber(ik));
				} else if (ik instanceof AllocationSite) {
					nodesOk.add(hg.getNumber(ik));
				} else if (ik instanceof ConcreteTypeKey) {
					nodesOk.add(hg.getNumber(ik));
				}
			} else if (obj instanceof PointerKey) {
				PointerKey pk = (PointerKey) obj;
				if (pk instanceof InstanceFieldKey) {
					InstanceFieldKey pki = (InstanceFieldKey) pk;
					nodesOk.add(hg.getNumber(pki));
				} else if (obj instanceof LocalPointerKey) {
					LocalPointerKey pkl = (LocalPointerKey) pk;
					if (!pkl.getNode().getMethod().isClinit()) {
						nodesOk.add(hg.getNumber(pkl));
					}
				} else if (obj instanceof StaticFieldKey) {
					StaticFieldKey pks = (StaticFieldKey) pk;
					nodesOk.add(hg.getNumber(pks));
				} else if (obj instanceof ArrayLengthKey) {
					ArrayLengthKey pka = (ArrayLengthKey) pk;
					nodesOk.add(hg.getNumber(pka));
				} else if (obj instanceof ArrayContentsKey) {
					ArrayContentsKey pka = (ArrayContentsKey) pk;
					nodesOk.add(hg.getNumber(pka));
				}
			}
		}

		Graph<Object> g = GraphUtil.createSubGraph(hg, nodesOk);

		// remove nodes without dependencies
		nodesOk = new BitVectorIntSet();
		for (Object o : g) {
			if (g.getPredNodeCount(o) > 0 || g.getSuccNodeCount(o) > 0) {
				nodesOk.add(hg.getNumber(o));
			}
		}

		g = GraphUtil.createSubGraph(hg, nodesOk);

		String dotFile = outputDir + "/heap/" + name + ".heap.dot";
		try {
			DotUtil.dotify(g, g, new HGNodeDec(), dotFile, null, DOT_EXEC, monitor);
		} catch (WalaException e) {
			e.printStackTrace();
		} catch (CancelException e) {
			e.printStackTrace();
		}
	}

	public static void dumpHeapGraphToFile(final String filename, final HeapGraph hg, final IMethod method) {
		BitVectorIntSet nodesOk = new BitVectorIntSet();
		for (Object obj : hg) {
			if (obj instanceof InstanceKey) {
				InstanceKey ik = (InstanceKey) obj;
				if (ik instanceof AllocationSiteInNode) {
					AllocationSiteInNode asin = (AllocationSiteInNode) ik;
					IMethod m = asin.getNode().getMethod();
					if (m.equals(method)) {
						nodesOk.add(hg.getNumber(ik));
					}
				} else if (ik instanceof AllocationSite) {
					AllocationSite as = (AllocationSite) ik;
					IMethod m = as.getMethod();
					if (m.equals(method)) {
						nodesOk.add(hg.getNumber(ik));
					}
				} else if (ik instanceof ConcreteTypeKey) {
					ConcreteTypeKey ctk = (ConcreteTypeKey) ik;
					nodesOk.add(hg.getNumber(ctk));
				}
			} else if (obj instanceof PointerKey) {
				PointerKey pk = (PointerKey) obj;
				if (pk instanceof InstanceFieldKey) {
//					InstanceFieldKey pki = (InstanceFieldKey) pk;
//					nodesOk.add(hg.getNumber(pki));
				} else if (obj instanceof LocalPointerKey) {
					LocalPointerKey pkl = (LocalPointerKey) pk;
					IMethod m = pkl.getNode().getMethod();
					if (m.equals(method)) {
						nodesOk.add(hg.getNumber(pkl));
					}
				} else if (obj instanceof StaticFieldKey) {
//					StaticFieldKey pks = (StaticFieldKey) pk;
//					nodesOk.add(hg.getNumber(pks));
				} else if (obj instanceof ArrayLengthKey) {
//					ArrayLengthKey pka = (ArrayLengthKey) pk;
//					nodesOk.add(hg.getNumber(pka));
				} else if (obj instanceof ArrayContentsKey) {
//					ArrayContentsKey pka = (ArrayContentsKey) pk;
//					nodesOk.add(hg.getNumber(pka));
				}
			}
		}

		Graph<Object> g = GraphUtil.createSubGraph(hg, nodesOk);

		// remove nodes without dependencies
		nodesOk = new BitVectorIntSet();
		for (Object o : g) {
			if (g.getPredNodeCount(o) > 0 || g.getSuccNodeCount(o) > 0) {
				nodesOk.add(hg.getNumber(o));
			}
		}

		g = GraphUtil.createSubGraph(hg, nodesOk);

		try {
			DotUtil.dotify(g, g, new HGNodeDec(), filename, null, DOT_EXEC, new NullProgressMonitor());
		} catch (WalaException e) {
			e.printStackTrace();
		} catch (CancelException e) {
			e.printStackTrace();
		}
	}

	public static void dumpCDG(PDG pdg, IProgressMonitor monitor) {
		String outputDir = Analyzer.cfg.outputDir;
		File cfgOut = new File(outputDir + "/cdg-pdg/");
		if (!cfgOut.exists()) {
			cfgOut.mkdir();
		}

		Set<EdgeType> cdEdges = HashSetFactory.make();
		cdEdges.add(EdgeType.CD_EX);
		cdEdges.add(EdgeType.CD_TRUE);
		cdEdges.add(EdgeType.CD_FALSE);
		cdEdges.add(EdgeType.UN);
		cdEdges.add(EdgeType.CE);
		SubLabeledGraph<AbstractPDGNode, EdgeType> cdg =
			new SubLabeledGraph<AbstractPDGNode, EdgeType>(pdg, cdEdges);

		String fileName = methodName(pdg.getMethod());
		if (fileName.length() > MAX_FILENAME_LENGHT) {
			fileName = fileName.substring(MAX_FILENAME_LENGHT) + fileNameId++;
		}
		String dotFile = outputDir + "/cdg-pdg/" + fileName + ".cdg-pdg.dot";
		try {
			DotUtil.dotify(cdg, cdg, ExtendedNodeDecorator.DEFAULT, dotFile, null, DOT_EXEC, monitor);
		} catch (WalaException e) {
			e.printStackTrace();
		} catch (CancelException e) {
			e.printStackTrace();
		}
	}

	public static void dumpCallGraph(CallGraph cg, Set<CGNode> root, String mainClass, IProgressMonitor monitor) {
		String outputDir = Analyzer.cfg.outputDir;
		File cfgOut = new File(outputDir + "/");
		if (!cfgOut.exists()) {
			cfgOut.mkdir();
		}

		Graph<CGNode> subCG = GraphUtil.createReachableFromSubGraph(cg, root);

		String dotFile = outputDir + "/" + mainClass + ".callgraph.dot";
		try {
			DotUtil.dotify(subCG, subCG, ExtendedNodeDecorator.DEFAULT, dotFile, null, DOT_EXEC, monitor);
		} catch (WalaException e) {
			e.printStackTrace();
		} catch (CancelException e) {
			e.printStackTrace();
		}
	}


	public static void dumpCallGraph(CallGraph cg, String mainClass, IProgressMonitor monitor) {
		if (Analyzer.cfg != null) {
			String outputDir = Analyzer.cfg.outputDir;
			File cfgOut = new File(outputDir + "/");
			if (!cfgOut.exists()) {
				cfgOut.mkdir();
			}

			String dotFile = outputDir + "/" + mainClass + ".callgraph.dot";
			try {
				DotUtil.dotify(cg, cg, ExtendedNodeDecorator.DEFAULT, dotFile, null, DOT_EXEC, monitor);
			} catch (WalaException e) {
				e.printStackTrace();
			} catch (CancelException e) {
				e.printStackTrace();
			}
		}
	}

	public static void dumpCDG(final ControlDependenceGraph<ISSABasicBlock> cdg, IMethod method, IProgressMonitor monitor) {
		String outputDir = Analyzer.cfg.outputDir;
		File cfgOut = new File(outputDir + "/cdg/");
		if (!cfgOut.exists()) {
			cfgOut.mkdir();
		}

		String fileName = methodName(method);
		if (fileName.length() > MAX_FILENAME_LENGHT) {
			fileName = fileName.substring(MAX_FILENAME_LENGHT) + fileNameId++;
		}
		String dotFile = outputDir + "/cdg/" + fileName + ".cdg.dot";
		try {
			DotUtil.dotify(cdg, cdg, BBNodeDec, EdgeDecorator.DEFAULT, dotFile, null, DOT_EXEC, monitor, true);
		} catch (WalaException e) {
			e.printStackTrace();
		} catch (CancelException e) {
			e.printStackTrace();
		}
	}

	public static void dumpWalaSDG(com.ibm.wala.ipa.slicer.SDG sdg, IMethod method, IProgressMonitor monitor) {
		String outputDir = Analyzer.cfg.outputDir;
		File cfgOut = new File(outputDir + "/wala/");
		if (!cfgOut.exists()) {
			cfgOut.mkdir();
		}

		String fileName = methodName(method);
		if (fileName.length() > MAX_FILENAME_LENGHT) {
			fileName = fileName.substring(MAX_FILENAME_LENGHT) + fileNameId++;
		}
		String dotFile = outputDir + "/wala/" + fileName + ".sdg.dot";
		try {
			DotUtil.dotify(sdg, sdg, ExtendedNodeDecorator.DEFAULT, dotFile, null, DOT_EXEC, monitor);
		} catch (WalaException e) {
			e.printStackTrace();
		} catch (CancelException e) {
			e.printStackTrace();
		}
	}

	public static void dumpWalaPDG(com.ibm.wala.ipa.slicer.PDG pdg, IMethod method, IProgressMonitor monitor) {
		String outputDir = Analyzer.cfg.outputDir;
		File cfgOut = new File(outputDir + "/wala/");
		if (!cfgOut.exists()) {
			cfgOut.mkdir();
		}

		String fileName = methodName(method);
		if (fileName.length() > MAX_FILENAME_LENGHT) {
			fileName = fileName.substring(MAX_FILENAME_LENGHT) + fileNameId++;
		}
		String dotFile = outputDir + "/wala/" + fileName + ".pdg.dot";
		try {
			DotUtil.dotify(pdg, pdg, ExtendedNodeDecorator.DEFAULT, dotFile, null, DOT_EXEC, monitor);
		} catch (WalaException e) {
			e.printStackTrace();
		} catch (CancelException e) {
			e.printStackTrace();
		}
	}

	public static void printThreads(final TIntObjectHashMap<IntSet> threads,
			final TIntObjectHashMap<IntSet> threadIds, final SDG sdg) {
		for (int pdgId : threads.keys()) {
			PDG pdg = sdg.getPdgForId(pdgId);
			IntSet ids = threadIds.get(pdg.getId());
			String id = "";
			for (IntIterator it = ids.intIterator(); it.hasNext();) {
				id += it.next();
				if (it.hasNext()) {
					id += ',';
				}
			}

			Log.info("\nThreadID(" + id + ") - " + pdg + " calls:\n{");
			IntSet transitiveCalled = threads.get(pdgId);
			transitiveCalled.foreach(new IntSetAction() {

				public void act(int x) {
					PDG pdg = sdg.getPdgForId(x);
					Log.info(Util.methodName(pdg.getMethod()) + ", ");
				}

			});
			Log.info("}");
		}
	}

	private final static int MAX_FILENAME_LENGHT = 200;
	private static int fileNameId = 0;

	public static void dumpSSA(IR ir, String outDir) throws IOException {
		BufferedWriter bOut;
		File dir = new File(outDir + "/ssa/");
		if (!dir.exists()) {
			dir.mkdir();
		}
		String fileName = methodName(ir.getMethod());
		if (fileName.length() > MAX_FILENAME_LENGHT) {
			fileName = fileName.substring(MAX_FILENAME_LENGHT) + fileNameId++;
		}
		bOut = new BufferedWriter(new FileWriter(outDir + "/ssa/" + fileName +".ssa"));

		bOut.write("IR of " + methodName(ir.getMethod()) + ":\n");
		/*
		SSAInstruction[] instr = ir.getInstructions();
		for (int i = 0; i < instr.length; i++) {
			String txt = prettyInstruction(ir, i);
			if (txt != null) {
				bOut.write(txt);
			}
		}*/
		int i = 0;
		for (Iterator<SSAInstruction> it = ir.iterateAllInstructions(); it.hasNext(); i++) {
			SSAInstruction instr = it.next();
			String txt = prettyInstruction(ir, instr, i);
			if (txt != null) {
				bOut.write(txt);
			}
		}
		bOut.flush();
		bOut.close();
	}

	public static void dumpSSA(IR ir, IBasicBlock<?> bb, PrintStream out) {
		dumpSSA(ir, bb.getFirstInstructionIndex(),
				bb.getLastInstructionIndex() + 1, out);
	}

	public static void dumpSSA(IR ir, PrintStream out) {
		dumpSSA(ir, 0, ir.getInstructions().length, out);
	}

	public static void dumpCatchExceptionSSA(IR ir, PrintStream out) {
		for (Iterator<? extends SSAInstruction> it = ir.iterateCatchInstructions(); it.hasNext();) {
			SSAGetCaughtExceptionInstruction instr =
				(SSAGetCaughtExceptionInstruction) it.next();

			String txt = "bb" + instr.getBasicBlockNumber() + "\t" +
				instr.toString(ir.getSymbolTable());

			out.println(txt);
		}
	}

	public static void dumpPhiSSA(IR ir, PrintStream out) {
		for (Iterator<? extends SSAInstruction> it = ir.iteratePhis(); it.hasNext();) {
			SSAInstruction instr = it.next();
			String txt = instr.toString(ir.getSymbolTable());
			out.println(txt);
		}
	}

	public static void dumpSSA(IR ir, int start, int end, PrintStream out) {
		if (out == null) {
			return;
		}

		out.println("\nIR of " + methodName(ir.getMethod()) + " [" + start +
				"-" + end+ "]:");

		for (int i = start; i < end; i++) {
			SSAInstruction instr = ir.getInstructions()[i];
			String txt = prettyInstruction(ir, instr, i);
			if (txt != null) {
				out.print(txt);
			}
		}

		out.println();
	}


	public static void printMultiTree(MultiTree<FieldRef> tree) {
		System.out.print("[");
		if (tree.value() != null) {
			System.out.print(tree.value().getField().getName().toString());
		} else {
			System.out.print("#");
		}

		Collection<MultiTree<FieldRef>> childs = tree.getChildren();
		if (childs != null && !childs.isEmpty()) {
			System.out.print("(");
			for (MultiTree<FieldRef> child : childs) {
				printMultiTree(child, "*");
			}
			System.out.print(")");
		}


		System.out.print("]\n");
	}

	public static void printMultiTree(MultiTree<FieldRef> tree, String prefix) {
		if (tree.value() != null) {
			System.out.print(prefix + tree.value().getField().getName().toString());
		}
		Collection<MultiTree<FieldRef>> childs = tree.getChildren();
		if (childs != null && !childs.isEmpty()) {
			System.out.print("(");
			for (MultiTree<FieldRef> child : childs) {
				printMultiTree(child, prefix + "*");
			}
			System.out.print(")");
		}
	}

	public static void printOrigins(CGNode method, int ssaVar, PointerAnalysis<InstanceKey> pta, IKey2Origin k2o) {
		PointerKey pk =
			pta.getHeapModel().getPointerKeyForLocal(method, ssaVar);

		OrdinalSet<InstanceKey> pts = pta.getPointsToSet(pk);
		System.out.println("Origins v" + ssaVar + ":");
		for (InstanceKey ik : pts) {
			printOrigin(ik, k2o);
		}

		System.out.println("Subobject tree v" + ssaVar + ":");
		ObjectTreeBuilder builder = new ObjectTreeBuilder(pta);
		MultiTree<FieldRef> tree = builder.getSubobjectTree(pk);
		printMultiTree(tree);
	}

	public static void printOrigin(InstanceKey ik, IKey2Origin k2o) {
		Set<InstanceKeyOrigin> ikOrig = k2o.getOrigin(ik);
		if (ikOrig != null) {
			for (InstanceKeyOrigin iorg : ikOrig) {
				System.out.println(iorg);
			}
		} else {
			System.out.println(ik);
		}
	}

	public static <T> void addAllToSet(Set<T> set, Iterator<? extends T> it) {
		while (it.hasNext()) {
			set.add(it.next());
		}
	}

	private static final int MAX_CONST_STR = 25;

	/**
	 * ssa var numer -> name
	 * e.g. #3 is referred to as v3
	 * @param ssaTmp ssa var number
	 * @return string representation of the ssa var identifier
	 */
	public static String tmpName(IntermediatePDG pdg, int var) {
		final SymbolTable sym = pdg.getIR().getSymbolTable();

		if (var < 0) {
			return "v?(" + var + ")";
		} else if (sym.isConstant(var)) {
			String cst = null;

			if (sym.isBooleanConstant(var)) {
				cst = (sym.isTrue(var) ? "true" : "false");
			} else if (sym.isDoubleConstant(var)) {
				cst = sym.getDoubleValue(var) + " d";
			} else if (sym.isFloatConstant(var)) {
				cst = sym.getFloatValue(var) + " f";
			} else if (sym.isIntegerConstant(var)) {
				cst = sym.getIntValue(var) + "";
			} else if (sym.isLongConstant(var)) {
				cst = sym.getLongValue(var) + " l";
			} else if (sym.isNullConstant(var)) {
				cst = "null";
			} else if (sym.isStringConstant(var)) {
				cst = sym.getStringValue(var).replace('"', '\'');
			} else {
				Object obj = sym.getConstantValue(var);
				cst = (obj == null ? "?" : obj.toString());
			}

			//sym.getConstantValue(var).toString();
			if (cst.length() > MAX_CONST_STR) {
				cst = cst.substring(0, MAX_CONST_STR - 4) + "...";
			}

			return "#(" + cst + ")";
		} else if (var <= pdg.getMethod().getNumberOfParameters()) {
			if (pdg.getMethod().isStatic()) {
				return "p" + var;
			} else if (var == 1){
				return "this";
			} else {
				return "p" + (var - 1);
			}
		} else {
			return "v" + var;
		}
	}

	private static class IteratorWrapper<T> implements Iterable<T> {

		private Iterator<T> iter;

		public IteratorWrapper(Iterator<T> iter) {
			this.iter = iter;
		}

		public Iterator<T> iterator() {
			return iter;
		}

	}

	public static String operator2String(UnaryOpInstruction.IOperator op) {
		if (op instanceof UnaryOpInstruction.Operator) {
			switch ((UnaryOpInstruction.Operator) op) {
			case NEG:
				return "-";
			}
		}

		return "?";
	}

	public static String operator2String(ConditionalBranchInstruction.IOperator op) {
		if (op instanceof ConditionalBranchInstruction.Operator) {
			switch ((ConditionalBranchInstruction.Operator) op) {
			case EQ:
				return "==";
			case GE:
				return ">=";
			case GT:
				return ">";
			case LE:
				return "<=";
			case LT:
				return "<";
			case NE:
				return "!=";
			}
		}

		return "?";
	}

	public static String opcode2String(IComparisonInstruction.Operator opcode) {
		return opcode.name();
//		Field [] fields = Constants.class.getFields();
//		for (int i = 0; i < fields.length; i++) {
//			if (fields[i].getName().startsWith("OP_")) {
//				try {
//					Short sf = fields[i].getShort(Constants.class);
//					if (sf == opcode) {
//						return fields[i].getName().substring(3);
//					}
//				} catch (IllegalArgumentException e) {
//				} catch (IllegalAccessException e) {}
//			}
//		}
//
//		return "?";
		/*
		switch(opcode) {
		case Constants.OP_lcmp:
	        return "lcmp";
		case Constants.OP_fcmpl:
	        return "fcmpl";
	    case Constants.OP_dcmpl:
	        return "dcmpl";
	    case Constants.OP_dcmpg:
	        return "dcmpg";
	    case Constants.OP_fcmpg:
	        return "fcmpg";
		default:
			return "?";
		}*/
	}

	public static <T> Iterable<T> makeIterable(Iterator<T> iter) {
		return new IteratorWrapper<T>(iter);
	}

	public static IMethod searchMethod(Iterable<Entrypoint> pts, String method) {
		for (Entrypoint p : pts) {
			if (p.getMethod() != null && p.getMethod().getSelector() != null &&
					p.getMethod().getSelector().toString().equals(method)) {
				return p.getMethod();
			}
		}

		return null;
	}

	public static Iterable<Entrypoint> makeMainEntrypoints(AnalysisScope scope, final IClassHierarchy cha, String className) {
		return com.ibm.wala.ipa.callgraph.impl.Util.makeMainEntrypoints(scope, cha);
	}

	public static <K, T> Set<T> findOrCreateSet(Map<K, Set<T>> M, K key) {
		if (M == null) {
			throw new IllegalArgumentException("map is null");
		}

		Set<T> result = M.get(key);
		if (result == null) {
			result = new TreeSet<T>();
			M.put(key, result);
		}

		return result;
	}

	public static String prettyBasicBlock(IExplodedBasicBlock bb) {
		if (bb.isEntryBlock()) {
			return "ENTRY";
		} else if (bb.isExitBlock()) {
			return "EXIT";
		} else if (bb.isCatchBlock()) {
			String tmp = "CATCH " + Util.prettyShortInstruction(bb.getCatchInstruction());
			if (bb.getLastInstruction() != null) {
				tmp += " >>> " + Util.prettyShortInstruction(bb.getLastInstruction());
			}
			return tmp;
		} else if (bb.getInstruction() != null) {
			SSAInstruction instr = bb.getInstruction();
			return Util.prettyShortInstruction(instr);
		} else {
			Iterator<SSAPhiInstruction> it = bb.iteratePhis();
			if (it.hasNext()) {
				StringBuilder str = new StringBuilder();
				while (it.hasNext()) {
					SSAPhiInstruction phi = it.next();
					str.append(Util.prettyShortInstruction(phi));
					if (it.hasNext()) {
						str.append('\n');
					}
				}

				return str.toString();
			} else {
				return "SKIP";
			}
		}
	}

	public static String prettyShortInstruction(SSAInstruction instr) {
		if (instr instanceof SSAFieldAccessInstruction) {
			SSAFieldAccessInstruction facc = (SSAFieldAccessInstruction) instr;
			String fieldName;
			if (facc.isStatic()) {
				fieldName = Util.fieldName(facc.getDeclaredField());
			} else {
				fieldName = "v" + facc.getRef() + "." + facc.getDeclaredField().getName();
			}
			if (instr instanceof SSAGetInstruction) {
				SSAGetInstruction fget = (SSAGetInstruction) instr;
				return "v" + fget.getDef() + " = " + fieldName;
			} else if (instr instanceof SSAPutInstruction) {
				SSAPutInstruction fset = (SSAPutInstruction) instr;
				return fieldName + " = v" + fset.getVal();
			}
		} else if (instr instanceof SSAInvokeInstruction) {
			SSAInvokeInstruction invk = (SSAInvokeInstruction) instr;
			if (invk.getDeclaredResultType() == TypeReference.Void) {
				return "call " + Util.methodName(invk.getDeclaredTarget());
			} else {
				return "v" + invk.getDef() + " = call " + Util.methodName(invk.getDeclaredTarget());
			}
		} else if (instr instanceof SSANewInstruction) {
			SSANewInstruction snew = (SSANewInstruction) instr;
			return "v" + snew.getDef() + " = new " + Util.typeName(snew.getConcreteType().getName());
		} else if (instr instanceof SSAPhiInstruction) {
			SSAPhiInstruction phi = (SSAPhiInstruction) instr;
			StringBuilder str = new StringBuilder();
			str.append("v" + phi.getDef() + " = phi ");
			for (int i = 0; i < phi.getNumberOfUses(); i++) {
				str.append("v" + phi.getUse(i));
				if (i+1 < phi.getNumberOfUses()) {
					str.append(',');
				}
			}

			return str.toString();
		} else if (instr instanceof SSAArrayLoadInstruction) {
			SSAArrayLoadInstruction ai = (SSAArrayLoadInstruction) instr;
			return "v" + instr.getDef() + " = v" + ai.getArrayRef() + "[v" + ai.getIndex() + "]";
		} else if (instr instanceof SSAArrayStoreInstruction) {
			SSAArrayStoreInstruction ai = (SSAArrayStoreInstruction) instr;
			return "v" + ai.getArrayRef() + "[v" + ai.getIndex() + "] = v" + ai.getValue();
		} else if (instr instanceof SSAArrayLengthInstruction) {
			SSAArrayLengthInstruction ai = (SSAArrayLengthInstruction) instr;
			return "v" + ai.getDef() + " = v" + ai.getArrayRef() + ".length";
		}

		return instr.toString();
	}

	public static String prettyInstruction(IR ir, SSAInstruction instr, int num) {
		if (instr != null) {
			SymbolTable stab = ir.getSymbolTable();
			String txt = instr.toString(stab);

			for (int i = 0; i < instr.getNumberOfUses(); i++) {
				int use = instr.getUse(i);
				if (use < 0) {
					continue;
				}

//				Value v = stab.getValue(use);
//
//				if (v != null && v instanceof PhiValue) {
//					PhiValue phi = (PhiValue) v;
//					txt += " (" + phi.getPhiInstruction().toString(stab) + ")";
//
//				} else {
				String [] names = ir.getLocalNames(num, use);
				if (names != null) {
					for (int j = 0; j < names.length; j++) {
						txt += " [v" + use + ":use " + names[j] + "]";
					}
				}
//				}
			}

			for (int i = 0; i < instr.getNumberOfDefs(); i++) {
				int def = instr.getDef(i);
				if (def < 0) {
					continue;
				}

				String [] names = ir.getLocalNames(num, def);
				if (names != null) {
					for (int j = 0; j < names.length; j++) {
						txt += " [v" + def + ":def " + names[j] + "]";
					}
				} else {
					txt += " [v" + def + ": def]";
				}
			}

			ISSABasicBlock block = ir.getBasicBlockForInstruction(instr);

			return "<" + instr.iindex + ">bb" + block.getNumber() + "\t" +num + "\t: " + txt + "\n";
		} else {
			return "<???>\t" + num + "\t: nop\n";
		}
	}

	public static final String methodName(IMethod method) {
		return methodName(method.getReference());
	}

	/**
	 * Create a human readable typename from a TypeName object
	 * convert sth like [Ljava/lang/String to java.lang.String[]
	 * @param tName
	 * @return type name
	 */
	public static final String typeName(TypeName tName) {
		StringBuilder test =
			new StringBuilder(tName.toString().replace('/', '.'));

		while (test.charAt(0) == '[') {
			test.deleteCharAt(0);
			test.append("[]");
		}

		// remove 'L' in front of object type
		test.deleteCharAt(0);

		return test.toString();
	}

	public static final String sourceFileName(TypeName name) {
		assert (name.isClassType());

		String source = name.toString();
		if (source.indexOf('$') > 0) {
			// remove inner classes stuff
			source = source.substring(0, source.indexOf('$'));
		}

		// remove 'L'
		source = source.substring(1);
		source = source + ".java";

		return source;
	}

	public static final String methodName(MethodReference mRef) {
		StringBuilder name =
			new StringBuilder(typeName(mRef.getDeclaringClass().getName()));

		name.append(".");
		name.append(mRef.getName().toString());
		name.append("(");
		for (int i = 0; i < mRef.getNumberOfParameters(); i++) {
			TypeReference tRef = mRef.getParameterType(i);
			if (i != 0) {
				name.append(",");
			}
			if (tRef.isPrimitiveType()) {
				if (tRef == TypeReference.Char) {
					name.append("char");
				} else if (tRef == TypeReference.Byte) {
					name.append("byte");
				} else if (tRef == TypeReference.Boolean) {
					name.append("boolean");
				} else if (tRef == TypeReference.Int) {
					name.append("int");
				} else if (tRef == TypeReference.Long) {
					name.append("long");
				} else if (tRef == TypeReference.Short) {
					name.append("short");
				} else if (tRef == TypeReference.Double) {
					name.append("double");
				} else if (tRef == TypeReference.Float) {
					name.append("float");
				} else {
					name.append("?" + tRef.getName());
				}
			} else {
				name.append(typeName(tRef.getName()));
			}
		}

		name.append(")");

		return name.toString();
	}

	public static String fieldName(ParameterField field) {
		if (field.isArray()) {
			return "[]";
		} else {
			return fieldName(((edu.kit.joana.deprecated.jsdg.sdg.nodes.ObjectField) field).getField());
		}
	}

	public static String fieldName(IField field) {
		return fieldName(field.getReference());
	}

	public static String fieldName(FieldReference field) {
		TypeName type = field.getDeclaringClass().getName();

		return typeName(type) + "." + field.getName();
	}

	private static class RegexOfClasses extends SetOfClasses {

		private static final long serialVersionUID = -1759218135339996773L;

		private Pattern pattern = null;
		private String regex = null;
		private boolean needsCompile = false;

		public RegexOfClasses(List<String> exclusions) {
			StringBuffer strbuf = null;
			for (String excl : exclusions) {
				if (strbuf == null) {
					strbuf = new StringBuffer("(" + excl + ")");
				} else {
					strbuf.append("|(" + excl + ")");
				}
			}

			if (strbuf != null) {
				regex = strbuf.toString();
				needsCompile = true;
			}
		}

		public void add(IClass klass) {
		    if (klass == null) {
		        throw new IllegalArgumentException("klass is null");
		      }
		      if (regex == null) {
		        regex = klass.getReference().getName().toString();
		      } else {
		        regex = regex + '|' + klass.getReference().getName().toString();
		      }
		      needsCompile = true;
		}

		@Override
		public boolean contains(String klassName) {
			if (needsCompile) {
				pattern = Pattern.compile(regex);
			}

			if (pattern == null) {
				return false;
			}

			Matcher m = pattern.matcher(klassName);
			return m.matches();
		}

		public boolean contains(TypeReference klass) {
			if (klass == null) {
				throw new IllegalArgumentException("klass is null");
			}

			return contains(klass.getName().toString());
		}

		@Override
		public void add(String klass) {
			Assertions.UNREACHABLE();
		}
	}


	private static class SetComplement extends SetOfClasses {

		private static final long serialVersionUID = 9195409285667103269L;

		private final RegexOfClasses set;

		private SetComplement(RegexOfClasses set) {
			this.set = set;
		}

		public static SetComplement complement(RegexOfClasses set) {
			return new SetComplement(set);
		}

		public void add(IClass klass) {
			Assertions.UNREACHABLE();
		}

		@Override
		public boolean contains(String klassName) {
			return !set.contains(klassName);
		}

		public boolean contains(TypeReference klass) {
			return !set.contains(klass);
		}

		@Override
		public void add(String klass) {
			Assertions.UNREACHABLE();
		}
	}

	public static final void printModRefKillGen(OrdinalSetMapping<AbstractPDGNode> mapping,
			IModRef modRef, CFGWithParameterNodes ecfg, IntermediatePDG pdg,
			BitVectorSolver<CFGNode> solver, ITransferFunctionProvider<CFGNode, BitVectorVariable> transfer,
			Map<AbstractPDGNode, OrdinalSet<AbstractPDGNode>> lastDefMap) {
		for (AbstractPDGNode node : mapping) {
			BitVectorVariable bvMod = modRef.getMod(node);
			BitVectorVariable bvRef = modRef.getRef(node);
			CFGNode cfgNode = null;
			if (node instanceof ParameterNode<?>) {
				ParameterNode<?> param = (ParameterNode<?>) node;
				cfgNode = ecfg.getNodeForParameter(param);
//			} else {
//				SSAInstruction instr = pdg.getInstructionForNode(node);
//				if (instr != null) {
//					cfgNode = ecfg.getCFGNode(instr);
//				}
			} else if (node instanceof ExpressionNode) {
				cfgNode = ecfg.getNodeForExpression((ExpressionNode) node);
			}

			Log.debug(node.toString() + ": ");
			Log.debug("\tMOD = " + printBV(bvMod, mapping));
			Log.debug("\tREF = " + printBV(bvRef, mapping));

			if (cfgNode != null) {
				BitVectorVariable bvIn = solver.getIn(cfgNode);
				Log.debug("\tIN = " + printBV(bvIn, mapping));
				ReachingDefsTransferFP rdtfp = (ReachingDefsTransferFP) transfer;
				BitVector kill = rdtfp.kill(cfgNode);
				IntSet gen = rdtfp.gen(cfgNode);
				BitVectorVariable bvKill = new BitVectorVariable();
				BitVectorVariable bvGen = new BitVectorVariable();
				if (kill != null) {
					bvKill.addAll(kill);
				}
				if (gen != null) {
					BitVectorIntSet isGen = new BitVectorIntSet(gen);
					if (!isGen.isEmpty()) {
						bvGen.addAll(isGen.getBitVector());
					}
				}
				Log.debug("\tKILL = " + printBV(bvKill, mapping));
				Log.debug("\tGEN = " + printBV(bvGen, mapping));
			}

			OrdinalSet<AbstractPDGNode> rdefs = lastDefMap.get(node);
			Log.debug("\tREACHING_DEFS = " + printOS(rdefs));
		}
	}


	private static <T> String printOS(OrdinalSet<T> set) {
		StringBuilder result = new StringBuilder("{");
		for (T elem : set) {
			result.append(elem.toString());
			result.append(", ");
		}
		result.append("}");

		return result.toString();
	}

	private static String printBV(BitVectorVariable bv, final OrdinalSetMapping<AbstractPDGNode> domain) {
		if (bv == null) {
			return "undef";
		}

		IntSet set = bv.getValue();
		if (set != null && !set.isEmpty()) {
			final StringBuilder result = new StringBuilder("{");
			set.foreach(new IntSetAction() {

				public void act(int x) {
					AbstractPDGNode node = domain.getMappedObject(x);
					result.append(node);
					result.append(", ");
				}

			});

			result.append("}");

			return result.toString();
		} else {
			return "{}";
		}
	}

	public final static void printSummaryNodeMappingErrors(ObjTreeParamModel calleePDG, ObjTreeParamModel callerPdg,
			AbstractParameterNode fOut, IParamSet<AbstractParameterNode> actOuts, CallNode cNode) {
		ArrayList<FormInOutNode> calleeFormOuts = new ArrayList<FormInOutNode>();
		calleeFormOuts.addAll(calleePDG.getRootFormOuts());
		ArrayList<FormInOutNode> calleeSFormOuts = new ArrayList<FormInOutNode>();
		calleeSFormOuts.addAll(calleePDG.getStaticFormOuts());

		ArrayList<FormInOutNode> callerFormOuts = new ArrayList<FormInOutNode>();
		callerFormOuts.addAll(callerPdg.getRootFormOuts());
		ArrayList<FormInOutNode> callerSFormOuts = new ArrayList<FormInOutNode>();
		callerSFormOuts.addAll(callerPdg.getStaticFormOuts());

		ArrayList<ActualInOutNode> callActOuts = new ArrayList<ActualInOutNode>();
		callActOuts.addAll(callerPdg.getRootActOuts(cNode));

		Log.debug("Call from " + callerPdg + " to " + calleePDG);
		Log.debug("No actOut found for " + fOut);
		Log.debug("ActOuts: " + actOuts);
		Log.debug("Callee FormOuts:" + calleeFormOuts);
		Log.debug("Callee sFormOuts:" + calleeSFormOuts);
		Log.debug("Caller FormOuts:" + callerFormOuts);
		Log.debug("Caller sFormOuts:" + callerSFormOuts);
	}
}
