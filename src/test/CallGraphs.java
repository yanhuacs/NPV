package test;

public class CallGraphs
{
	public static void main(String[] args) {
		doStuff();
	}
	
	public static void doStuff() {
		A a = new A();
		a.foo();
		b++;
		a.ham();
	}
	
	static int b;
}

class A
{
	public void foo() {
		bar();
	}
	
	public void bar() {
		x = 10;
		ham();
	}
	
	public void ham() {
		x--;
		System.out.println("xx");
	}
	
	int x;
}