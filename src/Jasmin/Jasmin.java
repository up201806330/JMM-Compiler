import org.specs.comp.ollir.*;

import java.util.HashMap;
import java.util.stream.Collectors;

public class Jasmin {
    private final String indent = "  ";
    private HashMap<String, Descriptor> varTable;
    private ClassUnit classUnit;

//    Example of what you can do with the OLLIR class
//    ollirClass.checkMethodLabels(); // check the use of labels in the OLLIR loaded
//    ollirClass.buildCFGs(); // build the CFG of each method
//    ollirClass.outputCFGs(); // output to .dot files the CFGs, one per method
//    ollirClass.buildVarTables(); // build the table of variables for each method
//    ollirClass.show(); // print to console main information about the input OLLIR

    public String getByteCode(ClassUnit classUnit) throws OllirErrorException {
        this.classUnit = classUnit;

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
            .append(method.isConstructMethod() ? "<init>" : method.getMethodName())
            .append("(")
                .append(method.getParams().stream().map(x -> typeToString(x.getType())).collect(Collectors.joining()))
            .append(")")
            .append(typeToString(method.getReturnType())).append("\n");

            if (!method.isConstructMethod()){
                result.append(indent).append(".limit stack 99").append("\n")   // For checkpoint 2 is allowed
                      .append(indent).append(".limit locals 99").append("\n");
            }

            method.getInstructions().forEach(x -> result.append(instructionToString(x)));
            // method.getInstructions().forEach(Instruction::show);

        if (method.isConstructMethod())
            result.append(indent).append(Constants.returnVoidInstr).append("\n");
        result.append(".end method").append("\n\n");

        return result.toString();
    }

    private String instructionToString(Instruction instruction) {
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

                before.append(instructionToString(assignInstruction.getRhs()));
            }
            case CALL -> {
                CallInstruction callInstruction = (CallInstruction) instruction;
                Element caller = callInstruction.getFirstArg();
                String callerName = ((Operand)caller).getName();

                switch (callInstruction.getInvocationType()){
                    case invokevirtual, invokespecial -> {
                        before.append(indent)
                                .append(pushElementToStack(caller));
                    }
                    case invokeinterface -> {
                        // NOT SUPPORTED
                    }
                    case invokestatic -> {
                    }
                    case NEW -> {
                        result.append(((Operand) caller).getName().equals("array") ?
                                Constants.newArray :
                                Constants.newObj) // Constant pool bish were
                                .append("\n");
                    }
                    case arraylength -> {
                        before.append(indent)
                              .append(pushElementToStack(caller));
                        result.append(callInstruction.getInvocationType()).append("\n");
                    }
                    case ldc -> {
                    }
                }

                Element method = callInstruction.getSecondArg();
                if (method != null){
                     result.append(callInstruction.getInvocationType()).append(" ")
                           .append(invocationToString(caller, method));
                }

                var operands = callInstruction.getListOfOperands();
                if (operands != null){
                    operands.forEach(op ->
                            before.append(indent)
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
                    before.append(indent)
                          .append(pushElementToStack(returnInstruction.getOperand()));
                    result.append(Constants.returnInstr).append("\n");
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
                      .append(classUnit.getPackage() != null ? classUnit.getPackage() + "/" : "")
                      .append(classUnit.getClassName()).append("/")
                      .append(secondOperand.getName()).append(" ")
                      .append(typeToString(secondOperand.getType()))
                      .append("\n");
            }
            case GETFIELD -> {
                GetFieldInstruction getFieldInstruction = (GetFieldInstruction) instruction;
                Operand secondOperand = (Operand) getFieldInstruction.getSecondOperand();
                before.append(indent)
                      .append(Constants.loadObjectRefSM).append(0).append("\n"); // Hardcoded since only fields from this class can be accessed
                result.append(Constants.getfield)
                      .append(classUnit.getPackage() != null ? classUnit.getPackage() + "/" : "")
                      .append(classUnit.getClassName()).append("/")
                      .append(secondOperand.getName()).append(" ")
                      .append(typeToString(secondOperand.getType()))
                      .append("\n");
            }
            case UNARYOPER -> {
                // NOT SUPPORTED
            }
            case BINARYOPER -> {
                BinaryOpInstruction binaryOpInstruction = (BinaryOpInstruction) instruction;
                if (binaryOpInstruction.getUnaryOperation().getOpType().equals(OperationType.NOT) ||
                    binaryOpInstruction.getUnaryOperation().getOpType().equals(OperationType.NOTB)) {
                    before.append(indent)
                          .append(pushElementToStack(binaryOpInstruction.getLeftOperand()))
                          .append(indent)
                          .append(Constants.constant1B).append(1).append("\n");
                    result.append(operationToString(binaryOpInstruction.getUnaryOperation()));

                }
                else {
                    before.append(indent)
                          .append(pushElementToStack(binaryOpInstruction.getLeftOperand()))
                          .append(indent)
                          .append(pushElementToStack(binaryOpInstruction.getRightOperand()));
                    result.append(operationToString(binaryOpInstruction.getUnaryOperation()));
                }
            }
            case NOPER -> {
                SingleOpInstruction singleOpInstruction = (SingleOpInstruction) instruction;
                result.append(pushElementToStack(singleOpInstruction.getSingleOperand()));
            }
        }

        return before.append(result).toString();
    }

    private String invocationToString(Element caller, Element method) {
        try{
            LiteralElement literalMethod = (LiteralElement) method;
            if (literalMethod.getLiteral().equals("\"<init>\""))
                return "java/lang/Object/<init>()V\n";

        } catch (ClassCastException ignored){ }

        // TODO

        return "ERRR\n";
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
                        // ldc or ldc_w
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
