import org.specs.comp.ollir.*;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static pt.up.fe.specs.util.SpecsBits.log2;

public class Jasmin {
    private final String indent = "\t";
    private HashMap<String, Descriptor> varTable;
    private ClassUnit classUnit;

    private int maxStackSize;

    List<String> deadTags = new ArrayList<>();

    public String getByteCode(ClassUnit classUnit) throws OllirErrorException {
        this.classUnit = classUnit;
        classUnit.buildVarTables();

        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(".class ")
                .append(accessModifierToJasmin(classUnit.getClassAccessModifier())).append(" ")
                .append(classUnit.getClassName()).append("\n");

        if (classUnit.getSuperClass() != null)
            stringBuilder.append(".super ").append(classUnit.getSuperClass()).append("\n\n");
        else
            stringBuilder.append(".super java/lang/Object").append("\n\n");


        classUnit.getFields().forEach(x -> stringBuilder.append(fieldToJasmin(x)));
        if (classUnit.getFields().size() > 0) stringBuilder.append("\n");

        for (Method method : classUnit.getMethods()){
            stringBuilder.append(methodToJasmin(method));
        }

        return stringBuilder.toString();
    }

    private String methodToJasmin(Method method) {
        StringBuilder start = new StringBuilder();
        StringBuilder result = new StringBuilder();

        varTable = method.getVarTable();

        maxStackSize = 0;
        deadTags = new ArrayList<>();

//        method.show();

        start.append(".method ")
            .append(accessModifierToJasmin(method.getMethodAccessModifier()))
            .append(method.isStaticMethod() ? " static " : " ")
            .append(method.isConstructMethod() ? "<init>" : method.getMethodName())
            .append("(")
                .append(parametersToJasmin(method.getParams()))
            .append(")")
            .append(typeToJasmin(method.getReturnType())).append("\n");

            method.getInstructions().forEach(instruction -> result.append(instructionToJasmin(method, instruction, true)));
            // method.getInstructions().forEach(Instruction::show);

        if (method.isConstructMethod())
            result.append(indent).append(Consts.returnVoidInstr).append("\n");
        result.append(".end method").append("\n\n");

        removeDeadCodeTags(result);
        calculateMaxStackSize(result.toString());

        if (!method.isConstructMethod()){
            start.append(indent).append(".limit stack ").append(maxStackSize).append("\n")
                    .append(indent).append(".limit locals ").append(findLocalsSize(varTable)).append("\n");
        }

        return start.append(result).toString();
    }

    private int findLocalsSize(HashMap<String, Descriptor> varTable) {
        int max = 0;
        for (var x : varTable.entrySet()){
            var reg = x.getValue().getVirtualReg();
            if (reg > max) max = reg;
        }
        return max + 1;
    }

    private void calculateMaxStackSize(String instructions) {
        int res = 0;
        BufferedReader bufReader = new BufferedReader(new StringReader(instructions));
        String line;
        try {
            while(!(line = bufReader.readLine()).equals(".end method")){
                if (!line.startsWith(indent) || line.contains(":")) continue;

                String[] chars = line.split("");
                var spacePos = line.indexOf(" ");

                var index = 0;
                if (spacePos == -1){
                    for (int i = 0 ; i < chars.length ; i++){
                        if (chars[i].matches("[0-9]+")){
                            index = i;
                        }
                    }
                }
                else index = spacePos;

                String key = line.substring(0, index).trim();
                res += Consts.instructionVals.get(key);

                if (key.equals(Consts.invokeStatic.trim()) || key.equals(Consts.invokeVirtual.trim()) || key.equals(Consts.invokeSpecial.trim())){
                    if (!line.substring(line.length() - 1).equals("V")) res += 1;

                    char[] args = line.substring(line.indexOf("(") + 1, line.indexOf(")")).toCharArray();
                    for (int i = 0 ; i < args.length ; i++){
                        res -= 1;
                        if (args[i] == '[') {
                            i++;
                        }
                    }
                }

                if (res > maxStackSize) maxStackSize = res;
            }

        } catch (IOException ignored){}
    }

