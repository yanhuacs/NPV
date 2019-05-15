package npda;

class Throw {
    Str_throw f = null;

    void test1() {
	try {
		test2();
	} catch (Exception e) {
		System.exit(1);
	}
    }

    void test2() throws Exception {
	    throw(new Exception());
    }

    public static void main(String argv[]) {
		Throw o = new Throw();
	
		o.test1();
	
		System.console().printf("%d", o.f.length());
    }
}

class Str_throw {
	int length() {
		if (Math.random() < 0.5)
			return 100;
		else return 200;
	}
}