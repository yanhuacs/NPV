package npda;

public class Recur4 {
	public static void main(String[] args) {
		Recur4_A a = new Recur4_A();
		a.getB().f = 400; //safe
		a.b = null;
		rec(a);
	}
	
	public static void rec(Recur4_A a) {
		System.console().printf("%d", a.b.f); //bug
		Recur4_A aa = new Recur4_A();
		aa.getB().f = 400; //safe
		rec(aa);
	}
}

class Recur4_A {
	Recur4_B b;
	Recur4_A() {
		b = new Recur4_B();
	}
	Recur4_B getB() {
		return b;
	}
}

class Recur4_B {
	int f;
}
