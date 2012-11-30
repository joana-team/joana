/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.wala.flowless.test;

import java.io.IOException;
import java.util.List;

import junit.framework.TestCase;

import org.antlr.runtime.ANTLRFileStream;
import org.antlr.runtime.ANTLRStringStream;
import org.antlr.runtime.CommonTokenStream;
import org.antlr.runtime.RecognitionException;

import edu.kit.joana.wala.flowless.spec.FlowLessLexer;
import edu.kit.joana.wala.flowless.spec.FlowLessParser;
import edu.kit.joana.wala.flowless.spec.FlowLessSimplifier;
import edu.kit.joana.wala.flowless.spec.FlowLessSimplifier.BasicIFCStmt;
import edu.kit.joana.wala.flowless.spec.ast.AstElement;
import edu.kit.joana.wala.flowless.spec.ast.IFCStmt;
import edu.kit.joana.wala.flowless.spec.ast.FlowAstVisitor.FlowAstException;

public class TestFlowLessParsing extends TestCase {

	public void testOk1() throws IOException, RecognitionException, FlowAstException {
		IFCStmt ifc = runParser("examples/ok1.flo");

		assertNotNull(ifc);

		System.out.println(ifc);

		assertEquals(AstElement.Type.IFC, ifc.getType());

		assertEquals("{a, b} => (b)->(c)", ifc.toString());

		List<BasicIFCStmt> basics = FlowLessSimplifier.simplify(ifc);

		BasicIFCStmt supremum = FlowLessSimplifier.upperBound(basics);
		System.out.println("SUP: " + supremum);

		BasicIFCStmt infimum = FlowLessSimplifier.lowerBound(basics);
		System.out.println("INF: " + infimum);
	}


	public void testOk2() throws IOException, RecognitionException, FlowAstException {
		IFCStmt ifc = runParser("examples/ok2.flo");

		assertNotNull(ifc);

		System.out.println(ifc);

		assertEquals(AstElement.Type.IFC, ifc.getType());

		assertEquals("(({a.*, b}) && (!{x, a})) || ({[g, a.*], t}) => (b)->(c), (x)->(\\result), (a)->(\\exc)", ifc.toString());

		List<BasicIFCStmt> basics = FlowLessSimplifier.simplify(ifc);

		BasicIFCStmt supremum = FlowLessSimplifier.upperBound(basics);
		System.out.println("SUP: " + supremum);

		BasicIFCStmt infimum = FlowLessSimplifier.lowerBound(basics);
		System.out.println("INF: " + infimum);
	}

	public void testOk3() throws IOException, RecognitionException, FlowAstException {
		IFCStmt ifc = runParser("examples/ok3.flo");

		assertNotNull(ifc);

		System.out.println(ifc);

		assertEquals(AstElement.Type.IFC, ifc.getType());

		assertEquals(" => (b)->(c, a.c.*), (x.a.e)->(\\state)", ifc.toString());

		List<BasicIFCStmt> basics = FlowLessSimplifier.simplify(ifc);

		BasicIFCStmt supremum = FlowLessSimplifier.upperBound(basics);
		System.out.println("SUP: " + supremum);

		BasicIFCStmt infimum = FlowLessSimplifier.lowerBound(basics);
		System.out.println("INF: " + infimum);
	}

	public void testOk4() throws IOException, RecognitionException, FlowAstException {
		IFCStmt ifc = runParser("examples/ok4.flo");

		assertNotNull(ifc);

		System.out.println(ifc);

		assertEquals(AstElement.Type.IFC, ifc.getType());

		assertEquals("({a.*, b}) && (!{x, a}) => (*)->(*)", ifc.toString());

		List<BasicIFCStmt> basics = FlowLessSimplifier.simplify(ifc);

		BasicIFCStmt supremum = FlowLessSimplifier.upperBound(basics);
		System.out.println("SUP: " + supremum);

		BasicIFCStmt infimum = FlowLessSimplifier.lowerBound(basics);
		System.out.println("INF: " + infimum);
	}

	public void testOk5() throws IOException, RecognitionException, FlowAstException {
		IFCStmt ifc = runParser("examples/ok5.flo");

		assertNotNull(ifc);

		System.out.println(ifc);

		assertEquals(AstElement.Type.IFC, ifc.getType());

		assertEquals("(!{a.*, b}) || (!{x, a}) => (x.c.z)-!>(\\result, \\exc, a.*)", ifc.toString());

		List<BasicIFCStmt> basics = FlowLessSimplifier.simplify(ifc);

		BasicIFCStmt supremum = FlowLessSimplifier.upperBound(basics);
		System.out.println("SUP: " + supremum);

		BasicIFCStmt infimum = FlowLessSimplifier.lowerBound(basics);
		System.out.println("INF: " + infimum);
	}

