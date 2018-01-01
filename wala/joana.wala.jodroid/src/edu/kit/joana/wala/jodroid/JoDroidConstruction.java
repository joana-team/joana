/*
 *  Copyright (c) 2013,
 *      Tobias Blaschke <code@tobiasblaschke.de>
 *  All rights reserved.

 *  Redistribution and use in source and binary forms, with or without
 *  modification, are permitted provided that the following conditions are met:
 *
 *  1. Redistributions of source code must retain the above copyright notice,
 *     this list of conditions and the following disclaimer.
 *
 *  2. Redistributions in binary form must reproduce the above copyright notice,
 *     this list of conditions and the following disclaimer in the documentation
 *     and/or other materials provided with the distribution.
 *
 *  3. The names of the contributors may not be used to endorse or promote
 *     products derived from this software without specific prior written
 *     permission.
 *
 *  THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 *  AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 *  IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 *  ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 *  LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 *  CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 *  SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 *  INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 *  CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 *  ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 *  POSSIBILITY OF SUCH DAMAGE.
 */
package edu.kit.joana.wala.jodroid;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.jar.JarFile;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// Needed by findMethod:
import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.classLoader.JarStreamModule;
import com.ibm.wala.dalvik.classLoader.DexFileModule;
import com.ibm.wala.dalvik.ipa.callgraph.androidModel.AndroidModel;
import com.ibm.wala.dalvik.ipa.callgraph.androidModel.AndroidModelSome;
import com.ibm.wala.dalvik.ipa.callgraph.androidModel.IntentModel;
import com.ibm.wala.dalvik.ipa.callgraph.androidModel.parameters.IInstantiationBehavior;
import com.ibm.wala.dalvik.ipa.callgraph.androidModel.parameters.LoadedInstantiationBehavior;
import com.ibm.wala.dalvik.ipa.callgraph.propagation.cfa.Intent;
import com.ibm.wala.dalvik.util.AndroidEntryPointLocator;
import com.ibm.wala.dalvik.util.AndroidEntryPointLocator.LocatorFlags;
import com.ibm.wala.dalvik.util.AndroidEntryPointManager;
import com.ibm.wala.dalvik.util.AndroidManifestXMLReader;
import com.ibm.wala.ipa.callgraph.AnalysisScope;
import com.ibm.wala.ipa.cha.ClassHierarchy;
import com.ibm.wala.ipa.cha.ClassHierarchyException;
import com.ibm.wala.ipa.cha.ClassHierarchyFactory;
import com.ibm.wala.ipa.cha.IClassHierarchy;
import com.ibm.wala.types.ClassLoaderReference;
import com.ibm.wala.types.MethodReference;
import com.ibm.wala.types.Selector;
import com.ibm.wala.types.TypeReference;
import com.ibm.wala.util.CancelException;
import com.ibm.wala.util.MonitorUtil.IProgressMonitor;
import com.ibm.wala.util.NullProgressMonitor;
import com.ibm.wala.util.config.AnalysisScopeReader;
import com.ibm.wala.util.graph.GraphIntegrity.UnsoundGraphException;
import com.ibm.wala.util.io.FileSuffixes;

import edu.kit.joana.ifc.sdg.graph.SDG;
import edu.kit.joana.ifc.sdg.graph.SDGSerializer;
import edu.kit.joana.wala.core.SDGBuilder;
import edu.kit.joana.wala.jodroid.AnalysisPresets.Preset;
import edu.kit.joana.wala.jodroid.entrypointsFile.Reader;
import edu.kit.joana.wala.jodroid.entrypointsFile.Writer;

/**
 *  "Main-Routines" of the inner JoDroid.
 *
 *  These methods get called by UI/CLI components.
 *  
 *  @see edu.kit.joana.wala.jodroid.JodDroidCLI
 */
public class JoDroidConstruction {
    private static final Logger logger;
    static {
        
        logger = LoggerFactory.getLogger(JoDroidConstruction.class);
    }

    /**
     *  The Preset contains any settings needed during JoDroid-runs.
     */
    final Preset p;

