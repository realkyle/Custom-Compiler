# Tine Compiler — Handoff

## What This Is

A handmade compiler for a custom language called **Tine** — a statically typed, C-like scripting language. Written in Java 21, no external libraries, no build tools (plain `javac`). The output is a tree-walking interpreter (not machine code), so the full pipeline is:

```
source.tine → Lexer → Parser → TypeChecker → Interpreter → stdout
```

---

## Current State (as of end of session 2)

**Phase 1 (Lexer) is complete and pushed to GitHub.** Phases 2–6 have not been started.

### What exists on disk

```
tine/
├── src/main/java/tine/
│   ├── Main.java                    ← currently a Phase 1 test harness; replace in Phase 6
│   └── lexer/
│       ├── TokenType.java           ← done
│       ├── Token.java               ← done
│       ├── Lexer.java               ← done
│       └── LexException.java        ← done
└── examples/                        ← empty; write hello.tine in Phase 6
```

Still needed (create these directories and files):
```
tine/src/main/java/tine/
├── parser/
│   ├── ast/                         ← all AST node types (Phase 2)
│   ├── Parser.java                  ← Phase 3
│   └── ParseException.java          ← Phase 3
├── analysis/
│   ├── TypeChecker.java             ← Phase 4
│   └── TypeException.java           ← Phase 4
└── runtime/
    └── Interpreter.java             ← Phase 5
```

---

## Environment Setup (Windows, PowerShell)

Java 21 was installed via winget (`winget install EclipseAdoptium.Temurin.21.JDK`). It may not be on PATH in a fresh shell. **Always refresh PATH before running javac:**

```powershell
$env:Path = [System.Environment]::GetEnvironmentVariable("Path", "Machine") + ";" + [System.Environment]::GetEnvironmentVariable("Path", "User")
```

### Compile command (run from `tine/`)

```powershell
cd D:\PersonalProjects\Custom-Compiler\tine
javac -d out (Get-ChildItem src\main\java\tine -Recurse -Filter *.java | Select-Object -ExpandProperty FullName)
```

Or list files explicitly:
```powershell
javac -d out src\main\java\tine\lexer\*.java src\main\java\tine\parser\ast\*.java src\main\java\tine\parser\*.java src\main\java\tine\analysis\*.java src\main\java\tine\runtime\*.java src\main\java\tine\Main.java
```

### Run command

```powershell
java -cp out tine.Main examples\hello.tine
```

### Git commit message syntax (PowerShell only — bash heredoc does NOT work)

```powershell
git commit -m @'
Your message here
'@
```

---

## What Was Tried / Session Notes

- **`Map.of` has a 10-entry limit.** The KEYWORDS map in `Lexer.java` has 11 entries (fn, return, if, else, while, int, bool, void, print, true, false). Must use `Map.ofEntries(Map.entry(...), ...)`.
- **Bash heredoc (`<<'EOF'`) does not work in PowerShell.** Use `@'...'@` instead.
- **`javac` not on PATH after fresh install** — refresh PATH in the same PowerShell session as shown above, or open a new terminal.
- **`tine/out/` is not committed to git** — only source files are tracked.
- **The `tine/` subdirectory is the project root** inside the repo `D:\PersonalProjects\Custom-Compiler`.

---

## Language Reference

### Example program

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

### Supported features

- Types: `int`, `bool`, `void`
- Arithmetic: `+`, `-`, `*`, `/`
- Comparison: `<`, `>`, `==`, `!=`, `<=`, `>=`
- Logic: `&&`, `||`, `!`
- Control flow: `if`/`else`, `while`
- Functions with parameters and return types
- Built-in: `print(expr)`
- Single-line comments: `// ...`
- **Not supported (intentionally):** strings, arrays, floats, closures

### Grammar (BNF)

