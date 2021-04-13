import pt.up.fe.comp.jmm.JmmNode;
import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp.jmm.ast.PostorderJmmVisitor;
import pt.up.fe.comp.jmm.report.Report;
import pt.up.fe.comp.jmm.report.ReportType;
import pt.up.fe.comp.jmm.report.Stage;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class MethodVerificationVisitor extends PostorderJmmVisitor<List<Report>, Boolean> {

    OurSymbolTable symbolTable;

    public MethodVerificationVisitor(OurSymbolTable symbolTable){
        this.symbolTable = symbolTable;

        addVisit(Constants.callExprNodeName, this::dealWithCallExpr);
        setDefaultVisit(MethodVerificationVisitor::defaultVisit);
    }

    private Boolean dealWithCallExpr(JmmNode node, List<Report> reports){
        var children = node.getChildren();
        var target = children.get(0);

        if (target.get(Constants.valueAttribute).equals(Constants.thisAttribute)){

        }

        return defaultVisit(node, reports);
    }

    private static Boolean defaultVisit(JmmNode node, List<Report> reports) {
        return true;
    }
}