    /**
     *  Locate Entrypoints and fill InstantiationBehavior.
     *
     *  The scan-results may be found in p.aem.ENTRIES after this method was called.
     *
     *  Sideeffects:
     *      * Fill Preset.aem.ENTRIES
     *      * Fill Cache of p.aem.getInstantiationBehavior()
     */

    private final AndroidEntryPointManager manager;
    public void scanEntryPoints() throws SDGConstructionException {
        if (p.scfg.cha == null) {
            throw new IllegalStateException("The cha has to be built first");
        }

        // Manifest?
        final IProgressMonitor mon = this.manager.getProgressMonitor();
        final IClassHierarchy cha = p.scfg.cha;

        try {
            logger.info("Scanning for EntryPoints");
            logger.debug("Using flags: {}", p.entrypointLocatorFlags.toString());
            final AndroidEntryPointLocator epl = new AndroidEntryPointLocator(this.manager, p.entrypointLocatorFlags);
            this.manager.setEntries(epl.getEntryPoints(cha));

            final IInstantiationBehavior instantiationBehvior;
            { // Grab the instantiation behavior of attributes to the entrypoints
                mon.beginTask("Filling InstantiationBehavior...", IProgressMonitor.UNKNOWN);
                mon.worked(1);
                instantiationBehvior = this.manager.getInstantiationBehavior(cha);

                // By building the model we fill the instanciationBehvior-cache. This is only needed when
                // writing out the ntrP-File directly. As it's rather inepensive we do it always non the 
                // less.
                final IMethod model = new AndroidModel(this.manager, cha, p.options, p.scfg.cache).getMethod();
                mon.done();
            }
        } catch (CancelException e) {
            throw new SDGConstructionException(e);
        }
    }

    /**
     *  Does an extensive search which results in a compete list of issued intents.
     *
     *  To get a list of all Intents issued by the app in analysis a small CallGraph has to be built.
     *
     *  Sideeffects:
     *      * Fill Preset.aem.ENTRIES
     *      * Fill Cache of p.aem.getInstantiationBehavior()
     *      * Fill p.em.getSeen() with instructions that start Intents
     */
    public void scanEntryPointsExtended() {
        throw new UnsupportedOperationException("Not implemented");

        // 1. Do scanEntryPoints()
        // 2. Generate minimum settings for CallGraphBuilder
        // 3. Add IntentContextSelector /-Interpreter to the CGB
        // 4. Call AndroidModel().getMethod()
        // 5. Build CallGraph and throw it away
    }

    /**
     *  Read in an extracted AndroidManifest.xml.
     *
     *  The file has to be previously extracted to the "real" XML-Format. 
     *  Reading the Manifest is optional. Doing so will enable JoDroid to resolve app-internal
     *  Intents.
     *
     *  Sideeffects:
     *      * Fill Preset.aem.overrideIntents 
     *      * Set Preset.aem.getPackage()
     *
     *  @param  manifest The path to the AndroidManifest.xml to read
     */
    public void loadAndroidManifest(final File manifest) {
        if (p.scfg.cha == null) {
            throw new IllegalStateException("The cha has to be built first");
        }

        if (manifest != null) {
            final AndroidManifestXMLReader reader = new AndroidManifestXMLReader(this.manager, manifest);
        }
    }

    public void loadAndroidManifest(final ExecutionOptions ex) {
        loadAndroidManifest(ex.getManifestFile());
    }
    /**
     *  Write the ntrP-File.
     *
     *  The ntrP-File contains all data detected during the --scan-Phase. It may be edited by the user and
     *  handed in in the --run-Pase.
     *
     *  Writes configs, intents, instantiation-behavior and entypoints to a file in XML-Fomrat.
     *
     *  @param  outFile Where to write the ntrP-File to
     */
    public void saveEntryPoints(final URI outFile) throws IOException {
        if (outFile == null) {
            throw new IllegalArgumentException("OutFile may not be null");
        }

        final IProgressMonitor mon = this.manager.getProgressMonitor();
        final IInstantiationBehavior instantiationBehvior = this.manager.getInstantiationBehavior(this.p.scfg.cha);
        final Writer writer = new Writer();

        mon.beginTask("Serializing and writing...", 5);
        mon.subTask("Android Model Config");
        writer.add(this.manager);
        mon.subTask("Joana Config"); mon.worked(1);
        writer.add(this.p.scfg);
        mon.subTask("Intent information"); mon.worked(2);
        writer.add(this.manager.overrideIntents);
        /*{ // DEBUG
            System.out.println("OVR: " + this.p.aem.overrideIntents);
        } // */
        mon.subTask("Instantiation Behavior"); mon.worked(3);
        writer.add(instantiationBehvior);
        mon.subTask("Entry Points"); mon.worked(4);
        writer.add(this.manager.getEntries());
        mon.subTask("Writing"); mon.worked(5);
        writer.write(new File(outFile));
        mon.done();
    }

