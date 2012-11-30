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
package edu.kit.joana.ui.ifc.sdg.textual.highlight.graphviewer;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.Socket;

import edu.kit.joana.ui.ifc.sdg.textual.highlight.highlight.HighlightPlugin;


public class ShowGraphViewer {

	private ShowGraphViewer() {}

	public static void showGraphViewer (final String graph, final int node) {

		final int port = HighlightPlugin.getDefault().getPreferenceStore().getInt("graphviewer.port");
		final String path = HighlightPlugin.getDefault().getPreferenceStore().getString("graphviewer.path");

		new Thread() {
			private boolean DEBUG = true;

			public void run() {
				if (DEBUG) System.out.println("Connecting to Graphviewer...");
				Socket s = connectToGraphviewer();

				if (s != null) {
					try {
						if (DEBUG) System.out.println("Sending request...");
						ObjectOutputStream out = new ObjectOutputStream(s.getOutputStream());
						out.writeObject(graph);
						if (DEBUG) System.out.println(graph);
						out.writeInt(node);
						out.close();
						s.close();
						if (DEBUG) System.out.println("Done!");

					} catch (IOException ex) {
						System.err.println(ex.getMessage());
					} finally {
						try {
							s.close();
						} catch (IOException e) { /* Nu ham wa alles versucht ... */ }
					}
				}
			}

			private Socket connectToGraphviewer() {
				Socket s = connect();

				if (s == null) {
					if (DEBUG) System.out.println("failed. Graphviewer seems to be down.");
					if (DEBUG) System.out.println("Starting Graphviewer... " + path + " " + port);

					try {
						startGraphviewer();

						int ctr = 30; // max number of trials
						while (s == null && ctr > 0) {
							try {
								sleep(1000);  // give graphviewer some time for booting
							} catch (InterruptedException ie) { }

							if (DEBUG) System.out.println("Connecting to Graphviewer...");
							s = connect();

							if (s == null) {
								if (DEBUG) System.out.println("failed. Retrying...");
								ctr--;
							}
						}

					} catch (IOException io) {
						if (DEBUG) System.out.println("failed.");
						System.err.println("Could not start the Graphviewer:\n"+io.getMessage());
					}
				}

				if (s != null) {
					if (DEBUG) System.out.println("Connection established.");
				} else {
					if (DEBUG) System.out.println("failed.");
				}

				return s;
			}

			private Socket connect() {
				try {
					Socket s = new Socket("localhost", port);
					return s;

				} catch (IOException io) {
					// no connection available
					return null;
				}
			}

			private void startGraphviewer()
			throws IOException {
				Runtime.getRuntime().exec("java -jar -Xmx1000M " + path + " -s " + port);
			}

		}.start();
	}

	public static void showGraphViewer (final String graph) {
		showGraphViewer(graph, 1);
	}

}
