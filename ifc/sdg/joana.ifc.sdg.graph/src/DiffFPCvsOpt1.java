/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */

import java.util.Collection;
import java.util.LinkedList;
import edu.kit.joana.ifc.sdg.graph.PDGs;
import edu.kit.joana.ifc.sdg.graph.SDG;
import edu.kit.joana.ifc.sdg.graph.SDGEdge;
import edu.kit.joana.ifc.sdg.graph.SDGNode;
import edu.kit.joana.ifc.sdg.graph.chopper.Chopper;
import edu.kit.joana.ifc.sdg.graph.chopper.FixedPointChopper;
import edu.kit.joana.ifc.sdg.graph.chopper.Opt1Chopper;


@SuppressWarnings("unused")
public class DiffFPCvsOpt1 {
	public static void main (String[] args) throws Exception {
		int nr = 10000;
		int offset = 25;
		int output = 100;

		for (int i = 9; i < PDGs.pdgs.length; i++) {
			SDG g = SDG.readFrom(PDGs.pdgs[i]);

			// XXX: uncomment in case you intend to chop sequential programs
			LinkedList<SDGEdge> l = new LinkedList<SDGEdge>();
			for (SDGEdge e : g.edgeSet()) {
				if (e.getKind().isThreadEdge()) l.add(e);
			}
			for (SDGEdge e : l) {
				g.removeEdge(e);
				if (e.getKind() == SDGEdge.Kind.FORK) g.addEdge(new SDGEdge(e.getSource(), e.getTarget(), SDGEdge.Kind.CALL));
			}
			// XXX>

			System.out.println("chopping "+PDGs.pdgs[i]);
			System.out.println("number of chops "+nr);
			System.out.println("output rate "+output);

			Chopper opt1 = new Opt1Chopper(g);
			Chopper fpc = new FixedPointChopper(g);
			System.out.println("choppers initialized");

			// many
			LinkedList<ChopCrit> ctrir = createCriteria(g, nr, offset);
			System.out.println("criteria created: "+ctrir.size());
			compare(opt1, fpc, ctrir, output);
			System.out.println("********************************\n");
		}
	}

	private static LinkedList<ChopCrit> createCriteria(SDG g, int nr, int offset) {
		LinkedList<ChopCrit> result = new LinkedList<ChopCrit>();
		SDGNode[] nodes = g.vertexSet().toArray(new SDGNode[0]);

		long total = (long) nodes.length * (long) nodes.length;
		long step = (total - offset) / nr;
		long pos = offset;

		long ctr = 0;
		for (int i = 0; i < nr; i ++) {
			int s = (int) (pos / nodes.length);
			int t = (int) (pos % nodes.length);
			pos += step;

			ChopCrit c = new ChopCrit(nodes[s], nodes[t]);
			result.add(c);

			if ((ctr % (nr / 100)) == 0) System.out.print("-");
			ctr++;
		}
		System.out.println();
		return result;
	}

	private static void compare(Chopper c1, Chopper c2, LinkedList<ChopCrit> criteria, int output) {
		int diff = 0;
		int ctr = 0;
		int min = Integer.MAX_VALUE;
		ChopCrit minCrit = null;

		for (ChopCrit crit : criteria) {
			ctr++;

			Collection<SDGNode> chop1 = c1.chop(crit.source, crit.target);
			Collection<SDGNode> chop2 = c2.chop(crit.source, crit.target);

			if (ctr % (output * 1000) == 0) {
				System.out.println();
			} else if (ctr % (output * 100) == 0) {
				System.out.println(ctr);
			} else if (ctr % (output * 10) == 0) {
				System.out.print("+");
			} else if (ctr % output == 0) {
				System.out.print(".");
			}

			if (chop1.size() != chop2.size()) {
				diff++;
			}
		}

		System.out.println("\nDiffering chops: "+diff);
	}
}
