package npda;

import java.util.Random;

public class Ptr7 {
	public static void main(String[] args) {
		doStuff();
	}
	
	public static void doStuff() {
		A_Ptr7 a = new A_Ptr7();
		A_Ptr7 b = new A_Ptr7();
		b.p = new B_Ptr7();
		b.p.f = new Ptr7();
		B_Ptr7 c = b.p;
		if (Math.random() < 0.5) {
			b.p.f.x++; //safe
			c.f = null;
			System.console().printf("%d", b.p.f.x); //bug
		}
	}
	
	int x;
}

class A_Ptr7 {
	B_Ptr7 p;
}

class B_Ptr7 {
	Ptr7 f;
}