package npda;

public class Recur1 {
	public static void main(String[] args) {
		rec();
	}
	
	public static void rec() {
		Recur1_A a = new Recur1_A();
		a.getB().f = 100; //safe
		System.console().printf("%d", a.b.f); //safe		
		rec();
	}
}

class Recur1_A {
	Recur1_B b;
	Recur1_A() {
		b = new Recur1_B();
	}
	Recur1_B getB() {
		return b;
	}
}

class Recur1_B {
	int f;
}
