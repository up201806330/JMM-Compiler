## GROUP:7a


| Name | Number | Self Assessment | Contribution |
| ---: | -----: | --------------: | -----------: |
| Diogo Miguel Ferreira Rodrigues | 201806429 | 20 | 25% |
| João António Cardoso Vieira e Basto de Sousa | 201806613 | 20 | 25% |
| Rafael Soares Ribeiro | 201806630 | 20 | 25% |
| Xavier Ruivo Pisco | 201806134 | 20 | 25% |

### GLOBAL Grade of the project: 20

### SUMMARY:
A *Java--* mini-compiler that handles generation of *.class* files compatible with *Java*.
Parses text files using *javacc* and the *Jmm grammar*, handles semantic and syntactic errors and performs some optimizations at the machine code level

### DEALING WITH SYNTACTIC ERRORS:

When the compiler encounters a syntatic error, at the level of the while expressions. That is, if an error is found,
a report is generated, indicating the line where it occurred, and the parser proceeds.
This can be seen in the `CompleteWhileTest.jmm` file

### SEMANTIC ANALYSIS:
All the semantic rules implemented follow below:

#### Symbol Table
- global: inclui info de imports e a classe declarada
- classe-specific: inclui info de extends, fields e methods
- method-specific: inclui info dos arguments e local variables
- retorno do SemanticsReport
    - permite consulta da tabela por parte da análise semantica
    - permite impressão para fins de debug
- small bonus: permitir method overload (i.e. métodos com mesmo nome mas assinatura de parâmetros diferente)

#### Expression Analysis
**Type Verification**
- verificar se operações são efetuadas com o mesmo tipo (e.g. int + boolean tem de dar erro)
- não é possível utilizar arrays diretamente para operações aritmeticas (e.g. array1 + array2) 
- verificar se um array access é de facto feito sobre um array (e.g. 1[10] não é permitido)
- verificar se o indice do array access é um inteiro (e.g. a[true] não é permitido)
- verificar se valor do assignee é igual ao do assigned (a_int = b_boolean não é permitido!)
- verificar se operação booleana (&&, < ou !) é efetuada só com booleanos
- verificar se conditional expressions (if e while) resulta num booleano

**Method Verification**
- verificar se o "target" do método existe, e se este contém o método (e.g. a.foo, ver se 'a' existe e se tem um método 'foo')
  - caso seja do tipo da classe declarada (e.g. a usar o this), se não existir declaração na própria classe: se não tiver extends retorna erro, se tiver extends assumir que é da classe super
  - caso o método não seja da classe declarada, isto é uma classe importada, assumir como existente e assumir tipos esperados. (e.g. a = Foo.b(), se a é um inteiro, e Foo é uma classe importada, assumir que o método b é estático (pois estamos a aceder a uma método diretamente da classe), que não tem argumentos e que retorna um inteiro)
- verificar se o número de argumentos na invocação é igual ao número de parâmetros da declaração
- verificar se o tipo dos parâmetros coincide com o tipo dos argumentos

**EXTRAS**
- Variable already defined in the scope
- Static function declaration and verification in invocation
- Function overloading and verification
- Return type mismatch
- Variables must be declared before used
- Variables must be initialized before used
- Array declare size must be int
- this cannot be used in a static context
- since all fields are non static, cannot access fields in static functions
- Variable type is undefined
- Cannot access property of primitive type

#### CODE GENERATION:
*Jasmin* is used to generate JVM Bytecodes from the OLLIR (generated previously from the AST).
In this phase, some optimzations were implemented (some executed after the AST generation and some after the OLLIR generation), as follows:

**Optimizations**
- Better while template
- If and while dead code removal (evaluates conditions and decides if there are dead blocks of code, be it a dead else statement in a `if (0 < 10)` condition, or an entire dead while loop in a `while (false)` condition)
- iinc optimization (`x = x + 2;`)
- In constant propagation, remove dead assignments (`int b = 10;` is propagated everytime it is called, so the assignment can be removed after constant propagation)
- Constant folding on all expressions
- division and multiplication by powers of 2 replaced with shifts (`x / 2 <=> x >> 1` and `x * 8 <=> x << 3`)
- use `if_lt` and `if_gt` in comparisons with 0 (`x < 0` and `0 < x`)

#### TASK DISTRIBUTION:
##### Checkpoint 1
1. Develop a parser for Java-- using JavaCC and taking as starting point the Java-- grammar furnished (note that the original grammar may originate conflicts when implemented with parsers of LL(1)type and in that case you need to modify the grammar in order to solve those conflicts); **Rafael e Xavier**
2. Include error treatment and recovery mechanisms for while conditions; **Diogo**
3. Proceed with the specification of the file jjt to generate, using JJTree, a new version of the parser including in this case the generation of the syntax tree (the generated tree should be an AST), annotating the nodes and leafs of the tree with the information (including tokens) necessary to perform the subsequent compiler steps; **João e Xavier**
##### Checkpoint 2
4. Implement the interfaces that will allow the generation of the JSON files representing the source code and the necessary symbol tables; **João**
5. Implement the Semantic Analysis and generate the LLIR code, OLLIR, from the AST; **Diogo, João, Rafael e Xavier**
6. Generate from the OLLIR the JVM code accepted by jasmin corresponding to the invocation of functions in Java--; **Diogo, João, Rafael e Xavier**
7. Generate from the OLLIR JVM code accepted by jasmin for arithmetic expressions; **Diogo, João, Rafael e Xavier**
##### Checkpoint 3
8. Generate from the OLLIR JVM code accepted by jasmin for conditional instructions (if and if-else); **João**
9. Generate from the OLLIR JVM code accepted by jasmin for loops; **João**
10. Generate from the OLLIR JVM code accepted by jasmin to deal with arrays. **Diogo**
11. Complete the compiler and test it using a set of Java-- classes; **Rafael e Xavier**
##### Final delivery
12. Proceed with the optimizations related to the code generation, related to the register allocation (“-r” option) and the optimizations related to the “-o” option **Diogo, Rafael e Xavier**

#### PROS: 
Does thorough analysis on every stage and takes advantage of clever tricks to save instructions (e.g. `boolean x = 10 < 11;` is done with by checking the 2's complement sign bit of the difference of the two values, so there's no need for a jump)

#### CONS:
We couldn't do as many optimizations in the code generation as we wanted.