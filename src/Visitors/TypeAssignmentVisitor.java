import pt.up.fe.comp.jmm.JmmNode;
import pt.up.fe.comp.jmm.ast.PostorderJmmVisitor;
import pt.up.fe.comp.jmm.report.Report;

import java.util.List;

public class TypeAssignmentVisitor extends PostorderJmmVisitor<List<Report>, Boolean> {


    public TypeAssignmentVisitor() {
        addVisit(Constants.typeNodeName, this::dealWithTypeAffectingParent);
        addVisit(Constants.terminalNodeName, this::dealWithTypeNotAffectingParent);
        addVisit(Constants.propertyAccessNodeName, this::dealWithTypeNotAffectingParent);
        addVisit(Constants.newNodeName, this::dealWithTypeNotAffectingParent);
        addVisit(Constants.literalNodeName, this::dealWithTypeNotAffectingParent);
        addVisit(Constants.arrayExprNodeName, this::dealWithArrayExpr);
        setDefaultVisit(TypeAssignmentVisitor::defaultVisit);
    }

    private void setType(JmmNode node) {
        if (node.get(Constants.typeAttribute).equals(Constants.intArrayType)) {
            node.put(Constants.arrayAttribute, "true");
            node.put(Constants.typeAttribute, Constants.intType);
        }
        else if (node.get(Constants.typeAttribute).equals(Constants.stringArrayType)) {
            node.put(Constants.arrayAttribute, "true");
            node.put(Constants.typeAttribute, Constants.stringType);
        }
        else
            node.put(Constants.arrayAttribute, "false");
    }

    public Boolean dealWithTypeAffectingParent(JmmNode node, List<Report> reports) {
        setType(node);

        node.getParent().put(Constants.typeAttribute, node.get(Constants.typeAttribute));
        node.getParent().put(Constants.arrayAttribute, node.get(Constants.arrayAttribute));
        return defaultVisit(node, reports);
    }

    public Boolean dealWithTypeNotAffectingParent(JmmNode node, List<Report> reports) {
        setType(node);

        return defaultVisit(node, reports);
    }

    private Boolean dealWithArrayExpr(JmmNode node, List<Report> reports){
        node.put(Constants.typeAttribute, Constants.intType); // Since arrays can only be of ints, this is hardcoded
        node.put(Constants.arrayAttribute, "false");
        return defaultVisit(node, reports);
    }

    private static Boolean defaultVisit(JmmNode node, List<Report> kindCount) {
        return true;
    }

}
