/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.ui.ifc.sdg.graphviewer.view;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JTextField;
import javax.swing.SwingConstants;

import edu.kit.joana.ui.ifc.sdg.graphviewer.model.CallGraph;
import edu.kit.joana.ui.ifc.sdg.graphviewer.model.Graph;
import edu.kit.joana.ui.ifc.sdg.graphviewer.translation.BundleConstants;
import edu.kit.joana.ui.ifc.sdg.graphviewer.translation.Resource;
import edu.kit.joana.ui.ifc.sdg.graphviewer.view.component.GVButton;
import edu.kit.joana.ui.ifc.sdg.graphviewer.view.component.GVDialog;
import edu.kit.joana.ui.ifc.sdg.graphviewer.view.component.GVLabel;
import edu.kit.joana.ui.ifc.sdg.graphviewer.view.component.GVPanel;

public class HideNodeDialog extends GVPanel implements BundleConstants, ActionListener {
	private static final long serialVersionUID = 3332528750892979739L;

	protected MainFrame parent = null;
	protected JTextField tfPackegName = null;
	protected GVDialog dialog = null;
	public static final int CANCEL_OPTION = 2;
	public static final int ERROR_OPTION = -1;

	public HideNodeDialog(MainFrame parent) {
		super(parent.getTranslator());
		this.parent = parent;
		this.initComponents();
	}

	private void initComponents() {
		GVLabel txt1 = new GVLabel(this.getTranslator(), new Resource(MAIN_FRAME_BUNDLE, "hidenode.text1"));
		txt1.setHorizontalAlignment(SwingConstants.CENTER);
		GVLabel txt2 = new GVLabel(this.getTranslator(), new Resource(MAIN_FRAME_BUNDLE, "hidenode.text2"));
		txt2.setHorizontalAlignment(SwingConstants.CENTER);

		tfPackegName = new JTextField("", 30);
		tfPackegName.setToolTipText("name");
		tfPackegName.setHorizontalAlignment(SwingConstants.RIGHT);
		tfPackegName.addActionListener(this);

		GVPanel panel_input = new GVPanel(this.getTranslator(), new GridLayout(
				3, 1, 10, 10));
		this.add(panel_input, BorderLayout.CENTER);
		// panel_input.add(jlPackegName,FlowLayout.LEFT);
		panel_input.add(txt1);
		panel_input.add(txt2);
		panel_input.add(tfPackegName);

		GVPanel buttonPanel = new GVPanel(this.getTranslator(), new FlowLayout(
				FlowLayout.RIGHT));
		this.add(buttonPanel, BorderLayout.SOUTH);
		GVButton lookupButton = new GVButton(this.getTranslator(),
				new Resource(MAIN_FRAME_BUNDLE, "hidenode.label"));
		lookupButton.addActionListener(this);
		buttonPanel.add(lookupButton);

	}

	public void actionPerformed(ActionEvent e) {
		String packageName = tfPackegName.getText();
//		ActionMap actions = this.parent.getActions();
//		OpenAction openAction = (OpenAction) actions.get(OpenAction.class);

		Graph g = parent.getGraphPane().getSelectedGraph();
		parent.getModel().hideNode((CallGraph) g, packageName);

//		this.parent.getCommandManager().invoke(
//				new HideNodeCommand(openAction, this.parent.model, packageName, this.parent));

		tfPackegName.setText("");
		// this.parent.model.removeGraph(this.title);
		this.dialog.setVisible(false);

//		return new CommandStatusEvent(this, CommandStatusEvent.SUCCESS,
//				new Resource(COMMANDS_BUNDLE, "open.success.status"));
//		return new CommandStatusEvent(this, CommandStatusEvent.SUCCESS,
//				new Resource(COMMANDS_BUNDLE, "open.failure.status"));
	}

	public void showHideNodeDialog(GraphPane graphPane) {
		this.dialog = new GVDialog(this.getTranslator(), this.parent,
				new Resource(MAIN_FRAME_BUNDLE, "hidenode.dialog.title"), true);
		this.dialog.setComponentOrientation(this.getComponentOrientation());
		this.dialog.getContentPane().setLayout(new BorderLayout());
		this.dialog.getContentPane().add(this, BorderLayout.CENTER);
		this.dialog.setSize(new Dimension(360, 155));
		this.dialog.setLocationRelativeTo(this.parent);

		this.dialog.setVisible(true);
		this.dialog.dispose();
		this.dialog = null;
	}
}
