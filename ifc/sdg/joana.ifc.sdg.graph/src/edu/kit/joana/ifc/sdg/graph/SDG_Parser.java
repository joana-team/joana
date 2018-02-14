// $ANTLR 3.5.2 /data1/hecker/gits/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g 2018-02-14 22:01:00
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
		"'P'", "'PE'", "'PI'", "'PO'", "'PRED'", "'PS'", "'RD'", "'RF'", "'RY'", 
		"'S'", "'SD'", "'SDG'", "'SF'", "'SH'", "'SP'", "'SU'", "'SYNC'", "'T'", 
		"'Thread'", "'U'", "'UN'", "'V'", "'VD'", "'Z'", "'['", "']'", "'act-in'", 
		"'act-out'", "'array'", "'assign'", "'binary'", "'call'", "'charconst'", 
		"'compound'", "'declaration'", "'derefer'", "'empty'", "'entry'", "'exit'", 
		"'false'", "'floatconst'", "'form-ellip'", "'form-in'", "'form-out'", 
		"'functionconst'", "'intconst'", "'jump'", "'loop'", "'modassign'", "'modify'", 
		"'monitor'", "'null'", "'question'", "'refer'", "'reference'", "'root'", 
		"'select'", "'shortcut'", "'stringconst'", "'true'", "'unary'", "'v'", 
		"'{'", "'}'"
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
	public static final int T__119=119;
	public static final int T__120=120;
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
	    if (e instanceof LexerException || e instanceof ParserException) {
	      Thrower.sneakyThrow(e);
	    }
	    super.reportError(e);
	    String hdr = getErrorHeader(e);
	    String msg = getErrorMessage(e, getTokenNames());
	    Thrower.sneakyThrow(new ParserException(hdr+' '+msg, e));
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
	    private Integer rootId;
	    
	    private SDGHeader(int version, String name, Integer rootId) {
	      this.version = version;
	      this.name = name;
	      this.rootId = rootId;
	    }
	    
	    public SDG createSDG() {
	      SDG sdg = (name == null ? new SDG() : new SDG(name));
	      return sdg;
	    }
	    
	    public String toString() {
	      return "SDG of " + name + " (v" + version + ")";
	    }
	    
	    public void setRoot(SDG sdg) {
	      if (rootId != null) {
	        sdg.setRoot(sdg.getNode(rootId));
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
	          ? new LabeledSDGEdge(from, to, e.kind, e.label)
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
	// /data1/hecker/gits/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:296:1: sdg_file returns [SDG sdg] : head= sdg_header '{' ( 'JComp' )? nl= node_list (ti= thread_info[sdg] )? '}' ;
	public final SDG sdg_file() throws RecognitionException {
		SDG sdg = null;


		SDGHeader head =null;
		List<SDGNodeStub> nl =null;
		ThreadsInformation ti =null;

		try {
			// /data1/hecker/gits/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:297:3: (head= sdg_header '{' ( 'JComp' )? nl= node_list (ti= thread_info[sdg] )? '}' )
			// /data1/hecker/gits/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:297:5: head= sdg_header '{' ( 'JComp' )? nl= node_list (ti= thread_info[sdg] )? '}'
			{
			pushFollow(FOLLOW_sdg_header_in_sdg_file73);
			head=sdg_header();
			state._fsp--;

			 sdg = head.createSDG(); 
			match(input,119,FOLLOW_119_in_sdg_file89); 
			// /data1/hecker/gits/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:299:7: ( 'JComp' )?
			int alt1=2;
			int LA1_0 = input.LA(1);
			if ( (LA1_0==46) ) {
				alt1=1;
			}
			switch (alt1) {
				case 1 :
					// /data1/hecker/gits/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:299:8: 'JComp'
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
			// /data1/hecker/gits/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:301:7: (ti= thread_info[sdg] )?
			int alt2=2;
			int LA2_0 = input.LA(1);
			if ( (LA2_0==75) ) {
				alt2=1;
			}
			switch (alt2) {
				case 1 :
					// /data1/hecker/gits/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:301:8: ti= thread_info[sdg]
					{
					pushFollow(FOLLOW_thread_info_in_sdg_file150);
					ti=thread_info(sdg);
					state._fsp--;

					 sdg.setThreadsInfo(ti); 
					}
					break;

			}

			match(input,120,FOLLOW_120_in_sdg_file163); 
			 head.setRoot(sdg); 
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
	// /data1/hecker/gits/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:305:9: private thread_info[SDG sdg] returns [ThreadsInformation tinfo] : (t= thread[sdg] )+ ;
	public final ThreadsInformation thread_info(SDG sdg) throws RecognitionException {
		ThreadsInformation tinfo = null;


		ThreadInstance t =null;

		try {
			// /data1/hecker/gits/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:306:3: ( (t= thread[sdg] )+ )
			// /data1/hecker/gits/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:306:5: (t= thread[sdg] )+
			{
			 final LinkedList<ThreadInstance> tis = new LinkedList<ThreadInstance>(); 
			// /data1/hecker/gits/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:307:5: (t= thread[sdg] )+
			int cnt3=0;
			loop3:
			while (true) {
				int alt3=2;
				int LA3_0 = input.LA(1);
				if ( (LA3_0==75) ) {
					alt3=1;
				}

				switch (alt3) {
				case 1 :
					// /data1/hecker/gits/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:307:6: t= thread[sdg]
					{
					pushFollow(FOLLOW_thread_in_thread_info214);
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
	// /data1/hecker/gits/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:311:9: private thread[SDG sdg] returns [ThreadInstance ti] : 'Thread' id= number '{' 'Entry' en= number ';' 'Exit' ex= number ';' 'Fork' fo= mayNegNumber ';' 'Join' joins= listOrSingleNumber[sdg] ';' 'Context' con= context[sdg] ';' 'Dynamic' dyn= bool ';' '}' ;
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
			// /data1/hecker/gits/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:312:3: ( 'Thread' id= number '{' 'Entry' en= number ';' 'Exit' ex= number ';' 'Fork' fo= mayNegNumber ';' 'Join' joins= listOrSingleNumber[sdg] ';' 'Context' con= context[sdg] ';' 'Dynamic' dyn= bool ';' '}' )
			// /data1/hecker/gits/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:312:5: 'Thread' id= number '{' 'Entry' en= number ';' 'Exit' ex= number ';' 'Fork' fo= mayNegNumber ';' 'Join' joins= listOrSingleNumber[sdg] ';' 'Context' con= context[sdg] ';' 'Dynamic' dyn= bool ';' '}'
			{
			match(input,75,FOLLOW_75_in_thread246); 
			pushFollow(FOLLOW_number_in_thread250);
			id=number();
			state._fsp--;

			match(input,119,FOLLOW_119_in_thread252); 
			match(input,31,FOLLOW_31_in_thread260); 
			pushFollow(FOLLOW_number_in_thread266);
			en=number();
			state._fsp--;

			match(input,10,FOLLOW_10_in_thread268); 
			match(input,32,FOLLOW_32_in_thread276); 
			pushFollow(FOLLOW_number_in_thread283);
			ex=number();
			state._fsp--;

			match(input,10,FOLLOW_10_in_thread285); 
			match(input,41,FOLLOW_41_in_thread293); 
			pushFollow(FOLLOW_mayNegNumber_in_thread300);
			fo=mayNegNumber();
			state._fsp--;

			match(input,10,FOLLOW_10_in_thread302); 
			match(input,50,FOLLOW_50_in_thread310); 
			pushFollow(FOLLOW_listOrSingleNumber_in_thread317);
			joins=listOrSingleNumber(sdg);
			state._fsp--;

			match(input,10,FOLLOW_10_in_thread320); 
			match(input,22,FOLLOW_22_in_thread328); 
			pushFollow(FOLLOW_context_in_thread332);
			con=context(sdg);
			state._fsp--;

			match(input,10,FOLLOW_10_in_thread335); 
			match(input,27,FOLLOW_27_in_thread343); 
			pushFollow(FOLLOW_bool_in_thread347);
			dyn=bool();
			state._fsp--;

			match(input,10,FOLLOW_10_in_thread349); 
			match(input,120,FOLLOW_120_in_thread355); 

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
	// /data1/hecker/gits/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:328:9: private listOrSingleNumber[SDG sdg] returns [LinkedList<SDGNode> js] : (joins= mayEmptyNumberList[sdg] |jo= number );
	public final LinkedList<SDGNode> listOrSingleNumber(SDG sdg) throws RecognitionException {
		LinkedList<SDGNode> js = null;


		LinkedList<SDGNode> joins =null;
		int jo =0;

		try {
			// /data1/hecker/gits/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:329:3: (joins= mayEmptyNumberList[sdg] |jo= number )
			int alt4=2;
			int LA4_0 = input.LA(1);
			if ( (LA4_0==81||LA4_0==108) ) {
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
					// /data1/hecker/gits/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:329:5: joins= mayEmptyNumberList[sdg]
					{
					pushFollow(FOLLOW_mayEmptyNumberList_in_listOrSingleNumber385);
					joins=mayEmptyNumberList(sdg);
					state._fsp--;

					 js = joins; 
					}
					break;
				case 2 :
					// /data1/hecker/gits/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:330:5: jo= number
					{
					pushFollow(FOLLOW_number_in_listOrSingleNumber396);
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
	// /data1/hecker/gits/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:336:9: private mayEmptyNumberList[SDG sdg] returns [LinkedList<SDGNode> js = new LinkedList<SDGNode>();] : ( 'null' | '[' ']' | '[' i= number ( ',' i= number )* ']' );
	public final LinkedList<SDGNode> mayEmptyNumberList(SDG sdg) throws RecognitionException {
		LinkedList<SDGNode> js =  new LinkedList<SDGNode>();;


		int i =0;

		try {
			// /data1/hecker/gits/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:337:3: ( 'null' | '[' ']' | '[' i= number ( ',' i= number )* ']' )
			int alt6=3;
			int LA6_0 = input.LA(1);
			if ( (LA6_0==108) ) {
				alt6=1;
			}
			else if ( (LA6_0==81) ) {
				int LA6_2 = input.LA(2);
				if ( (LA6_2==82) ) {
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
					// /data1/hecker/gits/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:337:5: 'null'
					{
					match(input,108,FOLLOW_108_in_mayEmptyNumberList418); 
					}
					break;
				case 2 :
					// /data1/hecker/gits/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:338:5: '[' ']'
					{
					match(input,81,FOLLOW_81_in_mayEmptyNumberList424); 
					match(input,82,FOLLOW_82_in_mayEmptyNumberList426); 
					}
					break;
				case 3 :
					// /data1/hecker/gits/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:339:5: '[' i= number ( ',' i= number )* ']'
					{
					match(input,81,FOLLOW_81_in_mayEmptyNumberList432); 
					pushFollow(FOLLOW_number_in_mayEmptyNumberList436);
					i=number();
					state._fsp--;

					 js.add(sdg.getNode(i)); 
					// /data1/hecker/gits/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:339:46: ( ',' i= number )*
					loop5:
					while (true) {
						int alt5=2;
						int LA5_0 = input.LA(1);
						if ( (LA5_0==7) ) {
							alt5=1;
						}

						switch (alt5) {
						case 1 :
							// /data1/hecker/gits/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:339:47: ',' i= number
							{
							match(input,7,FOLLOW_7_in_mayEmptyNumberList441); 
							pushFollow(FOLLOW_number_in_mayEmptyNumberList445);
							i=number();
							state._fsp--;

							 js.add(sdg.getNode(i)); 
							}
							break;

						default :
							break loop5;
						}
					}

					match(input,82,FOLLOW_82_in_mayEmptyNumberList452); 
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
	// /data1/hecker/gits/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:342:9: private mayEmptyStringList returns [LinkedList<String> ss = new LinkedList<String>();] : ( 'null' | '[' ']' | '[' s= string ( ',' s= string )* ']' );
	public final LinkedList<String> mayEmptyStringList() throws RecognitionException {
		LinkedList<String> ss =  new LinkedList<String>();;


		String s =null;

		try {
			// /data1/hecker/gits/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:343:3: ( 'null' | '[' ']' | '[' s= string ( ',' s= string )* ']' )
			int alt8=3;
			int LA8_0 = input.LA(1);
			if ( (LA8_0==108) ) {
				alt8=1;
			}
			else if ( (LA8_0==81) ) {
				int LA8_2 = input.LA(2);
				if ( (LA8_2==82) ) {
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
					// /data1/hecker/gits/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:343:5: 'null'
					{
					match(input,108,FOLLOW_108_in_mayEmptyStringList473); 
					}
					break;
				case 2 :
					// /data1/hecker/gits/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:344:5: '[' ']'
					{
					match(input,81,FOLLOW_81_in_mayEmptyStringList479); 
					match(input,82,FOLLOW_82_in_mayEmptyStringList481); 
					}
					break;
				case 3 :
					// /data1/hecker/gits/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:345:5: '[' s= string ( ',' s= string )* ']'
					{
					match(input,81,FOLLOW_81_in_mayEmptyStringList487); 
					pushFollow(FOLLOW_string_in_mayEmptyStringList491);
					s=string();
					state._fsp--;

					 ss.add(s); 
					// /data1/hecker/gits/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:345:33: ( ',' s= string )*
					loop7:
					while (true) {
						int alt7=2;
						int LA7_0 = input.LA(1);
						if ( (LA7_0==7) ) {
							alt7=1;
						}

						switch (alt7) {
						case 1 :
							// /data1/hecker/gits/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:345:34: ',' s= string
							{
							match(input,7,FOLLOW_7_in_mayEmptyStringList496); 
							pushFollow(FOLLOW_string_in_mayEmptyStringList500);
							s=string();
							state._fsp--;

							 ss.add(s); 
							}
							break;

						default :
							break loop7;
						}
					}

					match(input,82,FOLLOW_82_in_mayEmptyStringList507); 
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
	// /data1/hecker/gits/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:348:9: private context[SDG sdg] returns [LinkedList<SDGNode> cx = new LinkedList<SDGNode>();] : ( 'null' | '[' i= mayNegNumber ( ',' i= mayNegNumber )* ']' );
	public final LinkedList<SDGNode> context(SDG sdg) throws RecognitionException {
		LinkedList<SDGNode> cx =  new LinkedList<SDGNode>();;


		int i =0;

		try {
			// /data1/hecker/gits/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:349:3: ( 'null' | '[' i= mayNegNumber ( ',' i= mayNegNumber )* ']' )
			int alt10=2;
			int LA10_0 = input.LA(1);
			if ( (LA10_0==108) ) {
				alt10=1;
			}
			else if ( (LA10_0==81) ) {
				alt10=2;
			}

			else {
				NoViableAltException nvae =
					new NoViableAltException("", 10, 0, input);
				throw nvae;
			}

			switch (alt10) {
				case 1 :
					// /data1/hecker/gits/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:349:5: 'null'
					{
					match(input,108,FOLLOW_108_in_context529); 
					}
					break;
				case 2 :
					// /data1/hecker/gits/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:350:5: '[' i= mayNegNumber ( ',' i= mayNegNumber )* ']'
					{
					match(input,81,FOLLOW_81_in_context535); 
					pushFollow(FOLLOW_mayNegNumber_in_context539);
					i=mayNegNumber();
					state._fsp--;

					 cx.add(sdg.getNode(i)); 
					// /data1/hecker/gits/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:350:52: ( ',' i= mayNegNumber )*
					loop9:
					while (true) {
						int alt9=2;
						int LA9_0 = input.LA(1);
						if ( (LA9_0==7) ) {
							alt9=1;
						}

						switch (alt9) {
						case 1 :
							// /data1/hecker/gits/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:350:53: ',' i= mayNegNumber
							{
							match(input,7,FOLLOW_7_in_context544); 
							pushFollow(FOLLOW_mayNegNumber_in_context548);
							i=mayNegNumber();
							state._fsp--;

							 cx.add(sdg.getNode(i)); 
							}
							break;

						default :
							break loop9;
						}
					}

					match(input,82,FOLLOW_82_in_context555); 
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
	// /data1/hecker/gits/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:353:9: private sdg_header returns [SDGHeader header] : 'SDG' ( 'v' n= number )? (na= string )? ( 'root' root= number )? ;
	public final SDGHeader sdg_header() throws RecognitionException {
		SDGHeader header = null;


		int n =0;
		String na =null;
		int root =0;

		try {
			// /data1/hecker/gits/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:354:3: ( 'SDG' ( 'v' n= number )? (na= string )? ( 'root' root= number )? )
			// /data1/hecker/gits/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:354:5: 'SDG' ( 'v' n= number )? (na= string )? ( 'root' root= number )?
			{
			match(input,68,FOLLOW_68_in_sdg_header574); 
			 int version = SDG.DEFAULT_VERSION; 
			// /data1/hecker/gits/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:356:7: ( 'v' n= number )?
			int alt11=2;
			int LA11_0 = input.LA(1);
			if ( (LA11_0==118) ) {
				alt11=1;
			}
			switch (alt11) {
				case 1 :
					// /data1/hecker/gits/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:356:8: 'v' n= number
					{
					match(input,118,FOLLOW_118_in_sdg_header590); 
					pushFollow(FOLLOW_number_in_sdg_header594);
					n=number();
					state._fsp--;

					 version = n; 
					}
					break;

			}

			 String name = null; 
			// /data1/hecker/gits/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:358:7: (na= string )?
			int alt12=2;
			int LA12_0 = input.LA(1);
			if ( (LA12_0==STRING) ) {
				alt12=1;
			}
			switch (alt12) {
				case 1 :
					// /data1/hecker/gits/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:358:8: na= string
					{
					pushFollow(FOLLOW_string_in_sdg_header615);
					na=string();
					state._fsp--;

					 name = na; 
					}
					break;

			}

			 Integer rootId = null; 
			// /data1/hecker/gits/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:360:7: ( 'root' root= number )?
			int alt13=2;
			int LA13_0 = input.LA(1);
			if ( (LA13_0==112) ) {
				alt13=1;
			}
			switch (alt13) {
				case 1 :
					// /data1/hecker/gits/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:360:8: 'root' root= number
					{
					match(input,112,FOLLOW_112_in_sdg_header635); 
					pushFollow(FOLLOW_number_in_sdg_header639);
					root=number();
					state._fsp--;

					 rootId = root; 
					}
					break;

			}

			 header = new SDGHeader(version, name, rootId); 
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
	// /data1/hecker/gits/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:364:9: private node_list returns [List<SDGNodeStub> list = new LinkedList<SDGNodeStub>();] : (n= node )* ;
	public final List<SDGNodeStub> node_list() throws RecognitionException {
		List<SDGNodeStub> list =  new LinkedList<SDGNodeStub>();;


		SDGNodeStub n =null;

		try {
			// /data1/hecker/gits/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:365:3: ( (n= node )* )
			// /data1/hecker/gits/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:365:5: (n= node )*
			{
			// /data1/hecker/gits/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:365:5: (n= node )*
			loop14:
			while (true) {
				int alt14=2;
				int LA14_0 = input.LA(1);
				if ( ((LA14_0 >= 12 && LA14_0 <= 13)||LA14_0==16||(LA14_0 >= 28 && LA14_0 <= 30)||LA14_0==35||(LA14_0 >= 39 && LA14_0 <= 40)||LA14_0==55||LA14_0==61||LA14_0==73) ) {
					alt14=1;
				}

				switch (alt14) {
				case 1 :
					// /data1/hecker/gits/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:365:6: n= node
					{
					pushFollow(FOLLOW_node_in_node_list672);
					n=node();
					state._fsp--;

					 list.add(n); 
					}
					break;

				default :
					break loop14;
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
	// /data1/hecker/gits/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:368:9: private node returns [SDGNodeStub nstub] : k= node_kind id= mayNegNumber '{' node_attributes[nstub] node_edges[nstub] '}' ;
	public final SDGNodeStub node() throws RecognitionException {
		SDGNodeStub nstub = null;


		SDGNode.Kind k =null;
		int id =0;

		try {
			// /data1/hecker/gits/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:369:3: (k= node_kind id= mayNegNumber '{' node_attributes[nstub] node_edges[nstub] '}' )
			// /data1/hecker/gits/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:369:5: k= node_kind id= mayNegNumber '{' node_attributes[nstub] node_edges[nstub] '}'
			{
			pushFollow(FOLLOW_node_kind_in_node698);
			k=node_kind();
			state._fsp--;

			pushFollow(FOLLOW_mayNegNumber_in_node702);
			id=mayNegNumber();
			state._fsp--;

			 nstub = new SDGNodeStub(k, id, defaultSrcPos, defaultBcPos); 
			match(input,119,FOLLOW_119_in_node711); 
			pushFollow(FOLLOW_node_attributes_in_node720);
			node_attributes(nstub);
			state._fsp--;

			pushFollow(FOLLOW_node_edges_in_node730);
			node_edges(nstub);
			state._fsp--;

			match(input,120,FOLLOW_120_in_node737); 
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
	// /data1/hecker/gits/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:376:9: private node_kind returns [SDGNode.Kind kind] : ( 'NORM' | 'PRED' | 'EXPR' | 'ENTR' | 'CALL' | 'ACTI' | 'ACTO' | 'FRMI' | 'FRMO' | 'EXIT' | 'SYNC' | 'FOLD' );
	public final SDGNode.Kind node_kind() throws RecognitionException {
		SDGNode.Kind kind = null;


		try {
			// /data1/hecker/gits/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:377:3: ( 'NORM' | 'PRED' | 'EXPR' | 'ENTR' | 'CALL' | 'ACTI' | 'ACTO' | 'FRMI' | 'FRMO' | 'EXIT' | 'SYNC' | 'FOLD' )
			int alt15=12;
			switch ( input.LA(1) ) {
			case 55:
				{
				alt15=1;
				}
				break;
			case 61:
				{
				alt15=2;
				}
				break;
			case 30:
				{
				alt15=3;
				}
				break;
			case 28:
				{
				alt15=4;
				}
				break;
			case 16:
				{
				alt15=5;
				}
				break;
			case 12:
				{
				alt15=6;
				}
				break;
			case 13:
				{
				alt15=7;
				}
				break;
			case 39:
				{
				alt15=8;
				}
				break;
			case 40:
				{
				alt15=9;
				}
				break;
			case 29:
				{
				alt15=10;
				}
				break;
			case 73:
				{
				alt15=11;
				}
				break;
			case 35:
				{
				alt15=12;
				}
				break;
			default:
				NoViableAltException nvae =
					new NoViableAltException("", 15, 0, input);
				throw nvae;
			}
			switch (alt15) {
				case 1 :
					// /data1/hecker/gits/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:377:5: 'NORM'
					{
					match(input,55,FOLLOW_55_in_node_kind756); 
					 kind = SDGNode.Kind.NORMAL; 
					}
					break;
				case 2 :
					// /data1/hecker/gits/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:378:5: 'PRED'
					{
					match(input,61,FOLLOW_61_in_node_kind764); 
					 kind = SDGNode.Kind.PREDICATE; 
					}
					break;
				case 3 :
					// /data1/hecker/gits/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:379:5: 'EXPR'
					{
					match(input,30,FOLLOW_30_in_node_kind772); 
					 kind = SDGNode.Kind.EXPRESSION; 
					}
					break;
				case 4 :
					// /data1/hecker/gits/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:380:5: 'ENTR'
					{
					match(input,28,FOLLOW_28_in_node_kind780); 
					 kind = SDGNode.Kind.ENTRY; 
					}
					break;
				case 5 :
					// /data1/hecker/gits/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:381:5: 'CALL'
					{
					match(input,16,FOLLOW_16_in_node_kind788); 
					 kind = SDGNode.Kind.CALL; 
					}
					break;
				case 6 :
					// /data1/hecker/gits/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:382:5: 'ACTI'
					{
					match(input,12,FOLLOW_12_in_node_kind796); 
					 kind = SDGNode.Kind.ACTUAL_IN; 
					}
					break;
				case 7 :
					// /data1/hecker/gits/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:383:5: 'ACTO'
					{
					match(input,13,FOLLOW_13_in_node_kind804); 
					 kind = SDGNode.Kind.ACTUAL_OUT; 
					}
					break;
				case 8 :
					// /data1/hecker/gits/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:384:5: 'FRMI'
					{
					match(input,39,FOLLOW_39_in_node_kind812); 
					 kind = SDGNode.Kind.FORMAL_IN; 
					}
					break;
				case 9 :
					// /data1/hecker/gits/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:385:5: 'FRMO'
					{
					match(input,40,FOLLOW_40_in_node_kind820); 
					 kind = SDGNode.Kind.FORMAL_OUT; 
					}
					break;
				case 10 :
					// /data1/hecker/gits/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:386:5: 'EXIT'
					{
					match(input,29,FOLLOW_29_in_node_kind828); 
					 kind = SDGNode.Kind.EXIT; 
					}
					break;
				case 11 :
					// /data1/hecker/gits/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:387:5: 'SYNC'
					{
					match(input,73,FOLLOW_73_in_node_kind836); 
					 kind = SDGNode.Kind.SYNCHRONIZATION; 
					}
					break;
				case 12 :
					// /data1/hecker/gits/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:388:5: 'FOLD'
					{
					match(input,35,FOLLOW_35_in_node_kind844); 
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
	// /data1/hecker/gits/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:391:9: private node_attributes[SDGNodeStub node] : ( node_attr[node] ';' )* ;
	public final void node_attributes(SDGNodeStub node) throws RecognitionException {
		try {
			// /data1/hecker/gits/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:392:3: ( ( node_attr[node] ';' )* )
			// /data1/hecker/gits/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:392:5: ( node_attr[node] ';' )*
			{
			// /data1/hecker/gits/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:392:5: ( node_attr[node] ';' )*
			loop16:
			while (true) {
				int alt16=2;
				int LA16_0 = input.LA(1);
				if ( (LA16_0==11||(LA16_0 >= 14 && LA16_0 <= 15)||LA16_0==23||(LA16_0 >= 51 && LA16_0 <= 53)||(LA16_0 >= 56 && LA16_0 <= 57)||LA16_0==66||LA16_0==74||LA16_0==76||LA16_0==78||LA16_0==80) ) {
					alt16=1;
				}

				switch (alt16) {
				case 1 :
					// /data1/hecker/gits/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:392:6: node_attr[node] ';'
					{
					pushFollow(FOLLOW_node_attr_in_node_attributes863);
					node_attr(node);
					state._fsp--;

					match(input,10,FOLLOW_10_in_node_attributes866); 
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
	}
	// $ANTLR end "node_attributes"



	// $ANTLR start "node_attr"
	// /data1/hecker/gits/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:395:9: private node_attr[SDGNodeStub node] : ( 'S' spos= node_source | 'B' bpos= node_bytecode | 'U' number | 'P' procId= number | 'O' op= node_oper | 'V' val= string | 'T' type= string | 'Z' tn= may_neg_num_set | 'N' | 'C' cl= string | 'A' al= pos_num_set | 'D' ds= pos_num_set | 'U' uct= string | 'LD' ldefs= mayEmptyStringList | 'LU' luses= mayEmptyStringList );
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
			// /data1/hecker/gits/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:396:3: ( 'S' spos= node_source | 'B' bpos= node_bytecode | 'U' number | 'P' procId= number | 'O' op= node_oper | 'V' val= string | 'T' type= string | 'Z' tn= may_neg_num_set | 'N' | 'C' cl= string | 'A' al= pos_num_set | 'D' ds= pos_num_set | 'U' uct= string | 'LD' ldefs= mayEmptyStringList | 'LU' luses= mayEmptyStringList )
			int alt17=15;
			switch ( input.LA(1) ) {
			case 66:
				{
				alt17=1;
				}
				break;
			case 14:
				{
				alt17=2;
				}
				break;
			case 76:
				{
				int LA17_3 = input.LA(2);
				if ( (LA17_3==NUMBER) ) {
					alt17=3;
				}
				else if ( (LA17_3==STRING) ) {
					alt17=13;
				}

				else {
					int nvaeMark = input.mark();
					try {
						input.consume();
						NoViableAltException nvae =
							new NoViableAltException("", 17, 3, input);
						throw nvae;
					} finally {
						input.rewind(nvaeMark);
					}
				}

				}
				break;
			case 57:
				{
				alt17=4;
				}
				break;
			case 56:
				{
				alt17=5;
				}
				break;
			case 78:
				{
				alt17=6;
				}
				break;
			case 74:
				{
				alt17=7;
				}
				break;
			case 80:
				{
				alt17=8;
				}
				break;
			case 53:
				{
				alt17=9;
				}
				break;
			case 15:
				{
				alt17=10;
				}
				break;
			case 11:
				{
				alt17=11;
				}
				break;
			case 23:
				{
				alt17=12;
				}
				break;
			case 51:
				{
				alt17=14;
				}
				break;
			case 52:
				{
				alt17=15;
				}
				break;
			default:
				NoViableAltException nvae =
					new NoViableAltException("", 17, 0, input);
				throw nvae;
			}
			switch (alt17) {
				case 1 :
					// /data1/hecker/gits/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:396:5: 'S' spos= node_source
					{
					match(input,66,FOLLOW_66_in_node_attr884); 
					pushFollow(FOLLOW_node_source_in_node_attr888);
					spos=node_source();
					state._fsp--;

					 node.spos = spos; defaultSrcPos = spos; 
					}
					break;
				case 2 :
					// /data1/hecker/gits/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:397:5: 'B' bpos= node_bytecode
					{
					match(input,14,FOLLOW_14_in_node_attr900); 
					pushFollow(FOLLOW_node_bytecode_in_node_attr904);
					bpos=node_bytecode();
					state._fsp--;

					 node.bpos = bpos; defaultBcPos = bpos; 
					}
					break;
				case 3 :
					// /data1/hecker/gits/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:398:5: 'U' number
					{
					match(input,76,FOLLOW_76_in_node_attr915); 
					pushFollow(FOLLOW_number_in_node_attr917);
					number();
					state._fsp--;

					}
					break;
				case 4 :
					// /data1/hecker/gits/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:399:5: 'P' procId= number
					{
					match(input,57,FOLLOW_57_in_node_attr967); 
					pushFollow(FOLLOW_number_in_node_attr971);
					procId=number();
					state._fsp--;

					 node.procId = procId; 
					}
					break;
				case 5 :
					// /data1/hecker/gits/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:400:5: 'O' op= node_oper
					{
					match(input,56,FOLLOW_56_in_node_attr990); 
					pushFollow(FOLLOW_node_oper_in_node_attr994);
					op=node_oper();
					state._fsp--;

					 node.op = op; 
					}
					break;
				case 6 :
					// /data1/hecker/gits/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:401:5: 'V' val= string
					{
					match(input,78,FOLLOW_78_in_node_attr1022); 
					pushFollow(FOLLOW_string_in_node_attr1026);
					val=string();
					state._fsp--;

					 node.val = val; 
					}
					break;
				case 7 :
					// /data1/hecker/gits/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:402:5: 'T' type= string
					{
					match(input,74,FOLLOW_74_in_node_attr1054); 
					pushFollow(FOLLOW_string_in_node_attr1058);
					type=string();
					state._fsp--;

					 node.type = type; 
					}
					break;
				case 8 :
					// /data1/hecker/gits/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:403:5: 'Z' tn= may_neg_num_set
					{
					match(input,80,FOLLOW_80_in_node_attr1083); 
					pushFollow(FOLLOW_may_neg_num_set_in_node_attr1087);
					tn=may_neg_num_set();
					state._fsp--;

					 node.threadNums = tn; 
					}
					break;
				case 9 :
					// /data1/hecker/gits/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:404:5: 'N'
					{
					match(input,53,FOLLOW_53_in_node_attr1101); 
					 node.nonTerm = true; 
					}
					break;
				case 10 :
					// /data1/hecker/gits/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:405:5: 'C' cl= string
					{
					match(input,15,FOLLOW_15_in_node_attr1135); 
					pushFollow(FOLLOW_string_in_node_attr1139);
					cl=string();
					state._fsp--;

					 node.classLoader = cl; 
					}
					break;
				case 11 :
					// /data1/hecker/gits/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:406:5: 'A' al= pos_num_set
					{
					match(input,11,FOLLOW_11_in_node_attr1161); 
					pushFollow(FOLLOW_pos_num_set_in_node_attr1165);
					al=pos_num_set();
					state._fsp--;

					 node.allocSites = al; 
					}
					break;
				case 12 :
					// /data1/hecker/gits/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:407:5: 'D' ds= pos_num_set
					{
					match(input,23,FOLLOW_23_in_node_attr1183); 
					pushFollow(FOLLOW_pos_num_set_in_node_attr1187);
					ds=pos_num_set();
					state._fsp--;

					 node.aliasDataSrc = ds; 
					}
					break;
				case 13 :
					// /data1/hecker/gits/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:408:5: 'U' uct= string
					{
					match(input,76,FOLLOW_76_in_node_attr1204); 
					pushFollow(FOLLOW_string_in_node_attr1208);
					uct=string();
					state._fsp--;

					 node.unresolvedCallTarget = uct; 
					}
					break;
				case 14 :
					// /data1/hecker/gits/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:409:5: 'LD' ldefs= mayEmptyStringList
					{
					match(input,51,FOLLOW_51_in_node_attr1226); 
					pushFollow(FOLLOW_mayEmptyStringList_in_node_attr1230);
					ldefs=mayEmptyStringList();
					state._fsp--;


					                              node.localDefNames = ldefs;
					                            
					}
					break;
				case 15 :
					// /data1/hecker/gits/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:412:5: 'LU' luses= mayEmptyStringList
					{
					match(input,52,FOLLOW_52_in_node_attr1239); 
					pushFollow(FOLLOW_mayEmptyStringList_in_node_attr1243);
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
	// /data1/hecker/gits/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:416:9: private pos_num_set returns [TIntSet nums = new TIntHashSet();] : n= number ( ',' n2= number )* ;
	public final TIntSet pos_num_set() throws RecognitionException {
		TIntSet nums =  new TIntHashSet();;


		int n =0;
		int n2 =0;

		try {
			// /data1/hecker/gits/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:417:3: (n= number ( ',' n2= number )* )
			// /data1/hecker/gits/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:417:5: n= number ( ',' n2= number )*
			{
			pushFollow(FOLLOW_number_in_pos_num_set1264);
			n=number();
			state._fsp--;

			 nums.add(n); 
			// /data1/hecker/gits/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:417:31: ( ',' n2= number )*
			loop18:
			while (true) {
				int alt18=2;
				int LA18_0 = input.LA(1);
				if ( (LA18_0==7) ) {
					alt18=1;
				}

				switch (alt18) {
				case 1 :
					// /data1/hecker/gits/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:417:32: ',' n2= number
					{
					match(input,7,FOLLOW_7_in_pos_num_set1269); 
					pushFollow(FOLLOW_number_in_pos_num_set1273);
					n2=number();
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
	// $ANTLR end "pos_num_set"



	// $ANTLR start "may_neg_num_set"
	// /data1/hecker/gits/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:420:9: private may_neg_num_set returns [TIntSet nums = new TIntHashSet();] : n= mayNegNumber ( ',' n2= mayNegNumber )* ;
	public final TIntSet may_neg_num_set() throws RecognitionException {
		TIntSet nums =  new TIntHashSet();;


		int n =0;
		int n2 =0;

		try {
			// /data1/hecker/gits/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:421:3: (n= mayNegNumber ( ',' n2= mayNegNumber )* )
			// /data1/hecker/gits/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:421:5: n= mayNegNumber ( ',' n2= mayNegNumber )*
			{
			pushFollow(FOLLOW_mayNegNumber_in_may_neg_num_set1299);
			n=mayNegNumber();
			state._fsp--;

			 nums.add(n); 
			// /data1/hecker/gits/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:421:37: ( ',' n2= mayNegNumber )*
			loop19:
			while (true) {
				int alt19=2;
				int LA19_0 = input.LA(1);
				if ( (LA19_0==7) ) {
					alt19=1;
				}

				switch (alt19) {
				case 1 :
					// /data1/hecker/gits/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:421:38: ',' n2= mayNegNumber
					{
					match(input,7,FOLLOW_7_in_may_neg_num_set1304); 
					pushFollow(FOLLOW_mayNegNumber_in_may_neg_num_set1308);
					n2=mayNegNumber();
					state._fsp--;

					 nums.add(n2); 
					}
					break;

				default :
					break loop19;
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
	// /data1/hecker/gits/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:424:9: private node_source returns [SourcePos spos] : filename= string ':' startRow= number ',' startColumn= number '-' endRow= number ',' endColumn= number ;
	public final SourcePos node_source() throws RecognitionException {
		SourcePos spos = null;


		String filename =null;
		int startRow =0;
		int startColumn =0;
		int endRow =0;
		int endColumn =0;

		try {
			// /data1/hecker/gits/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:425:3: (filename= string ':' startRow= number ',' startColumn= number '-' endRow= number ',' endColumn= number )
			// /data1/hecker/gits/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:425:5: filename= string ':' startRow= number ',' startColumn= number '-' endRow= number ',' endColumn= number
			{
			pushFollow(FOLLOW_string_in_node_source1334);
			filename=string();
			state._fsp--;

			match(input,9,FOLLOW_9_in_node_source1336); 
			pushFollow(FOLLOW_number_in_node_source1340);
			startRow=number();
			state._fsp--;

			match(input,7,FOLLOW_7_in_node_source1342); 
			pushFollow(FOLLOW_number_in_node_source1346);
			startColumn=number();
			state._fsp--;

			match(input,8,FOLLOW_8_in_node_source1348); 
			pushFollow(FOLLOW_number_in_node_source1352);
			endRow=number();
			state._fsp--;

			match(input,7,FOLLOW_7_in_node_source1354); 
			pushFollow(FOLLOW_number_in_node_source1358);
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
	// /data1/hecker/gits/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:429:9: private node_bytecode returns [ByteCodePos bpos] : name= string ':' index= mayNegNumber ;
	public final ByteCodePos node_bytecode() throws RecognitionException {
		ByteCodePos bpos = null;


		String name =null;
		int index =0;

		try {
			// /data1/hecker/gits/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:430:3: (name= string ':' index= mayNegNumber )
			// /data1/hecker/gits/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:430:5: name= string ':' index= mayNegNumber
			{
			pushFollow(FOLLOW_string_in_node_bytecode1389);
			name=string();
			state._fsp--;

			match(input,9,FOLLOW_9_in_node_bytecode1391); 
			pushFollow(FOLLOW_mayNegNumber_in_node_bytecode1395);
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
	// /data1/hecker/gits/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:433:9: private node_oper returns [SDGNode.Operation op] : ( 'empty' | 'intconst' | 'floatconst' | 'charconst' | 'stringconst' | 'functionconst' | 'shortcut' | 'question' | 'binary' | 'unary' | 'derefer' | 'refer' | 'array' | 'select' | 'reference' | 'declaration' | 'modify' | 'modassign' | 'assign' | 'IF' | 'loop' | 'jump' | 'compound' | 'call' | 'entry' | 'exit' | 'form-in' | 'form-ellip' | 'form-out' | 'act-in' | 'act-out' | 'monitor' );
	public final SDGNode.Operation node_oper() throws RecognitionException {
		SDGNode.Operation op = null;


		try {
			// /data1/hecker/gits/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:434:3: ( 'empty' | 'intconst' | 'floatconst' | 'charconst' | 'stringconst' | 'functionconst' | 'shortcut' | 'question' | 'binary' | 'unary' | 'derefer' | 'refer' | 'array' | 'select' | 'reference' | 'declaration' | 'modify' | 'modassign' | 'assign' | 'IF' | 'loop' | 'jump' | 'compound' | 'call' | 'entry' | 'exit' | 'form-in' | 'form-ellip' | 'form-out' | 'act-in' | 'act-out' | 'monitor' )
			int alt20=32;
			switch ( input.LA(1) ) {
			case 93:
				{
				alt20=1;
				}
				break;
			case 102:
				{
				alt20=2;
				}
				break;
			case 97:
				{
				alt20=3;
				}
				break;
			case 89:
				{
				alt20=4;
				}
				break;
			case 115:
				{
				alt20=5;
				}
				break;
			case 101:
				{
				alt20=6;
				}
				break;
			case 114:
				{
				alt20=7;
				}
				break;
			case 109:
				{
				alt20=8;
				}
				break;
			case 87:
				{
				alt20=9;
				}
				break;
			case 117:
				{
				alt20=10;
				}
				break;
			case 92:
				{
				alt20=11;
				}
				break;
			case 110:
				{
				alt20=12;
				}
				break;
			case 85:
				{
				alt20=13;
				}
				break;
			case 113:
				{
				alt20=14;
				}
				break;
			case 111:
				{
				alt20=15;
				}
				break;
			case 91:
				{
				alt20=16;
				}
				break;
			case 106:
				{
				alt20=17;
				}
				break;
			case 105:
				{
				alt20=18;
				}
				break;
			case 86:
				{
				alt20=19;
				}
				break;
			case 44:
				{
				alt20=20;
				}
				break;
			case 104:
				{
				alt20=21;
				}
				break;
			case 103:
				{
				alt20=22;
				}
				break;
			case 90:
				{
				alt20=23;
				}
				break;
			case 88:
				{
				alt20=24;
				}
				break;
			case 94:
				{
				alt20=25;
				}
				break;
			case 95:
				{
				alt20=26;
				}
				break;
			case 99:
				{
				alt20=27;
				}
				break;
			case 98:
				{
				alt20=28;
				}
				break;
			case 100:
				{
				alt20=29;
				}
				break;
			case 83:
				{
				alt20=30;
				}
				break;
			case 84:
				{
				alt20=31;
				}
				break;
			case 107:
				{
				alt20=32;
				}
				break;
			default:
				NoViableAltException nvae =
					new NoViableAltException("", 20, 0, input);
				throw nvae;
			}
			switch (alt20) {
				case 1 :
					// /data1/hecker/gits/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:434:5: 'empty'
					{
					match(input,93,FOLLOW_93_in_node_oper1416); 
					 op = SDGNode.Operation.EMPTY; 
					}
					break;
				case 2 :
					// /data1/hecker/gits/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:435:5: 'intconst'
					{
					match(input,102,FOLLOW_102_in_node_oper1433); 
					 op = SDGNode.Operation.INT_CONST; 
					}
					break;
				case 3 :
					// /data1/hecker/gits/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:436:5: 'floatconst'
					{
					match(input,97,FOLLOW_97_in_node_oper1447); 
					 op = SDGNode.Operation.FLOAT_CONST; 
					}
					break;
				case 4 :
					// /data1/hecker/gits/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:437:5: 'charconst'
					{
					match(input,89,FOLLOW_89_in_node_oper1459); 
					 op = SDGNode.Operation.CHAR_CONST; 
					}
					break;
				case 5 :
					// /data1/hecker/gits/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:438:5: 'stringconst'
					{
					match(input,115,FOLLOW_115_in_node_oper1472); 
					 op = SDGNode.Operation.STRING_CONST; 
					}
					break;
				case 6 :
					// /data1/hecker/gits/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:439:5: 'functionconst'
					{
					match(input,101,FOLLOW_101_in_node_oper1483); 
					 op = SDGNode.Operation.FUNCTION_CONST; 
					}
					break;
				case 7 :
					// /data1/hecker/gits/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:440:5: 'shortcut'
					{
					match(input,114,FOLLOW_114_in_node_oper1492); 
					 op = SDGNode.Operation.SHORTCUT; 
					}
					break;
				case 8 :
					// /data1/hecker/gits/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:441:5: 'question'
					{
					match(input,109,FOLLOW_109_in_node_oper1506); 
					 op = SDGNode.Operation.QUESTION; 
					}
					break;
				case 9 :
					// /data1/hecker/gits/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:442:5: 'binary'
					{
					match(input,87,FOLLOW_87_in_node_oper1520); 
					 op = SDGNode.Operation.BINARY; 
					}
					break;
				case 10 :
					// /data1/hecker/gits/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:443:5: 'unary'
					{
					match(input,117,FOLLOW_117_in_node_oper1536); 
					 op = SDGNode.Operation.UNARY; 
					}
					break;
				case 11 :
					// /data1/hecker/gits/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:444:5: 'derefer'
					{
					match(input,92,FOLLOW_92_in_node_oper1553); 
					 op = SDGNode.Operation.DEREFER; 
					}
					break;
				case 12 :
					// /data1/hecker/gits/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:445:5: 'refer'
					{
					match(input,110,FOLLOW_110_in_node_oper1568); 
					 op = SDGNode.Operation.REFER; 
					}
					break;
				case 13 :
					// /data1/hecker/gits/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:446:5: 'array'
					{
					match(input,85,FOLLOW_85_in_node_oper1585); 
					 op = SDGNode.Operation.ARRAY; 
					}
					break;
				case 14 :
					// /data1/hecker/gits/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:447:5: 'select'
					{
					match(input,113,FOLLOW_113_in_node_oper1602); 
					 op = SDGNode.Operation.SELECT; 
					}
					break;
				case 15 :
					// /data1/hecker/gits/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:448:5: 'reference'
					{
					match(input,111,FOLLOW_111_in_node_oper1618); 
					 op = SDGNode.Operation.REFERENCE; 
					}
					break;
				case 16 :
					// /data1/hecker/gits/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:449:5: 'declaration'
					{
					match(input,91,FOLLOW_91_in_node_oper1631); 
					 op = SDGNode.Operation.DECLARATION; 
					}
					break;
				case 17 :
					// /data1/hecker/gits/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:450:5: 'modify'
					{
					match(input,106,FOLLOW_106_in_node_oper1642); 
					 op = SDGNode.Operation.MODIFY; 
					}
					break;
				case 18 :
					// /data1/hecker/gits/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:451:5: 'modassign'
					{
					match(input,105,FOLLOW_105_in_node_oper1658); 
					 op = SDGNode.Operation.MODASSIGN; 
					}
					break;
				case 19 :
					// /data1/hecker/gits/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:452:5: 'assign'
					{
					match(input,86,FOLLOW_86_in_node_oper1671); 
					 op = SDGNode.Operation.ASSIGN; 
					}
					break;
				case 20 :
					// /data1/hecker/gits/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:453:5: 'IF'
					{
					match(input,44,FOLLOW_44_in_node_oper1687); 
					 op = SDGNode.Operation.IF; 
					}
					break;
				case 21 :
					// /data1/hecker/gits/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:454:5: 'loop'
					{
					match(input,104,FOLLOW_104_in_node_oper1707); 
					 op = SDGNode.Operation.LOOP; 
					}
					break;
				case 22 :
					// /data1/hecker/gits/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:455:5: 'jump'
					{
					match(input,103,FOLLOW_103_in_node_oper1725); 
					 op = SDGNode.Operation.JUMP; 
					}
					break;
				case 23 :
					// /data1/hecker/gits/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:456:5: 'compound'
					{
					match(input,90,FOLLOW_90_in_node_oper1743); 
					 op = SDGNode.Operation.COMPOUND; 
					}
					break;
				case 24 :
					// /data1/hecker/gits/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:457:5: 'call'
					{
					match(input,88,FOLLOW_88_in_node_oper1757); 
					 op = SDGNode.Operation.CALL; 
					}
					break;
				case 25 :
					// /data1/hecker/gits/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:458:5: 'entry'
					{
					match(input,94,FOLLOW_94_in_node_oper1775); 
					 op = SDGNode.Operation.ENTRY; 
					}
					break;
				case 26 :
					// /data1/hecker/gits/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:459:5: 'exit'
					{
					match(input,95,FOLLOW_95_in_node_oper1792); 
					 op = SDGNode.Operation.EXIT; 
					}
					break;
				case 27 :
					// /data1/hecker/gits/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:460:5: 'form-in'
					{
					match(input,99,FOLLOW_99_in_node_oper1810); 
					 op = SDGNode.Operation.FORMAL_IN; 
					}
					break;
				case 28 :
					// /data1/hecker/gits/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:461:5: 'form-ellip'
					{
					match(input,98,FOLLOW_98_in_node_oper1825); 
					 op = SDGNode.Operation.FORMAL_ELLIP; 
					}
					break;
				case 29 :
					// /data1/hecker/gits/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:462:5: 'form-out'
					{
					match(input,100,FOLLOW_100_in_node_oper1837); 
					 op = SDGNode.Operation.FORMAL_OUT; 
					}
					break;
				case 30 :
					// /data1/hecker/gits/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:463:5: 'act-in'
					{
					match(input,83,FOLLOW_83_in_node_oper1851); 
					 op = SDGNode.Operation.ACTUAL_IN; 
					}
					break;
				case 31 :
					// /data1/hecker/gits/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:464:5: 'act-out'
					{
					match(input,84,FOLLOW_84_in_node_oper1867); 
					 op = SDGNode.Operation.ACTUAL_OUT; 
					}
					break;
				case 32 :
					// /data1/hecker/gits/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:465:5: 'monitor'
					{
					match(input,107,FOLLOW_107_in_node_oper1882); 
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
	// /data1/hecker/gits/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:468:9: private node_edges[SDGNodeStub node] : (e= edge ';' )* ;
	public final void node_edges(SDGNodeStub node) throws RecognitionException {
		SDGEdgeStub e =null;

		try {
			// /data1/hecker/gits/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:469:3: ( (e= edge ';' )* )
			// /data1/hecker/gits/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:469:5: (e= edge ';' )*
			{
			// /data1/hecker/gits/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:469:5: (e= edge ';' )*
			loop21:
			while (true) {
				int alt21=2;
				int LA21_0 = input.LA(1);
				if ( ((LA21_0 >= 17 && LA21_0 <= 21)||(LA21_0 >= 24 && LA21_0 <= 26)||(LA21_0 >= 33 && LA21_0 <= 34)||(LA21_0 >= 36 && LA21_0 <= 38)||(LA21_0 >= 42 && LA21_0 <= 43)||LA21_0==45||(LA21_0 >= 47 && LA21_0 <= 49)||LA21_0==54||(LA21_0 >= 58 && LA21_0 <= 60)||(LA21_0 >= 62 && LA21_0 <= 65)||LA21_0==67||(LA21_0 >= 69 && LA21_0 <= 72)||LA21_0==77||LA21_0==79) ) {
					alt21=1;
				}

				switch (alt21) {
				case 1 :
					// /data1/hecker/gits/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:469:6: e= edge ';'
					{
					pushFollow(FOLLOW_edge_in_node_edges1910);
					e=edge();
					state._fsp--;

					match(input,10,FOLLOW_10_in_node_edges1912); 
					 node.edges.add(e); 
					}
					break;

				default :
					break loop21;
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
	// /data1/hecker/gits/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:472:9: private edge returns [SDGEdgeStub estub] : k= edge_kind nr= number ( ':' label= string )? ;
	public final SDGEdgeStub edge() throws RecognitionException {
		SDGEdgeStub estub = null;


		SDGEdge.Kind k =null;
		int nr =0;
		String label =null;

		try {
			// /data1/hecker/gits/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:473:3: (k= edge_kind nr= number ( ':' label= string )? )
			// /data1/hecker/gits/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:473:5: k= edge_kind nr= number ( ':' label= string )?
			{
			pushFollow(FOLLOW_edge_kind_in_edge1937);
			k=edge_kind();
			state._fsp--;

			pushFollow(FOLLOW_number_in_edge1941);
			nr=number();
			state._fsp--;

			 estub = new SDGEdgeStub(k, nr); 
			// /data1/hecker/gits/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:473:63: ( ':' label= string )?
			int alt22=2;
			int LA22_0 = input.LA(1);
			if ( (LA22_0==9) ) {
				alt22=1;
			}
			switch (alt22) {
				case 1 :
					// /data1/hecker/gits/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:473:64: ':' label= string
					{
					match(input,9,FOLLOW_9_in_edge1946); 
					pushFollow(FOLLOW_string_in_edge1950);
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
	// /data1/hecker/gits/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:476:9: private edge_kind returns [SDGEdge.Kind kind] : ( 'DD' | 'DH' | 'DA' | 'CD' | 'CE' | 'UN' | 'CF' | 'NF' | 'RF' | 'CC' | 'CL' | 'PI' | 'PO' | 'SU' | 'SH' | 'SF' | 'PS' | 'PE' | 'FORK' | 'FORK_IN' | 'FORK_OUT' | 'JOIN' | 'ID' | 'IW' | 'SD' | 'HE' | 'FD' | 'FI' | 'RY' | 'JF' | 'SP' | 'VD' | 'RD' | 'JD' );
	public final SDGEdge.Kind edge_kind() throws RecognitionException {
		SDGEdge.Kind kind = null;


		try {
			// /data1/hecker/gits/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:478:3: ( 'DD' | 'DH' | 'DA' | 'CD' | 'CE' | 'UN' | 'CF' | 'NF' | 'RF' | 'CC' | 'CL' | 'PI' | 'PO' | 'SU' | 'SH' | 'SF' | 'PS' | 'PE' | 'FORK' | 'FORK_IN' | 'FORK_OUT' | 'JOIN' | 'ID' | 'IW' | 'SD' | 'HE' | 'FD' | 'FI' | 'RY' | 'JF' | 'SP' | 'VD' | 'RD' | 'JD' )
			int alt23=34;
			switch ( input.LA(1) ) {
			case 25:
				{
				alt23=1;
				}
				break;
			case 26:
				{
				alt23=2;
				}
				break;
			case 24:
				{
				alt23=3;
				}
				break;
			case 18:
				{
				alt23=4;
				}
				break;
			case 19:
				{
				alt23=5;
				}
				break;
			case 77:
				{
				alt23=6;
				}
				break;
			case 20:
				{
				alt23=7;
				}
				break;
			case 54:
				{
				alt23=8;
				}
				break;
			case 64:
				{
				alt23=9;
				}
				break;
			case 17:
				{
				alt23=10;
				}
				break;
			case 21:
				{
				alt23=11;
				}
				break;
			case 59:
				{
				alt23=12;
				}
				break;
			case 60:
				{
				alt23=13;
				}
				break;
			case 72:
				{
				alt23=14;
				}
				break;
			case 70:
				{
				alt23=15;
				}
				break;
			case 69:
				{
				alt23=16;
				}
				break;
			case 62:
				{
				alt23=17;
				}
				break;
			case 58:
				{
				alt23=18;
				}
				break;
			case 36:
				{
				alt23=19;
				}
				break;
			case 37:
				{
				alt23=20;
				}
				break;
			case 38:
				{
				alt23=21;
				}
				break;
			case 49:
				{
				alt23=22;
				}
				break;
			case 43:
				{
				alt23=23;
				}
				break;
			case 45:
				{
				alt23=24;
				}
				break;
			case 67:
				{
				alt23=25;
				}
				break;
			case 42:
				{
				alt23=26;
				}
				break;
			case 33:
				{
				alt23=27;
				}
				break;
			case 34:
				{
				alt23=28;
				}
				break;
			case 65:
				{
				alt23=29;
				}
				break;
			case 48:
				{
				alt23=30;
				}
				break;
			case 71:
				{
				alt23=31;
				}
				break;
			case 79:
				{
				alt23=32;
				}
				break;
			case 63:
				{
				alt23=33;
				}
				break;
			case 47:
				{
				alt23=34;
				}
				break;
			default:
				NoViableAltException nvae =
					new NoViableAltException("", 23, 0, input);
				throw nvae;
			}
			switch (alt23) {
				case 1 :
					// /data1/hecker/gits/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:478:5: 'DD'
					{
					match(input,25,FOLLOW_25_in_edge_kind1975); 
					 kind = SDGEdge.Kind.DATA_DEP; 
					}
					break;
				case 2 :
					// /data1/hecker/gits/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:479:5: 'DH'
					{
					match(input,26,FOLLOW_26_in_edge_kind1995); 
					 kind = SDGEdge.Kind.DATA_HEAP; 
					}
					break;
				case 3 :
					// /data1/hecker/gits/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:480:5: 'DA'
					{
					match(input,24,FOLLOW_24_in_edge_kind2014); 
					 kind = SDGEdge.Kind.DATA_ALIAS; 
					}
					break;
				case 4 :
					// /data1/hecker/gits/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:482:5: 'CD'
					{
					match(input,18,FOLLOW_18_in_edge_kind2033); 
					 kind = SDGEdge.Kind.CONTROL_DEP_COND; 
					}
					break;
				case 5 :
					// /data1/hecker/gits/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:483:5: 'CE'
					{
					match(input,19,FOLLOW_19_in_edge_kind2045); 
					 kind = SDGEdge.Kind.CONTROL_DEP_EXPR; 
					}
					break;
				case 6 :
					// /data1/hecker/gits/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:484:5: 'UN'
					{
					match(input,77,FOLLOW_77_in_edge_kind2057); 
					 kind = SDGEdge.Kind.CONTROL_DEP_UNCOND; 
					}
					break;
				case 7 :
					// /data1/hecker/gits/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:486:5: 'CF'
					{
					match(input,20,FOLLOW_20_in_edge_kind2068); 
					 kind = SDGEdge.Kind.CONTROL_FLOW; 
					}
					break;
				case 8 :
					// /data1/hecker/gits/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:487:5: 'NF'
					{
					match(input,54,FOLLOW_54_in_edge_kind2084); 
					 kind = SDGEdge.Kind.NO_FLOW; 
					}
					break;
				case 9 :
					// /data1/hecker/gits/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:488:5: 'RF'
					{
					match(input,64,FOLLOW_64_in_edge_kind2105); 
					 kind = SDGEdge.Kind.RETURN; 
					}
					break;
				case 10 :
					// /data1/hecker/gits/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:490:5: 'CC'
					{
					match(input,17,FOLLOW_17_in_edge_kind2128); 
					 kind = SDGEdge.Kind.CONTROL_DEP_CALL; 
					}
					break;
				case 11 :
					// /data1/hecker/gits/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:491:5: 'CL'
					{
					match(input,21,FOLLOW_21_in_edge_kind2136); 
					 kind = SDGEdge.Kind.CALL; 
					}
					break;
				case 12 :
					// /data1/hecker/gits/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:492:5: 'PI'
					{
					match(input,59,FOLLOW_59_in_edge_kind2144); 
					 kind = SDGEdge.Kind.PARAMETER_IN; 
					}
					break;
				case 13 :
					// /data1/hecker/gits/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:493:5: 'PO'
					{
					match(input,60,FOLLOW_60_in_edge_kind2152); 
					 kind = SDGEdge.Kind.PARAMETER_OUT; 
					}
					break;
				case 14 :
					// /data1/hecker/gits/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:495:5: 'SU'
					{
					match(input,72,FOLLOW_72_in_edge_kind2161); 
					 kind = SDGEdge.Kind.SUMMARY; 
					}
					break;
				case 15 :
					// /data1/hecker/gits/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:496:5: 'SH'
					{
					match(input,70,FOLLOW_70_in_edge_kind2169); 
					 kind = SDGEdge.Kind.SUMMARY_NO_ALIAS; 
					}
					break;
				case 16 :
					// /data1/hecker/gits/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:497:5: 'SF'
					{
					match(input,69,FOLLOW_69_in_edge_kind2177); 
					 kind = SDGEdge.Kind.SUMMARY_DATA; 
					}
					break;
				case 17 :
					// /data1/hecker/gits/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:499:5: 'PS'
					{
					match(input,62,FOLLOW_62_in_edge_kind2186); 
					 kind = SDGEdge.Kind.PARAMETER_STRUCTURE; 
					}
					break;
				case 18 :
					// /data1/hecker/gits/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:500:5: 'PE'
					{
					match(input,58,FOLLOW_58_in_edge_kind2194); 
					 kind = SDGEdge.Kind.PARAMETER_EQUIVALENCE; 
					}
					break;
				case 19 :
					// /data1/hecker/gits/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:502:5: 'FORK'
					{
					match(input,36,FOLLOW_36_in_edge_kind2203); 
					 kind = SDGEdge.Kind.FORK; 
					}
					break;
				case 20 :
					// /data1/hecker/gits/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:503:5: 'FORK_IN'
					{
					match(input,37,FOLLOW_37_in_edge_kind2211); 
					 kind = SDGEdge.Kind.FORK_IN; 
					}
					break;
				case 21 :
					// /data1/hecker/gits/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:504:5: 'FORK_OUT'
					{
					match(input,38,FOLLOW_38_in_edge_kind2219); 
					 kind = SDGEdge.Kind.FORK_OUT; 
					}
					break;
				case 22 :
					// /data1/hecker/gits/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:505:5: 'JOIN'
					{
					match(input,49,FOLLOW_49_in_edge_kind2227); 
					 kind = SDGEdge.Kind.JOIN; 
					}
					break;
				case 23 :
					// /data1/hecker/gits/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:506:5: 'ID'
					{
					match(input,43,FOLLOW_43_in_edge_kind2235); 
					 kind = SDGEdge.Kind.INTERFERENCE; 
					}
					break;
				case 24 :
					// /data1/hecker/gits/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:507:5: 'IW'
					{
					match(input,45,FOLLOW_45_in_edge_kind2243); 
					 kind = SDGEdge.Kind.INTERFERENCE_WRITE; 
					}
					break;
				case 25 :
					// /data1/hecker/gits/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:508:5: 'SD'
					{
					match(input,67,FOLLOW_67_in_edge_kind2251); 
					 kind = SDGEdge.Kind.SYNCHRONIZATION; 
					}
					break;
				case 26 :
					// /data1/hecker/gits/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:510:5: 'HE'
					{
					match(input,42,FOLLOW_42_in_edge_kind2260); 
					 kind = SDGEdge.Kind.HELP; 
					}
					break;
				case 27 :
					// /data1/hecker/gits/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:511:5: 'FD'
					{
					match(input,33,FOLLOW_33_in_edge_kind2268); 
					 kind = SDGEdge.Kind.FOLDED; 
					}
					break;
				case 28 :
					// /data1/hecker/gits/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:512:5: 'FI'
					{
					match(input,34,FOLLOW_34_in_edge_kind2276); 
					 kind = SDGEdge.Kind.FOLD_INCLUDE; 
					}
					break;
				case 29 :
					// /data1/hecker/gits/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:514:5: 'RY'
					{
					match(input,65,FOLLOW_65_in_edge_kind2285); 
					 kind = SDGEdge.Kind.READY_DEP; 
					}
					break;
				case 30 :
					// /data1/hecker/gits/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:515:5: 'JF'
					{
					match(input,48,FOLLOW_48_in_edge_kind2293); 
					 kind = SDGEdge.Kind.JUMP_FLOW; 
					}
					break;
				case 31 :
					// /data1/hecker/gits/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:516:5: 'SP'
					{
					match(input,71,FOLLOW_71_in_edge_kind2301); 
					 kind = SDGEdge.Kind.SUMMARY; 
					}
					break;
				case 32 :
					// /data1/hecker/gits/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:517:5: 'VD'
					{
					match(input,79,FOLLOW_79_in_edge_kind2309); 
					 kind = SDGEdge.Kind.DATA_DEP_EXPR_VALUE; 
					}
					break;
				case 33 :
					// /data1/hecker/gits/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:518:5: 'RD'
					{
					match(input,63,FOLLOW_63_in_edge_kind2317); 
					 kind = SDGEdge.Kind.DATA_DEP_EXPR_REFERENCE; 
					}
					break;
				case 34 :
					// /data1/hecker/gits/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:519:5: 'JD'
					{
					match(input,47,FOLLOW_47_in_edge_kind2325); 
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
	// /data1/hecker/gits/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:522:9: private mayNegNumber returns [int nr] : ( '-' n= number |n= number );
	public final int mayNegNumber() throws RecognitionException {
		int nr = 0;


		int n =0;

		try {
			// /data1/hecker/gits/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:523:3: ( '-' n= number |n= number )
			int alt24=2;
			int LA24_0 = input.LA(1);
			if ( (LA24_0==8) ) {
				alt24=1;
			}
			else if ( (LA24_0==NUMBER) ) {
				alt24=2;
			}

			else {
				NoViableAltException nvae =
					new NoViableAltException("", 24, 0, input);
				throw nvae;
			}

			switch (alt24) {
				case 1 :
					// /data1/hecker/gits/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:523:5: '-' n= number
					{
					match(input,8,FOLLOW_8_in_mayNegNumber2346); 
					pushFollow(FOLLOW_number_in_mayNegNumber2350);
					n=number();
					state._fsp--;

					 nr = -n; 
					}
					break;
				case 2 :
					// /data1/hecker/gits/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:524:5: n= number
					{
					pushFollow(FOLLOW_number_in_mayNegNumber2360);
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
	// /data1/hecker/gits/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:527:9: private number returns [int nr] : n= NUMBER ;
	public final int number() throws RecognitionException {
		int nr = 0;


		Token n=null;

		try {
			// /data1/hecker/gits/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:528:3: (n= NUMBER )
			// /data1/hecker/gits/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:528:5: n= NUMBER
			{
			n=(Token)match(input,NUMBER,FOLLOW_NUMBER_in_number2383); 
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
	// /data1/hecker/gits/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:531:9: private string returns [String str] : s= STRING ;
	public final String string() throws RecognitionException {
		String str = null;


		Token s=null;

		try {
			// /data1/hecker/gits/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:532:3: (s= STRING )
			// /data1/hecker/gits/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:532:5: s= STRING
			{
			s=(Token)match(input,STRING,FOLLOW_STRING_in_string2406); 
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
	// /data1/hecker/gits/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:535:9: private bool returns [boolean b] : ( 'true' | 'false' );
	public final boolean bool() throws RecognitionException {
		boolean b = false;


		try {
			// /data1/hecker/gits/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:536:3: ( 'true' | 'false' )
			int alt25=2;
			int LA25_0 = input.LA(1);
			if ( (LA25_0==116) ) {
				alt25=1;
			}
			else if ( (LA25_0==96) ) {
				alt25=2;
			}

			else {
				NoViableAltException nvae =
					new NoViableAltException("", 25, 0, input);
				throw nvae;
			}

			switch (alt25) {
				case 1 :
					// /data1/hecker/gits/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:536:5: 'true'
					{
					match(input,116,FOLLOW_116_in_bool2427); 
					 b = true; 
					}
					break;
				case 2 :
					// /data1/hecker/gits/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDG_.g:537:5: 'false'
					{
					match(input,96,FOLLOW_96_in_bool2436); 
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



	public static final BitSet FOLLOW_sdg_header_in_sdg_file73 = new BitSet(new long[]{0x0000000000000000L,0x0080000000000000L});
	public static final BitSet FOLLOW_119_in_sdg_file89 = new BitSet(new long[]{0x2080418870013000L,0x0100000000000A00L});
	public static final BitSet FOLLOW_46_in_sdg_file99 = new BitSet(new long[]{0x2080018870013000L,0x0100000000000A00L});
	public static final BitSet FOLLOW_node_list_in_sdg_file128 = new BitSet(new long[]{0x0000000000000000L,0x0100000000000800L});
	public static final BitSet FOLLOW_thread_info_in_sdg_file150 = new BitSet(new long[]{0x0000000000000000L,0x0100000000000000L});
	public static final BitSet FOLLOW_120_in_sdg_file163 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_thread_in_thread_info214 = new BitSet(new long[]{0x0000000000000002L,0x0000000000000800L});
	public static final BitSet FOLLOW_75_in_thread246 = new BitSet(new long[]{0x0000000000000010L});
	public static final BitSet FOLLOW_number_in_thread250 = new BitSet(new long[]{0x0000000000000000L,0x0080000000000000L});
	public static final BitSet FOLLOW_119_in_thread252 = new BitSet(new long[]{0x0000000080000000L});
	public static final BitSet FOLLOW_31_in_thread260 = new BitSet(new long[]{0x0000000000000010L});
	public static final BitSet FOLLOW_number_in_thread266 = new BitSet(new long[]{0x0000000000000400L});
	public static final BitSet FOLLOW_10_in_thread268 = new BitSet(new long[]{0x0000000100000000L});
	public static final BitSet FOLLOW_32_in_thread276 = new BitSet(new long[]{0x0000000000000010L});
	public static final BitSet FOLLOW_number_in_thread283 = new BitSet(new long[]{0x0000000000000400L});
	public static final BitSet FOLLOW_10_in_thread285 = new BitSet(new long[]{0x0000020000000000L});
	public static final BitSet FOLLOW_41_in_thread293 = new BitSet(new long[]{0x0000000000000110L});
	public static final BitSet FOLLOW_mayNegNumber_in_thread300 = new BitSet(new long[]{0x0000000000000400L});
	public static final BitSet FOLLOW_10_in_thread302 = new BitSet(new long[]{0x0004000000000000L});
	public static final BitSet FOLLOW_50_in_thread310 = new BitSet(new long[]{0x0000000000000010L,0x0000100000020000L});
	public static final BitSet FOLLOW_listOrSingleNumber_in_thread317 = new BitSet(new long[]{0x0000000000000400L});
	public static final BitSet FOLLOW_10_in_thread320 = new BitSet(new long[]{0x0000000000400000L});
	public static final BitSet FOLLOW_22_in_thread328 = new BitSet(new long[]{0x0000000000000000L,0x0000100000020000L});
	public static final BitSet FOLLOW_context_in_thread332 = new BitSet(new long[]{0x0000000000000400L});
	public static final BitSet FOLLOW_10_in_thread335 = new BitSet(new long[]{0x0000000008000000L});
	public static final BitSet FOLLOW_27_in_thread343 = new BitSet(new long[]{0x0000000000000000L,0x0010000100000000L});
	public static final BitSet FOLLOW_bool_in_thread347 = new BitSet(new long[]{0x0000000000000400L});
	public static final BitSet FOLLOW_10_in_thread349 = new BitSet(new long[]{0x0000000000000000L,0x0100000000000000L});
	public static final BitSet FOLLOW_120_in_thread355 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_mayEmptyNumberList_in_listOrSingleNumber385 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_number_in_listOrSingleNumber396 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_108_in_mayEmptyNumberList418 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_81_in_mayEmptyNumberList424 = new BitSet(new long[]{0x0000000000000000L,0x0000000000040000L});
	public static final BitSet FOLLOW_82_in_mayEmptyNumberList426 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_81_in_mayEmptyNumberList432 = new BitSet(new long[]{0x0000000000000010L});
	public static final BitSet FOLLOW_number_in_mayEmptyNumberList436 = new BitSet(new long[]{0x0000000000000080L,0x0000000000040000L});
	public static final BitSet FOLLOW_7_in_mayEmptyNumberList441 = new BitSet(new long[]{0x0000000000000010L});
	public static final BitSet FOLLOW_number_in_mayEmptyNumberList445 = new BitSet(new long[]{0x0000000000000080L,0x0000000000040000L});
	public static final BitSet FOLLOW_82_in_mayEmptyNumberList452 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_108_in_mayEmptyStringList473 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_81_in_mayEmptyStringList479 = new BitSet(new long[]{0x0000000000000000L,0x0000000000040000L});
	public static final BitSet FOLLOW_82_in_mayEmptyStringList481 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_81_in_mayEmptyStringList487 = new BitSet(new long[]{0x0000000000000020L});
	public static final BitSet FOLLOW_string_in_mayEmptyStringList491 = new BitSet(new long[]{0x0000000000000080L,0x0000000000040000L});
	public static final BitSet FOLLOW_7_in_mayEmptyStringList496 = new BitSet(new long[]{0x0000000000000020L});
	public static final BitSet FOLLOW_string_in_mayEmptyStringList500 = new BitSet(new long[]{0x0000000000000080L,0x0000000000040000L});
	public static final BitSet FOLLOW_82_in_mayEmptyStringList507 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_108_in_context529 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_81_in_context535 = new BitSet(new long[]{0x0000000000000110L});
	public static final BitSet FOLLOW_mayNegNumber_in_context539 = new BitSet(new long[]{0x0000000000000080L,0x0000000000040000L});
	public static final BitSet FOLLOW_7_in_context544 = new BitSet(new long[]{0x0000000000000110L});
	public static final BitSet FOLLOW_mayNegNumber_in_context548 = new BitSet(new long[]{0x0000000000000080L,0x0000000000040000L});
	public static final BitSet FOLLOW_82_in_context555 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_68_in_sdg_header574 = new BitSet(new long[]{0x0000000000000022L,0x0041000000000000L});
	public static final BitSet FOLLOW_118_in_sdg_header590 = new BitSet(new long[]{0x0000000000000010L});
	public static final BitSet FOLLOW_number_in_sdg_header594 = new BitSet(new long[]{0x0000000000000022L,0x0001000000000000L});
	public static final BitSet FOLLOW_string_in_sdg_header615 = new BitSet(new long[]{0x0000000000000002L,0x0001000000000000L});
	public static final BitSet FOLLOW_112_in_sdg_header635 = new BitSet(new long[]{0x0000000000000010L});
	public static final BitSet FOLLOW_number_in_sdg_header639 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_node_in_node_list672 = new BitSet(new long[]{0x2080018870013002L,0x0000000000000200L});
	public static final BitSet FOLLOW_node_kind_in_node698 = new BitSet(new long[]{0x0000000000000110L});
	public static final BitSet FOLLOW_mayNegNumber_in_node702 = new BitSet(new long[]{0x0000000000000000L,0x0080000000000000L});
	public static final BitSet FOLLOW_119_in_node711 = new BitSet(new long[]{0xDF7BAC7607BEC800L,0x010000000001F5EFL});
	public static final BitSet FOLLOW_node_attributes_in_node720 = new BitSet(new long[]{0xDC43AC76073E0000L,0x010000000000A1EBL});
	public static final BitSet FOLLOW_node_edges_in_node730 = new BitSet(new long[]{0x0000000000000000L,0x0100000000000000L});
	public static final BitSet FOLLOW_120_in_node737 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_55_in_node_kind756 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_61_in_node_kind764 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_30_in_node_kind772 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_28_in_node_kind780 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_16_in_node_kind788 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_12_in_node_kind796 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_13_in_node_kind804 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_39_in_node_kind812 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_40_in_node_kind820 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_29_in_node_kind828 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_73_in_node_kind836 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_35_in_node_kind844 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_node_attr_in_node_attributes863 = new BitSet(new long[]{0x0000000000000400L});
	public static final BitSet FOLLOW_10_in_node_attributes866 = new BitSet(new long[]{0x033800000080C802L,0x0000000000015404L});
	public static final BitSet FOLLOW_66_in_node_attr884 = new BitSet(new long[]{0x0000000000000020L});
	public static final BitSet FOLLOW_node_source_in_node_attr888 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_14_in_node_attr900 = new BitSet(new long[]{0x0000000000000020L});
	public static final BitSet FOLLOW_node_bytecode_in_node_attr904 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_76_in_node_attr915 = new BitSet(new long[]{0x0000000000000010L});
	public static final BitSet FOLLOW_number_in_node_attr917 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_57_in_node_attr967 = new BitSet(new long[]{0x0000000000000010L});
	public static final BitSet FOLLOW_number_in_node_attr971 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_56_in_node_attr990 = new BitSet(new long[]{0x0000100000000000L,0x002EEFFEFFF80000L});
	public static final BitSet FOLLOW_node_oper_in_node_attr994 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_78_in_node_attr1022 = new BitSet(new long[]{0x0000000000000020L});
	public static final BitSet FOLLOW_string_in_node_attr1026 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_74_in_node_attr1054 = new BitSet(new long[]{0x0000000000000020L});
	public static final BitSet FOLLOW_string_in_node_attr1058 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_80_in_node_attr1083 = new BitSet(new long[]{0x0000000000000110L});
	public static final BitSet FOLLOW_may_neg_num_set_in_node_attr1087 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_53_in_node_attr1101 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_15_in_node_attr1135 = new BitSet(new long[]{0x0000000000000020L});
	public static final BitSet FOLLOW_string_in_node_attr1139 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_11_in_node_attr1161 = new BitSet(new long[]{0x0000000000000010L});
	public static final BitSet FOLLOW_pos_num_set_in_node_attr1165 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_23_in_node_attr1183 = new BitSet(new long[]{0x0000000000000010L});
	public static final BitSet FOLLOW_pos_num_set_in_node_attr1187 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_76_in_node_attr1204 = new BitSet(new long[]{0x0000000000000020L});
	public static final BitSet FOLLOW_string_in_node_attr1208 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_51_in_node_attr1226 = new BitSet(new long[]{0x0000000000000000L,0x0000100000020000L});
	public static final BitSet FOLLOW_mayEmptyStringList_in_node_attr1230 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_52_in_node_attr1239 = new BitSet(new long[]{0x0000000000000000L,0x0000100000020000L});
	public static final BitSet FOLLOW_mayEmptyStringList_in_node_attr1243 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_number_in_pos_num_set1264 = new BitSet(new long[]{0x0000000000000082L});
	public static final BitSet FOLLOW_7_in_pos_num_set1269 = new BitSet(new long[]{0x0000000000000010L});
	public static final BitSet FOLLOW_number_in_pos_num_set1273 = new BitSet(new long[]{0x0000000000000082L});
	public static final BitSet FOLLOW_mayNegNumber_in_may_neg_num_set1299 = new BitSet(new long[]{0x0000000000000082L});
	public static final BitSet FOLLOW_7_in_may_neg_num_set1304 = new BitSet(new long[]{0x0000000000000110L});
	public static final BitSet FOLLOW_mayNegNumber_in_may_neg_num_set1308 = new BitSet(new long[]{0x0000000000000082L});
	public static final BitSet FOLLOW_string_in_node_source1334 = new BitSet(new long[]{0x0000000000000200L});
	public static final BitSet FOLLOW_9_in_node_source1336 = new BitSet(new long[]{0x0000000000000010L});
	public static final BitSet FOLLOW_number_in_node_source1340 = new BitSet(new long[]{0x0000000000000080L});
	public static final BitSet FOLLOW_7_in_node_source1342 = new BitSet(new long[]{0x0000000000000010L});
	public static final BitSet FOLLOW_number_in_node_source1346 = new BitSet(new long[]{0x0000000000000100L});
	public static final BitSet FOLLOW_8_in_node_source1348 = new BitSet(new long[]{0x0000000000000010L});
	public static final BitSet FOLLOW_number_in_node_source1352 = new BitSet(new long[]{0x0000000000000080L});
	public static final BitSet FOLLOW_7_in_node_source1354 = new BitSet(new long[]{0x0000000000000010L});
	public static final BitSet FOLLOW_number_in_node_source1358 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_string_in_node_bytecode1389 = new BitSet(new long[]{0x0000000000000200L});
	public static final BitSet FOLLOW_9_in_node_bytecode1391 = new BitSet(new long[]{0x0000000000000110L});
	public static final BitSet FOLLOW_mayNegNumber_in_node_bytecode1395 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_93_in_node_oper1416 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_102_in_node_oper1433 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_97_in_node_oper1447 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_89_in_node_oper1459 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_115_in_node_oper1472 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_101_in_node_oper1483 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_114_in_node_oper1492 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_109_in_node_oper1506 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_87_in_node_oper1520 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_117_in_node_oper1536 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_92_in_node_oper1553 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_110_in_node_oper1568 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_85_in_node_oper1585 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_113_in_node_oper1602 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_111_in_node_oper1618 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_91_in_node_oper1631 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_106_in_node_oper1642 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_105_in_node_oper1658 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_86_in_node_oper1671 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_44_in_node_oper1687 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_104_in_node_oper1707 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_103_in_node_oper1725 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_90_in_node_oper1743 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_88_in_node_oper1757 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_94_in_node_oper1775 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_95_in_node_oper1792 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_99_in_node_oper1810 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_98_in_node_oper1825 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_100_in_node_oper1837 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_83_in_node_oper1851 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_84_in_node_oper1867 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_107_in_node_oper1882 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_edge_in_node_edges1910 = new BitSet(new long[]{0x0000000000000400L});
	public static final BitSet FOLLOW_10_in_node_edges1912 = new BitSet(new long[]{0xDC43AC76073E0002L,0x000000000000A1EBL});
	public static final BitSet FOLLOW_edge_kind_in_edge1937 = new BitSet(new long[]{0x0000000000000010L});
	public static final BitSet FOLLOW_number_in_edge1941 = new BitSet(new long[]{0x0000000000000202L});
	public static final BitSet FOLLOW_9_in_edge1946 = new BitSet(new long[]{0x0000000000000020L});
	public static final BitSet FOLLOW_string_in_edge1950 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_25_in_edge_kind1975 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_26_in_edge_kind1995 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_24_in_edge_kind2014 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_18_in_edge_kind2033 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_19_in_edge_kind2045 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_77_in_edge_kind2057 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_20_in_edge_kind2068 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_54_in_edge_kind2084 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_64_in_edge_kind2105 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_17_in_edge_kind2128 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_21_in_edge_kind2136 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_59_in_edge_kind2144 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_60_in_edge_kind2152 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_72_in_edge_kind2161 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_70_in_edge_kind2169 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_69_in_edge_kind2177 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_62_in_edge_kind2186 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_58_in_edge_kind2194 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_36_in_edge_kind2203 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_37_in_edge_kind2211 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_38_in_edge_kind2219 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_49_in_edge_kind2227 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_43_in_edge_kind2235 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_45_in_edge_kind2243 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_67_in_edge_kind2251 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_42_in_edge_kind2260 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_33_in_edge_kind2268 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_34_in_edge_kind2276 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_65_in_edge_kind2285 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_48_in_edge_kind2293 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_71_in_edge_kind2301 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_79_in_edge_kind2309 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_63_in_edge_kind2317 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_47_in_edge_kind2325 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_8_in_mayNegNumber2346 = new BitSet(new long[]{0x0000000000000010L});
	public static final BitSet FOLLOW_number_in_mayNegNumber2350 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_number_in_mayNegNumber2360 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_NUMBER_in_number2383 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_STRING_in_string2406 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_116_in_bool2427 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_96_in_bool2436 = new BitSet(new long[]{0x0000000000000002L});
}
