/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */

package edu.kit.joana.ifc.sdg.qifc.nildumu.interproc;

import static edu.kit.joana.ifc.sdg.qifc.nildumu.util.Util.p;

import java.util.Properties;

import edu.kit.joana.ifc.sdg.qifc.nildumu.util.Pair;

/**
 * Parser of handler configuration strings.
 * 
 * It supports semicolon separated {@code key=value} pairs,
 * values can either be strings or key value pairs 
 * (enclosed in curly braces)
 */
public class HandlerConfigParser {

	public final static int EOF = -1;

	private final String input;
	private int cur = 0;

	public HandlerConfigParser(String input) {
		this.input = input;
		this.cur = 0;
	}

	public boolean is(int c){
		return c == cur();
	}

	public int cur(){
		return cur < input.length() ? input.charAt(cur) : -1;
	}

	public void error(String msg){
		throw new MethodInvocationHandlerInitializationException(
				String.format("%s[%s]%s",input.substring(0, cur), 
						msg, 
						cur >= input.length() ? "" : input.substring(cur)));
	}

	private void expect(char c){
		if (cur() != c){
			error(String.format("expected %s", c));
		}
		next();
	}

	public int next(){
		cur += 1;
		while (Character.isWhitespace(cur())){
			next();
		}
		return cur();
	}

	public Properties parse(){
		Properties props = new Properties();
		while (!atEnd()){
			Pair<String, String> prop = parseProperty();
			props.setProperty(prop.first, prop.second);
			if (is(';')){
				next();
			}
		}
		return props;
	}

	private Pair<String, String> parseProperty(){
		String id = parseIdentifier();
		expect('=');
		String argument = parseArgument();
		return p(id, argument);
	}

	private String parseIdentifier(){
		StringBuilder builder = new StringBuilder();
		while (Character.isJavaIdentifierPart(cur())){
			builder.appendCodePoint(cur());
			next();
		}
		return builder.toString();
	}

	private String parseArgument(){
		StringBuilder builder = new StringBuilder();
		int braceCounter = 0;
		while (!((is(';') || atEnd()) && braceCounter == 0)){
			switch (cur()){
			case EOF:
				error("unexpected end");
			case '{':
				braceCounter++;
				break;
			case '}':
				braceCounter--;
			}
			builder.appendCodePoint(cur());
			next();
		}
		String ret = builder.toString();
		if (ret.charAt(0) == '{'){
			return ret.substring(1, ret.length() - 1);
		}
		return ret;
	}

	private boolean atEnd(){
		return cur() == EOF;
	}
}