    private String parametersToJasmin(ArrayList<Element> parameters) {
        return parameters.stream().map(x -> typeToJasmin(x.getType())).collect(Collectors.joining());
    }

    private String instructionToJasmin(Method parentMethod, Instruction instruction, boolean popReturn) {
        StringBuilder before = new StringBuilder();
        StringBuilder result = new StringBuilder(indent);

        var labels = parentMethod.getLabels(instruction);

        switch (instruction.getInstType()){
            case ASSIGN -> {
                AssignInstruction assignInstruction = (AssignInstruction) instruction;
                var type = assignInstruction.getDest().getType();

                int vreg = varTable.get(((Operand) assignInstruction.getDest()).getName()).getVirtualReg();

                Optional<String> varIncrementOpt = checkVarIncrement(assignInstruction);
                if (varIncrementOpt.isEmpty())
                    before.append(instructionToJasmin(parentMethod, assignInstruction.getRhs(), false));

                switch (type.getTypeOfElement()){
                    case INT32, BOOLEAN -> {
                        try { // Left side of assignment can be array indexing
                            ArrayOperand arrayOperand = (ArrayOperand) assignInstruction.getDest();

                            // In this case, value of assignment comes after loading of array
                            StringBuilder temp = before;
                            before = new StringBuilder();

                            before.append(indent)
                                  .append(loadObjVar(vreg)).append("\n")
                                  .append(indent)
                                  .append(pushElementToStack(arrayOperand.getIndexOperands().get(0))) // TODO Check why there are multiple index operands
                                  .append(temp);
                            result.append(Consts.storeArrayElem).append("\n");

                        } catch (ClassCastException e){ // Otherwise, is int or boolean
                            if (varIncrementOpt.isEmpty())
                                result.append(storeInt(vreg)).append("\n");
                            else
                                result.append(Consts.incrementInt).append(vreg).append(" ").append(varIncrementOpt.get()).append("\n");
                        }
                    }
                    case ARRAYREF, OBJECTREF -> {
                        result.append(storeObjectRef(vreg)).append("\n");
                    }
                    case CLASS -> {
                        System.out.println("CANT ASSIGN VALUE TO CLASS");
                    }
                    case THIS -> {
                        System.out.println("CANT ASSIGN VALUE TO THIS");
                    }
                    case STRING -> {
                        // TODO Allowed?
                    }
                    case VOID -> {
                        System.out.println("CANT ASSIGN VALUE TO VOID");
                    }
                }
            }
            case CALL -> {
                CallInstruction callInstruction = (CallInstruction) instruction;
                Element caller = callInstruction.getFirstArg();
                Element method = callInstruction.getSecondArg();

                switch (callInstruction.getInvocationType()){
                    case invokevirtual ->
                            before.append(indent)
                                    .append(pushElementToStack(caller));
                    case invokespecial -> {
                        before.append(indent)
                                .append(pushElementToStack(caller));
                    }
                    case invokestatic -> {
                    }
                    case NEW -> {
                        String callerName = ((Operand) caller).getName();
                        if (callerName.equals("array")){
                            result.append(Consts.newArray).append(Consts.intType).append(" ");
                        }
                        else {
                            result.append(Consts.newObj).append(callerName);
                        }
                        result.append("\n");
                    }
                    case arraylength -> {
                        before.append(indent)
                              .append(pushElementToStack(caller));
                        result.append(Consts.arrayLength).append("\n");
                    }
                    case ldc, invokeinterface -> {
                        // NOT SUPPORTED
                    }
                }

                var parameters = callInstruction.getListOfOperands();
                if (parameters == null) parameters = new ArrayList<>();

                if (method != null){
                     result.append(callInstruction.getInvocationType()).append(" ")
                           .append(invocationToJasmin(caller, method, callInstruction.getReturnType(),
                                   parametersToJasmin(parameters),
                                   callInstruction.getInvocationType().equals(CallType.invokestatic)));
                }

                for (var op : parameters){
                    before.append(indent)
                          .append(pushElementToStack(op));
                }

                if (popReturn && !callInstruction.getReturnType().getTypeOfElement().equals(ElementType.VOID)) {
                    result.append(indent).append(Consts.pop).append("\n");
                }
            }
            case GOTO -> {
                GotoInstruction gotoInstruction = (GotoInstruction) instruction;
                result.append(Consts.gotoLabel).append(gotoInstruction.getLabel()).append("\n");
            }
            case BRANCH -> {
                CondBranchInstruction condBranchInstruction = (CondBranchInstruction) instruction;
                var left = condBranchInstruction.getLeftOperand();
                var right = condBranchInstruction.getRightOperand();
                var target = condBranchInstruction.getLabel();
                var targetIndex = target.substring(target.lastIndexOf("_"));

                String trueValue = Consts.constant1B + 1 + "\n";
                String falseValue = Consts.constant1B + 0 + "\n";
                switch (condBranchInstruction.getCondOperation().getOpType()){
                    case AND, ANDI32, ANDB ->{
                        String leftElement = pushElementToStack(left);
                        String rightElement = pushElementToStack(right);
                        boolean leftIsTRUE = trueValue.equals(leftElement);
                        boolean rightIsTRUE = trueValue.equals(rightElement);

                        var loopCond = target.startsWith("Loop");
                        var ifCond = target.startsWith("ifbody");

                        // If either is false, will ignore ifbody without any jumps
                        if (falseValue.equals(leftElement) || falseValue.equals(rightElement)) {
                            deadTags.add(target); // ifbody or Loop
                            result.deleteCharAt(0);
                            break;
                        }
                        else if (leftIsTRUE && rightIsTRUE) { // Both are true, elsebody is dead code and while condition is dead code
                            if (ifCond)
                                deadTags.add("elsebody" + targetIndex);
                            else if (loopCond) {
                                deadTags.add("Test" + targetIndex);
                            }
                        }

                        boolean needsIndent = false;
                        if (!leftIsTRUE) {
                            result.append(leftElement).append(indent);

                            if (rightIsTRUE) { // If right element won't be compared, this comparison decides if it goes to ifbody or elsebody
                                result.append(Consts.compTrue).append(target);
                            }
                            else { // Else, must compare
                                result.append(Consts.compFalse)
                                        .append(loopCond ? "End" :
                                                ifCond ? "elsebody" : "ERROR")
                                        .append(targetIndex);
                            }

                            result.append("\n");
                            needsIndent = true;
                        }

                        if (!rightIsTRUE) {
                            result.append(needsIndent ? indent : "")
                                  .append(rightElement)
                                  .append(indent).append(Consts.compTrue).append(target).append("\n");
                        }
                    }
                    case LTH, LTHI32 -> {
                        if (left.isLiteral() && right.isLiteral()){
                            var leftLiteral = Integer.parseInt(((LiteralElement) left).getLiteral());
                            var rightLiteral = Integer.parseInt(((LiteralElement) right).getLiteral());

                            if (leftLiteral < rightLiteral)
                                if (target.startsWith("ifbody"))
                                    deadTags.add("elsebody" + targetIndex);
                                else if (target.startsWith("Loop")) {
                                    deadTags.add("Test" + targetIndex);
                                }
                            else {
                                result.deleteCharAt(0);
                                deadTags.add(target);
                            }
                        }
                        else if (left.isLiteral() && ((LiteralElement) left).getLiteral().equals("0")){ // 0 < a -> push a; iflt
                            result.append(pushElementToStack(right))
                                  .append(indent).append(Consts.compGreaterThanZero).append(target).append("\n");
                        }
                        else if (right.isLiteral() && ((LiteralElement) right).getLiteral().equals("0")){ // a < 0 -> push a; ifgt
                            result.append(pushElementToStack(left))
                                    .append(indent).append(Consts.compLessThanZero).append(target).append("\n");
                        }
                        else {
                            result.append(pushElementToStack(left))
                                    .append(indent).append(pushElementToStack(right))
                                    .append(indent).append(Consts.compLessThan).append(target).append("\n");
                        }
                    }
                    case NOT, NOTB -> {
                        String element = pushElementToStack(left);

                        if (falseValue.equals(element) ) {
                            if (target.startsWith("ifbody")) // elsebody is never reached
                                deadTags.add("elsebody" + targetIndex);
                            else if (target.startsWith("Loop")) {
                                deadTags.add("Test" + targetIndex);
                            }
                        }
                        else if (trueValue.equals(element)) {
                            result.deleteCharAt(0);
                            deadTags.add(target);
                        }
                        else {
                            result.append(element)
                                  .append(indent).append(Consts.compFalse).append(target).append("\n");
                        }
                    }
                    case OR, ORB, ORI32, GTH, GTHI32, EQ, EQI32, GTE, GTEI32, LTE, LTEI32, NEQ, NEQI32 -> {
                        System.out.println("Branch: " + condBranchInstruction.getCondOperation().getOpType());
                        // NOT SUPPORTED
                    }
                    case ADD, SUB, MUL, DIV, SHR, SHL, SHRR, XOR, ADDI32, SUBI32, MULI32, DIVI32, SHRI32, SHLI32, SHRRI32, XORI32 -> {
                        // Not found in ollir conditions
                    }
                }
            }
            case RETURN -> {
                ReturnInstruction returnInstruction = (ReturnInstruction) instruction;
                if (returnInstruction.hasReturnValue()){
                    before.append(indent)
                          .append(pushElementToStack(returnInstruction.getOperand()));
                    result.append(returnToJasmin(returnInstruction.getOperand().getType().getTypeOfElement())).append("\n");
                }
                else
                    result.append(Consts.returnVoidInstr).append("\n");

            }
            case PUTFIELD -> {
                PutFieldInstruction putFieldInstruction = (PutFieldInstruction) instruction;
                Operand secondOperand = (Operand) putFieldInstruction.getSecondOperand();
                before.append(indent)
                      .append(Consts.loadObjectRefSM).append(0).append("\n") // Hardcoded since only fields from this class can be accessed
                      .append(indent)
                      .append(pushElementToStack(putFieldInstruction.getThirdOperand()));

                result.append(Consts.putfield)
                      .append(classPath())
                      .append(secondOperand.getName()).append(" ")
                      .append(typeToJasmin(secondOperand.getType()))
                      .append("\n");
            }
            case GETFIELD -> {
                GetFieldInstruction getFieldInstruction = (GetFieldInstruction) instruction;
                Operand secondOperand = (Operand) getFieldInstruction.getSecondOperand();
                before.append(indent)
                      .append(Consts.loadObjectRefSM).append(0).append("\n"); // Hardcoded since only fields from this class can be accessed

                result.append(Consts.getfield)
                      .append(classPath())
                      .append(secondOperand.getName()).append(" ")
                      .append(typeToJasmin(secondOperand.getType()))
                      .append("\n");
            }
            case BINARYOPER -> {
                BinaryOpInstruction binaryOpInstruction = (BinaryOpInstruction) instruction;
                var opType = binaryOpInstruction.getUnaryOperation().getOpType();

                int rightLiteral = 0;
                try {
                    rightLiteral = Integer.parseInt(((LiteralElement) binaryOpInstruction.getRightOperand()).getLiteral());
                }
                catch (ClassCastException ignored){
                }

                if (opType.equals(OperationType.NOT) ||
                    opType.equals(OperationType.NOTB)) {

                    before.append(indent)
                          .append(pushElementToStack(binaryOpInstruction.getLeftOperand()))
                          .append(indent)
                          .append(Consts.constant1B).append(1).append("\n");
                    result.append(operationToJasmin(binaryOpInstruction.getUnaryOperation()));

                }
                else if ((opType.equals(OperationType.MUL) || opType.equals(OperationType.MULI32) ||
                         opType.equals(OperationType.DIV) || opType.equals(OperationType.DIVI32)) &&
                         // Is being divided or multiplied by a power of 2, can use byte shifts
                         NodeUtils.isPowerOfTwo(rightLiteral) ){

                    LiteralElement alteredRight = new LiteralElement(
                            String.valueOf(log2(rightLiteral)),
                            new Type(ElementType.INT32));

                    before.append(indent)
                           .append(pushElementToStack(binaryOpInstruction.getLeftOperand()))
                           .append(indent)
                           .append(pushElementToStack(alteredRight));

                    result.append((opType.equals(OperationType.MUL) || opType.equals(OperationType.MULI32) ?
                            Consts.shiftLeft : Consts.shiftRight )).append("\n");
                }
                else {
                    before.append(indent)
                          .append(pushElementToStack(binaryOpInstruction.getLeftOperand()))
                          .append(indent)
                          .append(pushElementToStack(binaryOpInstruction.getRightOperand()));
                    result.append(operationToJasmin(binaryOpInstruction.getUnaryOperation()));
                }
            }
            case NOPER -> {
                SingleOpInstruction singleOpInstruction = (SingleOpInstruction) instruction;
                result.append(pushElementToStack(singleOpInstruction.getSingleOperand()));
            }
            case UNARYOPER -> {
                // NOT SUPPORTED
            }
        }

        StringBuilder labelsStr = new StringBuilder();
        for(var label : labels)
            labelsStr.append(label).append(":\n");

        return labelsStr.toString() + before + result;
    }

