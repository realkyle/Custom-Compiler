# Compiler Project Handoff

## What This Is

A handmade compiler written in Java, built from scratch with no parser-generator libraries (no ANTLR, no JavaCC). The goal is a fully working end-to-end pipeline that a developer can learn from and extend.

---

## Design Decisions Made

### Language Being Compiled: "Tine"

We designed a small custom language called **Tine** — a statically typed, C-like scripting language. It was designed to be:
- Simple enough to implement in a weekend
- Complex enough to be interesting (functions, loops, types)
- Familiar enough that the user doesn't have to learn weird syntax

**Example Tine program:**

```
fn add(int a, int b) -> int {
    return a + b;
}

fn main() -> void {
    int x = 10;
    int y = 20;
    int result = add(x, y);
    print(result);

    int i = 0;
    while (i < 5) {
        print(i);
        i = i + 1;
    }

    if (result > 25) {
        print(1);
    } else {
        print(0);
    }
}
```

**Supported features:**
- Types: `int`, `bool`, `void`
- Variable declarations: `int x = 5;`
- Arithmetic: `+`, `-`, `*`, `/`
- Comparison: `<`, `>`, `==`, `!=`, `<=`, `>=`
- Logic: `&&`, `||`, `!`
- Control flow: `if/else`, `while`
- Functions with parameters and return types
- Built-in: `print(expr)`
- Single-line comments: `// ...`

**Not supported (intentionally out of scope):**
- Strings
- Arrays
- Closures / first-class functions
- Floats

### Output Target: Tree-Walking Interpreter

The compiler **does not emit machine code or JVM bytecode**. Instead it evaluates the AST directly via a tree-walking interpreter. This was chosen because:
- It gets a working end-to-end pipeline fastest
- All the hard compiler phases (lexer, parser, type checker) are still fully implemented
- The user can later swap the interpreter for a code generator (bytecode, LLVM IR, etc.) without changing earlier phases

The pipeline is:

```
source.tine
    → Lexer         → List<Token>
    → Parser        → Expr/Stmt AST nodes
    → TypeChecker   → (annotated AST, or type errors)
    → Interpreter   → output printed to stdout
```

### Size Estimates

| Component       | Est. Lines | Notes                                  |
|-----------------|-----------|----------------------------------------|
| TokenType enum  | ~60       | ~30 token types                        |
| Lexer           | ~150      | Handles keywords, idents, numbers, ops |
| AST nodes       | ~120      | ~15 node types using sealed interfaces |
| Parser          | ~300      | Recursive descent, precedence climbing |
| Type Checker    | ~200      | Symbol table, scoping, type inference  |
| Interpreter     | ~200      | Tree-walking eval                      |
| Main / Driver   | ~30       | Reads file, runs pipeline              |
| **Total**       | **~1060** |                                        |

### Token Types (~30 total)

```
// Literals
NUMBER, BOOL_TRUE, BOOL_FALSE, IDENTIFIER

// Arithmetic
PLUS, MINUS, STAR, SLASH

// Comparison
LT, GT, LT_EQ, GT_EQ, EQ_EQ, BANG_EQ

// Logic
AMP_AMP, PIPE_PIPE, BANG

// Assignment & punctuation
EQ, SEMICOLON, COMMA, LPAREN, RPAREN, LBRACE, RBRACE, ARROW

// Keywords
FN, RETURN, IF, ELSE, WHILE, INT, BOOL, VOID, PRINT

// Meta
EOF
```

### Grammar Rules (simplified BNF, ~15 rules)

```
program     → fn_decl*
fn_decl     → "fn" IDENT "(" params? ")" "->" type block
params      → param ("," param)*
param       → type IDENT
block       → "{" stmt* "}"
stmt        → var_decl | assign | return_stmt | if_stmt | while_stmt | print_stmt | expr_stmt
var_decl    → type IDENT "=" expr ";"
assign      → IDENT "=" expr ";"
return_stmt → "return" expr ";"
if_stmt     → "if" "(" expr ")" block ("else" block)?
while_stmt  → "while" "(" expr ")" block
print_stmt  → "print" "(" expr ")" ";"
expr_stmt   → expr ";"
expr        → logic_or
logic_or    → logic_and ("||" logic_and)*
logic_and   → equality ("&&" equality)*
equality    → comparison (("==" | "!=") comparison)*
comparison  → addition (("<" | ">" | "<=" | ">=") addition)*
addition    → multiplication (("+" | "-") multiplication)*
multiplication → unary (("*" | "/") unary)*
unary       → "!" unary | primary
primary     → NUMBER | "true" | "false" | IDENT | call | "(" expr ")"
call        → IDENT "(" args? ")"
args        → expr ("," expr)*
type        → "int" | "bool" | "void"
```

---

## What Was Done in the Previous Session

The previous session was a **design and planning session only**. No code was written. The following was established:

1. Explained the full compiler pipeline (Lexer → Parser → Semantic Analysis → Code Gen)
2. Provided Java code sketches for a basic Lexer, recursive descent Parser, and tree-walking Interpreter for a simple arithmetic expression language
3. Made the full design decisions documented above (language name, feature set, output target, size estimates)

**Nothing was written to disk. No Java files exist yet.**

---

## What the Next Agent Should Do

### Step 1 — Project Structure

Create this directory layout:

