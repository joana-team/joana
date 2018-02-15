/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.util;

import java.util.HashMap;
import java.util.Map;

public class SourceLocation {

	public static final SourceLocation UNKNOWN = new SourceLocation(null, 0, 0, 0, 0);
	/**
	 * Filename of the class. e.g. "mypackage/subpackage/MyClass.java"
	 */
	private final String sourceFileName;
	private final int startLine;
	private final int startChar;
	private final int endLine;
	private final int endChar;
	
	private static final Map<SourceLocation, SourceLocation> sourceLocationPool  = new HashMap<>();
	public static void clearSourceLocationPool() {
		sourceLocationPool.clear();
		sourceLocationPool.put(UNKNOWN, UNKNOWN);
	}

	private SourceLocation(String sourceFileName, int startLine, int startChar, int endLine, int endChar) {
		if (startLine < 0 || startChar < 0 || endLine < 0 || endChar < 0) {
			throw new IllegalArgumentException("SourceLocations have to be > 0: ("
					+ startLine + "," + startChar + ") - (" + endLine + ","
					+ endChar + ")");
		}

		this.sourceFileName = sourceFileName;
		this.startLine = startLine;
		this.startChar = startChar;
		this.endLine = endLine;
		this.endChar = endChar;
	}

	public static SourceLocation getLocation(String sourceFileName, int startLine, int startChar, int endLine, int endChar) {
		final SourceLocation sourceLocation = new SourceLocation(sourceFileName, startLine, startChar, endLine, endChar);
		final SourceLocation pooled = sourceLocationPool.get(sourceLocation);
		if (pooled != null) {
			return pooled;
		} else {
			sourceLocationPool.put(sourceLocation, sourceLocation);
			return sourceLocation;
		}
	}

	public String toString() {
		StringBuilder str = new StringBuilder("\"");
		str.append(sourceFileName);
		str.append("\":");
		str.append(startLine);
		str.append(',');
		str.append(startChar);
		str.append('-');
		str.append(endLine);
		str.append(',');
		str.append(endChar);

		return str.toString();
	}

	public String getSourceFile() {
	    return sourceFileName;
	}

	public int getStartRow() {
	    return startLine;
	}

    public int getStartColumn() {
        return startChar;
    }

    public int getEndRow() {
        return endLine;
    }

    public int getEndColumn() {
        return endChar;
    }

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + endChar;
		result = prime * result + endLine;
		result = prime * result + ((sourceFileName == null) ? 0 : sourceFileName.hashCode());
		result = prime * result + startChar;
		result = prime * result + startLine;
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		SourceLocation other = (SourceLocation) obj;
		if (endChar != other.endChar)
			return false;
		if (endLine != other.endLine)
			return false;
		if (sourceFileName == null) {
			if (other.sourceFileName != null)
				return false;
		} else if (!sourceFileName.equals(other.sourceFileName))
			return false;
		if (startChar != other.startChar)
			return false;
		if (startLine != other.startLine)
			return false;
		return true;
	}
    
}
