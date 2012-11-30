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
package edu.kit.joana.ui.ifc.sdg.gui.marker;


import org.eclipse.core.resources.IMarker;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.ide.IDE;

import edu.kit.joana.ui.ifc.sdg.gui.NJSecPlugin;

public class MissingMarkerMessage implements Runnable {
    private IMarker m;

    public MissingMarkerMessage(IMarker m ) {
        this.m = m;
    }

    public void run() {
        IWorkbenchPage page = NJSecPlugin.singleton().getActivePage();

        try {
            IDE.openEditor(page, m);

        } catch (PartInitException e) {
            NJSecPlugin.singleton().showError("Problem while jumping to NJSec Annotation Marker", null, e);
        }
    }
}
