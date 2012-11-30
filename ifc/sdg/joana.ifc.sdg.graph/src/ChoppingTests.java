/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */

import java.util.Collection;
import java.util.LinkedList;
import java.util.TreeSet;

import edu.kit.joana.ifc.sdg.graph.PDGs;
import edu.kit.joana.ifc.sdg.graph.SDG;
import edu.kit.joana.ifc.sdg.graph.SDGEdge;
import edu.kit.joana.ifc.sdg.graph.SDGNode;
import edu.kit.joana.ifc.sdg.graph.chopper.Chopper;
import edu.kit.joana.ifc.sdg.graph.chopper.NonSameLevelChopper;
import edu.kit.joana.ifc.sdg.graph.chopper.RepsRosayChopper;
import edu.kit.joana.ifc.sdg.graph.chopper.RepsRosayChopperUnopt;
import edu.kit.joana.ifc.sdg.graph.chopper.conc.AlmostTimeSensitiveThreadChopper;
import edu.kit.joana.ifc.sdg.graph.chopper.conc.ContextSensitiveThreadChopper;
import edu.kit.joana.ifc.sdg.graph.chopper.conc.ThreadChopper;
import edu.kit.joana.ifc.sdg.graph.slicer.conc.nanda.experimental.Nanda;


@SuppressWarnings("unused")
public class ChoppingTests {
	public static void main (String[] args) throws Exception {
        int pdgID = Integer.parseInt(args[0]);
        int nr = Integer.parseInt(args[1]);
        int offset = Integer.parseInt(args[2]);
        int output = Integer.parseInt(args[3]);

        SDG g = SDG.readFrom(PDGs.pdgs[pdgID]);

        // XXX: uncomment in case you intend to chop sequential programs
//        LinkedList<SDGEdge> l = new LinkedList<SDGEdge>();
//        for (SDGEdge e : g.edgeSet()) {
//        	if (e.getKind().isThreadEdge()) l.add(e);
//        }
//        for (SDGEdge e : l) {
//        	g.removeEdge(e);
//        	if (e.getKind() == SDGEdge.Kind.FORK) g.addEdge(new SDGEdge(e.getSource(), e.getTarget(), SDGEdge.Kind.CALL));
//        }

        System.out.println("chopping "+PDGs.pdgs[pdgID]);
        System.out.println("number of chops "+nr);
        System.out.println("output rate "+output);

        Chopper[] array = createChopper(g);
        System.out.println("choppers initialized");

        // many
        LinkedList<ChopCrit> ctrir = createCriteria(g, nr, offset);
        System.out.println("criteria created: "+ctrir.size());
        String str = compare(array, ctrir, output);
        System.out.println(str);
//        System.out.println(ThreadChopper.time1);
//        System.out.println(ThreadChopper.time2);

        // single
//        ChopCrit c = new ChopCrit(g.getNode(3), g.getNode(1527));
//        System.out.println("criterion: "+c);
//        String str = compareSingle(array, c, output);
//        System.out.println(str);
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

    @SuppressWarnings("unchecked")
    private static String compare(Chopper[] chopper, LinkedList<ChopCrit> criteria, int output) {
        double[] size = new double[chopper.length];
        double[] time = new double[chopper.length];
        Collection<SDGNode>[] chops = new Collection[chopper.length];
        double[] sizeTMP = new double[chopper.length];
        double[] timeTMP = new double[chopper.length];

        int ctr = 0;
        int min = Integer.MAX_VALUE;
        ChopCrit minCrit = null;

        for (ChopCrit crit : criteria) {
            ctr++;

            for (int i = 0; i < chopper.length; i++) {
                long tmp = System.currentTimeMillis();
                chops[i] = chopper[i].chop(crit.source, crit.target);
                timeTMP[i] = System.currentTimeMillis() - tmp;
                sizeTMP[i] = chops[i].size();
                //System.out.println(chopper[i].getClass().getName()+": done"+" "+chop.size());
            }

            for (int i = 0; i < chopper.length; i++) {
            	time[i] += timeTMP[i];
                size[i] += sizeTMP[i];
            }

            if (ctr % (output * 1000) == 0) {
                System.out.println();
            } else if (ctr % (output * 100) == 0) {
                System.out.println(ctr);
            } else if (ctr % (output * 10) == 0) {
                System.out.print("+");
            } else if (ctr % output == 0) {
                System.out.print(".");
            }

            if (Math.abs(sizeTMP[chopper.length -1] - sizeTMP[chopper.length -2]) < min
            		&& Math.abs(sizeTMP[chopper.length -1] - sizeTMP[chopper.length -2]) > 0) {
            	min = (int) Math.abs(sizeTMP[chopper.length -1] - sizeTMP[chopper.length -2]);
            	minCrit = crit;
            }
        }

//        for (int i = 0; i < size.length; i++) {
//        	size[i] = size[i] / criteria.size();
//        	time[i] = time[i] / criteria.size();
//        }

        System.out.println("\nMinimal discrepancy");
        System.out.println(minCrit+" "+min);

        String str = "\n";
        for (int i = 0; i < chopper.length; i++) {
            str += chopper[i].getClass().getName()+": "+size[i]+"              time:"+time[i]+"\n";
        }

        return str;
    }

    private static String compareSingle(Chopper[] chopper, ChopCrit crit, int output) {
    	String str = "\n";

        for (int i = 0; i < chopper.length; i++) {
        	TreeSet<SDGNode> tmp = new TreeSet<SDGNode>(SDGNode.getIDComparator());
            tmp.addAll(chopper[i].chop(crit.source, crit.target));
            str += (chopper[i].getClass().getName()+": \n"+tmp+"\n\n");
        }

        return str;
    }

    private static Chopper[] createChopper(SDG g) {
        LinkedList<Chopper> l = new LinkedList<Chopper>();

        /* sequentiell */
//        l.add(new InsensitiveIntersectionChopper(g));
//        l.add(new IntersectionChopper(g));
//        l.add(new DoubleIntersectionChopper(g));
//        l.add(new Opt1Chopper(g));
//        l.add(new FixedPointChopper(g));
//        l.add(new RepsRosayChopperUnopt(g));
//        l.add(new NonSameLevelChopper(g));
//        l.add(new RepsRosayChopper(g));

        /* threads */
//        l.add(new VerySimpleThreadChopper(g));
//        l.add(new edu.kit.joana.ifc.sdg.graph.chopper.conc.SimpleThreadChopper(g));
//        l.add(new edu.kit.joana.ifc.sdg.graph.chopper.conc.FixedPointChopper(g));
        l.add(new ContextSensitiveThreadChopper(g));
//        l.add(new edu.kit.joana.ifc.sdg.graph.chopper.conc.ContextSensitiveThreadChopper2(g));
        l.add(new AlmostTimeSensitiveThreadChopper(g));
        l.add(new ThreadChopper(g));

        return l.toArray(new Chopper[0]);
    }
}
