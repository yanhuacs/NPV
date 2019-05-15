
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Set;

import soot.Body;
import soot.Scene;
import soot.SootClass;
import soot.SootMethod;
import soot.Unit;
import soot.Value;
import soot.jimple.AssignStmt;
import soot.jimple.IfStmt;
import soot.jimple.NullConstant;
import soot.toolkits.graph.UnitGraph;

public class RunSoot {
	
	public static void execute(String main_class_name) {
		soot.G.reset();
		String[] sootArgs = getSootConfiguration(main_class_name);
		soot.Main.main(sootArgs);
		System.out.println("CallGraph Size = " + Scene.v().getCallGraph().size());
	}
	
	public static void execute_benchmark(String main_class_name, String jar_name) {
		//String main_class_name = "org.sablecc.sablecc.launcher.SableCC";
		//String jar_name = "sablecc.jar;";
		soot.G.reset();
		String[] sootArgs = new String[] { 
				"-cp", // soot class path
				java_libs + java.io.File.pathSeparator + jar_name, 
				"-app", // application mode ?
				"-w", // whole program mode
				"-keep-line-number", "-no-writeout-body-releasing", "-p", // phase
				"cg.spark", // generate call graph
				"enabled", "-allow-phantom-refs", // allow phantom class
				"-src-prec", "c", "-f", // output format
				"J", // none
				"-main-class", // specify main class
				main_class_name, // main class
				main_class_name, // argument class
				"-no-bodies-for-excluded", 
		};
		soot.Main.main(sootArgs);
		System.out.println("CallGraph Size = " + Scene.v().getCallGraph().size());
	}
	
	public static void execute_SableCC() {
		String main_class_name = "org.sablecc.sablecc.launcher.SableCC";//"Inter100";
		soot.G.reset();
		String[] sootArgs = new String[] { 
				"-cp", // soot class path
				java_libs + java.io.File.pathSeparator + "sablecc.jar", 
				"-app", // application mode ?
				"-w", // whole program mode
				"-keep-line-number", "-no-writeout-body-releasing", "-p", // phase
				"cg.spark", // generate call graph
				"enabled", "-allow-phantom-refs", // allow phantom class
				"-src-prec", "c", "-f", // output format
				"J", // none
				"-main-class", // specify main class
				main_class_name, // main class
				main_class_name, // argument class
				"-no-bodies-for-excluded", 
		};
		soot.Main.main(sootArgs);
		System.out.println("CallGraph Size = " + Scene.v().getCallGraph().size());
	}
	
	private static String[] getSootConfiguration(String main_class_name) {
		String[] sootArgs = new String[] { 
				"-cp", // soot class path
				java_libs + java.io.File.pathSeparator + 
				"src" + java.io.File.pathSeparator +
				"tests", 
				"-app", // application mode ?
				"-w", // whole program mode
				"-keep-line-number", "-no-writeout-body-releasing", "-p", // phase
				"cg.spark", // generate call graph
				"enabled", "-allow-phantom-refs", // allow phantom class
				"-src-prec", "c", "-f", // output format
				"J", // none
				"-main-class", // specify main class
				main_class_name, // main class
				main_class_name, // argument class
				"-no-bodies-for-excluded", 
		};
		return sootArgs;
	}
	
	//private static String java_libs = "C:\\Java\\jre6\\lib\\rt.jar;";
	//private static String java_libs = ".\\jre\\jre1.8.0_131\\lib\\rt.jar;";
	private static String java_libs =  String.join(java.io.File.separator, 
							new String[]{".", "jre", "jre1.6.0_45", "lib", "rt.jar"});
	//private String java_libs = "C:\\Java\\jre1.8.0\\lib\\rt.jar";
	/*private String java_libs = "C:\\Java\\jre6\\lib\\rt.jar;"
			+ "C:\\Java\\jre6\\lib\\jce.jar;"
			+ "C:\\Java\\jre6\\lib\\jsse.jar";*/
}
