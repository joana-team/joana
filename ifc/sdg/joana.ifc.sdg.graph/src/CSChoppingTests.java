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
import edu.kit.joana.ifc.sdg.graph.SDGNode;
import edu.kit.joana.ifc.sdg.graph.chopper.Chopper;
import edu.kit.joana.ifc.sdg.graph.chopper.conc.ContextSensitiveThreadChopper;
import edu.kit.joana.ifc.sdg.graph.chopper.conc.VerySimpleThreadChopper;


public class CSChoppingTests {
    public static void main (String[] args) throws Exception {
        int pdgID = Integer.parseInt(args[0]);
        int nr = Integer.parseInt(args[1]);
        int offset = Integer.parseInt(args[2]);
        int output = Integer.parseInt(args[3]);

        SDG g = SDG.readFrom(PDGs.pdgs[pdgID]);

        System.out.println("chopping "+PDGs.pdgs[pdgID]);
        System.out.println("number of chops "+nr);
        System.out.println("output rate "+output);

        Chopper[] array = createChopper(g);
        System.out.println("choppers initialized");

        LinkedList<ChopCrit> ctrir = createCriteria(g, nr, offset);
        System.out.println("criteria created: "+ctrir.size());

        String str = compare(array, ctrir, output);
        System.out.println(str);
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
        }

        for (int i = 0; i < size.length; i++) {
        	size[i] = size[i] / criteria.size();
        	time[i] = time[i] / criteria.size();
        }


        String str = "\n";
        for (int i = 0; i < chopper.length; i++) {
            str += chopper[i].getClass().getName()+": "+size[i]+"              time:"+time[i]+"\n";
        }

        return str;
    }

    private static Chopper[] createChopper(SDG g) {
        LinkedList<Chopper> l = new LinkedList<Chopper>();

        // threads
        l.add(new VerySimpleThreadChopper(g));
        l.add(new edu.kit.joana.ifc.sdg.graph.chopper.conc.SimpleThreadChopper(g));
        l.add(new edu.kit.joana.ifc.sdg.graph.chopper.conc.FixedPointChopper(g));
        l.add(new ContextSensitiveThreadChopper(g));

        // sequentiell
//        InsensitiveIntersectionChopper c1 = new InsensitiveIntersectionChopper(g);
//        IntersectionChopper c2 = new IntersectionChopper(g);
//        DoubleIntersectionChopper c3 = new DoubleIntersectionChopper(g);
//        Opt1Chopper c4 = new Opt1Chopper(g);
//        FixedPointChopper c5 = new FixedPointChopper(g);
//        RepsRosayChopperUnopt c6 = new RepsRosayChopperUnopt(g);
//        NonSameLevelChopper c7 = new NonSameLevelChopper(g);
//        RepsRosayChopper c8 = new RepsRosayChopper(g);
//        l.add(c1);
//        l.add(c2);
//        l.add(c3);
//        l.add(c4);
//        l.add(c5);
//        l.add(c6);
//        l.add(c7);
//        l.add(c8);

        return l.toArray(new Chopper[0]);
    }
}
