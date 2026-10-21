package com.leelo.model;

import java.util.EnumMap;
import java.util.Map;

/**
 * Aggregated statistics for a book.
 *
 * <p>The values are sampled from the pages around the one being read, which
 * keeps the computation cheap while still reflecting the reader's progress.
 */
public class BookStats {

    private int bookId;
    private int distinctTerms;
    private int distinctUnknowns;
    private int unknownPercent;
    private Map<TermStatus, Integer> statusDistribution = new EnumMap<>(TermStatus.class);

    public int getBookId() {
        return bookId;
    }

    public void setBookId(int bookId) {
        this.bookId = bookId;
    }

    /** Number of distinct terms found in the sampled pages. */
    public int getDistinctTerms() {
        return distinctTerms;
    }

    public void setDistinctTerms(int distinctTerms) {
        this.distinctTerms = distinctTerms;
    }

    /** Number of distinct terms that are still unknown. */
    public int getDistinctUnknowns() {
        return distinctUnknowns;
    }

    public void setDistinctUnknowns(int distinctUnknowns) {
        this.distinctUnknowns = distinctUnknowns;
    }

    /** Percentage of the sampled terms that are unknown, rounded to the nearest integer. */
    public int getUnknownPercent() {
        return unknownPercent;
    }

    public void setUnknownPercent(int unknownPercent) {
        this.unknownPercent = unknownPercent;
    }

    /** Number of distinct terms per status. */
    public Map<TermStatus, Integer> getStatusDistribution() {
        return statusDistribution;
    }

    public void setStatusDistribution(Map<TermStatus, Integer> statusDistribution) {
        this.statusDistribution = statusDistribution == null
                ? new EnumMap<>(TermStatus.class)
                : statusDistribution;
    }

    /** Returns the number of terms recorded for the given status. */
    public int countFor(TermStatus status) {
        return statusDistribution.getOrDefault(status, 0);
    }
}
