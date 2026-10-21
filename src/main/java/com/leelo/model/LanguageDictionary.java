package com.leelo.model;

/**
 * A dictionary attached to a {@link Language}.
 *
 * <p>Dictionaries are used to look up either individual terms or whole
 * sentences. The URI may contain the {@code [LUTE]} placeholder, which is
 * replaced by the URL-encoded text being looked up.
 */
public class LanguageDictionary {

    /** What the dictionary is queried with. */
    public enum UseFor {
        TERMS("terms"),
        SENTENCES("sentences");

        private final String code;

        UseFor(String code) {
            this.code = code;
        }

        public String getCode() {
            return code;
        }

        public static UseFor fromCode(String code) {
            for (UseFor value : values()) {
                if (value.code.equalsIgnoreCase(code)) {
                    return value;
                }
            }
            return TERMS;
        }
    }

    /** How the dictionary result is displayed. */
    public enum DictType {
        EMBEDDED_HTML("embeddedhtml"),
        POPUP_HTML("popuphtml");

        private final String code;

        DictType(String code) {
            this.code = code;
        }

        public String getCode() {
            return code;
        }

        public static DictType fromCode(String code) {
            for (DictType value : values()) {
                if (value.code.equalsIgnoreCase(code)) {
                    return value;
                }
            }
            return EMBEDDED_HTML;
        }
    }

    private int id;
    private int languageId;
    private UseFor useFor = UseFor.TERMS;
    private DictType dictType = DictType.EMBEDDED_HTML;
    private String dictUri = "";
    private boolean active = true;
    private int sortOrder;

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

    public UseFor getUseFor() {
        return useFor;
    }

    public void setUseFor(UseFor useFor) {
        this.useFor = useFor;
    }

    public DictType getDictType() {
        return dictType;
    }

    public void setDictType(DictType dictType) {
        this.dictType = dictType;
    }

    public String getDictUri() {
        return dictUri;
    }

    public void setDictUri(String dictUri) {
        this.dictUri = dictUri;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public int getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(int sortOrder) {
        this.sortOrder = sortOrder;
    }
}
