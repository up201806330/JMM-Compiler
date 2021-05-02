import org.specs.comp.ollir.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.stream.Collectors;

public class Jasmin {
    private final String indent = "\t";
    private HashMap<String, Descriptor> varTable;
    private ClassUnit classUnit;

    public String getByteCode(ClassUnit classUnit) throws OllirErrorException {
        this.classUnit = classUnit;

        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(".class ")
                .append(accessModifierToJasmin(classUnit.getClassAccessModifier())).append(" ")
                .append(classUnit.getClassName()).append("\n");

        if (classUnit.getSuperClass() != null)
            stringBuilder.append(".super ").append(classUnit.getSuperClass()).append("\n\n");
        else
            stringBuilder.append(".super java/lang/Object").append("\n\n");


        classUnit.getFields().forEach(x -> stringBuilder.append(fieldToJasmin(x)));
        stringBuilder.append("\n");

        classUnit.buildVarTables();
        classUnit.getMethods().forEach(x -> stringBuilder.append(methodToJasmin(x)));

        return stringBuilder.toString();
    }

    private String methodToJasmin(Method method){
        StringBuilder result = new StringBuilder();
        varTable = method.getVarTable();

//        method.show();

        result.append(".method ")
            .append(accessModifierToJasmin(method.getMethodAccessModifier()))
            .append(method.isStaticMethod() ? " static " : " ")
            .append(method.isConstructMethod() ? "<init>" : method.getMethodName())
            .append("(")
                .append(parametersToJasmin(method.getParams()))
            .append(")")
            .append(typeToJasmin(method.getReturnType())).append("\n");

            if (!method.isConstructMethod()){
                result.append(indent).append(".limit stack 99").append("\n")   // For checkpoint 2 is allowed
                      .append(indent).append(".limit locals 99").append("\n");
            }

            method.getInstructions().forEach(x -> result.append(instructionToJasmin(x)));
            // method.getInstructions().forEach(Instruction::show);

        if (method.isConstructMethod())
            result.append(indent).append(Constants.returnVoidInstr).append("\n");
        result.append(".end method").append("\n\n");

        return result.toString();
    }

    private String parametersToJasmin(ArrayList<Element> parameters) {
        return parameters.stream().map(x -> typeToJasmin(x.getType())).collect(Collectors.joining());
    }