```
program        → fn_decl*
fn_decl        → "fn" IDENT "(" params? ")" "->" type block
params         → param ("," param)*
param          → type IDENT
block          → "{" stmt* "}"
stmt           → var_decl | assign | return_stmt | if_stmt | while_stmt | print_stmt | expr_stmt
var_decl       → type IDENT "=" expr ";"
assign         → IDENT "=" expr ";"
return_stmt    → "return" expr? ";"
if_stmt        → "if" "(" expr ")" block ("else" block)?
while_stmt     → "while" "(" expr ")" block
print_stmt     → "print" "(" expr ")" ";"
expr_stmt      → expr ";"
expr           → logic_or
logic_or       → logic_and ("||" logic_and)*
logic_and      → equality ("&&" equality)*
equality       → comparison (("==" | "!=") comparison)*
comparison     → addition (("<" | ">" | "<=" | ">=") addition)*
addition       → multiplication (("+" | "-") multiplication)*
multiplication → unary (("*" | "/") unary)*
unary          → "!" unary | primary
primary        → NUMBER | "true" | "false" | IDENT | call | "(" expr ")"
call           → IDENT "(" args? ")"
args           → expr ("," expr)*
type           → "int" | "bool" | "void"
```

---

## Phase 2 — AST Nodes

Create files in `tine/src/main/java/tine/parser/ast/`. Use Java sealed interfaces and records.

**`tine/src/main/java/tine/parser/ast/Expr.java`:**
```java
package tine.parser.ast;

import java.util.List;

public sealed interface Expr
    permits Expr.Number, Expr.Bool, Expr.Var, Expr.Binary, Expr.Unary, Expr.Call {

    record Number(int value) implements Expr {}
    record Bool(boolean value) implements Expr {}
    record Var(String name) implements Expr {}
    record Binary(Expr left, String op, Expr right) implements Expr {}
    record Unary(String op, Expr operand) implements Expr {}
    record Call(String name, List<Expr> args) implements Expr {}
}
```

**`tine/src/main/java/tine/parser/ast/Stmt.java`:**
```java
package tine.parser.ast;

import java.util.List;
import java.util.Optional;

public sealed interface Stmt
    permits Stmt.VarDecl, Stmt.Assign, Stmt.Return, Stmt.If, Stmt.While, Stmt.Print, Stmt.ExprStmt, Stmt.Block {

    record VarDecl(String type, String name, Expr init) implements Stmt {}
    record Assign(String name, Expr value) implements Stmt {}
    record Return(Optional<Expr> value) implements Stmt {}
    record If(Expr condition, Stmt.Block then, Stmt.Block else_) implements Stmt {}   // else_ may be an empty block
    record While(Expr condition, Stmt.Block body) implements Stmt {}
    record Print(Expr value) implements Stmt {}
    record ExprStmt(Expr expr) implements Stmt {}
    record Block(List<Stmt> stmts) implements Stmt {}
}
```

**`tine/src/main/java/tine/parser/ast/FnDecl.java`:**
```java
package tine.parser.ast;

import java.util.List;

public record FnDecl(String name, List<Param> params, String returnType, Stmt.Block body) {}
```

**`tine/src/main/java/tine/parser/ast/Param.java`:**
```java
package tine.parser.ast;

public record Param(String type, String name) {}
```

---

## Phase 3 — Parser

**`tine/src/main/java/tine/parser/ParseException.java`:**
```java
package tine.parser;

public class ParseException extends RuntimeException {
    public ParseException(String message) { super(message); }
}
```

**`tine/src/main/java/tine/parser/Parser.java`** — recursive descent, one method per grammar rule:

