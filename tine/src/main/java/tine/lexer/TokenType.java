package tine.lexer;

public enum TokenType {
    // Literals
    NUMBER, BOOL_TRUE, BOOL_FALSE, IDENTIFIER,

    // Arithmetic
    PLUS, MINUS, STAR, SLASH,

    // Comparison
    LT, GT, LT_EQ, GT_EQ, EQ_EQ, BANG_EQ,

    // Logic
    AMP_AMP, PIPE_PIPE, BANG,

    // Assignment & punctuation
    EQ, SEMICOLON, COMMA, LPAREN, RPAREN, LBRACE, RBRACE, ARROW,

    // Keywords
    FN, RETURN, IF, ELSE, WHILE, INT, BOOL, VOID, PRINT,

    // Meta
    EOF
}
