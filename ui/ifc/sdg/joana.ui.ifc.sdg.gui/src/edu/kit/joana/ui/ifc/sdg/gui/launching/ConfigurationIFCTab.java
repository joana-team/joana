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
package edu.kit.joana.ui.ifc.sdg.gui.launching;


import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.ui.ILaunchConfigurationTab;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;

import edu.kit.joana.ui.ifc.sdg.gui.NJSecPlugin;

public class ConfigurationIFCTab extends AbstractJoanaTab {

    // fields for chosen IFC
    private Button classic;
//    private Button classicTerminationSensitive;
	private Button krinke;
    private Button probabilistic;
//    private Button probabilisticTerminationSensitive;
    private Button possibilistic;
//    private Button possibilisticTerminationSensitive;


	public void createControl(Composite parent) {
		Composite comp = new Composite(parent, SWT.NONE);
		setControl(comp);
		GridLayout topLayout = new GridLayout(1, true);
		topLayout.horizontalSpacing = 10;
		comp.setLayout(topLayout);
		comp.setFont(parent.getFont());

		GridData gd;

		Group ifcGroup = new Group(comp, SWT.NONE);
		GridLayout ifcLayout = new GridLayout();
		ifcLayout.numColumns = 1;
		ifcGroup.setLayout(ifcLayout);
		gd = new GridData(GridData.FILL_HORIZONTAL);
		gd.horizontalSpan = 1;
		ifcGroup.setLayoutData(gd);
		ifcGroup.setText("Choose from the following kinds of IFC:");
		ifcGroup.setFont(comp.getFont());

		classic = new Button(ifcGroup,SWT.RADIO);
		classic.setText("Hammer's noninterference for sequential programs");
        gd = new GridData();
        gd.horizontalSpan = 1;
        classic.setLayoutData(gd);
        classic.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent evt) {
                updateLaunchConfigurationDialog();
            }
        });

//        classicTerminationSensitive = new Button(ifcGroup,SWT.RADIO);
//        classicTerminationSensitive.setText("Noninterference for sequential programs (termination-sensitive)");
//        gd = new GridData();
//        gd.horizontalSpan = 1;
//        classicTerminationSensitive.setLayoutData(gd);
//        classicTerminationSensitive.addSelectionListener(new SelectionAdapter() {
//            public void widgetSelected(SelectionEvent evt) {
//                updateLaunchConfigurationDialog();
//            }
//        });

		krinke = new Button(ifcGroup,SWT.RADIO);
		krinke.setText("Krinke's noninterference for sequential programs");
        gd = new GridData();
        gd.horizontalSpan = 1;
        krinke.setLayoutData(gd);
        krinke.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent evt) {
                updateLaunchConfigurationDialog();
            }
        });

        possibilistic = new Button(ifcGroup,SWT.RADIO);
        possibilistic.setText("Possibilistic noninterference");
        gd = new GridData();
        gd.horizontalSpan = 1;
        possibilistic.setLayoutData(gd);
        possibilistic.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent evt) {
                updateLaunchConfigurationDialog();
            }
        });

//        possibilisticTerminationSensitive = new Button(ifcGroup,SWT.RADIO);
//        possibilisticTerminationSensitive.setText("Possibilistic noninterference (termination-sensitive)");
//        gd = new GridData();
//        gd.horizontalSpan = 1;
//        possibilisticTerminationSensitive.setLayoutData(gd);
//        possibilisticTerminationSensitive.addSelectionListener(new SelectionAdapter() {
//            public void widgetSelected(SelectionEvent evt) {
//                updateLaunchConfigurationDialog();
//            }
//        });

        probabilistic = new Button(ifcGroup,SWT.RADIO);
        probabilistic.setText("Probabilistic noninterference");
        gd = new GridData();
        gd.horizontalSpan = 1;
        probabilistic.setLayoutData(gd);
        probabilistic.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent evt) {
                updateLaunchConfigurationDialog();
            }
        });

//        probabilisticTerminationSensitive = new Button(ifcGroup,SWT.RADIO);
//        probabilisticTerminationSensitive.setText("Probabilistic noninterference (termination-sensitive)");
//        gd = new GridData();
//        gd.horizontalSpan = 1;
//        probabilisticTerminationSensitive.setLayoutData(gd);
//        probabilisticTerminationSensitive.addSelectionListener(new SelectionAdapter() {
//            public void widgetSelected(SelectionEvent evt) {
//                updateLaunchConfigurationDialog();
//            }
//        });
	}

	public void setDefaults(ILaunchConfigurationWorkingCopy configuration) {
		configuration.setContainer(null);
		configuration.setAttribute(ConfigurationAttributes.CLASS_NI, true);
		configuration.setAttribute(ConfigurationAttributes.CLASS_NI_TS, false);
		configuration.setAttribute(ConfigurationAttributes.PROB_NI, false);
		configuration.setAttribute(ConfigurationAttributes.PROB_NI_TS, false);
        configuration.setAttribute(ConfigurationAttributes.POSS_NI, false);
        configuration.setAttribute(ConfigurationAttributes.POSS_NI_TS, false);
	}

	public void initializeFrom(ILaunchConfiguration configuration) {
		ConfigReader cr = new ConfigReader(configuration);

		try {
		    classic.setSelection(cr.getClassicNI());
//	        classicTerminationSensitive.setSelection(cr.getClassicNIWithTermination());
		    krinke.setSelection(cr.getKrinkeNI());
	        probabilistic.setSelection(cr.getProbabilisticNI());
//	        probabilisticTerminationSensitive.setSelection(cr.getProbabilisticNIWithTermination());
            possibilistic.setSelection(cr.getPossibilisticNI());
//            possibilisticTerminationSensitive.setSelection(cr.getPossibilisticNIWithTermination());

		} catch (CoreException e) {
			NJSecPlugin.singleton().showError("Problem while reading configuration attributes", null, e);
		}
	}

	public void performApply(ILaunchConfigurationWorkingCopy configuration) {
	    configuration.setAttribute(ConfigurationAttributes.CLASS_NI, classic.getSelection());
//        configuration.setAttribute(ConfigurationAttributes.CLASS_NI_TS, classicTerminationSensitive.getSelection());
	    configuration.setAttribute(ConfigurationAttributes.KRINKE_NI, krinke.getSelection());
        configuration.setAttribute(ConfigurationAttributes.PROB_NI, probabilistic.getSelection());
//        configuration.setAttribute(ConfigurationAttributes.PROB_NI_TS, probabilisticTerminationSensitive.getSelection());
        configuration.setAttribute(ConfigurationAttributes.POSS_NI, possibilistic.getSelection());
//        configuration.setAttribute(ConfigurationAttributes.POSS_NI_TS, possibilisticTerminationSensitive.getSelection());
	}

	/**
	 * @see ILaunchConfigurationTab#getName()
	 */
	public String getName() {
		return "IFC Algorithms";
	}
}
