package com.a9.arbor.grc.modules.export;

/**
 * WinAnsi-safe text for Standard 14 PDF fonts. Excel keeps the original characters.
 */
public final class PdfText {

    private PdfText() {
    }

    /** Maps punctuation the report fonts cannot draw. Keeps line breaks. */
    public static String sanitize(String raw) {
        if (raw == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder(raw.length());
        for (int i = 0; i < raw.length(); i++) {
            char c = raw.charAt(i);
            switch (c) {
                case '\u2014', '\u2013', '\u2212' -> sb.append('-');
                case '\u2018', '\u2019', '\u201A', '\u201B' -> sb.append('\'');
                case '\u201C', '\u201D', '\u201E' -> sb.append('"');
                case '\u2026' -> sb.append("...");
                case '\u2022', '\u00B7', '\u2027' -> sb.append('-');
                case '\u00A0', '\t' -> sb.append(' ');
                case '\n', '\r' -> sb.append('\n');
                default -> {
                    if ((c >= 32 && c <= 126) || (c >= 160 && c <= 255)) {
                        sb.append(c);
                    } else if (Character.isWhitespace(c)) {
                        sb.append(' ');
                    }
                }
            }
        }
        return sb.toString();
    }

    public static String oneLine(String raw) {
        String t = sanitize(raw).replace('\n', ' ').replaceAll(" {2,}", " ").trim();
        return t;
    }
}
