/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.ui.wala.easyifc.util;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.search.IJavaSearchConstants;
import org.eclipse.jdt.core.search.IJavaSearchScope;
import org.eclipse.jdt.core.search.SearchEngine;
import org.eclipse.jdt.core.search.SearchMatch;
import org.eclipse.jdt.core.search.SearchParticipant;
import org.eclipse.jdt.core.search.SearchPattern;
import org.eclipse.jdt.core.search.SearchRequestor;
import org.eclipse.jdt.internal.core.SourceType;

import edu.kit.joana.ui.wala.easyifc.model.IFCCheckResultConsumer.SPos;
import edu.kit.joana.ui.wala.easyifc.util.EasyIFCMarkerAndImageManager.Marker;

/**
 *
 * @author Juergen Graf <juergen.graf@gmail.com>
 *
 */
@SuppressWarnings("restriction")
public class SearchHelper {

	private final IJavaProject jp;
	private final Map<String, ICompilationUnit> file2cu = new HashMap<String, ICompilationUnit>();

	private SearchHelper(final IJavaProject jp) {
		this.jp = jp;
	}
	
	public static SearchHelper create(final IJavaProject jp) {
		return new SearchHelper(jp);
	}

	public ICompilationUnit getCompilationUnit(final String fileName) {
		if (file2cu.containsKey(fileName)) {
			return file2cu.get(fileName);
		}
		
		final IJavaSearchScope scope = SearchEngine.createJavaSearchScope(new IJavaElement[] {jp});
		final String classOfFile = fileName.substring(0, fileName.lastIndexOf('.')).replace('/', '.').replace('$', '.');
		
		final SearchPattern pat = SearchPattern.createPattern(classOfFile,
				IJavaSearchConstants.TYPE, IJavaSearchConstants.DECLARATIONS, SearchPattern.R_EXACT_MATCH | SearchPattern.R_CASE_SENSITIVE | SearchPattern.R_FULL_MATCH);
		final SearchEngine eng = new SearchEngine();
		final SearchParticipant[] defaultParts =
				new SearchParticipant[] {SearchEngine.getDefaultSearchParticipant()};
		final LinkedList<SourceType> found = new LinkedList<SourceType>();
		final SearchRequestor req = new SearchRequestor() {
			@Override
			public void acceptSearchMatch(final SearchMatch match) throws CoreException {
				if (match.getElement() instanceof SourceType) {
					final SourceType t = (SourceType) match.getElement();
					if (classOfFile.equals(t.getFullyQualifiedName('.'))) {
						found.add(t);
					}
				}
			}
		};
		
		try {
			eng.search(pat, defaultParts, scope, req, null);
		} catch (CoreException e) {
			e.printStackTrace();
		}

		if (!found.isEmpty()) {
			final SourceType t =  found.element();
			final ICompilationUnit cu = t.getCompilationUnit();
			
			file2cu.put(fileName, cu);
			
			return cu;
		} else {
			file2cu.put(fileName, null);
			
			return null;
		}
	}

	public IMarker createSideMarker(final SPos spos, final String message, final Marker marker) {
		final ICompilationUnit cu = getCompilationUnit(spos.sourceFile);
		
		if (cu != null) {
			final IResource res = cu.getResource();

			return EasyIFCMarkerAndImageManager.getInstance().createMarker(res, message, spos.startLine, marker);
		} else {
			return null;
		}
	}
}
