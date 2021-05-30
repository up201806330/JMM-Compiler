import org.specs.comp.ollir.*;
import org.specs.comp.ollir.Node;

import java.util.*;
import java.util.stream.Collectors;

public class Liveness {
    private int dashR;

    private HashMap<Integer, Set<String>> use = new HashMap<>();
    private HashMap<Integer, Set<String>> def = new HashMap<>();
    private HashMap<Integer, Set<Integer>> succ = new HashMap<>();

    private HashMap<Integer, Set<String>> in = new HashMap<>();
    private HashMap<Integer, Set<String>> out = new HashMap<>();
    private HashMap<String, TreeSet<Integer>> liveRanges = new HashMap<>();

    private HashMap<String, Set<String>> interferenceGraph = new HashMap<>();
    private HashMap<String, Integer> nodeColors = new HashMap<>();

    private HashMap<Integer, Integer> visited = new HashMap<>();
    private HashSet<String> params = new HashSet<>();

    public Liveness(int dashR) {
        this.dashR = dashR;
    }

    public boolean run(ClassUnit classUnit){
        classUnit.buildCFGs();
        classUnit.buildVarTables();

        for (var method : classUnit.getMethods()){
            use.clear();
            def.clear();
            succ.clear();
            in.clear();
            out.clear();
            liveRanges.clear();
            interferenceGraph.clear();
            nodeColors.clear();
            visited.clear();
            params.clear();
            for (var x : method.getParams()) params.add(((Operand)x).getName());

            if (method.isConstructMethod()) continue;

            buildUseAndDef(method);

            /*
            System.out.println(method.getMethodName());
            for (var x : method.getInstructions()){
                var id = x.getId();
                var useSet = use.get(id);
                var defSet = def.get(id);
                System.out.println(id + ": " + x.getInstType() + " [" +
                        (useSet != null ? useSet.stream().map(Objects::toString).collect(Collectors.joining(", ")) : " ") + "]  [" +
                        (defSet != null ? defSet.stream().map(Objects::toString).collect(Collectors.joining(", ")) : " ") + "]  [" +
                        succ.get(id).stream().map(Objects::toString).collect(Collectors.joining(", ")) + "]");
            }
             */

            buildInAndOut(method);

            /*
            System.out.println("In and out:");
            for (var x : method.getInstructions()){
                var id = x.getId();
                var inSet = in.get(id);
                var outSet = out.get(id);
                System.out.println(id + ": " + x.getInstType() + " [" +
                        (inSet != null ? inSet.stream().map(Objects::toString).collect(Collectors.joining(", ")) : " ") + "]  [" +
                        (outSet != null ? outSet.stream().map(Objects::toString).collect(Collectors.joining(", ")) : " ") + "]");
            }
             */

            buildLiveRanges();

            /*
            System.out.println("Live ranges:");
            for (var x : liveRanges.entrySet()){
                var range = x.getValue();
                System.out.println(x.getKey() + ": [" +
                        (range != null ? range.stream().map(Objects::toString).collect(Collectors.joining(", ")) : " ") + "]");
            }
             */

            buildInterferenceGraph();

            /*
            System.out.println("Interference graph:");
            for (var x : interferenceGraph.entrySet()){
                var range = x.getValue();
                System.out.println(x.getKey() + ": [" +
                        (range != null ? range.stream().map(Objects::toString).collect(Collectors.joining(", ")) : " ") + "]");
            }
             */

            var result = colorGraph(method);
            if (!result) return false;

            /*
            System.out.println(method.getMethodName());
            System.out.println("Colored graph:");
            for (var x : nodeColors.entrySet()){
                System.out.println(x.getKey() + ": " + x.getValue());
            }
            */

            updateVarTable(method);
        }
        return true;
    }

