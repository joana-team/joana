/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.ifc.sdg.core.conc;

import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import edu.kit.joana.ifc.sdg.core.SecurityNode;
import edu.kit.joana.ifc.sdg.core.sdgtools.SDGTools;
import edu.kit.joana.ifc.sdg.core.violations.AbstractConflictLeak;
import edu.kit.joana.ifc.sdg.core.violations.ConflictEdge;
import edu.kit.joana.ifc.sdg.core.violations.IConflictLeak;
import edu.kit.joana.ifc.sdg.graph.SDG;
import edu.kit.joana.ifc.sdg.graph.SDGEdge;
import edu.kit.joana.ifc.sdg.graph.SDGNode;
import edu.kit.joana.ifc.sdg.graph.slicer.Slicer;
import edu.kit.joana.ifc.sdg.graph.slicer.conc.CFGForward;
import edu.kit.joana.ifc.sdg.graph.slicer.conc.I2PBackward;
import edu.kit.joana.ifc.sdg.graph.slicer.conc.nanda.Nanda;
import edu.kit.joana.ifc.sdg.graph.slicer.conc.nanda.NandaBackward;
import edu.kit.joana.ifc.sdg.graph.slicer.graph.threads.MHPAnalysis;
import edu.kit.joana.ifc.sdg.graph.slicer.graph.threads.PreciseMHPAnalysis;
import edu.kit.joana.ifc.sdg.lattice.IStaticLattice;
import edu.kit.joana.util.Maybe;
import edu.kit.joana.util.Pair;

/**
 * Checks whether a given program satisfies the 'low-security observational
 * determinism (LSOD)' security property under the assumption, that it is
 * non-interferent in the possibilistic sense. Then, it suffices to check for
 * order conflicts and data conflicts (cf. PhD thesis of Dennis Giffhorn). The result of 
 * the check is not binary, but rather a collection of conflicts which may lead to the violation of 
 * the LSOD criterion. <p/>
 * There is also the possibility to use an optimization mentioned in Giffhorn's PhD thesis, where
 * only those conflicts are reported, which are triggered by a secret source (secret with respect
 * to the security level of the observer of the conflicts or their effects, respectively). <p/>
 * However, use this optimization with care since there is no soundness proof for it (as of July 2013).
 * 
 * @author Dennis Giffhorn
 * @author Martin Mohr
 * 
 */
public class LSODNISlicer implements ConflictScanner {

	/**
	 * the lattice which provides the security levels which the SDG under
	 * analysis is annotated with
	 */
	private IStaticLattice<String> l;

	/** the SDG under analysis */
	private SDG g;

	/** MHP analysis which is used to determine order conflicts */
	private MHPAnalysis mhp;

	/** computes, adds and removes conflict edges */
	private ConflictEdgeManager confEdgeMan;

	/** records actual conflicts found in the actual algorithm */
	private ConflictManager conf;

	private Collection<SecurityNode> sources;
	private Collection<SecurityNode> sinks;

	/** whether the used slicer shall be time-sensitive */
	private boolean timeSens;

	/** whether Giffhorn's Optimization to exclude benign conflicts shall be used */
	private boolean useOptimization = false;

	/**
	 * Initialisiert die Analyse. Fuehrt eine MHP-Analyse aus und fuegt
	 * Konfliktkanten in den SDG ein.
	 * 
	 * @param g
	 *            Ein SDG.
	 * @param l
	 *            Ein Sicherheitsverband.
	 * @param conf
	 *            Ein ConflictManager.
	 */
	public LSODNISlicer(SDG g, IStaticLattice<String> l, ConflictManager conf) {
		this(g, l, conf, PreciseMHPAnalysis.analyze(g), false);
	}

	public LSODNISlicer(SDG g, IStaticLattice<String> l, ConflictManager conf,
			MHPAnalysis mhp, boolean timeSens) {
		this(g, l, conf, mhp, timeSens, false);
	}

	public LSODNISlicer(SDG g, IStaticLattice<String> l, ConflictManager conf,
			MHPAnalysis mhp, boolean timeSens, boolean useOptimization) {
		this.l = l;
		this.g = g;
		this.conf = conf;
		this.mhp = mhp;
		this.timeSens = timeSens;
		this.sources = SDGTools.getInformationSources(g);
		this.sinks = SDGTools.getInformationSinks(g);
		this.confEdgeMan = new ConflictEdgeManager();
		this.useOptimization = useOptimization;
	}

