import pt.up.fe.comp.jmm.JmmNode;
import pt.up.fe.comp.jmm.ast.JmmNodeImpl;
import pt.up.fe.comp.jmm.ast.PostorderJmmVisitor;

import java.util.*;


public class ConstantPropagationVisitor extends PostorderJmmVisitor<List<List<JmmNode>>, Boolean> {
    public class CustomKey {
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

        @Override
        public String toString() {
            return "(" + parentMethod + ") " + varName;
        }
    }

    public HashMap<CustomKey, String> constants = new HashMap<>();

    public ConstantPropagationVisitor() {
        addVisit(Constants.assignmentNodeName, this::updateConstantsInAssignments);
        addVisit(Constants.terminalNodeName, this::propagateConstantsInTerminals);
        setDefaultVisit(ConstantPropagationVisitor::defaultVisit);
    }

    private Boolean updateConstantsInAssignments(JmmNode node, List<List<JmmNode>> nodesToRemove) {
        var right = node.getChildren().get(1);

        var customKey = customKeyFromAssignment(node);

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
        else { // Variable is being assigned something else than a constant, thus can't be completely propagated
            constants.remove(customKey);
        }

        return defaultVisit(node, nodesToRemove);
    }

    private Boolean propagateConstantsInTerminals(JmmNode node, List<List<JmmNode>> nodesToRemove){
        var parent = node.getParent();
        if ((parent.getKind().equals(Constants.assignmentNodeName) ||   // If node is left part of assignment,
                parent.getKind().equals(Constants.callExprNodeName)) && // Or left part of method call, won't propagate anything
                parent.getChildren().get(0).equals(node))
            return defaultVisit(node, nodesToRemove);

        var customKey= customKeyFromTerminal(node);

        var constant = constants.get(customKey);
        if (constant != null){

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

    private CustomKey customKeyFromTerminal(JmmNode node){
        var parentMethod = node.getAncestor(Constants.methodDeclNodeName);
        return new CustomKey((
                parentMethod.isPresent() ? parentMethod.get().get(Constants.nameAttribute) : "Global"),
                node.getOptional(Constants.valueAttribute).orElse(""));
    }

    private CustomKey customKeyFromAssignment(JmmNode node){
        var left = node.getChildren().get(0);
        var parentMethod = node.getAncestor(Constants.methodDeclNodeName);
        return new CustomKey((
                parentMethod.isPresent() ? parentMethod.get().get(Constants.nameAttribute) : "Global"),
                left.getOptional(Constants.valueAttribute).orElse(""));
    }

    public Set<JmmNode> tryDeleting(JmmNode root, List<List<JmmNode>> nodesToDeleteAndAdd) {
        Set<JmmNode> result = new HashSet<>();

        var astIterator = root.getChildren().listIterator();
        while (astIterator.hasNext()){
            var node = astIterator.next();
            result.addAll(tryDeleting(node, nodesToDeleteAndAdd));

            var tripletIterator = nodesToDeleteAndAdd.iterator();
            while(tripletIterator.hasNext()){
                var nodeTriplet = tripletIterator.next();

                // If node wasn't blacklisted, must remove its assignment
                if (node.getKind().equals(Constants.assignmentNodeName)){
                    var customKey = customKeyFromAssignment(node);
                    if (constants.containsKey(customKey)) {
                        result.add(node);
                    }
                }
                if (node.equals(nodeTriplet.get(0))){
                    var index = node.removeChild(nodeTriplet.get(1));
                    node.add(nodeTriplet.get(2), index);
                    tripletIterator.remove();
                }
            }
        }

        return result;
    }
}
