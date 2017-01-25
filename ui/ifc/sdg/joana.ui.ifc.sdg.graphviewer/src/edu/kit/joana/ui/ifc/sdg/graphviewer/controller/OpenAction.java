/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
/*
 * @(c)OpenAction.java
 *
 * Project: GraphViewer
 *
 * Chair for Softwaresystems
 * Faculty of Informatics and Mathematics
 * University of Passau
 *
 * Created on 13.12.2004 at 17:44:21
 */
package edu.kit.joana.ui.ifc.sdg.graphviewer.controller;

import java.awt.event.ActionEvent;

import javax.swing.JFileChooser;

import edu.kit.joana.ui.ifc.sdg.graphviewer.model.GraphViewerModel;
import edu.kit.joana.ui.ifc.sdg.graphviewer.translation.BundleConstants;
import edu.kit.joana.ui.ifc.sdg.graphviewer.translation.Resource;
import edu.kit.joana.ui.ifc.sdg.graphviewer.view.component.GVFileChooser;
import edu.kit.joana.ui.ifc.sdg.graphviewer.view.component.GVFrame;

/**
 * @author <a href="mailto:wellner@fmi.uni-passau.de">Tobias Wellner </a>
 * @version 1.0
 */
public class OpenAction extends AbstractGVAction implements BundleConstants {
	private static final long serialVersionUID = -5465148006219276706L;
	private static GVFileChooser fileChooser = null;

	private GraphViewerModel model;
    private GVFrame frame = null;
    private String fileName=null;


	/**
     * Constructs a new <code>OpenAction</code> object.
     */
    public OpenAction(GraphViewerModel model, GVFrame frame) {
        super("open.name", "Open.png", "open.description", "open");
        this.frame = frame;
        this.model = model;
    }

    public GVFileChooser getFileChooser() {
        if (fileChooser == null) {
            fileChooser = new GVFileChooser(this.frame.getTranslator(),
                    new Resource(ACTIONS_BUNDLE, "open.dialog.title"));
            fileChooser.setAcceptAllFileFilterUsed(false);
            fileChooser.setFileFilter(new PDGFilter(this.frame.getTranslator()));
        }
        return fileChooser;
    }

    /**
     * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
     */
    public void actionPerformed(ActionEvent event) {
        if (this.getFileChooser().showOpenDialog(frame) == JFileChooser.APPROVE_OPTION) {
            model.openSDG(getFileChooser().getSelectedFile());
        }
//        return new CommandStatusEvent(this, CommandStatusEvent.SUCCESS,
//				new Resource(COMMANDS_BUNDLE, "open.success.status"));
//		return new CommandStatusEvent(this, CommandStatusEvent.SUCCESS,
//				new Resource(COMMANDS_BUNDLE, "open.failure.status"));
//		return new CommandStatusEvent(this, CommandStatusEvent.SUCCESS,
//				new Resource(COMMANDS_BUNDLE, "open.abort.status"));
    }

	public void setFileName(String fileName) {
		this.fileName=fileName;

	}
	public String getFileName() {
		return fileName;
	}
}
