/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.ifc.sdg.core.conc;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import edu.kit.joana.ifc.sdg.core.SecurityNode;
import edu.kit.joana.ifc.sdg.core.interfaces.ProgressListener;
import edu.kit.joana.ifc.sdg.core.sdgtools.SDGTools;
import edu.kit.joana.ifc.sdg.core.violations.Conflict;
import edu.kit.joana.ifc.sdg.core.violations.OrderConflict;
import edu.kit.joana.ifc.sdg.core.violations.Violation;
import edu.kit.joana.ifc.sdg.graph.SDG;
import edu.kit.joana.ifc.sdg.graph.SDGEdge;
import edu.kit.joana.ifc.sdg.graph.SDGNode;
import edu.kit.joana.ifc.sdg.graph.slicer.Slicer;
import edu.kit.joana.ifc.sdg.graph.slicer.conc.CFGForward;
import edu.kit.joana.ifc.sdg.graph.slicer.conc.CFGSlicer;
import edu.kit.joana.ifc.sdg.graph.slicer.conc.nanda.Nanda;
import edu.kit.joana.ifc.sdg.graph.slicer.conc.nanda.NandaBackward;
import edu.kit.joana.ifc.sdg.graph.slicer.conc.nanda.NandaForward;
import edu.kit.joana.ifc.sdg.graph.slicer.graph.threads.MHPAnalysis;
import edu.kit.joana.ifc.sdg.graph.slicer.graph.threads.PreciseMHPAnalysis;
import edu.kit.joana.ifc.sdg.lattice.IStaticLattice;


/**
 * Sucht in SDGs nach probabilistischen Informationslecks.
 *
 * @author giffhorn
 *
 */
public class ProbabilisticNISlicer {
    /**
     * Interface, das die Verwendung zweier Strategien zulaesst:
     * Entweder werden nur Quellen und Senken eines Lecks bestimmt (SimpleConflicts),
     * oder es werden zusaetzlich auch die Races bestimmt, die zu dem LEck fuehren.
     *
     * @author giffhorn
     */
    private interface ConflictManager {
        /**
         * Liefert alle bisher gefundenen Konflikte.
         *
         * @return Alle bisher gefundenen Konflikte.
         */
        Collection<Conflict> getConflicts();

        /**
         * Aktualisiert die Konfliktliste.
         *
         * @param sink
         * @param source
         * @param edge
         */
        void updateConflicts(SecurityNode sink, SecurityNode source, SDGEdge edge, String attackerLevel);
    }

    /**
     * Eine einfache, aber schnelle Behandlung von Lecks.
     * Ein Leck (Conflict) besteht hier nur aus Quelle und Senke.
     *
     * @author giffhorn
     */
    private static class SimpleConflicts implements ConflictManager {
        // menge der bisherigen konflikte
        private HashSet<Conflict> conflicts;

        /**
         * Initialisierung.
         */
        public SimpleConflicts() {
            conflicts = new HashSet<Conflict>();
        }

        /**
         * Liefert alle bisher gefundenen Konflikte.
         *
         * @return Alle bisher gefundenen Konflikte.
         */
        public Collection<Conflict> getConflicts() {
            return conflicts;
        }

        /**
         * Aktualisiert die Konfliktliste.
         * Erhaelt die Quelle und die Senke eines neuen Lecks. Die Kante bestimmt die
         * Art des Lecks, entweder ein Data Conflict oder ein Order Conflict.
         *
         * @param sink    Senke des Lecks.
         * @param source  Quelle des Lecks.
         * @param edge    Vom Typ SDGEdge.Kind.CONFLICT_DATA oder SDGEdge.Kind.CONFLICT_ORDER.
         */
        public void updateConflicts(SecurityNode sink, SecurityNode source, SDGEdge edge, String attackerLevel) {
            if (edge.getKind() == SDGEdge.Kind.CONFLICT_DATA) {
                // erzeuge neuen Conflict
                Conflict con  = new Conflict(sink, source, attackerLevel);
                con.setConflictEdge(edge);
                conflicts.add(con);
            } else if (edge.getKind() == SDGEdge.Kind.CONFLICT_ORDER) {
                // erzeuge neuen OrderConflict
                Conflict con  = new OrderConflict(sink, source, (SecurityNode)edge.getSource(), attackerLevel);
                con.setConflictEdge(edge);
                conflicts.add(con);
            }
        }
    }

