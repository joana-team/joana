/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.ui.ifc.sdg.textual.highlight.highlight;
/*
// * Created on 11.09.2005
// * @author kai brueckner
// * University of Passau
// */
//package edu.kit.joana.ui.ifc.sdg.textual.highlight.highlight;
//
//import org.eclipse.jface.preference.ColorFieldEditor;
//import org.eclipse.jface.preference.FieldEditorPreferencePage;
//import org.eclipse.jface.preference.FloatFieldEditor;
//import org.eclipse.jface.preference.IPreferenceStore;
//import org.eclipse.jface.preference.IntegerFieldEditor;
//import org.eclipse.jface.preference.PreferenceConverter;
//import org.eclipse.swt.graphics.RGB;
//import org.eclipse.ui.IWorkbench;
//import org.eclipse.ui.IWorkbenchPreferencePage;
//import org.eclipse.ui.internal.editors.text.EditorsPlugin;
//
//@SuppressWarnings("restriction")
//public class HighlightPreferencePage extends FieldEditorPreferencePage implements IWorkbenchPreferencePage {
//
//	// the used ColorStep
//	private ColorStep step = null;
//
//	RGB bColor = null;
//
//	// the PreferenceStore
//	protected IPreferenceStore hStore;
//
//	/**
//	 * The constructor
//	 */
//	public HighlightPreferencePage() {
//		super(GRID);
//		hStore  = HighlightPlugin.getDefault().getPreferenceStore();
//		setPreferenceStore(hStore);
//		setDescription("Highlighting Preferences");
//
//		// initialize ColorStep and the base color
//		initialiseStepAndColor();
//		updateColors();
//	}
//
//	/*
//	 * @see org.eclipse.ui.IWorkbenchPreferencePage#init(org.eclipse.ui.IWorkbench)
//	 */
//	public void init(IWorkbench workbench) { }
//
//	/*
//	 * @see org.eclipse.jface.preference.FieldEditorPreferencePage#createFieldEditors()
//	 */
//	protected void createFieldEditors() {
//
//		// TODO check if supplied markers exist ... not hard code like here
//		IntegerFieldEditor ife =
//			new IntegerFieldEditor(HighlightPlugin.MAXLEVELS, "Maximum #Levels", getFieldEditorParent(), 4);
//		// addField(new StringFieldEditor(HighlightPlugin.STEP_RED,
//		// "Step size red",4,getFieldEditorParent()));
//		// addField(new StringFieldEditor(HighlightPlugin.STEP_BLUE,
//		// "Step size blue",4,getFieldEditorParent()));
//		// addField(new StringFieldEditor(HighlightPlugin.STEP_GREEN,
//		// "Step size green",4,getFieldEditorParent()));
//		//
//		FloatFieldEditor ffe1 = (new FloatFieldEditor(HighlightPlugin.STEP_RED, "Step size red", 6, getFieldEditorParent()));
//		FloatFieldEditor ffe2 = (new FloatFieldEditor(HighlightPlugin.STEP_BLUE, "Step size blue", 6, getFieldEditorParent()));
//		FloatFieldEditor ffe3 = (new FloatFieldEditor(HighlightPlugin.STEP_GREEN, "Step size green", 6, getFieldEditorParent()));
//
//		ife.setValidRange(0, 100);
//		ffe1.setValidRange(-255, 255);
//		ffe2.setValidRange(-255, 255);
//		ffe3.setValidRange(-255, 255);
//
//		ife.setErrorMessage("Input must be of type int and between 0 and 100!");
//		ffe1.setErrorMessage("Input must be of type float and between -255 and 255!");
//		ffe2.setErrorMessage("Input must be of type float and between -255 and 255!");
//		ffe3.setErrorMessage("Input must be of type float and between -255 and 255!");
//
//		addField(new ColorFieldEditor(HighlightPlugin.COLOR_KEY, "Basic color value", getFieldEditorParent()));
//		addField(ife);
//		addField(ffe1);
//		addField(ffe2);
//		addField(ffe3);
//	}
//
//	// TODO: deprecated
//	public void performApply() {
//		if (super.performOk()) {
//			initialiseStepAndColor();
//			updateColors();
//			EditorsPlugin.getDefault().savePluginPreferences();
//			HighlightPlugin.getDefault().savePluginPreferences();
//		}
//	}
//
//	private void initialiseStepAndColor() {
//		step = new ColorStep(hStore.getFloat(HighlightPlugin.STEP_RED), hStore
//				.getFloat(HighlightPlugin.STEP_GREEN), hStore
//				.getFloat(HighlightPlugin.STEP_BLUE));
//		bColor = PreferenceConverter
//				.getColor(hStore, HighlightPlugin.COLOR_KEY);
//	}
//
//	private void updateColors() {
//		RGB[] colorLevel = new RGB[hStore.getInt(HighlightPlugin.MAXLEVELS) + 1];
//		int level = hStore.getInt(HighlightPlugin.MAXLEVELS);
//
//		for (int i = 0; i <= level; i++) {
//			colorLevel[i] = new RGB(
//					inv((bColor.red + (int) (step.red * i)) % 256),
//					inv((bColor.green + (int) (step.green * i)) % 256),
//					inv((bColor.blue + (int) (step.blue * i)) % 256));
//			setColor(colorLevel[i], i);
//			// System.out.println("Level: "+i+" red: "+colorLevel[i].red+"
//			// green: "+colorLevel[i].green+" blue: "+colorLevel[i].blue);
//		}
//	}
//
//	private int inv(int col) {
//		if (col < 0) {
//			return (col + 255);
//		} else
//			return col;
//	}
//
//	protected void setColor(RGB color, int level) {
//		IPreferenceStore store = EditorsPlugin.getDefault().getPreferenceStore();
//		PreferenceConverter.setValue(store, "level" + level + ".highlight.color", color);
//	}
//}
