package adt;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Objects;

import solver.Solver;
import soot.jimple.NullConstant;

/**
 * Each disjunction as standard in mathematical logic,
 *  i.e., in the form of "D1 or D2 or ... or Dn",
 *  where Di is a standard disjunct as in mathematical logic.
 *  @see Disjunct
 */
public class Disjunction {
	
	public Disjunction() {
		disjunctSet = new HashSet<>();
	}
	
	public Disjunction(Disjunct d) {
		this();
		disjunctSet.add(d);
	}
	
	/**
	 * Deep clone by cloning each constraint in the constraint set.
	 */
	public Disjunction clone() {
		Disjunction clone = new Disjunction();
		for (Disjunct d : disjunctSet) {
			Disjunct d_clone = d.clone();
			clone.add(d_clone);
		}
		clone.logicValue = logicValue;
		clone.setAge(this.getAge());
		return clone;
	}
	
	/**
	 * Deep clone by cloning each constraint in the constraint set.
	 */
	public DataflowFact cloneDataflowFact() {
		DataflowFact clone = new DataflowFact();
		for (Disjunct d : disjunctSet) {
			Disjunct d_clone = d.clone();
			clone.add(d_clone);
		}
		clone.logicValue = logicValue;
		clone.setAge(this.getAge());
		return clone;
	}
	
	public void add(Disjunct d) {
		Iterator<Disjunct> I = disjunctSet.iterator();
		while (I.hasNext()) {
			Disjunct d_old = I.next();
			if (d.subsume(d_old)) {
				return;
			}
		}
		
		I = disjunctSet.iterator();
		while (I.hasNext()) {
			Disjunct d_old = I.next();
			if (d_old.subsume(d)) {
				I.remove();
			}
		}
		disjunctSet.add(d);
	}
	
	public void addAll(Disjunction ds) {
		/* if this disjunction is already true, 
		 * then nothing needs to be added. */
		this.solve();
		if (this.logicValue == LogicValue.True) 
			return;
		
		/* if the disjunction to be added is evaluated as true, 
		 * then nothing needs to be added, 
		 * just set the whole disjunction as true. */
		ds.solve();
		if (ds.logicValue == LogicValue.True) {
			this.setAsTrue();
			return;
		}
		
		for (Disjunct d : ds.disjunctSet) {
			add(d.clone());
		}
	}
	
	public void remove(Disjunct d) {
		disjunctSet.remove(d);
	}
	
	public boolean isEmpty() {
		return disjunctSet.isEmpty();
	}
	
	public HashSet<Disjunct> getDisjunctSet() {
		return disjunctSet;
	}
	
	public int size() {
		return disjunctSet.size();
	}
	
	public boolean outOfBudget() {
		return size() > threshold_num_paths;
	}
	
	private int threshold_num_paths = 500; //number of paths
	
	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof Disjunction)) {
			return false;
		}
		Disjunction d = (Disjunction) obj;
		return Objects.equals(this.disjunctSet, d.disjunctSet);
	}
	
	@Override
	public int hashCode() {
		return Objects.hash(disjunctSet);
	}
	
	@Override
	public String toString() {
		StringBuffer res = new StringBuffer();
		Iterator<Disjunct> I = disjunctSet.iterator();
		if (I.hasNext()) {
			Disjunct d = I.next();
			res.append(d);
		}
		while (I.hasNext()) {
			res.append("\n    || ");
			Disjunct d = I.next();
			res.append(d);
		}
		res.append("\n    {Disjunction_Eval: ").append(logicValue).append("}");
		return res.toString();
	}
	
	/**
	 * Compare this disjunction with another one.
	 * @param fact is the disjunction to be compared with.
	 * @return if this disjunction is a condition weaker than the other.
	 */
	public boolean isWeakerThan(Disjunction other) {
		this.solve();
		other.solve();
		if (this.logicValue == LogicValue.True //&& this.size() == 1
				&& other.logicValue != LogicValue.True) {
			return true;
		}
		if (other.logicValue == LogicValue.True)
			return false;
		
		if (this.size() < other.size())
			return false;
		/*For each disjunct d_other in "other",
		there should be a disjunct d_this in "this" that is weaker than d_other,
		in order to guarantee "this" is weaker than "other" */ 
		for (Disjunct d_other : other.disjunctSet) {
			boolean hasWeaker = false;
			for (Disjunct d_this : this.disjunctSet) {
				if (d_other.subsume(d_this)) {
					hasWeaker = true;
					break;
				}
			}
			if (hasWeaker == false)
				return false;
		}
		return true;
	}
	
	public boolean isStrictWeakerThan(Disjunction other) {
		return this.isWeakerThan(other) && !other.isWeakerThan(this);
	}
	
	public LogicValue solve() {
		return Solver.getInstance().solve(this);
	}
	
	/**
	 * Simplify a disjunct by dropping all FALSE constraint.
	 */
	public void simpify() {
		if (this.logicValue == LogicValue.False) {
			setAsFalse();
			return;
		}
		
		if (this.logicValue == LogicValue.True) {
			setAsTrue();
			return;
		}
		
		ArrayList<Disjunct> toDrop = new ArrayList<>();
		for (Disjunct d : disjunctSet) {
			d.simpify();
			if (d.solve() == LogicValue.False) {
				toDrop.add(d);
			}
		}
		for (Disjunct d : toDrop) {
			disjunctSet.remove(d);
		}
	}
	
	public void setAsTrue() {
		disjunctSet.clear();
		NullAssumption assumption = new NullAssumption(NullConstant.v(), NullConstant.v(), true, 0);
		assumption.logicValue = LogicValue.True;
		Disjunct dummyTrue = new Disjunct();
		dummyTrue.logicValue = LogicValue.True;
		dummyTrue.add(assumption);
		disjunctSet.add(dummyTrue);
		this.logicValue = LogicValue.True;
	}
	
	public void setAsFalse() {
		disjunctSet.clear();
		this.logicValue = LogicValue.False;
	}
	
	public void setAsUnkown() {
		disjunctSet.clear();
		this.logicValue = LogicValue.Unkown;
	}
	
	public LogicValue logicValue = LogicValue.Uninitialized;
	
	private HashSet<Disjunct> disjunctSet;
	
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
}
