/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
/*
 * @(c)StatusBar.java
 *
 * Project: GraphViewer
 *
 * Chair for Softwaresystems
 * Faculty of Informatics and Mathematics
 * University of Passau
 *
 * Created on 28.11.2004 at 12:20:57
 */
package edu.kit.joana.ui.ifc.sdg.graphviewer.view;

import edu.kit.joana.ui.ifc.sdg.graphviewer.controller.event.CommandStatusEvent;
import edu.kit.joana.ui.ifc.sdg.graphviewer.controller.event.CommandStatusListener;
import edu.kit.joana.ui.ifc.sdg.graphviewer.view.component.GVLabel;
import edu.kit.joana.ui.ifc.sdg.graphviewer.view.component.GVPanel;


import java.awt.Dimension;
import java.awt.GridBagLayout;
import java.awt.GridBagConstraints;
import java.awt.Insets;

import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

import javax.swing.BorderFactory;

/**
 * @author <a href="mailto:wellner@fmi.uni-passau.de">Tobias Wellner </a>
 * @version 1.0
 */
public class StatusBar extends GVPanel implements CommandStatusListener, MouseListener {
	private static final long serialVersionUID = 1372180742354751873L;
	protected MainFrame owner = null;
    private GVLabel messageLabel;

    /**
     * Constructs a new <code>StatusBar</code> object.
     */
    public StatusBar(MainFrame owner) {
        super(owner.getTranslator(), new GridBagLayout());
        this.owner = owner;
        this.initComponents();
//        owner.getCommandManager().addCommandStatusListener(this); TODO: statussystem machen
    }

    private void initComponents() {

        GridBagConstraints constraints = new GridBagConstraints();
        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.insets = new Insets(2, 2, 2, 2);
        constraints.gridx = GridBagConstraints.RELATIVE;
        constraints.weightx = 1.0;
        constraints.weighty = 1.0;

        this.messageLabel = new GVLabel(this.translator);
        this.messageLabel.setBorder(BorderFactory.createLoweredBevelBorder());
        this.messageLabel.setPreferredSize(new Dimension(100, 20));
        this.add(this.messageLabel, constraints);

    }

    /**
     * @see edu.kit.joana.ui.ifc.sdg.graphviewer.controller.event.CommandStatusListener#commandStatusChanged(edu.kit.joana.ui.ifc.sdg.graphviewer.controller.event.CommandStatusEvent)
     * @param event
     */
    public void commandStatusChanged(CommandStatusEvent event) {
        this.messageLabel.setTextResource(event.getMessageResource());
    }

    /**
     * @see java.awt.event.MouseListener#mouseClicked(java.awt.event.MouseEvent)
     */
    public void mouseClicked(MouseEvent e) {
        this.messageLabel.setText(null);
    }

    /**
     * @see java.awt.event.MouseListener#mouseEntered(java.awt.event.MouseEvent)
     */
    public void mouseEntered(MouseEvent e) {
    }

    /**
     * @see java.awt.event.MouseListener#mouseExited(java.awt.event.MouseEvent)
     */
    public void mouseExited(MouseEvent e) {
    }

    /**
     * @see java.awt.event.MouseListener#mousePressed(java.awt.event.MouseEvent)
     */
    public void mousePressed(MouseEvent e) {
        this.messageLabel.setText(null);
    }

    /**
     * @see java.awt.event.MouseListener#mouseReleased(java.awt.event.MouseEvent)
     */
    public void mouseReleased(MouseEvent e) { }
}
