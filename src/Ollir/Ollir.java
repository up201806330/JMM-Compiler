import pt.up.fe.comp.jmm.JmmNode;

import java.util.*;

public class Ollir {
    private final String ident = "  ";
    private List<String> methodParameters = new ArrayList<>();
    private List<String> methodVars = new ArrayList<>();
    private List<String> imports = new ArrayList<>();

    int tempVarCounter = 1;
    int ifCounter = 1;
    int whileCounter = 1;

    public String getCode(JmmNode root) {
        StringBuilder stringBuilder = new StringBuilder();

        for (var node: root.getChildren()) {
            switch (node.getKind()) {
                case Constants.importDeclNodeName -> stringBuilder.append(importDeclarationToOllir(node, ""));
                case Constants.classDeclNodeName -> stringBuilder.append(classDeclarationToOllir(node, ""));
                default -> System.out.println("getCode: " + node);
            }
        }

        return stringBuilder.toString();
    }

    private String importDeclarationToOllir(JmmNode node, String prefix) {
        StringBuilder stringBuilder = new StringBuilder();
        if (node.getParent().getKind().equals("Program"))
            stringBuilder.append("import ");

        if (node.getNumChildren() != 0){
            stringBuilder.append(node.get(Constants.typeAttribute)).append(".");
            stringBuilder.append(importDeclarationToOllir(node.getChildren().get(0), ""));
        }
        else {
            stringBuilder.append(node.get(Constants.typeAttribute)).append(";\n");
            imports.add(node.get(Constants.nameAttribute));
        }
        return stringBuilder.toString();
    }

    private String classDeclarationToOllir(JmmNode node, String prefix) {
        StringBuilder classOllir = new StringBuilder();

        classOllir.append(node.get(Constants.nameAttribute));

        StringBuilder methodsOllir = new StringBuilder();
        StringBuilder fieldsOllir = new StringBuilder();
        for (var child: node.getChildren()) {
            switch (child.getKind()) {
                case Constants.methodDeclNodeName -> methodsOllir.append("\n").append(methodDeclarationToOllir(child, prefix + ident));
                case Constants.fieldDeclNodeName -> fieldsOllir.append(fieldDeclarationToOllir(child, prefix + ident));
                case Constants.classInheritNodeName -> classOllir.append(" extends ").append(child.get(Constants.typeAttribute));
                default -> System.out.println("classDeclarationToOllir: " + child);
            }
        }

        classOllir.append(" {");

        classOllir.append("\n").append(prefix).append(fieldsOllir);
        classOllir.append("\n").append(prefix).append(ident);
        classOllir.append(OllirCodeUtils.defaultConstructor(ident, node.get(Constants.nameAttribute)));

        classOllir.append(methodsOllir);
        classOllir.append(prefix).append("}");

        return classOllir.toString();
    }

    private String fieldDeclarationToOllir(JmmNode node, String prefix) {
        StringBuilder fieldOllir = new StringBuilder(prefix);

        var child = node.getChildren().get(0);
        switch (child.getKind()) {
            case Constants.varDeclNodeName -> {
                fieldOllir.append(".field private ").append(child.get(Constants.nameAttribute))
                          .append(OllirCodeUtils.typeToOllir(child.get(Constants.typeAttribute), child.getOptional(Constants.arrayAttribute)))
                          .append(";\n");
            }
            default -> System.out.println("fieldDeclarationToOllir: " + child);
        }

        return fieldOllir.toString();
    }

