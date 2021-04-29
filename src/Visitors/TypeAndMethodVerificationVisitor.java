import pt.up.fe.comp.jmm.JmmNode;
import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp.jmm.ast.JmmNodeImpl;
import pt.up.fe.comp.jmm.ast.PostorderJmmVisitor;
import pt.up.fe.comp.jmm.report.Report;
import pt.up.fe.comp.jmm.report.ReportType;
import pt.up.fe.comp.jmm.report.Stage;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

public class TypeAndMethodVerificationVisitor extends PostorderJmmVisitor<List<Report>, Boolean> {

    OurSymbolTable symbolTable;

    public TypeAndMethodVerificationVisitor(OurSymbolTable symbolTable){
        this.symbolTable = symbolTable;

        addVisit(Constants.terminalNodeName, this::dealWithTerminal);
        addVisit(Constants.binaryNodeName, this::dealWithBinary);
        addVisit(Constants.assignmentNodeName, this::dealWithAssignment);
        addVisit(Constants.arrayExprNodeName, this::dealWithArrayExpression);
        addVisit(Constants.notExprNodeName, this::dealWithNotExpr);
        addVisit(Constants.ifConditionNodeName, this::dealWithCondition);
        addVisit(Constants.whileConditionNodeName, this::dealWithCondition);
        addVisit(Constants.callExprNodeName, this::dealWithCallExpr);
        addVisit(Constants.returnNodeName, this::dealWithReturn);
        addVisit(Constants.propertyAccessNodeName, this::dealWithPropertyAccess);
        addVisit(Constants.methodDeclNodeName, this::dealWithMethDecl);
        setDefaultVisit(TypeAndMethodVerificationVisitor::defaultVisit);
    }

    private Boolean dealWithTerminal(JmmNode node, List<Report> reports){
        if (!node.get(Constants.typeAttribute).equals(Constants.identifierAttribute) &&
            !node.get(Constants.valueAttribute).equals(Constants.thisAttribute)) return defaultVisit(node, reports);

        var variableOpt = symbolTable.tryGettingSymbol(
                node.getAncestor(Constants.methodDeclNodeName).map(ancestorNode -> ancestorNode.get(Constants.nameAttribute)).orElse("this"),
                node.get(Constants.valueAttribute));


        if (variableOpt.isEmpty()){
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
            var variable = variableOpt.get();
            // Check if its being assigned
            if (node.getParent().getKind().equals(Constants.assignmentNodeName) && node.getParent().getChildren().get(0).equals(node))
                variable.setInitialized();

            if (!variable.isInitialized() && !variable.isParameter() && !variable.isClass()){ // If variable isn't parameter or class, must be initialized
                reports.add(new Report(
                        ReportType.WARNING,
                        Stage.SEMANTIC,
                        Integer.parseInt(node.get(Constants.lineAttribute)),
                        Integer.parseInt(node.get(Constants.columnAttribute)),
                        "Variable " + node.get(Constants.valueAttribute) + " must be initialized before being used"
                ));
                node.put(Constants.typeAttribute, Constants.error);
                node.put(Constants.arrayAttribute, Constants.error);
            }
            else {
                node.put(Constants.typeAttribute, variableOpt.get().getType().getName());
                node.put(Constants.arrayAttribute, String.valueOf(variableOpt.get().getType().isArray()));
            }
        }

        return defaultVisit(node, reports);
    }

    private Boolean dealWithBinary(JmmNode node, List<Report> reports){
        var childrenTypesOpt = NodeUtils.childrenTypes(node, reports);
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
        var childrenTypesOpt = NodeUtils.childrenTypes(node, reports);
        if (childrenTypesOpt.isEmpty()) return defaultVisit(node, reports);
        var childrenTypes = childrenTypesOpt.get();
        Type leftOperandType = childrenTypes.get(0), rightOperandType = childrenTypes.get(1);

        // An error occurred somewhere in the children, no longer analyses this expression
        if (leftOperandType.getName().equals(Constants.error) || rightOperandType.getName().equals(Constants.error)){
            node.put(Constants.typeAttribute, Constants.error);
            node.put(Constants.arrayAttribute, Constants.error);
            return defaultVisit(node, reports);
        }

        return checkChildrenAreOfSameType(node, reports, leftOperandType, rightOperandType) &&
                checkArrayDeclSizeIsInt(node, reports);
    }

