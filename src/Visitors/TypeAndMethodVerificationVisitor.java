import pt.up.fe.comp.jmm.JmmNode;
import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp.jmm.ast.JmmNodeImpl;
import pt.up.fe.comp.jmm.ast.PostorderJmmVisitor;
import pt.up.fe.comp.jmm.report.Report;
import pt.up.fe.comp.jmm.report.ReportType;
import pt.up.fe.comp.jmm.report.Stage;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

public class TypeAndMethodVerificationVisitor extends PostorderJmmVisitor<List<Report>, Boolean> {

    OurSymbolTable symbolTable;

    public TypeAndMethodVerificationVisitor(OurSymbolTable symbolTable){
        this.symbolTable = symbolTable;

        addVisit(Consts.terminalNodeName, this::dealWithTerminal);
        addVisit(Consts.binaryNodeName, this::dealWithBinary);
        addVisit(Consts.assignmentNodeName, this::dealWithAssignment);
        addVisit(Consts.arrayExprNodeName, this::dealWithArrayExpression);
        addVisit(Consts.notExprNodeName, this::dealWithNotExpr);
        addVisit(Consts.ifConditionNodeName, this::dealWithCondition);
        addVisit(Consts.whileConditionNodeName, this::dealWithCondition);
        addVisit(Consts.callExprNodeName, this::dealWithCallExpr);
        addVisit(Consts.returnNodeName, this::dealWithReturn);
        addVisit(Consts.propertyAccessNodeName, this::dealWithPropertyAccess);
        addVisit(Consts.methodDeclNodeName, this::dealWithMethDecl);
        addVisit(Consts.varDeclNodeName, this::dealWithReservedNames);
        setDefaultVisit(TypeAndMethodVerificationVisitor::defaultVisit);
    }

    private Boolean dealWithTerminal(JmmNode node, List<Report> reports){
        if (!node.get(Consts.typeAttribute).equals(Consts.identifierAttribute) &&
            !node.get(Consts.valueAttribute).equals(Consts.thisAttribute)) return defaultVisit(node, reports);

        var variableOpt = symbolTable.tryGettingSymbol(
                node.getAncestor(Consts.methodDeclNodeName).map(ancestorNode -> ancestorNode.get(Consts.nameAttribute)).orElse("this"),
                node.get(Consts.valueAttribute));

        var methodOpt = node.getAncestor(Consts.methodDeclNodeName);
        var methodName = (methodOpt.isPresent() ? methodOpt.get().get(Consts.nameAttribute) : "");
        var methodSymOpt = symbolTable.tryGettingSymbol(Consts.thisAttribute, methodName);

        dealWithReservedNames(node, null);

        if (variableOpt.isEmpty()){
            reports.add(new Report(
                    ReportType.ERROR,
                    Stage.SEMANTIC,
                    Integer.parseInt(node.get(Consts.lineAttribute)),
                    Integer.parseInt(node.get(Consts.columnAttribute)),
                    "Variable " + node.get(Consts.valueAttribute) + " must be declared before being used"
            ));
            node.put(Consts.typeAttribute, Consts.error);
            node.put(Consts.arrayAttribute, Consts.error);
        }
        else {
            var variable = variableOpt.get();
            // Check if its being assigned
            if (node.getParent().getKind().equals(Consts.assignmentNodeName) && node.getParent().getChildren().get(0).equals(node))
                variable.setInitialized();

            if (!variable.isInitialized() && !variable.isParameter() && !variable.isClass()){ // If variable isn't parameter or class, must be initialized
                reports.add(new Report(
                        ReportType.WARNING,
                        Stage.SEMANTIC,
                        Integer.parseInt(node.get(Consts.lineAttribute)),
                        Integer.parseInt(node.get(Consts.columnAttribute)),
                        "Variable '" + node.get(Consts.valueAttribute) + "' must be initialized before being used"
                ));
                node.put(Consts.typeAttribute, Consts.error);
                node.put(Consts.arrayAttribute, Consts.error);
            }
            else if (methodSymOpt.isPresent() && methodSymOpt.get().isStatic() && variable.isField()) {
                reports.add(new Report(
                        ReportType.WARNING,
                        Stage.SEMANTIC,
                        Integer.parseInt(node.get(Consts.lineAttribute)),
                        Integer.parseInt(node.get(Consts.columnAttribute)),
                        "Non-static field '" + node.get(Consts.valueAttribute) + "' cannot be referenced from a static context"
                ));
                node.put(Consts.typeAttribute, Consts.error);
                node.put(Consts.arrayAttribute, Consts.error);
            }
            else {
                node.put(Consts.typeAttribute, variableOpt.get().getType().getName());
                node.put(Consts.arrayAttribute, String.valueOf(variableOpt.get().getType().isArray()));
            }
        }

        return defaultVisit(node, reports);
    }

