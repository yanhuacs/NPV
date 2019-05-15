package solver;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.List;

public class BlackListSrc {
	public static List<String> getBlackList() {
		List<String> methodList = new ArrayList<>();
		String fileName = String.join(File.separator, ".", "blacklist.txt");
		try (BufferedReader reader = new BufferedReader(new FileReader(fileName))) {
			String temp = null;
			while((temp = reader.readLine()) != null) {
				if(temp.trim().startsWith("#"))
					continue;
				if(temp.trim().equals(""))
					continue;
				methodList.add(temp);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return methodList;
	}
	
	public static void main(String[] args) {
		for(String string : getBlackList())
			System.out.println(string);
	}
}
