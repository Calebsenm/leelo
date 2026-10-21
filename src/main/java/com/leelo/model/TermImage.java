package com.leelo.model;

/** An image attached to a term, used as a visual cue while reading. */
public class TermImage {

    private int id;
    private int termId;
    private String source = "";

    public TermImage() {
    }

    public TermImage(int termId, String source) {
        this.termId = termId;
        this.source = source == null ? "" : source;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getTermId() {
        return termId;
    }

    public void setTermId(int termId) {
        this.termId = termId;
    }

    /** File name of the image, relative to the term image directory. */
    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source == null ? "" : source;
    }
}
