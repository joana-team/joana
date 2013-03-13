/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.wala.core;

import java.awt.Color;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.io.File;
import java.io.PrintStream;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import javax.swing.filechooser.FileFilter;

import edu.kit.joana.util.Log;
import edu.kit.joana.util.Logger;
import edu.kit.joana.util.io.IOFactory;
import edu.kit.joana.wala.core.Main.Config;
import edu.kit.joana.wala.core.SDGBuilder.FieldPropagation;

public class MainGui extends JFrame {

	private static final long serialVersionUID = 5531712277867894087L;

	private final Logger debug = Log.getLogger(Log.L_UI_DEBUG);
	
	public static void main(final String[] argv) {
		try {
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		} catch (InstantiationException e) {
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		} catch (UnsupportedLookAndFeelException e) {
			e.printStackTrace();
		}

		MainGui gui = new MainGui();
		gui.pack();
		gui.setMinimumSize(gui.getSize());
		gui.setVisible(true);
	}

	private static GridBagConstraints mkgbc_nofill(int gridx, int gridy, int gridwidth, int gridheight) {
		GridBagConstraints gbc = new GridBagConstraints(gridx, gridy, gridwidth, gridheight, 0, 0,
				GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(2, 4, 2, 4), 0, 0);

		return gbc;
	}

	private static GridBagConstraints mkgbc_fillx(int gridx, int gridy, int gridwidth, int gridheight) {
		GridBagConstraints gbc = new GridBagConstraints(gridx, gridy, gridwidth, gridheight, 1, 0,
				GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(2, 4, 2, 4), 0, 0);

		return gbc;
	}

	private static GridBagConstraints mkgbc_fillxy(int gridx, int gridy, int gridwidth, int gridheight) {
		GridBagConstraints gbc = new GridBagConstraints(gridx, gridy, gridwidth, gridheight, 1, 1,
				GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(2, 4, 2, 4), 0, 0);

		return gbc;
	}

	private final JTextField pathToBin;
	private final JTextField entryPoint;
	private final JTextArea output;
	private AtomicBoolean running = new AtomicBoolean(false);

	public MainGui() {
		super("System Dependence Graph Computation");
		final GridBagLayout gbl = new GridBagLayout();
		final JPanel panel = new JPanel(gbl);
		this.add(panel);
		pathToBin = new JTextField("<select bin dir or .jar file>                                                          ");
		pathToBin.setToolTipText("Path to .class files or .jar file.");
		panel.add(pathToBin);
		gbl.setConstraints(pathToBin, mkgbc_nofill(0, 0, 2, 1));
		JButton select = new JButton("Choose program");
		panel.add(select);
		gbl.setConstraints(select, mkgbc_nofill(2, 0, 1, 1));
		select.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				JFileChooser chooser = new JFileChooser();
				chooser.setFileFilter(new FileFilter() {

					@Override
					public String getDescription() {
						return "Directories or *.jar";
					}

					@Override
					public boolean accept(File arg0) {
						return arg0.isDirectory() || (arg0.isFile() && arg0.getName().endsWith(".jar"));
					}
				});
				chooser.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
				chooser.setDialogTitle("Select binary directory or .jar file of the program");
				if (chooser.showOpenDialog(panel) == JFileChooser.APPROVE_OPTION) {
					File f = chooser.getSelectedFile();
					pathToBin.setText(f.getAbsolutePath());
				}
			}
		});
		JPanel filler = new JPanel();
		panel.add(filler);
		GridBagConstraints gbcFill = mkgbc_fillx(3, 0, 1, 1);
		gbcFill.weightx = 2;
		gbl.setConstraints(filler, gbcFill);
		final JButton run = new JButton("Compute SDG");
		panel.add(run);
		gbl.setConstraints(run, mkgbc_nofill(4, 0, 1, 1));
		output = new JTextArea(20, 120);
		output.setBackground(Color.BLACK);
		output.setForeground(Color.WHITE);
		output.setFont(new Font("Monospaced", Font.PLAIN, 11));
		output.setToolTipText("Displays output of the sdg computation.");
		output.setEditable(false);
		JScrollPane outputPane = new JScrollPane(output, JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
				JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);
		panel.add(outputPane);
		GridBagConstraints outgbc = mkgbc_fillxy(0, 2, 5, 1);
		gbl.setConstraints(outputPane, outgbc);
		run.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				debug.outln("COMPUTE");

				if (running.compareAndSet(false, true)) {
					debug.outln("START COMPUTE");
					RunSDG runsdg = new RunSDG();
					runsdg.start();
				} else {
					debug.outln("NO COMPUTE");
				}
			}
		});

		entryPoint = new JTextField("<select main method>                                                          ");
		entryPoint.setToolTipText("Entrypoint for the analysis.");
		panel.add(entryPoint);
		gbl.setConstraints(entryPoint, mkgbc_nofill(0, 1, 2, 1));
		JButton search = new JButton("Search main");
		panel.add(search);
		gbl.setConstraints(search, mkgbc_nofill(2, 1, 1, 1));
		search.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				debug.outln("SEARCH");

				if (running.compareAndSet(false, true)) {
					debug.outln("START SEARCH");
					RunSearchMain runsearch = new RunSearchMain();
					runsearch.start();
				} else {
					debug.outln("NO SEARCH");
				}
			}
		});

		final long maxMem = Runtime.getRuntime().maxMemory() / (1024 * 1024);
		JTextField memInfo = new JTextField();
		memInfo.setEditable(false);
