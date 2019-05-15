
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import adt.AccessPath;
import adt.DataflowFactMapping;
import core.Analysis;
import core.DumpStmt;
import core.InterNullPtrAnalysis;
import core.InterNullPtrTransFunc;
import core.IntraNullPtrAnalysis;
import core.KeepBkReachableLineNum;
import core.KeepOnlyCurrentAssignment;
import core.KeepReachableAssignment;
import adt.IsSafe;
import adt.LogicValue;
import core.TransferFunc;
import solver.Helper;
import soot.Scene;
import soot.SootClass;
import soot.SootMethod;
import soot.toolkits.graph.ExceptionalUnitGraph;
import soot.toolkits.graph.UnitGraph;
import soot.toolkits.scalar.SimpleLocalDefs;
import soot.Unit;
import soot.Value;
import soot.jimple.AssignStmt;
import soot.Body;

public class RunTest {
	
	public static void main(String[] args) {
		//unitTest();
		//runRealWorldBenchmarks("org.sablecc.sablecc.launcher.SableCC", "sablecc.jar;"); System.exit(1);
		//testInterNullPtrAnalysis_AllDeref("npda.ABC"); System.exit(1);
		testInterNullPtrAnalysis_AllDeref("npda.InstanceOf"); System.exit(1);
		testInterNullPtrAnalysis_AllDeref("npda.Throw"); System.exit(1);
		//testInterNullPtrAnalysis_AllDeref("npda.SSA2"); System.exit(1);
		testInterNullPtrAnalysis_AllDeref("npda.SSA"); System.exit(1);
		testInterNullPtrAnalysis_AllDeref("npda.ShortCircuitEval1"); System.exit(1);
		regressionTest(); System.exit(1);
		//testInterNullPtrAnalysis_AllDeref("npda.Static2"); System.exit(1);
		//testInterNullPtrAnalysis_AllDeref("npda.vctest"); System.exit(1);
		//testInterNullPtrAnalysis_AllDeref("npda.vctest2"); System.exit(1);
		//testInterNullPtrAnalysis(); System.exit(1);
		
		if (args.length < 2) {
			regressionTest();
		}
		else {
			String main_class_name = args[0];
			String jar_name = args[1];
			main_class_name = "org.sablecc.sablecc.launcher.SableCC";
			jar_name = "sablecc.jar;";
			runRealWorldBenchmarks(main_class_name, jar_name);
		}
	}
	
	public static void runRealWorldBenchmarks(String main_class_name, String jar_name) {
		long timeBegin = System.currentTimeMillis();
		//RunSoot.execute_SableCC();
		//String main_class_name = "org.sablecc.sablecc.launcher.SableCC";
		//String jar_name = "sablecc.jar;";
		RunSoot.execute_benchmark(main_class_name, jar_name);
		Helper helper = Helper.getInstance();
		Helper.reset();
		int nDeref = 0, num_safe = 0, num_bug = 0, num_maybe = 0;
		bugLineNums = new ArrayList<>();
		safeLineNums = new ArrayList<>();
		maybeLineNums = new ArrayList<>();
		int ith = 0;
		for (SootClass soot_class : Scene.v().getApplicationClasses()) {
			//helper.printJimple(soot_class);
			nDeref += helper.getNumDeref(soot_class);

			for (Unit deref : helper.getAllDeref(soot_class)) {
				SootMethod m = helper.getMethod(deref);
				if (m == null) continue;
				InterNullPtrAnalysis analysis = new InterNullPtrAnalysis(deref);
				InterNullPtrTransFunc.reset();
				System.out.println(++ith + " - Analyzing deref: " + deref + " @ln: " + deref.getJavaSourceStartLineNumber());
				
				PrintStream original = System.out;
			    System.setOut(new PrintStream(new OutputStream() {public void write(int b) {}}));
				analysis.addAssumption(deref);
				IsSafe isSafe = analysis.computeFixPoint_RET(deref);
				if (isSafe == IsSafe.Safe) {
					++num_safe;
					safeLineNums.add(new Integer(deref.getJavaSourceStartLineNumber()));
				}
				else if (isSafe == IsSafe.Bug) {
					++num_bug;
					bugLineNums.add(new Integer(deref.getJavaSourceStartLineNumber()));
				}
				else {
					++num_maybe;
					maybeLineNums.add(new Integer(deref.getJavaSourceStartLineNumber()));
				}
				System.setOut(original);
				
				System.out.println((isSafe == IsSafe.Safe ? "--   " : "++   ") + isSafe + "\n");
			}
		}
		System.out.println("#Deref  = " + nDeref);
		System.out.println("#Safe   = " + num_safe);
		System.out.println("#Bug    = " + num_bug);
		System.out.println("#Maybe  = " + num_maybe);
		for(Integer i : bugLineNums) {
			System.out.println("Bug Deref @ line " + i);
		}
		long timeEnd = System.currentTimeMillis();
		System.out.println("Time used = " + (timeEnd - timeBegin) / 1000 + " seconds");
		System.out.println("\n\nTest successful!");
	}
	
