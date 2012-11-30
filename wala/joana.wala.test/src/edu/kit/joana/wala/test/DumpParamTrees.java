/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.wala.test;

import edu.kit.joana.ifc.sdg.graph.SDG;
import edu.kit.joana.ifc.sdg.graph.SDGNode;
import gnu.trove.iterator.TIntIterator;
import gnu.trove.set.TIntSet;
import gnu.trove.set.hash.TIntHashSet;

import java.io.File;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;


public class DumpParamTrees {

	public static void main(final String[] argv) throws IOException {
		if (argv.length < 1) {
			System.out.println("Please provide the .pdg to load as argument and the id of the entry"
					+ " node whose parameter trees should be dumped.\nIf no entry id is provided, all entries will be"
					+ " dumped.");
			return;
		}

		final TIntSet entries = new TIntHashSet();
		final String pdgStr = argv[0];
		final File pdgFile = new File(pdgStr);
		if (!pdgFile.exists() || !pdgFile.isFile() || !pdgFile.canRead()) {
			System.out.println("Cannot read pdg file: " + pdgFile.getAbsolutePath());
			return;
		}

		for (int i = 1; i < argv.length - 1; i++) {
			final String entryIdStr = argv[i];
			try {
				final int entryId = Integer.parseInt(entryIdStr);
				entries.add(entryId);
			} catch (NumberFormatException exc) {
				System.out.println("Could not parse entry node id: " + entryIdStr);
			}
		}

		System.out.print("Loading sdg from " + pdgFile.getAbsolutePath() + "... ");
		final SDG sdg = SDG.readFrom(pdgStr);
		System.out.println("done.");

		final List<SDGNode> entryNodes = new LinkedList<SDGNode>();
		System.out.print("Checking entry ids... ");
		if (entries.size() > 0) {
			for (final TIntIterator it = entries.iterator(); it.hasNext();) {
				final int id = it.next();
				final SDGNode n = sdg.getNode(id);
				if (n.kind == SDGNode.Kind.ENTRY || n.kind == SDGNode.Kind.CALL) {
					entryNodes.add(n);
				} else {
					System.out.print("no entry - skipping (" + n.getId() + ") ");
				}
			}
		} else {
			for (final SDGNode n : sdg.vertexSet()) {
				if (n.kind == SDGNode.Kind.ENTRY) {
					entryNodes.add(n);
				}
			}
		}

		System.out.println(entryNodes.size() + " nodes found.");

		final String toDirStr = pdgStr + "-ptree";
		final File toDir = new File(toDirStr);
		if (toDir.exists() && (!toDir.isDirectory() || !toDir.canWrite())) {
			System.out.println("Cannot write already existing dir " + toDir.getAbsolutePath());
			return;
		} else if (!toDir.exists()) {
			if (!toDir.mkdir()) {
				System.out.println("Could not create directory " + toDir.getAbsolutePath());
				return;
			}
		}

		for (final SDGNode n : entryNodes) {
			dumpParamTrees(sdg, n, toDirStr);
		}
	}

	private static void dumpParamTrees(final SDG sdg, final SDGNode n, final String toDir) {
		System.out.println("Dumping " + n.getId() + "|" + n.kind + "(" + n.getLabel() + ")");
		final String outDirStr = toDir + File.separator + "n_" + n.getId() + "_" + n.kind;
		final File outDir = new File(outDirStr);
		if (outDir.exists() && (!outDir.isDirectory() || !outDir.canWrite())) {
			System.out.println("Cannot write already existing dir " + outDir.getAbsolutePath());
			return;
		} else if (!outDir.exists()) {
			if (!outDir.mkdir()) {
				System.out.println("Could not create directory " + outDir.getAbsolutePath());
				return;
			}
		}


	}

}
