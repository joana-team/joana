/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.ifc.sdg.graph.slicer.barrier;

import edu.kit.joana.ifc.sdg.graph.slicer.Slicer;

/** Interface for barrier slicers.
 * A barrier slicer receives a set of nodes - the barrier - which it will not trespass.
 */
public interface BarrierSlicer extends Slicer, Barrier { }
