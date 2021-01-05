package edu.kit.joana.ifc.sdg.qifc.qif_interpreter;

import edu.kit.joana.ifc.sdg.graph.SDGNode;
import edu.kit.joana.ifc.sdg.qifc.nildumu.interproc.MethodInvocationHandler;

public class App {

	public static void main(String[] args) {

        Class c = SDGNode.class;
        System.out.println(c.toString());

        MethodInvocationHandler handler = MethodInvocationHandler.createDefault();
        System.out.println(handler.getName());

        System.out.println("Hello, World");
    }
}
