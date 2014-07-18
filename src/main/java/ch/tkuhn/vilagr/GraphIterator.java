package ch.tkuhn.vilagr;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.supercsv.io.CsvListReader;
import org.supercsv.prefs.CsvPreference;

/**
 * Iterates over the nodes and edges of a graph read from a CSV or GEXF file.
 *
 * @author Tobias Kuhn
 */
public class GraphIterator {

	public static interface GraphHandler {

		public void handleNode(String nodeId, Map<String,String> attributes) throws Exception;

		public void handleEdge(String nodeId1, String nodeId2) throws Exception;

	}

	private static CsvPreference csvPref = CsvPreference.STANDARD_PREFERENCE;

	public static void setCsvPreference(CsvPreference csvPreference) {
		csvPref = csvPreference;
	}

	private File file;
	private GraphHandler handler;
	private boolean nodeHandlingEnabled = true;
	private boolean edgeHandlingEnabled = true;

	public GraphIterator(File file, GraphHandler handler) {
		this.file = file;
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

	public void setNodeHandlingEnabled(boolean nodeHandlingEnabled) {
		this.nodeHandlingEnabled = nodeHandlingEnabled;
	}

	public void setEdgeHandlingEnabled(boolean edgeHandlingEnabled) {
		this.edgeHandlingEnabled = edgeHandlingEnabled;
	}

	private void processCsv() throws IOException {
		BufferedReader r = new BufferedReader(new FileReader(file), 64*1024);
		CsvListReader csvReader = new CsvListReader(r, csvPref);
		List<String> line;
		while ((line = csvReader.read()) != null) {
			String nodeId1 = line.get(0);
			String nodeId2 = line.get(1);
			try {
				handler.handleEdge(nodeId1, nodeId2);
			} catch (Exception ex) {
				ex.printStackTrace();
				break;
			}
		}
		csvReader.close();
	}


	private static final String nodeStartPattern = "^\\s*<node id=\"(.*?)\".*$";
	private static final String nodeAttPattern = "^\\s*<attvalue for=\"(.*?)\" value=\"(.*?)\".*$";
	private static final String nodeEndPattern = "^\\s*</node>.*$";
	private static final String edgePattern = "^\\s*<edge.*? source=\"(.*?)\".*? target=\"(.*?)\".*$";

	private void processGexf() throws IOException {
		BufferedReader reader = new BufferedReader(new FileReader(file), 64*1024);
		String nodeId = null;
		Map<String,String> atts = null;
		String line;
		while ((line = reader.readLine()) != null) {
			if (nodeHandlingEnabled && nodeId == null && line.matches(nodeStartPattern)) {
				nodeId = line.replaceFirst(nodeStartPattern, "$1");
				atts = new HashMap<String,String>();
			} else if (nodeHandlingEnabled && nodeId != null && line.matches(nodeAttPattern)) {
				String name = line.replaceFirst(nodeAttPattern, "$1");
				String value = line.replaceFirst(nodeAttPattern, "$2");
				atts.put(name, value);
			} else if (nodeHandlingEnabled && nodeId != null && line.matches(nodeEndPattern)) {
				try {
					handler.handleNode(nodeId, atts);
				} catch (Exception ex) {
					ex.printStackTrace();
					break;
				}
				nodeId = null;
			} else if (edgeHandlingEnabled && line.matches(edgePattern)) {
				String nodeId1 = line.replaceFirst(edgePattern, "$1");
				String nodeId2 = line.replaceFirst(edgePattern, "$2");
				try {
					handler.handleEdge(nodeId1, nodeId2);
				} catch (Exception ex) {
					ex.printStackTrace();
					break;
				}
			}
		}
		reader.close();
	}

}
