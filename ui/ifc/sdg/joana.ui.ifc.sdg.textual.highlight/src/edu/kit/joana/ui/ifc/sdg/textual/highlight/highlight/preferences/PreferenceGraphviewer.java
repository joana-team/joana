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


import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.preference.IntegerFieldEditor;
import org.eclipse.jface.preference.StringFieldEditor;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

import edu.kit.joana.ui.ifc.sdg.textual.highlight.highlight.HighlightPlugin;

public class PreferenceGraphviewer extends FieldEditorPreferencePage implements IWorkbenchPreferencePage {

	public void init(IWorkbench workbench) {
		setPreferenceStore(HighlightPlugin.getDefault().getPreferenceStore());
	}

	@Override
	protected void createFieldEditors() {
		addField(new StringFieldEditor("graphviewer.path", "Pfad:", getFieldEditorParent()));
		addField(new IntegerFieldEditor("graphviewer.port", "Verbindungsport:", getFieldEditorParent()));
	}
}
