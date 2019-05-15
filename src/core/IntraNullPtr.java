package core;

import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import adt.AccessPath;
import adt.Constraint;
import adt.DataflowFact;
import adt.DataflowFactMapping;
import adt.Disjunct;
import adt.Disjunction;
import solver.Solver;
import soot.Body;
import soot.Local;
import soot.PointsToAnalysis;
import soot.PointsToSet;
import soot.Scene;
import soot.SootField;
import soot.Unit;
import soot.Value;
import soot.jimple.AddExpr;
import soot.jimple.AndExpr;
import soot.jimple.ArrayRef;
import soot.jimple.AssignStmt;
import soot.jimple.ConditionExpr;
import soot.jimple.Constant;
import soot.jimple.DefinitionStmt;
import soot.jimple.DivExpr;
import soot.jimple.EqExpr;
import soot.jimple.GeExpr;
import soot.jimple.GotoStmt;
import soot.jimple.GtExpr;
import soot.jimple.IdentityStmt;
import soot.jimple.IfStmt;
import soot.jimple.InstanceFieldRef;
import soot.jimple.InvokeStmt;
import soot.jimple.LeExpr;
import soot.jimple.LengthExpr;
import soot.jimple.LtExpr;
import soot.jimple.MulExpr;
import soot.jimple.NeExpr;
import soot.jimple.NegExpr;
import soot.jimple.NewArrayExpr;
import soot.jimple.NewExpr;
import soot.jimple.NullConstant;
import soot.jimple.NumericConstant;
import soot.jimple.OrExpr;
import soot.jimple.ParameterRef;
import soot.jimple.RemExpr;
import soot.jimple.ReturnStmt;
import soot.jimple.ReturnVoidStmt;
import soot.jimple.ShlExpr;
import soot.jimple.ShrExpr;
import soot.jimple.SpecialInvokeExpr;
import soot.jimple.StaticFieldRef;
import soot.jimple.StaticInvokeExpr;
import soot.jimple.StringConstant;
import soot.jimple.SubExpr;
import soot.jimple.UshrExpr;
import soot.jimple.VirtualInvokeExpr;
import soot.jimple.XorExpr;
import soot.toolkits.graph.ExceptionalUnitGraph;
import soot.toolkits.graph.UnitGraph;
import soot.toolkits.scalar.SimpleLocalDefs;

public class IntraNullPtr extends TransferFunc {
	
	private static IntraNullPtr instance = null;
	
	public static IntraNullPtr getInstance() {
		if (instance == null)
			instance = new IntraNullPtr();
		return instance;
	}
	
	/**
	 * E.g., x = y
	 */
	private boolean isCopy(Unit u) { 
		if (u instanceof AssignStmt) {
			Value lv = ((AssignStmt) u).getLeftOp();
			Value rv = ((AssignStmt) u).getRightOp();
			if (lv instanceof Local
			 && rv instanceof Local)
				return true;
		}
		return false;
	}
	
	/**
	 * E.g., x = \@parameter0: java.lang.String[];
	 */
	private boolean isFormalIn(Unit u) {
		if (u instanceof IdentityStmt) {
			Value lv = ((IdentityStmt) u).getLeftOp();
			Value rv = ((IdentityStmt) u).getRightOp();
			if (lv instanceof Local
			 && rv instanceof ParameterRef)
				return true;
		}
		return false;
	}
	
	/**
	 * E.g., x = null
	 */
    private boolean isNullAssign(Unit u) { 
    	if (u instanceof AssignStmt) {
			Value lv = ((AssignStmt) u).getLeftOp();
			Value rv = ((AssignStmt) u).getRightOp();
			if (lv instanceof Local
			 && rv instanceof NullConstant)
				return true;
		}
    	return false;
    }
    
    /**
     * E.g., temp$12 = <java.lang.System: java.io.PrintStream out>
     */
    private boolean isStaticFldAssign(Unit u) { 
    	if (u instanceof AssignStmt) {
			Value lv = ((AssignStmt) u).getLeftOp();
			Value rv = ((AssignStmt) u).getRightOp();
			if (lv instanceof Local
			 && rv instanceof StaticFieldRef)
				return true;
		}
    	return false;
    }
    
    /**
	 * E.g., x = new A, or x = newarray(A)[100] 
	 */
    private boolean isNewAssign(Unit u) { 
    	if (u instanceof AssignStmt) {
			Value lv = ((AssignStmt) u).getLeftOp();
			Value rv = ((AssignStmt) u).getRightOp();
			if (lv instanceof Local
			 && ( rv instanceof NewArrayExpr || rv instanceof NewExpr))
				return true;
		}
    	return false;
    }
    
    /**
	 * E.g., x = y.z
	 */
    private boolean isGetField(Unit u) { 
    	if (u instanceof AssignStmt) {
			Value rv = ((AssignStmt) u).getRightOp();
			if (rv instanceof InstanceFieldRef)
				return true;
		}
    	return false;
    }
    
    /**
	 * E.g., if temp$8 < 10 goto label04
	 */
    private boolean isIf(Unit u) {
    	return u instanceof IfStmt;
    }
    
    /**
	 * E.g., x = y + z
	 */
	private boolean isExprAssign(Unit u) {
		if (u instanceof AssignStmt) {
			Value lv = ((AssignStmt) u).getLeftOp();
			Value rv = ((AssignStmt) u).getRightOp();
			if (lv instanceof Local
			 && ( rv instanceof Constant // ArithmeticConstant //Constant
					 || rv instanceof AddExpr  // +
					 || rv instanceof SubExpr  // -
					 || rv instanceof MulExpr  // *
					 || rv instanceof DivExpr  // /
					 || rv instanceof RemExpr  // %
					 || rv instanceof ShlExpr  // <<
					 || rv instanceof ShrExpr  // >>
					 || rv instanceof UshrExpr // >>>
					 || rv instanceof XorExpr  // ^
					 || rv instanceof GeExpr   // >=
					 || rv instanceof GtExpr   // >
					 || rv instanceof LeExpr   // <=
					 || rv instanceof LtExpr   // <
					 || rv instanceof AndExpr  // &&
					 || rv instanceof OrExpr   // ||
					 || rv instanceof NegExpr  // !
					 || rv instanceof NeExpr   // !=
					 || rv instanceof LengthExpr // .length
				))
				return true;
		}
		return false;
	}
	