//		memInfo.setEnabled(false);
		memInfo.setText("Maximal available memory is " + maxMem + "M. Use java -Xmx (e.g. -Xmx1024M) to change this setting.");
		panel.add(memInfo);
		gbl.setConstraints(memInfo, mkgbc_fillx(0, 3, 5, 1));
		addWindowListener(new WindowListener() {

			@Override
			public void windowOpened(WindowEvent e) {}

			@Override
			public void windowIconified(WindowEvent e) {}

			@Override
			public void windowDeiconified(WindowEvent e) {}

			@Override
			public void windowDeactivated(WindowEvent e) {}

			@Override
			public void windowClosing(WindowEvent e) {
				debug.outln("CLOSING");
				System.exit(0);
			}

			@Override
			public void windowClosed(WindowEvent e) {
				debug.outln("CLOSED");
				System.exit(0);
			}

			@Override
			public void windowActivated(WindowEvent arg0) {}
		});
	}

	public class RunSDG extends Thread {

		private final String entryMethod = entryPoint.getText().trim();
			//"C.main([Ljava/lang/String;)V";
		private final String classpath = pathToBin.getText().trim();

		public void run() {
			debug.outln("\tCOMPUTE RUN");

			synchronized (running) {
				debug.outln("\tCOMPUTE ENTER");

				final Config cfg = new Config("MainGui " + entryMethod, entryMethod, classpath, FieldPropagation.FLAT);
				output.setText("");
				final PrintStream outtotext = IOFactory.createPrintStreamFromJTextArea(output);
				try {
					Main.run(outtotext, cfg);
				} catch (Throwable e1) {
					outtotext.println();
					e1.printStackTrace(outtotext);
				}

				running.set(false);
				debug.outln("\tCOMPUTE EXIT");
			}
		}
	}

	public class RunSearchMain extends Thread {

		private final String entryMethod = "<unused>";
			//"C.main([Ljava/lang/String;)V";
		private final String classpath = pathToBin.getText().trim();

		public void run() {
			debug.outln("\tSEARCH RUN");

			synchronized (running) {
				debug.outln("\tSEARCH ENTER");
				final Config cfg = new Config("Search main " + entryMethod, entryMethod, classpath, FieldPropagation.FLAT);
				output.setText("");
				final PrintStream outtotext = IOFactory.createPrintStreamFromJTextArea(output);
				try {
					Main.searchMainMethods(outtotext, cfg);
				} catch (Throwable e1) {
					outtotext.println();
					e1.printStackTrace(outtotext);
					outtotext.println();
					outtotext.println("Error while searching for main method.");
					outtotext.println("Please check if '" + classpath + "' is really the correct classpath.");
				}

				running.set(false);
				debug.outln("\tSEARCH ENTER");
			}
		}
	}

}