	public static void testInterNullPtrAnalysis_withoutSysOut(String class_name, 
														ArrayList<Integer> expectedBugLineNums) 
	{
		System.out.println("Regression testing with Program [" + ++regressionID + "]");
		/*System.out.print(++regressionID + " & ");
		System.out.print(class_name.substring(10) + " & ");*/
		
		PrintStream original = System.out;
	    System.setOut(new PrintStream(new OutputStream() {
	                public void write(int b) {
	                    //DO NOTHING
	                }
	            }));
		
	    testInterNullPtrAnalysis_AllDeref(class_name);
	    //testInterNullPtrAnalysis(class_name);
		
		System.setOut(original);
		
		boolean isSuccessful = true;
		for (Integer i : expectedBugLineNums) { 
			if (bugLineNums.contains(i) == false) {
				isSuccessful = false;
				break;
			}
		}
		for (Integer i : bugLineNums) { 
			if (expectedBugLineNums.contains(i) == false) {
				isSuccessful = false;
				break;
			}
		}

		if (isSuccessful) {
			System.out.println("Successful : " + class_name);
			System.out.println();
		}
		else {
			System.out.println("Failed : " + class_name);
			System.out.println("-----------------------------");
			System.out.println("  Expected : " + expectedBugLineNums);
			System.out.println("  Actual   : " + bugLineNums);
			System.out.println("-----------------------------");
			System.out.println();
		}
		
/*		System.out.print(" &    ");
		System.out.print(safeLineNums.size() + maybeLineNums.size() + bugLineNums.size());
		System.out.print(" & ");
		System.out.print(safeLineNums.size() + maybeLineNums.size() + " & ");
		System.out.print(bugLineNums.size() + " & ");
		System.out.print(safeLineNums.size() + " & ");
		System.out.print(bugLineNums.size() + " & ");
		System.out.print(maybeLineNums.size());

		System.out.println("& \\checkmark  \\\\\\hline");*/
	}
	
	private static int regressionID = 0;
	

	public static void unitTest() {
		//testJimple();
		//testCallGraph();
		//testCFG();
		//testDeref();
		//testbkCFGTraversal();
		//testAccessPath();
		//testFixpoint();
		//testIntraNullPtr(); //old version of intra-procedural null-pointer analysis
		//testIntraNullPtrAnalysis(); //new version of intra-procedural null-pointer analysis
		
	}
	
	public static boolean debug = false;
	
	public static void testInterNullPtrAnalysis() {
		String class_name = "npda.vctest";
		//String class_name = "npda.Intra";
		//String class_name = "npda.Array1";
		//String class_name = "npda.Array2";
		//String class_name = "npda.Array3";
		//String class_name = "npda.Array4";
		//String class_name = "npda.If1";
		//String class_name = "npda.If2";
		//String class_name = "npda.If3";
		//String class_name = "npda.Ptr1";
		//String class_name = "npda.Ptr2";
		//String class_name = "npda.Ptr3";
		//String class_name = "npda.Ptr4";
		//String class_name = "npda.Ptr5";
		//String class_name = "npda.Ptr6";
		//String class_name = "npda.Ptr7";
		//String class_name = "npda.Loop1";
		//String class_name = "npda.Loop2";
		//String class_name = "npda.Loop3";
		//String class_name = "npda.Loop4";
		//String class_name = "npda.Loop5";
		//String class_name = "npda.Loop6";
		//String class_name = "npda.Inter1";
		//String class_name = "npda.Inter2";
		//String class_name = "npda.Inter3";
		//String class_name = "npda.Inter4";
		//String class_name = "npda.Inter5";
		//String class_name = "npda.Inter6";
		//String class_name = "npda.Inter7";
		//String class_name = "npda.Inter8";
		//String class_name = "npda.Inter9";
		//String class_name = "npda.Inter10";
		//String class_name = "npda.Inter11";
		//String class_name = "npda.Recur1";
		//String class_name = "npda.Recur2";
		//String class_name = "npda.Recur3";
		//String class_name = "npda.Recur4";
		//String class_name = "npda.LinkedList1";
		//String class_name = "npda.LinkedList2";
		//String class_name = "npda.LinkedList3";
		//String class_name = "npda.RetNull1";
		//String class_name = "npda.RetNull2";
		//String class_name = "npda.RetNull3";
		//String class_name = "npda.Switch1";
		//String class_name = "npda.Switch2";
		//String class_name = "npda.IrrelevantConstraint1";
		//String class_name = "npda.IrrelevantConstraint2";
		//String class_name = "npda.Static1";
		//String class_name = "npda.Static2";
		//String class_name = "npda.Static3";
		
		testInterNullPtrAnalysis(class_name);
	}
	
