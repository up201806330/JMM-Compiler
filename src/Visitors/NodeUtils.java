import pt.up.fe.comp.jmm.JmmNode;
import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp.jmm.report.Report;
import pt.up.fe.comp.jmm.report.ReportType;
import pt.up.fe.comp.jmm.report.Stage;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class NodeUtils {

    /**
     * Gets the children of a node
     * @param node node to get children from
     * @param reports Accumulated reports
     * @return Optional of list of child nodes. If node is Binary and there are not 2 child nodes, Optional.empty()
     */
    public static Optional<List<JmmNode>> getChildren(JmmNode node, List<Report> reports) {
        List<JmmNode> result = node.getChildren();
        if (result.size() != 2 && node.getKind().equals(Constants.binaryNodeName)) {
            reports.add(new Report(
                    ReportType.WARNING,
                    Stage.SEMANTIC,
                    Integer.parseInt(node.get(Constants.lineAttribute)),
                    Integer.parseInt(node.get(Constants.columnAttribute)),
                    "Binary node doesn't have 2 children. Something is wrong in syntactic phase"
            ));
            return Optional.empty();
        }
        return Optional.of(result);
    }

    /**
     * Create type from node with type and isArray parameters already set. Assumes node has already been filled in with these
     * parameters in a previous search
     * @param node Node whose type will be extracted
     * @return A Type object
     */
    public static Type extractTypeFromNode(JmmNode node){
        return new Type(node.get(Constants.typeAttribute), Boolean.parseBoolean(node.get(Constants.arrayAttribute)));
    }

    /**
     *
     * @param node Node to get children from
     * @param reports Accumulated reports
     * @return Optional of list of Types, or if there are no children, Optional.empty()
     */
    public static Optional<List<Type>> childrenTypes(JmmNode node, List<Report> reports){
        List<JmmNode> children;
        var childrenOptional = getChildren(node, reports);
        if (childrenOptional.isEmpty()) return Optional.empty();
        else children = childrenOptional.get();

        List<Type> result = new ArrayList<>();
        result.add(extractTypeFromNode(children.get(0)));
        if (children.size() > 1) result.add(extractTypeFromNode(children.get(1)));
        return Optional.of(result);
    }
}
