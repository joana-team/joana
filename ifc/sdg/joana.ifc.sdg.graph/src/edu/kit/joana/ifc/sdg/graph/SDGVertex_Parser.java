// $ANTLR 3.5.2 /data1/hecker/gits/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDGVertex_.g 2018-02-19 15:37:54
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
import edu.kit.joana.util.SourceLocation;


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
public class SDGVertex_Parser extends Parser {
	public static final String[] tokenNames = new String[] {
		"<invalid>", "<EOR>", "<DOWN>", "<UP>", "NUMBER", "STRING", "WHITESPACE", 
		"','", "'-'", "':'", "';'", "'A'", "'ACTI'", "'ACTO'", "'B'", "'C'", "'CALL'", 
		"'CC'", "'CD'", "'CE'", "'CF'", "'CL'", "'D'", "'DA'", "'DD'", "'DH'", 
		"'ENTR'", "'EXIT'", "'EXPR'", "'FD'", "'FI'", "'FOLD'", "'FORK'", "'FORK_IN'", 
		"'FORK_OUT'", "'FRMI'", "'FRMO'", "'HE'", "'ID'", "'IF'", "'IW'", "'JD'", 
		"'JF'", "'JOIN'", "'LD'", "'LU'", "'N'", "'NF'", "'NORM'", "'O'", "'P'", 
		"'PE'", "'PI'", "'PO'", "'PRED'", "'PS'", "'RD'", "'RF'", "'RY'", "'S'", 
		"'SD'", "'SF'", "'SH'", "'SP'", "'SU'", "'SYNC'", "'T'", "'U'", "'UN'", 
		"'V'", "'VD'", "'Z'", "'['", "']'", "'act-in'", "'act-out'", "'array'", 
		"'assign'", "'binary'", "'call'", "'charconst'", "'compound'", "'declaration'", 
		"'derefer'", "'empty'", "'entry'", "'exit'", "'false'", "'floatconst'", 
		"'form-ellip'", "'form-in'", "'form-out'", "'functionconst'", "'intconst'", 
		"'jump'", "'loop'", "'modassign'", "'modify'", "'monitor'", "'null'", 
		"'question'", "'refer'", "'reference'", "'select'", "'shortcut'", "'stringconst'", 
		"'true'", "'unary'", "'{'", "'}'"
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
	public static final int NUMBER=4;
	public static final int STRING=5;
	public static final int WHITESPACE=6;

	// delegates
	public Parser[] getDelegates() {
		return new Parser[] {};
	}

	// delegators


	public SDGVertex_Parser(TokenStream input) {
		this(input, new RecognizerSharedState());
	}
	public SDGVertex_Parser(TokenStream input, RecognizerSharedState state) {
		super(input, state);
	}

	@Override public String[] getTokenNames() { return SDGVertex_Parser.tokenNames; }
	@Override public String getGrammarFileName() { return "/data1/hecker/gits/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDGVertex_.g"; }


	  // Stores always the last position specified by a previous node. This is used
	  // for sane error recovery, when no position is defined for a node:
	  // We assume that its position may be somewhat equal to its pred node. 
	  private static SourceLocation defaultSrcPos = SourceLocation.UNKNOWN;
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

	  static final class SDGNodeStub {
	  
	    private final SDGNode.Kind kind;
	    private final int id;
	    private SourceLocation spos;
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
	    
	    private SDGNodeStub(final SDGNode.Kind kind, final int id, SourceLocation defSPos, ByteCodePos defBPos) {
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
	  
	      public SDGNode createNode(final SDGNode.NodeFactory nf) {
	        final int kindId = findKindId(op, kind);

	        final int[] allocSites;
	        if (this.allocSites != null) {
	          allocSites = this.allocSites.toArray();
	        } else {
	          allocSites = null;
	        }
	        
	        
	        final String[] localDefNames;
	        if (this.localDefNames != null) {
	          localDefNames = this.localDefNames.toArray(new String[this.localDefNames.size()]);
	        } else {
	          localDefNames = null;
	        }
	        
	        final String[] localUseNames;
	        if (this.localUseNames != null) {
	          localUseNames = this.localUseNames.toArray(new String[this.localUseNames.size()]);
	        } else {
	          localUseNames = null;
	        }
	        
	        final SDGNode n = nf.createNode(op, kindId, id, val, procId, type, spos, bpos.name, bpos.index,
	              localDefNames, localUseNames, unresolvedCallTarget, allocSites, classLoader);

	        if (aliasDataSrc != null) {
	          n.setAliasDataSources(aliasDataSrc);
	        }

	        if (threadNums != null) {
	          n.setThreadNumbers(this.threadNums.toArray());
	        }

	        return n;
	        
	      }
	    
	    public void createEdges(final SDG sdg) {
	      final SDGNode from = sdg.getNode(id);
	      
	      for (final SDGEdgeStub e : edges) {
	        final SDGNode to = sdg.getNode(e.to);
	        final SDGEdge edge = (e.label != null 
	          ? new LabeledSDGEdge(from, to, e.kind, e.label)
	          :  e.kind.newEdge(from, to));
	        
	        sdg.addEdge(edge);
	      }
	    }
	  }
	  
	  static final class ByteCodePos {
	  
	    private final String name;
	    private final int index;
	    
	    public ByteCodePos(final String name, final int index) {
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



	// $ANTLR start "node"
	// /data1/hecker/gits/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDGVertex_.g:224:1: node returns [SDGNodeStub nstub] : k= node_kind id= mayNegNumber '{' node_attributes[nstub] node_edges[nstub] '}' ;
	public final SDGNodeStub node() throws RecognitionException {
		SDGNodeStub nstub = null;


		SDGNode.Kind k =null;
		int id =0;

		try {
			// /data1/hecker/gits/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDGVertex_.g:225:3: (k= node_kind id= mayNegNumber '{' node_attributes[nstub] node_edges[nstub] '}' )
			// /data1/hecker/gits/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDGVertex_.g:225:5: k= node_kind id= mayNegNumber '{' node_attributes[nstub] node_edges[nstub] '}'
			{
			pushFollow(FOLLOW_node_kind_in_node73);
			k=node_kind();
			state._fsp--;

			pushFollow(FOLLOW_mayNegNumber_in_node77);
			id=mayNegNumber();
			state._fsp--;

			 nstub = new SDGNodeStub(k, id, defaultSrcPos, defaultBcPos); 
			match(input,108,FOLLOW_108_in_node86); 
			pushFollow(FOLLOW_node_attributes_in_node95);
			node_attributes(nstub);
			state._fsp--;

			pushFollow(FOLLOW_node_edges_in_node105);
			node_edges(nstub);
			state._fsp--;

			match(input,109,FOLLOW_109_in_node112); 
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
	// /data1/hecker/gits/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDGVertex_.g:232:9: private node_kind returns [SDGNode.Kind kind] : ( 'NORM' | 'PRED' | 'EXPR' | 'ENTR' | 'CALL' | 'ACTI' | 'ACTO' | 'FRMI' | 'FRMO' | 'EXIT' | 'SYNC' | 'FOLD' );
	public final SDGNode.Kind node_kind() throws RecognitionException {
		SDGNode.Kind kind = null;


		try {
			// /data1/hecker/gits/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDGVertex_.g:233:3: ( 'NORM' | 'PRED' | 'EXPR' | 'ENTR' | 'CALL' | 'ACTI' | 'ACTO' | 'FRMI' | 'FRMO' | 'EXIT' | 'SYNC' | 'FOLD' )
			int alt1=12;
			switch ( input.LA(1) ) {
			case 48:
				{
				alt1=1;
				}
				break;
			case 54:
				{
				alt1=2;
				}
				break;
			case 28:
				{
				alt1=3;
				}
				break;
			case 26:
				{
				alt1=4;
				}
				break;
			case 16:
				{
				alt1=5;
				}
				break;
			case 12:
				{
				alt1=6;
				}
				break;
			case 13:
				{
				alt1=7;
				}
				break;
			case 35:
				{
				alt1=8;
				}
				break;
			case 36:
				{
				alt1=9;
				}
				break;
			case 27:
				{
				alt1=10;
				}
				break;
			case 65:
				{
				alt1=11;
				}
				break;
			case 31:
				{
				alt1=12;
				}
				break;
			default:
				NoViableAltException nvae =
					new NoViableAltException("", 1, 0, input);
				throw nvae;
			}
			switch (alt1) {
				case 1 :
					// /data1/hecker/gits/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDGVertex_.g:233:5: 'NORM'
					{
					match(input,48,FOLLOW_48_in_node_kind131); 
					 kind = SDGNode.Kind.NORMAL; 
					}
					break;
				case 2 :
					// /data1/hecker/gits/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDGVertex_.g:234:5: 'PRED'
					{
					match(input,54,FOLLOW_54_in_node_kind139); 
					 kind = SDGNode.Kind.PREDICATE; 
					}
					break;
				case 3 :
					// /data1/hecker/gits/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDGVertex_.g:235:5: 'EXPR'
					{
					match(input,28,FOLLOW_28_in_node_kind147); 
					 kind = SDGNode.Kind.EXPRESSION; 
					}
					break;
				case 4 :
					// /data1/hecker/gits/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDGVertex_.g:236:5: 'ENTR'
					{
					match(input,26,FOLLOW_26_in_node_kind155); 
					 kind = SDGNode.Kind.ENTRY; 
					}
					break;
				case 5 :
					// /data1/hecker/gits/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDGVertex_.g:237:5: 'CALL'
					{
					match(input,16,FOLLOW_16_in_node_kind163); 
					 kind = SDGNode.Kind.CALL; 
					}
					break;
				case 6 :
					// /data1/hecker/gits/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDGVertex_.g:238:5: 'ACTI'
					{
					match(input,12,FOLLOW_12_in_node_kind171); 
					 kind = SDGNode.Kind.ACTUAL_IN; 
					}
					break;
				case 7 :
					// /data1/hecker/gits/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDGVertex_.g:239:5: 'ACTO'
					{
					match(input,13,FOLLOW_13_in_node_kind179); 
					 kind = SDGNode.Kind.ACTUAL_OUT; 
					}
					break;
				case 8 :
					// /data1/hecker/gits/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDGVertex_.g:240:5: 'FRMI'
					{
					match(input,35,FOLLOW_35_in_node_kind187); 
					 kind = SDGNode.Kind.FORMAL_IN; 
					}
					break;
				case 9 :
					// /data1/hecker/gits/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDGVertex_.g:241:5: 'FRMO'
					{
					match(input,36,FOLLOW_36_in_node_kind195); 
					 kind = SDGNode.Kind.FORMAL_OUT; 
					}
					break;
				case 10 :
					// /data1/hecker/gits/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDGVertex_.g:242:5: 'EXIT'
					{
					match(input,27,FOLLOW_27_in_node_kind203); 
					 kind = SDGNode.Kind.EXIT; 
					}
					break;
				case 11 :
					// /data1/hecker/gits/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDGVertex_.g:243:5: 'SYNC'
					{
					match(input,65,FOLLOW_65_in_node_kind211); 
					 kind = SDGNode.Kind.SYNCHRONIZATION; 
					}
					break;
				case 12 :
					// /data1/hecker/gits/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDGVertex_.g:244:5: 'FOLD'
					{
					match(input,31,FOLLOW_31_in_node_kind219); 
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
	// /data1/hecker/gits/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDGVertex_.g:247:9: private node_attributes[SDGNodeStub node] : ( node_attr[node] ';' )* ;
	public final void node_attributes(SDGNodeStub node) throws RecognitionException {
		try {
			// /data1/hecker/gits/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDGVertex_.g:248:3: ( ( node_attr[node] ';' )* )
			// /data1/hecker/gits/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDGVertex_.g:248:5: ( node_attr[node] ';' )*
			{
			// /data1/hecker/gits/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDGVertex_.g:248:5: ( node_attr[node] ';' )*
			loop2:
			while (true) {
				int alt2=2;
				int LA2_0 = input.LA(1);
				if ( (LA2_0==11||(LA2_0 >= 14 && LA2_0 <= 15)||LA2_0==22||(LA2_0 >= 44 && LA2_0 <= 46)||(LA2_0 >= 49 && LA2_0 <= 50)||LA2_0==59||(LA2_0 >= 66 && LA2_0 <= 67)||LA2_0==69||LA2_0==71) ) {
					alt2=1;
				}

				switch (alt2) {
				case 1 :
					// /data1/hecker/gits/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDGVertex_.g:248:6: node_attr[node] ';'
					{
					pushFollow(FOLLOW_node_attr_in_node_attributes238);
					node_attr(node);
					state._fsp--;

					match(input,10,FOLLOW_10_in_node_attributes241); 
					}
					break;

				default :
					break loop2;
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
	// /data1/hecker/gits/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDGVertex_.g:251:9: private node_attr[SDGNodeStub node] : ( 'S' spos= node_source | 'B' bpos= node_bytecode | 'U' number | 'P' procId= number | 'O' op= node_oper | 'V' val= string | 'T' type= string | 'Z' tn= may_neg_num_set | 'N' | 'C' cl= string | 'A' al= pos_num_set | 'D' ds= pos_num_set | 'U' uct= string | 'LD' ldefs= mayEmptyStringList | 'LU' luses= mayEmptyStringList );
	public final void node_attr(SDGNodeStub node) throws RecognitionException {
		SourceLocation spos =null;
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
			// /data1/hecker/gits/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDGVertex_.g:252:3: ( 'S' spos= node_source | 'B' bpos= node_bytecode | 'U' number | 'P' procId= number | 'O' op= node_oper | 'V' val= string | 'T' type= string | 'Z' tn= may_neg_num_set | 'N' | 'C' cl= string | 'A' al= pos_num_set | 'D' ds= pos_num_set | 'U' uct= string | 'LD' ldefs= mayEmptyStringList | 'LU' luses= mayEmptyStringList )
			int alt3=15;
			switch ( input.LA(1) ) {
			case 59:
				{
				alt3=1;
				}
				break;
			case 14:
				{
				alt3=2;
				}
				break;
			case 67:
				{
				int LA3_3 = input.LA(2);
				if ( (LA3_3==NUMBER) ) {
					alt3=3;
				}
				else if ( (LA3_3==STRING) ) {
					alt3=13;
				}

				else {
					int nvaeMark = input.mark();
					try {
						input.consume();
						NoViableAltException nvae =
							new NoViableAltException("", 3, 3, input);
						throw nvae;
					} finally {
						input.rewind(nvaeMark);
					}
				}

				}
				break;
			case 50:
				{
				alt3=4;
				}
				break;
			case 49:
				{
				alt3=5;
				}
				break;
			case 69:
				{
				alt3=6;
				}
				break;
			case 66:
				{
				alt3=7;
				}
				break;
			case 71:
				{
				alt3=8;
				}
				break;
			case 46:
				{
				alt3=9;
				}
				break;
			case 15:
				{
				alt3=10;
				}
				break;
			case 11:
				{
				alt3=11;
				}
				break;
			case 22:
				{
				alt3=12;
				}
				break;
			case 44:
				{
				alt3=14;
				}
				break;
			case 45:
				{
				alt3=15;
				}
				break;
			default:
				NoViableAltException nvae =
					new NoViableAltException("", 3, 0, input);
				throw nvae;
			}
			switch (alt3) {
				case 1 :
					// /data1/hecker/gits/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDGVertex_.g:252:5: 'S' spos= node_source
					{
					match(input,59,FOLLOW_59_in_node_attr259); 
					pushFollow(FOLLOW_node_source_in_node_attr263);
					spos=node_source();
					state._fsp--;

					 node.spos = spos; defaultSrcPos = spos; 
					}
					break;
				case 2 :
					// /data1/hecker/gits/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDGVertex_.g:253:5: 'B' bpos= node_bytecode
					{
					match(input,14,FOLLOW_14_in_node_attr275); 
					pushFollow(FOLLOW_node_bytecode_in_node_attr279);
					bpos=node_bytecode();
					state._fsp--;

					 node.bpos = bpos; defaultBcPos = bpos; 
					}
					break;
				case 3 :
					// /data1/hecker/gits/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDGVertex_.g:254:5: 'U' number
					{
					match(input,67,FOLLOW_67_in_node_attr290); 
					pushFollow(FOLLOW_number_in_node_attr292);
					number();
					state._fsp--;

					}
					break;
				case 4 :
					// /data1/hecker/gits/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDGVertex_.g:255:5: 'P' procId= number
					{
					match(input,50,FOLLOW_50_in_node_attr342); 
					pushFollow(FOLLOW_number_in_node_attr346);
					procId=number();
					state._fsp--;

					 node.procId = procId; 
					}
					break;
				case 5 :
					// /data1/hecker/gits/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDGVertex_.g:256:5: 'O' op= node_oper
					{
					match(input,49,FOLLOW_49_in_node_attr365); 
					pushFollow(FOLLOW_node_oper_in_node_attr369);
					op=node_oper();
					state._fsp--;

					 node.op = op; 
					}
					break;
				case 6 :
					// /data1/hecker/gits/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDGVertex_.g:257:5: 'V' val= string
					{
					match(input,69,FOLLOW_69_in_node_attr397); 
					pushFollow(FOLLOW_string_in_node_attr401);
					val=string();
					state._fsp--;

					 node.val = val; 
					}
					break;
				case 7 :
					// /data1/hecker/gits/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDGVertex_.g:258:5: 'T' type= string
					{
					match(input,66,FOLLOW_66_in_node_attr429); 
					pushFollow(FOLLOW_string_in_node_attr433);
					type=string();
					state._fsp--;

					 node.type = type; 
					}
					break;
				case 8 :
					// /data1/hecker/gits/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDGVertex_.g:259:5: 'Z' tn= may_neg_num_set
					{
					match(input,71,FOLLOW_71_in_node_attr458); 
					pushFollow(FOLLOW_may_neg_num_set_in_node_attr462);
					tn=may_neg_num_set();
					state._fsp--;

					 node.threadNums = tn; 
					}
					break;
				case 9 :
					// /data1/hecker/gits/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDGVertex_.g:260:5: 'N'
					{
					match(input,46,FOLLOW_46_in_node_attr476); 
					 node.nonTerm = true; 
					}
					break;
				case 10 :
					// /data1/hecker/gits/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDGVertex_.g:261:5: 'C' cl= string
					{
					match(input,15,FOLLOW_15_in_node_attr510); 
					pushFollow(FOLLOW_string_in_node_attr514);
					cl=string();
					state._fsp--;

					 node.classLoader = cl; 
					}
					break;
				case 11 :
					// /data1/hecker/gits/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDGVertex_.g:262:5: 'A' al= pos_num_set
					{
					match(input,11,FOLLOW_11_in_node_attr536); 
					pushFollow(FOLLOW_pos_num_set_in_node_attr540);
					al=pos_num_set();
					state._fsp--;

					 node.allocSites = al; 
					}
					break;
				case 12 :
					// /data1/hecker/gits/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDGVertex_.g:263:5: 'D' ds= pos_num_set
					{
					match(input,22,FOLLOW_22_in_node_attr558); 
					pushFollow(FOLLOW_pos_num_set_in_node_attr562);
					ds=pos_num_set();
					state._fsp--;

					 node.aliasDataSrc = ds; 
					}
					break;
				case 13 :
					// /data1/hecker/gits/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDGVertex_.g:264:5: 'U' uct= string
					{
					match(input,67,FOLLOW_67_in_node_attr579); 
					pushFollow(FOLLOW_string_in_node_attr583);
					uct=string();
					state._fsp--;

					 node.unresolvedCallTarget = uct; 
					}
					break;
				case 14 :
					// /data1/hecker/gits/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDGVertex_.g:265:5: 'LD' ldefs= mayEmptyStringList
					{
					match(input,44,FOLLOW_44_in_node_attr601); 
					pushFollow(FOLLOW_mayEmptyStringList_in_node_attr605);
					ldefs=mayEmptyStringList();
					state._fsp--;


					                              node.localDefNames = ldefs;
					                            
					}
					break;
				case 15 :
					// /data1/hecker/gits/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDGVertex_.g:268:5: 'LU' luses= mayEmptyStringList
					{
					match(input,45,FOLLOW_45_in_node_attr614); 
					pushFollow(FOLLOW_mayEmptyStringList_in_node_attr618);
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
	// /data1/hecker/gits/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDGVertex_.g:273:9: private pos_num_set returns [TIntSet nums = new TIntHashSet();] : n= number ( ',' n2= number )* ;
	public final TIntSet pos_num_set() throws RecognitionException {
		TIntSet nums =  new TIntHashSet();;


		int n =0;
		int n2 =0;

		try {
			// /data1/hecker/gits/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDGVertex_.g:274:3: (n= number ( ',' n2= number )* )
			// /data1/hecker/gits/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDGVertex_.g:274:5: n= number ( ',' n2= number )*
			{
			pushFollow(FOLLOW_number_in_pos_num_set642);
			n=number();
			state._fsp--;

			 nums.add(n); 
			// /data1/hecker/gits/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDGVertex_.g:274:31: ( ',' n2= number )*
			loop4:
			while (true) {
				int alt4=2;
				int LA4_0 = input.LA(1);
				if ( (LA4_0==7) ) {
					alt4=1;
				}

				switch (alt4) {
				case 1 :
					// /data1/hecker/gits/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDGVertex_.g:274:32: ',' n2= number
					{
					match(input,7,FOLLOW_7_in_pos_num_set647); 
					pushFollow(FOLLOW_number_in_pos_num_set651);
					n2=number();
					state._fsp--;

					 nums.add(n2); 
					}
					break;

				default :
					break loop4;
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
	// /data1/hecker/gits/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDGVertex_.g:277:9: private may_neg_num_set returns [TIntSet nums = new TIntHashSet();] : n= mayNegNumber ( ',' n2= mayNegNumber )* ;
	public final TIntSet may_neg_num_set() throws RecognitionException {
		TIntSet nums =  new TIntHashSet();;


		int n =0;
		int n2 =0;

		try {
			// /data1/hecker/gits/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDGVertex_.g:278:3: (n= mayNegNumber ( ',' n2= mayNegNumber )* )
			// /data1/hecker/gits/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDGVertex_.g:278:5: n= mayNegNumber ( ',' n2= mayNegNumber )*
			{
			pushFollow(FOLLOW_mayNegNumber_in_may_neg_num_set677);
			n=mayNegNumber();
			state._fsp--;

			 nums.add(n); 
			// /data1/hecker/gits/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDGVertex_.g:278:37: ( ',' n2= mayNegNumber )*
			loop5:
			while (true) {
				int alt5=2;
				int LA5_0 = input.LA(1);
				if ( (LA5_0==7) ) {
					alt5=1;
				}

				switch (alt5) {
				case 1 :
					// /data1/hecker/gits/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDGVertex_.g:278:38: ',' n2= mayNegNumber
					{
					match(input,7,FOLLOW_7_in_may_neg_num_set682); 
					pushFollow(FOLLOW_mayNegNumber_in_may_neg_num_set686);
					n2=mayNegNumber();
					state._fsp--;

					 nums.add(n2); 
					}
					break;

				default :
					break loop5;
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
	// /data1/hecker/gits/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDGVertex_.g:281:9: private node_source returns [SourceLocation spos] : filename= string ':' startRow= number ',' startColumn= number '-' endRow= number ',' endColumn= number ;
	public final SourceLocation node_source() throws RecognitionException {
		SourceLocation spos = null;


		String filename =null;
		int startRow =0;
		int startColumn =0;
		int endRow =0;
		int endColumn =0;

		try {
			// /data1/hecker/gits/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDGVertex_.g:282:3: (filename= string ':' startRow= number ',' startColumn= number '-' endRow= number ',' endColumn= number )
			// /data1/hecker/gits/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDGVertex_.g:282:5: filename= string ':' startRow= number ',' startColumn= number '-' endRow= number ',' endColumn= number
			{
			pushFollow(FOLLOW_string_in_node_source712);
			filename=string();
			state._fsp--;

			match(input,9,FOLLOW_9_in_node_source714); 
			pushFollow(FOLLOW_number_in_node_source718);
			startRow=number();
			state._fsp--;

			match(input,7,FOLLOW_7_in_node_source720); 
			pushFollow(FOLLOW_number_in_node_source724);
			startColumn=number();
			state._fsp--;

			match(input,8,FOLLOW_8_in_node_source726); 
			pushFollow(FOLLOW_number_in_node_source730);
			endRow=number();
			state._fsp--;

			match(input,7,FOLLOW_7_in_node_source732); 
			pushFollow(FOLLOW_number_in_node_source736);
			endColumn=number();
			state._fsp--;

			 spos = SourceLocation.getLocation(filename, startRow, startColumn, endRow, endColumn); 
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
	// /data1/hecker/gits/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDGVertex_.g:286:9: private node_bytecode returns [ByteCodePos bpos] : name= string ':' index= mayNegNumber ;
	public final ByteCodePos node_bytecode() throws RecognitionException {
		ByteCodePos bpos = null;


		String name =null;
		int index =0;

		try {
			// /data1/hecker/gits/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDGVertex_.g:287:3: (name= string ':' index= mayNegNumber )
			// /data1/hecker/gits/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDGVertex_.g:287:5: name= string ':' index= mayNegNumber
			{
			pushFollow(FOLLOW_string_in_node_bytecode767);
			name=string();
			state._fsp--;

			match(input,9,FOLLOW_9_in_node_bytecode769); 
			pushFollow(FOLLOW_mayNegNumber_in_node_bytecode773);
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
	// /data1/hecker/gits/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDGVertex_.g:290:9: private node_oper returns [SDGNode.Operation op] : ( 'empty' | 'intconst' | 'floatconst' | 'charconst' | 'stringconst' | 'functionconst' | 'shortcut' | 'question' | 'binary' | 'unary' | 'derefer' | 'refer' | 'array' | 'select' | 'reference' | 'declaration' | 'modify' | 'modassign' | 'assign' | 'IF' | 'loop' | 'jump' | 'compound' | 'call' | 'entry' | 'exit' | 'form-in' | 'form-ellip' | 'form-out' | 'act-in' | 'act-out' | 'monitor' );
	public final SDGNode.Operation node_oper() throws RecognitionException {
		SDGNode.Operation op = null;


		try {
			// /data1/hecker/gits/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDGVertex_.g:291:3: ( 'empty' | 'intconst' | 'floatconst' | 'charconst' | 'stringconst' | 'functionconst' | 'shortcut' | 'question' | 'binary' | 'unary' | 'derefer' | 'refer' | 'array' | 'select' | 'reference' | 'declaration' | 'modify' | 'modassign' | 'assign' | 'IF' | 'loop' | 'jump' | 'compound' | 'call' | 'entry' | 'exit' | 'form-in' | 'form-ellip' | 'form-out' | 'act-in' | 'act-out' | 'monitor' )
			int alt6=32;
			switch ( input.LA(1) ) {
			case 84:
				{
				alt6=1;
				}
				break;
			case 93:
				{
				alt6=2;
				}
				break;
			case 88:
				{
				alt6=3;
				}
				break;
			case 80:
				{
				alt6=4;
				}
				break;
			case 105:
				{
				alt6=5;
				}
				break;
			case 92:
				{
				alt6=6;
				}
				break;
			case 104:
				{
				alt6=7;
				}
				break;
			case 100:
				{
				alt6=8;
				}
				break;
			case 78:
				{
				alt6=9;
				}
				break;
			case 107:
				{
				alt6=10;
				}
				break;
			case 83:
				{
				alt6=11;
				}
				break;
			case 101:
				{
				alt6=12;
				}
				break;
			case 76:
				{
				alt6=13;
				}
				break;
			case 103:
				{
				alt6=14;
				}
				break;
			case 102:
				{
				alt6=15;
				}
				break;
			case 82:
				{
				alt6=16;
				}
				break;
			case 97:
				{
				alt6=17;
				}
				break;
			case 96:
				{
				alt6=18;
				}
				break;
			case 77:
				{
				alt6=19;
				}
				break;
			case 39:
				{
				alt6=20;
				}
				break;
			case 95:
				{
				alt6=21;
				}
				break;
			case 94:
				{
				alt6=22;
				}
				break;
			case 81:
				{
				alt6=23;
				}
				break;
			case 79:
				{
				alt6=24;
				}
				break;
			case 85:
				{
				alt6=25;
				}
				break;
			case 86:
				{
				alt6=26;
				}
				break;
			case 90:
				{
				alt6=27;
				}
				break;
			case 89:
				{
				alt6=28;
				}
				break;
			case 91:
				{
				alt6=29;
				}
				break;
			case 74:
				{
				alt6=30;
				}
				break;
			case 75:
				{
				alt6=31;
				}
				break;
			case 98:
				{
				alt6=32;
				}
				break;
			default:
				NoViableAltException nvae =
					new NoViableAltException("", 6, 0, input);
				throw nvae;
			}
			switch (alt6) {
				case 1 :
					// /data1/hecker/gits/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDGVertex_.g:291:5: 'empty'
					{
					match(input,84,FOLLOW_84_in_node_oper794); 
					 op = SDGNode.Operation.EMPTY; 
					}
					break;
				case 2 :
					// /data1/hecker/gits/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDGVertex_.g:292:5: 'intconst'
					{
					match(input,93,FOLLOW_93_in_node_oper811); 
					 op = SDGNode.Operation.INT_CONST; 
					}
					break;
				case 3 :
					// /data1/hecker/gits/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDGVertex_.g:293:5: 'floatconst'
					{
					match(input,88,FOLLOW_88_in_node_oper825); 
					 op = SDGNode.Operation.FLOAT_CONST; 
					}
					break;
				case 4 :
					// /data1/hecker/gits/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDGVertex_.g:294:5: 'charconst'
					{
					match(input,80,FOLLOW_80_in_node_oper837); 
					 op = SDGNode.Operation.CHAR_CONST; 
					}
					break;
				case 5 :
					// /data1/hecker/gits/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDGVertex_.g:295:5: 'stringconst'
					{
					match(input,105,FOLLOW_105_in_node_oper850); 
					 op = SDGNode.Operation.STRING_CONST; 
					}
					break;
				case 6 :
					// /data1/hecker/gits/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDGVertex_.g:296:5: 'functionconst'
					{
					match(input,92,FOLLOW_92_in_node_oper861); 
					 op = SDGNode.Operation.FUNCTION_CONST; 
					}
					break;
				case 7 :
					// /data1/hecker/gits/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDGVertex_.g:297:5: 'shortcut'
					{
					match(input,104,FOLLOW_104_in_node_oper870); 
					 op = SDGNode.Operation.SHORTCUT; 
					}
					break;
				case 8 :
					// /data1/hecker/gits/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDGVertex_.g:298:5: 'question'
					{
					match(input,100,FOLLOW_100_in_node_oper884); 
					 op = SDGNode.Operation.QUESTION; 
					}
					break;
				case 9 :
					// /data1/hecker/gits/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDGVertex_.g:299:5: 'binary'
					{
					match(input,78,FOLLOW_78_in_node_oper898); 
					 op = SDGNode.Operation.BINARY; 
					}
					break;
				case 10 :
					// /data1/hecker/gits/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDGVertex_.g:300:5: 'unary'
					{
					match(input,107,FOLLOW_107_in_node_oper914); 
					 op = SDGNode.Operation.UNARY; 
					}
					break;
				case 11 :
					// /data1/hecker/gits/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDGVertex_.g:301:5: 'derefer'
					{
					match(input,83,FOLLOW_83_in_node_oper931); 
					 op = SDGNode.Operation.DEREFER; 
					}
					break;
				case 12 :
					// /data1/hecker/gits/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDGVertex_.g:302:5: 'refer'
					{
					match(input,101,FOLLOW_101_in_node_oper946); 
					 op = SDGNode.Operation.REFER; 
					}
					break;
				case 13 :
					// /data1/hecker/gits/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDGVertex_.g:303:5: 'array'
					{
					match(input,76,FOLLOW_76_in_node_oper963); 
					 op = SDGNode.Operation.ARRAY; 
					}
					break;
				case 14 :
					// /data1/hecker/gits/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDGVertex_.g:304:5: 'select'
					{
					match(input,103,FOLLOW_103_in_node_oper980); 
					 op = SDGNode.Operation.SELECT; 
					}
					break;
				case 15 :
					// /data1/hecker/gits/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDGVertex_.g:305:5: 'reference'
					{
					match(input,102,FOLLOW_102_in_node_oper996); 
					 op = SDGNode.Operation.REFERENCE; 
					}
					break;
				case 16 :
					// /data1/hecker/gits/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDGVertex_.g:306:5: 'declaration'
					{
					match(input,82,FOLLOW_82_in_node_oper1009); 
					 op = SDGNode.Operation.DECLARATION; 
					}
					break;
				case 17 :
					// /data1/hecker/gits/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDGVertex_.g:307:5: 'modify'
					{
					match(input,97,FOLLOW_97_in_node_oper1020); 
					 op = SDGNode.Operation.MODIFY; 
					}
					break;
				case 18 :
					// /data1/hecker/gits/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDGVertex_.g:308:5: 'modassign'
					{
					match(input,96,FOLLOW_96_in_node_oper1036); 
					 op = SDGNode.Operation.MODASSIGN; 
					}
					break;
				case 19 :
					// /data1/hecker/gits/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDGVertex_.g:309:5: 'assign'
					{
					match(input,77,FOLLOW_77_in_node_oper1049); 
					 op = SDGNode.Operation.ASSIGN; 
					}
					break;
				case 20 :
					// /data1/hecker/gits/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDGVertex_.g:310:5: 'IF'
					{
					match(input,39,FOLLOW_39_in_node_oper1065); 
					 op = SDGNode.Operation.IF; 
					}
					break;
				case 21 :
					// /data1/hecker/gits/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDGVertex_.g:311:5: 'loop'
					{
					match(input,95,FOLLOW_95_in_node_oper1085); 
					 op = SDGNode.Operation.LOOP; 
					}
					break;
				case 22 :
					// /data1/hecker/gits/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDGVertex_.g:312:5: 'jump'
					{
					match(input,94,FOLLOW_94_in_node_oper1103); 
					 op = SDGNode.Operation.JUMP; 
					}
					break;
				case 23 :
					// /data1/hecker/gits/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDGVertex_.g:313:5: 'compound'
					{
					match(input,81,FOLLOW_81_in_node_oper1121); 
					 op = SDGNode.Operation.COMPOUND; 
					}
					break;
				case 24 :
					// /data1/hecker/gits/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDGVertex_.g:314:5: 'call'
					{
					match(input,79,FOLLOW_79_in_node_oper1135); 
					 op = SDGNode.Operation.CALL; 
					}
					break;
				case 25 :
					// /data1/hecker/gits/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDGVertex_.g:315:5: 'entry'
					{
					match(input,85,FOLLOW_85_in_node_oper1153); 
					 op = SDGNode.Operation.ENTRY; 
					}
					break;
				case 26 :
					// /data1/hecker/gits/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDGVertex_.g:316:5: 'exit'
					{
					match(input,86,FOLLOW_86_in_node_oper1170); 
					 op = SDGNode.Operation.EXIT; 
					}
					break;
				case 27 :
					// /data1/hecker/gits/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDGVertex_.g:317:5: 'form-in'
					{
					match(input,90,FOLLOW_90_in_node_oper1188); 
					 op = SDGNode.Operation.FORMAL_IN; 
					}
					break;
				case 28 :
					// /data1/hecker/gits/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDGVertex_.g:318:5: 'form-ellip'
					{
					match(input,89,FOLLOW_89_in_node_oper1203); 
					 op = SDGNode.Operation.FORMAL_ELLIP; 
					}
					break;
				case 29 :
					// /data1/hecker/gits/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDGVertex_.g:319:5: 'form-out'
					{
					match(input,91,FOLLOW_91_in_node_oper1215); 
					 op = SDGNode.Operation.FORMAL_OUT; 
					}
					break;
				case 30 :
					// /data1/hecker/gits/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDGVertex_.g:320:5: 'act-in'
					{
					match(input,74,FOLLOW_74_in_node_oper1229); 
					 op = SDGNode.Operation.ACTUAL_IN; 
					}
					break;
				case 31 :
					// /data1/hecker/gits/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDGVertex_.g:321:5: 'act-out'
					{
					match(input,75,FOLLOW_75_in_node_oper1245); 
					 op = SDGNode.Operation.ACTUAL_OUT; 
					}
					break;
				case 32 :
					// /data1/hecker/gits/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDGVertex_.g:322:5: 'monitor'
					{
					match(input,98,FOLLOW_98_in_node_oper1260); 
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
	// /data1/hecker/gits/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDGVertex_.g:325:9: private node_edges[SDGNodeStub node] : (e= edge ';' )* ;
	public final void node_edges(SDGNodeStub node) throws RecognitionException {
		SDGEdgeStub e =null;

		try {
			// /data1/hecker/gits/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDGVertex_.g:326:3: ( (e= edge ';' )* )
			// /data1/hecker/gits/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDGVertex_.g:326:5: (e= edge ';' )*
			{
			// /data1/hecker/gits/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDGVertex_.g:326:5: (e= edge ';' )*
			loop7:
			while (true) {
				int alt7=2;
				int LA7_0 = input.LA(1);
				if ( ((LA7_0 >= 17 && LA7_0 <= 21)||(LA7_0 >= 23 && LA7_0 <= 25)||(LA7_0 >= 29 && LA7_0 <= 30)||(LA7_0 >= 32 && LA7_0 <= 34)||(LA7_0 >= 37 && LA7_0 <= 38)||(LA7_0 >= 40 && LA7_0 <= 43)||LA7_0==47||(LA7_0 >= 51 && LA7_0 <= 53)||(LA7_0 >= 55 && LA7_0 <= 58)||(LA7_0 >= 60 && LA7_0 <= 64)||LA7_0==68||LA7_0==70) ) {
					alt7=1;
				}

				switch (alt7) {
				case 1 :
					// /data1/hecker/gits/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDGVertex_.g:326:6: e= edge ';'
					{
					pushFollow(FOLLOW_edge_in_node_edges1288);
					e=edge();
					state._fsp--;

					match(input,10,FOLLOW_10_in_node_edges1290); 
					 node.edges.add(e); 
					}
					break;

				default :
					break loop7;
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
	// /data1/hecker/gits/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDGVertex_.g:329:9: private edge returns [SDGEdgeStub estub] : k= edge_kind nr= number ( ':' label= string )? ;
	public final SDGEdgeStub edge() throws RecognitionException {
		SDGEdgeStub estub = null;


		SDGEdge.Kind k =null;
		int nr =0;
		String label =null;

		try {
			// /data1/hecker/gits/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDGVertex_.g:330:3: (k= edge_kind nr= number ( ':' label= string )? )
			// /data1/hecker/gits/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDGVertex_.g:330:5: k= edge_kind nr= number ( ':' label= string )?
			{
			pushFollow(FOLLOW_edge_kind_in_edge1315);
			k=edge_kind();
			state._fsp--;

			pushFollow(FOLLOW_number_in_edge1319);
			nr=number();
			state._fsp--;

			 estub = new SDGEdgeStub(k, nr); 
			// /data1/hecker/gits/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDGVertex_.g:330:63: ( ':' label= string )?
			int alt8=2;
			int LA8_0 = input.LA(1);
			if ( (LA8_0==9) ) {
				alt8=1;
			}
			switch (alt8) {
				case 1 :
					// /data1/hecker/gits/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDGVertex_.g:330:64: ':' label= string
					{
					match(input,9,FOLLOW_9_in_edge1324); 
					pushFollow(FOLLOW_string_in_edge1328);
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
	// /data1/hecker/gits/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDGVertex_.g:333:9: private edge_kind returns [SDGEdge.Kind kind] : ( 'DD' | 'DH' | 'DA' | 'CD' | 'CE' | 'UN' | 'CF' | 'NF' | 'RF' | 'CC' | 'CL' | 'PI' | 'PO' | 'SU' | 'SH' | 'SF' | 'PS' | 'PE' | 'FORK' | 'FORK_IN' | 'FORK_OUT' | 'JOIN' | 'ID' | 'IW' | 'SD' | 'HE' | 'FD' | 'FI' | 'RY' | 'JF' | 'SP' | 'VD' | 'RD' | 'JD' );
	public final SDGEdge.Kind edge_kind() throws RecognitionException {
		SDGEdge.Kind kind = null;


		try {
			// /data1/hecker/gits/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDGVertex_.g:335:3: ( 'DD' | 'DH' | 'DA' | 'CD' | 'CE' | 'UN' | 'CF' | 'NF' | 'RF' | 'CC' | 'CL' | 'PI' | 'PO' | 'SU' | 'SH' | 'SF' | 'PS' | 'PE' | 'FORK' | 'FORK_IN' | 'FORK_OUT' | 'JOIN' | 'ID' | 'IW' | 'SD' | 'HE' | 'FD' | 'FI' | 'RY' | 'JF' | 'SP' | 'VD' | 'RD' | 'JD' )
			int alt9=34;
			switch ( input.LA(1) ) {
			case 24:
				{
				alt9=1;
				}
				break;
			case 25:
				{
				alt9=2;
				}
				break;
			case 23:
				{
				alt9=3;
				}
				break;
			case 18:
				{
				alt9=4;
				}
				break;
			case 19:
				{
				alt9=5;
				}
				break;
			case 68:
				{
				alt9=6;
				}
				break;
			case 20:
				{
				alt9=7;
				}
				break;
			case 47:
				{
				alt9=8;
				}
				break;
			case 57:
				{
				alt9=9;
				}
				break;
			case 17:
				{
				alt9=10;
				}
				break;
			case 21:
				{
				alt9=11;
				}
				break;
			case 52:
				{
				alt9=12;
				}
				break;
			case 53:
				{
				alt9=13;
				}
				break;
			case 64:
				{
				alt9=14;
				}
				break;
			case 62:
				{
				alt9=15;
				}
				break;
			case 61:
				{
				alt9=16;
				}
				break;
			case 55:
				{
				alt9=17;
				}
				break;
			case 51:
				{
				alt9=18;
				}
				break;
			case 32:
				{
				alt9=19;
				}
				break;
			case 33:
				{
				alt9=20;
				}
				break;
			case 34:
				{
				alt9=21;
				}
				break;
			case 43:
				{
				alt9=22;
				}
				break;
			case 38:
				{
				alt9=23;
				}
				break;
			case 40:
				{
				alt9=24;
				}
				break;
			case 60:
				{
				alt9=25;
				}
				break;
			case 37:
				{
				alt9=26;
				}
				break;
			case 29:
				{
				alt9=27;
				}
				break;
			case 30:
				{
				alt9=28;
				}
				break;
			case 58:
				{
				alt9=29;
				}
				break;
			case 42:
				{
				alt9=30;
				}
				break;
			case 63:
				{
				alt9=31;
				}
				break;
			case 70:
				{
				alt9=32;
				}
				break;
			case 56:
				{
				alt9=33;
				}
				break;
			case 41:
				{
				alt9=34;
				}
				break;
			default:
				NoViableAltException nvae =
					new NoViableAltException("", 9, 0, input);
				throw nvae;
			}
			switch (alt9) {
				case 1 :
					// /data1/hecker/gits/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDGVertex_.g:335:5: 'DD'
					{
					match(input,24,FOLLOW_24_in_edge_kind1353); 
					 kind = SDGEdge.Kind.DATA_DEP; 
					}
					break;
				case 2 :
					// /data1/hecker/gits/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDGVertex_.g:336:5: 'DH'
					{
					match(input,25,FOLLOW_25_in_edge_kind1373); 
					 kind = SDGEdge.Kind.DATA_HEAP; 
					}
					break;
				case 3 :
					// /data1/hecker/gits/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDGVertex_.g:337:5: 'DA'
					{
					match(input,23,FOLLOW_23_in_edge_kind1392); 
					 kind = SDGEdge.Kind.DATA_ALIAS; 
					}
					break;
				case 4 :
					// /data1/hecker/gits/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDGVertex_.g:339:5: 'CD'
					{
					match(input,18,FOLLOW_18_in_edge_kind1411); 
					 kind = SDGEdge.Kind.CONTROL_DEP_COND; 
					}
					break;
				case 5 :
					// /data1/hecker/gits/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDGVertex_.g:340:5: 'CE'
					{
					match(input,19,FOLLOW_19_in_edge_kind1423); 
					 kind = SDGEdge.Kind.CONTROL_DEP_EXPR; 
					}
					break;
				case 6 :
					// /data1/hecker/gits/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDGVertex_.g:341:5: 'UN'
					{
					match(input,68,FOLLOW_68_in_edge_kind1435); 
					 kind = SDGEdge.Kind.CONTROL_DEP_UNCOND; 
					}
					break;
				case 7 :
					// /data1/hecker/gits/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDGVertex_.g:343:5: 'CF'
					{
					match(input,20,FOLLOW_20_in_edge_kind1446); 
					 kind = SDGEdge.Kind.CONTROL_FLOW; 
					}
					break;
				case 8 :
					// /data1/hecker/gits/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDGVertex_.g:344:5: 'NF'
					{
					match(input,47,FOLLOW_47_in_edge_kind1462); 
					 kind = SDGEdge.Kind.NO_FLOW; 
					}
					break;
				case 9 :
					// /data1/hecker/gits/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDGVertex_.g:345:5: 'RF'
					{
					match(input,57,FOLLOW_57_in_edge_kind1483); 
					 kind = SDGEdge.Kind.RETURN; 
					}
					break;
				case 10 :
					// /data1/hecker/gits/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDGVertex_.g:347:5: 'CC'
					{
					match(input,17,FOLLOW_17_in_edge_kind1506); 
					 kind = SDGEdge.Kind.CONTROL_DEP_CALL; 
					}
					break;
				case 11 :
					// /data1/hecker/gits/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDGVertex_.g:348:5: 'CL'
					{
					match(input,21,FOLLOW_21_in_edge_kind1514); 
					 kind = SDGEdge.Kind.CALL; 
					}
					break;
				case 12 :
					// /data1/hecker/gits/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDGVertex_.g:349:5: 'PI'
					{
					match(input,52,FOLLOW_52_in_edge_kind1522); 
					 kind = SDGEdge.Kind.PARAMETER_IN; 
					}
					break;
				case 13 :
					// /data1/hecker/gits/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDGVertex_.g:350:5: 'PO'
					{
					match(input,53,FOLLOW_53_in_edge_kind1530); 
					 kind = SDGEdge.Kind.PARAMETER_OUT; 
					}
					break;
				case 14 :
					// /data1/hecker/gits/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDGVertex_.g:352:5: 'SU'
					{
					match(input,64,FOLLOW_64_in_edge_kind1539); 
					 kind = SDGEdge.Kind.SUMMARY; 
					}
					break;
				case 15 :
					// /data1/hecker/gits/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDGVertex_.g:353:5: 'SH'
					{
					match(input,62,FOLLOW_62_in_edge_kind1547); 
					 kind = SDGEdge.Kind.SUMMARY_NO_ALIAS; 
					}
					break;
				case 16 :
					// /data1/hecker/gits/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDGVertex_.g:354:5: 'SF'
					{
					match(input,61,FOLLOW_61_in_edge_kind1555); 
					 kind = SDGEdge.Kind.SUMMARY_DATA; 
					}
					break;
				case 17 :
					// /data1/hecker/gits/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDGVertex_.g:356:5: 'PS'
					{
					match(input,55,FOLLOW_55_in_edge_kind1564); 
					 kind = SDGEdge.Kind.PARAMETER_STRUCTURE; 
					}
					break;
				case 18 :
					// /data1/hecker/gits/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDGVertex_.g:357:5: 'PE'
					{
					match(input,51,FOLLOW_51_in_edge_kind1572); 
					 kind = SDGEdge.Kind.PARAMETER_EQUIVALENCE; 
					}
					break;
				case 19 :
					// /data1/hecker/gits/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDGVertex_.g:359:5: 'FORK'
					{
					match(input,32,FOLLOW_32_in_edge_kind1581); 
					 kind = SDGEdge.Kind.FORK; 
					}
					break;
				case 20 :
					// /data1/hecker/gits/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDGVertex_.g:360:5: 'FORK_IN'
					{
					match(input,33,FOLLOW_33_in_edge_kind1589); 
					 kind = SDGEdge.Kind.FORK_IN; 
					}
					break;
				case 21 :
					// /data1/hecker/gits/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDGVertex_.g:361:5: 'FORK_OUT'
					{
					match(input,34,FOLLOW_34_in_edge_kind1597); 
					 kind = SDGEdge.Kind.FORK_OUT; 
					}
					break;
				case 22 :
					// /data1/hecker/gits/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDGVertex_.g:362:5: 'JOIN'
					{
					match(input,43,FOLLOW_43_in_edge_kind1605); 
					 kind = SDGEdge.Kind.JOIN; 
					}
					break;
				case 23 :
					// /data1/hecker/gits/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDGVertex_.g:363:5: 'ID'
					{
					match(input,38,FOLLOW_38_in_edge_kind1613); 
					 kind = SDGEdge.Kind.INTERFERENCE; 
					}
					break;
				case 24 :
					// /data1/hecker/gits/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDGVertex_.g:364:5: 'IW'
					{
					match(input,40,FOLLOW_40_in_edge_kind1621); 
					 kind = SDGEdge.Kind.INTERFERENCE_WRITE; 
					}
					break;
				case 25 :
					// /data1/hecker/gits/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDGVertex_.g:365:5: 'SD'
					{
					match(input,60,FOLLOW_60_in_edge_kind1629); 
					 kind = SDGEdge.Kind.SYNCHRONIZATION; 
					}
					break;
				case 26 :
					// /data1/hecker/gits/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDGVertex_.g:367:5: 'HE'
					{
					match(input,37,FOLLOW_37_in_edge_kind1638); 
					 kind = SDGEdge.Kind.HELP; 
					}
					break;
				case 27 :
					// /data1/hecker/gits/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDGVertex_.g:368:5: 'FD'
					{
					match(input,29,FOLLOW_29_in_edge_kind1646); 
					 kind = SDGEdge.Kind.FOLDED; 
					}
					break;
				case 28 :
					// /data1/hecker/gits/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDGVertex_.g:369:5: 'FI'
					{
					match(input,30,FOLLOW_30_in_edge_kind1654); 
					 kind = SDGEdge.Kind.FOLD_INCLUDE; 
					}
					break;
				case 29 :
					// /data1/hecker/gits/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDGVertex_.g:371:5: 'RY'
					{
					match(input,58,FOLLOW_58_in_edge_kind1663); 
					 kind = SDGEdge.Kind.READY_DEP; 
					}
					break;
				case 30 :
					// /data1/hecker/gits/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDGVertex_.g:372:5: 'JF'
					{
					match(input,42,FOLLOW_42_in_edge_kind1671); 
					 kind = SDGEdge.Kind.JUMP_FLOW; 
					}
					break;
				case 31 :
					// /data1/hecker/gits/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDGVertex_.g:373:5: 'SP'
					{
					match(input,63,FOLLOW_63_in_edge_kind1679); 
					 kind = SDGEdge.Kind.SUMMARY; 
					}
					break;
				case 32 :
					// /data1/hecker/gits/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDGVertex_.g:374:5: 'VD'
					{
					match(input,70,FOLLOW_70_in_edge_kind1687); 
					 kind = SDGEdge.Kind.DATA_DEP_EXPR_VALUE; 
					}
					break;
				case 33 :
					// /data1/hecker/gits/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDGVertex_.g:375:5: 'RD'
					{
					match(input,56,FOLLOW_56_in_edge_kind1695); 
					 kind = SDGEdge.Kind.DATA_DEP_EXPR_REFERENCE; 
					}
					break;
				case 34 :
					// /data1/hecker/gits/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDGVertex_.g:376:5: 'JD'
					{
					match(input,41,FOLLOW_41_in_edge_kind1703); 
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
	// /data1/hecker/gits/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDGVertex_.g:379:9: private mayNegNumber returns [int nr] : ( '-' n= number |n= number );
	public final int mayNegNumber() throws RecognitionException {
		int nr = 0;


		int n =0;

		try {
			// /data1/hecker/gits/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDGVertex_.g:380:3: ( '-' n= number |n= number )
			int alt10=2;
			int LA10_0 = input.LA(1);
			if ( (LA10_0==8) ) {
				alt10=1;
			}
			else if ( (LA10_0==NUMBER) ) {
				alt10=2;
			}

			else {
				NoViableAltException nvae =
					new NoViableAltException("", 10, 0, input);
				throw nvae;
			}

			switch (alt10) {
				case 1 :
					// /data1/hecker/gits/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDGVertex_.g:380:5: '-' n= number
					{
					match(input,8,FOLLOW_8_in_mayNegNumber1724); 
					pushFollow(FOLLOW_number_in_mayNegNumber1728);
					n=number();
					state._fsp--;

					 nr = -n; 
					}
					break;
				case 2 :
					// /data1/hecker/gits/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDGVertex_.g:381:5: n= number
					{
					pushFollow(FOLLOW_number_in_mayNegNumber1738);
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
	// /data1/hecker/gits/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDGVertex_.g:384:9: private number returns [int nr] : n= NUMBER ;
	public final int number() throws RecognitionException {
		int nr = 0;


		Token n=null;

		try {
			// /data1/hecker/gits/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDGVertex_.g:385:3: (n= NUMBER )
			// /data1/hecker/gits/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDGVertex_.g:385:5: n= NUMBER
			{
			n=(Token)match(input,NUMBER,FOLLOW_NUMBER_in_number1761); 
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
	// /data1/hecker/gits/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDGVertex_.g:388:9: private string returns [String str] : s= STRING ;
	public final String string() throws RecognitionException {
		String str = null;


		Token s=null;

		try {
			// /data1/hecker/gits/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDGVertex_.g:389:3: (s= STRING )
			// /data1/hecker/gits/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDGVertex_.g:389:5: s= STRING
			{
			s=(Token)match(input,STRING,FOLLOW_STRING_in_string1784); 
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



	// $ANTLR start "mayEmptyStringList"
	// /data1/hecker/gits/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDGVertex_.g:392:9: private mayEmptyStringList returns [LinkedList<String> ss = new LinkedList<String>();] : ( 'null' | '[' ']' | '[' s= string ( ',' s= string )* ']' );
	public final LinkedList<String> mayEmptyStringList() throws RecognitionException {
		LinkedList<String> ss =  new LinkedList<String>();;


		String s =null;

		try {
			// /data1/hecker/gits/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDGVertex_.g:393:3: ( 'null' | '[' ']' | '[' s= string ( ',' s= string )* ']' )
			int alt12=3;
			int LA12_0 = input.LA(1);
			if ( (LA12_0==99) ) {
				alt12=1;
			}
			else if ( (LA12_0==72) ) {
				int LA12_2 = input.LA(2);
				if ( (LA12_2==73) ) {
					alt12=2;
				}
				else if ( (LA12_2==STRING) ) {
					alt12=3;
				}

				else {
					int nvaeMark = input.mark();
					try {
						input.consume();
						NoViableAltException nvae =
							new NoViableAltException("", 12, 2, input);
						throw nvae;
					} finally {
						input.rewind(nvaeMark);
					}
				}

			}

			else {
				NoViableAltException nvae =
					new NoViableAltException("", 12, 0, input);
				throw nvae;
			}

			switch (alt12) {
				case 1 :
					// /data1/hecker/gits/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDGVertex_.g:393:5: 'null'
					{
					match(input,99,FOLLOW_99_in_mayEmptyStringList1805); 
					}
					break;
				case 2 :
					// /data1/hecker/gits/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDGVertex_.g:394:5: '[' ']'
					{
					match(input,72,FOLLOW_72_in_mayEmptyStringList1811); 
					match(input,73,FOLLOW_73_in_mayEmptyStringList1813); 
					}
					break;
				case 3 :
					// /data1/hecker/gits/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDGVertex_.g:395:5: '[' s= string ( ',' s= string )* ']'
					{
					match(input,72,FOLLOW_72_in_mayEmptyStringList1819); 
					pushFollow(FOLLOW_string_in_mayEmptyStringList1823);
					s=string();
					state._fsp--;

					 ss.add(s); 
					// /data1/hecker/gits/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDGVertex_.g:395:33: ( ',' s= string )*
					loop11:
					while (true) {
						int alt11=2;
						int LA11_0 = input.LA(1);
						if ( (LA11_0==7) ) {
							alt11=1;
						}

						switch (alt11) {
						case 1 :
							// /data1/hecker/gits/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDGVertex_.g:395:34: ',' s= string
							{
							match(input,7,FOLLOW_7_in_mayEmptyStringList1828); 
							pushFollow(FOLLOW_string_in_mayEmptyStringList1832);
							s=string();
							state._fsp--;

							 ss.add(s); 
							}
							break;

						default :
							break loop11;
						}
					}

					match(input,73,FOLLOW_73_in_mayEmptyStringList1839); 
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



	// $ANTLR start "bool"
	// /data1/hecker/gits/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDGVertex_.g:398:9: private bool returns [boolean b] : ( 'true' | 'false' );
	public final boolean bool() throws RecognitionException {
		boolean b = false;


		try {
			// /data1/hecker/gits/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDGVertex_.g:399:3: ( 'true' | 'false' )
			int alt13=2;
			int LA13_0 = input.LA(1);
			if ( (LA13_0==106) ) {
				alt13=1;
			}
			else if ( (LA13_0==87) ) {
				alt13=2;
			}

			else {
				NoViableAltException nvae =
					new NoViableAltException("", 13, 0, input);
				throw nvae;
			}

			switch (alt13) {
				case 1 :
					// /data1/hecker/gits/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDGVertex_.g:399:5: 'true'
					{
					match(input,106,FOLLOW_106_in_bool1858); 
					 b = true; 
					}
					break;
				case 2 :
					// /data1/hecker/gits/joana/ifc/sdg/joana.ifc.sdg.graph/src/edu/kit/joana/ifc/sdg/graph/SDGVertex_.g:400:5: 'false'
					{
					match(input,87,FOLLOW_87_in_bool1867); 
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



	public static final BitSet FOLLOW_node_kind_in_node73 = new BitSet(new long[]{0x0000000000000110L});
	public static final BitSet FOLLOW_mayNegNumber_in_node77 = new BitSet(new long[]{0x0000000000000000L,0x0000100000000000L});
	public static final BitSet FOLLOW_108_in_node86 = new BitSet(new long[]{0xFFBEFF6763FEC800L,0x00002000000000FDL});
	public static final BitSet FOLLOW_node_attributes_in_node95 = new BitSet(new long[]{0xF7B88F6763BE0000L,0x0000200000000051L});
	public static final BitSet FOLLOW_node_edges_in_node105 = new BitSet(new long[]{0x0000000000000000L,0x0000200000000000L});
	public static final BitSet FOLLOW_109_in_node112 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_48_in_node_kind131 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_54_in_node_kind139 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_28_in_node_kind147 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_26_in_node_kind155 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_16_in_node_kind163 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_12_in_node_kind171 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_13_in_node_kind179 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_35_in_node_kind187 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_36_in_node_kind195 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_27_in_node_kind203 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_65_in_node_kind211 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_31_in_node_kind219 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_node_attr_in_node_attributes238 = new BitSet(new long[]{0x0000000000000400L});
	public static final BitSet FOLLOW_10_in_node_attributes241 = new BitSet(new long[]{0x080670000040C802L,0x00000000000000ACL});
	public static final BitSet FOLLOW_59_in_node_attr259 = new BitSet(new long[]{0x0000000000000020L});
	public static final BitSet FOLLOW_node_source_in_node_attr263 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_14_in_node_attr275 = new BitSet(new long[]{0x0000000000000020L});
	public static final BitSet FOLLOW_node_bytecode_in_node_attr279 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_67_in_node_attr290 = new BitSet(new long[]{0x0000000000000010L});
	public static final BitSet FOLLOW_number_in_node_attr292 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_50_in_node_attr342 = new BitSet(new long[]{0x0000000000000010L});
	public static final BitSet FOLLOW_number_in_node_attr346 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_49_in_node_attr365 = new BitSet(new long[]{0x0000008000000000L,0x00000BF7FF7FFC00L});
	public static final BitSet FOLLOW_node_oper_in_node_attr369 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_69_in_node_attr397 = new BitSet(new long[]{0x0000000000000020L});
	public static final BitSet FOLLOW_string_in_node_attr401 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_66_in_node_attr429 = new BitSet(new long[]{0x0000000000000020L});
	public static final BitSet FOLLOW_string_in_node_attr433 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_71_in_node_attr458 = new BitSet(new long[]{0x0000000000000110L});
	public static final BitSet FOLLOW_may_neg_num_set_in_node_attr462 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_46_in_node_attr476 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_15_in_node_attr510 = new BitSet(new long[]{0x0000000000000020L});
	public static final BitSet FOLLOW_string_in_node_attr514 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_11_in_node_attr536 = new BitSet(new long[]{0x0000000000000010L});
	public static final BitSet FOLLOW_pos_num_set_in_node_attr540 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_22_in_node_attr558 = new BitSet(new long[]{0x0000000000000010L});
	public static final BitSet FOLLOW_pos_num_set_in_node_attr562 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_67_in_node_attr579 = new BitSet(new long[]{0x0000000000000020L});
	public static final BitSet FOLLOW_string_in_node_attr583 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_44_in_node_attr601 = new BitSet(new long[]{0x0000000000000000L,0x0000000800000100L});
	public static final BitSet FOLLOW_mayEmptyStringList_in_node_attr605 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_45_in_node_attr614 = new BitSet(new long[]{0x0000000000000000L,0x0000000800000100L});
	public static final BitSet FOLLOW_mayEmptyStringList_in_node_attr618 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_number_in_pos_num_set642 = new BitSet(new long[]{0x0000000000000082L});
	public static final BitSet FOLLOW_7_in_pos_num_set647 = new BitSet(new long[]{0x0000000000000010L});
	public static final BitSet FOLLOW_number_in_pos_num_set651 = new BitSet(new long[]{0x0000000000000082L});
	public static final BitSet FOLLOW_mayNegNumber_in_may_neg_num_set677 = new BitSet(new long[]{0x0000000000000082L});
	public static final BitSet FOLLOW_7_in_may_neg_num_set682 = new BitSet(new long[]{0x0000000000000110L});
	public static final BitSet FOLLOW_mayNegNumber_in_may_neg_num_set686 = new BitSet(new long[]{0x0000000000000082L});
	public static final BitSet FOLLOW_string_in_node_source712 = new BitSet(new long[]{0x0000000000000200L});
	public static final BitSet FOLLOW_9_in_node_source714 = new BitSet(new long[]{0x0000000000000010L});
	public static final BitSet FOLLOW_number_in_node_source718 = new BitSet(new long[]{0x0000000000000080L});
	public static final BitSet FOLLOW_7_in_node_source720 = new BitSet(new long[]{0x0000000000000010L});
	public static final BitSet FOLLOW_number_in_node_source724 = new BitSet(new long[]{0x0000000000000100L});
	public static final BitSet FOLLOW_8_in_node_source726 = new BitSet(new long[]{0x0000000000000010L});
	public static final BitSet FOLLOW_number_in_node_source730 = new BitSet(new long[]{0x0000000000000080L});
	public static final BitSet FOLLOW_7_in_node_source732 = new BitSet(new long[]{0x0000000000000010L});
	public static final BitSet FOLLOW_number_in_node_source736 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_string_in_node_bytecode767 = new BitSet(new long[]{0x0000000000000200L});
	public static final BitSet FOLLOW_9_in_node_bytecode769 = new BitSet(new long[]{0x0000000000000110L});
	public static final BitSet FOLLOW_mayNegNumber_in_node_bytecode773 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_84_in_node_oper794 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_93_in_node_oper811 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_88_in_node_oper825 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_80_in_node_oper837 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_105_in_node_oper850 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_92_in_node_oper861 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_104_in_node_oper870 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_100_in_node_oper884 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_78_in_node_oper898 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_107_in_node_oper914 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_83_in_node_oper931 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_101_in_node_oper946 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_76_in_node_oper963 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_103_in_node_oper980 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_102_in_node_oper996 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_82_in_node_oper1009 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_97_in_node_oper1020 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_96_in_node_oper1036 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_77_in_node_oper1049 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_39_in_node_oper1065 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_95_in_node_oper1085 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_94_in_node_oper1103 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_81_in_node_oper1121 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_79_in_node_oper1135 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_85_in_node_oper1153 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_86_in_node_oper1170 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_90_in_node_oper1188 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_89_in_node_oper1203 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_91_in_node_oper1215 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_74_in_node_oper1229 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_75_in_node_oper1245 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_98_in_node_oper1260 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_edge_in_node_edges1288 = new BitSet(new long[]{0x0000000000000400L});
	public static final BitSet FOLLOW_10_in_node_edges1290 = new BitSet(new long[]{0xF7B88F6763BE0002L,0x0000000000000051L});
	public static final BitSet FOLLOW_edge_kind_in_edge1315 = new BitSet(new long[]{0x0000000000000010L});
	public static final BitSet FOLLOW_number_in_edge1319 = new BitSet(new long[]{0x0000000000000202L});
	public static final BitSet FOLLOW_9_in_edge1324 = new BitSet(new long[]{0x0000000000000020L});
	public static final BitSet FOLLOW_string_in_edge1328 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_24_in_edge_kind1353 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_25_in_edge_kind1373 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_23_in_edge_kind1392 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_18_in_edge_kind1411 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_19_in_edge_kind1423 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_68_in_edge_kind1435 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_20_in_edge_kind1446 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_47_in_edge_kind1462 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_57_in_edge_kind1483 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_17_in_edge_kind1506 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_21_in_edge_kind1514 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_52_in_edge_kind1522 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_53_in_edge_kind1530 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_64_in_edge_kind1539 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_62_in_edge_kind1547 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_61_in_edge_kind1555 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_55_in_edge_kind1564 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_51_in_edge_kind1572 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_32_in_edge_kind1581 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_33_in_edge_kind1589 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_34_in_edge_kind1597 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_43_in_edge_kind1605 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_38_in_edge_kind1613 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_40_in_edge_kind1621 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_60_in_edge_kind1629 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_37_in_edge_kind1638 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_29_in_edge_kind1646 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_30_in_edge_kind1654 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_58_in_edge_kind1663 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_42_in_edge_kind1671 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_63_in_edge_kind1679 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_70_in_edge_kind1687 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_56_in_edge_kind1695 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_41_in_edge_kind1703 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_8_in_mayNegNumber1724 = new BitSet(new long[]{0x0000000000000010L});
	public static final BitSet FOLLOW_number_in_mayNegNumber1728 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_number_in_mayNegNumber1738 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_NUMBER_in_number1761 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_STRING_in_string1784 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_99_in_mayEmptyStringList1805 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_72_in_mayEmptyStringList1811 = new BitSet(new long[]{0x0000000000000000L,0x0000000000000200L});
	public static final BitSet FOLLOW_73_in_mayEmptyStringList1813 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_72_in_mayEmptyStringList1819 = new BitSet(new long[]{0x0000000000000020L});
	public static final BitSet FOLLOW_string_in_mayEmptyStringList1823 = new BitSet(new long[]{0x0000000000000080L,0x0000000000000200L});
	public static final BitSet FOLLOW_7_in_mayEmptyStringList1828 = new BitSet(new long[]{0x0000000000000020L});
	public static final BitSet FOLLOW_string_in_mayEmptyStringList1832 = new BitSet(new long[]{0x0000000000000080L,0x0000000000000200L});
	public static final BitSet FOLLOW_73_in_mayEmptyStringList1839 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_106_in_bool1858 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_87_in_bool1867 = new BitSet(new long[]{0x0000000000000002L});
}
