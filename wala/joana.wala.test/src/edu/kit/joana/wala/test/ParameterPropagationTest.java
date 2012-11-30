/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.wala.test;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
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
import edu.kit.joana.wala.core.SDGBuilder;
import edu.kit.joana.wala.core.Main.Config;
import edu.kit.joana.wala.core.SDGBuilder.ExceptionAnalysis;

public class ParameterPropagationTest {


	public static final String out = "./out/";

	@Test
	public void fieldPropagationSimple_Test_entry() {
		final String src = "../MoJo-TestCode/src";
		final String bin = "../MoJo-TestCode/bin";
		final String method = "FieldPropagation.entry()V";

		try {
			final MoJoTest mt = MoJoTest.create(src, bin, out);
			final Config cfg = mt.createDefaultConfig();
			cfg.name = "simple-prop";
			cfg.pts = SDGBuilder.PointsToPrecision.CONTEXT_SENSITIVE;
			cfg.fieldPropagation = SDGBuilder.FieldPropagation.OBJ_GRAPH_SIMPLE_PROPAGATION;
			cfg.accessPath = false;
			cfg.exceptions = ExceptionAnalysis.INTERPROC;
			final SDG sdg = mt.analyzeMethod(method, cfg);
			assertTrue(sdg.vertexSet().size() > 0);

			SDGNode entryMain = null;
			for (final SDGNode n : sdg.vertexSet()) {
				if (n.getKind() == SDGNode.Kind.ENTRY && n.getLabel().contains("entry")) {
					entryMain = n;
					break;
				}
			}

			assertNotNull(entryMain);

			SDGNode callFoo = null;
			for (final SDGNode n : sdg.getNodesOfProcedure(entryMain)) {
				if (n.kind == SDGNode.Kind.CALL && n.getLabel().contains("foo")) {
					callFoo = n;
					break;
				}
			}

			assertNotNull(callFoo);

			SDGNode actOutI = null;
			for (final SDGEdge e : sdg.outgoingEdgesOf(callFoo)) {
				if (e.getKind() == SDGEdge.Kind.CONTROL_DEP_EXPR && e.getTarget().getKind() == SDGNode.Kind.ACTUAL_OUT
						&& e.getTarget().getBytecodeName().equals("LFieldPropagation$B.i")) {
					actOutI = e.getTarget();
				}
			}

			assertNotNull("expected actual out parameter i at call to foo, because of imprecision from simple propagation", actOutI);
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
	public void fieldPropagationFixpoint_Test_entry() {
		final String src = "../MoJo-TestCode/src";
		final String bin = "../MoJo-TestCode/bin";
		final String method = "FieldPropagation.entry()V";

		try {
			final MoJoTest mt = MoJoTest.create(src, bin, out);
			final Config cfg = mt.createDefaultConfig();
			cfg.name = "fixpoint-prop";
			cfg.pts = SDGBuilder.PointsToPrecision.CONTEXT_SENSITIVE;
			cfg.fieldPropagation = SDGBuilder.FieldPropagation.OBJ_GRAPH_FIXPOINT_PROPAGATION;
			cfg.exceptions = ExceptionAnalysis.INTERPROC;
			cfg.accessPath = false;
			final SDG sdg = mt.analyzeMethod(method, cfg);
			assertTrue(sdg.vertexSet().size() > 0);

			SDGNode entryMain = null;
			for (final SDGNode n : sdg.vertexSet()) {
				if (n.getKind() == SDGNode.Kind.ENTRY && n.getLabel().contains("entry")) {
					entryMain = n;
					break;
				}
			}

			assertNotNull(entryMain);

			SDGNode callFoo = null;
			for (final SDGNode n : sdg.getNodesOfProcedure(entryMain)) {
				if (n.kind == SDGNode.Kind.CALL && n.getLabel().contains("foo")) {
					callFoo = n;
					break;
				}
			}

			assertNotNull(callFoo);

			SDGNode actOutI = null;
			for (final SDGEdge e : sdg.outgoingEdgesOf(callFoo)) {
				if (e.getKind() == SDGEdge.Kind.CONTROL_DEP_EXPR && e.getTarget().getKind() == SDGNode.Kind.ACTUAL_OUT
						&& e.getTarget().getBytecodeName().equals("LFieldPropagation$B.i")) {
					actOutI = e.getTarget();
				}
			}

			assertNull("expected no actual out parameter i at call to foo, because of precision from fixpoint propagation", actOutI);
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
	public void fieldPropagationAP_Test_entry() {
		final String src = "../MoJo-TestCode/src";
		final String bin = "../MoJo-TestCode/bin";
		final String method = "FieldPropagation.entry()V";

		try {
			final MoJoTest mt = MoJoTest.create(src, bin, out);
			final Config cfg = mt.createDefaultConfig();
			cfg.name = "objtree-ap-prop";
			cfg.pts = SDGBuilder.PointsToPrecision.CONTEXT_SENSITIVE;
			cfg.fieldPropagation = SDGBuilder.FieldPropagation.OBJ_TREE;
			cfg.accessPath = true;
			cfg.exceptions = ExceptionAnalysis.INTERPROC;
			final SDG sdg = mt.analyzeMethod(method, cfg);
			assertTrue(sdg.vertexSet().size() > 0);

			SDGNode entryMain = null;
			for (final SDGNode n : sdg.vertexSet()) {
				if (n.getKind() == SDGNode.Kind.ENTRY && n.getLabel().contains("entry")) {
					entryMain = n;
					break;
				}
			}

			assertNotNull(entryMain);

			SDGNode callFoo = null;
			for (final SDGNode n : sdg.getNodesOfProcedure(entryMain)) {
				if (n.kind == SDGNode.Kind.CALL && n.getLabel().contains("foo")) {
					callFoo = n;
					break;
				}
			}

			assertNotNull(callFoo);

			SDGNode actOutI = null;
			for (final SDGEdge e : sdg.outgoingEdgesOf(callFoo)) {
				if (e.getKind() == SDGEdge.Kind.CONTROL_DEP_EXPR && e.getTarget().getKind() == SDGNode.Kind.ACTUAL_OUT
						&& e.getTarget().getBytecodeName().equals("LFieldPropagation$B.i")) {
					actOutI = e.getTarget();
				}
			}

			assertNull("expected no actual out parameter i at call to foo, because of precision from fixpoint propagation", actOutI);
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
