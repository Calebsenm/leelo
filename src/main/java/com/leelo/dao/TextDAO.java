package com.leelo.dao;

import com.leelo.model.Texts;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class TextDAO {
    public boolean insertText(Texts text) {
        String sql = "INSERT INTO texts(tittle, text, creation_date) VALUES (?, ?, datetime('now'))";
        try (Connection conn = Database.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, text.getTittle());
            pstmt.setString(2, text.getText());
            pstmt.executeUpdate();
            return true;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }
    
    public boolean saveProgress(int id_book , int page){
        String sql = "INSERT INTO progress(id_book ,page_book) VALUES(? , ? )" ;
        String update = "";

        try (Connection conn = Database.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)){
            
            pstmt.setInt(1, id_book); 
            pstmt.setInt(2, page);
            
            pstmt.executeUpdate(); 
            return true; 

        }   catch (SQLException e){
            e.printStackTrace();
            return false; 
        }
    }
    
    public boolean updateProgress(int id_book , int page ) {
        // Eliminar registros antiguos del mismo libro
        String deleteSql = "DELETE FROM progress WHERE id_book = ?";
        try (Connection conn = Database.getConnection(); 
             PreparedStatement deleteStmt = conn.prepareStatement(deleteSql)) {
            deleteStmt.setInt(1, id_book);
            deleteStmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        
        // Insertar nuevo registro (esto crea un nuevo id_progress más alto)
        return saveProgress(id_book, page);
    }
    
    public int getPage(int id_book){
        
        int page = 0; 
        String sql = "SELECT page_book FROM progress WHERE  id_book = ?";

        try (Connection conn = Database.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, id_book);
            ResultSet rs = pstmt.executeQuery();
            page = rs.getInt("page_book");
    
        } catch (SQLException e) {
            e.printStackTrace();
        }   
        
        return page; 
    }

    public List<Texts> listAll() {
        List<Texts> texts = new ArrayList<>();
        String sql = "SELECT * FROM texts ORDER BY creation_date DESC";
        try (Connection conn = Database.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                Texts text = new Texts();
                text.setIdText(rs.getInt("id_text"));
                text.setTittle(rs.getString("tittle"));
                text.setText(rs.getString("text"));
                text.setCreationDate(rs.getString("creation_date"));
                texts.add(text);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return texts;
    }

    public boolean deleteText(int idText) {
        String sql = "DELETE FROM texts WHERE id_text = ?";
        try (Connection conn = Database.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, idText);
            pstmt.executeUpdate();
            return true;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean updateText(Texts text) {
        String sql = "UPDATE texts SET tittle = ?, text = ? WHERE id_text = ?";
        try (Connection conn = Database.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, text.getTittle());
            pstmt.setString(2, text.getText());
            pstmt.setInt(3, text.getIdText());
            pstmt.executeUpdate();
            return true;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }
    
    public Texts getLastReadBook() {
        String sql = "SELECT t.* FROM texts t " +
                     "INNER JOIN progress p ON t.id_text = p.id_book " +
                     "ORDER BY p.id_progress DESC LIMIT 1";
        
        try (Connection conn = Database.getConnection(); 
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                Texts text = new Texts();
                text.setIdText(rs.getInt("id_text"));
                text.setTittle(rs.getString("tittle"));
                text.setText(rs.getString("text"));
                text.setCreationDate(rs.getString("creation_date"));
                return text;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }
} 
