import org.specs.comp.ollir.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Optional;
import java.util.stream.Collectors;

public class Jasmin {
    private final String indent = "\t";
    private HashMap<String, Descriptor> varTable;
    private HashMap<Instruction, String> methodLabels;
    private ClassUnit classUnit;

    private int maxStackSize;
    private int currStackSize;
    private int maxLocalsSize;

    private void incrementStack(int n){
        currStackSize += n;
        if (currStackSize > maxStackSize) maxStackSize = currStackSize;
//        System.out.println(currStackSize);
    }

    private void updateMaxLocals(int vreg) {
        if (vreg + 1 > maxLocalsSize) maxLocalsSize = vreg + 1;
    }

    public String getByteCode(ClassUnit classUnit, int dashR, boolean dashO) throws OllirErrorException {
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
            stringBuilder.append(methodToJasmin(method, dashR));
        }

        return stringBuilder.toString();
    }

    private String methodToJasmin(Method method, int dashR) throws OllirErrorException {
        StringBuilder start = new StringBuilder();
        StringBuilder result = new StringBuilder();

        varTable = method.getVarTable();
        methodLabels = new HashMap<>();
        for (var entry : method.getLabels().entrySet()){
            methodLabels.put(entry.getValue(), entry.getKey());
        }

        maxStackSize = 0;
        currStackSize = 0;
        maxLocalsSize = 1;

//        method.show();

        start.append(".method ")
            .append(accessModifierToJasmin(method.getMethodAccessModifier()))
            .append(method.isStaticMethod() ? " static " : " ")
            .append(method.isConstructMethod() ? "<init>" : method.getMethodName())
            .append("(")
                .append(parametersToJasmin(method.getParams()))
            .append(")")
            .append(typeToJasmin(method.getReturnType())).append("\n");

            method.getInstructions().forEach(x -> result.append(instructionToJasmin(x, true)));
            // method.getInstructions().forEach(Instruction::show);

        if (method.isConstructMethod())
            result.append(indent).append(Constants.returnVoidInstr).append("\n");
        result.append(".end method").append("\n\n");

        if (!method.isConstructMethod()){
            start.append(indent).append(".limit stack ").append(maxStackSize).append("\n")
                    .append(indent).append(".limit locals ").append(maxLocalsSize).append("\n");
        }

        if (maxLocalsSize > dashR){
            throw new OllirErrorException("Cannot compile code with '" + dashR + "' local variables. Need atleast '" + maxLocalsSize + "'");
        }

        return start.append(result).toString();
    }

    private String parametersToJasmin(ArrayList<Element> parameters) {
        return parameters.stream().map(x -> typeToJasmin(x.getType())).collect(Collectors.joining());
    }

    private String instructionToJasmin(Instruction instruction, boolean popReturn) {
        StringBuilder before = new StringBuilder();
        StringBuilder result = new StringBuilder(indent);

        String label = methodLabels.get(instruction);

        switch (instruction.getInstType()){
            case ASSIGN -> {
                AssignInstruction assignInstruction = (AssignInstruction) instruction;
                var type = assignInstruction.getDest().getType();

                int vreg = varTable.get(((Operand) assignInstruction.getDest()).getName()).getVirtualReg();
                updateMaxLocals(vreg);

                Optional<String> varIncrementOpt = checkVarIncrement(assignInstruction);
                if (varIncrementOpt.isEmpty())
                    before.append(instructionToJasmin(assignInstruction.getRhs(), false));

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
                            result.append(Constants.storeArrayElem).append("\n");
                            incrementStack(-3);

                        } catch (ClassCastException e){ // Otherwise, is int or boolean
                            if (varIncrementOpt.isEmpty())
                                result.append(storeInt(vreg)).append("\n");
                            else
                                result.append(Constants.incrementInt).append(vreg).append(" ").append(varIncrementOpt.get()).append("\n");
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
                        incrementStack(-1);
                    }
                    case invokestatic -> {
                        incrementStack(1);
                    }
                    case NEW -> {
                        incrementStack(1);
                        String callerName = ((Operand) caller).getName();
                        result.append(callerName.equals("array") ?
                                Constants.newArray :
                                Constants.newObj + callerName)
                                .append("\n");
                    }
                    case arraylength -> {
                        before.append(indent)
                              .append(pushElementToStack(caller));
                        result.append(callInstruction.getInvocationType()).append("\n");
                    }
                    case ldc -> {
                        // NOT SUPPORTED ? TODO
                    }
                    case invokeinterface -> {
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

                if (popReturn && !callInstruction.getReturnType().getTypeOfElement().equals(ElementType.VOID))
                    result.append(indent).append("pop\n");

                for (var op : parameters){
                    before.append(indent)
                          .append(pushElementToStack(op));
                }
                incrementStack(parameters.size());
            }
            case GOTO -> {
                GotoInstruction gotoInstruction = (GotoInstruction) instruction;
                result.append(Constants.gotoLabel).append(gotoInstruction.getLabel()).append("\n");
            }
            case BRANCH -> {
                CondBranchInstruction condBranchInstruction = (CondBranchInstruction) instruction;
                var left = condBranchInstruction.getLeftOperand();
                var right = condBranchInstruction.getRightOperand();
                var target = condBranchInstruction.getLabel();

                switch (condBranchInstruction.getCondOperation().getOpType()){
                    case OR, ORB, ORI32, GTH, GTHI32, EQ, EQI32, GTE, GTEI32, LTE, LTEI32, NEQ, NEQI32, NOT, NOTB -> {
                        // NOT SUPPORTED
                    }
                    case AND, ANDI32, ANDB ->{
                        String leftElement = pushElementToStack(left);
                        String rightElement = pushElementToStack(right);
                        String trueValue = Constants.constant1B + 1 + "\n";

                        if (!trueValue.equals(leftElement)) {
                            incrementStack(-1);
                            result.append(leftElement)
                                    .append(indent).append(Constants.compTrue).append(target).append("\n");
                        }

                        if (!trueValue.equals(rightElement)) {
                            incrementStack(-1);
                            result.append(rightElement)
                                    .append(indent).append(Constants.compTrue).append(target).append("\n");
                        }

                        if (trueValue.equals(leftElement) && trueValue.equals(rightElement))
                            result.append(Constants.gotoLabel).append(target).append("\n");
                    }


                    case LTH, LTHI32 -> {
                        result.append(pushElementToStack(left))
                                .append(indent).append(pushElementToStack(right))
                                .append(indent).append(Constants.compLessThan).append(target).append("\n");
                        incrementStack(-2);
                    }

                    case ADD, SUB, MUL, DIV, SHR, SHL, SHRR, XOR, ADDI32, SUBI32, MULI32, DIVI32, SHRI32, SHLI32, SHRRI32, XORI32 -> {
                        System.out.println("Incompatible operation in condition ; Something wrong in semantic analysis");
                    }
                }
                // In progress

            }
            case RETURN -> {
                ReturnInstruction returnInstruction = (ReturnInstruction) instruction;
                if (returnInstruction.hasReturnValue()){
                    before.append(indent)
                          .append(pushElementToStack(returnInstruction.getOperand()));
                    result.append(returnToJasmin(returnInstruction.getOperand().getType().getTypeOfElement())).append("\n");
                }
                else
                    result.append(Constants.returnVoidInstr).append("\n");

            }
            case PUTFIELD -> {
                PutFieldInstruction putFieldInstruction = (PutFieldInstruction) instruction;
                Operand secondOperand = (Operand) putFieldInstruction.getSecondOperand();
                before.append(indent)
                      .append(Constants.loadObjectRefSM).append(0).append("\n") // Hardcoded since only fields from this class can be accessed
                      .append(indent)
                      .append(pushElementToStack(putFieldInstruction.getThirdOperand()));

                incrementStack(1);

                result.append(Constants.putfield)
                      .append(classPath())
                      .append(secondOperand.getName()).append(" ")
                      .append(typeToJasmin(secondOperand.getType()))
                      .append("\n");

                incrementStack(-2);
            }
            case GETFIELD -> {
                GetFieldInstruction getFieldInstruction = (GetFieldInstruction) instruction;
                Operand secondOperand = (Operand) getFieldInstruction.getSecondOperand();
                before.append(indent)
                      .append(Constants.loadObjectRefSM).append(0).append("\n"); // Hardcoded since only fields from this class can be accessed

                result.append(Constants.getfield)
                      .append(classPath())
                      .append(secondOperand.getName()).append(" ")
                      .append(typeToJasmin(secondOperand.getType()))
                      .append("\n");

                incrementStack(1);
            }
            case BINARYOPER -> {
                BinaryOpInstruction binaryOpInstruction = (BinaryOpInstruction) instruction;
                if (binaryOpInstruction.getUnaryOperation().getOpType().equals(OperationType.NOT) ||
                    binaryOpInstruction.getUnaryOperation().getOpType().equals(OperationType.NOTB)) {
                    incrementStack(1);
                    before.append(indent)
                          .append(pushElementToStack(binaryOpInstruction.getLeftOperand()))
                          .append(indent)
                          .append(Constants.constant1B).append(1).append("\n");
                    result.append(operationToJasmin(binaryOpInstruction.getUnaryOperation()));

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

        return (label != null ? label + ":\n" : "") +
                before + result;
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
        incrementStack(-1);
        if (elementType.equals(ElementType.INT32) || elementType.equals(ElementType.BOOLEAN))
            return Constants.returnInt;
        else
            return Constants.returnObjectRef;
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
        if (className.equals("out")) return "io"; // TODO ??????
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
                    "java/lang/Object/" : fullClassName(callerType.getName()))
                    + "<init>()V\n";

        String methodName = literalMethod.getLiteral().replace("\"", "");
        return fullClassName(isStatic ? ((Operand) caller).getName() : callerType.getName()) + methodName
                + "(" + parameters + ")" + typeToJasmin(callType) + "\n";

    }

    private String methodType(Element caller, String methodName) {
        if (caller.getType().getTypeOfElement().equals(ElementType.THIS) ||
            ((ClassType) caller.getType()).getName().equals(classUnit.getClassName())) {
            for (var method : classUnit.getMethods()){
                if (method.getMethodName().equals(methodName))
                    return typeToJasmin(method.getReturnType());
            }
            return "V"; //TODO Temporarily is V, should be ERR
        }
        else {
            return "V";
        }
    }

    private String operationToJasmin(Operation operation) {
        switch (operation.getOpType()){
            case ADD, ADDI32 -> {
                incrementStack(-1);
                return Constants.addInt + "\n";
            }
            case SUB, SUBI32 -> {
                incrementStack(-1);
                return Constants.subInt + "\n";
            }
            case MUL, MULI32 -> {
                incrementStack(-1);
                return Constants.mulInt + "\n";
            }
            case DIV, DIVI32 -> {
                incrementStack(-1);
                return Constants.divInt + "\n";
            }
            case AND, ANDI32, ANDB -> {
                incrementStack(-1);
                return Constants.andInt + "\n";
            }
            case LTH, LTHI32 -> {
                incrementStack(-1);
                return Constants.subInt + "\n" +
                        indent + Constants.constant2B + 31 + "\n" + // Hardcoded 32 bit right shift
                        indent + Constants.shiftR + "\n" +
                        indent + Constants.negateInt + "\n";
            }
            case NOT, NOTB -> { // Since all boolean values have been checked (are always 0 or 1), we can use this method, which is faster than the if approach
                incrementStack(-1);
                return Constants.notInt + "\n";
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
                    incrementStack(1);

                    if (Integer.parseInt(literal.getLiteral()) == -1) {
                        return Constants.constantMinus1 + "\n";
                    }
                    else if (Integer.parseInt(literal.getLiteral()) == 0){
                        return Constants.constant1B + 0 + "\n";
                    }
                    else if (Integer.parseInt(literal.getLiteral()) > 0 && Integer.parseInt(literal.getLiteral()) <= 5){
                        return Constants.constant1B + literal.getLiteral() + "\n";
                    }
                    else if (Integer.parseInt(literal.getLiteral()) >= -128 && Integer.parseInt(literal.getLiteral()) <= 127){
                        return Constants.constant2B + literal.getLiteral() + "\n";
                    }
                    else if (Integer.parseInt(literal.getLiteral()) >= -32768 && Integer.parseInt(literal.getLiteral()) <= 32767){
                        return Constants.constant3B + literal.getLiteral() + "\n";
                    }
                    else {
                        return Constants.constant4B + literal.getLiteral() + "\n";
                    }
                }
                else { // Is variable holding int, can still be array index
                    try {
                        ArrayOperand arrayOperand = (ArrayOperand) element;
                        return loadObjVar(varTable.get(arrayOperand.getName()).getVirtualReg()) + "\n" + indent +
                               pushElementToStack(arrayOperand.getIndexOperands().get(0)) +  indent + // TODO Check why there are multiple index operands
                               Constants.loadArrayElem + "\n";
                    }
                    catch (ClassCastException e){
                        return loadIntVar(varTable.get(operand.getName()).getVirtualReg()) + "\n";
                    }
                }

            }
            case BOOLEAN -> {
                if (operand.getName().equals("false")) {
                    incrementStack(1);
                    return Constants.constant1B + 0 + "\n";
                }
                else if (operand.getName().equals("true")) {
                    incrementStack(1);
                    return Constants.constant1B + 1 + "\n";
                }
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
                incrementStack(1);
                return Constants.loadObjectRefSM + "0" + "\n";
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

    private String loadIntVar(int vreg){
        incrementStack(1);
        return (vreg >= 0 && vreg <= 3 ? Constants.loadIntVarSM : Constants.loadIntVar) + vreg;
    }

    private String loadObjVar(int vreg){
        incrementStack(1);
        return (vreg >= 0 && vreg <= 3 ? Constants.loadObjectRefSM : Constants.loadObjectRef) + vreg;
    }

    private String storeInt(int vreg){
        incrementStack(-1);
        return (vreg >= 0 && vreg <= 3 ? Constants.storeIntSM : Constants.storeInt) + vreg;
    }

    private String storeObjectRef(int vreg){
        incrementStack(-1);
        return (vreg >= 0 && vreg <= 3 ? Constants.storeObjRefSM : Constants.storeObjRef) + vreg;
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
