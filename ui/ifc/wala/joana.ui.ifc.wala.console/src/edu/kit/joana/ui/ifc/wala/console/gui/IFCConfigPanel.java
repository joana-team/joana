/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.ui.ifc.wala.console.gui;

import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.regex.Pattern;

import javax.swing.BorderFactory;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.MutableComboBoxModel;
import javax.swing.event.ChangeEvent;
import javax.swing.filechooser.FileFilter;

import edu.kit.joana.ifc.sdg.mhpoptimization.MHPType;
import edu.kit.joana.ifc.sdg.util.JavaMethodSignature;
import edu.kit.joana.ui.ifc.wala.console.console.IFCConsole;
import edu.kit.joana.ui.ifc.wala.console.io.IFCConsoleOutput.Answer;
import edu.kit.joana.util.Stubs;
import edu.kit.joana.wala.core.SDGBuilder.ExceptionAnalysis;
import edu.kit.joana.wala.core.SDGBuilder.PointsToPrecision;

@SuppressWarnings({"rawtypes", "unchecked"})
public class IFCConfigPanel extends JPanel {

	private static final long serialVersionUID = -8606108705404396745L;

	private final IFCConsoleGUI consoleGui;
	private final JComboBox entryMethodSelect = new JComboBox();
	private boolean ignoreSelection = false;
	private final EntryMethodsComboBoxModel entryMethodsModel = new EntryMethodsComboBoxModel();
	private final JTextField classPathInput = new JTextField();
	private final JButton selectClassPath = new JButton("browse");
	private final JButton entryMethodUpdate = new JButton("update");
	private final JCheckBox autoSaveSDGCheckbox = new JCheckBox("auto-save");
	private final JLabel sdgStatusLabel = new JLabel("<no sdg in memory>");
	private final JCheckBox compIFECheckbox = new JCheckBox("compute interference edges");
	private final JComboBox mhpCombo = new JComboBox();
	private final JComboBox exceptionCombo = new JComboBox();
	private final JComboBox pointstoCombo = new JComboBox();
	private final JComboBox stubsCombo = new JComboBox();
	private final JButton loadSDG = new JButton("load SDG from file");
	private final JButton saveSDG = new JButton("save current SDG as");
	private final JButton buildSDG = new JButton("build");
	private final JLabel latticeLabel = new JLabel("Security lattice: ");
	private final JComboBox curLatticeComboBox = new JComboBox();
	private final JButton loadScript = new JButton("load script");
	private final JButton saveScript = new JButton("save script");

	private static final String MHP_NONE = "no may-happen-in-parallel analysis";
	private static final String MHP_SIMPLE = "simple may-happen-in-parallel analysis";
	private static final String MHP_PRECISE = "precise may-happen-in-parallel analysis";
	
	private static final String LATTICE_BINARY = "binary lattice low <= high";
	private static final String LATTICE_TERNARY = "ternary lattice low <= mid <= high";
	private static final String LATTICE_DIAMOND = "diamond lattice low <= midA <= high, low <= midB <= high";

	public IFCConfigPanel(final IFCConsoleGUI console) {
		super();
		this.consoleGui = console;
		init();
	}

