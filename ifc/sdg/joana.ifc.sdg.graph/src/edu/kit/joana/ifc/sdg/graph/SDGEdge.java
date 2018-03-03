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
import java.util.HashSet;
import java.util.Set;
import java.util.function.BiFunction;

import edu.kit.joana.util.graph.KnowsVertices;

/**
 * An SDGEdge is an edge in our graphs (despite the name not necessarily an edge in an SDG).
 * @author hammer, giffhorn
 */
public abstract class SDGEdge implements Cloneable, KnowsVertices<SDGNode> {

    /**
     * This is probably the most important property if you intend to analyze SDGs.
     * It classifies the kinds of edges. Besides the standard kinds like control flow,
     * data- and control dependence and so on we provide a sh!tload of different kinds of edges.
     *
     * A kind has a name and a flag signaling if it represents a program dependence.
     */
    public enum Kind {
        /** Is used for layouting SDGs in GUIs.*/
        HELP("HE", false, (source, target) -> new SDGEdgeHELP(source, target), 63),
        /** Used to capture structural interdependencies between parameter nodes a->b <=> b may be reached through a field of a */
        PARAMETER_STRUCTURE("PS", false, (source, target) -> new SDGEdgePARAMETER_STRUCTURE(source, target), 61),
        /** Used to capture structural equivalance between parameter nodes a->b <=> a represents the same paramater as b.
         *  This is the case whenever a single parameter is both read and modified in given method.
         **/
        PARAMETER_EQUIVALENCE("PE", false, (source, target) -> new SDGEdgePARAMETER_EQUIVALENCE(source, target), 62),

        
        
        /* different kinds of control flow edges */
        /** Control flow edge (intra-procedural only). */
        CONTROL_FLOW("CF", false, (source, target) -> new SDGEdgeCONTROL_FLOW(source, target), 29),
        /** No-flow edge, a sort of unrealizable control flow edge. */
        NO_FLOW("NF", false, (source, target) -> new SDGEdgeNO_FLOW(source, target), 28),
        /** Jump flow edge, used for gotos and the like. */
        JUMP_FLOW("JF", false, (source, target) -> new SDGEdgeJUMP_FLOW(source, target), 27), // I've never encountered one of those in 6 years
        /** Return edge. */
        RETURN("RF", false, (source, target) -> new SDGEdgeRETURN(source, target), 26),

        /* different kinds of control dependences */
        /** Unconditional control dependence edge */
        CONTROL_DEP_UNCOND("UN", true, (source, target) -> new SDGEdgeCONTROL_DEP_UNCOND(source, target), 42),
        /** Conditional control dependence edge, used for control dependences caused by conditional structures. */
        CONTROL_DEP_COND("CD", true, (source, target) -> new SDGEdgeCONTROL_DEP_COND(source, target), 41),
        /** Intra-expression control dependence edges. Used for parameter trees / graphs. */
        CONTROL_DEP_EXPR("CE", true, (source, target) -> new SDGEdgeCONTROL_DEP_EXPR(source, target), 0),
        /** Control dependences induced by procedure calls. */
        CONTROL_DEP_CALL("CC", true, (source, target) -> new SDGEdgeCONTROL_DEP_CALL(source, target), 40),
        /** Control dependences induced by jumps. */
        JUMP_DEP("JD", true, (source, target) -> new SDGEdgeJUMP_DEP(source, target), 39),
        /** Weak control dependence as defined by Ranganath et al. */
        NTSCD("NTSCD", false, (source, target) -> new SDGEdgeNTSCD(source, target), 38),
        /** Synchronization dependence, a kind of control dependence between nodes in a synchronized block and the head of the block. */
        SYNCHRONIZATION("SD", true, (source, target) -> new SDGEdgeSYNCHRONIZATION(source, target), 37),

