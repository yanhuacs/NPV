package npda;

import java.util.Random;

public class Ptr6 {
	public static void main(String[] args) {
		doStuff();
	}
	
	public static void doStuff() {
		A_Ptr6 a = new A_Ptr6();
		A_Ptr6 b = new A_Ptr6();
		a.f = new Ptr6();
		//Random rand = new Random();
		//int r = rand.nextInt(100);
		A_Ptr6 c = null;
		if (Math.random() < 0.5)
			c = a;
		else
			c = b;
		if (Math.random() < 0.5) {
			a.f.x++; //safe
			b.f.x++; //bug
			c.f = null;
			System.console().printf("%d", a.f.x); //bug
		}
	}
	
	int x;
}

class A_Ptr6 {
	Ptr6 f;
	/*public A_Ptr6() {
		// TODO Auto-generated constructor stub
	}*/
}