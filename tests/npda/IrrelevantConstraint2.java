package npda;

import java.util.Random;

public class IrrelevantConstraint2 {
	
	public static void main(String[] args) {
		doStuff();
	}
	
	public static void doStuff() {
		IrrelevantConstraint2 o = new IrrelevantConstraint2();
		//Random rand = new Random();
		//int r = rand.nextInt(100);
		A_IrrelevantConstraint2 irl = new A_IrrelevantConstraint2();
		if (Math.random() < 0.5) {
			irl = new A_IrrelevantConstraint2();
		}
		if (Math.random() < 0.5) {
			o.x++; //safe
			System.console().printf("%d", o.x); //safe
			if (o.x > 10) { //safe
				if (irl != null)
					o = null;
			}
			if (Math.random() < 0.5) {
				System.console().printf("%d", o.x); //bug
			}
		}
	}
	
	int x;
	

}

class A_IrrelevantConstraint2 {
	
}