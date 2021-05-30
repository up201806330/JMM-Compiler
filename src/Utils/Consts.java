import java.util.HashMap;

public class Consts {
    public static final String error = "error";

    // Node names
    public static final String typeNodeName = "Type";
    public final static String terminalNodeName = "Terminal";
    public final static String propertyAccessNodeName = "PropertyAccess";
    public static final String newNodeName = "New";
    public static final String literalNodeName = "Literal";
    public static final String binaryNodeName = "Binary";
    public static final String methodParamNodeName = "Parameter";
    public static final String arrayExprNodeName = "ArrayExpression";
    public static final String notExprNodeName = "NotExpression";
    public static final String callExprNodeName = "CallExpression";
    public static final String assignmentNodeName = "Assignment";
    public static final String methodDeclNodeName = "MethodDeclaration";
    public static final String classDeclNodeName = "ClassDeclaration";
    public static final String importDeclNodeName = "ImportDeclaration";
    public static final String fieldDeclNodeName = "Field";
    public static final String varDeclNodeName = "VarDeclaration";
    public static final String classInheritNodeName = "ClassInheritance";
    public static final String ifConditionNodeName = "IfCondition";
    public static final String ifStatementNodeName = "IfStatement";
    public static final String elseStatementNodeName = "Else";
    public static final String whileStatementNodeName = "WhileStatement";
    public static final String whileConditionNodeName = "WhileCondition";
    public static final String returnNodeName = "Return";

    // Attribute names
    public static final String nameAttribute = "name";
    public static final String typeAttribute = "type";
    public static final String valueAttribute = "value";
    public static final String lineAttribute = "line";
    public static final String columnAttribute = "column";
    public static final String arrayAttribute = "isArray";
    public static final String thisAttribute = "this";
    public static final String staticAttribute = "static";


    // Types
    public static final boolean isPrimitiveType(String type) {
        return type.equals("int") || type.equals("boolean") || type.equals("intArray");
    }
    public static final String identifierAttribute = "identifier";
    public static final String intArrayType = "intArray";
    public static final String stringArrayType = "StringArray";
    public static final String intType = "int";
    public static final String stringType = "String";
    public static final String booleanType = "boolean";
    public static final String voidType = "void";
    public static final String autoType = "auto"; // Special type that is assigned to method calls where type is assumed to be correct
    public static final String mainMethod = "main";
    public static final String lengthProperty = "length";

    public static final String andExpression = "&&";
    public static final String lessThanExpression = "<";

    // Symbol attributes
    public static final String importAttribute = "import";
    public static final String variableAttribute = "variable";
    public static final String methodAttribute = "method";
    public static final String parameterAttribute = "parameter";
    public static final String fieldAttribute = "field";
    public static final String classAttribute = "class";
    public static final String superAttribute = "super";

    // Jasmin instructions
    public static final String returnInt = "ireturn ";
    public static final String returnObjectRef = "areturn ";
    public static final String returnVoidInstr = "return ";
    public static final String loadObjectRefSM = "aload_";
    public static final String loadObjectRef = "aload ";
    public static final String loadIntVarSM = "iload_";
    public static final String loadIntVar = "iload ";
    public static final String loadArrayElem = "iaload ";
    public static final String constantMinus1 = "iconst_m1 ";
    public static final String constant1B = "iconst_";
    public static final String constant2B = "bipush ";
    public static final String constant3B = "sipush ";
    public static final String constant4B = "ldc ";
    public static final String putfield = "putfield ";
    public static final String getfield = "getfield ";
    public static final String gotoLabel = "goto ";

    public static final String storeInt = "istore ";
    public static final String storeIntSM = "istore_";
    public static final String storeArrayElem = "iastore ";
    public static final String storeObjRef = "astore ";
    public static final String storeObjRefSM = "astore_";

    public static final String newArray = "newarray ";
    public static final String newObj = "new ";
    public static final String arrayLength = "arraylength ";
    public static final String invokeSpecial = "invokespecial ";
    public static final String invokeVirtual = "invokevirtual ";
    public static final String invokeStatic = "invokestatic ";

    public static final String addInt = "iadd ";
    public static final String subInt = "isub ";
    public static final String mulInt = "imul ";
    public static final String divInt = "idiv ";
    public static final String andInt = "iand ";
    public static final String notInt = "ixor ";
    public static final String unsignedShiftRight = "iushr ";
    public static final String negateInt = "ineg ";
    public static final String compLessThan = "if_icmplt ";
    public static final String compFalse = "ifeq ";
    public static final String compTrue = "ifne ";
    public static final String compLessThanZero = "iflt ";
    public static final String compGreaterThanZero = "ifgt ";
    public static final String incrementInt = "iinc ";
    public static final String shiftRight = "ishr ";
    public static final String shiftLeft = "ishl ";
    public static final String pop = "pop ";

    public static final HashMap<String, Integer> instructionVals = new HashMap<>(){{
        put(returnInt.trim(), -1);
        put(returnObjectRef.trim(), -1);
        put(returnVoidInstr.trim(), 0);
        put(loadObjectRefSM.trim(), 1);
        put(loadObjectRef.trim(), 1);
        put(loadIntVarSM.trim(), 1);
        put(loadIntVar.trim(), 1);
        put(loadArrayElem.trim(), -1);
        put(constantMinus1.trim(), 1);
        put(constant1B.trim(), 1);
        put(constant2B.trim(), 1);
        put(constant3B.trim(), 1);
        put(constant4B.trim(), 1);
        put(putfield.trim(), -2);
        put(getfield.trim(), 0);
        put(gotoLabel.trim(), 0);
        put(storeInt.trim(), -1);
        put(storeIntSM.trim(), -1);
        put(storeArrayElem.trim(), -3);
        put(storeObjRef.trim(), -1);
        put(storeObjRefSM.trim(), -1);
        put(newArray.trim(), 0);
        put(arrayLength.trim(), 0);
        put(newObj.trim(), 1);
        put(addInt.trim(), -1);
        put(subInt.trim(), -1);
        put(mulInt.trim(), -1);
        put(divInt.trim(), -1);
        put(andInt.trim(), -1);
        put(notInt.trim(), -1);
        put(unsignedShiftRight.trim(), -1);
        put(negateInt.trim(), 0);
        put(compLessThan.trim(), -2);
        put(compLessThanZero.trim(), -1);
        put(compGreaterThanZero.trim(), -1);
        put(compFalse.trim(), -1);
        put(compTrue.trim(), -1);
        put(incrementInt.trim(), 0);
        put(shiftRight.trim(), -1);
        put(shiftLeft.trim(), -1);
        put(invokeSpecial.trim(), -1);
        put(invokeVirtual.trim(), -1);
        put(invokeStatic.trim(), 0);
        put(pop.trim(), -1);
    }};

}
