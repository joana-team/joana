/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
/** An interface for MHP analyses.
 *
 */
package edu.kit.joana.ifc.sdg.graph.slicer.graph.threads;

import java.util.Collection;

import edu.kit.joana.ifc.sdg.graph.SDGNode;
import edu.kit.joana.ifc.sdg.graph.slicer.graph.VirtualNode;


/**
 * @author giffhorn
 *
 */
public interface MHPAnalysis {
    /** Konservative Parallelitaetsabfrage - m und n sind sequentiell, wenn alle moeglichen Instanzen zueinander sequentiell sind.
     *
     * @param m
     * @param n
     * @return
     */
    public boolean isParallel(SDGNode m, SDGNode n);

    /** Konservative Parallelitaetsabfrage - m und n sind sequentiell, wenn alle moeglichen Instanzen zueinander sequentiell sind.
     *
     * @param m
     * @param n
     * @return
     */
    public boolean isParallel(VirtualNode m, VirtualNode n);

    public boolean isParallel(SDGNode m, int mThread, SDGNode n, int nThread);

    public boolean isParallel(SDGNode m, int mThread, int region);

    public SDGNode getThreadExit(int thread);

    public SDGNode getThreadEntry(int thread);

    public boolean isDynamic(int thread);

    public ThreadRegions getTR();

    public Collection<ThreadRegion> getThreadRegions();

    public ThreadRegion getThreadRegion(SDGNode node, int thread);

    public ThreadRegion getThreadRegion(VirtualNode node);

    public ThreadRegion getThreadRegion(int id);

    public boolean mayExist(int thread, VirtualNode v);

    public boolean mayExist(int thread, SDGNode n, int nThread);
}
