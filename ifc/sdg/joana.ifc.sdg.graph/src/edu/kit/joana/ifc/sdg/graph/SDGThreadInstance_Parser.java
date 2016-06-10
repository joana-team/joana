// $ANTLR 3.5.2 /afs/info.uni-karlsruhe.de/user/bischof/joana/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDGThreadInstance_.g 2016-06-10 12:55:02
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
@SuppressWarnings("all")
public class SDGThreadInstance_Parser extends Parser {
	public static final String[] tokenNames = new String[] {
		"<invalid>", "<EOR>", "<DOWN>", "<UP>", "NUMBER", "STRING", "WHITESPACE", 
		"','", "'-'", "';'", "'Context'", "'Dynamic'", "'Entry'", "'Exit'", "'Fork'", 
		"'Join'", "'Thread'", "'['", "']'", "'false'", "'null'", "'true'", "'{'", 
		"'}'"
	};
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

	// delegates
	public Parser[] getDelegates() {
		return new Parser[] {};
	}

	// delegators


	public SDGThreadInstance_Parser(TokenStream input) {
		this(input, new RecognizerSharedState());
	}
	public SDGThreadInstance_Parser(TokenStream input, RecognizerSharedState state) {
		super(input, state);
	}

	@Override public String[] getTokenNames() { return SDGThreadInstance_Parser.tokenNames; }
	@Override public String getGrammarFileName() { return "/afs/info.uni-karlsruhe.de/user/bischof/joana/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDGThreadInstance_.g"; }



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
	// /afs/info.uni-karlsruhe.de/user/bischof/joana/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDGThreadInstance_.g:161:1: thread returns [ThreadInstanceStub ti] : 'Thread' id= number '{' 'Entry' en= number ';' 'Exit' ex= number ';' 'Fork' fo= number ';' 'Join' joins= listOrSingleNumber ';' 'Context' con= context ';' 'Dynamic' dyn= bool ';' '}' ;
	public final ThreadInstanceStub thread() throws RecognitionException {
		ThreadInstanceStub ti = null;


		int id =0;
		int en =0;
		int ex =0;
		int fo =0;
		TIntList joins =null;
		TIntList con =null;
		boolean dyn =false;

		try {
			// /afs/info.uni-karlsruhe.de/user/bischof/joana/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDGThreadInstance_.g:162:3: ( 'Thread' id= number '{' 'Entry' en= number ';' 'Exit' ex= number ';' 'Fork' fo= number ';' 'Join' joins= listOrSingleNumber ';' 'Context' con= context ';' 'Dynamic' dyn= bool ';' '}' )
			// /afs/info.uni-karlsruhe.de/user/bischof/joana/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDGThreadInstance_.g:162:5: 'Thread' id= number '{' 'Entry' en= number ';' 'Exit' ex= number ';' 'Fork' fo= number ';' 'Join' joins= listOrSingleNumber ';' 'Context' con= context ';' 'Dynamic' dyn= bool ';' '}'
			{
			match(input,16,FOLLOW_16_in_thread71); 
			pushFollow(FOLLOW_number_in_thread75);
			id=number();
			state._fsp--;

			match(input,22,FOLLOW_22_in_thread77); 
			match(input,12,FOLLOW_12_in_thread85); 
			pushFollow(FOLLOW_number_in_thread91);
			en=number();
			state._fsp--;

			match(input,9,FOLLOW_9_in_thread93); 
			match(input,13,FOLLOW_13_in_thread101); 
			pushFollow(FOLLOW_number_in_thread108);
			ex=number();
			state._fsp--;

			match(input,9,FOLLOW_9_in_thread110); 
			match(input,14,FOLLOW_14_in_thread118); 
			pushFollow(FOLLOW_number_in_thread125);
			fo=number();
			state._fsp--;

			match(input,9,FOLLOW_9_in_thread127); 
			match(input,15,FOLLOW_15_in_thread135); 
			pushFollow(FOLLOW_listOrSingleNumber_in_thread142);
			joins=listOrSingleNumber();
			state._fsp--;

			match(input,9,FOLLOW_9_in_thread144); 
			match(input,10,FOLLOW_10_in_thread152); 
			pushFollow(FOLLOW_context_in_thread156);
			con=context();
			state._fsp--;

			match(input,9,FOLLOW_9_in_thread158); 
			match(input,11,FOLLOW_11_in_thread166); 
			pushFollow(FOLLOW_bool_in_thread170);
			dyn=bool();
			state._fsp--;

			match(input,9,FOLLOW_9_in_thread172); 
			match(input,23,FOLLOW_23_in_thread178); 

			      final int entry = en;
			      int exit = ThreadInstanceStub.UNDEF_NODE; if (ex != 0) { exit = ex; }
			      int fork = ThreadInstanceStub.UNDEF_NODE; if (fo != 0) { fork = fo; }
			      ti = new ThreadInstanceStub(id, entry, exit, fork, joins, con, dyn);
			    
			}

		}
		catch (RecognitionException re) {
			reportError(re);
			recover(input,re);
		}
		finally {
			// do for sure before leaving
		}
		return ti;
	}
	// $ANTLR end "thread"



