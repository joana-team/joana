/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.ifc.wala.sdpn.benchmark

import edu.kit.joana.ifc.wala.sdpn.CSDGwithSDPNBuilder
import edu.kit.joana.ifc.wala.sdpn.SDGCreatorJSDGStyle
import scala.collection.JavaConversions._
import com.codahale.logula.Logging
import org.apache.log4j.Level
import edu.kit.joana.ifc.wala.sdpn.RefinementResult

object TestRunner extends Logging {
    def runOn(app: TestApp, outputDir:String): RefinementResult = {
        import app._
        log.debug("TestApp: %s", app)
        CSDGwithSDPNBuilder.runAnalysis(new SDGCreatorJSDGStyle(), classPath, mainClass, scopeData, outputDir, false, true, 30 * 60 * 1000)
    }

       def runOn(apps: Map[String, TestApp], outputDir:String): Map[String, Either[RefinementResult,Throwable]] = {
        val results = for ((name, app) <- apps) yield {
            log.info("Running on %s", name)
            val res = try {
                Left(runOn(app, outputDir))
            } catch {
                case e: Throwable =>
                e.printStackTrace()
                log.error(e, "Error while running on %s", name)
                Right(e)
            }

            log.info("Ran on %s, result: %s", name, res)
            name -> res
        }
        val removed = results.collect{case (_,Left(x)) => x.removed}.sum
        val suspected = results.collect{case (_,Left(x)) => x.suspected}.sum
        log.info("Finished test run removed %s edges of a total of %s suspected edges in %s applications.", removed, suspected, results.size)
        results
    }
}

object RunGiffhornSuite extends Logging {
    def main(args: Array[String]) {
        Logging.configure { log =>

            log.level = Level.TRACE
            //log.loggers("com.myproject.weebits") = Level.OFF

            log.console.enabled = true
            log.console.threshold = Level.INFO

            log.file.enabled = true
            log.file.filename = "/tmp/sdpnifc/IFCGiffhornTest.log"
            log.file.maxSize = 10 * 1024 // KB
            log.file.retainedFiles = 5 // keep five old logs around
        }
        val results = TestRunner.runOn(TestData.giffhornSuite,"/tmp")
        val removed = results.collect{case (_,Left(x)) => x.removed}.sum
        val suspected = results.collect{case (_,Left(x)) => x.suspected}.sum
        val errors = results.collect{case (_,Right(x)) => 1}.sum
        
        log.info("Finished test run removed %s edges of a total of %s suspected edges in %s applications (%s with errors).", removed, suspected, results.size,errors)
        printf("Finished test run removed %s edges of a total of %s suspected edges in %s applications (%s with errors).\n", removed, suspected, results.size, errors)

    }
}

object RunExamplesSuite extends Logging {
    def main(args: Array[String]) {
        Logging.configure { log =>

            log.level = Level.TRACE
            //log.loggers("com.myproject.weebits") = Level.OFF

            log.console.enabled = true
            log.console.threshold = Level.INFO

            log.file.enabled = true
            log.file.filename = "/tmp/sdpn/IFCExamplesTest.log"
            log.file.maxSize = 10 * 1024 // KB
            log.file.retainedFiles = 5 // keep five old logs around
        }
        val results = TestRunner.runOn(TestData.examplesSuite,"/tmp")
        val removed = results.collect{case (_,Left(x)) => x.removed}.sum
        val suspected = results.collect{case (_,Left(x)) => x.suspected}.sum
        val errors = results.collect{case (_,Right(x)) => 1}.sum
        
        log.info("Finished test run removed %s edges of a total of %s suspected edges in %s applications (%s with errors).", removed, suspected, results.size,errors)
        printf("Finished test run removed %s edges of a total of %s suspected edges in %s applications (%s with errors).\n", removed, suspected, results.size, errors)

    }
}