    /**
     *  Reads a file generated using saveEntryPoints().
     *
     *  After setting up the AnalysisScope and creating the ClassHierarchy the definitions may be read
     *  from a ntrP-File. These contain: Intent-Overrides, Entrypoints and InstantiationBehavior.
     *
     *  Sideeffects:
     *      * Fill Preset.aem.ENTRIES
     *      * Fill Cache of p.aem.getInstantiationBehavior()
     *      * Fill Preset.aem.overrideIntents 
     *      * Set Preset.aem.getPackage()
     *
     *  @param  entryPointFile  Where to read the ntrP-File from
     *  @throws IllegalStateException If the cha has not been built before.
     */
    public void loadEntryPoints(final File entryPointFile) throws IOException {
        if (entryPointFile == null) {
            throw new IllegalArgumentException("entryPointFile may not be null");
        }
        if (p.scfg.cha == null) {
            throw new IllegalStateException("The cha has to be constructed before reading back the entrypoints");
        }
        if (p.scfg.scope == null) {
            throw new IllegalStateException("The scope has to be set before reading back the entrypoints");
        }

        final Reader reader = new Reader(entryPointFile, p.scfg.cha);
        final IInstantiationBehavior beh = new LoadedInstantiationBehavior(p.scfg.cha);

        reader.addTarget(beh);
        reader.addTarget(this.manager.getEntries());
        //reader.addTarget(p.aem.overrideIntents); // TODO: Implement
        reader.read(this.manager);

        this.manager.setInstantiationBehavior(beh);
    }

    /**
     *  Use a _single_ entry-point.
     *
     *  Setting an Entrypoint using this method will disable the generation of an Android-Livecycle.
     *
     *  @param  ep  The single EntryPoint to use
     */
    public void setSingleEntryPoint(final IMethod ep) {
        throw new UnsupportedOperationException("Not implemented!");
    }

    private SDG buildAndroidSDG(AndroidModel modeller) throws SDGConstructionException {
        final IMethod livecycle;
        { // Build the model
            try {
                //livecycle = modeller.getMethod();         // This variant uses FakeRoot-Init
                livecycle = modeller.getMethodEncap();      // Uses new Instantiator
            } catch (CancelException e) {
                throw new SDGConstructionException(e);
            }
        }

        AnalysisPresets.prepareBuild(this.manager, p);

        final SDG sdg;
        { // Hand over to Joana to construct the SDG
            try {
                p.scfg.entry = livecycle;
                sdg = SDGBuilder.build(p.scfg, this.manager.getProgressMonitor());
            } catch (UnsoundGraphException e) {
                throw new SDGConstructionException(e);
            } catch (CancelException e) {
                throw new SDGConstructionException(e);
            }
        }

        return sdg;
    	
    }
    
    /**
     *  Generate the Android-Livecycle and produce joanas SDG.
     *
     *  Before calling this method the AnalysisScope has to be set and the ClassHierarchy created. Entrypoints
     *  have to be specified.
     *
     *  This method will then construct the Livecycle for the android-App and build a SDG.
     *
     *  @throws IllegalStateException If cha has not been built before or no entrypoints have been specified
     *  @return The System-Dependence-Graph for the analyzed application
     */
    public SDG buildAndroidSDGAll() throws SDGConstructionException {
        if (p.scfg.cha == null) {
            throw new IllegalStateException("The cha has to be constructed before building the SDG");
        }
        if (this.manager.getEntries().isEmpty()) {
            throw new IllegalStateException("Androids entrypoints have to be set before generating the SDG! " + 
                    "This can be done using the scan-function or by reading a ntrP-File" );
        }
        if (p.scfg.scope == null) {
            throw new IllegalStateException("The scope has to be set before constructing the SDG!");
        }
        return buildAndroidSDG(new AndroidModel(this.manager, p.scfg.cha, p.options, p.scfg.cache)); 
    }
    
