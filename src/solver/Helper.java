package solver;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

import pta.PTA;
import soot.Body;
import soot.Local;
import soot.MethodOrMethodContext;
import soot.PointsToAnalysis;
import soot.PointsToSet;
import soot.Scene;
import soot.SootClass;
import soot.SootField;
import soot.SootMethod;
import soot.Unit;
import soot.Value;
import soot.jimple.ArrayRef;
import soot.jimple.AssignStmt;
import soot.jimple.DefinitionStmt;
import soot.jimple.IdentityStmt;
import soot.jimple.IfStmt;
import soot.jimple.InstanceFieldRef;
import soot.jimple.InvokeExpr;
import soot.jimple.InvokeStmt;
import soot.jimple.ParameterRef;
import soot.jimple.SpecialInvokeExpr;
import soot.jimple.StaticFieldRef;
import soot.jimple.StaticInvokeExpr;
import soot.jimple.VirtualInvokeExpr;
import soot.jimple.toolkits.callgraph.CallGraph;
import soot.jimple.toolkits.callgraph.Targets;
import soot.toolkits.graph.ExceptionalUnitGraph;
import soot.toolkits.graph.UnitGraph;

public class Helper {
	
	public static void reset() {
		getInstance().class2def.clear();
		getInstance().method2def.clear();
		getInstance().method2CFG.clear();
		getInstance().method2Reachable.clear();
		getInstance().unit2method.clear();
		getInstance().mappingUnit2Method();
	}
	
	private static Helper instance = null;
	
	public static Helper getInstance() {
		if (instance == null)
			instance = new Helper();
		return instance;
	}
	
	private Helper() {
		this.class2def = new HashMap<>();
		this.method2def = new HashMap<>();
		this.method2CFG = new HashMap<>();
		this.method2Reachable = new HashMap<>();
		
		//create a mapping, so that later, we can get a unit's enclosing method
		this.unit2method = new HashMap<>();		
		mappingUnit2Method(); 
	}
	
	@Deprecated
	private void mappingUnit2Method_deprecated() {
		SootClass main_class = Scene.v().getMainClass();
		SootMethod main_method = main_class.getMethodByName("main");
		
		for (SootClass cls : Scene.v().getApplicationClasses()) {
			System.out.println(cls.getName());
		}
		
		//Mapping all methods in main class, regardless of reachability.
		//TODO : This depends on requirement as to if ignoring unreachable parts.
		for (SootMethod m : main_class.getMethods()) {
			if (m.hasActiveBody()) {
				Body body = m.retrieveActiveBody();
				for (Unit u : body.getUnits()) {
					unit2method.put(u, m);
				}
			}
		}
		
		//Mapping all reachable methods.
		for (SootMethod m : getReachable(main_method)) {
			//System.out.println(m.getName());
			if (m.hasActiveBody()) {
				Body body = m.retrieveActiveBody();
				for (Unit u : body.getUnits()) {
					unit2method.put(u, m);
				}
			}
		}
	}
	
	/** Mapping all methods in all classes, regardless of reachability. */
	private void mappingUnit2Method() {
		for (SootClass soot_class : Scene.v().getClasses()) {
			for (SootMethod m : soot_class.getMethods()) {
				if (m.hasActiveBody()) {
					Body body = m.retrieveActiveBody();
					for (Unit u : body.getUnits()) {
						unit2method.put(u, m);
					}
				}
			}
		}
	}
	
	/**
	 * Given a method m, get all forward reachable nodes from m on Call Graph.
	 */
	public HashSet<SootMethod> getReachable(SootMethod m) {
		if (method2Reachable.containsKey(m)) {
			return method2Reachable.get(m); //cache hits
		}
		
		CallGraph cg = getSootCallGraph(); //cache misses
		HashSet<SootMethod> reachable = new HashSet<>();
		ArrayList<SootMethod> nxtRound = new ArrayList<>();
		ArrayList<SootMethod> curRound = new ArrayList<>();
		curRound.add(m);
		reachable.add(m);
		while(curRound.isEmpty() == false) {
			nxtRound.clear();
			for (SootMethod src : curRound) {
				Iterator<MethodOrMethodContext> Itgt = new Targets(cg.edgesOutOf(src));
				while (Itgt.hasNext()) {
					SootMethod tgt = (SootMethod)Itgt.next();
					if (reachable.contains(tgt) == false) {
						reachable.add(tgt);
						nxtRound.add(tgt);
					}
				}
			}
			ArrayList<SootMethod> tmp = curRound;
			curRound = nxtRound;
			nxtRound = tmp;
		}
		method2Reachable.put(m, reachable);
		return reachable;
	}
	
