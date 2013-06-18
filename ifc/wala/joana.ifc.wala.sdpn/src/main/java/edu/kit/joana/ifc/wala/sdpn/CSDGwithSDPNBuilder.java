/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.ifc.wala.sdpn;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.shrikeCT.InvalidClassFileException;
import com.ibm.wala.ssa.SSAInstruction;
import com.ibm.wala.types.ClassLoaderReference;
import com.ibm.wala.util.CancelException;
import com.ibm.wala.util.MonitorUtil.IProgressMonitor;
import com.ibm.wala.util.WalaException;
import com.ibm.wala.util.io.FileUtil;

import de.wwu.sdpn.core.util.NullProgressMonitor;
import de.wwu.sdpn.core.util.WPMWrapper;
import de.wwu.sdpn.wala.analyses.DPN4IFCAnalysis;
import edu.kit.joana.deprecated.jsdg.sdg.nodes.JDependencyGraph.PDGFormatException;
import edu.kit.joana.deprecated.jsdg.util.Util;
import edu.kit.joana.ifc.sdg.graph.SDGEdge;
import edu.kit.joana.ifc.sdg.graph.SDGNode;
import edu.kit.joana.ifc.sdg.graph.SDGSerializer;
import edu.kit.joana.ifc.sdg.graph.slicer.graph.threads.ThreadRegions;

/**
 * @author Juergen Graf <juergen.graf@gmail.com>
 *
 */
public class CSDGwithSDPNBuilder {

	public static final String JRE14_STUBS = "../../../contrib/lib/stubs/jSDG-stubs-jre1.4.jar";
	public static final String J2ME_STUBS = "../../../contrib/lib/stubs/jSDG-stubs-j2me2.0.jar";
	public static final String JAVACARD_STUBS = "../../../contrib/lib/stubs/jSDG-stubs-javacard.jar";
	
	public static final String JRE14_LIB = "Primordial,Java,jarFile," + JRE14_STUBS;
	public static final String J2ME_LIB = "Primordial,Java,jarFile," + J2ME_STUBS;
	public static final String JAVACARD_LIB = "Primordial,Java,jarFile," + JAVACARD_STUBS;
	
	
	private static final String OUTPUT_DIR = "/tmp/sdpnifc";
	private static final boolean DO_CACHE = false;
	private static final boolean SKIP_PRIMORDIAL = true;


	private static final long XSB_TIME_OUT = 1000*60*2;

