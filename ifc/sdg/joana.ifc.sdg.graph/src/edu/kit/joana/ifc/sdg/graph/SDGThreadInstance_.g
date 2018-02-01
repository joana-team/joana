/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
grammar SDGThreadInstance_;

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
import edu.kit.joana.ifc.sdg.graph.slicer.graph.threads.ThreadsInformation;
import edu.kit.joana.ifc.sdg.graph.slicer.graph.threads.ThreadsInformation.ThreadInstance;
import edu.kit.joana.ifc.sdg.graph.slicer.graph.building.GraphFolder;
import gnu.trove.list.TIntList;
import gnu.trove.list.array.TIntArrayList;
import gnu.trove.procedure.TIntProcedure;
import java.util.HashMap;
import java.util.Map;
}

@members {

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

  static class ThreadInstanceStub {
    
    public static final int UNDEF_NODE = -1;
    private final int id;
    private final int entry;
    private final int exit;
    private final int fork;
    private final TIntList joins;
    private final TIntList threadContext;
    private boolean dynamic;

    public ThreadInstanceStub(int id, int en, int ex, int fo, TIntList jo, TIntList tc, boolean dyn) {
      this.id = id;
      this.entry = en;
      this.exit = ex;
      this.fork = fo;
      this.joins = jo;
      this.threadContext = tc;
      this.dynamic = dyn;
    }

    public ThreadInstance create(final SDG sdg) {
      final SDGNode tentry = findNode(sdg, entry);
      final SDGNode texit = findNode(sdg, exit);
      final SDGNode tfork = findNode(sdg, fork);
      final LinkedList<SDGNode> tjoins = findNodes(sdg, joins);
      final LinkedList<SDGNode> tcontext = findNodes(sdg, threadContext);  

      return new ThreadInstance(id, tentry, texit, tfork, tjoins, tcontext, dynamic);
    }

      // this is a bit ugly, but i cannot see any harm of sharing foldNodes even if once is to load severel .pdg
      // files during one session.
    private static final Map<Integer, SDGNode> foldNodes = new HashMap<>();

    private static LinkedList<SDGNode> findNodes(final SDG sdg, final TIntList ctx) {
      final LinkedList<SDGNode> nodes = new LinkedList<SDGNode>();
      
      ctx.forEach(new TIntProcedure() {
      
        @Override
        public boolean execute(final int id) {
          final SDGNode n = findNode(sdg, id);
          if (n != null) {
            assert id >= 0;
            nodes.add(n);
          } else if (id < 0 && id != ThreadInstanceStub.UNDEF_NODE) {
            // this was a fold node in the (folded) callgraph during DynamicContext enumeration
            nodes.add(foldNodes.compute(id, (k, foldNode) -> {
              if (foldNode == null) {
                foldNode = new SDGNode(SDGNode.Kind.FOLDED, id, GraphFolder.PROC_ID_FOR_FOLDED_LOOPS);
              }
              return foldNode;
            }));
          }
          
          return true;
        }
      });
            
      return nodes;
    }

    private static SDGNode findNode(final SDG sdg, final int id) {
      return (id == UNDEF_NODE ? null : sdg.getNode(id));
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

thread returns [ThreadInstanceStub ti]
  : 'Thread' id=number '{'
      'Entry'   en=number ';'
      'Exit'    ex=number ';'
      'Fork'    fo=mayNegNumber ';'
      'Join'    joins=listOrSingleNumber ';'
      'Context' con=context ';'
      'Dynamic' dyn=bool ';'
    '}'
    {
      final int entry = en;
      int exit = ThreadInstanceStub.UNDEF_NODE; if (ex != 0) { exit = ex; }
      int fork = ThreadInstanceStub.UNDEF_NODE; if (fo != 0) { fork = fo; }
      ti = new ThreadInstanceStub(id, entry, exit, fork, joins, con, dyn);
    }
  ;
  
private listOrSingleNumber returns [TIntList js]
  : joins=mayEmptyNumberList { js = joins; }
  | jo=number {
                js = new TIntArrayList();
                int join = ThreadInstanceStub.UNDEF_NODE; if (jo != 0) { join = jo; }
                js.add(join);
              }
  ;

private mayEmptyNumberList returns [TIntList cx = new TIntArrayList();]
  : 'null'
  | '[' ']'
  | '[' i=number { cx.add(i); } (',' i=number { cx.add(i); } )* ']'
  ;

private context returns [TIntList cx = new TIntArrayList();]
  : 'null'
  | '[' i=mayNegNumber { cx.add(i); } (',' i=mayNegNumber { cx.add(i); } )* ']'
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
