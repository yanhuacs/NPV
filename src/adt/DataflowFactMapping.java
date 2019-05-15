package adt;

import java.util.HashMap;
import java.util.Set;

import soot.Unit;

/**
 * Each statement analyzed is associated with a pair of dataflow facts,
 *  which represents the IN and OUT sets, respectively, as in standard dataflow analysis.
 */
public class DataflowFactMapping {
	public DataflowFactMapping() {
		mapping = new HashMap<>();
	}
	
	public DataflowFact getIN(Unit u) {
		if (mapping.containsKey(u) == false) {
			mapping.put(u, new InOutPair());
		}
		return mapping.get(u).IN;
	}
	
	public DataflowFact getOUT(Unit u) {
		if (mapping.containsKey(u) == false) {
			mapping.put(u, new InOutPair());
		}
		return mapping.get(u).OUT;
	}
	
	public void setIN(Unit u, DataflowFact IN) {
		if (mapping.containsKey(u) == false) {
			mapping.put(u, new InOutPair());
		}
		mapping.get(u).IN = IN;
	}
	
	public void setOUT(Unit u, DataflowFact OUT) {
		if (mapping.containsKey(u) == false) {
			mapping.put(u, new InOutPair());
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
		for (int i = 0; i < 1000; ++i) {
			for (Unit u : keySet()) {
				DataflowFact IN  = getIN(u);
				DataflowFact OUT = getOUT(u);
				if (u == null) continue;
				if (u.getJavaSourceStartLineNumber() != i) continue;
				
				/*//for debugging
				if (u.getJavaSourceStartLineNumber() == 167 || u.getJavaSourceStartLineNumber() == 166) {
					IN.solve();
					OUT.solve();
					res.append("Stmt:  ").append(u).append(" @ln: ").append(u.getJavaSourceStartLineNumber()).append("\n");
					res.append("IN  :  ").append(IN).append("\n");
					res.append("OUT :  ").append(OUT).append("\n\n");
				}
				//if (true) continue;
*/				
				if (IN.equals(OUT)) continue;
				IN.solve();
				OUT.solve();
				res.append("Stmt:  ").append(u).append(" @ln: ").append(u.getJavaSourceStartLineNumber()).append("\n");
				res.append("IN  :  ").append(IN).append("\n");
				res.append("OUT :  ").append(OUT).append("\n\n");
			}
		}
		return res.toString();
	}
	
	public HashMap<Unit, InOutPair> mapping;
}