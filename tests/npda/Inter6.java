package npda;

import java.util.Random;

public class Inter6 {
	public static void main(String[] args) {
		Inter6_A a = new Inter6_A();
		a.b = new Inter6_B();
		a.b.c = new Inter6_C();
		a.b.c.d = new Inter6_D();
		a.b.c.d.e = new Inter6_E();
		Inter6_E y;
		if (Math.random() < 0.5) {
			y = a.b.c.d.e; //safe
			System.console().printf("%d", y.f); //safe
		}
		else 
			y = null;
		foo(y);
	}
	
	public static void foo(Inter6_E x) {
		x.f = 100; //bug
	}
}

class Inter6_A {
	Inter6_B b;
	Inter6_A() {
		//b = new Inter6_B();
	}
}

class Inter6_B {
	Inter6_C c;
	Inter6_B() {
		//c = new Inter6_C();
	}
}

class Inter6_C {
	Inter6_D d;
	Inter6_D dd;
	Inter6_C() {
		//d = new Inter6_D();
		//dd = new Inter6_D(); 
	}
}

class Inter6_D {
	Inter6_E e;
	Inter6_D() {
		//e = new Inter6_E();
	}
}

class Inter6_E {
	int f;
}