	public static void testInterNullPtrAnalysis(String class_name) {
		RunSoot.execute(class_name);
		Helper helper = Helper.getInstance();
		Helper.reset();
		SootClass soot_class = Scene.v().getSootClass(class_name);
		helper.printJimple(soot_class);
		int nDeref = helper.getNumDeref(soot_class);
		System.out.println("\n\n************");
		int num_safe = 0, num_bug = 0, num_maybe = 0;
		bugLineNums = new ArrayList<>();
		safeLineNums = new ArrayList<>();
		maybeLineNums = new ArrayList<>();
		for (Unit deref : helper.getAllDeref(soot_class)) {
			SootMethod m = helper.getMethod(deref);
			if (m == null) continue;
			InterNullPtrAnalysis analysis = new InterNullPtrAnalysis(deref);
			InterNullPtrTransFunc.reset();
			System.out.println("\n\n!!! Analyzing deref: " + deref + " @ln: " + deref.getJavaSourceStartLineNumber());
			
			analysis.addAssumption(deref);
			IsSafe isSafe = analysis.computeFixPoint_RET(deref);
			if (isSafe == IsSafe.Safe) {
				++num_safe;
				safeLineNums.add(new Integer(deref.getJavaSourceStartLineNumber()));
				System.out.println("++ SAFE");
			}
			else if (isSafe == IsSafe.Bug) {
				++num_bug;
				bugLineNums.add(new Integer(deref.getJavaSourceStartLineNumber()));
				System.out.println("++ BUG");
			}
			else {
				++num_maybe;
				maybeLineNums.add(new Integer(deref.getJavaSourceStartLineNumber()));
				System.out.println("++ MAYBE");
			}
			System.out.println("\n");
		}
		System.out.println("#Deref  = " + nDeref);
		System.out.println("#Safe   = " + num_safe);
		System.out.println("#Bug    = " + num_bug);
		System.out.println("#Maybe  = " + num_maybe);
		for(Integer i : bugLineNums) {
			System.out.println("Unsafe Deref @ line " + i);
		}
		System.out.println("\n\nTest successful!");
	}
	
	public static void testInterNullPtrAnalysis_AllDeref(String entry_class) {
		RunSoot.execute(entry_class);
		Helper helper = Helper.getInstance();
		Helper.reset();
		int num_safe = 0, num_bug = 0, num_maybe = 0;
		bugLineNums = new ArrayList<>();
		safeLineNums = new ArrayList<>();
		maybeLineNums = new ArrayList<>();
		int nDeref = 0;
		for (SootClass soot_class : Scene.v().getApplicationClasses()) {
			helper.printJimple(soot_class);
		}
		
		for (SootClass soot_class : Scene.v().getApplicationClasses()) {
			//System.out.println("\n\n************");
			nDeref += helper.getNumDeref(soot_class);
			
			for (Unit deref : helper.getAllDeref(soot_class)) {
				SootMethod m = helper.getMethod(deref);
				if (m == null) continue;
				InterNullPtrAnalysis analysis = new InterNullPtrAnalysis(deref);
				InterNullPtrTransFunc.reset();
				System.out.println("\n\n!!! Analyzing deref: " + deref + " @ln: " + deref.getJavaSourceStartLineNumber());
				
				analysis.addAssumption(deref);
				
				PrintStream original = System.out;
				if (!(deref.getJavaSourceStartLineNumber() == 25))
				    System.setOut(new PrintStream(new OutputStream() {public void write(int b) {}}));
				
				adt.IsSafe isSafe = analysis.computeFixPoint_RET(deref);
				
				System.setOut(original);
				
				
				if (isSafe == adt.IsSafe.Safe) {
					++num_safe;
					safeLineNums.add(new Integer(deref.getJavaSourceStartLineNumber()));
					System.out.println("++ SAFE");
				}
				else if (isSafe == adt.IsSafe.Bug) {
					++num_bug;
					bugLineNums.add(new Integer(deref.getJavaSourceStartLineNumber()));
					System.out.println("++ BUG");
				}
				else {
					++num_maybe;
					maybeLineNums.add(new Integer(deref.getJavaSourceStartLineNumber()));
					System.out.println("++ MAYBE");
				}
				//System.out.println("\n");
			}
		}
		System.out.println("#Deref  = " + nDeref);
		System.out.println("#Safe   = " + num_safe);
		System.out.println("#Bug    = " + num_bug);
		System.out.println("#Maybe  = " + num_maybe);
		for(Integer i : bugLineNums) {
			System.out.println("Unsafe Deref @ line " + i);
		}
		System.out.println("\n\nTest successful!");
	}
	
