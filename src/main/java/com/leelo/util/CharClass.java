package com.leelo.util;

import java.util.ArrayList;
import java.util.List;

/**
 * A set of characters described by a compact specification.
 *
 * <p>The specification may contain single characters and {@code X-Y} ranges, for
 * instance {@code "a-zA-ZÀ-Ö"}. Parsing it once and keeping the ranges around
 * makes the membership test a simple binary search, which matters because the
 * parsers test every character of every text.
 */
public final class CharClass {

    private final int[] starts;
    private final int[] ends;

    private CharClass(int[] starts, int[] ends) {
        this.starts = starts;
        this.ends = ends;
    }

    /** Parses a character specification into a reusable character class. */
    public static CharClass parse(String specification) {
        List<int[]> ranges = new ArrayList<>();
        if (specification != null) {
            int i = 0;
            int length = specification.length();
            while (i < length) {
                char current = specification.charAt(i);
                if (current == '\\' && i + 1 < length) {
                    // Escaped character: treat it literally.
                    i++;
                    addSingle(ranges, specification.charAt(i));
                    i++;
                    continue;
                }
                if (i + 2 < length && specification.charAt(i + 1) == '-') {
                    char from = current;
                    char to = specification.charAt(i + 2);
                    if (from <= to) {
                        ranges.add(new int[]{from, to});
                        i += 3;
                        continue;
                    }
                }
                addSingle(ranges, current);
                i++;
            }
        }
        return fromRanges(ranges);
    }

    private static void addSingle(List<int[]> ranges, char value) {
        ranges.add(new int[]{value, value});
    }

    private static CharClass fromRanges(List<int[]> ranges) {
        ranges.sort((a, b) -> Integer.compare(a[0], b[0]));
        int[] starts = new int[ranges.size()];
        int[] ends = new int[ranges.size()];
        int index = 0;
        for (int[] range : ranges) {
            if (index > 0 && range[0] <= ends[index - 1] + 1) {
                ends[index - 1] = Math.max(ends[index - 1], range[1]);
            } else {
                starts[index] = range[0];
                ends[index] = range[1];
                index++;
            }
        }
        int[] trimmedStarts = new int[index];
        int[] trimmedEnds = new int[index];
        System.arraycopy(starts, 0, trimmedStarts, 0, index);
        System.arraycopy(ends, 0, trimmedEnds, 0, index);
        return new CharClass(trimmedStarts, trimmedEnds);
    }

    /** True when the given character belongs to the class. */
    public boolean matches(char value) {
        int low = 0;
        int high = starts.length - 1;
        while (low <= high) {
            int mid = (low + high) >>> 1;
            if (value < starts[mid]) {
                high = mid - 1;
            } else if (value > ends[mid]) {
                low = mid + 1;
            } else {
                return true;
            }
        }
        return false;
    }

    /** True when the class holds no character at all. */
    public boolean isEmpty() {
        return starts.length == 0;
    }

    /** Returns the regular expression matching any character of the class. */
    public String toRegex() {
        StringBuilder builder = new StringBuilder("[");
        for (int i = 0; i < starts.length; i++) {
            appendEscaped(builder, (char) starts[i]);
            if (ends[i] != starts[i]) {
                builder.append('-');
                appendEscaped(builder, (char) ends[i]);
            }
        }
        return builder.append(']').toString();
    }

    private static void appendEscaped(StringBuilder builder, char value) {
        if (value == ']' || value == '\\' || value == '^' || value == '[') {
            builder.append('\\');
        }
        builder.append(value);
    }
}
