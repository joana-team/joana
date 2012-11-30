/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.deprecated.jsdg.gui.create;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.StringTokenizer;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.debug.internal.ui.SWTFactory;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.dialogs.FilteredItemsSelectionDialog;

import com.ibm.wala.ide.util.ProgressMonitorDelegate;
import com.ibm.wala.shrikeCT.InvalidClassFileException;
import com.ibm.wala.util.CancelException;
import com.ibm.wala.util.WalaException;
import com.ibm.wala.util.collections.HashSetFactory;

import edu.kit.joana.deprecated.jsdg.Activator;
import edu.kit.joana.deprecated.jsdg.SDGFactory;
import edu.kit.joana.deprecated.jsdg.SDGFactory.Config.ObjTreeType;
import edu.kit.joana.deprecated.jsdg.SDGFactory.Config.PointsToType;
import edu.kit.joana.deprecated.jsdg.gui.create.JarSelectionDialog.JarFile;
import edu.kit.joana.deprecated.jsdg.gui.create.SDGConfigBuilder.Stubs;
import edu.kit.joana.deprecated.jsdg.gui.launch.LaunchConfig;
import edu.kit.joana.deprecated.jsdg.sdg.nodes.JDependencyGraph.PDGFormatException;
import edu.kit.joana.deprecated.jsdg.util.Log;
import edu.kit.joana.ifc.sdg.graph.SDGSerializer;

@SuppressWarnings("restriction") // I know I want to use internal.ui.SWTFactory...
public class SDGCreator extends org.eclipse.swt.widgets.Composite {

	private final ICompilationUnit javafile;
	private SDGFactory.Config config;
	private final IPath defaultCfg;
	private final String workspaceRoot;

	private Button create;
	private Button cancel;
	private Button save;
	private Button load;
	private Combo pointsToType;
	private Combo objTreeType;

	private Combo stubsJar;
	private Button useStdLibrary;

	private Text exclusions;
	private Text libraries;

	private Button exclusionsDefaultButton;
	private Button exclusionsClearButton;
	private Button exclusionsInvertButton;
//	private Button simpleDataDepButton;
	private Button ignoreExceptionsButton;
	private Button optimizeExceptionsButton;
	private Button addControlFlowButton;
	private Button computeSummaryButton;
	private Button useSummaryInline;
	private Button computeInterferenceButton;
	private Button noClinits;
	private Button useEscape;
	private Button nonTermination;
	private Button useJoanaCompiler;

	public SDGCreator(ICompilationUnit javafile, org.eclipse.swt.widgets.Composite parent, int style) {
		super(parent, style);
		this.javafile = javafile;
		this.config = SDGConfigBuilder.createConfig(javafile);
		this.defaultCfg = SDGConfigBuilder.getDefaultCfg(javafile);
		IPath path2 = ResourcesPlugin.getWorkspace().getRoot().getLocation();
		this.workspaceRoot = path2.toOSString();
	}

	public void show() throws JavaModelException, IOException {
		initGUI();
		File file = defaultCfg.toFile();
		if (file.exists()) {
			FileInputStream fIn = new FileInputStream(file);
			config = SDGFactory.Config.readFrom(fIn);
			SDGConfigBuilder.adjustConfigToLocalPaths(javafile, config);
		}
		initConfig(config);
	}

