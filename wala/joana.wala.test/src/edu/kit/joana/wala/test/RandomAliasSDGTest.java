/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.wala.test;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.util.Random;

import org.junit.Test;

import com.ibm.wala.ipa.cha.ClassHierarchyException;
import com.ibm.wala.util.CancelException;
import com.ibm.wala.util.MonitorUtil.IProgressMonitor;

import edu.kit.joana.ifc.sdg.graph.SDG;
import edu.kit.joana.ifc.sdg.graph.SDGNode;
import edu.kit.joana.ifc.sdg.util.BytecodeLocation;
import edu.kit.joana.wala.dictionary.accesspath.AliasSDG;
import edu.kit.joana.wala.flowless.util.NullProgressMonitor;
import gnu.trove.iterator.TIntObjectIterator;
import gnu.trove.map.TIntObjectMap;
import gnu.trove.map.hash.TIntObjectHashMap;
import gnu.trove.set.TIntSet;
import gnu.trove.set.hash.TIntHashSet;

public class RandomAliasSDGTest {

	public static final String out = "./out/";

	private static TIntObjectMap<TIntSet> findMethodInputParams(final SDG sdg) {
		final TIntObjectMap<TIntSet> params = new TIntObjectHashMap<TIntSet>();

		for (final SDGNode n : sdg.vertexSet()) {
			if (n.kind == SDGNode.Kind.ENTRY) {
				final TIntSet formals = new TIntHashSet();
				for (final SDGNode fIn : sdg.getFormalInsOfProcedure(n)) {
					if (fIn.getBytecodeIndex() == BytecodeLocation.ROOT_PARAMETER ||
							fIn.getBytecodeIndex() == BytecodeLocation.STATIC_FIELD) {
						formals.add(fIn.getId());
					}
				}

				params.put(n.getId(), formals);
			}
		}

		return params;
	}

