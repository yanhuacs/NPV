package npda;

import java.util.Random;

public class Static3 {
	public static void main(String[] args) {
		doStuff();
	}
	
	public static void doStuff() {
		x = new Integer[30];
		x[3] = new Integer(99);
		//Random rand = new Random();
		//int r = rand.nextInt(300);
		if (Math.random() < 0.5) {
			x[3]++; //safe
			System.console().printf("%d", x.length); //safe
			if (x[3] > 30) { //safe
				x = null;
			}
			if (Math.random() < 0.5) {
				System.console().printf("%d", x[3]); //bug
			}
		}
	}
	
	static Integer x[];
}
