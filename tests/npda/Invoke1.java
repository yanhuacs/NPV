package npda;

public class Invoke1 {
	
	public static void main(String[] args) {
		Invoke1 a = null;
		a.foo();
	}
	
	void foo () {
		System.console().printf("%d", 100); //safe
	}
}