	public void testOk6() throws IOException, RecognitionException, FlowAstException {
		IFCStmt ifc = runParser("examples/ok6.flo");

		assertNotNull(ifc);

		System.out.println(ifc);

		assertEquals(AstElement.Type.IFC, ifc.getType());

		assertEquals("(({[a, x, y], [b, c], y}) && (!{x, t, [a, f, i]})) || ({[g, a.i], t}) => (b)-!>(c)", ifc.toString());

		List<BasicIFCStmt> basics = FlowLessSimplifier.simplify(ifc);

		BasicIFCStmt supremum = FlowLessSimplifier.upperBound(basics);
		System.out.println("SUP: " + supremum);

		BasicIFCStmt infimum = FlowLessSimplifier.lowerBound(basics);
		System.out.println("INF: " + infimum);
	}

	public void testOk7() throws IOException, RecognitionException, FlowAstException {
		IFCStmt ifc = runParser("examples/ok7.flo");

		assertNotNull(ifc);

		System.out.println(ifc);

		assertEquals(AstElement.Type.IFC, ifc.getType());

		assertEquals("({a, b}) && (({c, d}) || ({e, f})) => (*)->(*)", ifc.toString());

		List<BasicIFCStmt> basics = FlowLessSimplifier.simplify(ifc);

		BasicIFCStmt supremum = FlowLessSimplifier.upperBound(basics);
		System.out.println("SUP: " + supremum);

		BasicIFCStmt infimum = FlowLessSimplifier.lowerBound(basics);
		System.out.println("INF: " + infimum);
	}

	public void testOk8() throws IOException, RecognitionException, FlowAstException {
		IFCStmt ifc = runParser("examples/ok8.flo");

		assertNotNull(ifc);

		System.out.println(ifc);

		assertEquals(AstElement.Type.IFC, ifc.getType());

		assertEquals("({a, b}) && ((({c, d}) || ({g, h})) || ({e, f})) => (*)->(*)", ifc.toString());

		List<BasicIFCStmt> basics = FlowLessSimplifier.simplify(ifc);

		BasicIFCStmt supremum = FlowLessSimplifier.upperBound(basics);
		System.out.println("SUP: " + supremum);

		BasicIFCStmt infimum = FlowLessSimplifier.lowerBound(basics);
		System.out.println("INF: " + infimum);
	}

	public void testOk9() throws IOException, RecognitionException, FlowAstException {
		IFCStmt ifc = runParser("examples/ok9.flo");

		assertNotNull(ifc);

		System.out.println(ifc);

		assertEquals(AstElement.Type.IFC, ifc.getType());

		assertEquals("({a, b}) && ((({c, d}) || (({g, h}) && ({k, l}))) || (({e, f}) || ({i, j}))) => (*)->(*)", ifc.toString());

		List<BasicIFCStmt> basics = FlowLessSimplifier.simplify(ifc);

		BasicIFCStmt supremum = FlowLessSimplifier.upperBound(basics);
		System.out.println("SUP: " + supremum);

		BasicIFCStmt infimum = FlowLessSimplifier.lowerBound(basics);
		System.out.println("INF: " + infimum);
	}

	public void testOk10() throws IOException, RecognitionException, FlowAstException {
		IFCStmt ifc = runParser("examples/ok10.flo");

		assertNotNull(ifc);

		System.out.println(ifc);

		assertEquals(AstElement.Type.IFC, ifc.getType());

		assertEquals("(({a, b}) && (1(a, b, c))) || ((1(c)) && (1(x, a, b))) => (*)->(*)", ifc.toString());

		List<BasicIFCStmt> basics = FlowLessSimplifier.simplify(ifc);

		BasicIFCStmt supremum = FlowLessSimplifier.upperBound(basics);
		System.out.println("SUP: " + supremum);

		BasicIFCStmt infimum = FlowLessSimplifier.lowerBound(basics);
		System.out.println("INF: " + infimum);
	}

