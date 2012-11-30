/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.ifc.sdg.graph.slicer;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import edu.kit.joana.ifc.sdg.graph.SDG;
import edu.kit.joana.ifc.sdg.graph.SDGEdge;
import edu.kit.joana.ifc.sdg.graph.SDGNode;


public abstract class StepSlicer implements Slicer {
	protected Set<SDGEdge.Kind> omittedEdges = SDGEdge.Kind.threadEdges();
	protected Set<SDGEdge.Kind> stepEdges;
	protected SDG g;

	interface EdgePredicate {
		public boolean phase1();
		public boolean follow(SDGEdge e);
		public boolean saveInOtherWorklist(SDGEdge e);
	}

	/**
	 * Creates a new instance of SummarySlicer
	 */
	public StepSlicer(SDG graph, Set<SDGEdge.Kind> omit, Set<SDGEdge.Kind> step) {
		this.g = graph;
		this.omittedEdges = omit;
		this.stepEdges = step;
	}

	public StepSlicer(SDG graph, Set<SDGEdge.Kind> step) {
		this.g = graph;
		this.stepEdges = step;
	}

	protected abstract Collection<SDGEdge> edgesToTraverse(SDGNode node);

	protected abstract SDGNode reachedNode(SDGEdge edge);

	protected abstract EdgePredicate phase1Predicate();

	protected abstract EdgePredicate phase2Predicate();

    public Collection<SDGNode> slice(SDGNode criterion) {
    	return stepSlice(criterion).keySet();
    }

	public Collection<SDGNode> slice(Collection<SDGNode> criteria) {
		return stepSlice(criteria).keySet();
	}

	public Map<SDGNode, Integer> stepSlice(SDGNode criterion) {
		return stepSlice(Collections.singleton(criterion));
	}


	public Map<SDGNode, Integer> stepSlice(Collection<SDGNode> criteria) {
		Map<SDGNode, Integer> steps = new HashMap<SDGNode, Integer>();
		StepSlice stepSlicer = new StepSlice();

		int step= 0;
		StepResult sr = null;
		Map<SDGNode, SDGNode> stepCriteria = new HashMap<SDGNode, SDGNode>();
		for (SDGNode n : criteria) {
			stepCriteria.put(n, n);
		}

		do {
			sr = stepSlicer.sliceStep(stepCriteria);

			stepCriteria = sr.nextStep;

			for (SDGNode x : sr.stepSlice) {
				if (steps.get(x) == null) {
					steps.put(x, step);
				}
			}

//			System.out.println("step "+step+": "+sr);
			step++;

		} while (sr.nextStep());

		return steps;
	}

	public void setOmittedEdges(Set<SDGEdge.Kind> omit){
		this.omittedEdges = omit;
	}

	public void setStepEdges(Set<SDGEdge.Kind> step){
		this.stepEdges = step;
	}

	public void setGraph(SDG g) {
		this.g = g;
	}


	class StepSlice {
		Map<SDGNode, SDGNode> slice = new HashMap<SDGNode, SDGNode>();

		public StepResult sliceStep(Map<SDGNode, SDGNode> criteria) {
			Set<SDGNode> stepSlice = new HashSet<SDGNode>();
			Map<SDGNode, SDGNode> nextStep = new HashMap<SDGNode, SDGNode>();
			LinkedList<SDGNode> worklist1 = new LinkedList<SDGNode>();
			LinkedList<SDGNode> worklist2 = new LinkedList<SDGNode>();
			EdgePredicate p = phase1Predicate();

			for (SDGNode key : criteria.keySet()) {
				if (criteria.get(key) == null) {
					// phase 2
					worklist2.add(key);
					slice.put(key, null);

				} else {
					// phase 1
					worklist1.add(key);
					slice.put(key, key);
				}

				stepSlice.add(key);
			}

			while (!worklist1.isEmpty()) {
				SDGNode w = worklist1.poll();

				for (SDGEdge e : edgesToTraverse(w)) {
					if (!e.getKind().isSDGEdge() || omittedEdges.contains(e.getKind()))
						continue;

					SDGNode v = reachedNode(e);

					if (!slice.containsKey(v) || (p.phase1() && slice.get(v) == null)) {
						// if node was not yet added or node was added in phase2
						if (p.saveInOtherWorklist(e)) {
							if (stepEdges.contains(e.getKind())) {
								nextStep.put(v, null);

							} else {
								worklist2.add(v);
								slice.put(v, null);
								stepSlice.add(v);
							}

						} else if (p.follow(e)) {
							if (stepEdges.contains(e.getKind())) {
								nextStep.put(v, v);

							} else {
								worklist1.add(v);
								slice.put(v, v);
								stepSlice.add(v);
							}
						}
					}
				}
			}

			p = phase2Predicate();

			while (!worklist2.isEmpty()) {
				SDGNode w = worklist2.poll();

				for (SDGEdge e : edgesToTraverse(w)) {
					if (!e.getKind().isSDGEdge() || omittedEdges.contains(e.getKind()))
						continue;

					SDGNode v = reachedNode(e);

					if (!slice.containsKey(v)) {
						// if node was not yet added
						if (p.follow(e)) {
							if (stepEdges.contains(e.getKind()) && !nextStep.containsKey(v)) {
								nextStep.put(v, null);

							} else {
								worklist2.add(v);
								slice.put(v, null);
								stepSlice.add(v);
							}
						}
					}
				}
			}

			return new StepResult(stepSlice, nextStep);
		}
	}

	static class StepResult {
		Set<SDGNode> stepSlice;
		Map<SDGNode, SDGNode> nextStep;

