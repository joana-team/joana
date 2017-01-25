/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.deprecated.jsdg.util;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.Comparator;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;

import com.ibm.wala.util.collections.HashMapFactory;

import edu.kit.joana.deprecated.jsdg.sdg.PDG;
import edu.kit.joana.deprecated.jsdg.sdg.SDG;
import edu.kit.joana.deprecated.jsdg.sdg.nodes.AbstractPDGNode;
import edu.kit.joana.deprecated.jsdg.sdg.nodes.AbstractParameterNode;
import edu.kit.joana.deprecated.jsdg.sdg.nodes.CallNode;
import edu.kit.joana.deprecated.jsdg.sdg.nodes.CatchNode;
import edu.kit.joana.deprecated.jsdg.sdg.nodes.ConstantPhiValueNode;
import edu.kit.joana.deprecated.jsdg.sdg.nodes.EntryNode;
import edu.kit.joana.deprecated.jsdg.sdg.nodes.ExpressionNode;
import edu.kit.joana.deprecated.jsdg.sdg.nodes.IPDGNodeVisitor;
import edu.kit.joana.deprecated.jsdg.sdg.nodes.JDependencyGraph.EdgeType;
import edu.kit.joana.deprecated.jsdg.sdg.nodes.NormalNode;
import edu.kit.joana.deprecated.jsdg.sdg.nodes.PhiValueNode;
import edu.kit.joana.deprecated.jsdg.sdg.nodes.PredicateNode;
import edu.kit.joana.deprecated.jsdg.sdg.nodes.SyncNode;
import edu.kit.joana.deprecated.jsdg.sdg.parammodel.objtree.ActualInOutNode;
import edu.kit.joana.deprecated.jsdg.sdg.parammodel.objtree.FormInOutNode;

/**
 * Count edges, nodes, etc. for all PDGs in the SDG.
 *
 * @author Juergen Graf <graf@kit.edu>
 *
 */
public class SDGStatistics implements Serializable {

	private static final long serialVersionUID = 781109994474690891L;

	private final Stats sdgStat;

	public static class Stats implements Serializable {

		private static final long serialVersionUID = -8260995537370081342L;

		private String name;

		public Stats(String name) {
			this.name = name;
		}

		public int nodeCount = 0;
		public long edgeCount = 0;
		public int  instructionCount = 0;

		public long edgeCtrlDep = 0;
		public long edgeDataDep = 0;
		public long edgeParamInDep = 0;
		public long edgeParamOutDep = 0;
		public long edgeSummary = 0;
		public long edgeWriteWriteInterference = 0;
		public long edgeReadWriteInterference = 0;

		public int rootFormIn = 0;
		public int rootFormOut = 0;
		public int staticFormIn = 0;
		public int staticFormOut = 0;
		public int fieldFormIn = 0;
		public int fieldFormOut = 0;

		public int rootActIn = 0;
		public int rootActOut = 0;
		public int staticActIn = 0;
		public int staticActOut = 0;
		public int fieldActIn = 0;
		public int fieldActOut = 0;

		public int callSites = 0;
		public int callPossibleTargets = 0;

		public String toString() {
			return statToString(this);
		}

	}

	private final Map<Integer, Stats> pdgId2stats;

	public SDGStatistics() {
		this.sdgStat = new Stats("SDGStats");
		this.pdgId2stats = HashMapFactory.make();
	}

	public void buildStatistics(SDG sdg) {
		this.sdgStat.name = sdg.toString();
		for (PDG pdg : sdg.getAllContainedPDGs()) {
			buildStatistics(pdg);
		}
	}

	private void buildStatistics(PDG pdg) {
		Stats stats = pdgId2stats.get(pdg.getId());
		if (stats == null) {
			stats = new Stats(pdg.toString());
			pdgId2stats.put(pdg.getId(), stats);
		}

		nodeVisitor.setCurrentStats(stats);
		for (AbstractPDGNode node : pdg) {
			if (node.getPdgId() == pdg.getId()) {
				node.accept(nodeVisitor);
				countOutgoingEdges(pdg, node, stats);
			}
		}
	}

