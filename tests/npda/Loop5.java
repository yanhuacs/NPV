package npda;

import java.util.Random;

public class Loop5 {
	public static void main(String[] args) {
		doStuff();
	}
	
	public static void doStuff() {
		A_loop5 a = new A_loop5();
		//Random rand = new Random();
		//int r = rand.nextInt(100);
		if (Math.random() < 0.5)
			;//a = null;
		for (int i = 0; i < 5; ++i) {
			for (int k = 0; k < 5 && a == null; ++k) { 
				if (a == null)
					System.console().printf("%d", a.x + 1); //bug
				else
					System.console().printf("%d", a.x + 2); //safe
				if (a != null)
					System.console().printf("%d", a.x + 3); //safe
			}
			a = null; //This assignment makes the deref unsafe
		}
	}
}

class A_loop5
{
	int x;
}