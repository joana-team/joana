/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.ui.ifc.wala.console.gui;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.PrintStream;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;

import edu.kit.joana.api.annotations.AnnotationType;
import edu.kit.joana.api.annotations.IFCAnnotation;
import edu.kit.joana.api.sdg.SDGClass;
import edu.kit.joana.api.sdg.SDGProgramPart;
import edu.kit.joana.ifc.sdg.core.SecurityNode;
import edu.kit.joana.ifc.sdg.core.violations.IViolation;
import edu.kit.joana.ifc.sdg.graph.SDG;
import edu.kit.joana.ifc.sdg.lattice.IStaticLattice;
import edu.kit.joana.ifc.sdg.util.JavaMethodSignature;
import edu.kit.joana.ui.ifc.wala.console.console.EntryLocator;
import edu.kit.joana.ui.ifc.wala.console.console.IFCConsole;
import edu.kit.joana.ui.ifc.wala.console.console.IFCConsole.CMD;
import edu.kit.joana.ui.ifc.wala.console.console.IFCConsoleListener;
import edu.kit.joana.ui.ifc.wala.console.io.IFCConsoleOutput;
import edu.kit.joana.util.io.IOFactory;
import gnu.trove.map.TObjectIntMap;

public final class IFCConsoleGUI extends JFrame implements IFCConsoleListener, IFCConsoleOutput {

	public static final String NAME_OF_APPLICATION = "IFC Console";

	private static final long serialVersionUID = 8371062672682671006L;

	private final IFCConsolePanel consolePane;
	private final IFCConfigPanel configPane;
	private final IFCTreePanel treePane;
	private final IFCReportAndRunPanel runPane;
	private final IFCConsole console;

	public static final String PROPERTIES = "gui.properties";

	public static final boolean DECLASS_ENABLED;

	static {
		final InputStream propertyStream = IFCConsoleGUI.class.getClassLoader().getResourceAsStream(PROPERTIES);
		Properties p = new Properties();
		try {
			p.load(propertyStream);
		} catch (Throwable e) {
		}

		String declassEnabled = p.getProperty("declass");
		if (declassEnabled == null) {
			declassEnabled = System.getProperty("declassEnabled");
		}
		DECLASS_ENABLED = (declassEnabled != null && declassEnabled.equals("true"));
	}

	public static final String VERSION_ID = "versionid.properties";
	public static final String GIT_LAST_COMMIT;

	static {
		final InputStream propertyStream = IFCConsoleGUI.class.getClassLoader().getResourceAsStream(VERSION_ID);
		Properties p = new Properties();
		try {
			p.load(propertyStream);
		} catch (Throwable e) {
		}

		final String git_version = p.getProperty("git-version", "<no version information found>");

		GIT_LAST_COMMIT = git_version;
	}

	public IFCConsoleGUI() {
		super(NAME_OF_APPLICATION + " (last changed: " + GIT_LAST_COMMIT + ")");

		consolePane = new IFCConsolePanel(this);
		configPane = new IFCConfigPanel(this);

		console = new IFCConsole(new BufferedReader(IOFactory.createUTF8ISReader(System.in)), this);
		// console = new IFCConsole(new BufferedReader(new InputStreamReader(
		// System.in)), consolePane.getOutputStream(),
		// new JOptionPanePrintStream(this,
		// JOptionPanePrintStream.TYPE.INFO),
		// new JOptionPanePrintStream(this,
		// JOptionPanePrintStream.TYPE.ERROR));
		treePane = new IFCTreePanel(this);
		runPane = new IFCReportAndRunPanel(this);
		// final GridBagLayout gbl = new GridBagLayout();
		// root.setLayout(gbl);
		// root.setLayout(new BorderLayout());
		final JTabbedPane tabbedPane = new JTabbedPane();
		tabbedPane.addTab("Configuration", configPane);
		tabbedPane.addTab("Annotate", treePane);
		tabbedPane.addTab("Run", runPane);
		// tabbedPane.addTab("Console", consolePane);
		// root.add(tabbedPane, mkgbc_fillxy(0, 0, 1, 1));
		// root.add(consolePane, mkgbc_fillxy(0, 1, 1, 1));
		final JSplitPane root = new JSplitPane(JSplitPane.VERTICAL_SPLIT, tabbedPane, consolePane);
		// root.add(tabbedPane, BorderLayout.CENTER);
		// root.add(consolePane, BorderLayout.PAGE_END);
		// root.add(tabbedPane);
		// root.add(consolePane);
		root.setOneTouchExpandable(true);

		setContentPane(root);
		setDefaultCloseOperation(EXIT_ON_CLOSE);
		console.addListener(this);
		updateAll();
	}

