/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.ui.ifc.wala.console.gui;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;

import edu.kit.joana.ifc.sdg.lattice.IStaticLattice;



public class DeclassSelection extends JDialog {

    /**
     *
     */
    private static final long serialVersionUID = 7170674014539166547L;
    private final IStaticLattice<String> lattice;
    private final IFCConsoleGUI consoleGui;
    private final JComboBox<Object> level1Selection = new JComboBox<Object>();
    private final JComboBox<Object> level2Selection = new JComboBox<Object>();

    /** order lattice elements as good as possible by lattice order */
    private final Comparator<String> latCompAsc = new Comparator<String>() {

	@Override
	public int compare(String arg0, String arg1) {
	    if (lattice.greatestLowerBound(arg0, arg1).equals(arg0)) {
		return -1;
	    } else if (lattice.greatestLowerBound(arg0, arg1).equals(arg1)) {
		return 1;
	    } else {
		return arg0.compareTo(arg1); // incomparable elements are considered equal
	    }
	}

    };

    public DeclassSelection(IFCConsoleGUI consoleGui, IStaticLattice<String> lattice) {
	super(consoleGui, "Declassification", true);
	setModal(true);
	this.lattice = lattice;
	this.consoleGui = consoleGui;
	List<String> levels = new ArrayList<String>();
	levels.addAll(lattice.getElements());
	Collections.sort(levels, Collections.reverseOrder(latCompAsc));


	for (String level : levels) {
	    level1Selection.addItem(level);
	}

	Collections.reverse(levels);

	for (String level : levels) {
	    level2Selection.addItem(level);
	}

	JPanel contentPane = new JPanel();
	contentPane.add(new JLabel("Declassify from "));
	contentPane.add(level1Selection);
	contentPane.add(new JLabel(" to "));
	contentPane.add(level2Selection);
	JButton ok = new JButton("Ok");
	ok.addActionListener(new ActionListener() {

	    @Override
	    public void actionPerformed(ActionEvent arg0) {
		setVisible(false);
	    }

	});
	contentPane.add(ok);
	setContentPane(contentPane);
	pack();
	this.level1Selection.addItemListener(new ItemListener() {

	    @Override
	    public void itemStateChanged(ItemEvent e) {
		if (e.getStateChange() == ItemEvent.SELECTED) {
		    handleSelectLevel1();
		}
	    }

	});
	this.level2Selection.addItemListener(new ItemListener() {

	    @Override
	    public void itemStateChanged(ItemEvent e) {
		if (e.getStateChange() == ItemEvent.SELECTED) {
		    handleSelectLevel2();
		}
	    }
	});
    }

    private void handleSelectLevel1() {

	if (!lattice.greatestLowerBound(getLevel1(), getLevel2()).equals(getLevel2())) {
	    consoleGui.error("Cannot declassify from " + getLevel1() + " to " + getLevel2() + " because " + getLevel2() + " is not lower than " + getLevel1() + " in current lattice!");
	}

    }

    private void handleSelectLevel2() {

	if (!lattice.greatestLowerBound(getLevel1(), getLevel2()).equals(getLevel2())) {
	    consoleGui.error("Cannot declassify from " + getLevel1() + " to " + getLevel2() + " because " + getLevel2() + " is not lower than " + getLevel1() + " in current lattice!");
	}
    }

    public String getLevel1() {
	return (String)level1Selection.getSelectedItem();
    }

    public String getLevel2() {
	return (String)level2Selection.getSelectedItem();
    }

//    private boolean verifyLevel1(String level2) {
//	// all items in level1Selection must not be lower than level2
//	for (int i = 0; i < level1Selection.getItemCount(); i++) {
//	    String level1 = (String)level1Selection.getItemAt(i);
//	    if (lattice.leastUpperBound(level1, level2).equals(level2)) {
//		return false;
//	    }
//	}
//	return true;
//    }
//
//    private boolean verifyLevel2(String level1) {
//	// all items in level2Selection must not be greater than level1
//	for (int i = 0; i < level2Selection.getItemCount(); i++) {
//	    String level2 = (String)level2Selection.getItemAt(i);
//	    if (lattice.leastUpperBound(level1, level2).equals(level2)) {
//		return false;
//	    }
//	}
//
//	return true;
//    }
}