	/**
	 * E.g., x = y[10]
	 */
    private boolean isGetArray(Unit u) { 
    	if (u instanceof AssignStmt) {
			Value rv = ((AssignStmt) u).getRightOp();
			if (rv instanceof ArrayRef)
				return true;
    	}
    	return false;
    }
    
    /**
	 * E.g., x[10] = y
	 */
    private boolean isPutArray(Unit u) { 
    	if (u instanceof AssignStmt) {
			Value lv = ((AssignStmt) u).getLeftOp();
			if (lv instanceof ArrayRef)
				return true;
    	}
    	return false;
    }
    
    /**
	 * E.g., x.y = z
	 */
    private boolean isPutField(Unit u) { 
    	if (u instanceof AssignStmt) {
			Value lv = ((AssignStmt) u).getLeftOp();
			if (lv instanceof InstanceFieldRef)
				return true;
		}
    	return false;
    }
    
    /**
	 * E.g., return, or return $temp1
	 */
    private boolean isReturn(Unit u) { 
    	return u instanceof ReturnStmt || u instanceof ReturnVoidStmt;
    }
    
    /**
     * Invoking constructor.
	 * E.g., specialinvoke this.<java.lang.Object: void <init>()>()
	 */
    private boolean isSpecialInvoke(Unit u) {
    	if (u instanceof InvokeStmt) {
			InvokeStmt inv = (InvokeStmt) u;
			if (inv.getInvokeExpr() instanceof SpecialInvokeExpr)
				return true;
		}
    	return false;
    }
    
    /**
     * Invoking a static method.
	 * E.g., staticinvoke <test.NPTest.Intra1: void doStuff()>()
	 */
    private boolean isStaticInvoke(Unit u) {
    	if (u instanceof InvokeStmt) {
			InvokeStmt inv = (InvokeStmt) u;
			if (inv.getInvokeExpr() instanceof StaticInvokeExpr)
				return true;
		}
    	return false;
    }
    
    /**
     * Invoking a virtual method (member method).
	 * E.g., virtualinvoke temp$0.<test.NPTest.A: void foo()>()
	 */
    private boolean isVirtualInvoke(Unit u) {
    	if (u instanceof InvokeStmt) {
			InvokeStmt inv = (InvokeStmt) u;
			if (inv.getInvokeExpr() instanceof VirtualInvokeExpr)
				return true;
		}
    	return false;
    }
    
    /**
     * Assignment by invoking constructor.
     * Seems impossible, since constructor does not return value,
     * just put it here as a dummy handling function for completeness.
	 * E.g., x = specialinvoke this.<java.lang.Object: void <init>()>()
	 */
    private boolean isSpecialInvokeAssign(Unit u) {
    	if (u instanceof AssignStmt) {
			AssignStmt assign = (AssignStmt) u;
			if (assign.getRightOp() instanceof SpecialInvokeExpr)
				return true;
		}
    	return false;
    }
    
    /**
     * Assignment by invoking a static method.
	 * E.g., x = staticinvoke <test.NPTest.Intra1: void doStuff()>()
	 */
    private boolean isStaticInvokeAssign(Unit u) {
    	if (u instanceof AssignStmt) {
			AssignStmt assign = (AssignStmt) u;
			if (assign.getRightOp() instanceof StaticInvokeExpr)
				return true;
		}
    	return false;
    }
    
    /**
     * Assignment by invoking a virtual method (member method).
	 * E.g., x = virtualinvoke temp$0.<test.NPTest.A: void foo()>()
	 */
    private boolean isVirtualInvokeAssign(Unit u) {
    	if (u instanceof AssignStmt) {
			AssignStmt assign = (AssignStmt) u;
			if (assign.getRightOp() instanceof VirtualInvokeExpr)
				return true;
		}
    	return false;
    }
    
    private boolean isGoto(Unit u) {
		return u instanceof GotoStmt;
	}
    