    private Boolean dealWithBinary(JmmNode node, List<Report> reports){
        var childrenTypesOpt = NodeUtils.childrenTypes(node, reports);
        if (childrenTypesOpt.isEmpty()) return defaultVisit(node, reports);
        var childrenTypes = childrenTypesOpt.get();
        Type leftOperandType = childrenTypes.get(0), rightOperandType = (childrenTypes.size() > 1 ? childrenTypes.get(1) : null);

        String binaryOperation = node.get(Consts.valueAttribute);

        // An error occurred somewhere in the children, no longer analyses this expression
        if (leftOperandType.getName().equals(Consts.error) || (rightOperandType != null && rightOperandType.getName().equals(Consts.error))){
            node.put(Consts.typeAttribute, Consts.error);
            node.put(Consts.arrayAttribute, Consts.error);
            return defaultVisit(node, reports);
        }

        return checkTheresNoArrayType(node, reports, leftOperandType, rightOperandType) &&
        checkChildrenAreOfSameType(node, reports, leftOperandType, rightOperandType) &&
        ((binaryOperation.equals(Consts.andExpression)) ?
                dealWithBooleanExpr(node, reports, leftOperandType, rightOperandType) : true) &&
        ((binaryOperation.equals(Consts.lessThanExpression) ?
                dealWithLessThanExpr(node, reports, leftOperandType, rightOperandType): true));
    }

    private Boolean dealWithAssignment(JmmNode node, List<Report> reports){
        var childrenTypesOpt = NodeUtils.childrenTypes(node, reports);
        if (childrenTypesOpt.isEmpty()) return defaultVisit(node, reports);
        var childrenTypes = childrenTypesOpt.get();
        Type leftOperandType = childrenTypes.get(0), rightOperandType = childrenTypes.get(1);

        // An error occurred somewhere in the children, no longer analyses this expression
        if (leftOperandType.getName().equals(Consts.error) || rightOperandType.getName().equals(Consts.error)){
            node.put(Consts.typeAttribute, Consts.error);
            node.put(Consts.arrayAttribute, Consts.error);
            return defaultVisit(node, reports);
        }

        return checkChildrenAreOfSameType(node, reports, leftOperandType, rightOperandType) &&
                checkArrayDeclSizeIsInt(node, reports);
    }

