package mylib.res.ja;

import java.io.FileInputStream;
import java.io.ObjectInputStream;
import java.util.HashSet;

public class JaHypernym {
	
	private static HashSet<String> dic = null;
	private static boolean initialized = initialize();
	
	private static boolean initialize() {
		boolean ret = false;
		try {
			String path = JaHypernym.class.getClassLoader().getResource("data/ja/JaHypernym/dic.obj").getPath();
			ObjectInputStream ois = new ObjectInputStream(new FileInputStream(path));
			dic = (HashSet<String>)ois.readObject();
			ois.close();
			ret = true;
		} catch(Exception e) {
			System.err.println("cannot read dic.obj.");
		}
		return ret;
	}
	
	public static boolean lookup(String w1, String w2) {
		
		return dic.contains(w1 + "\t" + w2) || dic.contains(w1 + "する\t" + w2) || dic.contains(w1 + "\t" + w2 + "する") || dic.contains(w1 + "する\t" + w2 + "する");
	}
}
