/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
/*
 * Created on Dec 8, 2003
 *
 */
package edu.kit.joana.ifc.sdg.graph;

import gnu.trove.set.TIntSet;
import gnu.trove.set.hash.TIntHashSet;

import java.util.Comparator;

/**
 * <p>Title: SDGNode</p>
 * <p>Description: This class models a node in PDG/SDG graphs</p>
 * <p>Copyright: Copyright (c) 2002 Christian Hammer</p>
 * <p>Organization: University of Passau, Software Systems Chair</p>
 * @author Christian Hammer <hammer@fmi.uni-passau.de>
 */
public class SDGNode implements Cloneable {

	/** used for optimizations in (parallel) summary computation */
	public int tmp;
	public BitVector bv;

	/** SDGNodes are classified by their kinds.
	 * If you intend to analyze SDGs, this is probably the most important property of SDGNodes.
	 */
    public static enum Kind {
        /**
         * Normale Knoten.  Solche Knoten werden f\u00FCr Anweisungen und
         * Deklarationen erzeugt.  Im Grunde sind Knoten der Art
         * #Norm#, wenn es keine andere Art f\u00FCr sie gibt.
         */
        NORMAL("NORM"),
        /**
         * Knoten, die aus Expressions entstanden sind. Knoten dieser
         * Art liefern einen Wert.  Bei solchen Knoten entscheidet die
         * Kombination aus Operator und Wert \u00FCber den Teilausdruck.
         */
        EXPRESSION("EXPR"),
        /**
         * Knoten, die f\u00FCr Pr\u00E4dikate stehen.  Solche Knoten sind
         * Spezialisierungen der Art #Expr#, ihr Wert entscheidet \u00FCber
         * den weiteren Kontrollflu\u00DF des Programms.  Diese Knoten sind
         * daher Startknoten f\u00FCr Kontrollabh\u00E4ngigkeitskanten.
         * @see SDG::CD
         */
        PREDICATE("PRED"),
        /**
         * Funktions-Aufruf-Knoten.  An Knoten dieser Art werden
         * Funktionen aufgerufen.  Liefert der Funktionsaufruf einen
         * Wert, so liegt er an diesem Knoten vor.  Die Struktur eines
         * Funktionsaufrufs ist sehr streng: die erste UN-Kante f\u00FChrt zu
         * dem Ausdruck (bzw. der Funktionskonstante) der die
         * aufgerufene Funktion bestimmt.  Die anderen UN-Kanten f\u00FChren
         * zu den In- und den Out-Parametern.  Der Operator dieses
         * Knotens ist immer
         * #call#.
         */
        CALL("CALL"),
        /**
         * Aktuelle Parameter vor Aufruf.  An diesen Knoten sind die
         * Eingabe-Parameter von Aufrufstellen vorhanden.  Der Operator
         * entscheidet, ob ein Wert oder eine Variable als Parameter
         * \u00FCbergeben wird.  Ist er #act_in#, so ist es eine Variable.
         * Alle anderen Operatoren stehen f\u00FCr zu berechnende Werte (wie
         * bei #Expr#-Knoten).
         */
        ACTUAL_IN("ACTI"),
        /**
         * Aktuelle Parameter nach Aufruf.  Hier werden die Variablen
         * als Parameter wieder zur\u00FCckgegeben.  Derzeit mu\u00DF der Operator
         * #act_out# sein, da keine Werte zur\u00FCckgegeben werden k\u00F6nnen.
         * Dies kann sich aber \u00E4ndern: dann w\u00FCrde der durch
         * #return#-Anweisungen zur\u00FCckgelieferte Wert einen anderen
         * Operator bekommen.
         */
        ACTUAL_OUT("ACTO"),
        /**
         * Start-Knoten von Funktionen.  Dies ist der Knoten, an dem die
         * einzelnen Prozeduren aufgebaut sind.  Der Operator ist daher
         * auch #entry#.  Der Wert des Knotens ist der Funktionsname.
         */
        ENTRY("ENTR"),
        /**
         * End-Knoten von Funktionen.  Dies ist der Knoten, an dem die
         * einzelnen Prozeduren enden.  Der Operator ist daher
         * auch #exit#.  Der Wert des Knotens ist der Funktionsname.
         */
        EXIT("EXIT"),
        /**
         * Formale Parameter vor Aufruf.  Diese Art Knoten h\u00E4ngen direkt
         * an #Entry#-Knoten und stehen f\u00FCr die an die Funktion
         * \u00FCbergebene Parameter.  Der Operator ist immer
         * #form_in#, bis auf Ellipsen, die durch #form_ellip#
         * gekennzeichnet werden.
         */
        FORMAL_IN("FRMI"),
        /**
         * Formale Parameter nach Aufruf.  Diese Art Knoten
         * h\u00E4ngen auch direkt an den #Entry#-Knoten und stehen f\u00FCr die
         * R\u00FCckgabeparameter, die von der Funktion an die Aufrufstelle
         * zur\u00FCckgegeben werden.
         */
        FORMAL_OUT("FRMO"),
        /**
         * Knoten mit "spawn"-Aufruf.
         */
        //SPAWN(""),
        /**
         * Knoten f\u00FCr "sync"-Anweisung.
         */
        SYNCHRONIZATION("SYNC"),
        /**
         * Gefalteter Knoten.
         */
        FOLDED("FOLD"),
        /**
         * Abstrakter Join-Punkt
         */
        JOIN("JOIN"),

