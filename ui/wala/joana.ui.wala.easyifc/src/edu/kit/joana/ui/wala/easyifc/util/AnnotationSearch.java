package edu.kit.joana.ui.wala.easyifc.util;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.jdt.core.IAnnotatable;
import org.eclipse.jdt.core.IAnnotation;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.search.IJavaSearchConstants;
import org.eclipse.jdt.core.search.IJavaSearchScope;
import org.eclipse.jdt.core.search.SearchDocument;
import org.eclipse.jdt.core.search.SearchEngine;
import org.eclipse.jdt.core.search.SearchMatch;
import org.eclipse.jdt.core.search.SearchParticipant;
import org.eclipse.jdt.core.search.SearchPattern;
import org.eclipse.jdt.core.search.SearchRequestor;
import org.eclipse.jdt.core.search.TypeReferenceMatch;
import org.eclipse.jdt.internal.core.search.JavaSearchParticipant;
import org.eclipse.jdt.internal.core.search.matching.MatchLocator.WorkingCopyDocument;

@SuppressWarnings("restriction")
public class AnnotationSearch {
	
	public static <T extends IAnnotatable> Map<IAnnotation,T> findAnnotationsInContainer(
	IJavaElement element,
	Class<T> annotationTarget,
	Class<?> annotationType,
	AnnotationSearchRequestor<T> requestor,
	IProgressMonitor pm,
	boolean ignoreWorkingCopies) throws CoreException {
		if (element == null) throw new IllegalArgumentException();

		final IJavaSearchScope scope = SearchEngine.createJavaSearchScope(new IJavaElement[] {element}, IJavaSearchScope.SOURCES);
		return findAnnotationsInContainer(annotationTarget, annotationType, requestor, pm, scope, ignoreWorkingCopies);
	}
	public static <T extends IAnnotatable> Map<IAnnotation,T> findAnnotationsInContainer(
	Class<T> annotationTarget,
	Class<?> annotationType,
	AnnotationSearchRequestor<T> requestor,
	IProgressMonitor pm,
	IJavaSearchScope scope,
	boolean ignoreWorkingCopies) throws CoreException {
		
		if (annotationType == null || !annotationType.isAnnotation()) throw new IllegalArgumentException();
		if (pm == null) pm = new NullProgressMonitor();

		try {
			pm.beginTask("Finding Java Annotations..", 4);
			final int matchRule = SearchPattern.R_EXACT_MATCH | SearchPattern.R_CASE_SENSITIVE;
			final SearchPattern annotationsPattern = SearchPattern.createPattern(annotationType.getCanonicalName(), IJavaSearchConstants.ANNOTATION_TYPE, IJavaSearchConstants.ANNOTATION_TYPE_REFERENCE, matchRule);
			final SearchParticipant[] searchParticipants;
			if (ignoreWorkingCopies) {
				searchParticipants = new SearchParticipant[] { 
					new JavaSearchParticipant() {
						public void locateMatches(
							SearchDocument[] indexMatches,
							SearchPattern pattern,
							IJavaSearchScope scope,
							SearchRequestor requestor,
							IProgressMonitor monitor) throws CoreException {
							SearchDocument[] noWorkingCopies = 
									Arrays.stream(indexMatches).filter(document -> !(document instanceof WorkingCopyDocument)).toArray(SearchDocument[]::new);
							super.locateMatches(noWorkingCopies, annotationsPattern, scope, requestor, monitor);
						};
					}
					
				};
			} else {
				searchParticipants = new SearchParticipant[] { SearchEngine.getDefaultSearchParticipant() }; 
			};
			new SearchEngine().search(annotationsPattern, searchParticipants, scope, requestor, new SubProgressMonitor(pm, 2));

		} finally {
			pm.done();
		}
		return requestor.getResults();
	}
	
	public abstract static class AnnotationSearchRequestor<T extends IAnnotatable> extends SearchRequestor {

		protected abstract boolean accept(T t);
		
		private final Class<T> clazz;
		private final Map<IAnnotation,T> results = new HashMap<IAnnotation, T>();
		
		protected AnnotationSearchRequestor(Class<T> clazz) {
			this.clazz = clazz;
		}				

		
		@Override
		public void acceptSearchMatch(SearchMatch match) throws JavaModelException {
			if (match.getAccuracy() == SearchMatch.A_ACCURATE && !match.isInsideDocComment()) {
				// The following three casts ought to never throw due to the search parameters
				final TypeReferenceMatch typeReference = (TypeReferenceMatch) match;
				final IAnnotatable element = (IAnnotatable) typeReference.getElement();
				final IAnnotation annotation = (IAnnotation) typeReference.getLocalElement();
				
				assert element.getAnnotations()[annotation.getOccurrenceCount()-1] == annotation;
				if (clazz.isInstance(element) && accept(clazz.cast(element))) {
					results.put(annotation, clazz.cast(element));
				}
			}
		}
		
		public Map<IAnnotation,T> getResults() {
			return results;
		}
	}

}
