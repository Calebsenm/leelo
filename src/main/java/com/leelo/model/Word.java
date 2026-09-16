package com.leelo.model;

public class Word {
    private int idTerm;
    private String term;
    private String translation;
    private String pronunciation;
    private int state;
    private String urlImg;

    public int getIdTerm() {
        return idTerm;
    }
    public void setIdTerm(int idTerm) {
        this.idTerm = idTerm;
    }
    public String getTerm() {
        return term;
    }
    public void setTerm(String term) {
        this.term = term;
    }
    public String getTranslation() {
        return translation;
    }
    public void setTranslation(String translation) {
        this.translation = translation;
    }
    public String getPronunciation() {
        return pronunciation;
    }
    public void setPronunciation(String pronunciation) {
        this.pronunciation = pronunciation;
    }
    public int getState() {
        return state;
    }
    public void setState(int state) {
        this.state = state;
    }
    public String getUrlImg() {
        return urlImg;
    }
    public void setUrlImg(String urlImg) {
        this.urlImg = urlImg;
    }
}