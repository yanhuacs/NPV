package adt;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Objects;


/**
 * Each single path a set of line numbers.
 */
public class SinglePath {
	public SinglePath() {
		lineNumSet = new HashSet<>();
	}
	
	public SinglePath(SinglePath sp) {
		this();
		this.lineNumSet.addAll(sp.lineNumSet);
	}
	
	public SinglePath(Integer ln) {
		this();
		lineNumSet.add(ln);
	}
	
	public void add(Integer ln) {
		lineNumSet.add(ln);
	}
	
	public boolean isEmpty() {
		return lineNumSet.isEmpty();
	}
	
	public HashSet<Integer> lineNumSet;
	
	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof SinglePath)) {
			return false;
		}
		SinglePath sp = (SinglePath) obj;
		return Objects.equals(this.lineNumSet, sp.lineNumSet);
	}
	
	@Override
	public int hashCode() {
		return Objects.hash(lineNumSet);
	}
	
	@Override
	public String toString() {
		StringBuffer res = new StringBuffer();
		Iterator<Integer> I = lineNumSet.iterator();
		if (I.hasNext()) {
			Integer i = I.next();
			res.append(i);
		}
		while (I.hasNext()) {
			res.append(" && ");
			Integer i = I.next();
			res.append(i);
		}
		return res.toString();
	}
}


