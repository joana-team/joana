// $ANTLR 3.1.3 Mar 17, 2009 19:23:44 /Users/jgf/Documents/Projects/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDGThreadInstance_.g 2014-06-23 18:17:35
/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.ifc.sdg.graph;



import org.antlr.runtime.*;
import java.util.Stack;
import java.util.List;
import java.util.ArrayList;

public class SDGThreadInstance_Lexer extends Lexer {
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


    // delegates
    // delegators

    public SDGThreadInstance_Lexer() {;} 
    public SDGThreadInstance_Lexer(CharStream input) {
        this(input, new RecognizerSharedState());
    }
    public SDGThreadInstance_Lexer(CharStream input, RecognizerSharedState state) {
        super(input,state);

    }
    public String getGrammarFileName() { return "/Users/jgf/Documents/Projects/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDGThreadInstance_.g"; }

    // $ANTLR start "T__7"
    public final void mT__7() throws RecognitionException {
        try {
            int _type = T__7;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // /Users/jgf/Documents/Projects/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDGThreadInstance_.g:47:6: ( 'Thread' )
            // /Users/jgf/Documents/Projects/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDGThreadInstance_.g:47:8: 'Thread'
            {
            match("Thread"); 


            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        }
    }
    // $ANTLR end "T__7"

    // $ANTLR start "T__8"
    public final void mT__8() throws RecognitionException {
        try {
            int _type = T__8;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // /Users/jgf/Documents/Projects/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDGThreadInstance_.g:48:6: ( '{' )
            // /Users/jgf/Documents/Projects/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDGThreadInstance_.g:48:8: '{'
            {
            match('{'); 

            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        }
    }
    // $ANTLR end "T__8"

    // $ANTLR start "T__9"
    public final void mT__9() throws RecognitionException {
        try {
            int _type = T__9;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // /Users/jgf/Documents/Projects/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDGThreadInstance_.g:49:6: ( 'Entry' )
            // /Users/jgf/Documents/Projects/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDGThreadInstance_.g:49:8: 'Entry'
            {
            match("Entry"); 


            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        }
    }
    // $ANTLR end "T__9"

    // $ANTLR start "T__10"
    public final void mT__10() throws RecognitionException {
        try {
            int _type = T__10;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // /Users/jgf/Documents/Projects/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDGThreadInstance_.g:50:7: ( ';' )
            // /Users/jgf/Documents/Projects/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDGThreadInstance_.g:50:9: ';'
            {
            match(';'); 

            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        }
    }
    // $ANTLR end "T__10"

    // $ANTLR start "T__11"
    public final void mT__11() throws RecognitionException {
        try {
            int _type = T__11;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // /Users/jgf/Documents/Projects/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDGThreadInstance_.g:51:7: ( 'Exit' )
            // /Users/jgf/Documents/Projects/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDGThreadInstance_.g:51:9: 'Exit'
            {
            match("Exit"); 


            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        }
    }
    // $ANTLR end "T__11"

    // $ANTLR start "T__12"
    public final void mT__12() throws RecognitionException {
        try {
            int _type = T__12;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // /Users/jgf/Documents/Projects/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDGThreadInstance_.g:52:7: ( 'Fork' )
            // /Users/jgf/Documents/Projects/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDGThreadInstance_.g:52:9: 'Fork'
            {
            match("Fork"); 


            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        }
    }
    // $ANTLR end "T__12"

    // $ANTLR start "T__13"
    public final void mT__13() throws RecognitionException {
        try {
            int _type = T__13;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // /Users/jgf/Documents/Projects/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDGThreadInstance_.g:53:7: ( 'Join' )
            // /Users/jgf/Documents/Projects/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDGThreadInstance_.g:53:9: 'Join'
            {
            match("Join"); 


            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        }
    }
    // $ANTLR end "T__13"

    // $ANTLR start "T__14"
    public final void mT__14() throws RecognitionException {
        try {
            int _type = T__14;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // /Users/jgf/Documents/Projects/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDGThreadInstance_.g:54:7: ( 'Context' )
            // /Users/jgf/Documents/Projects/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDGThreadInstance_.g:54:9: 'Context'
            {
            match("Context"); 


            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        }
    }
    // $ANTLR end "T__14"

    // $ANTLR start "T__15"
    public final void mT__15() throws RecognitionException {
        try {
            int _type = T__15;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // /Users/jgf/Documents/Projects/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDGThreadInstance_.g:55:7: ( 'Dynamic' )
            // /Users/jgf/Documents/Projects/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDGThreadInstance_.g:55:9: 'Dynamic'
            {
            match("Dynamic"); 


            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        }
    }
    // $ANTLR end "T__15"

    // $ANTLR start "T__16"
    public final void mT__16() throws RecognitionException {
        try {
            int _type = T__16;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // /Users/jgf/Documents/Projects/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDGThreadInstance_.g:56:7: ( '}' )
            // /Users/jgf/Documents/Projects/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDGThreadInstance_.g:56:9: '}'
            {
            match('}'); 

            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        }
    }
    // $ANTLR end "T__16"

    // $ANTLR start "T__17"
    public final void mT__17() throws RecognitionException {
        try {
            int _type = T__17;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // /Users/jgf/Documents/Projects/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDGThreadInstance_.g:57:7: ( 'null' )
            // /Users/jgf/Documents/Projects/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDGThreadInstance_.g:57:9: 'null'
            {
            match("null"); 


            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        }
    }
    // $ANTLR end "T__17"

    // $ANTLR start "T__18"
    public final void mT__18() throws RecognitionException {
        try {
            int _type = T__18;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // /Users/jgf/Documents/Projects/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDGThreadInstance_.g:58:7: ( '[' )
            // /Users/jgf/Documents/Projects/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDGThreadInstance_.g:58:9: '['
            {
            match('['); 

            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        }
    }
    // $ANTLR end "T__18"

    // $ANTLR start "T__19"
    public final void mT__19() throws RecognitionException {
        try {
            int _type = T__19;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // /Users/jgf/Documents/Projects/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDGThreadInstance_.g:59:7: ( ',' )
            // /Users/jgf/Documents/Projects/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDGThreadInstance_.g:59:9: ','
            {
            match(','); 

            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        }
    }
    // $ANTLR end "T__19"

    // $ANTLR start "T__20"
    public final void mT__20() throws RecognitionException {
        try {
            int _type = T__20;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // /Users/jgf/Documents/Projects/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDGThreadInstance_.g:60:7: ( ']' )
            // /Users/jgf/Documents/Projects/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDGThreadInstance_.g:60:9: ']'
            {
            match(']'); 

            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        }
    }
    // $ANTLR end "T__20"

    // $ANTLR start "T__21"
    public final void mT__21() throws RecognitionException {
        try {
            int _type = T__21;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // /Users/jgf/Documents/Projects/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDGThreadInstance_.g:61:7: ( '-' )
            // /Users/jgf/Documents/Projects/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDGThreadInstance_.g:61:9: '-'
            {
            match('-'); 

            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        }
    }
    // $ANTLR end "T__21"

    // $ANTLR start "T__22"
    public final void mT__22() throws RecognitionException {
        try {
            int _type = T__22;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // /Users/jgf/Documents/Projects/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDGThreadInstance_.g:62:7: ( 'true' )
            // /Users/jgf/Documents/Projects/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDGThreadInstance_.g:62:9: 'true'
            {
            match("true"); 


            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        }
    }
    // $ANTLR end "T__22"

    // $ANTLR start "T__23"
    public final void mT__23() throws RecognitionException {
        try {
            int _type = T__23;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // /Users/jgf/Documents/Projects/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDGThreadInstance_.g:63:7: ( 'false' )
            // /Users/jgf/Documents/Projects/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDGThreadInstance_.g:63:9: 'false'
            {
            match("false"); 


            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        }
    }
    // $ANTLR end "T__23"

    // $ANTLR start "WHITESPACE"
    public final void mWHITESPACE() throws RecognitionException {
        try {
            int _type = WHITESPACE;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // /Users/jgf/Documents/Projects/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDGThreadInstance_.g:205:3: ( ( ' ' | '\\t' | '\\n' | '\\r' ) )
            // /Users/jgf/Documents/Projects/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDGThreadInstance_.g:205:5: ( ' ' | '\\t' | '\\n' | '\\r' )
            {
            if ( (input.LA(1)>='\t' && input.LA(1)<='\n')||input.LA(1)=='\r'||input.LA(1)==' ' ) {
                input.consume();

            }
            else {
                MismatchedSetException mse = new MismatchedSetException(null,input);
                recover(mse);
                throw mse;}

             _channel=HIDDEN; 

            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        }
    }
    // $ANTLR end "WHITESPACE"

    // $ANTLR start "NUMBER"
    public final void mNUMBER() throws RecognitionException {
        try {
            int _type = NUMBER;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // /Users/jgf/Documents/Projects/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDGThreadInstance_.g:209:3: ( ( '0' .. '9' )+ )
            // /Users/jgf/Documents/Projects/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDGThreadInstance_.g:209:5: ( '0' .. '9' )+
            {
            // /Users/jgf/Documents/Projects/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDGThreadInstance_.g:209:5: ( '0' .. '9' )+
            int cnt1=0;
            loop1:
            do {
                int alt1=2;
                int LA1_0 = input.LA(1);

                if ( ((LA1_0>='0' && LA1_0<='9')) ) {
                    alt1=1;
                }


                switch (alt1) {
            	case 1 :
            	    // /Users/jgf/Documents/Projects/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDGThreadInstance_.g:209:6: '0' .. '9'
            	    {
            	    matchRange('0','9'); 

            	    }
            	    break;

            	default :
            	    if ( cnt1 >= 1 ) break loop1;
                        EarlyExitException eee =
                            new EarlyExitException(1, input);
                        throw eee;
                }
                cnt1++;
            } while (true);


            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        }
    }
    // $ANTLR end "NUMBER"

    // $ANTLR start "STRING"
    public final void mSTRING() throws RecognitionException {
        try {
            int _type = STRING;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // /Users/jgf/Documents/Projects/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDGThreadInstance_.g:213:3: ( '<' '\"' ({...}? '\"' | ~ ( '\"' ) )* '\"' '>' | ( '\"' (~ '\"' )* '\"' ) )
            int alt4=2;
            int LA4_0 = input.LA(1);

            if ( (LA4_0=='<') ) {
                alt4=1;
            }
            else if ( (LA4_0=='\"') ) {
                alt4=2;
            }
            else {
                NoViableAltException nvae =
                    new NoViableAltException("", 4, 0, input);

                throw nvae;
            }
            switch (alt4) {
                case 1 :
                    // /Users/jgf/Documents/Projects/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDGThreadInstance_.g:213:5: '<' '\"' ({...}? '\"' | ~ ( '\"' ) )* '\"' '>'
                    {
                    match('<'); 
                    match('\"'); 
                    // /Users/jgf/Documents/Projects/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDGThreadInstance_.g:213:13: ({...}? '\"' | ~ ( '\"' ) )*
                    loop2:
                    do {
                        int alt2=3;
                        int LA2_0 = input.LA(1);

                        if ( (LA2_0=='\"') ) {
                            int LA2_1 = input.LA(2);

                            if ( (LA2_1=='>') ) {
                                int LA2_3 = input.LA(3);

                                if ( ((LA2_3>='\u0000' && LA2_3<='\uFFFF')) ) {
                                    alt2=1;
                                }


                            }
                            else if ( ((LA2_1>='\u0000' && LA2_1<='=')||(LA2_1>='?' && LA2_1<='\uFFFF')) ) {
                                alt2=1;
                            }


                        }
                        else if ( ((LA2_0>='\u0000' && LA2_0<='!')||(LA2_0>='#' && LA2_0<='\uFFFF')) ) {
                            alt2=2;
                        }


                        switch (alt2) {
                    	case 1 :
                    	    // /Users/jgf/Documents/Projects/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDGThreadInstance_.g:213:15: {...}? '\"'
                    	    {
                    	    if ( !(( input.LA(2) != '>' )) ) {
                    	        throw new FailedPredicateException(input, "STRING", " input.LA(2) != '>' ");
                    	    }
                    	    match('\"'); 

                    	    }
                    	    break;
                    	case 2 :
                    	    // /Users/jgf/Documents/Projects/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDGThreadInstance_.g:213:45: ~ ( '\"' )
                    	    {
                    	    if ( (input.LA(1)>='\u0000' && input.LA(1)<='!')||(input.LA(1)>='#' && input.LA(1)<='\uFFFF') ) {
                    	        input.consume();

                    	    }
                    	    else {
                    	        MismatchedSetException mse = new MismatchedSetException(null,input);
                    	        recover(mse);
                    	        throw mse;}


                    	    }
                    	    break;

                    	default :
                    	    break loop2;
                        }
                    } while (true);

                    match('\"'); 
                    match('>'); 

                    }
                    break;
                case 2 :
                    // /Users/jgf/Documents/Projects/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDGThreadInstance_.g:214:5: ( '\"' (~ '\"' )* '\"' )
                    {
                    // /Users/jgf/Documents/Projects/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDGThreadInstance_.g:214:5: ( '\"' (~ '\"' )* '\"' )
                    // /Users/jgf/Documents/Projects/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDGThreadInstance_.g:214:6: '\"' (~ '\"' )* '\"'
                    {
                    match('\"'); 
                    // /Users/jgf/Documents/Projects/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDGThreadInstance_.g:214:10: (~ '\"' )*
                    loop3:
                    do {
                        int alt3=2;
                        int LA3_0 = input.LA(1);

                        if ( ((LA3_0>='\u0000' && LA3_0<='!')||(LA3_0>='#' && LA3_0<='\uFFFF')) ) {
                            alt3=1;
                        }


                        switch (alt3) {
                    	case 1 :
                    	    // /Users/jgf/Documents/Projects/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDGThreadInstance_.g:214:11: ~ '\"'
                    	    {
                    	    if ( (input.LA(1)>='\u0000' && input.LA(1)<='!')||(input.LA(1)>='#' && input.LA(1)<='\uFFFF') ) {
                    	        input.consume();

                    	    }
                    	    else {
                    	        MismatchedSetException mse = new MismatchedSetException(null,input);
                    	        recover(mse);
                    	        throw mse;}


                    	    }
                    	    break;

                    	default :
                    	    break loop3;
                        }
                    } while (true);

                    match('\"'); 

                    }


                    }
                    break;

            }
            state.type = _type;
            state.channel = _channel;
        }
        finally {
        }
    }
    // $ANTLR end "STRING"

    public void mTokens() throws RecognitionException {
        // /Users/jgf/Documents/Projects/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDGThreadInstance_.g:1:8: ( T__7 | T__8 | T__9 | T__10 | T__11 | T__12 | T__13 | T__14 | T__15 | T__16 | T__17 | T__18 | T__19 | T__20 | T__21 | T__22 | T__23 | WHITESPACE | NUMBER | STRING )
        int alt5=20;
        alt5 = dfa5.predict(input);
        switch (alt5) {
            case 1 :
                // /Users/jgf/Documents/Projects/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDGThreadInstance_.g:1:10: T__7
                {
                mT__7(); 

                }
                break;
            case 2 :
                // /Users/jgf/Documents/Projects/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDGThreadInstance_.g:1:15: T__8
                {
                mT__8(); 

                }
                break;
            case 3 :
                // /Users/jgf/Documents/Projects/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDGThreadInstance_.g:1:20: T__9
                {
                mT__9(); 

                }
                break;
            case 4 :
                // /Users/jgf/Documents/Projects/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDGThreadInstance_.g:1:25: T__10
                {
                mT__10(); 

                }
                break;
            case 5 :
                // /Users/jgf/Documents/Projects/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDGThreadInstance_.g:1:31: T__11
                {
                mT__11(); 

                }
                break;
            case 6 :
                // /Users/jgf/Documents/Projects/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDGThreadInstance_.g:1:37: T__12
                {
                mT__12(); 

                }
                break;
            case 7 :
                // /Users/jgf/Documents/Projects/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDGThreadInstance_.g:1:43: T__13
                {
                mT__13(); 

                }
                break;
            case 8 :
                // /Users/jgf/Documents/Projects/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDGThreadInstance_.g:1:49: T__14
                {
                mT__14(); 

                }
                break;
            case 9 :
                // /Users/jgf/Documents/Projects/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDGThreadInstance_.g:1:55: T__15
                {
                mT__15(); 

                }
                break;
            case 10 :
                // /Users/jgf/Documents/Projects/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDGThreadInstance_.g:1:61: T__16
                {
                mT__16(); 

                }
                break;
            case 11 :
                // /Users/jgf/Documents/Projects/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDGThreadInstance_.g:1:67: T__17
                {
                mT__17(); 

                }
                break;
            case 12 :
                // /Users/jgf/Documents/Projects/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDGThreadInstance_.g:1:73: T__18
                {
                mT__18(); 

                }
                break;
            case 13 :
                // /Users/jgf/Documents/Projects/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDGThreadInstance_.g:1:79: T__19
                {
                mT__19(); 

                }
                break;
            case 14 :
                // /Users/jgf/Documents/Projects/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDGThreadInstance_.g:1:85: T__20
                {
                mT__20(); 

                }
                break;
            case 15 :
                // /Users/jgf/Documents/Projects/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDGThreadInstance_.g:1:91: T__21
                {
                mT__21(); 

                }
                break;
            case 16 :
                // /Users/jgf/Documents/Projects/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDGThreadInstance_.g:1:97: T__22
                {
                mT__22(); 

                }
                break;
            case 17 :
                // /Users/jgf/Documents/Projects/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDGThreadInstance_.g:1:103: T__23
                {
                mT__23(); 

                }
                break;
            case 18 :
                // /Users/jgf/Documents/Projects/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDGThreadInstance_.g:1:109: WHITESPACE
                {
                mWHITESPACE(); 

                }
                break;
            case 19 :
                // /Users/jgf/Documents/Projects/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDGThreadInstance_.g:1:120: NUMBER
                {
                mNUMBER(); 

                }
                break;
            case 20 :
                // /Users/jgf/Documents/Projects/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDGThreadInstance_.g:1:127: STRING
                {
                mSTRING(); 

                }
                break;

        }

    }


    protected DFA5 dfa5 = new DFA5(this);
    static final String DFA5_eotS =
        "\26\uffff";
    static final String DFA5_eofS =
        "\26\uffff";
    static final String DFA5_minS =
        "\1\11\2\uffff\1\156\22\uffff";
    static final String DFA5_maxS =
        "\1\175\2\uffff\1\170\22\uffff";
    static final String DFA5_acceptS =
        "\1\uffff\1\1\1\2\1\uffff\1\4\1\6\1\7\1\10\1\11\1\12\1\13\1\14\1"+
        "\15\1\16\1\17\1\20\1\21\1\22\1\23\1\24\1\3\1\5";
    static final String DFA5_specialS =
        "\26\uffff}>";
    static final String[] DFA5_transitionS = {
            "\2\21\2\uffff\1\21\22\uffff\1\21\1\uffff\1\23\11\uffff\1\14"+
            "\1\16\2\uffff\12\22\1\uffff\1\4\1\23\6\uffff\1\7\1\10\1\3\1"+
            "\5\3\uffff\1\6\11\uffff\1\1\6\uffff\1\13\1\uffff\1\15\10\uffff"+
            "\1\20\7\uffff\1\12\5\uffff\1\17\6\uffff\1\2\1\uffff\1\11",
            "",
            "",
            "\1\24\11\uffff\1\25",
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

    static final short[] DFA5_eot = DFA.unpackEncodedString(DFA5_eotS);
    static final short[] DFA5_eof = DFA.unpackEncodedString(DFA5_eofS);
    static final char[] DFA5_min = DFA.unpackEncodedStringToUnsignedChars(DFA5_minS);
    static final char[] DFA5_max = DFA.unpackEncodedStringToUnsignedChars(DFA5_maxS);
    static final short[] DFA5_accept = DFA.unpackEncodedString(DFA5_acceptS);
    static final short[] DFA5_special = DFA.unpackEncodedString(DFA5_specialS);
    static final short[][] DFA5_transition;

    static {
        int numStates = DFA5_transitionS.length;
        DFA5_transition = new short[numStates][];
        for (int i=0; i<numStates; i++) {
            DFA5_transition[i] = DFA.unpackEncodedString(DFA5_transitionS[i]);
        }
    }

    class DFA5 extends DFA {

        public DFA5(BaseRecognizer recognizer) {
            this.recognizer = recognizer;
            this.decisionNumber = 5;
            this.eot = DFA5_eot;
            this.eof = DFA5_eof;
            this.min = DFA5_min;
            this.max = DFA5_max;
            this.accept = DFA5_accept;
            this.special = DFA5_special;
            this.transition = DFA5_transition;
        }
        public String getDescription() {
            return "1:1: Tokens : ( T__7 | T__8 | T__9 | T__10 | T__11 | T__12 | T__13 | T__14 | T__15 | T__16 | T__17 | T__18 | T__19 | T__20 | T__21 | T__22 | T__23 | WHITESPACE | NUMBER | STRING );";
        }
    }
 

}