	public static ArrayList<Integer> getBugLineNums() {
		return bugLineNums;
	}
	
	private static ArrayList<Integer> bugLineNums;
	private static ArrayList<Integer> safeLineNums;
	private static ArrayList<Integer> maybeLineNums;
	
	public static void testIntraNullPtrAnalysis() {
		String class_name = "npda.Ptr7";
		RunSoot.execute(class_name);
		Helper helper = Helper.getInstance();
		SootClass soot_class = Scene.v().getSootClass(class_name);
		helper.printJimple(soot_class);
		//analysis.getHelper().printDeref(soot_class);
		int nDeref = helper.getNumDeref(soot_class);
		System.out.println("\n\n************");
		int num_safe = 0, num_unsafe = 0;
		ArrayList<Integer> unsafeLineNums = new ArrayList<>();
		ArrayList<Integer> safeLineNums = new ArrayList<>();
		for (Unit u : helper.getAllDeref(soot_class)) {
			SootMethod m = helper.getMethod(u);
			if (m == null) continue;
			IntraNullPtrAnalysis analysis = new IntraNullPtrAnalysis(m);
			System.out.println("\n\n!!! Analyzing deref: " + u + " @ln: " + u.getJavaSourceStartLineNumber());
			UnitGraph cfg = helper.getCFG(u);
			analysis.computeFixPoint(u);
			if (cfg.getHeads().size() != 1)
				throw new RuntimeException("More than one CFG entry???");
			Unit entry = cfg.getHeads().iterator().next();
			if (analysis.getDataflowFact().getOUT(entry).logicValue == adt.LogicValue.False) {
				++num_safe;
				safeLineNums.add(new Integer(u.getJavaSourceStartLineNumber()));
			}
			else {
				++num_unsafe;
				unsafeLineNums.add(new Integer(u.getJavaSourceStartLineNumber()));
			}
		}
		System.out.println("#Deref  = " + nDeref);
		System.out.println("#Safe   = " + num_safe);
		System.out.println("#Unsafe = " + num_unsafe);
		for(Integer i : unsafeLineNums) {
			System.out.println("Unsafe Deref @ line " + i);
		}
		System.out.println("\n\nTest successful!");
	}
	
	public static void testIntraNullPtr() {
		//String class_name = "test.AccessPath";
		//String class_name = "npda.Intra";
		//String class_name = "npda.Array4";
		//String class_name = "npda.Loop5";
		//String class_name = "npda.ArrayExamples";
		//String class_name = "npda.Ptr6";
		String class_name = "npda.Ptr7";
		//String class_name = "npda.Inter1";
		Analysis analysis = new Analysis(class_name);
		SootClass soot_class = Scene.v().getSootClass(class_name);
		analysis.getHelper().printJimple(soot_class);
		//analysis.getHelper().printDeref(soot_class);
		int nDeref = analysis.getHelper().getNumDeref(soot_class);
		System.out.println("\n\n************");
		//KeepReachableAssignment tf = new KeepReachableAssignment();
		core.IntraNullPtr tf = core.IntraNullPtr.getInstance();
		int num_safe = 0, num_unsafe = 0;
		ArrayList<Integer> unsafeLineNums = new ArrayList<>();
		ArrayList<Integer> safeLineNums = new ArrayList<>();
		for (Unit u : analysis.getHelper().getAllDeref(soot_class)) {
			SootMethod m = analysis.getHelper().getMethod(u);
			if (m == null) continue;
			//if (!m.getName().toString().equals("main")) continue;
			//if (!m.getName().toString().equals("doStuff")) continue;
			System.out.println("\n\n!!! Analyzing deref: " + u + " @ln: " + u.getJavaSourceStartLineNumber());
			UnitGraph cfg = analysis.getHelper().getCFG(u);
			DataflowFactMapping dfm = analysis.getDataflowFact();
			tf.init(cfg, dfm);
			analysis.computeFixPoint(u, cfg, tf);
			if (cfg.getHeads().size() != 1)
				throw new RuntimeException("More than one CFG entry???");
			Unit entry = cfg.getHeads().iterator().next();
			if (dfm.getOUT(entry).logicValue == LogicValue.False) {
				++num_safe;
				safeLineNums.add(new Integer(u.getJavaSourceStartLineNumber()));
			}
			else {
				++num_unsafe;
				unsafeLineNums.add(new Integer(u.getJavaSourceStartLineNumber()));
			}
		}
		System.out.println("#Deref  = " + nDeref);
		System.out.println("#Safe   = " + num_safe);
		System.out.println("#Unsafe = " + num_unsafe);
		for(Integer i : unsafeLineNums) {
			System.out.println("Unsafe Deref @ line " + i);
		}
		for(Integer i : safeLineNums) {
			//System.out.println("Safe Deref @ line " + i);
		}
		System.out.println("\n\nTest successful!");
	}