	private void initGUI() throws JavaModelException {
		Composite comp = this;
		GridLayout topLayout = new GridLayout(1, true);
		topLayout.horizontalSpacing = 10;
		comp.setLayout(topLayout);
		comp.setFont(this.getFont());

		GridData gd;

		{
			Label infoLabel = new Label(comp, SWT.SHADOW_NONE);
			infoLabel.setText("Create SDG for " + SDGConfigBuilder.getMainClassName(javafile) +
				" in project " + javafile.getJavaProject().getProject().getName());
		}

		Composite selGroup = new Composite(comp, SWT.NONE);
		{
			GridLayout selLayout = new GridLayout();
			selLayout.numColumns = 4;
			selGroup.setLayout(selLayout);
			gd = new GridData(GridData.FILL_HORIZONTAL);
			gd.horizontalSpan = 4;
			gd.verticalSpan = 1;
			selGroup.setLayoutData(gd);
		}

		/* Group Points-to type */
		{
			Group ptsGroup = new Group(selGroup, SWT.NONE);
			GridLayout ptsLayout = new GridLayout();
			ptsLayout.numColumns = 2;
			ptsGroup.setLayout(ptsLayout);
			gd = new GridData(GridData.FILL_HORIZONTAL);
			gd.horizontalSpan = 2;
			gd.verticalSpan = 1;
			ptsGroup.setLayoutData(gd);
			ptsGroup.setText("Select Points-to Analysis Algorithm:");
			ptsGroup.setFont(comp.getFont());

			pointsToType = new Combo(ptsGroup, SWT.READ_ONLY);
			for (SDGFactory.Config.PointsToType type : SDGFactory.Config.PointsToType.values()) {
				pointsToType.add(type.toString());
			}
			gd = new GridData(GridData.FILL_HORIZONTAL);
			gd.horizontalSpan = 1;
			pointsToType.setLayoutData(gd);

		}

		/* Group Objecttree type */
		{
			Group objGroup = new Group(selGroup, SWT.NONE);
			GridLayout objLayout = new GridLayout();
			objLayout.numColumns = 2;
			objGroup.setLayout(objLayout);
			gd = new GridData(GridData.FILL_HORIZONTAL);
			gd.horizontalSpan = 2;
			gd.verticalSpan = 1;
			objGroup.setLayoutData(gd);
			objGroup.setText("Select Parameter Passing Model:");
			objGroup.setFont(comp.getFont());

			objTreeType = new Combo(objGroup, SWT.READ_ONLY);
			for (SDGFactory.Config.ObjTreeType type : SDGFactory.Config.ObjTreeType.values()) {
				objTreeType.add(type.toString());
			}
			gd = new GridData(GridData.FILL_HORIZONTAL);
			gd.horizontalSpan = 1;
			objTreeType.setLayoutData(gd);
		}

		/* Stubs Group */
		{
			Group stubsGroup = new Group(comp, SWT.NONE);
			GridLayout stubsLayout = new GridLayout();
			stubsLayout.numColumns = 4;
			stubsGroup.setLayout(stubsLayout);
			gd = new GridData(GridData.FILL_HORIZONTAL);
			gd.horizontalSpan = 4;
			gd.verticalSpan = 3;
			stubsGroup.setLayoutData(gd);
			stubsGroup.setText("Select stubs and libraries to include:");
			stubsGroup.setFont(comp.getFont());

			Label stubsLabel = new Label(stubsGroup, SWT.LEFT);
			stubsLabel.setText("Select stubs:");

			stubsJar = new Combo(stubsGroup, SWT.READ_ONLY);
			for (SDGConfigBuilder.Stubs stub : SDGConfigBuilder.Stubs.values()) {
				stubsJar.add(stub.toString());
			}

			useStdLibrary = createCheckButton(stubsGroup, "Include java standard library");

			libraries =  new Text(stubsGroup, SWT.MULTI | SWT.BORDER | SWT.V_SCROLL);
			libraries.setEditable(false);
			gd = new GridData(GridData.FILL_BOTH);
			gd.horizontalSpan = 3;
			gd.verticalSpan = 2;
			libraries.setLayoutData(gd);

			Button addLibButton = createPushButton(stubsGroup, "Add Library", null);
			addLibButton.addSelectionListener(new SelectionAdapter() {
				public void widgetSelected(SelectionEvent evt) {
					handleAddLibraryButtonSelected();
				}
			});

			Button clearLibButton = createPushButton(stubsGroup, "Clear", null);
			clearLibButton.addSelectionListener(new SelectionAdapter() {
				public void widgetSelected(SelectionEvent evt) {
					String libs = libraries.getText();
					if (libs != null && libs.length() > 0) {
						libs = libs.trim();
						StringTokenizer sTok = new StringTokenizer(libs, LaunchConfig.NEW_LINE);
						Set<String> toDelete = new HashSet<String>();
						while (sTok.hasMoreTokens()) {
							String tok = sTok.nextToken();
							for (String str : config.scopeData) {
								if (str.contains(tok)) {
									toDelete.add(str);
								}
							}
						}

						for (String del : toDelete) {
							config.scopeData.remove(del);
						}
					}

					libraries.setText("");
				}
			});
		}

		/* Group Exclusions Class */
		{
			Group exclusionsGroup = new Group(comp, SWT.NONE);
			GridLayout exclusionsLayout = new GridLayout();
			exclusionsLayout.numColumns = 3;
			exclusionsGroup.setLayout(exclusionsLayout);
			gd = new GridData(GridData.FILL_HORIZONTAL);
			gd.horizontalSpan = 3;
			gd.verticalSpan = 3;
			exclusionsGroup.setLayoutData(gd);
			exclusionsGroup.setText("Set classes to exclude:");
			exclusionsGroup.setFont(comp.getFont());

			exclusions = new Text(exclusionsGroup, SWT.MULTI | SWT.BORDER | SWT.V_SCROLL);
			gd = new GridData(GridData.FILL_BOTH);
			gd.horizontalSpan = 2;
			gd.verticalSpan = 3;
			exclusions.setLayoutData(gd);

			exclusionsDefaultButton = createPushButton(exclusionsGroup, "Set Default", null);
			exclusionsDefaultButton.addSelectionListener(new SelectionAdapter() {
				public void widgetSelected(SelectionEvent evt) {
					exclusionsInvertButton.setSelection(LaunchConfig.EXCLUSIONS_INVERT_DEFAULT);
					exclusions.setText(LaunchConfig.EXCLUSIONS_DEFAULT);
				}
			});

			exclusionsClearButton = createPushButton(exclusionsGroup, "Clear", null);
			exclusionsClearButton.addSelectionListener(new SelectionAdapter() {
				public void widgetSelected(SelectionEvent evt) {
					exclusions.setText("");
				}
			});

			exclusionsInvertButton = createCheckButton(exclusionsGroup, "Invert selection");
			exclusionsInvertButton.addSelectionListener(new SelectionAdapter() {
				public void widgetSelected(SelectionEvent evt) {
					config.invertExclusion = exclusionsInvertButton.getSelection();
				}
			});

			// filler to reserve space for the exclusions text box
			Label l = new Label(exclusionsGroup, SWT.SHADOW_NONE);
			l.setText("  ");
		}


		/* Group Options */
		{
			Group optionsGroup = new Group(comp, SWT.NONE);
			GridLayout optionsLayout = new GridLayout();
			optionsLayout.numColumns = 4;
			optionsGroup.setLayout(optionsLayout);
			gd = new GridData(GridData.FILL_HORIZONTAL);
			gd.horizontalSpan = 4;
			gd.verticalSpan = 1;
			optionsGroup.setLayoutData(gd);
			optionsGroup.setText("Analysis options");
			optionsGroup.setFont(comp.getFont());

			computeSummaryButton = createCheckButton(optionsGroup, "Compute summary edges");
			//simpleDataDepButton = createCheckButton(optionsGroup, "Simple data dependencies");
			computeSummaryButton.addSelectionListener(new SelectionAdapter() {
				public void widgetSelected(SelectionEvent evt) {
					config.computeSummaryEdges = computeSummaryButton.getSelection();
				}
			});

			useSummaryInline = createCheckButton(optionsGroup, "Summary optimization (inlining)");
			//simpleDataDepButton = createCheckButton(optionsGroup, "Simple data dependencies");
			useSummaryInline.addSelectionListener(new SelectionAdapter() {
				public void widgetSelected(SelectionEvent evt) {
					config.useSummaryOpt = useSummaryInline.getSelection();
				}
			});

			ignoreExceptionsButton = createCheckButton(optionsGroup, "Ignore exceptions");
			ignoreExceptionsButton.addSelectionListener(new SelectionAdapter() {
				public void widgetSelected(SelectionEvent evt) {
					config.ignoreExceptions = ignoreExceptionsButton.getSelection();
				}
			});

			optimizeExceptionsButton = createCheckButton(optionsGroup, "Optimize exceptions");
			optimizeExceptionsButton.addSelectionListener(new SelectionAdapter() {
				public void widgetSelected(SelectionEvent evt) {
					config.optimizeExceptions = optimizeExceptionsButton.getSelection();
				}
			});

			addControlFlowButton = createCheckButton(optionsGroup, "Add control flow to SDG");
			addControlFlowButton.addSelectionListener(new SelectionAdapter() {
				public void widgetSelected(SelectionEvent evt) {
					config.addControlFlow = addControlFlowButton.getSelection();
				}
			});

			computeInterferenceButton = createCheckButton(optionsGroup, "Compute interference");
			computeInterferenceButton.addSelectionListener(new SelectionAdapter() {
				public void widgetSelected(SelectionEvent evt) {
					config.computeInterference = computeInterferenceButton.getSelection();

					if (!computeInterferenceButton.getSelection()) {
						noClinits.setEnabled(false);
						useEscape.setEnabled(false);
					} else {
						noClinits.setEnabled(true);
						useEscape.setEnabled(true);
					}
				}
			});

			noClinits = createCheckButton(optionsGroup, "Ignore static initializers (interference)");
			noClinits.addSelectionListener(new SelectionAdapter() {
				public void widgetSelected(SelectionEvent evt) {
					config.interferenceNoClinits = noClinits.getSelection();
				}
			});

			useEscape = createCheckButton(optionsGroup, "Use escape analysis (interference)");
			useEscape.addSelectionListener(new SelectionAdapter() {
				public void widgetSelected(SelectionEvent evt) {
					config.interferenceUseEscape = useEscape.getSelection();
				}
			});

			nonTermination = createCheckButton(optionsGroup, "Non-termination sensitive");
			nonTermination.addSelectionListener(new SelectionAdapter() {
				public void widgetSelected(SelectionEvent evt) {
					config.nonTermination = nonTermination.getSelection();
				}
			});

			useJoanaCompiler = createCheckButton(optionsGroup, "Use joana compiler");
			useJoanaCompiler.addSelectionListener(new SelectionAdapter() {
				public void widgetSelected(SelectionEvent evt) {
					config.useJoanaCompiler = useJoanaCompiler.getSelection();
				}
			});
		}

		/* Ok, Cancel Group*/
		{
			Composite confirmGroup = new Composite(comp, SWT.NONE);
			GridLayout confirmLayout = new GridLayout();
			confirmLayout.numColumns = 4;
			confirmGroup.setLayout(confirmLayout);
			gd = new GridData(GridData.FILL_HORIZONTAL);
			gd.horizontalSpan = 4;
			gd.verticalSpan = 1;
			confirmGroup.setLayoutData(gd);
			confirmGroup.setFont(comp.getFont());

			cancel = createPushButton(confirmGroup, "Cancel", null);
			cancel.addSelectionListener(new SelectionAdapter() {
				public void widgetSelected(SelectionEvent evt) {
					close();
				}
			});

			load = createPushButton(confirmGroup, "Load Config", null);
			load.addSelectionListener(new SelectionAdapter() {
				public void widgetSelected(SelectionEvent evt) {
					try {
						loadConfig();
					} catch (IOException e) {
						edu.kit.joana.deprecated.jsdg.gui.Activator.getDefault().showError(e, "Exception while loading configuration");
					}
				}
			});

			save = createPushButton(confirmGroup, "Save Config", null);
			save.addSelectionListener(new SelectionAdapter() {
				public void widgetSelected(SelectionEvent evt) {
					try {
						updateConfiguration();
						saveConfig();
					} catch (FileNotFoundException e) {
						edu.kit.joana.deprecated.jsdg.gui.Activator.getDefault().showError(e, "Exception while saving configuration");
					}
				}
			});

			create = createPushButton(confirmGroup, "Create SDG", null);
			create.addSelectionListener(new SelectionAdapter() {
				public void widgetSelected(SelectionEvent evt) {
					try {
						updateConfiguration();
						close();
						saveDefaultConfig();
						runCreateSDG();
					} catch (InvocationTargetException e) {
						edu.kit.joana.deprecated.jsdg.gui.Activator.getDefault().showError(e, "Exception while invoking SDGFactory.getSDG");
					} catch (InterruptedException e) {
						edu.kit.joana.deprecated.jsdg.gui.Activator.getDefault().showError(e, "InterruptedException while invoking SDGFactory.getSDG");
					} catch (FileNotFoundException e) {
						edu.kit.joana.deprecated.jsdg.gui.Activator.getDefault().showError(e, "FileNotFoundException while saving default configuration");
					}
				}
			});
		}

		this.layout();
	}

