/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
 // SDG-Datei-Grammatik
grammar DeprecatedOldSDG_;

@header {/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.ifc.sdg.graph;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.Vector;
import java.util.LinkedList;
import edu.kit.joana.ifc.sdg.graph.slicer.graph.threads.ThreadsInformation;
import edu.kit.joana.ifc.sdg.graph.slicer.graph.threads.ThreadsInformation.ThreadInstance;
}

@lexer::header {/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.ifc.sdg.graph;

}

@lexer::members {
  @Override
  public void reportError(RecognitionException e) {
    super.reportError(e);
    Thrower.sneakyThrow(e);
  }

  /**
   * See "Puzzle 43: Exceptionally Unsafe" from Bloch Gafter, <i>Java Puzzlers</i>. Addison Wesley 2005.
   */
  static class Thrower {
    private static Throwable t;
    private Thrower() throws Throwable {
      throw t;
    }
    public static synchronized void sneakyThrow(Throwable t) {
      Thrower.t = t;
      try {
        Thrower.class.newInstance();
      } catch (InstantiationException e) {
        throw new IllegalArgumentException(e);
      } catch (IllegalAccessException e) {
        throw new IllegalArgumentException(e);
      } finally {
        Thrower.t = null; // Avoid memory leak
      }
    }
  }
}


@members {		private SDG sdg;
			//SDGFactory theCreator = new SDGFactory();
			//sdg.setGraphFactory(theCreator);
			Vector<SDGNode> carrier = new Vector<SDGNode>();
			SDGNode.Kind kind;
			SDGNode.Operation op;
			String source;
			int sr, sc, er, ec;
			int bcIndex;
			String bcName;
			int proc;
			String clsLoader;
			int id;
			boolean nonTerm;
			EdgeEntry entry;
			SDGNode node;
			String value;
			String type;
			ArrayList<Integer> threadNumbers;
      ArrayList<Integer> allocSites;
      ArrayList<Integer> aliasDataSource;
      SDGNode.NodeFactory nodeFact = new SDGNode.SDGNodeFactory();
      
      LinkedList<ThreadInstance> threads = new LinkedList<ThreadInstance>();
      int thread_id;
      SDGNode thread_entry;
      SDGNode exit = null;
      SDGNode fork = null;
      SDGNode join = null;
      LinkedList<SDGNode> con = new LinkedList<SDGNode>();
      boolean dynamic;

      private void writeAliasDataSource() {
          if (aliasDataSource != null && !aliasDataSource.isEmpty()) {
              for (final Integer aliasSrcId : aliasDataSource) {
                  node.addAliasDataSource(aliasSrcId.intValue());
              }
          }
      }

			private void writeThreadNumbers() {
		            if (!threadNumbers.isEmpty()) {
		            	int[] tn = new int[threadNumbers.size()];
				int i = 0;
		            	for (Integer nr : threadNumbers)
		            	  tn[i++] = nr;
		            	node.setThreadNumbers(tn);
		            }
			}

      private void writeAllocationSites() {
                if (!allocSites.isEmpty()) {
                  int[] tn = new int[allocSites.size()];
                  int i = 0;
                  for (Integer nr : allocSites) {
                    tn[i++] = nr;
                  }
                  node.setAllocationSites(tn);
                }
      }

      public void setNodeFactory(SDGNode.NodeFactory nf) {
        nodeFact = nf;  
      }

			private static String getText(Token t) {
				String s = t.getText();
				//if (s.charAt(0) == '<')
				return s.substring(1, s.length() - 1);
			}

			private class EdgeEntry {
				SDGEdge.Kind kind;
				int to;
				String label;
			}

			Map<Integer, Set<EdgeEntry>> edges = new HashMap<Integer, Set<EdgeEntry>>();
			
		  @Override
		  public void reportError(RecognitionException e) {
		    super.reportError(e);
		    Thrower.sneakyThrow(e);
		  }
		
		  /**
		   * See "Puzzle 43: Exceptionally Unsafe" from Bloch Gafter, <i>Java Puzzlers</i>. Addison Wesley 2005.
		   */
		  static class Thrower {
		    private static Throwable t;
		    private Thrower() throws Throwable {
		      throw t;
		    }
		    public static synchronized void sneakyThrow(Throwable t) {
		      Thrower.t = t;
		      try {
		        Thrower.class.newInstance();
		      } catch (InstantiationException e) {
		        throw new IllegalArgumentException(e);
		      } catch (IllegalAccessException e) {
		        throw new IllegalArgumentException(e);
		      } finally {
		        Thrower.t = null; // Avoid memory leak
		      }
		    }
		  }

	}