	public static void testFixpoint() {
		//String class_name = "npda.Intra";
		//String class_name = "npda.Loop6";
		String class_name = "test.AccessPath";
		Analysis analysis = new Analysis(class_name);
		SootClass soot_class = Scene.v().getSootClass(class_name);
		analysis.getHelper().printJimple(soot_class);
		analysis.getHelper().printDeref(soot_class);
		int nDeref = analysis.getHelper().getNumDeref(soot_class);
		System.out.println("\n\n************");
		//TransferFunc tf = new KeepReachableAssignment();
		KeepReachableAssignment tf = new KeepReachableAssignment();
		KeepBkReachableLineNum kbkrln = new KeepBkReachableLineNum();
		for (Unit u : analysis.getHelper().getAllDeref(soot_class)) {
			SootMethod m = analysis.getHelper().getMethod(u);
			if (m == null) {
				System.out.println("\n\nNo method found !!!  " + u);
				continue;
			}
			//if (m.getName().toString().equals("doStuff") == false) continue;
			System.out.println("\n\n!!! Analyzing deref: " + u + " @ln: " + u.getJavaSourceStartLineNumber());
			UnitGraph cfg = analysis.getHelper().getCFG(u);
			SimpleLocalDefs du = new SimpleLocalDefs(cfg); //local def-use chain
			tf.init(du);
			analysis.computeFixPoint(u, cfg, tf);
		}
		System.out.println("#Deref = " + nDeref);
		System.out.println("\n\nTest successful!");
	}
	
	public static void testAccessPath() {
		String class_name = "test.AccessPath";
		Analysis analysis = new Analysis(class_name);
		SootClass soot_class = Scene.v().getSootClass(class_name);
		Helper helper = Helper.getInstance();
		helper.printJimple(soot_class);
		for (SootMethod m : soot_class.getMethods()) {
			if (!m.hasActiveBody()) {
				continue;
			}
			
			if (!m.getName().equals("main"))
				continue;
			
			System.out.println("Method" + m.getName() + " has Active Body:");
			System.out.println(m.retrieveActiveBody());
			
			Body body = m.retrieveActiveBody();
			UnitGraph cfg = new ExceptionalUnitGraph(body);
			SimpleLocalDefs du = new SimpleLocalDefs(cfg); //local def-use chain
			for (Unit u : body.getUnits()) {
				System.out.println("Stmt   : " + u);
				if (u instanceof AssignStmt) {
					Value l = ((AssignStmt) u).getLeftOp();
					Value r = ((AssignStmt) u).getRightOp();
					System.out.println("type L : " + l.getClass());
					AccessPath lap = new AccessPath();
					AccessPath.buildAccessPath(l, u, du, lap);
					System.out.println("L AP   : " + lap);
					
					System.out.println("type R : " + r.getClass());
					AccessPath rap = new AccessPath();
					AccessPath.buildAccessPath(r, u, du, rap);
					System.out.println("R AP   : " + rap);
				}
				System.out.println();
			}
		}
		System.out.println("\n\nTest successful!");
	}
	
	public static void testbkCFGTraversal() {
		String class_name = "test.IfElseDemo";
		class_name = "test.AccessPath";
		Analysis analysis = new Analysis(class_name);
		SootClass soot_class = Scene.v().getSootClass(class_name);
		for (SootMethod m : soot_class.getMethods()) {
			if (!m.hasActiveBody()) {
				continue;
			}
			System.out.println("Method" + m.getName() + " has Active Body:");
			System.out.println(m.retrieveActiveBody());
			
			System.out.println("\nDepth First Traversal:");
			Body body = m.retrieveActiveBody();
			UnitGraph cfg = new ExceptionalUnitGraph(body);
			
			TransferFunc tf = new KeepOnlyCurrentAssignment();
			tf = new DumpStmt();
			for (Unit u : cfg.getTails()) {
				System.out.println("^ from " + u);
				analysis.computeFixPoint_SLOW(u, cfg, tf);
			}
		}
		System.out.println("\n\nTest successful!");
	}

