/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
/*
 * Created on 06.12.2004
 *
 */
package edu.kit.joana.ui.ifc.sdg.latticeeditor;

/**
 * @author naxan
 *
 * Every class implementing this interface may register as
 * LatticeeditorsListener with LatticeEditorPlugin
 */
public interface ChangedLatticeeditorsListener {
	public void changedLatticeEditors();
}
