package core;

import adt.Constraint;
import adt.DataflowFact;
import adt.Disjunct;
import soot.Unit;
import soot.Value;
import soot.jimple.AssignStmt;

public class KeepOnlyCurrentAssignment extends TransferFunc {
	
	public DataflowFact transfer(Unit u, DataflowFact IN, DataflowFact oldOUT) {
		DataflowFact newOUT = new DataflowFact();
		newOUT.addAll(oldOUT);
		
		if (u instanceof AssignStmt) {
			Value lv = ((AssignStmt) u).getLeftOp();
			Value rv = ((AssignStmt) u).getRightOp();
			Constraint c = new Constraint(lv, rv, true, u.getJavaSourceStartLineNumber());
			Disjunct d = new Disjunct(c);
			DataflowFact df = new DataflowFact(d);
			newOUT = df;
		}
		
		return newOUT;
	}
}
