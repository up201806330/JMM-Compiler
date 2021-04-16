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
                case Constants.importDeclNodeName:
                    imports.add(node.get(Constants.nameAttribute));
                    break;
                case Constants.classDeclNodeName:
                    stringBuilder.append(classDeclarationToOllir(node, ""));
                    break;
                default:
                    System.out.println("getCode: " + node);
                    break;
            }
        }

        return stringBuilder.toString();
    }

    private String classDeclarationToOllir(JmmNode node, String prefix) {
        StringBuilder stringBuilder = new StringBuilder();

        stringBuilder.append(node.get(Constants.nameAttribute)).append(" {");

        StringBuilder insideClass = new StringBuilder();
        for (var child: node.getChildren()) {
            switch (child.getKind()) {
                case Constants.methodDeclNodeName:
                    insideClass.append("\n").append(prefix).append(ident);
                    insideClass.append(methodDeclarationToOllir(child, prefix+ ident));
                    break;
                default:
                    System.out.println(child);
                    break;
            }
        }

        stringBuilder.append("\n").append(prefix).append(ident);
        stringBuilder.append(OllirCodeUtils.defaultConstructor(ident, node.get(Constants.nameAttribute)));

        stringBuilder.append(insideClass);
        stringBuilder.append(prefix).append("}");

        return stringBuilder.toString();
    }

    private String methodDeclarationToOllir(JmmNode node, String prefix) {
        StringBuilder stringBuilder = new StringBuilder();

        methodParameters = new ArrayList<String>();
        nextTempVariable = 1;

        stringBuilder.append(".method public ");
        Optional<String> staticAttribute = node.getOptional(Constants.staticAttribute);
        if (staticAttribute.isPresent()) {
            stringBuilder.append("static ");
        }
        stringBuilder.append(node.get(Constants.nameAttribute)).append("(");

        var children = node.getChildren();

        List<JmmNode> parameters = new ArrayList<JmmNode>();
        JmmNode type = null;

        StringBuilder insideMethod = new StringBuilder();



        for (var child: children) {
            switch (child.getKind()) {
                case Constants.typeNodeName -> type = child;
                case Constants.methodParamNodeName -> { parameters.add(child); methodParameters.add(child.get(Constants.nameAttribute)); }
                case Constants.assignmentNodeName -> insideMethod.append(assignmentToOllir(child, prefix + ident));
                case Constants.callExprNodeName -> insideMethod.append(callExpressionToOllir(child, prefix + ident, insideMethod.append(prefix + ident)));
                case Constants.returnNodeName -> insideMethod.append(returnToOllir(child, prefix + ident));
                case Constants.varDeclNodeName -> {}
                case Constants.ifStatementNodeName -> insideMethod.append(ifStatementToOllir(child, prefix + ident));
                default -> System.out.println("methodDeclarationToOllir: " + child);
            }
        }

        if (parameters.size() > 0) {
            for (int i = 0; i < parameters.size(); i++) {
                stringBuilder.append(methodParameterToOllir(parameters.get(i), ""));
                if (i != parameters.size() - 1) {
                    stringBuilder.append(", ");
                }
            }
        } else if (node.get(Constants.nameAttribute).equals(Constants.mainMethod)) {
            stringBuilder.append("args.array.String");
        }
        stringBuilder.append(")");

        if (type != null)
            stringBuilder.append(OllirCodeUtils.typeToOllir(type.get(Constants.typeAttribute), node.getOptional(Constants.arrayAttribute)));
        else
            stringBuilder.append(OllirCodeUtils.typeToOllir(Constants.voidType, node.getOptional(Constants.arrayAttribute)));

        stringBuilder.append(" {\n");
        stringBuilder.append(insideMethod);
        stringBuilder.append(prefix).append("}\n");

        return stringBuilder.toString();
    }

    private String ifStatementToOllir(JmmNode node, String prefix) {
        StringBuilder stringBuilder = new StringBuilder(prefix);

        stringBuilder.append("if (");

        var children = node.getChildren();
        stringBuilder.append(ifConditionToOllir(children.get(0), ""));
        stringBuilder.append(") goto else;\n");

        var child = children.get(1);
        int i = 2;
        while (!child.getKind().equals(Constants.elseStatementNodeName)) {
            switch (child.getKind()) {
                case Constants.assignmentNodeName -> stringBuilder.append(assignmentToOllir(child, prefix + ident));
                default -> System.out.println("ifStatementToOllir: " + child);
            }
            child = children.get(i);
            i++;
        }

        stringBuilder.append(prefix).append("goto endif;\n");
        stringBuilder.append(prefix).append("else:\n");

        stringBuilder.append(elseStatementToOllir(children.get(2), prefix + ident));

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
            String type = OllirCodeUtils.typeToOllir(child.get(Constants.typeAttribute), node.getOptional(Constants.arrayAttribute));
            stringBuilder.append("ret").append(type).append(" ");
            stringBuilder.append(child.get(Constants.valueAttribute)).append(type);
        }
        return stringBuilder.append("\n").toString();
    }

    private String assignmentToOllir(JmmNode node, String prefix) {
        StringBuilder stringBuilder = new StringBuilder();

        var children = node.getChildren();

        StringBuilder left = new StringBuilder(prefix);
        StringBuilder right = new StringBuilder();
        StringBuilder before = new StringBuilder();

        var child = children.get(0);

        switch (child.getKind()) {
            case Constants.terminalNodeName -> left.append(terminalToOllir(child, "")).append(" ");
            default -> System.out.println("assignementToOllir: " + child);
        }

        child = children.get(1);
        switch (child.getKind()) {
            case Constants.terminalNodeName -> right.append(terminalToOllir(child, ""));
            case Constants.literalNodeName -> right.append(literalToOllir(child, ""));
            case Constants.newNodeName -> right.append(newToOllir(child, "", before));
            case Constants.binaryNodeName -> right.append(binaryToOllir(child, prefix, before));
            case Constants.callExprNodeName -> {
                String typeToOllir = OllirCodeUtils.typeToOllir(child.get(Constants.typeAttribute), child.getOptional(Constants.arrayAttribute));
                before.append("t").append(nextTempVariable).append(typeToOllir);
                before.append(" :=").append(typeToOllir).append(" ");
                right.append(callExpressionToOllir(child, "", before));
                right.append("t").append(String.valueOf(nextTempVariable)).append(typeToOllir);
                nextTempVariable++;
            }
            default -> System.out.println("assignementToOllir: " + child);
        }

        if (!before.toString().equals("")) {
            before.insert(0, prefix);
            stringBuilder.append(before);
        }
        stringBuilder.append(left);
        stringBuilder.append(":=").append(OllirCodeUtils.typeToOllir(node.get(Constants.typeAttribute), node.getOptional(Constants.arrayAttribute))).append(" ");
        stringBuilder.append(right);

        return  stringBuilder.append(";\n").toString();
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
                stringBuilder.append(callExpressionToOllir(child1, "", before));
                stringBuilder.append("t").append(String.valueOf(nextTempVariable)).append(typeToOllir);
                nextTempVariable++;
            }
            default -> System.out.println("binaryToOllir: " + child1);
        }

        return stringBuilder.toString();
    }

    private String callExpressionToOllir(JmmNode node, String prefix, StringBuilder before) {
        StringBuilder stringBuilder = new StringBuilder(prefix);

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
                        stringBuilder.append(callExpressionToOllir(arg, "", before));
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
                    default -> System.out.println("callExpressionToOllir: " + arg);
                }
            }
        }

        stringBuilder.append(")").append(OllirCodeUtils.typeToOllir(node.get(Constants.typeAttribute), node.getOptional(Constants.arrayAttribute)));
        stringBuilder.append(";\n");

        before.append(stringBuilder);

        return "";
    }

    private String newToOllir(JmmNode node, String prefix, StringBuilder parentBuilder) {
        StringBuilder stringBuilder = new StringBuilder(prefix);

        stringBuilder.append("new(").append(node.get(Constants.typeAttribute)).append(")");
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
            default -> System.out.println(type);
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

    private String methodParameterToOllir(JmmNode node, String prefix) {
        StringBuilder stringBuilder = new StringBuilder(prefix);

        stringBuilder.append(node.get(Constants.nameAttribute));

        Optional<String> type = node.getOptional(Constants.typeAttribute);
        Optional<String> isArray = node.getOptional(Constants.arrayAttribute);

        if (type.isPresent() && isArray.isPresent()) stringBuilder.append(OllirCodeUtils.typeToOllir(type.get(), isArray));

        return stringBuilder.toString();
    }


}
