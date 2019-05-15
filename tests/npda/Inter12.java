package npda;
public class Inter12 {
	public static void main(String[] args) {
		Inter12 a = new Inter12();
		Inter12 aa = new Inter12();
		Inter12 aaa = new Inter12();
		a.b.c = 100; //safe
		//System.console().printf("%d", a.b.c); //safe
	}
	
	Inter12_B b;
	Inter12() {
		b = new Inter12_B();
	}
}


class Inter12_B {
	int c;
	Inter12_B() {
	}
}
