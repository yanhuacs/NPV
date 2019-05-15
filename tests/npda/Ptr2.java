package npda;

import java.util.Random;

public class Ptr2 {
	public static void main(String[] args) {
		doStuff();
	}
	
	public static void doStuff() {
		Ptr2 o1 = new Ptr2();
		Ptr2 o2 = o1; //Alias
		Ptr2 o3 = o2; //Alias
		Ptr2 o4 = o3; //Alias
		Ptr2 o5 = o4; //Alias
		//Random rand = new Random();
		//int r = rand.nextInt(100);
		if (Math.random() < 0.5) {
			o5.x++; //safe
			o5 = null;
			System.console().printf("%d", o5.x); //bug
		}
	}
	
	int x;
}
