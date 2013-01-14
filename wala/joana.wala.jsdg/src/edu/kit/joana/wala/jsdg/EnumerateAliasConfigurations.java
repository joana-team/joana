/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.wala.jsdg;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.util.HashSet;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Set;


import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.ipa.callgraph.AnalysisScope;
import com.ibm.wala.ipa.callgraph.impl.SetOfClasses;
import com.ibm.wala.ipa.cha.ClassHierarchy;
import com.ibm.wala.util.CancelException;
import com.ibm.wala.util.WalaException;
import com.ibm.wala.util.config.AnalysisScopeReader;
import com.ibm.wala.util.config.FileOfClasses;

import edu.kit.joana.deprecated.jsdg.Analyzer;
import edu.kit.joana.deprecated.jsdg.SDGFactory.Config;
import edu.kit.joana.deprecated.jsdg.sdg.nodes.JDependencyGraph.PDGFormatException;
import edu.kit.joana.deprecated.jsdg.util.Util;
import edu.kit.joana.wala.flowless.MoJo;
import edu.kit.joana.wala.flowless.pointsto.PtsParameter;
import edu.kit.joana.wala.flowless.pointsto.AliasGraph.MayAliasGraph;
import edu.kit.joana.wala.flowless.pointsto.GraphAnnotater.Aliasing;
import edu.kit.joana.wala.flowless.pointsto.PtsParameter.RootParameter;
import edu.kit.joana.wala.flowless.spec.ast.FlowAstVisitor.FlowAstException;
import edu.kit.joana.wala.jsdg.EvaluationRunner.CFG;
import gnu.trove.iterator.TLongIterator;
import gnu.trove.list.TLongList;
import gnu.trove.list.array.TLongArrayList;

public class EnumerateAliasConfigurations {

	public static final CFG[] EVAL_CFGS = {
//		new CFG("../MoJo-FlowLess/examples/project1", "../MoJo-FlowLess/bin", "java\\/awt\\/.*\n"
//				+ "javax\\/swing\\/.*\n" + "sun\\/awt\\/.*\n" + "sun\\/swing\\/.*\n"
//				+ "com\\/sun\\/.*\n" + "sun\\/.*\n"	+ "java\\/nio\\/.*\n" + "javax\\/.*\n"
//				+ "java\\/util\\/.*\n" + "java\\/security\\/.*\n"
//				+ "java\\/text\\/.*\n" + "java\\/io\\/.*\n" + "java\\/beans\\/.*\n" + "org\\/omg\\/.*\n"
//				+ "apple\\/awt\\/.*\n" + "com\\/apple\\/.*\n"),
		new CFG("../MoJo-FlowLess/examples/project1", "../MoJo-FlowLess/bin", "java\\/awt\\/.*\n"
				+ "javax\\/swing\\/.*\n" + "sun\\/awt\\/.*\n" + "sun\\/swing\\/.*\n"
				+ "com\\/sun\\/.*\n" + "sun\\/.*\n"	+ "java\\/nio\\/.*\n" + "javax\\/.*\n"
				+ "java\\/util\\/.*\n" + "java\\/security\\/.*\n"
				+ "java\\/text\\/.*\n" + "java\\/io\\/.*\n" + "java\\/beans\\/.*\n" + "org\\/omg\\/.*\n"
				+ "apple\\/awt\\/.*\n" + "com\\/apple\\/.*\n"),
///		new CFG("src", "bin"),
	};

	public static final boolean PURE_NUMBERS = true;
	public static final boolean POW_NUMBERS = true;

	/**
	 * @param args
	 * @throws CancelException
	 * @throws IllegalArgumentException
	 * @throws FlowAstException
	 * @throws NoSuchElementException
	 * @throws IOException
	 * @throws WalaException
	 * @throws PDGFormatException
	 */
	public static void main(String[] args) throws IllegalArgumentException, CancelException, NoSuchElementException,
			FlowAstException, IOException, PDGFormatException, WalaException {
		Analyzer.cfg = new Config();
		Analyzer.cfg.outputDir = "out/";

		for (CFG cfg : EVAL_CFGS) {
			run(cfg);
		}
	}

