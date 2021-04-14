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

        if ( targetClassName.equals(Constants.thisAttribute) ||   // If method was called on this context ( e.g. this.Foo() )
                (targetClassName.equals(symbolTable.className) && // Or if it was called on this class' static context (e.g. ThisClass.Foo() )
                        node.getOptional(Constants.staticAttribute).isPresent()) ){

            if (methodTypeOpt.isEmpty() && symbolTable.superName == null) { // If method name isn't found and there is no inheritance present
                reports.add(new Report(
                        ReportType.ERROR,
                        Stage.SEMANTIC,
                        Integer.parseInt(node.get("line")),
                        Integer.parseInt(node.get("column")),
                        "Cannot resolve method '" + methodName +
                                "' in class '" + symbolTable.className
                ));
                node.put(Constants.typeAttribute, Constants.error);
                node.put(Constants.arrayAttribute, Constants.error);
            }
            else if (symbolTable.superName != null){ // If there is inheritance present, assume method is defined there and assume type is correct
                node.put(Constants.typeAttribute, Constants.autoType);
                node.put(Constants.arrayAttribute, "false");
            }
            else { // If method name was found in this class
                var argsList = children.get(1);
                var args = argsList.getChildren();
                var method = symbolTable.getMethodWithNParameters(methodName, args.size());
                if (method.isEmpty()) { // Check if method signature is correct (number of parameters and TODO types of parameters)
                    reports.add(new Report(
                            ReportType.ERROR,
                            Stage.SEMANTIC,
                            Integer.parseInt(node.get("line")),
                            Integer.parseInt(node.get("column")),
                            "No definition of " + methodName +
                                    " has " + args.size() +
                                    " parameters "
                    ));
                    node.put(Constants.typeAttribute, Constants.error);
                    node.put(Constants.arrayAttribute, Constants.error);
                } else { // Method signature is correct, success!
                    node.put(Constants.typeAttribute, methodTypeOpt.get().getName());
                    node.put(Constants.arrayAttribute, String.valueOf(methodTypeOpt.get().isArray()));
                }
            }
        }
        else { // If method isn't called in this context or own class context
            if (symbolTable.getImports().contains(targetClassName)){ // If calling context is an imported class, assume method is defined there and assume type is correct
                node.put(Constants.typeAttribute, Constants.autoType);
                node.put(Constants.arrayAttribute, "false");
            }
            else { // If calling context isn't found
                reports.add(new Report(
                        ReportType.ERROR,
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