    private Boolean dealWithArrayExpression(JmmNode node, List<Report> reports){
        var childrenTypesOpt = NodeUtils.childrenTypes(node, reports);
        if (childrenTypesOpt.isEmpty()) return defaultVisit(node, reports);

        var leftVarType = childrenTypesOpt.get().get(0);


        if (leftVarType.getName().equals(Constants.error))
            return defaultVisit(node ,reports);

        if (!leftVarType.isArray()) {
            reports.add(new Report(
                    ReportType.ERROR,
                    Stage.SEMANTIC,
                    Integer.parseInt(node.get("line")),
                    Integer.parseInt(node.get("column")),
                    "Array access requires type 'int[]' but got '" + leftVarType.getName() + "'"
            ));
        };

        var rightVarType = childrenTypesOpt.get().get(1);

        if (rightVarType.getName().equals(Constants.error))
            return defaultVisit(node ,reports);

        if (!rightVarType.getName().equals(Constants.intType)) {
            reports.add(new Report(
                    ReportType.ERROR,
                    Stage.SEMANTIC,
                    Integer.parseInt(node.get("line")),
                    Integer.parseInt(node.get("column")),
                    "Array access index requires type 'int' but got '" + rightVarType.getName() + "'"
            ));
        };

        return defaultVisit(node, reports);
    }

    private Boolean dealWithNotExpr(JmmNode node, List<Report> reports){
        var childrenTypesOpt = NodeUtils.childrenTypes(node, reports);
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
        if (node.getChildren().get(0).get(Constants.typeAttribute).equals(Constants.autoType)){
            node.getChildren().get(0).put(Constants.typeAttribute, Constants.booleanType);
        }

        var childrenTypesOpt = NodeUtils.childrenTypes(node, reports);
        if (childrenTypesOpt.isEmpty()) return defaultVisit(node, reports);
        var childrenTypes = childrenTypesOpt.get();
        Type operation = childrenTypes.get(0);

        if (!operation.getName().equals(Constants.booleanType)){
            reports.add(new Report(
                    ReportType.ERROR,
                    Stage.SEMANTIC,
                    Integer.parseInt(node.get("line")),
                    Integer.parseInt(node.get("column")),
                    "Condition requires type 'boolean' but got '" + operation.getName() + "'"
            ));
        }

        return defaultVisit(node, reports);
    }

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

