// $ANTLR 3.1.3 Mar 17, 2009 19:23:44 /Users/jgf/Documents/Projects/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDGThreadInstance_.g 2014-06-23 18:17:35
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
import gnu.trove.list.TIntList;
import gnu.trove.list.array.TIntArrayList;
import gnu.trove.procedure.TIntProcedure;


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
public class SDGThreadInstance_Parser extends Parser {
    public static final String[] tokenNames = new String[] {
        "<invalid>", "<EOR>", "<DOWN>", "<UP>", "NUMBER", "STRING", "WHITESPACE", "'Thread'", "'{'", "'Entry'", "';'", "'Exit'", "'Fork'", "'Join'", "'Context'", "'Dynamic'", "'}'", "'null'", "'['", "','", "']'", "'-'", "'true'", "'false'"
    };
    public static final int T__23=23;
    public static final int T__22=22;
    public static final int T__21=21;
    public static final int T__20=20;
    public static final int NUMBER=4;
    public static final int WHITESPACE=6;
    public static final int EOF=-1;
    public static final int T__9=9;
    public static final int T__8=8;
    public static final int T__7=7;
    public static final int T__19=19;
    public static final int T__16=16;
    public static final int T__15=15;
    public static final int T__18=18;
    public static final int T__17=17;
    public static final int T__12=12;
    public static final int T__11=11;
    public static final int T__14=14;
    public static final int T__13=13;
    public static final int T__10=10;
    public static final int STRING=5;

    // delegates
    // delegators


        public SDGThreadInstance_Parser(TokenStream input) {
            this(input, new RecognizerSharedState());
        }
        public SDGThreadInstance_Parser(TokenStream input, RecognizerSharedState state) {
            super(input, state);
             
        }
        

    public String[] getTokenNames() { return SDGThreadInstance_Parser.tokenNames; }
    public String getGrammarFileName() { return "/Users/jgf/Documents/Projects/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDGThreadInstance_.g"; }



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
        private final int join;
        private final TIntList threadContext;
        private boolean dynamic;

        public ThreadInstanceStub(int id, int en, int ex, int fo, int jo, TIntList tc, boolean dyn) {
          this.id = id;
          this.entry = en;
          this.exit = ex;
          this.fork = fo;
          this.join = jo;
          this.threadContext = tc;
          this.dynamic = dyn;
        }

        public ThreadInstance create(final SDG sdg) {
          final SDGNode tentry = findNode(sdg, entry);
          final SDGNode texit = findNode(sdg, exit);
          final SDGNode tfork = findNode(sdg, fork);
          final SDGNode tjoin = findNode(sdg, join);
          final LinkedList<SDGNode> tcontext = findNodes(sdg, threadContext);  

          return new ThreadInstance(id, tentry, texit, tfork, tjoin, tcontext, dynamic);
        }

