package com.leelo.dao;

import com.leelo.model.Language;
import com.leelo.model.LanguageDictionary;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/** Persistence of the languages and their dictionaries. */
public class LanguageDAO {

    /** Returns every language, ordered by name, with their dictionaries. */
    public List<Language> findAll() {
        List<Language> languages = new ArrayList<>();
        String sql = "SELECT * FROM languages ORDER BY name COLLATE NOCASE";
        try (Connection connection = Database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet rs = statement.executeQuery()) {
            while (rs.next()) {
                languages.add(readLanguage(rs));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        for (Language language : languages) {
            language.setDictionaries(findDictionaries(language.getId()));
        }
        return languages;
    }

    /** Returns the language with the given id, or {@code null}. */
    public Language findById(int id) {
        String sql = "SELECT * FROM languages WHERE id = ?";
        try (Connection connection = Database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, id);
            try (ResultSet rs = statement.executeQuery()) {
                if (rs.next()) {
                    Language language = readLanguage(rs);
                    language.setDictionaries(findDictionaries(id));
                    return language;
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    /** Returns the language with the given name, ignoring the case, or {@code null}. */
    public Language findByName(String name) {
        String sql = "SELECT * FROM languages WHERE name = ? COLLATE NOCASE";
        try (Connection connection = Database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, name == null ? "" : name);
            try (ResultSet rs = statement.executeQuery()) {
                if (rs.next()) {
                    Language language = readLanguage(rs);
                    language.setDictionaries(findDictionaries(language.getId()));
                    return language;
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    /** Inserts a language and returns its id, or zero on failure. */
    public int insert(Language language) {
        String sql = "INSERT INTO languages(name, parser_type, character_substitutions, "
                + "regexp_split_sentences, exceptions_split_sentences, word_characters, "
                + "right_to_left, show_romanization) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection connection = Database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setString(1, language.getName());
            statement.setString(2, language.getParserType());
            statement.setString(3, language.getCharacterSubstitutions());
            statement.setString(4, language.getRegexpSplitSentences());
            statement.setString(5, language.getExceptionsSplitSentences());
            statement.setString(6, language.getWordCharacters());
            statement.setInt(7, language.isRightToLeft() ? 1 : 0);
            statement.setInt(8, language.isShowRomanization() ? 1 : 0);
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (keys.next()) {
                    int id = keys.getInt(1);
                    language.setId(id);
                    saveDictionaries(language);
                    return id;
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }

    /** Updates an existing language. */
    public boolean update(Language language) {
        String sql = "UPDATE languages SET name = ?, parser_type = ?, character_substitutions = ?, "
                + "regexp_split_sentences = ?, exceptions_split_sentences = ?, word_characters = ?, "
                + "right_to_left = ?, show_romanization = ? WHERE id = ?";
        try (Connection connection = Database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, language.getName());
            statement.setString(2, language.getParserType());
            statement.setString(3, language.getCharacterSubstitutions());
            statement.setString(4, language.getRegexpSplitSentences());
            statement.setString(5, language.getExceptionsSplitSentences());
            statement.setString(6, language.getWordCharacters());
            statement.setInt(7, language.isRightToLeft() ? 1 : 0);
            statement.setInt(8, language.isShowRomanization() ? 1 : 0);
            statement.setInt(9, language.getId());
            statement.executeUpdate();
            saveDictionaries(language);
            return true;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    /** Deletes a language and everything attached to it. */
    public boolean delete(int id) {
        String sql = "DELETE FROM languages WHERE id = ?";
        try (Connection connection = Database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, id);
            statement.executeUpdate();
            return true;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    /** Returns the dictionaries of a language, ordered by their position. */
    public List<LanguageDictionary> findDictionaries(int languageId) {
        List<LanguageDictionary> dictionaries = new ArrayList<>();
        String sql = "SELECT * FROM language_dicts WHERE language_id = ? ORDER BY sort_order, id";
        try (Connection connection = Database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, languageId);
            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    LanguageDictionary dictionary = new LanguageDictionary();
                    dictionary.setId(rs.getInt("id"));
                    dictionary.setLanguageId(rs.getInt("language_id"));
                    dictionary.setUseFor(LanguageDictionary.UseFor.fromCode(rs.getString("use_for")));
                    dictionary.setDictType(LanguageDictionary.DictType.fromCode(rs.getString("dict_type")));
                    dictionary.setDictUri(rs.getString("dict_uri"));
                    dictionary.setActive(rs.getInt("is_active") != 0);
                    dictionary.setSortOrder(rs.getInt("sort_order"));
                    dictionaries.add(dictionary);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return dictionaries;
    }

    /** Replaces the dictionaries of a language with the given ones. */
    private void saveDictionaries(Language language) {
        String deleteSql = "DELETE FROM language_dicts WHERE language_id = ?";
        String insertSql = "INSERT INTO language_dicts(language_id, use_for, dict_type, dict_uri, "
                + "is_active, sort_order) VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection connection = Database.getConnection()) {
            try (PreparedStatement statement = connection.prepareStatement(deleteSql)) {
                statement.setInt(1, language.getId());
                statement.executeUpdate();
            }
            try (PreparedStatement statement = connection.prepareStatement(insertSql)) {
                int order = 0;
                for (LanguageDictionary dictionary : language.getDictionaries()) {
                    statement.setInt(1, language.getId());
                    statement.setString(2, dictionary.getUseFor().getCode());
                    statement.setString(3, dictionary.getDictType().getCode());
                    statement.setString(4, dictionary.getDictUri());
                    statement.setInt(5, dictionary.isActive() ? 1 : 0);
                    statement.setInt(6, dictionary.getSortOrder() > 0 ? dictionary.getSortOrder() : order);
                    statement.addBatch();
                    order++;
                }
                statement.executeBatch();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private Language readLanguage(ResultSet rs) throws SQLException {
        Language language = new Language();
        language.setId(rs.getInt("id"));
        language.setName(rs.getString("name"));
        language.setParserType(rs.getString("parser_type"));
        language.setCharacterSubstitutions(rs.getString("character_substitutions"));
        language.setRegexpSplitSentences(rs.getString("regexp_split_sentences"));
        language.setExceptionsSplitSentences(rs.getString("exceptions_split_sentences"));
        language.setWordCharacters(rs.getString("word_characters"));
        language.setRightToLeft(rs.getInt("right_to_left") != 0);
        language.setShowRomanization(rs.getInt("show_romanization") != 0);
        return language;
    }
}
