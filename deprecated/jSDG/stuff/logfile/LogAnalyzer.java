/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.wala.util;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;

import antlr.RecognitionException;
import antlr.Token;
import antlr.TokenStreamException;

public class LogAnalyzer {

	/**
	 * @param args
	 * @throws FileNotFoundException
	 * @throws TokenStreamException
	 * @throws RecognitionException
	 */
	public static void main(String[] args) throws FileNotFoundException, TokenStreamException, RecognitionException {
		try {
			BufferedInputStream bIn = new BufferedInputStream(new FileInputStream("stuff/logfile/sample.log"));
			LogFileLexer lexer = new LogFileLexer(bIn);
			LogFileParser parser = new LogFileParser(lexer);
			parser.startRule();
			/*
			Token tok = lexer.nextToken();
			while (tok != null && tok.getType() != LogFileLexerTokenTypes.EOF) {
				System.out.println(tok.toString());
				tok = lexer.nextToken();
			}*/
		} catch (Throwable t) {
			t.printStackTrace();
		}
	}

}
