package solver;

import java.util.HashMap;
import java.util.List;
import java.util.Objects;

import adt.Disjunction;
import soot.SootMethod;
import soot.Value;
import soot.jimple.InvokeExpr;
import soot.jimple.VirtualInvokeExpr;

public class Summary {
	private static Summary instance = null;
	
	private Summary() {
		summary = new HashMap<>();
	}
	
	public static Summary getInstance() {
		if (instance == null)
			instance = new Summary();
		return instance;
	}
	
	public boolean hasSummary(SootMethod m, Value base, List<Value> args, Disjunction IN) {
		Triple specificCall = new Triple(m, base, args);
		if (summary.containsKey(specificCall)) {
			HashMap<Disjunction, Disjunction> summary_call = summary.get(specificCall);
			if (summary_call.containsKey(IN))
				return true;
		}
		return false;
	}
	
	public Disjunction getSummary(SootMethod m, Value base, List<Value> args, Disjunction IN) {
		Triple specificCall = new Triple(m, base, args);
		if (hasSummary(m, base, args, IN) == false)
			return null;
		return summary.get(specificCall).get(IN);
	}
	
	public void addSummary(SootMethod m, Value base, List<Value> args, Disjunction IN, Disjunction OUT) {
		Triple specificCall = new Triple(m, base, args);
		if (summary.containsKey(specificCall) == false) {
			HashMap<Disjunction, Disjunction> summary_call = new HashMap<>();
			summary.put(specificCall, summary_call);
		}
		summary.get(specificCall).put(IN, OUT);
	}
	
	private static HashMap<Triple, HashMap<Disjunction, Disjunction>> summary;
}

class Triple {
	public Triple(SootMethod m, Value base, List<Value> args) {
		this.m = m;
		this.base = base;
		this.args = args;
	}
	
	SootMethod m;
	Value base;
	List<Value> args;
	
	@Override
	public boolean equals(Object other) {
		if (! (other instanceof Triple)) 
			return false;
		Triple triple = (Triple) other;
		return this.m == triple.m
			&& this.base == triple.base
			&& this.args != null
			&& this.args.equals(triple.args);
	}
	
	@Override
	public int hashCode() {
		return Objects.hash(m, base, args);
	}
}