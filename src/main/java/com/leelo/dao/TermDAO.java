package com.leelo.dao;

import com.leelo.model.Term;
import com.leelo.model.TermImage;
import com.leelo.model.TermStatus;
import com.leelo.model.TermTag;
import com.leelo.util.Timestamps;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Persistence of the terms, their tags, their parents and their images. */
public class TermDAO {

    /** Returns the term with the given id, fully populated, or {@code null}. */
    public Term findById(int id) {
        List<Term> terms = findByIds(List.of(id));
        return terms.isEmpty() ? null : terms.get(0);
    }

    /** Returns the given terms, fully populated. */
    public List<Term> findByIds(List<Integer> ids) {
        List<Term> terms = new ArrayList<>();
        if (ids == null || ids.isEmpty()) {
            return terms;
        }
        String sql = "SELECT t.*, l.name AS language_name FROM terms t "
                + "JOIN languages l ON l.id = t.language_id WHERE t.id IN (" + placeholders(ids.size()) + ")";
        try (Connection connection = Database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            bindIds(statement, ids);
            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    terms.add(readTerm(rs));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        populateRelations(terms);
        return terms;
    }

    /** Returns every term of a language, or of every language when the id is zero. */
    public List<Term> findAll(int languageId) {
        List<Term> terms = new ArrayList<>();
        StringBuilder sql = new StringBuilder(
                "SELECT t.*, l.name AS language_name FROM terms t "
                        + "JOIN languages l ON l.id = t.language_id");
        if (languageId > 0) {
            sql.append(" WHERE t.language_id = ?");
        }
        sql.append(" ORDER BY t.text COLLATE NOCASE");
        try (Connection connection = Database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql.toString())) {
            if (languageId > 0) {
                statement.setInt(1, languageId);
            }
            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    terms.add(readTerm(rs));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        populateRelations(terms);
        return terms;
    }

    /** Returns the term of a language with the given lower-cased text, or {@code null}. */
    public Term findByTextLc(int languageId, String textLc) {
        String sql = "SELECT t.*, l.name AS language_name FROM terms t "
                + "JOIN languages l ON l.id = t.language_id "
                + "WHERE t.language_id = ? AND t.text_lc = ?";
        try (Connection connection = Database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, languageId);
            statement.setString(2, textLc);
            try (ResultSet rs = statement.executeQuery()) {
                if (rs.next()) {
                    Term term = readTerm(rs);
                    populateRelations(List.of(term));
                    return term;
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Returns the terms of a language whose lower-cased text is one of the
     * given ones.
     */
    public List<Term> findByTextLcs(int languageId, java.util.Collection<String> textLcs) {
        List<Term> terms = new ArrayList<>();
        if (textLcs == null || textLcs.isEmpty()) {
            return terms;
        }
        List<String> values = new ArrayList<>(textLcs);
        String sql = "SELECT t.*, l.name AS language_name FROM terms t "
                + "JOIN languages l ON l.id = t.language_id "
                + "WHERE t.language_id = ? AND t.text_lc IN (" + placeholders(values.size()) + ")";
        try (Connection connection = Database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, languageId);
            for (int i = 0; i < values.size(); i++) {
                statement.setString(i + 2, values.get(i));
            }
            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    terms.add(readTerm(rs));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        populateRelations(terms);
        return terms;
    }

    /**
     * Returns the lower-cased texts of the multi-word terms of a language, which
     * is what the multi-word indexer is built from.
     */
    public List<String> findMultiWordTexts(int languageId) {
        List<String> texts = new ArrayList<>();
        String sql = "SELECT text_lc FROM terms WHERE language_id = ? AND token_count > 1";
        try (Connection connection = Database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, languageId);
            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    texts.add(rs.getString("text_lc"));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return texts;
    }

    /** Returns the terms of a language whose text contains the given query. */
    public List<Term> search(int languageId, String query, int limit) {
        List<Term> terms = new ArrayList<>();
        String sql = "SELECT t.*, l.name AS language_name FROM terms t "
                + "JOIN languages l ON l.id = t.language_id "
                + "WHERE t.text_lc LIKE ? ";
        if (languageId > 0) {
            sql += "AND t.language_id = ? ";
        }
        sql += "ORDER BY t.text COLLATE NOCASE LIMIT ?";
        try (Connection connection = Database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            int parameter = 1;
            statement.setString(parameter++, "%" + query.toLowerCase() + "%");
            if (languageId > 0) {
                statement.setInt(parameter++, languageId);
            }
            statement.setInt(parameter, limit);
            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    terms.add(readTerm(rs));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        populateRelations(terms);
        return terms;
    }

    /** Inserts a term and returns its id, or zero on failure. */
    public int insert(Term term) {
        String sql = "INSERT INTO terms(language_id, text, text_lc, status, translation, romanization, "
                + "token_count, sync_status, created_at, status_changed_at) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection connection = Database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            Instant now = Instant.now();
            statement.setInt(1, term.getLanguageId());
            statement.setString(2, term.getText());
            statement.setString(3, term.getTextLc());
            statement.setInt(4, term.getStatus().getCode());
            statement.setString(5, term.getTranslation());
            statement.setString(6, term.getRomanization());
            statement.setInt(7, term.getTokenCount());
            statement.setInt(8, term.isSyncStatus() ? 1 : 0);
            statement.setString(9, Timestamps.format(now));
            statement.setString(10, Timestamps.format(now));
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (keys.next()) {
                    int id = keys.getInt(1);
                    term.setId(id);
                    saveRelations(term);
                    return id;
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }

    /** Updates a term. */
    public boolean update(Term term) {
        String sql = "UPDATE terms SET text = ?, text_lc = ?, status = ?, translation = ?, "
                + "romanization = ?, token_count = ?, sync_status = ? WHERE id = ?";
        try (Connection connection = Database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, term.getText());
            statement.setString(2, term.getTextLc());
            statement.setInt(3, term.getStatus().getCode());
            statement.setString(4, term.getTranslation());
            statement.setString(5, term.getRomanization());
            statement.setInt(6, term.getTokenCount());
            statement.setInt(7, term.isSyncStatus() ? 1 : 0);
            statement.setInt(8, term.getId());
            statement.executeUpdate();
            saveRelations(term);
            return true;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    /** Updates the status of a term, recording the moment of the change. */
    public boolean setStatus(int termId, TermStatus status) {
        String sql = "UPDATE terms SET status = ?, status_changed_at = ? WHERE id = ?";
        try (Connection connection = Database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, status.getCode());
            statement.setString(2, Timestamps.now());
            statement.setInt(3, termId);
            statement.executeUpdate();
            return true;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    /** Deletes a term. */
    public boolean delete(int termId) {
        String sql = "DELETE FROM terms WHERE id = ?";
        try (Connection connection = Database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, termId);
            statement.executeUpdate();
            return true;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    /** Stores the image of a term, replacing the previous one. */
    public boolean saveImage(int termId, String source) {
        String deleteSql = "DELETE FROM term_images WHERE term_id = ?";
        String insertSql = "INSERT INTO term_images(term_id, source) VALUES (?, ?)";
        try (Connection connection = Database.getConnection()) {
            try (PreparedStatement statement = connection.prepareStatement(deleteSql)) {
                statement.setInt(1, termId);
                statement.executeUpdate();
            }
            if (source != null && !source.isBlank()) {
                try (PreparedStatement statement = connection.prepareStatement(insertSql)) {
                    statement.setInt(1, termId);
                    statement.setString(2, source);
                    statement.executeUpdate();
                }
            }
            return true;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    /** Stores the flash message of a term, replacing the previous one. */
    public boolean saveFlashMessage(int termId, String message) {
        String deleteSql = "DELETE FROM term_flash_messages WHERE term_id = ?";
        String insertSql = "INSERT INTO term_flash_messages(term_id, message) VALUES (?, ?)";
        try (Connection connection = Database.getConnection()) {
            try (PreparedStatement statement = connection.prepareStatement(deleteSql)) {
                statement.setInt(1, termId);
                statement.executeUpdate();
            }
            if (message != null && !message.isBlank()) {
                try (PreparedStatement statement = connection.prepareStatement(insertSql)) {
                    statement.setInt(1, termId);
                    statement.setString(2, message);
                    statement.executeUpdate();
                }
            }
            return true;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    /** Loads the tags, the parents, the image and the flash message of the terms. */
    private void populateRelations(List<Term> terms) {
        if (terms.isEmpty()) {
            return;
        }
        List<Integer> ids = new ArrayList<>();
        for (Term term : terms) {
            ids.add(term.getId());
        }
        Map<Integer, List<TermTag>> tags = loadTags(ids);
        Map<Integer, List<Term>> parents = loadParents(ids);
        Map<Integer, TermImage> images = loadImages(ids);
        Map<Integer, String> flashMessages = loadFlashMessages(ids);

        for (Term term : terms) {
            term.setTags(tags.getOrDefault(term.getId(), new ArrayList<>()));
            term.setParents(parents.getOrDefault(term.getId(), new ArrayList<>()));
            term.setImage(images.get(term.getId()));
            term.setFlashMessage(flashMessages.getOrDefault(term.getId(), ""));
        }
    }

    private Map<Integer, List<TermTag>> loadTags(List<Integer> termIds) {
        Map<Integer, List<TermTag>> result = new HashMap<>();
        String sql = "SELECT l.term_id, t.* FROM term_tags t "
                + "JOIN term_tag_links l ON l.tag_id = t.id WHERE l.term_id IN ("
                + placeholders(termIds.size()) + ") ORDER BY t.text COLLATE NOCASE";
        try (Connection connection = Database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            bindIds(statement, termIds);
            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    TermTag tag = new TermTag();
                    tag.setId(rs.getInt("id"));
                    tag.setText(rs.getString("text"));
                    tag.setComment(rs.getString("comment"));
                    result.computeIfAbsent(rs.getInt("term_id"), key -> new ArrayList<>()).add(tag);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return result;
    }

    private Map<Integer, List<Term>> loadParents(List<Integer> termIds) {
        Map<Integer, List<Term>> result = new LinkedHashMap<>();
        String sql = "SELECT l.term_id, p.* FROM term_parents l "
                + "JOIN terms p ON p.id = l.parent_id WHERE l.term_id IN ("
                + placeholders(termIds.size()) + ") ORDER BY p.text COLLATE NOCASE";
        try (Connection connection = Database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            bindIds(statement, termIds);
            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    Term parent = new Term();
                    parent.setId(rs.getInt("id"));
                    parent.setLanguageId(rs.getInt("language_id"));
                    parent.setText(rs.getString("text"));
                    parent.setTextLc(rs.getString("text_lc"));
                    parent.setStatus(TermStatus.fromCode(rs.getInt("status")));
                    parent.setTranslation(rs.getString("translation"));
                    parent.setRomanization(rs.getString("romanization"));
                    parent.setTokenCount(rs.getInt("token_count"));
                    result.computeIfAbsent(rs.getInt("term_id"), key -> new ArrayList<>()).add(parent);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return result;
    }

    private Map<Integer, TermImage> loadImages(List<Integer> termIds) {
        Map<Integer, TermImage> result = new HashMap<>();
        String sql = "SELECT * FROM term_images WHERE term_id IN (" + placeholders(termIds.size()) + ")";
        try (Connection connection = Database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            bindIds(statement, termIds);
            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    TermImage image = new TermImage();
                    image.setId(rs.getInt("id"));
                    image.setTermId(rs.getInt("term_id"));
                    image.setSource(rs.getString("source"));
                    result.put(image.getTermId(), image);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return result;
    }

    private Map<Integer, String> loadFlashMessages(List<Integer> termIds) {
        Map<Integer, String> result = new HashMap<>();
        String sql = "SELECT * FROM term_flash_messages WHERE term_id IN (" + placeholders(termIds.size()) + ")";
        try (Connection connection = Database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            bindIds(statement, termIds);
            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    result.put(rs.getInt("term_id"), rs.getString("message"));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return result;
    }

    /** Replaces the tags and the parents of a term. */
    private void saveRelations(Term term) {
        if (term.getId() <= 0) {
            return;
        }
        try (Connection connection = Database.getConnection()) {
            try (PreparedStatement statement = connection.prepareStatement(
                    "DELETE FROM term_tag_links WHERE term_id = ?")) {
                statement.setInt(1, term.getId());
                statement.executeUpdate();
            }
            try (PreparedStatement statement = connection.prepareStatement(
                    "INSERT INTO term_tag_links(term_id, tag_id) VALUES (?, ?)")) {
                for (TermTag tag : term.getTags()) {
                    statement.setInt(1, term.getId());
                    statement.setInt(2, tag.getId());
                    statement.addBatch();
                }
                statement.executeBatch();
            }
            try (PreparedStatement statement = connection.prepareStatement(
                    "DELETE FROM term_parents WHERE term_id = ?")) {
                statement.setInt(1, term.getId());
                statement.executeUpdate();
            }
            try (PreparedStatement statement = connection.prepareStatement(
                    "INSERT INTO term_parents(term_id, parent_id) VALUES (?, ?)")) {
                for (Term parent : term.getParents()) {
                    statement.setInt(1, term.getId());
                    statement.setInt(2, parent.getId());
                    statement.addBatch();
                }
                statement.executeBatch();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private Term readTerm(ResultSet rs) throws SQLException {
        Term term = new Term();
        term.setId(rs.getInt("id"));
        term.setLanguageId(rs.getInt("language_id"));
        try {
            term.setLanguageName(rs.getString("language_name"));
        } catch (SQLException ignored) {
            // The column is not part of every query.
        }
        term.setText(rs.getString("text"));
        term.setTextLc(rs.getString("text_lc"));
        term.setStatus(TermStatus.fromCode(rs.getInt("status")));
        term.setTranslation(rs.getString("translation"));
        term.setRomanization(rs.getString("romanization"));
        term.setTokenCount(rs.getInt("token_count"));
        term.setSyncStatus(rs.getInt("sync_status") != 0);
        term.setCreatedAt(Timestamps.parse(rs.getString("created_at")));
        term.setStatusChangedAt(Timestamps.parse(rs.getString("status_changed_at")));
        return term;
    }

    private static String placeholders(int count) {
        return String.join(",", java.util.Collections.nCopies(count, "?"));
    }

    private static void bindIds(PreparedStatement statement, List<Integer> ids) throws SQLException {
        int index = 1;
        for (Integer id : ids) {
            statement.setInt(index++, id);
        }
    }
}
