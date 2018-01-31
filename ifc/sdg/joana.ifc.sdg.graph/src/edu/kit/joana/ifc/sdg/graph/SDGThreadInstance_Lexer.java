// $ANTLR 3.5.2 /data1/hecker/gits/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDGThreadInstance_.g 2018-01-31 14:57:17
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

@SuppressWarnings("all")
public class SDGThreadInstance_Lexer extends Lexer {
	public static final int EOF=-1;
	public static final int T__7=7;
	public static final int T__8=8;
	public static final int T__9=9;
	public static final int T__10=10;
	public static final int T__11=11;
	public static final int T__12=12;
	public static final int T__13=13;
	public static final int T__14=14;
	public static final int T__15=15;
	public static final int T__16=16;
	public static final int T__17=17;
	public static final int T__18=18;
	public static final int T__19=19;
	public static final int T__20=20;
	public static final int T__21=21;
	public static final int T__22=22;
	public static final int T__23=23;
	public static final int NUMBER=4;
	public static final int STRING=5;
	public static final int WHITESPACE=6;

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
	public Lexer[] getDelegates() {
		return new Lexer[] {};
	}

	public SDGThreadInstance_Lexer() {} 
	public SDGThreadInstance_Lexer(CharStream input) {
		this(input, new RecognizerSharedState());
	}
	public SDGThreadInstance_Lexer(CharStream input, RecognizerSharedState state) {
		super(input,state);
	}
	@Override public String getGrammarFileName() { return "/data1/hecker/gits/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDGThreadInstance_.g"; }

	// $ANTLR start "T__7"
	public final void mT__7() throws RecognitionException {
		try {
			int _type = T__7;
			int _channel = DEFAULT_TOKEN_CHANNEL;
			// /data1/hecker/gits/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDGThreadInstance_.g:47:6: ( ',' )
			// /data1/hecker/gits/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDGThreadInstance_.g:47:8: ','
			{
			match(','); 
			}

			state.type = _type;
			state.channel = _channel;
		}
		finally {
			// do for sure before leaving
		}
	}
	// $ANTLR end "T__7"

	// $ANTLR start "T__8"
	public final void mT__8() throws RecognitionException {
		try {
			int _type = T__8;
			int _channel = DEFAULT_TOKEN_CHANNEL;
			// /data1/hecker/gits/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDGThreadInstance_.g:48:6: ( '-' )
			// /data1/hecker/gits/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDGThreadInstance_.g:48:8: '-'
			{
			match('-'); 
			}

			state.type = _type;
			state.channel = _channel;
		}
		finally {
			// do for sure before leaving
		}
	}
	// $ANTLR end "T__8"

	// $ANTLR start "T__9"
	public final void mT__9() throws RecognitionException {
		try {
			int _type = T__9;
			int _channel = DEFAULT_TOKEN_CHANNEL;
			// /data1/hecker/gits/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDGThreadInstance_.g:49:6: ( ';' )
			// /data1/hecker/gits/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDGThreadInstance_.g:49:8: ';'
			{
			match(';'); 
			}

			state.type = _type;
			state.channel = _channel;
		}
		finally {
			// do for sure before leaving
		}
	}
	// $ANTLR end "T__9"

	// $ANTLR start "T__10"
	public final void mT__10() throws RecognitionException {
		try {
			int _type = T__10;
			int _channel = DEFAULT_TOKEN_CHANNEL;
			// /data1/hecker/gits/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDGThreadInstance_.g:50:7: ( 'Context' )
			// /data1/hecker/gits/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDGThreadInstance_.g:50:9: 'Context'
			{
			match("Context"); 

			}

			state.type = _type;
			state.channel = _channel;
		}
		finally {
			// do for sure before leaving
		}
	}
	// $ANTLR end "T__10"

	// $ANTLR start "T__11"
	public final void mT__11() throws RecognitionException {
		try {
			int _type = T__11;
			int _channel = DEFAULT_TOKEN_CHANNEL;
			// /data1/hecker/gits/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDGThreadInstance_.g:51:7: ( 'Dynamic' )
			// /data1/hecker/gits/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDGThreadInstance_.g:51:9: 'Dynamic'
			{
			match("Dynamic"); 

			}

			state.type = _type;
			state.channel = _channel;
		}
		finally {
			// do for sure before leaving
		}
	}
	// $ANTLR end "T__11"