	public static void main(String args[]) throws IllegalArgumentException, CancelException, PDGFormatException, IOException, WalaException, InvalidClassFileException  {
		SDGCreator sdgCreator = new SDGCreatorMoJoStyle();
		//runAnalysis(sdgCreator, "../joana.ifc.wala.sdpn/bin", "Lexamples/BSP01", JRE14_LIB); // Removed 1 of 1 interference edges. Cached 0 runs.
		//runAnalysis(sdgCreator, "../joana.ifc.wala.sdpn/bin", "Lexamples/BSP02", JRE14_LIB); // Removed 0 of 1 interference edges. Cached 0 runs.
		//runAnalysis(sdgCreator, "../joana.ifc.wala.sdpn/bin", "Lexamples/BSP03", JRE14_LIB); // Removed 8 of 14 interference edges. Cached 7 runs.
		//runAnalysis(sdgCreator, "../joana.ifc.wala.sdpn/bin", "Lexamples/BSP04", JRE14_LIB); // Removed 0 of 0 interference edges. Cached 0 runs.
		//runAnalysis(sdgCreator, "../joana.ifc.wala.sdpn/bin", "Lexamples/CutTest", JRE14_LIB); // Removed 0 of 0 interference edges. Cached 0 runs.
		//runAnalysis(sdgCreator, "../joana.ifc.wala.sdpn/bin", "Lexamples/MyThread", JRE14_LIB); // Removed 0 of 0 interference edges. Cached 0 runs.
		//runAnalysis(sdgCreator, "../Tests/bin", "Lconc/TimeTravel", JRE14_LIB);
		//runAnalysis(sdgCreator, "../Tests/bin", "Lconc/dp/DiningPhilosophers", JRE14_LIB); 		// no locks found - error...
		//runAnalysis(sdgCreator, "../Tests/bin", "Ltests/Synchronization", JRE14_LIB);
		//runAnalysis(sdgCreator, "../Tests/bin", "Ltests/ConcPasswordFile", JRE14_LIB); // Removed 0 of 1
		//runAnalysis(sdgCreator, "../Tests/bin", "Lconc/cliser/kk/Main", JRE14_LIB); // nasty error in jsdg
		//runAnalysis(sdgCreator, "../Tests/bin", "Lconc/kn/Knapsack5", JRE14_LIB); 				// computes very long
		//runAnalysis(sdgCreator, "../Tests/bin", "Lconc/bb/ProducerConsumer", JRE14_LIB); 		// no locks found - error...
		//runAnalysis(sdgCreator, "../Tests/bin", "Ltests/Mantel00Page10", JRE14_LIB); 		    // no locks found - error...

		/* Is there something wrong with the lock sensitive thread regions?
		 * I would expect a new one to begin after a monitor exit so there
		 * shouldn't be caching.  (Make sure caching is turned on!)
		 * -- Benedikt
		 */


//		runAnalysis(sdgCreator, "../joana.ifc.wala.sdpn/bin", "Lexamples/A", JRE14_LIB); // Removed 2 of 2 interference edges. Cached 1 runs.


		// TODO UNSOUNDNESS?
		/* Compared to example A why are there no interference edges?
		 * The field was merely changed from static to nonstatic.
		 * -- Benedikt
		 */

//		runAnalysis("../jSDG-sdpn/bin", "Lexamples/B", JRE14_LIB); // Removed 0 of 0 interference edges. Cached 0 runs.


		/* When using a separate class for the nonstatic field everything is fine.
		 * -- Benedikt
		 */
//		runAnalysis("../jSDG-sdpn/bin", "Lexamples/C", JRE14_LIB); // Removed 2 of 2 interference edges. Cached 0 runs.


		/* Trying killing definitions.
		 */
		//runAnalysis(sdgCreator, "../joana.ifc.wala.sdpn/bin", "Lexamples/Killing04", JRE14_LIB);



//		System.exit(0); // xsb listener thread needs to be destroyed.
	}

	public static RefinementResult runAnalysis(SDGCreator sdgCreator, final String bin, final String mainClass, final String runtimeLib)
			throws IllegalArgumentException, CancelException, PDGFormatException, IOException, WalaException, InvalidClassFileException {
				List<String> runtimeLibs = new LinkedList<String>();
				runtimeLibs.add(runtimeLib);
				return runAnalysis(sdgCreator, bin,mainClass,runtimeLibs,OUTPUT_DIR,DO_CACHE,SKIP_PRIMORDIAL,XSB_TIME_OUT);
	}


