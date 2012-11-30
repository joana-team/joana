/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.ifc.sdg.graph.chopper;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.TreeSet;

import edu.kit.joana.ifc.sdg.graph.SDG;
import edu.kit.joana.ifc.sdg.graph.SDGEdge;
import edu.kit.joana.ifc.sdg.graph.SDGNode;
import edu.kit.joana.ifc.sdg.graph.SDGNodeTuple;


/**
 * This is our abstract class for chopping algorithms.
 * It serves the following purposes:
 * - makes chopping algorithms exchangeable,
 * - <code>setGraph</code> provides a call-back machinery to re-initialize choppers to work on new SDGs,
 * - provides convenience methods <code>chop(SDGNode source, SDGNode sink)</chop> and
 *   <code>chop(ChoppingCriterion criterion)</code>, which fall back to the abstract method
 *   <code>chop(Collection<SDGNode> sourceSet, Collection<SDGNode> sinkSet)</code>,
 * - offers routines for
 *   -- sorting chops
 *   -- testing if chopping criteria are same-level
 *   -- collecting summary edges in set of nodes
 *
 * @author Dennis Giffhorn
 */
public abstract class Chopper {
	protected SDG sdg;

	/**
	 * Calls <code>setGraph</code> for initialization.
	 *
	 * @param g  Depends on the concrete chopping algorithm, whether null or cSDGs are allowed.
	 */
	protected Chopper(SDG g) {
		setGraph(g);
	}

    /**
     * Stores g in the <code>sdg</code> attribute and calls <code>onSetGraph</code>.
     */
    public final void setGraph(SDG g) {
    	sdg = g;
    	onSetGraph();
    }

    /**
     * Is called at the end of setGraph and can be used for custom initializations.
     * At the time of this call-back, attribute <code>sdg</code> has been initialized and can be accessed.
     */
    protected abstract void onSetGraph();

    /**
     * Computes a chop between <code>sourceSet</code> and <code>sinkSet</code>.
     *
     * @param sourceSet   The source of the chop.
     * @param sinkSet     The sink of the chop.
     * @return            The chop, mostly a HashSet.
     */
    public abstract Collection<SDGNode> chop(Collection<SDGNode> sourceSet, Collection<SDGNode> sinkSet);

    /**
     * Convenience method computing a chop between a source node and a sink node.
     * Calls <code>chop(Collection<SDGNode> sourceSet, Collection<SDGNode> sinkSet)</code>
     *
     * @param source   The source criterion.
     * @param sink     The sink criterion.
     * @return         The chop.
     */
    public final Collection<SDGNode> chop(SDGNode source, SDGNode sink) {
        return chop(Collections.singleton(source), Collections.singleton(sink));
    }

    /** Convenience method encapsulating the chopping criterion.
     * Calls <code>chop(Collection<SDGNode> sourceSet, Collection<SDGNode> sinkSet)</code>
     *
     * @param criterion   The chopping criterion.
     * @return            The chop.
     */
	public final Collection<SDGNode> chop(ChoppingCriterion criterion) {
		return chop(criterion.getSourceSet(), criterion.getTargetSet());
	}


	/* *************************************************** */
	/* routines for collecting the summary edges in a chop */
	/* *************************************************** */

	/**
	 * Represents a distinct call site.
	 * Stores a call node and the ID of the called procedure.
	 * Necessary because due to dynamic dispatch a call site can call a set of possible procedures.
	 *
	 * Implements <code>hashCode</code> and <code>equals</code> consistent with the Java coding conventions.
	 */
	static class CallEntry {
    	SDGNode call;
    	int calledProcID;

    	CallEntry(SDGNode c, int i) {
    		call = c;
    		calledProcID = i;
    	}

    	public boolean equals(Object o) {
    		if (o instanceof CallEntry) {
    			CallEntry p = (CallEntry) o;
    			return call == p.call && calledProcID == p.calledProcID;
    		}
    		return false;
    	}

    	public int hashCode() {
    		return call.getId() *31 + calledProcID;
    	}
    }

