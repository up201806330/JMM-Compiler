import pt.up.fe.comp.jmm.JmmNode;
import pt.up.fe.comp.jmm.ast.PostorderJmmVisitor;
import pt.up.fe.comp.jmm.report.Report;

import java.util.List;

public class OllirExprVisitor extends PostorderJmmVisitor<List<Report>, Boolean> {

    OllirExprVisitor(){

        addVisit(Constants.terminalNodeName, this::dealWithTerminal);
        addVisit(Constants.binaryNodeName, this::dealWithBinary);
        setDefaultVisit(OllirExprVisitor::defaultVisit);
    }

    private Boolean dealWithBinary(JmmNode node, List<Report> reports) {
        return defaultVisit(node, reports);
    }

    private Boolean dealWithTerminal(JmmNode node, List<Report> reports) {
        return defaultVisit(node, reports);
    }

    private static Boolean defaultVisit(JmmNode node, List<Report> reports) {
        return true;
    }
}
