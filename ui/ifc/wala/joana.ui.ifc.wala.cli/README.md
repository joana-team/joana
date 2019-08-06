CLI
===

Usage
-----
Running `ant` builds the cli and produces a file named `joana.ui.ifc.wala.cli.jar` in the `dist` folder of the JOANA source directory. The class that should be analysed has to be in the classpath when calling the cli JAR. 

The following shows how to analyse the classes from the `example` folder, assuming that the current working directory is the nildumu main directory:

```
# compile the Basic class
javac -classpath ../../../../dist/joana.ifc.sdg.qifc.nildumu.jar example/Basic.java

# analyse it
java -cp ../../../../dist/joana.ifc.sdg.qifc.nildumu.jar:example edu.kit.joana.ifc.sdg.qifc.nildumu.Main --classpath example Basic
```

