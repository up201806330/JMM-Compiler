
import pt.up.fe.comp.MainAnalysis;
import pt.up.fe.comp.jmm.JmmParser;
import pt.up.fe.comp.jmm.JmmParserResult;
import pt.up.fe.comp.jmm.analysis.JmmSemanticsResult;
import pt.up.fe.comp.jmm.report.Report;
import pt.up.fe.comp.jmm.report.ReportType;
import pt.up.fe.comp.jmm.report.Stage;
import pt.up.fe.specs.util.SpecsIo;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.*;
import java.io.StringReader;

public class Main implements JmmParser {


	public JmmParserResult parse(String jmmCode) {
		List<Report> reports = new ArrayList<Report>();
		SimpleNode root = null;

		try {
		    Jmm jmm = new Jmm(new StringReader(jmmCode));
    		root = jmm.Program(); // returns reference to root node
            	
    		// root.dump(""); // prints the tree on the screen

			reports = jmm.getReports();

    		return new JmmParserResult(root, reports);
		} catch(Exception e) {
			//throw new RuntimeException("Error while parsing", e);
			reports.add(new Report(ReportType.ERROR, Stage.SYNTATIC, -1, -1, e.getMessage()));
			return new JmmParserResult(root, reports);
		}
	}

    public static void main(String[] args) throws FileNotFoundException {
		Main temp = new Main();
		String fileContents = SpecsIo.read(args[0]);
		JmmParserResult parserResults = temp.parse(fileContents);

		showReports(parserResults.getReports());

		// Write to JSON
		// File output = new File("./outputJson.txt");
		// try (PrintWriter out = new PrintWriter(output)) {
		//     out.println(parserResults.toJson());
		// }

		AnalysisStage analysis = new AnalysisStage();
		JmmSemanticsResult semanticsResults = analysis.semanticAnalysis(parserResults);

		showReports(semanticsResults.getReports());
	}

	private static void showReports(List<Report> reports){
		if (reports.size() > 0) {
			for (Report r : reports.subList(0, Math.min(reports.size(), 10))) { // Only shows first 10
				System.out.println("Report: " + r.getMessage());
			}
			if (reports.size() > 10) System.out.println("Aditional errors hidden");
		}
	}

}