	private void loadConfig() throws IOException {
		FileDialog dialog = new FileDialog(getShell(), SWT.OPEN);
		dialog.setText("Choose file to load jSDG configuration from");
		dialog.setFilterExtensions(new String[] {"cfg"});
		dialog.setFilterPath(defaultCfg.toFile().getParent());
		String file = dialog.open();
		if (file != null) {
			File cfgFile = new File(file);
			FileInputStream fIn = new FileInputStream(cfgFile);
			SDGFactory.Config cfg = SDGFactory.Config.readFrom(fIn);
			initConfig(cfg);
		}
	}

	private void saveConfig() throws FileNotFoundException {
		updateConfiguration();

		FileDialog dialog = new FileDialog(getShell(), SWT.SAVE);
		dialog.setText("Choose file to save jSDG configuration to");
		//dialog.setFilterExtensions(new String[] {"cfg"});
		dialog.setFilterPath(defaultCfg.toFile().getParent());
		String file = dialog.open();
		if (file != null) {
			File cfgFile = new File(file);
			saveConfigFile(cfgFile);
		}
	}

	public void initConfig(SDGFactory.Config cfg) {
		config = cfg;

		for (int i= 0; i < pointsToType.getItemCount(); i++) {
			String item = pointsToType.getItem(i);
			if (item.equals(cfg.pointsTo.toString())) {
				pointsToType.select(i);
				break;
			}
		}

		for (int i= 0; i < objTreeType.getItemCount(); i++) {
			String item = objTreeType.getItem(i);
			if (item.equals(cfg.objTree.toString())) {
				objTreeType.select(i);
				break;
			}
		}

		for (int i = 0; i < stubsJar.getItemCount(); i++) {
			String item = stubsJar.getItem(i);
			Stubs stub = Stubs.valueOf(item);
			IPath path = SDGConfigBuilder.getStubPath(javafile, stub);
			if (containsPath(config.scopeData, path)) {
				stubsJar.select(i);
				break;
			}
		}

		useStdLibrary.setSelection(config.scopeData.contains("Primordial,Java,stdlib,none"));

		libraries.setText(getIncludedLibsTxt());

		exclusions.setText(getExclusionsTxt());
		exclusionsInvertButton.setSelection(cfg.invertExclusion);

		computeSummaryButton.setSelection(cfg.computeSummaryEdges);
		useSummaryInline.setSelection(cfg.useSummaryOpt);
		ignoreExceptionsButton.setSelection(cfg.ignoreExceptions);
		optimizeExceptionsButton.setSelection(cfg.optimizeExceptions);
		addControlFlowButton.setSelection(cfg.addControlFlow);
		computeInterferenceButton.setSelection(cfg.computeInterference);

		noClinits.setSelection(cfg.interferenceNoClinits);
		useEscape.setSelection(cfg.interferenceUseEscape);
		nonTermination.setSelection(cfg.nonTermination);
		useJoanaCompiler.setSelection(cfg.useJoanaCompiler);

		if (!computeInterferenceButton.getSelection()) {
			noClinits.setEnabled(false);
			useEscape.setEnabled(false);
		}
	}

