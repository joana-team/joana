/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.ifc.sdg.graph.slicer.graph.threads;

import java.util.Collection;

import edu.kit.joana.ifc.sdg.graph.SDG;
import edu.kit.joana.ifc.sdg.graph.SDGNode;
import edu.kit.joana.ifc.sdg.graph.slicer.graph.VirtualNode;


public final class SimpleMHPAnalysis implements MHPAnalysis {
	private final ThreadsInformation ti;
	private final ThreadRegions regions;

	private SimpleMHPAnalysis(ThreadsInformation info, ThreadRegions tr) {
		ti = info;
		regions = tr;
	}

	public ThreadRegions getTR() {
		return regions;
	}

	public Collection<ThreadRegion> getThreadRegions() {
		return regions.getThreadRegions();
	}

	public ThreadRegion getThreadRegion(SDGNode node, int thread) {
		return regions.getThreadRegion(thread);
	}

	public ThreadRegion getThreadRegion(VirtualNode node) {
		return regions.getThreadRegion(node.getNumber());
	}

	public ThreadRegion getThreadRegion(int id) {
		return regions.getThreadRegion(id);
	}

	public boolean isParallel(SDGNode m, int mThread, int region) {
		// region entspricht der thread-ID
		if (mThread == region && ti.isDynamic(mThread)) {
            // dynamic thread
            return true;

        } else {
            return mThread != region;
        }
	}

	public boolean isParallel(SDGNode m, SDGNode n) {
		int[] mThreads = m.getThreadNumbers();
		int[] nThreads = n.getThreadNumbers();

		if (mThreads.length != nThreads.length) return true;

		for (int mt : mThreads) {
			for (int nt : nThreads) {
				if (isParallel(m, mt, n, nt)) {
					return true;
				}
			}
		}

		return false;
	}

    public boolean isParallel(VirtualNode m, VirtualNode n) {
        return isParallel(m.getNode(), m.getNumber(), n.getNode(), n.getNumber());
    }

	public boolean isParallel(SDGNode m, int mThread, SDGNode n, int nThread) {
        if (mThread == nThread && ti.isDynamic(mThread)) {
            // dynamic thread
            return true;

        } else {
            return mThread != nThread;
        }
	}

    public boolean isParallel(ThreadRegion r, ThreadRegion s) {
    	//region id is the same as thread id here
    	return isParallel(r.getID(), s.getID());
    }

    private boolean isParallel(int thread_1, int thread_2) {
    	if (thread_1 == thread_2 && ti.isDynamic(thread_1)) {
            // dynamic thread
            return true;

        } else {
            return thread_1 != thread_2;
        }
    }

	public SDGNode getThreadEntry(int thread) {
		return ti.getThreadEntry(thread);
	}

	public SDGNode getThreadExit(int thread) {
		return ti.getThreadExit(thread);
	}

	public boolean isDynamic(int thread) {
		return ti.isDynamic(thread);
	}


	/* Factory */

	public static SimpleMHPAnalysis analyze(SDG g) {
		ThreadRegions tr = ThreadRegions.allThreadsParallel(g);
		return new SimpleMHPAnalysis(g.getThreadsInfo(), tr);
	}

	@Override
	public boolean mayExist(int thread, VirtualNode v) {
		return true;
	}

	@Override
	public boolean mayExist(int thread, SDGNode n, int nThread) {
		return true;
	}
}
