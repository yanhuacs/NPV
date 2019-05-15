package adt;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Objects;
import java.util.Set;

import javax.swing.text.StyledEditorKit.BoldAction;

import solver.Solver;
import soot.SootFieldRef;
import soot.Unit;
import soot.Value;
import soot.jimple.AssignStmt;
import soot.jimple.InstanceFieldRef;
import soot.jimple.NullConstant;

/**
 * Each dataflow fact is a standard disjunction as in mathematical logic.
 *  DataflowFact is implemented as a wrapper of Disjunction.
 *  @see Disjunction
 */
public class DataflowFact extends Disjunction {
	
	public DataflowFact clone() {
		DataflowFact clone = new DataflowFact();
		for (Disjunct d : this.getDisjunctSet()) {
			Disjunct d_clone = d.clone();
			clone.add(d_clone);
		}
		clone.logicValue = logicValue;
		clone.setAge(this.getAge());
		return clone;
	}
	public DataflowFact() {
		super();
	}
	
	public DataflowFact(Disjunct d) {
		super(d);
	}
	
	public boolean isWeakerThan(Disjunction other) {
		return super.isWeakerThan(other);
	}
	
	public boolean isStrictWeakerThan(Disjunction other) {
		return this.isWeakerThan(other) && !other.isWeakerThan(this);
	}

	@Override
	public boolean equals(Object obj) {
		return super.equals(obj);
	}
	
	@Override
	public int hashCode() {
		return super.hashCode();
	}
}