	private boolean containsPath(List<String> scopeData, IPath path) {
		for (String str : scopeData) {
			if (str.contains(path.toOSString())) {
				return true;
			}
		}

		return false;
	}

	private String getIncludedLibsTxt() {
		String result = "";

		if (config.scopeData != null) {
			for (String lib : config.scopeData) {
				if (!isStub(lib) && !isPrimordial(lib)) {
					lib = lib.substring(lib.lastIndexOf(',') + 1);
					if (lib.startsWith(workspaceRoot)) {
						lib = lib.substring(workspaceRoot.length());
					}
					result += lib + LaunchConfig.NEW_LINE;
				}
			}
		}

		return result;
	}

	private final boolean isStub(String str) {
		boolean result = false;

		for (Stubs stub : SDGConfigBuilder.Stubs.values()) {
			if (str.contains(stub.getFile())) {
				result = true;
				break;
			}
		}

		return result;
	}

	private final boolean isPrimordial(String str) {
		return str.contains("Primordial") && str.contains("primordial.jar");
	}

	private String getExclusionsTxt() {
		String result = "";

		if (config.exclusions != null) {
			for (String excl : config.exclusions) {
				result += excl + LaunchConfig.NEW_LINE;
			}
		}

		return result;
	}

	public List<String> createExclusionList(String data) {
		List<String> result = new ArrayList<String>();

		StringTokenizer strTok = new StringTokenizer(data, LaunchConfig.NEW_LINE + "\n ;,\r\n");
		while (strTok.hasMoreTokens()) {
			result.add(strTok.nextToken());
		}

		return result;
	}


