package edu.kit.joana.wala.eval;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import com.ibm.wala.cfg.ControlFlowGraph;
import com.ibm.wala.cfg.IBasicBlock;
import com.ibm.wala.cfg.exc.ExceptionPruningAnalysis;
import com.ibm.wala.cfg.exc.NullPointerAnalysis;
import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.ipa.callgraph.AnalysisCache;
import com.ibm.wala.ipa.callgraph.AnalysisScope;
import com.ibm.wala.ipa.cha.ClassHierarchy;
import com.ibm.wala.ipa.cha.ClassHierarchyException;
import com.ibm.wala.ssa.IR;
import com.ibm.wala.ssa.ISSABasicBlock;
import com.ibm.wala.ssa.SSAInstruction;
import com.ibm.wala.types.ClassLoaderReference;
import com.ibm.wala.types.TypeReference;
import com.ibm.wala.util.CancelException;
import com.ibm.wala.util.config.AnalysisScopeReader;
import com.ibm.wala.util.config.FileOfClasses;
import com.ibm.wala.util.config.SetOfClasses;
import com.ibm.wala.util.graph.Graph;
import com.ibm.wala.util.graph.GraphIntegrity.UnsoundGraphException;

import edu.kit.joana.wala.core.NullProgressMonitor;
import edu.kit.joana.wala.util.PrettyWalaNames;

public class ExceptionTest {

	private static PrintStream out = System.out;
	public final static String EXCLUSION_REG_EXP = "java\\/awt\\/.*\n" + "javax\\/swing\\/.*\n" + "sun\\/awt\\/.*\n"
			+ "sun\\/swing\\/.*\n" + "com\\/sun\\/.*\n" + "sun\\/.*\n";

	private ExceptionTest() {
		throw new UnsupportedOperationException();
	}

	public static final Configuration[] EVALCFGS = {
			new Configuration("JGF Barrier", "def.JGFBarrierBench.main([Ljava/lang/String;)V",
					"../../example/joana.example.jars/javagrande/benchmarks.jar"),
			new Configuration("JGF Crypt", "def.JGFCryptBenchSizeA.main([Ljava/lang/String;)V",
					"../../example/joana.example.jars/javagrande/benchmarks.jar"),
			new Configuration("JGF ForkJoin", "def.JGFForkJoinBench.main([Ljava/lang/String;)V",
					"../../example/joana.example.jars/javagrande/benchmarks.jar"),
			new Configuration("JGF LUFact", "def.JGFLUFactBenchSizeA.main([Ljava/lang/String;)V",
					"../../example/joana.example.jars/javagrande/benchmarks.jar"),
			new Configuration("JGF MolDyn", "def.JGFMolDynBenchSizeA.main([Ljava/lang/String;)V",
					"../../example/joana.example.jars/javagrande/benchmarks.jar"),
			new Configuration("JGF MonteCarlo", "def.JGFMonteCarloBenchSizeA.main([Ljava/lang/String;)V",
					"../../example/joana.example.jars/javagrande/benchmarks.jar"),
			new Configuration("JGF RayTracer", "def.JGFRayTracerBenchSizeA.main([Ljava/lang/String;)V",
					"../../example/joana.example.jars/javagrande/benchmarks.jar"),
			new Configuration("JGF Series", "def.JGFSeriesBenchSizeA.main([Ljava/lang/String;)V",
					"../../example/joana.example.jars/javagrande/benchmarks.jar"),
			new Configuration("JGF SOR", "def.JGFSORBenchSizeA.main([Ljava/lang/String;)V",
					"../../example/joana.example.jars/javagrande/benchmarks.jar"),
			new Configuration("JGF SparseMatmult", "def.JGFSparseMatmultBenchSizeA.main([Ljava/lang/String;)V",
					"../../example/joana.example.jars/javagrande/benchmarks.jar"),
			new Configuration("JGF Sync", "def.JGFSyncBench.main([Ljava/lang/String;)V",
					"../../example/joana.example.jars/javagrande/benchmarks.jar"),
			new Configuration("HSQLDB", "org.hsqldb.Server.main([Ljava/lang/String;)V",
					"../../example/joana.example.jars/hsqldb/HSQLDB.jar",
					EXCLUSION_REG_EXP + "java\\/nio\\/.*\n" + "javax\\/.*\n" + "java\\/util\\/.*\n"
							+ "java\\/security\\/.*\n" + "java\\/beans\\/.*\n" + "org\\/omg\\/.*\n"
							+ "apple\\/awt\\/.*\n" + "com\\/apple\\/.*\n"),
			new Configuration("jEdit", "org.gjt.sp.jedit.jEdit.main([Ljava/lang/String;)V",
					"../../example/joana.example.jars/jedit/jedit.jar",
					EXCLUSION_REG_EXP + "java\\/nio\\/.*\n" + "javax\\/.*\n" + "java\\/util\\/.*\n"
							+ "java\\/security\\/.*\n" + "java\\/beans\\/.*\n" + "org\\/omg\\/.*\n"
							+ "apple\\/awt\\/.*\n" + "com\\/apple\\/.*\n"),
	};