	private void init() {
		final GridBagLayout gbl = new GridBagLayout();
		this.setLayout(gbl);
		final JLabel classPathLabel = new JLabel("Classpath: ");
		selectClassPath.addActionListener(makeSelectClasspathAction());
		classPathInput.addFocusListener(makeClassPathFocusListener());

		final JLabel entryMethodLabel = new JLabel("Entry method:");
		entryMethodSelect.setModel(entryMethodsModel);
		entryMethodSelect.addItemListener(makeSelectEntryListener());
		entryMethodUpdate.addActionListener(makeMethodUpdateListener());

		loadSDG.addActionListener(makeLoadSDGAction());
		buildSDG.addActionListener(makeBuildSDGAction());
		// curSDGTextField.setEditable(true);
		buildSDG.setPreferredSize(new Dimension(80, 30));

		final JPanel entryGroup = new JPanel();
		entryGroup.setBorder(BorderFactory.createTitledBorder("1. Choose program and entry point"));
		entryGroup.setLayout(new GridBagLayout());
		this.add(entryGroup, GUIUtil.mkgbc_fillx(0, 0, GridBagConstraints.REMAINDER, 1));

		entryGroup.add(classPathLabel, GUIUtil.mkgbc_nofill(0, 0, 1, 1));
		entryGroup.add(classPathInput, GUIUtil.mkgbc_fillx(1, 0, 1, 1));
		entryGroup.add(selectClassPath, GUIUtil.mkgbc_nofill(2, 0, 1, 1));

		entryGroup.add(entryMethodLabel, GUIUtil.mkgbc_nofill(0, 1, 1, 1));
		entryGroup.add(entryMethodSelect, GUIUtil.mkgbc_fillx(1, 1, 1, 1));
		entryGroup.add(entryMethodUpdate, GUIUtil.mkgbc_nofill(2, 1, 1, 1));

		final JPanel sdgGroup = new JPanel();
		sdgGroup.setBorder(BorderFactory.createTitledBorder("2. System Dependence Graph"));
		sdgGroup.setLayout(new GridBagLayout());

		sdgGroup.add(makeSDGBuildOptionsPanel(), GUIUtil.mkgbc_fillxy(0, 0, 1, 1));
		sdgGroup.add(makeSDGStatusPanel(), GUIUtil.mkgbc_fillx(0, 1, 1, 1));
		this.add(sdgGroup, GUIUtil.mkgbc_fillxy(0, 2, GridBagConstraints.REMAINDER, 1));
		//
		// sdgGroup.add(curSDGLabel, GUIUtil.mkgbc_nofill(0, 0, 1, 1));
		// sdgGroup.add(curSDGTextField, GUIUtil.mkgbc_fillx(1, 0, 1, 1));
		// sdgGroup.add(loadSDG, GUIUtil.mkgbc_nofill(2, 0, 1, 1));
		// sdgGroup.add(buildSDG, GUIUtil.mkgbc_nofill(2, 1, 1, 1));
		//
		// initExceptionCombo();
		// sdgGroup.add(new JLabel("exception analysis: "),
		// GUIUtil.mkgbc_nofill(0, 1, 1, 1));
		// sdgGroup.add(exceptionCombo, GUIUtil.mkgbc_nofill(1, 1, 1, 1));
		// compIFECheckbox.addChangeListener(new ChangeListener() {
		//
		// @Override
		// public void stateChanged(ChangeEvent e) {
		// mhpCombo.setEnabled(compIFECheckbox.isSelected());
		// }
		//
		// });
		// sdgGroup.add(compIFECheckbox, GUIUtil.mkgbc_nofill(0, 2, 1, 1));
		// initMHPCombo();
		// sdgGroup.add(mhpCombo, GUIUtil.mkgbc_nofill(1, 2, 1, 1));

		final JPanel latGroup = new JPanel();
		latGroup.setBorder(BorderFactory.createTitledBorder("3. Security Lattice"));
		latGroup.setLayout(new GridBagLayout());
		this.add(latGroup, GUIUtil.mkgbc_fillx(0, 3, GridBagConstraints.REMAINDER, 1));
		

		
		initLatticeComboBox();
		latGroup.add(latticeLabel, GUIUtil.mkgbc_nofill(0, 0, 1, 1));
		latGroup.add(curLatticeComboBox, GUIUtil.mkgbc_fillx(1, 0, 1, 1));

		final JPanel scriptGroup = new JPanel();
		scriptGroup.setBorder(BorderFactory.createTitledBorder("4. Configuration Script"));
		scriptGroup.setLayout(new GridBagLayout());
		this.add(scriptGroup, GUIUtil.mkgbc_fillx(0, 4, GridBagConstraints.REMAINDER, 1));
		loadScript.addActionListener(makeLoadScriptAction());
		saveScript.addActionListener(makeSaveScriptAction());
		scriptGroup.add(loadScript, GUIUtil.mkgbc_nofill(0, 0, 1, 1));
		scriptGroup.add(saveScript, GUIUtil.mkgbc_nofill(1, 0, 1, 1));
		final GridBagConstraints gbcFill = GUIUtil.mkgbc_fillxy(0, 10, GridBagConstraints.REMAINDER, 1);
		gbcFill.weighty = 4;
		this.add(new JPanel(), gbcFill);
		validate();
	}
	