    /**
     *  Generate the Android-Livecycle and produce joanas SDG.
     *
     *  Before calling this method the AnalysisScope has to be set and the ClassHierarchy created. Entrypoints
     *  have to be specified.
     *
     *  This method will then construct the Livecycle for the android-App and build a SDG.
     *
     *  @throws IllegalStateException If cha has not been built before or no entrypoints have been specified
     *  @return The System-Dependence-Graph for the analyzed application
     */
    public SDG buildAndroidSDGSome(List<String> intents) throws SDGConstructionException {
        if (p.scfg.cha == null) {
            throw new IllegalStateException("The cha has to be constructed before building the SDG");
        }
        if (this.manager.getEntries().isEmpty()) {
            throw new IllegalStateException("Androids entrypoints have to be set before generating the SDG! " + 
                    "This can be done using the scan-function or by reading a ntrP-File" );
        }
        if (p.scfg.scope == null) {
            throw new IllegalStateException("The scope has to be set before constructing the SDG!");
        }

        return buildAndroidSDG(new AndroidModelSome(this.manager, p.scfg.cha, p.options, p.scfg.cache, intents));
    }

    /**
     *  Generate the Android-Livecycle for a single Intent.
     *
     *  @param  intent The intent to build a livecycle for.
     *  @throws IllegalStateException If cha has not been built before or no entrypoints have been specified
     *  @return The System-Dependence-Graph for the analyzed application
     */
    public SDG buildAndroidSDGIntent(Intent intent) throws SDGConstructionException {
        if (p.scfg.cha == null) {
            throw new IllegalStateException("The cha has to be constructed before building the SDG");
        }
        if (this.manager.getEntries().isEmpty()) {
            throw new IllegalStateException("Androids entrypoints have to be set before generating the SDG! " + 
                    "This can be done using the scan-function or by reading a ntrP-File" );
        }
        if (p.scfg.scope == null) {
            throw new IllegalStateException("The scope has to be set before constructing the SDG!");
        }
        if (intent == null) {
            throw new IllegalArgumentException("The intent may not be null");
        }
        
        intent = this.manager.getIntent(intent);   // resolve intent
        if (! intent.isInternal(/* strict = */ true)) {
            throw new IllegalArgumentException("The Intent " + intent + " is not internally resolvable! " +
                    "Are specifications loaded - either from manifest or ntrP?");
        } 

        final IMethod livecycle;
        final AndroidModel modeller;
        { // Build the model
            try {
                /* { // TODO Get rid of this:
                    // The makroModel registers itself as a sideeffect - expected to be there
                    final AndroidModel makroModel = new AndroidModel(p.scfg.cha, p.options, p.scfg.cache);
                    makroModel.getMethod();
                } // */
                modeller = new IntentModel(this.manager, p.scfg.cha, p.options, p.scfg.cache, intent.getAction());
                //livecycle = modeller.getMethod();   
                livecycle = modeller.getMethodEncap();   
            } catch (CancelException e) {
                throw new SDGConstructionException(e);
            }
        }

        AnalysisPresets.prepareBuild(this.manager, p);

        final SDG sdg;
        { // Hand over to Joana to construct the SDG
            try {
                p.scfg.entry = livecycle;
                sdg = SDGBuilder.build(p.scfg, this.manager.getProgressMonitor());
            } catch (UnsoundGraphException e) {
                throw new SDGConstructionException(e);
            } catch (CancelException e) {
                throw new SDGConstructionException(e);
            }
        }

        return sdg;
    }