    private String methodDeclarationToOllir(JmmNode node, String prefix) {
        StringBuilder methodOllir = new StringBuilder(prefix);

        methodParameters = new ArrayList<>();
        methodVars = new ArrayList<>();
        tempVarCounter = 1;
        ifCounter = 1;
        whileCounter = 1;

        methodOllir.append(".method public ");
        Optional<String> staticAttribute = node.getOptional(Constants.staticAttribute);
        if (staticAttribute.isPresent()) {
            methodOllir.append("static ");
        }
        methodOllir.append(node.get(Constants.nameAttribute)).append("(");

        var children = node.getChildren();

        List<JmmNode> parameters = new ArrayList<JmmNode>();
        JmmNode type = null;
        StringBuilder insideMethod = new StringBuilder();

        for (var child: children) {
            switch (child.getKind()) {
                case Constants.typeNodeName -> type = child;
                case Constants.methodParamNodeName -> { parameters.add(child); methodParameters.add(child.get(Constants.nameAttribute)); }
                case Constants.assignmentNodeName -> insideMethod.append(assignmentToOllir(child, prefix + ident));
                case Constants.callExprNodeName -> insideMethod.append(callExpressionToOllir(child, prefix + ident, insideMethod, true)).append(";\n");
                case Constants.returnNodeName -> insideMethod.append(returnToOllir(child, prefix + ident));
                case Constants.varDeclNodeName -> methodVars.add(child.get(Constants.nameAttribute));
                case Constants.ifStatementNodeName -> {
                    insideMethod.append(ifStatementToOllir(child, prefix + ident));
                    prefix += ident;
                }
                case Constants.whileStatementNodeName -> {
                    insideMethod.append(whileStatementToOllir(child, prefix + ident));
                    prefix += ident;
                }
                default -> System.out.println("methodDeclarationToOllir: " + child);
            }
        }

        methodOllir.append(OllirCodeUtils.parametersToOllir(parameters, "", node.get(Constants.nameAttribute)));
        methodOllir.append(")");

        if (type != null)
            methodOllir.append(OllirCodeUtils.typeToOllir(type.get(Constants.typeAttribute), node.getOptional(Constants.arrayAttribute)));
        else
            methodOllir.append(OllirCodeUtils.typeToOllir(Constants.voidType, node.getOptional(Constants.arrayAttribute)));

        methodOllir.append(" {\n");
        methodOllir.append(insideMethod);
        methodOllir.append(prefix).append("}\n");

        return methodOllir.toString();
    }

    private String whileStatementToOllir(JmmNode node, String prefix) {
        StringBuilder stringBuilder = new StringBuilder(prefix);

        var children = node.getChildren();
        // nextTempVariable = 1; // We need a better way of doing this

        var thisWhile = whileCounter++;
        stringBuilder.append("goto Test_").append(thisWhile).append(";\n");
        stringBuilder.append(prefix).append("Loop_").append(thisWhile).append(":\n");
        stringBuilder.append(whileBodyToOllir(children.get(1), prefix + ident));
        stringBuilder.append(prefix).append("Test_").append(thisWhile).append(":\n");
        stringBuilder.append(whileConditionToOllir(children.get(0), prefix + ident, thisWhile)).append("\n");

        return stringBuilder.append(prefix).append("End_").append(thisWhile).append(":\n").toString();
    }

    private String whileBodyToOllir(JmmNode node, String prefix) {
        StringBuilder stringBuilder = new StringBuilder();
        StringBuilder before = new StringBuilder();

        for (var child: node.getChildren()) {
            switch (child.getKind()) {
                case Constants.assignmentNodeName -> stringBuilder.append(assignmentToOllir(child, prefix));
                case Constants.callExprNodeName -> stringBuilder.append(prefix).append(callExpressionToOllir(child, prefix, before, false)).append(";\n");
                case Constants.ifStatementNodeName -> {
                    stringBuilder.append(ifStatementToOllir(child, prefix + ident));
                    prefix += ident;
                }
                case Constants.whileStatementNodeName -> {
                    stringBuilder.append(whileStatementToOllir(child, prefix + ident));
                    prefix += ident;
                }
                default -> System.out.println("whileBodyToOllir: " + child);
            }
        }

        return before.append(stringBuilder).toString();
    }

