package core;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.Stack;

import adt.AccessPath;
import adt.Constraint;
import adt.DataflowFact;
import adt.DataflowFactMapping;
import adt.Disjunct;
import adt.IsSafe;
import adt.LogicValue;
import adt.NullAssumption;
import adt.OutOfBudget;
import driver.Main;
import solver.Helper;
import solver.Solver;
import soot.Body;
import soot.Local;
import soot.PointsToSet;
import soot.Scene;
import soot.SootClass;
import soot.SootMethod;
import soot.Unit;
import soot.Value;
import soot.jimple.AssignStmt;
import soot.jimple.DefinitionStmt;
import soot.jimple.IfStmt;
import soot.jimple.InvokeExpr;
import soot.jimple.InvokeStmt;
import soot.jimple.NullConstant;
import soot.jimple.ParameterRef;
import soot.jimple.SpecialInvokeExpr;
import soot.jimple.StaticInvokeExpr;
import soot.jimple.ThisRef;
import soot.jimple.ThrowStmt;
import soot.jimple.VirtualInvokeExpr;
import soot.jimple.toolkits.callgraph.CallGraph;
import soot.jimple.toolkits.callgraph.Edge;
import soot.toolkits.graph.UnitGraph;

public class InterNullPtrAnalysis{
	
	public InterNullPtrAnalysis(Unit deref) {
		this(new Stack<SootMethod>(), deref);
	}
	
	@SuppressWarnings("unchecked")
	public InterNullPtrAnalysis(Stack<SootMethod> callStk, Unit deref) {
		this.helper = Helper.getInstance();
		this.cg = helper.getCallGraph();
		this.env = new DataflowFactMapping();
		this.tf = new InterNullPtrTransFunc(this, this.env, deref);
		this.callStk = (Stack<SootMethod>) callStk.clone();
		this.retStk = new Stack<>();
		this.deref = deref;
	}
	
	public InterNullPtrAnalysis(Stack<SootMethod> callStk, SootMethod callee, Unit deref) {
		this(callStk, deref);
		this.callStk.push(callee);
	}
	
	public SootClass getMainClass() {
		return Scene.v().getMainClass();
	}
	
	private InterNullPtrTransFunc tf;
	private Helper helper;
	private CallGraph cg;
	private Stack<SootMethod> callStk;
	private Stack<SootMethod> retStk; //since call-graph is also traversed along return edges
	protected DataflowFactMapping env;
	private Unit deref; //the dereference site being verified
	
	
	private List<Value> callsiteArgs; //The arguments list at which call-site this inter-procedural analysis is invoked.
	private Value callsiteBase; //The base pointer at which call-site this inter-procedural analysis is invoked.
	
	public List<Value> getCallsiteArgs() {
		return callsiteArgs;
	}
	
	public Value getCallsiteBase() {
		return callsiteBase;
	}
	
	public void initCallSiteAnalysis(List<Value> callsiteArgs, Value callsiteBase) {
		this.callsiteArgs = callsiteArgs;
		this.callsiteBase = callsiteBase;
	}
	
	public Stack<SootMethod> getCallStack() {
		return callStk;
	}
	
	public Stack<SootMethod> getRetStack() {
		return retStk;
	}
	
	public Helper getHelper() {
		return helper;
	}
	
	public DataflowFactMapping getEnv() {
		return this.env;
	}
	
	/**
	 * Given a deref site e.g., p.f, add assumption "p == null" to the dataflow fact.
	 */
	public void addAssumption(Unit deref) {
		Value base = helper.getBase(deref);
		NullAssumption assumption = 
				new NullAssumption(NullConstant.v(), base, true, deref.getJavaSourceStartLineNumber());
		//env.getIN(deref).add(new Disjunct(assumption));
		env.getOUT(deref).add(new Disjunct(assumption));
		
		if (base instanceof Local)
			rootPts = helper.getPTA().reachingObjects((Local) base);
		
		rootDeref = deref;
		
		OutOfBudget.setFalse();
		/* heuristics: after "null=null" is found, 
		 * we still need to further the analysis 
		 * for a certain number of steps to improve precision. */
		Solver.resetNumExtendingSteps();
	}
	
	public Unit rootDeref;
	
	public PointsToSet rootPts;
	