        /**
         * Summary-Knoten (wird verwendet fuer IFC)
         */
        SUMMARY("Summary");

        private final String value;

        Kind(String s) { value = s; }

        public String toString() {
            return value;
        }

        /*static abstract SDGNode getInstance() {}*/
    }

    public enum Operation {
    	EMPTY("empty", Kind.EXPRESSION, Kind.FOLDED), //
        INT_CONST("intconst", Kind.EXPRESSION, Kind.PREDICATE, Kind.ACTUAL_IN),
        FLOAT_CONST("floatconst", Kind.EXPRESSION),
        CHAR_CONST("charconst", Kind.EXPRESSION),
        STRING_CONST("stringconst", Kind.EXPRESSION),
        FUNCTION_CONST("functionconst", Kind.EXPRESSION),
		SHORTCUT("shortcut", Kind.EXPRESSION, Kind.PREDICATE, Kind.ACTUAL_IN), // && ||
		QUESTION("question", Kind.EXPRESSION), // ? :
        BINARY("binary", Kind.EXPRESSION, Kind.PREDICATE),
        UNARY("unary", Kind.EXPRESSION),
//			//	     | "lookup"
		DEREFER("derefer", Kind.EXPRESSION, Kind.PREDICATE, Kind.ACTUAL_IN),
		REFER("refer", Kind.EXPRESSION, Kind.PREDICATE, Kind.ACTUAL_IN),
        ARRAY("array", Kind.EXPRESSION),
        SELECT("select", Kind.EXPRESSION),
        REFERENCE("reference", Kind.EXPRESSION, Kind.PREDICATE, Kind.ACTUAL_IN),
		DECLARATION("declaration", Kind.NORMAL),
		MODIFY("modify", Kind.EXPRESSION, Kind.PREDICATE), // ++ --
		MODASSIGN("modassign", Kind.EXPRESSION), // += etc.
        ASSIGN("assign", Kind.EXPRESSION),
        IF("IF", Kind.PREDICATE, Kind.NORMAL),
		LOOP("loop", Kind.NORMAL),
        JUMP("jump", Kind.NORMAL),
        COMPOUND("compound", Kind.NORMAL),
        CALL("call", Kind.CALL),
        ENTRY("entry", Kind.ENTRY),
        EXIT("exit", Kind.EXIT),
        FORMAL_IN("form-in", Kind.FORMAL_IN),
		FORMAL_ELLIP("form-ellip", Kind.FORMAL_IN), // Ellipse-Parameter "..."
        FORMAL_OUT("form-out", Kind.FORMAL_OUT),
        ACTUAL_IN("act-in", Kind.ACTUAL_IN),
        ACTUAL_OUT("act-out", Kind.ACTUAL_OUT),
//		NONE("None"
        MONITOR("monitor", Kind.SYNCHRONIZATION),
        SUMMARY("summary", Kind.SUMMARY);

        private final String value;
        private final Kind[] kind;

        Operation(String s, Kind k) { value = s; kind = new Kind[]{k}; }

        Operation(String s, Kind k1, Kind k2) {
            value = s;
            kind = new Kind[]{k1, k2};
        }

        Operation(String s, Kind k1, Kind k2, Kind k3) {
            value = s;
            kind = new Kind[]{k1, k2, k3};
        }

        public Kind getKind(int i) {
        	return kind[i];
        }

        public String toString() {
            return value;
        }

        Kind[] getCorrespondingKind() {
            return kind;
        }

        public SDGNode createNode(int id, String value, int proc, String type,
                String source, int sr, int sc, int er, int ec, String bcName, int bcIndex, String[] localDefNames, String[] localUseNames) {
            return new SDGNode(id, this, value, proc, type, source, sr, sc, er, ec, bcName, bcIndex);
        }
    }

