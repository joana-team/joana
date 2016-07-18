/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
/*
 * ExportAction.java
 *
 * Created on 7. Februar 2006, 11:32
 */

package edu.kit.joana.ui.ifc.sdg.graphviewer.controller;

import edu.kit.joana.ui.ifc.sdg.graphviewer.translation.Resource;
import edu.kit.joana.ui.ifc.sdg.graphviewer.view.Export;
import edu.kit.joana.ui.ifc.sdg.graphviewer.view.GraphPane;
import edu.kit.joana.ui.ifc.sdg.graphviewer.view.component.GVFileChooser;
import edu.kit.joana.ui.ifc.sdg.graphviewer.view.component.GVFrame;
import edu.kit.joana.ui.ifc.sdg.graphviewer.view.component.GVOptionPane;

import java.awt.event.ActionEvent;
import java.io.File;
import java.io.IOException;

import javax.swing.JFileChooser;
import javax.swing.filechooser.FileFilter;

import org.jgraph.JGraph;

/**
 * This is an action for exporting a PDG as image.
 * @author Siegfried Weber
 */
public class ExportAction extends AbstractGVAction {
	private static final long serialVersionUID = -7557842109364389705L;
	/**
     * a file chooser
     */
    private static GVFileChooser fileChooser;
    /**
     * the main frame
     */
    private GVFrame frame;
    /**
     * the graph pane
     */
    private GraphPane graphPane;

    /**
     * Creates a new instance of ExportAction
     * @param manager the command manager
     * @param receiver the command receiver
     * @param frame the main frame
     * @param graphPane the graph pane
     */
    public ExportAction(GVFrame frame, GraphPane graphPane) {
        super("export.name", "export.description");
        this.frame = frame;
        this.graphPane = graphPane;

        if(fileChooser == null) {
            fileChooser = new GVFileChooser(frame.getTranslator(),
                    new Resource(ACTIONS_BUNDLE, "export.dialog.title"));
            fileChooser.setAcceptAllFileFilterUsed(false);
            fileChooser.setFileFilter(new FileFilter() {
                @Override
				public boolean accept(File file) {
                    return file.isDirectory() ||
                            file.getName().toLowerCase().endsWith(".png");
                }

                @Override
				public String getDescription() {
                    return "PNG";
                }
            });
        }
    }

    /**
     * Performs the action.
     * @param e an action event
     */
    public void actionPerformed(ActionEvent e) {
        if(fileChooser.showSaveDialog(frame) == JFileChooser.APPROVE_OPTION) {
            JGraph graph = graphPane.getSelectedJGraph();
	        try {
	            Export.export(graph, fileChooser.getSelectedFile());
//	            return new CommandStatusEvent(this, CommandStatusEvent.SUCCESS,
//	                    new Resource(COMMANDS_BUNDLE,
//	                    "exportMethod.success.status"));
	        }
	        catch(OutOfMemoryError err) {
				GVOptionPane optionPane = new GVOptionPane(frame);
				optionPane.showErrorDialog(new Resource(ACTIONS_BUNDLE,
						"export.io_error.message", "\n"+err.getLocalizedMessage()));
	        }
//	        return new CommandStatusEvent(this, CommandStatusEvent.FAILURE,
//	                new Resource(COMMANDS_BUNDLE,
//	                "exportMethod.outOfMemory.status"));
			catch (IOException e1) {
				GVOptionPane optionPane = new GVOptionPane(frame);
				optionPane.showErrorDialog(new Resource(ACTIONS_BUNDLE,
						"export.io_error.message", "\n"+e1.getLocalizedMessage()));
			}
        }
    }
}
