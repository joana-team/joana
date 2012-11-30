package edu.kit.joana.ifc.wala.sdpn.benchmark

import java.io.File
import java.io.FileWriter
import java.io.BufferedWriter
import java.io.PrintWriter
import java.io.ObjectOutputStream
import java.io.BufferedOutputStream
import java.io.FileOutputStream

trait Suite {
    def name: String
    def note: String
    def settings: Iterable[Setting]
    def outputDir: String = odir

    def outputDir_=(x: String) {
        import java.io.File
        var j = 1
        var folder = new File(x)
        val xf = folder.toString
        while (folder.exists && !folder.listFiles.isEmpty) {
            folder = new File(xf + j)
            j += 1
        }
        odir = folder.toString
    }
    private var odir: String = null
}

object Benchmark {
    // Avoid stupid bug on nfs load left/right/either now.
    val _1234: Either[Exception, Any] = Left(new Exception("12334"))
    val _123: Either[Any, Exception] = Right(new Exception("123"))

    def main(args: Array[String]) {
        val JRE14_LIB = "Primordial,Java,jarFile,../jSDG/lib/jSDG-stubs-jre1.4.jar";
        val J2ME_LIB = "Primordial,Java,jarFile,../jSDG/lib/jSDG-stubs-j2me2.0.jar";
        val JAVACARD_LIB = "Primordial,Java,jarFile,../jSDG/lib/jSDG-stubs-javacard.jar";
        val DO_CACHE = false;
        val SKIP_PRIMORDIAL = true;
        val INTERPRET_KILL = true;
        val UNSAFE_KILL = false;
        val IGNORE_WAIT = false
        val NO_EXCEPTIONS = false
        val ITERABLE_ANALYSIS = false
        val THREAD_CONTEXTS = true
        val XSB_TIMEOUT = 1000 * 60 * 2;
        val JSDG_TIMEOUT = 1000 * 60 * 30;
        val BIN = "../Tests/bin"
        //        val BIN = "bin"

        def setting(main: String) = Setting(
            BIN,
            main,
            JRE14_LIB,
            DO_CACHE,
            SKIP_PRIMORDIAL,
            INTERPRET_KILL,
            UNSAFE_KILL,
            IGNORE_WAIT,
            NO_EXCEPTIONS,
            ITERABLE_ANALYSIS,
            THREAD_CONTEXTS,
            XSB_TIMEOUT,
            JSDG_TIMEOUT)

        def mains = "Lconc/kn/Knapsack5" :: Nil

        runOn(mains.map(setting), "results/benchmark01")

    }

    def runOn(st: Iterable[Setting], folder: String) {
        runOn(new Suite { val name = "noname"; val note = ""; val settings = st; outputDir = folder })
    }

    def runOn(st: Suite) {
        var folder = new File(st.outputDir)
        if (!folder.exists)
            folder.mkdirs
        assert(folder.isDirectory)
        val mainFile = new File(folder, "main.txt")
        val objectFile = new File(folder, "DataObject.dat")

        val mainOut = new PrintWriter(new BufferedWriter(new FileWriter(mainFile)))
        val summary = new File(folder, "summary.txt")
        var results: List[AppResult] = Nil
        try {
            for (s <- st.settings) {
                val name = s.main.drop(1).replace("/", ".").replace('$', '.')
                println("Running benchmark on: " + name)
                var i = 1
                var subFile = new File(folder, name + ".txt")
                while (subFile.exists) {
                    subFile = new File(folder, name + "_" + i + ".txt")
                    i += 1
                }
                val subOut = new PrintWriter(new BufferedWriter(new FileWriter(subFile)))
                val objectOut = new ObjectOutputStream(new BufferedOutputStream(new FileOutputStream(objectFile)))
                val res = new AppResult(s);
                results ::= res
                val start = System.currentTimeMillis
                try {
                    BenchmarkCSDGwithSDPNBuilder.runAnalysis(res)
                    res.totalTime = System.currentTimeMillis - start
                } catch {
                    case e: Throwable =>
                        res.setError(e.toString, exceptionWrapper(e).getStackTraceString)
                        res.totalTime = System.currentTimeMillis - start
                        e.printStackTrace
                        mainOut.println
                        mainOut.println("-------- ERROR FOR --------")
                        mainOut.println(s.makeString)
                        mainOut.println("----------ERROR-----------")
                        e.printStackTrace(mainOut)
                        mainOut.println()
                        mainOut.println("------------------------------")
                        subOut.println(res.makeString(true))
                } finally {
                    try {
                        mainOut.println
                        mainOut.println(res.makeString(false))
                        subOut.println(res.makeString(true))
                    } finally {
                        try {
                            objectOut.writeObject(results)
                        } finally {
                            try {
                                printSummaryToFile(st.name, st.note, results, summary)
                            } finally {
                                mainOut.flush
                                subOut.close
                                objectOut.close
                            }
                        }
                    }
                }
            }
        } finally {
            mainOut.close

        }
        de.wwu.sdpn.core.ta.xsb.XSBInterRunner.shutdown()
    }