	public Stats getStatistics() {
		return sdgStat;
	}

	private static int getTotalParameterNodes(Stats stat) {
		return stat.rootFormIn + stat.staticFormIn + stat.fieldFormIn +
			stat.rootFormOut + stat.staticFormOut + stat.fieldFormOut +
			stat.rootActIn + stat.staticActIn + stat.fieldActIn +
			stat.rootActOut + stat.staticActOut + stat.fieldActOut;
	}

	private static int getTotalFormNodes(Stats stat) {
		return stat.rootFormIn + stat.staticFormIn + stat.fieldFormIn +
			stat.rootFormOut + stat.staticFormOut + stat.fieldFormOut;
	}

	private void countOutgoingEdges(PDG pdg, AbstractPDGNode node, Stats stats) {
		for (EdgeType edge : EdgeType.values()) {
			int succNodes = pdg.getSuccNodeCount(node, edge);

			switch(edge) {
			case CC:
			case CD_TRUE:
			case CD_FALSE:
			case CE:
			case UN:
				sdgStat.edgeCtrlDep += succNodes;
				stats.edgeCtrlDep += succNodes;
				break;
			case SU:
				sdgStat.edgeSummary += succNodes;
				stats.edgeSummary += succNodes;
				break;
			case PI:
				sdgStat.edgeParamInDep += succNodes;
				stats.edgeParamInDep += succNodes;
				break;
			case PO:
				sdgStat.edgeParamOutDep += succNodes;
				stats.edgeParamOutDep += succNodes;
				break;
			case IW:
				sdgStat.edgeWriteWriteInterference += succNodes;
				stats.edgeWriteWriteInterference += succNodes;
				break;
			case ID:
				sdgStat.edgeReadWriteInterference += succNodes;
				stats.edgeReadWriteInterference += succNodes;
				break;
			case DD:
			case DH:
				sdgStat.edgeDataDep += succNodes;
				stats.edgeDataDep += succNodes;
				break;
			default: // nothing to do here
			}
			sdgStat.edgeCount += succNodes;
			stats.edgeCount += succNodes;
		}
	}

	public static String statToString(Stats stat) {
		StringBuilder str = new StringBuilder("Statistics for " + stat.name);
		str.append("\nTotal nodes: ").append(stat.nodeCount);
		str.append("\nTotal parameter nodes: ").append(getTotalParameterNodes(stat));
		str.append("\nTotal instructions: ").append(stat.instructionCount);
		str.append("\nNormal nodes / parameter nodes: ").append((((double) stat.nodeCount - (double) getTotalParameterNodes(stat)) / (double) getTotalParameterNodes(stat)));
		str.append("\nFormal nodes: ").append(stat.rootFormIn + stat.staticFormIn +
				stat.fieldFormIn + stat.rootFormOut + stat.staticFormOut + stat.fieldFormOut);
		str.append("\nFormal-in nodes: ").append(stat.rootFormIn + stat.staticFormIn + stat.fieldFormIn);
		str.append("\nFormal-out nodes: ").append(stat.rootFormOut + stat.staticFormOut + stat.fieldFormOut);
		str.append("\nFormal-in root: ").append(stat.rootFormIn);
		str.append("\nFormal-in static: ").append(stat.staticFormIn);
		str.append("\nFormal-in field: ").append(stat.fieldFormIn);
		str.append("\nFormal-out root: ").append(stat.rootFormOut);
		str.append("\nFormal-out static: ").append(stat.staticFormOut);
		str.append("\nFormal-out field: ").append(stat.fieldFormOut);
		str.append("\nCallsites: ").append(stat.callSites);
		str.append("\nPossible targets of all callsites: ").append(stat.callPossibleTargets);
		str.append("\nPossible targets per callsite: ").append(((double) stat.callPossibleTargets / (double) stat.callSites));
		str.append("\nActual nodes: ").append(stat.rootActIn + stat.staticActIn + stat.fieldActIn +
				stat.rootActOut + stat.staticActOut + stat.fieldActOut);
		str.append("\nActual-in nodes: ").append(stat.rootActIn + stat.staticActIn + stat.fieldActIn);
		str.append("\nActual-out nodes: ").append(stat.rootActOut + stat.staticActOut + stat.fieldActOut);
		str.append("\nActual-in root: ").append(stat.rootActIn);
		str.append("\nActual-in static: ").append(stat.staticActIn);
		str.append("\nActual-in field: ").append(stat.fieldActIn);
		str.append("\nActual-out root: ").append(stat.rootActOut);
		str.append("\nActual-out static: ").append(stat.staticActOut);
		str.append("\nActual-out field: ").append(stat.fieldActOut);
		str.append("\nTotal edges: ").append(stat.edgeCount);
		str.append("\nControl dep edges: ").append(stat.edgeCtrlDep);
		str.append("\nData dep edges: ").append(stat.edgeDataDep);
		str.append("\nParam-in edges: ").append(stat.edgeParamInDep);
		str.append("\nParam-out edges: ").append(stat.edgeParamOutDep);
		str.append("\nSummary edges: ").append(stat.edgeSummary);
		str.append("\nRead-write interference: ").append(stat.edgeReadWriteInterference);
		str.append("\nWrite-write interference: ").append(stat.edgeWriteWriteInterference);

		return str.toString();
	}