	@Deprecated
    private void updateEnv_Deprecated(Unit u, Disjunction IN, Disjunction OUT) {
		Value lv, rv;
		if (u instanceof AssignStmt) {
			lv = ((AssignStmt) u).getLeftOp();
			rv = ((AssignStmt) u).getRightOp();
		}
		else if (u instanceof IdentityStmt) {
			lv = ((IdentityStmt) u).getLeftOp();
			rv = ((IdentityStmt) u).getRightOp();
		}
		else {
			throw new RuntimeException("Must be AssignStmt or IdentityStmt!");
		}
		AccessPath replacementLap = new AccessPath(lv);
		AccessPath replacementRap = new AccessPath(rv);
		for (Disjunct d_IN : IN.getDisjunctSet()) {
			Disjunct d_OUT = new Disjunct();
			HashSet<Constraint> cSet_IN = d_IN.getConstraintSet();
			HashSet<Constraint> cSet_OUT = d_OUT.getConstraintSet();
			for (Constraint existing : cSet_IN) {
				AccessPath existingLap = existing.getLeftAP();
				AccessPath existingRap = existing.getRightAP();
				Value replacementLapBase = replacementLap.getBase();
				Value replacementRapBase = replacementRap.getBase();
				LinkedList<SootField> replacementLapFlds = replacementLap.getFields();
				LinkedList<SootField> replacementRapFlds = replacementRap.getFields();
				Value existingRapBase = existingRap.getBase();
				Value existingLapBase = existingLap.getBase();
				LinkedList<SootField> existingRapFlds = existingRap.getFields();
				LinkedList<SootField> existingLapFlds = existingLap.getFields();
				boolean isEqual = existing.isEqual();
				int ln = existing.getLineNum();
				
				/*
				 * TODO: logic disorder: 
				 * need to consider the case when both of left and right need update 
				 */
				
				if (replacementLapFlds.isEmpty() && sameValue(replacementLapBase, existingRapBase)) {
					AccessPath updatedRap = new AccessPath(replacementRapBase);
					LinkedList<SootField> updatedRapFlds = updatedRap.getFields(); 
					updatedRapFlds.addAll(replacementRapFlds);
					updatedRapFlds.addAll(existingRapFlds);
					Constraint updated = new Constraint(existingLap, updatedRap, isEqual, ln);
					cSet_OUT.add(updated);
/*					//for debugging
					System.err.println("---------update----------");
					System.err.println("replacement  : " + new Constraint(lv, rv, true, u.getJavaSourceStartLineNumber()));
					System.err.println("existing     : " + existing);
					System.err.println("updated      : " + updated);
					System.err.println("-------------------------");*/
				}
				else if (replacementLap.equals(existingRap)) {
					Constraint updated = new Constraint(existingLap, replacementRap, isEqual, ln);
					cSet_OUT.add(updated);
/*					//for debugging
					System.err.println("---------update----------");
					System.err.println("replacement : " + new Constraint(lv, rv, true, u.getJavaSourceStartLineNumber()));
					System.err.println("existing    : " + existing);
					System.err.println("updated     : " + updated);
					System.err.println("-------------------------");*/
				}
				
				if (replacementLapFlds.isEmpty() && sameValue(replacementLapBase, existingLapBase)) {
					AccessPath updatedLap = new AccessPath(replacementRapBase);
					LinkedList<SootField> updatedLapFlds = updatedLap.getFields(); 
					updatedLapFlds.addAll(replacementRapFlds);
					updatedLapFlds.addAll(existingLapFlds);
					Constraint updated = new Constraint(updatedLap, existingRap, isEqual, ln);
					cSet_OUT.add(updated);
/*					//for debugging
					System.err.println("---------update----------");
					System.err.println("replacement  : " + new Constraint(lv, rv, true, u.getJavaSourceStartLineNumber()));
					System.err.println("existing     : " + existing);
					System.err.println("updated      : " + updated);
					System.err.println("-------------------------");*/
					
					if (u.getJavaSourceStartLineNumber() == 11) {
						System.err.println("---------update----------");
						System.err.println("replacement  : " + new Constraint(lv, rv, true, u.getJavaSourceStartLineNumber()));
						System.err.println("existing     : " + existing);
						System.err.println("updated      : " + updated);
						System.err.println("-------------------------");
					}
				}
				else if (replacementLap.equals(existingLap)) {
					Constraint updated = new Constraint(replacementLap, existingRap, isEqual, ln);
					cSet_OUT.add(updated);
/*					//for debugging
					System.err.println("---------update----------");
					System.err.println("replacement : " + new Constraint(lv, rv, true, u.getJavaSourceStartLineNumber()));
					System.err.println("existing    : " + existing);
					System.err.println("updated     : " + updated);
					System.err.println("-------------------------");*/
				}
				else {
					cSet_OUT.add(existing);
				}
				
				/*if (u.getJavaSourceStartLineNumber() == 11) {
					System.err.println("@@ " + u);
					System.err.println("@@ " + existing);
					System.err.println("@@ " + replacementLap + " <> " + existingLap);
					System.err.println(replacementLap.equals(existingLap) ? " @@ equal" : " @@ not equal");
					System.err.println(sameValue(replacementLapBase, existingLapBase) ? " @@ equal" : " @@ not equal");
				}*/
			}
			OUT.add(d_OUT);
		}
	}
    
    
    private void updateEnv(Unit u, Disjunction IN, Disjunction OUT) {
		Value lv, rv;
		if (u instanceof AssignStmt) {
			lv = ((AssignStmt) u).getLeftOp();
			rv = ((AssignStmt) u).getRightOp();
		}
		else if (u instanceof IdentityStmt) {
			lv = ((IdentityStmt) u).getLeftOp();
			rv = ((IdentityStmt) u).getRightOp();
		}
		else {
			throw new RuntimeException("Must be AssignStmt or IdentityStmt!");
		}
		
		AccessPath replacementLap = new AccessPath(lv);
		AccessPath replacementRap = new AccessPath(rv);
		Value replacementLapBase = replacementLap.getBase();
		Value replacementRapBase = replacementRap.getBase();
		LinkedList<SootField> replacementLapFlds = replacementLap.getFields();
		LinkedList<SootField> replacementRapFlds = replacementRap.getFields();
		
		if (!replacementLapFlds.isEmpty()) 
			throw new RuntimeException("replacement flds is not empty! PutField should not be handled here!");
		
		for (Disjunct d_IN : IN.getDisjunctSet()) {
			Disjunct d_OUT = new Disjunct();
			HashSet<Constraint> cSet_IN = d_IN.getConstraintSet();
			HashSet<Constraint> cSet_OUT = d_OUT.getConstraintSet();
			for (Constraint existing : cSet_IN) {
				AccessPath existingLap = existing.getLeftAP();
				AccessPath existingRap = existing.getRightAP();
				Value existingRapBase = existingRap.getBase();
				Value existingLapBase = existingLap.getBase();
				LinkedList<SootField> existingRapFlds = existingRap.getFields();
				LinkedList<SootField> existingLapFlds = existingLap.getFields();
				boolean isEqual = existing.isEqual();
				int ln = existing.getLineNum();
				
				AccessPath updatedLap = existingLap;
				AccessPath updatedRap = existingRap;
				
				//update left-hand side
				if (sameValue(replacementLapBase, existingLapBase)) {
					updatedLap = new AccessPath(replacementRapBase);
					LinkedList<SootField> updatedLapFlds = updatedLap.getFields(); 
					updatedLapFlds.addAll(replacementRapFlds);
					updatedLapFlds.addAll(existingLapFlds);
				}
				
				//update right-hand side
				if (sameValue(replacementLapBase, existingRapBase)) {
					updatedRap = new AccessPath(replacementRapBase);
					LinkedList<SootField> updatedRapFlds = updatedRap.getFields(); 
					updatedRapFlds.addAll(replacementRapFlds);
					updatedRapFlds.addAll(existingRapFlds);
				}

				Constraint updated = new Constraint(updatedLap, updatedRap, isEqual, ln);
				cSet_OUT.add(updated);
			}
			OUT.add(d_OUT);
		}
	}
    
