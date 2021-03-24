import pt.up.fe.comp.jmm.JmmNode;
import pt.up.fe.comp.jmm.ast.AJmmVisitor;

import java.util.stream.Collectors;

public class JmmVisitor extends AJmmVisitor<String, String> {
    private final String identifierAttribute;

    public JmmVisitor(String identifierType, String identifierAttribute) {
        this.identifierAttribute = identifierAttribute;

        addVisit(identifierType, this::dealWithIdentifier); // Method reference
        setDefaultVisit(this::defaultVisit); // Method reference
    }

    public String dealWithIdentifier(JmmNode node, String space) {
        if (node.getOptional(identifierAttribute).orElse("").equals("this")) {
            return space + "THIS_ACCESS\n";
        }
        return defaultVisit(node, space);
    }

    private String defaultVisit(JmmNode node, String space) {
        String content = space + node.getKind();
        String attrs = node.getAttributes()
                .stream()
                .filter(a -> !a.equals("line"))
                .map(a -> a + "=" + node.get(a))
                .collect(Collectors.joining(", ", "[", "]"));

        content += ((attrs.length() > 2) ? attrs : "") + "\n";
        for (JmmNode child : node.getChildren()) {
            content += visit(child, space + " ");
        }
        return content;
    }

}
