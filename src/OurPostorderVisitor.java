import pt.up.fe.comp.jmm.JmmNode;
import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp.jmm.ast.PostorderJmmVisitor;
import pt.up.fe.comp.jmm.report.Report;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

/**
 * Counts the occurrences of each node kind.
 * 
 * @author JBispo
 *
 */
public class OurPostorderVisitor extends PostorderJmmVisitor<List<Report>, Boolean> {

    private final String methodDeclNodeName = "MethodDeclaration";
    private final String varDeclNodeName = "VarDeclaration";
    private final String typeNodeName = "Type";
    private final String typeAttribute = "type";
    private final String nameAttribute = "name";
    private final String isArrayAttribute = "isArray";


    OurScopedSymbolTable symbolTable;

    public OurPostorderVisitor(OurScopedSymbolTable symbolTable) {
        addVisit(typeNodeName, this::dealWithType);
        addVisit(varDeclNodeName, this::dealWithVarDecl);
        addVisit(methodDeclNodeName, this::dealWithMethodDecl);
        setDefaultVisit(OurPostorderVisitor::defaultVisit);

        this.symbolTable = symbolTable;
    }

    public Boolean dealWithType(JmmNode node, List<Report> reports) {
        if (node.get(typeAttribute).equals("intArray")) {
            node.put(isArrayAttribute, "true");
            node.put(typeAttribute, "int");
        }
        else
            node.put(isArrayAttribute, "false");

        node.getParent().put(typeAttribute, node.get(typeAttribute));
        node.getParent().put(isArrayAttribute, node.get(isArrayAttribute));
        return defaultVisit(node, reports);
    }

    public Boolean dealWithVarDecl(JmmNode node, List<Report> reports) {
        symbolTable.put(
                new OurSymbol(
                        new Type(node.get(typeAttribute), Boolean.parseBoolean(node.get(isArrayAttribute))),
                        node.get(nameAttribute),
                        new HashSet<>(Arrays.asList("variable"))
                ));

        return defaultVisit(node, reports);
    }

    public Boolean dealWithMethodDecl(JmmNode node, List<Report> reports) {
        symbolTable.put(
                new OurSymbol(
                        new Type(
                                node.getOptional(typeAttribute).orElse("void"),
                                Boolean.parseBoolean(node.getOptional(isArrayAttribute).orElse("false"))),
                        node.get(nameAttribute),
                        new HashSet<>(Arrays.asList("method"))
                ));

        return defaultVisit(node, reports);
    }

    private static Boolean defaultVisit(JmmNode node, List<Report> kindCount) {
        return true;
    }

}