	// $ANTLR start "T__12"
	public final void mT__12() throws RecognitionException {
		try {
			int _type = T__12;
			int _channel = DEFAULT_TOKEN_CHANNEL;
			// /data1/hecker/gits/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDGThreadInstance_.g:52:7: ( 'Entry' )
			// /data1/hecker/gits/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDGThreadInstance_.g:52:9: 'Entry'
			{
			match("Entry"); 

			}

			state.type = _type;
			state.channel = _channel;
		}
		finally {
			// do for sure before leaving
		}
	}
	// $ANTLR end "T__12"

	// $ANTLR start "T__13"
	public final void mT__13() throws RecognitionException {
		try {
			int _type = T__13;
			int _channel = DEFAULT_TOKEN_CHANNEL;
			// /data1/hecker/gits/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDGThreadInstance_.g:53:7: ( 'Exit' )
			// /data1/hecker/gits/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDGThreadInstance_.g:53:9: 'Exit'
			{
			match("Exit"); 

			}

			state.type = _type;
			state.channel = _channel;
		}
		finally {
			// do for sure before leaving
		}
	}
	// $ANTLR end "T__13"

	// $ANTLR start "T__14"
	public final void mT__14() throws RecognitionException {
		try {
			int _type = T__14;
			int _channel = DEFAULT_TOKEN_CHANNEL;
			// /data1/hecker/gits/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDGThreadInstance_.g:54:7: ( 'Fork' )
			// /data1/hecker/gits/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDGThreadInstance_.g:54:9: 'Fork'
			{
			match("Fork"); 

			}

			state.type = _type;
			state.channel = _channel;
		}
		finally {
			// do for sure before leaving
		}
	}
	// $ANTLR end "T__14"

	// $ANTLR start "T__15"
	public final void mT__15() throws RecognitionException {
		try {
			int _type = T__15;
			int _channel = DEFAULT_TOKEN_CHANNEL;
			// /data1/hecker/gits/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDGThreadInstance_.g:55:7: ( 'Join' )
			// /data1/hecker/gits/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDGThreadInstance_.g:55:9: 'Join'
			{
			match("Join"); 

			}

			state.type = _type;
			state.channel = _channel;
		}
		finally {
			// do for sure before leaving
		}
	}
	// $ANTLR end "T__15"

	// $ANTLR start "T__16"
	public final void mT__16() throws RecognitionException {
		try {
			int _type = T__16;
			int _channel = DEFAULT_TOKEN_CHANNEL;
			// /data1/hecker/gits/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDGThreadInstance_.g:56:7: ( 'Thread' )
			// /data1/hecker/gits/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDGThreadInstance_.g:56:9: 'Thread'
			{
			match("Thread"); 

			}

			state.type = _type;
			state.channel = _channel;
		}
		finally {
			// do for sure before leaving
		}
	}
	// $ANTLR end "T__16"

	// $ANTLR start "T__17"
	public final void mT__17() throws RecognitionException {
		try {
			int _type = T__17;
			int _channel = DEFAULT_TOKEN_CHANNEL;
			// /data1/hecker/gits/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDGThreadInstance_.g:57:7: ( '[' )
			// /data1/hecker/gits/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDGThreadInstance_.g:57:9: '['
			{
			match('['); 
			}

			state.type = _type;
			state.channel = _channel;
		}
		finally {
			// do for sure before leaving
		}
	}
	// $ANTLR end "T__17"

	// $ANTLR start "T__18"
	public final void mT__18() throws RecognitionException {
		try {
			int _type = T__18;
			int _channel = DEFAULT_TOKEN_CHANNEL;
			// /data1/hecker/gits/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDGThreadInstance_.g:58:7: ( ']' )
			// /data1/hecker/gits/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDGThreadInstance_.g:58:9: ']'
			{
			match(']'); 
			}

			state.type = _type;
			state.channel = _channel;
		}
		finally {
			// do for sure before leaving
		}
	}
	// $ANTLR end "T__18"

	// $ANTLR start "T__19"
	public final void mT__19() throws RecognitionException {
		try {
			int _type = T__19;
			int _channel = DEFAULT_TOKEN_CHANNEL;
			// /data1/hecker/gits/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDGThreadInstance_.g:59:7: ( 'false' )
			// /data1/hecker/gits/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDGThreadInstance_.g:59:9: 'false'
			{
			match("false"); 

			}

			state.type = _type;
			state.channel = _channel;
		}
		finally {
			// do for sure before leaving
		}
	}
	// $ANTLR end "T__19"

	// $ANTLR start "T__20"
	public final void mT__20() throws RecognitionException {
		try {
			int _type = T__20;
			int _channel = DEFAULT_TOKEN_CHANNEL;
			// /data1/hecker/gits/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDGThreadInstance_.g:60:7: ( 'null' )
			// /data1/hecker/gits/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDGThreadInstance_.g:60:9: 'null'
			{
			match("null"); 

			}

			state.type = _type;
			state.channel = _channel;
		}
		finally {
			// do for sure before leaving
		}
	}
	// $ANTLR end "T__20"

