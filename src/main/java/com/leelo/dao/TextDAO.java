package com.leelo.dao;

import com.leelo.model.Text;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class TextDAO {
    public boolean insertText(Text text) {
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

    public List<Text> listAll() {
        List<Text> texts = new ArrayList<>();
        String sql = "SELECT * FROM texts ORDER BY creation_date DESC";
        try (Connection conn = Database.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                Text text = new Text();
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

    public boolean updateText(Text text) {
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
} 