	public static RefinementResult runAnalysis(
						SDGCreator sdgCreator, 
						final String bin, 
						final String mainClass, 
						final List<String> runtimeLibs,
						final String outputDir,
						final boolean do_cache, 
						final boolean skip_primordial, 
						final long xsbtimeout) 
					throws IllegalArgumentException, CancelException, PDGFormatException, IOException, WalaException, InvalidClassFileException {
		final String mainClassSimpleName = mainClass.replace('/', '.').replace('$', '.').substring(1);
		final String outputSDGFile = outputDir + "/" + mainClassSimpleName + ".pdg";
				IProgressMonitor progress = new com.ibm.wala.util.NullProgressMonitor();//new VerboseProgressMonitor(System.out);
				JoanaSDGResult result = sdgCreator.buildSDG(bin, mainClass, runtimeLibs, outputDir, progress);

		// from here, stuff only relies on edu.kit.joana.ifc.sdg.graph.SDG - and some other structures, which we can get with or without use of jsdg

		{
		progress.beginTask("Saving SDG to " + outputSDGFile, -1);
		BufferedOutputStream bOut = new BufferedOutputStream(new FileOutputStream(outputSDGFile));
		SDGSerializer.toPDGFormat(result.sdg, bOut);
		progress.done();
		}

		//Util.dumpCallGraph(result.cg, mainClassSimpleName, null);

		final DPN4IFCAnalysis dpn = new DPN4IFCAnalysis(result.cg, result.pts);
		dpn.init(new WPMWrapper(progress));

		List<SDGEdge> toRemove = new LinkedList<SDGEdge>();
		int ifEdges = 0;

		for (SDGEdge e : result.sdg.edgeSet()) {
			if ((e.getKind() == SDGEdge.Kind.INTERFERENCE /*|| e.getKind() == SDGEdge.Kind.INTERFERENCE_WRITE*/) &&
					(e.getSource().kind != SDGNode.Kind.SYNCHRONIZATION && e.getTarget().kind != SDGNode.Kind.SYNCHRONIZATION)) {
				// ignore interference edges between syncs
				ifEdges++;
			}
		}

		System.out.println("Found " + ifEdges + " potential interferences.");

		//final MHPAnalysis mhp = result.mhp;
		final ThreadRegions tr =
			LockAwareThreadRegions.compute(result.sdg);
			//result.mhp.getTR();
		System.out.println("Lock sensitive thread regions: " + tr.size() + " - normal regions: " + result.mhp.getThreadRegions().size());
		//TODO caching does not work, because our thread regions are not fine grained enough - a synchronized/monitorenter
		// does not result in a new region.
		// entries are true iff the two thread regions in question may never run in parallel
		// so iff all pairs of thread regions of two nodes are true, the dpn analysis can be skipped
		// and the interference deleted.
		boolean[][] nopar = null;
		boolean[][] surepar = null;
		if (do_cache) {
			nopar = new boolean[tr.size()][tr.size()];
			surepar = new boolean[tr.size()][tr.size()];
		}

		int cached = 0;
		int current = 0;

		for (SDGEdge e : result.sdg.edgeSet()) {
			if ((e.getKind() == SDGEdge.Kind.INTERFERENCE /*|| e.getKind() == SDGEdge.Kind.INTERFERENCE_WRITE*/)
					 &&	(e.getSource().kind != SDGNode.Kind.SYNCHRONIZATION && e.getTarget().kind != SDGNode.Kind.SYNCHRONIZATION)) {

				System.out.print(++current + " of " + ifEdges + ": ");

				if (do_cache) {
					if (isSurePar(tr, surepar, e)) {
						cached++;
						// we know for sure that the two instructions may happen in parallel - no need to run dpn
						System.out.println("[Cached] Edge definitely exists: " + edge2str(result.sdg, e));
						continue;
					} else if (isNoPar(tr, nopar, e)) {
						cached++;
						// we know for sure that the two instructions may NEVER happen in parallel - no need to run dpn
						// remove the interference edge
						System.out.println("[Cached] Edge cannot exist: " + edge2str(result.sdg, e));
						toRemove.add(e);
						continue;
					}
				}

				final SDGNode from = result.sdg.getEntry(e.getSource());
				final SDGNode to = result.sdg.getEntry(e.getTarget());
				final CGNode nodeFrom = result.cg.getNode(findCorrespondingCGNodeId(result.sdg, from)); // get cgnode for corresponding method of edge
				final CGNode nodeTo = result.cg.getNode(findCorrespondingCGNodeId(result.sdg, to)); // get cgnode for corresponding method of edge
				final SDGNode source = e.getSource();
				final SDGNode target = e.getTarget();
				final int indexFrom = findCorrespondingInstructionIndex(result.sdg, nodeFrom, source); // get instruction index for from node
				final int indexTo = findCorrespondingInstructionIndex(result.sdg, nodeTo, target); // get instruction index for to node


				{
					boolean skip = false;
					final SSAInstruction[] irFrom = nodeFrom.getIR().getInstructions();
					if (indexFrom < 0 || irFrom == null || irFrom.length < indexFrom || irFrom[indexFrom] == null) {
						System.out.println("Illegal index from-node " + indexFrom + "@" + nodeFrom.getMethod() + ": " + source.getBytecodeMethod() + " - " + source.getKind() + "|" + source.getLabel());
						Util.dumpSSA(nodeFrom.getIR(), System.out);
						skip = true;
					}

					final SSAInstruction[] irTo = nodeTo.getIR().getInstructions();
					if (indexTo < 0 || irTo == null || irTo.length < indexTo || irTo[indexTo] == null) {
						System.out.println("Illegal index to-node " +  indexTo + "@" + nodeTo.getMethod() + ": " + target.getBytecodeMethod() + " - " + target.getKind() + "|" + target.getLabel());
						Util.dumpSSA(nodeTo.getIR(), System.out);
						skip = true;
					}

					if (skip) continue;
				}

				if (skip_primordial && (nodeFrom.getMethod().getDeclaringClass().getClassLoader().getReference().equals(ClassLoaderReference.Primordial) ||
						nodeTo.getMethod().getDeclaringClass().getClassLoader().getReference().equals(ClassLoaderReference.Primordial))) {
					System.out.println("Skipping interference: " + edge2str(result.sdg, e));
				} else {
					System.out.println("Checking interference: " + edge2str(result.sdg, e));

					try {
						if (!dpn.mayFlowFromTo(nodeFrom,indexFrom,nodeTo,indexTo,
																new NullProgressMonitor(),xsbtimeout)) {
															System.out
															.println("Removing interference from "
																	+ indexFrom + "@"
																	+ nodeFrom.getMethod() + " to "
																	+ indexTo + "@"
																	+ nodeTo.getMethod());
															System.out.println("\t"
																	+ edge2str(result.sdg, e));
															toRemove.add(e);
															if (do_cache) {
																setNoPar(tr, nopar, e);
							}
						} else {
							if (do_cache) {
								setSurePar(tr, surepar, e);
							}
						}
					} catch (ArrayIndexOutOfBoundsException exc) {
						exc.printStackTrace();
					} catch (NullPointerException exc) {
						exc.printStackTrace();
					} catch (RuntimeException exc) {
						exc.printStackTrace();
					}
				}
			}
		}

		result.sdg.removeAllEdges(toRemove);

		{
		progress.beginTask("Saving sdpn-optimized SDG to " + outputSDGFile + "-sdpn", -1);
		BufferedOutputStream bOut = new BufferedOutputStream(new FileOutputStream(outputSDGFile + "-sdpn"));
		SDGSerializer.toPDGFormat(result.sdg, bOut);
		progress.done();
		}

		for (final SDGEdge e : toRemove) {
			System.out.println("REMOVED: " + edge2str(result.sdg, e));
		}

		System.out.println("Removed " + toRemove.size() + " of " + ifEdges + " interference edges. Cached " + cached + " runs.");

		dpn.shutdown();

		return new RefinementResult(toRemove.size(),ifEdges,cached);
	}

