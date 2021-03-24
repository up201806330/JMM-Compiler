import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.analysis.table.Type;

import java.util.*;

public class OurSymbolTable implements SymbolTable { // TODO decide if we want global or hierarchical
    // For every valuable node holds a set of attributes or qualifiers i.e. Import, Class, ...
    // TODO Missing scope
    Set<OurSymbol> table = new HashSet<>();

    public void put(OurSymbol symbol) { table.add(symbol); }

    @Override
    public List<String> getImports() {
        List<String> result = new ArrayList<>();
        for (OurSymbol entry : table) {
            if (entry.isImport()) result.add(entry.getName());
        }
        return result;
    }

    @Override
    public String getClassName() {
        for (OurSymbol entry : table) {
            if (entry.isClass()) return entry.getName();
        }
        return null;
    }

    @Override
    public String getSuper() {
        for (OurSymbol entry : table) {
            if (entry.isSuper()) return entry.getName();
        }
        return null;
    }

    @Override
    public List<Symbol> getFields() {
        List<Symbol> result = new ArrayList<>();
        for (OurSymbol entry : table) {
            if (entry.isField()) result.add(entry);
        }
        return result;
    }

    @Override
    public List<String> getMethods() {
        List<String> result = new ArrayList<>();
        for (OurSymbol entry : table) {
            if (entry.isMethod()) result.add(entry.getName());
        }
        return result;
    }

    @Override
    public Type getReturnType(String methodName) {
        for (OurSymbol entry : table) {
            if (entry.isReturn()) return entry.getType();
        }
        return null;
    }

    @Override
    public List<Symbol> getParameters(String methodName) {
        List<Symbol> result = new ArrayList<>();
        for (OurSymbol entry : table) {
            if (entry.isParameter()) result.add(entry);
        }
        return result;
    }

    @Override
    public List<Symbol> getLocalVariables(String methodName) {
        List<Symbol> result = new ArrayList<>();
        for (OurSymbol entry : table) {
            if (entry.isVariable()) result.add(entry);
        }
        return result;
    }
}
