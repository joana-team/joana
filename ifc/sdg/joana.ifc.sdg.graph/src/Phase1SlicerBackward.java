/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */



import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import edu.kit.joana.ifc.sdg.graph.PDGs;
import edu.kit.joana.ifc.sdg.graph.SDG;
import edu.kit.joana.ifc.sdg.graph.SDGEdge;
import edu.kit.joana.ifc.sdg.graph.SDGNode;
import edu.kit.joana.ifc.sdg.graph.slicer.Slicer;
import edu.kit.joana.ifc.sdg.graph.slicer.SummarySlicer;
import edu.kit.joana.ifc.sdg.graph.slicer.SummarySlicerBackward;
import edu.kit.joana.ifc.sdg.graph.slicer.graph.threads.ThreadsInformation;
import edu.kit.joana.ifc.sdg.graph.slicer.graph.threads.ThreadsInformation.ThreadInstance;


/**
 * -- Created on September 6, 2005
 *
 * @author  Dennis Giffhorn
 */
public class Phase1SlicerBackward extends SummarySlicer {

    /**
     * Creates a new instance of SummarySlicerBackward
     */
    public Phase1SlicerBackward(SDG graph, Set<SDGEdge.Kind> omit) {
        super(graph, omit);
    }

    public Phase1SlicerBackward(SDG graph) {
        super(graph);
    }

    protected Collection<SDGEdge> edgesToTraverse(SDGNode node) {
        return this.g.incomingEdgesOf(node);
    }

    protected SDGNode reachedNode(SDGEdge edge) {
        return edge.getSource();
    }

    protected EdgePredicate phase1Predicate() {
        return new EdgePredicate() {
            public boolean phase1() {
                return true;
            }

            public boolean follow(SDGEdge e) {
                return !omittedEdges.contains(e.getKind());
            }

            public boolean saveInOtherWorklist(SDGEdge e) {
                return e.getKind() == SDGEdge.Kind.PARAMETER_OUT;
            }

            public String toString() {
                return "phase 1";
            }
        };
    }

    protected EdgePredicate phase2Predicate() {
        return new EdgePredicate() {
            public boolean phase1() {
                return false;
            }

            public boolean follow(SDGEdge e) {
                return false;
            }

            public boolean saveInOtherWorklist(SDGEdge e) {
                return false;
            }

            public String toString() {
                return "phase 2";
            }
        };
    }


	static SDG g;

    public static void main (String[] args) throws Exception {
        // explicit configuration
    	for (int i = 0; i < PDGs.pdgs.length; i++) {
	    	String file = PDGs.pdgs[i];
	        /* 1 */
	        g = SDG.readFrom(file);
	        ThreadInstance ti = g.getThreadsInfo().iterator().next();
	        LinkedList<ThreadInstance> l = new LinkedList<ThreadInstance>();
	        l.add(ti);
	        ThreadsInformation info = new ThreadsInformation(l);
	        g.setThreadsInfo(info);
	        int[] threads = new int[] {0};
	        for (SDGNode n : g.vertexSet()) {
	        	n.setThreadNumbers(threads);
	        }

	        System.out.println("initializing the slicers");
	        LinkedList<Slicer> array = new LinkedList<Slicer>();

	    	array.addLast(new SummarySlicerBackward(g));
	    	array.addLast(new Phase1SlicerBackward(g));


	        System.out.println(file);
	        System.out.println("criteria: "+g.vertexSet().size());

	        String str = compare(array, g.vertexSet());

	        System.out.println(str);
    	}
    }

    @SuppressWarnings("unchecked")
    private static String compare(List<Slicer> slicer, Collection<SDGNode> criteria) {
        int[] size = new int[slicer.size()];
        long[] time = new long[slicer.size()];
        Collection<SDGNode>[] slices = new Collection[slicer.size()];
        int s = 0; int diff = Integer.MAX_VALUE;

        int ctr = 0;

        for (SDGNode crit : criteria) {
            ctr++;
            if ((ctr % 1) != 0) continue;

            for (int i = 0; i < slicer.size(); i++) {
                long tmp = System.currentTimeMillis();
                slices[i] = slicer.get(i).slice(Collections.singleton(crit));
                time[i] += System.currentTimeMillis() - tmp;
                size[i] += slices[i].size();

                if (i == 1 && slices[0].size() != slices[1].size() && Math.abs(slices[0].size()-slices[1].size()) < diff) {
                	diff = Math.abs(slices[0].size()- slices[1].size());
                	s = crit.getId();
                }
            }
//            System.out.println("************************************** ");
            if (ctr % 10 == 0) {
                System.out.print(".");
            }
            if (ctr % 100 == 0) {
                System.out.print(ctr);
            }
            if (ctr % 1000 == 0) {
                System.out.println();
            }
        }

        String str = "\n";
        for (int i = 0; i < slicer.size(); i++) {
            str += slicer.get(i).getClass().getName()+": "+size[i]+"              time:"+time[i]+"\n";
        }

        str += s+": "+diff+"\n";

        return str;
    }
}
