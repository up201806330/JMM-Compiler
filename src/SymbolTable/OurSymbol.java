import pt.up.fe.comp.jmm.JmmNode;
import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.analysis.table.Type;

import java.util.*;

public class OurSymbol extends Symbol implements Comparable<OurSymbol> {
    private final HashSet<String> attributes;
    private final List<Type> parameterTypes = new ArrayList<>();
    private final OurScope scope;
    private final Integer line;
    private final Integer column;
    private boolean initialized;

    public OurSymbol(JmmNode node, HashSet<String> attributes, OurScope scope) {
        super(new Type(
                        node.getOptional(Constants.typeAttribute).orElse(Constants.voidType),
                        Boolean.parseBoolean(node.getOptional(Constants.arrayAttribute).orElse("false"))
                ),
                node.get(Constants.nameAttribute));
        this.attributes = attributes;
        this.scope = scope;
        this.line = Integer.parseInt(node.get(Constants.lineAttribute));
        this.column = Integer.parseInt(node.get(Constants.columnAttribute));
    }

    public void insertParameterTypes(List<Type> parameterTypes){
        if (!attributes.contains(Constants.methodAttribute)) return;

        this.parameterTypes.addAll(parameterTypes);
    }

    public boolean isImport() { return attributes.contains(Constants.importAttribute); }
    public boolean isClass() { return attributes.contains(Constants.classAttribute); }
    public boolean isField() { return attributes.contains(Constants.fieldAttribute); }
    public boolean isMethod() { return attributes.contains(Constants.methodAttribute); }
    public boolean isParameter() { return attributes.contains(Constants.parameterAttribute); }
    public boolean isVariable() { return attributes.contains(Constants.variableAttribute); }
    public boolean isStatic() { return attributes.contains(Constants.staticAttribute); }

    public void setInitialized(){
        initialized = true;
    }

    public boolean isInitialized() {
        return initialized;
    }

    public String getAttributes(){
        StringBuilder result = new StringBuilder(getType().getName() + ((getType().isArray()) ? "[]" : ""));
        for(String attribute : attributes){
            result.append(", ").append(attribute);
        }
        return result.toString();
    }

    public List<Type> getParameterTypes() {
        return parameterTypes;
    }

    public static String parameterTypesToString(List<Type> parameterTypes){
        StringBuilder result = new StringBuilder();
        if (parameterTypes.size() > 0) result.append("(");
        boolean first = true;
        for(Type parameterType : parameterTypes){
            if (!first){
                result.append(", ");
            } else first = false;
            result.append(parameterType.getName()).append(parameterType.isArray() ? "[]" : "");
        }
        if (parameterTypes.size() > 0) result.append(")");
        return result.toString();
    }

    public Integer getNumParameters() {
        return parameterTypes.size();
    }

    public OurScope getScope(){
        return scope;
    }

    public Integer getLine() {
        return line;
    }

    public Integer getColumn() {
        return column;
    }

    @Override
    public String toString() {
        return getName() + parameterTypesToString(getParameterTypes());
    }

    @Override
    public int hashCode() {
        Type type = getType();
        return (int) getName().hashCode() *
                type.getName().hashCode() *
                (type.isArray() ? 1231 : 1237) * // boolean doesnt have .hashCode() :sadge:
                Arrays.hashCode(attributes.toArray()) *
                scope.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;

        OurSymbol other = (OurSymbol) obj;
        if (!getName().equals(other.getName()))
            return false;
        if (!(getType().getName().equals(other.getType().getName()) && getType().isArray() == other.getType().isArray()))
            return false;
        if (!getAttributes().equals(other.getAttributes()))
            return false;
        if (!parameterTypesToString(getParameterTypes()).equals(parameterTypesToString(other.getParameterTypes())))
            return false;
        if (!scope.equals(other.scope))
            return false;
        return true;
    }

    @Override
    public int compareTo(OurSymbol o) {
        int compareByLine = getLine().compareTo(o.getLine());
        if (compareByLine == 0) return getColumn().compareTo(o.getColumn());
        else return compareByLine;

    }
}