    private String whileConditionToOllir(JmmNode node, String prefix, int label) {
        StringBuilder stringBuilder = new StringBuilder();
        StringBuilder ifCondition = new StringBuilder();
        StringBuilder before = new StringBuilder();

        var child = node.getChildren().get(0);

        switch (child.getKind()) {
            case Constants.terminalNodeName -> ifCondition.append(terminalToOllir(child, "")).append(" &&.bool true.bool");
            case Constants.literalNodeName -> ifCondition.append(literalToOllir(child, "")).append(" &&.bool true.bool");
            case Constants.binaryNodeName -> ifCondition.append(binaryToOllir(child, prefix, before));
            case Constants.notExprNodeName -> ifCondition.append(notExpressionToOllir(child, prefix, before));
            case Constants.callExprNodeName, Constants.propertyAccessNodeName,
                    Constants.arrayExprNodeName, Constants.newNodeName ->
                    ifCondition.append(makeLocalVar(child, prefix, before)).append(" &&.bool true.bool");
            default -> System.out.println("whileConditionToOllir: " + child);
        }


        stringBuilder.append(prefix).append("if (").append(ifCondition).append(") goto Loop_").append(label).append(";");
        return before.append(stringBuilder).toString();
    }

    private String notExpressionToOllir(JmmNode node, String prefix, StringBuilder before) {
        StringBuilder stringBuilder = new StringBuilder();
        String operand = "";

        var child = node.getChildren().get(0);

        if (child.get(Constants.typeAttribute).equals(Constants.autoType))
            child.put(Constants.typeAttribute, Constants.booleanType);

        switch (child.getKind()) {
            case Constants.terminalNodeName -> operand = terminalToOllir(child, "");
            case Constants.literalNodeName -> operand = literalToOllir(child, "");
            case Constants.callExprNodeName, Constants.propertyAccessNodeName, Constants.notExprNodeName,
                    Constants.binaryNodeName, Constants.arrayExprNodeName, Constants.newNodeName ->
                operand = makeLocalVar(child, prefix, before);

            default -> System.out.println("notToOllir: " + child);
        }
        stringBuilder.append(operand).append(" ");

        stringBuilder.append(node.get(Constants.valueAttribute)) // Hardcoded because at this point, all operations' types have been checked
                     .append(OllirCodeUtils.typeToOllir(Constants.booleanType, Optional.empty()))
                     .append(" ").append(operand);

        return stringBuilder.toString();
    }

    private String ifStatementToOllir(JmmNode node, String prefix) {
        StringBuilder stringBuilder = new StringBuilder(prefix);
        StringBuilder beforeCond = new StringBuilder();
        StringBuilder ifStatement = new StringBuilder();

        var children = node.getChildren();

        var thisIf = ifCounter++;
        stringBuilder.append("if (");
        stringBuilder.append(ifConditionToOllir(children.get(0), prefix, beforeCond));
        stringBuilder.append(") goto ifbody_").append(thisIf).append(";\n");
        stringBuilder.append(elseStatementToOllir(children.get(children.size() - 1), prefix + ident));
        stringBuilder.append(prefix).append(ident).append("goto endif_").append(thisIf).append(";\n");
        stringBuilder.append(prefix).append("ifbody_").append(thisIf).append(":\n");

        int i = 1;
        var child = children.get(1);
        while (!child.getKind().equals(Constants.elseStatementNodeName)) {
            switch (child.getKind()) {
                case Constants.assignmentNodeName -> ifStatement.append(assignmentToOllir(child, prefix + ident));
                case Constants.callExprNodeName -> ifStatement.append(callExpressionToOllir(child, prefix + ident, ifStatement, true)).append(";\n");
                case Constants.ifStatementNodeName -> {
                    ifStatement.append(ifStatementToOllir(child, prefix + ident));
                    prefix += ident;
                }
                case Constants.whileStatementNodeName -> {
                    ifStatement.append(whileStatementToOllir(child, prefix + ident));
                    prefix += ident;
                }
                default -> System.out.println("ifStatementToOllir: " + child);
            }
            i++;
            child = children.get(i);
        }

        stringBuilder.append(ifStatement);
        stringBuilder.append(prefix).append("endif_").append(thisIf).append(":\n");

        return beforeCond.append(stringBuilder).toString();
    }

