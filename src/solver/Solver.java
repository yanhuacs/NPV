package solver;
import java.util.LinkedList;

import adt.AccessPath;
import adt.Constraint;
import adt.Disjunct;
import adt.Disjunction;
import adt.LogicValue;
import adt.NullAssumption;
import soot.Local;
import soot.SootField;
import soot.Value;
import soot.jimple.NewArrayExpr;
import soot.jimple.NewExpr;
import soot.jimple.NullConstant;
import soot.jimple.ParameterRef;
import soot.jimple.ThisRef;

public class Solver {
	
	public static Solver getInstance() {
		if (instance == null)
			instance = new Solver();
		return instance;
	}
	
	private static Solver instance = null;
	
	public LogicValue solve(Disjunction disjunction) {
		disjunction.logicValue = LogicValue.False;
		for (Disjunct disjunct : disjunction.getDisjunctSet()) {
			disjunct.logicValue = solve(disjunct);
			if (disjunct.logicValue == LogicValue.True) { 
				disjunction.logicValue = LogicValue.True;
				//return disjunction.logicValue;
			}
			if (disjunction.logicValue != LogicValue.True && disjunct.logicValue == LogicValue.Unkown) {
				disjunction.logicValue = LogicValue.Unkown;
				//return disjunction.logicValue;
			}
		}
		disjunction.simpify();
		return disjunction.logicValue;
	}
	
	public LogicValue solve(Disjunct disjunct) {
		disjunct.logicValue = LogicValue.True;
		for (Constraint c1 : disjunct.getConstraintSet()) {
			c1.logicValue = solve(c1);
			switch (c1.logicValue) {
			case True:
				break;
				
			case False:
				disjunct.logicValue = LogicValue.False; 
				return LogicValue.False;
				//break;
				
			default: //LogicValue.Unkown
				if (disjunct.logicValue == LogicValue.False)
					return LogicValue.False;
				disjunct.logicValue = LogicValue.Unkown;
				for (Constraint c2 : disjunct.getConstraintSet()) {
					if (c1 == c2) continue;
					AccessPath c1l = c1.getLeftAP();
					AccessPath c1r = c1.getRightAP();
					AccessPath c2l = c2.getLeftAP();
					AccessPath c2r = c2.getRightAP();
					
					/*Case: c1 is "x == y", c2 is "x != y",
					    or  c1 is "x != y", c2 is "x == y",*/
					if (c1l.equals(c2l) && c1r.equals(c2r)) {
						if (c1.isEqual() != c2.isEqual())
							disjunct.logicValue = LogicValue.False; 
							return LogicValue.False;
					}
					/*Case: c1 is "x == y", c2 is "y != x",
				        or  c1 is "x != y", c2 is "y == x",*/
					else if (c1l.equals(c2r) && c1r.equals(c2l)) {
						if (c1.isEqual() != c2.isEqual()) {
							disjunct.logicValue = LogicValue.False; 
							return LogicValue.False;
						}
					}
				}
				break;
			}
		}
		
		/* Aggressive evaluation here. 
		 * The "null == null" heuristic. */
		/* At this step, disjunct is not False,
		 * because if it is False, the function should have already returned.*/
		/* Check if the disjunct contains ThisRef as one of its access paths.
		 * If so, the "null == null" heuristic should not be applied immediately.
		 * In other words, the "null == null" heuristic should be applied until 
		 * there is no ThisRef in the disjunct.
		 * This way, the precision can be improved.*/
		for (Constraint c : disjunct.getConstraintSet()) {
			if (c instanceof NullAssumption) {
				if (c.logicValue == LogicValue.True) {
					boolean hasThisPtr = disjunct.containsThisPtr();
					//System.err.println("hasThisPtr = " + hasThisPtr + "\n\n");
					if (hasThisPtr == false) {
						boolean needsToExtendExam = (decreaseExtendingSteps() > 0);
						if (needsToExtendExam == false) {
							//System.err.println("!!!!!!!!!!!!!!true");
							//System.exit(1);
							disjunct.logicValue = LogicValue.True;
							return LogicValue.True;
						}
					}
				}
				break;
			}
		}
		return disjunct.logicValue;
	}
	
