/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.ui.wala.easyifc.model;

import java.util.Collection;
import java.util.Collections;
import java.util.Set;
import java.util.TreeSet;

public class FileSourcePositions implements Comparable<FileSourcePositions> {

	private final String filename;
	private Set<SourcePosition> positions = new TreeSet<SourcePosition>();

	public FileSourcePositions(final String fileName) {
		this.filename = fileName;
	}

	public void addPosition(final int startLine, final int endLine, final int startRow, final int endRow) {
		addPosition(new SourcePosition(startLine, endLine, startRow, endRow));
	}

	public void addPosition(final SourcePosition spos) {
		positions.add(spos);
	}

	public Collection<SourcePosition> getPositions() {
		return Collections.unmodifiableCollection(positions);
	}

	public String getFilename() {
		return filename;
	}

	@Override
	public int compareTo(FileSourcePositions o) {
		return filename.compareTo(o.filename);
	}

	public String toString() {
		final StringBuffer sb = new StringBuffer();
		for (final SourcePosition sp : getPositions()) {
			sb.append(filename + ":" + sp.toString() + "\n");
		}

		if (sb.length() > 0) {
			sb.deleteCharAt(sb.length() - 1);
		}

		return sb.toString();
	}

}
