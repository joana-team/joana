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
import org.eclipse.jdt.internal.core.SourceMethod;
import org.eclipse.jdt.internal.core.SourceType;

import edu.kit.joana.api.sdg.SDGFormalParameter;
import edu.kit.joana.api.sdg.SDGMethod;
import edu.kit.joana.ifc.sdg.util.JavaType;
import edu.kit.joana.ui.wala.easyifc.model.IFCCheckResultConsumer.IFCResult;
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
	private final SourceMethod entryMethod;
	private final Map<String, ICompilationUnit> file2cu = new HashMap<String, ICompilationUnit>();

	private SearchHelper(final IJavaProject jp, final SourceMethod entryMethod) {
		this.jp = jp;
		this.entryMethod = entryMethod;
	}
	
	public static SearchHelper create(final IJavaProject jp, final IFCResult ifcres) {
		final IJavaSearchScope scope = SearchEngine.createJavaSearchScope(new IJavaElement[] {jp});
		
		final String mpat = searchPattern(ifcres);
		System.out.println("Searching for " + mpat);
		final SearchPattern pat = SearchPattern.createPattern(mpat,
				IJavaSearchConstants.METHOD, IJavaSearchConstants.IMPLEMENTORS, SearchPattern.R_EXACT_MATCH | SearchPattern.R_CASE_SENSITIVE | SearchPattern.R_FULL_MATCH);
		final SearchEngine eng = new SearchEngine();
		final SearchParticipant[] defaultParts =
				new SearchParticipant[] {SearchEngine.getDefaultSearchParticipant()};
		final Requestor req = new Requestor(ifcres);
		try {
			eng.search(pat, defaultParts, scope, req, null);
		} catch (CoreException e) {
			e.printStackTrace();
		}
	
		if (req.methodFound()) {
			return new SearchHelper(jp, req.getMethod());
		} else {
			return null;
		}
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
	
	public SourceMethod getMethod() {
		return entryMethod;
	}

	private static String searchPattern(final IFCResult ifcres) {
		final String m = ifcres.getMainMethod();

		final String clsName = m.substring(0, m.lastIndexOf('.')).replace('$', '.');
		final String methodName = m.substring(m.lastIndexOf('.') + 1, m.indexOf('('));
		
		return clsName + "." + methodName;
	}

	private static final class Requestor extends SearchRequestor {

		private final IFCResult result;
		private SourceMethod found = null;

		private Requestor(final IFCResult nfo) {
			this.result = nfo;
		}

		private boolean isBetterResult(final SourceMethod sm) {
			final String[] newParams = sm.getParameterTypes();
			final String[] oldParams = found.getParameterTypes();
			final SDGMethod im = result.getMainIMethod();

			final int numParam;
			if (im.parameterIndexValid(0)) {
				// dynamic method with this pointer
				numParam = im.getParameters().size() - 1;
			} else {
				numParam = im.getParameters().size();
			}
			
			if (numParam != newParams.length) {
				return false;
			}
			
			if (newParams.length != oldParams.length) {
				return true;
			}
			
			int newMatch = 0;
			int oldMatch = 0;

			for (int i = 0; i < sm.getNumberOfParameters(); i++) {
				
				final SDGFormalParameter tref = im.getParameter(i + 1);
				if (isMatching(tref,oldParams[i])) {
					oldMatch++;
				}
				if (isMatching(tref,newParams[i])) {
					newMatch++;
				}
			}

			if (newMatch != oldMatch) {
				return (newMatch > oldMatch);
			}

			return false;
		}

		private static boolean isMatching(final SDGFormalParameter tref, final String eType) {
			final JavaType jt = tref.getType();
			jt.toBCString();
			
			//TODO
			
			return true;
		}
		
		@Override
		public void acceptSearchMatch(final SearchMatch match) throws CoreException {
			if (match.getElement() instanceof SourceMethod) {
				final SourceMethod sm = (SourceMethod) match.getElement();
				final SDGMethod im = result.getMainIMethod();
				final int paramCount = 
						im.instructionIndexValid(0) ? im.getParameters().size() : im.getParameters().size() - 1;
						
				final String name = im.getSignature().getMethodName(); 
				if (sm.getElementName().equals(name) && sm.getParameterNames().length == paramCount) {
					if (found == null || isBetterResult(sm)) {
						found = sm;
					}
				}
			}
		}

		public SourceMethod getMethod() {
			return found;
		}

		public boolean methodFound() {
			return found != null;
		}
	}

}
