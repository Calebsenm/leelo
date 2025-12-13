package com.leelo.viewmodel;

import com.leelo.model.Texts;
import com.leelo.service.TextService;
import javafx.beans.property.*;

public class HomeViewModel {
    private final TextService textService;
    
    private final ObjectProperty<Texts> lastOpenedBook = new SimpleObjectProperty<>();
    private final IntegerProperty readingProgress = new SimpleIntegerProperty(0);
    private final StringProperty bookTitle = new SimpleStringProperty("Sin libro");
    private final BooleanProperty hasBook = new SimpleBooleanProperty(false);
    private final IntegerProperty wordCount = new SimpleIntegerProperty(0);
    
    public HomeViewModel() {
        this.textService = new TextService();
        loadLastOpenedBook();
    }
    
    private void loadLastOpenedBook() {
        // Primero intentar obtener el último libro leído desde la tabla progress
        Texts lastBook = textService.getLastReadBook();
        
        // Si no hay progreso registrado, obtener el último libro creado
        if (lastBook == null) {
            var texts = textService.listAllTexts();
            if (!texts.isEmpty()) {
                lastBook = texts.get(0);
            }
        }
        
        if (lastBook != null) {
            lastOpenedBook.set(lastBook);
            
            // Usar título o valor por defecto si es null
            String title = lastBook.getTittle();
            if (title == null || title.trim().isEmpty()) {
                title = "Libro sin título";
            }
            bookTitle.set(title);
            hasBook.set(true);
            
            int currentPage = textService.getPage(lastBook.getIdText());
            
            String textContent = lastBook.getText();
            if (textContent == null || textContent.trim().isEmpty()) {
                readingProgress.set(0);
                return;
            }
            
            String[] words = textContent.split("\\s+");
            int totalWords = words.length;
            
            // Establecer el conteo de palabras
            wordCount.set(totalWords);
            
            // Calcular progreso basado en palabras por página (400 palabras por página)
            int wordsPerPage = 400;
            int totalPages = (totalWords + wordsPerPage - 1) / wordsPerPage;
            int progress = totalPages > 0 ? (currentPage * 100) / totalPages : 0;
            
            readingProgress.set(Math.min(progress, 100));
        } else {
            hasBook.set(false);
            bookTitle.set("Sin libro");
            readingProgress.set(0);
            wordCount.set(0);
        }
    }
    

    
    public ObjectProperty<Texts> lastOpenedBookProperty() {
        return lastOpenedBook;
    }
    
    public IntegerProperty readingProgressProperty() {
        return readingProgress;
    }
    
    public StringProperty bookTitleProperty() {
        return bookTitle;
    }
    
    public BooleanProperty hasBookProperty() {
        return hasBook;
    }
    
    public Texts getLastOpenedBook() {
        return lastOpenedBook.get();
    }
    
    public int getReadingProgress() {
        return readingProgress.get();
    }
    
    public String getBookTitle() {
        return bookTitle.get();
    }
    
    public boolean hasBook() {
        return hasBook.get();
    }
    
    public IntegerProperty wordCountProperty() {
        return wordCount;
    }
    
    public int getWordCount() {
        return wordCount.get();
    }
}
