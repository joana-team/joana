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
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import javax.swing.JButton;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTree;
import javax.swing.tree.TreePath;

import edu.kit.joana.api.annotations.AnnotationType;
import edu.kit.joana.api.annotations.IFCAnnotation;
import edu.kit.joana.api.sdg.SDGClass;
import edu.kit.joana.api.sdg.SDGProgramPart;
import edu.kit.joana.ifc.sdg.lattice.IStaticLattice;
import edu.kit.joana.ui.ifc.wala.console.gui.IFCConsoleGUI.Command;
import edu.kit.joana.ui.ifc.wala.console.gui.tree.IFCTreeCellRenderer;
import edu.kit.joana.ui.ifc.wala.console.gui.tree.IFCTreeModel;
import edu.kit.joana.ui.ifc.wala.console.gui.tree.IFCTreeNode;




public class IFCTreePanel extends JPanel {

	private static final long serialVersionUID = 206500415193181862L;

	private final IFCConsoleGUI consoleGui;

	// private DefaultMutableTreeNode programRoot = new
	// DefaultMutableTreeNode();
	private final JTree programTree;
	private final IFCTreeModel treeModel;
	private final JScrollPane treeView;

	private final Set<SDGClass> classes = new HashSet<SDGClass>();
	//private Collection<IFCAnnotation> sources = new LinkedList<IFCAnnotation>();
	//private Collection<IFCAnnotation> sinks = new LinkedList<IFCAnnotation>();
	//private Collection<IFCAnnotation> declasses = new LinkedList<IFCAnnotation>();

