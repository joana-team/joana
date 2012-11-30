package edu.kit.joana.deprecated.jsdg.util;

/**
 * 
 * @author Juergen Graf <grafj@ipd.info.uni-karlsruhe.de>
 *
 */
public final class Debug {

	private Debug(){}
	
	public static enum Var  { 
		OLD_ASSERT(false),
		DUMP_CFG(false), 
		DUMP_PDG_CFG(false), 
		DUMP_CALLGRAPH(false), 
		DUMP_CDG(false), 
		DUMP_SSA(false),
		DUMP_HEAP_GRAPH(false),
		PRINT_MOD_REF_KILL_GEN(false),
		PRINT_SUMMARYEDGE_NODE_MAPPING_ERRORS(false),
		PRINT_SUMMARYEDGE_INFO(true),
		PRINT_INTERFACE(false),
		PRINT_THREADS(true),
		PRINT_CHARACTER_RANGE_TABLE(false),
		PRINT_THREAD_INTERFERENCES(false),
		PRINT_SUBOBJECT_TREE_INFO(false),
		PRINT_UNRESOLVED_CLASSES(true),
		PRINT_FIELD_PTS_INFO(false);
		
		private final boolean set;
		
		private Var(boolean set) {
			this.set = set;
		}
		
		public final boolean isSet() {
			return set;
		}
		
		public String toString() {
			return name() + "(" + (set ? "true" : "false") + ")";
		}
	}
	
	public static String getSettings() {
		StringBuilder str = new StringBuilder("Debug settings are:");
		for (Var v : Var.values()) {
			str.append(' ');
			str.append(v);
		}
		
		return str.toString();
	}

}
