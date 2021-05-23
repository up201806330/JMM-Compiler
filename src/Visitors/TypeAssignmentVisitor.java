import pt.up.fe.comp.jmm.JmmNode;
import pt.up.fe.comp.jmm.ast.PostorderJmmVisitor;
import pt.up.fe.comp.jmm.report.Report;

import java.util.List;

public class TypeAssignmentVisitor extends PostorderJmmVisitor<List<Report>, Boolean> {


    public TypeAssignmentVisitor() {
        addVisit(Consts.typeNodeName, this::dealWithTypeAffectingParent);
        addVisit(Consts.terminalNodeName, this::dealWithTypeNotAffectingParent);
        addVisit(Consts.propertyAccessNodeName, this::dealWithTypeNotAffectingParent);
        addVisit(Consts.newNodeName, this::dealWithTypeNotAffectingParent);
        addVisit(Consts.literalNodeName, this::dealWithTypeNotAffectingParent);
        addVisit(Consts.arrayExprNodeName, this::dealWithArrayExpr);
        setDefaultVisit(TypeAssignmentVisitor::defaultVisit);
    }

    private void setType(JmmNode node) {
        if (node.get(Consts.typeAttribute).equals(Consts.intArrayType)) {
            node.put(Consts.arrayAttribute, "true");
            node.put(Consts.typeAttribute, Consts.intType);
        }
        else if (node.get(Consts.typeAttribute).equals(Consts.stringArrayType)) {
            node.put(Consts.arrayAttribute, "true");
            node.put(Consts.typeAttribute, Consts.stringType);
        }
        else
            node.put(Consts.arrayAttribute, "false");
    }

    public Boolean dealWithTypeAffectingParent(JmmNode node, List<Report> reports) {
        setType(node);

        node.getParent().put(Consts.typeAttribute, node.get(Consts.typeAttribute));
        node.getParent().put(Consts.arrayAttribute, node.get(Consts.arrayAttribute));
        return defaultVisit(node, reports);
    }

    public Boolean dealWithTypeNotAffectingParent(JmmNode node, List<Report> reports) {
        setType(node);

        return defaultVisit(node, reports);
    }

    private Boolean dealWithArrayExpr(JmmNode node, List<Report> reports){
        node.put(Consts.typeAttribute, Consts.intType); // Since arrays can only be of ints, this is hardcoded
        node.put(Consts.arrayAttribute, "false");
        return defaultVisit(node, reports);
    }

    private static Boolean defaultVisit(JmmNode node, List<Report> kindCount) {
        return true;
    }

}