	private static int findCorrespondingCGNodeId(edu.kit.joana.ifc.sdg.graph.SDG sdg, SDGNode entryNode) {
		int cgid = sdg.getCGNodeId(entryNode);
		if (cgid != edu.kit.joana.ifc.sdg.graph.SDG.UNDEFINED_CGNODEID) {
			return cgid;
		} else {
			return entryNode.tmp;
		}
	}


	private static int findCorrespondingInstructionIndex(edu.kit.joana.ifc.sdg.graph.SDG sdg, CGNode method, SDGNode n) {
		int iindex = sdg.getInstructionIndex(n);
		if (iindex != edu.kit.joana.ifc.sdg.graph.SDG.UNDEFINED_IINDEX) {
			return iindex;
		} else {
			return findCorrespondingInstructionIndex(method, n);
		}
	}

	private static int findCorrespondingInstructionIndex(CGNode method, SDGNode n) {
		/*
		 * For normal nodes the instruction index is stored in n.tmp.
		 * For entry nodes n.tmp contains the id of the CGNode for the corresponding method.
		 * When we try to find a matching instruction index for a entry node, we have to search
		 * for the first non-null instruction. Which may not always be on pos 0.
		 */

		// -1 == no instructions here -> we have to ignore it -> no dpn analysis for this edge...
		int iindex = -1;

		if (n.kind == SDGNode.Kind.ENTRY) {
			final SSAInstruction[] ir = method.getIR().getInstructions();
			if (ir != null) {
				for (int i = 0; i < ir.length; i++) {
					if (ir[i] != null) {
						iindex = i;
						break;
					}
				}
			}
		} else if (n.kind == SDGNode.Kind.EXIT) {
			final SSAInstruction[] ir = method.getIR().getInstructions();
			if (ir != null) {
				iindex = ir.length - 1;
			}
		} else {
			iindex = n.tmp;
		}

		return iindex;
	}

