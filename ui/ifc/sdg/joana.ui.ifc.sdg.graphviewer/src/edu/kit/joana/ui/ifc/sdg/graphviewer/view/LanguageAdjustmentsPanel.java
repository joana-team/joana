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
 * @(c)LanguageAdjustmentsPanel.java
 *
 * Project: GraphViewer
 *
 * Chair for Softwaresystems
 * Faculty of Informatics and Mathematics
 * University of Passau
 *
 * Created on 06.11.2004 at 13:38:29
 */
package edu.kit.joana.ui.ifc.sdg.graphviewer.view;

import edu.kit.joana.ui.ifc.sdg.graphviewer.translation.BundleConstants;
import edu.kit.joana.ui.ifc.sdg.graphviewer.translation.DefaultTranslator;
import edu.kit.joana.ui.ifc.sdg.graphviewer.translation.Resource;
import edu.kit.joana.ui.ifc.sdg.graphviewer.translation.Translator;
import edu.kit.joana.ui.ifc.sdg.graphviewer.view.component.GVComboBox;
import edu.kit.joana.ui.ifc.sdg.graphviewer.view.component.GVLabel;
import edu.kit.joana.ui.ifc.sdg.graphviewer.view.component.GVPanel;


import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

import java.util.Locale;

import javax.swing.JLabel;

/**
 * @author <a href="mailto:wellner@fmi.uni-passau.de">Tobias Wellner </a>
 * @version 1.0
 */
public class LanguageAdjustmentsPanel extends GVPanel implements BundleConstants {
	private static final long serialVersionUID = 6530113091687876144L;

	private static final Resource[] LANGUAGES = new Resource[] {
            new Resource(LANGUAGE_ADJUSTMENTS_BUNDLE, "german.label"),
            new Resource(LANGUAGE_ADJUSTMENTS_BUNDLE, "english.label"),
            new Resource(LANGUAGE_ADJUSTMENTS_BUNDLE, "italian.label") };

    private GVComboBox languageBox = null;

    public LanguageAdjustmentsPanel() {
        super(new DefaultTranslator(), new GridBagLayout());
    }

    @Override
	public void setTranslator(Translator translator) {
        this.translator.removeLanguageListener(this);
        this.translator = translator;
        this.translator.addLanguageListener(this);
        this.initComponents();
    }

    public void storeValues(Locale language) {
        if (languageBox != null) {
            if (language.equals(Locale.GERMANY)) {
                this.languageBox.setSelectedIndex(0);
            } else if (language.equals(Locale.US)) {
                this.languageBox.setSelectedIndex(1);
            } else if (language.equals(Locale.ITALY)) {
                this.languageBox.setSelectedIndex(2);
            }
        }
    }

    private void initComponents() {

        GridBagConstraints constraints = new GridBagConstraints();
        constraints.fill = GridBagConstraints.NONE;
        constraints.gridx = 0;
        constraints.gridy = 0;
        constraints.insets = new Insets(10, 10, 0, 0);

        GVLabel languageLabel = new GVLabel(this.translator, new Resource(
                BundleConstants.LANGUAGE_ADJUSTMENTS_BUNDLE, "language.label"));
        this.add(languageLabel, constraints);

        constraints.gridx = 1;
        this.languageBox = new GVComboBox(this.translator, LANGUAGES);
        languageBox.setEditable(false);
        this.add(this.languageBox, constraints);

        constraints.gridx = 2;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.weightx = 1.0;
        constraints.insets = new Insets(0, 0, 0, 0);
        this.add(new JLabel(), constraints);

        constraints.gridx = 0;
        constraints.gridy = 1;
        constraints.weightx = 0;
        constraints.weighty = 1.0;
        constraints.fill = GridBagConstraints.VERTICAL;
        this.add(new JLabel(), constraints);

    }

    public Locale getLanguage() {
        switch (this.languageBox.getSelectedIndex()) {
        case 0:
            return Locale.GERMANY;
        case 1:
            return Locale.US;
        case 2:
            return Locale.ITALY;
        default:
            throw new RuntimeException(
                    "Exception: Invalid selected index in language combobox.");
        }
    }
}
