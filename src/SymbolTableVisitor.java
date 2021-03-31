import pt.up.fe.comp.jmm.JmmNode;
import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp.jmm.ast.PreorderJmmVisitor;
import pt.up.fe.comp.jmm.report.Report;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Stack;

public class SymbolTableVisitor extends PreorderJmmVisitor<List<Report>, Boolean> {

    private final String methodDeclNodeName = "MethodDeclaration";
    private final String varDeclNodeName = "VarDeclaration";
    private final String typeAttribute = "type";
    private final String nameAttribute = "name";
    private final String isArrayAttribute = "isArray";

    OurSymbolTable symbolTable;
    Stack<OurScope> scopeStack = new Stack<>();

    public SymbolTableVisitor(OurSymbolTable symbolTable) {
        addVisit(varDeclNodeName, this::dealWithVarDecl);
        addVisit(methodDeclNodeName, this::dealWithMethodDecl);
        setDefaultVisit(SymbolTableVisitor::defaultVisit);

        this.symbolTable = symbolTable;
        this.scopeStack.push(new OurScope(OurScope.ScopeEnum.Global, null));
    }

    public Boolean dealWithVarDecl(JmmNode node, List<Report> reports) {
        reports.add(symbolTable.put(
                new OurSymbol(
                        new Type(
                                node.get(typeAttribute),
                                Boolean.parseBoolean(node.get(isArrayAttribute))
                                ),
                        node.get(nameAttribute),
                        new HashSet<>(Arrays.asList("variable")),
                        scopeStack.peek()
                        )
                ));

        return defaultVisit(node, reports);
    }

    public Boolean dealWithMethodDecl(JmmNode node, List<Report> reports) {
        OurSymbol symbol = new OurSymbol(
                new Type(
                        node.getOptional(typeAttribute).orElse("void"),
                        Boolean.parseBoolean(node.getOptional(isArrayAttribute).orElse("false"))
                ),
                node.get(nameAttribute),
                new HashSet<>(Arrays.asList("method")),
                scopeStack.peek()
                );

        scopeStack.push(new OurScope(OurScope.ScopeEnum.FunctionVariable, symbol));
        reports.add(symbolTable.put(symbol));

        boolean result = defaultVisit(node, reports);

        scopeStack.pop();

        return result;
    }

    private static Boolean defaultVisit(JmmNode node, List<Report> kindCount) {
        return true;
    }

}
