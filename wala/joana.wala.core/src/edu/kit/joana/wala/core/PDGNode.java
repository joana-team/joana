/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.wala.core;

import edu.kit.joana.ifc.sdg.util.BytecodeLocation;
import edu.kit.joana.wala.util.PrettyWalaNames;
import gnu.trove.set.TIntSet;
import gnu.trove.set.hash.TIntHashSet;

import com.ibm.wala.types.TypeReference;
import com.ibm.wala.util.graph.INodeWithNumber;


public final class PDGNode implements INodeWithNumber {

	public final static TypeReference DEFAULT_TYPE = TypeReference.JavaLangObject;
	public final static TypeReference DEFAULT_NO_TYPE = TypeReference.Null;
	
	public final static String[] DEFAULT_NO_LOCAL = null;
	public final static String[] DEFAULT_EMPTY_LOCAL = new String[0];

	public static enum Kind {
		/**
		 * Ein phi knoten.
		 */
		PHI("PHI"),
		/**
		 * Objekt erzeugung.
		 */
		NEW("NEW"),
		/**
		 * Ein lesender zugriff auf den heap.
		 */
		HREAD("HREAD"),
		/**
		 * Ein schreibender zugriff auf den heap.
		 */
		HWRITE("HWRITE"),
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
        JOIN("JOIN");

        private final String value;

        Kind(String s) { value = s; }

        public String toString() {
            return value;
        }
	}

    /* The ID of the node. Should be unique in the graph. Negative IDs are permitted.*/
    private final Integer id;

	/* The ID of the procedure to which the node belongs.*/
    private final int proc;

    /* The bytecode of this node.*/
    private String label;

    private String debug = null;

    private ParameterField field = null;

    /* The bytecode type name of this node.*/
    private final TypeReference type;

    /* The kind of this node.*/
    private final Kind kind;

    /* The source file of the source code represented by this node.*/
    private SourceLocation source;

    /* Name of the bytecode method or parameter this node belongs to */
    private String bcName;

    /* index of the bytecode instruction  (or some magic code -> BytecodeLocation.*) this node belongs to */
    private int bcIndex;

    /* signature of call target, if it is a unresolved, e.g. a native method and this node is a call node */
    private String unresolvedCallTarget;
    
    /* for nodes defining a value that correspond to a definition of local variables, the names of the corresponding
     * local variables; 
     */
    private String[] localDefNames;

    /* for nodes using a value that correspond to a definition of local variables, the names of the corresponding
     * local variables; 
     */
    private String[] localUseNames;

    
    public PDGNode clone(int newId, int newPdgId) {
    	PDGNode clone = new PDGNode(newId, newPdgId, label, kind, type, localDefNames, localUseNames);
    	clone.setBytecodeIndex(bcIndex);
    	clone.setBytecodeName(bcName);
    	clone.setSourceLocation(source);
    	clone.setDebug(debug);

    	return clone;
    }

    // can be used to clone param-in nodes to matching param-out nodes.
    public PDGNode clone(int newId, Kind newKind) {
    	PDGNode clone = new PDGNode(newId, proc, label, newKind, type, localDefNames, localUseNames);
    	clone.setBytecodeIndex(bcIndex);
    	clone.setBytecodeName(bcName);
    	clone.setSourceLocation(source);
    	clone.setDebug(debug);

    	return clone;
    }

    public int getId() {
		return id;
	}

	public int getPdgId() {
		return proc;
	}

	public void setLabel(String str) {
		this.label = str;
	}

	public String getLabel() {
		return label;
	}

	public Kind getKind() {
		return kind;
	}

    public String getBytecodeName() {
		return bcName;
	}

	public void setBytecodeName(String bcName) {
		this.bcName = bcName;
	}

	public int getBytecodeIndex() {
		return bcIndex;
	}

	public boolean isBasePtr() {
		return bcIndex == BytecodeLocation.BASE_FIELD;
	}

	public boolean isFieldPtr() {
		return bcIndex == BytecodeLocation.OBJECT_FIELD || bcIndex == BytecodeLocation.ARRAY_FIELD;
	}