	// $ANTLR start "listOrSingleNumber"
	// /afs/info.uni-karlsruhe.de/user/bischof/joana/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDGThreadInstance_.g:178:9: private listOrSingleNumber returns [TIntList js] : (joins= mayEmptyNumberList |jo= number );
	public final TIntList listOrSingleNumber() throws RecognitionException {
		TIntList js = null;


		TIntList joins =null;
		int jo =0;

		try {
			// /afs/info.uni-karlsruhe.de/user/bischof/joana/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDGThreadInstance_.g:179:3: (joins= mayEmptyNumberList |jo= number )
			int alt1=2;
			int LA1_0 = input.LA(1);
			if ( (LA1_0==17||LA1_0==20) ) {
				alt1=1;
			}
			else if ( (LA1_0==NUMBER) ) {
				alt1=2;
			}

			else {
				NoViableAltException nvae =
					new NoViableAltException("", 1, 0, input);
				throw nvae;
			}

			switch (alt1) {
				case 1 :
					// /afs/info.uni-karlsruhe.de/user/bischof/joana/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDGThreadInstance_.g:179:5: joins= mayEmptyNumberList
					{
					pushFollow(FOLLOW_mayEmptyNumberList_in_listOrSingleNumber207);
					joins=mayEmptyNumberList();
					state._fsp--;

					 js = joins; 
					}
					break;
				case 2 :
					// /afs/info.uni-karlsruhe.de/user/bischof/joana/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDGThreadInstance_.g:180:5: jo= number
					{
					pushFollow(FOLLOW_number_in_listOrSingleNumber217);
					jo=number();
					state._fsp--;


					                js = new TIntArrayList();
					                int join = ThreadInstanceStub.UNDEF_NODE; if (jo != 0) { join = jo; }
					                js.add(join);
					              
					}
					break;

			}
		}
		catch (RecognitionException re) {
			reportError(re);
			recover(input,re);
		}
		finally {
			// do for sure before leaving
		}
		return js;
	}
	// $ANTLR end "listOrSingleNumber"



	// $ANTLR start "mayEmptyNumberList"
	// /afs/info.uni-karlsruhe.de/user/bischof/joana/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDGThreadInstance_.g:187:9: private mayEmptyNumberList returns [TIntList cx = new TIntArrayList();] : ( 'null' | '[' ']' | '[' i= number ( ',' i= number )* ']' );
	public final TIntList mayEmptyNumberList() throws RecognitionException {
		TIntList cx =  new TIntArrayList();;


		int i =0;

		try {
			// /afs/info.uni-karlsruhe.de/user/bischof/joana/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDGThreadInstance_.g:188:3: ( 'null' | '[' ']' | '[' i= number ( ',' i= number )* ']' )
			int alt3=3;
			int LA3_0 = input.LA(1);
			if ( (LA3_0==20) ) {
				alt3=1;
			}
			else if ( (LA3_0==17) ) {
				int LA3_2 = input.LA(2);
				if ( (LA3_2==18) ) {
					alt3=2;
				}
				else if ( (LA3_2==NUMBER) ) {
					alt3=3;
				}

				else {
					int nvaeMark = input.mark();
					try {
						input.consume();
						NoViableAltException nvae =
							new NoViableAltException("", 3, 2, input);
						throw nvae;
					} finally {
						input.rewind(nvaeMark);
					}
				}

			}

			else {
				NoViableAltException nvae =
					new NoViableAltException("", 3, 0, input);
				throw nvae;
			}

			switch (alt3) {
				case 1 :
					// /afs/info.uni-karlsruhe.de/user/bischof/joana/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDGThreadInstance_.g:188:5: 'null'
					{
					match(input,20,FOLLOW_20_in_mayEmptyNumberList238); 
					}
					break;
				case 2 :
					// /afs/info.uni-karlsruhe.de/user/bischof/joana/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDGThreadInstance_.g:189:5: '[' ']'
					{
					match(input,17,FOLLOW_17_in_mayEmptyNumberList244); 
					match(input,18,FOLLOW_18_in_mayEmptyNumberList246); 
					}
					break;
				case 3 :
					// /afs/info.uni-karlsruhe.de/user/bischof/joana/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDGThreadInstance_.g:190:5: '[' i= number ( ',' i= number )* ']'
					{
					match(input,17,FOLLOW_17_in_mayEmptyNumberList252); 
					pushFollow(FOLLOW_number_in_mayEmptyNumberList256);
					i=number();
					state._fsp--;

					 cx.add(i); 
					// /afs/info.uni-karlsruhe.de/user/bischof/joana/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDGThreadInstance_.g:190:33: ( ',' i= number )*
					loop2:
					while (true) {
						int alt2=2;
						int LA2_0 = input.LA(1);
						if ( (LA2_0==7) ) {
							alt2=1;
						}

						switch (alt2) {
						case 1 :
							// /afs/info.uni-karlsruhe.de/user/bischof/joana/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDGThreadInstance_.g:190:34: ',' i= number
							{
							match(input,7,FOLLOW_7_in_mayEmptyNumberList261); 
							pushFollow(FOLLOW_number_in_mayEmptyNumberList265);
							i=number();
							state._fsp--;

							 cx.add(i); 
							}
							break;

						default :
							break loop2;
						}
					}

					match(input,18,FOLLOW_18_in_mayEmptyNumberList272); 
					}
					break;

			}
		}
		catch (RecognitionException re) {
			reportError(re);
			recover(input,re);
		}
		finally {
			// do for sure before leaving
		}
		return cx;
	}
	// $ANTLR end "mayEmptyNumberList"



