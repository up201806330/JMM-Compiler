import pt.up.fe.comp.jmm.JmmNode;
import pt.up.fe.comp.jmm.ast.PreorderJmmVisitor;
import pt.up.fe.specs.util.utilities.StringLines;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Counts the occurences of each node kind.
 * 
 * @author JBispo
 *
 */
public class OurPreorderVisitor extends PreorderJmmVisitor<String, String> {

    private final String identifierType = "Identifier";
    private final String identifierAttribute = "id";

    private final String typeType = "Identifier";
    private final String typeAttribute = "type";

    public OurPreorderVisitor() {
        super(OurPreorderVisitor::reduce);

        addVisit(identifierType, this::dealWithIdentifier);
        addVisit(typeType, this::dealWithType);
        setDefaultVisit(this::defaultVisit);
    }

    public String dealWithIdentifier(JmmNode node, String space) {
        // if (node.getKind().equals("This")) {
        if (node.getOptional(identifierAttribute).orElse("").equals("this")) {
            return space + "THIS_ACCESS";
        }

        return defaultVisit(node, space);
    }
    public String dealWithType(JmmNode node, String space) {
        if (node.get(typeAttribute).equals("intArray")) {
            node.put("isArray", "true");
            node.put("type", "int");
        }
        else
            node.put("isArray", "false");

        return defaultVisit(node, space);
    }

    private String defaultVisit(JmmNode node, String space) {
        String content = space + node.getKind();
        String attrs = node.getAttributes()
                .stream()
                .filter(a -> !a.equals("line"))
                .map(a -> a + "=" + node.get(a))
                .collect(Collectors.joining(", ", "[", "]"));

        content += ((attrs.length() > 2) ? attrs : "");

        return content;
    }

    private static String reduce(String nodeResult, List<String> childrenResults) {
        var content = new StringBuilder();

        content.append(nodeResult).append("\n");

        for (var childResult : childrenResults) {
            var childContent = StringLines.getLines(childResult).stream()
                    .map(line -> " " + line + "\n")
                    .collect(Collectors.joining());

            content.append(childContent);
        }

        return content.toString();
    }

}
