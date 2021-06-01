
import java.util.*;

import pt.up.fe.comp.TestUtils;
import pt.up.fe.comp.jmm.JmmNode;
import pt.up.fe.comp.jmm.JmmParserResult;
import pt.up.fe.comp.jmm.analysis.JmmAnalysis;
import pt.up.fe.comp.jmm.analysis.JmmSemanticsResult;
import pt.up.fe.comp.jmm.report.Report;
import pt.up.fe.comp.jmm.report.ReportType;
import pt.up.fe.comp.jmm.report.Stage;

public class AnalysisStage implements JmmAnalysis {

    public JmmSemanticsResult semanticAnalysis(JmmParserResult parserResult){

        if (TestUtils.getNumReports(parserResult.getReports(), ReportType.ERROR) > 0) {
            var errorReport = new Report(ReportType.ERROR, Stage.SEMANTIC, -1,
                    "Started semantic analysis but there are errors from previous stage");
            return new JmmSemanticsResult(parserResult, null, Arrays.asList(errorReport));
        }

        if (parserResult.getRootNode() == null) {
            var errorReport = new Report(ReportType.ERROR, Stage.SEMANTIC, -1,
                    "Started semantic analysis but AST root node is null");
            return new JmmSemanticsResult(parserResult, null, Arrays.asList(errorReport));
        }

        JmmNode node = parserResult.getRootNode();
        List<Report> reports = new ArrayList<>();

        //System.out.println("Dump tree generated by Parser");
        var visitor = new OurVisitor();
        //System.out.println(visitor.visit(node, ""));

        //System.out.println("Putting node types in correct nodes...");
        var anotherPostOrderVisitor = new TypeAssignmentVisitor();
        anotherPostOrderVisitor.visit(node, reports);

        //System.out.println("Creating Symbol Table...");
        var preorderVisitor = new SymbolTableVisitor();
        preorderVisitor.visit(node, reports);
        OurSymbolTable symbolTable = preorderVisitor.getSymbolTable();
        System.out.println(symbolTable.toString());

        //System.out.println("Type and Method verifications...");
        var typeVerificationVisitor = new TypeAndMethodVerificationVisitor(symbolTable);
        typeVerificationVisitor.visit(node, reports);

        //System.out.println("Dump tree after semantic verifications");
        var anotherVisitor = new OurVisitor();
        //System.out.println(anotherVisitor.visit(node, ""));

        return new JmmSemanticsResult(parserResult, symbolTable, reports);
    }

}