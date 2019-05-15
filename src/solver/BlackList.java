package solver;

import java.util.HashSet;

public class BlackList {
	
	static private BlackList instance = null;
	
	public static BlackList getInstance() {
		if (instance == null) {
			instance = new BlackList();
		}
		return instance;
	}
	
	private BlackList() {
		list = new HashSet<>();
		//list.add("StringIndexOutOfBoundsException");
		list.addAll(BlackListSrc.getBlackList());
	}
	
	public static boolean contains(String methodName) {
		return getInstance().list.contains(methodName);
	}
	
	private HashSet<String> list;
}
