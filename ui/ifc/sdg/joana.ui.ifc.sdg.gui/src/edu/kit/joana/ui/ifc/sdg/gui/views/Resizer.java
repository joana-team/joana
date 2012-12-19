/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.ui.ifc.sdg.gui.views;

import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;

public final class Resizer {
	private Resizer() { }

	public static final void resize(Table t, int[] offset) {
		int width = t.getClientArea().width;
		if (width <= 1) return;

		TableColumn[] cols = t.getColumns();
		int size = cols.length;
		int[] widths = new int[size];

		// measure the needed sizes
		{
			GC gc = new GC(t);

			// largest text per column
			for (int column = 0; column < size; column++) {

				// initialize with the column headline size
				Point measurement = gc.textExtent(cols[column].getText());
				int columnWidth = measurement.x;

				// iterate through the rows
				for (TableItem tableItem : t.getItems()) {
					measurement = gc.textExtent(tableItem.getText(column));
					columnWidth = Math.max(columnWidth, measurement.x);
				}

				widths[column] = columnWidth + offset[column];
			}

			gc.dispose();
		}

		for (int i = 0; i < size; i++) {
			if (cols[i].getWidth() < widths[i]) {
				cols[i].setWidth(widths[i]);
			}
		}
	}
}