	/**
	 *
	 * @param cfg
	 * @throws IllegalArgumentException
	 * @throws CancelException
	 * @throws NoSuchElementException
	 * @throws FlowAstException
	 * @throws IOException
	 * @throws WalaException
	 * @throws PDGFormatException
	 */
	public static void run(final CFG cfg) throws IllegalArgumentException, CancelException, NoSuchElementException,
			FlowAstException, IOException, PDGFormatException, WalaException {
		System.out.print("Creating MoJo... ");

		AnalysisScope scope = AnalysisScopeReader.makeJavaBinaryAnalysisScope(cfg.bin, null);
		if (cfg.exclusions != null) {
			SetOfClasses exclusions = new FileOfClasses(new ByteArrayInputStream(cfg.exclusions.getBytes()));
			scope.setExclusions(exclusions);
		}

		final ClassHierarchy cha = ClassHierarchy.make(scope);
		final MoJo mojo = MoJo.create(cha);

		System.out.println("done.");

		for (IClass cls : cha) {
			if (cls.isArrayClass() || cls.isInterface()) {
				continue;
			}

			for (IMethod im : cls.getDeclaredMethods()) {
				if (im.isAbstract() || im.isNative() || im.isClinit() || im.isSynthetic() || im.isInit()
						|| !Util.methodName(im).contains("edu.kit.")) {
					continue;
				}

				// compute # alias configurations
				// compute min-max aliases
				if (!PURE_NUMBERS) { System.out.print("Working on " + Util.methodName(im) + ": "); }
//				{ System.out.print("Working on " + PrettyWalaNames.methodName(im) + ": "); }
				final Aliasing minMax = mojo.computeMinMaxAliasing(im);


				String naive = countNaiveAliasing(minMax.upperBound);
				if (!POW_NUMBERS) {	naive = pow(naive);	}
				if (!PURE_NUMBERS) { System.out.print("naive(" + naive + "), "); }

				String withCha = countChaAliasing(minMax.upperBound);
				if (!POW_NUMBERS) {	withCha = pow(withCha);	}
				if (!PURE_NUMBERS) { System.out.print("cha(" + withCha + "), "); }

				String withObjStruct = countObjStructAliasing(minMax.upperBound);
				if (!POW_NUMBERS) {	withObjStruct = pow(withObjStruct);	}
				if (!PURE_NUMBERS) { System.out.print("struct(" + withObjStruct + ")");}

				if (PURE_NUMBERS) {
					System.out.println(naive + "," + withCha + "," + withObjStruct);
				} else {
					System.out.println();
				}
			}
		}
	}

	private static String pow(String numStr) {
		final BigInteger two = new BigInteger("2");
		final int num = Integer.parseInt(numStr);
		final BigInteger twoTimesNum = two.pow(num);

		return twoTimesNum.toString();
	}

	private static String countNaiveAliasing(final MayAliasGraph max) {
		int numNonPrimitive = 0;
		for (PtsParameter p : max) {
			if (!p.isPrimitive()) {
				numNonPrimitive++;
			}
		}

		final int numEdges = (numNonPrimitive * (numNonPrimitive - 1)) / 2;
//		final BigInteger twoNum = new BigInteger("2");
//		final BigInteger twoTimesEdges = twoNum.pow(numEdges);

		if (PURE_NUMBERS) {
			return "" + numEdges;
		} else {
			return "2^" + numEdges;
		}
	}

	private static String countChaAliasing(final MayAliasGraph max) {
		final Set<PtsParameter> visited = new HashSet<PtsParameter>();
		int numNonPrimitiveEdges = 0;

		for (PtsParameter p : max) {
			visited.add(p);
			if (!p.isPrimitive()) {
				for (Iterator<PtsParameter> succIt = max.getSuccNodes(p); succIt.hasNext();) {
					PtsParameter succ = succIt.next();
					if (!succ.isPrimitive() && !visited.contains(succ)) {
						numNonPrimitiveEdges++;
					}
				}
			}
		}

//		final BigInteger twoNum = new BigInteger("2");
//		final BigInteger twoTimesEdges = twoNum.pow(numNonPrimitiveEdges);
//
//		return twoTimesEdges;
		if (PURE_NUMBERS) {
			return "" + numNonPrimitiveEdges;
		} else {
			return "2^" + numNonPrimitiveEdges;
		}
	}

