/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
/*
 * @(c)DefaultTranslator.java
 *
 * Project: GraphViewer
 *
 * Chair for Softwaresystems
 * Faculty of Informatics and Mathematics
 * University of Passau
 *
 * Created on 28.10.2004 at 15:38:44
 */
package edu.kit.joana.ui.ifc.sdg.graphviewer.translation;

import edu.kit.joana.ui.ifc.sdg.graphviewer.controller.event.LanguageEvent;
import edu.kit.joana.ui.ifc.sdg.graphviewer.controller.event.LanguageListener;

import java.util.Hashtable;
import java.util.Locale;
import java.util.ResourceBundle;

import javax.swing.event.EventListenerList;

/**
 * @author <a href="mailto:wellner@fmi.uni-passau.de">Tobias Wellner </a>
 * @version 1.0
 */
public class DefaultTranslator implements BundleConstants, Translator {

    // contains ResourceBundles (all .properties files)
    protected Hashtable<String, ResourceBundle> bundles = null;

    // according to this variable's value the button labels etc. are displayed
    // in the appropiate language
    protected Locale language = null;

    /**
     * @see javax.swing.event.EventListenerList
     *      javax.swing.event.EventListenerList
     */
    protected EventListenerList listeners = new EventListenerList();

    /**
     * @see javax.swing.event.EventListenerList
     *      javax.swing.event.EventListenerList
     */
    private LanguageEvent languageEvent = null;

    /**
     * Constructs a new <code>DefaultTranslator</code> object.
     */
    public DefaultTranslator() {
        this(Locale.getDefault());
    }

    /**
     * Constructs a new <code>DefaultTranslator</code> object.
     *
     * @param language
     *            represents regional settings
     */
    public DefaultTranslator(Locale language) {
        this.language = language;
        this.bundles = new Hashtable<String, ResourceBundle>();
    }

    /**
     * @see edu.kit.joana.ui.ifc.sdg.graphviewer.translation.Translator#getLanguage()
     * @return information about user's regional settings
     */
    public Locale getLanguage() {
        return this.language;
    }

    /**
     * If the user changed its regional settings or the language.
     *
     * @param language
     *            information about local settings and language
     */
    public void setLanguage(Locale language) {
        this.language = language;
        this.bundles.clear();
        this.fireLanguageChanged();
    }

    /**
     * Returns the text in the corresponding .properties file
     *
     * @see edu.kit.joana.ui.ifc.sdg.graphviewer.translation.Translator#getString(edu.kit.joana.ui.ifc.sdg.graphviewer.translation.Resource)
     * @see java.util.ResourceBundle#getBundle(java.lang.String,
     *      java.util.Locale, java.lang.ClassLoader)
     */
    public String getString(Resource resource) {
        if (!(this.bundles.containsKey(resource.getBundle()))) {
            /*
             * getBundle uses the base name, the specified locale, and the
             * default locale (obtained from Locale.getDefault) to generate a
             * sequence of candidate bundle names. If the specified locale's
             * language, country, and variant are all empty strings, then the
             * base name is the only candidate bundle name. Otherwise, the
             * following sequence is generated from the attribute values of the
             * specified locale (language1, country1, and variant1):
             *
             * baseName + "_" + language1 + "_" + country1 + "_" + variant1 etc.
             *
             * getBundle then iterates over the candidate bundle names to find
             * the first one for which it can instantiate an actual resource
             * bundle. For each candidate bundle name, it attempts to create a
             * resource bundle:
             *
             * First, it attempts to load a class using the candidate bundle
             * name. If such a class can be found and loaded using the specified
             * class loader, is assignment compatible with ResourceBundle, is
             * accessible from ResourceBundle, and can be instantiated,
             * getBundle creates a new instance of this class and uses it as the
             * result resource bundle. Otherwise, getBundle attempts to locate a
             * property resource file. It generates a path name from the
             * candidate bundle name by replacing all "." characters with "/"
             * and appending the string ".properties". It attempts to find a
             * "resource" with this name using ClassLoader.getResource. (Note
             * that a "resource" in the sense of getResource has nothing to do
             * with the contents of a resource bundle, it is just a container of
             * data, such as a file.) If it finds a "resource", it attempts to
             * create a new PropertyResourceBundle instance from its contents.
             * If successful, this instance becomes the result resource bundle.
             */
            this.bundles.put(resource.getBundle(), ResourceBundle.getBundle(
                    resource.getBundle(), this.language));
        }
        return this.bundles.get(resource.getBundle())
                .getString(resource.getKey());
    }

    /**
     * @see edu.kit.joana.ui.ifc.sdg.graphviewer.translation.Translator#addLanguageListener(edu.kit.joana.ui.ifc.sdg.graphviewer.controller.event.LanguageListener) *
     * @see javax.swing.event.EventListenerList
     *      javax.swing.event.EventListenerList
     */
    public void addLanguageListener(LanguageListener listener) {
        this.listeners.add(LanguageListener.class, listener);
        listener.languageChanged(new LanguageEvent(this, this.getLanguage()));
    }

    /**
     * @see edu.kit.joana.ui.ifc.sdg.graphviewer.translation.Translator#removeLanguageListener(edu.kit.joana.ui.ifc.sdg.graphviewer.controller.event.LanguageListener)
     * @see javax.swing.event.EventListenerList
     *      javax.swing.event.EventListenerList
     */
    public void removeLanguageListener(LanguageListener listener) {
        listeners.remove(LanguageListener.class, listener);
    }

    /**
     * Notify all listeners that have registered interest for notification on
     * this event type. Process the listeners last to first, notifying those
     * that are interested in this event.
     *
     * @see javax.swing.event.EventListenerList
     *      javax.swing.event.EventListenerList
     */
    protected void fireLanguageChanged() {
        Locale.setDefault(this.language);
        Object[] listeners = this.listeners.getListenerList();
        for (int i = listeners.length - 2; i >= 0; i -= 2) {
            if (listeners[i] == LanguageListener.class) {
                if (this.languageEvent == null) {
                    this.languageEvent = new LanguageEvent(this, this.language);
                }
                ((LanguageListener) listeners[i + 1])
                        .languageChanged(languageEvent);
            }
        }
        this.languageEvent = null;
    }

}
