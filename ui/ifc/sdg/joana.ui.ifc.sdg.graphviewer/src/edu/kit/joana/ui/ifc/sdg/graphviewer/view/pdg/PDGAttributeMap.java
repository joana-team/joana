/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
/*
 * @(c)PDGAttributeMap.java
 *
 * Project: GraphViewer
 *
 * Chair for Softwaresystems
 * Faculty of Informatics and Mathematics
 * University of Passau
 *
 * Created on 04.12.2004 at 12:29:45
 */
package edu.kit.joana.ui.ifc.sdg.graphviewer.view.pdg;

import edu.kit.joana.ui.ifc.sdg.graphviewer.controller.event.AttributeMapAdjustmentsEvent;
import edu.kit.joana.ui.ifc.sdg.graphviewer.controller.event.AttributeMapAdjustmentsListener;

import javax.swing.event.EventListenerList;

import org.jgraph.graph.AttributeMap;
import org.jgraph.graph.GraphConstants;

/**
 * The attributes are represented by maps of (key, value) pairs, which may be
 * accessed in a type-safe way by using the GraphConstants class. The
 * attributeMap argument used here contains instances of the CellView class as
 * keys.
 *
 * @author <a href="mailto:wellner@fmi.uni-passau.de">Tobias Wellner </a>, <a
 *         href="mailto:westerhe@fmi.uni-passau.de">Marieke Westerheide </a>
 * @version 1.1
 */
public class PDGAttributeMap extends AttributeMap {
	private static final long serialVersionUID = -8838189965592251304L;

	/**
     * @see javax.swing.event.EventListenerList
     *      javax.swing.event.EventListenerList
     */
    protected EventListenerList listenersList = new EventListenerList();

    /**
     * @see javax.swing.event.EventListenerList
     *      javax.swing.event.EventListenerList
     */
    private AttributeMapAdjustmentsEvent event = null;

    /**
     * Constructs a new <code>PDGAttributeMap</code> object.
     */
    public PDGAttributeMap() {
        super();
        GraphConstants.setResize(this, true);
        GraphConstants.setAutoSize(this, false);
        GraphConstants.setEditable(this, false);
    }

    /**
     * @see javax.swing.event.EventListenerList
     *      javax.swing.event.EventListenerList
     */
    public void addAttributeMapListener(AttributeMapAdjustmentsListener listener) {
        this.listenersList.add(AttributeMapAdjustmentsListener.class, listener);
        listener.attributeMapChanged(new AttributeMapAdjustmentsEvent(this));
    }

    /**
     * @see javax.swing.event.EventListenerList
     *      javax.swing.event.EventListenerList
     */
    public void removeAttributeMapListenerListener(
            AttributeMapAdjustmentsListener listener) {
        listenersList.remove(AttributeMapAdjustmentsListener.class, listener);
    }

    /**
     * Notify all listeners that have registered interest for notification on
     * this event type. Process the listeners last to first, notifying those
     * that are interested in this event.
     *
     * @see javax.swing.event.EventListenerList
     *      javax.swing.event.EventListenerList
     */
    protected void fireAttributeMapChanged() {
        Object[] listeners = this.listenersList.getListenerList();
        for (int i = listeners.length - 2; i >= 0; i -= 2) {
            if (listeners[i] == AttributeMapAdjustmentsListener.class) {
                if (this.event == null) {
                    this.event = new AttributeMapAdjustmentsEvent(this);
                }
                ((AttributeMapAdjustmentsListener) listeners[i + 1])
                        .attributeMapChanged(this.event);
            }
        }
    }
}
