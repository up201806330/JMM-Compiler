import pt.up.fe.comp.jmm.JmmNode;
import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp.jmm.ast.PostorderJmmVisitor;
import pt.up.fe.comp.jmm.report.Report;
import pt.up.fe.comp.jmm.report.ReportType;
import pt.up.fe.comp.jmm.report.Stage;

import java.util.List;
import java.util.Optional;

public class TypeVerificationVisitor extends PostorderJmmVisitor<List<Report>, Boolean> {

    private static final String terminalNodeName = "Terminal";
    private static final String binaryNodeName = "Binary";
    private static final String assignmentNodeName = "Assignment";
    private static final String arrayExprNodeName = "ArrayExpression";
    private static final String methodDeclNodeName = "MethodDeclaration";

    OurSymbolTable symbolTable;

    public TypeVerificationVisitor(OurSymbolTable symbolTable){
        this.symbolTable = symbolTable;

        addVisit(terminalNodeName, this::dealWithTerminal);
        addVisit(binaryNodeName, this::dealWithBinary);
        addVisit(assignmentNodeName, this::checkChildrenAreOfSameType);
        addVisit(arrayExprNodeName, this::dealWithArrayExpression);
        setDefaultVisit(TypeVerificationVisitor::defaultVisit);
    }

    private Boolean dealWithTerminal(JmmNode node, List<Report> reports){
        if (!node.get("type").equals("identifier")) return defaultVisit(node, reports);

        var variableTypeOptional = symbolTable.getLocalVariableTypeIfItsDeclared(
                node.getAncestor(methodDeclNodeName).map(ancestorNode -> ancestorNode.get("name")).orElse(null),
                node.get("value"));

        if (variableTypeOptional.isEmpty()){
            reports.add(new Report(
                    ReportType.WARNING,
                    Stage.SEMANTIC,
                    Integer.parseInt(node.get("line")),
                    Integer.parseInt(node.get("column")),
                    "Variable " + node.get("value") + " must be declared before being used"
            ));
            node.put("type", "error");
            node.put("isArray", "error");
        }
        else {
            node.put("type", variableTypeOptional.get().getName());
            node.put("isArray", String.valueOf(variableTypeOptional.get().isArray()));
        }

        return defaultVisit(node, reports);
    }

    private Boolean dealWithBinary(JmmNode node, List<Report> reports){
        return checkTheresNoArrayType(node, reports) &&
        checkChildrenAreOfSameType(node, reports);
    }

    private Boolean dealWithArrayExpression(JmmNode node, List<Report> reports){
        var childrenTypesOpt = binaryChildrenTypes(node, reports);
        if (childrenTypesOpt.isEmpty()) return defaultVisit(node, reports);

        var leftVarType = childrenTypesOpt.get().get(0);

        if (!leftVarType.isArray()) {
            reports.add(new Report(
                    ReportType.WARNING,
                    Stage.SEMANTIC,
                    Integer.parseInt(node.get("line")),
                    Integer.parseInt(node.get("column")),
                    "Array type expected; found: '" + leftVarType.getName() + "'"
            ));
        };

        return defaultVisit(node, reports);
    }

    private Boolean checkChildrenAreOfSameType(JmmNode node, List<Report> reports){
        var childrenTypesOpt = binaryChildrenTypes(node, reports);
        if (childrenTypesOpt.isEmpty()) return defaultVisit(node, reports);

        var leftVarType = childrenTypesOpt.get().get(0);
        var rightVarType = childrenTypesOpt.get().get(1);

        if (leftVarType.getName().equals("error") || rightVarType.getName().equals("error")){
            node.put("type", "error");
            node.put("isArray", "error");
            return defaultVisit(node, reports);
        }

        if (!leftVarType.equals(rightVarType)) {
            reports.add(new Report(
                    ReportType.ERROR,
                    Stage.SEMANTIC,
                    Integer.parseInt(node.get("line")),
                    Integer.parseInt(node.get("column")),
                    "Type mismatch (" +
                            leftVarType.getName() + (leftVarType.isArray()?"[]":"") +
                            " != " +
                            rightVarType.getName() + (rightVarType.isArray()?"[]":"") + ")"
            ));
            node.put("type", "error");
            node.put("isArray", "error");
        }
        else {
            node.put("type", leftVarType.getName());
            node.put("isArray", String.valueOf(leftVarType.isArray()));
        }

        return defaultVisit(node, reports);
    }

    private Boolean checkTheresNoArrayType(JmmNode node, List<Report> reports){
        var childrenTypesOpt = binaryChildrenTypes(node, reports);
        if (childrenTypesOpt.isEmpty()) return defaultVisit(node, reports);

        var leftVarType = childrenTypesOpt.get().get(0);
        var rightVarType = childrenTypesOpt.get().get(1);

        if (leftVarType.isArray() || rightVarType.isArray()) {
            reports.add(new Report(
                    ReportType.ERROR,
                    Stage.SEMANTIC,
                    Integer.parseInt(node.get("line")),
                    Integer.parseInt(node.get("column")),
                    "Operator '" + node.get("value") +
                            "' cannot be applied to '" +
                            leftVarType.getName() + (leftVarType.isArray() ? "[]" : "") + "', '" +
                            rightVarType.getName() + (rightVarType.isArray() ? "[]" : "") + "'"
            ));
            node.put("type", "error");
            node.put("isArray", "error");
        }

        return defaultVisit(node, reports);
    }

    /**
     * Gets the children of a node, assuring there are only 2. Else, creates a report and returns Optional.empty()
     * @param node Binary node to get children from
     * @param reports Accumulated reports
     * @return List of child nodes if there are only 2, else Optional.empty()
     */
    private Optional<List<JmmNode>> getBinaryChildren(JmmNode node, List<Report> reports) {
        List<JmmNode> result = node.getChildren();
        if (result.size() != 2) {
            reports.add(new Report(
                    ReportType.WARNING,
                    Stage.SEMANTIC,
                    Integer.parseInt(node.get("line")),
                    Integer.parseInt(node.get("column")),
                    "Binary node doesn't have 2 children. Something is wrong in syntactic phase"
            ));
            return Optional.empty();
        }
        return Optional.of(result);
    }

    /**
     * Create type from node with type and isArray parameters already set. Assumes node has already been filled in with these
     * parameters in a previous search
     * @param node node whose type will be extracted
     * @return a Type object
     */
    private Type extractTypeFromNode(JmmNode node){
        return new Type(node.get("type"), Boolean.parseBoolean(node.get("isArray")));
    }

    private Optional<List<Type>> binaryChildrenTypes(JmmNode node, List<Report> reports){
        List<JmmNode> children;
        var childrenOptional = getBinaryChildren(node, reports);
        if (childrenOptional.isEmpty()) return Optional.empty();
        else children = childrenOptional.get();

        return Optional.of(List.of(extractTypeFromNode(children.get(0)), extractTypeFromNode(children.get(1))));
    }

    private static Boolean defaultVisit(JmmNode node, List<Report> reports) {
        return true;
    }
}