	public void testOk11() throws IOException, RecognitionException, FlowAstException {
		IFCStmt ifc = runParser("examples/ok11.flo");

		assertNotNull(ifc);

		System.out.println(ifc);

		assertEquals(AstElement.Type.IFC, ifc.getType());

		assertEquals("({a, b}) && (1(a, [b, c, x.i])) => (*)->(*)", ifc.toString());

		List<BasicIFCStmt> basics = FlowLessSimplifier.simplify(ifc);

		BasicIFCStmt supremum = FlowLessSimplifier.upperBound(basics);
		System.out.println("SUP: " + supremum);

		BasicIFCStmt infimum = FlowLessSimplifier.lowerBound(basics);
		System.out.println("INF: " + infimum);
	}

	public void testOk12() throws IOException, RecognitionException, FlowAstException {
		IFCStmt ifc = runParser("examples/ok12.flo");

		assertNotNull(ifc);

		System.out.println(ifc);

		assertEquals(AstElement.Type.IFC, ifc.getType());

		assertEquals(" => (b)->(c, a.c), pure(x, a.i), pure(*), (d)->(b.i)", ifc.toString());

		List<BasicIFCStmt> basics = FlowLessSimplifier.simplify(ifc);

		BasicIFCStmt supremum = FlowLessSimplifier.upperBound(basics);
		System.out.println("SUP: " + supremum);

		BasicIFCStmt infimum = FlowLessSimplifier.lowerBound(basics);
		System.out.println("INF: " + infimum);
	}

	public void testReadOwnOutputAgainOk1() throws IOException, RecognitionException {
		testReadOwnOutputAgain("examples/ok1.flo");
	}

	public void testReadOwnOutputAgainOk2() throws IOException, RecognitionException {
		testReadOwnOutputAgain("examples/ok2.flo");
	}

	public void testReadOwnOutputAgainOk3() throws IOException, RecognitionException {
		testReadOwnOutputAgain("examples/ok3.flo");
	}

	public void testReadOwnOutputAgainOk4() throws IOException, RecognitionException {
		testReadOwnOutputAgain("examples/ok4.flo");
	}

	public void testReadOwnOutputAgainOk5() throws IOException, RecognitionException {
		testReadOwnOutputAgain("examples/ok5.flo");
	}

	public void testReadOwnOutputAgainOk6() throws IOException, RecognitionException {
		testReadOwnOutputAgain("examples/ok6.flo");
	}

	public void testReadOwnOutputAgainOk7() throws IOException, RecognitionException {
		testReadOwnOutputAgain("examples/ok7.flo");
	}

	public void testReadOwnOutputAgainOk8() throws IOException, RecognitionException {
		testReadOwnOutputAgain("examples/ok8.flo");
	}

	public void testReadOwnOutputAgainOk9() throws IOException, RecognitionException {
		testReadOwnOutputAgain("examples/ok9.flo");
	}

	public void testReadOwnOutputAgainOk10() throws IOException, RecognitionException {
		testReadOwnOutputAgain("examples/ok10.flo");
	}

	public void testReadOwnOutputAgainOk11() throws IOException, RecognitionException {
		testReadOwnOutputAgain("examples/ok11.flo");
	}

	public void testReadOwnOutputAgainOk12() throws IOException, RecognitionException {
		testReadOwnOutputAgain("examples/ok12.flo");
	}

	private void testReadOwnOutputAgain(String fileName) throws IOException, RecognitionException {
		IFCStmt ifc = runParser(fileName);

		assertNotNull(ifc);

		System.out.println("Parsing: '" + ifc.toString() + "'");

		IFCStmt ifc2 = runParserOnText(ifc.toString());

		assertNotNull(ifc2);

		System.out.println("Output:  '" + ifc2.toString() + "'");

		assertEquals(ifc.toString(), ifc2.toString());
	}

	private static IFCStmt runParserOnText(String inputTxt) throws RecognitionException {
		ANTLRStringStream sstream = new ANTLRStringStream(inputTxt);
		FlowLessLexer lexer = new FlowLessLexer(sstream);
		FlowLessParser parser = new FlowLessParser(new CommonTokenStream(lexer));

		return parser.ifc_stmt();
	}

	private static IFCStmt runParser(String fileName) throws IOException, RecognitionException {
		System.out.println("Reading " + fileName);

		FlowLessLexer lexer = new FlowLessLexer(new ANTLRFileStream(fileName));
		FlowLessParser parser = new FlowLessParser(new CommonTokenStream(lexer));

		return parser.ifc_stmt();
	}

}