	/**
	 * Fuehrt den Sicherheitscheck aus.
	 * 
	 * @return Die Menge der gefundenen Sicherheitsverletzungen.
	 */
	public Set<IConflictLeak<SecurityNode>> check() {
		LinkedList<Element> criteria = collectCriteria();
		this.sources.clear();
		this.sinks.clear();
		this.sources.addAll(SDGTools.getInformationSources(g));
		this.sinks.addAll(SDGTools.getInformationSinks(g));
		Set<IConflictLeak<SecurityNode>> set = new HashSet<IConflictLeak<SecurityNode>>();
		confEdgeMan.computeConflictEdges();
		confEdgeMan.addConflictEdges();
		conf.init();
		for (Element e : criteria) {
			scanForConflicts(e);
		}
		confEdgeMan.removeConflictEdges();
		set.addAll(conf.getConflicts());

		return set;
	}

	/**
	 * Bestimmt die annotierten Knoten im SDG.
	 * 
	 * @return Die annotierten Knoten.
	 */
	private LinkedList<Element> collectCriteria() {
		LinkedList<Element> criteria = new LinkedList<Element>();

		// suche alle annotierten knoten (keine deklassifikationen)
		for (SDGNode o : g.vertexSet()) {
			SecurityNode temp = (SecurityNode) o;
			if (temp.isInformationEndpoint()) {
				Element e = new Element(temp, temp.getLevel());
				criteria.add(e);
			}
		}

		return criteria;
	}

	private void scanForConflicts(Element e) {
		// HIGH nodes can only be seen by a HIGH attacker,
		// and a HIGH attacker can already see every input
		if (e.node.getLevel().equals(l.getTop())) {
			return;
		}
		DataConflictCollector confCollector = new DataConflictCollector();
		Slicer slicer;
		if (this.timeSens) {
			slicer = new Nanda(g, new NandaBackward(), confCollector);
		} else {
			slicer = new I2PBackward(g);
		}
		Collection<SDGNode> s = slicer.slice(e.node);

		for (SDGNode n : s) {
			collectPossibleDataChannels(e, (SecurityNode) n);
		}
		collectPossibleOrderChannels(e);
	}

	private void collectPossibleDataChannels(Element e, SecurityNode n) {
		// if n has an incoming data conflict edge, add a violation
		for (SDGEdge inc : g.getIncomingEdgesOfKind(n,
				SDGEdge.Kind.CONFLICT_DATA)) {
			// possible probabilistic data channel
			if (useOptimization) {
				Collection<SecurityNode> secTriggers = collectSecretTriggers(inc,
						e.node.getLevel());
				for (SecurityNode secTrigger : secTriggers) {
					conf.addTriggeredDataConflict(inc, secTrigger, e.node,
							e.node.getLevel());
				}
			} else {
				conf.addPossiblyUntriggeredDataConflict(inc, e.node,
						e.node.getLevel());
			}
		}
	}

	private void collectPossibleOrderChannels(Element e) {
		SecurityNode n = e.node;
		// if n has an incoming or outgoing order conflict edge and the conflict
		// is low-observable, add a violation
		List<SDGEdge> oConfs = g.getIncomingEdgesOfKind(n,
				SDGEdge.Kind.CONFLICT_ORDER);
		oConfs.addAll(g.getOutgoingEdgesOfKind(n, SDGEdge.Kind.CONFLICT_ORDER));
		for (SDGEdge oConf : oConfs) {
			// Calculate lowest level which can observe the conflict:
			// This is the least upper bounds of both levels since both conflicting parts must be seen.
			// Note that CONFLICT_ORDER edges only exist between annotated nodes,
			// so we do not have to check for null here.
			String refLevel = l.leastUpperBound(((SecurityNode) oConf.getSource()).getLevel(),
			                                    ((SecurityNode) oConf.getTarget()).getLevel());
			// If refLevel is HIGH, the conflict can only be seen by a HIGH attacker,
			// and a HIGH attacker can already see every input, so the conflict does not cause a leak.
			// In contrast, assume refLevel is not HIGH (i.e. lower).
			// There might be a HIGH source that influences the conflict.
			// Since a attacker of refLevel can see the conflict, he can learn something about HIGH input.
			// This would be a leak, since refLevel is lower than HIGH.
			if (!refLevel.equals(l.getTop())) {
				// possible probabilistic order channel
				if (useOptimization) {
					Collection<SecurityNode> secTriggers = collectSecretTriggers(
							oConf, refLevel);
					for (SecurityNode secTrigger : secTriggers) {
						conf.addTriggeredOrderConflict(oConf, secTrigger,
								refLevel);
					}
				} else {
					conf.addPossiblyUntriggeredOrderConflict(oConf, refLevel);
				}
			}
		}
	}

