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
import java.util.TreeSet;

import edu.kit.joana.ifc.sdg.graph.PDGs;
import edu.kit.joana.ifc.sdg.graph.SDG;
import edu.kit.joana.ifc.sdg.graph.SDGEdge;
import edu.kit.joana.ifc.sdg.graph.SDGNode;
import edu.kit.joana.ifc.sdg.graph.slicer.Slicer;
import edu.kit.joana.ifc.sdg.graph.slicer.conc.nanda.Nanda;
import edu.kit.joana.ifc.sdg.graph.slicer.conc.nanda.NandaFactory;


public class ConcSlicingTests {
	static SDG g;


//    public static void main (String[] args) throws Exception {
//    	// command line configuration
//    	String file = args[0];
//    	int algs = Integer.parseInt(args[1]);
//    	test(file, algs);
//    }

    public static void main (String[] args) throws Exception {
        // explicit configuration
//    	String file = "/afs/info.uni-karlsruhe.de/user/giffhorn/Desktop/eclipse/runtime-EclipseApplication/Tests/jSDG/conc.lg.LaplaceGrid.pdg";
    	String file = PDGs.pdgs[0];
//    	String file = PDGs.pdgs2[0];

        String i2p = "0";
        String sim = "0";
        String nan = "1";
        String nF  = "0";
        String nR  = "0";
        String nT  = "0";
        String nU  = "0";
        String nEx = "0";
        String dyn = "1";
        String kri = "0";
        String okr = "0";
        String str = okr+kri+dyn+nEx+nU+nT+nR+nF+nan+sim+i2p;

    	long algs = Long.parseLong(str);
    	test(file, algs);
    }

    @SuppressWarnings("deprecation")
	public static void test (String file, long algs) throws Exception {
        /* 1 */
        g = SDG.readFrom(file);


        System.out.println("initializing the slicers");
        LinkedList<Slicer> array = new LinkedList<Slicer>();

        if (algs % 2 == 1) {
        	array.addLast(new edu.kit.joana.ifc.sdg.graph.slicer.conc.I2PBackward(g));
//        	array.addLast(new edu.kit.joana.ifc.sdg.graph.slicer.conc.I2PForward(g));
        }
        algs = algs / 10;

        if (algs % 2 == 1) {
//        	array.addLast(new edu.kit.joana.ifc.sdg.graph.slicer.conc.NandaI2PBackward(g));
        	array.addLast(new edu.kit.joana.ifc.sdg.graph.slicer.conc.simple.SimpleConcurrentSlicer(g));
        }
        algs = algs / 10;

        if (algs % 2 == 1) {
        	array.addLast(NandaFactory.createNandaBackward(g));
//        	array.addLast(NandaFactory.createNandaForward(g));
        }
        algs = algs / 10;

        if (algs % 2 == 1) {
        	array.addLast(edu.kit.joana.ifc.sdg.graph.slicer.conc.nanda.experimental.NandaFactory.createNandaFlatBackward(g));
//        	array.addLast(edu.kit.joana.ifc.sdg.graph.slicer.conc.nanda.experimental.NandaFactory.createNandaFlatForward(g));
        }
        algs = algs / 10;

        if (algs % 2 == 1) {
        	array.addLast(edu.kit.joana.ifc.sdg.graph.slicer.conc.nanda.experimental.NandaFactory.createNandaReachBackward(g));
        }
        algs = algs / 10;

        if (algs % 2 == 1) {
        	array.addLast(edu.kit.joana.ifc.sdg.graph.slicer.conc.nanda.regions.NandaFactory.createNandaBackward(g));
//        	array.addLast(NandaFactory.createNandaThreadRegionsForward(g));
        }
        algs = algs / 10;

        if (algs % 2 == 1) {
//        	array.addLast(edu.kit.joana.ifc.sdg.graph.slicer.conc.nanda.regions.NandaFactory.createNandaOriginalBackward(g));
//        	array.addLast(edu.kit.joana.ifc.sdg.graph.slicer.conc.nanda.experimental.NandaFactory.createNandaForward(g));
        	array.addLast(edu.kit.joana.ifc.sdg.graph.slicer.conc.nanda.experimental.NandaFactory.createNandaBackwardTest(g));
        }
        algs = algs / 10;

        if (algs % 2 == 1) {
        	array.addLast(edu.kit.joana.ifc.sdg.graph.slicer.conc.nanda.experimental.NandaFactory.createNandaBackward(g));
//        	array.addLast(edu.kit.joana.ifc.sdg.graph.slicer.conc.nanda.experimental.NandaFactory.createNandaForward(g));
        }
        algs = algs / 10;

        if (algs % 2 == 1) {
        	array.addLast(new edu.kit.joana.ifc.sdg.graph.slicer.conc.dynamic.krinke.Slicer(g));
        }
        algs = algs / 10;

        if (algs % 2 == 1) {
        	array.addLast(new edu.kit.joana.ifc.sdg.graph.slicer.conc.krinke.Krinke(g));
        }
        algs = algs / 10;

        if (algs % 2 == 1) {
        	array.addLast(new edu.kit.joana.ifc.sdg.graph.slicer.conc.krinke.OptimizedKrinke(g));
        }

        System.out.println(file);
        Collection<SDGNode> crits = g.getNNodes(100, 15);
        System.out.println("criteria: "+crits.size());

//        String str = compare(array, crits);
        String str = compareOne(array, g.getNode(100));

        System.out.println(str);
        System.out.println(Nanda.elems);
    }

