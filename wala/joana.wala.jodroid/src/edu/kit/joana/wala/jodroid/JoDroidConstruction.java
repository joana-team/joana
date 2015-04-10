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

import com.ibm.wala.ipa.callgraph.AnalysisScope;
import com.ibm.wala.dalvik.util.AndroidAnalysisScope;
import com.ibm.wala.dalvik.util.AndroidEntryPointManager;
import com.ibm.wala.dalvik.util.AndroidManifestXMLReader;

import edu.kit.joana.wala.jodroid.AnalysisPresets;
import edu.kit.joana.wala.jodroid.AnalysisPresets.Preset;

import com.ibm.wala.ipa.cha.IClassHierarchy;
import com.ibm.wala.ipa.cha.ClassHierarchy;
import com.ibm.wala.ipa.cha.ClassHierarchyException;

import com.ibm.wala.classLoader.IMethod;

import com.ibm.wala.dalvik.ipa.callgraph.androidModel.AndroidModel;
import com.ibm.wala.dalvik.ipa.callgraph.androidModel.stubs.Overrides;
import com.ibm.wala.dalvik.util.AndroidEntryPointLocator;
import com.ibm.wala.dalvik.util.AndroidEntryPointLocator.LocatorFlags;
import com.ibm.wala.dalvik.ipa.callgraph.androidModel.parameters.IInstantiationBehavior;
import com.ibm.wala.dalvik.ipa.callgraph.androidModel.parameters.LoadedInstantiationBehavior;

import com.ibm.wala.dalvik.ipa.callgraph.propagation.cfa.Intent;
import com.ibm.wala.dalvik.ipa.callgraph.androidModel.IntentModel;

import edu.kit.joana.ifc.sdg.graph.SDG;
import edu.kit.joana.wala.core.SDGBuilder;
import edu.kit.joana.ifc.sdg.graph.SDGSerializer;

import com.ibm.wala.util.MonitorUtil.IProgressMonitor;
import com.ibm.wala.util.NullProgressMonitor;

import com.ibm.wala.types.ClassLoaderReference;
import com.ibm.wala.util.io.FileSuffixes;
import java.util.jar.JarInputStream;
import java.util.jar.JarFile;
import com.ibm.wala.classLoader.JarStreamModule;
import com.ibm.wala.dalvik.dex.util.config.DexAnalysisScopeReader;
import com.ibm.wala.util.config.AnalysisScopeReader;

// Needed by findMethod:
import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.types.TypeReference;
import com.ibm.wala.types.MethodReference;
import com.ibm.wala.types.Selector;

import edu.kit.joana.wala.jodroid.entrypointsFile.Writer;
import edu.kit.joana.wala.jodroid.entrypointsFile.Reader;

