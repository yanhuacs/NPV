package npda;

import java.util.Random;

public class Loop4 {
	public static void main(String[] args) {
		doStuff();
	}
	
	public static void doStuff() {
		A_loop4 a = new A_loop4();
		//Random rand = new Random();
		//int r = rand.nextInt(100);
		if (Math.random() < 0.5)
			a = null;
		for (int i = 0; i < 5 && a != null; ++i) {  //This condition makes the deref safe
			for (int k = 0; k < 5 && a == null; ++k) { 
				if (a == null)
					System.console().printf("%d", a.x + 1); //safe
				else
					System.console().printf("%d", a.x + 2); //safe
				if (a != null)
					System.console().printf("%d", a.x + 3); //safe
			}
		}
	}
}

class A_loop4
{
	int x;
}