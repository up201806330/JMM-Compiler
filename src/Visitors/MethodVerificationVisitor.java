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
        var argsList = children.get(1);

        var targetClassSymbolOpt = symbolTable.tryGettingSymbol(
                node.getAncestor(Constants.methodDeclNodeName).map(ancestorNode -> ancestorNode.get(Constants.nameAttribute)).orElse("this"),
                target.get(Constants.valueAttribute));

        var targetClass = targetClassSymbolOpt.isPresent() ?
                targetClassSymbolOpt.get().getType().getName() :
                target.get(Constants.valueAttribute);
        System.out.println(targetClass);
        var methodName = node.get(Constants.nameAttribute);

        var methodTypeOpt = symbolTable.tryGettingSymbolType(
                targetClass,
                methodName);

        if ( targetClass.equals(Constants.thisAttribute) ||   // If method was called on this context ( e.g. this.Foo() || ThisClass a; a.Foo() )
                targetClass.equals(symbolTable.className) ){  // Or if it was called on this class' static context (e.g. ThisClass.Foo() )

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
            else if (target.get(Constants.valueAttribute).equals(symbolTable.className) &&        // If method was called on this class' static context
                    (targetClassSymbolOpt.isPresent() && !targetClassSymbolOpt.get().isStatic())){ // but function isn't static (e.g. public void Foo(); ThisClass.Foo() )

                reports.add(new Report(
                        ReportType.WARNING,
                        Stage.SEMANTIC,
                        Integer.parseInt(node.get("line")),
                        Integer.parseInt(node.get("column")),
                        "Non-static method '" + methodName + "' cannot be referenced from a static context"
                ));
                node.put(Constants.typeAttribute, Constants.error);
                node.put(Constants.arrayAttribute, Constants.error);
            }
            else if (symbolTable.superName != null){ // If there is inheritance present, assume method is defined there and assume type is correct
                node.put(Constants.typeAttribute, Constants.autoType);
                node.put(Constants.arrayAttribute, "false");
            }
            else { // If method name was found in this class
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
            if (symbolTable.getImports().contains(targetClass)){ // If calling context is an imported class, assume method is defined there and assume type is correct
                node.put(Constants.typeAttribute, Constants.autoType);
                node.put(Constants.arrayAttribute, "false");
            }
            else { // If calling context isn't found
                reports.add(new Report(
                        ReportType.ERROR,
                        Stage.SEMANTIC,
                        Integer.parseInt(node.get("line")),
                        Integer.parseInt(node.get("column")),
                        "Cannot resolve class '" + targetClass + "'"
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