	/**
	 * Given a unit cur and a CFG cfg , get all backward reachable units from cur on cfg.
	 */
	public HashSet<Unit> getBKReachable(Unit cur, UnitGraph cfg) {
		HashSet<Unit> reachable = new HashSet<>();
		ArrayList<Unit> nxtRound = new ArrayList<>();
		ArrayList<Unit> curRound = new ArrayList<>();
		curRound.add(cur);
		while(curRound.isEmpty() == false) {
			nxtRound.clear();
			for (Unit u : curRound) {
				for (Unit pre : cfg.getPredsOf(u)) {
					if (reachable.contains(pre))
						continue;
					reachable.add(pre);
					nxtRound.add(pre);
				}
			}
			ArrayList<Unit> tmp = curRound;
			curRound = nxtRound;
			nxtRound = tmp;
			
		}
		return reachable;
	}
	
	public CallGraph getCallGraph() {
		if (flag_druid_analysis) 
			return getDruidCallGraph();
		else
			return getSootCallGraph();
	}
	
	private CallGraph getSootCallGraph() {
		return Scene.v().getCallGraph();
	}
	
	private CallGraph getDruidCallGraph() {
		return RunDruid.getCallGraph();
	}
	
	private PTA getDruidPTA() {
		return RunDruid.getPTA();
	}
	
	public void printOutEdge(String class_name, String method_name) {
		CallGraph cg = getSootCallGraph();
		SootMethod src;
		try {
			src = Scene.v().getSootClass(class_name).getMethodByName(method_name);
		} catch (Exception e) {
			return;
		}
		Iterator<MethodOrMethodContext> targets = new Targets(cg.edgesOutOf(src));
		while (targets.hasNext()) {
			SootMethod tgt = (SootMethod) targets.next();
			System.out.println(src + " may call " + tgt);
		}
	}
	
	public void printJimple(SootClass soot_class) {
		for (SootMethod m : soot_class.getMethods()) {
			printJimple(m);
		}
	}
	
	public void printJimple(SootMethod soot_method) {
		if (!soot_method.hasActiveBody()) {
			System.out.println("Method" + soot_method.getSignature() + " has NO Active Body.");
		} else {
			System.out.println("Method" + soot_method.getSignature() + " has Active Body:");
			System.out.println(soot_method.retrieveActiveBody());
		}
	}
	
	public void printDeref(SootClass soot_class) {
		for (SootMethod m : soot_class.getMethods()) {		
			printDeref(m);
		}
	}
	
	public void printDeref(SootMethod soot_method) {
		int iDeref = 0;
		System.out.println("Method" + soot_method.getName());
		if (soot_method.hasActiveBody()) {
			for (Unit u : soot_method.retrieveActiveBody().getUnits()) {
				System.out.println(u);
				if (isGetField(u)) {
					System.out.println("Found a LOAD!");
					System.out.println("BASE = " + getBase(u));
					System.out.print("Deref #" + ++iDeref);
					System.out.println(" @ln:" + u.getJavaSourceStartLineNumber());
				}
				else if (isPutField(u)) {
					System.out.println("Found a STORE!");
					System.out.println("BASE = " + getBase(u));
					System.out.print("Deref #" + ++iDeref);
					System.out.println(" @ln:" + u.getJavaSourceStartLineNumber());
				}
				else if (isInvoke(u)) {
					System.out.println("Found a INVOKE(ONLY)!");
					System.out.println("BASE = " + getBase(u));
					System.out.print("Deref #" + ++iDeref);
					System.out.println(" @ln:" + u.getJavaSourceStartLineNumber());
				}
				else if (isInvokeAssign(u)) {
					System.out.println("Found a INVOKE(ASSIGN)!");
					System.out.println("BASE = " + getBase(u));
					System.out.print("Deref #" + ++iDeref);
					System.out.println(" @ln:" + u.getJavaSourceStartLineNumber());
				}
				else if (isArrayGetField(u)) {
					System.out.println("Found a ArrayGetField!");
					System.out.println("BASE = " + getBase(u));
					System.out.print("Deref #" + ++iDeref);
					System.out.println(" @ln:" + u.getJavaSourceStartLineNumber());
				}
				else if (isArrayPutField(u)) {
					System.out.println("Found a ArrayPutField!");
					System.out.println("BASE = " + getBase(u));
					System.out.print("Deref #" + ++iDeref);
					System.out.println(" @ln:" + u.getJavaSourceStartLineNumber());
				}
				System.out.println();
			}
		}
	}
	
