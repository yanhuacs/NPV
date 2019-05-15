package npda;

import java.util.Random;

public class If1 {
	public static void main(String[] args) {
		doStuff();
	}
	
	public static void doStuff() {
		If1 o = new If1();
		//Random rand = new Random();
		//int r = rand.nextInt(100);
		if (Math.random() < 0.5) {
			o.x++; //safe
			System.console().printf("%d", o.x); //safe
			if (Math.random() < 0.5) {
				o = null;
			}
			if (Math.random() < 0.5) {
				System.console().printf("%d", o.x); //bug
			}
		}
	}
	
	int x;
}
