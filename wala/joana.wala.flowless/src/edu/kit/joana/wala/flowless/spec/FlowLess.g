/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
/*
 * Grammar for the IFC method specification language FlowLess
 *
 * @author Juergen Graf <graf@kit.edu>
 *
 */
grammar FlowLess;

options {
  language = Java;
}

@header {/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.wala.flowless.spec;

import edu.kit.joana.wala.flowless.spec.ast.Parameter;
import edu.kit.joana.wala.flowless.spec.ast.ParameterOptList;
import edu.kit.joana.wala.flowless.spec.ast.SimpleParameter;
import edu.kit.joana.wala.flowless.spec.ast.FlowStmt;
import edu.kit.joana.wala.flowless.spec.ast.ExplicitFlowStmt;
import edu.kit.joana.wala.flowless.spec.ast.PureStmt;
import edu.kit.joana.wala.flowless.spec.ast.AliasStmt;
import edu.kit.joana.wala.flowless.spec.ast.PrimitiveAliasStmt;
import edu.kit.joana.wala.flowless.spec.ast.InferableAliasStmt;
import edu.kit.joana.wala.flowless.spec.ast.UniqueStmt;
import edu.kit.joana.wala.flowless.spec.ast.BooleanAliasStmt;
import edu.kit.joana.wala.flowless.spec.ast.IFCStmt;
import java.util.LinkedList;
import java.util.List;
}

@members {
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

}

@lexer::header {/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.wala.flowless.spec;
}

@lexer::members {
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
}

ifc_stmt returns [IFCStmt ifc]
	: a=alias_stmts (IMPLIES f=flow_stmts)? { ifc = new IFCStmt(a, f); }
  | IMPLIES f=flow_stmts { ifc = new IFCStmt(null, f); }
  | '?' IMPLIES f=flow_stmts { ifc = new IFCStmt(InferableAliasStmt.getInstance(), f); }
	;

alias_stmts returns [AliasStmt alias]
	: a=alias_or { alias = a; }
	;

alias_or returns [AliasStmt alias]
	: a=alias_and { alias = a; } (ALIAS_OR b=alias_or { alias = new BooleanAliasStmt(alias, b, BooleanAliasStmt.Operator.OR); })?
	;

alias_and returns [AliasStmt alias]
	: a=alias_stmt { alias = a; } (ALIAS_AND b=alias_and { alias = new BooleanAliasStmt(alias, b, BooleanAliasStmt.Operator.AND); })?
	;

alias_stmt returns [AliasStmt alias]
	: { boolean negated = false; } (ALIAS_NOT { negated = true; })? 'alias' '(' p=params ')' { alias = new PrimitiveAliasStmt(p, negated); }
	| { boolean negated = false; } (ALIAS_NOT { negated = true; })? '{' p=params '}' { alias = new PrimitiveAliasStmt(p, negated); }
	| ('unique' | '1' | 'uniq' ) '(' p=params ')' { alias = new UniqueStmt(p); }
	| '(' a=alias_stmts ')' { alias = a; }
	;

flow_stmts returns [List<FlowStmt> flowList]
	: { flowList = new LinkedList<FlowStmt>(); } f=flow_not_list { flowList.addAll(f); } (',' fl=flow_not_list { flowList.addAll(fl); } )*
	;

flow_not_list returns [List<FlowStmt> flowList]
	: { flowList = new LinkedList<FlowStmt>(); } 
	     ALIAS_NOT '(' f=flow_not_stmt  { f.negateNoFlow(); flowList.add(f); } 
	     (',' fOpt=flow_not_stmt { fOpt.negateNoFlow(); flowList.add(fOpt); })* ')' 
	| { flowList = new LinkedList<FlowStmt>(); } f=flow_stmt { flowList.add(f); }
	| { flowList = new LinkedList<FlowStmt>(); } p=pure_stmt { flowList.add(p); }
	;

pure_stmt returns [PureStmt pure]
  : 'pure' ('(' p=simple_params ')')? 
    { if (p == null || p.isEmpty()) { 
        p = new LinkedList<SimpleParameter>(); 
        p.add(new SimpleParameter(new SimpleParameter.Wildcard())); 
      }
      pure = new PureStmt(p);
    }
  ;

flow_not_stmt returns [ExplicitFlowStmt flow]
	: ALIAS_NOT f=flow_stmt { flow = f; flow.negateNoFlow(); }
	| f=flow_stmt { flow = f; }
	;

