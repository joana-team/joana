### About

StubsBuilder is a tool to generate Android-Stubs usable by JoDroid. This ist done by combining the vanilla-Stubs to a single file and compiling in some extra classes.

### Usage

Do _not_ expect that stubsBuilder runs without compilation errors the first time. Obviously the process has to be adapted to the sources.

1. In the Android-SDK download:
    - The Android-Stubs
    - The Android Source Code
2. Change the stubsBuilder.ini so it includes the classes to build additinally. 
    Further changes may be necessary depending on the Android version.
    Running stubsBuilder and changing the ini is an iterative process until it works
3. Well,... run stubsBuilder. 
    Prefer using the 'all'-Variant. If running with single steps be shure to use the "--work-dir"-Option.
    If the Android-SDK contains multiple usable versions of Android the '--platform' option has to be used to select one. 
    Usable platforms may be listed using `stubsBuilder platforms`. If for a platform either Stubs or Sources are missing they will not be listed.
    It generates the subs somewhere in the temp-folder. The generated stubs may be used with JoDroid using the --lib-Option

### stubsBuilder.ini

This file is searched in the Current Working Directory (`pwd`) first. If not found it is searched in the folder the stubsBuilder-script is located in.
Alternatively it may be specified using the '--config' option.

Documentation on the effect of the various settings is given in the file itself.


### Requirements

To run stubsBuilder the following requirements have to be installed on the system

* Android SDK including stubs and source-code
* Python
* The Python-Library "cement"
* The program jar (has to be in the systems PATH)
* A Java-Compiler (javac has to be in the systems PATH)
