// $ANTLR 3.5.2 /afs/info.uni-karlsruhe.de/user/bischof/joana/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g 2016-06-10 12:46:51
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
@SuppressWarnings("all")
public class SDG_Parser extends Parser {
	public static final String[] tokenNames = new String[] {
		"<invalid>", "<EOR>", "<DOWN>", "<UP>", "NUMBER", "STRING", "WHITESPACE", 
		"','", "'-'", "':'", "';'", "'A'", "'ACTI'", "'ACTO'", "'B'", "'C'", "'CALL'", 
		"'CC'", "'CD'", "'CE'", "'CF'", "'CL'", "'Context'", "'D'", "'DA'", "'DD'", 
		"'DH'", "'Dynamic'", "'ENTR'", "'EXIT'", "'EXPR'", "'Entry'", "'Exit'", 
		"'FD'", "'FI'", "'FOLD'", "'FORK'", "'FORK_IN'", "'FORK_OUT'", "'FRMI'", 
		"'FRMO'", "'Fork'", "'HE'", "'ID'", "'IF'", "'IW'", "'JComp'", "'JD'", 
		"'JF'", "'JOIN'", "'Join'", "'N'", "'NF'", "'NORM'", "'O'", "'P'", "'PI'", 
		"'PO'", "'PRED'", "'PS'", "'RD'", "'RF'", "'RY'", "'S'", "'SD'", "'SDG'", 
		"'SF'", "'SH'", "'SP'", "'SU'", "'SYNC'", "'T'", "'Thread'", "'U'", "'UN'", 
		"'V'", "'VD'", "'Z'", "'['", "']'", "'act-in'", "'act-out'", "'array'", 
		"'assign'", "'binary'", "'call'", "'charconst'", "'compound'", "'declaration'", 
		"'derefer'", "'empty'", "'entry'", "'exit'", "'false'", "'floatconst'", 
		"'form-ellip'", "'form-in'", "'form-out'", "'functionconst'", "'intconst'", 
		"'jump'", "'loop'", "'modassign'", "'modify'", "'monitor'", "'null'", 
		"'question'", "'refer'", "'reference'", "'select'", "'shortcut'", "'stringconst'", 
		"'true'", "'unary'", "'v'", "'{'", "'}'"
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
	public static final int T__24=24;
	public static final int T__25=25;
	public static final int T__26=26;
	public static final int T__27=27;
	public static final int T__28=28;
	public static final int T__29=29;
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
	public static final int T__40=40;
	public static final int T__41=41;
	public static final int T__42=42;
	public static final int T__43=43;
	public static final int T__44=44;
	public static final int T__45=45;
	public static final int T__46=46;
	public static final int T__47=47;
	public static final int T__48=48;
	public static final int T__49=49;
	public static final int T__50=50;
	public static final int T__51=51;
	public static final int T__52=52;
	public static final int T__53=53;
	public static final int T__54=54;
	public static final int T__55=55;
	public static final int T__56=56;
	public static final int T__57=57;
	public static final int T__58=58;
	public static final int T__59=59;
	public static final int T__60=60;
	public static final int T__61=61;
	public static final int T__62=62;
	public static final int T__63=63;
	public static final int T__64=64;
	public static final int T__65=65;
	public static final int T__66=66;
	public static final int T__67=67;
	public static final int T__68=68;
	public static final int T__69=69;
	public static final int T__70=70;
	public static final int T__71=71;
	public static final int T__72=72;
	public static final int T__73=73;
	public static final int T__74=74;
	public static final int T__75=75;
	public static final int T__76=76;
	public static final int T__77=77;
	public static final int T__78=78;
	public static final int T__79=79;
	public static final int T__80=80;
	public static final int T__81=81;
	public static final int T__82=82;
	public static final int T__83=83;
	public static final int T__84=84;
	public static final int T__85=85;
	public static final int T__86=86;
	public static final int T__87=87;
	public static final int T__88=88;
	public static final int T__89=89;
	public static final int T__90=90;
	public static final int T__91=91;
	public static final int T__92=92;
	public static final int T__93=93;
	public static final int T__94=94;
	public static final int T__95=95;
	public static final int T__96=96;
	public static final int T__97=97;
	public static final int T__98=98;
	public static final int T__99=99;
	public static final int T__100=100;
	public static final int T__101=101;
	public static final int T__102=102;
	public static final int T__103=103;
	public static final int T__104=104;
	public static final int T__105=105;
	public static final int T__106=106;
	public static final int T__107=107;
	public static final int T__108=108;
	public static final int T__109=109;
	public static final int T__110=110;
	public static final int T__111=111;
	public static final int T__112=112;
	public static final int T__113=113;
	public static final int T__114=114;
	public static final int T__115=115;
	public static final int T__116=116;
	public static final int NUMBER=4;
	public static final int STRING=5;
	public static final int WHITESPACE=6;

	// delegates
	public Parser[] getDelegates() {
		return new Parser[] {};
	}

	// delegators


	public SDG_Parser(TokenStream input) {
		this(input, new RecognizerSharedState());
	}
	public SDG_Parser(TokenStream input, RecognizerSharedState state) {
		super(input, state);
	}

	@Override public String[] getTokenNames() { return SDG_Parser.tokenNames; }
	@Override public String getGrammarFileName() { return "/afs/info.uni-karlsruhe.de/user/bischof/joana/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g"; }


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
	// /afs/info.uni-karlsruhe.de/user/bischof/joana/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:270:1: sdg_file returns [SDG sdg] : head= sdg_header '{' ( 'JComp' )? nl= node_list (ti= thread_info[sdg] )? '}' ;
	public final SDG sdg_file() throws RecognitionException {
		SDG sdg = null;


		SDGHeader head =null;
		List<SDGNodeStub> nl =null;
		ThreadsInformation ti =null;

		try {
			// /afs/info.uni-karlsruhe.de/user/bischof/joana/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:271:3: (head= sdg_header '{' ( 'JComp' )? nl= node_list (ti= thread_info[sdg] )? '}' )
			// /afs/info.uni-karlsruhe.de/user/bischof/joana/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:271:5: head= sdg_header '{' ( 'JComp' )? nl= node_list (ti= thread_info[sdg] )? '}'
			{
			pushFollow(FOLLOW_sdg_header_in_sdg_file73);
			head=sdg_header();
			state._fsp--;

			 sdg = head.createSDG(); 
			match(input,115,FOLLOW_115_in_sdg_file89); 
			// /afs/info.uni-karlsruhe.de/user/bischof/joana/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:273:7: ( 'JComp' )?
			int alt1=2;
			int LA1_0 = input.LA(1);
			if ( (LA1_0==46) ) {
				alt1=1;
			}
			switch (alt1) {
				case 1 :
					// /afs/info.uni-karlsruhe.de/user/bischof/joana/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:273:8: 'JComp'
					{
					match(input,46,FOLLOW_46_in_sdg_file99); 
					 sdg.setJoanaCompiler(true); 
					}
					break;

			}

			pushFollow(FOLLOW_node_list_in_sdg_file128);
			nl=node_list();
			state._fsp--;

			 createNodesAndEdges(sdg, nl); 
			// /afs/info.uni-karlsruhe.de/user/bischof/joana/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:275:7: (ti= thread_info[sdg] )?
			int alt2=2;
			int LA2_0 = input.LA(1);
			if ( (LA2_0==72) ) {
				alt2=1;
			}
			switch (alt2) {
				case 1 :
					// /afs/info.uni-karlsruhe.de/user/bischof/joana/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:275:8: ti= thread_info[sdg]
					{
					pushFollow(FOLLOW_thread_info_in_sdg_file150);
					ti=thread_info(sdg);
					state._fsp--;

					 sdg.setThreadsInfo(ti); 
					}
					break;

			}

			match(input,116,FOLLOW_116_in_sdg_file163); 
			}

		}
		catch (RecognitionException re) {
			reportError(re);
			recover(input,re);
		}
		finally {
			// do for sure before leaving
		}
		return sdg;
	}
	// $ANTLR end "sdg_file"



	// $ANTLR start "thread_info"
	// /afs/info.uni-karlsruhe.de/user/bischof/joana/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:279:9: private thread_info[SDG sdg] returns [ThreadsInformation tinfo] : (t= thread[sdg] )+ ;
	public final ThreadsInformation thread_info(SDG sdg) throws RecognitionException {
		ThreadsInformation tinfo = null;


		ThreadInstance t =null;

		try {
			// /afs/info.uni-karlsruhe.de/user/bischof/joana/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:280:3: ( (t= thread[sdg] )+ )
			// /afs/info.uni-karlsruhe.de/user/bischof/joana/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:280:5: (t= thread[sdg] )+
			{
			 final LinkedList<ThreadInstance> tis = new LinkedList<ThreadInstance>(); 
			// /afs/info.uni-karlsruhe.de/user/bischof/joana/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:281:5: (t= thread[sdg] )+
			int cnt3=0;
			loop3:
			while (true) {
				int alt3=2;
				int LA3_0 = input.LA(1);
				if ( (LA3_0==72) ) {
					alt3=1;
				}

				switch (alt3) {
				case 1 :
					// /afs/info.uni-karlsruhe.de/user/bischof/joana/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:281:6: t= thread[sdg]
					{
					pushFollow(FOLLOW_thread_in_thread_info192);
					t=thread(sdg);
					state._fsp--;

					 tis.add(t); 
					}
					break;

				default :
					if ( cnt3 >= 1 ) break loop3;
					EarlyExitException eee = new EarlyExitException(3, input);
					throw eee;
				}
				cnt3++;
			}

			 tinfo = new ThreadsInformation(tis); 
			}

		}
		catch (RecognitionException re) {
			reportError(re);
			recover(input,re);
		}
		finally {
			// do for sure before leaving
		}
		return tinfo;
	}
	// $ANTLR end "thread_info"



	// $ANTLR start "thread"
	// /afs/info.uni-karlsruhe.de/user/bischof/joana/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:285:9: private thread[SDG sdg] returns [ThreadInstance ti] : 'Thread' id= number '{' 'Entry' en= number ';' 'Exit' ex= number ';' 'Fork' fo= number ';' 'Join' joins= listOrSingleNumber[sdg] ';' 'Context' con= context[sdg] ';' 'Dynamic' dyn= bool ';' '}' ;
	public final ThreadInstance thread(SDG sdg) throws RecognitionException {
		ThreadInstance ti = null;


		int id =0;
		int en =0;
		int ex =0;
		int fo =0;
		LinkedList<SDGNode> joins =null;
		LinkedList<SDGNode> con =null;
		boolean dyn =false;

		try {
			// /afs/info.uni-karlsruhe.de/user/bischof/joana/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:286:3: ( 'Thread' id= number '{' 'Entry' en= number ';' 'Exit' ex= number ';' 'Fork' fo= number ';' 'Join' joins= listOrSingleNumber[sdg] ';' 'Context' con= context[sdg] ';' 'Dynamic' dyn= bool ';' '}' )
			// /afs/info.uni-karlsruhe.de/user/bischof/joana/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:286:5: 'Thread' id= number '{' 'Entry' en= number ';' 'Exit' ex= number ';' 'Fork' fo= number ';' 'Join' joins= listOrSingleNumber[sdg] ';' 'Context' con= context[sdg] ';' 'Dynamic' dyn= bool ';' '}'
			{
			match(input,72,FOLLOW_72_in_thread224); 
			pushFollow(FOLLOW_number_in_thread228);
			id=number();
			state._fsp--;

			match(input,115,FOLLOW_115_in_thread230); 
			match(input,31,FOLLOW_31_in_thread238); 
			pushFollow(FOLLOW_number_in_thread244);
			en=number();
			state._fsp--;

			match(input,10,FOLLOW_10_in_thread246); 
			match(input,32,FOLLOW_32_in_thread254); 
			pushFollow(FOLLOW_number_in_thread261);
			ex=number();
			state._fsp--;

			match(input,10,FOLLOW_10_in_thread263); 
			match(input,41,FOLLOW_41_in_thread271); 
			pushFollow(FOLLOW_number_in_thread278);
			fo=number();
			state._fsp--;

			match(input,10,FOLLOW_10_in_thread280); 
			match(input,50,FOLLOW_50_in_thread288); 
			pushFollow(FOLLOW_listOrSingleNumber_in_thread295);
			joins=listOrSingleNumber(sdg);
			state._fsp--;

			match(input,10,FOLLOW_10_in_thread298); 
			match(input,22,FOLLOW_22_in_thread306); 
			pushFollow(FOLLOW_context_in_thread310);
			con=context(sdg);
			state._fsp--;

			match(input,10,FOLLOW_10_in_thread313); 
			match(input,27,FOLLOW_27_in_thread321); 
			pushFollow(FOLLOW_bool_in_thread325);
			dyn=bool();
			state._fsp--;

			match(input,10,FOLLOW_10_in_thread327); 
			match(input,116,FOLLOW_116_in_thread333); 

			      final SDGNode entry = sdg.getNode(en);
			      SDGNode exit = null; if (ex != 0) { exit = sdg.getNode(ex); }
			      SDGNode fork = null; if (fo != 0) { fork = sdg.getNode(fo); }
			      ti = new ThreadInstance(id, entry, exit, fork, joins, con, dyn);
			    
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
	// /afs/info.uni-karlsruhe.de/user/bischof/joana/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:302:9: private listOrSingleNumber[SDG sdg] returns [LinkedList<SDGNode> js] : (joins= mayEmptyNumberList[sdg] |jo= number );
	public final LinkedList<SDGNode> listOrSingleNumber(SDG sdg) throws RecognitionException {
		LinkedList<SDGNode> js = null;


		LinkedList<SDGNode> joins =null;
		int jo =0;

		try {
			// /afs/info.uni-karlsruhe.de/user/bischof/joana/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:303:3: (joins= mayEmptyNumberList[sdg] |jo= number )
			int alt4=2;
			int LA4_0 = input.LA(1);
			if ( (LA4_0==78||LA4_0==105) ) {
				alt4=1;
			}
			else if ( (LA4_0==NUMBER) ) {
				alt4=2;
			}

			else {
				NoViableAltException nvae =
					new NoViableAltException("", 4, 0, input);
				throw nvae;
			}

			switch (alt4) {
				case 1 :
					// /afs/info.uni-karlsruhe.de/user/bischof/joana/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:303:5: joins= mayEmptyNumberList[sdg]
					{
					pushFollow(FOLLOW_mayEmptyNumberList_in_listOrSingleNumber363);
					joins=mayEmptyNumberList(sdg);
					state._fsp--;

					 js = joins; 
					}
					break;
				case 2 :
					// /afs/info.uni-karlsruhe.de/user/bischof/joana/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:304:5: jo= number
					{
					pushFollow(FOLLOW_number_in_listOrSingleNumber374);
					jo=number();
					state._fsp--;


					                js = new LinkedList<SDGNode>();
					                if (jo != 0) { js.add(sdg.getNode(jo)); }
					              
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
	// /afs/info.uni-karlsruhe.de/user/bischof/joana/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:310:9: private mayEmptyNumberList[SDG sdg] returns [LinkedList<SDGNode> js = new LinkedList<SDGNode>();] : ( 'null' | '[' ']' | '[' i= number ( ',' i= number )* ']' );
	public final LinkedList<SDGNode> mayEmptyNumberList(SDG sdg) throws RecognitionException {
		LinkedList<SDGNode> js =  new LinkedList<SDGNode>();;


		int i =0;

		try {
			// /afs/info.uni-karlsruhe.de/user/bischof/joana/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:311:3: ( 'null' | '[' ']' | '[' i= number ( ',' i= number )* ']' )
			int alt6=3;
			int LA6_0 = input.LA(1);
			if ( (LA6_0==105) ) {
				alt6=1;
			}
			else if ( (LA6_0==78) ) {
				int LA6_2 = input.LA(2);
				if ( (LA6_2==79) ) {
					alt6=2;
				}
				else if ( (LA6_2==NUMBER) ) {
					alt6=3;
				}

				else {
					int nvaeMark = input.mark();
					try {
						input.consume();
						NoViableAltException nvae =
							new NoViableAltException("", 6, 2, input);
						throw nvae;
					} finally {
						input.rewind(nvaeMark);
					}
				}

			}

			else {
				NoViableAltException nvae =
					new NoViableAltException("", 6, 0, input);
				throw nvae;
			}

			switch (alt6) {
				case 1 :
					// /afs/info.uni-karlsruhe.de/user/bischof/joana/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:311:5: 'null'
					{
					match(input,105,FOLLOW_105_in_mayEmptyNumberList396); 
					}
					break;
				case 2 :
					// /afs/info.uni-karlsruhe.de/user/bischof/joana/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:312:5: '[' ']'
					{
					match(input,78,FOLLOW_78_in_mayEmptyNumberList402); 
					match(input,79,FOLLOW_79_in_mayEmptyNumberList404); 
					}
					break;
				case 3 :
					// /afs/info.uni-karlsruhe.de/user/bischof/joana/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:313:5: '[' i= number ( ',' i= number )* ']'
					{
					match(input,78,FOLLOW_78_in_mayEmptyNumberList410); 
					pushFollow(FOLLOW_number_in_mayEmptyNumberList414);
					i=number();
					state._fsp--;

					 js.add(sdg.getNode(i)); 
					// /afs/info.uni-karlsruhe.de/user/bischof/joana/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:313:46: ( ',' i= number )*
					loop5:
					while (true) {
						int alt5=2;
						int LA5_0 = input.LA(1);
						if ( (LA5_0==7) ) {
							alt5=1;
						}

						switch (alt5) {
						case 1 :
							// /afs/info.uni-karlsruhe.de/user/bischof/joana/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:313:47: ',' i= number
							{
							match(input,7,FOLLOW_7_in_mayEmptyNumberList419); 
							pushFollow(FOLLOW_number_in_mayEmptyNumberList423);
							i=number();
							state._fsp--;

							 js.add(sdg.getNode(i)); 
							}
							break;

						default :
							break loop5;
						}
					}

					match(input,79,FOLLOW_79_in_mayEmptyNumberList430); 
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
	// $ANTLR end "mayEmptyNumberList"



	// $ANTLR start "context"
	// /afs/info.uni-karlsruhe.de/user/bischof/joana/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:316:9: private context[SDG sdg] returns [LinkedList<SDGNode> cx = new LinkedList<SDGNode>();] : ( 'null' | '[' i= mayNegNumber ( ',' i= mayNegNumber )* ']' );
	public final LinkedList<SDGNode> context(SDG sdg) throws RecognitionException {
		LinkedList<SDGNode> cx =  new LinkedList<SDGNode>();;


		int i =0;

		try {
			// /afs/info.uni-karlsruhe.de/user/bischof/joana/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:317:3: ( 'null' | '[' i= mayNegNumber ( ',' i= mayNegNumber )* ']' )
			int alt8=2;
			int LA8_0 = input.LA(1);
			if ( (LA8_0==105) ) {
				alt8=1;
			}
			else if ( (LA8_0==78) ) {
				alt8=2;
			}

			else {
				NoViableAltException nvae =
					new NoViableAltException("", 8, 0, input);
				throw nvae;
			}

			switch (alt8) {
				case 1 :
					// /afs/info.uni-karlsruhe.de/user/bischof/joana/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:317:5: 'null'
					{
					match(input,105,FOLLOW_105_in_context452); 
					}
					break;
				case 2 :
					// /afs/info.uni-karlsruhe.de/user/bischof/joana/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:318:5: '[' i= mayNegNumber ( ',' i= mayNegNumber )* ']'
					{
					match(input,78,FOLLOW_78_in_context458); 
					pushFollow(FOLLOW_mayNegNumber_in_context462);
					i=mayNegNumber();
					state._fsp--;

					 cx.add(sdg.getNode(i)); 
					// /afs/info.uni-karlsruhe.de/user/bischof/joana/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:318:52: ( ',' i= mayNegNumber )*
					loop7:
					while (true) {
						int alt7=2;
						int LA7_0 = input.LA(1);
						if ( (LA7_0==7) ) {
							alt7=1;
						}

						switch (alt7) {
						case 1 :
							// /afs/info.uni-karlsruhe.de/user/bischof/joana/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:318:53: ',' i= mayNegNumber
							{
							match(input,7,FOLLOW_7_in_context467); 
							pushFollow(FOLLOW_mayNegNumber_in_context471);
							i=mayNegNumber();
							state._fsp--;

							 cx.add(sdg.getNode(i)); 
							}
							break;

						default :
							break loop7;
						}
					}

					match(input,79,FOLLOW_79_in_context478); 
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



	// $ANTLR start "sdg_header"
	// /afs/info.uni-karlsruhe.de/user/bischof/joana/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:321:9: private sdg_header returns [SDGHeader header] : 'SDG' ( 'v' n= number )? (na= string )? ;
	public final SDGHeader sdg_header() throws RecognitionException {
		SDGHeader header = null;


		int n =0;
		String na =null;

		try {
			// /afs/info.uni-karlsruhe.de/user/bischof/joana/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:322:3: ( 'SDG' ( 'v' n= number )? (na= string )? )
			// /afs/info.uni-karlsruhe.de/user/bischof/joana/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:322:5: 'SDG' ( 'v' n= number )? (na= string )?
			{
			match(input,65,FOLLOW_65_in_sdg_header497); 
			 int version = SDG.DEFAULT_VERSION; 
			// /afs/info.uni-karlsruhe.de/user/bischof/joana/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:324:7: ( 'v' n= number )?
			int alt9=2;
			int LA9_0 = input.LA(1);
			if ( (LA9_0==114) ) {
				alt9=1;
			}
			switch (alt9) {
				case 1 :
					// /afs/info.uni-karlsruhe.de/user/bischof/joana/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:324:8: 'v' n= number
					{
					match(input,114,FOLLOW_114_in_sdg_header513); 
					pushFollow(FOLLOW_number_in_sdg_header517);
					n=number();
					state._fsp--;

					 version = n; 
					}
					break;

			}

			 String name = null; 
			// /afs/info.uni-karlsruhe.de/user/bischof/joana/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:326:7: (na= string )?
			int alt10=2;
			int LA10_0 = input.LA(1);
			if ( (LA10_0==STRING) ) {
				alt10=1;
			}
			switch (alt10) {
				case 1 :
					// /afs/info.uni-karlsruhe.de/user/bischof/joana/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:326:8: na= string
					{
					pushFollow(FOLLOW_string_in_sdg_header538);
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
			// do for sure before leaving
		}
		return header;
	}
	// $ANTLR end "sdg_header"



	// $ANTLR start "node_list"
	// /afs/info.uni-karlsruhe.de/user/bischof/joana/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:330:9: private node_list returns [List<SDGNodeStub> list = new LinkedList<SDGNodeStub>();] : (n= node )* ;
	public final List<SDGNodeStub> node_list() throws RecognitionException {
		List<SDGNodeStub> list =  new LinkedList<SDGNodeStub>();;


		SDGNodeStub n =null;

		try {
			// /afs/info.uni-karlsruhe.de/user/bischof/joana/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:331:3: ( (n= node )* )
			// /afs/info.uni-karlsruhe.de/user/bischof/joana/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:331:5: (n= node )*
			{
			// /afs/info.uni-karlsruhe.de/user/bischof/joana/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:331:5: (n= node )*
			loop11:
			while (true) {
				int alt11=2;
				int LA11_0 = input.LA(1);
				if ( ((LA11_0 >= 12 && LA11_0 <= 13)||LA11_0==16||(LA11_0 >= 28 && LA11_0 <= 30)||LA11_0==35||(LA11_0 >= 39 && LA11_0 <= 40)||LA11_0==53||LA11_0==58||LA11_0==70) ) {
					alt11=1;
				}

				switch (alt11) {
				case 1 :
					// /afs/info.uni-karlsruhe.de/user/bischof/joana/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:331:6: n= node
					{
					pushFollow(FOLLOW_node_in_node_list571);
					n=node();
					state._fsp--;

					 list.add(n); 
					}
					break;

				default :
					break loop11;
				}
			}

			}

		}
		catch (RecognitionException re) {
			reportError(re);
			recover(input,re);
		}
		finally {
			// do for sure before leaving
		}
		return list;
	}
	// $ANTLR end "node_list"



	// $ANTLR start "node"
	// /afs/info.uni-karlsruhe.de/user/bischof/joana/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:334:9: private node returns [SDGNodeStub nstub] : k= node_kind id= mayNegNumber '{' node_attributes[nstub] node_edges[nstub] '}' ;
	public final SDGNodeStub node() throws RecognitionException {
		SDGNodeStub nstub = null;


		SDGNode.Kind k =null;
		int id =0;

		try {
			// /afs/info.uni-karlsruhe.de/user/bischof/joana/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:335:3: (k= node_kind id= mayNegNumber '{' node_attributes[nstub] node_edges[nstub] '}' )
			// /afs/info.uni-karlsruhe.de/user/bischof/joana/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:335:5: k= node_kind id= mayNegNumber '{' node_attributes[nstub] node_edges[nstub] '}'
			{
			pushFollow(FOLLOW_node_kind_in_node597);
			k=node_kind();
			state._fsp--;

			pushFollow(FOLLOW_mayNegNumber_in_node601);
			id=mayNegNumber();
			state._fsp--;

			 nstub = new SDGNodeStub(k, id, defaultSrcPos, defaultBcPos); 
			match(input,115,FOLLOW_115_in_node610); 
			pushFollow(FOLLOW_node_attributes_in_node619);
			node_attributes(nstub);
			state._fsp--;

			pushFollow(FOLLOW_node_edges_in_node629);
			node_edges(nstub);
			state._fsp--;

			match(input,116,FOLLOW_116_in_node636); 
			}

		}
		catch (RecognitionException re) {
			reportError(re);
			recover(input,re);
		}
		finally {
			// do for sure before leaving
		}
		return nstub;
	}
	// $ANTLR end "node"



	// $ANTLR start "node_kind"
	// /afs/info.uni-karlsruhe.de/user/bischof/joana/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:342:9: private node_kind returns [SDGNode.Kind kind] : ( 'NORM' | 'PRED' | 'EXPR' | 'ENTR' | 'CALL' | 'ACTI' | 'ACTO' | 'FRMI' | 'FRMO' | 'EXIT' | 'SYNC' | 'FOLD' );
	public final SDGNode.Kind node_kind() throws RecognitionException {
		SDGNode.Kind kind = null;


		try {
			// /afs/info.uni-karlsruhe.de/user/bischof/joana/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:343:3: ( 'NORM' | 'PRED' | 'EXPR' | 'ENTR' | 'CALL' | 'ACTI' | 'ACTO' | 'FRMI' | 'FRMO' | 'EXIT' | 'SYNC' | 'FOLD' )
			int alt12=12;
			switch ( input.LA(1) ) {
			case 53:
				{
				alt12=1;
				}
				break;
			case 58:
				{
				alt12=2;
				}
				break;
			case 30:
				{
				alt12=3;
				}
				break;
			case 28:
				{
				alt12=4;
				}
				break;
			case 16:
				{
				alt12=5;
				}
				break;
			case 12:
				{
				alt12=6;
				}
				break;
			case 13:
				{
				alt12=7;
				}
				break;
			case 39:
				{
				alt12=8;
				}
				break;
			case 40:
				{
				alt12=9;
				}
				break;
			case 29:
				{
				alt12=10;
				}
				break;
			case 70:
				{
				alt12=11;
				}
				break;
			case 35:
				{
				alt12=12;
				}
				break;
			default:
				NoViableAltException nvae =
					new NoViableAltException("", 12, 0, input);
				throw nvae;
			}
			switch (alt12) {
				case 1 :
					// /afs/info.uni-karlsruhe.de/user/bischof/joana/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:343:5: 'NORM'
					{
					match(input,53,FOLLOW_53_in_node_kind655); 
					 kind = SDGNode.Kind.NORMAL; 
					}
					break;
				case 2 :
					// /afs/info.uni-karlsruhe.de/user/bischof/joana/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:344:5: 'PRED'
					{
					match(input,58,FOLLOW_58_in_node_kind663); 
					 kind = SDGNode.Kind.PREDICATE; 
					}
					break;
				case 3 :
					// /afs/info.uni-karlsruhe.de/user/bischof/joana/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:345:5: 'EXPR'
					{
					match(input,30,FOLLOW_30_in_node_kind671); 
					 kind = SDGNode.Kind.EXPRESSION; 
					}
					break;
				case 4 :
					// /afs/info.uni-karlsruhe.de/user/bischof/joana/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:346:5: 'ENTR'
					{
					match(input,28,FOLLOW_28_in_node_kind679); 
					 kind = SDGNode.Kind.ENTRY; 
					}
					break;
				case 5 :
					// /afs/info.uni-karlsruhe.de/user/bischof/joana/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:347:5: 'CALL'
					{
					match(input,16,FOLLOW_16_in_node_kind687); 
					 kind = SDGNode.Kind.CALL; 
					}
					break;
				case 6 :
					// /afs/info.uni-karlsruhe.de/user/bischof/joana/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:348:5: 'ACTI'
					{
					match(input,12,FOLLOW_12_in_node_kind695); 
					 kind = SDGNode.Kind.ACTUAL_IN; 
					}
					break;
				case 7 :
					// /afs/info.uni-karlsruhe.de/user/bischof/joana/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:349:5: 'ACTO'
					{
					match(input,13,FOLLOW_13_in_node_kind703); 
					 kind = SDGNode.Kind.ACTUAL_OUT; 
					}
					break;
				case 8 :
					// /afs/info.uni-karlsruhe.de/user/bischof/joana/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:350:5: 'FRMI'
					{
					match(input,39,FOLLOW_39_in_node_kind711); 
					 kind = SDGNode.Kind.FORMAL_IN; 
					}
					break;
				case 9 :
					// /afs/info.uni-karlsruhe.de/user/bischof/joana/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:351:5: 'FRMO'
					{
					match(input,40,FOLLOW_40_in_node_kind719); 
					 kind = SDGNode.Kind.FORMAL_OUT; 
					}
					break;
				case 10 :
					// /afs/info.uni-karlsruhe.de/user/bischof/joana/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:352:5: 'EXIT'
					{
					match(input,29,FOLLOW_29_in_node_kind727); 
					 kind = SDGNode.Kind.EXIT; 
					}
					break;
				case 11 :
					// /afs/info.uni-karlsruhe.de/user/bischof/joana/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:353:5: 'SYNC'
					{
					match(input,70,FOLLOW_70_in_node_kind735); 
					 kind = SDGNode.Kind.SYNCHRONIZATION; 
					}
					break;
				case 12 :
					// /afs/info.uni-karlsruhe.de/user/bischof/joana/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:354:5: 'FOLD'
					{
					match(input,35,FOLLOW_35_in_node_kind743); 
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
			// do for sure before leaving
		}
		return kind;
	}
	// $ANTLR end "node_kind"



	// $ANTLR start "node_attributes"
	// /afs/info.uni-karlsruhe.de/user/bischof/joana/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:357:9: private node_attributes[SDGNodeStub node] : ( node_attr[node] ';' )* ;
	public final void node_attributes(SDGNodeStub node) throws RecognitionException {
		try {
			// /afs/info.uni-karlsruhe.de/user/bischof/joana/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:358:3: ( ( node_attr[node] ';' )* )
			// /afs/info.uni-karlsruhe.de/user/bischof/joana/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:358:5: ( node_attr[node] ';' )*
			{
			// /afs/info.uni-karlsruhe.de/user/bischof/joana/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:358:5: ( node_attr[node] ';' )*
			loop13:
			while (true) {
				int alt13=2;
				int LA13_0 = input.LA(1);
				if ( (LA13_0==11||(LA13_0 >= 14 && LA13_0 <= 15)||LA13_0==23||LA13_0==51||(LA13_0 >= 54 && LA13_0 <= 55)||LA13_0==63||LA13_0==71||LA13_0==73||LA13_0==75||LA13_0==77) ) {
					alt13=1;
				}

				switch (alt13) {
				case 1 :
					// /afs/info.uni-karlsruhe.de/user/bischof/joana/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:358:6: node_attr[node] ';'
					{
					pushFollow(FOLLOW_node_attr_in_node_attributes762);
					node_attr(node);
					state._fsp--;

					match(input,10,FOLLOW_10_in_node_attributes765); 
					}
					break;

				default :
					break loop13;
				}
			}

			}

		}
		catch (RecognitionException re) {
			reportError(re);
			recover(input,re);
		}
		finally {
			// do for sure before leaving
		}
	}
	// $ANTLR end "node_attributes"



	// $ANTLR start "node_attr"
	// /afs/info.uni-karlsruhe.de/user/bischof/joana/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:361:9: private node_attr[SDGNodeStub node] : ( 'S' spos= node_source | 'B' bpos= node_bytecode | 'U' number | 'P' procId= number | 'O' op= node_oper | 'V' val= string | 'T' type= string | 'Z' tn= may_neg_num_set | 'N' | 'C' cl= string | 'A' al= pos_num_set | 'D' ds= pos_num_set | 'U' uct= string );
	public final void node_attr(SDGNodeStub node) throws RecognitionException {
		SourcePos spos =null;
		ByteCodePos bpos =null;
		int procId =0;
		SDGNode.Operation op =null;
		String val =null;
		String type =null;
		TIntSet tn =null;
		String cl =null;
		TIntSet al =null;
		TIntSet ds =null;
		String uct =null;

		try {
			// /afs/info.uni-karlsruhe.de/user/bischof/joana/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:362:3: ( 'S' spos= node_source | 'B' bpos= node_bytecode | 'U' number | 'P' procId= number | 'O' op= node_oper | 'V' val= string | 'T' type= string | 'Z' tn= may_neg_num_set | 'N' | 'C' cl= string | 'A' al= pos_num_set | 'D' ds= pos_num_set | 'U' uct= string )
			int alt14=13;
			switch ( input.LA(1) ) {
			case 63:
				{
				alt14=1;
				}
				break;
			case 14:
				{
				alt14=2;
				}
				break;
			case 73:
				{
				int LA14_3 = input.LA(2);
				if ( (LA14_3==NUMBER) ) {
					alt14=3;
				}
				else if ( (LA14_3==STRING) ) {
					alt14=13;
				}

				else {
					int nvaeMark = input.mark();
					try {
						input.consume();
						NoViableAltException nvae =
							new NoViableAltException("", 14, 3, input);
						throw nvae;
					} finally {
						input.rewind(nvaeMark);
					}
				}

				}
				break;
			case 55:
				{
				alt14=4;
				}
				break;
			case 54:
				{
				alt14=5;
				}
				break;
			case 75:
				{
				alt14=6;
				}
				break;
			case 71:
				{
				alt14=7;
				}
				break;
			case 77:
				{
				alt14=8;
				}
				break;
			case 51:
				{
				alt14=9;
				}
				break;
			case 15:
				{
				alt14=10;
				}
				break;
			case 11:
				{
				alt14=11;
				}
				break;
			case 23:
				{
				alt14=12;
				}
				break;
			default:
				NoViableAltException nvae =
					new NoViableAltException("", 14, 0, input);
				throw nvae;
			}
			switch (alt14) {
				case 1 :
					// /afs/info.uni-karlsruhe.de/user/bischof/joana/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:362:5: 'S' spos= node_source
					{
					match(input,63,FOLLOW_63_in_node_attr783); 
					pushFollow(FOLLOW_node_source_in_node_attr787);
					spos=node_source();
					state._fsp--;

					 node.spos = spos; defaultSrcPos = spos; 
					}
					break;
				case 2 :
					// /afs/info.uni-karlsruhe.de/user/bischof/joana/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:363:5: 'B' bpos= node_bytecode
					{
					match(input,14,FOLLOW_14_in_node_attr799); 
					pushFollow(FOLLOW_node_bytecode_in_node_attr803);
					bpos=node_bytecode();
					state._fsp--;

					 node.bpos = bpos; defaultBcPos = bpos; 
					}
					break;
				case 3 :
					// /afs/info.uni-karlsruhe.de/user/bischof/joana/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:364:5: 'U' number
					{
					match(input,73,FOLLOW_73_in_node_attr814); 
					pushFollow(FOLLOW_number_in_node_attr816);
					number();
					state._fsp--;

					}
					break;
				case 4 :
					// /afs/info.uni-karlsruhe.de/user/bischof/joana/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:365:5: 'P' procId= number
					{
					match(input,55,FOLLOW_55_in_node_attr866); 
					pushFollow(FOLLOW_number_in_node_attr870);
					procId=number();
					state._fsp--;

					 node.procId = procId; 
					}
					break;
				case 5 :
					// /afs/info.uni-karlsruhe.de/user/bischof/joana/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:366:5: 'O' op= node_oper
					{
					match(input,54,FOLLOW_54_in_node_attr889); 
					pushFollow(FOLLOW_node_oper_in_node_attr893);
					op=node_oper();
					state._fsp--;

					 node.op = op; 
					}
					break;
				case 6 :
					// /afs/info.uni-karlsruhe.de/user/bischof/joana/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:367:5: 'V' val= string
					{
					match(input,75,FOLLOW_75_in_node_attr921); 
					pushFollow(FOLLOW_string_in_node_attr925);
					val=string();
					state._fsp--;

					 node.val = val; 
					}
					break;
				case 7 :
					// /afs/info.uni-karlsruhe.de/user/bischof/joana/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:368:5: 'T' type= string
					{
					match(input,71,FOLLOW_71_in_node_attr953); 
					pushFollow(FOLLOW_string_in_node_attr957);
					type=string();
					state._fsp--;

					 node.type = type; 
					}
					break;
				case 8 :
					// /afs/info.uni-karlsruhe.de/user/bischof/joana/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:369:5: 'Z' tn= may_neg_num_set
					{
					match(input,77,FOLLOW_77_in_node_attr982); 
					pushFollow(FOLLOW_may_neg_num_set_in_node_attr986);
					tn=may_neg_num_set();
					state._fsp--;

					 node.threadNums = tn; 
					}
					break;
				case 9 :
					// /afs/info.uni-karlsruhe.de/user/bischof/joana/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:370:5: 'N'
					{
					match(input,51,FOLLOW_51_in_node_attr1000); 
					 node.nonTerm = true; 
					}
					break;
				case 10 :
					// /afs/info.uni-karlsruhe.de/user/bischof/joana/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:371:5: 'C' cl= string
					{
					match(input,15,FOLLOW_15_in_node_attr1034); 
					pushFollow(FOLLOW_string_in_node_attr1038);
					cl=string();
					state._fsp--;

					 node.classLoader = cl; 
					}
					break;
				case 11 :
					// /afs/info.uni-karlsruhe.de/user/bischof/joana/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:372:5: 'A' al= pos_num_set
					{
					match(input,11,FOLLOW_11_in_node_attr1060); 
					pushFollow(FOLLOW_pos_num_set_in_node_attr1064);
					al=pos_num_set();
					state._fsp--;

					 node.allocSites = al; 
					}
					break;
				case 12 :
					// /afs/info.uni-karlsruhe.de/user/bischof/joana/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:373:5: 'D' ds= pos_num_set
					{
					match(input,23,FOLLOW_23_in_node_attr1082); 
					pushFollow(FOLLOW_pos_num_set_in_node_attr1086);
					ds=pos_num_set();
					state._fsp--;

					 node.aliasDataSrc = ds; 
					}
					break;
				case 13 :
					// /afs/info.uni-karlsruhe.de/user/bischof/joana/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:374:5: 'U' uct= string
					{
					match(input,73,FOLLOW_73_in_node_attr1103); 
					pushFollow(FOLLOW_string_in_node_attr1107);
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
			// do for sure before leaving
		}
	}
	// $ANTLR end "node_attr"



	// $ANTLR start "pos_num_set"
	// /afs/info.uni-karlsruhe.de/user/bischof/joana/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:377:9: private pos_num_set returns [TIntSet nums = new TIntHashSet();] : n= number ( ',' n2= number )* ;
	public final TIntSet pos_num_set() throws RecognitionException {
		TIntSet nums =  new TIntHashSet();;


		int n =0;
		int n2 =0;

		try {
			// /afs/info.uni-karlsruhe.de/user/bischof/joana/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:378:3: (n= number ( ',' n2= number )* )
			// /afs/info.uni-karlsruhe.de/user/bischof/joana/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:378:5: n= number ( ',' n2= number )*
			{
			pushFollow(FOLLOW_number_in_pos_num_set1140);
			n=number();
			state._fsp--;

			 nums.add(n); 
			// /afs/info.uni-karlsruhe.de/user/bischof/joana/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:378:31: ( ',' n2= number )*
			loop15:
			while (true) {
				int alt15=2;
				int LA15_0 = input.LA(1);
				if ( (LA15_0==7) ) {
					alt15=1;
				}

				switch (alt15) {
				case 1 :
					// /afs/info.uni-karlsruhe.de/user/bischof/joana/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:378:32: ',' n2= number
					{
					match(input,7,FOLLOW_7_in_pos_num_set1145); 
					pushFollow(FOLLOW_number_in_pos_num_set1149);
					n2=number();
					state._fsp--;

					 nums.add(n2); 
					}
					break;

				default :
					break loop15;
				}
			}

			}

		}
		catch (RecognitionException re) {
			reportError(re);
			recover(input,re);
		}
		finally {
			// do for sure before leaving
		}
		return nums;
	}
	// $ANTLR end "pos_num_set"



	// $ANTLR start "may_neg_num_set"
	// /afs/info.uni-karlsruhe.de/user/bischof/joana/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:381:9: private may_neg_num_set returns [TIntSet nums = new TIntHashSet();] : n= mayNegNumber ( ',' n2= mayNegNumber )* ;
	public final TIntSet may_neg_num_set() throws RecognitionException {
		TIntSet nums =  new TIntHashSet();;


		int n =0;
		int n2 =0;

		try {
			// /afs/info.uni-karlsruhe.de/user/bischof/joana/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:382:3: (n= mayNegNumber ( ',' n2= mayNegNumber )* )
			// /afs/info.uni-karlsruhe.de/user/bischof/joana/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:382:5: n= mayNegNumber ( ',' n2= mayNegNumber )*
			{
			pushFollow(FOLLOW_mayNegNumber_in_may_neg_num_set1175);
			n=mayNegNumber();
			state._fsp--;

			 nums.add(n); 
			// /afs/info.uni-karlsruhe.de/user/bischof/joana/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:382:37: ( ',' n2= mayNegNumber )*
			loop16:
			while (true) {
				int alt16=2;
				int LA16_0 = input.LA(1);
				if ( (LA16_0==7) ) {
					alt16=1;
				}

				switch (alt16) {
				case 1 :
					// /afs/info.uni-karlsruhe.de/user/bischof/joana/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:382:38: ',' n2= mayNegNumber
					{
					match(input,7,FOLLOW_7_in_may_neg_num_set1180); 
					pushFollow(FOLLOW_mayNegNumber_in_may_neg_num_set1184);
					n2=mayNegNumber();
					state._fsp--;

					 nums.add(n2); 
					}
					break;

				default :
					break loop16;
				}
			}

			}

		}
		catch (RecognitionException re) {
			reportError(re);
			recover(input,re);
		}
		finally {
			// do for sure before leaving
		}
		return nums;
	}
	// $ANTLR end "may_neg_num_set"



	// $ANTLR start "node_source"
	// /afs/info.uni-karlsruhe.de/user/bischof/joana/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:385:9: private node_source returns [SourcePos spos] : filename= string ':' startRow= number ',' startColumn= number '-' endRow= number ',' endColumn= number ;
	public final SourcePos node_source() throws RecognitionException {
		SourcePos spos = null;


		String filename =null;
		int startRow =0;
		int startColumn =0;
		int endRow =0;
		int endColumn =0;

		try {
			// /afs/info.uni-karlsruhe.de/user/bischof/joana/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:386:3: (filename= string ':' startRow= number ',' startColumn= number '-' endRow= number ',' endColumn= number )
			// /afs/info.uni-karlsruhe.de/user/bischof/joana/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:386:5: filename= string ':' startRow= number ',' startColumn= number '-' endRow= number ',' endColumn= number
			{
			pushFollow(FOLLOW_string_in_node_source1210);
			filename=string();
			state._fsp--;

			match(input,9,FOLLOW_9_in_node_source1212); 
			pushFollow(FOLLOW_number_in_node_source1216);
			startRow=number();
			state._fsp--;

			match(input,7,FOLLOW_7_in_node_source1218); 
			pushFollow(FOLLOW_number_in_node_source1222);
			startColumn=number();
			state._fsp--;

			match(input,8,FOLLOW_8_in_node_source1224); 
			pushFollow(FOLLOW_number_in_node_source1228);
			endRow=number();
			state._fsp--;

			match(input,7,FOLLOW_7_in_node_source1230); 
			pushFollow(FOLLOW_number_in_node_source1234);
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
			// do for sure before leaving
		}
		return spos;
	}
	// $ANTLR end "node_source"



	// $ANTLR start "node_bytecode"
	// /afs/info.uni-karlsruhe.de/user/bischof/joana/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:390:9: private node_bytecode returns [ByteCodePos bpos] : name= string ':' index= mayNegNumber ;
	public final ByteCodePos node_bytecode() throws RecognitionException {
		ByteCodePos bpos = null;


		String name =null;
		int index =0;

		try {
			// /afs/info.uni-karlsruhe.de/user/bischof/joana/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:391:3: (name= string ':' index= mayNegNumber )
			// /afs/info.uni-karlsruhe.de/user/bischof/joana/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:391:5: name= string ':' index= mayNegNumber
			{
			pushFollow(FOLLOW_string_in_node_bytecode1265);
			name=string();
			state._fsp--;

			match(input,9,FOLLOW_9_in_node_bytecode1267); 
			pushFollow(FOLLOW_mayNegNumber_in_node_bytecode1271);
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
			// do for sure before leaving
		}
		return bpos;
	}
	// $ANTLR end "node_bytecode"



	// $ANTLR start "node_oper"
	// /afs/info.uni-karlsruhe.de/user/bischof/joana/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:394:9: private node_oper returns [SDGNode.Operation op] : ( 'empty' | 'intconst' | 'floatconst' | 'charconst' | 'stringconst' | 'functionconst' | 'shortcut' | 'question' | 'binary' | 'unary' | 'derefer' | 'refer' | 'array' | 'select' | 'reference' | 'declaration' | 'modify' | 'modassign' | 'assign' | 'IF' | 'loop' | 'jump' | 'compound' | 'call' | 'entry' | 'exit' | 'form-in' | 'form-ellip' | 'form-out' | 'act-in' | 'act-out' | 'monitor' );
	public final SDGNode.Operation node_oper() throws RecognitionException {
		SDGNode.Operation op = null;


		try {
			// /afs/info.uni-karlsruhe.de/user/bischof/joana/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:395:3: ( 'empty' | 'intconst' | 'floatconst' | 'charconst' | 'stringconst' | 'functionconst' | 'shortcut' | 'question' | 'binary' | 'unary' | 'derefer' | 'refer' | 'array' | 'select' | 'reference' | 'declaration' | 'modify' | 'modassign' | 'assign' | 'IF' | 'loop' | 'jump' | 'compound' | 'call' | 'entry' | 'exit' | 'form-in' | 'form-ellip' | 'form-out' | 'act-in' | 'act-out' | 'monitor' )
			int alt17=32;
			switch ( input.LA(1) ) {
			case 90:
				{
				alt17=1;
				}
				break;
			case 99:
				{
				alt17=2;
				}
				break;
			case 94:
				{
				alt17=3;
				}
				break;
			case 86:
				{
				alt17=4;
				}
				break;
			case 111:
				{
				alt17=5;
				}
				break;
			case 98:
				{
				alt17=6;
				}
				break;
			case 110:
				{
				alt17=7;
				}
				break;
			case 106:
				{
				alt17=8;
				}
				break;
			case 84:
				{
				alt17=9;
				}
				break;
			case 113:
				{
				alt17=10;
				}
				break;
			case 89:
				{
				alt17=11;
				}
				break;
			case 107:
				{
				alt17=12;
				}
				break;
			case 82:
				{
				alt17=13;
				}
				break;
			case 109:
				{
				alt17=14;
				}
				break;
			case 108:
				{
				alt17=15;
				}
				break;
			case 88:
				{
				alt17=16;
				}
				break;
			case 103:
				{
				alt17=17;
				}
				break;
			case 102:
				{
				alt17=18;
				}
				break;
			case 83:
				{
				alt17=19;
				}
				break;
			case 44:
				{
				alt17=20;
				}
				break;
			case 101:
				{
				alt17=21;
				}
				break;
			case 100:
				{
				alt17=22;
				}
				break;
			case 87:
				{
				alt17=23;
				}
				break;
			case 85:
				{
				alt17=24;
				}
				break;
			case 91:
				{
				alt17=25;
				}
				break;
			case 92:
				{
				alt17=26;
				}
				break;
			case 96:
				{
				alt17=27;
				}
				break;
			case 95:
				{
				alt17=28;
				}
				break;
			case 97:
				{
				alt17=29;
				}
				break;
			case 80:
				{
				alt17=30;
				}
				break;
			case 81:
				{
				alt17=31;
				}
				break;
			case 104:
				{
				alt17=32;
				}
				break;
			default:
				NoViableAltException nvae =
					new NoViableAltException("", 17, 0, input);
				throw nvae;
			}
			switch (alt17) {
				case 1 :
					// /afs/info.uni-karlsruhe.de/user/bischof/joana/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:395:5: 'empty'
					{
					match(input,90,FOLLOW_90_in_node_oper1292); 
					 op = SDGNode.Operation.EMPTY; 
					}
					break;
				case 2 :
					// /afs/info.uni-karlsruhe.de/user/bischof/joana/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:396:5: 'intconst'
					{
					match(input,99,FOLLOW_99_in_node_oper1309); 
					 op = SDGNode.Operation.INT_CONST; 
					}
					break;
				case 3 :
					// /afs/info.uni-karlsruhe.de/user/bischof/joana/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:397:5: 'floatconst'
					{
					match(input,94,FOLLOW_94_in_node_oper1323); 
					 op = SDGNode.Operation.FLOAT_CONST; 
					}
					break;
				case 4 :
					// /afs/info.uni-karlsruhe.de/user/bischof/joana/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:398:5: 'charconst'
					{
					match(input,86,FOLLOW_86_in_node_oper1335); 
					 op = SDGNode.Operation.CHAR_CONST; 
					}
					break;
				case 5 :
					// /afs/info.uni-karlsruhe.de/user/bischof/joana/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:399:5: 'stringconst'
					{
					match(input,111,FOLLOW_111_in_node_oper1348); 
					 op = SDGNode.Operation.STRING_CONST; 
					}
					break;
				case 6 :
					// /afs/info.uni-karlsruhe.de/user/bischof/joana/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:400:5: 'functionconst'
					{
					match(input,98,FOLLOW_98_in_node_oper1359); 
					 op = SDGNode.Operation.FUNCTION_CONST; 
					}
					break;
				case 7 :
					// /afs/info.uni-karlsruhe.de/user/bischof/joana/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:401:5: 'shortcut'
					{
					match(input,110,FOLLOW_110_in_node_oper1368); 
					 op = SDGNode.Operation.SHORTCUT; 
					}
					break;
				case 8 :
					// /afs/info.uni-karlsruhe.de/user/bischof/joana/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:402:5: 'question'
					{
					match(input,106,FOLLOW_106_in_node_oper1382); 
					 op = SDGNode.Operation.QUESTION; 
					}
					break;
				case 9 :
					// /afs/info.uni-karlsruhe.de/user/bischof/joana/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:403:5: 'binary'
					{
					match(input,84,FOLLOW_84_in_node_oper1396); 
					 op = SDGNode.Operation.BINARY; 
					}
					break;
				case 10 :
					// /afs/info.uni-karlsruhe.de/user/bischof/joana/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:404:5: 'unary'
					{
					match(input,113,FOLLOW_113_in_node_oper1412); 
					 op = SDGNode.Operation.UNARY; 
					}
					break;
				case 11 :
					// /afs/info.uni-karlsruhe.de/user/bischof/joana/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:405:5: 'derefer'
					{
					match(input,89,FOLLOW_89_in_node_oper1429); 
					 op = SDGNode.Operation.DEREFER; 
					}
					break;
				case 12 :
					// /afs/info.uni-karlsruhe.de/user/bischof/joana/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:406:5: 'refer'
					{
					match(input,107,FOLLOW_107_in_node_oper1444); 
					 op = SDGNode.Operation.REFER; 
					}
					break;
				case 13 :
					// /afs/info.uni-karlsruhe.de/user/bischof/joana/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:407:5: 'array'
					{
					match(input,82,FOLLOW_82_in_node_oper1461); 
					 op = SDGNode.Operation.ARRAY; 
					}
					break;
				case 14 :
					// /afs/info.uni-karlsruhe.de/user/bischof/joana/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:408:5: 'select'
					{
					match(input,109,FOLLOW_109_in_node_oper1478); 
					 op = SDGNode.Operation.SELECT; 
					}
					break;
				case 15 :
					// /afs/info.uni-karlsruhe.de/user/bischof/joana/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:409:5: 'reference'
					{
					match(input,108,FOLLOW_108_in_node_oper1494); 
					 op = SDGNode.Operation.REFERENCE; 
					}
					break;
				case 16 :
					// /afs/info.uni-karlsruhe.de/user/bischof/joana/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:410:5: 'declaration'
					{
					match(input,88,FOLLOW_88_in_node_oper1507); 
					 op = SDGNode.Operation.DECLARATION; 
					}
					break;
				case 17 :
					// /afs/info.uni-karlsruhe.de/user/bischof/joana/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:411:5: 'modify'
					{
					match(input,103,FOLLOW_103_in_node_oper1518); 
					 op = SDGNode.Operation.MODIFY; 
					}
					break;
				case 18 :
					// /afs/info.uni-karlsruhe.de/user/bischof/joana/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:412:5: 'modassign'
					{
					match(input,102,FOLLOW_102_in_node_oper1534); 
					 op = SDGNode.Operation.MODASSIGN; 
					}
					break;
				case 19 :
					// /afs/info.uni-karlsruhe.de/user/bischof/joana/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:413:5: 'assign'
					{
					match(input,83,FOLLOW_83_in_node_oper1547); 
					 op = SDGNode.Operation.ASSIGN; 
					}
					break;
				case 20 :
					// /afs/info.uni-karlsruhe.de/user/bischof/joana/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:414:5: 'IF'
					{
					match(input,44,FOLLOW_44_in_node_oper1563); 
					 op = SDGNode.Operation.IF; 
					}
					break;
				case 21 :
					// /afs/info.uni-karlsruhe.de/user/bischof/joana/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:415:5: 'loop'
					{
					match(input,101,FOLLOW_101_in_node_oper1583); 
					 op = SDGNode.Operation.LOOP; 
					}
					break;
				case 22 :
					// /afs/info.uni-karlsruhe.de/user/bischof/joana/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:416:5: 'jump'
					{
					match(input,100,FOLLOW_100_in_node_oper1601); 
					 op = SDGNode.Operation.JUMP; 
					}
					break;
				case 23 :
					// /afs/info.uni-karlsruhe.de/user/bischof/joana/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:417:5: 'compound'
					{
					match(input,87,FOLLOW_87_in_node_oper1619); 
					 op = SDGNode.Operation.COMPOUND; 
					}
					break;
				case 24 :
					// /afs/info.uni-karlsruhe.de/user/bischof/joana/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:418:5: 'call'
					{
					match(input,85,FOLLOW_85_in_node_oper1633); 
					 op = SDGNode.Operation.CALL; 
					}
					break;
				case 25 :
					// /afs/info.uni-karlsruhe.de/user/bischof/joana/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:419:5: 'entry'
					{
					match(input,91,FOLLOW_91_in_node_oper1651); 
					 op = SDGNode.Operation.ENTRY; 
					}
					break;
				case 26 :
					// /afs/info.uni-karlsruhe.de/user/bischof/joana/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:420:5: 'exit'
					{
					match(input,92,FOLLOW_92_in_node_oper1668); 
					 op = SDGNode.Operation.EXIT; 
					}
					break;
				case 27 :
					// /afs/info.uni-karlsruhe.de/user/bischof/joana/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:421:5: 'form-in'
					{
					match(input,96,FOLLOW_96_in_node_oper1686); 
					 op = SDGNode.Operation.FORMAL_IN; 
					}
					break;
				case 28 :
					// /afs/info.uni-karlsruhe.de/user/bischof/joana/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:422:5: 'form-ellip'
					{
					match(input,95,FOLLOW_95_in_node_oper1701); 
					 op = SDGNode.Operation.FORMAL_ELLIP; 
					}
					break;
				case 29 :
					// /afs/info.uni-karlsruhe.de/user/bischof/joana/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:423:5: 'form-out'
					{
					match(input,97,FOLLOW_97_in_node_oper1713); 
					 op = SDGNode.Operation.FORMAL_OUT; 
					}
					break;
				case 30 :
					// /afs/info.uni-karlsruhe.de/user/bischof/joana/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:424:5: 'act-in'
					{
					match(input,80,FOLLOW_80_in_node_oper1727); 
					 op = SDGNode.Operation.ACTUAL_IN; 
					}
					break;
				case 31 :
					// /afs/info.uni-karlsruhe.de/user/bischof/joana/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:425:5: 'act-out'
					{
					match(input,81,FOLLOW_81_in_node_oper1743); 
					 op = SDGNode.Operation.ACTUAL_OUT; 
					}
					break;
				case 32 :
					// /afs/info.uni-karlsruhe.de/user/bischof/joana/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:426:5: 'monitor'
					{
					match(input,104,FOLLOW_104_in_node_oper1758); 
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
			// do for sure before leaving
		}
		return op;
	}
	// $ANTLR end "node_oper"



	// $ANTLR start "node_edges"
	// /afs/info.uni-karlsruhe.de/user/bischof/joana/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:429:9: private node_edges[SDGNodeStub node] : (e= edge ';' )* ;
	public final void node_edges(SDGNodeStub node) throws RecognitionException {
		SDGEdgeStub e =null;

		try {
			// /afs/info.uni-karlsruhe.de/user/bischof/joana/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:430:3: ( (e= edge ';' )* )
			// /afs/info.uni-karlsruhe.de/user/bischof/joana/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:430:5: (e= edge ';' )*
			{
			// /afs/info.uni-karlsruhe.de/user/bischof/joana/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:430:5: (e= edge ';' )*
			loop18:
			while (true) {
				int alt18=2;
				int LA18_0 = input.LA(1);
				if ( ((LA18_0 >= 17 && LA18_0 <= 21)||(LA18_0 >= 24 && LA18_0 <= 26)||(LA18_0 >= 33 && LA18_0 <= 34)||(LA18_0 >= 36 && LA18_0 <= 38)||(LA18_0 >= 42 && LA18_0 <= 43)||LA18_0==45||(LA18_0 >= 47 && LA18_0 <= 49)||LA18_0==52||(LA18_0 >= 56 && LA18_0 <= 57)||(LA18_0 >= 59 && LA18_0 <= 62)||LA18_0==64||(LA18_0 >= 66 && LA18_0 <= 69)||LA18_0==74||LA18_0==76) ) {
					alt18=1;
				}

				switch (alt18) {
				case 1 :
					// /afs/info.uni-karlsruhe.de/user/bischof/joana/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:430:6: e= edge ';'
					{
					pushFollow(FOLLOW_edge_in_node_edges1786);
					e=edge();
					state._fsp--;

					match(input,10,FOLLOW_10_in_node_edges1788); 
					 node.edges.add(e); 
					}
					break;

				default :
					break loop18;
				}
			}

			}

		}
		catch (RecognitionException re) {
			reportError(re);
			recover(input,re);
		}
		finally {
			// do for sure before leaving
		}
	}
	// $ANTLR end "node_edges"



	// $ANTLR start "edge"
	// /afs/info.uni-karlsruhe.de/user/bischof/joana/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:433:9: private edge returns [SDGEdgeStub estub] : k= edge_kind nr= number ( ':' label= string )? ;
	public final SDGEdgeStub edge() throws RecognitionException {
		SDGEdgeStub estub = null;


		SDGEdge.Kind k =null;
		int nr =0;
		String label =null;

		try {
			// /afs/info.uni-karlsruhe.de/user/bischof/joana/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:434:3: (k= edge_kind nr= number ( ':' label= string )? )
			// /afs/info.uni-karlsruhe.de/user/bischof/joana/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:434:5: k= edge_kind nr= number ( ':' label= string )?
			{
			pushFollow(FOLLOW_edge_kind_in_edge1813);
			k=edge_kind();
			state._fsp--;

			pushFollow(FOLLOW_number_in_edge1817);
			nr=number();
			state._fsp--;

			 estub = new SDGEdgeStub(k, nr); 
			// /afs/info.uni-karlsruhe.de/user/bischof/joana/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:434:63: ( ':' label= string )?
			int alt19=2;
			int LA19_0 = input.LA(1);
			if ( (LA19_0==9) ) {
				alt19=1;
			}
			switch (alt19) {
				case 1 :
					// /afs/info.uni-karlsruhe.de/user/bischof/joana/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:434:64: ':' label= string
					{
					match(input,9,FOLLOW_9_in_edge1822); 
					pushFollow(FOLLOW_string_in_edge1826);
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
			// do for sure before leaving
		}
		return estub;
	}
	// $ANTLR end "edge"



	// $ANTLR start "edge_kind"
	// /afs/info.uni-karlsruhe.de/user/bischof/joana/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:437:9: private edge_kind returns [SDGEdge.Kind kind] : ( 'DD' | 'DH' | 'DA' | 'CD' | 'CE' | 'UN' | 'CF' | 'NF' | 'RF' | 'CC' | 'CL' | 'PI' | 'PO' | 'SU' | 'SH' | 'SF' | 'PS' | 'FORK' | 'FORK_IN' | 'FORK_OUT' | 'JOIN' | 'ID' | 'IW' | 'SD' | 'HE' | 'FD' | 'FI' | 'RY' | 'JF' | 'SP' | 'VD' | 'RD' | 'JD' );
	public final SDGEdge.Kind edge_kind() throws RecognitionException {
		SDGEdge.Kind kind = null;


		try {
			// /afs/info.uni-karlsruhe.de/user/bischof/joana/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:439:3: ( 'DD' | 'DH' | 'DA' | 'CD' | 'CE' | 'UN' | 'CF' | 'NF' | 'RF' | 'CC' | 'CL' | 'PI' | 'PO' | 'SU' | 'SH' | 'SF' | 'PS' | 'FORK' | 'FORK_IN' | 'FORK_OUT' | 'JOIN' | 'ID' | 'IW' | 'SD' | 'HE' | 'FD' | 'FI' | 'RY' | 'JF' | 'SP' | 'VD' | 'RD' | 'JD' )
			int alt20=33;
			switch ( input.LA(1) ) {
			case 25:
				{
				alt20=1;
				}
				break;
			case 26:
				{
				alt20=2;
				}
				break;
			case 24:
				{
				alt20=3;
				}
				break;
			case 18:
				{
				alt20=4;
				}
				break;
			case 19:
				{
				alt20=5;
				}
				break;
			case 74:
				{
				alt20=6;
				}
				break;
			case 20:
				{
				alt20=7;
				}
				break;
			case 52:
				{
				alt20=8;
				}
				break;
			case 61:
				{
				alt20=9;
				}
				break;
			case 17:
				{
				alt20=10;
				}
				break;
			case 21:
				{
				alt20=11;
				}
				break;
			case 56:
				{
				alt20=12;
				}
				break;
			case 57:
				{
				alt20=13;
				}
				break;
			case 69:
				{
				alt20=14;
				}
				break;
			case 67:
				{
				alt20=15;
				}
				break;
			case 66:
				{
				alt20=16;
				}
				break;
			case 59:
				{
				alt20=17;
				}
				break;
			case 36:
				{
				alt20=18;
				}
				break;
			case 37:
				{
				alt20=19;
				}
				break;
			case 38:
				{
				alt20=20;
				}
				break;
			case 49:
				{
				alt20=21;
				}
				break;
			case 43:
				{
				alt20=22;
				}
				break;
			case 45:
				{
				alt20=23;
				}
				break;
			case 64:
				{
				alt20=24;
				}
				break;
			case 42:
				{
				alt20=25;
				}
				break;
			case 33:
				{
				alt20=26;
				}
				break;
			case 34:
				{
				alt20=27;
				}
				break;
			case 62:
				{
				alt20=28;
				}
				break;
			case 48:
				{
				alt20=29;
				}
				break;
			case 68:
				{
				alt20=30;
				}
				break;
			case 76:
				{
				alt20=31;
				}
				break;
			case 60:
				{
				alt20=32;
				}
				break;
			case 47:
				{
				alt20=33;
				}
				break;
			default:
				NoViableAltException nvae =
					new NoViableAltException("", 20, 0, input);
				throw nvae;
			}
			switch (alt20) {
				case 1 :
					// /afs/info.uni-karlsruhe.de/user/bischof/joana/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:439:5: 'DD'
					{
					match(input,25,FOLLOW_25_in_edge_kind1851); 
					 kind = SDGEdge.Kind.DATA_DEP; 
					}
					break;
				case 2 :
					// /afs/info.uni-karlsruhe.de/user/bischof/joana/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:440:5: 'DH'
					{
					match(input,26,FOLLOW_26_in_edge_kind1871); 
					 kind = SDGEdge.Kind.DATA_HEAP; 
					}
					break;
				case 3 :
					// /afs/info.uni-karlsruhe.de/user/bischof/joana/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:441:5: 'DA'
					{
					match(input,24,FOLLOW_24_in_edge_kind1890); 
					 kind = SDGEdge.Kind.DATA_ALIAS; 
					}
					break;
				case 4 :
					// /afs/info.uni-karlsruhe.de/user/bischof/joana/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:443:5: 'CD'
					{
					match(input,18,FOLLOW_18_in_edge_kind1909); 
					 kind = SDGEdge.Kind.CONTROL_DEP_COND; 
					}
					break;
				case 5 :
					// /afs/info.uni-karlsruhe.de/user/bischof/joana/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:444:5: 'CE'
					{
					match(input,19,FOLLOW_19_in_edge_kind1921); 
					 kind = SDGEdge.Kind.CONTROL_DEP_EXPR; 
					}
					break;
				case 6 :
					// /afs/info.uni-karlsruhe.de/user/bischof/joana/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:445:5: 'UN'
					{
					match(input,74,FOLLOW_74_in_edge_kind1933); 
					 kind = SDGEdge.Kind.CONTROL_DEP_UNCOND; 
					}
					break;
				case 7 :
					// /afs/info.uni-karlsruhe.de/user/bischof/joana/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:447:5: 'CF'
					{
					match(input,20,FOLLOW_20_in_edge_kind1944); 
					 kind = SDGEdge.Kind.CONTROL_FLOW; 
					}
					break;
				case 8 :
					// /afs/info.uni-karlsruhe.de/user/bischof/joana/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:448:5: 'NF'
					{
					match(input,52,FOLLOW_52_in_edge_kind1960); 
					 kind = SDGEdge.Kind.NO_FLOW; 
					}
					break;
				case 9 :
					// /afs/info.uni-karlsruhe.de/user/bischof/joana/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:449:5: 'RF'
					{
					match(input,61,FOLLOW_61_in_edge_kind1981); 
					 kind = SDGEdge.Kind.RETURN; 
					}
					break;
				case 10 :
					// /afs/info.uni-karlsruhe.de/user/bischof/joana/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:451:5: 'CC'
					{
					match(input,17,FOLLOW_17_in_edge_kind2004); 
					 kind = SDGEdge.Kind.CONTROL_DEP_CALL; 
					}
					break;
				case 11 :
					// /afs/info.uni-karlsruhe.de/user/bischof/joana/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:452:5: 'CL'
					{
					match(input,21,FOLLOW_21_in_edge_kind2012); 
					 kind = SDGEdge.Kind.CALL; 
					}
					break;
				case 12 :
					// /afs/info.uni-karlsruhe.de/user/bischof/joana/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:453:5: 'PI'
					{
					match(input,56,FOLLOW_56_in_edge_kind2020); 
					 kind = SDGEdge.Kind.PARAMETER_IN; 
					}
					break;
				case 13 :
					// /afs/info.uni-karlsruhe.de/user/bischof/joana/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:454:5: 'PO'
					{
					match(input,57,FOLLOW_57_in_edge_kind2028); 
					 kind = SDGEdge.Kind.PARAMETER_OUT; 
					}
					break;
				case 14 :
					// /afs/info.uni-karlsruhe.de/user/bischof/joana/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:456:5: 'SU'
					{
					match(input,69,FOLLOW_69_in_edge_kind2037); 
					 kind = SDGEdge.Kind.SUMMARY; 
					}
					break;
				case 15 :
					// /afs/info.uni-karlsruhe.de/user/bischof/joana/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:457:5: 'SH'
					{
					match(input,67,FOLLOW_67_in_edge_kind2045); 
					 kind = SDGEdge.Kind.SUMMARY_NO_ALIAS; 
					}
					break;
				case 16 :
					// /afs/info.uni-karlsruhe.de/user/bischof/joana/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:458:5: 'SF'
					{
					match(input,66,FOLLOW_66_in_edge_kind2053); 
					 kind = SDGEdge.Kind.SUMMARY_DATA; 
					}
					break;
				case 17 :
					// /afs/info.uni-karlsruhe.de/user/bischof/joana/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:460:5: 'PS'
					{
					match(input,59,FOLLOW_59_in_edge_kind2062); 
					 kind = SDGEdge.Kind.PARAMETER_STRUCTURE; 
					}
					break;
				case 18 :
					// /afs/info.uni-karlsruhe.de/user/bischof/joana/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:462:5: 'FORK'
					{
					match(input,36,FOLLOW_36_in_edge_kind2071); 
					 kind = SDGEdge.Kind.FORK; 
					}
					break;
				case 19 :
					// /afs/info.uni-karlsruhe.de/user/bischof/joana/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:463:5: 'FORK_IN'
					{
					match(input,37,FOLLOW_37_in_edge_kind2079); 
					 kind = SDGEdge.Kind.FORK_IN; 
					}
					break;
				case 20 :
					// /afs/info.uni-karlsruhe.de/user/bischof/joana/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:464:5: 'FORK_OUT'
					{
					match(input,38,FOLLOW_38_in_edge_kind2087); 
					 kind = SDGEdge.Kind.FORK_OUT; 
					}
					break;
				case 21 :
					// /afs/info.uni-karlsruhe.de/user/bischof/joana/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:465:5: 'JOIN'
					{
					match(input,49,FOLLOW_49_in_edge_kind2095); 
					 kind = SDGEdge.Kind.JOIN; 
					}
					break;
				case 22 :
					// /afs/info.uni-karlsruhe.de/user/bischof/joana/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:466:5: 'ID'
					{
					match(input,43,FOLLOW_43_in_edge_kind2103); 
					 kind = SDGEdge.Kind.INTERFERENCE; 
					}
					break;
				case 23 :
					// /afs/info.uni-karlsruhe.de/user/bischof/joana/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:467:5: 'IW'
					{
					match(input,45,FOLLOW_45_in_edge_kind2111); 
					 kind = SDGEdge.Kind.INTERFERENCE_WRITE; 
					}
					break;
				case 24 :
					// /afs/info.uni-karlsruhe.de/user/bischof/joana/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:468:5: 'SD'
					{
					match(input,64,FOLLOW_64_in_edge_kind2119); 
					 kind = SDGEdge.Kind.SYNCHRONIZATION; 
					}
					break;
				case 25 :
					// /afs/info.uni-karlsruhe.de/user/bischof/joana/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:470:5: 'HE'
					{
					match(input,42,FOLLOW_42_in_edge_kind2128); 
					 kind = SDGEdge.Kind.HELP; 
					}
					break;
				case 26 :
					// /afs/info.uni-karlsruhe.de/user/bischof/joana/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:471:5: 'FD'
					{
					match(input,33,FOLLOW_33_in_edge_kind2136); 
					 kind = SDGEdge.Kind.FOLDED; 
					}
					break;
				case 27 :
					// /afs/info.uni-karlsruhe.de/user/bischof/joana/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:472:5: 'FI'
					{
					match(input,34,FOLLOW_34_in_edge_kind2144); 
					 kind = SDGEdge.Kind.FOLD_INCLUDE; 
					}
					break;
				case 28 :
					// /afs/info.uni-karlsruhe.de/user/bischof/joana/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:474:5: 'RY'
					{
					match(input,62,FOLLOW_62_in_edge_kind2153); 
					 kind = SDGEdge.Kind.READY_DEP; 
					}
					break;
				case 29 :
					// /afs/info.uni-karlsruhe.de/user/bischof/joana/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:475:5: 'JF'
					{
					match(input,48,FOLLOW_48_in_edge_kind2161); 
					 kind = SDGEdge.Kind.JUMP_FLOW; 
					}
					break;
				case 30 :
					// /afs/info.uni-karlsruhe.de/user/bischof/joana/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:476:5: 'SP'
					{
					match(input,68,FOLLOW_68_in_edge_kind2169); 
					 kind = SDGEdge.Kind.SUMMARY; 
					}
					break;
				case 31 :
					// /afs/info.uni-karlsruhe.de/user/bischof/joana/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:477:5: 'VD'
					{
					match(input,76,FOLLOW_76_in_edge_kind2177); 
					 kind = SDGEdge.Kind.DATA_DEP_EXPR_VALUE; 
					}
					break;
				case 32 :
					// /afs/info.uni-karlsruhe.de/user/bischof/joana/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:478:5: 'RD'
					{
					match(input,60,FOLLOW_60_in_edge_kind2185); 
					 kind = SDGEdge.Kind.DATA_DEP_EXPR_REFERENCE; 
					}
					break;
				case 33 :
					// /afs/info.uni-karlsruhe.de/user/bischof/joana/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:479:5: 'JD'
					{
					match(input,47,FOLLOW_47_in_edge_kind2193); 
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
			// do for sure before leaving
		}
		return kind;
	}
	// $ANTLR end "edge_kind"



	// $ANTLR start "mayNegNumber"
	// /afs/info.uni-karlsruhe.de/user/bischof/joana/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:482:9: private mayNegNumber returns [int nr] : ( '-' n= number |n= number );
	public final int mayNegNumber() throws RecognitionException {
		int nr = 0;


		int n =0;

		try {
			// /afs/info.uni-karlsruhe.de/user/bischof/joana/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:483:3: ( '-' n= number |n= number )
			int alt21=2;
			int LA21_0 = input.LA(1);
			if ( (LA21_0==8) ) {
				alt21=1;
			}
			else if ( (LA21_0==NUMBER) ) {
				alt21=2;
			}

			else {
				NoViableAltException nvae =
					new NoViableAltException("", 21, 0, input);
				throw nvae;
			}

			switch (alt21) {
				case 1 :
					// /afs/info.uni-karlsruhe.de/user/bischof/joana/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:483:5: '-' n= number
					{
					match(input,8,FOLLOW_8_in_mayNegNumber2214); 
					pushFollow(FOLLOW_number_in_mayNegNumber2218);
					n=number();
					state._fsp--;

					 nr = -n; 
					}
					break;
				case 2 :
					// /afs/info.uni-karlsruhe.de/user/bischof/joana/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:484:5: n= number
					{
					pushFollow(FOLLOW_number_in_mayNegNumber2228);
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
	// /afs/info.uni-karlsruhe.de/user/bischof/joana/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:487:9: private number returns [int nr] : n= NUMBER ;
	public final int number() throws RecognitionException {
		int nr = 0;


		Token n=null;

		try {
			// /afs/info.uni-karlsruhe.de/user/bischof/joana/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:488:3: (n= NUMBER )
			// /afs/info.uni-karlsruhe.de/user/bischof/joana/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:488:5: n= NUMBER
			{
			n=(Token)match(input,NUMBER,FOLLOW_NUMBER_in_number2251); 
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
	// /afs/info.uni-karlsruhe.de/user/bischof/joana/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:491:9: private string returns [String str] : s= STRING ;
	public final String string() throws RecognitionException {
		String str = null;


		Token s=null;

		try {
			// /afs/info.uni-karlsruhe.de/user/bischof/joana/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:492:3: (s= STRING )
			// /afs/info.uni-karlsruhe.de/user/bischof/joana/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:492:5: s= STRING
			{
			s=(Token)match(input,STRING,FOLLOW_STRING_in_string2274); 
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
	// /afs/info.uni-karlsruhe.de/user/bischof/joana/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:495:9: private bool returns [boolean b] : ( 'true' | 'false' );
	public final boolean bool() throws RecognitionException {
		boolean b = false;


		try {
			// /afs/info.uni-karlsruhe.de/user/bischof/joana/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:496:3: ( 'true' | 'false' )
			int alt22=2;
			int LA22_0 = input.LA(1);
			if ( (LA22_0==112) ) {
				alt22=1;
			}
			else if ( (LA22_0==93) ) {
				alt22=2;
			}

			else {
				NoViableAltException nvae =
					new NoViableAltException("", 22, 0, input);
				throw nvae;
			}

			switch (alt22) {
				case 1 :
					// /afs/info.uni-karlsruhe.de/user/bischof/joana/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:496:5: 'true'
					{
					match(input,112,FOLLOW_112_in_bool2295); 
					 b = true; 
					}
					break;
				case 2 :
					// /afs/info.uni-karlsruhe.de/user/bischof/joana/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:497:5: 'false'
					{
					match(input,93,FOLLOW_93_in_bool2304); 
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



	public static final BitSet FOLLOW_sdg_header_in_sdg_file73 = new BitSet(new long[]{0x0000000000000000L,0x0008000000000000L});
	public static final BitSet FOLLOW_115_in_sdg_file89 = new BitSet(new long[]{0x0420418870013000L,0x0010000000000140L});
	public static final BitSet FOLLOW_46_in_sdg_file99 = new BitSet(new long[]{0x0420018870013000L,0x0010000000000140L});
	public static final BitSet FOLLOW_node_list_in_sdg_file128 = new BitSet(new long[]{0x0000000000000000L,0x0010000000000100L});
	public static final BitSet FOLLOW_thread_info_in_sdg_file150 = new BitSet(new long[]{0x0000000000000000L,0x0010000000000000L});
	public static final BitSet FOLLOW_116_in_sdg_file163 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_thread_in_thread_info192 = new BitSet(new long[]{0x0000000000000002L,0x0000000000000100L});
	public static final BitSet FOLLOW_72_in_thread224 = new BitSet(new long[]{0x0000000000000010L});
	public static final BitSet FOLLOW_number_in_thread228 = new BitSet(new long[]{0x0000000000000000L,0x0008000000000000L});
	public static final BitSet FOLLOW_115_in_thread230 = new BitSet(new long[]{0x0000000080000000L});
	public static final BitSet FOLLOW_31_in_thread238 = new BitSet(new long[]{0x0000000000000010L});
	public static final BitSet FOLLOW_number_in_thread244 = new BitSet(new long[]{0x0000000000000400L});
	public static final BitSet FOLLOW_10_in_thread246 = new BitSet(new long[]{0x0000000100000000L});
	public static final BitSet FOLLOW_32_in_thread254 = new BitSet(new long[]{0x0000000000000010L});
	public static final BitSet FOLLOW_number_in_thread261 = new BitSet(new long[]{0x0000000000000400L});
	public static final BitSet FOLLOW_10_in_thread263 = new BitSet(new long[]{0x0000020000000000L});
	public static final BitSet FOLLOW_41_in_thread271 = new BitSet(new long[]{0x0000000000000010L});
	public static final BitSet FOLLOW_number_in_thread278 = new BitSet(new long[]{0x0000000000000400L});
	public static final BitSet FOLLOW_10_in_thread280 = new BitSet(new long[]{0x0004000000000000L});
	public static final BitSet FOLLOW_50_in_thread288 = new BitSet(new long[]{0x0000000000000010L,0x0000020000004000L});
	public static final BitSet FOLLOW_listOrSingleNumber_in_thread295 = new BitSet(new long[]{0x0000000000000400L});
	public static final BitSet FOLLOW_10_in_thread298 = new BitSet(new long[]{0x0000000000400000L});
	public static final BitSet FOLLOW_22_in_thread306 = new BitSet(new long[]{0x0000000000000000L,0x0000020000004000L});
	public static final BitSet FOLLOW_context_in_thread310 = new BitSet(new long[]{0x0000000000000400L});
	public static final BitSet FOLLOW_10_in_thread313 = new BitSet(new long[]{0x0000000008000000L});
	public static final BitSet FOLLOW_27_in_thread321 = new BitSet(new long[]{0x0000000000000000L,0x0001000020000000L});
	public static final BitSet FOLLOW_bool_in_thread325 = new BitSet(new long[]{0x0000000000000400L});
	public static final BitSet FOLLOW_10_in_thread327 = new BitSet(new long[]{0x0000000000000000L,0x0010000000000000L});
	public static final BitSet FOLLOW_116_in_thread333 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_mayEmptyNumberList_in_listOrSingleNumber363 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_number_in_listOrSingleNumber374 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_105_in_mayEmptyNumberList396 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_78_in_mayEmptyNumberList402 = new BitSet(new long[]{0x0000000000000000L,0x0000000000008000L});
	public static final BitSet FOLLOW_79_in_mayEmptyNumberList404 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_78_in_mayEmptyNumberList410 = new BitSet(new long[]{0x0000000000000010L});
	public static final BitSet FOLLOW_number_in_mayEmptyNumberList414 = new BitSet(new long[]{0x0000000000000080L,0x0000000000008000L});
	public static final BitSet FOLLOW_7_in_mayEmptyNumberList419 = new BitSet(new long[]{0x0000000000000010L});
	public static final BitSet FOLLOW_number_in_mayEmptyNumberList423 = new BitSet(new long[]{0x0000000000000080L,0x0000000000008000L});
	public static final BitSet FOLLOW_79_in_mayEmptyNumberList430 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_105_in_context452 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_78_in_context458 = new BitSet(new long[]{0x0000000000000110L});
	public static final BitSet FOLLOW_mayNegNumber_in_context462 = new BitSet(new long[]{0x0000000000000080L,0x0000000000008000L});
	public static final BitSet FOLLOW_7_in_context467 = new BitSet(new long[]{0x0000000000000110L});
	public static final BitSet FOLLOW_mayNegNumber_in_context471 = new BitSet(new long[]{0x0000000000000080L,0x0000000000008000L});
	public static final BitSet FOLLOW_79_in_context478 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_65_in_sdg_header497 = new BitSet(new long[]{0x0000000000000022L,0x0004000000000000L});
	public static final BitSet FOLLOW_114_in_sdg_header513 = new BitSet(new long[]{0x0000000000000010L});
	public static final BitSet FOLLOW_number_in_sdg_header517 = new BitSet(new long[]{0x0000000000000022L});
	public static final BitSet FOLLOW_string_in_sdg_header538 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_node_in_node_list571 = new BitSet(new long[]{0x0420018870013002L,0x0000000000000040L});
	public static final BitSet FOLLOW_node_kind_in_node597 = new BitSet(new long[]{0x0000000000000110L});
	public static final BitSet FOLLOW_mayNegNumber_in_node601 = new BitSet(new long[]{0x0000000000000000L,0x0008000000000000L});
	public static final BitSet FOLLOW_115_in_node610 = new BitSet(new long[]{0xFBDBAC7607BEC800L,0x0010000000003EBDL});
	public static final BitSet FOLLOW_node_attributes_in_node619 = new BitSet(new long[]{0x7B13AC76073E0000L,0x001000000000143DL});
	public static final BitSet FOLLOW_node_edges_in_node629 = new BitSet(new long[]{0x0000000000000000L,0x0010000000000000L});
	public static final BitSet FOLLOW_116_in_node636 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_53_in_node_kind655 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_58_in_node_kind663 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_30_in_node_kind671 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_28_in_node_kind679 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_16_in_node_kind687 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_12_in_node_kind695 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_13_in_node_kind703 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_39_in_node_kind711 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_40_in_node_kind719 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_29_in_node_kind727 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_70_in_node_kind735 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_35_in_node_kind743 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_node_attr_in_node_attributes762 = new BitSet(new long[]{0x0000000000000400L});
	public static final BitSet FOLLOW_10_in_node_attributes765 = new BitSet(new long[]{0x80C800000080C802L,0x0000000000002A80L});
	public static final BitSet FOLLOW_63_in_node_attr783 = new BitSet(new long[]{0x0000000000000020L});
	public static final BitSet FOLLOW_node_source_in_node_attr787 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_14_in_node_attr799 = new BitSet(new long[]{0x0000000000000020L});
	public static final BitSet FOLLOW_node_bytecode_in_node_attr803 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_73_in_node_attr814 = new BitSet(new long[]{0x0000000000000010L});
	public static final BitSet FOLLOW_number_in_node_attr816 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_55_in_node_attr866 = new BitSet(new long[]{0x0000000000000010L});
	public static final BitSet FOLLOW_number_in_node_attr870 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_54_in_node_attr889 = new BitSet(new long[]{0x0000100000000000L,0x0002FDFFDFFF0000L});
	public static final BitSet FOLLOW_node_oper_in_node_attr893 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_75_in_node_attr921 = new BitSet(new long[]{0x0000000000000020L});
	public static final BitSet FOLLOW_string_in_node_attr925 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_71_in_node_attr953 = new BitSet(new long[]{0x0000000000000020L});
	public static final BitSet FOLLOW_string_in_node_attr957 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_77_in_node_attr982 = new BitSet(new long[]{0x0000000000000110L});
	public static final BitSet FOLLOW_may_neg_num_set_in_node_attr986 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_51_in_node_attr1000 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_15_in_node_attr1034 = new BitSet(new long[]{0x0000000000000020L});
	public static final BitSet FOLLOW_string_in_node_attr1038 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_11_in_node_attr1060 = new BitSet(new long[]{0x0000000000000010L});
	public static final BitSet FOLLOW_pos_num_set_in_node_attr1064 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_23_in_node_attr1082 = new BitSet(new long[]{0x0000000000000010L});
	public static final BitSet FOLLOW_pos_num_set_in_node_attr1086 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_73_in_node_attr1103 = new BitSet(new long[]{0x0000000000000020L});
	public static final BitSet FOLLOW_string_in_node_attr1107 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_number_in_pos_num_set1140 = new BitSet(new long[]{0x0000000000000082L});
	public static final BitSet FOLLOW_7_in_pos_num_set1145 = new BitSet(new long[]{0x0000000000000010L});
	public static final BitSet FOLLOW_number_in_pos_num_set1149 = new BitSet(new long[]{0x0000000000000082L});
	public static final BitSet FOLLOW_mayNegNumber_in_may_neg_num_set1175 = new BitSet(new long[]{0x0000000000000082L});
	public static final BitSet FOLLOW_7_in_may_neg_num_set1180 = new BitSet(new long[]{0x0000000000000110L});
	public static final BitSet FOLLOW_mayNegNumber_in_may_neg_num_set1184 = new BitSet(new long[]{0x0000000000000082L});
	public static final BitSet FOLLOW_string_in_node_source1210 = new BitSet(new long[]{0x0000000000000200L});
	public static final BitSet FOLLOW_9_in_node_source1212 = new BitSet(new long[]{0x0000000000000010L});
	public static final BitSet FOLLOW_number_in_node_source1216 = new BitSet(new long[]{0x0000000000000080L});
	public static final BitSet FOLLOW_7_in_node_source1218 = new BitSet(new long[]{0x0000000000000010L});
	public static final BitSet FOLLOW_number_in_node_source1222 = new BitSet(new long[]{0x0000000000000100L});
	public static final BitSet FOLLOW_8_in_node_source1224 = new BitSet(new long[]{0x0000000000000010L});
	public static final BitSet FOLLOW_number_in_node_source1228 = new BitSet(new long[]{0x0000000000000080L});
	public static final BitSet FOLLOW_7_in_node_source1230 = new BitSet(new long[]{0x0000000000000010L});
	public static final BitSet FOLLOW_number_in_node_source1234 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_string_in_node_bytecode1265 = new BitSet(new long[]{0x0000000000000200L});
	public static final BitSet FOLLOW_9_in_node_bytecode1267 = new BitSet(new long[]{0x0000000000000110L});
	public static final BitSet FOLLOW_mayNegNumber_in_node_bytecode1271 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_90_in_node_oper1292 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_99_in_node_oper1309 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_94_in_node_oper1323 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_86_in_node_oper1335 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_111_in_node_oper1348 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_98_in_node_oper1359 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_110_in_node_oper1368 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_106_in_node_oper1382 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_84_in_node_oper1396 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_113_in_node_oper1412 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_89_in_node_oper1429 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_107_in_node_oper1444 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_82_in_node_oper1461 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_109_in_node_oper1478 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_108_in_node_oper1494 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_88_in_node_oper1507 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_103_in_node_oper1518 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_102_in_node_oper1534 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_83_in_node_oper1547 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_44_in_node_oper1563 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_101_in_node_oper1583 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_100_in_node_oper1601 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_87_in_node_oper1619 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_85_in_node_oper1633 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_91_in_node_oper1651 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_92_in_node_oper1668 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_96_in_node_oper1686 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_95_in_node_oper1701 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_97_in_node_oper1713 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_80_in_node_oper1727 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_81_in_node_oper1743 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_104_in_node_oper1758 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_edge_in_node_edges1786 = new BitSet(new long[]{0x0000000000000400L});
	public static final BitSet FOLLOW_10_in_node_edges1788 = new BitSet(new long[]{0x7B13AC76073E0002L,0x000000000000143DL});
	public static final BitSet FOLLOW_edge_kind_in_edge1813 = new BitSet(new long[]{0x0000000000000010L});
	public static final BitSet FOLLOW_number_in_edge1817 = new BitSet(new long[]{0x0000000000000202L});
	public static final BitSet FOLLOW_9_in_edge1822 = new BitSet(new long[]{0x0000000000000020L});
	public static final BitSet FOLLOW_string_in_edge1826 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_25_in_edge_kind1851 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_26_in_edge_kind1871 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_24_in_edge_kind1890 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_18_in_edge_kind1909 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_19_in_edge_kind1921 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_74_in_edge_kind1933 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_20_in_edge_kind1944 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_52_in_edge_kind1960 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_61_in_edge_kind1981 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_17_in_edge_kind2004 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_21_in_edge_kind2012 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_56_in_edge_kind2020 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_57_in_edge_kind2028 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_69_in_edge_kind2037 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_67_in_edge_kind2045 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_66_in_edge_kind2053 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_59_in_edge_kind2062 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_36_in_edge_kind2071 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_37_in_edge_kind2079 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_38_in_edge_kind2087 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_49_in_edge_kind2095 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_43_in_edge_kind2103 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_45_in_edge_kind2111 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_64_in_edge_kind2119 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_42_in_edge_kind2128 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_33_in_edge_kind2136 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_34_in_edge_kind2144 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_62_in_edge_kind2153 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_48_in_edge_kind2161 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_68_in_edge_kind2169 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_76_in_edge_kind2177 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_60_in_edge_kind2185 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_47_in_edge_kind2193 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_8_in_mayNegNumber2214 = new BitSet(new long[]{0x0000000000000010L});
	public static final BitSet FOLLOW_number_in_mayNegNumber2218 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_number_in_mayNegNumber2228 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_NUMBER_in_number2251 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_STRING_in_string2274 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_112_in_bool2295 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_93_in_bool2304 = new BitSet(new long[]{0x0000000000000002L});
}
