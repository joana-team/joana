package edu.kit.joana.ifc.orlsod;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.jgrapht.alg.TransitiveClosure;

import edu.kit.joana.ifc.sdg.graph.SDG;
import edu.kit.joana.ifc.sdg.graph.SDGEdge;
import edu.kit.joana.ifc.sdg.graph.SDGNode;
import edu.kit.joana.ifc.sdg.graph.slicer.conc.CFGForward;
import edu.kit.joana.ifc.sdg.graph.slicer.conc.I2PBackward;
import edu.kit.joana.ifc.sdg.graph.slicer.graph.CFG;
import edu.kit.joana.ifc.sdg.graph.slicer.graph.building.ICFGBuilder;
import edu.kit.joana.ifc.sdg.graph.slicer.graph.threads.PreciseMHPAnalysis;
import edu.kit.joana.ifc.sdg.lattice.IStaticLattice;
import edu.kit.joana.ifc.sdg.lattice.LatticeUtil;
import edu.kit.joana.util.Pair;
import edu.kit.joana.util.maps.MapUtils;

public class TimimgClassificationChecker<L> {

	/** the lattice which provides the security levels we annotate nodes with */
	protected final IStaticLattice<L> secLattice;

	/** the SDG we want to check */
	protected final SDG sdg;

	/** user-provided annotations */
	protected final Map<SDGNode, L> userAnn;


	/** maps each node to its so-called <i>probabilistic influencers</i> */
	protected final ProbInfComputer probInf;

	protected final CFG icfg;
	protected final Map<SDGNode, Collection<SDGNode>> transClosure;
	
	protected final Map<SDGNode, Set<SDGNode>> timingDependence;
	protected final Map<Pair<SDGNode,SDGNode>, Collection<? extends SDGNode>> chops;
	
	protected final SimpleTCFGChopper tcfgChopper;
	protected final CFGForward tcfgForwardSlicer;

	
	private final ICDomOracle cdomOracle;
	
	protected final PreciseMHPAnalysis mhp;
	
	/** classification which is computed in a fixed-point iteration */
	protected Map<SDGNode, L> cl;
	
	protected Map<Pair<SDGNode, SDGNode>, L> clt; 

	public TimimgClassificationChecker(SDG sdg, IStaticLattice<L> secLattice, Map<SDGNode, L> userAnn, ProbInfComputer probInf, PreciseMHPAnalysis mhp, ICDomOracle cdomOracle) {
		this.sdg = sdg;
		this.secLattice = secLattice;
		this.userAnn = userAnn;
		this.probInf = probInf;
		this.mhp = mhp;
		this.cdomOracle = cdomOracle;
		
		this.transClosure = new HashMap<>();
		
		// TODO: try not to annoy Jürgen by "doing Work" in a constructor!
		this.icfg = ICFGBuilder.extractICFG(sdg);
		
		this.tcfgChopper = new SimpleTCFGChopper(icfg);
		this.tcfgForwardSlicer = new CFGForward(icfg);
		this.chops = new HashMap<>();
		
		final Map<SDGNode, Set<SDGNode>> timingDependence = new HashMap<>();
		for (SDGNode n : icfg.vertexSet()) {
			final List<SDGEdge> edges =
				icfg.outgoingEdgesOf(n)
				    .stream()
				    .filter(e -> !e.getKind().equals(SDGEdge.Kind.FORK))
				    .collect(Collectors.toList());
			
			final int nr = edges.size();
			
			// ENTRY Nodes may have three successors:
			//       i)   A formal-in node, which eventually leads to the procedured "real" control flow
			//       ii)  The EXIT node, as required for control-deps 
			//       iii) A formal-out node, also leading to to EXIT Node
			// TODO: possibly fix this in the graph construction
			
			// The following still doesn't hold
			// TODO: characterize exceptions to this assertion.
			//assert (nr==0 || nr == 1 || nr == 2 || n.kind.equals(SDGNode.Kind.ENTRY));
			
			if (nr == 2) {
				final SDGNode n2 = edges.get(0).getTarget();
				final SDGNode n3 = edges.get(1).getTarget();
				
				transClosure.computeIfAbsent(n2, tcfgForwardSlicer::slice);
				transClosure.computeIfAbsent(n3, tcfgForwardSlicer::slice);
				
				final Set<SDGNode> dependentNodes =
					transClosure.get(n2)
					            .stream()
					            .filter(transClosure.get(n3)::contains)
					            .collect(Collectors.toSet());
				timingDependence.put(n, dependentNodes);
			} else {
				timingDependence.put(n, Collections.emptySet());
			}
		}
		this.timingDependence = MapUtils.invert(timingDependence);
		
	}

