/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.ui.ifc.sdg.gui.marker;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;

import edu.kit.joana.ifc.sdg.core.SecurityNode;
import edu.kit.joana.ui.ifc.sdg.gui.NJSecPlugin;
import edu.kit.joana.ui.ifc.sdg.gui.launching.ConfigurationAttributes;

/** A facade for the annotation mechanism model.
 * Stores the markers in Eclipse RunConfigurations.
 */
public class MarkerManager {
	// this class is a singleton
	private static MarkerManager instance;

	public static MarkerManager singleton() {
		if (instance == null) {
			instance = new MarkerManager();
		}

		return instance;
	}


	/* The object */

	private List<NJSecMarkerListener> listeners;

	private MarkerManager() {
		listeners = new LinkedList<NJSecMarkerListener>();
	}

	public void addListener(NJSecMarkerListener l) {
		listeners.add(l);
	}

	public void removeListener(NJSecMarkerListener l) {
		listeners.remove(l);
	}

	public void addMarker(IProject project, IMarker marker)
	throws CoreException {
		// add marker to configuration
		List<String> lmarker = NJSecPlugin.singleton().getMarker(project);

		lmarker.add(markerMemento(marker));
		updateConfiguration(project, lmarker);

		notifyListeners(project);
	}

	/** Achtung: die Methode benoetigt die Marker, die _nicht_ geloescht werden sollen.
	 * Das liegt daran, dass die Marker in der RunConfiguration als String gespeichert werden
	 * und daher nicht rausgeloescht werden koennen.
	 * Stattdessen werden alle Marker entfernt und die remainingMarkers hinzugefuegt.
	 *
	 * @param project
	 * @param remainingMarkers
	 */
	public void removeMarkers(IProject project, Collection<IMarker> delete, Collection<IMarker> remaining)
	throws CoreException {
		// remove the markers from the project
		for (IMarker marker : delete) {
			// Marker loeschen
			marker.delete();
		}

		// remove the markers from the storage
		List<String> lmarker = new LinkedList<String>();
		for (IMarker marker : remaining) {
			lmarker.add(markerMemento(marker));
		}
		updateConfiguration(project, lmarker);

		notifyListeners(project);
	}

	public void removeAllMarkers(IProject project)
	throws CoreException {
		IMarker[] delete = project.findMarkers(NJSecMarkerConstants.MARKER_TYPE_NJSEC, true, IResource.DEPTH_INFINITE);

		// remove the markers from the project
		for (IMarker marker : delete) {
			// Marker loeschen
			marker.delete();
		}

		// remove the markers from the storage
		List<String> lmarker = new LinkedList<String>();
		updateConfiguration(project, lmarker);

		notifyListeners(project);
	}

	public void updateMarker(IProject project)
	throws CoreException {
		// refresh the marker storage
		IMarker[] markers = project.findMarkers(NJSecMarkerConstants.MARKER_TYPE_NJSEC, true, IResource.DEPTH_INFINITE);
		List<String> lmarker = new LinkedList<String>();
		for (IMarker m : markers) {
			lmarker.add(markerMemento(m));
		}
		updateConfiguration(project, lmarker);

		notifyListeners(project);
	}

	private void updateConfiguration(IProject project, List<String> lmarker)
	throws CoreException {
		ILaunchConfigurationWorkingCopy copy = NJSecPlugin.singleton().getStandardLaunchConfiguration(project).
				copy(NJSecPlugin.singleton().getStandardLaunchConfiguration(project).getName());
		copy.setAttribute(ConfigurationAttributes.MARKER, lmarker);
		copy.doSave();
	}

