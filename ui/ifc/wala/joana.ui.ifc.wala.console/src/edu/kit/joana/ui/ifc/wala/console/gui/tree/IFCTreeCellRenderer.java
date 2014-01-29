/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.ui.ifc.wala.console.gui.tree;

import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.net.URL;

import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JOptionPane;
import javax.swing.JTree;
import javax.swing.tree.DefaultTreeCellRenderer;

import edu.kit.joana.api.annotations.AnnotationType;
import edu.kit.joana.api.annotations.IFCAnnotation;
import edu.kit.joana.ui.ifc.wala.console.gui.IFCConsoleGUI;



class IconProvider {

	private IFCTreeCellRenderer renderer;

	private final Icon packageIcon;
	private final Icon classIcon;
	private final Icon attribIcon;
	private final Icon methIcon;
	private final Icon exitIcon;



	public IconProvider(IFCTreeCellRenderer renderer) {
		this.renderer = renderer;
		packageIcon = loadImageOrFallback("package_obj.gif", renderer.getDefaultClosedIcon());
		classIcon = loadImageOrFallback("class.gif", renderer.getDefaultClosedIcon());
		attribIcon = loadImageOrFallback("field_public_obj.gif", renderer.getDefaultLeafIcon());
		methIcon = loadImageOrFallback("methpub_obj.gif", renderer.getDefaultClosedIcon());
		exitIcon = loadImageOrFallback("exit.png", renderer.getDefaultLeafIcon());

		if (packageIcon == null || classIcon == null || attribIcon == null || methIcon == null || exitIcon == null) {
			JOptionPane.showMessageDialog(renderer, "Please note: At least one icon was not found!", IFCConsoleGUI.NAME_OF_APPLICATION, JOptionPane.WARNING_MESSAGE);
		}
	}

	private static Icon loadImageOrFallback(String path, Icon fallback) {
		Icon ret;
		Class<? extends IFCTreeCellRenderer> c = IFCTreeCellRenderer.class;
		ClassLoader cl = c.getClassLoader();
		URL uIcon = cl.getResource(path);
		if (uIcon != null) {
			ret = new ImageIcon(uIcon);
		} else {
			ret = fallback;
		}

		return ret;
	}

	public Icon getIcon(IFCTreeNode node) {

		switch (node.getKind()) {
		case ROOT:
			return renderer.getDefaultClosedIcon();
		case PACKAGE:
			return packageIcon;
		case CLASS:
			return classIcon;
		case ATTRIBUTE:
			return attribIcon;
		case METHOD:
			return methIcon;
		case PARAMETER:
			return renderer.getDefaultLeafIcon();
		case EXIT:
			return exitIcon;
		case INSTRUCTION:
			return renderer.getDefaultLeafIcon();
		case NONE:
			return renderer.getDefaultLeafIcon();
		default:
			throw new IllegalStateException();

		}
	}
}

public class IFCTreeCellRenderer extends DefaultTreeCellRenderer {

	private static final long serialVersionUID = 55359771367779706L;

	private final IFCConsoleGUI consoleGui;

	private IconProvider iconProvider = new IconProvider(this);

	private final Color defaultColorNotSelected;
	private final Color defaultColorSelected;

	public IFCTreeCellRenderer(IFCConsoleGUI consoleGui) {
		super();
		setFont(Font.decode(Font.MONOSPACED));
		this.consoleGui = consoleGui;
		this.defaultColorNotSelected = getBackgroundNonSelectionColor();
		this.defaultColorSelected = getBackgroundSelectionColor();
	}

	/* (non-Javadoc)
	 * @see javax.swing.tree.DefaultTreeCellRenderer#getTreeCellRendererComponent(javax.swing.JTree, java.lang.Object, boolean, boolean, boolean, int, boolean)
	 */
	@Override
	public Component getTreeCellRendererComponent(JTree tree, Object value,
			boolean sel, boolean expanded, boolean leaf, int row,
			boolean hasFocus) {

		if (!(value instanceof IFCTreeNode)) {
			throw new IllegalArgumentException();
		}

		IFCTreeNode node = (IFCTreeNode) value;

		if (leaf) {
			setLeafIcon(iconProvider.getIcon(node));
		} else if (expanded) {
			setOpenIcon(iconProvider.getIcon(node));
		} else {
			setClosedIcon(iconProvider.getIcon(node));
		}

		if (node.getIFCAnnotation() != null) {
			IFCAnnotation ann = node.getIFCAnnotation();
			if (ann.getType() == AnnotationType.SOURCE || ann.getType() == AnnotationType.SINK) {
				if (ann.getLevel1().equals(consoleGui.getLattice().getTop())) {
					setBackgroundNonSelectionColor(Color.RED);
					setBackgroundSelectionColor(Color.RED);
					setTextSelectionColor(Color.BLACK);
				} else if (ann.getLevel1().equals(consoleGui.getLattice().getBottom())) {
					setBackgroundNonSelectionColor(Color.GREEN);
					setBackgroundSelectionColor(Color.GREEN);
					setTextSelectionColor(Color.BLACK);
				} else {
					setBackgroundNonSelectionColor(defaultColorNotSelected);
					setBackgroundSelectionColor(defaultColorSelected);
				}
			}
		} else {
			setBackgroundNonSelectionColor(defaultColorNotSelected);
			setBackgroundSelectionColor(defaultColorSelected);
		}

		return super.getTreeCellRendererComponent(tree, value, sel, expanded, leaf,
				row, hasFocus);
	}

}


