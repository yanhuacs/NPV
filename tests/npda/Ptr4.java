package npda;

import java.util.Random;

public class Ptr4 {
	public static void main(String[] args) {
		doStuff();
	}
	
	public static void doStuff() {
		Ptr4 o1 = new Ptr4();
		Ptr4 o2 = o1; //Alias
		//Random rand = new Random();
		//int r = rand.nextInt(100);
		if (Math.random() < 0.5) {
			o2.x++; //safe
			System.console().printf("%d", o2.x); //safe
			if (Math.random() < 0.5 && o1.x > 10) { //safe
				o2 = null;
			}
			if (Math.random() < 0.5) {
				System.console().printf("%d", o2.x); //bug
			}
		}
	}
	
	int x;
}