	/**
	 * Given a start point, compute fix-point along return-edges on call graph
	 */
	public IsSafe computeFixPoint_RET(Unit u) {
		if (helper.getCFG(u) == null || helper.getCFG(u).getHeads().isEmpty()) {
			System.err.println("Cannot find CFG entry!! " + u);
			return IsSafe.Maybe;  //returning Maybe indicates that deref maybe unsafe (along this dead call-path)
		}
		
		SootMethod m = helper.getMethod(u);
		if (retStk.contains(m)) //avoid recursion
			return IsSafe.Maybe;  //returning Maybe indicates that deref may be unsafe (along this recursive path)
		retStk.push(m);
		
		Unit entry = helper.getCFG(u).getHeads().iterator().next();
		DataflowFact before = env.getOUT(entry).clone();
		computeFixPoint_CALL(u);
		if (OutOfBudget.isOutOfBudget())
			return IsSafe.Maybe;
		DataflowFact after  = env.getOUT(entry);
		Solver.getInstance().solve(after);
		
		/*the code commented out is wrong, shortcut does not apply here, 
		as one safe path does not justify other paths to be safe
		if (after.logicValue == LogicValue.False) //Shortcut: if deref is already proved safe, then no further analysis is required. 
			return true; //returning true indicates that deref is safe*/
		
		//Instead of traditional 2-value logic, we adopt 3-value logic to include the Maybe-Bug situation. 
		IsSafe isSafe = IsSafe.Safe;
		boolean isChanged = after.isStrictWeakerThan(before);
		if (isChanged) {
			Iterator<Edge> I = cg.edgesInto(m);
			while (I.hasNext()) {
				Edge eI = I.next();
				Unit cs = eI.srcUnit();
				System.out.println(cs + " @ " + cs.getJavaSourceStartLineNumber());
				DataflowFact preIN = new DataflowFact();
				Iterator<Edge> J = cg.edgesOutOf(cs);
				while (J.hasNext()) {
					Edge eJ = J.next();
					SootMethod callee = eJ.tgt();
					Unit calleeEntry = helper.getCFGEntry(callee);
					DataflowFact calleeOUT = env.getOUT(calleeEntry);
					DataflowFact callerOUT = calleeOUT.clone();
					
					//Step 0: prepare to substitute actual parameters.
					List<Value> actualINs = null;
					Value base = null;
					if (cs instanceof InvokeStmt) {
						InvokeStmt inv = (InvokeStmt) cs;
						InvokeExpr inv_expr = inv.getInvokeExpr();
						actualINs = inv_expr.getArgs();
						if (inv_expr instanceof VirtualInvokeExpr)
							base = ((VirtualInvokeExpr) inv_expr).getBase();
						else if (inv_expr instanceof SpecialInvokeExpr)
							base = ((SpecialInvokeExpr) inv_expr).getBase();
					}
					else if (cs instanceof DefinitionStmt 
							&& ((DefinitionStmt) cs).getRightOp() instanceof InvokeExpr) {
						InvokeExpr inv_expr = (InvokeExpr) ((DefinitionStmt) cs).getRightOp();
						actualINs = inv_expr.getArgs();
						if (inv_expr instanceof VirtualInvokeExpr)
							base = ((VirtualInvokeExpr) inv_expr).getBase();
						else if (inv_expr instanceof SpecialInvokeExpr)
							base = ((SpecialInvokeExpr) inv_expr).getBase();
					}
					else {
						continue;
						/*//for debugging 
						System.out.println(cs);
						System.err.println(cs);
						throw new RuntimeException("Jimple type for the callsite is not recognized!!!");*/
					}
					
					//Step 1: substitute actual parameters for formal parameters.
			    	if (!actualINs.isEmpty()) {
				    	for (Disjunct d : callerOUT.getDisjunctSet()) {
				    		for (Constraint c : d.getConstraintSet()) {
				    			AccessPath lap = c.getLeftAP();
				    			AccessPath rap = c.getRightAP();
				    			Value lapb = lap.getBase();
				    			Value rapb = rap.getBase();
				    			if (lapb instanceof ParameterRef) {
				    				int idx = ((ParameterRef) lapb).getIndex();
				    				Value aIN = actualINs.get(idx);
				    				lap.setBase(aIN);
				    			}
				    			if (rapb instanceof ParameterRef) {
				    				int idx = ((ParameterRef) rapb).getIndex();
				    				Value aIN = actualINs.get(idx);
				    				rap.setBase(aIN);
				    			}
				    		}
				    	}
			    	}
			    	
			    	//Step 2: substitute base pointer for "this" pointer.
			    	for (Disjunct d : callerOUT.getDisjunctSet()) {
			    		for (Constraint c : d.getConstraintSet()) {
			    			AccessPath lap = c.getLeftAP();
			    			AccessPath rap = c.getRightAP();
			    			Value lapb = lap.getBase();
			    			Value rapb = rap.getBase();
			    			if (lapb instanceof ThisRef)
			    				lap.setBase(base);
			    			if (rapb instanceof ThisRef)
			    				rap.setBase(base);
			    		}
			    	}
			    	
					preIN.addAll(callerOUT);
					if (preIN.getAge() < callerOUT.getAge() + 1)
						preIN.setAge(callerOUT.getAge() + 1);
				}
				env.setOUT(cs, preIN);
				
				//Recursively apply the algorithm to the callers.
				//if the deref is not safe in one of the predecessor (caller), then it is not safe 
				IsSafe isPreSafe = computeFixPoint_RET(cs);
				if (OutOfBudget.isOutOfBudget())
					return IsSafe.Maybe;
				if (isPreSafe == IsSafe.Maybe)
					isSafe = IsSafe.Maybe;

				if (isPreSafe == IsSafe.Bug)  //Shortcut: if deref is already proved a bug, then no further analysis is required. 
					return IsSafe.Bug; //returning false indicates that deref is a bug
			}
		}
		retStk.pop();
		
		/*
		 * Return value explanation:
		 * True:  indicates that deref is safe
		 * False: indicates that the deref is not verified and may be unsafe.*/
		if (cg.edgesInto(m).hasNext()) //if there are callers, the returned result depends on callers' results
			return isSafe;
		else { 
			if (after.logicValue == LogicValue.False)
				return IsSafe.Safe;
			else if (after.logicValue == LogicValue.True)
				return IsSafe.Bug;
			return IsSafe.Maybe;
		}
	}
	
	
	/**
	 * Given a halfway start point u in a method, 
	 * compute fix-point along call-edges on call graph
	 */
	public void computeFixPoint_CALL(Unit u) {
		UnitGraph cfg = helper.getCFG(u);
		LinkedList<Unit> worklist = new LinkedList<>();
		
		if (helper.isCallSite(u)) {
			DataflowFact predIN = env.getOUT(u);
			List<Unit> preds = cfg.getPredsOf(u);
			for (Unit pred : preds) 
				env.setIN(pred, predIN);
			worklist.addAll(preds);
		}
		else 
			//worklist.addLast(u);
		{
			DataflowFact predIN = env.getOUT(u);
			List<Unit> preds = cfg.getPredsOf(u);
			for (Unit pred : preds) 
				env.setIN(pred, predIN);
			worklist.addAll(preds);
		}
			
		while (worklist.isEmpty() == false) {
			Unit cur = worklist.removeFirst();
			if (cur instanceof ThrowStmt) continue; // Consider ThrowStmt terminates the program
			
			DataflowFact curIN = env.getIN(cur);
			DataflowFact curOUT_old = env.getOUT(cur);
			DataflowFact curOUT_new = tf.transfer(cur, curIN, curOUT_old);
			if (curOUT_new.outOfBudget()) {
				OutOfBudget.setTrue();
				break;
			}
			if (InterNullPtrTransFunc.outOfBudget()) {
				OutOfBudget.setTrue();
				break;
			}
			
			//should be weakest condition, instead of equality
			boolean isChanged = curOUT_new.isStrictWeakerThan(curOUT_old); 
			//env.setOUT(cur, curOUT_new);
			if (isChanged) {
				env.setOUT(cur, curOUT_new);
				for (Unit pre : cfg.getPredsOf(cur)) {
					DataflowFact preIN = new DataflowFact();
					for (Unit suc : cfg.getSuccsOf(pre)) {
						DataflowFact sucOUT = env.getOUT(suc);
						preIN.addAll(sucOUT);
						if (preIN.getAge() < sucOUT.getAge() + 1)
							preIN.setAge(sucOUT.getAge() + 1);
					}
					preIN.solve();
					env.setIN(pre, preIN);
					worklist.addLast(pre);
				}
			}
		}
		
		//Print dataflow fact for the entry node of CFG.
		System.out.println("=======Head====== @ " + helper.getMethod(u));
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
	
	/**
	 * Given a callee, compute fix-point along call-edges on call graph
	 */
	public void computeFixPoint_CALL(SootMethod m) {
		UnitGraph cfg = helper.getCFG(m);
		LinkedList<Unit> worklist = new LinkedList<>();
		worklist.addAll(cfg.getTails());
		
		while (worklist.isEmpty() == false) {
			Unit cur = worklist.removeFirst();
			DataflowFact curIN = env.getIN(cur);
			DataflowFact curOUT_old = env.getOUT(cur);
			DataflowFact curOUT_new = tf.transfer(cur, curIN, curOUT_old);
			if (curOUT_new.outOfBudget()) {
				OutOfBudget.setTrue();
				break;
			}
			if (InterNullPtrTransFunc.outOfBudget()) break;
			
			//should be weakest condition, instead of equality
			boolean isChanged = curOUT_new.isStrictWeakerThan(curOUT_old);
			env.setOUT(cur, curOUT_new);
			if (isChanged) {
				for (Unit pre : cfg.getPredsOf(cur)) {
					DataflowFact preIN = new DataflowFact();
					for (Unit suc : cfg.getSuccsOf(pre)) {
						DataflowFact sucOUT = env.getOUT(suc);
						preIN.addAll(sucOUT);
						if (preIN.getAge() < sucOUT.getAge() + 1)
							preIN.setAge(sucOUT.getAge() + 1);
					}
					preIN.solve();
					env.setIN(pre, preIN);
					worklist.addLast(pre);
				}
			}
		}
	}	
}

