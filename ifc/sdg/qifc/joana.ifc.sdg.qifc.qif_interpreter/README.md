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

#### Input Program

The input program should be a single-class Java program, that creates an object and calls a method on it in the main method.
This method call will be the entry-point of the analysis and the executing of the interpreter.

Parameters of the method are per default secret inputs. To mark a parameter as public, add the annotation ``@Source(level = Level.LOW`)``

The interpreter offers a single public output channel for values via the method ``Out.print()``