	// $ANTLR start "T__21"
	public final void mT__21() throws RecognitionException {
		try {
			int _type = T__21;
			int _channel = DEFAULT_TOKEN_CHANNEL;
			// /data1/hecker/gits/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDGThreadInstance_.g:61:7: ( 'true' )
			// /data1/hecker/gits/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDGThreadInstance_.g:61:9: 'true'
			{
			match("true"); 

			}

			state.type = _type;
			state.channel = _channel;
		}
		finally {
			// do for sure before leaving
		}
	}
	// $ANTLR end "T__21"

	// $ANTLR start "T__22"
	public final void mT__22() throws RecognitionException {
		try {
			int _type = T__22;
			int _channel = DEFAULT_TOKEN_CHANNEL;
			// /data1/hecker/gits/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDGThreadInstance_.g:62:7: ( '{' )
			// /data1/hecker/gits/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDGThreadInstance_.g:62:9: '{'
			{
			match('{'); 
			}

			state.type = _type;
			state.channel = _channel;
		}
		finally {
			// do for sure before leaving
		}
	}
	// $ANTLR end "T__22"

	// $ANTLR start "T__23"
	public final void mT__23() throws RecognitionException {
		try {
			int _type = T__23;
			int _channel = DEFAULT_TOKEN_CHANNEL;
			// /data1/hecker/gits/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDGThreadInstance_.g:63:7: ( '}' )
			// /data1/hecker/gits/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDGThreadInstance_.g:63:9: '}'
			{
			match('}'); 
			}

			state.type = _type;
			state.channel = _channel;
		}
		finally {
			// do for sure before leaving
		}
	}
	// $ANTLR end "T__23"

	// $ANTLR start "WHITESPACE"
	public final void mWHITESPACE() throws RecognitionException {
		try {
			int _type = WHITESPACE;
			int _channel = DEFAULT_TOKEN_CHANNEL;
			// /data1/hecker/gits/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDGThreadInstance_.g:219:3: ( ( ' ' | '\\t' | '\\n' | '\\r' ) )
			// /data1/hecker/gits/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDGThreadInstance_.g:219:5: ( ' ' | '\\t' | '\\n' | '\\r' )
			{
			if ( (input.LA(1) >= '\t' && input.LA(1) <= '\n')||input.LA(1)=='\r'||input.LA(1)==' ' ) {
				input.consume();
			}
			else {
				MismatchedSetException mse = new MismatchedSetException(null,input);
				recover(mse);
				throw mse;
			}
			 _channel=HIDDEN; 
			}

			state.type = _type;
			state.channel = _channel;
		}
		finally {
			// do for sure before leaving
		}
	}
	// $ANTLR end "WHITESPACE"

	// $ANTLR start "NUMBER"
	public final void mNUMBER() throws RecognitionException {
		try {
			int _type = NUMBER;
			int _channel = DEFAULT_TOKEN_CHANNEL;
			// /data1/hecker/gits/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDGThreadInstance_.g:223:3: ( ( '0' .. '9' )+ )
			// /data1/hecker/gits/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDGThreadInstance_.g:223:5: ( '0' .. '9' )+
			{
			// /data1/hecker/gits/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDGThreadInstance_.g:223:5: ( '0' .. '9' )+
			int cnt1=0;
			loop1:
			while (true) {
				int alt1=2;
				int LA1_0 = input.LA(1);
				if ( ((LA1_0 >= '0' && LA1_0 <= '9')) ) {
					alt1=1;
				}

				switch (alt1) {
				case 1 :
					// /data1/hecker/gits/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDGThreadInstance_.g:
					{
					if ( (input.LA(1) >= '0' && input.LA(1) <= '9') ) {
						input.consume();
					}
					else {
						MismatchedSetException mse = new MismatchedSetException(null,input);
						recover(mse);
						throw mse;
					}
					}
					break;

				default :
					if ( cnt1 >= 1 ) break loop1;
					EarlyExitException eee = new EarlyExitException(1, input);
					throw eee;
				}
				cnt1++;
			}

			}

			state.type = _type;
			state.channel = _channel;
		}
		finally {
			// do for sure before leaving
		}
	}
	// $ANTLR end "NUMBER"

