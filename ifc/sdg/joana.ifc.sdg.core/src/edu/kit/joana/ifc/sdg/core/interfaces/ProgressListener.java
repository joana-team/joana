/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
/*
 * Created on 27.02.2005
 *
 */
package edu.kit.joana.ifc.sdg.core.interfaces;

//import org.eclipse.core.runtime.IProgressMonitor;

/**
 * Listens to progress announced by  {@see  ProgressAnnouncer}
 * @author  naxan
 */
public interface ProgressListener {
	void progressChanged(String progressTitle, int progress, int progressmax);

//	IProgressMonitor getMonitor ();
}
