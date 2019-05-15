package adt;

import java.util.Objects;

import solver.Solver;
import soot.Type;
import soot.Value;
import soot.jimple.NullConstant;

/**
 * Each constraint describes an equality/inequality relation between two access paths,
 *  which is in the form of "AP_left == AP_right" or "AP_left != AP_right".
 *  @see AccessPath    
 */
public class Constraint {

	public Constraint() {
		left = new AccessPath();
		right = new AccessPath();
		isEqual = false;
	}
	
	public Constraint(Value l, Value r, boolean isEqual, int ln) {
		left = new AccessPath(l);
		right = new AccessPath(r);
		this.isEqual = isEqual;
		this.ln = ln;
	}
	
	public Constraint(Value l, Value r, boolean isEqual) {
		this(l, r, isEqual, 0);
	}
	
	public Constraint(AccessPath leftAP, AccessPath rightAP, boolean isEqual, int ln) {
		this.left = leftAP;
		this.right = rightAP;
		this.isEqual = isEqual;
		this.ln = ln;
	}
	
	/**
	 * Deep clone by cloning access paths.
	 */
	public Constraint clone() {
		AccessPath left_clone = left.clone();
		AccessPath right_clone = right.clone();
		Constraint clone = new Constraint(left_clone, right_clone, isEqual, ln);
		clone.logicValue = logicValue;
		clone.setAge(this.getAge());
		return clone;
	}
	
	public AccessPath getLeftAP() {
		return left;
	}
	
	public AccessPath getRightAP() {
		return right;
	}
	
	public void setLeftAP(AccessPath ap) {
		left = ap;
	}
	
	public void setRightAP(AccessPath ap) {
		right = ap;
	}
	
	public int getLineNum() {
		return ln;
	}
	
	public void setLineNum(int ln) {
		this.ln = ln;
	}
	
	public boolean isEqual() {
		return isEqual;
	}
	
	public void setIsEqual(boolean isEqual) {
		this.isEqual = isEqual;
	}
	
	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof Constraint)) {
			return false;
		}
		Constraint c = (Constraint) obj;
		return  (  Objects.equals(this.left, c.left)
				&& Objects.equals(this.right, c.right)
				&& this.isEqual == c.isEqual )
				||
				(  Objects.equals(this.left, c.right)
				&& Objects.equals(this.right, c.left)
				&& this.isEqual == c.isEqual )
				;
	}
	
	@Override
	public int hashCode() {
		return Objects.hash(left, right, isEqual);
	}
	
	@Override
	public String toString() {
		//return Integer.toString(ln);
		Type lty = left.getBase() == null ? NullConstant.v().getType() : left.getBase().getType();
		Type rty = right.getBase() == null ? NullConstant.v().getType() : right.getBase().getType();
		if (isEqual)
			return lty + " " +left.toString() + " == " + rty + " " + right.toString()
					//left.toString() + " == " + right.toString()
								   + " @ ln " + ln 
								   + " (Contraint_Eval: " + logicValue + ")";
		else
			return  lty + " " + left.toString() + " != " + rty + " " + right.toString()
					//left.toString() + " != " + right.toString()
								   + " @ ln " + ln 
								   + " (Contraint_Eval: " + logicValue + ")";
	}
	
	public LogicValue solve() {
		return Solver.getInstance().solve(this);
	}
	
	public LogicValue logicValue = LogicValue.Uninitialized;
	
	protected AccessPath left;
	protected AccessPath right;
	protected boolean isEqual;
	protected int ln; // line number in the program
	
	//age of the current constraint, the constraint will be dropped if its age reaches threshold.
	private int age = 1; 
	final public static int threshold_k1 = 1000;
	
	public int aging() {
		return ++age;
	}
	
	public int getAge() {
		return age;
	}
	
	public void setAge(int age) {
		this.age = age;
	}
}