    private String instructionToJasmin(Instruction instruction) {
        StringBuilder before = new StringBuilder();
        StringBuilder result = new StringBuilder(indent);

        switch (instruction.getInstType()){
            case ASSIGN -> {
                AssignInstruction assignInstruction = (AssignInstruction) instruction;
                var type = assignInstruction.getDest().getType();

                int vreg = varTable.get(((Operand) assignInstruction.getDest()).getName()).getVirtualReg();

                switch (type.getTypeOfElement()){
                    case INT32, BOOLEAN -> {
                        try { // Left side of assignment can be array indexing
                            ArrayOperand arrayOperand = (ArrayOperand) assignInstruction.getDest();
                            before.append(indent)
                                  .append(pushElementToStack(arrayOperand))
                                  .append(indent)
                                  .append(pushElementToStack(arrayOperand.getIndexOperands().get(0))); // TODO Check why there are multiple index operands
                            result.append(Constants.storeArrayElem).append("\n");
                        } catch (ClassCastException e){ // Otherwise, is int or boolean
                            result.append(storeInt(vreg)).append("\n");
                        }

                    }
                    case ARRAYREF -> {
                        result.append(storeArrayRef(vreg)).append("\n");
                    }
                    case OBJECTREF -> {
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

                before.append(instructionToJasmin(assignInstruction.getRhs()));
            }
            case CALL -> {
                CallInstruction callInstruction = (CallInstruction) instruction;
                Element caller = callInstruction.getFirstArg();
                Element method = callInstruction.getSecondArg();

                switch (callInstruction.getInvocationType()){
                    case invokevirtual, invokespecial -> {
                        before.append(indent)
                                .append(pushElementToStack(caller));
                    }
                    case invokestatic -> {
                    }
                    case NEW -> {
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
                           .append(invocationToJasmin(caller, method, parametersToJasmin(parameters), callInstruction.getInvocationType().equals(CallType.invokestatic)));
                }

                parameters.forEach(op ->
                        before.append(indent)
                                .append(pushElementToStack(op))
                );
            }
            case GOTO -> {
            }
            case BRANCH -> {
                CondBranchInstruction condBranchInstruction = (CondBranchInstruction) instruction;
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

                result.append(Constants.putfield)
                      .append(classPath())
                      .append(secondOperand.getName()).append(" ")
                      .append(typeToJasmin(secondOperand.getType()))
                      .append("\n");
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
            }
            case BINARYOPER -> {
                BinaryOpInstruction binaryOpInstruction = (BinaryOpInstruction) instruction;
                if (binaryOpInstruction.getUnaryOperation().getOpType().equals(OperationType.NOT) ||
                    binaryOpInstruction.getUnaryOperation().getOpType().equals(OperationType.NOTB)) {
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

        return before.append(result).toString();
    }

    private String returnToJasmin(ElementType elementType) {
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
        for (String fullName : classUnit.getImports()){
            if (fullName.substring(fullName.lastIndexOf(".") + 1).trim().equals(className))
                return fullName.replace(".", "/");
        }
        return "ERRRRR";
    }

    private String invocationToJasmin(Element caller, Element method, String parameters, boolean isStatic) {
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
                + "(" + parameters + ")" + methodType(caller, methodName) + "\n";

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
                return Constants.addInt + "\n";
            }
            case SUB, SUBI32 -> {
                return Constants.subInt + "\n";
            }
            case MUL, MULI32 -> {
                return Constants.mulInt + "\n";
            }
            case DIV, DIVI32 -> {
                return Constants.divInt + "\n";
            }
            case AND, ANDI32, ANDB -> {
                return Constants.andInt + "\n";
            }
            case LTH, LTHI32 -> {
            }
            case NOT, NOTB -> { // Since all boolean values have been checked (are always 0 or 1), we can use this method, which is faster than the if approach
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
                    if (Integer.parseInt(literal.getLiteral()) == -1) {
                        return Constants.constantMinus1 + "\n";
                    }
                    else if (Integer.parseInt(literal.getLiteral()) >= 0 && Integer.parseInt(literal.getLiteral()) <= 5){
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
                        return loadObjVar(varTable.get(arrayOperand.getName()).getVirtualReg()) + "\n";
                    }
                    catch (ClassCastException e){
                        return loadIntVar(varTable.get(operand.getName()).getVirtualReg()) + "\n";
                    }
                }

            }
            case BOOLEAN -> {
                return Constants.constant1B + (operand.getName().equals("false") ? 0 : 1) + "\n";
            }
            case ARRAYREF, OBJECTREF -> {
                var vreg = varTable.get(operand.getName()).getVirtualReg();
                return loadObjVar(vreg) + "\n";
            }
            case CLASS -> {
                return "";
            }
            case THIS -> {
                return Constants.loadObjectRefSM + "0" + "\n";
            }
            case STRING -> {
                // Constant pool bish were
            }
            case VOID -> {
                System.out.println("Cant push void");
            }
        }

        return null;
    }

    private String loadIntVar(int vreg){
        return (vreg >= 0 && vreg <= 3 ? Constants.loadIntVarSM : Constants.loadIntVar) + vreg;
    }

    private String loadObjVar(int vreg){
        return (vreg >= 0 && vreg <= 3 ? Constants.loadObjectRefSM : Constants.loadObjectRef) + vreg;
    }

    private String storeInt(int vreg){
        return (vreg >= 0 && vreg <= 3 ? Constants.storeIntSM : Constants.storeInt) + vreg;
    }

    private String storeArrayRef(int vreg){
        return (vreg >= 0 && vreg <= 3 ? Constants.storeArrayRefSM : Constants.storeArrayRef) + vreg;
    }

    private String storeObjectRef(int vreg){
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