    public static abstract class NodeFactory {
    	public abstract SDGNode createNode(Operation op, int kind, int id, String value, int proc, String type,
              String source, int sr, int sc, int er, int ec, String bcMethod, int bcIndex);
    }

	public static final class SDGNodeFactory extends NodeFactory {
		public SDGNode createNode(Operation op, int kind, int id, String value, int proc,
				String type, String source, int sr, int sc, int er, int ec, String bcMethod, int bcIndex) {
			return new SDGNode(op.getKind(kind), id, op, value, proc, type,
					source, sr, sc, er, ec, bcMethod, bcIndex);
		}
	}

	/** A comparator for SDGNodes.
	 * Two nodes are identified identical if they have the same ID.
	 * Is used for sorting node sets in ascending order of the IDs.
	 */
    private static class IDComparator implements Comparator<SDGNode> {
        public int compare(SDGNode n1, SDGNode n2) {
            return n1.getId() -n2.getId();
        }
    }

    /* An IDComparator singleton. */
    private static final IDComparator idcomp = new IDComparator();

    /**
     * @return Returns an IDComparator.
     */
    public static Comparator<SDGNode> getIDComparator() {
        return idcomp;
    }



    /* *** The node internals start here. *** */

    /* The ID of the node. Should be unique in the graph. Negative IDs are permitted.*/
    private int id;

    /* The source file of the source code represented by this node.*/
    private String source;

    /* The ID of the procedure to which the node belongs.*/
    private int proc;

    /* Classifies nodes by means of their bytecode operation. */
    public Operation operation;

    /* The bytecode of this node.*/
    private String value;

    /* The type of this node.*/
    private String type;

    /* Name of the bytecode method or parameter this node belongs to */
    private final String bcName;
    /* index of the bytecode instruction or one of the special ids < 0 (BytecodeLoaction.*) this node belongs to */
    private final int bcIndex;

    /*
     * A node stores the dimensions of the source code area over which it ranges.
     * `sr' is the index of the first line.
     * `sc' is the index of the first column.
     * `er' is the index of the last line.
     * `ec' is the index of the last column.
     */
    private int sr, sc, er, ec;

    /* The kind of this node.*/
    public Kind kind;

    /* The IDs of the threads to which the node belongs.
       Is never null if the node stems from a generated SDG.
       Should never be null for nodes that are added later, because the slicing algorithms rely on that.*/
    private static final int[] EMPTY = new int[0];
    private int[] threadNumbers = EMPTY;

    /* Used when interference computation is toggled. For nodes that call Thread.start we store
       the nodes that are potential allocation sites (declaration nodes) upon which start() is
       called. So we can compute which run() method is called. */
    private int[] allocationSites;

    private String clsLoader;

    /* used for call nodes where there is no pdg for the call target */
    private String unresolvedCallTarget;
    
    /* for nodes defining a value that correspond to a definition of local variables, the names of the corresponding
     * local variables; 
     */
    private String[] localDefNames;

    /* for nodes using a value that correspond to a definition of local variables, the names of the corresponding
     * local variables; 
     */
    private String[] localUseNames;

    public SDGNode(int id, Operation op, String value, int proc,
            String type, String source, int sr, int sc, int er, int ec, String bcName, int bcIndex) {
        this.kind = op.kind[0];
        this.id = id;
        this.operation = op;
        this.value = value;
        this.source = source;
        this.sr = sr;
        this.sc = sc;
        this.er = er;
        this.ec = ec;
        this.proc = proc;
        this.type = type;
        this.bcName = bcName;
        this.bcIndex = bcIndex;
    }

    protected SDGNode(Kind kind, int id, Operation op, String value, int proc,
            String type, String source, int sr, int sc, int er, int ec, String bcMethod, int bcIndex) {
        this(id, op, value, proc, type, source, sr, sc, er, ec, bcMethod, bcIndex);
        assert op.kind[0] == kind;
        this.kind = kind;
    }

    /**
     * A constructor for synthetic nodes that are added to an existing SDG.
     * Receives only a kind, an ID and a procedure ID and sets the other attributes to default values.
     * @param kind  The node's kind.
     * @param id    The node's ID.
     * @param proc  The procedure ID.
     */
    public SDGNode(Kind kind, int id, int proc){
        this.kind = kind;
        this.operation = Operation.EMPTY;
        this.id = id;
        this.proc = proc;
        this.sr = -1;
        this.sc = -1;
        this.er = -1;
        this.ec = -1;
        this.bcIndex = -1;
        this.bcName = null;
    }

