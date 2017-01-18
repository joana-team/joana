package edu.kit.joana.ui.wala.easyifc.util;

import java.util.Map;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.core.IAnnotatable;
import org.eclipse.jdt.core.IAnnotation;
import org.eclipse.jdt.core.search.IJavaSearchScope;

import edu.kit.joana.ui.wala.easyifc.util.AnnotationSearch.AnnotationSearchRequestor;

public class AnnotationProcessing {

	public static Map<IAnnotation, IAnnotatable> searchAnnotatedMethods(
			IProgressMonitor pm,
			IJavaSearchScope scope,
			Class<?> annotationType
	) throws CoreException {
		final AnnotationSearchRequestor<IAnnotatable> requestor = new AnnotationSearchRequestor<IAnnotatable>(IAnnotatable.class) {
			@Override
			protected boolean accept(IAnnotatable t) {
				return true;
			}
		};
		return AnnotationSearch.findAnnotationsInContainer(IAnnotatable.class, annotationType, requestor, pm, scope, true);
	}
}
