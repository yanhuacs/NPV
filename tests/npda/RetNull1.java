package npda;

import java.util.Random;

public class RetNull1 {
	public static void main(String[] args) {
		RetNull1_A a = new RetNull1_A();
		a.getB().f = 100; //safe
		System.console().printf("%d", a.b.f); //safe
		
		a = maybeNull(a);
		System.console().printf("%d", a.b.f); //bug
	}
	
	public static RetNull1_A maybeNull(RetNull1_A x) {
		if (Math.random() < 0.5) 
			return x;
		return null;
	}
}

class RetNull1_A {
	RetNull1_B b;
	RetNull1_A() {
		b = new RetNull1_B();
	}
	RetNull1_B getB() {
		return b;
	}
}

class RetNull1_B {
	int f;
}
