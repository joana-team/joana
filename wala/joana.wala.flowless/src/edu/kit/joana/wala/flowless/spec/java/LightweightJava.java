// $ANTLR 3.1.3 Mar 17, 2009 19:23:44 /Users/jgf/Documents/Projects/joana/wala/joana.wala.flowless/src/edu/kit/joana/wala/flowless/spec/java/LightweightJava.g 2013-03-11 18:55:30
/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.wala.flowless.spec.java;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.Map;


import org.antlr.runtime.*;

/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
public class LightweightJava extends Lexer {
    public static final int PACKAGE=30;
    public static final int CLASS=26;
    public static final int LT=19;
    public static final int KEYWORD=27;
    public static final int LINE_COMMENT=6;
    public static final int STATIC=28;
    public static final int CHAR_ESC=10;
    public static final int ANNOTATION=31;
    public static final int CHAR=11;
    public static final int INT=12;
    public static final int DP=23;
    public static final int ARRAY_DEF=16;
    public static final int BR_OPEN=24;
    public static final int EOF=-1;
    public static final int SC=4;
    public static final int BRACK_START=17;
    public static final int EXP_OP=34;
    public static final int WS_=5;
    public static final int BRACK_END=18;
    public static final int BLC_END=8;
    public static final int COMMA=21;
    public static final int PARAM_END=15;
    public static final int ASSIGN=33;
    public static final int BLC_START=7;
    public static final int PARAM_START=14;
    public static final int BLOCK_COMMENT=9;
    public static final int IDENT=32;
    public static final int GT=20;
    public static final int INTERFACE=29;
    public static final int BR_CLOSE=25;
    public static final int DOT=22;
    public static final int STRING=13;

