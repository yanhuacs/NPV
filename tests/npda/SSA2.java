package npda;

class A_SSA2 {
	Str_SSA2 f = null;
}
class B_SSA2 {
	Str_SSA2 g = new Str_SSA2();
}

class Str_SSA2 {
	int length() {
		if (Math.random()> 0.5)
			return 100;
		return 200;
	}
}

class SSA2 {
    void test1(A_SSA2 a, B_SSA2 b) {
    }
    void test2(A_SSA2 a, B_SSA2 b) {
	    a.f = new Str_SSA2();
	    b.g = null;
    }

    public static void main(String argv[]) {
	    A_SSA2 a = new A_SSA2();
	    B_SSA2 b = new B_SSA2();
		SSA2 o = new SSA2();
	 	o.test1(a, b);
	 	System.console().printf("%d", a.f.length());
	 	System.console().printf("%d", b.g.length());
	 	o.test2(a, b);
	 	System.console().printf("%d", a.f.length());
	 	System.console().printf("%d", b.g.length());
    }
}
