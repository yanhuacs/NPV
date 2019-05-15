package npda;

public class Inter2 {
	public static void main(String[] args) {
		Inter2_A a = new Inter2_A();
		a.getB().f = 100; //bug
		System.console().printf("%d", a.b.f); //bug
	}
}

class Inter2_A {
	Inter2_B b;
	Inter2_A() {
		//b = new Inter2_B();
	}
	Inter2_B getB() {
		return b;
	}
}

class Inter2_B {
	int f;
}