    @SuppressWarnings({ "unchecked", "unused" })
    private static String compare(List<Slicer> slicer, Collection<SDGNode> criteria) {
        int[] size = new int[slicer.size()];
        long[] time = new long[slicer.size()];
        Collection<SDGNode>[] slices = new Collection[slicer.size()];
        int s = 0; int diff = Integer.MAX_VALUE;

        int ctr = 0;

        for (SDGNode crit : criteria) {
            ctr++;

//            System.out.println("Slice for node "+crit);
            try {
	            for (int i = 0; i < slicer.size(); i++) {
	                long tmp = System.currentTimeMillis();
	                slices[i] = slicer.get(i).slice(Collections.singleton(crit));
	                time[i] += System.currentTimeMillis() - tmp;
	                size[i] += slices[i].size();
	//                TreeSet<SDGNode> ordered = new TreeSet<SDGNode>(SDGNode.getIDComparator());
	//                ordered.addAll(slices[i]);
	//                System.out.println(slicer[i].getClass().getName()+" done: "+ordered);
	//                if (i == 3 && slices[i].size() != slices[i-1].size()) {
	//                    System.out.println("Slice for node "+crit);
	//                    System.out.println(slicer[i-1].getClass().getName()+" done: "+slices[i-1].size());
	//                    System.out.println(slicer[i].getClass().getName()+" done: "+slices[i].size());
	//
	//                    TreeSet<SDGNode> s = new TreeSet<SDGNode>(SDGNode.getIDComparator());
	//                    s.addAll(slices[i]);
	//                    System.out.println(slicer[i].getClass().getName()+" done: "+s);
	//                    s.addAll(slices[i-1]);
	//                    s.removeAll(slices[i]);
	//                    System.out.println("missing: "+s);
	//                }

	                if (i == 1 && slices[0].size() != slices[1].size() && Math.abs(slices[0].size()-slices[1].size()) < diff) {
	                	diff = Math.abs(slices[0].size()- slices[1].size());
	                	s = crit.getId();
	                }
	            }
            } catch(RuntimeException e) {
            	e.printStackTrace();
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

    private static String compareOne(List<Slicer> slicer, SDGNode crit) {
    	if (slicer.size() != 2) throw new RuntimeException("Works with two slicers only!");
    	System.out.println("Slice for node "+crit);

        @SuppressWarnings("unchecked")
		final Collection<SDGNode>[] slices = (Collection<SDGNode>[]) new Collection[slicer.size()];

        for (int i = 0; i < slicer.size(); i++) {
            slices[i] = slicer.get(i).slice(Collections.singleton(crit));

            TreeSet<SDGNode> ordered = new TreeSet<SDGNode>(SDGNode.getIDComparator());
            ordered.addAll(slices[i]);
            System.out.println(slicer.get(i).getClass().getName()+" done: "+ordered);
        }

        System.out.println(slicer.get(0).getClass().getName()+" size: "+slices[0].size());
        System.out.println(slicer.get(1).getClass().getName()+" size: "+slices[1].size());

        int j = (slices[0].size() > slices[1].size() ? 0 : 1);

        TreeSet<SDGNode> s = new TreeSet<SDGNode>(SDGNode.getIDComparator());
        s.addAll(slices[j]);
        s.removeAll(slices[1-j]);
        System.out.println("missing: "+s);

        // backward
//        for (SDGNode miss : s) {
//        	for (SDGEdge e : g.outgoingEdgesOf(miss)) {
//        		if (slices[0].contains(e.getTarget()) && slices[1].contains(e.getTarget())) {
//        			System.out.println(e);
//        		}
//        	}
//        }

        // forward
        for (SDGNode miss : s) {
        	for (SDGEdge e : g.incomingEdgesOf(miss)) {
        		if (slices[0].contains(e.getSource()) && slices[1].contains(e.getSource())) {
        			System.out.println(e);
        		}
        	}
        }

        String str = "\n";

        return str;
    }
}
