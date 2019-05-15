package core;
import java.util.ArrayList;
import java.util.HashMap;
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
import adt.LogicValue;
import adt.NullAssumption;
import adt.OutOfBudget;
import solver.BlackList;
import solver.Helper;
import pag.node.alloc.Alloc_Node;
import pta.PTA;
import solver.Solver;
import soot.Body;
import soot.IntType;
import soot.Local;
import soot.MethodOrMethodContext;
import soot.PointsToAnalysis;
import soot.PointsToSet;
import soot.PrimType;
import soot.RefType;
import soot.Scene;
import soot.SootClass;
import soot.SootField;
import soot.SootFieldRef;
import soot.SootMethod;
import soot.Unit;
import soot.Value;
import soot.jimple.AddExpr;
import soot.jimple.AndExpr;
import soot.jimple.AnyNewExpr;
import soot.jimple.ArrayRef;
import soot.jimple.AssignStmt;
import soot.jimple.BinopExpr;
import soot.jimple.CastExpr;
import soot.jimple.CmpgExpr;
import soot.jimple.ConditionExpr;
import soot.jimple.Constant;
import soot.jimple.DefinitionStmt;
import soot.jimple.DivExpr;
import soot.jimple.EqExpr;
import soot.jimple.FieldRef;
import soot.jimple.GeExpr;
import soot.jimple.GotoStmt;
import soot.jimple.GtExpr;
import soot.jimple.IdentityStmt;
import soot.jimple.IfStmt;
import soot.jimple.InstanceFieldRef;
import soot.jimple.InstanceOfExpr;
import soot.jimple.InvokeExpr;
import soot.jimple.InvokeStmt;
import soot.jimple.Jimple;
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
import soot.jimple.SwitchStmt;
import soot.jimple.ThisRef;
import soot.jimple.ThrowStmt;
import soot.jimple.UshrExpr;
import soot.jimple.VirtualInvokeExpr;
import soot.jimple.XorExpr;
import soot.jimple.internal.JArrayRef;
import soot.jimple.internal.JAssignStmt;
import soot.jimple.internal.JInstanceFieldRef;
import soot.jimple.internal.JimpleLocal;
import soot.jimple.spark.pag.AllocNode;
import soot.jimple.spark.pag.Node;
import soot.jimple.spark.pag.PAG;
import soot.jimple.spark.pag.VarNode;
import soot.jimple.spark.sets.PointsToSetInternal;
import soot.jimple.toolkits.callgraph.CallGraph;
import soot.jimple.toolkits.callgraph.Edge;
import soot.jimple.toolkits.callgraph.Sources;
import soot.toolkits.graph.ExceptionalUnitGraph;
import soot.toolkits.graph.UnitGraph;
import soot.toolkits.scalar.SimpleLocalDefs;
import soot.util.Chain;


public class InterNullPtrTransFunc extends TransferFunc {
	
	//it requires def-use chain to perform basic analysis
	public InterNullPtrTransFunc(InterNullPtrAnalysis analysis, DataflowFactMapping env, Unit deref) {
		this.analysis = analysis;
		this.env = env;
		this.deref = deref;
	}
	
