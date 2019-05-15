package npda;

import java.util.Random;

public class If3 {
	public static void main(String[] args) {
		doStuff();
	}
	
	public static void doStuff() {
		If3 o = new If3(), o2 = null;
		//Random rand = new Random();
		//int r = rand.nextInt(100);
		if (Math.random() < 0.5) {
			o2 = new If3();
		}
		if (Math.random() < 0.5 && o2 == null) {
			o = null;
		}
		if (Math.random() < 0.5 && o2 != null) {
			o.x++; //safe
			System.console().printf("%d", o.x); //safe
			if (Math.random() < 0.5) { //safe
				o = null;
			}
			if (Math.random() < 0.5) {
				System.console().printf("%d", o.x); //bug
			}
		}
	}
	
	int x;
}
