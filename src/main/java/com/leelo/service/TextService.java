package com.leelo.service;

import com.leelo.dao.TextDAO;
import com.leelo.model.Text;
import java.util.List;

public class TextService {
    private TextDAO TextDAO = new TextDAO();

    public boolean addText(Text text) {
        return TextDAO.insertText(text);
    }

    public List<Text> listAllTexts() {
        return TextDAO.listAll();
    }

    public boolean deleteText(int idText) {
        return TextDAO.deleteText(idText);
    }

    public boolean updateText(Text text) {
        return TextDAO.updateText(text);
    }
} 