	private void updateConfiguration() {
		{
			int index = pointsToType.getSelectionIndex();
			String item = pointsToType.getItem(index);
			for (PointsToType pts : PointsToType.values()) {
				if (item.equals(pts.toString())) {
					config.pointsTo = pts;
					break;
				}
			}
		}

		{
			int index = objTreeType.getSelectionIndex();
			String item = objTreeType.getItem(index);

			for (ObjTreeType type : ObjTreeType.values()) {
				if (item.equals(type.toString())) {
					config.objTree = type;
					break;
				}
			}
		}

		{
			boolean useStdLib = useStdLibrary.getSelection();
			int index = stubsJar.getSelectionIndex();
			String item = stubsJar.getItem(index);

			for (Stubs stub : Stubs.values()) {
				if (item.equals(stub.toString())) {
					selectStubPath(stub, useStdLib);
					break;
				}
			}
		}

		{
			String libs = libraries.getText();
			if (libs != null) {
				libs = libs.trim();
				if (libs.length() > 0) {
					addToStubPath(libs);
				}
			}
		}

		{
			String excl = exclusions.getText();
			if (excl != null) {
				excl = excl.trim();
				if (excl.length() > 0) {
					List<String> exclList = createExclusionList(excl);
					config.exclusions = exclList;
				} else {
					config.exclusions = null;
				}
			} else {
				config.exclusions = null;
			}
		}
	}


