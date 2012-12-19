/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
/*******************************************************************************
 * Copyright (c) 2000, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package edu.kit.joana.ui.ifc.sdg.latticeeditor.policies;


import org.eclipse.draw2d.PolylineConnection;

import org.eclipse.gef.EditPart;
import org.eclipse.gef.Request;
import org.eclipse.gef.commands.Command;
import org.eclipse.gef.editpolicies.ConnectionEditPolicy;
import org.eclipse.gef.requests.CreateRequest;
import org.eclipse.gef.requests.GroupRequest;

import edu.kit.joana.ui.ifc.sdg.latticeeditor.commands.DeleteConnectionCommand;
import edu.kit.joana.ui.ifc.sdg.latticeeditor.commands.SplitTransitionCommand;
import edu.kit.joana.ui.ifc.sdg.latticeeditor.model.Activity;
import edu.kit.joana.ui.ifc.sdg.latticeeditor.model.LatticeElement;
import edu.kit.joana.ui.ifc.sdg.latticeeditor.model.Transition;
import edu.kit.joana.ui.ifc.sdg.latticeeditor.parts.TransitionPart;

/**
 * EditPolicy for Transitions. Supports deletion and "splitting", i.e. adding an
 * Activity that splits the transition into an incoming and outgoing connection
 * to the new Activity.
 *
 * @author Daniel Lee
 */
public class TransitionEditPolicy extends ConnectionEditPolicy {

	/**
	 * @see org.eclipse.gef.editpolicies.ConnectionEditPolicy#getCommand(org.eclipse.gef.Request)
	 */
	public Command getCommand(Request request) {
		if (REQ_CREATE.equals(request.getType()))
			return getSplitTransitionCommand(request);
		return super.getCommand(request);
	}

	private PolylineConnection getConnectionFigure() {
		return ((PolylineConnection) ((TransitionPart) getHost()).getFigure());
	}

	/**
	 * @see ConnectionEditPolicy#getDeleteCommand(org.eclipse.gef.requests.GroupRequest)
	 */
	protected Command getDeleteCommand(GroupRequest request) {
		DeleteConnectionCommand cmd = new DeleteConnectionCommand();
		Transition t = (Transition) getHost().getModel();
		cmd.setTransition(t);
		cmd.setSource(t.source);
		cmd.setTarget(t.target);
		return cmd;
	}

	protected Command getSplitTransitionCommand(Request request) {
		SplitTransitionCommand cmd = new SplitTransitionCommand();
		cmd.setTransition(((Transition) getHost().getModel()));
		cmd.setParent(((LatticeElement) ((TransitionPart) getHost()).getSource().getParent().getModel()));
		cmd.setNewActivity(((Activity) ((CreateRequest) request).getNewObject()));
		return cmd;
	}

	/**
	 * @see org.eclipse.gef.editpolicies.AbstractEditPolicy#getTargetEditPart(org.eclipse.gef.Request)
	 */
	public EditPart getTargetEditPart(Request request) {
		if (REQ_CREATE.equals(request.getType()))
			return getHost();
		return null;
	}

	/**
	 * @see org.eclipse.gef.editpolicies.AbstractEditPolicy#eraseTargetFeedback(org.eclipse.gef.Request)
	 */
	public void eraseTargetFeedback(Request request) {
		if (REQ_CREATE.equals(request.getType()))
			getConnectionFigure().setLineWidth(1);
	}

	/**
	 * @see org.eclipse.gef.editpolicies.AbstractEditPolicy#showTargetFeedback(org.eclipse.gef.Request)
	 */
	public void showTargetFeedback(Request request) {
		if (REQ_CREATE.equals(request.getType()))
			getConnectionFigure().setLineWidth(2);
	}

}
