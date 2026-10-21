package com.leelo.model;

/** A label that can be attached to one or more terms. */
public class TermTag {

    private int id;
    private String text = "";
    private String comment = "";
    private int termCount;

    public TermTag() {
    }

    public TermTag(String text, String comment) {
        this.text = text;
        this.comment = comment == null ? "" : comment;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment == null ? "" : comment;
    }

    /** Number of terms carrying this tag; only populated by listing queries. */
    public int getTermCount() {
        return termCount;
    }

    public void setTermCount(int termCount) {
        this.termCount = termCount;
    }

    @Override
    public String toString() {
        return text;
    }
}