```java
package tine.parser;

import tine.lexer.Token;
import tine.lexer.TokenType;
import tine.parser.ast.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class Parser {
    private final List<Token> tokens;
    private int pos = 0;

    public Parser(List<Token> tokens) { this.tokens = tokens; }

    // ── helpers ──────────────────────────────────────────────────────────────

    private Token peek() { return tokens.get(pos); }
    private Token previous() { return tokens.get(pos - 1); }
    private boolean isAtEnd() { return peek().type() == TokenType.EOF; }
    private Token advance() { if (!isAtEnd()) pos++; return previous(); }

    private boolean check(TokenType t) { return peek().type() == t; }

    private boolean match(TokenType... types) {
        for (TokenType t : types) {
            if (check(t)) { advance(); return true; }
        }
        return false;
    }

    private Token expect(TokenType t, String msg) {
        if (check(t)) return advance();
        throw new ParseException(msg + " at line " + peek().line() + ", got '" + peek().value() + "'");
    }

    // ── top level ─────────────────────────────────────────────────────────────

    public List<FnDecl> parseProgram() {
        List<FnDecl> fns = new ArrayList<>();
        while (!isAtEnd()) fns.add(parseFnDecl());
        return fns;
    }

    private FnDecl parseFnDecl() {
        expect(TokenType.FN, "Expected 'fn'");
        String name = expect(TokenType.IDENTIFIER, "Expected function name").value();
        expect(TokenType.LPAREN, "Expected '('");
        List<Param> params = new ArrayList<>();
        if (!check(TokenType.RPAREN)) {
            do { params.add(parseParam()); } while (match(TokenType.COMMA));
        }
        expect(TokenType.RPAREN, "Expected ')'");
        expect(TokenType.ARROW, "Expected '->'");
        String returnType = parseType();
        Stmt.Block body = parseBlock();
        return new FnDecl(name, params, returnType, body);
    }

    private Param parseParam() {
        String type = parseType();
        String name = expect(TokenType.IDENTIFIER, "Expected parameter name").value();
        return new Param(type, name);
    }

    private String parseType() {
        if (match(TokenType.INT))  return "int";
        if (match(TokenType.BOOL)) return "bool";
        if (match(TokenType.VOID)) return "void";
        throw new ParseException("Expected type at line " + peek().line());
    }

    private Stmt.Block parseBlock() {
        expect(TokenType.LBRACE, "Expected '{'");
        List<Stmt> stmts = new ArrayList<>();
        while (!check(TokenType.RBRACE) && !isAtEnd()) stmts.add(parseStmt());
        expect(TokenType.RBRACE, "Expected '}'");
        return new Stmt.Block(stmts);
    }

    // ── statements ────────────────────────────────────────────────────────────

    private Stmt parseStmt() {
        if (check(TokenType.INT) || check(TokenType.BOOL)) return parseVarDecl();
        if (check(TokenType.RETURN)) return parseReturn();
        if (check(TokenType.IF))     return parseIf();
        if (check(TokenType.WHILE))  return parseWhile();
        if (check(TokenType.PRINT))  return parsePrint();
        // assign vs expr_stmt: both start with IDENTIFIER
        if (check(TokenType.IDENTIFIER) && pos + 1 < tokens.size()
                && tokens.get(pos + 1).type() == TokenType.EQ) {
            return parseAssign();
        }
        return parseExprStmt();
    }

    private Stmt parseVarDecl() {
        String type = parseType();
        String name = expect(TokenType.IDENTIFIER, "Expected variable name").value();
        expect(TokenType.EQ, "Expected '='");
        Expr init = parseExpr();
        expect(TokenType.SEMICOLON, "Expected ';'");
        return new Stmt.VarDecl(type, name, init);
    }

    private Stmt parseAssign() {
        String name = advance().value(); // IDENTIFIER
        advance();                       // EQ
        Expr value = parseExpr();
        expect(TokenType.SEMICOLON, "Expected ';'");
        return new Stmt.Assign(name, value);
    }

    private Stmt parseReturn() {
        advance(); // "return"
        Optional<Expr> value = Optional.empty();
        if (!check(TokenType.SEMICOLON)) value = Optional.of(parseExpr());
        expect(TokenType.SEMICOLON, "Expected ';'");
        return new Stmt.Return(value);
    }

    private Stmt parseIf() {
        advance(); // "if"
        expect(TokenType.LPAREN, "Expected '('");
        Expr condition = parseExpr();
        expect(TokenType.RPAREN, "Expected ')'");
        Stmt.Block then = parseBlock();
        Stmt.Block else_ = new Stmt.Block(List.of());
        if (match(TokenType.ELSE)) else_ = parseBlock();
        return new Stmt.If(condition, then, else_);
    }

    private Stmt parseWhile() {
        advance(); // "while"
        expect(TokenType.LPAREN, "Expected '('");
        Expr condition = parseExpr();
        expect(TokenType.RPAREN, "Expected ')'");
        return new Stmt.While(condition, parseBlock());
    }

    private Stmt parsePrint() {
        advance(); // "print"
        expect(TokenType.LPAREN, "Expected '('");
        Expr value = parseExpr();
        expect(TokenType.RPAREN, "Expected ')'");
        expect(TokenType.SEMICOLON, "Expected ';'");
        return new Stmt.Print(value);
    }

    private Stmt parseExprStmt() {
        Expr e = parseExpr();
        expect(TokenType.SEMICOLON, "Expected ';'");
        return new Stmt.ExprStmt(e);
    }

    // ── expressions (precedence climbing via grammar nesting) ─────────────────

    private Expr parseExpr()           { return parseOr(); }

    private Expr parseOr() {
        Expr left = parseAnd();
        while (match(TokenType.PIPE_PIPE))
            left = new Expr.Binary(left, "||", parseAnd());
        return left;
    }

    private Expr parseAnd() {
        Expr left = parseEquality();
        while (match(TokenType.AMP_AMP))
            left = new Expr.Binary(left, "&&", parseEquality());
        return left;
    }

    private Expr parseEquality() {
        Expr left = parseComparison();
        while (match(TokenType.EQ_EQ, TokenType.BANG_EQ))
            left = new Expr.Binary(left, previous().value(), parseComparison());
        return left;
    }

    private Expr parseComparison() {
        Expr left = parseAddition();
        while (match(TokenType.LT, TokenType.GT, TokenType.LT_EQ, TokenType.GT_EQ))
            left = new Expr.Binary(left, previous().value(), parseAddition());
        return left;
    }

    private Expr parseAddition() {
        Expr left = parseMultiplication();
        while (match(TokenType.PLUS, TokenType.MINUS))
            left = new Expr.Binary(left, previous().value(), parseMultiplication());
        return left;
    }

    private Expr parseMultiplication() {
        Expr left = parseUnary();
        while (match(TokenType.STAR, TokenType.SLASH))
            left = new Expr.Binary(left, previous().value(), parseUnary());
        return left;
    }

    private Expr parseUnary() {
        if (match(TokenType.BANG)) return new Expr.Unary("!", parseUnary());
        if (match(TokenType.MINUS)) return new Expr.Unary("-", parseUnary());
        return parsePrimary();
    }

    private Expr parsePrimary() {
        if (match(TokenType.NUMBER))     return new Expr.Number(Integer.parseInt(previous().value()));
        if (match(TokenType.BOOL_TRUE))  return new Expr.Bool(true);
        if (match(TokenType.BOOL_FALSE)) return new Expr.Bool(false);
        if (match(TokenType.LPAREN)) {
            Expr e = parseExpr();
            expect(TokenType.RPAREN, "Expected ')'");
            return e;
        }
        if (check(TokenType.IDENTIFIER)) {
            String name = advance().value();
            if (match(TokenType.LPAREN)) {          // function call
                List<Expr> args = new ArrayList<>();
                if (!check(TokenType.RPAREN)) {
                    do { args.add(parseExpr()); } while (match(TokenType.COMMA));
                }
                expect(TokenType.RPAREN, "Expected ')'");
                return new Expr.Call(name, args);
            }
            return new Expr.Var(name);
        }
        throw new ParseException("Unexpected token '" + peek().value() + "' at line " + peek().line());
    }
}
```