    /**
     * Eine detaillierte, aber sehr teure Behandlung von Lecks.
     * Ein Leck (Conflict) besteht aus Quelle und Senke, sowie allen Races, die dieses Leck formen.
     *
     * @author giffhorn
     */
    private static class DetailedConflicts implements ConflictManager {
        // menge der bisherigen konflikte
        // ein einfacher Conflict, der nur aus quelle und sekne des lecks besteht,
        // dient als schluessel fuer den detaillierten Conflict
        private HashMap<Conflict, Conflict> conflicts;

        /**
         * Initialisierung.
         */
        public DetailedConflicts() {
            conflicts = new HashMap<Conflict, Conflict>();
        }

        /**
         * Liefert alle bisher gefundenen Konflikte.
         *
         * @return Alle bisher gefundenen Konflikte.
         */
        public Collection<Conflict> getConflicts() {
            return conflicts.values();
        }

        /**
         * Aktualisiert die Konfliktliste.
         * Erhaelt die Quelle und die Senke eines Lecks. Die Kante bestimmt die
         * Art des Lecks, entweder ein Data Conflict oder ein Order Conflict.
         *
         * Bei einem Order Conflict liegt ein neues Leck vor, sodass ein neuer Conflict angelegt wird.
         * Bei einem Data Conflict wird, falls das Leck bereits erfasst wurde, dem Conflict die Kante
         * hinzugefuegt.
         *
         * @param sink    Senke des Lecks.
         * @param source  Quelle des Lecks.
         * @param edge    Vom Typ SDGEdge.Kind.CONFLICT_DATA oder SDGEdge.Kind.CONFLICT_ORDER.
         */
        public void updateConflicts(SecurityNode sink, SecurityNode source, SDGEdge edge, String attackerLevel) {
            if (edge.getKind() == SDGEdge.Kind.CONFLICT_DATA) {
                // hole den Conflict aus der map und fuege die kante hinzu
                Conflict con  = new Conflict(sink, source, attackerLevel);
                Conflict saved = conflicts.get(con);

                if (saved == null) {
                    conflicts.put(con, con);
                } else {
                    con = saved;
                }

                con.addConflict(edge);

            } else if (edge.getKind() == SDGEdge.Kind.CONFLICT_ORDER) {
                // erzeuge einen neuen OrderConflict
                Conflict con  = new OrderConflict(sink, source, (SecurityNode)edge.getSource(), attackerLevel);
                conflicts.put(con, con);
            }


        }
    }

    /**
     * Berechnet die Konfliktkanten im SDG.
     * Startpunkt ist Methode addConflictEdges()
     *
     * Momentan basiert die Identifikation von Konflikten auf der isParallel-Relation meiner MHP-Analyse.
     * Das funktioniert, weil momentan nur forks und joins analysiert werden. Es funktioniert nicht mehr,
     * wenn einmal Synchronisation hinzukommt, weil Synchronisation keine festen Ausfuehrungsreihenfolgen festlegt!
     * Ab dann benoetigt man hier einen Mechanismus, mit dem man feste Ausfuehrungsreihenfolgen analysieren kann.
     *
     * @author giffhorn
     */
    private class ConflictEdgeManager {
    	private final boolean timeSens;
    	private HashMap<SDGNode, Collection<SDGNode>> before = new HashMap<SDGNode, Collection<SDGNode>>();
    	private List<SDGEdge> dataConflictEdges = new LinkedList<SDGEdge>();
    	private List<SDGEdge> orderConflictEdges = new LinkedList<SDGEdge>();
    	
    	ConflictEdgeManager(boolean timeSens) {
    		this.timeSens = timeSens;
    	}
    	
    	/**
         * Berechnet die Konfliktkanten, die spaeter in den SDG eingefuegt werden muessen.
         */
        void computeConflictEdges() {
            // berechne die potentiellen trigger der conflicts
        	computeBeforeMap();
            computeOrderConflicts();
            computeDataConflicts();

            // DEBUG
//            for (SDGEdge e : mayhemMap.keySet()) {
//                System.out.println(e+": "+mayhemMap.get(e));
//            }
        }