	protected Map<SDGNode, L> initCL(boolean incorporateSourceAnns) {
		Map<SDGNode, L> ret = new HashMap<SDGNode, L>();
		for (SDGNode n : sdg.vertexSet()) {
			if (incorporateSourceAnns && userAnn.containsKey(n)) {
				ret.put(n, userAnn.get(n));
			} else {
				ret.put(n, secLattice.getBottom());
			}
		}
		return ret;
	}

	protected Map<Pair<SDGNode,SDGNode>, L> initCLT() {
		Map<Pair<SDGNode,SDGNode>, L> ret = new HashMap<>();
		for (SDGNode n : icfg.vertexSet()) {
			for (SDGNode m : icfg.vertexSet()) {
				if (mhp.isParallel(m, n)) ret.put(Pair.pair(n,m), secLattice.getBottom());
			}
		}
		return ret;
	}
	
	private boolean interferenceWriteUndirected(SDGNode n) {
		for (SDGEdge e : sdg.getOutgoingEdgesOfKind(n, SDGEdge.Kind.INTERFERENCE_WRITE) ) {
			boolean found = false;
			for (SDGEdge e2 : sdg.getOutgoingEdgesOfKind(e.getTarget(), SDGEdge.Kind.INTERFERENCE_WRITE)) {
				if (e2.getTarget().equals(n)) found = true;
			}
			return found;
		}
		return true;
	}
	
