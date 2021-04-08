import pt.up.fe.comp.jmm.JmmNode;
import pt.up.fe.comp.jmm.ast.PreorderJmmVisitor;
import pt.up.fe.comp.jmm.report.Report;
import pt.up.fe.comp.jmm.report.ReportType;
import pt.up.fe.comp.jmm.report.Stage;

import java.util.*;

public class SymbolTableVisitor extends PreorderJmmVisitor<List<Report>, Boolean> {

    private final String classDeclNodeName = "ClassDeclaration";
    private final String classInheritNodeName = "ClassInheritance";
    private final String importDeclNodeName = "ImportDeclaration";
    private final String methodDeclNodeName = "MethodDeclaration";
    private final String varDeclNodeName = "VarDeclaration";
    private final String methodParamNodeName = "Parameter";

    OurSymbolTable symbolTable = new OurSymbolTable();

    OurSymbolTable getSymbolTable(){ return symbolTable; }

    public SymbolTableVisitor() {
        addVisit(classDeclNodeName, this::dealWithClassDecl);
        addVisit(classInheritNodeName, this::dealWithClassInheritance);
        addVisit(importDeclNodeName, this::dealWithImportDecl);
        addVisit(varDeclNodeName, this::dealWithVarDecl);
        addVisit(methodDeclNodeName, this::dealWithMethodDecl);
        addVisit(methodParamNodeName, this::dealWithMethodParameter);
        setDefaultVisit(SymbolTableVisitor::defaultVisit);
    }

    private Boolean dealWithClassDecl(JmmNode node, List<Report> reports){
        symbolTable.className = node.get("name");
        return defaultVisit(node, reports);
    }

    private Boolean dealWithClassInheritance(JmmNode node, List<Report> reports){
        symbolTable.superName = node.get("type");
        return defaultVisit(node, reports);
    }

    public Boolean dealWithImportDecl(JmmNode node, List<Report> reports){
        OurSymbol symbol = new OurSymbol(
                node,
                new HashSet<>(Arrays.asList("import")),
                new OurScope()
        );
        Optional<Report> insertionError = symbolTable.put(symbol, node);
        insertionError.ifPresent(reports::add);

        return defaultVisit(node, reports);
    }

    public Boolean dealWithVarDecl(JmmNode node, List<Report> reports) {
        var parentFunction = node.getAncestor(methodDeclNodeName);
        OurSymbol symbol = new OurSymbol(
                node,
                new HashSet<>(Arrays.asList("variable")),
                new OurScope(
                        OurScope.ScopeEnum.FunctionVariable,
                        parentFunction.map(ancestorNode -> symbolTable.getByValue(ancestorNode)).orElse(null)
                )
        );

        if (symbol.getScope().scope == OurScope.ScopeEnum.Error){
            reports.add(
                    new Report(
                            ReportType.WARNING,
                            Stage.SEMANTIC,
                            symbol.getLine(),
                            symbol.getColumn(),
                            "Variable '" + symbol.getName() + "' defined inside method with a lexical error"));
            return defaultVisit(node, reports);
        }

        Optional<Report> insertionError = symbolTable.put(symbol, node);
        insertionError.ifPresent(reports::add);

        return defaultVisit(node, reports);
    }

    public Boolean dealWithMethodDecl(JmmNode node, List<Report> reports) {
        OurSymbol symbol = new OurSymbol(
                node,
                new HashSet<>(Arrays.asList("method")),
                new OurScope());

        Optional<Report> insertionError = symbolTable.put(symbol, node);
        insertionError.ifPresent(reports::add);

        return defaultVisit(node, reports);
    }

    public Boolean dealWithMethodParameter(JmmNode node, List<Report> reports){
        var parentFunction = node.getAncestor(methodDeclNodeName);
        OurSymbol symbol = new OurSymbol(
                node,
                new HashSet<>(Arrays.asList("parameter")),
                new OurScope(
                        OurScope.ScopeEnum.FunctionParameter,
                        parentFunction.map(jmmNode -> symbolTable.getByValue(jmmNode)).orElse(null)
                )
        );

        if (symbol.getScope().scope == OurScope.ScopeEnum.Error){
            reports.add(
                    new Report(
                            ReportType.WARNING,
                            Stage.SEMANTIC,
                            symbol.getLine(),
                            symbol.getColumn(),
                            "Parameter '" + symbol.getName() + "' defined for method with a lexical error"));
            return defaultVisit(node, reports);
        }

        Optional<Report> insertionError = symbolTable.put(symbol, node);
        insertionError.ifPresent(reports::add);

        return defaultVisit(node, reports);
    }

    private static Boolean defaultVisit(JmmNode node, List<Report> reports) {
        return true;
    }

}
