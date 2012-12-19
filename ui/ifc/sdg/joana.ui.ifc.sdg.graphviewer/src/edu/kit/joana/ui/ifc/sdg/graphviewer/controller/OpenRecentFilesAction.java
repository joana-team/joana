/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.ui.ifc.sdg.graphviewer.controller;
//package edu.kit.joana.ui.ifc.sdg.graphviewer.view.action;
//
//import edu.kit.joana.ui.ifc.sdg.graphviewer.view.MainFrame;
//import edu.kit.joana.ui.ifc.sdg.graphviewer.view.OpenRecentFiles;
//import java.awt.event.MouseAdapter;
//import java.awt.event.MouseEvent;
//import java.util.LinkedList;
//import javax.swing.JMenuItem;
//
//public class OpenRecentFilesAction extends MouseAdapter {
//	private MainFrame mainFrame = null;
//	private String file=null;
//
//	public OpenRecentFilesAction(MainFrame mainFrame) {
//		this.mainFrame = mainFrame;
//	}
//
//	@Override
//	public void mouseEntered(MouseEvent e) {
//		((OpenRecentFiles) e.getSource()).removeAll();
//
//		if (this.mainFrame != null) {
//
//			LinkedList<String> files_vec = this.mainFrame.getModel()
//					.getFiles_vec();
//			if (files_vec != null) {
//				for (int i = files_vec.size()-1; i >= 0; i--) {
//					String j = files_vec.get(i);
//					JMenuItem jMenuItem = new JMenuItem(j);
//					jMenuItem.addMouseListener(new RecentFileListClickedAction(this.mainFrame, j));
//					((OpenRecentFiles) e.getSource()).add(jMenuItem);
//				}
//			}
//		}
//	}
//
//	public String getSelectedFile() {
//		return this.file;
//	}
//}
