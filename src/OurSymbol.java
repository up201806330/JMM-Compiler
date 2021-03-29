import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.analysis.table.Type;

import java.util.HashSet;

public class OurSymbol extends Symbol {
    private final HashSet<String> attributes;

    public OurSymbol(Type type, String name, HashSet<String> attributes) {
        super(type, name);
        this.attributes = attributes;
    }

    public HashSet<String> getAttributes() { return attributes; }

    public boolean isImport() { return attributes.contains("import"); }
    public boolean isClass() { return attributes.contains("class"); }
    public boolean isSuper() { return attributes.contains("extends"); }
    public boolean isField() { return attributes.contains("field"); }
    public boolean isMethod() { return attributes.contains("method"); }
    public boolean isReturn() { return attributes.contains("return"); }
    public boolean isParameter() { return attributes.contains("parameter"); }
    public boolean isVariable() { return attributes.contains("variable"); }

    @Override
    public String toString(){
        StringBuilder result = new StringBuilder(getType().getName() + ((getType().isArray()) ? "[]" : ""));
        for(String attribute : attributes){
            result.append(", ").append(attribute);
        }
        return result.toString();
    }

}
