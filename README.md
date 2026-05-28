# Tine Compiler

A handmade compiler for **Tine** — a small, statically typed, C-like language — written in Java from scratch. No parser-generator libraries (no ANTLR, no JavaCC). Every phase is implemented by hand.

## Pipeline

```
source.tine → Lexer → Parser → TypeChecker → Interpreter → stdout
```

The backend is a tree-walking interpreter, so the full compiler pipeline (lexing, parsing, type checking) is implemented without the complexity of code generation. The interpreter can later be swapped for a bytecode or LLVM IR emitter.

## Language Features

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

| Feature | Supported |
|---|---|
| Types: `int`, `bool`, `void` | ✓ |
| Arithmetic: `+` `-` `*` `/` | ✓ |
| Comparison: `<` `>` `==` `!=` `<=` `>=` | ✓ |
| Logic: `&&` `\|\|` `!` | ✓ |
| `if` / `else` | ✓ |
| `while` loops | ✓ |
| Functions with parameters and return types | ✓ |
| `print(expr)` built-in | ✓ |
| Single-line comments `// ...` | ✓ |
| Strings, arrays, floats, closures | — (out of scope) |

## Project Structure

```
tine/
├── src/main/java/tine/
│   ├── Main.java
│   ├── lexer/
│   │   ├── TokenType.java     # ~30 token types
│   │   ├── Token.java         # record: (type, value, line)
│   │   ├── Lexer.java         # char-by-char scanner
│   │   └── LexException.java
│   ├── parser/
│   │   ├── ast/               # sealed interface node types
│   │   └── Parser.java        # recursive descent
│   ├── analysis/
│   │   └── TypeChecker.java   # symbol table, type enforcement
│   └── runtime/
│       └── Interpreter.java   # tree-walking eval
└── examples/
    └── hello.tine             # factorial loop demo
```

## Build & Run

Requires **Java 21** (uses records, sealed interfaces, switch expressions).

```bash
# Compile
cd tine
javac -d out src/main/java/tine/**/*.java src/main/java/tine/Main.java

# Run a .tine file
java -cp out tine.Main examples/hello.tine
```

Expected output for `hello.tine` (factorial of 1–6):
```
1
2
6
24
120
720
```

## Implementation Status

| Phase | Component | Status |
|---|---|---|
| 1 | Lexer | ✅ Complete |
| 2 | AST Nodes | 🔲 Not started |
| 3 | Parser | 🔲 Not started |
| 4 | Type Checker | 🔲 Not started |
| 5 | Interpreter | 🔲 Not started |
| 6 | Main Driver | 🔲 Not started |
