package test;

import java.util.Random;

public class AccessPath {
	public static void main(String[] args) {
		AP_A a = new AP_A();
		AP_A[] aa = new AP_A[10];
		aa[0] = new AP_A();
		AP_E y;
		if (test())
			y = a.b.c.d.e;
		else if (test()){
			aa[2] = new AP_A();
			y = aa[1].b.c.dd.e;
		}
		else {
			y = null;
		}
		foo(y);
	}
	
	public static void foo(AP_E x) {
		x.f = 100;
	}
	
	public static boolean test() {
		Random rand = new Random();
		return rand.nextInt(100) > 50;
	}
}

class AP_A {
	AP_B b;
	AP_A() {
		b = new AP_B();
	}
}

class AP_B {
	AP_C c;
	AP_B() {
		c = new AP_C();
	}
}

class AP_C {
	AP_D d;
	AP_D dd;
	AP_C() {
		d = new AP_D();
		dd = new AP_D(); 
	}
}

class AP_D {
	AP_E e;
	AP_D() {
		e = new AP_E();
	}
}

class AP_E {
	int f;
}