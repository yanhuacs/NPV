package npda;

import java.util.Random;

public class Inter5 {
	public static void main(String[] args) {
		Inter5_A a = new Inter5_A();
		Inter5_E y;
		if (Math.random() < 0.5) {
			y = a.b.c.d.e; //safe
			System.console().printf("%d", y.f); //safe
		}
		else 
			y = null;
		foo(y);
	}
	
	public static void foo(Inter5_E x) {
		x.f = 100; //bug
	}
}

class Inter5_A {
	Inter5_B b;
	Inter5_A() {
		b = new Inter5_B();
	}
}

class Inter5_B {
	Inter5_C c;
	Inter5_B() {
		c = new Inter5_C();
	}
}

class Inter5_C {
	Inter5_D d;
	Inter5_D dd;
	Inter5_C() {
		d = new Inter5_D();
		dd = new Inter5_D(); 
	}
}

class Inter5_D {
	Inter5_E e;
	Inter5_D() {
		e = new Inter5_E();
	}
}

class Inter5_E {
	int f;
}