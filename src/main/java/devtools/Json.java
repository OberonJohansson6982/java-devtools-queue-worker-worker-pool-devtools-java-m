package devtools;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Minimal JSON reader for API envelopes (no runtime dependencies). */
final class Json {
    private final String source;
    private int pos;
    private Json(String source) { this.source = source; }
    static Object parse(String source) {
        Json reader = new Json(source);
        Object value = reader.value();
        reader.space();
        if (reader.pos != source.length()) throw new IllegalArgumentException("Trailing JSON content");
        return value;
    }
    static String quote(String value) {
        StringBuilder out = new StringBuilder("\"");
        for (char c : value.toCharArray()) {
            switch (c) {
                case '"' -> out.append("\\\"");
                case '\\' -> out.append("\\\\");
                case '\n' -> out.append("\\n");
                case '\r' -> out.append("\\r");
                case '\t' -> out.append("\\t");
                default -> { if (c < 32) out.append(String.format("\\u%04x", (int)c)); else out.append(c); }
            }
        }
        return out.append('"').toString();
    }
    private void space() { while (pos < source.length() && Character.isWhitespace(source.charAt(pos))) pos++; }
    private char take() {
        if (pos >= source.length()) throw new IllegalArgumentException("Incomplete JSON");
        return source.charAt(pos++);
    }
    private void expect(char c) { space(); if (take() != c) throw new IllegalArgumentException("Expected " + c); }
    private Object value() {
        space();
        if (pos >= source.length()) throw new IllegalArgumentException("Missing JSON value");
        char c = source.charAt(pos);
        if (c == '"') return string();
        if (c == '{') {
            pos++; Map<String, Object> map = new LinkedHashMap<>(); space();
            if (pos < source.length() && source.charAt(pos) == '}') { pos++; return map; }
            do {
                space(); String key = string(); expect(':'); map.put(key, value()); space();
                c = take();
                if (c == '}') return map;
                if (c != ',') throw new IllegalArgumentException("Expected object separator");
            } while (true);
        }
        if (c == '[') {
            pos++; List<Object> list = new ArrayList<>(); space();
            if (pos < source.length() && source.charAt(pos) == ']') { pos++; return list; }
            do {
                list.add(value()); space(); c = take();
                if (c == ']') return list;
                if (c != ',') throw new IllegalArgumentException("Expected array separator");
            } while (true);
        }
        for (String literal : List.of("true", "false", "null")) {
            if (source.startsWith(literal, pos)) { pos += literal.length(); return literal.equals("null") ? null : Boolean.valueOf(literal); }
        }
        int start = pos;
        while (pos < source.length() && "-+0123456789.eE".indexOf(source.charAt(pos)) >= 0) pos++;
        if (start == pos) throw new IllegalArgumentException("Invalid JSON value");
        return new java.math.BigDecimal(source.substring(start, pos));
    }
    private String string() {
        if (take() != '"') throw new IllegalArgumentException("Expected JSON string");
        StringBuilder out = new StringBuilder();
        while (true) {
            char c = take();
            if (c == '"') return out.toString();
            if (c == '\\') {
                c = take();
                switch (c) {
                    case '"', '\\', '/' -> out.append(c);
                    case 'b' -> out.append('\b');
                    case 'f' -> out.append('\f');
                    case 'n' -> out.append('\n');
                    case 'r' -> out.append('\r');
                    case 't' -> out.append('\t');
                    case 'u' -> { int n = 0; for (int i = 0; i < 4; i++) { int d = Character.digit(take(), 16); if (d < 0) throw new IllegalArgumentException("Invalid unicode escape"); n = n * 16 + d; } out.append((char)n); }
                    default -> throw new IllegalArgumentException("Invalid JSON escape");
                }
            } else { if (c < 32) throw new IllegalArgumentException("Invalid string character"); out.append(c); }
        }
    }
}