    /**
     * Returns a _shallow_ copy of the node.
     * Be careful if you intend to modify the operation attribute!
     * (noone is doing that for the time being, so a shallow copy is perfectly OK)
     */
    public SDGNode clone() {
    	SDGNode ret = new SDGNode(kind, id, operation, value, proc, type, source, sr, sc, er, ec, bcName, bcIndex);
    	if (allocationSites != null) {
    		ret.setAllocationSites(allocationSites.clone());
    	}

    	if (localDefNames != null) {
    		ret.setLocalDefNames(localDefNames.clone());
    	}

    	if (localUseNames != null) {
    		ret.setLocalDefNames(localUseNames.clone());
    	}
    	
    	return ret;
    }

    
    /**
     * Sets the names of local variables defined at this nodes.
     * @param localDefNames The names of local variables defined at this nodes.
     */
    public void setLocalDefNames(String[] localDefNames) {
       this.localDefNames = localDefNames;
    }
    
    /**
     * @return The names of local variables defined at this nodes.
     */
    public String[] getLocalDefNames() {
       return this.localDefNames;
    }
    
    /**
     * Sets the names of local variables used at this nodes.
     * @param localUseNames The names of local variables used at this nodes.
     */
    public void setLocalUseNames(String[] localUseNames) {
       this.localUseNames = localUseNames;
    }
    
    /**
     * @return The names of local variables used at this nodes.
     */
    public String[] getLocalUseNames() {
       return this.localUseNames;
    }
    
    /**
     * Sets the thread numbers of the node to the given array.
     * @param tn  The new thread numbers.
     */
    public void setAllocationSites(int[] sites) {
       this.allocationSites = sites;
    }

    /**
     * If available, this method returns the ids of the nodes, at which the object, on
     * which the respective method is called, is possibly allocated. <br>
     * Otherwise, {@code null} is returned.
     *
     * Specifically, these allocation Sites for calls of Thread.start() or Thread.join() or a method overriding Thread.run()
     * if the SDG was built with {@code computeInterference} enabled,
     * or for all call nodes if built with {@code computeAllocationSites} enabled. 
     *
     * @return possible allocation sites, if this node represents a call node,
     * and if the allocationSites were computed during SDG creation, 
     * or {@code null} otherwise
     */
    public int[] getAllocationSites() {
        return this.allocationSites;
    }

    /**
     * Sets the thread numbers of the node to the given array.
     * @param tn  The new thread numbers.
     */
    public void setThreadNumbers(int[] tn) {
       threadNumbers = tn;
    }

    /**
     * Returns the thread numbers of this node.
     * It should never return null. If it does, the SDG is broken!
     */
    public int[] getThreadNumbers(){
        return this.threadNumbers;
    }

    /**
     * Returns true if this node is a formal-in- or -out node, an actual-in- or -out node, or an exit node.
     */
    public boolean isParameter() {
        return getKind() == Kind.FORMAL_IN || getKind() == Kind.FORMAL_OUT
                || getKind() == Kind.ACTUAL_IN || getKind() == Kind.ACTUAL_OUT
                || getKind() == Kind.EXIT;
    }

/*
 * BEGIN alias data source stuff
 */

    /* stores for actual-ins the ids of formal-in nodes that may be data-sources for the value of
     * the current actual-in node. This is used to compute the aliasing of the actual-ins when the
     * aliasing of the formal-ins is known. */
    // They are stored as node attribute 'D' in the sdg file. E.g. "D 34, 564, 476, 1;"
    private TIntSet aliasDataSource;

    public final TIntSet getAliasDataSources() {
    	return aliasDataSource;
    }

    public final void setAliasDataSources(final TIntSet aliasDataSource) {
    	this.aliasDataSource = aliasDataSource;
    }

    public final void addAliasDataSource(final int sourceId) {
    	if (this.aliasDataSource == null) {
    		this.aliasDataSource = new TIntHashSet();
    	}

    	this.aliasDataSource.add(sourceId);
    }

/*
 * END alias data source stuff
 */

/*
 * BEGIN termination sensitive interprocedural control-dependencies
 */

    /* For the computation of interprocedural non-termination sensitive SDGs, we have
     * to mark certain instructions (mostly calls) as potentially non-terminating.
     * So we know, when this.nonTerm == true, all instructions after this may never
     * be executed -> termination sensitive control dependencies. */
    private boolean nonTerm = false;

    public boolean mayBeNonTerminating() {
    	return nonTerm;
    }

