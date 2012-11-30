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

/**
 * @author naxan
 *
 * A  ProgressAnnouncer should notify all known ProgressListeners of the
 * Progress done
 *
 */
public interface ProgressAnnouncer {
	void addProgressListener(ProgressListener pl);
	void removeProgressListener(ProgressListener pl);
}
