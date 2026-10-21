package com.leelo.model;

/**
 * Learning status of a term.
 *
 * <p>The numeric codes intentionally match the ones used by Lute v3 so that the
 * status semantics, the highlight colours and the CSV import/export format stay
 * interchangeable between both applications.
 *
 * <p>{@link #UNKNOWN} means "no term record exists yet"; every other value is a
 * real, persisted status.
 */
public enum TermStatus {

    UNKNOWN(0, "Unknown", "?"),
    NEW_1(1, "New (1)", "1"),
    NEW_2(2, "New (2)", "2"),
    LEARNING_3(3, "Learning (3)", "3"),
    LEARNING_4(4, "Learning (4)", "4"),
    LEARNED(5, "Learned", "5"),
    IGNORED(98, "Ignored", "I"),
    WELL_KNOWN(99, "Well Known", "W");

    /**
     * Ladder used when stepping a status up or down. {@link #IGNORED} is
     * deliberately excluded: it is an explicit opt-out, not a learning stage.
     */
    private static final TermStatus[] LADDER = {
            UNKNOWN, NEW_1, NEW_2, LEARNING_3, LEARNING_4, LEARNED, WELL_KNOWN
    };

    private final int code;
    private final String label;
    private final String abbreviation;

    TermStatus(int code, String label, String abbreviation) {
        this.code = code;
        this.label = label;
        this.abbreviation = abbreviation;
    }

    public int getCode() {
        return code;
    }

    public String getLabel() {
        return label;
    }

    public String getAbbreviation() {
        return abbreviation;
    }

    /** Returns the status for the given numeric code, defaulting to {@link #UNKNOWN}. */
    public static TermStatus fromCode(int code) {
        for (TermStatus status : values()) {
            if (status.code == code) {
                return status;
            }
        }
        return UNKNOWN;
    }

    /**
     * Resolves a status from its CSV abbreviation ({@code W}, {@code I}, a digit)
     * or from its label. Returns {@code null} when the value is not recognised.
     */
    public static TermStatus fromAbbreviation(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        if (trimmed.isEmpty()) {
            return null;
        }
        for (TermStatus status : values()) {
            if (status.abbreviation.equalsIgnoreCase(trimmed) || status.label.equalsIgnoreCase(trimmed)) {
                return status;
            }
        }
        return null;
    }

    /** Returns the next status in the learning ladder, or {@code null} at the top. */
    public TermStatus stepUp() {
        if (this == IGNORED) {
            return UNKNOWN;
        }
        int index = indexInLadder(this);
        return index >= 0 && index + 1 < LADDER.length ? LADDER[index + 1] : null;
    }

    /** Returns the previous status in the learning ladder, or {@code null} at the bottom. */
    public TermStatus stepDown() {
        if (this == IGNORED) {
            return UNKNOWN;
        }
        int index = indexInLadder(this);
        return index > 0 ? LADDER[index - 1] : null;
    }

    /** True when the status is part of the learning ladder (i.e. assignable by the user). */
    public boolean isAssignable() {
        return this != UNKNOWN;
    }

    /**
     * True when the reading screen paints the status as a background colour.
     *
     * <p>Only the unknown word and the learning stages 1 to 4 are highlighted:
     * learned, ignored and well known terms stay as plain text, so that the
     * words that are still being studied are the ones that stand out.
     */
    public boolean isHighlighted() {
        return this != LEARNED && this != IGNORED && this != WELL_KNOWN;
    }

    private static int indexInLadder(TermStatus status) {
        for (int i = 0; i < LADDER.length; i++) {
            if (LADDER[i] == status) {
                return i;
            }
        }
        return -1;
    }
}
