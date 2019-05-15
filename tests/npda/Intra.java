package npda;

public class Intra {
	public static void main(String[] args) {
		doStuff();
	}
	
	public static void doStuff() {
		A a = new A();
		a.x = 100; //safe
		for (int k = 0; k < 5; ++k) {
			a.foo();
		}
		if (a.x < 10) //safe
			a = null;
		
		if (a == null) {
			if (a != null)
				System.console().printf("11");
			else
				System.console().printf("22");
			System.console().printf("%d", a.x); //bug
		}
	}
}

class A
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