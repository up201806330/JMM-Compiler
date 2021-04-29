import org.specs.comp.ollir.*;

import java.util.HashMap;

public class Jasmin {
    private final String ident = "  ";

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
        var varTable = OllirAccesser.getVarTable(method);

        result.append(".method ")
            .append(accessModifierToString(method.getMethodAccessModifier()))
            .append(method.isStaticMethod() ? " static " : " ")
            .append(method.isConstructMethod() ? "<init>" : method.getMethodName()).append("()")
            .append(typeToString(method.getReturnType())).append("\n");

            if (!method.isConstructMethod()){
                result.append(ident).append(".limit stack 99").append("\n")   // For checkpoint 2 is allowed
                      .append(ident).append(".limit locals 99").append("\n");
            }

//            method.getInstructions().forEach(x -> result.append(instructionToString(x, varTable)).append("\n"));
              method.getInstructions().forEach(Instruction::show);

        result.append(".end method").append("\n\n");

        return result.toString();
    }

    private String instructionToString(Instruction instruction, HashMap<String, Descriptor> varTable) {
        StringBuilder before = new StringBuilder();
        StringBuilder result = new StringBuilder();

        switch (instruction.getInstType()){
            case ASSIGN -> {
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
//                        before.append(ident)
//                              .append(Constants.loadLocalVar)
//                              .append(varTable.get(caller.getName()).getVirtualReg())
//                              .append("\n");
                        before.append(ident)
                              .append(pushElementToStack(caller, varTable))
                              .append("\n");
                    }

                    Element secondArg = callInstruction.getSecondArg();
                    if (secondArg != null){
                        before.append(ident)
                              .append(pushElementToStack(secondArg, varTable))
                              .append("\n");
                    }
                }
            }
            case GOTO -> {
            }
            case BRANCH -> {
            }
            case RETURN -> {
            }
            case PUTFIELD -> {
            }
            case GETFIELD -> {
            }
            case UNARYOPER -> {
            }
            case BINARYOPER -> {
            }
            case NOPER -> {
            }
        }

        return before.append(result).toString();
    }

    private String pushElementToStack(Element element, HashMap<String, Descriptor> varTable) {
        switch (element.getType().getTypeOfElement()){
            case INT32 -> {
                LiteralElement elem = (LiteralElement) element;
                if (Integer.parseInt(elem.getLiteral()) == -1) {
                    return Constants.constantMinus1;
                }
                else if (Integer.parseInt(elem.getLiteral()) >= 0 && Integer.parseInt(elem.getLiteral()) <= 5){
                    return Constants.constant1B + elem.getLiteral();
                }
                else if (Integer.parseInt(elem.getLiteral()) >= -128 && Integer.parseInt(elem.getLiteral()) <= 127){
                    return Constants.constant2B + elem.getLiteral();
                }
                else if (Integer.parseInt(elem.getLiteral()) >= -32768 && Integer.parseInt(elem.getLiteral()) <= 32767){
                    return Constants.constant3B + elem.getLiteral();
                }
                else {
                    // ldc or ldc_w
                }
            }
            case BOOLEAN -> {
            }
            case ARRAYREF -> {
            }
            case OBJECTREF -> {
            }
            case CLASS -> {
            }
            case THIS -> {
                return Constants.loadLocalVar + "0";
            }
            case STRING -> {
            }
            case VOID -> {
                System.out.println("Cant push void");
            }
        }
        return null;
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
