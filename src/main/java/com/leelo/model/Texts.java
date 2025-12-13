package com.leelo.model;

public class Texts {
    private int idText;
    private String tittle;
    private String text;
    private String creationDate;

    public int getIdText() {
        return idText;
    }
    public void setIdText(int idText) {
        this.idText = idText;
    }
    public String getTittle() {
        return tittle;
    }
    public void setTittle(String tittle) {
        this.tittle = tittle;
    }
    public String getText() {
        return text;
    }
    public void setText(String text) {
        this.text = text;
    }
    public String getCreationDate() {
        return creationDate;
    }
    public void setCreationDate(String creationDate) {
        this.creationDate = creationDate;
    }
    
    @Override
    public String toString() {
        return "Text (ID: " + idText + ", Title: " + tittle + ")";
    }
} 
