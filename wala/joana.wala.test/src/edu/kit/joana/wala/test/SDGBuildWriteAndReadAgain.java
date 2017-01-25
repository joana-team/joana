/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.wala.test;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;

import org.junit.Test;

import com.ibm.wala.ipa.cha.ClassHierarchyException;
import com.ibm.wala.util.CancelException;
import com.ibm.wala.util.graph.GraphIntegrity.UnsoundGraphException;

import edu.kit.joana.ifc.sdg.graph.SDG;
import edu.kit.joana.ifc.sdg.graph.SDGEdge;
import edu.kit.joana.ifc.sdg.graph.SDGNode;

/**
 * @author Juergen Graf <juergen.graf@gmail.com>
 *
 */
public class SDGBuildWriteAndReadAgain {

	public static final String out = "./out/";

	private void checkEqual(final SDG sdg1, final SDG sdg2) {
		assertEquals(sdg1.vertexSet().size(), sdg2.vertexSet().size());
		assertEquals(sdg1.edgeSet().size(), sdg2.edgeSet().size());

		for (final SDGNode n1 : sdg1.vertexSet()) {
			final SDGNode n2 = sdg2.getNode(n1.getId());
			assertNotNull("Error on node id " + n1.getId(), n2);
			assertEquals("Error on node id " + n1.getId(), n1.kind, n2.kind);
			assertEquals("Error on node id " + n1.getId(), n1.operation, n2.operation);
			assertEquals("Error on node id " + n1.getId(), n1.getBytecodeName(), n2.getBytecodeName());
			assertEquals("Error on node id " + n1.getId(), n1.getBytecodeIndex(), n2.getBytecodeIndex());
			assertEquals("Error on node id " + n1.getId(), n1.getProc(), n2.getProc());
			assertEquals("Error on node id " + n1.getId(), n1.getSource(), n2.getSource());
			assertEquals("Error on node id " + n1.getId(), n1.getSc(), n2.getSc());
			assertEquals("Error on node id " + n1.getId(), n1.getSr(), n2.getSr());
			assertEquals("Error on node id " + n1.getId(), n1.getEc(), n2.getEc());
			assertEquals("Error on node id " + n1.getId(), n1.getLabel(), n2.getLabel());
			assertArrayEquals("Error on node id " + n1.getId(), n1.getThreadNumbers(), n2.getThreadNumbers());
			assertEquals("Error on node id " + n1.getId(), n1.getType(), n2.getType());
			assertEquals("Error on node id " + n1.getId(), n1.isParameter(), n2.isParameter());
			assertEquals("Error on node id " + n1.getId(), n1.mayBeNonTerminating(), n2.mayBeNonTerminating());

			assertEquals(sdg1.outDegreeOf(n1), sdg2.outDegreeOf(n2));
			for (final SDGEdge e1 : sdg1.outgoingEdgesOf(n1)) {
				assertTrue("Error on node id " + n1.getId() + " at " + e1.getKind() + " edge to " + e1.getTarget().getId(), sdg2.containsEdge(e1));
			}
		}
	}

	@Test
	public void buildWriteRead_Test_foo1() {
		final String src = "../MoJo-TestCode/src";
		final String bin = "../MoJo-TestCode/bin";
		final String method = "Test.foo1(LTest$A;LTest$A;)LTest$A2;";

		try {
			final MoJoTest mt = MoJoTest.create(src, bin, out);
			final SDG sdg = mt.analyzeMethod(method);
			final String fileName = mt.getSDGFileName(sdg.getFileName());
			final SDG sdg2 = SDG.readFrom(fileName);

			checkEqual(sdg, sdg2);
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
	public void buildWriteRead_Test_impossibleAlias() {
		final String src = "../MoJo-TestCode/src";
		final String bin = "../MoJo-TestCode/bin";
		final String method = "Test.impossibleAlias(LTest$A2;LTest$A2;LTest$A3;LTest$A3;)I";

		try {
			final MoJoTest mt = MoJoTest.create(src, bin, out);
			final SDG sdg = mt.analyzeMethod(method);
			final String fileName = mt.getSDGFileName(sdg.getFileName());
			final SDG sdg2 = SDG.readFrom(fileName);

			checkEqual(sdg, sdg2);
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
	public void buildWriteRead_Test_foo2() {
		final String src = "../MoJo-TestCode/src";
		final String bin = "../MoJo-TestCode/bin";
		final String method = "Test.foo2(LTest$A;LTest$A;)I";

		try {
			final MoJoTest mt = MoJoTest.create(src, bin, out);
			final SDG sdg = mt.analyzeMethod(method);
			final String fileName = mt.getSDGFileName(sdg.getFileName());
			final SDG sdg2 = SDG.readFrom(fileName);

			checkEqual(sdg, sdg2);
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
	public void buildWriteRead_Test_foo3() {
		final String src = "../MoJo-TestCode/src";
		final String bin = "../MoJo-TestCode/bin";
		final String method = "Test.foo3(LTest$A;LTest$A;)I";

		try {
			final MoJoTest mt = MoJoTest.create(src, bin, out);
			final SDG sdg = mt.analyzeMethod(method);
			final String fileName = mt.getSDGFileName(sdg.getFileName());
			final SDG sdg2 = SDG.readFrom(fileName);

			checkEqual(sdg, sdg2);
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
	public void buildWriteRead_Test_foo4() {
		final String src = "../MoJo-TestCode/src";
		final String bin = "../MoJo-TestCode/bin";
		final String method = "Test.foo4(LTest$A;LTest$A;LTest$A;LTest$A;)I";

		try {
			final MoJoTest mt = MoJoTest.create(src, bin, out);
			final SDG sdg = mt.analyzeMethod(method);
			final String fileName = mt.getSDGFileName(sdg.getFileName());
			final SDG sdg2 = SDG.readFrom(fileName);

			checkEqual(sdg, sdg2);
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
	public void buildWriteRead_Test_foo5() {
		final String src = "../MoJo-TestCode/src";
		final String bin = "../MoJo-TestCode/bin";
		final String method = "Test.foo5(LTest$A;LTest$A;LTest$A;LTest$A;)I";

		try {
			final MoJoTest mt = MoJoTest.create(src, bin, out);
			final SDG sdg = mt.analyzeMethod(method);
			final String fileName = mt.getSDGFileName(sdg.getFileName());
			final SDG sdg2 = SDG.readFrom(fileName);

			checkEqual(sdg, sdg2);
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
	public void buildWriteRead_Test_foo6() {
		final String src = "../MoJo-TestCode/src";
		final String bin = "../MoJo-TestCode/bin";
		final String method = "Test.foo6(LTest$A;LTest$A;)I";

		try {
			final MoJoTest mt = MoJoTest.create(src, bin, out);
			final SDG sdg = mt.analyzeMethod(method);
			final String fileName = mt.getSDGFileName(sdg.getFileName());
			final SDG sdg2 = SDG.readFrom(fileName);

			checkEqual(sdg, sdg2);
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
	public void buildWriteRead_Library_call() {
		final String src = "../MoJo-TestCode/src";
		final String bin = "../MoJo-TestCode/bin";
		final String method = "Library.call(LLibrary$A;LLibrary$A;LLibrary$A;I)I";

		try {
			final MoJoTest mt = MoJoTest.create(src, bin, out);
			final SDG sdg = mt.analyzeMethod(method);
			final String fileName = mt.getSDGFileName(sdg.getFileName());
			final SDG sdg2 = SDG.readFrom(fileName);

			checkEqual(sdg, sdg2);
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