	private Collection<SecurityNode> collectSecretTriggers(SDGEdge confEdge,
			String refLevel) {
		Collection<SecurityNode> ret = new LinkedList<SecurityNode>();
		SDGNode a = confEdge.getSource();
		SDGNode b = confEdge.getTarget();
		CFGForward forw = new CFGForward(g);
		for (SDGNode n : this.sources) {
			SecurityNode secN = (SecurityNode) n;
			if (secN.isInformationSource()
					&& !l.isLeq( secN.getLevel(), refLevel)) {
				Collection<SDGNode> reachable = forw.slice(secN);
				if ((reachable.contains(a) || mhp.isParallel(secN, a))
						& (reachable.contains(b) || mhp.isParallel(secN, b))) {
					ret.add(secN);
				}
			}
		}

		return ret;
	}

	/**
	 * Returns all the conflicts found in the last run of this algorithm.
	 */
	public Collection<AbstractConflictLeak<SecurityNode>> getAllConflicts() {
		return conf.getConflicts();
	}

	/* Factories */
	public static LSODNISlicer simpleCheck(SDG g, IStaticLattice<String> l) {
		return simpleCheck(g, l, PreciseMHPAnalysis.analyze(g), false);
	}

	public static LSODNISlicer simpleCheck(SDG g, IStaticLattice<String> l,
			MHPAnalysis mhp, boolean timeSens) {
		return new LSODNISlicer(g, l, new SimpleConflicts(), mhp, timeSens);
	}

	public static LSODNISlicer simpleCheck(SDG g, IStaticLattice<String> l,
			boolean useOptimization) {
		return simpleCheck(g, l, PreciseMHPAnalysis.analyze(g), false,
				useOptimization);
	}

	public static LSODNISlicer simpleCheck(SDG g, IStaticLattice<String> l,
			MHPAnalysis mhp, boolean timeSens, boolean useOptimization) {
		return new LSODNISlicer(g, l, new SimpleConflicts(), mhp, timeSens,
				useOptimization);
	}

	/**
	 * Helper class to compute conflict edges. Also saves the conflict edges and
	 * provides methods to add the conflict edges (before the actual LSOD
	 * algorithm) and remove them afterwards. Users of this class should use it
	 * as follows:
	 * <ol>
	 * <li>{@link #computeConflictEdges}</li>
	 * <li>{no changes of SDG in between}</li>
	 * <li>{@link #addConflictEdges}</li>
	 * <li>{run LSOD algorithm}</li>
	 * <li>{@link #removeConflictEdges}</li>
	 * <li>{changes of SDG possible again}</li>
	 * <li>{@link #computeConflictEdges}</li>
	 * <li>...</li>
	 * </ol>
	 * 
	 * @author Dennis Giffhorn
	 * @author Martin Mohr
	 */
	private class ConflictEdgeManager {

		private final List<SDGEdge> dataConflictEdges = new LinkedList<SDGEdge>();
		private final List<SDGEdge> orderConflictEdges = new LinkedList<SDGEdge>();

		/**
		 * Computes all conflict edges, i.e. all data conflicts and all order
		 * conflicts.
		 */
		void computeConflictEdges() {
			this.dataConflictEdges.clear();
			this.orderConflictEdges.clear();
			computeOrderConflicts();
			computeDataConflicts();
		}

		/**
		 * Computes all order conflict edges. Two nodes are in an order
		 * conflict, if they are both annotated and may happen in parallel.
		 */
		private void computeOrderConflicts() {
			// Consider all annotated nodes, i.e. all sources and sinks
			Collection<SecurityNode> annotatedNodes = new HashSet<SecurityNode>();
			annotatedNodes.addAll(sources);
			annotatedNodes.addAll(sinks);

			for (SecurityNode m : annotatedNodes) {
				for (SecurityNode n : annotatedNodes) {
					if (mhp.isParallel(m, n)) {
						SDGEdge edge = new SDGEdge(m, n,
								SDGEdge.Kind.CONFLICT_ORDER);
						orderConflictEdges.add(edge);
					}
				}
			}
		}

