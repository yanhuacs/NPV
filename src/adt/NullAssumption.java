package adt;

import soot.Value;

/**
 * NullAssumption is the initial assumption made at the pointer dereference site.
 * This is a special constraint that we should not drop during analysis,
 * and therefore its age is always 0.
 */
public class NullAssumption extends Constraint {
	public NullAssumption(Value l, Value r, boolean isEqual, int ln) {
		super(l, r, isEqual, ln);
		setAge(0);
	}
	
	public NullAssumption(AccessPath leftAP, AccessPath rightAP, boolean isEqual, int ln) {
		super(leftAP, rightAP, isEqual, ln);
		setAge(0);
	}
	
	public int aging() {
		setAge(0);
		return 0;
	}
	
	/**
	 * Deep clone by cloning access paths.
	 */
	public NullAssumption clone() {
		AccessPath left_clone = getLeftAP().clone();
		AccessPath right_clone = getRightAP().clone();
		NullAssumption clone = new NullAssumption(left_clone, right_clone, isEqual(), this.getLineNum());
		clone.logicValue = logicValue;
		clone.setAge(0);
		return clone;
	}
	
	@Override
	public String toString() {
		if (this.isEqual)
			return left.toString() + " == " + right.toString() 
								   + " @ ln " + ln 
								   + " (Contraint_Eval: " + logicValue + ")"
								   + "  RootAssumption";
		else
			return left.toString() + " != " + right.toString() 
								   + " @ ln " + ln 
								   + " (Contraint_Eval: " + logicValue + ")"
								   + "  RootAssumption";
	}
}