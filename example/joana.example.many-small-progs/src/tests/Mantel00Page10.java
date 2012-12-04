/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package tests;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;

public class Mantel00Page10 {
	static class Portfolio extends Thread {
		int[] esOldPrices;
		String[] pfNames;
		int[] pfNums;
		String pfTabPrint;

		public void run() {
		    pfNames = getPFNames(); // high
			pfNums = getPFNums();   // high
			for (int i = 0; i < pfNames.length; i++) {
				pfTabPrint += pfNames[i] + "|" + pfNums[i];
			}
		}

		int locPF(String name) {
			for (int i = 0; i < pfNames.length; i++) {
				if (pfNames[i].equals(name)) { return i; }
			}
			return -1;
		}

		static int[] getES50old() {
			return new int[50];
		}

		static String[] getPFNames() {
			return new String[10];
		}

		static int[] getPFNums() {
			return new int[10];
		}
	}

	static class EuroStoxx50 extends Thread {
		String[] esName = new String[50];
		int[] esPrice = new int[50];
		String coShort;
		String coFull;
		String coOld;

		public void run() {
			try {
		        nwOutBuf.append("getES50");
			    nwOutBuf.flush();                   // low
				String nwIn = nwInBuf.readLine();
				String[] strArr = nwIn.split(":");
				for (int j = 0; j < 50; j++) {
					esName[j] = strArr[2 * j];
					esPrice[j] = Integer.parseInt(strArr[2 * j + 1]);
				}
				// commercials
				coShort = strArr[100];
				coFull = strArr[101];
				coOld = strArr[102];
			} catch (IOException ex) {}
		}
	}

	static class Statistics extends Thread {
		int[] st = new int[50];
		volatile int k = 0;

		public void run() {
			k = 0;
			while (k < 50) {
				int ipf = p.locPF(e.esName[k]);
				if (ipf > 0) {
					set(k, (p.esOldPrices[k] - e.esPrice[k]) * p.pfNums[ipf]);
				} else {
					set(k, 0);
				}
				k++;
			}
		}
		synchronized void set(int k, int value) {
			st[k] = value;
		}
		synchronized int get(int k) {
			return st[k];
		}
	}

	static class Output extends Thread {
		public void run() {
			for (int m = 0; m < 50; m++) {
				while (s.k <= m) { ; } // busy-wait synchronization
				output[m] = m+"|"+e.esName[m]+"|"+e.esPrice[m]+"|"+s.get(m);
			}
		}
	}

	static Portfolio p 				= new Portfolio();
	static EuroStoxx50 e 			= new EuroStoxx50();
	static Statistics s  			= new Statistics();
	static Output o 				= new Output();
	static String[] output 			= new String[50];
	static BufferedWriter nwOutBuf 	=
			new BufferedWriter(new OutputStreamWriter(System.out));
	static BufferedReader nwInBuf 	=
			new BufferedReader(new InputStreamReader(System.in));

	public static void main(String[] args) throws Exception {
		// get portfolio and eurostoxx50
		p.start(); e.start();
		p.join(); e.join();
		// compute statistics and generate output
		s.start(); o.start();
		s.join(); o.join();
		// display output
		stTabPrint("No.\t | Name\t | Price\t | Profit");
		for (int n = 0; n < 50; n++) {
			stTabPrint(output[n]);
		}
		// show commercials
		stTabPrint(e.coShort + "Press # to get more information");
		char key = (char) System.in.read();
		if (key == '#') {
			System.out.println(e.coFull);
			nwOutBuf.append("shownComm:" + e.coOld);
			nwOutBuf.flush();                       // low
		}
	}

	static void stTabPrint(String str) {
	}
}
