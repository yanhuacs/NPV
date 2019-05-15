package npda;

import java.util.Random;

public class Inter7 {
	public static void main(String[] args) {
		Inter7_A a = new Inter7_A();
		a.b = new Inter7_B();
		a.b.c = new Inter7_C();
		a.b.c.d = new Inter7_D();
		//a.b.c.d.e = new Inter7_E();
		Inter7_E y;
		if (Math.random() < 0.5) {
			y = a.b.c.d.e; //safe
			System.console().printf("%d", y.f); //bug
		}
		else 
			y = null;
		foo(y);
	}
	
	public static void foo(Inter7_E x) {
		x.f = 100; //bug
	}
}

class Inter7_A {
	Inter7_B b;
	Inter7_A() {
		//b = new Inter7_B();
	}
}

class Inter7_B {
	Inter7_C c;
	Inter7_B() {
		//c = new Inter7_C();
	}
}

class Inter7_C {
	Inter7_D d;
	Inter7_D dd;
	Inter7_C() {
		//d = new Inter7_D();
		//dd = new Inter7_D(); 
	}
}

class Inter7_D {
	Inter7_E e;
	Inter7_D() {
		//e = new Inter7_E();
	}
}

class Inter7_E {
	int f;
}