package npda;

public class Inter1 {
	public static void main(String[] args) {
		Inter1_A a = new Inter1_A();
		a.getB().f = 100; //safe
		System.console().printf("%d", a.b.f); //safe
	}
}

class Inter1_A {
	Inter1_B b;
	Inter1_A() {
		b = new Inter1_B();
	}
	Inter1_B getB() {
		return b;
	}
}

class Inter1_B {
	int f;
}
