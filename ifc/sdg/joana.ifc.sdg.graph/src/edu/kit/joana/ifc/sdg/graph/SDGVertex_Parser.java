// $ANTLR 3.1.3 Mar 17, 2009 19:23:44 /Users/jgf/Documents/Projects/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDGVertex_.g 2014-06-23 17:44:29
/**
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


import org.antlr.runtime.*;
import java.util.Stack;
import java.util.List;
import java.util.ArrayList;

/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
public class SDGVertex_Parser extends Parser {
    public static final String[] tokenNames = new String[] {
        "<invalid>", "<EOR>", "<DOWN>", "<UP>", "NUMBER", "STRING", "WHITESPACE", "'{'", "'}'", "'NORM'", "'PRED'", "'EXPR'", "'ENTR'", "'CALL'", "'ACTI'", "'ACTO'", "'FRMI'", "'FRMO'", "'EXIT'", "'SYNC'", "'FOLD'", "';'", "'S'", "'B'", "'U'", "'P'", "'O'", "'V'", "'T'", "'Z'", "'N'", "'C'", "'A'", "'D'", "','", "':'", "'-'", "'empty'", "'intconst'", "'floatconst'", "'charconst'", "'stringconst'", "'functionconst'", "'shortcut'", "'question'", "'binary'", "'unary'", "'derefer'", "'refer'", "'array'", "'select'", "'reference'", "'declaration'", "'modify'", "'modassign'", "'assign'", "'IF'", "'loop'", "'jump'", "'compound'", "'call'", "'entry'", "'exit'", "'form-in'", "'form-ellip'", "'form-out'", "'act-in'", "'act-out'", "'monitor'", "'DD'", "'DH'", "'DA'", "'CD'", "'CE'", "'UN'", "'CF'", "'NF'", "'RF'", "'CC'", "'CL'", "'PI'", "'PO'", "'SU'", "'SH'", "'SF'", "'PS'", "'FORK'", "'FORK_IN'", "'FORK_OUT'", "'JOIN'", "'ID'", "'IW'", "'SD'", "'HE'", "'FD'", "'FI'", "'RY'", "'JF'", "'SP'", "'VD'", "'RD'", "'JD'", "'true'", "'false'"
    };
    public static final int T__29=29;
    public static final int T__28=28;
    public static final int T__27=27;
    public static final int T__26=26;
    public static final int T__25=25;
    public static final int T__24=24;
    public static final int T__23=23;
    public static final int T__22=22;
    public static final int T__21=21;
    public static final int T__20=20;
    public static final int EOF=-1;
    public static final int T__9=9;
    public static final int T__8=8;
    public static final int T__7=7;
    public static final int T__93=93;
    public static final int T__94=94;
    public static final int T__19=19;
    public static final int T__91=91;
    public static final int T__92=92;
    public static final int T__16=16;
    public static final int T__15=15;
    public static final int T__90=90;
    public static final int T__18=18;
    public static final int T__17=17;
    public static final int T__12=12;
    public static final int T__11=11;
    public static final int T__14=14;
    public static final int T__13=13;
    public static final int T__10=10;
    public static final int T__99=99;
    public static final int T__98=98;
    public static final int T__97=97;
    public static final int T__96=96;
    public static final int T__95=95;
    public static final int T__80=80;
    public static final int T__81=81;
    public static final int T__82=82;
    public static final int T__83=83;
    public static final int NUMBER=4;
    public static final int WHITESPACE=6;
    public static final int T__85=85;
    public static final int T__84=84;
    public static final int T__87=87;
    public static final int T__86=86;
    public static final int T__89=89;
    public static final int T__88=88;
    public static final int T__71=71;
    public static final int T__72=72;
    public static final int T__70=70;
    public static final int T__76=76;
    public static final int T__75=75;
    public static final int T__74=74;
    public static final int T__73=73;
    public static final int T__79=79;
    public static final int T__78=78;
    public static final int T__77=77;
    public static final int T__68=68;
    public static final int T__69=69;
    public static final int T__66=66;
    public static final int T__67=67;
    public static final int T__64=64;
    public static final int T__65=65;
    public static final int T__62=62;
    public static final int T__63=63;
    public static final int T__61=61;
    public static final int T__60=60;
    public static final int T__55=55;
    public static final int T__56=56;
    public static final int T__57=57;
    public static final int T__58=58;
    public static final int T__51=51;
    public static final int T__52=52;
    public static final int T__53=53;
    public static final int T__54=54;
    public static final int T__103=103;
    public static final int T__59=59;
    public static final int T__50=50;
    public static final int T__42=42;
    public static final int T__43=43;
    public static final int T__40=40;
    public static final int T__41=41;
    public static final int T__46=46;
    public static final int T__47=47;
    public static final int T__44=44;
    public static final int T__45=45;
    public static final int T__48=48;
    public static final int T__49=49;
    public static final int T__102=102;
    public static final int T__101=101;
    public static final int T__100=100;
    public static final int T__30=30;
    public static final int T__31=31;
    public static final int T__32=32;
    public static final int T__33=33;
    public static final int T__34=34;
    public static final int T__35=35;
    public static final int T__36=36;
    public static final int T__37=37;
    public static final int T__38=38;
    public static final int T__39=39;
    public static final int STRING=5;

    // delegates
    // delegators


        public SDGVertex_Parser(TokenStream input) {
            this(input, new RecognizerSharedState());
        }
        public SDGVertex_Parser(TokenStream input, RecognizerSharedState state) {
            super(input, state);
             
        }
        

    public String[] getTokenNames() { return SDGVertex_Parser.tokenNames; }
    public String getGrammarFileName() { return "/Users/jgf/Documents/Projects/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDGVertex_.g"; }


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



    // $ANTLR start "node"
    // /Users/jgf/Documents/Projects/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDGVertex_.g:230:1: node returns [SDGNodeStub nstub] : k= node_kind id= mayNegNumber '{' node_attributes[nstub] node_edges[nstub] '}' ;
    public final SDGNodeStub node() throws RecognitionException {
        SDGNodeStub nstub = null;

        SDGNode.Kind k = null;

        int id = 0;


        try {
            // /Users/jgf/Documents/Projects/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDGVertex_.g:231:3: (k= node_kind id= mayNegNumber '{' node_attributes[nstub] node_edges[nstub] '}' )
            // /Users/jgf/Documents/Projects/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDGVertex_.g:231:5: k= node_kind id= mayNegNumber '{' node_attributes[nstub] node_edges[nstub] '}'
            {
            pushFollow(FOLLOW_node_kind_in_node73);
            k=node_kind();

            state._fsp--;

            pushFollow(FOLLOW_mayNegNumber_in_node77);
            id=mayNegNumber();

            state._fsp--;

             nstub = new SDGNodeStub(k, id, defaultSrcPos, defaultBcPos); 
            match(input,7,FOLLOW_7_in_node86); 
            pushFollow(FOLLOW_node_attributes_in_node95);
            node_attributes(nstub);

            state._fsp--;

            pushFollow(FOLLOW_node_edges_in_node105);
            node_edges(nstub);

            state._fsp--;

            match(input,8,FOLLOW_8_in_node112); 

            }

        }
        catch (RecognitionException re) {
            reportError(re);
            recover(input,re);
        }
        finally {
        }
        return nstub;
    }
    // $ANTLR end "node"


    // $ANTLR start "node_kind"
    // /Users/jgf/Documents/Projects/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDGVertex_.g:238:9: private node_kind returns [SDGNode.Kind kind] : ( 'NORM' | 'PRED' | 'EXPR' | 'ENTR' | 'CALL' | 'ACTI' | 'ACTO' | 'FRMI' | 'FRMO' | 'EXIT' | 'SYNC' | 'FOLD' );
    public final SDGNode.Kind node_kind() throws RecognitionException {
        SDGNode.Kind kind = null;

        try {
            // /Users/jgf/Documents/Projects/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDGVertex_.g:239:3: ( 'NORM' | 'PRED' | 'EXPR' | 'ENTR' | 'CALL' | 'ACTI' | 'ACTO' | 'FRMI' | 'FRMO' | 'EXIT' | 'SYNC' | 'FOLD' )
            int alt1=12;
            alt1 = dfa1.predict(input);
            switch (alt1) {
                case 1 :
                    // /Users/jgf/Documents/Projects/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDGVertex_.g:239:5: 'NORM'
                    {
                    match(input,9,FOLLOW_9_in_node_kind131); 
                     kind = SDGNode.Kind.NORMAL; 

                    }
                    break;
                case 2 :
                    // /Users/jgf/Documents/Projects/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDGVertex_.g:240:5: 'PRED'
                    {
                    match(input,10,FOLLOW_10_in_node_kind139); 
                     kind = SDGNode.Kind.PREDICATE; 

                    }
                    break;
                case 3 :
                    // /Users/jgf/Documents/Projects/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDGVertex_.g:241:5: 'EXPR'
                    {
                    match(input,11,FOLLOW_11_in_node_kind147); 
                     kind = SDGNode.Kind.EXPRESSION; 

                    }
                    break;
                case 4 :
                    // /Users/jgf/Documents/Projects/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDGVertex_.g:242:5: 'ENTR'
                    {
                    match(input,12,FOLLOW_12_in_node_kind155); 
                     kind = SDGNode.Kind.ENTRY; 

                    }
                    break;
                case 5 :
                    // /Users/jgf/Documents/Projects/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDGVertex_.g:243:5: 'CALL'
                    {
                    match(input,13,FOLLOW_13_in_node_kind163); 
                     kind = SDGNode.Kind.CALL; 

                    }
                    break;
                case 6 :
                    // /Users/jgf/Documents/Projects/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDGVertex_.g:244:5: 'ACTI'
                    {
                    match(input,14,FOLLOW_14_in_node_kind171); 
                     kind = SDGNode.Kind.ACTUAL_IN; 

                    }
                    break;
                case 7 :
                    // /Users/jgf/Documents/Projects/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDGVertex_.g:245:5: 'ACTO'
                    {
                    match(input,15,FOLLOW_15_in_node_kind179); 
                     kind = SDGNode.Kind.ACTUAL_OUT; 

                    }
                    break;
                case 8 :
                    // /Users/jgf/Documents/Projects/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDGVertex_.g:246:5: 'FRMI'
                    {
                    match(input,16,FOLLOW_16_in_node_kind187); 
                     kind = SDGNode.Kind.FORMAL_IN; 

                    }
                    break;
                case 9 :
                    // /Users/jgf/Documents/Projects/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDGVertex_.g:247:5: 'FRMO'
                    {
                    match(input,17,FOLLOW_17_in_node_kind195); 
                     kind = SDGNode.Kind.FORMAL_OUT; 

                    }
                    break;
                case 10 :
                    // /Users/jgf/Documents/Projects/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDGVertex_.g:248:5: 'EXIT'
                    {
                    match(input,18,FOLLOW_18_in_node_kind203); 
                     kind = SDGNode.Kind.EXIT; 

                    }
                    break;
                case 11 :
                    // /Users/jgf/Documents/Projects/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDGVertex_.g:249:5: 'SYNC'
                    {
                    match(input,19,FOLLOW_19_in_node_kind211); 
                     kind = SDGNode.Kind.SYNCHRONIZATION; 

                    }
                    break;
                case 12 :
                    // /Users/jgf/Documents/Projects/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDGVertex_.g:250:5: 'FOLD'
                    {
                    match(input,20,FOLLOW_20_in_node_kind219); 
                     kind = SDGNode.Kind.FOLDED; 

                    }
                    break;

            }
        }
        catch (RecognitionException re) {
            reportError(re);
            recover(input,re);
        }
        finally {
        }
        return kind;
    }
    // $ANTLR end "node_kind"


    // $ANTLR start "node_attributes"
    // /Users/jgf/Documents/Projects/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDGVertex_.g:253:9: private node_attributes[SDGNodeStub node] : ( node_attr[node] ';' )* ;
    public final void node_attributes(SDGNodeStub node) throws RecognitionException {
        try {
            // /Users/jgf/Documents/Projects/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDGVertex_.g:254:3: ( ( node_attr[node] ';' )* )
            // /Users/jgf/Documents/Projects/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDGVertex_.g:254:5: ( node_attr[node] ';' )*
            {
            // /Users/jgf/Documents/Projects/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDGVertex_.g:254:5: ( node_attr[node] ';' )*
            loop2:
            do {
                int alt2=2;
                alt2 = dfa2.predict(input);
                switch (alt2) {
            	case 1 :
            	    // /Users/jgf/Documents/Projects/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDGVertex_.g:254:6: node_attr[node] ';'
            	    {
            	    pushFollow(FOLLOW_node_attr_in_node_attributes238);
            	    node_attr(node);

            	    state._fsp--;

            	    match(input,21,FOLLOW_21_in_node_attributes241); 

            	    }
            	    break;

            	default :
            	    break loop2;
                }
            } while (true);


            }

        }
        catch (RecognitionException re) {
            reportError(re);
            recover(input,re);
        }
        finally {
        }
        return ;
    }
    // $ANTLR end "node_attributes"


    // $ANTLR start "node_attr"
    // /Users/jgf/Documents/Projects/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDGVertex_.g:257:9: private node_attr[SDGNodeStub node] : ( 'S' spos= node_source | 'B' bpos= node_bytecode | 'U' number | 'P' procId= number | 'O' op= node_oper | 'V' val= string | 'T' type= string | 'Z' tn= may_neg_num_set | 'N' | 'C' cl= string | 'A' al= pos_num_set | 'D' ds= pos_num_set | 'U' uct= string );
    public final void node_attr(SDGNodeStub node) throws RecognitionException {
        SourcePos spos = null;

        ByteCodePos bpos = null;

        int procId = 0;

        SDGNode.Operation op = null;

        String val = null;

        String type = null;

        TIntSet tn = null;

        String cl = null;

        TIntSet al = null;

        TIntSet ds = null;

        String uct = null;


        try {
            // /Users/jgf/Documents/Projects/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDGVertex_.g:258:3: ( 'S' spos= node_source | 'B' bpos= node_bytecode | 'U' number | 'P' procId= number | 'O' op= node_oper | 'V' val= string | 'T' type= string | 'Z' tn= may_neg_num_set | 'N' | 'C' cl= string | 'A' al= pos_num_set | 'D' ds= pos_num_set | 'U' uct= string )
            int alt3=13;
            alt3 = dfa3.predict(input);
            switch (alt3) {
                case 1 :
                    // /Users/jgf/Documents/Projects/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDGVertex_.g:258:5: 'S' spos= node_source
                    {
                    match(input,22,FOLLOW_22_in_node_attr259); 
                    pushFollow(FOLLOW_node_source_in_node_attr263);
                    spos=node_source();

                    state._fsp--;

                     node.spos = spos; defaultSrcPos = spos; 

                    }
                    break;
                case 2 :
                    // /Users/jgf/Documents/Projects/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDGVertex_.g:259:5: 'B' bpos= node_bytecode
                    {
                    match(input,23,FOLLOW_23_in_node_attr275); 
                    pushFollow(FOLLOW_node_bytecode_in_node_attr279);
                    bpos=node_bytecode();

                    state._fsp--;

                     node.bpos = bpos; defaultBcPos = bpos; 

                    }
                    break;
                case 3 :
                    // /Users/jgf/Documents/Projects/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDGVertex_.g:260:5: 'U' number
                    {
                    match(input,24,FOLLOW_24_in_node_attr290); 
                    pushFollow(FOLLOW_number_in_node_attr292);
                    number();

                    state._fsp--;


                    }
                    break;
                case 4 :
                    // /Users/jgf/Documents/Projects/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDGVertex_.g:261:5: 'P' procId= number
                    {
                    match(input,25,FOLLOW_25_in_node_attr342); 
                    pushFollow(FOLLOW_number_in_node_attr346);
                    procId=number();

                    state._fsp--;

                     node.procId = procId; 

                    }
                    break;
                case 5 :
                    // /Users/jgf/Documents/Projects/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDGVertex_.g:262:5: 'O' op= node_oper
                    {
                    match(input,26,FOLLOW_26_in_node_attr365); 
                    pushFollow(FOLLOW_node_oper_in_node_attr369);
                    op=node_oper();

                    state._fsp--;

                     node.op = op; 

                    }
                    break;
                case 6 :
                    // /Users/jgf/Documents/Projects/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDGVertex_.g:263:5: 'V' val= string
                    {
                    match(input,27,FOLLOW_27_in_node_attr397); 
                    pushFollow(FOLLOW_string_in_node_attr401);
                    val=string();

                    state._fsp--;

                     node.val = val; 

                    }
                    break;
                case 7 :
                    // /Users/jgf/Documents/Projects/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDGVertex_.g:264:5: 'T' type= string
                    {
                    match(input,28,FOLLOW_28_in_node_attr429); 
                    pushFollow(FOLLOW_string_in_node_attr433);
                    type=string();

                    state._fsp--;

                     node.type = type; 

                    }
                    break;
                case 8 :
                    // /Users/jgf/Documents/Projects/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDGVertex_.g:265:5: 'Z' tn= may_neg_num_set
                    {
                    match(input,29,FOLLOW_29_in_node_attr458); 
                    pushFollow(FOLLOW_may_neg_num_set_in_node_attr462);
                    tn=may_neg_num_set();

                    state._fsp--;

                     node.threadNums = tn; 

                    }
                    break;
                case 9 :
                    // /Users/jgf/Documents/Projects/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDGVertex_.g:266:5: 'N'
                    {
                    match(input,30,FOLLOW_30_in_node_attr476); 
                     node.nonTerm = true; 

                    }
                    break;
                case 10 :
                    // /Users/jgf/Documents/Projects/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDGVertex_.g:267:5: 'C' cl= string
                    {
                    match(input,31,FOLLOW_31_in_node_attr510); 
                    pushFollow(FOLLOW_string_in_node_attr514);
                    cl=string();

                    state._fsp--;

                     node.classLoader = cl; 

                    }
                    break;
                case 11 :
                    // /Users/jgf/Documents/Projects/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDGVertex_.g:268:5: 'A' al= pos_num_set
                    {
                    match(input,32,FOLLOW_32_in_node_attr536); 
                    pushFollow(FOLLOW_pos_num_set_in_node_attr540);
                    al=pos_num_set();

                    state._fsp--;

                     node.allocSites = al; 

                    }
                    break;
                case 12 :
                    // /Users/jgf/Documents/Projects/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDGVertex_.g:269:5: 'D' ds= pos_num_set
                    {
                    match(input,33,FOLLOW_33_in_node_attr558); 
                    pushFollow(FOLLOW_pos_num_set_in_node_attr562);
                    ds=pos_num_set();

                    state._fsp--;

                     node.aliasDataSrc = ds; 

                    }
                    break;
                case 13 :
                    // /Users/jgf/Documents/Projects/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDGVertex_.g:270:5: 'U' uct= string
                    {
                    match(input,24,FOLLOW_24_in_node_attr579); 
                    pushFollow(FOLLOW_string_in_node_attr583);
                    uct=string();

                    state._fsp--;

                     node.unresolvedCallTarget = uct; 

                    }
                    break;

            }
        }
        catch (RecognitionException re) {
            reportError(re);
            recover(input,re);
        }
        finally {
        }
        return ;
    }
    // $ANTLR end "node_attr"


    // $ANTLR start "pos_num_set"
    // /Users/jgf/Documents/Projects/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDGVertex_.g:273:9: private pos_num_set returns [TIntSet nums = new TIntHashSet();] : n= number ( ',' n2= number )* ;
    public final TIntSet pos_num_set() throws RecognitionException {
        TIntSet nums =  new TIntHashSet();;

        int n = 0;

        int n2 = 0;


        try {
            // /Users/jgf/Documents/Projects/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDGVertex_.g:274:3: (n= number ( ',' n2= number )* )
            // /Users/jgf/Documents/Projects/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDGVertex_.g:274:5: n= number ( ',' n2= number )*
            {
            pushFollow(FOLLOW_number_in_pos_num_set616);
            n=number();

            state._fsp--;

             nums.add(n); 
            // /Users/jgf/Documents/Projects/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDGVertex_.g:274:31: ( ',' n2= number )*
            loop4:
            do {
                int alt4=2;
                int LA4_0 = input.LA(1);

                if ( (LA4_0==34) ) {
                    alt4=1;
                }


                switch (alt4) {
            	case 1 :
            	    // /Users/jgf/Documents/Projects/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDGVertex_.g:274:32: ',' n2= number
            	    {
            	    match(input,34,FOLLOW_34_in_pos_num_set621); 
            	    pushFollow(FOLLOW_number_in_pos_num_set625);
            	    n2=number();

            	    state._fsp--;

            	     nums.add(n2); 

            	    }
            	    break;

            	default :
            	    break loop4;
                }
            } while (true);


            }

        }
        catch (RecognitionException re) {
            reportError(re);
            recover(input,re);
        }
        finally {
        }
        return nums;
    }
    // $ANTLR end "pos_num_set"


    // $ANTLR start "may_neg_num_set"
    // /Users/jgf/Documents/Projects/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDGVertex_.g:277:9: private may_neg_num_set returns [TIntSet nums = new TIntHashSet();] : n= mayNegNumber ( ',' n2= mayNegNumber )* ;
    public final TIntSet may_neg_num_set() throws RecognitionException {
        TIntSet nums =  new TIntHashSet();;

        int n = 0;

        int n2 = 0;


        try {
            // /Users/jgf/Documents/Projects/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDGVertex_.g:278:3: (n= mayNegNumber ( ',' n2= mayNegNumber )* )
            // /Users/jgf/Documents/Projects/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDGVertex_.g:278:5: n= mayNegNumber ( ',' n2= mayNegNumber )*
            {
            pushFollow(FOLLOW_mayNegNumber_in_may_neg_num_set651);
            n=mayNegNumber();

            state._fsp--;

             nums.add(n); 
            // /Users/jgf/Documents/Projects/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDGVertex_.g:278:37: ( ',' n2= mayNegNumber )*
            loop5:
            do {
                int alt5=2;
                int LA5_0 = input.LA(1);

                if ( (LA5_0==34) ) {
                    alt5=1;
                }


                switch (alt5) {
            	case 1 :
            	    // /Users/jgf/Documents/Projects/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDGVertex_.g:278:38: ',' n2= mayNegNumber
            	    {
            	    match(input,34,FOLLOW_34_in_may_neg_num_set656); 
            	    pushFollow(FOLLOW_mayNegNumber_in_may_neg_num_set660);
            	    n2=mayNegNumber();

            	    state._fsp--;

            	     nums.add(n2); 

            	    }
            	    break;

            	default :
            	    break loop5;
                }
            } while (true);


            }

        }
        catch (RecognitionException re) {
            reportError(re);
            recover(input,re);
        }
        finally {
        }
        return nums;
    }
    // $ANTLR end "may_neg_num_set"


    // $ANTLR start "node_source"
    // /Users/jgf/Documents/Projects/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDGVertex_.g:281:9: private node_source returns [SourcePos spos] : filename= string ':' startRow= number ',' startColumn= number '-' endRow= number ',' endColumn= number ;
    public final SourcePos node_source() throws RecognitionException {
        SourcePos spos = null;

        String filename = null;

        int startRow = 0;

        int startColumn = 0;

        int endRow = 0;

        int endColumn = 0;


        try {
            // /Users/jgf/Documents/Projects/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDGVertex_.g:282:3: (filename= string ':' startRow= number ',' startColumn= number '-' endRow= number ',' endColumn= number )
            // /Users/jgf/Documents/Projects/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDGVertex_.g:282:5: filename= string ':' startRow= number ',' startColumn= number '-' endRow= number ',' endColumn= number
            {
            pushFollow(FOLLOW_string_in_node_source686);
            filename=string();

            state._fsp--;

            match(input,35,FOLLOW_35_in_node_source688); 
            pushFollow(FOLLOW_number_in_node_source692);
            startRow=number();

            state._fsp--;

            match(input,34,FOLLOW_34_in_node_source694); 
            pushFollow(FOLLOW_number_in_node_source698);
            startColumn=number();

            state._fsp--;

            match(input,36,FOLLOW_36_in_node_source700); 
            pushFollow(FOLLOW_number_in_node_source704);
            endRow=number();

            state._fsp--;

            match(input,34,FOLLOW_34_in_node_source706); 
            pushFollow(FOLLOW_number_in_node_source710);
            endColumn=number();

            state._fsp--;

             spos = new SourcePos(filename, startRow, startColumn, endRow, endColumn); 

            }

        }
        catch (RecognitionException re) {
            reportError(re);
            recover(input,re);
        }
        finally {
        }
        return spos;
    }
    // $ANTLR end "node_source"


    // $ANTLR start "node_bytecode"
    // /Users/jgf/Documents/Projects/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDGVertex_.g:286:9: private node_bytecode returns [ByteCodePos bpos] : name= string ':' index= mayNegNumber ;
    public final ByteCodePos node_bytecode() throws RecognitionException {
        ByteCodePos bpos = null;

        String name = null;

        int index = 0;


        try {
            // /Users/jgf/Documents/Projects/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDGVertex_.g:287:3: (name= string ':' index= mayNegNumber )
            // /Users/jgf/Documents/Projects/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDGVertex_.g:287:5: name= string ':' index= mayNegNumber
            {
            pushFollow(FOLLOW_string_in_node_bytecode741);
            name=string();

            state._fsp--;

            match(input,35,FOLLOW_35_in_node_bytecode743); 
            pushFollow(FOLLOW_mayNegNumber_in_node_bytecode747);
            index=mayNegNumber();

            state._fsp--;

             bpos = new ByteCodePos(name, index); 

            }

        }
        catch (RecognitionException re) {
            reportError(re);
            recover(input,re);
        }
        finally {
        }
        return bpos;
    }
    // $ANTLR end "node_bytecode"


    // $ANTLR start "node_oper"
    // /Users/jgf/Documents/Projects/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDGVertex_.g:290:9: private node_oper returns [SDGNode.Operation op] : ( 'empty' | 'intconst' | 'floatconst' | 'charconst' | 'stringconst' | 'functionconst' | 'shortcut' | 'question' | 'binary' | 'unary' | 'derefer' | 'refer' | 'array' | 'select' | 'reference' | 'declaration' | 'modify' | 'modassign' | 'assign' | 'IF' | 'loop' | 'jump' | 'compound' | 'call' | 'entry' | 'exit' | 'form-in' | 'form-ellip' | 'form-out' | 'act-in' | 'act-out' | 'monitor' );
    public final SDGNode.Operation node_oper() throws RecognitionException {
        SDGNode.Operation op = null;

        try {
            // /Users/jgf/Documents/Projects/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDGVertex_.g:291:3: ( 'empty' | 'intconst' | 'floatconst' | 'charconst' | 'stringconst' | 'functionconst' | 'shortcut' | 'question' | 'binary' | 'unary' | 'derefer' | 'refer' | 'array' | 'select' | 'reference' | 'declaration' | 'modify' | 'modassign' | 'assign' | 'IF' | 'loop' | 'jump' | 'compound' | 'call' | 'entry' | 'exit' | 'form-in' | 'form-ellip' | 'form-out' | 'act-in' | 'act-out' | 'monitor' )
            int alt6=32;
            alt6 = dfa6.predict(input);
            switch (alt6) {
                case 1 :
                    // /Users/jgf/Documents/Projects/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDGVertex_.g:291:5: 'empty'
                    {
                    match(input,37,FOLLOW_37_in_node_oper768); 
                     op = SDGNode.Operation.EMPTY; 

                    }
                    break;
                case 2 :
                    // /Users/jgf/Documents/Projects/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDGVertex_.g:292:5: 'intconst'
                    {
                    match(input,38,FOLLOW_38_in_node_oper785); 
                     op = SDGNode.Operation.INT_CONST; 

                    }
                    break;
                case 3 :
                    // /Users/jgf/Documents/Projects/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDGVertex_.g:293:5: 'floatconst'
                    {
                    match(input,39,FOLLOW_39_in_node_oper799); 
                     op = SDGNode.Operation.FLOAT_CONST; 

                    }
                    break;
                case 4 :
                    // /Users/jgf/Documents/Projects/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDGVertex_.g:294:5: 'charconst'
                    {
                    match(input,40,FOLLOW_40_in_node_oper811); 
                     op = SDGNode.Operation.CHAR_CONST; 

                    }
                    break;
                case 5 :
                    // /Users/jgf/Documents/Projects/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDGVertex_.g:295:5: 'stringconst'
                    {
                    match(input,41,FOLLOW_41_in_node_oper824); 
                     op = SDGNode.Operation.STRING_CONST; 

                    }
                    break;
                case 6 :
                    // /Users/jgf/Documents/Projects/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDGVertex_.g:296:5: 'functionconst'
                    {
                    match(input,42,FOLLOW_42_in_node_oper835); 
                     op = SDGNode.Operation.FUNCTION_CONST; 

                    }
                    break;
                case 7 :
                    // /Users/jgf/Documents/Projects/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDGVertex_.g:297:5: 'shortcut'
                    {
                    match(input,43,FOLLOW_43_in_node_oper844); 
                     op = SDGNode.Operation.SHORTCUT; 

                    }
                    break;
                case 8 :
                    // /Users/jgf/Documents/Projects/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDGVertex_.g:298:5: 'question'
                    {
                    match(input,44,FOLLOW_44_in_node_oper858); 
                     op = SDGNode.Operation.QUESTION; 

                    }
                    break;
                case 9 :
                    // /Users/jgf/Documents/Projects/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDGVertex_.g:299:5: 'binary'
                    {
                    match(input,45,FOLLOW_45_in_node_oper872); 
                     op = SDGNode.Operation.BINARY; 

                    }
                    break;
                case 10 :
                    // /Users/jgf/Documents/Projects/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDGVertex_.g:300:5: 'unary'
                    {
                    match(input,46,FOLLOW_46_in_node_oper888); 
                     op = SDGNode.Operation.UNARY; 

                    }
                    break;
                case 11 :
                    // /Users/jgf/Documents/Projects/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDGVertex_.g:301:5: 'derefer'
                    {
                    match(input,47,FOLLOW_47_in_node_oper905); 
                     op = SDGNode.Operation.DEREFER; 

                    }
                    break;
                case 12 :
                    // /Users/jgf/Documents/Projects/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDGVertex_.g:302:5: 'refer'
                    {
                    match(input,48,FOLLOW_48_in_node_oper920); 
                     op = SDGNode.Operation.REFER; 

                    }
                    break;
                case 13 :
                    // /Users/jgf/Documents/Projects/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDGVertex_.g:303:5: 'array'
                    {
                    match(input,49,FOLLOW_49_in_node_oper937); 
                     op = SDGNode.Operation.ARRAY; 

                    }
                    break;
                case 14 :
                    // /Users/jgf/Documents/Projects/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDGVertex_.g:304:5: 'select'
                    {
                    match(input,50,FOLLOW_50_in_node_oper954); 
                     op = SDGNode.Operation.SELECT; 

                    }
                    break;
                case 15 :
                    // /Users/jgf/Documents/Projects/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDGVertex_.g:305:5: 'reference'
                    {
                    match(input,51,FOLLOW_51_in_node_oper970); 
                     op = SDGNode.Operation.REFERENCE; 

                    }
                    break;
                case 16 :
                    // /Users/jgf/Documents/Projects/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDGVertex_.g:306:5: 'declaration'
                    {
                    match(input,52,FOLLOW_52_in_node_oper983); 
                     op = SDGNode.Operation.DECLARATION; 

                    }
                    break;
                case 17 :
                    // /Users/jgf/Documents/Projects/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDGVertex_.g:307:5: 'modify'
                    {
                    match(input,53,FOLLOW_53_in_node_oper994); 
                     op = SDGNode.Operation.MODIFY; 

                    }
                    break;
                case 18 :
                    // /Users/jgf/Documents/Projects/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDGVertex_.g:308:5: 'modassign'
                    {
                    match(input,54,FOLLOW_54_in_node_oper1010); 
                     op = SDGNode.Operation.MODASSIGN; 

                    }
                    break;
                case 19 :
                    // /Users/jgf/Documents/Projects/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDGVertex_.g:309:5: 'assign'
                    {
                    match(input,55,FOLLOW_55_in_node_oper1023); 
                     op = SDGNode.Operation.ASSIGN; 

                    }
                    break;
                case 20 :
                    // /Users/jgf/Documents/Projects/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDGVertex_.g:310:5: 'IF'
                    {
                    match(input,56,FOLLOW_56_in_node_oper1039); 
                     op = SDGNode.Operation.IF; 

                    }
                    break;
                case 21 :
                    // /Users/jgf/Documents/Projects/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDGVertex_.g:311:5: 'loop'
                    {
                    match(input,57,FOLLOW_57_in_node_oper1059); 
                     op = SDGNode.Operation.LOOP; 

                    }
                    break;
                case 22 :
                    // /Users/jgf/Documents/Projects/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDGVertex_.g:312:5: 'jump'
                    {
                    match(input,58,FOLLOW_58_in_node_oper1077); 
                     op = SDGNode.Operation.JUMP; 

                    }
                    break;
                case 23 :
                    // /Users/jgf/Documents/Projects/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDGVertex_.g:313:5: 'compound'
                    {
                    match(input,59,FOLLOW_59_in_node_oper1095); 
                     op = SDGNode.Operation.COMPOUND; 

                    }
                    break;
                case 24 :
                    // /Users/jgf/Documents/Projects/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDGVertex_.g:314:5: 'call'
                    {
                    match(input,60,FOLLOW_60_in_node_oper1109); 
                     op = SDGNode.Operation.CALL; 

                    }
                    break;
                case 25 :
                    // /Users/jgf/Documents/Projects/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDGVertex_.g:315:5: 'entry'
                    {
                    match(input,61,FOLLOW_61_in_node_oper1127); 
                     op = SDGNode.Operation.ENTRY; 

                    }
                    break;
                case 26 :
                    // /Users/jgf/Documents/Projects/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDGVertex_.g:316:5: 'exit'
                    {
                    match(input,62,FOLLOW_62_in_node_oper1144); 
                     op = SDGNode.Operation.EXIT; 

                    }
                    break;
                case 27 :
                    // /Users/jgf/Documents/Projects/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDGVertex_.g:317:5: 'form-in'
                    {
                    match(input,63,FOLLOW_63_in_node_oper1162); 
                     op = SDGNode.Operation.FORMAL_IN; 

                    }
                    break;
                case 28 :
                    // /Users/jgf/Documents/Projects/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDGVertex_.g:318:5: 'form-ellip'
                    {
                    match(input,64,FOLLOW_64_in_node_oper1177); 
                     op = SDGNode.Operation.FORMAL_ELLIP; 

                    }
                    break;
                case 29 :
                    // /Users/jgf/Documents/Projects/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDGVertex_.g:319:5: 'form-out'
                    {
                    match(input,65,FOLLOW_65_in_node_oper1189); 
                     op = SDGNode.Operation.FORMAL_OUT; 

                    }
                    break;
                case 30 :
                    // /Users/jgf/Documents/Projects/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDGVertex_.g:320:5: 'act-in'
                    {
                    match(input,66,FOLLOW_66_in_node_oper1203); 
                     op = SDGNode.Operation.ACTUAL_IN; 

                    }
                    break;
                case 31 :
                    // /Users/jgf/Documents/Projects/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDGVertex_.g:321:5: 'act-out'
                    {
                    match(input,67,FOLLOW_67_in_node_oper1219); 
                     op = SDGNode.Operation.ACTUAL_OUT; 

                    }
                    break;
                case 32 :
                    // /Users/jgf/Documents/Projects/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDGVertex_.g:322:5: 'monitor'
                    {
                    match(input,68,FOLLOW_68_in_node_oper1234); 
                     op = SDGNode.Operation.MONITOR; 

                    }
                    break;

            }
        }
        catch (RecognitionException re) {
            reportError(re);
            recover(input,re);
        }
        finally {
        }
        return op;
    }
    // $ANTLR end "node_oper"


    // $ANTLR start "node_edges"
    // /Users/jgf/Documents/Projects/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDGVertex_.g:325:9: private node_edges[SDGNodeStub node] : (e= edge ';' )* ;
    public final void node_edges(SDGNodeStub node) throws RecognitionException {
        SDGEdgeStub e = null;


        try {
            // /Users/jgf/Documents/Projects/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDGVertex_.g:326:3: ( (e= edge ';' )* )
            // /Users/jgf/Documents/Projects/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDGVertex_.g:326:5: (e= edge ';' )*
            {
            // /Users/jgf/Documents/Projects/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDGVertex_.g:326:5: (e= edge ';' )*
            loop7:
            do {
                int alt7=2;
                alt7 = dfa7.predict(input);
                switch (alt7) {
            	case 1 :
            	    // /Users/jgf/Documents/Projects/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDGVertex_.g:326:6: e= edge ';'
            	    {
            	    pushFollow(FOLLOW_edge_in_node_edges1262);
            	    e=edge();

            	    state._fsp--;

            	    match(input,21,FOLLOW_21_in_node_edges1264); 
            	     node.edges.add(e); 

            	    }
            	    break;

            	default :
            	    break loop7;
                }
            } while (true);


            }

        }
        catch (RecognitionException re) {
            reportError(re);
            recover(input,re);
        }
        finally {
        }
        return ;
    }
    // $ANTLR end "node_edges"


    // $ANTLR start "edge"
    // /Users/jgf/Documents/Projects/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDGVertex_.g:329:9: private edge returns [SDGEdgeStub estub] : k= edge_kind nr= number ( ':' label= string )? ;
    public final SDGEdgeStub edge() throws RecognitionException {
        SDGEdgeStub estub = null;

        SDGEdge.Kind k = null;

        int nr = 0;

        String label = null;


        try {
            // /Users/jgf/Documents/Projects/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDGVertex_.g:330:3: (k= edge_kind nr= number ( ':' label= string )? )
            // /Users/jgf/Documents/Projects/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDGVertex_.g:330:5: k= edge_kind nr= number ( ':' label= string )?
            {
            pushFollow(FOLLOW_edge_kind_in_edge1289);
            k=edge_kind();

            state._fsp--;

            pushFollow(FOLLOW_number_in_edge1293);
            nr=number();

            state._fsp--;

             estub = new SDGEdgeStub(k, nr); 
            // /Users/jgf/Documents/Projects/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDGVertex_.g:330:63: ( ':' label= string )?
            int alt8=2;
            int LA8_0 = input.LA(1);

            if ( (LA8_0==35) ) {
                alt8=1;
            }
            switch (alt8) {
                case 1 :
                    // /Users/jgf/Documents/Projects/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDGVertex_.g:330:64: ':' label= string
                    {
                    match(input,35,FOLLOW_35_in_edge1298); 
                    pushFollow(FOLLOW_string_in_edge1302);
                    label=string();

                    state._fsp--;

                     estub.label = label; 

                    }
                    break;

            }


            }

        }
        catch (RecognitionException re) {
            reportError(re);
            recover(input,re);
        }
        finally {
        }
        return estub;
    }
    // $ANTLR end "edge"


    // $ANTLR start "edge_kind"
    // /Users/jgf/Documents/Projects/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDGVertex_.g:333:9: private edge_kind returns [SDGEdge.Kind kind] : ( 'DD' | 'DH' | 'DA' | 'CD' | 'CE' | 'UN' | 'CF' | 'NF' | 'RF' | 'CC' | 'CL' | 'PI' | 'PO' | 'SU' | 'SH' | 'SF' | 'PS' | 'FORK' | 'FORK_IN' | 'FORK_OUT' | 'JOIN' | 'ID' | 'IW' | 'SD' | 'HE' | 'FD' | 'FI' | 'RY' | 'JF' | 'SP' | 'VD' | 'RD' | 'JD' );
    public final SDGEdge.Kind edge_kind() throws RecognitionException {
        SDGEdge.Kind kind = null;

        try {
            // /Users/jgf/Documents/Projects/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDGVertex_.g:335:3: ( 'DD' | 'DH' | 'DA' | 'CD' | 'CE' | 'UN' | 'CF' | 'NF' | 'RF' | 'CC' | 'CL' | 'PI' | 'PO' | 'SU' | 'SH' | 'SF' | 'PS' | 'FORK' | 'FORK_IN' | 'FORK_OUT' | 'JOIN' | 'ID' | 'IW' | 'SD' | 'HE' | 'FD' | 'FI' | 'RY' | 'JF' | 'SP' | 'VD' | 'RD' | 'JD' )
            int alt9=33;
            alt9 = dfa9.predict(input);
            switch (alt9) {
                case 1 :
                    // /Users/jgf/Documents/Projects/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDGVertex_.g:335:5: 'DD'
                    {
                    match(input,69,FOLLOW_69_in_edge_kind1327); 
                     kind = SDGEdge.Kind.DATA_DEP; 

                    }
                    break;
                case 2 :
                    // /Users/jgf/Documents/Projects/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDGVertex_.g:336:5: 'DH'
                    {
                    match(input,70,FOLLOW_70_in_edge_kind1347); 
                     kind = SDGEdge.Kind.DATA_HEAP; 

                    }
                    break;
                case 3 :
                    // /Users/jgf/Documents/Projects/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDGVertex_.g:337:5: 'DA'
                    {
                    match(input,71,FOLLOW_71_in_edge_kind1366); 
                     kind = SDGEdge.Kind.DATA_ALIAS; 

                    }
                    break;
                case 4 :
                    // /Users/jgf/Documents/Projects/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDGVertex_.g:339:5: 'CD'
                    {
                    match(input,72,FOLLOW_72_in_edge_kind1385); 
                     kind = SDGEdge.Kind.CONTROL_DEP_COND; 

                    }
                    break;
                case 5 :
                    // /Users/jgf/Documents/Projects/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDGVertex_.g:340:5: 'CE'
                    {
                    match(input,73,FOLLOW_73_in_edge_kind1397); 
                     kind = SDGEdge.Kind.CONTROL_DEP_EXPR; 

                    }
                    break;
                case 6 :
                    // /Users/jgf/Documents/Projects/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDGVertex_.g:341:5: 'UN'
                    {
                    match(input,74,FOLLOW_74_in_edge_kind1409); 
                     kind = SDGEdge.Kind.CONTROL_DEP_UNCOND; 

                    }
                    break;
                case 7 :
                    // /Users/jgf/Documents/Projects/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDGVertex_.g:343:5: 'CF'
                    {
                    match(input,75,FOLLOW_75_in_edge_kind1420); 
                     kind = SDGEdge.Kind.CONTROL_FLOW; 

                    }
                    break;
                case 8 :
                    // /Users/jgf/Documents/Projects/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDGVertex_.g:344:5: 'NF'
                    {
                    match(input,76,FOLLOW_76_in_edge_kind1436); 
                     kind = SDGEdge.Kind.NO_FLOW; 

                    }
                    break;
                case 9 :
                    // /Users/jgf/Documents/Projects/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDGVertex_.g:345:5: 'RF'
                    {
                    match(input,77,FOLLOW_77_in_edge_kind1457); 
                     kind = SDGEdge.Kind.RETURN; 

                    }
                    break;
                case 10 :
                    // /Users/jgf/Documents/Projects/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDGVertex_.g:347:5: 'CC'
                    {
                    match(input,78,FOLLOW_78_in_edge_kind1480); 
                     kind = SDGEdge.Kind.CONTROL_DEP_CALL; 

                    }
                    break;
                case 11 :
                    // /Users/jgf/Documents/Projects/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDGVertex_.g:348:5: 'CL'
                    {
                    match(input,79,FOLLOW_79_in_edge_kind1488); 
                     kind = SDGEdge.Kind.CALL; 

                    }
                    break;
                case 12 :
                    // /Users/jgf/Documents/Projects/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDGVertex_.g:349:5: 'PI'
                    {
                    match(input,80,FOLLOW_80_in_edge_kind1496); 
                     kind = SDGEdge.Kind.PARAMETER_IN; 

                    }
                    break;
                case 13 :
                    // /Users/jgf/Documents/Projects/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDGVertex_.g:350:5: 'PO'
                    {
                    match(input,81,FOLLOW_81_in_edge_kind1504); 
                     kind = SDGEdge.Kind.PARAMETER_OUT; 

                    }
                    break;
                case 14 :
                    // /Users/jgf/Documents/Projects/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDGVertex_.g:352:5: 'SU'
                    {
                    match(input,82,FOLLOW_82_in_edge_kind1513); 
                     kind = SDGEdge.Kind.SUMMARY; 

                    }
                    break;
                case 15 :
                    // /Users/jgf/Documents/Projects/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDGVertex_.g:353:5: 'SH'
                    {
                    match(input,83,FOLLOW_83_in_edge_kind1521); 
                     kind = SDGEdge.Kind.SUMMARY_NO_ALIAS; 

                    }
                    break;
                case 16 :
                    // /Users/jgf/Documents/Projects/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDGVertex_.g:354:5: 'SF'
                    {
                    match(input,84,FOLLOW_84_in_edge_kind1529); 
                     kind = SDGEdge.Kind.SUMMARY_DATA; 

                    }
                    break;
                case 17 :
                    // /Users/jgf/Documents/Projects/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDGVertex_.g:356:5: 'PS'
                    {
                    match(input,85,FOLLOW_85_in_edge_kind1538); 
                     kind = SDGEdge.Kind.PARAMETER_STRUCTURE; 

                    }
                    break;
                case 18 :
                    // /Users/jgf/Documents/Projects/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDGVertex_.g:358:5: 'FORK'
                    {
                    match(input,86,FOLLOW_86_in_edge_kind1547); 
                     kind = SDGEdge.Kind.FORK; 

                    }
                    break;
                case 19 :
                    // /Users/jgf/Documents/Projects/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDGVertex_.g:359:5: 'FORK_IN'
                    {
                    match(input,87,FOLLOW_87_in_edge_kind1555); 
                     kind = SDGEdge.Kind.FORK_IN; 

                    }
                    break;
                case 20 :
                    // /Users/jgf/Documents/Projects/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDGVertex_.g:360:5: 'FORK_OUT'
                    {
                    match(input,88,FOLLOW_88_in_edge_kind1563); 
                     kind = SDGEdge.Kind.FORK_OUT; 

                    }
                    break;
                case 21 :
                    // /Users/jgf/Documents/Projects/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDGVertex_.g:361:5: 'JOIN'
                    {
                    match(input,89,FOLLOW_89_in_edge_kind1571); 
                     kind = SDGEdge.Kind.JOIN; 

                    }
                    break;
                case 22 :
                    // /Users/jgf/Documents/Projects/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDGVertex_.g:362:5: 'ID'
                    {
                    match(input,90,FOLLOW_90_in_edge_kind1579); 
                     kind = SDGEdge.Kind.INTERFERENCE; 

                    }
                    break;
                case 23 :
                    // /Users/jgf/Documents/Projects/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDGVertex_.g:363:5: 'IW'
                    {
                    match(input,91,FOLLOW_91_in_edge_kind1587); 
                     kind = SDGEdge.Kind.INTERFERENCE_WRITE; 

                    }
                    break;
                case 24 :
                    // /Users/jgf/Documents/Projects/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDGVertex_.g:364:5: 'SD'
                    {
                    match(input,92,FOLLOW_92_in_edge_kind1595); 
                     kind = SDGEdge.Kind.SYNCHRONIZATION; 

                    }
                    break;
                case 25 :
                    // /Users/jgf/Documents/Projects/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDGVertex_.g:366:5: 'HE'
                    {
                    match(input,93,FOLLOW_93_in_edge_kind1604); 
                     kind = SDGEdge.Kind.HELP; 

                    }
                    break;
                case 26 :
                    // /Users/jgf/Documents/Projects/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDGVertex_.g:367:5: 'FD'
                    {
                    match(input,94,FOLLOW_94_in_edge_kind1612); 
                     kind = SDGEdge.Kind.FOLDED; 

                    }
                    break;
                case 27 :
                    // /Users/jgf/Documents/Projects/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDGVertex_.g:368:5: 'FI'
                    {
                    match(input,95,FOLLOW_95_in_edge_kind1620); 
                     kind = SDGEdge.Kind.FOLD_INCLUDE; 

                    }
                    break;
                case 28 :
                    // /Users/jgf/Documents/Projects/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDGVertex_.g:370:5: 'RY'
                    {
                    match(input,96,FOLLOW_96_in_edge_kind1629); 
                     kind = SDGEdge.Kind.READY_DEP; 

                    }
                    break;
                case 29 :
                    // /Users/jgf/Documents/Projects/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDGVertex_.g:371:5: 'JF'
                    {
                    match(input,97,FOLLOW_97_in_edge_kind1637); 
                     kind = SDGEdge.Kind.JUMP_FLOW; 

                    }
                    break;
                case 30 :
                    // /Users/jgf/Documents/Projects/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDGVertex_.g:372:5: 'SP'
                    {
                    match(input,98,FOLLOW_98_in_edge_kind1645); 
                     kind = SDGEdge.Kind.SUMMARY; 

                    }
                    break;
                case 31 :
                    // /Users/jgf/Documents/Projects/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDGVertex_.g:373:5: 'VD'
                    {
                    match(input,99,FOLLOW_99_in_edge_kind1653); 
                     kind = SDGEdge.Kind.DATA_DEP_EXPR_VALUE; 

                    }
                    break;
                case 32 :
                    // /Users/jgf/Documents/Projects/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDGVertex_.g:374:5: 'RD'
                    {
                    match(input,100,FOLLOW_100_in_edge_kind1661); 
                     kind = SDGEdge.Kind.DATA_DEP_EXPR_REFERENCE; 

                    }
                    break;
                case 33 :
                    // /Users/jgf/Documents/Projects/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDGVertex_.g:375:5: 'JD'
                    {
                    match(input,101,FOLLOW_101_in_edge_kind1669); 
                     kind = SDGEdge.Kind.JUMP_DEP; 

                    }
                    break;

            }
        }
        catch (RecognitionException re) {
            reportError(re);
            recover(input,re);
        }
        finally {
        }
        return kind;
    }
    // $ANTLR end "edge_kind"


    // $ANTLR start "mayNegNumber"
    // /Users/jgf/Documents/Projects/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDGVertex_.g:378:9: private mayNegNumber returns [int nr] : ( '-' n= number | n= number );
    public final int mayNegNumber() throws RecognitionException {
        int nr = 0;

        int n = 0;


        try {
            // /Users/jgf/Documents/Projects/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDGVertex_.g:379:3: ( '-' n= number | n= number )
            int alt10=2;
            int LA10_0 = input.LA(1);

            if ( (LA10_0==36) ) {
                alt10=1;
            }
            else if ( (LA10_0==NUMBER) ) {
                alt10=2;
            }
            else {
                NoViableAltException nvae =
                    new NoViableAltException("", 10, 0, input);

                throw nvae;
            }
            switch (alt10) {
                case 1 :
                    // /Users/jgf/Documents/Projects/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDGVertex_.g:379:5: '-' n= number
                    {
                    match(input,36,FOLLOW_36_in_mayNegNumber1690); 
                    pushFollow(FOLLOW_number_in_mayNegNumber1694);
                    n=number();

                    state._fsp--;

                     nr = -n; 

                    }
                    break;
                case 2 :
                    // /Users/jgf/Documents/Projects/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDGVertex_.g:380:5: n= number
                    {
                    pushFollow(FOLLOW_number_in_mayNegNumber1704);
                    n=number();

                    state._fsp--;

                     nr = n; 

                    }
                    break;

            }
        }
        catch (RecognitionException re) {
            reportError(re);
            recover(input,re);
        }
        finally {
        }
        return nr;
    }
    // $ANTLR end "mayNegNumber"


    // $ANTLR start "number"
    // /Users/jgf/Documents/Projects/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDGVertex_.g:383:9: private number returns [int nr] : n= NUMBER ;
    public final int number() throws RecognitionException {
        int nr = 0;

        Token n=null;

        try {
            // /Users/jgf/Documents/Projects/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDGVertex_.g:384:3: (n= NUMBER )
            // /Users/jgf/Documents/Projects/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDGVertex_.g:384:5: n= NUMBER
            {
            n=(Token)match(input,NUMBER,FOLLOW_NUMBER_in_number1727); 
             nr = Integer.parseInt(n.getText()); 

            }

        }
        catch (RecognitionException re) {
            reportError(re);
            recover(input,re);
        }
        finally {
        }
        return nr;
    }
    // $ANTLR end "number"


    // $ANTLR start "string"
    // /Users/jgf/Documents/Projects/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDGVertex_.g:387:9: private string returns [String str] : s= STRING ;
    public final String string() throws RecognitionException {
        String str = null;

        Token s=null;

        try {
            // /Users/jgf/Documents/Projects/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDGVertex_.g:388:3: (s= STRING )
            // /Users/jgf/Documents/Projects/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDGVertex_.g:388:5: s= STRING
            {
            s=(Token)match(input,STRING,FOLLOW_STRING_in_string1750); 
             str = s.getText(); str = str.substring(1, str.length() - 1); 

            }

        }
        catch (RecognitionException re) {
            reportError(re);
            recover(input,re);
        }
        finally {
        }
        return str;
    }
    // $ANTLR end "string"


    // $ANTLR start "bool"
    // /Users/jgf/Documents/Projects/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDGVertex_.g:391:9: private bool returns [boolean b] : ( 'true' | 'false' );
    public final boolean bool() throws RecognitionException {
        boolean b = false;

        try {
            // /Users/jgf/Documents/Projects/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDGVertex_.g:392:3: ( 'true' | 'false' )
            int alt11=2;
            int LA11_0 = input.LA(1);

            if ( (LA11_0==102) ) {
                alt11=1;
            }
            else if ( (LA11_0==103) ) {
                alt11=2;
            }
            else {
                NoViableAltException nvae =
                    new NoViableAltException("", 11, 0, input);

                throw nvae;
            }
            switch (alt11) {
                case 1 :
                    // /Users/jgf/Documents/Projects/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDGVertex_.g:392:5: 'true'
                    {
                    match(input,102,FOLLOW_102_in_bool1771); 
                     b = true; 

                    }
                    break;
                case 2 :
                    // /Users/jgf/Documents/Projects/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDGVertex_.g:393:5: 'false'
                    {
                    match(input,103,FOLLOW_103_in_bool1780); 
                     b = false; 

                    }
                    break;

            }
        }
        catch (RecognitionException re) {
            reportError(re);
            recover(input,re);
        }
        finally {
        }
        return b;
    }
    // $ANTLR end "bool"

    // Delegated rules


    protected DFA1 dfa1 = new DFA1(this);
    protected DFA2 dfa2 = new DFA2(this);
    protected DFA3 dfa3 = new DFA3(this);
    protected DFA6 dfa6 = new DFA6(this);
    protected DFA7 dfa7 = new DFA7(this);
    protected DFA9 dfa9 = new DFA9(this);
    static final String DFA1_eotS =
        "\15\uffff";
    static final String DFA1_eofS =
        "\15\uffff";
    static final String DFA1_minS =
        "\1\11\14\uffff";
    static final String DFA1_maxS =
        "\1\24\14\uffff";
    static final String DFA1_acceptS =
        "\1\uffff\1\1\1\2\1\3\1\4\1\5\1\6\1\7\1\10\1\11\1\12\1\13\1\14";
    static final String DFA1_specialS =
        "\15\uffff}>";
    static final String[] DFA1_transitionS = {
            "\1\1\1\2\1\3\1\4\1\5\1\6\1\7\1\10\1\11\1\12\1\13\1\14",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            ""
    };

    static final short[] DFA1_eot = DFA.unpackEncodedString(DFA1_eotS);
    static final short[] DFA1_eof = DFA.unpackEncodedString(DFA1_eofS);
    static final char[] DFA1_min = DFA.unpackEncodedStringToUnsignedChars(DFA1_minS);
    static final char[] DFA1_max = DFA.unpackEncodedStringToUnsignedChars(DFA1_maxS);
    static final short[] DFA1_accept = DFA.unpackEncodedString(DFA1_acceptS);
    static final short[] DFA1_special = DFA.unpackEncodedString(DFA1_specialS);
    static final short[][] DFA1_transition;

    static {
        int numStates = DFA1_transitionS.length;
        DFA1_transition = new short[numStates][];
        for (int i=0; i<numStates; i++) {
            DFA1_transition[i] = DFA.unpackEncodedString(DFA1_transitionS[i]);
        }
    }

    class DFA1 extends DFA {

        public DFA1(BaseRecognizer recognizer) {
            this.recognizer = recognizer;
            this.decisionNumber = 1;
            this.eot = DFA1_eot;
            this.eof = DFA1_eof;
            this.min = DFA1_min;
            this.max = DFA1_max;
            this.accept = DFA1_accept;
            this.special = DFA1_special;
            this.transition = DFA1_transition;
        }
        public String getDescription() {
            return "238:9: private node_kind returns [SDGNode.Kind kind] : ( 'NORM' | 'PRED' | 'EXPR' | 'ENTR' | 'CALL' | 'ACTI' | 'ACTO' | 'FRMI' | 'FRMO' | 'EXIT' | 'SYNC' | 'FOLD' );";
        }
    }
    static final String DFA2_eotS =
        "\57\uffff";
    static final String DFA2_eofS =
        "\57\uffff";
    static final String DFA2_minS =
        "\1\10\56\uffff";
    static final String DFA2_maxS =
        "\1\145\56\uffff";
    static final String DFA2_acceptS =
        "\1\uffff\1\2\41\uffff\1\1\13\uffff";
    static final String DFA2_specialS =
        "\57\uffff}>";
    static final String[] DFA2_transitionS = {
            "\1\1\15\uffff\14\43\43\uffff\41\1",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            ""
    };

    static final short[] DFA2_eot = DFA.unpackEncodedString(DFA2_eotS);
    static final short[] DFA2_eof = DFA.unpackEncodedString(DFA2_eofS);
    static final char[] DFA2_min = DFA.unpackEncodedStringToUnsignedChars(DFA2_minS);
    static final char[] DFA2_max = DFA.unpackEncodedStringToUnsignedChars(DFA2_maxS);
    static final short[] DFA2_accept = DFA.unpackEncodedString(DFA2_acceptS);
    static final short[] DFA2_special = DFA.unpackEncodedString(DFA2_specialS);
    static final short[][] DFA2_transition;

    static {
        int numStates = DFA2_transitionS.length;
        DFA2_transition = new short[numStates][];
        for (int i=0; i<numStates; i++) {
            DFA2_transition[i] = DFA.unpackEncodedString(DFA2_transitionS[i]);
        }
    }

    class DFA2 extends DFA {

        public DFA2(BaseRecognizer recognizer) {
            this.recognizer = recognizer;
            this.decisionNumber = 2;
            this.eot = DFA2_eot;
            this.eof = DFA2_eof;
            this.min = DFA2_min;
            this.max = DFA2_max;
            this.accept = DFA2_accept;
            this.special = DFA2_special;
            this.transition = DFA2_transition;
        }
        public String getDescription() {
            return "()* loopback of 254:5: ( node_attr[node] ';' )*";
        }
    }
    static final String DFA3_eotS =
        "\17\uffff";
    static final String DFA3_eofS =
        "\17\uffff";
    static final String DFA3_minS =
        "\1\26\2\uffff\1\4\13\uffff";
    static final String DFA3_maxS =
        "\1\41\2\uffff\1\5\13\uffff";
    static final String DFA3_acceptS =
        "\1\uffff\1\1\1\2\1\uffff\1\4\1\5\1\6\1\7\1\10\1\11\1\12\1\13\1\14"+
        "\1\15\1\3";
    static final String DFA3_specialS =
        "\17\uffff}>";
    static final String[] DFA3_transitionS = {
            "\1\1\1\2\1\3\1\4\1\5\1\6\1\7\1\10\1\11\1\12\1\13\1\14",
            "",
            "",
            "\1\16\1\15",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            ""
    };

    static final short[] DFA3_eot = DFA.unpackEncodedString(DFA3_eotS);
    static final short[] DFA3_eof = DFA.unpackEncodedString(DFA3_eofS);
    static final char[] DFA3_min = DFA.unpackEncodedStringToUnsignedChars(DFA3_minS);
    static final char[] DFA3_max = DFA.unpackEncodedStringToUnsignedChars(DFA3_maxS);
    static final short[] DFA3_accept = DFA.unpackEncodedString(DFA3_acceptS);
    static final short[] DFA3_special = DFA.unpackEncodedString(DFA3_specialS);
    static final short[][] DFA3_transition;

    static {
        int numStates = DFA3_transitionS.length;
        DFA3_transition = new short[numStates][];
        for (int i=0; i<numStates; i++) {
            DFA3_transition[i] = DFA.unpackEncodedString(DFA3_transitionS[i]);
        }
    }

    class DFA3 extends DFA {

        public DFA3(BaseRecognizer recognizer) {
            this.recognizer = recognizer;
            this.decisionNumber = 3;
            this.eot = DFA3_eot;
            this.eof = DFA3_eof;
            this.min = DFA3_min;
            this.max = DFA3_max;
            this.accept = DFA3_accept;
            this.special = DFA3_special;
            this.transition = DFA3_transition;
        }
        public String getDescription() {
            return "257:9: private node_attr[SDGNodeStub node] : ( 'S' spos= node_source | 'B' bpos= node_bytecode | 'U' number | 'P' procId= number | 'O' op= node_oper | 'V' val= string | 'T' type= string | 'Z' tn= may_neg_num_set | 'N' | 'C' cl= string | 'A' al= pos_num_set | 'D' ds= pos_num_set | 'U' uct= string );";
        }
    }
    static final String DFA6_eotS =
        "\41\uffff";
    static final String DFA6_eofS =
        "\41\uffff";
    static final String DFA6_minS =
        "\1\45\40\uffff";
    static final String DFA6_maxS =
        "\1\104\40\uffff";
    static final String DFA6_acceptS =
        "\1\uffff\1\1\1\2\1\3\1\4\1\5\1\6\1\7\1\10\1\11\1\12\1\13\1\14\1"+
        "\15\1\16\1\17\1\20\1\21\1\22\1\23\1\24\1\25\1\26\1\27\1\30\1\31"+
        "\1\32\1\33\1\34\1\35\1\36\1\37\1\40";
    static final String DFA6_specialS =
        "\41\uffff}>";
    static final String[] DFA6_transitionS = {
            "\1\1\1\2\1\3\1\4\1\5\1\6\1\7\1\10\1\11\1\12\1\13\1\14\1\15\1"+
            "\16\1\17\1\20\1\21\1\22\1\23\1\24\1\25\1\26\1\27\1\30\1\31\1"+
            "\32\1\33\1\34\1\35\1\36\1\37\1\40",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            ""
    };

    static final short[] DFA6_eot = DFA.unpackEncodedString(DFA6_eotS);
    static final short[] DFA6_eof = DFA.unpackEncodedString(DFA6_eofS);
    static final char[] DFA6_min = DFA.unpackEncodedStringToUnsignedChars(DFA6_minS);
    static final char[] DFA6_max = DFA.unpackEncodedStringToUnsignedChars(DFA6_maxS);
    static final short[] DFA6_accept = DFA.unpackEncodedString(DFA6_acceptS);
    static final short[] DFA6_special = DFA.unpackEncodedString(DFA6_specialS);
    static final short[][] DFA6_transition;

    static {
        int numStates = DFA6_transitionS.length;
        DFA6_transition = new short[numStates][];
        for (int i=0; i<numStates; i++) {
            DFA6_transition[i] = DFA.unpackEncodedString(DFA6_transitionS[i]);
        }
    }

    class DFA6 extends DFA {

        public DFA6(BaseRecognizer recognizer) {
            this.recognizer = recognizer;
            this.decisionNumber = 6;
            this.eot = DFA6_eot;
            this.eof = DFA6_eof;
            this.min = DFA6_min;
            this.max = DFA6_max;
            this.accept = DFA6_accept;
            this.special = DFA6_special;
            this.transition = DFA6_transition;
        }
        public String getDescription() {
            return "290:9: private node_oper returns [SDGNode.Operation op] : ( 'empty' | 'intconst' | 'floatconst' | 'charconst' | 'stringconst' | 'functionconst' | 'shortcut' | 'question' | 'binary' | 'unary' | 'derefer' | 'refer' | 'array' | 'select' | 'reference' | 'declaration' | 'modify' | 'modassign' | 'assign' | 'IF' | 'loop' | 'jump' | 'compound' | 'call' | 'entry' | 'exit' | 'form-in' | 'form-ellip' | 'form-out' | 'act-in' | 'act-out' | 'monitor' );";
        }
    }
    static final String DFA7_eotS =
        "\43\uffff";
    static final String DFA7_eofS =
        "\43\uffff";
    static final String DFA7_minS =
        "\1\10\42\uffff";
    static final String DFA7_maxS =
        "\1\145\42\uffff";
    static final String DFA7_acceptS =
        "\1\uffff\1\2\1\1\40\uffff";
    static final String DFA7_specialS =
        "\43\uffff}>";
    static final String[] DFA7_transitionS = {
            "\1\1\74\uffff\41\2",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            ""
    };

    static final short[] DFA7_eot = DFA.unpackEncodedString(DFA7_eotS);
    static final short[] DFA7_eof = DFA.unpackEncodedString(DFA7_eofS);
    static final char[] DFA7_min = DFA.unpackEncodedStringToUnsignedChars(DFA7_minS);
    static final char[] DFA7_max = DFA.unpackEncodedStringToUnsignedChars(DFA7_maxS);
    static final short[] DFA7_accept = DFA.unpackEncodedString(DFA7_acceptS);
    static final short[] DFA7_special = DFA.unpackEncodedString(DFA7_specialS);
    static final short[][] DFA7_transition;

    static {
        int numStates = DFA7_transitionS.length;
        DFA7_transition = new short[numStates][];
        for (int i=0; i<numStates; i++) {
            DFA7_transition[i] = DFA.unpackEncodedString(DFA7_transitionS[i]);
        }
    }

    class DFA7 extends DFA {

        public DFA7(BaseRecognizer recognizer) {
            this.recognizer = recognizer;
            this.decisionNumber = 7;
            this.eot = DFA7_eot;
            this.eof = DFA7_eof;
            this.min = DFA7_min;
            this.max = DFA7_max;
            this.accept = DFA7_accept;
            this.special = DFA7_special;
            this.transition = DFA7_transition;
        }
        public String getDescription() {
            return "()* loopback of 326:5: (e= edge ';' )*";
        }
    }
    static final String DFA9_eotS =
        "\42\uffff";
    static final String DFA9_eofS =
        "\42\uffff";
    static final String DFA9_minS =
        "\1\105\41\uffff";
    static final String DFA9_maxS =
        "\1\145\41\uffff";
    static final String DFA9_acceptS =
        "\1\uffff\1\1\1\2\1\3\1\4\1\5\1\6\1\7\1\10\1\11\1\12\1\13\1\14\1"+
        "\15\1\16\1\17\1\20\1\21\1\22\1\23\1\24\1\25\1\26\1\27\1\30\1\31"+
        "\1\32\1\33\1\34\1\35\1\36\1\37\1\40\1\41";
    static final String DFA9_specialS =
        "\42\uffff}>";
    static final String[] DFA9_transitionS = {
            "\1\1\1\2\1\3\1\4\1\5\1\6\1\7\1\10\1\11\1\12\1\13\1\14\1\15\1"+
            "\16\1\17\1\20\1\21\1\22\1\23\1\24\1\25\1\26\1\27\1\30\1\31\1"+
            "\32\1\33\1\34\1\35\1\36\1\37\1\40\1\41",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            ""
    };

    static final short[] DFA9_eot = DFA.unpackEncodedString(DFA9_eotS);
    static final short[] DFA9_eof = DFA.unpackEncodedString(DFA9_eofS);
    static final char[] DFA9_min = DFA.unpackEncodedStringToUnsignedChars(DFA9_minS);
    static final char[] DFA9_max = DFA.unpackEncodedStringToUnsignedChars(DFA9_maxS);
    static final short[] DFA9_accept = DFA.unpackEncodedString(DFA9_acceptS);
    static final short[] DFA9_special = DFA.unpackEncodedString(DFA9_specialS);
    static final short[][] DFA9_transition;

    static {
        int numStates = DFA9_transitionS.length;
        DFA9_transition = new short[numStates][];
        for (int i=0; i<numStates; i++) {
            DFA9_transition[i] = DFA.unpackEncodedString(DFA9_transitionS[i]);
        }
    }

    class DFA9 extends DFA {

        public DFA9(BaseRecognizer recognizer) {
            this.recognizer = recognizer;
            this.decisionNumber = 9;
            this.eot = DFA9_eot;
            this.eof = DFA9_eof;
            this.min = DFA9_min;
            this.max = DFA9_max;
            this.accept = DFA9_accept;
            this.special = DFA9_special;
            this.transition = DFA9_transition;
        }
        public String getDescription() {
            return "333:9: private edge_kind returns [SDGEdge.Kind kind] : ( 'DD' | 'DH' | 'DA' | 'CD' | 'CE' | 'UN' | 'CF' | 'NF' | 'RF' | 'CC' | 'CL' | 'PI' | 'PO' | 'SU' | 'SH' | 'SF' | 'PS' | 'FORK' | 'FORK_IN' | 'FORK_OUT' | 'JOIN' | 'ID' | 'IW' | 'SD' | 'HE' | 'FD' | 'FI' | 'RY' | 'JF' | 'SP' | 'VD' | 'RD' | 'JD' );";
        }
    }
 

    public static final BitSet FOLLOW_node_kind_in_node73 = new BitSet(new long[]{0x0000001000000010L});
    public static final BitSet FOLLOW_mayNegNumber_in_node77 = new BitSet(new long[]{0x0000000000000080L});
    public static final BitSet FOLLOW_7_in_node86 = new BitSet(new long[]{0x00000003FFC00100L,0x0000003FFFFFFFE0L});
    public static final BitSet FOLLOW_node_attributes_in_node95 = new BitSet(new long[]{0x0000000000000100L,0x0000003FFFFFFFE0L});
    public static final BitSet FOLLOW_node_edges_in_node105 = new BitSet(new long[]{0x0000000000000100L});
    public static final BitSet FOLLOW_8_in_node112 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_9_in_node_kind131 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_10_in_node_kind139 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_11_in_node_kind147 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_12_in_node_kind155 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_13_in_node_kind163 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_14_in_node_kind171 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_15_in_node_kind179 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_16_in_node_kind187 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_17_in_node_kind195 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_18_in_node_kind203 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_19_in_node_kind211 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_20_in_node_kind219 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_node_attr_in_node_attributes238 = new BitSet(new long[]{0x0000000000200000L});
    public static final BitSet FOLLOW_21_in_node_attributes241 = new BitSet(new long[]{0x00000003FFC00002L});
    public static final BitSet FOLLOW_22_in_node_attr259 = new BitSet(new long[]{0x0000000000000020L});
    public static final BitSet FOLLOW_node_source_in_node_attr263 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_23_in_node_attr275 = new BitSet(new long[]{0x0000000000000020L});
    public static final BitSet FOLLOW_node_bytecode_in_node_attr279 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_24_in_node_attr290 = new BitSet(new long[]{0x0000001000000010L});
    public static final BitSet FOLLOW_number_in_node_attr292 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_25_in_node_attr342 = new BitSet(new long[]{0x0000001000000010L});
    public static final BitSet FOLLOW_number_in_node_attr346 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_26_in_node_attr365 = new BitSet(new long[]{0xFFFFFFE000000000L,0x000000000000001FL});
    public static final BitSet FOLLOW_node_oper_in_node_attr369 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_27_in_node_attr397 = new BitSet(new long[]{0x0000000000000020L});
    public static final BitSet FOLLOW_string_in_node_attr401 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_28_in_node_attr429 = new BitSet(new long[]{0x0000000000000020L});
    public static final BitSet FOLLOW_string_in_node_attr433 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_29_in_node_attr458 = new BitSet(new long[]{0x0000001000000010L});
    public static final BitSet FOLLOW_may_neg_num_set_in_node_attr462 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_30_in_node_attr476 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_31_in_node_attr510 = new BitSet(new long[]{0x0000000000000020L});
    public static final BitSet FOLLOW_string_in_node_attr514 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_32_in_node_attr536 = new BitSet(new long[]{0x0000001000000010L});
    public static final BitSet FOLLOW_pos_num_set_in_node_attr540 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_33_in_node_attr558 = new BitSet(new long[]{0x0000001000000010L});
    public static final BitSet FOLLOW_pos_num_set_in_node_attr562 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_24_in_node_attr579 = new BitSet(new long[]{0x0000000000000020L});
    public static final BitSet FOLLOW_string_in_node_attr583 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_number_in_pos_num_set616 = new BitSet(new long[]{0x0000000400000002L});
    public static final BitSet FOLLOW_34_in_pos_num_set621 = new BitSet(new long[]{0x0000001000000010L});
    public static final BitSet FOLLOW_number_in_pos_num_set625 = new BitSet(new long[]{0x0000000400000002L});
    public static final BitSet FOLLOW_mayNegNumber_in_may_neg_num_set651 = new BitSet(new long[]{0x0000000400000002L});
    public static final BitSet FOLLOW_34_in_may_neg_num_set656 = new BitSet(new long[]{0x0000001000000010L});
    public static final BitSet FOLLOW_mayNegNumber_in_may_neg_num_set660 = new BitSet(new long[]{0x0000000400000002L});
    public static final BitSet FOLLOW_string_in_node_source686 = new BitSet(new long[]{0x0000000800000000L});
    public static final BitSet FOLLOW_35_in_node_source688 = new BitSet(new long[]{0x0000001000000010L});
    public static final BitSet FOLLOW_number_in_node_source692 = new BitSet(new long[]{0x0000000400000000L});
    public static final BitSet FOLLOW_34_in_node_source694 = new BitSet(new long[]{0x0000001000000010L});
    public static final BitSet FOLLOW_number_in_node_source698 = new BitSet(new long[]{0x0000001000000000L});
    public static final BitSet FOLLOW_36_in_node_source700 = new BitSet(new long[]{0x0000001000000010L});
    public static final BitSet FOLLOW_number_in_node_source704 = new BitSet(new long[]{0x0000000400000000L});
    public static final BitSet FOLLOW_34_in_node_source706 = new BitSet(new long[]{0x0000001000000010L});
    public static final BitSet FOLLOW_number_in_node_source710 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_string_in_node_bytecode741 = new BitSet(new long[]{0x0000000800000000L});
    public static final BitSet FOLLOW_35_in_node_bytecode743 = new BitSet(new long[]{0x0000001000000010L});
    public static final BitSet FOLLOW_mayNegNumber_in_node_bytecode747 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_37_in_node_oper768 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_38_in_node_oper785 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_39_in_node_oper799 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_40_in_node_oper811 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_41_in_node_oper824 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_42_in_node_oper835 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_43_in_node_oper844 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_44_in_node_oper858 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_45_in_node_oper872 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_46_in_node_oper888 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_47_in_node_oper905 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_48_in_node_oper920 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_49_in_node_oper937 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_50_in_node_oper954 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_51_in_node_oper970 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_52_in_node_oper983 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_53_in_node_oper994 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_54_in_node_oper1010 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_55_in_node_oper1023 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_56_in_node_oper1039 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_57_in_node_oper1059 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_58_in_node_oper1077 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_59_in_node_oper1095 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_60_in_node_oper1109 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_61_in_node_oper1127 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_62_in_node_oper1144 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_63_in_node_oper1162 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_64_in_node_oper1177 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_65_in_node_oper1189 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_66_in_node_oper1203 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_67_in_node_oper1219 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_68_in_node_oper1234 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_edge_in_node_edges1262 = new BitSet(new long[]{0x0000000000200000L});
    public static final BitSet FOLLOW_21_in_node_edges1264 = new BitSet(new long[]{0x0000000000000002L,0x0000003FFFFFFFE0L});
    public static final BitSet FOLLOW_edge_kind_in_edge1289 = new BitSet(new long[]{0x0000001000000010L});
    public static final BitSet FOLLOW_number_in_edge1293 = new BitSet(new long[]{0x0000000800000002L});
    public static final BitSet FOLLOW_35_in_edge1298 = new BitSet(new long[]{0x0000000000000020L});
    public static final BitSet FOLLOW_string_in_edge1302 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_69_in_edge_kind1327 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_70_in_edge_kind1347 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_71_in_edge_kind1366 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_72_in_edge_kind1385 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_73_in_edge_kind1397 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_74_in_edge_kind1409 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_75_in_edge_kind1420 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_76_in_edge_kind1436 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_77_in_edge_kind1457 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_78_in_edge_kind1480 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_79_in_edge_kind1488 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_80_in_edge_kind1496 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_81_in_edge_kind1504 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_82_in_edge_kind1513 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_83_in_edge_kind1521 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_84_in_edge_kind1529 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_85_in_edge_kind1538 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_86_in_edge_kind1547 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_87_in_edge_kind1555 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_88_in_edge_kind1563 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_89_in_edge_kind1571 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_90_in_edge_kind1579 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_91_in_edge_kind1587 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_92_in_edge_kind1595 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_93_in_edge_kind1604 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_94_in_edge_kind1612 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_95_in_edge_kind1620 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_96_in_edge_kind1629 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_97_in_edge_kind1637 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_98_in_edge_kind1645 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_99_in_edge_kind1653 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_100_in_edge_kind1661 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_101_in_edge_kind1669 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_36_in_mayNegNumber1690 = new BitSet(new long[]{0x0000001000000010L});
    public static final BitSet FOLLOW_number_in_mayNegNumber1694 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_number_in_mayNegNumber1704 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_NUMBER_in_number1727 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_STRING_in_string1750 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_102_in_bool1771 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_103_in_bool1780 = new BitSet(new long[]{0x0000000000000002L});

}