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
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import com.ibm.wala.util.CancelException;
import com.ibm.wala.util.WalaException;
import com.ibm.wala.util.graph.Graph;
import com.ibm.wala.util.graph.impl.SparseNumberedGraph;
import com.ibm.wala.util.intset.OrdinalSet;

import edu.kit.joana.wala.core.NullProgressMonitor;
import edu.kit.joana.wala.core.PDG;
import edu.kit.joana.wala.core.PDGEdge;
import edu.kit.joana.wala.core.PDGNode;
import edu.kit.joana.wala.core.accesspath.APContextManager.CallContext;
import edu.kit.joana.wala.core.accesspath.APIntraProcV2.MergeInfo;
import edu.kit.joana.wala.core.accesspath.APIntraProcV2.MergeOp;
import edu.kit.joana.wala.core.accesspath.APIntraProcV2.Merges;
import edu.kit.joana.wala.core.accesspath.AccessPathV2.AliasEdge;
import edu.kit.joana.wala.core.accesspath.nodes.APGraph;
import edu.kit.joana.wala.core.accesspath.nodes.APNode;
import edu.kit.joana.wala.core.params.objgraph.dataflow.ModRefControlFlowGraph;
import edu.kit.joana.wala.flowless.util.DotUtil;
import edu.kit.joana.wala.util.WriteGraphToDot;
import gnu.trove.procedure.TIntProcedure;
import gnu.trove.set.TIntSet;
import gnu.trove.set.hash.TIntHashSet;

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
	
	public static void writeResultToFile(final APResult result,  final String debugAccessPathOutputDir,
			final PDG mainPdg, final String suffix) {
		final String outFile = outputFileName(debugAccessPathOutputDir, mainPdg, suffix);
		try {
			final PrintWriter pw = new PrintWriter(outFile);
			final APContextManager start = result.get(mainPdg.getId());
			final LinkedList<APContextManager> worklist = new LinkedList<>();
			worklist.add(start);
			final TIntSet visited = new TIntHashSet();
			visited.add(start.getPdgId());
			
			while (!worklist.isEmpty()) {
				final APContextManager cur = worklist.removeFirst();
				
				printContext(cur, pw);
				
				for (final CallContext call : cur.getCallContexts()) {
					if (!visited.contains(call.calleeId)) {
						visited.add(call.calleeId);
						final APContextManager callee = result.get(call.calleeId);
						worklist.add(callee);
					}
				}
			}
			pw.flush();
			pw.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
	}
	
	private static void printContext(final APContextManager ctx, final PrintWriter pw) {
		pw.println("====================================");
		pw.println(ctx);
		pw.println("--- all paths ---");
		{
			final SortedSet<String> sorted = new TreeSet<>();
			for (final AP ap : ctx.getAllPaths()) {
				sorted.add(ap.toString());
			}
			for (final String str : sorted) {
				pw.println(str);
			}
		}
		pw.println("--- mapped nodes ---");
		{
			final TIntSet allMapped = ctx.getAllMappedNodes();
			final int[] keys = allMapped.toArray();
			Arrays.sort(keys);
			for (final int cur : keys) {
				final Set<AP> paths = ctx.getAccessPaths(cur);
				if (paths == null || paths.isEmpty()) continue;
				
				pw.print(cur + "\t: ");
				for (final AP ap : paths) {
					pw.print(ap + " ");
				}
				pw.println();
			}
		}
		pw.println("--- merge ops ---");
		{
			for (final MergeOp mop : ctx.getOrigMerges()) {
				pw.println("(" + mop.id + ") " + mop);
			}
		}
		pw.println("--- mop map ---");
		{
			final TIntSet allMapped = ctx.getAllMappedNodes();
			final int[] keys = allMapped.toArray();
			Arrays.sort(keys);
			for (final int id : keys) {
				final OrdinalSet<MergeOp> reached = ctx.getReachingMerges(id);
				if (reached == null || reached.isEmpty()) continue;
				
				pw.print(id + "\t: ");
				for (final MergeOp mop : reached) {
					pw.print("(" + mop.id + ") " + mop + "; ");
				}
				pw.println();
			}
		}
		pw.println("====================================");
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
