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
        OurSymbol symbol = new OurSymbol(
                node,
                new HashSet<>(Arrays.asList("variable")),
                scopeStack.peek());

        Report insertionError = symbolTable.put(symbol);
        if (insertionError != null) reports.add(insertionError);

        return defaultVisit(node, reports);
    }

    public Boolean dealWithMethodDecl(JmmNode node, List<Report> reports) {
        OurSymbol symbol = new OurSymbol(
                node,
                new HashSet<>(Arrays.asList("method")),
                scopeStack.peek());

        scopeStack.push(new OurScope(OurScope.ScopeEnum.FunctionVariable, symbol));

        Report insertionError = symbolTable.put(symbol);
        if (insertionError != null) reports.add(insertionError);

        boolean result = defaultVisit(node, reports);

        scopeStack.pop();

        return result;
    }

    private static Boolean defaultVisit(JmmNode node, List<Report> kindCount) {
        return true;
    }

}