	public String toString() {
		return sdgStat.toString();
	}

	private PDGStatVisitor nodeVisitor = new PDGStatVisitor();

	private class PDGStatVisitor implements IPDGNodeVisitor, Serializable {

		private static final long serialVersionUID = 1257563080229342797L;

		private Stats pdgStat;

		public void setCurrentStats(Stats stat) {
			pdgStat = stat;
		}

		public void visitCall(CallNode node) {
			sdgStat.nodeCount++;
			pdgStat.nodeCount++;

			sdgStat.instructionCount++;
			pdgStat.instructionCount++;

//			int targets = node.getPossibleTargets().size();
//
//			if (targets == 0) {
//				//TODO count callsites with no targets in extra counter.
//			} else {
//				sdgStat.callSites++;
//				pdgStat.callSites++;
//
//				sdgStat.callPossibleTargets += targets;
//				pdgStat.callPossibleTargets += targets;
//			}
		}

		public void visitCatch(CatchNode node) {
			sdgStat.nodeCount++;
			pdgStat.nodeCount++;

			sdgStat.instructionCount++;
			pdgStat.instructionCount++;
		}

		public void visitConstPhiValue(ConstantPhiValueNode node) {
			sdgStat.nodeCount++;
			pdgStat.nodeCount++;
		}

		public void visitEntry(EntryNode node) {
			sdgStat.nodeCount++;
			pdgStat.nodeCount++;
		}

		public void visitExpression(ExpressionNode node) {
			sdgStat.nodeCount++;
			pdgStat.nodeCount++;

			sdgStat.instructionCount++;
			pdgStat.instructionCount++;
		}

		public void visitNormal(NormalNode node) {
			sdgStat.nodeCount++;
			pdgStat.nodeCount++;

			sdgStat.instructionCount++;
			pdgStat.instructionCount++;
		}

		public void visitPredicate(PredicateNode node) {
			sdgStat.nodeCount++;
			pdgStat.nodeCount++;

			sdgStat.instructionCount++;
			pdgStat.instructionCount++;

		}

		public void visitSync(SyncNode node) {
			sdgStat.nodeCount++;
			pdgStat.nodeCount++;

			sdgStat.instructionCount++;
			pdgStat.instructionCount++;
		}

