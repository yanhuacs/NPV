package npda;

import java.util.Random;

public class Inter8 {
	public static void main(String[] args) {
		Inter8_A a = new Inter8_A();
		a.b = new Inter8_B();
		a.b.c = new Inter8_C();
		a.b.c.d = new Inter8_D();
		//a.b.c.d.e = new Inter8_E();
		Inter8_E y;
		if (Math.random() < 0.5) {
			y = a.b.c.dd.e; //bug
			System.console().printf("%d", y.f); //bug, but may not be reported because there is not null assignment
		}
		else 
			y = null;
		foo(y);
	}
	
	public static void foo(Inter8_E x) {
		x.f = 100; //bug
	}
}

class Inter8_A {
	Inter8_B b;
	Inter8_A() {
		//b = new Inter8_B();
	}
}

class Inter8_B {
	Inter8_C c;
	Inter8_B() {
		//c = new Inter8_C();
	}
}

class Inter8_C {
	Inter8_D d;
	Inter8_D dd;
	Inter8_C() {
		//d = new Inter8_D();
		//dd = new Inter8_D(); 
	}
}

class Inter8_D {
	Inter8_E e;
	Inter8_D() {
		//e = new Inter8_E();
	}
}

class Inter8_E {
	int f;
}