	// $ANTLR start "context"
	// /afs/info.uni-karlsruhe.de/user/bischof/joana/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDGThreadInstance_.g:193:9: private context returns [TIntList cx = new TIntArrayList();] : ( 'null' | '[' i= mayNegNumber ( ',' i= mayNegNumber )* ']' );
	public final TIntList context() throws RecognitionException {
		TIntList cx =  new TIntArrayList();;


		int i =0;

		try {
			// /afs/info.uni-karlsruhe.de/user/bischof/joana/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDGThreadInstance_.g:194:3: ( 'null' | '[' i= mayNegNumber ( ',' i= mayNegNumber )* ']' )
			int alt5=2;
			int LA5_0 = input.LA(1);
			if ( (LA5_0==20) ) {
				alt5=1;
			}
			else if ( (LA5_0==17) ) {
				alt5=2;
			}

			else {
				NoViableAltException nvae =
					new NoViableAltException("", 5, 0, input);
				throw nvae;
			}

			switch (alt5) {
				case 1 :
					// /afs/info.uni-karlsruhe.de/user/bischof/joana/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDGThreadInstance_.g:194:5: 'null'
					{
					match(input,20,FOLLOW_20_in_context291); 
					}
					break;
				case 2 :
					// /afs/info.uni-karlsruhe.de/user/bischof/joana/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDGThreadInstance_.g:195:5: '[' i= mayNegNumber ( ',' i= mayNegNumber )* ']'
					{
					match(input,17,FOLLOW_17_in_context297); 
					pushFollow(FOLLOW_mayNegNumber_in_context301);
					i=mayNegNumber();
					state._fsp--;

					 cx.add(i); 
					// /afs/info.uni-karlsruhe.de/user/bischof/joana/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDGThreadInstance_.g:195:39: ( ',' i= mayNegNumber )*
					loop4:
					while (true) {
						int alt4=2;
						int LA4_0 = input.LA(1);
						if ( (LA4_0==7) ) {
							alt4=1;
						}

						switch (alt4) {
						case 1 :
							// /afs/info.uni-karlsruhe.de/user/bischof/joana/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDGThreadInstance_.g:195:40: ',' i= mayNegNumber
							{
							match(input,7,FOLLOW_7_in_context306); 
							pushFollow(FOLLOW_mayNegNumber_in_context310);
							i=mayNegNumber();
							state._fsp--;

							 cx.add(i); 
							}
							break;

						default :
							break loop4;
						}
					}

					match(input,18,FOLLOW_18_in_context317); 
					}
					break;

			}
		}
		catch (RecognitionException re) {
			reportError(re);
			recover(input,re);
		}
		finally {
			// do for sure before leaving
		}
		return cx;
	}
	// $ANTLR end "context"



