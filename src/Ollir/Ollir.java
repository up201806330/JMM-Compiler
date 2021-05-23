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
                case Consts.importDeclNodeName -> stringBuilder.append(importDeclarationToOllir(node, ""));
                case Consts.classDeclNodeName -> stringBuilder.append(classDeclarationToOllir(node, ""));
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
            stringBuilder.append(node.get(Consts.typeAttribute)).append(".");
            stringBuilder.append(importDeclarationToOllir(node.getChildren().get(0), ""));
        }
        else {
            stringBuilder.append(node.get(Consts.typeAttribute)).append(";\n");
            imports.add(node.get(Consts.nameAttribute));
        }
        return stringBuilder.toString();
    }

    private String classDeclarationToOllir(JmmNode node, String prefix) {
        StringBuilder classOllir = new StringBuilder();

        classOllir.append(node.get(Consts.nameAttribute));

        StringBuilder methodsOllir = new StringBuilder();
        StringBuilder fieldsOllir = new StringBuilder();
        for (var child: node.getChildren()) {
            switch (child.getKind()) {
                case Consts.methodDeclNodeName -> methodsOllir.append("\n").append(methodDeclarationToOllir(child, prefix + ident));
                case Consts.fieldDeclNodeName -> fieldsOllir.append(fieldDeclarationToOllir(child, prefix + ident));
                case Consts.classInheritNodeName -> classOllir.append(" extends ").append(child.get(Consts.typeAttribute));
                default -> System.out.println("classDeclarationToOllir: " + child);
            }
        }

        classOllir.append(" {");

        classOllir.append("\n").append(prefix).append(fieldsOllir);
        classOllir.append("\n").append(prefix).append(ident);
        classOllir.append(OllirCodeUtils.defaultConstructor(ident, node.get(Consts.nameAttribute)));

        classOllir.append(methodsOllir);
        classOllir.append(prefix).append("}");

        return classOllir.toString();
    }

    private String fieldDeclarationToOllir(JmmNode node, String prefix) {
        StringBuilder fieldOllir = new StringBuilder(prefix);

        var child = node.getChildren().get(0);
        switch (child.getKind()) {
            case Consts.varDeclNodeName -> {
                fieldOllir.append(".field private ").append(child.get(Consts.nameAttribute))
                          .append(OllirCodeUtils.typeToOllir(child.get(Consts.typeAttribute), child.getOptional(Consts.arrayAttribute)))
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
        Optional<String> staticAttribute = node.getOptional(Consts.staticAttribute);
        if (staticAttribute.isPresent()) {
            methodOllir.append("static ");
        }
        methodOllir.append(node.get(Consts.nameAttribute)).append("(");

        var children = node.getChildren();

        List<JmmNode> parameters = new ArrayList<JmmNode>();
        JmmNode type = null;
        StringBuilder insideMethod = new StringBuilder();

        for (var child: children) {
            switch (child.getKind()) {
                case Consts.typeNodeName -> type = child;
                case Consts.methodParamNodeName -> { parameters.add(child); methodParameters.add(child.get(Consts.nameAttribute)); }
                case Consts.assignmentNodeName -> insideMethod.append(assignmentToOllir(child, prefix + ident));
                case Consts.callExprNodeName -> insideMethod.append(callExpressionToOllir(child, prefix + ident, insideMethod, true)).append(";\n");
                case Consts.returnNodeName -> insideMethod.append(returnToOllir(child, prefix + ident));
                case Consts.varDeclNodeName -> methodVars.add(child.get(Consts.nameAttribute));
                case Consts.ifStatementNodeName -> {
                    insideMethod.append(ifStatementToOllir(child, prefix + ident));
                    prefix += ident;
                }
                case Consts.whileStatementNodeName -> {
                    insideMethod.append(whileStatementToOllir(child, prefix + ident));
                    prefix += ident;
                }
                default -> System.out.println("methodDeclarationToOllir: " + child);
            }
        }

        methodOllir.append(OllirCodeUtils.parametersToOllir(parameters, "", node.get(Consts.nameAttribute)));
        methodOllir.append(")");

        if (type != null)
            methodOllir.append(OllirCodeUtils.typeToOllir(type.get(Consts.typeAttribute), node.getOptional(Consts.arrayAttribute)));
        else
            methodOllir.append(OllirCodeUtils.typeToOllir(Consts.voidType, node.getOptional(Consts.arrayAttribute)));

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
                case Consts.assignmentNodeName -> stringBuilder.append(assignmentToOllir(child, prefix));
                case Consts.callExprNodeName -> stringBuilder.append(callExpressionToOllir(child, prefix, before, true)).append(";\n");
                case Consts.ifStatementNodeName -> {
                    stringBuilder.append(ifStatementToOllir(child, prefix + ident));
                    prefix += ident;
                }
                case Consts.whileStatementNodeName -> {
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
            case Consts.terminalNodeName -> ifCondition.append(terminalToOllir(child, "")).append(" &&.bool true.bool");
            case Consts.literalNodeName -> ifCondition.append(literalToOllir(child, "")).append(" &&.bool true.bool");
            case Consts.binaryNodeName -> ifCondition.append(binaryToOllir(child, prefix, before));
            case Consts.notExprNodeName -> ifCondition.append(notExpressionToOllir(child, prefix, before));
            case Consts.callExprNodeName, Consts.propertyAccessNodeName,
                    Consts.arrayExprNodeName, Consts.newNodeName ->
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

        if (child.get(Consts.typeAttribute).equals(Consts.autoType))
            child.put(Consts.typeAttribute, Consts.booleanType);

        switch (child.getKind()) {
            case Consts.terminalNodeName -> operand = terminalToOllir(child, "");
            case Consts.literalNodeName -> operand = literalToOllir(child, "");
            case Consts.callExprNodeName, Consts.propertyAccessNodeName, Consts.notExprNodeName,
                    Consts.binaryNodeName, Consts.arrayExprNodeName, Consts.newNodeName ->
                operand = makeLocalVar(child, prefix, before);

            default -> System.out.println("notToOllir: " + child);
        }
        stringBuilder.append(operand).append(" ");

        stringBuilder.append(node.get(Consts.valueAttribute)) // Hardcoded because at this point, all operations' types have been checked
                     .append(OllirCodeUtils.typeToOllir(Consts.booleanType, Optional.empty()))
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
        stringBuilder.append(prefix).append("elsebody_").append(thisIf).append(":\n");
        stringBuilder.append(elseStatementToOllir(children.get(children.size() - 1), prefix + ident));
        stringBuilder.append(prefix).append(ident).append("goto endif_").append(thisIf).append(";\n");
        stringBuilder.append(prefix).append("ifbody_").append(thisIf).append(":\n");

        int i = 1;
        var child = children.get(1);
        while (!child.getKind().equals(Consts.elseStatementNodeName)) {
            switch (child.getKind()) {
                case Consts.assignmentNodeName -> ifStatement.append(assignmentToOllir(child, prefix + ident));
                case Consts.callExprNodeName -> ifStatement.append(callExpressionToOllir(child, prefix + ident, ifStatement, true)).append(";\n");
                case Consts.ifStatementNodeName -> {
                    ifStatement.append(ifStatementToOllir(child, prefix + ident));
                    prefix += ident;
                }
                case Consts.whileStatementNodeName -> {
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
                case Consts.assignmentNodeName -> stringBuilder.append(assignmentToOllir(child, prefix));
                case Consts.callExprNodeName -> stringBuilder.append(callExpressionToOllir(child, prefix, stringBuilder, true)).append(";\n");
                case Consts.returnNodeName -> stringBuilder.append(returnToOllir(child, prefix));
                case Consts.varDeclNodeName -> methodVars.add(child.get(Consts.nameAttribute));
                case Consts.ifStatementNodeName -> {
                    stringBuilder.append(ifStatementToOllir(child, prefix));
                    prefix += ident;
                }
                case Consts.whileStatementNodeName -> {
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
            case Consts.terminalNodeName -> stringBuilder.append(terminalToOllir(child, "")).append(" &&.bool true.bool");
            case Consts.literalNodeName -> stringBuilder.append(literalToOllir(child, "")).append(" &&.bool true.bool");
            case Consts.binaryNodeName -> stringBuilder.append(binaryToOllir(child, prefix, before));
            case Consts.notExprNodeName -> stringBuilder.append(notExpressionToOllir(child, prefix, before));
            case Consts.callExprNodeName, Consts.propertyAccessNodeName,
                    Consts.arrayExprNodeName, Consts.newNodeName ->
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
            String type = OllirCodeUtils.typeToOllir(child.get(Consts.typeAttribute), child.getOptional(Consts.arrayAttribute));
            stringBuilder.append("ret").append(type).append(" ");
            switch (child.getKind()) {
                case Consts.literalNodeName -> stringBuilder.append(literalToOllir(child, ""));
                case Consts.terminalNodeName -> stringBuilder.append(terminalToOllir(child, prefix, before));
                case Consts.callExprNodeName, Consts.propertyAccessNodeName, Consts.notExprNodeName,
                        Consts.binaryNodeName, Consts.arrayExprNodeName, Consts.newNodeName ->
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
        String varName = (child0.getKind().equals(Consts.arrayExprNodeName) ?
                child0.getChildren().get(0).get(Consts.valueAttribute) :
                child0.get(Consts.valueAttribute));
        boolean isField = isField(varName);

        if (child1.get(Consts.typeAttribute).equals(Consts.autoType)) {
            child1.put(Consts.typeAttribute, child0.get(Consts.typeAttribute));
            child1.put(Consts.arrayAttribute, child0.getOptional(Consts.arrayAttribute).orElse("false"));
        }

        switch (child0.getKind()) {
            case Consts.terminalNodeName -> left.append(terminalToOllir(child0, ""));
            case Consts.arrayExprNodeName -> left.append(arrayExpressionToOllir(child0, prefix, before));
            default -> System.out.println("assignementToOllir: " + child0);
        }

        switch (child1.getKind()) {
            case Consts.terminalNodeName ->
                    right.append(terminalToOllir(child1, ""));
            case Consts.literalNodeName ->
                    right.append(literalToOllir(child1, ""));
            case Consts.newNodeName ->
                    right.append(isField ? makeLocalVar(child1, prefix, before) : newToOllir(child1, prefix, left.toString(), before));
            case Consts.notExprNodeName ->
                    right.append(isField ? makeLocalVar(child1, prefix, before) : notExpressionToOllir(child1, prefix, before));
            case Consts.binaryNodeName ->
                    right.append(isField ? makeLocalVar(child1, prefix, before) : binaryToOllir(child1, prefix, before));
            case Consts.callExprNodeName ->
                    right.append(isField ? makeLocalVar(child1, prefix, before) : callExpressionToOllir(child1, prefix, before, false));
            case Consts.arrayExprNodeName ->
                    right.append(isField ? makeLocalVar(child1, prefix, before) : arrayExpressionToOllir(child1, prefix, before));
            case Consts.propertyAccessNodeName ->
                    right.append(isField ? makeLocalVar(child1, prefix, before) : propertyAccessToOllir(child1, prefix, before));
            default -> System.out.println("assignementToOllir: " + child1);
        }

        if (!before.toString().equals(prefix)) {
            stringBuilder.append(before);
        }

        if (isField && !child0.getKind().equals(Consts.arrayExprNodeName)){
            stringBuilder.append(prefix)
                         .append("putfield(this, ")
                         .append(left).append(", ")
                         .append(right)
                         .append(").V;\n");
        }
        else {
            stringBuilder.append(prefix)
                         .append(left).append(" ")
                         .append(":=").append(OllirCodeUtils.typeToOllir(node.get(Consts.typeAttribute), node.getOptional(Consts.arrayAttribute))).append(" ")
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
        if (child.getOptional(Consts.valueAttribute).isEmpty() || isField(child.get(Consts.valueAttribute)))
            stringBuilder.append(makeLocalVar(child, prefix, before, false));
        else
            stringBuilder.append(terminalToOllir(child, "", false));
        stringBuilder.append("[");

        child = children.get(1);
        switch (child.getKind()) {
            case Consts.terminalNodeName ->
                    stringBuilder.append(terminalToOllir(child, prefix, before));
            case Consts.literalNodeName,
                    Consts.callExprNodeName, Consts.propertyAccessNodeName, Consts.notExprNodeName,
                    Consts.binaryNodeName, Consts.arrayExprNodeName, Consts.newNodeName ->
                    stringBuilder.append(makeLocalVar(child, prefix, before));
            default -> System.out.println("arrayExpressionToOllir: " + child);
        }

        stringBuilder.append("]").append(OllirCodeUtils.typeToOllir(node.get(Consts.typeAttribute), node.getOptional(Consts.arrayAttribute)));
        return stringBuilder.toString();
    }

    private String binaryToOllir(JmmNode node, String prefix, StringBuilder before) {
        StringBuilder stringBuilder = new StringBuilder();

        var child0 = node.getChildren().get(0);
        var child1 = node.getChildren().get(1);

        if (child0.get(Consts.typeAttribute).equals(Consts.autoType))
            child0.put(Consts.typeAttribute, child1.get(Consts.typeAttribute));
        else if (child1.get(Consts.typeAttribute).equals(Consts.autoType))
            child1.put(Consts.typeAttribute, child0.get(Consts.typeAttribute));

        switch (child0.getKind()) {
            case Consts.terminalNodeName -> {
                if (isField(child0.getOptional(Consts.valueAttribute).orElse("")))
                    stringBuilder.append(makeLocalVar(child0, prefix, before)).append(" ");
                else
                    stringBuilder.append(terminalToOllir(child0, "")).append(" ");
            }
            case Consts.literalNodeName -> stringBuilder.append(literalToOllir(child0, "")).append(" ");
            case Consts.callExprNodeName, Consts.propertyAccessNodeName, Consts.notExprNodeName,
                    Consts.binaryNodeName, Consts.arrayExprNodeName, Consts.newNodeName ->
                    stringBuilder.append(makeLocalVar(child0, prefix, before)).append(" ");
            default -> System.out.println("binaryToOllir: " + child0);
        }

        stringBuilder.append(node.get(Consts.valueAttribute));
        stringBuilder.append(node.get(Consts.valueAttribute).equals(Consts.lessThanExpression) ? // Hardcoded because at this point, all operations' types have been checked
                OllirCodeUtils.typeToOllir(Consts.intType, Optional.empty()) :
                OllirCodeUtils.typeToOllir(node.get(Consts.typeAttribute), node.getOptional(Consts.arrayAttribute))).append(" ");

        switch (child1.getKind()) {
            case Consts.terminalNodeName ->  {
                if (isField(child1.getOptional(Consts.valueAttribute).orElse("")))
                    stringBuilder.append(makeLocalVar(child1, prefix, before)).append(" ");
                else
                    stringBuilder.append(terminalToOllir(child1, "")).append(" ");
            }
            case Consts.literalNodeName -> stringBuilder.append(literalToOllir(child1, ""));
            case Consts.callExprNodeName, Consts.propertyAccessNodeName, Consts.notExprNodeName,
                    Consts.binaryNodeName, Consts.arrayExprNodeName, Consts.newNodeName ->
                stringBuilder.append(makeLocalVar(child1, prefix, before));
            default -> System.out.println("binaryToOllir: " + child1);
        }

        return stringBuilder.toString();
    }

    private String makeLocalVar(JmmNode child, String prefix, StringBuilder before) {
        return makeLocalVar(child, prefix, before, true);
    }

    private String makeLocalVar(JmmNode child, String prefix, StringBuilder before, boolean withType){
        String typeToOllir = OllirCodeUtils.typeToOllir(child.get(Consts.typeAttribute), child.getOptional(Consts.arrayAttribute));
        String result = "";
        switch (child.getKind()){
            case Consts.callExprNodeName-> result = callExpressionToOllir(child, prefix, before, false);
            case Consts.propertyAccessNodeName -> result = propertyAccessToOllir(child, prefix, before);
            case Consts.notExprNodeName -> result = notExpressionToOllir(child, prefix, before);
            case Consts.binaryNodeName -> result = binaryToOllir(child, prefix, before);
            case Consts.arrayExprNodeName -> result = arrayExpressionToOllir(child, prefix, before);
            case Consts.newNodeName -> result = newToOllir(child, prefix, "t" + tempVarCounter + typeToOllir, before);
            case Consts.literalNodeName -> result = literalToOllir(child, "");
            case Consts.terminalNodeName -> result = terminalToOllir(child, "");
        }

        before.append(prefix).append("t").append(tempVarCounter).append(typeToOllir)
                .append(" :=").append(typeToOllir).append(" ")
                .append(result)
                .append(";\n");

        return "t" + tempVarCounter++ + (withType ? typeToOllir : "");
    }

    private String propertyAccessToOllir(JmmNode node, String prefix, StringBuilder before) {
        StringBuilder stringBuilder = new StringBuilder();

        switch (node.get(Consts.valueAttribute)) {
            case Consts.lengthProperty -> stringBuilder.append("arraylength(");
            default -> System.out.println("propertyAccessToOllir: " + node);
        }

        var child = node.getChildren().get(0);
        switch (child.getKind()) {
            case Consts.terminalNodeName -> stringBuilder.append(terminalToOllir(child, prefix, before));
            case Consts.literalNodeName -> stringBuilder.append(literalToOllir(child, ""));
            case Consts.callExprNodeName, Consts.propertyAccessNodeName, Consts.notExprNodeName,
                    Consts.binaryNodeName, Consts.arrayExprNodeName, Consts.newNodeName ->
                    stringBuilder.append(makeLocalVar(child, prefix, before));
            default -> System.out.println("propertyAccessToOllir: " + child);
        }

        stringBuilder.append(")").append(OllirCodeUtils.typeToOllir(child.get(Consts.typeAttribute), Optional.empty()));

        return stringBuilder.toString();
    }

    private String callExpressionToOllir(JmmNode node, String prefix, StringBuilder before, boolean insideMethod) {
        StringBuilder stringBuilder = new StringBuilder();
        StringBuilder beforeArgs = new StringBuilder();

        var children = node.getChildren();

        var child0 = children.get(0);

        if (imports.contains(child0.get(Consts.valueAttribute)) || node.getOptional(Consts.staticAttribute).isPresent()) {
            stringBuilder.append("invokestatic(");
            stringBuilder.append(child0.get(Consts.typeAttribute));
        } else {
            stringBuilder.append("invokevirtual(");
            if (child0.get(Consts.valueAttribute).equals(Consts.thisAttribute)) {
                stringBuilder.append("this");
            } else {
                switch (child0.getKind()) {
                    case Consts.terminalNodeName -> stringBuilder.append(terminalToOllir(child0, prefix, before));
                    case Consts.literalNodeName -> stringBuilder.append(literalToOllir(child0, ""));
                    case Consts.callExprNodeName, Consts.propertyAccessNodeName, Consts.notExprNodeName,
                            Consts.binaryNodeName, Consts.arrayExprNodeName, Consts.newNodeName ->
                            stringBuilder.append(makeLocalVar(child0, prefix, before));
                    default -> System.out.println("callExpressionToOllirLeftSide: " + child0);
                }
            }
        }

        stringBuilder.append(", \"").append(node.get(Consts.nameAttribute)).append("\"");

        if (children.size() > 1) {
            var args = children.get(1).getChildren();
            for (var arg : args) {
                stringBuilder.append(", ");
                switch (arg.getKind()) {
                    case Consts.terminalNodeName -> stringBuilder.append(terminalToOllir(arg, prefix, insideMethod ? beforeArgs : before));
                    case Consts.literalNodeName -> stringBuilder.append(literalToOllir(arg, ""));
                    case Consts.callExprNodeName, Consts.propertyAccessNodeName, Consts.notExprNodeName,
                            Consts.binaryNodeName, Consts.arrayExprNodeName, Consts.newNodeName ->
                            stringBuilder.append(makeLocalVar(arg, prefix, insideMethod ? beforeArgs : before));
                    default -> System.out.println("callExpressionToOllir: " + arg);
                }
            }
        }

        stringBuilder.append(")").
                append(OllirCodeUtils.typeToOllir(node.get(Consts.typeAttribute), node.getOptional(Consts.arrayAttribute)));

        return (insideMethod ? beforeArgs + prefix : "") + stringBuilder;
    }

    private String newToOllir(JmmNode node, String prefix, String left, StringBuilder before) {
        StringBuilder stringBuilder = new StringBuilder();

        var isArray = node.getOptional(Consts.arrayAttribute);
        stringBuilder.append("new(")
                .append(isArray.isPresent() && isArray.get().equals("true") ? "array" : node.get(Consts.typeAttribute));

        var children = node.getChildren();
        if (children.size() > 0) {
            var child = children.get(0);
            stringBuilder.append(", ");
            switch (child.getKind()) {
                case Consts.terminalNodeName -> stringBuilder.append(terminalToOllir(child, ""));
                case Consts.literalNodeName -> stringBuilder.append(literalToOllir(child, ""));
                case Consts.callExprNodeName, Consts.propertyAccessNodeName, Consts.notExprNodeName,
                        Consts.binaryNodeName, Consts.arrayExprNodeName, Consts.newNodeName ->
                        stringBuilder.append(makeLocalVar(child, prefix, before)).append(" ");
                default -> System.out.println("newToOllir: " + child);
            }
        }

        stringBuilder.append(")");
        stringBuilder.append(OllirCodeUtils.typeToOllir(node.get(Consts.typeAttribute), node.getOptional(Consts.arrayAttribute)));

        if (isArray.isEmpty() || isArray.get().equals("false"))
            stringBuilder.append(";\n").append(prefix).append("invokespecial(").append(left).append(", \"<init>\").V");

        return stringBuilder.toString();
    }

    private String literalToOllir(JmmNode node, String prefix) {
        StringBuilder stringBuilder = new StringBuilder(prefix);

        String type = node.get(Consts.typeAttribute);
        switch (type) {
            case Consts.intType, Consts.booleanType -> {
                stringBuilder.append(node.get(Consts.valueAttribute));
                stringBuilder.append(OllirCodeUtils.typeToOllir(type, node.getOptional(Consts.arrayAttribute)));
            }
            default -> System.out.println("literalToOllir: " + type);
        }

        return stringBuilder.toString();
    }

    private String terminalToOllir(JmmNode node, String prefix, boolean withType) {
        StringBuilder stringBuilder = new StringBuilder(prefix);
        String varName = node.get(Consts.valueAttribute);
        JmmNode parent = node.getParent();
        String leftName = parent.getKind().equals(Consts.assignmentNodeName) ?
                    parent.getChildren().get(0).getOptional(Consts.valueAttribute).orElse("") : "";

        if (!isField(varName)){ // Is local var or parameter
            if (varName.equals(Consts.thisAttribute)){
                stringBuilder.append("$0.").append(Consts.thisAttribute);
                stringBuilder.append(OllirCodeUtils.typeToOllir(node.get(Consts.typeAttribute), node.getOptional(Consts.arrayAttribute)));
                return stringBuilder.toString();
            }
            for (int i = 0; i < methodParameters.size(); i++) {
                if (methodParameters.get(i).equals(varName)) {
                    stringBuilder.append("$").append(i + 1).append(".");
                }
            }
        }
        else if (!isField(leftName)) { // Is a field and isn't being assigned to a field
            String type = OllirCodeUtils.typeToOllir(node.get(Consts.typeAttribute), node.getOptional(Consts.arrayAttribute));
            stringBuilder.append("getfield(this, ")
                    .append(varName).append(type)
                    .append(")").append(type);
            return stringBuilder.toString();
        }

        stringBuilder.append(varName);
        if (withType)
            stringBuilder.append(OllirCodeUtils.typeToOllir(
                    node.get(Consts.typeAttribute),
                    node.getOptional(Consts.arrayAttribute)));

        return stringBuilder.toString();
    }

    private String terminalToOllir(JmmNode node, String prefix) {
        return terminalToOllir(node, prefix, true);
    }

    private String terminalToOllir(JmmNode node, String prefix, StringBuilder before) {
        return terminalToOllir(node, prefix, before, true);
    }

    private String terminalToOllir(JmmNode node, String prefix, StringBuilder before , boolean withType) {
        if (isField(node.getOptional(Consts.valueAttribute).orElse("")))
            return makeLocalVar(node, prefix, before, withType);
        else
            return terminalToOllir(node, "", withType);
    }
}