        /**
         * Fuegt Kanten fuer order conflicts ein. Ausserdem wird die 'orderConflicts2Triggers'-Map befuellt.
         */
        private void computeOrderConflicts() {
            // sammel alle quellen und senken ein
            Collection<SecurityNode> annotatedNodes = new HashSet<SecurityNode>();
            annotatedNodes.addAll(sources);
            annotatedNodes.addAll(sinks);

            for (SecurityNode m : annotatedNodes) {
                for (SecurityNode n : annotatedNodes) {
                    // falls m und n parallel sind, potentieller order-konflikt m -> n
                    if (mhp.isParallel(m, n)) {
                    	// teste, ob konflikt harmlos ist
                    	HashSet<SecurityNode> triggers = trigger(m, n);
                    	HashSet<SecurityNode> refined = new HashSet<SecurityNode>();

                    	// ermittle mindest-angreifer
                    	String mLevel = (m.isInformationSink()? m.getRequired() : m.getProvided());
                    	String nLevel = (n.isInformationSink()? n.getRequired() : n.getProvided());
                    	String attacker = l.leastUpperBound(mLevel, nLevel);

                    	// jeder bzgl. des Konflikts geheime Trigger ist gefaehrlich (hier kann Information ueber die
                    	// Ausfuehrungsreihenfolge der am Konflikt beteiligten Knoten fliessen)
                    	for (SecurityNode t : triggers) {
                    		// teste, ob t geheime infos hat bzgl. des attackers
                    		if (!l.leastUpperBound(t.getProvided(), attacker).equals(attacker)) {
                    			refined.add(t); // geheime Information beeinflusst den Order-Conflict
                    		}
                    	}

                    	// wenn refined leer ist, ist der konflikt harmlos (es gibt keine geheimen trigger, also
                    	// verraet die Ausfuehrungsreihenfolge nichts, was nicht verraten werden soll)
                    	if (!refined.isEmpty()) {
    	                	SDGEdge edge = new SDGEdge(m, n, SDGEdge.Kind.CONFLICT_ORDER);
    	                	orderConflictEdges.add(edge);
    	                    orderConflicts2Triggers.put(edge, triggers);
//    	                    System.out.println("ORDER CONFLICT: "+m+" <-> "+n);
                    	}
                    }
                }
            }
        }

        /**
         * Fuegt Kanten fuer data conflicts ein. Ausserdem wird hier die {@link ProbabilisticNISlicer#triggersToDataConflicts}-Map befuellt.
         */
        private void computeDataConflicts() {
        	
            // suche nach interferenzen -> potentielle data-konflikte
            for (SDGEdge edge : g.edgeSet()) {
                if (edge.getKind() == SDGEdge.Kind.INTERFERENCE) {
                	// teste, ob er harmlos ist
                	HashSet<SecurityNode> triggers = trigger((SecurityNode)edge.getSource(), (SecurityNode)edge.getTarget());
                	if (!triggers.isEmpty()) {
                    	SDGEdge e = new SDGEdge(edge.getSource(), edge.getTarget(), SDGEdge.Kind.CONFLICT_DATA);
                        dataConflictEdges.add(e);


                        for (SecurityNode n : triggers) {
                        	HashSet<SDGEdge> dataConflicts = triggersToDataConflicts.get(n);
                        	if (dataConflicts == null) {
                        		dataConflicts = new HashSet<SDGEdge>();
                        		triggersToDataConflicts.put(n, dataConflicts);
                        	}
                        	dataConflicts.add(e);
                        }
                	}

                } else if (edge.getKind() == SDGEdge.Kind.INTERFERENCE_WRITE) {

                	// teste, ob er harmlos ist
                	HashSet<SecurityNode> triggers = trigger((SecurityNode)edge.getSource(), (SecurityNode)edge.getTarget());
                	if (!triggers.isEmpty()) {
                        // bidirected conflict
                    	SDGEdge e = new SDGEdge(edge.getSource(), edge.getTarget(), SDGEdge.Kind.CONFLICT_DATA);
                    	SDGEdge f = new SDGEdge(edge.getTarget(), edge.getSource(), SDGEdge.Kind.CONFLICT_DATA);
                    	dataConflictEdges.add(e);
                    	dataConflictEdges.add(f);

                        for (SecurityNode trigger : triggers) {
                        	HashSet<SDGEdge> dataConflicts = triggersToDataConflicts.get(trigger);
                        	if (dataConflicts == null) {
                        		dataConflicts = new HashSet<SDGEdge>();
                        		triggersToDataConflicts.put(trigger, dataConflicts);
                        	}
                        	dataConflicts.add(e);
                        	dataConflicts.add(f); // evtl. ueberfluessig
                        }
                	}
                }
            }
        }

