package core;

import adt.BkReachbleLineNum;
import adt.SinglePath;
import soot.Unit;

public class KeepBkReachableLineNum {
	
	public BkReachbleLineNum transfer(Unit u, BkReachbleLineNum IN, BkReachbleLineNum oldOUT) {
		BkReachbleLineNum newOUT = new BkReachbleLineNum();
		newOUT.pathSet.addAll(oldOUT.pathSet);
		
		Integer ln = u.getJavaSourceStartLineNumber();
		for (SinglePath in : IN.pathSet) {
			SinglePath sp = new SinglePath(in);
			sp.add(ln);
			newOUT.add(sp);
		}
		
		return newOUT;
	}
}
