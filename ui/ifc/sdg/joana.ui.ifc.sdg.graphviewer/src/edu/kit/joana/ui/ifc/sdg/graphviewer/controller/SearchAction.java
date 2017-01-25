/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.ui.ifc.sdg.graphviewer.controller;

import java.awt.event.ActionEvent;

import edu.kit.joana.ui.ifc.sdg.graphviewer.translation.BundleConstants;
import edu.kit.joana.ui.ifc.sdg.graphviewer.view.SearchDialog;

/**
 * displays search dialog
 */
public class SearchAction extends AbstractGVAction implements BundleConstants {
	private static final long serialVersionUID = 9163795803048039942L;
	private SearchDialog searchDialog = null;

    /**
     * constructor
     * @param searchDialog
     * 				instance of search dialog
     */
    public SearchAction(SearchDialog searchDialog) {
        super("search.name", "Search.png", "search.description",
        		"search");
        this.searchDialog = searchDialog;
    }
    /**
     * show search dialog
     */
    public void actionPerformed(ActionEvent event) {
        this.searchDialog.showSearchDialog();
    }

}
