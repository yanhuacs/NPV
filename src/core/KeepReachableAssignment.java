package core;

import adt.AccessPath;
import adt.Constraint;
import adt.DataflowFact;
import adt.Disjunct;
import soot.Body;
import soot.Unit;
import soot.Value;
import soot.jimple.AssignStmt;
import soot.toolkits.graph.UnitGraph;
import soot.toolkits.scalar.SimpleLocalDefs;

public class KeepReachableAssignment extends TransferFunc {
	
	public DataflowFact transfer(Unit u, DataflowFact IN, DataflowFact oldOUT) {
		DataflowFact newOUT = new DataflowFact();
		newOUT.addAll(oldOUT);
		
		if (u instanceof AssignStmt) {
			Value lv = ((AssignStmt) u).getLeftOp();
			Value rv = ((AssignStmt) u).getRightOp();
			//Constraint c = new Constraint(lv, rv, true, u.getJavaSourceStartLineNumber());
			AccessPath leftAP = new AccessPath();
			AccessPath rightAP = new AccessPath();
			AccessPath.buildAccessPath(lv, u, du, leftAP);
			AccessPath.buildAccessPath(rv, u, du, rightAP);
			Constraint c = new Constraint(leftAP, rightAP, true, u.getJavaSourceStartColumnNumber());
			if (IN.isEmpty()) {
				Disjunct d = new Disjunct(c);
				newOUT.add(d);
			}
			else {
				for (Disjunct in : IN.getDisjunctSet()) {
					Disjunct d = new Disjunct(in);
					d.add(c);
					newOUT.add(d);
				}
			}
		}
		else {
			for (Disjunct in : IN.getDisjunctSet()) {
				Disjunct d = new Disjunct(in);
				newOUT.add(d);
			}
		}
		
		return newOUT;
	}
	
	public void init(Body body, UnitGraph cfg, SimpleLocalDefs du) {
		this.body = body;
		this.cfg = cfg;
		this.du = du; //local def-use chain
	}
	
	public void init(SimpleLocalDefs du) {
		this.du = du; //local def-use chain
	}
	
	Body body;
	UnitGraph cfg;
	SimpleLocalDefs du; //local def-use chain
	
}