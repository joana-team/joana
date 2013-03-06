package edu.kit.joana.ui.ifc.wala.rifl.example;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;



public class Sample1 {


	public static void main(final String[] args) throws IOException {
		final BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
		final String h = br.readLine();
		System.out.println(h);
	}
}