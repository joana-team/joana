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
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.NoSuchElementException;

import javax.swing.JTextField;
import javax.swing.SwingConstants;

import edu.kit.joana.ifc.sdg.graph.SDG;
import edu.kit.joana.ifc.sdg.graph.SDGNode;
import edu.kit.joana.ui.ifc.sdg.graphviewer.translation.BundleConstants;
import edu.kit.joana.ui.ifc.sdg.graphviewer.translation.Resource;
import edu.kit.joana.ui.ifc.sdg.graphviewer.view.component.GVButton;
import edu.kit.joana.ui.ifc.sdg.graphviewer.view.component.GVDialog;
import edu.kit.joana.ui.ifc.sdg.graphviewer.view.component.GVLabel;
import edu.kit.joana.ui.ifc.sdg.graphviewer.view.component.GVOptionPane;
import edu.kit.joana.ui.ifc.sdg.graphviewer.view.component.GVPanel;


public class LookupDialog extends GVPanel implements BundleConstants, ActionListener {
	private static final long serialVersionUID = -4060969760147147357L;
	public static final int ERROR_OPTION = -1;
	public static final int CANCEL_OPTION = 2;
	public static final int LOOUP_OPTION = 0;

	protected MainFrame parent = null;
	protected GVDialog dialog = null;
	protected GVPanel panel = null;
	protected JTextField tf_id = null;
	protected GVButton button = null;

//	private String graphName = null;
	@SuppressWarnings("unused")
	private int returnValue = ERROR_OPTION;
	private int id = -1;

	public LookupDialog(MainFrame parent) {
		super(parent.getTranslator());
		this.parent = parent;
		this.initComponents();
	}

	public void showLookforDialog() {
		this.dialog = new GVDialog(this.getTranslator(), this.parent,
				new Resource(MAIN_FRAME_BUNDLE, "search.dialog.title"), true);
		this.dialog.setComponentOrientation(this.getComponentOrientation());
		this.dialog.getContentPane().setLayout(new BorderLayout());
		this.dialog.getContentPane().add(this, BorderLayout.CENTER);
		this.dialog.setSize(new Dimension(360, 135));
		this.dialog.setLocationRelativeTo(this.parent);
		this.dialog.addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent e) {
				returnValue = CANCEL_OPTION;
			}
		});
		this.returnValue = ERROR_OPTION;
		this.dialog.setVisible(true);
		this.dialog.dispose();
		this.dialog = null;
	}

	private void initComponents() {
		this.setLayout(new GridBagLayout());
		GridBagConstraints c = new GridBagConstraints();
		c.fill = GridBagConstraints.BOTH;

		GVPanel searchPanelNorth = new GVPanel(this.getTranslator(), new FlowLayout());

		GVLabel jlId = new GVLabel(this.getTranslator(), new Resource(MAIN_FRAME_BUNDLE, "lookup.text"));
		jlId.setHorizontalAlignment(SwingConstants.RIGHT);

		tf_id = new JTextField();
		tf_id.setPreferredSize(new Dimension(100, 20));
		tf_id.setToolTipText("id");
		tf_id.addActionListener(this);
		searchPanelNorth.add(jlId);
		searchPanelNorth.add(tf_id);

		GVPanel searchPanelSouth = new GVPanel(this.getTranslator(), new FlowLayout());
		GVButton lookupButton = new GVButton(this.getTranslator(),
				new Resource(MAIN_FRAME_BUNDLE, "lookup.label"));
		lookupButton.addActionListener(this);
		searchPanelSouth.add(lookupButton);
		c.gridy = 0;
		this.add(searchPanelNorth, c);

		c.gridy = 1;
		c.gridheight = 1;
		this.add(searchPanelSouth,c);
	}



	public void actionPerformed(ActionEvent e) {
		try {
			setId(Integer.parseInt(tf_id.getText()));
			// graphName = parent.getGraphPane().getSelectedGraph().getName();
			// if(parent.get)
//			graphName = this.parent.getGraphPane().getTitleAt(0);
//			GraphViewerModel model = parent.getModel();
			SDG sdg = parent.getGraphPane().getSelectedGraph().getCompleteSDG();
			SDGNode sdgnode = sdg.getNode(getId());
			int procId = sdgnode.getProc();
			parent.getGraphPane().setCenterID(getId());
//			ActionMap actions = parent.getActions();
//			OpenMethodAction action = (OpenMethodAction) actions
//					.get(OpenMethodAction.class);
//			parent.getCommandManager().invoke(
//					new OpenMethodCommand(action, parent.model, procId,
//							graphName));
	    	parent.model.openPDG(parent.getGraphPane().getSelectedGraph(), procId);
//	    	return new CommandStatusEvent(this, CommandStatusEvent.SUCCESS,
//	                new Resource(COMMANDS_BUNDLE, "openMethod.success.status"));

		} catch (NumberFormatException | NoSuchElementException |
				ArrayIndexOutOfBoundsException | NullPointerException ex) {
			GVOptionPane optionPane = new GVOptionPane(this.parent);
			optionPane.showErrorDialog(new Resource(ACTIONS_BUNDLE,
					"error_nosuchid.message", " "));
		} finally {
			tf_id.setText("");
			this.dialog.setVisible(false);

		}
	}

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}
}
