import pt.up.fe.comp.jmm.JmmNode;

public class Ollir {
    public static String getCode(JmmNode root) {
        StringBuilder stringBuilder = new StringBuilder();

        for (var node: root.getChildren()) {
            switch (node.getKind()) {
                case Constants.importDeclNodeName:
                    break;
                case Constants.classDeclNodeName:
                    stringBuilder.append(toOllirClassDeclaration(node, ""));
                    break;
                default:
                    //System.out.println("Type not yet implemented");
                    break;
            }
        }

        return stringBuilder.toString();
    }

    private static String toOllirClassDeclaration(JmmNode node, String prefix) {
        StringBuilder stringBuilder = new StringBuilder();

        stringBuilder.append(prefix).append(node.get(Constants.nameAttribute)).append(" {\n\n");

        for (var child: node.getChildren()) {
            switch (child.getKind()) {
                case Constants.methodDeclNodeName:
                    stringBuilder.append(toOllirMethodDeclaration(child, prefix+ "  "));
                    break;
                default:
                    //System.out.println("Type not yet implemented");
                    break;
            }
        }

        stringBuilder.append(prefix).append("}");

        return stringBuilder.toString();
    }

    private static String toOllirMethodDeclaration(JmmNode node, String prefix) {
        StringBuilder stringBuilder = new StringBuilder();

        stringBuilder.append(prefix).append(".method public ");
        //System.out.println(node.get(Constants.staticAttribute));
        stringBuilder.append(node.get(Constants.nameAttribute));

        //System.out.println(node.getChildren().get(0));

        stringBuilder.append(prefix).append("}\n");

        return stringBuilder.toString();
    }

}
