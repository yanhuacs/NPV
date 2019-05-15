package npda;

import java.util.Random;

public class RetNull3 {
	public static void main(String[] args) {
		RetNull3_A a = new RetNull3_A();
		a.getB().f = 300; //safe
		System.console().printf("%d", a.b.f); //safe
		
		System.console().printf("%d", maybeNull(a).b.f); //bug
		
	}
	
	public static RetNull3_A maybeNull(RetNull3_A x) {
		if (Math.random() < 0.5) 
			return maybeNull_sub(x);
		return null;
	}
	
	public static RetNull3_A maybeNull_sub(RetNull3_A y) {
		if (Math.random() < 0.5) 
			return maybeNull_sub_sub(y);
		return null;
	}
	
	public static RetNull3_A maybeNull_sub_sub(RetNull3_A z) {
		if (Math.random() < 0.5) 
			return z;
		return null;
	}
}

class RetNull3_A {
	RetNull3_B b;
	RetNull3_A() {
		b = new RetNull3_B();
	}
	RetNull3_B getB() {
		return b;
	}
}

class RetNull3_B {
	int f;
}
