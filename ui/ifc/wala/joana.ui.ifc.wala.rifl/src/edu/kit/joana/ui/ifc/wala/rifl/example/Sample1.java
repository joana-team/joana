package edu.kit.joana.ui.ifc.wala.rifl.example;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;



public class Sample1 {


	public static void main(String[] args) throws IOException {
		BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
		String h = br.readLine();
		System.out.println(h);
	}
}