	public void changeProject(IProject newProject, List<String> marker)
	throws CoreException {
		// TODO: bereits geoeffnete Projekte cachen, dann braucht man nur fuer bisher nicht geoeffnete Projekte
		// die Marker laden und ansonsten nur die Annotation view zu updaten

		// zuerst alle Marker entfernen
		IMarker[] markers = newProject.findMarkers(NJSecMarkerConstants.MARKER_TYPE_NJSEC, true, IResource.DEPTH_INFINITE);
		for (int i = 0; i < markers.length; i++) {
	        markers[i].delete();
        }

		// Dann Marker der Konfiguration erstellen
		LinkedList<IMarker> newMarkers = new LinkedList<IMarker>();
		for (String rawData : marker) {
			String[] data = rawData.split("\n");
			if (data.length < 12) {
				notifyListeners(newProject);
				continue;
			}
			IMarker im = null;
			IResource file = ResourcesPlugin.getWorkspace().getRoot().findMember(data[9]);
			NJSecMarkerFactory mf = NJSecPlugin.singleton().getMarkerFactory();

			if (data[0].equals(NJSecMarkerConstants.MARKER_TYPE_OUTPUT)) {
				im = mf.createOutputMarker(file, data[6], data[1], Integer.parseInt(data[2]),
						Integer.parseInt(data[3]), Integer.parseInt(data[4]), data[7], Integer.parseInt(data[10]), Integer.parseInt(data[11]));

			} else if (data[0].equals(NJSecMarkerConstants.MARKER_TYPE_INPUT)) {
				im = mf.createInputMarker(file, data[5], data[1], Integer.parseInt(data[2]),
						Integer.parseInt(data[3]), Integer.parseInt(data[4]), data[7], Integer.parseInt(data[10]), Integer.parseInt(data[11]));

			} else if (data[0].equals(NJSecMarkerConstants.MARKER_TYPE_REDEFINE)) {
				im = mf.createRedefiningMarker(file, data[6], data[5], data[1], Integer.parseInt(data[2]),
						Integer.parseInt(data[3]), Integer.parseInt(data[4]), data[7], Integer.parseInt(data[10]), Integer.parseInt(data[11]));
			}

			if (im != null) {
				if (data[8].equals("false")) {
					im.setAttribute(NJSecMarkerConstants.MARKER_ATTR_ACTIVE, false);
				}
				newMarkers.add(im);
			}
		}

		// notify the listeners
		notifyListeners(newProject);
	}

	private void notifyListeners(IProject project) {
		for (NJSecMarkerListener l : listeners) {
			l.markersChanged(project);
		}
	}

    // Wandelt die Attribute eines Markers in einen String
    private String markerMemento(IMarker marker) {
		String ret = "";
		try {
			ret += marker.getType();
			ret += "\n";
			ret += marker.getAttribute(IMarker.MESSAGE, "");
			ret += "\n";
			ret += marker.getAttribute(IMarker.LINE_NUMBER, 0);
			ret += "\n";
			ret += marker.getAttribute(IMarker.CHAR_START, 0);
			ret += "\n";
			int length = marker.getAttribute(IMarker.CHAR_END, 0) - marker.getAttribute(IMarker.CHAR_START, 0);
			ret += length;
			ret += "\n";
			ret += marker.getAttribute(NJSecMarkerConstants.MARKER_ATTR_PROVIDED, "");
			ret += "\n";
			ret += marker.getAttribute(NJSecMarkerConstants.MARKER_ATTR_REQUIRED, "");
			ret += "\n";
			ret += marker.getAttribute(NJSecMarkerConstants.MARKER_ATTR_MATCHING_SDGNODES, "");
			ret += "\n";
			ret += marker.getAttribute(NJSecMarkerConstants.MARKER_ATTR_ACTIVE, true);
			ret += "\n";
			ret += marker.getResource().getFullPath();
			ret += "\n";
			ret += marker.getAttribute(NJSecMarkerConstants.MARKER_ATTR_START_COLUMN, 0);
			ret += "\n";
			ret += marker.getAttribute(NJSecMarkerConstants.MARKER_ATTR_END_COLUMN, 0);
			ret += "\n";

		} catch (CoreException e) {
			e.printStackTrace();
		}

    	return ret;
    }

    public void map(IProject p, IMarker im, SecurityNode node) {
    	try {
			im.setAttribute(NJSecMarkerConstants.MARKER_ATTR_MATCHING_SDGNODES, node.getId() + ";");
		} catch (CoreException e) {
		}
		notifyListeners(p);
    }
}
