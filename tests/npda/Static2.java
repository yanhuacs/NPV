package npda;

import java.util.Random;

public class Static2 {
	public static void main(String[] args) {
		doStuff();
	}
	
	public static void doStuff() {
		a = new A_Static2();
		//Random rand = new Random();
		//int r = rand.nextInt(200);
		if (Math.random() < 0.5) {
			a.x++; //safe
			System.console().printf("%d", a.x); //safe
			if (a.x > 20) { //safe
				a = null;
			}
			if (Math.random() < 0.5) {
				System.console().printf("%d", a.x); //bug
			}
		}
	}
	
	static A_Static2 a;
}

class A_Static2 {
	int x;
}