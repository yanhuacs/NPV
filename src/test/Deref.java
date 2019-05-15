package test;

public class Deref {
	public static void main(String[] args) {
		doStuff();
	}
	
	public static void doStuff() {
		Integer i = new Integer(100);
		String s = i.toString();  //deref 1
		int len = s.length(); //deref 2
		System.out.println(len);
		AA a = new AA();
		a.x = 100; //deref 3
		a.foo(); //deref 4
		int y = a.x; //deref 5
		System.out.println(y);
		y = a.bar(a.bar(5)); //deref 6 //deref 7
		System.out.println(y);
	}
}

class AA
{
	public void foo() {
		x++;
		System.out.println("calling a method." + x);
	}
	
	public int bar(int n) {
		return x + n;
	}
	
	int x;
}