parse returns [SDG graph] throws RecognitionException:
			//{ theCreator = new SDG.SDG_Creator(sdg, slim); }
		// In einer Datei kann nur ein Graph definiert werden
		pdg
			{ graph = sdg; }
	;
private pdg throws RecognitionException:
		// Ein SDG besteht aus einer Liste von Knoten und Kanten
		'SDG'
				//{ sdg = new SDG(); }
		(
			'{'
				{ sdg = new SDG(); }
		|
			id=STRING '{'
				{ sdg = new SDG(getText(id)); }
		)
		(joana_compiler)? ( node )* ( thread )* '}'
		
		{sdg.setThreadsInfo(new ThreadsInformation(threads));}
			//{ theCreator.end_sdg(); }
	;
private joana_compiler throws RecognitionException:
    'JComp'
      { sdg.setJoanaCompiler(true); }
  ;
private thread throws RecognitionException:
    'Thread' i=number
      { thread_id = i; }
    '{' 
    'Entry' en=number ';'
    'Exit' ex=number ';'
    'Fork' fo=number ';'
    'Join' jo=number ';'
    'Context' con=context ';'
    'Dynamic' dyn=bool ';'
    '}'
    {thread_entry = sdg.getNode(en);
    if (ex != 0) exit = sdg.getNode(ex);
    if (fo != 0) fork = sdg.getNode(fo);
    if (jo != 0) join = sdg.getNode(jo);
    dynamic=dyn; 
    threads.add(new ThreadInstance(thread_id, thread_entry, exit, fork, join, con, dyn));}
  ;
private context returns [LinkedList<SDGNode> c = new LinkedList<SDGNode>();]:
  'null'
  |
  '[' i=number 
  {c.add(sdg.getNode(i));}
  (',' i=number {c.add(sdg.getNode(i));} )*
  ']'
  ;
private node throws RecognitionException:
  { bcName=null; bcIndex = -1 /* BytecodeLocation.UNDEFINED_POS_IN_BYTECODE */; }
		node_begin i=number
			{ id = i; threadNumbers = new ArrayList<Integer>(); allocSites = new ArrayList<Integer>(); nonTerm = false; }
		'{' node_infos node_edges '}'
	;
private node_begin:
		norm_node | pred_node | expr_node | exit_node//| error_node
    | call_node | actin_node | actout_node
    | entry_node | formin_node | formout_node
	| sync_node //| spawn_node
//    | folded_node
;
private norm_node:
		'NORM'
			{ kind = SDGNode.Kind.NORMAL; }
	;
private pred_node:
		'PRED'
			{ kind = SDGNode.Kind.PREDICATE; }
	;
private expr_node:
		'EXPR'
			{ kind = SDGNode.Kind.EXPRESSION; }
	;
private entry_node:
		'ENTR'
			{ kind = SDGNode.Kind.ENTRY; clsLoader = null; }
	;
private call_node:
		'CALL'
			{ kind = SDGNode.Kind.CALL; }
	;
private actin_node:
		'ACTI'
			{ kind = SDGNode.Kind.ACTUAL_IN; }
	;
private actout_node:
		'ACTO'
			{ kind = SDGNode.Kind.ACTUAL_OUT; }
	;
private formin_node:
		'FRMI'
			{ kind = SDGNode.Kind.FORMAL_IN; }
	;
