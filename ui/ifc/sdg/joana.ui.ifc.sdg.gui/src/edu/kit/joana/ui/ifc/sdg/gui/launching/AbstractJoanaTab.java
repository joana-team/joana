/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.ui.ifc.sdg.gui.launching;


import org.eclipse.debug.ui.AbstractLaunchConfigurationTab;
import org.eclipse.debug.ui.ILaunchConfigurationTab;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Text;

import edu.kit.joana.ui.ifc.sdg.gui.NJSecPlugin;

public abstract class AbstractJoanaTab extends AbstractLaunchConfigurationTab {

    /**
     * Updates the buttons and message in this page's launch
     * configuration dialog.
     */
    protected void updateLaunchConfigurationDialog() {
        if (getLaunchConfigurationDialog() != null) {
            //order is important here due to the call to
            //refresh the tab viewer in updateButtons()
            //which ensures that the messages are up to date
            getLaunchConfigurationDialog().updateButtons();
            getLaunchConfigurationDialog().updateMessage();
        }
    }

    /**
     * @see ILaunchConfigurationTab#getName()
     */
    public String getName() {
        return "Joana IFC Tab";
    }

    /**
     * @see ILaunchConfigurationTab#getImage()
     */
    public Image getImage() {
        return NJSecPlugin.singleton().getImageRegistry().get("joana");
    }

    /***
     * Show FileSelection Dialog.
     * Only files with extension in paramater extensions are shown
     * the selected files complete path gets written with target.setText();
     * @param target
     * @param extensions
     */
    protected void handleBrowseFileButtonSelected(Text target, String[] extensions) {
        FileDialog dialog = new FileDialog(getShell());

        dialog.setFilterExtensions(extensions);
        dialog.open();
        String path = !dialog.getFilterPath().equals("") ? dialog.getFilterPath() + System.getProperty("file.separator") : null;
        String result = path + dialog.getFileName();

        if (!dialog.getFileName().equals("")) {
            target.setText(result);
        }
    }

    /***
     * Show FileSelection Dialog.
     * Only files with extension in paramater extensions are shown
     * the selected files complete path gets written with target.setText();
     * @param target
     * @param extensions
     */
    protected void handleBrowseDirectoryButtonSelected(Text target) {
        DirectoryDialog dialog = new DirectoryDialog(getShell());
        target.setText(dialog.open());
    }
}
