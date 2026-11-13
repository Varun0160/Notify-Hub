package com.notifyhub.common.exception;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class ConstraintNames {

    // Only characters a constraint name can contain. Take the LAST match: the real name comes after
    // the offending value, and an attacker controls that value ("x' for key 'evil").
    private static final Pattern DUPLICATE = Pattern.compile("for key '([A-Za-z0-9_.$-]+)'");
    private static final Pattern FOREIGN_KEY = Pattern.compile("CONSTRAINT `([A-Za-z0-9_.$-]+)`");

    private ConstraintNames() {
    }

    /** The violated constraint's name, or "unknown". Never returns data values. */
    public static String from(Throwable error) {
        Throwable root = error;
        for (int depth = 0; depth < 10 && root.getCause() != null && root.getCause() != root; depth++) {
            root = root.getCause();
        }
        String message = root.getMessage();
        if (message == null) {
            return "unknown";
        }
        String name = lastMatch(DUPLICATE, message);
        if (name == null) {
            name = lastMatch(FOREIGN_KEY, message);
        }
        return name == null ? "unknown" : name;
    }

    private static String lastMatch(Pattern pattern, String text) {
        Matcher m = pattern.matcher(text);
        String last = null;
        while (m.find()) {
            last = m.group(1);
        }
        return last;
    }
}