	private static void checkRandomAlias(final AliasSDG alias) {
		final IProgressMonitor progress = NullProgressMonitor.INSTANCE;
		final TIntObjectMap<TIntSet> params = findMethodInputParams(alias.getSDG());
		final Random rand = new Random();

		System.out.print("precompute summaries: ");
		int alwaysOn = 0;
		int maximalOn = 0;
		try {
			alwaysOn = alias.precomputeSummary(progress);
			maximalOn = alias.precomputeAllAliasSummary(progress);
			System.out.print("min(" + alwaysOn + ") max(" + (alwaysOn + maximalOn) +") ");
		} catch (CancelException e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
		System.out.println("done.");

		boolean change = false;
		System.out.print("no alias:");

		final TIntObjectIterator<TIntSet> it = params.iterator();
		assertTrue(it.hasNext());
		while (it.hasNext()) {
			it.advance();
			final int[] forms = it.value().toArray();
			if (forms.length == 0) {
				continue;
			}

			for (int permutations = rand.nextInt(forms.length); permutations > 0; permutations--) {
				final int a = forms[rand.nextInt(forms.length)];
				final int b = forms[rand.nextInt(forms.length)];
				alias.setNoAlias(a, b);
				change = true;
				System.out.print(" (" + a + "," + b + ")");
			}

		}

		System.out.print((change ? ": " : " "));

		try {
			if (alias.adjustMaxSDG(progress) > 0) {
				assertTrue(change);
				final int newSum = alias.recomputeSummary(progress);
				System.out.println("removed " + (maximalOn - newSum) + " summary edges. " + newSum + " remaining.");
			} else {
				System.out.println("no change.");
			}
		} catch (CancelException e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

	@Test
	public void adjustAlias_Test_foo1() {
		final String src = "../MoJo-TestCode/src";
		final String bin = "../MoJo-TestCode/bin";
		// final String method = "Test.foo1(LTest$A;LTest$A;)LTest$A2;";
		final String file = "Test.foo1(Test.A,Test.A)-mojotest";

		try {
			final MoJoTest mt = MoJoTest.create(src, bin, out);
			final String sdgFile = mt.getSDGFileName(file);
			final AliasSDG alias = AliasSDG.readFrom(sdgFile);
			System.out.println("Using SDG from " + sdgFile);
			assertNotNull(alias.getSDG());
			checkRandomAlias(alias);
		} catch (ClassHierarchyException e) {
			e.printStackTrace();
			fail(e.getMessage());
		} catch (IOException e) {
			e.printStackTrace();
			fail(e.getMessage());
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

	@Test
	public void adjustAlias_Test_indirectFoo1() {
		final String src = "../MoJo-TestCode/src";
		final String bin = "../MoJo-TestCode/bin";
		// final String method = "Test.indirectFoo1(LTest$A;LTest$A;)LTest$A2;";
		final String file = "Test.indirectFoo1(Test.A,Test.A)-mojotest";

		try {
			final MoJoTest mt = MoJoTest.create(src, bin, out);
			final String sdgFile = mt.getSDGFileName(file);
			final AliasSDG alias = AliasSDG.readFrom(sdgFile);
			System.out.println("Using SDG from " + sdgFile);
			assertNotNull(alias.getSDG());
			checkRandomAlias(alias);
		} catch (ClassHierarchyException e) {
			e.printStackTrace();
			fail(e.getMessage());
		} catch (IOException e) {
			e.printStackTrace();
			fail(e.getMessage());
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

	@Test
	public void adjustAlias_Test_indirectRevFoo1() {
		final String src = "../MoJo-TestCode/src";
		final String bin = "../MoJo-TestCode/bin";
		// final String method = "Test.indirectRevFoo1(LTest$A;LTest$A;)LTest$A2;";
		final String file = "Test.indirectRevFoo1(Test.A,Test.A)-mojotest";

		try {
			final MoJoTest mt = MoJoTest.create(src, bin, out);
			final String sdgFile = mt.getSDGFileName(file);
			final AliasSDG alias = AliasSDG.readFrom(sdgFile);
			System.out.println("Using SDG from " + sdgFile);
			assertNotNull(alias.getSDG());
			checkRandomAlias(alias);
		} catch (ClassHierarchyException e) {
			e.printStackTrace();
			fail(e.getMessage());
		} catch (IOException e) {
			e.printStackTrace();
			fail(e.getMessage());
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

	@Test
	public void adjustAlias_Test_indirectMultipleFoo1() {
		final String src = "../MoJo-TestCode/src";
		final String bin = "../MoJo-TestCode/bin";
		// final String method = "Test.indirectMultipleFoo1(LTest$A;LTest$A;)LTest$A2;";
		final String file = "Test.indirectMultipleFoo1(Test.A,Test.A)-mojotest";

		try {
			final MoJoTest mt = MoJoTest.create(src, bin, out);
			final String sdgFile = mt.getSDGFileName(file);
			final AliasSDG alias = AliasSDG.readFrom(sdgFile);
			System.out.println("Using SDG from " + sdgFile);
			assertNotNull(alias.getSDG());
			checkRandomAlias(alias);
		} catch (ClassHierarchyException e) {
			e.printStackTrace();
			fail(e.getMessage());
		} catch (IOException e) {
			e.printStackTrace();
			fail(e.getMessage());
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

	@Test
	public void adjustAlias_Test_indirectSameFoo1() {
		final String src = "../MoJo-TestCode/src";
		final String bin = "../MoJo-TestCode/bin";
		// final String method = "Test.indirectSameFoo1(LTest$A;LTest$A;)LTest$A2;";
		final String file = "Test.indirectSameFoo1(Test.A,Test.A)-mojotest";

		try {
			final MoJoTest mt = MoJoTest.create(src, bin, out);
			final String sdgFile = mt.getSDGFileName(file);
			final AliasSDG alias = AliasSDG.readFrom(sdgFile);
			System.out.println("Using SDG from " + sdgFile);
			assertNotNull(alias.getSDG());
			checkRandomAlias(alias);
		} catch (ClassHierarchyException e) {
			e.printStackTrace();
			fail(e.getMessage());
		} catch (IOException e) {
			e.printStackTrace();
			fail(e.getMessage());
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

	@Test
	public void adjustAlias_Test_foo2() {
		final String src = "../MoJo-TestCode/src";
		final String bin = "../MoJo-TestCode/bin";
		// final String method = "Test.foo2(LTest$A;LTest$A;)I";
		final String file = "Test.foo2(Test.A,Test.A)-mojotest";

		try {
			final MoJoTest mt = MoJoTest.create(src, bin, out);
			final String sdgFile = mt.getSDGFileName(file);
			final AliasSDG alias = AliasSDG.readFrom(sdgFile);
			System.out.println("Using SDG from " + sdgFile);
			assertNotNull(alias.getSDG());
			checkRandomAlias(alias);
		} catch (ClassHierarchyException e) {
			e.printStackTrace();
			fail(e.getMessage());
		} catch (IOException e) {
			e.printStackTrace();
			fail(e.getMessage());
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

	@Test
	public void adjustAlias_Test_foo3() {
		final String src = "../MoJo-TestCode/src";
		final String bin = "../MoJo-TestCode/bin";
		// final String method = "Test.foo3(LTest$A;LTest$A;)I";
		final String file = "Test.foo3(Test.A,Test.A)-mojotest";

		try {
			final MoJoTest mt = MoJoTest.create(src, bin, out);
			final String sdgFile = mt.getSDGFileName(file);
			final AliasSDG alias = AliasSDG.readFrom(sdgFile);
			System.out.println("Using SDG from " + sdgFile);
			assertNotNull(alias.getSDG());
			checkRandomAlias(alias);
		} catch (ClassHierarchyException e) {
			e.printStackTrace();
			fail(e.getMessage());
		} catch (IOException e) {
			e.printStackTrace();
			fail(e.getMessage());
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

	@Test
	public void adjustAlias_Test_foo4() {
		final String src = "../MoJo-TestCode/src";
		final String bin = "../MoJo-TestCode/bin";
		// final String method = "Test.foo4(LTest$A;LTest$A;LTest$A;LTest$A;)I";
		final String file = "Test.foo4(Test.A,Test.A,Test.A,Test.A)-mojotest";

		try {
			final MoJoTest mt = MoJoTest.create(src, bin, out);
			final String sdgFile = mt.getSDGFileName(file);
			final AliasSDG alias = AliasSDG.readFrom(sdgFile);
			System.out.println("Using SDG from " + sdgFile);
			assertNotNull(alias.getSDG());
			checkRandomAlias(alias);
		} catch (ClassHierarchyException e) {
			e.printStackTrace();
			fail(e.getMessage());
		} catch (IOException e) {
			e.printStackTrace();
			fail(e.getMessage());
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

	@Test
	public void adjustAlias_Test_foo5() {
		final String src = "../MoJo-TestCode/src";
		final String bin = "../MoJo-TestCode/bin";
		// final String method = "Test.foo5(LTest$A;LTest$A;LTest$A;LTest$A;)I";
		final String file = "Test.foo5(Test.A,Test.A,Test.A,Test.A)-mojotest";

		try {
			final MoJoTest mt = MoJoTest.create(src, bin, out);
			final String sdgFile = mt.getSDGFileName(file);
			final AliasSDG alias = AliasSDG.readFrom(sdgFile);
			System.out.println("Using SDG from " + sdgFile);
			assertNotNull(alias.getSDG());
			checkRandomAlias(alias);
		} catch (ClassHierarchyException e) {
			e.printStackTrace();
			fail(e.getMessage());
		} catch (IOException e) {
			e.printStackTrace();
			fail(e.getMessage());
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

	@Test
	public void adjustAlias_Test_foo6() {
		final String src = "../MoJo-TestCode/src";
		final String bin = "../MoJo-TestCode/bin";
		// final String method = "Test.foo6(LTest$A;LTest$A;)I";
		final String file = "Test.foo6(Test.A,Test.A)-mojotest";

		try {
			final MoJoTest mt = MoJoTest.create(src, bin, out);
			final String sdgFile = mt.getSDGFileName(file);
			final AliasSDG alias = AliasSDG.readFrom(sdgFile);
			System.out.println("Using SDG from " + sdgFile);
			assertNotNull(alias.getSDG());
			checkRandomAlias(alias);
		} catch (ClassHierarchyException e) {
			e.printStackTrace();
			fail(e.getMessage());
		} catch (IOException e) {
			e.printStackTrace();
			fail(e.getMessage());
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

	@Test
	public void adjustAlias_Test_foo7() {
		final String src = "../MoJo-TestCode/src";
		final String bin = "../MoJo-TestCode/bin";
		// final String method = "Test.foo7(LTest$A3;LTest$A3;)I";
		final String file = "Test.foo7(Test.A3,Test.A3)-mojotest";

		try {
			final MoJoTest mt = MoJoTest.create(src, bin, out);
			final String sdgFile = mt.getSDGFileName(file);
			final AliasSDG alias = AliasSDG.readFrom(sdgFile);
			System.out.println("Using SDG from " + sdgFile);
			assertNotNull(alias.getSDG());
			checkRandomAlias(alias);
		} catch (ClassHierarchyException e) {
			e.printStackTrace();
			fail(e.getMessage());
		} catch (IOException e) {
			e.printStackTrace();
			fail(e.getMessage());
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

	@Test
	public void adjustAlias_Test_foo8() {
		final String src = "../MoJo-TestCode/src";
		final String bin = "../MoJo-TestCode/bin";
		// final String method = "Test.foo8(LTest$A3;LTest$A3;)I";
		final String file = "Test.foo8(Test.A3,Test.A3)-mojotest";

		try {
			final MoJoTest mt = MoJoTest.create(src, bin, out);
			final String sdgFile = mt.getSDGFileName(file);
			final AliasSDG alias = AliasSDG.readFrom(sdgFile);
			System.out.println("Using SDG from " + sdgFile);
			assertNotNull(alias.getSDG());
			checkRandomAlias(alias);
		} catch (ClassHierarchyException e) {
			e.printStackTrace();
			fail(e.getMessage());
		} catch (IOException e) {
			e.printStackTrace();
			fail(e.getMessage());
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

	@Test
	public void adjustAlias_Test_foo9() {
		final String src = "../MoJo-TestCode/src";
		final String bin = "../MoJo-TestCode/bin";
		// final String method = "Test.foo9(LTest$A;LTest$A2;)I";
		final String file = "Test.foo9(Test.A,Test.A2)-mojotest";

		try {
			final MoJoTest mt = MoJoTest.create(src, bin, out);
			final String sdgFile = mt.getSDGFileName(file);
			final AliasSDG alias = AliasSDG.readFrom(sdgFile);
			System.out.println("Using SDG from " + sdgFile);
			assertNotNull(alias.getSDG());
			checkRandomAlias(alias);
		} catch (ClassHierarchyException e) {
			e.printStackTrace();
			fail(e.getMessage());
		} catch (IOException e) {
			e.printStackTrace();
			fail(e.getMessage());
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

	@Test
	public void adjustAlias_Test_foo10() {
		final String src = "../MoJo-TestCode/src";
		final String bin = "../MoJo-TestCode/bin";
		// final String method = "Test.foo10(LTest$A2;LTest$A2;)V";
		final String file = "Test.foo10(Test.A2,Test.A2)-mojotest";

		try {
			final MoJoTest mt = MoJoTest.create(src, bin, out);
			final String sdgFile = mt.getSDGFileName(file);
			final AliasSDG alias = AliasSDG.readFrom(sdgFile);
			System.out.println("Using SDG from " + sdgFile);
			assertNotNull(alias.getSDG());
			checkRandomAlias(alias);
		} catch (ClassHierarchyException e) {
			e.printStackTrace();
			fail(e.getMessage());
		} catch (IOException e) {
			e.printStackTrace();
			fail(e.getMessage());
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
			fail(e.getMessage());
		}

	}

	@Test
	public void adjustAlias_Test_foo11() {
		final String src = "../MoJo-TestCode/src";
		final String bin = "../MoJo-TestCode/bin";
		// final String method = "Test.foo11()LTest$A3;";
		final String file = "Test.foo11()-mojotest";

		try {
			final MoJoTest mt = MoJoTest.create(src, bin, out);
			final String sdgFile = mt.getSDGFileName(file);
			final AliasSDG alias = AliasSDG.readFrom(sdgFile);
			System.out.println("Using SDG from " + sdgFile);
			assertNotNull(alias.getSDG());
			checkRandomAlias(alias);
		} catch (ClassHierarchyException e) {
			e.printStackTrace();
			fail(e.getMessage());
		} catch (IOException e) {
			e.printStackTrace();
			fail(e.getMessage());
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
			fail(e.getMessage());
		}

	}

	@Test
	public void adjustAlias_Test_foo12() {
		final String src = "../MoJo-TestCode/src";
		final String bin = "../MoJo-TestCode/bin";
		// final String method = "Test.foo12(LTest$A3;LTest$A3;)V";
		final String file = "Test.foo12(Test.A3,Test.A3)-mojotest";

		try {
			final MoJoTest mt = MoJoTest.create(src, bin, out);
			final String sdgFile = mt.getSDGFileName(file);
			final AliasSDG alias = AliasSDG.readFrom(sdgFile);
			System.out.println("Using SDG from " + sdgFile);
			assertNotNull(alias.getSDG());
			checkRandomAlias(alias);
		} catch (ClassHierarchyException e) {
			e.printStackTrace();
			fail(e.getMessage());
		} catch (IOException e) {
			e.printStackTrace();
			fail(e.getMessage());
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
			fail(e.getMessage());
		}

	}

	@Test
	public void adjustAlias_Test_foo13() {
		final String src = "../MoJo-TestCode/src";
		final String bin = "../MoJo-TestCode/bin";
		// final String method = "Test.foo13(LTest$A3;LTest$A3;)V";
		final String file = "Test.foo13(Test.A3,Test.A3)-mojotest";

		try {
			final MoJoTest mt = MoJoTest.create(src, bin, out);
			final String sdgFile = mt.getSDGFileName(file);
			final AliasSDG alias = AliasSDG.readFrom(sdgFile);
			System.out.println("Using SDG from " + sdgFile);
			assertNotNull(alias.getSDG());
			checkRandomAlias(alias);
		} catch (ClassHierarchyException e) {
			e.printStackTrace();
			fail(e.getMessage());
		} catch (IOException e) {
			e.printStackTrace();
			fail(e.getMessage());
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
			fail(e.getMessage());
		}

	}

	@Test
	public void adjustAlias_Library_call() {
		final String src = "../MoJo-TestCode/src";
		final String bin = "../MoJo-TestCode/bin";
		// final String method = "Library.call(LLibrary$A;LLibrary$A;LLibrary$A;I)I";
		final String file = "Library.call(Library.A,Library.A,Library.A,int)-mojotest";

		try {
			final MoJoTest mt = MoJoTest.create(src, bin, out);
			final String sdgFile = mt.getSDGFileName(file);
			final AliasSDG alias = AliasSDG.readFrom(sdgFile);
			System.out.println("Using SDG from " + sdgFile);
			assertNotNull(alias.getSDG());
			checkRandomAlias(alias);
		} catch (ClassHierarchyException e) {
			e.printStackTrace();
			fail(e.getMessage());
		} catch (IOException e) {
			e.printStackTrace();
			fail(e.getMessage());
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

	@Test
	public void adjustAlias_Test_invokeSingleParamAlias() {
		final String src = "../MoJo-TestCode/src";
		final String bin = "../MoJo-TestCode/bin";
		// final String method = "Test.invokeSingleParamAlias(LTest$A3;)I";
		final String file = "Test.invokeSingleParamAlias(Test.A3)-mojotest";

		try {
			final MoJoTest mt = MoJoTest.create(src, bin, out);
			final String sdgFile = mt.getSDGFileName(file);
			final AliasSDG alias = AliasSDG.readFrom(sdgFile);
			System.out.println("Using SDG from " + sdgFile);
			assertNotNull(alias.getSDG());
			checkRandomAlias(alias);
		} catch (ClassHierarchyException e) {
			e.printStackTrace();
			fail(e.getMessage());
		} catch (IOException e) {
			e.printStackTrace();
			fail(e.getMessage());
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

	@Test
	public void adjustAlias_Test_invokeStringAndPrintln() {
		final String src = "../MoJo-TestCode/src";
		final String bin = "../MoJo-TestCode/bin";
		// final String method = "Test.invokeStringAndPrintln(Ljava/lang/String;Ljava/lang/String;LTest$A;)I";
		final String file = "Test.invokeStringAndPrintln(java.lang.String,java.lang.String,Test.A)-mojotest";

		try {
			final MoJoTest mt = MoJoTest.create(src, bin, out);
			final String sdgFile = mt.getSDGFileName(file);
			final AliasSDG alias = AliasSDG.readFrom(sdgFile);
			System.out.println("Using SDG from " + sdgFile);
			assertNotNull(alias.getSDG());
			checkRandomAlias(alias);
		} catch (ClassHierarchyException e) {
			e.printStackTrace();
			fail(e.getMessage());
		} catch (IOException e) {
			e.printStackTrace();
			fail(e.getMessage());
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

}