    private Boolean dealWithArrayExpression(JmmNode node, List<Report> reports){
        var childrenTypesOpt = NodeUtils.childrenTypes(node, reports);
        if (childrenTypesOpt.isEmpty()) return defaultVisit(node, reports);

        var leftVarType = childrenTypesOpt.get().get(0);


        if (leftVarType.getName().equals(Consts.error))
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

        if (rightVarType.getName().equals(Consts.error))
            return defaultVisit(node ,reports);

        if (!rightVarType.getName().equals(Consts.intType)) {
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
        if (rightOperandType.getName().equals(Consts.error)){
            node.put(Consts.typeAttribute, Consts.error);
            node.put(Consts.arrayAttribute, Consts.error);
            return defaultVisit(node, reports);
        }

        dealWithBooleanExpr(node, reports, null, rightOperandType);

        return defaultVisit(node, reports);
    }

    private Boolean dealWithLessThanExpr(JmmNode node, List<Report> reports, Type leftOperandType, Type rightOperandType){
        if (!node.get(Consts.typeAttribute).equals(Consts.intType)){
            reports.add(new Report(
                    ReportType.ERROR,
                    Stage.SEMANTIC,
                    Integer.parseInt(node.get(Consts.lineAttribute)),
                    Integer.parseInt(node.get(Consts.columnAttribute)),
                    "Operator '" + node.get(Consts.valueAttribute) +
                            "' cannot be applied to '" +
                            leftOperandType.getName() + "', '" +
                            rightOperandType.getName() + "'"
            ));
        }
        else {
            node.put(Consts.typeAttribute, Consts.booleanType);
        }
        return defaultVisit(node, reports);
    }

    private Boolean dealWithCondition(JmmNode node, List<Report> reports){
        if (node.getChildren().get(0).get(Consts.typeAttribute).equals(Consts.autoType)){
            node.getChildren().get(0).put(Consts.typeAttribute, Consts.booleanType);
        }

        var childrenTypesOpt = NodeUtils.childrenTypes(node, reports);
        if (childrenTypesOpt.isEmpty()) return defaultVisit(node, reports);
        var childrenTypes = childrenTypesOpt.get();
        Type operation = childrenTypes.get(0);

        if (!operation.getName().equals(Consts.booleanType) && !operation.getName().equals(Consts.error) ){
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
                !leftOperandType.getName().equals(Consts.booleanType) || !rightOperandType.getName().equals(Consts.booleanType) :
                !rightOperandType.getName().equals(Consts.booleanType)){

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
            node.put(Consts.typeAttribute, Consts.error);
            node.put(Consts.arrayAttribute, Consts.error);
        }

        node.put(Consts.typeAttribute, Consts.booleanType);
        node.put(Consts.arrayAttribute, "false");
        return defaultVisit(node, reports);
    }

    private Boolean dealWithCallExpr(JmmNode node, List<Report> reports){
        var children = node.getChildren();
        var target = children.get(0);
        var argsList = children.get(1);
        var args = Arrays.asList(argsList.getChildren().stream().map(NodeUtils::extractTypeFromNode).toArray(Type[]::new));

        var targetClassSymbolOpt = symbolTable.tryGettingSymbol(
                node.getAncestor(Consts.methodDeclNodeName).map(ancestorNode -> ancestorNode.get(Consts.nameAttribute)).orElse("this"),
                target.get(Consts.valueAttribute));

        var methodName = node.get(Consts.nameAttribute);
        var methodSymbolOpt = symbolTable.tryGettingSymbol(
             "this",
                methodName
        );

        var targetClass = targetClassSymbolOpt.isPresent() ?
                targetClassSymbolOpt.get().getType().getName() :
                target.get(Consts.valueAttribute);

        var methodTypeOpt = symbolTable.tryGettingMethodType(methodName, args);

        if ( targetClass.equals(Consts.thisAttribute) ||      // If method was called on this context ( e.g. this.Foo() || ThisClass a; a.Foo() )
                targetClass.equals(symbolTable.className) ){  // Or if it was called on this class' static context (e.g. ThisClass.Foo() )

            if (targetClass.equals(Consts.thisAttribute) &&   // This cannot be used in a static context
                    node.getAncestor(Consts.methodDeclNodeName).get().getOptional("static").isPresent()){
                reports.add(new Report(
                        ReportType.WARNING,
                        Stage.SEMANTIC,
                        Integer.parseInt(node.get("line")),
                        Integer.parseInt(node.get("column")),
                        "'this' cannot be used in a static context"
                ));
                node.put(Consts.typeAttribute, Consts.error);
                node.put(Consts.arrayAttribute, Consts.error);
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
                node.put(Consts.typeAttribute, Consts.error);
                node.put(Consts.arrayAttribute, Consts.error);
            }
            else if (target.get(Consts.valueAttribute).equals(symbolTable.className) &&    // If method was called on this class' static context
                    (methodSymbolOpt.isPresent() && !methodSymbolOpt.get().isStatic()) &&  // but function isn't static (e.g. public void Foo(); ThisClass.Foo() )
                    (!target.getKind().equals(Consts.newNodeName))){                       // or a new object (e.g. new ThisClass().Foo(); )

                reports.add(new Report(
                        ReportType.WARNING,
                        Stage.SEMANTIC,
                        Integer.parseInt(node.get("line")),
                        Integer.parseInt(node.get("column")),
                        "Non-static method '" + methodName + "' cannot be referenced from a static context"
                ));
                node.put(Consts.typeAttribute, Consts.error);
                node.put(Consts.arrayAttribute, Consts.error);
            }
            else if (symbolTable.superName != null && methodTypeOpt.isEmpty()){ // If there is inheritance present, assume method is defined there and assume type is correct
                node.put(Consts.typeAttribute, Consts.autoType);
                node.put(Consts.arrayAttribute, "false");
            }
            else { // If method name was found in this class
                var method = symbolTable.getMethod(methodName, args, node, reports);
                if (method.isEmpty()) { // Check if method signature is correct (number and type of parameters)
                    node.put(Consts.typeAttribute, Consts.error);
                    node.put(Consts.arrayAttribute, Consts.error);
                }
                else { // Method signature is correct, success!
                    if (method.get().isStatic()) // Check if its static (our extra)
                        node.put(Consts.staticAttribute, "true");

                    node.put(Consts.typeAttribute, method.get().getType().getName());
                    node.put(Consts.arrayAttribute, String.valueOf(method.get().getType().isArray()));
                }
            }
        }
        else { // If method isn't called in this context or own class context
            if (symbolTable.getImports().contains(targetClass) || symbolTable.superName.equals(targetClass)){ // If calling context is an imported class or the super class, assume method is defined there and assume type is correct
                node.put(Consts.typeAttribute, Consts.autoType);
                node.put(Consts.arrayAttribute, "false");
            }
            else { // If calling context isn't found
                reports.add(new Report(
                        ReportType.ERROR,
                        Stage.SEMANTIC,
                        Integer.parseInt(node.get("line")),
                        Integer.parseInt(node.get("column")),
                        "Cannot resolve class '" + targetClass + "'"
                ));
                node.put(Consts.typeAttribute, Consts.error);
                node.put(Consts.arrayAttribute, Consts.error);
            }
        }

        return defaultVisit(node, reports);
    }

    private Boolean dealWithReturn(JmmNode node, List<Report> reports) {
        var returningType = node.getChildren().size() > 0 ?
                node.getChildren().get(0).getOptional(Consts.typeAttribute).orElse(Consts.voidType) : Consts.voidType;

        var parentOpt = node.getAncestor(Consts.methodDeclNodeName);

        if (parentOpt.isEmpty()){
            System.out.println("Return has no method. How??");
        }
        else if (returningType.equals(Consts.error)){
            return defaultVisit(node, reports);
        }
        else if (!returningType.equals(parentOpt.get().get(Consts.typeAttribute))){
            if (returningType.equals(Consts.autoType)){
                node.getChildren().get(0).put(Consts.typeAttribute, parentOpt.get().get(Consts.typeAttribute));
                node.getChildren().get(0).put(Consts.arrayAttribute, parentOpt.get().getOptional(Consts.arrayAttribute).orElse("false"));
            }
            else {
                reports.add(new Report(
                        ReportType.WARNING,
                        Stage.SEMANTIC,
                        Integer.parseInt(node.get(Consts.lineAttribute)),
                        Integer.parseInt(node.get(Consts.columnAttribute)),
                        "Return type mismatch; Required '" +
                                parentOpt.get().get("type") + "' but got '" +
                                returningType + "'"
                ));
            }
        }

        return defaultVisit(node, reports);
    }

    private Boolean dealWithPropertyAccess(JmmNode node, List<Report> reports){
        var child = node.getChildren().get(0);
        if (child.get(Consts.arrayAttribute).equals("false")){
            reports.add(new Report(
                    ReportType.ERROR,
                    Stage.SEMANTIC,
                    Integer.parseInt(node.get("line")),
                    Integer.parseInt(node.get("column")),
                    "Cannot access property of type '" + child.get(Consts.typeAttribute) + "'"
            ));
            node.put(Consts.typeAttribute, Consts.error);
            node.put(Consts.arrayAttribute, Consts.error);
        }

        return defaultVisit(node, reports);
    }

    private Boolean dealWithMethDecl(JmmNode node, List<Report> reports) {
        // Ensures void methods have a return ; present
        AtomicBoolean hasReturn = new AtomicBoolean(false);
        node.getChildren().forEach(child -> {
            if (child.getKind().equals(Consts.returnNodeName)) hasReturn.set(true);
        });

        if (!hasReturn.get()){
            node.add(new JmmNodeImpl(Consts.returnNodeName), node.getNumChildren());
        }

        return defaultVisit(node, reports);
    }

    private Boolean dealWithReservedNames(JmmNode node, List<Report> reports) {
        Set<String> reservedNames = new HashSet<>(Arrays.asList("ret", "array", "field"));
        var nodeName = node.getOptional(Consts.nameAttribute);
        var nodeVal = node.getOptional(Consts.valueAttribute);
        if (nodeVal.isEmpty()) {
            if (reservedNames.contains(nodeName.get()))
                node.put(Consts.nameAttribute, nodeName.get() + "_temp");
        }
        else {
            if (reservedNames.contains(nodeVal.get()))
                node.put(Consts.valueAttribute, nodeVal.get() + "_temp");
        }
        return true;
    }

    /**
     * Checks if array size given in instantiation is int (e.g. new int[true] not allowed)
     * @param node Binary node to get children from
     * @param reports Accumulated reports
     * @return
     */
    private boolean checkArrayDeclSizeIsInt(JmmNode node, List<Report> reports) {
        var rightOperand = node.getChildren().get(1);
        if (rightOperand.getKind().equals(Consts.newNodeName) &&
                rightOperand.get(Consts.typeAttribute).equals(Consts.intType) &&
                rightOperand.get(Consts.arrayAttribute).equals("true")) {

            var child = rightOperand.getChildren().get(0);
            if (child.get(Consts.typeAttribute).equals(Consts.intType) &&
                    child.get(Consts.arrayAttribute).equals("false")) return defaultVisit(node, reports);
            else {
                reports.add(new Report(
                        ReportType.ERROR,
                        Stage.SEMANTIC,
                        Integer.parseInt(node.get(Consts.lineAttribute)),
                        Integer.parseInt(node.get(Consts.columnAttribute)),
                        "Array must be declared with an 'int' size; Got '" +
                                child.get(Consts.typeAttribute) + "'"
                ));
                node.put(Consts.typeAttribute, Consts.error);
                node.put(Consts.arrayAttribute, Consts.error);
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
        if (leftOperandType.getName().equals(Consts.autoType)) leftOperandType = rightOperandType;
        else if (rightOperandType.getName().equals(Consts.autoType)) rightOperandType = leftOperandType;

        if (!leftOperandType.equals(rightOperandType)) {
            reports.add(new Report(
                    ReportType.ERROR,
                    Stage.SEMANTIC,
                    Integer.parseInt(node.get(Consts.lineAttribute)),
                    Integer.parseInt(node.get(Consts.columnAttribute)),
                    "Type mismatch ('" +
                            leftOperandType.getName() + (leftOperandType.isArray()?"[]":"") +
                            "' != '" +
                            rightOperandType.getName() + (rightOperandType.isArray()?"[]":"") + "')"
            ));
            node.put(Consts.typeAttribute, Consts.error);
            node.put(Consts.arrayAttribute, Consts.error);
        }
        else {
            String commonType = leftOperandType.getName();
            boolean commonIsArray = leftOperandType.isArray();
            node.put(Consts.typeAttribute, commonType);
            node.put(Consts.arrayAttribute, String.valueOf(commonIsArray));
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
                    Integer.parseInt(node.get(Consts.lineAttribute)),
                    Integer.parseInt(node.get(Consts.columnAttribute)),
                    "Operator '" + node.get(Consts.valueAttribute) +
                            "' cannot be applied to '" +
                            leftOperandType.getName() + (leftOperandType.isArray() ? "[]" : "") + "', '" +
                            rightOperandType.getName() + (rightOperandType.isArray() ? "[]" : "") + "'"
            ));
            node.put(Consts.typeAttribute, Consts.error);
            node.put(Consts.arrayAttribute, Consts.error);
        }

        return defaultVisit(node, reports);
    }

    private static Boolean defaultVisit(JmmNode node, List<Report> reports) {
        return true;
    }
}
