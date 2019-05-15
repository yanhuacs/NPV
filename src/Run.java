
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import core.InterNullPtrAnalysis;
import core.InterNullPtrTransFunc;
import adt.IsSafe;
import adt.OutOfBudget;
import solver.Helper;
import solver.RunDruid;
import soot.Scene;
import soot.SootClass;
import soot.SootMethod;
import soot.Type;
import soot.Unit;

//-jre jre/jre1.6.0_45  -apppath benchmarks/SSA2.jar -mainclass npda.SSA2
//-jre jre/jre1.6.0_45 -apppath benchmarks/checker3.jar -mainclass VC.vctest
//-jre jre/jre1.6.0_45 -apppath benchmarks/checker2.jar -mainclass VC.vcchecker
//python runJA-npda -apppath benchmarks/dacapo-bench/antlr.jar -mainclass dacapo.antlr.Main | tee dacapo.antlr.log 
//python runJA-npda -apppath benchmarks/dacapo-bench/bloat.jar -mainclass dacapo.bloat.Main | tee dacapo.bloat.log
//python runJA-npda -apppath benchmarks/dacapo-bench/chart.jar -mainclass dacapo.chart.Main | tee dacapo.chart.log
//python runJA-npda -apppath benchmarks/dacapo-bench/eclipse.jar -mainclass dacapo.eclipse.Main | tee dacapo.eclipse.log
//python runJA-npda -apppath benchmarks/dacapo-bench/fop.jar -mainclass dacapo.fop.Main | tee dacapo.fop.log
//python runJA-npda -apppath benchmarks/dacapo-bench/hsqldb.jar -mainclass dacapo.hsqldb.Main | tee dacapo.hsqldb.log
//python runJA-npda -apppath benchmarks/dacapo-bench/jython.jar -mainclass dacapo.jython.Main | tee dacapo.jython.log
//python runJA-npda -apppath benchmarks/dacapo-bench/luindex.jar -mainclass dacapo.luindex.Main | tee dacapo.luindex.log
//python runJA-npda -apppath benchmarks/dacapo-bench/lusearch.jar -mainclass dacapo.lusearch.Main | tee dacapo.lusearch.log
//python runJA-npda -apppath benchmarks/dacapo-bench/pmd.jar -mainclass dacapo.pmd.Main | tee dacapo.pmd.log
//python runJA-npda -apppath benchmarks/dacapo-bench/xalan.jar -mainclass dacapo.xalan.Main | tee dacapo.xalan.log


public class Run {
	
	public static void main(String[] args) {
		RunDruid.execute(args);
		runRealWorldBenchmarks();
	}
	
	private static ArrayList<String> analyzeList = new ArrayList<>();
	
	private static void setAnalysisList() {
		File file = new File("list.txt");
		try {
			FileInputStream fis = new FileInputStream(file);
			BufferedReader br = new BufferedReader(new InputStreamReader(fis));
			String line = null;
			while((line = br.readLine()) != null) {
				analyzeList.add(line);
			}
		} catch (IOException e) {
			//e.printStackTrace();
		}
	}
	
	private static boolean inAnalysisList(String name) {
		return (analyzeList.size() == 0)
				||
				analyzeList.contains(name);
	}
	
