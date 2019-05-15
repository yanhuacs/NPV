package adt;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Objects;


/**
 * Each disjunction as standard in mathematical logic,
 *  i.e., in the form of "D1 or D2 or ... or Dn",
 *  where Di is a standard disjunct as in mathematical logic.
 *  @see Disjunct
 */
class MultiplePath {
	public MultiplePath() {
		pathSet = new HashSet<>();
	}
	
	public MultiplePath(SinglePath sp) {
		this();
		pathSet.add(sp);
	}
	
	public void add(SinglePath sp) {
		pathSet.add(sp);
	}
	
	public void addAll(MultiplePath mp) {
		pathSet.addAll(mp.pathSet);
	}
	
	public boolean isEmpty() {
		return pathSet.isEmpty();
	}
	
	public HashSet<SinglePath> pathSet;
	
	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof MultiplePath)) {
			return false;
		}
		MultiplePath mp = (MultiplePath) obj;
		return Objects.equals(this.pathSet, mp.pathSet);
	}
	
	@Override
	public int hashCode() {
		return Objects.hash(pathSet);
	}
	
	@Override
	public String toString() {
		StringBuffer res = new StringBuffer();
		Iterator<SinglePath> I = pathSet.iterator();
		if (I.hasNext()) {
			SinglePath sp = I.next();
			res.append(sp);
		}
		while (I.hasNext()) {
			res.append("\n  || ");
			SinglePath sp = I.next();
			res.append(sp);
		}
		return res.toString();
	}
}

