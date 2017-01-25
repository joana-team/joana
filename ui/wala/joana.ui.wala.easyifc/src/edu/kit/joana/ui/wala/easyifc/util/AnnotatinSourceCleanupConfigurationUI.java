package edu.kit.joana.ui.wala.easyifc.util;

import java.util.Map;

import org.eclipse.jdt.internal.ui.fix.AbstractCleanUp;
import org.eclipse.jdt.internal.ui.preferences.cleanup.AbstractCleanUpTabPage;
import org.eclipse.jdt.ui.cleanup.CleanUpOptions;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;

public class AnnotatinSourceCleanupConfigurationUI extends AbstractCleanUpTabPage {

	public static final String ID = "edu.kit.joana.ui.wala.easyifc.util.AnnotatinSourceCleanupConfigurationUI.tabpage";

	public static final String GROUP_HEADER_MESSAGE = "Annotation cleanups";
	public static final String UPDATE_LINE_COLUMN_IN_ANNOTATIONS = 
		"edu.kit.joana.ui.wala.easyifc.util.AnnotatinSourceCleanupConfigurationUI.tabpage.update_line_column_in_annotations";
	public static final String UPDATE_LINE_COLUMN_IN_ANNOTATIONS_MESSAGE = 
			"Update line- and column-numbers in @Source, @Sink annotations";
	public AnnotatinSourceCleanupConfigurationUI() {
		super();
	}

	@Override
	protected AbstractCleanUp[] createPreviewCleanUps(Map<String, String> values) {
		return new AbstractCleanUp[] { };
	}

	@Override
	protected void doCreatePreferences(Composite composite, int numColumns) {

		Group annotationsGroup= createGroup(numColumns, composite, GROUP_HEADER_MESSAGE);

		final CheckboxPreference annotationsPref = createCheckboxPref(
			annotationsGroup,
			numColumns,
			UPDATE_LINE_COLUMN_IN_ANNOTATIONS_MESSAGE,
			UPDATE_LINE_COLUMN_IN_ANNOTATIONS,
			new String[] { CleanUpOptions.FALSE, CleanUpOptions.TRUE }
		);
		registerPreference(annotationsPref);
		
	}
	protected Group createGroup(int numColumns, Composite parent, String text ) {
		final Group group= new Group(parent, SWT.NONE);
		group.setFont(parent.getFont());
		group.setLayoutData(createGridData(numColumns, GridData.FILL_HORIZONTAL, SWT.DEFAULT));

		final GridLayout layout= new GridLayout(numColumns, false);
		layout.verticalSpacing = 20;
		layout.horizontalSpacing = 20;
		layout.marginHeight =  20;

		group.setLayout(layout);
		group.setText(text);
		return group;
	}
	
	protected static GridData createGridData(int numColumns, int style, int widthHint) {
		final GridData gd= new GridData(style);
		gd.horizontalSpan= numColumns;
		gd.widthHint= widthHint;
		return gd;
	}
}