    private void buildUseAndDef(Method method, Node node) {
        var nodeId = node.getId();

        if (node.getNodeType().equals(NodeType.END) || (
                visited.containsKey(nodeId) && visited.get(nodeId) > 10)) return;

//        try {
//            System.out.print("NODEID: " + nodeId + " -> ");((Instruction) node).show();
//        } catch (ClassCastException ignored){}

        var suc1 = node.getSucc1();
        var suc2 = node.getSucc2();
        if (suc1 != null) addToSucc(nodeId, suc1.getId());
        if (suc2 != null) addToSucc(nodeId, suc2.getId());

        if (node.getNodeType().equals(NodeType.INSTRUCTION) && nodeId != 0){
            var instruction = (Instruction) node;
            switch (instruction.getInstType()){
                case ASSIGN -> {
                    var leftSide = ((Operand) ((AssignInstruction) instruction).getDest());

                    // Parameters are live the entire scope of the method ; dont appear in use/def
                    addToDef(nodeId, leftSide.getName());

                    buildUseAndDef(method, ((AssignInstruction) instruction).getRhs());
                }
                case CALL -> {
                    var callInstr = (CallInstruction) instruction;

                    if (!callInstr.getFirstArg().getType().getTypeOfElement().equals(ElementType.CLASS)) // Callee isn't class name, is actual variable
                        addToUse(nodeId, ((Operand) callInstr.getFirstArg()).getName());

                    var ops =  callInstr.getListOfOperands();
                    if (ops != null) {
                        for (var arg :ops){
                            try{
                                addToUse(nodeId, ((Operand) arg).getName());
                            } catch (ClassCastException ignored){}
                        }
                    }
                }
                case GOTO -> {
                    addToSucc(nodeId, instrIndexFromLabel(method, ((GotoInstruction)instruction).getLabel()));
                }
                case BRANCH -> {
                    var branchInstr = (CondBranchInstruction) instruction;
                    try {
                        addToUse(nodeId, ((Operand) branchInstr.getLeftOperand()).getName());
                        addToUse(nodeId, ((Operand) branchInstr.getRightOperand()).getName());
                    } catch (ClassCastException ignored) {}
                }
                case RETURN -> {
                    var returnInstr = (ReturnInstruction) instruction;
                    try {
                        addToUse(nodeId, ((Operand) returnInstr.getOperand()).getName());
                    } catch (NullPointerException | ClassCastException ignored){}
                }
                case PUTFIELD -> {
                    var putfieldInstr = (PutFieldInstruction) instruction;
                    try {
                        addToUse(nodeId, ((Operand) putfieldInstr.getThirdOperand()).getName());
                    } catch (ClassCastException ignored){}
                }
                case GETFIELD -> {
                    // Has no effect
                }
                case NOPER -> {
                    try {
                        var arrOp = (ArrayOperand) ((SingleOpInstruction) instruction).getSingleOperand();
                        addToUse(nodeId, arrOp.getName());
                        addToUse(nodeId, ((Operand) arrOp.getIndexOperands().get(0)).getName());
                    } catch (ClassCastException ignored1){
                        try {
                            addToUse(nodeId, ((Operand) ((SingleOpInstruction) instruction).getSingleOperand()).getName());
                        } catch (ClassCastException ignored2){}
                    }
                }
                case  BINARYOPER -> {
                    try {
                        var leftOp = (Operand) ((BinaryOpInstruction) instruction).getLeftOperand();
                        addToUse(nodeId, leftOp.getName());
                    } catch (ClassCastException ignored){}

                    try {
                        var rightOp = (Operand) ((BinaryOpInstruction) instruction).getRightOperand();
                        addToUse(nodeId, rightOp.getName());
                    } catch (ClassCastException ignored){}
                }
                case UNARYOPER -> {
                    // Wont appear in ollir
                }
            }
        }

        var existingEntry = visited.putIfAbsent(nodeId, 1);
        if (existingEntry != null) visited.put(nodeId, existingEntry + 1);

        if (suc1 != null) buildUseAndDef(method, suc1);
        if (suc2 != null) buildUseAndDef(method, suc2);
    }
    public void buildUseAndDef(Method method){
        buildUseAndDef(method, method.getBeginNode());
    }

    private void buildInAndOut(Method method) {
        boolean atLeastOneChange;
        do {
            atLeastOneChange = false;

            for (var x : method.getInstructions()){
                var id = x.getId();
                var useSet = use.get(id); if (useSet == null) useSet = new HashSet<>();
                var defSet = def.get(id); if (defSet == null) defSet = new HashSet<>();
                var succSet = succ.get(id); if (succSet == null) succSet = new HashSet<>();

                var newOut = new HashSet<String>();
                for (int succ : succSet){
                    var succIn = in.get(succ); if (succIn == null) succIn = new HashSet<>();
                    newOut.addAll(succIn);
                }
                if (!newOut.equals(out.get(id))) atLeastOneChange = true;
                out.put(id, newOut);

                var newIn = new HashSet<String>();
                newIn.addAll(useSet);
                var newOutCopy = new HashSet<>(newOut); newOutCopy.removeAll(defSet);
                newIn.addAll(newOutCopy);

                if (!newIn.equals(in.get(id))) atLeastOneChange = true;
                in.put(id, newIn);
            }

        } while(atLeastOneChange);
    }

