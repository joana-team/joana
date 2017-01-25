// $ANTLR 3.1.3 Mar 17, 2009 19:23:44 /Users/jgf/Documents/Projects/joana/wala/joana.wala.flowless/src/edu/kit/joana/wala/flowless/spec/FlowLess.g 2013-03-11 18:55:31
/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.wala.flowless.spec;

import java.util.LinkedList;
import java.util.List;

import org.antlr.runtime.BitSet;
import org.antlr.runtime.MismatchedSetException;
import org.antlr.runtime.NoViableAltException;
import org.antlr.runtime.Parser;
import org.antlr.runtime.RecognitionException;
import org.antlr.runtime.RecognizerSharedState;
import org.antlr.runtime.Token;
import org.antlr.runtime.TokenStream;

import edu.kit.joana.wala.flowless.spec.ast.AliasStmt;
import edu.kit.joana.wala.flowless.spec.ast.BooleanAliasStmt;
import edu.kit.joana.wala.flowless.spec.ast.ExplicitFlowStmt;
import edu.kit.joana.wala.flowless.spec.ast.FlowStmt;
import edu.kit.joana.wala.flowless.spec.ast.IFCStmt;
import edu.kit.joana.wala.flowless.spec.ast.InferableAliasStmt;
import edu.kit.joana.wala.flowless.spec.ast.Parameter;
import edu.kit.joana.wala.flowless.spec.ast.ParameterOptList;
import edu.kit.joana.wala.flowless.spec.ast.PrimitiveAliasStmt;
import edu.kit.joana.wala.flowless.spec.ast.PureStmt;
import edu.kit.joana.wala.flowless.spec.ast.SimpleParameter;
import edu.kit.joana.wala.flowless.spec.ast.UniqueStmt;

/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
public class FlowLessParser extends Parser {
    public static final String[] tokenNames = new String[] {
        "<invalid>", "<EOR>", "<DOWN>", "<UP>", "IMPLIES", "ALIAS_OR", "ALIAS_AND", "ALIAS_NOT", "ARROW", "NOT_ARROW", "WILDCARD", "RESULT", "EXC", "STATE", "IDENT", "ARRAY", "WS_", "'?'", "'alias'", "'('", "')'", "'{'", "'}'", "'unique'", "'1'", "'uniq'", "','", "'pure'", "'.'", "'['", "']'"
    };
    public static final int STATE=13;
    public static final int T__29=29;
    public static final int T__28=28;
    public static final int IMPLIES=4;
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
    public static final int T__19=19;
    public static final int T__30=30;
    public static final int WS_=16;
    public static final int EXC=12;
    public static final int T__18=18;
    public static final int T__17=17;
    public static final int WILDCARD=10;
    public static final int IDENT=14;
    public static final int ARROW=8;
    public static final int ARRAY=15;

    // delegates
    // delegators


        public FlowLessParser(TokenStream input) {
            this(input, new RecognizerSharedState());
        }
        public FlowLessParser(TokenStream input, RecognizerSharedState state) {
            super(input, state);
             
        }
        

    public String[] getTokenNames() { return FlowLessParser.tokenNames; }
    public String getGrammarFileName() { return "/Users/jgf/Documents/Projects/joana/wala/joana.wala.flowless/src/edu/kit/joana/wala/flowless/spec/FlowLess.g"; }


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




