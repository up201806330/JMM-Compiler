import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.analysis.table.Type;

import java.util.*;

public class OurScopedSymbolTable implements SymbolTable {
    String scopeName;
    OurScopedSymbolTable parent;

    // For every valuable node holds a set of attributes or qualifiers i.e. Import, Class, ...
    HashMap<String, OurSymbol> table = new HashMap<>();

    OurScopedSymbolTable(OurScopedSymbolTable parent, String scopeName){
        this.parent = parent;
        this.scopeName = scopeName;
    }

    public void put(OurSymbol symbol) { table.put(symbol.getName(), symbol); }

    @Override
    public List<String> getImports() {
        List<String> result = new ArrayList<>();

        if (parent != null) result.addAll(parent.getImports());

        for (OurSymbol entry : table.values()) {
            if (entry.isImport()) result.add(entry.getName());
        }
        return result;
    }

    @Override
    public String getClassName() {
        for (OurSymbol entry : table.values()) {
            if (entry.isClass()) return entry.getName();
        }
        return null;
    }

    @Override
    public String getSuper() {
        for (OurSymbol entry : table.values()) {
            if (entry.isSuper()) return entry.getName();
        }
        return null;
    }

    @Override
    public List<Symbol> getFields() {
        List<Symbol> result = new ArrayList<>();
        for (OurSymbol entry : table.values()) {
            if (entry.isField()) result.add(entry);
        }
        return result;
    }

    @Override
    public List<String> getMethods() {
        List<String> result = new ArrayList<>();
        for (OurSymbol entry : table.values()) {
            if (entry.isMethod()) result.add(entry.getName());
        }
        return result;
    }

    @Override
    public Type getReturnType(String methodName) {
        for (OurSymbol entry : table.values()) {
            if (entry.isReturn()) return entry.getType();
        }
        return new Type("void", false);
    }

    @Override
    public List<Symbol> getParameters(String methodName) {
        List<Symbol> result = new ArrayList<>();
        for (OurSymbol entry : table.values()) {
            if (entry.isParameter()) result.add(entry);
        }
        return result;
    }

    @Override
    public List<Symbol> getLocalVariables(String methodName) {
        List<Symbol> result = new ArrayList<>();
        for (OurSymbol entry : table.values()) {
            if (entry.isVariable()) result.add(entry);
        }
        return result;
    }

    @Override
    public String toString(){
        final Object[][] stringTable = new String[table.size() + 1][];
        StringBuilder result = new StringBuilder();
        stringTable[0] = new String[] {"| SYMBOL NAME", "| TYPE |" /*Missing Scope Here*/};
        Iterator it = table.entrySet().iterator(); int i = 1;
        while(it.hasNext()){
            Map.Entry pair = (Map.Entry)it.next();
            stringTable[i++] = new String[] { "| " + pair.getKey().toString(), "| " + pair.getValue().toString() + " |" /*Missing Scope Here*/ };
        }
        for (final Object[] row : stringTable) {
            // result.format("%-15s%-15s%-15s%n", row); Change to this after adding scope
            result.append(String.format("%-15s%-15s%n", row));
        }
        return result.toString();
    }
}