	/**
	 * E.g., x = y
	 */
	private boolean isCopy(Unit u) { 
		if (u instanceof DefinitionStmt) {
			Value lv = ((DefinitionStmt) u).getLeftOp();
			Value rv = ((DefinitionStmt) u).getRightOp();
			if (lv instanceof Local
			 && (rv instanceof Local || rv instanceof ParameterRef || rv instanceof ThisRef))
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
	 * E.g., x = new A, or x = newarray(A)[100] 
	 */
    private boolean isNewAssign(Unit u) { 
    	if (u instanceof AssignStmt) {
			Value lv = ((AssignStmt) u).getLeftOp();
			Value rv = ((AssignStmt) u).getRightOp();
			if (lv instanceof Local
			 && ( rv instanceof AnyNewExpr))
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
     * E.g., temp$12 = <java.lang.System: java.io.PrintStream out>.
	 * E.g., x = Y.z where Y is a class and z is a static field.
	 * StaticGetField itself is not a dereference,
	 * but it requires to be handled, 
	 * in order to analysis the dereference of the static field.
	 */
    private boolean isStaticGetField(Unit u) { 
    	if (u instanceof AssignStmt) {
			Value rv = ((AssignStmt) u).getRightOp();
			if (rv instanceof StaticFieldRef)
				return true;
		}
    	return false;
    }
    
    /**
     * E.g., X.y = z where X is a class and y is static field
     * E.g., <VC.vctest: VC.ASTs.AST theAST> = null.
	 */
    private boolean isStaticPutField(Unit u) { 
    	if (u instanceof AssignStmt) {
			Value lv = ((AssignStmt) u).getLeftOp();
			if (lv instanceof StaticFieldRef)
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
	 * E.g., tableswitch(temp$2)
        {
            case 1: goto label01;
            case 2: goto label02;
            case 3: goto label03;
            case 4: goto label04;
            case 5: goto label05;
            default: goto label06;
        };
	 */
    private boolean isSwitch(Unit u) {
    	return u instanceof SwitchStmt;
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
					 || rv instanceof InstanceOfExpr // instanceof
				))
				return true;
		}
		return false;
	}
	
	/**
	 * E.g. e = (npda.AssignExpr) ast;
	 */
	private boolean isCasting(Unit u) {
		if (u instanceof AssignStmt) {
			Value lv = ((AssignStmt) u).getLeftOp();
			Value rv = ((AssignStmt) u).getRightOp();
			if (lv instanceof Local
			 && rv instanceof CastExpr //casting
			 )
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
    
    private boolean isThrow(Unit u) {
    	return u instanceof ThrowStmt;
    }
    
    /** e.g. temp$14 = temp$13 cmpg 0.5 */
    private boolean isBinopExprAssign(Unit u) {
    	if (u instanceof AssignStmt) {
			AssignStmt assign = (AssignStmt) u;
			if (assign.getRightOp() instanceof BinopExpr)
				return true;
		}
    	return false;
    }
    
    private void updateEnv(Unit u, Disjunction IN, Disjunction OUT) {
		Value lv, rv;
		if (u instanceof DefinitionStmt) {
			lv = ((DefinitionStmt) u).getLeftOp();
			rv = ((DefinitionStmt) u).getRightOp();
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
			d_OUT.setAge(d_IN.getAge() + 1);
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
				
				AccessPath updatedLap = existingLap.clone();
				AccessPath updatedRap = existingRap.clone();
				
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

				Constraint updated = (existing instanceof NullAssumption) ?
									new NullAssumption(updatedLap, updatedRap, isEqual, ln)
								  : new Constraint(updatedLap, updatedRap, isEqual, ln);
				cSet_OUT.add(updated);
			}
			OUT.add(d_OUT);
		}
	}
    
    /**
     * Connects a method and its call site,
     * by replacing the formal parameters by actual parameters.
     */
    private void updateMethodParameters(ParameterRef formalIN, Value actualIN, Disjunction BEFORE, Disjunction AFTER) {
    	for (Disjunct d_before : BEFORE.getDisjunctSet()) {
    		Disjunct d_after = new Disjunct();
    		d_after.setAge(d_before.getAge() + 1);
    		for (Constraint c_before : d_before.getConstraintSet()) {
    			Constraint c_after = c_before.clone();
    			if (c_after.getLeftAP().getBase() != null 
    			 && c_after.getLeftAP().getBase().equals(formalIN))
    				c_after.getLeftAP().setBase(actualIN);
    			if (c_after.getRightAP().getBase() != null
    			 && c_after.getRightAP().getBase().equals(formalIN)) 
    				c_after.getRightAP().setBase(actualIN);
    			d_after.add(c_after);
    		}
    		AFTER.add(d_after);
    	}
    }
    
    /**
     * Connects a method and its call site,
     * by replacing this pointer by base pointer.
     */
    private void updateThisPtr(ThisRef thisPtr, Value actualPtr, Disjunction BEFORE, Disjunction AFTER) {
    	for (Disjunct d_before : BEFORE.getDisjunctSet()) {
    		Disjunct d_after = new Disjunct();
    		d_after.setAge(d_before.getAge() + 1);
    		for (Constraint c_before : d_before.getConstraintSet()) {
    			Constraint c_after = c_before.clone();
    			if (c_after.getLeftAP().getBase() != null
    			 && c_after.getLeftAP().getBase().equals(thisPtr)) 
    				c_after.getLeftAP().setBase(actualPtr);
    			if (c_after.getRightAP().getBase() != null
    			 && c_after.getRightAP().getBase().equals(thisPtr)) 
    				c_after.getRightAP().setBase(actualPtr);
    			d_after.add(c_after);
    		}
    		AFTER.add(d_after);
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
	
	private void processCasting(Unit u, Disjunction IN, Disjunction OUT) {
		updateEnv(u, IN, OUT);
	}
	
	//interprocedural analysis
	private void processFormalIN(Unit u, Disjunction IN, Disjunction OUT) {
		updateEnv(u, IN, OUT);
	}
	
    private void processNullAssign(Unit u, Disjunction IN, Disjunction OUT) {
    	updateEnv(u, IN, OUT);
    }
    
    private void processNewAssign(Unit u, Disjunction IN, Disjunction OUT) {
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
    	if (cond instanceof EqExpr || cond instanceof NeExpr) {
    		Value lv = ((ConditionExpr) cond).getOp1();
    		Value rv = ((ConditionExpr) cond).getOp2();
    		if (lv instanceof PrimType 		  || rv instanceof PrimType
    		 || lv instanceof NumericConstant || rv instanceof NumericConstant
    		 || lv instanceof StringConstant  || rv instanceof StringConstant )
    			handleCond = false;
    	}
    	else {
    		handleCond = false;
    	}
    	
    	UnitGraph cfg = analysis.getHelper().getCFG(u);
    	if (!handleCond) {
    		for (Unit u_pre : cfg.getSuccsOf(u)) {
        		Disjunction OUT_pre = env.getOUT(u_pre);
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
    		Disjunction OUT_pre = env.getOUT(u_pre);
			Constraint added;
			if (if_stmt.getTarget() == u_pre) // handle the if-branch of IfStmt
				added = new Constraint(lv, rv, isEqual, ln);
			else                              // handle the else-branch of IfStmt
				added = new Constraint(lv, rv, !isEqual, ln);
			for (Disjunct d_IN : OUT_pre.getDisjunctSet()) {
    			Disjunct d_OUT = d_IN.clone();
    			d_OUT.add(added);
    			OUT.add(d_OUT);
    		}
    	}
    }
    
    /**
     * Given an SwitchStmt, merge all dataflow facts from its CFG successors 
     */
    private void processSwitch(Unit u, Disjunction IN, Disjunction OUT) {
    	UnitGraph cfg = analysis.getHelper().getCFG(u);
		for (Unit u_pre : cfg.getSuccsOf(u)) {
    		Disjunction OUT_pre = env.getOUT(u_pre);
    		for (Disjunct d_IN : OUT_pre.getDisjunctSet()) {
    			OUT.add(d_IN);
    		}
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
    
    private void processGetField(Unit u, Disjunction IN, Disjunction OUT) {
    	updateEnv(u, IN, OUT);
    	handleRecurFld(OUT); 
    }
    
    private void processStaticGetField(Unit u, Disjunction IN, Disjunction OUT) {
    	updateEnv(u, IN, OUT);
    	handleRecurFld(OUT); 
    }
    
    /**
     * The original algorithm ignores array.
     * However, we handles array accesses with non-arithmetic index,
     * in order to get better precision.
     */
    private void processGetArray(Unit u, DataflowFact IN, Disjunction OUT) { 
    	updateEnv(u, IN, OUT);
    	
    	/*
    	 * scan each constraint in the disjunction, 
    	 * drop constraints that contains any recursive field.
    	 * This is in order to guarantee termination,
    	 * because recursive data structures (e.g. linked list)
    	 * cannot be precisely analyzed.
    	 */
    	handleRecurFld(OUT); 
    }
    
    private void handleRecurFld(Disjunction disjunction) {
    	boolean setAsTrue = false; //flag indicating if the whole disjunction reduces to True
    	HashSet<Disjunct> dSet = disjunction.getDisjunctSet();
    	for (Disjunct d : dSet) {
    		HashSet<Constraint> cSet = d.getConstraintSet();
    		ArrayList<Constraint> toDel = new ArrayList<>();
    		for (Constraint c : cSet) {
    			AccessPath lap = c.getLeftAP();
    			AccessPath rap = c.getRightAP();
    			if (hasRecurFld(lap) || hasRecurFld(rap))
    				toDel.add(c);
    		}
    		for (Constraint c : toDel)
    			cSet.remove(c);
    		
    		/* if such dropping operation leads to any disjunct becoming empty,
    		 * then the disjunct becomes true,
    		 * and the whole disjunction becomes true. */
    		if (d.isEmpty()) {
    			setAsTrue = true;
    			break;
    		}
    	}
    	
    	if (setAsTrue) {
    		disjunction.setAsTrue();
    	}
    }
    
    /**
     * check if the first field element occurs in other field position in the access path
     */
    private boolean hasRecurFld(AccessPath ap) {
    	if (ap == null) return false;
    	
    	LinkedList<SootField> flds = ap.getFields();
    	if (flds.isEmpty()) return false;
    	
    	Iterator<SootField> I = flds.iterator();
    	SootField headFld = I.next();
    	while (I.hasNext()) {
    		SootField fld = I.next();
    		if (fld == headFld) {    			
    			return true;
    		}
    	}
    	return false;
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
			d_OUT.setAge(d_IN.getAge() + 1);
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
				
				Constraint updated = (existing instanceof NullAssumption) ?
						new NullAssumption(updatedLap, updatedRap, isEqual, ln)
					  : new Constraint(updatedLap, updatedRap, isEqual, ln);
				
				cSet_OUT.add(updated);
			}
			tmpOUT.add(d_OUT);
		}
		
		//Second, handle address-taken variables.
		processPutFieldWithAlias(u, tmpOUT, OUT);
    }
    
    /** For each dereference site x.y backward reachable from the dereference site being verified,
     *  assume the base pointer x is not null */
    private void assumeBaseNotNull(Unit u, DataflowFact IN) {
    	Value base = Helper.getInstance().getBase(u);
    	if (base == null)
    		return;
    	IN.solve();
		if (IN.logicValue == LogicValue.True)
			return;
		
		Constraint assumeBaseNull = new Constraint(base, NullConstant.v(), true, u.getJavaSourceStartLineNumber());
		ArrayList<Disjunct> toDel = new ArrayList<>();
		for (Disjunct d : IN.getDisjunctSet()) {
			for (Constraint c : d.getConstraintSet()) {
				if (c.equals(assumeBaseNull)) {
					toDel.add(d);
					break;
				}
			}
		}
		for (Disjunct d : toDel)
			IN.remove(d);
		
		if (IN.isEmpty())
			IN.setAsFalse();
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
		
		//PointsToAnalysis pta = Scene.v().getPointsToAnalysis();
		PointsToAnalysis pta = solver.Helper.getInstance().getPTA();
		PointsToSet pts_lb = pta.reachingObjects((Local) lb);
		HashSet<AccessPath> allAccessPathSegments = getAllAccessPathSegments(IN, lf);
		
		for (AccessPath ap_segment : allAccessPathSegments) {
			AccessPath ap_segment_base = ap_segment.clone();
			ap_segment_base.getFields().removeLast();
			PointsToSet pts_ap = getAccessPathPointsTo(ap_segment_base);
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
								if (i >= ap_segment.getFields().size())
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
								if (i >= ap_segment.getFields().size())
									rapFlds_new.addLast(fld);
							}
							c.setRightAP(rap_new);
						}
		    		}
					int ln = u.getJavaSourceStartLineNumber();
					if (ln == -1) ln = lineNum_dummyAssignStmt;
	    			Constraint assumeAlias = new Constraint(ap_segment_base, new AccessPath(lb), true, ln);
	    			d_assumeAlias.add(assumeAlias);
					tmpOUT.add(d_assumeAlias);
					
					Disjunct d_assumeNotAlias = d.clone();
					Constraint assumeNotAlias = new Constraint(ap_segment_base, new AccessPath(lb), false, ln);
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
    

   /* private void processStaticPutField(Unit u, DataflowFact IN, DataflowFact OUT) {
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
                StaticFieldRef l = (StaticFieldRef) lv;
                SootField lf = l.getField();
                AccessPath replacementLap = new AccessPath();
                replacementLap.setBase(null);
                replacementLap.addFieldFront(lf);
                AccessPath replacementRap = new AccessPath(rv);

                for (Disjunct d_IN : IN.getDisjunctSet()) {
                        Disjunct d_OUT = new Disjunct();
                        d_OUT.setAge(d_IN.getAge() + 1);
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

                OUT.addAll(tmpOUT);
    }*/

    
    private void processStaticPutField(Unit u, DataflowFact IN, DataflowFact OUT) { 
    	DataflowFact tmpOUT = new DataflowFact();
    	Value lv, rv;
		if (u instanceof DefinitionStmt) {
			lv = ((DefinitionStmt) u).getLeftOp();
			rv = ((DefinitionStmt) u).getRightOp();
		}
		else {
			throw new RuntimeException("Must be DefinitionStmt!");
		}
		
		AccessPath replacementLap = new AccessPath(lv);
		AccessPath replacementRap = new AccessPath(rv);
		
		for (Disjunct d_IN : IN.getDisjunctSet()) {
			Disjunct d_OUT = new Disjunct();
			d_OUT.setAge(d_IN.getAge() + 1);
			HashSet<Constraint> cSet_IN = d_IN.getConstraintSet();
			HashSet<Constraint> cSet_OUT = d_OUT.getConstraintSet();
			for (Constraint existing : cSet_IN) {
				AccessPath existingLap = existing.getLeftAP();
				AccessPath existingRap = existing.getRightAP();
				boolean isEqual = existing.isEqual();
				int ln = existing.getLineNum();
				
				AccessPath updatedLap = existingLap;
				AccessPath updatedRap = existingRap;
				if (existingLap.getBase() == null || existingLap.getBase() instanceof NullConstant) {
					LinkedList<SootField> replacementLapFld = replacementLap.getFields();
					LinkedList<SootField> existingLapFld = existingLap.getFields();
					int i = 0;
					boolean isPrefix = true;
					if (existingLapFld.size() >= replacementLapFld.size()) {
						for (; i < replacementLapFld.size(); ++i) {
							if (replacementLapFld.get(i) != existingLapFld.get(i))
								isPrefix = false;
						}
					}
					else {
						isPrefix = false;
					}
					if (isPrefix) {
						updatedLap = replacementRap;
						for (; i < existingLapFld.size(); ++i) {
							updatedLap.getFields().push(existingLapFld.get(i));
						}
					}
				}
				if (existingRap.getBase() == null || existingRap.getBase() instanceof NullConstant) {
					LinkedList<SootField> replacementLapFld = replacementLap.getFields();
					LinkedList<SootField> existingRapFld = existingRap.getFields();
					int i = 0;
					boolean isPrefix = true;
					if (existingRapFld.size() >= replacementLapFld.size()) {
						for (; i < replacementLapFld.size(); ++i) {
							if (replacementLapFld.get(i) != existingRapFld.get(i))
								isPrefix = false;
						}
					}
					else {
						isPrefix = false;
					}
					if (isPrefix) {
						updatedRap = replacementRap;
						for (; i < existingRapFld.size(); ++i) {
							updatedRap.getFields().push(existingRapFld.get(i));
						}
					}
				}
				
				Constraint updated = (existing instanceof NullAssumption) ?
						new NullAssumption(updatedLap, updatedRap, isEqual, ln)
					  : new Constraint(updatedLap, updatedRap, isEqual, ln);
				
				cSet_OUT.add(updated);
			}
			tmpOUT.add(d_OUT);
		}
		
		OUT.addAll(tmpOUT);
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
    	
    	if (base instanceof ParameterRef) {
			int idx = ((ParameterRef) base).getIndex();
			analysis.getCallsiteArgs().get(idx);
		}
		else if (base instanceof ThisRef) {
			base = analysis.getCallsiteBase();
		}
    	/* Have to hack here, because given a NewExpr, 
    	 * we need to get the internal PAG representation of it. */ 
    	else if (base instanceof AnyNewExpr) {
    		//PAG pta = (PAG) Scene.v().getPointsToAnalysis();
    		PointsToAnalysis pta = Helper.getInstance().getPTA();
    		if (pta instanceof PAG) {
	    		AllocNode alloc = ((PAG) pta).makeAllocNode(base, base.getType(), null);
	    		LinkedList<SootField> flds = ap.getFields();
	    		PointsToSet pts = alloc.getP2Set();
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
    		else { //pta is Druid pta
    			PTA pta_druid = (PTA) pta;
    			Alloc_Node alloc = pta_druid.getPag().makeAllocNode(base, base.getType(), null);
    			
	    		LinkedList<SootField> flds = ap.getFields();
	    		PointsToSet pts = alloc.getP2Set();
	    		Iterator<SootField> I = flds.iterator();
	    		while (I.hasNext()) {
	    			SootField fld = I.next();
	    			/* level by level, iteratively compute the result 
	    			 * by using output of last step as input of this step.*/
	    			if (fld.isStatic())
	    				pts = pta.reachingObjects(fld);
	    			else
	    				pts = pta.reachingObjects(pts, fld);
	    		}
	        	return pts;
    		}
    	}
    	
    	if (!(base instanceof Local)) {
    		return null;
    		/*//for debugging
    		System.out.println(ap);
    		System.out.flush();
    		throw new RuntimeException("Access path base is not Local!");*/
    	}
    	LinkedList<SootField> flds = ap.getFields();
    	//PointsToAnalysis pta = Scene.v().getPointsToAnalysis();
    	PointsToAnalysis pta = Helper.getInstance().getPTA();
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
    				List<SootField> lapFlds_sub = lapFlds.subList(0, idx + 1);
    				Iterator<SootField> I = lapFlds_sub.iterator();
    				while(I.hasNext()) {
    					wanted.getFields().addLast(I.next());
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
    				List<SootField> rapFlds_sub = rapFlds.subList(0, idx + 1);
    				Iterator<SootField> I = rapFlds_sub.iterator();
    				while(I.hasNext()) {
    					wanted.getFields().addLast(I.next());
    				}
    				res.add(wanted);
    			}
    		}
    	}
    	return res;
    }
    
    /**
     * ReturnStmt simply passes IN to OUT.
     * Replacing actual return value by formal return value is handled in InvokeStmt.  
     */
    private void processReturn(Unit u, Disjunction IN, Disjunction OUT) {
    	OUT.addAll(IN);
    }
    
    private void processSpecialInvoke(Unit u, Disjunction IN, Disjunction OUT) {
    	InvokeStmt inv = (InvokeStmt) u;
    	SpecialInvokeExpr inv_expr = (SpecialInvokeExpr) inv.getInvokeExpr();
    	SootMethod callee = inv_expr.getMethod();

    	//Step 0: handle special cases
    	if (callee == null || !callee.hasActiveBody()) {
    		OUT.addAll(IN);
    		return;
    	}

    	if (callee.getDeclaringClass().getName().startsWith("java")) {
    		OUT.addAll(IN);
    		return;
    	}
    	
    	//Avoid recursion
    	if (analysis.getCallStack().contains(callee)) {
    		OUT.addAll(IN);
    		return;
    	}
    	
    	//Step 1: create a new analysis environment to analyze the callee. 
    	InterNullPtrAnalysis analyzeInto = new InterNullPtrAnalysis(analysis.getCallStack(), callee, deref);
    	List<Value> actualINs = inv_expr.getArgs();
    	Value base = inv_expr.getBase();
    	analyzeInto.initCallSiteAnalysis(actualINs, base);
    	for (Unit exit : Helper.getInstance().getCFG(callee).getTails()) {
    		DataflowFact calleeIN = IN.cloneDataflowFact();
    		analyzeInto.getEnv().setIN(exit, calleeIN);
    	}
    	analyzeInto.computeFixPoint_CALL(callee);
    	Unit calleeEntry = Helper.getInstance().getCFGEntry(callee);
    	DataflowFact calleeOUT = analyzeInto.getEnv().getOUT(calleeEntry);
    	DataflowFact callerOUT = calleeOUT.clone();
    	removeLocalVars(callerOUT, callee);
    	
    	//Step 2: substitute actual parameters for formal parameters.
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
    	
    	//Step 3: substitute base pointer for "this" pointer.
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
	    	
    	//Step 4: set all member fields (excluding static fields) as null (as standard java default)
    	for (SootField fld : callee.getDeclaringClass().getFields()) {
    		if (!fld.isStatic()) {
    			for (Disjunct d : callerOUT.getDisjunctSet()) {
    	    		for (Constraint c : d.getConstraintSet()) {
    	    			AccessPath lap = c.getLeftAP();
    	    			AccessPath rap = c.getRightAP();
    	    			Value lapb = lap.getBase();
    	    			Value rapb = rap.getBase();
    	    			LinkedList<SootField> lflds = lap.getFields();
    	    			LinkedList<SootField> rflds = rap.getFields();
    	    			if (lapb != null && lapb.equals(base) && lflds.size() == 1) {
    	    				AccessPath defaultNull = new AccessPath();
    	    				defaultNull.setBase(NullConstant.v());
    	    				c.setLeftAP(defaultNull);
    	    			}
    	    			if (rapb != null && rapb.equals(base) && rflds.size() == 1) {
    	    				AccessPath defaultNull = new AccessPath();
    	    				defaultNull.setBase(NullConstant.v());
    	    				c.setRightAP(defaultNull);
    	    			}
    	    		}
    	    	}
    		}
    	}
    	
    	DataflowFact tmpIN = null;
    	DataflowFact tmpOUT = callerOUT.clone();
    	JimpleLocal thisPtr = getThis(inv_expr);
    	if (thisPtr != null) {
	    	for (SootField fld : callee.getDeclaringClass().getFields()) {
	    		if (fld.isStatic()) continue;
	    		int ln = callee.getJavaSourceStartLineNumber() + 1;
	    		AssignStmt dummyNullAsgn = getDummyNullAssignment(fld, thisPtr, ln);
	    		tmpIN = tmpOUT;
	    		tmpOUT = new DataflowFact();
	    		processPutField(dummyNullAsgn, tmpIN, tmpOUT);
	    	}
    	}
    	
    	ensureEqualityBetweenThisAndBasePtr(base, thisPtr, tmpOUT);
    	OUT.addAll(tmpOUT);
    	
    	//OUT.addAll(callerOUT);
    }
	
    private void processVirtualInvoke(Unit u, Disjunction IN, Disjunction OUT) {
    	InvokeStmt inv = (InvokeStmt) u;
    	VirtualInvokeExpr inv_expr = (VirtualInvokeExpr) inv.getInvokeExpr();
    	//SootMethod callee = inv_expr.getMethod();
    	CallGraph cg = Helper.getInstance().getCallGraph();
    	Iterator<Edge> Icallee = cg.edgesOutOf(u);
    	int numCallees = 0;
    	while(Icallee.hasNext()) {
    		Icallee.next();
    		++numCallees;
    	}
    	if (numCallees == 0 || numCallees > 300) {
    		OUT.addAll(IN);
    		return;
    	}

    	Icallee = cg.edgesOutOf(u);
    	while(Icallee.hasNext()) {
    		SootMethod callee = Icallee.next().tgt();
    		
	    	//Step -1: ModRef optimization
	    	if (!ModRefAnalysis.getInstance().hasModRef(callee, analysis.rootPts)) {
	    		OUT.addAll(IN);
	    		return;
	    	}
	    	
	    	//Step 0: handle special cases
	    	if (callee == null || !callee.hasActiveBody()) {
	    		OUT.addAll(IN);
	    		return;
	    	}
	    	
	    	if (BlackList.contains(callee.getSignature())) {
	    		OUT.addAll(IN);
	    		return;
	    	}
	    	
	    	//Avoid recursion
	    	if (analysis.getCallStack().contains(callee)) {
	    		OUT.addAll(IN);
	    		return;
	    	}
	    	
	    	//Step 1: create a new analysis environment to analyze the callee. 
	    	InterNullPtrAnalysis analyzeInto = new InterNullPtrAnalysis(analysis.getCallStack(), callee, deref);
	    	List<Value> actualINs = inv_expr.getArgs();
	    	Value base = inv_expr.getBase();
	    	analyzeInto.initCallSiteAnalysis(actualINs, base);
	    	for (Unit exit : Helper.getInstance().getCFG(callee).getTails()) {
	    		DataflowFact calleeIN = IN.cloneDataflowFact();
	    		analyzeInto.getEnv().setIN(exit, calleeIN);
	    	}
	    	analyzeInto.computeFixPoint_CALL(callee);
	    	Unit calleeEntry = Helper.getInstance().getCFGEntry(callee);
	    	DataflowFact calleeOUT = analyzeInto.getEnv().getOUT(calleeEntry);
	    	DataflowFact callerOUT = calleeOUT.clone();
	    	removeLocalVars(callerOUT, callee);
	    	
	    	//Step 2: substitute actual parameters for formal parameters.
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
	    	
	    	//Step 3: substitute base pointer for "this" pointer.
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
	    	
	    	JimpleLocal thisPtr = getThis(inv_expr);
	    	ensureEqualityBetweenThisAndBasePtr(base, thisPtr, callerOUT);
	    	OUT.addAll(callerOUT);
    	}
    }
    
    private void processStaticInvoke(Unit u, Disjunction IN, Disjunction OUT) {
    	InvokeStmt inv = (InvokeStmt) u;
    	StaticInvokeExpr inv_expr = (StaticInvokeExpr) inv.getInvokeExpr();
    	SootMethod callee = inv_expr.getMethod();
    	
    	//Step -1: ModRef optimization
    	if (!ModRefAnalysis.getInstance().hasModRef(callee, analysis.rootPts)) {
    		OUT.addAll(IN);
    		return;
    	}

    	//Step 0: handle special cases
    	if (callee == null || !callee.hasActiveBody()) {
    		OUT.addAll(IN);
    		return;
    	}
    	
    	if (BlackList.contains(callee.getSignature())) {
    		OUT.addAll(IN);
    		return;
    	}
    	
    	//Avoid recursion
    	if (analysis.getCallStack().contains(callee)) {
    		OUT.addAll(IN);
    		return;
    	}
    	
    	//Step 1: create a new analysis environment to analyze the callee. 
    	InterNullPtrAnalysis analyzeInto = new InterNullPtrAnalysis(analysis.getCallStack(), callee, deref);
    	List<Value> actualINs = inv_expr.getArgs();
    	Value base = null;
    	analyzeInto.initCallSiteAnalysis(actualINs, base);
    	for (Unit exit : Helper.getInstance().getCFG(callee).getTails()) {
    		DataflowFact calleeIN = IN.cloneDataflowFact();
    		analyzeInto.getEnv().setIN(exit, calleeIN);
    	}
    	analyzeInto.computeFixPoint_CALL(callee);
    	Unit calleeEntry = Helper.getInstance().getCFGEntry(callee);
    	DataflowFact calleeOUT = analyzeInto.getEnv().getOUT(calleeEntry);
    	DataflowFact callerOUT = calleeOUT.clone();
    	removeLocalVars(callerOUT, callee);
    	
    	//Step 2: substitute actual parameters for formal parameters.
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
    	
    	//Step 3: no need to substitute base pointer for "this" pointer, because there is not "this" pointer.
    	
    	OUT.addAll(callerOUT);
    }
    
    //SpecialInvoke seems never return a value, and this handler is dummy, put here for symmetry.
    private void processSpecialInvokeAssign(Unit u, Disjunction IN, Disjunction OUT) {
    	AssignStmt assign = (AssignStmt) u;
    	Value lv = assign.getLeftOp();
    	SpecialInvokeExpr inv_expr = (SpecialInvokeExpr) assign.getRightOp();
    	SootMethod callee = inv_expr.getMethod();

    	//Step 0: handle special cases
    	if (callee == null || !callee.hasActiveBody()) {
    		OUT.addAll(IN);
    		return;
    	}
    	
    	if (callee.getDeclaringClass().getName().startsWith("java")) {
    		OUT.addAll(IN);
    		return;
    	}
    	
    	//Avoid recursion
    	if (analysis.getCallStack().contains(callee)) {
    		OUT.addAll(IN);
    		return;
    	}
    	
    	//Step 1: create a new analysis environment to analyze the callee. 
    	InterNullPtrAnalysis analyzeInto = new InterNullPtrAnalysis(analysis.getCallStack(), callee, deref);
    	List<Value> actualINs = inv_expr.getArgs();
    	Value base = inv_expr.getBase();
    	analyzeInto.initCallSiteAnalysis(actualINs, base);
    	for (Unit exit : Helper.getInstance().getCFG(callee).getTails()) {
    		DataflowFact calleeIN = IN.cloneDataflowFact();
    		if (exit instanceof ReturnStmt) {
	    		ReturnStmt ret = (ReturnStmt) exit;
	    		Value retV = ret.getOp();
	    		
	    		//Step 1.1: substitute formal return value for actual return value.
	    		for (Disjunct d : calleeIN.getDisjunctSet()) {
	        		for (Constraint c : d.getConstraintSet()) {
	        			AccessPath lap = c.getLeftAP();
	        			AccessPath rap = c.getRightAP();
	        			Value lapb = lap.getBase();
	        			Value rapb = rap.getBase();
	        			if (lapb != null && lapb.equals(lv))
	        				lap.setBase(retV);
	        			if (rapb != null && rapb.equals(lv))
	        				rap.setBase(retV);
	        		}
	        	}
    		}
    		analyzeInto.getEnv().setIN(exit, calleeIN);
    	}
    	analyzeInto.computeFixPoint_CALL(callee);
    	Unit calleeEntry = Helper.getInstance().getCFGEntry(callee);
    	DataflowFact calleeOUT = analyzeInto.getEnv().getOUT(calleeEntry);
    	DataflowFact callerOUT = calleeOUT.clone();
    	removeLocalVars(callerOUT, callee);
    	
    	//Step 2: substitute actual parameters for formal parameters.
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
    	
    	//Step 3: substitute base pointer for "this" pointer.
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
    	
    	//Step 4: set all member fields (excluding static fields) as null (as standard java default)
    	for (SootField fld : callee.getDeclaringClass().getFields()) {
    		if (!fld.isStatic()) {
    			for (Disjunct d : callerOUT.getDisjunctSet()) {
    	    		for (Constraint c : d.getConstraintSet()) {
    	    			AccessPath lap = c.getLeftAP();
    	    			AccessPath rap = c.getRightAP();
    	    			Value lapb = lap.getBase();
    	    			Value rapb = rap.getBase();
    	    			LinkedList<SootField> lflds = lap.getFields();
    	    			LinkedList<SootField> rflds = rap.getFields();
    	    			if (lapb != null && lapb.equals(base) && lflds.size() == 1) {
    	    				AccessPath defaultNull = new AccessPath();
    	    				defaultNull.setBase(NullConstant.v());
    	    				c.setLeftAP(defaultNull);
    	    			}
    	    			if (rapb != null && rapb.equals(base) && rflds.size() == 1) {
    	    				AccessPath defaultNull = new AccessPath();
    	    				defaultNull.setBase(NullConstant.v());
    	    				c.setRightAP(defaultNull);
    	    			}
    	    		}
    	    	}
    		}
    	}
    	
    	DataflowFact tmpIN = null;
    	DataflowFact tmpOUT = callerOUT.clone();
    	JimpleLocal thisPtr = getThis(inv_expr);
    	if (thisPtr != null) {
	    	for (SootField fld : callee.getDeclaringClass().getFields()) {
	    		if (fld.isStatic()) continue;
	    		int ln = callee.getJavaSourceStartLineNumber();
	    		AssignStmt dummyNullAsgn = getDummyNullAssignment(fld, thisPtr, ln);
	    		tmpIN = tmpOUT;
	    		tmpOUT = new DataflowFact();
	    		processPutField(dummyNullAsgn, tmpIN, tmpOUT);
	    	}
    	}
    	
    	ensureEqualityBetweenThisAndBasePtr(base, thisPtr, tmpOUT);
    	OUT.addAll(tmpOUT);
    	
    	//OUT.addAll(callerOUT);
    }
    
    /** Hacking: Given a specialInvokeExpr, retrieve This pointer of its class. */
    private JimpleLocal getThis(SpecialInvokeExpr inv) {
    	Body body = inv.getMethod().getActiveBody();
    	for (Unit u : body.getUnits()) {
    		if (u instanceof IdentityStmt) {
    			Value lv = ((IdentityStmt) u).getLeftOp();
    			if (lv instanceof JimpleLocal) {
    				JimpleLocal jloc = (JimpleLocal) lv;
    				String name = jloc.getName();
    				if (name != null && name.equals("this"))
    					return jloc;
    			}
    		}
    	}
    	return null;
    }
    
    private JimpleLocal getThis(VirtualInvokeExpr inv) {
    	if (inv.getMethod().hasActiveBody() == false) return null;
    	Body body = inv.getMethod().getActiveBody();
    	for (Unit u : body.getUnits()) {
    		if (u instanceof IdentityStmt) {
    			Value lv = ((IdentityStmt) u).getLeftOp();
    			if (lv instanceof JimpleLocal) {
    				JimpleLocal jloc = (JimpleLocal) lv;
    				String name = jloc.getName();
    				if (name != null && name.equals("this"))
    					return jloc;
    			}
    		}
    	}
    	return null;
    }
    
    private AssignStmt getDummyNullAssignment(SootField fld, JimpleLocal thisPtr, int ln) {
    	SootClass cls = fld.getDeclaringClass();
		SootFieldRef fldRef = soot.Scene.v().makeFieldRef(cls, fld.getName(), fld.getType(), false);
		InstanceFieldRef instanceFieldRef = Jimple.v().newInstanceFieldRef(thisPtr, fldRef);
		AssignStmt assignStmt = new JAssignStmt(instanceFieldRef, NullConstant.v());
		lineNum_dummyAssignStmt = ln;
		return assignStmt;
    }
    
    private static int lineNum_dummyAssignStmt = -2;
    
    private void processVirtualInvokeAssign(Unit u, Disjunction IN, Disjunction OUT) {
    	AssignStmt assign = (AssignStmt) u;
    	Value lv = assign.getLeftOp();
    	VirtualInvokeExpr inv_expr = (VirtualInvokeExpr) assign.getRightOp();
    	//SootMethod callee = inv_expr.getMethod();
    	CallGraph cg = Helper.getInstance().getCallGraph();
    	Iterator<Edge> Icallee = cg.edgesOutOf(u);
    	int numCallees = 0;
    	while(Icallee.hasNext()) {
    		Icallee.next();
    		++numCallees;
    	}
    	if (numCallees == 0 || numCallees > 3) {
    		OUT.addAll(IN);
    		return;
    	}
    	
    	Icallee = cg.edgesOutOf(u);
    	while(Icallee.hasNext()) {
    		SootMethod callee = Icallee.next().tgt();
    	
	    	//Step 0: handle special cases
	    	if (callee == null || !callee.hasActiveBody()) {
	    		OUT.addAll(IN);
	    		return;
	    	}
	    	
	    	if (BlackList.contains(callee.getSignature())) {
	    		OUT.addAll(IN);
	    		return;
	    	}
	    	
	    	//Avoid recursion
	    	if (analysis.getCallStack().contains(callee)) {
	    		OUT.addAll(IN);
	    		return;
	    	}
	    	
	    	//Step 1: create a new analysis environment to analyze the callee. 
	    	InterNullPtrAnalysis analyzeInto = new InterNullPtrAnalysis(analysis.getCallStack(), callee, deref);
	    	List<Value> actualINs = inv_expr.getArgs();
	    	Value base = inv_expr.getBase();
	    	analyzeInto.initCallSiteAnalysis(actualINs, base);
	    	for (Unit exit : Helper.getInstance().getCFG(callee).getTails()) {
	    		DataflowFact calleeIN = IN.cloneDataflowFact();
	    		if (exit instanceof ReturnStmt) {
		    		ReturnStmt ret = (ReturnStmt) exit;
		    		Value retV = ret.getOp();
		    		
		    		//Step 1.1: substitute formal return value for actual return value.
		    		for (Disjunct d : calleeIN.getDisjunctSet()) {
		        		for (Constraint c : d.getConstraintSet()) {
		        			AccessPath lap = c.getLeftAP();
		        			AccessPath rap = c.getRightAP();
		        			Value lapb = lap.getBase();
		        			Value rapb = rap.getBase();
		        			if (lapb != null && lapb.equals(lv))
		        				lap.setBase(retV);
		        			if (rapb != null && rapb.equals(lv))
		        				rap.setBase(retV);
		        		}
		        	}
	    		}
	    		analyzeInto.getEnv().setIN(exit, calleeIN);
	    	}
	    	analyzeInto.computeFixPoint_CALL(callee);
	    	Unit calleeEntry = Helper.getInstance().getCFGEntry(callee);
	    	DataflowFact calleeOUT = analyzeInto.getEnv().getOUT(calleeEntry);
	    	DataflowFact callerOUT = calleeOUT.clone();
	    	removeLocalVars(callerOUT, callee);
	    	
	    	//Step 2: substitute actual parameters for formal parameters.
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
	    	
	    	//Step 3: substitute base pointer for "this" pointer.
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
	    	
	    	JimpleLocal thisPtr = getThis(inv_expr);
	    	ensureEqualityBetweenThisAndBasePtr(base, thisPtr, callerOUT);
	    	
	    	OUT.addAll(callerOUT);
    	}
    }
    
    private void processStaticInvokeAssign(Unit u, Disjunction IN, Disjunction OUT) {
    	AssignStmt assign = (AssignStmt) u;
    	Value lv = assign.getLeftOp();
    	StaticInvokeExpr inv_expr = (StaticInvokeExpr) assign.getRightOp();
    	SootMethod callee = inv_expr.getMethod();

    	//Step 0: handle special cases
    	if (callee == null || !callee.hasActiveBody()) {
    		OUT.addAll(IN);
    		return;
    	}
    	
    	if (BlackList.contains(callee.getSignature())) {
    		OUT.addAll(IN);
    		return;
    	}
    	
    	//Avoid recursion
    	if (analysis.getCallStack().contains(callee)) {
    		OUT.addAll(IN);
    		return;
    	}
    	
    	//Step 1: create a new analysis environment to analyze the callee. 
    	InterNullPtrAnalysis analyzeInto = new InterNullPtrAnalysis(analysis.getCallStack(), callee, deref);
    	List<Value> actualINs = inv_expr.getArgs();
    	Value base = null;
    	analyzeInto.initCallSiteAnalysis(actualINs, base);
    	for (Unit exit : Helper.getInstance().getCFG(callee).getTails()) {
    		DataflowFact calleeIN = IN.cloneDataflowFact();
    		if (exit instanceof ReturnStmt) {
	    		ReturnStmt ret = (ReturnStmt) exit;
	    		Value retV = ret.getOp();
	    		
	    		//Step 1.1: substitute formal return value for actual return value.
	    		for (Disjunct d : calleeIN.getDisjunctSet()) {
	        		for (Constraint c : d.getConstraintSet()) {
	        			AccessPath lap = c.getLeftAP();
	        			AccessPath rap = c.getRightAP();
	        			Value lapb = lap.getBase();
	        			Value rapb = rap.getBase();
	        			if (lapb != null && lapb.equals(lv))
	        				lap.setBase(retV);
	        			if (rapb != null && rapb.equals(lv))
	        				rap.setBase(retV);
	        		}
	        	}
    		}
    		analyzeInto.getEnv().setIN(exit, calleeIN);
    	}
    	analyzeInto.computeFixPoint_CALL(callee);
    	Unit calleeEntry = Helper.getInstance().getCFGEntry(callee);
    	DataflowFact calleeOUT = analyzeInto.getEnv().getOUT(calleeEntry);
    	DataflowFact callerOUT = calleeOUT.clone();
    	removeLocalVars(callerOUT, callee);

    	
    	//Step 2: substitute actual parameters for formal parameters.
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
    	
    	//Step 3: no need to substitute base pointer for "this" pointer, because there is not "this" pointer.
    	
    	OUT.addAll(callerOUT);
    }
    
    private void processThrow(Unit u, Disjunction IN, Disjunction OUT) {
    	OUT.setAsFalse();
    }
    
	public DataflowFact transfer(Unit u, DataflowFact IN, DataflowFact oldOUT) {
		
		DataflowFact newOUT = new DataflowFact();
		newOUT.setAge(IN.getAge() + 1);
		
		if (u != deref) { //do not make the assumption for the deref itself which is under verification
			IN = IN.clone();
	    	assumeBaseNotNull(u, IN);
		}
		
		if (isCopy(u)) {
			System.out.println("+++ handling Copy : " + u.getJavaSourceStartLineNumber() + " : " + u);
			processCopy(u, IN, newOUT);
		}
		else if (isCasting(u)) {
			System.out.println("+++ handling Casting : " + u.getJavaSourceStartLineNumber() + " : " + u);
			processCasting(u, IN, newOUT);
		}
		else if (isFormalIn(u)) {
			//inter-procedural analysis
			System.out.println("+++ handling FormalIN : " + u.getJavaSourceStartLineNumber() + " : " + u);
			processFormalIN(u, IN, newOUT);
		}
		else if (isNullAssign(u)) {
			System.out.println("+++ handling NullAssign : " + u.getJavaSourceStartLineNumber() + " : " + u);
			processNullAssign(u, IN, newOUT);
		}
		else if (isNewAssign(u)) {
			System.out.println("+++ handling NewAssign : " + u.getJavaSourceStartLineNumber() + " : " + u);
			processNewAssign(u, IN, newOUT);
		}
		else if (isGetField(u)) {
			System.out.println("+++ handling GetField : " + u.getJavaSourceStartLineNumber() + " : " + u);
			processGetField(u, IN, newOUT);
		}
		else if (isStaticGetField(u)) {
			System.out.println("+++ handling StaticGetField : " + u.getJavaSourceStartLineNumber() + " : " + u);
			processStaticGetField(u, IN, newOUT);
		}
		else if (isIf(u)) {
			System.out.println("+++ handling If : " + u.getJavaSourceStartLineNumber() + " : " + u);
			processIf(u, IN, newOUT);
		}
		else if (isSwitch(u)) {
			System.out.println("+++ handling Switch : " + u.getJavaSourceStartLineNumber() + " : " + u);
			processSwitch(u, IN, newOUT);
		}
		else if (isGoto(u)) {
			System.out.println("+++ handling Goto : " + u.getJavaSourceStartLineNumber() + " : " + u);
			processGoto(u, IN, newOUT);
		}
		else if (isExprAssign(u)) {
			System.out.println("+++ handling ExprAssign : " + u.getJavaSourceStartLineNumber() + " : " + u);
			processExprAssign(u, IN, newOUT);
		}
		else if (isGetArray(u)) {
			System.out.println("+++ handling GetArray : " + u.getJavaSourceStartLineNumber() + " : " + u);
			processGetArray(u, IN, newOUT);
		}
		else if (isPutArray(u)) {
			//This requires alias analysis
			System.out.println("+++ handling PutArray : " + u.getJavaSourceStartLineNumber() + " : " + u);
			processPutArray(u, IN, newOUT);
		}
		else if (isPutField(u)) {
			//This requires alias analysis
			System.out.println("+++ handling PutField : " + u.getJavaSourceStartLineNumber() + " : " + u);
			processPutField(u, IN, newOUT);
		}
		else if (isStaticPutField(u)) {
			System.out.println("+++ handling StaticPutField : " + u.getJavaSourceStartLineNumber() + " : " + u);
			processStaticPutField(u, IN, newOUT);
		}
		else if (isReturn(u)) {
			//inter-procedural analysis
			System.out.println("+++ handling Return : " + u.getJavaSourceStartLineNumber() + " : " + u);
			processReturn(u, IN, newOUT);
		}
		else if (isStaticInvoke(u)) {
			//inter-procedural analysis
			System.out.println("+++ handling StaticInvoke : " + u.getJavaSourceStartLineNumber() + " : " + u);
			processStaticInvoke(u, IN, newOUT);
		}
		else if (isSpecialInvoke(u)) {
			System.out.println("+++ handling SpecialInvoke : " + u.getJavaSourceStartLineNumber() + " : " + u);
			processSpecialInvoke(u, IN, newOUT);
		}
		else if (isVirtualInvoke(u)) {
			//inter-procedural analysis
			System.out.println("+++ handling VirtualInvoke : " + u.getJavaSourceStartLineNumber() + " : " + u);
			processVirtualInvoke(u, IN, newOUT);
		}
		else if (isStaticInvokeAssign(u)) {
			//inter-procedural analysis
			System.out.println("+++ handling StaticInvokeAssign : " + u.getJavaSourceStartLineNumber() + " : " + u);
			processStaticInvokeAssign(u, IN, newOUT);
		}
		else if (isSpecialInvokeAssign(u)) {
			//inter-procedural analysis
			System.out.println("+++ handling SpecialInvokeAssign : " + u.getJavaSourceStartLineNumber() + " : " + u);
			processSpecialInvokeAssign(u, IN, newOUT);
		}
		else if (isVirtualInvokeAssign(u)) {
			//inter-procedural analysis
			System.out.println("+++ handling VirtualInvokeAssign : " + u.getJavaSourceStartLineNumber() + " : " + u);
			processVirtualInvokeAssign(u, IN, newOUT);
		}
		else if (isThrow(u)) {
			System.out.println("+++ handling Throw : " + u.getJavaSourceStartLineNumber() + " : " + u);
			processThrow(u, IN, newOUT);
		}
		else if (isBinopExprAssign(u)) {
			System.out.println("+++ handling BinopExprAssign : " + u.getJavaSourceStartLineNumber() + " : " + u);
			newOUT.addAll(IN);
		}
		else {
			/*//for debugging
			newOUT.addAll(IN);
			System.err.println("=======Unkown " + u.getJavaSourceStartLineNumber() + " : " + u);
			if (u instanceof DefinitionStmt) {
				System.err.println(((DefinitionStmt) u).getLeftOp().getClass());
				System.err.println(((DefinitionStmt) u).getRightOp().getClass());
				System.err.println();
			}
			else {
				System.err.println(u.getClass());
			}
			throw new RuntimeException("Unkown statement! Additional handling required!");*/
		}
		
		Solver.getInstance().solve(newOUT);
		//newOUT.simpify();
		
		aging(newOUT);
		
		return newOUT;
	}
	
	/** Hacking: thisPtr and basePtr of a virtual or special invoke are must-alias.*/
	private void ensureEqualityBetweenThisAndBasePtr(Value base, Value thisPtr, DataflowFact fact) {
		for (Disjunct d : fact.getDisjunctSet()) {
			for (Constraint c : d.getConstraintSet()) {
				AccessPath lap = c.getLeftAP();
				AccessPath rap = c.getRightAP();
				if (lap.getBase() == thisPtr)
					lap.setBase(base);
				if (rap.getBase() == thisPtr)
					rap.setBase(base);
			}
		}
	}
	
	
	private void aging (DataflowFact df) {
		aging();
		
		if (df.logicValue == LogicValue.True) {
			df.setAsTrue();
			return;
		}
		
		df.aging();
		if (df.getAge() > Disjunction.threshold_age) {
			df.setAsFalse();
			OutOfBudget.setTrue();
			return;
		}
		
		ArrayList<Constraint> toDel_constr = new ArrayList<>();
		ArrayList<Disjunct> toDel_disjunct = new ArrayList<>();
		for (Disjunct d : df.getDisjunctSet()) {
			d.aging();
			if (d.getAge() > Disjunct.threshold_age) {
				toDel_disjunct.add(d);
			}
			else {
				toDel_constr.clear();
				for (Constraint c : d.getConstraintSet()) {
					c.aging();
					if (c.getAge() > Constraint.threshold_k1) 
						toDel_constr.add(c);
				}
				
				for (Constraint c : toDel_constr) 
					d.remove(c);
				
				if (d.isEmpty() && d.logicValue != LogicValue.True)
					toDel_disjunct.add(d);
			}
		}
		
		for (Disjunct d : toDel_disjunct) 
			df.remove(d);
	}
	
	//Soot does not implement equal function for ArrayRef, so we have to handle it here.
	private boolean sameValue(Value v1, Value v2) {
		if (v1 == null || v2 == null)
			return v1 == v2;
		if (v1 instanceof ArrayRef && v2 instanceof ArrayRef) {
			ArrayRef a1 = (ArrayRef) v1;
			ArrayRef a2 = (ArrayRef) v2;
			if (a1.getBase() == null) {
				if (a1.getIndex() == null)
					return a2.getIndex() == null;
				else
					return a2.getBase() == null && a1.getIndex().equals(a2.getIndex());
			}
			return a1.getBase().equals(a2.getBase())
					&& a1.getIndex().equals(a2.getIndex());
		}
		return v1.equals(v2);
	}
	
	private void removeLocalVars(DataflowFact calleeOUT, SootMethod callee) {
		if (callee.hasActiveBody() == false) return;
		Body b = callee.getActiveBody();
		ArrayList<Constraint> todel_c = new ArrayList<>();
		for (Local local : b.getLocals()) {
			for (Disjunct d : calleeOUT.getDisjunctSet()) {
				todel_c.clear();
				for (Constraint c : d.getConstraintSet()) {
					if (c instanceof NullAssumption) continue; // Never remove root constraint.
					Value lb = c.getLeftAP().getBase();
					Value rb = c.getRightAP().getBase();
					if (local.equals(lb) || local.equals(rb)) {
						todel_c.add(c);
					}
				}
				d.getConstraintSet().removeAll(todel_c);
			}
		}
	}
	
	//to guarantee termination
	private static int age = 0; 
	final public static int threshold = 10000;
	
	public static int aging() {
		/*System.err.println(age);
		System.err.flush();*/
		return ++age;
	}
	
	public static boolean outOfBudget() {
		return age > threshold;
	}
	
	public static void reset() {
		age = 0;
	}
	
	public void setDeref(Unit u) {
		deref = u;
	}
	private Unit deref; //the dereference site being verified
	private DataflowFactMapping env;
	private InterNullPtrAnalysis analysis;
	
}