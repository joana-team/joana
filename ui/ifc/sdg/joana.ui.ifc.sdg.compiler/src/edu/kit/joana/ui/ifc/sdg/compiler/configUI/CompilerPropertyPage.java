/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.ui.ifc.sdg.compiler.configUI;


import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.IWorkbenchPropertyPage;

import edu.kit.joana.ui.ifc.sdg.compiler.util.Activator;

/**
 * Implements a project specific property page for the JOANA compiler options
 * based on the workspace global preference page.
 *
 */
public class CompilerPropertyPage extends CompilerPrefPage implements IWorkbenchPropertyPage {

	private ExtendedBooleanFieldEditor projectSpecific;

	@Override
	protected void createFieldEditors() {
		addField(projectSpecific);

		super.createFieldEditors();

		super.setCompilerSettingsFieldsEnabled(projectSpecific.getBooleanValue());
	}

	@Override
	protected Control createContents(Composite parent) {
		Composite container = new Composite(parent, SWT.NONE);
		GridLayout layout = new GridLayout(1, false);
		layout.marginHeight = 0;
		layout.marginWidth = 0;
		container.setLayout(layout);

		projectSpecific = new ExtendedBooleanFieldEditor(ConfigConstants.COMPILER_PROJECT_SPECIFIC, "Use project specific settings", container);
		projectSpecific.addPropertyChangeListener(new IPropertyChangeListener() {
			public void propertyChange(PropertyChangeEvent event) {
				updateCompilerControls();
			}
		});

		Label label = new Label(container, SWT.HORIZONTAL | SWT.SEPARATOR);
		label.setLayoutData(new GridData(GridData.GRAB_HORIZONTAL | GridData.FILL_HORIZONTAL));

		Control settings = super.createContents(container);
		settings.setLayoutData(new GridData(GridData.GRAB_HORIZONTAL | GridData.GRAB_VERTICAL | GridData.FILL_BOTH));

		updateCompilerControls();

		return container;
	}

	private void updateCompilerControls() {
		setCompilerSettingsFieldsEnabled(projectSpecific.getBooleanValue());
	}

	private IAdaptable element;

	/**
	 * Constructor
	 */
	public CompilerPropertyPage() {
		super();
	}

	/**
	 * Constructor
	 *
	 * @param style the style of the property page
	 */
	public CompilerPropertyPage(int style) {
		super(style);
	}

	/**
	 * Constructor
	 *
	 * @param title the title of the property page
	 * @param image the image of the property page
	 * @param style the style of the property page
	 */
	public CompilerPropertyPage(String title, ImageDescriptor image, int style) {
		super(title, image, style);
	}

	/**
	 * Constructor
	 * @param title the title of the property page
	 * @param style the style of the property page
	 */
	public CompilerPropertyPage(String title, int style) {
		super(title, style);
	}

	public IAdaptable getElement() {
		return element;
	}

	public void setElement(IAdaptable element) {
		this.element = element;
		setPreferenceStore(new ProjectPreferenceStore(getJavaProject().getProject(), Activator.PLUGIN_ID));
	}

	private IJavaProject getJavaProject() {
		return (IJavaProject) element.getAdapter(IJavaProject.class);
	}

	@Override
	public boolean rebuildRequired() {
		if (projectSpecific.getBooleanValue() != getPreferenceStore().getBoolean(ConfigConstants.COMPILER_PROJECT_SPECIFIC))
			return true;
		return super.rebuildRequired();
	}

}
