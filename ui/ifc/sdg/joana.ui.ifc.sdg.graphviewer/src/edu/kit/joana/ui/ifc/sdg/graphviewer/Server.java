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
package edu.kit.joana.ui.ifc.sdg.graphviewer;

import java.io.File;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.ServerSocket;
import java.net.Socket;

import edu.kit.joana.ifc.sdg.graph.SDG;
import edu.kit.joana.ifc.sdg.graph.SDGNode;
import edu.kit.joana.ui.ifc.sdg.graphviewer.view.MainFrame;


public class Server implements Runnable {

	//Server
	ServerSocket sock;
	Socket con;
	ObjectInputStream input;
	int port;

	String openGraph;

	MainFrame parent;

	public Server(MainFrame parent, int port) {
		this.port = port;
		this.parent = parent;
		openGraph = "";
	}

	public void run() {
		try {
			sock = new ServerSocket(port);
			while (true) {
				con = sock.accept();
				input = new ObjectInputStream(con.getInputStream());
				String graph = "";
				try {
					graph = (String) input.readObject();
				} catch (ClassNotFoundException e) {
				}
				if (!openGraph.equals(graph)) {
					openGraph(graph);
				}
				int node = input.readInt();
				openNode(node);
				input.close();
				con.close();
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void openGraph(String graph) {
//		ActionMap actions = parent.getActions();
//		OpenAction action = (OpenAction) actions
//		.get(OpenAction.class);
//		parent.getCommandManager().invoke(new OpenPDGCommand(action, parent.getModel(), new File(graph)));
		parent.getModel().openSDG(new File(graph));
	}

	private void openNode(int node) {
		SDG sdg = parent.getGraphPane().getSelectedGraph().getCompleteSDG();
		SDGNode sdgnode = sdg.getNode(node);
		int procId = sdgnode.getProc();
//		ActionMap actions = parent.getActions();
		parent.setLookupId(node);
//		OpenMethodAction action = (OpenMethodAction) actions
//				.get(OpenMethodAction.class);
//		parent.getCommandManager().invoke(
//				new OpenMethodCommand(action, parent.getModel(), procId,
//						graphName));
		parent.getModel().openPDG(parent.getGraphPane().getSelectedGraph(), procId);
	}

}