	public int getNumDeref(SootClass soot_class) {
		int res = 0;
		for (SootMethod m : soot_class.getMethods()) {
			int numDerefOfMethod = getNumDeref(m);
			res = res + numDerefOfMethod;
		}
		return res;
	}
	
	public int getNumDeref(SootMethod soot_method) {
		int res = 0;
		if (soot_method.hasActiveBody()) {
			if (isSafeLib(soot_method)) return 0;
			for (Unit u : soot_method.retrieveActiveBody().getUnits()) {
				if (isDeref(u)) {
					++res;
				}
			}
		}
		return res;
	}
	
	
	
	public ArrayList<Unit> getAllDeref(SootClass soot_class) {
		if (class2def.containsKey(soot_class)) { //cache hits
			return class2def.get(soot_class);
		}

		ArrayList<Unit> res = new ArrayList<Unit>(); //cache misses
		for (SootMethod m : soot_class.getMethods()) {
			ArrayList<Unit> subRes = getAllDeref(m);
			res.addAll(subRes);
		}
		class2def.put(soot_class, res);
		return res;
	}
	
	public ArrayList<Unit> getAllDeref(SootMethod soot_method) {
		if (method2def.containsKey(soot_method)) { //cache hits
			return method2def.get(soot_method);
		}
		
		ArrayList<Unit> res = new ArrayList<Unit>(); //cache misses
		if (soot_method.hasActiveBody()) {
			for (Unit u : soot_method.retrieveActiveBody().getUnits()) {
				if (isSafeLib(soot_method)) continue;
				if (isDeref(u)) {
					res.add(u);
				}
			}
		}
		method2def.put(soot_method, res);
		return res;
	}

	public boolean isSafeLib(SootMethod m) {
		if (m.getName().endsWith("toString"))
			return false;
		return m.getDeclaringClass().getName().startsWith("java.")
				|| m.getDeclaringClass().getPackageName().startsWith("java.");
	}
	
	public boolean isDeref(Unit u) {
		boolean isDereference = isGetField(u) 
								|| isPutField(u) 
								|| isInvoke(u) 
								|| isInvokeAssign(u)
								|| isArrayGetField(u)
								|| isArrayPutField(u);
								
		if (isDereference) {
			if (!isSysCall(u))
				return true;
		}
		return false;
	}
	
	public Value getBase(Unit u) {
		if (isGetField(u)) {
			return getGetFieldBase(u);
		}
		else if (isPutField(u)) {
			return getPutFieldBase(u);
		}
		else if (isInvoke(u)) {
			return getInvokeBase(u);
		}
		else if (isInvokeAssign(u)) {
			return getInvokeAssignBase(u);
		}
		else if (isArrayGetField(u)) {
			return getArrayGetFieldBase(u);
		}
		else if (isArrayPutField(u)) {
			return getArrayPutFieldBase(u);
		}
		return null;
	}
	
	/*
	 * E.g. System.out.println, java.io.PrintStream
	 */
	public boolean isSysCall(Unit u) {
		if (isInvoke(u)) {
			InvokeStmt inv = (InvokeStmt) u;
			InvokeExpr inv_expr = inv.getInvokeExpr();
			SootMethod m = inv_expr.getMethod();
			SootClass cls = m.getDeclaringClass();
			if (m.getName().endsWith("toString"))
				return false;
			return cls.getName().startsWith("java");
		}
		else if (isInvokeAssign(u)) {
			Value rval = ((AssignStmt) u).getRightOp();
			InvokeExpr inv_expr = (InvokeExpr) rval;
			SootMethod m = inv_expr.getMethod();
			SootClass cls = m.getDeclaringClass();
			if (m.getName().endsWith("toString"))
				return false;
			return cls.getName().startsWith("java");
		}
		return false;
	}
	
