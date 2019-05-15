package adt;

public class OutOfBudget {
	public static void setFalse() {
		flag = false;
	}
	
	public static void setTrue() {
		flag = true;
	}
	
	public static boolean isOutOfBudget() {
		return flag;
	}
	
	public static boolean flag = false; 
}
