package com.leelo.service;

import com.leelo.dao.BookPageDAO;
import com.leelo.dao.BookStatsDAO;
import com.leelo.dao.ReadingEventDAO;
import com.leelo.model.Book;
import com.leelo.model.BookPage;
import com.leelo.model.BookStats;
import com.leelo.model.Language;
import com.leelo.model.Term;
import com.leelo.model.TermStatus;
import com.leelo.parse.ParsedToken;
import com.leelo.parse.ParserRegistry;
import com.leelo.render.MultiwordTermIndexer;
import com.leelo.render.TextItem;
import com.leelo.render.TextRenderer;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The statistics of the reading activity.
 *
 * <p>Two levels are covered: the global statistics, which aggregate the words
 * read per language, and the statistics of a book, which show how much of it
 * is still unknown.
 */
public class StatisticsService {

    private final ReadingEventDAO readingEventDAO = new ReadingEventDAO();
    private final BookStatsDAO bookStatsDAO = new BookStatsDAO();
    private final BookPageDAO bookPageDAO = new BookPageDAO();
    private final TermService termService = new TermService();
    private final SettingService settingService = new SettingService();
    private final TextRenderer textRenderer = new TextRenderer();

    /** Returns the statistics of every language that was read. */
    public List<LanguageStats> getLanguageStats() {
        Map<String, Map<LocalDate, Integer>> countsByLanguage =
                readingEventDAO.countWordsByLanguageAndDate();
        List<LanguageStats> stats = new ArrayList<>();
        for (Map.Entry<String, Map<LocalDate, Integer>> entry : countsByLanguage.entrySet()) {
            LanguageStats languageStats = new LanguageStats(entry.getKey());
            int runningTotal = 0;
            for (Map.Entry<LocalDate, Integer> day : entry.getValue().entrySet()) {
                runningTotal += day.getValue();
                languageStats.getChart().add(new ChartPoint(day.getKey(), day.getValue(), runningTotal));
            }
            languageStats.setTotal(runningTotal);
            languageStats.setDay(countOf(entry.getValue(), LocalDate.now()));
            languageStats.setWeek(countOf(entry.getValue(), LocalDate.now().minusDays(6)));
            languageStats.setMonth(countOf(entry.getValue(), LocalDate.now().minusDays(29)));
            languageStats.setYear(countOf(entry.getValue(), LocalDate.now().minusDays(364)));
            stats.add(languageStats);
        }
        stats.sort((a, b) -> a.getLanguageName().compareToIgnoreCase(b.getLanguageName()));
        return stats;
    }

    /** Counts the words read on the given day and on the days that follow it. */
    private int countOf(Map<LocalDate, Integer> counts, LocalDate from) {
        int total = 0;
        for (Map.Entry<LocalDate, Integer> entry : counts.entrySet()) {
            if (!entry.getKey().isBefore(from)) {
                total += entry.getValue();
            }
        }
        return total;
    }

    /**
     * Returns the number of consecutive days on which something was read.
     *
     * <p>The streak ends today or yesterday, so that a day that is still
     * running does not break it.
     */
    public int getReadingStreak() {
        List<LocalDate> days = readingEventDAO.findReadDays();
        if (days.isEmpty()) {
            return 0;
        }
        LocalDate today = LocalDate.now();
        LocalDate expected = today;
        if (!days.contains(today)) {
            expected = today.minusDays(1);
            if (!days.contains(expected)) {
                return 0;
            }
        }
        int streak = 0;
        for (LocalDate day : days) {
            if (day.equals(expected)) {
                streak++;
                expected = expected.minusDays(1);
            } else if (day.isBefore(expected)) {
                break;
            }
        }
        return streak;
    }

    /**
     * Returns the statistics of a book, calculating them when they are not
     * cached yet.
     */
    public BookStats getBookStats(Book book, Language language) {
        BookStats cached = bookStatsDAO.findByBook(book.getId());
        if (cached != null) {
            return cached;
        }
        BookStats stats = calculateBookStats(book, language);
        bookStatsDAO.save(stats);
        return stats;
    }

