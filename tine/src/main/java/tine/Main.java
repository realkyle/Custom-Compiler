package tine;

import tine.lexer.Lexer;
import tine.lexer.Token;
import java.util.List;

public class Main {
    public static void main(String[] args) {
        String source = "fn main() -> void { print(1 + 2); }";
        List<Token> tokens = new Lexer(source).tokenize();
        for (Token t : tokens) {
            System.out.printf("%-12s %-15s line %d%n", t.type(), t.value(), t.line());
        }
    }
}
