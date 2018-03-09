/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.api.test;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import org.junit.Test;

import edu.kit.joana.ifc.sdg.lattice.IEditableLattice;
import edu.kit.joana.ifc.sdg.lattice.IStaticLattice;
import edu.kit.joana.ifc.sdg.lattice.LatticeUtil;
import edu.kit.joana.ifc.sdg.lattice.WrongLatticeDefinitionException;

/**
 * Test class to test lattice construction and operations.
 * 
 * @author Simon Bischof <simon.bischof@kit.edu>
 */
public class LatticeTests {

	private class TestExpectation {
		String e1, e2, resultGLB, resultLUB;

		public TestExpectation(String e1, String e2, String resultGLB, String resultLUB) {
			super();
			this.e1 = e1;
			this.e2 = e2;
			this.resultGLB = resultGLB;
			this.resultLUB = resultLUB;
		}

		/* (non-Javadoc)
		 * @see java.lang.Object#toString()
		 */
		@Override
		public String toString() {
			StringBuilder builder = new StringBuilder();
			builder.append("{");
			builder.append(e1);
			builder.append(", ");
			builder.append(e2);
			builder.append(", GLB=");
			builder.append(resultGLB);
			builder.append(", LUB=");
			builder.append(resultLUB);
			builder.append("}");
			return builder.toString();
		}
	}

	void test(IEditableLattice<String> latticeE, IStaticLattice<String> latticeB,
	             List<TestExpectation> tests) {
		for (TestExpectation test : tests) {
			assertEquals(test.resultGLB, latticeE.greatestLowerBound(test.e1, test.e2));
			assertEquals(test.resultGLB, latticeE.greatestLowerBound(test.e2, test.e1));
			assertEquals(test.resultGLB, latticeB.greatestLowerBound(test.e1, test.e2));
			assertEquals(test.resultGLB, latticeB.greatestLowerBound(test.e2, test.e1));

			assertEquals(test.resultLUB, latticeE.leastUpperBound(test.e1, test.e2));
			assertEquals(test.resultLUB, latticeE.leastUpperBound(test.e2, test.e1));
			assertEquals(test.resultLUB, latticeB.leastUpperBound(test.e1, test.e2));
			assertEquals(test.resultLUB, latticeB.leastUpperBound(test.e2, test.e1));
		}
	}

	/*
	 * lattice consisting of just a chain
	 */
	private String linearLattice(int n) {
		StringBuilder sb = new StringBuilder();
		for (int i = 1; i < n; i++) {
			sb.append(i-1).append(" <= ").append(i).append("\n");
		}
		return sb.toString();
	}

	private void testLinearLattice(int n) throws WrongLatticeDefinitionException {
		IEditableLattice<String> latticeE = LatticeUtil.loadLattice(linearLattice(n));
		IStaticLattice<String> latticeB = LatticeUtil.compileBitsetLattice(latticeE);
		List<TestExpectation> tests = new LinkedList<>();

		for (int i = 0; i < n; i++) {
			for (int j = i; j < n; j++) {
				tests.add(new TestExpectation(Integer.toString(i), Integer.toString(j),
				                              Integer.toString(i), Integer.toString(j)));
			}
		}
		test(latticeE, latticeB, tests);
	}

	private String chainElem(int chain, int height) {
		return new StringBuilder().append(chain).append("_").append(height).toString();
	}

