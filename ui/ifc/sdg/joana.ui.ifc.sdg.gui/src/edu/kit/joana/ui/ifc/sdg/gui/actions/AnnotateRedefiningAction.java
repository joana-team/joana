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
import edu.kit.joana.ui.ifc.sdg.gui.views.DoubleLatticeDialog;


public class AnnotateRedefiningAction extends AnnotateAction {
	protected String provided = "";

	protected SelectionDialog createDialog(IEditableLattice<String> l) {
	    DoubleLatticeDialog dlg = new DoubleLatticeDialog(NJSecPlugin.singleton().getShell());

        dlg.setLattice(l);
        dlg.setAddCancelButton(true);
        dlg.setMessage1("Select Security Class Allowed To Flow Into Selected Code\n");
        dlg.setMessage2("Select Security Class For Information Flowing Out Of Selected Code\n");
        dlg.setTitle("Select Security Class");

	    return dlg;
    }

    protected void evaluateDialog(SelectionDialog dlg) {
        Object[] secclasses = dlg.getResult();
        if (secclasses == null || secclasses.length < 2) return;
        secclass = secclasses[0].toString(); // secclass is used as the `required' label
        if (secclass == null) return;
        provided = secclasses[1].toString();
        if (provided == null) return;
    }

    protected IMarker createMarker() throws CoreException {
    	String message = "RED Value: " + selText;
        return NJSecPlugin.singleton().getMarkerFactory().
        		createRedefiningMarker(resource, secclass, provided, message, line, offset, length, sc, ec);
    }
}
