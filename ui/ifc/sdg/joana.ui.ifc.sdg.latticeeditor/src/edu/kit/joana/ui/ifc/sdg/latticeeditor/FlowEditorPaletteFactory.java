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
package edu.kit.joana.ui.ifc.sdg.latticeeditor;

import java.util.ArrayList;
import java.util.List;


import org.eclipse.gef.palette.CombinedTemplateCreationEntry;
import org.eclipse.gef.palette.ConnectionCreationToolEntry;
import org.eclipse.gef.palette.MarqueeToolEntry;
import org.eclipse.gef.palette.PaletteContainer;
import org.eclipse.gef.palette.PaletteDrawer;
import org.eclipse.gef.palette.PaletteEntry;
import org.eclipse.gef.palette.PaletteGroup;
import org.eclipse.gef.palette.PaletteRoot;
import org.eclipse.gef.palette.PaletteSeparator;
import org.eclipse.gef.palette.SelectionToolEntry;
import org.eclipse.gef.palette.ToolEntry;
import org.eclipse.gef.requests.SimpleFactory;
import org.eclipse.jface.resource.ImageDescriptor;

import edu.kit.joana.ui.ifc.sdg.latticeeditor.model.Activity;
import edu.kit.joana.ui.ifc.sdg.latticeeditor.model.ParallelActivity;
import edu.kit.joana.ui.ifc.sdg.latticeeditor.model.SequentialActivity;

/**
 * Handles the creation of the palette for the Flow Editor.
 *
 * @author Daniel Lee
 */
public class FlowEditorPaletteFactory {

	private static List<PaletteContainer> createCategories(PaletteRoot root) {
		List<PaletteContainer> categories = new ArrayList<PaletteContainer>();
		categories.add(createControlGroup(root));
		categories.add(createComponentsDrawer());
		return categories;
	}

	private static PaletteContainer createComponentsDrawer() {

		PaletteDrawer drawer = new PaletteDrawer("Components", null);

		List<CombinedTemplateCreationEntry> entries = new ArrayList<CombinedTemplateCreationEntry>();

		CombinedTemplateCreationEntry combined = new CombinedTemplateCreationEntry("Activity", "Create a new Activity Node", Activity.class, new SimpleFactory(Activity.class), ImageDescriptor
				.createFromFile(LatticeEditorPlugin.class, "images/gear16.gif"), ImageDescriptor.createFromFile(Activity.class, "images/gear16.gif"));
		entries.add(combined);

		combined = new CombinedTemplateCreationEntry("Sequential Activity", "Create a Sequential Activity", SequentialActivity.class, new SimpleFactory(SequentialActivity.class), ImageDescriptor
				.createFromFile(LatticeEditorPlugin.class, "images/sequence16.gif"), ImageDescriptor.createFromFile(LatticeEditorPlugin.class, "images/sequence16.gif"));
		entries.add(combined);

		combined = new CombinedTemplateCreationEntry("Parallel Activity", "Create a  Parallel Activity", ParallelActivity.class, new SimpleFactory(ParallelActivity.class), ImageDescriptor
				.createFromFile(LatticeEditorPlugin.class, "images/parallel16.gif"), ImageDescriptor.createFromFile(LatticeEditorPlugin.class, "images/parallel16.gif"));
		entries.add(combined);

		drawer.addAll(entries);
		return drawer;
	}

	private static PaletteContainer createControlGroup(PaletteRoot root) {
		PaletteGroup controlGroup = new PaletteGroup("Control Group");

		List<Object> entries = new ArrayList<Object>();

		ToolEntry tool = new SelectionToolEntry();
		entries.add(tool);
		root.setDefaultEntry(tool);

		tool = new MarqueeToolEntry();
		entries.add(tool);

		PaletteSeparator sep = new PaletteSeparator("org.eclipse.gef.latticeeditor.sep2");
		sep.setUserModificationPermission(PaletteEntry.PERMISSION_NO_MODIFICATION);
		entries.add(sep);

		tool = new ConnectionCreationToolEntry("Connection Creation", "Creating connections", null, ImageDescriptor.createFromFile(LatticeEditorPlugin.class, "images/connection16.gif"),
				ImageDescriptor.createFromFile(Activity.class, "images/connection16.gif"));
		entries.add(tool);
		controlGroup.addAll(entries);
		return controlGroup;
	}

	/**
	 * Creates the PaletteRoot and adds all Palette elements.
	 *
	 * @return the root
	 */
	public static PaletteRoot createPalette() {
		PaletteRoot flowPalette = new PaletteRoot();
		flowPalette.addAll(createCategories(flowPalette));
		return flowPalette;
	}

}
