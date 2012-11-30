/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.deprecated.jsdg.wala;

public class SourceLocation {

	/**
	 * Filename of the class. e.g. "mypackage/subpackage/MyClass.java"
	 */
	private final String sourceFileName;
	private final int startLine;
	private final int startChar;
	private final int endLine;
	private final int endChar;

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
		return new SourceLocation(sourceFileName, startLine, startChar, endLine, endChar);
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
}
