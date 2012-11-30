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
package edu.kit.joana.ui.ifc.sdg.gui.ifcalgorithms;


import org.eclipse.core.runtime.IProgressMonitor;

import edu.kit.joana.ifc.sdg.core.interfaces.ProgressListener;

public class Progress implements ProgressListener {
    private IProgressMonitor monitor = null;

    public Progress(IProgressMonitor monitor) {
        this.monitor = monitor;
    }

    public void progressChanged(String progressTitle, int progress, int progressmax) {
        if (!progressTitle.equals("")) monitor.beginTask(progressTitle, progressmax);

        monitor.worked(progress);
    }


//    public IProgressMonitor getMonitor() {
//        return monitor;
//    }
}
