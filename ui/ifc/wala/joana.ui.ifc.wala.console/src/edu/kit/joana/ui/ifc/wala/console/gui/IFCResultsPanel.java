/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.ui.ifc.wala.console.gui;

import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTree;

import edu.kit.joana.api.sdg.SDGProgramPart;
import edu.kit.joana.ifc.sdg.core.violations.IViolation;
import edu.kit.joana.ui.ifc.wala.console.gui.IFCConsoleGUI.Command;
import edu.kit.joana.ui.ifc.wala.console.gui.results.ChopNode;
import edu.kit.joana.ui.ifc.wala.console.gui.results.IFCResultModel;

public class IFCResultsPanel extends JPanel {
	private static final long serialVersionUID = 206500411193181862L;

	private final IFCConsoleGUI consoleGui;

	private final JTree resultsTree;
	private final IFCResultModel treeModel;
	private final JScrollPane treeView;

	public IFCResultsPanel(final IFCConsoleGUI console) {
		super();
		this.consoleGui = console;

		final GridBagLayout gbl = new GridBagLayout();
		this.setLayout(gbl);

		this.treeModel = new IFCResultModel();
		this.resultsTree = new JTree(treeModel);
		this.resultsTree.setRootVisible(true);
        this.resultsTree.addMouseListener(new MouseAdapter() {
                public void mouseClicked(MouseEvent me) {
                    if (me.getClickCount() == 2) {
                        
                        final Object elem = resultsTree.getLastSelectedPathComponent();
                        if ( elem instanceof ChopNode ) {
                            // Only catch double-click for chop-entries
                            final ChopNode chopNode = (ChopNode) elem;

                            if (chopNode.wasComputed) {
                                return;
                            }
                            me.consume();

                            final List<Command> cmds = new LinkedList<Command>();
                            final Command cmd = consoleGui.createCmdChop(chopNode.getSource(), chopNode.getSink());
                            cmds.add(cmd);
                            mute();
                            consoleGui.executeCmdList(cmds);
                        }
                    }
                }
            });
		this.treeView = new JScrollPane(resultsTree,
				JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
				JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
		this.treeView.setMinimumSize(new Dimension(600, 300));
		this.add(this.treeView,
				GUIUtil.mkgbc_fillxy(0, 0, GridBagConstraints.REMAINDER, 1));
	}

	public void mute() {
		treeView.setEnabled(false);
	}

	public void unmute() {
		treeView.setEnabled(true);
	}

    public void updateEntries(final Collection<? extends IViolation<SDGProgramPart>> vios) {
        this.treeModel.update(vios);
    }

    /**
     *  Insert chop-instructions into the selected element.
     *
     *  It is excpected that the selected node hasn't changed since the start of the
     *  computation.
     */
    public void updateChop(final Set<edu.kit.joana.api.sdg.SDGInstruction> chop) {
        final Object elem = resultsTree.getLastSelectedPathComponent();
        if ( elem instanceof ChopNode ) {
            final ChopNode chopNode = (ChopNode) elem;
            chopNode.wasComputed = true;
            this.treeModel.insertChop(chopNode, chop);
        } else {
            throw new IllegalStateException("Selected node is not a chop node");
        }
    }
}