    /**
     * Given a copy in the form of "copyLap = copyRap",
     *  update the dataflow fact IN
     *  by replacing appearance of copyRap in each constraint with copyLap. 
     */
	private void processCopy(Unit u, Disjunction IN, Disjunction OUT) {
		updateEnv(u, IN, OUT);
	}
	
	//TODO: interprocedural analysis
	private void processFormalIN(Unit u, Disjunction IN, Disjunction OUT) {
		updateEnv(u, IN, OUT);
	}
	
    private void processNullAssign(Unit u, Disjunction IN, Disjunction OUT) {
    	//TODO: need to consider dropping false constraint here
    	updateEnv(u, IN, OUT);
    }
    
    private void processNewAssign(Unit u, Disjunction IN, Disjunction OUT) {
    	//TODO: need to consider dropping false constraint here
    	updateEnv(u, IN, OUT);
    }
    
    
    private void processGetField(Unit u, Disjunction IN, Disjunction OUT) {
    	updateEnv(u, IN, OUT);
    }
    
    /**
     * Given an IfStmt, its condition produces a new constraint added to current pass.
     * Note that the handling of IfStmt is different from that of AssignStmt:
     *  AssignStmt does not produce new constraint, it only update environment.
     * Note that IN is discarded, and each OUT of u's predecessors (if-branch and then-branch)
     *  is handled differently to distinguish the if-condition and its negation. 
     * @param u: current unit (program statement)
     * @param IN: incoming dataflow fact before u
     * @param OUT: outgoing dataflow fact after u
     */
    private void processIf(Unit u, Disjunction IN, Disjunction OUT) {
    	IfStmt if_stmt = (IfStmt) u;
    	Value cond = if_stmt.getCondition();
    	boolean handleCond = true;
    	if (!(cond instanceof ConditionExpr))
    		handleCond = false;
    	else {
    		Value lv = ((ConditionExpr) cond).getOp1();
    		Value rv = ((ConditionExpr) cond).getOp2();
    		if (lv instanceof NumericConstant || rv instanceof NumericConstant
    		 || lv instanceof StringConstant  || rv instanceof StringConstant )
    			handleCond = false;
    	}
    	if (!handleCond) {
    		for (Unit u_pre : cfg.getSuccsOf(u)) {
        		Disjunction OUT_pre = dfm.getOUT(u_pre);
        		for (Disjunct d_IN : OUT_pre.getDisjunctSet()) {
        			OUT.add(d_IN);
        		}
    		}
    		return;
    	}
    	
    	Value lv = ((ConditionExpr) cond).getOp1();
		Value rv = ((ConditionExpr) cond).getOp2();
		boolean isEqual = cond instanceof EqExpr;
		int ln = u.getJavaSourceStartLineNumber();
    	for (Unit u_pre : cfg.getSuccsOf(u)) {
    		Disjunction OUT_pre = dfm.getOUT(u_pre);
			Constraint added;
			if (if_stmt.getTarget() == u_pre) // handle the if-branch of IfStmt
				added = new Constraint(lv, rv, isEqual, ln);
			else                              // handle the else-branch of IfStmt
				added = new Constraint(lv, rv, !isEqual, ln);
			for (Disjunct d_IN : OUT_pre.getDisjunctSet()) {
    			Disjunct d_OUT = new Disjunct();
    			d_OUT.addAll(d_IN);
    			d_OUT.add(added);
    			OUT.add(d_OUT);
    		}
    	}
    }
    
    @Deprecated
    private void processIf_Deprecated(Unit u, Disjunction IN, Disjunction OUT) {
    	//For IfStmt, we do not just add all disjunct in IN to OUT.
    	//OUT.addAll(IN);
    	Constraint added = null;
    	IfStmt if_stmt = (IfStmt) u;
    	Value cond = if_stmt.getCondition();
    	if (cond instanceof EqExpr) {
    		Value lv = ((EqExpr) cond).getOp1();
    		Value rv = ((EqExpr) cond).getOp2();
    		boolean isEqual = true;
    		int ln = u.getJavaSourceStartLineNumber();
    		added = new Constraint(lv, rv, isEqual, ln);
    	}
    	else if (cond instanceof NeExpr) {
    		Value lv = ((NeExpr) cond).getOp1();
    		Value rv = ((NeExpr) cond).getOp2();
    		boolean isEqual = false;
    		int ln = u.getJavaSourceStartLineNumber();
    		added = new Constraint(lv, rv, isEqual, ln);
    	}
    	
    	if (added != null) {
    		for (Disjunct d_IN : IN.getDisjunctSet()) {
    			Disjunct d_OUT = new Disjunct();
    			HashSet<Constraint> cSet_OUT = d_OUT.getConstraintSet();
    			HashSet<Constraint> cSet_IN = d_IN.getConstraintSet();
    			cSet_OUT.addAll(cSet_IN);
    			cSet_OUT.add(added);
    			OUT.add(d_OUT);
    		}
    	}
    	else {		
    		OUT.addAll(IN);
    	}
    }
    
    /**
     * For GotoStmt, simply pass the content in IN to OUT. 
     * The else-branch of an IfStmt should be handled together with the IfStmt itself,
     *  since the GotoStmt in the else-branch 
     *  will backward reach the IfStmt anyway.
     */
    private void processGoto(Unit u, Disjunction IN, Disjunction OUT) { 
        OUT.addAll(IN);
    }
    
