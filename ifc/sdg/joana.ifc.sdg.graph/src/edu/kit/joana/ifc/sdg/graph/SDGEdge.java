/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
/*
 * Created on Feb 5, 2004
 *
 * To change the template for this generated file go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
package edu.kit.joana.ifc.sdg.graph;

import java.util.Comparator;
import java.util.EnumSet;
import java.util.Set;

/**
 * An SDGEdge is an edge in our graphs (despite the name not necessarily an edge in an SDG).
 * @author hammer, giffhorn
 */
public class SDGEdge implements Cloneable {
	private static final long serialVersionUID = -7849877704948368788L;

    /**
     * This is probably the most important property if you intend to analyze SDGs.
     * It classifies the kinds of edges. Besides the standard kinds like control flow,
     * data- and control dependence and so on we provide a sh!tload of different kinds of edges.
     *
     * A kind has a name and a flag signaling if it represents a program dependence.
     */
    public enum Kind {
        /** Is used for layouting SDGs in GUIs.*/
        HELP("HE", false),
        /** Used to capture structural interdependencies between parameter nodes a->b <=> b may be reached through a field of a */
        PARAMETER_STRUCTURE("PS", false),

        /* different kinds of control flow edges */
        /** Control flow edge (intra-procedural only). */
        CONTROL_FLOW("CF", false),
        /** No-flow edge, a sort of unrealizable control flow edge. */
        NO_FLOW("NF", false),
        /** Jump flow edge, used for gotos and the like. */
        JUMP_FLOW("JF", false), // I've never encountered one of those in 6 years
        /** Return edge. */
        RETURN("RF", false),

        /* different kinds of control dependences */
        /** Unconditional control dependence edge */
        CONTROL_DEP_UNCOND("UN", true),
        /** Conditional control dependence edge, used for control dependences caused by conditional structures. */
        CONTROL_DEP_COND("CD", true),
        /** Intra-expression control dependence edges. Used for parameter trees / graphs. */
        CONTROL_DEP_EXPR("CE", true),
        /** Control dependences induced by procedure calls. */
        CONTROL_DEP_CALL("CC", true),
        /** Control dependences induced by jumps. */
        JUMP_DEP("JD", true),
        /** Weak control dependence as defined by Ranganath et al. */
        NTSCD("NTSCD", false),
        /** Synchronization dependence, a kind of control dependence between nodes in a synchronized block and the head of the block. */
        SYNCHRONIZATION("SD", true),

        /* different kinds of data dependences */
        /** `Standard' data dependence. */
        DATA_DEP("DD", true),
        /** data dependence through values on the heap. */
        DATA_HEAP("DH", true),
        /** data dependence through values on the heap. */
        DATA_ALIAS("DA", true),
        /** Loop-carried data dependence. Currently not distinguished from standard data dependence. */
        DATA_LOOP("DL", true),
        /** Data dependence between tokens of the same expression.
         * Used for fine-grained SDGs for the purpose of creating path conditions. */
        DATA_DEP_EXPR_VALUE("VD", true),
        /** Data dependence between tokens that reference the same object.
         * Used for fine-grained SDGs for the purpose of creating path conditions. */
        DATA_DEP_EXPR_REFERENCE("RD", true),

        /* interprocedural edges */
        /** Summary edge - full summary includes control deps, data deps, heap data deps and heap data deps with aliasing. */
        SUMMARY("SU", true),
        /** Summary edge - includes  control deps, data deps and heap data deps, but no aliasing related data deps. */
        SUMMARY_NO_ALIAS("SH", true),
        /** Another summary edge - includes only data deps and heap data deps only without aliasing. No control deps and no aliasing */
        SUMMARY_DATA("SF", true),
        /** Call edge. */
        CALL("CL", true),
        /** Parameter-in edge. */
        PARAMETER_IN("PI", true),
        /** Parameter-out edge. */
        PARAMETER_OUT("PO", true),

        /* dependences caused by threads */
        /** Interference dependence. */
        INTERFERENCE("ID", true),
        /** Interference-write dependence, happens between two conflicting writes to the same shared variable.
          Not a program dependence in the classic sense. */
        INTERFERENCE_WRITE("IW", false),
        /** Ready dependence, caused by operations that can block other threads (e.g. between wait and notify).
         Currently not used, because we do not need to be termination-sensitive. */
        READY_DEP("RY", true),

        /** Fork edge. */
        FORK("FORK", true),
        /** Parameter-in edge for fork sites. */
        FORK_IN("FORK_IN", true),
        /** Parameter-Out edge for fork sites. */
        FORK_OUT("FORK_OUT", true),
        /** Join edge */
        JOIN("JOIN", false),
        /** Parameter-Out edge for join sites. */
        JOIN_OUT("JOIN_OUT", true),

        /* CONFLICTS: Nondeterministic execution order between two nodes. Used for IFC. */
        /** A conflict between a read and a write of the same shared variable. */
        CONFLICT_DATA("CONFLICT_DATA", false),
        /** A conflict between two output events. */
        CONFLICT_ORDER("CONFLICT_ORDER", false),

