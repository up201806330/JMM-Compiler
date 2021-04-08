import pt.up.fe.comp.jmm.JmmNode;
import pt.up.fe.comp.jmm.ast.PostorderJmmVisitor;
import pt.up.fe.comp.jmm.report.Report;
import pt.up.fe.comp.jmm.report.ReportType;
import pt.up.fe.comp.jmm.report.Stage;

import java.util.List;

public class TypeVerificationVisitor extends PostorderJmmVisitor<List<Report>, Boolean> {

    private final String terminalNodeName = "Terminal";
    private final String binaryNodeName = "Binary";
    private final String methodDeclNodeName = "MethodDeclaration";

    OurSymbolTable symbolTable;

    public TypeVerificationVisitor(OurSymbolTable symbolTable){
        this.symbolTable = symbolTable;

        addVisit(terminalNodeName, this::dealWithTerminal);
        addVisit(binaryNodeName, this::dealWithBinary);
        setDefaultVisit(TypeVerificationVisitor::defaultVisit);
    }

    private Boolean dealWithTerminal(JmmNode node, List<Report> reports){
        if (!node.get("type").equals("identifier")) return defaultVisit(node, reports);

        var parentFunction = node.getAncestor(methodDeclNodeName);

        String returnType = symbolTable.getLocalVariableReturnType(
                parentFunction.map(jmmNode -> jmmNode.get("name")).orElse(null)
                , node.get("name"));

        if (returnType == null){
            reports.add(new Report(
                    ReportType.WARNING,
                    Stage.SEMANTIC,
                    Integer.parseInt(node.get("line")),
                    Integer.parseInt(node.get("column")),
                    "Variable " + node.get("name") + " must be declared before being used"
            ));
            node.put("type", "error");
            return defaultVisit(node, reports);
        }

        node.put("type", returnType);

        return defaultVisit(node, reports);
    }


    private Boolean dealWithBinary(JmmNode node, List<Report> reports){
        List<JmmNode> children = node.getChildren();
        if (children.size() != 2) {
            reports.add(new Report(
                    ReportType.WARNING,
                    Stage.SEMANTIC,
                    Integer.parseInt(node.get("line")),
                    Integer.parseInt(node.get("column")),
                    "Binary node doesn't have 2 children. Something is wrong in syntactic phase"
            ));
        }

        String leftType = children.get(0).get("type");
        String rightType = children.get(1).get("type");

        if (leftType.equals("error") || rightType.equals("error")){
            node.put("type", "error");
            return defaultVisit(node, reports);
        }

        if (!leftType.equals(rightType)){
            reports.add(new Report(
                    ReportType.ERROR,
                    Stage.SEMANTIC,
                    Integer.parseInt(node.get("line")),
                    Integer.parseInt(node.get("column")),
                    "Type mismatch (" + leftType + " != " + rightType + ")"
            ));
            node.put("type", "error");
            return defaultVisit(node, reports);
        }

        node.put("type", leftType);
        return defaultVisit(node, reports);
    }


    private static Boolean defaultVisit(JmmNode node, List<Report> reports) {
        return true;
    }
}