    @Deprecated
    private void processGoto_Deprecated(Unit u, Disjunction IN, Disjunction OUT) { 
    	//For IfStmt, we do not just add all disjunct in IN to OUT.
    	//OUT.addAll(IN);
    	Constraint added = null;
    	
    	/*
    	 * We are interested in only If-Else statement.
    	 * In soot, an else-branch is constructed as a GotoStmt immediately after an IfStmt.
    	 * That means the number of predecessor of the GotoStmt must be exactly 1.
    	 * In addition, the only predecessor should be an IfStmt.
    	 */
    	List<Unit> u_pres = cfg.getPredsOf(u);
    	if (u_pres.size() != 1) {
        	OUT.addAll(IN);
    		return;
    	}
    	Unit u_pre = u_pres.iterator().next();
    	if (!(u_pre instanceof IfStmt)) {
        	OUT.addAll(IN);
    		return;
    	}
    	
    	IfStmt if_stmt = (IfStmt) u_pre;
    	Value cond = if_stmt.getCondition();
    	if (cond instanceof EqExpr) {
    		Value lv = ((EqExpr) cond).getOp1();
    		Value rv = ((EqExpr) cond).getOp2();
    		boolean isEqual = false; //Else-branch is opposite to If-branch
    		int ln = u.getJavaSourceStartLineNumber();
    		added = new Constraint(lv, rv, isEqual, ln);
    	}
    	else if (cond instanceof NeExpr) {
    		Value lv = ((NeExpr) cond).getOp1();
    		Value rv = ((NeExpr) cond).getOp2();
    		boolean isEqual = true; //Else-branch is opposite to If-branch
    		int ln = u.getJavaSourceStartLineNumber();
    		added = new Constraint(lv, rv, isEqual, ln);
    	}
    	
    	if (added != null) {
    		for (Disjunct d_IN : IN.getDisjunctSet()) {
    			Disjunct d_OUT = new Disjunct();
    			HashSet<Constraint> cSet_OUT = d_OUT.getConstraintSet();
    			HashSet<Constraint> cSet_IN = d_IN.getConstraintSet();
    			cSet_OUT.addAll(cSet_IN);
    			cSet_OUT.add(added);
    			OUT.add(d_OUT);
    		}
    	}
    	else {
        	OUT.addAll(IN);
    	}
    }
    
    /**
     * Do not keep track of Expr (e.g., +, -, <<, &&, !).
     * Given a ExprAssign, say x = y + z,
     *   the algorithm consider x as not solvable,
     *   and as such, drop all existing constraints that contain x.
     */
    private void processExprAssign(Unit u, Disjunction IN, Disjunction OUT) {
    	//TODO
    	OUT.addAll(IN);
    }
    
    /**
     * The original algorithm ignores array.
     * However, we handles array accesses with non-arithmetic index,
     * in order to get better precision.
     */
    private void processGetArray(Unit u, DataflowFact IN, DataflowFact OUT) { 
    	updateEnv(u, IN, OUT);
    }
    
    /**
     * The original algorithm ignores array.
     * However, we handles array accesses with non-arithmetic index,
     * in order to get better precision.
     */
    private void processPutArray(Unit u, DataflowFact IN, DataflowFact OUT) { 
    	updateEnv(u, IN, OUT);
    }
    
    
    private void processPutField(Unit u, DataflowFact IN, DataflowFact OUT) { 
    	//First, handle top-level pointers.
    	DataflowFact tmpOUT = new DataflowFact();
    	Value lv, rv;
		if (u instanceof AssignStmt) {
			lv = ((AssignStmt) u).getLeftOp();
			rv = ((AssignStmt) u).getRightOp();
		}
		else if (u instanceof IdentityStmt) {
			lv = ((IdentityStmt) u).getLeftOp();
			rv = ((IdentityStmt) u).getRightOp();
		}
		else {
			throw new RuntimeException("Must be AssignStmt or IdentityStmt!");
		}
		InstanceFieldRef l = (InstanceFieldRef) lv; 
		Value lb = l.getBase();
		SootField lf = l.getField();
		AccessPath replacementLap = new AccessPath();
		replacementLap.setBase(lb);
		replacementLap.addFieldFront(lf);
		AccessPath replacementRap = new AccessPath(rv);
		
		for (Disjunct d_IN : IN.getDisjunctSet()) {
			Disjunct d_OUT = new Disjunct();
			HashSet<Constraint> cSet_IN = d_IN.getConstraintSet();
			HashSet<Constraint> cSet_OUT = d_OUT.getConstraintSet();
			for (Constraint existing : cSet_IN) {
				AccessPath existingLap = existing.getLeftAP();
				AccessPath existingRap = existing.getRightAP();
				boolean isEqual = existing.isEqual();
				int ln = existing.getLineNum();
				
				AccessPath updatedLap = existingLap;
				AccessPath updatedRap = existingRap;
				if (replacementLap.equals(existingLap)) {
					updatedLap = replacementRap;
				}
				if (replacementLap.equals(existingRap)) {
					updatedRap = replacementRap;
				}
					
				Constraint updated = new Constraint(updatedLap, updatedRap, isEqual, ln);
				cSet_OUT.add(updated);
			}
			tmpOUT.add(d_OUT);
		}
		
		//Second, handle address-taken variables.
		processPutFieldWithAlias(u, tmpOUT, OUT);
    }
    