		/**
		 * Computes all data conflicts.<br>
		 * If there is an interference-read edge between two nodes n1 and n2,
		 * then a corresponding data-conflict edge n1 --dc--> n2 is added.<br>
		 * If there is an interference-write edge between n1 and n2, then two
		 * data-conflict edges n1 --dc--> n2 and n2 --dc--> n1 are added.
		 */
		private void computeDataConflicts() {
			// suche nach interferenzen -> potentielle data-konflikte
			for (SDGEdge edge : g.edgeSet()) {
				if (edge.getKind() == SDGEdge.Kind.INTERFERENCE) {
					SDGEdge e = new SDGEdge(edge.getSource(), edge.getTarget(),
							SDGEdge.Kind.CONFLICT_DATA);
					dataConflictEdges.add(e);
				} else if (edge.getKind() == SDGEdge.Kind.INTERFERENCE_WRITE) {
					// bidirected conflict
					SDGEdge e = new SDGEdge(edge.getSource(), edge.getTarget(),
							SDGEdge.Kind.CONFLICT_DATA);
					SDGEdge f = new SDGEdge(edge.getTarget(), edge.getSource(),
							SDGEdge.Kind.CONFLICT_DATA);
					dataConflictEdges.add(e);
					dataConflictEdges.add(f);
				}
			}
		}

		/**
		 * Adds the previously computed conflict edges to the sdg.
		 */
		void addConflictEdges() {
			for (SDGEdge e : orderConflictEdges) {
				g.addEdge(e);
			}

			for (SDGEdge e : dataConflictEdges) {
				g.addEdge(e);
			}
		}

		/**
		 * Removes the previously computed conflict edges from the sdg.
		 */
		void removeConflictEdges() {
			for (SDGEdge e : orderConflictEdges) {
				g.removeEdge(e);
			}

			for (SDGEdge e : dataConflictEdges) {
				g.removeEdge(e);
			}

		}
	}

	/**
	 * Interface, das die Verwendung zweier Strategien zulaesst: Entweder werden
	 * nur Quellen und Senken eines Lecks bestimmt (SimpleConflicts), oder es
	 * werden zusaetzlich auch die Races bestimmt, die zu dem LEck fuehren.
	 * 
	 * @author giffhorn
	 */
	private interface ConflictManager {

		/**
		 * initializes the conflict manager. Called before the actual algorithm
		 * starts, all conflicts found in the last run are deleted.
		 */
		void init();

		/**
		 * Liefert alle bisher gefundenen Konflikte.
		 * 
		 * @return Alle bisher gefundenen Konflikte.
		 */
		Collection<AbstractConflictLeak<SecurityNode>> getConflicts();
		Collection<DataConflict<SecurityNode>> getDataConflicts();
		Collection<OrderConflict<SecurityNode>> getOrderConflicts();
		
		void addPossiblyUntriggeredOrderConflict(SDGEdge confEdge,
				String attackerLevel);

		void addTriggeredOrderConflict(SDGEdge confEdge, SecurityNode trigger,
				String attackerLevel);

		void addPossiblyUntriggeredDataConflict(SDGEdge confEdge,
				SecurityNode influenced, String attackerLevel);

		void addTriggeredDataConflict(SDGEdge confEdge, SecurityNode trigger,
				SecurityNode influenced, String attackerLevel);

	}

	private static class SimpleConflicts implements ConflictManager {
		// menge der bisherigen konflikte
		private final LinkedList<AbstractConflictLeak<SecurityNode>> conflicts;
		private final LinkedList<DataConflict<SecurityNode>> dataConflicts;
		private final LinkedList<OrderConflict<SecurityNode>> orderConflicts;
		private final Set<Pair<SecurityNode, SecurityNode>> ocEdges = new HashSet<Pair<SecurityNode, SecurityNode>>();

