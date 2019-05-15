package adt;

/**
 * Instead of traditional 2-value logic (True/False), 
 * we adopt 3-value logic to include the Maybe-Bug situation.
 */
public enum IsSafe {
		Safe,
		Bug,
		Maybe;
	}