	public int check() {
		I2PBackward backw = new I2PBackward(sdg);
		// 1.) initialize classification: we go from the bottom up, so every node is classified as low initially
		// except for the sources: They are classified with the user-annotated source level
		cl  = initCL(true);
		clt = initCLT(); 
		// 2.) fixed-point iteration
		int numIters = 0;
		boolean change;
		do {
			change = false;
			for (SDGNode n : sdg.vertexSet()) {
				L oldLevel = cl.get(n);
				// nothing changes if current level is top already
				if (secLattice.getTop().equals(oldLevel)) continue;
				L newLevel = oldLevel;
				// 2a.) propagate from backward slice
				System.out.println(String.format("BS(%s) = %s", n, backw.slice(n)));
				for (SDGNode m : backw.slice(n)) {
					newLevel = secLattice.leastUpperBound(newLevel, cl.get(m));
					if (secLattice.getTop().equals(newLevel)) {
						break; // we can abort the loop here - level cannot get any higher
					}
				}
				
				// 2b.) propagate security levels from the relative timing of
				//      i)  n, and
				//      ii) nodes m from other threads whose execution write some value used at n
				//          (i.e.: there is an interference dependence from m to n, or
				//           a interference-write dependence between n and m)
				assert(interferenceWriteUndirected(n));
				
				for (SDGEdge e : sdg.getIncomingEdgesOfKind(n, SDGEdge.Kind.INTERFERENCE)) {
					final SDGNode m = e.getSource();
					newLevel = secLattice.leastUpperBound(newLevel, clt.get(Pair.pair(n, m)));
					if (secLattice.getTop().equals(newLevel)) {
						break; // we can abort the loop here - level cannot get any higher
					}
				}
				for (SDGEdge e : sdg.getIncomingEdgesOfKind(n, SDGEdge.Kind.INTERFERENCE_WRITE)) {
					final SDGNode m = e.getSource();
					newLevel = secLattice.leastUpperBound(newLevel, clt.get(Pair.pair(n, m)));
					if (secLattice.getTop().equals(newLevel)) {
						break; // we can abort the loop here - level cannot get any higher
					}
				}
				if (!newLevel.equals(oldLevel)) {
					cl.put(n, newLevel);
					change = true;
				}
			}
			
			for (Pair<SDGNode,SDGNode> nm : clt.keySet()) {
				L oldLevel = clt.get(nm);
				// nothing changes if current level is top already
				if (secLattice.getTop().equals(oldLevel)) continue;
				L newLevel = oldLevel;
				
				final SDGNode n = nm.getFirst();
				final SDGNode m = nm.getSecond();
				
				transClosure.computeIfAbsent(n, tcfgForwardSlicer::slice);
				transClosure.computeIfAbsent(m, tcfgForwardSlicer::slice);
				
				for (int threadN : n.getThreadNumbers()) {
					for (int threadM : m.getThreadNumbers()) {
						final SDGNode c = cdomOracle.cdom(n, threadN ,m, threadM).getNode();
						chops.computeIfAbsent(
							Pair.pair(c, n),
							pair -> tcfgChopper.chop(pair.getFirst(), pair.getSecond())
						);
						chops.computeIfAbsent(
								Pair.pair(c, m),
								pair -> tcfgChopper.chop(pair.getFirst(), pair.getSecond())
						);
						List<? extends SDGNode> relevant = Stream.concat(
							chops.get(Pair.pair(c,n))
							     .stream()
							     .filter(timingDependence.get(n)::contains),
							chops.get(Pair.pair(c,m))
								     .stream()
								     .filter(timingDependence.get(m)::contains)
						).collect(Collectors.toList());
						
						for (SDGNode c2 : relevant) {
							newLevel = secLattice.leastUpperBound(newLevel, cl.get(c2));
							if (secLattice.getTop().equals(newLevel)) {
								break; // we can abort the loop here - level cannot get any higher
							}
						}
					}
				}
				if (!newLevel.equals(oldLevel)) {
					clt.put(nm, newLevel);
					change = true;
				}
				

				
			}
			
			numIters++;
		} while (change);
		System.out.println(String.format("needed %d iteration(s).", numIters));
		// 3.) check that sink levels comply
		return checkCompliance();
	}

	protected final int checkCompliance() {
		boolean compliant = true;
		int noViolations = 0;
		for (Map.Entry<SDGNode, L> snkEntry : userAnn.entrySet()) {
			SDGNode s = snkEntry.getKey();
			L snkLevel = snkEntry.getValue();
			if (!LatticeUtil.isLeq(secLattice, cl.get(s), snkLevel)) {
				System.out.println("Violation at node " + s + ": user-annotated level is " + snkLevel + ", computed level is " + cl.get(s));
				noViolations++;
				compliant = false;
			}
		}
		for (Pair<SDGNode,SDGNode> nm : clt.keySet()) {
			final SDGNode n = nm.getFirst();
			final SDGNode m = nm.getSecond();
			if(userAnn.containsKey(n) && userAnn.containsKey(m) && (
			      !LatticeUtil.isLeq(secLattice, clt.get(nm), userAnn.get(n))
			   || !LatticeUtil.isLeq(secLattice, clt.get(nm), userAnn.get(m)))
			){
				System.out.println("Violation at nodes " + nm + ": user-annotated levels are (" +
			                       userAnn.get(n) + ", "+ userAnn.get(m) + "), " +
				                   "but their relative timing is classified " + clt.get(nm));
				noViolations++;
				compliant = false;
			}
		}
		if (compliant) {
			System.out.println("no violations found.");
		}
		return noViolations;
	}
}
