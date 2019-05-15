package npda;

public class LinkedList2 {
	public static void main(String[] args) {
		doStuff();
	}
	
	public static void doStuff() {
		Node_2 cur = new Node_2();
		for (int k = 0; k < 5; ++k) { 
			System.console().printf("%d", cur.val);
			cur.nxt = new Node_2();
			cur = cur.nxt; //safe
			cur.nxt = null; //safe
		}
/*		for (int k = 0; k < 5; ++k) { 
			System.console().printf("%d", cur.val); //safe
			cur = cur.nxt; //safe
		}*/
		System.console().printf("%d", cur.val); //safe
		cur = cur.nxt; //safe
	}
}

class Node_2 //Linked list data structure.
{
	int val;
	Node_2 nxt;
}