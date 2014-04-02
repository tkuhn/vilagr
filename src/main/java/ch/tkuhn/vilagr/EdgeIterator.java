package ch.tkuhn.vilagr;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.List;

import org.supercsv.io.CsvListReader;
import org.supercsv.prefs.CsvPreference;

// Iterates over the edges read from a gexf file.
public class EdgeIterator {

	public static interface EdgeHandler {

		public void handleEdge(String nodeId1, String nodeId2) throws Exception;

	}

	private static CsvPreference csvPref = CsvPreference.STANDARD_PREFERENCE;

	public static void setCsvPreference(CsvPreference csvPreference) {
		csvPref = csvPreference;
	}

	private File file;
	private EdgeHandler handler;

	public EdgeIterator(File file, EdgeHandler handler) {
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


	private static final String edgePattern = "^\\s*<edge.* source=\"(.*?)\".* target=\"(.*?)\".*$";

	private void processGexf() throws IOException {
		BufferedReader reader = new BufferedReader(new FileReader(file), 64*1024);
		String line;
		while ((line = reader.readLine()) != null) {
			if (line.matches(edgePattern)) {
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
