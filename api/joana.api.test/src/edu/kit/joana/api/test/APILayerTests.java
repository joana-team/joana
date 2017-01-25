/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.api.test;

import java.io.IOException;

import org.junit.Assert;
import org.junit.Test;

import com.ibm.wala.ipa.cha.ClassHierarchyException;
import com.ibm.wala.util.CancelException;
import com.ibm.wala.util.graph.GraphIntegrity.UnsoundGraphException;

import edu.kit.joana.api.IFCAnalysis;
import edu.kit.joana.api.IFCType;
import edu.kit.joana.api.lattice.BuiltinLattices;
import edu.kit.joana.api.sdg.SDGConfig;
import edu.kit.joana.api.sdg.SDGProgram;
import edu.kit.joana.api.sdg.SDGProgramPart;
import edu.kit.joana.api.test.util.JoanaPath;
import edu.kit.joana.ifc.sdg.util.JavaMethodSignature;
import edu.kit.joana.ifc.sdg.util.JavaType;
import edu.kit.joana.util.Stubs;
import edu.kit.joana.wala.core.SDGBuilder.FieldPropagation;
import joana.api.testdata.seq.AttributeAsParameter;

/**
 * @author Martin Mohr
 */
public class APILayerTests {
	
	/**
	 * If an attribute is annotated as a sink, then also passing it as a parameter should be tracked.
	 * @throws ClassHierarchyException
	 * @throws IOException
	 * @throws UnsoundGraphException
	 * @throws CancelException
	 */
	@Test
	public void testAttributeAsParameter() throws ClassHierarchyException, IOException, UnsoundGraphException, CancelException {
		SDGConfig config = new SDGConfig(JoanaPath.JOANA_API_TEST_DATA_CLASSPATH, JavaMethodSignature.mainMethodOfClass(AttributeAsParameter.class.getCanonicalName()).toBCString(), Stubs.JRE_14);
		config.setFieldPropagation(FieldPropagation.OBJ_GRAPH);
		SDGProgram program = SDGProgram.createSDGProgram(config);
		IFCAnalysis ana = new IFCAnalysis(program);
		for (SDGProgramPart a : program.getAttribute(JavaType.parseSingleTypeFromString("joana.api.testdata.seq.AttributeAsParameter"), "high")) {
			ana.addSourceAnnotation(a, BuiltinLattices.STD_SECLEVEL_HIGH);
		}
		for (SDGProgramPart a : program.getAttribute(JavaType.parseSingleTypeFromString("joana.api.testdata.seq.AttributeAsParameter"), "low")) {
			ana.addSinkAnnotation(a, BuiltinLattices.STD_SECLEVEL_LOW);
		}
		
		Assert.assertEquals(1, ana.getSources().size());
		Assert.assertEquals(1, ana.getSinks().size());
		Assert.assertEquals(1, ana.doIFCAndGroupByPPPart(IFCType.CLASSICAL_NI).size());
	}
}
