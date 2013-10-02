package utilities.config;

import java.util.ArrayList;
import java.util.Map;
import java.util.TreeMap;

public class Block {
	Map<String, ArrayList<String>> values = new TreeMap<String, ArrayList<String>>();

	public ArrayList<String> get(String key) {
		if (values.containsKey(key))
			return values.get(key);
		// Debugger.error(key + " not found in block");
		return null;
	}

	public Map<String, ArrayList<String>> getValues() {
		return values;
	}
}
