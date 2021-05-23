import pt.up.fe.comp.jmm.JmmNode;
import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp.jmm.report.Report;
import pt.up.fe.comp.jmm.report.ReportType;
import pt.up.fe.comp.jmm.report.Stage;

import java.util.*;

public class OurSymbolTable implements SymbolTable {

    String className;
    String superName;
    SortedMap<OurSymbol, JmmNode> table = new TreeMap<>();

    public Optional<Report> put(OurSymbol symbol, JmmNode node) {
        // Check for repeat symbols inside scope (e.q. boolean a; int a; gives error)
        for (OurSymbol entry : table.keySet()) {
            if (!symbol.isMethod() &&
                    symbol.getName().equals(entry.getName()) &&
                    symbol.getScope().equals(entry.getScope()))
                return Optional.of(
                        new Report(
                                ReportType.WARNING,
                                Stage.SEMANTIC,
                                symbol.getLine(),
                                symbol.getColumn(),
                                "Variable '" + symbol.getName() + "' is already defined in the scope "));
        }

        // Check for repeat symbols
        var existingEntry = table.putIfAbsent(symbol, node);
        if (existingEntry != null)
            return Optional.of(
                    new Report(
                        ReportType.WARNING,
                        Stage.SEMANTIC,
                        symbol.getLine(),
                        symbol.getColumn(),
                            "Symbol '" + symbol.getName() + "' is already defined in the scope"));
        else return Optional.empty();
    }

    public OurSymbol getByValue(JmmNode node) {
        for (var entry : table.entrySet()) {
            if (entry.getValue().equals(node)) return entry.getKey();
        }
        return null;
    }

    public Optional<Type> tryGettingSymbolType(String methodName, String value){
        var symbolOpt = tryGettingSymbol(methodName, value);
        if (symbolOpt.isEmpty()) return Optional.empty();
        else return Optional.of(symbolOpt.get().getType());
    }

    public Optional<OurSymbol> tryGettingSymbol(String methodName, String value){
        if (value.equals(Consts.thisAttribute)){
            return tryGettingSymbol(Consts.thisAttribute, getClassName());
        }

        // If methodName is provided (!= 'this'), search for definition inside that method's scope
        if (!methodName.equals(Consts.thisAttribute)){
            for (OurSymbol entry : table.keySet()) {
                if (entry.getScope().getName().equals(methodName) &&
                        entry.getName().equals(value)) return Optional.of(entry);
            }
        }

        // If symbol isn't declared locally, search in the globals
        for (OurSymbol entry : table.keySet()) {
            if (entry.getScope().scope.equals(OurScope.ScopeEnum.Global) &&
                    entry.getName().equals(value)) return Optional.of(entry);
        }

        return Optional.empty();
    }

    public Optional<OurSymbol> getMethod(String methodName, List<Type> args, JmmNode node, List<Report> reports) {
        List<OurSymbol> methodOverloads = new ArrayList<>();
        for (OurSymbol entry : table.keySet()) {
            if (entry.isMethod() && entry.getName().equals(methodName) && entry.getParameterTypes().size() == args.size())
                methodOverloads.add(entry);
        }

        if (methodOverloads.size() == 0){
            reports.add(new Report(
                    ReportType.ERROR,
                    Stage.SEMANTIC,
                    Integer.parseInt(node.get("line")),
                    Integer.parseInt(node.get("column")),
                    "No definition of " + methodName +
                            " has " + args.size() +
                            " parameters "
            ));
            return Optional.empty();
        }

        for (OurSymbol methodOverload : methodOverloads){
            if (methodOverload.getParameterTypes().equals(args)) return Optional.of(methodOverload);
        }

        reports.add(new Report(
                ReportType.ERROR,
                Stage.SEMANTIC,
                Integer.parseInt(node.get("line")),
                Integer.parseInt(node.get("column")),
                "No definition of " + methodName +
                        " has parameter types " +  OurSymbol.parameterTypesToString(args)
        ));
        return Optional.empty();
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
        return className;
    }

    @Override
    public String getSuper() {
        return superName;
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
            if (entry.isMethod() && entry.getName().equals(methodName)) return entry.getType();
        }
        return null;
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
        final Object[][] stringTable = new String[table.size() + 2][];
        StringBuilder result = new StringBuilder();

        stringTable[0] = new String[] {className + (superName != null ? (" extends " + superName) : "") ,"", "", ""};
        stringTable[1] = new String[] {"| SYMBOL NAME ", " | TYPE ", " | SCOPE ", " |"};

        Iterator<Map.Entry<OurSymbol, JmmNode>> it = table.entrySet().iterator(); int i = 2;
        while(it.hasNext()){
            Map.Entry<OurSymbol, JmmNode> pair = it.next();
            OurSymbol symbol = pair.getKey();
            stringTable[i++] = new String[] { "| " + symbol.getName(), " | " + symbol.getAttributes(), " | " + symbol.getScope(), " |" };
        }
        for (final Object[] row : stringTable) {
            result.append(String.format("%-25s%-35s%-70s%-1s%n", row));
        }
        return result.toString();
    }
}
