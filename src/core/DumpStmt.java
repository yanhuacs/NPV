package core;

import adt.DataflowFact;
import soot.Unit;
import soot.jimple.AssignStmt;
import soot.jimple.IdentityStmt;

public class DumpStmt extends TransferFunc {
    
	public DataflowFact transfer(Unit u, DataflowFact IN, DataflowFact oldOUT) {
		System.out.println("Stmt: " + u);
		System.out.println("Type: " + u.getClass());
		if (u instanceof AssignStmt) {
			System.out.println("LTY :" + ((AssignStmt) u).getLeftOp().getClass());
			System.out.println("RTY :" + ((AssignStmt) u).getRightOp().getClass());
		}
		if (u instanceof IdentityStmt) {
			System.out.println("LTY :" + ((IdentityStmt) u).getLeftOp().getClass());
			System.out.println("RTY :" + ((IdentityStmt) u).getRightOp().getClass());
		}
		System.out.println();
		return oldOUT;
	}
}