// $ANTLR 3.1.3 Mar 17, 2009 19:23:44 /Users/jgf/Documents/Projects/mojo/wala/joana.wala.flowless/src/edu/kit/joana/wala/flowless/spec/FlowLess.g 2012-11-17 22:34:47
/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.wala.flowless.spec;


import org.antlr.runtime.*;
import java.util.Stack;
import java.util.List;
import java.util.ArrayList;

public class FlowLessLexer extends Lexer {
    public static final int STATE=13;
    public static final int T__29=29;
    public static final int IMPLIES=4;
    public static final int T__28=28;
    public static final int T__27=27;
    public static final int T__26=26;
    public static final int T__25=25;
    public static final int T__24=24;
    public static final int ALIAS_AND=6;
    public static final int T__23=23;
    public static final int T__22=22;
    public static final int T__21=21;
    public static final int T__20=20;
    public static final int NOT_ARROW=9;
    public static final int RESULT=11;
    public static final int EOF=-1;
    public static final int ALIAS_NOT=7;
    public static final int ALIAS_OR=5;
    public static final int T__30=30;
    public static final int T__19=19;
    public static final int EXC=12;
    public static final int WS_=16;
    public static final int T__18=18;
    public static final int T__17=17;
    public static final int WILDCARD=10;
    public static final int ARROW=8;
    public static final int IDENT=14;
    public static final int ARRAY=15;

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

    public FlowLessLexer() {;} 
    public FlowLessLexer(CharStream input) {
        this(input, new RecognizerSharedState());
    }
    public FlowLessLexer(CharStream input, RecognizerSharedState state) {
        super(input,state);

    }
    public String getGrammarFileName() { return "/Users/jgf/Documents/Projects/mojo/wala/joana.wala.flowless/src/edu/kit/joana/wala/flowless/spec/FlowLess.g"; }

