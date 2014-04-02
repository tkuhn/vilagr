package ch.tkuhn.vilagr;

import java.awt.Color;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class VParams {

	private static Properties defaultProperties;

	static {
		defaultProperties = new Properties();
		InputStream in = Vilagr.class.getResourceAsStream("default.properties");
		try {
			defaultProperties.load(in);
		} catch (IOException ex) {
			ex.printStackTrace();
		}
	}

	private Properties properties = new Properties();
	private File dir;

	public VParams(Properties properties, File dir) {
		loadDefaultProperties();
		this.properties.putAll(properties);
		this.dir = dir;
	}

	public VParams(File propertiesFile) throws IOException {
		loadDefaultProperties();
		Properties specificProps = new Properties();
		specificProps.load(new FileInputStream(propertiesFile));
		properties.putAll(specificProps);
		dir = propertiesFile.getParentFile();
	}

	private void loadDefaultProperties() {
		properties = new Properties();
		properties.putAll(defaultProperties);
	}

	public String get(String key) {
		if (properties.containsKey(key)) {
			return properties.getProperty(key).toString();
		}
		return null;
	}

	public int getInt(String key) {
		if (properties.containsKey(key)) {
			return Integer.parseInt(properties.getProperty(key).toString());
		}
		return 0;
	}

	public float getFloat(String key) {
		if (properties.containsKey(key)) {
			return Float.parseFloat(properties.getProperty(key).toString());
		}
		return 0.0f;
	}

	public boolean getBoolean(String key) {
		if (properties.containsKey(key)) {
			return Boolean.parseBoolean(properties.getProperty(key).toString());
		}
		return false;
	}

	public File getDir() {
		return dir;
	}

	public File getInputFile() {
		return new File(dir, get("input-file"));
	}

	public Color getEdgeColor() {
		return Color.decode(get("edge-color"));
	}

	public float getEdgeOpacity() {
		return Float.parseFloat(get("edge-opacity"));
	}

	public float getEdgeThickness() {
		return Float.parseFloat(get("edge-thickness"));
	}

	public float getNodeOpacity() {
		return Float.parseFloat(get("node-opacity"));
	}

	public boolean doLayout() {
		return !get("do-layout").equals("no");
	}

	public long getRandomSeed() {
		return Long.parseLong(get("random-seed"));
	}

	public String[] getOutputFormats() {
		return get("output-formats").split(",");
	}

	public int getOutputSize() {
		return getInt("output-size");
	}

	public String getOutputFileName() {
		String outputName = get("output-file");
		if (outputName.isEmpty()) {
			outputName = getInputFile().getName().replaceFirst("[.][^.]+$", "");
			if (outputName.isEmpty()) outputName = "out";
		}
		return outputName;
	}

}
