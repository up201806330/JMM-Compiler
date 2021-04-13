import pt.up.fe.comp.jmm.JmmNode;
import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp.jmm.ast.PostorderJmmVisitor;
import pt.up.fe.comp.jmm.report.Report;
import pt.up.fe.comp.jmm.report.ReportType;
import pt.up.fe.comp.jmm.report.Stage;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class TypeVerificationVisitor extends PostorderJmmVisitor<List<Report>, Boolean> {

    private static final String terminalNodeName = "Terminal";
    private static final String binaryNodeName = "Binary";
    private static final String assignmentNodeName = "Assignment";
    private static final String arrayExprNodeName = "ArrayExpression";
    private static final String notExprNodeName = "NotExpression";
    private static final String methodDeclNodeName = "MethodDeclaration";

    OurSymbolTable symbolTable;

    public TypeVerificationVisitor(OurSymbolTable symbolTable){
        this.symbolTable = symbolTable;

        addVisit(terminalNodeName, this::dealWithTerminal);
        addVisit(binaryNodeName, this::dealWithBinary);
        addVisit(assignmentNodeName, this::dealWithAssignment);
        addVisit(arrayExprNodeName, this::dealWithArrayExpression);
        addVisit(notExprNodeName, this::dealWithNotExpr);
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
        var childrenTypesOpt = childrenTypes(node, reports);
        if (childrenTypesOpt.isEmpty()) return defaultVisit(node, reports);
        var childrenTypes = childrenTypesOpt.get();
        Type leftOperandType = childrenTypes.get(0), rightOperandType = (childrenTypes.size() > 1 ? childrenTypes.get(1) : null);

        String binaryOperation = node.get("value");

        // An error occurred somewhere in the children, no longer analyses this expression
        if (leftOperandType.getName().equals("error") || (rightOperandType != null && rightOperandType.getName().equals("error"))){
            node.put("type", "error");
            node.put("isArray", "error");
            return defaultVisit(node, reports);
        }

        return checkTheresNoArrayType(node, reports, leftOperandType, rightOperandType) &&
        checkChildrenAreOfSameType(node, reports, leftOperandType, rightOperandType) &&
        ((binaryOperation.equals("&&")) ?
                dealWithBooleanExpr(node, reports, leftOperandType, rightOperandType) : true) &&
        ((binaryOperation.equals("<") ?
                dealWithLessThanExpr(node, reports, leftOperandType, rightOperandType): true));
    }

    private Boolean dealWithAssignment(JmmNode node, List<Report> reports){
        var childrenTypesOpt = childrenTypes(node, reports);
        if (childrenTypesOpt.isEmpty()) return defaultVisit(node, reports);
        var childrenTypes = childrenTypesOpt.get();
        Type leftOperandType = childrenTypes.get(0), rightOperandType = childrenTypes.get(1);

        // An error occurred somewhere in the children, no longer analyses this expression
        if (leftOperandType.getName().equals("error") || rightOperandType.getName().equals("error")){
            node.put("type", "error");
            node.put("isArray", "error");
            return defaultVisit(node, reports);
        }

        return checkChildrenAreOfSameType(node, reports, leftOperandType, rightOperandType);
    }

    private Boolean dealWithArrayExpression(JmmNode node, List<Report> reports){
        var childrenTypesOpt = childrenTypes(node, reports);
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

    private Boolean dealWithNotExpr(JmmNode node, List<Report> reports){
        var childrenTypesOpt = childrenTypes(node, reports);
        if (childrenTypesOpt.isEmpty()) return defaultVisit(node, reports);
        var childrenTypes = childrenTypesOpt.get();
        Type rightOperandType = childrenTypes.get(0);

        // An error occurred somewhere in the children, no longer analyses this expression
        if (rightOperandType.getName().equals("error")){
            node.put("type", "error");
            node.put("isArray", "error");
            return defaultVisit(node, reports);
        }

        dealWithBooleanExpr(node, reports, null, rightOperandType);

        return defaultVisit(node, reports);
    }

    private Boolean dealWithLessThanExpr(JmmNode node, List<Report> reports, Type leftOperandType, Type rightOperandType){
        System.out.println(node.get("type"));
        if (!node.get("type").equals("int")){
            reports.add(new Report(
                    ReportType.ERROR,
                    Stage.SEMANTIC,
                    Integer.parseInt(node.get("line")),
                    Integer.parseInt(node.get("column")),
                    "Operator '" + node.get("value") +
                            "' cannot be applied to '" +
                            leftOperandType.getName() + "', '" +
                            rightOperandType.getName() + "'"
            ));
        }
        else {
            node.put("type", "bool");
        }
        return defaultVisit(node, reports);
    }

    /**
     * Checks if both operands (or if there is only one, the right one) are of type bool
     *
     * @param node Node to get children from
     * @param reports Accumulated reports
     * @param leftOperandType Type object
     * @param rightOperandType Type object
     * @return
     */
    private Boolean dealWithBooleanExpr(JmmNode node, List<Report> reports, Type leftOperandType, Type rightOperandType){
        if (leftOperandType != null ?
                !leftOperandType.getName().equals("bool") || !rightOperandType.getName().equals("bool") :
                !rightOperandType.getName().equals("bool")){

            System.out.println(node.get("line"));
            reports.add(new Report(
                    ReportType.ERROR,
                    Stage.SEMANTIC,
                    Integer.parseInt(node.get("line")),
                    Integer.parseInt(node.get("column")),
                    "Operator '" + node.get("value") +
                            "' cannot be applied to '" +
                            (leftOperandType != null ? leftOperandType.getName() + "', '" : "") +
                            rightOperandType.getName() + "'"
            ));
            node.put("type", "error");
            node.put("isArray", "error");
        }

        return defaultVisit(node, reports);
    }

    /**
     * Checks if children are of the same type, otherwise creating an error report
     * @param node Binary node to get children from
     * @param reports Accumulated reports
     * @param leftOperandType Type object
     * @param rightOperandType Type object
     * @return
     */
    private Boolean checkChildrenAreOfSameType(JmmNode node, List<Report> reports, Type leftOperandType, Type rightOperandType){
        if (!leftOperandType.equals(rightOperandType)) {
            reports.add(new Report(
                    ReportType.ERROR,
                    Stage.SEMANTIC,
                    Integer.parseInt(node.get("line")),
                    Integer.parseInt(node.get("column")),
                    "Type mismatch (" +
                            leftOperandType.getName() + (leftOperandType.isArray()?"[]":"") +
                            " != " +
                            rightOperandType.getName() + (rightOperandType.isArray()?"[]":"") + ")"
            ));
            node.put("type", "error");
            node.put("isArray", "error");
        }
        else {
            node.put("type", leftOperandType.getName());
            node.put("isArray", String.valueOf(leftOperandType.isArray()));
        }

        return defaultVisit(node, reports);
    }

    /**
     * Checks if both operands aren't arrays, as they can't be used directly for arithmetic operations
     * @param node Binary node to get children from
     * @param reports Accumulated reports
     * @return
     */
    private Boolean checkTheresNoArrayType(JmmNode node, List<Report> reports, Type leftOperandType, Type rightOperandType){
        if (leftOperandType.isArray() || rightOperandType.isArray()) {
            reports.add(new Report(
                    ReportType.ERROR,
                    Stage.SEMANTIC,
                    Integer.parseInt(node.get("line")),
                    Integer.parseInt(node.get("column")),
                    "Operator '" + node.get("value") +
                            "' cannot be applied to '" +
                            leftOperandType.getName() + (leftOperandType.isArray() ? "[]" : "") + "', '" +
                            rightOperandType.getName() + (rightOperandType.isArray() ? "[]" : "") + "'"
            ));
            node.put("type", "error");
            node.put("isArray", "error");
        }

        return defaultVisit(node, reports);
    }

    /**
     * Gets the children of a node
     * @param node node to get children from
     * @param reports Accumulated reports
     * @return Optional of list of child nodes. If node is Binary and there are not 2 child nodes, Optional.empty()
     */
    private Optional<List<JmmNode>> getChildren(JmmNode node, List<Report> reports) {
        List<JmmNode> result = node.getChildren();
        if (result.size() != 2 && node.getKind().equals(binaryNodeName)) {
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
     * @param node Node whose type will be extracted
     * @return A Type object
     */
    private Type extractTypeFromNode(JmmNode node){
        return new Type(node.get("type"), Boolean.parseBoolean(node.get("isArray")));
    }

    /**
     *
     * @param node Node to get children from
     * @param reports Accumulated reports
     * @return Optional of list of Types, or if there are no children, Optional.empty()
     */
    private Optional<List<Type>> childrenTypes(JmmNode node, List<Report> reports){
        List<JmmNode> children;
        var childrenOptional = getChildren(node, reports);
        if (childrenOptional.isEmpty()) return Optional.empty();
        else children = childrenOptional.get();

        List<Type> result = new ArrayList<>();
        result.add(extractTypeFromNode(children.get(0)));
        if (children.size() > 1) result.add(extractTypeFromNode(children.get(1)));
        return Optional.of(result);
    }

    private static Boolean defaultVisit(JmmNode node, List<Report> reports) {
        return true;
    }
}
