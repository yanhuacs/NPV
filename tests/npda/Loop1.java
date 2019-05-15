package npda;

import java.util.Random;

public class Loop1 {
	public static void main(String[] args) {
		doStuff();
	}
	
	public static void doStuff() {
		A_loop1 a = new A_loop1();
		//Random rand = new Random();
		//int r = rand.nextInt(100);
		if (Math.random() < 0.5)
			a = null;
		for (int k = 0; k < 5 && a != null; ++k) { //This condition makes the deref safe
			if (Math.random() < 0.5 && a == null)
				System.console().printf("%d", a.x + 1); //safe
		}
	}
}

class A_loop1
{
	int x;
}