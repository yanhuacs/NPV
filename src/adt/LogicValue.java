package adt;

/**
 * Instead of traditional 2-value logic (True/False), 
 * we adopt 4-value logic for Disjunctions, Disjuncts and Constraints 
 * to include the Unkown and Uninit situations.
 */
public enum LogicValue {
	True,
	False,
	Unkown,
	Uninitialized;
}