	public static class Configuration {
		public final String name;
		public final String entryMethod;
		public final String classpath;
		public final String exclusions;

		public Configuration(String name, String entryMethod, String classpath) {
			this(name, entryMethod, classpath, EXCLUSION_REG_EXP);
		}

		public Configuration(String name, String entryMethod, String classpath, String exclusions) {
			this.name = name;
			this.classpath = classpath;
			this.entryMethod = entryMethod;
			this.exclusions = exclusions;
		}
	}
	
	private static class ExcEntry implements Comparable<ExcEntry> {
		private final TypeReference type;
		private int count;
		
		private ExcEntry(final TypeReference type) {
			this.type = type;
			this.count = 0;
		}
		
		public String toString() {
			return count + "\t" + PrettyWalaNames.bcTypeName(type);
		}

		@Override
		public int compareTo(final ExcEntry o) {
			return this.count - o.count;
		}
 	}

	private static void run(final Configuration config, final Result r) throws IOException, ClassHierarchyException {
		out.println("analyzing " + config.name);
		out.print("\tcreating class hierarchy... ");

		final AnalysisScope scope = AnalysisScopeReader.makePrimordialScope(null);
		final SetOfClasses exclusions = new FileOfClasses(new ByteArrayInputStream(config.exclusions.getBytes()));
		scope.setExclusions(exclusions);
		final ClassLoaderReference loader = scope.getLoader(AnalysisScope.APPLICATION);
		AnalysisScopeReader.addClassPathToScope(config.classpath, scope, loader);
		final ClassHierarchy cha = ClassHierarchy.make(scope);

		out.println("done.");
		
		out.print("\tcounting instructions... ");

		int methods = 0;
		final AnalysisCache cache = new AnalysisCache();
		for (final IClass cls : cha) {
			if (cls.getClassLoader().getName() == AnalysisScope.APPLICATION) {
//				out.println(PrettyWalaNames.bcTypeName(cls));
				
				for (final IMethod im : cls.getDeclaredMethods()) {
					final IR ir = cache.getIR(im);
					if (ir != null) {
						methods++;
//						out.println("\t" + PrettyWalaNames.methodName(im));
						final SSAInstruction[] m = ir.getInstructions();
						for (final SSAInstruction instr : m) {
							if (instr != null && instr.isPEI()) {
								final Collection<TypeReference> excs = instr.getExceptionTypes();
								countExceptions(r.count, excs);
								r.countExc++;
							}
							if (instr != null) r.realInstr++;
						}
						r.totalInstr += m.length;
						
						try {
							final ControlFlowGraph<SSAInstruction, ISSABasicBlock> cfgBefore = ir.getControlFlowGraph();
							final ExceptionPruningAnalysis<SSAInstruction, ISSABasicBlock> epa =
								NullPointerAnalysis.createIntraproceduralSSACFGAnalyis(ir);
							epa.compute(NullProgressMonitor.INSTANCE);
							final ControlFlowGraph<SSAInstruction, ISSABasicBlock> cfgAfter = epa.getCFG();
							r.totalEdges += countEdges(cfgBefore);
							r.intraEdges += countEdges(cfgAfter);
							r.totalBranches += countBranches(cfgBefore);
							r.intraBranches += countBranches(cfgAfter);
							r.nonExcBranches += countRealBranches(cfgBefore);
						} catch (UnsoundGraphException e) {
							e.printStackTrace();
						} catch (CancelException e) {
							e.printStackTrace();
						}
					}
				}
			}
		}

		out.println("(" + methods + " methods) done.");
	}
	
