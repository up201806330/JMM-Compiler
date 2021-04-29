import org.specs.comp.ollir.*;

import java.awt.*;
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
            .append(typeToString(method.getReturnType())).append("\n")

                .append(ident).append(".limit stack 99").append("\n")   // For checkpoint 2 is allowed
                .append(ident).append(".limit locals 99").append("\n");

            method.getInstructions().forEach(x -> result.append(instructionToString(x, varTable)).append("\n"));
//              method.getInstructions().forEach(Instruction::show);

        result.append(".end method").append("\n\n");

        return result.toString();
    }

    private String instructionToString(Instruction instruction, HashMap<String, Descriptor> varTable) {
        StringBuilder before = new StringBuilder(ident);
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
                Operand caller = (Operand) callInstruction.getFirstArg();
                System.out.println(varTable.get(caller.getName()).getVirtualReg());
                if (callInstruction.getNumOperands() == 1) {

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

        before.append(result);
        return before.toString();
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
                return "";
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