    // $ANTLR start "ifc_stmt"
    // /Users/jgf/Documents/Projects/joana/wala/joana.wala.flowless/src/edu/kit/joana/wala/flowless/spec/FlowLess.g:116:1: ifc_stmt returns [IFCStmt ifc] : (a= alias_stmts ( IMPLIES f= flow_stmts )? | IMPLIES f= flow_stmts | '?' IMPLIES f= flow_stmts );
    public final IFCStmt ifc_stmt() throws RecognitionException {
        IFCStmt ifc = null;

        AliasStmt a = null;

        List<FlowStmt> f = null;


        try {
            // /Users/jgf/Documents/Projects/joana/wala/joana.wala.flowless/src/edu/kit/joana/wala/flowless/spec/FlowLess.g:117:2: (a= alias_stmts ( IMPLIES f= flow_stmts )? | IMPLIES f= flow_stmts | '?' IMPLIES f= flow_stmts )
            int alt2=3;
            switch ( input.LA(1) ) {
            case ALIAS_NOT:
            case 18:
            case 19:
            case 21:
            case 23:
            case 24:
            case 25:
                {
                alt2=1;
                }
                break;
            case IMPLIES:
                {
                alt2=2;
                }
                break;
            case 17:
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
                    // /Users/jgf/Documents/Projects/joana/wala/joana.wala.flowless/src/edu/kit/joana/wala/flowless/spec/FlowLess.g:117:4: a= alias_stmts ( IMPLIES f= flow_stmts )?
                    {
                    pushFollow(FOLLOW_alias_stmts_in_ifc_stmt65);
                    a=alias_stmts();

                    state._fsp--;

                    // /Users/jgf/Documents/Projects/joana/wala/joana.wala.flowless/src/edu/kit/joana/wala/flowless/spec/FlowLess.g:117:18: ( IMPLIES f= flow_stmts )?
                    int alt1=2;
                    int LA1_0 = input.LA(1);

                    if ( (LA1_0==IMPLIES) ) {
                        alt1=1;
                    }
                    switch (alt1) {
                        case 1 :
                            // /Users/jgf/Documents/Projects/joana/wala/joana.wala.flowless/src/edu/kit/joana/wala/flowless/spec/FlowLess.g:117:19: IMPLIES f= flow_stmts
                            {
                            match(input,IMPLIES,FOLLOW_IMPLIES_in_ifc_stmt68); 
                            pushFollow(FOLLOW_flow_stmts_in_ifc_stmt72);
                            f=flow_stmts();

                            state._fsp--;


                            }
                            break;

                    }

                     ifc = new IFCStmt(a, f); 

                    }
                    break;
                case 2 :
                    // /Users/jgf/Documents/Projects/joana/wala/joana.wala.flowless/src/edu/kit/joana/wala/flowless/spec/FlowLess.g:118:5: IMPLIES f= flow_stmts
                    {
                    match(input,IMPLIES,FOLLOW_IMPLIES_in_ifc_stmt82); 
                    pushFollow(FOLLOW_flow_stmts_in_ifc_stmt86);
                    f=flow_stmts();

                    state._fsp--;

                     ifc = new IFCStmt(null, f); 

                    }
                    break;
                case 3 :
                    // /Users/jgf/Documents/Projects/joana/wala/joana.wala.flowless/src/edu/kit/joana/wala/flowless/spec/FlowLess.g:119:5: '?' IMPLIES f= flow_stmts
                    {
                    match(input,17,FOLLOW_17_in_ifc_stmt94); 
                    match(input,IMPLIES,FOLLOW_IMPLIES_in_ifc_stmt96); 
                    pushFollow(FOLLOW_flow_stmts_in_ifc_stmt100);
                    f=flow_stmts();

                    state._fsp--;

                     ifc = new IFCStmt(InferableAliasStmt.getInstance(), f); 

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
        return ifc;
    }
    // $ANTLR end "ifc_stmt"


    // $ANTLR start "alias_stmts"
    // /Users/jgf/Documents/Projects/joana/wala/joana.wala.flowless/src/edu/kit/joana/wala/flowless/spec/FlowLess.g:122:1: alias_stmts returns [AliasStmt alias] : a= alias_or ;
    public final AliasStmt alias_stmts() throws RecognitionException {
        AliasStmt alias = null;

        AliasStmt a = null;


        try {
            // /Users/jgf/Documents/Projects/joana/wala/joana.wala.flowless/src/edu/kit/joana/wala/flowless/spec/FlowLess.g:123:2: (a= alias_or )
            // /Users/jgf/Documents/Projects/joana/wala/joana.wala.flowless/src/edu/kit/joana/wala/flowless/spec/FlowLess.g:123:4: a= alias_or
            {
            pushFollow(FOLLOW_alias_or_in_alias_stmts119);
            a=alias_or();

            state._fsp--;

             alias = a; 

            }

        }
        catch (RecognitionException re) {
            reportError(re);
            recover(input,re);
        }
        finally {
        }
        return alias;
    }
    // $ANTLR end "alias_stmts"


    // $ANTLR start "alias_or"
    // /Users/jgf/Documents/Projects/joana/wala/joana.wala.flowless/src/edu/kit/joana/wala/flowless/spec/FlowLess.g:126:1: alias_or returns [AliasStmt alias] : a= alias_and ( ALIAS_OR b= alias_or )? ;
    public final AliasStmt alias_or() throws RecognitionException {
        AliasStmt alias = null;

        AliasStmt a = null;

        AliasStmt b = null;


        try {
            // /Users/jgf/Documents/Projects/joana/wala/joana.wala.flowless/src/edu/kit/joana/wala/flowless/spec/FlowLess.g:127:2: (a= alias_and ( ALIAS_OR b= alias_or )? )
            // /Users/jgf/Documents/Projects/joana/wala/joana.wala.flowless/src/edu/kit/joana/wala/flowless/spec/FlowLess.g:127:4: a= alias_and ( ALIAS_OR b= alias_or )?
            {
            pushFollow(FOLLOW_alias_and_in_alias_or138);
            a=alias_and();

            state._fsp--;

             alias = a; 
            // /Users/jgf/Documents/Projects/joana/wala/joana.wala.flowless/src/edu/kit/joana/wala/flowless/spec/FlowLess.g:127:31: ( ALIAS_OR b= alias_or )?
            int alt3=2;
            int LA3_0 = input.LA(1);

            if ( (LA3_0==ALIAS_OR) ) {
                alt3=1;
            }
            switch (alt3) {
                case 1 :
                    // /Users/jgf/Documents/Projects/joana/wala/joana.wala.flowless/src/edu/kit/joana/wala/flowless/spec/FlowLess.g:127:32: ALIAS_OR b= alias_or
                    {
                    match(input,ALIAS_OR,FOLLOW_ALIAS_OR_in_alias_or143); 
                    pushFollow(FOLLOW_alias_or_in_alias_or147);
                    b=alias_or();

                    state._fsp--;

                     alias = new BooleanAliasStmt(alias, b, BooleanAliasStmt.Operator.OR); 

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
        }
        return alias;
    }
    // $ANTLR end "alias_or"


    // $ANTLR start "alias_and"
    // /Users/jgf/Documents/Projects/joana/wala/joana.wala.flowless/src/edu/kit/joana/wala/flowless/spec/FlowLess.g:130:1: alias_and returns [AliasStmt alias] : a= alias_stmt ( ALIAS_AND b= alias_and )? ;
    public final AliasStmt alias_and() throws RecognitionException {
        AliasStmt alias = null;

        AliasStmt a = null;

        AliasStmt b = null;


        try {
            // /Users/jgf/Documents/Projects/joana/wala/joana.wala.flowless/src/edu/kit/joana/wala/flowless/spec/FlowLess.g:131:2: (a= alias_stmt ( ALIAS_AND b= alias_and )? )
            // /Users/jgf/Documents/Projects/joana/wala/joana.wala.flowless/src/edu/kit/joana/wala/flowless/spec/FlowLess.g:131:4: a= alias_stmt ( ALIAS_AND b= alias_and )?
            {
            pushFollow(FOLLOW_alias_stmt_in_alias_and168);
            a=alias_stmt();

            state._fsp--;

             alias = a; 
            // /Users/jgf/Documents/Projects/joana/wala/joana.wala.flowless/src/edu/kit/joana/wala/flowless/spec/FlowLess.g:131:32: ( ALIAS_AND b= alias_and )?
            int alt4=2;
            int LA4_0 = input.LA(1);

            if ( (LA4_0==ALIAS_AND) ) {
                alt4=1;
            }
            switch (alt4) {
                case 1 :
                    // /Users/jgf/Documents/Projects/joana/wala/joana.wala.flowless/src/edu/kit/joana/wala/flowless/spec/FlowLess.g:131:33: ALIAS_AND b= alias_and
                    {
                    match(input,ALIAS_AND,FOLLOW_ALIAS_AND_in_alias_and173); 
                    pushFollow(FOLLOW_alias_and_in_alias_and177);
                    b=alias_and();

                    state._fsp--;

                     alias = new BooleanAliasStmt(alias, b, BooleanAliasStmt.Operator.AND); 

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
        }
        return alias;
    }
    // $ANTLR end "alias_and"


    // $ANTLR start "alias_stmt"
    // /Users/jgf/Documents/Projects/joana/wala/joana.wala.flowless/src/edu/kit/joana/wala/flowless/spec/FlowLess.g:134:1: alias_stmt returns [AliasStmt alias] : ( ( ALIAS_NOT )? 'alias' '(' p= params ')' | ( ALIAS_NOT )? '{' p= params '}' | ( 'unique' | '1' | 'uniq' ) '(' p= params ')' | '(' a= alias_stmts ')' );
    public final AliasStmt alias_stmt() throws RecognitionException {
        AliasStmt alias = null;

        List<Parameter> p = null;

        AliasStmt a = null;


        try {
            // /Users/jgf/Documents/Projects/joana/wala/joana.wala.flowless/src/edu/kit/joana/wala/flowless/spec/FlowLess.g:135:2: ( ( ALIAS_NOT )? 'alias' '(' p= params ')' | ( ALIAS_NOT )? '{' p= params '}' | ( 'unique' | '1' | 'uniq' ) '(' p= params ')' | '(' a= alias_stmts ')' )
            int alt7=4;
            switch ( input.LA(1) ) {
            case ALIAS_NOT:
                {
                int LA7_1 = input.LA(2);

                if ( (LA7_1==21) ) {
                    alt7=2;
                }
                else if ( (LA7_1==18) ) {
                    alt7=1;
                }
                else {
                    NoViableAltException nvae =
                        new NoViableAltException("", 7, 1, input);

                    throw nvae;
                }
                }
                break;
            case 18:
                {
                alt7=1;
                }
                break;
            case 21:
                {
                alt7=2;
                }
                break;
            case 23:
            case 24:
            case 25:
                {
                alt7=3;
                }
                break;
            case 19:
                {
                alt7=4;
                }
                break;
            default:
                NoViableAltException nvae =
                    new NoViableAltException("", 7, 0, input);

                throw nvae;
            }

            switch (alt7) {
                case 1 :
                    // /Users/jgf/Documents/Projects/joana/wala/joana.wala.flowless/src/edu/kit/joana/wala/flowless/spec/FlowLess.g:135:4: ( ALIAS_NOT )? 'alias' '(' p= params ')'
                    {
                     boolean negated = false; 
                    // /Users/jgf/Documents/Projects/joana/wala/joana.wala.flowless/src/edu/kit/joana/wala/flowless/spec/FlowLess.g:135:33: ( ALIAS_NOT )?
                    int alt5=2;
                    int LA5_0 = input.LA(1);

                    if ( (LA5_0==ALIAS_NOT) ) {
                        alt5=1;
                    }
                    switch (alt5) {
                        case 1 :
                            // /Users/jgf/Documents/Projects/joana/wala/joana.wala.flowless/src/edu/kit/joana/wala/flowless/spec/FlowLess.g:135:34: ALIAS_NOT
                            {
                            match(input,ALIAS_NOT,FOLLOW_ALIAS_NOT_in_alias_stmt199); 
                             negated = true; 

                            }
                            break;

                    }

                    match(input,18,FOLLOW_18_in_alias_stmt205); 
                    match(input,19,FOLLOW_19_in_alias_stmt207); 
                    pushFollow(FOLLOW_params_in_alias_stmt211);
                    p=params();

                    state._fsp--;

                    match(input,20,FOLLOW_20_in_alias_stmt213); 
                     alias = new PrimitiveAliasStmt(p, negated); 

                    }
                    break;
                case 2 :
                    // /Users/jgf/Documents/Projects/joana/wala/joana.wala.flowless/src/edu/kit/joana/wala/flowless/spec/FlowLess.g:136:4: ( ALIAS_NOT )? '{' p= params '}'
                    {
                     boolean negated = false; 
                    // /Users/jgf/Documents/Projects/joana/wala/joana.wala.flowless/src/edu/kit/joana/wala/flowless/spec/FlowLess.g:136:33: ( ALIAS_NOT )?
                    int alt6=2;
                    int LA6_0 = input.LA(1);

                    if ( (LA6_0==ALIAS_NOT) ) {
                        alt6=1;
                    }
                    switch (alt6) {
                        case 1 :
                            // /Users/jgf/Documents/Projects/joana/wala/joana.wala.flowless/src/edu/kit/joana/wala/flowless/spec/FlowLess.g:136:34: ALIAS_NOT
                            {
                            match(input,ALIAS_NOT,FOLLOW_ALIAS_NOT_in_alias_stmt223); 
                             negated = true; 

                            }
                            break;

                    }

                    match(input,21,FOLLOW_21_in_alias_stmt229); 
                    pushFollow(FOLLOW_params_in_alias_stmt233);
                    p=params();

                    state._fsp--;

                    match(input,22,FOLLOW_22_in_alias_stmt235); 
                     alias = new PrimitiveAliasStmt(p, negated); 

                    }
                    break;
                case 3 :
                    // /Users/jgf/Documents/Projects/joana/wala/joana.wala.flowless/src/edu/kit/joana/wala/flowless/spec/FlowLess.g:137:4: ( 'unique' | '1' | 'uniq' ) '(' p= params ')'
                    {
                    if ( (input.LA(1)>=23 && input.LA(1)<=25) ) {
                        input.consume();
                        state.errorRecovery=false;
                    }
                    else {
                        MismatchedSetException mse = new MismatchedSetException(null,input);
                        throw mse;
                    }

                    match(input,19,FOLLOW_19_in_alias_stmt255); 
                    pushFollow(FOLLOW_params_in_alias_stmt259);
                    p=params();

                    state._fsp--;

                    match(input,20,FOLLOW_20_in_alias_stmt261); 
                     alias = new UniqueStmt(p); 

                    }
                    break;
                case 4 :
                    // /Users/jgf/Documents/Projects/joana/wala/joana.wala.flowless/src/edu/kit/joana/wala/flowless/spec/FlowLess.g:138:4: '(' a= alias_stmts ')'
                    {
                    match(input,19,FOLLOW_19_in_alias_stmt268); 
                    pushFollow(FOLLOW_alias_stmts_in_alias_stmt272);
                    a=alias_stmts();

                    state._fsp--;

                    match(input,20,FOLLOW_20_in_alias_stmt274); 
                     alias = a; 

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
        return alias;
    }
    // $ANTLR end "alias_stmt"


    // $ANTLR start "flow_stmts"
    // /Users/jgf/Documents/Projects/joana/wala/joana.wala.flowless/src/edu/kit/joana/wala/flowless/spec/FlowLess.g:141:1: flow_stmts returns [List<FlowStmt> flowList] : f= flow_not_list ( ',' fl= flow_not_list )* ;
    public final List<FlowStmt> flow_stmts() throws RecognitionException {
        List<FlowStmt> flowList = null;

        List<FlowStmt> f = null;

        List<FlowStmt> fl = null;


        try {
            // /Users/jgf/Documents/Projects/joana/wala/joana.wala.flowless/src/edu/kit/joana/wala/flowless/spec/FlowLess.g:142:2: (f= flow_not_list ( ',' fl= flow_not_list )* )
            // /Users/jgf/Documents/Projects/joana/wala/joana.wala.flowless/src/edu/kit/joana/wala/flowless/spec/FlowLess.g:142:4: f= flow_not_list ( ',' fl= flow_not_list )*
            {
             flowList = new LinkedList<FlowStmt>(); 
            pushFollow(FOLLOW_flow_not_list_in_flow_stmts295);
            f=flow_not_list();

            state._fsp--;

             flowList.addAll(f); 
            // /Users/jgf/Documents/Projects/joana/wala/joana.wala.flowless/src/edu/kit/joana/wala/flowless/spec/FlowLess.g:142:87: ( ',' fl= flow_not_list )*
            loop8:
            do {
                int alt8=2;
                int LA8_0 = input.LA(1);

                if ( (LA8_0==26) ) {
                    alt8=1;
                }


                switch (alt8) {
            	case 1 :
            	    // /Users/jgf/Documents/Projects/joana/wala/joana.wala.flowless/src/edu/kit/joana/wala/flowless/spec/FlowLess.g:142:88: ',' fl= flow_not_list
            	    {
            	    match(input,26,FOLLOW_26_in_flow_stmts300); 
            	    pushFollow(FOLLOW_flow_not_list_in_flow_stmts304);
            	    fl=flow_not_list();

            	    state._fsp--;

            	     flowList.addAll(fl); 

            	    }
            	    break;

            	default :
            	    break loop8;
                }
            } while (true);


            }

        }
        catch (RecognitionException re) {
            reportError(re);
            recover(input,re);
        }
        finally {
        }
        return flowList;
    }
    // $ANTLR end "flow_stmts"


    // $ANTLR start "flow_not_list"
    // /Users/jgf/Documents/Projects/joana/wala/joana.wala.flowless/src/edu/kit/joana/wala/flowless/spec/FlowLess.g:145:1: flow_not_list returns [List<FlowStmt> flowList] : ( ALIAS_NOT '(' f= flow_not_stmt ( ',' fOpt= flow_not_stmt )* ')' | f= flow_stmt | p= pure_stmt );
    public final List<FlowStmt> flow_not_list() throws RecognitionException {
        List<FlowStmt> flowList = null;

        ExplicitFlowStmt f = null;

        ExplicitFlowStmt fOpt = null;

        PureStmt p = null;


        try {
            // /Users/jgf/Documents/Projects/joana/wala/joana.wala.flowless/src/edu/kit/joana/wala/flowless/spec/FlowLess.g:146:2: ( ALIAS_NOT '(' f= flow_not_stmt ( ',' fOpt= flow_not_stmt )* ')' | f= flow_stmt | p= pure_stmt )
            int alt10=3;
            switch ( input.LA(1) ) {
            case ALIAS_NOT:
                {
                alt10=1;
                }
                break;
            case WILDCARD:
            case IDENT:
            case 19:
                {
                alt10=2;
                }
                break;
            case 27:
                {
                alt10=3;
                }
                break;
            default:
                NoViableAltException nvae =
                    new NoViableAltException("", 10, 0, input);

                throw nvae;
            }

            switch (alt10) {
                case 1 :
                    // /Users/jgf/Documents/Projects/joana/wala/joana.wala.flowless/src/edu/kit/joana/wala/flowless/spec/FlowLess.g:146:4: ALIAS_NOT '(' f= flow_not_stmt ( ',' fOpt= flow_not_stmt )* ')'
                    {
                     flowList = new LinkedList<FlowStmt>(); 
                    match(input,ALIAS_NOT,FOLLOW_ALIAS_NOT_in_flow_not_list333); 
                    match(input,19,FOLLOW_19_in_flow_not_list335); 
                    pushFollow(FOLLOW_flow_not_stmt_in_flow_not_list339);
                    f=flow_not_stmt();

                    state._fsp--;

                     f.negateNoFlow(); flowList.add(f); 
                    // /Users/jgf/Documents/Projects/joana/wala/joana.wala.flowless/src/edu/kit/joana/wala/flowless/spec/FlowLess.g:148:7: ( ',' fOpt= flow_not_stmt )*
                    loop9:
                    do {
                        int alt9=2;
                        int LA9_0 = input.LA(1);

                        if ( (LA9_0==26) ) {
                            alt9=1;
                        }


                        switch (alt9) {
                    	case 1 :
                    	    // /Users/jgf/Documents/Projects/joana/wala/joana.wala.flowless/src/edu/kit/joana/wala/flowless/spec/FlowLess.g:148:8: ',' fOpt= flow_not_stmt
                    	    {
                    	    match(input,26,FOLLOW_26_in_flow_not_list352); 
                    	    pushFollow(FOLLOW_flow_not_stmt_in_flow_not_list356);
                    	    fOpt=flow_not_stmt();

                    	    state._fsp--;

                    	     fOpt.negateNoFlow(); flowList.add(fOpt); 

                    	    }
                    	    break;

                    	default :
                    	    break loop9;
                        }
                    } while (true);

                    match(input,20,FOLLOW_20_in_flow_not_list362); 

                    }
                    break;
                case 2 :
                    // /Users/jgf/Documents/Projects/joana/wala/joana.wala.flowless/src/edu/kit/joana/wala/flowless/spec/FlowLess.g:149:4: f= flow_stmt
                    {
                     flowList = new LinkedList<FlowStmt>(); 
                    pushFollow(FOLLOW_flow_stmt_in_flow_not_list372);
                    f=flow_stmt();

                    state._fsp--;

                     flowList.add(f); 

                    }
                    break;
                case 3 :
                    // /Users/jgf/Documents/Projects/joana/wala/joana.wala.flowless/src/edu/kit/joana/wala/flowless/spec/FlowLess.g:150:4: p= pure_stmt
                    {
                     flowList = new LinkedList<FlowStmt>(); 
                    pushFollow(FOLLOW_pure_stmt_in_flow_not_list383);
                    p=pure_stmt();

                    state._fsp--;

                     flowList.add(p); 

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
        return flowList;
    }
    // $ANTLR end "flow_not_list"


    // $ANTLR start "pure_stmt"
    // /Users/jgf/Documents/Projects/joana/wala/joana.wala.flowless/src/edu/kit/joana/wala/flowless/spec/FlowLess.g:153:1: pure_stmt returns [PureStmt pure] : 'pure' ( '(' p= simple_params ')' )? ;
    public final PureStmt pure_stmt() throws RecognitionException {
        PureStmt pure = null;

        List<SimpleParameter> p = null;


        try {
            // /Users/jgf/Documents/Projects/joana/wala/joana.wala.flowless/src/edu/kit/joana/wala/flowless/spec/FlowLess.g:154:3: ( 'pure' ( '(' p= simple_params ')' )? )
            // /Users/jgf/Documents/Projects/joana/wala/joana.wala.flowless/src/edu/kit/joana/wala/flowless/spec/FlowLess.g:154:5: 'pure' ( '(' p= simple_params ')' )?
            {
            match(input,27,FOLLOW_27_in_pure_stmt401); 
            // /Users/jgf/Documents/Projects/joana/wala/joana.wala.flowless/src/edu/kit/joana/wala/flowless/spec/FlowLess.g:154:12: ( '(' p= simple_params ')' )?
            int alt11=2;
            int LA11_0 = input.LA(1);

            if ( (LA11_0==19) ) {
                alt11=1;
            }
            switch (alt11) {
                case 1 :
                    // /Users/jgf/Documents/Projects/joana/wala/joana.wala.flowless/src/edu/kit/joana/wala/flowless/spec/FlowLess.g:154:13: '(' p= simple_params ')'
                    {
                    match(input,19,FOLLOW_19_in_pure_stmt404); 
                    pushFollow(FOLLOW_simple_params_in_pure_stmt408);
                    p=simple_params();

                    state._fsp--;

                    match(input,20,FOLLOW_20_in_pure_stmt410); 

                    }
                    break;

            }

             if (p == null || p.isEmpty()) { 
                    p = new LinkedList<SimpleParameter>(); 
                    p.add(new SimpleParameter(new SimpleParameter.Wildcard())); 
                  }
                  pure = new PureStmt(p);
                

            }

        }
        catch (RecognitionException re) {
            reportError(re);
            recover(input,re);
        }
        finally {
        }
        return pure;
    }
    // $ANTLR end "pure_stmt"


    // $ANTLR start "flow_not_stmt"
    // /Users/jgf/Documents/Projects/joana/wala/joana.wala.flowless/src/edu/kit/joana/wala/flowless/spec/FlowLess.g:163:1: flow_not_stmt returns [ExplicitFlowStmt flow] : ( ALIAS_NOT f= flow_stmt | f= flow_stmt );
    public final ExplicitFlowStmt flow_not_stmt() throws RecognitionException {
        ExplicitFlowStmt flow = null;

        ExplicitFlowStmt f = null;


        try {
            // /Users/jgf/Documents/Projects/joana/wala/joana.wala.flowless/src/edu/kit/joana/wala/flowless/spec/FlowLess.g:164:2: ( ALIAS_NOT f= flow_stmt | f= flow_stmt )
            int alt12=2;
            int LA12_0 = input.LA(1);

            if ( (LA12_0==ALIAS_NOT) ) {
                alt12=1;
            }
            else if ( (LA12_0==WILDCARD||LA12_0==IDENT||LA12_0==19) ) {
                alt12=2;
            }
            else {
                NoViableAltException nvae =
                    new NoViableAltException("", 12, 0, input);

                throw nvae;
            }
            switch (alt12) {
                case 1 :
                    // /Users/jgf/Documents/Projects/joana/wala/joana.wala.flowless/src/edu/kit/joana/wala/flowless/spec/FlowLess.g:164:4: ALIAS_NOT f= flow_stmt
                    {
                    match(input,ALIAS_NOT,FOLLOW_ALIAS_NOT_in_flow_not_stmt435); 
                    pushFollow(FOLLOW_flow_stmt_in_flow_not_stmt439);
                    f=flow_stmt();

                    state._fsp--;

                     flow = f; flow.negateNoFlow(); 

                    }
                    break;
                case 2 :
                    // /Users/jgf/Documents/Projects/joana/wala/joana.wala.flowless/src/edu/kit/joana/wala/flowless/spec/FlowLess.g:165:4: f= flow_stmt
                    {
                    pushFollow(FOLLOW_flow_stmt_in_flow_not_stmt448);
                    f=flow_stmt();

                    state._fsp--;

                     flow = f; 

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
        return flow;
    }
    // $ANTLR end "flow_not_stmt"


    // $ANTLR start "flow_stmt"
    // /Users/jgf/Documents/Projects/joana/wala/joana.wala.flowless/src/edu/kit/joana/wala/flowless/spec/FlowLess.g:168:1: flow_stmt returns [ExplicitFlowStmt flow] : ( ( '(' from= simple_params ')' | f= param_exp ) ( ARROW | NOT_ARROW ) ( '(' to= simple_out_params ')' | t= out_param_exp ) | WILDCARD );
    public final ExplicitFlowStmt flow_stmt() throws RecognitionException {
        ExplicitFlowStmt flow = null;

        List<SimpleParameter> from = null;

        SimpleParameter f = null;

        List<SimpleParameter> to = null;

        SimpleParameter t = null;


        try {
            // /Users/jgf/Documents/Projects/joana/wala/joana.wala.flowless/src/edu/kit/joana/wala/flowless/spec/FlowLess.g:169:2: ( ( '(' from= simple_params ')' | f= param_exp ) ( ARROW | NOT_ARROW ) ( '(' to= simple_out_params ')' | t= out_param_exp ) | WILDCARD )
            int alt16=2;
            int LA16_0 = input.LA(1);

            if ( (LA16_0==IDENT||LA16_0==19) ) {
                alt16=1;
            }
            else if ( (LA16_0==WILDCARD) ) {
                int LA16_2 = input.LA(2);

                if ( (LA16_2==EOF||LA16_2==20||LA16_2==26) ) {
                    alt16=2;
                }
                else if ( ((LA16_2>=ARROW && LA16_2<=NOT_ARROW)) ) {
                    alt16=1;
                }
                else {
                    NoViableAltException nvae =
                        new NoViableAltException("", 16, 2, input);

                    throw nvae;
                }
            }
            else {
                NoViableAltException nvae =
                    new NoViableAltException("", 16, 0, input);

                throw nvae;
            }
            switch (alt16) {
                case 1 :
                    // /Users/jgf/Documents/Projects/joana/wala/joana.wala.flowless/src/edu/kit/joana/wala/flowless/spec/FlowLess.g:169:4: ( '(' from= simple_params ')' | f= param_exp ) ( ARROW | NOT_ARROW ) ( '(' to= simple_out_params ')' | t= out_param_exp )
                    {
                     boolean negated = false; 
                    // /Users/jgf/Documents/Projects/joana/wala/joana.wala.flowless/src/edu/kit/joana/wala/flowless/spec/FlowLess.g:170:3: ( '(' from= simple_params ')' | f= param_exp )
                    int alt13=2;
                    int LA13_0 = input.LA(1);

                    if ( (LA13_0==19) ) {
                        alt13=1;
                    }
                    else if ( (LA13_0==WILDCARD||LA13_0==IDENT) ) {
                        alt13=2;
                    }
                    else {
                        NoViableAltException nvae =
                            new NoViableAltException("", 13, 0, input);

                        throw nvae;
                    }
                    switch (alt13) {
                        case 1 :
                            // /Users/jgf/Documents/Projects/joana/wala/joana.wala.flowless/src/edu/kit/joana/wala/flowless/spec/FlowLess.g:170:4: '(' from= simple_params ')'
                            {
                            match(input,19,FOLLOW_19_in_flow_stmt471); 
                            pushFollow(FOLLOW_simple_params_in_flow_stmt475);
                            from=simple_params();

                            state._fsp--;

                            match(input,20,FOLLOW_20_in_flow_stmt477); 

                            }
                            break;
                        case 2 :
                            // /Users/jgf/Documents/Projects/joana/wala/joana.wala.flowless/src/edu/kit/joana/wala/flowless/spec/FlowLess.g:170:33: f= param_exp
                            {
                            pushFollow(FOLLOW_param_exp_in_flow_stmt483);
                            f=param_exp();

                            state._fsp--;

                             from = new LinkedList<SimpleParameter>(); from.add(f); 

                            }
                            break;

                    }

                    // /Users/jgf/Documents/Projects/joana/wala/joana.wala.flowless/src/edu/kit/joana/wala/flowless/spec/FlowLess.g:171:3: ( ARROW | NOT_ARROW )
                    int alt14=2;
                    int LA14_0 = input.LA(1);

                    if ( (LA14_0==ARROW) ) {
                        alt14=1;
                    }
                    else if ( (LA14_0==NOT_ARROW) ) {
                        alt14=2;
                    }
                    else {
                        NoViableAltException nvae =
                            new NoViableAltException("", 14, 0, input);

                        throw nvae;
                    }
                    switch (alt14) {
                        case 1 :
                            // /Users/jgf/Documents/Projects/joana/wala/joana.wala.flowless/src/edu/kit/joana/wala/flowless/spec/FlowLess.g:171:4: ARROW
                            {
                            match(input,ARROW,FOLLOW_ARROW_in_flow_stmt492); 

                            }
                            break;
                        case 2 :
                            // /Users/jgf/Documents/Projects/joana/wala/joana.wala.flowless/src/edu/kit/joana/wala/flowless/spec/FlowLess.g:171:12: NOT_ARROW
                            {
                            match(input,NOT_ARROW,FOLLOW_NOT_ARROW_in_flow_stmt496); 
                             negated = true; 

                            }
                            break;

                    }

                    // /Users/jgf/Documents/Projects/joana/wala/joana.wala.flowless/src/edu/kit/joana/wala/flowless/spec/FlowLess.g:172:3: ( '(' to= simple_out_params ')' | t= out_param_exp )
                    int alt15=2;
                    int LA15_0 = input.LA(1);

                    if ( (LA15_0==19) ) {
                        alt15=1;
                    }
                    else if ( ((LA15_0>=WILDCARD && LA15_0<=IDENT)) ) {
                        alt15=2;
                    }
                    else {
                        NoViableAltException nvae =
                            new NoViableAltException("", 15, 0, input);

                        throw nvae;
                    }
                    switch (alt15) {
                        case 1 :
                            // /Users/jgf/Documents/Projects/joana/wala/joana.wala.flowless/src/edu/kit/joana/wala/flowless/spec/FlowLess.g:172:4: '(' to= simple_out_params ')'
                            {
                            match(input,19,FOLLOW_19_in_flow_stmt505); 
                            pushFollow(FOLLOW_simple_out_params_in_flow_stmt509);
                            to=simple_out_params();

                            state._fsp--;

                            match(input,20,FOLLOW_20_in_flow_stmt511); 

                            }
                            break;
                        case 2 :
                            // /Users/jgf/Documents/Projects/joana/wala/joana.wala.flowless/src/edu/kit/joana/wala/flowless/spec/FlowLess.g:172:35: t= out_param_exp
                            {
                            pushFollow(FOLLOW_out_param_exp_in_flow_stmt517);
                            t=out_param_exp();

                            state._fsp--;

                            to = new LinkedList<SimpleParameter>(); to.add(t); 

                            }
                            break;

                    }

                     
                    		 flow = new ExplicitFlowStmt(from, to);
                    		 if (negated) {
                    		   flow.negateNoFlow();
                    		 } 
                    		

                    }
                    break;
                case 2 :
                    // /Users/jgf/Documents/Projects/joana/wala/joana.wala.flowless/src/edu/kit/joana/wala/flowless/spec/FlowLess.g:179:4: WILDCARD
                    {
                    match(input,WILDCARD,FOLLOW_WILDCARD_in_flow_stmt530); 
                     
                    			List<SimpleParameter> fromList = new LinkedList<SimpleParameter>(); 
                    			fromList.add(new SimpleParameter(new SimpleParameter.Wildcard())); 
                    			List<SimpleParameter> toList = new LinkedList<SimpleParameter>(); 
                    			toList.add(new SimpleParameter(new SimpleParameter.Wildcard())); 
                    			flow = new ExplicitFlowStmt(fromList, toList); 
                    		

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
        return flow;
    }
    // $ANTLR end "flow_stmt"


    // $ANTLR start "simple_out_params"
    // /Users/jgf/Documents/Projects/joana/wala/joana.wala.flowless/src/edu/kit/joana/wala/flowless/spec/FlowLess.g:189:1: simple_out_params returns [List<SimpleParameter> params] : p= out_param_exp ( ',' pOpt= out_param_exp )* ;
    public final List<SimpleParameter> simple_out_params() throws RecognitionException {
        List<SimpleParameter> params = null;

        SimpleParameter p = null;

        SimpleParameter pOpt = null;


        try {
            // /Users/jgf/Documents/Projects/joana/wala/joana.wala.flowless/src/edu/kit/joana/wala/flowless/spec/FlowLess.g:190:3: (p= out_param_exp ( ',' pOpt= out_param_exp )* )
            // /Users/jgf/Documents/Projects/joana/wala/joana.wala.flowless/src/edu/kit/joana/wala/flowless/spec/FlowLess.g:190:5: p= out_param_exp ( ',' pOpt= out_param_exp )*
            {
             params = new LinkedList<SimpleParameter>(); 
            pushFollow(FOLLOW_out_param_exp_in_simple_out_params555);
            p=out_param_exp();

            state._fsp--;

             params.add(p); 
            // /Users/jgf/Documents/Projects/joana/wala/joana.wala.flowless/src/edu/kit/joana/wala/flowless/spec/FlowLess.g:190:88: ( ',' pOpt= out_param_exp )*
            loop17:
            do {
                int alt17=2;
                int LA17_0 = input.LA(1);

                if ( (LA17_0==26) ) {
                    alt17=1;
                }


                switch (alt17) {
            	case 1 :
            	    // /Users/jgf/Documents/Projects/joana/wala/joana.wala.flowless/src/edu/kit/joana/wala/flowless/spec/FlowLess.g:190:89: ',' pOpt= out_param_exp
            	    {
            	    match(input,26,FOLLOW_26_in_simple_out_params560); 
            	    pushFollow(FOLLOW_out_param_exp_in_simple_out_params564);
            	    pOpt=out_param_exp();

            	    state._fsp--;

            	     params.add(pOpt); 

            	    }
            	    break;

            	default :
            	    break loop17;
                }
            } while (true);


            }

        }
        catch (RecognitionException re) {
            reportError(re);
            recover(input,re);
        }
        finally {
        }
        return params;
    }
    // $ANTLR end "simple_out_params"


    // $ANTLR start "out_param_exp"
    // /Users/jgf/Documents/Projects/joana/wala/joana.wala.flowless/src/edu/kit/joana/wala/flowless/spec/FlowLess.g:193:1: out_param_exp returns [SimpleParameter param] : (p= param_exp | ( RESULT | EXC ) ( '.' p= param_exp )? | STATE );
    public final SimpleParameter out_param_exp() throws RecognitionException {
        SimpleParameter param = null;

        SimpleParameter p = null;


        try {
            // /Users/jgf/Documents/Projects/joana/wala/joana.wala.flowless/src/edu/kit/joana/wala/flowless/spec/FlowLess.g:194:2: (p= param_exp | ( RESULT | EXC ) ( '.' p= param_exp )? | STATE )
            int alt20=3;
            switch ( input.LA(1) ) {
            case WILDCARD:
            case IDENT:
                {
                alt20=1;
                }
                break;
            case RESULT:
            case EXC:
                {
                alt20=2;
                }
                break;
            case STATE:
                {
                alt20=3;
                }
                break;
            default:
                NoViableAltException nvae =
                    new NoViableAltException("", 20, 0, input);

                throw nvae;
            }

            switch (alt20) {
                case 1 :
                    // /Users/jgf/Documents/Projects/joana/wala/joana.wala.flowless/src/edu/kit/joana/wala/flowless/spec/FlowLess.g:194:4: p= param_exp
                    {
                    pushFollow(FOLLOW_param_exp_in_out_param_exp586);
                    p=param_exp();

                    state._fsp--;

                     param = p; 

                    }
                    break;
                case 2 :
                    // /Users/jgf/Documents/Projects/joana/wala/joana.wala.flowless/src/edu/kit/joana/wala/flowless/spec/FlowLess.g:195:4: ( RESULT | EXC ) ( '.' p= param_exp )?
                    {
                     param = new SimpleParameter(); SimpleParameter.Part part = null; 
                    // /Users/jgf/Documents/Projects/joana/wala/joana.wala.flowless/src/edu/kit/joana/wala/flowless/spec/FlowLess.g:196:7: ( RESULT | EXC )
                    int alt18=2;
                    int LA18_0 = input.LA(1);

                    if ( (LA18_0==RESULT) ) {
                        alt18=1;
                    }
                    else if ( (LA18_0==EXC) ) {
                        alt18=2;
                    }
                    else {
                        NoViableAltException nvae =
                            new NoViableAltException("", 18, 0, input);

                        throw nvae;
                    }
                    switch (alt18) {
                        case 1 :
                            // /Users/jgf/Documents/Projects/joana/wala/joana.wala.flowless/src/edu/kit/joana/wala/flowless/spec/FlowLess.g:196:8: RESULT
                            {
                            match(input,RESULT,FOLLOW_RESULT_in_out_param_exp603); 
                             part = new SimpleParameter.Result(); 

                            }
                            break;
                        case 2 :
                            // /Users/jgf/Documents/Projects/joana/wala/joana.wala.flowless/src/edu/kit/joana/wala/flowless/spec/FlowLess.g:196:58: EXC
                            {
                            match(input,EXC,FOLLOW_EXC_in_out_param_exp609); 
                             part = new SimpleParameter.ExceptionValue(); 

                            }
                            break;

                    }

                    // /Users/jgf/Documents/Projects/joana/wala/joana.wala.flowless/src/edu/kit/joana/wala/flowless/spec/FlowLess.g:197:7: ( '.' p= param_exp )?
                    int alt19=2;
                    int LA19_0 = input.LA(1);

                    if ( (LA19_0==28) ) {
                        alt19=1;
                    }
                    switch (alt19) {
                        case 1 :
                            // /Users/jgf/Documents/Projects/joana/wala/joana.wala.flowless/src/edu/kit/joana/wala/flowless/spec/FlowLess.g:197:8: '.' p= param_exp
                            {
                            match(input,28,FOLLOW_28_in_out_param_exp622); 
                            pushFollow(FOLLOW_param_exp_in_out_param_exp626);
                            p=param_exp();

                            state._fsp--;

                             param = p; 

                            }
                            break;

                    }

                     param.addFirstPart(part); 

                    }
                    break;
                case 3 :
                    // /Users/jgf/Documents/Projects/joana/wala/joana.wala.flowless/src/edu/kit/joana/wala/flowless/spec/FlowLess.g:198:4: STATE
                    {
                    match(input,STATE,FOLLOW_STATE_in_out_param_exp637); 
                     param = new SimpleParameter(new SimpleParameter.ProgramState()); 

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
        return param;
    }
    // $ANTLR end "out_param_exp"


    // $ANTLR start "simple_params"
    // /Users/jgf/Documents/Projects/joana/wala/joana.wala.flowless/src/edu/kit/joana/wala/flowless/spec/FlowLess.g:201:1: simple_params returns [List<SimpleParameter> params] : p= param_exp ( ',' pOpt= param_exp )* ;
    public final List<SimpleParameter> simple_params() throws RecognitionException {
        List<SimpleParameter> params = null;

        SimpleParameter p = null;

        SimpleParameter pOpt = null;


        try {
            // /Users/jgf/Documents/Projects/joana/wala/joana.wala.flowless/src/edu/kit/joana/wala/flowless/spec/FlowLess.g:202:3: (p= param_exp ( ',' pOpt= param_exp )* )
            // /Users/jgf/Documents/Projects/joana/wala/joana.wala.flowless/src/edu/kit/joana/wala/flowless/spec/FlowLess.g:202:5: p= param_exp ( ',' pOpt= param_exp )*
            {
             params = new LinkedList<SimpleParameter>(); 
            pushFollow(FOLLOW_param_exp_in_simple_params659);
            p=param_exp();

            state._fsp--;

             params.add(p); 
            // /Users/jgf/Documents/Projects/joana/wala/joana.wala.flowless/src/edu/kit/joana/wala/flowless/spec/FlowLess.g:202:84: ( ',' pOpt= param_exp )*
            loop21:
            do {
                int alt21=2;
                int LA21_0 = input.LA(1);

                if ( (LA21_0==26) ) {
                    alt21=1;
                }


                switch (alt21) {
            	case 1 :
            	    // /Users/jgf/Documents/Projects/joana/wala/joana.wala.flowless/src/edu/kit/joana/wala/flowless/spec/FlowLess.g:202:85: ',' pOpt= param_exp
            	    {
            	    match(input,26,FOLLOW_26_in_simple_params664); 
            	    pushFollow(FOLLOW_param_exp_in_simple_params668);
            	    pOpt=param_exp();

            	    state._fsp--;

            	     params.add(pOpt); 

            	    }
            	    break;

            	default :
            	    break loop21;
                }
            } while (true);


            }

        }
        catch (RecognitionException re) {
            reportError(re);
            recover(input,re);
        }
        finally {
        }
        return params;
    }
    // $ANTLR end "simple_params"


    // $ANTLR start "params"
    // /Users/jgf/Documents/Projects/joana/wala/joana.wala.flowless/src/edu/kit/joana/wala/flowless/spec/FlowLess.g:205:1: params returns [List<Parameter> params] : p= param ( ',' pOpt= param )* ;
    public final List<Parameter> params() throws RecognitionException {
        List<Parameter> params = null;

        Parameter p = null;

        Parameter pOpt = null;


        try {
            // /Users/jgf/Documents/Projects/joana/wala/joana.wala.flowless/src/edu/kit/joana/wala/flowless/spec/FlowLess.g:206:2: (p= param ( ',' pOpt= param )* )
            // /Users/jgf/Documents/Projects/joana/wala/joana.wala.flowless/src/edu/kit/joana/wala/flowless/spec/FlowLess.g:206:4: p= param ( ',' pOpt= param )*
            {
             params = new LinkedList<Parameter>(); 
            pushFollow(FOLLOW_param_in_params694);
            p=param();

            state._fsp--;

             params.add(p); 
            // /Users/jgf/Documents/Projects/joana/wala/joana.wala.flowless/src/edu/kit/joana/wala/flowless/spec/FlowLess.g:206:73: ( ',' pOpt= param )*
            loop22:
            do {
                int alt22=2;
                int LA22_0 = input.LA(1);

                if ( (LA22_0==26) ) {
                    alt22=1;
                }


                switch (alt22) {
            	case 1 :
            	    // /Users/jgf/Documents/Projects/joana/wala/joana.wala.flowless/src/edu/kit/joana/wala/flowless/spec/FlowLess.g:206:74: ',' pOpt= param
            	    {
            	    match(input,26,FOLLOW_26_in_params699); 
            	    pushFollow(FOLLOW_param_in_params703);
            	    pOpt=param();

            	    state._fsp--;

            	     params.add(pOpt); 

            	    }
            	    break;

            	default :
            	    break loop22;
                }
            } while (true);


            }

        }
        catch (RecognitionException re) {
            reportError(re);
            recover(input,re);
        }
        finally {
        }
        return params;
    }
    // $ANTLR end "params"


    // $ANTLR start "param"
    // /Users/jgf/Documents/Projects/joana/wala/joana.wala.flowless/src/edu/kit/joana/wala/flowless/spec/FlowLess.g:209:1: param returns [Parameter param] : (p= param_exp | '[' p= param_exp ( ',' pOpt= param_exp )* ']' );
    public final Parameter param() throws RecognitionException {
        Parameter param = null;

        SimpleParameter p = null;

        SimpleParameter pOpt = null;


        try {
            // /Users/jgf/Documents/Projects/joana/wala/joana.wala.flowless/src/edu/kit/joana/wala/flowless/spec/FlowLess.g:210:3: (p= param_exp | '[' p= param_exp ( ',' pOpt= param_exp )* ']' )
            int alt24=2;
            int LA24_0 = input.LA(1);

            if ( (LA24_0==WILDCARD||LA24_0==IDENT) ) {
                alt24=1;
            }
            else if ( (LA24_0==29) ) {
                alt24=2;
            }
            else {
                NoViableAltException nvae =
                    new NoViableAltException("", 24, 0, input);

                throw nvae;
            }
            switch (alt24) {
                case 1 :
                    // /Users/jgf/Documents/Projects/joana/wala/joana.wala.flowless/src/edu/kit/joana/wala/flowless/spec/FlowLess.g:210:5: p= param_exp
                    {
                    pushFollow(FOLLOW_param_exp_in_param727);
                    p=param_exp();

                    state._fsp--;

                     param = p; 

                    }
                    break;
                case 2 :
                    // /Users/jgf/Documents/Projects/joana/wala/joana.wala.flowless/src/edu/kit/joana/wala/flowless/spec/FlowLess.g:211:5: '[' p= param_exp ( ',' pOpt= param_exp )* ']'
                    {
                     ParameterOptList l = new ParameterOptList(); 
                    match(input,29,FOLLOW_29_in_param737); 
                    pushFollow(FOLLOW_param_exp_in_param741);
                    p=param_exp();

                    state._fsp--;

                     l.addParam(p); 
                    // /Users/jgf/Documents/Projects/joana/wala/joana.wala.flowless/src/edu/kit/joana/wala/flowless/spec/FlowLess.g:211:89: ( ',' pOpt= param_exp )*
                    loop23:
                    do {
                        int alt23=2;
                        int LA23_0 = input.LA(1);

                        if ( (LA23_0==26) ) {
                            alt23=1;
                        }


                        switch (alt23) {
                    	case 1 :
                    	    // /Users/jgf/Documents/Projects/joana/wala/joana.wala.flowless/src/edu/kit/joana/wala/flowless/spec/FlowLess.g:211:90: ',' pOpt= param_exp
                    	    {
                    	    match(input,26,FOLLOW_26_in_param746); 
                    	    pushFollow(FOLLOW_param_exp_in_param750);
                    	    pOpt=param_exp();

                    	    state._fsp--;

                    	     l.addParam(pOpt); 

                    	    }
                    	    break;

                    	default :
                    	    break loop23;
                        }
                    } while (true);

                    match(input,30,FOLLOW_30_in_param756); 
                     param = l; 

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
        return param;
    }
    // $ANTLR end "param"


    // $ANTLR start "param_exp"
    // /Users/jgf/Documents/Projects/joana/wala/joana.wala.flowless/src/edu/kit/joana/wala/flowless/spec/FlowLess.g:214:1: param_exp returns [SimpleParameter param] : (p= param_single ( '.' WILDCARD )? | WILDCARD );
    public final SimpleParameter param_exp() throws RecognitionException {
        SimpleParameter param = null;

        SimpleParameter p = null;


        try {
            // /Users/jgf/Documents/Projects/joana/wala/joana.wala.flowless/src/edu/kit/joana/wala/flowless/spec/FlowLess.g:215:2: (p= param_single ( '.' WILDCARD )? | WILDCARD )
            int alt26=2;
            int LA26_0 = input.LA(1);

            if ( (LA26_0==IDENT) ) {
                alt26=1;
            }
            else if ( (LA26_0==WILDCARD) ) {
                alt26=2;
            }
            else {
                NoViableAltException nvae =
                    new NoViableAltException("", 26, 0, input);

                throw nvae;
            }
            switch (alt26) {
                case 1 :
                    // /Users/jgf/Documents/Projects/joana/wala/joana.wala.flowless/src/edu/kit/joana/wala/flowless/spec/FlowLess.g:215:4: p= param_single ( '.' WILDCARD )?
                    {
                    pushFollow(FOLLOW_param_single_in_param_exp778);
                    p=param_single();

                    state._fsp--;

                    // /Users/jgf/Documents/Projects/joana/wala/joana.wala.flowless/src/edu/kit/joana/wala/flowless/spec/FlowLess.g:215:19: ( '.' WILDCARD )?
                    int alt25=2;
                    int LA25_0 = input.LA(1);

                    if ( (LA25_0==28) ) {
                        alt25=1;
                    }
                    switch (alt25) {
                        case 1 :
                            // /Users/jgf/Documents/Projects/joana/wala/joana.wala.flowless/src/edu/kit/joana/wala/flowless/spec/FlowLess.g:215:20: '.' WILDCARD
                            {
                            match(input,28,FOLLOW_28_in_param_exp781); 
                            match(input,WILDCARD,FOLLOW_WILDCARD_in_param_exp783); 
                             SimpleParameter.Part part = new SimpleParameter.Wildcard(); p.addLastPart(part); 

                            }
                            break;

                    }

                     param = p;

                    }
                    break;
                case 2 :
                    // /Users/jgf/Documents/Projects/joana/wala/joana.wala.flowless/src/edu/kit/joana/wala/flowless/spec/FlowLess.g:216:4: WILDCARD
                    {
                    match(input,WILDCARD,FOLLOW_WILDCARD_in_param_exp794); 
                     param = new SimpleParameter(new SimpleParameter.Wildcard()); 

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
        return param;
    }
    // $ANTLR end "param_exp"


    // $ANTLR start "param_single"
    // /Users/jgf/Documents/Projects/joana/wala/joana.wala.flowless/src/edu/kit/joana/wala/flowless/spec/FlowLess.g:219:1: param_single returns [SimpleParameter param] : id= IDENT ( ( '.' id= IDENT | ( '.' )? ARRAY ) )* ;
    public final SimpleParameter param_single() throws RecognitionException {
        SimpleParameter param = null;

        Token id=null;

        try {
            // /Users/jgf/Documents/Projects/joana/wala/joana.wala.flowless/src/edu/kit/joana/wala/flowless/spec/FlowLess.g:220:2: (id= IDENT ( ( '.' id= IDENT | ( '.' )? ARRAY ) )* )
            // /Users/jgf/Documents/Projects/joana/wala/joana.wala.flowless/src/edu/kit/joana/wala/flowless/spec/FlowLess.g:220:4: id= IDENT ( ( '.' id= IDENT | ( '.' )? ARRAY ) )*
            {
            id=(Token)match(input,IDENT,FOLLOW_IDENT_in_param_single813); 
             param = new SimpleParameter(new SimpleParameter.NormalPart(id.getText())); 
            // /Users/jgf/Documents/Projects/joana/wala/joana.wala.flowless/src/edu/kit/joana/wala/flowless/spec/FlowLess.g:221:7: ( ( '.' id= IDENT | ( '.' )? ARRAY ) )*
            loop29:
            do {
                int alt29=2;
                int LA29_0 = input.LA(1);

                if ( (LA29_0==28) ) {
                    int LA29_1 = input.LA(2);

                    if ( ((LA29_1>=IDENT && LA29_1<=ARRAY)) ) {
                        alt29=1;
                    }


                }
                else if ( (LA29_0==ARRAY) ) {
                    alt29=1;
                }


                switch (alt29) {
            	case 1 :
            	    // /Users/jgf/Documents/Projects/joana/wala/joana.wala.flowless/src/edu/kit/joana/wala/flowless/spec/FlowLess.g:221:8: ( '.' id= IDENT | ( '.' )? ARRAY )
            	    {
            	     SimpleParameter.Part part = null; 
            	    // /Users/jgf/Documents/Projects/joana/wala/joana.wala.flowless/src/edu/kit/joana/wala/flowless/spec/FlowLess.g:222:10: ( '.' id= IDENT | ( '.' )? ARRAY )
            	    int alt28=2;
            	    int LA28_0 = input.LA(1);

            	    if ( (LA28_0==28) ) {
            	        int LA28_1 = input.LA(2);

            	        if ( (LA28_1==IDENT) ) {
            	            alt28=1;
            	        }
            	        else if ( (LA28_1==ARRAY) ) {
            	            alt28=2;
            	        }
            	        else {
            	            NoViableAltException nvae =
            	                new NoViableAltException("", 28, 1, input);

            	            throw nvae;
            	        }
            	    }
            	    else if ( (LA28_0==ARRAY) ) {
            	        alt28=2;
            	    }
            	    else {
            	        NoViableAltException nvae =
            	            new NoViableAltException("", 28, 0, input);

            	        throw nvae;
            	    }
            	    switch (alt28) {
            	        case 1 :
            	            // /Users/jgf/Documents/Projects/joana/wala/joana.wala.flowless/src/edu/kit/joana/wala/flowless/spec/FlowLess.g:222:12: '.' id= IDENT
            	            {
            	            match(input,28,FOLLOW_28_in_param_single838); 
            	            id=(Token)match(input,IDENT,FOLLOW_IDENT_in_param_single842); 
            	             part = new SimpleParameter.NormalPart(id.getText()); 

            	            }
            	            break;
            	        case 2 :
            	            // /Users/jgf/Documents/Projects/joana/wala/joana.wala.flowless/src/edu/kit/joana/wala/flowless/spec/FlowLess.g:225:12: ( '.' )? ARRAY
            	            {
            	            // /Users/jgf/Documents/Projects/joana/wala/joana.wala.flowless/src/edu/kit/joana/wala/flowless/spec/FlowLess.g:225:12: ( '.' )?
            	            int alt27=2;
            	            int LA27_0 = input.LA(1);

            	            if ( (LA27_0==28) ) {
            	                alt27=1;
            	            }
            	            switch (alt27) {
            	                case 1 :
            	                    // /Users/jgf/Documents/Projects/joana/wala/joana.wala.flowless/src/edu/kit/joana/wala/flowless/spec/FlowLess.g:225:12: '.'
            	                    {
            	                    match(input,28,FOLLOW_28_in_param_single877); 

            	                    }
            	                    break;

            	            }

            	            match(input,ARRAY,FOLLOW_ARRAY_in_param_single880); 
            	             part = new SimpleParameter.ArrayContent(); 

            	            }
            	            break;

            	    }

            	     param.addLastPart(part); 

            	    }
            	    break;

            	default :
            	    break loop29;
                }
            } while (true);


            }

        }
        catch (RecognitionException re) {
            reportError(re);
            recover(input,re);
        }
        finally {
        }
        return param;
    }
    // $ANTLR end "param_single"

    // Delegated rules


 

    public static final BitSet FOLLOW_alias_stmts_in_ifc_stmt65 = new BitSet(new long[]{0x0000000000000012L});
    public static final BitSet FOLLOW_IMPLIES_in_ifc_stmt68 = new BitSet(new long[]{0x0000000008084480L});
    public static final BitSet FOLLOW_flow_stmts_in_ifc_stmt72 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_IMPLIES_in_ifc_stmt82 = new BitSet(new long[]{0x0000000008084480L});
    public static final BitSet FOLLOW_flow_stmts_in_ifc_stmt86 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_17_in_ifc_stmt94 = new BitSet(new long[]{0x0000000000000010L});
    public static final BitSet FOLLOW_IMPLIES_in_ifc_stmt96 = new BitSet(new long[]{0x0000000008084480L});
    public static final BitSet FOLLOW_flow_stmts_in_ifc_stmt100 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_alias_or_in_alias_stmts119 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_alias_and_in_alias_or138 = new BitSet(new long[]{0x0000000000000022L});
    public static final BitSet FOLLOW_ALIAS_OR_in_alias_or143 = new BitSet(new long[]{0x0000000003AC0080L});
    public static final BitSet FOLLOW_alias_or_in_alias_or147 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_alias_stmt_in_alias_and168 = new BitSet(new long[]{0x0000000000000042L});
    public static final BitSet FOLLOW_ALIAS_AND_in_alias_and173 = new BitSet(new long[]{0x0000000003AC0080L});
    public static final BitSet FOLLOW_alias_and_in_alias_and177 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_ALIAS_NOT_in_alias_stmt199 = new BitSet(new long[]{0x0000000000040000L});
    public static final BitSet FOLLOW_18_in_alias_stmt205 = new BitSet(new long[]{0x0000000000080000L});
    public static final BitSet FOLLOW_19_in_alias_stmt207 = new BitSet(new long[]{0x0000000020084400L});
    public static final BitSet FOLLOW_params_in_alias_stmt211 = new BitSet(new long[]{0x0000000000100000L});
    public static final BitSet FOLLOW_20_in_alias_stmt213 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_ALIAS_NOT_in_alias_stmt223 = new BitSet(new long[]{0x0000000000200000L});
    public static final BitSet FOLLOW_21_in_alias_stmt229 = new BitSet(new long[]{0x0000000020084400L});
    public static final BitSet FOLLOW_params_in_alias_stmt233 = new BitSet(new long[]{0x0000000000400000L});
    public static final BitSet FOLLOW_22_in_alias_stmt235 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_set_in_alias_stmt242 = new BitSet(new long[]{0x0000000000080000L});
    public static final BitSet FOLLOW_19_in_alias_stmt255 = new BitSet(new long[]{0x0000000020084400L});
    public static final BitSet FOLLOW_params_in_alias_stmt259 = new BitSet(new long[]{0x0000000000100000L});
    public static final BitSet FOLLOW_20_in_alias_stmt261 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_19_in_alias_stmt268 = new BitSet(new long[]{0x0000000003AC0080L});
    public static final BitSet FOLLOW_alias_stmts_in_alias_stmt272 = new BitSet(new long[]{0x0000000000100000L});
    public static final BitSet FOLLOW_20_in_alias_stmt274 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_flow_not_list_in_flow_stmts295 = new BitSet(new long[]{0x0000000004000002L});
    public static final BitSet FOLLOW_26_in_flow_stmts300 = new BitSet(new long[]{0x0000000008084480L});
    public static final BitSet FOLLOW_flow_not_list_in_flow_stmts304 = new BitSet(new long[]{0x0000000004000002L});
    public static final BitSet FOLLOW_ALIAS_NOT_in_flow_not_list333 = new BitSet(new long[]{0x0000000000080000L});
    public static final BitSet FOLLOW_19_in_flow_not_list335 = new BitSet(new long[]{0x0000000000084480L});
    public static final BitSet FOLLOW_flow_not_stmt_in_flow_not_list339 = new BitSet(new long[]{0x0000000004100000L});
    public static final BitSet FOLLOW_26_in_flow_not_list352 = new BitSet(new long[]{0x0000000000084480L});
    public static final BitSet FOLLOW_flow_not_stmt_in_flow_not_list356 = new BitSet(new long[]{0x0000000004100000L});
    public static final BitSet FOLLOW_20_in_flow_not_list362 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_flow_stmt_in_flow_not_list372 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_pure_stmt_in_flow_not_list383 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_27_in_pure_stmt401 = new BitSet(new long[]{0x0000000000080002L});
    public static final BitSet FOLLOW_19_in_pure_stmt404 = new BitSet(new long[]{0x0000000000084400L});
    public static final BitSet FOLLOW_simple_params_in_pure_stmt408 = new BitSet(new long[]{0x0000000000100000L});
    public static final BitSet FOLLOW_20_in_pure_stmt410 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_ALIAS_NOT_in_flow_not_stmt435 = new BitSet(new long[]{0x0000000000084400L});
    public static final BitSet FOLLOW_flow_stmt_in_flow_not_stmt439 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_flow_stmt_in_flow_not_stmt448 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_19_in_flow_stmt471 = new BitSet(new long[]{0x0000000000084400L});
    public static final BitSet FOLLOW_simple_params_in_flow_stmt475 = new BitSet(new long[]{0x0000000000100000L});
    public static final BitSet FOLLOW_20_in_flow_stmt477 = new BitSet(new long[]{0x0000000000000300L});
    public static final BitSet FOLLOW_param_exp_in_flow_stmt483 = new BitSet(new long[]{0x0000000000000300L});
    public static final BitSet FOLLOW_ARROW_in_flow_stmt492 = new BitSet(new long[]{0x0000000000087C00L});
    public static final BitSet FOLLOW_NOT_ARROW_in_flow_stmt496 = new BitSet(new long[]{0x0000000000087C00L});
    public static final BitSet FOLLOW_19_in_flow_stmt505 = new BitSet(new long[]{0x0000000000087C00L});
    public static final BitSet FOLLOW_simple_out_params_in_flow_stmt509 = new BitSet(new long[]{0x0000000000100000L});
    public static final BitSet FOLLOW_20_in_flow_stmt511 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_out_param_exp_in_flow_stmt517 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_WILDCARD_in_flow_stmt530 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_out_param_exp_in_simple_out_params555 = new BitSet(new long[]{0x0000000004000002L});
    public static final BitSet FOLLOW_26_in_simple_out_params560 = new BitSet(new long[]{0x0000000000087C00L});
    public static final BitSet FOLLOW_out_param_exp_in_simple_out_params564 = new BitSet(new long[]{0x0000000004000002L});
    public static final BitSet FOLLOW_param_exp_in_out_param_exp586 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_RESULT_in_out_param_exp603 = new BitSet(new long[]{0x0000000010000002L});
    public static final BitSet FOLLOW_EXC_in_out_param_exp609 = new BitSet(new long[]{0x0000000010000002L});
    public static final BitSet FOLLOW_28_in_out_param_exp622 = new BitSet(new long[]{0x0000000000084400L});
    public static final BitSet FOLLOW_param_exp_in_out_param_exp626 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_STATE_in_out_param_exp637 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_param_exp_in_simple_params659 = new BitSet(new long[]{0x0000000004000002L});
    public static final BitSet FOLLOW_26_in_simple_params664 = new BitSet(new long[]{0x0000000000084400L});
    public static final BitSet FOLLOW_param_exp_in_simple_params668 = new BitSet(new long[]{0x0000000004000002L});
    public static final BitSet FOLLOW_param_in_params694 = new BitSet(new long[]{0x0000000004000002L});
    public static final BitSet FOLLOW_26_in_params699 = new BitSet(new long[]{0x0000000020084400L});
    public static final BitSet FOLLOW_param_in_params703 = new BitSet(new long[]{0x0000000004000002L});
    public static final BitSet FOLLOW_param_exp_in_param727 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_29_in_param737 = new BitSet(new long[]{0x0000000000084400L});
    public static final BitSet FOLLOW_param_exp_in_param741 = new BitSet(new long[]{0x0000000044000000L});
    public static final BitSet FOLLOW_26_in_param746 = new BitSet(new long[]{0x0000000000084400L});
    public static final BitSet FOLLOW_param_exp_in_param750 = new BitSet(new long[]{0x0000000044000000L});
    public static final BitSet FOLLOW_30_in_param756 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_param_single_in_param_exp778 = new BitSet(new long[]{0x0000000010000002L});
    public static final BitSet FOLLOW_28_in_param_exp781 = new BitSet(new long[]{0x0000000000000400L});
    public static final BitSet FOLLOW_WILDCARD_in_param_exp783 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_WILDCARD_in_param_exp794 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_IDENT_in_param_single813 = new BitSet(new long[]{0x0000000010008002L});
    public static final BitSet FOLLOW_28_in_param_single838 = new BitSet(new long[]{0x0000000000004000L});
    public static final BitSet FOLLOW_IDENT_in_param_single842 = new BitSet(new long[]{0x0000000010008002L});
    public static final BitSet FOLLOW_28_in_param_single877 = new BitSet(new long[]{0x0000000000008000L});
    public static final BitSet FOLLOW_ARRAY_in_param_single880 = new BitSet(new long[]{0x0000000010008002L});

}