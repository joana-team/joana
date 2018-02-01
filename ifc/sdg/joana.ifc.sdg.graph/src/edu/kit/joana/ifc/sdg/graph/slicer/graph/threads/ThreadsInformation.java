/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.ifc.sdg.graph.slicer.graph.threads;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import edu.kit.joana.ifc.sdg.graph.SDGNode;


/**
 * A class for informations about threads in a CFG.
 * For every thread it contains information about, this informations consist of<p>
 * <ul>
 * <li> the entry node </li>
 * <li> the exit node </li>
 * <li> the fork node ('null' for the main thread, which has, by convention, the id 0) </li>
 * <li> the join nodes (maybe empty) </li>
 * <li> the thread context (empty list for the main thread) </li>
 * <li> whether this thread is dynamic </li>
 * </ul>
 * The class computes that information itself.
 *
 * -- Created on August 24, 2005
 *
 * @author  Dennis Giffhorn
 */
public final class ThreadsInformation implements Iterable<ThreadsInformation.ThreadInstance> {
    public static class ThreadInstance {
    	public static final int MAIN_THREAD_ID = 0;
        private final int id;
        private final SDGNode entry;
        private final SDGNode exit;
        private final SDGNode fork;
        private final Collection<SDGNode> join;
        private final LinkedList<SDGNode> threadContext;
        private final boolean dynamic;

        public ThreadInstance(int id, SDGNode en, SDGNode ex, SDGNode fo,             LinkedList<SDGNode> tc, boolean dyn) {
            this(id, en, ex, fo, new LinkedList<SDGNode>(), tc, dyn);
        }
        
        public ThreadInstance(int id, SDGNode en, SDGNode ex, SDGNode fo, Collection<SDGNode> jo, LinkedList<SDGNode> tc, boolean dyn) {
            if (en == null || jo == null || tc == null || ex == null) throw new IllegalArgumentException();
            if (fo == null && id != MAIN_THREAD_ID) throw new IllegalArgumentException();
            if (en.getKind() != SDGNode.Kind.ENTRY) throw new IllegalArgumentException();
            if (ex.getKind() != SDGNode.Kind.EXIT) throw new IllegalArgumentException();
            if (fo != null && fo.getKind() != SDGNode.Kind.CALL) throw new IllegalArgumentException();
            this.id = id;
            this.entry = en;
            this.exit = ex;
            this.fork = fo;
            this.join = jo;
            this.threadContext = tc;
            this.dynamic = dyn;
        }

        public String toString() {
            StringBuilder b = new StringBuilder();
            b.append("Thread ").append(getId()).append(" {\n");
            b.append("Entry ").append(getEntry()).append(";\n");
            b.append("Exit ").append(getExit()).append(";\n");
            if (getFork() == null) b.append("Fork ").append("0").append(";\n");
            else b.append("Fork ").append(getFork()).append(";\n");
            if (join.isEmpty()) b.append("Join ").append("0").append(";\n");
            else if (join.size() == 1) b.append("Join ").append(join.iterator().next()).append(";\n");
            else b.append("Join ").append(join).append(";\n");
            if (getThreadContext().isEmpty()) b.append("Context null;\n");
            else b.append("Context ").append(getThreadContext()).append(";\n");
            b.append("Dynamic ").append(dynamic).append(";\n}\n");
            return b.toString();
        }

		public int getId() {
			return id;
		}

		public SDGNode getEntry() {
			return entry;
		}

		public SDGNode getExit() {
			return exit;
		}

		public SDGNode getFork() {
			return fork;
		}

		public SDGNode getJoin() {
			if (join.isEmpty()) return null;
			return join.iterator().next();
		}
		
		public Collection<SDGNode> getJoins() {
			return join;
		}

		public void setJoin(SDGNode join) {
			assert join != null;
			this.join.add(join);
		}

		public LinkedList<SDGNode> getThreadContext() {
			return threadContext;
		}

		public boolean isDynamic() {
			return dynamic;
		}

		@Override
		public int hashCode() {
			return 31 + getId() * 17;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj) return true;
			if (!(obj instanceof ThreadInstance)) return false;
			ThreadInstance ti = (ThreadInstance) obj;
			return getId() == ti.getId();
		}
    }

    // Class Invariant: ThreadInstance ID's are consistent with the order in the threads list
    
    private final List<ThreadInstance> threads;
     
    
    /**
     * Creates a new instance of ThreadsInformation.
     *
     * @param icfg  The CFG whose threads need to be examined.
     */
    public ThreadsInformation(List<ThreadInstance> ti) {
    	this.threads = new ArrayList<ThreadInstance>();
    	this.threads.addAll(ti);
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
        return threads.get(thread).getEntry();
    }

    /**
     * Returns the exit node of a given thread.
     *
     * @param thread  The ID of some thread.
     */
    public SDGNode getThreadExit(int thread) {
        return threads.get(thread).getExit();
    }

    /**
     * Returns the fork node of a given thread.
     *
     * @param thread  The ID of some thread.
     */
    public SDGNode getThreadFork(int thread) {
        return threads.get(thread).getFork();
    }

    /**
     * Returns the join node of a given thread.
     *
     * @param thread  The ID of some thread.
     * @return        The returned value can be 'null'.
     */
    public SDGNode getThreadJoin(int thread) {
        return threads.get(thread).getJoin();
    }

    public LinkedList<SDGNode> getThreadContext(int thread) {
        return threads.get(thread).threadContext;
    }

    public ThreadInstance getThread(int id) {
        return threads.get(id);
    }

    public ThreadInstance getThread(SDGNode fork) {
        for (ThreadInstance ti : threads) {
            if (ti.getFork() == fork) {
                return ti;
            }
        }
        return null;
    }

    public Collection<ThreadInstance> getThreads(SDGNode fork) {
    	Collection<ThreadInstance> ret = new LinkedList<ThreadInstance>();
        for (ThreadInstance ti : threads) {
            if (ti.getFork() == fork) {
                ret.add(ti);
            }
        }
        return ret;
    }

    public boolean isDynamic(int thread) {
        return threads.get(thread).dynamic;
    }

    public boolean isThreadStart(SDGNode node) {
        for (ThreadInstance  ti : threads) {
            if (ti.getEntry() == node) return true;
        }

        return false;
    }

    public boolean isFork(SDGNode node) {
        for (ThreadInstance  ti : threads) {
            if (ti.getFork() == node) return true;
        }

        return false;
    }

    public Collection<SDGNode> getAllForks() {
        HashSet<SDGNode> s = new HashSet<SDGNode>();
        for (ThreadInstance t : threads) {
           if (t.getFork() != null) {
               s.add(t.getFork());
           }
        }
        return s;
    }

    public Collection<SDGNode> getAllEntries() {
        HashSet<SDGNode> s = new HashSet<SDGNode>();
        for (ThreadInstance t : threads) {
           if (t.getEntry() != null) {
               s.add(t.getEntry());
           }
        }
        return s;
    }

    public Collection<SDGNode> getAllJoins() {
        HashSet<SDGNode> s = new HashSet<SDGNode>();
        for (ThreadInstance t : threads) {
           if (t.getJoin() != null) {
               s.add(t.getJoin());
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
