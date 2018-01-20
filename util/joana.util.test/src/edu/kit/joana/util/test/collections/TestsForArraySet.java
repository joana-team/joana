/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 */
package edu.kit.joana.util.test.collections;


import com.google.common.collect.testing.MinimalSet;
import com.google.common.collect.testing.SetTestSuiteBuilder;
import com.google.common.collect.testing.TestStringSetGenerator;
import com.google.common.collect.testing.features.CollectionFeature;
import com.google.common.collect.testing.features.CollectionSize;

import junit.framework.Test;
import junit.framework.TestSuite;

import java.util.Set;

import edu.kit.joana.util.collections.ArraySet;

/**
 * Generates a test suite covering the {@link ArraySet} implementation.
 *
 * @author Martin Hecker
 */

public class TestsForArraySet {

	public static Test suite() {
		return new TestsForArraySet().allTests();
	}

	public Test allTests() {
		TestSuite suite = new TestSuite("edu.kit.joana.util.collections.ArraySet");
		suite.addTest(testsForArraySet());
		return suite;
	}

	public Test testsForArraySet() {
		return SetTestSuiteBuilder
				.using(new TestStringSetGenerator() {
					@Override public Set<String> create(String[] elements) {
						return new ArraySet<String>(MinimalSet.of(elements));
					}
				})
				.named("ArraySet")
				.withFeatures(
						CollectionFeature.SUPPORTS_ADD,
						CollectionFeature.SUPPORTS_REMOVE,
						CollectionSize.ANY)
				.createTestSuite();
	}
}
