import pt.up.fe.comp.jmm.JmmNode;
import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp.jmm.report.Report;
import pt.up.fe.comp.jmm.report.ReportType;
import pt.up.fe.comp.jmm.report.Stage;

import java.util.*;

public class OurSymbolTable implements SymbolTable {

    // For every valuable node holds a  set of attributes or qualifiers i.e. Import, Class, ...
    HashMap<OurSymbol, JmmNode> table = new HashMap<>();

    public Optional<Report> put(OurSymbol symbol, JmmNode node) {
        // Check for repeat symbols
        var existingEntry = table.putIfAbsent(symbol, node);
        if (existingEntry != null)
            return Optional.of(
                    new Report(
                        ReportType.ERROR,
                        Stage.SYNTATIC,
                        symbol.getLine(),
                        symbol.getColumn(),
                            "Symbol " + symbol.getName() + " is already defined in the scope"));
        else return Optional.empty();
    }

    public OurSymbol getByValue(JmmNode node) {
        for (var entry : table.entrySet()) {
            if (entry.getValue().equals(node)) return entry.getKey();
        }
        return null;
    }

    @Override
    public List<String> getImports() {
        List<String> result = new ArrayList<>();

        for (OurSymbol entry : table.keySet()) {
            if (entry.isImport()) result.add(entry.getName());
        }
        return result;
    }

    @Override
    public String getClassName() {
        for (OurSymbol entry : table.keySet()) {
            if (entry.isClass()) return entry.getName();
        }
        return null;
    }

    @Override
    public String getSuper() {
        for (OurSymbol entry : table.keySet()) {
            if (entry.isSuper()) return entry.getName();
        }
        return null;
    }

    @Override
    public List<Symbol> getFields() {
        List<Symbol> result = new ArrayList<>();
        for (OurSymbol entry : table.keySet()) {
            if (entry.isField()) result.add(entry);
        }
        return result;
    }

    @Override
    public List<String> getMethods() {
        List<String> result = new ArrayList<>();
        for (OurSymbol entry : table.keySet()) {
            if (entry.isMethod()) result.add(entry.getName());
        }
        return result;
    }

    @Override
    public Type getReturnType(String methodName) {
        for (OurSymbol entry : table.keySet()) {
            if (entry.isReturn() && entry.getScope().getName().equals(methodName)) return entry.getType();
        }
        return new Type("void", false);
    }

    @Override
    public List<Symbol> getParameters(String methodName) {
        List<Symbol> result = new ArrayList<>();
        for (OurSymbol entry : table.keySet()) {
            if (entry.isParameter() && entry.getScope().getName().equals(methodName)) result.add(entry);
        }
        return result;
    }

    @Override
    public List<Symbol> getLocalVariables(String methodName) {
        List<Symbol> result = new ArrayList<>();
        for (OurSymbol entry : table.keySet()) {
            if (entry.isVariable() && entry.getScope().getName().equals(methodName)) result.add(entry);
        }
        return result;
    }

    @Override
    public String toString(){
        final Object[][] stringTable = new String[table.size() + 1][];
        StringBuilder result = new StringBuilder();
        stringTable[0] = new String[] {"| SYMBOL NAME ", " | TYPE ", " | SCOPE ", " |"};
        Iterator it = table.entrySet().iterator(); int i = 1;
        while(it.hasNext()){
            Map.Entry pair = (Map.Entry)it.next();
            OurSymbol symbol = (OurSymbol) pair.getKey();
            stringTable[i++] = new String[] { "| " + symbol.getName(), " | " + symbol.getAttributes(), " | " + symbol.getScope(), " |" };
        }
        for (final Object[] row : stringTable) {
            result.append(String.format("%-25s%-35s%-40s%-1s%n", row));
        }
        return result.toString();
    }
}