    /**
     * Collects all summary edges in the given chop and returns the corresponding ({formal-in, entry} / formal-out) pairs.
     * Essential auxiliary procedure for extending truncated chops to non-truncated chops.
     *
     * @param chop   The chop.
     * @return       The ({formal-in, entry} / formal-out) pairs corresponding to the summary edges in the chop.
     *               The first node of each SDGNodeTuple is a {formal-in, entry} node, the second node is a formal-out node.
     */
    protected final Collection<SDGNodeTuple> getSummaryEdgePairs(Collection<SDGNode> chop) {
        Collection<SDGNodeTuple> sePairs = new HashSet<SDGNodeTuple>();

        for (SDGNode actOut : chop) {
        	if (actOut.getKind() == SDGNode.Kind.ACTUAL_OUT) {
        		// act-out node found, now search in the chop for act-in nodes connected via summary edges
            	for (SDGNode actIn : allIncomingActIns(actOut)) {
            		if (chop.contains(actIn)) {
            			// retrieve the corresponding formal parameter nodes
            			sePairs.addAll(sdg.getAllFormalPairs(actIn, actOut));
            		}
            	}
            }
        }

        return sePairs;
    }

    /**
     * Collects all actual-in nodes connected with the given actual-out node via summary edges.
     * The result also contains the call node, since those have to be treated like actual-in nodes by the chopping algorithms.
     * Essential auxiliary procedure for extending truncated chops to non-truncated chops.
     *
     * @param actOut  An actual-out node (kind is not checked).
     * @return        All actual-in- and call nodes connected with <code>actOut</code> in the actual-parameters graph.
     */
    private Collection<SDGNode> allIncomingActIns(SDGNode actOut) {
    	LinkedList<SDGNode> result = new LinkedList<SDGNode>();
    	LinkedList<SDGNode> w = new LinkedList<SDGNode>();
    	HashSet<SDGNode> visited = new HashSet<SDGNode>();

    	w.add(actOut);
    	visited.add(actOut);

    	while (!w.isEmpty()) {
    		SDGNode next = w.poll();

    		for (SDGEdge e : sdg.incomingEdgesOf(next)) {
				// climb the actual-parameters graph
    			if (e.getKind() == SDGEdge.Kind.CONTROL_DEP_EXPR || e.getKind() == SDGEdge.Kind.SUMMARY) {
    				SDGNode n = e.getSource();

    				if (visited.add(n)) {
    					w.add(n);
    				}

    				// reaching formal-in or call nodes
    				if (n.getKind() == SDGNode.Kind.CALL || n.getKind() == SDGNode.Kind.ACTUAL_IN) {
    					result.add(n);
    				}
    			}
        	}
    	}

    	return result;
    }

