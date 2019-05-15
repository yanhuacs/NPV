package core;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

//import fj.data.Option;
import soot.Body;
import soot.Local;
import soot.MethodOrMethodContext;
import soot.NormalUnitPrinter;
import soot.PackManager;
import soot.PhaseOptions;
import soot.Scene;
import soot.SootClass;
import soot.SootMethod;
import soot.Unit;
import soot.UnitPrinter;
import soot.jimple.Jimple;
import soot.jimple.JimpleBody;
import soot.jimple.spark.geom.dataRep.CgEdge;
import soot.jimple.toolkits.callgraph.CallGraph;
import soot.jimple.toolkits.callgraph.Targets;
import soot.options.Options;
import soot.toolkits.graph.ExceptionalUnitGraph;
import soot.toolkits.graph.UnitGraph;
import soot.toolkits.scalar.SimpleLiveLocals;

public class MyCall {
	public static void dumpIR() {
		configure("C:\\Program Files\\Java\\jre6\\lib\\rt.jar;.");
		SootClass sootClass = Scene.v().loadClassAndSupport("test.CallGraphs");
		sootClass.setApplicationClass();

		System.out.println(Scene.v().getMainClass());
		System.out.println(sootClass);
		
		// Retrieve the method and its body
		SootMethod me = sootClass.getMethodByName("doStuff");
		Body bo = me.retrieveActiveBody();
		System.out.println("$$"+bo);
	}
	
	public static void configure(String classpath) {
        //Options.v().set_verbose(false);
        Options.v().set_keep_line_number(true);
        Options.v().set_src_prec(Options.src_prec_class);
        Options.v().set_soot_classpath(classpath);
        Options.v().set_prepend_classpath(true);
        Options.v().set_whole_program(true);
        Options.v().set_main_class("test.CallGraphs");
        //Options.v().set_java_version(8);
        Options.v().set_allow_phantom_refs(true);
        
        Options.v().set_no_writeout_body_releasing(true);
        
        PhaseOptions.v().setPhaseOption("cg.spark", "enabled");
        PhaseOptions.v().setPhaseOption("jb", "on");
        /*PhaseOptions.v().setPhaseOption("bb", "off");
        PhaseOptions.v().setPhaseOption("tag.ln", "on");
        PhaseOptions.v().setPhaseOption("jj.a", "on");
        PhaseOptions.v().setPhaseOption("jj.ule", "on");
*/
        Options.v().set_whole_program(true);
        
        Options.v().set_main_class("test.CallGraphs");
        List<String> pd = new LinkedList<String>(); 
        Options.v().set_process_dir(pd);
        Scene.v().loadClassAndSupport("test.CallGraphs");
        PackManager.v().runPacks();
    }
	public static void main(String[] args) {
		List<String> argsList = new ArrayList<String>(Arrays.asList(args));
		argsList.addAll(Arrays.asList(new String[] { 
				"-cp", 
				"C:\\Program Files\\Java\\jre6\\lib\\rt.jar;.\\",
				"-allow-phantom-refs",
				"-p", "cg.spark", "enabled",
				//"-p", "jb", "enabled",
				"-src-prec",
				"c",
				"-f",
				"J",
				"-w", 
				"-main-class", 
				"test.CallGraphs", // main-class
				"test.CallGraphs", // argument classes
				// "test.A",
				//"-verbose",
				"-no-bodies-for-excluded",
				"-no-writeout-body-releasing",
		}));

		
		/*String javaLibs = "C:\\Java\\jdk1.6.0_45\\jre\\lib\\rt.jar;C:\\Java\\jdk1.6.0_45\\jre\\lib\\jce.jar;C:\\Java\\jdk1.6.0_45\\jre\\lib\\jsse.jar";
		String javaLibs = "C:\\Program Files\\Java\\jre6\\lib\\rt.jar";
        String mainClass = "test.CallGraphs";
        String[] sootArgs = new String[] { "-cp", // soot class path
                                        ".\\" + ";" + javaLibs, //
                                        "-app", // application mode ?
                                        "-w", // whole program mode
                                        "-keep-line-number", "-no-writeout-body-releasing", "-p", // phase
                                        "cg.spark", // generate call graph
                                        "enabled",
                                        "-allow-phantom-refs", // allow phantom class
                                        "-src-prec", "c", "-f", // output format
                                        "J", // none
                                        "-main-class", // specify main class
                                        mainClass, // main class
                                        mainClass, // argument class
                                        "-no-bodies-for-excluded",
        };
        soot.G.reset();
        // ReflectionOptions.v().setInferenceReflectionModel(true);
        soot.Main.main(sootArgs);
		*/
		
		args = argsList.toArray(new String[0]);
		soot.Main.main(args);
		
		//dumpIR();
		
		CallGraph cg = Scene.v().getCallGraph();
		SootMethod src = Scene.v().getMainClass().getMethodByName("doStuff");
		Iterator<MethodOrMethodContext> targets = new Targets(cg.edgesOutOf(src));
		while (targets.hasNext()) {
			SootMethod tgt = (SootMethod)targets.next();
			System.out.println(src + " may call " + tgt);
		}
		System.out.println(cg.size());
		
		
		
		
		
		//System.exit(0);
		

		
		
		Scene.v().loadNecessaryClasses();
		
		for (SootMethod m : Scene.v().getSootClass("test.CallGraphs").getMethods()) {
			System.out.println(Scene.v().getMainClass());

			if (!m.hasActiveBody()) {
				System.out.println("NoActiveBody" + m.getName());
				continue;
			}
			else 
				System.out.println("HasActiveBody" + m.getName());
			
			for(Unit u : m.retrieveActiveBody().getUnits()) {
				System.out.println(u);
			}
			
			//JimpleBody body = Jimple.v().newBody(m);
			//m.setActiveBody(body);
			
			Body b = m.retrieveActiveBody();
			
			System.out.println("=======================================");			
			System.out.println(m.getName());
			
			UnitGraph graph = new ExceptionalUnitGraph(b);
			SimpleLiveLocals sll = new SimpleLiveLocals(graph);
			
			for (Unit u : graph) {
				List<Local> before = sll.getLiveLocalsBefore(u);
				List<Local> after = sll.getLiveLocalsAfter(u);
				UnitPrinter up = new NormalUnitPrinter(b);
				up.setIndent("");
				
				System.out.println("---------------------------------------");			
				u.toString(up);			
				System.out.println(up.output());
				System.out.print("Live in: {");
				String sep = "";
				for (Local l : before) {
					System.out.print(sep);
					System.out.print(l.getName() + ": " + l.getType());
					sep = ", ";
				}
				System.out.println("}");
				System.out.print("Live out: {");
				sep = "";
				for (Local l : after) {
					System.out.print(sep);
					System.out.print(l.getName() + ": " + l.getType());
					sep = ", ";
				}			
				System.out.println("}");			
				System.out.println("---------------------------------------");
			}
			System.out.println("=======================================");
		}

	}
	

}
