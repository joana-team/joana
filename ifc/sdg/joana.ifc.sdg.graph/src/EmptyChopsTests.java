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
import edu.kit.joana.ifc.sdg.graph.chopper.conc.VerySimpleThreadChopper;


@SuppressWarnings("unused")
public class EmptyChopsTests {
    public static void main (String[] args) throws Exception {
        int pdgID = 8;
        int nr = 10000;
        int offset = 25;
        int output = 10;

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
        int[] size = new int[chopper.length];
        Collection<SDGNode>[] chops = new Collection[chopper.length];

        int ctr = 0;

        for (ChopCrit crit : criteria) {
            ctr++;

            for (int i = 0; i < chopper.length; i++) {
                chops[i] = chopper[i].chop(crit.source, crit.target);
                if (chops[i].isEmpty()) size[i]++;
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

        String str = "\n";
        for (int i = 0; i < chopper.length; i++) {
            str += chopper[i].getClass().getName()+": "+size[i]+"\n";
        }

        return str;
    }

    private static Chopper[] createChopper(SDG g) {
        LinkedList<Chopper> l = new LinkedList<Chopper>();

        // threads
        l.add(new VerySimpleThreadChopper(g));
        l.add(new edu.kit.joana.ifc.sdg.graph.chopper.conc.FixedPointChopper(g));
//        l.add(new ThreadChopperE(g));

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