flow_stmt returns [ExplicitFlowStmt flow]
	: { boolean negated = false; } 
	 ('(' from=simple_params ')' | f=param_exp { from = new LinkedList<SimpleParameter>(); from.add(f); }) 
	 (ARROW | NOT_ARROW { negated = true; }) 
	 ('(' to=simple_out_params ')' | t=out_param_exp {to = new LinkedList<SimpleParameter>(); to.add(t); }) 
		{ 
		 flow = new ExplicitFlowStmt(from, to);
		 if (negated) {
		   flow.negateNoFlow();
		 } 
		}
	| WILDCARD 
		{ 
			List<SimpleParameter> fromList = new LinkedList<SimpleParameter>(); 
			fromList.add(new SimpleParameter(new SimpleParameter.Wildcard())); 
			List<SimpleParameter> toList = new LinkedList<SimpleParameter>(); 
			toList.add(new SimpleParameter(new SimpleParameter.Wildcard())); 
			flow = new ExplicitFlowStmt(fromList, toList); 
		}
	;

simple_out_params returns [List<SimpleParameter> params]
  : { params = new LinkedList<SimpleParameter>(); } p=out_param_exp { params.add(p); } (',' pOpt=out_param_exp { params.add(pOpt); })*
  ;

out_param_exp returns [SimpleParameter param]
	: p=param_exp { param = p; }
	| { param = new SimpleParameter(); SimpleParameter.Part part = null; } 
	     (RESULT { part = new SimpleParameter.Result(); } | EXC { part = new SimpleParameter.ExceptionValue(); }) 
	     ('.' p=param_exp { param = p; })? { param.addFirstPart(part); }
	| STATE { param = new SimpleParameter(new SimpleParameter.ProgramState()); }
	;

simple_params returns [List<SimpleParameter> params]
  : { params = new LinkedList<SimpleParameter>(); } p=param_exp { params.add(p); } (',' pOpt=param_exp { params.add(pOpt); })*
  ;
  
params returns [List<Parameter> params]
	: { params = new LinkedList<Parameter>(); } p=param { params.add(p); } (',' pOpt=param { params.add(pOpt); } )*
	;
	
param returns [Parameter param]
  : p=param_exp { param = p; }
  | { ParameterOptList l = new ParameterOptList(); } '[' p=param_exp { l.addParam(p); } (',' pOpt=param_exp { l.addParam(pOpt); })* ']' { param = l; }
  ;
  
param_exp returns [SimpleParameter param]
	: p=param_single ('.' WILDCARD { SimpleParameter.Part part = new SimpleParameter.Wildcard(); p.addLastPart(part); })? { param = p;}
	| WILDCARD { param = new SimpleParameter(new SimpleParameter.Wildcard()); }
	;

param_single	returns [SimpleParameter param]
	: id=IDENT { param = new SimpleParameter(new SimpleParameter.NormalPart(id.getText())); }
	     ({ SimpleParameter.Part part = null; } 
         ( '.' id=IDENT { part = new SimpleParameter.NormalPart(id.getText()); }
         // support array field references without a preceeding '.' to look mor java like
         // "a.f.[].i" is OK, but "a.f[].i" now works too....
         | '.'? ARRAY { part = new SimpleParameter.ArrayContent(); })
	     { param.addLastPart(part); })*
	;
	
WS_
	: ( 	' '
	        | '\t'
        	| '\n' //{ newline(); }
	        | '\r'
  ) { $channel=HIDDEN; }
	;

IDENT
	: ('a'..'z'|'A'..'Z'|'_') ('a'..'z'|'A'..'Z'|'_'|'0'..'9')*
	;

WILDCARD
	: '*'
	;

NOT_ARROW
  : ('-!>' | '\u21F8' | '\u219B')
  ;
  
ARROW
  : ('->' | '\u2192')
  ;
  
IMPLIES
  : ('=>' | '\u21D2')
  ;
  
ALIAS_OR
  : ('||' | '|' | '\u2228')
  ;
  
ALIAS_AND
  : ('&&' | '&' | '\u2227')
  ;
  
ALIAS_NOT
  : ('!' | '\u00AC')
  ;
  
RESULT
  : ('\\result' | '\\ret' | '\\return' | '\\res')
  ;

EXC
  : ('\\exc' | '\\e' | '\\exception')
  ;
  
STATE
  : ('\\state' | '\\s' | '\\stat')
  ;

ARRAY
  : '[]'
  ;
