package npda;

import java.util.Random;

public class Inter10 {
	public static void main(String[] args) {
		Inter10_A a = new Inter10_A();
		Inter10_B y;
		if (Math.random() < 0.5) {
			y = a.b; //safe
			System.console().printf("%d", y.c); //safe
		}
	}
}

class Inter10_A {
	Inter10_B b;
	Inter10_A() {
		b = new Inter10_B();
	}
}

class Inter10_B {
	int c;
	Inter10_B() {
	}
}
