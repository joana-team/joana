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
/*******************************************************************************
 * Copyright (c) 2003, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package edu.kit.joana.ui.ifc.sdg.latticeeditor.figures;

import org.eclipse.draw2d.ColorConstants;
import org.eclipse.draw2d.Graphics;
import org.eclipse.draw2d.MarginBorder;
import org.eclipse.draw2d.geometry.PointList;
import org.eclipse.draw2d.geometry.Rectangle;

/**
 * @author hudsonr
 */
public class SequentialActivityFigure extends SubgraphFigure {

	static final MarginBorder MARGIN_BORDER = new MarginBorder(0, 8, 0, 0);

	static final PointList ARROW = new PointList(3);
	{
		ARROW.addPoint(0, 0);
		ARROW.addPoint(10, 0);
		ARROW.addPoint(5, 5);
	}

	/**
	 * @param header
	 * @param footer
	 */
	public SequentialActivityFigure() {
		super(new StartTag(""), new EndTag(""));
		setBorder(MARGIN_BORDER);
		setOpaque(true);
	}

	protected void paintFigure(Graphics graphics) {
		super.paintFigure(graphics);
		graphics.setBackgroundColor(ColorConstants.button);
		Rectangle r = getBounds();
		graphics.fillRectangle(r.x + 13, r.y + 10, 8, r.height - 18);
		// graphics.fillPolygon(ARROW);
		// graphics.drawPolygon(ARROW);
	}

}
