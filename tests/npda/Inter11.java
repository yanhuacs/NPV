package npda;

import java.util.Random;

public class Inter11 {
	public static void main(String[] args) {
		Inter11_A a = new Inter11_A();
		Inter11_C y;
		if (Math.random() < 0.5) {
			y = a.b.c; //safe
			System.console().printf("%d", y.d); //safe
		}
	}
}

class Inter11_A {
	Inter11_B b;
	Inter11_A() {
		b = new Inter11_B();
	}
}

class Inter11_B {
	Inter11_C c;
	Inter11_B() {
		c = new Inter11_C();
	}
}

class Inter11_C {
	int d;
	Inter11_C() {
	}
}