        /* different kinds of data dependences */
        /** `Standard' data dependence. */
        DATA_DEP("DD", true, (source, target) -> new SDGEdgeDATA_DEP(source, target), 36),
        /** data dependence through values on the heap. */
        DATA_HEAP("DH", true, (source, target) -> new SDGEdgeDATA_HEAP(source, target), 35),
        /** data dependence through values on the heap. */
        DATA_ALIAS("DA", true, (source, target) -> new SDGEdgeDATA_ALIAS(source, target), 34),
        /** Loop-carried data dependence. Currently not distinguished from standard data dependence. */
        DATA_LOOP("DL", true, (source, target) -> new SDGEdgeDATA_LOOP(source, target), 33),
        /** Data dependence between tokens of the same expression.
         * Used for fine-grained SDGs for the purpose of creating path conditions. */
        DATA_DEP_EXPR_VALUE("VD", true, (source, target) -> new SDGEdgeDATA_DEP_EXPR_VALUE(source, target), 32),
        /** Data dependence between tokens that reference the same object.
         * Used for fine-grained SDGs for the purpose of creating path conditions. */
        DATA_DEP_EXPR_REFERENCE("RD", true, (source, target) -> new SDGEdgeDATA_DEP_EXPR_REFERENCE(source, target), 31),

        /* interprocedural edges */
        /** Summary edge - full summary includes control deps, data deps, heap data deps and heap data deps with aliasing. */
        SUMMARY("SU", true, (source, target) -> new SDGEdgeSUMMARY(source, target), 58),
        /** Summary edge - includes  control deps, data deps and heap data deps, but no aliasing related data deps. */
        SUMMARY_NO_ALIAS("SH", true, (source, target) -> new SDGEdgeSUMMARY_NO_ALIAS(source, target), 57),
        /** Another summary edge - includes only data deps and heap data deps only without aliasing. No control deps and no aliasing */
        SUMMARY_DATA("SF", true, (source, target) -> new SDGEdgeSUMMARY_DATA(source, target), 56),
        /** Call edge. */
        CALL("CL", true, (source, target) -> new SDGEdgeCALL(source, target), 55),
        /** Parameter-in edge. */
        PARAMETER_IN("PI", true, (source, target) -> new SDGEdgePARAMETER_IN(source, target), 54),
        /** Parameter-out edge. */
        PARAMETER_OUT("PO", true, (source, target) -> new SDGEdgePARAMETER_OUT(source, target), 53),

        /* dependences caused by threads */
        /** Interference dependence. */
        INTERFERENCE("ID", true, (source, target) -> new SDGEdgeINTERFERENCE(source, target), 25),
        /** Interference-write dependence, happens between two conflicting writes to the same shared variable.
          Not a program dependence in the classic sense. */
        INTERFERENCE_WRITE("IW", false, (source, target) -> new SDGEdgeINTERFERENCE_WRITE(source, target), 24),
        /** Ready dependence, caused by operations that can block other threads (e.g. between wait and notify).
         Currently not used, because we do not need to be termination-sensitive. */
        READY_DEP("RY", true, (source, target) -> new SDGEdgeREADY_DEP(source, target), 23),

        /** Fork edge. */
        FORK("FORK", true, (source, target) -> new SDGEdgeFORK(source, target), 22),
        /** Parameter-in edge for fork sites. */
        FORK_IN("FORK_IN", true, (source, target) -> new SDGEdgeFORK_IN(source, target), 21),
        /** Parameter-Out edge for fork sites. */
        FORK_OUT("FORK_OUT", true, (source, target) -> new SDGEdgeFORK_OUT(source, target), 20),
        /** Join edge */
        JOIN("JOIN", false, (source, target) -> new SDGEdgeJOIN(source, target), 19),
        /** Parameter-Out edge for join sites. */
        JOIN_OUT("JOIN_OUT", true, (source, target) -> new SDGEdgeJOIN_OUT(source, target), 18),

        /* CONFLICTS: Nondeterministic execution order between two nodes. Used for IFC. */
        /** A conflict between a read and a write of the same shared variable. */
        CONFLICT_DATA("CONFLICT_DATA", false, (source, target) -> new SDGEdgeCONFLICT_DATA(source, target), 46),
        /** A conflict between two output events. */
        CONFLICT_ORDER("CONFLICT_ORDER", false, (source, target) -> new SDGEdgeCONFLICT_ORDER(source, target), 45),

        /* edges used for graph folding */
        /** A folded edge subsumes a set of edges having the same source and target. */
        FOLDED("FD", true, (source, target) -> new SDGEdgeFOLDED(source, target), 15),
        /** An edge connecting a folded node with its fold node. */
        FOLD_INCLUDE("FI", false, (source, target) -> new SDGEdgeFOLD_INCLUDE(source, target), 14);


