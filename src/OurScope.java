public class OurScope {
    public enum ScopeEnum {
        Global,
        FunctionParameter,
        FunctionVariable
    }

    ScopeEnum scope;
    OurSymbol functionSymbol;

    public OurScope(ScopeEnum scope, OurSymbol functionSymbol){
        this.scope = scope;
        this.functionSymbol = functionSymbol;
    }

    @Override
    public String toString(){
        return scope.toString() + (scope.ordinal() > 0 ? " (" + functionSymbol.getName() + ")" : "");
    }
}
