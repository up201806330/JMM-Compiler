import pt.up.fe.comp.jmm.JmmNode;
import pt.up.fe.comp.jmm.ast.JmmNodeImpl;
import pt.up.fe.comp.jmm.ast.PostorderJmmVisitor;

import java.util.*;


public class ConstantPropagationVisitor extends PostorderJmmVisitor<List<List<JmmNode>>, Boolean> {
    private HashMap<String, String> constants = new HashMap<>();

    public ConstantPropagationVisitor() {
        addVisit(Constants.assignmentNodeName, this::updateConstants);
        addVisit(Constants.terminalNodeName, this::propagateConstants);
        setDefaultVisit(ConstantPropagationVisitor::defaultVisit);
    }

    private Boolean updateConstants(JmmNode node, List<List<JmmNode>> nodesToRemove) {
        // Ver se já existe no map, se sim substitui
        // Só mete o valor se for um literal
        var left = node.getChildren().get(0);
        var right = node.getChildren().get(1);

        if (right.getKind().equals(Constants.literalNodeName)){
            constants.put(left.get(Constants.valueAttribute), right.get(Constants.valueAttribute));
        }
        else constants.remove(left.getOptional(Constants.valueAttribute).orElse(""));

        return defaultVisit(node, nodesToRemove);
    }

    private Boolean propagateConstants(JmmNode node, List<List<JmmNode>> nodesToRemove){
        var parent = node.getParent();
        if (parent.getKind().equals(Constants.assignmentNodeName) // If node is left part of assignment, wont propagate anything
                && parent.getChildren().get(0).equals(node))
            return defaultVisit(node, nodesToRemove);

        var constant = constants.get(node.get(Constants.valueAttribute));
        if (constant != null){
            // Find this nodes index in parent
            var index = 0;
            for (var child : parent.getChildren()){
                if (child.equals(node)) break;
                index++;
            }

            // Replace node with terminal with that value and type
            var newNode = new JmmNodeImpl(Constants.literalNodeName);
            newNode.put(Constants.valueAttribute, constant);
            newNode.put(Constants.typeAttribute, ((constant.equals("true") || constant.equals("false")) ? Constants.booleanType : Constants.intType));
            newNode.put(Constants.arrayAttribute, "false");
            nodesToRemove.add(new ArrayList<>(Arrays.asList(parent, node, newNode)));
        }

        return true;
    }

    private static Boolean defaultVisit(JmmNode node, List<List<JmmNode>> nodesToRemove) {
        return true;
    }

    public void tryDeleting(JmmNode root, List<List<JmmNode>> nodesToDeleteAndAdd) {
        for (var child : root.getChildren()){
            var it = nodesToDeleteAndAdd.iterator();
            while(it.hasNext()){
                var nodeTriple = it.next();
                if (child.equals(nodeTriple.get(0))){
                    var index = nodeTriple.get(0).removeChild(nodeTriple.get(1));
                    nodeTriple.get(0).add(nodeTriple.get(2), 1); // TODO Replace with 'index' when update is done
                    it.remove();
                    break; // or return
                }
            }
            tryDeleting(child, nodesToDeleteAndAdd);
        }
    }
}