    /**
     * Handle PutField with aliases considered.
     * May-alias information is obtained by Soot's default pointer analysis.
     * The imprecision of pointer analysis could only cause some overhead, 
     *  without hurting the precision of the algorithm. 
     */
    private void processPutFieldWithAlias(Unit u, DataflowFact IN, DataflowFact OUT) {
    	Value lv, rv;
		if (u instanceof AssignStmt) {
			lv = ((AssignStmt) u).getLeftOp();
			rv = ((AssignStmt) u).getRightOp();
		}
		else if (u instanceof IdentityStmt) {
			lv = ((IdentityStmt) u).getLeftOp();
			rv = ((IdentityStmt) u).getRightOp();
		}
		else
			throw new RuntimeException("Must be AssignStmt or IdentityStmt!");
		
		if (!(lv instanceof InstanceFieldRef)) 
			throw new RuntimeException("PutField left-hand side is not InstanceFieldRef!");
		
		Value lb = ((InstanceFieldRef) lv).getBase();
		SootField lf = ((InstanceFieldRef) lv).getField();
		
		if (!(lb instanceof Local)) {
			System.out.println(u);
			System.out.println(lb.getClass());
			throw new RuntimeException("PutField base in left-hand side is not Local!");
		}
		
		DataflowFact tmpIN = IN;
		DataflowFact tmpOUT = tmpIN.clone();
		
		PointsToAnalysis pta = Scene.v().getPointsToAnalysis();
		PointsToSet pts_lb = pta.reachingObjects((Local) lb);
		HashSet<AccessPath> allAccessPathSegments = getAllAccessPathSegments(IN, lf);
		for (AccessPath ap_segment : allAccessPathSegments) {
			PointsToSet pts_ap = getAccessPathPointsTo(ap_segment);
			if (pts_ap == null) continue;
			boolean mayAlias = pts_lb.hasNonEmptyIntersection(pts_ap);
			if (!mayAlias) continue;
			
			tmpOUT = new DataflowFact();
			for (Disjunct d : tmpIN.getDisjunctSet()) {
				/* First check if update is required. 
				   This is a lazy allocation, 
				   as when update is not required, new constraints are not allocated. */
				boolean requireUpdate = false;
	    		for (Constraint c : d.getConstraintSet()) {
	    			if (c.getLeftAP().subsume(ap_segment) || c.getRightAP().subsume(ap_segment)) {
	    				requireUpdate = true;
	    				break;
	    			}
	    		}
	    		
	    		/* If update is required, we fork the existing constraints, 
	    		   assuming alias and not-alias both possible.*/ 
	    		if (requireUpdate) {
	    			Disjunct d_assumeAlias = d.clone(); //fork
					for (Constraint c : d_assumeAlias.getConstraintSet()) {
						
						AccessPath lap = c.getLeftAP();
						if (lap.subsume(ap_segment)) {
							AccessPath lap_new = new AccessPath();
							/*if (!(rv instanceof Local))
								throw new RuntimeException("PutField right-hand side is not Local!");*/
							lap_new.setBase(rv);
							LinkedList<SootField> lapFlds_new = lap_new.getFields();
							LinkedList<SootField> lapFlds = lap.getFields();
							Iterator<SootField> I = lapFlds.iterator();
							for (int i = 0; i < lapFlds.size(); ++i) {
								SootField fld = I.next();
								if (i > ap_segment.getFields().size())
									lapFlds_new.addLast(fld);
							}
							c.setLeftAP(lap_new);
						}
						
						AccessPath rap = c.getRightAP();
						if (rap.subsume(ap_segment)) {
							AccessPath rap_new = new AccessPath();
							/*if (!(rv instanceof Local))
								throw new RuntimeException("PutField right-hand side is not Local!");*/
							rap_new.setBase(rv);
							LinkedList<SootField> rapFlds_new = rap_new.getFields();
							LinkedList<SootField> rapFlds = rap.getFields();
							Iterator<SootField> I = rapFlds.iterator();
							for (int i = 0; i < rapFlds.size(); ++i) {
								SootField fld = I.next();
								if (i > ap_segment.getFields().size())
									rapFlds_new.addLast(fld);
							}
							c.setRightAP(rap_new);
						}
		    		}
	    			Constraint assumeAlias = new Constraint(ap_segment, new AccessPath(lb), true, u.getJavaSourceStartLineNumber());
	    			d_assumeAlias.add(assumeAlias);
					tmpOUT.add(d_assumeAlias);
					
					Disjunct d_assumeNotAlias = d.clone();
					Constraint assumeNotAlias = new Constraint(ap_segment, new AccessPath(lb), false, u.getJavaSourceStartLineNumber());
	    			d_assumeNotAlias.add(assumeNotAlias);
					tmpOUT.add(d_assumeNotAlias);
	    		}
	    		else {
	    			tmpOUT.add(d.clone());
	    		}
	    		
	    	}
			tmpIN = tmpOUT;
		}
		OUT.addAll(tmpOUT);
    }
    
