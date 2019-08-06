package edu.kit.joana.ui.ifc.wala.cli;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Test {

	public static void main(String[] args) {
		command("find_command entry");
		//command("searchEntries");
		command("search_entry_points", "buildSDG");
		//command("search_entry_points");
	}

	private static void command(String... args) {
		List<String> arguments = new ArrayList<>();
		arguments.addAll(Arrays.asList("-cp", "example", "console"));
		arguments.addAll(Arrays.asList(args));
		Main.main(arguments.toArray(new String[0]));
	}
}
