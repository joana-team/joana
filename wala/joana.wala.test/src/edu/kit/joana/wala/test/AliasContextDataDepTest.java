/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.wala.test;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;

import org.junit.Test;

import com.ibm.wala.ipa.cha.ClassHierarchyException;
import com.ibm.wala.util.CancelException;
import com.ibm.wala.util.graph.GraphIntegrity.UnsoundGraphException;

import edu.kit.joana.ifc.sdg.graph.SDG;


/**
 * @author Juergen Graf <juergen.graf@gmail.com>
 *
 */
public class AliasContextDataDepTest {

	public static final String out = "./out/";

	@Test
	public void aliasDataDep_Test_foo1() {
		final String src = "../MoJo-TestCode/src";
		final String bin = "../MoJo-TestCode/bin";
		final String method = "Test.foo1(LTest$A;LTest$A;)LTest$A2;";

		try {
			final MoJoTest mt = MoJoTest.create(src, bin, out);
			final SDG sdg = mt.analyzeMethod(method);
			assertTrue(sdg.vertexSet().size() > 0);
		} catch (ClassHierarchyException e) {
			e.printStackTrace();
			fail(e.getMessage());
		} catch (IOException e) {
			e.printStackTrace();
			fail(e.getMessage());
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
			fail(e.getMessage());
		} catch (CancelException e) {
			e.printStackTrace();
			fail(e.getMessage());
		} catch (UnsoundGraphException e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

	@Test
	public void aliasDataDep_Test_impossibleAlias() {
		final String src = "../MoJo-TestCode/src";
		final String bin = "../MoJo-TestCode/bin";
		final String method = "Test.impossibleAlias(LTest$A2;LTest$A2;LTest$A3;LTest$A3;)I";

		try {
			final MoJoTest mt = MoJoTest.create(src, bin, out);
			final SDG sdg = mt.analyzeMethod(method);
			assertTrue(sdg.vertexSet().size() > 0);
		} catch (ClassHierarchyException e) {
			e.printStackTrace();
			fail(e.getMessage());
		} catch (IOException e) {
			e.printStackTrace();
			fail(e.getMessage());
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
			fail(e.getMessage());
		} catch (CancelException e) {
			e.printStackTrace();
			fail(e.getMessage());
		} catch (UnsoundGraphException e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

	@Test
	public void aliasDataDep_Test_indirectFoo1() {
		final String src = "../MoJo-TestCode/src";
		final String bin = "../MoJo-TestCode/bin";
		final String method = "Test.indirectFoo1(LTest$A;LTest$A;)LTest$A2;";

		try {
			final MoJoTest mt = MoJoTest.create(src, bin, out);
			final SDG sdg = mt.analyzeMethod(method);
			assertTrue(sdg.vertexSet().size() > 0);
		} catch (ClassHierarchyException e) {
			e.printStackTrace();
			fail(e.getMessage());
		} catch (IOException e) {
			e.printStackTrace();
			fail(e.getMessage());
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
			fail(e.getMessage());
		} catch (CancelException e) {
			e.printStackTrace();
			fail(e.getMessage());
		} catch (UnsoundGraphException e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

	@Test
	public void aliasDataDep_Test_indirectRevFoo1() {
		final String src = "../MoJo-TestCode/src";
		final String bin = "../MoJo-TestCode/bin";
		final String method = "Test.indirectRevFoo1(LTest$A;LTest$A;)LTest$A2;";

		try {
			final MoJoTest mt = MoJoTest.create(src, bin, out);
			final SDG sdg = mt.analyzeMethod(method);
			assertTrue(sdg.vertexSet().size() > 0);
		} catch (ClassHierarchyException e) {
			e.printStackTrace();
			fail(e.getMessage());
		} catch (IOException e) {
			e.printStackTrace();
			fail(e.getMessage());
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
			fail(e.getMessage());
		} catch (CancelException e) {
			e.printStackTrace();
			fail(e.getMessage());
		} catch (UnsoundGraphException e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

	@Test
	public void aliasDataDep_Test_indirectMultipleFoo1() {
		final String src = "../MoJo-TestCode/src";
		final String bin = "../MoJo-TestCode/bin";
		final String method = "Test.indirectMultipleFoo1(LTest$A;LTest$A;)LTest$A2;";

		try {
			final MoJoTest mt = MoJoTest.create(src, bin, out);
			final SDG sdg = mt.analyzeMethod(method);
			assertTrue(sdg.vertexSet().size() > 0);
		} catch (ClassHierarchyException e) {
			e.printStackTrace();
			fail(e.getMessage());
		} catch (IOException e) {
			e.printStackTrace();
			fail(e.getMessage());
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
			fail(e.getMessage());
		} catch (CancelException e) {
			e.printStackTrace();
			fail(e.getMessage());
		} catch (UnsoundGraphException e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

	@Test
	public void aliasDataDep_Test_indirectSameFoo1() {
		final String src = "../MoJo-TestCode/src";
		final String bin = "../MoJo-TestCode/bin";
		final String method = "Test.indirectSameFoo1(LTest$A;LTest$A;)LTest$A2;";

		try {
			final MoJoTest mt = MoJoTest.create(src, bin, out);
			final SDG sdg = mt.analyzeMethod(method);
			assertTrue(sdg.vertexSet().size() > 0);
		} catch (ClassHierarchyException e) {
			e.printStackTrace();
			fail(e.getMessage());
		} catch (IOException e) {
			e.printStackTrace();
			fail(e.getMessage());
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
			fail(e.getMessage());
		} catch (CancelException e) {
			e.printStackTrace();
			fail(e.getMessage());
		} catch (UnsoundGraphException e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

	@Test
	public void aliasDataDep_Test_foo2() {
		final String src = "../MoJo-TestCode/src";
		final String bin = "../MoJo-TestCode/bin";
		final String method = "Test.foo2(LTest$A;LTest$A;)I";

		try {
			final MoJoTest mt = MoJoTest.create(src, bin, out);
			final SDG sdg = mt.analyzeMethod(method);
			assertTrue(sdg.vertexSet().size() > 0);
		} catch (ClassHierarchyException e) {
			e.printStackTrace();
			fail(e.getMessage());
		} catch (IOException e) {
			e.printStackTrace();
			fail(e.getMessage());
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
			fail(e.getMessage());
		} catch (CancelException e) {
			e.printStackTrace();
			fail(e.getMessage());
		} catch (UnsoundGraphException e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

	@Test
	public void aliasDataDep_Test_foo3() {
		final String src = "../MoJo-TestCode/src";
		final String bin = "../MoJo-TestCode/bin";
		final String method = "Test.foo3(LTest$A;LTest$A;)I";

		try {
			final MoJoTest mt = MoJoTest.create(src, bin, out);
			final SDG sdg = mt.analyzeMethod(method);
			assertTrue(sdg.vertexSet().size() > 0);
		} catch (ClassHierarchyException e) {
			e.printStackTrace();
			fail(e.getMessage());
		} catch (IOException e) {
			e.printStackTrace();
			fail(e.getMessage());
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
			fail(e.getMessage());
		} catch (CancelException e) {
			e.printStackTrace();
			fail(e.getMessage());
		} catch (UnsoundGraphException e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

	@Test
	public void aliasDataDep_Test_foo4() {
		final String src = "../MoJo-TestCode/src";
		final String bin = "../MoJo-TestCode/bin";
		final String method = "Test.foo4(LTest$A;LTest$A;LTest$A;LTest$A;)I";

		try {
			final MoJoTest mt = MoJoTest.create(src, bin, out);
			final SDG sdg = mt.analyzeMethod(method);
			assertTrue(sdg.vertexSet().size() > 0);
		} catch (ClassHierarchyException e) {
			e.printStackTrace();
			fail(e.getMessage());
		} catch (IOException e) {
			e.printStackTrace();
			fail(e.getMessage());
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
			fail(e.getMessage());
		} catch (CancelException e) {
			e.printStackTrace();
			fail(e.getMessage());
		} catch (UnsoundGraphException e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

	@Test
	public void aliasDataDep_Test_foo5() {
		final String src = "../MoJo-TestCode/src";
		final String bin = "../MoJo-TestCode/bin";
		final String method = "Test.foo5(LTest$A;LTest$A;LTest$A;LTest$A;)I";

		try {
			final MoJoTest mt = MoJoTest.create(src, bin, out);
			final SDG sdg = mt.analyzeMethod(method);
			assertTrue(sdg.vertexSet().size() > 0);
		} catch (ClassHierarchyException e) {
			e.printStackTrace();
			fail(e.getMessage());
		} catch (IOException e) {
			e.printStackTrace();
			fail(e.getMessage());
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
			fail(e.getMessage());
		} catch (CancelException e) {
			e.printStackTrace();
			fail(e.getMessage());
		} catch (UnsoundGraphException e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

	@Test
	public void aliasDataDep_Test_foo6() {
		final String src = "../MoJo-TestCode/src";
		final String bin = "../MoJo-TestCode/bin";
		final String method = "Test.foo6(LTest$A;LTest$A;)I";

		try {
			final MoJoTest mt = MoJoTest.create(src, bin, out);
			final SDG sdg = mt.analyzeMethod(method);
			assertTrue(sdg.vertexSet().size() > 0);
		} catch (ClassHierarchyException e) {
			e.printStackTrace();
			fail(e.getMessage());
		} catch (IOException e) {
			e.printStackTrace();
			fail(e.getMessage());
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
			fail(e.getMessage());
		} catch (CancelException e) {
			e.printStackTrace();
			fail(e.getMessage());
		} catch (UnsoundGraphException e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

	@Test
	public void aliasDataDep_Test_foo7() {
		final String src = "../MoJo-TestCode/src";
		final String bin = "../MoJo-TestCode/bin";
		final String method = "Test.foo7(LTest$A3;LTest$A3;)I";

		try {
			final MoJoTest mt = MoJoTest.create(src, bin, out);
			final SDG sdg = mt.analyzeMethod(method);
			assertTrue(sdg.vertexSet().size() > 0);
		} catch (ClassHierarchyException e) {
			e.printStackTrace();
			fail(e.getMessage());
		} catch (IOException e) {
			e.printStackTrace();
			fail(e.getMessage());
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
			fail(e.getMessage());
		} catch (CancelException e) {
			e.printStackTrace();
			fail(e.getMessage());
		} catch (UnsoundGraphException e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

	@Test
	public void aliasDataDep_Test_foo8() {
		final String src = "../MoJo-TestCode/src";
		final String bin = "../MoJo-TestCode/bin";
		final String method = "Test.foo8(LTest$A3;LTest$A3;)I";

		try {
			final MoJoTest mt = MoJoTest.create(src, bin, out);
			final SDG sdg = mt.analyzeMethod(method);
			assertTrue(sdg.vertexSet().size() > 0);
		} catch (ClassHierarchyException e) {
			e.printStackTrace();
			fail(e.getMessage());
		} catch (IOException e) {
			e.printStackTrace();
			fail(e.getMessage());
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
			fail(e.getMessage());
		} catch (CancelException e) {
			e.printStackTrace();
			fail(e.getMessage());
		} catch (UnsoundGraphException e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

	@Test
	public void aliasDataDep_Test_foo9() {
		final String src = "../MoJo-TestCode/src";
		final String bin = "../MoJo-TestCode/bin";
		final String method = "Test.foo9(LTest$A;LTest$A2;)I";

		try {
			final MoJoTest mt = MoJoTest.create(src, bin, out);
			final SDG sdg = mt.analyzeMethod(method);
			assertTrue(sdg.vertexSet().size() > 0);
		} catch (ClassHierarchyException e) {
			e.printStackTrace();
			fail(e.getMessage());
		} catch (IOException e) {
			e.printStackTrace();
			fail(e.getMessage());
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
			fail(e.getMessage());
		} catch (CancelException e) {
			e.printStackTrace();
			fail(e.getMessage());
		} catch (UnsoundGraphException e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

	@Test
	public void aliasDataDep_Test_foo10() {
		final String src = "../MoJo-TestCode/src";
		final String bin = "../MoJo-TestCode/bin";
		final String method = "Test.foo10(LTest$A2;LTest$A2;)V";

		try {
			final MoJoTest mt = MoJoTest.create(src, bin, out);
			final SDG sdg = mt.analyzeMethod(method);
			assertTrue(sdg.vertexSet().size() > 0);
		} catch (ClassHierarchyException e) {
			e.printStackTrace();
			fail(e.getMessage());
		} catch (IOException e) {
			e.printStackTrace();
			fail(e.getMessage());
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
			fail(e.getMessage());
		} catch (CancelException e) {
			e.printStackTrace();
			fail(e.getMessage());
		} catch (UnsoundGraphException e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

	@Test
	public void aliasDataDep_Test_foo11() {
		final String src = "../MoJo-TestCode/src";
		final String bin = "../MoJo-TestCode/bin";
		final String method = "Test.foo11()LTest$A3;";

		try {
			final MoJoTest mt = MoJoTest.create(src, bin, out);
			final SDG sdg = mt.analyzeMethod(method);
			assertTrue(sdg.vertexSet().size() > 0);
		} catch (ClassHierarchyException e) {
			e.printStackTrace();
			fail(e.getMessage());
		} catch (IOException e) {
			e.printStackTrace();
			fail(e.getMessage());
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
			fail(e.getMessage());
		} catch (CancelException e) {
			e.printStackTrace();
			fail(e.getMessage());
		} catch (UnsoundGraphException e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

	@Test
	public void aliasDataDep_Test_foo12() {
		final String src = "../MoJo-TestCode/src";
		final String bin = "../MoJo-TestCode/bin";
		final String method = "Test.foo12(LTest$A3;LTest$A3;)V";

		try {
			final MoJoTest mt = MoJoTest.create(src, bin, out);
			final SDG sdg = mt.analyzeMethod(method);
			assertTrue(sdg.vertexSet().size() > 0);
		} catch (ClassHierarchyException e) {
			e.printStackTrace();
			fail(e.getMessage());
		} catch (IOException e) {
			e.printStackTrace();
			fail(e.getMessage());
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
			fail(e.getMessage());
		} catch (CancelException e) {
			e.printStackTrace();
			fail(e.getMessage());
		} catch (UnsoundGraphException e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

	@Test
	public void aliasDataDep_Test_foo13() {
		final String src = "../MoJo-TestCode/src";
		final String bin = "../MoJo-TestCode/bin";
		final String method = "Test.foo13(LTest$A3;LTest$A3;)V";

		try {
			final MoJoTest mt = MoJoTest.create(src, bin, out);
			final SDG sdg = mt.analyzeMethod(method);
			assertTrue(sdg.vertexSet().size() > 0);
		} catch (ClassHierarchyException e) {
			e.printStackTrace();
			fail(e.getMessage());
		} catch (IOException e) {
			e.printStackTrace();
			fail(e.getMessage());
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
			fail(e.getMessage());
		} catch (CancelException e) {
			e.printStackTrace();
			fail(e.getMessage());
		} catch (UnsoundGraphException e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

	@Test
	public void aliasDataDep_Library_call() {
		final String src = "../MoJo-TestCode/src";
		final String bin = "../MoJo-TestCode/bin";
		final String method = "Library.call(LLibrary$A;LLibrary$A;LLibrary$A;I)I";

		try {
			final MoJoTest mt = MoJoTest.create(src, bin, out);
			final SDG sdg = mt.analyzeMethod(method);
			assertTrue(sdg.vertexSet().size() > 0);
		} catch (ClassHierarchyException e) {
			e.printStackTrace();
			fail(e.getMessage());
		} catch (IOException e) {
			e.printStackTrace();
			fail(e.getMessage());
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
			fail(e.getMessage());
		} catch (CancelException e) {
			e.printStackTrace();
			fail(e.getMessage());
		} catch (UnsoundGraphException e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

	@Test
	public void aliasDataDep_Test_invokeSingleParamAlias() {
		final String src = "../MoJo-TestCode/src";
		final String bin = "../MoJo-TestCode/bin";
		final String method = "Test.invokeSingleParamAlias(LTest$A3;)I";

		try {
			final MoJoTest mt = MoJoTest.create(src, bin, out);
			final SDG sdg = mt.analyzeMethod(method);
			assertTrue(sdg.vertexSet().size() > 0);
		} catch (ClassHierarchyException e) {
			e.printStackTrace();
			fail(e.getMessage());
		} catch (IOException e) {
			e.printStackTrace();
			fail(e.getMessage());
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
			fail(e.getMessage());
		} catch (CancelException e) {
			e.printStackTrace();
			fail(e.getMessage());
		} catch (UnsoundGraphException e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

	@Test
	public void aliasDataDep_Test_invokeStringAndPrintln() {
		final String src = "../MoJo-TestCode/src";
		final String bin = "../MoJo-TestCode/bin";
		final String method = "Test.invokeStringAndPrintln(Ljava/lang/String;Ljava/lang/String;LTest$A;)I";

		try {
			final MoJoTest mt = MoJoTest.create(src, bin, out);
			final SDG sdg = mt.analyzeMethod(method);
			assertTrue(sdg.vertexSet().size() > 0);
		} catch (ClassHierarchyException e) {
			e.printStackTrace();
			fail(e.getMessage());
		} catch (IOException e) {
			e.printStackTrace();
			fail(e.getMessage());
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
			fail(e.getMessage());
		} catch (CancelException e) {
			e.printStackTrace();
			fail(e.getMessage());
		} catch (UnsoundGraphException e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

	@Test
	public void aliasDataDep_Test_aliasTest() {
		final String src = "../MoJo-TestCode/src";
		final String bin = "../MoJo-TestCode/bin";
		final String method = "Test.aliasTest(LTest$A3;LTest$A3;)I";

		try {
			final MoJoTest mt = MoJoTest.create(src, bin, out);
			final SDG sdg = mt.analyzeMethod(method);
			assertTrue(sdg.vertexSet().size() > 0);
		} catch (ClassHierarchyException e) {
			e.printStackTrace();
			fail(e.getMessage());
		} catch (IOException e) {
			e.printStackTrace();
			fail(e.getMessage());
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
			fail(e.getMessage());
		} catch (CancelException e) {
			e.printStackTrace();
			fail(e.getMessage());
		} catch (UnsoundGraphException e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

	@Test
	public void aliasDataDep_Test_indirectAliasTest() {
		final String src = "../MoJo-TestCode/src";
		final String bin = "../MoJo-TestCode/bin";
		final String method = "Test.indirectAliasTest(LTest$A3;LTest$A3;)I";

		try {
			final MoJoTest mt = MoJoTest.create(src, bin, out);
			final SDG sdg = mt.analyzeMethod(method);
			assertTrue(sdg.vertexSet().size() > 0);
		} catch (ClassHierarchyException e) {
			e.printStackTrace();
			fail(e.getMessage());
		} catch (IOException e) {
			e.printStackTrace();
			fail(e.getMessage());
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
			fail(e.getMessage());
		} catch (CancelException e) {
			e.printStackTrace();
			fail(e.getMessage());
		} catch (UnsoundGraphException e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

}
