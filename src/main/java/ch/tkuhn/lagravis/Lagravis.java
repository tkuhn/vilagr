package ch.tkuhn.lagravis;

import java.awt.Color;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

import org.gephi.data.attributes.api.AttributeColumn;
import org.gephi.data.attributes.api.AttributeController;
import org.gephi.data.attributes.api.AttributeModel;
import org.gephi.data.attributes.api.AttributeTable;
import org.gephi.data.attributes.api.AttributeType;
import org.gephi.graph.api.GraphController;
import org.gephi.graph.api.GraphModel;
import org.gephi.io.exporter.api.ExportController;
import org.gephi.io.importer.api.Container;
import org.gephi.io.importer.api.ImportController;
import org.gephi.io.processor.plugin.DefaultProcessor;
import org.gephi.layout.plugin.openord.OpenOrdLayoutBuilder;
import org.gephi.layout.spi.Layout;
import org.gephi.partition.api.Partition;
import org.gephi.partition.api.PartitionController;
import org.gephi.partition.plugin.NodeColorTransformer;
import org.gephi.preview.api.PreviewController;
import org.gephi.preview.api.PreviewModel;
import org.gephi.preview.api.PreviewProperty;
import org.gephi.preview.types.EdgeColor;
import org.gephi.project.api.ProjectController;
import org.gephi.project.api.Workspace;
import org.openide.util.Lookup;

public class Lagravis {

	public static void main(String[] args) {
		File inputFile = new File(args[0]);
		String typeCol = "type";
		if (args.length > 1) typeCol = args[1];

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
		imp.process(c, new DefaultProcessor(), ws);
		GraphModel gm = Lookup.getDefault().lookup(GraphController.class).getModel();

		PreviewModel model = Lookup.getDefault().lookup(PreviewController.class).getModel();
		model.getProperties().putValue(PreviewProperty.EDGE_COLOR, new EdgeColor(Color.GRAY));
		model.getProperties().putValue(PreviewProperty.EDGE_CURVED, false);
		model.getProperties().putValue(PreviewProperty.EDGE_OPACITY, 2);
		model.getProperties().putValue(PreviewProperty.ARROW_SIZE, 0);
		model.getProperties().putValue(PreviewProperty.EDGE_THICKNESS, 1);
		model.getProperties().putValue(PreviewProperty.NODE_BORDER_WIDTH, 0);
		model.getProperties().putValue(PreviewProperty.NODE_OPACITY, 10);

		AttributeModel attributeModelLocal = Lookup.getDefault().lookup(AttributeController.class).getModel();
		AttributeTable nodeTable = attributeModelLocal.getNodeTable();
		AttributeColumn col;
		col = nodeTable.getColumn(typeCol);
		if (col == null) {
			col = nodeTable.addColumn(typeCol, AttributeType.STRING);
		}
		PartitionController partitionController = Lookup.getDefault().lookup(PartitionController.class);
		@SuppressWarnings("rawtypes")
		Partition p = partitionController.buildPartition(col, gm.getGraph());
		NodeColorTransformer nodeColorTransformer = new NodeColorTransformer();
		nodeColorTransformer.randomizeColors(p);
		partitionController.transform(p, nodeColorTransformer);

		OpenOrdLayoutBuilder b = new OpenOrdLayoutBuilder();
		Layout layout = b.buildLayout();
		layout.setGraphModel(gm);
		layout.resetPropertiesValues();
		layout.initAlgo();
		while (layout.canAlgo()) {
			layout.goAlgo();
		}
		layout.endAlgo();
		ExportController ec = Lookup.getDefault().lookup(ExportController.class);
		String outputName = inputFile.getName().replaceFirst("[.][^.]+$", "");
		if (outputName.isEmpty()) outputName = "out";
		String outputFileName;
		File parent = inputFile.getAbsoluteFile().getParentFile();

		// Export as PDF:
		outputFileName = outputName + ".pdf";
		try {
			ec.exportFile(new File(parent, outputFileName));
		} catch (IOException ex) {
			ex.printStackTrace();
		}

		// Export as PNG:
		outputFileName = outputName + ".png";
		try {
			ec.exportFile(new File(parent, outputFileName));
		} catch (IOException ex) {
			ex.printStackTrace();
		}
	}

}