	public static void runRealWorldBenchmarks() {
		/* Set a list of package/class/method to analyze.
		 * If the list is empty, then analyze everything. */
		setAnalysisList();
		
		long timeBegin = System.currentTimeMillis();
		Helper helper = Helper.getInstance();
		Helper.reset();
		int nDeref = 0, num_safe = 0, num_bug = 0, num_maybe = 0;
		bugLineNums = new ArrayList<>();
		safeLineNums = new ArrayList<>();
		maybeLineNums = new ArrayList<>();
		bugUnits = new ArrayList<>();
		safeUnits = new ArrayList<>();
		maybeUnits = new ArrayList<>();
		int ith = 0;
		for (SootClass soot_class : Scene.v().getApplicationClasses()) { //Scene.v().getClasses()
			boolean analyzePackageFlag = inAnalysisList(soot_class.getPackageName());
			boolean analyzeClassFlag = inAnalysisList(soot_class.getName());
			for (SootMethod soot_method : soot_class.getMethods()) {
				boolean analyzeMehtodFlag = inAnalysisList(getMethodSignature(soot_method));
				if (analyzeMehtodFlag == true || analyzePackageFlag == true || analyzeClassFlag == true) {
					nDeref += helper.getNumDeref(soot_method);
					System.out.println("==Jimple== @ " + soot_method.getSignature());
					helper.printJimple(soot_method);
				}
			}
		}

		/* We only care application classes, 
		 * although we can simply switch to analyzing all classes by calling Scene.v().getClasses() */
		for (SootClass soot_class : Scene.v().getApplicationClasses()) { //Scene.v().getClasses()
			boolean analyzePackageFlag = inAnalysisList(soot_class.getPackageName());
			boolean analyzeClassFlag = inAnalysisList(soot_class.getName());
			//if (analyzePackageFlag == false && analyzeClassFlag == false) continue;
			for (SootMethod soot_method : soot_class.getMethods()) {
				if (soot_method == null) continue;
				boolean analyzeMehtodFlag = inAnalysisList(getMethodSignature(soot_method));
				if (analyzeMehtodFlag == false && analyzePackageFlag == false && analyzeClassFlag == false) 
					continue;
				for (Unit deref : helper.getAllDeref(soot_method)) {
					InterNullPtrAnalysis analysis = new InterNullPtrAnalysis(deref);
					InterNullPtrTransFunc.reset();
					System.out.println("[ " + ++ith + " / " + nDeref + " ]");
					System.out.println("- Analyzing deref: " + deref + " @ln: " + deref.getJavaSourceStartLineNumber());
					System.out.println("@PACKAGE: " + soot_class.getPackageName());
					System.out.println("@CLASS: " + soot_class.getName());
					System.out.println("@METHOD: " + soot_method.getName());
					
					PrintStream original = System.out;
				    System.setOut(new PrintStream(new OutputStream() {public void write(int b) {}}));
					analysis.addAssumption(deref);
					IsSafe isSafe = analysis.computeFixPoint_RET(deref);
					if (isSafe == IsSafe.Safe) {
						++num_safe;
						safeLineNums.add(new Integer(deref.getJavaSourceStartLineNumber()));
						safeUnits.add(deref);
					}
					else if (isSafe == IsSafe.Bug) {
						++num_bug;
						bugLineNums.add(new Integer(deref.getJavaSourceStartLineNumber()));
						bugUnits.add(deref);
					}
					else {
						++num_maybe;
						maybeLineNums.add(new Integer(deref.getJavaSourceStartLineNumber()));
						maybeUnits.add(deref);
					}
					System.setOut(original);
					
					System.out.println((isSafe == IsSafe.Safe ? "--   " : "++   ") + isSafe + "\n");
					
					/*//Debug
					if (deref.getJavaSourceStartLineNumber() == 19
							&&
							!helper.isGetField(deref)) {
						InterNullPtrAnalysis analysis_debug = new InterNullPtrAnalysis(deref);
						InterNullPtrTransFunc.reset();
						analysis_debug.addAssumption(deref);
						analysis_debug.computeFixPoint_RET(deref);
						System.out.println("OutOfBudget = " + OutOfBudget.isOutOfBudget());
						helper.printJimple(helper.getMethod(deref));
					}*/
				}
			}
		}
		System.out.println("#Deref  = " + nDeref);
		System.out.println("#Safe   = " + num_safe);
		System.out.println("#Bug    = " + num_bug);
		System.out.println("#Maybe  = " + num_maybe);
		for (Unit u: bugUnits) {
			System.out.println("Bug Deref: " + u + " @ line " + u.getJavaSourceStartLineNumber()
							+ " @METHOD: " + helper.getMethod(u).getName() 
							+ " @CLASS: " + helper.getMethod(u).getDeclaringClass().getName());
		}
		long timeEnd = System.currentTimeMillis();
		System.out.println("Time used = " + (timeEnd - timeBegin) / 1000 + " seconds");
		System.out.println("\n\nTest successful!");
		
		//output bug report to a txt file
		try{
		    PrintWriter writer = new PrintWriter("np_report.txt", "UTF-8");
		    for (int i = 0; i < bugUnits.size(); ++i) {
		    	Unit u = bugUnits.get(i);
		    	writer.println("Warning : " + (i+1) );
		    	writer.println("statment: " + u);
		    	writer.println("line    : " + u.getJavaSourceStartLineNumber());
		    	writer.println("method  : " + helper.getMethod(u).getSubSignature());
		    	writer.println("class   : " + helper.getMethod(u).getDeclaringClass());
		    	writer.println();
		    }
		    //writer.println("\n\n************************\n\n");
		    for (int i = 0; i < maybeUnits.size(); ++i) {
		    	Unit u = maybeUnits.get(i);
		    	writer.println("Unsure  : " + (i+1) );
		    	writer.println("statment: " + u);
		    	writer.println("line    : " + u.getJavaSourceStartLineNumber());
		    	writer.println("method  : " + helper.getMethod(u).getSubSignature());
		    	writer.println("class   : " + helper.getMethod(u).getDeclaringClass());
		    	writer.println();
		    }
		    writer.close();
		} 
		catch (IOException e) {
		   e.printStackTrace();
		}
		
		dumpStatistics();
	}
	
	
	private static void dumpStatistics() {
		Map<SootMethod, Set<Unit>> m2u = new HashMap<SootMethod, Set<Unit>>();
		Helper helper = Helper.getInstance();
		
		HashSet<Unit> report = new HashSet<>();
		report.addAll(bugUnits);
		report.addAll(maybeUnits);
		//for (Unit u : bugUnits) {
		for (Unit u : report) {
			SootMethod m = helper.getMethod(u);
			if (m2u.containsKey(m) == false) {
				Set<Unit> set = new HashSet<>();
				m2u.put(m, set);
			}
			Set<Unit> set = m2u.get(m);
			set.add(u);
		}
		
		System.out.println();
		System.out.println("==================== Statistics ====================");
	    System.out.println("\twrong instructions: ");
	    for (SootMethod m : m2u.keySet()) {
	        System.out.println("\tmethod" + m + " : ");
	        for (Unit u : m2u.get(m)) {
	            System.out.println("\t\t" + u.getJavaSourceStartLineNumber() + " = " + u);
	        }

	    }
	    System.out.println("====================================================");
	}
	
	private static String getMethodSignature(SootMethod method) {
	    StringBuffer ret = new StringBuffer();
	    ret.append(method.getDeclaringClass().toString());
	    ret.append(".");
	    ret.append(method.getName());
	    List<Type> params = method.getParameterTypes();
	    ret.append("(");
	    for(int i = 0; i < params.size(); ++i) {
	        ret.append(((Type)params.get(i)).getEscapedName());
	        if (i < params.size() - 1) {
	            ret.append(",");
	        }
	    }
	    ret.append(")");
	    ret.append(method.getReturnType().toString());
	    return ret.toString().intern();
	}
	
	private static ArrayList<Integer> bugLineNums;
	private static ArrayList<Integer> safeLineNums;
	private static ArrayList<Integer> maybeLineNums;
	
	private static ArrayList<Unit> bugUnits;
	private static ArrayList<Unit> safeUnits;
	private static ArrayList<Unit> maybeUnits;
}