	/*
	 * lattice consisting of multiple chains with a common bottom and top element added
	 */
	private String chainsLattice(int chains, int height) {
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < chains; i++) {
			sb.append("L <= ")
			  .append(chainElem(i, 0))
			  .append("\n");
			sb.append(chainElem(i, height-1))
			  .append(" <= H\n");
			for (int j = 1; j < height; j++) {
				sb.append(chainElem(i, j - 1))
				  .append(" <= ")
				  .append(chainElem(i, j))
				  .append("\n");
			}
		}
		return sb.toString();
	}

	private void testChainsLattice(int chains, int height) throws WrongLatticeDefinitionException {
		IEditableLattice<String> latticeE = LatticeUtil.loadLattice(chainsLattice(chains, height));
		IStaticLattice<String> latticeB = LatticeUtil.compileBitsetLattice(latticeE);
		List<TestExpectation> tests = new LinkedList<>();

		tests.add(new TestExpectation("L", "L", "L", "L"));
		tests.add(new TestExpectation("L", "H", "L", "H"));
		tests.add(new TestExpectation("H", "H", "H", "H"));
		for (int i1 = 0; i1 < chains; i1++) {
			for (int j1 = 0; j1 < height; j1++) {
				tests.add(new TestExpectation("L", chainElem(i1, j1), "L", chainElem(i1, j1)));
				tests.add(new TestExpectation(chainElem(i1, j1), "H", chainElem(i1, j1), "H"));
				for (int j2 = j1; j2 < height; j2++) {
					tests.add(new TestExpectation(chainElem(i1, j1), chainElem(i1, j2),
							chainElem(i1, j1), chainElem(i1, j2)));
				}
				for (int i2 = i1+1; i2 < chains; i2++) {
					for (int j2 = 0; j2 < height; j2++) {
						tests.add(new TestExpectation(chainElem(i1, j1), chainElem(i2, j2), "L", "H"));
					}
				}
			}
		}
		test(latticeE, latticeB, tests);
	}

	/*
	 * power set lattice
	 */
	private String powerSetLattice(int dim) {
		StringBuilder sb = new StringBuilder();
		int elements = 1 << dim;
		for (int i = 0; i < elements; i++) {
			for (int bit = 0; bit < dim; bit++) {
				int mask = 1 << bit;
				if ((i & mask) == 0) {
					sb.append(i).append(" <= ").append(i | mask).append("\n");
				}
			}
		}
		return sb.toString();
	}

	private void testPowerSetLattice(int dim) throws WrongLatticeDefinitionException {
		IEditableLattice<String> latticeE = LatticeUtil.loadLattice(powerSetLattice(dim));
		IStaticLattice<String> latticeB = LatticeUtil.compileBitsetLattice(latticeE);
		List<TestExpectation> tests = new LinkedList<>();

		int elements = 1 << dim;
		for (int i = 0; i < elements; i++) {
			for (int j = i; j < elements; j++) {
				tests.add(new TestExpectation(Integer.toString(i), Integer.toString(j),
				                              Integer.toString(i & j), Integer.toString(i | j)));
			}
		}
		test(latticeE, latticeB, tests);
	}

	@Test
	public void testSingleElementLattice() throws WrongLatticeDefinitionException {
		IEditableLattice<String> latticeE = LatticeUtil.loadLattice(
				"LH"
			);
		IStaticLattice<String> latticeB = LatticeUtil.compileBitsetLattice(latticeE);
		List<TestExpectation> tests = Arrays.asList(new TestExpectation[]{
			new TestExpectation("LH", "LH", "LH", "LH")
		});
		test(latticeE, latticeB, tests);
	}

	@Test
	public void testDiamondWithTop() throws WrongLatticeDefinitionException {
		IEditableLattice<String> latticeE = LatticeUtil.loadLattice(
				"L <= A\n" +
				"L <= B\n" +
				"A <= C\n" +
				"B <= C\n" +
				"C <= H\n"
			);
		IStaticLattice<String> latticeB = LatticeUtil.compileBitsetLattice(latticeE);
		List<TestExpectation> tests = Arrays.asList(new TestExpectation[]{
			new TestExpectation("L", "A", "L", "A"),
			new TestExpectation("L", "B", "L", "B"),
			new TestExpectation("L", "C", "L", "C"),
			new TestExpectation("L", "H", "L", "H"),
			new TestExpectation("A", "B", "L", "C"),
			new TestExpectation("A", "C", "A", "C"),
			new TestExpectation("A", "H", "A", "H"),
			new TestExpectation("B", "C", "B", "C"),
			new TestExpectation("B", "H", "B", "H"),
			new TestExpectation("C", "H", "C", "H"),
		});
		test(latticeE, latticeB, tests);
	}

	@Test
	public void testDiamondWithBottom() throws WrongLatticeDefinitionException {
		IEditableLattice<String> latticeE = LatticeUtil.loadLattice(
				"L <= A\n" +
				"A <= B\n" +
				"A <= C\n" +
				"B <= H\n" +
				"C <= H\n"
			);
		IStaticLattice<String> latticeB = LatticeUtil.compileBitsetLattice(latticeE);
		List<TestExpectation> tests = Arrays.asList(new TestExpectation[]{
			new TestExpectation("L", "A", "L", "A"),
			new TestExpectation("L", "B", "L", "B"),
			new TestExpectation("L", "C", "L", "C"),
			new TestExpectation("L", "H", "L", "H"),
			new TestExpectation("A", "B", "A", "B"),
			new TestExpectation("A", "C", "A", "C"),
			new TestExpectation("A", "H", "A", "H"),
			new TestExpectation("B", "C", "A", "H"),
			new TestExpectation("B", "H", "B", "H"),
			new TestExpectation("C", "H", "C", "H"),
		});
		test(latticeE, latticeB, tests);
	}

	@Test
	public void testLinearLattices() throws WrongLatticeDefinitionException {
		testLinearLattice(2);
		testLinearLattice(3);
		testLinearLattice(4);
		testLinearLattice(7);
		testLinearLattice(8);
		testLinearLattice(9);
		testLinearLattice(25);
		testLinearLattice(50);
	}

	@Test
	public void testChainLattices() throws WrongLatticeDefinitionException {
		testChainsLattice(1,1);
		testChainsLattice(2,1);
		testChainsLattice(1,1);
		testChainsLattice(2,2);
		testChainsLattice(4,3);
		testChainsLattice(3,4);
		testChainsLattice(10,10);
	}

	@Test
	public void testPowerSetLattices() throws WrongLatticeDefinitionException {
		testPowerSetLattice(1);
		testPowerSetLattice(2);
		testPowerSetLattice(3);
		testPowerSetLattice(4);
		testPowerSetLattice(5);
		testPowerSetLattice(6);
	}
}