---

## Phase 4 — Type Checker

**`tine/src/main/java/tine/analysis/TypeException.java`:**
```java
package tine.analysis;

public class TypeException extends RuntimeException {
    public TypeException(String message) { super(message); }
}
```

**`tine/src/main/java/tine/analysis/TypeChecker.java`:**

```java
package tine.analysis;

import tine.parser.ast.*;

import java.util.*;

public class TypeChecker {
    private final List<FnDecl> program;
    private final Map<String, FnDecl> functions = new HashMap<>();
    private final Deque<Map<String, String>> scopes = new ArrayDeque<>();
    private String currentReturnType;

    public TypeChecker(List<FnDecl> program) { this.program = program; }

    public void check() {
        // Pre-register all function signatures before checking bodies
        for (FnDecl fn : program) {
            if (functions.containsKey(fn.name()))
                throw new TypeException("Duplicate function: " + fn.name());
            functions.put(fn.name(), fn);
        }
        for (FnDecl fn : program) checkFn(fn);
    }

    // ── scoping ───────────────────────────────────────────────────────────────

    private void pushScope() { scopes.push(new HashMap<>()); }
    private void popScope()  { scopes.pop(); }

    private void declare(String name, String type) {
        scopes.peek().put(name, type);
    }

    private String lookup(String name) {
        for (Map<String, String> scope : scopes) {
            if (scope.containsKey(name)) return scope.get(name);
        }
        throw new TypeException("Undefined variable: " + name);
    }

    // ── functions ─────────────────────────────────────────────────────────────

    private void checkFn(FnDecl fn) {
        currentReturnType = fn.returnType();
        pushScope();
        for (Param p : fn.params()) declare(p.name(), p.type());
        checkBlock(fn.body());
        popScope();
    }

    private void checkBlock(Stmt.Block block) {
        pushScope();
        for (Stmt s : block.stmts()) checkStmt(s);
        popScope();
    }

    // ── statements ────────────────────────────────────────────────────────────

    private void checkStmt(Stmt s) {
        switch (s) {
            case Stmt.VarDecl d -> {
                String initType = checkExpr(d.init());
                if (!initType.equals(d.type()))
                    throw new TypeException("Cannot assign " + initType + " to " + d.type() + " variable '" + d.name() + "'");
                declare(d.name(), d.type());
            }
            case Stmt.Assign a -> {
                String varType = lookup(a.name());
                String valType = checkExpr(a.value());
                if (!varType.equals(valType))
                    throw new TypeException("Cannot assign " + valType + " to " + varType + " variable '" + a.name() + "'");
            }
            case Stmt.Return r -> {
                String retType = r.value().map(this::checkExpr).orElse("void");
                if (!retType.equals(currentReturnType))
                    throw new TypeException("Return type mismatch: expected " + currentReturnType + ", got " + retType);
            }
            case Stmt.If i -> {
                String condType = checkExpr(i.condition());
                if (!condType.equals("bool"))
                    throw new TypeException("if condition must be bool, got " + condType);
                checkBlock(i.then());
                checkBlock(i.else_());
            }
            case Stmt.While w -> {
                String condType = checkExpr(w.condition());
                if (!condType.equals("bool"))
                    throw new TypeException("while condition must be bool, got " + condType);
                checkBlock(w.body());
            }
            case Stmt.Print p -> checkExpr(p.value());
            case Stmt.ExprStmt e -> checkExpr(e.expr());
            case Stmt.Block b -> checkBlock(b);
        }
    }

    // ── expressions ───────────────────────────────────────────────────────────

    private String checkExpr(Expr e) {
        return switch (e) {
            case Expr.Number n  -> "int";
            case Expr.Bool b    -> "bool";
            case Expr.Var v     -> lookup(v.name());
            case Expr.Unary u   -> {
                String t = checkExpr(u.operand());
                yield switch (u.op()) {
                    case "!" -> { if (!t.equals("bool")) throw new TypeException("'!' requires bool, got " + t); yield "bool"; }
                    case "-" -> { if (!t.equals("int"))  throw new TypeException("Unary '-' requires int, got " + t);  yield "int"; }
                    default  -> throw new TypeException("Unknown unary op: " + u.op());
                };
            }
            case Expr.Binary b  -> {
                String lt = checkExpr(b.left());
                String rt = checkExpr(b.right());
                yield switch (b.op()) {
                    case "+", "-", "*", "/" -> {
                        if (!lt.equals("int") || !rt.equals("int"))
                            throw new TypeException("Arithmetic requires int operands, got " + lt + " and " + rt);
                        yield "int";
                    }
                    case "<", ">", "<=", ">=" -> {
                        if (!lt.equals("int") || !rt.equals("int"))
                            throw new TypeException("Comparison requires int operands, got " + lt + " and " + rt);
                        yield "bool";
                    }
                    case "==", "!=" -> {
                        if (!lt.equals(rt))
                            throw new TypeException("Equality requires matching types, got " + lt + " and " + rt);
                        yield "bool";
                    }
                    case "&&", "||" -> {
                        if (!lt.equals("bool") || !rt.equals("bool"))
                            throw new TypeException("Logical op requires bool operands, got " + lt + " and " + rt);
                        yield "bool";
                    }
                    default -> throw new TypeException("Unknown binary op: " + b.op());
                };
            }
            case Expr.Call c -> {
                FnDecl fn = functions.get(c.name());
                if (fn == null) throw new TypeException("Undefined function: " + c.name());
                if (c.args().size() != fn.params().size())
                    throw new TypeException("Function '" + c.name() + "' expects " + fn.params().size() + " args, got " + c.args().size());
                for (int i = 0; i < c.args().size(); i++) {
                    String argType = checkExpr(c.args().get(i));
                    String paramType = fn.params().get(i).type();
                    if (!argType.equals(paramType))
                        throw new TypeException("Argument " + (i + 1) + " of '" + c.name() + "': expected " + paramType + ", got " + argType);
                }
                yield fn.returnType();
            }
        };
    }
}
```

