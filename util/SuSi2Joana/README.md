### About

SuSi (see links) is a tool to automaticly select sources and sinks of an Android-Application.
This tool (SuSi2Joana) converts SuSis findings to a format usable with Joana.

### Build

Chdir to this folder, run `ant`.
A jar-package will be placed in ../../dist/edu.kit.joana.SuSi2Joana.jar

### Usage

1. Generate a SDG (.pdg-File) using JoDroid
2. Download SuSi. It contains two files:
    - SourceSinkLists/Android\ 4.2/SourcesSinks/Ouput_CatSources_v0_9.txt
    - SourceSinkLists/Android\ 4.2/SourcesSinks/Ouput_CatSinks_v0_9.txt
3. Run SuSi2Joana giving the mentioned files as options; give a filename ("ifcscript") to the --out option.
    Use the --binary option.
4. Launch the IFC-Console
5. Read in the .pdg-File using the "load SDG from file"-Button
6. Load the ifcscript generated before using the "load script" button

### Links
SuSi: http://sseblog.ec-spride.de/tools/susi/
