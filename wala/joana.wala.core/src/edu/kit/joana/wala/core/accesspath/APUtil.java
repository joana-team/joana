/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.wala.core.accesspath;

import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import com.ibm.wala.util.CancelException;
import com.ibm.wala.util.WalaException;
import com.ibm.wala.util.graph.Graph;
import com.ibm.wala.util.graph.impl.SparseNumberedGraph;
import com.ibm.wala.util.intset.OrdinalSet;

import edu.kit.joana.wala.core.NullProgressMonitor;
import edu.kit.joana.wala.core.PDG;
import edu.kit.joana.wala.core.PDGEdge;
import edu.kit.joana.wala.core.PDGNode;
import edu.kit.joana.wala.core.accesspath.APIntraProcV2.MergeInfo;
import edu.kit.joana.wala.core.accesspath.APIntraProcV2.Merges;
import edu.kit.joana.wala.core.accesspath.AccessPathV2.AliasEdge;
import edu.kit.joana.wala.core.accesspath.nodes.APGraph;
import edu.kit.joana.wala.core.accesspath.nodes.APNode;
import edu.kit.joana.wala.core.params.objgraph.dataflow.ModRefControlFlowGraph;
import edu.kit.joana.wala.flowless.util.DotUtil;
import edu.kit.joana.wala.util.WriteGraphToDot;
import gnu.trove.procedure.TIntProcedure;
import gnu.trove.set.TIntSet;

/**
 * Utils mainly for debugging purposes.
 * 
 * @author Juergen Graf <juergen.graf@gmail.com>
 */
public final class APUtil {

	private APUtil() {}
	
	public static void writeCFGtoFile(final String outDir, final PDG pdg, final String suffix)
			throws WalaException, CancelException {
		final Graph<PDGNode> graph = extractCFG(pdg); 
		writeCFGtoFile(graph, outDir, pdg, suffix);
	}
	
	public static void writeCFGtoFile(final Graph<PDGNode> graph, final String outDir, final PDG pdg, final String suffix)
			throws WalaException, CancelException {
		final String outFile = outputFileName(outDir, pdg, suffix); 
		DotUtil.dot(graph, outFile);
	}

	public static void writeAPGraphToFile(final APGraph graph, final String outDir, final PDG pdg, final String suffix)
			throws WalaException, CancelException {
		final String outFile = outputFileName(outDir, pdg, suffix); 
		DotUtil.dot(graph, outFile);
	}

	public static void writeModRefCFGtoFile(final ModRefControlFlowGraph graph, final String outDir, final PDG pdg,
			final String suffix) throws WalaException, CancelException {
		final String outFile = outputFileName(outDir, pdg, suffix); 
		DotUtil.dot(graph, outFile);
	}

	public static String sanitizedPDGname(final PDG pdg) {
		return WriteGraphToDot.sanitizeFileName(pdg.getMethod().getName().toString());
	}
	
	public static String outputFileName(final String outDir, final PDG pdg, final String suffix) {
		final String sanOutDir = WriteGraphToDot.fixupDirectoryName(outDir);
		final String sanPdgName = sanitizedPDGname(pdg);
		return sanOutDir + sanPdgName + suffix;
	}

	private static List<APNode> extractAPNodes(final TIntSet pdgNodeIds, final PDG pdg, final APIntraProcV2 ap) {
		final List<APNode> apnodelist = new LinkedList<>();
		pdgNodeIds.forEach(new TIntProcedure() {
			@Override
			public boolean execute(final int id) {
				final PDGNode n = pdg.getNodeWithId(id);
				final APNode apn = ap.findAPNode(n);
				apnodelist.add(apn);
				return false;
			}
		});
		
		return apnodelist;
	}
	
	private static Set<AP> extractAPs (final Collection<APNode> nodes) {
		final Set<AP> aps = new HashSet<>();
		for (final APNode n : nodes) {
			final Iterator<AP> it = n.getOutgoingPaths();
			while (it.hasNext()) {
				aps.add(it.next());
			}
		}
		
		return aps;
	}
	
