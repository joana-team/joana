# QIF Interpreter

Java Interpreter that measures information flow during execution.

----------------

### Build

Running `ant` build a jar file and puts it in joana/dist

### Usage

Use the ``run.sh`` script to run the interpreter. The only required parameter is the path of the program to be compiled

Optional parameters:
- option ``-args [..]``: input parameters with which the program should be executed
- option ``-o *path_to_directory``: Specify where the output directory should be created
- option ``dump``: dumps the SDGs to the output directory
- option ``--static``: performs only a static analysis and dumps the results

If a static analysis of the program has already been performed, use the ``.class`` file and the ``.dnnf`` file from the analysis output as the input for the interpreter.
The static analysis will then be skipped.