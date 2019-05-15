package adt;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Objects;

import solver.Solver;
import soot.Local;
import soot.Value;
import soot.jimple.ThisRef;

/**
 * Each disjunct is standard as in mathematical logic,
 *  i.e., in the form of "C1 and C2 and ... and Cm",
 *  where Cm is a constraint designed specifically for null-pointer-detection client.
 *  @see Constraint
 */
public class Disjunct {
	
	public Disjunct() {
		constraintSet = new HashSet<>();
	}
	
	public Disjunct(Disjunct d) {
		this();
		this.constraintSet.addAll(d.constraintSet);
	}
	
	public Disjunct(Constraint c) {
		this();
		constraintSet.add(c);
	}
	
	/**
	 * Deep clone by cloning each constraint in the constraint set.
	 */
	public Disjunct clone() {
		Disjunct clone = new Disjunct();
		for (Constraint c : constraintSet) {
			Constraint c_clone = c.clone();
			clone.add(c_clone);
		}
		clone.logicValue = logicValue;
		clone.setAge(this.getAge());
		return clone;
	}
	
	public void add(Constraint c) {
		if (constraintSet.contains(c))
			return;
		constraintSet.add(c);
		dropOld();
	}
	
	public void addAll(Disjunct d) {
		for (Constraint c : d.constraintSet) {
			add(c);
		}
	}
	
	final static int k2 = 5;
	
	public void dropOld() {
		if (constraintSet.size() <= k2) return;
		
		Constraint oldest = constraintSet.iterator().next();
		for (Constraint c : constraintSet) {
			if (c.getAge() > oldest.getAge())
				oldest = c;
		}
		remove(oldest);
	}
	
	public void remove(Constraint c) {
		constraintSet.remove(c);
	}
	
	public boolean isEmpty() {
		return constraintSet.isEmpty();
	}
	
	public HashSet<Constraint> getConstraintSet() {
		return constraintSet;
	}
	
	public int size() {
		return constraintSet.size();
	}
	
	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof Disjunct)) {
			return false;
		}
		Disjunct d = (Disjunct) obj;
		return Objects.equals(this.constraintSet, d.constraintSet);
	}
	
	@Override
	public int hashCode() {
		return Objects.hash(constraintSet);
	}
	
	@Override
	public String toString() {
		StringBuffer res = new StringBuffer();
		Iterator<Constraint> I = constraintSet.iterator();
		if (I.hasNext()) {
			Constraint c = I.next();
			res.append(c);
		}
		while (I.hasNext()) {
			res.append("\n       && ");
			Constraint c = I.next();
			res.append(c);
		}
		res.append("\n       [Disjunct_Eval: ").append(logicValue).append("]");
		return res.toString();
	}
	
	/**
	 * Given another Disjunct d, return if this subsumes d.
	 * X subsuming Y means that X is a stronger condition than Y.
	 * E.g., "a==b && c==d" subsumes "a==b",
	 *  and "a==b && c==d" does not subsume "b==c".
	 */
	public boolean subsume(Disjunct d) {
		for (Constraint c : d.constraintSet) {
			if (c.logicValue == LogicValue.True)
				continue;
			if (!this.constraintSet.contains(c))
				return false;
		}
		return true;
	}
	
	public LogicValue solve() {
		return Solver.getInstance().solve(this);
	}
	
	/**
	 * Simplify a disjunct by dropping all TRUE constraint.
	 */
	public void simpify() {
		ArrayList<Constraint> toDrop = new ArrayList<>();
		for (Constraint c : constraintSet) {
			if (c instanceof NullAssumption) // Never drop the null assumption. 
				continue;
			if (c.solve() == LogicValue.True) {
				toDrop.add(c);
			}
		}
		for (Constraint c : toDrop) {
			constraintSet.remove(c);
		}
	}
	
	public LogicValue logicValue = LogicValue.Uninitialized;
	
	private HashSet<Constraint> constraintSet;
	
	//age of the current constraint, the constraint will be dropped if its age reaches threshold.
	private int age = 1; 
	final public static int threshold_age = 1000;
	
	public int aging() {
		return ++age;
	}
	
	public int getAge() {
		return age;
	}
	
	public void setAge(int age) {
		this.age = age;
	}
	
	/** Check if the disjunct contains ThisRef as one of its access paths.
	 * If so, the "null == null" heuristic should not be applied immediately.
	 * In other words, the "null == null" heuristic should be applied until 
	 * there is no ThisRef in the disjunct.
	 * This way, the precision can be improved.*/
	public boolean containsThisPtr() {
		//System.err.println(this);
		for (Constraint c : constraintSet) {
			Value lb = c.left.getBase();
			Value rb = c.right.getBase();
			if (lb instanceof ThisRef
			 || rb instanceof ThisRef) {
				return true;
			}
			if (lb instanceof Local) {
				Local lb_loc = (Local) lb;
				if ("this".equals(lb_loc.getName())) {
					return true;
				}
			}
			if (rb instanceof Local) {
				Local rb_loc = (Local) rb;
				if ("this".equals(rb_loc.getName())) {
					return true;
				}
			}
		}
		return false;
	}
}
