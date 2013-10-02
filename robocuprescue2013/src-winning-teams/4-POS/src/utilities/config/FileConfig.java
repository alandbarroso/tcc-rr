package utilities.config;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.TreeMap;

public class FileConfig {
	private String fileAddress = "";
	private Map<String, Block> blocks = new TreeMap<String, Block>();

	public FileConfig(File file) {
		fileAddress = file.getPath();
		BufferedReader br = null;
		try {
			br = new BufferedReader(new InputStreamReader(new FileInputStream(
					file)));
			Block lastBlock = null;
			while (true) {
				String line = br.readLine();
				if (line == null)
					break;
				StringTokenizer st = new StringTokenizer(line);
				if (!st.hasMoreTokens())
					continue;

				String str = st.nextToken();
				if (str.charAt(0) == '#')
					continue;
				else if (str.charAt(0) == '[') {
					str = str.replaceAll("\\[", "");
					lastBlock = new Block();
					blocks.put(str.replaceAll("\\]", ""), lastBlock);
				} else {
					ArrayList<String> values = new ArrayList<String>();
					if (lastBlock == null)
						System.err.println("Error in parsing file: " + fileAddress);
					lastBlock.values.put(str, values);
					while (st.hasMoreTokens()) {
						str = st.nextToken();
						if (!str.equals("="))
							values.add(str);
					}
				}
			}
		} catch (FileNotFoundException e) {
			System.err.println("File: " + fileAddress + " not found");
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			try {
				br.close();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	public Block get(String blockName) {
		if (blocks.containsKey(blockName))
			return blocks.get(blockName);
		return null;
	}

	public Map<String, Block> getBlocks() {
		return blocks;
	}

	public String getFileAddress() {
		return fileAddress;
	}
}
