package adt;


/**
 * Each dataflow fact is a standard disjunction as in mathematical logic.
 *  DataflowFact is implemented as a wrapper of Disjunction.
 *  @see Disjunction
 */
public class BkReachbleLineNum extends MultiplePath {
	public BkReachbleLineNum() {
		super();
	}
	
	public BkReachbleLineNum(SinglePath sp) {
		super(sp);
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