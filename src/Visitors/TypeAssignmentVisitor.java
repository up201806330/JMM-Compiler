import pt.up.fe.comp.jmm.JmmNode;
import pt.up.fe.comp.jmm.ast.PostorderJmmVisitor;
import pt.up.fe.comp.jmm.report.Report;

import java.util.List;

public class TypeAssignmentVisitor extends PostorderJmmVisitor<List<Report>, Boolean> {

    private final String typeNodeName = "Type";
    private final String typeAttribute = "type";
    private final String isArrayAttribute = "isArray";
    private final String arrayExprNodeName = "ArrayExpression";

    public TypeAssignmentVisitor() {
        addVisit(typeNodeName, this::dealWithType);
        addVisit(arrayExprNodeName, this::dealWithArrayExpr);
        setDefaultVisit(TypeAssignmentVisitor::defaultVisit);
    }

    public Boolean dealWithType(JmmNode node, List<Report> reports) {
        if (node.get(typeAttribute).equals("intArray")) {
            node.put(isArrayAttribute, "true");
            node.put(typeAttribute, "int");
        }
        else
            node.put(isArrayAttribute, "false");

        node.getParent().put(typeAttribute, node.get(typeAttribute));
        node.getParent().put(isArrayAttribute, node.get(isArrayAttribute));
        return defaultVisit(node, reports);
    }

    private Boolean dealWithArrayExpr(JmmNode node, List<Report> reports){
        node.put("type", "int"); // Since arrays can only be of ints, this is hardcoded
        return defaultVisit(node, reports);
    }

    private static Boolean defaultVisit(JmmNode node, List<Report> kindCount) {
        return true;
    }

}