	// $ANTLR start "STRING"
	public final void mSTRING() throws RecognitionException {
		try {
			int _type = STRING;
			int _channel = DEFAULT_TOKEN_CHANNEL;
			// /data1/hecker/gits/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDGThreadInstance_.g:227:3: ( '<' '\"' ({...}? '\"' |~ ( '\"' ) )* '\"' '>' | ( '\"' (~ '\"' )* '\"' ) )
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
					// /data1/hecker/gits/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDGThreadInstance_.g:227:5: '<' '\"' ({...}? '\"' |~ ( '\"' ) )* '\"' '>'
					{
					match('<'); 
					match('\"'); 
					// /data1/hecker/gits/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDGThreadInstance_.g:227:13: ({...}? '\"' |~ ( '\"' ) )*
					loop2:
					while (true) {
						int alt2=3;
						int LA2_0 = input.LA(1);
						if ( (LA2_0=='\"') ) {
							int LA2_1 = input.LA(2);
							if ( (LA2_1=='>') ) {
								int LA2_3 = input.LA(3);
								if ( ((LA2_3 >= '\u0000' && LA2_3 <= '\uFFFF')) ) {
									alt2=1;
								}

							}
							else if ( ((LA2_1 >= '\u0000' && LA2_1 <= '=')||(LA2_1 >= '?' && LA2_1 <= '\uFFFF')) ) {
								alt2=1;
							}

						}
						else if ( ((LA2_0 >= '\u0000' && LA2_0 <= '!')||(LA2_0 >= '#' && LA2_0 <= '\uFFFF')) ) {
							alt2=2;
						}

						switch (alt2) {
						case 1 :
							// /data1/hecker/gits/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDGThreadInstance_.g:227:15: {...}? '\"'
							{
							if ( !(( input.LA(2) != '>' )) ) {
								throw new FailedPredicateException(input, "STRING", " input.LA(2) != '>' ");
							}
							match('\"'); 
							}
							break;
						case 2 :
							// /data1/hecker/gits/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDGThreadInstance_.g:227:45: ~ ( '\"' )
							{
							if ( (input.LA(1) >= '\u0000' && input.LA(1) <= '!')||(input.LA(1) >= '#' && input.LA(1) <= '\uFFFF') ) {
								input.consume();
							}
							else {
								MismatchedSetException mse = new MismatchedSetException(null,input);
								recover(mse);
								throw mse;
							}
							}
							break;

						default :
							break loop2;
						}
					}

					match('\"'); 
					match('>'); 
					}
					break;
				case 2 :
					// /data1/hecker/gits/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDGThreadInstance_.g:228:5: ( '\"' (~ '\"' )* '\"' )
					{
					// /data1/hecker/gits/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDGThreadInstance_.g:228:5: ( '\"' (~ '\"' )* '\"' )
					// /data1/hecker/gits/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDGThreadInstance_.g:228:6: '\"' (~ '\"' )* '\"'
					{
					match('\"'); 
					// /data1/hecker/gits/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDGThreadInstance_.g:228:10: (~ '\"' )*
					loop3:
					while (true) {
						int alt3=2;
						int LA3_0 = input.LA(1);
						if ( ((LA3_0 >= '\u0000' && LA3_0 <= '!')||(LA3_0 >= '#' && LA3_0 <= '\uFFFF')) ) {
							alt3=1;
						}

						switch (alt3) {
						case 1 :
							// /data1/hecker/gits/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDGThreadInstance_.g:
							{
							if ( (input.LA(1) >= '\u0000' && input.LA(1) <= '!')||(input.LA(1) >= '#' && input.LA(1) <= '\uFFFF') ) {
								input.consume();
							}
							else {
								MismatchedSetException mse = new MismatchedSetException(null,input);
								recover(mse);
								throw mse;
							}
							}
							break;

						default :
							break loop3;
						}
					}

					match('\"'); 
					}

					}
					break;

			}
			state.type = _type;
			state.channel = _channel;
		}
		finally {
			// do for sure before leaving
		}
	}
	// $ANTLR end "STRING"

	@Override
	public void mTokens() throws RecognitionException {
		// /data1/hecker/gits/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDGThreadInstance_.g:1:8: ( T__7 | T__8 | T__9 | T__10 | T__11 | T__12 | T__13 | T__14 | T__15 | T__16 | T__17 | T__18 | T__19 | T__20 | T__21 | T__22 | T__23 | WHITESPACE | NUMBER | STRING )
		int alt5=20;
		switch ( input.LA(1) ) {
		case ',':
			{
			alt5=1;
			}
			break;
		case '-':
			{
			alt5=2;
			}
			break;
		case ';':
			{
			alt5=3;
			}
			break;
		case 'C':
			{
			alt5=4;
			}
			break;
		case 'D':
			{
			alt5=5;
			}
			break;
		case 'E':
			{
			int LA5_6 = input.LA(2);
			if ( (LA5_6=='n') ) {
				alt5=6;
			}
			else if ( (LA5_6=='x') ) {
				alt5=7;
			}

			else {
				int nvaeMark = input.mark();
				try {
					input.consume();
					NoViableAltException nvae =
						new NoViableAltException("", 5, 6, input);
					throw nvae;
				} finally {
					input.rewind(nvaeMark);
				}
			}

			}
			break;
		case 'F':
			{
			alt5=8;
			}
			break;
		case 'J':
			{
			alt5=9;
			}
			break;
		case 'T':
			{
			alt5=10;
			}
			break;
		case '[':
			{
			alt5=11;
			}
			break;
		case ']':
			{
			alt5=12;
			}
			break;
		case 'f':
			{
			alt5=13;
			}
			break;
		case 'n':
			{
			alt5=14;
			}
			break;
		case 't':
			{
			alt5=15;
			}
			break;
		case '{':
			{
			alt5=16;
			}
			break;
		case '}':
			{
			alt5=17;
			}
			break;
		case '\t':
		case '\n':
		case '\r':
		case ' ':
			{
			alt5=18;
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
			alt5=19;
			}
			break;
		case '\"':
		case '<':
			{
			alt5=20;
			}
			break;
		default:
			NoViableAltException nvae =
				new NoViableAltException("", 5, 0, input);
			throw nvae;
		}
		switch (alt5) {
			case 1 :
				// /data1/hecker/gits/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDGThreadInstance_.g:1:10: T__7
				{
				mT__7(); 

				}
				break;
			case 2 :
				// /data1/hecker/gits/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDGThreadInstance_.g:1:15: T__8
				{
				mT__8(); 

				}
				break;
			case 3 :
				// /data1/hecker/gits/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDGThreadInstance_.g:1:20: T__9
				{
				mT__9(); 

				}
				break;
			case 4 :
				// /data1/hecker/gits/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDGThreadInstance_.g:1:25: T__10
				{
				mT__10(); 

				}
				break;
			case 5 :
				// /data1/hecker/gits/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDGThreadInstance_.g:1:31: T__11
				{
				mT__11(); 

				}
				break;
			case 6 :
				// /data1/hecker/gits/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDGThreadInstance_.g:1:37: T__12
				{
				mT__12(); 

				}
				break;
			case 7 :
				// /data1/hecker/gits/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDGThreadInstance_.g:1:43: T__13
				{
				mT__13(); 

				}
				break;
			case 8 :
				// /data1/hecker/gits/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDGThreadInstance_.g:1:49: T__14
				{
				mT__14(); 

				}
				break;
			case 9 :
				// /data1/hecker/gits/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDGThreadInstance_.g:1:55: T__15
				{
				mT__15(); 

				}
				break;
			case 10 :
				// /data1/hecker/gits/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDGThreadInstance_.g:1:61: T__16
				{
				mT__16(); 

				}
				break;
			case 11 :
				// /data1/hecker/gits/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDGThreadInstance_.g:1:67: T__17
				{
				mT__17(); 

				}
				break;
			case 12 :
				// /data1/hecker/gits/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDGThreadInstance_.g:1:73: T__18
				{
				mT__18(); 

				}
				break;
			case 13 :
				// /data1/hecker/gits/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDGThreadInstance_.g:1:79: T__19
				{
				mT__19(); 

				}
				break;
			case 14 :
				// /data1/hecker/gits/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDGThreadInstance_.g:1:85: T__20
				{
				mT__20(); 

				}
				break;
			case 15 :
				// /data1/hecker/gits/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDGThreadInstance_.g:1:91: T__21
				{
				mT__21(); 

				}
				break;
			case 16 :
				// /data1/hecker/gits/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDGThreadInstance_.g:1:97: T__22
				{
				mT__22(); 

				}
				break;
			case 17 :
				// /data1/hecker/gits/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDGThreadInstance_.g:1:103: T__23
				{
				mT__23(); 

				}
				break;
			case 18 :
				// /data1/hecker/gits/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDGThreadInstance_.g:1:109: WHITESPACE
				{
				mWHITESPACE(); 

				}
				break;
			case 19 :
				// /data1/hecker/gits/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDGThreadInstance_.g:1:120: NUMBER
				{
				mNUMBER(); 

				}
				break;
			case 20 :
				// /data1/hecker/gits/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDGThreadInstance_.g:1:127: STRING
				{
				mSTRING(); 

				}
				break;

		}
	}



}
