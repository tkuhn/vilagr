package ch.tkuhn.vilagr;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class Vilagr {

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

	public static void main(String[] args) throws IOException {
		if (args.length != 1) {
			throw new IllegalArgumentException("Exactly one argument expected: properties file");
		}
		Vilagr vilagr = new Vilagr(new File(args[0]));
		vilagr.run();
	}

	public Vilagr(Properties properties, File dir) {
		loadDefaultProperties();
		this.properties.putAll(properties);
		this.dir = dir;
	}

	public Vilagr(File propertiesFile) throws IOException {
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

	public void run() {
		String engineId = getProperty("engine");
		if ("gephi".equals(engineId)) {
			GephiEngine engine = new GephiEngine(properties, dir);
			engine.run();
		} else {
			throw new RuntimeException("Unknown engine: " + engineId);
		}
	}

	private String getProperty(String key) {
		return properties.getProperty(key).toString();
	}

}
