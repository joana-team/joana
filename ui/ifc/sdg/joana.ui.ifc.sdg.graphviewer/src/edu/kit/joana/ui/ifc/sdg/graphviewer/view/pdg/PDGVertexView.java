/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
/*
 * @(c)PDGVertexView.java
 *
 * Project: GraphViewer
 *
 * Chair for Softwaresystems
 * Faculty of Informatics and Mathematics
 * University of Passau
 *
 * Created on 25.03.2005 at 21:58:18
 */
package edu.kit.joana.ui.ifc.sdg.graphviewer.view.pdg;

import org.jgraph.graph.AttributeMap;
import org.jgraph.graph.CellViewRenderer;
import org.jgraph.graph.GraphConstants;
import org.jgraph.graph.VertexView;

import edu.kit.joana.ui.ifc.sdg.graphviewer.controller.event.AttributeMapAdjustmentsEvent;
import edu.kit.joana.ui.ifc.sdg.graphviewer.controller.event.AttributeMapAdjustmentsListener;
import edu.kit.joana.ui.ifc.sdg.graphviewer.translation.BundleConstants;



/**
 * This class provides a view for a vertex cell to be drawn. The view defines
 * the appearance (colour, font, label etc.) of the cell. These values are
 * provided by an attribute map.
 *
 * (A view is associated it with the specified cell using a CellMapper. See
 * JGraph tutorial.)
 *
 * @author <a href="mailto:westerhe@fmi.uni-passau.de">Marieke Westerheide </a>
 * @version 1.1
 */
public class PDGVertexView extends VertexView implements
        AttributeMapAdjustmentsListener, BundleConstants {
	private static final long serialVersionUID = 4437051206500200014L;

	/**
     * Constructs a new <code>PDGVertexView</code> for the specified cell
     * object.
     *
     * @param cell
     *            the vertex cell to be displayed
     * @param map
     *            contains the attribute values for this cell
     */
    public PDGVertexView(Object cell, PDGAttributeMap map) {
        super(cell);
        //set autosize to avoid resizing bugs
        GraphConstants.setAutoSize(map,true);
        this.setAttributes(map);
    }

    /**
     * Gets the renderer. The renderer is responsible for "rendering"
     * (displaying) a vertex.
     *
     * @see org.jgraph.graph.AbstractCellView#getRenderer()
     * @return the cell view renderer
     */
    @Override
	public CellViewRenderer getRenderer() {
        return new PDGDefaultVertexRenderer();
    }

    /**
     * @see edu.kit.joana.ui.ifc.sdg.graphviewer.controller.event.AttributeMapAdjustmentsListener#attributeMapChanged(edu.kit.joana.ui.ifc.sdg.graphviewer.controller.event.AttributeMapAdjustmentsEvent)
     * @param event
     *            the event that triggers a change in the node's attributes
     */
    public void attributeMapChanged(AttributeMapAdjustmentsEvent event) {
        this.setAttributes((AttributeMap) event.getSource());
    }


}
