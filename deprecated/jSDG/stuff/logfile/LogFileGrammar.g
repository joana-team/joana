header {
package edu.kit.joana.wala.util;

}

class LogFileLexer extends Lexer;

options {
	k = 2;
}

NUMBER       : ('0'..'9')+;
INFO         : "[INFO]";
DEBUG        : "[DEBUG]";// {$setType(Token.SKIP);};
WARN         : "[WARN]";// {$setType(Token.SKIP);};
ERROR        : "[ERROR]";// {$setType(Token.SKIP);};

//PDG_CREATE	 : "Creating PDG";
//NODES_CREATE : "nodes created.";
//CONTROL_DEP  : "control dependencies.";
//DATA_DEP	 : "data dependencies.";
//ROOT_FIN	 : "root form-in nodes,";
//ROOT_FOUT    : "root form-out nodes";
//STATIC_FIN	 : "static form-in nodes,";
//STATIC_FOUT  : "static form-out nodes";
//OBJ_FIN		 : "form-in object field nodes,";
//OBJ_FOUT	 : "form-out obj field nodes";
//PDG_DONE	 : "PDG done.";
//WORD		 : ('a'..'z' | 'A'..'Z')('a'..'z' | 'A'..'Z' | '0'..'9')*
//LBRACK		 : '[';
//RBRACK		 : ']';
//LBRACE		 : '(';
//RBRACE		 : ')';
//METHOD		 : ('a'..'z' | 'A'..'Z')('a'..'z' | 'A'..'Z' | '0'..'9' | '[' | ']' | '<' | '>' | '(' | ')' | '.' | '$')+;
WS           : ('\n' { newline(); } |'\r'|' '|'\t')+ {$setType(Token.SKIP);};
TEXT		: ('a'..'z' | 'A'..'Z' | '0'..'9' | '-' | ',' | ':' | '_' | '/' | '=' | '[' | ']' | '<' | '>' | '(' | ')' | '.' | '$')+;

class LogFileParser extends Parser;

startRule returns [String log]
	{ log = null; }
    : log=logMessages EOF
    ;
    
    
logMessages returns [String msg]
	{ msg = null; }
	: DEBUG msg=message logMessages {msg = "D: " + msg; System.out.println(msg);}
	| INFO msg=message logMessages {msg = "I: " + msg; System.out.println(msg);}
	| WARN msg=message logMessages {msg = "W: " + msg; System.out.println(msg);}
	| ERROR msg=message logMessages {msg = "E: " + msg; System.out.println(msg);}
	| 
	;

message returns [String txt]
	{ txt = ""; String tmp;}
	: "Creating" "PDG" tmp=message {txt = "***" + tmp;}
	| "PDG" "done." tmp=message {txt = "done";}
	| w:TEXT tmp=message {txt = w + tmp;}
	| n:NUMBER tmp=message {txt = n + tmp;}
	| 
	;

pdgCreation
	: PDG_CREATE m:METHOD NUMBER NODES_CREATE NUMBER CONTROL_DEP NUMBER DATA_DEP NUMBER ROOT_FIN NUMBER ROOT_FOUT NUMBER STATIC_FIN NUMBER STATIC_FOUT NUMBER OBJ_FIN NUMBER OBJ_FOUT PDG_DONE
	{ System.out.println("Parsed " + m.getText());}
	;
	
	
	
