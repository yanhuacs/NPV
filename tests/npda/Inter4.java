package npda;

import java.util.Random;

public class Inter4 {
	public static void main(String[] args) {
		Inter4_A a = new Inter4_A();
		Inter4_E y;
		if (Math.random() < 0.5) {
			y = a.b.c.d.e; //bug
			System.console().printf("%d", y.f); //bug, but may not be reported because there is not null assignment
		}
		/*else 
			y = null;
		foo(y);*/
	}
	
/*	public static void foo(Inter4_E x) {
		x.f = 100;
	}*/
}

class Inter4_A {
	Inter4_B b;
	Inter4_A() {
		//b = new Inter4_B();
	}
}

class Inter4_B {
	Inter4_C c;
	Inter4_B() {
		c = new Inter4_C();
	}
}

class Inter4_C {
	Inter4_D d;
	Inter4_D dd;
	Inter4_C() {
		d = new Inter4_D();
		dd = new Inter4_D(); 
	}
}

class Inter4_D {
	Inter4_E e;
	Inter4_D() {
		e = new Inter4_E();
	}
}

class Inter4_E {
	int f;
}