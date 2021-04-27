import org.specs.comp.ollir.*;

import java.util.Locale;

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

        if (!classUnit.getSuperClass().equals(""))
            stringBuilder.append(".super ").append(classUnit.getSuperClass()).append("\n\n");

        classUnit.getMethods().forEach(x -> stringBuilder.append(methodToString(x)));

        return stringBuilder.toString();
    }

    private String accessModifierToString(AccessModifiers classAccessModifier) {
        return String.valueOf(classAccessModifier.equals(AccessModifiers.DEFAULT) ? AccessModifiers.PUBLIC : classAccessModifier).toLowerCase();
    }

    public String methodToString(Method method){
        StringBuilder result = new StringBuilder();
        result.append(".method ")
            .append(accessModifierToString(method.getMethodAccessModifier()))
            .append(method.isStaticMethod() ? " static " : " ")
            .append(method.isConstructMethod() ? "<init>" : method.getMethodName()).append("()")
            .append(typeToString(method.getReturnType())).append("\n")

                .append(ident).append(".limit stack 99").append("\n")   // For checkpoint 2 is allowed
                .append(ident).append(".limit locals 99").append("\n");

            method.getInstructions().forEach(x -> result.append(ident).append(x).append("\n"));

        result.append(".end method").append("\n\n");

        return result.toString();
    }

    String typeToString(Type type){
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


}
