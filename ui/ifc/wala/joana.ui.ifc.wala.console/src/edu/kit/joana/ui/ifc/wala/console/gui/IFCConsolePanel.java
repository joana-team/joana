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
import java.awt.Font;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.io.PrintStream;
import java.util.Stack;

import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;

import edu.kit.joana.util.io.IOFactory;

public class IFCConsolePanel extends JPanel {

	private static final long serialVersionUID = 3852317334299417963L;

	private final IFCConsoleGUI consoleGui;
	private final JTextArea output = new JTextArea(
			"Type help to view availiable commands.\n");
	private final PrintStream out = IOFactory.createPrintStreamFromJTextArea(output);
	private final JTextField input = new JTextField("");
	private final JButton enter = new JButton("enter");

	private final Stack<String> history = new Stack<String>();
	private int posInHistory = 0;

	public IFCConsolePanel(IFCConsoleGUI consoleGui) {
		super();
		this.consoleGui = consoleGui;
		output.setEditable(false);
		output.setBackground(Color.BLACK);
		output.setForeground(Color.GREEN);
		output.setFont(new Font("Monospaced", Font.BOLD, 12));

		enter.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent arg0) {
				executeCmdStr();
			}

		});

		input.addKeyListener(makeKeyListener());

		final JScrollPane outputPane = new JScrollPane(output,
				JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
				JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
		final GridBagLayout gbl = new GridBagLayout();
		this.setLayout(gbl);

		gbl.setConstraints(outputPane, GUIUtil.mkgbc_fillxy(0, 0, 2, 1));
		gbl.setConstraints(input, GUIUtil.mkgbc_fillx(0, 1, 1, 1));
		gbl.setConstraints(enter, GUIUtil.mkgbc_nofill(1, 1, 1, 1));

		outputPane.setPreferredSize(new Dimension(700, 270));
		outputPane.setMinimumSize(new Dimension(700, 10));
		input.setPreferredSize(new Dimension(600, 30));
		input.setPreferredSize(new Dimension(100, 30));

		this.add(outputPane);
		this.add(input);
		this.add(enter);
	}

	public void mute() {
		input.setEnabled(false);
		enter.setEnabled(false);
	}

	public void unmute() {
		input.setEnabled(true);
		enter.setEnabled(true);
	}

	private void executeCmdStr() {
		final String cmdstr = input.getText();
		input.setText("");

		if (cmdstr != null && !cmdstr.trim().isEmpty()) {
			history.push(cmdstr.trim());
			posInHistory = history.size();
			IFCConsolePanel.this.consoleGui.execStrCmd(cmdstr.trim());
		}
	}

	private KeyListener makeKeyListener() {
		return new KeyListener() {

			@Override
			public void keyPressed(KeyEvent arg0) {
				switch (arg0.getKeyCode()) {
				case KeyEvent.VK_ENTER:
					executeCmdStr();
					input.requestFocus();
					break;
				case KeyEvent.VK_UP:
					if (!history.isEmpty() && posInHistory > 0) {
						posInHistory--;
						String histCmd = history.get(posInHistory);
						input.setText(histCmd);
					}
					break;
				case KeyEvent.VK_DOWN:
					if (!history.isEmpty() && posInHistory < history.size() - 1) {
						posInHistory++;
						String histCmd = history.get(posInHistory);
						input.setText(histCmd);
					}
					break;
				default:
					break;
				}
			}

			@Override
			public void keyReleased(KeyEvent arg0) {
			}

			@Override
			public void keyTyped(KeyEvent arg0) {
			}

		};
	}
	
	public void grabFocusForCommandline() {
		input.grabFocus();
	}

	public void println(String str) {
		out.println(str);
	}

	public PrintStream getOutputStream() {
		return out;
	}

}
