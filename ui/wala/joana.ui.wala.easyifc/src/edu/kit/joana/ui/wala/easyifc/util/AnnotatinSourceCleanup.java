package edu.kit.joana.ui.wala.easyifc.util;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.core.IAnnotatable;
import org.eclipse.jdt.core.IAnnotation;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.Annotation;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.MarkerAnnotation;
import org.eclipse.jdt.core.dom.MemberValuePair;
import org.eclipse.jdt.core.dom.Name;
import org.eclipse.jdt.core.dom.NodeFinder;
import org.eclipse.jdt.core.dom.NormalAnnotation;
import org.eclipse.jdt.core.dom.NumberLiteral;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jdt.core.dom.rewrite.ListRewrite;
import org.eclipse.jdt.core.refactoring.CompilationUnitChange;
import org.eclipse.jdt.core.search.IJavaSearchScope;
import org.eclipse.jdt.core.search.SearchEngine;
import org.eclipse.jdt.ui.cleanup.CleanUpContext;
import org.eclipse.jdt.ui.cleanup.CleanUpOptions;
import org.eclipse.jdt.ui.cleanup.CleanUpRequirements;
import org.eclipse.jdt.ui.cleanup.ICleanUp;
import org.eclipse.jdt.ui.cleanup.ICleanUpFix;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.text.edits.TextEdit;

import com.google.common.collect.Sets;

import edu.kit.joana.ui.annotations.Sink;
import edu.kit.joana.ui.annotations.Source;

public class AnnotatinSourceCleanup implements ICleanUp {

	@Override
	public void setOptions(CleanUpOptions options) {
		// TODO Auto-generated method stub
	}

	@Override
	public String[] getStepDescriptions() {
		return null;
	}

	@Override
	public CleanUpRequirements getRequirements() {
		return new CleanUpRequirements(true, true, true, null);
	}

	@Override
	public RefactoringStatus checkPreConditions(IJavaProject project, ICompilationUnit[] compilationUnits,
			IProgressMonitor monitor) throws CoreException {
		return new RefactoringStatus();
	}

	@Override
	public ICleanUpFix createFix(final CleanUpContext context) throws CoreException {
		return new ICleanUpFix() {
			@Override
			public CompilationUnitChange createChange(IProgressMonitor progressMonitor) throws CoreException {
				
				final ICompilationUnit cu = context.getCompilationUnit();
				
				final ASTParser parser = ASTParser.newParser(AST.JLS8);
				parser.setSource(cu);
				parser.setResolveBindings(true);
				CompilationUnit astRoot = (CompilationUnit) parser.createAST(null);
				
				final AST ast = astRoot.getAST();
				final ASTRewrite rewrite = ASTRewrite.create(ast);
				
				final IJavaSearchScope scope = SearchEngine.createJavaSearchScope(
						new IJavaElement[] { cu },
						IJavaSearchScope.SOURCES
				);

				final Set<Map.Entry<IAnnotation, IAnnotatable>> sourcesAndSinks = Sets.union( 
						AnnotationProcessing.searchAnnotatedMethods(null, scope, Source.class).entrySet(),
						AnnotationProcessing.searchAnnotatedMethods(null, scope, Sink.class).entrySet()
				);
				
				Map<IAnnotation, ASTNode> nodes = new HashMap<>();
				for (Map.Entry<IAnnotation, IAnnotatable> e : sourcesAndSinks) {
					final IAnnotation annotation = e.getKey();
					nodes.put(annotation, NodeFinder.perform(astRoot, annotation.getSourceRange()));
				}
				
				// TODO: Since changes in the annotation fields may invalidate columnNumbers whenever
				// two annotations may appear in the same line, , we would need to fixpoint-iterate
				// this in order to be 100% correct. This would, however, be really fugly, since
				// we're working on an ASTRewrite here, not the AST itself.
				for (Map.Entry<IAnnotation, IAnnotatable> e : sourcesAndSinks) {
					final IAnnotation annotation = e.getKey();
					final Annotation node = (Annotation) nodes.get(annotation);

					node.accept(new ASTVisitor() {
						@SuppressWarnings("unchecked")
						@Override
						public boolean visit(MarkerAnnotation node) {
							NormalAnnotation newNormal = ast.newNormalAnnotation();
							
							newNormal.setTypeName((Name) ASTNode.copySubtree(ast, node.getTypeName()));
							
							{
								MemberValuePair lineNumber = ast.newMemberValuePair();
								lineNumber.setName(ast.newSimpleName("lineNumber"));
								{
									NumberLiteral lineValue = ast.newNumberLiteral();
									lineValue.setToken(Integer.toString(astRoot.getLineNumber(node.getStartPosition())));
									lineNumber.setValue(lineValue);
								}
								newNormal.values().add(lineNumber);
							}
							{
								MemberValuePair columnNumber = ast.newMemberValuePair();
								columnNumber.setName(ast.newSimpleName("columnNumber"));
								{
									NumberLiteral columnValue = ast.newNumberLiteral();
									columnValue.setToken(Integer.toString(astRoot.getColumnNumber(node.getStartPosition())));
									columnNumber.setValue(columnValue);
								}
								newNormal.values().add(columnNumber);
							}
							rewrite.replace(node, newNormal, null);
							return false;
						}
						
						@Override
						public boolean visit(NormalAnnotation node) {
							boolean lineNumberReplaced = false;
							boolean columnNumberReplaced = false;
							
							NumberLiteral lineValue = ast.newNumberLiteral();
							lineValue.setToken(Integer.toString(astRoot.getLineNumber(node.getStartPosition())));
							
							NumberLiteral columnValue = ast.newNumberLiteral();
							columnValue.setToken(Integer.toString(astRoot.getColumnNumber(node.getStartPosition())));
							
							for (Object o : node.values()) {
								MemberValuePair mvp = (MemberValuePair) o;
								if ("lineNumber".equals(mvp.getName().toString())) {
									rewrite.replace(mvp.getValue(), lineValue, null);
									lineNumberReplaced = true;
								}
								if ("columnNumber".equals(mvp.getName().toString())) {
									rewrite.replace(mvp.getValue(), columnValue, null);
									columnNumberReplaced = true;
								}
							}
							if (!lineNumberReplaced || !columnNumberReplaced) {
								ListRewrite listRewrite = rewrite.getListRewrite(node, NormalAnnotation.VALUES_PROPERTY);
								if (!lineNumberReplaced) {
									MemberValuePair lineNumber = ast.newMemberValuePair();
									lineNumber.setName(ast.newSimpleName("lineNumber"));
									lineNumber.setValue(lineValue);
									listRewrite.insertLast(lineNumber, null);
								}
								if (!columnNumberReplaced) {
									MemberValuePair columnNumber = ast.newMemberValuePair();
									columnNumber.setName(ast.newSimpleName("columnNumber"));
									columnNumber.setValue(columnValue);
									listRewrite.insertLast(columnNumber, null);
								}
							}
							return false;
						}
					});
				}
				
				final CompilationUnitChange compilationUnitChange = new CompilationUnitChange(
					"Update Linenumbers in IFC AnnotationsMake field Final",
					cu
				);
				final TextEdit edit = rewrite.rewriteAST();
				compilationUnitChange.setEdit(edit);
				return compilationUnitChange;
			}
		};
	}

	@Override
	public RefactoringStatus checkPostConditions(IProgressMonitor monitor) throws CoreException {
		return new RefactoringStatus();
	}

}
