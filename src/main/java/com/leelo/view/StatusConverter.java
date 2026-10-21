package com.leelo.view;

import com.leelo.model.TermStatus;
import javafx.util.StringConverter;

/**
 * Shows the statuses with a readable Spanish label instead of the name of the
 * enumeration.
 */
public class StatusConverter extends StringConverter<TermStatus> {

    @Override
    public String toString(TermStatus status) {
        return status == null ? "" : labelFor(status);
    }

    @Override
    public TermStatus fromString(String value) {
        return TermStatus.fromAbbreviation(value);
    }

    /** The label shown in the interface for a status. */
    public static String labelFor(TermStatus status) {
        if (status == null) {
            return "";
        }
        return switch (status) {
            case UNKNOWN -> "Desconocida";
            case NEW_1 -> "Nueva 1";
            case NEW_2 -> "Nueva 2";
            case LEARNING_3 -> "Aprendiendo 3";
            case LEARNING_4 -> "Aprendiendo 4";
            case LEARNED -> "Aprendida";
            case IGNORED -> "Ignorada";
            case WELL_KNOWN -> "Bien conocida";
        };
    }
}
