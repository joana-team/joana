/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.deprecated.jsdg.gui.launch;

import java.io.File;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.ui.AbstractLaunchConfigurationTab;
import org.eclipse.debug.ui.ILaunchConfigurationTab;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaModel;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.search.IJavaSearchScope;
import org.eclipse.jdt.core.search.SearchEngine;
import org.eclipse.jdt.ui.IJavaElementSearchConstants;
import org.eclipse.jdt.ui.JavaUI;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
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
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.dialogs.ElementListSelectionDialog;
import org.eclipse.ui.dialogs.SelectionDialog;

import edu.kit.joana.deprecated.jsdg.SDGFactory;
import edu.kit.joana.deprecated.jsdg.gui.Activator;
import edu.kit.joana.deprecated.jsdg.util.Log.LogLevel;

public class ConfigurationMainTab extends AbstractLaunchConfigurationTab {

	private Text mainClass;
	private Button mainClassButton;
	private Text projectName;
	private Button projectButton;
	private Combo pointsToType;
	private Button pointsToTypeDefaultButton;
	private Combo logLevel;
	private Button logLevelDefaultButton;
	private Text exclusions;
	private Button exclusionsDefaultButton;
	private Button exclusionsInvertButton;
	private Text scopeFileData;
	private Button scopeFileDataDefaultButton;
	private Text logfileName;
	private Button logfileButton;
	private Button simpleDataDepButton;
	private Button ignoreExceptionsButton;
	private Button addControlFlowButton;
	private Button nonTerminationButton;
	private Button computeInterferenceButton;


	/**
	 * Modify listener that simply updates the owning launch configuration dialog.
	 */
	private ModifyListener fBasicModifyListener = new ModifyListener() {
			public void modifyText(ModifyEvent evt) {
				updateLaunchConfigurationDialog();
			}
	};

    /**
     * Updates the buttons and message in this page's launch
     * configuration dialog.
     */
    protected void updateLaunchConfigurationDialog() {
        if (getLaunchConfigurationDialog() != null) {
            //order is important here due to the call to
            //refresh the tab viewer in updateButtons()
            //which ensures that the messages are up to date
            getLaunchConfigurationDialog().updateButtons();
            getLaunchConfigurationDialog().updateMessage();
        }
    }

    /**
     * @see ILaunchConfigurationTab#getImage()
     */
    public Image getImage() {
        return Activator.getDefault().getImageRegistry().get("joana");
    }

