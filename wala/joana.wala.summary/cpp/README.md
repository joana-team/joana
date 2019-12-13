CPP
===
Trials of summary computation in C++, using the simplified summary graph created by
the parex package of JOANA and exported using protobuf.

Requirements
------------
- C++17 compiler
- protobuf libraries

Usage
-----
Use it in JOANA via the `CPPAnalysis`. 
On the command line, pass the mode and the graph file name (a few examples are in this folder 
ending with `.pg`). Or alternatively pipe the file into this program.

| mode | description
|------|-------------
| s    | basic sequential algorithm
| p    | parallel algorithm where each of the threads is responsible for a specific segment of the functions, set the number of computer threads via the `CPP_THREADS` environment variable.