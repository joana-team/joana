/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.ifc.sdg.graph.slicer;

import java.math.BigInteger;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;

import edu.kit.joana.ifc.sdg.graph.SDG;
import edu.kit.joana.ifc.sdg.graph.SDGEdge;
import edu.kit.joana.ifc.sdg.graph.SDGNode;
import edu.kit.joana.util.Log;
import edu.kit.joana.util.Logger;


/**
 * Implementation of the standard 2-phase slicer.
 *
 * -- Created on September 6, 2005
 *
 * @author  Christian Hammer, Dennis Giffhorn
 */
public abstract class SummarySlicer implements Slicer {

	private final Logger debug = Log.getLogger(Log.L_SDG_GRAPH_DEBUG);
    protected Set<SDGEdge.Kind> omittedEdges = SDGEdge.Kind.threadEdges();
    protected SDG g;

    public interface EdgePredicate {
        public boolean phase1();
        public boolean follow(SDGEdge e);
        public boolean saveInOtherWorklist(SDGEdge e);
    }

    /**
     * Creates a new instance of SummarySlicer
     */
    public SummarySlicer(SDG graph, Set<SDGEdge.Kind> omit) {
        this.g = graph;
        this.omittedEdges = omit;
    }

    public SummarySlicer(SDG graph) {
        this.g = graph;
    }

    public void addToOmit(SDGEdge.Kind kind) {
    	this.omittedEdges.add(kind);
    }

    public void setGraph(SDG graph) {
        g = graph;
    }

    public Collection<SDGNode> slice(SDGNode criterion) {
    	return slice(Collections.singleton(criterion));
    }

    public Collection<SDGNode> slice(Collection<SDGNode> criteria) {
        Map<SDGNode, SDGNode> slice = new HashMap<SDGNode, SDGNode>();
        LinkedList<SDGNode> worklist = new LinkedList<SDGNode>();
        LinkedList<SDGNode> nextWorklist = new LinkedList<SDGNode>();
        EdgePredicate p = phase1Predicate();

        worklist.addAll(criteria);

        for (SDGNode v : criteria) {
            slice.put(v, phase1Predicate().phase1() ? v : null);
        }

        while (!worklist.isEmpty()) {

            while (!worklist.isEmpty()) {
                SDGNode w = worklist.poll();

                for (SDGEdge e : edgesToTraverse(w)) {

                    if (!e.getKind().isSDGEdge() ||
                            omittedEdges.contains(e.getKind())) {

                        continue;
                    }

                    SDGNode v = reachedNode(e);

                    if (!slice.containsKey(v) ||
                            (p.phase1() &&
                            slice.get(v) == null)) {

                        // if node was not yet added or node was added in phase2
                        if (p.saveInOtherWorklist(e)) {

                            debug.outln("OTHER\t" + e);
                            nextWorklist.add(v);
                            slice.put(v, p.phase1() ? v : null);

                        } else if (p.follow(e)) {

                        	debug.outln("FOLLOW\t" + e);
                            worklist.add(v);
                            slice.put(v, p.phase1() ? v : null);
                        }
                    }
                }
            }

            // swap worklists and predicates
            debug.outln("swap");

            worklist = nextWorklist;
            p =  phase2Predicate();
        }

        return slice.keySet();
    }

    public Collection<SDGNode> subgraphSlice(Collection<SDGNode> criteria, Collection<SDGNode> sub) {
        Map<SDGNode, SDGNode> slice = new HashMap<SDGNode, SDGNode>();
        LinkedList<SDGNode> worklist = new LinkedList<SDGNode>();
        LinkedList<SDGNode> nextWorklist = new LinkedList<SDGNode>();
        EdgePredicate p = phase1Predicate();

        for (SDGNode v : criteria) {
            if (sub.contains(v)) {
                worklist.add(v);
                slice.put(v, phase1Predicate().phase1() ? v : null);
            }
        }

        while (!worklist.isEmpty()) {
            while (!worklist.isEmpty()) {
                SDGNode w = worklist.poll();

                for (SDGEdge e : edgesToTraverse(w)) {

                    if (!e.getKind().isSDGEdge() ||
                            omittedEdges.contains(e.getKind())) {

                        continue;
                    }

                    SDGNode v = reachedNode(e);

                    if (sub.contains(v) &&
                            (!slice.containsKey(v) || (p.phase1() && slice.get(v) == null))) {

                        // if node was not yet added or node was added in phase2
                        if (p.saveInOtherWorklist(e)) {

                        	debug.outln("OTHER\t" + e);
                            nextWorklist.add(v);
                            slice.put(v, p.phase1() ? v : null);

                        } else if (p.follow(e)) {

                        	debug.outln("FOLLOW\t" + e);
                            worklist.add(v);
                            slice.put(v, p.phase1() ? v : null);
                        }
                    }
                }
            }

            // swap worklists and predicates
            debug.outln("swap");

            worklist = nextWorklist;
            p =  phase2Predicate();
        }

        return slice.keySet();
    }

