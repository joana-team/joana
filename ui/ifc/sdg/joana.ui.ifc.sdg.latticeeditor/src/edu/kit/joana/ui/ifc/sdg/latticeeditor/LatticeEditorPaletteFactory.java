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

/**
 * Handles the creation of the palette for the Flow Editor.
 *
 * @author Daniel Lee
 */
public class LatticeEditorPaletteFactory extends FlowEditorPaletteFactory {

	private static List<PaletteContainer> createCategories(PaletteRoot root) {
		List<PaletteContainer> categories = new ArrayList<PaletteContainer>();
		categories.add(createControlGroup(root));

		//Components nicht mehr benoetigt.
		//categories.add(createComponentsDrawer());
		return categories;
	}

	@SuppressWarnings("unused")
    private static PaletteContainer createComponentsDrawer() {

		PaletteDrawer drawer = new PaletteDrawer("Components", null);

		List<CombinedTemplateCreationEntry> entries = new ArrayList<CombinedTemplateCreationEntry>();

		CombinedTemplateCreationEntry combined = new CombinedTemplateCreationEntry("Security Class", "Create a new Security Class", Activity.class, new SimpleFactory(Activity.class), ImageDescriptor
				.createFromFile(LatticeEditorPlugin.class, "images/schloss4e.gif"), // gear16.gif
				ImageDescriptor.createFromFile(Activity.class, "images/schloss4e.gif"));
		entries.add(combined);
		/*
		 * combined = new CombinedTemplateCreationEntry( "Sequential Activity",
		 * "Create a Sequential Activity", SequentialActivity.class, new
		 * SimpleFactory(SequentialActivity.class),
		 * ImageDescriptor.createFromFile(FlowPlugin.class,
		 * "images/sequence16.gif"),
		 * ImageDescriptor.createFromFile(FlowPlugin.class,
		 * "images/sequence16.gif") ); entries.add(combined);
		 *
		 * combined = new CombinedTemplateCreationEntry( "Generate LatticeFile",
		 * "Generate LatticeFile from this Graph", ParallelActivity.class, new
		 * SimpleFactory(ParallelActivity.class),
		 * ImageDescriptor.createFromFile(FlowPlugin.class,
		 * "images/sequence16.gif"),
		 * ImageDescriptor.createFromFile(FlowPlugin.class,
		 * "images/sequence16.gif") //parallel ); entries.add(combined);
		 */
		drawer.addAll(entries);
		return drawer;
	}

	private static PaletteContainer createControlGroup(PaletteRoot root) {
		PaletteGroup controlGroup = new PaletteGroup("Control Group");

		List<Object> entries = new ArrayList<Object>();

		ToolEntry tool = new SelectionToolEntry();
		entries.add(tool);
		root.setDefaultEntry(tool);

		//Marquee als tool nicht benoetigt.
		//tool = new MarqueeToolEntry();
		//entries.add(tool);

		PaletteSeparator sep = new PaletteSeparator("org.eclipse.gef.latticeeditor.sep2");
		sep.setUserModificationPermission(PaletteEntry.PERMISSION_NO_MODIFICATION);
		entries.add(sep);

		tool = new ConnectionCreationToolEntry("Relation Creation", "Creating new Relations between security classes", null, ImageDescriptor.createFromFile(LatticeEditorPlugin.class,
				"images/connection16.gif"), ImageDescriptor.createFromFile(Activity.class, "images/connection16.gif"));
		entries.add(tool);

		//Neues Schloss erstellen
		CombinedTemplateCreationEntry combined = new CombinedTemplateCreationEntry("Security Class", "Create a new Security Class", Activity.class, new SimpleFactory(Activity.class), ImageDescriptor
            .createFromFile(LatticeEditorPlugin.class, "images/schloss4e.gif"), // gear16.gif
            ImageDescriptor.createFromFile(Activity.class, "images/schloss4e.gif"));
		entries.add(combined);

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
