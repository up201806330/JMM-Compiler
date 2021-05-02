

public class Constants {
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
    public static final String returnInt = "ireturn";
    public static final String returnObjectRef = "areturn";
    public static final String returnVoidInstr = "return";
    public static final String loadObjectRefSM = "aload_";
    public static final String loadObjectRef = "aload ";
    public static final String loadIntVarSM = "iload_";
    public static final String loadIntVar = "iload ";
    public static final String constantMinus1 = "iconst_m1";
    public static final String constant1B = "iconst_";
    public static final String constant2B = "bipush ";
    public static final String constant3B = "sipush ";
    public static final String constant4B = "ldc ";
    public static final String putfield = "putfield ";
    public static final String getfield = "getfield ";

    public static final String storeInt = "istore ";
    public static final String storeIntSM = "istore_";
    public static final String storeArrayElem = "iastore";
    public static final String storeArrayRef = "astore ";
    public static final String storeArrayRefSM = "astore_";
    public static final String storeObjRef = "astore ";
    public static final String storeObjRefSM = "astore_";

    public static final String newArray = "newarray int";
    public static final String newObj = "new ";

    public static final String addInt = "iadd";
    public static final String subInt = "isub";
    public static final String mulInt = "imul";
    public static final String divInt = "idiv";
    public static final String andInt = "iand";
    public static final String notInt = "ixor";

}
