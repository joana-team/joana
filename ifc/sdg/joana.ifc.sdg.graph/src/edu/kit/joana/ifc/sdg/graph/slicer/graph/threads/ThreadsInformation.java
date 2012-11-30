/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.ifc.sdg.graph.slicer.graph.threads;

import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;

import edu.kit.joana.ifc.sdg.graph.SDGEdge;
import edu.kit.joana.ifc.sdg.graph.SDGNode;


/**
 * A class for informations about threads in a CFG.
 * For every thread it contains information about, this informations consist of<p>
 * <ul>
 * <li> the entry node </li>
 * <li> the exit node ('null' for the main thread) </li>
 * <li> the fork node ('null' for the main thread) </li>
 * <li> the thread allocation node ('null' for the main thread) </li>
 * <li> the thread invocation node ('null' for the main thread) </li>
 * <li> the join node (maybe 'null') </li>
 * </ul>
 * The class computes that information itself.
 *
 * -- Created on August 24, 2005
 *
 * @author  Dennis Giffhorn
 */
public final class ThreadsInformation implements Iterable<ThreadsInformation.ThreadInstance> {
    public static class ThreadInstance {
        public int id;
        public SDGNode entry;
        public SDGNode exit;
        public SDGNode fork;
        public SDGNode join;
        public final LinkedList<SDGNode> threadContext;
        public boolean dynamic;

//        public ThreadInstance() { }

        public ThreadInstance(int id, SDGNode en, SDGNode fo, LinkedList<SDGNode> tc) {
            this.id = id;
            entry = en;
            fork = fo;
            threadContext = tc;
        }

        public ThreadInstance(int id, SDGNode en, SDGNode ex, SDGNode fo, SDGNode jo, LinkedList<SDGNode> tc, boolean dyn) {
            this.id = id;
            entry = en;
            exit = ex;
            fork = fo;
            join = jo;
            threadContext = tc;
            dynamic = dyn;
        }

        public String toString() {
            StringBuilder b = new StringBuilder();
            b.append("Thread ").append(id).append(" {\n");
            if (entry == null) b.append("Entry ").append("0").append(";\n");
            else b.append("Entry ").append(entry).append(";\n");
            if (exit == null) b.append("Exit ").append("0").append(";\n");
            else b.append("Exit ").append(exit).append(";\n");
            if (fork == null) b.append("Fork ").append("0").append(";\n");
            else b.append("Fork ").append(fork).append(";\n");
            if (join == null) b.append("Join ").append("0").append(";\n");
            else b.append("Join ").append(join).append(";\n");
            if (threadContext == null || threadContext.size() == 0) b.append("Context null;\n");
            else b.append("Context ").append(threadContext).append(";\n");
            b.append("Dynamic ").append(dynamic).append(";\n}\n");
            return b.toString();
        }
    }

    // Class Invariant: ThreadInstance ID's are consistent with the order in the threads list
    private LinkedList<ThreadInstance> threads;

    /**
     * Creates a new instance of ThreadsInformation.
     *
     * @param icfg  The CFG whose threads need to be examined.
     */
    public ThreadsInformation(LinkedList<ThreadInstance> ti) {
        threads = ti;
    }

    /* (non-Javadoc)
     * @see java.lang.Iterable#iterator()
     */
    public Iterator<ThreadInstance> iterator() {
        return threads.iterator();
    }

    /**
     * Returns the entry node of a given thread.
     *
     * @param thread  The ID of some thread.
     */
    public SDGNode getThreadEntry(int thread) {
        return threads.get(thread).entry;
    }

    /**
     * Returns the exit node of a given thread.
     *
     * @param thread  The ID of some thread.
     */
    public SDGNode getThreadExit(int thread) {
        return threads.get(thread).exit;
    }

    /**
     * Returns the fork node of a given thread.
     *
     * @param thread  The ID of some thread.
     */
    public SDGNode getThreadFork(int thread) {
        return threads.get(thread).fork;
    }

    /**
     * Returns the join node of a given thread.
     *
     * @param thread  The ID of some thread.
     * @return        The returned value can be 'null'.
     */
    public SDGNode getThreadJoin(int thread) {
        return threads.get(thread).join;
    }

    public LinkedList<SDGNode> getThreadContext(int thread) {
        return threads.get(thread).threadContext;
    }

    public ThreadInstance getThread(SDGEdge fork) {
        for (ThreadInstance ti : threads) {
            if (ti.fork == fork.getSource() && ti.entry == fork.getTarget()) {
                return ti;
            }
        }
        return null;
    }

    public ThreadInstance getThread(int id) {
        return threads.get(id);
    }

    public ThreadInstance getThread(SDGNode fork) {
        for (ThreadInstance ti : threads) {
            if (ti.fork == fork) {
                return ti;
            }
        }
        return null;
    }

    public boolean isDynamic(int thread) {
        return threads.get(thread).dynamic;
    }

    public boolean isThreadStart(SDGNode node) {
        for (ThreadInstance  ti : threads) {
            if (ti.entry == node) return true;
        }

        return false;
    }

    public boolean isFork(SDGNode node) {
        for (ThreadInstance  ti : threads) {
            if (ti.fork == node) return true;
        }

        return false;
    }

    public Collection<SDGNode> getAllForks() {
        HashSet<SDGNode> s = new HashSet<SDGNode>();
        for (ThreadInstance t : threads) {
           if (t.fork != null) {
               s.add(t.fork);
           }
        }
        return s;
    }

    public Collection<SDGNode> getAllEntries() {
        HashSet<SDGNode> s = new HashSet<SDGNode>();
        for (ThreadInstance t : threads) {
           if (t.entry != null) {
               s.add(t.entry);
           }
        }
        return s;
    }

    public Collection<SDGNode> getAllJoins() {
        HashSet<SDGNode> s = new HashSet<SDGNode>();
        for (ThreadInstance t : threads) {
           if (t.join != null) {
               s.add(t.join);
           }
        }
        return s;
    }

    public int getNumberOfThreads() {
        return threads.size();
    }

    /**
     * Returns a String representation.
     */
    public String toString() {
        StringBuilder str = new StringBuilder();

        for (ThreadInstance  ti : threads) {
            str.append(ti.toString());
        }

        return str.toString();
    }
}
