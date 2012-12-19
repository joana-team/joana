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

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.EventObject;
import java.util.List;


import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.gef.ContextMenuProvider;
import org.eclipse.gef.DefaultEditDomain;
import org.eclipse.gef.KeyHandler;
import org.eclipse.gef.KeyStroke;
import org.eclipse.gef.dnd.TemplateTransferDragSourceListener;
import org.eclipse.gef.editparts.ScalableRootEditPart;
import org.eclipse.gef.palette.PaletteRoot;
import org.eclipse.gef.tools.ConnectionCreationTool;
import org.eclipse.gef.ui.actions.ActionRegistry;
import org.eclipse.gef.ui.actions.DirectEditAction;
import org.eclipse.gef.ui.actions.GEFActionConstants;
import org.eclipse.gef.ui.parts.GraphicalEditorWithPalette;
import org.eclipse.gef.ui.parts.GraphicalViewerKeyHandler;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.swt.SWT;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.actions.ActionFactory;
import org.eclipse.ui.actions.WorkspaceModifyOperation;
import org.eclipse.ui.dialogs.SaveAsDialog;
import org.eclipse.ui.part.FileEditorInput;

import edu.kit.joana.ifc.sdg.lattice.IEditableLattice;
import edu.kit.joana.ifc.sdg.lattice.LatticeUtil;
import edu.kit.joana.ifc.sdg.lattice.WrongLatticeDefinitionException;
import edu.kit.joana.ui.ifc.sdg.latticeeditor.LatticeChangedListener;
import edu.kit.joana.ui.ifc.sdg.latticeeditor.LatticeEditorPaletteFactory;
import edu.kit.joana.ui.ifc.sdg.latticeeditor.LatticeEditorPlugin;
import edu.kit.joana.ui.ifc.sdg.latticeeditor.actions.FlowContextMenuProvider;
import edu.kit.joana.ui.ifc.sdg.latticeeditor.dnd.FlowTemplateTransferDropTargetListener;
import edu.kit.joana.ui.ifc.sdg.latticeeditor.model.Activity;
import edu.kit.joana.ui.ifc.sdg.latticeeditor.model.ActivityDiagram;
import edu.kit.joana.ui.ifc.sdg.latticeeditor.model.Transition;
import edu.kit.joana.ui.ifc.sdg.latticeeditor.parts.ActivityPartFactory;

/**
 *
 * @author hudsonr Created on Jun 27, 2003
 */
public class LatticeEditor extends GraphicalEditorWithPalette {

	ActivityDiagram diagram;
	private PaletteRoot root;
	private KeyHandler sharedKeyHandler;
	private boolean savePreviouslyNeeded = false;
	protected List<LatticeChangedListener<String>> changeListeners = new ArrayList<LatticeChangedListener<String>>();

	public LatticeEditor() {
		DefaultEditDomain defaultEditDomain = new DefaultEditDomain(this);
		defaultEditDomain.setActiveTool(new ConnectionCreationTool());
		setEditDomain(defaultEditDomain);
		LatticeEditorPlugin.getDefault().registerLatticeEditor(this);

		changeListeners.add(new LatticeFileMarkListener());
	}

	public void dispose() {
		LatticeEditorPlugin.getDefault().unregisterLatticeEditor(this);
		super.dispose();
	}

	public void addLatticeChangedListener(LatticeChangedListener<String> ll) {
		if (!changeListeners.contains(ll))
			changeListeners.add(ll);
	}

	public void removeLatticeChangedListener(LatticeChangedListener<String> ll) {
		changeListeners.remove(ll);
	}

	/**
	 * @see org.eclipse.gef.commands.CommandStackListener#commandStackChanged(java.util.EventObject)
	 * @author Frank Nodes (artially)
	 */
	public void commandStackChanged(EventObject event) {
		if (isDirty()) {
			if (!savePreviouslyNeeded()) {
				setSavePreviouslyNeeded(true);
				firePropertyChange(IEditorPart.PROP_DIRTY);
			}
		} else {
			setSavePreviouslyNeeded(false);
			firePropertyChange(IEditorPart.PROP_DIRTY);
		}

		updateStatusViews();

		super.commandStackChanged(event);
		/** *(e1) Frank Nodes ** */
	}

	/**
	 *
	 */
	private void updateStatusViews() {
		String latticeSource = this.getLatSource();

		IEditableLattice<String> lattice = null;
		try {
			lattice = LatticeUtil.loadLattice(latticeSource);
		} catch (WrongLatticeDefinitionException e) {
		}

		IResource res = getEditorResource();
		ArrayList<LatticeChangedListener<String>> listeners = new ArrayList<LatticeChangedListener<String>>(changeListeners);
		new LatticeValidationJob<String>(lattice, res.getName(), res, latticeSource, listeners).schedule();
	}

