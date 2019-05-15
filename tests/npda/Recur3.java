package npda;

public class Recur3 {
	public static void main(String[] args) {
		Recur3_A a = new Recur3_A();
		a.getB().f = 300; //bug
		rec(a);
	}
	
	public static void rec(Recur3_A a) {
		System.console().printf("%d", a.b.f); //bug
		Recur3_A aa = new Recur3_A();
		aa.getB().f = 300; //bug
		rec(aa);
	}
}

class Recur3_A {
	Recur3_B b;
	Recur3_A() {
		//b = new Recur3_B();
	}
	Recur3_B getB() {
		return b;
	}
}

class Recur3_B {
	int f;
}
