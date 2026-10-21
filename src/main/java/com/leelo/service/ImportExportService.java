package com.leelo.service;

import com.leelo.model.Language;
import com.leelo.model.Term;
import com.leelo.model.TermStatus;
import com.leelo.model.TermTag;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Import and export of the vocabulary.
 *
 * <p>The format is the CSV format of Lute, so that the word lists can be moved
 * between the two applications.
 */
public class ImportExportService {

    private static final List<String> REQUIRED_COLUMNS = List.of("language", "term");
    private static final Set<String> ALLOWED_COLUMNS = new HashSet<>(Arrays.asList(
            "language", "term", "translation", "parent", "status", "tags", "pronunciation", "link_status", "added"));

    private final TermService termService = new TermService();
    private final LanguageService languageService = new LanguageService();

    /** The options of an import. */
    public record ImportOptions(boolean createTerms, boolean updateTerms, boolean newAsUnknown) {
    }

    /** The outcome of an import. */
    public record ImportResult(int created, int updated, int skipped, List<String> errors) {
    }

    /**
     * Writes the given terms as a CSV file.
     */
    public void exportTerms(List<Term> terms, Writer writer) throws IOException {
        writer.write("term,parent,translation,language,tags,added,status,link_status,pronunciation\n");
        for (Term term : terms) {
            StringBuilder line = new StringBuilder();
            line.append(quote(plainText(term.getText()))).append(',');
            line.append(quote(parentText(term))).append(',');
            line.append(quote(term.getTranslation())).append(',');
            line.append(quote(term.getLanguageName())).append(',');
            line.append(quote(tagText(term))).append(',');
            line.append(quote(term.getCreatedAt() == null ? "" : term.getCreatedAt().toString())).append(',');
            line.append(quote(term.getStatus().getAbbreviation())).append(',');
            line.append(term.isSyncStatus() ? "y" : "n").append(',');
            line.append(quote(term.getRomanization()));
            writer.write(line.toString());
            writer.write("\n");
        }
        writer.flush();
    }

    /**
     * Reads the terms of a CSV file.
     *
     * @param input   the content of the file
     * @param options what to do with the terms that already exist
     */
    public ImportResult importTerms(InputStream input, ImportOptions options) throws IOException {
        List<String> errors = new ArrayList<>();
        int created = 0;
        int updated = 0;
        int skipped = 0;

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8))) {
            String headerLine = reader.readLine();
            if (headerLine == null) {
                return new ImportResult(0, 0, 0, List.of("The file is empty."));
            }
            String[] headers = headerLine.replace("﻿", "").split(",");
            List<String> columns = new ArrayList<>();
            for (String header : headers) {
                columns.add(header.trim().toLowerCase());
            }
            for (String required : REQUIRED_COLUMNS) {
                if (!columns.contains(required)) {
                    errors.add("The file must have a '" + required + "' column.");
                }
            }
            for (String column : columns) {
                if (!ALLOWED_COLUMNS.contains(column)) {
                    errors.add("Unknown field '" + column + "'");
                }
            }
            if (!errors.isEmpty()) {
                return new ImportResult(0, 0, 0, errors);
            }

            String line;
            int lineNumber = 1;
            Set<String> seen = new HashSet<>();
            while ((line = reader.readLine()) != null) {
                lineNumber++;
                if (line.isBlank()) {
                    continue;
                }
                String[] values = line.split(",", -1);
                if (values.length != columns.size()) {
                    errors.add("Line " + lineNumber + ": wrong number of values.");
                    continue;
                }

                String languageName = value(values, columns, "language").trim();
                String termText = value(values, columns, "term").trim();
                if (termText.isEmpty()) {
                    errors.add("Line " + lineNumber + ": term is required.");
                    continue;
                }
                Language language = languageService.getByName(languageName);
                if (language == null) {
                    errors.add("Line " + lineNumber + ": unknown language '" + languageName + "'.");
                    continue;
                }

                String statusText = value(values, columns, "status").trim();
                TermStatus status = statusText.isEmpty()
                        ? (options.newAsUnknown() ? TermStatus.UNKNOWN : TermStatus.NEW_1)
                        : TermStatus.fromAbbreviation(statusText);
                if (status == null) {
                    errors.add("Line " + lineNumber + ": status must be one of 1, 2, 3, 4, 5, I, W, or blank.");
                    continue;
                }

                String key = language.getName().toLowerCase() + "|" + termText.toLowerCase();
                if (!seen.add(key)) {
                    errors.add("Duplicate terms in import: " + termText);
                    continue;
                }

                Term term = termService.findOrCreate(language, termText);
                if (term.getId() > 0 && !options.updateTerms) {
                    skipped++;
                    continue;
                }
                if (term.getId() == 0 && !options.createTerms) {
                    skipped++;
                    continue;
                }

                term.setStatus(status);
                term.setTranslation(value(values, columns, "translation"));
                term.setRomanization(value(values, columns, "pronunciation"));
                term.setSyncStatus("y".equalsIgnoreCase(value(values, columns, "link_status").trim()));
                term.setTags(termService.resolveTags(splitList(value(values, columns, "tags"))));
                termService.save(term);

                if (term.getId() > 0) {
                    updated++;
                } else {
                    created++;
                }
            }
        }
        return new ImportResult(created, updated, skipped, errors);
    }

    /** Returns the value of a column, or an empty string when it is missing. */
    private String value(String[] values, List<String> columns, String column) {
        int index = columns.indexOf(column);
        if (index < 0 || index >= values.length) {
            return "";
        }
        String value = values[index];
        if (value.length() >= 2 && value.startsWith("\"") && value.endsWith("\"")) {
            return value.substring(1, value.length() - 1);
        }
        return value;
    }

    /** Splits a comma separated list. */
    private List<String> splitList(String value) {
        List<String> items = new ArrayList<>();
        if (value == null) {
            return items;
        }
        for (String item : value.split(",")) {
            if (!item.isBlank()) {
                items.add(item.trim());
            }
        }
        return items;
    }

    /** Removes the zero-width separators of a term. */
    private String plainText(String text) {
        return com.leelo.util.TextTokens.strip(text);
    }

    /** Returns the parents of a term as a comma separated list. */
    private String parentText(Term term) {
        List<String> parents = new ArrayList<>();
        for (Term parent : term.getParents()) {
            parents.add(plainText(parent.getText()));
        }
        return String.join(", ", parents);
    }

    /** Returns the tags of a term as a comma separated list. */
    private String tagText(Term term) {
        List<String> tags = new ArrayList<>();
        for (TermTag tag : term.getTags()) {
            tags.add(tag.getText());
        }
        return String.join(", ", tags);
    }

    /** Escapes a value so that it can be written in a CSV file. */
    private String quote(String value) {
        if (value == null) {
            return "";
        }
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }
}
