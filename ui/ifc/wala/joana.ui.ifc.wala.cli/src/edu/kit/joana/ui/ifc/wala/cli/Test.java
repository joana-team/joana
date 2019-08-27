package edu.kit.joana.ui.ifc.wala.cli;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Test {

	public static void main(String[] args) {
		//command("fin", "entry");
		// command("searchEntries");
		//command("searchEntryPointsOutYaml");
		//command("selectEntryPoint bla");
		command("runEntryPoint bla", "run");
		// command("search_entry_points");
	}

	private static void command(String... args) {
		List<String> arguments = new ArrayList<>();
		arguments.addAll(Arrays.asList("-v", "-cp", "example", "console"));
		arguments.addAll(Arrays.asList(args));
		Main.main(arguments.toArray(new String[0]));
	}
}
