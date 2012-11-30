/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
/*
 * CellViewFactory.java
 *
 * Created on 21. Oktober 2005, 14:42
 */

package edu.kit.joana.ui.ifc.sdg.graphviewer.view.pdg;
import org.jgraph.graph.DefaultCellViewFactory;


/**
 * A factory for vertex views.
 * @author Siegfried Weber
 */
public class CellViewFactory extends DefaultCellViewFactory {

    /**
     * Constructs a VertexView view for the specified object.
     * @param cell a vertex
     * @return a vertex view
     */
    @Override
	protected VertexView createVertexView(Object cell) {
        return new VertexView(cell);
    }

    /*protected EdgeView createEdgeView(Object cell){
    	return new EdgeView(cell);
    }*/
}
