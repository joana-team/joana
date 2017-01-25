/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
/*
 * @(c)AdjustmentsDialog.java
 *
 * Project: GraphViewer
 *
 * Chair for Softwaresystems
 * Faculty of Informatics and Mathematics
 * University of Passau
 *
 * Created on 02.11.2004 at 15:38:23
 */
package edu.kit.joana.ui.ifc.sdg.graphviewer.view;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;

import javax.swing.JScrollPane;

import edu.kit.joana.ui.ifc.sdg.graphviewer.translation.BundleConstants;
import edu.kit.joana.ui.ifc.sdg.graphviewer.translation.Resource;
import edu.kit.joana.ui.ifc.sdg.graphviewer.view.component.GVButton;
import edu.kit.joana.ui.ifc.sdg.graphviewer.view.component.GVDialog;
import edu.kit.joana.ui.ifc.sdg.graphviewer.view.component.GVFrame;
import edu.kit.joana.ui.ifc.sdg.graphviewer.view.component.GVPanel;
import edu.kit.joana.ui.ifc.sdg.graphviewer.view.component.GVTabbedPane;

/**
 * A dialog where the user can make adjustments. It contains the tabbed pane
 * where several adjustment tabs can be included, for example to change the
 * language or the colour of a vertex.
 *
 * @author <a href="mailto:wellner@fmi.uni-passau.de">Tobias Wellner </a>
 * @version 1.0
 */
public class AdjustmentsDialog extends GVPanel implements ActionListener, BundleConstants {

	private static final long serialVersionUID = -6871581964936460384L;

	protected List<Adjustable> adjustables = new Vector<Adjustable>();

    public static final int ERROR_OPTION = -1;

    public static final int OK_OPTION = 0;

    public static final int CANCEL_OPTION = 2;

    private static final String OK_ACTION = "ok";

    private static final String APPLY_ACTION = "apply";

    private static final String CANCEL_ACTION = "cancel";

    private static final int WIDTH = 500;

    private static final int HEIGHT = 400;

    private int returnValue = ERROR_OPTION;

    protected GVTabbedPane dialogPane = null;

    protected GVFrame parent = null;

    protected GVDialog dialog = null;

    /**
     * Constructs a new <code>AdjustmentsDialog</code> object.
     */
    public AdjustmentsDialog(MainFrame parent) {
        super(parent.getTranslator(), new BorderLayout());
        this.parent = parent;
        this.initComponents();
    }

    public int showAdjustmentDialog() {
        this.dialog = new GVDialog(this.getTranslator(), this.parent,
                new Resource(MAIN_FRAME_BUNDLE, "adjustments.dialog.title"),
                true);
        this.dialog.setComponentOrientation(this.getComponentOrientation());
        this.dialog.getContentPane().setLayout(new BorderLayout());
        this.dialog.getContentPane().add(this, BorderLayout.CENTER);
        this.dialog.setSize(new Dimension(WIDTH, HEIGHT));
        this.dialog.setLocationRelativeTo(this.parent);
        this.dialog.addWindowListener(new WindowAdapter() {
            @Override
			public void windowClosing(WindowEvent e) {
                returnValue = CANCEL_OPTION;
            }
        });
        this.returnValue = ERROR_OPTION;
        this.dialog.setVisible(true);
        this.dialog.dispose();
        this.dialog = null;
        return this.returnValue;
    }

    private void initComponents() {

        this.dialogPane = new GVTabbedPane(this.getTranslator());
        this.add(dialogPane, BorderLayout.CENTER);

        GVPanel buttonPanel = new GVPanel(this.getTranslator(), new FlowLayout(
                FlowLayout.RIGHT));
        this.add(buttonPanel, BorderLayout.SOUTH);

        GVButton okButton = new GVButton(this.getTranslator(), new Resource(
                MAIN_FRAME_BUNDLE, "ok.label"));
        okButton.addActionListener(this);
        okButton.setActionCommand(OK_ACTION);
        buttonPanel.add(okButton);

        GVButton applyButton = new GVButton(this.getTranslator(), new Resource(
                MAIN_FRAME_BUNDLE, "apply.label"));
        applyButton.addActionListener(this);
        applyButton.setActionCommand(APPLY_ACTION);
        buttonPanel.add(applyButton);

        GVButton cancelButton = new GVButton(this.getTranslator(),
                new Resource(MAIN_FRAME_BUNDLE, "cancel.label"));
        cancelButton.addActionListener(this);
        cancelButton.setActionCommand(CANCEL_ACTION);
        buttonPanel.add(cancelButton);

    }

    public void addAdjustable(Adjustable adj) {
        this.adjustables.add(adj);
        adj.getAdjustmentDialog().setTranslator(this.translator);
        this.dialogPane.addTab(adj.getKeyResource(), new JScrollPane(adj
                .getAdjustmentDialog()));
    }

    public void removeAdjustable(Adjustable adj) {
        this.dialogPane.removeTabAt(this.adjustables.indexOf(adj));
        this.adjustables.remove(adj);
    }

    /**
     * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
     */
    public void actionPerformed(ActionEvent event) {
        if (event.getActionCommand().equals(CANCEL_ACTION)) {
            this.dialog.setVisible(false);
            this.returnValue = CANCEL_OPTION;
            for (Iterator<Adjustable> i = adjustables.iterator(); i.hasNext();) {
                i.next().adjustmentPerformed(false);
            }
        } else if (event.getActionCommand().equals(APPLY_ACTION)) {
            this.returnValue = OK_OPTION;
            for (Iterator<Adjustable> i = adjustables.iterator(); i.hasNext();) {
                i.next().adjustmentPerformed(true);
            }
        } else if (event.getActionCommand().equals(OK_ACTION)) {
            this.dialog.setVisible(false);
            this.returnValue = OK_OPTION;
            for (Iterator<Adjustable> i = adjustables.iterator(); i.hasNext();) {
                i.next().adjustmentPerformed(true);
            }
        } else {
            throw new Error("Error: Invalid option.");
        }
    }

}