---

## Phase 5 — Interpreter

**`tine/src/main/java/tine/runtime/Interpreter.java`:**

```java
package tine.runtime;

import tine.parser.ast.*;

import java.util.*;

public class Interpreter {
    private final Map<String, FnDecl> functions = new HashMap<>();

    // Sentinel used to unwind the call stack on return
    private static class ReturnValue extends RuntimeException {
        final Object value;
        ReturnValue(Object value) { super(null, null, true, false); this.value = value; }
    }

    public Interpreter(List<FnDecl> program) {
        // Pre-register all functions before running
        for (FnDecl fn : program) functions.put(fn.name(), fn);
    }

    public void run() {
        FnDecl main = functions.get("main");
        if (main == null) throw new RuntimeException("No 'main' function found");
        callFn(main, List.of());
    }

    // ── function calls ────────────────────────────────────────────────────────

    private Object callFn(FnDecl fn, List<Object> argValues) {
        // Each call gets its own fresh scope — never share caller's locals
        Environment env = new Environment();
        for (int i = 0; i < fn.params().size(); i++)
            env.declare(fn.params().get(i).name(), argValues.get(i));
        try {
            execBlock(fn.body(), env);
        } catch (ReturnValue rv) {
            return rv.value;
        }
        return null; // void return
    }

    // ── statements ────────────────────────────────────────────────────────────

    private void execBlock(Stmt.Block block, Environment env) {
        Environment inner = new Environment(env);
        for (Stmt s : block.stmts()) execStmt(s, inner);
    }

    private void execStmt(Stmt s, Environment env) {
        switch (s) {
            case Stmt.VarDecl d    -> env.declare(d.name(), evalExpr(d.init(), env));
            case Stmt.Assign a     -> env.assign(a.name(), evalExpr(a.value(), env));
            case Stmt.Return r     -> throw new ReturnValue(r.value().map(e -> evalExpr(e, env)).orElse(null));
            case Stmt.If i         -> {
                if ((boolean) evalExpr(i.condition(), env)) execBlock(i.then(), env);
                else if (!i.else_().stmts().isEmpty())      execBlock(i.else_(), env);
            }
            case Stmt.While w      -> {
                while ((boolean) evalExpr(w.condition(), env)) execBlock(w.body(), env);
            }
            case Stmt.Print p      -> System.out.println(evalExpr(p.value(), env));
            case Stmt.ExprStmt e   -> evalExpr(e.expr(), env);
            case Stmt.Block b      -> execBlock(b, env);
        }
    }

    // ── expressions ───────────────────────────────────────────────────────────

    private Object evalExpr(Expr e, Environment env) {
        return switch (e) {
            case Expr.Number n  -> n.value();
            case Expr.Bool b    -> b.value();
            case Expr.Var v     -> env.get(v.name());
            case Expr.Unary u   -> switch (u.op()) {
                case "!" -> !(boolean) evalExpr(u.operand(), env);
                case "-" -> -(int)     evalExpr(u.operand(), env);
                default  -> throw new RuntimeException("Unknown unary op: " + u.op());
            };
            case Expr.Binary b  -> {
                Object left  = evalExpr(b.left(), env);
                Object right = evalExpr(b.right(), env);
                yield switch (b.op()) {
                    case "+"  -> (int) left +  (int) right;
                    case "-"  -> (int) left -  (int) right;
                    case "*"  -> (int) left *  (int) right;
                    case "/"  -> (int) left /  (int) right;
                    case "<"  -> (int) left <  (int) right;
                    case ">"  -> (int) left >  (int) right;
                    case "<=" -> (int) left <= (int) right;
                    case ">=" -> (int) left >= (int) right;
                    case "==" -> left.equals(right);
                    case "!=" -> !left.equals(right);
                    case "&&" -> (boolean) left && (boolean) right;
                    case "||" -> (boolean) left || (boolean) right;
                    default   -> throw new RuntimeException("Unknown binary op: " + b.op());
                };
            }
            case Expr.Call c    -> {
                FnDecl fn = functions.get(c.name());
                if (fn == null) throw new RuntimeException("Undefined function: " + c.name());
                List<Object> args = c.args().stream().map(a -> evalExpr(a, env)).toList();
                yield callFn(fn, args);
            }
        };
    }

    // ── environment ───────────────────────────────────────────────────────────

    private static class Environment {
        private final Map<String, Object> vars = new HashMap<>();
        private final Environment parent;

        Environment()               { this.parent = null; }
        Environment(Environment p)  { this.parent = p; }

        void declare(String name, Object value) { vars.put(name, value); }

        void assign(String name, Object value) {
            if (vars.containsKey(name))       { vars.put(name, value); return; }
            if (parent != null)               { parent.assign(name, value); return; }
            throw new RuntimeException("Undefined variable: " + name);
        }

        Object get(String name) {
            if (vars.containsKey(name)) return vars.get(name);
            if (parent != null)         return parent.get(name);
            throw new RuntimeException("Undefined variable: " + name);
        }
    }
}
```

