package ch.tkuhn.vilagr;

import java.awt.Color;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
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
	private Map<String,Color> typeColorMap;
	private Map<String,Color> attColorMap;

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
		return !"no".equals(get("do-layout"));
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

	public String getTypeColumn() {
		return get("type-column");
	}

	public boolean doPartition() {
		return "yes".equals(get("do-partition"));
	}

	public Color getTypeColor(String type) {
		if (typeColorMap == null) {
			initColorMaps();
		}
		return typeColorMap.get(type);
	}

	public Color getAttributeColor(String attribute) {
		if (attColorMap == null) {
			initColorMaps();
		}
		return attColorMap.get(attribute);
	}

	public String getAttributePattern() {
		String p = "";
		for (String s : get("node-colors").split(",")) {
			if (s.isEmpty() || !s.startsWith("@")) continue;
			String f = s.replaceFirst("^(.*)#......$", "$1");
			if (!p.isEmpty()) p += "|";
			p += f.substring(1);
		}
		if (p.isEmpty()) return null;
		return p;
	}

	private void initColorMaps() {
		typeColorMap = new HashMap<String,Color>();
		attColorMap = new HashMap<String,Color>();
		for (String s : get("node-colors").split(",")) {
			if (s.isEmpty()) continue;
			String colorString = s.replaceFirst("^.*(#......)$", "$1");
			Color c = Color.decode(colorString);
			c = new Color(c.getRed(), c.getGreen(), c.getBlue(), (int) (getNodeOpacity() * 255));
			String f = s.replaceFirst("^(.*)#......$", "$1");
			if (f.startsWith("@")) {
				System.err.println("Attribute color: " + f.substring(1) + " " + colorString);
				attColorMap.put(f.substring(1), c);
			} else {
				System.err.println("Type color: " + f + " " + colorString);
				typeColorMap.put(f, c);
			}
		}
	}

}
