import java.util.Objects;

public class OurScope {
    public enum ScopeEnum {
        Global,
        FunctionParameter,
        FunctionVariable
    }

    ScopeEnum scope;
    OurSymbol functionSymbol;

    public OurScope() {
        this.functionSymbol = null;
        this.scope = ScopeEnum.Global;
    }

    public OurScope(ScopeEnum scope, OurSymbol functionSymbol){
        this.functionSymbol = functionSymbol;
        this.scope = (scope != null && functionSymbol != null) ? scope : ScopeEnum.Global;
    }

    public String getName(){
        return functionSymbol.getName();
    }

    @Override
    public String toString(){
        return scope.toString() + (scope.ordinal() > 0 ? " (" + functionSymbol.getName() + ")" : "");
    }

    @Override
    public int hashCode() {
        return scope.hashCode() *
                (functionSymbol != null ? functionSymbol.hashCode() : 1);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;

        OurScope other = (OurScope) obj;
        if (!scope.equals(other.scope))
            return false;
        return Objects.equals(functionSymbol, other.functionSymbol);
    }
}
