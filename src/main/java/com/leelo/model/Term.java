package com.leelo.model;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * A vocabulary item.
 *
 * <p>The term text is stored as the sequence of its tokens joined by a
 * zero-width space, which makes multi-word terms a first class citizen: the
 * token boundaries of the original text are preserved and the term can be
 * matched against a text item-for-item.
 *
 * <p>{@code textLc} holds the lower-cased counterpart and is what makes a term
 * unique inside a language.
 */
public class Term {

    private int id;
    private int languageId;
    private String languageName = "";
    private String text = "";
    private String textLc = "";
    private TermStatus status = TermStatus.UNKNOWN;
    private String translation = "";
    private String romanization = "";
    private int tokenCount = 1;
    private boolean syncStatus;
    private Instant createdAt;
    private Instant statusChangedAt;
    private List<TermTag> tags = new ArrayList<>();
    private List<Term> parents = new ArrayList<>();
    private TermImage image;
    private String flashMessage = "";

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getLanguageId() {
        return languageId;
    }

    public void setLanguageId(int languageId) {
        this.languageId = languageId;
    }

    public String getLanguageName() {
        return languageName;
    }

    public void setLanguageName(String languageName) {
        this.languageName = languageName;
    }

    /** The raw term text, with tokens separated by zero-width spaces. */
    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text == null ? "" : text;
    }

    /** The lower-cased term text, with tokens separated by zero-width spaces. */
    public String getTextLc() {
        return textLc;
    }

    public void setTextLc(String textLc) {
        this.textLc = textLc == null ? "" : textLc;
    }

    public TermStatus getStatus() {
        return status;
    }

    public void setStatus(TermStatus status) {
        this.status = status == null ? TermStatus.UNKNOWN : status;
    }

    public String getTranslation() {
        return translation;
    }

    public void setTranslation(String translation) {
        this.translation = translation == null ? "" : translation;
    }

    public String getRomanization() {
        return romanization;
    }

    public void setRomanization(String romanization) {
        this.romanization = romanization == null ? "" : romanization;
    }

    /** Number of tokens the term is made of; terms with more than one are "multi-word". */
    public int getTokenCount() {
        return tokenCount;
    }

    public void setTokenCount(int tokenCount) {
        this.tokenCount = tokenCount;
    }

    /** True when the term inherits its status from its (single) parent. */
    public boolean isSyncStatus() {
        return syncStatus;
    }

    public void setSyncStatus(boolean syncStatus) {
        this.syncStatus = syncStatus;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getStatusChangedAt() {
        return statusChangedAt;
    }

    public void setStatusChangedAt(Instant statusChangedAt) {
        this.statusChangedAt = statusChangedAt;
    }

    public List<TermTag> getTags() {
        return tags;
    }

    public void setTags(List<TermTag> tags) {
        this.tags = tags == null ? new ArrayList<>() : tags;
    }

    public List<Term> getParents() {
        return parents;
    }

    public void setParents(List<Term> parents) {
        this.parents = parents == null ? new ArrayList<>() : parents;
    }

    public TermImage getImage() {
        return image;
    }

    public void setImage(TermImage image) {
        this.image = image;
    }

    public String getFlashMessage() {
        return flashMessage;
    }

    public void setFlashMessage(String flashMessage) {
        this.flashMessage = flashMessage == null ? "" : flashMessage;
    }

    /** True when the term spans more than one token. */
    public boolean isMultiWord() {
        return tokenCount > 1;
    }

    /** The term text without the zero-width separators, for display purposes. */
    public String getDisplayText() {
        return com.leelo.util.TextTokens.strip(text);
    }

    /** The label of the status of the term. */
    public String getStatusLabel() {
        return status.getLabel();
    }

    /** The tags of the term as a single comma separated string. */
    public String getTagList() {
        List<String> names = new ArrayList<>();
        for (TermTag tag : tags) {
            names.add(tag.getText());
        }
        return String.join(", ", names);
    }

    /** The parents of the term as a single comma separated string. */
    public String getParentList() {
        List<String> names = new ArrayList<>();
        for (Term parent : parents) {
            names.add(com.leelo.util.TextTokens.strip(parent.getText()));
        }
        return String.join(", ", names);
    }

    @Override
    public String toString() {
        return getDisplayText();
    }
}