	public void createControl(Composite parent) {
		Composite comp = new Composite(parent, SWT.NONE);
		setControl(comp);
		GridLayout topLayout = new GridLayout(1, true);
		topLayout.horizontalSpacing = 10;
		comp.setLayout(topLayout);
		comp.setFont(parent.getFont());

		GridData gd;

		/* Group Project */
		{
			Group projectGroup = new Group(comp, SWT.NONE);
			GridLayout projectLayout = new GridLayout();
			projectLayout.numColumns = 3;
			projectGroup.setLayout(projectLayout);
			gd = new GridData(GridData.FILL_HORIZONTAL);
			gd.horizontalSpan = 1;
			projectGroup.setLayoutData(gd);
			projectGroup.setText("Project:");
			projectGroup.setFont(comp.getFont());

			projectName = new Text(projectGroup, SWT.SINGLE | SWT.BORDER);
			gd = new GridData(GridData.FILL_HORIZONTAL);
			gd.horizontalSpan = 2;
			projectName.setLayoutData(gd);
			projectName.addModifyListener(fBasicModifyListener);

			projectButton = createPushButton(projectGroup, "Browse...", null);
			projectButton.addSelectionListener(new SelectionAdapter() {
				public void widgetSelected(SelectionEvent evt) {
					handleProjectButtonSelected(projectName);
				}
			});
		}

		/* Group Main Class */
		{
			Group mainClassGroup = new Group(comp, SWT.NONE);
			GridLayout mainClassLayout = new GridLayout();
			mainClassLayout.numColumns = 3;
			mainClassGroup.setLayout(mainClassLayout);
			gd = new GridData(GridData.FILL_HORIZONTAL);
			gd.horizontalSpan = 2;
			gd.verticalSpan = 2;
			mainClassGroup.setLayoutData(gd);
			mainClassGroup.setText("Main class:");
			mainClassGroup.setFont(comp.getFont());

			mainClass = new Text(mainClassGroup, SWT.SINGLE | SWT.BORDER);
			gd = new GridData(GridData.FILL_HORIZONTAL);
			gd.horizontalSpan = 2;
			mainClass.setLayoutData(gd);
			mainClass.addModifyListener(fBasicModifyListener);

			mainClassButton = createPushButton(mainClassGroup, "Search...", null);
			mainClassButton.addSelectionListener(new SelectionAdapter() {
				public void widgetSelected(SelectionEvent evt) {
					handleMainClassButtonSelected();
				}
			});
		}

		/* Group Points-to type */
		{
			Group ptsGroup = new Group(comp, SWT.NONE);
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
			pointsToType.addModifyListener(fBasicModifyListener);

			pointsToTypeDefaultButton = createPushButton(ptsGroup, "Set Default", null);
			pointsToTypeDefaultButton.addSelectionListener(new SelectionAdapter() {
				public void widgetSelected(SelectionEvent evt) {
					handlePointsToTypeDefaultButtonSelected();
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
			exclusions.addModifyListener(fBasicModifyListener);

			exclusionsDefaultButton = createPushButton(exclusionsGroup, "Set Default", null);
			exclusionsDefaultButton.addSelectionListener(new SelectionAdapter() {
				public void widgetSelected(SelectionEvent evt) {
					handleExclusionsDefaultButtonSelected();
				}
			});

			exclusionsInvertButton = createCheckButton(exclusionsGroup, "Invert selection");
			exclusionsInvertButton.addSelectionListener(new SelectionAdapter() {
				public void widgetSelected(SelectionEvent evt) {
					handleExclusionsInvertButtonSelected();
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

			simpleDataDepButton = createCheckButton(optionsGroup, "Simple data dependencies");
			simpleDataDepButton.addSelectionListener(new SelectionAdapter() {
				public void widgetSelected(SelectionEvent evt) {
					handleSimpleDataDepButtonSelected();
				}
			});

			ignoreExceptionsButton = createCheckButton(optionsGroup, "Ignore excpetions");
			ignoreExceptionsButton.addSelectionListener(new SelectionAdapter() {
				public void widgetSelected(SelectionEvent evt) {
					handleIgnoreExceptionsButtonSelected();
				}
			});

			addControlFlowButton = createCheckButton(optionsGroup, "Add control flow to SDG");
			addControlFlowButton.addSelectionListener(new SelectionAdapter() {
				public void widgetSelected(SelectionEvent evt) {
					handleAddControlFlowButtonSelected();
				}
			});

			nonTerminationButton = createCheckButton(optionsGroup, "Detect non termination");
			nonTerminationButton.addSelectionListener(new SelectionAdapter() {
				public void widgetSelected(SelectionEvent evt) {
					handleNonTerminationButtonSelected();
				}
			});

			computeInterferenceButton = createCheckButton(optionsGroup, "Compute interference");
			computeInterferenceButton.addSelectionListener(new SelectionAdapter() {
				public void widgetSelected(SelectionEvent evt) {
					handleComputeInterferenceButtonSelected();
				}
			});
		}

		/* Group Scope File */
		{
			Group scopeFileGroup = new Group(comp, SWT.NONE);
			GridLayout scopeFileLayout = new GridLayout();
			scopeFileLayout.numColumns = 3;
			scopeFileGroup.setLayout(scopeFileLayout);
			gd = new GridData(GridData.FILL_HORIZONTAL);
			gd.horizontalSpan = 3;
			gd.verticalSpan = 3;
			scopeFileGroup.setLayoutData(gd);
			scopeFileGroup.setText("Set libraries to include:");
			scopeFileGroup.setFont(comp.getFont());

			scopeFileData = new Text(scopeFileGroup, SWT.MULTI | SWT.BORDER | SWT.V_SCROLL | SWT.H_SCROLL);
			gd = new GridData(GridData.FILL_BOTH);
			gd.horizontalSpan = 2;
			gd.verticalSpan = 3;
			scopeFileData.setLayoutData(gd);
			scopeFileData.addModifyListener(fBasicModifyListener);

			scopeFileDataDefaultButton = createPushButton(scopeFileGroup, "Set Default", null);
			scopeFileDataDefaultButton.addSelectionListener(new SelectionAdapter() {
				public void widgetSelected(SelectionEvent evt) {
					handleScopeFileDataDefaultButtonSelected();
				}
			});

			// filler to reserve space for the exclusions text box
			Label l = new Label(scopeFileGroup, SWT.SHADOW_NONE);
			l.setText("  ");
		}

		/* Group Logfile */
		{
			Group logfileGroup = new Group(comp, SWT.NONE);
			GridLayout logfileLayout = new GridLayout();
			logfileLayout.numColumns = 3;
			logfileGroup.setLayout(logfileLayout);
			gd = new GridData(GridData.FILL_HORIZONTAL);
			gd.horizontalSpan = 2;
			gd.verticalSpan = 2;
			logfileGroup.setLayoutData(gd);
			logfileGroup.setText("Log file:");
			logfileGroup.setFont(comp.getFont());

			logfileName = new Text(logfileGroup, SWT.SINGLE | SWT.BORDER);
			gd = new GridData(GridData.FILL_HORIZONTAL);
			gd.horizontalSpan = 2;
			logfileName.setLayoutData(gd);
			logfileName.addModifyListener(fBasicModifyListener);

			logfileButton = createPushButton(logfileGroup, "Choose Logfile", null);
			logfileButton.addSelectionListener(new SelectionAdapter() {
				public void widgetSelected(SelectionEvent evt) {
					handleLogfileButtonSelected();
				}
			});

			Label logLvllabel = new Label(logfileGroup, SWT.LEFT);
			logLvllabel.setText("Select log level: ");
			gd = new GridData(GridData.FILL_HORIZONTAL);
			gd.horizontalSpan = 1;
			logLvllabel.setLayoutData(gd);

			logLevel = new Combo(logfileGroup, SWT.READ_ONLY);
			for (LogLevel type : LogLevel.values()) {
				logLevel.add(type.toString());
			}
			gd = new GridData(GridData.FILL_HORIZONTAL);
			gd.horizontalSpan = 1;
			logLevel.setLayoutData(gd);
			logLevel.addModifyListener(fBasicModifyListener);

			logLevelDefaultButton = createPushButton(logfileGroup, "Set Default", null);
			logLevelDefaultButton.addSelectionListener(new SelectionAdapter() {
				public void widgetSelected(SelectionEvent evt) {
					handleLogLevelDefaultButtonSelected();
				}
			});
		}

	}

	public String getName() {
		return "jSDG Configuration";
	}

	public void initializeFrom(ILaunchConfiguration configuration) {
		LaunchConfigReader reader = new LaunchConfigReader(configuration);
		try {
			mainClass.setText(reader.getMainClassName());
			for (int i= 0; i < pointsToType.getItemCount(); i++) {
				String item = pointsToType.getItem(i);
				if (item.equals(reader.getPointsToType().toString())) {
					pointsToType.select(i);
					break;
				}
			}
			for (int i= 0; i < logLevel.getItemCount(); i++) {
				String item = logLevel.getItem(i);
				LogLevel lvl = reader.getLogLevel();
				if (lvl == null) {
					break;
				}

				if (item.equals(lvl.toString())) {
					logLevel.select(i);
					break;
				}
			}
			projectName.setText(reader.getProjectName());
			exclusions.setText(reader.getExclusionsTxt());
			exclusionsInvertButton.setSelection(reader.isInvertExclusion());
			simpleDataDepButton.setSelection(reader.isSimpleDataDependency());
			ignoreExceptionsButton.setSelection(reader.isIgnoreExceptions());
			addControlFlowButton.setSelection(reader.isAddControlFlow());
			nonTerminationButton.setSelection(reader.isNonTermination());
			computeInterferenceButton.setSelection(reader.isComputeInterference());
			scopeFileData.setText(reader.getScopeFileDataTxt());
			logfileName.setText(reader.getLogfile());
		} catch (CoreException e) {
			edu.kit.joana.deprecated.jsdg.gui.Activator.getDefault().showError(e, "Failed to read configuration");
		}
	}

	public void performApply(ILaunchConfigurationWorkingCopy conf) {
		try {
			LaunchConfigReader.setMainClassName(conf, mainClass.getText());
			LaunchConfigReader.setProjectName(conf, projectName.getText());
			LaunchConfigReader.setExclusionsTxt(conf, exclusions.getText());
			LaunchConfigReader.setPointsToType(conf, pointsToType.getText());
			LaunchConfigReader.setLogLevel(conf, logLevel.getText());
			LaunchConfigReader.setInvertExclusion(conf, exclusionsInvertButton.getSelection());
			LaunchConfigReader.setSimpleDataDependency(conf, simpleDataDepButton.getSelection());
			LaunchConfigReader.setIgnoreExceptions(conf, ignoreExceptionsButton.getSelection());
			LaunchConfigReader.setAddControlFlow(conf, addControlFlowButton.getSelection());
			LaunchConfigReader.setNonTermination(conf, nonTerminationButton.getSelection());
			LaunchConfigReader.setComputeInterference(conf, computeInterferenceButton.getSelection());
			LaunchConfigReader.setScopeFileDataTxt(conf, scopeFileData.getText());
			LaunchConfigReader.setLogfile(conf, logfileName.getText());
		} catch (CoreException e) {
			edu.kit.joana.deprecated.jsdg.gui.Activator.getDefault().showError(e, "Failed to set configuration");
		}
	}

	public void setDefaults(ILaunchConfigurationWorkingCopy conf) {
		try {
			LaunchConfigReader.setMainClassName(conf);
			LaunchConfigReader.setProjectName(conf);
			LaunchConfigReader.setExclusionsTxt(conf);
			LaunchConfigReader.setPointsToType(conf);
			LaunchConfigReader.setLogLevel(conf);
			LaunchConfigReader.setInvertExclusion(conf);
			LaunchConfigReader.setSimpleDataDependency(conf);
			LaunchConfigReader.setIgnoreExceptions(conf);
			LaunchConfigReader.setAddControlFlow(conf);
			LaunchConfigReader.setNonTermination(conf);
			LaunchConfigReader.setComputeInterference(conf);
			LaunchConfigReader.setScopeFileDataTxt(conf);
			LaunchConfigReader.setLogfile(conf);
		} catch (CoreException e) {
			edu.kit.joana.deprecated.jsdg.gui.Activator.getDefault().showError(e, "Failed to set configuration to defaults");
		}
	}

	private void handleProjectButtonSelected(Text target) {
		ILabelProvider lp = new LabelProvider() {
			public String getText(Object element) {
				if (element instanceof IProject) {
					IProject ip = (IProject) element;
					return ip.getName();
				} else {
					return element.toString();
				}
			}
		};

		ElementListSelectionDialog elsd = new ElementListSelectionDialog (getShell(), lp);
		elsd.setElements(this.getWorkspaceRoot().getProjects());
		elsd.setMultipleSelection(false);
		elsd.open();

		if (elsd.getFirstResult() == null) return;

		IProject result = (IProject)elsd.getFirstResult();

		projectName.setText(result.getName());

		/*
		 * if mainClass already selected and not existent in new project,
		 * remove that mainclass-selection
		 */
		IResource ex = result.findMember(mainClass.getText());
		if (ex instanceof IProject || ex == null) {
			mainClass.setText("");
		}
	}

	private void handleMainClassButtonSelected() {
		/*
		 * Determine Searchscope
		 * use selected project as searchscope if already selected
		 * use ALL projects as searchscope if none selected
		 */
		IResource ir = this.getWorkspaceRoot().findMember(projectName.getText());
		IJavaSearchScope searchScope;
		if ( ir == null || ir instanceof IWorkspaceRoot) {
			IJavaModel ijm = JavaCore.create(this.getWorkspaceRoot());

			try {
				searchScope = SearchEngine.createJavaSearchScope(ijm.getJavaProjects());
			} catch (JavaModelException e) {
				// if it fails... use searchscope null
				searchScope = null;
			}
		} else {
			IProject ip =(IProject) ir;
			IJavaProject jp = JavaCore.create(ip);
			searchScope = SearchEngine.createJavaSearchScope(new IJavaElement[]{jp});
		}

		/*
		 * create and open dialog
		 */
		SelectionDialog mtsd = JavaUI.createMainTypeDialog(
				getShell(),
				getLaunchConfigurationDialog(),
				searchScope,
				IJavaElementSearchConstants.CONSIDER_BINARIES,
				false );
		mtsd.open();

		/*
		 * get and evaluate result from dialog
		 */
		Object[] result = mtsd.getResult();
		if (result == null) return;
		IType it = (IType) result[0];
		/*
		 * select corresponding Project to ensure sychronisation
		 */
		//this.projectText.setText(it.getJavaProject().getProject().getName());
		projectName.setText(it.getJavaProject().getProject().getName());

		/*
		 * set fieldcontent
		 */
		mainClass.setText(getMainName(it));
	}

	private String getMainName(IType mainClass) {
        String path = "";
        IJavaElement elem = mainClass.getParent(); // this is the .java file containing mainClass
        String mainFile = elem.getElementName();

        while (elem.getParent() != null && elem.getParent().getElementType() == IJavaElement.PACKAGE_FRAGMENT) {
            elem = elem.getParent();
            path = elem.getElementName() + path;
        }

        if (path.equals("")) {
            return mainFile;

        } else {
            return path + File.separator + mainFile;
        }
    }

	private void handleExclusionsDefaultButtonSelected() {
		exclusionsInvertButton.setSelection(LaunchConfig.EXCLUSIONS_INVERT_DEFAULT);
		exclusions.setText(LaunchConfig.EXCLUSIONS_DEFAULT);
	}

	private void handleExclusionsInvertButtonSelected() {
		updateLaunchConfigurationDialog();
	}

	private void handleSimpleDataDepButtonSelected() {
		updateLaunchConfigurationDialog();
	}

	private void handleIgnoreExceptionsButtonSelected() {
		updateLaunchConfigurationDialog();
	}

	private void handleAddControlFlowButtonSelected() {
		updateLaunchConfigurationDialog();
	}

	private void handleNonTerminationButtonSelected() {
		updateLaunchConfigurationDialog();
	}

	private void handleComputeInterferenceButtonSelected() {
		updateLaunchConfigurationDialog();
	}

	private void handleLogfileButtonSelected() {
		FileDialog dialog = new FileDialog(getShell(), SWT.OPEN);
		dialog.setText("Choose Logfile");
		String file = dialog.open();
		if (file != null) {
			logfileName.setText(file);
		}
	}

	private void handleScopeFileDataDefaultButtonSelected() {
		scopeFileData.setText(LaunchConfig.SCOPE_FILE_DATA_DEFAULT);
	}

	private void handlePointsToTypeDefaultButtonSelected() {
		for (int i = 0; i < pointsToType.getItemCount(); i++) {
			String type = pointsToType.getItem(i);
			if (type.equals(LaunchConfig.POINTS_TO_TYPE_DEFAULT)) {
				pointsToType.select(i);
				break;
			}
		}
	}

	private void handleLogLevelDefaultButtonSelected() {
		for (int i = 0; i < logLevel.getItemCount(); i++) {
			String type = logLevel.getItem(i);
			if (type.equals(LaunchConfig.LOG_LEVEL_DEFAULT)) {
				logLevel.select(i);
				break;
			}
		}
	}

	/**
	 * Convenience method for getting the workspace root.
	 */
	private IWorkspaceRoot getWorkspaceRoot() {
		return ResourcesPlugin.getWorkspace().getRoot();
	}

}
