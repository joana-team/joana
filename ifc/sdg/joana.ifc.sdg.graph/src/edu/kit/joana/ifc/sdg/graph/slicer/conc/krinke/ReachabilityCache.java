/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.ifc.sdg.graph.slicer.conc.krinke;

import java.util.HashMap;

import edu.kit.joana.ifc.sdg.graph.slicer.graph.Context;

/** A cache for optimising the reachability computations.
 * After the reachability of two contexts is computed, the result together with
 * the contexts are saved in the cache.
 * If reachability of two contexts shall be computed, the cache is checked first
 * whether the result for this contexts is already known.
 * Because the reachability computation is only computed between contexts of vertices
 * of the same thread and with incoming or outgoing interference edges, the cache will
 * usually be of maintainable size.
 *
 *
 * @author Dennis Giffhorn
 * @version 1.0
 */
public class ReachabilityCache {
    private HashMap<Key, Boolean> cache;

    /** Creates a new empty ReachabilityCache.
     */
    public ReachabilityCache(){
        cache = new HashMap<Key, Boolean>();
    }

    /** Adds a reachability computation's result to the cache.
     *
     * @param from  The source context in the computation.
     * @param to  The target context in the computation.
     * @param result   Its result.
     */
    public void add(Context from, Context to, boolean result){
        cache.put(new Key(from, to), result);
    }

    /** Checks whether the cache contains a reachability result for a given pair of contexts.
     *
     * @param from  The source context in the computation.
     * @param to  The target context in the computation.
     */
    public boolean contains(Context from, Context to){
        return cache.containsKey(new Key(from, to));
    }

    /** Returns the cached result of a certain reachability computation.
     *
     * @param from  The source context in the computation.
     * @param to  The target context in the computation.
     */
    public boolean isReaching(Context from, Context to) {
        return cache.get(new Key(from, to));
    }



    /** A Key for the used HashMap.
     * Contains two Contexts, the start and the target of a reachability analysis.
     */
    static class Key {
        private Context from;
        private Context to;

        /** Creates a new Key for two Contexts.
         * @param f  Context one.
         * @param t  Context two.
         */
        Key(Context f, Context t) {
            from = f;
            to = t;
        }

        /** Returns the start Context.
         * @return  A Context.
         */
        Context from() {
            return from;
        }

        /** Returns the target Context.
         * @return  A Context.
         */
        Context to() {
            return to;
        }

		public boolean equals(Object o) {
			if (this == o) {
				return true;
			} else if (o == null) {
				return false;
			} else if (!(o instanceof Key)) {
				return false;
			} else {
				Key k = (Key) o;
				return from.equals(k.from) && to.equals(k.to);
			}
		}

        public int hashCode() {
        	return from.hashCode() | (to.hashCode() << 16);
        }
    }
}