	private IResource getEditorResource() {
		IResource resource = null;
		if (this.getEditorInput() instanceof IFileEditorInput) {
			IFileEditorInput fileInput = (IFileEditorInput) this.getEditorInput();
			resource = fileInput.getFile();
		}
		return resource;
	}

	/***************************************************************************
	 * @author Frank Nodes
	 * @return latticeSource corresponding to active diagram
	 */
	private String getLatSource() {
		String latticeSource = "";

		int i = 0;
		for (Activity ac : diagram.getChildren()) {
			i++;
			for (Transition tr : ac.getIncomingTransitions()) {
				Activity in = tr.source;
				latticeSource += ac.getName() + "<=" + in.getName() + "\n";
			}

			//Einzelne Knoten schreiben
			if (ac.getIncomingTransitions().isEmpty() && ac.getOutgoingTransitions().isEmpty()) {
			  latticeSource += ac.getName() + "\n";
			}
		}
		return latticeSource;
	}

	/**
	 * @see org.eclipse.gef.ui.parts.GraphicalEditor#createActions()
	 */
	@SuppressWarnings("unchecked")
	protected void createActions() {
		super.createActions();
		ActionRegistry registry = getActionRegistry();
		IAction action;

		action = new DirectEditAction((IWorkbenchPart) this);
		registry.registerAction(action);
		getSelectionActions().add(action.getId());
	}

	/**
	 * Creates an appropriate output stream and writes the activity diagram out
	 * to this stream.
	 *
	 * @param os
	 *            the base output stream
	 * @throws IOException
	 */
	protected void createOutputStream(OutputStream os) throws IOException {
		ObjectOutputStream out = new ObjectOutputStream(os);
		out.writeObject(diagram);
		out.close();
	}

	/**
	 * @see org.eclipse.gef.ui.parts.GraphicalEditor#configureGraphicalViewer()
	 */
	protected void configureGraphicalViewer() {
		super.configureGraphicalViewer();
		getGraphicalViewer().setRootEditPart(new ScalableRootEditPart());
		getGraphicalViewer().setEditPartFactory(new ActivityPartFactory());
		getGraphicalViewer().setKeyHandler(new GraphicalViewerKeyHandler(getGraphicalViewer()).setParent(getCommonKeyHandler()));

		ContextMenuProvider provider = new FlowContextMenuProvider(getGraphicalViewer(), getActionRegistry());
		getGraphicalViewer().setContextMenu(provider);
		getSite().registerContextMenu("org.eclipse.gef.latticeeditor.editor.contextmenu", //$NON-NLS-1$
				provider, getGraphicalViewer());

	}

	/**
	 * @see org.eclipse.gef.ui.parts.GraphicalEditor#initializeGraphicalViewer()
	 */
	protected void initializeGraphicalViewer() {
		getGraphicalViewer().setContents(diagram);
		getGraphicalViewer().addDropTargetListener(new FlowTemplateTransferDropTargetListener(getGraphicalViewer()));

	}

	/**
	 * @see org.eclipse.gef.ui.parts.GraphicalEditorWithPalette#initializePaletteViewer()
	 */
	protected void initializePaletteViewer() {
		super.initializePaletteViewer();
		getPaletteViewer().addDragSourceListener(new TemplateTransferDragSourceListener(getPaletteViewer()));
	}

