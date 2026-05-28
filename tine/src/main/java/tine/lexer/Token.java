package tine.lexer;

public record Token(TokenType type, String value, int line) {}