	private void selectStubPath(Stubs stub, boolean includeStdLib) {
		IPath path = SDGConfigBuilder.getStubPath(javafile, stub);

		ArrayList<String> newScope = new ArrayList<String>();

		Set<String> removePaths = HashSetFactory.make();
		removePaths.add("Primordial,Java,stdlib,none");
		for (Stubs s : Stubs.values()) {
			IPath sPath = SDGConfigBuilder.getStubPath(javafile, s);
			removePaths.add("Primordial,Java,jarFile," + sPath.toOSString());
		}

		newScope.add("Primordial,Java,jarFile," + path.toOSString());

		for (String scope : config.scopeData) {
			if (!removePaths.contains(scope)) {
				newScope.add(scope);
			} else {
//					System.out.println("removing " + scope);
			}
		}

		if (includeStdLib) {
			newScope.add("Primordial,Java,stdlib,none");
		}

		config.scopeData = newScope;
	}

	private void addToStubPath(String libs) {
		StringTokenizer sTok = new StringTokenizer(libs, LaunchConfig.NEW_LINE);
		while (sTok.hasMoreTokens()) {
			String lib = sTok.nextToken();
			lib = "Primordial,Java,jarFile," + workspaceRoot + lib;
			if (!config.scopeData.contains(lib)) {
				config.scopeData.add(lib);
			}
		}
	}

