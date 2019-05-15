package core;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Objects;
import java.util.Set;

import adt.BkReachbleLineNum;
import adt.Disjunct;
import adt.Disjunction;
import adt.InOutPair_BKR;
import soot.Unit;
import soot.Value;

/**
 * Each statement analyzed is associated with a pair of dataflow facts,
 *  which represents the IN and OUT sets, respectively, as in standard dataflow analysis.
 */
class BkReachbleLineNumMapping {
	public BkReachbleLineNumMapping() {
		mapping = new HashMap<>();
	}
	
	public BkReachbleLineNum getIN(Unit u) {
		if (mapping.containsKey(u) == false) {
			mapping.put(u, new InOutPair_BKR());
		}
		return mapping.get(u).IN;
	}
	
	public BkReachbleLineNum getOUT(Unit u) {
		if (mapping.containsKey(u) == false) {
			mapping.put(u, new InOutPair_BKR());
		}
		return mapping.get(u).OUT;
	}
	
	public void setIN(Unit u, BkReachbleLineNum IN) {
		if (mapping.containsKey(u) == false) {
			mapping.put(u, new InOutPair_BKR());
		}
		mapping.get(u).IN = IN;
	}
	
	public void setOUT(Unit u, BkReachbleLineNum OUT) {
		if (mapping.containsKey(u) == false) {
			mapping.put(u, new InOutPair_BKR());
		}
		mapping.get(u).OUT = OUT;
	}
	
	public Set<Unit> keySet() {
		return mapping.keySet();
	}
	
	public void clear() {
		mapping.clear();
	}
	
	@Override
	public String toString() {
		StringBuffer res = new StringBuffer();
		for (Unit u : keySet()) {
			BkReachbleLineNum IN  = getIN(u);
			BkReachbleLineNum OUT = getOUT(u);
			res.append("Stmt:  ").append(u).append(" @ln: ").append(u.getJavaSourceStartLineNumber()).append("\n");
			res.append("IN  :  ").append(IN).append("\n");
			res.append("OUT :  ").append(OUT).append("\n\n");
		}
		return res.toString();
	}
	
	public HashMap<Unit, InOutPair_BKR> mapping;
}