    private String elseStatementToOllir(JmmNode node, String prefix) {
        StringBuilder stringBuilder = new StringBuilder();

        var children = node.getChildren();
        for (var child: children) {
            switch (child.getKind()) {
                case Constants.assignmentNodeName -> stringBuilder.append(assignmentToOllir(child, prefix));
                case Constants.callExprNodeName -> stringBuilder.append(callExpressionToOllir(child, prefix, stringBuilder, true)).append(";\n");
                case Constants.returnNodeName -> stringBuilder.append(returnToOllir(child, prefix));
                case Constants.varDeclNodeName -> methodVars.add(child.get(Constants.nameAttribute));
                case Constants.ifStatementNodeName -> {
                    stringBuilder.append(ifStatementToOllir(child, prefix));
                    prefix += ident;
                }
                case Constants.whileStatementNodeName -> {
                    stringBuilder.append(whileStatementToOllir(child, prefix));
                    prefix += ident;
                }
                default -> System.out.println("elseStatementToOllir: " + child);
            }
        }

        return stringBuilder.toString();
    }

    private String ifConditionToOllir(JmmNode node, String prefix, StringBuilder before) {
        StringBuilder stringBuilder = new StringBuilder();

        var child = node.getChildren().get(0);
        switch (child.getKind()) {
            case Constants.terminalNodeName -> stringBuilder.append(terminalToOllir(child, "")).append(" &&.bool true.bool");
            case Constants.literalNodeName -> stringBuilder.append(literalToOllir(child, "")).append(" &&.bool true.bool");
            case Constants.binaryNodeName -> stringBuilder.append(binaryToOllir(child, prefix, before));
            case Constants.notExprNodeName -> stringBuilder.append(notExpressionToOllir(child, prefix, before));
            case Constants.callExprNodeName, Constants.propertyAccessNodeName,
                    Constants.arrayExprNodeName, Constants.newNodeName ->
                    stringBuilder.append(makeLocalVar(child, prefix, before)).append(" &&.bool true.bool");
            default -> System.out.println("ifConditionToOllir: " + child);
        }

        return stringBuilder.toString();
    }

    private String returnToOllir(JmmNode node, String prefix) {
        StringBuilder stringBuilder = new StringBuilder(prefix);
        StringBuilder before = new StringBuilder();

        if (node.getChildren().size() == 0) {
            stringBuilder.append("ret.V");
        } else {
            var child = node.getChildren().get(0);
            String type = OllirCodeUtils.typeToOllir(child.get(Constants.typeAttribute), child.getOptional(Constants.arrayAttribute));
            stringBuilder.append("ret").append(type).append(" ");
            switch (child.getKind()) {
                case Constants.literalNodeName -> stringBuilder.append(literalToOllir(child, ""));
                case Constants.terminalNodeName -> stringBuilder.append(terminalToOllir(child, prefix, before));
                case Constants.callExprNodeName, Constants.propertyAccessNodeName, Constants.notExprNodeName,
                        Constants.binaryNodeName, Constants.arrayExprNodeName, Constants.newNodeName ->
                        stringBuilder.append(makeLocalVar(child, prefix, before)).append(" ");
                default -> System.out.println("returnToOllir: " + child);
            }
        }
        return before.append(stringBuilder).append(";\n").toString();
    }

