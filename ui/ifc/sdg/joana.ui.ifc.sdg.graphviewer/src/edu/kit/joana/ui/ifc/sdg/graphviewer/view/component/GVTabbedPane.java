/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
/*
 * @(c)GVTabbedPane.java
 *
 * Project: GraphViewer
 *
 * Chair for Softwaresystems
 * Faculty of Informatics and Mathematics
 * University of Passau
 *
 * Created on 06.11.2004 at 14:15:07
 */
package edu.kit.joana.ui.ifc.sdg.graphviewer.view.component;

import edu.kit.joana.ui.ifc.sdg.graphviewer.controller.event.LanguageEvent;
import edu.kit.joana.ui.ifc.sdg.graphviewer.controller.event.LanguageListener;
import edu.kit.joana.ui.ifc.sdg.graphviewer.translation.Resource;
import edu.kit.joana.ui.ifc.sdg.graphviewer.translation.Translator;

import java.awt.Component;
import java.awt.ComponentOrientation;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import javax.swing.JTabbedPane;

/**
 * A component that lets the user switch between a group of components by
 * clicking on a tab with a given title and/or icon.
 *
 * Contains only language support specific novelties.
 *
 * @author <a href="mailto:wellner@fmi.uni-passau.de">Tobias Wellner </a>
 * @version 1.0
 */
public class GVTabbedPane extends JTabbedPane implements LanguageListener {
	private static final long serialVersionUID = 6564343972219436013L;

	protected List<Resource> tabTitles = new LinkedList<Resource>();

    protected Translator translator = null;

    /**
     * Constructs a new <code>GVTabbedPane</code> object.
     */
    public GVTabbedPane(Translator translator) {
        super();
        this.translator = translator;
        this.translator.addLanguageListener(this);
    }

    public void addTab(Resource title, Component component) {
        super.addTab(this.translator.getString(title), component);
        this.tabTitles.add(title);
    }

    public void removeTab(Resource title) {
        this.removeTabAt(this.tabTitles.indexOf(title));
        this.tabTitles.remove(title);
    }

    /**
     * @see edu.kit.joana.ui.ifc.sdg.graphviewer.controller.event.LanguageListener#languageChanged(edu.kit.joana.ui.ifc.sdg.graphviewer.controller.event.LanguageEvent)
     * @param event
     */
    public void languageChanged(LanguageEvent event) {
        int index = 0;
        for (Iterator<Resource> i = this.tabTitles.iterator(); i.hasNext();) {
            this.setTitleAt(index++, this.translator.getString(i
                    .next()));
        }
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

}
