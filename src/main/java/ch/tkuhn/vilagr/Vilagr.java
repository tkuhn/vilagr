package ch.tkuhn.vilagr;

import java.io.File;
import java.io.IOException;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Vilagr {

	private Logger logger = LoggerFactory.getLogger(this.getClass());

	public static void main(String[] args) throws IOException {
		if (args.length != 1) {
			throw new IllegalArgumentException("Exactly one argument expected: properties file");
		}
		Vilagr vilagr = new Vilagr(new File(args[0]));
		vilagr.run();
	}

	private VParams params;

	public Vilagr(Properties properties, File dir) {
		params = new VParams(properties, dir);
	}

	public Vilagr(File propertiesFile) throws IOException {
		params = new VParams(propertiesFile);
	}

	public Vilagr(VParams params) {
		this.params = params;
	}

	public void run() {
		logger.info("*** Starting Vilagr ***");
		String engineId = params.get("engine").toLowerCase();
		VilagrEngine engine;
		if ("gephi".equals(engineId)) {
			logger.info("Using Gephi engine");
			engine = new GephiEngine(params);
		} else if ("vrender".equals(engineId)) {
			logger.info("Using VRender engine");
			engine = new VRenderEngine(params);
		} else {
			throw new RuntimeException("Unknown engine: " + engineId);
		}
		engine.run();
		logger.info("*** finished ***");
	}

}
