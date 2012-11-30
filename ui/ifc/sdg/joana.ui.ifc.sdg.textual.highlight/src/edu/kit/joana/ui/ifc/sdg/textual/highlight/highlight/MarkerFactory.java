/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
/*
 * Created on 09.09.2005
 * @author kai brueckner
 */
package edu.kit.joana.ui.ifc.sdg.textual.highlight.highlight;

import java.awt.Point;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.Assignment;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.ForStatement;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.Modifier;
import org.eclipse.jdt.core.dom.PrimitiveType;
import org.eclipse.jdt.core.dom.ReturnStatement;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jdt.core.dom.VariableDeclarationStatement;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.IDocument;
import org.eclipse.ui.texteditor.MarkerUtilities;

import edu.kit.joana.ifc.sdg.graph.SDGNode;

public class MarkerFactory {

    static class Key implements Comparator<Key> {
        final static Key COMP = new Key();
        int line;
        int lvl;
        String file;

        public int compare(Key k1, Key k2) {
            if (k1.line != k2.line) {
                if (k1.line > k2.line) return 1;
                else return -1;
            }

            if (k1.lvl != k2.lvl) {
                if (k1.lvl > k2.lvl) return 1;
                else return -1;
            }

            if (k1.file != null && k2.file != null) {
                return k1.file.compareTo(k2.file);

            } else if (k1.file == null && k2.file == null ){
                return 0;

            } else if (k1.file == null) {
                return 1;

            } else {
                return -1;
            }
        }
    }

    class ASTTraversor extends ASTVisitor {

    	SDGNode node;
    	IDocument doc;
    	Point p;
    	ASTNode fnode;

    	@Override
    	public boolean visit(MethodInvocation n) {
    		if (p.x != -1) {
    			return false;
    		}
    		try {
				if (doc.getLineOfOffset(n.getStartPosition()) + 1 == node.getSr()) {
					String label = node.getLabel();
					if (label.contains(n.getName().getFullyQualifiedName())) {
						p.setLocation(n.getStartPosition(), n.getLength());
						return false;
					} else if (label.contains("param")) {
						int k = Integer.parseInt(label.substring(label.length() -1));
						if (k == 0) {
							k++;
						}
						ASTNode arg = (ASTNode) n.arguments().get(k - 1);
						p.setLocation(arg.getStartPosition(), arg.getLength());
					}
				}
			} catch (BadLocationException e) {
			}
    		return true;
    	}

    	@Override
    	public boolean visit(Assignment n) {
    		if (p.x != -1) {
    			return false;
    		}
    		try {
				if (doc.getLineOfOffset(n.getStartPosition()) + 1 == node.getSr()) {
					String label = node.getLabel();
					if (label.contains(n.getOperator().toString())) {
						p.setLocation(n.getStartPosition(), n.getLength());
						return false;
					}
				}
    		} catch (BadLocationException e) {
			}
    		return true;
    	}

    	@Override
    	public boolean visit(VariableDeclarationStatement n) {
    		if (p.x != -1) {
    			return false;
    		}
    		try {
				if (doc.getLineOfOffset(n.getStartPosition()) + 1 == node.getSr()) {
					if (n.getParent() instanceof ForStatement) {
						ForStatement f = (ForStatement) n.getParent();
						int l = f.getExpression().getLength();
						p.setLocation(n.getStartPosition(), n.getLength() + l);
						return false;
					}
		    		String label = node.getLabel();
		    		if (((VariableDeclarationFragment) n.fragments().get(0)).getInitializer() != null && label.contains("=")) {
		    			p.setLocation(n.getStartPosition(), n.getLength());
		    			return false;
					}
	    		}
    		} catch (BadLocationException e) {
			}
			return true;
    	}

    	@Override
    	public boolean visit(ReturnStatement n) {
    		if (p.x != -1) {
    			return false;
    		}
    		try {
				if (doc.getLineOfOffset(n.getStartPosition()) + 1 == node.getSr()) {
					String label = node.getLabel();
					if (label.contains("return")) {
						p.setLocation(n.getStartPosition(), n.getLength());
						return false;
					}
				}
    		} catch (BadLocationException e) {
			}
    		return true;
    	}

