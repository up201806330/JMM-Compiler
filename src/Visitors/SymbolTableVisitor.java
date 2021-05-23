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
        addVisit(Consts.classDeclNodeName, this::dealWithClassDecl);
        addVisit(Consts.classInheritNodeName, this::dealWithClassInheritance);
        addVisit(Consts.importDeclNodeName, this::dealWithImportDecl);
        addVisit(Consts.varDeclNodeName, this::dealWithVarDecl);
        addVisit(Consts.methodDeclNodeName, this::dealWithMethodDecl);
        addVisit(Consts.methodParamNodeName, this::dealWithMethodParameter);
        setDefaultVisit(SymbolTableVisitor::defaultVisit);
    }

    private Boolean dealWithClassDecl(JmmNode node, List<Report> reports){
        OurSymbol symbol = new OurSymbol(
                node,
                new HashSet<>(Arrays.asList(Consts.classAttribute)),
                new OurScope()
        );
        Optional<Report> insertionError = symbolTable.put(symbol, node);
        insertionError.ifPresent(reports::add);

        symbolTable.className = node.get(Consts.nameAttribute);
        return defaultVisit(node, reports);
    }

    private Boolean dealWithClassInheritance(JmmNode node, List<Report> reports){
        OurSymbol symbol = new OurSymbol(
                node,
                new HashSet<>(Arrays.asList(Consts.superAttribute, Consts.classAttribute)),
                new OurScope()
        );
        Optional<Report> insertionError = symbolTable.put(symbol, node);
        insertionError.ifPresent(reports::add);

        symbolTable.superName = node.get(Consts.typeAttribute);
        return defaultVisit(node, reports);
    }

    public Boolean dealWithImportDecl(JmmNode node, List<Report> reports){
        OurSymbol symbol;
        if (node.getChildren().size() != 0){
                symbol = new OurSymbol(
                        node.getChildren().get(node.getNumChildren() - 1),
                        new HashSet<>(Arrays.asList(Consts.importAttribute, Consts.classAttribute)),
                        new OurScope()
                );
        }
        else {
            symbol = new OurSymbol(
                    node,
                    new HashSet<>(Arrays.asList(Consts.importAttribute, Consts.classAttribute)),
                    new OurScope()
            );
        }

        Optional<Report> insertionError = symbolTable.put(symbol, node);
        insertionError.ifPresent(reports::add);
        return defaultVisit(node, reports);
    }

    public Boolean dealWithVarDecl(JmmNode node, List<Report> reports) {
        var parentFunction = node.getAncestor(Consts.methodDeclNodeName);
        List<String> attributes = new ArrayList<>();
        if (parentFunction.isEmpty()) attributes.add(Consts.fieldAttribute);
        else attributes.add(Consts.variableAttribute);

        OurSymbol symbol = new OurSymbol(
                node,
                new HashSet<>(attributes),
                parentFunction.isEmpty() ? new OurScope() :
                new OurScope(
                        OurScope.ScopeEnum.FunctionVariable,
                        symbolTable.getByValue(parentFunction.get())
                )
        );

        if (!Consts.isPrimitiveType(node.get(Consts.typeAttribute)) &&             // Isn't primitive type
                !symbolTable.getImports().contains(node.get(Consts.typeAttribute)) && // Or of a class that is included
                !symbolTable.getClassName().equals(node.get(Consts.typeAttribute))) { // Or of this class

            reports.add(new Report(
                    ReportType.WARNING,
                    Stage.SEMANTIC,
                    symbol.getLine(),
                    symbol.getColumn(),
                    "Type '" + node.get(Consts.typeAttribute) + "' not found"
            ));
        }


        if (symbol.getScope().scope == OurScope.ScopeEnum.Error){
            return defaultVisit(node, reports);
        }

        Optional<Report> insertionError = symbolTable.put(symbol, node);
        insertionError.ifPresent(reports::add);

        return defaultVisit(node, reports);
    }

    public Boolean dealWithMethodDecl(JmmNode node, List<Report> reports) {
        var attributes = new HashSet<String>();
        attributes.add(Consts.methodAttribute);
        if (node.getOptional(Consts.staticAttribute).isPresent()) attributes.add(Consts.staticAttribute);

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
            if (child.getKind().equals(Consts.methodParamNodeName))
                result.add(new Type(child.get(Consts.typeAttribute), Boolean.parseBoolean(child.get(Consts.arrayAttribute))));
        }
        return result;
    }

    public Boolean dealWithMethodParameter(JmmNode node, List<Report> reports){
        var parentFunction = node.getAncestor(Consts.methodDeclNodeName);
        OurSymbol symbol = new OurSymbol(
                node,
                new HashSet<>(Arrays.asList(Consts.parameterAttribute)),
                new OurScope(
                        OurScope.ScopeEnum.FunctionParameter,
                        parentFunction.map(jmmNode -> symbolTable.getByValue(jmmNode)).orElse(null)
                )
        );

        if (symbol.getScope().scope == OurScope.ScopeEnum.Error){
//            reports.add(
//                    new Report(
//                            ReportType.WARNING,
//                            Stage.SEMANTIC,
//                            symbol.getLine(),
//                            symbol.getColumn(),
//                            "Parameter '" + symbol.getName() + "' defined for method with a lexical error"));
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
