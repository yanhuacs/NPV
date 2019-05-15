package npda;

import java.util.Random;

public class Loop2 {
	public static void main(String[] args) {
		doStuff();
	}
	
	public static void doStuff() {
		A_loop2 a = new A_loop2();
		//Random rand = new Random();
		//int r = rand.nextInt(100);
		if (Math.random() < 0.5)
			a = null;
		for (int k = 0; k < 5 && a != null; ++k) { //This condition makes the deref safe
			if (a == null)
				System.console().printf("%d", a.x + 1); //safe
			else
				System.console().printf("%d", a.x + 2); //safe
			if (a != null)
				System.console().printf("%d", a.x + 3); //safe
		}
	}
}

class A_loop2
{
	int x;
}