	private static String countObjStructAliasing(final MayAliasGraph max) {
		final MayAliasGraph clone = max.clone();

		// remove primitive edges
		for (final PtsParameter p : clone) {
			if (p.isPrimitive()) {
				for (Iterator<PtsParameter> it = clone.getSuccNodes(p); it.hasNext();) {
					PtsParameter target = it.next();
					clone.removeEdge(p, target);
					clone.removeEdge(target, p);
				}
			}
		}

		final TLongList ladders = new TLongArrayList();

		for (final RootParameter r : clone.getRoots()) {
			final Set<PtsParameter> aliases = new HashSet<PtsParameter>();
			for (Iterator<PtsParameter> it = clone.getSuccNodes(r); it.hasNext();) {
				final PtsParameter alias = it.next();
				aliases.add(alias);
			}

			final Set<PtsParameter> toRemove = new HashSet<PtsParameter>();
			for (final PtsParameter alias : aliases) {
				if (alias.hasParent()) {
					// remove alias if it has a parent that is already included
					PtsParameter current = alias.getParent();
					while (current != null) {
						if (aliases.contains(current)) {
							toRemove.add(alias);
							current = null;
						} else {
							current = (current.hasParent() ? current.getParent() : null);
						}
					}
				}
			}

			aliases.removeAll(toRemove);

			for (final PtsParameter alias : aliases) {
				clone.removeEdge(r, alias);
				clone.removeEdge(alias, r);
				final Ladder ladder = new Ladder(r, alias);
				buildLadder(clone, ladder);
				ladders.add(ladder.countCombinations());
			}

			for (final PtsParameter p : toRemove) {
				clone.removeEdge(r, p);
				clone.removeEdge(p, r);
			}
		}

		int pow = 0;
		if (ladders.size() > 0) {
//		System.out.print("\nFound " + ladders.size() + " ladders: ");
		BigInteger big = new BigInteger("1");
		for (TLongIterator it = ladders.iterator(); it.hasNext();) {
			long next = it.next();
//			System.out.print(next + (it.hasNext() ? " * " : " "));
			big = big.multiply(new BigInteger(Long.toString(next)));
		}

		final BigInteger two = new BigInteger("2");
		while (!big.equals(BigInteger.ONE)) {
			big = big.divide(two);
			pow++;
		}

//		System.out.println(" * 2^(" + pow +  " edgecount)");
		}

		final Set<PtsParameter> visited = new HashSet<PtsParameter>();
		int numNonPrimitiveEdges = 0;

		for (PtsParameter p : clone) {
			visited.add(p);
			if (!p.isPrimitive()) {
				for (Iterator<PtsParameter> succIt = clone.getSuccNodes(p); succIt.hasNext();) {
					PtsParameter succ = succIt.next();
					if (!visited.contains(succ)) {
						numNonPrimitiveEdges++;
					}
				}
			}
		}

		if (PURE_NUMBERS) {
			return "" + (ladders.size() > 0 ? (pow + numNonPrimitiveEdges) : numNonPrimitiveEdges);
		} else {
			return ladders.size() > 0 ? "2^" + pow +  "+" + numNonPrimitiveEdges + "="
					+ (pow + numNonPrimitiveEdges) + " [" + ladders.size() + "]" : "2^" + numNonPrimitiveEdges ;
		}
	}

	private static void buildLadder(MayAliasGraph g, Ladder l) {
		// check for alias ladder
		for (final PtsParameter child : l.p1.getChildren()) {
			if (l.p2.hasChild(child.getName())) {
				final PtsParameter aliasChild = l.p2.getChild(child.getName());

				if (g.hasEdge(child, aliasChild)) {
					g.removeEdge(child, aliasChild);
					g.removeEdge(aliasChild, child);

					// add pair to ladder.
					final Ladder ladderChild = new Ladder(child, aliasChild, l);
					buildLadder(g, ladderChild);
				}
			}
		}
	}

	private static class Ladder {
		private final PtsParameter p1;
		private final PtsParameter p2;
		private final Ladder parent;
		private final Set<Ladder> children = new HashSet<Ladder>();

		private Ladder(final PtsParameter p1, final PtsParameter p2) {
			this(p1, p2, null);
		}

		private Ladder(final PtsParameter p1, final PtsParameter p2, final Ladder parent) {
			this.p1 = p1;
			this.p2 = p2;
			this.parent = parent;
			if (this.parent != null) {
				this.parent.children.add(this);
			}
		}

		private long countCombinations() {
			if (children.isEmpty()) {
				return 2l;
			} else {
				long comb = 1l;
				for (final Ladder child : children) {
					comb *= child.countCombinations();
				}
				comb += 1l;

				return comb;
			}
		}
	}
}
