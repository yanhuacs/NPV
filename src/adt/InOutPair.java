package adt;

class InOutPair {	
	public DataflowFact IN;
	public DataflowFact OUT;
	
	public InOutPair() {
		IN  = new DataflowFact();
		OUT = new DataflowFact();
	}
}