    private Optional<String> checkVarIncrement(AssignInstruction instruction) {
        try{
            var rhs = (BinaryOpInstruction)instruction.getRhs();
            var opType = rhs.getUnaryOperation().getOpType();

            if ((opType.equals(OperationType.ADD) || opType.equals(OperationType.SUB))){ // Operation must be simple add or sub
                var dest = (Operand)instruction.getDest();
                var leftOp = (rhs.getLeftOperand().isLiteral() ? null : ((Operand) rhs.getLeftOperand()));
                var rightOp = (rhs.getRightOperand().isLiteral() ? null : ((Operand) rhs.getRightOperand()));

                if (leftOp != null &&  dest.getName().equals(leftOp.getName())){
                    var leftVal = (opType.equals(OperationType.SUB) ? "-" : "") + ((LiteralElement)rhs.getRightOperand()).getLiteral();
                    if (Integer.parseInt(leftVal) >= -128 && Integer.parseInt(leftVal) <= 127)
                        return Optional.of(leftVal);
                }
                else if (rightOp != null &&  dest.getName().equals(rightOp.getName())){
                    var rightVal = (opType.equals(OperationType.SUB) ? "-" : "") + ((LiteralElement)rhs.getLeftOperand()).getLiteral();
                    if (Integer.parseInt(rightVal) >= -128 && Integer.parseInt(rightVal) <= 127)
                        return Optional.of(rightVal);
                }
            }
            return Optional.empty();
        }
        catch (ClassCastException ignored){
            return Optional.empty();
        }
    }