    	@Override
    	public void postVisit(ASTNode n) {
    		if (p.x == -1 && fnode == n) {
    			p.setLocation(n.getStartPosition(), n.getLength());
    		}
    	}

    	@Override
    	public void preVisit(ASTNode n) {
    		if (fnode == null && p.x == -1) {
    			try {
    				if (doc.getLineOfOffset(n.getStartPosition()) + 1 == node.getSr()
    					&& doc.getLineOfOffset(n.getStartPosition()+ n.getLength()) + 1 == node.getSr()
    					&& !(n instanceof PrimitiveType) && !(n instanceof Modifier)) {
    							fnode = n;
    				}
    			} catch (BadLocationException e) {
    			}
    		}
    	}

    	public void startTraversal(CompilationUnit unit, SDGNode node, IDocument doc, Point p) {
    		this.node = node;
    		this.doc = doc;
    		this.p = p;
    		fnode = null;
    		p.setLocation(-1, -1);
    		unit.accept(this);
    	}
    }

    private CompilationUnit ast;

	// access the text
	private static String getText(IFile file) throws CoreException, IOException {
		InputStream in = file.getContents();
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		byte[] buf = new byte[1024];
		int read = in.read(buf);
		while (read > 0) {
			out.write(buf, 0, read);
			read = in.read(buf);
		}
		return out.toString();
	}

	public int getLineOffset(IFile file, int line)
	throws CoreException, IOException, BadLocationException {
		IDocument doc = new Document(getText(file));
		return doc.getLineOffset(line);
	}

	private CompilationUnit initAST(ICompilationUnit unit) {
		ASTParser parser = ASTParser.newParser(AST.JLS3);
		parser.setKind(ASTParser.K_COMPILATION_UNIT);
		parser.setSource(unit); // set source
		parser.setResolveBindings(true);
		return (CompilationUnit) parser.createAST(null); // parse
	}

	public void createCustomMarkerAST(IProject p, edu.kit.joana.ifc.sdg.graph.SDGNode node, String type) {
		String filename = node.getSource();

		if ((filename != null) && !(node.getSr() < 0)) {
			TabulatorFilter tf = new TabulatorFilter(p);
			int line = node.getSr();
//			int lineoffset =  tf.filter(node.getSc()) - 1;
//			int length = node.getEc() - node.getSc();

			IFile resource;
			IType itype = null;
			try {
				IJavaProject jp = (IJavaProject) p.getNature(JavaCore.NATURE_ID);
				String typeName = filename.replace('/', '.').replace('$', '.');
				typeName = typeName.substring(0, typeName.indexOf(".java"));
				itype = jp.findType(typeName);
			} catch (JavaModelException e1) {
				e1.printStackTrace();
			} catch (CoreException e) {
				e.printStackTrace();
			}

			if (itype != null) {
				IPath pathToFile = itype.getPath();
				// remove the dirst segment which is the project path
				// this is not needed when IProject.getFile() is used
				pathToFile = pathToFile.removeFirstSegments(1);
				resource = p.getFile(pathToFile);
			} else {
				resource = p.getFile(filename);
			}

			if (resource.exists()) {
				IFile file = resource.getProject().getFile(
						resource.getProjectRelativePath());
//				int offset;
				Map<String, Integer> map = new HashMap<String, Integer>();
				try {

//					try {
//						offset = getLineOffset(file, line - 1) + lineoffset;
//
//						//length = 0 ... color entire line
//						if (length <= 0) {
//							offset++;
//							length = getLineOffset(file, line) - offset - 1;
//						}
//						map.put(IMarker.CHAR_START, new Integer(offset));
//						map.put(IMarker.CHAR_END, new Integer(offset + (length+1)));
//					} catch (BadLocationException e) {
//						System.out.println("unable to get offset of line: "
//								+ (line - 1) + " in file: " + file);
//						System.out.println(node.getSource()+" "+node.getLabel()+" "+node.getSr());
//					}
					ASTTraversor tra = new ASTTraversor();
					IDocument doc = new Document(getText(file));
					CompilationUnit unit = initAST(JavaCore.createCompilationUnitFrom(file));
					Point point = new Point();
					tra.startTraversal(unit, node, doc, point);
					map.put(IMarker.CHAR_START, point.x);
					map.put(IMarker.CHAR_END, point.x + point.y);

					map.put(IMarker.LINE_NUMBER, new Integer(line));
					map.put(HighlightPlugin.SDG_ID, new Integer(node.getId()));
					map.put(IMarker.SEVERITY, new Integer(
							IMarker.SEVERITY_ERROR));
					MarkerUtilities.createMarker(resource, map, type);

				} catch (CoreException e) {
					e.printStackTrace();
				} catch (IOException e) {
					e.printStackTrace();
				}
			} else {
				//throw new IllegalStateException("Could not find file " + resource);
			}

		} else {
			Map<String, Integer> map = new HashMap<String, Integer>();
			map.put(HighlightPlugin.SDG_ID, new Integer(node.getId()));
			map.put(IMarker.SEVERITY, new Integer(IMarker.SEVERITY_ERROR));
			try {
				MarkerUtilities.createMarker(p, map, type);
			} catch (CoreException e) {
				e.printStackTrace();
			}
		}
	}

