package com.leelo.model;

/** A label that can be attached to one or more books. */
public class BookTag {

    private int id;
    private String text = "";
    private String comment = "";

    public BookTag() {
    }

    public BookTag(String text, String comment) {
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

    @Override
    public String toString() {
        return text;
    }
}
