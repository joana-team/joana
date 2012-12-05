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
package edu.kit.joana.ui.ifc.wala.console.gui;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Collection;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.MutableComboBoxModel;

import edu.kit.joana.api.IFCType;
import edu.kit.joana.api.annotations.IFCAnnotation;



class SignalLight extends JPanel {

	/**
	 *
	 */
	private static final long serialVersionUID = -3234460792317296323L;

	enum State {
		RED, GREEN, OFF;
	}

	private State state = State.OFF;
	private static final Color darkRed = new Color(32, 0, 0);
	private static final Color darkGreen = new Color(0, 32, 0);

	public void setRed() {
		state = State.RED;
		repaint();
	}

	public void setGreen() {
		state = State.GREEN;
		repaint();
	}

	public void switchOff() {
		state = State.OFF;
		repaint();
	}

	public void paint(Graphics g) {
		Color cRed, cGreen;
		switch (state) {
		case RED:
			cRed = Color.red;
			cGreen = darkGreen;
			break;
		case GREEN:
			cRed = darkRed;
			cGreen = Color.green;
			break;
		case OFF:
			cRed = darkRed;
			cGreen = darkGreen;
			break;
		default:
			throw new IllegalStateException();
		}
		int diameter = Math.min(getWidth() / 2 + getHeight() / 2, getHeight());
		g.setColor(cRed);
		g.fillOval(0, 0, diameter, diameter);
		g.setColor(cGreen);
		g.fillOval(diameter, 0, diameter, diameter);
	}
}

public class IFCReportAndRunPanel extends JPanel {

	private static final long serialVersionUID = -5605182653315520355L;

	private final IFCConsoleGUI consoleGui;
	private final JTextArea ifcParams = new JTextArea();
	private final JButton runButton = new JButton("Run IFC Analysis");
	private final JComboBox typeCombo = new JComboBox();
	private final JCheckBox avoidTimeTravel = new JCheckBox("avoid time-travel");
	private final SignalLight resultIndicator = new SignalLight();
	private final JLabel resultText = new JLabel("<no analysis run yet>");

	public IFCReportAndRunPanel(final IFCConsoleGUI console) {
		super();
		this.consoleGui = console;
		init();
	}

	private void init() {
		final GridBagLayout gbl = new GridBagLayout();
		this.setLayout(gbl);

		ifcParams.setEditable(false);
		final JScrollPane ifcParamsScrollPane = new JScrollPane(ifcParams,
				JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
				JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
		ifcParamsScrollPane.setMinimumSize(new Dimension(600, 300));
		add(ifcParamsScrollPane,
				GUIUtil.mkgbc_fillxy(0, 0, GridBagConstraints.REMAINDER, 1));
//		JPanel dummy = new JPanel();
//		dummy.setMinimumSize(new Dimension(200, 30));
//		dummy.setPreferredSize(new Dimension(200, 30));
//		add(dummy, GUIUtil.mkgbc_nofill(0, 1, 1, 1));

		add(new JLabel("analysis type: "), GUIUtil.mkgbc_nofill(0, 1, 1, 1));
		initIFCTypeCombo();
		add(typeCombo, GUIUtil.mkgbc_nofill(1, 1, 1, 1));
		add(avoidTimeTravel, GUIUtil.mkgbc_nofill(2, 1, 1, 1));
		runButton.setMinimumSize(new Dimension(150, 30));
		runButton.setPreferredSize(new Dimension(150, 30));
		runButton.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent arg0) {
				consoleGui.execRunIFC();
			}

		});
		add(runButton, GUIUtil.mkgbc_nofill(3, 1, 1, 1));

		resultIndicator.setMinimumSize(new Dimension(60, 30));
		resultIndicator.setPreferredSize(new Dimension(60, 30));
		add(resultIndicator, GUIUtil.mkgbc_nofill(0, 2, 1, 1));
		add(resultText, GUIUtil.mkgbc_fillx(1, 2, GridBagConstraints.REMAINDER, 1));
//		JPanel dummy2 = new JPanel();
//		dummy2.setMinimumSize(new Dimension(220, 30));
//		dummy2.setPreferredSize(new Dimension(220, 30));
//		add(dummy2, GUIUtil.mkgbc_nofill(7, 1, GridBagConstraints.REMAINDER, 1));
	}

	private void initIFCTypeCombo() {
		MutableComboBoxModel cBoxModel = new DefaultComboBoxModel();
		for (IFCType ifcType : IFCType.values()) {
			cBoxModel.addElement(ifcType);
		}
		this.typeCombo.setModel(cBoxModel);
	}

	public void unmute() {
		runButton.setEnabled(true);
	}

	public void mute() {
		runButton.setEnabled(false);
		noLight();
	}

	private void updateIFCParams() {
		Collection<IFCAnnotation> srcs = consoleGui.getSources();
		Collection<IFCAnnotation> sinks = consoleGui.getSinks();
		Collection<IFCAnnotation> declass = consoleGui.getDeclassifications();

		ifcParams.setText("");

		ifcParams.append("sdg = " + consoleGui.getSDGFile() + "\n");
		if (consoleGui.getSDG() != null) {
			ifcParams.append("\t number of nodes: "
					+ consoleGui.getSDG().vertexSet().size() + "\n");
			ifcParams.append("\t number of edges: "
					+ consoleGui.getSDG().edgeSet().size() + "\n");
		}
		ifcParams.append("lattice = " + consoleGui.getLatticeFile() + "\n");
		ifcParams.append("Sources:\n");
		if (srcs.isEmpty()) {
			ifcParams.append("\t<none>\n");
		} else {
			for (IFCAnnotation src : srcs) {
				ifcParams.append("\t" + src.getProgramPart() + " annotated as "
						+ src.getLevel1() + "\n");
			}
		}
		ifcParams.append("Sinks:\n");
		if (sinks.isEmpty()) {
			ifcParams.append("\t<none>\n");
		} else {
			for (IFCAnnotation snk : sinks) {
				ifcParams.append("\t" + snk.getProgramPart() + " annotated as "
						+ snk.getLevel1() + "\n");
			}
		}

		if (IFCConsoleGUI.DECLASS_ENABLED) {
			ifcParams.append("Declassifications:\n");
			if (declass.isEmpty()) {
				ifcParams.append("\t<none>\n");
			} else {
				for (IFCAnnotation dcs : declass) {
					ifcParams.append("\t" + dcs.getProgramPart()
							+ " declassified from " + dcs.getLevel1() + " to "
							+ dcs.getLevel2() + "\n");
				}
			}
		}
	}

	public void updateEntries() {
		updateIFCParams();
		if (consoleGui.getSDG() == null) {
			mute();
		} else {
			unmute();
		}
	}

	public void greenLight() {
		resultIndicator.setGreen();
		resultText.setForeground(Color.green);
		resultText.setText("No violations found!");
	}

	public void redLight() {
		resultIndicator.setRed();
		resultText.setForeground(Color.red);
		resultText.setText("Found " + consoleGui.getLastAnalysisResult().size()
				+ " potential violation(s)!");
	}

	public void noLight() {
		resultIndicator.switchOff();
		resultText.setForeground(Color.black);
		resultText.setText("<no analysis run yet>");
	}

	public IFCType getIFCType() {
		return IFCType.fromString(typeCombo.getModel().getSelectedItem().toString());
	}
	
	public boolean getTimeSensitivity() {
		return avoidTimeTravel.isSelected();
	}

}
