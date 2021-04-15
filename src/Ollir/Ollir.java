import pt.up.fe.comp.jmm.JmmNode;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class Ollir {
    private final String ident = "  ";
    public String getCode(JmmNode root) {
        StringBuilder stringBuilder = new StringBuilder();

        for (var node: root.getChildren()) {
            switch (node.getKind()) {
                case Constants.importDeclNodeName:
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

        stringBuilder.append(node.get(Constants.nameAttribute)).append(" {\n\n");

        for (var child: node.getChildren()) {
            switch (child.getKind()) {
                case Constants.methodDeclNodeName:
                    stringBuilder.append(methodDeclarationToOllir(child, prefix+ ident));
                    break;
                default:
                    //System.out.println("Type not yet implemented");
                    break;
            }
        }

        stringBuilder.append(prefix).append("}");

        return stringBuilder.toString();
    }

    private String methodDeclarationToOllir(JmmNode node, String prefix) {
        StringBuilder stringBuilder = new StringBuilder(prefix);

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
                case Constants.methodParamNodeName -> parameters.add(child);
                case Constants.assignmentNodeName -> insideMethod.append(assignmentToOllir(child, prefix + ident));
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
            stringBuilder.append(typeToOllir(type.get(Constants.typeAttribute)));
        else
            stringBuilder.append(typeToOllir(Constants.voidType));

        stringBuilder.append(" {\n");
        stringBuilder.append(insideMethod);
        stringBuilder.append(prefix).append("}\n");

        return stringBuilder.toString();
    }

    private String assignmentToOllir(JmmNode node, String prefix) {
        StringBuilder stringBuilder = new StringBuilder(prefix);

        var children = node.getChildren();
        for (var child: children) {
            switch (child.getKind()){
                case Constants.terminalNodeName:
                    stringBuilder.append(terminalToOllir(child, "")).append(" ");
                    stringBuilder.append(":=").append(typeToOllir(child.get(Constants.typeAttribute))).append(" ");
                    break;
                case Constants.literalNodeName:
                    stringBuilder.append(literalToOllir(child, "")).append(" ");
                    break;
                case Constants.newNodeName:
                    stringBuilder.append(newToOllir(child, ""));
                    break;
                case Constants.binaryNodeName:
                    stringBuilder.append(binaryToOllir(child, ""));
                    break;
                default:
                    System.out.println(child);
                    //System.out.println("Not yet implemented");
                    break;
            }
        }

        return  stringBuilder.append(";\n").toString();
    }

    private String binaryToOllir(JmmNode node, String prefix) {
        StringBuilder stringBuilder = new StringBuilder(prefix);

        var child0 = node.getChildren().get(0);
        var child1 = node.getChildren().get(1);

        switch (child0.getKind()) {
            case Constants.terminalNodeName -> stringBuilder.append(terminalToOllir(child0, ""));
            default -> System.out.println(child0);
        }

        stringBuilder.append(node.get(Constants.valueAttribute)).append(typeToOllir(node.get(Constants.typeAttribute)));


        switch (child1.getKind()) {
            case Constants.terminalNodeName -> stringBuilder.append(terminalToOllir(child1, ""));
            default -> System.out.println(child1);
        }

        return stringBuilder.toString();
    }

    private String newToOllir(JmmNode node, String prefix) {
        StringBuilder stringBuilder = new StringBuilder(prefix);

        stringBuilder.append("new(").append(node.get(Constants.typeAttribute)).append(")");
        stringBuilder.append(typeToOllir(node.get(Constants.typeAttribute)));

        return stringBuilder.toString();
    }

    private String literalToOllir(JmmNode node, String prefix) {
        StringBuilder stringBuilder = new StringBuilder(prefix);

        String type = node.get(Constants.typeAttribute);
        switch (type) {
            case Constants.intType -> stringBuilder.append(node.get(Constants.valueAttribute)).append(typeToOllir(type));
        }

        return stringBuilder.toString();
    }

    private String terminalToOllir(JmmNode node, String prefix) {
        StringBuilder stringBuilder = new StringBuilder(prefix);

        stringBuilder.append(node.get(Constants.valueAttribute)).append(typeToOllir(node.get(Constants.typeAttribute)));

        return stringBuilder.toString();
    }

    private String methodParameterToOllir(JmmNode node, String prefix) {
        StringBuilder stringBuilder = new StringBuilder(prefix);

        stringBuilder.append(node.get(Constants.nameAttribute));

        Optional<String> type = node.getOptional(Constants.typeAttribute);
        Optional<String> isArray = node.getOptional(Constants.arrayAttribute);

        if (isArray.isPresent() && isArray.get().equals("true")) stringBuilder.append(".").append("array");
        if (type.isPresent()) stringBuilder.append(typeToOllir(type.get()));

        return stringBuilder.toString();
    }

    private String typeToOllir(String type) {
        switch (type) {
            case "int":
                return ".i32";
            case "boolean":
                return ".bool";
            case "void":
                return ".V";
            default:
                return "." + type;
        }
    }


}