	public boolean isObjectField() {
		return bcIndex == BytecodeLocation.OBJECT_FIELD;
	}

	public boolean isArrayField() {
		return bcIndex == BytecodeLocation.ARRAY_FIELD;
	}

	public boolean isArrayIndex() {
		return bcIndex == BytecodeLocation.ARRAY_INDEX;
	}

	public boolean isRootParam() {
		return bcIndex == BytecodeLocation.ROOT_PARAMETER;
	}

	public boolean isStaticField() {
		return bcIndex == BytecodeLocation.STATIC_FIELD;
	}

	public void setBytecodeIndex(int bcIndex) {
		this.bcIndex = bcIndex;
	}

	public void setSourceLocation(SourceLocation sloc) {
		this.source = sloc;
	}

	public SourceLocation getSourceLocation() {
		return source;
	}
	
	// TODO: make copy to prevent mutation?
	public String[] getLocalDefNames() {
		return localDefNames;
	}
	
	
	// TODO: make copy to prevent mutation?
	public String[] getLocalUseNames() {
		return localUseNames;
	}

	void setLocalDefNames(String[] localDefNames) {
		this.localDefNames = localDefNames;
	}

	
	public PDGNode(int id, int pdgId, String label, Kind kind, TypeReference type, String[] localDefNames, String[] localUseNames) {
    	this.id = id;
    	this.nodeID = id;
    	this.proc = pdgId;
    	this.label = label;
    	this.kind = kind;
    	this.type = type;
    	this.localDefNames = localDefNames;
    	this.localUseNames = localUseNames;
    }

	public void setDebug(String str) {
		this.debug = str;
	}

	public String getDebug() {
		return debug;
	}

    /**
     * @return A representation of this node. Currently just the ID.
     */
    public String toString() {
        return String.valueOf(id) + "|" + kind.value + "|" + label + (debug == null ? "" : " " + debug);
    }

    /**
     * There are two cases in which two nodes can be equal.
     * 1. They point to the same object. This holds if the nodes stem from the same graph. This case is checked first.
     * 2. They have the same id.
     */
    public boolean equals(Object obj) {
    	if (obj == this) {
    		return true;
    	}

    	if (obj instanceof PDGNode) {
    		PDGNode other = (PDGNode) obj;
    		return id.intValue() == other.id.intValue();
    	}

    	return false;
    }

    public int hashCode() {
    	return System.identityHashCode(id);
    }

    /**
     * Bytecodename of the type corresponding to this node.
     * @return Bytecodename of the type corresponding to this node.
     */
	public String getType() {
		return (type == DEFAULT_NO_TYPE ? null : PrettyWalaNames.bcTypeName(type));
	}

	public TypeReference getTypeRef() {
		return type;
	}

	public void setParameterField(final ParameterField field) {
		this.field = field;
	}

	public ParameterField getParameterField() {
		return field;
	}

	private int nodeID;

	@Override
	public int getGraphNodeId() {
		return nodeID;
	}

	@Override
	public void setGraphNodeId(int number) {
		this.nodeID = number;
	}

    /* stores for actual-ins the ids of formal-in nodes that may be data-sources for the value of
     * the current actual-in node. This is used to compute the aliasing of the actual-ins when the
     * aliasing of the formal-ins is known.
     *
     * For formal-nodes this attribute stores the informations with wicht other formal-nodes the
     * current node may be aliasing.
     */
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

	/**
	 * Sets the call target of this node (if it is a call node) - in particular for native call target
	 * where there is no PDG to jump to
	 * @param unresolvedCallTarget signature of call target
	 */
	public void setUnresolvedCallTarget(String unresolvedCallTarget) {
		this.unresolvedCallTarget = unresolvedCallTarget;
	}

	/**
	 * Returns the signature of the call target if this node is a call node and the call target is a native method.
	 * @return the signature of the call target if this node is a call node and the call target is a native method -
	 * may return {@code null} otherwise
	 */
	public String getUnresolvedCallTarget() {
		return unresolvedCallTarget;
	}
}
