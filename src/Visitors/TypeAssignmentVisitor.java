import pt.up.fe.comp.jmm.JmmNode;
import pt.up.fe.comp.jmm.ast.PostorderJmmVisitor;
import pt.up.fe.comp.jmm.report.Report;

import java.util.List;

public class TypeAssignmentVisitor extends PostorderJmmVisitor<List<Report>, Boolean> {

    private static final String typeNodeName = "Type";
    private final static String terminalNodeName = "Terminal";
    private final static String propertyAccessNodeName = "PropertyAccess";
    private static final String newNodeName = "New";
    private static final String literalNodeName = "Literal";
    private static final String arrayExprNodeName = "ArrayExpression";

    private static final String typeAttribute = "type";
    private static final String isArrayAttribute = "isArray";

    public TypeAssignmentVisitor() {
        addVisit(typeNodeName, this::dealWithTypeAffectingParent);
        addVisit(terminalNodeName, this::dealWithTypeNotAffectingParent);
        addVisit(propertyAccessNodeName, this::dealWithTypeNotAffectingParent);
        addVisit(newNodeName, this::dealWithTypeNotAffectingParent);
        addVisit(literalNodeName, this::dealWithTypeNotAffectingParent);
        addVisit(arrayExprNodeName, this::dealWithArrayExpr);
        setDefaultVisit(TypeAssignmentVisitor::defaultVisit);
    }

    private void setType(JmmNode node) {
        if (node.get(typeAttribute).equals("intArray")) {
            node.put(isArrayAttribute, "true");
            node.put(typeAttribute, "int");
        }
        else
            node.put(isArrayAttribute, "false");
    }

    public Boolean dealWithTypeAffectingParent(JmmNode node, List<Report> reports) {
        setType(node);

        node.getParent().put(typeAttribute, node.get(typeAttribute));
        node.getParent().put(isArrayAttribute, node.get(isArrayAttribute));
        return defaultVisit(node, reports);
    }

    public Boolean dealWithTypeNotAffectingParent(JmmNode node, List<Report> reports) {
        setType(node);

        return defaultVisit(node, reports);
    }

    private Boolean dealWithArrayExpr(JmmNode node, List<Report> reports){
        node.put(typeAttribute, "int"); // Since arrays can only be of ints, this is hardcoded
        node.put(isArrayAttribute, "false");
        return defaultVisit(node, reports);
    }

    private static Boolean defaultVisit(JmmNode node, List<Report> kindCount) {
        return true;
    }

}
