import org.specs.comp.ollir.*;

import java.util.HashMap;

public class Jasmin {
    private final String ident = "  ";
    private HashMap<String, Descriptor> varTable;

//    Example of what you can do with the OLLIR class
//    ollirClass.checkMethodLabels(); // check the use of labels in the OLLIR loaded
//    ollirClass.buildCFGs(); // build the CFG of each method
//    ollirClass.outputCFGs(); // output to .dot files the CFGs, one per method
//    ollirClass.buildVarTables(); // build the table of variables for each method
//    ollirClass.show(); // print to console main information about the input OLLIR

    public String getByteCode(ClassUnit classUnit) throws OllirErrorException {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(".class ")
                .append(accessModifierToString(classUnit.getClassAccessModifier())).append(" ")
                .append(classUnit.getClassName()).append("\n");

        if (classUnit.getSuperClass() != null)
            stringBuilder.append(".super ").append(classUnit.getSuperClass()).append("\n\n");

        classUnit.getFields().forEach(x -> stringBuilder.append(fieldToString(x)));
        stringBuilder.append("\n");

        classUnit.buildVarTables();
        classUnit.getMethods().forEach(x -> stringBuilder.append(methodToString(x)));

        return stringBuilder.toString();
    }

    private String methodToString(Method method){
        StringBuilder result = new StringBuilder();
        varTable = method.getVarTable();

        method.show();

        result.append(".method ")
            .append(accessModifierToString(method.getMethodAccessModifier()))
            .append(method.isStaticMethod() ? " static " : " ")
            .append(method.isConstructMethod() ? "<init>" : method.getMethodName()).append("()")
            .append(typeToString(method.getReturnType())).append("\n");

            if (!method.isConstructMethod()){
                result.append(ident).append(".limit stack 99").append("\n")   // For checkpoint 2 is allowed
                      .append(ident).append(".limit locals 99").append("\n");
            }

            method.getInstructions().forEach(x -> result.append(instructionToString(x)).append("\n"));
//            method.getInstructions().forEach(Instruction::show);

        result.append(".end method").append("\n\n");

        return result.toString();
    }

