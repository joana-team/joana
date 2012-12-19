/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
/*
 * @(c)CommandStatusEvent.java
 *
 * Project: GraphViewer
 *
 * Chair for Softwaresystems
 * Faculty of Informatics and Mathematics
 * University of Passau
 *
 * Created on 28.11.2004 at 12:50:24
 */
package edu.kit.joana.ui.ifc.sdg.graphviewer.controller.event;

import edu.kit.joana.ui.ifc.sdg.graphviewer.translation.Resource;

import java.util.EventObject;

/**
 * @author <a href="mailto:wellner@fmi.uni-passau.de">Tobias Wellner </a>
 * @version 1.0
 */
public class CommandStatusEvent extends EventObject { //TODO eventsystem wiederaufbauen
	private static final long serialVersionUID = -2605660935265334967L;

	/**
     * Constant with value 0 to indicate successful execution of the command.
     */
    public static final int SUCCESS = 0;

    /**
     * Constant with value -1 to indicate the failure of the command.
     */
    public static final int FAILURE = -1;

    protected int status = FAILURE;

    protected Resource message = null;

    /**
     * @param source
     *            the command object that invoked the event
     * @param status
     *            indicates success or failure
     * @param message
     *            language information
     */
    public CommandStatusEvent(Object source, int status, Resource message) {
        super(source);
        this.status = status;
        this.message = message;
    }

    /**
     * @return the resource that contains information about language and other
     *         local settings
     */
    public Resource getMessageResource() {
        return this.message;
    }

    /**
     * @return the status which indicates if a command was executed successfully
     */
    public int getStatus() {
        return this.status;
    }

}