	/**
	 * @see org.eclipse.ui.ISaveablePart#doSave(org.eclipse.core.runtime.IProgressMonitor)
	 */
	public void doSave(IProgressMonitor monitor) {

		try {

			ByteArrayOutputStream out = new ByteArrayOutputStream();
			OutputStreamWriter o2 = new OutputStreamWriter(out);
			o2.write(this.getLatSource());
			o2.close();

			IFile file = ((IFileEditorInput) getEditorInput()).getFile();
			file.setContents(new ByteArrayInputStream(out.toByteArray()), true, false, monitor);
			out.close();
			getCommandStack().markSaveLocation();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * @see org.eclipse.ui.ISaveablePart#doSaveAs()
	 */
	public void doSaveAs() {

		SaveAsDialog dialog = new SaveAsDialog(getSite().getWorkbenchWindow().getShell());
		dialog.setOriginalFile(((IFileEditorInput) getEditorInput()).getFile());
		dialog.open();
		IPath path = dialog.getResult();

		if (path == null)
			return;

		IWorkspace workspace = ResourcesPlugin.getWorkspace();
		final IFile file = workspace.getRoot().getFile(path);

		WorkspaceModifyOperation op = new WorkspaceModifyOperation() {
			public void execute(final IProgressMonitor monitor) throws CoreException {
				try {
					ByteArrayOutputStream out = new ByteArrayOutputStream();
					createOutputStream(out);
					file.create(new ByteArrayInputStream(out.toByteArray()), true, monitor);
					out.close();
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		};

		try {
			new ProgressMonitorDialog(getSite().getWorkbenchWindow().getShell()).run(false, true, op);
			setInput(new FileEditorInput(file));
			getCommandStack().markSaveLocation();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	protected KeyHandler getCommonKeyHandler() {
		if (sharedKeyHandler == null) {
			sharedKeyHandler = new KeyHandler();
			sharedKeyHandler.put(KeyStroke.getPressed(SWT.DEL, 127, 0), getActionRegistry().getAction(ActionFactory.DELETE.getId())); // GEFActionConstants.DELETE));
			sharedKeyHandler.put(KeyStroke.getPressed(SWT.F2, 0), getActionRegistry().getAction(GEFActionConstants.DIRECT_EDIT));
		}
		return sharedKeyHandler;
	}

	/**
	 * @see org.eclipse.gef.ui.parts.GraphicalEditorWithPalette#getPaletteRoot()
	 */
	protected PaletteRoot getPaletteRoot() {
		if (root == null)
			root = LatticeEditorPaletteFactory.createPalette();
		return root;
	}

	/** This method is a stub doing nothing.
	 *
	 * @param marker
	 */
	public void gotoMarker(IMarker marker) { }

	/**
	 * @see org.eclipse.ui.ISaveablePart#isDirty()
	 */
	public boolean isDirty() {
		return isSaveOnCloseNeeded();
	}

	/**
	 * @see org.eclipse.ui.ISaveablePart#isSaveAsAllowed()
	 */
	public boolean isSaveAsAllowed() {
		return true;
	}

	/**
	 * @see org.eclipse.ui.ISaveablePart#isSaveOnCloseNeeded()
	 */
	public boolean isSaveOnCloseNeeded() {
		return getCommandStack().isDirty();
	}

	private boolean savePreviouslyNeeded() {
		return savePreviouslyNeeded;
	}

	/**
	 * @see org.eclipse.ui.part.EditorPart#setInput(org.eclipse.ui.IEditorInput)
	 * @author Frank Nodes
	 */
	protected void setInput(IEditorInput input) {
		super.setInput(input);

		IFile file = ((IFileEditorInput) input).getFile();

		diagram = new ActivityDiagram();

		InputStream is = null;
		try {
			is = file.getContents(false);
		} catch (CoreException e) {
			ErrorDialog.openError(LatticeEditorPlugin.getDefault().getWorkbench().getDisplay().getActiveShell(), "Error", "Couldn't set Input for LatticeEditor", e.getStatus());
			LatticeEditorPlugin.getDefault().getLog().log(e.getStatus());
		}
		BufferedReader sr = new BufferedReader(new InputStreamReader(is));
		try {
			while (sr.ready()) {
				String readLine = sr.readLine();
				if (readLine != null && readLine.indexOf("<=") >= 0 && !readLine.startsWith("#")) {
					String lower = readLine.split("<=")[0];
					String higher = readLine.split("<=")[1];
					Activity lo = diagram.getChild(lower);
					Activity hi = diagram.getChild(higher);

					/*
					 * Elemente Top und Bottom werden als normale Elemente behandelt.
					 */
					if (lo == null)
					  diagram.addChild(lo = new Activity(lower));
					if (hi == null)
					  diagram.addChild(hi = new Activity(higher));

					new Transition(hi, lo);

				//Einzelne Elemente Zulassen
				} else if (readLine != null && !readLine.startsWith("#")) {
				  Activity ac = diagram.getChild(readLine);
				  if (ac == null) {
				    diagram.addChild(ac = new Activity(readLine));
				  }
				}
			}
		} catch (IOException e1) {
			IStatus status = new Status(IStatus.ERROR, LatticeEditorPlugin.getDefault().getBundle().getSymbolicName(), 0, "Problem reading file into LatticeEditor", e1);
			ErrorDialog.openError(LatticeEditorPlugin.getDefault().getWorkbench().getDisplay().getActiveShell(), "Error", "Problem reading file into LatticeEditor", status);
			LatticeEditorPlugin.getDefault().getLog().log(status);

		}

		this.setPartName(input.getName());

		updateStatusViews();
	}

	private void setSavePreviouslyNeeded(boolean value) {
		savePreviouslyNeeded = value;
	}

}
