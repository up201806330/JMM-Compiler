import pt.up.fe.comp.jmm.JmmNode;
import pt.up.fe.comp.jmm.ast.PreorderJmmVisitor;
import pt.up.fe.comp.jmm.report.Report;

import java.util.*;

public class SymbolTableVisitor extends PreorderJmmVisitor<List<Report>, Boolean> {

    private final String importDeclNodeName = "ImportDeclaration";
    private final String methodDeclNodeName = "MethodDeclaration";
    private final String varDeclNodeName = "VarDeclaration";
    private final String methodParamNodeName = "Parameter";

    OurSymbolTable symbolTable;

    OurSymbolTable getSymbolTable(){ return symbolTable; }

    public SymbolTableVisitor() {
        addVisit(importDeclNodeName, this::dealWithImportDecl);
        addVisit(varDeclNodeName, this::dealWithVarDecl);
        addVisit(methodDeclNodeName, this::dealWithMethodDecl);
        addVisit(methodParamNodeName, this::dealWithMethodParameter);
        setDefaultVisit(SymbolTableVisitor::defaultVisit);
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
                        parentFunction.map(jmmNode -> symbolTable.getByValue(jmmNode)).orElse(null)
                )
        );

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

        Optional<Report> insertionError = symbolTable.put(symbol, node);
        insertionError.ifPresent(reports::add);

        return defaultVisit(node, reports);
    }

    private static Boolean defaultVisit(JmmNode node, List<Report> reports) {
        return true;
    }

}
