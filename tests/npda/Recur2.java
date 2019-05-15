package npda;

public class Recur2 {
	public static void main(String[] args) {
		Recur2_A a = new Recur2_A();
		a.getB().f = 200; //safe
		rec(a);
	}
	
	public static void rec(Recur2_A a) {
		System.console().printf("%d", a.b.f); //safe
		Recur2_A aa = new Recur2_A();
		aa.getB().f = 200; //safe
		rec(aa);
	}
}

class Recur2_A {
	Recur2_B b;
	Recur2_A() {
		b = new Recur2_B();
	}
	Recur2_B getB() {
		return b;
	}
}

class Recur2_B {
	int f;
}
