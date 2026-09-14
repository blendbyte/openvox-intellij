package net.blendbyte.openvox.lexer;

import com.intellij.lexer.FlexLexer;
import com.intellij.psi.tree.IElementType;
import com.intellij.psi.TokenType;
import java.util.ArrayDeque;
import java.util.Deque;

import static net.blendbyte.openvox.psi.OpenVoxTypes.*;

%%

%public
%class _OpenVoxLexer
%implements FlexLexer
%unicode
%function advance
%type IElementType

%{
  private IElementType lastToken = null;

  private final Deque<Integer> interpolationDepth = new ArrayDeque<>();
  private final Deque<Integer> interpolationReturnState = new ArrayDeque<>();

  private final Deque<String> pendingHeredocs = new ArrayDeque<>();
  private String currentHeredocTag = null;

  public _OpenVoxLexer() {
    this((java.io.Reader) null);
  }

  public void resetInternalState() {
    lastToken = null;
    interpolationDepth.clear();
    interpolationReturnState.clear();
    pendingHeredocs.clear();
    currentHeredocTag = null;
  }

  public static final int REGEX_STATE = 16;
  private static final int SELECTOR_STATE = 32;
  public static final int COMPLEX_STATE = 64;

  public int contextState() {
    int state = yystate() | (regexAcceptable() ? REGEX_STATE : 0)
        | (lastToken == QMARK ? SELECTOR_STATE : 0);
    return inInterpolation() || !pendingHeredocs.isEmpty() || currentHeredocTag != null
        ? state | COMPLEX_STATE : state;
  }

  public void restoreContext(int state) {
    resetInternalState();
    lastToken = (state & SELECTOR_STATE) != 0 ? QMARK
        : (state & REGEX_STATE) != 0 ? EQUALS : IDENTIFIER;
  }

  private IElementType eppText() {
    if (zzMarkedPos + 3 <= zzEndRead && zzBuffer.charAt(zzMarkedPos) == '<'
        && zzBuffer.charAt(zzMarkedPos + 1) == '%' && zzBuffer.charAt(zzMarkedPos + 2) == '-') {
      int keep = yylength();
      while (keep > 0 && (yycharat(keep - 1) == ' ' || yycharat(keep - 1) == '\t')) keep--;
      if (keep == 0) return TokenType.WHITE_SPACE;
      if (keep < yylength()) yypushback(yylength() - keep);
    }
    return EPP_TEXT;
  }

  private IElementType tok(IElementType type) {
    lastToken = type;
    return type;
  }

  private boolean inInterpolation() {
    return !interpolationDepth.isEmpty();
  }

  private boolean regexAcceptable() {
    if (lastToken == null) return true;
    return lastToken == LPAREN || lastToken == WSLPAREN || lastToken == LISTSTART
        || lastToken == LBRACK || lastToken == LBRACE || lastToken == SELBRACE
        || lastToken == RBRACE || lastToken == COMMA || lastToken == COLON || lastToken == SEMIC
        || lastToken == MATCH || lastToken == NOMATCH || lastToken == FARROW || lastToken == PARROW
        || lastToken == EQUALS || lastToken == APPENDS || lastToken == DELETES
        || lastToken == AND || lastToken == OR || lastToken == NOT || lastToken == IN
        || lastToken == IF || lastToken == ELSIF || lastToken == UNLESS
        || lastToken == CASE || lastToken == NODE || lastToken == QMARK
        || lastToken == IN_EDGE || lastToken == IN_EDGE_SUB
        || lastToken == OUT_EDGE || lastToken == OUT_EDGE_SUB
        || lastToken == ISEQUAL || lastToken == NOTEQUAL
        || lastToken == LESSTHAN || lastToken == LESSEQUAL
        || lastToken == GREATERTHAN || lastToken == GREATEREQUAL;
  }

  private boolean precededByWhitespace() {
    int start = getTokenStart();
    if (start == 0) return true;
    char c = zzBuffer.charAt(start - 1);
    return c == ' ' || c == '\t' || c == '\r' || c == '\n';
  }

  private void queueHeredocTag() {
    int i = zzMarkedPos;
    int end = zzEndRead;
    StringBuilder sb = new StringBuilder();
    while (i < end && zzBuffer.charAt(i) != ')' && zzBuffer.charAt(i) != '\n') {
      sb.append(zzBuffer.charAt(i));
      i++;
    }
    String raw = sb.toString().trim();
    int cut = raw.length();
    int slash = raw.indexOf('/');
    int colon = raw.indexOf(':');
    if (slash >= 0) cut = Math.min(cut, slash);
    if (colon >= 0) cut = Math.min(cut, colon);
    String tag = raw.substring(0, cut).trim();
    if (tag.length() >= 2) {
      char first = tag.charAt(0);
      char last = tag.charAt(tag.length() - 1);
      if ((first == '"' && last == '"') || (first == '\'' && last == '\'')) {
        tag = tag.substring(1, tag.length() - 1);
      }
    }
    if (!tag.isEmpty()) pendingHeredocs.addLast(tag);
  }

  private boolean isHeredocTerminator(CharSequence line, String tag) {
    int i = 0;
    int n = line.length();
    while (i < n && (line.charAt(i) == ' ' || line.charAt(i) == '\t')) i++;
    if (i < n && line.charAt(i) == '|') {
      i++;
      while (i < n && (line.charAt(i) == ' ' || line.charAt(i) == '\t')) i++;
    }
    if (i < n && line.charAt(i) == '-') {
      i++;
      while (i < n && (line.charAt(i) == ' ' || line.charAt(i) == '\t')) i++;
    }
    int remaining = n;
    while (remaining > i && (line.charAt(remaining - 1) == '\n' || line.charAt(remaining - 1) == '\r'
        || line.charAt(remaining - 1) == ' ' || line.charAt(remaining - 1) == '\t')) {
      remaining--;
    }
    return line.subSequence(i, remaining).toString().equals(tag);
  }
%}

