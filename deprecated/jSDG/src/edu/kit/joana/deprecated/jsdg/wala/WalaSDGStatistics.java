/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.deprecated.jsdg.wala;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.Comparator;
import java.util.Iterator;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import com.ibm.wala.analysis.pointers.HeapGraph;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.CallGraph;
import com.ibm.wala.ipa.callgraph.propagation.InstanceKey;
import com.ibm.wala.ipa.callgraph.propagation.PointerKey;
import com.ibm.wala.ipa.slicer.NormalStatement;
import com.ibm.wala.ipa.slicer.PDG;
import com.ibm.wala.ipa.slicer.SDG;
import com.ibm.wala.ipa.slicer.Statement;
import com.ibm.wala.ssa.SSAInstruction;
import com.ibm.wala.ssa.SSAInvokeInstruction;
import com.ibm.wala.util.collections.HashSetFactory;

import edu.kit.joana.deprecated.jsdg.util.Util;

public class WalaSDGStatistics implements Serializable {

	private static final long serialVersionUID = -3932775900044970472L;

	private final Stats sdgStat;
	private final HeapStats heapStat;
	private final Set<Stats> pdgStats;

	public WalaSDGStatistics() {
		sdgStat = new Stats("Wala SDG");
		heapStat = new HeapStats();
		pdgStats = HashSetFactory.make();
	}

	public static class Stats implements Serializable {

		private static final long serialVersionUID = -2405700766288193334L;

		private String name;

		public Stats(String str) {
			this.name = str;
		}

		private int nodes = 0;
		private int edges = 0;
		private int actIn = 0;
		private int actOut = 0;
		private int actInHeap = 0;
		private int actOutHeap = 0;
		private int formIn = 0;
		private int formOut = 0;
		private int formInHeap = 0;
		private int formOutHeap = 0;
		private int statement = 0;
		private int callSites = 0;
		private int unusedNodes = 0;

		public String toString() {
			StringBuilder str = new StringBuilder("Statistics of " + name);

			str.append("\nnodes: ").append(nodes);
			str.append("\nedges: ").append(edges);
			str.append("\nnormal nodes per param nodes: ").append((double) (nodes - getTotalParamNodes(this)) / (double) getTotalParamNodes(this));
			str.append("\nparameter nodes: ").append(getTotalParamNodes(this));
			str.append("\nformal nodes: ").append(getTotalFormNodes(this));
			str.append("\nform-in: ").append(formIn);
			str.append("\nform-in heap: ").append(formInHeap);
			str.append("\nform-out: ").append(formOut);
			str.append("\nform-out heap: ").append(formOutHeap);
			str.append("\nactual nodes: ").append(getTotalActNodes(this));
			str.append("\nact-in: ").append(actIn);
			str.append("\nact-in heap: ").append(actInHeap);
			str.append("\nact-out: ").append(actOut);
			str.append("\nact-out heap: ").append(actOutHeap);
			str.append("\nstatements: ").append(statement);
			str.append("\nunused nodes: ").append(unusedNodes).append(" -> ").append(100 * (double) unusedNodes / (double) nodes).append("%");
			str.append("\ncallsites: ").append(callSites);
			str.append("\nact nodes per callsite: ").append((double) getTotalActNodes(this) / (double) callSites);
			str.append("\nedges per node: ").append((double) edges / (double) nodes);

			return str.toString();
		}
	}

	public void buildStats(SDG<?> sdg) {
		sdgStat.nodes = sdg.getNumberOfNodes();
		CallGraph cg = sdg.getCallGraph();
		for (Iterator<CGNode> it = cg.iterator(); it.hasNext();) {
			CGNode node = it.next();
			PDG<?> pdg = sdg.getPDG(node);
			analyze(pdg);
		}
		HeapGraph<?> hg = sdg.getPointerAnalysis().getHeapGraph();
		buildStats(hg);
	}

	public static class HeapStats implements Serializable {

		private static final long serialVersionUID = -1940209794164547047L;

		private int pointerKeys = 0;
		private int instanceKeys = 0;
		private int unknownNode = 0;
		private int edges = 0;
		private int edgesFromPK = 0;
		private int edgesFromIK = 0;
		private int nodes = 0;

		public String toString() {
			StringBuilder str = new StringBuilder("HeapGraph stats:");

			str.append("\nnodes: ").append(nodes);
			str.append("\nedges: ").append(edges);
			str.append("\nedges from ik: ").append(edgesFromIK);
			str.append("\nedges from pk: ").append(edgesFromPK);
			str.append("\npointer-keys: ").append(pointerKeys);
			str.append("\ninstance-keys: ").append(instanceKeys);
			str.append("\nunknown nodes: ").append(unknownNode);
			str.append("\navg pointer-keys referenced from a single ik: ").append((double) edgesFromIK / (double) instanceKeys);
			str.append("\navg instance-keys referenced from a single pk: ").append((double) edgesFromPK / (double) pointerKeys);
			str.append("\nedges to pk / num pks: ").append((double) edgesFromIK / (double) pointerKeys);
			str.append("\nedges to ik / num iks: ").append((double) edgesFromPK / (double) instanceKeys);

			return str.toString();
		}

	}

