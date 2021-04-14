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
        var targetClassNameOpt = symbolTable.tryGettingSymbolType(
                node.getAncestor(Constants.methodDeclNodeName).map(ancestorNode -> ancestorNode.get(Constants.nameAttribute)).orElse("this"),
                target.get(Constants.valueAttribute));
        var targetClassName = targetClassNameOpt.isPresent() ? targetClassNameOpt.get().getName() : target.get(Constants.valueAttribute);

        var methodName = node.get(Constants.nameAttribute);

        var methodTypeOpt = symbolTable.tryGettingSymbolType(
                targetClassName,
                methodName);

        // If method is called on this and class doesn't extend any other class, will check for unresolved method
        if (targetClassName.equals(Constants.thisAttribute) || targetClassName.equals(symbolTable.className)){
            if (methodTypeOpt.isEmpty() && symbolTable.superName == null) {
                reports.add(new Report(
                        ReportType.WARNING,
                        Stage.SEMANTIC,
                        Integer.parseInt(node.get("line")),
                        Integer.parseInt(node.get("column")),
                        "Cannot resolve method '" + methodName +
                                "' in class '" + symbolTable.className
                ));
                node.put(Constants.typeAttribute, Constants.error);
                node.put(Constants.arrayAttribute, Constants.error);
            }
            else if (symbolTable.superName != null){
                node.put(Constants.typeAttribute, Constants.autoType);
                node.put(Constants.arrayAttribute, "false");
            }
            else {
                node.put(Constants.typeAttribute, methodTypeOpt.get().getName());
                node.put(Constants.arrayAttribute, String.valueOf(methodTypeOpt.get().isArray()));
            }
        }
        else {
            if (symbolTable.getImports().contains(targetClassName)){
                node.put(Constants.typeAttribute, Constants.autoType);
                node.put(Constants.arrayAttribute, "false");
            }
            else {
                reports.add(new Report(
                        ReportType.WARNING,
                        Stage.SEMANTIC,
                        Integer.parseInt(node.get("line")),
                        Integer.parseInt(node.get("column")),
                        "Cannot resolve class '" + targetClassName + "'"
                ));
                node.put(Constants.typeAttribute, Constants.error);
                node.put(Constants.arrayAttribute, Constants.error);
            }
        }

        return defaultVisit(node, reports);
    }

    private static Boolean defaultVisit(JmmNode node, List<Report> reports) {
        return true;
    }
}