    public void setMayBeNonTerminating(final boolean nonTerm) {
    	this.nonTerm = nonTerm;
    }

/*
 * END termination sensitive interprocedural control-dependencies
 */

    public void setClassLoader(final String clsLoader) {
    	this.clsLoader = clsLoader;
    }

    public String getClassLoader() {
    	return this.clsLoader;
    }

    /**
     * Returns the label.
     */
    public String getLabel() {
        return value;
    }

    /**
     * Sets the label to the given String.
     * @param arg0  A String, should not be null.
     */
    public void setLabel(String arg0) {
        value = arg0;
    }

    public void setLine(int start, int end) {
    	sr = start;
    	er = end;
    }

    /**
     * Returns the index of the last line of the source code area over which the node ranges.
     * @return  The index or -1, if this is a synthetic node.
     */
    public int getEr() {
        return er;
    }

    /**
     * Returns the index of the last column of the source code area over which the node ranges.
     * @return  The index or -1, if this is a synthetic node.
     */
    public int getEc() {
        return ec;
    }

    /**
     * Returns the index of the first column of the source code area over which the node ranges.
     * @return  The index or -1, if this is a synthetic node.
     */
    public int getSc() {
        return sc;
    }

    /**
     * Returns the index of the first line of the source code area over which the node ranges.
     * @return  The index or -1, if this is a synthetic node.
     */
    public int getSr() {
        return sr;
    }

    public final int getBytecodeIndex() {
    	return bcIndex;
    }

    public final String getBytecodeName() {
    	return bcName;
    }

    public final String getBytecodeMethod() {
    	return bcName;
    }

    /**
     * A setter for the node ID.
     * A node ID should be unique in the graph. Negative IDs are possible and are currently
     * used for folded nodes.
     *
     * @param i  The ID, should be unique in the graph.
     */
    public void setId(int i) {
        id = i;
    }

    /**
     * @return The ID of the node.
     */
    public int getId() {
        return id;
    }

    /**
     * @return The kind of the node.
     */
    public Kind getKind() {
        return kind;
    }

    /**
     * @return The operation of the node.
     */
    public Operation getOperation() {
        return operation;
    }

    /**
     * @return The procedure ID of the node.
     */
    public int getProc() {
        return proc;
    }

    /**
     * A setter for the procedure ID of the node.
     * If a node does not belong to a procedure, e.g. in case it is
     * a folded node that spans over several procedures, use -1.
     */
    public void setProc(int p) {
        proc = p;
    }

    /**
     * @return The source file of this node.
     */
    public String getSource() {
        return source;
    }

    /**
     * Sets the source file of this node.
     */
    public void setSource(String s) {
    	source = s;
    }

    /**
     * @return The type of this node.
     */
    public String getType() {
        return type;
    }

    public String getUnresolvedCallTarget() {
        return unresolvedCallTarget;
    }

    public void setUnresolvedCallTarget(String unresolvedCallTarget) {
        this.unresolvedCallTarget = unresolvedCallTarget;
    }

    /**
     * @return A representation of this node. Currently just the ID.
     */
    public String toString() {
        return String.valueOf(id);
    }

    /**
     * There are two cases in which two nodes can be equal.
     * 1. They point to the same object. This holds if the nodes stem from the same graph. This case is checked first.
     * 2. They have the same attributes. This can be the case if the nodes stem from different graphs.
     */
    public boolean equals(Object o) {
    	if (o == this) {
    		return true;
    	}

        if (o instanceof SDGNode) {
            SDGNode node  = (SDGNode) o;

            if (this.id != node.getId()) return false;
            if (!this.operation.equals(node.getOperation())) return false;
            if (this.value != null && !this.value.equals(node.getLabel())) return false;
            if (this.value == null && node.getLabel() != null) return false;
            if (this.sr != node.getSr()) return false;
            if (this.sc != node.getSc()) return false;
            if (this.er != node.getEr()) return false;
            if (this.ec != node.getEc()) return false;
            if (this.proc != node.getProc()) return false;
            if (this.type != null && !this.type.equals(node.getType())) return false;
            if (this.type == null && node.getType() != null) return false;
            if (this.source != null && !this.source.equals(node.getSource())) return false;
            if (this.source == null && node.getSource() != null) return false;

            return true;
        }

        return false;
    }

    /**
     * The node ID is a good hash code.
     */
    public int hashCode() {
        return id;
    }

    /**
     * Returns true if the node is in the given thread.
     * @param t  The ID of the desired thread.
     */
    public boolean isInThread(int t) {
        if (threadNumbers == null) return false;

        for (int i : threadNumbers) {
            if (i == t) return true;
        }

        return false;
    }
}