    def makeSummary(suiteName: String, note: String, results: List[AppResult]): String = {
        val buf = new StringBuilder()
        def out(x: Any) { buf.append(x); buf.append("\n") }
        def secs(x: Long) = "%d.%03ds".format(x / 1000, x % 1000)        
        out("Summary for: " + suiteName)
        out("---- Note ----")
        out(note)
        if(results.isEmpty) {
            out ("---- Results ----")
            out (" !! EMPTY RESULT LIST !! ")
            return buf.toString
        }
        out("---- Applications ----")
        out("Applications                                  " + results.size)
        out("Applications checked                          " + results.filter(_.error.isEmpty).size)
        out("Applications not checked (error)              " + results.filter(_.error.isDefined).size)
        out("Applications w/o ife                          " + results.filter(_.numberExpected == 0).size)
        out("Applications w/o checked ife                  " + results.filter(_.numberChecked == 0).size)
        out("Applications with checked ife                 " + results.filter(_.numberChecked > 0).size)
        out("Applications with removed ife                 " + results.filter(_.numberImpossible > 0).size)
        out("Applications with checkt but w/o removed ife  " + results.filter(x => x.numberImpossible == 0 && x.numberChecked > 0).size)
        out("Applications with locks                       " + results.filter(_.anyWithLock).size)
        out("Applications w/o locks                        " + results.filter(x => !(x.anyWithLock)).size)
        out("---- Total edges ----")
        out("Expected     " + results.map(_.numberExpected).sum)
        out("Checked      " + results.map(_.numberChecked).sum)
        out("Removed      " + results.map(_.numberImpossible).sum)
        out("Not removed  " + results.map(_.numberPossible).sum)
        out("Errors       " + results.map(_.numberError).sum)
        out("---- Maximal number of edges ----")
        out("Expected     " + results.map(_.numberExpected).max + " ( " + results.maxBy(_.numberExpected).setting.main + " )")
        out("Checked      " + results.map(_.numberChecked).max + " ( " + results.maxBy(_.numberChecked).setting.main + " )")
        out("Removed      " + results.map(_.numberImpossible).max + " ( " + results.maxBy(_.numberImpossible).setting.main + " )")
        out("Not removed  " + results.map(_.numberPossible).max + " ( " + results.maxBy(_.numberPossible).setting.main + " )")
        out("Errors       " + results.map(_.numberError).max + " ( " + results.maxBy(_.numberError).setting.main + " )")
        out("---- Total times ----")
        out("Total time   " + secs(results.map(_.totalTime).sum))
        out("JSDG time    " + secs(results.map(_.timeJSDG).sum))
        out("SDPN time    " + secs(results.map(_.timeDPN).sum))
        out("XSB time     " + secs(results.map(_.timeXSB).sum))
        out("---- Maximal times for one application ----")
        out("Total time   " + secs(results.map(_.totalTime).max) + " ( " + results.maxBy(_.totalTime).setting.main + " )")
        out("jSDG time    " + secs(results.map(_.timeJSDG).max) + " ( " + results.maxBy(_.timeJSDG).setting.main + " )")
        out("sDPN time    " + secs(results.map(_.timeDPN).max) + " ( " + results.maxBy(_.timeDPN).setting.main + " )")
        out("XSB time     " + secs(results.map(_.timeXSB).max) + " ( " + results.maxBy(_.timeXSB).setting.main + " )")
        out("Edge time    " + secs(results.map(_.maxTimeXSB).max) + " ( " + results.maxBy(_.maxTimeXSB).setting.main + " )")
        out("---- Grouped ----")
        out(" -- With locks --")
        out("Total checked      " + results.map(_.numberWithLocks).sum)
        out("Total removed      " + results.map(_.numberImpossibleWithLocks).sum)
        out("Total not removed  " + results.map(_.numberPossibleWithLocks).sum)
        out("Max checked        " + results.map(_.numberWithLocks).max + " ( " + results.maxBy(_.numberWithLocks).setting.main + " )")
        out("Max removed        " + results.map(_.numberImpossibleWithLocks).max + " ( " + results.maxBy(_.numberImpossibleWithLocks).setting.main + " )")
        out("Max not removed    " + results.map(_.numberPossibleWithLocks).max + " ( " + results.maxBy(_.numberPossibleWithLocks).setting.main + " )")
        out(" -- Without locks --")
        out("Total checked      " + results.map(_.numberWithoutLocks).sum)
        out("Total removed      " + results.map(_.numberImpossibleWithoutLocks).sum)
        out("Total not removed  " + results.map(_.numberPossibleWithoutLocks).sum)
        out("Max checked        " + results.map(_.numberWithoutLocks).max + " ( " + results.maxBy(_.numberWithoutLocks).setting.main + " )")
        out("Max removed        " + results.map(_.numberImpossibleWithoutLocks).max + " ( " + results.maxBy(_.numberImpossibleWithoutLocks).setting.main + " )")
        out("Max not removed    " + results.map(_.numberPossibleWithoutLocks).max + " ( " + results.maxBy(_.numberPossibleWithoutLocks).setting.main + " )")
        out(" -- On regular fields --")
        out("Total checked      " + results.map(_.numberRegular).sum)
        out("Total removed      " + results.map(_.numberImpossibleRegular).sum)
        out("Total not removed  " + results.map(_.numberPossibleRegular).sum)
        out("Max checked        " + results.map(_.numberRegular).max + " ( " + results.maxBy(_.numberRegular).setting.main + " )")
        out("Max removed        " + results.map(_.numberImpossibleRegular).max + " ( " + results.maxBy(_.numberImpossibleRegular).setting.main + " )")
        out("Max not removed    " + results.map(_.numberPossibleRegular).max + " ( " + results.maxBy(_.numberPossibleRegular).setting.main + " )")
        out(" -- On non regular fields --")
        out("Total checked      " + results.map(_.numberNonRegular).sum)
        out("Total removed      " + results.map(_.numberImpossibleNonRegular).sum)
        out("Total not removed  " + results.map(_.numberPossibleNonRegular).sum)
        out("Max checked        " + results.map(_.numberNonRegular).max + " ( " + results.maxBy(_.numberNonRegular).setting.main + " )")
        out("Max removed        " + results.map(_.numberImpossibleNonRegular).max + " ( " + results.maxBy(_.numberImpossibleNonRegular).setting.main + " )")
        out("Max not removed    " + results.map(_.numberPossibleNonRegular).max + " ( " + results.maxBy(_.numberPossibleNonRegular).setting.main + " )")
        out(" -- With overwrite (only on regular fields) --")
        out("Total checked      " + results.map(_.numberWithOverwrite).sum)
        out("Total removed      " + results.map(_.numberImpossibleWithOverwrite).sum)
        out("Total not removed  " + results.map(_.numberPossibleWithOverwrite).sum)
        out("Max checked        " + results.map(_.numberWithOverwrite).max + " ( " + results.maxBy(_.numberWithOverwrite).setting.main + " )")
        out("Max removed        " + results.map(_.numberImpossibleWithOverwrite).max + " ( " + results.maxBy(_.numberImpossibleWithOverwrite).setting.main + " )")
        out("Max not removed    " + results.map(_.numberPossibleWithOverwrite).max + " ( " + results.maxBy(_.numberPossibleWithOverwrite).setting.main + " )")
        out(" -- Without overwrite (only on regular fields) --")
        out("Total checked      " + results.map(_.numberWithoutOverwrite).sum)
        out("Total removed      " + results.map(_.numberImpossibleWithoutOverwrite).sum)
        out("Total not removed  " + results.map(_.numberPossibleWithoutOverwrite).sum)
        out("Max checked        " + results.map(_.numberWithoutOverwrite).max + " ( " + results.maxBy(_.numberWithoutOverwrite).setting.main + " )")
        out("Max removed        " + results.map(_.numberImpossibleWithoutOverwrite).max + " ( " + results.maxBy(_.numberImpossibleWithoutOverwrite).setting.main + " )")
        out("Max not removed    " + results.map(_.numberPossibleWithoutOverwrite).max + " ( " + results.maxBy(_.numberPossibleWithoutOverwrite).setting.main + " )")
        out("---- Other ----")
        out("Maximal dpn size         " + results.map(_.maxSize).max + " ( " + results.maxBy(_.maxSize).setting.main + " )")
        if (results.filter(_.anyWithLock).size > 0)
            out("Maximal number of locks  " + results.map(_.maxLocks).max + " ( " + results.maxBy(_.maxLocks).setting.main + " )")
        else
            out("Maximal number of locks  0")
        out("---- Checked applications ----")
        for (r <- results)
            out(r.setting.main)
        out("---- Errorneous applications ----")
        for (r <- results.filter(_.error.isDefined))
            out(r.setting.main + " ( " + r.error.get + " )")

        buf.toString
    }

    def printSummaryToFile(suiteName: String, note: String, results: List[AppResult], summary: File) {
        val sumOut = new PrintWriter(new BufferedWriter(new FileWriter(summary)))
        try {
            sumOut.print(makeSummary(suiteName, note, results))
        } finally {
            sumOut.close
        }

    }

}