    private String assignmentToOllir(JmmNode node, String prefix) {
        StringBuilder stringBuilder = new StringBuilder();

        var children = node.getChildren();

        StringBuilder left = new StringBuilder();
        StringBuilder right = new StringBuilder();
        StringBuilder before = new StringBuilder();

        var child0 = children.get(0);
        var child1 = children.get(1);
        String varName = (child0.getKind().equals(Constants.arrayExprNodeName) ?
                child0.getChildren().get(0).get(Constants.valueAttribute) :
                child0.get(Constants.valueAttribute));
        boolean isField = isField(varName);

        if (child1.get(Constants.typeAttribute).equals(Constants.autoType)) {
            child1.put(Constants.typeAttribute, child0.get(Constants.typeAttribute));
            child1.put(Constants.arrayAttribute, child0.getOptional(Constants.arrayAttribute).orElse("false"));
        }

        switch (child0.getKind()) {
            case Constants.terminalNodeName -> left.append(terminalToOllir(child0, ""));
            case Constants.arrayExprNodeName -> left.append(arrayExpressionToOllir(child0, prefix, before));
            default -> System.out.println("assignementToOllir: " + child0);
        }

        switch (child1.getKind()) {
            case Constants.terminalNodeName ->
                    right.append(terminalToOllir(child1, ""));
            case Constants.literalNodeName ->
                    right.append(literalToOllir(child1, ""));
            case Constants.newNodeName ->
                    right.append(isField ? makeLocalVar(child1, prefix, before) : newToOllir(child1, prefix, left.toString(), before));
            case Constants.notExprNodeName ->
                    right.append(isField ? makeLocalVar(child1, prefix, before) : notExpressionToOllir(child1, prefix, before));
            case Constants.binaryNodeName ->
                    right.append(isField ? makeLocalVar(child1, prefix, before) : binaryToOllir(child1, prefix, before));
            case Constants.callExprNodeName ->
                    right.append(isField ? makeLocalVar(child1, prefix, before) : callExpressionToOllir(child1, prefix, before, false));
            case Constants.arrayExprNodeName ->
                    right.append(isField ? makeLocalVar(child1, prefix, before) : arrayExpressionToOllir(child1, prefix, before));
            case Constants.propertyAccessNodeName ->
                    right.append(isField ? makeLocalVar(child1, prefix, before) : propertyAccessToOllir(child1, prefix, before));
            default -> System.out.println("assignementToOllir: " + child1);
        }

        if (!before.toString().equals(prefix)) {
            stringBuilder.append(before);
        }

        if (isField && !child0.getKind().equals(Constants.arrayExprNodeName)){
            stringBuilder.append(prefix)
                         .append("putfield(this, ")
                         .append(left).append(", ")
                         .append(right)
                         .append(").V;\n");
        }
        else {
            stringBuilder.append(prefix)
                         .append(left).append(" ")
                         .append(":=").append(OllirCodeUtils.typeToOllir(node.get(Constants.typeAttribute), node.getOptional(Constants.arrayAttribute))).append(" ")
                         .append(right)
                         .append(";\n");
        }

        return stringBuilder.toString();
    }

    private boolean isField(String varName) {
        return !varName.equals("") && !(methodVars.contains(varName) || methodParameters.contains(varName));
    }

    private String arrayExpressionToOllir(JmmNode node, String prefix, StringBuilder before) {
        StringBuilder stringBuilder = new StringBuilder();

        var children = node.getChildren();

        var child = children.get(0);
        if (child.getOptional(Constants.valueAttribute).isEmpty() || isField(child.get(Constants.valueAttribute)))
            stringBuilder.append(makeLocalVar(child, prefix, before, false));
        else
            stringBuilder.append(terminalToOllir(child, "", false));
        stringBuilder.append("[");

        child = children.get(1);
        switch (child.getKind()) {
            case Constants.terminalNodeName ->
                    stringBuilder.append(terminalToOllir(child, prefix, before));
            case Constants.literalNodeName,
                    Constants.callExprNodeName, Constants.propertyAccessNodeName, Constants.notExprNodeName,
                    Constants.binaryNodeName, Constants.arrayExprNodeName, Constants.newNodeName ->
                    stringBuilder.append(makeLocalVar(child, prefix, before));
            default -> System.out.println("arrayExpressionToOllir: " + child);
        }

        stringBuilder.append("]").append(OllirCodeUtils.typeToOllir(node.get(Constants.typeAttribute), node.getOptional(Constants.arrayAttribute)));
        return stringBuilder.toString();
    }