        private static LinkedList<SDGNode> findNodes(final SDG sdg, final TIntList ctx) {
          final LinkedList<SDGNode> nodes = new LinkedList<SDGNode>();
          
          ctx.forEach(new TIntProcedure() {
          
            @Override
            public boolean execute(final int id) {
              final SDGNode n = findNode(sdg, id);
              if (n != null) {
                nodes.add(n);
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



    // $ANTLR start "thread"
    // /Users/jgf/Documents/Projects/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDGThreadInstance_.g:161:1: thread returns [ThreadInstanceStub ti] : 'Thread' id= number '{' 'Entry' en= number ';' 'Exit' ex= number ';' 'Fork' fo= number ';' 'Join' jo= number ';' 'Context' con= context ';' 'Dynamic' dyn= bool ';' '}' ;
    public final ThreadInstanceStub thread() throws RecognitionException {
        ThreadInstanceStub ti = null;

        int id = 0;

        int en = 0;

        int ex = 0;

        int fo = 0;

        int jo = 0;

        TIntList con = null;

        boolean dyn = false;


        try {
            // /Users/jgf/Documents/Projects/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDGThreadInstance_.g:162:3: ( 'Thread' id= number '{' 'Entry' en= number ';' 'Exit' ex= number ';' 'Fork' fo= number ';' 'Join' jo= number ';' 'Context' con= context ';' 'Dynamic' dyn= bool ';' '}' )
            // /Users/jgf/Documents/Projects/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDGThreadInstance_.g:162:5: 'Thread' id= number '{' 'Entry' en= number ';' 'Exit' ex= number ';' 'Fork' fo= number ';' 'Join' jo= number ';' 'Context' con= context ';' 'Dynamic' dyn= bool ';' '}'
            {
            match(input,7,FOLLOW_7_in_thread71); 
            pushFollow(FOLLOW_number_in_thread75);
            id=number();

            state._fsp--;

            match(input,8,FOLLOW_8_in_thread77); 
            match(input,9,FOLLOW_9_in_thread85); 
            pushFollow(FOLLOW_number_in_thread91);
            en=number();

            state._fsp--;

            match(input,10,FOLLOW_10_in_thread93); 
            match(input,11,FOLLOW_11_in_thread101); 
            pushFollow(FOLLOW_number_in_thread108);
            ex=number();

            state._fsp--;

            match(input,10,FOLLOW_10_in_thread110); 
            match(input,12,FOLLOW_12_in_thread118); 
            pushFollow(FOLLOW_number_in_thread125);
            fo=number();

            state._fsp--;

            match(input,10,FOLLOW_10_in_thread127); 
            match(input,13,FOLLOW_13_in_thread135); 
            pushFollow(FOLLOW_number_in_thread142);
            jo=number();

            state._fsp--;

            match(input,10,FOLLOW_10_in_thread144); 
            match(input,14,FOLLOW_14_in_thread152); 
            pushFollow(FOLLOW_context_in_thread156);
            con=context();

            state._fsp--;

            match(input,10,FOLLOW_10_in_thread158); 
            match(input,15,FOLLOW_15_in_thread166); 
            pushFollow(FOLLOW_bool_in_thread170);
            dyn=bool();

            state._fsp--;

            match(input,10,FOLLOW_10_in_thread172); 
            match(input,16,FOLLOW_16_in_thread178); 

                  final int entry = en;
                  int exit = ThreadInstanceStub.UNDEF_NODE; if (ex != 0) { exit = ex; }
                  int fork = ThreadInstanceStub.UNDEF_NODE; if (fo != 0) { fork = fo; }
                  int join = ThreadInstanceStub.UNDEF_NODE; if (jo != 0) { join = jo; }
                  ti = new ThreadInstanceStub(id, entry, exit, fork, join, con, dyn);
                

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
    // /Users/jgf/Documents/Projects/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDGThreadInstance_.g:179:9: private context returns [TIntList cx = new TIntArrayList();] : ( 'null' | '[' i= mayNegNumber ( ',' i= mayNegNumber )* ']' );
    public final TIntList context() throws RecognitionException {
        TIntList cx =  new TIntArrayList();;

        int i = 0;


        try {
            // /Users/jgf/Documents/Projects/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDGThreadInstance_.g:180:3: ( 'null' | '[' i= mayNegNumber ( ',' i= mayNegNumber )* ']' )
            int alt2=2;
            int LA2_0 = input.LA(1);

            if ( (LA2_0==17) ) {
                alt2=1;
            }
            else if ( (LA2_0==18) ) {
                alt2=2;
            }
            else {
                NoViableAltException nvae =
                    new NoViableAltException("", 2, 0, input);

                throw nvae;
            }
            switch (alt2) {
                case 1 :
                    // /Users/jgf/Documents/Projects/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDGThreadInstance_.g:180:5: 'null'
                    {
                    match(input,17,FOLLOW_17_in_context205); 

                    }
                    break;
                case 2 :
                    // /Users/jgf/Documents/Projects/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDGThreadInstance_.g:181:5: '[' i= mayNegNumber ( ',' i= mayNegNumber )* ']'
                    {
                    match(input,18,FOLLOW_18_in_context211); 
                    pushFollow(FOLLOW_mayNegNumber_in_context215);
                    i=mayNegNumber();

                    state._fsp--;

                     cx.add(i); 
                    // /Users/jgf/Documents/Projects/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDGThreadInstance_.g:181:39: ( ',' i= mayNegNumber )*
                    loop1:
                    do {
                        int alt1=2;
                        int LA1_0 = input.LA(1);

                        if ( (LA1_0==19) ) {
                            alt1=1;
                        }


                        switch (alt1) {
                    	case 1 :
                    	    // /Users/jgf/Documents/Projects/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDGThreadInstance_.g:181:40: ',' i= mayNegNumber
                    	    {
                    	    match(input,19,FOLLOW_19_in_context220); 
                    	    pushFollow(FOLLOW_mayNegNumber_in_context224);
                    	    i=mayNegNumber();

                    	    state._fsp--;

                    	     cx.add(i); 

                    	    }
                    	    break;

                    	default :
                    	    break loop1;
                        }
                    } while (true);

                    match(input,20,FOLLOW_20_in_context231); 

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


    // $ANTLR start "mayNegNumber"
    // /Users/jgf/Documents/Projects/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDGThreadInstance_.g:184:9: private mayNegNumber returns [int nr] : ( '-' n= number | n= number );
    public final int mayNegNumber() throws RecognitionException {
        int nr = 0;

        int n = 0;


        try {
            // /Users/jgf/Documents/Projects/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDGThreadInstance_.g:185:3: ( '-' n= number | n= number )
            int alt3=2;
            int LA3_0 = input.LA(1);

            if ( (LA3_0==21) ) {
                alt3=1;
            }
            else if ( (LA3_0==NUMBER) ) {
                alt3=2;
            }
            else {
                NoViableAltException nvae =
                    new NoViableAltException("", 3, 0, input);

                throw nvae;
            }
            switch (alt3) {
                case 1 :
                    // /Users/jgf/Documents/Projects/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDGThreadInstance_.g:185:5: '-' n= number
                    {
                    match(input,21,FOLLOW_21_in_mayNegNumber250); 
                    pushFollow(FOLLOW_number_in_mayNegNumber254);
                    n=number();

                    state._fsp--;

                     nr = -n; 

                    }
                    break;
                case 2 :
                    // /Users/jgf/Documents/Projects/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDGThreadInstance_.g:186:5: n= number
                    {
                    pushFollow(FOLLOW_number_in_mayNegNumber264);
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
    // /Users/jgf/Documents/Projects/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDGThreadInstance_.g:189:9: private number returns [int nr] : n= NUMBER ;
    public final int number() throws RecognitionException {
        int nr = 0;

        Token n=null;

        try {
            // /Users/jgf/Documents/Projects/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDGThreadInstance_.g:190:3: (n= NUMBER )
            // /Users/jgf/Documents/Projects/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDGThreadInstance_.g:190:5: n= NUMBER
            {
            n=(Token)match(input,NUMBER,FOLLOW_NUMBER_in_number287); 
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
    // /Users/jgf/Documents/Projects/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDGThreadInstance_.g:193:9: private string returns [String str] : s= STRING ;
    public final String string() throws RecognitionException {
        String str = null;

        Token s=null;

        try {
            // /Users/jgf/Documents/Projects/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDGThreadInstance_.g:194:3: (s= STRING )
            // /Users/jgf/Documents/Projects/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDGThreadInstance_.g:194:5: s= STRING
            {
            s=(Token)match(input,STRING,FOLLOW_STRING_in_string310); 
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
    // /Users/jgf/Documents/Projects/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDGThreadInstance_.g:197:9: private bool returns [boolean b] : ( 'true' | 'false' );
    public final boolean bool() throws RecognitionException {
        boolean b = false;

        try {
            // /Users/jgf/Documents/Projects/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDGThreadInstance_.g:198:3: ( 'true' | 'false' )
            int alt4=2;
            int LA4_0 = input.LA(1);

            if ( (LA4_0==22) ) {
                alt4=1;
            }
            else if ( (LA4_0==23) ) {
                alt4=2;
            }
            else {
                NoViableAltException nvae =
                    new NoViableAltException("", 4, 0, input);

                throw nvae;
            }
            switch (alt4) {
                case 1 :
                    // /Users/jgf/Documents/Projects/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDGThreadInstance_.g:198:5: 'true'
                    {
                    match(input,22,FOLLOW_22_in_bool331); 
                     b = true; 

                    }
                    break;
                case 2 :
                    // /Users/jgf/Documents/Projects/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDGThreadInstance_.g:199:5: 'false'
                    {
                    match(input,23,FOLLOW_23_in_bool340); 
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


 

    public static final BitSet FOLLOW_7_in_thread71 = new BitSet(new long[]{0x0000000000000010L});
    public static final BitSet FOLLOW_number_in_thread75 = new BitSet(new long[]{0x0000000000000100L});
    public static final BitSet FOLLOW_8_in_thread77 = new BitSet(new long[]{0x0000000000000200L});
    public static final BitSet FOLLOW_9_in_thread85 = new BitSet(new long[]{0x0000000000000010L});
    public static final BitSet FOLLOW_number_in_thread91 = new BitSet(new long[]{0x0000000000000400L});
    public static final BitSet FOLLOW_10_in_thread93 = new BitSet(new long[]{0x0000000000000800L});
    public static final BitSet FOLLOW_11_in_thread101 = new BitSet(new long[]{0x0000000000000010L});
    public static final BitSet FOLLOW_number_in_thread108 = new BitSet(new long[]{0x0000000000000400L});
    public static final BitSet FOLLOW_10_in_thread110 = new BitSet(new long[]{0x0000000000001000L});
    public static final BitSet FOLLOW_12_in_thread118 = new BitSet(new long[]{0x0000000000000010L});
    public static final BitSet FOLLOW_number_in_thread125 = new BitSet(new long[]{0x0000000000000400L});
    public static final BitSet FOLLOW_10_in_thread127 = new BitSet(new long[]{0x0000000000002000L});
    public static final BitSet FOLLOW_13_in_thread135 = new BitSet(new long[]{0x0000000000000010L});
    public static final BitSet FOLLOW_number_in_thread142 = new BitSet(new long[]{0x0000000000000400L});
    public static final BitSet FOLLOW_10_in_thread144 = new BitSet(new long[]{0x0000000000004000L});
    public static final BitSet FOLLOW_14_in_thread152 = new BitSet(new long[]{0x0000000000060000L});
    public static final BitSet FOLLOW_context_in_thread156 = new BitSet(new long[]{0x0000000000000400L});
    public static final BitSet FOLLOW_10_in_thread158 = new BitSet(new long[]{0x0000000000008000L});
    public static final BitSet FOLLOW_15_in_thread166 = new BitSet(new long[]{0x0000000000C00000L});
    public static final BitSet FOLLOW_bool_in_thread170 = new BitSet(new long[]{0x0000000000000400L});
    public static final BitSet FOLLOW_10_in_thread172 = new BitSet(new long[]{0x0000000000010000L});
    public static final BitSet FOLLOW_16_in_thread178 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_17_in_context205 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_18_in_context211 = new BitSet(new long[]{0x0000000000200010L});
    public static final BitSet FOLLOW_mayNegNumber_in_context215 = new BitSet(new long[]{0x0000000000180000L});
    public static final BitSet FOLLOW_19_in_context220 = new BitSet(new long[]{0x0000000000200010L});
    public static final BitSet FOLLOW_mayNegNumber_in_context224 = new BitSet(new long[]{0x0000000000180000L});
    public static final BitSet FOLLOW_20_in_context231 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_21_in_mayNegNumber250 = new BitSet(new long[]{0x0000000000000010L});
    public static final BitSet FOLLOW_number_in_mayNegNumber254 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_number_in_mayNegNumber264 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_NUMBER_in_number287 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_STRING_in_string310 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_22_in_bool331 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_23_in_bool340 = new BitSet(new long[]{0x0000000000000002L});

}