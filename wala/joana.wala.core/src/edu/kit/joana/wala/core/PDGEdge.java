/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.wala.core;



public final class PDGEdge {

	public final PDGNode from;
	public final PDGNode to;
	public final Kind kind;
	private String label = null;

	public static enum Kind {
		DATA_DEP("DD", true, true, false, false), // data dependence stack
		DATA_HEAP("DH", true, true, false, false), // data dependence heap
		DATA_ALIAS("DA", true, true, false, false), // data dependence heap alias
		SUMMARY_DATA("SD", true, true, false, false), // data dependence summary edges (for stack data deps)
		SUMMARY_NO_ALIAS("SU", true, true, false, false), // data dependence summary edges (for stack data deps)
		SUMMARY_ALIAS("SA", true, true, false, false), // data dependence summary edges (for stack data deps)
		CONTROL_DEP("CD", true, false, false, true), // control dependence
		CONTROL_DEP_EXPR("CE", true, false, false, true), // control dependence for expressions (used to connect parameter nodes)
		CONTROL_FLOW("CF", false, false, true, false), // control flow
		CONTROL_FLOW_EXC("CFE", false, false, true, false), // control flow through exception
		PARAM_STRUCT("PS", false, false, false, false), // parameter structure
		UTILITY("HE", false, false, false, false),		// help/utility edge for layouting in graphviewer
        PARAMETER_IN("PI", true, true, false, false), // parameter input
        PARAMETER_OUT("PO", true, true, false, false), // parameter output
		CALL_STATIC("CS", true, false, false, false), // call static
		CALL_VIRTUAL("CV", true, false, false, false), // call virtual (dynamic dispatch)
		RETURN("RET", true, false, false, false), // return edge
		INTERFERENCE("ID", false, true, false, false), // read-write interference edge
		INTERFERENCE_WRITE("IW", false, true, false, false),
		FORK("FORK", false, false, false, false), // special edges for calls of Thread.run() from Thread.start()
		FORK_IN("FORK_IN", false, false, false, false); // param-in edges for calls of Thread.run() from Thread.start()
        private final String name;
        private final boolean isRelevant; // signals kinds that represent a program dependence.
        private final boolean isData; // is a deta dependence
        private final boolean isFlow; // is control flow
        private final boolean isControl; //is a control dependence

        private Kind(String name, boolean relevant, boolean isData, boolean isFlow, boolean isControl) {
        	this.name = name;
        	this.isRelevant = relevant;
        	this.isData = isData;
        	this.isControl = isControl;
        	this.isFlow = isFlow;
        }

        /**
         * @return Returns the name of this kind.
         */
        public String toString() {
            return name;
        }

        /**
         * @return `true' if this kind denotes a program dependence.
         */
        public boolean isRelevant() {
            return isRelevant;
        }

		public boolean isData() {
			return isData;
		}

		public boolean isFlow() {
			return isFlow;
		}

		public boolean isControl() {
			return isControl;
		}

	};

	public PDGEdge(PDGNode from, PDGNode to, Kind kind) {
		if (from == null || to == null) {
			throw new IllegalArgumentException(from + " -" + kind + "-> " + to);
		}

		this.from = from;
		this.to = to;
		this.kind = kind;
	}

	public boolean equals(Object obj) {
		if (obj == this) {
			return true;
		}

		if (obj instanceof PDGEdge) {
			PDGEdge other = (PDGEdge) obj;
			return from.equals(other.from) && to.equals(other.to) && kind == other.kind;
		}

		return false;
	}

	public int hashCode() {
		return (from.hashCode() ^ (to.hashCode() >> 6)) + kind.hashCode();
	}

	public String toString() {
//		return from.toString() + "-" + kind.name() + "->" + to.toString();
		return kind.toString();
	}
	
	public String toDetailedString() {
		return from.toString() + "-" + kind.name() + "->" + to.toString();
	}

	public void setLabel(String string) {
		if (kind != Kind.DATA_ALIAS) {
			throw new IllegalStateException("setLabel not supported by " + this.getClass().getSimpleName());
		}

		this.label = string;
	}

	public String getLabel() {
		return label;
	}

}
