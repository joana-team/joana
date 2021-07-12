import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.input.Out;

public class SmartGrid {

	public static void main(String[] args) {
		new SmartGrid().smartGrid(0);
	}

	void smartGrid(int h) {
		int totalCount = 16;
		int smallCount = 4;
		int mediumCount = 8;
		int largeCount = totalCount - smallCount - mediumCount;

		int smallConsumption = 1;
		int mediumConsumption = 2;
		int largeConsumption = 3;

		int present = h & (-1 >>> 32 - totalCount);
		int globalConsumption = 0;

		for (int i = 0; i < totalCount; ++i) {
			if (((present >> i) & 1) != 0) {
				if (i < smallCount) {
					globalConsumption += smallConsumption;
				} else if (i < smallCount + mediumCount) {
					globalConsumption += mediumConsumption;
				} else {
					globalConsumption += largeConsumption;
				}
			}
		}

		Out.print(globalConsumption);
	}

}