/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.api.test;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

/**
 * TODO: @author Add your name here.
 */
@RunWith(Suite.class)
@SuiteClasses({ ConcurrentTests.class, FullIFCSensitivityTest.class, FullIFCSequentialTest.class, IFCJoinTest.class,
		JoinAnalysisIFCMantelTest.class, ProbNITest.class, SDGConstructionCCTest.class, TimeSensSlicingTest.class,
		ToyTests.class })
public class FastTests {

}
