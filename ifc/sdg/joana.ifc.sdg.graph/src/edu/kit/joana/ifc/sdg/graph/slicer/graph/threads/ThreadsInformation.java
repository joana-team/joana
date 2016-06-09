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
 * <li> the exit node ('null' for the main thread) </li>
 * <li> the fork node ('null' for the main thread) </li>
 * <li> the thread allocation node ('null' for the main thread) </li>
 * <li> the thread invocation node ('null' for the main thread) </li>
 * <li> the join nodes (maybe empty) </li>
 * </ul>
 * The class computes that information itself.
 *
 * -- Created on August 24, 2005
 *
 * @author  Dennis Giffhorn
 */
public final class ThreadsInformation implements Iterable<ThreadsInformation.ThreadInstance> {
    public static class ThreadInstance {
        private final int id;
        private final SDGNode entry;
        private SDGNode exit = null;
        private final SDGNode fork;
        private Collection<SDGNode> join;
        private final LinkedList<SDGNode> threadContext;
        private boolean dynamic;

//        public ThreadInstance() { }

        public ThreadInstance(int id, SDGNode en, SDGNode fo, LinkedList<SDGNode> tc) {
            this(id, en, null, fo, null, tc, false);
        }

        public ThreadInstance(int id, SDGNode en, SDGNode ex, SDGNode fo, SDGNode jo, LinkedList<SDGNode> tc, boolean dyn) {
        	this.id = id;
            this.entry = en;
            this.exit = ex;
            this.fork = fo;
            this.join = new LinkedList<SDGNode>();
            if (jo != null)
            	this.join.add(jo);
            this.threadContext = tc;
            this.dynamic = dyn;
        }

        public String toString() {
            StringBuilder b = new StringBuilder();
            b.append("Thread ").append(getId()).append(" {\n");
            if (getEntry() == null) b.append("Entry ").append("0").append(";\n");
            else b.append("Entry ").append(getEntry()).append(";\n");
            if (getExit() == null) b.append("Exit ").append("0").append(";\n");
            else b.append("Exit ").append(getExit()).append(";\n");
            if (getFork() == null) b.append("Fork ").append("0").append(";\n");
            else b.append("Fork ").append(getFork()).append(";\n");
            if (join.isEmpty()) b.append("Join ").append("0").append(";\n");
            else b.append("Join ").append(join.iterator().next()).append(";\n");
            if (getThreadContext() == null || getThreadContext().size() == 0) b.append("Context null;\n");
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

		public void setExit(SDGNode exit) {
			if (this.exit != null) {
				throw new IllegalStateException("The 'exit' field of ThreadInstance is supposed to be set only once!");
			}
			this.exit = exit;
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
			this.join.add(join);
			/*if (this.join != null) {
				throw new IllegalStateException("The 'join' field of ThreadInstance is supposed to be set only once!");
			}
			this.join = join;*/
		}

		public LinkedList<SDGNode> getThreadContext() {
			return threadContext;
		}

		public boolean isDynamic() {
			return dynamic;
		}

		public void setDynamic(boolean dynamic) {
			this.dynamic = dynamic;
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
    
    private List<ThreadInstance> threads;
     
    
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
