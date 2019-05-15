package npda;

class X_inst { 
}
class A_inst extends X_inst {
	Str_inst f = null;
}

class B_inst extends X_inst {
	Str_inst f = new Str_inst();
}

class InstanceOf {
	X_inst test1() {
		int i = 1;	
	    if (Math.random() < 0.5)//if (i != 1)
			return new A_inst();
		else
			return new B_inst();
    }
    
    void test2(X_inst x) {
		if (x instanceof A_inst)
			return;
		System.console().printf("%d", ((B_inst)x).f.length());
    }
    
    public static void main(String argv[]) {
		InstanceOf o = new InstanceOf();
		X_inst x = o.test1();
		o.test2(x);
    }
}

class Str_inst {
	int length() {
		if (Math.random() < 0.5)
			return 100;
		else return 200;
	}
}