	public static void main(String[] args) {
		try {
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
			System.out.println(UIManager.getLookAndFeel());
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		} catch (InstantiationException e) {
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		} catch (UnsupportedLookAndFeelException e) {
			e.printStackTrace();
		}
		SwingUtilities.invokeLater(new Runnable() {

			@Override
			public void run() {
				final IFCConsoleGUI gui = new IFCConsoleGUI();
				//gui.setPreferredSize(new Dimension(800, 600));
				gui.pack();
				gui.setVisible(true);
			}
			
		});
		
	}

	// console commands

	private final AtomicBoolean cmdExecRunning = new AtomicBoolean(false);

	public void execStrCmd(final String cmdstr) {
		if (console.isQuit(cmdstr)) {
			info("Close the window to exit this application!");
		} else {
		final CMD cmd = console.searchCommand(cmdstr);
		final String[] args = cmdstr.split("\\s+");

		if (cmd != null) {
			executeCmd(cmd, args);
		} else {
			consolePane.println("> " + cmdstr);
			consolePane.println("Unknown command '" + args[0] + "'");
		}
		}
	}

	private void executeCmd(final CMD cmd, final String[] args) {
		final List<Command> cmds = new LinkedList<Command>();
		cmds.add(new Command(cmd, args));
		executeCmdList(cmds);
	}

	public static final class Command {
		private final CMD cmd;
		private final String[] args;

		private Command(final CMD cmd, final String[] args) {
			this.cmd = cmd;
			this.args = args;
		}
	}

	public void executeCmdList(final List<Command> cmds) {
		if (cmdExecRunning.compareAndSet(false, true)) {
			final Thread processCmdList = new Thread() {
				@Override
				public void run() {
					for (final Command c : cmds) {
						try {
							console.processCommand(c.cmd, c.args);
						} catch (Exception e) {
							e.printStackTrace(consolePane.getOutputStream());
							console.executionAborted(c.cmd, c.args, e);
						} catch (AssertionError e2) {
							throw e2;
						} catch (Throwable t) {
							t.printStackTrace(System.out);
							console.executionAborted(c.cmd, c.args, t);
						}
					}

					cmdExecRunning.set(false);
				}
			};

			processCmdList.start();
		} else {
			// already running
			for (Command c : cmds) {
				info("Ignoring command " + c.cmd.getName() + " because another operation is currently running.");
			}
		}
	}

	@Override
	public void cmdIssued(CMD cmd, String[] args) {
		consolePane.mute();
		configPane.mute();
		treePane.mute();
		runPane.mute();
		StringBuilder sb = new StringBuilder(">");
		for (String arg : args) {
			sb.append(" ");
			sb.append(arg);
		}
		consolePane.println(sb.toString());

		switch (cmd) {
		case SEARCH_ENTRIES:
			configPane.searchEntryStarted();
			break;
		default:
			break;
		}
	}

	@Override
	public void cmdDone(CMD cmd, String[] args) {
		switch (cmd) {
		case SEARCH_ENTRIES:
			final EntryLocator loc = console.getEntryLocator();
			final List<JavaMethodSignature> found = loc.getLastSearchResults();

			Collections.sort(found, new Comparator<JavaMethodSignature>() {

				@Override
				public int compare(JavaMethodSignature o1, JavaMethodSignature o2) {
					return o1.toHRString().compareTo(o2.toHRString());
				}

			});

			runPane.noLight();
			configPane.searchEntryFinished(found);
			break;
		case RUN:
			if (getLastAnalysisResult().isEmpty()) {
				runPane.greenLight();
			} else {
				runPane.redLight();
			}
			break;
		case BUILD_SDG:
		case LOAD_SDG:
			configPane.sdgLoadedOrBuilt();
			break;
		default:
			break;
		}
		updateAll();
		consolePane.unmute();
		configPane.unmute();
		treePane.unmute();
		runPane.unmute();
	}

