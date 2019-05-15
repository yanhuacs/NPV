package core;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Set;

import adt.Constraint;
import adt.DataflowFact;
import adt.DataflowFactMapping;
import adt.Disjunct;
import solver.Helper;
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

public class IntraNullPtrAnalysis{
	
	public IntraNullPtrAnalysis(SootMethod m) {
		this.helper = Helper.getInstance();
		this.cfg = helper.getCFG(m); 
		this.env = new DataflowFactMapping();
		this.tf = IntraNullPtrTransFunc.getInstance();
		this.tf.init(cfg, env);
	}
	
	protected Helper helper;
	protected IntraNullPtrTransFunc tf;
	protected DataflowFactMapping env;
	protected UnitGraph cfg;
	
	public Helper getHelper() {
		return helper;
	}
	
	public DataflowFactMapping getDataflowFact() {
		return env;
	}
	
	
	/**
	 * Intra-procedural fix-point algorithm
	 */
	public void computeFixPoint(Unit cur) {
		env.clear();
		worklistAlg(cur);
		
		Unit entry = cfg.getHeads().iterator().next();
		env.getOUT(entry).solve();
	}
	
	/**
	 * Intra-procedural fix-point algorithm implementation
	 */
	public void worklistAlg(Unit u) {
		LinkedList<Unit> worklist = new LinkedList<>();
		worklist.addLast(u);
		
		Value lv = NullConstant.v();
		Value rv = helper.getBase(u);
		boolean isEqual = true;
		int ln = u.getJavaSourceStartLineNumber();
		Constraint assumption = new Constraint(lv, rv, isEqual, ln);
		
		Disjunct d = new Disjunct(assumption);
		env.getIN(u).add(d);
		while (worklist.isEmpty() == false) {
			Unit cur = worklist.removeFirst();
			DataflowFact curIN = env.getIN(cur);
			DataflowFact curOUT_old = env.getOUT(cur);
			DataflowFact curOUT_new = tf.transfer(cur, curIN, curOUT_old);
			
			//TODO: should be weakest condition, instead of equality
			boolean isChanged = curOUT_new.isStrictWeakerThan(curOUT_old);
			env.setOUT(cur, curOUT_new);
			if (isChanged) {
				for (Unit pre : cfg.getPredsOf(cur)) {
					DataflowFact preIN = new DataflowFact();
					for (Unit suc : cfg.getSuccsOf(pre)) {
						DataflowFact sucOUT = env.getOUT(suc);
						preIN.addAll(sucOUT);
					}
					env.setIN(pre, preIN);
					worklist.addLast(pre);
				}
			}
		}
		
		//Print dataflow fact for the entry node of CFG.
		System.out.println("=======Head======");
		for (Unit head: cfg.getHeads()) {
			DataflowFact IN  = env.getIN(head);
			DataflowFact OUT = env.getOUT(head);
			Solver.getInstance().solve(IN);
			Solver.getInstance().solve(OUT);
			System.out.println("Stmt:  " + head + " @ln: " + head.getJavaSourceStartLineNumber());
			System.out.println("IN  :  " + IN);
			System.out.println("OUT :  " + OUT);
		}
		System.out.println("=================\n");
		
		System.out.println(env.toString());
	}
}
