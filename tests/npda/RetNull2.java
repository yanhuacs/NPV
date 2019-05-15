package npda;

import java.util.Random;

public class RetNull2 {
	public static void main(String[] args) {
		RetNull2_A a = new RetNull2_A();
		a.getB().f = 200; //safe
		System.console().printf("%d", a.b.f); //safe
		
		System.console().printf("%d", maybeNull(a).b.f); //bug
		
	}
	
	public static RetNull2_A maybeNull(RetNull2_A x) {
		if (Math.random() < 0.5) 
			return x;
		return null;
	}
}

class RetNull2_A {
	RetNull2_B b;
	RetNull2_A() {
		b = new RetNull2_B();
	}
	RetNull2_B getB() {
		return b;
	}
}

class RetNull2_B {
	int f;
}
