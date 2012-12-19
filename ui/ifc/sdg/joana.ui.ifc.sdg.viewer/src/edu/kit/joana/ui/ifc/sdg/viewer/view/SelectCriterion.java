/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.ui.ifc.sdg.viewer.view;

import java.util.regex.Matcher;


import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.internal.ui.javaeditor.EditorUtility;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ui.IEditorActionDelegate;
import org.eclipse.ui.IEditorPart;

import edu.kit.joana.ui.ifc.sdg.viewer.view.analysisview.AnalysisView;

/** This class is a popup-menu contribution for the Java editor in eclipse.
 * A user can select a piece of code in the editor and then convert it to
 * a criterion of the chosen algorithm by activating this popup menu.
 *
 * @author giffhorn
 *
 */
@SuppressWarnings("restriction")
public class SelectCriterion implements IEditorActionDelegate {
	// the active selection
	private ISelection selection;

	/*
	 * (non-Javadoc)
	 * Needs a org.eclipse.jdt.internal.ui.javaeditor.CompilationUnitEditor
	 * @see org.eclipse.ui.IEditorActionDelegate#setActiveEditor(org.eclipse.jface.action.IAction, org.eclipse.ui.IEditorPart)
	 */
	public void setActiveEditor(IAction action, IEditorPart targetEditor) { }

    /*
     * (non-Javadoc)
     * @see org.eclipse.ui.IActionDelegate#selectionChanged(org.eclipse.jface.action.IAction, org.eclipse.jface.viewers.ISelection)
     */
    public void selectionChanged(IAction action, ISelection selection) {
        this.selection = selection;
    }

	/** This method opens a SelectCriterionDialog where a user can choose <br>
	 * which kind of criteria his selection will be.
	 * It calls method askCriterion in class AnalysisView.
	 *
	 */
	public void run(IAction action) {
		if (!selection.isEmpty()) {
		    ITextSelection txt = (ITextSelection)selection;
		    IJavaElement ije = EditorUtility.getActiveEditorJavaInput();

		    if (ije == null) {
		        throw new IllegalArgumentException("Invalid Editor in edu.kit.joana.ui.ifc.sdg.viewer.view.SelectCriterion.run()");
		    }
		    String name = getFileName(ije);

			AnalysisView.getInstance().askCriterion(txt, name);
		}
	}

    private String getFileName(IJavaElement javafile) {
        String path = "";
        IJavaElement elem = javafile;

        while (elem.getParent() != null && elem.getParent().getElementType() == IJavaElement.PACKAGE_FRAGMENT) {
            elem = elem.getParent();
            path = elem.getElementName() + path;
        }
        String refPath = path.replaceAll("[.]", Matcher.quoteReplacement(java.io.File.separator));
        String mainFile = javafile.getElementName();

        if (refPath.equals("")) {
            return mainFile;

        } else {
            return refPath + java.io.File.separator + mainFile;
        }
    }

}