	public static void testDeref() {
		String main_class_name = "test.Deref";
		String other_class_name = "test.A";
		Analysis analysis = new Analysis(main_class_name);
		SootClass main_soot_class = Scene.v().getSootClass(main_class_name);
		SootClass other_soot_class = Scene.v().getSootClass(other_class_name);
		Helper helper = Helper.getInstance();
		helper.printDeref(main_soot_class);
		helper.printDeref(other_soot_class);
		int nDeref = helper.getNumDeref(main_soot_class);
		System.out.println("#Deref = " + nDeref);
		System.out.println("\n\nTest successful!");
	}
	
	public static void testCallGraph() {
		String class_name = "test.CallGraphs";
		Analysis analysis = new Analysis(class_name);
		SootClass soot_class = Scene.v().getSootClass(class_name);
		Helper helper = Helper.getInstance();
		helper.printOutEdge(class_name, "doStuff");
		helper.printJimple(soot_class);
		System.out.println("\n\nTest successful!");
	}
	
	public static void testCFG() {
		String class_name = "test.IfElseDemo";
		Analysis analysis = new Analysis(class_name);
		SootClass soot_class = Scene.v().getSootClass(class_name);
		Helper helper = Helper.getInstance();
		helper.printOutEdge(class_name, "doStuff");
		helper.printJimple(soot_class);
		for (SootMethod m : soot_class.getMethods()) {
			if (!m.hasActiveBody()) {
				continue;
			}
			System.out.println("Method" + m.getName() + " has Active Body:");
			System.out.println(m.retrieveActiveBody());
			
			System.out.println("\nDepth First Traversal:");
			
			Body body = m.retrieveActiveBody();
			UnitGraph cfg = new ExceptionalUnitGraph(body);
			for (Unit u : cfg.getHeads()) {
				dfs(u, cfg, new ArrayList<Unit>());
			}
		}
		System.out.println("\n\nTest successful!");
	}
	
	public static void dfs(Unit cur, UnitGraph cfg, ArrayList<Unit> path) {
		if (cfg.getSuccsOf(cur).isEmpty()) {
			System.out.println("===========PATH ===========");
			for (Unit u : path) {
				System.out.println(u);
			}
			System.out.println();
		}
		else {
			for (Unit suc : cfg.getSuccsOf(cur)) {
				path.add(suc);
				dfs(suc, cfg, path);
				path.remove(path.size()-1);
			}
		}	
	}
	
