/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package examples;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;

public class Mantel00Page10 {
	// threads
	static class Portfolio extends Thread {
		int[] esOldPrices;
		String[] pfNames;
		int[] pfNums;
		String pfTabPrint;

		public void run() {
			int i = 0;
		    pfNames = getPFNames(); // high
			pfNums = getPFNums();   // high

			while (i < pfNames.length) {
				pfTabPrint += pfNames[i] + "|" + pfNums[i];
				i++;
			}

		}

		int locPF(String name) {
			for (int i = 0; i < pfNames.length; i++) {
				if (pfNames[i].equals(name)) {
					return i;
				}
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
				int j = 0;
		        nwOutBuf.append("getES50");
			    nwOutBuf.flush();                   // low
				String nwIn = nwInBuf.readLine();
				String[] strArr = nwIn.split(":");

				while (j < 50) {
					esName[j] = strArr[2*j];
					esPrice[j] = Integer.parseInt(strArr[2*j+1]);
					j++;
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
		int k = 0;

		public void run() {
			k = 0;
			while (k < 50) {
				int ipf = p.locPF(e.esName[k]);

				if (ipf > 0) {
					st[k] = (p.esOldPrices[k] - e.esPrice[k]) * p.pfNums[ipf];
				} else {
					st[k] = 0;
				}

				k++;
			}
		}
	}

	static class Output extends Thread {
		public void run() {
			int m = 0;
			while (m < 50) {
				while (s.k <= m) { ; } // busy-wait synchronization

				output[m] = m+"|"+e.esName[m]+"|"+e.esPrice[m]+"|"+s.st[m];
				m++;
			}
		}
	}

	static Portfolio p = new Portfolio();
	static EuroStoxx50 e = new EuroStoxx50();
	static Statistics s = new Statistics();
	static Output o = new Output();

	static BufferedWriter nwOutBuf = new BufferedWriter(new OutputStreamWriter(System.out));
	static BufferedReader nwInBuf = new BufferedReader(new InputStreamReader(System.in));

	static String[] output = new String[50];

	public static void main(String[] args) throws Exception {
//		while (true) {
			// get portfolio and eurostoxx50
			p.start();
			e.start();
			p.join();
			e.join();

			// compute statistics and generate output
			s.start();
			o.start();
			s.join();
			o.join();

			// display output
			int n = 0;
			stTabPrint("No.\t | Name\t | Price\t | Profit");
			while (n < 50) {
				stTabPrint(output[n]);
				n++;
			}

			// show commercials
			stTabPrint(e.coShort+"Press # to get more information");
			char key = (char)System.in.read();
			if (key == '#') {
				System.out.println(e.coFull);
				nwOutBuf.append("shownComm:"+e.coOld);
				nwOutBuf.flush();                       // low
			}
//		}
	}

	static void stTabPrint(String str) {
		//System.out.println(str);
	}
}
