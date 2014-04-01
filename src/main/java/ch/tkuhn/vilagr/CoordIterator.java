package ch.tkuhn.vilagr;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.List;

import org.supercsv.io.CsvListReader;
import org.supercsv.prefs.CsvPreference;

// Iterates over the nodes and their coordinates read from a csv or gexf file.
public class CoordIterator {

	public static interface CoordHandler {

		public void handleCoord(String nodeId, float x, float y) throws Exception;

	}

	private static CsvPreference csvPref = CsvPreference.STANDARD_PREFERENCE;

	public static void setCsvPreference(CsvPreference csvPreference) {
		csvPref = csvPreference;
	}

	private File file;
	private CoordHandler handler;

	public CoordIterator(File file, CoordHandler handler) {
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
			String nodeId = line.get(0);
			float x = Float.parseFloat(line.get(1));
			float y = Float.parseFloat(line.get(2));
			try {
				handler.handleCoord(nodeId, x, y);
			} catch (Exception ex) {
				ex.printStackTrace();
				break;
			}
		}
		csvReader.close();
	}


	private static final String idPattern = "^\\s*<node id=\"(.*?)\".*$";
	private static final String coordPattern = "^\\s*<viz:position x=\"(.*?)\" y=\"(.*?)\".*$";

	private void processGexf() throws IOException {
		BufferedReader reader = new BufferedReader(new FileReader(file), 64*1024);
		String line;
		String nodeId = null;
		while ((line = reader.readLine()) != null) {
			if (line.matches(idPattern)) {
				if (nodeId != null) {
					reader.close();
					throw new RuntimeException("No coordinates found for: " + nodeId);
				}
				nodeId = line.replaceFirst(idPattern, "$1");
			} else if (line.matches(coordPattern)) {
				if (nodeId == null) {
					reader.close();
					throw new RuntimeException("No ID found for coordinates: " + line);
				} else {
					float x = Float.parseFloat(line.replaceFirst(coordPattern, "$1"));
					float y = Float.parseFloat(line.replaceFirst(coordPattern, "$2"));
					try {
						handler.handleCoord(nodeId, x, y);
					} catch (Exception ex) {
						ex.printStackTrace();
						break;
					}
					nodeId = null;
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