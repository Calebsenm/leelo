package com.leelo.service;

import com.leelo.dao.BookDAO;
import com.leelo.dao.BookPageDAO;
import com.leelo.model.Book;
import com.leelo.model.BookPage;
import com.leelo.model.BookTag;
import com.leelo.model.Language;
import com.leelo.parse.ParserRegistry;
import com.leelo.parse.TextParser;

import java.util.ArrayList;
import java.util.List;

/** Manages the books, their pages and their tags. */
public class BookService {

    private final BookDAO bookDAO = new BookDAO();
    private final BookPageDAO bookPageDAO = new BookPageDAO();
    private final BookTagService bookTagService = new BookTagService();

    /** Returns the books of a language, or every book when the language is zero. */
    public List<Book> getAll(int languageId, boolean includeArchived) {
        return bookDAO.findAll(languageId, includeArchived);
    }

    /** Returns the book with the given id, or {@code null}. */
    public Book getById(int id) {
        return bookDAO.findById(id);
    }

    /**
     * Creates a book and splits its text into pages.
     *
     * @return the id of the new book, or zero on failure
     */
    public int create(String title, Language language, String text, String sourceUri,
                      PageSplitter.SplitBy splitBy, int thresholdTokens, List<String> tagTexts) {
        if (title == null || title.trim().isEmpty() || language == null) {
            return 0;
        }
        Book book = new Book();
        book.setTitle(title.trim());
        book.setLanguageId(language.getId());
        book.setLanguageName(language.getName());
        book.setSourceUri(sourceUri == null ? "" : sourceUri);
        book.setTags(resolveTags(tagTexts));

        int bookId = bookDAO.insert(book);
        if (bookId <= 0) {
            return 0;
        }
        List<String> pages = PageSplitter.split(text, language, splitBy, thresholdTokens);
        savePages(bookId, pages, language);
        return bookId;
    }

    /** Updates the metadata of a book. */
    public boolean update(Book book, List<String> tagTexts) {
        book.setTags(resolveTags(tagTexts));
        return bookDAO.update(book);
    }

    /** Deletes a book and its pages. */
    public boolean delete(int id) {
        return bookDAO.delete(id);
    }

    /** Archives or unarchives a book. */
    public boolean setArchived(int bookId, boolean archived) {
        return bookDAO.setArchived(bookId, archived);
    }

    /** Records the page the reader was last on. */
    public boolean setCurrentPage(int bookId, int pageId) {
        return bookDAO.setCurrentPage(bookId, pageId);
    }

    /** Returns the pages of a book, in reading order. */
    public List<BookPage> getPages(int bookId) {
        return bookPageDAO.findByBook(bookId);
    }

    /** Returns the page with the given id, or {@code null}. */
    public BookPage getPage(int pageId) {
        return bookPageDAO.findById(pageId);
    }

    /** Returns the number of pages of a book. */
    public int getPageCount(int bookId) {
        return bookPageDAO.countByBook(bookId);
    }

    /** Adds an empty page before or after the given page. */
    public int addPage(int bookId, int referencePageId, boolean after) {
        List<BookPage> pages = bookPageDAO.findByBook(bookId);
        int position = 0;
        for (int i = 0; i < pages.size(); i++) {
            if (pages.get(i).getId() == referencePageId) {
                position = after ? i + 1 : i;
                break;
            }
        }
        BookPage page = new BookPage();
        page.setBookId(bookId);
        page.setOrder(position + 1);
        page.setContent("");
        int newId = bookPageDAO.insert(page);
        reorder(bookId, pages, newId, position);
        return newId;
    }

    /** Updates the content of a page and recalculates its word count. */
    public boolean updatePageContent(int pageId, String content, Language language) {
        int wordCount = countWords(content, language);
        return bookPageDAO.updateContent(pageId, content, wordCount);
    }

    /** Deletes a page; the last page of a book cannot be deleted. */
    public boolean deletePage(int bookId, int pageId) {
        List<BookPage> pages = bookPageDAO.findByBook(bookId);
        if (pages.size() <= 1) {
            return false;
        }
        if (!bookPageDAO.delete(pageId)) {
            return false;
        }
        List<Integer> remaining = new ArrayList<>();
        for (BookPage page : pages) {
            if (page.getId() != pageId) {
                remaining.add(page.getId());
            }
        }
        bookPageDAO.reorder(bookId, remaining);
        return true;
    }

    /** Marks a page as read. */
    public boolean setPageRead(int pageId, boolean read) {
        return bookPageDAO.setReadDate(pageId, read ? java.time.Instant.now() : null);
    }

    /** Records when a page was opened. */
    public boolean setPageStarted(int pageId) {
        return bookPageDAO.setStartDate(pageId, java.time.Instant.now());
    }

    /** Counts the words of a text, using the parser of the language. */
    public int countWords(String text, Language language) {
        if (text == null || text.isBlank()) {
            return 0;
        }
        TextParser parser = ParserRegistry.getParserOrDefault(language.getParserType());
        int count = 0;
        for (var token : parser.getParsedTokens(text, language)) {
            if (token.isWord()) {
                count++;
            }
        }
        return count;
    }

    /** Stores the pages of a book. */
    private void savePages(int bookId, List<String> contents, Language language) {
        int order = 1;
        for (String content : contents) {
            BookPage page = new BookPage();
            page.setBookId(bookId);
            page.setOrder(order++);
            page.setContent(content);
            page.setWordCount(countWords(content, language));
            bookPageDAO.insert(page);
        }
    }

    /** Rewrites the order of the pages after an insertion. */
    private void reorder(int bookId, List<BookPage> pages, int newPageId, int position) {
        List<Integer> ordered = new ArrayList<>();
        for (int i = 0; i < pages.size(); i++) {
            if (i == position) {
                ordered.add(newPageId);
            }
            ordered.add(pages.get(i).getId());
        }
        if (position >= pages.size()) {
            ordered.add(newPageId);
        }
        bookPageDAO.reorder(bookId, ordered);
    }

    /** Turns the tag names into the tags themselves, creating the missing ones. */
    private List<BookTag> resolveTags(List<String> tagTexts) {
        List<BookTag> tags = new ArrayList<>();
        if (tagTexts == null) {
            return tags;
        }
        for (String tagText : tagTexts) {
            if (tagText == null || tagText.isBlank()) {
                continue;
            }
            BookTag tag = bookTagService.findOrCreateByText(tagText.trim());
            if (tag != null) {
                tags.add(tag);
            }
        }
        return tags;
    }
}