    private String returnToJasmin(ElementType elementType) {
        if (elementType.equals(ElementType.INT32) || elementType.equals(ElementType.BOOLEAN))
            return Consts.returnInt;
        else
            return Consts.returnObjectRef;
    }

    private String classPath() {
        return classUnit.getClassName() + "/";
    }

    private String fullClassName(String className){
        if(className.equals(classUnit.getClassName()) || className.equals(classUnit.getSuperClass()))
            return className + "/";
        else
            return importedClass(className) + "/";
    }

    private String importedClass(String className){
        for (String fullName : classUnit.getImports()){
            if (fullName.substring(fullName.lastIndexOf(".") + 1).trim().equals(className))
                return fullName.replace(".", "/");
        }
        return "ERRRRR";
    }

    private String invocationToJasmin(Element caller, Element method, Type callType, String parameters, boolean isStatic) {
        LiteralElement literalMethod;
        ClassType callerType = (ClassType) caller.getType();

        try{
            literalMethod = (LiteralElement) method;
        } catch (ClassCastException ignored){
            System.out.println("Method name isnt literal ????");
            return "ERRR";
        }
//        System.out.println(callerType.getName() + " " + literalMethod.getLiteral());

        if (literalMethod.getLiteral().equals("\"<init>\""))
            return (caller.getType().getTypeOfElement().equals(ElementType.THIS) ?
                    (classUnit.getSuperClass() != null ?
                            classUnit.getSuperClass() : "java/lang/Object") + "/" : fullClassName(callerType.getName()))
                    + "<init>()V\n";

        String methodName = literalMethod.getLiteral().replace("\"", "");
        return fullClassName(isStatic ? ((Operand) caller).getName() : callerType.getName()) + methodName
                + "(" + parameters + ")" + typeToJasmin(callType) + "\n";

    }