      @Override
      public void reportError(RecognitionException e) {
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
      
      private static Map<Integer, String> id2name = buildTokenNameMap(LightweightJava.class);
      
      public static Map<Integer, String> buildTokenNameMap(Class<?> cl) {
        Map<Integer, String> id2name = new HashMap<Integer, String>();
        
        Field[] fields = cl.getDeclaredFields();
        for (Field f : fields) {
          final int mod = f.getModifiers();
          if (Modifier.isPublic(mod) && Modifier.isStatic(mod) && f.getType() == int.class) {
            try {
              final int id = f.getInt(cl);
              final String name = f.getName();
              id2name.put(id, name);
            } catch (IllegalArgumentException exc) {
              Thrower.sneakyThrow(exc);
            } catch (IllegalAccessException exc) {
              Thrower.sneakyThrow(exc);
            }
          }
        }
        
        return id2name;
      }
      
      public static String getTokenName(int tokId) {
        return id2name.get(tokId);
      }


    // delegates
    // delegators

    public LightweightJava() {;} 
    public LightweightJava(CharStream input) {
        this(input, new RecognizerSharedState());
    }
    public LightweightJava(CharStream input, RecognizerSharedState state) {
        super(input,state);

    }
    public String getGrammarFileName() { return "/Users/jgf/Documents/Projects/joana/wala/joana.wala.flowless/src/edu/kit/joana/wala/flowless/spec/java/LightweightJava.g"; }

    // $ANTLR start "SC"
    public final void mSC() throws RecognitionException {
        try {
            int _type = SC;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // /Users/jgf/Documents/Projects/joana/wala/joana.wala.flowless/src/edu/kit/joana/wala/flowless/spec/java/LightweightJava.g:91:3: ( ';' )
            // /Users/jgf/Documents/Projects/joana/wala/joana.wala.flowless/src/edu/kit/joana/wala/flowless/spec/java/LightweightJava.g:91:5: ';'
            {
            match(';'); 

            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        }
    }
    // $ANTLR end "SC"

    // $ANTLR start "WS_"
    public final void mWS_() throws RecognitionException {
        try {
            int _type = WS_;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // /Users/jgf/Documents/Projects/joana/wala/joana.wala.flowless/src/edu/kit/joana/wala/flowless/spec/java/LightweightJava.g:94:3: ( ( ' ' | '\\t' | '\\n' | '\\r' | '\\u000C' ) )
            // /Users/jgf/Documents/Projects/joana/wala/joana.wala.flowless/src/edu/kit/joana/wala/flowless/spec/java/LightweightJava.g:94:5: ( ' ' | '\\t' | '\\n' | '\\r' | '\\u000C' )
            {
            if ( (input.LA(1)>='\t' && input.LA(1)<='\n')||(input.LA(1)>='\f' && input.LA(1)<='\r')||input.LA(1)==' ' ) {
                input.consume();

            }
            else {
                MismatchedSetException mse = new MismatchedSetException(null,input);
                recover(mse);
                throw mse;}

             _channel = HIDDEN; 

            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        }
    }
    // $ANTLR end "WS_"

    // $ANTLR start "LINE_COMMENT"
    public final void mLINE_COMMENT() throws RecognitionException {
        try {
            int _type = LINE_COMMENT;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // /Users/jgf/Documents/Projects/joana/wala/joana.wala.flowless/src/edu/kit/joana/wala/flowless/spec/java/LightweightJava.g:102:14: ( '//' (~ '\\n' )* ( '\\n' | EOF ) )
            // /Users/jgf/Documents/Projects/joana/wala/joana.wala.flowless/src/edu/kit/joana/wala/flowless/spec/java/LightweightJava.g:102:16: '//' (~ '\\n' )* ( '\\n' | EOF )
            {
            match("//"); 

            // /Users/jgf/Documents/Projects/joana/wala/joana.wala.flowless/src/edu/kit/joana/wala/flowless/spec/java/LightweightJava.g:102:21: (~ '\\n' )*
            loop1:
            do {
                int alt1=2;
                int LA1_0 = input.LA(1);

                if ( ((LA1_0>='\u0000' && LA1_0<='\t')||(LA1_0>='\u000B' && LA1_0<='\uFFFF')) ) {
                    alt1=1;
                }


                switch (alt1) {
            	case 1 :
            	    // /Users/jgf/Documents/Projects/joana/wala/joana.wala.flowless/src/edu/kit/joana/wala/flowless/spec/java/LightweightJava.g:102:22: ~ '\\n'
            	    {
            	    if ( (input.LA(1)>='\u0000' && input.LA(1)<='\t')||(input.LA(1)>='\u000B' && input.LA(1)<='\uFFFF') ) {
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

            // /Users/jgf/Documents/Projects/joana/wala/joana.wala.flowless/src/edu/kit/joana/wala/flowless/spec/java/LightweightJava.g:102:30: ( '\\n' | EOF )
            int alt2=2;
            int LA2_0 = input.LA(1);

            if ( (LA2_0=='\n') ) {
                alt2=1;
            }
            else {
                alt2=2;}
            switch (alt2) {
                case 1 :
                    // /Users/jgf/Documents/Projects/joana/wala/joana.wala.flowless/src/edu/kit/joana/wala/flowless/spec/java/LightweightJava.g:102:31: '\\n'
                    {
                    match('\n'); 

                    }
                    break;
                case 2 :
                    // /Users/jgf/Documents/Projects/joana/wala/joana.wala.flowless/src/edu/kit/joana/wala/flowless/spec/java/LightweightJava.g:102:38: EOF
                    {
                    match(EOF); 

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
    // $ANTLR end "LINE_COMMENT"

    // $ANTLR start "BLOCK_COMMENT"
    public final void mBLOCK_COMMENT() throws RecognitionException {
        try {
            int _type = BLOCK_COMMENT;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // /Users/jgf/Documents/Projects/joana/wala/joana.wala.flowless/src/edu/kit/joana/wala/flowless/spec/java/LightweightJava.g:104:15: ( BLC_START ( options {greedy=false; } : ~ ( BLC_END ) )* BLC_END )
            // /Users/jgf/Documents/Projects/joana/wala/joana.wala.flowless/src/edu/kit/joana/wala/flowless/spec/java/LightweightJava.g:104:17: BLC_START ( options {greedy=false; } : ~ ( BLC_END ) )* BLC_END
            {
            mBLC_START(); 
            // /Users/jgf/Documents/Projects/joana/wala/joana.wala.flowless/src/edu/kit/joana/wala/flowless/spec/java/LightweightJava.g:104:27: ( options {greedy=false; } : ~ ( BLC_END ) )*
            loop3:
            do {
                int alt3=2;
                int LA3_0 = input.LA(1);

                if ( (LA3_0=='*') ) {
                    int LA3_1 = input.LA(2);

                    if ( (LA3_1=='/') ) {
                        alt3=2;
                    }
                    else if ( ((LA3_1>='\u0000' && LA3_1<='.')||(LA3_1>='0' && LA3_1<='\uFFFF')) ) {
                        alt3=1;
                    }


                }
                else if ( ((LA3_0>='\u0000' && LA3_0<=')')||(LA3_0>='+' && LA3_0<='\uFFFF')) ) {
                    alt3=1;
                }


                switch (alt3) {
            	case 1 :
            	    // /Users/jgf/Documents/Projects/joana/wala/joana.wala.flowless/src/edu/kit/joana/wala/flowless/spec/java/LightweightJava.g:104:58: ~ ( BLC_END )
            	    {
            	    if ( (input.LA(1)>='\u0000' && input.LA(1)<='\uFFFF') ) {
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

            mBLC_END(); 

            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        }
    }
    // $ANTLR end "BLOCK_COMMENT"

    // $ANTLR start "BLC_START"
    public final void mBLC_START() throws RecognitionException {
        try {
            int _type = BLC_START;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // /Users/jgf/Documents/Projects/joana/wala/joana.wala.flowless/src/edu/kit/joana/wala/flowless/spec/java/LightweightJava.g:107:11: ( '/*' )
            // /Users/jgf/Documents/Projects/joana/wala/joana.wala.flowless/src/edu/kit/joana/wala/flowless/spec/java/LightweightJava.g:107:13: '/*'
            {
            match("/*"); 


            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        }
    }
    // $ANTLR end "BLC_START"

    // $ANTLR start "BLC_END"
    public final void mBLC_END() throws RecognitionException {
        try {
            int _type = BLC_END;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // /Users/jgf/Documents/Projects/joana/wala/joana.wala.flowless/src/edu/kit/joana/wala/flowless/spec/java/LightweightJava.g:110:9: ( '*/' )
            // /Users/jgf/Documents/Projects/joana/wala/joana.wala.flowless/src/edu/kit/joana/wala/flowless/spec/java/LightweightJava.g:110:11: '*/'
            {
            match("*/"); 


            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        }
    }
    // $ANTLR end "BLC_END"

    // $ANTLR start "CHAR"
    public final void mCHAR() throws RecognitionException {
        try {
            int _type = CHAR;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // /Users/jgf/Documents/Projects/joana/wala/joana.wala.flowless/src/edu/kit/joana/wala/flowless/spec/java/LightweightJava.g:113:6: ( '\\'' ( CHAR_ESC | ~ ( '\\'' | '\\\\' ) )+ '\\'' )
            // /Users/jgf/Documents/Projects/joana/wala/joana.wala.flowless/src/edu/kit/joana/wala/flowless/spec/java/LightweightJava.g:113:8: '\\'' ( CHAR_ESC | ~ ( '\\'' | '\\\\' ) )+ '\\''
            {
            match('\''); 
            // /Users/jgf/Documents/Projects/joana/wala/joana.wala.flowless/src/edu/kit/joana/wala/flowless/spec/java/LightweightJava.g:113:13: ( CHAR_ESC | ~ ( '\\'' | '\\\\' ) )+
            int cnt4=0;
            loop4:
            do {
                int alt4=3;
                int LA4_0 = input.LA(1);

                if ( (LA4_0=='\\') ) {
                    alt4=1;
                }
                else if ( ((LA4_0>='\u0000' && LA4_0<='&')||(LA4_0>='(' && LA4_0<='[')||(LA4_0>=']' && LA4_0<='\uFFFF')) ) {
                    alt4=2;
                }


                switch (alt4) {
            	case 1 :
            	    // /Users/jgf/Documents/Projects/joana/wala/joana.wala.flowless/src/edu/kit/joana/wala/flowless/spec/java/LightweightJava.g:113:15: CHAR_ESC
            	    {
            	    mCHAR_ESC(); 

            	    }
            	    break;
            	case 2 :
            	    // /Users/jgf/Documents/Projects/joana/wala/joana.wala.flowless/src/edu/kit/joana/wala/flowless/spec/java/LightweightJava.g:113:26: ~ ( '\\'' | '\\\\' )
            	    {
            	    if ( (input.LA(1)>='\u0000' && input.LA(1)<='&')||(input.LA(1)>='(' && input.LA(1)<='[')||(input.LA(1)>=']' && input.LA(1)<='\uFFFF') ) {
            	        input.consume();

            	    }
            	    else {
            	        MismatchedSetException mse = new MismatchedSetException(null,input);
            	        recover(mse);
            	        throw mse;}


            	    }
            	    break;

            	default :
            	    if ( cnt4 >= 1 ) break loop4;
                        EarlyExitException eee =
                            new EarlyExitException(4, input);
                        throw eee;
                }
                cnt4++;
            } while (true);

            match('\''); 

            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        }
    }
    // $ANTLR end "CHAR"

    // $ANTLR start "CHAR_ESC"
    public final void mCHAR_ESC() throws RecognitionException {
        try {
            int _type = CHAR_ESC;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // /Users/jgf/Documents/Projects/joana/wala/joana.wala.flowless/src/edu/kit/joana/wala/flowless/spec/java/LightweightJava.g:117:10: ( '\\\\' ( '\\'' | INT | 'a' .. 'z' | 'A' .. 'Z' | '\\\\' | '\\\"' ) )
            // /Users/jgf/Documents/Projects/joana/wala/joana.wala.flowless/src/edu/kit/joana/wala/flowless/spec/java/LightweightJava.g:117:12: '\\\\' ( '\\'' | INT | 'a' .. 'z' | 'A' .. 'Z' | '\\\\' | '\\\"' )
            {
            match('\\'); 
            // /Users/jgf/Documents/Projects/joana/wala/joana.wala.flowless/src/edu/kit/joana/wala/flowless/spec/java/LightweightJava.g:117:17: ( '\\'' | INT | 'a' .. 'z' | 'A' .. 'Z' | '\\\\' | '\\\"' )
            int alt5=6;
            switch ( input.LA(1) ) {
            case '\'':
                {
                alt5=1;
                }
                break;
            case '0':
            case '1':
            case '2':
            case '3':
            case '4':
            case '5':
            case '6':
            case '7':
            case '8':
            case '9':
                {
                alt5=2;
                }
                break;
            case 'a':
            case 'b':
            case 'c':
            case 'd':
            case 'e':
            case 'f':
            case 'g':
            case 'h':
            case 'i':
            case 'j':
            case 'k':
            case 'l':
            case 'm':
            case 'n':
            case 'o':
            case 'p':
            case 'q':
            case 'r':
            case 's':
            case 't':
            case 'u':
            case 'v':
            case 'w':
            case 'x':
            case 'y':
            case 'z':
                {
                alt5=3;
                }
                break;
            case 'A':
            case 'B':
            case 'C':
            case 'D':
            case 'E':
            case 'F':
            case 'G':
            case 'H':
            case 'I':
            case 'J':
            case 'K':
            case 'L':
            case 'M':
            case 'N':
            case 'O':
            case 'P':
            case 'Q':
            case 'R':
            case 'S':
            case 'T':
            case 'U':
            case 'V':
            case 'W':
            case 'X':
            case 'Y':
            case 'Z':
                {
                alt5=4;
                }
                break;
            case '\\':
                {
                alt5=5;
                }
                break;
            case '\"':
                {
                alt5=6;
                }
                break;
            default:
                NoViableAltException nvae =
                    new NoViableAltException("", 5, 0, input);

                throw nvae;
            }

            switch (alt5) {
                case 1 :
                    // /Users/jgf/Documents/Projects/joana/wala/joana.wala.flowless/src/edu/kit/joana/wala/flowless/spec/java/LightweightJava.g:117:18: '\\''
                    {
                    match('\''); 

                    }
                    break;
                case 2 :
                    // /Users/jgf/Documents/Projects/joana/wala/joana.wala.flowless/src/edu/kit/joana/wala/flowless/spec/java/LightweightJava.g:117:25: INT
                    {
                    mINT(); 

                    }
                    break;
                case 3 :
                    // /Users/jgf/Documents/Projects/joana/wala/joana.wala.flowless/src/edu/kit/joana/wala/flowless/spec/java/LightweightJava.g:117:31: 'a' .. 'z'
                    {
                    matchRange('a','z'); 

                    }
                    break;
                case 4 :
                    // /Users/jgf/Documents/Projects/joana/wala/joana.wala.flowless/src/edu/kit/joana/wala/flowless/spec/java/LightweightJava.g:117:42: 'A' .. 'Z'
                    {
                    matchRange('A','Z'); 

                    }
                    break;
                case 5 :
                    // /Users/jgf/Documents/Projects/joana/wala/joana.wala.flowless/src/edu/kit/joana/wala/flowless/spec/java/LightweightJava.g:117:53: '\\\\'
                    {
                    match('\\'); 

                    }
                    break;
                case 6 :
                    // /Users/jgf/Documents/Projects/joana/wala/joana.wala.flowless/src/edu/kit/joana/wala/flowless/spec/java/LightweightJava.g:117:60: '\\\"'
                    {
                    match('\"'); 

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
    // $ANTLR end "CHAR_ESC"

    // $ANTLR start "STRING"
    public final void mSTRING() throws RecognitionException {
        try {
            int _type = STRING;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // /Users/jgf/Documents/Projects/joana/wala/joana.wala.flowless/src/edu/kit/joana/wala/flowless/spec/java/LightweightJava.g:119:8: ( '\"' ( CHAR_ESC | ~ ( '\\\\' | '\"' | '\\n' ) )* '\"' )
            // /Users/jgf/Documents/Projects/joana/wala/joana.wala.flowless/src/edu/kit/joana/wala/flowless/spec/java/LightweightJava.g:119:10: '\"' ( CHAR_ESC | ~ ( '\\\\' | '\"' | '\\n' ) )* '\"'
            {
            match('\"'); 
            // /Users/jgf/Documents/Projects/joana/wala/joana.wala.flowless/src/edu/kit/joana/wala/flowless/spec/java/LightweightJava.g:119:14: ( CHAR_ESC | ~ ( '\\\\' | '\"' | '\\n' ) )*
            loop6:
            do {
                int alt6=3;
                int LA6_0 = input.LA(1);

                if ( (LA6_0=='\\') ) {
                    alt6=1;
                }
                else if ( ((LA6_0>='\u0000' && LA6_0<='\t')||(LA6_0>='\u000B' && LA6_0<='!')||(LA6_0>='#' && LA6_0<='[')||(LA6_0>=']' && LA6_0<='\uFFFF')) ) {
                    alt6=2;
                }


                switch (alt6) {
            	case 1 :
            	    // /Users/jgf/Documents/Projects/joana/wala/joana.wala.flowless/src/edu/kit/joana/wala/flowless/spec/java/LightweightJava.g:119:15: CHAR_ESC
            	    {
            	    mCHAR_ESC(); 

            	    }
            	    break;
            	case 2 :
            	    // /Users/jgf/Documents/Projects/joana/wala/joana.wala.flowless/src/edu/kit/joana/wala/flowless/spec/java/LightweightJava.g:119:26: ~ ( '\\\\' | '\"' | '\\n' )
            	    {
            	    if ( (input.LA(1)>='\u0000' && input.LA(1)<='\t')||(input.LA(1)>='\u000B' && input.LA(1)<='!')||(input.LA(1)>='#' && input.LA(1)<='[')||(input.LA(1)>=']' && input.LA(1)<='\uFFFF') ) {
            	        input.consume();

            	    }
            	    else {
            	        MismatchedSetException mse = new MismatchedSetException(null,input);
            	        recover(mse);
            	        throw mse;}


            	    }
            	    break;

            	default :
            	    break loop6;
                }
            } while (true);

            match('\"'); 

            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        }
    }
    // $ANTLR end "STRING"

    // $ANTLR start "INT"
    public final void mINT() throws RecognitionException {
        try {
            int _type = INT;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // /Users/jgf/Documents/Projects/joana/wala/joana.wala.flowless/src/edu/kit/joana/wala/flowless/spec/java/LightweightJava.g:124:8: ( ( '0' .. '9' )+ )
            // /Users/jgf/Documents/Projects/joana/wala/joana.wala.flowless/src/edu/kit/joana/wala/flowless/spec/java/LightweightJava.g:124:10: ( '0' .. '9' )+
            {
            // /Users/jgf/Documents/Projects/joana/wala/joana.wala.flowless/src/edu/kit/joana/wala/flowless/spec/java/LightweightJava.g:124:10: ( '0' .. '9' )+
            int cnt7=0;
            loop7:
            do {
                int alt7=2;
                int LA7_0 = input.LA(1);

                if ( ((LA7_0>='0' && LA7_0<='9')) ) {
                    alt7=1;
                }


                switch (alt7) {
            	case 1 :
            	    // /Users/jgf/Documents/Projects/joana/wala/joana.wala.flowless/src/edu/kit/joana/wala/flowless/spec/java/LightweightJava.g:124:11: '0' .. '9'
            	    {
            	    matchRange('0','9'); 

            	    }
            	    break;

            	default :
            	    if ( cnt7 >= 1 ) break loop7;
                        EarlyExitException eee =
                            new EarlyExitException(7, input);
                        throw eee;
                }
                cnt7++;
            } while (true);


            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        }
    }
    // $ANTLR end "INT"

    // $ANTLR start "PARAM_START"
    public final void mPARAM_START() throws RecognitionException {
        try {
            int _type = PARAM_START;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // /Users/jgf/Documents/Projects/joana/wala/joana.wala.flowless/src/edu/kit/joana/wala/flowless/spec/java/LightweightJava.g:126:13: ( '(' )
            // /Users/jgf/Documents/Projects/joana/wala/joana.wala.flowless/src/edu/kit/joana/wala/flowless/spec/java/LightweightJava.g:126:15: '('
            {
            match('('); 

            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        }
    }
    // $ANTLR end "PARAM_START"

    // $ANTLR start "PARAM_END"
    public final void mPARAM_END() throws RecognitionException {
        try {
            int _type = PARAM_END;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // /Users/jgf/Documents/Projects/joana/wala/joana.wala.flowless/src/edu/kit/joana/wala/flowless/spec/java/LightweightJava.g:128:11: ( ')' )
            // /Users/jgf/Documents/Projects/joana/wala/joana.wala.flowless/src/edu/kit/joana/wala/flowless/spec/java/LightweightJava.g:128:13: ')'
            {
            match(')'); 

            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        }
    }
    // $ANTLR end "PARAM_END"

    // $ANTLR start "ARRAY_DEF"
    public final void mARRAY_DEF() throws RecognitionException {
        try {
            int _type = ARRAY_DEF;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // /Users/jgf/Documents/Projects/joana/wala/joana.wala.flowless/src/edu/kit/joana/wala/flowless/spec/java/LightweightJava.g:130:11: ( '[]' )
            // /Users/jgf/Documents/Projects/joana/wala/joana.wala.flowless/src/edu/kit/joana/wala/flowless/spec/java/LightweightJava.g:130:13: '[]'
            {
            match("[]"); 


            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        }
    }
    // $ANTLR end "ARRAY_DEF"

    // $ANTLR start "BRACK_START"
    public final void mBRACK_START() throws RecognitionException {
        try {
            int _type = BRACK_START;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // /Users/jgf/Documents/Projects/joana/wala/joana.wala.flowless/src/edu/kit/joana/wala/flowless/spec/java/LightweightJava.g:132:13: ( '[' )
            // /Users/jgf/Documents/Projects/joana/wala/joana.wala.flowless/src/edu/kit/joana/wala/flowless/spec/java/LightweightJava.g:132:15: '['
            {
            match('['); 

            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        }
    }
    // $ANTLR end "BRACK_START"

    // $ANTLR start "BRACK_END"
    public final void mBRACK_END() throws RecognitionException {
        try {
            int _type = BRACK_END;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // /Users/jgf/Documents/Projects/joana/wala/joana.wala.flowless/src/edu/kit/joana/wala/flowless/spec/java/LightweightJava.g:134:11: ( ']' )
            // /Users/jgf/Documents/Projects/joana/wala/joana.wala.flowless/src/edu/kit/joana/wala/flowless/spec/java/LightweightJava.g:134:13: ']'
            {
            match(']'); 

            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        }
    }
    // $ANTLR end "BRACK_END"

    // $ANTLR start "LT"
    public final void mLT() throws RecognitionException {
        try {
            int _type = LT;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // /Users/jgf/Documents/Projects/joana/wala/joana.wala.flowless/src/edu/kit/joana/wala/flowless/spec/java/LightweightJava.g:136:4: ( '<' )
            // /Users/jgf/Documents/Projects/joana/wala/joana.wala.flowless/src/edu/kit/joana/wala/flowless/spec/java/LightweightJava.g:136:6: '<'
            {
            match('<'); 

            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        }
    }
    // $ANTLR end "LT"

    // $ANTLR start "GT"
    public final void mGT() throws RecognitionException {
        try {
            int _type = GT;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // /Users/jgf/Documents/Projects/joana/wala/joana.wala.flowless/src/edu/kit/joana/wala/flowless/spec/java/LightweightJava.g:138:4: ( '>' )
            // /Users/jgf/Documents/Projects/joana/wala/joana.wala.flowless/src/edu/kit/joana/wala/flowless/spec/java/LightweightJava.g:138:6: '>'
            {
            match('>'); 

            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        }
    }
    // $ANTLR end "GT"

    // $ANTLR start "COMMA"
    public final void mCOMMA() throws RecognitionException {
        try {
            int _type = COMMA;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // /Users/jgf/Documents/Projects/joana/wala/joana.wala.flowless/src/edu/kit/joana/wala/flowless/spec/java/LightweightJava.g:140:7: ( ',' )
            // /Users/jgf/Documents/Projects/joana/wala/joana.wala.flowless/src/edu/kit/joana/wala/flowless/spec/java/LightweightJava.g:140:9: ','
            {
            match(','); 

            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        }
    }
    // $ANTLR end "COMMA"

    // $ANTLR start "DOT"
    public final void mDOT() throws RecognitionException {
        try {
            int _type = DOT;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // /Users/jgf/Documents/Projects/joana/wala/joana.wala.flowless/src/edu/kit/joana/wala/flowless/spec/java/LightweightJava.g:142:5: ( '.' )
            // /Users/jgf/Documents/Projects/joana/wala/joana.wala.flowless/src/edu/kit/joana/wala/flowless/spec/java/LightweightJava.g:142:7: '.'
            {
            match('.'); 

            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        }
    }
    // $ANTLR end "DOT"

    // $ANTLR start "DP"
    public final void mDP() throws RecognitionException {
        try {
            int _type = DP;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // /Users/jgf/Documents/Projects/joana/wala/joana.wala.flowless/src/edu/kit/joana/wala/flowless/spec/java/LightweightJava.g:144:4: ( ':' )
            // /Users/jgf/Documents/Projects/joana/wala/joana.wala.flowless/src/edu/kit/joana/wala/flowless/spec/java/LightweightJava.g:144:6: ':'
            {
            match(':'); 

            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        }
    }
    // $ANTLR end "DP"

    // $ANTLR start "BR_OPEN"
    public final void mBR_OPEN() throws RecognitionException {
        try {
            int _type = BR_OPEN;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // /Users/jgf/Documents/Projects/joana/wala/joana.wala.flowless/src/edu/kit/joana/wala/flowless/spec/java/LightweightJava.g:146:9: ( '{' )
            // /Users/jgf/Documents/Projects/joana/wala/joana.wala.flowless/src/edu/kit/joana/wala/flowless/spec/java/LightweightJava.g:146:11: '{'
            {
            match('{'); 

            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        }
    }
    // $ANTLR end "BR_OPEN"

    // $ANTLR start "BR_CLOSE"
    public final void mBR_CLOSE() throws RecognitionException {
        try {
            int _type = BR_CLOSE;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // /Users/jgf/Documents/Projects/joana/wala/joana.wala.flowless/src/edu/kit/joana/wala/flowless/spec/java/LightweightJava.g:148:10: ( '}' )
            // /Users/jgf/Documents/Projects/joana/wala/joana.wala.flowless/src/edu/kit/joana/wala/flowless/spec/java/LightweightJava.g:148:12: '}'
            {
            match('}'); 

            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        }
    }
    // $ANTLR end "BR_CLOSE"

    // $ANTLR start "CLASS"
    public final void mCLASS() throws RecognitionException {
        try {
            int _type = CLASS;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // /Users/jgf/Documents/Projects/joana/wala/joana.wala.flowless/src/edu/kit/joana/wala/flowless/spec/java/LightweightJava.g:150:7: ( 'class' )
            // /Users/jgf/Documents/Projects/joana/wala/joana.wala.flowless/src/edu/kit/joana/wala/flowless/spec/java/LightweightJava.g:150:9: 'class'
            {
            match("class"); 


            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        }
    }
    // $ANTLR end "CLASS"

    // $ANTLR start "KEYWORD"
    public final void mKEYWORD() throws RecognitionException {
        try {
            int _type = KEYWORD;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // /Users/jgf/Documents/Projects/joana/wala/joana.wala.flowless/src/edu/kit/joana/wala/flowless/spec/java/LightweightJava.g:152:9: ( ( 'strictfp' | 'native' | 'public' | 'private' | 'protected' | 'final' | 'volatile' | 'synchronized' | 'abstract' ) )
            // /Users/jgf/Documents/Projects/joana/wala/joana.wala.flowless/src/edu/kit/joana/wala/flowless/spec/java/LightweightJava.g:152:11: ( 'strictfp' | 'native' | 'public' | 'private' | 'protected' | 'final' | 'volatile' | 'synchronized' | 'abstract' )
            {
            // /Users/jgf/Documents/Projects/joana/wala/joana.wala.flowless/src/edu/kit/joana/wala/flowless/spec/java/LightweightJava.g:152:11: ( 'strictfp' | 'native' | 'public' | 'private' | 'protected' | 'final' | 'volatile' | 'synchronized' | 'abstract' )
            int alt8=9;
            alt8 = dfa8.predict(input);
            switch (alt8) {
                case 1 :
                    // /Users/jgf/Documents/Projects/joana/wala/joana.wala.flowless/src/edu/kit/joana/wala/flowless/spec/java/LightweightJava.g:152:13: 'strictfp'
                    {
                    match("strictfp"); 


                    }
                    break;
                case 2 :
                    // /Users/jgf/Documents/Projects/joana/wala/joana.wala.flowless/src/edu/kit/joana/wala/flowless/spec/java/LightweightJava.g:152:26: 'native'
                    {
                    match("native"); 


                    }
                    break;
                case 3 :
                    // /Users/jgf/Documents/Projects/joana/wala/joana.wala.flowless/src/edu/kit/joana/wala/flowless/spec/java/LightweightJava.g:152:37: 'public'
                    {
                    match("public"); 


                    }
                    break;
                case 4 :
                    // /Users/jgf/Documents/Projects/joana/wala/joana.wala.flowless/src/edu/kit/joana/wala/flowless/spec/java/LightweightJava.g:152:48: 'private'
                    {
                    match("private"); 


                    }
                    break;
                case 5 :
                    // /Users/jgf/Documents/Projects/joana/wala/joana.wala.flowless/src/edu/kit/joana/wala/flowless/spec/java/LightweightJava.g:152:60: 'protected'
                    {
                    match("protected"); 


                    }
                    break;
                case 6 :
                    // /Users/jgf/Documents/Projects/joana/wala/joana.wala.flowless/src/edu/kit/joana/wala/flowless/spec/java/LightweightJava.g:152:74: 'final'
                    {
                    match("final"); 


                    }
                    break;
                case 7 :
                    // /Users/jgf/Documents/Projects/joana/wala/joana.wala.flowless/src/edu/kit/joana/wala/flowless/spec/java/LightweightJava.g:152:84: 'volatile'
                    {
                    match("volatile"); 


                    }
                    break;
                case 8 :
                    // /Users/jgf/Documents/Projects/joana/wala/joana.wala.flowless/src/edu/kit/joana/wala/flowless/spec/java/LightweightJava.g:152:97: 'synchronized'
                    {
                    match("synchronized"); 


                    }
                    break;
                case 9 :
                    // /Users/jgf/Documents/Projects/joana/wala/joana.wala.flowless/src/edu/kit/joana/wala/flowless/spec/java/LightweightJava.g:152:114: 'abstract'
                    {
                    match("abstract"); 


                    }
                    break;

            }

             _channel = HIDDEN; 

            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        }
    }
    // $ANTLR end "KEYWORD"

    // $ANTLR start "STATIC"
    public final void mSTATIC() throws RecognitionException {
        try {
            int _type = STATIC;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // /Users/jgf/Documents/Projects/joana/wala/joana.wala.flowless/src/edu/kit/joana/wala/flowless/spec/java/LightweightJava.g:154:8: ( 'static' )
            // /Users/jgf/Documents/Projects/joana/wala/joana.wala.flowless/src/edu/kit/joana/wala/flowless/spec/java/LightweightJava.g:154:10: 'static'
            {
            match("static"); 


            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        }
    }
    // $ANTLR end "STATIC"

    // $ANTLR start "INTERFACE"
    public final void mINTERFACE() throws RecognitionException {
        try {
            int _type = INTERFACE;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // /Users/jgf/Documents/Projects/joana/wala/joana.wala.flowless/src/edu/kit/joana/wala/flowless/spec/java/LightweightJava.g:156:11: ( 'interface' )
            // /Users/jgf/Documents/Projects/joana/wala/joana.wala.flowless/src/edu/kit/joana/wala/flowless/spec/java/LightweightJava.g:156:13: 'interface'
            {
            match("interface"); 


            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        }
    }
    // $ANTLR end "INTERFACE"

    // $ANTLR start "PACKAGE"
    public final void mPACKAGE() throws RecognitionException {
        try {
            int _type = PACKAGE;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // /Users/jgf/Documents/Projects/joana/wala/joana.wala.flowless/src/edu/kit/joana/wala/flowless/spec/java/LightweightJava.g:158:9: ( 'package' )
            // /Users/jgf/Documents/Projects/joana/wala/joana.wala.flowless/src/edu/kit/joana/wala/flowless/spec/java/LightweightJava.g:158:11: 'package'
            {
            match("package"); 


            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        }
    }
    // $ANTLR end "PACKAGE"

    // $ANTLR start "ANNOTATION"
    public final void mANNOTATION() throws RecognitionException {
        try {
            int _type = ANNOTATION;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // /Users/jgf/Documents/Projects/joana/wala/joana.wala.flowless/src/edu/kit/joana/wala/flowless/spec/java/LightweightJava.g:160:12: ( ( '@' ( 'a' .. 'z' | 'A' .. 'Z' | '_' ) ( 'a' .. 'z' | 'A' .. 'Z' | '_' | '0' .. '9' )* ) )
            // /Users/jgf/Documents/Projects/joana/wala/joana.wala.flowless/src/edu/kit/joana/wala/flowless/spec/java/LightweightJava.g:160:14: ( '@' ( 'a' .. 'z' | 'A' .. 'Z' | '_' ) ( 'a' .. 'z' | 'A' .. 'Z' | '_' | '0' .. '9' )* )
            {
            // /Users/jgf/Documents/Projects/joana/wala/joana.wala.flowless/src/edu/kit/joana/wala/flowless/spec/java/LightweightJava.g:160:14: ( '@' ( 'a' .. 'z' | 'A' .. 'Z' | '_' ) ( 'a' .. 'z' | 'A' .. 'Z' | '_' | '0' .. '9' )* )
            // /Users/jgf/Documents/Projects/joana/wala/joana.wala.flowless/src/edu/kit/joana/wala/flowless/spec/java/LightweightJava.g:160:16: '@' ( 'a' .. 'z' | 'A' .. 'Z' | '_' ) ( 'a' .. 'z' | 'A' .. 'Z' | '_' | '0' .. '9' )*
            {
            match('@'); 
            if ( (input.LA(1)>='A' && input.LA(1)<='Z')||input.LA(1)=='_'||(input.LA(1)>='a' && input.LA(1)<='z') ) {
                input.consume();

            }
            else {
                MismatchedSetException mse = new MismatchedSetException(null,input);
                recover(mse);
                throw mse;}

            // /Users/jgf/Documents/Projects/joana/wala/joana.wala.flowless/src/edu/kit/joana/wala/flowless/spec/java/LightweightJava.g:160:44: ( 'a' .. 'z' | 'A' .. 'Z' | '_' | '0' .. '9' )*
            loop9:
            do {
                int alt9=2;
                int LA9_0 = input.LA(1);

                if ( ((LA9_0>='0' && LA9_0<='9')||(LA9_0>='A' && LA9_0<='Z')||LA9_0=='_'||(LA9_0>='a' && LA9_0<='z')) ) {
                    alt9=1;
                }


                switch (alt9) {
            	case 1 :
            	    // /Users/jgf/Documents/Projects/joana/wala/joana.wala.flowless/src/edu/kit/joana/wala/flowless/spec/java/LightweightJava.g:
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
            	    break loop9;
                }
            } while (true);


            }


            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        }
    }
    // $ANTLR end "ANNOTATION"

    // $ANTLR start "IDENT"
    public final void mIDENT() throws RecognitionException {
        try {
            int _type = IDENT;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // /Users/jgf/Documents/Projects/joana/wala/joana.wala.flowless/src/edu/kit/joana/wala/flowless/spec/java/LightweightJava.g:163:3: ( ( 'a' .. 'z' | 'A' .. 'Z' | '_' ) ( 'a' .. 'z' | 'A' .. 'Z' | '_' | '0' .. '9' )* )
            // /Users/jgf/Documents/Projects/joana/wala/joana.wala.flowless/src/edu/kit/joana/wala/flowless/spec/java/LightweightJava.g:163:5: ( 'a' .. 'z' | 'A' .. 'Z' | '_' ) ( 'a' .. 'z' | 'A' .. 'Z' | '_' | '0' .. '9' )*
            {
            if ( (input.LA(1)>='A' && input.LA(1)<='Z')||input.LA(1)=='_'||(input.LA(1)>='a' && input.LA(1)<='z') ) {
                input.consume();

            }
            else {
                MismatchedSetException mse = new MismatchedSetException(null,input);
                recover(mse);
                throw mse;}

            // /Users/jgf/Documents/Projects/joana/wala/joana.wala.flowless/src/edu/kit/joana/wala/flowless/spec/java/LightweightJava.g:163:29: ( 'a' .. 'z' | 'A' .. 'Z' | '_' | '0' .. '9' )*
            loop10:
            do {
                int alt10=2;
                int LA10_0 = input.LA(1);

                if ( ((LA10_0>='0' && LA10_0<='9')||(LA10_0>='A' && LA10_0<='Z')||LA10_0=='_'||(LA10_0>='a' && LA10_0<='z')) ) {
                    alt10=1;
                }


                switch (alt10) {
            	case 1 :
            	    // /Users/jgf/Documents/Projects/joana/wala/joana.wala.flowless/src/edu/kit/joana/wala/flowless/spec/java/LightweightJava.g:
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
            	    break loop10;
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

    // $ANTLR start "ASSIGN"
    public final void mASSIGN() throws RecognitionException {
        try {
            int _type = ASSIGN;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // /Users/jgf/Documents/Projects/joana/wala/joana.wala.flowless/src/edu/kit/joana/wala/flowless/spec/java/LightweightJava.g:166:8: ( '=' )
            // /Users/jgf/Documents/Projects/joana/wala/joana.wala.flowless/src/edu/kit/joana/wala/flowless/spec/java/LightweightJava.g:166:10: '='
            {
            match('='); 

            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        }
    }
    // $ANTLR end "ASSIGN"

    // $ANTLR start "EXP_OP"
    public final void mEXP_OP() throws RecognitionException {
        try {
            int _type = EXP_OP;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // /Users/jgf/Documents/Projects/joana/wala/joana.wala.flowless/src/edu/kit/joana/wala/flowless/spec/java/LightweightJava.g:168:8: ( '!' | '+=' | '~' | '-=' | '*' | '+' | '-' | '/' | '&' | '^' | '%' | '<=' | '>=' | '==' | '|' | '||' | '&&' | '?' )
            int alt11=18;
            alt11 = dfa11.predict(input);
            switch (alt11) {
                case 1 :
                    // /Users/jgf/Documents/Projects/joana/wala/joana.wala.flowless/src/edu/kit/joana/wala/flowless/spec/java/LightweightJava.g:168:10: '!'
                    {
                    match('!'); 

                    }
                    break;
                case 2 :
                    // /Users/jgf/Documents/Projects/joana/wala/joana.wala.flowless/src/edu/kit/joana/wala/flowless/spec/java/LightweightJava.g:168:16: '+='
                    {
                    match("+="); 


                    }
                    break;
                case 3 :
                    // /Users/jgf/Documents/Projects/joana/wala/joana.wala.flowless/src/edu/kit/joana/wala/flowless/spec/java/LightweightJava.g:168:23: '~'
                    {
                    match('~'); 

                    }
                    break;
                case 4 :
                    // /Users/jgf/Documents/Projects/joana/wala/joana.wala.flowless/src/edu/kit/joana/wala/flowless/spec/java/LightweightJava.g:168:29: '-='
                    {
                    match("-="); 


                    }
                    break;
                case 5 :
                    // /Users/jgf/Documents/Projects/joana/wala/joana.wala.flowless/src/edu/kit/joana/wala/flowless/spec/java/LightweightJava.g:168:36: '*'
                    {
                    match('*'); 

                    }
                    break;
                case 6 :
                    // /Users/jgf/Documents/Projects/joana/wala/joana.wala.flowless/src/edu/kit/joana/wala/flowless/spec/java/LightweightJava.g:168:42: '+'
                    {
                    match('+'); 

                    }
                    break;
                case 7 :
                    // /Users/jgf/Documents/Projects/joana/wala/joana.wala.flowless/src/edu/kit/joana/wala/flowless/spec/java/LightweightJava.g:168:48: '-'
                    {
                    match('-'); 

                    }
                    break;
                case 8 :
                    // /Users/jgf/Documents/Projects/joana/wala/joana.wala.flowless/src/edu/kit/joana/wala/flowless/spec/java/LightweightJava.g:168:54: '/'
                    {
                    match('/'); 

                    }
                    break;
                case 9 :
                    // /Users/jgf/Documents/Projects/joana/wala/joana.wala.flowless/src/edu/kit/joana/wala/flowless/spec/java/LightweightJava.g:168:60: '&'
                    {
                    match('&'); 

                    }
                    break;
                case 10 :
                    // /Users/jgf/Documents/Projects/joana/wala/joana.wala.flowless/src/edu/kit/joana/wala/flowless/spec/java/LightweightJava.g:168:66: '^'
                    {
                    match('^'); 

                    }
                    break;
                case 11 :
                    // /Users/jgf/Documents/Projects/joana/wala/joana.wala.flowless/src/edu/kit/joana/wala/flowless/spec/java/LightweightJava.g:168:72: '%'
                    {
                    match('%'); 

                    }
                    break;
                case 12 :
                    // /Users/jgf/Documents/Projects/joana/wala/joana.wala.flowless/src/edu/kit/joana/wala/flowless/spec/java/LightweightJava.g:168:78: '<='
                    {
                    match("<="); 


                    }
                    break;
                case 13 :
                    // /Users/jgf/Documents/Projects/joana/wala/joana.wala.flowless/src/edu/kit/joana/wala/flowless/spec/java/LightweightJava.g:168:85: '>='
                    {
                    match(">="); 


                    }
                    break;
                case 14 :
                    // /Users/jgf/Documents/Projects/joana/wala/joana.wala.flowless/src/edu/kit/joana/wala/flowless/spec/java/LightweightJava.g:168:92: '=='
                    {
                    match("=="); 


                    }
                    break;
                case 15 :
                    // /Users/jgf/Documents/Projects/joana/wala/joana.wala.flowless/src/edu/kit/joana/wala/flowless/spec/java/LightweightJava.g:168:99: '|'
                    {
                    match('|'); 

                    }
                    break;
                case 16 :
                    // /Users/jgf/Documents/Projects/joana/wala/joana.wala.flowless/src/edu/kit/joana/wala/flowless/spec/java/LightweightJava.g:168:105: '||'
                    {
                    match("||"); 


                    }
                    break;
                case 17 :
                    // /Users/jgf/Documents/Projects/joana/wala/joana.wala.flowless/src/edu/kit/joana/wala/flowless/spec/java/LightweightJava.g:168:112: '&&'
                    {
                    match("&&"); 


                    }
                    break;
                case 18 :
                    // /Users/jgf/Documents/Projects/joana/wala/joana.wala.flowless/src/edu/kit/joana/wala/flowless/spec/java/LightweightJava.g:168:119: '?'
                    {
                    match('?'); 

                    }
                    break;

            }
            state.type = _type;
            state.channel = _channel;
        }
        finally {
        }
    }
    // $ANTLR end "EXP_OP"

    public void mTokens() throws RecognitionException {
        // /Users/jgf/Documents/Projects/joana/wala/joana.wala.flowless/src/edu/kit/joana/wala/flowless/spec/java/LightweightJava.g:1:8: ( SC | WS_ | LINE_COMMENT | BLOCK_COMMENT | BLC_START | BLC_END | CHAR | CHAR_ESC | STRING | INT | PARAM_START | PARAM_END | ARRAY_DEF | BRACK_START | BRACK_END | LT | GT | COMMA | DOT | DP | BR_OPEN | BR_CLOSE | CLASS | KEYWORD | STATIC | INTERFACE | PACKAGE | ANNOTATION | IDENT | ASSIGN | EXP_OP )
        int alt12=31;
        alt12 = dfa12.predict(input);
        switch (alt12) {
            case 1 :
                // /Users/jgf/Documents/Projects/joana/wala/joana.wala.flowless/src/edu/kit/joana/wala/flowless/spec/java/LightweightJava.g:1:10: SC
                {
                mSC(); 

                }
                break;
            case 2 :
                // /Users/jgf/Documents/Projects/joana/wala/joana.wala.flowless/src/edu/kit/joana/wala/flowless/spec/java/LightweightJava.g:1:13: WS_
                {
                mWS_(); 

                }
                break;
            case 3 :
                // /Users/jgf/Documents/Projects/joana/wala/joana.wala.flowless/src/edu/kit/joana/wala/flowless/spec/java/LightweightJava.g:1:17: LINE_COMMENT
                {
                mLINE_COMMENT(); 

                }
                break;
            case 4 :
                // /Users/jgf/Documents/Projects/joana/wala/joana.wala.flowless/src/edu/kit/joana/wala/flowless/spec/java/LightweightJava.g:1:30: BLOCK_COMMENT
                {
                mBLOCK_COMMENT(); 

                }
                break;
            case 5 :
                // /Users/jgf/Documents/Projects/joana/wala/joana.wala.flowless/src/edu/kit/joana/wala/flowless/spec/java/LightweightJava.g:1:44: BLC_START
                {
                mBLC_START(); 

                }
                break;
            case 6 :
                // /Users/jgf/Documents/Projects/joana/wala/joana.wala.flowless/src/edu/kit/joana/wala/flowless/spec/java/LightweightJava.g:1:54: BLC_END
                {
                mBLC_END(); 

                }
                break;
            case 7 :
                // /Users/jgf/Documents/Projects/joana/wala/joana.wala.flowless/src/edu/kit/joana/wala/flowless/spec/java/LightweightJava.g:1:62: CHAR
                {
                mCHAR(); 

                }
                break;
            case 8 :
                // /Users/jgf/Documents/Projects/joana/wala/joana.wala.flowless/src/edu/kit/joana/wala/flowless/spec/java/LightweightJava.g:1:67: CHAR_ESC
                {
                mCHAR_ESC(); 

                }
                break;
            case 9 :
                // /Users/jgf/Documents/Projects/joana/wala/joana.wala.flowless/src/edu/kit/joana/wala/flowless/spec/java/LightweightJava.g:1:76: STRING
                {
                mSTRING(); 

                }
                break;
            case 10 :
                // /Users/jgf/Documents/Projects/joana/wala/joana.wala.flowless/src/edu/kit/joana/wala/flowless/spec/java/LightweightJava.g:1:83: INT
                {
                mINT(); 

                }
                break;
            case 11 :
                // /Users/jgf/Documents/Projects/joana/wala/joana.wala.flowless/src/edu/kit/joana/wala/flowless/spec/java/LightweightJava.g:1:87: PARAM_START
                {
                mPARAM_START(); 

                }
                break;
            case 12 :
                // /Users/jgf/Documents/Projects/joana/wala/joana.wala.flowless/src/edu/kit/joana/wala/flowless/spec/java/LightweightJava.g:1:99: PARAM_END
                {
                mPARAM_END(); 

                }
                break;
            case 13 :
                // /Users/jgf/Documents/Projects/joana/wala/joana.wala.flowless/src/edu/kit/joana/wala/flowless/spec/java/LightweightJava.g:1:109: ARRAY_DEF
                {
                mARRAY_DEF(); 

                }
                break;
            case 14 :
                // /Users/jgf/Documents/Projects/joana/wala/joana.wala.flowless/src/edu/kit/joana/wala/flowless/spec/java/LightweightJava.g:1:119: BRACK_START
                {
                mBRACK_START(); 

                }
                break;
            case 15 :
                // /Users/jgf/Documents/Projects/joana/wala/joana.wala.flowless/src/edu/kit/joana/wala/flowless/spec/java/LightweightJava.g:1:131: BRACK_END
                {
                mBRACK_END(); 

                }
                break;
            case 16 :
                // /Users/jgf/Documents/Projects/joana/wala/joana.wala.flowless/src/edu/kit/joana/wala/flowless/spec/java/LightweightJava.g:1:141: LT
                {
                mLT(); 

                }
                break;
            case 17 :
                // /Users/jgf/Documents/Projects/joana/wala/joana.wala.flowless/src/edu/kit/joana/wala/flowless/spec/java/LightweightJava.g:1:144: GT
                {
                mGT(); 

                }
                break;
            case 18 :
                // /Users/jgf/Documents/Projects/joana/wala/joana.wala.flowless/src/edu/kit/joana/wala/flowless/spec/java/LightweightJava.g:1:147: COMMA
                {
                mCOMMA(); 

                }
                break;
            case 19 :
                // /Users/jgf/Documents/Projects/joana/wala/joana.wala.flowless/src/edu/kit/joana/wala/flowless/spec/java/LightweightJava.g:1:153: DOT
                {
                mDOT(); 

                }
                break;
            case 20 :
                // /Users/jgf/Documents/Projects/joana/wala/joana.wala.flowless/src/edu/kit/joana/wala/flowless/spec/java/LightweightJava.g:1:157: DP
                {
                mDP(); 

                }
                break;
            case 21 :
                // /Users/jgf/Documents/Projects/joana/wala/joana.wala.flowless/src/edu/kit/joana/wala/flowless/spec/java/LightweightJava.g:1:160: BR_OPEN
                {
                mBR_OPEN(); 

                }
                break;
            case 22 :
                // /Users/jgf/Documents/Projects/joana/wala/joana.wala.flowless/src/edu/kit/joana/wala/flowless/spec/java/LightweightJava.g:1:168: BR_CLOSE
                {
                mBR_CLOSE(); 

                }
                break;
            case 23 :
                // /Users/jgf/Documents/Projects/joana/wala/joana.wala.flowless/src/edu/kit/joana/wala/flowless/spec/java/LightweightJava.g:1:177: CLASS
                {
                mCLASS(); 

                }
                break;
            case 24 :
                // /Users/jgf/Documents/Projects/joana/wala/joana.wala.flowless/src/edu/kit/joana/wala/flowless/spec/java/LightweightJava.g:1:183: KEYWORD
                {
                mKEYWORD(); 

                }
                break;
            case 25 :
                // /Users/jgf/Documents/Projects/joana/wala/joana.wala.flowless/src/edu/kit/joana/wala/flowless/spec/java/LightweightJava.g:1:191: STATIC
                {
                mSTATIC(); 

                }
                break;
            case 26 :
                // /Users/jgf/Documents/Projects/joana/wala/joana.wala.flowless/src/edu/kit/joana/wala/flowless/spec/java/LightweightJava.g:1:198: INTERFACE
                {
                mINTERFACE(); 

                }
                break;
            case 27 :
                // /Users/jgf/Documents/Projects/joana/wala/joana.wala.flowless/src/edu/kit/joana/wala/flowless/spec/java/LightweightJava.g:1:208: PACKAGE
                {
                mPACKAGE(); 

                }
                break;
            case 28 :
                // /Users/jgf/Documents/Projects/joana/wala/joana.wala.flowless/src/edu/kit/joana/wala/flowless/spec/java/LightweightJava.g:1:216: ANNOTATION
                {
                mANNOTATION(); 

                }
                break;
            case 29 :
                // /Users/jgf/Documents/Projects/joana/wala/joana.wala.flowless/src/edu/kit/joana/wala/flowless/spec/java/LightweightJava.g:1:227: IDENT
                {
                mIDENT(); 

                }
                break;
            case 30 :
                // /Users/jgf/Documents/Projects/joana/wala/joana.wala.flowless/src/edu/kit/joana/wala/flowless/spec/java/LightweightJava.g:1:233: ASSIGN
                {
                mASSIGN(); 

                }
                break;
            case 31 :
                // /Users/jgf/Documents/Projects/joana/wala/joana.wala.flowless/src/edu/kit/joana/wala/flowless/spec/java/LightweightJava.g:1:240: EXP_OP
                {
                mEXP_OP(); 

                }
                break;

        }

    }


    protected DFA8 dfa8 = new DFA8(this);
    protected DFA11 dfa11 = new DFA11(this);
    protected DFA12 dfa12 = new DFA12(this);
    static final String DFA8_eotS =
        "\15\uffff";
    static final String DFA8_eofS =
        "\15\uffff";
    static final String DFA8_minS =
        "\1\141\1\164\1\uffff\1\162\6\uffff\1\151\2\uffff";
    static final String DFA8_maxS =
        "\1\166\1\171\1\uffff\1\165\6\uffff\1\157\2\uffff";
    static final String DFA8_acceptS =
        "\2\uffff\1\2\1\uffff\1\6\1\7\1\11\1\1\1\10\1\3\1\uffff\1\4\1\5";
    static final String DFA8_specialS =
        "\15\uffff}>";
    static final String[] DFA8_transitionS = {
            "\1\6\4\uffff\1\4\7\uffff\1\2\1\uffff\1\3\2\uffff\1\1\2\uffff"+
            "\1\5",
            "\1\7\4\uffff\1\10",
            "",
            "\1\12\2\uffff\1\11",
            "",
            "",
            "",
            "",
            "",
            "",
            "\1\13\5\uffff\1\14",
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
            return "152:11: ( 'strictfp' | 'native' | 'public' | 'private' | 'protected' | 'final' | 'volatile' | 'synchronized' | 'abstract' )";
        }
    }
    static final String DFA11_eotS =
        "\2\uffff\1\20\1\uffff\1\22\2\uffff\1\24\5\uffff\1\26\11\uffff";
    static final String DFA11_eofS =
        "\27\uffff";
    static final String DFA11_minS =
        "\1\41\1\uffff\1\75\1\uffff\1\75\2\uffff\1\46\5\uffff\1\174\11\uffff";
    static final String DFA11_maxS =
        "\1\176\1\uffff\1\75\1\uffff\1\75\2\uffff\1\46\5\uffff\1\174\11\uffff";
    static final String DFA11_acceptS =
        "\1\uffff\1\1\1\uffff\1\3\1\uffff\1\5\1\10\1\uffff\1\12\1\13\1\14"+
        "\1\15\1\16\1\uffff\1\22\1\2\1\6\1\4\1\7\1\21\1\11\1\20\1\17";
    static final String DFA11_specialS =
        "\27\uffff}>";
    static final String[] DFA11_transitionS = {
            "\1\1\3\uffff\1\11\1\7\3\uffff\1\5\1\2\1\uffff\1\4\1\uffff\1"+
            "\6\14\uffff\1\12\1\14\1\13\1\16\36\uffff\1\10\35\uffff\1\15"+
            "\1\uffff\1\3",
            "",
            "\1\17",
            "",
            "\1\21",
            "",
            "",
            "\1\23",
            "",
            "",
            "",
            "",
            "",
            "\1\25",
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
            return "168:1: EXP_OP : ( '!' | '+=' | '~' | '-=' | '*' | '+' | '-' | '/' | '&' | '^' | '%' | '<=' | '>=' | '==' | '|' | '||' | '&&' | '?' );";
        }
    }
    static final String DFA12_eotS =
        "\3\uffff\2\37\6\uffff\1\44\1\uffff\1\45\1\46\5\uffff\10\35\2\uffff"+
        "\1\62\2\uffff\1\63\5\uffff\13\35\3\uffff\32\35\1\134\10\35\1\145"+
        "\3\35\1\uffff\1\35\1\152\1\35\2\145\3\35\1\uffff\4\35\1\uffff\1"+
        "\35\1\145\1\35\1\165\3\35\1\145\2\35\1\uffff\2\145\2\35\1\145\1"+
        "\175\1\35\1\uffff\1\35\1\145";
    static final String DFA12_eofS =
        "\u0080\uffff";
    static final String DFA12_minS =
        "\1\11\2\uffff\1\52\1\57\6\uffff\1\135\1\uffff\2\75\5\uffff\1\154"+
        "\1\164\2\141\1\151\1\157\1\142\1\156\2\uffff\1\75\2\uffff\1\0\5"+
        "\uffff\2\141\1\156\1\164\1\142\1\151\1\143\1\156\1\154\1\163\1\164"+
        "\3\uffff\1\163\1\151\1\164\1\143\1\151\1\154\1\166\1\164\1\153\2"+
        "\141\1\164\1\145\1\163\1\143\1\151\1\150\1\166\1\151\1\141\1\145"+
        "\1\141\1\154\1\164\2\162\1\60\1\164\1\143\1\162\1\145\1\143\1\164"+
        "\1\143\1\147\1\60\1\151\1\141\1\146\1\uffff\1\146\1\60\1\157\2\60"+
        "\1\145\1\164\1\145\1\uffff\1\154\1\143\1\141\1\160\1\uffff\1\156"+
        "\1\60\1\145\1\60\1\145\1\164\1\143\1\60\1\151\1\144\1\uffff\2\60"+
        "\1\145\1\172\2\60\1\145\1\uffff\1\144\1\60";
    static final String DFA12_maxS =
        "\1\176\2\uffff\2\57\6\uffff\1\135\1\uffff\2\75\5\uffff\1\154\1\171"+
        "\1\141\1\165\1\151\1\157\1\142\1\156\2\uffff\1\75\2\uffff\1\uffff"+
        "\5\uffff\1\141\1\162\1\156\1\164\1\142\1\157\1\143\1\156\1\154\1"+
        "\163\1\164\3\uffff\1\163\1\151\1\164\1\143\1\151\1\154\1\166\1\164"+
        "\1\153\2\141\1\164\1\145\1\163\1\143\1\151\1\150\1\166\1\151\1\141"+
        "\1\145\1\141\1\154\1\164\2\162\1\172\1\164\1\143\1\162\1\145\1\143"+
        "\1\164\1\143\1\147\1\172\1\151\1\141\1\146\1\uffff\1\146\1\172\1"+
        "\157\2\172\1\145\1\164\1\145\1\uffff\1\154\1\143\1\141\1\160\1\uffff"+
        "\1\156\1\172\1\145\1\172\1\145\1\164\1\143\1\172\1\151\1\144\1\uffff"+
        "\2\172\1\145\3\172\1\145\1\uffff\1\144\1\172";
    static final String DFA12_acceptS =
        "\1\uffff\1\1\1\2\2\uffff\1\7\1\10\1\11\1\12\1\13\1\14\1\uffff\1"+
        "\17\2\uffff\1\22\1\23\1\24\1\25\1\26\10\uffff\1\34\1\35\1\uffff"+
        "\1\37\1\3\1\uffff\1\6\1\15\1\16\1\20\1\21\13\uffff\1\36\1\5\1\4"+
        "\47\uffff\1\27\10\uffff\1\30\4\uffff\1\31\12\uffff\1\33\7\uffff"+
        "\1\32\2\uffff";
    static final String DFA12_specialS =
        "\41\uffff\1\0\136\uffff}>";
    static final String[] DFA12_transitionS = {
            "\2\2\1\uffff\2\2\22\uffff\1\2\1\37\1\7\2\uffff\2\37\1\5\1\11"+
            "\1\12\1\4\1\37\1\17\1\37\1\20\1\3\12\10\1\21\1\1\1\15\1\36\1"+
            "\16\1\37\1\34\32\35\1\13\1\6\1\14\1\37\1\35\1\uffff\1\32\1\35"+
            "\1\24\2\35\1\30\2\35\1\33\4\35\1\26\1\35\1\27\2\35\1\25\2\35"+
            "\1\31\4\35\1\22\1\37\1\23\1\37",
            "",
            "",
            "\1\41\4\uffff\1\40",
            "\1\42",
            "",
            "",
            "",
            "",
            "",
            "",
            "\1\43",
            "",
            "\1\37",
            "\1\37",
            "",
            "",
            "",
            "",
            "",
            "\1\47",
            "\1\50\4\uffff\1\51",
            "\1\52",
            "\1\55\20\uffff\1\54\2\uffff\1\53",
            "\1\56",
            "\1\57",
            "\1\60",
            "\1\61",
            "",
            "",
            "\1\37",
            "",
            "",
            "\0\64",
            "",
            "",
            "",
            "",
            "",
            "\1\65",
            "\1\67\20\uffff\1\66",
            "\1\70",
            "\1\71",
            "\1\72",
            "\1\73\5\uffff\1\74",
            "\1\75",
            "\1\76",
            "\1\77",
            "\1\100",
            "\1\101",
            "",
            "",
            "",
            "\1\102",
            "\1\103",
            "\1\104",
            "\1\105",
            "\1\106",
            "\1\107",
            "\1\110",
            "\1\111",
            "\1\112",
            "\1\113",
            "\1\114",
            "\1\115",
            "\1\116",
            "\1\117",
            "\1\120",
            "\1\121",
            "\1\122",
            "\1\123",
            "\1\124",
            "\1\125",
            "\1\126",
            "\1\127",
            "\1\130",
            "\1\131",
            "\1\132",
            "\1\133",
            "\12\35\7\uffff\32\35\4\uffff\1\35\1\uffff\32\35",
            "\1\135",
            "\1\136",
            "\1\137",
            "\1\140",
            "\1\141",
            "\1\142",
            "\1\143",
            "\1\144",
            "\12\35\7\uffff\32\35\4\uffff\1\35\1\uffff\32\35",
            "\1\146",
            "\1\147",
            "\1\150",
            "",
            "\1\151",
            "\12\35\7\uffff\32\35\4\uffff\1\35\1\uffff\32\35",
            "\1\153",
            "\12\35\7\uffff\32\35\4\uffff\1\35\1\uffff\32\35",
            "\12\35\7\uffff\32\35\4\uffff\1\35\1\uffff\32\35",
            "\1\154",
            "\1\155",
            "\1\156",
            "",
            "\1\157",
            "\1\160",
            "\1\161",
            "\1\162",
            "",
            "\1\163",
            "\12\35\7\uffff\32\35\4\uffff\1\35\1\uffff\32\35",
            "\1\164",
            "\12\35\7\uffff\32\35\4\uffff\1\35\1\uffff\32\35",
            "\1\166",
            "\1\167",
            "\1\170",
            "\12\35\7\uffff\32\35\4\uffff\1\35\1\uffff\32\35",
            "\1\171",
            "\1\172",
            "",
            "\12\35\7\uffff\32\35\4\uffff\1\35\1\uffff\32\35",
            "\12\35\7\uffff\32\35\4\uffff\1\35\1\uffff\32\35",
            "\1\173",
            "\1\174",
            "\12\35\7\uffff\32\35\4\uffff\1\35\1\uffff\32\35",
            "\12\35\7\uffff\32\35\4\uffff\1\35\1\uffff\32\35",
            "\1\176",
            "",
            "\1\177",
            "\12\35\7\uffff\32\35\4\uffff\1\35\1\uffff\32\35"
    };

    static final short[] DFA12_eot = DFA.unpackEncodedString(DFA12_eotS);
    static final short[] DFA12_eof = DFA.unpackEncodedString(DFA12_eofS);
    static final char[] DFA12_min = DFA.unpackEncodedStringToUnsignedChars(DFA12_minS);
    static final char[] DFA12_max = DFA.unpackEncodedStringToUnsignedChars(DFA12_maxS);
    static final short[] DFA12_accept = DFA.unpackEncodedString(DFA12_acceptS);
    static final short[] DFA12_special = DFA.unpackEncodedString(DFA12_specialS);
    static final short[][] DFA12_transition;

    static {
        int numStates = DFA12_transitionS.length;
        DFA12_transition = new short[numStates][];
        for (int i=0; i<numStates; i++) {
            DFA12_transition[i] = DFA.unpackEncodedString(DFA12_transitionS[i]);
        }
    }

    class DFA12 extends DFA {

        public DFA12(BaseRecognizer recognizer) {
            this.recognizer = recognizer;
            this.decisionNumber = 12;
            this.eot = DFA12_eot;
            this.eof = DFA12_eof;
            this.min = DFA12_min;
            this.max = DFA12_max;
            this.accept = DFA12_accept;
            this.special = DFA12_special;
            this.transition = DFA12_transition;
        }
        public String getDescription() {
            return "1:1: Tokens : ( SC | WS_ | LINE_COMMENT | BLOCK_COMMENT | BLC_START | BLC_END | CHAR | CHAR_ESC | STRING | INT | PARAM_START | PARAM_END | ARRAY_DEF | BRACK_START | BRACK_END | LT | GT | COMMA | DOT | DP | BR_OPEN | BR_CLOSE | CLASS | KEYWORD | STATIC | INTERFACE | PACKAGE | ANNOTATION | IDENT | ASSIGN | EXP_OP );";
        }
        public int specialStateTransition(int s, IntStream _input) throws NoViableAltException {
            IntStream input = _input;
        	int _s = s;
            switch ( s ) {
                    case 0 : 
                        int LA12_33 = input.LA(1);

                        s = -1;
                        if ( ((LA12_33>='\u0000' && LA12_33<='\uFFFF')) ) {s = 52;}

                        else s = 51;

                        if ( s>=0 ) return s;
                        break;
            }
            NoViableAltException nvae =
                new NoViableAltException(getDescription(), 12, _s, input);
            error(nvae);
            throw nvae;
        }
    }
 

}