    public void setOmittedEdges(Set<SDGEdge.Kind> omit){
        this.omittedEdges = omit;
    }

    protected abstract Collection<SDGEdge> edgesToTraverse(SDGNode node);

    protected abstract SDGNode reachedNode(SDGEdge edge);

    protected abstract EdgePredicate phase1Predicate();

    protected abstract EdgePredicate phase2Predicate();


    public static void summarySliceAll(SDG sdg) {
    	sliceAll(sdg, new SummarySlicerBackward(sdg));
    }

    public static void sliceAll(SDG sdg, Slicer slicer) {
    	StringBuilder slicerName = new StringBuilder(slicer.getClass().getSimpleName());
    	int j = 0;
    	for (int i = 0; i < slicerName.length(); i++) {
				char c = slicerName.charAt(i);
				if (Character.isUpperCase(c))
					slicerName.setCharAt(j++, c);
			}
    	slicerName.setLength(j);
      int criterion_count = 0;
      int sdg_size = 0;
      BigInteger sum_size = BigInteger.ZERO;
      int max_size = 0;
      double max_slice_time = 0;

      //Timer timer, slice_timer;

      for (SDGNode n : sdg.vertexSet()) {
        if (n.getKind() != SDGNode.Kind.FOLDED && !n.isParameter()) {
          ++ sdg_size;
        }
      }

//      timer.start();

//      Slicer slicer = new SummarySlicerBackward(sdg);

      for (SDGNode n : sdg.vertexSet()) {
//        if (n.getKind() == SDGNode.Kind.FORMAL_IN) {
        if (isCriterion(n)) {
          ++criterion_count;
          Collection<SDGNode> criterion = Collections.singleton(n);
//          criterion.push_back(n);
          System.out.print("slice " + criterion_count + " @" + n + ": ");
//          slice_timer.start();
          Collection<SDGNode> result = slicer.slice(criterion/*, true*/);
//          Collection result1 = new SDGSlicer(sdg, criterion).slice();
//          assert result1.size() == result.size() && result.containsAll(result1) :
//          	"help size " + result.size() + " size1 " + result1.size() + " result\n" +
//          	removeDuplicates(result, result1) + "\n" + result1;
//          slice_timer.stop();
//          if (slice_timer.elapsed() > max_slice_time)
//          	max_slice_time = slice_timer.elapsed();
//          cout.precision(2);
          for (Iterator<SDGNode> i = result.iterator(); i.hasNext();) {
						SDGNode node = i.next();
						if (node.isParameter())
							i.remove();
					}
          System.out.println(" size " + result.size() + " - " + ((100 * result.size() / sdg_size))
          		+ "%");
          if (result.size() > max_size)
          	max_size = result.size();
          sum_size = sum_size.add(BigInteger.valueOf(result.size()));
        }
      }
//      timer.stop();
      if (criterion_count > 0) {
        BigInteger average_size = sum_size.divide(BigInteger.valueOf(criterion_count));
        BigInteger average = average_size.multiply(BigInteger.valueOf(100)).divide(BigInteger.valueOf(sdg_size));
//        clog.precision(2);
        System.out.println(sdg.getName() + /*setw(10-sdg.getName().length()) +*/ " (" + slicerName + "): " /*+ setw(5)*/ //<< timer.elapsed()
        		+ " " + criterion_count + " sl. avgsize " + average_size+ " " + average
        		+ "%/" + sdg_size + ", max " + max_slice_time+ " size " + max_size);
      } else {
//        clog.precision(2);
        System.out.println(sdg.getName() + /*setw(10-sdg.getName().length()) +*/ " (" + slicerName + "): no slices");
      }
      //javacard.framework.JCMainPurse (SIS):  42546 sl. avgsize 0 0%/188519, max 0.0 size 105531
    }

		private static boolean isCriterion(SDGNode n) {
			return n.getKind() != SDGNode.Kind.FOLDED && !n.isParameter(); //n.getKind() == SDGNode.Kind.EXPRESSION;
		}

    @SuppressWarnings("unused")
    private static <E> Collection<E> removeDuplicates(Collection<E> from, Collection<E> c) {
    	Collection<E> n = new LinkedHashSet<E>(from);
    	n.retainAll(c);
    	from.removeAll(n);
    	c.removeAll(n);
    	return from;
    }

    public static void sliceAll(SDG sdg) {
    	sliceAll(sdg, new ContextInsensitiveBackward(sdg));
    }
}