private formout_node:
		'FRMO'
			{ kind = SDGNode.Kind.FORMAL_OUT; }
	;
//error_node:
//		'ERRR'
//			{ kind = SDGNode.Kind.ERROR; }
//	;
private exit_node:
		'EXIT'
			{ kind = SDGNode.Kind.EXIT; }
	;
//spawn_node:
//		'SPAW'
//			{ kind = SDGNode.Kind.SPAWN; }
//	;
private sync_node:
		'SYNC'
			{ kind = SDGNode.Kind.SYNCHRONIZATION; }
	;
folded_node:
		'FOLD'
			{ kind = SDGNode.Kind.FOLDED; }
	;
private node_infos:
		( node_attr ';' )*
	;
private node_attr:
		source | bytecode | unit | proc | oper | value | type | threadNr | nonTerm | classLoader | allocSites | aliasDataSource
	;
	
private classLoader:
  'C' loader=STRING { clsLoader = getText(loader); }
  ;

private nonTerm:
  'N' { nonTerm = true;}
  ; 

private threadNr:
		'Z' posNegTNR ( ',' posNegTNR )*
	;
	
private posNegTNR:
    nr=number 
      { threadNumbers.add(nr); }
    |
    '-' nr=number
      { nr = Integer.parseInt("-" + nr); threadNumbers.add(nr); }
	;

private aliasDataSource:
  'D' { aliasDataSource = new ArrayList<Integer>(); } aliasSrcId ( ',' aliasSrcId )*
  ;

private aliasSrcId:
  nr=number { aliasDataSource.add(nr); }
  ;

private allocSites:
    'A' posASNR ( ',' posASNR )*
  ;
  
private posASNR:
    nr=number 
      { allocSites.add(nr); }
  ;

private bytecode:
  'B' method=STRING ':' { boolean neg = false; } ('-' { neg = true; })? index=number { bcIndex = (neg ? -index : index); bcName = getText(method); }
  ;	
  
private source:
		// Quelltextposition
		'S' fl=STRING
		':' sr1=number
		',' sc1=number
		'-' er1=number
		',' ec1=number
			{ sr = sr1; sc = sc1; er = er1; ec = ec1; source = getText(fl); }
	;
private unit:
			//{ unsigned int num; }
		'U' number
			//{ theCreator.node_unit(num); }
	;
private proc:
			//{ unsigned int num; }
		'P' procNr=number { proc = procNr; }
			//{ theCreator.node_proc(num); }
	;
private oper:
		'O' ('empty'
				{ op = SDGNode.Operation.EMPTY; }
			|
			  'intconst'
				{ op = SDGNode.Operation.INT_CONST; }
			| 'floatconst'
				{ op = SDGNode.Operation.FLOAT_CONST; }
			| 'charconst'
				{ op = SDGNode.Operation.CHAR_CONST; }
			| 'stringconst'
				{ op = SDGNode.Operation.STRING_CONST; }
			| 'functionconst'
				{ op = SDGNode.Operation.FUNCTION_CONST; }
			| 'shortcut'
				{ op = SDGNode.Operation.SHORTCUT; }
			| 'question'
				{ op = SDGNode.Operation.QUESTION; }
			| 'binary'
				{ op = SDGNode.Operation.BINARY; }
			| 'unary'
				{ op = SDGNode.Operation.UNARY; }
			//	     | 'lookup'
			//{ op = SDGNode.Operation.LOOKUP; }
			| 'derefer'
				{ op = SDGNode.Operation.DEREFER; }
			| 'refer'
				{ op = SDGNode.Operation.REFER; }
			| 'array'
				{ op = SDGNode.Operation.ARRAY; }
			| 'select'
				{ op = SDGNode.Operation.SELECT; }
			| 'reference'
				{ op = SDGNode.Operation.REFERENCE; }
			| 'declaration'
				{ op = SDGNode.Operation.DECLARATION; }
			| 'modify'
				{ op = SDGNode.Operation.MODIFY; }
			| 'modassign'
				{ op = SDGNode.Operation.MODASSIGN; }
			| 'assign'
				{ op = SDGNode.Operation.ASSIGN; }
			| 'IF'
				{ op = SDGNode.Operation.IF; }
			| 'loop'
				{ op = SDGNode.Operation.LOOP; }
			| 'jump'
				{ op = SDGNode.Operation.JUMP; }
			| 'compound'
				{ op = SDGNode.Operation.COMPOUND; }
			| 'call'
				{ op = SDGNode.Operation.CALL; }
			| 'entry'
				{ op = SDGNode.Operation.ENTRY; }
			| 'exit'
				{ op = SDGNode.Operation.EXIT; }
			| 'form-in'
				{ op = SDGNode.Operation.FORMAL_IN; }
			| 'form-ellip'
				{ op = SDGNode.Operation.FORMAL_ELLIP; }
			| 'form-out'
				{ op = SDGNode.Operation.FORMAL_OUT; }
			| 'act-in'
				{ op = SDGNode.Operation.ACTUAL_IN; }
			| 'act-out'
				{ op = SDGNode.Operation.ACTUAL_OUT; }
//			| 'None'
//				{ op = SDGNode.Operation.NONE; }
			| 'monitor'
				{ op = SDGNode.Operation.MONITOR; }

		);
