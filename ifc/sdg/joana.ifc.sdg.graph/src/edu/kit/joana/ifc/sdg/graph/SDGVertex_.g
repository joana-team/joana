/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
grammar SDGVertex_;

options {
  language = Java;
  k = 2;
}

@header {/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.ifc.sdg.graph;

import java.util.LinkedList;
import gnu.trove.set.TIntSet;
import gnu.trove.set.hash.TIntHashSet;
}

@members {
  // Stores always the last position specified by a previous node. This is used
  // for sane error recovery, when no position is defined for a node:
  // We assume that its position may be somewhat equal to its pred node. 
  private static SourcePos defaultSrcPos = new SourcePos("undefined", 0, 0, 0, 0);
  private static ByteCodePos defaultBcPos = new ByteCodePos("<undefined>", -1);

  @Override
  public void reportError(final RecognitionException e) {
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
    public static synchronized void sneakyThrow(final Throwable t) {
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

  static final class SDGNodeStub {
  
    private final SDGNode.Kind kind;
    private final int id;
    private SourcePos spos;
    private ByteCodePos bpos;
    private int procId;
    private SDGNode.Operation op;
    private String val;
    private String type;
    private TIntSet threadNums;
    private boolean nonTerm = false;
    private String unresolvedCallTarget;
    private String classLoader;
    private TIntSet allocSites;
    private TIntSet aliasDataSrc;
    private List<String> localDefNames;
    private List<String> localUseNames;
    private final List<SDGEdgeStub> edges = new LinkedList<SDGEdgeStub>();
    
    private SDGNodeStub(final SDGNode.Kind kind, final int id, SourcePos defSPos, ByteCodePos defBPos) {
      this.kind = kind;
      this.id = id;
      this.spos = defSPos;
      this.bpos = defBPos;
    }
  
    private static int findKindId(final SDGNode.Operation op, final SDGNode.Kind kind) {
      final SDGNode.Kind[] kinds = op.getCorrespondingKind();
      for (int id = 0; id < kinds.length; id++) {
        if (kind == kinds[id]) {
          return id;
        }
      }
      
      // shoud not happen - default to first kind
      return 0;
    }
  
    public SDGNode createNode(final SDGNode.NodeFactory nf) {
      final int kindId = findKindId(op, kind);
      final SDGNode n = nf.createNode(op, kindId, id, val, procId, type, spos.filename,
        spos.startRow, spos.startColumn, spos.endRow, spos.endColumn, bpos.name, bpos.index);

      if (threadNums != null) {
        n.setThreadNumbers(threadNums.toArray());
      }

      if (nonTerm) {
        n.setMayBeNonTerminating(true);
      }

      if (classLoader != null) {
        n.setClassLoader(classLoader);
      }
      
      if (allocSites != null) {
        n.setAllocationSites(allocSites.toArray());
      }
      
      if (aliasDataSrc != null) {
        n.setAliasDataSources(aliasDataSrc);
      }
      
      if (unresolvedCallTarget != null) {
        n.setUnresolvedCallTarget(unresolvedCallTarget);
      }
      
      if (localDefNames != null) {
        n.setLocalDefNames(localDefNames.toArray(new String[localDefNames.size()]));
      }
      
      if (localUseNames != null) {
        n.setLocalUseNames(localUseNames.toArray(new String[localUseNames.size()]));
      }
      
      return n;
      
    }
    
    public void createEdges(final SDG sdg) {
      final SDGNode from = sdg.getNode(id);
      
      for (final SDGEdgeStub e : edges) {
        final SDGNode to = sdg.getNode(e.to);
        final SDGEdge edge = (e.label != null 
          ? new SDGEdge(from, to, e.kind, e.label)
          : new SDGEdge(from, to, e.kind));
        
        sdg.addEdge(edge);
      }
    }
  }
  
  static final class SourcePos {
  
    private final String filename;
    private final int startRow;
    private final int startColumn;
    private final int endRow;
    private final int endColumn;
  
    public SourcePos(final String filename, final int startRow, final int startColumn,
        final int endRow, final int endColumn) {
        this.filename = filename;
        this.startRow = startRow;
        this.startColumn = startColumn;
        this.endRow = endRow;
        this.endColumn = endColumn;
    }
  }
  
  static final class ByteCodePos {
  
    private final String name;
    private final int index;
    
    public ByteCodePos(final String name, final int index) {
      this.name = name;
      this.index = index;
    }
  
  }
  
  static final class SDGEdgeStub {
    
    private final SDGEdge.Kind kind;
    private final int to;
    private String label;
  
    private SDGEdgeStub(final SDGEdge.Kind kind, final int to) {
      this.kind = kind;
      this.to = to;
    }
  }
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

node returns [SDGNodeStub nstub]
  : k=node_kind id=mayNegNumber { nstub = new SDGNodeStub(k, id, defaultSrcPos, defaultBcPos); } 
    '{' 
      node_attributes[nstub] 
      node_edges[nstub]
    '}'
  ;

private node_kind returns [SDGNode.Kind kind]
  : 'NORM' { kind = SDGNode.Kind.NORMAL; }
  | 'PRED' { kind = SDGNode.Kind.PREDICATE; }
  | 'EXPR' { kind = SDGNode.Kind.EXPRESSION; }
  | 'ENTR' { kind = SDGNode.Kind.ENTRY; }
  | 'CALL' { kind = SDGNode.Kind.CALL; }
  | 'ACTI' { kind = SDGNode.Kind.ACTUAL_IN; }
  | 'ACTO' { kind = SDGNode.Kind.ACTUAL_OUT; }
  | 'FRMI' { kind = SDGNode.Kind.FORMAL_IN; }
  | 'FRMO' { kind = SDGNode.Kind.FORMAL_OUT; }
  | 'EXIT' { kind = SDGNode.Kind.EXIT; }
  | 'SYNC' { kind = SDGNode.Kind.SYNCHRONIZATION; }
  | 'FOLD' { kind = SDGNode.Kind.FOLDED; }
  ;

private node_attributes[SDGNodeStub node]
  : (node_attr[node] ';')*
  ;

private node_attr[SDGNodeStub node]
  : 'S' spos=node_source    { node.spos = spos; defaultSrcPos = spos; } // sourcecode position
  | 'B' bpos=node_bytecode  { node.bpos = bpos; defaultBcPos = bpos; }  // bytecode position
  | 'U' number                                            // deprecated 'unit' id
  | 'P' procId=number       { node.procId = procId; }     // procedure id
  | 'O' op=node_oper        { node.op = op; }             // operation
  | 'V' val=string          { node.val = val; }           // value
  | 'T' type=string         { node.type = type; }         // type
  | 'Z' tn=may_neg_num_set  { node.threadNums = tn; }     // thread numbers
  | 'N'                     { node.nonTerm = true; }      // no termination guaranteed (termination sensitive ana)
  | 'C' cl=string           { node.classLoader = cl; }    // class loader name
  | 'A' al=pos_num_set      { node.allocSites = al; }     // possible allocation sites (node ids of 'new')
  | 'D' ds=pos_num_set      { node.aliasDataSrc = ds; }    // definitve data sources for this value
  | 'U' uct=string          { node.unresolvedCallTarget = uct; } // signature of call target if call target is a native method
  | 'LD' ldefs=mayEmptyStringList {
                              node.localDefNames = ldefs;
                            } // names of local variables defined
  | 'LU' luses=mayEmptyStringList {
                              node.localUseNames = ldefs;
                            } // names of local variables used
  ;

private pos_num_set returns [TIntSet nums = new TIntHashSet();]
  : n=number { nums.add(n); } (',' n2=number { nums.add(n2); } )*
  ;

private may_neg_num_set returns [TIntSet nums = new TIntHashSet();]
  : n=mayNegNumber { nums.add(n); } (',' n2=mayNegNumber { nums.add(n2); } )*
  ;

private node_source returns [SourcePos spos]
  : filename=string ':' startRow=number ',' startColumn=number '-' endRow=number ',' endColumn=number
      { spos = new SourcePos(filename, startRow, startColumn, endRow, endColumn); }
  ;
  
private node_bytecode returns [ByteCodePos bpos]
  : name=string ':' index=mayNegNumber { bpos = new ByteCodePos(name, index); }
  ;

private node_oper returns [SDGNode.Operation op]
  : 'empty'          { op = SDGNode.Operation.EMPTY; }
  | 'intconst'       { op = SDGNode.Operation.INT_CONST; }
  | 'floatconst'     { op = SDGNode.Operation.FLOAT_CONST; }
  | 'charconst'      { op = SDGNode.Operation.CHAR_CONST; }
  | 'stringconst'    { op = SDGNode.Operation.STRING_CONST; }
  | 'functionconst'  { op = SDGNode.Operation.FUNCTION_CONST; }
  | 'shortcut'       { op = SDGNode.Operation.SHORTCUT; }
  | 'question'       { op = SDGNode.Operation.QUESTION; }
  | 'binary'         { op = SDGNode.Operation.BINARY; }
  | 'unary'          { op = SDGNode.Operation.UNARY; }
  | 'derefer'        { op = SDGNode.Operation.DEREFER; }
  | 'refer'          { op = SDGNode.Operation.REFER; }
  | 'array'          { op = SDGNode.Operation.ARRAY; }
  | 'select'         { op = SDGNode.Operation.SELECT; }
  | 'reference'      { op = SDGNode.Operation.REFERENCE; }
  | 'declaration'    { op = SDGNode.Operation.DECLARATION; }
  | 'modify'         { op = SDGNode.Operation.MODIFY; }
  | 'modassign'      { op = SDGNode.Operation.MODASSIGN; }
  | 'assign'         { op = SDGNode.Operation.ASSIGN; }
  | 'IF'             { op = SDGNode.Operation.IF; }
  | 'loop'           { op = SDGNode.Operation.LOOP; }
  | 'jump'           { op = SDGNode.Operation.JUMP; }
  | 'compound'       { op = SDGNode.Operation.COMPOUND; }
  | 'call'           { op = SDGNode.Operation.CALL; }
  | 'entry'          { op = SDGNode.Operation.ENTRY; }
  | 'exit'           { op = SDGNode.Operation.EXIT; }
  | 'form-in'        { op = SDGNode.Operation.FORMAL_IN; }
  | 'form-ellip'     { op = SDGNode.Operation.FORMAL_ELLIP; }
  | 'form-out'       { op = SDGNode.Operation.FORMAL_OUT; }
  | 'act-in'         { op = SDGNode.Operation.ACTUAL_IN; }
  | 'act-out'        { op = SDGNode.Operation.ACTUAL_OUT; }
  | 'monitor'        { op = SDGNode.Operation.MONITOR; }
  ;

private node_edges[SDGNodeStub node]
  : (e=edge ';' { node.edges.add(e); })*
  ;

private edge returns [SDGEdgeStub estub]
  : k=edge_kind nr=number { estub = new SDGEdgeStub(k, nr); } (':' label=string { estub.label = label; } )?
  ;

private edge_kind returns [SDGEdge.Kind kind]
// data dependencies
  : 'DD' { kind = SDGEdge.Kind.DATA_DEP; }            // data dependencies between local variables
  | 'DH' { kind = SDGEdge.Kind.DATA_HEAP; }           // data dependencies between field accesses
  | 'DA' { kind = SDGEdge.Kind.DATA_ALIAS; }          // data dependencies between aliasing fields accesses
// control dependencies
  | 'CD' { kind = SDGEdge.Kind.CONTROL_DEP_COND; }    // control dependencies between statements
  | 'CE' { kind = SDGEdge.Kind.CONTROL_DEP_EXPR; }    // control dependencies between nodes that correspond to the same statement
  | 'UN' { kind = SDGEdge.Kind.CONTROL_DEP_UNCOND; }  // unconditional control dependencies
// control flow
  | 'CF' { kind = SDGEdge.Kind.CONTROL_FLOW; }        // control flow between statements
  | 'NF' { kind = SDGEdge.Kind.NO_FLOW; }             // control flow that is actually not possible (dead code)
  | 'RF' { kind = SDGEdge.Kind.RETURN; }              // control flow from method exit to call site
// method call related
  | 'CC' { kind = SDGEdge.Kind.CONTROL_DEP_CALL; }
  | 'CL' { kind = SDGEdge.Kind.CALL; }
  | 'PI' { kind = SDGEdge.Kind.PARAMETER_IN; }
  | 'PO' { kind = SDGEdge.Kind.PARAMETER_OUT; }
// summary edges
  | 'SU' { kind = SDGEdge.Kind.SUMMARY; }
  | 'SH' { kind = SDGEdge.Kind.SUMMARY_NO_ALIAS; }
  | 'SF' { kind = SDGEdge.Kind.SUMMARY_DATA; }
// method interface structure
  | 'PS' { kind = SDGEdge.Kind.PARAMETER_STRUCTURE; }
// thread/concurrency related edges
  | 'FORK' { kind = SDGEdge.Kind.FORK; }
  | 'FORK_IN' { kind = SDGEdge.Kind.FORK_IN; }
  | 'FORK_OUT' { kind = SDGEdge.Kind.FORK_OUT; }
  | 'JOIN' { kind = SDGEdge.Kind.JOIN; }
  | 'ID' { kind = SDGEdge.Kind.INTERFERENCE; }
  | 'IW' { kind = SDGEdge.Kind.INTERFERENCE_WRITE; }
  | 'SD' { kind = SDGEdge.Kind.SYNCHRONIZATION; }
// general helper edges
  | 'HE' { kind = SDGEdge.Kind.HELP; }
  | 'FD' { kind = SDGEdge.Kind.FOLDED; }
  | 'FI' { kind = SDGEdge.Kind.FOLD_INCLUDE; }
// deprecated edges
  | 'RY' { kind = SDGEdge.Kind.READY_DEP; }
  | 'JF' { kind = SDGEdge.Kind.JUMP_FLOW; }
  | 'SP' { kind = SDGEdge.Kind.SUMMARY; }
  | 'VD' { kind = SDGEdge.Kind.DATA_DEP_EXPR_VALUE; }
  | 'RD' { kind = SDGEdge.Kind.DATA_DEP_EXPR_REFERENCE; }
  | 'JD' { kind = SDGEdge.Kind.JUMP_DEP; }
  ;

private mayNegNumber returns [int nr]
  : '-' n=number { nr = -n; }
  | n=number { nr = n; }
  ;

private number returns [int nr]
  : n=NUMBER { nr = Integer.parseInt(n.getText()); }
  ;

private string returns [String str]
  : s=STRING { str = s.getText(); str = str.substring(1, str.length() - 1); }
  ;

private mayEmptyStringList returns [LinkedList<String> ss = new LinkedList<String>();]
  : 'null'
  | '[' ']'
  | '[' s=string { ss.add(s); } (',' s=string { ss.add(s); } )* ']'
  ;

private bool returns [boolean b]
  : 'true'  { b = true; }
  | 'false' { b = false; }
  ;

// Lexer rules below

WHITESPACE
  : ( ' ' | '\t' | '\n' | '\r' )  { $channel=HIDDEN; }
  ;
  
NUMBER
  : ('0'..'9')+
  ;

STRING
  : '<' '"' ( { input.LA(2) != '>' }? '"' | ~('"') )* '"' '>'   // deprecated
  | ('"' (~'"')* '"')
  ;