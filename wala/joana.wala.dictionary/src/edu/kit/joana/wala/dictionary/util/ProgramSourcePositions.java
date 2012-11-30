/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.wala.dictionary.util;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeSet;

public class ProgramSourcePositions {

	private final Map<String, FileSourcePositions> map = new HashMap<String, FileSourcePositions>();

	public void addSourcePosition(final String filename, final int startLine, final int endLine, final int startRow,
			final int endRow) {
		final FileSourcePositions fpos = getFileSourcePositions(filename);
		fpos.addPosition(startLine, endLine, startRow, endRow);
	}

	public void addSourcePosition(final String filename, final SourcePosition spos) {
		final FileSourcePositions fpos = getFileSourcePositions(filename);
		fpos.addPosition(spos);
	}

	public FileSourcePositions getFileSourcePositions(final String filename) {
		FileSourcePositions fpos = null;
		if (!map.containsKey(filename)) {
			fpos = new FileSourcePositions(filename);
			map.put(filename, fpos);
		} else {
			fpos = map.get(filename);
		}

		return fpos;
	}

	public boolean hasFileSourcePositions(final String filename) {
		return map.containsKey(filename);
	}

	public Collection<FileSourcePositions> getFileSourcePositions() {
		return Collections.unmodifiableCollection(new TreeSet<FileSourcePositions>(map.values()));
	}

	public String toString() {
		final StringBuffer sb = new StringBuffer();

		for (final FileSourcePositions fpos : getFileSourcePositions()) {
			sb.append(fpos + "\n");
		}

		if (sb.length() > 0) {
			sb.deleteCharAt(sb.length() - 1);
		}

		return sb.toString();
	}

}