    private String operationToJasmin(Operation operation) {
        switch (operation.getOpType()){
            case ADD, ADDI32 -> {
                return Consts.addInt + "\n";
            }
            case SUB, SUBI32 -> {
                return Consts.subInt + "\n";
            }
            case MUL, MULI32 -> {
                return Consts.mulInt + "\n";
            }
            case DIV, DIVI32 -> {
                return Consts.divInt + "\n";
            }
            case AND, ANDI32, ANDB -> {
                return Consts.andInt + "\n";
            }
            case LTH, LTHI32 -> {
                return Consts.subInt + "\n" +
                        indent + Consts.constant2B + 31 + "\n" + // Hardcoded 32 bit right shift
                        indent + Consts.unsignedShiftRight + "\n";
            }
            case NOT, NOTB -> { // Since all boolean values have been checked (are always 0 or 1), we can use this method, which is faster than the if approach
                return Consts.notInt + "\n";
            }
            case SHR, XOR, SHRR, SHL, OR, GTH, EQ, NEQ, LTE, GTE, SHRI32, SHLI32, SHRRI32, XORI32, ORI32, GTHI32, EQI32, NEQI32, LTEI32, GTEI32, ORB -> {
                // NOT SUPPORTED
            }
        }
        return null;
    }

    private String pushElementToStack(Element element) {
        LiteralElement literal = null;
        Operand operand = null;

        if (element.isLiteral())
            literal = (LiteralElement) element;
        else
            operand = (Operand) element;

        switch (element.getType().getTypeOfElement()){
            case INT32 -> {
                if (element.isLiteral()){
                    if (Integer.parseInt(literal.getLiteral()) == -1) {
                        return Consts.constantMinus1 + "\n";
                    }
                    else if (Integer.parseInt(literal.getLiteral()) == 0){
                        return Consts.constant1B + 0 + "\n";
                    }
                    else if (Integer.parseInt(literal.getLiteral()) > 0 && Integer.parseInt(literal.getLiteral()) <= 5){
                        return Consts.constant1B + literal.getLiteral() + "\n";
                    }
                    else if (Integer.parseInt(literal.getLiteral()) >= -128 && Integer.parseInt(literal.getLiteral()) <= 127){
                        return Consts.constant2B + literal.getLiteral() + "\n";
                    }
                    else if (Integer.parseInt(literal.getLiteral()) >= -32768 && Integer.parseInt(literal.getLiteral()) <= 32767){
                        return Consts.constant3B + literal.getLiteral() + "\n";
                    }
                    else {
                        return Consts.constant4B + literal.getLiteral() + "\n";
                    }
                }
                else { // Is variable holding int, can still be array index
                    try {
                        ArrayOperand arrayOperand = (ArrayOperand) element;
                        var res = loadObjVar(varTable.get(arrayOperand.getName()).getVirtualReg()) + "\n" + indent +
                               pushElementToStack(arrayOperand.getIndexOperands().get(0)) +  indent + // TODO Check why there are multiple index operands
                               Consts.loadArrayElem + "\n";
                        return res;
                    }
                    catch (ClassCastException e){
                        return loadIntVar(varTable.get(operand.getName()).getVirtualReg()) + "\n";
                    }
                }

            }
            case BOOLEAN -> {
                if (operand.getName().equals("false"))
                    return Consts.constant1B + 0 + "\n";
                else if (operand.getName().equals("true"))
                    return Consts.constant1B + 1 + "\n";
                else
                    return loadIntVar(varTable.get(operand.getName()).getVirtualReg()) + "\n";
            }
            case ARRAYREF, OBJECTREF -> {
                var vreg = varTable.get(operand.getName()).getVirtualReg();
                return loadObjVar(vreg) + "\n";
            }
            case CLASS -> {
                return "";
            }
            case THIS -> {
                return Consts.loadObjectRefSM + "0" + "\n";
            }
            case STRING -> {
                return "";
            }
            case VOID -> {
                System.out.println("Cant push void");
            }
        }

        return null;
    }

