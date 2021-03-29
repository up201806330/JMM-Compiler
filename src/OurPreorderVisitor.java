import pt.up.fe.comp.jmm.JmmNode;
import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.ast.PreorderJmmVisitor;
import pt.up.fe.comp.jmm.report.Report;
import pt.up.fe.specs.util.utilities.StringLines;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Counts the occurences of each node kind.
 * 
 * @author JBispo
 *
 */
public class OurPreorderVisitor extends PreorderJmmVisitor<List<Report>, Boolean> {

    private final String identifierType = "Identifier";
    private final String identifierAttribute = "id";

    private final String typeType = "Identifier";
    private final String typeAttribute = "type";

    SymbolTable symbolTable;

    public OurPreorderVisitor(SymbolTable symbolTable) {
        addVisit(typeType, this::dealWithType);
        setDefaultVisit(this::defaultVisit);

        this.symbolTable = symbolTable;
    }

    public Boolean dealWithType(JmmNode node, List<Report> reports) {
        if (node.get(typeAttribute).equals("intArray")) {
            node.put("isArray", "true");
            node.put("type", "int");
        }
        else
            node.put("isArray", "false");

        return defaultVisit(node, reports);
    }

    private Boolean defaultVisit(JmmNode node, List<Report> space) {

        return true;
    }
}
