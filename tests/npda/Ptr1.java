package npda;

import java.util.Random;

public class Ptr1 {
	public static void main(String[] args) {
		doStuff();
	}
	
	public static void doStuff() {
		Ptr1 o1 = new Ptr1();
		Ptr1 o2 = o1; //Alias
		//Random rand = new Random();
		//int r = rand.nextInt(100);
		if (Math.random() < 0.5) {
			o2.x++; //safe
			o2 = null;
			System.console().printf("%d", o2.x); //bug
		}
	}
	
	int x;
}
