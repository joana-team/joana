/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */

package edu.kit.joana.ui.wala.easyifc.views;

import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.ui.wizards.IClasspathContainerPage;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;

public class AnnotationClassPathContainerPage extends WizardPage implements IClasspathContainerPage {

	private IClasspathEntry fEntry;

	public AnnotationClassPathContainerPage() {
		super("AnnotationClassPathContainerPage");
		setTitle("JOANA Source Code Annotations");
	}

	@Override
	public void createControl(final Composite parent) {
		final Label label = new Label(parent, SWT.NONE);
		if (fEntry == null) {
			label.setText("Nothing to configure. Press 'Finish' to add new entry");
		} else {
			label.setText("Nothing to configure.");
			setPageComplete(false);
		}
		setControl(label);
	}

	@Override
	public boolean finish() {
		if (fEntry == null) {
			fEntry = JavaCore.newContainerEntry(new Path("joana.ui.wala.easyifc.JOANA_ANNOTATIONS"));
		}
		return true;
	}

	@Override
	public IClasspathEntry getSelection() {
		return fEntry;
	}

	@Override
	public void setSelection(final IClasspathEntry containerEntry) {
		fEntry = containerEntry;
	}

}