	public void createCustomMarkerJC(IProject p, edu.kit.joana.ifc.sdg.graph.SDGNode node, String type) {
		String filename = node.getSource();

		if ((filename != null) && !(node.getSr() < 0)) {
			TabulatorFilter tf = new TabulatorFilter(p);
			int line = node.getSr();
			int lineoffset =  tf.filter(node.getSc()) - 1;
			int length = node.getEc() - node.getSc();

			IFile resource;
			IType itype = null;
			try {
				IJavaProject jp = (IJavaProject) p.getNature(JavaCore.NATURE_ID);
				String typeName = filename.replace('/', '.').replace('$', '.');
				typeName = typeName.substring(0, typeName.indexOf(".java"));
				itype = jp.findType(typeName);
			} catch (JavaModelException e1) {
				e1.printStackTrace();
			} catch (CoreException e) {
				e.printStackTrace();
			}

			if (itype != null) {
				IPath pathToFile = itype.getPath();
				// remove the dirst segment which is the project path
				// this is not needed when IProject.getFile() is used
				pathToFile = pathToFile.removeFirstSegments(1);
				resource = p.getFile(pathToFile);
			} else {
				resource = p.getFile(filename);
			}

			if (resource.exists()) {
				IFile file = resource.getProject().getFile(
						resource.getProjectRelativePath());
				int offset;
				Map<String, Integer> map = new HashMap<String, Integer>();
				try {

					try {
						offset = getLineOffset(file, line - 1) + lineoffset;

						//length = 0 ... color entire line
						if (length <= 0) {
							offset++;
							length = getLineOffset(file, line) - offset - 1;
						}
						map.put(IMarker.CHAR_START, new Integer(offset));
						map.put(IMarker.CHAR_END, new Integer(offset + (length+1)));
					} catch (BadLocationException e) {
						System.out.println("unable to get offset of line: "
								+ (line - 1) + " in file: " + file);
						System.out.println(node.getSource()+" "+node.getLabel()+" "+node.getSr());
					}
					map.put(IMarker.LINE_NUMBER, new Integer(line));
					map.put(HighlightPlugin.SDG_ID, new Integer(node.getId()));
					map.put(IMarker.SEVERITY, new Integer(
							IMarker.SEVERITY_ERROR));
					MarkerUtilities.createMarker(resource, map, type);

				} catch (CoreException e) {
					e.printStackTrace();
				} catch (IOException e) {
					e.printStackTrace();
				}
			} else {
				//throw new IllegalStateException("Could not find file " + resource);
			}

		} else {
			Map<String, Integer> map = new HashMap<String, Integer>();
			map.put(HighlightPlugin.SDG_ID, new Integer(node.getId()));
			map.put(IMarker.SEVERITY, new Integer(IMarker.SEVERITY_ERROR));
			try {
				MarkerUtilities.createMarker(p, map, type);
			} catch (CoreException e) {
				e.printStackTrace();
			}
		}
	}

