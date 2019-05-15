package npda;

import java.util.Random;

public class If2 {
	public static void main(String[] args) {
		doStuff();
	}
	
	public static void doStuff() {
		If2 o = new If2();
		//Random rand = new Random();
		//int r = rand.nextInt(100);
		if (Math.random() < 0.5) {
			o.x++; //safe
			System.console().printf("%d", o.x); //safe
			if (Math.random() < 0.5 && o == null) { //safe
				o = null;
			}
			if (Math.random() < 0.5) {
				System.console().printf("%d", o.x); //safe
			}
		}
	}
	
	int x;
}
