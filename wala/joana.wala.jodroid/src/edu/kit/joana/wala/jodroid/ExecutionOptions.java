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
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
//import java.nio.file.Files;   //  Java 7 :(
// Replacement for java.nio.file.Files needs
    

/**
 *  Encapsulate user acessible settings. 
 *
 *  Settings made in CLI or UI are placed in this class first. The class then will be 
 *  used to generate an AnalysisPresets.Preset which replicates these settings in a 
 *  form usable by the internals of the application.
 *
 *  @author Tobias Blaschke <code@tobiasblaschke.de>
 */
public class ExecutionOptions {
    /**
     *  Modes how to retreive the information later presented in the ntrP-File.
     *  
     *  This information includes entrypoints, intents and initialisation-behavior
     *
     *  @see    setScan(ScanMode)
     */
    public static enum ScanMode {
        /** Do not scan anything, rely on information already there ie using an ntrP-file */
        OFF("(default) do not scan anything, rely on information already there ie using an ntrP-file"),
        /** A fast scan based on cha and AndroidManifst.xml */
        NORMAL("A fast scan based on cha and AndroidManifst.xml"),
        /** Build a small call-graph to find out about issued intents */
        EXTENDED("Build a small call-graph to find out about issued intents");
        public final String description;
        private ScanMode(String description) {
            this.description = description;
        }
        public static String dumpOptions() {
            String ret = "";
            for (final ScanMode d : ScanMode.values()) {
                ret = ret + d.toString() + ": " + d.description + "\n";
            }
            return ret;
        }
    }

    /**
     *  How to build the SDG.
     *
     *  @see    setConstruct(BuildMode)
     */
    public static enum BuildMode {
        /** Do not build the SDG, useful in scan-only runs */
        OFF("(default) Do not build the SDG, useful in scan-only runs"),
        /** A conservative setting which causes inclusion of all android-components.
         *  This has been the default before */
        ALL("A conservative setting which causes inclusion of all android-components."),
        /** This causes a model of the target of the MAIN-Intent to be the entrypoint.
         *  Entrypoints detected by Heuristics and for CallBacks will not yet be part
         *  of the model
         */
        MAIN("This causes a model of the target of the MAIN-Intent to be the entrypoint."),
        /** A generalized version of the MAIN-Setting with selectable Intent. */
        INTENT("A generalized version of the MAIN-Setting with selectable Intent."),
        /** Does not use an Android livecycle model but a single method as entrypoint. */
        METHOD("Does not use an Android livecycle model but a single method as entrypoint.");
        public final String description;
        private BuildMode(String description) {
            this.description = description;
        }
        public static String dumpOptions() {
            String ret = "";
            for (final BuildMode d : BuildMode.values()) {
                ret = ret + d.toString() + ": " + d.description + "\n";
            }
            return ret;
        }
    }


    private URI classPath = null;
    private URI androidLib;
    private URI javaStubs;
    private URI exclusions;
    private String entryMethod = null;
    private URI sdgFile;
    private String manifest = null;
    private ScanMode scan = ScanMode.OFF;
    private BuildMode construct = BuildMode.OFF;
    private String intent = null;
    private boolean writeEpFile = true;
    private URI epFile;
    private AnalysisPresets.PresetDescription preset = AnalysisPresets.PresetDescription.DEFAULT;
    private AnalysisPresets.OutputDescription output = AnalysisPresets.OutputDescription.PRETTY;


    public ExecutionOptions() {
        try {
            this.javaStubs = ExecutionOptions.class.getResource("/jSDG-stubs-jre1.4.jar").toURI();
            this.androidLib = ExecutionOptions.class.getResource("/android-18.jar").toURI();
            this.exclusions = ExecutionOptions.class.getResource("/exclusions.txt").toURI();
        } catch (URISyntaxException e) {
            throw new IllegalStateException(e);
        }
    }

    /**
     *  Set the Intent for INTENT-BuildMode.
     */
    public void setIntent(String intent) {
        this.intent = intent;
    }

    public String getIntent() {
        return this.intent;
    }

    /**
     *  Set the App to analyze.
     *
     *  @param  classPath a .jar, .apk, .dex file or path of the App.
     */
    public void setClassPath(final String classPath) {
        final File f = new File(classPath);
        if (! f.exists()) {
            System.err.println("The ClassPath (e.g. the application under analysis) cannot be found at '" + classPath + "'");
            System.exit(17);
        }

        setClassPath(f.toURI());
    }
    /**
     *  Set the App to analyze.
     *
     *  @param  classPath a .jar, .apk, .dex file or path of the App.
     */
    public void setClassPath(final URI classPath) {
        if (this.classPath != null) {
            System.err.println("Error: ClassPath set twice!");
        }
        this.classPath = classPath;
    }
    public URI getClassPath() {
        assert (this.classPath != null) : "ClassPath is null";
        return this.classPath;
    }

