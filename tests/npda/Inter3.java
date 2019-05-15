package npda;

import java.util.Random;

public class Inter3 {
	public static void main(String[] args) {
		Inter3_A a = new Inter3_A();
		Inter3_E y;
		if (Math.random() < 0.5) {
			y = a.b.c.d.e; //safe
			System.console().printf("%d", y.f); //safe
		}
		/*else 
			y = null;
		foo(y);*/
	}
	
/*	public static void foo(Inter3_E x) {
		x.f = 100;
	}*/
}

class Inter3_A {
	Inter3_B b;
	Inter3_A() {
		b = new Inter3_B();
	}
}

class Inter3_B {
	Inter3_C c;
	Inter3_B() {
		c = new Inter3_C();
	}
}

class Inter3_C {
	Inter3_D d;
	Inter3_D dd;
	Inter3_C() {
		d = new Inter3_D();
		dd = new Inter3_D(); 
	}
}

class Inter3_D {
	Inter3_E e;
	Inter3_D() {
		e = new Inter3_E();
	}
}

class Inter3_E {
	int f;
}