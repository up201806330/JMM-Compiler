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
    public boolean atLeastOneChange;

    public ConstantPropagationVisitor() {
        addVisit(Consts.assignmentNodeName, this::updateConstantsInAssignments);
        addVisit(Consts.terminalNodeName, this::propagateConstantsInTerminals);
        addVisit(Consts.binaryNodeName, this::propagateConstantsInBinaries);
        setDefaultVisit(ConstantPropagationVisitor::defaultVisit);
    }

    private Boolean updateConstantsInAssignments(JmmNode node, List<List<JmmNode>> nodesToRemove) {
        var right = node.getChildren().get(1);

        var customKey = customKeyFromAssignment(node);

        // Can't do optimization if value is being looped through or is being assigned inside if
        if (node.getAncestor(Consts.whileStatementNodeName).isPresent() ||
            node.getAncestor(Consts.ifStatementNodeName).isPresent()) {
            constants.remove(customKey);
            return defaultVisit(node, nodesToRemove);
        }

        if (right.getKind().equals(Consts.literalNodeName)){
            constants.put(
                    customKey,
                    right.get(Consts.valueAttribute));
        }
        else { // Variable is being assigned something else than a constant, thus can't be completely propagated
            constants.remove(customKey);
        }

        return defaultVisit(node, nodesToRemove);
    }

    private Boolean propagateConstantsInTerminals(JmmNode node, List<List<JmmNode>> nodesToRemove){
        var parent = node.getParent();
        if ((parent.getKind().equals(Consts.assignmentNodeName) ||   // If node is left part of assignment,
                parent.getKind().equals(Consts.callExprNodeName)) && // Or left part of method call, won't propagate anything
                parent.getChildren().get(0).equals(node))
            return defaultVisit(node, nodesToRemove);

        var customKey= customKeyFromTerminal(node);

        var constant = constants.get(customKey);
        if (constant != null){

            // Replace node with terminal with that value and type
            var newNode = new JmmNodeImpl(Consts.literalNodeName);
            newNode.put(Consts.valueAttribute, constant);
            newNode.put(Consts.typeAttribute, ((constant.equals("true") || constant.equals("false")) ? Consts.booleanType : Consts.intType));
            newNode.put(Consts.arrayAttribute, "false");
            nodesToRemove.add(new ArrayList<>(Arrays.asList(parent, node, newNode)));

            atLeastOneChange = true;
        }

        return true;
    }

    private Boolean propagateConstantsInBinaries(JmmNode node, List<List<JmmNode>> nodesToRemove){
        var parent = node.getParent();
        var left = node.getChildren().get(0); String leftVal = left.get(Consts.valueAttribute);
        var right = node.getChildren().get(1); String rightVal = right.get(Consts.valueAttribute);

        var newNode = new JmmNodeImpl(Consts.literalNodeName);
        newNode.put(Consts.typeAttribute, left.get(Consts.typeAttribute));
        newNode.put(Consts.arrayAttribute, "false");
        
        if (!left.getKind().equals(Consts.literalNodeName) || !right.getKind().equals(Consts.literalNodeName))
            return true;
        
        if (left.get(Consts.typeAttribute).equals(Consts.booleanType) &&
                right.get(Consts.typeAttribute).equals(Consts.booleanType)) {
            if (node.get(Consts.valueAttribute).equals(Consts.andExpression)) {
                newNode.put(Consts.valueAttribute,
                        String.valueOf(Boolean.parseBoolean(leftVal) && Boolean.parseBoolean(rightVal)));
            }
        }
        else if (left.get(Consts.typeAttribute).equals(Consts.intType) &&
                    right.get(Consts.typeAttribute).equals(Consts.intType)){
            newNode.put(Consts.typeAttribute, Consts.intType);
            String value = "";
            switch (node.get(Consts.valueAttribute)){
                case "+":
                    value = String.valueOf(Integer.parseInt(leftVal) + Integer.parseInt(rightVal));
                    break;
                case "-":
                    value = String.valueOf(Integer.parseInt(leftVal) - Integer.parseInt(rightVal));
                    break;
                case "*":
                    value = String.valueOf(Integer.parseInt(leftVal) * Integer.parseInt(rightVal));
                    break;
                case "/":
                    value = String.valueOf(Integer.parseInt(leftVal) / Integer.parseInt(rightVal));
                    break;
                case "<":
                    value = String.valueOf(Integer.parseInt(leftVal) < Integer.parseInt(rightVal));
                    break;
                case "&&":
                default:
                    break;
            }
            newNode.put(Consts.valueAttribute, value);
        }
        else return true;

        nodesToRemove.add(new ArrayList<>(Arrays.asList(parent, node, newNode)));
        atLeastOneChange = true;
        
        return defaultVisit(node, nodesToRemove);
    }

    private static Boolean defaultVisit(JmmNode node, List<List<JmmNode>> nodesToRemove) {
        return true;
    }

    private CustomKey customKeyFromTerminal(JmmNode node){
        var parentMethod = node.getAncestor(Consts.methodDeclNodeName);
        return new CustomKey((
                parentMethod.isPresent() ? parentMethod.get().get(Consts.nameAttribute) : "Global"),
                node.getOptional(Consts.valueAttribute).orElse(""));
    }

    private CustomKey customKeyFromAssignment(JmmNode node){
        var left = node.getChildren().get(0);
        var parentMethod = node.getAncestor(Consts.methodDeclNodeName);
        return new CustomKey((
                parentMethod.isPresent() ? parentMethod.get().get(Consts.nameAttribute) : "Global"),
                left.getOptional(Consts.valueAttribute).orElse(""));
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
                if (node.getKind().equals(Consts.assignmentNodeName)){
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