        /**
         * Berechnet fuer einen gegebenen Konflikt die potentiellen Trigger. Ein Trigger ist ein Quellknoten,
         * welcher den Konflikt beeinflussen koennte.
         *
         * @param a  der eine Knoten des Konflikts
         * @param b  der andere Knoten des Konflikts
         */
        private HashSet<SecurityNode> trigger(SecurityNode a, SecurityNode b) {
        	HashSet<SecurityNode> triggers = new HashSet<SecurityNode>();

            // jede quelle wird untersucht; sie muss entweder parallel zu a bzw b sein oder im CFG-slice von b bzw a liegen
            for (SecurityNode n : sources) {
            	/**
            	 * n kann den Konflikt beeinflussen, wenn sowohl fuer x=a als auch fuer x=b entweder MHP(n,x) gilt, oder
            	 * x nach n im CFG ausgefuehrt werden kann (n beeinflusst also sowohl den einen als auch den anderen
            	 * Konfliktknoten).
            	 */
            	if ((before.get(n).contains(a) || mhp.isParallel(n, a))
            			&& (before.get(n).contains(b) || mhp.isParallel(n, b))) {
            		// kann den konflikt beeinflussen
            		triggers.add(n);
            	}
            }

            return triggers;
        }

        /**
         * Initialisiert die Map 'before'. before.get(n) enthaelt alle Knoten die im CFG nach n ausgefuehrt werden
         * und damit von n beeinflusst werden koennten (CFG forward slice)
         */
        private void computeBeforeMap() {
        	CFGSlicer slicer = new CFGForward(g);
        	
        	for (SDGNode n : sources) {
        		Collection<SDGNode> s = slicer.slice(n);
        		before.put(n, s);
        	}
        	
        	if (this.timeSens) {
        		Nanda tsfwSlicer = new Nanda(g, new NandaForward());
        		for (SDGNode n : sources) {
        			Collection<SDGNode> tsfwSliceOfN = tsfwSlicer.slice(n);
        			Collection<SDGNode> maybeInfluenced = before.get(n);
        			maybeInfluenced.retainAll(tsfwSliceOfN);
        			before.put(n, maybeInfluenced);
        		}
        	}
        }
        
        void addConflictEdges() {
        	for (SDGEdge e : orderConflictEdges) {
        		g.addEdge(e);
        	}
        	
        	for (SDGEdge e : dataConflictEdges) {
        		g.addEdge(e);
        	}
        }
        
        void removeConflictEdges() {
        	for (SDGEdge e : orderConflictEdges) {
        		g.removeEdge(e);
        	}
        	
        	for (SDGEdge e : dataConflictEdges) {
        		g.removeEdge(e);
        	}
        	
        }
        
        
    }


    private ArrayList<ProgressListener> pls = new ArrayList<ProgressListener>();

    // der lattice
    private IStaticLattice<String> l;
    // der SDG
    private SDG g;
    // eine MHP-analyse
    private MHPAnalysis mhp;

    /**
     *  bildet jede ORDER-CONFLICT-Kante auf diejenigen Quellknoten ab, die diese beeinflussen koennten.
     *  Solche Quellknoten sind mindestens so geheim wie die beiden am ORDER-CONFLICT beteiligten Knoten.
     *  Beachte: Jede ORDER-CONFLICT-Kante kommt als key in dieser map vor! Folglich kann man davon
     *  ausgehen, dass fuer jede ORDER-CONFLICT-Kante der zugehoerige Wert nicht null ist (es sei denn,
     *  es wurde explizit null als Wert eingetragen).
     *  Diese Map wird in {@link ConflictEdgeManager#computeOrderConflicts()} befuellt.
     **/
    private HashMap<SDGEdge, HashSet<SecurityNode>> orderConflicts2Triggers;

