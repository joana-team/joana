// $ANTLR 3.1.3 Mar 17, 2009 19:23:44 /Users/jgf/Documents/Projects/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g 2014-06-23 14:33:56
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
import edu.kit.joana.ifc.sdg.graph.slicer.graph.threads.ThreadsInformation;
import edu.kit.joana.ifc.sdg.graph.slicer.graph.threads.ThreadsInformation.ThreadInstance;


import org.antlr.runtime.*;
import java.util.List;

/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
public class SDG_Parser extends Parser {
    public static final String[] tokenNames = new String[] {
        "<invalid>", "<EOR>", "<DOWN>", "<UP>", "NUMBER", "STRING", "WHITESPACE", "'{'", "'JComp'", "'}'", "'Thread'", "'Entry'", "';'", "'Exit'", "'Fork'", "'Join'", "'Context'", "'Dynamic'", "'null'", "'['", "','", "']'", "'SDG'", "'v'", "'NORM'", "'PRED'", "'EXPR'", "'ENTR'", "'CALL'", "'ACTI'", "'ACTO'", "'FRMI'", "'FRMO'", "'EXIT'", "'SYNC'", "'FOLD'", "'S'", "'B'", "'U'", "'P'", "'O'", "'V'", "'T'", "'Z'", "'N'", "'C'", "'A'", "'D'", "':'", "'-'", "'empty'", "'intconst'", "'floatconst'", "'charconst'", "'stringconst'", "'functionconst'", "'shortcut'", "'question'", "'binary'", "'unary'", "'derefer'", "'refer'", "'array'", "'select'", "'reference'", "'declaration'", "'modify'", "'modassign'", "'assign'", "'IF'", "'loop'", "'jump'", "'compound'", "'call'", "'entry'", "'exit'", "'form-in'", "'form-ellip'", "'form-out'", "'act-in'", "'act-out'", "'monitor'", "'DD'", "'DH'", "'DA'", "'CD'", "'CE'", "'UN'", "'CF'", "'NF'", "'RF'", "'CC'", "'CL'", "'PI'", "'PO'", "'SU'", "'SH'", "'SF'", "'PS'", "'FORK'", "'FORK_IN'", "'FORK_OUT'", "'JOIN'", "'ID'", "'IW'", "'SD'", "'HE'", "'FD'", "'FI'", "'RY'", "'JF'", "'SP'", "'VD'", "'RD'", "'JD'", "'true'", "'false'"
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
    public static final int T__116=116;
    public static final int T__114=114;
    public static final int T__115=115;
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
    public static final int T__107=107;
    public static final int T__108=108;
    public static final int T__109=109;
    public static final int T__103=103;
    public static final int T__59=59;
    public static final int T__104=104;
    public static final int T__105=105;
    public static final int T__106=106;
    public static final int T__111=111;
    public static final int T__110=110;
    public static final int T__113=113;
    public static final int T__112=112;
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


        public SDG_Parser(TokenStream input) {
            this(input, new RecognizerSharedState());
        }
        public SDG_Parser(TokenStream input, RecognizerSharedState state) {
            super(input, state);
             
        }
        

    public String[] getTokenNames() { return SDG_Parser.tokenNames; }
    public String getGrammarFileName() { return "/Users/jgf/Documents/Projects/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g"; }


      private SDGNode.NodeFactory nodeFact = new SDGNode.SDGNodeFactory();
      
      public void setNodeFactory(SDGNode.NodeFactory nodeFact) {
        this.nodeFact = nodeFact;
      }

      public void createNodesAndEdges(final SDG sdg, final List<SDGNodeStub> stubs) {
        // 1. create all nodes
        for (SDGNodeStub n : stubs) {
          final SDGNode node = n.createNode(nodeFact);
          sdg.addVertex(node);
        }
        
        // 2. create all edges
        for (SDGNodeStub n : stubs) {
          n.createEdges(sdg);
        }
      } 

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

      static class SDGHeader {
        private int version;
        private String name;
        
        private SDGHeader(int version, String name) {
          this.version = version;
          this.name = name;
        }
        
        private SDG createSDG() {
          SDG sdg = (name == null ? new SDG() : new SDG(name));
          return sdg;
        }
        
        public String toString() {
          return "SDG of " + name + " (v" + version + ")";
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
      
        private SDGNode createNode(final SDGNode.NodeFactory nf) {
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
        
        private void createEdges(final SDG sdg) {
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
      
        private SourcePos(final String filename, final int startRow, final int startColumn,
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
        
        private ByteCodePos(final String name, final int index) {
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



    // $ANTLR start "sdg_file"
    // /Users/jgf/Documents/Projects/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:270:1: sdg_file returns [SDG sdg] : head= sdg_header '{' ( 'JComp' )? nl= node_list (ti= thread_info[sdg] )? '}' ;
    public final SDG sdg_file() throws RecognitionException {
        SDG sdg = null;

        SDGHeader head = null;

        List<SDGNodeStub> nl = null;

        ThreadsInformation ti = null;


        try {
            // /Users/jgf/Documents/Projects/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:271:3: (head= sdg_header '{' ( 'JComp' )? nl= node_list (ti= thread_info[sdg] )? '}' )
            // /Users/jgf/Documents/Projects/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:271:5: head= sdg_header '{' ( 'JComp' )? nl= node_list (ti= thread_info[sdg] )? '}'
            {
            pushFollow(FOLLOW_sdg_header_in_sdg_file73);
            head=sdg_header();

            state._fsp--;

             sdg = head.createSDG(); 
            match(input,7,FOLLOW_7_in_sdg_file89); 
            // /Users/jgf/Documents/Projects/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:273:7: ( 'JComp' )?
            int alt1=2;
            alt1 = dfa1.predict(input);
            switch (alt1) {
                case 1 :
                    // /Users/jgf/Documents/Projects/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:273:8: 'JComp'
                    {
                    match(input,8,FOLLOW_8_in_sdg_file99); 
                     sdg.setJoanaCompiler(true); 

                    }
                    break;

            }

            pushFollow(FOLLOW_node_list_in_sdg_file128);
            nl=node_list();

            state._fsp--;

             createNodesAndEdges(sdg, nl); 
            // /Users/jgf/Documents/Projects/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:275:7: (ti= thread_info[sdg] )?
            int alt2=2;
            int LA2_0 = input.LA(1);

            if ( (LA2_0==10) ) {
                alt2=1;
            }
            switch (alt2) {
                case 1 :
                    // /Users/jgf/Documents/Projects/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:275:8: ti= thread_info[sdg]
                    {
                    pushFollow(FOLLOW_thread_info_in_sdg_file150);
                    ti=thread_info(sdg);

                    state._fsp--;

                     sdg.setThreadsInfo(ti); 

                    }
                    break;

            }

            match(input,9,FOLLOW_9_in_sdg_file163); 

            }

        }
        catch (RecognitionException re) {
            reportError(re);
            recover(input,re);
        }
        finally {
        }
        return sdg;
    }
    // $ANTLR end "sdg_file"


    // $ANTLR start "thread_info"
    // /Users/jgf/Documents/Projects/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:279:9: private thread_info[SDG sdg] returns [ThreadsInformation tinfo] : (t= thread[sdg] )+ ;
    public final ThreadsInformation thread_info(SDG sdg) throws RecognitionException {
        ThreadsInformation tinfo = null;

        ThreadInstance t = null;


        try {
            // /Users/jgf/Documents/Projects/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:280:3: ( (t= thread[sdg] )+ )
            // /Users/jgf/Documents/Projects/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:280:5: (t= thread[sdg] )+
            {
             final LinkedList<ThreadInstance> tis = new LinkedList<ThreadInstance>(); 
            // /Users/jgf/Documents/Projects/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:281:5: (t= thread[sdg] )+
            int cnt3=0;
            loop3:
            do {
                int alt3=2;
                int LA3_0 = input.LA(1);

                if ( (LA3_0==10) ) {
                    alt3=1;
                }


                switch (alt3) {
            	case 1 :
            	    // /Users/jgf/Documents/Projects/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:281:6: t= thread[sdg]
            	    {
            	    pushFollow(FOLLOW_thread_in_thread_info192);
            	    t=thread(sdg);

            	    state._fsp--;

            	     tis.add(t); 

            	    }
            	    break;

            	default :
            	    if ( cnt3 >= 1 ) break loop3;
                        EarlyExitException eee =
                            new EarlyExitException(3, input);
                        throw eee;
                }
                cnt3++;
            } while (true);

             tinfo = new ThreadsInformation(tis); 

            }

        }
        catch (RecognitionException re) {
            reportError(re);
            recover(input,re);
        }
        finally {
        }
        return tinfo;
    }
    // $ANTLR end "thread_info"


    // $ANTLR start "thread"
    // /Users/jgf/Documents/Projects/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:285:9: private thread[SDG sdg] returns [ThreadInstance ti] : 'Thread' id= number '{' 'Entry' en= number ';' 'Exit' ex= number ';' 'Fork' fo= number ';' 'Join' jo= number ';' 'Context' con= context[sdg] ';' 'Dynamic' dyn= bool ';' '}' ;
    public final ThreadInstance thread(SDG sdg) throws RecognitionException {
        ThreadInstance ti = null;

        int id = 0;

        int en = 0;

        int ex = 0;

        int fo = 0;

        int jo = 0;

        LinkedList<SDGNode> con = null;

        boolean dyn = false;


        try {
            // /Users/jgf/Documents/Projects/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:286:3: ( 'Thread' id= number '{' 'Entry' en= number ';' 'Exit' ex= number ';' 'Fork' fo= number ';' 'Join' jo= number ';' 'Context' con= context[sdg] ';' 'Dynamic' dyn= bool ';' '}' )
            // /Users/jgf/Documents/Projects/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:286:5: 'Thread' id= number '{' 'Entry' en= number ';' 'Exit' ex= number ';' 'Fork' fo= number ';' 'Join' jo= number ';' 'Context' con= context[sdg] ';' 'Dynamic' dyn= bool ';' '}'
            {
            match(input,10,FOLLOW_10_in_thread224); 
            pushFollow(FOLLOW_number_in_thread228);
            id=number();

            state._fsp--;

            match(input,7,FOLLOW_7_in_thread230); 
            match(input,11,FOLLOW_11_in_thread238); 
            pushFollow(FOLLOW_number_in_thread244);
            en=number();

            state._fsp--;

            match(input,12,FOLLOW_12_in_thread246); 
            match(input,13,FOLLOW_13_in_thread254); 
            pushFollow(FOLLOW_number_in_thread261);
            ex=number();

            state._fsp--;

            match(input,12,FOLLOW_12_in_thread263); 
            match(input,14,FOLLOW_14_in_thread271); 
            pushFollow(FOLLOW_number_in_thread278);
            fo=number();

            state._fsp--;

            match(input,12,FOLLOW_12_in_thread280); 
            match(input,15,FOLLOW_15_in_thread288); 
            pushFollow(FOLLOW_number_in_thread295);
            jo=number();

            state._fsp--;

            match(input,12,FOLLOW_12_in_thread297); 
            match(input,16,FOLLOW_16_in_thread305); 
            pushFollow(FOLLOW_context_in_thread309);
            con=context(sdg);

            state._fsp--;

            match(input,12,FOLLOW_12_in_thread312); 
            match(input,17,FOLLOW_17_in_thread320); 
            pushFollow(FOLLOW_bool_in_thread324);
            dyn=bool();

            state._fsp--;

            match(input,12,FOLLOW_12_in_thread326); 
            match(input,9,FOLLOW_9_in_thread332); 

                  final SDGNode entry = sdg.getNode(en);
                  SDGNode exit = null; if (ex != 0) { exit = sdg.getNode(ex); }
                  SDGNode fork = null; if (fo != 0) { fork = sdg.getNode(fo); }
                  SDGNode join = null; if (jo != 0) { join = sdg.getNode(jo); }
                  ti = new ThreadInstance(id, entry, exit, fork, join, con, dyn);
                

            }

        }
        catch (RecognitionException re) {
            reportError(re);
            recover(input,re);
        }
        finally {
        }
        return ti;
    }
    // $ANTLR end "thread"


    // $ANTLR start "context"
    // /Users/jgf/Documents/Projects/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:303:9: private context[SDG sdg] returns [LinkedList<SDGNode> cx = new LinkedList<SDGNode>();] : ( 'null' | '[' i= mayNegNumber ( ',' i= mayNegNumber )* ']' );
    public final LinkedList<SDGNode> context(SDG sdg) throws RecognitionException {
        LinkedList<SDGNode> cx =  new LinkedList<SDGNode>();;

        int i = 0;


        try {
            // /Users/jgf/Documents/Projects/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:304:3: ( 'null' | '[' i= mayNegNumber ( ',' i= mayNegNumber )* ']' )
            int alt5=2;
            int LA5_0 = input.LA(1);

            if ( (LA5_0==18) ) {
                alt5=1;
            }
            else if ( (LA5_0==19) ) {
                alt5=2;
            }
            else {
                NoViableAltException nvae =
                    new NoViableAltException("", 5, 0, input);

                throw nvae;
            }
            switch (alt5) {
                case 1 :
                    // /Users/jgf/Documents/Projects/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:304:5: 'null'
                    {
                    match(input,18,FOLLOW_18_in_context360); 

                    }
                    break;
                case 2 :
                    // /Users/jgf/Documents/Projects/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:305:5: '[' i= mayNegNumber ( ',' i= mayNegNumber )* ']'
                    {
                    match(input,19,FOLLOW_19_in_context366); 
                    pushFollow(FOLLOW_mayNegNumber_in_context370);
                    i=mayNegNumber();

                    state._fsp--;

                     cx.add(sdg.getNode(i)); 
                    // /Users/jgf/Documents/Projects/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:305:52: ( ',' i= mayNegNumber )*
                    loop4:
                    do {
                        int alt4=2;
                        int LA4_0 = input.LA(1);

                        if ( (LA4_0==20) ) {
                            alt4=1;
                        }


                        switch (alt4) {
                    	case 1 :
                    	    // /Users/jgf/Documents/Projects/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:305:53: ',' i= mayNegNumber
                    	    {
                    	    match(input,20,FOLLOW_20_in_context375); 
                    	    pushFollow(FOLLOW_mayNegNumber_in_context379);
                    	    i=mayNegNumber();

                    	    state._fsp--;

                    	     cx.add(sdg.getNode(i)); 

                    	    }
                    	    break;

                    	default :
                    	    break loop4;
                        }
                    } while (true);

                    match(input,21,FOLLOW_21_in_context386); 

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
        return cx;
    }
    // $ANTLR end "context"


    // $ANTLR start "sdg_header"
    // /Users/jgf/Documents/Projects/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:308:9: private sdg_header returns [SDGHeader header] : 'SDG' ( 'v' n= number )? (na= string )? ;
    public final SDGHeader sdg_header() throws RecognitionException {
        SDGHeader header = null;

        int n = 0;

        String na = null;


        try {
            // /Users/jgf/Documents/Projects/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:309:3: ( 'SDG' ( 'v' n= number )? (na= string )? )
            // /Users/jgf/Documents/Projects/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:309:5: 'SDG' ( 'v' n= number )? (na= string )?
            {
            match(input,22,FOLLOW_22_in_sdg_header405); 
             int version = SDG.DEFAULT_VERSION; 
            // /Users/jgf/Documents/Projects/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:311:7: ( 'v' n= number )?
            int alt6=2;
            int LA6_0 = input.LA(1);

            if ( (LA6_0==23) ) {
                alt6=1;
            }
            switch (alt6) {
                case 1 :
                    // /Users/jgf/Documents/Projects/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:311:8: 'v' n= number
                    {
                    match(input,23,FOLLOW_23_in_sdg_header421); 
                    pushFollow(FOLLOW_number_in_sdg_header425);
                    n=number();

                    state._fsp--;

                     version = n; 

                    }
                    break;

            }

             String name = null; 
            // /Users/jgf/Documents/Projects/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:313:7: (na= string )?
            int alt7=2;
            int LA7_0 = input.LA(1);

            if ( (LA7_0==STRING) ) {
                alt7=1;
            }
            switch (alt7) {
                case 1 :
                    // /Users/jgf/Documents/Projects/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:313:8: na= string
                    {
                    pushFollow(FOLLOW_string_in_sdg_header446);
                    na=string();

                    state._fsp--;

                     name = na; 

                    }
                    break;

            }

             header = new SDGHeader(version, name); 

            }

        }
        catch (RecognitionException re) {
            reportError(re);
            recover(input,re);
        }
        finally {
        }
        return header;
    }
    // $ANTLR end "sdg_header"


    // $ANTLR start "node_list"
    // /Users/jgf/Documents/Projects/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:317:9: private node_list returns [List<SDGNodeStub> list = new LinkedList<SDGNodeStub>();] : (n= node )* ;
    public final List<SDGNodeStub> node_list() throws RecognitionException {
        List<SDGNodeStub> list =  new LinkedList<SDGNodeStub>();;

        SDGNodeStub n = null;


        try {
            // /Users/jgf/Documents/Projects/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:318:3: ( (n= node )* )
            // /Users/jgf/Documents/Projects/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:318:5: (n= node )*
            {
            // /Users/jgf/Documents/Projects/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:318:5: (n= node )*
            loop8:
            do {
                int alt8=2;
                alt8 = dfa8.predict(input);
                switch (alt8) {
            	case 1 :
            	    // /Users/jgf/Documents/Projects/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:318:6: n= node
            	    {
            	    pushFollow(FOLLOW_node_in_node_list479);
            	    n=node();

            	    state._fsp--;

            	     list.add(n); 

            	    }
            	    break;

            	default :
            	    break loop8;
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
        return list;
    }
    // $ANTLR end "node_list"


    // $ANTLR start "node"
    // /Users/jgf/Documents/Projects/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:321:9: private node returns [SDGNodeStub nstub] : k= node_kind id= mayNegNumber '{' node_attributes[nstub] node_edges[nstub] '}' ;
    public final SDGNodeStub node() throws RecognitionException {
        SDGNodeStub nstub = null;

        SDGNode.Kind k = null;

        int id = 0;


        try {
            // /Users/jgf/Documents/Projects/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:322:3: (k= node_kind id= mayNegNumber '{' node_attributes[nstub] node_edges[nstub] '}' )
            // /Users/jgf/Documents/Projects/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:322:5: k= node_kind id= mayNegNumber '{' node_attributes[nstub] node_edges[nstub] '}'
            {
            pushFollow(FOLLOW_node_kind_in_node505);
            k=node_kind();

            state._fsp--;

            pushFollow(FOLLOW_mayNegNumber_in_node509);
            id=mayNegNumber();

            state._fsp--;

             nstub = new SDGNodeStub(k, id, defaultSrcPos, defaultBcPos); 
            match(input,7,FOLLOW_7_in_node518); 
            pushFollow(FOLLOW_node_attributes_in_node527);
            node_attributes(nstub);

            state._fsp--;

            pushFollow(FOLLOW_node_edges_in_node537);
            node_edges(nstub);

            state._fsp--;

            match(input,9,FOLLOW_9_in_node544); 

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
    // /Users/jgf/Documents/Projects/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:329:9: private node_kind returns [SDGNode.Kind kind] : ( 'NORM' | 'PRED' | 'EXPR' | 'ENTR' | 'CALL' | 'ACTI' | 'ACTO' | 'FRMI' | 'FRMO' | 'EXIT' | 'SYNC' | 'FOLD' );
    public final SDGNode.Kind node_kind() throws RecognitionException {
        SDGNode.Kind kind = null;

        try {
            // /Users/jgf/Documents/Projects/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:330:3: ( 'NORM' | 'PRED' | 'EXPR' | 'ENTR' | 'CALL' | 'ACTI' | 'ACTO' | 'FRMI' | 'FRMO' | 'EXIT' | 'SYNC' | 'FOLD' )
            int alt9=12;
            alt9 = dfa9.predict(input);
            switch (alt9) {
                case 1 :
                    // /Users/jgf/Documents/Projects/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:330:5: 'NORM'
                    {
                    match(input,24,FOLLOW_24_in_node_kind563); 
                     kind = SDGNode.Kind.NORMAL; 

                    }
                    break;
                case 2 :
                    // /Users/jgf/Documents/Projects/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:331:5: 'PRED'
                    {
                    match(input,25,FOLLOW_25_in_node_kind571); 
                     kind = SDGNode.Kind.PREDICATE; 

                    }
                    break;
                case 3 :
                    // /Users/jgf/Documents/Projects/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:332:5: 'EXPR'
                    {
                    match(input,26,FOLLOW_26_in_node_kind579); 
                     kind = SDGNode.Kind.EXPRESSION; 

                    }
                    break;
                case 4 :
                    // /Users/jgf/Documents/Projects/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:333:5: 'ENTR'
                    {
                    match(input,27,FOLLOW_27_in_node_kind587); 
                     kind = SDGNode.Kind.ENTRY; 

                    }
                    break;
                case 5 :
                    // /Users/jgf/Documents/Projects/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:334:5: 'CALL'
                    {
                    match(input,28,FOLLOW_28_in_node_kind595); 
                     kind = SDGNode.Kind.CALL; 

                    }
                    break;
                case 6 :
                    // /Users/jgf/Documents/Projects/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:335:5: 'ACTI'
                    {
                    match(input,29,FOLLOW_29_in_node_kind603); 
                     kind = SDGNode.Kind.ACTUAL_IN; 

                    }
                    break;
                case 7 :
                    // /Users/jgf/Documents/Projects/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:336:5: 'ACTO'
                    {
                    match(input,30,FOLLOW_30_in_node_kind611); 
                     kind = SDGNode.Kind.ACTUAL_OUT; 

                    }
                    break;
                case 8 :
                    // /Users/jgf/Documents/Projects/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:337:5: 'FRMI'
                    {
                    match(input,31,FOLLOW_31_in_node_kind619); 
                     kind = SDGNode.Kind.FORMAL_IN; 

                    }
                    break;
                case 9 :
                    // /Users/jgf/Documents/Projects/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:338:5: 'FRMO'
                    {
                    match(input,32,FOLLOW_32_in_node_kind627); 
                     kind = SDGNode.Kind.FORMAL_OUT; 

                    }
                    break;
                case 10 :
                    // /Users/jgf/Documents/Projects/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:339:5: 'EXIT'
                    {
                    match(input,33,FOLLOW_33_in_node_kind635); 
                     kind = SDGNode.Kind.EXIT; 

                    }
                    break;
                case 11 :
                    // /Users/jgf/Documents/Projects/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:340:5: 'SYNC'
                    {
                    match(input,34,FOLLOW_34_in_node_kind643); 
                     kind = SDGNode.Kind.SYNCHRONIZATION; 

                    }
                    break;
                case 12 :
                    // /Users/jgf/Documents/Projects/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:341:5: 'FOLD'
                    {
                    match(input,35,FOLLOW_35_in_node_kind651); 
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
    // /Users/jgf/Documents/Projects/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:344:9: private node_attributes[SDGNodeStub node] : ( node_attr[node] ';' )* ;
    public final void node_attributes(SDGNodeStub node) throws RecognitionException {
        try {
            // /Users/jgf/Documents/Projects/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:345:3: ( ( node_attr[node] ';' )* )
            // /Users/jgf/Documents/Projects/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:345:5: ( node_attr[node] ';' )*
            {
            // /Users/jgf/Documents/Projects/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:345:5: ( node_attr[node] ';' )*
            loop10:
            do {
                int alt10=2;
                alt10 = dfa10.predict(input);
                switch (alt10) {
            	case 1 :
            	    // /Users/jgf/Documents/Projects/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:345:6: node_attr[node] ';'
            	    {
            	    pushFollow(FOLLOW_node_attr_in_node_attributes670);
            	    node_attr(node);

            	    state._fsp--;

            	    match(input,12,FOLLOW_12_in_node_attributes673); 

            	    }
            	    break;

            	default :
            	    break loop10;
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
    // /Users/jgf/Documents/Projects/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:348:9: private node_attr[SDGNodeStub node] : ( 'S' spos= node_source | 'B' bpos= node_bytecode | 'U' number | 'P' procId= number | 'O' op= node_oper | 'V' val= string | 'T' type= string | 'Z' tn= may_neg_num_set | 'N' | 'C' cl= string | 'A' al= pos_num_set | 'D' ds= pos_num_set | 'U' uct= string );
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
            // /Users/jgf/Documents/Projects/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:349:3: ( 'S' spos= node_source | 'B' bpos= node_bytecode | 'U' number | 'P' procId= number | 'O' op= node_oper | 'V' val= string | 'T' type= string | 'Z' tn= may_neg_num_set | 'N' | 'C' cl= string | 'A' al= pos_num_set | 'D' ds= pos_num_set | 'U' uct= string )
            int alt11=13;
            alt11 = dfa11.predict(input);
            switch (alt11) {
                case 1 :
                    // /Users/jgf/Documents/Projects/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:349:5: 'S' spos= node_source
                    {
                    match(input,36,FOLLOW_36_in_node_attr691); 
                    pushFollow(FOLLOW_node_source_in_node_attr695);
                    spos=node_source();

                    state._fsp--;

                     node.spos = spos; defaultSrcPos = spos; 

                    }
                    break;
                case 2 :
                    // /Users/jgf/Documents/Projects/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:350:5: 'B' bpos= node_bytecode
                    {
                    match(input,37,FOLLOW_37_in_node_attr707); 
                    pushFollow(FOLLOW_node_bytecode_in_node_attr711);
                    bpos=node_bytecode();

                    state._fsp--;

                     node.bpos = bpos; defaultBcPos = bpos; 

                    }
                    break;
                case 3 :
                    // /Users/jgf/Documents/Projects/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:351:5: 'U' number
                    {
                    match(input,38,FOLLOW_38_in_node_attr722); 
                    pushFollow(FOLLOW_number_in_node_attr724);
                    number();

                    state._fsp--;


                    }
                    break;
                case 4 :
                    // /Users/jgf/Documents/Projects/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:352:5: 'P' procId= number
                    {
                    match(input,39,FOLLOW_39_in_node_attr774); 
                    pushFollow(FOLLOW_number_in_node_attr778);
                    procId=number();

                    state._fsp--;

                     node.procId = procId; 

                    }
                    break;
                case 5 :
                    // /Users/jgf/Documents/Projects/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:353:5: 'O' op= node_oper
                    {
                    match(input,40,FOLLOW_40_in_node_attr797); 
                    pushFollow(FOLLOW_node_oper_in_node_attr801);
                    op=node_oper();

                    state._fsp--;

                     node.op = op; 

                    }
                    break;
                case 6 :
                    // /Users/jgf/Documents/Projects/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:354:5: 'V' val= string
                    {
                    match(input,41,FOLLOW_41_in_node_attr829); 
                    pushFollow(FOLLOW_string_in_node_attr833);
                    val=string();

                    state._fsp--;

                     node.val = val; 

                    }
                    break;
                case 7 :
                    // /Users/jgf/Documents/Projects/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:355:5: 'T' type= string
                    {
                    match(input,42,FOLLOW_42_in_node_attr861); 
                    pushFollow(FOLLOW_string_in_node_attr865);
                    type=string();

                    state._fsp--;

                     node.type = type; 

                    }
                    break;
                case 8 :
                    // /Users/jgf/Documents/Projects/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:356:5: 'Z' tn= may_neg_num_set
                    {
                    match(input,43,FOLLOW_43_in_node_attr890); 
                    pushFollow(FOLLOW_may_neg_num_set_in_node_attr894);
                    tn=may_neg_num_set();

                    state._fsp--;

                     node.threadNums = tn; 

                    }
                    break;
                case 9 :
                    // /Users/jgf/Documents/Projects/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:357:5: 'N'
                    {
                    match(input,44,FOLLOW_44_in_node_attr908); 
                     node.nonTerm = true; 

                    }
                    break;
                case 10 :
                    // /Users/jgf/Documents/Projects/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:358:5: 'C' cl= string
                    {
                    match(input,45,FOLLOW_45_in_node_attr942); 
                    pushFollow(FOLLOW_string_in_node_attr946);
                    cl=string();

                    state._fsp--;

                     node.classLoader = cl; 

                    }
                    break;
                case 11 :
                    // /Users/jgf/Documents/Projects/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:359:5: 'A' al= pos_num_set
                    {
                    match(input,46,FOLLOW_46_in_node_attr968); 
                    pushFollow(FOLLOW_pos_num_set_in_node_attr972);
                    al=pos_num_set();

                    state._fsp--;

                     node.allocSites = al; 

                    }
                    break;
                case 12 :
                    // /Users/jgf/Documents/Projects/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:360:5: 'D' ds= pos_num_set
                    {
                    match(input,47,FOLLOW_47_in_node_attr990); 
                    pushFollow(FOLLOW_pos_num_set_in_node_attr994);
                    ds=pos_num_set();

                    state._fsp--;

                     node.aliasDataSrc = ds; 

                    }
                    break;
                case 13 :
                    // /Users/jgf/Documents/Projects/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:361:5: 'U' uct= string
                    {
                    match(input,38,FOLLOW_38_in_node_attr1011); 
                    pushFollow(FOLLOW_string_in_node_attr1015);
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
    // /Users/jgf/Documents/Projects/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:364:9: private pos_num_set returns [TIntSet nums = new TIntHashSet();] : n= number ( ',' n2= number )* ;
    public final TIntSet pos_num_set() throws RecognitionException {
        TIntSet nums =  new TIntHashSet();;

        int n = 0;

        int n2 = 0;


        try {
            // /Users/jgf/Documents/Projects/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:365:3: (n= number ( ',' n2= number )* )
            // /Users/jgf/Documents/Projects/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:365:5: n= number ( ',' n2= number )*
            {
            pushFollow(FOLLOW_number_in_pos_num_set1048);
            n=number();

            state._fsp--;

             nums.add(n); 
            // /Users/jgf/Documents/Projects/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:365:31: ( ',' n2= number )*
            loop12:
            do {
                int alt12=2;
                int LA12_0 = input.LA(1);

                if ( (LA12_0==20) ) {
                    alt12=1;
                }


                switch (alt12) {
            	case 1 :
            	    // /Users/jgf/Documents/Projects/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:365:32: ',' n2= number
            	    {
            	    match(input,20,FOLLOW_20_in_pos_num_set1053); 
            	    pushFollow(FOLLOW_number_in_pos_num_set1057);
            	    n2=number();

            	    state._fsp--;

            	     nums.add(n2); 

            	    }
            	    break;

            	default :
            	    break loop12;
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
    // /Users/jgf/Documents/Projects/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:368:9: private may_neg_num_set returns [TIntSet nums = new TIntHashSet();] : n= mayNegNumber ( ',' n2= mayNegNumber )* ;
    public final TIntSet may_neg_num_set() throws RecognitionException {
        TIntSet nums =  new TIntHashSet();;

        int n = 0;

        int n2 = 0;


        try {
            // /Users/jgf/Documents/Projects/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:369:3: (n= mayNegNumber ( ',' n2= mayNegNumber )* )
            // /Users/jgf/Documents/Projects/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:369:5: n= mayNegNumber ( ',' n2= mayNegNumber )*
            {
            pushFollow(FOLLOW_mayNegNumber_in_may_neg_num_set1083);
            n=mayNegNumber();

            state._fsp--;

             nums.add(n); 
            // /Users/jgf/Documents/Projects/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:369:37: ( ',' n2= mayNegNumber )*
            loop13:
            do {
                int alt13=2;
                int LA13_0 = input.LA(1);

                if ( (LA13_0==20) ) {
                    alt13=1;
                }


                switch (alt13) {
            	case 1 :
            	    // /Users/jgf/Documents/Projects/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:369:38: ',' n2= mayNegNumber
            	    {
            	    match(input,20,FOLLOW_20_in_may_neg_num_set1088); 
            	    pushFollow(FOLLOW_mayNegNumber_in_may_neg_num_set1092);
            	    n2=mayNegNumber();

            	    state._fsp--;

            	     nums.add(n2); 

            	    }
            	    break;

            	default :
            	    break loop13;
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
    // /Users/jgf/Documents/Projects/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:372:9: private node_source returns [SourcePos spos] : filename= string ':' startRow= number ',' startColumn= number '-' endRow= number ',' endColumn= number ;
    public final SourcePos node_source() throws RecognitionException {
        SourcePos spos = null;

        String filename = null;

        int startRow = 0;

        int startColumn = 0;

        int endRow = 0;

        int endColumn = 0;


        try {
            // /Users/jgf/Documents/Projects/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:373:3: (filename= string ':' startRow= number ',' startColumn= number '-' endRow= number ',' endColumn= number )
            // /Users/jgf/Documents/Projects/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:373:5: filename= string ':' startRow= number ',' startColumn= number '-' endRow= number ',' endColumn= number
            {
            pushFollow(FOLLOW_string_in_node_source1118);
            filename=string();

            state._fsp--;

            match(input,48,FOLLOW_48_in_node_source1120); 
            pushFollow(FOLLOW_number_in_node_source1124);
            startRow=number();

            state._fsp--;

            match(input,20,FOLLOW_20_in_node_source1126); 
            pushFollow(FOLLOW_number_in_node_source1130);
            startColumn=number();

            state._fsp--;

            match(input,49,FOLLOW_49_in_node_source1132); 
            pushFollow(FOLLOW_number_in_node_source1136);
            endRow=number();

            state._fsp--;

            match(input,20,FOLLOW_20_in_node_source1138); 
            pushFollow(FOLLOW_number_in_node_source1142);
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
    // /Users/jgf/Documents/Projects/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:377:9: private node_bytecode returns [ByteCodePos bpos] : name= string ':' index= mayNegNumber ;
    public final ByteCodePos node_bytecode() throws RecognitionException {
        ByteCodePos bpos = null;

        String name = null;

        int index = 0;


        try {
            // /Users/jgf/Documents/Projects/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:378:3: (name= string ':' index= mayNegNumber )
            // /Users/jgf/Documents/Projects/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:378:5: name= string ':' index= mayNegNumber
            {
            pushFollow(FOLLOW_string_in_node_bytecode1173);
            name=string();

            state._fsp--;

            match(input,48,FOLLOW_48_in_node_bytecode1175); 
            pushFollow(FOLLOW_mayNegNumber_in_node_bytecode1179);
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
    // /Users/jgf/Documents/Projects/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:381:9: private node_oper returns [SDGNode.Operation op] : ( 'empty' | 'intconst' | 'floatconst' | 'charconst' | 'stringconst' | 'functionconst' | 'shortcut' | 'question' | 'binary' | 'unary' | 'derefer' | 'refer' | 'array' | 'select' | 'reference' | 'declaration' | 'modify' | 'modassign' | 'assign' | 'IF' | 'loop' | 'jump' | 'compound' | 'call' | 'entry' | 'exit' | 'form-in' | 'form-ellip' | 'form-out' | 'act-in' | 'act-out' | 'monitor' );
    public final SDGNode.Operation node_oper() throws RecognitionException {
        SDGNode.Operation op = null;

        try {
            // /Users/jgf/Documents/Projects/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:382:3: ( 'empty' | 'intconst' | 'floatconst' | 'charconst' | 'stringconst' | 'functionconst' | 'shortcut' | 'question' | 'binary' | 'unary' | 'derefer' | 'refer' | 'array' | 'select' | 'reference' | 'declaration' | 'modify' | 'modassign' | 'assign' | 'IF' | 'loop' | 'jump' | 'compound' | 'call' | 'entry' | 'exit' | 'form-in' | 'form-ellip' | 'form-out' | 'act-in' | 'act-out' | 'monitor' )
            int alt14=32;
            alt14 = dfa14.predict(input);
            switch (alt14) {
                case 1 :
                    // /Users/jgf/Documents/Projects/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:382:5: 'empty'
                    {
                    match(input,50,FOLLOW_50_in_node_oper1200); 
                     op = SDGNode.Operation.EMPTY; 

                    }
                    break;
                case 2 :
                    // /Users/jgf/Documents/Projects/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:383:5: 'intconst'
                    {
                    match(input,51,FOLLOW_51_in_node_oper1217); 
                     op = SDGNode.Operation.INT_CONST; 

                    }
                    break;
                case 3 :
                    // /Users/jgf/Documents/Projects/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:384:5: 'floatconst'
                    {
                    match(input,52,FOLLOW_52_in_node_oper1231); 
                     op = SDGNode.Operation.FLOAT_CONST; 

                    }
                    break;
                case 4 :
                    // /Users/jgf/Documents/Projects/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:385:5: 'charconst'
                    {
                    match(input,53,FOLLOW_53_in_node_oper1243); 
                     op = SDGNode.Operation.CHAR_CONST; 

                    }
                    break;
                case 5 :
                    // /Users/jgf/Documents/Projects/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:386:5: 'stringconst'
                    {
                    match(input,54,FOLLOW_54_in_node_oper1256); 
                     op = SDGNode.Operation.STRING_CONST; 

                    }
                    break;
                case 6 :
                    // /Users/jgf/Documents/Projects/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:387:5: 'functionconst'
                    {
                    match(input,55,FOLLOW_55_in_node_oper1267); 
                     op = SDGNode.Operation.FUNCTION_CONST; 

                    }
                    break;
                case 7 :
                    // /Users/jgf/Documents/Projects/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:388:5: 'shortcut'
                    {
                    match(input,56,FOLLOW_56_in_node_oper1276); 
                     op = SDGNode.Operation.SHORTCUT; 

                    }
                    break;
                case 8 :
                    // /Users/jgf/Documents/Projects/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:389:5: 'question'
                    {
                    match(input,57,FOLLOW_57_in_node_oper1290); 
                     op = SDGNode.Operation.QUESTION; 

                    }
                    break;
                case 9 :
                    // /Users/jgf/Documents/Projects/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:390:5: 'binary'
                    {
                    match(input,58,FOLLOW_58_in_node_oper1304); 
                     op = SDGNode.Operation.BINARY; 

                    }
                    break;
                case 10 :
                    // /Users/jgf/Documents/Projects/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:391:5: 'unary'
                    {
                    match(input,59,FOLLOW_59_in_node_oper1320); 
                     op = SDGNode.Operation.UNARY; 

                    }
                    break;
                case 11 :
                    // /Users/jgf/Documents/Projects/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:392:5: 'derefer'
                    {
                    match(input,60,FOLLOW_60_in_node_oper1337); 
                     op = SDGNode.Operation.DEREFER; 

                    }
                    break;
                case 12 :
                    // /Users/jgf/Documents/Projects/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:393:5: 'refer'
                    {
                    match(input,61,FOLLOW_61_in_node_oper1352); 
                     op = SDGNode.Operation.REFER; 

                    }
                    break;
                case 13 :
                    // /Users/jgf/Documents/Projects/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:394:5: 'array'
                    {
                    match(input,62,FOLLOW_62_in_node_oper1369); 
                     op = SDGNode.Operation.ARRAY; 

                    }
                    break;
                case 14 :
                    // /Users/jgf/Documents/Projects/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:395:5: 'select'
                    {
                    match(input,63,FOLLOW_63_in_node_oper1386); 
                     op = SDGNode.Operation.SELECT; 

                    }
                    break;
                case 15 :
                    // /Users/jgf/Documents/Projects/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:396:5: 'reference'
                    {
                    match(input,64,FOLLOW_64_in_node_oper1402); 
                     op = SDGNode.Operation.REFERENCE; 

                    }
                    break;
                case 16 :
                    // /Users/jgf/Documents/Projects/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:397:5: 'declaration'
                    {
                    match(input,65,FOLLOW_65_in_node_oper1415); 
                     op = SDGNode.Operation.DECLARATION; 

                    }
                    break;
                case 17 :
                    // /Users/jgf/Documents/Projects/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:398:5: 'modify'
                    {
                    match(input,66,FOLLOW_66_in_node_oper1426); 
                     op = SDGNode.Operation.MODIFY; 

                    }
                    break;
                case 18 :
                    // /Users/jgf/Documents/Projects/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:399:5: 'modassign'
                    {
                    match(input,67,FOLLOW_67_in_node_oper1442); 
                     op = SDGNode.Operation.MODASSIGN; 

                    }
                    break;
                case 19 :
                    // /Users/jgf/Documents/Projects/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:400:5: 'assign'
                    {
                    match(input,68,FOLLOW_68_in_node_oper1455); 
                     op = SDGNode.Operation.ASSIGN; 

                    }
                    break;
                case 20 :
                    // /Users/jgf/Documents/Projects/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:401:5: 'IF'
                    {
                    match(input,69,FOLLOW_69_in_node_oper1471); 
                     op = SDGNode.Operation.IF; 

                    }
                    break;
                case 21 :
                    // /Users/jgf/Documents/Projects/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:402:5: 'loop'
                    {
                    match(input,70,FOLLOW_70_in_node_oper1491); 
                     op = SDGNode.Operation.LOOP; 

                    }
                    break;
                case 22 :
                    // /Users/jgf/Documents/Projects/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:403:5: 'jump'
                    {
                    match(input,71,FOLLOW_71_in_node_oper1509); 
                     op = SDGNode.Operation.JUMP; 

                    }
                    break;
                case 23 :
                    // /Users/jgf/Documents/Projects/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:404:5: 'compound'
                    {
                    match(input,72,FOLLOW_72_in_node_oper1527); 
                     op = SDGNode.Operation.COMPOUND; 

                    }
                    break;
                case 24 :
                    // /Users/jgf/Documents/Projects/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:405:5: 'call'
                    {
                    match(input,73,FOLLOW_73_in_node_oper1541); 
                     op = SDGNode.Operation.CALL; 

                    }
                    break;
                case 25 :
                    // /Users/jgf/Documents/Projects/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:406:5: 'entry'
                    {
                    match(input,74,FOLLOW_74_in_node_oper1559); 
                     op = SDGNode.Operation.ENTRY; 

                    }
                    break;
                case 26 :
                    // /Users/jgf/Documents/Projects/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:407:5: 'exit'
                    {
                    match(input,75,FOLLOW_75_in_node_oper1576); 
                     op = SDGNode.Operation.EXIT; 

                    }
                    break;
                case 27 :
                    // /Users/jgf/Documents/Projects/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:408:5: 'form-in'
                    {
                    match(input,76,FOLLOW_76_in_node_oper1594); 
                     op = SDGNode.Operation.FORMAL_IN; 

                    }
                    break;
                case 28 :
                    // /Users/jgf/Documents/Projects/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:409:5: 'form-ellip'
                    {
                    match(input,77,FOLLOW_77_in_node_oper1609); 
                     op = SDGNode.Operation.FORMAL_ELLIP; 

                    }
                    break;
                case 29 :
                    // /Users/jgf/Documents/Projects/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:410:5: 'form-out'
                    {
                    match(input,78,FOLLOW_78_in_node_oper1621); 
                     op = SDGNode.Operation.FORMAL_OUT; 

                    }
                    break;
                case 30 :
                    // /Users/jgf/Documents/Projects/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:411:5: 'act-in'
                    {
                    match(input,79,FOLLOW_79_in_node_oper1635); 
                     op = SDGNode.Operation.ACTUAL_IN; 

                    }
                    break;
                case 31 :
                    // /Users/jgf/Documents/Projects/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:412:5: 'act-out'
                    {
                    match(input,80,FOLLOW_80_in_node_oper1651); 
                     op = SDGNode.Operation.ACTUAL_OUT; 

                    }
                    break;
                case 32 :
                    // /Users/jgf/Documents/Projects/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:413:5: 'monitor'
                    {
                    match(input,81,FOLLOW_81_in_node_oper1666); 
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
    // /Users/jgf/Documents/Projects/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:416:9: private node_edges[SDGNodeStub node] : (e= edge ';' )* ;
    public final void node_edges(SDGNodeStub node) throws RecognitionException {
        SDGEdgeStub e = null;


        try {
            // /Users/jgf/Documents/Projects/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:417:3: ( (e= edge ';' )* )
            // /Users/jgf/Documents/Projects/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:417:5: (e= edge ';' )*
            {
            // /Users/jgf/Documents/Projects/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:417:5: (e= edge ';' )*
            loop15:
            do {
                int alt15=2;
                alt15 = dfa15.predict(input);
                switch (alt15) {
            	case 1 :
            	    // /Users/jgf/Documents/Projects/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:417:6: e= edge ';'
            	    {
            	    pushFollow(FOLLOW_edge_in_node_edges1694);
            	    e=edge();

            	    state._fsp--;

            	    match(input,12,FOLLOW_12_in_node_edges1696); 
            	     node.edges.add(e); 

            	    }
            	    break;

            	default :
            	    break loop15;
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
    // /Users/jgf/Documents/Projects/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:420:9: private edge returns [SDGEdgeStub estub] : k= edge_kind nr= number ( ':' label= string )? ;
    public final SDGEdgeStub edge() throws RecognitionException {
        SDGEdgeStub estub = null;

        SDGEdge.Kind k = null;

        int nr = 0;

        String label = null;


        try {
            // /Users/jgf/Documents/Projects/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:421:3: (k= edge_kind nr= number ( ':' label= string )? )
            // /Users/jgf/Documents/Projects/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:421:5: k= edge_kind nr= number ( ':' label= string )?
            {
            pushFollow(FOLLOW_edge_kind_in_edge1721);
            k=edge_kind();

            state._fsp--;

            pushFollow(FOLLOW_number_in_edge1725);
            nr=number();

            state._fsp--;

             estub = new SDGEdgeStub(k, nr); 
            // /Users/jgf/Documents/Projects/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:421:63: ( ':' label= string )?
            int alt16=2;
            int LA16_0 = input.LA(1);

            if ( (LA16_0==48) ) {
                alt16=1;
            }
            switch (alt16) {
                case 1 :
                    // /Users/jgf/Documents/Projects/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:421:64: ':' label= string
                    {
                    match(input,48,FOLLOW_48_in_edge1730); 
                    pushFollow(FOLLOW_string_in_edge1734);
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
    // /Users/jgf/Documents/Projects/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:424:9: private edge_kind returns [SDGEdge.Kind kind] : ( 'DD' | 'DH' | 'DA' | 'CD' | 'CE' | 'UN' | 'CF' | 'NF' | 'RF' | 'CC' | 'CL' | 'PI' | 'PO' | 'SU' | 'SH' | 'SF' | 'PS' | 'FORK' | 'FORK_IN' | 'FORK_OUT' | 'JOIN' | 'ID' | 'IW' | 'SD' | 'HE' | 'FD' | 'FI' | 'RY' | 'JF' | 'SP' | 'VD' | 'RD' | 'JD' );
    public final SDGEdge.Kind edge_kind() throws RecognitionException {
        SDGEdge.Kind kind = null;

        try {
            // /Users/jgf/Documents/Projects/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:426:3: ( 'DD' | 'DH' | 'DA' | 'CD' | 'CE' | 'UN' | 'CF' | 'NF' | 'RF' | 'CC' | 'CL' | 'PI' | 'PO' | 'SU' | 'SH' | 'SF' | 'PS' | 'FORK' | 'FORK_IN' | 'FORK_OUT' | 'JOIN' | 'ID' | 'IW' | 'SD' | 'HE' | 'FD' | 'FI' | 'RY' | 'JF' | 'SP' | 'VD' | 'RD' | 'JD' )
            int alt17=33;
            alt17 = dfa17.predict(input);
            switch (alt17) {
                case 1 :
                    // /Users/jgf/Documents/Projects/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:426:5: 'DD'
                    {
                    match(input,82,FOLLOW_82_in_edge_kind1759); 
                     kind = SDGEdge.Kind.DATA_DEP; 

                    }
                    break;
                case 2 :
                    // /Users/jgf/Documents/Projects/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:427:5: 'DH'
                    {
                    match(input,83,FOLLOW_83_in_edge_kind1779); 
                     kind = SDGEdge.Kind.DATA_HEAP; 

                    }
                    break;
                case 3 :
                    // /Users/jgf/Documents/Projects/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:428:5: 'DA'
                    {
                    match(input,84,FOLLOW_84_in_edge_kind1798); 
                     kind = SDGEdge.Kind.DATA_ALIAS; 

                    }
                    break;
                case 4 :
                    // /Users/jgf/Documents/Projects/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:430:5: 'CD'
                    {
                    match(input,85,FOLLOW_85_in_edge_kind1817); 
                     kind = SDGEdge.Kind.CONTROL_DEP_COND; 

                    }
                    break;
                case 5 :
                    // /Users/jgf/Documents/Projects/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:431:5: 'CE'
                    {
                    match(input,86,FOLLOW_86_in_edge_kind1829); 
                     kind = SDGEdge.Kind.CONTROL_DEP_EXPR; 

                    }
                    break;
                case 6 :
                    // /Users/jgf/Documents/Projects/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:432:5: 'UN'
                    {
                    match(input,87,FOLLOW_87_in_edge_kind1841); 
                     kind = SDGEdge.Kind.CONTROL_DEP_UNCOND; 

                    }
                    break;
                case 7 :
                    // /Users/jgf/Documents/Projects/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:434:5: 'CF'
                    {
                    match(input,88,FOLLOW_88_in_edge_kind1852); 
                     kind = SDGEdge.Kind.CONTROL_FLOW; 

                    }
                    break;
                case 8 :
                    // /Users/jgf/Documents/Projects/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:435:5: 'NF'
                    {
                    match(input,89,FOLLOW_89_in_edge_kind1868); 
                     kind = SDGEdge.Kind.NO_FLOW; 

                    }
                    break;
                case 9 :
                    // /Users/jgf/Documents/Projects/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:436:5: 'RF'
                    {
                    match(input,90,FOLLOW_90_in_edge_kind1889); 
                     kind = SDGEdge.Kind.RETURN; 

                    }
                    break;
                case 10 :
                    // /Users/jgf/Documents/Projects/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:438:5: 'CC'
                    {
                    match(input,91,FOLLOW_91_in_edge_kind1912); 
                     kind = SDGEdge.Kind.CONTROL_DEP_CALL; 

                    }
                    break;
                case 11 :
                    // /Users/jgf/Documents/Projects/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:439:5: 'CL'
                    {
                    match(input,92,FOLLOW_92_in_edge_kind1920); 
                     kind = SDGEdge.Kind.CALL; 

                    }
                    break;
                case 12 :
                    // /Users/jgf/Documents/Projects/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:440:5: 'PI'
                    {
                    match(input,93,FOLLOW_93_in_edge_kind1928); 
                     kind = SDGEdge.Kind.PARAMETER_IN; 

                    }
                    break;
                case 13 :
                    // /Users/jgf/Documents/Projects/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:441:5: 'PO'
                    {
                    match(input,94,FOLLOW_94_in_edge_kind1936); 
                     kind = SDGEdge.Kind.PARAMETER_OUT; 

                    }
                    break;
                case 14 :
                    // /Users/jgf/Documents/Projects/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:443:5: 'SU'
                    {
                    match(input,95,FOLLOW_95_in_edge_kind1945); 
                     kind = SDGEdge.Kind.SUMMARY; 

                    }
                    break;
                case 15 :
                    // /Users/jgf/Documents/Projects/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:444:5: 'SH'
                    {
                    match(input,96,FOLLOW_96_in_edge_kind1953); 
                     kind = SDGEdge.Kind.SUMMARY_NO_ALIAS; 

                    }
                    break;
                case 16 :
                    // /Users/jgf/Documents/Projects/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:445:5: 'SF'
                    {
                    match(input,97,FOLLOW_97_in_edge_kind1961); 
                     kind = SDGEdge.Kind.SUMMARY_DATA; 

                    }
                    break;
                case 17 :
                    // /Users/jgf/Documents/Projects/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:447:5: 'PS'
                    {
                    match(input,98,FOLLOW_98_in_edge_kind1970); 
                     kind = SDGEdge.Kind.PARAMETER_STRUCTURE; 

                    }
                    break;
                case 18 :
                    // /Users/jgf/Documents/Projects/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:449:5: 'FORK'
                    {
                    match(input,99,FOLLOW_99_in_edge_kind1979); 
                     kind = SDGEdge.Kind.FORK; 

                    }
                    break;
                case 19 :
                    // /Users/jgf/Documents/Projects/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:450:5: 'FORK_IN'
                    {
                    match(input,100,FOLLOW_100_in_edge_kind1987); 
                     kind = SDGEdge.Kind.FORK_IN; 

                    }
                    break;
                case 20 :
                    // /Users/jgf/Documents/Projects/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:451:5: 'FORK_OUT'
                    {
                    match(input,101,FOLLOW_101_in_edge_kind1995); 
                     kind = SDGEdge.Kind.FORK_OUT; 

                    }
                    break;
                case 21 :
                    // /Users/jgf/Documents/Projects/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:452:5: 'JOIN'
                    {
                    match(input,102,FOLLOW_102_in_edge_kind2003); 
                     kind = SDGEdge.Kind.JOIN; 

                    }
                    break;
                case 22 :
                    // /Users/jgf/Documents/Projects/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:453:5: 'ID'
                    {
                    match(input,103,FOLLOW_103_in_edge_kind2011); 
                     kind = SDGEdge.Kind.INTERFERENCE; 

                    }
                    break;
                case 23 :
                    // /Users/jgf/Documents/Projects/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:454:5: 'IW'
                    {
                    match(input,104,FOLLOW_104_in_edge_kind2019); 
                     kind = SDGEdge.Kind.INTERFERENCE_WRITE; 

                    }
                    break;
                case 24 :
                    // /Users/jgf/Documents/Projects/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:455:5: 'SD'
                    {
                    match(input,105,FOLLOW_105_in_edge_kind2027); 
                     kind = SDGEdge.Kind.SYNCHRONIZATION; 

                    }
                    break;
                case 25 :
                    // /Users/jgf/Documents/Projects/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:457:5: 'HE'
                    {
                    match(input,106,FOLLOW_106_in_edge_kind2036); 
                     kind = SDGEdge.Kind.HELP; 

                    }
                    break;
                case 26 :
                    // /Users/jgf/Documents/Projects/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:458:5: 'FD'
                    {
                    match(input,107,FOLLOW_107_in_edge_kind2044); 
                     kind = SDGEdge.Kind.FOLDED; 

                    }
                    break;
                case 27 :
                    // /Users/jgf/Documents/Projects/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:459:5: 'FI'
                    {
                    match(input,108,FOLLOW_108_in_edge_kind2052); 
                     kind = SDGEdge.Kind.FOLD_INCLUDE; 

                    }
                    break;
                case 28 :
                    // /Users/jgf/Documents/Projects/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:461:5: 'RY'
                    {
                    match(input,109,FOLLOW_109_in_edge_kind2061); 
                     kind = SDGEdge.Kind.READY_DEP; 

                    }
                    break;
                case 29 :
                    // /Users/jgf/Documents/Projects/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:462:5: 'JF'
                    {
                    match(input,110,FOLLOW_110_in_edge_kind2069); 
                     kind = SDGEdge.Kind.JUMP_FLOW; 

                    }
                    break;
                case 30 :
                    // /Users/jgf/Documents/Projects/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:463:5: 'SP'
                    {
                    match(input,111,FOLLOW_111_in_edge_kind2077); 
                     kind = SDGEdge.Kind.SUMMARY; 

                    }
                    break;
                case 31 :
                    // /Users/jgf/Documents/Projects/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:464:5: 'VD'
                    {
                    match(input,112,FOLLOW_112_in_edge_kind2085); 
                     kind = SDGEdge.Kind.DATA_DEP_EXPR_VALUE; 

                    }
                    break;
                case 32 :
                    // /Users/jgf/Documents/Projects/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:465:5: 'RD'
                    {
                    match(input,113,FOLLOW_113_in_edge_kind2093); 
                     kind = SDGEdge.Kind.DATA_DEP_EXPR_REFERENCE; 

                    }
                    break;
                case 33 :
                    // /Users/jgf/Documents/Projects/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:466:5: 'JD'
                    {
                    match(input,114,FOLLOW_114_in_edge_kind2101); 
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
    // /Users/jgf/Documents/Projects/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:469:9: private mayNegNumber returns [int nr] : ( '-' n= number | n= number );
    public final int mayNegNumber() throws RecognitionException {
        int nr = 0;

        int n = 0;


        try {
            // /Users/jgf/Documents/Projects/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:470:3: ( '-' n= number | n= number )
            int alt18=2;
            int LA18_0 = input.LA(1);

            if ( (LA18_0==49) ) {
                alt18=1;
            }
            else if ( (LA18_0==NUMBER) ) {
                alt18=2;
            }
            else {
                NoViableAltException nvae =
                    new NoViableAltException("", 18, 0, input);

                throw nvae;
            }
            switch (alt18) {
                case 1 :
                    // /Users/jgf/Documents/Projects/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:470:5: '-' n= number
                    {
                    match(input,49,FOLLOW_49_in_mayNegNumber2122); 
                    pushFollow(FOLLOW_number_in_mayNegNumber2126);
                    n=number();

                    state._fsp--;

                     nr = -n; 

                    }
                    break;
                case 2 :
                    // /Users/jgf/Documents/Projects/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:471:5: n= number
                    {
                    pushFollow(FOLLOW_number_in_mayNegNumber2136);
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
    // /Users/jgf/Documents/Projects/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:474:9: private number returns [int nr] : n= NUMBER ;
    public final int number() throws RecognitionException {
        int nr = 0;

        Token n=null;

        try {
            // /Users/jgf/Documents/Projects/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:475:3: (n= NUMBER )
            // /Users/jgf/Documents/Projects/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:475:5: n= NUMBER
            {
            n=(Token)match(input,NUMBER,FOLLOW_NUMBER_in_number2159); 
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
    // /Users/jgf/Documents/Projects/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:478:9: private string returns [String str] : s= STRING ;
    public final String string() throws RecognitionException {
        String str = null;

        Token s=null;

        try {
            // /Users/jgf/Documents/Projects/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:479:3: (s= STRING )
            // /Users/jgf/Documents/Projects/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:479:5: s= STRING
            {
            s=(Token)match(input,STRING,FOLLOW_STRING_in_string2182); 
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
    // /Users/jgf/Documents/Projects/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:482:9: private bool returns [boolean b] : ( 'true' | 'false' );
    public final boolean bool() throws RecognitionException {
        boolean b = false;

        try {
            // /Users/jgf/Documents/Projects/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:483:3: ( 'true' | 'false' )
            int alt19=2;
            int LA19_0 = input.LA(1);

            if ( (LA19_0==115) ) {
                alt19=1;
            }
            else if ( (LA19_0==116) ) {
                alt19=2;
            }
            else {
                NoViableAltException nvae =
                    new NoViableAltException("", 19, 0, input);

                throw nvae;
            }
            switch (alt19) {
                case 1 :
                    // /Users/jgf/Documents/Projects/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:483:5: 'true'
                    {
                    match(input,115,FOLLOW_115_in_bool2203); 
                     b = true; 

                    }
                    break;
                case 2 :
                    // /Users/jgf/Documents/Projects/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:484:5: 'false'
                    {
                    match(input,116,FOLLOW_116_in_bool2212); 
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
    protected DFA8 dfa8 = new DFA8(this);
    protected DFA9 dfa9 = new DFA9(this);
    protected DFA10 dfa10 = new DFA10(this);
    protected DFA11 dfa11 = new DFA11(this);
    protected DFA14 dfa14 = new DFA14(this);
    protected DFA15 dfa15 = new DFA15(this);
    protected DFA17 dfa17 = new DFA17(this);
    static final String DFA1_eotS =
        "\20\uffff";
    static final String DFA1_eofS =
        "\20\uffff";
    static final String DFA1_minS =
        "\1\10\17\uffff";
    static final String DFA1_maxS =
        "\1\43\17\uffff";
    static final String DFA1_acceptS =
        "\1\uffff\1\1\1\2\15\uffff";
    static final String DFA1_specialS =
        "\20\uffff}>";
    static final String[] DFA1_transitionS = {
            "\1\1\2\2\15\uffff\14\2",
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
            return "273:7: ( 'JComp' )?";
        }
    }
    static final String DFA8_eotS =
        "\17\uffff";
    static final String DFA8_eofS =
        "\17\uffff";
    static final String DFA8_minS =
        "\1\11\16\uffff";
    static final String DFA8_maxS =
        "\1\43\16\uffff";
    static final String DFA8_acceptS =
        "\1\uffff\1\2\1\uffff\1\1\13\uffff";
    static final String DFA8_specialS =
        "\17\uffff}>";
    static final String[] DFA8_transitionS = {
            "\2\1\15\uffff\14\3",
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

    static final short[] DFA8_eot = DFA.unpackEncodedString(DFA8_eotS);
    static final short[] DFA8_eof = DFA.unpackEncodedString(DFA8_eofS);
    static final char[] DFA8_min = DFA.unpackEncodedStringToUnsignedChars(DFA8_minS);
    static final char[] DFA8_max = DFA.unpackEncodedStringToUnsignedChars(DFA8_maxS);
    static final short[] DFA8_accept = DFA.unpackEncodedString(DFA8_acceptS);
    static final short[] DFA8_special = DFA.unpackEncodedString(DFA8_specialS);
    static final short[][] DFA8_transition;

    static {
        int numStates = DFA8_transitionS.length;
        DFA8_transition = new short[numStates][];
        for (int i=0; i<numStates; i++) {
            DFA8_transition[i] = DFA.unpackEncodedString(DFA8_transitionS[i]);
        }
    }

    class DFA8 extends DFA {

        public DFA8(BaseRecognizer recognizer) {
            this.recognizer = recognizer;
            this.decisionNumber = 8;
            this.eot = DFA8_eot;
            this.eof = DFA8_eof;
            this.min = DFA8_min;
            this.max = DFA8_max;
            this.accept = DFA8_accept;
            this.special = DFA8_special;
            this.transition = DFA8_transition;
        }
        public String getDescription() {
            return "()* loopback of 318:5: (n= node )*";
        }
    }
    static final String DFA9_eotS =
        "\15\uffff";
    static final String DFA9_eofS =
        "\15\uffff";
    static final String DFA9_minS =
        "\1\30\14\uffff";
    static final String DFA9_maxS =
        "\1\43\14\uffff";
    static final String DFA9_acceptS =
        "\1\uffff\1\1\1\2\1\3\1\4\1\5\1\6\1\7\1\10\1\11\1\12\1\13\1\14";
    static final String DFA9_specialS =
        "\15\uffff}>";
    static final String[] DFA9_transitionS = {
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
            return "329:9: private node_kind returns [SDGNode.Kind kind] : ( 'NORM' | 'PRED' | 'EXPR' | 'ENTR' | 'CALL' | 'ACTI' | 'ACTO' | 'FRMI' | 'FRMO' | 'EXIT' | 'SYNC' | 'FOLD' );";
        }
    }
    static final String DFA10_eotS =
        "\57\uffff";
    static final String DFA10_eofS =
        "\57\uffff";
    static final String DFA10_minS =
        "\1\11\56\uffff";
    static final String DFA10_maxS =
        "\1\162\56\uffff";
    static final String DFA10_acceptS =
        "\1\uffff\1\2\41\uffff\1\1\13\uffff";
    static final String DFA10_specialS =
        "\57\uffff}>";
    static final String[] DFA10_transitionS = {
            "\1\1\32\uffff\14\43\42\uffff\41\1",
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

    static final short[] DFA10_eot = DFA.unpackEncodedString(DFA10_eotS);
    static final short[] DFA10_eof = DFA.unpackEncodedString(DFA10_eofS);
    static final char[] DFA10_min = DFA.unpackEncodedStringToUnsignedChars(DFA10_minS);
    static final char[] DFA10_max = DFA.unpackEncodedStringToUnsignedChars(DFA10_maxS);
    static final short[] DFA10_accept = DFA.unpackEncodedString(DFA10_acceptS);
    static final short[] DFA10_special = DFA.unpackEncodedString(DFA10_specialS);
    static final short[][] DFA10_transition;

    static {
        int numStates = DFA10_transitionS.length;
        DFA10_transition = new short[numStates][];
        for (int i=0; i<numStates; i++) {
            DFA10_transition[i] = DFA.unpackEncodedString(DFA10_transitionS[i]);
        }
    }

    class DFA10 extends DFA {

        public DFA10(BaseRecognizer recognizer) {
            this.recognizer = recognizer;
            this.decisionNumber = 10;
            this.eot = DFA10_eot;
            this.eof = DFA10_eof;
            this.min = DFA10_min;
            this.max = DFA10_max;
            this.accept = DFA10_accept;
            this.special = DFA10_special;
            this.transition = DFA10_transition;
        }
        public String getDescription() {
            return "()* loopback of 345:5: ( node_attr[node] ';' )*";
        }
    }
    static final String DFA11_eotS =
        "\17\uffff";
    static final String DFA11_eofS =
        "\17\uffff";
    static final String DFA11_minS =
        "\1\44\2\uffff\1\4\13\uffff";
    static final String DFA11_maxS =
        "\1\57\2\uffff\1\5\13\uffff";
    static final String DFA11_acceptS =
        "\1\uffff\1\1\1\2\1\uffff\1\4\1\5\1\6\1\7\1\10\1\11\1\12\1\13\1\14"+
        "\1\3\1\15";
    static final String DFA11_specialS =
        "\17\uffff}>";
    static final String[] DFA11_transitionS = {
            "\1\1\1\2\1\3\1\4\1\5\1\6\1\7\1\10\1\11\1\12\1\13\1\14",
            "",
            "",
            "\1\15\1\16",
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

    static final short[] DFA11_eot = DFA.unpackEncodedString(DFA11_eotS);
    static final short[] DFA11_eof = DFA.unpackEncodedString(DFA11_eofS);
    static final char[] DFA11_min = DFA.unpackEncodedStringToUnsignedChars(DFA11_minS);
    static final char[] DFA11_max = DFA.unpackEncodedStringToUnsignedChars(DFA11_maxS);
    static final short[] DFA11_accept = DFA.unpackEncodedString(DFA11_acceptS);
    static final short[] DFA11_special = DFA.unpackEncodedString(DFA11_specialS);
    static final short[][] DFA11_transition;

    static {
        int numStates = DFA11_transitionS.length;
        DFA11_transition = new short[numStates][];
        for (int i=0; i<numStates; i++) {
            DFA11_transition[i] = DFA.unpackEncodedString(DFA11_transitionS[i]);
        }
    }

    class DFA11 extends DFA {

        public DFA11(BaseRecognizer recognizer) {
            this.recognizer = recognizer;
            this.decisionNumber = 11;
            this.eot = DFA11_eot;
            this.eof = DFA11_eof;
            this.min = DFA11_min;
            this.max = DFA11_max;
            this.accept = DFA11_accept;
            this.special = DFA11_special;
            this.transition = DFA11_transition;
        }
        public String getDescription() {
            return "348:9: private node_attr[SDGNodeStub node] : ( 'S' spos= node_source | 'B' bpos= node_bytecode | 'U' number | 'P' procId= number | 'O' op= node_oper | 'V' val= string | 'T' type= string | 'Z' tn= may_neg_num_set | 'N' | 'C' cl= string | 'A' al= pos_num_set | 'D' ds= pos_num_set | 'U' uct= string );";
        }
    }
    static final String DFA14_eotS =
        "\41\uffff";
    static final String DFA14_eofS =
        "\41\uffff";
    static final String DFA14_minS =
        "\1\62\40\uffff";
    static final String DFA14_maxS =
        "\1\121\40\uffff";
    static final String DFA14_acceptS =
        "\1\uffff\1\1\1\2\1\3\1\4\1\5\1\6\1\7\1\10\1\11\1\12\1\13\1\14\1"+
        "\15\1\16\1\17\1\20\1\21\1\22\1\23\1\24\1\25\1\26\1\27\1\30\1\31"+
        "\1\32\1\33\1\34\1\35\1\36\1\37\1\40";
    static final String DFA14_specialS =
        "\41\uffff}>";
    static final String[] DFA14_transitionS = {
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

    static final short[] DFA14_eot = DFA.unpackEncodedString(DFA14_eotS);
    static final short[] DFA14_eof = DFA.unpackEncodedString(DFA14_eofS);
    static final char[] DFA14_min = DFA.unpackEncodedStringToUnsignedChars(DFA14_minS);
    static final char[] DFA14_max = DFA.unpackEncodedStringToUnsignedChars(DFA14_maxS);
    static final short[] DFA14_accept = DFA.unpackEncodedString(DFA14_acceptS);
    static final short[] DFA14_special = DFA.unpackEncodedString(DFA14_specialS);
    static final short[][] DFA14_transition;

    static {
        int numStates = DFA14_transitionS.length;
        DFA14_transition = new short[numStates][];
        for (int i=0; i<numStates; i++) {
            DFA14_transition[i] = DFA.unpackEncodedString(DFA14_transitionS[i]);
        }
    }

    class DFA14 extends DFA {

        public DFA14(BaseRecognizer recognizer) {
            this.recognizer = recognizer;
            this.decisionNumber = 14;
            this.eot = DFA14_eot;
            this.eof = DFA14_eof;
            this.min = DFA14_min;
            this.max = DFA14_max;
            this.accept = DFA14_accept;
            this.special = DFA14_special;
            this.transition = DFA14_transition;
        }
        public String getDescription() {
            return "381:9: private node_oper returns [SDGNode.Operation op] : ( 'empty' | 'intconst' | 'floatconst' | 'charconst' | 'stringconst' | 'functionconst' | 'shortcut' | 'question' | 'binary' | 'unary' | 'derefer' | 'refer' | 'array' | 'select' | 'reference' | 'declaration' | 'modify' | 'modassign' | 'assign' | 'IF' | 'loop' | 'jump' | 'compound' | 'call' | 'entry' | 'exit' | 'form-in' | 'form-ellip' | 'form-out' | 'act-in' | 'act-out' | 'monitor' );";
        }
    }
    static final String DFA15_eotS =
        "\43\uffff";
    static final String DFA15_eofS =
        "\43\uffff";
    static final String DFA15_minS =
        "\1\11\42\uffff";
    static final String DFA15_maxS =
        "\1\162\42\uffff";
    static final String DFA15_acceptS =
        "\1\uffff\1\2\1\1\40\uffff";
    static final String DFA15_specialS =
        "\43\uffff}>";
    static final String[] DFA15_transitionS = {
            "\1\1\110\uffff\41\2",
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

    static final short[] DFA15_eot = DFA.unpackEncodedString(DFA15_eotS);
    static final short[] DFA15_eof = DFA.unpackEncodedString(DFA15_eofS);
    static final char[] DFA15_min = DFA.unpackEncodedStringToUnsignedChars(DFA15_minS);
    static final char[] DFA15_max = DFA.unpackEncodedStringToUnsignedChars(DFA15_maxS);
    static final short[] DFA15_accept = DFA.unpackEncodedString(DFA15_acceptS);
    static final short[] DFA15_special = DFA.unpackEncodedString(DFA15_specialS);
    static final short[][] DFA15_transition;

    static {
        int numStates = DFA15_transitionS.length;
        DFA15_transition = new short[numStates][];
        for (int i=0; i<numStates; i++) {
            DFA15_transition[i] = DFA.unpackEncodedString(DFA15_transitionS[i]);
        }
    }

    class DFA15 extends DFA {

        public DFA15(BaseRecognizer recognizer) {
            this.recognizer = recognizer;
            this.decisionNumber = 15;
            this.eot = DFA15_eot;
            this.eof = DFA15_eof;
            this.min = DFA15_min;
            this.max = DFA15_max;
            this.accept = DFA15_accept;
            this.special = DFA15_special;
            this.transition = DFA15_transition;
        }
        public String getDescription() {
            return "()* loopback of 417:5: (e= edge ';' )*";
        }
    }
    static final String DFA17_eotS =
        "\42\uffff";
    static final String DFA17_eofS =
        "\42\uffff";
    static final String DFA17_minS =
        "\1\122\41\uffff";
    static final String DFA17_maxS =
        "\1\162\41\uffff";
    static final String DFA17_acceptS =
        "\1\uffff\1\1\1\2\1\3\1\4\1\5\1\6\1\7\1\10\1\11\1\12\1\13\1\14\1"+
        "\15\1\16\1\17\1\20\1\21\1\22\1\23\1\24\1\25\1\26\1\27\1\30\1\31"+
        "\1\32\1\33\1\34\1\35\1\36\1\37\1\40\1\41";
    static final String DFA17_specialS =
        "\42\uffff}>";
    static final String[] DFA17_transitionS = {
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

    static final short[] DFA17_eot = DFA.unpackEncodedString(DFA17_eotS);
    static final short[] DFA17_eof = DFA.unpackEncodedString(DFA17_eofS);
    static final char[] DFA17_min = DFA.unpackEncodedStringToUnsignedChars(DFA17_minS);
    static final char[] DFA17_max = DFA.unpackEncodedStringToUnsignedChars(DFA17_maxS);
    static final short[] DFA17_accept = DFA.unpackEncodedString(DFA17_acceptS);
    static final short[] DFA17_special = DFA.unpackEncodedString(DFA17_specialS);
    static final short[][] DFA17_transition;

    static {
        int numStates = DFA17_transitionS.length;
        DFA17_transition = new short[numStates][];
        for (int i=0; i<numStates; i++) {
            DFA17_transition[i] = DFA.unpackEncodedString(DFA17_transitionS[i]);
        }
    }

    class DFA17 extends DFA {

        public DFA17(BaseRecognizer recognizer) {
            this.recognizer = recognizer;
            this.decisionNumber = 17;
            this.eot = DFA17_eot;
            this.eof = DFA17_eof;
            this.min = DFA17_min;
            this.max = DFA17_max;
            this.accept = DFA17_accept;
            this.special = DFA17_special;
            this.transition = DFA17_transition;
        }
        public String getDescription() {
            return "424:9: private edge_kind returns [SDGEdge.Kind kind] : ( 'DD' | 'DH' | 'DA' | 'CD' | 'CE' | 'UN' | 'CF' | 'NF' | 'RF' | 'CC' | 'CL' | 'PI' | 'PO' | 'SU' | 'SH' | 'SF' | 'PS' | 'FORK' | 'FORK_IN' | 'FORK_OUT' | 'JOIN' | 'ID' | 'IW' | 'SD' | 'HE' | 'FD' | 'FI' | 'RY' | 'JF' | 'SP' | 'VD' | 'RD' | 'JD' );";
        }
    }
 

    public static final BitSet FOLLOW_sdg_header_in_sdg_file73 = new BitSet(new long[]{0x0000000000000080L});
    public static final BitSet FOLLOW_7_in_sdg_file89 = new BitSet(new long[]{0x0000000FFF000700L});
    public static final BitSet FOLLOW_8_in_sdg_file99 = new BitSet(new long[]{0x0000000FFF000600L});
    public static final BitSet FOLLOW_node_list_in_sdg_file128 = new BitSet(new long[]{0x0000000000000600L});
    public static final BitSet FOLLOW_thread_info_in_sdg_file150 = new BitSet(new long[]{0x0000000000000200L});
    public static final BitSet FOLLOW_9_in_sdg_file163 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_thread_in_thread_info192 = new BitSet(new long[]{0x0000000000000402L});
    public static final BitSet FOLLOW_10_in_thread224 = new BitSet(new long[]{0x0000000000000010L});
    public static final BitSet FOLLOW_number_in_thread228 = new BitSet(new long[]{0x0000000000000080L});
    public static final BitSet FOLLOW_7_in_thread230 = new BitSet(new long[]{0x0000000000000800L});
    public static final BitSet FOLLOW_11_in_thread238 = new BitSet(new long[]{0x0000000000000010L});
    public static final BitSet FOLLOW_number_in_thread244 = new BitSet(new long[]{0x0000000000001000L});
    public static final BitSet FOLLOW_12_in_thread246 = new BitSet(new long[]{0x0000000000002000L});
    public static final BitSet FOLLOW_13_in_thread254 = new BitSet(new long[]{0x0000000000000010L});
    public static final BitSet FOLLOW_number_in_thread261 = new BitSet(new long[]{0x0000000000001000L});
    public static final BitSet FOLLOW_12_in_thread263 = new BitSet(new long[]{0x0000000000004000L});
    public static final BitSet FOLLOW_14_in_thread271 = new BitSet(new long[]{0x0000000000000010L});
    public static final BitSet FOLLOW_number_in_thread278 = new BitSet(new long[]{0x0000000000001000L});
    public static final BitSet FOLLOW_12_in_thread280 = new BitSet(new long[]{0x0000000000008000L});
    public static final BitSet FOLLOW_15_in_thread288 = new BitSet(new long[]{0x0000000000000010L});
    public static final BitSet FOLLOW_number_in_thread295 = new BitSet(new long[]{0x0000000000001000L});
    public static final BitSet FOLLOW_12_in_thread297 = new BitSet(new long[]{0x0000000000010000L});
    public static final BitSet FOLLOW_16_in_thread305 = new BitSet(new long[]{0x00000000000C0000L});
    public static final BitSet FOLLOW_context_in_thread309 = new BitSet(new long[]{0x0000000000001000L});
    public static final BitSet FOLLOW_12_in_thread312 = new BitSet(new long[]{0x0000000000020000L});
    public static final BitSet FOLLOW_17_in_thread320 = new BitSet(new long[]{0x0000000000000000L,0x0018000000000000L});
    public static final BitSet FOLLOW_bool_in_thread324 = new BitSet(new long[]{0x0000000000001000L});
    public static final BitSet FOLLOW_12_in_thread326 = new BitSet(new long[]{0x0000000000000200L});
    public static final BitSet FOLLOW_9_in_thread332 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_18_in_context360 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_19_in_context366 = new BitSet(new long[]{0x0002000000000010L});
    public static final BitSet FOLLOW_mayNegNumber_in_context370 = new BitSet(new long[]{0x0000000000300000L});
    public static final BitSet FOLLOW_20_in_context375 = new BitSet(new long[]{0x0002000000000010L});
    public static final BitSet FOLLOW_mayNegNumber_in_context379 = new BitSet(new long[]{0x0000000000300000L});
    public static final BitSet FOLLOW_21_in_context386 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_22_in_sdg_header405 = new BitSet(new long[]{0x0000000000800022L});
    public static final BitSet FOLLOW_23_in_sdg_header421 = new BitSet(new long[]{0x0000000000000010L});
    public static final BitSet FOLLOW_number_in_sdg_header425 = new BitSet(new long[]{0x0000000000000022L});
    public static final BitSet FOLLOW_string_in_sdg_header446 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_node_in_node_list479 = new BitSet(new long[]{0x0000000FFF000002L});
    public static final BitSet FOLLOW_node_kind_in_node505 = new BitSet(new long[]{0x0002000000000010L});
    public static final BitSet FOLLOW_mayNegNumber_in_node509 = new BitSet(new long[]{0x0000000000000080L});
    public static final BitSet FOLLOW_7_in_node518 = new BitSet(new long[]{0x0000FFF000000200L,0x0007FFFFFFFC0000L});
    public static final BitSet FOLLOW_node_attributes_in_node527 = new BitSet(new long[]{0x0000000000000200L,0x0007FFFFFFFC0000L});
    public static final BitSet FOLLOW_node_edges_in_node537 = new BitSet(new long[]{0x0000000000000200L});
    public static final BitSet FOLLOW_9_in_node544 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_24_in_node_kind563 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_25_in_node_kind571 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_26_in_node_kind579 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_27_in_node_kind587 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_28_in_node_kind595 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_29_in_node_kind603 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_30_in_node_kind611 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_31_in_node_kind619 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_32_in_node_kind627 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_33_in_node_kind635 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_34_in_node_kind643 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_35_in_node_kind651 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_node_attr_in_node_attributes670 = new BitSet(new long[]{0x0000000000001000L});
    public static final BitSet FOLLOW_12_in_node_attributes673 = new BitSet(new long[]{0x0000FFF000000002L});
    public static final BitSet FOLLOW_36_in_node_attr691 = new BitSet(new long[]{0x0000000000000020L});
    public static final BitSet FOLLOW_node_source_in_node_attr695 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_37_in_node_attr707 = new BitSet(new long[]{0x0000000000000020L});
    public static final BitSet FOLLOW_node_bytecode_in_node_attr711 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_38_in_node_attr722 = new BitSet(new long[]{0x0000000000000010L});
    public static final BitSet FOLLOW_number_in_node_attr724 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_39_in_node_attr774 = new BitSet(new long[]{0x0000000000000010L});
    public static final BitSet FOLLOW_number_in_node_attr778 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_40_in_node_attr797 = new BitSet(new long[]{0xFFFC000000000000L,0x000000000003FFFFL});
    public static final BitSet FOLLOW_node_oper_in_node_attr801 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_41_in_node_attr829 = new BitSet(new long[]{0x0000000000000020L});
    public static final BitSet FOLLOW_string_in_node_attr833 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_42_in_node_attr861 = new BitSet(new long[]{0x0000000000000020L});
    public static final BitSet FOLLOW_string_in_node_attr865 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_43_in_node_attr890 = new BitSet(new long[]{0x0002000000000010L});
    public static final BitSet FOLLOW_may_neg_num_set_in_node_attr894 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_44_in_node_attr908 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_45_in_node_attr942 = new BitSet(new long[]{0x0000000000000020L});
    public static final BitSet FOLLOW_string_in_node_attr946 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_46_in_node_attr968 = new BitSet(new long[]{0x0000000000000010L});
    public static final BitSet FOLLOW_pos_num_set_in_node_attr972 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_47_in_node_attr990 = new BitSet(new long[]{0x0000000000000010L});
    public static final BitSet FOLLOW_pos_num_set_in_node_attr994 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_38_in_node_attr1011 = new BitSet(new long[]{0x0000000000000020L});
    public static final BitSet FOLLOW_string_in_node_attr1015 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_number_in_pos_num_set1048 = new BitSet(new long[]{0x0000000000100002L});
    public static final BitSet FOLLOW_20_in_pos_num_set1053 = new BitSet(new long[]{0x0000000000000010L});
    public static final BitSet FOLLOW_number_in_pos_num_set1057 = new BitSet(new long[]{0x0000000000100002L});
    public static final BitSet FOLLOW_mayNegNumber_in_may_neg_num_set1083 = new BitSet(new long[]{0x0000000000100002L});
    public static final BitSet FOLLOW_20_in_may_neg_num_set1088 = new BitSet(new long[]{0x0002000000000010L});
    public static final BitSet FOLLOW_mayNegNumber_in_may_neg_num_set1092 = new BitSet(new long[]{0x0000000000100002L});
    public static final BitSet FOLLOW_string_in_node_source1118 = new BitSet(new long[]{0x0001000000000000L});
    public static final BitSet FOLLOW_48_in_node_source1120 = new BitSet(new long[]{0x0000000000000010L});
    public static final BitSet FOLLOW_number_in_node_source1124 = new BitSet(new long[]{0x0000000000100000L});
    public static final BitSet FOLLOW_20_in_node_source1126 = new BitSet(new long[]{0x0000000000000010L});
    public static final BitSet FOLLOW_number_in_node_source1130 = new BitSet(new long[]{0x0002000000000000L});
    public static final BitSet FOLLOW_49_in_node_source1132 = new BitSet(new long[]{0x0000000000000010L});
    public static final BitSet FOLLOW_number_in_node_source1136 = new BitSet(new long[]{0x0000000000100000L});
    public static final BitSet FOLLOW_20_in_node_source1138 = new BitSet(new long[]{0x0000000000000010L});
    public static final BitSet FOLLOW_number_in_node_source1142 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_string_in_node_bytecode1173 = new BitSet(new long[]{0x0001000000000000L});
    public static final BitSet FOLLOW_48_in_node_bytecode1175 = new BitSet(new long[]{0x0002000000000010L});
    public static final BitSet FOLLOW_mayNegNumber_in_node_bytecode1179 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_50_in_node_oper1200 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_51_in_node_oper1217 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_52_in_node_oper1231 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_53_in_node_oper1243 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_54_in_node_oper1256 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_55_in_node_oper1267 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_56_in_node_oper1276 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_57_in_node_oper1290 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_58_in_node_oper1304 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_59_in_node_oper1320 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_60_in_node_oper1337 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_61_in_node_oper1352 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_62_in_node_oper1369 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_63_in_node_oper1386 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_64_in_node_oper1402 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_65_in_node_oper1415 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_66_in_node_oper1426 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_67_in_node_oper1442 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_68_in_node_oper1455 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_69_in_node_oper1471 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_70_in_node_oper1491 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_71_in_node_oper1509 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_72_in_node_oper1527 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_73_in_node_oper1541 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_74_in_node_oper1559 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_75_in_node_oper1576 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_76_in_node_oper1594 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_77_in_node_oper1609 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_78_in_node_oper1621 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_79_in_node_oper1635 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_80_in_node_oper1651 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_81_in_node_oper1666 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_edge_in_node_edges1694 = new BitSet(new long[]{0x0000000000001000L});
    public static final BitSet FOLLOW_12_in_node_edges1696 = new BitSet(new long[]{0x0000000000000002L,0x0007FFFFFFFC0000L});
    public static final BitSet FOLLOW_edge_kind_in_edge1721 = new BitSet(new long[]{0x0000000000000010L});
    public static final BitSet FOLLOW_number_in_edge1725 = new BitSet(new long[]{0x0001000000000002L});
    public static final BitSet FOLLOW_48_in_edge1730 = new BitSet(new long[]{0x0000000000000020L});
    public static final BitSet FOLLOW_string_in_edge1734 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_82_in_edge_kind1759 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_83_in_edge_kind1779 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_84_in_edge_kind1798 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_85_in_edge_kind1817 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_86_in_edge_kind1829 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_87_in_edge_kind1841 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_88_in_edge_kind1852 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_89_in_edge_kind1868 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_90_in_edge_kind1889 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_91_in_edge_kind1912 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_92_in_edge_kind1920 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_93_in_edge_kind1928 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_94_in_edge_kind1936 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_95_in_edge_kind1945 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_96_in_edge_kind1953 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_97_in_edge_kind1961 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_98_in_edge_kind1970 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_99_in_edge_kind1979 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_100_in_edge_kind1987 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_101_in_edge_kind1995 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_102_in_edge_kind2003 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_103_in_edge_kind2011 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_104_in_edge_kind2019 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_105_in_edge_kind2027 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_106_in_edge_kind2036 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_107_in_edge_kind2044 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_108_in_edge_kind2052 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_109_in_edge_kind2061 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_110_in_edge_kind2069 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_111_in_edge_kind2077 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_112_in_edge_kind2085 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_113_in_edge_kind2093 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_114_in_edge_kind2101 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_49_in_mayNegNumber2122 = new BitSet(new long[]{0x0000000000000010L});
    public static final BitSet FOLLOW_number_in_mayNegNumber2126 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_number_in_mayNegNumber2136 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_NUMBER_in_number2159 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_STRING_in_string2182 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_115_in_bool2203 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_116_in_bool2212 = new BitSet(new long[]{0x0000000000000002L});

}