    private String instructionToString(Instruction instruction) {
        StringBuilder before = new StringBuilder();
        StringBuilder result = new StringBuilder();

        switch (instruction.getInstType()){
            case ASSIGN -> {
                AssignInstruction assignInstruction = (AssignInstruction) instruction;
                var type = assignInstruction.getDest().getType();

                ArrayOperand arrayDest = null;
                int vreg;
                if (type.getTypeOfElement().equals(ElementType.ARRAYREF)){
                    arrayDest = (ArrayOperand) assignInstruction.getDest();
                    vreg = varTable.get(arrayDest.getName()).getVirtualReg();
                }
                else {
                    vreg = varTable.get(((Operand) assignInstruction.getDest()).getName()).getVirtualReg();
                }

                result.append(ident);
                switch (type.getTypeOfElement()){
                    case INT32, BOOLEAN -> {
                        result.append(Constants.storeInt).append(vreg).append("\n");
                    }
                    case ARRAYREF -> {
                        before.append(loadLocalVar(vreg)).append("\n")
                              .append(pushElementToStack(arrayDest.getIndexOperands().get(0))).append("\n");

                        result.append(Constants.storeArrayElem).append("\n"); // Hardcoded because all arrays are int[]
                    }
                    case OBJECTREF -> {
                        result.append(vreg >= 0 && vreg <= 3 ? Constants.storeObjRefSM : Constants.storeObjRef).append(vreg).append("\n");
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

                before.append(instructionToString(assignInstruction.getRhs()));
            }
            case CALL -> {
                CallInstruction callInstruction = (CallInstruction) instruction;
                if (callInstruction.getFirstArg().isLiteral()){
                    System.out.println("CALLER IS LITERAL");
                    callInstruction.getFirstArg().show();
                }
                Element caller = callInstruction.getFirstArg();
                if (callInstruction.getNumOperands() > 1) {
                    if (!callInstruction.getInvocationType().equals(CallType.invokestatic)){
                        before.append(ident)
                              .append(pushElementToStack(caller));
                    }

                    Element secondArg = callInstruction.getSecondArg();
                    if (secondArg != null){
                        var method = varTable.get(((LiteralElement) secondArg).getLiteral());
                        // Constant pool bish were
//                        result.append(ident)
//                              .append(pushElementToStack(secondArg));
                    }

                    callInstruction.getListOfOperands().forEach(op ->
                        before.append(ident)
                               .append(pushElementToStack(op))
                    );
                }
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
                    result.append(pushElementToStack(returnInstruction.getOperand()));
                }
                result.append(Constants.returnInstr).append("\n");
            }
            case PUTFIELD -> {
                PutFieldInstruction putFieldInstruction = (PutFieldInstruction) instruction;
                result.append(Constants.loadObjectRefSM).append(0).append("\n") // Hardcoded since only fields from this class can be accessed
                      .append(pushElementToStack(putFieldInstruction.getThirdOperand()))
                      .append(Constants.putfield); // Constant pool bish were
            }
            case GETFIELD -> {
                GetFieldInstruction getFieldInstruction = (GetFieldInstruction) instruction;
                result.append(Constants.loadObjectRefSM).append(0).append("\n") // Hardcoded since only fields from this class can be accessed
                        .append(Constants.getfield); // Constant pool bish were
            }
            case UNARYOPER -> {
                // NOT SUPPORTED
            }
            case BINARYOPER -> {
                BinaryOpInstruction binaryOpInstruction = (BinaryOpInstruction) instruction;
                if (binaryOpInstruction.getUnaryOperation().getOpType().equals(OperationType.NOT) ||
                    binaryOpInstruction.getUnaryOperation().getOpType().equals(OperationType.NOTB)) {
                    result.append(pushElementToStack(binaryOpInstruction.getLeftOperand()))
                          .append(Constants.constant1B).append(1).append("\n")
                          .append(operationToString(binaryOpInstruction.getUnaryOperation()));

                }
                else {
                    result.append(pushElementToStack(binaryOpInstruction.getLeftOperand()))
                          .append(pushElementToStack(binaryOpInstruction.getRightOperand()))
                          .append(operationToString(binaryOpInstruction.getUnaryOperation()));
                }
            }
            case NOPER -> {
                SingleOpInstruction singleOpInstruction = (SingleOpInstruction) instruction;
                result.append(pushElementToStack(singleOpInstruction.getSingleOperand()));
            }
        }

        return before.append(result).toString();
    }

    private String operationToString(Operation operation) {
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

        if (element.getType().getTypeOfElement().equals(ElementType.INT32))
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
                        // ldc or ldc_w
                    }
                }
                else {
                    return loadLocalVar(varTable.get(operand.getName()).getVirtualReg()) + "\n";
                }

            }
            case BOOLEAN -> {
                return Constants.constant1B + (operand.getName().equals("false") ? 0 : 1) + "\n";
            }
            case ARRAYREF -> {
                // TODO
            }
            case OBJECTREF -> {
                var vreg = varTable.get(operand.getName()).getVirtualReg();
                return (vreg >= 0 && vreg <= 3 ? Constants.loadObjectRefSM : Constants.loadObjectRef) + vreg + "\n";
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

    private String loadLocalVar(int vreg){
        return (vreg >= 0 && vreg <= 3 ? Constants.loadLocalVarSM : Constants.loadLocalVar) + vreg + "\n";
    }

    private String fieldToString(Field field) {
        return ".field " + accessModifierToString(field.getFieldAccessModifier()) + " " + field.getFieldName() + " " + typeToString(field.getFieldType()) + "\n";
    }

    private String typeToString(Type type){
        switch (type.getTypeOfElement()){
            case INT32 -> {
                return "I";
            }
            case BOOLEAN -> {
                return "Z";
            }
            case ARRAYREF -> {
                return "[I";
            }
            case OBJECTREF -> {
                return "";
            }
            case CLASS -> {
                return "";
            }
            case THIS -> {
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

    private String accessModifierToString(AccessModifiers classAccessModifier) {
        return String.valueOf(classAccessModifier.equals(AccessModifiers.DEFAULT) ? AccessModifiers.PUBLIC : classAccessModifier).toLowerCase();
    }
}
