/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */

package edu.kit.joana.ifc.sdg.qifc.nildumu;

import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import edu.kit.joana.ifc.sdg.qifc.nildumu.Runner.TestCase;
import edu.kit.joana.ifc.sdg.qifc.nildumu.prog.SimpleTestBed;
import edu.kit.joana.ifc.sdg.qifc.nildumu.prog.SimpleTestBed2;

/**
 * Basic tests using the Runner class
 */
public class SimpleRunnerTests {

	public static Stream<Arguments> simpleTestsSupplier(){
		return Runner.testCases(SimpleTestBed.class);
	}
	
	@ParameterizedTest
	@MethodSource("simpleTestsSupplier")
	void test(TestCase testCase, String handlerProp) {
		Runner.test(testCase, handlerProp, false);
	}
	
	public static Stream<Arguments> simpleTestsSupplier2(){
		return Runner.testCases(SimpleTestBed2.class);
	}
	
	@ParameterizedTest
	@MethodSource("simpleTestsSupplier2")
	void test2(TestCase testCase, String handlerProp) {
		Runner.test(testCase, handlerProp, true);
	}
	
	public static void main(String[] args) {
		try {
			Thread.sleep(10000);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		Runner.testCases(SimpleTestBed.class).forEach(a -> Runner.test((TestCase)a.get()[0], (String)a.get()[1], false));
	}
}