        node.put(Constants.typeAttribute, Constants.booleanType);
        node.put(Constants.arrayAttribute, "false");
        return defaultVisit(node, reports);
    }

    private Boolean dealWithCallExpr(JmmNode node, List<Report> reports){
        var children = node.getChildren();
        var target = children.get(0);
        var argsList = children.get(1);

        var targetClassSymbolOpt = symbolTable.tryGettingSymbol(
                node.getAncestor(Constants.methodDeclNodeName).map(ancestorNode -> ancestorNode.get(Constants.nameAttribute)).orElse("this"),
                target.get(Constants.valueAttribute));

        var methodName = node.get(Constants.nameAttribute);
        var methodSymbolOpt = symbolTable.tryGettingSymbol(
             "this",
                methodName
        );

        var targetClass = targetClassSymbolOpt.isPresent() ?
                targetClassSymbolOpt.get().getType().getName() :
                target.get(Constants.valueAttribute);

        var methodTypeOpt = symbolTable.tryGettingSymbolType(
                targetClass,
                methodName);

        if ( targetClass.equals(Constants.thisAttribute) ||   // If method was called on this context ( e.g. this.Foo() || ThisClass a; a.Foo() )
                targetClass.equals(symbolTable.className) ){  // Or if it was called on this class' static context (e.g. ThisClass.Foo() )

            if (targetClass.equals(Constants.thisAttribute) && // This cannot be used in a static context
                    node.getAncestor(Constants.methodDeclNodeName).get().getOptional("static").isPresent()){
                reports.add(new Report(
                        ReportType.WARNING,
                        Stage.SEMANTIC,
                        Integer.parseInt(node.get("line")),
                        Integer.parseInt(node.get("column")),
                        "'this' cannot be used in a static context"
                ));
                node.put(Constants.typeAttribute, Constants.error);
                node.put(Constants.arrayAttribute, Constants.error);
            }

            else if (methodTypeOpt.isEmpty() && symbolTable.superName == null) { // If method name isn't found and there is no inheritance present
                reports.add(new Report(
                        ReportType.ERROR,
                        Stage.SEMANTIC,
                        Integer.parseInt(node.get("line")),
                        Integer.parseInt(node.get("column")),
                        "Cannot resolve method '" + methodName +
                                "' in class '" + symbolTable.className + "'"
                ));
                node.put(Constants.typeAttribute, Constants.error);
                node.put(Constants.arrayAttribute, Constants.error);
            }
            else if (target.get(Constants.valueAttribute).equals(symbolTable.className) && // If method was called on this class' static context
                    (methodSymbolOpt.isPresent() && !methodSymbolOpt.get().isStatic()) &&  // but function isn't static (e.g. public void Foo(); ThisClass.Foo() )
                    (!target.getKind().equals(Constants.newNodeName))){                    // or a new object (e.g. new ThisClass().Foo(); )

                reports.add(new Report(
                        ReportType.WARNING,
                        Stage.SEMANTIC,
                        Integer.parseInt(node.get("line")),
                        Integer.parseInt(node.get("column")),
                        "Non-static method '" + methodName + "' cannot be referenced from a static context"
                ));
                node.put(Constants.typeAttribute, Constants.error);
                node.put(Constants.arrayAttribute, Constants.error);
            }
            else if (symbolTable.superName != null){ // If there is inheritance present, assume method is defined there and assume type is correct
                node.put(Constants.typeAttribute, Constants.autoType);
                node.put(Constants.arrayAttribute, "false");
            }
            else { // If method name was found in this class
                List<Type> args = Arrays.asList(argsList.getChildren().stream().map(NodeUtils::extractTypeFromNode).toArray(Type[]::new));
                var method = symbolTable.getMethod(methodName, args, node, reports);
                if (method.isEmpty()) { // Check if method signature is correct (number and type of parameters)
                    node.put(Constants.typeAttribute, Constants.error);
                    node.put(Constants.arrayAttribute, Constants.error);
                }
                else { // Method signature is correct, success!
                    node.put(Constants.typeAttribute, method.get().getType().getName());
                    node.put(Constants.arrayAttribute, String.valueOf(method.get().getType().isArray()));
                }
            }
        }
        else { // If method isn't called in this context or own class context
            if (symbolTable.getImports().contains(targetClass) || symbolTable.superName.equals(targetClass)){ // If calling context is an imported class or the super class, assume method is defined there and assume type is correct
                node.put(Constants.typeAttribute, Constants.autoType);
                node.put(Constants.arrayAttribute, "false");
            }
            else { // If calling context isn't found
                reports.add(new Report(
                        ReportType.ERROR,
                        Stage.SEMANTIC,
                        Integer.parseInt(node.get("line")),
                        Integer.parseInt(node.get("column")),
                        "Cannot resolve class '" + targetClass + "'"
                ));
                node.put(Constants.typeAttribute, Constants.error);
                node.put(Constants.arrayAttribute, Constants.error);
            }
        }

        return defaultVisit(node, reports);
    }

    private Boolean dealWithReturn(JmmNode node, List<Report> reports) {
        var returningType = node.getChildren().size() > 0 ?
                node.getChildren().get(0).getOptional(Constants.typeAttribute).orElse(Constants.voidType) : Constants.voidType;

        var parentOpt = node.getAncestor(Constants.methodDeclNodeName);

        if (parentOpt.isEmpty()){
            System.out.println("Return has no method. How??");
        }
        else if (returningType.equals(Constants.autoType) || returningType.equals(Constants.error)){
            return defaultVisit(node, reports);
        }
        else if (!returningType.equals(parentOpt.get().get("type"))){
            reports.add(new Report(
                    ReportType.WARNING,
                    Stage.SEMANTIC,
                    Integer.parseInt(node.get(Constants.lineAttribute)),
                    Integer.parseInt(node.get(Constants.columnAttribute)),
                    "Return type mismatch; Required '" +
                            parentOpt.get().get("type") + "' but got '" +
                            returningType + "'"
            ));
        }

        return defaultVisit(node, reports);
    }

    private Boolean dealWithPropertyAccess(JmmNode node, List<Report> reports){
        var child = node.getChildren().get(0);
        if (child.get(Constants.arrayAttribute).equals("false")){
            reports.add(new Report(
                    ReportType.ERROR,
                    Stage.SEMANTIC,
                    Integer.parseInt(node.get("line")),
                    Integer.parseInt(node.get("column")),
                    "Cannot access property of type '" + child.get(Constants.typeAttribute) + "'"
            ));
            node.put(Constants.typeAttribute, Constants.error);
            node.put(Constants.arrayAttribute, Constants.error);
        }

        return defaultVisit(node, reports);
    }

    private Boolean dealWithMethDecl(JmmNode node, List<Report> reports) {
        // Ensures void methods have a return ; present
        AtomicBoolean hasReturn = new AtomicBoolean(false);
        node.getChildren().forEach(child -> {
            if (child.getKind().equals(Constants.returnNodeName)) hasReturn.set(true);
        });

        if (!hasReturn.get()){
            node.add(new JmmNodeImpl(Constants.returnNodeName), node.getNumChildren());
        }

        return defaultVisit(node, reports);
    }
    /**
     * Checks if array size given in instantiation is int (e.g. new int[true] not allowed)
     * @param node Binary node to get children from
     * @param reports Accumulated reports
     * @return
     */
    private boolean checkArrayDeclSizeIsInt(JmmNode node, List<Report> reports) {
        var rightOperand = node.getChildren().get(1);
        if (rightOperand.getKind().equals(Constants.newNodeName) &&
                rightOperand.get(Constants.typeAttribute).equals(Constants.intType) &&
                rightOperand.get(Constants.arrayAttribute).equals("true")) {

            var child = rightOperand.getChildren().get(0);
            if (child.get(Constants.typeAttribute).equals(Constants.intType) &&
                    child.get(Constants.arrayAttribute).equals("false")) return defaultVisit(node, reports);
            else {
                reports.add(new Report(
                        ReportType.ERROR,
                        Stage.SEMANTIC,
                        Integer.parseInt(node.get(Constants.lineAttribute)),
                        Integer.parseInt(node.get(Constants.columnAttribute)),
                        "Array must be declared with an 'int' size; Got '" +
                                child.get(Constants.typeAttribute) + "'"
                ));
                node.put(Constants.typeAttribute, Constants.error);
                node.put(Constants.arrayAttribute, Constants.error);
            }
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
        if (leftOperandType.getName().equals(Constants.autoType)) leftOperandType = rightOperandType;
        else if (rightOperandType.getName().equals(Constants.autoType)) rightOperandType = leftOperandType;

        if (!leftOperandType.equals(rightOperandType)) {
            reports.add(new Report(
                    ReportType.ERROR,
                    Stage.SEMANTIC,
                    Integer.parseInt(node.get(Constants.lineAttribute)),
                    Integer.parseInt(node.get(Constants.columnAttribute)),
                    "Type mismatch ('" +
                            leftOperandType.getName() + (leftOperandType.isArray()?"[]":"") +
                            "' != '" +
                            rightOperandType.getName() + (rightOperandType.isArray()?"[]":"") + "')"
            ));
            node.put(Constants.typeAttribute, Constants.error);
            node.put(Constants.arrayAttribute, Constants.error);
        }
        else {
            String commonType = leftOperandType.getName();
            boolean commonIsArray = leftOperandType.isArray();
            node.put(Constants.typeAttribute, commonType);
            node.put(Constants.arrayAttribute, String.valueOf(commonIsArray));
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

    private static Boolean defaultVisit(JmmNode node, List<Report> reports) {
        return true;
    }
}