    /**
     * Calculates the statistics of a book from a sample of its pages.
     *
     * <p>The sample is taken around the page being read, which keeps the
     * calculation cheap while still reflecting the progress of the reader.
     */
    public BookStats calculateBookStats(Book book, Language language) {
        List<BookPage> pages = bookPageDAO.findByBook(book.getId());
        List<BookPage> sample = samplePages(pages, book.getCurrentPageId());

        Map<String, TermStatus> seen = new LinkedHashMap<>();
        MultiwordTermIndexer indexer = termService.buildMultiwordIndexer(language.getId());
        for (BookPage page : sample) {
            List<ParsedToken> tokens = ParserRegistry.getParserOrDefault(language.getParserType())
                    .getParsedTokens(page.getContent(), language);
            List<Term> terms = termService.findTermsInTokens(tokens, language, indexer);
            TextRenderer.RenderResult result = textRenderer.render(tokens, terms, language, indexer);
            for (TextItem item : result.items()) {
                if (!item.isWord() || item.getStatus() == null) {
                    continue;
                }
                seen.putIfAbsent(item.getTextLc(), TermStatus.fromCode(item.getStatus()));
            }
        }

        Map<TermStatus, Integer> distribution = new EnumMap<>(TermStatus.class);
        for (TermStatus status : TermStatus.values()) {
            distribution.put(status, 0);
        }
        for (TermStatus status : seen.values()) {
            distribution.merge(status, 1, Integer::sum);
        }

        BookStats stats = new BookStats();
        stats.setBookId(book.getId());
        stats.setStatusDistribution(distribution);
        int allUnique = seen.size();
        int unknowns = distribution.getOrDefault(TermStatus.UNKNOWN, 0);
        stats.setDistinctTerms(allUnique);
        stats.setDistinctUnknowns(unknowns);
        stats.setUnknownPercent(allUnique == 0 ? 0 : Math.round(100.0f * unknowns / allUnique));
        return stats;
    }

    /** Selects the pages used to calculate the statistics of a book. */
    private List<BookPage> samplePages(List<BookPage> pages, int currentPageId) {
        List<BookPage> sample = new ArrayList<>();
        if (pages.isEmpty()) {
            return sample;
        }
        int sampleSize = Math.max(1, settingService.getInt(SettingService.STATS_SAMPLE_SIZE));

        int current = 0;
        for (int i = 0; i < pages.size(); i++) {
            if (pages.get(i).getId() == currentPageId) {
                current = i;
                break;
            }
        }
        int start = Math.max(0, current - sampleSize / 2);
        int end = Math.min(pages.size(), start + sampleSize);
        start = Math.max(0, end - sampleSize);
        sample.addAll(pages.subList(start, end));
        return sample;
    }

    /** The reading statistics of a single language. */
    public static class LanguageStats {
        private final String languageName;
        private final List<ChartPoint> chart = new ArrayList<>();
        private int day;
        private int week;
        private int month;
        private int year;
        private int total;

        public LanguageStats(String languageName) {
            this.languageName = languageName;
        }

        public String getLanguageName() {
            return languageName;
        }

        public List<ChartPoint> getChart() {
            return chart;
        }

        public int getDay() {
            return day;
        }

        public void setDay(int day) {
            this.day = day;
        }

        public int getWeek() {
            return week;
        }

        public void setWeek(int week) {
            this.week = week;
        }

        public int getMonth() {
            return month;
        }

        public void setMonth(int month) {
            this.month = month;
        }

        public int getYear() {
            return year;
        }

        public void setYear(int year) {
            this.year = year;
        }

        public int getTotal() {
            return total;
        }

        public void setTotal(int total) {
            this.total = total;
        }
    }

    /** A point of the reading chart. */
    public record ChartPoint(LocalDate date, int wordCount, int runningTotal) {
    }
}
