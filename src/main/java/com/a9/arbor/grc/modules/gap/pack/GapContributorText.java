package com.a9.arbor.grc.modules.gap.pack;

import java.util.regex.Pattern;

/**
 * Doc 04 D4-G4: Contributor Gap comments / question notes are plain prose only.
 * Letters (any script), numbers, whitespace, and sentence punctuation.
 */
public final class GapContributorText {

    public static final String HINT =
            "Letters, numbers, and . , ; : ! ? ' \" - ( ) / only.";

    public static final String MESSAGE =
            "Comments may only include letters, numbers, spaces, and . , ; : ! ? ' \" - ( ) /";

    /** Disallowed: symbols (@ # $ % …), markup, emoji. Curly quotes/dashes from Word are allowed. */
    private static final Pattern DISALLOWED = Pattern.compile(
            "[^\\p{L}\\p{N}\\s.,;:!?\"'()\\-/\\u2018\\u2019\\u201C\\u201D\\u2013\\u2014]");

    private GapContributorText() {
    }

    public static boolean isAllowed(String text) {
        if (text == null || text.isBlank()) {
            return true;
        }
        return !DISALLOWED.matcher(text).find();
    }

    public static void requireAllowed(String text) {
        if (!isAllowed(text)) {
            throw new IllegalArgumentException(MESSAGE);
        }
    }
}
