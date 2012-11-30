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
package edu.kit.joana.ui.ifc.sdg.gui.views.mapping;

import java.util.LinkedList;
import java.util.List;

import org.eclipse.core.resources.IMarker;

/** Connects AnnotationViews with MarkerSecurityNodeViews.
 * Cumbersome, but necessary due to the lazy initialization of Eclipse views.
 *
 * @author giffhorn
 */
public class MarkerSelectionBroker {
	private static MarkerSelectionBroker instance;

	public static MarkerSelectionBroker getInstance() {
		if (instance == null) {
			instance = new MarkerSelectionBroker();
		}
		return instance;
	}

	private List<MarkerSelectionListener> listeners;

	private MarkerSelectionBroker () {
		listeners = new LinkedList<MarkerSelectionListener>();
	}

	public void addListener(MarkerSelectionListener l) {
		if (!listeners.contains(l)) listeners.add(l);
	}

	public void removeListener(MarkerSelectionListener l) {
		listeners.remove(l);
	}

	public void notifyListeners(IMarker marker) {
		for (MarkerSelectionListener l : listeners) {
			l.markerSelectionChanged(marker);
		}
	}
}
