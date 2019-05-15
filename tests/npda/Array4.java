package npda;

import java.util.Random;

public class Array4 {
	public static void main(String[] args) {
		doStuff();
	}
	
	public static void doStuff() {
		A_Array4[] a = new A_Array4[10];
		a[4] = new A_Array4();
		a[3] = new A_Array4();
		for (int k = 0; k < 5; ++k) {
			a[3].foo(); //safe
		}
		//Random rand = new Random();
		//int r = rand.nextInt(100);
		if (Math.random() < 0.5)
			a[3] = null; //safe
		
		if (a[3] == null) { //safe
			if (a[4] != null) //safe
				System.console().printf("%d", a[3].x); //bug
			else
				System.console().printf("22");
			System.console().printf("%d", a[3].x); //bug
		}
	}
}

class A_Array4
{
	public void foo() {
		x++;
		System.console().printf("%d", x);
	}
	
	public int bar(int n) {
		return x + n;
	}
	
	int x;
}