	private static String extractAPstr(final TIntSet pdgNodeIds, final PDG pdg, final APIntraProcV2 ap) {
		final List<APNode> nodes = extractAPNodes(pdgNodeIds, pdg, ap);
		final Set<AP> aps = extractAPs(nodes);
		final StringBuilder sb = new StringBuilder();
		for (final AP p : aps) {
			sb.append(p.toString());
			sb.append(",");
		}
		if (sb.length() > 0) {
			sb.deleteCharAt(sb.length() - 1);
		}
		
		return sb.toString();
	}
	
	private static String extractAliasStr(final AliasEdge e, final PDG pdg, final APIntraProcV2 ap) {
		final String from = extractAPstr(e.fromAlias, pdg, ap);
		final String to = extractAPstr(e.toAlias, pdg, ap);
		return "(" + from + ") -> (" + to + ")";
	}

	private static String extractAPstr(final APNode n) {
		if (n == null) {
			return "no paths";
		}
		final Set<AP> aps = extractAPs(Collections.singleton(n));
		final StringBuilder sb = new StringBuilder();
		for (final AP p : aps) {
			sb.append(p.toString());
			sb.append(",");
		}
		if (sb.length() > 0) {
			sb.deleteCharAt(sb.length() - 1);
		}
		
		return sb.toString();
	}
	

	private static String extractFineGrainedAliasStr(final AliasEdge e, final APIntraProcV2 ap) {
		final APNode apFrom = ap.findAPNode(e.edge.from);
		final APNode apTo = ap.findAPNode(e.edge.to);

		final String from = extractAPstr(apFrom);
		final String to = extractAPstr(apTo);
		
		return "(" + from + ") -> (" + to + ")";
	}
	
	public static Graph<PDGNode> extractCFG(final PDG pdg) {
		final SparseNumberedGraph<PDGNode> graph = new SparseNumberedGraph<>(2);
		
		for (final PDGNode n : pdg.vertexSet()) {
			if (n.getPdgId() == pdg.getId()) {
				graph.addNode(n);
			}
		}
		
		for (final PDGEdge e : pdg.edgeSet()) {
			if (e.kind == PDGEdge.Kind.CONTROL_FLOW || e.kind == PDGEdge.Kind.CONTROL_FLOW_EXC) {
				graph.addEdge(e.from, e.to);
			}
		}
		
		return graph;
	}
	
	public static void writeAliasEdgesToFile(final APIntraProcV2 ap, final String debugAccessPathOutputDir,
			final PDG pdg, final String suffix) {
		final String outFile = outputFileName(debugAccessPathOutputDir, pdg, suffix);
		final int numAliasEdges = ap.getMergeInfo().getNumAliasEdges();
		try {
			final PrintWriter pw = new PrintWriter(outFile);
			pw.println("access paths for all nodes...");
			for (final PDGNode n : pdg.vertexSet()) {
				final APNode nAP = ap.findAPNode(n);
				pw.println(n.getId() + "|" + n.getKind() + "|" + n.getLabel() + ": " + extractAPstr(nAP));
			}
			pw.println("found alias edges: " + numAliasEdges);
			pw.println("extracting access path graph");
			final Set<AP> aps = ap.getMergeInfo().getAllAPs();
			for (final AP p : aps) {
				pw.println("(" + p + ")");
			}
//			for (final AliasEdge e : alias) {
//				if (e.edge.from.getKind() == PDGNode.Kind.NORMAL && e.edge.to.getKind() == PDGNode.Kind.NORMAL) {
//					pw.println(extractFineGrainedAliasStr(e, ap));
//				}
//			}
			pw.println("extract intraproc merge operations...");
			final MergeInfo mops = ap.getMergeInfo();
			for (final Merges mop : mops.getAllMerges()) {
				pw.println(mop);
			}
			pw.println("computing reach mops...");
			try {
				mops.computeReach(ap.getConfig(), NullProgressMonitor.INSTANCE);
			} catch (CancelException e1) {
				e1.printStackTrace();
			}
			for (final PDGNode n : pdg.vertexSet()) {
				final OrdinalSet<Merges> ms = mops.getReachM(n);
				pw.print(n.getId() + "|" + n.getKind() + "|" + n.getLabel() + ": ");
				if (ms == null || ms.isEmpty()) {
					pw.println("none.");
				} else {
					for (final Merges m : ms) {
						pw.print(m);
						pw.print("; ");
					}
					pw.println();
				}
			}
			pw.flush();
			pw.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		
	}
	
}