private value:
		'V' str=STRING
			{ value = getText(str); }
	;
private type:
		'T' str=STRING
			{ type = getText(str); }
	;
private node_edges throws FailedPredicateException:
			{		boolean found = false;
				for (int i = 0; i < op.getCorrespondingKind().length; i++) {
					if (op.getCorrespondingKind()[i] == kind) {
            node = nodeFact.createNode(op, i, id, value, proc, type, source, sr, sc, er, ec, bcName, bcIndex);
            if (clsLoader != null) {
              node.setClassLoader(clsLoader);
            }
            node.setMayBeNonTerminating(nonTerm);
            writeAliasDataSource();
		            	writeThreadNumbers();
		            	writeAllocationSites();
		            	found = true;
		            	break;
					}
			}
            if (!found)  throw new FailedPredicateException(input, "node_edges",
              "kind / operation mismatch at node " + id + ": " + kind + ", " + op);
		    value = null; type = null; source = null;
			if (carrier.size() <= id)
			  carrier.setSize(id + 1);
			carrier.set(id, node);
			sdg.addVertex(node);
			  Set<EdgeEntry> s = edges.get(id);
			  if (s != null) {
				  for (EdgeEntry e: s) {
				  	SDGEdge edge = new SDGEdge(carrier.get(e.to), node, e.kind, e.label);
				  	sdg.addEdge(edge);
				  }
				  edges.remove(id);
			  }
			}
		( edge ';' )*
			//{ theCreator.end_node(); }
	;
private edge:
			{ entry = new EdgeEntry(); }
		edge_begin edge_infos
			{ if (entry.to <= id) {
				SDGEdge edge = new SDGEdge(node, carrier.get(entry.to),
				  entry.kind, entry.label);
				sdg.addEdge(edge);
			  } else {
			  	Set<EdgeEntry> s = edges.get(entry.to);
			  	if (s == null) s = new HashSet<EdgeEntry>();
			  	s.add(entry);
			  	edges.put(entry.to, s);
			  	entry.to = id; // exchange roles of nodes
			  }
			}
	;
private edge_begin:
		he_edge | ps_edge |
		cd_edge |// cq_edge |
        dd_edge | dh_edge | vd_edge | rd_edge |// dl_edge | od_edge | ol_edge |
		id_edge | iw_edge | sd_edge | ry_edge |
        cf_edge | nf_edge | jf_edge | rf_edge |
		in_edge | ce_edge | cc_edge | jd_edge |
		call_edge | parmin_edge | parmout_edge |
		sum_edge | sumnoalias_edge | sumdata_edge |
		fork_edge | forkin_edge | forkout_edge | join_edge
//		fold_edge | foldincl_edge
	;
