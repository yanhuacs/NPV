package npda;

public class SSA {
    Str s;
    Str test1() {
	    s = null;
	    s = new Str();
	    return s;
    }

    Str test2() {
    	s = new Str();
	    s = null;
	    return s;
    }
    Str test3() {
	    s = new Str();
	    if (s.length() == 0)
	    	s = null;
	    return s;
    }

    public static void main(String argv[]) {
		SSA o = new SSA();
	 	Str s = o.test1();
	 	System.console().printf("%d", s.length());
	 	s = o.test2();
	 	System.console().printf("%d", s.length());
	 	s = o.test3();
	 	System.console().printf("%d", s.length());
    }
}

class Str {
	public int length() {
		if (Math.random()> 0.5)
			return 100;
		return 200;
	}
}