	private void saveDefaultConfig() throws FileNotFoundException {
		File cfgFile = defaultCfg.toFile();
		File parent = cfgFile.getParentFile();
		if (!parent.exists()) {
			parent.mkdirs();
		}

		saveConfigFile(cfgFile);
	}

	private void saveConfigFile(File cfgFile) throws FileNotFoundException {
		FileOutputStream fOut = new FileOutputStream(cfgFile);
		PrintWriter pw = new PrintWriter(fOut);
		pw.print(config.toString());
		pw.close();
	}

	private final IProject findCurrentProject() {
		IProject project = null;
		String relativeBuild = config.classpath.substring(workspaceRoot.length());
		IResource res = ResourcesPlugin.getWorkspace().getRoot().findMember(relativeBuild);

		if (res != null) {
			project = res.getProject();
		}

		return project;
	}

	private void runCreateSDG() throws InvocationTargetException, InterruptedException {
		if (config.useJoanaCompiler) {
			String oldCp = config.classpath;
			try {
				IProject project = findCurrentProject();
				String newBuildPath = project.getWorkingLocation("joana.ifc.compiler") + File.separator + "build";
				config.classpath = newBuildPath;
			} catch (Exception e) {
				Log.warn("No joana compiled binaries found -> fallback to normal class files.");
				Log.warn(e);
				config.classpath = oldCp;
			}
		}

		PlatformUI.getWorkbench().getProgressService().
		//PlatformUI.getWorkbench().getActiveWorkbenchWindow().
		run(true, true, new IRunnableWithProgress() {

			public void run(IProgressMonitor monitor)
					throws InvocationTargetException, InterruptedException {
				SDGFactory fact = Activator.getDefault().getFactory();
				try {
					ProgressMonitorDelegate delegate = ProgressMonitorDelegate.createProgressMonitorDelegate(monitor);
					edu.kit.joana.ifc.sdg.graph.SDG sdg = fact.getJoanaSDG(config, delegate);
					sdg.setJoanaCompiler(config.useJoanaCompiler);

//					SDGStatistics stats = new SDGStatistics();
//					stats.buildStatistics(sdg);
//					Log.info(stats.toString());
//
//					if (config.outputSDGfile != null) {
//						String fileName = config.outputSDGfile + ".stats";
//						monitor.beginTask("Writing statistics to file " + fileName, -1);
//						SDGStatistics.writeTo(stats, fileName);
//						monitor.done();
//					}

					if (config.outputSDGfile != null) {
//						String fileName = config.outputSDGfile;
//						edu.kit.joana.ifc.sdg.graph.SDG joana = JoanaStyleSDG.createJoanaSDG(sdg, config.addControlFlow, monitor);

//						/* Postprocessing */
//						monitor.beginTask("Creating cSDG from SDG " + fileName, -1);
//						Log.info("Creating cSDG from SDG " + fileName);
//						monitor.subTask("Fixing WALA control flow");
//						joana = Hack.hack(joana); // FIXME: make this hack obsolete!
//                        monitor.done();
//
//						if (config.computeSummaryEdges) {
//						    monitor.subTask("Compute Summary Edges");
//						    SummaryEdgeComputation sum = new SummaryEdgeComputation();
//						    sum.computeSummaryEdges(joana);
//	                        monitor.done();
//						}
//                        monitor.done();

						monitor.beginTask("Saving SDG to " + config.outputSDGfile, -1);

						BufferedOutputStream bOut = new BufferedOutputStream(
								new FileOutputStream(config.outputSDGfile));
						SDGSerializer.toPDGFormat(sdg, bOut);

						monitor.done();

						//TODO mod-ref maps output optional and not dependent on sdg type

//	                    /* FRICKEL */
//	                    monitor.beginTask("Begin gefrickel", -1);
//	                    Map<Integer, Set<Integer>>[] maps =
//	                    	ModRefOutput.createModRefMaps(fact.getRawSDG(), monitor, JoanaStyleSDG.nodeMap);
//						ModRefOutput.saveMaps(config.outputSDGfile, maps[0], maps[1], maps[2], maps[3], maps[4]);
//	                    System.out.println(maps[0]);
//	                    System.out.println(maps[1]);
//	                    System.out.println(maps[2]);
//                        System.out.println(maps[0].size());
//                        System.out.println(maps[1].size());
//                        System.out.println(maps[2].size());

                        monitor.done();
					}

					PlatformUI.getWorkbench().getDisplay().asyncExec(new Runnable() {

						public void run() {
							Shell activeShell = PlatformUI.getWorkbench().getDisplay().getActiveShell();
							//edu.kit.joana.deprecated.jsdg.gui.Activator.getDefault().getWorkbench().getDisplay().getActiveShell();
							MessageDialog.openInformation(activeShell, "jSDG: System Dependency Graph created",
								"The System Dependency Graph has been successfully created in:\n " + config.outputSDGfile);
						}
					});

					edu.kit.joana.deprecated.jsdg.gui.Activator.getDefault().sdgChanged(config.outputSDGfile);

				} catch (InvalidClassFileException e) {
					throw new InvocationTargetException(e);
				} catch (IllegalArgumentException e) {
					throw new InvocationTargetException(e);
				} catch (CancelException e) {
					throw new InvocationTargetException(e);
				} catch (PDGFormatException e) {
					throw new InvocationTargetException(e);
				} catch (IOException e) {
					throw new InvocationTargetException(e);
				} catch (WalaException e) {
					throw new InvocationTargetException(e);
				}
			}});
	}

