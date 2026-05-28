package tine.lexer;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class Lexer {
    private final String source;
    private int pos = 0;
    private int line = 1;

    private static final Map<String, TokenType> KEYWORDS = Map.ofEntries(
        Map.entry("fn",     TokenType.FN),
        Map.entry("return", TokenType.RETURN),
        Map.entry("if",     TokenType.IF),
        Map.entry("else",   TokenType.ELSE),
        Map.entry("while",  TokenType.WHILE),
        Map.entry("int",    TokenType.INT),
        Map.entry("bool",   TokenType.BOOL),
        Map.entry("void",   TokenType.VOID),
        Map.entry("print",  TokenType.PRINT),
        Map.entry("true",   TokenType.BOOL_TRUE),
        Map.entry("false",  TokenType.BOOL_FALSE)
    );

    public Lexer(String source) {
        this.source = source;
    }

    public List<Token> tokenize() {
        List<Token> tokens = new ArrayList<>();
        while (pos < source.length()) {
            skipWhitespaceAndComments();
            if (pos >= source.length()) break;
            tokens.add(nextToken());
        }
        tokens.add(new Token(TokenType.EOF, "", line));
        return tokens;
    }

    private void skipWhitespaceAndComments() {
        while (pos < source.length()) {
            char c = source.charAt(pos);
            if (c == '\n') {
                line++;
                pos++;
            } else if (Character.isWhitespace(c)) {
                pos++;
            } else if (c == '/' && pos + 1 < source.length() && source.charAt(pos + 1) == '/') {
                while (pos < source.length() && source.charAt(pos) != '\n') pos++;
            } else {
                break;
            }
        }
    }

    private Token nextToken() {
        int startLine = line;
        char c = source.charAt(pos);

        if (Character.isDigit(c)) return readNumber(startLine);
        if (Character.isLetter(c) || c == '_') return readIdentOrKeyword(startLine);

        pos++;
        return switch (c) {
            case '+' -> new Token(TokenType.PLUS,      "+",  startLine);
            case '*' -> new Token(TokenType.STAR,      "*",  startLine);
            case '/' -> new Token(TokenType.SLASH,     "/",  startLine);
            case ';' -> new Token(TokenType.SEMICOLON, ";",  startLine);
            case ',' -> new Token(TokenType.COMMA,     ",",  startLine);
            case '(' -> new Token(TokenType.LPAREN,    "(",  startLine);
            case ')' -> new Token(TokenType.RPAREN,    ")",  startLine);
            case '{' -> new Token(TokenType.LBRACE,    "{",  startLine);
            case '}' -> new Token(TokenType.RBRACE,    "}",  startLine);
            case '-' -> {
                if (peek() == '>') { pos++; yield new Token(TokenType.ARROW,   "->", startLine); }
                yield new Token(TokenType.MINUS, "-", startLine);
            }
            case '<' -> {
                if (peek() == '=') { pos++; yield new Token(TokenType.LT_EQ,   "<=", startLine); }
                yield new Token(TokenType.LT, "<", startLine);
            }
            case '>' -> {
                if (peek() == '=') { pos++; yield new Token(TokenType.GT_EQ,   ">=", startLine); }
                yield new Token(TokenType.GT, ">", startLine);
            }
            case '=' -> {
                if (peek() == '=') { pos++; yield new Token(TokenType.EQ_EQ,   "==", startLine); }
                yield new Token(TokenType.EQ, "=", startLine);
            }
            case '!' -> {
                if (peek() == '=') { pos++; yield new Token(TokenType.BANG_EQ, "!=", startLine); }
                yield new Token(TokenType.BANG, "!", startLine);
            }
            case '&' -> {
                if (peek() == '&') { pos++; yield new Token(TokenType.AMP_AMP,   "&&", startLine); }
                throw new LexException("Unexpected '&' at line " + startLine + " (did you mean '&&'?)");
            }
            case '|' -> {
                if (peek() == '|') { pos++; yield new Token(TokenType.PIPE_PIPE, "||", startLine); }
                throw new LexException("Unexpected '|' at line " + startLine + " (did you mean '||'?)");
            }
            default -> throw new LexException("Unexpected character '" + c + "' at line " + startLine);
        };
    }

    private char peek() {
        return pos < source.length() ? source.charAt(pos) : '\0';
    }

    private Token readNumber(int startLine) {
        int start = pos;
        while (pos < source.length() && Character.isDigit(source.charAt(pos))) pos++;
        return new Token(TokenType.NUMBER, source.substring(start, pos), startLine);
    }

    private Token readIdentOrKeyword(int startLine) {
        int start = pos;
        while (pos < source.length() && (Character.isLetterOrDigit(source.charAt(pos)) || source.charAt(pos) == '_')) pos++;
        String text = source.substring(start, pos);
        TokenType type = KEYWORDS.getOrDefault(text, TokenType.IDENTIFIER);
        return new Token(type, text, startLine);
    }
}
