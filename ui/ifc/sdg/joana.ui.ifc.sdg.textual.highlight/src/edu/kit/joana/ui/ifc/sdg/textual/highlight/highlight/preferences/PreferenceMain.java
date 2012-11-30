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
package edu.kit.joana.ui.ifc.sdg.textual.highlight.highlight.preferences;


import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

import edu.kit.joana.ui.ifc.sdg.textual.highlight.highlight.HighlightPlugin;

public class PreferenceMain extends PreferencePage implements IWorkbenchPreferencePage {

	protected Control createContents(Composite parent) {
		return null;
	}

	public void init(IWorkbench workbench) {
		setPreferenceStore(HighlightPlugin.getDefault().getPreferenceStore());
	}
}
