/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.ui.ifc.sdg.gui;

import org.eclipse.ui.IFolderLayout;
import org.eclipse.ui.IPageLayout;
import org.eclipse.ui.IPerspectiveFactory;

public class NJSecPerspective implements IPerspectiveFactory {

	public void createInitialLayout(IPageLayout layout) {
		// Get the editor area.
		String editorArea = layout.getEditorArea();

		// Top left: Package Explorer view and Bookmarks view placeholder
		IFolderLayout topLeft = layout.createFolder("topLeft", IPageLayout.LEFT, 0.15f, editorArea);
		topLeft.addView("org.eclipse.jdt.ui.PackageExplorer");
		topLeft.addPlaceholder(IPageLayout.ID_BOOKMARKS);

		// Bottom left: Outline view and Property Sheet view
		IFolderLayout bottomLeft = layout.createFolder("bottomLeft", IPageLayout.BOTTOM, 0.65f, "topLeft");
		bottomLeft.addView(IPageLayout.ID_OUTLINE);
		bottomLeft.addView(IPageLayout.ID_PROP_SHEET);

		// Bottom: Task List view
		IFolderLayout bottom = layout.createFolder("bottom", IPageLayout.BOTTOM, 0.65f, editorArea);
		bottom.addView("edu.kit.joana.ifc.sdg.gui.views.AnnotationView");
		bottom.addView(IPageLayout.ID_TASK_LIST);
		bottom.addView(IPageLayout.ID_PROBLEM_VIEW);
		bottom.addView("joana.project.slicer");
		bottom.addView("joana.viewer.view.sdgview.SDGView");

		IFolderLayout bottombottom = layout.createFolder("bottombottom", IPageLayout.BOTTOM, 0.65f, "bottom");
		bottombottom.addView("edu.kit.joana.ifc.sdg.gui.views.MarkerSDGNodeView");

		IFolderLayout right = layout.createFolder("right", IPageLayout.RIGHT, 0.80f, editorArea);
		right.addView("edu.kit.joana.ifc.sdg.gui.views.ViolationView");
		layout.addView("edu.kit.joana.ifc.sdg.gui.views.ClassificationView", IPageLayout.BOTTOM, 0.50f, "right");
	}
}
