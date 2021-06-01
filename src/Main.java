import pt.up.fe.comp.TestUtils;
import pt.up.fe.comp.jmm.JmmParser;
import pt.up.fe.comp.jmm.JmmParserResult;
import pt.up.fe.comp.jmm.analysis.JmmSemanticsResult;
import pt.up.fe.comp.jmm.jasmin.JasminResult;
import pt.up.fe.comp.jmm.jasmin.JasminUtils;
import pt.up.fe.comp.jmm.ollir.OllirResult;
import pt.up.fe.comp.jmm.report.Report;
import pt.up.fe.comp.jmm.report.ReportType;
import pt.up.fe.comp.jmm.report.Stage;
import pt.up.fe.specs.util.SpecsIo;

import java.io.File;
import java.io.FileNotFoundException;
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
		if (TestUtils.getNumErrors(parserResults.getReports()) > 0)
			return;

		var originalJSON = parserResults.toJson();

		AnalysisStage analysis = new AnalysisStage();
		JmmSemanticsResult semanticsResults = analysis.semanticAnalysis(parserResults);

		showReports(semanticsResults.getReports());
		if (TestUtils.getNumErrors(semanticsResults.getReports()) > 0)
			return;

		OptimizationStage optimization = new OptimizationStage();
		OllirResult ollirResult;
		if (args.length == 2){
			if (args[1].startsWith("-r"))
				ollirResult = optimization.toOllir(semanticsResults, false, Integer.parseInt(args[1].substring(args[1].lastIndexOf("=") + 1)));
			else if (args[1].startsWith("-o"))
				ollirResult = optimization.toOllir(semanticsResults, true);
			else
				ollirResult = optimization.toOllir(semanticsResults);
		}
		else if (args.length == 3){
			int r;
			boolean o = args[1].startsWith("-o") || args[2].startsWith("-o");

			if (args[1].startsWith("-r")) {
				r = Integer.parseInt(args[1].substring(args[1].lastIndexOf("=") + 1));
				ollirResult = optimization.toOllir(semanticsResults, o, r);
			}
			else if (args[2].startsWith("-r")) {
				r = Integer.parseInt(args[2].substring(args[2].lastIndexOf("=") + 1));
				ollirResult = optimization.toOllir(semanticsResults, o, r);
			}
			else {
				ollirResult = optimization.toOllir(semanticsResults, o);
			}


		}
		else {
			ollirResult = optimization.toOllir(semanticsResults);
		}

		BackendStage backend = new BackendStage();
		JasminResult jasminResult = backend.toJasmin(ollirResult);

		if (jasminResult.getReports().size() > 0){
			jasminResult.getReports().forEach(System.out::println);
		}
		else {
//			System.out.println("vvvv Program results vvvv");
//			jasminResult.run();
			System.out.println("Compiled!");

			// Write original JSON
			SpecsIo.write(new File("./" + jasminResult.getClassName() + ".json"), originalJSON);

			// Write optimized JSON, if optimizations were applied
			if ((args.length > 1 && args[1].startsWith("-o")) || (args.length > 2 && args[2].startsWith("-o")))
				SpecsIo.write(new File("./" + jasminResult.getClassName() + "_Optimized" + ".json"), parserResults.toJson());

			// Write Symbol table
			SpecsIo.write(new File("./" + jasminResult.getClassName() + ".symbols.txt"), semanticsResults.getSymbolTable().print());

			// Write OLLIR
			SpecsIo.write(new File("./" + jasminResult.getClassName() + ".ollir"), ollirResult.getOllirCode());

			// Write Jasmin
			var jasminFile = new File("./" + jasminResult.getClassName() + ".j");
			SpecsIo.write(jasminFile, jasminResult.getJasminCode());

			// Write .class
			JasminUtils.assemble(jasminFile, new File("./"));
		}
	}

	private static void showReports(List<Report> reports){
		if (reports.size() > 0) {
			for (Report r : reports.subList(0, Math.min(reports.size(), 10))) { // Only shows first 10
				System.out.println("Report: " + r);
			}
			if (reports.size() > 10) System.out.println("Aditional errors hidden");
		}
	}
}
