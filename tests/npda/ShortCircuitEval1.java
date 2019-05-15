package npda;

class ShortCircuitEval1 {
    void test() {
		Str_sce1 s = new Str_sce1();
		if (Math.random() < 0.5)
			s = null;
	  	if (s == null || s.length() >= 0)
	  		System.console().printf("should be executed");
	 	else
	 		System.console().printf("shouldn't be executed");
    }

    public static void main(String argv[]) {
	ShortCircuitEval1 o = new ShortCircuitEval1();
	o.test();
	
    }
}

class Str_sce1 {
	int length() {
		if (Math.random() < 0.5)
			return -100;
		return 100;
	}
}