%state DQ
%state HEREDOC_BODY
%state EPP_TEXT_ST
%state EPP_COMMENT_ST

BLANK          = [ \t\r]
NEWLINE        = \r\n | \r | \n
LINE_COMMENT_R = "#" [^\r\n]*
BLOCK_COMMENT_R= "/*" ( [^*] | \*+ [^/*] )* \*+ "/"

NAME_R         = ("::")? [a-z] [a-zA-Z0-9_]* ("::" [a-z] [a-zA-Z0-9_]*)*
CLASSREF_R     = ("::")? [A-Z] [a-zA-Z0-9_]* ("::" [A-Z] [a-zA-Z0-9_]*)*
BARE_WORD_R    = ("::")? [a-z_] ([a-zA-Z0-9_\-]* [a-zA-Z0-9_])?
VARIABLE_R     = "$" ("::")? ([a-zA-Z0-9_]+ "::")* [a-zA-Z0-9_]+
NUMBER_R       = 0[xX][0-9A-Fa-f]+ | 0[0-7]+ | [0-9]+ ("." [0-9]+)? ([eE] "-"? [0-9]+)?
SQ_STRING_R    = "'" ( [^'\\] | \\ . )* "'"
REGEX_R        = "/" ( [^/\\\r\n] | \\ [^\r\n] )* "/"

%%

<HEREDOC_BODY> {
  ( [^\r\n]+ {NEWLINE}? ) | {NEWLINE} {
      if (currentHeredocTag != null && isHeredocTerminator(yytext(), currentHeredocTag)) {
        currentHeredocTag = pendingHeredocs.pollFirst();
        if (currentHeredocTag == null) yybegin(YYINITIAL);
        return tok(HD_END);
      }
      return HD_TEXT;
  }
  <<EOF>> { yybegin(YYINITIAL); return null; }
}

<DQ> {
  "\""                      { yybegin(YYINITIAL); return tok(DQ_END); }
  \\ .                      { return DQ_ESCAPE; }
  "${"                      { interpolationDepth.push(0); interpolationReturnState.push(DQ);
                              yybegin(YYINITIAL); return tok(INTERP_START); }
  {VARIABLE_R}              { return tok(VARIABLE); }
  [^\"\\$]+                 { return DQ_TEXT; }
  "$"                       { return DQ_TEXT; }
}

<EPP_TEXT_ST> {
  "<%#"                     { yybegin(EPP_COMMENT_ST); return tok(EPP_COMMENT); }
  "<%%"                     { return EPP_TEXT; }
  "<%="                     { yybegin(YYINITIAL); return tok(EPP_EXPR_START); }
  "<%-"                     { yybegin(YYINITIAL); return tok(EPP_START_TRIM); }
  "<%"                      { yybegin(YYINITIAL); return tok(EPP_START); }
  [^<]+                     { return eppText(); }
  "<"                       { return EPP_TEXT; }
}

<EPP_COMMENT_ST> {
  "-%>" {BLANK}* {NEWLINE}?  { yybegin(EPP_TEXT_ST); return EPP_COMMENT; }
  "%>"                      { yybegin(EPP_TEXT_ST); return EPP_COMMENT; }
  [^%\-]+                   { return EPP_COMMENT; }
  [%\-]                     { return EPP_COMMENT; }
}

<YYINITIAL> {
  "-%>" {BLANK}* {NEWLINE}?  { yybegin(EPP_TEXT_ST); return tok(EPP_END_TRIM); }
  "%>"                      { yybegin(EPP_TEXT_ST); return tok(EPP_END); }

  {NEWLINE}                 { if (!pendingHeredocs.isEmpty() && !inInterpolation()) {
                                currentHeredocTag = pendingHeredocs.pollFirst();
                                yybegin(HEREDOC_BODY);
                              }
                              return TokenType.WHITE_SPACE; }
  {BLANK}+                  { return TokenType.WHITE_SPACE; }
  {LINE_COMMENT_R}          { return LINE_COMMENT; }
  {BLOCK_COMMENT_R}         { return BLOCK_COMMENT; }

  "case"                    { return tok(CASE); }
  "class"                   { return tok(CLASS); }
  "default"                 { return tok(DEFAULT); }
  "define"                  { return tok(DEFINE); }
  "if"                      { return tok(IF); }
  "elsif"                   { return tok(ELSIF); }
  "else"                    { return tok(ELSE); }
  "unless"                  { return tok(UNLESS); }
  "inherits"                { return tok(INHERITS); }
  "node"                    { return tok(NODE); }
  "and"                     { return tok(AND); }
  "or"                      { return tok(OR); }
  "in"                      { return tok(IN); }
  "undef"                   { return tok(UNDEF); }
  "true"                    { return tok(TRUE); }
  "false"                   { return tok(FALSE); }
  "function"                { return tok(FUNCTION); }
  "type"                    { return tok(TYPE); }
  "attr"                    { return tok(ATTR); }
  "private"                 { return tok(PRIVATE); }
  "plan"                    { return tok(PLAN); }
  "apply"                   { return tok(APPLY); }

  "@("                      { queueHeredocTag(); return tok(HD_TAG); }

  "\""                      { yybegin(DQ); return tok(DQ_START); }
  {SQ_STRING_R}             { return tok(SQ_STRING); }

  "}"                       { if (inInterpolation()) {
                                int depth = interpolationDepth.peek();
                                if (depth == 0) {
                                  interpolationDepth.pop();
                                  yybegin(interpolationReturnState.pop());
                                  return tok(INTERP_END);
                                }
                                interpolationDepth.pop();
                                interpolationDepth.push(depth - 1);
                              }
                              return tok(RBRACE); }
  "{"                       { if (inInterpolation()) {
                                interpolationDepth.push(interpolationDepth.pop() + 1);
                              }
                              return tok(lastToken == QMARK ? SELBRACE : LBRACE); }

  "<<|"                     { return tok(LLCOLLECT); }
  "|>>"                     { return tok(RRCOLLECT); }
  "<|"                      { return tok(LCOLLECT); }
  "|>"                      { return tok(RCOLLECT); }
  "=="                      { return tok(ISEQUAL); }
  "!="                      { return tok(NOTEQUAL); }
  "=~"                      { return tok(MATCH); }
  "!~"                      { return tok(NOMATCH); }
  ">="                      { return tok(GREATEREQUAL); }
  "<="                      { return tok(LESSEQUAL); }
  "=>"                      { return tok(FARROW); }
  "+>"                      { return tok(PARROW); }
  "+="                      { return tok(APPENDS); }
  "-="                      { return tok(DELETES); }
  "->"                      { return tok(IN_EDGE); }
  "~>"                      { return tok(IN_EDGE_SUB); }
  "<-"                      { return tok(OUT_EDGE); }
  "<~"                      { return tok(OUT_EDGE_SUB); }
  "<<"                      { return tok(LSHIFT); }
  ">>"                      { return tok(RSHIFT); }
  "@@"                      { return tok(ATAT); }

  {REGEX_R}                 { if (regexAcceptable()) return tok(REGEX);
                              yypushback(yylength() - 1);
                              return tok(DIV); }

  "["                       { return tok(precededByWhitespace() ? LISTSTART : LBRACK); }
  "]"                       { return tok(RBRACK); }
  "("                       { return tok(precededByWhitespace() ? WSLPAREN : LPAREN); }
  ")"                       { return tok(RPAREN); }
  "="                       { return tok(EQUALS); }
  ">"                       { return tok(GREATERTHAN); }
  "<"                       { return tok(LESSTHAN); }
  "+"                       { return tok(PLUS); }
  "-"                       { return tok(MINUS); }
  "*"                       { return tok(TIMES); }
  "/"                       { return tok(DIV); }
  "%"                       { return tok(MODULO); }
  "!"                       { return tok(NOT); }
  "."                       { return tok(DOT); }
  "|"                       { return tok(PIPE); }
  "@"                       { return tok(AT); }
  ":"                       { return tok(COLON); }
  ","                       { return tok(COMMA); }
  ";"                       { return tok(SEMIC); }
  "?"                       { return tok(QMARK); }
  "~"                       { return tok(TILDE); }

  {VARIABLE_R}              { return tok(VARIABLE); }
  {NUMBER_R}                { return tok(NUMBER); }
  {CLASSREF_R}              { return tok(CLASSREF); }
  {NAME_R}                  { return tok(IDENTIFIER); }
  {BARE_WORD_R}             { return tok(BARE_WORD); }
}

[^]                         { return TokenType.BAD_CHARACTER; }
