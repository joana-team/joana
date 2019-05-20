Nildumu
=======

An implementation of the quantitative information flow analysis describe in [the master's thesis of Johannes Bechberger](https://pp.info.uni-karlsruhe.de/publication.php?id=bechberger18masterarbeit).

The base implementation is https://github.com/parttimenerd/nildumu
this implementation is a reimplementation on top of JOANA with support for more language features.

Usage
-----
Running `ant` builds nildumu and produces a file named `joana.ifc.sdg.qifc.nildumu.jar` in the `dist` folder of the JOANA source directory. The class that should be analysed has to be in the classpath when calling the nildumu JAR (and using its command line interface from class `edu.kit.joana.ifc.sdg.qifc.nildumu.Main`). 

The following shows how to analyse the program `Basic` from the `example` folder, assuming that the current working directory is the nildumu main directory:

```
# compile the Basic class
javac -classpath ../../../../dist/joana.ifc.sdg.qifc.nildumu.jar example/Basic.java

# analyse it
java -cp ../../../../dist/joana.ifc.sdg.qifc.nildumu.jar:example edu.kit.joana.ifc.sdg.qifc.nildumu.Main --classpath example Basic
```


Method Invocation Handlers
--------------------------
 - it uses as syntax the standard Java property syntax, using `;`
     instead of line breaks
   - handlers are selected using the property `handler`
   - if this misses and the configuration string just consists of
      a single identifier, then this identifier is considered as
      the chosen handler
   - the `basic` handler
    - this is the default handler, that just connects returns
      a return value for every function call in which every bit
      depends on every parameter bit
    - this yields to maximal over-approximation, but it is fast
      and therefore used as default handler to gather some kind of
      basis for other handlers
   - the `inlining` handler
    - A call string based handler that just inlines a function.
    - If a function was inlined in the current call path more
      than a defined number of times (`maxRec`), then another
      handler is used to compute a conservative approximation
    - properties
        - `maxrec`: default is `2`
        - `bot`: the handler for the overapproximation, just
          a handler configuration, as the current one
            - allows to chain handlers, it might be useful to use
              the `summary` handler here
            - default is `basic`
   - the `summary` handler
    - A summary-edge based handler.
    - It creates for each function beforehand summary edges:
        - these edges connect the parameter bits and the return bits
    - The analysis assumes that all parameter bits might have a
      statically unknown value.
    - The summary-edge analysis builds the summary edges using a
      fix point iteration over the call graph.
    - Each analysis of a method runs the normal analysis of the
      method body and uses the prior summary edges if a method is
      called in the body.
    - The resulting bit graph is then reduced.
    - It supports coinduction (`mode=coind`)
      and induction (`mode=ind`), but the default is to choose
      induction for non-recursive programs and else coinduction
      (`mode=auto`)
    - Induction starts with no edges between parameter bits and
      return bits and iterates till no new connection between a
      return bit and a parameter bit is added.
        - It only works for programs without recursion.
    - Coinduction starts with the an over approximation produced by
      another handler (`bot` property) and iterates at most a
      configurable number of times (`maxiter` property), by default
      this number is 2147483647 (the maximum number of signed 32 bit
      integer)
    - The default reduction policy is to connect all return bits
      with all parameter bits that they depend upon
      ("reduction=basic")
        - An improved version (`reduction=mincut`) includes the
          minimal cut bits of the bit graph from the return to the
          parameter bits, assuming that the return bits have
          infinite weights
    - properties
        - `maxiter`: maximum number of iterations for the
          coinduction, as every intermediate state is also valid
        - `bot`: default is `basic`
        - `mode`: `ind` or `coind`, default is `coind`
        - `reduction`: reduction policy, either `basic` or `mincut`,
          default is `mincut`
        - `dot`: folder to output dot files for the bit graphs of
          the methods in different iterations and the call-graph
            - default: empty string, produces no dot files
        - `csmaxrec`: if > 0, each sub analysis uses a call-string
          based handler for evaluation method invocations, using
          the computed summary edges as `bot` and the passed value
          as `maxrec`. Likely improves precision but also increases
          the size of the summary edges.

License
-------
MIT