		public void visitParameter(AbstractParameterNode node) {
			sdgStat.nodeCount++;
			pdgStat.nodeCount++;

			if (node.isException()) {
				sdgStat.rootFormOut++;
				pdgStat.rootFormOut++;
			} else if (node.isExit()) {
				if (!node.isVoid()) {
					sdgStat.rootFormOut++;
					pdgStat.rootFormOut++;
				}
			} else if (node.isFormal()) {
				if (node.isIn()) {
					if (node.isOnHeap()) {
						if (node instanceof FormInOutNode) {
							if (((FormInOutNode) node).isStatic()) {
								sdgStat.staticFormIn++;
								pdgStat.staticFormIn++;
							} else {
								sdgStat.fieldFormIn++;
								pdgStat.fieldFormIn++;
							}
						} else {
							sdgStat.fieldFormIn++;
							pdgStat.fieldFormIn++;
						}
					} else {
						sdgStat.rootFormIn++;
						pdgStat.rootFormIn++;
					}
				} else {
					if (node.isOnHeap()) {
						if (node instanceof FormInOutNode) {
							if (((FormInOutNode) node).isStatic()) {
								sdgStat.staticFormOut++;
								pdgStat.staticFormOut++;
							} else {
								sdgStat.fieldFormOut++;
								pdgStat.fieldFormOut++;
							}
						} else {
							sdgStat.fieldFormOut++;
							pdgStat.fieldFormOut++;
						}
					} else {
						sdgStat.rootFormOut++;
						pdgStat.rootFormOut++;
					}
				}
			} else {
				if (node.isIn()) {
					if (node.isOnHeap()) {
						if (node instanceof ActualInOutNode) {
							if (((ActualInOutNode) node).isStatic()) {
								sdgStat.staticActIn++;
								pdgStat.staticActIn++;
							} else {
								sdgStat.fieldActIn++;
								pdgStat.fieldActIn++;
							}
						} else {
							sdgStat.fieldActIn++;
							pdgStat.fieldActIn++;
						}
					} else {
						sdgStat.rootActIn++;
						pdgStat.rootActIn++;
					}
				} else {
					if (node.isOnHeap()) {
						if (node instanceof ActualInOutNode) {
							if (((ActualInOutNode) node).isStatic()) {
								sdgStat.staticActOut++;
								pdgStat.staticActOut++;
							} else {
								sdgStat.fieldActOut++;
								pdgStat.fieldActOut++;
							}
						} else {
							sdgStat.fieldActOut++;
							pdgStat.fieldActOut++;
						}
					} else {
						sdgStat.rootActOut++;
						pdgStat.rootActOut++;
					}
				}
			}
		}

		public void visitPhiValue(PhiValueNode node) {
			sdgStat.nodeCount++;
			pdgStat.nodeCount++;
		}

	};

	public SortedSet<Stats> getPdgStatsSortedBy(Comparator<Stats> comp) {
		SortedSet<Stats> stats = new TreeSet<Stats>(comp);
		stats.addAll(pdgId2stats.values());

		return stats;
	}

	public static final Comparator<Stats> sortByFormalNodes = new Comparator<Stats>() {

		public int compare(Stats o1, Stats o2) {
			return getTotalFormNodes(o1) - getTotalFormNodes(o2);
		}

	};

	public static final Comparator<Stats> sortBySummaryEdges = new Comparator<Stats>() {

		public int compare(Stats o1, Stats o2) {
			//UGLY this is unsave - but its only for statistics...
			return (int) o1.edgeSummary -  (int) o2.edgeSummary;
		}

	};

	public static void writeTo(SDGStatistics stats, String filename) throws IOException {
		FileOutputStream fos = new FileOutputStream(filename);
		BufferedOutputStream bos = new BufferedOutputStream(fos);
		ObjectOutputStream oos = new ObjectOutputStream(bos);
		oos.writeObject(stats);
		oos.flush();
		oos.close();
	}

	public static SDGStatistics readFrom(String filename) throws IOException, ClassNotFoundException {
		SDGStatistics stats;

		FileInputStream fis = new FileInputStream(filename);
		BufferedInputStream bis = new BufferedInputStream(fis);
		ObjectInputStream ois = new ObjectInputStream(bis);
		stats = (SDGStatistics) ois.readObject();

		ois.close();

		return stats;
	}

	public static void main(String args[]) throws IOException, ClassNotFoundException {
		SDGStatistics stats = readFrom("../TempStuff/output/staticfield.SFieldPropagation.pdg.stats");
		SortedSet<Stats> pdgStats = stats.getPdgStatsSortedBy(SDGStatistics.sortByFormalNodes);
		for (Stats s : pdgStats) {
			System.out.println(s.toString());
		}
	}

}