    /**
     *  JoDroid is stateful, create a state.
     *
     *  CAUTION: JoDroid uses static fields in some of its classes. During the livetime of the application
     *  it is only save to have a single instance of JoDroidConstruction! 
     *
     *  @param  manager Object which has overview over entrypoints, intents etc. and which interacts with everything
     *  @param  scope   Where to load various parts of the app in analysis from
     *  @param  preset  Name of a hardcoded preset (the other constructor may be used to customize).
     */
    public JoDroidConstruction(final AndroidEntryPointManager manager, final AnalysisScope scope, final AnalysisPresets.PresetDescription preset) throws SDGConstructionException {
        try {
            this.manager = manager;
            final IClassHierarchy cha = ClassHierarchyFactory.make(scope, this.manager.getProgressMonitor());
            this.p = AnalysisPresets.make(this.manager, preset, scope, cha);
        } catch (ClassHierarchyException e) {
            throw new SDGConstructionException(e);
        }
    }

    /**
     *  JoDroid is stateful, create a state.
     *
     *  CAUTION: JoDroid uses static fields in some of its classes. During the livetime of the application
     *  it is only save to have a single instance of JoDroidConstruction! 
     *
     *  @param  manager Object which has overview over entrypoints, intents etc. and which interacts with everything
     *  @param  p   Options to JoDroid may be set in p
     */
    public JoDroidConstruction(final AndroidEntryPointManager manager, final Preset p) {
        this.manager = manager;
        this.p = p;
    }

    /**
     *  "main"-method usable by JoDroid-UIs.
     *
     *  User-Exposed options or set in "ExecutionOptions ex". This method dispatches the execution of JoDroid
     *  based on this settings
     *
     *  @param  ex  Options exposed to the user.
     */
    public static void dispatch(final AndroidEntryPointManager manager, final ExecutionOptions ex) throws SDGConstructionException, IOException {
        final AnalysisScope scope = makeScope(null, null, ex);

        final IClassHierarchy cha;
        { // Build cha
            try {
                if (ex.getOutput() == AnalysisPresets.OutputDescription.QUIET) {
                    cha = ClassHierarchyFactory.make(scope, new NullProgressMonitor());
                } else {
                    cha = ClassHierarchyFactory.make(scope, manager.getProgressMonitor());
                }
            } catch (ClassHierarchyException e) {
                throw new SDGConstructionException(e);
            }
        }

        Preset p = AnalysisPresets.make(manager, ex.getPreset(), scope, cha);
        AnalysisPresets.applyOutput(manager, ex.getOutput(), p);
        final JoDroidConstruction constr = new JoDroidConstruction(manager, p);

        constr.loadAndroidManifest(ex.getManifestFile());



        switch (ex.getScan()) {
            case OFF: // Read in the ntrP-File  TODO
                final File epFile = new File(ex.getEpFile());
                if (epFile.exists()) {  // XXX: This is a bad way to check
                    constr.loadEntryPoints(epFile);
                }
                break;
            case NORMAL:
                constr.scanEntryPoints();
                break;
            case EXTENDED:
                constr.scanEntryPointsExtended();
                break;
            default:
                throw new IllegalStateException("The Scan-Mode " + ex.getScan() + " is not implemented.");
        }

        SDG sdg = null; 
        switch (ex.getConstruct()) {
            case OFF:
                sdg = null;
                break;
            case ALL:
                sdg = constr.buildAndroidSDGAll();
                break;
            case SOME:
            	final List<String> intentsS = ex.getIntents();
            	sdg = constr.buildAndroidSDGSome(intentsS);
            	break;
            case MAIN:
                ex.setIntent("Landroid/intent/action/MAIN");
                // no break
            case INTENT:
                final String intent = ex.getIntent();
                if (intent == null) {
                    throw new IllegalStateException("In INTENT-Construction mode an intent has to be set.");
                }
                sdg = constr.buildAndroidSDGIntent(new Intent(manager, intent));
                break;
            case METHOD:
                if (ex.getEntryMethod() == null) {
                    throw new IllegalStateException("In METHOD-Construction mode a method has to be set.");
                }
                try {
                    constr.setSingleEntryPoint(findMethod(cha, ex.getEntryMethod()));
                } catch (MethodNotFoundException e) {
                    throw new SDGConstructionException(e);
                }
                break;
            default:
                throw new IllegalStateException("The Construction-Mode " + ex.getConstruct() + " is not implemented.");
        }

        if (ex.isWriteEpFile()) {
            constr.saveEntryPoints(ex.getEpFile());
        }

        { // Write the pdg-file
            if (sdg != null) {
                final File outFile = new File(ex.getSdgFile());
                if (! outFile.exists()) {
                    outFile.createNewFile();
                }
                SDGSerializer.toPDGFormat(sdg, new FileOutputStream(outFile));
            }
        }

        { // Pretty :/
            final java.util.Map< com.ibm.wala.classLoader.CallSiteReference, com.ibm.wala.dalvik.ipa.callgraph.propagation.cfa.Intent > seen = manager.getSeen ();
            
            System.out.println("Encountered Intents were:");
            for (final com.ibm.wala.classLoader.CallSiteReference site : seen.keySet()) {
                System.out.println("\t" + site + " calls " + seen.get(site));
            }

        } // */
    }   