    /**
     * Only one layer of indirection is considered, 
     * i.e., alias(r.f, x.y.f) is considered, but alias(r.f, x.y.z.f) is not. 
     */
    @Deprecated
    private void processPutFieldWithAlias_Deprecated(Unit u, DataflowFact IN, DataflowFact OUT) {
    	Value lv, rv;
		if (u instanceof AssignStmt) {
			lv = ((AssignStmt) u).getLeftOp();
			rv = ((AssignStmt) u).getRightOp();
		}
		else if (u instanceof IdentityStmt) {
			lv = ((IdentityStmt) u).getLeftOp();
			rv = ((IdentityStmt) u).getRightOp();
		}
		else
			throw new RuntimeException("Must be AssignStmt or IdentityStmt!");
		
		if (!(lv instanceof InstanceFieldRef)) 
			throw new RuntimeException("PutField left-hand side is not InstanceFieldRef!");
		
		Value lb = ((InstanceFieldRef) lv).getBase();
		SootField lf = ((InstanceFieldRef) lv).getField();
		
		if (!(lb instanceof Local)) {
			System.out.println(u);
			System.out.println(lb.getClass());
			throw new RuntimeException("PutField base in left-hand side is not Local!");
		}
		
		DataflowFact tmpIN = IN;
		DataflowFact tmpOUT = tmpIN.clone();
		
		PointsToAnalysis pta = Scene.v().getPointsToAnalysis();
		PointsToSet pts_lb = pta.reachingObjects((Local) lb);
		for (Local loc : getAllLocal(IN)) {
			if (loc == lb) continue;
			
			PointsToSet pts_loc = pta.reachingObjects(loc);
			boolean mayAlias = pts_lb.hasNonEmptyIntersection(pts_loc);
			if (!mayAlias) continue;
			
			tmpOUT = new DataflowFact();
			for (Disjunct d : tmpIN.getDisjunctSet()) {
				/* First check if update is required. 
				   This is a lazy allocation, 
				   as when update is not required, new constraints are not allocated. */
				boolean requireUpdate = false;
	    		for (Constraint c : d.getConstraintSet()) {
	    			Value clb = c.getLeftAP().getBase();
	    			if (clb.equals(loc)) {
		    			LinkedList<SootField> clfs = c.getLeftAP().getFields();
		    			if (!clfs.isEmpty() && clfs.getFirst().equals(lf)) {
    	    				requireUpdate = true;
    	    				break;
	    	    		}
	    			}
	    			Value crb = c.getRightAP().getBase();
	    			if (crb.equals(loc)) {
	    				LinkedList<SootField> crfs = c.getRightAP().getFields();
	    				if (!crfs.isEmpty() && crfs.getFirst().equals(lf)) {
	    					requireUpdate = true;
    	    				break;
	    				}
	    			}
	    		}
	    		
	    		Disjunct d_assumeNotAlias = d.clone();
	    		/* If update is required, we fork the existing constraints, 
	    		   assuming alias and not-alias both possible. */
	    		if (requireUpdate) {
	    			Constraint assumeNotAlias = new Constraint(loc, lb, false, u.getJavaSourceStartLineNumber());
	    			d_assumeNotAlias.add(assumeNotAlias);
	    			Disjunct d_assumeAlias = d.clone(); //fork
					for (Constraint c : d_assumeAlias.getConstraintSet()) {
		    			Value clb = c.getLeftAP().getBase();
		    			if (clb.equals(loc)) {
			    			LinkedList<SootField> clfs = c.getLeftAP().getFields();
			    			if (!clfs.isEmpty() && clfs.getFirst().equals(lf)) {
			    				clfs.removeFirst();
			    				c.getLeftAP().setBase(rv);
		    	    		}
		    			}
		    			Value crb = c.getRightAP().getBase();
		    			if (crb.equals(loc)) {
		    				LinkedList<SootField> crfs = c.getRightAP().getFields();
		    				if (!crfs.isEmpty() && crfs.getFirst().equals(lf)) {
		    					crfs.removeFirst();
		    					c.getRightAP().setBase(rv);
		    				}
		    			}
		    		}
					Constraint assumeAlias = new Constraint(loc, lb, true, u.getJavaSourceStartLineNumber());
	    			d_assumeAlias.add(assumeAlias);
					tmpOUT.add(d_assumeAlias);
	    		}
	    		tmpOUT.add(d_assumeNotAlias);
	    	}
			tmpIN = tmpOUT;
		}
		OUT.addAll(tmpOUT);
    }
    
    private HashSet<Local> getAllLocal(DataflowFact df) {
    	HashSet<Local> res = new HashSet<>();
    	for (Disjunct d : df.getDisjunctSet()) {
    		for (Constraint c : d.getConstraintSet()) {
    			Value lb = c.getLeftAP().getBase();
    			Value rb = c.getRightAP().getBase();
    			if (lb instanceof Local)
    				res.add((Local) lb);
    			if (rb instanceof Local)
    				res.add((Local) rb);
    		}
    	}
    	return res;
    }
    
    /**
     * Given an access path ap, 
     * compute its points-to set 
     * by iteratively calling reachingObjects(pts, field)
     * @param ap : access path, e.g., a.b.c.
     * @return : points-to set of ap.
     */
    private PointsToSet getAccessPathPointsTo(AccessPath ap) {
    	Value base = ap.getBase();
    	if (base instanceof NullConstant)
    		return null;
    	
    	if (!(base instanceof Local)) {
    		System.out.println(ap);
    		throw new RuntimeException("Access path base is not Local!");
    	}
    	LinkedList<SootField> flds = ap.getFields();
    	PointsToAnalysis pta = Scene.v().getPointsToAnalysis();
		PointsToSet pts = pta.reachingObjects((Local) base);
		//System.out.println("PTS_BASE ( " + base + " ) = " + pts);
		Iterator<SootField> I = flds.iterator();
		while (I.hasNext()) {
			SootField fld = I.next();
			/* level by level, iteratively compute the result 
			 * by using output of last step as input of this step.*/
			pts = pta.reachingObjects(pts, fld);
			//System.out.println("PTS_Field ( " + fld + " ) = " + pts);
		}
    	return pts;
    }
    
    /**
     * @param df : dataflow fact.
     * @param fld : a specified field name.
     * @return : set of cloned copies of all access paths in df ending with fld.
     */
    private HashSet<AccessPath> getAllAccessPathSegments(DataflowFact df, SootField fld) {
    	HashSet<AccessPath> res = new HashSet<>();
    	for (Disjunct d : df.getDisjunctSet()) {
    		for (Constraint c : d.getConstraintSet()) {
    			AccessPath lap = c.getLeftAP();
    			Value lapBase = lap.getBase();
    			LinkedList<SootField> lapFlds = lap.getFields();
    			int idx = lapFlds.indexOf(fld);
    			if (idx != -1) {
    				AccessPath wanted = new AccessPath();
    				wanted.setBase(lapBase);
    				if (idx >= 1) {
	    				List<SootField> lapFlds_sub = lapFlds.subList(0, idx);
	    				Iterator<SootField> I = lapFlds_sub.iterator();
	    				while(I.hasNext()) {
	    					wanted.getFields().addLast(I.next());
	    				}
    				}
    				res.add(wanted);
    			}
    			
    			AccessPath rap = c.getRightAP();
    			Value rapBase = rap.getBase();
    			LinkedList<SootField> rapFlds = rap.getFields();
    			idx = rapFlds.indexOf(fld);
    			if (idx != -1) {
    				AccessPath wanted = new AccessPath();
    				wanted.setBase(rapBase);
    				if (idx >= 1) {
	    				List<SootField> rapFlds_sub = rapFlds.subList(0, idx);
	    				Iterator<SootField> I = rapFlds_sub.iterator();
	    				while(I.hasNext()) {
	    					wanted.getFields().addLast(I.next());
	    				}
    				}
    				res.add(wanted);
    			}
    		}
    	}
    	return res;
    }
    
    private void processReturn() { 
    	//TODO
    }
    