---

## Phase 6 — Main Driver + Example File

**Replace `tine/src/main/java/tine/Main.java`** with:

```java
package tine;

import tine.analysis.TypeChecker;
import tine.lexer.Lexer;
import tine.lexer.LexException;
import tine.lexer.Token;
import tine.parser.ParseException;
import tine.parser.Parser;
import tine.parser.ast.FnDecl;
import tine.runtime.Interpreter;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public class Main {
    public static void main(String[] args) throws IOException {
        if (args.length < 1) {
            System.err.println("Usage: tine <file.tine>");
            System.exit(1);
        }
        String source = Files.readString(Path.of(args[0]));
        try {
            List<Token>  tokens  = new Lexer(source).tokenize();
            List<FnDecl> program = new Parser(tokens).parseProgram();
            new TypeChecker(program).check();
            new Interpreter(program).run();
        } catch (LexException | ParseException | tine.analysis.TypeException e) {
            System.err.println("Error: " + e.getMessage());
            System.exit(1);
        }
    }
}
```

**Create `tine/examples/hello.tine`:**

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

Expected output: `1`, `2`, `6`, `24`, `120`, `720` (one per line).

---

## Key Design Gotchas

- **Return unwinding:** `ReturnValue` extends `RuntimeException` with suppressed stack traces (`super(null, null, true, false)`) so it's cheap to throw inside deep recursion. Do NOT use a boolean flag.
- **Scoping in the interpreter:** `execBlock` creates a new `Environment(parent)` child scope. `callFn` creates a fresh root `Environment()` with no parent — so callers' locals are invisible inside a function call.
- **Operator precedence:** Encoded by grammar nesting (addition is a higher-level rule than multiplication). Do not flatten the parse methods.
- **`void` return:** `Stmt.Return` holds `Optional<Expr>`. A bare `return;` is `Optional.empty()`, which evaluates to Java `null`.
- **Forward declarations:** Both TypeChecker and Interpreter pre-register all function signatures before walking any body. This allows mutual recursion.
- **Unary minus:** The grammar has `unary → "!" unary | primary`. Unary minus (`-x`) is not in the grammar as written in the original handoff, but is needed for expressions like `factorial(-1)`. It was added to the parser and interpreter above.

---

## What "Done" Looks Like

1. `java -cp out tine.Main examples\hello.tine` prints `1 2 6 24 120 720` (one per line).
2. A type error (e.g. `int x = true;`) prints a clear error message and exits with code 1.
3. A syntax error (e.g. missing `}`) prints the line number and exits with code 1.
4. `javac` compiles with zero warnings on Java 21.
5. Update `README.md` implementation status table to mark all phases complete.