		/**
		 * Initialisierung.
		 */
		public SimpleConflicts() {
			conflicts = new LinkedList<AbstractConflictLeak<SecurityNode>>();
			dataConflicts = new LinkedList<DataConflict<SecurityNode>>();
			orderConflicts = new LinkedList<OrderConflict<SecurityNode>>();
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see
		 * edu.kit.joana.ifc.sdg.core.conc.LSODNISlicer.ConflictManager#init()
		 */
		@Override
		public void init() {
			conflicts.clear();
		}

		/**
		 * Liefert alle bisher gefundenen Konflikte.
		 * 
		 * @return Alle bisher gefundenen Konflikte.
		 */
		public Collection<AbstractConflictLeak<SecurityNode>> getConflicts() {
			return new LinkedList<AbstractConflictLeak<SecurityNode>>(conflicts);
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see edu.kit.joana.ifc.sdg.core.conc.LSODNISlicer.ConflictManager#
		 * addPossiblyUntriggeredOrderConflict
		 * (edu.kit.joana.ifc.sdg.graph.SDGEdge, java.lang.String)
		 */
		@Override
		public void addPossiblyUntriggeredOrderConflict(SDGEdge confEdge,
				String attackerLevel) {
			if (!ocEdges.contains(Pair.pair(confEdge.getSource(),
					confEdge.getTarget()))) {
				OrderConflict<SecurityNode> oc = new OrderConflict<SecurityNode>(ConflictEdge.fromSDGEdge(confEdge),
						attackerLevel);
				addOrderConflict(oc);
			}
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see edu.kit.joana.ifc.sdg.core.conc.LSODNISlicer.ConflictManager#
		 * addTriggeredOrderConflict(edu.kit.joana.ifc.sdg.graph.SDGEdge,
		 * edu.kit.joana.ifc.sdg.graph.SDGNode, java.lang.String)
		 */
		@Override
		public void addTriggeredOrderConflict(SDGEdge confEdge,
				SecurityNode trigger, String attackerLevel) {
			if (!ocEdges.contains(Pair.pair(confEdge.getSource(),
					confEdge.getTarget()))) {
				addOrderConflict(new OrderConflict<SecurityNode>(ConflictEdge.fromSDGEdge(confEdge),
						attackerLevel, Maybe.just(trigger)));
			}

		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see edu.kit.joana.ifc.sdg.core.conc.LSODNISlicer.ConflictManager#
		 * addPossiblyUntriggeredDataConflict
		 * (edu.kit.joana.ifc.sdg.graph.SDGEdge,
		 * edu.kit.joana.ifc.sdg.graph.SDGNode, java.lang.String)
		 */
		@Override
		public void addPossiblyUntriggeredDataConflict(SDGEdge confEdge,
				SecurityNode influenced, String attackerLevel) {
			
			addDataConflict(new DataConflict<SecurityNode>(ConflictEdge.fromSDGEdge(confEdge), influenced, attackerLevel));
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see edu.kit.joana.ifc.sdg.core.conc.LSODNISlicer.ConflictManager#
		 * addTriggeredDataConflict(edu.kit.joana.ifc.sdg.graph.SDGEdge,
		 * edu.kit.joana.ifc.sdg.graph.SDGNode,
		 * edu.kit.joana.ifc.sdg.graph.SDGNode, java.lang.String)
		 */
		@Override
		public void addTriggeredDataConflict(SDGEdge confEdge, SecurityNode trigger,
				SecurityNode influenced, String attackerLevel) {
			addDataConflict(new DataConflict<SecurityNode>(ConflictEdge.fromSDGEdge(confEdge), influenced, attackerLevel, Maybe.just(trigger)));
		}
		
		private void addOrderConflict(OrderConflict<SecurityNode> oc) {
			conflicts.add(oc);
			orderConflicts.add(oc);
			ConflictEdge<SecurityNode> confEdge = oc.getConflictEdge();
			ocEdges.add(Pair.pair(confEdge.getSource(),
					confEdge.getTarget()));
			ocEdges.add(Pair.pair(confEdge.getTarget(),
					confEdge.getSource()));
		}
		
		private void addDataConflict(DataConflict<SecurityNode> dc) {
			conflicts.add(dc);
			dataConflicts.add(dc);
		}

		/* (non-Javadoc)
		 * @see edu.kit.joana.ifc.sdg.core.conc.LSODNISlicer.ConflictManager#getDataConflicts()
		 */
		@Override
		public Collection<DataConflict<SecurityNode>> getDataConflicts() {
			return dataConflicts;
		}

		/* (non-Javadoc)
		 * @see edu.kit.joana.ifc.sdg.core.conc.LSODNISlicer.ConflictManager#getOrderConflicts()
		 */
		@Override
		public Collection<OrderConflict<SecurityNode>> getOrderConflicts() {
			return orderConflicts;
		}
	}

	/* (non-Javadoc)
	 * @see edu.kit.joana.ifc.sdg.core.conc.ConflictScanner#getDataConflicts()
	 */
	@Override
	public Collection<DataConflict<SecurityNode>> getDataConflicts() {
		return conf.getDataConflicts();
	}

	/* (non-Javadoc)
	 * @see edu.kit.joana.ifc.sdg.core.conc.ConflictScanner#getOrderConflicts()
	 */
	@Override
	public Collection<OrderConflict<SecurityNode>> getOrderConflicts() {
		return conf.getOrderConflicts();
	}
}