    //
    // Short-Hand and compatibility stuff follows
    //

    /**
     *  Search the entrypoints of an Android-App and write results to a ntrP-File.
     *
     *  @param  classPath   The android-application to analyze
     *  @param  androidLib  null or path to Android-Stubs. If null the bundled-ones will be used.
     *  @param  outFile     null or path where to write the ntrP-File to.
     *  @param  manifest    AndroidManifest.xml of the App in analysis (may be null)
     */
    public static void scanEntryPointsAndSave(final AndroidEntryPointManager manager, final String classPath, final String androidLib, final String ntrPFile, final File manifest)
                throws SDGConstructionException, IOException {
        if (classPath == null) {
            throw new IllegalArgumentException("classPath may not be null");
        }

        final ExecutionOptions ex = new ExecutionOptions();
        ex.setClassPath(classPath);
        if (androidLib != null) {
            ex.setAndroidLib(androidLib);
        }
        if (ntrPFile != null) {
            ex.setEpFile(ntrPFile);
        }
        if (manifest != null) {
            ex.setManifest(manifest.getPath());
        }
        ex.setScan(ExecutionOptions.ScanMode.NORMAL);
        ex.setConstruct(ExecutionOptions.BuildMode.OFF);
        ex.setWriteEpFile(true);
        dispatch(manager, ex);
    }

    public static AnalysisScope makeScope(final String dexClassPath, final String androidLib, ExecutionOptions ex) throws IOException {
        {
            if (ex == null) {
                ex = new ExecutionOptions();
            }
            if (dexClassPath != null) {
                ex.setClassPath(dexClassPath);
            }
            if (androidLib != null) {
                ex.setAndroidLib(androidLib);
            }
        } 

        final AnalysisScope scope;
        { // generate the scope
            final URI classPathUri = ex.getClassPath();
            final String javaClassPath = ex.getJavaClassPath();
            if (javaClassPath == null && classPathUri == null) {
            	throw new IllegalArgumentException();
            }
            final URI androidStubs = ex.getAndroidLib();
            assert(androidStubs != null);
            final URI javaStubs = ex.getJavaStubs();
            final File exclusions = ex.getExclusionsFile();
            
            logger.info("Using ClassPath:\t{}", classPathUri);
            logger.info("Using Android Stubs:\t{}", androidStubs);
            logger.info("Using Java Stubs:\t{}", javaStubs);

            assert (exclusions != null); // XXX: Handle case without exclusions?
            scope = AnalysisScope.createJavaAnalysisScope();
            scope.setLoaderImpl(ClassLoaderReference.Primordial, "com.ibm.wala.dalvik.classLoader.WDexClassLoaderImpl");
            scope.setLoaderImpl(ClassLoaderReference.Application, "com.ibm.wala.dalvik.classLoader.WDexClassLoaderImpl");
            if (classPathUri != null) {
            	scope.addToScope(ClassLoaderReference.Application, DexFileModule.make(new File(classPathUri)));
            }
            if (javaClassPath != null) {
            	AnalysisScopeReader.addClassPathToScope(String.join(File.pathSeparator, javaClassPath), scope, scope.getLoader(AnalysisScope.APPLICATION));
            }

            if (FileSuffixes.isRessourceFromJar(javaStubs)) {
                final InputStream is = javaStubs.toURL().openStream();
                scope.addToScope(ClassLoaderReference.Primordial,
                        new JarStreamModule(is));
            } else {
                scope.addToScope(ClassLoaderReference.Primordial, new JarFile(new File(
                                javaStubs)));
            }


            if (FileSuffixes.isRessourceFromJar(androidStubs)) {
                final InputStream is = androidStubs.toURL().openStream();
                scope.addToScope(ClassLoaderReference.Primordial,
                        new JarStreamModule(is));
            } else {
                scope.addToScope(ClassLoaderReference.Primordial, new JarFile(new File(
                                androidStubs)));
            }

        }

        return scope;
    }

