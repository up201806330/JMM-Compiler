import pt.up.fe.comp.jmm.JmmNode;

import java.util.List;
import java.util.Optional;

public class OllirCodeUtils {
    public static String defaultConstructor(String ident, String className) {
        String result = ".construct " + className + "().V {\n" +
                ident + ident + "invokespecial(this, \"<init>\").V;\n" +
                ident + "}\n";
        return result;
    }

    public static String typeToOllir(String type, Optional<String> isArray) {
        boolean array = isArray.isPresent() && isArray.get().equals("true");
        StringBuilder stringBuilder = new StringBuilder();
        if (array)
            stringBuilder.append(".array");
        return switch (type) {
            case "int" -> stringBuilder.append(".i32").toString();
            case "boolean" -> stringBuilder.append(".bool").toString();
            case "void", "auto" -> stringBuilder.append(".V").toString();
            default -> stringBuilder.append(".").append(type).toString();
        };
    }

    public static String parametersToOllir(List<JmmNode> parameters, String prefix, String functionName) {
        StringBuilder paramsOllir = new StringBuilder(prefix);

        if (parameters.size() > 0) {
            for (int i = 0; i < parameters.size(); i++) {
                paramsOllir.append(methodParameterToOllir(parameters.get(i), ""));
                if (i != parameters.size() - 1) {
                    paramsOllir.append(", ");
                }
            }
        } else if (functionName.equals(Consts.mainMethod)){
            paramsOllir.append("args.array.String");
        }

        return paramsOllir.toString();
    }

    public static String methodParameterToOllir(JmmNode node, String prefix) {
        StringBuilder stringBuilder = new StringBuilder(prefix);

        stringBuilder.append(node.get(Consts.nameAttribute));

        Optional<String> type = node.getOptional(Consts.typeAttribute);
        Optional<String> isArray = node.getOptional(Consts.arrayAttribute);

        if (type.isPresent() && isArray.isPresent()) stringBuilder.append(OllirCodeUtils.typeToOllir(type.get(), isArray));

        return stringBuilder.toString();
    }
}
