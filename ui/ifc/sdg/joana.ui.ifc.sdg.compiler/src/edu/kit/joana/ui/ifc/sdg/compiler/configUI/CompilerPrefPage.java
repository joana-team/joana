/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.ui.ifc.sdg.compiler.configUI;


import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.preference.BooleanFieldEditor;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

import edu.kit.joana.ui.ifc.sdg.compiler.util.Activator;

/**
 * Preference page for setting the JOANA compiler options.
 *
 */
public class CompilerPrefPage extends FieldEditorPreferencePage implements IWorkbenchPreferencePage {

	private static final String RECOMPILE_MESSAGE = "Changing the Joana Compiler settings requires a full rebuild of all projects in the workspace. Do you want to continue?";
	private BooleanFieldEditor privateAccess;
	private BooleanFieldEditor allowUncaught;
	private Composite parent;

	@Override
	public boolean performOk() {
		boolean rebuildRequired = rebuildRequired();

		if (rebuildRequired) {
			if (!MessageDialog.openQuestion(getShell(), getTitle(), RECOMPILE_MESSAGE))
				return false;
			rebuild();
		}

		return super.performOk();
	}

	protected boolean rebuildRequired() {
		boolean rebuildRequired = false;
		rebuildRequired = rebuildRequired || privateAccess.getBooleanValue() != getPreferenceStore().getBoolean(ConfigConstants.COMPILER_PRIVATE_ACCESS);
		rebuildRequired = rebuildRequired || allowUncaught.getBooleanValue() != getPreferenceStore().getBoolean(ConfigConstants.COMPILER_ALLOW_UNCAUGHT);
		return rebuildRequired;
	}

	private void rebuild() {
		new Job("Joana build cache update") {
			@Override
			protected IStatus run(IProgressMonitor monitor) {
				try {
					Activator.getWorkspace().build(IncrementalProjectBuilder.FULL_BUILD, null);
				} catch (CoreException e) {
					return new Status(IStatus.WARNING, Activator.PLUGIN_ID, IStatus.OK, "Joana build cache update completed with errors", e);
				}
				return new Status(IStatus.OK, Activator.PLUGIN_ID, IStatus.OK, "Joana build cache update succeeded", null);
			}
		}.schedule();
	}

	/**
	 * Constructor
	 */
	public CompilerPrefPage() {
        super(FLAT);
		setPreferenceStore(Activator.getDefault().getPreferenceStore());
	}

	/**
	 * Constructor
	 *
	 * @param style
	 *            the style of the pref page
	 */
	public CompilerPrefPage(int style) {
		super(style);
		setPreferenceStore(Activator.getDefault().getPreferenceStore());
	}

	/**
	 * Constructor
	 *
	 * @param title
	 *            the title of the pref page
	 * @param style
	 *            the style of the pref page
	 */
	public CompilerPrefPage(String title, int style) {
		super(title, style);
		setPreferenceStore(Activator.getDefault().getPreferenceStore());
	}

	/**
	 * Constructor
	 *
	 * @param title
	 *            the title of the pref page
	 * @param image
	 *            the icon of the pref page
	 * @param style
	 *            the style of the pref page
	 */
	public CompilerPrefPage(String title, ImageDescriptor image, int style) {
		super(title, image, style);
		setPreferenceStore(Activator.getDefault().getPreferenceStore());
	}

	@Override
	protected void createFieldEditors() {
		parent = getFieldEditorParent();
		privateAccess = new BooleanFieldEditor(ConfigConstants.COMPILER_PRIVATE_ACCESS, "Allow acess to private members", parent);
		allowUncaught = new BooleanFieldEditor(ConfigConstants.COMPILER_ALLOW_UNCAUGHT, "Allow throwing of undeclared Exceptions", parent);
		addField(privateAccess);
		addField(allowUncaught);
	}

	public void init(IWorkbench workbench) {

	}

	protected void setCompilerSettingsFieldsEnabled(boolean enabled) {
		privateAccess.setEnabled(enabled, parent);
		allowUncaught.setEnabled(enabled, parent);
	}

}
