import java.util.Optional;

public class OllirCodeUtils {
    public static String defaultConstructor(String ident, String className) {
        String result = ".construct " + className +
                "().V {\n" + ident + ident +
                "invokespecial(this, \"<init>\").V;\n" +
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
}