	public void createMarkersAST(IProject p, Map<? extends SDGNode,Integer> nodes) {

		List<Line> lines = computeHighlightAST(nodes, p);
	    for (Line l : lines) {
		    mark(p, l) ;
		}
	}

	public void createMarkersJC(IProject p, Map<? extends SDGNode,Integer> nodes) {

	    List<Line> lines = computeHighlightJC(nodes, p);

	    for (Line l : lines) {
	       mark(p, l) ;
		}
	}

	private List<Line> computeHighlightAST(Map<? extends SDGNode,Integer> nodes, IProject p) {
        List<Line> lines = new LinkedList<Line>();
	    Collection<LinkedList<SDGNode>> lineBundle = bundleNodes(nodes);

	    for (LinkedList<SDGNode> bundle : lineBundle) {
	        Line l = new Line();

	        SDGNode first = bundle.poll();

	        if (first.getSource() == null) {
	        	continue;
	        }

	        String filename = first.getSource();
			IType itype = null;
	        try {
				IJavaProject jp = (IJavaProject) p.getNature(JavaCore.NATURE_ID);
				String typeName = filename.replace('/', '.').replace('$', '.');
				typeName = typeName.substring(0, typeName.indexOf(".java"));
				itype = jp.findType(typeName);
			} catch (JavaModelException e1) {
				e1.printStackTrace();
			} catch (CoreException e) {
				e.printStackTrace();
			}

			IFile resource;

			if (itype != null) {
				IPath pathToFile = itype.getPath();
				// remove the dirst segment which is the project path
				// this is not needed when IProject.getFile() is used
				pathToFile = pathToFile.removeFirstSegments(1);
				resource = p.getFile(pathToFile);
			} else {
				resource = p.getFile(filename);
			}

			if (!resource.exists()) {
				continue;
			}


			IFile file = resource.getProject().getFile(
						resource.getProjectRelativePath());

			ASTTraversor tra = new ASTTraversor();
			IDocument doc = new Document();
			Point point = new Point();
			try {
				doc = new Document(getText(file));
			} catch (CoreException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
			CompilationUnit unit = initAST(JavaCore.createCompilationUnitFrom(file));

	        l.setFileName(first.getSource());
	        l.setRowStart(first.getSr());
	        l.setType(nodes.get(first));
	        int cs;

			tra.startTraversal(unit, first, doc, point);
	        cs = -1;
	        try {
				cs = point.x - doc.getLineOffset(first.getSr() - 1) + 1;
			} catch (BadLocationException e) {
				e.printStackTrace();
			}
	        l.setColumnStart(cs);
	        l.setColumnEnd(cs + point.y - 1);

	        for (SDGNode n : bundle) {
	            l.setRowStart(n.getSr());

	            tra.startTraversal(unit, n, doc, point);
	            cs = -1;
	            try {
					cs = point.x - doc.getLineOffset(first.getSr() - 1) + 1;
				} catch (BadLocationException e) {
					e.printStackTrace();
				}
	            l.setColumnStart(cs);
	            l.setColumnEnd(cs + point.y - 1);
	        }

	        lines.add(l);
	    }

	    return lines;
	}

	private List<Line> computeHighlightJC(Map<? extends SDGNode,Integer> nodes, IProject p) {
        List<Line> lines = new LinkedList<Line>();
	    Collection<LinkedList<SDGNode>> lineBundle = bundleNodes(nodes);

	    for (LinkedList<SDGNode> bundle : lineBundle) {
	        Line l = new Line();

	        SDGNode first = bundle.poll();
	        if (first.getSource() == null) {
	        	continue;
	        }

	        l.setFileName(first.getSource());
	        l.setRowStart(first.getSr());
	        l.setType(nodes.get(first));

        	l.setColumnStart(first.getSc());
	        l.setColumnEnd(first.getEc());


	        for (SDGNode n : bundle) {
	            l.setRowStart(n.getSr());

            	l.setColumnStart(n.getSc());
	            l.setColumnEnd(n.getEc());
	        }

	        lines.add(l);
	    }

	    return lines;
	}

	private Collection<LinkedList<SDGNode>> bundleNodes(Map<? extends SDGNode,Integer> nodes) {
	    Map<Key, LinkedList<SDGNode>> lineBundle = new TreeMap<Key, LinkedList<SDGNode>>(Key.COMP);

	    for (SDGNode n : nodes.keySet()) {
	        Key k = new Key();
	        k.line = n.getSr();
	        k.file = n.getSource();
	        k.lvl = nodes.get(n);
	        LinkedList<SDGNode> l = lineBundle.get(k);

	        if (l == null) {
	            l = new LinkedList<SDGNode>();
	            lineBundle.put(k, l);
	        }

	        l.add(n);
	    }

	    return lineBundle.values();
	}

	private void mark(IProject p, Line l) {
        String filename = l.getFileName();//node.getSource();

        if ((filename != null) && !(l.getRowStart() < 0)) {//(node.getSr() < 0)) {
            TabulatorFilter tf = new TabulatorFilter(p);
            int line = l.getRowStart();////node.getSr();
            int lineoffset = tf.filter(l.getColumnStart()) - 1; //tf.filter(node.getSc()) - 1;
            int length = l.getColumnEnd() - l.getColumnStart();//node.getEc() - node.getSc();

            IFile resource;
            IType itype = null;
            try {
                IJavaProject jp = (IJavaProject) p.getNature(JavaCore.NATURE_ID);
                String typeName = filename.replace('/', '.').replace('$', '.');
                typeName = typeName.substring(0, typeName.indexOf(".java"));
                itype = jp.findType(typeName);
            } catch (JavaModelException e1) {
                e1.printStackTrace();
            } catch (CoreException e) {
                e.printStackTrace();
            }

            if (itype != null) {
                IPath pathToFile = itype.getPath();
                // remove the dirst segment which is the project path
                // this is not needed when IProject.getFile() is used
                pathToFile = pathToFile.removeFirstSegments(1);
                resource = p.getFile(pathToFile);
            } else {
                resource = p.getFile(filename);
            }

            if (resource.exists()) {
                IFile file = resource.getProject().getFile(
                        resource.getProjectRelativePath());
                int offset;
                Map<String, Integer> map = new HashMap<String, Integer>();
                try {
                    try {
                        offset = getLineOffset(file, line - 1) + lineoffset;

                        //length = 0 ... color entire line
                        if (length <= 0) {
                            offset++;
                            length = getLineOffset(file, line) - offset - 1;
                        }
                        map.put(IMarker.CHAR_START, new Integer(offset));
                        map.put(IMarker.CHAR_END, new Integer(offset + (length+1)));
                    } catch (BadLocationException e) {
                        System.out.println("unable to get offset of line: "
                                + (line - 1) + " in file: " + file);
                        //System.out.println(node.getSource()+" "+node.getLabel()+" "+node.getSr());
                        System.out.println(l);
                    }

                    map.put(IMarker.LINE_NUMBER, new Integer(line));
                    //map.put(HighlightPlugin.SDG_ID, new Integer(node.getId()));
                    map.put(IMarker.SEVERITY, new Integer(
                            IMarker.SEVERITY_ERROR));

                    MarkerUtilities.createMarker(resource, map, HighlightPlugin.MARKER_ID+"level"+l.getType());

                } catch (CoreException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } else {
                //throw new IllegalStateException("Could not find file " + resource);
            }

        } /*else {
            Map<String, Integer> map = new HashMap<String, Integer>();
            //map.put(HighlightPlugin.SDG_ID, new Integer(node.getId()));
            map.put(IMarker.SEVERITY, new Integer(IMarker.SEVERITY_ERROR));
            try {
                MarkerUtilities.createMarker(p, map, type);
            } catch (CoreException e) {
                e.printStackTrace();
            }
        }*/
    }
}
