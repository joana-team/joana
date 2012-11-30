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
 * @(c)GraphViewer.java
 *
 * Project: GraphViewer
 *
 * Chair for Softwaresystems
 * Faculty of Informatics and Mathematics
 * University of Passau
 *
 * Created on 2004-10-28 at 14:56
 */
package edu.kit.joana.ui.ifc.sdg.graphviewer;

import edu.kit.joana.ui.ifc.sdg.graphviewer.model.GraphViewerModel;
import edu.kit.joana.ui.ifc.sdg.graphviewer.translation.AdjustableTranslator;
import edu.kit.joana.ui.ifc.sdg.graphviewer.translation.Translator;
import edu.kit.joana.ui.ifc.sdg.graphviewer.util.Debug;
import edu.kit.joana.ui.ifc.sdg.graphviewer.view.MainFrame;

import java.awt.Font;
import java.io.File;
import java.io.InputStream;
import java.util.Properties;

import javax.swing.UIDefaults;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;

/**
 * This is the main class of the programm. It contains the main method that
 * initializes the user interface and launches the main frame with all its
 * components.
 *
 * @author <a href="mailto:wellner@fmi.uni-passau.de">Tobias Wellner </a>, <a
 *         href="mailto:westerhe@fmi.uni-passau.de">Marieke Westerheide </a>
 * @version 1.1
 */
public final class GraphViewer {

	// to be able to hand over the current instance of this class
	private static GraphViewer instance = null;

	// a JFrame
	private MainFrame frame = null;

	public static final boolean IS_MAC = System.getProperty("mrj.version") != null;

	public static final String VERSION_ID = "versionid.properties";
	public static final String CVS_ID;

	static {
		final InputStream propertyStream = GraphViewer.class.getClassLoader().getResourceAsStream(VERSION_ID);
		Properties p = new Properties();
		try {
			p.load(propertyStream);
		} catch (Throwable e) {}

		final String git_version = p.getProperty("git-version", "No version information found.");

		CVS_ID = git_version;
	}


	// handles local settings
	private Translator translator = null;

	// initializes frame with translator, model and command manager, sets look
	// and feel
	private GraphViewer() {
		this.initUI();
		Translator translator = AdjustableTranslator.getInstance();
//		CommandManager commandManager = DefaultCommandManager.getInstance();
		GraphViewerModel model = new GraphViewerModel();
		this.frame = new MainFrame(translator, model);
	}

	private void initUI() {
		// get current look and feel (swing default)
		UIDefaults defaults = UIManager.getDefaults();
		Font font = defaults.getFont("Label.font").deriveFont(Font.PLAIN);
		defaults.put("Label.font", font);
		defaults.put("Button.font", font);
		defaults.put("RadioButton.font", font);
		defaults.put("ToggleButton.font", font);
		defaults.put("MenuItem.font", font);
		defaults.put("Menu.font", font);
		defaults.put("ComboBox.font", font);
		defaults.put("TabbedPane.font", font);
		defaults.put("FileChooser.font", font);
	}

	/**
	 * This method is needed to access the current instance of the graph viewer.
	 * If no instance exists a new one is created.
	 *
	 * @return the current instance
	 */
	public static GraphViewer getInstance() {
		if (instance == null) {
			instance = new GraphViewer();
		}
		return instance;
	}

	private static String toStr(String[] arr) {
		if (arr == null) {
			return "null";
		}

		StringBuilder sb = new StringBuilder("[");
		for (int i = 0; i < arr.length; i++) {
			sb.append("'" + arr[i] + "'");
			if (i + 1 < arr.length) {
				sb.append(", ");
			}
		}
		sb.append("]");

		return sb.toString();
	}

	/**
	 * Initializes the main frame with the user's current look and feel.
	 *
	 */
	public static void main(String[] args) {
		Debug.print("GraphViewer started with args: " + toStr(args));

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

		if (args.length > 1 && args[0].equals("-s")) {
			try {
				Thread server = new Thread(new Server(GraphViewer.getInstance().frame, Integer.parseInt(args[1])));
				server.start();
			} catch (NumberFormatException e) {
				return;
			}

			GraphViewer.getInstance().frame.setVisible(true);
		} else if (args.length > 0) {
			final String file = args[args.length - 1];
			Debug.print("Trying to open file: " + file);
			GraphViewer.getInstance().frame.setVisible(true);

			File f = new File(file);
			if (f.exists() && f.canRead()) {
				// open file
				GraphViewer.getInstance().frame.getModel().openSDG(f);
			}
		} else {
			GraphViewer.getInstance().frame.setVisible(true);
		}
	}

	/**
	 * A translator handles local settings, specifically the language.
	 *
	 * @return translator instance variable
	 */
	public Translator getTranslator() {
		return this.translator;
	}
}
