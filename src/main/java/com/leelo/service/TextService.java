package com.leelo.service;

import com.leelo.dao.TextDAO;
import com.leelo.model.Text;
import java.util.List;

public class TextService {
    private TextDAO TextDAO = new TextDAO();

    public boolean addText(Text text) {
        return TextDAO.insertText(text);
    }

    public boolean savePage(int id_text , int page ){
        return TextDAO.saveProgress(id_text , page); 
    }

    public boolean updateProgress(int id_text , int page){
        return TextDAO.updateProgress(id_text , page ); 
    }

    public int  getPage(int id_book){
        return TextDAO.getPage(id_book); 
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
    
    public Text getLastReadBook() {
        return TextDAO.getLastReadBook();
    }
} 