    private void removeDeadCodeTags(StringBuilder result) {
        for (var deadTag : deadTags){
            var nextTag = deadTag.startsWith("ifbody") ? "endif" :
                    deadTag.startsWith("elsebody") ? "ifbody" :
                            (deadTag.startsWith("Loop") || deadTag.startsWith("Test")) ? "End" : "ERROR";
            nextTag += deadTag.substring(deadTag.lastIndexOf("_"));

            System.out.println(deadTag + " " + nextTag);

            var gotoTestLabel = result.indexOf(Consts.gotoLabel + "Test" + nextTag.substring(nextTag.lastIndexOf("_"))) - 1;

            if (deadTag.startsWith("ifbody")){
                result.delete(result.indexOf(Consts.gotoLabel + nextTag) - 1,
                        result.indexOf(nextTag, result.indexOf(nextTag) + 1));
            }
            else if (deadTag.startsWith("Loop")){
                result.delete(gotoTestLabel,
                        result.indexOf(nextTag) + nextTag.length() + 2);
            }
            else if (deadTag.startsWith("Test")){
                result.delete(gotoTestLabel,
                        result.indexOf("\n", gotoTestLabel) + 1);
                result.delete(result.lastIndexOf(deadTag),
                        result.indexOf(nextTag));
                result.insert(result.lastIndexOf(nextTag) + deadTag.length() + 1,
                        indent + Consts.gotoLabel + "Loop" + nextTag.substring(nextTag.lastIndexOf("_")) + "\n");
            }
            else
                result.delete(result.indexOf(deadTag) - 1,
                        result.indexOf(nextTag));
        }
    }

