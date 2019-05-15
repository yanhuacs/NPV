package npda;

class ShortCircuitEval {
    void test() {
		Str_sce s = new Str_sce();
		if (Math.random() < 0.5)
			s = null;
	  	if (s != null && s.length() >= 0)
	  		System.console().printf("shouldn't be executed");
	 	else
	 		System.console().printf("should be executed");
    }

    public static void main(String argv[]) {
	ShortCircuitEval o = new ShortCircuitEval();
	o.test();
	
    }
}

class Str_sce {
	int length() {
		if (Math.random() < 0.5)
			return -100;
		return 100;
	}
}
