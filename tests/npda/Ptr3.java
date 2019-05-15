package npda;

import java.util.Random;

public class Ptr3 {
	public static void main(String[] args) {
		doStuff();
	}
	
	public static void doStuff() {
		Ptr3 o1 = null;
		Ptr3 o2 = o1; //Alias
		Ptr3 o3 = o2; //Alias
		Ptr3 o4 = o3; //Alias
		Ptr3 o5 = o4; //Alias
		//Random rand = new Random();
		//int r = rand.nextInt(100);
		if (Math.random() < 0.5) {
			o5.x++; //bug
			o5 = new Ptr3();
			System.console().printf("%d", o5.x); //safe
		}
	}
	
	int x;
}