	/**
	 * GetField is in the form: x = y.z
	 */
	public boolean isGetField(Unit u) {
		if (u instanceof AssignStmt) {
			Value rval = ((AssignStmt) u).getRightOp();
			return rval instanceof InstanceFieldRef;
		}
		return false;
	}
	
	/**
	 * PutField is in the form: y.z = x
	 */
	public boolean isPutField(Unit u) {
		if (u instanceof AssignStmt) {
			Value lval = ((AssignStmt) u).getLeftOp();
			return lval instanceof InstanceFieldRef;
		}
		return false;
	}
	
	/**
	 * StaticPutField is in the form: Y.z = x where Y is a class
	 * StaticPutField itself is not a dereference,
	 * however it requires to be handled,
	 * in order to analyze the dereference of the static field.
	 */
	public boolean isStaticPutField(Unit u) {
		if (u instanceof AssignStmt) {
			Value lval = ((AssignStmt) u).getLeftOp();
			return lval instanceof StaticFieldRef;
		}
		return false;
	}
	
	/**
	 * Invoke is in the form: y.foo()
	 * We only care virtual invoke (and disregard special invoke and static invoke).
	 */
	public boolean isInvoke(Unit u) {
		if (u instanceof InvokeStmt) {
			InvokeExpr inv = ((InvokeStmt) u).getInvokeExpr();
			return inv instanceof VirtualInvokeExpr;
		}
		return false;
	}
	
	/**
	 * InvokeAssign is in the form: x = y.foo()
	 * We only care virtual invoke (and disregard special invoke and static invoke).
	 */
	public boolean isInvokeAssign(Unit u) {
		if (u instanceof AssignStmt) {
			Value rval = ((AssignStmt) u).getRightOp();
			return rval instanceof VirtualInvokeExpr;
		}
		return false;
	}
	
	/**
	 * ArrayGetField is in the form: x = y[1]
	 */
	public boolean isArrayGetField(Unit u) {
		if (u instanceof AssignStmt) {
			Value rval = ((AssignStmt) u).getRightOp();
			return rval instanceof ArrayRef;
		}
		return false;
	}
	
	/**
	 * ArrayGetField is in the form: y[1] = x
	 */
	public boolean isArrayPutField(Unit u) {
		if (u instanceof AssignStmt) {
			Value lval = ((AssignStmt) u).getLeftOp();
			return lval instanceof ArrayRef;
		}
		return false;
	}

	
	public Value getGetFieldBase(Unit u) {
		if (u instanceof AssignStmt) {
			Value rval = ((AssignStmt) u).getRightOp();
			if (rval instanceof InstanceFieldRef) {
				return ((InstanceFieldRef) rval).getBase();
			}
		}
		return null;
	}
	
	public Value getPutFieldBase(Unit u) {
		if (u instanceof AssignStmt) {
			Value lval = ((AssignStmt) u).getLeftOp();
			if (lval instanceof InstanceFieldRef) {
				return ((InstanceFieldRef) lval).getBase();
			}
		}
		return null;
	}
	
	public SootField getPutFieldFld(Unit u) {
		if (u instanceof AssignStmt) {
			Value lval = ((AssignStmt) u).getLeftOp();
			if (lval instanceof InstanceFieldRef) {
				return ((InstanceFieldRef) lval).getField();
			}
		}
		return null;
	}
	
	public SootField getStaticPutFieldFld(Unit u) {
		if (u instanceof AssignStmt) {
			Value lval = ((AssignStmt) u).getLeftOp();
			if (lval instanceof StaticFieldRef) {
				return ((StaticFieldRef) lval).getField();
			}
		}
		return null;
	}
	
	public Value getInvokeBase(Unit u) {
		if (u instanceof InvokeStmt) {
			InvokeExpr inv = ((InvokeStmt) u).getInvokeExpr();
			if (inv instanceof VirtualInvokeExpr) {
				return ((VirtualInvokeExpr) inv).getBase();
			}
		}
		return null;
	}
	
	public Value getInvokeAssignBase(Unit u) {
		if (u instanceof AssignStmt) {
			Value rval = ((AssignStmt) u).getRightOp();
			if (rval instanceof VirtualInvokeExpr) {
				return ((VirtualInvokeExpr) rval).getBase();
			}
		}
		return null;
	}
	
	public Value getArrayGetFieldBase(Unit u) {
		if (u instanceof AssignStmt) {
			Value rval = ((AssignStmt) u).getRightOp();
			if (rval instanceof ArrayRef) {
				return ((ArrayRef) rval).getBase();
			}
		}
		return null;
	}
	
