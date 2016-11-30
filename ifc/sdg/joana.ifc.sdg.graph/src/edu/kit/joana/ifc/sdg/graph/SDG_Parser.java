// $ANTLR 3.5.2 /data1/hecker/gits/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g 2016-11-30 16:03:48
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
public class SDG_Parser extends Parser {
	public static final String[] tokenNames = new String[] {
		"<invalid>", "<EOR>", "<DOWN>", "<UP>", "NUMBER", "STRING", "WHITESPACE", 
		"','", "'-'", "':'", "';'", "'A'", "'ACTI'", "'ACTO'", "'B'", "'C'", "'CALL'", 
		"'CC'", "'CD'", "'CE'", "'CF'", "'CL'", "'Context'", "'D'", "'DA'", "'DD'", 
		"'DH'", "'Dynamic'", "'ENTR'", "'EXIT'", "'EXPR'", "'Entry'", "'Exit'", 
		"'FD'", "'FI'", "'FOLD'", "'FORK'", "'FORK_IN'", "'FORK_OUT'", "'FRMI'", 
		"'FRMO'", "'Fork'", "'HE'", "'ID'", "'IF'", "'IW'", "'JComp'", "'JD'", 
		"'JF'", "'JOIN'", "'Join'", "'LD'", "'LU'", "'N'", "'NF'", "'NORM'", "'O'", 
		"'P'", "'PI'", "'PO'", "'PRED'", "'PS'", "'RD'", "'RF'", "'RY'", "'S'", 
		"'SD'", "'SDG'", "'SF'", "'SH'", "'SP'", "'SU'", "'SYNC'", "'T'", "'Thread'", 
		"'U'", "'UN'", "'V'", "'VD'", "'Z'", "'['", "']'", "'act-in'", "'act-out'", 
		"'array'", "'assign'", "'binary'", "'call'", "'charconst'", "'compound'", 
		"'declaration'", "'derefer'", "'empty'", "'entry'", "'exit'", "'false'", 
		"'floatconst'", "'form-ellip'", "'form-in'", "'form-out'", "'functionconst'", 
		"'intconst'", "'jump'", "'loop'", "'modassign'", "'modify'", "'monitor'", 
		"'null'", "'question'", "'refer'", "'reference'", "'select'", "'shortcut'", 
		"'stringconst'", "'true'", "'unary'", "'v'", "'{'", "'}'"
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
	public static final int T__117=117;
	public static final int T__118=118;
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
	@Override public String getGrammarFileName() { return "/data1/hecker/gits/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g"; }


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
	    private List<String> localDefNames;
	    private List<String> localUseNames;
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
	      
	      if (localDefNames != null) {
	        n.setLocalDefNames(localDefNames.toArray(new String[localDefNames.size()]));
	      }
	      
	      if (localUseNames != null) {
	        n.setLocalUseNames(localUseNames.toArray(new String[localUseNames.size()]));
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
	// /data1/hecker/gits/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:281:1: sdg_file returns [SDG sdg] : head= sdg_header '{' ( 'JComp' )? nl= node_list (ti= thread_info[sdg] )? '}' ;
	public final SDG sdg_file() throws RecognitionException {
		SDG sdg = null;


		SDGHeader head =null;
		List<SDGNodeStub> nl =null;
		ThreadsInformation ti =null;

		try {
			// /data1/hecker/gits/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:282:3: (head= sdg_header '{' ( 'JComp' )? nl= node_list (ti= thread_info[sdg] )? '}' )
			// /data1/hecker/gits/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:282:5: head= sdg_header '{' ( 'JComp' )? nl= node_list (ti= thread_info[sdg] )? '}'
			{
			pushFollow(FOLLOW_sdg_header_in_sdg_file73);
			head=sdg_header();
			state._fsp--;

			 sdg = head.createSDG(); 
			match(input,117,FOLLOW_117_in_sdg_file89); 
			// /data1/hecker/gits/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:284:7: ( 'JComp' )?
			int alt1=2;
			int LA1_0 = input.LA(1);
			if ( (LA1_0==46) ) {
				alt1=1;
			}
			switch (alt1) {
				case 1 :
					// /data1/hecker/gits/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:284:8: 'JComp'
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
			// /data1/hecker/gits/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:286:7: (ti= thread_info[sdg] )?
			int alt2=2;
			int LA2_0 = input.LA(1);
			if ( (LA2_0==74) ) {
				alt2=1;
			}
			switch (alt2) {
				case 1 :
					// /data1/hecker/gits/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:286:8: ti= thread_info[sdg]
					{
					pushFollow(FOLLOW_thread_info_in_sdg_file150);
					ti=thread_info(sdg);
					state._fsp--;

					 sdg.setThreadsInfo(ti); 
					}
					break;

			}

			match(input,118,FOLLOW_118_in_sdg_file163); 
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
	// /data1/hecker/gits/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:290:9: private thread_info[SDG sdg] returns [ThreadsInformation tinfo] : (t= thread[sdg] )+ ;
	public final ThreadsInformation thread_info(SDG sdg) throws RecognitionException {
		ThreadsInformation tinfo = null;


		ThreadInstance t =null;

		try {
			// /data1/hecker/gits/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:291:3: ( (t= thread[sdg] )+ )
			// /data1/hecker/gits/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:291:5: (t= thread[sdg] )+
			{
			 final LinkedList<ThreadInstance> tis = new LinkedList<ThreadInstance>(); 
			// /data1/hecker/gits/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:292:5: (t= thread[sdg] )+
			int cnt3=0;
			loop3:
			while (true) {
				int alt3=2;
				int LA3_0 = input.LA(1);
				if ( (LA3_0==74) ) {
					alt3=1;
				}

				switch (alt3) {
				case 1 :
					// /data1/hecker/gits/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:292:6: t= thread[sdg]
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
	// /data1/hecker/gits/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:296:9: private thread[SDG sdg] returns [ThreadInstance ti] : 'Thread' id= number '{' 'Entry' en= number ';' 'Exit' ex= number ';' 'Fork' fo= number ';' 'Join' joins= listOrSingleNumber[sdg] ';' 'Context' con= context[sdg] ';' 'Dynamic' dyn= bool ';' '}' ;
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
			// /data1/hecker/gits/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:297:3: ( 'Thread' id= number '{' 'Entry' en= number ';' 'Exit' ex= number ';' 'Fork' fo= number ';' 'Join' joins= listOrSingleNumber[sdg] ';' 'Context' con= context[sdg] ';' 'Dynamic' dyn= bool ';' '}' )
			// /data1/hecker/gits/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:297:5: 'Thread' id= number '{' 'Entry' en= number ';' 'Exit' ex= number ';' 'Fork' fo= number ';' 'Join' joins= listOrSingleNumber[sdg] ';' 'Context' con= context[sdg] ';' 'Dynamic' dyn= bool ';' '}'
			{
			match(input,74,FOLLOW_74_in_thread224); 
			pushFollow(FOLLOW_number_in_thread228);
			id=number();
			state._fsp--;

			match(input,117,FOLLOW_117_in_thread230); 
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
			match(input,118,FOLLOW_118_in_thread333); 

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
	// /data1/hecker/gits/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:313:9: private listOrSingleNumber[SDG sdg] returns [LinkedList<SDGNode> js] : (joins= mayEmptyNumberList[sdg] |jo= number );
	public final LinkedList<SDGNode> listOrSingleNumber(SDG sdg) throws RecognitionException {
		LinkedList<SDGNode> js = null;


		LinkedList<SDGNode> joins =null;
		int jo =0;

		try {
			// /data1/hecker/gits/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:314:3: (joins= mayEmptyNumberList[sdg] |jo= number )
			int alt4=2;
			int LA4_0 = input.LA(1);
			if ( (LA4_0==80||LA4_0==107) ) {
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
					// /data1/hecker/gits/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:314:5: joins= mayEmptyNumberList[sdg]
					{
					pushFollow(FOLLOW_mayEmptyNumberList_in_listOrSingleNumber363);
					joins=mayEmptyNumberList(sdg);
					state._fsp--;

					 js = joins; 
					}
					break;
				case 2 :
					// /data1/hecker/gits/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:315:5: jo= number
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
	// /data1/hecker/gits/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:321:9: private mayEmptyNumberList[SDG sdg] returns [LinkedList<SDGNode> js = new LinkedList<SDGNode>();] : ( 'null' | '[' ']' | '[' i= number ( ',' i= number )* ']' );
	public final LinkedList<SDGNode> mayEmptyNumberList(SDG sdg) throws RecognitionException {
		LinkedList<SDGNode> js =  new LinkedList<SDGNode>();;


		int i =0;

		try {
			// /data1/hecker/gits/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:322:3: ( 'null' | '[' ']' | '[' i= number ( ',' i= number )* ']' )
			int alt6=3;
			int LA6_0 = input.LA(1);
			if ( (LA6_0==107) ) {
				alt6=1;
			}
			else if ( (LA6_0==80) ) {
				int LA6_2 = input.LA(2);
				if ( (LA6_2==81) ) {
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
					// /data1/hecker/gits/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:322:5: 'null'
					{
					match(input,107,FOLLOW_107_in_mayEmptyNumberList396); 
					}
					break;
				case 2 :
					// /data1/hecker/gits/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:323:5: '[' ']'
					{
					match(input,80,FOLLOW_80_in_mayEmptyNumberList402); 
					match(input,81,FOLLOW_81_in_mayEmptyNumberList404); 
					}
					break;
				case 3 :
					// /data1/hecker/gits/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:324:5: '[' i= number ( ',' i= number )* ']'
					{
					match(input,80,FOLLOW_80_in_mayEmptyNumberList410); 
					pushFollow(FOLLOW_number_in_mayEmptyNumberList414);
					i=number();
					state._fsp--;

					 js.add(sdg.getNode(i)); 
					// /data1/hecker/gits/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:324:46: ( ',' i= number )*
					loop5:
					while (true) {
						int alt5=2;
						int LA5_0 = input.LA(1);
						if ( (LA5_0==7) ) {
							alt5=1;
						}

						switch (alt5) {
						case 1 :
							// /data1/hecker/gits/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:324:47: ',' i= number
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

					match(input,81,FOLLOW_81_in_mayEmptyNumberList430); 
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



	// $ANTLR start "mayEmptyStringList"
	// /data1/hecker/gits/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:327:9: private mayEmptyStringList returns [LinkedList<String> ss = new LinkedList<String>();] : ( 'null' | '[' ']' | '[' s= string ( ',' s= string )* ']' );
	public final LinkedList<String> mayEmptyStringList() throws RecognitionException {
		LinkedList<String> ss =  new LinkedList<String>();;


		String s =null;

		try {
			// /data1/hecker/gits/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:328:3: ( 'null' | '[' ']' | '[' s= string ( ',' s= string )* ']' )
			int alt8=3;
			int LA8_0 = input.LA(1);
			if ( (LA8_0==107) ) {
				alt8=1;
			}
			else if ( (LA8_0==80) ) {
				int LA8_2 = input.LA(2);
				if ( (LA8_2==81) ) {
					alt8=2;
				}
				else if ( (LA8_2==STRING) ) {
					alt8=3;
				}

				else {
					int nvaeMark = input.mark();
					try {
						input.consume();
						NoViableAltException nvae =
							new NoViableAltException("", 8, 2, input);
						throw nvae;
					} finally {
						input.rewind(nvaeMark);
					}
				}

			}

			else {
				NoViableAltException nvae =
					new NoViableAltException("", 8, 0, input);
				throw nvae;
			}

			switch (alt8) {
				case 1 :
					// /data1/hecker/gits/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:328:5: 'null'
					{
					match(input,107,FOLLOW_107_in_mayEmptyStringList451); 
					}
					break;
				case 2 :
					// /data1/hecker/gits/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:329:5: '[' ']'
					{
					match(input,80,FOLLOW_80_in_mayEmptyStringList457); 
					match(input,81,FOLLOW_81_in_mayEmptyStringList459); 
					}
					break;
				case 3 :
					// /data1/hecker/gits/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:330:5: '[' s= string ( ',' s= string )* ']'
					{
					match(input,80,FOLLOW_80_in_mayEmptyStringList465); 
					pushFollow(FOLLOW_string_in_mayEmptyStringList469);
					s=string();
					state._fsp--;

					 ss.add(s); 
					// /data1/hecker/gits/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:330:33: ( ',' s= string )*
					loop7:
					while (true) {
						int alt7=2;
						int LA7_0 = input.LA(1);
						if ( (LA7_0==7) ) {
							alt7=1;
						}

						switch (alt7) {
						case 1 :
							// /data1/hecker/gits/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:330:34: ',' s= string
							{
							match(input,7,FOLLOW_7_in_mayEmptyStringList474); 
							pushFollow(FOLLOW_string_in_mayEmptyStringList478);
							s=string();
							state._fsp--;

							 ss.add(s); 
							}
							break;

						default :
							break loop7;
						}
					}

					match(input,81,FOLLOW_81_in_mayEmptyStringList485); 
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
		return ss;
	}
	// $ANTLR end "mayEmptyStringList"



	// $ANTLR start "context"
	// /data1/hecker/gits/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:333:9: private context[SDG sdg] returns [LinkedList<SDGNode> cx = new LinkedList<SDGNode>();] : ( 'null' | '[' i= mayNegNumber ( ',' i= mayNegNumber )* ']' );
	public final LinkedList<SDGNode> context(SDG sdg) throws RecognitionException {
		LinkedList<SDGNode> cx =  new LinkedList<SDGNode>();;


		int i =0;

		try {
			// /data1/hecker/gits/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:334:3: ( 'null' | '[' i= mayNegNumber ( ',' i= mayNegNumber )* ']' )
			int alt10=2;
			int LA10_0 = input.LA(1);
			if ( (LA10_0==107) ) {
				alt10=1;
			}
			else if ( (LA10_0==80) ) {
				alt10=2;
			}

			else {
				NoViableAltException nvae =
					new NoViableAltException("", 10, 0, input);
				throw nvae;
			}

			switch (alt10) {
				case 1 :
					// /data1/hecker/gits/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:334:5: 'null'
					{
					match(input,107,FOLLOW_107_in_context507); 
					}
					break;
				case 2 :
					// /data1/hecker/gits/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:335:5: '[' i= mayNegNumber ( ',' i= mayNegNumber )* ']'
					{
					match(input,80,FOLLOW_80_in_context513); 
					pushFollow(FOLLOW_mayNegNumber_in_context517);
					i=mayNegNumber();
					state._fsp--;

					 cx.add(sdg.getNode(i)); 
					// /data1/hecker/gits/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:335:52: ( ',' i= mayNegNumber )*
					loop9:
					while (true) {
						int alt9=2;
						int LA9_0 = input.LA(1);
						if ( (LA9_0==7) ) {
							alt9=1;
						}

						switch (alt9) {
						case 1 :
							// /data1/hecker/gits/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:335:53: ',' i= mayNegNumber
							{
							match(input,7,FOLLOW_7_in_context522); 
							pushFollow(FOLLOW_mayNegNumber_in_context526);
							i=mayNegNumber();
							state._fsp--;

							 cx.add(sdg.getNode(i)); 
							}
							break;

						default :
							break loop9;
						}
					}

					match(input,81,FOLLOW_81_in_context533); 
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
	// /data1/hecker/gits/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:338:9: private sdg_header returns [SDGHeader header] : 'SDG' ( 'v' n= number )? (na= string )? ;
	public final SDGHeader sdg_header() throws RecognitionException {
		SDGHeader header = null;


		int n =0;
		String na =null;

		try {
			// /data1/hecker/gits/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:339:3: ( 'SDG' ( 'v' n= number )? (na= string )? )
			// /data1/hecker/gits/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:339:5: 'SDG' ( 'v' n= number )? (na= string )?
			{
			match(input,67,FOLLOW_67_in_sdg_header552); 
			 int version = SDG.DEFAULT_VERSION; 
			// /data1/hecker/gits/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:341:7: ( 'v' n= number )?
			int alt11=2;
			int LA11_0 = input.LA(1);
			if ( (LA11_0==116) ) {
				alt11=1;
			}
			switch (alt11) {
				case 1 :
					// /data1/hecker/gits/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:341:8: 'v' n= number
					{
					match(input,116,FOLLOW_116_in_sdg_header568); 
					pushFollow(FOLLOW_number_in_sdg_header572);
					n=number();
					state._fsp--;

					 version = n; 
					}
					break;

			}

			 String name = null; 
			// /data1/hecker/gits/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:343:7: (na= string )?
			int alt12=2;
			int LA12_0 = input.LA(1);
			if ( (LA12_0==STRING) ) {
				alt12=1;
			}
			switch (alt12) {
				case 1 :
					// /data1/hecker/gits/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:343:8: na= string
					{
					pushFollow(FOLLOW_string_in_sdg_header593);
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
	// /data1/hecker/gits/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:347:9: private node_list returns [List<SDGNodeStub> list = new LinkedList<SDGNodeStub>();] : (n= node )* ;
	public final List<SDGNodeStub> node_list() throws RecognitionException {
		List<SDGNodeStub> list =  new LinkedList<SDGNodeStub>();;


		SDGNodeStub n =null;

		try {
			// /data1/hecker/gits/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:348:3: ( (n= node )* )
			// /data1/hecker/gits/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:348:5: (n= node )*
			{
			// /data1/hecker/gits/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:348:5: (n= node )*
			loop13:
			while (true) {
				int alt13=2;
				int LA13_0 = input.LA(1);
				if ( ((LA13_0 >= 12 && LA13_0 <= 13)||LA13_0==16||(LA13_0 >= 28 && LA13_0 <= 30)||LA13_0==35||(LA13_0 >= 39 && LA13_0 <= 40)||LA13_0==55||LA13_0==60||LA13_0==72) ) {
					alt13=1;
				}

				switch (alt13) {
				case 1 :
					// /data1/hecker/gits/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:348:6: n= node
					{
					pushFollow(FOLLOW_node_in_node_list626);
					n=node();
					state._fsp--;

					 list.add(n); 
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
		return list;
	}
	// $ANTLR end "node_list"



	// $ANTLR start "node"
	// /data1/hecker/gits/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:351:9: private node returns [SDGNodeStub nstub] : k= node_kind id= mayNegNumber '{' node_attributes[nstub] node_edges[nstub] '}' ;
	public final SDGNodeStub node() throws RecognitionException {
		SDGNodeStub nstub = null;


		SDGNode.Kind k =null;
		int id =0;

		try {
			// /data1/hecker/gits/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:352:3: (k= node_kind id= mayNegNumber '{' node_attributes[nstub] node_edges[nstub] '}' )
			// /data1/hecker/gits/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:352:5: k= node_kind id= mayNegNumber '{' node_attributes[nstub] node_edges[nstub] '}'
			{
			pushFollow(FOLLOW_node_kind_in_node652);
			k=node_kind();
			state._fsp--;

			pushFollow(FOLLOW_mayNegNumber_in_node656);
			id=mayNegNumber();
			state._fsp--;

			 nstub = new SDGNodeStub(k, id, defaultSrcPos, defaultBcPos); 
			match(input,117,FOLLOW_117_in_node665); 
			pushFollow(FOLLOW_node_attributes_in_node674);
			node_attributes(nstub);
			state._fsp--;

			pushFollow(FOLLOW_node_edges_in_node684);
			node_edges(nstub);
			state._fsp--;

			match(input,118,FOLLOW_118_in_node691); 
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
	// /data1/hecker/gits/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:359:9: private node_kind returns [SDGNode.Kind kind] : ( 'NORM' | 'PRED' | 'EXPR' | 'ENTR' | 'CALL' | 'ACTI' | 'ACTO' | 'FRMI' | 'FRMO' | 'EXIT' | 'SYNC' | 'FOLD' );
	public final SDGNode.Kind node_kind() throws RecognitionException {
		SDGNode.Kind kind = null;


		try {
			// /data1/hecker/gits/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:360:3: ( 'NORM' | 'PRED' | 'EXPR' | 'ENTR' | 'CALL' | 'ACTI' | 'ACTO' | 'FRMI' | 'FRMO' | 'EXIT' | 'SYNC' | 'FOLD' )
			int alt14=12;
			switch ( input.LA(1) ) {
			case 55:
				{
				alt14=1;
				}
				break;
			case 60:
				{
				alt14=2;
				}
				break;
			case 30:
				{
				alt14=3;
				}
				break;
			case 28:
				{
				alt14=4;
				}
				break;
			case 16:
				{
				alt14=5;
				}
				break;
			case 12:
				{
				alt14=6;
				}
				break;
			case 13:
				{
				alt14=7;
				}
				break;
			case 39:
				{
				alt14=8;
				}
				break;
			case 40:
				{
				alt14=9;
				}
				break;
			case 29:
				{
				alt14=10;
				}
				break;
			case 72:
				{
				alt14=11;
				}
				break;
			case 35:
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
					// /data1/hecker/gits/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:360:5: 'NORM'
					{
					match(input,55,FOLLOW_55_in_node_kind710); 
					 kind = SDGNode.Kind.NORMAL; 
					}
					break;
				case 2 :
					// /data1/hecker/gits/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:361:5: 'PRED'
					{
					match(input,60,FOLLOW_60_in_node_kind718); 
					 kind = SDGNode.Kind.PREDICATE; 
					}
					break;
				case 3 :
					// /data1/hecker/gits/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:362:5: 'EXPR'
					{
					match(input,30,FOLLOW_30_in_node_kind726); 
					 kind = SDGNode.Kind.EXPRESSION; 
					}
					break;
				case 4 :
					// /data1/hecker/gits/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:363:5: 'ENTR'
					{
					match(input,28,FOLLOW_28_in_node_kind734); 
					 kind = SDGNode.Kind.ENTRY; 
					}
					break;
				case 5 :
					// /data1/hecker/gits/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:364:5: 'CALL'
					{
					match(input,16,FOLLOW_16_in_node_kind742); 
					 kind = SDGNode.Kind.CALL; 
					}
					break;
				case 6 :
					// /data1/hecker/gits/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:365:5: 'ACTI'
					{
					match(input,12,FOLLOW_12_in_node_kind750); 
					 kind = SDGNode.Kind.ACTUAL_IN; 
					}
					break;
				case 7 :
					// /data1/hecker/gits/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:366:5: 'ACTO'
					{
					match(input,13,FOLLOW_13_in_node_kind758); 
					 kind = SDGNode.Kind.ACTUAL_OUT; 
					}
					break;
				case 8 :
					// /data1/hecker/gits/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:367:5: 'FRMI'
					{
					match(input,39,FOLLOW_39_in_node_kind766); 
					 kind = SDGNode.Kind.FORMAL_IN; 
					}
					break;
				case 9 :
					// /data1/hecker/gits/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:368:5: 'FRMO'
					{
					match(input,40,FOLLOW_40_in_node_kind774); 
					 kind = SDGNode.Kind.FORMAL_OUT; 
					}
					break;
				case 10 :
					// /data1/hecker/gits/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:369:5: 'EXIT'
					{
					match(input,29,FOLLOW_29_in_node_kind782); 
					 kind = SDGNode.Kind.EXIT; 
					}
					break;
				case 11 :
					// /data1/hecker/gits/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:370:5: 'SYNC'
					{
					match(input,72,FOLLOW_72_in_node_kind790); 
					 kind = SDGNode.Kind.SYNCHRONIZATION; 
					}
					break;
				case 12 :
					// /data1/hecker/gits/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:371:5: 'FOLD'
					{
					match(input,35,FOLLOW_35_in_node_kind798); 
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
	// /data1/hecker/gits/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:374:9: private node_attributes[SDGNodeStub node] : ( node_attr[node] ';' )* ;
	public final void node_attributes(SDGNodeStub node) throws RecognitionException {
		try {
			// /data1/hecker/gits/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:375:3: ( ( node_attr[node] ';' )* )
			// /data1/hecker/gits/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:375:5: ( node_attr[node] ';' )*
			{
			// /data1/hecker/gits/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:375:5: ( node_attr[node] ';' )*
			loop15:
			while (true) {
				int alt15=2;
				int LA15_0 = input.LA(1);
				if ( (LA15_0==11||(LA15_0 >= 14 && LA15_0 <= 15)||LA15_0==23||(LA15_0 >= 51 && LA15_0 <= 53)||(LA15_0 >= 56 && LA15_0 <= 57)||LA15_0==65||LA15_0==73||LA15_0==75||LA15_0==77||LA15_0==79) ) {
					alt15=1;
				}

				switch (alt15) {
				case 1 :
					// /data1/hecker/gits/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:375:6: node_attr[node] ';'
					{
					pushFollow(FOLLOW_node_attr_in_node_attributes817);
					node_attr(node);
					state._fsp--;

					match(input,10,FOLLOW_10_in_node_attributes820); 
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
	}
	// $ANTLR end "node_attributes"



	// $ANTLR start "node_attr"
	// /data1/hecker/gits/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:378:9: private node_attr[SDGNodeStub node] : ( 'S' spos= node_source | 'B' bpos= node_bytecode | 'U' number | 'P' procId= number | 'O' op= node_oper | 'V' val= string | 'T' type= string | 'Z' tn= may_neg_num_set | 'N' | 'C' cl= string | 'A' al= pos_num_set | 'D' ds= pos_num_set | 'U' uct= string | 'LD' ldefs= mayEmptyStringList | 'LU' luses= mayEmptyStringList );
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
		LinkedList<String> ldefs =null;
		LinkedList<String> luses =null;

		try {
			// /data1/hecker/gits/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:379:3: ( 'S' spos= node_source | 'B' bpos= node_bytecode | 'U' number | 'P' procId= number | 'O' op= node_oper | 'V' val= string | 'T' type= string | 'Z' tn= may_neg_num_set | 'N' | 'C' cl= string | 'A' al= pos_num_set | 'D' ds= pos_num_set | 'U' uct= string | 'LD' ldefs= mayEmptyStringList | 'LU' luses= mayEmptyStringList )
			int alt16=15;
			switch ( input.LA(1) ) {
			case 65:
				{
				alt16=1;
				}
				break;
			case 14:
				{
				alt16=2;
				}
				break;
			case 75:
				{
				int LA16_3 = input.LA(2);
				if ( (LA16_3==NUMBER) ) {
					alt16=3;
				}
				else if ( (LA16_3==STRING) ) {
					alt16=13;
				}

				else {
					int nvaeMark = input.mark();
					try {
						input.consume();
						NoViableAltException nvae =
							new NoViableAltException("", 16, 3, input);
						throw nvae;
					} finally {
						input.rewind(nvaeMark);
					}
				}

				}
				break;
			case 57:
				{
				alt16=4;
				}
				break;
			case 56:
				{
				alt16=5;
				}
				break;
			case 77:
				{
				alt16=6;
				}
				break;
			case 73:
				{
				alt16=7;
				}
				break;
			case 79:
				{
				alt16=8;
				}
				break;
			case 53:
				{
				alt16=9;
				}
				break;
			case 15:
				{
				alt16=10;
				}
				break;
			case 11:
				{
				alt16=11;
				}
				break;
			case 23:
				{
				alt16=12;
				}
				break;
			case 51:
				{
				alt16=14;
				}
				break;
			case 52:
				{
				alt16=15;
				}
				break;
			default:
				NoViableAltException nvae =
					new NoViableAltException("", 16, 0, input);
				throw nvae;
			}
			switch (alt16) {
				case 1 :
					// /data1/hecker/gits/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:379:5: 'S' spos= node_source
					{
					match(input,65,FOLLOW_65_in_node_attr838); 
					pushFollow(FOLLOW_node_source_in_node_attr842);
					spos=node_source();
					state._fsp--;

					 node.spos = spos; defaultSrcPos = spos; 
					}
					break;
				case 2 :
					// /data1/hecker/gits/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:380:5: 'B' bpos= node_bytecode
					{
					match(input,14,FOLLOW_14_in_node_attr854); 
					pushFollow(FOLLOW_node_bytecode_in_node_attr858);
					bpos=node_bytecode();
					state._fsp--;

					 node.bpos = bpos; defaultBcPos = bpos; 
					}
					break;
				case 3 :
					// /data1/hecker/gits/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:381:5: 'U' number
					{
					match(input,75,FOLLOW_75_in_node_attr869); 
					pushFollow(FOLLOW_number_in_node_attr871);
					number();
					state._fsp--;

					}
					break;
				case 4 :
					// /data1/hecker/gits/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:382:5: 'P' procId= number
					{
					match(input,57,FOLLOW_57_in_node_attr921); 
					pushFollow(FOLLOW_number_in_node_attr925);
					procId=number();
					state._fsp--;

					 node.procId = procId; 
					}
					break;
				case 5 :
					// /data1/hecker/gits/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:383:5: 'O' op= node_oper
					{
					match(input,56,FOLLOW_56_in_node_attr944); 
					pushFollow(FOLLOW_node_oper_in_node_attr948);
					op=node_oper();
					state._fsp--;

					 node.op = op; 
					}
					break;
				case 6 :
					// /data1/hecker/gits/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:384:5: 'V' val= string
					{
					match(input,77,FOLLOW_77_in_node_attr976); 
					pushFollow(FOLLOW_string_in_node_attr980);
					val=string();
					state._fsp--;

					 node.val = val; 
					}
					break;
				case 7 :
					// /data1/hecker/gits/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:385:5: 'T' type= string
					{
					match(input,73,FOLLOW_73_in_node_attr1008); 
					pushFollow(FOLLOW_string_in_node_attr1012);
					type=string();
					state._fsp--;

					 node.type = type; 
					}
					break;
				case 8 :
					// /data1/hecker/gits/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:386:5: 'Z' tn= may_neg_num_set
					{
					match(input,79,FOLLOW_79_in_node_attr1037); 
					pushFollow(FOLLOW_may_neg_num_set_in_node_attr1041);
					tn=may_neg_num_set();
					state._fsp--;

					 node.threadNums = tn; 
					}
					break;
				case 9 :
					// /data1/hecker/gits/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:387:5: 'N'
					{
					match(input,53,FOLLOW_53_in_node_attr1055); 
					 node.nonTerm = true; 
					}
					break;
				case 10 :
					// /data1/hecker/gits/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:388:5: 'C' cl= string
					{
					match(input,15,FOLLOW_15_in_node_attr1089); 
					pushFollow(FOLLOW_string_in_node_attr1093);
					cl=string();
					state._fsp--;

					 node.classLoader = cl; 
					}
					break;
				case 11 :
					// /data1/hecker/gits/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:389:5: 'A' al= pos_num_set
					{
					match(input,11,FOLLOW_11_in_node_attr1115); 
					pushFollow(FOLLOW_pos_num_set_in_node_attr1119);
					al=pos_num_set();
					state._fsp--;

					 node.allocSites = al; 
					}
					break;
				case 12 :
					// /data1/hecker/gits/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:390:5: 'D' ds= pos_num_set
					{
					match(input,23,FOLLOW_23_in_node_attr1137); 
					pushFollow(FOLLOW_pos_num_set_in_node_attr1141);
					ds=pos_num_set();
					state._fsp--;

					 node.aliasDataSrc = ds; 
					}
					break;
				case 13 :
					// /data1/hecker/gits/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:391:5: 'U' uct= string
					{
					match(input,75,FOLLOW_75_in_node_attr1158); 
					pushFollow(FOLLOW_string_in_node_attr1162);
					uct=string();
					state._fsp--;

					 node.unresolvedCallTarget = uct; 
					}
					break;
				case 14 :
					// /data1/hecker/gits/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:392:5: 'LD' ldefs= mayEmptyStringList
					{
					match(input,51,FOLLOW_51_in_node_attr1180); 
					pushFollow(FOLLOW_mayEmptyStringList_in_node_attr1184);
					ldefs=mayEmptyStringList();
					state._fsp--;


					                              node.localDefNames = ldefs;
					                            
					}
					break;
				case 15 :
					// /data1/hecker/gits/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:395:5: 'LU' luses= mayEmptyStringList
					{
					match(input,52,FOLLOW_52_in_node_attr1193); 
					pushFollow(FOLLOW_mayEmptyStringList_in_node_attr1197);
					luses=mayEmptyStringList();
					state._fsp--;


					                              node.localUseNames = ldefs;
					                            
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
	// /data1/hecker/gits/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:399:9: private pos_num_set returns [TIntSet nums = new TIntHashSet();] : n= number ( ',' n2= number )* ;
	public final TIntSet pos_num_set() throws RecognitionException {
		TIntSet nums =  new TIntHashSet();;


		int n =0;
		int n2 =0;

		try {
			// /data1/hecker/gits/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:400:3: (n= number ( ',' n2= number )* )
			// /data1/hecker/gits/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:400:5: n= number ( ',' n2= number )*
			{
			pushFollow(FOLLOW_number_in_pos_num_set1218);
			n=number();
			state._fsp--;

			 nums.add(n); 
			// /data1/hecker/gits/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:400:31: ( ',' n2= number )*
			loop17:
			while (true) {
				int alt17=2;
				int LA17_0 = input.LA(1);
				if ( (LA17_0==7) ) {
					alt17=1;
				}

				switch (alt17) {
				case 1 :
					// /data1/hecker/gits/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:400:32: ',' n2= number
					{
					match(input,7,FOLLOW_7_in_pos_num_set1223); 
					pushFollow(FOLLOW_number_in_pos_num_set1227);
					n2=number();
					state._fsp--;

					 nums.add(n2); 
					}
					break;

				default :
					break loop17;
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
	// /data1/hecker/gits/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:403:9: private may_neg_num_set returns [TIntSet nums = new TIntHashSet();] : n= mayNegNumber ( ',' n2= mayNegNumber )* ;
	public final TIntSet may_neg_num_set() throws RecognitionException {
		TIntSet nums =  new TIntHashSet();;


		int n =0;
		int n2 =0;

		try {
			// /data1/hecker/gits/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:404:3: (n= mayNegNumber ( ',' n2= mayNegNumber )* )
			// /data1/hecker/gits/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:404:5: n= mayNegNumber ( ',' n2= mayNegNumber )*
			{
			pushFollow(FOLLOW_mayNegNumber_in_may_neg_num_set1253);
			n=mayNegNumber();
			state._fsp--;

			 nums.add(n); 
			// /data1/hecker/gits/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:404:37: ( ',' n2= mayNegNumber )*
			loop18:
			while (true) {
				int alt18=2;
				int LA18_0 = input.LA(1);
				if ( (LA18_0==7) ) {
					alt18=1;
				}

				switch (alt18) {
				case 1 :
					// /data1/hecker/gits/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:404:38: ',' n2= mayNegNumber
					{
					match(input,7,FOLLOW_7_in_may_neg_num_set1258); 
					pushFollow(FOLLOW_mayNegNumber_in_may_neg_num_set1262);
					n2=mayNegNumber();
					state._fsp--;

					 nums.add(n2); 
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
		return nums;
	}
	// $ANTLR end "may_neg_num_set"



	// $ANTLR start "node_source"
	// /data1/hecker/gits/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:407:9: private node_source returns [SourcePos spos] : filename= string ':' startRow= number ',' startColumn= number '-' endRow= number ',' endColumn= number ;
	public final SourcePos node_source() throws RecognitionException {
		SourcePos spos = null;


		String filename =null;
		int startRow =0;
		int startColumn =0;
		int endRow =0;
		int endColumn =0;

		try {
			// /data1/hecker/gits/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:408:3: (filename= string ':' startRow= number ',' startColumn= number '-' endRow= number ',' endColumn= number )
			// /data1/hecker/gits/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:408:5: filename= string ':' startRow= number ',' startColumn= number '-' endRow= number ',' endColumn= number
			{
			pushFollow(FOLLOW_string_in_node_source1288);
			filename=string();
			state._fsp--;

			match(input,9,FOLLOW_9_in_node_source1290); 
			pushFollow(FOLLOW_number_in_node_source1294);
			startRow=number();
			state._fsp--;

			match(input,7,FOLLOW_7_in_node_source1296); 
			pushFollow(FOLLOW_number_in_node_source1300);
			startColumn=number();
			state._fsp--;

			match(input,8,FOLLOW_8_in_node_source1302); 
			pushFollow(FOLLOW_number_in_node_source1306);
			endRow=number();
			state._fsp--;

			match(input,7,FOLLOW_7_in_node_source1308); 
			pushFollow(FOLLOW_number_in_node_source1312);
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
	// /data1/hecker/gits/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:412:9: private node_bytecode returns [ByteCodePos bpos] : name= string ':' index= mayNegNumber ;
	public final ByteCodePos node_bytecode() throws RecognitionException {
		ByteCodePos bpos = null;


		String name =null;
		int index =0;

		try {
			// /data1/hecker/gits/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:413:3: (name= string ':' index= mayNegNumber )
			// /data1/hecker/gits/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:413:5: name= string ':' index= mayNegNumber
			{
			pushFollow(FOLLOW_string_in_node_bytecode1343);
			name=string();
			state._fsp--;

			match(input,9,FOLLOW_9_in_node_bytecode1345); 
			pushFollow(FOLLOW_mayNegNumber_in_node_bytecode1349);
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
	// /data1/hecker/gits/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:416:9: private node_oper returns [SDGNode.Operation op] : ( 'empty' | 'intconst' | 'floatconst' | 'charconst' | 'stringconst' | 'functionconst' | 'shortcut' | 'question' | 'binary' | 'unary' | 'derefer' | 'refer' | 'array' | 'select' | 'reference' | 'declaration' | 'modify' | 'modassign' | 'assign' | 'IF' | 'loop' | 'jump' | 'compound' | 'call' | 'entry' | 'exit' | 'form-in' | 'form-ellip' | 'form-out' | 'act-in' | 'act-out' | 'monitor' );
	public final SDGNode.Operation node_oper() throws RecognitionException {
		SDGNode.Operation op = null;


		try {
			// /data1/hecker/gits/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:417:3: ( 'empty' | 'intconst' | 'floatconst' | 'charconst' | 'stringconst' | 'functionconst' | 'shortcut' | 'question' | 'binary' | 'unary' | 'derefer' | 'refer' | 'array' | 'select' | 'reference' | 'declaration' | 'modify' | 'modassign' | 'assign' | 'IF' | 'loop' | 'jump' | 'compound' | 'call' | 'entry' | 'exit' | 'form-in' | 'form-ellip' | 'form-out' | 'act-in' | 'act-out' | 'monitor' )
			int alt19=32;
			switch ( input.LA(1) ) {
			case 92:
				{
				alt19=1;
				}
				break;
			case 101:
				{
				alt19=2;
				}
				break;
			case 96:
				{
				alt19=3;
				}
				break;
			case 88:
				{
				alt19=4;
				}
				break;
			case 113:
				{
				alt19=5;
				}
				break;
			case 100:
				{
				alt19=6;
				}
				break;
			case 112:
				{
				alt19=7;
				}
				break;
			case 108:
				{
				alt19=8;
				}
				break;
			case 86:
				{
				alt19=9;
				}
				break;
			case 115:
				{
				alt19=10;
				}
				break;
			case 91:
				{
				alt19=11;
				}
				break;
			case 109:
				{
				alt19=12;
				}
				break;
			case 84:
				{
				alt19=13;
				}
				break;
			case 111:
				{
				alt19=14;
				}
				break;
			case 110:
				{
				alt19=15;
				}
				break;
			case 90:
				{
				alt19=16;
				}
				break;
			case 105:
				{
				alt19=17;
				}
				break;
			case 104:
				{
				alt19=18;
				}
				break;
			case 85:
				{
				alt19=19;
				}
				break;
			case 44:
				{
				alt19=20;
				}
				break;
			case 103:
				{
				alt19=21;
				}
				break;
			case 102:
				{
				alt19=22;
				}
				break;
			case 89:
				{
				alt19=23;
				}
				break;
			case 87:
				{
				alt19=24;
				}
				break;
			case 93:
				{
				alt19=25;
				}
				break;
			case 94:
				{
				alt19=26;
				}
				break;
			case 98:
				{
				alt19=27;
				}
				break;
			case 97:
				{
				alt19=28;
				}
				break;
			case 99:
				{
				alt19=29;
				}
				break;
			case 82:
				{
				alt19=30;
				}
				break;
			case 83:
				{
				alt19=31;
				}
				break;
			case 106:
				{
				alt19=32;
				}
				break;
			default:
				NoViableAltException nvae =
					new NoViableAltException("", 19, 0, input);
				throw nvae;
			}
			switch (alt19) {
				case 1 :
					// /data1/hecker/gits/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:417:5: 'empty'
					{
					match(input,92,FOLLOW_92_in_node_oper1370); 
					 op = SDGNode.Operation.EMPTY; 
					}
					break;
				case 2 :
					// /data1/hecker/gits/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:418:5: 'intconst'
					{
					match(input,101,FOLLOW_101_in_node_oper1387); 
					 op = SDGNode.Operation.INT_CONST; 
					}
					break;
				case 3 :
					// /data1/hecker/gits/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:419:5: 'floatconst'
					{
					match(input,96,FOLLOW_96_in_node_oper1401); 
					 op = SDGNode.Operation.FLOAT_CONST; 
					}
					break;
				case 4 :
					// /data1/hecker/gits/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:420:5: 'charconst'
					{
					match(input,88,FOLLOW_88_in_node_oper1413); 
					 op = SDGNode.Operation.CHAR_CONST; 
					}
					break;
				case 5 :
					// /data1/hecker/gits/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:421:5: 'stringconst'
					{
					match(input,113,FOLLOW_113_in_node_oper1426); 
					 op = SDGNode.Operation.STRING_CONST; 
					}
					break;
				case 6 :
					// /data1/hecker/gits/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:422:5: 'functionconst'
					{
					match(input,100,FOLLOW_100_in_node_oper1437); 
					 op = SDGNode.Operation.FUNCTION_CONST; 
					}
					break;
				case 7 :
					// /data1/hecker/gits/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:423:5: 'shortcut'
					{
					match(input,112,FOLLOW_112_in_node_oper1446); 
					 op = SDGNode.Operation.SHORTCUT; 
					}
					break;
				case 8 :
					// /data1/hecker/gits/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:424:5: 'question'
					{
					match(input,108,FOLLOW_108_in_node_oper1460); 
					 op = SDGNode.Operation.QUESTION; 
					}
					break;
				case 9 :
					// /data1/hecker/gits/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:425:5: 'binary'
					{
					match(input,86,FOLLOW_86_in_node_oper1474); 
					 op = SDGNode.Operation.BINARY; 
					}
					break;
				case 10 :
					// /data1/hecker/gits/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:426:5: 'unary'
					{
					match(input,115,FOLLOW_115_in_node_oper1490); 
					 op = SDGNode.Operation.UNARY; 
					}
					break;
				case 11 :
					// /data1/hecker/gits/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:427:5: 'derefer'
					{
					match(input,91,FOLLOW_91_in_node_oper1507); 
					 op = SDGNode.Operation.DEREFER; 
					}
					break;
				case 12 :
					// /data1/hecker/gits/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:428:5: 'refer'
					{
					match(input,109,FOLLOW_109_in_node_oper1522); 
					 op = SDGNode.Operation.REFER; 
					}
					break;
				case 13 :
					// /data1/hecker/gits/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:429:5: 'array'
					{
					match(input,84,FOLLOW_84_in_node_oper1539); 
					 op = SDGNode.Operation.ARRAY; 
					}
					break;
				case 14 :
					// /data1/hecker/gits/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:430:5: 'select'
					{
					match(input,111,FOLLOW_111_in_node_oper1556); 
					 op = SDGNode.Operation.SELECT; 
					}
					break;
				case 15 :
					// /data1/hecker/gits/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:431:5: 'reference'
					{
					match(input,110,FOLLOW_110_in_node_oper1572); 
					 op = SDGNode.Operation.REFERENCE; 
					}
					break;
				case 16 :
					// /data1/hecker/gits/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:432:5: 'declaration'
					{
					match(input,90,FOLLOW_90_in_node_oper1585); 
					 op = SDGNode.Operation.DECLARATION; 
					}
					break;
				case 17 :
					// /data1/hecker/gits/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:433:5: 'modify'
					{
					match(input,105,FOLLOW_105_in_node_oper1596); 
					 op = SDGNode.Operation.MODIFY; 
					}
					break;
				case 18 :
					// /data1/hecker/gits/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:434:5: 'modassign'
					{
					match(input,104,FOLLOW_104_in_node_oper1612); 
					 op = SDGNode.Operation.MODASSIGN; 
					}
					break;
				case 19 :
					// /data1/hecker/gits/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:435:5: 'assign'
					{
					match(input,85,FOLLOW_85_in_node_oper1625); 
					 op = SDGNode.Operation.ASSIGN; 
					}
					break;
				case 20 :
					// /data1/hecker/gits/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:436:5: 'IF'
					{
					match(input,44,FOLLOW_44_in_node_oper1641); 
					 op = SDGNode.Operation.IF; 
					}
					break;
				case 21 :
					// /data1/hecker/gits/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:437:5: 'loop'
					{
					match(input,103,FOLLOW_103_in_node_oper1661); 
					 op = SDGNode.Operation.LOOP; 
					}
					break;
				case 22 :
					// /data1/hecker/gits/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:438:5: 'jump'
					{
					match(input,102,FOLLOW_102_in_node_oper1679); 
					 op = SDGNode.Operation.JUMP; 
					}
					break;
				case 23 :
					// /data1/hecker/gits/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:439:5: 'compound'
					{
					match(input,89,FOLLOW_89_in_node_oper1697); 
					 op = SDGNode.Operation.COMPOUND; 
					}
					break;
				case 24 :
					// /data1/hecker/gits/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:440:5: 'call'
					{
					match(input,87,FOLLOW_87_in_node_oper1711); 
					 op = SDGNode.Operation.CALL; 
					}
					break;
				case 25 :
					// /data1/hecker/gits/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:441:5: 'entry'
					{
					match(input,93,FOLLOW_93_in_node_oper1729); 
					 op = SDGNode.Operation.ENTRY; 
					}
					break;
				case 26 :
					// /data1/hecker/gits/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:442:5: 'exit'
					{
					match(input,94,FOLLOW_94_in_node_oper1746); 
					 op = SDGNode.Operation.EXIT; 
					}
					break;
				case 27 :
					// /data1/hecker/gits/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:443:5: 'form-in'
					{
					match(input,98,FOLLOW_98_in_node_oper1764); 
					 op = SDGNode.Operation.FORMAL_IN; 
					}
					break;
				case 28 :
					// /data1/hecker/gits/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:444:5: 'form-ellip'
					{
					match(input,97,FOLLOW_97_in_node_oper1779); 
					 op = SDGNode.Operation.FORMAL_ELLIP; 
					}
					break;
				case 29 :
					// /data1/hecker/gits/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:445:5: 'form-out'
					{
					match(input,99,FOLLOW_99_in_node_oper1791); 
					 op = SDGNode.Operation.FORMAL_OUT; 
					}
					break;
				case 30 :
					// /data1/hecker/gits/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:446:5: 'act-in'
					{
					match(input,82,FOLLOW_82_in_node_oper1805); 
					 op = SDGNode.Operation.ACTUAL_IN; 
					}
					break;
				case 31 :
					// /data1/hecker/gits/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:447:5: 'act-out'
					{
					match(input,83,FOLLOW_83_in_node_oper1821); 
					 op = SDGNode.Operation.ACTUAL_OUT; 
					}
					break;
				case 32 :
					// /data1/hecker/gits/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:448:5: 'monitor'
					{
					match(input,106,FOLLOW_106_in_node_oper1836); 
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
	// /data1/hecker/gits/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:451:9: private node_edges[SDGNodeStub node] : (e= edge ';' )* ;
	public final void node_edges(SDGNodeStub node) throws RecognitionException {
		SDGEdgeStub e =null;

		try {
			// /data1/hecker/gits/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:452:3: ( (e= edge ';' )* )
			// /data1/hecker/gits/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:452:5: (e= edge ';' )*
			{
			// /data1/hecker/gits/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:452:5: (e= edge ';' )*
			loop20:
			while (true) {
				int alt20=2;
				int LA20_0 = input.LA(1);
				if ( ((LA20_0 >= 17 && LA20_0 <= 21)||(LA20_0 >= 24 && LA20_0 <= 26)||(LA20_0 >= 33 && LA20_0 <= 34)||(LA20_0 >= 36 && LA20_0 <= 38)||(LA20_0 >= 42 && LA20_0 <= 43)||LA20_0==45||(LA20_0 >= 47 && LA20_0 <= 49)||LA20_0==54||(LA20_0 >= 58 && LA20_0 <= 59)||(LA20_0 >= 61 && LA20_0 <= 64)||LA20_0==66||(LA20_0 >= 68 && LA20_0 <= 71)||LA20_0==76||LA20_0==78) ) {
					alt20=1;
				}

				switch (alt20) {
				case 1 :
					// /data1/hecker/gits/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:452:6: e= edge ';'
					{
					pushFollow(FOLLOW_edge_in_node_edges1864);
					e=edge();
					state._fsp--;

					match(input,10,FOLLOW_10_in_node_edges1866); 
					 node.edges.add(e); 
					}
					break;

				default :
					break loop20;
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
	// /data1/hecker/gits/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:455:9: private edge returns [SDGEdgeStub estub] : k= edge_kind nr= number ( ':' label= string )? ;
	public final SDGEdgeStub edge() throws RecognitionException {
		SDGEdgeStub estub = null;


		SDGEdge.Kind k =null;
		int nr =0;
		String label =null;

		try {
			// /data1/hecker/gits/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:456:3: (k= edge_kind nr= number ( ':' label= string )? )
			// /data1/hecker/gits/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:456:5: k= edge_kind nr= number ( ':' label= string )?
			{
			pushFollow(FOLLOW_edge_kind_in_edge1891);
			k=edge_kind();
			state._fsp--;

			pushFollow(FOLLOW_number_in_edge1895);
			nr=number();
			state._fsp--;

			 estub = new SDGEdgeStub(k, nr); 
			// /data1/hecker/gits/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:456:63: ( ':' label= string )?
			int alt21=2;
			int LA21_0 = input.LA(1);
			if ( (LA21_0==9) ) {
				alt21=1;
			}
			switch (alt21) {
				case 1 :
					// /data1/hecker/gits/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:456:64: ':' label= string
					{
					match(input,9,FOLLOW_9_in_edge1900); 
					pushFollow(FOLLOW_string_in_edge1904);
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
	// /data1/hecker/gits/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:459:9: private edge_kind returns [SDGEdge.Kind kind] : ( 'DD' | 'DH' | 'DA' | 'CD' | 'CE' | 'UN' | 'CF' | 'NF' | 'RF' | 'CC' | 'CL' | 'PI' | 'PO' | 'SU' | 'SH' | 'SF' | 'PS' | 'FORK' | 'FORK_IN' | 'FORK_OUT' | 'JOIN' | 'ID' | 'IW' | 'SD' | 'HE' | 'FD' | 'FI' | 'RY' | 'JF' | 'SP' | 'VD' | 'RD' | 'JD' );
	public final SDGEdge.Kind edge_kind() throws RecognitionException {
		SDGEdge.Kind kind = null;


		try {
			// /data1/hecker/gits/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:461:3: ( 'DD' | 'DH' | 'DA' | 'CD' | 'CE' | 'UN' | 'CF' | 'NF' | 'RF' | 'CC' | 'CL' | 'PI' | 'PO' | 'SU' | 'SH' | 'SF' | 'PS' | 'FORK' | 'FORK_IN' | 'FORK_OUT' | 'JOIN' | 'ID' | 'IW' | 'SD' | 'HE' | 'FD' | 'FI' | 'RY' | 'JF' | 'SP' | 'VD' | 'RD' | 'JD' )
			int alt22=33;
			switch ( input.LA(1) ) {
			case 25:
				{
				alt22=1;
				}
				break;
			case 26:
				{
				alt22=2;
				}
				break;
			case 24:
				{
				alt22=3;
				}
				break;
			case 18:
				{
				alt22=4;
				}
				break;
			case 19:
				{
				alt22=5;
				}
				break;
			case 76:
				{
				alt22=6;
				}
				break;
			case 20:
				{
				alt22=7;
				}
				break;
			case 54:
				{
				alt22=8;
				}
				break;
			case 63:
				{
				alt22=9;
				}
				break;
			case 17:
				{
				alt22=10;
				}
				break;
			case 21:
				{
				alt22=11;
				}
				break;
			case 58:
				{
				alt22=12;
				}
				break;
			case 59:
				{
				alt22=13;
				}
				break;
			case 71:
				{
				alt22=14;
				}
				break;
			case 69:
				{
				alt22=15;
				}
				break;
			case 68:
				{
				alt22=16;
				}
				break;
			case 61:
				{
				alt22=17;
				}
				break;
			case 36:
				{
				alt22=18;
				}
				break;
			case 37:
				{
				alt22=19;
				}
				break;
			case 38:
				{
				alt22=20;
				}
				break;
			case 49:
				{
				alt22=21;
				}
				break;
			case 43:
				{
				alt22=22;
				}
				break;
			case 45:
				{
				alt22=23;
				}
				break;
			case 66:
				{
				alt22=24;
				}
				break;
			case 42:
				{
				alt22=25;
				}
				break;
			case 33:
				{
				alt22=26;
				}
				break;
			case 34:
				{
				alt22=27;
				}
				break;
			case 64:
				{
				alt22=28;
				}
				break;
			case 48:
				{
				alt22=29;
				}
				break;
			case 70:
				{
				alt22=30;
				}
				break;
			case 78:
				{
				alt22=31;
				}
				break;
			case 62:
				{
				alt22=32;
				}
				break;
			case 47:
				{
				alt22=33;
				}
				break;
			default:
				NoViableAltException nvae =
					new NoViableAltException("", 22, 0, input);
				throw nvae;
			}
			switch (alt22) {
				case 1 :
					// /data1/hecker/gits/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:461:5: 'DD'
					{
					match(input,25,FOLLOW_25_in_edge_kind1929); 
					 kind = SDGEdge.Kind.DATA_DEP; 
					}
					break;
				case 2 :
					// /data1/hecker/gits/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:462:5: 'DH'
					{
					match(input,26,FOLLOW_26_in_edge_kind1949); 
					 kind = SDGEdge.Kind.DATA_HEAP; 
					}
					break;
				case 3 :
					// /data1/hecker/gits/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:463:5: 'DA'
					{
					match(input,24,FOLLOW_24_in_edge_kind1968); 
					 kind = SDGEdge.Kind.DATA_ALIAS; 
					}
					break;
				case 4 :
					// /data1/hecker/gits/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:465:5: 'CD'
					{
					match(input,18,FOLLOW_18_in_edge_kind1987); 
					 kind = SDGEdge.Kind.CONTROL_DEP_COND; 
					}
					break;
				case 5 :
					// /data1/hecker/gits/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:466:5: 'CE'
					{
					match(input,19,FOLLOW_19_in_edge_kind1999); 
					 kind = SDGEdge.Kind.CONTROL_DEP_EXPR; 
					}
					break;
				case 6 :
					// /data1/hecker/gits/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:467:5: 'UN'
					{
					match(input,76,FOLLOW_76_in_edge_kind2011); 
					 kind = SDGEdge.Kind.CONTROL_DEP_UNCOND; 
					}
					break;
				case 7 :
					// /data1/hecker/gits/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:469:5: 'CF'
					{
					match(input,20,FOLLOW_20_in_edge_kind2022); 
					 kind = SDGEdge.Kind.CONTROL_FLOW; 
					}
					break;
				case 8 :
					// /data1/hecker/gits/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:470:5: 'NF'
					{
					match(input,54,FOLLOW_54_in_edge_kind2038); 
					 kind = SDGEdge.Kind.NO_FLOW; 
					}
					break;
				case 9 :
					// /data1/hecker/gits/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:471:5: 'RF'
					{
					match(input,63,FOLLOW_63_in_edge_kind2059); 
					 kind = SDGEdge.Kind.RETURN; 
					}
					break;
				case 10 :
					// /data1/hecker/gits/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:473:5: 'CC'
					{
					match(input,17,FOLLOW_17_in_edge_kind2082); 
					 kind = SDGEdge.Kind.CONTROL_DEP_CALL; 
					}
					break;
				case 11 :
					// /data1/hecker/gits/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:474:5: 'CL'
					{
					match(input,21,FOLLOW_21_in_edge_kind2090); 
					 kind = SDGEdge.Kind.CALL; 
					}
					break;
				case 12 :
					// /data1/hecker/gits/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:475:5: 'PI'
					{
					match(input,58,FOLLOW_58_in_edge_kind2098); 
					 kind = SDGEdge.Kind.PARAMETER_IN; 
					}
					break;
				case 13 :
					// /data1/hecker/gits/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:476:5: 'PO'
					{
					match(input,59,FOLLOW_59_in_edge_kind2106); 
					 kind = SDGEdge.Kind.PARAMETER_OUT; 
					}
					break;
				case 14 :
					// /data1/hecker/gits/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:478:5: 'SU'
					{
					match(input,71,FOLLOW_71_in_edge_kind2115); 
					 kind = SDGEdge.Kind.SUMMARY; 
					}
					break;
				case 15 :
					// /data1/hecker/gits/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:479:5: 'SH'
					{
					match(input,69,FOLLOW_69_in_edge_kind2123); 
					 kind = SDGEdge.Kind.SUMMARY_NO_ALIAS; 
					}
					break;
				case 16 :
					// /data1/hecker/gits/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:480:5: 'SF'
					{
					match(input,68,FOLLOW_68_in_edge_kind2131); 
					 kind = SDGEdge.Kind.SUMMARY_DATA; 
					}
					break;
				case 17 :
					// /data1/hecker/gits/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:482:5: 'PS'
					{
					match(input,61,FOLLOW_61_in_edge_kind2140); 
					 kind = SDGEdge.Kind.PARAMETER_STRUCTURE; 
					}
					break;
				case 18 :
					// /data1/hecker/gits/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:484:5: 'FORK'
					{
					match(input,36,FOLLOW_36_in_edge_kind2149); 
					 kind = SDGEdge.Kind.FORK; 
					}
					break;
				case 19 :
					// /data1/hecker/gits/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:485:5: 'FORK_IN'
					{
					match(input,37,FOLLOW_37_in_edge_kind2157); 
					 kind = SDGEdge.Kind.FORK_IN; 
					}
					break;
				case 20 :
					// /data1/hecker/gits/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:486:5: 'FORK_OUT'
					{
					match(input,38,FOLLOW_38_in_edge_kind2165); 
					 kind = SDGEdge.Kind.FORK_OUT; 
					}
					break;
				case 21 :
					// /data1/hecker/gits/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:487:5: 'JOIN'
					{
					match(input,49,FOLLOW_49_in_edge_kind2173); 
					 kind = SDGEdge.Kind.JOIN; 
					}
					break;
				case 22 :
					// /data1/hecker/gits/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:488:5: 'ID'
					{
					match(input,43,FOLLOW_43_in_edge_kind2181); 
					 kind = SDGEdge.Kind.INTERFERENCE; 
					}
					break;
				case 23 :
					// /data1/hecker/gits/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:489:5: 'IW'
					{
					match(input,45,FOLLOW_45_in_edge_kind2189); 
					 kind = SDGEdge.Kind.INTERFERENCE_WRITE; 
					}
					break;
				case 24 :
					// /data1/hecker/gits/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:490:5: 'SD'
					{
					match(input,66,FOLLOW_66_in_edge_kind2197); 
					 kind = SDGEdge.Kind.SYNCHRONIZATION; 
					}
					break;
				case 25 :
					// /data1/hecker/gits/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:492:5: 'HE'
					{
					match(input,42,FOLLOW_42_in_edge_kind2206); 
					 kind = SDGEdge.Kind.HELP; 
					}
					break;
				case 26 :
					// /data1/hecker/gits/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:493:5: 'FD'
					{
					match(input,33,FOLLOW_33_in_edge_kind2214); 
					 kind = SDGEdge.Kind.FOLDED; 
					}
					break;
				case 27 :
					// /data1/hecker/gits/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:494:5: 'FI'
					{
					match(input,34,FOLLOW_34_in_edge_kind2222); 
					 kind = SDGEdge.Kind.FOLD_INCLUDE; 
					}
					break;
				case 28 :
					// /data1/hecker/gits/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:496:5: 'RY'
					{
					match(input,64,FOLLOW_64_in_edge_kind2231); 
					 kind = SDGEdge.Kind.READY_DEP; 
					}
					break;
				case 29 :
					// /data1/hecker/gits/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:497:5: 'JF'
					{
					match(input,48,FOLLOW_48_in_edge_kind2239); 
					 kind = SDGEdge.Kind.JUMP_FLOW; 
					}
					break;
				case 30 :
					// /data1/hecker/gits/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:498:5: 'SP'
					{
					match(input,70,FOLLOW_70_in_edge_kind2247); 
					 kind = SDGEdge.Kind.SUMMARY; 
					}
					break;
				case 31 :
					// /data1/hecker/gits/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:499:5: 'VD'
					{
					match(input,78,FOLLOW_78_in_edge_kind2255); 
					 kind = SDGEdge.Kind.DATA_DEP_EXPR_VALUE; 
					}
					break;
				case 32 :
					// /data1/hecker/gits/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:500:5: 'RD'
					{
					match(input,62,FOLLOW_62_in_edge_kind2263); 
					 kind = SDGEdge.Kind.DATA_DEP_EXPR_REFERENCE; 
					}
					break;
				case 33 :
					// /data1/hecker/gits/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:501:5: 'JD'
					{
					match(input,47,FOLLOW_47_in_edge_kind2271); 
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
	// /data1/hecker/gits/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:504:9: private mayNegNumber returns [int nr] : ( '-' n= number |n= number );
	public final int mayNegNumber() throws RecognitionException {
		int nr = 0;


		int n =0;

		try {
			// /data1/hecker/gits/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:505:3: ( '-' n= number |n= number )
			int alt23=2;
			int LA23_0 = input.LA(1);
			if ( (LA23_0==8) ) {
				alt23=1;
			}
			else if ( (LA23_0==NUMBER) ) {
				alt23=2;
			}

			else {
				NoViableAltException nvae =
					new NoViableAltException("", 23, 0, input);
				throw nvae;
			}

			switch (alt23) {
				case 1 :
					// /data1/hecker/gits/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:505:5: '-' n= number
					{
					match(input,8,FOLLOW_8_in_mayNegNumber2292); 
					pushFollow(FOLLOW_number_in_mayNegNumber2296);
					n=number();
					state._fsp--;

					 nr = -n; 
					}
					break;
				case 2 :
					// /data1/hecker/gits/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:506:5: n= number
					{
					pushFollow(FOLLOW_number_in_mayNegNumber2306);
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
	// /data1/hecker/gits/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:509:9: private number returns [int nr] : n= NUMBER ;
	public final int number() throws RecognitionException {
		int nr = 0;


		Token n=null;

		try {
			// /data1/hecker/gits/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:510:3: (n= NUMBER )
			// /data1/hecker/gits/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:510:5: n= NUMBER
			{
			n=(Token)match(input,NUMBER,FOLLOW_NUMBER_in_number2329); 
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
	// /data1/hecker/gits/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:513:9: private string returns [String str] : s= STRING ;
	public final String string() throws RecognitionException {
		String str = null;


		Token s=null;

		try {
			// /data1/hecker/gits/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:514:3: (s= STRING )
			// /data1/hecker/gits/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:514:5: s= STRING
			{
			s=(Token)match(input,STRING,FOLLOW_STRING_in_string2352); 
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
	// /data1/hecker/gits/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:517:9: private bool returns [boolean b] : ( 'true' | 'false' );
	public final boolean bool() throws RecognitionException {
		boolean b = false;


		try {
			// /data1/hecker/gits/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:518:3: ( 'true' | 'false' )
			int alt24=2;
			int LA24_0 = input.LA(1);
			if ( (LA24_0==114) ) {
				alt24=1;
			}
			else if ( (LA24_0==95) ) {
				alt24=2;
			}

			else {
				NoViableAltException nvae =
					new NoViableAltException("", 24, 0, input);
				throw nvae;
			}

			switch (alt24) {
				case 1 :
					// /data1/hecker/gits/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:518:5: 'true'
					{
					match(input,114,FOLLOW_114_in_bool2373); 
					 b = true; 
					}
					break;
				case 2 :
					// /data1/hecker/gits/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:519:5: 'false'
					{
					match(input,95,FOLLOW_95_in_bool2382); 
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



	public static final BitSet FOLLOW_sdg_header_in_sdg_file73 = new BitSet(new long[]{0x0000000000000000L,0x0020000000000000L});
	public static final BitSet FOLLOW_117_in_sdg_file89 = new BitSet(new long[]{0x1080418870013000L,0x0040000000000500L});
	public static final BitSet FOLLOW_46_in_sdg_file99 = new BitSet(new long[]{0x1080018870013000L,0x0040000000000500L});
	public static final BitSet FOLLOW_node_list_in_sdg_file128 = new BitSet(new long[]{0x0000000000000000L,0x0040000000000400L});
	public static final BitSet FOLLOW_thread_info_in_sdg_file150 = new BitSet(new long[]{0x0000000000000000L,0x0040000000000000L});
	public static final BitSet FOLLOW_118_in_sdg_file163 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_thread_in_thread_info192 = new BitSet(new long[]{0x0000000000000002L,0x0000000000000400L});
	public static final BitSet FOLLOW_74_in_thread224 = new BitSet(new long[]{0x0000000000000010L});
	public static final BitSet FOLLOW_number_in_thread228 = new BitSet(new long[]{0x0000000000000000L,0x0020000000000000L});
	public static final BitSet FOLLOW_117_in_thread230 = new BitSet(new long[]{0x0000000080000000L});
	public static final BitSet FOLLOW_31_in_thread238 = new BitSet(new long[]{0x0000000000000010L});
	public static final BitSet FOLLOW_number_in_thread244 = new BitSet(new long[]{0x0000000000000400L});
	public static final BitSet FOLLOW_10_in_thread246 = new BitSet(new long[]{0x0000000100000000L});
	public static final BitSet FOLLOW_32_in_thread254 = new BitSet(new long[]{0x0000000000000010L});
	public static final BitSet FOLLOW_number_in_thread261 = new BitSet(new long[]{0x0000000000000400L});
	public static final BitSet FOLLOW_10_in_thread263 = new BitSet(new long[]{0x0000020000000000L});
	public static final BitSet FOLLOW_41_in_thread271 = new BitSet(new long[]{0x0000000000000010L});
	public static final BitSet FOLLOW_number_in_thread278 = new BitSet(new long[]{0x0000000000000400L});
	public static final BitSet FOLLOW_10_in_thread280 = new BitSet(new long[]{0x0004000000000000L});
	public static final BitSet FOLLOW_50_in_thread288 = new BitSet(new long[]{0x0000000000000010L,0x0000080000010000L});
	public static final BitSet FOLLOW_listOrSingleNumber_in_thread295 = new BitSet(new long[]{0x0000000000000400L});
	public static final BitSet FOLLOW_10_in_thread298 = new BitSet(new long[]{0x0000000000400000L});
	public static final BitSet FOLLOW_22_in_thread306 = new BitSet(new long[]{0x0000000000000000L,0x0000080000010000L});
	public static final BitSet FOLLOW_context_in_thread310 = new BitSet(new long[]{0x0000000000000400L});
	public static final BitSet FOLLOW_10_in_thread313 = new BitSet(new long[]{0x0000000008000000L});
	public static final BitSet FOLLOW_27_in_thread321 = new BitSet(new long[]{0x0000000000000000L,0x0004000080000000L});
	public static final BitSet FOLLOW_bool_in_thread325 = new BitSet(new long[]{0x0000000000000400L});
	public static final BitSet FOLLOW_10_in_thread327 = new BitSet(new long[]{0x0000000000000000L,0x0040000000000000L});
	public static final BitSet FOLLOW_118_in_thread333 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_mayEmptyNumberList_in_listOrSingleNumber363 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_number_in_listOrSingleNumber374 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_107_in_mayEmptyNumberList396 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_80_in_mayEmptyNumberList402 = new BitSet(new long[]{0x0000000000000000L,0x0000000000020000L});
	public static final BitSet FOLLOW_81_in_mayEmptyNumberList404 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_80_in_mayEmptyNumberList410 = new BitSet(new long[]{0x0000000000000010L});
	public static final BitSet FOLLOW_number_in_mayEmptyNumberList414 = new BitSet(new long[]{0x0000000000000080L,0x0000000000020000L});
	public static final BitSet FOLLOW_7_in_mayEmptyNumberList419 = new BitSet(new long[]{0x0000000000000010L});
	public static final BitSet FOLLOW_number_in_mayEmptyNumberList423 = new BitSet(new long[]{0x0000000000000080L,0x0000000000020000L});
	public static final BitSet FOLLOW_81_in_mayEmptyNumberList430 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_107_in_mayEmptyStringList451 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_80_in_mayEmptyStringList457 = new BitSet(new long[]{0x0000000000000000L,0x0000000000020000L});
	public static final BitSet FOLLOW_81_in_mayEmptyStringList459 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_80_in_mayEmptyStringList465 = new BitSet(new long[]{0x0000000000000020L});
	public static final BitSet FOLLOW_string_in_mayEmptyStringList469 = new BitSet(new long[]{0x0000000000000080L,0x0000000000020000L});
	public static final BitSet FOLLOW_7_in_mayEmptyStringList474 = new BitSet(new long[]{0x0000000000000020L});
	public static final BitSet FOLLOW_string_in_mayEmptyStringList478 = new BitSet(new long[]{0x0000000000000080L,0x0000000000020000L});
	public static final BitSet FOLLOW_81_in_mayEmptyStringList485 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_107_in_context507 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_80_in_context513 = new BitSet(new long[]{0x0000000000000110L});
	public static final BitSet FOLLOW_mayNegNumber_in_context517 = new BitSet(new long[]{0x0000000000000080L,0x0000000000020000L});
	public static final BitSet FOLLOW_7_in_context522 = new BitSet(new long[]{0x0000000000000110L});
	public static final BitSet FOLLOW_mayNegNumber_in_context526 = new BitSet(new long[]{0x0000000000000080L,0x0000000000020000L});
	public static final BitSet FOLLOW_81_in_context533 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_67_in_sdg_header552 = new BitSet(new long[]{0x0000000000000022L,0x0010000000000000L});
	public static final BitSet FOLLOW_116_in_sdg_header568 = new BitSet(new long[]{0x0000000000000010L});
	public static final BitSet FOLLOW_number_in_sdg_header572 = new BitSet(new long[]{0x0000000000000022L});
	public static final BitSet FOLLOW_string_in_sdg_header593 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_node_in_node_list626 = new BitSet(new long[]{0x1080018870013002L,0x0000000000000100L});
	public static final BitSet FOLLOW_node_kind_in_node652 = new BitSet(new long[]{0x0000000000000110L});
	public static final BitSet FOLLOW_mayNegNumber_in_node656 = new BitSet(new long[]{0x0000000000000000L,0x0020000000000000L});
	public static final BitSet FOLLOW_117_in_node665 = new BitSet(new long[]{0xEF7BAC7607BEC800L,0x004000000000FAF7L});
	public static final BitSet FOLLOW_node_attributes_in_node674 = new BitSet(new long[]{0xEC43AC76073E0000L,0x00400000000050F5L});
	public static final BitSet FOLLOW_node_edges_in_node684 = new BitSet(new long[]{0x0000000000000000L,0x0040000000000000L});
	public static final BitSet FOLLOW_118_in_node691 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_55_in_node_kind710 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_60_in_node_kind718 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_30_in_node_kind726 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_28_in_node_kind734 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_16_in_node_kind742 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_12_in_node_kind750 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_13_in_node_kind758 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_39_in_node_kind766 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_40_in_node_kind774 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_29_in_node_kind782 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_72_in_node_kind790 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_35_in_node_kind798 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_node_attr_in_node_attributes817 = new BitSet(new long[]{0x0000000000000400L});
	public static final BitSet FOLLOW_10_in_node_attributes820 = new BitSet(new long[]{0x033800000080C802L,0x000000000000AA02L});
	public static final BitSet FOLLOW_65_in_node_attr838 = new BitSet(new long[]{0x0000000000000020L});
	public static final BitSet FOLLOW_node_source_in_node_attr842 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_14_in_node_attr854 = new BitSet(new long[]{0x0000000000000020L});
	public static final BitSet FOLLOW_node_bytecode_in_node_attr858 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_75_in_node_attr869 = new BitSet(new long[]{0x0000000000000010L});
	public static final BitSet FOLLOW_number_in_node_attr871 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_57_in_node_attr921 = new BitSet(new long[]{0x0000000000000010L});
	public static final BitSet FOLLOW_number_in_node_attr925 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_56_in_node_attr944 = new BitSet(new long[]{0x0000100000000000L,0x000BF7FF7FFC0000L});
	public static final BitSet FOLLOW_node_oper_in_node_attr948 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_77_in_node_attr976 = new BitSet(new long[]{0x0000000000000020L});
	public static final BitSet FOLLOW_string_in_node_attr980 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_73_in_node_attr1008 = new BitSet(new long[]{0x0000000000000020L});
	public static final BitSet FOLLOW_string_in_node_attr1012 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_79_in_node_attr1037 = new BitSet(new long[]{0x0000000000000110L});
	public static final BitSet FOLLOW_may_neg_num_set_in_node_attr1041 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_53_in_node_attr1055 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_15_in_node_attr1089 = new BitSet(new long[]{0x0000000000000020L});
	public static final BitSet FOLLOW_string_in_node_attr1093 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_11_in_node_attr1115 = new BitSet(new long[]{0x0000000000000010L});
	public static final BitSet FOLLOW_pos_num_set_in_node_attr1119 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_23_in_node_attr1137 = new BitSet(new long[]{0x0000000000000010L});
	public static final BitSet FOLLOW_pos_num_set_in_node_attr1141 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_75_in_node_attr1158 = new BitSet(new long[]{0x0000000000000020L});
	public static final BitSet FOLLOW_string_in_node_attr1162 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_51_in_node_attr1180 = new BitSet(new long[]{0x0000000000000000L,0x0000080000010000L});
	public static final BitSet FOLLOW_mayEmptyStringList_in_node_attr1184 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_52_in_node_attr1193 = new BitSet(new long[]{0x0000000000000000L,0x0000080000010000L});
	public static final BitSet FOLLOW_mayEmptyStringList_in_node_attr1197 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_number_in_pos_num_set1218 = new BitSet(new long[]{0x0000000000000082L});
	public static final BitSet FOLLOW_7_in_pos_num_set1223 = new BitSet(new long[]{0x0000000000000010L});
	public static final BitSet FOLLOW_number_in_pos_num_set1227 = new BitSet(new long[]{0x0000000000000082L});
	public static final BitSet FOLLOW_mayNegNumber_in_may_neg_num_set1253 = new BitSet(new long[]{0x0000000000000082L});
	public static final BitSet FOLLOW_7_in_may_neg_num_set1258 = new BitSet(new long[]{0x0000000000000110L});
	public static final BitSet FOLLOW_mayNegNumber_in_may_neg_num_set1262 = new BitSet(new long[]{0x0000000000000082L});
	public static final BitSet FOLLOW_string_in_node_source1288 = new BitSet(new long[]{0x0000000000000200L});
	public static final BitSet FOLLOW_9_in_node_source1290 = new BitSet(new long[]{0x0000000000000010L});
	public static final BitSet FOLLOW_number_in_node_source1294 = new BitSet(new long[]{0x0000000000000080L});
	public static final BitSet FOLLOW_7_in_node_source1296 = new BitSet(new long[]{0x0000000000000010L});
	public static final BitSet FOLLOW_number_in_node_source1300 = new BitSet(new long[]{0x0000000000000100L});
	public static final BitSet FOLLOW_8_in_node_source1302 = new BitSet(new long[]{0x0000000000000010L});
	public static final BitSet FOLLOW_number_in_node_source1306 = new BitSet(new long[]{0x0000000000000080L});
	public static final BitSet FOLLOW_7_in_node_source1308 = new BitSet(new long[]{0x0000000000000010L});
	public static final BitSet FOLLOW_number_in_node_source1312 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_string_in_node_bytecode1343 = new BitSet(new long[]{0x0000000000000200L});
	public static final BitSet FOLLOW_9_in_node_bytecode1345 = new BitSet(new long[]{0x0000000000000110L});
	public static final BitSet FOLLOW_mayNegNumber_in_node_bytecode1349 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_92_in_node_oper1370 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_101_in_node_oper1387 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_96_in_node_oper1401 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_88_in_node_oper1413 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_113_in_node_oper1426 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_100_in_node_oper1437 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_112_in_node_oper1446 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_108_in_node_oper1460 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_86_in_node_oper1474 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_115_in_node_oper1490 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_91_in_node_oper1507 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_109_in_node_oper1522 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_84_in_node_oper1539 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_111_in_node_oper1556 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_110_in_node_oper1572 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_90_in_node_oper1585 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_105_in_node_oper1596 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_104_in_node_oper1612 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_85_in_node_oper1625 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_44_in_node_oper1641 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_103_in_node_oper1661 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_102_in_node_oper1679 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_89_in_node_oper1697 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_87_in_node_oper1711 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_93_in_node_oper1729 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_94_in_node_oper1746 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_98_in_node_oper1764 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_97_in_node_oper1779 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_99_in_node_oper1791 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_82_in_node_oper1805 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_83_in_node_oper1821 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_106_in_node_oper1836 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_edge_in_node_edges1864 = new BitSet(new long[]{0x0000000000000400L});
	public static final BitSet FOLLOW_10_in_node_edges1866 = new BitSet(new long[]{0xEC43AC76073E0002L,0x00000000000050F5L});
	public static final BitSet FOLLOW_edge_kind_in_edge1891 = new BitSet(new long[]{0x0000000000000010L});
	public static final BitSet FOLLOW_number_in_edge1895 = new BitSet(new long[]{0x0000000000000202L});
	public static final BitSet FOLLOW_9_in_edge1900 = new BitSet(new long[]{0x0000000000000020L});
	public static final BitSet FOLLOW_string_in_edge1904 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_25_in_edge_kind1929 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_26_in_edge_kind1949 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_24_in_edge_kind1968 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_18_in_edge_kind1987 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_19_in_edge_kind1999 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_76_in_edge_kind2011 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_20_in_edge_kind2022 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_54_in_edge_kind2038 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_63_in_edge_kind2059 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_17_in_edge_kind2082 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_21_in_edge_kind2090 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_58_in_edge_kind2098 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_59_in_edge_kind2106 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_71_in_edge_kind2115 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_69_in_edge_kind2123 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_68_in_edge_kind2131 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_61_in_edge_kind2140 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_36_in_edge_kind2149 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_37_in_edge_kind2157 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_38_in_edge_kind2165 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_49_in_edge_kind2173 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_43_in_edge_kind2181 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_45_in_edge_kind2189 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_66_in_edge_kind2197 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_42_in_edge_kind2206 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_33_in_edge_kind2214 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_34_in_edge_kind2222 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_64_in_edge_kind2231 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_48_in_edge_kind2239 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_70_in_edge_kind2247 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_78_in_edge_kind2255 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_62_in_edge_kind2263 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_47_in_edge_kind2271 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_8_in_mayNegNumber2292 = new BitSet(new long[]{0x0000000000000010L});
	public static final BitSet FOLLOW_number_in_mayNegNumber2296 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_number_in_mayNegNumber2306 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_NUMBER_in_number2329 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_STRING_in_string2352 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_114_in_bool2373 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_95_in_bool2382 = new BitSet(new long[]{0x0000000000000002L});
}
