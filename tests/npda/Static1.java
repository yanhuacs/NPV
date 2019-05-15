package npda;

import java.util.Random;

public class Static1 {
	public static void main(String[] args) {
		doStuff();
	}
	
	public static void doStuff() {
		x = new Integer(10);
		//Random rand = new Random();
		//int r = rand.nextInt(100);
		if (Math.random() < 0.5) {
			x++; //safe
			System.console().printf("%d", x); //safe
			if (x > 10) { //safe
				x = null;
			}
			if (Math.random() < 0.5) {
				System.console().printf("%d", x); //safe
			}
		}
	}
	
	static Integer x;
}
