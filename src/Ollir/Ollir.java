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
                    //System.out.println("Type not yet implemented");
                    break;
            }
        }

        return stringBuilder.toString();
    }

    private String classDeclarationToOllir(JmmNode node, String prefix) {
        StringBuilder stringBuilder = new StringBuilder(prefix);

        stringBuilder.append(node.get(Constants.nameAttribute)).append(" {");

        for (var child: node.getChildren()) {
            switch (child.getKind()) {
                case Constants.methodDeclNodeName:
                    stringBuilder.append("\n").append(methodDeclarationToOllir(child, prefix+ ident));
                    break;
                default:
                    System.out.println(child);
                    break;
            }
        }

        stringBuilder.append(prefix).append("}");

        return stringBuilder.toString();
    }

    private String methodDeclarationToOllir(JmmNode node, String prefix) {
        StringBuilder stringBuilder = new StringBuilder(prefix);

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
            stringBuilder.append(typeToOllir(type.get(Constants.typeAttribute), node.getOptional(Constants.arrayAttribute)));
        else
            stringBuilder.append(typeToOllir(Constants.voidType, node.getOptional(Constants.arrayAttribute)));

        stringBuilder.append(" {\n");
        stringBuilder.append(insideMethod);
        stringBuilder.append(prefix).append("}\n");

        return stringBuilder.toString();
    }

    private String returnToOllir(JmmNode node, String prefix) {
        StringBuilder stringBuilder = new StringBuilder(prefix);

        if (node.getChildren().size() == 0) {
            stringBuilder.append("ret.V");
        } else {
            var child = node.getChildren().get(0);
            String type = typeToOllir(child.get(Constants.typeAttribute), node.getOptional(Constants.arrayAttribute));
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
            case Constants.terminalNodeName:
                left.append(terminalToOllir(child, "")).append(" ");
                break;
            default:
                System.out.println(child);
                System.out.println("Not yet implemented");
                break;
        }

        child = children.get(1);
        switch (child.getKind()) {
            case Constants.terminalNodeName:
                right.append(terminalToOllir(child, ""));
                break;
            case Constants.literalNodeName:
                right.append(literalToOllir(child, ""));
                break;
            case Constants.newNodeName:
                right.append(newToOllir(child, "", before));
                break;
            case Constants.binaryNodeName:
                right.append(binaryToOllir(child, "", before));
                break;
            case Constants.callExprNodeName:
                before.append("t").append(nextTempVariable).append(typeToOllir(node.get(Constants.typeAttribute), node.getOptional(Constants.arrayAttribute)));
                before.append(" :=").append(typeToOllir(node.get(Constants.typeAttribute), node.getOptional(Constants.arrayAttribute))).append(" ");
                right.append(callExpressionToOllir(child, "", before));
                right.append("t").append(String.valueOf(nextTempVariable)).append(typeToOllir(child.get(Constants.typeAttribute), node.getOptional(Constants.arrayAttribute)));
                break;
            default:
                System.out.println(child);
                System.out.println("Not yet implemented");
                break;
        }

        if (!before.toString().equals("")) {
            before.insert(0, prefix);
            stringBuilder.append(before);
        }
        stringBuilder.append(left);
        stringBuilder.append("=").append(typeToOllir(node.get(Constants.typeAttribute), node.getOptional(Constants.arrayAttribute))).append(" ");
        stringBuilder.append(right);

        return  stringBuilder.append(";\n").toString();
    }

    private String binaryToOllir(JmmNode node, String prefix, StringBuilder before) {
        StringBuilder stringBuilder = new StringBuilder(prefix);

        var child0 = node.getChildren().get(0);
        var child1 = node.getChildren().get(1);

        switch (child0.getKind()) {
            case Constants.terminalNodeName:
                stringBuilder.append(terminalToOllir(child0, "")).append(" ");
                break;
            case Constants.literalNodeName:
                stringBuilder.append(literalToOllir(child0, "")).append(" ");
                break;
            case Constants.binaryNodeName:
                stringBuilder.append(binaryToOllir(child0, "", before)).append(" ");
                break;
            default:
                System.out.println(child0);
                break;
        }

        stringBuilder.append(node.get(Constants.valueAttribute)).append(typeToOllir(node.get(Constants.typeAttribute), node.getOptional(Constants.arrayAttribute))).append(" ");


        switch (child1.getKind()) {
            case Constants.terminalNodeName :
                stringBuilder.append(terminalToOllir(child1, "")).append(" ");
                break;
            case Constants.literalNodeName:
                stringBuilder.append(literalToOllir(child1, "")).append(" ");
                break;
            case Constants.binaryNodeName:
                stringBuilder.append(binaryToOllir(child1, "", before)).append(" ");
                break;
            case Constants.callExprNodeName:
                before.append("t").append(nextTempVariable).append(typeToOllir(node.get(Constants.typeAttribute), node.getOptional(Constants.arrayAttribute)));
                before.append(" :=").append(typeToOllir(node.get(Constants.typeAttribute), node.getOptional(Constants.arrayAttribute))).append(" ");
                stringBuilder.append(callExpressionToOllir(child1, "", before));
                stringBuilder.append("t").append(String.valueOf(nextTempVariable)).append(typeToOllir(child1.get(Constants.typeAttribute), node.getOptional(Constants.arrayAttribute)));
                break;
            default:
                System.out.println(child1);
                break;
        }

        return stringBuilder.toString();
    }

    private String callExpressionToOllir(JmmNode node, String prefix, StringBuilder before) {
        StringBuilder stringBuilder = new StringBuilder();

        var children = node.getChildren();
        String type = typeToOllir(node.get(Constants.typeAttribute), node.getOptional(Constants.arrayAttribute));

        // TODO: Needs some changes maybe not working on all possible cases

        if (imports.contains(children.get(0).get(Constants.typeAttribute))) {
            before.append("invokestatic(");
            before.append(children.get(0).get(Constants.typeAttribute));
        } else {
            before.append("invokevirtual(");
            if (children.get(0).get(Constants.valueAttribute).equals(Constants.thisAttribute)) {
                before.append("this");
            } else {
                before.append(children.get(0).get(Constants.valueAttribute));
                before.append(typeToOllir(children.get(0).get(Constants.typeAttribute), node.getOptional(Constants.arrayAttribute)));
            }
        }

        before.append(", \"").append(node.get(Constants.nameAttribute)).append("\"");

        if (children.size() > 1) {
            var args = children.get(1).getChildren();
            for (int i = 0; i < args.size(); i++) {
                before.append(", ");
                before.append(args.get(i).get(Constants.valueAttribute)).append(typeToOllir(args.get(i).get(Constants.typeAttribute), node.getOptional(Constants.arrayAttribute)));
            }
        }

        before.append(")").append(typeToOllir(node.get(Constants.typeAttribute), node.getOptional(Constants.arrayAttribute)));
        before.append(";\n");

        return stringBuilder.toString();
    }

    private String newToOllir(JmmNode node, String prefix, StringBuilder parentBuilder) {
        StringBuilder stringBuilder = new StringBuilder(prefix);

        stringBuilder.append("new(").append(node.get(Constants.typeAttribute)).append(")");
        stringBuilder.append(typeToOllir(node.get(Constants.typeAttribute), node.getOptional(Constants.arrayAttribute)));

        return stringBuilder.toString();
    }

    private String literalToOllir(JmmNode node, String prefix) {
        StringBuilder stringBuilder = new StringBuilder(prefix);

        String type = node.get(Constants.typeAttribute);
        switch (type) {
            case Constants.intType -> stringBuilder.append(node.get(Constants.valueAttribute)).append(typeToOllir(type, node.getOptional(Constants.arrayAttribute)));
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


        stringBuilder.append(node.get(Constants.valueAttribute)).append(typeToOllir(node.get(Constants.typeAttribute), node.getOptional(Constants.arrayAttribute)));

        return stringBuilder.toString();
    }

    private String methodParameterToOllir(JmmNode node, String prefix) {
        StringBuilder stringBuilder = new StringBuilder(prefix);

        stringBuilder.append(node.get(Constants.nameAttribute));

        Optional<String> type = node.getOptional(Constants.typeAttribute);
        Optional<String> isArray = node.getOptional(Constants.arrayAttribute);

        if (type.isPresent() && isArray.isPresent()) stringBuilder.append(typeToOllir(type.get(), isArray));

        return stringBuilder.toString();
    }

    private String typeToOllir(String type, Optional<String> isArray) {
        boolean array = isArray.isPresent() && isArray.get().equals("true");
        StringBuilder stringBuilder = new StringBuilder();
        if (array)
            stringBuilder.append(".array");
        switch (type) {
            case "int":
                return stringBuilder.append(".i32").toString();
            case "boolean":
                return stringBuilder.append(".bool").toString();
            case "void":
            case "auto":
                return stringBuilder.append(".V").toString();
            default:
                return stringBuilder.append(".").append(type).toString();
        }
    }


}