	public IFCTreePanel(final IFCConsoleGUI console) {
		super();
		this.consoleGui = console;

		final GridBagLayout gbl = new GridBagLayout();
		this.setLayout(gbl);

		this.treeModel = new IFCTreeModel();
		this.programTree = new JTree(treeModel);
		this.programTree.setCellRenderer(new IFCTreeCellRenderer(consoleGui));
		this.programTree.setRootVisible(true);
		this.treeView = new JScrollPane(programTree,
				JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
				JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
		this.treeView.setMinimumSize(new Dimension(600, 300));
		this.add(this.treeView,
				GUIUtil.mkgbc_fillxy(0, 0, GridBagConstraints.REMAINDER, 1));

		final JButton markAsSource = new JButton("Source");
		markAsSource.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				getAndApplyAnnotation(AnnotationType.SOURCE);
			}
		});
		this.add(markAsSource, GUIUtil.mkgbc_nofill(0, 1, 1, 1));

		final JButton markAsSink = new JButton("Sink");
		markAsSink.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				getAndApplyAnnotation(AnnotationType.SINK);
			}
		});
		this.add(markAsSink, GUIUtil.mkgbc_nofill(1, 1, 1, 1));

		final JButton markAsDeclass = new JButton("Declassify");
		markAsDeclass.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				getAndApplyAnnotation(AnnotationType.DECLASS);
			}
		});
		if (!IFCConsoleGUI.DECLASS_ENABLED) {
			markAsDeclass.setVisible(false);
		}
		this.add(markAsDeclass, GUIUtil.mkgbc_nofill(2, 1, 1, 1));

		final JButton clearMark = new JButton("Clear");
		clearMark.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				Collection<SDGProgramPart> selectedParts = getSelectedMethodParts();
				if (selectedParts == null) {
					consoleGui.error("Selected a not annotatable node.");
				} else if (selectedParts.isEmpty()) {
					consoleGui.info("No program parts selected.");
				} else {
					final List<Command> cmds = new LinkedList<Command>();
					for (SDGProgramPart part : selectedParts) {
						cmds.add(consoleGui.createCmdClear(part));
					}
					consoleGui.executeCmdList(cmds);
				}
			}
		});

		this.add(clearMark, GUIUtil.mkgbc_nofill(3, 1, 1, 1));

		// spacer between buttons relating to marked elements and "clear all"
		this.add(new JPanel(), GUIUtil.mkgbc_fillx(4, 1, 1, 1));

		final JButton clearAll = new JButton("Clear All");

		clearAll.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent arg0) {
				consoleGui.execClearAll();
			}

		});
		this.add(clearAll, GUIUtil.mkgbc_nofill(5, 1, 1, 1));
	}

	protected void getAndApplyAnnotation(AnnotationType type) {
		Collection<SDGProgramPart> selectedParts = getSelectedMethodParts();
		if (selectedParts == null) {
			consoleGui.error("You selected a node which cannot be annotated!");
			return;
		}

//		for (SDGProgramPart part : selectedParts) {
//			if (consoleGui.getIFCAnnotationManager().isAnnotated(part)) {
//
//				Answer answer = consoleGui
//						.question("At least one of the selected program parts is already annotated. Do you want to overwrite these annotations?");
//				if (answer == Answer.NO) {
//					break;
//				} else {
//					return;
//				}
//			}

//			SDGProgramPart interf = consoleGui.getInterferingPart(part, type);
//			if (interf != null) {
//				consoleGui.error("Cannot annotate " + part + " as " + type.toString().toLowerCase() + " because of interference with already annotated program parts!");
//				return;
//			}

		if (consoleGui.canAnnotate(selectedParts, type) && !selectedParts.isEmpty()) {
			String secLevel1 = null;
			String secLevel2 = null;
			switch (type) {
			case SOURCE:
				secLevel1 = promptForSecurityLevel("Security level for source", type);
				if (secLevel1 == null) {
					return;
				}
				break;
			case SINK:
				secLevel1 = promptForSecurityLevel("Security level for sink", type);
				if (secLevel1 == null) {
					return;
				}
				break;
			case DECLASS:
				DeclassSelection d = new DeclassSelection(consoleGui, consoleGui.getLattice());
				d.setVisible(true);
				secLevel1 = d.getLevel1();
				secLevel2 = d.getLevel2();
				if (secLevel1.equals(secLevel2)) {
					consoleGui.info("Specified declassification has no effect.");
					return;
				}
				break;
			default:
				throw new IllegalStateException();
			}
			final List<Command> cmds = new LinkedList<Command>();

			for (SDGProgramPart part : selectedParts) {
				Command c;
				switch (type) {
				case SOURCE:
					c = consoleGui.createCmdMarkAsSource(part, secLevel1);
					break;
				case SINK:
					c = consoleGui.createCmdMarkAsSink(part, secLevel1);
					break;
				case DECLASS:
					c = consoleGui.createCmdDeclassify(part, secLevel1,
							secLevel2);
					break;
				default:
					throw new IllegalStateException();
				}
				cmds.add(c);
			}

			consoleGui.executeCmdList(cmds);

		} else {
		    if (selectedParts.isEmpty()) {
			consoleGui.info("No program parts selected.");
		    }
		}
	}

	private String promptForSecurityLevel(String title, AnnotationType type) {
		assert type == AnnotationType.SOURCE || type == AnnotationType.SINK;
		ArrayList<String> availSecLevels = new ArrayList<String>(consoleGui.getSecurityLevels());
		if (type == AnnotationType.SOURCE) {
			Collections.sort(availSecLevels, LatticeComparator.<String>makeReversed(consoleGui.getLattice()));
		} else {
			Collections.sort(availSecLevels, LatticeComparator.<String>make(consoleGui.getLattice()));
		}
		String secLevel = (String) JOptionPane.showInputDialog(consoleGui
				.getRootPane(), "Select security level", title,
				JOptionPane.QUESTION_MESSAGE, null, availSecLevels.toArray(), availSecLevels.get(0));
		assert secLevel == null
				|| consoleGui.getSecurityLevels().contains(secLevel);
		if (secLevel == null) {
			return null;
		} else {
			return secLevel;
		}
	}

	private Collection<SDGProgramPart> getSelectedMethodParts() {
		final TreePath[] selections = programTree.getSelectionPaths();
		Collection<SDGProgramPart> ret = new LinkedList<SDGProgramPart>();
		if (selections == null) {
			return Collections.emptyList();
		}

		for (final TreePath tp : selections) {
			IFCTreeNode node = (IFCTreeNode) tp.getLastPathComponent();
			if (!node.isAnnotateable()) {
				return null;
			}
			if (node.getUserObject() instanceof SDGProgramPart) {
				SDGProgramPart part = (SDGProgramPart) node.getUserObject();
				ret.add(part);
			}
		}
		return ret;
	}

	public void mute() {
		treeView.setEnabled(false);
	}

	public void unmute() {
		treeView.setEnabled(true);
	}

	public void updateEntries(Collection<IFCAnnotation> sources,
			Collection<IFCAnnotation> sinks, Collection<IFCAnnotation> declasses) {
		final Collection<SDGClass> newClasses = consoleGui.getClasses();

		if (!classes.equals(newClasses)) {
			if (newClasses.isEmpty()) {
				treeModel.clearMethods();
			} else {
				// a change occurred
				treeModel.updateClasses(newClasses, consoleGui.getSDGFile());
				treeModel.updateAnnotations(sources, sinks, declasses);
			}
			classes.clear();
			classes.addAll(newClasses);
		} else /**if (!this.sources.equals(sources) || !this.sinks.equals(sinks)
				|| !this.declasses.equals(declasses))*/ {
			// annotations changed -> update
			treeModel.updateAnnotations(sources, sinks, declasses);
		}
	}

	/**
	 * Provides a possibility to totally order the elements of a lattice if the element type of the lattice
	 * implements the {@link java.util.Comparable} interface (and obeys its contract).<br>
	 * Elements of the lattice are first tried to be compared according to the lattice. If they are incomparable
	 * in the lattice, the comparison method falls back to their type's comparator.<br>
	 * It is assumed that the {@link #compare(Comparable, Comparable)} method is only called with elements of the
	 * lattice.<br>
	 * @author Martin Mohr
	 */
	private static class LatticeComparator<C extends Comparable<C>> implements Comparator<C> {

		/** the lattice which provides the initial semi-order */
		private final IStaticLattice<C> l;

		/** direction modificator for the lattice part of this comparator - can be positive (does not modify the lattice relation) or negative (reverses the lattice relation) */
		private final int dir;

		/** for usage of normal lattice relation */
		private static final int ASCENDING = 1;

		/** for usage of reversed lattice relation */
		private static final int DESCENDING = -1;

		/**
		 * Constructs a new lattice-based comparator.
		 * @param l the lattice which is to be used as initial comparison relation
		 * @param dir direction to be used - positive if the lattice semi-order is to be used in normal direction, negative if it shall be reversed
		 */
		private LatticeComparator(IStaticLattice<C> l, int dir) {
			assert l != null && dir != 0;
			this.l = l;
			this.dir = dir;
		}

		/* (non-Javadoc)
		 * @see java.util.Comparator#compare(java.lang.Object, java.lang.Object)
		 */
		@Override
		public int compare(C lvl1, C lvl2) {
			if (lvl1.equals(lvl2)) {
				return 0;
			} else if (l.isLeq(lvl1, lvl2)) {
				return -1 * dir;
			} else if (l.isLeq(lvl2, lvl1)) {
				return 1 * dir;
			} else {
				return lvl1.compareTo(lvl2);
			}
		}

		/**
		 * @param l lattice to be used as initial semi-order
		 * @return new lattice-based comparator, which uses the given lattice relation as initial semi-order.
		 */
		static <D extends Comparable<D>> LatticeComparator<D> make(IStaticLattice<D> l) {
			return new LatticeComparator<D>(l, ASCENDING);
		}

		/**
		 * @param l lattice to be used as initial semi-order
		 * @return new lattice-based comparator, which uses the reversal of the given lattice relation as initial semi-order.
		 */
		static <D extends Comparable<D>> LatticeComparator<D> makeReversed(IStaticLattice<D> l) {
			return new LatticeComparator<D>(l, DESCENDING);
		}

	}

}
