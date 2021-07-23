# QIFCI: QIFC Interpreter

A (minimal) Java Interpreter that measures information flow during execution. The information flow analysis combines static and dynamic approaches and delivers sound approximations for the program's channel capacity and the amount of information leaked by the execution itself.

----------------

### Demo
To get QIFCI running quickly, we provide a ``Dockerfile`` in [``https://github.com/tnstrssnr/QIFC-Evaluation``](https://github.com/tnstrssnr/QIFC-Evaluation). The image contains the example programs we used to benchmark QIFCI as well as a script to run the programs in different configurations.


### Build

1. Setup JOANA Dependencies
    - make sure to check if the requirements specified in the base directory's ``README.md`` are fulfilled
    - run ``git submodule init`` and ``git submodule update``
    - run ``setup_deps.sh`` in the JOANA base directory


2. Install cryptominisat & ApproxMC
    - install the following packages:
      ````
      build-essential cmake zlib1g-dev libboost-program-options-dev libm4ri-dev libgmp10 libgmp-dev
      ````
    - follow the instructions at ``https://github.com/meelgroup/approxmc`` to install cryptominisat and ApproxMc

      or
    - simply run ./setup.sh


3. Build QIFCI
    - Run `ant` to build the interpreter and package it in a jar file. The jar will be put in ``joana/dist/``

### Usage

To run the interpreter, use the provided ``run.sh`` script:

````
./run.sh PATH_TO_INPUT_PROGRAM [--args ARGS..]
````

Input parameters for the entrypoint method of the input program can be added as a space-separated list via the
option ``--args``.

The QIF interpreter will execute the program with the given parameters and provide (a lower bound of) the size of the
input set, that would have produced the same outputs. Log files and program analysis results will be dumped in an output
folder.

#### Further options

- ``--usage``: overview over the interpreter's CLI
- ``--o DEST``: specify a directory ``DEST`` where the output folder should be placed. If not specified, the current
  working directory will be used
  ``--static``: Only compute the channel capacity of the program without executing it
  ``--pp``: Enalbe the static pre-processing
- ``--hybrid``: Enable the hybrid analysis for the channel capacity
  ``--unwind BOUND``: Set ``BOUND`` as the limit for th analysis of loop iterations / recursion depth
- ``--run``: only execute the input program, without performing any analysis on it
- ``--dump-graphs``: dump program graphs created by JOANA + CFGs of the program used in the interpreter analysis
- ``--working-dir CURRENT_WORKING_DIR``: required parameter specifying the current working directory. If the ``run.sh``
  script is used, this will be taken care of automatically, if not make sure to include this option

#### Input Programs

The interpreter supports a variant of the while-language, using Java syntax.

The supported language includes the following features:

- commands: ``if .. else``, ``while``, ``return``, ``break``
- data types are restricted to ``int``, which are assumed to be 3 bit wide
- expressions with standard arithmetic and logic operators: ``+, -, *, /, |, &, ^``
- function calls
  - direct recursion is allowed, however we restrict functions to include maximally one recursive call

The parameters of the entry method are per default secret inputs.  
To mark a parameter as public, add the annotation ``@Source(level = Level.LOW`)``

To leak a value to a public output, use the method ``Out.print()``

Because the interpreter is based on JOANA, all input programs must be valid Java 8 programs.  
Make sure to wrap the input program in a class and include a main function, where a single object is created and the
entrypoint method for the analysis is called.  
(The parameters of this call will have no influence on the execution / analysis of the program).

````public static void main(String[] args)````

### Test Suite (deprecated)

To automatically run the testcases in the ``testResources`` folder, use

````
python testTooling/runner.py [OPTION] [PATH_TO_TESTCASE]
````

- To run all testcases, use the option ``all``
- To run only the testcases that previously failed, use the option ``failed``

Before a testcase can be automatically run, it must be analysed and the expected results stored
in ``testResources/results``. To analyze a new testcase, use

````
python testTooling/analyzer NUM_ARGS PATH_TO_TESTCASE
````

``NUM_ARGS`` is the number of input parameters that are expected by the test program