	private void buildStats(HeapGraph<?> hg) {
		heapStat.nodes = hg.getNumberOfNodes();
		for (Iterator<Object> it = hg.iterator(); it.hasNext();) {
			Object obj = it.next();
			int	succs = hg.getSuccNodeCount(obj);

			if (obj instanceof PointerKey) {
				heapStat.pointerKeys++;
				heapStat.edgesFromPK += succs;
			} else if (obj instanceof InstanceKey) {
				heapStat.instanceKeys++;
				heapStat.edgesFromIK += succs;
			} else {
				heapStat.unknownNode++;
			}
			heapStat.edges++;
		}
	}

	private void analyze(PDG<?> pdg) {
		Stats stat = new Stats(Util.methodName(pdg.getCallGraphNode().getMethod()));
		pdgStats.add(stat);

		for (Iterator<Statement> it = pdg.iterator(); it.hasNext();) {
			Statement st = it.next();
			int succs = 0;
			for (Iterator<? extends Statement> itSucc = pdg.getSuccNodes(st); itSucc.hasNext();) {
				//Statement succ =
				itSucc.next();
				succs++;
			}
			if (succs == 0) {
				int preds = 0;
				for (Iterator<? extends Statement> itPreds = pdg.getPredNodes(st); itPreds.hasNext();) {
					//Statement pred =
					itPreds.next();
					preds++;
				}
				if (preds == 0) {
					stat.unusedNodes++;
					sdgStat.unusedNodes++;
				}
			}
			sdgStat.edges += succs;
			stat.edges += succs;
			switch (st.getKind()) {
			case CATCH:
				break;
			case EXC_RET_CALLEE:
				sdgStat.formOut++;
				stat.formOut++;
				break;
			case EXC_RET_CALLER:
				sdgStat.actOut++;
				stat.actOut++;
				break;
			case HEAP_PARAM_CALLEE:
				sdgStat.formInHeap++;
				stat.formInHeap++;
				break;
			case HEAP_PARAM_CALLER:
				sdgStat.actInHeap++;
				stat.actInHeap++;
				break;
			case HEAP_RET_CALLEE:
				sdgStat.formOutHeap++;
				stat.formOutHeap++;
				break;
			case HEAP_RET_CALLER:
				sdgStat.actOutHeap++;
				stat.actOutHeap++;
				break;
			case METHOD_ENTRY:
				break;
			case METHOD_EXIT:
				break;
			case NORMAL:
				sdgStat.statement++;
				stat.statement++;
				NormalStatement norm = (NormalStatement) st;
				SSAInstruction instr = norm.getInstruction();
				if (instr != null && instr instanceof SSAInvokeInstruction) {
					sdgStat.callSites++;
					stat.callSites++;
				}
				break;
			case NORMAL_RET_CALLEE:
				sdgStat.formOut++;
				stat.formOut++;
				break;
			case NORMAL_RET_CALLER:
				sdgStat.actOut++;
				stat.actOut++;
				break;
			case PARAM_CALLEE:
				sdgStat.formIn++;
				stat.formIn++;
				break;
			case PARAM_CALLER:
				sdgStat.actIn++;
				stat.actIn++;
				break;
			case PHI:
				break;
			case PI:
				break;
			}
		}
	}

	public static int getTotalFormNodes(Stats stat) {
		return stat.formIn + stat.formInHeap + stat.formOut + stat.formOutHeap;
	}

	public static int getTotalActNodes(Stats stat) {
		return stat.actIn + stat.actInHeap + stat.actOut + stat.actOutHeap;
	}

	public static int getTotalParamNodes(Stats stat) {
		return getTotalActNodes(stat) + getTotalFormNodes(stat);
	}

	public SortedSet<Stats> getPdgStatsSortedBy(Comparator<Stats> comp) {
		SortedSet<Stats> stats = new TreeSet<Stats>(comp);
		stats.addAll(pdgStats);

		return stats;
	}

	public static final Comparator<Stats> sortByFormalNodes = new Comparator<Stats>() {

		public int compare(Stats o1, Stats o2) {
			return getTotalFormNodes(o1) - getTotalFormNodes(o2);
		}

	};

	public String toString() {
		return sdgStat.toString() + "\n" + heapStat.toString();
	}

	public static void writeTo(WalaSDGStatistics stats, String filename) throws IOException {
		FileOutputStream fos = new FileOutputStream(filename);
		BufferedOutputStream bos = new BufferedOutputStream(fos);
		ObjectOutputStream oos = new ObjectOutputStream(bos);
		oos.writeObject(stats);
		oos.flush();
		oos.close();
	}

	public static WalaSDGStatistics readFrom(String filename) throws IOException, ClassNotFoundException {
		WalaSDGStatistics stats;

		FileInputStream fis = new FileInputStream(filename);
		BufferedInputStream bis = new BufferedInputStream(fis);
		ObjectInputStream ois = new ObjectInputStream(bis);
		stats = (WalaSDGStatistics) ois.readObject();

		ois.close();

		return stats;
	}

	public static void main(String args[]) throws IOException, ClassNotFoundException {
		WalaSDGStatistics stats = readFrom("../TempStuff/output/staticfield.SFieldPropagation.pdg.stats");
		SortedSet<Stats> pdgStats = stats.getPdgStatsSortedBy(WalaSDGStatistics.sortByFormalNodes);
		for (Stats s : pdgStats) {
			System.out.println(s.toString());
		}
	}

}
