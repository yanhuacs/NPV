package npda;

import java.util.Random;

public class Inter9 {
	public static void main(String[] args) {
		Inter9_A a = new Inter9_A();
		Inter9_D y;
		if (Math.random() < 0.5) {
			y = a.b.c.d; //safe
			System.console().printf("%d", y.e); //safe
		}
	}
}

class Inter9_A {
	Inter9_B b;
	Inter9_A() {
		b = new Inter9_B();
	}
}

class Inter9_B {
	Inter9_C c;
	Inter9_B() {
		c = new Inter9_C();
	}
}

class Inter9_C {
	Inter9_D d;
	Inter9_C() {
		d = new Inter9_D();
	}
}

class Inter9_D {
	int e;
	Inter9_D() {
	}
}
