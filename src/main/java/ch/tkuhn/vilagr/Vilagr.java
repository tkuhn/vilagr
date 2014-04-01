package ch.tkuhn.vilagr;

import java.awt.Color;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.gephi.data.attributes.api.AttributeColumn;
import org.gephi.data.attributes.api.AttributeController;
import org.gephi.data.attributes.api.AttributeModel;
import org.gephi.data.attributes.api.AttributeTable;
import org.gephi.data.attributes.api.AttributeType;
import org.gephi.filters.api.FilterController;
import org.gephi.filters.api.Query;
import org.gephi.filters.plugin.attribute.AttributeEqualBuilder;
import org.gephi.graph.api.GraphController;
import org.gephi.graph.api.GraphModel;
import org.gephi.graph.api.GraphView;
import org.gephi.graph.api.Node;
import org.gephi.graph.api.NodeIterator;
import org.gephi.io.exporter.api.ExportController;
import org.gephi.io.exporter.preview.PNGExporter;
import org.gephi.io.importer.api.Container;
import org.gephi.io.importer.api.ImportController;
import org.gephi.io.processor.plugin.DefaultProcessor;
import org.gephi.layout.plugin.openord.OpenOrdLayout;
import org.gephi.layout.plugin.openord.OpenOrdLayoutBuilder;
import org.gephi.partition.api.Part;
import org.gephi.partition.api.Partition;
import org.gephi.partition.api.PartitionController;
import org.gephi.partition.plugin.NodeColorTransformer;
import org.gephi.preview.api.PreviewController;
import org.gephi.preview.api.PreviewModel;
import org.gephi.preview.api.PreviewProperties;
import org.gephi.preview.api.PreviewProperty;
import org.gephi.preview.types.EdgeColor;
import org.gephi.project.api.ProjectController;
import org.gephi.project.api.Workspace;
import org.openide.util.Lookup;

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
	private GraphModel gm;

	public static void main(String[] args) throws IOException {
		if (args.length != 1) {
			throw new IllegalArgumentException("Exactly one argument expected: properties file");
		}
		Vilagr vilagr = new Vilagr(new File(args[0]));
		vilagr.run();
	}

	public Vilagr(Properties properties, File dir) {
		this.properties = properties;
		this.dir = dir;
	}

	public Vilagr(File propertiesFile) throws IOException {
		properties = new Properties();
		properties.load(new FileInputStream(propertiesFile));
		dir = propertiesFile.getParentFile();
	}

	public void run() {
		if ("gephi".equals(getMode())) {
			runGephi();
		} else {
			throw new RuntimeException("Unknown mode: " + getMode());
		}
	}

	@SuppressWarnings("rawtypes")
	public void runGephi() {
		File inputFile = new File(dir, getProperty("input-file"));

		ProjectController pc = Lookup.getDefault().lookup(ProjectController.class);
		pc.newProject();
		Workspace ws = pc.getCurrentWorkspace();
		ImportController imp = Lookup.getDefault().lookup(ImportController.class);
		Container c = null;
		try {
			c = imp.importFile(inputFile);
		} catch (FileNotFoundException ex) {
			ex.printStackTrace();
		}
		if ("no".equals(getProperty("allow-auto-node"))) {
			c.setAllowAutoNode(false);
		} else if ("yes".equals(getProperty("allow-auto-node"))) {
			c.setAllowAutoNode(true);
		}
		imp.process(c, new DefaultProcessor(), ws);
		gm = Lookup.getDefault().lookup(GraphController.class).getModel();

		String filterProp = getProperty("filter");
		if (filterProp != null) {
			String filterCol = filterProp.split("#")[0];
			FilterController fc = Lookup.getDefault().lookup(FilterController.class);
			AttributeColumn ac = getAttributeColumn(filterCol, AttributeType.STRING);
			AttributeEqualBuilder.EqualStringFilter filter =
					new AttributeEqualBuilder.EqualStringFilter(ac);
			filter.init(gm.getGraph());
			filter.setPattern(filterProp.split("#")[1]);
			filter.setUseRegex(true);
			Query query = fc.createQuery(filter);
			GraphView view = fc.filter(query);
			gm.setVisibleView(view);
		}

		PreviewModel model = Lookup.getDefault().lookup(PreviewController.class).getModel();
		PreviewProperties props = model.getProperties();
		props.putValue(PreviewProperty.EDGE_CURVED, false);
		props.putValue(PreviewProperty.ARROW_SIZE, 0);
		props.putValue(PreviewProperty.NODE_BORDER_WIDTH, 0);
		Color edgeColor = Color.decode(getProperty("edge-color"));
		props.putValue(PreviewProperty.EDGE_COLOR, new EdgeColor(edgeColor));
		props.putValue(PreviewProperty.EDGE_OPACITY, new Float(getProperty("edge-opacity")));
		props.putValue(PreviewProperty.NODE_OPACITY, new Float(getProperty("node-opacity")));

		// Normalize edge thickness by node size:
		float edgeThickness = new Float(getProperty("edge-thickness")) * (getNodeSize() / 10.0f);
		props.putValue(PreviewProperty.EDGE_THICKNESS, edgeThickness);

		if (!getProperty("do-layout").equals("no")) {
			OpenOrdLayoutBuilder b = new OpenOrdLayoutBuilder();
			OpenOrdLayout layout = (OpenOrdLayout) b.buildLayout();
			layout.resetPropertiesValues();
			layout.setRandSeed(new Long(getProperty("random-seed")));
			layout.setLiquidStage(new Integer(getProperty("liquid-stage")));
			layout.setExpansionStage(new Integer(getProperty("expansion-stage")));
			layout.setCooldownStage(new Integer(getProperty("cooldown-stage")));
			layout.setCrunchStage(new Integer(getProperty("crunch-stage")));
			layout.setSimmerStage(new Integer(getProperty("simmer-stage")));
			layout.setGraphModel(gm);
			layout.initAlgo();
			while (layout.canAlgo()) {
				layout.goAlgo();
			}
			layout.endAlgo();
		}

		AttributeColumn col = getAttributeColumn(getProperty("type-column"), AttributeType.STRING);
		PartitionController partitionController = Lookup.getDefault().lookup(PartitionController.class);
		Partition p = partitionController.buildPartition(col, gm.getGraph());
		NodeColorTransformer transform = new NodeColorTransformer();
		Color[] defaultColors = new Color[] {
			Color.BLUE, Color.RED, Color.GREEN, Color.YELLOW, Color.PINK, Color.MAGENTA, Color.ORANGE, Color.CYAN
		};
		Map<String,Color> colorMap = new HashMap<String,Color>();
		for (String s : getProperty("node-colors").split(",")) {
			if (s.isEmpty()) continue;
			Color color = Color.decode(s.replaceFirst("^.*(#......)$", "$1"));
			colorMap.put(s.replaceFirst("^(.*)#......$", "$1"), color);
		}

		int i = 0;
		System.out.println("Color codes:");
		for (Part part : p.getParts()) {
			String v;
			if (part.getValue() == null) {
				v = "";
			} else {
				v = part.getValue().toString();
			}
			Color color = defaultColors[i];
			if (colorMap.containsKey(v)) {
				color = colorMap.get(v);
			}
			transform.getMap().put(part.getValue(), color);
			System.out.println(String.format("#%06X", (0xFFFFFF & color.getRGB())) + " " + v);
			i = (i + 1) % defaultColors.length;
		}
		partitionController.transform(p, transform);

		ExportController ec = Lookup.getDefault().lookup(ExportController.class);
		String outputName = getProperty("output-file");
		if (outputName.isEmpty()) {
			outputName = inputFile.getName().replaceFirst("[.][^.]+$", "");
			if (outputName.isEmpty()) outputName = "out";
		}

		for (String s : getProperty("output-formats").split(",")) {
			if (s.isEmpty()) continue;
			if (s.equals("png")) {
				int size = 1000;
				if (getProperty("output-size") != null) {
					size = new Integer(getProperty("output-size"));
				}
				PNGExporter pngexp = (PNGExporter) ec.getExporter("png");
				pngexp.setHeight(size);
				pngexp.setWidth(size);
				pngexp.setWorkspace(ws);
				try {
					ec.exportFile(new File(dir, outputName + ".png"), pngexp);
				} catch (IOException ex) {
					ex.printStackTrace();
				}
			} else {
				try {
					ec.exportFile(new File(dir, outputName + "." + s));
				} catch (IOException ex) {
					ex.printStackTrace();
				}
			}
		}

	}

	private String getProperty(String key) {
		String value = properties.getProperty(key);
		if (value != null) return value;
		return defaultProperties.getProperty(key);
	}

	public String getMode() {
		return getProperty("mode");
	}

	private float getNodeSize() {
		NodeIterator iter = gm.getGraph().getNodes().iterator();
		while (iter.hasNext()) {
			Node n = iter.next();
			if (n == null) continue;
			return n.getNodeData().getSize();
		}
		return 10.0f;
	}

	private AttributeColumn getAttributeColumn(String name, AttributeType type) {
		AttributeModel attributeModelLocal = Lookup.getDefault().lookup(AttributeController.class).getModel();
		AttributeTable nodeTable = attributeModelLocal.getNodeTable();
		AttributeColumn col = nodeTable.getColumn(name);
		if (col == null) {
			col = nodeTable.addColumn(name, type);
		}
		return col;
	}

}
