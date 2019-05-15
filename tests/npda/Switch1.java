package npda;

import java.util.Random;

public class Switch1 {
	public static void main(String[] args) {
		doStuff();
	}
	
	public static void doStuff() {
		Switch1 o = new Switch1();
		//Random rand = new Random();
		//int r = rand.nextInt(100);
		switch ((new Double(Math.random() * 10)).intValue()) {
		case 1:
			o.x++; //safe
			break;
			
		case 2:
			o.x++; //safe
			break;
			
		case 3:
			o.x++; //safe
			break;
			
		case 4:
			o.x++; //safe
			o = null; //causing subsequent null-pointer dereference
			break;
		case 5:
			o.x++; //safe
			break;
			
		default:
			break;
		}
		
		if (Math.random() < 0.5) {
			System.console().printf("%d", o.x); //bug
		}
	}
	
	int x;
}
