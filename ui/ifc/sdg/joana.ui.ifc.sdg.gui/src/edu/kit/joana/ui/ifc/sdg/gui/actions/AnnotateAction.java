/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.ui.ifc.sdg.gui.actions;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.TextSelection;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ui.dialogs.SelectionDialog;
import org.eclipse.ui.texteditor.ITextEditor;

import edu.kit.joana.ifc.sdg.lattice.IEditableLattice;
import edu.kit.joana.ifc.sdg.lattice.LatticeUtil;
import edu.kit.joana.ifc.sdg.lattice.WrongLatticeDefinitionException;
import edu.kit.joana.ui.ifc.sdg.gui.ConfigurationException;
import edu.kit.joana.ui.ifc.sdg.gui.NJSecPlugin;
import edu.kit.joana.ui.ifc.sdg.gui.launching.ConfigReader;
import edu.kit.joana.ui.ifc.sdg.gui.launching.LaunchConfigurationTools;
import edu.kit.joana.ui.ifc.sdg.gui.marker.MarkerManager;

public abstract class AnnotateAction extends MarkerAction {
    protected int offset = 0;
    protected int line = 0;
    protected int length = 0;
    protected String selText = "";
    protected String secclass = "";
    protected int sc;
    protected int ec;

    private IEditableLattice<String> l;
    private IMarker marker;

    /* To be overridden by subclasses */
    abstract protected SelectionDialog createDialog(IEditableLattice<String> l);

    abstract protected void evaluateDialog(SelectionDialog dlg);

    abstract protected IMarker createMarker() throws CoreException;


    protected boolean preconditions() {
        // Ohne Lattice macht es keinen Sinn, weiterzumachen.
        l = openLattice(project);
        return l != null;
    }

	protected boolean concreteAction() throws CoreException {
		// dynamic dispatch
		SelectionDialog dlg = createDialog(l);

		dlg.open();

		// dynamic dispatch
		evaluateDialog(dlg);

		// Falls der Dialog abgebrochen wurde, nichts tun.
		if (secclass == null || secclass.equals("")) {
			return false;

		} else {
            // dynamic dispatch
            marker = createMarker();
			return true;
		}
	}

    protected IWorkspaceRunnable createWorkspaceRunnable() {
        return new IWorkspaceRunnable() {
            public void run(IProgressMonitor monitor) throws CoreException {
            	MarkerManager.singleton().addMarker(project, marker);
            }
        };
    }

	protected String getErrorMessage() {
		return "Couldn't create NJSecAnnotationMarker";
	}

    private IEditableLattice<String> openLattice(IProject ip) {
        IEditableLattice<String> lattice = null;

        try {
            ConfigReader cr = new ConfigReader(LaunchConfigurationTools.getStandardLaunchConfiguration(ip));
            String loc = cr.getLatticeLocation();
            if (loc == null) throw new ConfigurationException("No Lattice available. Maybe no NJSec-Standard-Launch-Configuration available?");
            File latFile = new File(loc);
            if (!latFile.exists()) throw new ConfigurationException("No Lattice available. Maybe no NJSec-Standard-Launch-Configuration available?");;
            lattice = LatticeUtil.loadLattice(new FileInputStream(latFile));

        } catch (CoreException ce) {
            NJSecPlugin.singleton().showError("Problem getting Lattice-File-Location", null, ce);

        } catch (IOException e1) {
            IStatus status = new Status(IStatus.ERROR, NJSecPlugin.singleton().getSymbolicName(), 0, "Couldn't read Projects Lattice", e1);
            NJSecPlugin.singleton().showError("Couldn't read Projects Lattice (defined in \"Project Properties -> NJSec\")", status, null);

        } catch (WrongLatticeDefinitionException e) {
            IStatus status = new Status(IStatus.ERROR, NJSecPlugin.singleton().getSymbolicName(), 0, "Invalid Lattice Definition", e);
            NJSecPlugin.singleton().showError("Invalid Lattice Definition", status, null);

        } catch (ConfigurationException e) {
            IStatus status = new Status(IStatus.ERROR, NJSecPlugin.singleton().getSymbolicName(), 0, e.getMessage(), e);
            NJSecPlugin.singleton().showError(e.getMessage(), status, null);
        }

        return lattice;
    }

    public void selectionChanged(ISelection selection) {
        //Decide which kind of selection it is
        if (selection != null && NJSecPlugin.singleton().getActivePage() != null && selection instanceof TextSelection) {
            TextSelection ims = (TextSelection) selection;
            if (!(NJSecPlugin.singleton().getActivePage().getActiveEditor() instanceof ITextEditor)) {
            	return;
            }
            ITextEditor editor = (ITextEditor) NJSecPlugin.singleton().getActivePage().getActiveEditor();
    		IDocument document = editor.getDocumentProvider().getDocument(editor.getEditorInput());
            selText = ims.getText();
            line = ims.getStartLine()+1;
            offset = ims.getOffset();
            try {
				sc = ims.getOffset() - document.getLineOffset(line - 1) + 1;
			} catch (BadLocationException e) {
			};
            length = ims.getLength();
            ec = sc + length;
        }
    }

    @Override
    public IMarker getMarker() {
    	return marker;
    }
}

