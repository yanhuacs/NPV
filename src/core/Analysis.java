package core;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Set;

import adt.BkReachbleLineNum;
import adt.Constraint;
import adt.DataflowFact;
import adt.DataflowFactMapping;
import adt.Disjunct;
import adt.SinglePath;
import solver.Helper;
import core.TransferFunc;
import solver.Solver;
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

public class Analysis {
	
	public Analysis(String main_class_name) {
		this.main_class_name = main_class_name;
		runSoot();
		
		helper = solver.Helper.getInstance();
		dfm = new DataflowFactMapping();
		bkln = new BkReachbleLineNumMapping();
	}
	
	public solver.Helper getHelper() {
		return helper;
	}
	
	protected solver.Helper helper;
	
	private void runSoot() {
		soot.G.reset();
		String[] sootArgs = getSootConfiguration();
		soot.Main.main(sootArgs);
		System.out.println("CallGraph Size = " + Scene.v().getCallGraph().size());
	}
	
	private String[] getSootConfiguration() {
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
		return sootArgs;
	}
	
	public String getMainClassName() {
		return main_class_name;
	}
	
	public SootClass getMainClass() {
		return Scene.v().getMainClass();
	}
	
	private String main_class_name = null;
	private String java_libs = "C:\\Java\\jre6\\lib\\rt.jar";
	//private String java_libs = "C:\\Java\\jre1.8.0\\lib\\rt.jar";
	/*private String java_libs = "C:\\Java\\jre6\\lib\\rt.jar;"
			+ "C:\\Java\\jre6\\lib\\jce.jar;"
			+ "C:\\Java\\jre6\\lib\\jsse.jar";*/

	
	
	public void computeFixPoint_SLOW(Unit cur, UnitGraph cfg, TransferFunc tf) {
		boolean isChanged = false;
		Set<Unit> visited = new HashSet<Unit>();
		do {
			visited.clear();
			isChanged = bkCFGTraversal(cur, cfg, tf, visited);
		} while (isChanged);
	}
	
	//Depth-first traversal on CFG backward from a program location.
	public boolean bkCFGTraversal(Unit cur, UnitGraph cfg, TransferFunc tf, Set<Unit> visited) {
		boolean isChanged = false;
		if (visited.contains(cur)) {
			return isChanged;
		}
		visited.add(cur);
		
		DataflowFact IN = dfm.getIN(cur);
		DataflowFact oldOUT = dfm.getOUT(cur);
		DataflowFact newOUT = tf.transfer(cur, IN, oldOUT);
		isChanged = ! oldOUT.equals(newOUT);
		
		for (Unit pre : cfg.getPredsOf(cur)) {
			bkCFGTraversal(pre, cfg, tf, visited);
		}
		return isChanged;
	}
	
	public DataflowFactMapping getDataflowFact() {
		return dfm;
	}
	
	protected DataflowFactMapping dfm;
	private BkReachbleLineNumMapping bkln;
	
	/**
	 * Intra-procedural fix-point algorithm
	 */
	public void computeFixPoint(Unit cur, UnitGraph cfg, TransferFunc tf) {
		dfm.clear();
		worklistAlg(cur, cfg, tf);
		
		Unit entry = cfg.getHeads().iterator().next();
		dfm.getOUT(entry).solve();
	}
	
	/**
	 * Intra-procedural fix-point algorithm implementation
	 */
	public void worklistAlg(Unit u, UnitGraph cfg, TransferFunc tf) {
		LinkedList<Unit> worklist = new LinkedList<>();
		worklist.addLast(u);
		
		Value lv = NullConstant.v();
		Value rv = helper.getBase(u);
		boolean isEqual = true;
		int ln = u.getJavaSourceStartLineNumber();
		Constraint assumption = new Constraint(lv, rv, isEqual, ln);
		
		Disjunct d = new Disjunct(assumption);
		dfm.getIN(u).add(d);
		while (worklist.isEmpty() == false) {
			Unit cur = worklist.removeFirst();
			DataflowFact curIN = dfm.getIN(cur);
			DataflowFact curOUT_old = dfm.getOUT(cur);
			DataflowFact curOUT_new = tf.transfer(cur, curIN, curOUT_old);
			
			//TODO: should be weakest condition, instead of equality
			boolean isChanged = curOUT_new.isStrictWeakerThan(curOUT_old);
			dfm.setOUT(cur, curOUT_new);
			if (isChanged) {
				for (Unit pre : cfg.getPredsOf(cur)) {
					DataflowFact preIN = new DataflowFact();
					for (Unit suc : cfg.getSuccsOf(pre)) {
						DataflowFact sucOUT = dfm.getOUT(suc);
						preIN.addAll(sucOUT);
					}
					dfm.setIN(pre, preIN);
					worklist.addLast(pre);
				}
			}
		}
		
		//Print dataflow fact for the entry node of CFG.
		System.out.println("=======Head======");
		for (Unit head: cfg.getHeads()) {
			DataflowFact IN  = dfm.getIN(head);
			DataflowFact OUT = dfm.getOUT(head);
			Solver.getInstance().solve(IN);
			Solver.getInstance().solve(OUT);
			System.out.println("Stmt:  " + head + " @ln: " + head.getJavaSourceStartLineNumber());
			System.out.println("IN  :  " + IN);
			System.out.println("OUT :  " + OUT);
		}
		System.out.println("=================\n");
		
		System.out.println(dfm.toString());
	}
	
	
	
	public void computeFixPoint(Unit cur, UnitGraph cfg, KeepBkReachableLineNum tf) {
		bkln.clear();
		worklistAlg(cur, cfg, tf);
	}
	
	public void worklistAlg(Unit u, UnitGraph cfg, KeepBkReachableLineNum tf) {
		LinkedList<Unit> worklist = new LinkedList<>();
		//HashSet<Unit> bkReachable = helper.getBKReachable(u, cfg);
		//worklist.addAll(bkReachable);
		worklist.addLast(u);
		SinglePath sp = new SinglePath();
		bkln.getIN(u).add(sp);
		while (worklist.isEmpty() == false) {
			Unit cur = worklist.removeFirst();
			BkReachbleLineNum curIN = bkln.getIN(cur);
			BkReachbleLineNum curOUT_old = bkln.getOUT(cur);
			BkReachbleLineNum curOUT_new = tf.transfer(cur, curIN, curOUT_old);
			boolean isChanged = ! curOUT_old.equals(curOUT_new);
			bkln.setOUT(cur, curOUT_new);
			if (isChanged) {
				for (Unit pre : cfg.getPredsOf(cur)) {
					BkReachbleLineNum preIN = new BkReachbleLineNum();
					for (Unit suc : cfg.getSuccsOf(pre)) {
						BkReachbleLineNum sucOUT = bkln.getOUT(suc);
						preIN.addAll(sucOUT);
					}
					bkln.setIN(pre, preIN);
					worklist.addLast(pre);
				}
			}	
		}
		System.out.println(bkln);
	}
}
