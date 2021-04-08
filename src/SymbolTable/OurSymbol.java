import pt.up.fe.comp.jmm.JmmNode;
import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.analysis.table.Type;

import java.util.Arrays;
import java.util.HashSet;

public class OurSymbol extends Symbol {
    private final HashSet<String> attributes;
    private final OurScope scope;
    private final Integer line;
    private final Integer column;

    public OurSymbol(JmmNode node, HashSet<String> attributes, OurScope scope) {
        super(new Type(
                        node.getOptional("type").orElse("void"),
                        Boolean.parseBoolean(node.getOptional("isArray").orElse("false"))
                ),
                node.get("name"));
        this.attributes = attributes;
        this.scope = scope;
        this.line = Integer.parseInt(node.get("line"));
        this.column = Integer.parseInt(node.get("column"));
    }

    public boolean isImport() { return attributes.contains("import"); }
    public boolean isClass() { return attributes.contains("class"); }
    public boolean isField() { return attributes.contains("field"); }
    public boolean isMethod() { return attributes.contains("method"); }
    public boolean isParameter() { return attributes.contains("parameter"); }
    public boolean isVariable() { return attributes.contains("variable"); }

    public String getAttributes(){
        StringBuilder result = new StringBuilder(getType().getName() + ((getType().isArray()) ? "[]" : ""));
        for(String attribute : attributes){
            result.append(", ").append(attribute);
        }
        return result.toString();
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
        if (!scope.equals(other.scope))
            return false;
        return true;
    }
}
