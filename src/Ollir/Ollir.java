import pt.up.fe.comp.jmm.JmmNode;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class Ollir {
    private final String ident = "  ";
    private List<String> methodParameters = new ArrayList<String>();    // All the parameters of the current function
    private List<String> imports = new ArrayList<String>();
    int nextTempVariable = 1;
    public String getCode(JmmNode root) {
        StringBuilder stringBuilder = new StringBuilder();

        for (var node: root.getChildren()) {
            switch (node.getKind()) {
                case Constants.importDeclNodeName -> imports.add(node.get(Constants.nameAttribute));
                case Constants.classDeclNodeName -> stringBuilder.append(classDeclarationToOllir(node, ""));
                default -> System.out.println("getCode: " + node);
            }
        }

        return stringBuilder.toString();
    }

    private String classDeclarationToOllir(JmmNode node, String prefix) {
        StringBuilder classOllir = new StringBuilder();

        classOllir.append(node.get(Constants.nameAttribute)).append(" {");

        StringBuilder methodsOllir = new StringBuilder();
        StringBuilder fieldsOllir = new StringBuilder();
        for (var child: node.getChildren()) {
            switch (child.getKind()) {
                case Constants.methodDeclNodeName -> methodsOllir.append("\n").append(methodDeclarationToOllir(child, prefix + ident));
                case Constants.fieldDeclNodeName -> fieldsOllir.append("\n").append(fieldDeclarationToOllir(child, prefix + ident));
                default -> System.out.println("classDeclarationToOllir: " + child);
            }
        }

        // Put everything in order, first fields, then constructor, then methods.

        classOllir.append(fieldsOllir);
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
                fieldOllir.append(".field private ").append(child.get(Constants.nameAttribute));
                fieldOllir.append(OllirCodeUtils.typeToOllir(child.get(Constants.typeAttribute), child.getOptional(Constants.arrayAttribute)));
                fieldOllir.append("\n");
            }
            default -> System.out.println("fieldDeclarationToOllir: " + child);
        }

        return fieldOllir.toString();
    }

    private String methodDeclarationToOllir(JmmNode node, String prefix) {
        StringBuilder methodOllir = new StringBuilder(prefix);

        methodParameters = new ArrayList<String>();
        nextTempVariable = 1;

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
                case Constants.callExprNodeName -> insideMethod.append(prefix + ident).append(callExpressionToOllir(child, prefix + ident, insideMethod));
                case Constants.returnNodeName -> insideMethod.append(returnToOllir(child, prefix + ident));
                case Constants.varDeclNodeName -> {}
                case Constants.ifStatementNodeName -> insideMethod.append(ifStatementToOllir(child, prefix + ident));
                case Constants.whileStatementNodeName -> insideMethod.append("\n").append(whileStatementToOllir(child, prefix + ident));
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

        stringBuilder.append("Loop:\n");
        stringBuilder.append(whileConditionToOllir(children.get(0), prefix + ident)).append("\n");
        stringBuilder.append(whileBodyToOllir(children.get(1), prefix + ident));
        stringBuilder.append(prefix + ident + ident).append("goto Loop;\n");

        return stringBuilder.append(prefix).append("End:\n").toString();
    }

    private String whileBodyToOllir(JmmNode node, String prefix) {
        StringBuilder stringBuilder = new StringBuilder();
        StringBuilder before = new StringBuilder();

        for (var child: node.getChildren()) {
            switch (child.getKind()) {
                case Constants.assignmentNodeName -> stringBuilder.append(assignmentToOllir(child, prefix + ident));
                case Constants.ifStatementNodeName -> stringBuilder.append(ifStatementToOllir(child, prefix + ident));
                case Constants.callExprNodeName -> stringBuilder.append(callExpressionToOllir(child, prefix + ident, before));
                default -> System.out.println("whileBodyToOllir: " + child);
            }
        }

        return before.append(stringBuilder).toString();
    }

    private String whileConditionToOllir(JmmNode node, String prefix) {
        StringBuilder stringBuilder = new StringBuilder(prefix);
        StringBuilder before = new StringBuilder();

        var child = node.getChildren().get(0);

        StringBuilder ifCondition = new StringBuilder();

        switch (child.getKind()) {
            case Constants.binaryNodeName -> ifCondition.append(binaryToOllir(child, prefix, before));
            default -> System.out.println("whileConditionToOllir: " + child);
        }

        stringBuilder.append(before);
        stringBuilder.append("if (").append(ifCondition).append(") goto End;");

        return stringBuilder.toString();
    }

    private String ifStatementToOllir(JmmNode node, String prefix) {
        StringBuilder stringBuilder = new StringBuilder(prefix);
        StringBuilder ifStatement = new StringBuilder();
        StringBuilder elseStatement = new StringBuilder();

        // Since an else is always necessary I changed if and else order to simplify code.

        var children = node.getChildren();

        int i = 1;
        var child = children.get(1);
        while (!child.getKind().equals(Constants.elseStatementNodeName)) {
            switch (child.getKind()) {
                case Constants.assignmentNodeName -> ifStatement.append(assignmentToOllir(child, prefix + ident));
                default -> System.out.println("ifStatementToOllir: " + child);
            }
            i++;
            child = children.get(i);
        }

        elseStatement.append(elseStatementToOllir(child, prefix + ident));

        stringBuilder.append("if (");
        stringBuilder.append(ifConditionToOllir(children.get(0), ""));
        stringBuilder.append(") goto else;\n");
        stringBuilder.append(elseStatement);
        stringBuilder.append(prefix).append(ident).append("goto endif;\n");
        stringBuilder.append(prefix).append("else:\n");
        stringBuilder.append(ifStatement);
        stringBuilder.append(prefix).append("endif:\n");

        return stringBuilder.toString();
    }

    private String elseStatementToOllir(JmmNode node, String prefix) {
        StringBuilder stringBuilder = new StringBuilder();

        var children = node.getChildren();
        for (var child: children) {
            switch (child.getKind()) {
                case Constants.assignmentNodeName -> stringBuilder.append(assignmentToOllir(child, prefix));
                default -> System.out.println("elseStatementToOllir: " + child);
            }
        }

        return stringBuilder.toString();
    }

    private String ifConditionToOllir(JmmNode node, String prefix) {
        StringBuilder stringBuilder = new StringBuilder(prefix);
        StringBuilder before = new StringBuilder();

        var child = node.getChildren().get(0);
        switch (child.getKind()) {
            case Constants.binaryNodeName -> stringBuilder.append(binaryToOllir(child, "", before));
            default -> System.out.println("ifConditionToOllir: " + child);
        }

        return stringBuilder.toString();
    }

    private String returnToOllir(JmmNode node, String prefix) {
        StringBuilder stringBuilder = new StringBuilder(prefix);

        if (node.getChildren().size() == 0) {
            stringBuilder.append("ret.V");
        } else {
            var child = node.getChildren().get(0);
            String type = OllirCodeUtils.typeToOllir(child.get(Constants.typeAttribute), child.getOptional(Constants.arrayAttribute));
            stringBuilder.append("ret").append(type).append(" ");
            stringBuilder.append(child.getOptional(Constants.valueAttribute)).append(type);
        }
        return stringBuilder.append(";\n").toString();
    }

    private String assignmentToOllir(JmmNode node, String prefix) {
        StringBuilder stringBuilder = new StringBuilder();

        var children = node.getChildren();

        StringBuilder left = new StringBuilder();
        StringBuilder right = new StringBuilder();
        StringBuilder before = new StringBuilder(prefix);

        var child = children.get(0);

        switch (child.getKind()) {
            case Constants.terminalNodeName -> left.append(terminalToOllir(child, ""));
            case Constants.arrayExprNodeName -> left.append(arrayExpressionToOllir(child, "")).append(" ");
            default -> System.out.println("assignementToOllir: " + child);
        }

        child = children.get(1);
        switch (child.getKind()) {
            case Constants.terminalNodeName -> right.append(terminalToOllir(child, "")).append(";\n");
            case Constants.literalNodeName -> right.append(literalToOllir(child, "")).append(";\n");
            case Constants.newNodeName -> {
                right.append(newToOllir(child, prefix, before)).append(";\n");
                var isArray = child.getOptional(Constants.arrayAttribute);
                if (isArray.isEmpty() || isArray.get().equals("false"))
                    right.append(prefix).append("invokespecial(").append(left).append(", \"<init>\").V;\n");
            }
            case Constants.binaryNodeName -> right.append(binaryToOllir(child, prefix, before)).append(";\n");
            case Constants.callExprNodeName -> right.append(callExpressionToOllir(child, prefix, before));
            case Constants.arrayExprNodeName -> right.append(arrayExpressionToOllir(child, "")).append(";\n");
            default -> System.out.println("assignementToOllir: " + child);
        }

        if (!before.toString().equals(prefix)) {
            stringBuilder.append(before);
        }
        stringBuilder.append(prefix).append(left).append(" ");
        stringBuilder.append(":=").append(OllirCodeUtils.typeToOllir(node.get(Constants.typeAttribute), node.getOptional(Constants.arrayAttribute))).append(" ");
        stringBuilder.append(right);

        return  stringBuilder.toString();
    }

    private String arrayExpressionToOllir(JmmNode node, String prefix) {
        StringBuilder stringBuilder = new StringBuilder(prefix);

        var children = node.getChildren();

        var child = children.get(0);
        for (int i = 0; i < methodParameters.size(); i++) {
            if (methodParameters.get(i).equals(child.get(Constants.valueAttribute))) {
                stringBuilder.append("$").append(i + 1).append(".");
            }
        }
        stringBuilder.append(children.get(0).get(Constants.valueAttribute)).append("[");

        child = children.get(1);
        switch (child.getKind()) {
            case Constants.literalNodeName -> stringBuilder.append(literalToOllir(child, ""));
            case Constants.terminalNodeName -> stringBuilder.append(terminalToOllir(child, ""));
            default -> System.out.println("arrayExpressionToOllir: " + child);
        }

        stringBuilder.append("]").append(OllirCodeUtils.typeToOllir(node.get(Constants.typeAttribute), node.getOptional(Constants.arrayAttribute)));
        return stringBuilder.toString();
    }

    private String binaryToOllir(JmmNode node, String prefix, StringBuilder before) {
        StringBuilder stringBuilder = new StringBuilder();

        var child0 = node.getChildren().get(0);
        var child1 = node.getChildren().get(1);

        switch (child0.getKind()) {
            case Constants.terminalNodeName -> stringBuilder.append(terminalToOllir(child0, "")).append(" ");
            case Constants.literalNodeName -> stringBuilder.append(literalToOllir(child0, "")).append(" ");
            case Constants.binaryNodeName -> stringBuilder.append(binaryToOllir(child0, "", before)).append(" ");
            default -> System.out.println("binaryToOllir: " + child0);
        }

        stringBuilder.append(node.get(Constants.valueAttribute));
        stringBuilder.append(OllirCodeUtils.typeToOllir(node.get(Constants.typeAttribute), node.getOptional(Constants.arrayAttribute))).append(" ");


        switch (child1.getKind()) {
            case Constants.terminalNodeName -> stringBuilder.append(terminalToOllir(child1, "")).append(" ");
            case Constants.literalNodeName -> stringBuilder.append(literalToOllir(child1, "")).append(" ");
            case Constants.binaryNodeName -> stringBuilder.append(binaryToOllir(child1, "", before)).append(" ");
            case Constants.callExprNodeName -> {
                String typeToOllir = OllirCodeUtils.typeToOllir(child1.get(Constants.typeAttribute), child1.getOptional(Constants.arrayAttribute));
                before.append("t").append(nextTempVariable).append(typeToOllir);
                before.append(" :=").append(typeToOllir).append(" ");
                before.append(callExpressionToOllir(child1, prefix, before));
                stringBuilder.append("t").append(String.valueOf(nextTempVariable)).append(typeToOllir);
                nextTempVariable++;
            }
            case Constants.propertyAccessNodeName -> {
                String typeToOllir = OllirCodeUtils.typeToOllir(child1.get(Constants.typeAttribute), child1.getOptional(Constants.arrayAttribute));
                before.append("t").append(nextTempVariable).append(typeToOllir);
                before.append(" :=").append(typeToOllir).append(" ");
                stringBuilder.append(propertyAccessToOllir(child1, "", before));
                stringBuilder.append("t").append(String.valueOf(nextTempVariable)).append(typeToOllir);
                before.append(prefix);
                nextTempVariable++;
            }
            default -> System.out.println("binaryToOllir: " + child1);
        }

        return stringBuilder.toString();
    }

    private String propertyAccessToOllir(JmmNode node, String prefix, StringBuilder before) {
        StringBuilder stringBuilder = new StringBuilder(prefix);

        switch (node.get(Constants.valueAttribute)) {
            case Constants.lengthProperty -> before.append("arraylength(");
            default -> System.out.println("propertyAccessToOllir: " + node);
        }

        var child = node.getChildren().get(0);
        before.append(terminalToOllir(child, ""));

        before.append(")").append(OllirCodeUtils.typeToOllir(child.get(Constants.typeAttribute), Optional.empty()));
        before.append(";\n");

        return stringBuilder.toString();
    }

    private String callExpressionToOllir(JmmNode node, String prefix, StringBuilder before) {
        StringBuilder stringBuilder = new StringBuilder();

        var children = node.getChildren();
        String type = OllirCodeUtils.typeToOllir(node.get(Constants.typeAttribute), node.getOptional(Constants.arrayAttribute));

        if (imports.contains(children.get(0).get(Constants.typeAttribute))) {
            stringBuilder.append("invokestatic(");
            stringBuilder.append(children.get(0).get(Constants.typeAttribute));
        } else {
            stringBuilder.append("invokevirtual(");
            if (children.get(0).get(Constants.valueAttribute).equals(Constants.thisAttribute)) {
                stringBuilder.append("this");
            } else {
                stringBuilder.append(children.get(0).get(Constants.valueAttribute));
                stringBuilder.append(OllirCodeUtils.typeToOllir(children.get(0).get(Constants.typeAttribute), node.getOptional(Constants.arrayAttribute)));
            }
        }

        stringBuilder.append(", \"").append(node.get(Constants.nameAttribute)).append("\"");

        if (children.size() > 1) {
            var args = children.get(1).getChildren();
            for (var arg : args) {
                stringBuilder.append(", ");
                switch (arg.getKind()) {
                    case Constants.terminalNodeName -> stringBuilder.append(terminalToOllir(arg, ""));
                    case Constants.callExprNodeName -> {
                        String typeToOllir = OllirCodeUtils.typeToOllir(arg.get(Constants.typeAttribute), arg.getOptional(Constants.arrayAttribute));
                        before.append("t").append(nextTempVariable).append(typeToOllir);
                        before.append(" :=").append(typeToOllir).append(" ");
                        before.append(callExpressionToOllir(arg, prefix, before));
                        stringBuilder.append("t").append(nextTempVariable).append(typeToOllir);
                        nextTempVariable++;
                    }
                    case Constants.literalNodeName -> stringBuilder.append(literalToOllir(arg, ""));
                    case Constants.binaryNodeName -> {
                        before.append(binaryToOllir(arg, "", before)).append(";\n");
                        String typeToOllir = OllirCodeUtils.typeToOllir(arg.get(Constants.typeAttribute), arg.getOptional(Constants.arrayAttribute));
                        stringBuilder.append("t").append(nextTempVariable).append(typeToOllir);
                        nextTempVariable++;
                        before.append(prefix).append("t").append(nextTempVariable).append(typeToOllir);
                        before.append(" :=").append(typeToOllir).append(" ");
                    }
                    case Constants.arrayExprNodeName -> {
                        String typeToOllir = OllirCodeUtils.typeToOllir(arg.get(Constants.typeAttribute), arg.getOptional(Constants.arrayAttribute));
                        stringBuilder.append("t").append(nextTempVariable).append(typeToOllir);
                        before.append(prefix).append("t").append(nextTempVariable).append(typeToOllir);
                        before.append(" :=").append(typeToOllir).append(" ");
                        before.append(arrayExpressionToOllir(arg, "")).append("\n").append(prefix);
                        nextTempVariable++;
                    }
                    default -> System.out.println("callExpressionToOllir: " + arg);
                }
            }
        }

        stringBuilder.append(")").append(OllirCodeUtils.typeToOllir(node.get(Constants.typeAttribute), node.getOptional(Constants.arrayAttribute)));
        stringBuilder.append(";\n");

        //before.append(stringBuilder);

        return stringBuilder.toString();
    }

    private String newToOllir(JmmNode node, String prefix, StringBuilder parentBuilder) {
        StringBuilder stringBuilder = new StringBuilder();

        var isArray = node.getOptional(Constants.arrayAttribute);
        stringBuilder.append("new(").append(isArray.isPresent() && isArray.get().equals("true") ? "array" : node.get(Constants.typeAttribute));

        var children = node.getChildren();
        if (children.size() > 0) {
            var child = children.get(0);
            stringBuilder.append(", ");
            switch (child.getKind()) {
                case Constants.literalNodeName -> stringBuilder.append(literalToOllir(child, ""));
                default -> System.out.println("newToOllir: " + child);
            }
        }

        stringBuilder.append(")");
        stringBuilder.append(OllirCodeUtils.typeToOllir(node.get(Constants.typeAttribute), node.getOptional(Constants.arrayAttribute)));

        return stringBuilder.toString();
    }

    private String literalToOllir(JmmNode node, String prefix) {
        StringBuilder stringBuilder = new StringBuilder(prefix);

        String type = node.get(Constants.typeAttribute);
        switch (type) {
            case Constants.intType -> {
                stringBuilder.append(node.get(Constants.valueAttribute));
                stringBuilder.append(OllirCodeUtils.typeToOllir(type, node.getOptional(Constants.arrayAttribute)));
            }
            default -> System.out.println("literalToOllir: " + type);
        }

        return stringBuilder.toString();
    }

    private String terminalToOllir(JmmNode node, String prefix) {
        StringBuilder stringBuilder = new StringBuilder(prefix);

        for (int i = 0; i < methodParameters.size(); i++) {
            if (methodParameters.get(i).equals(node.get(Constants.valueAttribute))) {
                stringBuilder.append("$").append(i + 1).append(".");
            }
        }


        stringBuilder.append(node.get(Constants.valueAttribute));
        stringBuilder.append(OllirCodeUtils.typeToOllir(node.get(Constants.typeAttribute), node.getOptional(Constants.arrayAttribute)));

        return stringBuilder.toString();
    }
}