    private String binaryToOllir(JmmNode node, String prefix, StringBuilder before) {
        StringBuilder stringBuilder = new StringBuilder();

        var child0 = node.getChildren().get(0);
        var child1 = node.getChildren().get(1);

        if (child0.get(Constants.typeAttribute).equals(Constants.autoType))
            child0.put(Constants.typeAttribute, child1.get(Constants.typeAttribute));
        else if (child1.get(Constants.typeAttribute).equals(Constants.autoType))
            child1.put(Constants.typeAttribute, child0.get(Constants.typeAttribute));

        switch (child0.getKind()) {
            case Constants.terminalNodeName -> {
                if (isField(child0.getOptional(Constants.valueAttribute).orElse("")))
                    stringBuilder.append(makeLocalVar(child0, prefix, before)).append(" ");
                else
                    stringBuilder.append(terminalToOllir(child0, "")).append(" ");
            }
            case Constants.literalNodeName -> stringBuilder.append(literalToOllir(child0, "")).append(" ");
            case Constants.callExprNodeName, Constants.propertyAccessNodeName, Constants.notExprNodeName,
                    Constants.binaryNodeName, Constants.arrayExprNodeName, Constants.newNodeName ->
                    stringBuilder.append(makeLocalVar(child0, prefix, before)).append(" ");
            default -> System.out.println("binaryToOllir: " + child0);
        }

        stringBuilder.append(node.get(Constants.valueAttribute));
        stringBuilder.append(node.get(Constants.valueAttribute).equals(Constants.lessThanExpression) ? // Hardcoded because at this point, all operations' types have been checked
                OllirCodeUtils.typeToOllir(Constants.intType, Optional.empty()) :
                OllirCodeUtils.typeToOllir(node.get(Constants.typeAttribute), node.getOptional(Constants.arrayAttribute))).append(" ");

        switch (child1.getKind()) {
            case Constants.terminalNodeName ->  {
                if (isField(child1.getOptional(Constants.valueAttribute).orElse("")))
                    stringBuilder.append(makeLocalVar(child1, prefix, before)).append(" ");
                else
                    stringBuilder.append(terminalToOllir(child1, "")).append(" ");
            }
            case Constants.literalNodeName -> stringBuilder.append(literalToOllir(child1, ""));
            case Constants.callExprNodeName, Constants.propertyAccessNodeName, Constants.notExprNodeName,
                    Constants.binaryNodeName, Constants.arrayExprNodeName, Constants.newNodeName ->
                stringBuilder.append(makeLocalVar(child1, prefix, before));
            default -> System.out.println("binaryToOllir: " + child1);
        }

        return stringBuilder.toString();
    }

    private String makeLocalVar(JmmNode child, String prefix, StringBuilder before) {
        return makeLocalVar(child, prefix, before, true);
    }

    private String makeLocalVar(JmmNode child, String prefix, StringBuilder before, boolean withType){
        String typeToOllir = OllirCodeUtils.typeToOllir(child.get(Constants.typeAttribute), child.getOptional(Constants.arrayAttribute));
        String result = "";
        switch (child.getKind()){
            case Constants.callExprNodeName-> result = callExpressionToOllir(child, prefix, before, false);
            case Constants.propertyAccessNodeName -> result = propertyAccessToOllir(child, prefix, before);
            case Constants.notExprNodeName -> result = notExpressionToOllir(child, prefix, before);
            case Constants.binaryNodeName -> result = binaryToOllir(child, prefix, before);
            case Constants.arrayExprNodeName -> result = arrayExpressionToOllir(child, prefix, before);
            case Constants.newNodeName -> result = newToOllir(child, prefix, "t" + tempVarCounter + typeToOllir, before);
            case Constants.literalNodeName -> result = literalToOllir(child, "");
            case Constants.terminalNodeName -> result = terminalToOllir(child, "");
        }

        before.append(prefix).append("t").append(tempVarCounter).append(typeToOllir)
                .append(" :=").append(typeToOllir).append(" ")
                .append(result)
                .append(";\n");

        return "t" + tempVarCounter++ + (withType ? typeToOllir : "");
    }