    /**
     *  Generate the ClassHierarchy for an Android-App.
     *
     *  A bundled exclusions-File is used during generation!
     *
     *  @param  classPath   The Android-App to generate the cha for
     *  @param  androidLib  Adroid-Stubs to use during generation. If null: use bunded ones.
     */
    public static IClassHierarchy computeCH(AndroidEntryPointManager manager, String classPath, String androidLib) throws IOException, ClassHierarchyException {
        final AnalysisScope scope = makeScope(classPath, androidLib, new ExecutionOptions()); 
        return ClassHierarchyFactory.make(scope, manager.getProgressMonitor());
    }

    // public static SDG buildAndroidSDG(IMethod entryMethod) throws SDGConstructionException, IOException
    // public static SDG buildAndroidSDG(String classPath, String androidLib, String entryMethod) throws SDGConstructionException, IOException
    // public static SDG buildAndroidSDG(String classPath, String androidLib, Maybe<String> stubsPath, String entryMethod)
    public static void buildAndroidSDGAndSave(AndroidEntryPointManager manager, String classPath, String androidLib, String entryMethod, String sdgFile) throws SDGConstructionException, IOException {
        buildAndroidSDGAndSave(manager, classPath, androidLib, /* stubsPath = */ null, entryMethod, sdgFile);
    }

    /**
     *  Analyze an Android-App using a single EntryPoint - e.g without Livecycle-Model.
     *
     *  @param  classPath   Android-App to analyze
     *  @param  androidLib  Android-Stubs to use. If null: Use the bundled ones
     *  @param  stubsPath   Java-Stubs to use. If null: Use the bundled ones
     *  @param  entryMethod Signature of the EntryPoint
     *  @param  sdgFile     Where to write the results
     */
    public static void buildAndroidSDGAndSave(AndroidEntryPointManager manager, String classPath, String androidLib, String stubsPath, String entryMethod, String sdgFile)
                             throws SDGConstructionException, IOException {
        if (classPath == null) {
            throw new IllegalArgumentException("classPath may not be null");
        }
        if (entryMethod == null) {
            throw new IllegalArgumentException("entryMethod may not be null");
        }

        final ExecutionOptions ex = new ExecutionOptions();
        ex.setClassPath(classPath);
        if (androidLib != null) {
            ex.setAndroidLib(androidLib);
        }
        if (stubsPath != null) {
            ex.setJavaStubs(stubsPath);
        }
        ex.setEntryMethod(entryMethod);
        if (sdgFile != null) {
            ex.setSdgFile(sdgFile);
        }

        ex.setScan(ExecutionOptions.ScanMode.OFF);
        ex.setConstruct(ExecutionOptions.BuildMode.ALL);
        ex.setWriteEpFile(false);
        dispatch(manager, ex);
    }

    /**
     *  Resolves a method-signature to an IMethod.
     */
    private static IMethod findMethod(IClassHierarchy cha, String mSig) throws MethodNotFoundException {
        int off = mSig.lastIndexOf('.');
        String type = "L" + mSig.substring(0, off).replace('.', '/');
        String mSel = mSig.substring(off + 1);
        TypeReference tRef = TypeReference.findOrCreate(ClassLoaderReference.Application, type);
        MethodReference mRef = MethodReference.findOrCreate(tRef, Selector.make(mSel));
        IMethod cand = cha.resolveMethod(mRef);
        if (cand != null) {
            return cand;
        } else {
            for (IClass c : cha) {
                for (IMethod m : c.getAllMethods()) {
                    if (m.getSignature().equals(mSig)) {
                        return m;
                    }
                }
            }

            throw new MethodNotFoundException(mSig);
        }
    }
}
