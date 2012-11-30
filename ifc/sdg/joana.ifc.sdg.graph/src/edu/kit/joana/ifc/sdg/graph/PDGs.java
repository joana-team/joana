/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.ifc.sdg.graph;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class PDGs {
//	public static String path = "/data1/giffhorn/pdgs/";
	public static String path = "/afs/info.uni-karlsruhe.de/user/giffhorn/Desktop/pdgs/";
//	public static String path = "/afs/info.uni-karlsruhe.de/user/giffhorn/Desktop/pdgs/vanilla/";

	public static String[] pdgs = {
		path+"conc.TimeTravel.pdg",                  // 0
		path+"conc.bb.ProducerConsumer.pdg",         // 1
		path+"conc.ds.DiskSchedulerDriver.pdg",      // 2
		path+"conc.ac.AlarmClock.pdg",               // 3
		path+"conc.dp.DiningPhilosophers.pdg",       // 4
		path+"conc.lg.LaplaceGrid.pdg",              // 5
		path+"conc.sq.SharedQueue.pdg",              // 6
		path+"conc.auto.EnvDriver.pdg",              // 7
		path+"conc.kn.Knapsack5.pdg",				 // 8
		path+"conc.cliser.kk.Main.pdg",              // 9
		path+"conc.daisy.DaisyTest.pdg",             // 10
		path+"conc.cliser.dt.Main.pdg",              // 11

		path+"Logger.pdg",                           // 12
		path+"Maza.pdg",                             // 13
		path+"Barcode.pdg",                          // 14
		path+"Guitar.pdg",                           // 15
		path+"J2MESafe.pdg",                         // 16
		path+"HyperM.pdg",                           // 17
		path+"Podcast.pdg",                          // 18
		path+"GoldenSMS_KeyManagement.pdg",          // 19
		path+"GoldenSMS_Message.pdg",                // 20
		path+"GoldenSMS_Reception.pdg",              // 21
		path+"Cellsafe.pdg",                         // 22
//		path+"MobileCube.pdg",                       // 23

//		path+"JRemCntl.pdg",                         // 23
//		path+"KeePass.pdg",                          // 24
//		path+"OneTimePass.pdg",                      // 25
	};

	public static String[] javagrande = {
		path+"javagrande/section1.JGFBarrierBench.pdg",         // 0
		path+"javagrande/section1.JGFForkJoinBench.pdg",        // 1
		path+"javagrande/section1.JGFSyncBench.pdg",            // 2
		path+"javagrande/section2.JGFCryptBenchSizeC.pdg",      // 3
		path+"javagrande/section2.JGFLUFactBenchSizeC.pdg",     // 4
		path+"javagrande/section2.JGFSeriesBenchSizeC.pdg",     // 5
		path+"javagrande/section2.JGFSORBenchSizeC.pdg",        // 6
		path+"javagrande/section2.JGFSORBenchSizeC.pdg",        // 7
		path+"javagrande/section2.JGFSORBenchSizeC.pdg",		 // 8
		path+"javagrande/section2.JGFSparseMatmultBenchSizeC.pdg", // 9
		path+"javagrande/section3.JGFMolDynBenchSizeB.pdg",     // 10
		path+"javagrande/section3.JGFMonteCarloBenchSizeB.pdg" // 11
	};

	public static String[] pdgs2 = {
		path+"one/conc.TimeTravel.pdg",                // 0
		path+"one/conc.bb.ProducerConsumer.pdg",       // 1
		path+"one/conc.ds.DiskSchedulerDriver.pdg",    // 2
		path+"one/conc.lg.LaplaceGrid.pdg",            // 3
		path+"one/conc.ac.AlarmClock.pdg",             // 4

		path+"two/conc.TimeTravel.pdg",                // 5
		path+"two/conc.bb.ProducerConsumer.pdg",       // 6
		path+"two/conc.ds.DiskSchedulerDriver.pdg",    // 7
		path+"two/conc.lg.LaplaceGrid.pdg",            // 8
		path+"two/conc.ac.AlarmClock.pdg",             // 9

		path+"three/conc.TimeTravel.pdg",              // 10
		path+"three/conc.bb.ProducerConsumer.pdg",     // 11
		path+"three/conc.ds.DiskSchedulerDriver.pdg",  // 12
		path+"three/conc.lg.LaplaceGrid.pdg",          // 13
		path+"three/conc.ac.AlarmClock.pdg"            // 14
	};

	public static String[] vanillaPDGs = {
		path+"conc.TimeTravel.pdg",                // 0
		path+"conc.bb.ProducerConsumer.pdg",       // 1
		path+"conc.ds.DiskSchedulerDriver.pdg",    // 2
		path+"conc.lg.LaplaceGrid.pdg",            // 3
		path+"conc.ac.AlarmClock.pdg",             // 4
	};

	public static final String TIME_TRAVEL = "TIME_TRAVEL";
	public static final String PROD_CONS = "PROD_CONS";
	public static final String DINING_PHILS = "DINING_PHILS";
	public static final String ALARM_CLOCK = "ALARM_CLOCK";
	public static final String LAPLACE_GRID = "LAPLACE_GRID";
	public static final String SHARED_QUEUE = "SHARED_QUEUE";
	public static final String DAISY_TEST = "DAISY_TEST";
	public static final String KNOCK_KNOCK = "KNOCK_KNOCK";
	public static final String DAY_TIME = "DAY_TIME";
	public static final String DISK_SCHED = "DISK_SCHED";
	public static final String ENV_DRIVER = "ENV_DRIVER";
	public static final String LOGGER = "LOGGER";
	public static final String MAZA = "MAZA";
	public static final String BARCODE = "BARCODE";
	public static final String GUITAR = "GUITAR";
	public static final String J2MESAFE = "J2MESAFE";
	public static final String PODCAST = "PODCAST";
	public static final String CELLSAFE = "CELLSAFE";
	public static final String GOLDEN_SMS_KEY = "GOLDEN_SMS_KEY";
	public static final String GOLDEN_SMS_MESS = "GOLDEN_SMS_MESS";
	public static final String GOLDEN_SMS_REC = "GOLDEN_SMS_REC";
	public static final String HYPER_M = "HYPER_M";
//	public static final String JREM_CNTL = "JREM_CNTL";
//	public static final String KEE_PASS = "KEE_PASS";
//	public static final String ONE_TIME_PASS = "ONE_TIME_PASS";

	private static Map<String, String> map;
	static {
	    HashMap<String, String>tempMap = new HashMap<String, String>();
	    tempMap.put("TIME_TRAVEL", path + "conc.TimeTravel.pdg");
	    tempMap.put("PROD_CONS", path + "conc.bb.ProducerConsumer.pdg");
	    tempMap.put("DINING_PHILS", path + "conc.dp.DiningPhilosophers.pdg");
	    tempMap.put("ALARM_CLOCK", path + "conc.ac.AlarmClock.pdg");
	    tempMap.put("LAPLACE_GRID", path + "conc.lg.LaplaceGrid.pdg");
	    tempMap.put("SHARED_QUEUE", path + "conc.sq.SharedQueue.pdg");
	    tempMap.put("DAISY_TEST", path + "conc.daisy.DaisyTest.pdg");
	    tempMap.put("DAY_TIME", path + "conc.cliser.dt.Main.pdg");
	    tempMap.put("KNOCK_KNOCK", path + "conc.cliser.kk.Main.pdg");
	    tempMap.put("DISK_SCHED", path + "conc.TimeTravel.pdg");
	    tempMap.put("ENV_DRIVER", path + "conc.auto.EnvDriver.pdg");
	    tempMap.put("LOGGER", path + "Logger.pdg");
	    tempMap.put("MAZA", path + "Maza.pdg");
	    tempMap.put("BARCODE", path + "Barcode.pdg");
	    tempMap.put("GUITAR", path + "Guitar.pdg");
	    tempMap.put("J2MESAFE", path + "J2MESafe.pdg");
	    tempMap.put("PODCAST", path + "Podcast.pdg");
	    tempMap.put("CELLSAFE", path + "Cellsafe.pdg");
	    tempMap.put("GOLDEN_SMS_KEY", path + "GoldenSMS_KeyManagement.pdg");
	    tempMap.put("GOLDEN_SMS_MESS", path + "GoldenSMS_Message.pdg");
	    tempMap.put("GOLDEN_SMS_REC", path + "GoldenSMS_Reception.pdg");
	    tempMap.put("HYPER_M", path + "HyperM.pdg");
//	    tempMap.put("JREM_CNTL", path + "JRemCntl.pdg");
//	    tempMap.put("KEE_PASS", path + "KeePass.pdg");
//	    tempMap.put("ONE_TIME_PASS", path + "OneTimePass.pdg");
	    map = Collections.unmodifiableMap(tempMap);
	}

	public static SDG get(String symb_name)
	throws IOException {
		return SDG.readFrom(map.get(symb_name));
	}

	public static PDGIterator getPDGs() {
		return new PDGIterator();
	}

	public static class PDGIterator implements Iterator<SDG> {
		private int ctr = 0;
		private String[] names = {"TIME_TRAVEL", "PROD_CONS", "DINING_PHILS", "ALARM_CLOCK", "LAPLACE_GRID",
								  "SHARED_QUEUE", "DAISY_TEST", "KNOCK_KNOCK", "DAY_TIME", "DISK_SCHED",
								  "ENV_DRIVER", "LOGGER", "MAZA", "BARCODE", "GUITAR", "J2MESAFE",
								  "PODCAST", "CELLSAFE", "GOLDEN_SMS_KEY", "GOLDEN_SMS_MESS", "GOLDEN_SMS_REC",
								  "HYPER_M"};//, "JREM_CNTL", "KEE_PASS", "ONE_TIME_PASS"};

		public boolean hasNext() {
			return ctr < names.length;
		}

		public SDG next() {
			try {
				SDG g = SDG.readFrom(map.get(names[ctr]));
				ctr++;
				return g;

			} catch(IOException e) {
				ctr++; // increment pointer to proceed in the iterator
				throw new RuntimeException(e.getMessage());
			}
		}

		public void remove() {
			; // do nuffin
		}
	}
}