```
tine/
├── src/
│   └── main/java/tine/
│       ├── Main.java
│       ├── lexer/
│       │   ├── TokenType.java
│       │   ├── Token.java
│       │   └── Lexer.java
│       ├── parser/
│       │   ├── ast/          ← all AST node types go here
│       │   └── Parser.java
│       ├── analysis/
│       │   └── TypeChecker.java
│       └── runtime/
│           └── Interpreter.java
└── examples/
    └── hello.tine
```

Use a plain Maven or Gradle project, or just `javac` — no frameworks needed.

### Step 2 — Build Order (do these in order, test each before moving on)

**Phase 1: Lexer**
- Implement `TokenType` enum with all ~30 types listed above
- Implement `Token` as a record: `record Token(TokenType type, String value, int line)`
- Implement `Lexer`: scan char-by-char, produce `List<Token>`
- Test: tokenize `fn main() -> void { print(1 + 2); }` and print tokens

**Phase 2: AST Nodes**
- Use Java `sealed interface` + `record` for each node type
- Two hierarchies: `Expr` (expressions) and `Stmt` (statements)
- Also a `FnDecl` top-level node

Suggested node types:
```java
// Expressions
sealed interface Expr permits NumberExpr, BoolExpr, VarExpr, BinaryExpr, UnaryExpr, CallExpr {}
record NumberExpr(int value) implements Expr {}
record BoolExpr(boolean value) implements Expr {}
record VarExpr(String name) implements Expr {}
record BinaryExpr(Expr left, String op, Expr right) implements Expr {}
record UnaryExpr(String op, Expr operand) implements Expr {}
record CallExpr(String name, List<Expr> args) implements Expr {}

// Statements
sealed interface Stmt permits VarDeclStmt, AssignStmt, ReturnStmt, IfStmt, WhileStmt, PrintStmt, ExprStmt, BlockStmt {}
record VarDeclStmt(String type, String name, Expr init) implements Stmt {}
record AssignStmt(String name, Expr value) implements Stmt {}
record ReturnStmt(Expr value) implements Stmt {}
record IfStmt(Expr condition, BlockStmt then, BlockStmt else_) implements Stmt {}
record WhileStmt(Expr condition, BlockStmt body) implements Stmt {}
record PrintStmt(Expr value) implements Stmt {}
record ExprStmt(Expr expr) implements Stmt {}
record BlockStmt(List<Stmt> stmts) implements Stmt {}

// Top-level
record Param(String type, String name) {}
record FnDecl(String name, List<Param> params, String returnType, BlockStmt body) {}
```

**Phase 3: Parser**
- Recursive descent, one method per grammar rule
- `parseProgram()` → `List<FnDecl>`
- Precedence climbing for expressions (follow the grammar rules in order: or → and → equality → comparison → addition → multiplication → unary → primary)
- Throw a `ParseException` with line number on syntax errors

**Phase 4: Type Checker**
- Maintain a `Map<String, String>` symbol table for variable types
- Maintain a `Map<String, FnDecl>` for function signatures
- Walk every node, verify types match
- Key rules to enforce:
  - Both sides of arithmetic must be `int`
  - Both sides of `&&`/`||` must be `bool`
  - `if`/`while` conditions must be `bool`
  - Return type of function must match declared `-> type`
  - Function call arg types must match param types
- Throw `TypeException` with helpful message on violations

**Phase 5: Interpreter**
- Maintain an `Environment` class: a stack of `Map<String, Object>` scopes
- Walk the AST, evaluate expressions to `Object` (use `Integer` for int, `Boolean` for bool)
- `evalExpr(Expr e, Environment env)` → `Object`
- `execStmt(Stmt s, Environment env)` → `void` (or a special `ReturnValue` wrapper for return)
- Handle function calls by creating a new scope, binding args, executing body
- `print(value)` → `System.out.println(value)`

**Phase 6: Main Driver**
```java
public class Main {
    public static void main(String[] args) throws IOException {
        String source = Files.readString(Path.of(args[0]));
        List<Token> tokens = new Lexer(source).tokenize();
        List<FnDecl> program = new Parser(tokens).parseProgram();
        new TypeChecker(program).check();
        new Interpreter(program).run();  // calls main()
    }
}
```

### Step 3 — Example Test File

Write `examples/hello.tine`:
```
fn factorial(int n) -> int {
    if (n <= 1) {
        return 1;
    } else {
        return n * factorial(n - 1);
    }
}

fn main() -> void {
    int i = 1;
    while (i <= 6) {
        print(factorial(i));
        i = i + 1;
    }
}
```

Expected output: `1`, `2`, `6`, `24`, `120`, `720` (one per line)

---

## Known Design Gotchas to Watch For

- **Return in the interpreter:** Use a sentinel exception or a `ReturnValue` wrapper class to unwind the call stack cleanly. Don't use a boolean flag.
- **Scoping for functions:** Each function call needs its own fresh scope. Don't share the caller's local scope — only pass function parameters in.
- **Operator precedence:** The grammar already encodes precedence via nesting (addition is lower precedence than multiplication because it appears higher in the grammar). Don't flatten it.
- **`void` return:** Functions declared `-> void` should accept a bare `return;` (no expression). Add a `ReturnStmt` variant that allows a null value, or use an `Optional<Expr>`.
- **Forward declarations:** Functions in Tine can call each other freely. Pre-register all function signatures in the type checker and interpreter before walking any bodies.

---

## What "Done" Looks Like

The project is complete when:
1. `examples/hello.tine` (factorial loop) runs and produces correct output
2. A type error (e.g. `int x = true;`) prints a clear error message and exits
3. A syntax error (e.g. missing `}`) prints the line number and exits
4. The code compiles cleanly with `javac` (no external dependencies required)
