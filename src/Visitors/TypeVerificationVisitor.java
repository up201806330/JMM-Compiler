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

    OurSymbolTable symbolTable;

    public TypeVerificationVisitor(OurSymbolTable symbolTable){
        this.symbolTable = symbolTable;

        addVisit(Constants.terminalNodeName, this::dealWithTerminal);
        addVisit(Constants.binaryNodeName, this::dealWithBinary);
        addVisit(Constants.assignmentNodeName, this::dealWithAssignment);
        addVisit(Constants.arrayExprNodeName, this::dealWithArrayExpression);
        addVisit(Constants.notExprNodeName, this::dealWithNotExpr);
        addVisit(Constants.ifConditionNodeName, this::dealWithCondition);
        addVisit(Constants.whileConditionNodeName, this::dealWithCondition);
        setDefaultVisit(TypeVerificationVisitor::defaultVisit);
    }

    private Boolean dealWithTerminal(JmmNode node, List<Report> reports){
        if (!node.get(Constants.typeAttribute).equals(Constants.identifierAttribute)) return defaultVisit(node, reports);

        var variableTypeOptional = symbolTable.getLocalVariableTypeIfItsDeclared(
                node.getAncestor(Constants.methodDeclNodeName).map(ancestorNode -> ancestorNode.get(Constants.nameAttribute)).orElse(null),
                node.get(Constants.valueAttribute));

        if (variableTypeOptional.isEmpty()){
            reports.add(new Report(
                    ReportType.WARNING,
                    Stage.SEMANTIC,
                    Integer.parseInt(node.get(Constants.lineAttribute)),
                    Integer.parseInt(node.get(Constants.columnAttribute)),
                    "Variable " + node.get(Constants.valueAttribute) + " must be declared before being used"
            ));
            node.put(Constants.typeAttribute, Constants.error);
            node.put(Constants.arrayAttribute, Constants.error);
        }
        else {
            node.put(Constants.typeAttribute, variableTypeOptional.get().getName());
            node.put(Constants.arrayAttribute, String.valueOf(variableTypeOptional.get().isArray()));
        }

        return defaultVisit(node, reports);
    }

    private Boolean dealWithBinary(JmmNode node, List<Report> reports){
        var childrenTypesOpt = childrenTypes(node, reports);
        if (childrenTypesOpt.isEmpty()) return defaultVisit(node, reports);
        var childrenTypes = childrenTypesOpt.get();
        Type leftOperandType = childrenTypes.get(0), rightOperandType = (childrenTypes.size() > 1 ? childrenTypes.get(1) : null);

        String binaryOperation = node.get(Constants.valueAttribute);

        // An error occurred somewhere in the children, no longer analyses this expression
        if (leftOperandType.getName().equals(Constants.error) || (rightOperandType != null && rightOperandType.getName().equals(Constants.error))){
            node.put(Constants.typeAttribute, Constants.error);
            node.put(Constants.arrayAttribute, Constants.error);
            return defaultVisit(node, reports);
        }

        return checkTheresNoArrayType(node, reports, leftOperandType, rightOperandType) &&
        checkChildrenAreOfSameType(node, reports, leftOperandType, rightOperandType) &&
        ((binaryOperation.equals(Constants.andExpression)) ?
                dealWithBooleanExpr(node, reports, leftOperandType, rightOperandType) : true) &&
        ((binaryOperation.equals(Constants.lessThanExpression) ?
                dealWithLessThanExpr(node, reports, leftOperandType, rightOperandType): true));
    }

    private Boolean dealWithAssignment(JmmNode node, List<Report> reports){
        var childrenTypesOpt = childrenTypes(node, reports);
        if (childrenTypesOpt.isEmpty()) return defaultVisit(node, reports);
        var childrenTypes = childrenTypesOpt.get();
        Type leftOperandType = childrenTypes.get(0), rightOperandType = childrenTypes.get(1);

        // An error occurred somewhere in the children, no longer analyses this expression
        if (leftOperandType.getName().equals(Constants.error) || rightOperandType.getName().equals(Constants.error)){
            node.put(Constants.typeAttribute, Constants.error);
            node.put(Constants.arrayAttribute, Constants.error);
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
                    "Array access requires type: 'int[]' ; Got '" + leftVarType.getName() + "'"
            ));
        };

        var rightVarType = childrenTypesOpt.get().get(1);

        if (!rightVarType.getName().equals(Constants.intType)) {
            reports.add(new Report(
                    ReportType.WARNING,
                    Stage.SEMANTIC,
                    Integer.parseInt(node.get("line")),
                    Integer.parseInt(node.get("column")),
                    "Array access index requires type: 'int' ; Got '" + rightVarType.getName() + "'"
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
        if (rightOperandType.getName().equals(Constants.error)){
            node.put(Constants.typeAttribute, Constants.error);
            node.put(Constants.arrayAttribute, Constants.error);
            return defaultVisit(node, reports);
        }

        dealWithBooleanExpr(node, reports, null, rightOperandType);

        return defaultVisit(node, reports);
    }

    private Boolean dealWithLessThanExpr(JmmNode node, List<Report> reports, Type leftOperandType, Type rightOperandType){
        if (!node.get(Constants.typeAttribute).equals(Constants.intType)){
            reports.add(new Report(
                    ReportType.ERROR,
                    Stage.SEMANTIC,
                    Integer.parseInt(node.get(Constants.lineAttribute)),
                    Integer.parseInt(node.get(Constants.columnAttribute)),
                    "Operator '" + node.get(Constants.valueAttribute) +
                            "' cannot be applied to '" +
                            leftOperandType.getName() + "', '" +
                            rightOperandType.getName() + "'"
            ));
        }
        else {
            node.put(Constants.typeAttribute, Constants.booleanType);
        }
        return defaultVisit(node, reports);
    }

    private Boolean dealWithCondition(JmmNode node, List<Report> reports){
        var childrenTypesOpt = childrenTypes(node, reports);
        if (childrenTypesOpt.isEmpty()) return defaultVisit(node, reports);
        var childrenTypes = childrenTypesOpt.get();
        Type operation = childrenTypes.get(0);

        if (!operation.getName().equals(Constants.booleanType)){
            reports.add(new Report(
                    ReportType.ERROR,
                    Stage.SEMANTIC,
                    Integer.parseInt(node.get("line")),
                    Integer.parseInt(node.get("column")),
                    "Condition requires type: 'boolean' ; Got '" + operation.getName() + "'"
            ));
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
                !leftOperandType.getName().equals(Constants.booleanType) || !rightOperandType.getName().equals(Constants.booleanType) :
                !rightOperandType.getName().equals(Constants.booleanType)){

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
            node.put(Constants.typeAttribute, Constants.error);
            node.put(Constants.arrayAttribute, Constants.error);
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
                    Integer.parseInt(node.get(Constants.lineAttribute)),
                    Integer.parseInt(node.get(Constants.columnAttribute)),
                    "Type mismatch (" +
                            leftOperandType.getName() + (leftOperandType.isArray()?"[]":"") +
                            " != " +
                            rightOperandType.getName() + (rightOperandType.isArray()?"[]":"") + ")"
            ));
            node.put(Constants.typeAttribute, Constants.error);
            node.put(Constants.arrayAttribute, Constants.error);
        }
        else {
            node.put(Constants.typeAttribute, leftOperandType.getName());
            node.put(Constants.arrayAttribute, String.valueOf(leftOperandType.isArray()));
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
                    Integer.parseInt(node.get(Constants.lineAttribute)),
                    Integer.parseInt(node.get(Constants.columnAttribute)),
                    "Operator '" + node.get(Constants.valueAttribute) +
                            "' cannot be applied to '" +
                            leftOperandType.getName() + (leftOperandType.isArray() ? "[]" : "") + "', '" +
                            rightOperandType.getName() + (rightOperandType.isArray() ? "[]" : "") + "'"
            ));
            node.put(Constants.typeAttribute, Constants.error);
            node.put(Constants.arrayAttribute, Constants.error);
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
        if (result.size() != 2 && node.getKind().equals(Constants.binaryNodeName)) {
            reports.add(new Report(
                    ReportType.WARNING,
                    Stage.SEMANTIC,
                    Integer.parseInt(node.get(Constants.lineAttribute)),
                    Integer.parseInt(node.get(Constants.columnAttribute)),
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
        return new Type(node.get(Constants.typeAttribute), Boolean.parseBoolean(node.get(Constants.arrayAttribute)));
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
