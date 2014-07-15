package ch.tkuhn.vilagr;

import java.awt.Color;
import java.io.File;
import java.io.FileNotFoundException;
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
import org.gephi.statistics.plugin.Modularity;
import org.openide.util.Lookup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GephiEngine implements VilagrEngine {

	private Logger logger = LoggerFactory.getLogger(this.getClass());

	private VParams params;
	private GraphModel gm;

	public GephiEngine(Properties properties, File dir) {
		params = new VParams(properties, dir);
	}

	public GephiEngine(VParams params) {
		this.params = params;
	}

	@Override
	@SuppressWarnings("rawtypes")
	public void run() {
		logger.info("Initializing Gephi engine...");
		ProjectController pc = Lookup.getDefault().lookup(ProjectController.class);
		pc.newProject();
		Workspace ws = pc.getCurrentWorkspace();
		ImportController imp = Lookup.getDefault().lookup(ImportController.class);
		Container c = null;
		File inputFile = params.getInputFile();
		logger.info("Importing from file: " + inputFile);
		try {
			c = imp.importFile(inputFile);
		} catch (FileNotFoundException ex) {
			logger.error("File not found: " + inputFile, ex);
		}
		if ("no".equals(params.get("allow-auto-node"))) {
			c.setAllowAutoNode(false);
		} else if ("yes".equals(params.get("allow-auto-node"))) {
			c.setAllowAutoNode(true);
		}
		logger.info("Loading graph...");
		imp.process(c, new DefaultProcessor(), ws);
		gm = Lookup.getDefault().lookup(GraphController.class).getModel();

		String filterProp = params.get("filter");
		if (filterProp != null) {
			logger.info("Filtering...");
			String filterCol = filterProp.split("#")[0];
			FilterController fc = Lookup.getDefault().lookup(FilterController.class);
			AttributeColumn ac = getAttributeColumn(filterCol, AttributeType.STRING);
			AttributeEqualBuilder.EqualStringFilter filter = new AttributeEqualBuilder.EqualStringFilter(ac);
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
		props.putValue(PreviewProperty.EDGE_COLOR, new EdgeColor(params.getEdgeColor()));
		props.putValue(PreviewProperty.EDGE_OPACITY, params.getEdgeOpacity() * 100);
		props.putValue(PreviewProperty.NODE_OPACITY, params.getNodeOpacity() * 100);

		// Normalize edge thickness by node size:
		float edgeThickness = params.getEdgeThickness() * (getNodeSize() / 10.0f);
		props.putValue(PreviewProperty.EDGE_THICKNESS, edgeThickness);

		if (params.doLayout()) {
			logger.info("Calculate layout...");
			OpenOrdLayoutBuilder b = new OpenOrdLayoutBuilder();
			OpenOrdLayout layout = (OpenOrdLayout) b.buildLayout();
			layout.resetPropertiesValues();
			layout.setRandSeed(params.getRandomSeed());
			layout.setLiquidStage(params.getInt("liquid-stage"));
			layout.setExpansionStage(params.getInt("expansion-stage"));
			layout.setCooldownStage(params.getInt("cooldown-stage"));
			layout.setCrunchStage(params.getInt("crunch-stage"));
			layout.setSimmerStage(params.getInt("simmer-stage"));
			layout.setGraphModel(gm);
			layout.initAlgo();
			while (layout.canAlgo()) {
				layout.goAlgo();
			}
			layout.endAlgo();
		} else {
			logger.info("Skip layout");
		}

		String typeColumn = params.getTypeColumn();

		if (params.doPartition()) {
			logger.info("Calculate partition...");
			Modularity modularity = new Modularity();
			modularity.execute(gm, getAttributeModel());
			typeColumn = Modularity.MODULARITY_CLASS;
		} else {
			logger.info("Skip partition");
		}

		AttributeColumn col = getAttributeColumn(typeColumn, AttributeType.STRING);
		PartitionController partitionController = Lookup.getDefault().lookup(PartitionController.class);
		Partition p = partitionController.buildPartition(col, gm.getGraph());
		NodeColorTransformer transform = new NodeColorTransformer();
		Color[] defaultColors = new Color[] {
			Color.BLUE, Color.RED, Color.GREEN, Color.YELLOW, Color.PINK, Color.MAGENTA, Color.ORANGE, Color.CYAN
		};

		int i = 0;
		logger.info("Number of partitions: " + p.getPartsCount());
		if (p.getPartsCount() < 100) {
			logger.info("Color codes:");
			for (Part part : p.getParts()) {
				String v;
				if (part.getValue() == null) {
					v = "";
				} else {
					v = part.getValue().toString();
				}
				Color color = params.getTypeColor(v);
				if (color == null) {
					color = defaultColors[i];
				}
				transform.getMap().put(part.getValue(), color);
				logger.info(String.format("#%06X", (0xFFFFFF & color.getRGB())) + " " + v);
				i = (i + 1) % defaultColors.length;
			}
		}
		partitionController.transform(p, transform);

		ExportController ec = Lookup.getDefault().lookup(ExportController.class);
		String outputName = params.getOutputFileName();

		logger.info("Writing output...");
		for (String s : params.getOutputFormats()) {
			if (s.isEmpty()) continue;
			logger.info("Write file: " + s);
			if (s.equals("png")) {
				int size = params.getOutputSize();
				PNGExporter pngexp = (PNGExporter) ec.getExporter("png");
				pngexp.setHeight(size);
				pngexp.setWidth(size);
				pngexp.setWorkspace(ws);
				try {
					ec.exportFile(new File(params.getDir(), outputName + ".png"), pngexp);
				} catch (Exception ex) {
					ex.printStackTrace();
				}
			} else {
				try {
					ec.exportFile(new File(params.getDir(), outputName + "." + s));
				} catch (Exception ex) {
					ex.printStackTrace();
				}
			}
		}
		logger.info("Finished running Gephi engine");
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

	private AttributeModel getAttributeModel() {
		return Lookup.getDefault().lookup(AttributeController.class).getModel();
	}

	private AttributeColumn getAttributeColumn(String name, AttributeType type) {
		AttributeTable nodeTable = getAttributeModel().getNodeTable();
		AttributeColumn col = nodeTable.getColumn(name);
		if (col == null) {
			col = nodeTable.addColumn(name, type);
		}
		return col;
	}

}
