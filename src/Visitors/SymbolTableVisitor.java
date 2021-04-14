import pt.up.fe.comp.jmm.JmmNode;
import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp.jmm.ast.PreorderJmmVisitor;
import pt.up.fe.comp.jmm.report.Report;
import pt.up.fe.comp.jmm.report.ReportType;
import pt.up.fe.comp.jmm.report.Stage;

import java.util.*;

public class SymbolTableVisitor extends PreorderJmmVisitor<List<Report>, Boolean> {


    OurSymbolTable symbolTable = new OurSymbolTable();

    OurSymbolTable getSymbolTable(){ return symbolTable; }

    public SymbolTableVisitor() {
        addVisit(Constants.classDeclNodeName, this::dealWithClassDecl);
        addVisit(Constants.classInheritNodeName, this::dealWithClassInheritance);
        addVisit(Constants.importDeclNodeName, this::dealWithImportDecl);
        addVisit(Constants.varDeclNodeName, this::dealWithVarDecl);
        addVisit(Constants.methodDeclNodeName, this::dealWithMethodDecl);
        addVisit(Constants.methodParamNodeName, this::dealWithMethodParameter);
        setDefaultVisit(SymbolTableVisitor::defaultVisit);
    }

    private Boolean dealWithClassDecl(JmmNode node, List<Report> reports){
        OurSymbol symbol = new OurSymbol(
                node,
                new HashSet<>(Arrays.asList(Constants.classAttribute)),
                new OurScope()
        );
        Optional<Report> insertionError = symbolTable.put(symbol, node);
        insertionError.ifPresent(reports::add);

        symbolTable.className = node.get(Constants.nameAttribute);
        return defaultVisit(node, reports);
    }

    private Boolean dealWithClassInheritance(JmmNode node, List<Report> reports){
        symbolTable.superName = node.get(Constants.typeAttribute);
        return defaultVisit(node, reports);
    }

    public Boolean dealWithImportDecl(JmmNode node, List<Report> reports){
        OurSymbol symbol = new OurSymbol(
                node,
                new HashSet<>(Arrays.asList(Constants.importAttribute, Constants.classAttribute)),
                new OurScope()
        );
        Optional<Report> insertionError = symbolTable.put(symbol, node);
        insertionError.ifPresent(reports::add);

        return defaultVisit(node, reports);
    }

    public Boolean dealWithVarDecl(JmmNode node, List<Report> reports) {
        var parentFunction = node.getAncestor(Constants.methodDeclNodeName);
        List<String> attributes = new ArrayList<>(); attributes.add(Constants.variableAttribute);
        if (parentFunction.isEmpty()) attributes.add(Constants.fieldAttribute);
        OurSymbol symbol = new OurSymbol(
                node,
                new HashSet<>(attributes),
                parentFunction.isEmpty() ? new OurScope() :
                new OurScope(
                        OurScope.ScopeEnum.FunctionVariable,
                        symbolTable.getByValue(parentFunction.get())
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
        var attributes = new HashSet<String>();
        attributes.add(Constants.methodAttribute);
        if (node.getOptional(Constants.staticAttribute).isPresent()) attributes.add(Constants.staticAttribute);

        OurSymbol symbol = new OurSymbol(
                node,
                attributes,
                new OurScope());

        symbol.insertParameterTypes(getParameterTypes(node));
        Optional<Report> insertionError = symbolTable.put(symbol, node);
        insertionError.ifPresent(reports::add);

        return defaultVisit(node, reports);
    }

    private List<Type> getParameterTypes(JmmNode node) {
        List<Type> result = new ArrayList<>();
        for (JmmNode child : node.getChildren()){
            if (child.getKind().equals(Constants.methodParamNodeName))
                result.add(new Type(child.get(Constants.typeAttribute), Boolean.parseBoolean(child.get(Constants.arrayAttribute))));
        }
        return result;
    }

    public Boolean dealWithMethodParameter(JmmNode node, List<Report> reports){
        var parentFunction = node.getAncestor(Constants.methodDeclNodeName);
        OurSymbol symbol = new OurSymbol(
                node,
                new HashSet<>(Arrays.asList(Constants.parameterAttribute)),
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
