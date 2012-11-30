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
/*******************************************************************************
 * Copyright (c) 2003, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package edu.kit.joana.ui.ifc.sdg.latticeeditor.ui;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.ObjectOutputStream;


import org.eclipse.core.resources.IFile;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.dialogs.WizardNewFileCreationPage;
import org.eclipse.ui.ide.IDE;

import edu.kit.joana.ui.ifc.sdg.latticeeditor.LatticeEditorPlugin;
import edu.kit.joana.ui.ifc.sdg.latticeeditor.model.Activity;
import edu.kit.joana.ui.ifc.sdg.latticeeditor.model.ActivityDiagram;
import edu.kit.joana.ui.ifc.sdg.latticeeditor.model.ParallelActivity;
import edu.kit.joana.ui.ifc.sdg.latticeeditor.model.SequentialActivity;
import edu.kit.joana.ui.ifc.sdg.latticeeditor.model.Transition;

/**
 * FlowWizardPage1
 *
 * @author Daniel Lee
 */
public class FlowWizardPage1 extends WizardNewFileCreationPage {

	private IWorkbench workbench;
	private static int exampleCount = 1;

	public FlowWizardPage1(IWorkbench aWorkbench, IStructuredSelection selection) {
		super("sampleFlowPage1", selection);
		this.setTitle("Create Flow Example File");
		this.setDescription("Create a new flow file resource");
		this.setImageDescriptor(ImageDescriptor.createFromFile(LatticeEditorPlugin.class, "images/flowbanner.gif"));
		this.workbench = aWorkbench;
	}

	/**
	 * @see org.eclipse.jface.dialogs.IDialogPage#createControl(org.eclipse.swt.widgets.Composite)
	 */
	public void createControl(Composite parent) {
		super.createControl(parent);
		this.setFileName("flowExample" + exampleCount + ".flow");
		setPageComplete(validatePage());
	}

	private ActivityDiagram createWakeupModel() {
		ActivityDiagram diagram = new ActivityDiagram();
		SequentialActivity wakeup = new SequentialActivity();
		Activity backToSleep = new Activity("Go back to sleep");
		Activity turnOff = new Activity("Turn off alarm");
		wakeup.setName("Wake up");
		wakeup.addChild(new Activity("Hit snooze button"));
		wakeup.addChild(backToSleep);
		wakeup.addChild(turnOff);
		wakeup.addChild(new Activity("Get out of bed"));
		diagram.addChild(wakeup);

		SequentialActivity bathroom = new SequentialActivity();
		bathroom.addChild(new Activity("Brush teeth"));
		bathroom.addChild(new Activity("Take shower"));
		bathroom.addChild(new Activity("Comb hair"));
		bathroom.setName("Bathroom activities");
		diagram.addChild(bathroom);

		ParallelActivity relaxation = new ParallelActivity();
		relaxation.addChild(new Activity("Watch cartoons"));
		relaxation.addChild(new Activity("Power Yoga"));
		relaxation.setName("Morning relaxation ritual");
		diagram.addChild(relaxation);

		Activity sleep, alarm, alarm2, clothes, spare, no, yes, drive;
		diagram.addChild(sleep = new Activity("Sleep....."));
		diagram.addChild(alarm = new Activity("Alarm!!!"));
		diagram.addChild(alarm2 = new Activity("Alarm!!!"));
		diagram.addChild(clothes = new Activity("Put on clothes"));
		diagram.addChild(spare = new Activity("Is there time to spare?"));
		diagram.addChild(yes = new Activity("YES"));
		diagram.addChild(no = new Activity("NO"));
		diagram.addChild(drive = new Activity("Drive to work"));

		new Transition(sleep, alarm);
		new Transition(alarm, wakeup);
		new Transition(backToSleep, alarm2);
		new Transition(alarm2, turnOff);
		new Transition(wakeup, bathroom);
		new Transition(bathroom, clothes);
		new Transition(clothes, spare);
		new Transition(spare, yes);
		new Transition(spare, no);
		new Transition(yes, relaxation);
		new Transition(no, drive);
		new Transition(relaxation, drive);
		return diagram;
	}

	protected InputStream getInitialContents() {
		ActivityDiagram diag = createWakeupModel();
		ByteArrayInputStream bais = null;
		try {
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			ObjectOutputStream oos = new ObjectOutputStream(baos);
			oos.writeObject(diag);
			oos.flush();
			oos.close();
			baos.close();
			bais = new ByteArrayInputStream(baos.toByteArray());
			bais.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return bais;
	}

	public boolean finish() {
		IFile newFile = createNewFile();
		if (newFile == null)
			return false; // ie.- creation was unsuccessful

		// Since the file resource was created fine, open it for editing
		// iff requested by the user
		try {
			IWorkbenchWindow dwindow = workbench.getActiveWorkbenchWindow();
			IWorkbenchPage page = dwindow.getActivePage();
			if (page != null)
				IDE.openEditor(page, newFile, true);
		} catch (org.eclipse.ui.PartInitException e) {
			e.printStackTrace();
			return false;
		}
		exampleCount++;
		return true;
	}
}
