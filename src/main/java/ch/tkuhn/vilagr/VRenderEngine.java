package ch.tkuhn.vilagr;

import java.awt.Color;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.imageio.ImageIO;

import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.supercsv.io.CsvListReader;
import org.supercsv.prefs.CsvPreference;

public class VRenderEngine implements VilagrEngine {

	private Logger logger = LoggerFactory.getLogger(this.getClass());

	private VParams params;
	private GraphDrawer graphDrawer;
	private Map<String,Float> pointsX;
	private Map<String,Float> pointsY;
	private Map<String,String> types;
	private Map<String,String> attributes;
	private Map<String,Boolean> connected;

	public VRenderEngine(Properties properties, File dir) {
		params = new VParams(properties, dir);
		init();
	}

	public VRenderEngine(VParams params) {
		this.params = params;
		init();
	}

	private void init() {
		graphDrawer = new GraphDrawer(params.getOutputSize());
		graphDrawer.setTransformation(params.getFloat("offset"), params.getFloat("scale"), params.getBoolean("yaxis-bottomup"));
		graphDrawer.setEdgeAlpha(params.getEdgeOpacity());
		graphDrawer.setNodeSize(params.getInt("node-size"));
		pointsX = new HashMap<String,Float>();
		pointsY = new HashMap<String,Float>();
		types = new HashMap<String,String>();
		attributes = new HashMap<String,String>();
		if (params.getBoolean("ignore-isolates")) {
			connected = new HashMap<String,Boolean>();
		}
	}