private he_edge:
		// Kontrollfluss-Kanten
		'HE'
			{ entry.kind = SDGEdge.Kind.HELP; }
	;
private ps_edge:
    // Parameterstruktur-Kanten
    'PS'
      { entry.kind = SDGEdge.Kind.PARAMETER_STRUCTURE; }
  ;
private in_edge:
		// Inklusions-Kanten, entsprechen Kontrollabhaengigkeitskanten ohne
		// Markierung.  Sie entstehen z.B. durch comma_list oder
		// statement_list.
		'UN'
			{ entry.kind = SDGEdge.Kind.CONTROL_DEP_UNCOND; }
	;
private ce_edge:
		'CE'
			{ entry.kind = SDGEdge.Kind.CONTROL_DEP_EXPR; }
	;
private cf_edge:
		// Kontrollfluss-Kanten
		'CF'
			{ entry.kind = SDGEdge.Kind.CONTROL_FLOW; }
	;
private nf_edge:
		// Kontrollfluss-Kanten
		'NF'
			{ entry.kind = SDGEdge.Kind.NO_FLOW; }
	;
private jf_edge:
		// Kontrollfluss-Kanten
		'JF'
			{ entry.kind = SDGEdge.Kind.JUMP_FLOW; }
	;
private rf_edge:
		// Kontrollfluss-Kanten
		'RF'
			{ entry.kind = SDGEdge.Kind.RETURN; }
	;	
private cd_edge:
		// Kontrollabhaengigkeits-Kanten
		'CD'
			{ entry.kind = SDGEdge.Kind.CONTROL_DEP_COND; }
	;
//cq_edge:
		// Kontrollabhaengigkeits-Kanten
//		'CQ'
//			{ entry.kind = SDGEdge.Kind.CQ; }
//	;
private cc_edge:
		// Kontrollabhaengigkeits-Kanten
		'CC'
			{ entry.kind = SDGEdge.Kind.CONTROL_DEP_CALL; }
	;
private jd_edge:
		// Sprung-Kanten
		'JD'
			{ entry.kind = SDGEdge.Kind.JUMP_DEP; }
	;
private dd_edge:
		// Datenabhaengigkeits-Kanten (loop-independent)
		'DD'
			{ entry.kind = SDGEdge.Kind.DATA_DEP; }
	;

private dh_edge:
    // Datenabhaengigkeits-Kanten (fuer werte auf dem heap)
    'DH'
      { entry.kind = SDGEdge.Kind.DATA_HEAP; }
    // Datenabhaengigkeits-Kanten (fuer werte auf dem heap)
   | 'DA'
      { entry.kind = SDGEdge.Kind.DATA_ALIAS; }
  ;
//dl_edge:
		// Datenabhaengigkeits-Kanten (loop-dependent)
//		'DL'
//			{ entry.kind = SDGEdge.Kind.DATA_DEP_LOOP; }
//	;
//od_edge:
		// Outputabhaengigkeits-Kanten (loop-independent)
//		'OD'
//			{ entry.kind = SDGEdge.Kind.OUTPUT_DEP; }
//	;
//ol_edge:
		// Outputabhaengigkeits-Kanten (loop-dependent)
//		'OL'
//			{ entry.kind = SDGEdge.Kind.OUTPUT_DEP_LOOP; }
//	;
private vd_edge:
		// Datenabhaengigkeits-Kanten innerhalb von Expressions
		'VD'
			{ entry.kind = SDGEdge.Kind.DATA_DEP_EXPR_VALUE; }
	;
private rd_edge:
		// Abhaengigkeits-Kanten durch Referenz-Erzeugung
		'RD'
			{ entry.kind = SDGEdge.Kind.DATA_DEP_EXPR_REFERENCE; }
	;
private id_edge:
		// Interferenz-Kanten innerhalb von Expressions
		'ID'
			{ entry.kind = SDGEdge.Kind.INTERFERENCE; }
	;