    private String propertyAccessToOllir(JmmNode node, String prefix, StringBuilder before) {
        StringBuilder stringBuilder = new StringBuilder();

        switch (node.get(Constants.valueAttribute)) {
            case Constants.lengthProperty -> stringBuilder.append("arraylength(");
            default -> System.out.println("propertyAccessToOllir: " + node);
        }

        var child = node.getChildren().get(0);
        switch (child.getKind()) {
            case Constants.terminalNodeName -> stringBuilder.append(terminalToOllir(child, prefix, before));
            case Constants.literalNodeName -> stringBuilder.append(literalToOllir(child, ""));
            case Constants.callExprNodeName, Constants.propertyAccessNodeName, Constants.notExprNodeName,
                    Constants.binaryNodeName, Constants.arrayExprNodeName, Constants.newNodeName ->
                    stringBuilder.append(makeLocalVar(child, prefix, before));
            default -> System.out.println("propertyAccessToOllir: " + child);
        }

        stringBuilder.append(")").append(OllirCodeUtils.typeToOllir(child.get(Constants.typeAttribute), Optional.empty()));

        return stringBuilder.toString();
    }

    private String callExpressionToOllir(JmmNode node, String prefix, StringBuilder before, boolean insideMethod) {
        StringBuilder stringBuilder = new StringBuilder();

        var children = node.getChildren();

        var child0 = children.get(0);

        if (imports.contains(child0.get(Constants.valueAttribute)) || node.getOptional(Constants.staticAttribute).isPresent()) {
            stringBuilder.append("invokestatic(");
            stringBuilder.append(child0.get(Constants.typeAttribute));
        } else {
            stringBuilder.append("invokevirtual(");
            if (child0.get(Constants.valueAttribute).equals(Constants.thisAttribute)) {
                stringBuilder.append("this");
            } else {
                switch (child0.getKind()) {
                    case Constants.terminalNodeName -> stringBuilder.append(terminalToOllir(child0, prefix, before));
                    case Constants.literalNodeName -> stringBuilder.append(literalToOllir(child0, ""));
                    case Constants.callExprNodeName, Constants.propertyAccessNodeName, Constants.notExprNodeName,
                            Constants.binaryNodeName, Constants.arrayExprNodeName, Constants.newNodeName ->
                            stringBuilder.append(makeLocalVar(child0, prefix, before));
                    default -> System.out.println("callExpressionToOllirLeftSide: " + child0);
                }
            }
        }

        stringBuilder.append(", \"").append(node.get(Constants.nameAttribute)).append("\"");

        if (children.size() > 1) {
            var args = children.get(1).getChildren();
            for (var arg : args) {
                stringBuilder.append(", ");
                switch (arg.getKind()) {
                    case Constants.terminalNodeName -> stringBuilder.append(terminalToOllir(arg, prefix, before));
                    case Constants.literalNodeName -> stringBuilder.append(literalToOllir(arg, ""));
                    case Constants.callExprNodeName, Constants.propertyAccessNodeName, Constants.notExprNodeName,
                            Constants.binaryNodeName, Constants.arrayExprNodeName, Constants.newNodeName ->
                            stringBuilder.append(makeLocalVar(arg, prefix, before));
                    default -> System.out.println("callExpressionToOllir: " + arg);
                }
            }
        }

        stringBuilder.append(")").
                append(OllirCodeUtils.typeToOllir(node.get(Constants.typeAttribute), node.getOptional(Constants.arrayAttribute)));

        return (insideMethod ? prefix : "") + stringBuilder;
    }