	private void updateAll() {
		configPane.updateEntries();
		Collection<IFCAnnotation> sources = console.getSources();
		Collection<IFCAnnotation> sinks = console.getSinks();
		Collection<IFCAnnotation> declasses = console.getDeclassifications();
		treePane.updateEntries(sources, sinks, declasses);
		runPane.updateEntries();
	}

	public void execSearchForEntries() {
		executeCmd(CMD.SEARCH_ENTRIES, new String[] { CMD.SEARCH_ENTRIES.getName() });
	}

	public void execSetClassPath(String cp) {
		executeCmd(CMD.SET_CLASSPATH, new String[] { CMD.SET_CLASSPATH.getName(), cp });
	}
	
	public void execSetStubsPath(String stubsPath) {
		executeCmd(CMD.SET_STUBSPATH, new String[] { CMD.SET_STUBSPATH.getName(), stubsPath });
		
	}

	public void execLoadSDG(String pathToSDG) {
		executeCmd(CMD.LOAD_SDG, new String[] { CMD.LOAD_SDG.getName(), pathToSDG });
		// loadProgramIntoTree(programRoot);
		// programRoot.setUserObject(choose.getSelectedFile().getName());
		// programTree.expandRow(0);
		// repaint();
		// } catch (IOException exc) {
		// JOptionPane.showMessageDialog(getRootPane(),
		// "I/O error while loading SDG from file \n"
		// + choose.getSelectedFile().getAbsolutePath() + ":\n" +
		// exc.getMessage());
		// }
	}

	public void execSaveSDG(String path) {
		executeCmd(CMD.SAVE_SDG, new String[] { CMD.SAVE_SDG.getName(), path });
	}

	public void execBuildSDG(String path) {
		List<Command> cmdList = new LinkedList<Command>();
		cmdList.add(new Command(CMD.BUILD_SDG, new String[] { CMD.BUILD_SDG.getName(),
				Boolean.toString(configPane.computeInterferenceEdges()), configPane.getMHPType().toString(),
				configPane.getExceptionAnalysisType().toString() }));
		if (path != null) {
			cmdList.add(new Command(CMD.SAVE_SDG, new String[] { CMD.SAVE_SDG.getName(), path }));
		}
		executeCmdList(cmdList);
		// if (!configPane.computeInterferenceEdges()) {
		// executeCmd(CMD.BUILD_SDG, new String[] { CMD.BUILD_SDG.getName() });
		// } else {
		// executeCmd(CMD.BUILD_CSDG, new String[] { CMD.BUILD_CSDG.getName(),
		// configPane.getMHPType().toString(),
		// configPane.getExceptionAnalysisType().toString() });
		// }
	}

	public void execRunIFC() {
		
		List<Command> cmdList = new LinkedList<Command>();
//		cmdList.add(new Command(CMD.SET_LATTICE, new String[] {CMD.SET_LATTICE.getName(), configPane.getCurrentLattice()}));
//		
		if (runPane.getTimeSensitivity()) {
			cmdList.add(new Command(CMD.RUN, new String[] { CMD.RUN.getName(), IFCConsole.convertIFCType(runPane.getIFCType()), IFCConsole.AVOID_TIME_TRAVEL }));
		} else {
			cmdList.add(new Command(CMD.RUN, new String[] { CMD.RUN.getName(),  IFCConsole.convertIFCType(runPane.getIFCType()) }));
		}
		
		executeCmdList(cmdList);
	}

	public void execSetEntryMethod(final JavaMethodSignature sig) {
		// final EntryLocator loc = console.getEntryLocator();
		executeCmd(CMD.SELECT_ENTRY, new String[] { CMD.SELECT_ENTRY.getName(), sig.toBCString() });
	}

	public void execClearAll() {
		executeCmd(CMD.CLEARALL, new String[] { CMD.CLEARALL.getName() });
	}

	public Command createCmdMarkAsSource(SDGProgramPart part, String level) {
		return new Command(CMD.SOURCE, new String[] { CMD.SOURCE.getName(),
				console.getSelectorStringFromMethodPart(part), level });
	}

	public Command createCmdMarkAsSink(SDGProgramPart part, String level) {
		return new Command(CMD.SINK, new String[] { CMD.SINK.getName(), console.getSelectorStringFromMethodPart(part),
				level });
	}

	public Command createCmdDeclassify(SDGProgramPart part, String level1, String level2) {
		return new Command(CMD.DECLASS, new String[] { CMD.DECLASS.getName(),
				console.getSelectorStringFromMethodPart(part), level1, level2 });
	}

