### About JoDroid

JoDroid is the starting point for analyzing an Android-Application usin Joana. Doing so involves multiple steps - see Usage

### Building

Be sure to grab a recent version of Joana and the adapted version of WALA from GitHub. Build Joana as described there.
Then navigate to the root-folder of the Joana-project and type 'ant joana.wala.jodroid'. A Jar-file will be generated 
in the /dist-directory.

### Usage

Analyzing an Android-Application involves multiple steps:

1.  Grab the application in apk-file format

2.  Extract AndroidManifest.xml using the program apktool (optional).
    Simply unpacking the file using a Zip-Tool is _not_ sufficient as Android uses a special coding that JoDroid can't handle yet

3. Generate a new folder containing the apk and AndroidManifest.xml (optional)

4. Run joana.wala.jodroid.jar using the --scan (and optionally the --manifest) options. Be sure to give java enough memory (using the -Xmx-Option). This will generate a entrypoint-specification file (.ntrP)

    Example:
        java -Xss16m -Xmx2048m -jar joana.wala.jodroid.jar --manifest project/AndroidManifest.xml --scan normal project/project.apk
        
        produces: project/project.apk.ntrP

5. Edit the entrypoint-specification file to match the needs of the analysis. Options under the settings-tag are _not_ read back yet. However entrypoints may be added or moved in the model at this point. Instantiation-behavior and resolution of intents may be changed.

If the file is not edited JoDroid should produce usable results too. However due to the huge count of possible entry-points it's hard to guarantee correct behavior.

6. Run JoDroid again using the specifications-file. This will produce a SDG (.pdg-File)

    Example:
        java -Xss16m -Xmx2048m -jar joana.wala.jodroid.jar --ep-file project/project.apk.ntrP --construct all --analysis full project/project.apk
        
        produces: project/project.apk.pdg

        The Option "--analysis full" generates an Object-Sensitive SDG.
        The Option "--contruct all" tells JoDroid to considder all components of the application as the reason the app was started. A Useful alternative would be "--construct main" to start with the Main-Intent. The later variant is however less conservative and fails for Apps without Main-Intent.


7. Launch the IFC-Console (/dist/joana.ui.ifc.wala.console.jar).

8. Load project.apk.pdg using the button "load SDG from file"

9. Specify Sources and Sinks in the "Annotations"-Tab, finally run.

### See also

* The tool in /util/SuSi2Joana may be used to automatically select sources and sinks. Doing so will also issue security-warnings for Android-internal flows.
* The tool in /util/AndroidStubsBuilder may be used to generate newer versions of Androids stubs usable with JoDroid. 
