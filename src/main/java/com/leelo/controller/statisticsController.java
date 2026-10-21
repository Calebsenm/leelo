package com.leelo.controller;

import com.leelo.App;
import com.leelo.service.StatisticsService;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;

import java.time.LocalDate;
import java.util.List;

/**
 * The statistics of the reading activity.
 */
public class statisticsController {

    @FXML
    private TableView<StatisticsService.LanguageStats> statsTable;
    @FXML
    private TableColumn<StatisticsService.LanguageStats, String> languageCol;
    @FXML
    private TableColumn<StatisticsService.LanguageStats, Integer> dayCol;
    @FXML
    private TableColumn<StatisticsService.LanguageStats, Integer> weekCol;
    @FXML
    private TableColumn<StatisticsService.LanguageStats, Integer> monthCol;
    @FXML
    private TableColumn<StatisticsService.LanguageStats, Integer> yearCol;
    @FXML
    private TableColumn<StatisticsService.LanguageStats, Integer> totalCol;
    @FXML
    private Label streakLabel;
    @FXML
    private BarChart<String, Integer> chart;
    @FXML
    private CategoryAxis xAxis;
    @FXML
    private NumberAxis yAxis;
    @FXML
    private Button backButton;

    private final StatisticsService statisticsService = new StatisticsService();
    private final ObservableList<StatisticsService.LanguageStats> stats =
            FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        languageCol.setCellValueFactory(new PropertyValueFactory<>("languageName"));
        dayCol.setCellValueFactory(new PropertyValueFactory<>("day"));
        weekCol.setCellValueFactory(new PropertyValueFactory<>("week"));
        monthCol.setCellValueFactory(new PropertyValueFactory<>("month"));
        yearCol.setCellValueFactory(new PropertyValueFactory<>("year"));
        totalCol.setCellValueFactory(new PropertyValueFactory<>("total"));

        statsTable.setItems(stats);
        backButton.setOnAction(event -> goToHome());

        loadStats();
    }

    /** Loads the statistics and fills the chart. */
    private void loadStats() {
        List<StatisticsService.LanguageStats> all = statisticsService.getLanguageStats();
        stats.setAll(all);

        int streak = statisticsService.getReadingStreak();
        streakLabel.setText(streak > 0
                ? "Racha de lectura: " + streak + (streak == 1 ? " día" : " días")
                : "Aún no hay una racha de lectura.");

        chart.getData().clear();
        XYChart.Series<String, Integer> series = new XYChart.Series<>();
        series.setName("Palabras leídas");
        int maxWords = 0;
        for (StatisticsService.LanguageStats languageStats : all) {
            for (StatisticsService.ChartPoint point : languageStats.getChart()) {
                series.getData().add(new XYChart.Data<>(formatDate(point.date()), point.wordCount()));
                maxWords = Math.max(maxWords, point.wordCount());
            }
        }
        if (!series.getData().isEmpty()) {
            chart.getData().add(series);
        }
        configureAxis(maxWords);
    }

    /** Keeps the vertical axis to a few readable ticks, whatever the data. */
    private void configureAxis(int maxValue) {
        int tick = niceTick(Math.max(maxValue, 1));
        int upper = Math.max(tick, (int) Math.ceil(maxValue / (double) tick) * tick);
        yAxis.setAutoRanging(false);
        yAxis.setLowerBound(0);
        yAxis.setUpperBound(upper);
        yAxis.setTickUnit(tick);
        yAxis.setMinorTickCount(0);
    }

    /** Rounds a value up to 1, 2 or 5 times a power of ten. */
    private int niceTick(int maxValue) {
        double raw = Math.max(maxValue / 5.0, 1);
        double magnitude = Math.pow(10, Math.floor(Math.log10(raw)));
        double normalized = raw / magnitude;
        double nice = normalized <= 1 ? 1 : normalized <= 2 ? 2 : normalized <= 5 ? 5 : 10;
        return Math.max(1, (int) Math.round(nice * magnitude));
    }

    /** Formats a date for the axis of the chart. */
    private String formatDate(LocalDate date) {
        return date.getDayOfMonth() + "/" + date.getMonthValue();
    }

    /** Returns to the home screen. */
    private void goToHome() {
        try {
            App.showHome();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
