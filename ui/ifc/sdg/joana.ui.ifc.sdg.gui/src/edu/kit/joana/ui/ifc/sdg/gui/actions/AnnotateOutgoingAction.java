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
package edu.kit.joana.ui.ifc.sdg.gui.actions;


import org.eclipse.core.resources.IMarker;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.ui.dialogs.SelectionDialog;

import edu.kit.joana.ifc.sdg.lattice.IEditableLattice;
import edu.kit.joana.ui.ifc.sdg.gui.NJSecPlugin;
import edu.kit.joana.ui.ifc.sdg.gui.views.LatticeDialog;


public class AnnotateOutgoingAction extends AnnotateAction {
	private static final boolean DEBUG = false;

    protected SelectionDialog createDialog(IEditableLattice<String> l) {
        LatticeDialog dlg = new LatticeDialog(NJSecPlugin.singleton().getShell());

         if (DEBUG) System.out.println("AnnotateAction.Run");
         dlg.setLattice(l);
         dlg.setAddCancelButton(true);
         dlg.setMessage("Select Security Class for Selected Code");
         dlg.setTitle("Select Security Class");

         return dlg;
    }

    protected void evaluateDialog(SelectionDialog dlg) {
        Object[] secclasses = dlg.getResult();
        if (secclasses == null) return;
        secclass = secclasses[0].toString();
        if (secclass == null) return;
    }

    protected IMarker createMarker() throws CoreException {
//        String message = "OUT Value: " + selText + " Security-Class: " + secclass;
        String message = "OUT Value: " + selText;
        return NJSecPlugin.singleton().getMarkerFactory().createOutputMarker(resource, secclass, message, line, offset, length, sc, ec);
    }
}
