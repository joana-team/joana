/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.ui.ifc.sdg.graphviewer.view;

import java.awt.Color;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Hashtable;
import java.util.Map;

import javax.swing.BorderFactory;
import javax.swing.JMenuItem;
import javax.swing.border.Border;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;


import org.jgraph.JGraph;
import org.jgraph.graph.DefaultGraphCell;
import org.jgraph.graph.DefaultGraphModel;
import org.jgraph.graph.GraphLayoutCache;
import org.jgraph.graph.GraphModel;

import edu.kit.joana.ifc.sdg.graph.SDG;
import edu.kit.joana.ifc.sdg.graph.SDGNode;
import edu.kit.joana.ifc.sdg.graph.slicer.SummarySlicer;
import edu.kit.joana.ifc.sdg.graph.slicer.SummarySlicerBackward;
import edu.kit.joana.ui.ifc.sdg.graphviewer.translation.Resource;
import edu.kit.joana.ui.ifc.sdg.graphviewer.translation.Translator;
import edu.kit.joana.ui.ifc.sdg.graphviewer.view.component.GVMenu;


public class SlicingMenu extends GVMenu implements ChangeListener {
	private static final long serialVersionUID = 5431822284677678749L;
	private MainFrame mainFrame = null;
	private GraphPane graphPane = null;
	public final String SOURCE = "criterion:";
	private int sourceID = 0;
	static SDG graph;

	public SlicingMenu(MainFrame owner, Translator translator, Resource text, MenuBar menuBar) {
		super(translator, text);
		bildUnterMenu();
		this.mainFrame = owner;
		menuBar.add(this);
		this.graphPane = owner.getGraphPane();

	}

	private void bildUnterMenu() {
		JMenuItem menuItemSource = new JMenuItem(SOURCE + " " + this.sourceID);
		this.add(menuItemSource);
		menuItemSource.addMouseListener(new MyMouseSourceAction());
//		JMenuItem menuItemTarget = new JMenuItem(TARGET + " " + this.targetID);
//		this.add(menuItemTarget);
//		menuItemTarget.addMouseListener(new MyMouseTargetAction());
		JMenuItem menuItemComputer = new JMenuItem("compute");
		this.add(menuItemComputer);
		menuItemComputer.addMouseListener(new MyMouseComputeAction());
	}

	class MyMouseSourceAction extends MouseAdapter {
		@Override
		public void mousePressed(MouseEvent e) {
			super.mousePressed(e);
			int id = getSelectCellID();
			setSourceID(id);
			((JMenuItem) e.getSource()).setText(SOURCE + " " + id);

		}
	}

	private int getSelectCellID() {
		int id = 0;
		if (graphPane.getSelectedIndex() != -1) {

			JGraph selectedGraph = graphPane.getSelectedJGraph();
			if (selectedGraph instanceof CallGraphView) {
			} else {
				Object[] selection = selectedGraph.getSelectionCells();
				if (selection != null) {
					if (selection.length != 0) {

						String string = selection[0].toString();

						int indexOf = string.indexOf('>');
						int indexOf2 = string.indexOf(' ');
						String substring = string.substring(indexOf + 1,
								indexOf2);// 6,8
						id = Integer.parseInt(substring);

					}
				}
			}
		}

		return id;
	}

	public void setSourceID(int sourceID) {
		this.sourceID = sourceID;
	}

	public int getSourceID() {
		return sourceID;
	}

	public void stateChanged(ChangeEvent arg0) { }

	class MyMouseComputeAction extends MouseAdapter {

		@Override
		public void mousePressed(MouseEvent e) {
			super.mousePressed(e);
			int sourceNum = getSourceID();

			System.out.println("sourceNum:	"+sourceNum);
//			graph = mainFrame.getModel().getMethodGraph(
//					mainFrame.getGraphPane().getSelectedGraph()
//					.getName()).getCompleteSDG();
			graph = graphPane.getSelectedGraph().getCompleteSDG();

			if (graphPane.getSelectedIndex() != -1) {
				SDGNode source = graph.getNode(sourceNum);
				SummarySlicer slicer = new SummarySlicerBackward(graph);
				Collection<SDGNode> slice = slicer.slice(Collections.singleton(source));
				System.out.println(slice);

				GraphLayoutCache view;
				JGraph currGraph = graphPane.getSelectedJGraph();
				ArrayList<Integer> resultInteger = new ArrayList<Integer>();
				ArrayList<SDGNode> result = null;
				result = new ArrayList<SDGNode>(slice);
				Collections.sort(result, SDGNode.getIDComparator());
				if (slice != null) {
					for (SDGNode s : result) {
						resultInteger.add(s.getId());
					}
				}
				view = currGraph.getGraphLayoutCache();
				GraphModel model = currGraph.getModel();
				Object[] cells = DefaultGraphModel.getAll(model);
				Map<DefaultGraphCell, Map<String, Border>> nested = new Hashtable<DefaultGraphCell, Map<String, Border>>();
				for (Object o : cells) {
					if (!model.isPort(o)) {
						DefaultGraphCell cell = (DefaultGraphCell) o;

						Object key = (cell.getAttributes()).get("id");
						if (key != null) {
							Integer idInteger = Integer.valueOf(Integer
									.parseInt(key.toString()));
							Map<String, Border> map = new Hashtable<String, Border>();

							if (resultInteger.contains(idInteger)) {
								System.out.println("cell:" + idInteger);
								// System.out.println(cell.getAttributes());
								Border predBorder = BorderFactory.createLineBorder(Color.cyan, 5);
								map = new Hashtable<String, Border>();
								map.put("border", predBorder);
								nested.put(cell, map);

							}
						}

					}
				}
				view.edit(nested);
			}
		}
	}
}
