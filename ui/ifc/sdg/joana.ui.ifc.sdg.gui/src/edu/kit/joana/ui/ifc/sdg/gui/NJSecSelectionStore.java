/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
/*
 * Created on 16.12.2004
 *
 */
package edu.kit.joana.ui.ifc.sdg.gui;

import java.util.ArrayList;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.internal.ui.javaeditor.IClassFileEditorInput;
import org.eclipse.jdt.ui.IWorkingCopyManager;
import org.eclipse.jdt.ui.JavaUI;
import org.eclipse.jface.text.TextSelection;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.ISelectionListener;
import org.eclipse.ui.ISelectionService;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.part.FileEditorInput;

import edu.kit.joana.util.Log;
import edu.kit.joana.util.Logger;

/**
 * Watches which project and ressource are currently selected
 * may inform Listeners of changes and offers direct query ability
 *
 * @author naxan
 *
 */
//@SuppressWarnings("restriction") // TODO: access restrictions for InternalClassFileEditorInput
public class NJSecSelectionStore implements ISelectionListener {

	private final Logger debug = Log.getLogger(Log.L_UI_DEBUG);
	private IResource activeResource;
	private ArrayList<ActiveResourceChangeListener> arlisteners = new ArrayList<ActiveResourceChangeListener>();

	public NJSecSelectionStore () {
		this.registerListener();

		if (activeResource == null) {
			setActiveResource();
		}
	}

	private boolean setActiveResource() {
		IEditorInput iei = null;
		try {
			iei = NJSecPlugin.singleton().getActivePage().getActiveEditor().getEditorInput();

		} catch (NullPointerException npe) {
			if (activeResource == null) {
				return false;
			} else {
				activeResource = null;
				return true;
			}
		}

		/**
		 * Works for CompilationUnitEditor AND ClassFileEditor
		 */
		if (iei instanceof FileEditorInput) {
			activeResource = ((FileEditorInput)iei).getFile();
			return true;

		} else if (iei instanceof IClassFileEditorInput) {
			try {
				activeResource = ((IClassFileEditorInput)iei).getClassFile().getCorrespondingResource();
			} catch (JavaModelException e) {

			}

			return true;
		}


		/**
		 * From here on nearly everthing (beside null-cases)
		 * shold be unnecessary now; but doesnt hurt :o)
		 * @deprecated
		 */
		IWorkingCopyManager mgr = JavaUI.getWorkingCopyManager();

		try {
			mgr.connect(iei);

		} catch (CoreException e) {
			NJSecPlugin.singleton().showError("Problem while creating Annotation View couldn't connect to active editor", null, e);

			if (activeResource == null) {
				return false;
			} else {
				activeResource = null;
				return true;
			}

		}

		ICompilationUnit icu = mgr.getWorkingCopy(iei);
		try {
			if (icu == null) {
				activeResource = null;
				return true;
			} else if (activeResource != icu.getCorrespondingResource()) {
				activeResource = icu.getCorrespondingResource();
				return true;
			} else {
				return false;
			}

		} catch (JavaModelException e1) {
			if (activeResource == null) {
				return false;
			} else {
				activeResource = null;
				return true;
			}
		}

	}

	private void registerListener() {
		IWorkbenchWindow iww = NJSecPlugin.singleton().getWorkbench().getActiveWorkbenchWindow();
		ISelectionService iss = iww.getSelectionService();
		iss.addSelectionListener(this);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.ISelectionListener#selectionChanged(org.eclipse.ui.IWorkbenchPart, org.eclipse.jface.viewers.ISelection)
	 */
	public void selectionChanged(IWorkbenchPart part, ISelection selection) {
		boolean change = false;
		if (selection instanceof TextSelection) {
//			TextSelection ts = (TextSelection) selection;
			change = setActiveResource(); // ||	setActiveProject();


		} else if (selection instanceof StructuredSelection) {
			StructuredSelection ss = (StructuredSelection) selection;
			if (ss.getFirstElement() instanceof IResource) {
				IResource r = (IResource) ss.getFirstElement();
				if (activeResource != r) {
					activeResource = r;
					debug.outln(activeResource.getProjectRelativePath());
					change = true;
				}
			}
		}

		if (change) {
//			System.out.println("changed Resource: " + activeResource + " in Project " + activeResource.getProject());
			notifyActiveResourceChangeListeners();
		}
		//		System.out.println("Wbp: " + part + " Selection " + selection);
	}

	public IProject getActiveProject() {
		if (activeResource == null) {
			IWorkbench wb = NJSecPlugin.singleton().getWorkbench(); if (wb == null) return null;
			IWorkbenchWindow wbw = wb.getActiveWorkbenchWindow(); if (wbw == null) return null;
			IWorkbenchPage wp = wbw.getActivePage(); if (wp == null) return null;
			IEditorPart editor = wp.getActiveEditor(); if (editor == null) return null;
			IEditorInput iei = editor.getEditorInput();

			if (iei instanceof IClassFileEditorInput) {
				IProject p = ((IClassFileEditorInput) iei).getClassFile().getJavaProject().getProject();
				return p;
			} else if (iei instanceof IFileEditorInput) {
				IFile file = ((IFileEditorInput) iei).getFile();
				return file.getProject();
			} else {
				return null;
			}

		} else {
			return activeResource.getProject();
		}
	}

	public IResource getActiveResource() {
		if (activeResource instanceof IProject) {
			debug.outln("Projekt " + activeResource + " aktiv");
		}
		return activeResource;
	}

	public void addActiveResourceChangeListener(ActiveResourceChangeListener arcl) {
		if (!arlisteners.contains(arcl)) arlisteners.add(arcl);
		arcl.activeResourceChanged(getActiveResource(), getActiveProject());
	}

	public void removeActiveResourceChangeListener(ActiveResourceChangeListener arcl) {
		arlisteners.remove(arcl);
	}

	public void notifyActiveResourceChangeListeners() {
		for (int i = 0; i < arlisteners.size(); i++) {
			arlisteners.get(i).activeResourceChanged(getActiveResource(), getActiveProject());
		}
	}
}