	/* Number of steps that needs to go after "null=null" is found.
	 * This is to ensure precision (i.e., eliminate false positives). */
	private static int numExtendingSteps; 
	private static int decreaseExtendingSteps() {
		System.out.println("numExtendingSteps = " + numExtendingSteps);
		if (numExtendingSteps == 0) return 0;
		return --numExtendingSteps;
	}
	public static void resetNumExtendingSteps() {
		numExtendingSteps = 1000;
	}
	
	
	public LogicValue solve(Constraint constraint) {
		AccessPath l = constraint.getLeftAP();
		AccessPath r = constraint.getRightAP();
		Value lb = l.getBase();
		Value rb = r.getBase();
		LinkedList<SootField> lf = l.getFields();
		LinkedList<SootField> rf = r.getFields();
		// null.f can exist only when f is a static field
		if (lb instanceof NullConstant) {
			for (SootField fld : lf) {
				if (fld.isStatic() == false) {
					constraint.logicValue = LogicValue.False;
					return LogicValue.False;
				}
			}
		}
		// null.f can exist only when f is a static field
		if (rb instanceof NullConstant) {
			for (SootField fld : rf) {
				if (fld.isStatic() == false) {
					constraint.logicValue = LogicValue.False;
					return LogicValue.False;
				}
			}
		}
		
		boolean isEqual = constraint.isEqual();
		if (isEqual) {
			if (l.equals(r)) { // Handle constraint like  "x == y"
				constraint.logicValue = LogicValue.True;
			}
			else if ( lb instanceof NullConstant
					&& rf.isEmpty()
					&& (rb instanceof NewExpr || rb instanceof NewArrayExpr) ) {
				constraint.logicValue = LogicValue.False;
			}
			else if ( rb instanceof NullConstant
				  && lf.isEmpty()
				  && (lb instanceof NewExpr || lb instanceof NewArrayExpr) ) {
				constraint.logicValue = LogicValue.False;
			}
			else if ( l != r
					&& lf.isEmpty() 
					&& rf.isEmpty()
					&& (lb instanceof NewExpr || lb instanceof NewArrayExpr)
					&& (rb instanceof NewExpr || rb instanceof NewArrayExpr)) {
				// Two allocation sites are definitely different objects.
				constraint.logicValue = LogicValue.False;
			}
			//assume "this == null" is false
			else if ( lb instanceof NullConstant
					&& rf.isEmpty()
					&&   ( rb instanceof ThisRef 
					   ||  rb instanceof Local && "this".equals(((Local) rb).getName()))
					) {
				constraint.logicValue = LogicValue.False;
			}
			//assume "this == null" is false
			else if ( rb instanceof NullConstant
					&& lf.isEmpty()
					&&   ( lb instanceof ThisRef
					   ||  lb instanceof Local && "this".equals(((Local) lb).getName()))
					) {
				constraint.logicValue = LogicValue.False;
			}
			else {
				constraint.logicValue = LogicValue.Unkown;
			}
		}
		else {				  // Handle constraint like "x != y"
			if (l.equals(r)) {
				constraint.logicValue = LogicValue.False;
			}
			else if (lf.isEmpty() && rf.isEmpty()
				 && (lb instanceof NewExpr      || lb instanceof NewArrayExpr)
				 && (rb instanceof NullConstant)) {
				// "new != null" is true
				constraint.logicValue = LogicValue.True;
			}
			else if (lf.isEmpty() && rf.isEmpty()
				 && (rb instanceof NewExpr      || rb instanceof NewArrayExpr)
				 && (lb instanceof NullConstant)) {
			// "null != new" is true
				constraint.logicValue = LogicValue.True;
			}
			//assume "null != this" is true
			else if ( lb instanceof NullConstant
					&& rf.isEmpty()
					&&  ( rb instanceof ThisRef 
					   || rb instanceof Local && "this".equals(((Local) rb).getName()))
					) {
				constraint.logicValue = LogicValue.True;
			}
			//assume "this != null" is true
			else if ( rb instanceof NullConstant
					&& lf.isEmpty()
					&&   ( lb instanceof ThisRef
					   ||  lb instanceof Local && "this".equals(((Local) lb).getName()))
					) {
				constraint.logicValue = LogicValue.True;
			}
			else if (lf.isEmpty() && rf.isEmpty()
					 && lb != rb 
					 &&  ((lb instanceof NewExpr && rb instanceof NewExpr)
					   || (lb instanceof NewArrayExpr && rb instanceof NewArrayExpr)))
				{
				// "new A != new B" is true
					constraint.logicValue = LogicValue.True;
				}
			else {
				constraint.logicValue = LogicValue.Unkown;
			}
		}
		
		return constraint.logicValue;
	}

}