        /* edges used for graph folding */
        /** A folded edge subsumes a set of edges having the same source and target. */
        FOLDED("FD", true),
        /** An edge connecting a folded node with its fold node. */
        FOLD_INCLUDE("FI", false);


        private final String value;
        private final boolean isSDG; // signals kinds that represent a program dependence.

        Kind(String s, boolean sdg) { value = s; isSDG = sdg; }

        /**
         * @return Returns the name of this kind.
         */
        public String toString() {
            return value;
        }

        /**
         * @return `true' if this kind denotes a program dependence.
         */
        public boolean isSDGEdge() {
            return isSDG;
        }

        /**
         * @return `true' if this kind denotes an edge between different threads.
         */
        public boolean isThreadEdge() {
            return threadEdges().contains(this);
        }

        /**
         * @return `true' if this kind is an intra-procedural program dependence.
         */
        public boolean isIntraSDGEdge() {
            return this == CONTROL_DEP_UNCOND
                    || this == CONTROL_DEP_COND
                    || this == CONTROL_DEP_EXPR
                    || this == CONTROL_DEP_CALL
                    || this == JUMP_DEP
                    || this == DATA_DEP
                    || this == DATA_HEAP
                    || this == DATA_ALIAS
                    || this == DATA_LOOP
                    || this == DATA_DEP_EXPR_VALUE
                    || this == DATA_DEP_EXPR_REFERENCE
                    || this == SUMMARY
                    || this == SUMMARY_DATA
                    || this == SUMMARY_NO_ALIAS
                    || this == SYNCHRONIZATION;
        }

        /**
         * @return `true' if this kind is an intra-procedural edge.
         */
        public boolean isIntraproceduralEdge() {
            return this != RETURN
                && this != CALL
                && this != PARAMETER_IN
                && this != PARAMETER_OUT
                && this != INTERFERENCE
                && this != INTERFERENCE_WRITE
                && this != FORK
                && this != FORK_IN
                && this != FORK_OUT
                && this != JOIN
                && this != CONFLICT_DATA
                && this != CONFLICT_ORDER;
        }

        /**
         * @return `true' if this kind is a control flow edge.
         */
        public boolean isControlFlowEdge() {
        	return controlFlowEdges().contains(this);
        }

        public static Set<SDGEdge.Kind> controlFlowEdges() {
            return controlFlowEdges;
        }

        public static Set<SDGEdge.Kind> dataflowEdges() {
            return dataflowEdges;
        }

        public static Set<SDGEdge.Kind> threadEdges() {
            return threadEdges;
        }

        private static Set<SDGEdge.Kind> controlFlowEdges =
        		EnumSet.of(SDGEdge.Kind.CONTROL_FLOW, SDGEdge.Kind.NO_FLOW,
                SDGEdge.Kind.JUMP_FLOW, SDGEdge.Kind.RETURN, SDGEdge.Kind.CALL,
                SDGEdge.Kind.FORK, SDGEdge.Kind.JOIN);

        private static Set<SDGEdge.Kind> dataflowEdges =
        		EnumSet.of(SDGEdge.Kind.CONTROL_DEP_UNCOND,
                SDGEdge.Kind.CONTROL_DEP_COND, SDGEdge.Kind.CONTROL_DEP_EXPR,
                SDGEdge.Kind.CONTROL_DEP_CALL, SDGEdge.Kind.JUMP_DEP,
                SDGEdge.Kind.DATA_DEP, SDGEdge.Kind.DATA_HEAP, SDGEdge.Kind.DATA_ALIAS, SDGEdge.Kind.DATA_LOOP,
                SDGEdge.Kind.DATA_DEP_EXPR_VALUE, SDGEdge.Kind.DATA_DEP_EXPR_REFERENCE,
                SDGEdge.Kind.SUMMARY, SDGEdge.Kind.SUMMARY_DATA, SDGEdge.Kind.SUMMARY_NO_ALIAS, SDGEdge.Kind.CALL,
                SDGEdge.Kind.PARAMETER_IN, SDGEdge.Kind.PARAMETER_OUT,
                SDGEdge.Kind.INTERFERENCE, SDGEdge.Kind.FORK, SDGEdge.Kind.FORK_IN,
                SDGEdge.Kind.FORK_OUT, SDGEdge.Kind.READY_DEP, SDGEdge.Kind.SYNCHRONIZATION);

        private static Set<SDGEdge.Kind> threadEdges =
            	EnumSet.of(SDGEdge.Kind.INTERFERENCE, SDGEdge.Kind.FORK, SDGEdge.Kind.FORK_IN,
                SDGEdge.Kind.FORK_OUT, SDGEdge.Kind.INTERFERENCE_WRITE,
                SDGEdge.Kind.READY_DEP, SDGEdge.Kind.JOIN);
    }


