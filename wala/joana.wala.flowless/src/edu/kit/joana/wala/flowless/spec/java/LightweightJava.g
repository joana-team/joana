/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
/*
 * @author Juergen Graf <graf@kit.edu>
 */
lexer grammar LightweightJava;

options {
  language = Java;
  //k = 2;
}

@header {/**
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
import java.util.List;
import java.util.Map;
}

@lexer::members {
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
}

SC: ';';

WS_
  : (   ' '
          | '\t'
          | '\n' //{ newline(); }
          | '\r'
          | '\u000C'
  ) { $channel = HIDDEN; }
  ;

LINE_COMMENT : '//' (~'\n')* ('\n' | EOF) ;

BLOCK_COMMENT : BLC_START (options { greedy = false; } : ~(BLC_END))* BLC_END;

protected
BLC_START : '/*' ;

protected
BLC_END : '*/' ;

//CHAR : '\'' (options { greedy = false; } : ('\\\\' | '\\\'' | ~('\'')))+ '\'' ;
CHAR : '\'' ( CHAR_ESC | ~('\'' | '\\'))+ '\'' ;

protected
//CHAR_ESC : '\\' ('\\' | '\'' );
CHAR_ESC : '\\' ('\'' | INT | 'a'..'z' | 'A'..'Z' | '\\' | '\"') ;

STRING : '"' (CHAR_ESC | ~('\\' | '"' | '\n') )* '"' ;

//protected
//ESC    : '\\' ('"' | INT | 'a'..'z' | '\\') ;

INT    : ('0'..'9')+ ;

PARAM_START : '(' ;

PARAM_END : ')' ;

ARRAY_DEF : '[]' ;

BRACK_START : '[' ;

BRACK_END : ']' ;

LT : '<' ;

GT : '>' ;

COMMA : ',' ;

DOT : '.' ;

DP : ':' ;

BR_OPEN : '{' ;

BR_CLOSE : '}' ;

CLASS : 'class' ;

KEYWORD : ( 'strictfp' | 'native' | 'public' | 'private' | 'protected' | 'final' | 'volatile' | 'synchronized' | 'abstract' ) { $channel = HIDDEN; };
 
STATIC : 'static' ;

INTERFACE : 'interface' ;

PACKAGE : 'package' ;

ANNOTATION : ( '@' ('a'..'z'|'A'..'Z'|'_') ('a'..'z'|'A'..'Z'|'_'|'0'..'9')* ) ;//{ $channel = HIDDEN; };

IDENT
  : ('a'..'z'|'A'..'Z'|'_') ('a'..'z'|'A'..'Z'|'_'|'0'..'9')*
  ;

ASSIGN : '=' ;

EXP_OP : '!' | '+=' | '~' | '-=' | '*' | '+' | '-' | '/' | '&' | '^' | '%' | '<=' | '>=' | '==' | '|' | '||' | '&&' | '?';

