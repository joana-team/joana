/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
/*******************************************************************************
 * Copyright (c) 2000, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package edu.kit.joana.ui.ifc.sdg.gui.views;

import java.util.ArrayList;
import java.util.List;


import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.draw2d.ColorConstants;
import org.eclipse.gef.DefaultEditDomain;
import org.eclipse.gef.EditDomain;
import org.eclipse.gef.EditPart;
import org.eclipse.gef.EditPartViewer;
import org.eclipse.gef.GraphicalViewer;
import org.eclipse.gef.SelectionManager;
import org.eclipse.gef.editparts.ScalableRootEditPart;
import org.eclipse.gef.ui.parts.ScrollingGraphicalViewer;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.IPropertyListener;
import org.eclipse.ui.IWorkbenchPartSite;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.dialogs.SelectionDialog;

import edu.kit.joana.ifc.sdg.lattice.IEditableLattice;
import edu.kit.joana.ui.ifc.sdg.latticeeditor.model.Activity;
import edu.kit.joana.ui.ifc.sdg.latticeeditor.model.ActivityDiagram;
import edu.kit.joana.ui.ifc.sdg.latticeeditor.model.Transition;
import edu.kit.joana.ui.ifc.sdg.latticeeditor.parts.ActivityPart;
import edu.kit.joana.ui.ifc.sdg.latticeeditor.parts.ActivityPartFactory;
import edu.kit.joana.ui.ifc.sdg.latticeeditor.parts.TransitionPart;

/**
 * A dialog that shows a Lattice in its graphical form.
 *
 * @author Martin S.
 */
public class LatticeDialog extends SelectionDialog implements IEditorPart {
	protected GraphicalViewer viewer;
	protected EditDomain domain;
	protected ActivityDiagram diagram;

	protected String label1text;
	private boolean fAddCancelButton = true;

	//Nur einzelene Selektion zulassen und keine Kanten
	protected class SelMan extends SelectionManager {

		private List<EditPart> selection;
		private EditPartViewer viewer;
		private Runnable notifier;

		public SelMan() { }

		public void appendSelection(EditPart editpart) {
			if (editpart instanceof TransitionPart) {
				return;
			}
			if (editpart != getFocus())
				viewer.setFocus(null);
			if (!selection.isEmpty()) {
				for (Object p : selection) {
					((EditPart) p).setSelected(EditPart.SELECTED_NONE);
				}
			}
			selection.clear();
			selection.add(editpart);
			editpart.setSelected(EditPart.SELECTED_PRIMARY);

			notifier.run(); //fireSelectionChanged
		}

		@SuppressWarnings("unchecked")
		public void internalInitialize(EditPartViewer viewer, List selection, Runnable notifier) {
			this.viewer = viewer;
			this.selection = selection;
			this.notifier = notifier;

			hookViewer(viewer);
		}

		public ISelection getSelection() {
			if (selection.isEmpty())
				return new StructuredSelection();
			return new StructuredSelection(selection);
		}

		@SuppressWarnings("unchecked")
		public void setSelection(ISelection newSelection) {
			if (!(newSelection instanceof IStructuredSelection))
				return;

			List orderedSelection = ((IStructuredSelection)newSelection).toList();

			setFocus(null);
			for (int i = 0; i < selection.size(); i++) {
				EditPart part = selection.get(i);
				part.setSelected(EditPart.SELECTED_NONE);
			}
			selection.clear();

			if (!orderedSelection.isEmpty()) {
				EditPart part = (EditPart) orderedSelection.get(0);
				selection.add(part);
				part.setSelected(EditPart.SELECTED_PRIMARY);
			}
			notifier.run(); //fireSelectionChanged
		}
	}

	/**
	 * Create a new instance of the receiver with parent shell of parent.
	 * @param parent
	 */
	public LatticeDialog(Shell parent) {
		super(parent);
		diagram = new ActivityDiagram();
	}

