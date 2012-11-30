/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.wala.flowless.test;


public class TestParsingAndSemantics extends AbstractParsingAndSemanticsTestCase {

	public TestParsingAndSemantics() {
		super("examples/project1");
	}

	public void testNoSyntacticError1() {
		runExpectedOk("Library2", "noSyntacticError1", "There should be no exception.");
	}

	public void testNoSyntacticError2() {
		runExpectedOk("Library2", "noSyntacticError2", "There should be no exception.");
	}

	public void testSyntacticError3() {
		runExpectedError("Library2", "syntacticError3", "A syntactical exception was expected to appear.");
	}

	public void testNoSemanticError1() {
		runExpectedOk("Library2", "noSemanticError1", "There should be no exception.");
	}

	public void testNoSemanticError2() {
		runExpectedOk("Library2", "noSemanticError2", "There should be no exception.");
	}

	public void testOk1() {
		runExpectedOk("Library2", "ok1", "There should be no exception.");
	}

	public void testOk2() {
		runExpectedOk("Library2", "ok1", "There should be no exception.");
	}

}