	public static void testJimple() {
		String main_class_name = "test.CallGraphs";
		main_class_name = "test.AccessPath";
		String java_libs = "C:\\Java\\jre6\\lib\\rt.jar;"
				+ "C:\\Java\\jre6\\lib\\jce.jar;"
				+ "C:\\Java\\jre6\\lib\\jsse.jar";
		/*String[] sootArgs = new String[] { 
				"-cp", // soot class path
				".\\;" + java_libs, "-app", // application mode ?
				"-w", // whole program mode
				"-keep-line-number", "-no-writeout-body-releasing", "-p", // phase
				"jb", "enabled",
				//"cg.spark", "enabled", // generate call graph
				//"-allow-phantom-refs", // allow phantom class
				"-src-prec", "c", "-f", // output format
				"J", // none
				//"-main-class", // specify main class
				//main_class, // main class
				main_class_name, // argument class
				"-no-bodies-for-excluded", 
		};*/
		
		String[] sootArgs = new String[] { 
				"-cp", // soot class path
				"src\\;" + java_libs, "-app", // application mode ?
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
		System.out.println("\n\nTest successful!");
	}
	
	
	
	
	public static void regressionTest() {
		ArrayList<Integer> expectedUnsafeLineNums;

		//49 test cases, with 42 null-ptr bugs in total
		
		expectedUnsafeLineNums = new ArrayList<>(Arrays.asList(22));
		testInterNullPtrAnalysis_withoutSysOut("npda.Intra",
												expectedUnsafeLineNums);
		
		expectedUnsafeLineNums = new ArrayList<>(Arrays.asList(21));
		testInterNullPtrAnalysis_withoutSysOut("npda.If1",
												expectedUnsafeLineNums);
		
		expectedUnsafeLineNums = new ArrayList<>();
		testInterNullPtrAnalysis_withoutSysOut("npda.If2",
												expectedUnsafeLineNums);
		
		expectedUnsafeLineNums = new ArrayList<>(Arrays.asList(27));
		testInterNullPtrAnalysis_withoutSysOut("npda.If3",
												expectedUnsafeLineNums);
		
		expectedUnsafeLineNums = new ArrayList<>(Arrays.asList(40));
		testInterNullPtrAnalysis_withoutSysOut("npda.Switch1",
												expectedUnsafeLineNums);
		
		expectedUnsafeLineNums = new ArrayList<>(Arrays.asList(62));
		testInterNullPtrAnalysis_withoutSysOut("npda.Switch2",
												expectedUnsafeLineNums);
		
		expectedUnsafeLineNums = new ArrayList<>(Arrays.asList(18));
		testInterNullPtrAnalysis_withoutSysOut("npda.Ptr1",
												expectedUnsafeLineNums);
		
		expectedUnsafeLineNums = new ArrayList<>(Arrays.asList(21));
		testInterNullPtrAnalysis_withoutSysOut("npda.Ptr2",
												expectedUnsafeLineNums);
		
		expectedUnsafeLineNums = new ArrayList<>(Arrays.asList(19));
		testInterNullPtrAnalysis_withoutSysOut("npda.Ptr3",
												expectedUnsafeLineNums);
		
		expectedUnsafeLineNums = new ArrayList<>(Arrays.asList(22));
		testInterNullPtrAnalysis_withoutSysOut("npda.Ptr4",
												expectedUnsafeLineNums);
		
		expectedUnsafeLineNums = new ArrayList<>(Arrays.asList(18));
		testInterNullPtrAnalysis_withoutSysOut("npda.Ptr5",
												expectedUnsafeLineNums);
		
		expectedUnsafeLineNums = new ArrayList<>(Arrays.asList(23, 25));
		testInterNullPtrAnalysis_withoutSysOut("npda.Ptr6",
												expectedUnsafeLineNums);
		
		expectedUnsafeLineNums = new ArrayList<>(Arrays.asList(19));
		testInterNullPtrAnalysis_withoutSysOut("npda.Ptr7",
												expectedUnsafeLineNums);
		
		expectedUnsafeLineNums = new ArrayList<>(Arrays.asList(27));
		testInterNullPtrAnalysis_withoutSysOut("npda.IrrelevantConstraint1",
												expectedUnsafeLineNums);
		
		expectedUnsafeLineNums = new ArrayList<>(Arrays.asList(27));
		testInterNullPtrAnalysis_withoutSysOut("npda.IrrelevantConstraint2",
												expectedUnsafeLineNums);
		
		expectedUnsafeLineNums = new ArrayList<>();
		testInterNullPtrAnalysis_withoutSysOut("npda.Loop1",
												expectedUnsafeLineNums);
		
		expectedUnsafeLineNums = new ArrayList<>();
		testInterNullPtrAnalysis_withoutSysOut("npda.Loop2",
												expectedUnsafeLineNums);
		
		expectedUnsafeLineNums = new ArrayList<>(Arrays.asList(18));
		testInterNullPtrAnalysis_withoutSysOut("npda.Loop3",
												expectedUnsafeLineNums);
		
		expectedUnsafeLineNums = new ArrayList<>();
		testInterNullPtrAnalysis_withoutSysOut("npda.Loop4",
												expectedUnsafeLineNums);
		
		expectedUnsafeLineNums = new ArrayList<>(Arrays.asList(19));
		testInterNullPtrAnalysis_withoutSysOut("npda.Loop5",
												expectedUnsafeLineNums);
		
		expectedUnsafeLineNums = new ArrayList<>();
		testInterNullPtrAnalysis_withoutSysOut("npda.Loop6",
												expectedUnsafeLineNums);
		
		expectedUnsafeLineNums = new ArrayList<>(Arrays.asList(41));
		testInterNullPtrAnalysis_withoutSysOut("npda.Array1",
												expectedUnsafeLineNums);
		
		expectedUnsafeLineNums = new ArrayList<>(Arrays.asList(22));
		testInterNullPtrAnalysis_withoutSysOut("npda.Array2",
												expectedUnsafeLineNums);
		
		expectedUnsafeLineNums = new ArrayList<>();
		testInterNullPtrAnalysis_withoutSysOut("npda.Array3",
												expectedUnsafeLineNums);
		
		expectedUnsafeLineNums = new ArrayList<>(Arrays.asList(24, 27));
		testInterNullPtrAnalysis_withoutSysOut("npda.Array4",
												expectedUnsafeLineNums);
		
		expectedUnsafeLineNums = new ArrayList<>();
		testInterNullPtrAnalysis_withoutSysOut("npda.Inter1",
												expectedUnsafeLineNums);
		
		expectedUnsafeLineNums = new ArrayList<>(Arrays.asList(6, 7));
		testInterNullPtrAnalysis_withoutSysOut("npda.Inter2",
												expectedUnsafeLineNums);
		
		expectedUnsafeLineNums = new ArrayList<>();
		testInterNullPtrAnalysis_withoutSysOut("npda.Inter3",
												expectedUnsafeLineNums);
		
		expectedUnsafeLineNums = new ArrayList<>(Arrays.asList(10));
		testInterNullPtrAnalysis_withoutSysOut("npda.Inter4",
												expectedUnsafeLineNums);
		
		expectedUnsafeLineNums = new ArrayList<>(Arrays.asList(19));
		testInterNullPtrAnalysis_withoutSysOut("npda.Inter5",
												expectedUnsafeLineNums);
		
		expectedUnsafeLineNums = new ArrayList<>(Arrays.asList(23));
		testInterNullPtrAnalysis_withoutSysOut("npda.Inter6",
												expectedUnsafeLineNums);
		
		expectedUnsafeLineNums = new ArrayList<>(Arrays.asList(15, 23));
		testInterNullPtrAnalysis_withoutSysOut("npda.Inter7",
												expectedUnsafeLineNums);
		
		expectedUnsafeLineNums = new ArrayList<>(Arrays.asList(14, 23));
		testInterNullPtrAnalysis_withoutSysOut("npda.Inter8",
												expectedUnsafeLineNums);
		
		expectedUnsafeLineNums = new ArrayList<>();
		testInterNullPtrAnalysis_withoutSysOut("npda.Inter9",
												expectedUnsafeLineNums);
		
		expectedUnsafeLineNums = new ArrayList<>();
		testInterNullPtrAnalysis_withoutSysOut("npda.Inter10",
												expectedUnsafeLineNums);
		
		expectedUnsafeLineNums = new ArrayList<>();
		testInterNullPtrAnalysis_withoutSysOut("npda.Inter11",
												expectedUnsafeLineNums);
		
		expectedUnsafeLineNums = new ArrayList<>();
		testInterNullPtrAnalysis_withoutSysOut("npda.Recur1",
												expectedUnsafeLineNums);
		
		expectedUnsafeLineNums = new ArrayList<>();
		testInterNullPtrAnalysis_withoutSysOut("npda.Recur2",
												expectedUnsafeLineNums);
		
		expectedUnsafeLineNums = new ArrayList<>(Arrays.asList(6, 11, 13));
		testInterNullPtrAnalysis_withoutSysOut("npda.Recur3",
												expectedUnsafeLineNums);
		
		expectedUnsafeLineNums = new ArrayList<>(Arrays.asList(12));
		testInterNullPtrAnalysis_withoutSysOut("npda.Recur4",
												expectedUnsafeLineNums);
		
		expectedUnsafeLineNums = new ArrayList<>();
		testInterNullPtrAnalysis_withoutSysOut("npda.Static1",
												expectedUnsafeLineNums);
		
		expectedUnsafeLineNums = new ArrayList<>(Arrays.asList(21));
		testInterNullPtrAnalysis_withoutSysOut("npda.Static2",
												expectedUnsafeLineNums);
		
		expectedUnsafeLineNums = new ArrayList<>(Arrays.asList(22));
		testInterNullPtrAnalysis_withoutSysOut("npda.Static3",
												expectedUnsafeLineNums);
		
		expectedUnsafeLineNums = new ArrayList<>(Arrays.asList(20));
		testInterNullPtrAnalysis_withoutSysOut("npda.LinkedList1",
												expectedUnsafeLineNums);
		
		expectedUnsafeLineNums = new ArrayList<>();
		testInterNullPtrAnalysis_withoutSysOut("npda.LinkedList2",
												expectedUnsafeLineNums);
		
		expectedUnsafeLineNums = new ArrayList<>(Arrays.asList(15));
		testInterNullPtrAnalysis_withoutSysOut("npda.LinkedList3",
												expectedUnsafeLineNums);
		
		expectedUnsafeLineNums = new ArrayList<>(Arrays.asList(12));
		testInterNullPtrAnalysis_withoutSysOut("npda.RetNull1",
												expectedUnsafeLineNums);
		
		expectedUnsafeLineNums = new ArrayList<>(Arrays.asList(11));
		testInterNullPtrAnalysis_withoutSysOut("npda.RetNull2",
												expectedUnsafeLineNums);
		
		expectedUnsafeLineNums = new ArrayList<>(Arrays.asList(11));
		testInterNullPtrAnalysis_withoutSysOut("npda.RetNull3",
												expectedUnsafeLineNums);
		
		
		
		//Additional tests. Not included in the original 49 test cases.
		
		expectedUnsafeLineNums = new ArrayList<>(Arrays.asList(30, 31));
		testInterNullPtrAnalysis_withoutSysOut("npda.vctest",
												expectedUnsafeLineNums);
		
		expectedUnsafeLineNums = new ArrayList<>(Arrays.asList(24, 25));
		testInterNullPtrAnalysis_withoutSysOut("npda.vctest2",
												expectedUnsafeLineNums);
		
		expectedUnsafeLineNums = new ArrayList<>();
		testInterNullPtrAnalysis_withoutSysOut("npda.ABC",
												expectedUnsafeLineNums);
	}
	
}