	//Fuegt die Knoten und Kanten durch Tiefensuche hinzu
	public void setLattice(IEditableLattice<String> lattice) {
		diagram = new ActivityDiagram();
		Activity bottom = new Activity(lattice.getBottom());
		diagram.addChild(bottom);
		addNodes(bottom, lattice);
		addTransitions(bottom, lattice);
	}

	private void addNodes(Activity child, IEditableLattice<String> lattice) {
		ArrayList<String> greater = new ArrayList<String>(lattice.getImmediatelyGreater(child.getName()));
		for (int i = 0; i < greater.size(); ++i) {
			Activity parent = new Activity(greater.get(i));
			if (!diagram.hasChild(parent.getName())) {
				diagram.addChild(parent);
				addNodes(parent, lattice);
			}
		}
	}

	private void addTransitions(Activity child, IEditableLattice<String> lattice) {
		ArrayList<String> greater = new ArrayList<String>(lattice.getImmediatelyGreater(child.getName()));
		for (int i = 0; i < greater.size(); ++i) {
			Activity parent = diagram.getChild(greater.get(i));
			new Transition(parent, child);
			addTransitions(parent, lattice);
		}
	}

	/**
	 *@param addCancelButton if <code>true</code> there will be a cancel
	 * button.
	 */
	public void setAddCancelButton(boolean addCancelButton) {
		fAddCancelButton = addCancelButton;
	}

	@Override
	protected void createButtonsForButtonBar(Composite parent) {
		createButton(parent, IDialogConstants.OK_ID,
				IDialogConstants.OK_LABEL, true);
		if (fAddCancelButton) {
			createButton(parent, IDialogConstants.CANCEL_ID,
				IDialogConstants.CANCEL_LABEL, true);
		}
	}

	@Override
	protected Control createDialogArea(Composite container) {
	    Composite parent = (Composite) super.createDialogArea(container);

	    Label label1 = new Label(parent, SWT.LEAD);
	    label1.setText(label1text);

	    viewer = new ScrollingGraphicalViewer();
	    viewer.setRootEditPart(new ScalableRootEditPart());
	    domain = new DefaultEditDomain(this);
	    domain.addViewer(viewer);
	    viewer.createControl(parent);
	    viewer.setEditPartFactory(new ActivityPartFactory());
	    viewer.getControl().setBackground(ColorConstants.listBackground);
	    viewer.setSelectionManager(new SelMan());
	    viewer.setContents(diagram);

	    return parent;
	}

	protected void okPressed() {
		if (viewer.getSelection().isEmpty()) {
			super.okPressed();
		}
		String result = ((Activity) ((ActivityPart) ((IStructuredSelection) viewer.getSelection()).getFirstElement()).getModel()).getName();
		ArrayList<String> res = new ArrayList<String>(1);
		res.add(result);
		setResult(res);
	    super.okPressed();
	}

	protected void parentOK() {
		super.okPressed();
	}

	@Override
	public void setMessage(String message) {
		this.label1text = message;
	}

	public IEditorInput getEditorInput() {
		return null;
	}

	public IEditorSite getEditorSite() {
		return null;
	}

	public void init(IEditorSite site, IEditorInput input)
			throws PartInitException {

	}

	public void addPropertyListener(IPropertyListener listener) {

	}

	public void createPartControl(Composite parent) {

	}

	public void dispose() {

	}

	public IWorkbenchPartSite getSite() {
		return null;
	}

	public String getTitle() {
		return null;
	}

	public Image getTitleImage() {
		return null;
	}

	public String getTitleToolTip() {
		return null;
	}

	public void removePropertyListener(IPropertyListener listener) {

	}

	public void setFocus() {

	}

	@SuppressWarnings("unchecked")
	public Object getAdapter(Class adapter) {
		return null;
	}

	public void doSave(IProgressMonitor monitor) {

	}

	public void doSaveAs() {

	}

	public boolean isDirty() {
		return false;
	}

	public boolean isSaveAsAllowed() {
		return false;
	}

	public boolean isSaveOnCloseNeeded() {
		return false;
	}
}