import java.io.File;
import java.net.URI;
import java.io.FileOutputStream;
import java.util.jar.JarFile;
import java.io.IOException;
import java.io.InputStream;
import com.ibm.wala.util.CancelException;
import com.ibm.wala.util.graph.GraphIntegrity.UnsoundGraphException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
    public void scanEntryPoints() throws SDGConstructionException {
        if (p.scfg.cha == null) {
            throw new IllegalStateException("The cha has to be built first");
        }

        // Manifest?
        final IProgressMonitor mon = Preset.aem.getProgressMonitor();
        final IClassHierarchy cha = p.scfg.cha;

        try {
            logger.info("Scanning for EntryPoints");
            logger.debug("Using flags: {}", p.entrypointLocatorFlags.toString());
            final AndroidEntryPointLocator epl = new AndroidEntryPointLocator(p.entrypointLocatorFlags);
            AndroidEntryPointManager.ENTRIES = epl.getEntryPoints(cha);

            final IInstantiationBehavior instantiationBehvior;
            { // Grab the instantiation behavior of attributes to the entrypoints
                mon.beginTask("Filling InstantiationBehavior...", IProgressMonitor.UNKNOWN);
                mon.worked(1);
                instantiationBehvior = Preset.aem.getInstantiationBehavior(cha);

                // By building the model we fill the instanciationBehvior-cache. This is only needed when
                // writing out the ntrP-File directly. As it's rather inepensive we do it always non the 
                // less.
                final IMethod model = new AndroidModel(cha, p.options, p.scfg.cache).getMethod();
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
            try {
                final AndroidManifestXMLReader reader = new AndroidManifestXMLReader(manifest);
            } catch (IOException e) {
                System.err.println();
                System.err.println("!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
                System.err.println("Error reading in the Manifest. - Continuing without it!");
                e.printStackTrace();
            }
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

        final IProgressMonitor mon = Preset.aem.getProgressMonitor();
        final IInstantiationBehavior instantiationBehvior = Preset.aem.getInstantiationBehavior(this.p.scfg.cha);
        final Writer writer = new Writer();

        mon.beginTask("Serializing and writing...", 5);
        mon.subTask("Android Model Config");
        writer.add(Preset.aem);
        mon.subTask("Joana Config"); mon.worked(1);
        writer.add(this.p.scfg);
        mon.subTask("Intent information"); mon.worked(2);
        writer.add(Preset.aem.overrideIntents);
        /*{ // DEBUG
            System.out.println("OVR: " + this.p.aem.overrideIntents);
        } // */
        mon.subTask("Instantiation Behavior"); mon.worked(3);
        writer.add(instantiationBehvior);
        mon.subTask("Entry Points"); mon.worked(4);
        writer.add(AndroidEntryPointManager.ENTRIES);
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
        reader.addTarget(AndroidEntryPointManager.ENTRIES);
        //reader.addTarget(p.aem.overrideIntents); // TODO: Implement
        reader.read();

        Preset.aem.setInstantiationBehavior(beh);
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
        if (AndroidEntryPointManager.ENTRIES.isEmpty()) {
            throw new IllegalStateException("Androids entrypoints have to be set before generating the SDG! " + 
                    "This can be done using the scan-function or by reading a ntrP-File" );
        }
        if (p.scfg.scope == null) {
            throw new IllegalStateException("The scope has to be set before constructing the SDG!");
        }

        final IMethod livecycle;
        final AndroidModel modeller;
        { // Build the model
            modeller = new AndroidModel(p.scfg.cha, p.options, p.scfg.cache);
            try {
                //livecycle = modeller.getMethod();         // This variant uses FakeRoot-Init
                livecycle = modeller.getMethodEncap();      // Uses new Instantiator
            } catch (CancelException e) {
                throw new SDGConstructionException(e);
            }
        }

        AnalysisPresets.prepareBuild(p);

        final SDG sdg;
        { // Hand over to Joana to construct the SDG
            try {
                p.scfg.entry = livecycle;
                sdg = SDGBuilder.build(p.scfg, Preset.aem.getProgressMonitor());
            } catch (UnsoundGraphException e) {
                throw new SDGConstructionException(e);
            } catch (CancelException e) {
                throw new SDGConstructionException(e);
            }
        }

        return sdg;
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
        if (AndroidEntryPointManager.ENTRIES.isEmpty()) {
            throw new IllegalStateException("Androids entrypoints have to be set before generating the SDG! " + 
                    "This can be done using the scan-function or by reading a ntrP-File" );
        }
        if (p.scfg.scope == null) {
            throw new IllegalStateException("The scope has to be set before constructing the SDG!");
        }
        if (intent == null) {
            throw new IllegalArgumentException("The intent may not be null");
        }
        
        intent = Preset.aem.getIntent(intent);   // resolve intent
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
                modeller = new IntentModel(p.scfg.cha, p.options, p.scfg.cache, intent.getAction());
                //livecycle = modeller.getMethod();   
                livecycle = modeller.getMethodEncap();   
            } catch (CancelException e) {
                throw new SDGConstructionException(e);
            }
        }

        AnalysisPresets.prepareBuild(p);

        final SDG sdg;
        { // Hand over to Joana to construct the SDG
            try {
                p.scfg.entry = livecycle;
                sdg = SDGBuilder.build(p.scfg, Preset.aem.getProgressMonitor());
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
     *  @param  scope   Where to load various parts of the app in analysis from
     *  @param  preset  Name of a hardcoded preset (the other constructor may be used to customize).
     */
    public JoDroidConstruction(final AnalysisScope scope, final AnalysisPresets.PresetDescription preset) throws SDGConstructionException {
        try {
            final IClassHierarchy cha = ClassHierarchy.make(scope, Preset.aem.getProgressMonitor());
            this.p = AnalysisPresets.make(preset, scope, cha);
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
     *  @param  p   Options to JoDroid may be set in p
     */
    public JoDroidConstruction(final Preset p) {
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
    public static void dispatch(final ExecutionOptions ex) throws SDGConstructionException, IOException {
        final AnalysisScope scope = makeScope(null, null, ex);

        final IClassHierarchy cha;
        { // Build cha
            try {
                if (ex.getOutput() == AnalysisPresets.OutputDescription.QUIET) {
                    cha = ClassHierarchy.make(scope, new NullProgressMonitor());
                } else {
                    cha = ClassHierarchy.make(scope, Preset.aem.getProgressMonitor());
                }
            } catch (ClassHierarchyException e) {
                throw new SDGConstructionException(e);
            }
        }

        Preset p = AnalysisPresets.make(ex.getPreset(), scope, cha);
        AnalysisPresets.applyOutput(ex.getOutput(), p);
        final JoDroidConstruction constr = new JoDroidConstruction(p);

        constr.loadAndroidManifest(ex.getManifestFile());

        { // Read in the ntrP-File  TODO
            final File epFile = new File(ex.getEpFile());   
            if (epFile.exists()) {  // XXX: This is a bad way to check
                constr.loadEntryPoints(epFile);
            }
        }

        switch (ex.getScan()) {
            case OFF:
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
            case MAIN:
                ex.setIntent("Landroid/intent/action/MAIN");
                // no break
            case INTENT:
                final String intent = ex.getIntent();
                if (intent == null) {
                    throw new IllegalStateException("In INTENT-Construction mode an intent has to be set.");
                }
                sdg = constr.buildAndroidSDGIntent(new Intent(intent));
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
            final java.util.Map< com.ibm.wala.classLoader.CallSiteReference, com.ibm.wala.dalvik.ipa.callgraph.propagation.cfa.Intent > seen = Preset.aem.getSeen ();
            
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
    public static void scanEntryPointsAndSave(final String classPath, final String androidLib, final String ntrPFile, final File manifest)
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
        dispatch(ex);
    }

    public static AnalysisScope makeScope(final String classPath, final String androidLib, ExecutionOptions ex) throws IOException {
        {
            if (ex == null) {
                ex = new ExecutionOptions();
            }
            if (classPath != null) {
                ex.setClassPath(classPath);
            }
            if (androidLib != null) {
                ex.setAndroidLib(androidLib);
            }
        } 

        final AnalysisScope scope;
        { // generate the scope
            final URI classPathUri = ex.getClassPath();
            assert (classPathUri != null);
            final URI androidStubs = ex.getAndroidLib();
            assert(androidStubs != null);
            final URI javaStubs = ex.getJavaStubs();
            final File exclusions = ex.getExclusionsFile();
            
            logger.info("Using ClassPath:\t{}", classPathUri);
            logger.info("Using Android Stubs:\t{}", androidStubs);
            logger.info("Using Java Stubs:\t{}", javaStubs);

            assert (exclusions != null); // XXX: Handle case without exclusions?
            
            scope = DexAnalysisScopeReader.makeAndroidBinaryAnalysisScope(classPathUri, exclusions.getAbsolutePath());
            scope.setLoaderImpl(ClassLoaderReference.Application,
                    "com.ibm.wala.dalvik.classLoader.WDexClassLoaderImpl");
            scope.setLoaderImpl(ClassLoaderReference.Primordial,
                    "com.ibm.wala.dalvik.classLoader.WDexClassLoaderImpl");

            scope.addToScope(AnalysisScopeReader.makePrimordialScope(exclusions)); // Reads primordial.txt 

            if (FileSuffixes.isRessourceFromJar(javaStubs)) {
                final InputStream is = javaStubs.toURL().openStream();
                scope.addToScope(ClassLoaderReference.Primordial,
                        new JarStreamModule(new JarInputStream(is)));
            } else {
                scope.addToScope(ClassLoaderReference.Primordial, new JarFile(new File(
                                javaStubs)));
            }


            if (FileSuffixes.isRessourceFromJar(androidStubs)) {
                final InputStream is = androidStubs.toURL().openStream();
                scope.addToScope(ClassLoaderReference.Primordial,
                        new JarStreamModule(new JarInputStream(is)));
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
    public static IClassHierarchy computeCH(String classPath, String androidLib) throws IOException, ClassHierarchyException {
        final AnalysisScope scope = makeScope(classPath, androidLib, new ExecutionOptions()); 
        return ClassHierarchy.make(scope, Preset.aem.getProgressMonitor());
    }

    // public static SDG buildAndroidSDG(IMethod entryMethod) throws SDGConstructionException, IOException
    // public static SDG buildAndroidSDG(String classPath, String androidLib, String entryMethod) throws SDGConstructionException, IOException
    // public static SDG buildAndroidSDG(String classPath, String androidLib, Maybe<String> stubsPath, String entryMethod)
    public static void buildAndroidSDGAndSave(String classPath, String androidLib, String entryMethod, String sdgFile) throws SDGConstructionException, IOException {
        buildAndroidSDGAndSave(classPath, androidLib, /* stubsPath = */ null, entryMethod, sdgFile);
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
    public static void buildAndroidSDGAndSave(String classPath, String androidLib, String stubsPath, String entryMethod, String sdgFile)
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
        dispatch(ex);
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
