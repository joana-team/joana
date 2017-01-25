/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.ui.ifc.sdg.gui.launching;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.ui.ILaunchConfigurationTab;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.dialogs.ElementListSelectionDialog;
import org.eclipse.ui.ide.IDE;

import edu.kit.joana.ifc.sdg.core.violations.ClassifiedViolation;
import edu.kit.joana.ui.ifc.sdg.gui.NJSecPlugin;
import edu.kit.joana.ui.ifc.sdg.gui.marker.MarkerManager;

public class ConfigurationMainTab extends AbstractJoanaTab {

	/**
	 * Modify listener that simply updates the owning launch configuration dialog.
	 */
	private ModifyListener fBasicModifyListener = new ModifyListener() {
			public void modifyText(ModifyEvent evt) {
				updateLaunchConfigurationDialog();
			}
	};

	private Button pdgLocationButton;
	private Text pdgLocationText;
	private Button pdgRegenNev;
	private Text latticeText;
	private Text projectText;
	private Button standardConf;
    private Button useOriginalLines;
    private Button useJoanaCompiler;
    private Text classText;

	public void createControl(Composite parent) {
		Composite comp = new Composite(parent, SWT.NONE);
		setControl(comp);
		GridLayout topLayout = new GridLayout(1, true);
		topLayout.horizontalSpacing = 10;
		comp.setLayout(topLayout);
		comp.setFont(parent.getFont());

		GridData gd;

		/* Group Project */
		Group projectGroup = new Group(comp, SWT.NONE);
		GridLayout projectLayout = new GridLayout();
		projectLayout.numColumns = 3;
		projectGroup.setLayout(projectLayout);
		gd = new GridData(GridData.FILL_HORIZONTAL);
		gd.horizontalSpan = 1;
		projectGroup.setLayoutData(gd);
		projectGroup.setText("Project:");
		projectGroup.setFont(comp.getFont());

		projectText = new Text(projectGroup, SWT.SINGLE | SWT.BORDER);
		gd = new GridData(GridData.FILL_HORIZONTAL);
		gd.horizontalSpan = 2;
		projectText.setLayoutData(gd);
		projectText.addModifyListener(fBasicModifyListener);

		Button projectButton = createPushButton(projectGroup, "Browse...", null);
		projectButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent evt) {
				handleProjectButtonSelected();
			}
		});

		standardConf = new Button(projectGroup,SWT.CHECK);
		standardConf.setText("Standard Configuration for this Project (this configuration is used for Assignement)");
		gd = new GridData();
		gd.horizontalSpan = 3;
		standardConf.setLayoutData(gd);
		standardConf.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent evt) {
				updateLaunchConfigurationDialog();
			}
		});

		/* Group Class */
		Group classGroup = new Group(comp, SWT.NONE);
		GridLayout classLayout = new GridLayout();
		classLayout.numColumns = 3;
		classGroup.setLayout(classLayout);
		gd = new GridData(GridData.FILL_HORIZONTAL);
		gd.horizontalSpan = 1;
		classGroup.setLayoutData(gd);
		classGroup.setText("Hauptklasse:");
		classGroup.setFont(comp.getFont());

		classText = new Text(classGroup, SWT.SINGLE | SWT.BORDER);
		gd = new GridData(GridData.FILL_HORIZONTAL);
		gd.horizontalSpan = 2;
		classText.setLayoutData(gd);
		classText.addModifyListener(fBasicModifyListener);

		Button classButton = createPushButton(classGroup, "Browse...", null);
		classButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent evt) {
				String[] exts = {"*.java"};
				handleBrowseFileButtonSelected(classText, exts);
			}
		});

		/* Group Lattice */
		Group latticegroup = new Group(comp, SWT.NONE);
		GridLayout latticelayout = new GridLayout();
		latticelayout.numColumns = 3;
		latticegroup.setLayout(latticelayout);
		gd = new GridData(GridData.FILL_HORIZONTAL);
		gd.horizontalSpan = 1;
		latticegroup.setLayoutData(gd);
		latticegroup.setText("Security Class Lattice:");
		latticegroup.setFont(comp.getFont());

		latticeText = new Text(latticegroup, SWT.SINGLE | SWT.BORDER);
		gd = new GridData(GridData.FILL_HORIZONTAL);
		gd.horizontalSpan = 2;
		latticeText.setLayoutData(gd);
		latticeText.addModifyListener(fBasicModifyListener);

		Button latticeButton = createPushButton(latticegroup, "Browse...", null);
		latticeButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent evt) {
				String[] exts = {"*.lat"};
				handleBrowseFileButtonSelected(latticeText, exts);
			}
		});

		/* Group SDG Generation / Selection */
		Group group = new Group(comp, SWT.NONE);
		GridLayout layout = new GridLayout();
		layout.numColumns = 3;
		group.setLayout(layout);
		gd = new GridData(GridData.FILL_HORIZONTAL);
		gd.horizontalSpan = 1;
		group.setLayoutData(gd);
		group.setText("SDG Handling");
		group.setFont(comp.getFont());

		pdgRegenNev = new Button(group, SWT.RADIO);
        pdgRegenNev.setSelection(true);
		pdgRegenNev.setText("Use SDG:");
		gd = new GridData();
		gd.horizontalSpan = 1;
		pdgRegenNev.setLayoutData(gd);
		pdgRegenNev.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent evt) {
				handleuseGivenPdgButtonSelected();
			}
		});

		pdgLocationText = new Text(group, SWT.SINGLE | SWT.BORDER);
		gd = new GridData(GridData.FILL_HORIZONTAL);
		gd.horizontalSpan = 1;
		pdgLocationText.setLayoutData(gd);
		pdgLocationText.addModifyListener(fBasicModifyListener);
		pdgLocationText.setEnabled(false);

		pdgLocationButton = createPushButton(group, "Browse...", null);
		pdgLocationButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent evt) {
				String[] exts = {"*.pdg", "*.sdg"};
				handleBrowseFileButtonSelected(pdgLocationText, exts);
			}
		});
		pdgLocationButton.setEnabled(false);

        /* Group Compiler */
        Group othergroup = new Group(comp, SWT.NONE);
        GridLayout otherlayout = new GridLayout();
        otherlayout.numColumns = 1;
        othergroup.setLayout(otherlayout);
        gd = new GridData(GridData.FILL_HORIZONTAL);
        gd.horizontalSpan = 1;
        othergroup.setLayoutData(gd);
        othergroup.setText("Source-Code Mapping");
        othergroup.setFont(comp.getFont());

        useOriginalLines = new Button(othergroup,SWT.RADIO);
        useOriginalLines.setText("Use standard Java compiler (imprecise source-code mapping)");
        useOriginalLines.setToolTipText("To gain more precision, erase empty lines between method header(s) and first line(s)");
        gd = new GridData();
        gd.horizontalSpan = 1;
        useOriginalLines.setLayoutData(gd);
        useOriginalLines.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent evt) {
                updateLaunchConfigurationDialog();
            }
        });

        useJoanaCompiler = new Button(othergroup,SWT.RADIO);
        useJoanaCompiler.setText("Use modified Java compiler");
        useJoanaCompiler.setToolTipText("This currently works only for Java < 1.6.");
        gd = new GridData();
        gd.horizontalSpan = 1;
        useJoanaCompiler.setLayoutData(gd);
        useJoanaCompiler.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent evt) {
                updateLaunchConfigurationDialog();
            }
        });
	}

	private void handleProjectButtonSelected() {
		ILabelProvider lp = new ProjectSelectionDialogLabelProvider();

		ElementListSelectionDialog elsd = new ElementListSelectionDialog (getShell(), lp);
		elsd.setElements(this.getWorkspaceRoot().getProjects());
		elsd.setMultipleSelection(false);
		elsd.open();

		if (elsd.getFirstResult() == null) return;

		IProject result = (IProject)elsd.getFirstResult();

		this.projectText.setText(result.getName());

		try {
			if (LaunchConfigurationTools.getAllNJSecLaunchConfiguration(result).length == 0) {
				standardConf.setSelection(true);
			}
		} catch (CoreException e) {
			NJSecPlugin.singleton().showError("Problem while setting Standard Configuration", null, e);
		}
	}

	private void handleuseGivenPdgButtonSelected() {
		setUseGivenPdgEnabled(pdgRegenNev.getSelection());
		updateLaunchConfigurationDialog();
	}

	private void setUseGivenPdgEnabled(boolean enable) {
		pdgLocationText.setEnabled(enable);
		pdgLocationButton.setEnabled(enable);
	}

	/***
	 * Show FileSelection Dialog.
	 * Only files with extension in paramater extensions are shown
	 * the selected files complete path gets written with target.setText();
	 * @param target
	 * @param extensions
	 */
	protected void handleBrowseFileButtonSelected(Text target, String[] extensions) {
		FileDialog dialog = new FileDialog(getShell());
		dialog.setFilterExtensions(extensions);

		IResource ir = this.getWorkspaceRoot().findMember(this.projectText.getText());

		String currentContainerString = target.getText();
		Path containerPath = new Path(currentContainerString );
		if (containerPath.toFile().exists()) {
			dialog.setFilterPath(containerPath.toOSString());
		} else if (ir != null){
			IProject ip = (IProject) ir;
			String ipp = ip.getLocation().toOSString();
			dialog.setFilterPath(ipp);
		}

		dialog.open();
		String path = !dialog.getFilterPath().equals("") ? dialog.getFilterPath() + System.getProperty("file.separator") : null;
		String result = path + dialog.getFileName();

		if (!dialog.getFileName().equals("")) {
			target.setText(result);
		}
	}

	/**
	 * Convenience method for getting the workspace root.
	 */
	private IWorkspaceRoot getWorkspaceRoot() {
		return ResourcesPlugin.getWorkspace().getRoot();
	}

	public void setDefaults(ILaunchConfigurationWorkingCopy configuration) {
		configuration.setContainer(null);
		configuration.setAttribute(ConfigurationAttributes.SDG_LOCATION, "");
		configuration.setAttribute(ConfigurationAttributes.LATTICE_LOCATION, "");
		configuration.setAttribute(ConfigurationAttributes.PROJECT_NAME, "");
		configuration.setAttribute("MAIN_CLASS", new LinkedList<String>());  // TODO: find out if this is ever used
		configuration.setAttribute(ConfigurationAttributes.SDG_REB_ALW, true);
		configuration.setAttribute(ConfigurationAttributes.SDG_REB_NEC, false);
		configuration.setAttribute(ConfigurationAttributes.SDG_REB_NEV, false);
		configuration.setAttribute(ConfigurationAttributes.IS_PROJECT_STANDARD, false);
        configuration.setAttribute(ConfigurationAttributes.USE_JOANA_COMPILER, true);
	}

	public void initializeFrom(ILaunchConfiguration configuration) {
		ConfigReader cr = new ConfigReader(configuration);

		try {
			this.pdgLocationText.setText(cr.getSDGLocation());
			this.latticeText.setText(cr.getLatticeLocation());
			this.projectText.setText(cr.getProjectName());
			this.standardConf.setSelection(cr.isStandardConfiguration());
			this.classText.setText(cr.getMainClassName());

			setUseGivenPdgEnabled(true);

            this.useJoanaCompiler.setSelection(cr.getUseJoanaCompiler());
            this.useOriginalLines.setSelection(!cr.getUseJoanaCompiler());

		} catch (CoreException e) {
			NJSecPlugin.singleton().showError("Problem while reading configuration-attributes", null, e);
		}
	}

	@SuppressWarnings("unchecked")
	public void performApply(ILaunchConfigurationWorkingCopy configuration) {
		configuration.setAttribute(ConfigurationAttributes.PROJECT_NAME, this.projectText.getText());
		configuration.setAttribute(ConfigurationAttributes.MAIN_CLASS_NAME, this.classText.getText());
		configuration.setAttribute(ConfigurationAttributes.LATTICE_LOCATION, this.latticeText.getText());
		configuration.setAttribute(ConfigurationAttributes.SDG_LOCATION, this.pdgLocationText.getText());
        configuration.setAttribute(ConfigurationAttributes.USE_JOANA_COMPILER, this.useJoanaCompiler.getSelection());

		if (this.standardConf.getSelection()) {
			ConfigReader cr = new ConfigReader(configuration.getOriginal());
			try {
				LaunchConfigurationTools.setAllNJSecLaunchersToNonStandard(cr.getProject());
				List<String> marker = configuration.getAttribute(ConfigurationAttributes.MARKER, new ArrayList<String>());
				if (NJSecPlugin.singleton().getActivePage().getActiveEditor() == null) {
					IPath path = Path.fromPortableString(this.classText.getText().replace("\\", "/"));
					IFile file = ResourcesPlugin.getWorkspace().getRoot().getFileForLocation(path);
					IDE.openEditor(NJSecPlugin.singleton().getActivePage(), file);
				}
				if (NJSecPlugin.singleton().getActivePage().getActiveEditor() != null) {
					IEditorInput iei = NJSecPlugin.singleton().getActivePage().getActiveEditor().getEditorInput();
					IResource resource = (IResource) ((IAdaptable) iei).getAdapter(IResource.class);
					IProject project = resource.getProject();

					MarkerManager.singleton().changeProject(project, marker);
					//Violations reset
					NJSecPlugin.singleton().getSDGFactory().violationsChanged(project, new ArrayList<ClassifiedViolation>());
				}

			} catch (CoreException e) {
				NJSecPlugin.singleton().showError("Problem while setting configuration-attributes", null, e);
			}
		}

		configuration.setAttribute(ConfigurationAttributes.IS_PROJECT_STANDARD, this.standardConf.getSelection());
	}

	/**
	 * @see ILaunchConfigurationTab#getName()
	 */
	public String getName() {
		return "Joana IFC Configuration";
	}
}