    private String newToOllir(JmmNode node, String prefix, String left, StringBuilder before) {
        StringBuilder stringBuilder = new StringBuilder();

        var isArray = node.getOptional(Constants.arrayAttribute);
        stringBuilder.append("new(")
                .append(isArray.isPresent() && isArray.get().equals("true") ? "array" : node.get(Constants.typeAttribute));

        var children = node.getChildren();
        if (children.size() > 0) {
            var child = children.get(0);
            stringBuilder.append(", ");
            switch (child.getKind()) {
                case Constants.terminalNodeName -> stringBuilder.append(terminalToOllir(child, ""));
                case Constants.literalNodeName -> stringBuilder.append(literalToOllir(child, ""));
                case Constants.callExprNodeName, Constants.propertyAccessNodeName, Constants.notExprNodeName,
                        Constants.binaryNodeName, Constants.arrayExprNodeName, Constants.newNodeName ->
                        stringBuilder.append(makeLocalVar(child, prefix, before)).append(" ");
                default -> System.out.println("newToOllir: " + child);
            }
        }

        stringBuilder.append(")");
        stringBuilder.append(OllirCodeUtils.typeToOllir(node.get(Constants.typeAttribute), node.getOptional(Constants.arrayAttribute)));

        if (isArray.isEmpty() || isArray.get().equals("false"))
            stringBuilder.append(";\n").append(prefix).append("invokespecial(").append(left).append(", \"<init>\").V");

        return stringBuilder.toString();
    }

    private String literalToOllir(JmmNode node, String prefix) {
        StringBuilder stringBuilder = new StringBuilder(prefix);

        String type = node.get(Constants.typeAttribute);
        switch (type) {
            case Constants.intType, Constants.booleanType -> {
                stringBuilder.append(node.get(Constants.valueAttribute));
                stringBuilder.append(OllirCodeUtils.typeToOllir(type, node.getOptional(Constants.arrayAttribute)));
            }
            default -> System.out.println("literalToOllir: " + type);
        }

        return stringBuilder.toString();
    }

    private String terminalToOllir(JmmNode node, String prefix, boolean withType) {
        StringBuilder stringBuilder = new StringBuilder(prefix);
        String varName = node.get(Constants.valueAttribute);
        JmmNode parent = node.getParent();
        String leftName = parent.getKind().equals(Constants.assignmentNodeName) ?
                    parent.getChildren().get(0).getOptional(Constants.valueAttribute).orElse("") : "";

        if (!isField(varName)){ // Is local var or parameter
            if (varName.equals(Constants.thisAttribute)){
                stringBuilder.append("$0.").append(Constants.thisAttribute);
                stringBuilder.append(OllirCodeUtils.typeToOllir(node.get(Constants.typeAttribute), node.getOptional(Constants.arrayAttribute)));
                return stringBuilder.toString();
            }
            for (int i = 0; i < methodParameters.size(); i++) {
                if (methodParameters.get(i).equals(varName)) {
                    stringBuilder.append("$").append(i + 1).append(".");
                }
            }
        }
        else if (!isField(leftName)) { // Is a field and isn't being assigned to a field
            String type = OllirCodeUtils.typeToOllir(node.get(Constants.typeAttribute), node.getOptional(Constants.arrayAttribute));
            stringBuilder.append("getfield(this, ")
                    .append(varName).append(type)
                    .append(")").append(type);
            return stringBuilder.toString();
        }

        stringBuilder.append(varName);
        if (withType)
            stringBuilder.append(OllirCodeUtils.typeToOllir(
                    node.get(Constants.typeAttribute),
                    node.getOptional(Constants.arrayAttribute)));

        return stringBuilder.toString();
    }

    private String terminalToOllir(JmmNode node, String prefix) {
        return terminalToOllir(node, prefix, true);
    }

    private String terminalToOllir(JmmNode node, String prefix, StringBuilder before) {
        return terminalToOllir(node, prefix, before, true);
    }

    private String terminalToOllir(JmmNode node, String prefix, StringBuilder before , boolean withType) {
        if (isField(node.getOptional(Constants.valueAttribute).orElse("")))
            return makeLocalVar(node, prefix, before, withType);
        else
            return terminalToOllir(node, "", withType);
    }
}
