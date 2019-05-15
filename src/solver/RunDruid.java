package solver;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Set;

import driver.Main;
import pta.PTA;
import soot.Body;
import soot.Scene;
import soot.SootClass;
import soot.SootMethod;
import soot.Unit;
import soot.Value;
import soot.jimple.AssignStmt;
import soot.jimple.IfStmt;
import soot.jimple.NullConstant;
import soot.jimple.toolkits.callgraph.CallGraph;
import soot.toolkits.graph.UnitGraph;

public class RunDruid {
	
	public static void execute(String args[]) {
		Main.main(args);
		Helper.getInstance().setDruidAnalysisFlag();
	}
	
	public static PTA getPTA() {
		return Main.pta;
	}
	
	public static CallGraph getCallGraph() {
		return Main.pta.getCgb().getCICallGraph();
	}
}