    /**
     *  bildet jeden Quellknoten, der mindestens eine DATA-CONFLICT-Kante beeinflussen koennte, auf die
     *  Menge aller der von ihm beeinflussten DATA-CONFLICT-Kanten ab
     *  Beachte: Nicht jeder Quellknoten kommt als Key in der Map vor, daher ist ein {@code null}- bzw.
     *  containsKey()-check unerlaesslich, bevor man auf Werte zugreift.
     *  Diese Map wird in {@link ConflictEdgeManager#computeDataConflicts()} befuellt.
     **/
    private HashMap<SecurityNode, HashSet<SDGEdge>> triggersToDataConflicts;
    // der zu verwendende ConflictManager
    private ConflictManager conf;
    
    private ConflictEdgeManager confEdgeMan;

    // quellen und senken werden dazwischengespeichert
    private Collection<SecurityNode> sources;
    private Collection<SecurityNode> sinks;
    
    private boolean timeSens;

    /**
     * Initialisiert die Analyse.
     * Fuehrt eine MHP-Analyse aus und fuegt Konfliktkanten in den SDG ein.
     *
     * @param g     Ein SDG.
     * @param l     Ein Sicherheitsverband.
     * @param conf  Ein ConflictManager.
     */
    public ProbabilisticNISlicer(SDG g, IStaticLattice<String> l, ConflictManager conf) {
       this(g, l, conf, PreciseMHPAnalysis.analyze(g), false);
    }
    
    public ProbabilisticNISlicer(SDG g, IStaticLattice<String> l, ConflictManager conf, MHPAnalysis mhp, boolean timeSens) {
    	 this.l = l;
         this.g = g;
         this.conf = conf;
         this.mhp = mhp;
         this.timeSens = timeSens;
         orderConflicts2Triggers = new HashMap<SDGEdge, HashSet<SecurityNode>>();
         triggersToDataConflicts = new HashMap<SecurityNode, HashSet<SDGEdge>>();
         sources = SDGTools.getInformationSources(g);
         sinks = SDGTools.getInformationSinks(g);
         this.confEdgeMan = new ConflictEdgeManager(this.timeSens);
         confEdgeMan.computeConflictEdges();
    }



    /** Fuehrt den Sicherheitscheck aus.
     *
     * @return Die Menge der gefundenen Sicherheitsverletzungen.
     */
    public Set<Violation> check() {
        // bestimme alle annotierten knoten
        LinkedList<Element> criteria = collectCriteria();
        Set<Violation> set = new HashSet<Violation>();
        confEdgeMan.addConflictEdges();
        // pruefe jeden annotierten knoten auf probabilistische noninterferenz
        for (Element e : criteria) {
        	// suche order channels
            long tmp = System.currentTimeMillis();
        	orderChannels(e);
        	tmp = System.currentTimeMillis() - tmp;
        	orderChannels += tmp;
        	// suche data channels
        	tmp = System.currentTimeMillis();
            dataChannels(e);
        	tmp = System.currentTimeMillis() - tmp;
        	dataChannels += tmp;
        }
        confEdgeMan.removeConflictEdges();
        set.addAll(conf.getConflicts());

        return set;
    }

    public long dataChannels;
    public long orderChannels;

    /**
     * Bestimmt die annotierten Knoten im SDG.
     *
     * @return Die annotierten Knoten.
     */
    private LinkedList<Element> collectCriteria() {
        LinkedList<Element> criteria = new LinkedList<Element>();

        // suche alle annotierten knoten (keine deklassifikationen)
        for (SDGNode o :  g.vertexSet())  {
            SecurityNode temp = (SecurityNode) o;

            if (temp.isInformationSink()) {
                // baue ein entsprechendes Element
                String label = temp.getRequired();
                Element e = new Element(temp, label);
                criteria.add(e);

            } else if (temp.isInformationSource()) {
                // baue ein entsprechendes Element
                String label = temp.getProvided();
                Element e = new Element(temp, label);
                criteria.add(e);
            }
        }

        return criteria;
    }


