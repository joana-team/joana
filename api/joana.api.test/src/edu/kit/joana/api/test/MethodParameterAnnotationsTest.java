/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.api.test;

import static org.junit.Assert.assertFalse;

import java.io.IOException;
import java.util.Collection;

import org.junit.Test;

import com.ibm.wala.ipa.cha.ClassHierarchyException;
import com.ibm.wala.util.CancelException;
import com.ibm.wala.util.graph.GraphIntegrity.UnsoundGraphException;

import edu.kit.joana.api.IFCAnalysis;
import edu.kit.joana.api.test.util.ApiTestException;
import edu.kit.joana.api.test.util.BuildSDG;
import edu.kit.joana.api.test.util.DumpTestSDG;
import edu.kit.joana.ifc.sdg.core.SecurityNode;
import edu.kit.joana.ifc.sdg.core.violations.IViolation;

/**
 * @author Martin Hecker <martin.hecker@kit.edu>
 */
public class MethodParameterAnnotationsTest {


	public void test(Class<?> cls) throws ClassHierarchyException, ApiTestException, IOException, UnsoundGraphException,
	CancelException {
		IFCAnalysis ana = BuildSDG.buldAndUseJavaAnnotations(
			cls,
			BuildSDG.top_sequential,
			false);
		DumpTestSDG.dumpSDG(ana.getProgram().getSDG(), cls.getCanonicalName() + ".pdg");

		Collection<? extends IViolation<SecurityNode>> illegal = ana.doIFC();
		assertFalse(illegal.isEmpty());
	}

	
	@Test
	public void testSource() throws ClassHierarchyException, ApiTestException, IOException, UnsoundGraphException,
	CancelException {
		test(joana.api.testdata.javannotations.MethodParameterAnnotations.class);
	}

	@Test
	public void testSink() throws ClassHierarchyException, ApiTestException, IOException, UnsoundGraphException,
	CancelException {
		test(joana.api.testdata.javannotations.MethodParameterAnnotationsSink.class);
	}
	
	@Test
	public void testSink2() throws ClassHierarchyException, ApiTestException, IOException, UnsoundGraphException,
	CancelException {
		test(joana.api.testdata.javannotations.MethodParameterAnnotationsSink2.class);
	}

	
	public static void main(String[] args)
		throws ClassHierarchyException,
		       ApiTestException,
		       IOException,
		       UnsoundGraphException,
		       CancelException {
		MethodParameterAnnotationsTest test = new MethodParameterAnnotationsTest();
		test.testSource();
		test.testSink();
	}

}