    private void buildLiveRanges() {
        for (var val : out.entrySet()){
            for (var variable : val.getValue()){
                var existingRange = liveRanges.putIfAbsent(variable, new TreeSet<>(Arrays.asList(val.getKey())));
                if (existingRange != null) existingRange.add(val.getKey());
            }
        }
    }

    private void buildInterferenceGraph() {
        for (var x : liveRanges.entrySet()){
            interferenceGraph.put(x.getKey(), new HashSet<>());
        }

        for (var left : liveRanges.entrySet()){
            for (var right : liveRanges.entrySet()){
                if (left.getKey().equals(right.getKey())) continue;

                // Equivalent to left (INTERSECTION) right != empty
                if (left.getValue().stream().anyMatch(right.getValue()::contains)) {
                    var existingEdges = interferenceGraph.get(left.getKey());
                    if (existingEdges != null) existingEdges.add(right.getKey());
                }
            }
        }
    }

    private boolean colorGraph(Method method) {
        var copyGraph = new HashMap<>(interferenceGraph);

        Stack<String> nodeStack = new Stack<>();

        String nodeToRemove;
        do {
            nodeToRemove = null;

            for (var node : copyGraph.entrySet()){
                if (node.getValue().size() < dashR){ // Found node to simplify
                    nodeToRemove = node.getKey();
                    break;
                }
            }

            if (nodeToRemove != null){
                copyGraph.remove(nodeToRemove); // Remove node from graph
                for (var node : copyGraph.entrySet()){
                    node.getValue().remove(nodeToRemove); // Remove edges going to that node
                }
                nodeStack.push(nodeToRemove);
            }
        } while (nodeToRemove != null);

        // Couldnt remove all nodes, cant color with dashR
        if (copyGraph.size() != 0) return false;

        // If method is static (aka has no 'this'), registers can start at 0
        var firstReg = method.isStaticMethod() ? 0 : 1;
        var lastReg = method.isStaticMethod() ? interferenceGraph.size() - 1 : interferenceGraph.size();
        firstReg += method.getParams().size();
        lastReg += method.getParams().size();

        while(nodeStack.size() > 0){
            var node = nodeStack.pop();

            boolean failedColor = false;
            for (int i = firstReg ; i <= lastReg ; i++){
                failedColor = false;
                for (var connectedNode : interferenceGraph.get(node)){
                    // If there is a connected node of this color, cant paint with it
                    if (nodeColors.get(connectedNode) == i) {
                        failedColor = true;
                        break;
                    }
                }

                if (!failedColor){
                    nodeColors.put(node, i);
                    break;
                }
            }

            // If couldnt paint a node, cant color with dashR
            if (failedColor) return false;
        }

        return true;
    }

    private void updateVarTable(Method method) {
        for (var node : nodeColors.entrySet()){
            for (var varTableEntry : method.getVarTable().entrySet()){
                if (varTableEntry.getKey().equals(node.getKey())) {
                    var oldDescriptor = varTableEntry.getValue();
                    varTableEntry.setValue(new Descriptor(oldDescriptor.getScope(), node.getValue(), oldDescriptor.getVarType()));
                }
            }
        }
    }
    private void addToUse(int index, String var){
        if (var.equals("this") || params.contains(var))
            return;

        if (use.containsKey(index)){
            use.get(index).add(var);
        }
        else {
            use.put(index, new HashSet<>(Arrays.asList(var)));
        }
    }

    private void addToDef(int index, String var) {
        if (var.equals("this") || params.contains(var))
            return;

        if (def.containsKey(index)) {
            def.get(index).add(var);
        } else {
            def.put(index, new HashSet<>(Arrays.asList(var)));
        }
    }

    private void addToSucc(int index, int dest){
        if (succ.containsKey(index)){
            succ.get(index).add(dest);
        }
        else {
            succ.put(index, new HashSet<>(Arrays.asList(dest)));
        }
    }

    private int instrIndexFromLabel(Method method, String label){
        int i = 1;
        for (var instr : method.getInstructions()){
            var instrLabels = method.getLabels(instr);
            if (instrLabels.size() > 0 && instrLabels.contains(label)){
                return i;
            }
            i++;
        }
        return -1;
    }
}