	// $ANTLR start "mayNegNumber"
	// /afs/info.uni-karlsruhe.de/user/bischof/joana/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDGThreadInstance_.g:198:9: private mayNegNumber returns [int nr] : ( '-' n= number |n= number );
	public final int mayNegNumber() throws RecognitionException {
		int nr = 0;


		int n =0;

		try {
			// /afs/info.uni-karlsruhe.de/user/bischof/joana/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDGThreadInstance_.g:199:3: ( '-' n= number |n= number )
			int alt6=2;
			int LA6_0 = input.LA(1);
			if ( (LA6_0==8) ) {
				alt6=1;
			}
			else if ( (LA6_0==NUMBER) ) {
				alt6=2;
			}

			else {
				NoViableAltException nvae =
					new NoViableAltException("", 6, 0, input);
				throw nvae;
			}

			switch (alt6) {
				case 1 :
					// /afs/info.uni-karlsruhe.de/user/bischof/joana/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDGThreadInstance_.g:199:5: '-' n= number
					{
					match(input,8,FOLLOW_8_in_mayNegNumber336); 
					pushFollow(FOLLOW_number_in_mayNegNumber340);
					n=number();
					state._fsp--;

					 nr = -n; 
					}
					break;
				case 2 :
					// /afs/info.uni-karlsruhe.de/user/bischof/joana/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDGThreadInstance_.g:200:5: n= number
					{
					pushFollow(FOLLOW_number_in_mayNegNumber350);
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
			// do for sure before leaving
		}
		return nr;
	}
	// $ANTLR end "mayNegNumber"



	// $ANTLR start "number"
	// /afs/info.uni-karlsruhe.de/user/bischof/joana/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDGThreadInstance_.g:203:9: private number returns [int nr] : n= NUMBER ;
	public final int number() throws RecognitionException {
		int nr = 0;


		Token n=null;

		try {
			// /afs/info.uni-karlsruhe.de/user/bischof/joana/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDGThreadInstance_.g:204:3: (n= NUMBER )
			// /afs/info.uni-karlsruhe.de/user/bischof/joana/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDGThreadInstance_.g:204:5: n= NUMBER
			{
			n=(Token)match(input,NUMBER,FOLLOW_NUMBER_in_number373); 
			 nr = Integer.parseInt(n.getText()); 
			}

		}
		catch (RecognitionException re) {
			reportError(re);
			recover(input,re);
		}
		finally {
			// do for sure before leaving
		}
		return nr;
	}
	// $ANTLR end "number"



	// $ANTLR start "string"
	// /afs/info.uni-karlsruhe.de/user/bischof/joana/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDGThreadInstance_.g:207:9: private string returns [String str] : s= STRING ;
	public final String string() throws RecognitionException {
		String str = null;


		Token s=null;

		try {
			// /afs/info.uni-karlsruhe.de/user/bischof/joana/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDGThreadInstance_.g:208:3: (s= STRING )
			// /afs/info.uni-karlsruhe.de/user/bischof/joana/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDGThreadInstance_.g:208:5: s= STRING
			{
			s=(Token)match(input,STRING,FOLLOW_STRING_in_string396); 
			 str = s.getText(); str = str.substring(1, str.length() - 1); 
			}

		}
		catch (RecognitionException re) {
			reportError(re);
			recover(input,re);
		}
		finally {
			// do for sure before leaving
		}
		return str;
	}
	// $ANTLR end "string"



	// $ANTLR start "bool"
	// /afs/info.uni-karlsruhe.de/user/bischof/joana/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDGThreadInstance_.g:211:9: private bool returns [boolean b] : ( 'true' | 'false' );
	public final boolean bool() throws RecognitionException {
		boolean b = false;


		try {
			// /afs/info.uni-karlsruhe.de/user/bischof/joana/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDGThreadInstance_.g:212:3: ( 'true' | 'false' )
			int alt7=2;
			int LA7_0 = input.LA(1);
			if ( (LA7_0==21) ) {
				alt7=1;
			}
			else if ( (LA7_0==19) ) {
				alt7=2;
			}

			else {
				NoViableAltException nvae =
					new NoViableAltException("", 7, 0, input);
				throw nvae;
			}

			switch (alt7) {
				case 1 :
					// /afs/info.uni-karlsruhe.de/user/bischof/joana/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDGThreadInstance_.g:212:5: 'true'
					{
					match(input,21,FOLLOW_21_in_bool417); 
					 b = true; 
					}
					break;
				case 2 :
					// /afs/info.uni-karlsruhe.de/user/bischof/joana/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDGThreadInstance_.g:213:5: 'false'
					{
					match(input,19,FOLLOW_19_in_bool426); 
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
			// do for sure before leaving
		}
		return b;
	}
	// $ANTLR end "bool"

	// Delegated rules



	public static final BitSet FOLLOW_16_in_thread71 = new BitSet(new long[]{0x0000000000000010L});
	public static final BitSet FOLLOW_number_in_thread75 = new BitSet(new long[]{0x0000000000400000L});
	public static final BitSet FOLLOW_22_in_thread77 = new BitSet(new long[]{0x0000000000001000L});
	public static final BitSet FOLLOW_12_in_thread85 = new BitSet(new long[]{0x0000000000000010L});
	public static final BitSet FOLLOW_number_in_thread91 = new BitSet(new long[]{0x0000000000000200L});
	public static final BitSet FOLLOW_9_in_thread93 = new BitSet(new long[]{0x0000000000002000L});
	public static final BitSet FOLLOW_13_in_thread101 = new BitSet(new long[]{0x0000000000000010L});
	public static final BitSet FOLLOW_number_in_thread108 = new BitSet(new long[]{0x0000000000000200L});
	public static final BitSet FOLLOW_9_in_thread110 = new BitSet(new long[]{0x0000000000004000L});
	public static final BitSet FOLLOW_14_in_thread118 = new BitSet(new long[]{0x0000000000000010L});
	public static final BitSet FOLLOW_number_in_thread125 = new BitSet(new long[]{0x0000000000000200L});
	public static final BitSet FOLLOW_9_in_thread127 = new BitSet(new long[]{0x0000000000008000L});
	public static final BitSet FOLLOW_15_in_thread135 = new BitSet(new long[]{0x0000000000120010L});
	public static final BitSet FOLLOW_listOrSingleNumber_in_thread142 = new BitSet(new long[]{0x0000000000000200L});
	public static final BitSet FOLLOW_9_in_thread144 = new BitSet(new long[]{0x0000000000000400L});
	public static final BitSet FOLLOW_10_in_thread152 = new BitSet(new long[]{0x0000000000120000L});
	public static final BitSet FOLLOW_context_in_thread156 = new BitSet(new long[]{0x0000000000000200L});
	public static final BitSet FOLLOW_9_in_thread158 = new BitSet(new long[]{0x0000000000000800L});
	public static final BitSet FOLLOW_11_in_thread166 = new BitSet(new long[]{0x0000000000280000L});
	public static final BitSet FOLLOW_bool_in_thread170 = new BitSet(new long[]{0x0000000000000200L});
	public static final BitSet FOLLOW_9_in_thread172 = new BitSet(new long[]{0x0000000000800000L});
	public static final BitSet FOLLOW_23_in_thread178 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_mayEmptyNumberList_in_listOrSingleNumber207 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_number_in_listOrSingleNumber217 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_20_in_mayEmptyNumberList238 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_17_in_mayEmptyNumberList244 = new BitSet(new long[]{0x0000000000040000L});
	public static final BitSet FOLLOW_18_in_mayEmptyNumberList246 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_17_in_mayEmptyNumberList252 = new BitSet(new long[]{0x0000000000000010L});
	public static final BitSet FOLLOW_number_in_mayEmptyNumberList256 = new BitSet(new long[]{0x0000000000040080L});
	public static final BitSet FOLLOW_7_in_mayEmptyNumberList261 = new BitSet(new long[]{0x0000000000000010L});
	public static final BitSet FOLLOW_number_in_mayEmptyNumberList265 = new BitSet(new long[]{0x0000000000040080L});
	public static final BitSet FOLLOW_18_in_mayEmptyNumberList272 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_20_in_context291 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_17_in_context297 = new BitSet(new long[]{0x0000000000000110L});
	public static final BitSet FOLLOW_mayNegNumber_in_context301 = new BitSet(new long[]{0x0000000000040080L});
	public static final BitSet FOLLOW_7_in_context306 = new BitSet(new long[]{0x0000000000000110L});
	public static final BitSet FOLLOW_mayNegNumber_in_context310 = new BitSet(new long[]{0x0000000000040080L});
	public static final BitSet FOLLOW_18_in_context317 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_8_in_mayNegNumber336 = new BitSet(new long[]{0x0000000000000010L});
	public static final BitSet FOLLOW_number_in_mayNegNumber340 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_number_in_mayNegNumber350 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_NUMBER_in_number373 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_STRING_in_string396 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_21_in_bool417 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_19_in_bool426 = new BitSet(new long[]{0x0000000000000002L});
}