    private String loadIntVar(int vreg){
        return (vreg >= 0 && vreg <= 3 ? Consts.loadIntVarSM : Consts.loadIntVar) + vreg;
    }

    private String loadObjVar(int vreg){
        return (vreg >= 0 && vreg <= 3 ? Consts.loadObjectRefSM : Consts.loadObjectRef) + vreg;
    }

    private String storeInt(int vreg){
        return (vreg >= 0 && vreg <= 3 ? Consts.storeIntSM : Consts.storeInt) + vreg;
    }

    private String storeObjectRef(int vreg){
        return (vreg >= 0 && vreg <= 3 ? Consts.storeObjRefSM : Consts.storeObjRef) + vreg;
    }

    private String fieldToJasmin(Field field) {
        return ".field " + accessModifierToJasmin(field.getFieldAccessModifier()) + " " + field.getFieldName() + " " + typeToJasmin(field.getFieldType()) + "\n";
    }

    private String typeToJasmin(ElementType type){
        switch (type){
            case INT32 -> {
                return "I";
            }
            case BOOLEAN -> {
                return "Z";
            }
            case ARRAYREF, OBJECTREF, CLASS, THIS -> {
                // Wont reach here
                return "";
            }
            case STRING -> {
                return "Ljava/lang/String;";
            }
            case VOID -> {
                return "V";
            }
            default -> {
                return "";
            }
        }
    }

    private String typeToJasmin(Type type){
        switch (type.getTypeOfElement()){
            case INT32, BOOLEAN, STRING, VOID -> {
                return typeToJasmin(type.getTypeOfElement());
            }
            case ARRAYREF -> {
                ArrayType arrayType = (ArrayType) type;
                return "[" + typeToJasmin(arrayType.getTypeOfElements());
            }
            case OBJECTREF, CLASS -> {
                ClassType classType = (ClassType) type;
                type.show();
                return "L" + classType.getName() + ";";
            }
            case THIS -> {
                return "";
            }
            default -> {
                return "";
            }
        }
    }

    private String accessModifierToJasmin(AccessModifiers classAccessModifier) {
        return String.valueOf(classAccessModifier.equals(AccessModifiers.DEFAULT) ? AccessModifiers.PUBLIC : classAccessModifier).toLowerCase();
    }
}
