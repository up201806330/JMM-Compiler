

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
    public static final String varDeclNodeName = "VarDeclaration";
    public static final String classInheritNodeName = "ClassInheritance";
    public static final String ifConditionNodeName = "IfCondition";
    public static final String ifStatementNodeName = "IfStatement";
    public static final String elseStatementNodeName = "Else";
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
    public static final String identifierAttribute = "identifier";
    public static final String intArrayType = "intArray";
    public static final String intType = "int";
    public static final String booleanType = "boolean";
    public static final String voidType = "void";
    public static final String autoType = "auto"; // Special type that is assigned to method calls where type is assumed to be correct
    public static final String mainMethod = "main";

    public static final String andExpression = "&&";
    public static final String lessThanExpression = "<";

    // Symbol attributes
    public static final String importAttribute = "import";
    public static final String variableAttribute = "variable";
    public static final String methodAttribute = "method";
    public static final String parameterAttribute = "parameter";
    public static final String fieldAttribute = "field";
    public static final String classAttribute = "class";
}
