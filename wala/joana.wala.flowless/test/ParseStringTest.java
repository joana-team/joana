/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
import org.antlr.runtime.ANTLRStringStream;
import org.antlr.runtime.CommonTokenStream;
import org.antlr.runtime.RecognitionException;

import edu.kit.joana.wala.flowless.spec.FlowLessLexer;
import edu.kit.joana.wala.flowless.spec.FlowLessParser;
import edu.kit.joana.wala.flowless.spec.ast.IFCStmt;


public class ParseStringTest {

	public static void main(String[] argv) throws RecognitionException {
		final IFCStmt stmt = runParserOnText("!{b,b} & !{d,a} & !{a,a} => a.f2.f3 -!> \result");
		System.out.println(stmt.toString());
	}

	private static IFCStmt runParserOnText(String inputTxt) throws RecognitionException {
		ANTLRStringStream sstream = new ANTLRStringStream(inputTxt);
		FlowLessLexer lexer = new FlowLessLexer(sstream);
		FlowLessParser parser = new FlowLessParser(new CommonTokenStream(lexer));

		return parser.ifc_stmt();
	}

}
