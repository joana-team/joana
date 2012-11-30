package edu.kit.joana.ifc.wala.sdpn.benchmark

class AppResult(
        val setting: Setting) extends Serializable {

    var timeJSDG: Long = 0
    var numberExpected: Int = 0
    
    private var ifr: List[IFResult] = Nil
    private[benchmark] def registerIFR(r: IFResult) { ifr ::= r }

    def timeDPN = ifr filter (_.error.isEmpty) map (_.time) sum
    def timeXSB = ifr filter (_.error.isEmpty) map (_.timeXSB) sum

    def maxTimeDPN = if (ifr filter (_.error.isEmpty) isEmpty) 0 else ifr filter (_.error.isEmpty) map (_.time) max
    def maxTimeXSB = if (ifr filter (_.error.isEmpty) isEmpty) 0 else ifr filter (_.error.isEmpty) map (_.timeXSB) max

    def numberChecked = ifr size
    def numberNotChecked = ifr.size - numberChecked
    def numberPossible = ifr filter (x => x.error.isEmpty && x.possible) size
    def numberImpossible = ifr filter (x => x.error.isEmpty && !x.possible) size

    def numberWithOverwrite = ifr filter (x => x.error.isEmpty && x.overwrite && x.regularField) size
    def numberPossibleWithOverwrite = ifr filter (x => x.error.isEmpty && x.possible && x.overwrite && x.regularField) size
    def numberImpossibleWithOverwrite = ifr filter (x => x.error.isEmpty && !x.possible && x.overwrite && x.regularField) size

    def numberWithoutOverwrite = ifr filter (x => x.error.isEmpty && !x.overwrite && x.regularField) size
    def numberPossibleWithoutOverwrite = ifr filter (x => x.error.isEmpty && x.possible && !x.overwrite && x.regularField) size
    def numberImpossibleWithoutOverwrite = ifr filter (x => x.error.isEmpty && !x.possible && !x.overwrite && x.regularField) size

    def numberRegular = ifr filter (x => x.error.isEmpty && x.regularField) size
    def numberPossibleRegular = ifr filter (x => x.error.isEmpty && x.possible && x.regularField) size
    def numberImpossibleRegular = ifr filter (x => x.error.isEmpty && !x.possible && x.regularField) size

    def numberNonRegular = ifr filter (x => x.error.isEmpty && !x.regularField) size
    def numberPossibleNonRegular = ifr filter (x => x.error.isEmpty && x.possible && !x.regularField) size
    def numberImpossibleNonRegular = ifr filter (x => x.error.isEmpty && !x.possible && !x.regularField) size

    def numberWithLocks = ifr filter (_.lockNumber > 0) size
    def numberPossibleWithLocks = ifr filter (x => x.error.isEmpty && x.possible && x.lockNumber > 0) size
    def numberImpossibleWithLocks = ifr filter (x => x.error.isEmpty && !x.possible && x.lockNumber > 0) size

    def numberWithoutLocks = ifr filter (_.lockNumber == 0) size
    def numberPossibleWithoutLocks = ifr filter (x => x.error.isEmpty && x.possible && x.lockNumber == 0) size
    def numberImpossibleWithoutLocks = ifr filter (x => x.error.isEmpty && !x.possible && x.lockNumber == 0) size

    def numberError = ifr filter (x => x.error.isDefined) size

    def maxLocks = if (ifr.isEmpty) 0 else ifr map (_.lockNumber) max
    def maxSize = if (ifr.isEmpty) 0 else ifr map (_.dpnSize) max
    def anyWithLock = numberWithLocks > 0

    def ifResults = ifr

    var totalTime: Long = -1
    var error: Option[String] = None
    var stackTrace: Option[String] = None

    def setError(e:String,trace:String) {error=Some(e);stackTrace=Some(trace)}
    
    def secs(time: Long) = "%d.%03ds".format(time / 1000, time % 1000)

    def makeString(withIndividual: Boolean = true): String = {
        val sb = new StringBuilder()
        import sb.{ append => out }
        def outln(s: Any) = { out(s); out("\n") }
        outln("----------- Setup ------------")
        outln(setting.makeString)
        if (error.isDefined) {
            outln("----------- ERROR ------------")
            outln(error.get)
            outln(stackTrace.get)
        }
        outln("------ Overall result --------")
        outln("Expected:     " + numberExpected)
        outln("Checked:      " + numberChecked)
        outln("Not checked:  " + numberNotChecked)
        outln("Possible:     " + numberPossible)
        outln("Impossible:   " + numberImpossible)
        outln("Error:        " + numberError)
        outln(" -- Locks --")
        outln("With locks:            " + numberWithLocks)
        outln("W/o locks:             " + numberWithoutLocks)
        outln("Possible with locks:   " + numberPossibleWithLocks)
        outln("Impossible with locks: " + numberImpossibleWithLocks)
        outln("Possible w/o locks:    " + numberPossibleWithoutLocks)
        outln("Impossible w/o locks:  " + numberImpossibleWithoutLocks)
        outln(" -- Regular --")
        outln("On regular fields:                " + numberRegular)
        outln("On non regular fields:            " + numberNonRegular)
        outln("Possible on regular fields:       " + numberPossibleRegular)
        outln("Impossible on regular fields:     " + numberImpossibleRegular)
        outln("Possible on non regular fields:   " + numberPossibleNonRegular)
        outln("Impossible on non regular fields: " + numberImpossibleNonRegular)
        outln(" -- Overwrite (only regular fields) --")
        outln("With overwrite:            " + numberWithOverwrite)
        outln("W/o overwrite:             " + numberWithoutOverwrite)
        outln("Possible with overwrite:   " + numberPossibleWithOverwrite)
        outln("Impossible with overwrite: " + numberImpossibleWithOverwrite)
        outln("Possible w/o overwrite:    " + numberPossibleWithoutOverwrite)
        outln("Impossible w/o overwrite:  " + numberImpossibleWithoutOverwrite)
        outln(" -- Times & sizes --")
        outln("Max locks:      " + maxLocks)
        outln("Max size:       " + maxSize)
        outln("Total time:     " + secs(totalTime))
        outln("Time jsdg:      " + secs(timeJSDG))
        outln("Time DPN:       " + secs(timeDPN))
        outln("Time xsb:       " + secs(timeXSB))
        outln("Max time DPN:   " + secs(maxTimeDPN))
        outln("Max time xsb:   " + secs(maxTimeXSB))
        if (withIndividual) {
            outln("-------- Individual results ------------")
            for (r <- ifr)
                outln(r.makeString)
        }
        outln("------ End of Result ---------")
        sb.toString
    }
}

class IFResult(
        var appResult: AppResult,
        var srcNode: String,
        var snkNode: String,
        var edge: String) extends Serializable {
    appResult.registerIFR(this)
    var dpnSize: Int = -1
    var lockNumber: Int = -1
    var overwrite: Boolean = false
    var regularField: Boolean = false
    var possible: Boolean = true
    var timeXSB: Long = -1
    var time: Long = -1
    var error: Option[String] = None
    var stackTrace: Option[String] = None

    def setError(e:String,trace:String) {error=Some(e);stackTrace=Some(trace)}

    def secs(time: Long) = "%d.%03ds".format(time / 1000, time % 1000)

    def makeString: String = {
        if (error.isDefined) {
            "Error " + error.get + " for edge: " + edge
        } else {
            (if (possible) "possible for " else "impossible for ") +
                (if (regularField) "regular field " else "not regular field ") +
                " dpn size: " + dpnSize +
                " lock number: " + lockNumber +
                (if (overwrite) " overwrite" else " no overwrite") +
                " time xsb: " + secs(timeXSB) +
                " time total: " + secs(time) +
                " edge: " + edge
        }
    }
}