        static final int PRIORITY_BITS = 6;
        static final int  PRIORITY_HASH_PART_MASK = (Integer.MIN_VALUE) | ((2 << Kind.PRIORITY_BITS) - 1) << (31 - Kind.PRIORITY_BITS);
        static final int REMAINING_HASH_PART_MASK = ~PRIORITY_HASH_PART_MASK;

    	
        static {
        	final Set<Integer> priorities = new HashSet<>();
        	for (Kind kind : Kind.values()) {
        		final boolean added = priorities.add(kind.getPriority());
        		if (!added) {
        			kind = null;
        		}
        		assert added;
        	}
        	assert priorities.size() == Kind.values().length;
        	
        }
        private final String value;
        private final boolean isSDG; // signals kinds that represent a program dependence.
        private final int priority; // priority for edges of this kind.
                                    // This can be used to enumerate, e.g., CONTROL_DEP_EXPR before SUMMARY edges
                                    // in a give set of edges. smaller priority numbers come first.
        private final int priorityPart;
        
        private final BiFunction<SDGNode, SDGNode, SDGEdge> newEdge;

        Kind(String s, boolean sdg, BiFunction<SDGNode, SDGNode, SDGEdge> newEdge, int priority) {
        	this.value = s;
        	this.isSDG = sdg;
        	this.newEdge = newEdge;
        	if (!( 0 <= priority && priority < (1 << PRIORITY_BITS))) {
        		throw new IllegalArgumentException();
        	}
        	this.priority = priority;
        	this.priorityPart = Integer.MIN_VALUE | (priority << (31 - Kind.PRIORITY_BITS));
        }
        
        /**
         * @return a new Edge of this Kind.
         */
        public SDGEdge newEdge(SDGNode source, SDGNode target) {
        	return newEdge.apply(source, target);
        }

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
        * @return the priority
        */
        public int getPriority() {
        	return priority;
        }
        
        /**
         * @return the priorityPart
         */
        int getPriorityPart() {
        	return priorityPart;
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
            return intraSDGEdges().contains(this);
        }

