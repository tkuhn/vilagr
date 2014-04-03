package ch.tkuhn.vilagr;

import java.awt.Color;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import javax.imageio.ImageIO;

public class VRenderEngine implements VilagrEngine {

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
		log("Reading nodes...");
		CoordIterator ci = new CoordIterator(params.getInputFile(), params.getTypeColumn(), params.getAttributePattern(),
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
		log("Drawing edges...");
		EdgeIterator ei = new EdgeIterator(params.getInputFile(), new EdgeIterator.EdgeHandler() {

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
		ei.run();
		graphDrawer.finishEdgeDrawing();
	}

	private void drawNodes() {
		log("Drawing nodes...");
		Color baseColor = new Color(128, 128, 128, (int) (params.getNodeOpacity() * 255));
		String[] atts = params.getAttributePattern().split("\\|");
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
		log("Writing image...");
		for (String format : params.getOutputFormats()) {
			File outputFile = new File(params.getDir(), params.getOutputFileName() + "." + format);
			ImageIO.write(graphDrawer.getImage(), format, outputFile);
		}
	}

	private void log(String text) {
		System.err.println(text);
	}

}
