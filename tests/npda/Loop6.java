package npda;

public class Loop6 {
	public static void main(String[] args) {
		doStuff();
	}
	
	public static void doStuff() {
		boolean flag = false;
		B_Loop6 a = new B_Loop6();
		for (int k = 0; k < 5; ++k) {
			foo(a);
		}
		if (a.x < 10) {
			foo(a);
			flag = true;
		}
		System.console().printf("%d", a.x); //safe
		System.console().printf("%d", flag);
	}
	
	public static void foo(B_Loop6 b) {
		b.x++; //safe
		System.console().printf("%d", b.x); //safe
	}
}

class B_Loop6
{	
	int x;
}