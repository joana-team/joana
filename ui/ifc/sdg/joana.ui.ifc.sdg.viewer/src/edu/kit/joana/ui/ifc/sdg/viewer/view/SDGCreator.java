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
package edu.kit.joana.ui.ifc.sdg.viewer.view;
//package edu.kit.joana.ui.ifc.sdg.viewer.view;
//
//import java.io.IOException;
//
//import edu.kit.joana.ui.ifc.sdg.textual.highlight.highlight.sdgcreator.SDGConfiguration;
//import edu.kit.joana.ui.ifc.sdg.textual.highlight.highlight.sdgcreator.SDGCreationException;
//import edu.kit.joana.ui.ifc.sdg.viewer.model.Graph;
//import edu.kit.joana.ui.ifc.sdg.viewer.model.GraphFactory;
//import edu.kit.joana.ui.ifc.sdg.viewer.view.analysisview.AnalysisView;
//
//import org.eclipse.core.runtime.CoreException;
//import org.eclipse.jdt.internal.core.CompilationUnit;
//import org.eclipse.jface.dialogs.MessageDialog;
//import org.eclipse.swt.SWT;
//import org.eclipse.swt.events.SelectionAdapter;
//import org.eclipse.swt.events.SelectionEvent;
//import org.eclipse.swt.layout.FormAttachment;
//import org.eclipse.swt.layout.FormData;
//import org.eclipse.swt.layout.FormLayout;
//import org.eclipse.swt.layout.GridData;
//import org.eclipse.swt.layout.GridLayout;
//import org.eclipse.swt.widgets.Button;
//import org.eclipse.swt.widgets.Composite;
//import org.eclipse.swt.widgets.DirectoryDialog;
//import org.eclipse.swt.widgets.FileDialog;
//import org.eclipse.swt.widgets.Label;
//import org.eclipse.swt.widgets.Shell;
//import org.eclipse.swt.widgets.Text;
//
//
///** This Class is used to configure and run the Joana analysis for Java.
// * It opens a Composite window where the user can enter specific configurations.
// * When done it calls class Graph to compute a new SDG and adds the SDG to the AnalysisView.
// */
//public class SDGCreator extends org.eclipse.swt.widgets.Composite {
//	// the Java file that shall be analyzed
//	private CompilationUnit javafile;
//	private SDGConfiguration conf;
//
//	// the widgets
//	private Button createSDG;
//	private Button fineGrained;
//	private Text sdgPath;
//	private Label sdgLabel;
//	private Button stubsBrowser;
//	private Button stubsBrowser2;
//	private Button cancel;
//	private Text stubsPath;
//	private Label stubsLabel;
//	private Button interference;
//	private Button sdgBrowser;
//	private Button sdgBrowser2;
//	private Button controlFlow;
//	private Button summaryEdges;
//	private Button wholeProgram;
//	private Text memoryTextField;
//	private Label memoryLabel;
//	private Button javahomeButton;
//	private Label javahomeLabel;
//	private Text javahome;
//	private Composite composite2;
//	private Label label1;
//	private Composite composite1;
//	private Button joanaCompiler;
//    //private Label concLabel;
//    private Button concurrent;
//    private Label libFilterLabel;
//    private Text libFilter;
//
//	/** Creates a new SDGCreator.
//	 *
//	 * @param javafile  A Java file.
//	 * @param parent    The shell where this SDGCreator is shown.
//	 * @param style     Style settings.
//	 */
//	public SDGCreator(CompilationUnit javafile, org.eclipse.swt.widgets.Composite parent, int style) {
//		super(parent, style);
//		this.javafile = javafile;
//
//
//		try {
//		    conf = SDGConfiguration.createSDGConfiguration(javafile);
//
//		} catch (CoreException e) {
//		    MessageDialog.openInformation(getShell(),"Sample View","Could not load stored values");
//		    try {
//		        conf = SDGConfiguration.getDefaultConfiguration(javafile);
//
//		    }catch (CoreException f) {
//	            MessageDialog.openInformation(getShell(),"Sample View","Could not load default values");
//		    }
//
//		}
//
//        initGUI();
//	}
//
//	/** Initializes the SDGCreator.
//	 */
//	private void initGUI() {
//		try {
//			FormLayout thisLayout = new FormLayout();
//			this.setLayout(thisLayout);
//			this.setSize(595, 640);
//			this.getShell().setText("Joana Dataflow Analysis");
//
//			{
//				// a cancel button
//				cancel = new Button(this, SWT.PUSH | SWT.CENTER);
//				FormData cancelLData = new FormData();
//				cancelLData.width = 120;
//				cancelLData.height = 28;
//				cancelLData.left =  new FormAttachment(0, 1000, 293);
//			    cancelLData.top =  new FormAttachment(0, 1000, 525);
//				cancel.setLayoutData(cancelLData);
//				//cancel.setBounds(273, 570, 120, 28);
//				cancel.setText("Cancel");
//				cancel.addSelectionListener(new SelectionAdapter() {
//					public void widgetSelected(SelectionEvent evt) {
//						close();
//					}
//				});
//			}
//
//			{
//				// this button starts the SDG analysis
//				createSDG = new Button(this, SWT.PUSH | SWT.CENTER);
//				FormData createSDGLData = new FormData();
//				createSDGLData.width = 120;
//				createSDGLData.height = 28;
//				createSDGLData.left =  new FormAttachment(0, 1000, 420);
//				createSDGLData.top =  new FormAttachment(0, 1000, 525);
//				createSDG.setLayoutData(createSDGLData);
//				createSDG.setText("Create SDG");
//				//createSDG.setBounds(3500, 2400, 91, 28);
//				createSDG.addSelectionListener(new SelectionAdapter() {
//					public void widgetSelected(SelectionEvent evt) {
//				        // create a new Configuration object
//				        //String mainFile = javafile.getElementName().replaceAll(".java", "");
//					    //SDGConfiguration conf = SDGConfiguration.createSDGConfiguration(mainFile, p, joanaCompiler.getSelection());
//					    //SDGConfiguration conf = SDGConfiguration.createSDGConfiguration(javafile, joanaCompiler.getSelection());
//
//						conf.setJavaHarpoon(javahome.getText().trim());
//						conf.setMemory(memoryTextField.getText().trim());
//						conf.setStubsPath(stubsPath.getText().trim());
//						conf.setSDGPath(sdgPath.getText().trim());
//
//						conf.setWholeProgram(wholeProgram.getSelection());
//                        conf.setFineGrained(fineGrained.getSelection());
//                        conf.setConcPostprocessor(concurrent.getSelection());
//                        try {
//                            conf.setjoanaCompiler(joanaCompiler.getSelection());
//                        } catch(CoreException e) {
//                            MessageDialog.openInformation(getShell(),"Sample View","Could not change the binaries directory");
//                        }
//
//                        if (concurrent.getSelection()) {
//                            conf.setSummaryEdges(true);
//                            conf.setCtrlFlow(true);
//                            conf.setInterference(true);
//
//                        } else {
//                            conf.setSummaryEdges(summaryEdges.getSelection());
//                            conf.setCtrlFlow(controlFlow.getSelection());
//                            conf.setInterference(interference.getSelection());
//                        }
//                        conf.setLibraryFilter(libFilter.getText().trim());
//
//						System.out.println("*************************");
//                        System.out.println(conf);
//                        System.out.println("*************************");
//
//                        // save the configuration
//                        try{
//                            conf.save();
//
//                        } catch(IOException ex) {
//                            System.out.println(ex.getMessage());
//                            ex.printStackTrace();
//                        }
//
//						// compute the SDG for javafile
//						try{
//						    Graph g = GraphFactory.createGraph(javafile, conf);
//
//						    // add g to AnalysisView
//						    AnalysisView.getInstance().setNewGraph(g);
//						    close();
//						} catch(SDGCreationException ex) {
//				            System.out.println(ex.getMessage());
//				            ex.printStackTrace();
//				        }
//					}
//				});
//			}
//			{
//				FormData composite2LData = new FormData();
//				composite2LData.width = 530;
//				composite2LData.height = 320;
//				composite2LData.left =  new FormAttachment(0, 1000, 20);
//				composite2LData.top =  new FormAttachment(0, 1000, 20);
//				composite2 = new Composite(this, SWT.BORDER);
//				composite2.setLayout(null);
//				composite2.setLayoutData(composite2LData);
//	            {
//	                // a choose button for our Joana compiler
//	                joanaCompiler = new Button(composite2, SWT.CHECK | SWT.LEFT);
//	                joanaCompiler.setText("Use Joana Compiler");
//	                joanaCompiler.setSelection(conf.useJoanaCompiler());
//                    joanaCompiler.setBounds(7, 5, 259, 28);
//	            }
//
//				{
//					// textfield to enter the amount of memory
//
//				    // the label
//					memoryLabel = new Label(composite2, SWT.NONE);
//					memoryLabel.setText("Amount of memory (added to Java option -Xmx)");
//					memoryLabel.setBounds(7, 40, 350, 20);
//
//					// the textfield
//					memoryTextField = new Text(composite2, SWT.BORDER);
//					memoryTextField.setText(conf.getMemory());
//					memoryTextField.setBounds(7, 60, 259, 28);
//				}
//				{
//					// the Java home textfield
//
//					// the label
//					javahomeLabel = new Label(composite2, SWT.NONE);
//					javahomeLabel.setText("Path to Java home directory");
//					javahomeLabel.setBounds(7, 110, 315, 20);
//
//					// the textfield
//					javahome = new Text(composite2, SWT.BORDER);
//					javahome.setText(conf.getJavaHarpoon());
//					javahome.setBounds(7, 130, 259, 28);
//
//				    // the button to browse the file system
//					javahomeButton = new Button(composite2, SWT.PUSH
//						| SWT.CENTER);
//					javahomeButton.setText("Java home directory");
//					javahomeButton.setBounds(273, 130, 247, 28);
//					javahomeButton.addSelectionListener(new SelectionAdapter() {
//						public void widgetSelected(SelectionEvent evt) {
//							Shell shell = getShell();
//							DirectoryDialog fd = new DirectoryDialog(shell, SWT.OPEN);
//
//					        fd.setText("Open");
//					        javahome.setText(fd.open());
//						}
//					});
//				}
//				{
//					// the stubs textfield
//
//					// the label
//					stubsLabel = new Label(composite2, SWT.NONE);
//					stubsLabel.setText("Path to stubs ");
//					stubsLabel.setBounds(7, 180, 315, 20);
//
//				    // the textfield
//					stubsPath = new Text(composite2, SWT.BORDER);
//					stubsPath.setText(conf.getStubs());
//					stubsPath.setBounds(7, 200, 259, 28);
//
//				    // the button to browse the file system
//					stubsBrowser = new Button(composite2, SWT.PUSH | SWT.CENTER);
//					stubsBrowser.setText("JAR file");
//					stubsBrowser.setBounds(273, 200, 120, 28);
//					stubsBrowser.addSelectionListener(new SelectionAdapter() {
//						public void widgetSelected(SelectionEvent evt) {
//							Shell shell = getShell();
//							FileDialog fd = new FileDialog(shell, SWT.OPEN);
//
//							fd.setFilterNames(new String[] {"Java archive files (*.jar)"});
//							fd.setFilterExtensions(new String[] {"*.jar"});
//							fd.setText("Open");
//							stubsPath.setText(fd.open());
//						}
//					});
//
//					  // the button to browse the file system
//					stubsBrowser2 = new Button(composite2, SWT.PUSH | SWT.CENTER);
//					stubsBrowser2.setText("directory");
//					stubsBrowser2.setBounds(400, 200, 120, 28);
//					stubsBrowser2.addSelectionListener(new SelectionAdapter() {
//						public void widgetSelected(SelectionEvent evt) {
//							Shell shell = getShell();
//							DirectoryDialog fd = new DirectoryDialog(shell, SWT.OPEN);
//
//					        fd.setText("Open");
//							stubsPath.setText(fd.open());
//						}
//					});
//				}
//				{
//					// the Path to Joana textfield
//
//					// the label
//					sdgLabel = new Label(composite2, SWT.NONE);
//					sdgLabel.setText("Path to SDG library");
//					sdgLabel.setBounds(7, 250, 245, 20);
//
//					// the textfield
//					sdgPath = new Text(composite2, SWT.BORDER);
//					sdgPath.setText(conf.getSDGLibrary());
//					sdgPath.setBounds(7, 270, 259, 28);
//
//				    // the button to browse the file system
//					sdgBrowser = new Button(composite2, SWT.PUSH | SWT.CENTER);
//					sdgBrowser.setText("JAR file");
//					sdgBrowser.setBounds(273, 270, 120, 28);
//					sdgBrowser.addSelectionListener(new SelectionAdapter() {
//						public void widgetSelected(SelectionEvent evt) {
//							Shell shell = getShell();
//							FileDialog fd = new FileDialog(shell, SWT.OPEN);
//
//							fd.setFilterNames(new String[] {"Java archive files (*.jar)"});
//							fd.setFilterExtensions(new String[] {"*.jar"});
//							fd.setText("Open");
//							sdgPath.setText(fd.open());
//						}
//					});
//
//					sdgBrowser2 = new Button(composite2, SWT.PUSH | SWT.CENTER);
//					sdgBrowser2.setText("directory");
//					sdgBrowser2.setBounds(400, 270, 120, 28);
//					sdgBrowser2.addSelectionListener(new SelectionAdapter() {
//						public void widgetSelected(SelectionEvent evt) {
//							Shell shell = getShell();
//							DirectoryDialog fd = new DirectoryDialog(shell, SWT.OPEN);
//
//					        fd.setText("Open");
//							sdgPath.setText(fd.open());
//						}
//					});
//				}
//			}
//			{
//				// layout for the checkboxes that configure the flags for the analysis
//
//				FormData composite1LData = new FormData();
//				composite1LData.width = 250;
//				composite1LData.height = 245;
//				composite1LData.left =  new FormAttachment(0, 1000, 20);
//				composite1LData.top =  new FormAttachment(0, 1000, 365);
//				composite1 = new Composite(this, SWT.BORDER);
//				GridLayout composite1Layout = new GridLayout(1, true);
//				//composite1Layout.makeColumnsEqualWidth = true;
//				composite1.setLayout(composite1Layout);
//				composite1.setLayoutData(composite1LData);
//				{
//					label1 = new Label(composite1, SWT.NONE);
//					label1.setText("       SDG generator options");
//					GridData label1LData = new GridData();
//					//label1LData.horizontalAlignment = GridData.END;
//					label1.setLayoutData(label1LData);
//					label1.setBounds(98, 7, 140, 20);
//				}
//				{
//					wholeProgram = new Button(composite1, SWT.CHECK | SWT.LEFT);
//					wholeProgram.setText("Analyze whole program");
//					wholeProgram.setSelection(conf.isWholeProgram());
//					wholeProgram.setBounds(7, 28, 189, 28);
//				}
//                {
//                    fineGrained = new Button(composite1, SWT.CHECK | SWT.LEFT);
//                    fineGrained.setText("Create fine-grained SDG");
//                    fineGrained.setSelection(conf.isFineGrained());
//                    fineGrained.setBounds(7, 56, 189, 28);
//                }
//				{
//                    concurrent = new Button(composite1, SWT.CHECK | SWT.LEFT);
//                    concurrent.setText("Create precise concurrent SDG");
//                    concurrent.setSelection(conf.useConcPostprocessor());
//                    concurrent.setBounds(7, 84, 189, 28);
//                    concurrent.addSelectionListener(new SelectionAdapter() {
//                        public void widgetSelected(SelectionEvent evt) {
//                            if (concurrent.getSelection()) {
//                                summaryEdges.setEnabled(false);
//                                interference.setEnabled(false);
//                                controlFlow.setEnabled(false);
//
//                            } else {
//                                summaryEdges.setEnabled(true);
//                                interference.setEnabled(true);
//                                controlFlow.setEnabled(true);
//                            }
//                        }
//                    });
//                }
//				{
//					summaryEdges = new Button(composite1, SWT.CHECK | SWT.LEFT);
//					summaryEdges.setText("Compute summary edges");
//					summaryEdges.setSelection(conf.isSummaryEdges());
//					summaryEdges.setBounds(7, 112, 189, 28);
//				}
//				{
//					interference = new Button(composite1, SWT.CHECK | SWT.LEFT);
//					interference.setText("Compute interference edges");
//					interference.setSelection(conf.isInterference());
//					interference.setBounds(7, 140, 189, 28);
//				}
//				{
//					controlFlow = new Button(composite1, SWT.CHECK | SWT.LEFT);
//					controlFlow.setText("Insert control flow edges");
//					controlFlow.setSelection(conf.isCtrlFlow());
//					controlFlow.setBounds(7, 168, 189, 28);
//				}
//				{
//                    // textfield for filtering classes or libraries
//
//                    // the label
//                    libFilterLabel = new Label(composite1, SWT.NONE);
//                    libFilterLabel.setText("Regexp for including library classes");
//                    libFilterLabel.setBounds(15, 185, 189, 28);
//
//                    // the textfield
//                    libFilter = new Text(composite1, SWT.BORDER);
//                    GridData filterLayout = new GridData();
//                    filterLayout.horizontalSpan = 2;
//                    filterLayout.horizontalAlignment = GridData.FILL;
//                    filterLayout.grabExcessHorizontalSpace = true;
//                    libFilter.setLayoutData(filterLayout);
//                    libFilter.setText(conf.getLibraryFilter());
//                    libFilter.setBounds(7, 215, 259, 28);
//                }
//
//			}
//
//            if (concurrent.getSelection()) {
//                summaryEdges.setEnabled(false);
//                interference.setEnabled(false);
//                controlFlow.setEnabled(false);
//
//            } else {
//                summaryEdges.setEnabled(true);
//                interference.setEnabled(true);
//                controlFlow.setEnabled(true);
//            }
//
//			this.layout();
//		} catch (Exception e) {
//			e.printStackTrace();
//		}
//	}
//
//	/** Closes the SDGCreator.
//	 *
//	 */
//	private void close() {
//		this.getShell().close();
//	}
//
//}