	private static <T> long countEdges(final Graph<T> g) {
		long count = 0;
		
		for (final T n : g) {
			count += g.getSuccNodeCount(n);
		}
		
		return count;
	}

	private static <T> long countBranches(final Graph<T> g) {
		long count = 0;
		
		for (final T n : g) {
			if (g.getSuccNodeCount(n) > 1) {
				count++;
			}
		}
		
		return count;
	}

	private static <I, T extends IBasicBlock<I>> long countRealBranches(final ControlFlowGraph<I, T> g) {
		long count = 0;
		
		for (final T n : g) {
			if (g.getNormalSuccessors(n).size() > 1) {
				count++;
			}
		}
		
		return count;
	}

	private static void countExceptions(final Map<TypeReference, ExcEntry> map, final Collection<TypeReference> excs) {
		for (final TypeReference t : excs) {
			ExcEntry l = map.get(t);
			if (l == null) {
				l = new ExcEntry(t);
				map.put(t, l);
			}
			l.count++;
		}
	}
	
	private static class Result {
		private final Map<TypeReference,ExcEntry> count = new HashMap<TypeReference, ExcEntry>();
		private long countExc = 0;
		private long totalInstr = 0;
		private long realInstr = 0;
		private long totalEdges = 0;
		private long intraEdges = 0;
		private long totalBranches = 0;
		private long intraBranches = 0;
		private long nonExcBranches = 0;
		
		public String toString() {
			final StringBuffer sb = new StringBuffer("\n==== results ====\n");
			sb.append("counted " + totalInstr + " instructions. " + realInstr + " real instructions. " + countExc
					+ "(" + ((100 * countExc) / totalInstr) + "% total) (" + ((100 * countExc) / realInstr)
					+ "% real) throw exceptions.\n");
			
			final Set<ExcEntry> results = new TreeSet<ExceptionTest.ExcEntry>(count.values());
			for (final ExcEntry e : results) {
				sb.append("\t(" + ((100 * e.count) / totalInstr) + "% of all) \t("
						+ ((100 * e.count) / realInstr) + "% of real) \t("
						+ ((100 * e.count) / countExc) + "% of pei) \t" + e + "\n");
			}

			sb.append("cfg optimization:\n");
			sb.append("\t" + totalEdges + " total edges. " + intraEdges + "("
					+ ((100 * intraEdges) / totalEdges) + "%) after intraproc optimization.\n");
			sb.append("\t" + totalBranches + " total branches. " + intraBranches + "("
					+ ((100 * intraBranches) / totalBranches) + "%) after intraproc optimization.\n");
			final long excBranches = totalBranches - nonExcBranches;
			final long removedBranches = totalBranches - intraBranches;
			final long excOptBranches = excBranches - removedBranches;
			sb.append("\t" + nonExcBranches + "(" + ((100 * nonExcBranches) / totalBranches) + "%) real, "
					+ excBranches + "(" + ((100 * excBranches) / totalBranches) + "%) exception branches. "
					+ excOptBranches + "(" + ((100 * excOptBranches) / totalBranches) + "%) exception branches optimized. "
					+ removedBranches + "(" + ((100 * removedBranches) / excBranches) + "%) exception branches removed.\n");
			
			return sb.toString();
		}
	}
	
	public static void main(String[] args) {
		final Result r = new Result();
		
		for (final Configuration config : EVALCFGS) {
			try {
				run(config, r);
			} catch (ClassHierarchyException e) {
				e.printStackTrace(out);
				out.println("error in evaluation: skipping " + config);
			} catch (IOException e) {
				e.printStackTrace(out);
				out.println("error in evaluation: skipping " + config);
			}
		}
		
		out.println(r);
	}

}