    /**
     * Collects for each call site in the given chop all ({actual-in, call) / actual-out)-pairs connected via summary edges
     * and maps them to the corresponding {formal-in, entry} and formal-out nodes.
     * For each of these call sites a Criterion object is created, whose source consist of the {formal-in, entry} nodes and
     * whose sink consists of the formal-out nodes.
     *
     * @param chop  The chop.
     * @return      the Criterion objects, in a Collection without a specific order.
     */
    protected final Collection<Criterion> getSummarySites(Collection<SDGNode> chop) {
    	HashMap<SDGNode, Criterion> criteria = new HashMap<SDGNode, Criterion>();

    	// collect the call sites in the chop -
    	// bundle all call- actual-in- and actual-out nodes that belong together
    	for (SDGNode n : chop) {
    		if (n.getKind() == SDGNode.Kind.CALL) {
    			Criterion c = criteria.get(n);

    			if (c == null) {
    				c = new Criterion(n);
    				criteria.put(n, c);
    			}

    			c.source.add(n);

    		} else if (n.getKind() == SDGNode.Kind.ACTUAL_IN) {
    			SDGNode call = sdg.getCallSiteFor(n);
    			Criterion c = criteria.get(call);

    			if (c == null) {
    				c = new Criterion(call);
    				criteria.put(call, c);
    			}

    			c.source.add(n);

    		} else if (n.getKind() == SDGNode.Kind.ACTUAL_OUT) {
    			SDGNode call = sdg.getCallSiteFor(n);
    			Criterion c = criteria.get(call);

    			if (c == null) {
    				c = new Criterion(call);
    				criteria.put(call, c);
    			}

    			c.target.add(n);
    		}
    	}

    	//    System.out.println("call sites "+criteria.values());

    	// take the collected call sites and map them to their associated formal parameter nodes
    	HashMap<CallEntry, Criterion> result = new HashMap<CallEntry, Criterion>();
    	for (Criterion c : criteria.values()) {
    		// the call site was not taken
    		// this happens if an actual-in or actual-out node is part of the chopping criterion - skip it
    		if (c.source.isEmpty() || c.target.isEmpty()) continue;

    		/* map the actual parameters and the call nodes to the formal parameters and the entry nodes */

    		for (SDGEdge e : sdg.outgoingEdgesOf(c.entry)) {
    			if (e.getKind() == SDGEdge.Kind.CALL || e.getKind() == SDGEdge.Kind.FORK) {
    				SDGNode entry = e.getTarget();
    				CallEntry key = new CallEntry(c.entry, entry.getProc());
    				Criterion d = new Criterion(c.entry);
    				result.put(key, d);
    			}
    		}

    		for (SDGNode n : c.source) {
    			if (n.getKind() == SDGNode.Kind.ACTUAL_IN) {
    				for (SDGEdge e : sdg.outgoingEdgesOf(n)) {
    					if (e.getKind() == SDGEdge.Kind.PARAMETER_IN || e.getKind() == SDGEdge.Kind.FORK_IN) {
    						SDGNode formIn = e.getTarget();
    						CallEntry key = new CallEntry(c.entry, formIn.getProc());

    						Criterion d = result.get(key);

    						if (d != null) {
    							d.source.add(formIn);

    						} else {
    							throw new RuntimeException("kein criterion fuer ("+n +", "+formIn+")");
    						}
    					}
    				}

    			} else if (n.getKind() == SDGNode.Kind.CALL) {
    				for (SDGEdge e : sdg.outgoingEdgesOf(n)) {
    					if (e.getKind() == SDGEdge.Kind.CALL || e.getKind() == SDGEdge.Kind.FORK) {
    						SDGNode entry = e.getTarget();
    						CallEntry key = new CallEntry(c.entry, entry.getProc());

    						Criterion d = result.get(key);

    						if (d != null) {
    							d.source.add(entry);

    						} else {
    							throw new RuntimeException("kein criterion fuer ("+n +", "+entry+")");
    						}
    					}
    				}
    			}
    		}

    		for (SDGNode n : c.target) {
    			if (n.getKind() == SDGNode.Kind.ACTUAL_OUT) {
    				for (SDGEdge e : sdg.incomingEdgesOf(n)) {
    					if (e.getKind() == SDGEdge.Kind.PARAMETER_OUT || e.getKind() == SDGEdge.Kind.FORK_OUT) {
    						SDGNode formOut = e.getSource();
    						CallEntry key = new CallEntry(c.entry, formOut.getProc());

    						Criterion d = result.get(key);

    						if (d != null) {
    							d.target.add(formOut);

    						} else {
    							throw new RuntimeException("kein criterion fuer ("+n +", "+formOut+") "+c.entry+" "+formOut.getProc());
    						}
    					}
    				}
    			}
    		}
    	}

    	return result.values();
    }



    /* ****************************** */
    /* testing of same-level criteria */
	/* ****************************** */

    /**
     * Tests if all the given nodes stem from the same procedure.
     *
     * @param sourceSet The source criterion set. Must not be null.
     * @param sinkSet The target criterion set. Must not be null.
     * @return `true' if all the given nodes stem from the same procedure.
     * @throws InvalidCriterionException, if one of the sets is empty.
     */
    public static boolean testSameLevelSetCriteria(Collection<SDGNode> sourceSet, Collection<SDGNode> sinkSet)
    throws InvalidCriterionException{
        // sets must not be empty
        if (sourceSet.isEmpty())
            throw new InvalidCriterionException("source criterion is empty");
        if (sinkSet.isEmpty())
            throw new InvalidCriterionException("sink criterion is empty");

        // same-level?
        Iterator<SDGNode> i = sourceSet.iterator();
        int chopProcedure = i.next().getProc(); // take the proc. ID of an arbitrary node in the source set

        for (SDGNode n : sourceSet) {
            if (n.getProc() != chopProcedure) {
            	return false;
            }
        }

        for (SDGNode n : sinkSet) {
            if (n.getProc() != chopProcedure) {
            	return false;
            }
        }

        return true;
    }


    /* ************* */
    /* sorting chops *
    /* ************* */

    /**
     * Sorts a chop in ascending order of SDGNode IDs.
     * Can be used for all SDGNode collections, of course.
     */
    public static TreeSet<SDGNode> sortChop(Collection<SDGNode> chop) {
    	TreeSet<SDGNode> set = new TreeSet<SDGNode>(SDGNode.getIDComparator());
    	set.addAll(chop);
    	return set;
    }
}
