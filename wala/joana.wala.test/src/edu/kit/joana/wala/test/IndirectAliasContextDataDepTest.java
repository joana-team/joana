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
public class IndirectAliasContextDataDepTest {

	public static final String out = "./out/";

	@Test
	public void aliasDataDep_Test_changeField() {
		final String src = "../MoJo-TestCode/src3";
		final String bin = "../MoJo-TestCode/bin";
		final String method = "IndirectSideEffects.changeField(LIndirectSideEffects$A1;LIndirectSideEffects$A3;I)V";

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
	public void aliasDataDep_Test_readField() {
		final String src = "../MoJo-TestCode/src3";
		final String bin = "../MoJo-TestCode/bin";
		final String method = "IndirectSideEffects.readField(LIndirectSideEffects$A1;LIndirectSideEffects$A3;)I";

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
