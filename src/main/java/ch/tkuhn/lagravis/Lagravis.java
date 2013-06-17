package ch.tkuhn.lagravis;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

import org.gephi.graph.api.GraphController;
import org.gephi.graph.api.GraphModel;
import org.gephi.io.exporter.api.ExportController;
import org.gephi.io.generator.plugin.RandomGraph;
import org.gephi.io.importer.api.Container;
import org.gephi.io.importer.api.ContainerFactory;
import org.gephi.io.importer.api.ImportController;
import org.gephi.io.processor.plugin.DefaultProcessor;
import org.gephi.layout.plugin.AutoLayout;
import org.gephi.layout.plugin.openord.OpenOrdLayoutBuilder;
import org.gephi.project.api.ProjectController;
import org.gephi.project.api.Workspace;
import org.openide.util.Lookup;

public class Lagravis {

	public static void main(String[] args) {
		ProjectController pc = Lookup.getDefault().lookup(ProjectController.class);
		pc.newProject();
		Workspace ws = pc.getCurrentWorkspace();
		Container c = Lookup.getDefault().lookup(ContainerFactory.class).newContainer();
		RandomGraph randomGraph = new RandomGraph();
		randomGraph.setNumberOfNodes(10000);
		randomGraph.setWiringProbability(0.00025);
		randomGraph.generate(c.getLoader());
		ImportController imp = Lookup.getDefault().lookup(ImportController.class);
		imp.process(c, new DefaultProcessor(), ws);
		GraphModel gm = Lookup.getDefault().lookup(GraphController.class).getModel();
		AutoLayout layout = new AutoLayout(10, TimeUnit.SECONDS);
		layout.setGraphModel(gm);
		OpenOrdLayoutBuilder b = new OpenOrdLayoutBuilder();
		layout.addLayout(b.buildLayout(), 1.0f);
		layout.execute();
		ExportController ec = Lookup.getDefault().lookup(ExportController.class);
		try {
			ec.exportFile(new File("target/out.pdf"));
		} catch (IOException ex) {
			ex.printStackTrace();
		}
	}

}
