/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
/*
 * Created on 21.05.2006
 * @author kai brueckner
 */
package edu.kit.joana.ui.ifc.sdg.textual.highlight.highlight;

import org.eclipse.core.resources.IProject;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.internal.corext.util.CodeFormatterUtil;

@SuppressWarnings("restriction")
public class TabulatorFilter {

	private IProject p;
	static int compilerTabWidth = 4;

	public TabulatorFilter(IProject project) {
		this.p = project;
	}

	/* TODO: Implementieren!*/
	public int filter(int c) {
		return c;
	}

	public void printTabWidth() {
		System.out.println(getTabWidth());
	}

	private int getTabWidth() {
		IJavaProject jp = JavaCore.create(p);
		return CodeFormatterUtil.getTabWidth(jp);
	}
}