    // $ANTLR start "T__17"
    public final void mT__17() throws RecognitionException {
        try {
            int _type = T__17;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // /Users/jgf/Documents/Projects/mojo/wala/joana.wala.flowless/src/edu/kit/joana/wala/flowless/spec/FlowLess.g:46:7: ( '?' )
            // /Users/jgf/Documents/Projects/mojo/wala/joana.wala.flowless/src/edu/kit/joana/wala/flowless/spec/FlowLess.g:46:9: '?'
            {
            match('?'); 

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
            // /Users/jgf/Documents/Projects/mojo/wala/joana.wala.flowless/src/edu/kit/joana/wala/flowless/spec/FlowLess.g:47:7: ( 'alias' )
            // /Users/jgf/Documents/Projects/mojo/wala/joana.wala.flowless/src/edu/kit/joana/wala/flowless/spec/FlowLess.g:47:9: 'alias'
            {
            match("alias"); 


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
            // /Users/jgf/Documents/Projects/mojo/wala/joana.wala.flowless/src/edu/kit/joana/wala/flowless/spec/FlowLess.g:48:7: ( '(' )
            // /Users/jgf/Documents/Projects/mojo/wala/joana.wala.flowless/src/edu/kit/joana/wala/flowless/spec/FlowLess.g:48:9: '('
            {
            match('('); 

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
            // /Users/jgf/Documents/Projects/mojo/wala/joana.wala.flowless/src/edu/kit/joana/wala/flowless/spec/FlowLess.g:49:7: ( ')' )
            // /Users/jgf/Documents/Projects/mojo/wala/joana.wala.flowless/src/edu/kit/joana/wala/flowless/spec/FlowLess.g:49:9: ')'
            {
            match(')'); 

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
            // /Users/jgf/Documents/Projects/mojo/wala/joana.wala.flowless/src/edu/kit/joana/wala/flowless/spec/FlowLess.g:50:7: ( '{' )
            // /Users/jgf/Documents/Projects/mojo/wala/joana.wala.flowless/src/edu/kit/joana/wala/flowless/spec/FlowLess.g:50:9: '{'
            {
            match('{'); 

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
            // /Users/jgf/Documents/Projects/mojo/wala/joana.wala.flowless/src/edu/kit/joana/wala/flowless/spec/FlowLess.g:51:7: ( '}' )
            // /Users/jgf/Documents/Projects/mojo/wala/joana.wala.flowless/src/edu/kit/joana/wala/flowless/spec/FlowLess.g:51:9: '}'
            {
            match('}'); 

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
            // /Users/jgf/Documents/Projects/mojo/wala/joana.wala.flowless/src/edu/kit/joana/wala/flowless/spec/FlowLess.g:52:7: ( 'unique' )
            // /Users/jgf/Documents/Projects/mojo/wala/joana.wala.flowless/src/edu/kit/joana/wala/flowless/spec/FlowLess.g:52:9: 'unique'
            {
            match("unique"); 


            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        }
    }
    // $ANTLR end "T__23"

    // $ANTLR start "T__24"
    public final void mT__24() throws RecognitionException {
        try {
            int _type = T__24;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // /Users/jgf/Documents/Projects/mojo/wala/joana.wala.flowless/src/edu/kit/joana/wala/flowless/spec/FlowLess.g:53:7: ( '1' )
            // /Users/jgf/Documents/Projects/mojo/wala/joana.wala.flowless/src/edu/kit/joana/wala/flowless/spec/FlowLess.g:53:9: '1'
            {
            match('1'); 

            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        }
    }
    // $ANTLR end "T__24"

    // $ANTLR start "T__25"
    public final void mT__25() throws RecognitionException {
        try {
            int _type = T__25;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // /Users/jgf/Documents/Projects/mojo/wala/joana.wala.flowless/src/edu/kit/joana/wala/flowless/spec/FlowLess.g:54:7: ( 'uniq' )
            // /Users/jgf/Documents/Projects/mojo/wala/joana.wala.flowless/src/edu/kit/joana/wala/flowless/spec/FlowLess.g:54:9: 'uniq'
            {
            match("uniq"); 


            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        }
    }
    // $ANTLR end "T__25"

    // $ANTLR start "T__26"
    public final void mT__26() throws RecognitionException {
        try {
            int _type = T__26;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // /Users/jgf/Documents/Projects/mojo/wala/joana.wala.flowless/src/edu/kit/joana/wala/flowless/spec/FlowLess.g:55:7: ( ',' )
            // /Users/jgf/Documents/Projects/mojo/wala/joana.wala.flowless/src/edu/kit/joana/wala/flowless/spec/FlowLess.g:55:9: ','
            {
            match(','); 

            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        }
    }
    // $ANTLR end "T__26"

    // $ANTLR start "T__27"
    public final void mT__27() throws RecognitionException {
        try {
            int _type = T__27;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // /Users/jgf/Documents/Projects/mojo/wala/joana.wala.flowless/src/edu/kit/joana/wala/flowless/spec/FlowLess.g:56:7: ( 'pure' )
            // /Users/jgf/Documents/Projects/mojo/wala/joana.wala.flowless/src/edu/kit/joana/wala/flowless/spec/FlowLess.g:56:9: 'pure'
            {
            match("pure"); 


            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        }
    }
    // $ANTLR end "T__27"

    // $ANTLR start "T__28"
    public final void mT__28() throws RecognitionException {
        try {
            int _type = T__28;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // /Users/jgf/Documents/Projects/mojo/wala/joana.wala.flowless/src/edu/kit/joana/wala/flowless/spec/FlowLess.g:57:7: ( '.' )
            // /Users/jgf/Documents/Projects/mojo/wala/joana.wala.flowless/src/edu/kit/joana/wala/flowless/spec/FlowLess.g:57:9: '.'
            {
            match('.'); 

            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        }
    }
    // $ANTLR end "T__28"

    // $ANTLR start "T__29"
    public final void mT__29() throws RecognitionException {
        try {
            int _type = T__29;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // /Users/jgf/Documents/Projects/mojo/wala/joana.wala.flowless/src/edu/kit/joana/wala/flowless/spec/FlowLess.g:58:7: ( '[' )
            // /Users/jgf/Documents/Projects/mojo/wala/joana.wala.flowless/src/edu/kit/joana/wala/flowless/spec/FlowLess.g:58:9: '['
            {
            match('['); 

            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        }
    }
    // $ANTLR end "T__29"

    // $ANTLR start "T__30"
    public final void mT__30() throws RecognitionException {
        try {
            int _type = T__30;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // /Users/jgf/Documents/Projects/mojo/wala/joana.wala.flowless/src/edu/kit/joana/wala/flowless/spec/FlowLess.g:59:7: ( ']' )
            // /Users/jgf/Documents/Projects/mojo/wala/joana.wala.flowless/src/edu/kit/joana/wala/flowless/spec/FlowLess.g:59:9: ']'
            {
            match(']'); 

            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        }
    }
    // $ANTLR end "T__30"

    // $ANTLR start "WS_"
    public final void mWS_() throws RecognitionException {
        try {
            int _type = WS_;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // /Users/jgf/Documents/Projects/mojo/wala/joana.wala.flowless/src/edu/kit/joana/wala/flowless/spec/FlowLess.g:230:2: ( ( ' ' | '\\t' | '\\n' | '\\r' ) )
            // /Users/jgf/Documents/Projects/mojo/wala/joana.wala.flowless/src/edu/kit/joana/wala/flowless/spec/FlowLess.g:230:4: ( ' ' | '\\t' | '\\n' | '\\r' )
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
    // $ANTLR end "WS_"

    // $ANTLR start "IDENT"
    public final void mIDENT() throws RecognitionException {
        try {
            int _type = IDENT;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // /Users/jgf/Documents/Projects/mojo/wala/joana.wala.flowless/src/edu/kit/joana/wala/flowless/spec/FlowLess.g:238:2: ( ( 'a' .. 'z' | 'A' .. 'Z' | '_' ) ( 'a' .. 'z' | 'A' .. 'Z' | '_' | '0' .. '9' )* )
            // /Users/jgf/Documents/Projects/mojo/wala/joana.wala.flowless/src/edu/kit/joana/wala/flowless/spec/FlowLess.g:238:4: ( 'a' .. 'z' | 'A' .. 'Z' | '_' ) ( 'a' .. 'z' | 'A' .. 'Z' | '_' | '0' .. '9' )*
            {
            if ( (input.LA(1)>='A' && input.LA(1)<='Z')||input.LA(1)=='_'||(input.LA(1)>='a' && input.LA(1)<='z') ) {
                input.consume();

            }
            else {
                MismatchedSetException mse = new MismatchedSetException(null,input);
                recover(mse);
                throw mse;}

            // /Users/jgf/Documents/Projects/mojo/wala/joana.wala.flowless/src/edu/kit/joana/wala/flowless/spec/FlowLess.g:238:28: ( 'a' .. 'z' | 'A' .. 'Z' | '_' | '0' .. '9' )*
            loop1:
            do {
                int alt1=2;
                int LA1_0 = input.LA(1);

                if ( ((LA1_0>='0' && LA1_0<='9')||(LA1_0>='A' && LA1_0<='Z')||LA1_0=='_'||(LA1_0>='a' && LA1_0<='z')) ) {
                    alt1=1;
                }


                switch (alt1) {
            	case 1 :
            	    // /Users/jgf/Documents/Projects/mojo/wala/joana.wala.flowless/src/edu/kit/joana/wala/flowless/spec/FlowLess.g:
            	    {
            	    if ( (input.LA(1)>='0' && input.LA(1)<='9')||(input.LA(1)>='A' && input.LA(1)<='Z')||input.LA(1)=='_'||(input.LA(1)>='a' && input.LA(1)<='z') ) {
            	        input.consume();

            	    }
            	    else {
            	        MismatchedSetException mse = new MismatchedSetException(null,input);
            	        recover(mse);
            	        throw mse;}


            	    }
            	    break;

            	default :
            	    break loop1;
                }
            } while (true);


            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        }
    }
    // $ANTLR end "IDENT"

    // $ANTLR start "WILDCARD"
    public final void mWILDCARD() throws RecognitionException {
        try {
            int _type = WILDCARD;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // /Users/jgf/Documents/Projects/mojo/wala/joana.wala.flowless/src/edu/kit/joana/wala/flowless/spec/FlowLess.g:242:2: ( '*' )
            // /Users/jgf/Documents/Projects/mojo/wala/joana.wala.flowless/src/edu/kit/joana/wala/flowless/spec/FlowLess.g:242:4: '*'
            {
            match('*'); 

            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        }
    }
    // $ANTLR end "WILDCARD"

    // $ANTLR start "NOT_ARROW"
    public final void mNOT_ARROW() throws RecognitionException {
        try {
            int _type = NOT_ARROW;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // /Users/jgf/Documents/Projects/mojo/wala/joana.wala.flowless/src/edu/kit/joana/wala/flowless/spec/FlowLess.g:246:3: ( ( '-!>' | '\\u21F8' | '\\u219B' ) )
            // /Users/jgf/Documents/Projects/mojo/wala/joana.wala.flowless/src/edu/kit/joana/wala/flowless/spec/FlowLess.g:246:5: ( '-!>' | '\\u21F8' | '\\u219B' )
            {
            // /Users/jgf/Documents/Projects/mojo/wala/joana.wala.flowless/src/edu/kit/joana/wala/flowless/spec/FlowLess.g:246:5: ( '-!>' | '\\u21F8' | '\\u219B' )
            int alt2=3;
            switch ( input.LA(1) ) {
            case '-':
                {
                alt2=1;
                }
                break;
            case '\u21F8':
                {
                alt2=2;
                }
                break;
            case '\u219B':
                {
                alt2=3;
                }
                break;
            default:
                NoViableAltException nvae =
                    new NoViableAltException("", 2, 0, input);

                throw nvae;
            }

            switch (alt2) {
                case 1 :
                    // /Users/jgf/Documents/Projects/mojo/wala/joana.wala.flowless/src/edu/kit/joana/wala/flowless/spec/FlowLess.g:246:6: '-!>'
                    {
                    match("-!>"); 


                    }
                    break;
                case 2 :
                    // /Users/jgf/Documents/Projects/mojo/wala/joana.wala.flowless/src/edu/kit/joana/wala/flowless/spec/FlowLess.g:246:14: '\\u21F8'
                    {
                    match('\u21F8'); 

                    }
                    break;
                case 3 :
                    // /Users/jgf/Documents/Projects/mojo/wala/joana.wala.flowless/src/edu/kit/joana/wala/flowless/spec/FlowLess.g:246:25: '\\u219B'
                    {
                    match('\u219B'); 

                    }
                    break;

            }


            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        }
    }
    // $ANTLR end "NOT_ARROW"

    // $ANTLR start "ARROW"
    public final void mARROW() throws RecognitionException {
        try {
            int _type = ARROW;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // /Users/jgf/Documents/Projects/mojo/wala/joana.wala.flowless/src/edu/kit/joana/wala/flowless/spec/FlowLess.g:250:3: ( ( '->' | '\\u2192' ) )
            // /Users/jgf/Documents/Projects/mojo/wala/joana.wala.flowless/src/edu/kit/joana/wala/flowless/spec/FlowLess.g:250:5: ( '->' | '\\u2192' )
            {
            // /Users/jgf/Documents/Projects/mojo/wala/joana.wala.flowless/src/edu/kit/joana/wala/flowless/spec/FlowLess.g:250:5: ( '->' | '\\u2192' )
            int alt3=2;
            int LA3_0 = input.LA(1);

            if ( (LA3_0=='-') ) {
                alt3=1;
            }
            else if ( (LA3_0=='\u2192') ) {
                alt3=2;
            }
            else {
                NoViableAltException nvae =
                    new NoViableAltException("", 3, 0, input);

                throw nvae;
            }
            switch (alt3) {
                case 1 :
                    // /Users/jgf/Documents/Projects/mojo/wala/joana.wala.flowless/src/edu/kit/joana/wala/flowless/spec/FlowLess.g:250:6: '->'
                    {
                    match("->"); 


                    }
                    break;
                case 2 :
                    // /Users/jgf/Documents/Projects/mojo/wala/joana.wala.flowless/src/edu/kit/joana/wala/flowless/spec/FlowLess.g:250:13: '\\u2192'
                    {
                    match('\u2192'); 

                    }
                    break;

            }


            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        }
    }
    // $ANTLR end "ARROW"

    // $ANTLR start "IMPLIES"
    public final void mIMPLIES() throws RecognitionException {
        try {
            int _type = IMPLIES;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // /Users/jgf/Documents/Projects/mojo/wala/joana.wala.flowless/src/edu/kit/joana/wala/flowless/spec/FlowLess.g:254:3: ( ( '=>' | '\\u21D2' ) )
            // /Users/jgf/Documents/Projects/mojo/wala/joana.wala.flowless/src/edu/kit/joana/wala/flowless/spec/FlowLess.g:254:5: ( '=>' | '\\u21D2' )
            {
            // /Users/jgf/Documents/Projects/mojo/wala/joana.wala.flowless/src/edu/kit/joana/wala/flowless/spec/FlowLess.g:254:5: ( '=>' | '\\u21D2' )
            int alt4=2;
            int LA4_0 = input.LA(1);

            if ( (LA4_0=='=') ) {
                alt4=1;
            }
            else if ( (LA4_0=='\u21D2') ) {
                alt4=2;
            }
            else {
                NoViableAltException nvae =
                    new NoViableAltException("", 4, 0, input);

                throw nvae;
            }
            switch (alt4) {
                case 1 :
                    // /Users/jgf/Documents/Projects/mojo/wala/joana.wala.flowless/src/edu/kit/joana/wala/flowless/spec/FlowLess.g:254:6: '=>'
                    {
                    match("=>"); 


                    }
                    break;
                case 2 :
                    // /Users/jgf/Documents/Projects/mojo/wala/joana.wala.flowless/src/edu/kit/joana/wala/flowless/spec/FlowLess.g:254:13: '\\u21D2'
                    {
                    match('\u21D2'); 

                    }
                    break;

            }


            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        }
    }
    // $ANTLR end "IMPLIES"

    // $ANTLR start "ALIAS_OR"
    public final void mALIAS_OR() throws RecognitionException {
        try {
            int _type = ALIAS_OR;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // /Users/jgf/Documents/Projects/mojo/wala/joana.wala.flowless/src/edu/kit/joana/wala/flowless/spec/FlowLess.g:258:3: ( ( '||' | '|' | '\\u2228' ) )
            // /Users/jgf/Documents/Projects/mojo/wala/joana.wala.flowless/src/edu/kit/joana/wala/flowless/spec/FlowLess.g:258:5: ( '||' | '|' | '\\u2228' )
            {
            // /Users/jgf/Documents/Projects/mojo/wala/joana.wala.flowless/src/edu/kit/joana/wala/flowless/spec/FlowLess.g:258:5: ( '||' | '|' | '\\u2228' )
            int alt5=3;
            int LA5_0 = input.LA(1);

            if ( (LA5_0=='|') ) {
                int LA5_1 = input.LA(2);

                if ( (LA5_1=='|') ) {
                    alt5=1;
                }
                else {
                    alt5=2;}
            }
            else if ( (LA5_0=='\u2228') ) {
                alt5=3;
            }
            else {
                NoViableAltException nvae =
                    new NoViableAltException("", 5, 0, input);

                throw nvae;
            }
            switch (alt5) {
                case 1 :
                    // /Users/jgf/Documents/Projects/mojo/wala/joana.wala.flowless/src/edu/kit/joana/wala/flowless/spec/FlowLess.g:258:6: '||'
                    {
                    match("||"); 


                    }
                    break;
                case 2 :
                    // /Users/jgf/Documents/Projects/mojo/wala/joana.wala.flowless/src/edu/kit/joana/wala/flowless/spec/FlowLess.g:258:13: '|'
                    {
                    match('|'); 

                    }
                    break;
                case 3 :
                    // /Users/jgf/Documents/Projects/mojo/wala/joana.wala.flowless/src/edu/kit/joana/wala/flowless/spec/FlowLess.g:258:19: '\\u2228'
                    {
                    match('\u2228'); 

                    }
                    break;

            }


            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        }
    }
    // $ANTLR end "ALIAS_OR"

    // $ANTLR start "ALIAS_AND"
    public final void mALIAS_AND() throws RecognitionException {
        try {
            int _type = ALIAS_AND;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // /Users/jgf/Documents/Projects/mojo/wala/joana.wala.flowless/src/edu/kit/joana/wala/flowless/spec/FlowLess.g:262:3: ( ( '&&' | '&' | '\\u2227' ) )
            // /Users/jgf/Documents/Projects/mojo/wala/joana.wala.flowless/src/edu/kit/joana/wala/flowless/spec/FlowLess.g:262:5: ( '&&' | '&' | '\\u2227' )
            {
            // /Users/jgf/Documents/Projects/mojo/wala/joana.wala.flowless/src/edu/kit/joana/wala/flowless/spec/FlowLess.g:262:5: ( '&&' | '&' | '\\u2227' )
            int alt6=3;
            int LA6_0 = input.LA(1);

            if ( (LA6_0=='&') ) {
                int LA6_1 = input.LA(2);

                if ( (LA6_1=='&') ) {
                    alt6=1;
                }
                else {
                    alt6=2;}
            }
            else if ( (LA6_0=='\u2227') ) {
                alt6=3;
            }
            else {
                NoViableAltException nvae =
                    new NoViableAltException("", 6, 0, input);

                throw nvae;
            }
            switch (alt6) {
                case 1 :
                    // /Users/jgf/Documents/Projects/mojo/wala/joana.wala.flowless/src/edu/kit/joana/wala/flowless/spec/FlowLess.g:262:6: '&&'
                    {
                    match("&&"); 


                    }
                    break;
                case 2 :
                    // /Users/jgf/Documents/Projects/mojo/wala/joana.wala.flowless/src/edu/kit/joana/wala/flowless/spec/FlowLess.g:262:13: '&'
                    {
                    match('&'); 

                    }
                    break;
                case 3 :
                    // /Users/jgf/Documents/Projects/mojo/wala/joana.wala.flowless/src/edu/kit/joana/wala/flowless/spec/FlowLess.g:262:19: '\\u2227'
                    {
                    match('\u2227'); 

                    }
                    break;

            }


            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        }
    }
    // $ANTLR end "ALIAS_AND"

    // $ANTLR start "ALIAS_NOT"
    public final void mALIAS_NOT() throws RecognitionException {
        try {
            int _type = ALIAS_NOT;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // /Users/jgf/Documents/Projects/mojo/wala/joana.wala.flowless/src/edu/kit/joana/wala/flowless/spec/FlowLess.g:266:3: ( ( '!' | '\\u00AC' ) )
            // /Users/jgf/Documents/Projects/mojo/wala/joana.wala.flowless/src/edu/kit/joana/wala/flowless/spec/FlowLess.g:266:5: ( '!' | '\\u00AC' )
            {
            if ( input.LA(1)=='!'||input.LA(1)=='\u00AC' ) {
                input.consume();

            }
            else {
                MismatchedSetException mse = new MismatchedSetException(null,input);
                recover(mse);
                throw mse;}


            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        }
    }
    // $ANTLR end "ALIAS_NOT"

    // $ANTLR start "RESULT"
    public final void mRESULT() throws RecognitionException {
        try {
            int _type = RESULT;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // /Users/jgf/Documents/Projects/mojo/wala/joana.wala.flowless/src/edu/kit/joana/wala/flowless/spec/FlowLess.g:270:3: ( ( '\\\\result' | '\\\\ret' | '\\\\return' | '\\\\res' ) )
            // /Users/jgf/Documents/Projects/mojo/wala/joana.wala.flowless/src/edu/kit/joana/wala/flowless/spec/FlowLess.g:270:5: ( '\\\\result' | '\\\\ret' | '\\\\return' | '\\\\res' )
            {
            // /Users/jgf/Documents/Projects/mojo/wala/joana.wala.flowless/src/edu/kit/joana/wala/flowless/spec/FlowLess.g:270:5: ( '\\\\result' | '\\\\ret' | '\\\\return' | '\\\\res' )
            int alt7=4;
            alt7 = dfa7.predict(input);
            switch (alt7) {
                case 1 :
                    // /Users/jgf/Documents/Projects/mojo/wala/joana.wala.flowless/src/edu/kit/joana/wala/flowless/spec/FlowLess.g:270:6: '\\\\result'
                    {
                    match("\\result"); 


                    }
                    break;
                case 2 :
                    // /Users/jgf/Documents/Projects/mojo/wala/joana.wala.flowless/src/edu/kit/joana/wala/flowless/spec/FlowLess.g:270:19: '\\\\ret'
                    {
                    match("\\ret"); 


                    }
                    break;
                case 3 :
                    // /Users/jgf/Documents/Projects/mojo/wala/joana.wala.flowless/src/edu/kit/joana/wala/flowless/spec/FlowLess.g:270:29: '\\\\return'
                    {
                    match("\\return"); 


                    }
                    break;
                case 4 :
                    // /Users/jgf/Documents/Projects/mojo/wala/joana.wala.flowless/src/edu/kit/joana/wala/flowless/spec/FlowLess.g:270:42: '\\\\res'
                    {
                    match("\\res"); 


                    }
                    break;

            }


            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        }
    }
    // $ANTLR end "RESULT"

    // $ANTLR start "EXC"
    public final void mEXC() throws RecognitionException {
        try {
            int _type = EXC;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // /Users/jgf/Documents/Projects/mojo/wala/joana.wala.flowless/src/edu/kit/joana/wala/flowless/spec/FlowLess.g:274:3: ( ( '\\\\exc' | '\\\\e' | '\\\\exception' ) )
            // /Users/jgf/Documents/Projects/mojo/wala/joana.wala.flowless/src/edu/kit/joana/wala/flowless/spec/FlowLess.g:274:5: ( '\\\\exc' | '\\\\e' | '\\\\exception' )
            {
            // /Users/jgf/Documents/Projects/mojo/wala/joana.wala.flowless/src/edu/kit/joana/wala/flowless/spec/FlowLess.g:274:5: ( '\\\\exc' | '\\\\e' | '\\\\exception' )
            int alt8=3;
            int LA8_0 = input.LA(1);

            if ( (LA8_0=='\\') ) {
                int LA8_1 = input.LA(2);

                if ( (LA8_1=='e') ) {
                    int LA8_2 = input.LA(3);

                    if ( (LA8_2=='x') ) {
                        int LA8_3 = input.LA(4);

                        if ( (LA8_3=='c') ) {
                            int LA8_5 = input.LA(5);

                            if ( (LA8_5=='e') ) {
                                alt8=3;
                            }
                            else {
                                alt8=1;}
                        }
                        else {
                            NoViableAltException nvae =
                                new NoViableAltException("", 8, 3, input);

                            throw nvae;
                        }
                    }
                    else {
                        alt8=2;}
                }
                else {
                    NoViableAltException nvae =
                        new NoViableAltException("", 8, 1, input);

                    throw nvae;
                }
            }
            else {
                NoViableAltException nvae =
                    new NoViableAltException("", 8, 0, input);

                throw nvae;
            }
            switch (alt8) {
                case 1 :
                    // /Users/jgf/Documents/Projects/mojo/wala/joana.wala.flowless/src/edu/kit/joana/wala/flowless/spec/FlowLess.g:274:6: '\\\\exc'
                    {
                    match("\\exc"); 


                    }
                    break;
                case 2 :
                    // /Users/jgf/Documents/Projects/mojo/wala/joana.wala.flowless/src/edu/kit/joana/wala/flowless/spec/FlowLess.g:274:16: '\\\\e'
                    {
                    match("\\e"); 


                    }
                    break;
                case 3 :
                    // /Users/jgf/Documents/Projects/mojo/wala/joana.wala.flowless/src/edu/kit/joana/wala/flowless/spec/FlowLess.g:274:24: '\\\\exception'
                    {
                    match("\\exception"); 


                    }
                    break;

            }


            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        }
    }
    // $ANTLR end "EXC"

    // $ANTLR start "STATE"
    public final void mSTATE() throws RecognitionException {
        try {
            int _type = STATE;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // /Users/jgf/Documents/Projects/mojo/wala/joana.wala.flowless/src/edu/kit/joana/wala/flowless/spec/FlowLess.g:278:3: ( ( '\\\\state' | '\\\\s' | '\\\\stat' ) )
            // /Users/jgf/Documents/Projects/mojo/wala/joana.wala.flowless/src/edu/kit/joana/wala/flowless/spec/FlowLess.g:278:5: ( '\\\\state' | '\\\\s' | '\\\\stat' )
            {
            // /Users/jgf/Documents/Projects/mojo/wala/joana.wala.flowless/src/edu/kit/joana/wala/flowless/spec/FlowLess.g:278:5: ( '\\\\state' | '\\\\s' | '\\\\stat' )
            int alt9=3;
            int LA9_0 = input.LA(1);

            if ( (LA9_0=='\\') ) {
                int LA9_1 = input.LA(2);

                if ( (LA9_1=='s') ) {
                    int LA9_2 = input.LA(3);

                    if ( (LA9_2=='t') ) {
                        int LA9_3 = input.LA(4);

                        if ( (LA9_3=='a') ) {
                            int LA9_5 = input.LA(5);

                            if ( (LA9_5=='t') ) {
                                int LA9_6 = input.LA(6);

                                if ( (LA9_6=='e') ) {
                                    alt9=1;
                                }
                                else {
                                    alt9=3;}
                            }
                            else {
                                NoViableAltException nvae =
                                    new NoViableAltException("", 9, 5, input);

                                throw nvae;
                            }
                        }
                        else {
                            NoViableAltException nvae =
                                new NoViableAltException("", 9, 3, input);

                            throw nvae;
                        }
                    }
                    else {
                        alt9=2;}
                }
                else {
                    NoViableAltException nvae =
                        new NoViableAltException("", 9, 1, input);

                    throw nvae;
                }
            }
            else {
                NoViableAltException nvae =
                    new NoViableAltException("", 9, 0, input);

                throw nvae;
            }
            switch (alt9) {
                case 1 :
                    // /Users/jgf/Documents/Projects/mojo/wala/joana.wala.flowless/src/edu/kit/joana/wala/flowless/spec/FlowLess.g:278:6: '\\\\state'
                    {
                    match("\\state"); 


                    }
                    break;
                case 2 :
                    // /Users/jgf/Documents/Projects/mojo/wala/joana.wala.flowless/src/edu/kit/joana/wala/flowless/spec/FlowLess.g:278:18: '\\\\s'
                    {
                    match("\\s"); 


                    }
                    break;
                case 3 :
                    // /Users/jgf/Documents/Projects/mojo/wala/joana.wala.flowless/src/edu/kit/joana/wala/flowless/spec/FlowLess.g:278:26: '\\\\stat'
                    {
                    match("\\stat"); 


                    }
                    break;

            }


            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        }
    }
    // $ANTLR end "STATE"

    // $ANTLR start "ARRAY"
    public final void mARRAY() throws RecognitionException {
        try {
            int _type = ARRAY;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // /Users/jgf/Documents/Projects/mojo/wala/joana.wala.flowless/src/edu/kit/joana/wala/flowless/spec/FlowLess.g:282:3: ( '[]' )
            // /Users/jgf/Documents/Projects/mojo/wala/joana.wala.flowless/src/edu/kit/joana/wala/flowless/spec/FlowLess.g:282:5: '[]'
            {
            match("[]"); 


            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        }
    }
    // $ANTLR end "ARRAY"

    public void mTokens() throws RecognitionException {
        // /Users/jgf/Documents/Projects/mojo/wala/joana.wala.flowless/src/edu/kit/joana/wala/flowless/spec/FlowLess.g:1:8: ( T__17 | T__18 | T__19 | T__20 | T__21 | T__22 | T__23 | T__24 | T__25 | T__26 | T__27 | T__28 | T__29 | T__30 | WS_ | IDENT | WILDCARD | NOT_ARROW | ARROW | IMPLIES | ALIAS_OR | ALIAS_AND | ALIAS_NOT | RESULT | EXC | STATE | ARRAY )
        int alt10=27;
        alt10 = dfa10.predict(input);
        switch (alt10) {
            case 1 :
                // /Users/jgf/Documents/Projects/mojo/wala/joana.wala.flowless/src/edu/kit/joana/wala/flowless/spec/FlowLess.g:1:10: T__17
                {
                mT__17(); 

                }
                break;
            case 2 :
                // /Users/jgf/Documents/Projects/mojo/wala/joana.wala.flowless/src/edu/kit/joana/wala/flowless/spec/FlowLess.g:1:16: T__18
                {
                mT__18(); 

                }
                break;
            case 3 :
                // /Users/jgf/Documents/Projects/mojo/wala/joana.wala.flowless/src/edu/kit/joana/wala/flowless/spec/FlowLess.g:1:22: T__19
                {
                mT__19(); 

                }
                break;
            case 4 :
                // /Users/jgf/Documents/Projects/mojo/wala/joana.wala.flowless/src/edu/kit/joana/wala/flowless/spec/FlowLess.g:1:28: T__20
                {
                mT__20(); 

                }
                break;
            case 5 :
                // /Users/jgf/Documents/Projects/mojo/wala/joana.wala.flowless/src/edu/kit/joana/wala/flowless/spec/FlowLess.g:1:34: T__21
                {
                mT__21(); 

                }
                break;
            case 6 :
                // /Users/jgf/Documents/Projects/mojo/wala/joana.wala.flowless/src/edu/kit/joana/wala/flowless/spec/FlowLess.g:1:40: T__22
                {
                mT__22(); 

                }
                break;
            case 7 :
                // /Users/jgf/Documents/Projects/mojo/wala/joana.wala.flowless/src/edu/kit/joana/wala/flowless/spec/FlowLess.g:1:46: T__23
                {
                mT__23(); 

                }
                break;
            case 8 :
                // /Users/jgf/Documents/Projects/mojo/wala/joana.wala.flowless/src/edu/kit/joana/wala/flowless/spec/FlowLess.g:1:52: T__24
                {
                mT__24(); 

                }
                break;
            case 9 :
                // /Users/jgf/Documents/Projects/mojo/wala/joana.wala.flowless/src/edu/kit/joana/wala/flowless/spec/FlowLess.g:1:58: T__25
                {
                mT__25(); 

                }
                break;
            case 10 :
                // /Users/jgf/Documents/Projects/mojo/wala/joana.wala.flowless/src/edu/kit/joana/wala/flowless/spec/FlowLess.g:1:64: T__26
                {
                mT__26(); 

                }
                break;
            case 11 :
                // /Users/jgf/Documents/Projects/mojo/wala/joana.wala.flowless/src/edu/kit/joana/wala/flowless/spec/FlowLess.g:1:70: T__27
                {
                mT__27(); 

                }
                break;
            case 12 :
                // /Users/jgf/Documents/Projects/mojo/wala/joana.wala.flowless/src/edu/kit/joana/wala/flowless/spec/FlowLess.g:1:76: T__28
                {
                mT__28(); 

                }
                break;
            case 13 :
                // /Users/jgf/Documents/Projects/mojo/wala/joana.wala.flowless/src/edu/kit/joana/wala/flowless/spec/FlowLess.g:1:82: T__29
                {
                mT__29(); 

                }
                break;
            case 14 :
                // /Users/jgf/Documents/Projects/mojo/wala/joana.wala.flowless/src/edu/kit/joana/wala/flowless/spec/FlowLess.g:1:88: T__30
                {
                mT__30(); 

                }
                break;
            case 15 :
                // /Users/jgf/Documents/Projects/mojo/wala/joana.wala.flowless/src/edu/kit/joana/wala/flowless/spec/FlowLess.g:1:94: WS_
                {
                mWS_(); 

                }
                break;
            case 16 :
                // /Users/jgf/Documents/Projects/mojo/wala/joana.wala.flowless/src/edu/kit/joana/wala/flowless/spec/FlowLess.g:1:98: IDENT
                {
                mIDENT(); 

                }
                break;
            case 17 :
                // /Users/jgf/Documents/Projects/mojo/wala/joana.wala.flowless/src/edu/kit/joana/wala/flowless/spec/FlowLess.g:1:104: WILDCARD
                {
                mWILDCARD(); 

                }
                break;
            case 18 :
                // /Users/jgf/Documents/Projects/mojo/wala/joana.wala.flowless/src/edu/kit/joana/wala/flowless/spec/FlowLess.g:1:113: NOT_ARROW
                {
                mNOT_ARROW(); 

                }
                break;
            case 19 :
                // /Users/jgf/Documents/Projects/mojo/wala/joana.wala.flowless/src/edu/kit/joana/wala/flowless/spec/FlowLess.g:1:123: ARROW
                {
                mARROW(); 

                }
                break;
            case 20 :
                // /Users/jgf/Documents/Projects/mojo/wala/joana.wala.flowless/src/edu/kit/joana/wala/flowless/spec/FlowLess.g:1:129: IMPLIES
                {
                mIMPLIES(); 

                }
                break;
            case 21 :
                // /Users/jgf/Documents/Projects/mojo/wala/joana.wala.flowless/src/edu/kit/joana/wala/flowless/spec/FlowLess.g:1:137: ALIAS_OR
                {
                mALIAS_OR(); 

                }
                break;
            case 22 :
                // /Users/jgf/Documents/Projects/mojo/wala/joana.wala.flowless/src/edu/kit/joana/wala/flowless/spec/FlowLess.g:1:146: ALIAS_AND
                {
                mALIAS_AND(); 

                }
                break;
            case 23 :
                // /Users/jgf/Documents/Projects/mojo/wala/joana.wala.flowless/src/edu/kit/joana/wala/flowless/spec/FlowLess.g:1:156: ALIAS_NOT
                {
                mALIAS_NOT(); 

                }
                break;
            case 24 :
                // /Users/jgf/Documents/Projects/mojo/wala/joana.wala.flowless/src/edu/kit/joana/wala/flowless/spec/FlowLess.g:1:166: RESULT
                {
                mRESULT(); 

                }
                break;
            case 25 :
                // /Users/jgf/Documents/Projects/mojo/wala/joana.wala.flowless/src/edu/kit/joana/wala/flowless/spec/FlowLess.g:1:173: EXC
                {
                mEXC(); 

                }
                break;
            case 26 :
                // /Users/jgf/Documents/Projects/mojo/wala/joana.wala.flowless/src/edu/kit/joana/wala/flowless/spec/FlowLess.g:1:177: STATE
                {
                mSTATE(); 

                }
                break;
            case 27 :
                // /Users/jgf/Documents/Projects/mojo/wala/joana.wala.flowless/src/edu/kit/joana/wala/flowless/spec/FlowLess.g:1:183: ARRAY
                {
                mARRAY(); 

                }
                break;

        }

    }


    protected DFA7 dfa7 = new DFA7(this);
    protected DFA10 dfa10 = new DFA10(this);
    static final String DFA7_eotS =
        "\4\uffff\1\7\1\11\4\uffff";
    static final String DFA7_eofS =
        "\12\uffff";
    static final String DFA7_minS =
        "\1\134\1\162\1\145\1\163\2\165\4\uffff";
    static final String DFA7_maxS =
        "\1\134\1\162\1\145\1\164\2\165\4\uffff";
    static final String DFA7_acceptS =
        "\6\uffff\1\1\1\4\1\3\1\2";
    static final String DFA7_specialS =
        "\12\uffff}>";
    static final String[] DFA7_transitionS = {
            "\1\1",
            "\1\2",
            "\1\3",
            "\1\4\1\5",
            "\1\6",
            "\1\10",
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
            return "270:5: ( '\\\\result' | '\\\\ret' | '\\\\return' | '\\\\res' )";
        }
    }
    static final String DFA10_eotS =
        "\2\uffff\1\17\4\uffff\1\17\2\uffff\1\17\1\uffff\1\35\14\uffff\3"+
        "\17\5\uffff\4\17\1\51\1\52\1\53\1\17\3\uffff\1\55\1\uffff";
    static final String DFA10_eofS =
        "\56\uffff";
    static final String DFA10_minS =
        "\1\11\1\uffff\1\154\4\uffff\1\156\2\uffff\1\165\1\uffff\1\135\4"+
        "\uffff\1\41\6\uffff\1\145\2\151\1\162\5\uffff\1\141\1\161\1\145"+
        "\1\163\3\60\1\145\3\uffff\1\60\1\uffff";
    static final String DFA10_maxS =
        "\1\u2228\1\uffff\1\154\4\uffff\1\156\2\uffff\1\165\1\uffff\1\135"+
        "\4\uffff\1\76\6\uffff\1\163\2\151\1\162\5\uffff\1\141\1\161\1\145"+
        "\1\163\3\172\1\145\3\uffff\1\172\1\uffff";
    static final String DFA10_acceptS =
        "\1\uffff\1\1\1\uffff\1\3\1\4\1\5\1\6\1\uffff\1\10\1\12\1\uffff\1"+
        "\14\1\uffff\1\16\1\17\1\20\1\21\1\uffff\1\22\1\23\1\24\1\25\1\26"+
        "\1\27\4\uffff\1\33\1\15\1\30\1\31\1\32\10\uffff\1\11\1\13\1\2\1"+
        "\uffff\1\7";
    static final String DFA10_specialS =
        "\56\uffff}>";
    static final String[] DFA10_transitionS = {
            "\2\16\2\uffff\1\16\22\uffff\1\16\1\27\4\uffff\1\26\1\uffff\1"+
            "\3\1\4\1\20\1\uffff\1\11\1\21\1\13\2\uffff\1\10\13\uffff\1\24"+
            "\1\uffff\1\1\1\uffff\32\17\1\14\1\30\1\15\1\uffff\1\17\1\uffff"+
            "\1\2\16\17\1\12\4\17\1\7\5\17\1\5\1\25\1\6\56\uffff\1\27\u20e5"+
            "\uffff\1\23\10\uffff\1\22\66\uffff\1\24\45\uffff\1\22\56\uffff"+
            "\1\26\1\25",
            "",
            "\1\31",
            "",
            "",
            "",
            "",
            "\1\32",
            "",
            "",
            "\1\33",
            "",
            "\1\34",
            "",
            "",
            "",
            "",
            "\1\22\34\uffff\1\23",
            "",
            "",
            "",
            "",
            "",
            "",
            "\1\37\14\uffff\1\36\1\40",
            "\1\41",
            "\1\42",
            "\1\43",
            "",
            "",
            "",
            "",
            "",
            "\1\44",
            "\1\45",
            "\1\46",
            "\1\47",
            "\12\17\7\uffff\32\17\4\uffff\1\17\1\uffff\24\17\1\50\5\17",
            "\12\17\7\uffff\32\17\4\uffff\1\17\1\uffff\32\17",
            "\12\17\7\uffff\32\17\4\uffff\1\17\1\uffff\32\17",
            "\1\54",
            "",
            "",
            "",
            "\12\17\7\uffff\32\17\4\uffff\1\17\1\uffff\32\17",
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
            return "1:1: Tokens : ( T__17 | T__18 | T__19 | T__20 | T__21 | T__22 | T__23 | T__24 | T__25 | T__26 | T__27 | T__28 | T__29 | T__30 | WS_ | IDENT | WILDCARD | NOT_ARROW | ARROW | IMPLIES | ALIAS_OR | ALIAS_AND | ALIAS_NOT | RESULT | EXC | STATE | ARRAY );";
        }
    }
 

}