    private void processSpecialInvoke(Unit u, Disjunction IN, Disjunction OUT) {
    	OUT.addAll(IN);
    	InvokeStmt inv = (InvokeStmt) u;
		SpecialInvokeExpr inv_expr = (SpecialInvokeExpr) inv.getInvokeExpr();
		System.err.println(u);
		System.err.println(inv_expr.getBase());
		System.err.println(inv_expr.getMethod());
		Body body = inv_expr.getMethod().retrieveActiveBody();
		System.err.println(body);
		UnitGraph cfg = new ExceptionalUnitGraph(body);
		Unit cur = cfg.getTails().iterator().next();
		DataflowFact local_IN = new DataflowFact();
		DataflowFact local_OUT = new DataflowFact();
		while(true) {
			transfer(cur, local_IN, local_OUT);
			List<Unit> pres = cfg.getPredsOf(cur);
			if (pres.isEmpty())
				break;
			cur = pres.iterator().next();
			local_IN = local_OUT.clone();
			local_OUT = new DataflowFact();
		}
		System.err.println("local OUT : " + local_OUT);
    }
	
	public DataflowFact transfer(Unit u, DataflowFact IN, DataflowFact oldOUT) {
		
		DataflowFact newOUT = new DataflowFact();
		
		if (isCopy(u)) {
			System.out.println("+++ handling Copy : " + u);
			processCopy(u, IN, newOUT);
		}
		else if (isFormalIn(u)) {
			//TODO: inter-procedural analysis
			System.out.println("+++ handling FormalIN : " + u);
			processFormalIN(u, IN, newOUT);
		}
		else if (isNullAssign(u)) {
			System.out.println("+++ handling NullAssign : " + u);
			processNullAssign(u, IN, newOUT);
		}
		else if (isStaticFldAssign(u)) {
			//TODO
			System.out.println("+++ handling StaticFldAssign : " + u);
			newOUT.addAll(IN);
		}
		else if (isNewAssign(u)) {
			System.out.println("+++ handling NewAssign : " + u);
			processNewAssign(u, IN, newOUT);
		}
		else if (isGetField(u)) {
			System.out.println("+++ handling GetField : " + u);
			processGetField(u, IN, newOUT);
		}
		else if (isIf(u)) {
			System.out.println("+++ handling If : " + u);
			processIf(u, IN, newOUT);
		}
		else if (isGoto(u)) {
			System.out.println("+++ handling Goto : " + u);
			processGoto(u, IN, newOUT);
		}
		else if (isExprAssign(u)) {
			System.out.println("+++ handling ExprAssign : " + u);
			processExprAssign(u, IN, newOUT);
		}
		else if (isGetArray(u)) {
			System.out.println("+++ handling GetArray : " + u);
			processGetArray(u, IN, newOUT);
		}
		else if (isPutArray(u)) {
			//TODO : This requires alias analysis
			System.out.println("+++ handling PutArray : " + u);
			processPutArray(u, IN, newOUT);
		}
		else if (isPutField(u)) {
			//TODO : This requires alias analysis
			System.out.println("+++ handling PutField : " + u);
			processPutField(u, IN, newOUT);
		}
		else if (isReturn(u)) {
			//TODO: inter-procedural analysis
			System.out.println("+++ handling Return : " + u);
			newOUT.addAll(IN);
		}
		else if (isStaticInvoke(u)) {
			//TODO: inter-procedural analysis
			System.out.println("+++ handling StaticInvoke : " + u);
			newOUT.addAll(IN);
		}
		else if (isSpecialInvoke(u)) {
			//TODO: inter-procedural analysis
			System.out.println("+++ handling SpecialInvoke : " + u);
			//processSpecialInvoke(u, IN, newOUT);
			newOUT.addAll(IN);
		}
		else if (isVirtualInvoke(u)) {
			//TODO: inter-procedural analysis
			System.out.println("+++ handling VirtualInvoke : " + u);
			newOUT.addAll(IN);
		}
		else if (isStaticInvokeAssign(u)) {
			//TODO: inter-procedural analysis
			System.out.println("+++ handling StaticInvokeAssign : " + u);
			newOUT.addAll(IN);
		}
		else if (isSpecialInvokeAssign(u)) {
			//TODO: inter-procedural analysis
			System.out.println("+++ handling SpecialInvokeAssign : " + u);
			newOUT.addAll(IN);
		}
		else if (isVirtualInvokeAssign(u)) {
			//TODO: inter-procedural analysis
			System.out.println("+++ handling VirtualInvokeAssign : " + u);
			newOUT.addAll(IN);
		}
		else {
			System.err.println("=======Unkown " + u);
			if (u instanceof DefinitionStmt) {
				System.err.println(((DefinitionStmt) u).getLeftOp().getClass());
				System.err.println(((DefinitionStmt) u).getRightOp().getClass());
				System.err.println();
			}
			else {
				System.err.println(u.getClass());
			}
			throw new RuntimeException("Unkown statement! Additional handling required!");
		}
		
		Solver.getInstance().solve(newOUT);
		//newOUT.simpify();
		
		return newOUT;
	}
	
	//Soot does not implement equal function for ArrayRef, so we have to handle it here.
	private boolean sameValue(Value v1, Value v2) {
		if (v1 instanceof ArrayRef && v2 instanceof ArrayRef) {
			ArrayRef a1 = (ArrayRef) v1;
			ArrayRef a2 = (ArrayRef) v2;
			return a1.getBase().equals(a2.getBase())
					&& a1.getIndex().equals(a2.getIndex());
		}
		return v1.equals(v2);
	}
	
	//it requires def-use chain to perform basic analysis
	public void init(UnitGraph cfg, DataflowFactMapping dfm) {
		this.body = cfg.getBody();
		this.cfg = cfg;
		this.du = new SimpleLocalDefs(cfg); //local def-use chain
		this.dfm = dfm;
	}
	
	
	private Body body;
	private UnitGraph cfg;
	private SimpleLocalDefs du; //local def-use chain
	private DataflowFactMapping dfm;
}