	private void close() {
		this.getShell().close();
	}

	private void handleAddLibraryButtonSelected() {
		final IProject ip = javafile.getJavaProject().getProject();

		/*
		 * create and open dialog
		 */
		FilteredItemsSelectionDialog mtsd = new JarSelectionDialog(ip, getShell(), true);
		mtsd.open();

		/*
		 * get and evaluate result from dialog
		 */
		Object[] result = mtsd.getResult();
		if (result == null) return;

		for (Object obj : result) {
			JarFile jarFile = (JarFile) obj;
//			System.err.println("Selected " + jarFile);
			String libs = libraries.getText();
			if (!libs.contains(jarFile.toString())) {
				libraries.append(jarFile + LaunchConfig.NEW_LINE);
				addToStubPath(jarFile.toString());
			}
		}
	}


	/**
	 * Creates and returns a new push button with the given
	 * label and/or image.
	 *
	 * @param parent parent control
	 * @param label button label or <code>null</code>
	 * @param image image of <code>null</code>
	 *
	 * @return a new push button
	 */
	protected Button createPushButton(Composite parent, String label, Image image) {
		return SWTFactory.createPushButton(parent, label, image);
	}

	/**
	 * Creates and returns a new radio button with the given
	 * label and/or image.
	 *
	 * @param parent parent control
	 * @param label button label or <code>null</code>
	 *
	 * @return a new radio button
	 */
	protected Button createRadioButton(Composite parent, String label) {
		return SWTFactory.createRadioButton(parent, label);
	}

	/**
	 * Creates and returns a new check button with the given
	 * label.
	 *
	 * @param parent the parent composite
	 * @param label the button label
	 * @return a new check button
	 * @since 3.0
	 */
	protected Button createCheckButton(Composite parent, String label) {
		Button button = new Button(parent, SWT.CHECK);
		button.setText(label);
		GridData data = new GridData();
		button.setLayoutData(data);
		button.setFont(parent.getFont());
		SWTFactory.setButtonDimensionHint(button);
		return button;
	}

}