        /**
         * @return `true' if this kind is an intra-procedural program dependence but no summary edge.
         */
        public boolean isIntraPDGEdge() {
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

        public static Set<SDGEdge.Kind> intraSDGEdges() {
            return intraSDGEdges;
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

        private static Set<SDGEdge.Kind> intraSDGEdges =
        		EnumSet.of(SDGEdge.Kind.CONTROL_DEP_UNCOND,
                SDGEdge.Kind.CONTROL_DEP_COND, SDGEdge.Kind.CONTROL_DEP_EXPR,
                SDGEdge.Kind.CONTROL_DEP_CALL, SDGEdge.Kind.JUMP_DEP,
                SDGEdge.Kind.DATA_DEP, SDGEdge.Kind.DATA_HEAP, SDGEdge.Kind.DATA_ALIAS, SDGEdge.Kind.DATA_LOOP,
                SDGEdge.Kind.DATA_DEP_EXPR_VALUE, SDGEdge.Kind.DATA_DEP_EXPR_REFERENCE,
                SDGEdge.Kind.SUMMARY, SDGEdge.Kind.SUMMARY_DATA, SDGEdge.Kind.SUMMARY_NO_ALIAS,
                SDGEdge.Kind.SYNCHRONIZATION);

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

    /**
     *
     * @param sourceVertex source vertex of the edge.
     * @param targetVertex target vertex of the edge.
     */
    protected SDGEdge(SDGNode sourceVertex, SDGNode targetVertex) {
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
            throw new InternalError(e);
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
     * @return Returns the kind of the edge.
     */
    public abstract SDGEdge.Kind getKind();

    /**
     * @return `true' if the edge is labelled.
     */
    public boolean hasLabel() {
        return false;
    }

    /**
     * @return Returns the label of the edge. Can be null.
     */
    public String getLabel() {
        return null;
    }

    /**
     * @return A textual representation of the edge.
     */
    public String toString() {
        return new String(getSource() + " -" + getKind() + "-" + "> " + getTarget());
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

        if (getKind() != edge.getKind()) return false;
        if (edge.getLabel() != null) return false;
        if (!getSource().equals(edge.getSource())) return false;
        return getTarget().equals(edge.getTarget());
    }

    protected static final int ROT_BITS = 16 - Kind.PRIORITY_BITS;
    
    /**
     * Returns a hash code consistent with Java's equals/hashCode directive.
     */
    public int hashCode() {
    	final int priorityPart = getKind().getPriorityPart();

    	assert (priorityPart & Kind.PRIORITY_HASH_PART_MASK) == priorityPart;
    	assert (priorityPart & Kind.REMAINING_HASH_PART_MASK) == 0;
    	
    	int hc = getSource().hashCode() ^ (Integer.rotateLeft(getTarget().hashCode(), ROT_BITS));
        return (hc & Kind.REMAINING_HASH_PART_MASK) | priorityPart;
    }
}

class SDGEdgeHELP extends SDGEdge {
	public SDGEdgeHELP(SDGNode source, SDGNode target) {
		super(source, target);
	}
	
	@Override
	public final Kind getKind() {
		return Kind.HELP;
	}
}


class SDGEdgePARAMETER_STRUCTURE extends SDGEdge {
	public SDGEdgePARAMETER_STRUCTURE(SDGNode source, SDGNode target) {
		super(source, target);
	}
	
	@Override
	public final Kind getKind() {
		return Kind.PARAMETER_STRUCTURE;
	}
}
class SDGEdgePARAMETER_EQUIVALENCE extends SDGEdge {
	public SDGEdgePARAMETER_EQUIVALENCE(SDGNode source, SDGNode target) {
		super(source, target);
	}
	
	@Override
	public final Kind getKind() {
		return Kind.PARAMETER_EQUIVALENCE;
	}
}
class SDGEdgeCONTROL_FLOW extends SDGEdge {
	public SDGEdgeCONTROL_FLOW(SDGNode source, SDGNode target) {
		super(source, target);
	}
	
	@Override
	public final Kind getKind() {
		return Kind.CONTROL_FLOW;
	}
}
class SDGEdgeNO_FLOW extends SDGEdge {
	public SDGEdgeNO_FLOW(SDGNode source, SDGNode target) {
		super(source, target);
	}
	
	@Override
	public final Kind getKind() {
		return Kind.NO_FLOW;
	}
}
class SDGEdgeJUMP_FLOW extends SDGEdge {
	public SDGEdgeJUMP_FLOW(SDGNode source, SDGNode target) {
		super(source, target);
	}
	
	@Override
	public final Kind getKind() {
		return Kind.JUMP_FLOW;
	}
}
class SDGEdgeRETURN extends SDGEdge {
	public SDGEdgeRETURN(SDGNode source, SDGNode target) {
		super(source, target);
	}
	
	@Override
	public final Kind getKind() {
		return Kind.RETURN;
	}
}
class SDGEdgeCONTROL_DEP_UNCOND extends SDGEdge {
	public SDGEdgeCONTROL_DEP_UNCOND(SDGNode source, SDGNode target) {
		super(source, target);
	}
	
	@Override
	public final Kind getKind() {
		return Kind.CONTROL_DEP_UNCOND;
	}
}
class SDGEdgeCONTROL_DEP_COND extends SDGEdge {
	public SDGEdgeCONTROL_DEP_COND(SDGNode source, SDGNode target) {
		super(source, target);
	}
	
	@Override
	public final Kind getKind() {
		return Kind.CONTROL_DEP_COND;
	}
}
class SDGEdgeCONTROL_DEP_EXPR extends SDGEdge {
	public SDGEdgeCONTROL_DEP_EXPR(SDGNode source, SDGNode target) {
		super(source, target);
	}
	
	@Override
	public final Kind getKind() {
		return Kind.CONTROL_DEP_EXPR;
	}
	
	private final static int priorityPart = SDGEdge.Kind.CONTROL_DEP_EXPR.getPriorityPart();

	@Override
	public int hashCode() {
		int hc = m_source.hashCode() ^ (Integer.rotateLeft(m_target.hashCode(), ROT_BITS));
		hc = (hc & Kind.REMAINING_HASH_PART_MASK) | priorityPart;
		assert hc == super.hashCode();
		return hc;
	}
}
class SDGEdgeCONTROL_DEP_CALL extends SDGEdge {
	public SDGEdgeCONTROL_DEP_CALL(SDGNode source, SDGNode target) {
		super(source, target);
	}
	
	@Override
	public final Kind getKind() {
		return Kind.CONTROL_DEP_CALL;
	}
}
class SDGEdgeJUMP_DEP extends SDGEdge {
	public SDGEdgeJUMP_DEP(SDGNode source, SDGNode target) {
		super(source, target);
	}
	
	@Override
	public final Kind getKind() {
		return Kind.JUMP_DEP;
	}
}
class SDGEdgeNTSCD extends SDGEdge {
	public SDGEdgeNTSCD(SDGNode source, SDGNode target) {
		super(source, target);
	}
	
	@Override
	public final Kind getKind() {
		return Kind.NTSCD;
	}
}
class SDGEdgeSYNCHRONIZATION extends SDGEdge {
	public SDGEdgeSYNCHRONIZATION(SDGNode source, SDGNode target) {
		super(source, target);
	}
	
	@Override
	public final Kind getKind() {
		return Kind.SYNCHRONIZATION;
	}
}

class SDGEdgeDATA_DEP extends SDGEdge {
	public SDGEdgeDATA_DEP(SDGNode source, SDGNode target) {
		super(source, target);
	}
	
	@Override
	public final Kind getKind() {
		return Kind.DATA_DEP;
	}
}
class SDGEdgeDATA_HEAP extends SDGEdge {
	public SDGEdgeDATA_HEAP(SDGNode source, SDGNode target) {
		super(source, target);
	}
	
	@Override
	public final Kind getKind() {
		return Kind.DATA_HEAP;
	}
}
class SDGEdgeDATA_ALIAS extends SDGEdge {
	public SDGEdgeDATA_ALIAS(SDGNode source, SDGNode target) {
		super(source, target);
	}
	
	@Override
	public final Kind getKind() {
		return Kind.DATA_ALIAS;
	}
}
class SDGEdgeDATA_LOOP extends SDGEdge {
	public SDGEdgeDATA_LOOP(SDGNode source, SDGNode target) {
		super(source, target);
	}
	
	@Override
	public final Kind getKind() {
		return Kind.DATA_LOOP;
	}
}
class SDGEdgeDATA_DEP_EXPR_VALUE extends SDGEdge {
	public SDGEdgeDATA_DEP_EXPR_VALUE(SDGNode source, SDGNode target) {
		super(source, target);
	}
	
	@Override
	public final Kind getKind() {
		return Kind.DATA_DEP_EXPR_VALUE;
	}
}
class SDGEdgeDATA_DEP_EXPR_REFERENCE extends SDGEdge {
	public SDGEdgeDATA_DEP_EXPR_REFERENCE(SDGNode source, SDGNode target) {
		super(source, target);
	}
	
	@Override
	public final Kind getKind() {
		return Kind.DATA_DEP_EXPR_REFERENCE;
	}
}

class SDGEdgeSUMMARY extends SDGEdge {
	public SDGEdgeSUMMARY(SDGNode source, SDGNode target) {
		super(source, target);
	}
	
	@Override
	public final Kind getKind() {
		return Kind.SUMMARY;
	}
	
	private final static int priorityPart = SDGEdge.Kind.SUMMARY.getPriorityPart();

	@Override
	public int hashCode() {
		int hc = m_source.hashCode() ^ (Integer.rotateLeft(m_target.hashCode(), ROT_BITS));
		hc = (hc & Kind.REMAINING_HASH_PART_MASK) | priorityPart;
		assert hc == super.hashCode();
		return hc;
	}
}
class SDGEdgeSUMMARY_NO_ALIAS extends SDGEdge {
	public SDGEdgeSUMMARY_NO_ALIAS(SDGNode source, SDGNode target) {
		super(source, target);
	}
	
	@Override
	public final Kind getKind() {
		return Kind.SUMMARY_NO_ALIAS;
	}
}
class SDGEdgeSUMMARY_DATA extends SDGEdge {
	public SDGEdgeSUMMARY_DATA(SDGNode source, SDGNode target) {
		super(source, target);
	}
	
	@Override
	public final Kind getKind() {
		return Kind.SUMMARY_DATA;
	}
}
class SDGEdgeCALL extends SDGEdge {
	public SDGEdgeCALL(SDGNode source, SDGNode target) {
		super(source, target);
	}
	
	@Override
	public final Kind getKind() {
		return Kind.CALL;
	}
}
class SDGEdgePARAMETER_IN extends SDGEdge {
	public SDGEdgePARAMETER_IN(SDGNode source, SDGNode target) {
		super(source, target);
	}
	
	@Override
	public final Kind getKind() {
		return Kind.PARAMETER_IN;
	}
}
class SDGEdgePARAMETER_OUT extends SDGEdge {
	public SDGEdgePARAMETER_OUT(SDGNode source, SDGNode target) {
		super(source, target);
	}
	
	@Override
	public final Kind getKind() {
		return Kind.PARAMETER_OUT;
	}
}

class SDGEdgeINTERFERENCE extends SDGEdge {
	public SDGEdgeINTERFERENCE(SDGNode source, SDGNode target) {
		super(source, target);
	}
	
	@Override
	public final Kind getKind() {
		return Kind.INTERFERENCE;
	}
}
class SDGEdgeINTERFERENCE_WRITE extends SDGEdge {
	public SDGEdgeINTERFERENCE_WRITE(SDGNode source, SDGNode target) {
		super(source, target);
	}
	
	@Override
	public final Kind getKind() {
		return Kind.INTERFERENCE_WRITE;
	}
}
class SDGEdgeREADY_DEP extends SDGEdge {
	public SDGEdgeREADY_DEP(SDGNode source, SDGNode target) {
		super(source, target);
	}
	
	@Override
	public final Kind getKind() {
		return Kind.READY_DEP;
	}
}

class SDGEdgeFORK extends SDGEdge {
	public SDGEdgeFORK(SDGNode source, SDGNode target) {
		super(source, target);
	}
	
	@Override
	public final Kind getKind() {
		return Kind.FORK;
	}
}
class SDGEdgeFORK_IN extends SDGEdge {
	public SDGEdgeFORK_IN(SDGNode source, SDGNode target) {
		super(source, target);
	}
	
	@Override
	public final Kind getKind() {
		return Kind.FORK_IN;
	}
}
class SDGEdgeFORK_OUT extends SDGEdge {
	public SDGEdgeFORK_OUT(SDGNode source, SDGNode target) {
		super(source, target);
	}
	
	@Override
	public final Kind getKind() {
		return Kind.FORK_OUT;
	}
}
class SDGEdgeJOIN extends SDGEdge {
	public SDGEdgeJOIN(SDGNode source, SDGNode target) {
		super(source, target);
	}
	
	@Override
	public final Kind getKind() {
		return Kind.JOIN;
	}
}
class SDGEdgeJOIN_OUT extends SDGEdge {
	public SDGEdgeJOIN_OUT(SDGNode source, SDGNode target) {
		super(source, target);
	}
	
	@Override
	public final Kind getKind() {
		return Kind.JOIN_OUT;
	}
}

class SDGEdgeCONFLICT_DATA extends SDGEdge {
	public SDGEdgeCONFLICT_DATA(SDGNode source, SDGNode target) {
		super(source, target);
	}
	
	@Override
	public final Kind getKind() {
		return Kind.CONFLICT_DATA;
	}
}
class SDGEdgeCONFLICT_ORDER extends SDGEdge {
	public SDGEdgeCONFLICT_ORDER(SDGNode source, SDGNode target) {
		super(source, target);
	}
	
	@Override
	public final Kind getKind() {
		return Kind.CONFLICT_ORDER;
	}
}
class SDGEdgeFOLDED extends SDGEdge {
	public SDGEdgeFOLDED(SDGNode source, SDGNode target) {
		super(source, target);
	}
	
	@Override
	public final Kind getKind() {
		return Kind.FOLDED;
	}
}
class SDGEdgeFOLD_INCLUDE extends SDGEdge {
	public SDGEdgeFOLD_INCLUDE(SDGNode source, SDGNode target) {
		super(source, target);
	}
	
	@Override
	public final Kind getKind() {
		return Kind.FOLD_INCLUDE;
	}
}


