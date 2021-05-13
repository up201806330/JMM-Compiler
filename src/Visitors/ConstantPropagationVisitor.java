import pt.up.fe.comp.jmm.JmmNode;
import pt.up.fe.comp.jmm.ast.JmmNodeImpl;
import pt.up.fe.comp.jmm.ast.PostorderJmmVisitor;

import java.util.*;


public class ConstantPropagationVisitor extends PostorderJmmVisitor<List<List<JmmNode>>, Boolean> {
    private class CustomKey {
        String parentMethod;
        String varName;

        public CustomKey(String parentMethod, String varName) {
            this.parentMethod = parentMethod;
            this.varName = varName;
        }

        public String getParentMethod() {
            return parentMethod;
        }

        public String getVarName() {
            return varName;
        }

        @Override
        public int hashCode() {
            return parentMethod.hashCode() * varName.hashCode();
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj)
                return true;
            if (obj == null)
                return false;
            if (getClass() != obj.getClass())
                return false;

            CustomKey other = (CustomKey)obj;
            if (!getParentMethod().equals(other.getParentMethod()))
                return false;
            else return getVarName().equals(other.getVarName());
        }
    }

    private HashMap<CustomKey, String> constants = new HashMap<>();

    public ConstantPropagationVisitor() {
        addVisit(Constants.assignmentNodeName, this::updateConstants);
        addVisit(Constants.terminalNodeName, this::propagateConstants);
        setDefaultVisit(ConstantPropagationVisitor::defaultVisit);
    }

    private Boolean updateConstants(JmmNode node, List<List<JmmNode>> nodesToRemove) {
        var left = node.getChildren().get(0);
        var right = node.getChildren().get(1);

        var parentMethod = node.getAncestor(Constants.methodDeclNodeName);
        var customKey= new CustomKey((
                parentMethod.isPresent() ? parentMethod.get().get(Constants.nameAttribute) : "Global"),
                left.getOptional(Constants.valueAttribute).orElse(""));

        // Can't do optimization if value is being looped through or is being assigned inside if
        if (node.getAncestor(Constants.whileStatementNodeName).isPresent() ||
            node.getAncestor(Constants.ifStatementNodeName).isPresent()) {
            constants.remove(customKey);
            return defaultVisit(node, nodesToRemove);
        }

        if (right.getKind().equals(Constants.literalNodeName)){
            constants.put(
                    customKey,
                    right.get(Constants.valueAttribute));
        }
        else constants.remove(customKey);

        return defaultVisit(node, nodesToRemove);
    }

    private Boolean propagateConstants(JmmNode node, List<List<JmmNode>> nodesToRemove){
        var parent = node.getParent();
        if (parent.getKind().equals(Constants.assignmentNodeName) // If node is left part of assignment, wont propagate anything
                && parent.getChildren().get(0).equals(node))
            return defaultVisit(node, nodesToRemove);

        var parentMethod = node.getAncestor(Constants.methodDeclNodeName);
        var customKey= new CustomKey((
                parentMethod.isPresent() ? parentMethod.get().get(Constants.nameAttribute) : "Global"),
                node.getOptional(Constants.valueAttribute).orElse(""));

        var constant = constants.get(customKey);
        if (constant != null){
            System.out.println("Replacing " + constant);
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
                    nodeTriple.get(0).add(nodeTriple.get(2), index);
                    it.remove();
                    break; // or return
                }
            }
            tryDeleting(child, nodesToDeleteAndAdd);
        }
    }
}