		StepResult(Set<SDGNode> stepSlice, Map<SDGNode, SDGNode> nextStep) {
			this.stepSlice = stepSlice;
			this.nextStep = nextStep;
		}

		boolean nextStep() {
			return !nextStep.isEmpty();
		}

		public String toString() {
			return "("+stepSlice+", "+nextStep.keySet()+")";
		}
	}


	/* Debug */
	public static void main (String[] args) throws Exception {
		/* 1 */
		String file = "/afs/info.uni-karlsruhe.de/user/giffhorn/Desktop/eclipse/runtime-New_configuration/Tests/jSDG/conc.TimeTravel.pdg";
		//	String file = "/afs/info.uni-karlsruhe.de/user/giffhorn/Desktop/eclipse/runtime-New_configuration/Tests/jSDG/conc.bb.ProducerConsumer.pdg";
		//	String file = "/afs/info.uni-karlsruhe.de/user/giffhorn/Desktop/eclipse/runtime-New_configuration/Tests/jSDG/conc.dp.DiningPhilosophers.pdg";
		//	String file = "/afs/info.uni-karlsruhe.de/user/giffhorn/Desktop/eclipse/runtime-New_configuration/Tests/jSDG/conc.lg.LaplaceGrid.pdg";
//		String file = "/afs/info.uni-karlsruhe.de/user/giffhorn/Desktop/eclipse/runtime-New_configuration/Tests/jSDG/conc.ac.AlarmClock.pdg";

		//  String file = "/afs/info.uni-karlsruhe.de/user/giffhorn/Desktop/eclipse/runtime-ME_configuration/Barcode/jSDG/MainEmulator.pdg";
		//  String file = "/afs/info.uni-karlsruhe.de/user/giffhorn/Desktop/eclipse/runtime-ME_configuration/BluetoothLogger/jSDG/MainEmulator.pdg";
		//  String file = "/afs/info.uni-karlsruhe.de/user/giffhorn/Desktop/eclipse/runtime-ME_configuration/CellSafe/jSDG/cellsafe.MainEmulator.pdg";
		//  String file = "/afs/info.uni-karlsruhe.de/user/giffhorn/Desktop/eclipse/runtime-ME_configuration/GoldenSMS/jSDG/pt.uminho.msm.goldensms.midlet.MessageEmulator.pdg";
		//  String file = "/afs/info.uni-karlsruhe.de/user/giffhorn/Desktop/eclipse/runtime-ME_configuration/GoldenSMS/jSDG/pt.uminho.msm.goldensms.midlet.ReceptionEmulator.pdg";
		//  String file = "/afs/info.uni-karlsruhe.de/user/giffhorn/Desktop/eclipse/runtime-ME_configuration/Guitar/jSDG/it.denzosoft.denzoGuitarSoft.midp.MainEmulator.pdg";
		//  String file = "/afs/info.uni-karlsruhe.de/user/giffhorn/Desktop/eclipse/runtime-ME_configuration/Hyper-M/jSDG/edu.hit.nus.can.gui.MainEmulator.pdg";
		//  String file = "/afs/info.uni-karlsruhe.de/user/giffhorn/Desktop/eclipse/runtime-ME_configuration/J2MESafe/jSDG/MainEmulator.pdg";
		//  String file = "/afs/info.uni-karlsruhe.de/user/giffhorn/Desktop/eclipse/runtime-ME_configuration/JRemCntl/jSDG/Emulator.pdg";
		//  String file = "/afs/info.uni-karlsruhe.de/user/giffhorn/Desktop/eclipse/runtime-ME_configuration/KeePassJ2ME/jSDG/MainEmulator.pdg";
		//  String file = "/afs/info.uni-karlsruhe.de/user/giffhorn/Desktop/eclipse/runtime-ME_configuration/maza/jSDG/Sergi.MainEmulator.pdg";
		//  String file = "/afs/info.uni-karlsruhe.de/user/giffhorn/Desktop/eclipse/runtime-ME_configuration/OneTimePass/jSDG/MainEmulator.pdg";

		SDG g = SDG.readFrom(file);

		Set<SDGEdge.Kind> stepEdges = SDGEdge.Kind.threadEdges();
		stepEdges.add(SDGEdge.Kind.CONTROL_DEP_UNCOND);
		stepEdges.add(SDGEdge.Kind.CONTROL_DEP_COND);
		stepEdges.add(SDGEdge.Kind.CONTROL_DEP_EXPR);
		stepEdges.add(SDGEdge.Kind.CONTROL_DEP_CALL);
		stepEdges.add(SDGEdge.Kind.JUMP_DEP);
		stepEdges.add(SDGEdge.Kind.CALL);

		System.out.println("initializing the slicer...");
		StepSlicerBackward one = new StepSlicerBackward(g, stepEdges);
		System.out.println("done");

		System.out.println(file);
		System.out.println("criteria: "+g.vertexSet().size());

		for (SDGNode n : g.vertexSet()) {
			Map<SDGNode, Integer> slice = one.stepSlice(n);
			int max = 0;
			for (int i : slice.values()) {
				if (i > max) max = i;
			}
			max += 1;
			System.out.println(max);
			ArrayList<Set<SDGNode>> result = new ArrayList<Set<SDGNode>>(max);
			for (int i = 0; i < max; i++) {
				result.add(new TreeSet<SDGNode>(SDGNode.getIDComparator()));
			}
			for (SDGNode x : slice.keySet()) {
				int pos = slice.get(x);
				result.get(pos).add(x);
			}
			System.out.println(result);
		}
	}
}