    /** A comparator for SDGEdges.
     * Two edges are identified as equal if they have the same source, target, and kind.
     */
    static class SDGEdgeComparator implements Comparator<SDGEdge> {
        public int compare(SDGEdge arg0, SDGEdge arg1) {
            if (arg0.getKind().hashCode() > arg1.getKind().hashCode()) {
                return 1;

            } else if (arg0.getKind().hashCode() < arg1.getKind().hashCode()) {
                return -1;
            }

            if (arg0.getSource().getId() > arg1.getSource().getId()) {
                return 1;

            } else if (arg0.getSource().getId() < arg1.getSource().getId()) {
                return -1;
            }

            if (arg0.getTarget().getId() > arg1.getTarget().getId()) {
                return 1;

            } else if (arg0.getTarget().getId() < arg1.getTarget().getId()) {
                return -1;
            }

            return 0;
        }
    }

    /* The SDGEdgeComparator singleton. */
    private static final SDGEdgeComparator COMP = new SDGEdgeComparator();

    /**
     * @return Returns an SDGEdgeComparator.
     */
    public static SDGEdgeComparator getComparator() {
        return COMP;
    }


    /* *** Start of the SDGEdge guts *** */

	/* An edge has a source and a target */
    protected SDGNode m_source;
    protected SDGNode m_target;

    Kind kind;  // the kind of the edge
    private String label;  // the label of the edge

    /**
     * Constructor for DefaultEdge.
     *
     * @param sourceVertex source vertex of the edge.
     * @param targetVertex target vertex of the edge.
     */
    private SDGEdge(SDGNode sourceVertex, SDGNode targetVertex) {
    	if (sourceVertex == null || targetVertex == null) {
    		throw new IllegalArgumentException("Source or target is null. Source: " + sourceVertex
    				+ " - Target: " + targetVertex);
    	}
        m_source = sourceVertex;
        m_target = targetVertex;
    }

    /**
     *
     * @return Returns the source of the edge.
     */
    public SDGNode getSource() {
        return m_source;
    }

    /**
     * Sets the source of the edge to the given node.
     * @param source  A node, should not be null.
     */
    public void setSource(SDGNode source) {
    	m_source = source;
    }

    /**
     *
     * @return Returns the target of the edge.
     */
    public SDGNode getTarget() {
        return m_target;
    }

    /**
     * Sets the target of the edge to the given node.
     * @param target  A node, should not be null.
     */
    public void setTarget(SDGNode target) {
    	m_target = target;
    }

    /**
     * Returns a shallow copy of the edge.
     */
    public Object clone() {
        try {
            return super.clone();
        }
         catch(CloneNotSupportedException e) {
            // shouldn't happen as we are cloneable
            throw new InternalError();
        }
    }

    /**
     * Returns true if either the source or the target of the edge equal the given node.
     * @param v  A node.
     */
    public boolean containsVertex(SDGNode v) {
        return m_source.equals(v) || m_target.equals(v);
    }

    /**
     * Returns the node at the other end of the edge.
     * @param v  The source or the target node of the edge. Otherwise an IllegalArgumentException is thrown.
     */
    public SDGNode getOppositeVertex(SDGNode v) {
        if (v.equals(m_source)) {
            return m_target;

        } else if(v.equals(m_target)) {
            return m_source;

        } else {
            throw new IllegalArgumentException("no such vertex");
        }
    }

    /**
     * Creates an SDGEdge of kind `kind' from source to sink.
     */
    public SDGEdge(SDGNode source, SDGNode sink, Kind kind) {
        this(source, sink);
        this.kind = kind;
    }

    /**
     * Creates an SDGEdge of kind `kind' from source to sink and labels it with `label'.
     */
    public SDGEdge(SDGNode source, SDGNode sink, Kind kind, String label) {
        this(source, sink, kind);
        this.label = label;
    }

    /**
     * @return Returns the kind of the edge.
     */
    public SDGEdge.Kind getKind() {
        return kind;
    }

    /**
     * @return `true' if the edge is labelled.
     */
    public boolean hasLabel() {
        return label != null;
    }

    /**
     * @return Returns the label of the edge. Can be null.
     */
    public String getLabel() {
        return label;
    }

    /**
     * @return A textual representation of the edge.
     */
    public String toString() {
//        return label;
        return new String(getSource() + " -" + getKind() + "-" +
        		(label == null ? "" : label) + "> " + getTarget());
        //return ""+getKind();
    }

    /**
     * Two edges are equal, if
     * - they point to the same objekt, or
     * - they have the same source, target, kind and label.
     */
    public boolean equals(Object o) {
    	if (this == o) return true;

        if (!(o instanceof SDGEdge)) {
            return false;
        }
        SDGEdge edge = (SDGEdge) o;

        if (kind != edge.kind) return false;
        boolean labeled = label != null;
        if (labeled == (edge.label == null)) return false;
        if (labeled && !label.equals(edge.label)) return false;
        if (!getSource().equals(edge.getSource())) return false;
        return getTarget().equals(edge.getTarget());
    }

    /**
     * Returns a hash code consistent with Java's equals/hashCode directive.
     */
    public int hashCode() {
    	int hc = kind.hashCode();
    	hc = 37 * hc + getSource().hashCode();
        return 37 * hc + getTarget().hashCode();
    }
}
