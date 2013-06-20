package ch.tkuhn.lagravis;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

import org.gephi.graph.api.GraphController;
import org.gephi.graph.api.GraphModel;
import org.gephi.io.exporter.api.ExportController;
import org.gephi.io.importer.api.Container;
import org.gephi.io.importer.api.ImportController;
import org.gephi.io.processor.plugin.DefaultProcessor;
import org.gephi.layout.plugin.openord.OpenOrdLayoutBuilder;
import org.gephi.layout.spi.Layout;
import org.gephi.project.api.ProjectController;
import org.gephi.project.api.Workspace;
import org.openide.util.Lookup;

public class Lagravis {

	public static void main(String[] args) {
		File inputFile = new File(args[0]);
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
		String outputFileName = inputFile.getName().replaceFirst("[.][^.]+$", "");
		if (outputFileName.isEmpty()) outputFileName = "out";
		outputFileName = outputFileName + ".pdf";
		try {
			ec.exportFile(new File(inputFile.getAbsoluteFile().getParentFile(), outputFileName));
		} catch (IOException ex) {
			ex.printStackTrace();
		}
	}

}