	public Value getArrayPutFieldBase(Unit u) {
		if (u instanceof AssignStmt) {
			Value lval = ((AssignStmt) u).getLeftOp();
			if (lval instanceof ArrayRef) {
				return ((ArrayRef) lval).getBase();
			}
		}
		return null;
	}
	
	public boolean isCallSite(Unit u) {
		if (u instanceof DefinitionStmt) {
			Value rv = ((DefinitionStmt) u).getRightOp();
			return rv instanceof InvokeExpr;
		}
		return u instanceof InvokeStmt;
	}
	
	/**
	 * Control-flow graph with exception handling considered
	 */
	public UnitGraph getCFG(SootMethod m) {
		if (method2CFG.containsKey(m)) { //cache hits
			return method2CFG.get(m);
		}
		
		UnitGraph cfg = null; //cache misses
		if (m.hasActiveBody()) {
			Body body = m.retrieveActiveBody();
			cfg = new ExceptionalUnitGraph(body);
		}
		method2CFG.put(m, cfg);
		return cfg;
	}
	
	public SootMethod getMethod(Unit u) {
		return unit2method.get(u);
	}
	
	public UnitGraph getCFG(Unit u) {
		SootMethod m = getMethod(u);
		if (m == null) {
			return null;
		}
		UnitGraph cfg = getCFG(m);
		return cfg;
	}
	
	public Unit getCFGEntry(UnitGraph cfg) {
		if (cfg == null) 
			return null;
		List<Unit> heads = cfg.getHeads();
		if (heads.isEmpty())
			return null;
		/*//for debugging
		if (heads.size() > 1)
			System.err.println("More than one head!!! " + cfg.getBody().getMethod().getName());*/
		return heads.iterator().next();
	}
	
	public Unit getCFGEntry(SootMethod m) {
		if (m == null)
			return null;
		UnitGraph cfg = getCFG(m);
		if (cfg == null) {
			/*//for debugging
			System.err.println("Cannot get CFG!!! " + m.getName());*/
			return null;
		}
		return getCFGEntry(cfg);
	}
	
	public Unit getCFGEntry(Unit u) {
		if (u == null)
			return null;
		SootMethod m = getMethod(u);
		if (m == null) {
			/*//for debugging
			System.err.println("Cannot get CFG!!! " + u);*/
			return null;
		}
		return getCFGEntry(m);
	}
	
	public PointsToAnalysis getPTA() {
		if (flag_druid_analysis)
			return this.getDruidPTA();
		else
			return Scene.v().getPointsToAnalysis();
	}
	
	public List<ParameterRef> getAllParameterRefs(SootMethod m) {
		ArrayList<ParameterRef> res = new ArrayList<>();
		if (m == null || !m.hasActiveBody()) 
			return res;
		for (Unit u : m.getActiveBody().getUnits()) {
			if (u instanceof IdentityStmt) {
				Value lv = ((IdentityStmt) u).getLeftOp();
				Value rv = ((IdentityStmt) u).getRightOp();
				if (lv instanceof Local
				 && rv instanceof ParameterRef)
					res.add((ParameterRef) rv);
			}
		}
		return res;
	}
	
	public boolean alias(Local o1, Local o2) {
		PointsToSet pts1 = getPTA().reachingObjects(o1);
		PointsToSet pts2 = getPTA().reachingObjects(o2);
		return pts1.hasNonEmptyIntersection(pts2);
	}
	
	public void setDruidAnalysisFlag() {
		flag_druid_analysis = true;
	}
	
	private boolean flag_druid_analysis = false;
	
	//Buffer that stores the mapping from method to all reachable methods.
	private HashMap<SootMethod, HashSet<SootMethod>> method2Reachable;
	
	//Buffer that stores a mapping from class to deref.
	private HashMap<SootClass, ArrayList<Unit>> class2def;
	
	//Buffer that stores a mapping from class to deref.
	private HashMap<SootMethod, ArrayList<Unit>> method2def;
	
	//Buffer that stores a mapping from unit to method.
	private HashMap<Unit, SootMethod> unit2method;
	 
	//Buffer that stores a mapping from method to CFG.
	private HashMap<SootMethod, UnitGraph> method2CFG;
	
}