    /**
     *  Stubs of the Android-library to use.
     *
     *  If not set explicitly a bundled version of stubs is used. 
     *  Stubs may be built using the stubsBuilder-Utility that comes with JoDroid.
     *
     *  @param  androidLib path of a .jar-File containing the stubs.
     */
    public void setAndroidLib(String androidLib) {
        final File f = new File(androidLib);
        if (! f.exists()) {
            System.err.println("The Android-Stubs cannot be found at '" + androidLib + "'");
            System.exit(17);
        }

        this.androidLib = f.toURI();
    }
    public URI getAndroidLib() {
        try {
            final InputStream testAccess = this.androidLib.toURL().openStream();
        } catch (IOException e) {
            System.err.println("Stubs for the Android library couldn't be found at location '" + androidLib + "'");
            System.err.println("You may want to specify an other library path using the '--lib'-option");
           
            System.exit(17);
        }

        return this.androidLib;
    }

    /**
     *  Stubs of the Java-library to use.
     *
     *  If not set explicitly a bundled version of stubs is used. 
     *
     *  @param  javaStubs path of a .jar-File containing the stubs.
     */
    public void setJavaStubs(String javaStubs) {
        final File f = new File(javaStubs);
        if (! f.exists()) {
            System.err.println("The Java-Stubs cannot be found at '" + javaStubs + "'");
            System.exit(18);
        }

        this.javaStubs = f.toURI();
    }
    public URI getJavaStubs() {
        try {
            final InputStream testAccess = this.javaStubs.toURL().openStream();
        } catch (IOException e) {
            System.err.println("Java-Stubs couldn't be found at location '" + javaStubs + "'");
           
            System.exit(18);
        }

        return this.javaStubs;
    }
    public File getJavaStubsFile() {
        final URI ex = this.getJavaStubs();
        File file;

        if (ex == null) {
            return null;
        } else {
            if (ex.toString().startsWith("jar:")) { // XXX Pretty :(
                try {
                    final URL url = ex.toURL();
                    InputStream input = url.openStream();
                    file = File.createTempFile("javaStubs", ".jar");
                    OutputStream out = new FileOutputStream(file);
                    int read;
                    byte[] bytes = new byte[1024];

                    while ((read = input.read(bytes)) != -1) {
                        out.write(bytes, 0, read);
                    }
                    file.deleteOnExit(); 
                } catch (IOException e) {
                    throw new IllegalStateException(e);
                }
            } else { 
                file = new File(ex);
            }

            if (file != null && !file.exists()) {
                throw new IllegalStateException("Error opening java stubs: Does not exist");
            } else {
                return file;
            }
        }
    }


    /**
     *  Classes to exclude in the Analysis.
     *
     *  Uses a file containing regular expressions of classes to exclude.
     *  If not set explicitly a bundled version of exclusions.txt is used. 
     *
     *  @param  javaStubs path of an exclusion.txt.
     */
    public void setExclusions(String exclusionsPath) {
        final File f = new File(exclusionsPath);
        if (! f.exists()) {
            System.err.println("Exclusions cannot be found at '" + exclusionsPath + "'");
            System.exit(18);
        }

        this.exclusions = f.toURI();
    }
    public URI getExclusions() {
        //final File f = new File(this.exclusions);
        //if (! f.exists()) {
        //    System.err.println("Exclusions cannot be found at '" + this.exclusions + "'");
        //    System.exit(18);
        //}

        return this.exclusions;
    }
    public File getExclusionsFile() {
        final URI ex = this.getExclusions();
        File file;

        if (ex == null) {
            return null;
        } else {
            if (ex.toString().startsWith("jar:")) { // XXX Pretty :(
                try {
                    final URL url = ex.toURL();
                    InputStream input = url.openStream();
                    file = File.createTempFile("exclusions", ".txt");
                    OutputStream out = new FileOutputStream(file);
                    int read;
                    byte[] bytes = new byte[1024];

                    while ((read = input.read(bytes)) != -1) {
                        out.write(bytes, 0, read);
                    }
                    file.deleteOnExit();
                } catch (IOException e) {
                    throw new IllegalStateException(e);
                }
            } else { 
                file = new File(ex);
            }

            if (file != null && !file.exists()) {
                throw new IllegalStateException("Error opening exclusions.txt: Does not exist");
            } else {
                return file;
            }
        }
    }

