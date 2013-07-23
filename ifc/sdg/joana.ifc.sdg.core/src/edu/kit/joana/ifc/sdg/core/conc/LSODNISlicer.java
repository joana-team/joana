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
import edu.kit.joana.ifc.sdg.core.violations.ClassifiedViolation;
import edu.kit.joana.ifc.sdg.core.violations.IConflictLeak;
import edu.kit.joana.ifc.sdg.graph.SDG;
import edu.kit.joana.ifc.sdg.graph.SDGEdge;
import edu.kit.joana.ifc.sdg.graph.SDGNode;
import edu.kit.joana.ifc.sdg.graph.slicer.Slicer;
import edu.kit.joana.ifc.sdg.graph.slicer.conc.CFGForward;
import edu.kit.joana.ifc.sdg.graph.slicer.conc.nanda.Nanda;
import edu.kit.joana.ifc.sdg.graph.slicer.conc.nanda.NandaBackward;
import edu.kit.joana.ifc.sdg.graph.slicer.graph.threads.MHPAnalysis;
import edu.kit.joana.ifc.sdg.graph.slicer.graph.threads.PreciseMHPAnalysis;
import edu.kit.joana.ifc.sdg.lattice.IStaticLattice;
import edu.kit.joana.ifc.sdg.lattice.LatticeUtil;
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
	public Set<IConflictLeak> check() {
		LinkedList<Element> criteria = collectCriteria();
		this.sources.clear();
		this.sinks.clear();
		this.sources.addAll(SDGTools.getInformationSources(g));
		this.sinks.addAll(SDGTools.getInformationSinks(g));
		Set<IConflictLeak> set = new HashSet<IConflictLeak>();
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
		DataConflictCollector confCollector = new DataConflictCollector();
		Slicer slicer;
		if (this.timeSens) {
			slicer = new Nanda(g, new NandaBackward(), confCollector);
		} else {
			slicer = new AdhocBackwardSlicer(g, confCollector);
		}
		Collection<SDGNode> s = slicer.slice(e.node);

		for (SDGNode n : s) {
			collectPossibleDataChannels(e, (SecurityNode) n);
			collectPossibleOrderChannels(e, (SecurityNode) n);
		}
	}

	private void collectPossibleDataChannels(Element e, SecurityNode n) {
		// if n has an incoming data conflict edge, add a violation
		for (SDGEdge inc : g.getIncomingEdgesOfKind(n,
				SDGEdge.Kind.CONFLICT_DATA)) {
			// possible probabilistic data channel
			if (!useOptimization) {
				conf.addPossiblyUntriggeredDataConflict(inc, e.node,
						e.node.getLevel());
			} else {
				Collection<SecurityNode> secTriggers = collectSecretTriggers(inc,
						e.node.getLevel());
				for (SecurityNode secTrigger : secTriggers) {
					conf.addTriggeredDataConflict(inc, secTrigger, e.node,
							e.node.getLevel());
				}
			}
		}
	}

	private void collectPossibleOrderChannels(Element e, SecurityNode n) {
		// if n has an incoming or outgoing order conflict edge and the conflict
		// is
		// low-observable, add a violation
		List<SDGEdge> oConfs = g.getIncomingEdgesOfKind(n,
				SDGEdge.Kind.CONFLICT_ORDER);
		oConfs.addAll(g.getOutgoingEdgesOfKind(n, SDGEdge.Kind.CONFLICT_ORDER));
		for (SDGEdge oConf : oConfs) {
			// check whether order-conflict is low-observable (with respect to
			// the level of the current element)
			String refLevel = e.node.getLevel();
			if (isLowObservable(oConf, refLevel)) {
				// possible probabilistic order channel
				if (!useOptimization) {
					conf.addPossiblyUntriggeredOrderConflict(oConf, refLevel);
				} else {
					Collection<SecurityNode> secTriggers = collectSecretTriggers(
							oConf, refLevel);
					for (SecurityNode secTrigger : secTriggers) {
						conf.addTriggeredOrderConflict(oConf, secTrigger,
								refLevel);
					}
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
					&& !LatticeUtil.isLeq(l, secN.getLevel(), refLevel)) {
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
	 * Returns whether the given order conflict edge is low-observable with
	 * respect to the given reference level. This is the case, if both nodes
	 * participating in the conflict have a level which is lower than or equal
	 * to the given reference level.
	 * 
	 * @param e
	 *            conflict edge to be checked for low-observability
	 * @param refLevel
	 *            reference level providing the least upper bound for
	 *            observability
	 * @return {@code true} if both nodes participating in the conflict have a
	 *         level which is lower than or equal to the given reference level
	 *         (i.e. both execution orders are observable by an attacker which
	 *         has the given level), {@code false} otherwise
	 */
	private boolean isLowObservable(SDGEdge e, String refLevel) {
		SecurityNode s1 = (SecurityNode) e.getSource();
		SecurityNode s2 = (SecurityNode) e.getTarget();
		String levelS1 = s1.getLevel();
		String levelS2 = s2.getLevel();
		return levelS1 != null && levelS2 != null
				&& LatticeUtil.isLeq(l, levelS1, refLevel)
				&& LatticeUtil.isLeq(l, levelS2, refLevel);

	}

	/**
	 * Returns all the conflicts found in the last run of this algorithm.
	 */
	public Collection<AbstractConflictLeak> getAllConflicts() {
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
		Collection<AbstractConflictLeak> getConflicts();
		Collection<DataConflict> getDataConflicts();
		Collection<OrderConflict> getOrderConflicts();
		
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
		private LinkedList<AbstractConflictLeak> conflicts;
		private LinkedList<DataConflict> dataConflicts;
		private LinkedList<OrderConflict> orderConflicts;
		private Set<Pair<SDGNode, SDGNode>> ocEdges = new HashSet<Pair<SDGNode, SDGNode>>();

		/**
		 * Initialisierung.
		 */
		public SimpleConflicts() {
			conflicts = new LinkedList<AbstractConflictLeak>();
			dataConflicts = new LinkedList<DataConflict>();
			orderConflicts = new LinkedList<OrderConflict>();
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
		public Collection<AbstractConflictLeak> getConflicts() {
			return new LinkedList<AbstractConflictLeak>(conflicts);
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
				OrderConflict oc = new OrderConflict(confEdge,
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
				addOrderConflict(new OrderConflict(confEdge,
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
			
			addDataConflict(new DataConflict(confEdge, influenced, attackerLevel));
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
			addDataConflict(new DataConflict(confEdge, influenced, attackerLevel, Maybe.just(trigger)));
		}
		
		private void addOrderConflict(OrderConflict oc) {
			conflicts.add(oc);
			orderConflicts.add(oc);
			SDGEdge confEdge = oc.getConflictEdge();
			ocEdges.add(Pair.pair(confEdge.getSource(),
					confEdge.getTarget()));
			ocEdges.add(Pair.pair(confEdge.getTarget(),
					confEdge.getSource()));
		}
		
		private void addDataConflict(DataConflict dc) {
			conflicts.add(dc);
			dataConflicts.add(dc);
		}

		/* (non-Javadoc)
		 * @see edu.kit.joana.ifc.sdg.core.conc.LSODNISlicer.ConflictManager#getDataConflicts()
		 */
		@Override
		public Collection<DataConflict> getDataConflicts() {
			return dataConflicts;
		}

		/* (non-Javadoc)
		 * @see edu.kit.joana.ifc.sdg.core.conc.LSODNISlicer.ConflictManager#getOrderConflicts()
		 */
		@Override
		public Collection<OrderConflict> getOrderConflicts() {
			return orderConflicts;
		}
	}

	/* (non-Javadoc)
	 * @see edu.kit.joana.ifc.sdg.core.conc.ConflictScanner#getDataConflicts()
	 */
	@Override
	public Collection<DataConflict> getDataConflicts() {
		return conf.getDataConflicts();
	}

	/* (non-Javadoc)
	 * @see edu.kit.joana.ifc.sdg.core.conc.ConflictScanner#getOrderConflicts()
	 */
	@Override
	public Collection<OrderConflict> getOrderConflicts() {
		return conf.getOrderConflicts();
	}
}
