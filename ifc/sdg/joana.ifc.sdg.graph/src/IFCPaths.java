/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.TreeSet;

import edu.kit.joana.ifc.sdg.graph.SDG;
import edu.kit.joana.ifc.sdg.graph.SDGNode;
import edu.kit.joana.ifc.sdg.graph.chopper.barrier.BarrierChopper;
import edu.kit.joana.ifc.sdg.graph.chopper.barrier.ContextSensitiveThreadBarrierChopper;
import edu.kit.joana.ifc.sdg.graph.chopper.barrier.IntraproceduralBarrierChopper;
import edu.kit.joana.ifc.sdg.graph.chopper.barrier.NonSameLevelBarrierChopper;
import edu.kit.joana.ifc.sdg.graph.chopper.barrier.SimpleThreadBarrierChopper;
import edu.kit.joana.ifc.sdg.graph.chopper.barrier.SummaryMergedBarrierChopper;
import edu.kit.joana.ifc.sdg.graph.chopper.barrier.TruncatedNonSameLevelBarrierChopper;



public class IFCPaths {
	public static void main(String[] args) throws Exception {
		SDG g = SDG.readFrom("/afs/info.uni-karlsruhe.de/user/giffhorn/Desktop/eclipse/runtime-New_configuration/Tests/jSDG/tests.TransitiveDeclass.pdg");

		HashSet<SDGNode> barrier = new HashSet<SDGNode>();
		barrier.add(g.getNode(8));
		barrier.add(g.getNode(26));

		 Collection<BarrierChopper> bc  = init(g);

		for (BarrierChopper chopper : bc) {
			chopper.setBarrier(barrier);
			Collection<SDGNode> chop = chopper.chop(g.getNode(7), g.getNode(52));
			TreeSet<SDGNode> sorted = new TreeSet<SDGNode>(SDGNode.getIDComparator());
			sorted.addAll(chop);
			System.out.println("********************************");
			System.out.println(chopper.getClass().getName()+":");
			System.out.println(sorted.size()+": "+sorted);
		}
	}

	public static Collection<BarrierChopper> init(SDG g) {
		LinkedList<BarrierChopper> l = new LinkedList<BarrierChopper>();
		{
			l.add(new IntraproceduralBarrierChopper(g));
			l.add(new SummaryMergedBarrierChopper(g));
			l.add(new TruncatedNonSameLevelBarrierChopper(g));
			l.add(new NonSameLevelBarrierChopper(g));
			l.add(new SimpleThreadBarrierChopper(g));
			l.add(new ContextSensitiveThreadBarrierChopper(g));
		}
		return l;
	}
}