    /**
     *  Have a single EntryPoint istead of building an Android-Livecycle.
     *
     *  @param  method  Signature of the method
     */
    public void setEntryMethod(String method) {
        this.entryMethod = method;
    }
    public String getEntryMethod() {
        return this.entryMethod;
    }

    /**
     *  Where to write Joanas SDG (.pdg-File).
     *
     *  This File is generated during JoDroids --run-Phase. It may be loaded into the IFC-Console
     *  later.
     *
     *  @param  sdgFile Path where to write the file to
     */
    public void setSdgFile(String sdgFile) {
        final File f = new File(sdgFile);
        if (! f.exists()) {
            System.out.println("The file '" + sdgFile + "' will be overwritten.");
            //System.exit(19);
        }

        this.sdgFile = f.toURI();
    }
    public URI getSdgFile() {
        if (this.sdgFile == null) {
            try {
                this.sdgFile = new URI(this.getClassPath().toString() + ".pdg");
            } catch (java.net.URISyntaxException e) {
                throw new IllegalStateException("Error construction URI for pdg-File", e);
            }
        }
        return this.sdgFile;
    }

    /**
     *  Set an extracted AndroidManifest.xml to read.
     *
     *  File has to be plain XML. May be extracted using apktool.
     *
     *  @param  manifest    Path to AndroidManifest.xml
     */
    public void setManifest(String manifest) {
        final File f = new File(manifest);
        if (! f.exists()) {
            System.out.println("The Manifest-file '" + manifest + "' does not exist.");
            System.exit(20);
        } else {
            try {
                final String mime;
                // Java 7:
                // mime = Files.probeContentType(manifest); 

                // Java 6 is rather bad in determining the mime-type of a file.
                {
                    final FileInputStream is = new FileInputStream(manifest);
                    mime = URLConnection.guessContentTypeFromStream(is);
                    if (is != null) {
                        is.close();
                    }
                }

                if (mime == null) {
                    // This happens if file is a symlink, ...
                    // Do further handling?
                } else if (!(mime.equals("text/xml"))) {
                    System.err.println("The Manifest given is expected to be in the Format 'text/xml'. " +
                            "It was detected as '" + mime + "'. I'll continue for now but reading it later will " +
                            "Almost certainly fail.");
                    System.exit(12);
                }
            } catch (java.io.IOException e) {
                e.printStackTrace();
                System.exit(3);
            } 
        }

        this.manifest = manifest;
    }
    public String getManifest() {
        return this.manifest;
    }
    public File getManifestFile() {
        if (this.manifest == null) {
            return null;
        } else {
            return new File(this.manifest);
        }
    }

    /**
     *  Wheter to search the App for EntryPoints.
     */
    public void setScan(ScanMode scan) {
        this.scan = scan;
    }
    public ScanMode getScan() {
        return this.scan;
    }

    /**
     *  Wheter to build the SDG.
     */
    public void setConstruct(BuildMode construct) {
        this.construct = construct;
    }
    public BuildMode getConstruct() {
        return this.construct;
    }

    /**
     *  Write ot the findings of the --scan-Phase to a file.
     */
    public void setWriteEpFile(boolean write) {
        this.writeEpFile = write;
    }
    public boolean isWriteEpFile() { 
        return this.writeEpFile;
    }

    /**
     *  Write ot the findings of the --scan-Phase to this file.
     *
     *  Sideeffect:
     *      * Enables setWriteEpFile()
     *
     *  @param  epFile where to write the file to
     */
    public void setEpFile(String epFile) {
        final File f = new File(epFile);
        if (! f.exists()) {
            System.out.println("The File '" + epFile + "' does not exist.");
            System.exit(20);
        }
        this.setWriteEpFile(true);
        this.epFile = f.toURI();
    }
    public URI getEpFile() {
        if (this.epFile == null) {
            try {
                this.epFile = new URI(this.getClassPath().toString() + ".ntrP");
            } catch (java.net.URISyntaxException e) {
                throw new IllegalStateException("Error construction URI for ntrP-File", e);
            }
        }
        return this.epFile;
    }

    /**
     *  Use to load predefined settings.
     *
     *  Settings are defined in AnalysisPresets.
     */
    public void setPreset(AnalysisPresets.PresetDescription preset) {
        this.preset = preset;
    }
    public AnalysisPresets.PresetDescription getPreset() {
        return this.preset;
    }

    /**
     *  What to write to the console.
     *
     *  Settings are defined in AnalysisPresets.
     */
    public void setOutput(AnalysisPresets.OutputDescription output) {
        this.output = output;
    }
    public AnalysisPresets.OutputDescription getOutput() {
        return this.output;
    }
}

