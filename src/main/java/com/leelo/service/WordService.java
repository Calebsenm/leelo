package com.leelo.service;

import com.leelo.dao.WordDAO;
import com.leelo.model.Word;
import java.util.List;

public class WordService {
    private WordDAO WordDAO = new WordDAO();

    public boolean addWord(Word word) {
        return WordDAO.insertWord(word);
    }

    public List<Word> listWords() {
        return WordDAO.listAll();
    }

    public boolean updateWord(Word word) {
        return WordDAO.updateWord(word);
    }

    public boolean deleteWord(int idTerm) {
        return WordDAO.deleteWord(idTerm);
    }
} 