	private static String edge2str(edu.kit.joana.ifc.sdg.graph.SDG sdg, SDGEdge e) {
		final SDGNode from = e.getSource();
		final SDGNode to = e.getTarget();
		final SDGNode eFrom = sdg.getEntry(e.getSource());
		final SDGNode eTo = sdg.getEntry(e.getTarget());

		return eFrom.getLabel() + "{" + from.getLabel() + " @" + from.getBytecodeIndex() + "} -|" + e.getKind() + "|-> "
			+ eTo.getLabel() + "{" + to.getLabel() + " @" + to.getBytecodeIndex() + "}";
	}

	private static int[] getThreadRegions(final ThreadRegions tr, final SDGNode n) {
		final int[] tr1 = new int[n.getThreadNumbers().length];
		int pos = 0;

		for (int i : n.getThreadNumbers()) {
			tr1[pos] = tr.getThreadRegion(n, i).getID();
			pos++;
		}

		return tr1;
	}

	private static boolean isSurePar(ThreadRegions tr, boolean[][]sure, SDGEdge e) {
		final SDGNode n1 = e.getSource();
		final SDGNode n2 = e.getTarget();
		final int[] tr1 = getThreadRegions(tr, n1);
		final int[] tr2 = getThreadRegions(tr, n2);

		for (int i1 = 0; i1 < tr1.length; i1++) {
			for (int i2 = 0; i2 < tr2.length; i2++) {
				if (sure[i1][i2]) {
					return true;
				}
			}
		}

		return false;
	}

	private static boolean isNoPar(ThreadRegions tr, boolean[][]nopar, SDGEdge e) {
		final SDGNode n1 = e.getSource();
		final SDGNode n2 = e.getTarget();
		final int[] tr1 = getThreadRegions(tr, n1);
		final int[] tr2 = getThreadRegions(tr, n2);

		for (int i1 = 0; i1 < tr1.length; i1++) {
			for (int i2 = 0; i2 < tr2.length; i2++) {
				// all entries have to be true
				if (!nopar[i1][i2]) {
					return false;
				}
			}
		}

		return true;
	}

	private static void setSurePar(ThreadRegions tr, boolean[][]sure, SDGEdge e) {
		final SDGNode n1 = e.getSource();
		final SDGNode n2 = e.getTarget();
		final int[] tr1 = getThreadRegions(tr, n1);
		final int[] tr2 = getThreadRegions(tr, n2);

		if (tr1.length == 1 && tr2.length == 1) {
			sure[tr1[0]][tr2[0]] = true;
			sure[tr2[0]][tr1[0]] = true;
		}
	}

	private static void setNoPar(ThreadRegions tr, boolean[][]nopar, SDGEdge e) {
		final SDGNode n1 = e.getSource();
		final SDGNode n2 = e.getTarget();
		final int[] tr1 = getThreadRegions(tr, n1);
		final int[] tr2 = getThreadRegions(tr, n2);

		for (int i1 = 0; i1 < tr1.length; i1++) {
			for (int i2 = 0; i2 < tr2.length; i2++) {
				// all entries have to be true
				nopar[i1][i2] = true;
				nopar[i2][i1] = true;
			}
		}
	}

	private static String[] getJarsInDirectory(String dir) {
		Collection<File> col = FileUtil.listFiles(dir, ".*\\.jar$", true);
		String[] result = new String[col.size()];
		int i = 0;
		for (File jarFile : col) {
			result[i++] = jarFile.getAbsolutePath();
		}
		return result;
	}

}