    /**
     * Untersucht das Programm nach Probabilistic Order Channels.
     *
     * @param element  Ein annotierter Knoten.
     */
    private void orderChannels(Element element) {
    	/**
    	 * Is this element influenced by an order conflict, which has been triggered by a secret source
    	 */
        for (SDGEdge orderConflictEdge : g.getIncomingEdgesOfKind(element.node, SDGEdge.Kind.CONFLICT_ORDER)) {

        	// determine minimal attacker
        	String elementLevel = (element.node.isInformationSource() ?
        							element.node.getProvided() : element.node.getRequired());
        	SecurityNode conflictingNode = (SecurityNode) orderConflictEdge.getSource();

        	String levelOfConflicting = (conflictingNode.isInformationSource() ? conflictingNode.getProvided() : conflictingNode.getRequired());

        	/**
        	 * NOTE: We know that both elementLevel and levelOfConflicting are not null, since order conflict edges exist
        	 * only between information endpoints where either getRequired() != null or getProvided() != null
        	 */

        	String attacker = l.leastUpperBound(levelOfConflicting, elementLevel);

            // collect the triggers
        	Set<SecurityNode> trigger = orderConflicts2Triggers.get(orderConflictEdge);

        	// since every order conflict edge occurs as key in the map, we know trigger != null

            for (SecurityNode t : trigger) {
                /**
                 * t is leaked through order conflict edge.
                 * Note that by construction, every trigger of the conflict edge is not
                 * more public than the two nodes participating in that conflict, so this has not
                 * to be checked here!
                 */
                conf.updateConflicts(element.node, t, orderConflictEdge, attacker);
            }
        }
    }

    /**
     * Untersucht das Programm nach Probabilistic Data Channels.
     *
     * @param element  Ein annotierter Knoten.
     */
    private void dataChannels(Element element) {

        DataConflictCollector confCollector = new DataConflictCollector();
        Slicer slicer;
        if (this.timeSens) {
        	slicer = new Nanda(g, new NandaBackward(), confCollector);
        } else {
        	slicer = new AdhocBackwardSlicer(g, confCollector);
        }
        slicer.slice(element.node);
    	List<SDGEdge> dataConflicts = confCollector.getDataConflicts();
    	
        /**
         * All data conflicts, which possibly could influence the given element have been computed.
         * Now, check if one of these data conflicts is influenced by a secret (relative to the security
         * level of the given element) source.
         */
        for (SecurityNode source : sources) {


        	if (l.leastUpperBound(source.getProvided(), element.label).equals(element.label)) {
        		/**
            	 * All sources which are 'not secret enough' can safely be ignored.
            	 */
        		continue;
        	}

        	Collection<SDGEdge> triggered = triggersToDataConflicts.get(source);

        	if (triggered == null) {
        		/**
        		 * All sources (even those which are 'secret enough'!) which do not trigger any conflicts
        		 * can safely be ignored
        		 */
        		continue;
        	}

        	HashSet<SDGEdge> dataConflictTriggeredBySecret = new HashSet<SDGEdge>();

        	dataConflictTriggeredBySecret.addAll(dataConflicts);
        	dataConflictTriggeredBySecret.retainAll(triggered);

        	for (SDGEdge dangerousDataConflict : dataConflictTriggeredBySecret) {
        		/**
        		 * the secret source leaks information through this conflict!
        		 */
                conf.updateConflicts(element.node, source, dangerousDataConflict, element.label);
            }
        }
    }

	 /* ProgressListener */
	
	public void addProgressListener(ProgressListener pl) {
        this.pls.add(pl);
    }

    public void removeProgressListener(ProgressListener pl) {
        this.pls.remove(pl);
    }
    
    public Collection<Conflict> getConflicts() {
    	return conf.getConflicts();
    }


    /* Factories */
    public static ProbabilisticNISlicer simpleCheck(SDG g, IStaticLattice<String> l){
       return simpleCheck(g, l, PreciseMHPAnalysis.analyze(g), false);
    }

    public static ProbabilisticNISlicer detailedCheck(SDG g, IStaticLattice<String> l){
    	 return detailedCheck(g, l, PreciseMHPAnalysis.analyze(g), false);
    }
    
    public static ProbabilisticNISlicer simpleCheck(SDG g, IStaticLattice<String> l, MHPAnalysis mhp, boolean timeSens) {
    	return new ProbabilisticNISlicer(g, l, new SimpleConflicts(), mhp, timeSens);
    }
    
    public static ProbabilisticNISlicer detailedCheck(SDG g, IStaticLattice<String> l, MHPAnalysis mhp, boolean timeSens) {
    	return new ProbabilisticNISlicer(g, l, new DetailedConflicts(), mhp, timeSens);
    }
}