	@Override
	public void run() {
		try {
			readNodes();
			drawEdges();
			drawNodes();
			writeImage();
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}

	private void readNodes() {
		String tc = params.getTypeColumn();
		String ap = params.getAttributePattern();
		logger.info("Type column: " + tc);
		logger.info("Attribute pattern: " + ap);
		logger.info("Reading nodes...");
		CoordIterator ci = new CoordIterator(params.getInputFile(), tc, ap,
				new CoordIterator.CoordHandler() {
			
			@Override
			public void handleCoord(String nodeId, String type, String atts, float x, float y) throws Exception {
				pointsX.put(nodeId, x);
				pointsY.put(nodeId, y);
				if (type != null) {
					types.put(nodeId, type.intern());
				}
				if (atts != null && !atts.isEmpty()) {
					attributes.put(nodeId, atts.intern());
				}
			}

		});
		ci.run();
	}

	private void drawEdges() {
		logger.info("Drawing edges...");
		GraphIterator ei = new GraphIterator(params.getInputFile(), new GraphIterator.GraphHandler() {

			@Override
			public void handleNode(String nodeId, Pair<Float, Float> coords, Map<String, String> attributes) throws Exception {
			}
	
			@Override
			public void handleEdge(String nodeId1, String nodeId2) throws Exception {
				if (!pointsX.containsKey(nodeId1) || !pointsX.containsKey(nodeId2)) return;
				float x1 = pointsX.get(nodeId1);
				float y1 = pointsY.get(nodeId1);
				float x2 = pointsX.get(nodeId2);
				float y2 = pointsY.get(nodeId2);
				graphDrawer.recordEdge(x1, y1, x2, y2);
				if (connected != null) {
					connected.put(nodeId1, true);
					connected.put(nodeId2, true);
				}
			}

		});
		ei.setNodeHandlingEnabled(false);
		ei.run();
		graphDrawer.finishEdgeDrawing();
	}

	private void drawNodes() {
		logger.info("Drawing nodes...");
		Color baseColor = new Color(95, 95, 95, (int) (params.getNodeOpacity() * 255));
		String[] atts = new String[] {};
		String attPattern = params.getAttributePattern();
		if (attPattern != null) {
			atts = attPattern.split("\\|");
		}
		for (String id : pointsX.keySet()) {
			if (connected != null && !connected.containsKey(id)) {
				// ignore isolates
				continue;
			}
			Color color = baseColor;
			if (attributes.containsKey(id)) {
				for (String a : atts) {
					if (("|" + attributes.get(id) + "|").contains("|" + a + "|")) {
						color = params.getAttributeColor(attributes.get(id));
						break;
					}
				}
			} else if (types.containsKey(id)) {
				color = params.getTypeColor(types.get(id));
			}
			graphDrawer.drawNode(pointsX.get(id), pointsY.get(id), color);
		}
	}

	private void writeImage() throws IOException {
		logger.info("Writing image...");
		for (String format : params.getOutputFormats()) {
			File outputFile = new File(params.getDir(), params.getOutputFileName() + "." + format);
			ImageIO.write(graphDrawer.getImage(), format, outputFile);
		}
	}


	// TODO merge this class into GraphIterator
	private static class CoordIterator {

		public static interface CoordHandler {

			public void handleCoord(String nodeId, String type, String attributes, float x, float y) throws Exception;

		}

		private static CsvPreference csvPref = CsvPreference.STANDARD_PREFERENCE;

		private File file;
		private CoordHandler handler;
		private String typeColumn;
		private String attNamePattern;

		public CoordIterator(File file, String typeColumn, String attNamePattern, CoordHandler handler) {
			this.file = file;
			this.typeColumn = typeColumn;
			this.attNamePattern = attNamePattern;
			this.handler = handler;
		}

		public void run() {
			try {
				if (file.getName().endsWith(".csv")) {
					processCsv();
				} else if (file.getName().endsWith(".gexf")) {
					processGexf();
				} else {
					throw new RuntimeException("Unsupported format: " + file);
				}
			} catch (IOException ex) {
				ex.printStackTrace();
			}
		}

		private void processCsv() throws IOException {
			BufferedReader r = new BufferedReader(new FileReader(file), 64*1024);
			CsvListReader csvReader = new CsvListReader(r, csvPref);
			List<String> line;
			while ((line = csvReader.read()) != null) {
				String nodeId = line.get(0);
				float x = Float.parseFloat(line.get(1));
				float y = Float.parseFloat(line.get(2));
				try {
					handler.handleCoord(nodeId, null, "", x, y);
				} catch (Exception ex) {
					ex.printStackTrace();
					break;
				}
			}
			csvReader.close();
		}


		private static final String idPattern = "^\\s*<node id=\"(.*?)\".*$";
		private static final String typePattern = "^\\s*<attvalue.*? for=\"<TYPE>\".*? value=\"(.*?)\".*$";
		private static final String attPattern = "^\\s*<attvalue.*? for=\"(<PATTERN>)\".*$";
		private static final String coordPattern = "^\\s*<viz:position x=\"(.*?)\" y=\"(.*?)\".*$";

		private void processGexf() throws IOException {
			BufferedReader reader = new BufferedReader(new FileReader(file), 64*1024);
			String spTypePattern = null;
			if (typeColumn != null) {
				spTypePattern = typePattern.replace("<TYPE>", typeColumn);
			}
			String spAttPattern = null;
			if (attNamePattern != null) {
				spAttPattern = attPattern.replace("<PATTERN>", attNamePattern);
			}
			String line;
			String type = null;
			String nodeId = null;
			String atts = "";
			while ((line = reader.readLine()) != null) {
				if (line.matches(idPattern)) {
					if (nodeId != null) {
						reader.close();
						throw new RuntimeException("No coordinates found for: " + nodeId);
					}
					nodeId = line.replaceFirst(idPattern, "$1");
				} else if (spTypePattern != null && line.matches(spTypePattern)) {
					type = line.replaceFirst(spTypePattern, "$1");
				} else if (spAttPattern != null && line.matches(spAttPattern)) {
					if (!atts.isEmpty()) atts += "|";
					atts += line.replaceFirst(spAttPattern, "$1");
				} else if (line.matches(coordPattern)) {
					if (nodeId == null) {
						reader.close();
						throw new RuntimeException("No ID found for coordinates: " + line);
					} else {
						float x = Float.parseFloat(line.replaceFirst(coordPattern, "$1"));
						float y = Float.parseFloat(line.replaceFirst(coordPattern, "$2"));
						try {
							handler.handleCoord(nodeId, type, atts, x, y);
						} catch (Exception ex) {
							ex.printStackTrace();
							break;
						}
						nodeId = null;
						type = null;
						atts = "";
					}
				}
			}
			if (nodeId != null) {
				reader.close();
				throw new RuntimeException("No coordinates found for: " + nodeId);
			}
			reader.close();
		}

	}

}
