/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
/*
 * @(c)GVComboBox.java
 *
 * Project: GraphViewer
 *
 * Chair for Softwaresystems
 * Faculty of Informatics and Mathematics
 * University of Passau
 *
 * Created on 06.11.2004 at 13:51:56
 */
package edu.kit.joana.ui.ifc.sdg.graphviewer.view.component;

import edu.kit.joana.ui.ifc.sdg.graphviewer.controller.event.LanguageEvent;
import edu.kit.joana.ui.ifc.sdg.graphviewer.controller.event.LanguageListener;
import edu.kit.joana.ui.ifc.sdg.graphviewer.translation.Resource;
import edu.kit.joana.ui.ifc.sdg.graphviewer.translation.Translator;


import java.awt.Component;
import java.awt.ComponentOrientation;

import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.ListCellRenderer;

/**
 * A component that combines a button or editable field and a drop-down list.
 * The user can select a value from the drop-down list, which appears at the
 * user's request.
 *
 * Contains only language support specific novelties.
 *
 * @author <a href="mailto:wellner@fmi.uni-passau.de">Tobias Wellner </a>
 * @version 1.0
 *
 */

@SuppressWarnings({"rawtypes", "unchecked"})
public class GVComboBox extends JComboBox implements LanguageListener {
	private static final long serialVersionUID = -1796655306771599978L;
	protected Translator translator = null;

    /**
     * Constructs a new <code>GVComboBox</code> object.
     */
    public GVComboBox(Translator translator, Resource[] items) {
        super(items);
        this.translator = translator;
        this.translator.addLanguageListener(this);
        this.setRenderer(new GVComboBoxCellRenderer());
    }

    /**
     * @see edu.kit.joana.ui.ifc.sdg.graphviewer.controller.event.LanguageListener#languageChanged(edu.kit.joana.ui.ifc.sdg.graphviewer.controller.event.LanguageEvent)
     */
    public void languageChanged(LanguageEvent event) {
        this.setLocale(event.getLanguage());
        this.setComponentOrientation(ComponentOrientation.getOrientation(event
                .getLanguage()));
    }

    public Translator getTranslator() {
        return this.translator;
    }

    public void setTranslator(Translator translator) {
        this.translator.removeLanguageListener(this);
        this.translator = translator;
        this.translator.addLanguageListener(this);
    }

    private class GVComboBoxCellRenderer extends JLabel implements  ListCellRenderer {
		private static final long serialVersionUID = -2428964537665771602L;

		public GVComboBoxCellRenderer() {
            this.setOpaque(true);
        }

        public Component getListCellRendererComponent(JList list, Object value,
                int index, boolean isSelected, boolean cellHasFocus) {
            this.setText(translator.getString((Resource) value));
            if (isSelected) {
                this.setBackground(list.getSelectionBackground());
                this.setForeground(list.getSelectionForeground());
            } else {
                this.setBackground(list.getBackground());
                this.setForeground(list.getForeground());
            }
            return this;
        }
    }

}