private iw_edge:
		// Interferenz-Kanten innerhalb von Expressions
		'IW'
			{ entry.kind = SDGEdge.Kind.INTERFERENCE_WRITE; }
	;
private sd_edge:
		// Synchronisations-Kanten innerhalb von Methoden
		'SD'
			{ entry.kind = SDGEdge.Kind.SYNCHRONIZATION; }
	;
private ry_edge:
		// Maybe infinite blocking-Kanten durch Threads
		'RY'
			{ entry.kind = SDGEdge.Kind.READY_DEP; }
	;
private call_edge:
		'CL'
			{ entry.kind = SDGEdge.Kind.CALL; }
	;
private parmin_edge:
		'PI'
			{ entry.kind = SDGEdge.Kind.PARAMETER_IN; }
	;
private parmout_edge:
		'PO'
			{ entry.kind = SDGEdge.Kind.PARAMETER_OUT; }
	;
private sum_edge:
		'SU' 
  		{ entry.kind = SDGEdge.Kind.SUMMARY; }
	| 'SP'
			{ entry.kind = SDGEdge.Kind.SUMMARY; }
	;
private sumnoalias_edge:
		'SH'
			{ entry.kind = SDGEdge.Kind.SUMMARY_NO_ALIAS; }
	;
private sumdata_edge:
    'SF'
      { entry.kind = SDGEdge.Kind.SUMMARY_DATA; }
  ;
fold_edge:
		'FD'
			{ entry.kind = SDGEdge.Kind.FOLDED; }
	;
foldincl_edge:
		'FI'
			{ entry.kind = SDGEdge.Kind.FOLD_INCLUDE; }
	;
private fork_edge:
		// Fork-Kanten
		'FORK'
			{ entry.kind = SDGEdge.Kind.FORK; }
	;	
private forkin_edge:
		// Parameter-In-Kanten an Fork-Stellen
		'FORK_IN'
			{ entry.kind = SDGEdge.Kind.FORK_IN; }
	;		
private forkout_edge:
		// Parameter-In-Kanten an Fork-Stellen
		'FORK_OUT'
			{ entry.kind = SDGEdge.Kind.FORK_OUT; }
	;	
private join_edge:
    // Fork-Kanten
    'JOIN'
      { entry.kind = SDGEdge.Kind.JOIN; }
  ;
	
	
private edge_infos:
		// Endknoten einer Kante
		id=number
			{ entry.to = id; }
		// ...und Attribute
		( ':' edge_label )?
	;
private edge_label:
		// Kanten koennen eine Markierung tragen
		str=STRING
			{ entry.label = getText(str); }
	;
private number returns [int num = 0]:
		txt=NUMBER
			{ num=Integer.parseInt(txt.getText()); }
	;
private bool returns [boolean b]:
    txt=IDENT
      { b=Boolean.parseBoolean(txt.getText()); }
  ;


//lexer grammar SDG_Lexer;

/*options
{
	charVocabulary = '\0'..'\377'; // 0-255 enough for utf8
}*/

WS_
	:	( ' '
        | '\t'
        | '\n' //{ newline(); }
        | '\r'
		)
			{ $channel=HIDDEN; }
	;
fragment
DIGIT
	:	'0'..'9'
	;
NUMBER
	:	(DIGIT)+
	;
IDENT
	:	('a'..'z'|'A'..'Z'|'_') ('a'..'z'|'A'..'Z'|'_'|'/'|'-'|'0'..'9')*
	;
STRING
//	: 	('<' '"' (~'"'|'"'~'>')*  '"' '>')
// see COMMENT_1 in http://www.antlr.org/grammar/pascal/pascal.g
	: '<' '"'
		( /*options { generateAmbigWarnings=false; }:*/
				{ input.LA(2) != '>' }? '"'
			|   ~('"')
		)*
		'"' '>'
//	: 	('<' '"' (~'"')* '"' '>')
	| 	('"' (~'"')* '"')
	;
/*OTHER
	:	('{'|'}'|';'|':'|'-'|',')
	;
*/
