package utilities.config;

import java.io.File;
import java.util.ArrayList;
import java.util.Map;
import java.util.TreeMap;

public class Config {
	private Map<String, FileConfig> fileConfigs = new TreeMap<String, FileConfig>();

	public Config(String mainFolderPath) {
		File mainFolder = new File(mainFolderPath);
		for (File file : mainFolder.listFiles())
			if (file.isFile() && file.getName().endsWith(".conf"))
				fileConfigs.put(file.getName().replaceAll(".conf", ""),
						new FileConfig(file));
	}

	public FileConfig get(String fileName) {
		if (fileConfigs.containsKey(fileName))
			return fileConfigs.get(fileName);
		return null;
	}

	public Map<String, FileConfig> getFileConfigs() {
		return fileConfigs;
	}

	public ArrayList<String> getArray(String fileName, String blockName,
			String key) {
		FileConfig fileConfig = get(fileName);
		if (fileConfig != null) {
			Block block = fileConfig.get(blockName);
			if (block != null)
				return block.get(key);
		}
		return null;
	}

	public String get(String fileName, String blockName, String key) {
		return getArray(fileName, blockName, key).get(0);
	}

	public void set(String fileName, String blockName, String key, String value) {
		String f = get(fileName, blockName, key);
		if (f == null)
			System.err.println("Error in set for Config");
		fileConfigs.get(fileName).get(blockName).get(key).set(0, value);
	}
}