	public Command createCmdClear(SDGProgramPart part) {
		return new Command(CMD.CLEAR,
				new String[] { CMD.CLEAR.getName(), console.getSelectorStringFromMethodPart(part) });
	}

	public Command createCmdClearALL(SDGProgramPart part) {
		return new Command(CMD.CLEARALL, new String[] {});
	}

	@Override
	public void error(final String msg) {
		JOptionPane.showMessageDialog(getRootPane(), msg, "Error", JOptionPane.ERROR_MESSAGE);
	}

	@Override
	public Answer question(final String questionMessage) {
		int ans = JOptionPane.showConfirmDialog(getRootPane(), questionMessage, "Question", JOptionPane.YES_NO_OPTION,
				JOptionPane.QUESTION_MESSAGE);

		if (ans == JOptionPane.YES_OPTION) {
			return Answer.YES;
		} else {
			return Answer.NO;
		}
	}

	public int yesno(final String message, int messageType) {
		return JOptionPane.showConfirmDialog(getRootPane(), message, "Warning", JOptionPane.YES_NO_OPTION,
				JOptionPane.WARNING_MESSAGE);
	}

	@Override
	public void info(String msg) {
		JOptionPane.showMessageDialog(getRootPane(), msg, "Info", JOptionPane.INFORMATION_MESSAGE);
	}

	public String getClassPath() {
		return console.getClassPath();
	}

	public Collection<String> getSecurityLevels() {
		return console.getSecurityLevels();
	}

	public JavaMethodSignature getEntryMethod() {
		return console.getEntryLocator().getActiveEntry();
	}

	public int getActiveEntryMethodIndex() {
		final EntryLocator loc = console.getEntryLocator();
		final JavaMethodSignature sig = loc.getActiveEntry();
		return loc.getIndex(sig);
	}

	public Collection<? extends IViolation<SecurityNode>> getLastAnalysisResult() {
		return console.getLastAnalysisResult();
	}
	
	public TObjectIntMap<IViolation<SDGProgramPart>> getLastAnalysisResultGrouped() {
		return console.getLastAnalysisResultGrouped();
	}

	public Collection<IFCAnnotation> getSources() {
		return console.getSources();
	}

	public Collection<IFCAnnotation> getSinks() {
		return console.getSinks();
	}

	public Collection<IFCAnnotation> getDeclassifications() {
		return console.getDeclassifications();
	}

	public String getLatticeFile() {
		return console.getLatticeFile();
	}

	public SDG getSDG() {
		return console.getSDG();
	}

	public void execLoadLattice(String path) {
		executeCmd(CMD.LOAD_LATTICE, new String[] { CMD.LOAD_LATTICE.getName(), path });
	}

	public void execSpecifyLattice(String latticeSpec) {
		executeCmd(CMD.SET_LATTICE, new String[] { CMD.SET_LATTICE.getName(), latticeSpec });
	}

	public void execLoadScript(String scriptPath) {
		executeCmd(CMD.LOADSCRIPT, new String[] { CMD.LOADSCRIPT.getName(), scriptPath });
	}

	public void execSaveScript(String scriptPath) {
		executeCmd(CMD.SAVESCRIPT, new String[] { CMD.SAVESCRIPT.getName(), scriptPath });
	}

	public Collection<SDGClass> getClasses() {
		if (console.getProgram() == null) {
			return new LinkedList<SDGClass>();
		} else {
			return console.getProgram().getClasses();
		}
	}

	public IStaticLattice<String> getLattice() {
		return console.getLattice();
	}

	@Override
	public void log(String logMessage) {
		consolePane.getOutputStream().print(logMessage);
	}

	@Override
	public void logln(String logMessage) {
		consolePane.getOutputStream().println(logMessage);
	}

	public boolean canAnnotate(Collection<SDGProgramPart> selectedParts, AnnotationType type) {
		return console.canAnnotate(selectedParts, type);
	}

	@Override
	public PrintStream getPrintStream() {
		return consolePane.getOutputStream();
	}

	public String getSDGFile() {
		return configPane.getSDGFile();
	}

	

	// public SDGProgramPart getInterferingPart(SDGProgramPart part, Type type)
	// {
	// return console.getInterferingPart(part, type);
	// }
}
