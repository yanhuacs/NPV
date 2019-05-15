package npda;

import java.util.Random;

public class Ptr5 {
	public static void main(String[] args) {
		doStuff();
	}
	
	public static void doStuff() {
		A_Ptr5 a = new A_Ptr5();
		a.f = new Ptr5();
		//Random rand = new Random();
		//int r = rand.nextInt(100);
		if (Math.random() < 0.5) {
			a.f.x++; //safe
			a.f = null;
			System.console().printf("%d", a.f.x); //bug
		}
	}
	
	int x;
}

class A_Ptr5 {
	Ptr5 f;
}