	private void initLatticeComboBox() {
		MutableComboBoxModel latticeTypes = new DefaultComboBoxModel();
		latticeTypes.addElement(LATTICE_BINARY);
		latticeTypes.addElement(LATTICE_TERNARY);
		latticeTypes.addElement(LATTICE_DIAMOND);
		curLatticeComboBox.setModel(latticeTypes);
		curLatticeComboBox.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				consoleGui.execSpecifyLattice(getCurrentLattice());
			}
			
		});
		
		curLatticeComboBox.setEditable(false);
	}

	private JPanel makeSDGBuildOptionsPanel() {
		final JPanel ret = new JPanel();
		ret.setBorder(BorderFactory.createTitledBorder("SDG Build options"));
		ret.setLayout(new GridBagLayout());

		ret.add(new JLabel("Analyze exceptions: "), GUIUtil.mkgbc_nofill(0, 0, 1, 1));
		initExceptionCombo();
		ret.add(exceptionCombo, GUIUtil.mkgbc_nofill(1, 0, 1, 1));
		
		ret.add(new JLabel("Points-to precision: "), GUIUtil.mkgbc_nofill(0, 1, 1, 1));
		initPointsToCombo();
		ret.add(pointstoCombo, GUIUtil.mkgbc_nofill(1,  1, 1, 1));

		compIFECheckbox.addItemListener(new ItemListener() {
			@Override
			public void itemStateChanged(ItemEvent e) {
				if (!ignoreSelection) {
					if (e.getStateChange() == ItemEvent.SELECTED) {
						consoleGui.execSetComputeInterferences(true);
					} else if (e.getStateChange() == ItemEvent.DESELECTED) {
						consoleGui.execSetComputeInterferences(false);
					}
				}
			}

		});
		ret.add(compIFECheckbox, GUIUtil.mkgbc_nofill(0, 2, 1, 1));
		initMHPCombo();
		ret.add(mhpCombo, GUIUtil.mkgbc_nofill(1, 2, 1, 1));

		
		
		ret.add(new JLabel("Choose stubs: "), GUIUtil.mkgbc_nofill(0, 3, 1, 1));
		initStubsCombo();
		ret.add(stubsCombo, GUIUtil.mkgbc_nofill(1, 3, 1, 1));
		stubsCombo.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				consoleGui.execSetStubsPath(stubsCombo.getSelectedItem().toString());
			}

			

		});
	
		ret.add(buildSDG, GUIUtil.mkgbc_nofill(0, 4, 1, 1));
		ret.add(autoSaveSDGCheckbox, GUIUtil.mkgbc_fillx(1, 4, 1, 1));

		return ret;
	}

	private JPanel makeSDGStatusPanel() {
		JPanel ret = new JPanel();
		ret.setBorder(BorderFactory.createTitledBorder("status"));
		ret.setLayout(new GridBagLayout());
		ret.add(sdgStatusLabel, GUIUtil.mkgbc_fillx(0, 0, 1, 1));
		ret.add(loadSDG, GUIUtil.mkgbc_nofill(1, 0, 1, 1));
		ret.add(saveSDG, GUIUtil.mkgbc_nofill(2, 0, 1, 1));
		saveSDG.addActionListener(makeSaveSDGAction());
		saveSDG.setEnabled(false);
		return ret;
	}

	private void initMHPCombo() {
		MutableComboBoxModel mhpTypes = new DefaultComboBoxModel();
		mhpTypes.addElement(new ElementWithDescription<MHPType>(MHPType.NONE, MHP_NONE));
		mhpTypes.addElement(new ElementWithDescription<MHPType>(MHPType.SIMPLE, MHP_SIMPLE));
		mhpTypes.addElement(new ElementWithDescription<MHPType>(MHPType.PRECISE, MHP_PRECISE));
		mhpCombo.setModel(mhpTypes);
		mhpCombo.setSelectedItem(consoleGui.getMHPType());
		mhpCombo.setEnabled(false);
		mhpCombo.addItemListener(makeSelectMHPTypeListener());
	}
	
	private void initStubsCombo() {
		final MutableComboBoxModel possibleStubs = new DefaultComboBoxModel();
		for (Stubs stubs : Stubs.values()) {
			possibleStubs.addElement(stubs);
		}
		stubsCombo.setModel(possibleStubs);
		stubsCombo.setSelectedItem(consoleGui.getStubsPath());
		stubsCombo.setEditable(false);
	}

	private void initExceptionCombo() {
		final MutableComboBoxModel exceptionTypes = new DefaultComboBoxModel();
		for (final ExceptionAnalysis elem : ExceptionAnalysis.values()) {
			if (elem.recommended) {
				exceptionTypes.addElement(new ElementWithDescription<ExceptionAnalysis>(elem, elem.desc));	
			}
		}

		exceptionCombo.setModel(exceptionTypes);
		exceptionCombo.addItemListener(makeSelectExceptionAnalysisListener());
	}

	private void initPointsToCombo() {
		final MutableComboBoxModel pointstoTypes = new DefaultComboBoxModel();
		for (final PointsToPrecision elem: PointsToPrecision.values()) {
			if (elem.recommended) {
				pointstoTypes.addElement(new ElementWithDescription<PointsToPrecision>(elem, elem.desc));
			}
		}

		pointstoCombo.setModel(pointstoTypes);
		pointstoCombo.addItemListener(makeSelectPointsToListener());
	}

	private ActionListener makeLoadScriptAction() {
		return new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				final JFileChooser choose = new JFileChooser();
				choose.setFileSelectionMode(JFileChooser.FILES_ONLY);
				int retval = choose.showOpenDialog(getRootPane());
				if (retval == JFileChooser.APPROVE_OPTION) {
					consoleGui.execLoadScript(choose.getSelectedFile().getAbsolutePath());
				}
			}
		};
	}

	private ActionListener makeSaveScriptAction() {
		return new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				final JFileChooser choose = new JFileChooser();
				choose.setFileSelectionMode(JFileChooser.FILES_ONLY);
				int retval = choose.showSaveDialog(getRootPane());
				if (retval == JFileChooser.APPROVE_OPTION) {
					consoleGui.execSaveScript(choose.getSelectedFile().getAbsolutePath());
				}
			}
		};
	}

	private String translateLattice(String latticeSpec) {
		if (LATTICE_BINARY.equals(latticeSpec)) {
			return IFCConsole.LATTICE_BINARY;
		} else if (LATTICE_TERNARY.equals(latticeSpec)) {
			return IFCConsole.LATTICE_TERNARY;
		} else if (LATTICE_DIAMOND.equals(latticeSpec)) {
			return IFCConsole.LATTICE_DIAMOND;
		} else {
			throw new Error(String.format("The combo box for preset lattices should not provide non-translateable lattice specification %s! Please report this as an error!", latticeSpec));
		}
	}

	private FocusListener makeClassPathFocusListener() {
		return new FocusListener() {

			@Override
			public void focusLost(FocusEvent e) {
				final String txt = classPathInput.getText().trim();
				final String ctxt = (consoleGui.getClassPath() == null ? "" : consoleGui.getClassPath().trim());

				if (!ctxt.equals(txt)) {
					consoleGui.execSetClassPath(txt);
				}
			}

			@Override
			public void focusGained(FocusEvent e) {
			}
		};
	}

	private ItemListener makeSelectEntryListener() {
		return new ItemListener() {

			private Object previous = null;

			@Override
			public void itemStateChanged(ItemEvent e) {
				if (!ignoreSelection && e.getStateChange() == ItemEvent.SELECTED) {
					final Object item = e.getItem();

					if (previous != item && item instanceof JavaMethodSignature) {
						final JavaMethodSignature sig = (JavaMethodSignature) item;
						consoleGui.execSetEntryMethod(sig);
						previous = item;
					}
				}
			}
		};
	}

	private ActionListener makeMethodUpdateListener() {
		return new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				consoleGui.execSearchForEntries();
			}

		};
	}

	private ActionListener makeSelectClasspathAction() {
		return new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent arg0) {
				final JFileChooser choose = new JFileChooser("./");
				choose.setFileFilter(new FileFilter() {

					@Override
					public String getDescription() {
						return "Directories or *.jar";
					}

					@Override
					public boolean accept(File arg0) {
						return arg0.isDirectory() || (arg0.isFile() && arg0.getName().endsWith(".jar"));
					}
				});
				choose.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
				choose.setDialogTitle("Select binary directory or .jar file of the program");
				choose.validate();
				final int retval = choose.showOpenDialog(getRootPane());

				if (retval == JFileChooser.APPROVE_OPTION) {
					String path = choose.getSelectedFile().getAbsolutePath();
					File f = new File(path);
					if (f.exists()) {
						classPathInput.setText(path);
						consoleGui.execSetClassPath(classPathInput.getText());
					} else {
						JOptionPane.showMessageDialog(IFCConfigPanel.this, "Selected file or directory '" + path
								+ "' does not exist! Please select again!", "Selected file not found",
								JOptionPane.ERROR_MESSAGE);
					}
				}
			}

		};
	}

	private ItemListener makeSelectPointsToListener() {
		return new ItemListener() {

			private Object previous = null;

			@Override
			public void itemStateChanged(ItemEvent e) {
				if (!ignoreSelection && e.getStateChange() == ItemEvent.SELECTED) {
					final Object item = e.getItem();

					if (previous != item && item instanceof ElementWithDescription<?>) {
						final ElementWithDescription<Object> elem = (ElementWithDescription<Object>) item;
						if (elem.element instanceof PointsToPrecision) {
							consoleGui.execSetPointsTo((PointsToPrecision) elem.element);
							previous = item;
						}
					}
				}
			}
		};
	}

	private ItemListener makeSelectMHPTypeListener() {
		return new ItemListener() {

			private Object previous = null;

			@Override
			public void itemStateChanged(ItemEvent e) {
				if (!ignoreSelection && e.getStateChange() == ItemEvent.SELECTED) {
					final Object item = e.getItem();

					if (previous != item && item instanceof ElementWithDescription<?>) {
						final ElementWithDescription<Object> elem = (ElementWithDescription<Object>) item;
						if (elem.element instanceof MHPType) {
							consoleGui.execSetMHPType((MHPType) elem.element);
							previous = item;
						}
					}
				}
			}
		};
	}


	private ItemListener makeSelectExceptionAnalysisListener() {
		return new ItemListener() {

			private Object previous = null;

			@Override
			public void itemStateChanged(ItemEvent e) {
				if (!ignoreSelection && e.getStateChange() == ItemEvent.SELECTED) {
					final Object item = e.getItem();

					if (previous != item && item instanceof ElementWithDescription<?>) {
						final ElementWithDescription<Object> elem = (ElementWithDescription<Object>) item;
						if (elem.element instanceof ExceptionAnalysis) {
							consoleGui.execSetExcAnalysis((ExceptionAnalysis) elem.element);
							previous = item;
						}
					}
				}
			}
		};
	}

	private ActionListener makeSaveSDGAction() {
		return new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				if (consoleGui.getSDG() != null) {
					final JFileChooser choose = new JFileChooser("./");
					choose.setFileSelectionMode(JFileChooser.FILES_ONLY);
					choose.setFileFilter(new FileFilter() {

						@Override
						public boolean accept(File arg0) {
							return arg0.isDirectory() || Pattern.matches(".*\\.pdg", arg0.getName());
						}

						@Override
						public String getDescription() {
							return "SDG files (*.pdg)";
						}

					});
					boolean approvedAndSuccess = false;
					boolean canceled = false;
					while (!approvedAndSuccess && !canceled) {
						approvedAndSuccess = true;
						canceled = false;
						int retval = choose.showSaveDialog(getRootPane());
						if (retval == JFileChooser.APPROVE_OPTION) {
							File selectedFile = choose.getSelectedFile();
							String path = selectedFile.getAbsolutePath();
							boolean doSave = true;
							if (path.endsWith(".pdg")) {
								if (selectedFile.exists()) {
									Answer a = consoleGui.question("The selected file already exists. Overwrite? (Y/N)");
									doSave = (a == Answer.YES);
								}
								if (doSave) {
									consoleGui.execSaveSDG(choose.getSelectedFile().getAbsolutePath());
									sdgStatusLabel.setText("current sdg stored in: "
											+ choose.getSelectedFile().getAbsolutePath());
								} else {
									approvedAndSuccess = false;
								}
							} else {
								consoleGui.error("Chosen file has wrong suffix! Must end with '.pdg'!");
								approvedAndSuccess = false;
							}
						} else {
							canceled = true;
						}
					}
				}
			}
		};
	}

	private ActionListener makeLoadSDGAction() {
		return new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				final JFileChooser choose = new JFileChooser("./");
				choose.setFileSelectionMode(JFileChooser.FILES_ONLY);
				choose.setFileFilter(new FileFilter() {

					@Override
					public boolean accept(File arg0) {
						return arg0.isDirectory() || Pattern.matches(".*\\.pdg", arg0.getName());
					}

					@Override
					public String getDescription() {
						return "SDG files";
					}

				});

				int retval = choose.showOpenDialog(getRootPane());
				if (retval == JFileChooser.APPROVE_OPTION) {
					consoleGui.execLoadSDG(choose.getSelectedFile().getAbsolutePath());
					sdgStatusLabel.setText("current SDG loaded from: " + choose.getSelectedFile().getAbsolutePath());
				}
			}
		};
	}

	private ActionListener makeBuildSDGAction() {
		return new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent arg0) {
				
				

				final String cp = consoleGui.getClassPath();
				final JavaMethodSignature entry = consoleGui.getEntryMethod();

				if ((cp == null || cp.isEmpty()) || (entry == null)) {
					consoleGui.info("Please select classpath and entry method first.");
				} else {
					String saveSDGPath = null;
					if (autoSaveSDGCheckbox.isSelected()) {
						try {
							saveSDGPath = new File(consoleGui.getEntryMethod().getFullyQualifiedMethodName() + ".pdg")
									.getCanonicalPath();
						} catch (IOException e) {
							consoleGui.error("I/O error while saving SDG to "
									+ consoleGui.getEntryMethod().getFullyQualifiedMethodName() + ".pdg");
						}
					}
					consoleGui.execBuildSDG(saveSDGPath);
					if (saveSDGPath != null) {
						sdgStatusLabel.setText("current SDG stored in: " + saveSDGPath);
					} else {
						sdgStatusLabel.setText("unsaved SDG with entry method: "
								+ consoleGui.getEntryMethod().toHRString());
					}
					//consoleGui.pack();
					// curSDGTextField.setText(console.getSDGFile());
					// loadProgramIntoTree(programRoot);
					// programRoot.setUserObject(choose.getSelectedFile().getName());
					// programTree.expandRow(0);
					// repaint();
				}

			}

		};
	}
	
	public Stubs getCurrentStubs() {
		return Stubs.fromString(stubsCombo.getSelectedItem().toString());
	}

	public void searchEntryStarted() {
		entryMethodsModel.searchStarted();
	}

	public void searchEntryFinished(List<JavaMethodSignature> found) {
		entryMethodsModel.searchFinished(found);
	}

	public void updateEntries() {
		classPathInput.setText(consoleGui.getClassPath());
		final int selIndex = entryMethodSelect.getSelectedIndex();
		final int cselIndex = consoleGui.getActiveEntryMethodIndex();
		if (selIndex != cselIndex) {
			ignoreSelection = true;
			entryMethodSelect.setSelectedIndex(cselIndex);
			entryMethodSelect.repaint();
			ignoreSelection = false;
		}

		final PointsToPrecision pts = consoleGui.getPointsTo();
		for (int ptsIndex = 0; ptsIndex < pointstoCombo.getItemCount(); ptsIndex++) {
			final ElementWithDescription<PointsToPrecision> item =
					(ElementWithDescription<PointsToPrecision>) pointstoCombo.getItemAt(ptsIndex);
			if (item.element == pts) {
				ignoreSelection = true;
				pointstoCombo.setSelectedIndex(ptsIndex);
				pointstoCombo.repaint();
				ignoreSelection = false;
				break;
			}
		}

		final ExceptionAnalysis exc = consoleGui.getExceptionAnalysis();
		for (int excIndex = 0; excIndex < exceptionCombo.getItemCount(); excIndex++) {
			final ElementWithDescription<ExceptionAnalysis> item =
					(ElementWithDescription<ExceptionAnalysis>) exceptionCombo.getItemAt(excIndex);
			if (item.element == exc) {
				ignoreSelection = true;
				exceptionCombo.setSelectedIndex(excIndex);
				exceptionCombo.repaint();
				ignoreSelection = false;
				break;
			}
		}
		ignoreSelection = true;
		compIFECheckbox.setSelected(consoleGui.getComputeInterferences());
		compIFECheckbox.repaint();
		ignoreSelection = false;
		mhpCombo.setEnabled(compIFECheckbox.isSelected());
		final MHPType mhp = consoleGui.getMHPType();
		for (int mhpIndex = 0; mhpIndex < mhpCombo.getItemCount(); mhpIndex++) {
			final ElementWithDescription<MHPType> item = (ElementWithDescription<MHPType>) mhpCombo.getItemAt(mhpIndex);
			if (item.element == mhp) {
				ignoreSelection = true;
				mhpCombo.setSelectedIndex(mhpIndex);
				mhpCombo.repaint();
				ignoreSelection = true;
				break;
			}
		}
	}

	public void mute() {
		setEnabled(this, false);
	}

	public void unmute() {
		setEnabled(this, true);
		mhpCombo.setEnabled(compIFECheckbox.isSelected());
	}

	private static void setEnabled(Component c, boolean enabled) {
		if (c instanceof Container) {
			Container ccont = (Container) c;
			for (Component c0 : ccont.getComponents()) {
				setEnabled(c0, enabled);
			}
		}

		c.setEnabled(enabled);
	}

	public boolean computeInterferenceEdges() {
		return compIFECheckbox.isSelected();
	}

	public MHPType getMHPType() {
		return ((ElementWithDescription<MHPType>) mhpCombo.getSelectedItem()).getElement();
	}
	
	public String getCurrentLattice() {
		return translateLattice(curLatticeComboBox.getSelectedItem().toString());
	}

	public ExceptionAnalysis getExceptionAnalysisType() {
		return ((ElementWithDescription<ExceptionAnalysis>) exceptionCombo.getSelectedItem()).getElement();
	}

	public PointsToPrecision getPointsToPrecision() {
		return ((ElementWithDescription<PointsToPrecision>) pointstoCombo.getSelectedItem()).getElement();
	}

	public String getSDGFile() {
		return sdgStatusLabel.getText();
	}

	public void sdgLoadedOrBuilt() {
		saveSDG.setEnabled(true);
	}

	private static class ElementWithDescription<A> {
	
		private final A element;
		private final String description;
	
		private ElementWithDescription(A element, String description) {
			this.element = element;
			this.description = description;
		}
	
		public A getElement() {
			return element;
		}
	
		@Override
		public String toString() {
			return description;
		}
	
	}
}
