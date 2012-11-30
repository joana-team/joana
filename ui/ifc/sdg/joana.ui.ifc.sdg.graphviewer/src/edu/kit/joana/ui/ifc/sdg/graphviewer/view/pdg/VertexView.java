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
 * VertexView.java
 *
 * Created on 21. Oktober 2005, 14:46
 */

package edu.kit.joana.ui.ifc.sdg.graphviewer.view.pdg;
import org.jgraph.graph.CellViewRenderer;

/**
 * This class implements the vertex view of JGraph.
 * @author Siegfried Weber
 */
public class VertexView extends org.jgraph.graph.VertexView {
	private static final long serialVersionUID = -8722011124767060452L;
	/**
     * renderer for the class
     */
    public static transient VertexRenderer renderer = new VertexRenderer();

    /**
     * Constructs an empty vertex view.
     */
    public VertexView() {
        super();
    }

    /**
     * Constructs a vertex view for the specified model object and the specified
     * child views.
     * @param cell reference to the model object
     */
    public VertexView(Object cell) {
        super(cell);
    }

    /**
     * Returns the renderer.
     * @return the renderer
     */
    @Override
	public CellViewRenderer getRenderer() {
        return renderer;
    }
}
